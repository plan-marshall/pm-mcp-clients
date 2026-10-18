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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import de.planmarshall.api.layout.InstallationLayout;

/**
 * The installation layout as {@code pm-operator} sees it (PM-ARCH-1, PM-ARCH-7): the versioned
 * installation directory of the running operator binary, the runtime and relay binaries of that
 * same version directory that the operator starts by absolute path, and whether the installation
 * may be upgraded by the runtime from a client.
 * <p>
 * The operator binary is reached through one of the two {@code PATH} links, or directly inside its
 * version directory; either way {@link InstallationLayout} resolves the same {@code <PM_MCP_HOME>}
 * through the switched link, so after an upgrade the operator starts the {@code pm-mcpd} of the new
 * release and never a same-named executable earlier on {@code PATH}.
 *
 * @param installation the resolved installation layout of the running operator binary
 */
public record OperatorLayout(InstallationLayout installation) {

    /**
     * @param installation the resolved installation layout
     */
    public OperatorLayout {
        Objects.requireNonNull(installation, "installation");
    }

    /**
     * Resolves the layout of the running operator process.
     *
     * @param environment the process environment
     * @return the layout
     * @throws IOException if it cannot be resolved
     */
    public static OperatorLayout current(Map<String, String> environment) throws IOException {
        return new OperatorLayout(InstallationLayout.current(environment));
    }

    /**
     * Resolves the layout of an operator binary.
     *
     * @param environment the process environment
     * @param executable  the operator binary, if known
     * @return the layout
     * @throws IOException if it cannot be resolved
     */
    public static OperatorLayout resolve(Map<String, String> environment, Optional<Path> executable)
                        throws IOException {
        return new OperatorLayout(InstallationLayout.resolve(environment, executable));
    }

    /** @return {@code <PM_MCP_HOME>/bin/pm-mcpd}, the runtime the operator starts on demand */
    public Path runtimeExecutable() {
        return installation.locateSibling(InstallationLayout.DAEMON_BINARY);
    }

    /** @return {@code <PM_MCP_HOME>/bin/pm-mcp}, the relay of the same installation */
    public Path relayExecutable() {
        return installation.locateSibling(InstallationLayout.CLIENT_BINARY);
    }

    /** @return the version token of the installation directory, or empty when it is not versioned */
    public Optional<String> version() {
        return installation.version();
    }

    /**
     * @return {@code true} if the operator runs from a versioned installation resolved from its own
     *         binary rather than one named by {@code PM_MCP_HOME}
     */
    public boolean installed() {
        return installation.resolvedFromBinary() && installation.version().isPresent();
    }

    /** @return {@code true} if the runtime may upgrade this installation */
    public boolean runtimeUpgradable() {
        return installation.runtimeUpgradable();
    }

    /** @return the reason the runtime may not upgrade this installation, or empty when it may */
    public Optional<String> upgradabilityRefusal() {
        return installation.upgradabilityRefusal();
    }
}
