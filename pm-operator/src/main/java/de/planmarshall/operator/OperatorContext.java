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
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.MachinePaths;
import de.planmarshall.api.PmVersion;
import de.planmarshall.api.runtime.RuntimeAccess;
import de.planmarshall.api.runtime.RuntimeClient;

/**
 * What an operator command takes from its process: the environment and the user's home directory,
 * whether the process is attached to an interactive terminal, the client environment, the security
 * gate, the operator state directory, and the runtime clients built from them. Tests pass their own.
 */
public final class OperatorContext {

    private final Map<String, String> environment;
    private final Path userHome;
    private final boolean interactiveTerminal;
    private final ClientEnvironment clientEnvironment;
    private final OperatorGate gate;
    private final OperatorStateDirectory stateDirectory;
    private final DirectFileAccess directFiles;

    /**
     * Creates an operator context assuming an interactive terminal and capturing client environment.
     *
     * @param environment the process environment
     * @param userHome    the user's home directory
     */
    public OperatorContext(Map<String, String> environment, Path userHome) {
        this(environment, userHome, true);
    }

    /**
     * Creates an operator context with explicit terminal interactivity flag.
     *
     * @param environment         the process environment
     * @param userHome            the user's home directory
     * @param interactiveTerminal {@code true} if attached to an interactive terminal
     */
    public OperatorContext(Map<String, String> environment, Path userHome, boolean interactiveTerminal) {
        this(environment, userHome, interactiveTerminal, ClientEnvironment.capture(environment));
    }

    /**
     * Creates an operator context with full explicit configuration.
     *
     * @param environment         the process environment
     * @param userHome            the user's home directory
     * @param interactiveTerminal {@code true} if attached to an interactive terminal
     * @param clientEnvironment   the captured client environment
     */
    public OperatorContext(Map<String, String> environment, Path userHome, boolean interactiveTerminal,
            ClientEnvironment clientEnvironment) {
        this.environment = Map.copyOf(Objects.requireNonNull(environment, "environment"));
        this.userHome = Objects.requireNonNull(userHome, "userHome");
        this.interactiveTerminal = interactiveTerminal;
        this.clientEnvironment = Objects.requireNonNull(clientEnvironment, "clientEnvironment");
        this.gate = new OperatorGate(this);
        this.stateDirectory = OperatorStateDirectory.resolve(environment, userHome);
        this.directFiles = new DirectFileAccess(paths());
    }

    /** @return the context of the running process */
    static OperatorContext system() {
        var console = System.console();
        var interactive = console != null && console.isTerminal();
        return new OperatorContext(System.getenv(), Path.of(System.getProperty("user.home")), interactive);
    }

    /** @return the process environment */
    public Map<String, String> environment() {
        return environment;
    }

    /** @return the user's home directory */
    public Path userHome() {
        return userHome;
    }

    /** @return {@code true} if attached to an interactive terminal */
    public boolean isInteractiveTerminal() {
        return interactiveTerminal;
    }

    /** @return the captured client environment */
    public ClientEnvironment clientEnvironment() {
        return clientEnvironment;
    }

    /** @return the operator gate for security verifications */
    public OperatorGate gate() {
        return gate;
    }

    /** @return the state directory manager */
    public OperatorStateDirectory stateDirectory() {
        return stateDirectory;
    }

    /** @return the direct file access enforcer */
    public DirectFileAccess directFiles() {
        return directFiles;
    }

    /** @return the machine paths */
    public MachinePaths paths() {
        return MachinePaths.resolve(environment, userHome, MachinePaths.Os.current());
    }

    /**
     * @param startOnDemand {@code true} to start the runtime when none answers
     * @return the runtime client of this command
     * @throws IOException if the client cannot be set up
     */
    public RuntimeClient runtime(boolean startOnDemand) throws IOException {
        var headers = Map.of(
                "User-Agent", "pm-operator/" + PmVersion.current(),
                "Accept-Language", clientEnvironment.locale());
        return RuntimeAccess.local(environment, userHome, headers, startOnDemand);
    }
}
