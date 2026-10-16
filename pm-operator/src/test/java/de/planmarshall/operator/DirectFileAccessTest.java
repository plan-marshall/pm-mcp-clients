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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.planmarshall.api.MachinePaths;

@DisplayName("DirectFileAccess")
class DirectFileAccessTest {

    @TempDir
    Path base;

    private MachinePaths paths;
    private DirectFileAccess direct;

    @BeforeEach
    void setUp() {
        paths = new MachinePaths(base, MachinePaths.Os.LINUX);
        direct = new DirectFileAccess(paths);
    }

    @Nested
    @DisplayName("allowed paths policy")
    class AllowedPathsPolicy {

        @Test
        @DisplayName("allowedPaths contains exactly the 4 direct read files")
        void allowedPathsContent() {
            var expected = Set.of(
                                      paths.runtimeRecord(),
                                      paths.runtimeToken(),
                                      base.resolve("state/install-records.json"),
                                      base.resolve("machine-config.json")
            );

            assertEquals(expected, direct.allowedPaths());
        }

        @Test
        @DisplayName("isAllowed returns true only for the 4 direct read files")
        void isAllowed() {
            assertAll(
                                      () -> assertTrue(direct.isAllowed(paths.runtimeRecord())),
                                      () -> assertTrue(direct.isAllowed(paths.runtimeToken())),
                                      () -> assertTrue(direct.isAllowed(direct.installRecordsPath())),
                                      () -> assertTrue(direct.isAllowed(direct.machineConfigPath())),
                                      () -> assertFalse(direct.isAllowed(paths.runtimeLock())),
                                      () -> assertFalse(direct.isAllowed(paths.daemonLog())),
                                      () -> assertFalse(direct.isAllowed(base.resolve("projects/proj1/store.json")))
            );
        }

        @Test
        @DisplayName("checkAllowed throws SecurityException for paths under base outside the allowed list")
        void checkAllowedRejectsForbiddenFiles() {
            var forbidden = paths.daemonLog();
            var ex = assertThrows(SecurityException.class, () -> direct.checkAllowed(forbidden));
            assertTrue(ex.getMessage().contains("forbidden"));
        }
    }

    @Nested
    @DisplayName("file reading")
    class FileReading {

        @Test
        @DisplayName("reads machineConfig and installRecords when present")
        void readsConfigAndRecords() throws Exception {
            Files.createDirectories(paths.stateDir());
            Files.writeString(direct.machineConfigPath(), "{\"web\":{\"port\":7420}}");
            Files.writeString(direct.installRecordsPath(), "{\"bundles\":[]}");

            assertAll(
                                      () -> assertEquals("{\"web\":{\"port\":7420}}", direct.readMachineConfig().orElseThrow()),
                                      () -> assertEquals("{\"bundles\":[]}", direct.readInstallRecords().orElseThrow())
            );
        }

        @Test
        @DisplayName("returns empty when machineConfig or installRecords is absent")
        void returnsEmptyWhenAbsent() {
            assertAll(
                                      () -> assertTrue(direct.readMachineConfig().isEmpty()),
                                      () -> assertTrue(direct.readInstallRecords().isEmpty())
            );
        }
    }
}
