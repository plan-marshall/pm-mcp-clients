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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.interaction.OperatorScopeKind;
import de.planmarshall.api.interaction.OperatorStateResource;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.onboarding.AutoResultItem;
import de.planmarshall.api.onboarding.AutoResultReason;
import de.planmarshall.api.onboarding.ConsentKind;
import de.planmarshall.api.onboarding.DocumentsMode;
import de.planmarshall.api.onboarding.HarnessScanResult;
import de.planmarshall.api.onboarding.HarnessVerification;
import de.planmarshall.api.onboarding.HarnessVerificationStage;
import de.planmarshall.api.onboarding.MachineOnboardingState;
import de.planmarshall.api.onboarding.MachineSettingsRecord;
import de.planmarshall.api.onboarding.MachineSettingsUpdateResult;
import de.planmarshall.api.onboarding.OnboardingAspect;
import de.planmarshall.api.onboarding.OnboardingStepCompletedEvent;
import de.planmarshall.api.onboarding.TokenUsageCoverage;
import de.planmarshall.mock.scenario.ScenarioLoader;

@DisplayName("OnboardingScenario")
@EnabledOnOs({OS.MAC, OS.LINUX})
@EnableGeneratorController
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

    @Test
    @DisplayName("every state carries an OperatorStateResource whose links the scenario links serve")
    void shouldDecodeEveryStateAsAnOperatorState() throws Exception {
        var scenario = ScenarioLoader.load(StandardScenarios.ONBOARDING);
        // The consolidation of the guided, entry, device, and shared-interaction cases.
        assertTrue(scenario.states().size() >= 60, "states: " + scenario.states().size());

        for (var entry : scenario.states().entrySet()) {
            var representation = JsonTree.asObject(entry.getValue().representation());
            assertNotNull(representation, entry.getKey());
            var state = OperatorStateResource.fromMap(representation);

            assertEquals(entry.getKey(), state.stateId(), "state id of " + entry.getKey());
            assertEquals(OperatorScopeKind.MACHINE, state.scope().kind());
            assertEquals(entry.getValue().links().keySet(),
                    state.links().stream().map(link -> link.id()).collect(Collectors.toSet()),
                    "operator links of " + entry.getKey());
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
                    assertEquals(OnboardingStepCompletedEvent.EVENT_NAME, event.event());
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

        @Test
        @DisplayName("serves the recorded summary of each done step")
        void shouldServeStepSummaries() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.ONBOARDING)) {
                runtime.goToState("harness.choose");
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = "Bearer " + runtime.token();

                var res = client.send(HttpRequest.get("/api/v1/onboarding/state")
                        .withHeader("Authorization", auth));
                assertEquals(200, res.status());
                var map = JsonTree.asObject(JsonTree.parse(new String(res.body(), StandardCharsets.UTF_8)));
                var summaries = JsonTree.asObject(map.get("step_summaries"));

                // Only the step the run has passed reports a summary; the current one does not.
                assertEquals(1, summaries.size());
                assertEquals("Deutsch, models English", JsonTree.string(summaries, "1"));
                assertFalse(summaries.containsKey("2"));
            }
        }

        @Test
        @DisplayName("serves the automatic run's result items with their closed reasons")
        void shouldServeAutoResultItems() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.ONBOARDING)) {
                runtime.goToState("auto.result");
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = "Bearer " + runtime.token();

                var res = client.send(HttpRequest.get("/api/v1/onboarding/state")
                        .withHeader("Authorization", auth));
                assertEquals(200, res.status());
                var state = JsonTree.asObject(JsonTree.parse(new String(res.body(), StandardCharsets.UTF_8)));
                var items = AutoResultItem.listFromJson(JsonTree.write(state.get("results")));

                assertEquals(OnboardingAspect.values().length, items.size());
                var harness = items.stream()
                        .filter(item -> item.aspect() == OnboardingAspect.HARNESSES).findFirst().orElseThrow();
                assertEquals(AutoResultReason.ALL_SUPPORTED, harness.reason());
            }
        }

        @Test
        @DisplayName("reports the seven done aspects at the review step without completing the machine")
        void shouldReportTheReviewStep() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.ONBOARDING)) {
                runtime.goToState("onboard.summary");
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = "Bearer " + runtime.token();

                var stateRes = client.send(HttpRequest.get("/api/v1/onboarding/state")
                        .withHeader("Authorization", auth));
                var state = JsonTree.asObject(JsonTree.parse(new String(stateRes.body(), StandardCharsets.UTF_8)));
                assertEquals(7, JsonTree.integer(state, "completed_steps"));
                assertEquals(7, JsonTree.asObject(state.get("step_summaries")).size());

                var statusRes = client.send(HttpRequest.get("/api/v1/status")
                        .withHeader("Authorization", auth));
                var status = JsonTree.asObject(JsonTree.parse(new String(statusRes.body(), StandardCharsets.UTF_8)));
                var onboard = JsonTree.asObject(status.get("onboard"));
                assertEquals("onboard.summary", JsonTree.string(onboard, "state"));
                assertEquals(7, JsonTree.integer(onboard, "step"));
                assertEquals(Boolean.FALSE, onboard.get("completed"));
                assertEquals(Boolean.FALSE, status.get("onboarded"));
            }
        }

        @Test
        @DisplayName("keeps the route's onboarding progress for a state without an aspect")
        void shouldKeepTheRouteProgressWithoutAnAspect() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.ONBOARDING)) {
                runtime.goToState("auto.consent");
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = "Bearer " + runtime.token();

                var statusRes = client.send(HttpRequest.get("/api/v1/status")
                        .withHeader("Authorization", auth));
                var status = JsonTree.asObject(JsonTree.parse(new String(statusRes.body(), StandardCharsets.UTF_8)));
                assertEquals("onboard.mode", JsonTree.string(JsonTree.asObject(status.get("onboard")), "state"));
                assertEquals(Boolean.FALSE, status.get("onboarded"));
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
                var unknownMode = "unknown-" + Generators.letterStrings(4, 8).next();
                // An unknown documents mode is accepted but changes nothing
                assertEquals(200, put(client, auth,
                        "{\"key\":\"documents_mode\",\"value\":\"" + unknownMode + "\",\"revision\":8}"));
                // An unknown consent kind changes nothing
                assertEquals(200, put(client, auth,
                        "{\"key\":\"consent:" + Generators.letterStrings(4, 8).next()
                                + "\",\"value\":\"" + Generators.letterStrings(4, 8).next() + "\",\"revision\":9}"));

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

        @Test
        @DisplayName("rotates a credential, dropping its old expiry")
        void shouldRotateCredential() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.ONBOARDING)) {
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = "Bearer " + runtime.token();

                var credentialKind = Generators.letterStrings(4, 12).next();
                assertEquals(200, put(client, auth,
                        "{\"key\":\"credential:github\",\"value\":\"" + credentialKind + "\",\"revision\":4}"));

                var settings = MachineSettingsRecord.fromJson(new String(
                        client.send(HttpRequest.get("/api/v1/machine/settings")
                                .withHeader("Authorization", auth)).body(), StandardCharsets.UTF_8));
                var github = settings.credentials().stream()
                        .filter(credential -> "github".equals(credential.key())).findFirst().orElseThrow();
                assertEquals(credentialKind, github.kind());
                assertNull(github.expiresInDays());
                assertFalse(github.expired());
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

    @Nested
    @DisplayName("Case scenarios")
    class CaseScenarios {

        @Test
        @DisplayName("serves a complete, a partial, and a not-reported harness token report")
        void shouldServeHarnessTokenReports() throws Exception {
            assertTokenReport(StandardScenarios.ONBOARDING_HARNESS_TOKENS_COMPLETE, "claude",
                    TokenUsageCoverage.COMPLETE, List.of());
            assertTokenReport(StandardScenarios.ONBOARDING_HARNESS_TOKENS_PARTIAL, "opencode",
                    TokenUsageCoverage.PARTIAL, List.of("cache_read", "cache_write"));
            assertTokenReport(StandardScenarios.ONBOARDING_HARNESS_TOKENS_NOT_REPORTED, "codex",
                    TokenUsageCoverage.NOT_REPORTED, List.of("input"));
        }

        @Test
        @DisplayName("comes to the none-found step when every chosen harness fails its check")
        void shouldFallBackWhenEveryHarnessFails() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.ONBOARDING_HARNESS_ALL_FAILED)) {
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = Map.of("Authorization", "Bearer " + runtime.token());

                var probe = step(client, auth, "check");
                assertEquals("harness.probe-failed", probe.get("state"));
                var checks = JsonTree.asArray(probe.get("checks"));
                assertEquals(2, checks.size());
                for (var element : checks) {
                    assertEquals("fail", JsonTree.string(JsonTree.asObject(element), "result"));
                }

                var none = step(client, auth, "continue");
                assertEquals("harness.none", none.get("state"));
                assertEquals("No harness can be used", none.get("summary"));
                assertEquals("harness.none", step(client, auth, "later").get("state"));
            }
        }

        @Test
        @DisplayName("holds a required consent answered no and goes on without an optional one")
        void shouldRefuseEachConsent() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.ONBOARDING_REFUSALS)) {
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = Map.of("Authorization", "Bearer " + runtime.token());

                runtime.goToState("harness.consent");
                assertEquals("harness.consent", step(client, auth, "no").get("state"));
                assertEquals("harness.choose", step(client, auth, "yes").get("state"));

                runtime.goToState("catalogue.consent");
                assertEquals("catalogue.consent", step(client, auth, "no").get("state"));
                assertEquals("catalogue.select", step(client, auth, "no_advice").get("state"));

                runtime.goToState("store.consent");
                assertEquals("store.consent", step(client, auth, "no").get("state"));
                assertEquals("store.check", step(client, auth, "yes").get("state"));

                runtime.goToState("providers.consent");
                assertEquals("providers.consent", step(client, auth, "no").get("state"));
                assertEquals("providers.select", step(client, auth, "yes").get("state"));

                runtime.goToState("roles.consent");
                assertEquals("roles.review", step(client, auth, "no").get("state"));
                runtime.goToState("roles.consent");
                assertEquals("roles.validate", step(client, auth, "yes").get("state"));

                runtime.goToState("providers.verify-consent");
                assertEquals("providers.verify-consent", step(client, auth, "no").get("state"));
                assertEquals("providers.verify", step(client, auth, "yes").get("state"));

                runtime.goToState("defaults.review");
                assertEquals("defaults.validate", step(client, auth, "accept").get("state"));
            }
        }

        @Test
        @DisplayName("adds and verifies a harness the binary does not know, failing the usage stage")
        void shouldAddAnUnknownHarness() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.ONBOARDING_UNKNOWN_HARNESS)) {
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = Map.of("Authorization", "Bearer " + runtime.token());

                var add = step(client, auth, "add");
                assertEquals("harness.add", add.get("state"));
                assertEquals("aider", add.get("client_id"));
                assertEquals("opencode", add.get("start_from"));

                var consent = step(client, auth, "submit");
                assertEquals("harness.add-consent", consent.get("state"));
                assertEquals(ConsentKind.VERIFY_HARNESS_PROFILE.wireValue(), consent.get("kind"));

                assertEquals("harness.add", step(client, auth, "no").get("state"));
                assertEquals("harness.add-consent", step(client, auth, "submit").get("state"));

                var verified = step(client, auth, "yes");
                assertEquals("harness.verify-profile", verified.get("state"));
                var verification = HarnessVerification.fromMap(JsonTree.asObject(verified.get("verification")));
                assertFalse(verification.passed());
                assertEquals(HarnessVerificationStage.USAGE, verification.firstFailure().orElseThrow());
                assertNotNull(verification.usage());
                assertEquals(List.of("cache_write"), verification.usage().missingFigures());
            }
        }

        @Test
        @DisplayName("refuses a documents-mode change against an older revision and applies it again")
        void shouldRefuseAStaleSettingsChange() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.ONBOARDING_SETTINGS_CONFLICT)) {
                assertEquals("settings.conflict", runtime.currentStateName());
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = "Bearer " + runtime.token();

                var applied = client.send(HttpRequest.json("PUT", "/api/v1/machine/settings",
                        "{\"key\":\"documents_mode\",\"value\":\"copy\",\"revision\":4}")
                        .withHeader("Authorization", auth));
                assertEquals(200, applied.status());

                var stale = client.send(HttpRequest.json("PUT", "/api/v1/machine/settings",
                        "{\"key\":\"documents_mode\",\"value\":\"move\",\"revision\":4}")
                        .withHeader("Authorization", auth));
                assertEquals(409, stale.status());
                var conflict = MachineSettingsUpdateResult.fromJson(new String(stale.body(), StandardCharsets.UTF_8));
                assertTrue(conflict.conflict());
                assertEquals("copy", conflict.currentValue());

                var again = client.send(HttpRequest.json("PUT", "/api/v1/machine/settings",
                        "{\"key\":\"documents_mode\",\"value\":\"move\",\"revision\":5}")
                        .withHeader("Authorization", auth));
                assertEquals(200, again.status());
                var reread = MachineSettingsRecord.fromJson(new String(
                        client.send(HttpRequest.get("/api/v1/machine/settings")
                                .withHeader("Authorization", auth)).body(), StandardCharsets.UTF_8));
                assertEquals(DocumentsMode.MOVE, reread.documentsMode());
            }
        }

        private static void assertTokenReport(String scenario, String harnessId, TokenUsageCoverage coverage,
                List<String> missingFigures) throws Exception {
            try (var runtime = MockRuntime.start(scenario)) {
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = "Bearer " + runtime.token();

                var res = client.send(HttpRequest.get("/api/v1/onboarding/state").withHeader("Authorization", auth));
                assertEquals(200, res.status());
                var state = JsonTree.asObject(JsonTree.parse(new String(res.body(), StandardCharsets.UTF_8)));
                var harness = findHarness(state, harnessId);

                assertEquals(coverage, harness.tokenUsage());
                assertNotNull(harness.usage());
                assertEquals(coverage, harness.usage().coverage());
                assertEquals(missingFigures, harness.usage().missingFigures());
            }
        }

        private static HarnessScanResult findHarness(Map<String, Object> state, String harnessId) throws Exception {
            var array = JsonTree.asArray(state.get("harnesses"));
            assertNotNull(array, "the state must list the scanned harnesses");
            for (var element : array) {
                var harness = HarnessScanResult.fromMap(JsonTree.asObject(element));
                if (harnessId.equals(harness.harnessId())) {
                    return harness;
                }
            }
            throw new AssertionError("harness not found in the scan: " + harnessId);
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
