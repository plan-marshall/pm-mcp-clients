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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Formats the terminal client prompt: {@code pm › } when normal,
 * {@code pm [<badges>] › } when items wait for the operator or onboarding is in progress, and
 * {@code pm (reconnecting) › } when reconnecting to the runtime event stream.
 */
public final class ConsolePrompt {

    static final String PROMPT_BASE = "pm";
    static final String PROMPT_SUFFIX = " › ";
    static final String PROMPT_SEPARATOR = " · ";
    static final String RECONNECTING_PROMPT = "pm (reconnecting) › ";

    private ConsolePrompt() {
    }

    /**
     * Formats the prompt with given badges and reconnection status.
     *
     * @param badges       the badges to display, in order
     * @param reconnecting {@code true} if reconnecting to the runtime
     * @return the formatted prompt string
     */
    public static String format(List<String> badges, boolean reconnecting) {
        if (reconnecting) {
            return RECONNECTING_PROMPT;
        }
        if (badges == null || badges.isEmpty()) {
            return PROMPT_BASE + PROMPT_SUFFIX;
        }
        var effective = new ArrayList<String>();
        if (badges.size() <= 3) {
            effective.addAll(badges);
        } else {
            effective.add(badges.get(0));
            effective.add(badges.get(1));
            effective.add(badges.get(2));
            effective.add("+" + (badges.size() - 3));
        }
        return PROMPT_BASE + " [" + String.join(PROMPT_SEPARATOR, effective) + "]" + PROMPT_SUFFIX;
    }

    /**
     * Formats a badge for a waiting item count according to 04-console.adoc.
     *
     * @param kind  the item kind
     * @param count the waiting count
     * @return the formatted badge text, or {@code null} if count &lt;= 0
     */
    public static @Nullable String formatBadge(String kind, int count) {
        Objects.requireNonNull(kind, "kind");
        if (count <= 0) {
            return null;
        }
        return switch (kind) {
            case "decision", "question" -> count == 1 ? "1 question" : count + " questions";
            case "waiver_request" -> count == 1 ? "1 waiver request" : count + " waiver requests";
            case "operator_wait" -> count == 1 ? "1 waits for you" : count + " wait for you";
            case "integrity_pause" -> count == 1 ? "1 integrity pause" : count + " integrity pauses";
            case "stalled" -> count + " stalled";
            case "condition" -> count + " needs a fix";
            case "held" -> count + " held";
            case "landing_failed" -> count + " landing failed";
            case "findings" -> count + " new findings";
            case "setup" -> count + " setup";
            default -> count + " " + kind;
        };
    }

    /**
     * Formats a badge for machine onboarding step.
     *
     * @param currentStep 1-based current step (or 0 if not started)
     * @param totalSteps  total steps (7)
     * @return formatted setup badge e.g. "setup 3/7"
     */
    public static String formatMachineSetupBadge(int currentStep, int totalSteps) {
        return "setup " + currentStep + "/" + totalSteps;
    }

    /**
     * Formats a badge for project onboarding step.
     *
     * @param projectId   project ID or checkout directory name
     * @param currentStep 1-based current step
     * @param totalSteps  total steps
     * @return formatted project setup badge e.g. "setup pm-mcp-clients 5/9"
     */
    public static String formatProjectSetupBadge(String projectId, int currentStep, int totalSteps) {
        return "setup " + Objects.requireNonNull(projectId, "projectId") + " " + currentStep + "/" + totalSteps;
    }
}
