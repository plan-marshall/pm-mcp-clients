/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.chat;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * The closed list of the kinds of change a chat's role proposes, as the shared texts name them
 * ({@code control.chat.proposal.kind}; design {@code work-actions-chat-texts.adoc#texts}).
 */
public enum ProposalKind {

    /** A rewrite of, or an addition to, a start's description. */
    DESCRIPTION("description"),

    /** A revision of the plan. */
    REVISION("revision"),

    /** A change of an epic's staged rows. */
    ROWS("rows"),

    /** A change of the decomposition's draft. */
    DRAFT("draft"),

    /** A disposition or rewrite of a register item. */
    ITEM("item"),

    /** An answer to the decision, offered as the operator's own words. */
    ANSWER("answer"),

    /** A follow-up plan of ended work. */
    FOLLOW_UP("follow_up");

    private final String wireValue;

    ProposalKind(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * @return the value used on the wire
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * @param value the wire value, may be {@code null}
     * @return the kind, or {@code null} when the value names none
     */
    public static @Nullable ProposalKind fromWireOrNull(@Nullable String value) {
        if (value == null) {
            return null;
        }
        for (var kind : values()) {
            if (kind.wireValue.equals(value)) {
                return kind;
            }
        }
        return null;
    }

    /**
     * @param value the wire value
     * @return the kind
     * @throws IllegalArgumentException if the value names no kind (fail-closed)
     */
    public static ProposalKind fromWire(String value) {
        Objects.requireNonNull(value, "value");
        var kind = fromWireOrNull(value);
        if (kind == null) {
            throw new IllegalArgumentException("Unknown proposal kind: " + value);
        }
        return kind;
    }

    /**
     * @return the wire values of the closed list, in declaration order
     */
    public static List<String> wireValues() {
        return Arrays.stream(values()).map(ProposalKind::wireValue).toList();
    }
}
