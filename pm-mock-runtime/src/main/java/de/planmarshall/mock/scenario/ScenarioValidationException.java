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

import java.util.Objects;

/**
 * Thrown when a scenario file violates the scenario format specification.
 */
public final class ScenarioValidationException extends RuntimeException {

    private final String scenario;
    private final String state;
    private final String field;

    /**
     * @param scenario the scenario name or file path
     * @param state    the state name, or {@code null} if file-level
     * @param field    the field name, or {@code null}
     * @param message  the violation description
     */
    public ScenarioValidationException(String scenario, String state, String field, String message) {
        super(formatMessage(scenario, state, field, message));
        this.scenario = Objects.requireNonNull(scenario, "scenario");
        this.state = state;
        this.field = field;
    }

    private static String formatMessage(String scenario, String state, String field, String message) {
        var sb = new StringBuilder("Scenario '").append(scenario).append("'");
        if (state != null) {
            sb.append(", state '").append(state).append("'");
        }
        if (field != null) {
            sb.append(", field '").append(field).append("'");
        }
        sb.append(": ").append(message);
        return sb.toString();
    }

    /** @return the scenario name or file path */
    public String scenario() {
        return scenario;
    }

    /** @return the state name, or {@code null} if file-level */
    public String state() {
        return state;
    }

    /** @return the field name, or {@code null} */
    public String field() {
        return field;
    }
}
