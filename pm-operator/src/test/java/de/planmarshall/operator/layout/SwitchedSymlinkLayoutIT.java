/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import de.planmarshall.api.layout.InstallationLayout;
import de.planmarshall.api.layout.InstallationLayoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/**
 * The installation layout as {@code pm-operator} sees it, staged as two versioned installation
 * directories below one installation root with the operator's two {@code PATH} links (PM-ARCH-1,
 * PM-ARCH-7): both binaries resolve {@code PM_MCP_HOME} through the links, the operator starts the
 * {@code pm-mcpd} of the same version directory by its absolute path, and after the links are
 * switched atomically to the new release both binaries resolve the new installation and start the
 * new {@code pm-mcpd} - never a same-named executable resolved through {@code PATH} (PM-WATCH-CLI-10
 * hazard: a sibling looked up through {@code PATH} lets any earlier executable run as the runtime).
 */
@DisplayName("Switched symlink installation layout")
@EnabledOnOs({OS.MAC, OS.LINUX})
class SwitchedSymlinkLayoutIT {

    private static final String OLD = "0.1.0";
    private static final String NEW = "0.2.0";
    private static final List<String> ENTRY_BINARIES = List.of(InstallationLayout.CLIENT_BINARY,
                        InstallationLayout.OPERATOR_BINARY);

    @TempDir
    Path temporary;

    private Path installRoot;
    private Path userBin;
    private Path sentinel;
    private Path poisonSentinel;

    @BeforeEach
    void setUp() throws IOException {
        var root = temporary.toRealPath();
        installRoot = Files.createDirectories(root.resolve("install"));
        userBin = Files.createDirectories(root.resolve("local/bin"));
        sentinel = root.resolve("sibling-started");
        poisonSentinel = root.resolve("path-lookalike-started");
        stage(OLD);
        stage(NEW);
        switchLinksTo(OLD);
    }

    @Test
    @DisplayName("resolves the installation of the linked binary and starts its sibling by path")
    void resolvesAndStartsSibling() throws Exception {
        var operator = OperatorLayout.resolve(Map.of(), Optional.of(userBin.resolve(
                            InstallationLayout.OPERATOR_BINARY)));

        assertEquals(installRoot.resolve(OLD), operator.installation().home());
        assertEquals(Optional.of(OLD), operator.version());
        assertEquals(installRoot.resolve(OLD).resolve("bin/pm-mcpd"), operator.runtimeExecutable());
        assertTrue(operator.runtimeUpgradable());

        for (var name : ENTRY_BINARIES) {
            assertEquals(installRoot.resolve(OLD).resolve("bin/pm-mcpd"), runLauncher(name));
        }
    }

    @Test
    @DisplayName("both binaries resolve the new installation after the links are switched")
    void resolvesNewInstallationAfterSwitch() throws Exception {
        assertEquals(installRoot.resolve(OLD).resolve("bin/pm-mcpd"),
                            runLauncher(InstallationLayout.OPERATOR_BINARY));

        switchLinksTo(NEW);

        var operator = OperatorLayout.resolve(Map.of(), Optional.of(userBin.resolve(
                            InstallationLayout.OPERATOR_BINARY)));
        assertEquals(installRoot.resolve(NEW), operator.installation().home());
        assertEquals(Optional.of(NEW), operator.version());
        assertEquals(installRoot.resolve(NEW).resolve("bin/pm-mcpd"), operator.runtimeExecutable());
        for (var name : ENTRY_BINARIES) {
            assertEquals(installRoot.resolve(NEW).resolve("bin/pm-mcpd"), runLauncher(name));
        }
    }

    @Test
    @DisplayName("refuses an unversioned installation")
    void unversioned() throws Exception {
        var unversioned = Files.createDirectories(installRoot.resolve("latest").resolve("bin"));
        writeScript(unversioned.resolve(InstallationLayout.OPERATOR_BINARY), launcher());

        var operator = OperatorLayout.resolve(Map.of(),
                            Optional.of(unversioned.resolve(InstallationLayout.OPERATOR_BINARY)));

        assertFalse(operator.installed());
        assertFalse(operator.runtimeUpgradable());
        assertTrue(operator.upgradabilityRefusal().orElseThrow().contains("not named by a version"));
    }

    @Test
    @DisplayName("refuses an installation root writable by group or others")
    void insecure() throws Exception {
        Files.setPosixFilePermissions(installRoot, PosixFilePermissions.fromString("rwxrwxrwx"));

        assertThrows(InstallationLayoutException.class, () -> OperatorLayout.resolve(Map.of(),
                            Optional.of(installRoot.resolve(OLD).resolve("bin/pm-mcpd"))));
        assertThrows(InstallationLayoutException.class, () -> OperatorLayout.resolve(Map.of(),
                            Optional.of(userBin.resolve(InstallationLayout.OPERATOR_BINARY))));
    }

    @Test
    @DisplayName("starts the sibling of its own version directory, never a same-named executable earlier on PATH")
    void ignoresExecutablesEarlierOnPath() throws Exception {
        var poison = Files.createDirectories(installRoot.getParent().resolve("poison"));
        for (var name : List.of(InstallationLayout.DAEMON_BINARY, InstallationLayout.LAUNCHER_BINARY)) {
            writeScript(poison.resolve(name), poison());
        }
        Files.deleteIfExists(sentinel);
        var builder = new ProcessBuilder(userBin.resolve(InstallationLayout.OPERATOR_BINARY).toString());
        builder.environment().remove(InstallationLayout.ENV_HOME);
        builder.environment().put("PATH", poison + ":/usr/bin:/bin");
        builder.redirectErrorStream(true);
        var process = builder.start();
        assertTrue(process.waitFor(30, TimeUnit.SECONDS), "the launcher did not finish");
        assertEquals(0, process.exitValue(), "the launcher failed");

        assertFalse(Files.exists(poisonSentinel), "a same-named executable earlier on PATH was started");
        assertEquals(installRoot.resolve(OLD).resolve("bin/pm-mcpd"), Path.of(Files.readString(sentinel).strip()));
    }

    private void stage(String version) throws IOException {
        var bin = Files.createDirectories(installRoot.resolve(version).resolve("bin"));
        for (var name : ENTRY_BINARIES) {
            writeScript(bin.resolve(name), launcher());
        }
        writeScript(bin.resolve(InstallationLayout.DAEMON_BINARY), daemon());
    }

    private void switchLinksTo(String version) throws IOException {
        for (var name : ENTRY_BINARIES) {
            var next = userBin.resolve(name + ".new");
            Files.deleteIfExists(next);
            Files.createSymbolicLink(next, installRoot.resolve(version).resolve("bin").resolve(name));
            Files.move(next, userBin.resolve(name), StandardCopyOption.ATOMIC_MOVE);
        }
    }

    /**
     * Runs one staged entry binary through its {@code PATH} link and returns the absolute path of
     * the sibling {@code pm-mcpd} that binary started.
     */
    private Path runLauncher(String name) throws IOException, InterruptedException {
        Files.deleteIfExists(sentinel);
        var builder = new ProcessBuilder(userBin.resolve(name).toString());
        builder.environment().remove(InstallationLayout.ENV_HOME);
        builder.redirectErrorStream(true);
        var process = builder.start();
        assertTrue(process.waitFor(30, TimeUnit.SECONDS), "the launcher did not finish");
        assertEquals(0, process.exitValue(), "the launcher failed");
        var started = Files.readAllLines(sentinel);
        assertEquals(1, started.size());
        return Path.of(started.getFirst());
    }

    /**
     * @return the launcher script: resolve this binary's real directory through the symlink chain
     *         and start the {@code pm-mcpd} of that same version directory by its absolute path
     */
    private static String launcher() {
        return """
                            #!/bin/sh
                            self="$0"
                            while [ -L "$self" ]; do
                                target="$(readlink "$self")"
                                case "$target" in
                                    /*) self="$target" ;;
                                    *) self="$(dirname "$self")/$target" ;;
                                esac
                            done
                            dir="$(cd "$(dirname "$self")" && pwd -P)"
                            exec "$dir/pm-mcpd"
                            """;
    }

    /** @return the mock {@code pm-mcpd}: record its own absolute path for the test to read */
    private String daemon() {
        return """
                            #!/bin/sh
                            self="$0"
                            case "$self" in
                                /*) ;;
                                *) self="$(pwd)/$self" ;;
                            esac
                            printf '%%s\\n' "$self" >> '%s'
                            """.formatted(sentinel);
    }

    /** @return a lookalike {@code pm-mcpd}/{@code pm-exec}: it must never be started */
    private String poison() {
        return """
                            #!/bin/sh
                            printf '%%s\\n' "$0" >> '%s'
                            """.formatted(poisonSentinel);
    }

    private static void writeScript(Path path, String body) throws IOException {
        Files.writeString(path, body);
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwxr-xr-x"));
    }
}
