/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.mock.scenario;

import java.util.Map;

/**
 * An immutable scenario definition driving the mock runtime state machine.
 *
 * @param formatVersion the format version (must be 1)
 * @param name          the scenario name
 * @param description   human-readable description
 * @param start         the starting state identifier
 * @param pairingCode   the accepted pairing code on the web listener (default MOCKMOCK)
 * @param routes        static routes mapped from {@code "METHOD /path"}
 * @param states        all states indexed by their state id
 * @param designer      the process-designer fixtures of this scenario (UC-PLN-3), or an empty map
 * @param startCase     the start fixtures of this scenario (UC-STA-1..9), or an empty map
 */
public record Scenario(
int formatVersion,
String name,
String description,
String start,
String pairingCode,
Map<String, RouteDefinition> routes,
Map<String, StateDefinition> states,
Map<String, Object> designer,
Map<String, Object> startCase) {

    /** The supported format version. */
    public static final int SUPPORTED_FORMAT_VERSION = 1;

    /** Default pairing code for web clients. */
    public static final String DEFAULT_PAIRING_CODE = "MOCKMOCK";

    /**
     * Convenience constructor without designer or start fixtures.
     *
     * @param formatVersion the format version (must be 1)
     * @param name          the scenario name
     * @param description   human-readable description
     * @param start         the starting state identifier
     * @param pairingCode   the accepted pairing code on the web listener
     * @param routes        static routes
     * @param states        all states indexed by their state id
     */
    public Scenario(
            int formatVersion,
            String name,
            String description,
            String start,
            String pairingCode,
            Map<String, RouteDefinition> routes,
            Map<String, StateDefinition> states) {
        this(formatVersion, name, description, start, pairingCode, routes, states, Map.of(), Map.of());
    }

    /**
     * Compact constructor validating required properties.
     */
    public Scenario {
        if (name == null) {
            throw new NullPointerException("name");
        }
        if (start == null) {
            throw new NullPointerException("start");
        }
        pairingCode = pairingCode == null || pairingCode.isBlank() ? DEFAULT_PAIRING_CODE : pairingCode;
        routes = routes == null ? Map.of() : Map.copyOf(routes);
        states = states == null ? Map.of() : Map.copyOf(states);
        designer = designer == null ? Map.of() : Map.copyOf(designer);
        startCase = startCase == null ? Map.of() : Map.copyOf(startCase);
    }

    /**
     * @param stateId the state identifier
     * @return the state definition
     */
    public StateDefinition state(String stateId) {
        return states.get(stateId);
    }

    /**
     * @param method the HTTP method
     * @param path   the request path
     * @return the matching route definition, or {@code null}
     */
    public RouteDefinition route(String method, String path) {
        var key = method.toUpperCase() + " " + path;
        return routes.get(key);
    }
}
