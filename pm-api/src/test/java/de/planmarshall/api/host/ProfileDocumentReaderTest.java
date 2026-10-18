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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("ProfileDocumentReader")
class ProfileDocumentReaderTest {

    private static ProfileDocumentReader.Profile parse(String json) {
        return ProfileDocumentReader
                .parseDocument(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
    }

    /** A minimal document in the closed form, for a client id and a precedence. */
    private static String document(String clientId, int precedence) {
        return """
                { "client_id": "%s",
                  "detection": { "precedence": %d, "primary_signals": [], "secondary_signals": [] },
                  "identity": { "session_id_variable": null, "version_probe": null, "version_pattern": null,
                    "model_source": "declared", "provisional": false } }
                """.formatted(clientId, precedence);
    }

    /** A class loader whose single classpath entry is an archive of the given {@code name, json} pairs. */
    private static URLClassLoader archiveOf(Path temp, String... documents) throws IOException {
        var archive = temp.resolve("profiles.jar");
        try (var out = new JarOutputStream(Files.newOutputStream(archive))) {
            out.putNextEntry(new JarEntry("profiles/"));
            out.closeEntry();
            for (var i = 0; i < documents.length; i += 2) {
                out.putNextEntry(new JarEntry("profiles/" + documents[i]));
                out.write(documents[i + 1].getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
        }
        return new URLClassLoader(new URL[]{archive.toUri().toURL()}, null);
    }

    @Nested
    @DisplayName("the shipped documents")
    class ShippedDocuments {

        @Test
        @DisplayName("are read in a strictly ascending, unique precedence")
        void precedenceIsAscendingAndUnique() {
            var profiles = ProfileDocumentReader.readShippedProfiles();

            var precedences = profiles.stream().map(profile -> profile.detection().precedence()).toList();
            assertAll(
                    () -> assertFalse(profiles.isEmpty(), "at least one profile document is shipped"),
                    () -> assertEquals(precedences.stream().distinct().count(), precedences.size()),
                    () -> assertEquals(precedences.stream().sorted().toList(), precedences));
        }

        @Test
        @DisplayName("declare a client id in the closed form of the schema")
        void clientIdsAreValid() {
            var profiles = ProfileDocumentReader.readShippedProfiles();

            assertTrue(profiles.stream().allMatch(profile -> profile.clientId().matches("[a-z][a-z0-9-]*")));
        }

        @Test
        @DisplayName("are read from an archive, skipping the records beside them")
        void readsFromAnArchive(@TempDir Path temp) throws Exception {
            try (var loader = archiveOf(temp, "fixture.json", document("fixture", 3), "verification/fixture.json",
                         "{}")) {
                var profiles = ProfileDocumentReader.readShippedProfiles(loader);

                assertAll(
                        () -> assertEquals(1, profiles.size()),
                        () -> assertEquals("fixture", profiles.getFirst().clientId()));
            }
        }

        @Test
        @DisplayName("refuse a class loader without a document")
        void refusesAnEmptyClasspath(@TempDir Path temp) throws Exception {
            try (var loader = archiveOf(temp)) {
                assertThrows(IllegalStateException.class, () -> ProfileDocumentReader.readShippedProfiles(loader));
            }
        }

        @Test
        @DisplayName("refuse a document whose file name is not its client id")
        void refusesFileNameMismatch(@TempDir Path temp) throws Exception {
            try (var loader = archiveOf(temp, "fixture.json", document("other", 3))) {
                assertThrows(IllegalStateException.class, () -> ProfileDocumentReader.readShippedProfiles(loader));
            }
        }

        @Test
        @DisplayName("refuse two documents that share a precedence")
        void refusesDuplicatedPrecedence(@TempDir Path temp) throws Exception {
            try (var loader = archiveOf(temp, "alpha.json", document("alpha", 3), "beta.json",
                         document("beta", 3))) {
                assertThrows(IllegalStateException.class, () -> ProfileDocumentReader.readShippedProfiles(loader));
            }
        }
    }

    @Nested
    @DisplayName("one document")
    class OneDocument {

        @Test
        @DisplayName("extracts client id, detection, and identity")
        void extractsTheTwoObjects() {
            var profile = parse("""
                    {
                      "client_id": "fixture",
                      "detection": {
                        "precedence": 7,
                        "primary_signals": [ { "variable": "FIXTURE_PRIMARY", "equals": null } ],
                        "secondary_signals": [ { "variable": "FIXTURE_SECONDARY", "equals": "1" } ]
                      },
                      "identity": {
                        "session_id_variable": "FIXTURE_SESSION",
                        "version_probe": [ "fixture", "--version" ],
                        "version_pattern": "(\\\\d+(?:\\\\.\\\\d+)+)",
                        "model_source": "declared",
                        "provisional": true
                      }
                    }
                    """);

            assertAll(
                    () -> assertEquals("fixture", profile.clientId()),
                    () -> assertEquals(7, profile.detection().precedence()),
                    () -> assertEquals(List.of(new HostDetection.Signal("FIXTURE_PRIMARY", null)),
                            profile.detection().primarySignals()),
                    () -> assertEquals(List.of(new HostDetection.Signal("FIXTURE_SECONDARY", "1")),
                            profile.detection().secondarySignals()),
                    () -> assertEquals("FIXTURE_SESSION", profile.identity().sessionIdVariable()),
                    () -> assertEquals(List.of("fixture", "--version"), profile.identity().versionProbe()),
                    () -> assertEquals("(\\d+(?:\\.\\d+)+)", profile.identity().versionPattern()),
                    () -> assertEquals("declared", profile.identity().modelSource()),
                    () -> assertTrue(profile.identity().provisional()));
        }

        @Test
        @DisplayName("skips every unrelated section")
        void skipsUnrelatedSections() {
            var profile = assertDoesNotThrow(() -> parse("""
                    {
                      "schema_version": 1,
                      "client_id": "fixture",
                      "tools": { "read_file": "Read", "idioms": { "Bash": { "disposition": "omit" } } },
                      "detection": {
                        "precedence": 7,
                        "primary_signals": [ { "variable": "FIXTURE_PRIMARY", "equals": null } ],
                        "secondary_signals": [],
                        "process_markers": [ "FIXTURE_PID" ],
                        "reaches": [ "command", "serve" ]
                      },
                      "identity": {
                        "session_id_variable": null,
                        "version_probe": null,
                        "version_pattern": null,
                        "model_source": "declared",
                        "provisional": false
                      },
                      "prompt_extensions": [ { "text": "ignored" } ]
                    }
                    """));

            assertAll(
                    () -> assertEquals("fixture", profile.clientId()),
                    () -> assertNull(profile.identity().sessionIdVariable()),
                    () -> assertNull(profile.identity().versionProbe()));
        }

        @Test
        @DisplayName("refuses malformed JSON")
        void refusesMalformedJson() {
            assertThrows(IllegalStateException.class, () -> parse("{ \"client_id\": "));
        }

        @Test
        @DisplayName("refuses trailing content after the object")
        void refusesTrailingContent() {
            assertThrows(IllegalStateException.class, () -> parse("{} {}"));
        }

        @Test
        @DisplayName("refuses a document without the detection object")
        void refusesMissingDetection() {
            assertThrows(IllegalStateException.class, () -> parse("""
                    { "client_id": "fixture",
                      "identity": { "session_id_variable": null, "version_probe": null, "version_pattern": null,
                        "model_source": "declared", "provisional": false } }
                    """));
        }

        @Test
        @DisplayName("refuses a non-positive precedence")
        void refusesNonPositivePrecedence() {
            assertThrows(IllegalStateException.class, () -> parse("""
                    { "client_id": "fixture",
                      "detection": { "precedence": 0, "primary_signals": [], "secondary_signals": [] },
                      "identity": { "session_id_variable": null, "version_probe": null, "version_pattern": null,
                        "model_source": "declared", "provisional": false } }
                    """));
        }

        @Test
        @DisplayName("refuses a signal without its equals value")
        void refusesSignalWithoutEquals() {
            assertThrows(IllegalStateException.class, () -> parse("""
                    { "client_id": "fixture",
                      "detection": { "precedence": 1,
                        "primary_signals": [ { "variable": "FIXTURE_PRIMARY" } ], "secondary_signals": [] },
                      "identity": { "session_id_variable": null, "version_probe": null, "version_pattern": null,
                        "model_source": "declared", "provisional": false } }
                    """));
        }

        @Test
        @DisplayName("refuses a version pattern without a version probe")
        void refusesPatternWithoutProbe() {
            assertThrows(IllegalStateException.class, () -> parse("""
                    { "client_id": "fixture",
                      "detection": { "precedence": 1, "primary_signals": [], "secondary_signals": [] },
                      "identity": { "session_id_variable": null, "version_probe": null,
                        "version_pattern": "(\\\\d+)", "model_source": "declared", "provisional": false } }
                    """));
        }
    }
}
