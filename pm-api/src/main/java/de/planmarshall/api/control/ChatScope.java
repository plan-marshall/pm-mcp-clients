/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.control;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

/**
 * Closed enumeration of what a chat is about (design <em>Chatting with the Planning Role</em> and
 * <em>The Same Chat in Three More Places</em>).
 *
 * <p>The same chat is opened from a plan's actions (the whole plan), from an epic's actions (the
 * whole epic), from the decomposition (the draft), and from a review (one register item or the
 * whole register); an ended plan or epic opens it about its archived record, and the request while
 * starting about the entry.
 */
public enum ChatScope {
    PLAN("plan"),
    EPIC("epic"),
    DECOMPOSITION("decomposition"),
    REGISTER_ITEM("register_item"),
    REGISTER("register"),
    ARCHIVE("archive"),
    REQUEST("request");

    private final String value;

    ChatScope(String value) {
        this.value = value;
    }

    /**
     * @return the wire string value
     */
    public String value() {
        return value;
    }

    /**
     * Resolves an enum constant from its wire string value.
     *
     * @param value wire string
     * @return enum constant, or {@code null} if unmatched
     */
    public static @Nullable ChatScope fromValue(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        var clean = value.trim().toLowerCase(Locale.ROOT);
        for (ChatScope scope : values()) {
            if (scope.value.equals(clean) || scope.name().equalsIgnoreCase(clean)) {
                return scope;
            }
        }
        return null;
    }

    /**
     * Resolves an enum constant or throws.
     *
     * @param value wire string
     * @return enum constant
     */
    public static ChatScope parse(String value) {
        var scope = fromValue(value);
        if (scope == null) {
            throw new IllegalArgumentException("Unknown ChatScope: " + value);
        }
        return scope;
    }
}
