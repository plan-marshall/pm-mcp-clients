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
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Map;
import java.util.Optional;

import de.planmarshall.api.layout.InstallationLayout;
import de.planmarshall.api.layout.InstallationLayoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/**
 * The layout as the operator sees it (PM-ARCH-1, PM-ARCH-7): the runtime and relay siblings of the
 * operator's own version directory, the version token, whether the operator runs from a versioned
 * installation, and the runtime-upgradability of the installation (also the refusal of an
 * unversioned and an insecure layout, PM-SEC-4).
 */
@DisplayName("OperatorLayout")
@EnabledOnOs({OS.MAC, OS.LINUX})
class OperatorLayoutTest {

    private static final String VERSION = "1.2.3";

    @TempDir
    Path temporary;

    private Path root;
    private Path installRoot;

    @BeforeEach
    void setUp() throws IOException {
        root = temporary.toRealPath();
        installRoot = Files.createDirectories(root.resolve("install"));
    }

    private Path stage(String version) throws IOException {
        var bin = Files.createDirectories(installRoot.resolve(version).resolve("bin"));
        for (var name : new String[]{InstallationLayout.CLIENT_BINARY, InstallationLayout.OPERATOR_BINARY,
                InstallationLayout.DAEMON_BINARY}) {
            Files.writeString(bin.resolve(name), "");
        }
        return bin.resolve(InstallationLayout.OPERATOR_BINARY);
    }

    @Test
    @DisplayName("exposes the runtime and relay siblings and the version of the installation")
    void versioned() throws Exception {
        var operator = OperatorLayout.resolve(Map.of(), Optional.of(stage(VERSION)));

        assertEquals(Optional.of(VERSION), operator.version());
        assertTrue(operator.installed());
        assertTrue(operator.runtimeUpgradable());
        assertEquals(Optional.empty(), operator.upgradabilityRefusal());
        assertEquals(installRoot.resolve(VERSION).resolve("bin/pm-mcpd"), operator.runtimeExecutable());
        assertEquals(installRoot.resolve(VERSION).resolve("bin/pm-mcp"), operator.relayExecutable());
    }

    @Test
    @DisplayName("resolves the layout of the running operator process")
    void currentProcess() throws Exception {
        stage(VERSION);

        var operator = OperatorLayout.current(
                            Map.of(InstallationLayout.ENV_HOME, installRoot.resolve(VERSION).toString()));

        assertEquals(Optional.of(VERSION), operator.version());
        assertEquals(installRoot.resolve(VERSION).resolve("bin/pm-mcpd"), operator.runtimeExecutable());
    }

    @Test
    @DisplayName("refuses an installation that is not named by a version")
    void unversioned() throws Exception {
        var operator = OperatorLayout.resolve(Map.of(), Optional.of(stage("latest")));

        assertEquals(Optional.empty(), operator.version());
        assertFalse(operator.installed());
        assertFalse(operator.runtimeUpgradable());
        assertTrue(operator.upgradabilityRefusal().orElseThrow().contains("not named by a version"));
    }

    @Test
    @DisplayName("refuses an installation named by PM_MCP_HOME")
    void namedByEnvironment() throws Exception {
        var operator = OperatorLayout.resolve(
                            Map.of(InstallationLayout.ENV_HOME, installRoot.resolve(VERSION).toString()), Optional.empty());

        assertFalse(operator.installed());
        assertFalse(operator.runtimeUpgradable());
        assertTrue(operator.upgradabilityRefusal().orElseThrow()
                            .contains("named by " + InstallationLayout.ENV_HOME));
    }

    @Test
    @DisplayName("refuses an installation root writable by group or others")
    void insecure() throws Exception {
        var operator = stage(VERSION);
        Files.setPosixFilePermissions(installRoot, PosixFilePermissions.fromString("rwxrwxrwx"));

        assertThrows(InstallationLayoutException.class,
                            () -> OperatorLayout.resolve(Map.of(), Optional.of(operator)));
    }
}
