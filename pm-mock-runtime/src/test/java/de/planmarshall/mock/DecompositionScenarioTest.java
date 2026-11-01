/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.control.DecompositionResource;
import de.planmarshall.api.decomposition.CheckSeverity;
import de.planmarshall.api.decomposition.DecompositionCheckFinding;
import de.planmarshall.api.decomposition.DecompositionCommitResult;
import de.planmarshall.api.decomposition.DecompositionInstructRequest;
import de.planmarshall.api.decomposition.DecompositionState;
import de.planmarshall.api.decomposition.EpicDecomposition;
import de.planmarshall.api.decomposition.InstructAction;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.HttpResponse;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.json.JsonTree;

@DisplayName("DecompositionScenario")
@EnabledOnOs({OS.MAC, OS.LINUX})
@EnableGeneratorController
class DecompositionScenarioTest {

    private static final String BASE = "/api/v1/projects/pm-mcp-clients/epics/";
    private static final String STATE = StandardScenarios.RUNNING_WORK + "#control.decomposition";
    private static final String AUTH_HEADER = "Authorization";

    @Test
    @DisplayName("loads the scenario and serves the drafted cut")
    void shouldLoadScenarioAndServeCut() throws Exception {
        try (var runtime = MockRuntime.start(STATE)) {
            assertEquals(StandardScenarios.RUNNING_WORK, runtime.currentScenarioName());
            assertEquals("control.decomposition", runtime.currentStateName());

            var res = get(runtime, BASE + "relay-hardening/decomposition");
            assertEquals(200, res.status());

            var cut = EpicDecomposition.fromJson(res.bodyText());
            assertEquals("relay-hardening", cut.epicId());
            assertEquals(DecompositionState.DRAFT, cut.status());
            assertEquals(2, cut.workstreams().size());
            assertEquals(4, cut.stagedPlanCount());
            assertEquals("relay-auth-v2", cut.plan("relay-auth-v2").planId());
        }
    }

    @Test
    @DisplayName("serves the control view of the draft with its workstreams and plans")
    void servesTheControlView() throws Exception {
        try (var runtime = MockRuntime.start(STATE)) {
            var res = get(runtime, "/api/v1/epics/relay-hardening/decomposition");
            assertEquals(200, res.status());
            var decomposition = DecompositionResource.fromJson(res.bodyText());
            assertEquals("relay-hardening", decomposition.epicId());
            assertEquals(2, decomposition.workstreams().size());
            assertEquals(4, decomposition.planCount());
            assertEquals("relay-reconnect-backoff",
                    decomposition.workstreams().getFirst().plans().getFirst().planId());
            assertEquals(List.of("pm-relay/src/reconnect"),
                    decomposition.workstreams().getFirst().plans().getFirst().surface());
        }
    }

    @Test
    @DisplayName("checks the surface disjointness and names the overlapping plans")
    void shouldCheckSurfaceDisjointness() throws Exception {
        try (var runtime = MockRuntime.start(STATE)) {
            var res = post(runtime, BASE + "relay-hardening/decomposition/check", "{}");
            assertEquals(200, res.status());

            var cut = EpicDecomposition.fromJson(res.bodyText());
            assertFalse(cut.everyCheckPasses());
            assertEquals(DecompositionState.CHECK, cut.status());
            var overlap = cut.findings().stream()
                    .filter(finding -> DecompositionCheckFinding.RULE_SURFACE_OVERLAP.equals(finding.rule()))
                    .findFirst().orElseThrow();
            assertEquals(CheckSeverity.ERROR, overlap.severity());
            assertTrue(overlap.affectedPlans().containsAll(List.of("relay-auth-v2", "relay-cursor-order")));
        }
    }

    @Test
    @DisplayName("reports a dependency cycle")
    void shouldReportDependencyCycle() throws Exception {
        try (var runtime = MockRuntime.start(STATE)) {
            var res = post(runtime, BASE + "storage-migration/decomposition/check", "{}");
            assertEquals(200, res.status());

            var cut = EpicDecomposition.fromJson(res.bodyText());
            assertTrue(cut.findings().stream().anyMatch(
                    finding -> DecompositionCheckFinding.RULE_DEPENDENCY_CYCLE.equals(finding.rule())));
            assertFalse(cut.everyCheckPasses());
        }
    }

    @Test
    @DisplayName("instructs a rewrite, reads it back, and lets the checks pass afterwards")
    void shouldInstructRewriteAndPassChecks() throws Exception {
        try (var runtime = MockRuntime.start(STATE)) {
            var instruct = new DecompositionInstructRequest("relay-auth-v2",
                    "Keep it out of pm-api: the exchange belongs into pm-relay.", InstructAction.EDIT);
            var instructed = post(runtime, BASE + "relay-hardening/decomposition/instruct",
                    instruct.toJson());
            assertEquals(200, instructed.status());

            var rewritten = EpicDecomposition.fromJson(instructed.bodyText());
            assertEquals(DecompositionState.READ_BACK, rewritten.status());
            assertTrue(rewritten.plan("relay-auth-v2").expectedSurface().getFirst().startsWith("pm-relay"));

            var checked = post(runtime, BASE + "relay-hardening/decomposition/check", "{}");
            var afterCheck = EpicDecomposition.fromJson(checked.bodyText());
            assertTrue(afterCheck.everyCheckPasses());
            assertEquals(DecompositionState.REVIEW, afterCheck.status());
        }
    }

    @Test
    @DisplayName("refuses a commit while a check fails")
    void shouldRefuseCommitWhileCheckFails() throws Exception {
        try (var runtime = MockRuntime.start(STATE)) {
            var res = post(runtime, BASE + "relay-hardening/decomposition/commit", "{}");
            assertEquals(409, res.status());
            assertEquals("checks_fail", JsonTree.string(JsonTree.asObject(JsonTree.parse(res.bodyText())),
                    "error"));
        }
    }

    @Test
    @DisplayName("commits an agreed cut and broadcasts the launchable plans")
    void shouldCommitAndBroadcast() throws Exception {
        try (var runtime = MockRuntime.start(STATE)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var instruct = new DecompositionInstructRequest("relay-auth-v2",
                    "the exchange belongs into pm-relay instead", InstructAction.EDIT);
            post(runtime, BASE + "relay-hardening/decomposition/instruct", instruct.toJson());

            try (var exchange = client.open(HttpRequest.get("/api/v1/events")
                         .withHeader(AUTH_HEADER, auth))) {
                assertTrue(exchange.isEventStream());
                var sse = exchange.events();

                var res = post(runtime, BASE + "relay-hardening/decomposition/commit", "{}");
                assertEquals(200, res.status());

                var result = DecompositionCommitResult.fromJson(res.bodyText());
                assertEquals(DecompositionCommitResult.STATUS_COMMITTED, result.status());
                assertEquals(4, result.ledgerRevision());
                assertEquals(4, result.launchablePlanCount());

                var updated = sse.next();
                assertNotNull(updated);
                assertEquals("epic.decomposition_updated", updated.event());

                var launchable = sse.next();
                assertNotNull(launchable);
                assertEquals("epic.launchable", launchable.event());
                var data = JsonTree.asObject(JsonTree.parse(launchable.data()));
                assertEquals("relay-hardening", JsonTree.string(data, "epic_id"));
            }
        }
    }

    @Test
    @DisplayName("adds a plan through an add instruction")
    void shouldAddPlan() throws Exception {
        try (var runtime = MockRuntime.start(STATE)) {
            var instruct = new DecompositionInstructRequest(null, "add a docs plan for pm-relay",
                    InstructAction.ADD);
            var res = post(runtime, BASE + "relay-hardening/decomposition/instruct", instruct.toJson());
            assertEquals(200, res.status());

            var cut = EpicDecomposition.fromJson(res.bodyText());
            assertEquals(5, cut.stagedPlanCount());
            assertTrue(cut.workstreams().stream().flatMap(ws -> ws.stagedPlans().stream())
                    .anyMatch(plan -> "new".equals(plan.status())));
        }
    }

    @Test
    @DisplayName("answers 404 for an epic without a decomposition and for an unknown project")
    void refusesUnknownEpicAndProject() throws Exception {
        var unknown = Generators.letterStrings(4, 10).next().toLowerCase();
        try (var runtime = MockRuntime.start(STATE)) {
            assertEquals(404, get(runtime, BASE + unknown + "/decomposition").status());
            assertEquals(404, get(runtime,
                    "/api/v1/projects/" + unknown + "/epics/relay-hardening/decomposition").status());
        }
    }

    private static HttpResponse get(MockRuntime runtime, String path) throws Exception {
        return new UnixSocketHttpClient(runtime.socketPath())
                .send(HttpRequest.get(path).withHeader(AUTH_HEADER, "Bearer " + runtime.token()));
    }

    private static HttpResponse post(MockRuntime runtime, String path, String body) throws Exception {
        return new UnixSocketHttpClient(runtime.socketPath())
                .send(HttpRequest.json("POST", path, body).withHeader(AUTH_HEADER, "Bearer " + runtime.token()));
    }
}
