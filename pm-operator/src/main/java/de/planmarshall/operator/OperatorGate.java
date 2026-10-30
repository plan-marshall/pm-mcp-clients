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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.host.HostDetection;
import de.planmarshall.api.message.MessageCatalogue;

/**
 * Gate for operator acts (PM-SEC-3): verifies that the process is attached to an interactive
 * terminal ({@code System.console() != null && System.console().isTerminal()}) and that no
 * coding-agent environment signal from the Host Integration Table is present.
 *
 * <p>The gate guards acts only: the read-only verbs and the console entry run without it, and an
 * act calls {@link #verify()} before it sends anything. The refusal comes from the catalogue and
 * names the marker that was set, so the operator learns what refused the act and where to run it.
 */
public final class OperatorGate {

    /** Exit code of a verb refused by the operator checks. */
    public static final int EXIT_OPERATOR_CHECKS = 4;

    /** The catalogue key of the refusal when a coding-agent marker is set. */
    public static final String AGENT_REFUSED_KEY = "operator.gate.agent.refused";

    /** The catalogue key of the refusal when no interactive terminal is attached. */
    public static final String TERMINAL_REFUSED_KEY = "operator.gate.terminal.refused";

    /** The catalogue parameter naming the marker that was set. */
    public static final String MARKER_PARAM = "marker";

    private static final String CODEX = "codex";

    /** The hosts in the normative detection precedence. */
    private static final List<String> HOST_PRECEDENCE = List.of("antigravity", "opencode", "claude", CODEX);

    /** The marker of a coding-agent environment that names no host. */
    private static final String UNMAPPED_MARKER = "AI_AGENT";

    /** The prefix of every Codex variable: any of them marks the Codex CLI. */
    private static final String CODEX_PREFIX = "CODEX_";

    /** The signals the gate knows beside the Host Integration Table, per client id. */
    private static final Map<String, List<String>> EXTRA_SIGNALS = Map.of(
                        "antigravity", List.of("GEMINI_CLI", "ANTIGRAVITY_AGENT"),
                        "opencode", List.of("OPENCODE_CLIENT", "OPENCODE", "OPENCODE_PID"),
                        "claude", List.of("CLAUDE_CODE", "CLAUDE_PROJECT_ROOT", "CLAUDECODE", "CLAUDE_CODE_SESSION_ID"),
                        CODEX, List.of());

    /** One detected coding-agent environment: the host it belongs to and the marker that was set. */
    private record Marker(String host, String variable) {
    }

    private final OperatorContext context;

    /**
     * @param context the operator context
     */
    public OperatorGate(OperatorContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    /**
     * Verifies the operator preconditions of an act.
     *
     * @throws OperatorSecurityException if an agent environment is detected or no interactive terminal is attached
     */
    public void verify() throws OperatorSecurityException {
        var marker = detectMarker(context.environment());
        if (marker != null) {
            throw new OperatorSecurityException(
                                MessageCatalogue.format(context.clientEnvironment().locale(), AGENT_REFUSED_KEY,
                                                    Map.of(MARKER_PARAM, marker.variable())));
        }

        if (!context.isInteractiveTerminal()) {
            throw new OperatorSecurityException(
                                MessageCatalogue.format(context.clientEnvironment().locale(), TERMINAL_REFUSED_KEY,
                                                    Map.of()));
        }
    }

    /**
     * Tells whether an act may run, for a caller that keeps going without it: the console's entry
     * flow offers the web app and a pairing code, which are acts, and a refused act leaves the
     * console a read-only view instead of closing it.
     *
     * @return {@code true} when {@link #verify()} would pass
     */
    public boolean permits() {
        try {
            verify();
            return true;
        } catch (OperatorSecurityException _) {
            return false;
        }
    }

    /**
     * Verifies the gate for a caller whose contract is an {@link java.io.IOException}: the
     * full-screen views and their dialogues send their acts through such a seam, and they show the
     * refusal the way they show any other refusal of the runtime.
     *
     * @throws OperatorRefusedException carrying the catalogue text, when the act is refused
     */
    public void verifyAct() throws OperatorRefusedException {
        try {
            verify();
        } catch (OperatorSecurityException e) {
            throw new OperatorRefusedException(e.getMessage());
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
        var marker = detectMarker(environment);
        return marker == null ? Optional.empty() : Optional.of(marker.host());
    }

    /**
     * Names the first coding-agent marker of an environment, in the normative precedence.
     *
     * @param environment the process environment
     * @return the marker, or {@code null} when the environment carries none
     */
    private static @Nullable Marker detectMarker(Map<String, String> environment) {
        for (var host : hostsInPrecedence()) {
            var variable = matchedSignal(host, environment);
            if (variable != null) {
                return new Marker(host, variable);
            }
        }

        if (HostDetection.agentSignalPresent(environment)) {
            return new Marker(HostDetection.UNKNOWN, UNMAPPED_MARKER);
        }

        return null;
    }

    /**
     * @return the client ids of the Host Integration Table in the normative precedence, then any
     *         host a later profile adds after it
     */
    private static List<String> hostsInPrecedence() {
        var hosts = new ArrayList<>(HOST_PRECEDENCE);
        for (var host : HostDetection.HOSTS) {
            if (!hosts.contains(host.client())) {
                hosts.add(host.client());
            }
        }
        return hosts;
    }

    /**
     * @param hostName    the client id of the Host Integration Table
     * @param environment the process environment
     * @return the variable of the first matching signal of that host, or {@code null}
     */
    private static @Nullable String matchedSignal(String hostName, Map<String, String> environment) {
        for (var signal : signalsOf(hostName)) {
            if (signal.matches(environment)) {
                return signal.variable();
            }
        }
        if (CODEX.equals(hostName)) {
            return environment.keySet().stream().filter(key -> key.startsWith(CODEX_PREFIX)).sorted().findFirst()
                                .orElse(null);
        }
        return null;
    }

    /**
     * @param hostName the client id of the Host Integration Table
     * @return its signals, primary first, then the ones the table does not carry
     */
    private static List<HostDetection.Signal> signalsOf(String hostName) {
        var signals = new ArrayList<HostDetection.Signal>();
        HostDetection.host(hostName).ifPresent(host -> {
            signals.addAll(host.primarySignals());
            signals.addAll(host.secondarySignals());
        });
        for (var variable : EXTRA_SIGNALS.getOrDefault(hostName, List.of())) {
            signals.add(HostDetection.Signal.present(variable));
        }
        return signals;
    }
}
