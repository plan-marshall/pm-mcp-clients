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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.overview.ActiveEpicSummary;
import de.planmarshall.api.overview.ActivePlanSummary;
import de.planmarshall.api.overview.AlongsideLanding;
import de.planmarshall.api.overview.ProjectDetailResponse;
import de.planmarshall.operator.tui.TuiLevel;
import de.planmarshall.operator.tui.TuiRow;

/**
 * Level 2 of the full-screen overview: a project's active work by plan or by epic
 * ({@code doc/design/terminal-client/15-overview.adoc#full-screen-project}).
 */
public final class ProjectWorkLevel {

    /** Group header row ID for alongside landings. */
    public static final String GROUP_ALONGSIDE_ID = "__group_alongside__";

    /** Group header row ID for plans without an epic. */
    public static final String GROUP_WITHOUT_EPIC_ID = "__group_without_epic__";

    /** Empty project row ID. */
    public static final String EMPTY_PROJECT_ROW_ID = "__project_empty__";

    private static final String DOT_SEPARATOR = " \u00b7 ";
    private static final String RUNNING = "running";
    private static final String KEY_DURATION = "duration";

    private final ProjectDetailResponse detail;
    private final boolean byEpic;
    private final boolean setupNeedsOperator;

    /**
     * @param detail             the project detail data
     * @param byEpic             {@code true} for {@code BY_EPIC} view, {@code false} for {@code BY_PLAN}
     * @param setupNeedsOperator whether project setup needs operator attention
     */
    public ProjectWorkLevel(ProjectDetailResponse detail, boolean byEpic, boolean setupNeedsOperator) {
        this.detail = Objects.requireNonNull(detail, "detail");
        this.byEpic = byEpic;
        this.setupNeedsOperator = setupNeedsOperator;
    }

    /**
     * @param detail the project detail data
     * @param byEpic {@code true} for {@code BY_EPIC} view, {@code false} for {@code BY_PLAN}
     */
    public ProjectWorkLevel(ProjectDetailResponse detail, boolean byEpic) {
        this(detail, byEpic, false);
    }

    /** @return the underlying project detail */
    public ProjectDetailResponse detail() {
        return detail;
    }

    /** @return {@code true} if displaying by epic */
    public boolean byEpic() {
        return byEpic;
    }

    /** @return {@code true} if setup needs operator */
    public boolean setupNeedsOperator() {
        return setupNeedsOperator;
    }

    /**
     * Creates the {@link TuiLevel} representation for Level 2.
     *
     * @param locale the target locale
     * @return the project work level
     */
    public TuiLevel level(String locale) {
        var kind = byEpic ? TuiLevel.Kind.PROJECT_BY_EPIC : TuiLevel.Kind.PROJECT_BY_PLAN;
        var viewName = MessageCatalogue.format(locale, byEpic ? "overview.view.by_epic" : "overview.view.by_plan", Map.of());
        var name = detail.displayName() != null && !detail.displayName().isBlank()
                            ? detail.displayName()
                            : detail.projectId();
        var title = MessageCatalogue.format(locale, "overview.running.title", Map.of())
                            + DOT_SEPARATOR
                            + OverviewFormatting.clean(name)
                            + DOT_SEPARATOR
                            + viewName;
        return TuiLevel.of(kind, detail.projectId(), title);
    }

    /**
     * Builds the visible {@link TuiRow} items for this project.
     *
     * @param locale      target locale
     * @param clock       reference clock
     * @param helpVisible whether the help overlay is active (to reveal plain PR URLs)
     * @return list of TUI rows
     */
    public List<TuiRow> rows(String locale, Clock clock, boolean helpVisible) {
        if (detail.plans().isEmpty() && detail.epics().isEmpty()
                            && detail.alongsideLandings().isEmpty() && !setupNeedsOperator) {
            var emptyText = MessageCatalogue.format(locale, "overview.projects.none_active", Map.of());
            return List.of(new TuiRow(EMPTY_PROJECT_ROW_ID, emptyText, null, false, false));
        }

        var rows = new ArrayList<TuiRow>();
        if (byEpic) {
            buildByEpicRows(rows, locale, clock, helpVisible);
        } else {
            buildByPlanRows(rows, locale, clock, helpVisible);
        }

        buildAlongsideRows(rows, locale);
        return rows;
    }

    private void buildByPlanRows(List<TuiRow> rows, String locale, Clock clock, boolean helpVisible) {
        var sorted = OverviewFormatting.sortPlans(detail.plans());
        for (var plan : sorted) {
            rows.add(formatPlanRow(plan, locale, clock, helpVisible, null, false));
        }
    }

    private void buildByEpicRows(List<TuiRow> rows, String locale, Clock clock, boolean helpVisible) {
        var planMap = new HashMap<String, ActivePlanSummary>();
        for (var p : detail.plans()) {
            planMap.put(p.planId(), p);
        }

        var sortedEpics = OverviewFormatting.sortEpics(detail.epics());
        for (var epic : sortedEpics) {
            rows.add(formatEpicRow(epic, locale, clock));

            var childSummaries = epic.plans();
            for (int i = 0; i < childSummaries.size(); i++) {
                var child = childSummaries.get(i);
                var fullPlan = planMap.getOrDefault(child.planId(), child);
                boolean lastChild = i == childSummaries.size() - 1;
                var prefix = lastChild ? "\u2514\u2500" : "\u251c\u2500";
                rows.add(formatPlanRow(fullPlan, locale, clock, helpVisible, prefix, true));
            }
        }

        appendWithoutEpicRows(rows, locale, clock, helpVisible);
    }

    private void appendWithoutEpicRows(List<TuiRow> rows, String locale, Clock clock, boolean helpVisible) {
        var epicIds = detail.epics().stream().map(ActiveEpicSummary::epicId).collect(Collectors.toSet());
        var withoutEpic = detail.plans().stream()
                            .filter(p -> p.epicId() == null || p.epicId().isBlank() || !epicIds.contains(p.epicId()))
                            .toList();
        if (!withoutEpic.isEmpty()) {
            var header = MessageCatalogue.format(locale, "overview.group.without_epic", Map.of());
            rows.add(new TuiRow(GROUP_WITHOUT_EPIC_ID, header, null, false, false));
            for (var plan : OverviewFormatting.sortPlans(withoutEpic)) {
                rows.add(formatPlanRow(plan, locale, clock, helpVisible, null, false));
            }
        }
    }

    private TuiRow formatEpicRow(ActiveEpicSummary epic, String locale, Clock clock) {
        boolean needs = OverviewFormatting.needsOperator(epic);
        var markerText = OverviewFormatting.formatAttentionMarker(locale, epic.needsOperatorKind(), epic.needsOperatorCount());
        var markerCol = formatMarkerColumn(markerText);

        var kindText = OverviewFormatting.EPIC_GLYPH + " " + MessageCatalogue.format(locale, "overview.kind.epic", Map.of());
        var title = OverviewFormatting.clean(epic.title());
        var progress = MessageCatalogue.format(locale, "overview.epic.plans",
                            Map.of(RUNNING, epic.runningPlansCount(), "staged", epic.stagedPlansCount()));

        var line = String.format("%s%s  %-30s %s", markerCol, kindText, title, progress).stripTrailing();

        var detailsParts = new ArrayList<String>();
        if (epic.runningSince() != null) {
            var dur = OverviewFormatting.formatDuration(epic.runningSince(), clock);
            if (!dur.isEmpty()) {
                detailsParts.add(MessageCatalogue.format(locale, "overview.time.running", Map.of(KEY_DURATION, dur)));
            }
        }
        if (epic.plansTotal() > 0) {
            detailsParts.add(MessageCatalogue.format(locale, "overview.progress.plans",
                                Map.of("done", epic.plansDone(), "total", epic.plansTotal())));
        }
        var details = detailsParts.isEmpty() ? null : String.join(DOT_SEPARATOR, detailsParts);

        return new TuiRow(epic.epicId(), line, details, needs, true);
    }

    private TuiRow formatPlanRow(ActivePlanSummary plan, String locale, Clock clock, boolean helpVisible,
                        @Nullable String treeBranch, boolean insideEpic) {
        boolean needs = OverviewFormatting.needsOperator(plan);
        var markerText = OverviewFormatting.formatAttentionMarker(locale, plan.needsOperatorKind(), plan.needsOperatorCount());

        var kind = OverviewFormatting.PLAN_GLYPH + " " + MessageCatalogue.format(locale, "overview.kind.plan", Map.of());
        var title = OverviewFormatting.clean(plan.title());
        var stateStr = plan.currentPhase() + DOT_SEPARATOR + OverviewFormatting.formatPhaseState(locale, plan.phaseState(), plan.pausingPoint());

        String line;
        if (treeBranch != null) {
            // Nested child under epic: e.g. "        ├─    ▸ plan  Overview resource...   execute · running"
            line = String.format("        %-6s%s  %-30s %s", treeBranch, kind, title, stateStr).stripTrailing();
        } else {
            var markerCol = formatMarkerColumn(markerText);
            line = String.format("%s%s  %-30s %s", markerCol, kind, title, stateStr).stripTrailing();
        }

        var detailParts = buildPlanDetailParts(plan, locale, clock, helpVisible, insideEpic);
        var details = detailParts.isEmpty() ? null : String.join(DOT_SEPARATOR, detailParts);
        return new TuiRow(plan.planId(), line, details, needs, true);
    }

    private static List<String> buildPlanDetailParts(ActivePlanSummary plan, String locale, Clock clock,
                        boolean helpVisible, boolean insideEpic) {
        var detailParts = new ArrayList<String>();
        if (plan.runningSince() != null) {
            var dur = OverviewFormatting.formatDuration(plan.runningSince(), clock);
            if (!dur.isEmpty()) {
                detailParts.add(MessageCatalogue.format(locale, "overview.time.running", Map.of(KEY_DURATION, dur)));
            }
        }
        if (plan.phaseEnteredAt() != null) {
            var phaseDur = OverviewFormatting.formatDuration(plan.phaseEnteredAt(), clock);
            if (!phaseDur.isEmpty()) {
                detailParts.add(MessageCatalogue.format(locale, "overview.time.in_phase",
                                    Map.of("phase", OverviewFormatting.clean(plan.currentPhase()), KEY_DURATION, phaseDur)));
            }
        }
        if (plan.tasksTotal() > 0 || "refine".equalsIgnoreCase(plan.currentPhase())) {
            detailParts.add(MessageCatalogue.format(locale, "overview.progress.tasks",
                                Map.of("done", plan.tasksDone(), "total", plan.tasksTotal())));
        }
        if (!insideEpic && plan.epicTitle() != null && !plan.epicTitle().isBlank()) {
            detailParts.add(MessageCatalogue.format(locale, "overview.plan.belongs_to",
                                Map.of("epic", OverviewFormatting.EPIC_GLYPH + " " + OverviewFormatting.clean(plan.epicTitle()))));
        }
        if (plan.offTrackSignal() != null && !plan.offTrackSignal().isBlank()) {
            detailParts.add(MessageCatalogue.format(locale, "overview.plan.off_track",
                                Map.of("signal", OverviewFormatting.clean(plan.offTrackSignal()))));
        }
        if (plan.pullRequestNumber() != null) {
            var prNum = MessageCatalogue.format(locale, "overview.plan.pull_request",
                                Map.of("number", plan.pullRequestNumber()));
            if (helpVisible && plan.pullRequestUrl() != null && !plan.pullRequestUrl().isBlank()) {
                detailParts.add(prNum + " " + OverviewFormatting.clean(plan.pullRequestUrl()));
            } else {
                detailParts.add(prNum);
            }
        }
        return detailParts;
    }

    private void buildAlongsideRows(List<TuiRow> rows, String locale) {
        var landings = detail.alongsideLandings();
        if (landings.isEmpty() && !setupNeedsOperator) {
            return;
        }

        var header = MessageCatalogue.format(locale, "overview.group.alongside", Map.of());
        rows.add(new TuiRow(GROUP_ALONGSIDE_ID, header, null, false, false));

        for (var landing : landings) {
            rows.add(formatLandingRow(locale, landing));
        }

        if (setupNeedsOperator) {
            var marker = MessageCatalogue.format(locale, "overview.needs.setup", Map.of());
            var text = MessageCatalogue.format(locale, "overview.project.setup", Map.of());
            var line = String.format("%-18s %s", "! " + marker, text);
            rows.add(new TuiRow("setup:" + detail.projectId(), line, null, true, true));
        }
    }

    private static TuiRow formatLandingRow(String locale, AlongsideLanding landing) {
        boolean failed = "failed".equalsIgnoreCase(landing.state()) || landing.holdKind() != null;
        String state = OverviewFormatting.clean(landing.state() != null ? landing.state() : RUNNING);
        String text;
        if ("removal".equalsIgnoreCase(landing.kind())) {
            int count = landing.count() != null ? landing.count() : 1;
            text = MessageCatalogue.format(locale, "overview.landing.removal",
                                Map.of("count", count, "state", state));
        } else {
            String unit = OverviewFormatting.clean(landing.unit() != null ? landing.unit() : "");
            text = MessageCatalogue.format(locale, "overview.landing.unit",
                                Map.of("unit", unit, "state", state));
        }

        String line;
        if (failed) {
            var marker = MessageCatalogue.format(locale, "overview.needs.landing_failed", Map.of());
            line = String.format("%-18s %s", marker, text);
        } else {
            line = "  " + text;
        }

        var id = "landing:" + (landing.unit() != null ? landing.unit() : landing.kind());
        return new TuiRow(id, line.stripTrailing(), null, failed, false);
    }

    private static String formatMarkerColumn(String markerText) {
        if (markerText.isEmpty()) {
            return "              ";
        }
        return String.format("%-14s", markerText);
    }
}
