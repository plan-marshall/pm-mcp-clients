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
 * Closed enumeration of the planning roles a chat is routed to (design <em>Chatting with the
 * Planning Role</em>).
 *
 * <p>A plan's chat, also about ended work, goes to {@code replan}; an epic's chat, also in its
 * decomposition and its review, to {@code decompose}. The chat about a request while starting is
 * routed by the start contract and only named here.
 */
public enum ChatRole {
    REPLAN("replan"),
    DECOMPOSE("decompose"),
    REFINE("refine");

    private final String value;

    ChatRole(String value) {
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
    public static @Nullable ChatRole fromValue(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        var clean = value.trim().toLowerCase(Locale.ROOT);
        for (ChatRole role : values()) {
            if (role.value.equals(clean) || role.name().equalsIgnoreCase(clean)) {
                return role;
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
    public static ChatRole parse(String value) {
        var role = fromValue(value);
        if (role == null) {
            throw new IllegalArgumentException("Unknown ChatRole: " + value);
        }
        return role;
    }
}
