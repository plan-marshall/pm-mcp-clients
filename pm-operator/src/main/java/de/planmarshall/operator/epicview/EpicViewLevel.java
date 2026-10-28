/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.epicview;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.control.EpicEvent;
import de.planmarshall.api.control.EpicResource;
import de.planmarshall.api.control.PlanTokenFigure;
import de.planmarshall.api.control.WorkRow;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.operator.tui.TuiLevel;
import de.planmarshall.operator.tui.TuiRow;

/**
 * The full-screen view of one epic ({@code doc/design/shared/epic-view.adoc},
 * {@code doc/design/terminal-client/19-actions.adoc#epic-view}, UC-PLN-5).
 *
 * <p>It is one level of the same view stack as the plan view: the head states the epic's state, its
 * mode, and how many plans run of how many may; the workstreams open to their plans with what each
 * waits for; the sources, landings, registers, tokens, and events open like nodes of the plan view,
 * with every event in this view. The model is pure: it turns the epic resource into rows and keeps
 * which nodes are open, and it never reads or writes the runtime itself.
 */
public final class EpicViewLevel {

    /** The row of the epic's head. */
    public static final String SUMMARY_ID = "summary";

    /** The workstream prefix. */
    public static final String WORKSTREAM_PREFIX = "ws:";

    /** The plan prefix. */
    public static final String PLAN_PREFIX = "plan:";

    /** The row of the epic's sources. */
    public static final String SOURCES_ID = "sources";

    /** The source prefix. */
    public static final String SOURCE_PREFIX = "source:";

    /** The row of the epic's landings. */
    public static final String LANDINGS_ID = "landings";

    /** The landing prefix. */
    public static final String LANDING_PREFIX = "landing:";

    /** The row of the epic's findings. */
    public static final String FINDINGS_ID = "findings";

    /** The row of the epic's watches. */
    public static final String WATCHES_ID = "watches";

    /** The row of the epic's tokens. */
    public static final String TOKENS_ID = "tokens";

    /** The prefix of the detail rows a staged plan opens. */
    public static final String PLAN_DETAIL_PREFIX = "plandetail:";

    /** The row of the epic's events. */
    public static final String EVENTS_ID = "events";

    /** The event prefix. */
    public static final String EVENT_PREFIX = "event:";

    private static final String OPEN = "\u25be ";
    private static final String CLOSED = "\u25b8 ";
    private static final String NO_CHILDREN = "  ";
    private static final String CHILD_INDENT = "    ";
    private static final String DOT = " \u00b7 ";

    private final Map<String, WorkRow> rowsById = new LinkedHashMap<>();
    private final Map<String, String> workstreamOfPlan = new LinkedHashMap<>();
    private final Set<String> expanded = new HashSet<>();

    private EpicResource epic;
    private @Nullable EndedNote endedNote;

    /**
     * A note for an epic whose resource the runtime no longer serves: the ended work row names how
     * it ended, and the view shows that instead of inventing sections.
     *
     * @param end    the end: {@code completed}, {@code cancelled}, or {@code released}
     * @param reason the operator's reason, or {@code null}
     * @param when   when it ended, already formatted, or {@code null}
     */
    public record EndedNote(String end, @Nullable String reason, @Nullable String when) {
        /**
         * @param end    the end
         * @param reason the reason, or {@code null}
         * @param when   when it ended, already formatted, or {@code null}
         */
        public EndedNote {
            Objects.requireNonNull(end, "end");
        }
    }

    /**
     * @param epic the epic as the runtime serves it
     */
    public EpicViewLevel(EpicResource epic) {
        this.epic = Objects.requireNonNull(epic, "epic");
        index();
    }

    /**
     * @param epic      the epic
     * @param endedNote how it ended, shown in place of the sections
     */
    public EpicViewLevel(EpicResource epic, @Nullable EndedNote endedNote) {
        this(epic);
        this.endedNote = endedNote;
    }

    /**
     * @param epicId the epic
     * @param projectId the project
     * @param displayName the title the runtime or the ended work row gives
     * @param endedNote how it ended
     * @return a read-only view of an ended epic whose resource the runtime no longer serves
     */
    public static EpicViewLevel ended(String epicId, String projectId, String displayName,
                        EndedNote endedNote) {
        var resource = new EpicResource(epicId, projectId, displayName, null, "ended", false, 0, 0, 0,
                            endedNote.end(), List.of(), List.of(), 0, null, null, null, List.of());
        return new EpicViewLevel(resource, endedNote);
    }

    /**
     * @param fresh the epic as the runtime serves it now
     */
    public void update(EpicResource fresh) {
        this.epic = Objects.requireNonNull(fresh, "fresh");
        index();
    }

    /** @return the epic as the runtime serves it */
    public EpicResource epic() {
        return epic;
    }

    /** @return the epic's id */
    public String epicId() {
        return epic.epicId();
    }

    /** @return the epic's project */
    public String projectId() {
        return epic.projectId();
    }

    /** @return {@code true} when the epic has ended */
    public boolean isEnded() {
        return epic.hasEnded();
    }

    /** @return {@code true} when the runtime still serves the epic's resource */
    public boolean hasResource() {
        return endedNote == null;
    }

    /**
     * @param locale the interface locale
     * @return the level, its breadcrumb the path {@code project › epic}
     */
    public TuiLevel level(String locale) {
        var project = EpicViewFormatting.clean(epic.projectId());
        var title = MessageCatalogue.format(locale, "operator.epic.header.path",
                            Map.of("project", project, "epic", EpicViewFormatting.clean(epic.epicId())));
        return TuiLevel.of(TuiLevel.Kind.EPIC, epic.epicId(), title);
    }

    /**
     * @param locale the interface locale
     * @return the footer of the epic view
     */
    public String footer(String locale) {
        var parts = new ArrayList<String>();
        // Priority order: a narrower terminal drops the tail, never a key that matters more.
        parts.add("\u2191\u2193 " + MessageCatalogue.format(locale, "operator.overview.keys.move", Map.of()));
        parts.add("Enter/\u2192 " + MessageCatalogue.format(locale, "operator.epic.keys.open", Map.of()));
        parts.add("Esc " + MessageCatalogue.format(locale, "operator.overview.keys.back", Map.of()));
        parts.add("a " + MessageCatalogue.format(locale, "operator.epic.keys.actions", Map.of()));
        parts.add("q " + MessageCatalogue.format(locale, "operator.overview.keys.leave", Map.of()));
        parts.add("p " + MessageCatalogue.format(locale, "operator.epic.keys.project", Map.of()));
        parts.add("\u2190 " + MessageCatalogue.format(locale, "operator.epic.keys.close", Map.of()));
        return String.join("   ", parts);
    }

    /**
     * @param locale the interface locale
     * @return the rows of the epic's view
     */
    public List<TuiRow> rows(String locale) {
        var rows = new ArrayList<TuiRow>();
        rows.add(summaryRow(locale));
        if (endedNote != null) {
            var extra = new ArrayList<String>();
            if (endedNote.reason() != null && !endedNote.reason().isBlank()) {
                extra.add(EpicViewFormatting.clean(endedNote.reason()));
            }
            rows.add(TuiRow.openable("ended", MessageCatalogue.format(locale, "operator.epic.ended_plain",
                                                    Map.of("when", endedNote.when() != null ? endedNote.when() : "")),
                                extra.isEmpty() ? null : String.join(DOT, extra)));
            return rows;
        }
        if (epic.workstreams().isEmpty() && epic.sources().isEmpty() && epic.events().isEmpty()) {
            rows.add(TuiRow.leaf("empty", MessageCatalogue.format(locale, "operator.epic.empty", Map.of())));
            return rows;
        }
        appendWorkstreams(rows, locale);
        appendSources(rows, locale);
        appendLandings(rows, locale);
        rows.add(registerRow(locale, FINDINGS_ID, "operator.epic.findings",
                            EpicViewFormatting.registerCounts(locale, epic.findings(), "findings"),
                            "epic findings", epic.findings().newCount() > 0));
        rows.add(registerRow(locale, WATCHES_ID, "operator.epic.watches",
                            EpicViewFormatting.registerCounts(locale, epic.watches(), "watches"),
                            "epic watches", false));
        rows.add(tokensRow(locale));
        appendTokens(rows, locale);
        appendEvents(rows, locale);
        return rows;
    }

    /**
     * @param rowId the row
     * @param locale the interface locale
     * @return the row to select after opening or closing, or {@code null}
     */
    public @Nullable String toggle(@Nullable String rowId, String locale) {
        if (rowId == null) {
            return null;
        }
        if (rowId.startsWith(PLAN_PREFIX)) {
            var planId = rowId.substring(PLAN_PREFIX.length());
            var row = rowsById.get(planId);
            if (row == null || !"staged".equals(EpicViewFormatting.statusKey(row.status()))) {
                return null;
            }
            toggle(planId);
            return rowId;
        }
        if (SOURCES_ID.equals(rowId) || LANDINGS_ID.equals(rowId) || TOKENS_ID.equals(rowId)
                            || rowId.startsWith(WORKSTREAM_PREFIX)) {
            toggle(rowId);
            return rowId;
        }
        return null;
    }

    /**
     * @param rowId the row
     * @return the row to select after closing, or {@code null}
     */
    public @Nullable String closeRow(@Nullable String rowId) {
        if (rowId == null) {
            return null;
        }
        if (rowId.startsWith(PLAN_PREFIX)) {
            var planId = rowId.substring(PLAN_PREFIX.length());
            if (expanded.remove(planId)) {
                return rowId;
            }
            var workstream = workstreamOfPlan.get(planId);
            return workstream != null ? WORKSTREAM_PREFIX + workstream : SUMMARY_ID;
        }
        if (!expanded.remove(rowId)) {
            return SUMMARY_ID;
        }
        return rowId;
    }

    /** Opens every workstream, section, and staged plan below the head. */
    public void expandAll() {
        for (var workstream : epic.workstreams()) {
            expanded.add(WORKSTREAM_PREFIX + workstream.id());
            for (var row : workstream.rows()) {
                if ("staged".equals(EpicViewFormatting.statusKey(row.status()))) {
                    expanded.add(row.planId());
                }
            }
        }
        expanded.add(SOURCES_ID);
        expanded.add(LANDINGS_ID);
        expanded.add(TOKENS_ID);
    }

    /**
     * @param rowId the row
     * @return {@code true} when the row opens to something
     */
    public boolean openable(String rowId) {
        if (rowId.startsWith(PLAN_PREFIX)) {
            return true;
        }
        return SOURCES_ID.equals(rowId) || LANDINGS_ID.equals(rowId) || TOKENS_ID.equals(rowId)
                            || rowId.startsWith(WORKSTREAM_PREFIX);
    }

    /**
     * @param rowId the row
     * @return {@code true} when {@code Enter} on the row only opens it, rather than a plan view
     */
    public boolean isStagedPlanRow(@Nullable String rowId) {
        if (rowId == null || !rowId.startsWith(PLAN_PREFIX)) {
            return false;
        }
        var row = rowsById.get(rowId.substring(PLAN_PREFIX.length()));
        return row != null && "staged".equals(EpicViewFormatting.statusKey(row.status()));
    }

    /**
     * @param rowId the row
     * @return the plan id when the row is a plan, else {@code null}
     */
    public @Nullable String planIdOf(@Nullable String rowId) {
        if (rowId == null || !rowId.startsWith(PLAN_PREFIX)) {
            return null;
        }
        var planId = rowId.substring(PLAN_PREFIX.length());
        return rowsById.containsKey(planId) ? planId : null;
    }

    /**
     * @param rowId the row
     * @return the register when the row is a register row, else {@code null}
     */
    public @Nullable String registerOf(@Nullable String rowId) {
        if (FINDINGS_ID.equals(rowId)) {
            return "findings";
        }
        if (WATCHES_ID.equals(rowId)) {
            return "watches";
        }
        return null;
    }

    /**
     * @return the plan a next-work action would start, or {@code null}
     */
    public @Nullable String nextLaunchable() {
        for (var row : rowsById.values()) {
            var status = EpicViewFormatting.statusKey(row.status());
            var reason = row.waits();
            if ("staged".equals(status) && (reason == null || "none".equals(reason.value()))) {
                return row.planId();
            }
        }
        return null;
    }

    /**
     * @param locale the interface locale
     * @return why no plan can start, one line per row that waits
     */
    public List<String> waitReasons(String locale) {
        var reasons = new ArrayList<String>();
        for (var row : rowsById.values()) {
            if (!"staged".equals(EpicViewFormatting.statusKey(row.status()))) {
                continue;
            }
            var line = EpicViewFormatting.waitReasonLine(locale, row);
            if (!line.isEmpty()) {
                reasons.add(line);
            }
        }
        return List.copyOf(reasons);
    }

    /**
     * @return the first row of the view
     */
    public String initialRowId() {
        return SUMMARY_ID;
    }

    /**
     * @param rowId the row to find
     * @return the workstream that holds the plan row, or {@code null}
     */
    public @Nullable String workstreamOf(@Nullable String rowId) {
        if (rowId == null || !rowId.startsWith(WORKSTREAM_PREFIX)) {
            return null;
        }
        return rowId.substring(WORKSTREAM_PREFIX.length());
    }

    private void index() {
        rowsById.clear();
        workstreamOfPlan.clear();
        for (var workstream : epic.workstreams()) {
            if (workstream.open()) {
                expanded.add(WORKSTREAM_PREFIX + workstream.id());
            }
            for (var row : workstream.rows()) {
                rowsById.put(row.planId(), row);
                workstreamOfPlan.put(row.planId(), workstream.id());
            }
        }
    }

    private void toggle(String id) {
        if (!expanded.remove(id)) {
            expanded.add(id);
        }
    }

    private void appendWorkstreams(List<TuiRow> rows, String locale) {
        for (var workstream : epic.workstreams()) {
            var open = expanded.contains(WORKSTREAM_PREFIX + workstream.id());
            var marker = workstream.rows().isEmpty() ? NO_CHILDREN : open ? OPEN : CLOSED;
            var line = marker + MessageCatalogue.format(locale, "operator.epic.workstream",
                                Map.of("id", EpicViewFormatting.clean(workstream.id()),
                                                    "name", EpicViewFormatting.clean(workstream.name())))
                                + "   " + EpicViewFormatting.countsText(locale, workstream.rows());
            rows.add(new TuiRow(WORKSTREAM_PREFIX + workstream.id(), line, null, false,
                                !workstream.rows().isEmpty()));
            if (!open) {
                continue;
            }
            for (var row : workstream.rows()) {
                appendPlanRow(rows, row, locale);
            }
        }
    }

    private void appendPlanRow(List<TuiRow> rows, WorkRow row, String locale) {
        var status = EpicViewFormatting.statusText(locale, row);
        var line = new StringBuilder(CHILD_INDENT)
                            .append(EpicViewFormatting.statusGlyph(row)).append("  ")
                            .append(EpicViewFormatting.clean(row.title()))
                            .append("   ").append(status);
        if ("staged".equals(EpicViewFormatting.statusKey(row.status()))) {
            line.append(DOT).append(EpicViewFormatting.waitText(locale, row, epic.running(), epic.scope()));
        }
        var pr = EpicViewFormatting.pullRequest(row);
        if (!pr.isEmpty()) {
            line.append(DOT).append(pr);
        }
        var details = new ArrayList<String>();
        if (row.objective() != null && !row.objective().isBlank()) {
            details.add(EpicViewFormatting.clean(row.objective()));
        }
        if (row.overlapFiles() != null && !row.overlapFiles().isEmpty()) {
            details.add(MessageCatalogue.format(locale, "overview.epic.waits.overlap_files",
                                Map.of("files", row.overlapFiles().size(),
                                                    "plan", EpicViewFormatting.clean(row.waitPlanId() != null
                                                                        ? row.waitPlanId() : ""),
                                                    "paths", String.join(", ", cleanAll(row.overlapFiles())))));
        }
        rows.add(new TuiRow(PLAN_PREFIX + row.planId(), line.toString(), joinOrNull(details), false, true));
        if (expanded.contains(row.planId()) && "staged".equals(EpicViewFormatting.statusKey(row.status()))) {
            appendPlanDetails(rows, row, locale);
        }
    }

    private void appendPlanDetails(List<TuiRow> rows, WorkRow row, String locale) {
        var id = PLAN_DETAIL_PREFIX + row.planId();
        if (row.objective() != null && !row.objective().isBlank()) {
            rows.add(TuiRow.leaf(id + ":objective", MessageCatalogue.format(locale,
                                "operator.epic.plan.objective",
                                Map.of("objective", EpicViewFormatting.clean(row.objective())))));
        }
        if (!row.deliverables().isEmpty()) {
            rows.add(TuiRow.leaf(id + ":deliverables", MessageCatalogue.format(locale,
                                "operator.epic.plan.deliverables",
                                Map.of("deliverables", String.join("; ", cleanAll(row.deliverables()))))));
        }
        if (!row.surface().isEmpty()) {
            rows.add(TuiRow.leaf(id + ":surface", MessageCatalogue.format(locale, "operator.epic.plan.surface",
                                Map.of("surface", String.join(", ", cleanAll(row.surface()))))));
        }
        if (!row.dependencies().isEmpty()) {
            rows.add(TuiRow.leaf(id + ":depends", MessageCatalogue.format(locale, "operator.epic.plan.depends",
                                Map.of("plans", String.join(", ", cleanAll(row.dependencies()))))));
        }
    }

    private void appendSources(List<TuiRow> rows, String locale) {
        if (epic.sources().isEmpty()) {
            return;
        }
        var open = expanded.contains(SOURCES_ID);
        var summary = String.join(", ", cleanAll(epic.sources()));
        var line = (open ? OPEN : CLOSED)
                            + MessageCatalogue.format(locale, "operator.epic.sources", Map.of())
                            + "   " + summary;
        rows.add(new TuiRow(SOURCES_ID, line, null, false, true));
        if (!open) {
            return;
        }
        for (int index = 0; index < epic.sources().size(); index++) {
            rows.add(TuiRow.leaf(SOURCE_PREFIX + index, CHILD_INDENT + EpicViewFormatting.clean(epic.sources().get(index))));
        }
    }

    private void appendLandings(List<TuiRow> rows, String locale) {
        var landings = new ArrayList<WorkRow>();
        for (var workstream : epic.workstreams()) {
            for (var row : workstream.rows()) {
                if (row.pullRequestNumber() != null) {
                    landings.add(row);
                }
            }
        }
        if (landings.isEmpty() && epic.landings() == 0) {
            return;
        }
        var open = expanded.contains(LANDINGS_ID);
        var count = Math.max(epic.landings(), landings.size());
        var line = (open ? OPEN : CLOSED)
                            + MessageCatalogue.format(locale, "operator.epic.landings", Map.of())
                            + "   " + count;
        rows.add(new TuiRow(LANDINGS_ID, line, null, false, true));
        if (!open) {
            return;
        }
        for (var landing : landings) {
            var text = MessageCatalogue.format(locale, "operator.epic.landing",
                                Map.of("plan", EpicViewFormatting.clean(landing.planId()),
                                                    "number", landing.pullRequestNumber()));
            var analysis = analysisFor(landing.planId());
            rows.add(new TuiRow(LANDING_PREFIX + landing.planId(), CHILD_INDENT + text,
                                analysis != null ? MessageCatalogue.format(locale,
                                                    "operator.epic.landing.analysis", Map.of("text", analysis)) : null,
                                false, false));
        }
    }

    private @Nullable String analysisFor(String planId) {
        for (var event : epic.events()) {
            var text = EpicViewFormatting.clean(event.text());
            var marker = text.indexOf("analysis:");
            if (marker >= 0 && startsWithPlanId(text, planId)) {
                return text.substring(marker + "analysis:".length()).trim();
            }
        }
        return null;
    }

    private static boolean startsWithPlanId(String text, String planId) {
        if (!text.startsWith(planId)) {
            return false;
        }
        if (text.length() == planId.length()) {
            return true;
        }
        char next = text.charAt(planId.length());
        return !Character.isLetterOrDigit(next) && next != '-';
    }

    private TuiRow registerRow(String locale, String id, String labelKey, String counts, String command,
                        boolean needs) {
        var line = CLOSED + MessageCatalogue.format(locale, labelKey, Map.of()) + "   " + counts;
        var details = command + " " + EpicViewFormatting.clean(epic.epicId());
        return new TuiRow(id, line, details, needs, true);
    }

    private TuiRow tokensRow(String locale) {
        var open = expanded.contains(TOKENS_ID);
        var plans = planCount();
        var line = (open ? OPEN : CLOSED) + MessageCatalogue.format(locale, "operator.epic.tokens", Map.of())
                            + "   " + EpicViewFormatting.tokensText(locale, epic.tokens(), plans);
        return new TuiRow(TOKENS_ID, line, null, false, true);
    }

    private void appendTokens(List<TuiRow> rows, String locale) {
        if (!expanded.contains(TOKENS_ID)) {
            return;
        }
        var tokens = epic.tokens();
        for (var plan : tokens.plans()) {
            rows.add(TuiRow.leaf(TOKENS_ID + ":" + plan.planId(),
                                CHILD_INDENT + tokenPlanLine(locale, plan)));
        }
        var own = MessageCatalogue.format(locale, "overview.epic.tokens_own", Map.of());
        rows.add(TuiRow.leaf(TOKENS_ID + ":own", CHILD_INDENT + own + "  "
                            + EpicViewFormatting.tokenFigure(locale, tokens.own())));
        var total = MessageCatalogue.format(locale, "operator.epic.tokens", Map.of());
        rows.add(TuiRow.leaf(TOKENS_ID + ":total", CHILD_INDENT + total + "  "
                            + EpicViewFormatting.tokenFigure(locale, tokens.total())));
    }

    private static String tokenPlanLine(String locale, PlanTokenFigure plan) {
        return MessageCatalogue.format(locale, "operator.epic.tokens_plan",
                            Map.of("plan", EpicViewFormatting.clean(plan.planId()),
                                                "figure", EpicViewFormatting.tokenFigure(locale, plan.figure())));
    }

    private void appendEvents(List<TuiRow> rows, String locale) {
        if (epic.events().isEmpty()) {
            return;
        }
        rows.add(new TuiRow(EVENTS_ID, MessageCatalogue.format(locale, "operator.epic.events", Map.of())
                            + "   " + MessageCatalogue.format(locale, "operator.actions.epic.events", Map.of()),
                            null, false, false));
        for (int index = 0; index < epic.events().size(); index++) {
            var event = epic.events().get(index);
            rows.add(TuiRow.leaf(EVENT_PREFIX + index, eventLine(locale, event)));
        }
    }

    private static String eventLine(String locale, EpicEvent event) {
        return MessageCatalogue.format(locale, "operator.epic.event",
                            Map.of("time", EpicViewFormatting.eventTime(event.at()),
                                                "text", EpicViewFormatting.clean(event.text())));
    }

    private TuiRow summaryRow(String locale) {
        var state = EpicViewFormatting.stateText(locale, epic);
        var line = EpicViewFormatting.clean(epic.epicId()) + " \u00b7 epic   " + state
                            + DOT + EpicViewFormatting.modeText(locale, epic)
                            + DOT + EpicViewFormatting.atOnce(locale, epic.running(), epic.scope());
        if (epic.findings().newCount() > 0) {
            line += "   " + MessageCatalogue.format(locale, "operator.epic.header.needs",
                                Map.of("count", epic.findings().newCount()));
        }
        if (endedNote != null) {
            line += DOT + MessageCatalogue.format(locale, "overview.ended.epic_end",
                                Map.of("end", endedNote.end()));
        }
        var needs = new ArrayList<String>();
        if (epic.findings().newCount() > 0) {
            needs.add(MessageCatalogue.format(locale, "operator.actions.epic.needs_you", Map.of())
                                + " " + MessageCatalogue.format(locale, "operator.console.badge.findings",
                                Map.of("count", epic.findings().newCount())));
        }
        if (epic.hasEnded() && epic.end() != null && endedNote == null) {
            needs.add(MessageCatalogue.format(locale, "overview.ended.epic_end",
                                Map.of("end", epic.end().value())));
        }
        return new TuiRow(SUMMARY_ID, line, joinOrNull(needs), epic.findings().newCount() > 0, false);
    }

    private int planCount() {
        return rowsById.size();
    }

    private static List<String> cleanAll(List<String> values) {
        var clean = new ArrayList<String>(values.size());
        for (var value : values) {
            clean.add(EpicViewFormatting.clean(value));
        }
        return clean;
    }

    private static @Nullable String joinOrNull(List<String> parts) {
        return parts.isEmpty() ? null : String.join(DOT, parts);
    }
}
