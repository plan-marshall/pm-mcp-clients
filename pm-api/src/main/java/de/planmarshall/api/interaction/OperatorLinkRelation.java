/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.interaction;

import java.util.Arrays;
import java.util.List;

/**
 * The closed set of link relations an {@link OperatorLink} of the local API draws from, shared with
 * the model-facing links: the nine relation identifiers of the Hypermedia Format specification
 * (Closed Link Relations). An operator link's {@code rel} is one of these, never a relation of its own.
 * <p>
 * Waiting on a job and cancelling it are engine events, never relations; no model-facing link reaches
 * a client of the operator and no operator link reaches a worker. The list is closed and fails closed:
 * a relation read from the wire that is not a member is refused by {@link #fromWire(String)}.
 */
public enum OperatorLinkRelation {

    /** Default forward transition along the primary workflow path (for example a consent's {@code yes}). */
    NEXT("next"),

    /** Alternative forward transition (for example the {@code no} of a consent, a hold's alternative). */
    ALT("alt"),

    /** Repeat the last failed deterministic operation without changing code. */
    RETRY("retry"),

    /** Choose an in-branch fix for a failed build, check, or gate finding. */
    FIX("fix"),

    /** Return from {@code finalize} to {@code execute}, or re-author the outline. */
    LOOP_BACK("loop_back"),

    /** Deliver the operator's answer or choice to the server. */
    SUBMIT("submit"),

    /** File a request for operator action (a waiver request). */
    ESCALATE("escalate"),

    /** Ask another role through the server. */
    CONSULT("consult"),

    /** Leave a sub-workflow through the implicit {@code step_out}. */
    ABORT("abort");

    private final String wire;

    OperatorLinkRelation(String wire) {
        this.wire = wire;
    }

    /**
     * @return the wire representation of this relation
     */
    public String wire() {
        return wire;
    }

    /**
     * Resolves a wire value to its relation.
     *
     * @param wire the wire representation
     * @return the matching relation, or {@code null} if none matches
     */
    public static OperatorLinkRelation fromWireOrNull(String wire) {
        if (wire == null) {
            return null;
        }
        for (var relation : values()) {
            if (relation.wire.equals(wire)) {
                return relation;
            }
        }
        return null;
    }

    /**
     * Resolves a wire value to its relation.
     *
     * @param wire the wire representation
     * @return the matching relation
     * @throws IllegalArgumentException if no relation matches (fail-closed)
     */
    public static OperatorLinkRelation fromWire(String wire) {
        var relation = fromWireOrNull(wire);
        if (relation == null) {
            throw new IllegalArgumentException("Unknown operator link relation: " + wire);
        }
        return relation;
    }

    /**
     * @return the wire representations of every relation, in catalogue order
     */
    public static List<String> wireValues() {
        return Arrays.stream(values()).map(OperatorLinkRelation::wire).toList();
    }
}
