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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.jline.terminal.impl.DumbTerminal;

import de.planmarshall.api.start.EntryKind;
import de.planmarshall.api.start.EntryResource;
import de.planmarshall.api.start.StartState;
import de.planmarshall.api.start.StartStateResource;
import de.planmarshall.api.testsupport.RuntimeFixture;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The chat about ended work as the overview opens it: a follow-up the operator keeps starts a plan
 * in the project the start resolves, and a chat left at its proposal starts nothing.
 */
@EnableGeneratorController
@DisplayName("OperatorActions")
class OperatorActionsTest {

    private RuntimeFixture fixture;

    @BeforeEach
    void setUp() throws Exception {
        fixture = RuntimeFixture.create();
        PmOperatorTest.writeLiveRecord(fixture);
    }

    @AfterEach
    void tearDown() throws Exception {
        fixture.close();
    }

    private void serveEndedChat(String project, String summary, AtomicReference<String> opened) throws Exception {
        var entry = new EntryResource("ent-1", EntryKind.PLAN, project, StartState.SOURCES,
                            null, null, List.of(), List.of(), true, true, null, null, List.of(), null);
        var startState = StartStateResource.of(entry, null, null, null, null, List.of()).toJson();
        fixture.start((request, response) -> {
            if ("/api/v1/plans/p-1/chat".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, "{\"chat_id\":\"c-1\",\"subject_id\":\"p-1\",\"subject_kind\":\"plan\","
                                    + "\"role\":\"replan\",\"scope\":\"archive\",\"state\":\"open\",\"turns\":[],"
                                    + "\"proposal\":{\"summary\":\"" + summary + "\"}}");
            } else if ("/api/v1/projects".equals(request.target())) {
                response.json(200, "{\"projects\":[{\"project_id\":\"" + project + "\"}]}");
            } else if (request.target().endsWith("/entries") && "POST".equals(request.method())) {
                opened.set(request.target());
                response.json(200, startState);
            } else {
                response.send(404, null, "");
            }
        });
    }

    private OperatorContext context(String typed) {
        var input = new ByteArrayInputStream(typed.getBytes(StandardCharsets.UTF_8));
        var environment = new HashMap<>(fixture.environment());
        environment.put("LANG", "en_US.UTF-8");
        environment.remove("PM_PROJECT");
        return new OperatorContext(environment, fixture.base().getParent(), true,
                            ClientEnvironment.capture(environment),
                            () -> new DumbTerminal(input, new ByteArrayOutputStream()), null);
    }

    @Test
    @DisplayName("a kept follow-up without a project starts a plan in the one project the runtime names")
    void keptFollowUpStartsInTheRuntimesProject() throws Exception {
        var project = "p" + Generators.letterStrings(6, 12).next().toLowerCase();
        var summary = Generators.letterStrings(8, 20).next();
        var opened = new AtomicReference<String>();
        serveEndedChat(project, summary, opened);
        var out = new StringWriter();

        var code = OperatorActions.chatAboutEnded(context("\n"), new PrintWriter(out, true), "plan", "p-1", null, "en");

        assertAll(
                            () -> assertEquals(0, code, out.toString()),
                            () -> assertTrue(out.toString().contains(
                                                "A new plan is being started with this description: " + summary), out.toString()),
                            () -> assertEquals("/api/v1/projects/" + project + "/entries", opened.get())
        );
    }

    @Test
    @DisplayName("a chat left at its proposal starts no follow-up")
    void leftChatStartsNothing() throws Exception {
        var opened = new AtomicReference<String>();
        serveEndedChat("pm-mcp-clients", Generators.letterStrings(8, 20).next(), opened);
        var out = new StringWriter();

        var code = OperatorActions.chatAboutEnded(context("q\n"), new PrintWriter(out, true), "plan", "p-1",
                            "pm-mcp-clients", "en");

        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertFalse(out.toString().contains("A new plan is being started"), out.toString()),
                            () -> assertNull(opened.get(), "no entry is opened")
        );
    }
}
