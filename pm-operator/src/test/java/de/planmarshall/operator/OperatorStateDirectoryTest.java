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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Map;

import de.planmarshall.api.runtime.RuntimeTokenFile;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("OperatorStateDirectory")
class OperatorStateDirectoryTest {

    @TempDir
    Path tempDir;

    @Nested
    @DisplayName("resolution")
    class Resolution {

        @Test
        @DisplayName("resolves XDG_STATE_HOME/pm-operator when variable is set")
        void resolvesFromXdg() {
            var xdg = tempDir.resolve("custom-xdg");
            var state = OperatorStateDirectory.resolve(Map.of("XDG_STATE_HOME", xdg.toString()), tempDir);

            assertAll(
                                () -> assertEquals(xdg.resolve("pm-operator"), state.root()),
                                () -> assertEquals(xdg.resolve("pm-operator/history"), state.historyFile())
            );
        }

        @Test
        @DisplayName("defaults to userHome/.local/state/pm-operator when XDG_STATE_HOME is unset or blank")
        void defaultsToUserHome() {
            var state1 = OperatorStateDirectory.resolve(Map.of(), tempDir);
            var state2 = OperatorStateDirectory.resolve(Map.of("XDG_STATE_HOME", " "), tempDir);

            assertAll(
                                () -> assertEquals(tempDir.resolve(".local/state/pm-operator"), state1.root()),
                                () -> assertEquals(tempDir.resolve(".local/state/pm-operator"), state2.root())
            );
        }
    }

    @Nested
    @DisplayName("directory and file security verification (PM-SEC-4)")
    @EnabledOnOs({OS.MAC, OS.LINUX})
    class SecurityVerification {

        @Test
        @DisplayName("ensureDirectory creates directory with mode 0700")
        void createsWithMode0700() throws Exception {
            var dir = tempDir.resolve("state-dir");
            var state = new OperatorStateDirectory(dir);

            state.ensureDirectory();

            assertAll(
                                () -> assertTrue(Files.isDirectory(dir)),
                                () -> assertEquals(RuntimeTokenFile.DIRECTORY_MODE, Files.getPosixFilePermissions(dir))
            );
        }

        @Test
        @DisplayName("verifyDirectory rejects symbolic links")
        void rejectsSymlinkDirectory() throws Exception {
            var realDir = tempDir.resolve("real-dir");
            Files.createDirectory(realDir);
            var symlink = tempDir.resolve("symlink-dir");
            Files.createSymbolicLink(symlink, realDir);

            var ex = assertThrows(InsecureOperatorFileException.class,
                                () -> OperatorStateDirectory.verifyDirectory(symlink));
            assertTrue(ex.getMessage().contains("is a symbolic link"));
        }

        @Test
        @DisplayName("verifyDirectory rejects insecure permissions like 0755")
        void rejectsInsecureDirectoryPermissions() throws Exception {
            var dir = tempDir.resolve("insecure-dir");
            Files.createDirectory(dir);
            Files.setPosixFilePermissions(dir, PosixFilePermissions.fromString("rwxr-xr-x"));

            var ex = assertThrows(InsecureOperatorFileException.class,
                                () -> OperatorStateDirectory.verifyDirectory(dir));
            assertTrue(ex.getMessage().contains("expected 0700"));
        }

        @Test
        @DisplayName("verifyFile accepts mode 0600 and rejects mode 0644 or symlinks")
        void verifyFilePermissions() throws Exception {
            var file = tempDir.resolve("test-file");
            Files.createFile(file);
            Files.setPosixFilePermissions(file, RuntimeTokenFile.FILE_MODE);

            OperatorStateDirectory.verifyFile(file);

            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-r--r--"));
            var ex1 = assertThrows(InsecureOperatorFileException.class,
                                () -> OperatorStateDirectory.verifyFile(file));
            assertTrue(ex1.getMessage().contains("expected 0600"));

            var symlink = tempDir.resolve("symlink-file");
            Files.setPosixFilePermissions(file, RuntimeTokenFile.FILE_MODE);
            Files.createSymbolicLink(symlink, file);
            var ex2 = assertThrows(InsecureOperatorFileException.class,
                                () -> OperatorStateDirectory.verifyFile(symlink));
            assertTrue(ex2.getMessage().contains("is a symbolic link"));
        }

        @Test
        @DisplayName("ensureFile creates file with mode 0600 within directory")
        void ensureFileCreatesWith0600() throws Exception {
            var dir = tempDir.resolve("state-file-dir");
            var state = new OperatorStateDirectory(dir);
            var history = state.historyFile();

            state.ensureFile(history);

            assertAll(
                                () -> assertTrue(Files.isRegularFile(history)),
                                () -> assertEquals(RuntimeTokenFile.FILE_MODE, Files.getPosixFilePermissions(history))
            );
        }
    }
}
