/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.exec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;

@DisplayName("Command: the pm-exec argument vector")
@EnableGeneratorController
class CommandTest {

    private static Path absolutePath(String parent, String namePrefix) {
        return Path.of(parent, namePrefix + Generators.letterStrings(3, 8).next());
    }

    @Nested
    @DisplayName("accepts")
    class Accepts {

        @Test
        @DisplayName("--probe alone")
        void probe() throws Exception {
            assertInstanceOf(Command.Probe.class, Command.parse(List.of("--probe")));
        }

        @Test
        @DisplayName("a launch with write, read and deny-read sets")
        void fullLaunch() throws Exception {
            var workPath = absolutePath("/work", "p");
            var readPath = absolutePath("/base/run/jobs", "l");
            var deniedPath = absolutePath("/home/u", "base");
            var command = Command.parse(List.of("--write", workPath.toString(), "--read", readPath.toString(),
                    "--write", "/tmp/", "--deny-read", deniedPath.toString(), "--", "/usr/bin/git",
                    "status", "--", "-x"));

            var launch = assertInstanceOf(Command.Launch.class, command);
            assertEquals(List.of(workPath, Path.of("/tmp")), launch.writePaths());
            assertEquals(List.of(readPath), launch.readPaths());
            assertEquals(List.of(deniedPath), launch.deniedPaths());
            assertEquals(Optional.of(deniedPath), launch.deniedBase());
            assertEquals(List.of("/usr/bin/git", "status", "--", "-x"), launch.argv());
            assertEquals("/usr/bin/git", launch.program());
        }

        @Test
        @DisplayName("several --deny-read paths, in the order given")
        void severalDeniedPaths() throws Exception {
            var base = absolutePath("/home/u", "base");
            var bus = absolutePath("/run/user", "bus");
            var docker = absolutePath("/var/run", "socket");
            var launch = assertInstanceOf(Command.Launch.class, Command.parse(List.of(
                    "--deny-read", base.toString(),
                    "--deny-read", bus.toString(),
                    "--deny-read", docker.toString(),
                    "--", "/bin/true")));

            assertEquals(List.of(base, bus, docker), launch.deniedPaths());
            assertEquals(Optional.of(base), launch.deniedBase());
        }

        @Test
        @DisplayName("a bare launch without sets")
        void bareLaunch() throws Exception {
            var launch = assertInstanceOf(Command.Launch.class, Command.parse(List.of("--", "/bin/echo")));

            assertTrue(launch.writePaths().isEmpty());
            assertTrue(launch.readPaths().isEmpty());
            assertTrue(launch.deniedPaths().isEmpty());
            assertTrue(launch.deniedBase().isEmpty());
            assertEquals(List.of("/bin/echo"), launch.argv());
        }
    }

    @Nested
    @DisplayName("refuses")
    class Refuses {

        @Test
        @DisplayName("an empty argument vector")
        void empty() {
            assertThrows(UsageException.class, () -> Command.parse(List.of()));
        }

        @Test
        @DisplayName("a missing program after --")
        void missingProgram() {
            assertThrows(UsageException.class, () -> Command.parse(List.of("--write", "/a", "--")));
        }

        @Test
        @DisplayName("a missing --")
        void missingSeparator() {
            assertThrows(UsageException.class, () -> Command.parse(List.of("--write", "/a")));
        }

        @Test
        @DisplayName("an option without value")
        void optionWithoutValue() {
            var e = assertThrows(UsageException.class, () -> Command.parse(List.of("--write")));
            assertTrue(e.getMessage().contains("needs a value"));
        }

        @Test
        @DisplayName("an unknown option")
        void unknownOption() {
            assertThrows(UsageException.class, () -> Command.parse(List.of("--probe", "x", "--", "/bin/true")));
        }

        @Test
        @DisplayName("a relative --deny-read path")
        void relativeDeniedPath() {
            assertThrows(UsageException.class,
                    () -> Command.parse(List.of("--deny-read", "relative", "--", "/bin/true")));
        }

        @ParameterizedTest(name = "path ''{0}''")
        @ValueSource(strings = {"relative/path", "", "./x", "/nul\0byte"})
        @DisplayName("a path that is not absolute")
        void relativePath(String path) {
            assertThrows(UsageException.class, () -> Command.parse(List.of("--write", path, "--", "/bin/true")));
        }

        @Test
        @DisplayName("a program that is not absolute (no PATH search)")
        void relativeProgram() {
            assertThrows(UsageException.class, () -> Command.parse(List.of("--", "git", "status")));
        }
    }
}
