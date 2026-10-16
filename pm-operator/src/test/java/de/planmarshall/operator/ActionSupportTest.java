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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.planmarshall.api.testsupport.FakeRuntimeServer;
import de.planmarshall.api.testsupport.RuntimeFixture;

import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

@DisplayName("ActionSupport")
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
    @DisplayName("endWork sends default reason and close_pr=true on success")
    void endWorkDefaultReasonSuccess() throws Exception {
        runtime((request, response) -> {
            if ("/api/v1/plans/p-1/cancel".equals(request.target()) && "POST".equals(request.method())) {
                response.send(200, null, "");
            } else {
                response.send(404, null, "");
            }
        });

        var opts = terminationOptions("p-1", null, false, false);
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
            if ("/api/v1/plans/p-1/chat".equals(request.target()) && "POST".equals(request.method())) {
                assertTrue(request.bodyText().contains("\"message\":\"hello plan\""));
                response.json(200, "{\"status\":\"delivered\"}");
            } else {
                response.send(404, null, "");
            }
        });

        var code = ActionSupport.chat(context, spec, "plan", "p-1", "hello plan");
        assertEquals(0, code);
        assertTrue(out.toString().contains("delivered"));
    }

    @Test
    @DisplayName("chat fetches history when message is null or blank")
    void chatGetHistory() throws Exception {
        runtime((request, response) -> {
            if ("/api/v1/epics/e-1/chat".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, "[{\"role\":\"user\",\"content\":\"hi\"}]");
            } else {
                response.send(404, null, "");
            }
        });

        var code = ActionSupport.chat(context, spec, "epic", "e-1", "   ");
        assertEquals(0, code);
        assertTrue(out.toString().contains("hi"));
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
