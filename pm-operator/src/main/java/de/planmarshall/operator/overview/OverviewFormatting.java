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
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.overview.ActiveEpicSummary;
import de.planmarshall.api.overview.ActivePlanSummary;
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

    private static final DateTimeFormatter SHORT_DATE_EN = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);
    private static final DateTimeFormatter SHORT_DATE_DE = DateTimeFormatter.ofPattern("d. MMM", Locale.GERMAN);
    private static final DateTimeFormatter SHORT_TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);

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
     * Orders projects with those needing the operator first, then by most recent activity,
     * preserving original runtime order on ties.
     *
     * @param projects the input project list
     * @return sorted copy of projects
     */
    public static List<ProjectSummary> sortProjects(List<ProjectSummary> projects) {
        Objects.requireNonNull(projects, "projects");
        var indexed = new ArrayList<Indexed<ProjectSummary>>(projects.size());
        for (int i = 0; i < projects.size(); i++) {
            var project = projects.get(i);
            indexed.add(new Indexed<>(project, i, needsOperator(project), project.lastActivityAt()));
        }
        indexed.sort(OverviewFormatting::compareIndexed);
        return indexed.stream().map(Indexed::item).toList();
    }

    /**
     * Orders plans with those needing the operator first, then by most recent activity,
     * preserving original runtime order on ties.
     *
     * @param plans the input plan list
     * @return sorted copy of plans
     */
    public static List<ActivePlanSummary> sortPlans(List<ActivePlanSummary> plans) {
        Objects.requireNonNull(plans, "plans");
        var indexed = new ArrayList<Indexed<ActivePlanSummary>>(plans.size());
        for (int i = 0; i < plans.size(); i++) {
            var plan = plans.get(i);
            var instant = plan.phaseEnteredAt() != null ? plan.phaseEnteredAt() : plan.runningSince();
            indexed.add(new Indexed<>(plan, i, needsOperator(plan), instant));
        }
        indexed.sort(OverviewFormatting::compareIndexed);
        return indexed.stream().map(Indexed::item).toList();
    }

    /**
     * Orders epics with those needing the operator first, then by start instant descending,
     * preserving original runtime order on ties.
     *
     * @param epics the input epic list
     * @return sorted copy of epics
     */
    public static List<ActiveEpicSummary> sortEpics(List<ActiveEpicSummary> epics) {
        Objects.requireNonNull(epics, "epics");
        var indexed = new ArrayList<Indexed<ActiveEpicSummary>>(epics.size());
        for (int i = 0; i < epics.size(); i++) {
            var epic = epics.get(i);
            boolean needs = needsOperator(epic) || epic.plans().stream().anyMatch(OverviewFormatting::needsOperator);
            indexed.add(new Indexed<>(epic, i, needs, epic.runningSince()));
        }
        indexed.sort(OverviewFormatting::compareIndexed);
        return indexed.stream().map(Indexed::item).toList();
    }

    private static <T> int compareIndexed(Indexed<T> a, Indexed<T> b) {
        return Comparator.<Indexed<T>, Boolean>comparing(i -> !i.needsOperator())
                            .thenComparing(Indexed::instant,
                                                Comparator.nullsLast(Comparator.<Instant>reverseOrder()))
                            .thenComparingInt(Indexed::index)
                            .compare(a, b);
    }

    /**
     * Formats the attention badge for a plan or epic row (e.g. {@code "! decision"} or
     * {@code "! 2 · decision"}).
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
        var key = switch (kind) {
            case "question", "decision" -> "overview.needs.decision";
            case "waiver" -> "overview.needs.waiver";
            case "waiver_request" -> "overview.needs.waiver_request";
            case "waits_for_you" -> "overview.needs.waits_for_you";
            case "operator_wait" -> "overview.needs.operator_wait";
            case "integrity_pause" -> "overview.needs.integrity_pause";
            case "stalled" -> "overview.needs.stalled";
            case "condition" -> "overview.needs.condition";
            case "held" -> "overview.needs.held";
            case "landing_failed" -> "overview.needs.landing_failed";
            case "findings" -> "overview.needs.findings";
            case "setup" -> "overview.needs.setup";
            default -> null;
        };
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
     * Formats an elapsed duration from an Instant to {@code clock.instant()}.
     *
     * @param instant start instant
     * @param clock   reference clock
     * @return human-readable duration (e.g. {@code "18 min"}, {@code "1 h 12 min"})
     */
    public static String formatDuration(@Nullable Instant instant, Clock clock) {
        if (instant == null) {
            return "";
        }
        long seconds = Math.max(0L, Duration.between(instant, clock.instant()).getSeconds());
        return formatSeconds(seconds);
    }

    /**
     * Formats an elapsed duration from an ISO-8601 timestamp to {@code clock.instant()}.
     *
     * @param timestampIso ISO-8601 timestamp or pre-formatted duration string
     * @param clock        reference clock
     * @return human-readable duration (e.g. {@code "18 min"}, {@code "1 h 12 min"})
     */
    public static String formatDuration(@Nullable String timestampIso, Clock clock) {
        if (timestampIso == null || timestampIso.isBlank()) {
            return "";
        }
        var parsed = parseInstant(timestampIso);
        if (parsed == null) {
            return clean(timestampIso);
        }
        return formatDuration(parsed, clock);
    }

    /**
     * Formats a duration in seconds as {@code "<m> min"} or {@code "<h> h <m> min"}.
     *
     * @param seconds non-negative seconds
     * @return formatted duration string
     */
    public static String formatSeconds(long seconds) {
        if (seconds < 60L) {
            return Math.max(1L, seconds) + " s";
        }
        long totalMinutes = seconds / 60L;
        long hours = totalMinutes / 60L;
        long minutes = totalMinutes % 60L;
        if (hours == 0L) {
            return minutes + " min";
        }
        if (minutes == 0L) {
            return hours + " h";
        }
        return hours + " h " + minutes + " min";
    }

    /**
     * Formats a relative activity timestamp from an Instant (e.g. {@code "2 min ago"}).
     *
     * @param locale  target locale
     * @param instant activity instant
     * @param clock   reference clock
     * @return formatted relative time
     */
    public static String formatRelativeActivity(String locale, @Nullable Instant instant, Clock clock) {
        if (instant == null) {
            return "";
        }
        long seconds = Math.max(0L, Duration.between(instant, clock.instant()).getSeconds());
        long minutes = Math.max(1L, seconds / 60L);
        boolean german = locale.toLowerCase(Locale.ROOT).startsWith("de");
        if (minutes < 60L) {
            return german ? "vor " + minutes + " Min." : minutes + " min ago";
        }
        long hours = minutes / 60L;
        if (hours < 24L) {
            return german ? "vor " + hours + " Std." : hours + " h ago";
        }
        long days = hours / 24L;
        if (days == 1L) {
            return german ? "vor 1 Tag" : "1 day ago";
        }
        return german ? "vor " + days + " Tagen" : days + " days ago";
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
     * Formats the ended-at timestamp for an ended work item from an Instant.
     *
     * @param locale  target locale
     * @param instant ended instant
     * @param clock   reference clock
     * @return formatted ended time
     */
    public static String formatEndedTime(String locale, @Nullable Instant instant, Clock clock) {
        if (instant == null) {
            return "";
        }
        boolean german = locale.toLowerCase(Locale.ROOT).startsWith("de");
        LocalDate nowDate = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
        LocalDate endDate = LocalDate.ofInstant(instant, ZoneOffset.UTC);
        long daysBetween = ChronoUnit.DAYS.between(endDate, nowDate);
        if (daysBetween <= 0L) {
            return formatRelativeActivity(locale, instant, clock);
        }
        if (daysBetween == 1L) {
            var timePart = SHORT_TIME.format(instant.atOffset(ZoneOffset.UTC));
            return (german ? "gestern " : "yesterday ") + timePart;
        }
        if (daysBetween <= 7L) {
            return german ? "vor " + daysBetween + " Tagen" : daysBetween + " days ago";
        }
        return (german ? SHORT_DATE_DE : SHORT_DATE_EN).format(instant.atOffset(ZoneOffset.UTC));
    }

    /**
     * Formats the ended-at timestamp for an ended work item (e.g. {@code "yesterday 18:10"},
     * {@code "2 days ago"}, {@code "2 Oct"}).
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

    private record Indexed<T>(T item, int index, boolean needsOperator, @Nullable Instant instant) {
    }
}
