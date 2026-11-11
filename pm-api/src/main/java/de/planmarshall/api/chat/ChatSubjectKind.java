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
 * The closed list of the subjects a chat's routes are addressed below
 * ({@code 01-processes-and-transport.adoc#chat-routes}). Ended work is a plan or an epic and is
 * addressed like the running one.
 */
public enum ChatSubjectKind {

    /** A plan, running, pausing, paused, or ended. */
    PLAN("plan"),

    /** An epic with its decomposition and its review, running or ended. */
    EPIC("epic"),

    /** A start's entry. */
    ENTRY("entry"),

    /** A decision below its scope. */
    DECISION("decision");

    private final String wireValue;

    ChatSubjectKind(String wireValue) {
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
     * @return the subject kind, or {@code null} when the value names none
     */
    public static @Nullable ChatSubjectKind fromWireOrNull(@Nullable String value) {
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
     * @return the subject kind
     * @throws IllegalArgumentException if the value names no subject kind (fail-closed)
     */
    public static ChatSubjectKind fromWire(String value) {
        Objects.requireNonNull(value, "value");
        var kind = fromWireOrNull(value);
        if (kind == null) {
            throw new IllegalArgumentException("Unknown chat subject kind: " + value);
        }
        return kind;
    }

    /**
     * @return the wire values of the closed list, in declaration order
     */
    public static List<String> wireValues() {
        return Arrays.stream(values()).map(ChatSubjectKind::wireValue).toList();
    }
}
