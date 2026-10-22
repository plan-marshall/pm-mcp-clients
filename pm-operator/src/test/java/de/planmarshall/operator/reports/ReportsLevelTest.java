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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.report.ReportFrom;
import de.planmarshall.api.report.ReportItem;
import de.planmarshall.api.report.ReportKind;
import de.planmarshall.operator.tui.TuiRow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ReportsLevel")
class ReportsLevelTest {

    private static final Instant NOON = Instant.parse("2026-10-16T12:00:00Z");

    private static ReportItem report(String id, String project, @Nullable String epic, @Nullable String plan,
                        ReportKind kind, String subject, Instant created) {
        return report(id, project, epic, plan, kind, subject, created, created, null);
    }

    private static ReportItem report(String id, String project, @Nullable String epic, @Nullable String plan,
                        ReportKind kind, String subject, Instant created, Instant sent, @Nullable String body) {
        return new ReportItem(id, new ReportFrom(project, epic, plan), kind, subject, Map.of(), body, created, sent,
                            null, null, null, null);
    }

    private static List<ReportItem> sample() {
        return List.of(report("rep-0142", "pm-mcp-clients", "relay-hardening", "fix-relay", ReportKind.PLAN_ENDED,
                            "fix-relay ended: merged (#214)", NOON),
                            report("rep-0143", "pm-mcp-clients", "relay-hardening", "relay-token-store",
                                                ReportKind.PLAN_STARTED, "relay-token-store started", NOON.minusSeconds(60)),
                            report("rep-0139", "pm-mcp-clients", null, "docs-typo", ReportKind.PLAN_ENDED,
                                                "docs-typo ended: no change", NOON.minusSeconds(7200),
                                                NOON.minusSeconds(3600), "the plan produced no change"),
                            report("rep-0131", "pm-mcp-core", null, null, ReportKind.PROJECT_ONBOARDED,
                                                "pm-mcp-core is onboarded", NOON.minusSeconds(86_400)));
    }

    @Test
    @DisplayName("groups by project, then epic, then plan, and puts the project's own report last")
    void groupsTheReports() {
        var rows = new ReportsLevel(sample(), 4).rows("en");

        assertEquals(List.of(
                            "group:project:pm-mcp-clients",
                            "group:epic:pm-mcp-clients/relay-hardening",
                            "report:rep-0142",
                            "report:rep-0143",
                            "report:rep-0139",
                            "group:project:pm-mcp-core",
                            "report:rep-0131"), rows.stream().map(TuiRow::id).toList());
        assertTrue(rows.get(0).line().contains("pm-mcp-clients"));
        assertTrue(rows.get(1).line().contains("\u25c7 relay-hardening"));
        assertTrue(rows.get(2).line().contains("\u25b8 fix-relay"));
        assertTrue(rows.get(6).line().contains("\u25aa project"));
    }

    @Test
    @DisplayName("a heading is no report and the selection id of a report is its report id")
    void classifiesRows() {
        assertTrue(ReportsLevel.isHeading("group:project:pm-mcp-clients"));
        assertTrue(ReportsLevel.isHeading("group:epic:pm-mcp-clients/relay-hardening"));
        assertFalse(ReportsLevel.isHeading("report:rep-0142"));
        assertEquals("rep-0142", ReportsLevel.reportIdOf("report:rep-0142"));
        assertNull(ReportsLevel.reportIdOf("group:project:pm-mcp-clients"));
    }

    @Test
    @DisplayName("the details line carries the body, the id, and both times")
    void detailsCarryBodyIdAndTimes() {
        var level = new ReportsLevel(sample(), 4);
        var rows = level.rows("en");
        var docs = rows.stream().filter(row -> "report:rep-0139".equals(row.id())).findFirst().orElseThrow();

        assertNotNull(docs.details());
        assertTrue(docs.details().contains("the plan produced no change"), docs.details());
        assertTrue(docs.details().contains("rep-0139"), docs.details());
        assertTrue(docs.details().contains("sent "), docs.details());
        assertTrue(docs.openable());
    }

    @Test
    @DisplayName("the neighbour of the last report is the one before it, and the only report has none")
    void findsTheNeighbour() {
        var level = new ReportsLevel(sample(), 4);

        assertEquals("report:rep-0143", level.neighbourOf("rep-0142", "en"));
        assertEquals("report:rep-0139", level.neighbourOf("rep-0143", "en"));
        assertEquals("report:rep-0131", level.neighbourOf("rep-0139", "en"));
        assertNull(new ReportsLevel(List.of(sample().getFirst()), 1).neighbourOf("rep-0142", "en"));
    }

    @Test
    @DisplayName("marking read removes the report and lowers the unread count, never below zero")
    void removesAReport() {
        var level = new ReportsLevel(sample(), 4);

        var one = level.without("rep-0142");
        assertEquals(3, one.reports().size());
        assertEquals(3, one.unreadCount());
        assertNull(one.reportOf("report:rep-0142"));

        var all = one.withoutAll(List.of("rep-0143", "rep-0139"));
        assertEquals(1, all.reports().size());
        assertEquals(1, all.unreadCount());

        assertEquals(0, new ReportsLevel(sample(), 0).without("rep-0142").unreadCount());
    }

    @Test
    @DisplayName("an empty level says there is nothing new")
    void showsTheEmptyLine() {
        var rows = new ReportsLevel(List.of(), 0).rows("en");

        assertEquals(1, rows.size());
        assertEquals(ReportsLevel.EMPTY_ROW_ID, rows.getFirst().id());
        assertTrue(rows.getFirst().line().contains("Nothing new"), rows.getFirst().line());
    }

    @Test
    @DisplayName("a report of the machine stands in a group of its own")
    void groupsMachineReports() {
        var machine = new ReportItem("rep-0100", new ReportFrom("system", null, null), ReportKind.SYSTEM_CLEANUP_DUE,
                            "12 plans can be deleted", Map.of(), null, NOON, NOON, null, null, null, null);
        var rows = new ReportsLevel(List.of(machine), 1).rows("en");

        assertEquals("group:project:__machine__", rows.getFirst().id());
        assertTrue(rows.getFirst().line().contains("This machine"), rows.getFirst().line());
    }
}
