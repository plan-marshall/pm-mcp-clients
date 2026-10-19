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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import de.planmarshall.api.decomposition.DecompositionInstructRequest;
import de.planmarshall.api.decomposition.InstructAction;
import de.planmarshall.api.start.EntryKind;
import de.planmarshall.api.start.EntryOpenRequest;
import de.planmarshall.api.testsupport.FakeRuntimeServer;
import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The start contract's error surface: every call reports a refusal of the runtime as the dialogue
 * expects it, and the endpoints that answer a benign absence return the documented value.
 */
@DisplayName("start contract client")
class StartApiTest {

    private static final String PROJECT = "/api/v1/projects/pm-mcp-clients";
    private static final String DECOMPOSITION = PROJECT + "/epics/relay-hardening/decomposition";

    private RuntimeFixture fixture;
    private StartApi api;

    @BeforeEach
    void setUp() throws IOException {
        fixture = RuntimeFixture.create();
        PmOperatorTest.writeLiveRecord(fixture);
        api = new StartApi(new OperatorContext(fixture.environment(), fixture.base().getParent(), true));
    }

    @AfterEach
    void tearDown() throws IOException {
        fixture.close();
    }

    private void serve(FakeRuntimeServer.Handler handler) throws IOException {
        fixture.start(handler);
    }

    private static FakeRuntimeServer.Handler failure(int status, String body) {
        return (request, response) -> response.send(status, "application/json", body);
    }

    @Test
    @DisplayName("open, fetch, and act report a refusal")
    void openFetchActRefused() throws Exception {
        serve(failure(500, "{\"message\":\"no\"}"));
        assertAll(
                            () -> assertThrows(OperatorRefusedException.class,
                                                () -> api.open("pm-mcp-clients", EntryOpenRequest.of(EntryKind.PLAN))),
                            () -> assertThrows(OperatorRefusedException.class, () -> api.fetch("ent-1")),
                            () -> assertThrows(StartApi.StartActException.class,
                                                () -> api.act("ent-1", "add_issue", Map.of())));
    }

    @Test
    @DisplayName("a refused act carries the runtime's message")
    void actRefusedCarriesMessage() throws Exception {
        serve(failure(409, "{\"message\":\"the source does not combine\"}"));
        var refused = assertThrows(StartApi.StartActException.class,
                            () -> api.act("ent-1", "add_issue", Map.of()));
        assertTrue(refused.getMessage().contains("the source does not combine"), refused.getMessage());
    }

    @Test
    @DisplayName("chat turn, proposal, and end report a refusal")
    void chatRefused() throws Exception {
        serve(failure(409, "{\"message\":\"no chat\"}"));
        assertAll(
                            () -> assertThrows(StartApi.StartActException.class,
                                                () -> api.chatTurn("ent-1", "hello")),
                            () -> assertThrows(StartApi.StartActException.class,
                                                () -> api.chatProposal("ent-1", true)),
                            () -> assertThrows(OperatorRefusedException.class, () -> api.endChat("ent-1")));
    }

    @Test
    @DisplayName("projects and recipes treat a refusal as empty and failed")
    void projectsAndRecipes() throws Exception {
        serve(failure(500, "{}"));
        assertAll(
                            () -> assertTrue(api.projects().isEmpty()),
                            () -> assertThrows(OperatorRefusedException.class, () -> api.recipes()));
    }

    @Test
    @DisplayName("the decomposition is absent, and a failed check or commit is reported")
    void decompositionPaths() throws Exception {
        serve((request, response) -> {
            if (DECOMPOSITION.equals(request.target()) && "GET".equals(request.method())) {
                response.send(500, "application/json", "{}");
            } else if ((DECOMPOSITION + "/check").equals(request.target())) {
                response.send(404, "application/json", "{}");
            } else if ((DECOMPOSITION + "/instruct").equals(request.target())) {
                response.send(409, "application/json", "{}");
            } else if ((DECOMPOSITION + "/commit").equals(request.target())) {
                response.send(409, "application/json", "{}");
            } else {
                response.send(404, "application/json", "{}");
            }
        });
        assertAll(
                            () -> assertThrows(OperatorRefusedException.class,
                                                () -> api.decomposition("pm-mcp-clients", "relay-hardening")),
                            () -> assertNull(api.checkDecomposition("pm-mcp-clients", "relay-hardening")),
                            () -> assertThrows(OperatorRefusedException.class,
                                                () -> api.instruct("pm-mcp-clients", "relay-hardening",
                                                                    new DecompositionInstructRequest(null, "x",
                                                                                        InstructAction.EDIT))),
                            () -> assertNull(api.commit("pm-mcp-clients", "relay-hardening")));
    }

    @Test
    @DisplayName("an absent decomposition is null")
    void decompositionAbsent() throws Exception {
        serve(failure(404, "{}"));
        assertNull(api.decomposition("pm-mcp-clients", "relay-hardening"));
    }

    @Test
    @DisplayName("the projects endpoint reads an object and a bare array")
    void projectsReadsBothShapes() throws Exception {
        serve((request, response) -> response.json(200,
                            "{\"projects\":[{\"project_id\":\"pm-mcp-clients\"},{\"project_id\":\"pm-api\"}]}"));
        assertTrue(api.projects().containsAll(List.of("pm-mcp-clients", "pm-api")));
    }
}
