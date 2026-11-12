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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import de.planmarshall.api.chat.ChatResource;
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
import de.planmarshall.operator.epicview.EpicViewLevel;
import de.planmarshall.operator.tui.VirtualScreenTerminal;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The control contract as the dialogues and the client read and write it: every dialogue's
 * prompts are scripted on a virtual terminal, so the pauses, the confirmations, the settings
 * conflict, the review dispositions, the chat proposals, and the WordPress actions are exercised
 * without a real terminal.
 */
@DisplayName("control dialogues and client")
@EnabledOnOs({OS.MAC, OS.LINUX})
class ControlDialoguesTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-20T12:00:00Z"),
                        ZoneId.of("Pacific/Auckland"));

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
                           {"action":"common_files","available":true},{"action":"archive","available":true},
                           {"action":"cancel","available":true}]}
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
                        {"id":"C-1","subject":{"kind":"plan","id":"p"},
                         "role":{"name":"replan","model":"sonnet"},"scope":"plan","state":"chat.turn",
                         "context":{"knows":[],"as_of":"2026-10-16T10:00:00Z"},"turns":[
                           {"turn_id":"T-1","kind":"message","author":"operator","status":"complete",
                            "format":"plain","untrusted":false,"text":"Can the test cover SSE?","steps":[],
                            "refs":[],"at":"2026-10-16T10:00:00Z"},
                           {"turn_id":"T-2","kind":"message","author":"replan","status":"complete",
                            "format":"markdown","untrusted":false,"model":"sonnet",
                            "text":"Yes, I propose a second test.","steps":[],"refs":[],
                            "at":"2026-10-16T10:01:00Z"}]}
                        """;

    private static final String CHAT_ANSWERED = """
                        {"id":"C-1","subject":{"kind":"plan","id":"p"},
                         "role":{"name":"replan","model":"sonnet"},"scope":"plan","state":"chat.turn",
                         "context":{"knows":[],"as_of":"2026-10-16T10:02:00Z"},"turns":[
                           {"turn_id":"T-1","kind":"message","author":"operator","status":"complete",
                            "format":"plain","untrusted":false,"text":"Can the test cover SSE?","steps":[],
                            "refs":[],"at":"2026-10-16T10:00:00Z"},
                           {"turn_id":"T-2","kind":"message","author":"replan","status":"complete",
                            "format":"markdown","untrusted":false,"model":"sonnet",
                            "text":"Yes, I propose a second test.","steps":[],"refs":[],
                            "at":"2026-10-16T10:01:00Z"},
                           {"turn_id":"T-3","kind":"message","author":"operator","status":"complete",
                            "format":"plain","untrusted":false,"text":"Add SSE","steps":[],"refs":[],
                            "at":"2026-10-16T10:02:00Z"},
                           {"turn_id":"T-4","kind":"message","author":"replan","status":"complete",
                            "format":"markdown","untrusted":false,"model":"sonnet",
                            "text":"Yes, I propose a second test.","steps":[],"refs":[],
                            "at":"2026-10-16T10:03:00Z"}]}
                        """;

    private static final String CHAT_PROPOSAL = """
                        {"id":"C-1","subject":{"kind":"plan","id":"p"},
                         "role":{"name":"replan","model":"sonnet"},"scope":"plan","state":"chat.read-back",
                         "context":{"knows":[],"as_of":"2026-10-16T10:00:00Z"},"turns":[
                           {"turn_id":"T-1","kind":"message","author":"operator","status":"complete",
                            "format":"plain","untrusted":false,"text":"Add SSE","steps":[],"refs":[],
                            "at":"2026-10-16T10:00:00Z"}],
                         "proposal":{"turn_id":"T-3","reply_turn_id":"T-2","kind":"revision",
                           "summary":"a test per transport","before":"one transport",
                           "after":"websocket and SSE",
                           "applies":{"key":"chat.applies.after_task","args":{"task_id":"TASK-4"}},
                           "state":"open"}}
                        """;

    private static final String CHAT_ENDED = """
                        {"id":"C-2","subject":{"kind":"plan","id":"docs-typo"},
                         "role":{"name":"replan","model":"sonnet"},"scope":"archive","state":"chat.read-back",
                         "context":{"knows":[],"as_of":"2026-10-16T10:00:00Z"},"turns":[
                           {"turn_id":"T-1","kind":"message","author":"operator","status":"complete",
                            "format":"plain","untrusted":false,"text":"Why was it cancelled?","steps":[],
                            "refs":[],"at":"2026-10-16T10:00:00Z"}],
                         "proposal":{"turn_id":"T-3","reply_turn_id":"T-2","kind":"follow_up",
                           "summary":"a follow-up","after":"Add the provider on top of relay-v2",
                           "state":"open"}}
                        """;

    private static final String CHAT_STREAMING = """
                        {"id":"C-1","subject":{"kind":"plan","id":"p"},
                         "role":{"name":"replan","model":"sonnet"},"scope":"plan","state":"chat.reply",
                         "context":{"knows":[],"as_of":"2026-10-16T10:00:00Z"},"turns":[
                           {"turn_id":"T-1","kind":"message","author":"operator","status":"complete",
                            "format":"plain","untrusted":false,"text":"Can the test cover SSE?","steps":[],
                            "refs":[],"at":"2026-10-16T10:00:00Z"},
                           {"turn_id":"T-2","kind":"message","author":"replan","status":"streaming",
                            "format":"markdown","untrusted":false,"model":"sonnet","text":"Yes, I",
                            "steps":[],"refs":[],"at":"2026-10-16T10:01:00Z"}]}
                        """;

    private static final String CHAT_ANSWERED_PROPOSAL = """
                        {"id":"C-1","subject":{"kind":"plan","id":"p"},
                         "role":{"name":"replan","model":"sonnet"},"scope":"plan","state":"chat.turn",
                         "context":{"knows":[],"as_of":"2026-10-16T10:00:00Z"},"turns":[
                           {"turn_id":"T-1","kind":"message","author":"operator","status":"complete",
                            "format":"plain","untrusted":false,"text":"Add SSE","steps":[],"refs":[],
                            "at":"2026-10-16T10:00:00Z"}],
                         "proposal":{"turn_id":"T-3","reply_turn_id":"T-2","kind":"revision",
                           "summary":"a test per transport","after":"websocket and SSE",
                           "state":"accepted"}}
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

    private static final String ACTIONS_PATH = "/actions";
    private static final String NEXT_PATH = "/actions/next";

    private static final String EPIC_WITHOUT_RUNNING =
                        EPIC.replace("\"scope\":3,\"running\":3", "\"scope\":3");

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

    private FakeRuntimeServer serveNext() throws IOException {
        return fixture.start((request, response) -> {
            if (request.target().endsWith(ACTIONS_PATH)) {
                response.json(200, EPIC_ACTIONS);
            } else if (request.target().endsWith(NEXT_PATH)) {
                response.json(200, EPIC_ACTIONS);
            } else {
                response.send(404, null, "");
            }
        });
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
        script("d\nnot relevant\nq\n");
        assertEquals(0, ReviewDialogue.run(context, new PrintWriter(out), "e", "findings", false));
        assertTrue(out.toString().contains("F-1 dropped"), out.toString());
    }

    @Test
    @DisplayName("a chat message is answered, and an empty line ends the chat")
    void chatTurn() throws Exception {
        var answered = new AtomicBoolean();
        serve((request, response) -> {
            var path = request.target();
            if (path.endsWith("/chat") && "GET".equals(request.method())) {
                response.json(200, answered.get() ? CHAT_ANSWERED : CHAT);
            } else if (path.endsWith("/chat/turns")) {
                answered.set(true);
                response.json(202, "{\"turn_id\":\"T-3\"}");
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
        // The point the proposal applies at is read from its runtime key, not printed as one.
        assertTrue(text.contains("applies after the current task"), text);
        assertFalse(text.contains("chat.applies."), text);
        assertTrue(text.contains("The chat is kept with p"), text);
    }

    @Test
    @DisplayName("a chat about ended work proposes a follow-up plan")
    void chatEndedFollowUp() throws Exception {
        var accepted = new AtomicBoolean();
        serve((request, response) -> {
            var path = request.target();
            if (path.endsWith("/chat")) {
                response.json(200, CHAT_ENDED);
            } else if (path.endsWith("/proposal/accept")) {
                accepted.set(true);
                response.json(200, CHAT_ANSWERED_PROPOSAL);
            } else {
                response.send(404, null, "");
            }
        });
        virtual.type("\n");
        assertNull(WorkChatDialogue.run(context, new PrintWriter(out), "plan", "docs-typo", true, false));
        // The accept reaches the runtime, which opens the start; the terminal says so once.
        assertTrue(accepted.get(), "the follow-up proposal was not accepted on the runtime");
        assertTrue(out.toString().contains("read only"), out.toString());
        assertTrue(out.toString().contains("A new plan is being started with this description"),
                            out.toString());
    }

    @Test
    @DisplayName("an answered proposal is not read back again: the chat asks for a message")
    void chatAnsweredProposalAsksForAMessage() throws Exception {
        serve((request, response) -> {
            var path = request.target();
            if (path.endsWith("/chat")) {
                response.json(200, CHAT_PROPOSAL);
            } else if (path.endsWith("/proposal/accept")) {
                response.json(200, CHAT_ANSWERED_PROPOSAL);
            } else {
                response.send(404, null, "");
            }
        });
        virtual.type("\n\n");
        assertNull(WorkChatDialogue.run(context, new PrintWriter(out), "plan", "p", false, false));
        var text = out.toString();
        // The read-back is drawn once, and the answered proposal is not offered a second time.
        assertEquals(1, times(text, "Proposed change"), text);
        assertTrue(text.contains("The chat is kept with p"), text);
    }

    @Test
    @DisplayName("a chart turn follows the reply until its turn stops streaming")
    void chatFollowsTheStreamingReply() throws Exception {
        var reads = new AtomicInteger();
        serve((request, response) -> {
            var path = request.target();
            if (path.endsWith("/chat") && "GET".equals(request.method())) {
                // The first read still shows the reply being written; the next one has it complete.
                response.json(200, reads.incrementAndGet() < 3 ? CHAT_STREAMING : CHAT);
            } else if (path.endsWith("/chat/turns")) {
                response.json(202, "{\"turn_id\":\"T-1\"}");
            } else {
                response.send(404, null, "");
            }
        });
        var client = new WorkActionsClient(context);
        var chat = client.chatTurn("plan", "p", "Can the test cover SSE?");
        assertEquals(2, chat.turns().size());
        assertTrue(reads.get() >= 3, "the reply was not read again while it streamed: " + reads.get());
    }

    @Test
    @DisplayName("a chat without a proposal, and a proposal without an applies point, render")
    void chatRenderWithoutProposalOrApplies() throws Exception {
        WorkChatDialogue.render(context, new PrintWriter(out), ChatResource.fromJson(CHAT));
        WorkChatDialogue.render(context, new PrintWriter(out), ChatResource.fromJson(CHAT_ENDED));
        var text = out.toString();
        assertTrue(text.contains("Yes, I propose a second test."), text);
        assertTrue(text.contains("Proposed follow-up plan"), text);
        assertFalse(text.contains("chat.applies."), text);
    }

    /**
     * @param text the drawn transcript
     * @param part the text to count
     * @return how often the transcript carries the part
     */
    private static int times(String text, String part) {
        var count = 0;
        var index = text.indexOf(part);
        while (index >= 0) {
            count++;
            index = text.indexOf(part, index + part.length());
        }
        return count;
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
            } else if (path.contains("/plans/p/decisions")) {
                response.json(200, "[{\"id\":\"Q-009\",\"title\":\"Delete published v1 endpoints?\"}]");
            } else if (path.contains("/plans/quiet/decisions")) {
                response.json(200, "[]");
            } else if (path.startsWith("/api/v1/decisions?plan=legacy")) {
                response.json(200, "[{\"id\":\"Q-011\"}]");
            } else if (path.endsWith("/holds")) {
                response.json(200, STALLED_HOLD);
            } else if (path.contains("/plans/conflict/settings") && "GET".equals(request.method())) {
                response.json(200, SETTINGS);
            } else if (path.contains("/plans/conflict/settings")) {
                response.json(409, PlanSettingsUpdateResult.conflict(8, "landing.main").toJson());
            } else if (path.contains("/plans/refused/settings") && "GET".equals(request.method())) {
                response.json(200, SETTINGS);
            } else if (path.contains("/plans/refused/settings")) {
                response.json(400, "{\"error\":\"refused\",\"reason\":\"the base branch is protected\"}");
            } else if (path.endsWith("/settings") && "GET".equals(request.method())) {
                response.json(200, SETTINGS);
            } else if (path.endsWith("/settings")) {
                response.json(200, PlanSettingsUpdateResult.applied(8).toJson());
            } else if (path.contains("/review")) {
                response.json(200, REVIEW);
            } else if (path.endsWith("/chat")) {
                response.json(200, CHAT);
            } else if (path.endsWith("/chat/turns")) {
                response.json(202, "{\"turn_id\":\"T-1\"}");
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
        assertEquals("Q-009", client.openDecision("p"));
        assertNull(client.openDecision("quiet"));
        assertEquals("Q-011", client.openDecision("legacy"));
        List<HoldRecord> holds = client.holds("p");
        assertEquals(1, holds.size());
        assertNotNull(client.hold("p"));
        assertTrue(client.resolveHold("p", "H-1", "retry", null).released());
        PlanSettingsRecord settings = client.settings("p");
        assertEquals(7, settings.revision());
        assertTrue(client.updateSettings("p", 7, "landing", "landing.main").result().applied());
        assertTrue(client.updateSettings("conflict", 7, "landing", "landing.main").result().conflict());
        assertEquals("the base branch is protected",
                            client.updateSettings("refused", 7, "landing", "landing.main").refusalReason());
        ReviewResource review = client.review("e", "findings");
        assertEquals(1, review.items().size());
        assertEquals(1, client.disposition("e", "F-1", "kept", null).items().size());
        assertEquals(1, client.commitReview("e").items().size());
        assertEquals(2, client.chat("plan", "p").turns().size());
        assertEquals(2, client.chatTurn("plan", "p", "hello").turns().size());
        assertEquals(2, client.chatProposal("plan", "p", "T-3", true).turns().size());
        assertEquals(2, client.chatProposal("plan", "p", "T-3", false).turns().size());
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

    @Test
    @DisplayName("cancelling an epic asks for the plans and the reason")
    void workActionCancelEpic() throws Exception {
        serve((request, response) -> {
            if (request.target().endsWith("/actions")) {
                response.json(200, EPIC_ACTIONS);
            } else if (request.target().endsWith("/actions/cancel")) {
                response.json(200, EPIC_ACTIONS);
            } else {
                response.send(404, null, "");
            }
        });
        virtual.type("1\nbecause the scope moved\ny\n");
        assertEquals(0, dialogue("epic", "e", "cancel"), out.toString());
        var text = out.toString();
        assertTrue(text.contains("Cancel e?"), text);
        assertTrue(text.contains("e cancelled and archived"), text);
    }

    @Test
    @DisplayName("cancelling an epic can let its plans run on")
    void workActionCancelEpicKeepsPlans() throws Exception {
        var body = new String[1];
        serve((request, response) -> {
            if (request.target().endsWith("/actions/cancel")) {
                body[0] = request.bodyText();
                response.json(200, EPIC_ACTIONS);
            } else if (request.target().endsWith("/actions")) {
                response.json(200, EPIC_ACTIONS);
            } else {
                response.send(404, null, "");
            }
        });
        virtual.type("9\n2\nbecause the scope moved\ny\n");
        assertEquals(0, dialogue("epic", "e", "cancel"), out.toString());
        assertTrue(out.toString().contains("9 is not offered here: 1-2, q, ?"), out.toString());
        assertNotNull(body[0], out.toString());
        assertTrue(body[0].contains("\"with_plans\":false"), body[0]);
    }

    @Test
    @DisplayName("an archive that does not pass names what to decide")
    void workActionArchiveNotPassed() throws Exception {
        serve((request, response) -> {
            if (request.target().endsWith("/actions")) {
                response.json(200, EPIC_ACTIONS);
            } else if (request.target().endsWith("/archive-check")) {
                response.json(200, ARCHIVE_CHECK.replace("\"passed\":true,\n", "\"passed\":false,\n")
                                    .replace("\"passed\":true}]", "\"passed\":false}]"));
            } else {
                response.send(404, null, "");
            }
        });
        assertEquals(1, dialogue("epic", "e", "archive"));
        assertTrue(out.toString().contains("cannot be archived yet"), out.toString());
    }

    @Test
    @DisplayName("an archive whose completion review runs first stops there")
    void workActionArchiveReviewFirst() throws Exception {
        serve((request, response) -> {
            if (request.target().endsWith("/actions")) {
                response.json(200, EPIC_ACTIONS);
            } else if (request.target().endsWith("/archive-check")) {
                response.json(200, ARCHIVE_CHECK.replace("\"completion_review_first\":false",
                                    "\"completion_review_first\":true"));
            } else {
                response.send(404, null, "");
            }
        });
        assertEquals(0, dialogue("epic", "e", "archive"));
        assertTrue(out.toString().contains("completion review runs first"), out.toString());
    }

    @Test
    @DisplayName("next with nothing launchable names why")
    void workActionNextNone() throws Exception {
        serve((request, response) -> {
            if (request.target().endsWith("/actions")) {
                response.json(200, EPIC_ACTIONS);
            } else if (request.target().endsWith("/actions/next")) {
                response.json(200, EPIC_ACTIONS);
            } else {
                response.send(404, null, "");
            }
        });
        var noWork = EPIC.replace("\"status\":\"staged\",\"wait_reason\":\"none\"",
                            "\"status\":\"shipped\"");
        assertEquals(0, WorkActionDialogue.run(context, new PrintWriter(out), "epic", "e", "next", false,
                            EpicResource.fromJson(noWork), null));
        assertTrue(out.toString().contains("No plan can start"), out.toString());
    }

    @Test
    @DisplayName("next over an unknown running count asks and sends nothing until answered")
    void workActionNextUnknownRunning() throws Exception {
        var server = serveNext();
        virtual.type("n\n");
        var unknown = EPIC_WITHOUT_RUNNING;
        assertEquals(0, WorkActionDialogue.run(context, new PrintWriter(out), "epic", "e", "next", false,
                            EpicResource.fromJson(unknown), null));
        var text = out.toString();
        assertTrue(text.contains("How many plans run is not known"), text);
        assertFalse(server.requests().stream().anyMatch(request -> request.target().endsWith("/actions/next")),
                            server.requests().toString());
    }

    @Test
    @DisplayName("an empty answer over an unknown running count sends nothing")
    void workActionNextUnknownRunningEmptyAnswer() throws Exception {
        var server = serveNext();
        virtual.type("\nn\n");
        var unknown = EPIC_WITHOUT_RUNNING;
        assertEquals(0, WorkActionDialogue.run(context, new PrintWriter(out), "epic", "e", "next", false,
                            EpicResource.fromJson(unknown), null));
        var text = out.toString();
        assertTrue(text.contains("How many plans run is not known"), text);
        assertFalse(server.requests().stream().anyMatch(request -> request.target().endsWith("/actions/next")),
                            server.requests().toString());
    }

    @Test
    @DisplayName("an answered raise over an unknown running count sends raise_scope")
    void workActionNextUnknownRunningRaise() throws Exception {
        var server = serveNext();
        virtual.type("r\n");
        var unknown = EPIC_WITHOUT_RUNNING;
        assertEquals(0, WorkActionDialogue.run(context, new PrintWriter(out), "epic", "e", "next", false,
                            EpicResource.fromJson(unknown), null));
        assertTrue(server.requests().stream().anyMatch(request -> request.target().endsWith("/actions/next")
                                                && request.bodyText().contains("\"raise_scope\":true")),
                            server.requests().toString());
        assertTrue(out.toString().contains("next-plan started (? of 4 at once)"), out.toString());
    }

    @Test
    @DisplayName("a full scope over a known count raises on the raise key")
    void workActionNextKnownFullRaise() throws Exception {
        var server = serveNext();
        virtual.type("r\n");
        assertEquals(0, WorkActionDialogue.run(context, new PrintWriter(out), "epic", "e", "next", false,
                            EpicResource.fromJson(EPIC), null));
        var text = out.toString();
        assertTrue(text.contains("e runs 3 of 3 at once"), text);
        assertTrue(text.contains("next-plan started (4 of 4 at once)"), text);
        assertTrue(server.requests().stream().anyMatch(request -> request.target().endsWith(NEXT_PATH)
                                                && request.bodyText().contains("\"raise_scope\":true")),
                            server.requests().toString());
    }

    @Test
    @DisplayName("next without the epic resource says it is still being read and sends nothing")
    void workActionNextWithoutResource() throws Exception {
        var server = serveNext();
        assertEquals(0, WorkActionDialogue.run(context, new PrintWriter(out), "epic", "e", "next", false,
                            null, null));
        var text = out.toString();
        assertTrue(text.contains("The epic is still being read"), text);
        assertFalse(server.requests().stream().anyMatch(request -> request.target().endsWith(NEXT_PATH)),
                            server.requests().toString());
    }

    @Test
    @DisplayName("a scope-blocked plan is offered for a raise")
    void workActionNextScopeBlocked() throws Exception {
        var server = serveNext();
        virtual.type("r\n");
        var blocked = EPIC.replace("\"wait_reason\":\"none\"", "\"wait_reason\":\"scope\"");
        assertEquals(0, WorkActionDialogue.run(context, new PrintWriter(out), "epic", "e", "next", false,
                            EpicResource.fromJson(blocked), null));
        var text = out.toString();
        assertTrue(text.contains("e runs 3 of 3 at once"), text);
        assertTrue(text.contains("next-plan started (4 of 4 at once)"), text);
        assertTrue(server.requests().stream().anyMatch(request -> request.target().endsWith(NEXT_PATH)
                                                && request.bodyText().contains("\"raise_scope\":true")),
                            server.requests().toString());
    }

    @Test
    @DisplayName("an unrecognised answer over an unknown running count is reported and asked again")
    void workActionNextInvalidAnswerReprompts() throws Exception {
        var server = serveNext();
        virtual.type("x\nr\n");
        assertEquals(0, WorkActionDialogue.run(context, new PrintWriter(out), "epic", "e", "next", false,
                            EpicResource.fromJson(EPIC_WITHOUT_RUNNING), null));
        var text = out.toString();
        assertTrue(text.contains("Type r to raise the scope or n to leave it."), text);
        assertTrue(server.requests().stream().anyMatch(request -> request.target().endsWith(NEXT_PATH)
                                                && request.bodyText().contains("\"raise_scope\":true")),
                            server.requests().toString());
    }

    @Test
    @DisplayName("automatic off keeps what runs and starts nothing")
    void workActionAutoOff() throws Exception {
        serve((request, response) -> {
            if (request.target().endsWith("/actions")) {
                response.json(200, EPIC_ACTIONS);
            } else if (request.target().endsWith("/actions/auto")) {
                response.json(200, EPIC_ACTIONS);
            } else {
                response.send(404, null, "");
            }
        });
        var running = EpicResource.fromJson(EPIC.replace("\"auto_start\":false", "\"auto_start\":true"));
        assertEquals(0, WorkActionDialogue.run(context, new PrintWriter(out), "epic", "e", "auto", false,
                            running, null));
        assertTrue(out.toString().contains("starts no plan by itself any more"), out.toString());
    }

    @Test
    @DisplayName("a common-files answer that is not a number is refused")
    void workActionCommonFilesInvalid() throws Exception {
        serve((request, response) -> {
            if (request.target().endsWith("/actions")) {
                response.json(200, EPIC_ACTIONS);
            } else {
                response.send(404, null, "");
            }
        });
        virtual.type("many\n");
        assertEquals(1, WorkActionDialogue.run(context, new PrintWriter(out), "epic", "e", "common_files",
                            false, EpicResource.fromJson(EPIC), null));
        assertTrue(out.toString().contains("Give the number"), out.toString());
    }

    @Test
    @DisplayName("the settings action hands over to the settings dialogue")
    void workActionSettings() throws Exception {
        serve((request, response) -> {
            var path = request.target();
            if (path.endsWith("/actions")) {
                response.json(200, PLAN_ACTIONS);
            } else if (path.endsWith("/settings") && "GET".equals(request.method())) {
                response.json(200, SETTINGS);
            } else if (path.endsWith("/settings")) {
                response.json(200, PlanSettingsUpdateResult.applied(8).toJson());
            } else {
                response.send(404, null, "");
            }
        });
        virtual.type("1\n1\n2\n");
        assertEquals(0, dialogue("plan", "p", "settings"));
        assertTrue(out.toString().contains("version becomes 8"), out.toString());
    }

    @Test
    @DisplayName("the findings action opens the review")
    void workActionFindings() throws Exception {
        serve((request, response) -> {
            var path = request.target();
            if (path.endsWith("/actions")) {
                response.json(200, EPIC_ACTIONS.replace("{\"action\":\"next\",\"available\":true},",
                                    "{\"action\":\"findings\",\"available\":true},"
                                                        + "{\"action\":\"next\",\"available\":true},"));
            } else if (path.contains("/review")) {
                response.json(200, REVIEW_KEPT);
            } else {
                response.send(404, null, "");
            }
        });
        virtual.type("y\n");
        assertEquals(0, WorkActionDialogue.run(context, new PrintWriter(out), "epic", "e", "findings", false,
                            EpicResource.fromJson(EPIC), null));
        assertTrue(out.toString().contains("written to the ledger"), out.toString());
    }

    @Test
    @DisplayName("the remaining review proposals are accepted once")
    void reviewRemaining() throws Exception {
        var two = REVIEW.replace("\"total\":1", "\"total\":2").replace("]}", "," +
                            "{\"item_id\":\"F-2\",\"register\":\"findings\",\"statement\":\"Second\"," +
                            "\"rules\":[],\"origin\":\"landing\",\"trust\":\"trusted\",\"state\":\"open\"," +
                            "\"proposal\":\"keep\"}]}");
        serve((request, response) -> {
            var path = request.target();
            if (path.contains("/review?")) {
                response.json(200, two);
            } else if (path.contains("/disposition")) {
                response.json(200, REVIEW_KEPT);
            } else if (path.endsWith("/review/commit")) {
                response.json(200, REVIEW_COMMITTED);
            } else {
                response.send(404, null, "");
            }
        });
        virtual.type("r\ny\ny\n");
        assertEquals(0, ReviewDialogue.run(context, new PrintWriter(out), "e", "findings", false));
        assertTrue(out.toString().contains("kept as a finding"), out.toString());
    }

    @Test
    @DisplayName("an untrusted item is printed between quotes")
    void reviewUntrusted() throws Exception {
        serve((request, response) -> {
            if (request.target().contains("/review")) {
                response.json(200, REVIEW.replace("\"trust\":\"trusted\"", "\"trust\":\"untrusted\""));
            } else {
                response.send(404, null, "");
            }
        });
        script("q\n");
        ReviewDialogue.run(context, new PrintWriter(out), "e", "findings", false);
        assertTrue(out.toString().contains("\"Reconnect loses the last event\""), out.toString());
    }

    @Test
    @DisplayName("a plan's own value is removed with the inherited choice")
    void settingsRemoved() throws Exception {
        serve((request, response) -> {
            var path = request.target();
            if (path.endsWith("/settings") && "GET".equals(request.method())) {
                response.json(200, SETTINGS);
            } else if (path.endsWith("/settings")) {
                response.json(200, PlanSettingsUpdateResult.applied(8).toJson());
            } else {
                response.send(404, null, "");
            }
        });
        virtual.type("1\n0\n");
        assertEquals(0, PlanSettingsDialogue.run(context, new PrintWriter(out), "p", "plan-defaults", false));
        assertTrue(out.toString().contains("uses the inherited value"), out.toString());
    }

    @Test
    @DisplayName("a change that is too late changes nothing")
    void settingsTooLate() throws Exception {
        serve((request, response) -> {
            var path = request.target();
            if (path.endsWith("/settings") && "GET".equals(request.method())) {
                response.json(200, SETTINGS);
            } else if (path.endsWith("/settings")) {
                response.json(409, PlanSettingsUpdateResult.tooLate("landing").toJson());
            } else {
                response.send(404, null, "");
            }
        });
        virtual.type("1\n2\n");
        assertEquals(0, PlanSettingsDialogue.run(context, new PrintWriter(out), "p", "plan-defaults", false));
        assertTrue(out.toString().contains("Too late"), out.toString());
    }

    @Test
    @DisplayName("a conflict applied again writes the value")
    void settingsConflictApplyAgain() throws Exception {
        var attempts = new AtomicInteger();
        serve((request, response) -> {
            var path = request.target();
            if (path.endsWith("/settings") && "GET".equals(request.method())) {
                response.json(200, SETTINGS);
            } else if (path.endsWith("/settings")) {
                if (attempts.getAndIncrement() == 0) {
                    response.json(409, PlanSettingsUpdateResult.conflict(8, "landing.main").toJson());
                } else {
                    response.json(200, PlanSettingsUpdateResult.applied(9).toJson());
                }
            } else {
                response.send(404, null, "");
            }
        });
        virtual.type("1\n2\n\n");
        assertEquals(0, PlanSettingsDialogue.run(context, new PrintWriter(out), "p", "plan-defaults", false));
        assertTrue(out.toString().contains("version becomes 9"), out.toString());
    }

    @Test
    @DisplayName("the group is asked when the verb names none")
    void settingsGroupAsked() throws Exception {
        serve((request, response) -> {
            var path = request.target();
            if (path.endsWith("/settings") && "GET".equals(request.method())) {
                response.json(200, SETTINGS);
            } else if (path.endsWith("/settings")) {
                response.json(200, PlanSettingsUpdateResult.applied(8).toJson());
            } else {
                response.send(404, null, "");
            }
        });
        virtual.type("1\n1\n2\n");
        assertEquals(0, PlanSettingsDialogue.run(context, new PrintWriter(out), "p", null, false));
        assertTrue(out.toString().contains("Which settings of p?"), out.toString());
    }

    @Test
    @DisplayName("a chat proposal is rejected and the chat goes on")
    void chatReject() throws Exception {
        serve((request, response) -> {
            var path = request.target();
            if (path.endsWith("/chat")) {
                response.json(200, CHAT_PROPOSAL);
            } else if (path.endsWith("/proposal/reject")) {
                response.json(200, CHAT);
            } else {
                response.send(404, null, "");
            }
        });
        virtual.type("r\n\n");
        assertNull(WorkChatDialogue.run(context, new PrintWriter(out), "plan", "p", false, false));
        assertTrue(out.toString().contains("The chat is kept with p"), out.toString());
    }

    @Test
    @DisplayName("a chat can be rendered without asking")
    void chatRender() throws Exception {
        WorkChatDialogue.render(context, new PrintWriter(out), ChatResource.fromJson(CHAT_PROPOSAL));
        assertTrue(out.toString().contains("Proposed change"), out.toString());
        assertTrue(out.toString().contains("websocket and SSE"), out.toString());
    }

    @Test
    @DisplayName("the epic view closes rows and expands every section")
    void epicViewCloseAndExpand() throws Exception {
        var view = new EpicViewLevel(CLOCK, EpicResource.fromJson(EPIC));
        view.expandAll();
        assertTrue(view.rows("en").stream().anyMatch(row -> row.line().contains("Tokens")),
                            view.rows("en").toString());
        assertNotNull(view.closeRow(EpicViewLevel.SOURCES_ID));
        assertNotNull(view.closeRow(EpicViewLevel
                            .WORKSTREAM_PREFIX + "WS-1"));
    }

    @Test
    @DisplayName("plan cancel asks its reason, the pull request, and confirms")
    void verbEndAsksReason() throws Exception {
        serve((request, response) -> {
            if (request.target().endsWith("/plans/p/cancel")) {
                response.json(200, "{}");
            } else {
                response.send(404, null, "");
            }
        });
        virtual.type("the approach changed\ny\n");
        var code = PmOperator.execute(context, new PrintWriter(out), new PrintWriter(new StringWriter()),
                            "plan", "cancel", "p");
        assertEquals(0, code, out.toString());
        assertTrue(out.toString().contains("cancelled and archived"), out.toString());
    }

    @Test
    @DisplayName("plan pause without a flag asks its point")
    void verbPauseAsksPoint() throws Exception {
        serve((request, response) -> {
            if (request.target().endsWith("/plans/p/pause")) {
                response.json(200, "{}");
            } else {
                response.send(404, null, "");
            }
        });
        virtual.type("2\n");
        var code = PmOperator.execute(context, new PrintWriter(out), new PrintWriter(new StringWriter()),
                            "plan", "pause", "p");
        assertEquals(0, code, out.toString());
        assertTrue(out.toString().contains("pausing after the task"), out.toString());
    }

    @Test
    @DisplayName("config <aspect> --plan opens the plan's settings")
    void verbConfigPlan() throws Exception {
        serve((request, response) -> {
            var path = request.target();
            if (path.endsWith("/settings") && "GET".equals(request.method())) {
                response.json(200, SETTINGS);
            } else if (path.endsWith("/settings")) {
                response.json(200, PlanSettingsUpdateResult.applied(8).toJson());
            } else {
                response.send(404, null, "");
            }
        });
        virtual.type("1\n2\n");
        var code = PmOperator.execute(context, new PrintWriter(out), new PrintWriter(new StringWriter()),
                            "config", "plan-defaults", "--plan", "p");
        assertEquals(0, code, out.toString());
        assertTrue(out.toString().contains("version becomes 8"), out.toString());
    }

    @Test
    @DisplayName("config roles --plan asks that group and finds nothing to change")
    void verbConfigPlanEmptyGroup() throws Exception {
        serve((request, response) -> {
            if (request.target().endsWith("/settings")) {
                response.json(200, SETTINGS);
            } else {
                response.send(404, null, "");
            }
        });
        var code = PmOperator.execute(context, new PrintWriter(out), new PrintWriter(new StringWriter()),
                            "config", "roles", "--plan", "p");
        assertEquals(0, code, out.toString());
    }

    /**
     * Types the keys and ends the script, so a dialogue that asks once more than the test expects
     * meets the end of the input at once instead of waiting for keys.
     */
    private void script(String keys) {
        virtual.type(keys);
        virtual.endOfInput();
    }

    /** Serves the holds read by read in order (the last one repeats) and records every request. */
    private FakeRuntimeServer serveHolds(HoldResolutionResult result, String... holds) throws IOException {
        var reads = new AtomicInteger();
        return fixture.start((request, response) -> {
            var path = request.target();
            if (path.endsWith("/holds")) {
                response.json(200, holds[Math.min(reads.getAndIncrement(), holds.length - 1)]);
            } else if (path.endsWith("/resolve")) {
                response.json(200, result.toJson());
            } else {
                response.send(404, null, "");
            }
        });
    }

    private static boolean resolved(FakeRuntimeServer server) {
        return server.requests().stream().anyMatch(request -> request.target().endsWith("/resolve"));
    }

    private static String resolveBody(FakeRuntimeServer server) {
        return server.requests().stream().filter(request -> request.target().endsWith("/resolve"))
                            .map(FakeRuntimeServer.Request::bodyText).findFirst().orElse("");
    }

    @Nested
    @DisplayName("the hold dialogue")
    @EnableGeneratorController
    class HoldDialogueBehaviour {

        private static final String END_HOLD = """
                            [{"hold_id":"H-3","subject_id":"p","subject_kind":"plan","kind":"stalled",
                              "cause":"the worker stopped","attempts":0,"acts":["retry","end_plan"]}]
                            """;

        private final HoldResolutionResult released = new HoldResolutionResult("H-1", true, null, "p");

        @Test
        @DisplayName("an input the hold does not offer is answered with one line and asked again")
        void notOfferedAskedAgain() throws Exception {
            var server = serveHolds(released, STALLED_HOLD);
            var input = Generators.letterStrings(8, 12).next();
            script(input + "\nr\n");

            assertEquals(0, HoldDialogue.run(context, new PrintWriter(out), "p", null, false));

            var text = out.toString();
            assertTrue(text.contains("x) End the plan…   › "), text);
            assertFalse(text.contains("›  ›"), text);
            assertTrue(text.contains(input + " is not offered here: r, w, x, q"), text);
            assertTrue(resolveBody(server).contains("\"act\":\"retry\""), server.requests().toString());
        }

        @Test
        @DisplayName("the wire name of an act is not a key: it is not offered and nothing is sent")
        void wireNameNotOffered() throws Exception {
            var server = serveHolds(released, STALLED_HOLD);
            script("recycle\nq\n");

            assertEquals(0, HoldDialogue.run(context, new PrintWriter(out), "p", null, false));

            assertTrue(out.toString().contains("recycle is not offered here"), out.toString());
            assertFalse(resolved(server), server.requests().toString());
        }

        @Test
        @DisplayName("German v leaves the hold and sends nothing")
        void germanLeave() throws Exception {
            var server = serveHolds(released, STALLED_HOLD);
            var env = new HashMap<>(fixture.environment());
            env.put("LANG", "de_DE.UTF-8");
            var german = new OperatorContext(env, fixture.base().getParent(), true, ClientEnvironment.capture(env),
                                virtual::terminal, null);
            script("v\n");

            assertEquals(0, HoldDialogue.run(german, new PrintWriter(out), "p", null, false));

            assertFalse(out.toString().contains("nicht angeboten"), out.toString());
            assertFalse(resolved(server), server.requests().toString());
        }

        @Test
        @DisplayName("the act a menu chose for a hold that is gone is asked, not sent to the hold that holds now")
        void preselectedActOfGoneHoldIsAsked() throws Exception {
            var server = serveHolds(released, STALLED_HOLD);
            script("q\n");

            assertEquals(0, HoldDialogue.run(context, new PrintWriter(out), "p", "H-9", "retry", false));

            assertTrue(out.toString().contains("the worker stopped"), out.toString());
            assertFalse(resolved(server), server.requests().toString());
        }

        @Test
        @DisplayName("an act the hold does not offer is asked, not sent")
        void preselectedActNotOfferedIsAsked() throws Exception {
            var server = serveHolds(released, STALLED_HOLD);
            script("q\n");

            assertEquals(0, HoldDialogue.run(context, new PrintWriter(out), "p", null, "accept", false));

            assertFalse(resolved(server), server.requests().toString());
        }

        @Test
        @DisplayName("the act a menu chose for the hold it names is sent to that hold")
        void preselectedActOfNamedHold() throws Exception {
            var two = STALLED_HOLD.replace("]", ",{\"hold_id\":\"H-2\",\"subject_id\":\"p\",\"kind\":\"stalled\","
                                + "\"cause\":\"the second\",\"attempts\":0,\"acts\":[\"retry\"]}]");
            var server = serveHolds(released, two);

            assertEquals(0, HoldDialogue.run(context, new PrintWriter(out), "p", "H-2", "retry", false));

            assertTrue(server.requests().stream().anyMatch(request -> request.target().endsWith("/holds/H-2/resolve")),
                                server.requests().toString());
            assertTrue(out.toString().contains("the second"), out.toString());
        }

        @Test
        @DisplayName("ending the plan confirms, then sends the reason")
        void endPlanSendsReason() throws Exception {
            var server = serveHolds(released, END_HOLD);
            var reason = Generators.letterStrings(5, 20).next();
            script("x\ny\n" + reason + "\n");

            assertEquals(0, HoldDialogue.run(context, new PrintWriter(out), "p", null, false));

            assertTrue(resolveBody(server).contains("\"reason\":\"" + reason + "\""), resolveBody(server));
            assertTrue(out.toString().contains("p goes on"), out.toString());
            assertFalse(out.toString().contains("goes on with the change"), out.toString());
        }

        @Test
        @DisplayName("ending the plan without a reason is refused and sends nothing")
        void endPlanBlankReasonRefused() throws Exception {
            var server = serveHolds(released, END_HOLD);
            script("x\ny\n\n");

            assertEquals(0, HoldDialogue.run(context, new PrintWriter(out), "p", null, false));

            assertTrue(out.toString().contains("The reason is required."), out.toString());
            assertFalse(resolved(server), server.requests().toString());
        }

        @ParameterizedTest(name = "{0} declined")
        @CsvSource({"x,n", "c,n", "x,later"})
        @DisplayName("a declined confirmation sends nothing")
        void declinedSendsNothing(String key, String answer) throws Exception {
            var server = serveHolds(released, "x".equals(key) ? END_HOLD : INTEGRITY_HOLD);
            script(key + "\n" + answer + "\n");

            assertEquals(0, HoldDialogue.run(context, new PrintWriter(out), "p", null, false));

            assertFalse(resolved(server), server.requests().toString());
        }

        @Test
        @DisplayName("a refused act names the runtime's reason and fails")
        void refusedAct() throws Exception {
            var server = fixture.start((request, response) -> {
                if (request.target().endsWith("/holds")) {
                    response.json(200, STALLED_HOLD);
                } else {
                    response.json(409, "{\"error\":\"conflict\",\"reason\":\"the hold moved on\"}");
                }
            });
            script("r\n");

            assertEquals(1, HoldDialogue.run(context, new PrintWriter(out), "p", null, false));

            assertTrue(out.toString().contains("✗"), out.toString());
            assertTrue(resolved(server), server.requests().toString());
        }

        @Test
        @DisplayName("a hold that is still held but no longer listed lets the plan go on")
        void stillHeldThenGone() throws Exception {
            serveHolds(new HoldResolutionResult("H-1", false, null, "p"), STALLED_HOLD, "[]");
            script("r\n");

            assertEquals(0, HoldDialogue.run(context, new PrintWriter(out), "p", null, false));

            var text = out.toString();
            assertTrue(text.contains("Still held: "), text);
            assertTrue(text.contains("p goes on"), text);
        }

        @Test
        @DisplayName("a plan that is not held says so")
        void notHeld() throws Exception {
            serveHolds(released, "[]");

            assertEquals(0, HoldDialogue.run(context, new PrintWriter(out), "p", null, false));

            assertTrue(out.toString().contains("p is not held."), out.toString());
        }

        @Test
        @DisplayName("without input the act it needs stops the dialogue")
        void noInputStops() throws Exception {
            var server = serveHolds(released, STALLED_HOLD);

            assertEquals(StartDialogue.EXIT_MISSING_INPUT,
                                HoldDialogue.run(context, new PrintWriter(out), "p", null, true));

            assertFalse(resolved(server), server.requests().toString());
        }

        @Test
        @DisplayName("a failed landing shows its pull request and asks again")
        void showPullRequest() throws Exception {
            var number = Generators.integers(1, 9999).next();
            var landing = """
                                [{"hold_id":"H-4","subject_id":"p","kind":"landing_failed","cause":"checks failed",
                                  "attempts":3,"last_tried_at":"2026-10-20T11:00:00Z",
                                  "pull_request_url":"https://example.org/o/r/pull/%d",
                                  "acts":["show_pr","end_plan"]}]
                                """.formatted(number);
            var server = serveHolds(released, landing);
            script("p\nq\n");

            assertEquals(0, HoldDialogue.run(context, new PrintWriter(out), "p", null, false));

            var text = out.toString();
            assertTrue(text.contains("#" + number + " https://example.org/o/r/pull/" + number + " is open"), text);
            assertTrue(text.contains("Tried 3 times, last "), text);
            assertFalse(resolved(server), server.requests().toString());
        }
    }

    private OperatorContext german() {
        var env = new HashMap<>(fixture.environment());
        env.put("LANG", "de_DE.UTF-8");
        return new OperatorContext(env, fixture.base().getParent(), true, ClientEnvironment.capture(env),
                            virtual::terminal, null);
    }

    /** Serves the settings, answers every change with the given results in order, and records them. */
    private FakeRuntimeServer serveSettings(String settings, PlanSettingsUpdateResult... results) throws IOException {
        var writes = new AtomicInteger();
        return fixture.start((request, response) -> {
            if (!request.target().endsWith("/settings")) {
                response.send(404, null, "");
            } else if ("GET".equals(request.method())) {
                response.json(200, settings);
            } else {
                var result = results[Math.min(writes.getAndIncrement(), results.length - 1)];
                response.json(result.applied() ? 200 : 409, result.toJson());
            }
        });
    }

    private static List<String> settingsWrites(FakeRuntimeServer server) {
        return server.requests().stream()
                            .filter(request -> request.target().endsWith("/settings") && !"GET".equals(request.method()))
                            .map(FakeRuntimeServer.Request::bodyText).toList();
    }

    @Nested
    @DisplayName("the plan settings dialogue")
    @EnableGeneratorController
    class PlanSettingsBehaviour {

        @Test
        @DisplayName("a value outside the runtime's choices is not offered and never sent")
        void valueOutsideChoices() throws Exception {
            var server = serveSettings(SETTINGS, PlanSettingsUpdateResult.applied(8));
            var input = Generators.letterStrings(8, 12).next();
            script("1\n" + input + "\nq\n");

            assertEquals(0, PlanSettingsDialogue.run(context, new PrintWriter(out), "p", "plan-defaults", false));

            assertTrue(out.toString().contains(input + " is not offered here: 0-2, q"), out.toString());
            assertEquals(List.of(), settingsWrites(server));
        }

        @Test
        @DisplayName("a value is typed where the runtime names no choices")
        void typedValueWithoutChoices() throws Exception {
            var budget = String.valueOf(Generators.integers(1, 500).next());
            var server = serveSettings("""
                                {"plan_id":"p","revision":7,"values":[
                                  {"aspect":"budget","value":"10","origin":"plan"}]}
                                """, PlanSettingsUpdateResult.applied(8));
            script("1\n" + budget + "\n");

            assertEquals(0, PlanSettingsDialogue.run(context, new PrintWriter(out), "p", "plan-defaults", false));

            assertEquals(1, settingsWrites(server).size(), server.requests().toString());
            assertTrue(settingsWrites(server).getFirst().contains("\"" + budget + "\""), settingsWrites(server).toString());
            assertTrue(out.toString().contains("version becomes 8"), out.toString());
        }

        @Test
        @DisplayName("the German keep key keeps the current value and applies nothing again")
        void germanKeepKey() throws Exception {
            var server = serveSettings(SETTINGS, PlanSettingsUpdateResult.conflict(8, "landing.main"),
                                PlanSettingsUpdateResult.applied(9));
            script("1\n2\nh\n");

            assertEquals(0, PlanSettingsDialogue.run(german(), new PrintWriter(out), "p", "plan-defaults", false));

            assertEquals(1, settingsWrites(server).size(), out.toString());
        }

        @ParameterizedTest(name = "the conflict answered with \"{0}\" applies nothing again")
        @CsvSource({"q", "k", "zz"})
        @DisplayName("a conflict applies the value again only on Enter")
        void conflictAppliedOnlyOnEnter(String answer) throws Exception {
            var server = serveSettings(SETTINGS, PlanSettingsUpdateResult.conflict(8, "landing.main"),
                                PlanSettingsUpdateResult.applied(9));
            script("1\n2\n" + answer + "\nq\n");

            assertEquals(0, PlanSettingsDialogue.run(context, new PrintWriter(out), "p", "plan-defaults", false));

            assertEquals(1, settingsWrites(server).size(), out.toString());
            assertEquals("zz".equals(answer), out.toString().contains("zz is not offered here: k, q"), out.toString());
        }

        @Test
        @DisplayName("an aspect that is not offered is answered and asked again")
        void aspectNotOffered() throws Exception {
            var server = serveSettings(SETTINGS, PlanSettingsUpdateResult.applied(8));
            script("landing\n9\nq\n");

            assertEquals(0, PlanSettingsDialogue.run(context, new PrintWriter(out), "p", "plan-defaults", false));

            var text = out.toString();
            assertTrue(text.contains("landing is not offered here: 1, q"), text);
            assertTrue(text.contains("9 is not offered here: 1, q"), text);
            assertEquals(List.of(), settingsWrites(server));
        }

        @Test
        @DisplayName("a group that is not offered is answered and asked again")
        void groupNotOffered() throws Exception {
            var server = serveSettings(SETTINGS, PlanSettingsUpdateResult.applied(8));
            script("5\nv\n");

            assertEquals(0, PlanSettingsDialogue.run(german(), new PrintWriter(out), "p", null, false));

            assertTrue(out.toString().contains("5 wird hier nicht angeboten: 1-4, v"), out.toString());
            assertEquals(List.of(), settingsWrites(server));
        }

        @Test
        @DisplayName("without input the dialogue stops where it would ask")
        void noInputStops() throws Exception {
            var server = serveSettings(SETTINGS, PlanSettingsUpdateResult.applied(8));

            assertEquals(StartDialogue.EXIT_MISSING_INPUT,
                                PlanSettingsDialogue.run(context, new PrintWriter(out), "p", "plan-defaults", true));

            assertEquals(List.of(), settingsWrites(server));
        }

        @Test
        @DisplayName("each group holds its own aspects, with where each value comes from")
        void groupsAndOrigins() throws Exception {
            serveSettings("""
                                {"plan_id":"p","revision":7,"values":[
                                  {"aspect":"landing","value":"landing.main","origin":"plan","choices":["landing.main"]},
                                  {"aspect":"role.planning","origin":"machine","choices":["a"]},
                                  {"aspect":"integration.github","value":"on","origin":"team","choices":["on"]},
                                  {"aspect":"provider.speech","value":"off","origin":"project","choices":["off"]}]}
                                """, PlanSettingsUpdateResult.applied(8));
            script("2\nq\n");

            assertEquals(0, PlanSettingsDialogue.run(context, new PrintWriter(out), "p", null, false));

            var text = out.toString();
            assertTrue(text.contains("role.planning"), text);
            assertTrue(text.contains("from the machine"), text);
            assertFalse(text.contains("integration.github"), text);
            assertFalse(text.contains("provider.speech"), text);
        }
    }

    /** Serves the review, answers each disposition with the decided review, and records every request. */
    private FakeRuntimeServer serveReview(String review) throws IOException {
        return fixture.start((request, response) -> {
            var path = request.target();
            if (path.contains("/disposition")) {
                response.json(200, REVIEW_KEPT);
            } else if (path.endsWith("/review/commit")) {
                response.json(200, REVIEW_COMMITTED);
            } else if (path.contains("/review")) {
                response.json(200, review);
            } else if (path.endsWith("/chat")) {
                response.json(200, CHAT);
            } else {
                response.send(404, null, "");
            }
        });
    }

    private static List<String> sent(FakeRuntimeServer server, String suffix) {
        return server.requests().stream().filter(request -> request.target().contains(suffix))
                            .map(FakeRuntimeServer.Request::bodyText).toList();
    }

    @Nested
    @DisplayName("the review dialogue")
    @EnableGeneratorController
    class ReviewBehaviour {

        @Test
        @DisplayName("a key the item does not offer is answered and asked again")
        void decisionNotOffered() throws Exception {
            var server = serveReview(REVIEW);
            script("r\nq\n");

            assertEquals(0, ReviewDialogue.run(context, new PrintWriter(out), "e", "findings", false));

            assertTrue(out.toString().contains("r is not offered here: k, d, h, f, q"), out.toString());
            assertEquals(List.of(), sent(server, "/disposition"));
        }

        @ParameterizedTest(name = "{0}: answered \"{1}\" -> {2}")
        @CsvSource({"findings,1,kept", "findings,2,watch", "findings,'',kept", "watches,1,watch",
            "watches,2,kept"})
        @DisplayName("keeping an item keeps it in its register or moves it to the other")
        void keepOrMove(String register, String answer, String disposition) throws Exception {
            var server = serveReview(REVIEW.replace("\"register\":\"findings\"", "\"register\":\"" + register + "\""));
            script("k\n" + answer + "\n");

            assertEquals(0, ReviewDialogue.run(context, new PrintWriter(out), "e", register, false));

            var bodies = sent(server, "/disposition");
            assertEquals(1, bodies.size(), out.toString());
            assertTrue(bodies.getFirst().contains("\"" + disposition + "\""), bodies.getFirst());
        }

        @ParameterizedTest(name = "q at the {0} question sends nothing")
        @CsvSource({"k", "d"})
        @DisplayName("leaving at the keep or the drop question sends nothing")
        void leaveSendsNothing(String key) throws Exception {
            var server = serveReview(REVIEW);
            script(key + "\nq\n");

            assertEquals(0, ReviewDialogue.run(context, new PrintWriter(out), "e", "findings", false));

            assertEquals(List.of(), sent(server, "/disposition"));
        }

        @Test
        @DisplayName("a register answer that is not offered is answered and asked again")
        void registerNotOffered() throws Exception {
            var server = serveReview(REVIEW);
            script("k\n3\n1\n");

            assertEquals(0, ReviewDialogue.run(context, new PrintWriter(out), "e", "findings", false));

            assertTrue(out.toString().contains("3 is not offered here: 1-2, q"), out.toString());
            assertEquals(1, sent(server, "/disposition").size(), out.toString());
        }

        @Test
        @DisplayName("an item with a drafted plan shows the plan")
        void draftedPlanShown() throws Exception {
            var objective = Generators.letterStrings(8, 20).next();
            var drafted = REVIEW.replace("\"items\":[", "\"drafted_plan\":{\"plan_id\":\"relay-fix\","
                                + "\"objective\":\"" + objective + "\",\"deliverables\":[\"one\",\"two\"],"
                                + "\"surface\":[\"a.java\",\"b.java\"]},\"items\":[");
            serveReview(drafted);
            script("q\n");

            assertEquals(0, ReviewDialogue.run(context, new PrintWriter(out), "e", "findings", false));

            var text = out.toString();
            assertTrue(text.contains("Plan relay-fix"), text);
            assertTrue(text.contains(objective), text);
            assertTrue(text.contains("one; two"), text);
            assertTrue(text.contains("a.java, b.java"), text);
            assertTrue(text.contains("p) Promote to a plan"), text);
        }

        @Test
        @DisplayName("the chat about the review reads the review again afterwards")
        void chatThenReadAgain() throws Exception {
            var server = serveReview(REVIEW);
            script("h\n\nq\n");

            assertEquals(0, ReviewDialogue.run(context, new PrintWriter(out), "e", "findings", false));

            assertTrue(server.requests().stream().anyMatch(request -> request.target().endsWith("/epics/e/chat")),
                                server.requests().toString());
            assertEquals(2, server.requests().stream().filter(request -> request.target().contains("/review")).count(),
                                server.requests().toString());
        }

        @Test
        @DisplayName("a declined finish writes nothing to the ledger")
        void finishDeclined() throws Exception {
            var server = serveReview(REVIEW_KEPT);
            script("n\n");

            assertEquals(0, ReviewDialogue.run(context, new PrintWriter(out), "e", "findings", false));

            assertEquals(List.of(), sent(server, "/review/commit"));
        }

        @ParameterizedTest(name = "without input over {0}")
        @CsvSource({"an open item", "a decided review"})
        @DisplayName("without input the review stops where it would ask")
        void noInputStops(String state) throws Exception {
            var server = serveReview("an open item".equals(state) ? REVIEW : REVIEW_KEPT);

            assertEquals(StartDialogue.EXIT_MISSING_INPUT,
                                ReviewDialogue.run(context, new PrintWriter(out), "e", "findings", true));

            assertEquals(List.of(), sent(server, "/disposition"));
            assertEquals(List.of(), sent(server, "/review/commit"));
        }
    }

    private static final String CUT = """
                        {"epic_id":"e","status":"check","revision":3,
                         "workstreams":[{"id":"WS-1","name":"Reconnect","order":1,
                           "staged_plans":[{"plan_id":"relay-cursor-order","title":"relay-cursor-order",
                             "expected_surface":["pm-relay/src/cursor"],"dependencies":[],"status":"staged"}]},
                         {"id":"WS-2","name":"Authentication","order":2,
                           "staged_plans":[{"plan_id":"relay-auth-v2","title":"relay-auth-v2",
                             "objective":"a token exchange","expected_surface":["pm-api/src/cursor"],
                             "dependencies":["relay-cursor-order"],"status":"staged"}]}],
                         "findings":[{"severity":"error","rule":"surface_overlap",
                           "affected_plans":["relay-cursor-order","relay-auth-v2"],
                           "message":"surface overlaps relay-auth-v2 (pm-api/src/cursor)"}]}
                        """;

    private static final String CUT_PASSING = """
                        {"epic_id":"e","status":"review","revision":4,
                         "workstreams":[{"id":"WS-1","name":"Reconnect","order":1,
                           "staged_plans":[{"plan_id":"relay-cursor-order","title":"relay-cursor-order",
                             "expected_surface":["pm-relay/src/cursor"],"dependencies":[],"status":"staged"}]}],
                         "findings":[]}
                        """;

    /** Serves the epic's decompose action and its decomposition; the check answers with the given cut. */
    private FakeRuntimeServer serveDecomposition(String checked) throws IOException {
        var base = "/projects/pr/epics/e/decomposition";
        return fixture.start((request, response) -> {
            var path = request.target();
            if (path.endsWith("/epics/e/actions")) {
                response.json(200, EPIC_ACTIONS.replace("{\"action\":\"next\",\"available\":true},",
                                    "{\"action\":\"decompose\",\"available\":true},"));
            } else if (path.endsWith(base + "/check")) {
                response.json(200, checked);
            } else if (path.endsWith(base + "/commit")) {
                response.json(200, "{\"status\":\"committed\",\"ledger_revision\":4,\"launchable_plan_count\":1}");
            } else if (path.endsWith(base + "/instruct")) {
                response.json(200, CUT_PASSING);
            } else if (path.endsWith(base)) {
                response.json(200, CUT);
            } else {
                response.send(404, null, "");
            }
        });
    }

    private int decompose(OperatorContext on) throws IOException {
        return WorkActionDialogue.run(on, new PrintWriter(out), "epic", "e", "decompose", false,
                            EpicResource.fromJson(EPIC), null);
    }

    @Nested
    @DisplayName("the decomposition dialogue")
    @EnableGeneratorController
    class DecompositionBehaviour {

        @Test
        @DisplayName("an input the cut does not offer is answered with one line, and q leaves keeping the draft")
        void notOffered() throws Exception {
            var server = serveDecomposition(CUT);
            var input = Generators.letterStrings(8, 12).next();
            script(input + "\nq\n");

            assertEquals(0, decompose(context));

            var text = out.toString();
            assertTrue(text.contains(input + " is not offered here: 1-2, +, r, q, ?"), text);
            assertTrue(text.contains("Left [3/3] Decomposition of e. The draft is kept."), text);
            assertEquals(List.of(), sent(server, "/check"));
        }

        @Test
        @DisplayName("? says that nothing more is explained")
        void questionMark() throws Exception {
            serveDecomposition(CUT);
            script("?\nq\n");

            assertEquals(0, decompose(context));

            assertTrue(out.toString().contains("Nothing more is explained here; choose one of the keys."),
                                out.toString());
        }

        @Test
        @DisplayName("German v leaves and keeps the draft")
        void germanLeave() throws Exception {
            var server = serveDecomposition(CUT);
            script("v\n");

            assertEquals(0, decompose(german()));

            assertTrue(out.toString().contains("Dekomposition von e verlassen"), out.toString());
            assertEquals(List.of(), sent(server, "/check"));
        }

        @Test
        @DisplayName("a plan opens with its detail and is edited by the operator's words")
        void openAndEdit() throws Exception {
            var server = serveDecomposition(CUT);
            var words = Generators.letterStrings(8, 30).next();
            script("2\ne\n" + words + "\n\nq\n");

            assertEquals(0, decompose(context));

            var text = out.toString();
            assertTrue(text.contains("a token exchange"), text);
            assertTrue(text.contains("WS-2 Authentication"), text);
            var instructs = sent(server, "/instruct");
            assertEquals(1, instructs.size(), text);
            assertTrue(instructs.getFirst().contains("\"edit\"") && instructs.getFirst().contains("relay-auth-v2")
                                && instructs.getFirst().contains(words), instructs.getFirst());
        }

        @Test
        @DisplayName("Enter agrees: a cut whose checks pass is committed")
        void agreeCommits() throws Exception {
            var server = serveDecomposition(CUT_PASSING);
            script("\n");

            assertEquals(0, decompose(context));

            assertEquals(1, sent(server, "/commit").size(), out.toString());
        }

        @Test
        @DisplayName("Enter over a cut whose checks fail commits nothing and shows the cut again")
        void agreeRefusedByChecks() throws Exception {
            var server = serveDecomposition(CUT);
            script("\nq\n");

            assertEquals(0, decompose(context));

            assertEquals(1, sent(server, "/check").size(), out.toString());
            assertEquals(List.of(), sent(server, "/commit"));
        }
    }

    /** Serves the given actions for every subject and answers every act with them. */
    private FakeRuntimeServer serveActions(String actions) throws IOException {
        return fixture.start((request, response) -> {
            var path = request.target();
            if (path.endsWith("/actions") || path.contains("/actions/")) {
                response.json(200, actions);
            } else if (path.endsWith("/epics/e")) {
                response.json(200, EPIC);
            } else if (path.endsWith("/archive-check")) {
                response.json(200, ARCHIVE_CHECK.replace("\"lines\":[",
                                    "\"lines\":[{\"label\":\"the ledger is behind\",\"passed\":true,\"note\":\"3 rows\"},"));
            } else {
                response.send(404, null, "");
            }
        });
    }

    private static List<String> performed(FakeRuntimeServer server) {
        return server.requests().stream().map(FakeRuntimeServer.Request::target)
                            .filter(target -> target.contains("/actions/")).toList();
    }

    @Nested
    @DisplayName("the work action dialogue")
    @EnableGeneratorController
    class WorkActionBehaviour {

        @Test
        @DisplayName("a negative common-files count is refused and nothing is sent")
        void commonFilesNegative() throws Exception {
            var server = serveActions(EPIC_ACTIONS);
            script("-" + Generators.integers(1, 50).next() + "\n");

            assertEquals(1, WorkActionDialogue.run(context, new PrintWriter(out), "epic", "e", "common_files",
                                false, EpicResource.fromJson(EPIC), null));

            assertTrue(out.toString().contains("Give the number"), out.toString());
            assertEquals(List.of(), performed(server));
        }

        @Test
        @DisplayName("an action the state does not offer is refused with the actions it allows")
        void actionNotInState() throws Exception {
            var server = serveActions(PAUSED_ACTIONS);

            assertEquals(1, dialogue("plan", "p", "pause"));

            assertTrue(out.toString().contains("start, abandon"), out.toString());
            assertEquals(List.of(), performed(server));
        }

        @Test
        @DisplayName("an action that cannot run now is refused with the runtime's reason")
        void actionDisabled() throws Exception {
            var reason = Generators.letterStrings(8, 20).next();
            var server = serveActions(PLAN_ACTIONS.replace("{\"action\":\"pause\",\"available\":true}",
                                "{\"action\":\"pause\",\"available\":false,\"reason\":\"" + reason + "\"}"));

            assertEquals(1, dialogue("plan", "p", "pause"));

            assertTrue(out.toString().contains("pause is not available: " + reason), out.toString());
            assertEquals(List.of(), performed(server));
        }

        @Test
        @DisplayName("an offered action the dialogue has no form for is refused")
        void offeredWithoutDialogue() throws Exception {
            var server = serveActions(PLAN_ACTIONS.replace("{\"action\":\"pause\",\"available\":true}",
                                "{\"action\":\"decide\",\"available\":true}"));

            assertEquals(1, dialogue("plan", "p", "decide"));

            assertEquals(List.of(), performed(server));
        }

        @Test
        @DisplayName("abandoning a paused plan without an epic asks the reason and confirms")
        void abandonWithoutEpic() throws Exception {
            var server = serveActions(PAUSED_ACTIONS);
            script("given up\n1\ny\n");

            assertEquals(0, dialogue("plan", "p", "abandon"));

            var text = out.toString();
            assertTrue(text.contains("abandoned"), text);
            assertFalse(text.contains(" told"), text);
            assertEquals(List.of("/api/v1/plans/p/actions/abandon"), performed(server));
        }

        @Test
        @DisplayName("ending without a reason is refused and sends nothing")
        void endWithoutReason() throws Exception {
            var server = serveActions(PLAN_ACTIONS);
            script("\n");

            assertEquals(0, dialogue("plan", "p", "cancel"));

            assertTrue(out.toString().contains("The reason is required."), out.toString());
            assertEquals(List.of(), performed(server));
        }

        @Test
        @DisplayName("a plan held by an integrity pause is aborted, not cancelled")
        void integrityPausedAborts() throws Exception {
            serveActions(PLAN_ACTIONS.replace("\"state\":\"running\"",
                                "\"state\":\"held\",\"hold_kind\":\"integrity_pause\""));
            script("the file changed\n1\ny\n");

            assertEquals(0, dialogue("plan", "p", "cancel"));

            assertTrue(out.toString().contains("aborted"), out.toString());
        }

        @Test
        @DisplayName("a hold act of an epic is sent as the epic's act")
        void epicHoldAct() throws Exception {
            var server = serveActions(EPIC_ACTIONS);

            assertEquals(0, WorkActionDialogue.run(context, new PrintWriter(out), "epic", "e", "retry", false,
                                null, null));

            assertEquals(List.of("/api/v1/epics/e/actions/retry"), performed(server));
        }

        @Test
        @DisplayName("an archive check line that names no catalogue text is shown as the runtime wrote it")
        void archiveUncataloguedLine() throws Exception {
            serveActions(EPIC_ACTIONS);
            script("n\n");

            assertEquals(0, dialogue("epic", "e", "archive"));

            assertTrue(out.toString().contains("the ledger is behind"), out.toString());
            assertTrue(out.toString().contains("3 rows"), out.toString());
        }

        @Test
        @DisplayName("automatic mode without the epic resource reads the epic first")
        void autoReadsEpic() throws Exception {
            var server = serveActions(EPIC_ACTIONS);

            assertEquals(0, dialogue("epic", "e", "auto"));

            assertTrue(server.requests().stream().anyMatch(request -> request.target().endsWith("/epics/e")),
                                server.requests().toString());
            assertTrue(out.toString().contains("starts its plans by itself"), out.toString());
        }

        @Test
        @DisplayName("without input the action stops and says so")
        void noInputStops() throws Exception {
            var server = serveActions(PLAN_ACTIONS);

            assertEquals(StartDialogue.EXIT_MISSING_INPUT,
                                WorkActionDialogue.run(context, new PrintWriter(out), "plan", "p", "pause", true, null,
                                                    null));

            assertEquals(List.of(), performed(server));
        }
    }

    @Test
    @DisplayName("every client call is fail-closed on a refused response")
    void clientRefusals() throws Exception {
        serve((request, response) -> response.json(500, "{\"error\":\"server_error\"}"));
        var client = new WorkActionsClient(context);
        assertThrows(OperatorRefusedException.class, () -> client.actions("plan", "p"));
        assertThrows(OperatorRefusedException.class, () -> client.holds("p"));
        assertThrows(OperatorRefusedException.class, () -> client.openDecision("p"));
        assertThrows(OperatorRefusedException.class, () -> client.perform("plan", "p", "start", Map.of()));
        assertThrows(OperatorRefusedException.class, () -> client.resolveHold("p", "h", "retry", null));
        assertThrows(OperatorRefusedException.class, () -> client.settings("p"));
        assertThrows(OperatorRefusedException.class, () -> client.updateSettings("p", 1, "a", "b"));
        assertThrows(OperatorRefusedException.class, () -> client.review("e", "findings"));
        assertThrows(OperatorRefusedException.class, () -> client.disposition("e", "F-1", "kept", null));
        assertThrows(OperatorRefusedException.class, () -> client.commitReview("e"));
        assertThrows(OperatorRefusedException.class, () -> client.chat("plan", "p"));
        assertThrows(OperatorRefusedException.class, () -> client.chatTurn("plan", "p", "x"));
        assertThrows(OperatorRefusedException.class, () -> client.chatProposal("plan", "p", "T-3", true));
        assertThrows(OperatorRefusedException.class, () -> client.epic("e"));
        assertThrows(OperatorRefusedException.class, () -> client.archiveCheck("e"));
        assertThrows(OperatorRefusedException.class, () -> client.performLanding("pr", "unit", "retry"));
    }
}
