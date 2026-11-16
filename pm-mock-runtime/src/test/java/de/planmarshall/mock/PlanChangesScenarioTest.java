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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.changes.PlanChangeCapturedEvent;
import de.planmarshall.api.changes.PlanChangeFile;
import de.planmarshall.api.changes.PlanChangeLineKind;
import de.planmarshall.api.changes.PlanChangesAnswer;
import de.planmarshall.api.changes.PlanChangesCaptured;
import de.planmarshall.api.changes.PlanChangesNotCaptured;
import de.planmarshall.api.changes.PlanChangesRoutes;
import de.planmarshall.api.changes.PlanChangesUnavailable;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.HttpResponse;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.json.JsonNumber;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.overview.ChangeSize;
import de.planmarshall.api.process.ActivityEntry;
import de.planmarshall.api.process.ActivityEntryKind;
import de.planmarshall.api.process.ProcessTree;
import de.planmarshall.api.process.TaskActivity;
import de.planmarshall.mock.scenario.ScenarioLoader;
import de.planmarshall.mock.scenario.ScenarioValidationException;

/**
 * The mock runtime's plan changes: the two routes answer the {@code pm-api} contract from the
 * scenario's fixtures, a broadcast capture is followed by a list read of the new head, and the
 * scenario load holds the list's totals to the plan's size of change.
 */
@DisplayName("PlanChangesScenarioTest")
@EnabledOnOs({OS.MAC, OS.LINUX})
@EnableGeneratorController
class PlanChangesScenarioTest {

    private static final String AUTH_HEADER = "Authorization";
    private static final String SCENARIO_RESOURCE = "scenarios/running-work.json";
    private static final String FIX_RELAY = "fix-relay";

    /** The head the fixture's capture was taken at. */
    private static final String CAPTURED_HEAD = "9f3c2ab41d7e";

    /** The escape the fixture plants in a path and in a line: it stays plain text. */
    private static final String CONTROL = "\u001b[2J";

    private static final String PLAN_CHANGES_FILE =
            "pm-api/src/main/java/de/planmarshall/api/changes/PlanChanges.java";
    private static final String BINARY_PATH = "doc/design/shared/" + CONTROL + "plan-changes.svg";
    private static final String TRUNCATED_FILE =
            "pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java";

    private static HttpResponse get(MockRuntime runtime, String path) throws IOException {
        return new UnixSocketHttpClient(runtime.socketPath())
                .send(HttpRequest.get(path).withHeader(AUTH_HEADER, "Bearer " + runtime.token()));
    }

    private static PlanChangesAnswer readChanges(MockRuntime runtime, String planId) throws IOException {
        var response = get(runtime, PlanChangesRoutes.plan(planId).list());
        assertEquals(200, response.status(), response.bodyText());
        return PlanChangesAnswer.fromMap(JsonTree.asObject(JsonTree.parse(response.body())));
    }

    private static PlanChangeFile readFile(MockRuntime runtime, String planId, String path) throws IOException {
        var response = get(runtime, PlanChangesRoutes.plan(planId).file(path));
        assertEquals(200, response.status(), response.bodyText());
        return PlanChangeFile.fromMap(JsonTree.asObject(JsonTree.parse(response.body())));
    }

    private static HttpResponse broadcast(MockRuntime runtime, PlanChangeCapturedEvent event) throws IOException {
        return broadcastPayload(runtime, PlanChangeCapturedEvent.EVENT_NAME, event.toMap());
    }

    private static HttpResponse broadcastPayload(MockRuntime runtime, String event,
            Map<String, Object> data) throws IOException {
        var body = JsonTree.write(Map.of("event", event, "data", data));
        return new UnixSocketHttpClient(runtime.socketPath())
                .send(HttpRequest.json("POST", "/mock/broadcast", body));
    }

    private static String scenarioJson() throws IOException {
        try (var in = PlanChangesScenarioTest.class.getClassLoader().getResourceAsStream(SCENARIO_RESOURCE)) {
            assertNotNull(in, "the running-work scenario is on the classpath");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * @param representation the project detail's representation
     * @param planId         the plan to find
     * @return the plan's row
     */
    private static Map<String, Object> planOf(Map<String, Object> representation, String planId) {
        var rows = JsonTree.asArray(representation.get("plans"));
        if (rows == null) {
            throw new AssertionError("the project detail lists no plans");
        }
        for (var row : rows) {
            var plan = JsonTree.asObject(row);
            if (plan != null && planId.equals(JsonTree.string(plan, "plan_id"))) {
                return plan;
            }
        }
        throw new AssertionError("no row for " + planId + " in the project detail");
    }

    @Nested
    @DisplayName("The list")
    class TheList {

        @Test
        @DisplayName("answers the capture with every file of the fixture")
        void answersTheCapture() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
                var captured = assertInstanceOf(PlanChangesCaptured.class, readChanges(runtime, FIX_RELAY));

                assertAll(
                        () -> assertEquals(FIX_RELAY, captured.planId()),
                        () -> assertEquals(CAPTURED_HEAD, captured.head()),
                        () -> assertEquals("main", captured.baseRef()),
                        () -> assertEquals(12, captured.files().size()));
            }
        }

        @Test
        @DisplayName("carries a renamed, a binary and the unplanned files")
        void carriesTheSpecialFiles() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
                var captured = assertInstanceOf(PlanChangesCaptured.class, readChanges(runtime, FIX_RELAY));

                var renamed = captured.files().stream()
                        .filter(file -> file.renamedFrom() != null).toList();
                assertEquals(1, renamed.size(), "the fixture renames one file");
                assertTrue(renamed.getFirst().path().endsWith("PlanChangesContractTest.java"));
                assertTrue(renamed.getFirst().renamedFrom().endsWith("overview/PlanChangesContractTest.java"));
                assertEquals(1, captured.files().stream().filter(file -> file.binary()).count(),
                        "the fixture holds one binary file");
                assertEquals(2, captured.files().stream().filter(file -> file.unplanned()).count(),
                        "the fixture holds two changes beyond the plan's footprint");
            }
        }

        @Test
        @DisplayName("its totals agree with the plan's size of change")
        void totalsAgreeWithThePlan() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
                var captured = assertInstanceOf(PlanChangesCaptured.class, readChanges(runtime, FIX_RELAY));
                var process = get(runtime, "/api/v1/plans/" + FIX_RELAY + "/process");
                var tree = ProcessTree.fromMap(JsonTree.asObject(JsonTree.parse(process.body())));

                assertAll(
                        () -> assertEquals(new ChangeSize(12, 340, 27, 2), captured.totals()),
                        () -> assertEquals(tree.change(), captured.totals(),
                                "the list and the plan carry one measurement"));
            }
        }
    }

    @Nested
    @DisplayName("A file")
    class AFile {

        @Test
        @DisplayName("answers its hunks with every line marked, and the counts of each side")
        void answersTheHunks() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
                var file = readFile(runtime, FIX_RELAY, PLAN_CHANGES_FILE);

                assertAll(
                        () -> assertEquals(PLAN_CHANGES_FILE, file.path()),
                        () -> assertFalse(file.binary()),
                        () -> assertFalse(file.truncated()),
                        () -> assertEquals(1, file.hunks().size()),
                        () -> assertEquals(1, file.hunks().getFirst().newStart()),
                        () -> assertEquals("/*", file.hunks().getFirst().lines().getFirst().text()),
                        () -> assertEquals("}", file.hunks().getFirst().lines().getLast().text()),
                        () -> assertEquals(file.hunks().getFirst().lines().size(),
                                file.hunks().getFirst().newLen(),
                                "the hunk's new length is the count of its lines"));
            }
        }

        @Test
        @DisplayName("keeps a planted control sequence in a line as text")
        void keepsTheControlSequence() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
                var lines = readFile(runtime, FIX_RELAY, "pm-web-app/src/lib/plan-changes.ts")
                        .hunks().getFirst().lines();

                assertTrue(lines.getLast().text().contains(CONTROL), "the escape is served unchanged");
            }
        }

        @Test
        @DisplayName("answers a binary file with no lines, also at a path with a control sequence")
        void answersABinaryFile() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
                var file = readFile(runtime, FIX_RELAY, BINARY_PATH);

                assertAll(
                        () -> assertEquals(BINARY_PATH, file.path()),
                        () -> assertTrue(file.binary()),
                        () -> assertTrue(file.hunks().isEmpty(), "a binary file has no lines"));
            }
        }

        @Test
        @DisplayName("answers a cut file with the count of the lines left out")
        void answersACutFile() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
                var file = readFile(runtime, FIX_RELAY, TRUNCATED_FILE);

                assertAll(
                        () -> assertTrue(file.truncated()),
                        () -> assertEquals(120, file.linesOmitted()),
                        () -> assertEquals(1, file.hunks().size()));
            }
        }

        @Test
        @DisplayName("refuses a file read without a path, and one the capture does not hold")
        void refusesAMissingOrUnknownFile() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
                var withoutPath = get(runtime, PlanChangesRoutes.plan(FIX_RELAY).list() + "/file");
                var unknown = get(runtime, PlanChangesRoutes.plan(FIX_RELAY)
                        .file("pm-api/src/main/java/de/planmarshall/api/changes/NotThere.java"));

                assertAll(
                        () -> assertEquals(400, withoutPath.status()),
                        () -> assertEquals("invalid_arguments",
                                JsonTree.string(JsonTree.asObject(JsonTree.parse(withoutPath.body())), "error")),
                        () -> assertEquals(404, unknown.status()),
                        () -> assertEquals("target_not_found",
                                JsonTree.string(JsonTree.asObject(JsonTree.parse(unknown.body())), "error")));
            }
        }

        @Test
        @DisplayName("refuses a file of a plan the scenario does not know, as the list route does")
        void refusesAFileOfAnUnknownPlan() throws Exception {
            var unknown = Generators.letterStrings(6, 12).next().toLowerCase();
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
                var response = get(runtime, PlanChangesRoutes.plan(unknown).file("pm-api/src/PlanChanges.java"));

                assertEquals(404, response.status(), response.bodyText());
                assertEquals("scope_not_found",
                        JsonTree.string(JsonTree.asObject(JsonTree.parse(response.body())), "error"),
                        "an unknown plan is a missing scope on both routes");
            }
        }

        @Test
        @DisplayName("refuses a file of a plan whose changes were never captured, and of a gone capture")
        void refusesAFileOfACaptureThatIsNotThere() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
                var notCaptured = get(runtime, PlanChangesRoutes.plan("drop-legacy-api").file("a/B.java"));
                var unavailable = get(runtime, PlanChangesRoutes.plan("docs-typo").file("a/B.java"));

                assertAll(
                        () -> assertEquals(404, notCaptured.status(), notCaptured.bodyText()),
                        () -> assertEquals("target_not_found",
                                JsonTree.string(JsonTree.asObject(JsonTree.parse(notCaptured.body())), "error")),
                        () -> assertEquals(404, unavailable.status(), unavailable.bodyText()),
                        () -> assertEquals("target_not_found",
                                JsonTree.string(JsonTree.asObject(JsonTree.parse(unavailable.body())), "error")));
            }
        }

        @Test
        @DisplayName("refuses a path that is not validly encoded rather than failing")
        void refusesAMalformedPath() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
                var response = get(runtime, PlanChangesRoutes.plan(FIX_RELAY).list() + "/file?path=%");

                assertEquals(400, response.status(), response.bodyText());
                assertEquals("invalid_arguments",
                        JsonTree.string(JsonTree.asObject(JsonTree.parse(response.body())), "error"));
            }
        }

        @Test
        @DisplayName("refuses a route shape that names no plan, never answering a fault")
        void refusesAPlanlessPath() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
                var list = get(runtime, "/api/v1/plans/changes");
                var file = get(runtime, "/api/v1/plans/changes/file");

                assertAll(
                        () -> assertEquals(404, list.status(), list.bodyText()),
                        () -> assertEquals(404, file.status(), file.bodyText()));
            }
        }

        @Test
        @DisplayName("answers a capture at a percent-encoded plan segment")
        void decodesThePlanSegment() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
                var response = get(runtime, "/api/v1/plans/%66ix-relay/changes");

                assertEquals(200, response.status(), response.bodyText());
                assertEquals(FIX_RELAY,
                        assertInstanceOf(PlanChangesCaptured.class,
                                PlanChangesAnswer.fromMap(JsonTree.asObject(JsonTree.parse(response.body()))))
                                .planId());
            }
        }

        @Test
        @DisplayName("its files stand in path order")
        void filesInPathOrder() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
                var captured = assertInstanceOf(PlanChangesCaptured.class, readChanges(runtime, FIX_RELAY));
                var paths = captured.files().stream().map(file -> file.path()).toList();

                assertEquals(paths.stream().sorted().toList(), paths, "the list is served in path order");
            }
        }

        @Test
        @DisplayName("every non-binary file answers its hunks, so the fixture has contents for each")
        void everyNonBinaryFileHasContents() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
                var captured = assertInstanceOf(PlanChangesCaptured.class, readChanges(runtime, FIX_RELAY));

                for (var file : captured.files()) {
                    if (file.binary()) {
                        continue;
                    }
                    var content = readFile(runtime, FIX_RELAY, file.path());
                    assertEquals(file.path(), content.path());
                    if (content.truncated()) {
                        // The runtime cut this file's answer: its hunks show a part of its lines.
                        continue;
                    }
                    assertEquals(file.added(), addedLines(content),
                            "the file's added lines are the lines its hunks add");
                    assertEquals(file.removed(), removedLines(content),
                            "the file's removed lines are the lines its hunks remove");
                }
            }
        }

        @Test
        @DisplayName("a cut file's hunks are no more than its counts")
        void aCutFilesHunksFitItsCounts() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
                var captured = assertInstanceOf(PlanChangesCaptured.class, readChanges(runtime, FIX_RELAY));
                var cut = captured.files().stream().filter(file -> TRUNCATED_FILE.equals(file.path()))
                        .findFirst().orElseThrow();

                var content = readFile(runtime, FIX_RELAY, TRUNCATED_FILE);
                assertAll(
                        () -> assertTrue(content.truncated(), "the fixture cuts this file"),
                        () -> assertTrue(addedLines(content) <= cut.added()),
                        () -> assertTrue(removedLines(content) <= cut.removed()));
            }
        }

        private static int addedLines(PlanChangeFile file) {
            return file.hunks().stream()
                    .mapToInt(hunk -> (int) hunk.lines().stream()
                            .filter(line -> line.kind() == PlanChangeLineKind.ADDED).count())
                    .sum();
        }

        private static int removedLines(PlanChangeFile file) {
            return file.hunks().stream()
                    .mapToInt(hunk -> (int) hunk.lines().stream()
                            .filter(line -> line.kind() == PlanChangeLineKind.REMOVED).count())
                    .sum();
        }
    }

    @Nested
    @DisplayName("The refusals")
    class TheRefusals {

        @Test
        @DisplayName("answers not_captured before the first capture, never an empty list")
        void notCaptured() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
                var answer = readChanges(runtime, "drop-legacy-api");

                assertEquals("drop-legacy-api", assertInstanceOf(PlanChangesNotCaptured.class, answer).planId());
            }
        }

        @Test
        @DisplayName("answers not_captured for a plan of the scenario the fixture names no capture for")
        void notCapturedWithoutAFixture() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
                var answer = readChanges(runtime, "relay-metrics");

                assertEquals("relay-metrics", assertInstanceOf(PlanChangesNotCaptured.class, answer).planId());
            }
        }

        @Test
        @DisplayName("answers unavailable with its reason for a capture that is gone")
        void unavailable() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
                var answer = readChanges(runtime, "docs-typo");

                var unavailable = assertInstanceOf(PlanChangesUnavailable.class, answer);
                assertEquals("the captured commit 4c1d7e0 is no longer in the repository", unavailable.reason());
            }
        }

        @Test
        @DisplayName("refuses a plan the scenario does not know")
        void unknownPlan() throws Exception {
            var unknown = Generators.letterStrings(6, 12).next().toLowerCase();
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
                var response = get(runtime, PlanChangesRoutes.plan(unknown).list());

                assertEquals(404, response.status());
                assertEquals("scope_not_found",
                        JsonTree.string(JsonTree.asObject(JsonTree.parse(response.body())), "error"));
            }
        }
    }

    @Nested
    @DisplayName("Live")
    class Live {

        @Test
        @DisplayName("a broadcast plan.change_captured is followed by a list read of the new head")
        void broadcastThenRead() throws Exception {
            var newHead = "a1b2c3d4e5f6";
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
                assertEquals(CAPTURED_HEAD,
                        assertInstanceOf(PlanChangesCaptured.class, readChanges(runtime, FIX_RELAY)).head());

                var broadcast = broadcast(runtime,
                        new PlanChangeCapturedEvent(FIX_RELAY, newHead, 12, 340, 27, 2));

                assertEquals(200, broadcast.status(), broadcast.bodyText());
                var captured = assertInstanceOf(PlanChangesCaptured.class, readChanges(runtime, FIX_RELAY));
                assertEquals(newHead, captured.head(), "the plan answers the capture the event announced");
                assertEquals(new ChangeSize(12, 340, 27, 2), captured.totals(),
                        "the files the fixture serves still add up to the announced size");
            }
        }

        @Test
        @DisplayName("refuses an announcement whose size contradicts the files the mock serves")
        void refusesAContradictingCapture() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
                var broadcast = broadcast(runtime,
                        new PlanChangeCapturedEvent(FIX_RELAY, "a1b2c3d4e5f6", 13, 352, 30, 2));

                assertEquals(400, broadcast.status(), broadcast.bodyText());
                assertEquals("invalid_arguments",
                        JsonTree.string(JsonTree.asObject(JsonTree.parse(broadcast.body())), "error"));
                assertEquals(CAPTURED_HEAD,
                        assertInstanceOf(PlanChangesCaptured.class, readChanges(runtime, FIX_RELAY)).head(),
                        "the refused announcement moved nothing");
            }
        }

        @Test
        @DisplayName("refuses a capture announced with a negative size")
        void refusesANegativeSize() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
                var payload = Map.<String, Object>of(
                        "plan_id", FIX_RELAY,
                        "head", "a1b2c3d4e5f6",
                        "files_total", 12,
                        "insertions_total", -340,
                        "deletions_total", 27,
                        "unplanned_total", 2);

                var broadcast = broadcastPayload(runtime, PlanChangeCapturedEvent.EVENT_NAME, payload);

                assertEquals(400, broadcast.status(), broadcast.bodyText());
                assertEquals("invalid_arguments",
                        JsonTree.string(JsonTree.asObject(JsonTree.parse(broadcast.body())), "error"));
            }
        }

        @Test
        @DisplayName("refuses a capture announced for a plan the fixture holds no capture for")
        void refusesACaptureWithoutAFixture() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
                var broadcast = broadcast(runtime,
                        new PlanChangeCapturedEvent("relay-metrics", "a1b2c3d4e5f6", 0, 0, 0, 0));

                assertEquals(400, broadcast.status(), broadcast.bodyText());
                assertInstanceOf(PlanChangesNotCaptured.class, readChanges(runtime, "relay-metrics"));
            }
        }
    }

    @Nested
    @DisplayName("The scenario")
    class TheScenario {

        @Test
        @DisplayName("loads the new fixtures, and holds the worker's activity to the model_differs entry")
        void loadsTheFixtures() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
                var response = get(runtime, "/api/v1/plans/" + FIX_RELAY + "/tasks/TASK-003/activity");
                var activity = TaskActivity.fromMap(JsonTree.asObject(JsonTree.parse(response.body())));
                var differs = activity.entries().stream()
                        .filter(entry -> ActivityEntryKind.MODEL_DIFFERS
                                .equals(ActivityEntryKind.fromValue(entry.kind())))
                        .toList();

                assertEquals(1, differs.size(), "the fixture holds one model_differs entry");
                assertEquals(new ActivityEntry(
                        differs.getFirst().timestamp(), "model_differs", null, null, false, null, null,
                        List.of(), List.of(), "sonnet", "sonnet-5.5"), differs.getFirst());
            }
        }

        @Test
        @DisplayName("refuses a file whose totals contradict the plan's ChangeSize")
        void refusesContradictingTotals() throws Exception {
            var valid = scenarioJson();

            assertDoesNotThrow(() -> ScenarioLoader.load(valid, StandardScenarios.RUNNING_WORK));
            var contradicting = valid.replace("\"insertions\": 340", "\"insertions\": 341");
            assertNotEquals(valid, contradicting, "the fixture carries the size the test contradicts");

            var refused = assertThrows(ScenarioValidationException.class,
                    () -> ScenarioLoader.load(contradicting, StandardScenarios.RUNNING_WORK));
            assertAll(
                    () -> assertTrue(refused.getMessage().contains(FIX_RELAY), refused.getMessage()),
                    () -> assertTrue(refused.getMessage().contains("insertions"), refused.getMessage()));
        }

        @Test
        @DisplayName("refuses a state route override whose size contradicts the capture")
        void refusesAContradictingStateRoute() throws Exception {
            var valid = scenarioJson();
            var root = JsonTree.asObject(JsonTree.parse(valid));
            var runtime = JsonTree.object(JsonTree.object(JsonTree.object(
                    JsonTree.object(JsonTree.object(root, "states"), "running_work.active"),
                    "representation"), "facts"), "runtime");
            var override = new LinkedHashMap<String, Object>();
            override.put("plan_id", FIX_RELAY);
            override.put("change", Map.of("files", 12, "insertions", 341, "deletions", 27, "unplanned", 2));
            var routes = new LinkedHashMap<String, Object>();
            routes.put("GET /api/v1/plans/fix-relay/process", override);
            runtime.put("routes", routes);
            var contradicting = JsonTree.write(root);
            assertNotEquals(valid, contradicting, "the fixture carries the state the test overrides");

            var refused = assertThrows(ScenarioValidationException.class,
                    () -> ScenarioLoader.load(contradicting, StandardScenarios.RUNNING_WORK));
            assertTrue(refused.getMessage().contains(FIX_RELAY), refused.getMessage());
        }

        @Test
        @DisplayName("refuses a scenario whose plan's size of change is no measurement")
        void refusesAnInvalidSize() throws Exception {
            var valid = scenarioJson();
            // Only the project-detail rows: they carry a plan's size but no codec decodes them, so
            // the load's own size collection is the decoder this test exercises.
            var root = JsonTree.asObject(JsonTree.parse(valid));
            var project = JsonTree.object(JsonTree.object(root, "routes"),
                    "GET /api/v1/projects/pm-mcp-clients");
            var change = JsonTree.object(planOf(JsonTree.object(project, "representation"), FIX_RELAY),
                    "change");
            change.put("insertions", new JsonNumber("-340"));
            var negative = JsonTree.write(root);
            assertNotEquals(valid, negative, "the fixture carries the size the test makes invalid");

            var refused = assertThrows(ScenarioValidationException.class,
                    () -> ScenarioLoader.load(negative, StandardScenarios.RUNNING_WORK));
            assertTrue(refused.getMessage().contains("not a measurement"), refused.getMessage());
        }

        @Test
        @DisplayName("refuses a changes fixture in a state the contract does not know")
        void refusesAnUnknownState() throws Exception {
            var valid = scenarioJson();

            var unknown = valid.replace("\"plan_id\": \"docs-typo\",\n                \"state\": \"unavailable\"",
                    "\"plan_id\": \"docs-typo\",\n                \"state\": \"gone\"");
            assertNotEquals(valid, unknown, "the fixture carries the refusal the test renames");

            var refused = assertThrows(ScenarioValidationException.class,
                    () -> ScenarioLoader.load(unknown, StandardScenarios.RUNNING_WORK));
            assertTrue(refused.getMessage().contains("Unknown state of a plan's changes"), refused.getMessage());
        }

        /**
         * @param root the scenario's parsed tree
         * @return the runtime fixture of the start state
         */
        private static Map<String, Object> startRuntime(Map<String, Object> root) {
            return JsonTree.object(JsonTree.object(JsonTree.object(JsonTree.object(
                    JsonTree.object(root, "states"), "running_work.active"), "representation"), "facts"),
                    "runtime");
        }

        /**
         * @param runtime the start state's runtime fixture
         * @return the fixture entry of the plan whose changes the tests contradict
         */
        private static Map<String, Object> fixRelayEntry(Map<String, Object> runtime) {
            for (var row : JsonTree.asArray(runtime.get("plan_changes"))) {
                var entry = JsonTree.asObject(row);
                if (entry != null && FIX_RELAY.equals(JsonTree.string(entry, "plan_id"))) {
                    return entry;
                }
            }
            throw new AssertionError("the fixture names no changes for " + FIX_RELAY);
        }

        private static void refusesTheLoad(String scenario, String expected) {
            var refused = assertThrows(ScenarioValidationException.class,
                    () -> ScenarioLoader.load(scenario, StandardScenarios.RUNNING_WORK));
            assertTrue(refused.getMessage().contains(expected),
                    "the refusal names " + expected + ", was: " + refused.getMessage());
        }

        @Test
        @DisplayName("refuses a file whose counts disagree with its hunks")
        void refusesAFileCountThatDisagreesWithItsHunks() throws Exception {
            var valid = scenarioJson();
            assertDoesNotThrow(() -> ScenarioLoader.load(valid, StandardScenarios.RUNNING_WORK));

            var root = JsonTree.asObject(JsonTree.parse(valid));
            var files = JsonTree.asArray(fixRelayEntry(startRuntime(root)).get("files"));
            JsonTree.asObject(files.get(1)).put("added", 53);

            refusesTheLoad(JsonTree.write(root), "PlanChangeFile.java");
        }

        @Test
        @DisplayName("refuses files that are not in path order")
        void refusesFilesOutOfPathOrder() throws Exception {
            var valid = scenarioJson();
            assertDoesNotThrow(() -> ScenarioLoader.load(valid, StandardScenarios.RUNNING_WORK));

            var root = JsonTree.asObject(JsonTree.parse(valid));
            var files = JsonTree.asArray(fixRelayEntry(startRuntime(root)).get("files"));
            Collections.swap(files, 1, 2);

            refusesTheLoad(JsonTree.write(root), "path order");
        }

        @Test
        @DisplayName("refuses contents at a path the list does not name")
        void refusesContentsAtAnUnknownPath() throws Exception {
            var valid = scenarioJson();
            assertDoesNotThrow(() -> ScenarioLoader.load(valid, StandardScenarios.RUNNING_WORK));

            var root = JsonTree.asObject(JsonTree.parse(valid));
            var contents = JsonTree.asArray(fixRelayEntry(startRuntime(root)).get("contents"));
            for (var row : contents) {
                var content = JsonTree.asObject(row);
                if (content != null && "pm-web-app/src/lib/plan-changes.ts".equals(JsonTree.string(content, "path"))) {
                    content.put("path", "pm-web-app/src/lib/not-there.ts");
                }
            }

            refusesTheLoad(JsonTree.write(root), "not-there.ts");
        }

        @Test
        @DisplayName("refuses a non-binary file without contents")
        void refusesANonBinaryFileWithoutContents() throws Exception {
            var valid = scenarioJson();
            assertDoesNotThrow(() -> ScenarioLoader.load(valid, StandardScenarios.RUNNING_WORK));

            var root = JsonTree.asObject(JsonTree.parse(valid));
            var contents = JsonTree.asArray(fixRelayEntry(startRuntime(root)).get("contents"));
            contents.removeIf(row -> {
                var content = JsonTree.asObject(row);
                return content != null
                        && "pm-web-app/src/lib/plan-changes.ts".equals(JsonTree.string(content, "path"));
            });

            refusesTheLoad(JsonTree.write(root), "plan-changes.ts");
        }

        @Test
        @DisplayName("refuses two captures of one plan")
        void refusesADuplicatePlanId() throws Exception {
            var valid = scenarioJson();
            assertDoesNotThrow(() -> ScenarioLoader.load(valid, StandardScenarios.RUNNING_WORK));

            var root = JsonTree.asObject(JsonTree.parse(valid));
            var runtime = startRuntime(root);
            var entries = JsonTree.asArray(runtime.get("plan_changes"));
            entries.add(fixRelayEntry(runtime));

            refusesTheLoad(JsonTree.write(root), "twice");
        }

        @Test
        @DisplayName("refuses a size of change on a plan whose changes were never captured")
        void refusesASizeOnAPlanWithoutACapture() throws Exception {
            var valid = scenarioJson();
            assertDoesNotThrow(() -> ScenarioLoader.load(valid, StandardScenarios.RUNNING_WORK));

            var root = JsonTree.asObject(JsonTree.parse(valid));
            var runtime = startRuntime(root);
            var override = new LinkedHashMap<String, Object>();
            override.put("plan_id", "drop-legacy-api");
            override.put("change", Map.of("files", 12, "insertions", 340, "deletions", 27, "unplanned", 2));
            var routes = new LinkedHashMap<String, Object>();
            routes.put("GET /api/v1/plans/drop-legacy-api/process", override);
            runtime.put("routes", routes);

            refusesTheLoad(JsonTree.write(root), "drop-legacy-api");
        }
    }
}
