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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.planmarshall.api.control.ActionAvailability;
import de.planmarshall.api.control.HoldRecord;
import de.planmarshall.api.control.PlanSettingsRecord;
import de.planmarshall.api.control.PlanSettingsUpdateResult;
import de.planmarshall.api.control.WorkActions;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.json.JsonTree;

@DisplayName("ControlScenario")
@EnabledOnOs({OS.MAC, OS.LINUX})
class ControlScenarioTest {

    private static final String AUTH_HEADER = "Authorization";

    @Test
    @DisplayName("loads the control scenario and serves a running plan's actions")
    void shouldServePlanActions() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.CONTROL)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            assertEquals(StandardScenarios.CONTROL, runtime.currentScenarioName());
            assertEquals("control.start", runtime.currentStateName());

            var res = client.send(HttpRequest.get("/api/v1/plans/fix-relay/actions").withHeader(AUTH_HEADER, auth));
            assertEquals(200, res.status());
            var actions = WorkActions.fromJson(new String(res.body()));
            assertEquals("fix-relay", actions.subjectId());
            assertEquals("plan", actions.subjectKind());
            assertEquals("running", actions.state());
            assertEquals("relay-hardening", actions.epicId());
            assertTrue(actions.action("pause").available());
            assertTrue(actions.action("cancel").available());
            assertNull(actions.action("abandon"));

            // A held plan names its hold kind in its actions.
            var heldRes = client.send(HttpRequest.get("/api/v1/plans/relay-docs/actions").withHeader(AUTH_HEADER, auth));
            var heldActions = WorkActions.fromJson(new String(heldRes.body()));
            assertEquals("held", heldActions.state());
            assertEquals("held", heldActions.holdKind());
        }
    }

    @Test
    @DisplayName("pauses a plan at its point, then starts it again, broadcasting the transition")
    void shouldPauseAndResumePlan() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.CONTROL)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            try (var exchange = client.open(HttpRequest.get("/api/v1/events").withHeader(AUTH_HEADER, auth))) {
                assertTrue(exchange.isEventStream());
                var sse = exchange.events();

                var pauseBody = JsonTree.write(Map.of("point", "phase"));
                var pauseRes = client.send(HttpRequest.json("POST", "/api/v1/plans/fix-relay/actions/pause", pauseBody)
                        .withHeader(AUTH_HEADER, auth));
                assertEquals(200, pauseRes.status());
                var paused = WorkActions.fromJson(new String(pauseRes.body()));
                assertEquals("pausing", paused.state());
                assertEquals("phase", paused.pausingPoint());
                assertTrue(paused.action("start").available());

                var pausedEvent = sse.next();
                assertNotNull(pausedEvent);
                assertEquals("plan.paused", pausedEvent.event());
                var pausedData = JsonTree.asObject(JsonTree.parse(pausedEvent.data()));
                assertEquals("phase", JsonTree.string(pausedData, "point"));

                var startRes = client.send(HttpRequest.json("POST", "/api/v1/plans/fix-relay/actions/start", "{}")
                        .withHeader(AUTH_HEADER, auth));
                assertEquals(200, startRes.status());
                assertEquals("running", WorkActions.fromJson(new String(startRes.body())).state());

                var resumedEvent = sse.next();
                assertNotNull(resumedEvent);
                assertEquals("plan.resumed", resumedEvent.event());
            }
        }
    }

    @Test
    @DisplayName("cancels a plan with its reason")
    void shouldCancelPlan() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.CONTROL)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            try (var exchange = client.open(HttpRequest.get("/api/v1/events").withHeader(AUTH_HEADER, auth))) {
                var sse = exchange.events();
                var body = JsonTree.write(Map.of("reason", "replaced by relay-v2"));
                var res = client.send(HttpRequest.json("POST", "/api/v1/plans/fix-relay/actions/cancel", body)
                        .withHeader(AUTH_HEADER, auth));
                assertEquals(200, res.status());
                assertEquals("ended", WorkActions.fromJson(new String(res.body())).state());

                var event = sse.next();
                assertNotNull(event);
                assertEquals("plan.cancelled", event.event());
                var data = JsonTree.asObject(JsonTree.parse(event.data()));
                assertEquals("replaced by relay-v2", JsonTree.string(data, "reason"));
            }
        }
    }

    @Test
    @DisplayName("lists an integrity pause and still holds after a retry, releasing after an accept")
    void shouldResolveIntegrityHold() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.CONTROL)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var listRes = client.send(HttpRequest.get("/api/v1/plans/relay-cursor-order/holds").withHeader(AUTH_HEADER, auth));
            assertEquals(200, listRes.status());
            var holds = JsonTree.asArray(JsonTree.parse(listRes.body()));
            assertNotNull(holds);
            assertEquals(1, holds.size());
            var hold = HoldRecord.fromMap(JsonTree.asObject(holds.getFirst()));
            assertEquals("integrity_pause", hold.kind());
            assertEquals("references.json", hold.filePath());
            assertEquals(4812L, hold.recordedSize());
            assertTrue(hold.offers("accept"));
            assertTrue(hold.offers("abort"));

            try (var exchange = client.open(HttpRequest.get("/api/v1/events").withHeader(AUTH_HEADER, auth))) {
                var sse = exchange.events();

                var retryRes = client.send(HttpRequest.json("POST",
                        "/api/v1/plans/relay-cursor-order/holds/H-0001/resolve",
                        JsonTree.write(Map.of("act", "retry")))
                        .withHeader(AUTH_HEADER, auth));
                assertEquals(200, retryRes.status());
                var retryResult = JsonTree.asObject(JsonTree.parse(retryRes.body()));
                assertEquals(Boolean.FALSE, retryResult.get("released"));
                assertNotNull(JsonTree.string(retryResult, "cause"));

                var changed = sse.next();
                assertNotNull(changed);
                assertEquals("hold.changed", changed.event());

                var acceptRes = client.send(HttpRequest.json("POST",
                        "/api/v1/plans/relay-cursor-order/holds/H-0001/resolve",
                        JsonTree.write(Map.of("act", "accept")))
                        .withHeader(AUTH_HEADER, auth));
                assertEquals(200, acceptRes.status());
                assertEquals(Boolean.TRUE, JsonTree.asObject(JsonTree.parse(acceptRes.body())).get("released"));

                var cleared = sse.next();
                assertNotNull(cleared);
                assertEquals("hold.cleared", cleared.event());

                // The plan goes on from where it rested.
                var actionsRes = client.send(HttpRequest.get("/api/v1/plans/relay-cursor-order/actions").withHeader(AUTH_HEADER, auth));
                assertEquals("running", WorkActions.fromJson(new String(actionsRes.body())).state());
            }
        }
    }

    @Test
    @DisplayName("guards plan settings against a stale revision and refused landing")
    void shouldGuardPlanSettings() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.CONTROL)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var getRes = client.send(HttpRequest.get("/api/v1/plans/fix-relay/settings").withHeader(AUTH_HEADER, auth));
            assertEquals(200, getRes.status());
            var settings = PlanSettingsRecord.fromJson(new String(getRes.body()));
            assertEquals("fix-relay", settings.planId());
            assertEquals(7, settings.revision());
            assertEquals("project", settings.value("landing").origin());
            assertEquals("plan", settings.value("asking").origin());

            var staleBody = JsonTree.write(Map.of("aspect", "method", "value", "method.tests-after", "revision", 6));
            var staleRes = client.send(HttpRequest.json("PUT", "/api/v1/plans/fix-relay/settings", staleBody)
                    .withHeader(AUTH_HEADER, auth));
            assertEquals(409, staleRes.status());
            var conflict = PlanSettingsUpdateResult.fromJson(new String(staleRes.body()));
            assertTrue(conflict.conflict());
            assertEquals("method.test-first", conflict.currentValue());

            var applyBody = JsonTree.write(Map.of("aspect", "method", "value", "method.tests-after", "revision", 7));
            var applyRes = client.send(HttpRequest.json("PUT", "/api/v1/plans/fix-relay/settings", applyBody)
                    .withHeader(AUTH_HEADER, auth));
            assertEquals(200, applyRes.status());
            var applied = PlanSettingsUpdateResult.fromJson(new String(applyRes.body()));
            assertTrue(applied.applied());
            assertEquals(8, applied.revision());

            var refusedBody = JsonTree.write(Map.of("aspect", "landing", "value", "landing.main", "revision", 8));
            var refusedRes = client.send(HttpRequest.json("PUT", "/api/v1/plans/fix-relay/settings", refusedBody)
                    .withHeader(AUTH_HEADER, auth));
            assertEquals(400, refusedRes.status());
            assertEquals("refused", JsonTree.string(JsonTree.asObject(JsonTree.parse(refusedRes.body())), "error"));
        }
    }

    @Test
    @DisplayName("serves epic actions with archive disabled while the ledger lands, and toggles automatic")
    void shouldServeEpicActions() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.CONTROL)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var landingRes = client.send(HttpRequest.get("/api/v1/epics/relay-docs-epic/actions").withHeader(AUTH_HEADER, auth));
            assertEquals(200, landingRes.status());
            var landing = WorkActions.fromJson(new String(landingRes.body()));
            assertEquals("epic", landing.subjectKind());
            assertEquals("landing", landing.state());
            ActionAvailability archive = landing.action("archive");
            assertNotNull(archive);
            assertFalse(archive.available());
            assertEquals("the ledger is landing", archive.reason());

            var runningRes = client.send(HttpRequest.get("/api/v1/epics/relay-hardening/actions").withHeader(AUTH_HEADER, auth));
            var running = WorkActions.fromJson(new String(runningRes.body()));
            assertEquals("running", running.state());
            assertTrue(running.action("next").available());
            assertTrue(running.action("auto").available());

            var autoBody = JsonTree.write(Map.of("enabled", false));
            var autoRes = client.send(HttpRequest.json("POST", "/api/v1/epics/relay-hardening/actions/auto", autoBody)
                    .withHeader(AUTH_HEADER, auth));
            assertEquals(200, autoRes.status());
            assertEquals(Boolean.FALSE, JsonTree.asObject(JsonTree.parse(autoRes.body())).get("auto_enabled"));
        }
    }

    @Test
    @DisplayName("returns 404 for an action on an unknown plan")
    void shouldReturn404ForUnknownPlan() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.CONTROL)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var res = client.send(HttpRequest.json("POST", "/api/v1/plans/unknown-plan/actions/pause", "{}")
                    .withHeader(AUTH_HEADER, auth));
            assertEquals(404, res.status());
        }
    }
}
