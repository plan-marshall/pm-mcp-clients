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


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.planmarshall.api.control.ActionAvailability;
import de.planmarshall.api.control.ArchiveCheck;
import de.planmarshall.api.control.ChatReadBackResource;
import de.planmarshall.api.control.ChatState;
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

@DisplayName("ControlScenario")
@EnabledOnOs({OS.MAC, OS.LINUX})
class ControlScenarioTest {

    private static final String AUTH_HEADER = "Authorization";
    private static final String READY = StandardScenarios.RUNNING_WORK + "#control.ready";

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
                var cancelled = WorkActions.fromJson(post(runtime,
                        "/api/v1/plans/fix-relay/actions/cancel",
                        JsonTree.write(Map.of("reason", "replaced by relay-v2", "close_pr", true)))
                        .bodyText());
                assertEquals("ended", cancelled.state());

                var abandoned = WorkActions.fromJson(post(runtime,
                        "/api/v1/plans/relay-metrics/actions/abandon",
                        JsonTree.write(Map.of("reason", "no longer needed"))).bodyText());
                assertEquals("ended", abandoned.state());

                var heldEnded = post(runtime, "/api/v1/plans/relay-docs/actions/cancel",
                        JsonTree.write(Map.of("reason", "the docs lane was dropped")));
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
        @DisplayName("serves a plan's read-back, an epic's chat, and a read-only chat about ended work")
        void chats() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK + "#control.chat")) {
                var chat = ChatReadBackResource.fromJson(
                        get(runtime, "/api/v1/plans/fix-relay/chat").bodyText());
                assertEquals("replan", chat.role());
                assertEquals("plan", chat.scope());
                assertEquals(ChatState.READ_BACK, chat.chatState());
                assertTrue(chat.hasProposal());
                assertEquals(2, chat.turns().size());

                assertEquals("decompose", ChatReadBackResource.fromJson(
                        get(runtime, "/api/v1/epics/relay-hardening/chat").bodyText()).role());

                var ended = ChatReadBackResource.fromJson(
                        get(runtime, "/api/v1/plans/docs-typo/chat").bodyText());
                assertEquals("archive", ended.scope());
                assertFalse(ended.hasProposal());
            }
        }

        @Test
        @DisplayName("applies a proposal once, refuses a second, and carries the chat on with a turn")
        void proposalAndTurn() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK + "#control.chat")) {
                var accepted = ChatReadBackResource.fromJson(post(runtime,
                        "/api/v1/plans/fix-relay/chat/proposal/accept", "{}").bodyText());
                assertEquals(ChatState.APPLY, accepted.chatState());
                assertFalse(accepted.hasProposal());

                assertEquals(409, post(runtime, "/api/v1/plans/fix-relay/chat/proposal/accept",
                        "{}").status());

                var turned = ChatReadBackResource.fromJson(post(runtime,
                        "/api/v1/plans/fix-relay/chat/turns",
                        JsonTree.write(Map.of("text", "Make the test cover the SSE transport too.")))
                        .bodyText());
                assertEquals(ChatState.TURN, turned.chatState());
                assertEquals(4, turned.turns().size());
                assertTrue(turned.turns().get(2).isOperator());
            }
        }

        @Test
        @DisplayName("rejects a proposal and then has none left to apply")
        void reject() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK + "#control.chat")) {
                var rejected = ChatReadBackResource.fromJson(post(runtime,
                        "/api/v1/plans/fix-relay/chat/proposal/reject", "{}").bodyText());
                assertFalse(rejected.hasProposal());
                assertEquals(ChatState.TURN, rejected.chatState());
                assertEquals(409, post(runtime, "/api/v1/plans/fix-relay/chat/proposal/accept",
                        "{}").status());
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

    @Test
    @DisplayName("answers 404 for an unknown subject")
    void unknownSubject() throws Exception {
        try (var runtime = MockRuntime.start(READY)) {
            for (var path : List.of(
                    "/api/v1/epics/unknown-epic",
                    "/api/v1/epics/unknown-epic/tokens",
                    "/api/v1/epics/unknown-epic/archive-check",
                    "/api/v1/epics/unknown-epic/decomposition",
                    "/api/v1/epics/unknown-epic/review",
                    "/api/v1/plans/unknown-plan/tokens",
                    "/api/v1/plans/unknown-plan/chat")) {
                assertEquals(404, get(runtime, path).status(), path);
            }
            assertEquals(404, post(runtime, "/api/v1/plans/unknown-plan/actions/pause", "{}").status());
            assertEquals(404, post(runtime, "/api/v1/plans/unknown-plan/chat/turns", "{}").status());
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
