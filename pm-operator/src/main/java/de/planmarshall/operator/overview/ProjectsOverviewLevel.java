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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.overview.NeedsOperatorItem;
import de.planmarshall.api.overview.NeedsOperatorPage;
import de.planmarshall.api.overview.ProjectSummary;
import de.planmarshall.operator.LoadRefusal;
import de.planmarshall.operator.tui.TuiLevel;
import de.planmarshall.operator.tui.TuiRow;

/**
 * Level 1 of the full-screen overview: the projects list and needs-you group
 * ({@code doc/design/terminal-client/15-overview.adoc#full-screen-layout}).
 *
 * @param projects        the list of project summaries
 * @param needsOperator   the page of items needing operator attention, or {@code null}
 * @param projectsRefusal the refusal of the projects read, or {@code null} when it succeeded
 * @param needsRefusal    the refusal of the needs read, or {@code null} when it succeeded
 */
public record ProjectsOverviewLevel(List<ProjectSummary> projects, @Nullable NeedsOperatorPage needsOperator,
@Nullable LoadRefusal projectsRefusal, @Nullable LoadRefusal needsRefusal) {

    /** Group header row ID for the Needs You section. */
    public static final String GROUP_NEEDS_YOU_ID = "__group_needs_you__";

    /** Row ID prefix for items in the Needs You section. */
    public static final String NEEDS_ITEM_PREFIX = "needs:";

    /** Empty project list row ID. */
    public static final String EMPTY_ROW_ID = "__empty__";

    /**
     * Compact constructor guaranteeing immutable non-null list.
     */
    public ProjectsOverviewLevel {
        projects = List.copyOf(Objects.requireNonNull(projects, "projects"));
    }

    /**
     * Convenience constructor without needs-operator page and without a refusal.
     *
     * @param projects the list of project summaries
     */
    public ProjectsOverviewLevel(List<ProjectSummary> projects) {
        this(projects, null, null, null);
    }

    /**
     * Convenience constructor without a refusal.
     *
     * @param projects      the list of project summaries
     * @param needsOperator the page of items needing operator attention, or {@code null}
     */
    public ProjectsOverviewLevel(List<ProjectSummary> projects, @Nullable NeedsOperatorPage needsOperator) {
        this(projects, needsOperator, null, null);
    }

    /**
     * Creates the {@link TuiLevel} representation for Level 1.
     *
     * @param locale the target locale
     * @return the root overview level
     */
    public static TuiLevel level(String locale) {
        var projectsLabel = MessageCatalogue.format(locale, "system.work.field.projects", Map.of()).toLowerCase(Locale.ROOT);
        var title = MessageCatalogue.format(locale, "overview.running.title", Map.of())
                            + " \u00b7 "
                            + projectsLabel;
        return TuiLevel.root(TuiLevel.Kind.OVERVIEW, title);
    }

    /**
     * Builds the visible {@link TuiRow} items for this level.
     *
     * @param locale the target locale
     * @param clock  the reference clock for relative activity timestamps
     * @return list of TUI rows
     */
    public List<TuiRow> rows(String locale, Clock clock) {
        var rows = new ArrayList<TuiRow>();
        if (needsRefusal != null) {
            rows.addAll(needsRefusal.rows());
        } else {
            addNeedsYouRows(rows, locale, clock);
        }

        if (projectsRefusal != null) {
            rows.addAll(projectsRefusal.rows());
            if (projects.isEmpty()) {
                return rows;
            }
        } else if (projects.isEmpty()) {
            if (rows.isEmpty()) {
                var emptyText = MessageCatalogue.format(locale, "overview.projects.empty", Map.of());
                return List.of(new TuiRow(EMPTY_ROW_ID, emptyText, null, false, false));
            }
            return rows;
        }

        var sorted = OverviewFormatting.sortProjects(projects);

        for (var project : sorted) {
            boolean needs = OverviewFormatting.needsOperator(project);
            int attentionCount = OverviewFormatting.projectAttentionCount(project);

            String marker;
            if (attentionCount > 0) {
                marker = String.format("%-5s", MessageCatalogue.format(locale, "overview.needs.count", Map.of("count", attentionCount)));
            } else {
                marker = "     ";
            }

            var name = project.displayName() != null && !project.displayName().isBlank()
                                ? project.displayName()
                                : project.projectId();
            var cleanName = OverviewFormatting.clean(name);

            var summary = formatSummary(locale, project);
            var relativeTime = OverviewFormatting.formatRelativeActivity(locale, project.lastActivityAt(), clock);

            String line;
            if (!relativeTime.isEmpty()) {
                line = String.format("%s%-16s %-36s %s", marker, cleanName, summary, relativeTime);
            } else {
                line = String.format("%s%-16s %s", marker, cleanName, summary);
            }

            rows.add(new TuiRow(project.projectId(), line.stripTrailing(), null, needs, true));
        }

        return rows;
    }

    /**
     * Finds the {@link NeedsOperatorItem} corresponding to a given row ID.
     *
     * @param rowId the row ID
     * @return the needs item, or {@code null} if unmatched
     */
    public @Nullable NeedsOperatorItem needsItemOf(@Nullable String rowId) {
        if (needsOperator == null || rowId == null || !rowId.startsWith(NEEDS_ITEM_PREFIX)) {
            return null;
        }
        for (var item : needsOperator.items()) {
            if (rowIdOf(item).equals(rowId)) {
                return item;
            }
        }
        return null;
    }

    /**
     * The row ID of one item: its own identity, never its position, so an item that arrives above a
     * row never moves the selection or what {@code !} opens
     * ({@code doc/design/shared/needs-and-find.adoc#waits}).
     *
     * @param item the item
     * @return the row ID
     */
    public static String rowIdOf(NeedsOperatorItem item) {
        if (item.link() != null && !item.link().isBlank()) {
            return NEEDS_ITEM_PREFIX + item.link();
        }
        var id = new StringBuilder(NEEDS_ITEM_PREFIX).append(item.kind().value());
        if (item.project() != null) {
            id.append("@project:").append(item.project().id());
        }
        if (item.epic() != null) {
            id.append("@epic:").append(item.epic().id());
        }
        if (item.plan() != null) {
            id.append("@plan:").append(item.plan().id());
        }
        return id.toString();
    }

    /**
     * Resolves the ID of the first selectable (openable) row in the given rows.
     *
     * @param rows the rows
     * @return first selectable row ID, or first row ID, or {@code null} if empty
     */
    public static @Nullable String firstSelectableId(List<TuiRow> rows) {
        for (var row : rows) {
            if (row.openable()) {
                return row.id();
            }
        }
        return rows.isEmpty() ? null : rows.getFirst().id();
    }

    private void addNeedsYouRows(List<TuiRow> rows, String locale, Clock clock) {
        if (needsOperator == null || needsOperator.items().isEmpty()) {
            return;
        }
        var header = MessageCatalogue.format(locale, "operator.needs.title", Map.of("total", needsOperator.total()));
        rows.add(new TuiRow(GROUP_NEEDS_YOU_ID, header, null, false, false));

        for (var item : needsOperator.items()) {
            var kindLabel = OverviewFormatting.formatNeedsItemKind(item.kind(), locale);
            var subject = OverviewFormatting.formatNeedsItemSubject(item, locale);
            var blocks = OverviewFormatting.formatNeedsItemBlocks(item.blocks(), locale);
            var since = OverviewFormatting.formatNeedsItemSince(item.since(), locale, clock);
            var cmd = OverviewFormatting.formatNeedsItemCommand(item);

            var line = String.format("%-16s %-16s %-19s %-13s %s",
                                "! " + kindLabel,
                                subject,
                                blocks,
                                since,
                                cmd).stripTrailing();

            rows.add(new TuiRow(rowIdOf(item), line, null, true, true));
        }
    }

    private static String formatSummary(String locale, ProjectSummary project) {
        if (project.setupNeedsOperator()) {
            return MessageCatalogue.format(locale, "overview.project.setup", Map.of());
        }
        int plans = project.activePlansCount();
        int epics = project.activeEpicsCount();

        if (plans > 0 && epics > 0) {
            return MessageCatalogue.format(locale, "overview.project.counts", Map.of("plans", plans, "epics", epics));
        }
        if (plans > 0) {
            return MessageCatalogue.format(locale, "overview.project.plans_only", Map.of("plans", plans));
        }
        if (epics > 0) {
            return MessageCatalogue.format(locale, "overview.project.epics_only", Map.of("epics", epics));
        }
        return MessageCatalogue.format(locale, "overview.projects.none_active", Map.of());
    }
}
