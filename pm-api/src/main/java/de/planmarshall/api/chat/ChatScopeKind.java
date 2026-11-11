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
 * The closed list of a chat's scope as the shared texts name it ({@code control.chat.scope},
 * {@code control.chat.starter.<scope>}; design {@code work-actions-chat-texts.adoc#texts}).
 */
public enum ChatScopeKind {

    /** The whole plan. */
    PLAN("plan"),

    /** The whole epic. */
    EPIC("epic"),

    /** The decomposition's draft. */
    DRAFT("draft"),

    /** One item of an epic's register. */
    ITEM("item"),

    /** A decision. */
    DECISION("decision"),

    /** A start's entry, the request. */
    ENTRY("entry"),

    /** The archived record of ended work. */
    ARCHIVE("archive"),

    /** The whole register of an epic's review. */
    REGISTER("register");

    private final String wireValue;

    ChatScopeKind(String wireValue) {
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
     * @return the scope, or {@code null} when the value names none
     */
    public static @Nullable ChatScopeKind fromWireOrNull(@Nullable String value) {
        if (value == null) {
            return null;
        }
        for (var scope : values()) {
            if (scope.wireValue.equals(value)) {
                return scope;
            }
        }
        return null;
    }

    /**
     * @param value the wire value
     * @return the scope
     * @throws IllegalArgumentException if the value names no scope (fail-closed)
     */
    public static ChatScopeKind fromWire(String value) {
        Objects.requireNonNull(value, "value");
        var scope = fromWireOrNull(value);
        if (scope == null) {
            throw new IllegalArgumentException("Unknown chat scope: " + value);
        }
        return scope;
    }

    /**
     * @return the wire values of the closed list, in declaration order
     */
    public static List<String> wireValues() {
        return Arrays.stream(values()).map(ChatScopeKind::wireValue).toList();
    }
}
