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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import de.planmarshall.api.MachinePaths;
import de.planmarshall.api.runtime.RuntimeRecord;

/**
 * Enforces the closed list of direct file reads under {@code <PM_MCP_BASE>} for CLI binaries
 * (01-cli-and-installer.adoc#cli-file-reads):
 * <ul>
 *   <li>{@code state/runtime.json}</li>
 *   <li>{@code run/runtime.token}</li>
 *   <li>{@code state/install-records.json} (with recorded target files)</li>
 *   <li>{@code machine-config.json}</li>
 * </ul>
 * Neither CLI binary writes any file under {@code <PM_MCP_BASE>}; every other state reaches a CLI
 * binary through the runtime's API (PM-SEC-4).
 */
public final class DirectFileAccess {

    private final MachinePaths paths;

    /**
     * @param paths the machine paths
     */
    public DirectFileAccess(MachinePaths paths) {
        this.paths = Objects.requireNonNull(paths, "paths");
    }

    /**
     * @return the machine paths
     */
    public MachinePaths paths() {
        return paths;
    }

    /**
     * @return the machine configuration path {@code <PM_MCP_BASE>/machine-config.json}
     */
    public Path machineConfigPath() {
        return paths.base().resolve("machine-config.json");
    }

    /**
     * @return the install records path {@code <PM_MCP_BASE>/state/install-records.json}
     */
    public Path installRecordsPath() {
        return paths.stateDir().resolve("install-records.json");
    }

    /**
     * @return the set of direct read paths under {@code <PM_MCP_BASE>}
     */
    public Set<Path> allowedPaths() {
        return Set.of(
                            paths.runtimeRecord().toAbsolutePath().normalize(),
                            paths.runtimeToken().toAbsolutePath().normalize(),
                            installRecordsPath().toAbsolutePath().normalize(),
                            machineConfigPath().toAbsolutePath().normalize());
    }

    /**
     * Checks if the given path is in the normative closed list of direct file reads under {@code <PM_MCP_BASE>}.
     *
     * @param target the target path to check
     * @return {@code true} if reading target directly is permitted
     */
    public boolean isAllowed(Path target) {
        var norm = target.toAbsolutePath().normalize();
        return allowedPaths().contains(norm);
    }

    /**
     * Asserts that reading the target path directly is permitted.
     *
     * @param target the target path
     * @return the verified path
     * @throws SecurityException if the path is under {@code <PM_MCP_BASE>} but not in the allowed list
     */
    public Path checkAllowed(Path target) {
        var norm = target.toAbsolutePath().normalize();
        if (norm.startsWith(paths.base()) && !isAllowed(norm)) {
            throw new SecurityException("Direct file read under " + paths.base() + " forbidden: " + target);
        }
        return target;
    }

    /**
     * Reads the runtime record if present and valid.
     *
     * @return the runtime record, or empty
     * @throws IOException on I/O failure
     */
    public Optional<RuntimeRecord> readRuntimeRecord() throws IOException {
        checkAllowed(paths.runtimeRecord());
        return RuntimeRecord.read(paths.runtimeRecord());
    }

    /**
     * Reads the machine configuration JSON if present.
     *
     * @return the raw machine-config JSON string, or empty
     * @throws IOException on I/O failure
     */
    public Optional<String> readMachineConfig() throws IOException {
        var path = checkAllowed(machineConfigPath());
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        return Optional.of(Files.readString(path));
    }

    /**
     * Reads the install records JSON if present.
     *
     * @return the raw install-records JSON string, or empty
     * @throws IOException on I/O failure
     */
    public Optional<String> readInstallRecords() throws IOException {
        var path = checkAllowed(installRecordsPath());
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        return Optional.of(Files.readString(path));
    }
}
