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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import org.jline.terminal.Terminal;

import de.planmarshall.api.start.EntryKind;
import de.planmarshall.api.start.EntryResource;
import de.planmarshall.api.start.StartState;
import de.planmarshall.api.start.StartStateResource;
import de.planmarshall.api.testsupport.FakeRuntimeServer;
import de.planmarshall.api.testsupport.RuntimeFixture;
import de.planmarshall.operator.tui.VirtualScreenTerminal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

@DisplayName("ActionSupport")
@EnableGeneratorController
@EnabledOnOs({OS.MAC, OS.LINUX})
class ActionSupportTest {

    private RuntimeFixture fixture;
    private StringWriter out;
    private StringWriter err;
    private CommandSpec spec;
    private OperatorContext context;

    @BeforeEach
    void setUp() throws IOException {
        fixture = RuntimeFixture.create();
        out = new StringWriter();
        err = new StringWriter();
        var cmd = new CommandLine(CommandSpec.create());
        cmd.setOut(new PrintWriter(out));
        cmd.setErr(new PrintWriter(err));
        spec = cmd.getCommandSpec();
        context = new OperatorContext(fixture.environment(), fixture.base().getParent());
    }

    @AfterEach
    void tearDown() throws IOException {
        fixture.close();
    }

    private FakeRuntimeServer runtime(FakeRuntimeServer.Handler handler) throws IOException {
        var self = ProcessHandle.current();
        Files.createDirectories(fixture.paths().stateDir());
        Files.writeString(fixture.paths().runtimeRecord(), "{\"pid\":" + self.pid() + ",\"start_instant\":\""
                            + self.info().startInstant().orElseThrow() + "\"}");
        return fixture.start(handler);
    }

    private ActionSupport.TerminationOptions terminationOptions(String id, String reason, boolean keepPr, boolean noInput) {
        var opts = new ActionSupport.TerminationOptions();
        opts.id = id;
        opts.reason = reason;
        opts.keepPr = keepPr;
        opts.noInput = noInput;
        return opts;
    }

    @Test
    @DisplayName("an interrupt inside a dialogue the view opened leaves back with 0, every other code stands")
    void theLeaveBackMapping() {
        var other = Generators.integers(0, 129).next();

        assertAll(
                            () -> assertEquals(0, ActionSupport.leaveBack(StartPrompt.EXIT_INTERRUPTED),
                                                "the interrupt is the view's own leave-back"),
                            () -> assertEquals(other, ActionSupport.leaveBack(other),
                                                "a code that is not an interrupt is kept")
        );
    }

    @Test
    @DisplayName("a follow-up in the console is accepted on the runtime and the start is announced")
    void chatFollowUpAcceptsOnTheRuntime() throws Exception {
        var accepted = new AtomicBoolean();
        runtime((request, response) -> {
            var path = request.target();
            if ("/api/v1/plans/p-1/chat".equals(path) && "GET".equals(request.method())) {
                response.json(200, """
                                    {"id":"c-1","subject":{"kind":"plan","id":"p-1"},
                                     "role":{"name":"replan","model":"sonnet"},"scope":"archive",
                                     "state":"chat.read-back","context":{"knows":[],"as_of":"2026-10-16T10:00:00Z"},
                                     "turns":[],
                                     "proposal":{"turn_id":"T-3","reply_turn_id":"T-2","kind":"follow_up",
                                       "summary":"Follow-up plan","after":"Follow up","state":"open"}}
                                    """);
            } else if (path.endsWith("/proposal/accept")) {
                accepted.set(true);
                response.json(200, """
                                    {"id":"c-1","subject":{"kind":"plan","id":"p-1"},
                                     "role":{"name":"replan","model":"sonnet"},"scope":"archive",
                                     "state":"chat.turn","context":{"knows":[],"as_of":"2026-10-16T10:00:01Z"},
                                     "turns":[],
                                     "proposal":{"turn_id":"T-3","reply_turn_id":"T-2","kind":"follow_up",
                                       "summary":"Follow-up plan","after":"Follow up","state":"accepted"}}
                                    """);
            } else {
                response.send(404, null, "");
            }
        });

        var virtual = VirtualScreenTerminal.create(80, 24);
        var env = new HashMap<>(fixture.environment());
        env.put("LANG", "en_US.UTF-8");
        var activeContext = new OperatorContext(env, fixture.base().getParent(), true,
                            ClientEnvironment.capture(env), virtual::terminal, null);
        activeContext.setConsoleActive(true);

        virtual.type("\n");
        var code = ActionSupport.chat(activeContext, spec, "plan", "p-1", null);
        assertEquals(0, code, "the chat leaves back with 0");
        assertTrue(accepted.get(), "the follow-up proposal was not accepted on the runtime");
        assertTrue(out.toString().contains("A new plan is being started with this description"),
                            out.toString());
    }

    @Test
    @DisplayName("leaveBack preserves non-interrupt exit codes and maps interrupted to 0")
    void leaveBackPreservesNonInterruptExitCodes() {
        assertAll(
                            () -> assertEquals(0, ActionSupport.leaveBack(0)),
                            () -> assertEquals(1, ActionSupport.leaveBack(1)),
                            () -> assertEquals(2, ActionSupport.leaveBack(2)),
                            () -> assertEquals(PmOperator.EXIT_FAILURE, ActionSupport.leaveBack(PmOperator.EXIT_FAILURE)),
                            () -> assertEquals(0, ActionSupport.leaveBack(StartPrompt.EXIT_INTERRUPTED))
        );
    }

    @Test
    @DisplayName("chatAboutEnded accepts the follow-up on the runtime and announces the start")
    void chatAboutEndedAcceptsTheFollowUp() throws Exception {
        var accepted = new AtomicBoolean();
        runtime((request, response) -> {
            var path = request.target();
            if ("/api/v1/plans/p-1/chat".equals(path) && "GET".equals(request.method())) {
                response.json(200, """
                                    {"id":"c-1","subject":{"kind":"plan","id":"p-1"},
                                     "role":{"name":"replan","model":"sonnet"},"scope":"archive",
                                     "state":"chat.read-back","context":{"knows":[],"as_of":"2026-10-16T10:00:00Z"},
                                     "turns":[],
                                     "proposal":{"turn_id":"T-3","reply_turn_id":"T-2","kind":"follow_up",
                                       "summary":"Follow-up plan","after":"Follow up","state":"open"}}
                                    """);
            } else if (path.endsWith("/proposal/accept")) {
                accepted.set(true);
                response.json(200, """
                                    {"id":"c-1","subject":{"kind":"plan","id":"p-1"},
                                     "role":{"name":"replan","model":"sonnet"},"scope":"archive",
                                     "state":"chat.turn","context":{"knows":[],"as_of":"2026-10-16T10:00:01Z"},
                                     "turns":[],
                                     "proposal":{"turn_id":"T-3","reply_turn_id":"T-2","kind":"follow_up",
                                       "summary":"Follow-up plan","after":"Follow up","state":"accepted"}}
                                    """);
            } else {
                response.send(404, null, "");
            }
        });

        var virtual = VirtualScreenTerminal.create(80, 24);
        var env = new HashMap<>(fixture.environment());
        env.put("LANG", "en_US.UTF-8");
        var activeContext = new OperatorContext(env, fixture.base().getParent(), true,
                            ClientEnvironment.capture(env), virtual::terminal, null);

        virtual.type("\n");
        var code = OperatorActions.chatAboutEnded(activeContext, new PrintWriter(out, true), "plan", "p-1",
                            "pm-mcp-clients", "en");
        assertEquals(0, code, "the ended work chat leaves back with 0");
        assertTrue(accepted.get(), "the follow-up proposal was not accepted on the runtime");
    }

    @Test
    @DisplayName("endWork requires reason when noInput is true")
    void endWorkRequiresReason() throws Exception {
        var opts1 = terminationOptions("p-1", null, false, true);
        var code = ActionSupport.endWork(context, spec, "plan", opts1, ActionSupport.CANCEL);
        assertEquals(3, code);
        assertTrue(out.toString().contains("Cancellation reason required (--reason)"));

        out.getBuffer().setLength(0);
        var opts2 = terminationOptions("e-1", "", false, true);
        var abandonCode = ActionSupport.endWork(context, spec, "epic", opts2, "abandon");
        assertEquals(3, abandonCode);
        assertTrue(out.toString().contains("Abandon reason required (--reason)"));
    }

    @Test
    @DisplayName("endWork sends the operator's reason and close_pr=true on success")
    void endWorkDefaultReasonSuccess() throws Exception {
        runtime((request, response) -> {
            if ("/api/v1/plans/p-1/cancel".equals(request.target()) && "POST".equals(request.method())) {
                assertTrue(request.bodyText().contains("\"reason\":\"the approach is replaced\""));
                assertTrue(request.bodyText().contains("\"close_pr\":true"));
                response.send(200, null, "");
            } else {
                response.send(404, null, "");
            }
        });

        var opts = terminationOptions("p-1", "the approach is replaced", false, false);
        var code = ActionSupport.endWork(context, spec, "plan", opts, ActionSupport.CANCEL);
        assertEquals(0, code);
        assertTrue(out.toString().contains("✓ p-1 cancelled and archived"));
    }

    @Test
    @DisplayName("endWork sends custom reason and keepPr")
    void endWorkCustomReasonAndKeepPr() throws Exception {
        runtime((request, response) -> {
            if ("/api/v1/epics/e-1/abandon".equals(request.target()) && "POST".equals(request.method())) {
                assertTrue(request.bodyText().contains("\"close_pr\":false"));
                assertTrue(request.bodyText().contains("\"reason\":\"no longer relevant\""));
                response.send(202, null, "");
            } else {
                response.send(404, null, "");
            }
        });

        var opts = terminationOptions("e-1", "no longer relevant", true, false);
        var code = ActionSupport.endWork(context, spec, "epic", opts, "abandon");
        assertEquals(0, code);
        assertTrue(out.toString().contains("✓ e-1 abandoned and archived"));
    }

    @Test
    @DisplayName("endWork throws OperatorRefusedException on HTTP error")
    void endWorkRefused() throws Exception {
        runtime((request, response) -> response.json(500, "{\"code\":\"server_error\"}"));

        var opts = terminationOptions("p-1", "test", false, false);
        assertThrows(OperatorRefusedException.class, () ->
                            ActionSupport.endWork(context, spec, "plan", opts, ActionSupport.CANCEL));
    }

    @Test
    @DisplayName("chat posts message when provided")
    void chatPostMessage() throws Exception {
        runtime((request, response) -> {
            if ("/api/v1/plans/p-1/chat/turns".equals(request.target())) {
                assertTrue(request.bodyText().contains("\"text\":\"hello plan\""), request.bodyText());
                assertTrue(request.bodyText().contains("client_turn_id"), request.bodyText());
                response.json(202, "{\"turn_id\":\"T-1\"}");
            } else if ("/api/v1/plans/p-1/chat".equals(request.target())) {
                response.json(200, """
                                    {"id":"C-1","subject":{"kind":"plan","id":"p-1"},
                                     "role":{"name":"replan","model":"sonnet"},"scope":"plan",
                                     "state":"chat.turn","context":{"knows":[],"as_of":"2026-10-16T10:00:00Z"},
                                     "turns":[{"turn_id":"T-1","kind":"message","author":"operator",
                                               "status":"complete","format":"plain","untrusted":false,
                                               "text":"hello plan","steps":[],"refs":[],
                                               "at":"2026-10-16T10:00:00Z"},
                                              {"turn_id":"T-2","kind":"message","author":"replan",
                                               "status":"complete","format":"markdown","untrusted":false,
                                               "model":"sonnet","text":"delivered","steps":[],"refs":[],
                                               "at":"2026-10-16T10:00:01Z"}]}
                                    """);
            } else {
                response.send(404, null, "");
            }
        });

        var code = ActionSupport.chat(context, spec, "plan", "p-1", "hello plan");
        assertEquals(0, code);
        assertTrue(out.toString().contains("delivered"), out.toString());
    }

    @Test
    @DisplayName("chat fetches history when message is null or blank")
    void chatGetHistory() throws Exception {
        runtime((request, response) -> {
            if ("/api/v1/epics/e-1/chat".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, """
                                    {"id":"C-1","subject":{"kind":"epic","id":"e-1"},
                                     "role":{"name":"decompose","model":"sonnet"},"scope":"epic",
                                     "state":"chat.turn","context":{"knows":[],"as_of":"2026-10-16T10:00:00Z"},
                                     "turns":[
                                       {"turn_id":"T-1","kind":"message","author":"operator","status":"complete",
                                        "format":"plain","untrusted":false,"text":"hi","steps":[],"refs":[],
                                        "at":"2026-10-16T10:00:00Z"},
                                       {"turn_id":"T-2","kind":"message","author":"decompose","status":"complete",
                                        "format":"markdown","untrusted":false,"model":"sonnet",
                                        "text":"hello from the epic","steps":[],"refs":[],
                                        "at":"2026-10-16T10:00:01Z"}]}
                                    """);
            } else {
                response.send(404, null, "");
            }
        });

        var code = ActionSupport.chat(context, spec, "epic", "e-1", "   ");
        assertEquals(0, code);
        assertTrue(out.toString().contains("hello from the epic"), out.toString());
    }

    @Test
    @DisplayName("chat throws on failure")
    void chatThrowsOnFailure() throws Exception {
        runtime((request, response) -> response.json(404, "{\"code\":\"not_found\"}"));

        assertThrows(OperatorRefusedException.class, () ->
                            ActionSupport.chat(context, spec, "plan", "p-1", "hello"));
        assertThrows(OperatorRefusedException.class, () ->
                            ActionSupport.chat(context, spec, "plan", "p-1", null));
    }

    @Test
    @DisplayName("printWorkList formats empty and populated lists")
    void printWorkList() {
        var pw = new PrintWriter(out);
        ActionSupport.printWorkList(pw, "Plans", "phase", List.of());
        pw.flush();
        assertTrue(out.toString().contains("No plans running."));

        out.getBuffer().setLength(0);
        ActionSupport.printWorkList(pw, "Epics", "state", List.of(
                            Map.of("id", "epic-1", "state", "RUNNING", "title", "Refactor Auth"),
                            Map.of("id", "epic-2", "state", "PAUSED"),
                            "simple-string-item"
        ));
        pw.flush();
        var result = out.toString();
        assertTrue(result.contains("Epics"));
        assertTrue(result.contains("epic-1"));
        assertTrue(result.contains("Refactor Auth"));
        assertTrue(result.contains("epic-2"));
        assertTrue(result.contains("simple-string-item"));
    }
}
