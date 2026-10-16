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

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import de.planmarshall.api.host.HostDetection;

/**
 * Gate for operator verbs and console entry (PM-SEC-3): verifies that the process is attached
 * to an interactive terminal ({@code System.console() != null && System.console().isTerminal()})
 * and that no coding-agent environment signal from the Host Integration Table is present.
 */
public final class OperatorGate {

    /** Exit code of a verb refused by the operator checks. */
    public static final int EXIT_OPERATOR_CHECKS = 4;

    private static final String CODEX = "codex";

    private final OperatorContext context;

    /**
     * @param context the operator context
     */
    public OperatorGate(OperatorContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    /**
     * Verifies the operator preconditions.
     *
     * @throws OperatorSecurityException if an agent environment is detected or no interactive terminal is attached
     */
    public void verify() throws OperatorSecurityException {
        var detectedHost = detectHost(context.environment());
        if (detectedHost.isPresent()) {
            var host = detectedHost.get();
            if (HostDetection.UNKNOWN.equals(host)) {
                throw new OperatorSecurityException("operator refused: coding-agent environment detected");
            }
            throw new OperatorSecurityException("operator refused: coding-agent environment detected (" + host + ")");
        }

        if (!context.isInteractiveTerminal()) {
            throw new OperatorSecurityException("operator refused: interactive terminal required");
        }
    }

    /**
     * Detects a coding-agent host from the environment variables according to the normative precedence:
     * {@code antigravity -> opencode -> claude -> codex}.
     *
     * @param environment the process environment
     * @return the detected host name, {@link HostDetection#UNKNOWN} if unmapped, or empty if none
     */
    public static Optional<String> detectHost(Map<String, String> environment) {
        // Precedence: antigravity -> opencode -> claude -> codex
        if (hasHostSignal("antigravity", environment)
                      || environment.containsKey("GEMINI_CLI") || environment.containsKey("ANTIGRAVITY_AGENT")) {
            return Optional.of("antigravity");
        }

        if (hasHostSignal("opencode", environment)
                      || environment.containsKey("OPENCODE_CLIENT") || environment.containsKey("OPENCODE")
                      || environment.containsKey("OPENCODE_PID")) {
            return Optional.of("opencode");
        }

        if (hasHostSignal("claude", environment)
                      || environment.containsKey("CLAUDE_CODE") || environment.containsKey("CLAUDE_PROJECT_ROOT")
                      || environment.containsKey("CLAUDECODE") || environment.containsKey("CLAUDE_CODE_SESSION_ID")) {
            return Optional.of("claude");
        }

        if (hasHostSignal(CODEX, environment)) {
            return Optional.of(CODEX);
        }
        for (var key : environment.keySet()) {
            if (key.startsWith("CODEX_")) {
                return Optional.of(CODEX);
            }
        }

        if (HostDetection.agentSignalPresent(environment) || environment.containsKey("AI_AGENT")) {
            return Optional.of(HostDetection.UNKNOWN);
        }

        return Optional.empty();
    }

    private static boolean hasHostSignal(String hostName, Map<String, String> environment) {
        return HostDetection.host(hostName)
                      .map(host -> host.primarySignals().stream().anyMatch(signal -> signal.matches(environment))
                                    || host.secondarySignals().stream().anyMatch(signal -> signal.matches(environment)))
                      .orElse(false);
    }
}
