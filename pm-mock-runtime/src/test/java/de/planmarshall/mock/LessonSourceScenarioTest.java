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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.source.lesson.LessonClaimResult;
import de.planmarshall.api.source.lesson.LessonPremiseCheckResult;
import de.planmarshall.api.source.lesson.LessonRetirementNotice;
import de.planmarshall.api.source.lesson.PremiseEvidenceKind;
import de.planmarshall.api.source.lesson.UnclaimedLessonSummary;

@DisplayName("LessonSourceScenario")
@EnabledOnOs({OS.MAC, OS.LINUX})
@EnableGeneratorController
class LessonSourceScenarioTest {

    private static final String PROJECT = "pm-mcp-clients";
    private static final String SCENARIO = StandardScenarios.PROJECTS_AND_START;
    private static final String LESSON_SOURCE = SCENARIO + "#sources.lessons";

    @Test
    @DisplayName("lists the unclaimed lessons newest first and leaves claimed and retired ones out")
    void shouldListUnclaimedLessonsNewestFirst() throws Exception {
        try (var runtime = MockRuntime.start(LESSON_SOURCE)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            assertEquals(SCENARIO, runtime.currentScenarioName());
            assertEquals("sources.lessons", runtime.currentStateName());

            var lessons = listLessons(client, auth, PROJECT);
            assertEquals(List.of("les-0007", "les-0009"), lessons.stream()
                    .map(UnclaimedLessonSummary::lessonId).toList());
            assertTrue(lessons.getFirst().recordedAt().isAfter(lessons.getLast().recordedAt()));

            var held = lessons.getFirst();
            assertEquals("Retire lessons when a plan lands", held.title());
            assertEquals("fix-relay", held.citingPlanId());
            assertEquals(2, held.symbolsCited().size());
        }
    }

    @Test
    @DisplayName("reports a holding premise for a lesson whose cited files and symbols exist")
    void shouldReportHoldingPremise() throws Exception {
        try (var runtime = MockRuntime.start(LESSON_SOURCE)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var check = premiseCheck(client, auth, PROJECT, "les-0007");

            assertEquals(LessonPremiseCheckResult.STATUS_HOLDS, check.status());
            assertTrue(check.holds());
            assertTrue(check.evidence().isEmpty());
        }
    }

    @Test
    @DisplayName("reports the missing files and symbols of a broken premise")
    void shouldReportBrokenPremise() throws Exception {
        try (var runtime = MockRuntime.start(LESSON_SOURCE)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var check = premiseCheck(client, auth, PROJECT, "les-0009");

            assertEquals(LessonPremiseCheckResult.STATUS_BROKEN, check.status());
            assertEquals(2, check.evidence().size());
            assertEquals(PremiseEvidenceKind.FILE, check.evidence().get(0).kind());
            assertEquals("src/main/java/de/planmarshall/store/OldStore.java", check.evidence().get(0).name());
            assertEquals(PremiseEvidenceKind.SYMBOL, check.evidence().get(1).kind());
            assertEquals("a1b2c3d", check.evidence().get(1).lastVerifiedCommit());
        }
    }

    @Test
    @DisplayName("claims a lesson with a holding premise and broadcasts source.lesson_claimed")
    void shouldClaimLesson() throws Exception {
        try (var runtime = MockRuntime.start(LESSON_SOURCE)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            try (var exchange = client.open(HttpRequest.get("/api/v1/events").withHeader("Authorization", auth))) {
                assertTrue(exchange.isEventStream());
                var sse = exchange.events();

                var planId = "plan-" + Generators.letterStrings(4, 10).next().toLowerCase();
                var result = claim(client, auth, PROJECT, "les-0007", false, planId);
                assertEquals(LessonClaimResult.STATUS_CLAIMED, result.status());
                assertEquals(planId, result.planId());

                var event = sse.next();
                assertNotNull(event);
                assertEquals("source.lesson_claimed", event.event());
                var data = JsonTree.asObject(JsonTree.parse(event.data()));
                assertEquals("les-0007", JsonTree.string(data, "lesson_id"));
                assertEquals(planId, JsonTree.string(data, "plan_id"));
            }

            var remaining = listLessons(client, auth, PROJECT);
            assertFalse(remaining.stream().anyMatch(lesson -> "les-0007".equals(lesson.lessonId())));
        }
    }

    @Test
    @DisplayName("refuses a claim of a lesson whose broken premise was not overridden")
    void shouldRefuseClaimOfBrokenPremise() throws Exception {
        try (var runtime = MockRuntime.start(LESSON_SOURCE)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var res = client.send(HttpRequest.json("POST",
                    "/api/v1/projects/" + PROJECT + "/sources/lessons/les-0009/claim", "{}")
                    .withHeader("Authorization", auth));

            assertEquals(409, res.status());
            var body = JsonTree.asObject(JsonTree.parse(res.bodyText()));
            assertEquals("lesson_claim_refused", JsonTree.string(body, "error"));
            assertEquals("lesson_premise_broken", JsonTree.string(body, "reason"));
        }
    }

    @Test
    @DisplayName("claims a lesson with a broken premise when the operator uses it anyway")
    void shouldClaimBrokenPremiseWithOverride() throws Exception {
        try (var runtime = MockRuntime.start(LESSON_SOURCE)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var planId = "plan-" + Generators.letterStrings(4, 10).next().toLowerCase();
            var result = claim(client, auth, PROJECT, "les-0009", true, planId);

            assertEquals(LessonClaimResult.STATUS_CLAIMED, result.status());
            assertEquals(planId, result.planId());
        }
    }

    @Test
    @DisplayName("refuses a claim of a lesson another plan already owns")
    void shouldRefuseClaimOfOwnedLesson() throws Exception {
        try (var runtime = MockRuntime.start(LESSON_SOURCE)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var res = client.send(HttpRequest.json("POST",
                    "/api/v1/projects/" + PROJECT + "/sources/lessons/les-0011/claim", "{}")
                    .withHeader("Authorization", auth));

            assertEquals(409, res.status());
            var body = JsonTree.asObject(JsonTree.parse(res.bodyText()));
            assertEquals("lesson_claimed_elsewhere", JsonTree.string(body, "reason"));
        }
    }

    @Test
    @DisplayName("retires a lesson, broadcasts source.lesson_retired, and never offers it again")
    void shouldRetireLesson() throws Exception {
        try (var runtime = MockRuntime.start(LESSON_SOURCE)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            try (var exchange = client.open(HttpRequest.get("/api/v1/events").withHeader("Authorization", auth))) {
                var sse = exchange.events();

                var retireReason = Generators.letterStrings(4, 12).next();
                var res = client.send(HttpRequest.json("POST",
                        "/api/v1/projects/" + PROJECT + "/sources/lessons/les-0009/retire",
                        JsonTree.write(Map.of("reason", retireReason))).withHeader("Authorization", auth));
                assertEquals(200, res.status());

                var notice = LessonRetirementNotice.fromJson(res.bodyText());
                assertTrue(notice.retired());
                assertEquals(retireReason, notice.reason());

                var event = sse.next();
                assertNotNull(event);
                assertEquals("source.lesson_retired", event.event());
            }

            var remaining = listLessons(client, auth, PROJECT);
            assertFalse(remaining.stream().anyMatch(lesson -> "les-0009".equals(lesson.lessonId())));
        }
    }

    @Test
    @DisplayName("answers 404 for a premise check of a lesson that does not exist")
    void shouldAnswerNotFoundForUnknownLesson() throws Exception {
        try (var runtime = MockRuntime.start(LESSON_SOURCE)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var unknownLesson = "les-" + Generators.integers(1000, 9999).next();
            var res = client.send(HttpRequest.json("POST",
                    "/api/v1/projects/" + PROJECT + "/sources/lessons/" + unknownLesson + "/premise-check", "{}")
                    .withHeader("Authorization", auth));

            assertEquals(404, res.status());
            var body = JsonTree.asObject(JsonTree.parse(res.bodyText()));
            assertEquals("lesson_not_found", JsonTree.string(body, "error"));
        }
    }

    @Test
    @DisplayName("refuses a lesson of a project that is not ready")
    void shouldRefuseNotReadyProject() throws Exception {
        try (var runtime = MockRuntime.start(LESSON_SOURCE)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var res = client.send(HttpRequest.get("/api/v1/projects/plan-marshall/sources/lessons")
                    .withHeader("Authorization", auth));

            assertEquals(409, res.status());
            var body = JsonTree.asObject(JsonTree.parse(res.bodyText()));
            assertEquals("project_not_ready", JsonTree.string(body, "reason"));
        }
    }

    private static List<UnclaimedLessonSummary> listLessons(UnixSocketHttpClient client, String auth,
            String project) throws IOException {
        var res = client.send(HttpRequest.get("/api/v1/projects/" + project + "/sources/lessons")
                .withHeader("Authorization", auth));
        assertEquals(200, res.status());
        var body = JsonTree.asObject(JsonTree.parse(res.bodyText()));
        var raw = JsonTree.asArray(body.get("lessons"));
        return raw.stream().map(item -> {
            try {
                return UnclaimedLessonSummary.fromMap(JsonTree.asObject(item));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }).toList();
    }

    private static LessonPremiseCheckResult premiseCheck(UnixSocketHttpClient client, String auth, String project,
            String lessonId) throws IOException {
        var res = client.send(HttpRequest.json("POST",
                "/api/v1/projects/" + project + "/sources/lessons/" + lessonId + "/premise-check", "{}")
                .withHeader("Authorization", auth));
        assertEquals(200, res.status());
        return LessonPremiseCheckResult.fromJson(res.bodyText());
    }

    private static LessonClaimResult claim(UnixSocketHttpClient client, String auth, String project,
            String lessonId, boolean useAnyway, String planId) throws IOException {
        var body = new LinkedHashMap<String, Object>();
        body.put("use_anyway", useAnyway);
        body.put("plan_id", planId);
        var res = client.send(HttpRequest.json("POST",
                "/api/v1/projects/" + project + "/sources/lessons/" + lessonId + "/claim",
                JsonTree.write(body)).withHeader("Authorization", auth));
        assertEquals(200, res.status());
        return LessonClaimResult.fromJson(res.bodyText());
    }
}
