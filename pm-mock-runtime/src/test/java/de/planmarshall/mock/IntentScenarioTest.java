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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.HttpResponse;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.intent.BuilderCheckResource;
import de.planmarshall.api.intent.CriterionStatus;
import de.planmarshall.api.intent.DeliverableRecord;
import de.planmarshall.api.intent.DoneWhenCheck;
import de.planmarshall.api.intent.EpicIntentResource;
import de.planmarshall.api.intent.IntentDecider;
import de.planmarshall.api.intent.OutlineResource;
import de.planmarshall.api.intent.OverSizeDistributionResource;
import de.planmarshall.api.intent.PlanIntentResource;
import de.planmarshall.api.interaction.OperatorStateResource;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.mock.scenario.ScenarioLoader;

@DisplayName("Intent scenarios")
@EnabledOnOs({OS.MAC, OS.LINUX})
class IntentScenarioTest {

    private static final String AUTH_HEADER = "Authorization";
    private static final String STATE = StandardScenarios.RUNNING_WORK + "#intent.ready";

    @Nested
    @DisplayName("The plan's intent record")
    class PlanIntent {

        @Test
        @DisplayName("serves the intent record with its self-review and builder check")
        void servesIntentRecord() throws Exception {
            try (var runtime = MockRuntime.start(STATE)) {
                assertEquals(StandardScenarios.RUNNING_WORK, runtime.currentScenarioName());
                assertEquals("intent.ready", runtime.currentStateName());

                var response = get(runtime, "/api/v1/plans/fix-relay/intent");
                assertEquals(200, response.status(), response.bodyText());

                var intent = PlanIntentResource.fromJson(response.bodyText());
                assertEquals("fix-relay", intent.planId());
                assertEquals(3, intent.revision());
                assertEquals(4, intent.criteria().size());
                assertEquals(2, intent.operatorDecisions().size());
                assertEquals(1, intent.modelCalls().size());
                assertEquals(1, intent.notVerifiedCount());
                assertTrue(intent.hasNotVerified());
                assertEquals(1, intent.notMetCount());
                assertEquals(2, intent.metCount());

                var notVerified = intent.criterion("AC-003");
                assertEquals(CriterionStatus.NOT_VERIFIED, notVerified.status());
                assertEquals("The mock harness does not expose the reconnect counter.",
                        notVerified.statusReason());

                var check = intent.builderCheck();
                assertNotNull(check);
                assertEquals(BuilderCheckResource.Resolution.PENDING, check.resolution());
                assertEquals(1, check.operatorQuestions().size());
                assertEquals(1, check.recommendedQuestions().size());
                assertEquals("BQ-001", check.operatorQuestions().getFirst().id());
                assertTrue(check.operatorQuestions().getFirst().needsOperator());
                assertEquals("BQ-003", check.questions().get(2).id());
                assertEquals("AC-002", check.questions().get(2).answer());

                assertTrue(intent.decisions().stream()
                        .anyMatch(decision -> decision.decider() == IntentDecider.FACT));
            }
        }

        @Test
        @DisplayName("answers another plan id from the same fixture")
        void servesAnotherPlanId() throws Exception {
            try (var runtime = MockRuntime.start(STATE)) {
                var response = get(runtime, "/api/v1/plans/other-plan/intent");
                assertEquals(200, response.status(), response.bodyText());
                assertEquals("other-plan", PlanIntentResource.fromJson(response.bodyText()).planId());
            }
        }

        @Test
        @DisplayName("a path without an id is answered mock_no_route")
        void pathWithoutId() throws Exception {
            try (var runtime = MockRuntime.start(STATE)) {
                assertEquals(404, get(runtime, "/api/v1/plans/intent").status());
                assertEquals(404, get(runtime, "/api/v1/plans/outline").status());
                assertEquals(404, get(runtime, "/api/v1/epics/intent").status());
                assertEquals(404, get(runtime, "/api/v1/projects/metrics/oversize").status());
            }
        }
    }

    @Nested
    @DisplayName("The plan's outline record")
    class Outline {

        @Test
        @DisplayName("serves the deliverables, affected files, dependencies, and missing key link")
        void servesOutlineRecord() throws Exception {
            try (var runtime = MockRuntime.start(STATE)) {
                var response = get(runtime, "/api/v1/plans/fix-relay/outline");
                assertEquals(200, response.status(), response.bodyText());

                var outline = OutlineResource.fromJson(response.bodyText());
                assertEquals("fix-relay", outline.planId());
                assertEquals(2, outline.revision());
                assertEquals(3, outline.deliverables().size());
                assertEquals(1, outline.addedDependencies().size());
                assertEquals("de.cuioss:cui-http", outline.addedDependencies().getFirst().name());
                assertEquals(1, outline.addedDependencies().getFirst().deliverable());
                assertEquals(1, outline.satisfying("AC-001").size());
                assertTrue(outline.hasMissingKeyLink());
                assertEquals("EventQueue -> RelayStatus", outline.missingKeyLink());

                var second = outline.deliverable(2);
                assertNotNull(second);
                assertEquals(1, second.dependsOn().size());
                assertEquals(DeliverableRecord.FileIntent.WRITE_NEW,
                        second.affectedFiles().getFirst().intent());
                assertNull(outline.deliverable(3).verification());
            }
        }
    }

    @Nested
    @DisplayName("The epic's intent")
    class EpicIntent {

        @Test
        @DisplayName("serves the done-when checks with their evidence and each plan's criterion")
        void servesEpicIntent() throws Exception {
            try (var runtime = MockRuntime.start(STATE)) {
                var response = get(runtime, "/api/v1/epics/relay-hardening/intent");
                assertEquals(200, response.status(), response.bodyText());

                var intent = EpicIntentResource.fromJson(response.bodyText());
                assertEquals("relay-hardening", intent.epicId());
                assertEquals(4, intent.doneWhen().size());
                assertEquals(3, intent.decidedCount());
                assertEquals(1, intent.pendingChecks().size());
                assertFalse(intent.allMet());
                assertEquals(2, intent.nonGoals().size());
                assertEquals(2, intent.notYetSpecified().size());
                assertEquals(2, intent.plans().size());

                var met = intent.doneWhen().getFirst();
                assertEquals(DoneWhenCheck.Verdict.MET, met.verdict());
                assertTrue(met.met());
                assertFalse(met.evidence().isEmpty());
                assertEquals(DoneWhenCheck.Verdict.NOT_MET, intent.doneWhen().get(2).verdict());

                var plan = intent.plan("fix-relay");
                assertNotNull(plan);
                assertEquals(2, plan.doneWhen().size());
                assertNull(intent.plan("no-such-plan"));
            }
        }
    }

    @Nested
    @DisplayName("The over-size distribution")
    class OverSize {

        @Test
        @DisplayName("serves the token buckets and the share the proposed multiple would flag")
        void servesDistribution() throws Exception {
            try (var runtime = MockRuntime.start(STATE)) {
                var response = get(runtime, "/api/v1/projects/pm-mcp-clients/metrics/oversize");
                assertEquals(200, response.status(), response.bodyText());

                var distribution = OverSizeDistributionResource.fromJson(response.bodyText());
                assertEquals("pm-mcp-clients", distribution.projectId());
                assertEquals("implement", distribution.role());
                assertEquals(3.0, distribution.currentMultiple());
                assertEquals(2.0, distribution.proposedMultiple());
                assertEquals(4, distribution.buckets().size());
                assertEquals(20, distribution.totalTasks());
                assertEquals(7, distribution.flaggedTasks());
                assertEquals(0.35, distribution.flaggedShare(), 0.0001);
                assertEquals(2, distribution.flaggedBuckets().size());
                assertTrue(distribution.flagsMore());

                var another = get(runtime, "/api/v1/projects/another-project/metrics/oversize");
                assertEquals(200, another.status());
                assertEquals("another-project",
                        OverSizeDistributionResource.fromJson(another.bodyText()).projectId());
            }
        }
    }

    @Test
    @DisplayName("every intent fixture is the contract's shape before it is served")
    void fixturesAreContractShaped() {
        var scenario = ScenarioLoader.load(StandardScenarios.RUNNING_WORK);
        var planIntent = scenario.route("GET", "/api/v1/plans/fix-relay/intent");
        var outline = scenario.route("GET", "/api/v1/plans/fix-relay/outline");
        var epicIntent = scenario.route("GET", "/api/v1/epics/relay-hardening/intent");
        var oversize = scenario.route("GET", "/api/v1/projects/pm-mcp-clients/metrics/oversize");

        assertAll(
                () -> assertNotNull(planIntent),
                () -> assertNotNull(outline),
                () -> assertNotNull(epicIntent),
                () -> assertNotNull(oversize),
                () -> assertNotNull(PlanIntentResource.fromMap(
                        JsonTree.asObject(planIntent.representation()))),
                () -> assertNotNull(OutlineResource.fromMap(
                        JsonTree.asObject(outline.representation()))),
                () -> assertNotNull(EpicIntentResource.fromMap(
                        JsonTree.asObject(epicIntent.representation()))),
                () -> assertNotNull(OverSizeDistributionResource.fromMap(
                        JsonTree.asObject(oversize.representation()))));
    }

    @Test
    @DisplayName("every state of the consolidated scenario decodes as an operator state")
    void everyStateDecodes() throws Exception {
        var scenario = ScenarioLoader.load(StandardScenarios.RUNNING_WORK);
        assertEquals(40, scenario.states().size());
        for (var entry : scenario.states().entrySet()) {
            var representation = JsonTree.asObject(entry.getValue().representation());
            assertNotNull(representation, entry.getKey());
            assertEquals(entry.getKey(),
                    JsonTree.string(JsonTree.object(representation, "state"), "state_id"), entry.getKey());
            assertNotNull(OperatorStateResource.fromMap(representation),
                    entry.getKey());
        }
        assertTrue(scenario.states().keySet().containsAll(List.of(
                "overview.running", "overview.empty", "overview.setup-only", "overview.plan-ending",
                "process.running", "process.loopback", "process.nested-3-deep", "process.pruned-lane",
                "process.new-version", "process.failed-tool", "process.off-track",
                "process.tokens-partial", "process.tokens-none", "process.tokens-not-measured",
                "intent.ready", "decisions.open", "decisions.answered-elsewhere", "decisions.withdrawn",
                "control.ready", "control.pause", "control.holds", "control.settings", "control.epic",
                "control.decomposition", "control.chat", "control.review", "control.tokens")));
    }

    private static HttpResponse get(MockRuntime runtime, String path) throws Exception {
        return new UnixSocketHttpClient(runtime.socketPath())
                .send(HttpRequest.get(path).withHeader(AUTH_HEADER, "Bearer " + runtime.token()));
    }
}
