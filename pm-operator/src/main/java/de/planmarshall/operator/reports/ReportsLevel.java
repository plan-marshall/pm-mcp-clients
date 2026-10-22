/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.reports;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.report.ReportItem;
import de.planmarshall.operator.OutputSafety;
import de.planmarshall.operator.tui.TuiRow;

/**
 * The unread reports as a level of the full-screen view
 * ({@code doc/design/terminal-client/24-reports.adoc#full-screen}).
 *
 * <p>The level groups the reports the runtime served by project, then by epic, then by plan, in the
 * order the runtime delivered them: the newest first, and a group ordered by its newest report. A
 * group is a heading and not a report, so the selection skips it.
 *
 * <p>The model is pure: it turns a page of reports into rows and knows nothing about the runtime.
 * Marking a report read is the caller's act; {@link #without(String)} returns the level without the
 * report, so the row leaves at once and with it the selection moves to its neighbour.
 */
public final class ReportsLevel {

    /** The prefix of the row id of a report. */
    public static final String REPORT_PREFIX = "report:";

    /** The prefix of the row id of a project's heading. */
    public static final String PROJECT_GROUP_PREFIX = "group:project:";

    /** The prefix of the row id of an epic's heading. */
    public static final String EPIC_GROUP_PREFIX = "group:epic:";

    /** The row id of the line that says there is nothing to show. */
    public static final String EMPTY_ROW_ID = "__empty__";

    private static final String MACHINE_GROUP = "__machine__";
    private static final String DOT = " · ";
    private static final String PLAN_GLYPH = "\u25b8 ";
    private static final String PROJECT_GLYPH = "\u25aa ";
    private static final String EPIC_GLYPH = "\u25c7 ";
    private static final String PROJECT_INDENT = "  ";
    private static final String REPORT_INDENT = "    ";

    private final List<ReportItem> reports;
    private final int unreadCount;

    /**
     * @param reports     the unread reports of the page, newest first
     * @param unreadCount the runtime's count of unread reports across all pages
     */
    public ReportsLevel(List<ReportItem> reports, int unreadCount) {
        this.reports = List.copyOf(Objects.requireNonNull(reports, "reports"));
        this.unreadCount = Math.max(0, unreadCount);
    }

    /** @return the reports the level holds, in the order the runtime served them */
    public List<ReportItem> reports() {
        return reports;
    }

    /** @return the runtime's count of unread reports */
    public int unreadCount() {
        return unreadCount;
    }

    /**
     * @param locale the interface locale
     * @return the header of the level
     */
    public String breadcrumb(String locale) {
        return MessageCatalogue.format(locale, "operator.reports.title", Map.of("count", unreadCount));
    }

    /**
     * @param rowId the row id
     * @return {@code true} when the row is a group heading rather than a report
     */
    public static boolean isHeading(String rowId) {
        return rowId.startsWith(PROJECT_GROUP_PREFIX) || rowId.startsWith(EPIC_GROUP_PREFIX);
    }

    /**
     * @param rowId the row id
     * @return the report id of the row, or {@code null} when the row is no report
     */
    public static @Nullable String reportIdOf(String rowId) {
        return rowId.startsWith(REPORT_PREFIX) ? rowId.substring(REPORT_PREFIX.length()) : null;
    }

    /**
     * @param rowId the row id
     * @return the report of the row, or {@code null} when the row is no report
     */
    public @Nullable ReportItem reportOf(String rowId) {
        var id = reportIdOf(rowId);
        if (id == null) {
            return null;
        }
        return reports.stream().filter(report -> report.id().equals(id)).findFirst().orElse(null);
    }

    /**
     * @param rows the visible rows of the level
     * @return the ids of the reports those rows show, in order
     */
    public static List<String> reportIdsIn(List<TuiRow> rows) {
        var ids = new ArrayList<String>(rows.size());
        for (var row : rows) {
            var id = reportIdOf(row.id());
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }

    /**
     * @param locale the interface locale
     * @return the rows of the level, with a heading per project and per epic
     */
    public List<TuiRow> rows(String locale) {
        var rows = new ArrayList<TuiRow>();
        if (reports.isEmpty()) {
            rows.add(TuiRow.leaf(EMPTY_ROW_ID,
                                MessageCatalogue.format(locale, "operator.reports.empty_level", Map.of())));
            return rows;
        }

        var byProject = new LinkedHashMap<String, List<ReportItem>>();
        for (var report : reports) {
            byProject.computeIfAbsent(projectKey(report), _ -> new ArrayList<>()).add(report);
        }
        for (var projectEntry : byProject.entrySet()) {
            rows.add(TuiRow.leaf(PROJECT_GROUP_PREFIX + projectEntry.getKey(),
                                sanitize(projectTitle(locale, projectEntry.getValue()))));
            appendProject(rows, locale, projectEntry.getKey(), projectEntry.getValue());
        }
        return rows;
    }

    private void appendProject(List<TuiRow> rows, String locale, String projectKey, List<ReportItem> projectReports) {
        var byEpic = new LinkedHashMap<String, List<ReportItem>>();
        var planOnly = new ArrayList<ReportItem>();
        var projectLevel = new ArrayList<ReportItem>();
        for (var report : projectReports) {
            var epic = trimToNull(report.from().epicId());
            var plan = trimToNull(report.from().planId());
            if (epic != null) {
                byEpic.computeIfAbsent(epic, _ -> new ArrayList<>()).add(report);
            } else if (plan != null) {
                planOnly.add(report);
            } else {
                projectLevel.add(report);
            }
        }

        for (var epicEntry : byEpic.entrySet()) {
            rows.add(TuiRow.leaf(EPIC_GROUP_PREFIX + projectKey + "/" + epicEntry.getKey(),
                                PROJECT_INDENT + EPIC_GLYPH + sanitize(epicEntry.getKey())));
            for (var report : epicEntry.getValue()) {
                rows.add(reportRow(locale, report, REPORT_INDENT));
            }
        }
        for (var report : planOnly) {
            rows.add(reportRow(locale, report, PROJECT_INDENT));
        }
        for (var report : projectLevel) {
            rows.add(reportRow(locale, report, PROJECT_INDENT));
        }
    }

    private static TuiRow reportRow(String locale, ReportItem report, String indent) {
        var plan = trimToNull(report.from().planId());
        var epic = trimToNull(report.from().epicId());
        var name = plan != null ? plan : epic != null
                            ? epic : MessageCatalogue.format(locale, "report.list.project_level", Map.of());
        var glyph = plan == null && epic == null ? PROJECT_GLYPH : PLAN_GLYPH;
        var time = sanitize(report.created());
        if (report.isSentLater()) {
            time += DOT + MessageCatalogue.format(locale, "report.list.sent_at",
                                Map.of("time", sanitize(report.sent())));
        }
        var line = indent + glyph
                            + String.format("%-24s %-40s %s", sanitize(name), sanitize(report.subject()), time);
        return new TuiRow(REPORT_PREFIX + report.id(), line.stripTrailing(), details(locale, report), false, true);
    }

    private static String details(String locale, ReportItem report) {
        var parts = new ArrayList<String>(4);
        var body = trimToNull(report.body());
        if (body != null) {
            parts.add(sanitize(body));
        }
        parts.add(sanitize(report.id()));
        parts.add(sanitize(report.created()));
        if (report.isSentLater()) {
            parts.add(MessageCatalogue.format(locale, "report.list.sent_at",
                                Map.of("time", sanitize(report.sent()))));
        }
        return String.join(DOT, parts);
    }

    private static String projectKey(ReportItem report) {
        return report.from().isSystem() ? MACHINE_GROUP : report.from().projectId();
    }

    private static String projectTitle(String locale, List<ReportItem> projectReports) {
        var first = projectReports.getFirst();
        if (first.from().isSystem()) {
            return MessageCatalogue.format(locale, "report.list.machine", Map.of());
        }
        var title = trimToNull(first.from().projectTitle());
        return title != null ? title : first.from().projectId();
    }

    private static @Nullable String trimToNull(@Nullable String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String sanitize(@Nullable String text) {
        return text == null ? "" : OutputSafety.stripControlSequences(text);
    }

    /**
     * @param reportId the report that was read
     * @return the level without that report, one unread fewer
     */
    public ReportsLevel without(String reportId) {
        var remaining = reports.stream().filter(report -> !report.id().equals(reportId)).toList();
        return new ReportsLevel(remaining, unreadCount - (remaining.size() == reports.size() ? 0 : 1));
    }

    /**
     * @param reportIds the reports that were read
     * @return the level without those reports, as many unread fewer
     */
    public ReportsLevel withoutAll(List<String> reportIds) {
        var remaining = reports.stream().filter(report -> !reportIds.contains(report.id())).toList();
        return new ReportsLevel(remaining, unreadCount - (reports.size() - remaining.size()));
    }

    /**
     * The report the selection moves to when the given one leaves the level: the next report, or
     * the one before it when it was the last.
     *
     * @param reportId the report that leaves
     * @param locale   the interface locale
     * @return the row id of its neighbour, or {@code null} when no report is left
     */
    public @Nullable String neighbourOf(String reportId, String locale) {
        var ids = reportIdsIn(rows(locale));
        int index = ids.indexOf(reportId);
        if (index < 0) {
            return null;
        }
        if (index + 1 < ids.size()) {
            return REPORT_PREFIX + ids.get(index + 1);
        }
        return index > 0 ? REPORT_PREFIX + ids.get(index - 1) : null;
    }
}
