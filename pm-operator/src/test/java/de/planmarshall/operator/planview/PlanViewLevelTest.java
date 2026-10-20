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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import de.planmarshall.api.process.ProcessTree;
import de.planmarshall.operator.tui.TuiRow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PlanViewLevel")
class PlanViewLevelTest {

    private static final String TREE = """
                        {
                          "plan_id": "fix-relay",
                          "title": "Fix Relay Transport",
                          "project": {"project_id": "pm-mcp-clients", "display_name": "pm-mcp-clients"},
                          "epic": {"epic_id": "relay-hardening", "title": "relay-hardening"},
                          "current_phase": "execute",
                          "phase_state": "running",
                          "duration_seconds": 7800,
                          "pull_request_number": 214,
                          "needs_operator": false,
                          "tokens_so_far": {
                            "total": 412000, "input": 380000, "output": 32000,
                            "floor": true, "tasks_total": 4, "tasks_measured": 3,
                            "none_yet": false, "not_measured": false
                          },
                          "nodes": [
                            {"node_id": "init", "title": "phase init", "kind": "phase", "state": "done",
                             "run_count": 1, "current_run": 1, "duration_seconds": 120},
                            {"node_id": "execute", "title": "phase execute", "kind": "phase", "state": "running",
                             "run_count": 1, "current_run": 1, "duration_seconds": 1500,
                             "sub_nodes": [
                               {"node_id": "execute.envelope", "title": "execute.envelope", "kind": "tasks",
                                "state": "running", "run_count": 3, "current_run": 3, "duration_seconds": 360,
                                "leaf_tasks": [
                                  {"task_id": "TASK-001", "role": "implement", "harness": "claude",
                                   "model": "sonnet", "attempt": 1, "status": "done", "outcome": "success",
                                   "duration_seconds": 240, "tokens_so_far": 25000},
                                  {"task_id": "TASK-003", "role": "tester", "harness": "opencode",
                                   "model": "deepseek", "attempt": 2, "status": "running",
                                   "duration_seconds": 360, "tokens_so_far": 48000}
                                ]},
                               {"node_id": "remediation", "title": "sub-workflow remediation", "kind": "sub_workflow",
                                "state": "planned", "run_count": 0, "current_run": 0}
                             ]},
                            {"node_id": "finalize", "title": "phase finalize", "kind": "phase", "state": "planned"},
                            {"node_id": "refine", "title": "refine, outline, plan", "kind": "phase",
                             "state": "not_in_plan", "prune_reason": "lane_choice"}
                          ],
                          "current_position_path": ["execute", "execute.envelope", "TASK-003"]
                        }
                        """;

    private static PlanViewLevel view() throws Exception {
        return new PlanViewLevel(ProcessTree.fromJson(TREE));
    }

    private static List<String> ids(PlanViewLevel view) {
        return view.rows("en").stream().map(TuiRow::id).toList();
    }

    @Test
    @DisplayName("opens the path to the plan's current position and selects its running leaf")
    void opensCurrentPath() throws Exception {
        var view = view();
        var rows = view.rows("en");
        var text = rows.stream().map(TuiRow::line).collect(Collectors.joining("\n"));
        assertAll(
                            () -> assertEquals("task:TASK-003", view.initialRowId()),
                            () -> assertTrue(ids(view).contains("node:execute")),
                            () -> assertTrue(ids(view).contains("node:execute.envelope")),
                            () -> assertTrue(ids(view).contains("task:TASK-003")),
                            () -> assertTrue(text.contains("phase execute")),
                            () -> assertTrue(text.contains("execute.envelope")),
                            () -> assertTrue(text.contains("tester")),
                            () -> assertTrue(text.contains("run 3")),
                            () -> assertTrue(text.contains("1 of 2 tasks done"))
        );
    }

    @Test
    @DisplayName("the header carries the path from the project through the epic and the plan to the node")
    void breadcrumb() throws Exception {
        var view = view();
        assertEquals("pm-mcp-clients \u203a relay-hardening \u203a fix-relay \u203a execute.envelope",
                            view.breadcrumb("en", "node:execute.envelope"));
        assertEquals("pm-mcp-clients \u203a relay-hardening \u203a fix-relay \u203a fix-relay",
                            view.breadcrumb("en", "summary:plan"));
    }

    @Test
    @DisplayName("open and close walk the tree and the close of a closed node moves to its parent")
    void openAndClose() throws Exception {
        var view = view();
        assertTrue(view.closeRow("task:TASK-003").contains("node:execute.envelope"));
        assertNull(view.closeRow("node:execute.envelope"));
        assertTrue(ids(view).contains("node:execute.envelope"));
        assertFalse(ids(view).contains("task:TASK-003"));
        assertTrue(view.openRow("node:execute.envelope"));
        assertTrue(ids(view).contains("task:TASK-003"));
        assertNull(view.closeRow("node:init"));
    }

    @Test
    @DisplayName("open all opens every descendant of the selected node")
    void expandAll() throws Exception {
        var view = view();
        view.closeRow("node:execute.envelope");
        view.expandAll("node:execute");
        var ids = ids(view);
        assertTrue(ids.contains("node:execute.envelope"));
        assertTrue(ids.contains("task:TASK-003"));
        assertTrue(ids.contains("node:remediation"));
        assertTrue(ids.contains("node:finalize"));
    }

    @Test
    @DisplayName("where it is reopens the path and returns the active row")
    void whereItIs() throws Exception {
        var view = view();
        view.closeRow("node:execute.envelope");
        assertEquals("task:TASK-003", view.jumpToActive());
        assertTrue(ids(view).contains("task:TASK-003"));
    }

    @Test
    @DisplayName("n hides and shows the nodes not in this plan's lane")
    void notInPlanToggle() throws Exception {
        var view = view();
        assertTrue(ids(view).contains("node:refine"));
        view.toggleNonLane();
        assertFalse(ids(view).contains("node:refine"));
        view.toggleNonLane();
        assertTrue(ids(view).contains("node:refine"));
    }

    @Test
    @DisplayName("t shows and hides the token breakdown by phase and by role with coverage")
    void tokensToggle() throws Exception {
        var view = view();
        assertFalse(view.tokensVisible());
        view.toggleTokens();
        var text = view.rows("en").stream().map(TuiRow::line).collect(Collectors.joining("\n"));
        assertAll(
                            () -> assertTrue(view.tokensVisible()),
                            () -> assertTrue(text.contains("by phase")),
                            () -> assertTrue(text.contains("by role")),
                            () -> assertTrue(text.contains("tester")),
                            () -> assertTrue(text.contains("1 of 4 tasks not measured")),
                            () -> assertTrue(text.contains("\u2265") || text.contains("412 k"))
        );
        view.toggleTokens();
        assertFalse(view.rows("en").stream().anyMatch(row -> row.line().contains("by phase")));
    }

    @Test
    @DisplayName("a plan of an epic offers e; an ended plan offers c")
    void footerKeys() throws Exception {
        var view = view();
        assertTrue(view.footer("en").contains("e epic"), view.footer("en"));
        assertFalse(view.footer("en").contains("c completion"), view.footer("en"));
        var ended = new PlanViewLevel(ProcessTree.fromJson(TREE), new PlanViewLevel.EndedInfo("merged", "done", "2 d ago"));
        assertTrue(ended.footer("en").contains("c completion"), ended.footer("en"));
    }

    @Test
    @DisplayName("a plan of no epic leaves e out of the footer")
    void footerWithoutEpic() throws Exception {
        var tree = TREE.replaceAll("(?s),\\s*\"epic\": \\{[^}]*\\}", "");
        var view = new PlanViewLevel(ProcessTree.fromJson(tree));
        assertFalse(view.footer("en").contains("e epic"), view.footer("en"));
        assertNull(view.epicId());
    }


    @Test
    @DisplayName("a later process replaces the tree while the expansion and the path stay")
    void updatesTree() throws Exception {
        var view = view();
        var fresh = ProcessTree.fromJson(TREE.replace(
                            "\"current_position_path\": [\"execute\", \"execute.envelope\", \"TASK-003\"]",
                            "\"current_position_path\": [\"finalize\"]"));
        view.update(fresh);
        assertEquals("node:finalize", view.jumpToActive());
        assertTrue(ids(view).contains("node:execute.envelope"));
    }

    @Test
    @DisplayName("rows of a plan with no epic and no tokens still render the plan header")
    void noEpicNoTokens() throws Exception {
        var tree = """
                            {"plan_id":"docs-typo","title":"Docs",
                              "project":{"project_id":"pm-mcp-clients"},
                              "current_phase":"execute","phase_state":"running",
                              "tokens_so_far":{"total":0,"input":0,"output":0,"none_yet":true},
                              "nodes":[{"node_id":"execute","title":"phase execute","kind":"phase",
                                        "state":"running"}],
                              "current_position_path":["execute"]}
                            """;
        var view = new PlanViewLevel(ProcessTree.fromJson(tree));
        var text = view.rows("en").stream().map(TuiRow::line).collect(Collectors.joining("\n"));
        assertAll(
                            () -> assertTrue(text.contains("none yet"), text),
                            () -> assertTrue(view.breadcrumb("en", "node:execute").contains("docs-typo")),
                            () -> assertFalse(view.footer("en").contains("e epic"))
        );
    }

    @Test
    @DisplayName("unknown, summary and null rows are not tasks, nodes, or waits")
    void lookups() throws Exception {
        var view = view();
        assertAll(
                            () -> assertNull(view.taskIdOf(null)),
                            () -> assertNull(view.taskIdOf("node:execute")),
                            () -> assertNull(view.taskIdOf("task:UNKNOWN")),
                            () -> assertNull(view.waitingPlanOf(null)),
                            () -> assertNull(view.waitingPlanOf("task:TASK-003")),
                            () -> assertNull(view.waitingPlanOf("node:unknown")),
                            () -> assertNull(view.closeRow(null)),
                            () -> assertNull(view.closeRow("summary:plan")),
                            () -> assertNull(view.nodeIdOfTask("UNKNOWN"))
        );
    }

    @Test
    @DisplayName("open all without a selection opens the whole tree")
    void expandAllWithoutSelection() throws Exception {
        var view = view();
        view.closeRow("node:execute.envelope");
        view.expandAll(null);
        assertTrue(ids(view).contains("task:TASK-001"));
    }

    @Test
    @DisplayName("a leaf job renders its kind, status, duration and outcome")
    void leafJob() throws Exception {
        var tree = TREE.replace("\"node_id\": \"finalize\", \"title\": \"phase finalize\", \"kind\": \"phase\", \"state\": \"planned\"",
                            "\"node_id\": \"finalize\", \"title\": \"phase finalize\", \"kind\": \"phase\", \"state\": \"running\","
                                                + " \"leaf_job\": {\"kind\": \"compile\", \"status\": \"done\", \"outcome\": \"success\","
                                                + " \"duration_seconds\": 45}");
        var view = new PlanViewLevel(ProcessTree.fromJson(tree));
        view.expandAll(null);
        var text = view.rows("en").stream().map(TuiRow::line).collect(Collectors.joining("\n"));
        assertTrue(text.contains("compile"), text);
        assertTrue(text.contains("success"), text);
    }

    @Test
    @DisplayName("an ended plan's summary names the outcome, the time and the reason")
    void endedSummary() throws Exception {
        var view = new PlanViewLevel(ProcessTree.fromJson(TREE),
                            new PlanViewLevel.EndedInfo("cancelled", "replaced", "2 d ago"));
        var text = view.rows("en").stream().map(TuiRow::line).collect(Collectors.joining("\n"));
        assertAll(
                            () -> assertTrue(text.contains("cancelled"), text),
                            () -> assertTrue(text.contains("2 d ago"), text),
                            () -> assertTrue(text.contains("replaced"), text)
        );
    }

    @Test
    @DisplayName("a waiting node names its plan for the decision hand-off")
    void waitingNode() throws Exception {
        var tree = TREE.replace("\"state\": \"running\", \"run_count\": 3", "\"state\": \"waiting_for_operator\", \"run_count\": 3");
        var view = new PlanViewLevel(ProcessTree.fromJson(tree));
        assertEquals("fix-relay", view.waitingPlanOf("node:execute.envelope"));
        assertNull(view.waitingPlanOf("node:execute"));
        assertEquals("execute.envelope", view.nodeIdOfTask("TASK-003"));
        assertNotNull(view.taskIdOf("task:TASK-003"));
    }
}
