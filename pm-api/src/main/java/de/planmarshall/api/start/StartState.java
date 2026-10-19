/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.start;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The closed list of the operator states of a start (State Catalogue, Starting from the Clients,
 * Recipe Choice, and Source Units; design <em>Starting Work</em>).
 * <p>
 * The state ids follow the operator-interaction scheme: the id is the wire value, and the
 * representation's {@code state.state_id} is that id. The list is closed and fails closed: a state
 * read from the wire that is not a member is refused by {@link #fromWire(String)}.
 */
public enum StartState {

    /** A plan's entry chooses between the sources and a recipe ({@code start.choose}). */
    CHOOSE("start.choose"),

    /** A plan's entry gathers its sources ({@code start.sources}). */
    SOURCES("start.sources"),

    /** A plan's or an epic's entry is ready to create its work ({@code start.ready}). */
    READY("start.ready"),

    /** A plan's entry chooses a recipe ({@code start.recipe}). */
    RECIPE("start.recipe"),

    /** A recipe's plan is being created ({@code start.recipe-create}). */
    RECIPE_CREATE("start.recipe-create"),

    /** The setup gate refused the start ({@code start.refused}). */
    REFUSED("start.refused"),

    /** An epic's entry offers an adoptable ledger ({@code start-epic.adopt}). */
    EPIC_ADOPT("start-epic.adopt"),

    /** An epic's ledger cannot be adopted ({@code start-epic.adopt-failed}). */
    EPIC_ADOPT_FAILED("start-epic.adopt-failed"),

    /** A recipe is chosen from the offered ones ({@code recipe.choose}). */
    RECIPE_CHOOSE("recipe.choose"),

    /** A recipe's parameters are resolved and proposed ({@code recipe.parameters}). */
    RECIPE_PARAMETERS("recipe.parameters"),

    /** A recipe's plan is created ({@code recipe.create}). */
    RECIPE_STEP_CREATE("recipe.create");

    private final String wireValue;

    StartState(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * @return the state id and wire value
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * @return the state id the operator representation carries
     */
    public String stateId() {
        return wireValue;
    }

    /**
     * Resolves a state from its wire value or state id, ignoring case.
     *
     * @param value the wire value
     * @return the matching state, or {@code null} if none matches
     */
    public static StartState fromWireOrNull(String value) {
        if (value == null) {
            return null;
        }
        var clean = value.trim();
        return Arrays.stream(values())
                .filter(state -> state.wireValue.equalsIgnoreCase(clean))
                .findFirst()
                .orElse(null);
    }

    /**
     * Resolves a state from its wire value or state id, ignoring case.
     *
     * @param value the wire value
     * @return the matching state
     * @throws IllegalArgumentException if no state carries that value (fail-closed)
     */
    public static StartState fromWire(String value) {
        var state = fromWireOrNull(value);
        if (state == null) {
            throw new IllegalArgumentException("Unknown start state: " + value);
        }
        return state;
    }

    /**
     * @return the wire values of every state, in catalogue order
     */
    public static List<String> wireValues() {
        return new ArrayList<>(Arrays.stream(values()).map(StartState::wireValue).toList());
    }
}
