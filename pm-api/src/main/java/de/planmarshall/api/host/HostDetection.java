/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.host;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import lombok.experimental.UtilityClass;

/**
 * The host-detection table (Client Profiles Specification, Host-Detection Table): per host its
 * environment signals in detection precedence, its session ID variable, and its version probe. The
 * table holds no host by name; it is read from the {@code detection} and {@code identity} objects of
 * the shipped profile documents ({@link ProfileDocumentReader}), which are the single source of
 * these values. Profile overrides never touch it.
 * <p>
 * Most values are <em>provisional</em> until they are verified against a live host (verification gate
 * 14); each row records whether it carries a provisional value.
 */
@UtilityClass
public class HostDetection {

    /** The client id of the neutral fallback profile, not a host of the table. */
    public static final String NEUTRAL = "neutral";

    /** The detected client when no signal matched. */
    public static final String UNKNOWN = "unknown";

    /**
     * One environment signal.
     *
     * @param variable      the environment variable
     * @param requiredValue the value it must have, {@code null} if presence suffices
     */
    public record Signal(String variable, String requiredValue) {

        /**
         * @param variable      the variable
         * @param requiredValue the required value
         */
        public Signal {
            Objects.requireNonNull(variable, "variable");
        }

        /**
         * @param variable the variable
         * @return a signal matched by the variable's presence
         */
        public static Signal present(String variable) {
            return new Signal(variable, null);
        }

        /**
         * @param environment an environment
         * @return {@code true} if the signal is present in it
         */
        public boolean matches(Map<String, String> environment) {
            var value = environment.get(variable);
            return value != null && (requiredValue == null || requiredValue.equals(value));
        }
    }

    /**
     * One host of the table.
     *
     * @param client            the client id
     * @param primarySignals    the primary detection signals, any one matches
     * @param secondarySignals  signals consulted only when no primary signal of any host matched
     * @param sessionIdVariable the variable holding the host session identifier, {@code null} if none
     * @param versionProbe      the argument vector printing the host build, empty if none
     * @param versionPattern    the pattern over the probe's output, {@code null} without a probe
     * @param modelSource       where the model identity comes from
     * @param provisional       {@code true} if any value of the row is provisional
     */
    public record Host(String client, List<Signal> primarySignals, List<Signal> secondarySignals,
                       String sessionIdVariable, List<String> versionProbe, String versionPattern, String modelSource,
                       boolean provisional) {

        /**
         * Copies the lists.
         *
         * @param client            the client id
         * @param primarySignals    the primary signals
         * @param secondarySignals  the secondary signals
         * @param sessionIdVariable the session ID variable
         * @param versionProbe      the version probe
         * @param versionPattern    the version pattern
         * @param modelSource       the model source
         * @param provisional       whether a value is provisional
         */
        public Host {
            Objects.requireNonNull(client, "client");
            primarySignals = List.copyOf(primarySignals);
            secondarySignals = List.copyOf(secondarySignals);
            versionProbe = List.copyOf(versionProbe);
        }

        /**
         * @param environment an environment
         * @return the host session identifier, if the host provides one
         */
        public Optional<String> sessionId(Map<String, String> environment) {
            return sessionIdVariable == null ? Optional.empty()
                    : Optional.ofNullable(environment.get(sessionIdVariable)).filter(id -> !id.isBlank());
        }
    }

    /**
     * The hosts in detection precedence, read from the shipped profile documents. The neutral
     * fallback profile is not a host: it carries no signal and is never detected.
     */
    public static final List<Host> HOSTS = loadHosts();

    /**
     * Secondary agent signals not yet mapped to a host (provisional; the mapping is a host fact
     * still to verify). They mark a coding-agent environment but name no client.
     */
    public static final List<Signal> UNMAPPED_AGENT_SIGNALS = List.of(Signal.present("AI_AGENT"));

    /**
     * Detects the host from an environment: primary signals in the fixed precedence, then the
     * secondary signals; the first match wins.
     *
     * @param environment the environment
     * @return the detected client id, or {@link #UNKNOWN}
     */
    public static String detect(Map<String, String> environment) {
        for (var host : HOSTS) {
            if (host.primarySignals().stream().anyMatch(signal -> signal.matches(environment))) {
                return host.client();
            }
        }
        for (var host : HOSTS) {
            if (host.secondarySignals().stream().anyMatch(signal -> signal.matches(environment))) {
                return host.client();
            }
        }
        return UNKNOWN;
    }

    /**
     * @param environment the environment
     * @return {@code true} if any coding-agent signal, mapped or not, is present
     */
    public static boolean agentSignalPresent(Map<String, String> environment) {
        return !UNKNOWN.equals(detect(environment))
                || UNMAPPED_AGENT_SIGNALS.stream().anyMatch(signal -> signal.matches(environment));
    }

    /**
     * @param client a client id
     * @return its table row; empty for {@link #NEUTRAL} and unknown ids
     */
    public static Optional<Host> host(String client) {
        return HOSTS.stream().filter(host -> host.client().equals(client)).findFirst();
    }

    /** @return every client id a relay may declare, {@link #NEUTRAL} last */
    public static List<String> clientIds() {
        var ids = new ArrayList<String>();
        HOSTS.forEach(host -> ids.add(host.client()));
        ids.add(NEUTRAL);
        return List.copyOf(ids);
    }

    private static List<Host> loadHosts() {
        return ProfileDocumentReader.readShippedProfiles().stream().filter(profile -> !NEUTRAL.equals(profile.clientId()))
                .map(HostDetection::toHost).toList();
    }

    private static Host toHost(ProfileDocumentReader.Profile profile) {
        var identity = profile.identity();
        return new Host(profile.clientId(), profile.detection().primarySignals(), profile.detection().secondarySignals(),
                identity.sessionIdVariable(), identity.versionProbe() == null ? List.of() : identity.versionProbe(),
                identity.versionPattern(), identity.modelSource(), identity.provisional());
    }
}
