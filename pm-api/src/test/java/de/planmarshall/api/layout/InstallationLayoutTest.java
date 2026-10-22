/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The installation layout of the client binaries: the resolution of {@code PM_MCP_HOME} through the
 * two switched {@code PATH} symbolic links (and through a chain of two), the versioned installation
 * directory, the sibling binaries beside the running one, and the refusals of an unversioned, a
 * broken, and an insecure layout (PM-ARCH-1, PM-ARCH-7, PM-SEC-4).
 */
@DisplayName("InstallationLayout")
@EnabledOnOs({OS.MAC, OS.LINUX})
class InstallationLayoutTest {

    private static final String OLD = "0.1.0";
    private static final String NEW = "0.2.0";

    @TempDir
    Path temporary;

    private Path root;

    @BeforeEach
    void canonical() throws IOException {
        root = temporary.toRealPath();
    }

    private Path stage(String installRoot, String version) throws IOException {
        var bin = Files.createDirectories(root.resolve(installRoot).resolve(version).resolve("bin"));
        for (var name : new String[]{InstallationLayout.CLIENT_BINARY, InstallationLayout.OPERATOR_BINARY,
                InstallationLayout.DAEMON_BINARY, InstallationLayout.LAUNCHER_BINARY}) {
            Files.writeString(bin.resolve(name), "");
        }
        return bin.getParent();
    }

    private Path linkInto(String name, Path target) throws IOException {
        var userBin = Files.createDirectories(root.resolve("local/bin"));
        return Files.createSymbolicLink(userBin.resolve(name), target);
    }

    private static Path binaryOf(Path installation) {
        return installation.resolve(InstallationLayout.BIN_DIRECTORY).resolve(InstallationLayout.CLIENT_BINARY);
    }

    @Nested
    @DisplayName("Resolution of PM_MCP_HOME")
    class Resolution {

        @Test
        @DisplayName("takes the environment variable when set, normalized")
        void environment() throws Exception {
            var home = InstallationLayout.resolve(
                    Map.of(InstallationLayout.ENV_HOME, "/opt/pm/../pm-1"), Optional.empty());

            assertEquals(Path.of("/opt/pm-1"), home.home());
            assertFalse(home.resolvedFromBinary());
            assertEquals(Path.of("/opt/pm-1/bin/pm-mcpd"), home.locateSibling(InstallationLayout.DAEMON_BINARY));
        }

        @Test
        @DisplayName("ignores a blank environment variable")
        void ignoresBlank() throws Exception {
            var installation = stage("install", OLD);

            var home = InstallationLayout.resolve(Map.of(InstallationLayout.ENV_HOME, " "),
                    Optional.of(binaryOf(installation)));

            assertEquals(installation, home.home());
        }

        @Test
        @DisplayName("resolves the versioned directory of the running binary")
        void resolvesFromBinary() throws Exception {
            var installation = stage("install", OLD);

            var home = InstallationLayout.resolve(Map.of(), Optional.of(binaryOf(installation)));

            assertEquals(root.resolve("install").resolve(OLD), home.home());
            assertEquals(Optional.of(root.resolve("install")), home.installRoot());
            assertEquals(Optional.of(OLD), home.version());
            assertTrue(home.resolvedFromBinary());
        }

        @Test
        @DisplayName("resolves a chain of two symlinks to the installation of the executable")
        void symlinkChain() throws Exception {
            var installation = stage("install", OLD);
            var middle = Files.createSymbolicLink(root.resolve("middle"), binaryOf(installation));
            var entry = linkInto(InstallationLayout.CLIENT_BINARY, middle);

            var home = InstallationLayout.resolve(Map.of(), Optional.of(entry));

            assertEquals(installation, home.home());
        }

        @Test
        @DisplayName("resolves the version of each installation below the installation root")
        void installationRootHoldsVersions() throws Exception {
            var old = stage("install", OLD);
            var fresh = stage("install", NEW);

            assertEquals(Optional.of(OLD),
                    InstallationLayout.resolve(Map.of(), Optional.of(binaryOf(old))).version());
            assertEquals(Optional.of(NEW),
                    InstallationLayout.resolve(Map.of(), Optional.of(binaryOf(fresh))).version());
        }

        @Test
        @DisplayName("resolves a versions/<version> nesting the same way")
        void versionsNesting() throws Exception {
            var installation = stage("install/versions", OLD);

            var home = InstallationLayout.resolve(Map.of(), Optional.of(binaryOf(installation)));

            assertEquals(installation, home.home());
            assertEquals(Optional.of(root.resolve("install/versions")), home.installRoot());
            assertEquals(Optional.of(OLD), home.version());
        }

        @Test
        @DisplayName("resolveHome and resolveVersion expose the two steps")
        void staticSteps() throws Exception {
            var installation = stage("install", OLD);

            assertEquals(installation, InstallationLayout.resolveHome(Map.of(), Optional.of(binaryOf(installation))));
            assertEquals(Optional.of(OLD), InstallationLayout.resolveVersion(binaryOf(installation)));
        }
    }

    @Nested
    @DisplayName("Version tokens")
    class VersionTokens {

        @ParameterizedTest(name = "\"{0}\" is a version: {1}")
        @CsvSource({"1.2.3, true", "1, true", "0.1.0-SNAPSHOT, true", "1.2.3+build.4, true", "1-2, true",
                "1.2.3x, false", "1.2.3!, false", "1.2.3-SNAP!, false", "1-, false", "latest, false", "1., false",
                "1..2, false", ".1, false"})
        void recognizesVersions(String name, boolean versioned) throws Exception {
            var installation = stage("install", name);

            var home = InstallationLayout.resolve(Map.of(), Optional.of(binaryOf(installation)));

            assertEquals(versioned, home.version().isPresent());
        }
    }

    @Nested
    @DisplayName("Sibling binaries")
    class Siblings {

        @Test
        @DisplayName("locates a sibling by absolute path in the same version directory")
        void locateSibling() throws Exception {
            var installation = stage("install", NEW);
            var entry = linkInto(InstallationLayout.OPERATOR_BINARY,
                    installation.resolve(InstallationLayout.BIN_DIRECTORY).resolve(InstallationLayout.OPERATOR_BINARY));

            var home = InstallationLayout.resolve(Map.of(), Optional.of(entry));

            assertEquals(installation.resolve("bin/pm-mcpd"), home.locateSibling(InstallationLayout.DAEMON_BINARY));
            assertEquals(installation.resolve("bin/pm-mcp"), home.locateSibling(InstallationLayout.CLIENT_BINARY));
        }
    }

    @Nested
    @DisplayName("Refusals of a layout that cannot be resolved")
    class Refusals {

        @Test
        @DisplayName("refuses when neither the variable nor the executable names an installation")
        void unknownExecutable() {
            assertThrows(InstallationLayoutException.class,
                    () -> InstallationLayout.resolve(Map.of(), Optional.empty()));
        }

        @Test
        @DisplayName("refuses an executable at the filesystem root")
        void executableAtRoot() {
            assertThrows(InstallationLayoutException.class,
                    () -> InstallationLayout.resolve(Map.of(), Optional.of(Path.of("/"))));
        }

        @Test
        @DisplayName("refuses an executable outside a bin/ directory")
        void executableOutsideBin() throws Exception {
            var plain = Files.writeString(Files.createDirectories(root.resolve("plain")).resolve("pm-mcp"), "");

            assertThrows(InstallationLayoutException.class,
                    () -> InstallationLayout.resolve(Map.of(), Optional.of(plain)));
            assertEquals(Optional.empty(), InstallationLayout.resolveVersion(plain));
        }

        @Test
        @DisplayName("refuses a dangling symbolic link")
        void danglingLink() throws Exception {
            var dangling = Files.createSymbolicLink(root.resolve("dangling"),
                    root.resolve("missing/bin/pm-mcp"));

            assertThrows(InstallationLayoutException.class,
                    () -> InstallationLayout.resolve(Map.of(), Optional.of(dangling)));
        }

        @Test
        @DisplayName("refuses an executable that is not one of the release binaries")
        void notAReleaseBinary() throws Exception {
            var bin = Files.createDirectories(root.resolve("install").resolve(OLD).resolve("bin"));
            var java = Files.writeString(bin.resolve("java"), "");

            assertThrows(InstallationLayoutException.class,
                    () -> InstallationLayout.resolve(Map.of(), Optional.of(java)));
        }

        @Test
        @DisplayName("refuses a bin/ directory writable by group or others")
        void insecureBinDirectory() throws Exception {
            var installation = stage("install", OLD);
            Files.setPosixFilePermissions(installation.resolve(InstallationLayout.BIN_DIRECTORY),
                    PosixFilePermissions.fromString("rwxrwxrwx"));

            assertThrows(InstallationLayoutException.class,
                    () -> InstallationLayout.resolve(Map.of(), Optional.of(binaryOf(installation))));
        }

        @Test
        @DisplayName("refuses an installation directory writable by group or others")
        void insecureInstallationDirectory() throws Exception {
            var installation = stage("install", OLD);
            Files.setPosixFilePermissions(installation, PosixFilePermissions.fromString("rwxrwxrwx"));

            assertThrows(InstallationLayoutException.class,
                    () -> InstallationLayout.resolve(Map.of(), Optional.of(binaryOf(installation))));
        }

        @Test
        @DisplayName("refuses an installation root writable by group or others")
        void insecureInstallationRoot() throws Exception {
            var installation = stage("install", OLD);
            Files.setPosixFilePermissions(installation.getParent(), PosixFilePermissions.fromString("rwxrwxrwx"));

            assertThrows(InstallationLayoutException.class,
                    () -> InstallationLayout.resolve(Map.of(), Optional.of(binaryOf(installation))));
        }
    }

    @Nested
    @DisplayName("Runtime upgradability")
    class Upgradability {

        @Test
        @DisplayName("accepts a versioned installation in a user-writable installation root")
        void upgradable() throws Exception {
            var installation = stage("install", OLD);

            var home = InstallationLayout.resolve(Map.of(), Optional.of(binaryOf(installation)));

            assertTrue(home.runtimeUpgradable());
            assertEquals(Optional.empty(), home.upgradabilityRefusal());
        }

        @Test
        @DisplayName("refuses an installation named by PM_MCP_HOME")
        void namedByEnvironment() throws Exception {
            var installation = stage("install", OLD);

            var home = InstallationLayout.resolve(
                    Map.of(InstallationLayout.ENV_HOME, installation.toString()), Optional.empty());

            assertFalse(home.runtimeUpgradable());
            assertTrue(home.upgradabilityRefusal().orElseThrow()
                    .contains("named by " + InstallationLayout.ENV_HOME));
        }

        @Test
        @DisplayName("refuses an installation that is not named by a version")
        void unversioned() throws Exception {
            var unversioned = stage("install", "latest");

            var home = InstallationLayout.resolve(Map.of(), Optional.of(binaryOf(unversioned)));

            assertEquals(Optional.empty(), home.version());
            assertFalse(home.runtimeUpgradable());
            assertTrue(home.upgradabilityRefusal().orElseThrow().contains("not named by a version"));
        }
    }
}
