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

import java.util.List;
import java.util.Map;
import java.util.Objects;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.chat.ChatResource;
import de.planmarshall.api.chat.ChatScopeKind;
import de.planmarshall.api.chat.ChatStatus;
import de.planmarshall.api.chat.ChatTurnAccepted;
import de.planmarshall.api.chat.ChatTurnRequest;
import de.planmarshall.api.chat.ProposalAnswerRequest;
import de.planmarshall.api.chat.ProposalState;
import de.planmarshall.api.control.ActionAvailability;
import de.planmarshall.api.control.ArchiveCheck;
import de.planmarshall.api.control.EpicResource;
import de.planmarshall.api.control.EpicWaitReason;
import de.planmarshall.api.control.HoldRecord;
import de.planmarshall.api.control.PlanSettingsRecord;
import de.planmarshall.api.control.PlanSettingsUpdateResult;
import de.planmarshall.api.control.ReviewDisposition;
import de.planmarshall.api.control.ReviewResource;
import de.planmarshall.api.control.TokenFigure;
import de.planmarshall.api.control.WorkActions;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.HttpResponse;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.resource.ApiErrorCode;
import de.planmarshall.mock.runtime.MockTiming;

@DisplayName("ControlScenario")
@EnabledOnOs({OS.MAC, OS.LINUX})
@EnableGeneratorController
class ControlScenarioTest {

    private static final String AUTH_HEADER = "Authorization";
    private static final String READY = StandardScenarios.RUNNING_WORK + "#control.ready";
    /** The answer naming the open proposal of the plan fix-relay in the state control.chat. */
    private static final String PROPOSAL_ANSWER = new ProposalAnswerRequest("T-0003").toJson();

    @Nested
    @DisplayName("The actions of a plan")
    class PlanActions {

        @Test
        @DisplayName("serves the running, waiting, held, paused, and ended states")
        void everyPlanState() throws Exception {
            try (var runtime = MockRuntime.start(READY)) {
                assertEquals(StandardScenarios.RUNNING_WORK, runtime.currentScenarioName());
                assertEquals("control.ready", runtime.currentStateName());

                var running = actions(runtime, "fix-relay");
                assertEquals("running", running.state());
                assertEquals("relay-hardening", running.epicId());
                assertTrue(running.action("pause").available());
                assertTrue(running.action("cancel").available());
                assertNull(running.action("abandon"));

                assertEquals("waiting", actions(runtime, "drop-legacy-api").state());

                var held = actions(runtime, "relay-docs");
                assertEquals("held", held.state());
                assertEquals("held", held.holdKind());
                assertTrue(held.action("cancel").available());

                var paused = actions(runtime, "relay-metrics");
                assertEquals("paused", paused.state());
                assertTrue(paused.action("start").available());
                assertTrue(paused.action("abandon").available());
                assertFalse(paused.action("chat").available());
                assertEquals("no configuration of replan qualifies yet", paused.action("chat").reason());

                var ended = actions(runtime, "docs-typo");
                assertEquals("ended", ended.state());
                assertEquals(List.of("chat"), ended.actions().stream()
                        .map(ActionAvailability::action).toList());
            }
        }

        @Test
        @DisplayName("pauses at now, after the task, and after the phase, and withdraws the pause")
        void pausePointsAndWithdrawal() throws Exception {
            for (var point : List.of("now", "task", "phase")) {
                try (var runtime = MockRuntime.start(READY)) {
                    var paused = WorkActions.fromJson(
                            post(runtime, "/api/v1/plans/fix-relay/actions/pause",
                                    JsonTree.write(Map.of("point", point))).bodyText());
                    assertEquals("pausing", paused.state(), point);
                    assertEquals(point, paused.pausingPoint(), point);

                    var resumed = WorkActions.fromJson(
                            post(runtime, "/api/v1/plans/fix-relay/actions/start", "{}").bodyText());
                    assertEquals("running", resumed.state(), point);
                    assertNull(resumed.pausingPoint(), point);
                }
            }

            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK
                         + "#control.pause-withdrawn")) {
                assertEquals("running", actions(runtime, "fix-relay").state());
            }
        }

        @Test
        @DisplayName("cancels a plan with its reason, abandons a paused one, and ends a held one")
        void ending() throws Exception {
            try (var runtime = MockRuntime.start(READY)) {
                var cancelReason = Generators.letterStrings(4, 12).next();
                var abandonReason = Generators.letterStrings(4, 12).next();
                var heldReason = Generators.letterStrings(4, 12).next();
                var cancelled = WorkActions.fromJson(post(runtime,
                        "/api/v1/plans/fix-relay/actions/cancel",
                        JsonTree.write(Map.of("reason", cancelReason, "close_pr", true)))
                        .bodyText());
                assertEquals("ended", cancelled.state());

                var abandoned = WorkActions.fromJson(post(runtime,
                        "/api/v1/plans/relay-metrics/actions/abandon",
                        JsonTree.write(Map.of("reason", abandonReason))).bodyText());
                assertEquals("ended", abandoned.state());

                var heldEnded = post(runtime, "/api/v1/plans/relay-docs/actions/cancel",
                        JsonTree.write(Map.of("reason", heldReason)));
                assertEquals(200, heldEnded.status());
                assertTrue(holds(runtime, "relay-docs").isEmpty());
            }
        }
    }

    @Nested
    @DisplayName("The holds")
    class Holds {

        @Test
        @DisplayName("shows an integrity pause with its file and digests, still held after a retry")
        void integrityPause() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK + "#control.holds")) {
                var holds = holds(runtime, "relay-cursor-order");
                assertEquals(1, holds.size());
                var hold = HoldRecord.fromMap(JsonTree.asObject(holds.getFirst()));
                assertEquals("integrity_pause", hold.kind());
                assertEquals("references.json", hold.filePath());
                assertEquals(4812L, hold.recordedSize());
                assertEquals(5034L, hold.currentSize());
                assertTrue(hold.offers("accept"));
                assertTrue(hold.offers("abort"));

                var retry = resolve(runtime, "relay-cursor-order", "H-0001", "retry");
                assertEquals(Boolean.FALSE, retry.get("released"));
                assertNotNull(retry.get("cause"));
                assertEquals("held", actions(runtime, "relay-cursor-order").state());

                var accept = resolve(runtime, "relay-cursor-order", "H-0001", "accept");
                assertEquals(Boolean.TRUE, accept.get("released"));
                assertEquals("running", actions(runtime, "relay-cursor-order").state());
            }
        }

        @Test
        @DisplayName("aborts a plan from its integrity pause and ends it")
        void abortFromIntegrityPause() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK + "#control.holds")) {
                var abort = resolve(runtime, "relay-cursor-order", "H-0001", "abort");
                assertEquals(Boolean.TRUE, abort.get("released"));
                assertEquals("ended", actions(runtime, "relay-cursor-order").state());
            }
        }

        @Test
        @DisplayName("releases a plan held for an overlap and leaves one an act does not resolve held")
        void overlapHold() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK + "#control.holds")) {
                var hold = holds(runtime, "relay-docs").getFirst();
                var record = HoldRecord.fromMap(JsonTree.asObject(hold));
                assertEquals("held", record.kind());
                assertEquals(List.of("retry", "end_plan"), record.acts());

                var retry = resolve(runtime, "relay-docs", "H-0002", "retry");
                assertEquals(Boolean.TRUE, retry.get("released"));
                assertEquals("running", actions(runtime, "relay-docs").state());
            }
        }
    }

    @Nested
    @DisplayName("A plan's settings")
    class Settings {

        @Test
        @DisplayName("guards the revision, applies a change, and refuses a value")
        void revisionGuard() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK + "#control.settings")) {
                var settings = PlanSettingsRecord.fromJson(
                        get(runtime, "/api/v1/plans/fix-relay/settings").bodyText());
                assertEquals(7, settings.revision());
                assertEquals("project", settings.value("landing").origin());
                assertEquals("plan", settings.value("asking").origin());

                var stale = put(runtime, "/api/v1/plans/fix-relay/settings",
                        JsonTree.write(Map.of("aspect", "method", "value", "method.tests-after",
                                "revision", 6)));
                assertEquals(409, stale.status());
                var conflict = PlanSettingsUpdateResult.fromJson(stale.bodyText());
                assertTrue(conflict.conflict());
                assertEquals("method.test-first", conflict.currentValue());

                var applied = PlanSettingsUpdateResult.fromJson(put(runtime,
                        "/api/v1/plans/fix-relay/settings",
                        JsonTree.write(Map.of("aspect", "method", "value", "method.tests-after",
                                "revision", 7))).bodyText());
                assertTrue(applied.applied());
                assertEquals(8, applied.revision());

                var refused = put(runtime, "/api/v1/plans/fix-relay/settings",
                        JsonTree.write(Map.of("aspect", "landing", "value", "landing.main",
                                "revision", 8)));
                assertEquals(400, refused.status());
                assertEquals("refused", JsonTree.string(JsonTree.asObject(JsonTree.parse(refused.bodyText())),
                        "error"));

                var tooLate = put(runtime, "/api/v1/plans/docs-typo/settings",
                        JsonTree.write(Map.of("aspect", "method", "value", "method.tests-after",
                                "revision", 1)));
                assertEquals(409, tooLate.status());
                var late = PlanSettingsUpdateResult.fromJson(tooLate.bodyText());
                assertFalse(late.applied());
                assertEquals("ended", late.restoreState());
            }
        }
    }

    @Nested
    @DisplayName("The epic")
    class Epic {

        @Test
        @DisplayName("serves the resource with its rows, waits, and archive check")
        void resource() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK + "#control.epic")) {
                var epic = EpicResource.fromJson(get(runtime, "/api/v1/epics/relay-hardening").bodyText());
                assertEquals("relay-hardening", epic.epicId());
                assertEquals("pm-mcp-clients", epic.projectId());
                assertEquals("running", epic.state());
                assertTrue(epic.autoStart());
                assertEquals(3, epic.scope());
                assertEquals(2, epic.workstreams().size());

                var auth = epic.workstreams().get(1);
                assertEquals(EpicWaitReason.DEPENDENCY, auth.rows().get(1).waits());
                assertEquals("relay-auth-v2", auth.rows().get(1).waitPlanId());
                var docs = auth.rows().get(2);
                assertEquals(EpicWaitReason.OVERLAP, docs.waits());
                assertEquals(1, docs.overlapFiles().size());

                var check = ArchiveCheck.fromJson(
                        get(runtime, "/api/v1/epics/relay-hardening/archive-check").bodyText());
                assertFalse(check.passed());
                assertEquals(3, check.carriedFindings());
                assertEquals(5, check.carriedWatches());
                assertEquals("review-findings", check.lines().get(2).action());
            }
        }

        @Test
        @DisplayName("disables archive while the ledger lands and switches automatic to a named value")
        void actionsAndAuto() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK + "#control.epic")) {
                var landing = WorkActions.fromJson(get(runtime,
                        "/api/v1/epics/relay-docs-epic/actions").bodyText());
                assertEquals("landing", landing.state());
                assertFalse(landing.action("archive").available());
                assertEquals("the ledger is landing", landing.action("archive").reason());

                var autoOff = post(runtime, "/api/v1/epics/relay-hardening/actions/auto",
                        JsonTree.write(Map.of("mode", "off")));
                assertEquals(Boolean.FALSE, JsonTree.asObject(JsonTree.parse(autoOff.bodyText()))
                        .get("auto_enabled"));
                var autoOn = post(runtime, "/api/v1/epics/relay-hardening/actions/auto",
                        JsonTree.write(Map.of("mode", "on")));
                assertEquals(Boolean.TRUE, JsonTree.asObject(JsonTree.parse(autoOn.bodyText()))
                        .get("auto_enabled"));
            }
        }

        @Test
        @DisplayName("serves a manual epic whose completion review runs first")
        void manualEpicReviewFirst() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK + "#control.archive")) {
                var check = ArchiveCheck.fromJson(
                        get(runtime, "/api/v1/epics/relay-docs-epic/archive-check").bodyText());
                assertTrue(check.completionReviewFirst());
            }
        }

        @Test
        @DisplayName("serves tokens as a floor per plan, and not measured at all")
        void tokens() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK + "#control.tokens")) {
                var tokens = JsonTree.asObject(JsonTree.parse(
                        get(runtime, "/api/v1/epics/relay-hardening/tokens").body()));
                assertEquals(Boolean.FALSE, tokens.get("not_measured"));
                var figure = TokenFigure.fromMap(JsonTree.object(tokens, "total"));
                assertEquals(3_860_000L, figure.total());
                assertTrue(figure.floor());
                assertEquals(1, figure.notMeasuredTasks());

                assertEquals(1_400_000L, TokenFigure.fromJson(
                        get(runtime, "/api/v1/plans/relay-reconnect-backoff/tokens").bodyText()).total());

                var unmeasured = JsonTree.asObject(JsonTree.parse(
                        get(runtime, "/api/v1/epics/relay-docs-epic/tokens").body()));
                assertEquals(Boolean.TRUE, unmeasured.get("not_measured"));
                assertFalse(TokenFigure.fromMap(JsonTree.object(unmeasured, "total")).measured());
            }
        }
    }

    @Nested
    @DisplayName("The chat")
    class Chat {

        @Test
        @DisplayName("serves a plan's read-back, an epic's chat, and a chat about ended work")
        void chats() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK + "#control.chat")) {
                var chat = ChatResource.fromJson(get(runtime, "/api/v1/plans/fix-relay/chat").bodyText());
                assertEquals("replan", chat.role().name());
                assertEquals(ChatScopeKind.PLAN, chat.scope());
                assertEquals(ChatStatus.READ_BACK, chat.state());
                assertTrue(chat.hasProposal());
                assertEquals(3, chat.turns().size());

                assertEquals("decompose", ChatResource.fromJson(
                        get(runtime, "/api/v1/epics/relay-hardening/chat").bodyText()).role().name());

                var ended = ChatResource.fromJson(get(runtime, "/api/v1/plans/docs-typo/chat").bodyText());
                assertEquals(ChatScopeKind.ARCHIVE, ended.scope());
                assertFalse(ended.hasProposal());
            }
        }

        @Test
        @DisplayName("accepts a proposal once, refuses a second, and carries the chat on with a turn")
        void proposalAndTurn() throws Exception {
            // Ordered timing plays the reply to its end before the message's 202 arrives.
            try (var runtime = MockRuntime.start(MockRuntimeConfig.builder()
                         .scenario(StandardScenarios.RUNNING_WORK + "#control.chat").timing(MockTiming.ORDERED).build())) {
                var accepted = ChatResource.fromJson(post(runtime,
                        "/api/v1/plans/fix-relay/chat/proposal/accept", PROPOSAL_ANSWER).bodyText());
                assertEquals(ChatStatus.TURN, accepted.state());
                assertFalse(accepted.hasProposal());
                assertEquals(ProposalState.ACCEPTED, Objects.requireNonNull(accepted.proposal()).state());

                assertEquals(409, post(runtime, "/api/v1/plans/fix-relay/chat/proposal/accept",
                        PROPOSAL_ANSWER).status());

                var message = new ChatTurnRequest(Generators.letterStrings(4, 12).next(),
                        Generators.letterStrings(8, 12).next());
                var turned = post(runtime, "/api/v1/plans/fix-relay/chat/turns", message.toJson());
                assertEquals(ChatTurnAccepted.HTTP_STATUS, turned.status());
                var operatorTurnId = ChatTurnAccepted.fromJson(turned.bodyText()).turnId();

                // The role's second reply of the scripted conversation makes a new proposal, a turn of its own.
                var chat = ChatResource.fromJson(get(runtime, "/api/v1/plans/fix-relay/chat").bodyText());
                assertEquals(ChatStatus.READ_BACK, chat.state());
                assertEquals(6, chat.turns().size());
                var operatorTurn = chat.turns().get(3);
                assertEquals(operatorTurnId, operatorTurn.turnId());
                assertTrue(operatorTurn.isOperator());
                assertEquals(message.text(), operatorTurn.text());
            }
        }

        @Test
        @DisplayName("rejects a proposal and then has none left to accept")
        void reject() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK + "#control.chat")) {
                var rejected = ChatResource.fromJson(post(runtime,
                        "/api/v1/plans/fix-relay/chat/proposal/reject", PROPOSAL_ANSWER).bodyText());
                assertFalse(rejected.hasProposal());
                assertEquals(ProposalState.REJECTED, Objects.requireNonNull(rejected.proposal()).state());
                assertEquals(ChatStatus.TURN, rejected.state());
                assertEquals(409, post(runtime, "/api/v1/plans/fix-relay/chat/proposal/accept",
                        PROPOSAL_ANSWER).status());
            }
        }
    }

    @Nested
    @DisplayName("The review")
    class Review {

        @Test
        @DisplayName("serves every disposition and marks an item from outside the machine untrusted")
        void dispositions() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK + "#control.review")) {
                var review = ReviewResource.fromJson(get(runtime,
                        "/api/v1/epics/relay-hardening/review?register=findings").bodyText());
                assertEquals("findings", review.register());
                assertEquals(2, review.decided());
                assertEquals(3, review.total());
                assertEquals(1, review.openItems().size());
                assertEquals(ReviewDisposition.OPEN, review.item("F-0004").disposition());
                assertTrue(review.item("F-0005").isUntrusted());
                assertEquals(ReviewDisposition.DROPPED, review.item("F-0006").disposition());

                var watches = ReviewResource.fromJson(get(runtime,
                        "/api/v1/epics/relay-hardening/review?register=watches").bodyText());
                assertEquals("watches", watches.register());
                assertEquals(2, watches.items().size());
            }
        }

        @Test
        @DisplayName("defaults to the findings register and honours a later query parameter")
        void defaults() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK + "#control.review")) {
                assertEquals("findings", ReviewResource.fromJson(
                        get(runtime, "/api/v1/epics/relay-hardening/review").bodyText()).register());
                assertEquals("watches", ReviewResource.fromJson(get(runtime,
                        "/api/v1/epics/relay-hardening/review?limit=1&register=watches").bodyText())
                        .register());
            }
        }
    }

    @Nested
    @DisplayName("An act the state does not offer")
    class ActNotOffered {

        @Test
        @DisplayName("is refused act_not_available for a plan, whether absent or disabled")
        void planActs() throws Exception {
            try (var runtime = MockRuntime.start(READY)) {
                // An ended plan offers chat only; a pause is not among its acts.
                var absent = post(runtime, "/api/v1/plans/docs-typo/actions/pause", "{}");
                assertActNotAvailable(absent);
                assertEquals("ended", actions(runtime, "docs-typo").state());

                // A paused plan lists chat but disables it, with the reason it names.
                var disabled = post(runtime, "/api/v1/plans/relay-metrics/actions/chat", "{}");
                assertActNotAvailable(disabled);
                assertEquals("no configuration of replan qualifies yet",
                        JsonTree.string(JsonTree.asObject(JsonTree.parse(disabled.body())), "reason"));
                assertEquals("paused", actions(runtime, "relay-metrics").state());
            }
        }

        @Test
        @DisplayName("is refused act_not_available for an ended epic, after its automatic was switched by flag")
        void epicActs() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK + "#control.epic")) {
                var autoOff = post(runtime, "/api/v1/epics/relay-hardening/actions/auto",
                        JsonTree.write(Map.of("enabled", false)));
                assertEquals(200, autoOff.status());
                assertEquals(Boolean.FALSE, JsonTree.asObject(JsonTree.parse(autoOff.body())).get("auto_enabled"));

                var cancelled = post(runtime, "/api/v1/epics/relay-hardening/actions/cancel", "{}");
                assertEquals(200, cancelled.status());
                assertEquals("ended", WorkActions.fromJson(get(runtime,
                        "/api/v1/epics/relay-hardening/actions").bodyText()).state());

                assertActNotAvailable(post(runtime, "/api/v1/epics/relay-hardening/actions/auto",
                        JsonTree.write(Map.of("enabled", true))));
            }
        }

        @Test
        @DisplayName("is refused act_not_available for a hold, and the hold's own end act ends the plan")
        void holdActs() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK + "#control.holds")) {
                // The overlap hold offers retry and end_plan, never accept.
                var refused = post(runtime, "/api/v1/plans/relay-docs/holds/H-0002/resolve",
                        JsonTree.write(Map.of("act", "accept")));
                assertActNotAvailable(refused);
                assertEquals(1, holds(runtime, "relay-docs").size(), "a refused act leaves the hold");

                var ended = resolve(runtime, "relay-docs", "H-0002", "end_plan");
                assertEquals(Boolean.TRUE, ended.get("released"));
                assertEquals("ended", actions(runtime, "relay-docs").state());
                assertTrue(holds(runtime, "relay-docs").isEmpty(), "ending the plan ends its holds");
            }
        }

        private static void assertActNotAvailable(HttpResponse response) throws Exception {
            assertEquals(409, response.status());
            var code = JsonTree.string(JsonTree.asObject(JsonTree.parse(response.body())), "error");
            assertEquals(ApiErrorCode.ACT_NOT_AVAILABLE, ApiErrorCode.fromWireOrNull(code),
                    "the refusal is a member of the closed catalogue: " + code);
        }
    }

    @Test
    @DisplayName("answers the catalogue's scope or target code for an unknown subject")
    void unknownSubject() throws Exception {
        var unknownEpic = Generators.letterStrings(4, 10).next().toLowerCase();
        var unknownPlan = Generators.letterStrings(4, 10).next().toLowerCase();
        try (var runtime = MockRuntime.start(READY)) {
            var codes = Map.of(
                    "/api/v1/epics/" + unknownEpic, "scope_not_found",
                    "/api/v1/epics/" + unknownEpic + "/tokens", "scope_not_found",
                    "/api/v1/epics/" + unknownEpic + "/archive-check", "scope_not_found",
                    "/api/v1/epics/" + unknownEpic + "/decomposition", "scope_not_found",
                    "/api/v1/epics/" + unknownEpic + "/review", "scope_not_found",
                    "/api/v1/plans/" + unknownPlan + "/tokens", "scope_not_found",
                    "/api/v1/plans/" + unknownPlan + "/chat", "target_not_found");
            for (var entry : codes.entrySet()) {
                var response = get(runtime, entry.getKey());
                assertEquals(404, response.status(), entry.getKey());
                assertEquals(entry.getValue(), JsonTree.string(JsonTree.asObject(JsonTree.parse(response.body())),
                        "error"), entry.getKey());
            }
            assertEquals(404, post(runtime, "/api/v1/plans/" + unknownPlan + "/actions/pause", "{}").status());
            var message = new ChatTurnRequest(Generators.letterStrings(4, 12).next(),
                    Generators.letterStrings(8, 12).next());
            var turn = post(runtime, "/api/v1/plans/" + unknownPlan + "/chat/turns", message.toJson());
            assertEquals(404, turn.status());
            assertEquals("target_not_found",
                    JsonTree.string(JsonTree.asObject(JsonTree.parse(turn.body())), "error"));
            var proposal = post(runtime, "/api/v1/plans/" + unknownPlan + "/chat/proposal/accept",
                    PROPOSAL_ANSWER);
            assertEquals(404, proposal.status());
            assertEquals("target_not_found",
                    JsonTree.string(JsonTree.asObject(JsonTree.parse(proposal.body())), "error"));
            var epicAction = post(runtime, "/api/v1/epics/" + unknownEpic + "/actions/cancel", "{}");
            assertEquals(404, epicAction.status());
            assertEquals("scope_not_found",
                    JsonTree.string(JsonTree.asObject(JsonTree.parse(epicAction.body())), "error"));
        }
    }

    @Test
    @DisplayName("answers target_not_found for a sub-resource a known epic does not carry")
    void knownEpicWithoutSubResource() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
            var review = get(runtime, "/api/v1/epics/relay-hardening/review?register=nothing-of-that-kind");
            assertEquals(404, review.status());
            assertEquals("target_not_found",
                    JsonTree.string(JsonTree.asObject(JsonTree.parse(review.body())), "error"));
        }
    }

    @Test
    @DisplayName("answers target_not_found for an unknown hold of a known plan")
    void unknownHold() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK + "#control.holds")) {
            var unknownHold = "hold-" + Generators.letterStrings(4, 8).next().toLowerCase();
            var response = post(runtime, "/api/v1/plans/relay-cursor-order/holds/" + unknownHold + "/resolve",
                    JsonTree.write(Map.of("act", "accept")));

            assertEquals(404, response.status());
            assertEquals("target_not_found",
                    JsonTree.string(JsonTree.asObject(JsonTree.parse(response.body())), "error"));
        }
    }

    private static WorkActions actions(MockRuntime runtime, String planId) throws Exception {
        return WorkActions.fromJson(get(runtime, "/api/v1/plans/" + planId + "/actions").bodyText());
    }

    private static List<Object> holds(MockRuntime runtime, String planId) throws Exception {
        var holds = JsonTree.asArray(JsonTree.parse(
                get(runtime, "/api/v1/plans/" + planId + "/holds").body()));
        assertNotNull(holds);
        return List.copyOf(holds);
    }

    private static Map<String, Object> resolve(MockRuntime runtime, String planId, String holdId,
            String act) throws Exception {
        var response = post(runtime, "/api/v1/plans/" + planId + "/holds/" + holdId + "/resolve",
                JsonTree.write(Map.of("act", act)));
        assertEquals(200, response.status(), act);
        return JsonTree.asObject(JsonTree.parse(response.body()));
    }

    private static HttpResponse get(MockRuntime runtime, String path) throws Exception {
        return new UnixSocketHttpClient(runtime.socketPath())
                .send(HttpRequest.get(path).withHeader(AUTH_HEADER, "Bearer " + runtime.token()));
    }

    private static HttpResponse post(MockRuntime runtime, String path, String body) throws Exception {
        return new UnixSocketHttpClient(runtime.socketPath())
                .send(HttpRequest.json("POST", path, body).withHeader(AUTH_HEADER, "Bearer " + runtime.token()));
    }

    private static HttpResponse put(MockRuntime runtime, String path, String body) throws Exception {
        return new UnixSocketHttpClient(runtime.socketPath())
                .send(HttpRequest.json("PUT", path, body).withHeader(AUTH_HEADER, "Bearer " + runtime.token()));
    }
}
