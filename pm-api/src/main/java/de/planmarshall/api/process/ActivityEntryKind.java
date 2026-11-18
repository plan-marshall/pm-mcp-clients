/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.process;

import java.util.Objects;

/**
 * Kind of entry in a worker's live activity log (UC-PLN-1).
 * <p>
 * {@link #MODEL_DIFFERS} is the server's own entry: a turn reported a model other than the
 * worker's launch model, and the entry names both
 * ({@code harness-profiles/01-profile-schema.adoc#served-model}).
 */
public enum ActivityEntryKind {
    MODEL_TEXT("text"),
    TOOL_CALL("tool_call"),
    FILES_CHANGED("file_changed"),
    QUESTIONS_ASKED("question"),
    ANSWER_ACK("answer_ack"),
    LINK("link"),
    MODEL_DIFFERS("model_differs");

    private final String value;

    ActivityEntryKind(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static ActivityEntryKind fromValue(String value) {
        Objects.requireNonNull(value, "value");
        if ("tool".equalsIgnoreCase(value)) {
            return TOOL_CALL;
        }
        if ("wrote".equalsIgnoreCase(value)) {
            return MODEL_TEXT;
        }
        if ("changed".equalsIgnoreCase(value)) {
            return FILES_CHANGED;
        }
        if ("asked".equalsIgnoreCase(value)) {
            return QUESTIONS_ASKED;
        }
        for (var k : values()) {
            if (k.value.equalsIgnoreCase(value) || k.name().equalsIgnoreCase(value)) {
                return k;
            }
        }
        return MODEL_TEXT;
    }
}
