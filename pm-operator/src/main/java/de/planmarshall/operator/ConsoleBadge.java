/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A structured prompt badge for the operator console (04-console.adoc#prompt).
 *
 * <p>Carries its kind and raw count or entity ID so question scans and verb decisions
 * read facts as structured data instead of scraping localized rendered strings.
 *
 * @param kind      the badge kind
 * @param text      the rendered badge text
 * @param itemKind  the notice or needs_operator kind (for {@link Kind#WAITING})
 * @param count     the waiting item count (for {@link Kind#WAITING})
 * @param projectId the project name or directory (for {@link Kind#PROJECT_SETUP})
 */
public record ConsoleBadge(
        Kind kind,
        String text,
        @Nullable String itemKind,
        int count,
        @Nullable String projectId) {

    public enum Kind {
        MACHINE_SETUP,
        PROJECT_SETUP,
        WAITING
    }

    public ConsoleBadge {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(text, "text");
    }

    public static ConsoleBadge machineSetup(String text) {
        return new ConsoleBadge(Kind.MACHINE_SETUP, text, null, 0, null);
    }

    public static ConsoleBadge projectSetup(String projectId, String text) {
        return new ConsoleBadge(Kind.PROJECT_SETUP, text, null, 0, projectId);
    }

    public static ConsoleBadge waiting(String itemKind, int count, String text) {
        return new ConsoleBadge(Kind.WAITING, text, itemKind, count, null);
    }

    /**
     * @return {@code true} if this badge represents an open question, decision, or waiver request
     */
    public boolean isQuestion() {
        return kind == Kind.WAITING && ("question".equals(itemKind)
                || "decision".equals(itemKind)
                || "waiver_request".equals(itemKind));
    }
}
