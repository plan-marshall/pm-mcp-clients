/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.overview;

import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.display.Display;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.overview.ActiveEpicSummary;
import de.planmarshall.api.overview.ActivePlanSummary;
import de.planmarshall.api.overview.NeedsOperatorItem;
import de.planmarshall.api.overview.NeedsOperatorKind;
import de.planmarshall.api.overview.ProjectSummary;
import de.planmarshall.operator.OutputSafety;

/**
 * Pure formatting and ordering helpers for the full-screen overview
 * ({@code doc/design/shared/plan-overview.adoc}, {@code doc/design/terminal-client/15-overview.adoc}).
 */
public final class OverviewFormatting {

    /** Glyph for a plan row ({@code ▸}). */
    public static final String PLAN_GLYPH = "\u25b8";

    /** Glyph for an epic row ({@code ◇}). */
    public static final String EPIC_GLYPH = "\u25c7";

    private static final String KIND_PAUSED = "paused";
    private static final String ANSWER_PREFIX = "answer ";
    private static final String CMD_PLANS = "plans";

    private OverviewFormatting() {
    }

    /**
     * Sanitizes a single-line cell value via {@link OutputSafety#stripControlSequences(String)}.
     *
     * @param text raw text, may be {@code null}
     * @return sanitized single-line string
     */
    public static String clean(@Nullable String text) {
        return OutputSafety.stripControlSequences(text).replace('\n', ' ').replace('\t', ' ').trim();
    }

    /**
     * Checks whether a project needs operator attention.
     *
     * @param project the project summary
     * @return {@code true} if any plan/epic or project setup needs the operator
     */
    public static boolean needsOperator(ProjectSummary project) {
        Objects.requireNonNull(project, "project");
        return project.needsOperatorCount() > 0 || project.setupNeedsOperator();
    }

    /**
     * Returns the effective operator attention count for a project row.
     *
     * @param project the project summary
     * @return attention count ({@code >= 0})
     */
    public static int projectAttentionCount(ProjectSummary project) {
        Objects.requireNonNull(project, "project");
        if (project.needsOperatorCount() > 0) {
            return project.needsOperatorCount();
        }
        return project.setupNeedsOperator() ? 1 : 0;
    }

    /**
     * Checks whether a plan needs operator attention. A paused plan is never counted as needing the
     * operator merely for being paused; it is counted only if a decision, waiver, or hold is open.
     *
     * @param plan the active plan summary
     * @return {@code true} if the plan has an active operator wait or hold
     */
    public static boolean needsOperator(ActivePlanSummary plan) {
        Objects.requireNonNull(plan, "plan");
        var count = plan.needsOperatorCount();
        var kind = plan.needsOperatorKind();
        return count != null && count > 0 && kind != null && !kind.isBlank() && !KIND_PAUSED.equalsIgnoreCase(kind);
    }

    /**
     * Checks whether an epic (or any of its nested plans) needs operator attention.
     *
     * @param epic the active epic summary
     * @return {@code true} if the epic has an operator wait or hold
     */
    public static boolean needsOperator(ActiveEpicSummary epic) {
        Objects.requireNonNull(epic, "epic");
        var count = epic.needsOperatorCount();
        var kind = epic.needsOperatorKind();
        return count != null && count > 0 && kind != null && !kind.isBlank() && !KIND_PAUSED.equalsIgnoreCase(kind);
    }

    /**
     * Returns the projects in the order the runtime serves them.
     *
     * <p>The runtime already serves running work with those that need the operator first, then the
     * newest started; a client does not sort again, and activity moves no row
     * ({@code doc/design/shared/plan-overview.adoc#levels}). The time beside a row only says how
     * recently it was active.
     *
     * @param projects the input project list
     * @return the same order, as an immutable copy
     */
    public static List<ProjectSummary> sortProjects(List<ProjectSummary> projects) {
        Objects.requireNonNull(projects, "projects");
        return List.copyOf(projects);
    }

    /**
     * Returns the plans in the order the runtime serves them; activity moves no row.
     *
     * @param plans the input plan list
     * @return the same order, as an immutable copy
     */
    public static List<ActivePlanSummary> sortPlans(List<ActivePlanSummary> plans) {
        Objects.requireNonNull(plans);
        return List.copyOf(plans);
    }

    /**
     * Returns the epics in the order the runtime serves them; activity moves no row.
     *
     * @param epics the input epic list
     * @return the same order, as an immutable copy
     */
    public static List<ActiveEpicSummary> sortEpics(List<ActiveEpicSummary> epics) {
        Objects.requireNonNull(epics);
        return List.copyOf(epics);
    }

    /**
     * Maps an attention kind name to its {@link MessageCatalogue} key.
     *
     * @param kind the attention kind
     * @return message catalogue key, or null if unknown
     */
    public static @Nullable String needsKeyForKind(@Nullable String kind) {
        if (kind == null) {
            return null;
        }
        return switch (kind) {
            case "question", "decision" -> "overview.needs.decision";
            case "waiver", "waiver_request" -> "overview.needs.waiver";
            case "waits_for_you", "operator_wait" -> "overview.needs.waits_for_you";
            case "integrity_pause" -> "overview.needs.integrity_pause";
            case "stalled" -> "overview.needs.stalled";
            case "condition" -> "overview.needs.condition";
            case "held" -> "overview.needs.held";
            case "landing_failed" -> "overview.needs.landing_failed";
            case "findings" -> "overview.needs.findings";
            case "setup" -> "overview.needs.setup";
            case "credential" -> "overview.needs.credential";
            case "cleanup_due" -> "overview.needs.cleanup_due";
            default -> null;
        };
    }

    /**
     * Formats the attention marker for a project summary cell.
     *
     * @param locale the target locale
     * @param kind   the attention kind
     * @param count  the attention count
     * @return the formatted marker, or empty string if none
     */
    public static String formatAttentionMarker(String locale, @Nullable String kind, @Nullable Integer count) {
        if (count == null || count <= 0 || kind == null || kind.isBlank() || KIND_PAUSED.equalsIgnoreCase(kind)) {
            return "";
        }
        var key = needsKeyForKind(kind);
        var label = key != null
                            ? MessageCatalogue.format(locale, key, Map.of())
                            : "! " + clean(kind);
        if (count > 1) {
            var bareLabel = label.startsWith("! ") ? label.substring(2) : label;
            var countPrefix = MessageCatalogue.format(locale, "overview.needs.count", Map.of("count", count));
            return countPrefix + " \u00b7 " + bareLabel;
        }
        return label;
    }

    /**
     * Formats the localized phase state of a plan ({@code running}, {@code pausing after the task},
     * {@code pausing after the phase}, {@code paused}, {@code waiting for you}, {@code held}).
     *
     * @param locale       the target locale
     * @param phaseState   the raw phase state
     * @param pausingPoint the pausing point ({@code "task"} or {@code "phase"}), may be {@code null}
     * @return localized state label
     */
    public static String formatPhaseState(String locale, @Nullable String phaseState, @Nullable String pausingPoint) {
        if (phaseState == null || phaseState.isBlank()) {
            return MessageCatalogue.format(locale, "overview.state.running", Map.of());
        }
        return switch (phaseState) {
            case "running" -> MessageCatalogue.format(locale, "overview.state.running", Map.of());
            case "pausing" -> MessageCatalogue.format(locale, "overview.state.pausing",
                                Map.of("point", pausingPoint != null && !pausingPoint.isBlank() ? clean(pausingPoint) : "phase"));
            case KIND_PAUSED -> MessageCatalogue.format(locale, "overview.state.paused", Map.of());
            case "waiting", "waiting_for_you" -> MessageCatalogue.format(locale, "overview.state.waiting_for_you", Map.of());
            case "held" -> MessageCatalogue.format(locale, "overview.state.held", Map.of());
            default -> clean(phaseState);
        };
    }

    /**
     * Formats a relative activity timestamp through the display layer (e.g. {@code "2 min ago"}).
     *
     * @param locale  target locale
     * @param instant activity instant
     * @param clock   reference clock
     * @return formatted relative time
     */
    public static String formatRelativeActivity(String locale, @Nullable Instant instant, Clock clock) {
        return Display.time(locale, instant, clock);
    }

    /**
     * Formats a relative activity timestamp for a project row (e.g. {@code "2 min ago"}).
     *
     * @param locale       target locale
     * @param timestampIso ISO-8601 timestamp or pre-formatted string
     * @param clock        reference clock
     * @return formatted relative time
     */
    public static String formatRelativeActivity(String locale, @Nullable String timestampIso, Clock clock) {
        if (timestampIso == null || timestampIso.isBlank()) {
            return "";
        }
        var parsed = parseInstant(timestampIso);
        if (parsed == null) {
            return clean(timestampIso);
        }
        return formatRelativeActivity(locale, parsed, clock);
    }

    /**
     * Formats the ended-at timestamp for an ended work item through the display layer's short
     * moment style ({@code today 14:02}, {@code Mon 18:40}, {@code 12 Sep, 09:10}).
     *
     * @param locale  target locale
     * @param instant ended instant
     * @param clock   reference clock
     * @return formatted ended time
     */
    public static String formatEndedTime(String locale, @Nullable Instant instant, Clock clock) {
        return Display.time(locale, instant, Display.TimeStyle.SHORT, clock);
    }

    /**
     * Formats the ended-at timestamp for an ended work item.
     *
     * @param locale       target locale
     * @param timestampIso ISO-8601 timestamp or pre-formatted string
     * @param clock        reference clock
     * @return formatted ended time
     */
    public static String formatEndedTime(String locale, @Nullable String timestampIso, Clock clock) {
        if (timestampIso == null || timestampIso.isBlank()) {
            return "";
        }
        var parsed = parseInstant(timestampIso);
        if (parsed == null) {
            return clean(timestampIso);
        }
        return formatEndedTime(locale, parsed, clock);
    }

    /**
     * Formats an OSC 8 terminal hyperlink ({@code ESC ] 8 ; ; <url> ST <label> ESC ] 8 ; ; ST})
     * after sanitizing both URL and label. When {@code url} is {@code null} or blank, returns the
     * sanitized label.
     *
     * @param url   target URL, may be {@code null}
     * @param label visible link text
     * @return OSC 8 link string or plain sanitized label
     */
    public static String osc8Link(@Nullable String url, String label) {
        var safeLabel = clean(label);
        if (url == null || url.isBlank()) {
            return safeLabel;
        }
        var safeUrl = clean(url);
        if (safeUrl.isEmpty()) {
            return safeLabel;
        }
        return "\u001B]8;;" + safeUrl + "\u001B\\" + safeLabel + "\u001B]8;;\u001B\\";
    }

    static @Nullable Instant parseInstant(@Nullable String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(text.trim());
        } catch (DateTimeParseException _) {
            return null;
        }
    }

    /**
     * Formats the localized label of a {@link NeedsOperatorKind}.
     *
     * @param kind   the kind
     * @param locale the target locale
     * @return localized kind label
     */
    public static String formatNeedsItemKind(@Nullable NeedsOperatorKind kind, String locale) {
        if (kind == null || kind.value() == null) {
            return "";
        }
        var value = kind.value();
        var key = needsKeyForKind(value);
        if (key != null && MessageCatalogue.hasKey(locale, key)) {
            return MessageCatalogue.format(locale, key, Map.of());
        }
        return value.replace('_', ' ');
    }

    /**
     * Formats the subject of a {@link NeedsOperatorItem}.
     *
     * @param item the needs-operator item
     * @return clean subject string
     */
    public static String formatNeedsItemSubject(NeedsOperatorItem item) {
        if (item.plan() != null && !item.plan().id().isBlank()) {
            return clean(item.plan().id());
        }
        if (item.epic() != null && !item.epic().id().isBlank()) {
            return clean(item.epic().id());
        }
        if (item.project() != null && !item.project().id().isBlank()) {
            return clean(item.project().id());
        }
        if (item.kind() == NeedsOperatorKind.CLEANUP_DUE) {
            return "cleanup";
        }
        if (item.kind() == NeedsOperatorKind.CREDENTIAL) {
            return "credential";
        }
        return "machine";
    }

    /**
     * Formats the blocking description of a {@link NeedsOperatorItem}.
     *
     * @param blocks the blocks target
     * @param locale the target locale
     * @return localized blocking string
     */
    public static String formatNeedsItemBlocks(@Nullable String blocks, String locale) {
        if (blocks == null || blocks.isBlank()) {
            if (MessageCatalogue.hasKey(locale, "overview.needs.blocks_nothing")) {
                return MessageCatalogue.format(locale, "overview.needs.blocks_nothing", Map.of());
            }
            return "blocks no work";
        }
        if (MessageCatalogue.hasKey(locale, "overview.needs.blocks")) {
            return MessageCatalogue.format(locale, "overview.needs.blocks", Map.of("what", blocks));
        }
        return "blocks: " + blocks;
    }

    /**
     * Formats the relative since timestamp of a {@link NeedsOperatorItem}.
     *
     * @param since  the start instant
     * @param locale the target locale
     * @param clock  the reference clock
     * @return formatted since string
     */
    public static String formatNeedsItemSince(@Nullable Instant since, String locale, Clock clock) {
        if (since == null) {
            return "";
        }
        var formatted = Display.time(locale, since, Display.TimeStyle.SHORT, clock);
        if (formatted.isBlank()) {
            formatted = Display.time(locale, since, clock);
        }
        if (MessageCatalogue.hasKey(locale, "overview.needs.since")) {
            return MessageCatalogue.format(locale, "overview.needs.since", Map.of("time", formatted));
        }
        return "waits since " + formatted;
    }

    /**
     * Formats the resolving command for a {@link NeedsOperatorItem}.
     *
     * @param item        the needs-operator item
     * @param decisionIds map of plan/project IDs to open decision IDs
     * @return resolving command string
     */
    public static String formatNeedsItemCommand(NeedsOperatorItem item, Map<String, String> decisionIds) {
        var kind = item.kind();
        var planId = item.plan() != null ? item.plan().id() : null;
        var epicId = item.epic() != null ? item.epic().id() : null;
        var projId = item.project() != null ? item.project().id() : null;

        return switch (kind) {
            case DECISION, WAIVER -> decisionCommand(planId, projId, item.link(), decisionIds);
            case FINDINGS -> epicId != null ? "epic findings " + epicId : "reports";
            case SETUP -> projId != null ? "project setup " + projId : "project setup";
            case CREDENTIAL -> "credentials";
            case CLEANUP_DUE -> "cleanup";
            case LANDING_FAILED -> landingFailedCommand(planId, epicId);
            case HELD, CONDITION, STALLED, INTEGRITY_PAUSE, WAITS_FOR_YOU -> planOrEpicCommand(planId, epicId);
        };
    }

    private static String decisionCommand(@Nullable String planId, @Nullable String projId,
                        @Nullable String link, Map<String, String> decisionIds) {
        if (planId != null && decisionIds.containsKey(planId)) {
            return ANSWER_PREFIX + decisionIds.get(planId);
        }
        if (projId != null && decisionIds.containsKey(projId)) {
            return ANSWER_PREFIX + decisionIds.get(projId);
        }
        if (link != null && link.contains("/decisions/")) {
            return ANSWER_PREFIX + link.substring(link.lastIndexOf('/') + 1);
        }
        return "answer";
    }

    private static String landingFailedCommand(@Nullable String planId, @Nullable String epicId) {
        if (planId != null) {
            return "plan hold " + planId;
        }
        if (epicId != null) {
            return "epic " + epicId;
        }
        return CMD_PLANS;
    }

    private static String planOrEpicCommand(@Nullable String planId, @Nullable String epicId) {
        if (planId != null) {
            return "plan " + planId;
        }
        if (epicId != null) {
            return "epic " + epicId;
        }
        return CMD_PLANS;
    }
}
