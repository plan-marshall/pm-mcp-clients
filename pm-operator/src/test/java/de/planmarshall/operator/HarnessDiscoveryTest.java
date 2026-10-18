/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@DisplayName("HarnessDiscovery")
@EnabledOnOs({OS.MAC, OS.LINUX})
class HarnessDiscoveryTest {

    private Path root;
    private Path bin;

    @BeforeEach
    void setUp() throws IOException {
        root = Files.createTempDirectory(Path.of("/tmp"), "pm-discovery");
        bin = Files.createDirectories(root.resolve("bin"));
    }

    @AfterEach
    void tearDown() throws IOException {
        try (var walk = Files.walk(root)) {
            for (var path : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private Path executable(String name, String output, int exitCode) throws IOException {
        var path = bin.resolve(name);
        Files.writeString(path, "#!/bin/sh\nprintf '%s\\n' \"" + output + "\"\nexit " + exitCode + "\n");
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwx------"));
        return path.toRealPath();
    }

    @Test
    @DisplayName("resolves the executable through an absolute PATH entry and reports its digest")
    void discoversHarness() throws Exception {
        var executable = executable("claude", "Claude Code 2.4.1", 0);

        var harness = HarnessDiscovery.discover("claude", Map.of("PATH", bin.toString()));

        assertEquals("claude", harness.client());
        assertEquals(executable, harness.executable());
        assertEquals("2.4.1", harness.version());
        assertEquals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                            .digest(Files.readAllBytes(executable))), harness.sha256Digest());
    }

    @Test
    @DisplayName("ignores a relative PATH entry and never executes from it")
    void ignoresRelativePathEntry() throws Exception {
        Files.writeString(bin.resolve("claude"), "#!/bin/sh\nexit 0\n");
        Files.setPosixFilePermissions(bin.resolve("claude"), PosixFilePermissions.fromString("rwx------"));

        var relative = Path.of("").toAbsolutePath().relativize(bin);
        assertThrows(HarnessDiscovery.HarnessNotFoundException.class,
                            () -> HarnessDiscovery.discover("claude", Map.of("PATH", relative.toString())));
    }

    @Test
    @DisplayName("reports an executable that is absent from PATH")
    void reportsMissingExecutable() {
        assertThrows(HarnessDiscovery.HarnessNotFoundException.class,
                            () -> HarnessDiscovery.discover("codex", Map.of("PATH", bin.toString())));
        assertThrows(HarnessDiscovery.HarnessNotFoundException.class,
                            () -> HarnessDiscovery.discover("codex", Map.of()));
    }

    @Test
    @DisplayName("reports a failing version probe and an empty one")
    void reportsFailedProbe() throws Exception {
        executable("codex", "boom", 1);
        assertThrows(HarnessDiscovery.HarnessProbeException.class,
                            () -> HarnessDiscovery.discover("codex", Map.of("PATH", bin.toString())));

        executable("opencode", "", 0);
        assertThrows(HarnessDiscovery.HarnessProbeException.class,
                            () -> HarnessDiscovery.discover("opencode", Map.of("PATH", bin.toString())));
    }

    @Test
    @DisplayName("records an unreported version for a profile without a version probe")
    void antigravityHasNoVersionProbe() throws Exception {
        var marker = root.resolve("antigravity-ran");
        Files.writeString(bin.resolve("antigravity"),
                            "#!/bin/sh\ntouch '" + marker + "'\nprintf 'antigravity preview\\n'\n");
        Files.setPosixFilePermissions(bin.resolve("antigravity"), PosixFilePermissions.fromString("rwx------"));

        var harness = HarnessDiscovery.discover("antigravity", Map.of("PATH", bin.toString()));

        assertEquals(HarnessDiscovery.UNREPORTED_VERSION, harness.version());
        assertFalse(Files.exists(marker), "a profile without a version probe never runs the executable");
    }

    @Test
    @DisplayName("falls back to the first output line when it carries no dotted version")
    void fallsBackToFirstLine() throws Exception {
        executable("claude", "claude preview", 0);

        var harness = HarnessDiscovery.discover("claude", Map.of("PATH", bin.toString()));

        assertEquals("claude preview", harness.version());
    }

    @Test
    @DisplayName("never hands the operator's environment to the probed executable")
    void probeEnvironmentIsRestricted() throws Exception {
        Files.writeString(bin.resolve("claude"),
                            "#!/bin/sh\nprintf '%s\\n' \"${PM_TEST_SECRET:-none}\"\n");
        Files.setPosixFilePermissions(bin.resolve("claude"), PosixFilePermissions.fromString("rwx------"));

        var harness = HarnessDiscovery.discover("claude",
                            Map.of("PATH", bin.toString(), "PM_TEST_SECRET", "super-secret"));

        assertEquals("none", harness.version());
    }

    @Test
    @DisplayName("knows the four client profiles of the plan and no other")
    void closedClientCatalogue() {
        assertTrue(HarnessDiscovery.isSupported("claude"));
        assertTrue(HarnessDiscovery.isSupported("opencode"));
        assertTrue(HarnessDiscovery.isSupported("codex"));
        assertTrue(HarnessDiscovery.isSupported("antigravity"));
        assertThrows(IllegalArgumentException.class,
                            () -> HarnessDiscovery.discover("cursor", Map.of("PATH", bin.toString())));
    }
}
