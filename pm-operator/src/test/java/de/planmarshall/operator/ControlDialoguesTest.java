/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import de.planmarshall.api.control.ArchiveCheck;
import de.planmarshall.api.control.EpicResource;
import de.planmarshall.api.control.HoldRecord;
import de.planmarshall.api.control.HoldResolutionResult;
import de.planmarshall.api.control.PlanSettingsRecord;
import de.planmarshall.api.control.PlanSettingsUpdateResult;
import de.planmarshall.api.control.ReviewResource;
import de.planmarshall.api.control.WorkActions;
import de.planmarshall.api.testsupport.FakeRuntimeServer;
import de.planmarshall.api.testsupport.RuntimeFixture;
import de.planmarshall.operator.actions.WorkActionsClient;
import de.planmarshall.operator.tui.VirtualScreenTerminal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * The control contract as the dialogues and the client read and write it: every dialogue's
 * prompts are scripted on a virtual terminal, so the pauses, the confirmations, the settings
 * conflict, the review dispositions, the chat proposals, and the WordPress actions are exercised
 * without a real terminal.
 */
@DisplayName("control dialogues and client")
@EnabledOnOs({OS.MAC, OS.LINUX})
class ControlDialoguesTest {

    private static final String PLAN_ACTIONS = """
                        {"subject_id":"p","subject_kind":"plan","state":"running","epic_id":"e",
                         "actions":[{"action":"pause","available":true},{"action":"cancel","available":true},
                           {"action":"chat","available":true},{"action":"settings","available":true}]}
                        """;

    private static final String PAUSED_ACTIONS = """
                        {"subject_id":"p","subject_kind":"plan","state":"paused",
                         "actions":[{"action":"start","available":true},{"action":"abandon","available":true}]}
                        """;

    private static final String EPIC_ACTIONS = """
                        {"subject_id":"e","subject_kind":"epic","state":"running",
                         "actions":[{"action":"next","available":true},{"action":"auto","available":true},
                           {"action":"common_files","available":true},{"action":"archive","available":true}]}
                        """;

    private static final String STALLED_HOLD = """
                        [{"hold_id":"H-1","subject_id":"p","subject_kind":"plan","kind":"stalled",
                          "cause":"the worker stopped","attempts":2,"acts":["retry","recycle","end_plan"]}]
                        """;

    private static final String INTEGRITY_HOLD = """
                        [{"hold_id":"H-2","subject_id":"p","subject_kind":"plan","kind":"integrity_pause",
                          "cause":"references.json changed outside the runtime","attempts":0,
                          "file_path":"references.json","recorded_size":4812,"recorded_digest":"3f9a",
                          "current_size":5034,"current_digest":"b70d","acts":["retry","accept","abort"]}]
                        """;

    private static final String REVIEW = """
                        {"review_id":"R-1","epic_id":"e","register":"findings","state":"open",
                         "decided":0,"total":1,"items":[
                           {"item_id":"F-1","register":"findings","statement":"Reconnect loses the last event",
                            "rules":["cover every transport"],"origin":"landing of relay-reconnect (#211)",
                            "trust":"trusted","state":"open","proposal":"promote to a plan"}]}
                        """;

    private static final String REVIEW_KEPT = """
                        {"review_id":"R-1","epic_id":"e","register":"findings","state":"open",
                         "decided":1,"total":1,"items":[
                           {"item_id":"F-1","register":"findings","statement":"Reconnect loses the last event",
                            "rules":["cover every transport"],"origin":"landing of relay-reconnect (#211)",
                            "trust":"trusted","state":"kept","proposal":"promote to a plan"}]}
                        """;

    private static final String REVIEW_COMMITTED = """
                        {"review_id":"R-1","epic_id":"e","register":"findings","state":"committed",
                         "decided":1,"total":1,"items":[
                           {"item_id":"F-1","register":"findings","statement":"Reconnect loses the last event",
                            "rules":["cover every transport"],"origin":"landing of relay-reconnect (#211)",
                            "trust":"trusted","state":"kept","proposal":"promote to a plan"}]}
                        """;

    private static final String CHAT = """
                        {"chat_id":"C-1","subject_id":"p","subject_kind":"plan","role":"replan",
                         "scope":"plan","state":"turn","turns":[
                           {"role":"operator","text":"Can the test cover SSE?","at":"2026-10-16T10:00:00Z",
                            "untrusted":false},
                           {"role":"replan","text":"Yes, I propose a second test.","at":"2026-10-16T10:01:00Z",
                            "untrusted":false}]}
                        """;

    private static final String CHAT_PROPOSAL = """
                        {"chat_id":"C-1","subject_id":"p","subject_kind":"plan","role":"replan",
                         "scope":"plan","state":"read-back","turns":[
                           {"role":"operator","text":"Add SSE","at":"2026-10-16T10:00:00Z","untrusted":false}],
                         "proposal":{"summary":"a test per transport","before":"one transport",
                           "after":"websocket and SSE","applies":"after the current task"}}
                        """;

    private static final String CHAT_ENDED = """
                        {"chat_id":"C-2","subject_id":"docs-typo","subject_kind":"plan","role":"replan",
                         "scope":"archive","state":"read-back","turns":[
                           {"role":"operator","text":"Why was it cancelled?","at":"2026-10-16T10:00:00Z",
                            "untrusted":false}],
                         "proposal":{"summary":"a follow-up","after":"Add the provider on top of relay-v2"}}
                        """;

    private static final String SETTINGS = """
                        {"plan_id":"p","revision":7,"values":[
                          {"aspect":"landing","value":"landing.pull-request","origin":"project",
                           "choices":["landing.pull-request","landing.main"]}]}
                        """;

    private static final String EPIC = """
                        {"epic_id":"e","project_id":"pr","display_name":"pr","state":"running",
                         "auto_start":false,"overlap_max_files":0,"scope":3,"running":3,
                         "workstreams":[{"id":"WS-1","name":"W","open":true,"rows":[
                           {"plan_id":"next-plan","title":"next-plan","status":"staged","wait_reason":"none"}]}],
                         "sources":[],"landings":0,"findings":{"new":0,"open":0,"kept":0},
                         "watches":{"new":0,"open":0,"kept":0},"events":[]}
                        """;

    private static final String ARCHIVE_CHECK = """
                        {"epic_id":"e","lines":[{"label":"control.archive.no_plan_active","passed":true}],
                         "carried_findings":0,"carried_watches":0,"completion_review_first":false,
                         "passed":true}
                        """;

    private RuntimeFixture fixture;
    private VirtualScreenTerminal virtual;
    private StringWriter out;
    private OperatorContext context;

    @BeforeEach
    void setUp() throws IOException {
        fixture = RuntimeFixture.create();
        var self = ProcessHandle.current();
        Files.createDirectories(fixture.paths().stateDir());
        Files.writeString(fixture.paths().runtimeRecord(), "{\"pid\":" + self.pid() + ",\"start_instant\":\""
                            + self.info().startInstant().orElseThrow() + "\"}");
        fixture.writeToken("test-token");
        virtual = VirtualScreenTerminal.create(160, 40);
        out = new StringWriter();
        var env = new HashMap<>(fixture.environment());
        env.put("LANG", "en_US.UTF-8");
        context = new OperatorContext(env, fixture.base().getParent(), true,
                            ClientEnvironment.capture(env), virtual::terminal, null);
    }

    @AfterEach
    void tearDown() throws Exception {
        virtual.close();
        fixture.close();
    }

    private void serve(FakeRuntimeServer.Handler handler) throws IOException {
        fixture.start(handler);
    }

    private int dialogue(String kind, String id, String action) throws IOException {
        return WorkActionDialogue.run(context, new PrintWriter(out), kind, id, action, false, null, null);
    }

    @Test
    @DisplayName("a hold's retry releases the plan")
    void holdRetry() throws Exception {
        serve((request, response) -> {
            var path = request.target();
            if (path.endsWith("/holds")) {
                response.json(200, STALLED_HOLD);
            } else if (path.endsWith("/resolve")) {
                response.json(200, new HoldResolutionResult("H-1", true, null, "p").toJson());
            } else {
                response.send(404, null, "");
            }
        });
        virtual.type("r\n");
        assertEquals(0, HoldDialogue.run(context, new PrintWriter(out), "p", null, false));
        assertTrue(out.toString().contains("p goes on"), out.toString());
    }

    @Test
    @DisplayName("an integrity pause shows its file and accepts the change")
    void holdAccept() throws Exception {
        serve((request, response) -> {
            var path = request.target();
            if (path.endsWith("/holds")) {
                response.json(200, INTEGRITY_HOLD);
            } else if (path.endsWith("/resolve")) {
                response.json(200, new HoldResolutionResult("H-2", true, null, "p").toJson());
            } else {
                response.send(404, null, "");
            }
        });
        virtual.type("c\ny\n");
        assertEquals(0, HoldDialogue.run(context, new PrintWriter(out), "p", null, false));
        var text = out.toString();
        assertTrue(text.contains("references.json"), text);
        assertTrue(text.contains("4812"), text);
        assertTrue(text.contains("goes on with the change"), text);
    }

    @Test
    @DisplayName("an act that does not release the hold shows it again with what still holds it")
    void holdStillHeld() throws Exception {
        serve((request, response) -> {
            var path = request.target();
            if (path.endsWith("/holds")) {
                response.json(200, STALLED_HOLD);
            } else if (path.endsWith("/resolve")) {
                response.json(200, new HoldResolutionResult("H-1", false, "the worker stopped again", "p")
                                    .toJson());
            } else {
                response.send(404, null, "");
            }
        });
        virtual.type("r\n\n");
        assertEquals(0, HoldDialogue.run(context, new PrintWriter(out), "p", null, false));
        assertTrue(out.toString().contains("Still held: the worker stopped again"), out.toString());
    }

    @Test
    @DisplayName("the review keeps a finding and writes it to the ledger")
    void reviewKeepFinish() throws Exception {
        serve((request, response) -> {
            var path = request.target();
            if (path.contains("/review?") || path.endsWith("/review")) {
                response.json(200, REVIEW);
            } else if (path.contains("/review/F-1/disposition")) {
                response.json(200, REVIEW_KEPT);
            } else if (path.endsWith("/review/commit")) {
                response.json(200, REVIEW_COMMITTED);
            } else {
                response.send(404, null, "");
            }
        });
        virtual.type("k\n1\ny\n");
        assertEquals(0, ReviewDialogue.run(context, new PrintWriter(out), "e", "findings", false));
        var text = out.toString();
        assertTrue(text.contains("Reconnect loses the last event"), text);
        assertTrue(text.contains("F-1 kept as a finding"), text);
        assertTrue(text.contains("written to the ledger"), text);
    }

    @Test
    @DisplayName("the review drops an item with its reason")
    void reviewDrop() throws Exception {
        serve((request, response) -> {
            var path = request.target();
            if (path.contains("/review")) {
                response.json(200, REVIEW.replace("\"state\":\"open\"", "\"state\":\"open\""));
            } else if (path.contains("/disposition")) {
                response.json(200, REVIEW_KEPT);
            } else {
                response.send(404, null, "");
            }
        });
        virtual.type("d\nnot relevant\ny\n");
        assertEquals(0, ReviewDialogue.run(context, new PrintWriter(out), "e", "findings", false));
        assertTrue(out.toString().contains("F-1 dropped"), out.toString());
    }

    @Test
    @DisplayName("a chat message is answered, and an empty line ends the chat")
    void chatTurn() throws Exception {
        serve((request, response) -> {
            var path = request.target();
            if (path.endsWith("/chat") && "GET".equals(request.method())) {
                response.json(200, CHAT);
            } else if (path.endsWith("/chat/turns")) {
                response.json(200, CHAT.replace("\"state\":\"turn\"", "\"state\":\"turn\"").replace(
                                    "Can the test cover SSE?", "Add SSE"));
            } else {
                response.send(404, null, "");
            }
        });
        virtual.type("Add SSE\n\n");
        assertNull(WorkChatDialogue.run(context, new PrintWriter(out), "plan", "p", false, false));
        assertTrue(out.toString().contains("Yes, I propose a second test."), out.toString());
        assertTrue(out.toString().contains("The chat is kept with p"), out.toString());
    }

    @Test
    @DisplayName("a chat proposal is read back and accepted")
    void chatProposalAccept() throws Exception {
        serve((request, response) -> {
            var path = request.target();
            if (path.endsWith("/chat")) {
                response.json(200, CHAT_PROPOSAL);
            } else if (path.endsWith("/proposal/accept")) {
                response.json(200, CHAT);
            } else {
                response.send(404, null, "");
            }
        });
        virtual.type("\n\n");
        assertNull(WorkChatDialogue.run(context, new PrintWriter(out), "plan", "p", false, false));
        var text = out.toString();
        assertTrue(text.contains("Proposed change"), text);
        assertTrue(text.contains("websocket and SSE"), text);
        assertTrue(text.contains("The chat is kept with p"), text);
    }

    @Test
    @DisplayName("a chat about ended work proposes a follow-up plan")
    void chatEndedFollowUp() throws Exception {
        serve((request, response) -> {
            if (request.target().endsWith("/chat")) {
                response.json(200, CHAT_ENDED);
            } else {
                response.send(404, null, "");
            }
        });
        virtual.type("\n");
        var description = WorkChatDialogue.run(context, new PrintWriter(out), "plan", "docs-typo", true, false);
        assertEquals("Add the provider on top of relay-v2", description);
        assertTrue(out.toString().contains("read only"), out.toString());
    }

    @Test
    @DisplayName("a setting is applied and a conflict keeps the current value")
    void settingsConflict() throws Exception {
        serve((request, response) -> {
            var path = request.target();
            if (path.endsWith("/settings") && "GET".equals(request.method())) {
                response.json(200, SETTINGS);
            } else if (path.endsWith("/settings")) {
                response.json(409, PlanSettingsUpdateResult.conflict(8, "landing.pull-request").toJson());
            } else {
                response.send(404, null, "");
            }
        });
        virtual.type("1\n2\nk\n");
        assertEquals(0, PlanSettingsDialogue.run(context, new PrintWriter(out), "p", "plan-defaults", false));
        var text = out.toString();
        assertTrue(text.contains("landing.pull-request"), text);
        assertTrue(text.contains("Changed meanwhile"), text);
    }

    @Test
    @DisplayName("a refused setting is asked again and then applied")
    void settingsRefusedThenApplied() throws Exception {
        var attempts = new AtomicInteger();
        serve((request, response) -> {
            var path = request.target();
            if (path.endsWith("/settings") && "GET".equals(request.method())) {
                response.json(200, SETTINGS);
            } else if (path.endsWith("/settings")) {
                if (attempts.getAndIncrement() == 0) {
                    response.json(400, "{\"error\":\"refused\",\"reason\":\"the base branch is protected\","
                                        + "\"value\":\"landing.main\"}");
                } else {
                    response.json(200, PlanSettingsUpdateResult.applied(8).toJson());
                }
            } else {
                response.send(404, null, "");
            }
        });
        virtual.type("1\n1\n2\n1\n");
        assertEquals(0, PlanSettingsDialogue.run(context, new PrintWriter(out), "p", "plan-defaults", false));
        var text = out.toString();
        assertTrue(text.contains("the base branch is protected"), text);
        assertTrue(text.contains("version becomes 8"), text);
    }

    @Test
    @DisplayName("pause asks its point and sends it")
    void workActionPause() throws Exception {
        serve((request, response) -> {
            if (request.target().endsWith("/actions")) {
                response.json(200, PLAN_ACTIONS);
            } else if (request.target().endsWith("/actions/pause")) {
                response.json(200, PLAN_ACTIONS);
            } else {
                response.send(404, null, "");
            }
        });
        virtual.type("2\n");
        assertEquals(0, dialogue("plan", "p", "pause"));
        assertTrue(out.toString().contains("pausing after the task"), out.toString());
    }

    @Test
    @DisplayName("start acts at once")
    void workActionStart() throws Exception {
        serve((request, response) -> {
            if (request.target().endsWith("/actions")) {
                response.json(200, PAUSED_ACTIONS);
            } else if (request.target().endsWith("/actions/start")) {
                response.json(200, PLAN_ACTIONS);
            } else {
                response.send(404, null, "");
            }
        });
        assertEquals(0, dialogue("plan", "p", "start"));
        assertTrue(out.toString().contains("p started"), out.toString());
    }

    @Test
    @DisplayName("cancel asks its reason, the pull request, and confirms")
    void workActionCancel() throws Exception {
        serve((request, response) -> {
            if (request.target().endsWith("/actions")) {
                response.json(200, PLAN_ACTIONS);
            } else if (request.target().endsWith("/actions/cancel")) {
                response.json(200, PLAN_ACTIONS);
            } else {
                response.send(404, null, "");
            }
        });
        virtual.type("the approach is replaced\n1\ny\n");
        assertEquals(0, dialogue("plan", "p", "cancel"));
        var text = out.toString();
        assertTrue(text.contains("cancelled and archived"), text);
        assertTrue(text.contains("e told"), text);
    }

    @Test
    @DisplayName("the epic's automatic, common files, next, and archive")
    void epicActions() throws Exception {
        serve((request, response) -> {
            var path = request.target();
            if (path.endsWith("/epics/e")) {
                response.json(200, EPIC);
            } else if (path.endsWith("/epics/e/actions")) {
                response.json(200, EPIC_ACTIONS);
            } else if (path.endsWith("/epics/e/actions/auto")) {
                response.json(200, EPIC_ACTIONS);
            } else if (path.endsWith("/epics/e/actions/common_files")) {
                response.json(200, EPIC_ACTIONS);
            } else if (path.endsWith("/epics/e/actions/next")) {
                response.json(200, EPIC_ACTIONS);
            } else if (path.endsWith("/epics/e/archive-check")) {
                response.json(200, ARCHIVE_CHECK);
            } else if (path.endsWith("/epics/e/actions/archive")) {
                response.json(200, EPIC_ACTIONS);
            } else {
                response.send(404, null, "");
            }
        });
        var epic = EpicResource.fromJson(EPIC);
        assertEquals(0, WorkActionDialogue.run(context, new PrintWriter(out), "epic", "e", "auto", false,
                            epic, null));
        assertTrue(out.toString().contains("starts its plans by itself"), out.toString());

        out.getBuffer().setLength(0);
        virtual.type("2\n");
        assertEquals(0, WorkActionDialogue.run(context, new PrintWriter(out), "epic", "e", "common_files",
                            false, epic, null));
        assertTrue(out.toString().contains("allows 2"), out.toString());

        out.getBuffer().setLength(0);
        virtual.type("r\n");
        assertEquals(0, WorkActionDialogue.run(context, new PrintWriter(out), "epic", "e", "next", false,
                            epic, null));
        assertTrue(out.toString().contains("started"), out.toString());

        out.getBuffer().setLength(0);
        virtual.type("y\n");
        assertEquals(0, WorkActionDialogue.run(context, new PrintWriter(out), "epic", "e", "archive", false,
                            epic, null));
        assertTrue(out.toString().contains("e archived"), out.toString());
    }

    @Test
    @DisplayName("the client reads and writes every control resource")
    void client() throws Exception {
        serve((request, response) -> {
            var path = request.target();
            if (path.contains("/holds/H-1/resolve")) {
                response.json(200, new HoldResolutionResult("H-1", true, null, "p").toJson());
            } else if (path.endsWith("/holds")) {
                response.json(200, STALLED_HOLD);
            } else if (path.endsWith("/settings") && "GET".equals(request.method())) {
                response.json(200, SETTINGS);
            } else if (path.endsWith("/settings")) {
                response.json(200, PlanSettingsUpdateResult.applied(8).toJson());
            } else if (path.contains("/review")) {
                response.json(200, REVIEW);
            } else if (path.endsWith("/chat")) {
                response.json(200, CHAT);
            } else if (path.endsWith("/chat/turns")) {
                response.json(200, CHAT);
            } else if (path.endsWith("/chat/proposal/accept")) {
                response.json(200, CHAT);
            } else if (path.endsWith("/chat/proposal/reject")) {
                response.json(200, CHAT);
            } else if (path.endsWith("/archive-check")) {
                response.json(200, ARCHIVE_CHECK);
            } else if (path.endsWith("/epics/e")) {
                response.json(200, EPIC);
            } else if (path.endsWith("/actions")) {
                response.json(200, PLAN_ACTIONS);
            } else if (path.contains("/actions/")) {
                response.json(200, PLAN_ACTIONS);
            } else if (path.contains("/landings/")) {
                response.json(200, "{}");
            } else {
                response.send(404, null, "");
            }
        });
        var client = new WorkActionsClient(context);
        WorkActions actions = client.actions("plan", "p");
        assertNotNull(actions);
        assertEquals("running", actions.state());
        List<HoldRecord> holds = client.holds("p");
        assertEquals(1, holds.size());
        assertNotNull(client.hold("p"));
        assertTrue(client.resolveHold("p", "H-1", "retry", null).released());
        PlanSettingsRecord settings = client.settings("p");
        assertEquals(7, settings.revision());
        assertTrue(client.updateSettings("p", 7, "landing", "landing.main").result().applied());
        ReviewResource review = client.review("e", "findings");
        assertEquals(1, review.items().size());
        assertEquals(1, client.disposition("e", "F-1", "kept", null).items().size());
        assertEquals(1, client.commitReview("e").items().size());
        assertEquals(2, client.chat("plan", "p").turns().size());
        assertEquals(2, client.chatTurn("plan", "p", "hello").turns().size());
        assertEquals(2, client.chatProposal("plan", "p", true).turns().size());
        assertEquals(2, client.chatProposal("plan", "p", false).turns().size());
        EpicResource epic = client.epic("e");
        assertEquals("e", epic.epicId());
        ArchiveCheck check = client.archiveCheck("e");
        assertTrue(check.passed());
        client.performLanding("pr", "remediation", "retry");
    }

    @Test
    @DisplayName("a yes answer is the same in both languages")
    void answers() {
        assertTrue(Answers.isYes("j"));
        assertTrue(Answers.isNo("nein"));
        assertTrue(Answers.isYes("y"));
        assertTrue(Answers.isNo("n"));
    }
}
