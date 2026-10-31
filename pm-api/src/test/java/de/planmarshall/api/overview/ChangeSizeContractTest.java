/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.overview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.report.PlanCompletionReport;

/**
 * The size of a plan's change on the wire: carried by the plan and by the completion report, and
 * absent before the first capture.
 */
@DisplayName("ChangeSizeContractTest")
class ChangeSizeContractTest {

    private static final String MEASURED_SIZE =
            "\"change\":{\"files\":12,\"insertions\":340,\"deletions\":27,\"unplanned\":2}";

    @Nested
    @DisplayName("The plan")
    class ThePlan {

        @Test
        @DisplayName("carries a measured size as change with all four figures")
        void carriesTheSize() throws Exception {
            var plan = ActivePlanSummary.fromJson("{\"plan_id\":\"fix-relay\",\"title\":\"Fix Relay\","
                    + "\"current_phase\":\"execute\",\"phase_state\":\"running\"," + MEASURED_SIZE + "}");

            var change = JsonTree.asObject(plan.toMap().get("change"));
            assertNotNull(change, "a measured size is served as change");
            assertEquals(12, JsonTree.integer(change, "files"));
            assertEquals(340, JsonTree.integer(change, "insertions"));
            assertEquals(27, JsonTree.integer(change, "deletions"));
            assertEquals(2, JsonTree.integer(change, "unplanned"));
        }

        @Test
        @DisplayName("leaves change absent before the first capture, never as a zero size")
        void absentBeforeTheFirstCapture() throws Exception {
            var plan = ActivePlanSummary.fromJson("{\"plan_id\":\"fix-relay\",\"title\":\"Fix Relay\"}");

            assertNull(plan.toMap().get("change"), "no capture means no size at all");
        }

        @Test
        @DisplayName("round-trips the size unchanged")
        void roundTrip() throws Exception {
            var plan = ActivePlanSummary.fromJson("{\"plan_id\":\"fix-relay\"," + MEASURED_SIZE + "}");

            var again = ActivePlanSummary.fromJson(JsonTree.write(plan.toMap()));
            assertEquals(JsonTree.write(plan.toMap()), JsonTree.write(again.toMap()));
        }
    }

    @Nested
    @DisplayName("The completion report")
    class TheCompletionReport {

        @Test
        @DisplayName("carries a measured size as change with all four figures")
        void carriesTheSize() throws Exception {
            var report = PlanCompletionReport.fromJson(
                    "{\"plan_id\":\"fix-relay\",\"title\":\"Fix Relay\",\"outcome\":\"merged\","
                            + "\"started_at\":\"2026-10-16T14:00:00Z\",\"ended_at\":\"2026-10-16T16:00:00Z\","
                            + MEASURED_SIZE + "}");

            var change = JsonTree.asObject(report.toMap().get("change"));
            assertNotNull(change, "a measured size is served as change");
            assertEquals(12, JsonTree.integer(change, "files"));
            assertEquals(340, JsonTree.integer(change, "insertions"));
            assertEquals(27, JsonTree.integer(change, "deletions"));
            assertEquals(2, JsonTree.integer(change, "unplanned"));
        }

        @Test
        @DisplayName("leaves change absent before the first capture, never as a zero size")
        void absentBeforeTheFirstCapture() throws Exception {
            var report = PlanCompletionReport.fromJson(
                    "{\"plan_id\":\"fix-relay\",\"title\":\"Fix Relay\",\"outcome\":\"merged\","
                            + "\"started_at\":\"2026-10-16T14:00:00Z\",\"ended_at\":\"2026-10-16T16:00:00Z\"}");

            assertFalse(report.toMap().containsKey("change"), "no capture means no size at all");
        }
    }
}
