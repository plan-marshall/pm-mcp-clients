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

import de.planmarshall.api.decomposition.CheckSeverity;
import de.planmarshall.api.decomposition.DecompositionCheckFinding;
import de.planmarshall.api.decomposition.DecompositionCommitResult;
import de.planmarshall.api.decomposition.DecompositionInstructRequest;
import de.planmarshall.api.decomposition.DecompositionState;
import de.planmarshall.api.decomposition.EpicDecomposition;
import de.planmarshall.api.decomposition.InstructAction;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.json.JsonTree;

@DisplayName("DecompositionScenario")
@EnabledOnOs({OS.MAC, OS.LINUX})
class DecompositionScenarioTest {

    private static final String BASE = "/api/v1/projects/pm-mcp-clients/epics/";

    @Test
    @DisplayName("loads the decomposition scenario and serves the drafted cut")
    void shouldLoadScenarioAndServeCut() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.DECOMPOSITION)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            assertEquals(StandardScenarios.DECOMPOSITION, runtime.currentScenarioName());
            assertEquals("decomposition.review", runtime.currentStateName());

            var res = client.send(HttpRequest.get(BASE + "relay-hardening/decomposition")
                    .withHeader("Authorization", auth));
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
    @DisplayName("checks the surface disjointness and names the overlapping plans")
    void shouldCheckSurfaceDisjointness() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.DECOMPOSITION)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var res = client.send(HttpRequest.json("POST", BASE + "relay-hardening/decomposition/check", "{}")
                    .withHeader("Authorization", auth));
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
        try (var runtime = MockRuntime.start(StandardScenarios.DECOMPOSITION)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var res = client.send(HttpRequest.json("POST", BASE + "storage-migration/decomposition/check", "{}")
                    .withHeader("Authorization", auth));
            assertEquals(200, res.status());

            var cut = EpicDecomposition.fromJson(res.bodyText());
            assertTrue(cut.findings().stream().anyMatch(
                    finding -> DecompositionCheckFinding.RULE_DEPENDENCY_CYCLE.equals(finding.rule())));
            assertFalse(cut.everyCheckPasses());
        }
    }

    @Test
    @DisplayName("instructs a rewrite and lets the checks pass afterwards")
    void shouldInstructRewriteAndPassChecks() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.DECOMPOSITION)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var instruct = new DecompositionInstructRequest("relay-auth-v2",
                    "Keep it out of pm-api: the exchange belongs into pm-relay.", InstructAction.EDIT);
            var instructed = client.send(HttpRequest.json("POST", BASE + "relay-hardening/decomposition/instruct",
                    instruct.toJson()).withHeader("Authorization", auth));
            assertEquals(200, instructed.status());

            var rewritten = EpicDecomposition.fromJson(instructed.bodyText());
            assertEquals(DecompositionState.READ_BACK, rewritten.status());
            var plan = rewritten.plan("relay-auth-v2");
            assertEquals("relay-auth-v2", plan.planId());
            assertTrue(plan.expectedSurface().getFirst().startsWith("pm-relay"));
            assertEquals("relay-auth-v2", rewritten.plan("relay-auth-v2").planId());

            var checked = client.send(HttpRequest.json("POST", BASE + "relay-hardening/decomposition/check", "{}")
                    .withHeader("Authorization", auth));
            var afterCheck = EpicDecomposition.fromJson(checked.bodyText());
            assertTrue(afterCheck.everyCheckPasses());
            assertEquals(DecompositionState.REVIEW, afterCheck.status());
        }
    }

    @Test
    @DisplayName("refuses a commit while a check fails")
    void shouldRefuseCommitWhileCheckFails() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.DECOMPOSITION)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var res = client.send(HttpRequest.json("POST", BASE + "relay-hardening/decomposition/commit", "{}")
                    .withHeader("Authorization", auth));
            assertEquals(409, res.status());
            var body = JsonTree.asObject(JsonTree.parse(res.bodyText()));
            assertEquals("checks_fail", JsonTree.string(body, "error"));
        }
    }

    @Test
    @DisplayName("commits an agreed cut and broadcasts the launchable plans")
    void shouldCommitAndBroadcast() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.DECOMPOSITION)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var instruct = new DecompositionInstructRequest("relay-auth-v2",
                    "the exchange belongs into pm-relay instead", InstructAction.EDIT);
            client.send(HttpRequest.json("POST", BASE + "relay-hardening/decomposition/instruct",
                    instruct.toJson()).withHeader("Authorization", auth));

            try (var exchange = client.open(HttpRequest.get("/api/v1/events").withHeader("Authorization", auth))) {
                assertTrue(exchange.isEventStream());
                var sse = exchange.events();

                var res = client.send(HttpRequest.json("POST", BASE + "relay-hardening/decomposition/commit", "{}")
                        .withHeader("Authorization", auth));
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
    @DisplayName("answers 404 for an epic without a decomposition")
    void shouldRefuseUnknownEpic() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.DECOMPOSITION)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var res = client.send(HttpRequest.get(BASE + "no-such-epic/decomposition")
                    .withHeader("Authorization", auth));
            assertEquals(404, res.status());
        }
    }

    @Test
    @DisplayName("answers 404 for an unknown project")
    void shouldRefuseUnknownProject() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.DECOMPOSITION)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var res = client.send(HttpRequest.get("/api/v1/projects/no-such-project/epics/relay-hardening/decomposition")
                    .withHeader("Authorization", auth));
            assertEquals(404, res.status());
        }
    }

    @Test
    @DisplayName("adds a plan through an add instruction")
    void shouldAddPlan() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.DECOMPOSITION)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var instruct = new DecompositionInstructRequest(null, "add a docs plan for pm-relay",
                    InstructAction.ADD);
            var res = client.send(HttpRequest.json("POST", BASE + "relay-hardening/decomposition/instruct",
                    instruct.toJson()).withHeader("Authorization", auth));
            assertEquals(200, res.status());

            var cut = EpicDecomposition.fromJson(res.bodyText());
            assertEquals(5, cut.stagedPlanCount());
            assertTrue(cut.workstreams().stream().flatMap(ws -> ws.stagedPlans().stream())
                    .anyMatch(plan -> "new".equals(plan.status())));
        }
    }
}
