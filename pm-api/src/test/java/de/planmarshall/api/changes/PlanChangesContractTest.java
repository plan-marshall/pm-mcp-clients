/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.changes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.planmarshall.api.event.EventName;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.overview.ChangeSize;
import de.planmarshall.api.process.ActivityEntry;
import de.planmarshall.api.process.ActivityEntryKind;

/**
 * The wire contract of a plan's changes: the list, a file's hunks, and the two refusals, each
 * round-tripped literally, plus the event and the activity kind a capture adds.
 */
@DisplayName("PlanChangesContractTest")
class PlanChangesContractTest {

    /** The escape a worker or the project could plant in a path or a line, as the wire spells it. */
    private static final String CONTROL_JSON = "\\u001B[2J";

    /** The same escape after parsing: the clients show it as plain text. */
    private static final String CONTROL = "\u001b[2J";

    private static final String LIST = "{\"plan_id\":\"fix-relay\",\"state\":\"captured\","
            + "\"head\":\"9f3c2ab41d7e\",\"base_ref\":\"main\",\"files\":["
            + "{\"path\":\"pm-api/src/main/java/de/planmarshall/api/changes/PlanChanges.java\","
            + "\"status\":\"added\",\"added\":84,\"removed\":0,\"binary\":false,\"unplanned\":false},"
            + "{\"path\":\"pm-api/src/test/java/de/planmarshall/api/changes/PlanChangesContractTest.java\","
            + "\"renamed_from\":\"pm-api/src/test/java/de/planmarshall/api/overview/PlanChangesContractTest.java\","
            + "\"status\":\"renamed\",\"added\":61,\"removed\":8,\"binary\":false,\"unplanned\":false},"
            + "{\"path\":\"doc/design/shared/" + CONTROL_JSON + "plan-changes.svg\",\"status\":\"modified\","
            + "\"added\":0,\"removed\":0,\"binary\":true,\"unplanned\":true}]}";

    private static final String FILE = "{\"path\":\"pm-api/src/main/java/de/planmarshall/api/changes/"
            + "PlanChanges.java\",\"hunks\":[{\"old_start\":1,\"old_len\":0,\"new_start\":1,\"new_len\":3,"
            + "\"lines\":[{\"kind\":\"added\",\"text\":\"package de.planmarshall.api.changes;\"},"
            + "{\"kind\":\"context\",\"text\":\"import de.planmarshall.api.json.JsonTree;\"},"
            + "{\"kind\":\"added\",\"text\":\"" + CONTROL_JSON + "hover\"}]}],\"binary\":false,"
            + "\"truncated\":false}";

    private static final String BINARY_FILE = "{\"path\":\"doc/design/shared/plan-changes.svg\","
            + "\"hunks\":[],\"binary\":true,\"truncated\":false}";

    private static final String TRUNCATED_FILE = "{\"path\":\"pm-mock-runtime/src/main/java/de/planmarshall/"
            + "mock/runtime/MockRuntimeEngine.java\",\"hunks\":[{\"old_start\":40,\"old_len\":1,"
            + "\"new_start\":40,\"new_len\":1,\"lines\":[{\"kind\":\"removed\",\"text\":\"old\"},"
            + "{\"kind\":\"added\",\"text\":\"new\"}]}],\"binary\":false,\"truncated\":true,"
            + "\"lines_omitted\":120}";

    private static final String NOT_CAPTURED = "{\"plan_id\":\"drop-legacy-api\",\"state\":\"not_captured\"}";

    private static final String UNAVAILABLE = "{\"plan_id\":\"docs-typo\",\"state\":\"unavailable\","
            + "\"reason\":\"the captured commit 4c1d7e0 is no longer in the repository\"}";

    private static String wireJson(PlanChangesAnswer answer) {
        return JsonTree.write(answer.toMap());
    }

    @Nested
    @DisplayName("The list")
    class TheList {

        @Test
        @DisplayName("round-trips the wire JSON literally")
        void roundTrip() throws Exception {
            var answer = PlanChangesAnswer.fromJson(LIST);

            var captured = assertInstanceOf(PlanChangesCaptured.class, answer);
            assertEquals(LIST, wireJson(answer));
            assertEquals("fix-relay", captured.planId());
            assertEquals("9f3c2ab41d7e", captured.head());
            assertEquals("main", captured.baseRef());
            assertEquals(3, captured.files().size());
        }

        @Test
        @DisplayName("carries the path a file was renamed from, and only there")
        void renamedFrom() throws Exception {
            var captured = assertInstanceOf(PlanChangesCaptured.class, PlanChangesAnswer.fromJson(LIST));

            assertEquals(ChangedFileStatus.RENAMED, captured.files().get(1).status());
            assertEquals("pm-api/src/test/java/de/planmarshall/api/overview/PlanChangesContractTest.java",
                    captured.files().get(1).renamedFrom());
            assertNull(captured.files().getFirst().renamedFrom(), "an added file was renamed from nowhere");
            assertEquals(ChangedFileStatus.ADDED, captured.files().getFirst().status());
        }

        @Test
        @DisplayName("keeps a planted control sequence as text")
        void controlSequenceStaysText() throws Exception {
            var captured = assertInstanceOf(PlanChangesCaptured.class, PlanChangesAnswer.fromJson(LIST));

            assertEquals("doc/design/shared/" + CONTROL + "plan-changes.svg", captured.files().get(2).path());
        }

        @Test
        @DisplayName("derives totals that agree with the plan's size of change")
        void totalsAgree() throws Exception {
            var captured = assertInstanceOf(PlanChangesCaptured.class, PlanChangesAnswer.fromJson(LIST));

            assertEquals(new ChangeSize(3, 145, 8, 1), captured.totals());
        }
    }

    @Nested
    @DisplayName("A file")
    class AFile {

        @Test
        @DisplayName("round-trips the wire JSON literally")
        void roundTrip() throws Exception {
            var file = PlanChangeFile.fromJson(FILE);

            assertEquals(FILE, JsonTree.write(file.toMap()));
            assertEquals(1, file.hunks().size());
            assertEquals(1, file.hunks().getFirst().oldStart());
            assertEquals(3, file.hunks().getFirst().lines().size());
            assertFalse(file.truncated());
            assertFalse(file.binary());
            assertEquals(0, file.linesOmitted());
        }

        @Test
        @DisplayName("marks every line context, removed or added")
        void lineKinds() throws Exception {
            var lines = PlanChangeFile.fromJson(FILE).hunks().getFirst().lines();

            assertEquals(List.of(PlanChangeLineKind.ADDED, PlanChangeLineKind.CONTEXT, PlanChangeLineKind.ADDED),
                    lines.stream().map(PlanChangeLine::kind).toList());
            assertEquals(CONTROL + "hover", lines.get(2).text(), "a planted control sequence stays text");
        }

        @Test
        @DisplayName("answers a binary file with no lines")
        void binary() throws Exception {
            var file = PlanChangeFile.fromJson(BINARY_FILE);

            assertEquals(BINARY_FILE, JsonTree.write(file.toMap()));
            assertTrue(file.binary());
            assertTrue(file.hunks().isEmpty(), "a binary file has no lines");
        }

        @Test
        @DisplayName("answers a cut file with the count of the lines left out")
        void truncated() throws Exception {
            var file = PlanChangeFile.fromJson(TRUNCATED_FILE);

            assertEquals(TRUNCATED_FILE, JsonTree.write(file.toMap()));
            assertTrue(file.truncated());
            assertEquals(120, file.linesOmitted());
        }
    }

    @Nested
    @DisplayName("The refusals")
    class TheRefusals {

        @Test
        @DisplayName("not_captured is an answer, never an empty list")
        void notCaptured() throws Exception {
            var answer = PlanChangesAnswer.fromJson(NOT_CAPTURED);

            assertInstanceOf(PlanChangesNotCaptured.class, answer);
            assertEquals(NOT_CAPTURED, wireJson(answer));
        }

        @Test
        @DisplayName("unavailable carries its reason")
        void unavailable() throws Exception {
            var answer = PlanChangesAnswer.fromJson(UNAVAILABLE);

            var unavailable = assertInstanceOf(PlanChangesUnavailable.class, answer);
            assertEquals("docs-typo", unavailable.planId());
            assertEquals("the captured commit 4c1d7e0 is no longer in the repository", unavailable.reason());
            assertEquals(UNAVAILABLE, wireJson(answer));
        }

        @Test
        @DisplayName("an unknown state is refused, never read as an empty list")
        void unknownStateRefused() {
            assertThrows(IllegalArgumentException.class,
                    () -> PlanChangesAnswer.fromMap(JsonTree.asObject(JsonTree.parse("{\"state\":\"gone\"}"))));
        }
    }

    @Nested
    @DisplayName("The event and the activity kind")
    class TheEventAndKind {

        @Test
        @DisplayName("the catalogue knows plan.change_captured and its payload")
        void eventNameParses() throws Exception {
            assertEquals(EventName.PLAN_CHANGE_CAPTURED, EventName.fromWire("plan.change_captured"));

            var event = PlanChangeCapturedEvent.fromMap(JsonTree.asObject(JsonTree.parse(
                    "{\"plan_id\":\"fix-relay\",\"head\":\"9f3c2ab41d7e\",\"files_total\":12,"
                            + "\"insertions_total\":340,\"deletions_total\":27,\"unplanned_total\":2}")));

            assertEquals("fix-relay", event.planId());
            assertEquals("9f3c2ab41d7e", event.head());
            assertEquals(new ChangeSize(12, 340, 27, 2), event.size());
        }

        @Test
        @DisplayName("the activity kind model_differs resolves, with both models")
        void modelDiffers() throws Exception {
            assertEquals(ActivityEntryKind.MODEL_DIFFERS, ActivityEntryKind.fromValue("model_differs"));

            var entry = ActivityEntry.fromJson("{\"timestamp\":\"14:03:20\",\"kind\":\"model_differs\","
                    + "\"launch_model\":\"sonnet\",\"served_model\":\"sonnet-5.5\"}");

            assertEquals("model_differs", entry.kind());
            assertEquals("sonnet", entry.launchModel());
            assertEquals("sonnet-5.5", entry.servedModel());
            assertEquals("{\"timestamp\":\"14:03:20\",\"kind\":\"model_differs\",\"launch_model\":\"sonnet\","
                    + "\"served_model\":\"sonnet-5.5\"}", JsonTree.write(entry.toMap()));
        }
    }

    @Nested
    @DisplayName("The routes")
    class TheRoutes {

        @Test
        @DisplayName("names the list and a file of a plan")
        void routes() {
            var routes = PlanChangesRoutes.plan("fix-relay");

            assertEquals("/api/v1/plans/fix-relay/changes", routes.list());
            assertEquals("/api/v1/plans/fix-relay/changes/file?path=pm-api%2Fsrc%2FPlanChanges.java",
                    routes.file("pm-api/src/PlanChanges.java"));
            assertThrows(IllegalArgumentException.class, () -> PlanChangesRoutes.plan(" "));
        }
    }
}
