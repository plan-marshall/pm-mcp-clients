/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.planview;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.report.PlanCompletionReport;
import de.planmarshall.api.report.ReportItem;
import de.planmarshall.operator.tui.TuiRow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@EnableGeneratorController
@DisplayName("PlanCompletionLevel")
class PlanCompletionLevelTest {

    private static final String REPORT = """
                        {
                          "plan_id": "fix-relay",
                          "title": "Fix Relay Transport",
                          "from": {"project_id": "pm-mcp-clients", "epic_id": "relay-hardening"},
                          "outcome": "merged",
                          "reason": "the relay transport is hardened",
                          "pull_request_number": 214,
                          "started_at": "2026-10-18T10:00:00Z",
                          "ended_at": "2026-10-18T12:00:00Z",
                          "deliverables": [{"name": "transport", "state": "done"}],
                          "decision_records": [{"id": "Q-1", "question": "Keep v1?", "by": "operator"}],
                           "own_calls": [
                             {"kind": "shortcut", "description": "reuse the SSE adapter",
                              "limit": "single subscriber", "ends_when": "a second subscriber connects",
                              "judgment": "conflicts", "conflict_target": "the transport test"},
                             {"kind": "design", "description": "the relay writes the cursor",
                              "judgment": "consistent"}
                           ],
                           "change_map": {"diagram": "relay -> cursor\\n  +--> sse",
                                          "state": "partial", "cause": "the vendored tree is not tracked"},
                          "token_usage": {"total_tokens": 412000, "input_tokens": 380000,
                                          "output_tokens": 32000, "tasks_measured": 15, "tasks_total": 17}
                        }
                        """;

    private static String text() throws Exception {
        var level = new PlanCompletionLevel(PlanCompletionReport.fromJson(REPORT));
        return level.rows("en").stream().map(TuiRow::line).collect(Collectors.joining("\n"));
    }

    private static final String REPORT_WITH_CHANGE = REPORT.replace(
                        "\"token_usage\"",
                        "\"change\": {\"files\": 12, \"insertions\": 340, \"deletions\": 27, \"unplanned\": 2},\n"
                                            + "  \"token_usage\"");

    @Test
    @DisplayName("the completion level shows Change when the report carries a size and no line without")
    void changeSizeLine() throws Exception {
        var withChange = new PlanCompletionLevel(PlanCompletionReport.fromJson(REPORT_WITH_CHANGE));
        var line = withChange.rows("en").stream()
                            .filter(row -> "completion:change".equals(row.id()))
                            .map(TuiRow::line)
                            .findFirst()
                            .orElse("");
        assertAll(
                            () -> assertTrue(line.startsWith("  " + String.format("%-16s", "Change")), line),
                            () -> assertTrue(withChange.rows("de").stream()
                                                .filter(row -> "completion:change".equals(row.id()))
                                                .map(TuiRow::line)
                                                .findFirst().orElse("")
                                                .startsWith("  " + String.format("%-16s", "Änderung"))),
                            () -> assertTrue(line.contains("12 files"), line),
                            () -> assertTrue(line.contains("+340"), line),
                            () -> assertTrue(line.contains("\u221227"), line),
                            () -> assertTrue(line.contains("2 unplanned"), line),
                            () -> assertFalse(new PlanCompletionLevel(PlanCompletionReport.fromJson(REPORT))
                                                .rows("en").stream()
                                                .anyMatch(row -> "completion:change".equals(row.id()))));
    }

    @Test
    @DisplayName("the report names its outcome, reason, deliverables, decisions and tokens")
    void reportLines() throws Exception {
        var text = text();
        var level = new PlanCompletionLevel(PlanCompletionReport.fromJson(REPORT));
        assertEquals("fix-relay", level.planId());
        assertTrue(text.contains("fix-relay \u00b7 completion report"), text);
        assertFalse(text.contains("fix-relay \u00b7 fix-relay"), text);
        assertTrue(text.contains("merged"), text);
        assertTrue(text.contains("#214"), text);
        assertTrue(text.contains("the relay transport is hardened"), text);
        assertTrue(text.contains("1 of 1 done"), text);
        assertTrue(text.contains("transport"), text);
        assertTrue(text.contains("Q-1"), text);
        assertTrue(text.contains("412 k"), text);
        assertTrue(text.contains("2 / 17"), text);
    }

    @Test
    @DisplayName("the report names the plan's own calls and its change map as delivered")
    void ownCallsAndChangeMap() throws Exception {
        var text = text();

        assertAll(
                            () -> assertTrue(text.contains("Own calls"), text),
                            () -> assertTrue(text.contains("shortcut"), text),
                            () -> assertTrue(text.contains("reuse the SSE adapter"), text),
                            () -> assertTrue(text.contains("limit single subscriber"), text),
                            () -> assertTrue(text.contains("ends when a second subscriber connects"), text),
                            () -> assertTrue(text.contains("conflicts with the transport test"), text),
                            () -> assertTrue(text.contains("design call"), text),
                            () -> assertTrue(text.contains("the relay writes the cursor"), text),
                            () -> assertTrue(text.contains("consistent"), text),
                            () -> assertTrue(text.contains("Change map"), text),
                            () -> assertTrue(text.contains("partial"), text),
                            () -> assertTrue(text.contains("cause: the vendored tree is not tracked"), text),
                            () -> assertTrue(text.contains("relay -> cursor"), text),
                            () -> assertTrue(text.contains("  +--> sse"), text)
        );
    }


    @Test
    @DisplayName("a minimal report shows the raw time and omits the empty sections")
    void minimalReport() throws Exception {
        // The report formats the age of its end instant, so the fixture names an instant two days
        // back instead of a fixed date that ages out of the assertion.
        var endedAt = Instant.now().minus(2, ChronoUnit.DAYS);
        var minimal = """
                            {
                              "plan_id": "docs-typo", "title": "Docs", "outcome": "no_change",
                              "started_at": "2026-10-18T10:00:00Z", "ended_at": "%s",
                              "ended": "5 min ago", "ran": "5 min",
                              "duration_seconds": 300
                            }
                            """.formatted(endedAt);
        var level = new PlanCompletionLevel(PlanCompletionReport.fromJson(minimal));
        var text = level.rows("en").stream().map(TuiRow::line).collect(Collectors.joining("\n"));
        assertAll(
                            () -> assertTrue(text.contains(ReportItem.formatTime(endedAt)), text),
                            () -> assertTrue(text.contains("5 min"), text),
                            () -> assertFalse(text.contains("Deliverables"), text),
                            () -> assertFalse(text.contains("Decisions"), text),
                            () -> assertFalse(text.contains("Own calls"), text),
                            () -> assertFalse(text.contains("Change map"), text),
                            () -> assertFalse(text.contains("Tokens"), text)
        );
    }

    @Test
    @DisplayName("a report without an end time says the end is unknown instead of inventing one")
    void unknownEndTime() throws Exception {
        var planId = "plan-" + Generators.integers(1, 9999).next();
        var withoutTimes = """
                            {"plan_id": "%s", "title": "Docs", "outcome": "no_change"}
                            """.formatted(planId);
        var level = new PlanCompletionLevel(PlanCompletionReport.fromJson(withoutTimes));

        var english = summary(level, "en");
        var german = summary(level, "de");

        assertAll(
                            () -> assertTrue(english.endsWith(" · unknown"), english),
                            () -> assertTrue(german.endsWith(" · unbekannt"), german));
    }

    private static String summary(PlanCompletionLevel level, String locale) {
        return level.rows(locale).stream()
                            .filter(row -> "completion:summary".equals(row.id()))
                            .map(TuiRow::line)
                            .findFirst()
                            .orElse("");
    }

    @Test
    @DisplayName("a control sequence in a runtime text is stripped")
    void sanitizes() throws Exception {
        var injected = REPORT.replace("the relay transport is hardened",
                            "\\u001B[31mthe relay transport is hardened\\u001B[0m");
        var level = new PlanCompletionLevel(PlanCompletionReport.fromJson(injected));
        var text = level.rows("en").stream().map(TuiRow::line).collect(Collectors.joining("\n"));
        assertFalse(text.contains("\u001B[31m"), "control sequence must be stripped");
        assertTrue(text.contains("the relay transport is hardened"));
    }

    @Test
    @DisplayName("a control sequence in the change map diagram is stripped without redrawing it")
    void sanitizesDiagram() throws Exception {
        var injected = REPORT.replace("relay -> cursor\\n  +--> sse",
                            "relay \\u001B[31m-> cursor\\n  +--> sse");
        var level = new PlanCompletionLevel(PlanCompletionReport.fromJson(injected));
        var text = level.rows("en").stream().map(TuiRow::line).collect(Collectors.joining("\n"));
        assertAll(
                            () -> assertFalse(text.contains("\u001B[31m"), "control sequence must be stripped"),
                            () -> assertTrue(text.contains("relay -> cursor"), text),
                            () -> assertTrue(text.contains("  +--> sse"), text)
        );
    }
}
