/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.host;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Architectural guard of the host-detection table (PM-CLIENT-2, PM-CLIENT-3): a production class of
 * the host package names no harness. A harness exists for the code only as its profile document, so a
 * harness id in a class is a defect the build refuses.
 * <p>
 * The set of forbidden ids is derived from the shipped profile documents, never listed here: a new
 * harness is a new document, and its id is guarded without a change of this test. The scan reads the
 * compiled classes, so a harness id in a Javadoc example of an unrelated class stays documentation.
 * The covered surface is the host package, the package this table owns; widening the guard to the
 * siblings of the module is a work package of its own.
 */
@DisplayName("harness isolation of the host package")
class HarnessIsolationArchTest {

    /** The package whose production classes the guard covers. */
    private static final String HOST_PACKAGE = "de/planmarshall/api/host";

    @Test
    @DisplayName("no production class of the host package names a harness")
    void productionClassesNameNoHarness() throws Exception {
        var harnessIds = shippedHarnessIds();
        assertFalse(harnessIds.isEmpty(), "the shipped profile documents name the harnesses of the table");

        var violations = violationsIn(productionClassFiles(), harnessIds);
        assertTrue(violations.isEmpty(), () -> "a production class names a harness: " + violations.keySet());
    }

    @Test
    @DisplayName("the guard finds an injected harness name (negative control)")
    void guardFindsInjectedHarnessName() throws Exception {
        var harnessIds = shippedHarnessIds();
        var injected = HarnessNamingControlFixture.injectedHarnessId();
        var fixture = classFileOf(HarnessNamingControlFixture.class);

        var violations = violationsIn(List.of(fixture), harnessIds);
        assertAll(
                () -> assertTrue(harnessIds.contains(injected),
                        "the derived ids hold the injected harness name"),
                () -> assertTrue(violations.getOrDefault(fixture, Set.of()).contains(injected),
                        "the guard finds the harness name in the test fixture"));
    }

    private static Set<String> shippedHarnessIds() throws IOException {
        try (var documents = Files.list(codeSourceOf(HostDetection.class).resolve("profiles"))) {
            return documents.map(path -> path.getFileName().toString()).filter(name -> name.endsWith(".json"))
                    .map(name -> name.substring(0, name.length() - ".json".length()))
                    .filter(id -> !HostDetection.NEUTRAL.equals(id)).collect(Collectors.toUnmodifiableSet());
        }
    }

    private static List<Path> productionClassFiles() throws IOException {
        try (var files = Files.walk(codeSourceOf(HostDetection.class).resolve(HOST_PACKAGE))) {
            return files.filter(path -> path.toString().endsWith(".class")).toList();
        }
    }

    private static Map<Path, Set<String>> violationsIn(List<Path> classFiles, Set<String> harnessIds)
            throws IOException {
        var violations = new LinkedHashMap<Path, Set<String>>();
        for (var classFile : classFiles) {
            var text = Files.readString(classFile, StandardCharsets.ISO_8859_1).toLowerCase(Locale.ROOT);
            var found = harnessIds.stream().filter(text::contains).collect(Collectors.toCollection(TreeSet::new));
            if (!found.isEmpty()) {
                violations.put(classFile, found);
            }
        }
        return violations;
    }

    private static Path classFileOf(Class<?> type) {
        var resource = type.getResource(type.getSimpleName() + ".class");
        if (resource == null) {
            throw new IllegalStateException("the class file of " + type.getName() + " is not on the classpath");
        }
        return toPath(resource, type);
    }

    private static Path codeSourceOf(Class<?> type) {
        var source = type.getProtectionDomain().getCodeSource();
        if (source == null) {
            throw new IllegalStateException("the code source of " + type.getName() + " is unknown");
        }
        return toPath(source.getLocation(), type);
    }

    private static Path toPath(URL url, Class<?> type) {
        try {
            return Path.of(url.toURI());
        } catch (URISyntaxException | FileSystemNotFoundException e) {
            throw new IllegalStateException("cannot resolve the classpath location of " + type.getName(), e);
        }
    }
}
