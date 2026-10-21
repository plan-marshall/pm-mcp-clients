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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import de.planmarshall.api.control.EpicResource;
import de.planmarshall.operator.tui.TuiLevel;
import de.planmarshall.operator.tui.TuiRow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EpicViewLevel")
class EpicViewLevelTest {

    private static final String EPIC = """
                        {"epic_id":"relay-hardening","project_id":"pm-mcp-clients",
                         "display_name":"pm-mcp-clients","state":"running","auto_start":true,
                         "overlap_max_files":0,"scope":3,"running":3,
                         "workstreams":[
                           {"id":"WS-1","name":"Reconnect","open":true,"rows":[
                             {"plan_id":"relay-reconnect-backoff","title":"relay-reconnect-backoff",
                              "status":"shipped","pull_request_number":211},
                             {"plan_id":"relay-cursor-order","title":"relay-cursor-order","status":"running"},
                             {"plan_id":"relay-reconnect-docs","title":"relay-reconnect-docs","status":"paused"},
                             {"plan_id":"relay-reconnect-metrics","title":"relay-reconnect-metrics",
                              "status":"staged","wait_reason":"none","objective":"measure the reconnect",
                              "deliverables":["the reconnect metrics"],"surface":["pm-relay/src/metrics"]}]},
                           {"id":"WS-2","name":"Authentication","open":true,"rows":[
                             {"plan_id":"relay-auth-v2","title":"relay-auth-v2","status":"waiting for you"},
                             {"plan_id":"relay-token-rotation","title":"relay-token-rotation","status":"staged",
                              "wait_reason":"dependency","wait_plan_id":"relay-auth-v2"},
                             {"plan_id":"relay-docs","title":"relay-docs","status":"staged",
                              "wait_reason":"overlap","wait_plan_id":"relay-auth-v2",
                              "overlap_files":["pm-api/src/store/Docs.java"]}]}],
                         "sources":["2 documents","1 issue"],"landings":2,
                         "findings":{"new":3,"open":4,"kept":2},
                         "watches":{"new":1,"open":4,"kept":0},
                         "tokens":{"total":{"total":3860000,"input":3520000,"output":340000,"measured":true,
                                            "floor":true,"measured_tasks":4,"not_measured_tasks":1},
                                   "own":{"total":120000,"input":110000,"output":10000,"measured":true,
                                          "floor":false,"measured_tasks":1,"not_measured_tasks":0},
                                   "plans":[{"plan_id":"relay-reconnect-backoff",
                                             "figure":{"total":1400000,"input":1300000,"output":100000,
                                                       "measured":true,"floor":false,"measured_tasks":2,
                                                       "not_measured_tasks":0}}]},
                         "events":[
                           {"at":"2026-10-16T12:40:00Z","text":"relay-auth-v2 asks a decision"},
                           {"at":"2026-10-16T12:05:00Z",
                            "text":"relay-reconnect-backoff landed; relay-reconnect-metrics re-ordered"},
                           {"at":"2026-10-16T11:55:00Z","text":"you paused relay-reconnect-docs"}]}
                        """;

    private static EpicResource epic() throws Exception {
        return EpicResource.fromJson(EPIC);
    }

    private static String text(EpicViewLevel view) {
        return view.rows("en").stream().map(TuiRow::line).collect(Collectors.joining("\n"));
    }

    @Test
    @DisplayName("the head names the state, the mode, how many run at once, and what needs you")
    void head() throws Exception {
        var view = new EpicViewLevel(epic());
        var text = text(view);
        assertTrue(text.contains("relay-hardening \u00b7 epic"), text);
        assertTrue(text.contains("running"), text);
        assertTrue(text.contains("automatic"), text);
        assertTrue(text.contains("3 of 3 at once"), text);
        assertTrue(text.contains("! 3"), text);
        assertTrue(view.rows("en").getFirst().details() != null
                            && view.rows("en").getFirst().details().contains("3 new findings"));
        assertTrue(view.rows("en").getFirst().needsOperator(), text);
    }

    @Test
    @DisplayName("the workstreams open to their plans with status, waits, and counts")
    void workstreams() throws Exception {
        var view = new EpicViewLevel(epic());
        var text = text(view);
        assertTrue(text.contains("WS-1  Reconnect"), text);
        assertTrue(text.contains("2 shipped") || text.contains("1 shipped"), text);
        assertTrue(text.contains("relay-reconnect-backoff"), text);
        assertTrue(text.contains("shipped"), text);
        assertTrue(text.contains("#211"), text);
        assertTrue(text.contains("paused by you"), text);
        assertTrue(text.contains("staged \u00b7 can start"), text);
        assertTrue(text.contains("after relay-auth-v2"), text);
        assertTrue(text.contains("waits: overlap with relay-auth-v2"), text);
        var overlap = view.rows("en").stream().filter(row -> row.id().equals(
                            EpicViewLevel.PLAN_PREFIX + "relay-docs")).findFirst().orElseThrow();
        assertTrue(overlap.details() != null && overlap.details().contains("1 file in common with relay-auth-v2"),
                            String.valueOf(overlap.details()));
    }

    @Test
    @DisplayName("sources, landings, registers, tokens, and every event are there")
    void sections() throws Exception {
        var view = new EpicViewLevel(epic());
        var text = text(view);
        assertTrue(text.contains("Sources   2 documents, 1 issue"), text);
        assertTrue(text.contains("Landings"), text);
        assertTrue(text.contains("Findings   3 new \u00b7 4 open \u00b7 2 kept"), text);
        assertTrue(text.contains("Watches   4 open \u00b7 0 kept"), text);
        assertTrue(text.contains("3.86 M"), text);
        assertTrue(text.contains("3.52 M in"), text);
        assertTrue(text.contains("Events"), text);
        assertTrue(text.contains("newest first"), text);
        assertTrue(text.contains("relay-auth-v2 asks a decision"), text);
        assertTrue(text.contains("you paused relay-reconnect-docs"), text);
    }

    @Test
    @DisplayName("a section opens and closes, and its children appear")
    void toggles() throws Exception {
        var view = new EpicViewLevel(epic());
        assertFalse(text(view).contains("doc/concepts/relay-hardening/"));
        view.toggle(EpicViewLevel.SOURCES_ID, "en");
        assertTrue(text(view).contains("2 documents"), text(view));
        view.toggle(EpicViewLevel.SOURCES_ID, "en");
        assertFalse(text(view).contains("\u25be Sources"), text(view));

        view.toggle(EpicViewLevel.LANDINGS_ID, "en");
        assertTrue(text(view).contains("relay-reconnect-backoff  #211"), text(view));

        view.toggle(EpicViewLevel.TOKENS_ID, "en");
        assertTrue(text(view).contains("the epic's own tasks"), text(view));
        assertTrue(text(view).contains("1.40 M"), text(view));
    }

    @Test
    @DisplayName("a staged plan opens to its objective, deliverables, and surface")
    void stagedPlanOpens() throws Exception {
        var view = new EpicViewLevel(epic());
        var rowId = EpicViewLevel.PLAN_PREFIX + "relay-reconnect-metrics";
        assertTrue(view.isStagedPlanRow(rowId));
        view.toggle(rowId, "en");
        var text = text(view);
        assertTrue(text.contains("Objective     measure the reconnect"), text);
        assertTrue(text.contains("Deliverables  the reconnect metrics"), text);
        assertTrue(text.contains("Surface       pm-relay/src/metrics"), text);
    }

    @Test
    @DisplayName("Enter names the plan, the register, and the next launchable plan")
    void lookups() throws Exception {
        var view = new EpicViewLevel(epic());
        assertEquals("relay-cursor-order",
                            view.planIdOf(EpicViewLevel.PLAN_PREFIX + "relay-cursor-order"));
        assertEquals("findings", view.registerOf(EpicViewLevel.FINDINGS_ID));
        assertEquals("watches", view.registerOf(EpicViewLevel.WATCHES_ID));
        assertNull(view.planIdOf("sources"));
        assertEquals("relay-reconnect-metrics", view.nextLaunchable());
        assertEquals(2, view.waitReasons("en").size());
        assertTrue(view.waitReasons("en").stream().anyMatch(line -> line.contains("waits for relay-auth-v2")));
    }

    @Test
    @DisplayName("the level's breadcrumb is the path from the project and the footer names the keys")
    void breadcrumbAndFooter() throws Exception {
        var view = new EpicViewLevel(epic());
        TuiLevel level = view.level("en");
        assertEquals(TuiLevel.Kind.EPIC, level.kind());
        assertEquals("relay-hardening", level.id());
        assertEquals("pm-mcp-clients \u203a relay-hardening", level.title());
        var footer = view.footer("en");
        assertTrue(footer.contains("a actions"), footer);
        assertTrue(footer.contains("p project"), footer);
        assertTrue(footer.contains("Esc back"), footer);
        assertTrue(footer.contains("q leave"), footer);
    }

    @Test
    @DisplayName("an ended epic whose resource is gone shows how it ended, read-only")
    void endedNote() {
        var view = EpicViewLevel.ended("relay-docs", "pm-mcp-clients", "relay-docs",
                            new EpicViewLevel.EndedNote("completed", "all plans shipped", "2 d ago"));
        assertTrue(view.isEnded());
        assertFalse(view.hasResource());
        var text = text(view);
        assertTrue(text.contains("ended"), text);
        assertTrue(view.rows("en").stream().anyMatch(row -> row.details() != null
                            && row.details().contains("all plans shipped")), text);
        assertTrue(text.contains("completed"), text);
    }

    @Test
    @DisplayName("an epic whose collection is empty says so")
    void empty() throws Exception {
        var resource = new EpicResource("e-1", "p", "p", null, "running", true, 0, 1, 0,
                            null, List.of(), List.of(), 0, null, null, null, List.of());
        var view = new EpicViewLevel(resource);
        assertTrue(text(view).contains("Nothing runs in this epic."), text(view));
    }
}
