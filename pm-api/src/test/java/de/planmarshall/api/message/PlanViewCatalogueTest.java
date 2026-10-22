/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.planmarshall.api.process.JobOutcome;
import de.planmarshall.api.process.JobStatus;
import de.planmarshall.api.process.OffTrackAction;
import de.planmarshall.api.process.OffTrackSignal;
import de.planmarshall.api.process.PruneReason;
import de.planmarshall.api.process.TaskOutcome;
import de.planmarshall.api.process.TaskStatus;

@DisplayName("PlanViewCatalogueTest")
class PlanViewCatalogueTest {

    @Test
    @DisplayName("should load plan_view and epic_view areas in English and German with identical key sets")
    void shouldLoadPlanAndEpicViewAreas() {
        var planEn = MessageCatalogue.getMessages("en", "plan_view");
        var planDe = MessageCatalogue.getMessages("de", "plan_view");

        assertFalse(planEn.isEmpty(), "English plan_view messages must not be empty");
        assertFalse(planDe.isEmpty(), "German plan_view messages must not be empty");
        assertEquals(new TreeSet<>(planEn.keySet()), new TreeSet<>(planDe.keySet()),
                "Keys in English and German plan_view message packs must match exactly");

        var epicEn = MessageCatalogue.getMessages("en", "epic_view");
        var epicDe = MessageCatalogue.getMessages("de", "epic_view");

        assertFalse(epicEn.isEmpty(), "English epic_view messages must not be empty");
        assertFalse(epicDe.isEmpty(), "German epic_view messages must not be empty");
        assertEquals(new TreeSet<>(epicEn.keySet()), new TreeSet<>(epicDe.keySet()),
                "Keys in English and German epic_view message packs must match exactly");
    }

    @Test
    @DisplayName("should contain all specification keys from plan-view.adoc#texts")
    void shouldContainAllSpecificationKeys() {
        var en = MessageCatalogue.getMessages("en", "plan_view");

        String[] requiredKeys = {
                "plan.view.title",
                "plan.node.state.done",
                "plan.node.state.done_times",
                "plan.node.state.running",
                "plan.node.state.running_run",
                "plan.node.state.waiting",
                "plan.node.state.failed",
                "plan.node.state.planned",
                "plan.node.state.not_taken",
                "plan.node.state.not_in_plan",
                "plan.node.kind.phase",
                "plan.node.kind.sub_workflow",
                "plan.node.kind.job",
                "plan.node.kind.tasks",
                "plan.node.kind.waits",
                "plan.not_in_plan.group",
                "plan.not_in_plan.reason",
                "plan.leaf.tasks_done",
                "plan.leaf.task",
                "plan.leaf.job",
                "plan.leaf.fired",
                "plan.leaf.entered",
                "plan.path.jump",
                "plan.view.changed",
                "plan.view.ended",
                "plan.view.state",
                "plan.view.holds",
                "plan.view.pull_request",
                "plan.leaf.job_waits_slot",
                "plan.view.tokens",
                "plan.view.tokens_none",
                "plan.view.tokens_detail",
                "plan.activity.title",
                "plan.activity.kind.text",
                "plan.activity.kind.tool_call",
                "plan.activity.kind.tool_failed",
                "plan.activity.kind.file_changed",
                "plan.activity.kind.link",
                "plan.activity.kind.question",
                "plan.activity.kind.answer_ack",
                "plan.activity.server_only",
                "plan.activity.gap",
                "plan.activity.off_track"
        };

        for (String key : requiredKeys) {
            assertTrue(en.containsKey(key), "Key '" + key + "' must exist in plan_view.en.properties");
            assertNotNull(en.get(key), "Value for '" + key + "' must not be null");
            assertFalse(en.get(key).isBlank(), "Value for '" + key + "' must not be blank");
        }
    }

    @Test
    @DisplayName("should assert every closed list enum value has a key in both English and German packs")
    void shouldHaveKeysForEveryEnumValue() {
        for (var prune : PruneReason.values()) {
            var key = "plan.prune." + prune.value();
            assertTrue(MessageCatalogue.hasKey("en", key), "Missing English key for prune reason: " + key);
            assertTrue(MessageCatalogue.hasKey("de", key), "Missing German key for prune reason: " + key);
        }

        for (var status : JobStatus.values()) {
            var key = "plan.job.status." + status.value();
            assertTrue(MessageCatalogue.hasKey("en", key), "Missing English key for job status: " + key);
            assertTrue(MessageCatalogue.hasKey("de", key), "Missing German key for job status: " + key);
        }

        for (var outcome : JobOutcome.values()) {
            var key = "plan.job.outcome." + outcome.value();
            assertTrue(MessageCatalogue.hasKey("en", key), "Missing English key for job outcome: " + key);
            assertTrue(MessageCatalogue.hasKey("de", key), "Missing German key for job outcome: " + key);
        }

        for (var status : TaskStatus.values()) {
            var key = "plan.task.status." + status.value();
            assertTrue(MessageCatalogue.hasKey("en", key), "Missing English key for task status: " + key);
            assertTrue(MessageCatalogue.hasKey("de", key), "Missing German key for task status: " + key);
        }

        for (var outcome : TaskOutcome.values()) {
            var key = "plan.task.outcome." + outcome.value();
            assertTrue(MessageCatalogue.hasKey("en", key), "Missing English key for task outcome: " + key);
            assertTrue(MessageCatalogue.hasKey("de", key), "Missing German key for task outcome: " + key);
        }

        for (var signal : OffTrackSignal.values()) {
            var key = "plan.signal." + signal.value();
            assertTrue(MessageCatalogue.hasKey("en", key), "Missing English key for signal: " + key);
            assertTrue(MessageCatalogue.hasKey("de", key), "Missing German key for signal: " + key);
        }

        for (var action : OffTrackAction.values()) {
            var key = "plan.signal.action." + action.value();
            assertTrue(MessageCatalogue.hasKey("en", key), "Missing English key for action: " + key);
            assertTrue(MessageCatalogue.hasKey("de", key), "Missing German key for action: " + key);
        }
    }

    @Test
    @DisplayName("should format parameterized plan and epic messages")
    void shouldFormatParameterizedMessages() {
        var tasksDoneEn = MessageCatalogue.format("en", "plan.leaf.tasks_done", Map.of("done", 2, "total", 5));
        assertEquals("2 of 5 tasks done", tasksDoneEn);

        var tasksDoneDe = MessageCatalogue.format("de", "plan.leaf.tasks_done", Map.of("done", 2, "total", 5));
        assertEquals("2 von 5 Tasks fertig", tasksDoneDe);

        var offTrackEn = MessageCatalogue.format("en", "plan.activity.off_track", Map.of(
                "signal", "repeating",
                "value", 5,
                "threshold", 5,
                "action", "shown"
        ));
        assertEquals("off track: repeating (5 of 5): shown", offTrackEn);

        var offTrackDe = MessageCatalogue.format("de", "plan.activity.off_track", Map.of(
                "signal", "wiederholend",
                "value", 5,
                "threshold", 5,
                "action", "angezeigt"
        ));
        assertEquals("vom Weg ab: wiederholend (5 von 5): angezeigt", offTrackDe);

        var epicTitle = MessageCatalogue.format("en", "epic.view.title", Map.of("epic", "relay-hardening"));
        assertEquals("relay-hardening", epicTitle);
    }
}
