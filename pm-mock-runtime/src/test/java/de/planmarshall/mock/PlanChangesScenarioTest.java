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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
        var body = JsonTree.write(Map.of("event", PlanChangeCapturedEvent.EVENT_NAME,
                "data", event.toMap()));
        return new UnixSocketHttpClient(runtime.socketPath())
                .send(HttpRequest.json("POST", "/mock/broadcast", body));
    }

    private static String scenarioJson() throws IOException {
        try (var in = PlanChangesScenarioTest.class.getClassLoader().getResourceAsStream(SCENARIO_RESOURCE)) {
            assertTrue(in != null, "the running-work scenario is on the classpath");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
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
        @DisplayName("answers its hunks with every line marked")
        void answersTheHunks() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
                var file = readFile(runtime, FIX_RELAY, PLAN_CHANGES_FILE);

                assertAll(
                        () -> assertEquals(PLAN_CHANGES_FILE, file.path()),
                        () -> assertFalse(file.binary()),
                        () -> assertFalse(file.truncated()),
                        () -> assertEquals(1, file.hunks().size()),
                        () -> assertEquals(1, file.hunks().getFirst().newStart()),
                        () -> assertEquals(List.of(PlanChangeLineKind.ADDED, PlanChangeLineKind.CONTEXT,
                                        PlanChangeLineKind.ADDED),
                                file.hunks().getFirst().lines().stream().map(line -> line.kind()).toList()));
            }
        }

        @Test
        @DisplayName("keeps a planted control sequence in a line as text")
        void keepsTheControlSequence() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
                var lines = readFile(runtime, FIX_RELAY, PLAN_CHANGES_FILE).hunks().getFirst().lines();

                assertTrue(lines.get(2).text().startsWith(CONTROL), "the escape is served unchanged");
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
                        new PlanChangeCapturedEvent(FIX_RELAY, newHead, 13, 352, 30, 2));

                assertEquals(200, broadcast.status(), broadcast.bodyText());
                assertEquals(newHead,
                        assertInstanceOf(PlanChangesCaptured.class, readChanges(runtime, FIX_RELAY)).head(),
                        "the plan answers the capture the event announced");
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
    }
}
