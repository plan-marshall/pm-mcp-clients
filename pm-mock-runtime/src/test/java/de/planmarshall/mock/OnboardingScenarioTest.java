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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Map;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.onboarding.MachineOnboardingState;
import de.planmarshall.api.onboarding.MachineSettingsRecord;
import de.planmarshall.api.onboarding.MachineSettingsUpdateResult;
import de.planmarshall.api.onboarding.OnboardingAspect;
import de.planmarshall.api.onboarding.OnboardingStepCompletedEvent;

@DisplayName("OnboardingScenario")
@EnabledOnOs({OS.MAC, OS.LINUX})
class OnboardingScenarioTest {

    @Test
    @DisplayName("loads the onboarding scenario and registers it among the standard scenarios")
    void shouldLoadOnboardingScenario() throws Exception {
        assertTrue(StandardScenarios.ALL.contains(StandardScenarios.ONBOARDING));
        try (var runtime = MockRuntime.start(StandardScenarios.ONBOARDING)) {
            assertEquals(StandardScenarios.ONBOARDING, runtime.currentScenarioName());
            assertEquals("onboard.mode", runtime.currentStateName());
        }
    }

    @Nested
    @DisplayName("Guided run")
    class GuidedRun {

        @Test
        @DisplayName("steps the aspects, holds on a declined consent, and broadcasts step_completed")
        void shouldStepThroughTheAspects() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.ONBOARDING)) {
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = Map.of("Authorization", "Bearer " + runtime.token());

                try (var exchange = client.open(HttpRequest.get("/api/v1/events").withHeader("Authorization", auth.get("Authorization")))) {
                    assertTrue(exchange.isEventStream());
                    var sse = exchange.events();

                    assertEquals("language.choose", step(client, auth, "begin").get("state"));

                    // language.choose -> harness.consent crosses the language aspect
                    var next = step(client, auth, "set");
                    assertEquals("harness.consent", next.get("state"));

                    var event = sse.next();
                    assertNotNull(event);
                    assertEquals(OnboardingStepCompletedEvent.eventName(), event.event());
                    var data = JsonTree.asObject(JsonTree.parse(event.data()));
                    assertEquals(OnboardingAspect.LANGUAGE.wireValue(), JsonTree.string(data, "aspect"));
                    assertEquals("harness.consent", JsonTree.string(data, "state"));
                    assertEquals(1, JsonTree.integer(data, "completed_steps"));

                    // a declined required consent stays
                    assertEquals("harness.consent", step(client, auth, "no").get("state"));
                    // accepting it moves on
                    var chosen = step(client, auth, "yes");
                    assertEquals("harness.choose", chosen.get("state"));
                    assertEquals(1, JsonTree.integer(chosen, "completed_steps"));
                    assertNotEquals(Boolean.TRUE, chosen.get("auto"));
                }
            }
        }

        @Test
        @DisplayName("serves the current state over GET /api/v1/onboarding/state")
        void shouldServeCurrentState() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.ONBOARDING)) {
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = Map.of("Authorization", "Bearer " + runtime.token());

                var res = client.send(HttpRequest.get("/api/v1/onboarding/state")
                        .withHeader("Authorization", auth.get("Authorization")));
                assertEquals(200, res.status());
                var map = JsonTree.asObject(JsonTree.parse(new String(res.body(), StandardCharsets.UTF_8)));
                var state = MachineOnboardingState.fromMap(map);
                assertEquals("onboard.mode", state.state());
                assertFalse(state.auto());
                assertEquals(0, state.completedSteps());
            }
        }

        @Test
        @DisplayName("starts the automatic run at its consent")
        void shouldStartAutomaticRun() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.ONBOARDING)) {
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = "Bearer " + runtime.token();

                var res = client.send(HttpRequest.json("POST", "/api/v1/onboarding/auto", "{}")
                        .withHeader("Authorization", auth));
                assertEquals(200, res.status());
                var map = JsonTree.asObject(JsonTree.parse(new String(res.body(), StandardCharsets.UTF_8)));
                assertEquals("auto.consent", JsonTree.string(map, "state"));
                assertEquals(Boolean.TRUE, JsonTree.bool(map, "auto"));
            }
        }
    }

    @Nested
    @DisplayName("Machine settings")
    class MachineSettings {

        @Test
        @DisplayName("serves the settings record and refuses a change against an older revision with conflict")
        void shouldGuardTheRevision() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.ONBOARDING)) {
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = "Bearer " + runtime.token();

                var get = client.send(HttpRequest.get("/api/v1/machine/settings").withHeader("Authorization", auth));
                assertEquals(200, get.status());
                var settings = MachineSettingsRecord.fromJson(new String(get.body(), StandardCharsets.UTF_8));
                assertEquals(7, settings.aspects().size());
                assertEquals(4, settings.revision());

                try (var exchange = client.open(HttpRequest.get("/api/v1/events").withHeader("Authorization", auth))) {
                    var sse = exchange.events();

                    var applied = client.send(HttpRequest.json("PUT", "/api/v1/machine/settings",
                            "{\"key\":\"documents_mode\",\"value\":\"copy\",\"revision\":4}")
                            .withHeader("Authorization", auth));
                    assertEquals(200, applied.status());
                    var appliedResult = MachineSettingsUpdateResult.fromJson(
                            new String(applied.body(), StandardCharsets.UTF_8));
                    assertTrue(appliedResult.applied());
                    assertEquals(5, appliedResult.revision());

                    var changed = sse.next();
                    assertNotNull(changed);
                    assertEquals("machine.settings_changed", changed.event());

                    var stale = client.send(HttpRequest.json("PUT", "/api/v1/machine/settings",
                            "{\"key\":\"documents_mode\",\"value\":\"move\",\"revision\":4}")
                            .withHeader("Authorization", auth));
                    assertEquals(409, stale.status());
                    var conflict = MachineSettingsUpdateResult.fromJson(
                            new String(stale.body(), StandardCharsets.UTF_8));
                    assertTrue(conflict.conflict());
                    assertEquals("copy", conflict.currentValue());
                }

                var reread = MachineSettingsRecord.fromJson(new String(
                        client.send(HttpRequest.get("/api/v1/machine/settings").withHeader("Authorization", auth)).body(),
                        StandardCharsets.UTF_8));
                assertEquals(5, reread.revision());
                assertTrue(reread.toJson().contains("copy"));
            }
        }

        @Test
        @DisplayName("withdraws a remembered consent and reports no consent left")
        void shouldWithdrawConsent() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.ONBOARDING)) {
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = "Bearer " + runtime.token();

                var res = client.send(HttpRequest.json("PUT", "/api/v1/machine/settings",
                        "{\"key\":\"consent:scan_harnesses\",\"revision\":4}")
                        .withHeader("Authorization", auth));
                assertEquals(200, res.status());

                var reread = MachineSettingsRecord.fromJson(new String(
                        client.send(HttpRequest.get("/api/v1/machine/settings").withHeader("Authorization", auth)).body(),
                        StandardCharsets.UTF_8));
                assertEquals(1, reread.consents().size());
                assertFalse(reread.toJson().contains("scan_harnesses"));
            }
        }

        @Test
        @DisplayName("applies plan defaults, cleanup durations, rejects an unknown mode, and is idempotent on an unknown consent")
        void shouldApplyTheSettingsKeys() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.ONBOARDING)) {
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = "Bearer " + runtime.token();

                assertEquals(200, put(client, auth,
                        "{\"key\":\"plan_defaults\",\"value\":\"landing.main\",\"revision\":4}"));
                assertEquals(200, put(client, auth,
                        "{\"key\":\"plan_defaults\",\"value\":\"method.tests-after\",\"revision\":5}"));
                assertEquals(200, put(client, auth,
                        "{\"key\":\"plan_defaults\",\"value\":\"ask.every-phase\",\"revision\":6}"));
                assertEquals(200, put(client, auth,
                        "{\"key\":\"cleanup_durations\",\"value\":\"7\",\"revision\":7}"));
                // An unknown documents mode is accepted but changes nothing
                assertEquals(200, put(client, auth,
                        "{\"key\":\"documents_mode\",\"value\":\"nope\",\"revision\":8}"));
                // An unknown consent kind changes nothing
                assertEquals(200, put(client, auth,
                        "{\"key\":\"consent:not_a_kind\",\"value\":\"x\",\"revision\":9}"));

                var settings = MachineSettingsRecord.fromJson(new String(
                        client.send(HttpRequest.get("/api/v1/machine/settings").withHeader("Authorization", auth)).body(),
                        StandardCharsets.UTF_8));
                assertEquals(10, settings.revision());
                assertTrue(settings.toJson().contains("landing.main"));
                assertTrue(settings.toJson().contains("tests-after"));
                assertEquals("ask", settings.documentsMode().wireValue());
                assertEquals(2, settings.consents().size());
            }
        }

        @Test
        @DisplayName("records the optional posture of a later run")
        void shouldRecordPosture() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.ONBOARDING)) {
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = "Bearer " + runtime.token();

                assertEquals(200, put(client, auth,
                        "{\"key\":\"plan_defaults\",\"value\":\"posture.full\",\"revision\":4}"));

                var settings = MachineSettingsRecord.fromJson(new String(
                        client.send(HttpRequest.get("/api/v1/machine/settings").withHeader("Authorization", auth)).body(),
                        StandardCharsets.UTF_8));
                assertEquals("full", settings.planDefaults().posture());
            }
        }
    }

    @Nested
    @DisplayName("State view and stepping")
    class StateView {

        @Test
        @DisplayName("steps to the first link when no name is given and reports completed steps through the settings")
        void shouldStepByDefaultAndReportSettingsSteps() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.ONBOARDING)) {
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = "Bearer " + runtime.token();

                // No link name: some offered link is followed (the map order is unspecified)
                var res = client.send(HttpRequest.json("POST", "/api/v1/onboarding/step", "{}")
                        .withHeader("Authorization", auth));
                assertEquals(200, res.status());
                var map = JsonTree.asObject(JsonTree.parse(new String(res.body(), StandardCharsets.UTF_8)));
                var chosen = JsonTree.string(map, "state");
                assertTrue("auto.consent".equals(chosen) || "language.choose".equals(chosen),
                        "unexpected state: " + chosen);

                // The settings states count as the seven completed steps
                runtime.goToState("settings.choose");
                var state = client.send(HttpRequest.get("/api/v1/onboarding/state").withHeader("Authorization", auth));
                var stateMap = JsonTree.asObject(JsonTree.parse(new String(state.body(), StandardCharsets.UTF_8)));
                assertEquals(7, JsonTree.integer(stateMap, "completed_steps"));
            }
        }

        @Test
        @DisplayName("an automatic start on a scenario without an automatic run stays where it is")
        void shouldStayWithoutAutoRun() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.STATUS_ONLY)) {
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = "Bearer " + runtime.token();

                var res = client.send(HttpRequest.json("POST", "/api/v1/onboarding/auto", "{}")
                        .withHeader("Authorization", auth));
                assertEquals(200, res.status());
                var map = JsonTree.asObject(JsonTree.parse(new String(res.body(), StandardCharsets.UTF_8)));
                assertEquals(Boolean.TRUE, JsonTree.bool(map, "auto"));
            }
        }
    }

    private static int put(UnixSocketHttpClient client, String auth, String body) throws Exception {
        return client.send(HttpRequest.json("PUT", "/api/v1/machine/settings", body)
                .withHeader("Authorization", auth)).status();
    }

    private static Map<String, Object> step(UnixSocketHttpClient client, Map<String, String> auth, String link)
            throws Exception {
        var res = client.send(HttpRequest.json("POST", "/api/v1/onboarding/step",
                "{\"link\":\"" + link + "\"}").withHeader("Authorization", auth.get("Authorization")));
        assertEquals(200, res.status(), "step '" + link + "' must be accepted");
        var map = JsonTree.asObject(JsonTree.parse(new String(res.body(), StandardCharsets.UTF_8)));
        assertNotNull(map);
        return map;
    }
}
