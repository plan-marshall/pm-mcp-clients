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
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.overview.NeedsOperatorKind;

/**
 * Formats the terminal client prompt: {@code pm › } when normal,
 * {@code pm [<badges>] › } when items wait for the operator or onboarding is in progress, and
 * {@code pm (reconnecting) › } when reconnecting to the runtime event stream.
 *
 * <p>The prompt is one of the few texts that is the same in every language; the badges are not
 * (04-console.adoc#prompt) and come from the message catalogue, each with its plural.
 */
public final class ConsolePrompt {

    static final String PROMPT_BASE = "pm";
    static final String PROMPT_SUFFIX = " \u203a ";
    static final String PROMPT_SEPARATOR = " \u00b7 ";
    private static final String DEFAULT_LOCALE = "en";
    private static final String BADGE_QUESTIONS = "operator.console.badge.questions";
    private static final String BADGE_WAIVERS = "operator.console.badge.waivers";
    static final String BADGE_FALLBACK = "operator.console.badge.fallback";
    private static final String QUESTION_WORD = "question";
    private static final String COUNT = "count";

    private ConsolePrompt() {
    }

    /**
     * Formats the prompt with given badges and reconnection status.
     *
     * @param badges       the badges to display, in order
     * @param reconnecting {@code true} if reconnecting to the runtime
     * @return the formatted prompt string
     */
    public static String format(@Nullable List<String> badges, boolean reconnecting) {
        return format(DEFAULT_LOCALE, badges, reconnecting);
    }

    /**
     * Formats the prompt with given badges and reconnection status.
     *
     * @param locale       the interface locale
     * @param badges       the badges to display, in order
     * @param reconnecting {@code true} if reconnecting to the runtime
     * @return the formatted prompt string
     */
    public static String format(String locale, @Nullable List<String> badges, boolean reconnecting) {
        if (reconnecting) {
            return PROMPT_BASE + " (" + MessageCatalogue.format(locale, "operator.console.badge.reconnecting", Map.of())
                                + ")" + PROMPT_SUFFIX;
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
            effective.add(MessageCatalogue.format(locale, "operator.console.badge.more",
                                Map.of(COUNT, badges.size() - 3)));
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
        return formatBadge(DEFAULT_LOCALE, kind, count);
    }

    /**
     * Formats a badge for a waiting item count according to 04-console.adoc.
     *
     * @param locale the interface locale
     * @param kind   the item kind
     * @param count  the waiting count
     * @return the formatted badge text, or {@code null} if count &lt;= 0
     */
    public static @Nullable String formatBadge(String locale, String kind, int count) {
        Objects.requireNonNull(kind, "kind");
        if (count <= 0) {
            return null;
        }
        return MessageCatalogue.format(locale, badgeKey(kind),
                            Map.of(COUNT, count, "kind", kindWord(locale, kind)));
    }

    /**
     * The catalogue key of the badge of a kind: every kind of the contract has its own badge. The
     * word is resolved to the contract's kind first, so the badge reads every word
     * {@link NeedsOperatorKind#fromValue} reads, and in addition the console's word {@code question}
     * for a decision; a word this version does not know reads the fallback badge,
     * which names the kind by its shared word.
     *
     * @param kind the item kind
     * @return the badge key of the kind, or {@link #BADGE_FALLBACK} for a kind this version does not know
     */
    static String badgeKey(String kind) {
        return switch (badgeKind(kind)) {
            case DECISION -> BADGE_QUESTIONS;
            case WAIVER -> BADGE_WAIVERS;
            case WAITS_FOR_YOU -> "operator.console.badge.operator_waits";
            case INTEGRITY_PAUSE -> "operator.console.badge.integrity_pauses";
            case STALLED -> "operator.console.badge.stalled";
            case CONDITION -> "operator.console.badge.conditions";
            case HELD -> "operator.console.badge.held";
            case LANDING_FAILED -> "operator.console.badge.landing_failed";
            case FINDINGS -> "operator.console.badge.findings";
            case SETUP -> "operator.console.badge.setup_needs";
            case CREDENTIAL -> "operator.console.badge.credential";
            case CLEANUP_DUE -> "operator.console.badge.cleanup_due";
            case OTHER -> BADGE_FALLBACK;
        };
    }

    /**
     * The kind a badge is selected by: the console word {@code question} is not a word of the
     * contract and is read as a {@link NeedsOperatorKind#DECISION} here; every other word is
     * resolved by {@link NeedsOperatorKind#fromWireOrOther}.
     *
     * @param kind the item kind
     * @return the resolved kind, {@link NeedsOperatorKind#OTHER} for a word this version does not know
     */
    private static NeedsOperatorKind badgeKind(String kind) {
        if (QUESTION_WORD.equalsIgnoreCase(kind.trim())) {
            return NeedsOperatorKind.DECISION;
        }
        return NeedsOperatorKind.fromWireOrOther(kind);
    }

    /**
     * The word a fallback badge names its kind by: the runtime's word is resolved to the contract's
     * kind first ({@link NeedsOperatorKind#fromWireOrOther}, which reads the aliases and answers
     * {@code other} for a word this version does not know), and the word is the catalogue's text of
     * that kind (shared/texts-and-languages.adoc). The key is built from the resolved kind only, so a
     * runtime word that happens to name another key of the pack ({@code title}, {@code count}) reads
     * as another reason, never as that key's text.
     *
     * @param locale the interface locale
     * @param kind   the item kind
     * @return the shared word of the resolved kind ({@code overview.needs.<kind>}), or the word of a
     *         reason this version does not know ({@code common.unknown}) — never the wire word
     */
    static String kindWord(String locale, @Nullable String kind) {
        if (kind == null || kind.isBlank()) {
            return MessageCatalogue.format(locale, "common.unknown", Map.of());
        }
        var resolved = NeedsOperatorKind.fromWireOrOther(kind);
        return MessageCatalogue.format(locale, "overview.needs." + resolved.value(), Map.of());
    }

    /**
     * Formats a badge for machine onboarding step.
     *
     * @param currentStep 1-based current step (or 0 if not started)
     * @param totalSteps  total steps (7)
     * @return formatted setup badge e.g. "setup 3/7"
     */
    public static String formatMachineSetupBadge(int currentStep, int totalSteps) {
        return formatMachineSetupBadge(DEFAULT_LOCALE, currentStep, totalSteps);
    }

    /**
     * Formats a badge for machine onboarding step.
     *
     * @param locale      the interface locale
     * @param currentStep 1-based current step (or 0 if not started)
     * @param totalSteps  total steps (7)
     * @return formatted setup badge e.g. "setup 3/7"
     */
    public static String formatMachineSetupBadge(String locale, int currentStep, int totalSteps) {
        return MessageCatalogue.format(locale, "operator.console.badge.setup",
                            Map.of("done", currentStep, "total", totalSteps));
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
        return formatProjectSetupBadge(DEFAULT_LOCALE, projectId, currentStep, totalSteps);
    }

    /**
     * Formats a badge for project onboarding step.
     *
     * @param locale      the interface locale
     * @param projectId   project ID or checkout directory name
     * @param currentStep 1-based current step
     * @param totalSteps  total steps
     * @return formatted project setup badge e.g. "setup pm-mcp-clients 5/9"
     */
    public static String formatProjectSetupBadge(String locale, String projectId, int currentStep, int totalSteps) {
        // A project's name comes from its checkout's directory: it is untrusted text and reaches
        // the prompt and the window title, so it is sanitised like every other runtime text.
        return MessageCatalogue.format(locale, "operator.console.badge.setup_project",
                            Map.of("project", TerminalSanitizer.sanitizeLine(
                                                                    Objects.requireNonNull(projectId, "projectId")),
                                                "done", currentStep, "total", totalSteps));
    }
}
