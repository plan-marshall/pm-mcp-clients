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

import java.io.IOException;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import de.planmarshall.api.process.JobOutcome;
import de.planmarshall.api.process.JobStatus;
import de.planmarshall.api.process.OffTrackAction;
import de.planmarshall.api.process.OffTrackSignal;
import de.planmarshall.api.process.PruneReason;
import de.planmarshall.api.process.TaskOutcome;
import de.planmarshall.api.process.TaskStatus;

@DisplayName("PlanViewCatalogueTest")
class PlanViewCatalogueTest {

    /** The design of the plan view, whose Texts table is the source of the area's keys. */
    private static final String PLAN_VIEW_DESIGN = "shared/plan-view.adoc";

    /** The prefix of the changes keys, the area whose pack may hold no key the design does not name. */
    private static final String CHANGES_PREFIX = "plan.changes.";

    /**
     * @return the rows of the design's Texts table, one per key
     * @throws IOException when the design cannot be read
     */
    static Stream<Arguments> designRows() throws IOException {
        return SpecificationTables.rows(PLAN_VIEW_DESIGN, "plan.").stream()
                .map(row -> Arguments.of(row.key(), row));
    }


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

    @ParameterizedTest(name = "{0}")
    @MethodSource("designRows")
    @DisplayName("each key of the design's Texts table is in both packs, and the design's text where this plan adds it")
    void designKeyIsInBothPacks(String key, SpecificationTables.Row row) {
        for (var locale : MessageCatalogue.SUPPORTED_LOCALES) {
            var pack = MessageCatalogue.getMessages(locale, "plan_view");
            assertTrue(pack.containsKey(key), "the " + locale + " pack must hold " + key);
            assertFalse(pack.get(key).isBlank(), "the " + locale + " text of " + key + " must not be blank");
        }
        if (key.startsWith(CHANGES_PREFIX)) {
            // The changes keys are this plan's; their texts are the design's, verbatim.
            assertEquals(row.english(), MessageCatalogue.getMessages("en", "plan_view").get(key),
                    "English text of " + key);
            assertEquals(row.german(), MessageCatalogue.getMessages("de", "plan_view").get(key),
                    "German text of " + key);
        }
    }

    @Test
    @DisplayName("the design's Texts table yields rows, so an unreadable table never passes")
    void designTableYieldsRows() throws Exception {
        assertFalse(SpecificationTables.rows(PLAN_VIEW_DESIGN, "plan.").isEmpty(),
                "no plan.* row read from " + PLAN_VIEW_DESIGN);
        assertFalse(SpecificationTables.rows(PLAN_VIEW_DESIGN, CHANGES_PREFIX).isEmpty(),
                "no " + CHANGES_PREFIX + "* row read from " + PLAN_VIEW_DESIGN);
    }

    @Test
    @DisplayName("the packs hold no plan.changes.* key the design does not name")
    void noChangesKeyBeyondTheDesign() throws Exception {
        var design = new TreeSet<String>();
        SpecificationTables.rows(PLAN_VIEW_DESIGN, CHANGES_PREFIX).forEach(row -> design.add(row.key()));

        for (var locale : MessageCatalogue.SUPPORTED_LOCALES) {
            var pack = new TreeSet<String>();
            MessageCatalogue.getMessages(locale, "plan_view").keySet().stream()
                    .filter(packKey -> packKey.startsWith(CHANGES_PREFIX))
                    .forEach(pack::add);
            assertEquals(design, pack, "the " + locale + " pack's " + CHANGES_PREFIX + "* keys are the design's");
        }
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
                "plan.node.select",
                "plan.node.narrative",
                "plan.node.child_tasks",
                "plan.node.child_nodes",
                "plan.node.waits_for",
                "plan.node.prune_reason",
                "plan.node.attempt",
                "plan.node.no_activity",
                "plan.node.has_activity",
                "plan.node.no_activity_link",
                "plan.node.run",
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
                "plan.activity.model_differs",
                "plan.activity.server_only",
                "plan.activity.gap",
                "plan.activity.off_track",
                "plan.activity.select_item",
                "plan.change.size"
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
    @DisplayName("retires the old signal keys and carries failing_again, edit_churn and agent_started in both locales")
    void signalKeysReflectClosedSetOfFive() {
        var retired = java.util.List.of(
                "state.signal.over_size", "state.signal.outside_footprint",
                "plan.signal.over_size", "plan.signal.outside_footprint");
        var added = java.util.List.of("failing_again", "edit_churn", "agent_started");
        for (var locale : MessageCatalogue.SUPPORTED_LOCALES) {
            var common = MessageCatalogue.getMessages(locale, "common");
            var planView = MessageCatalogue.getMessages(locale, "plan_view");
            var all = MessageCatalogue.getMessages(locale, "all");
            for (var key : retired) {
                assertFalse(all.containsKey(key), "Retired key must be absent in " + locale + ": " + key);
            }
            for (var signal : added) {
                var stateKey = "state.signal." + signal;
                var planKey = "plan.signal." + signal;
                assertTrue(common.containsKey(stateKey), "Missing " + stateKey + " in " + locale);
                assertFalse(common.get(stateKey).isBlank(), "Blank " + stateKey + " in " + locale);
                assertTrue(planView.containsKey(planKey), "Missing " + planKey + " in " + locale);
                assertFalse(planView.get(planKey).isBlank(), "Blank " + planKey + " in " + locale);
            }
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

    @Test
    @DisplayName("should format the size of a plan's change in both languages")
    void shouldFormatTheChangeSize() {
        assertTrue(MessageCatalogue.hasKey("en", "plan.change.size"), "Missing English key plan.change.size");
        assertTrue(MessageCatalogue.hasKey("de", "plan.change.size"), "Missing German key plan.change.size");

        var measured = Map.<String, Object>of("files", 12, "insertions", 340, "deletions", 27, "unplanned", 2);
        assertEquals("12 files \u00b7 +340 \u221227 \u00b7 2 unplanned",
                MessageCatalogue.format("en", "plan.change.size", measured));
        assertEquals("12 Dateien \u00b7 +340 \u221227 \u00b7 2 ungeplant",
                MessageCatalogue.format("de", "plan.change.size", measured));

        // Plural forms of the file count, in both languages.
        var oneFile = Map.<String, Object>of("files", 1, "insertions", 3, "deletions", 0, "unplanned", 1);
        assertEquals("1 file \u00b7 +3 \u22120 \u00b7 1 unplanned",
                MessageCatalogue.format("en", "plan.change.size", oneFile));
        assertEquals("1 Datei \u00b7 +3 \u22120 \u00b7 1 ungeplant",
                MessageCatalogue.format("de", "plan.change.size", oneFile));

        // A zero count of unplanned changes leaves the part out, in both languages.
        var noneUnplanned = Map.<String, Object>of("files", 2, "insertions", 8, "deletions", 1, "unplanned", 0);
        assertEquals("2 files \u00b7 +8 \u22121", MessageCatalogue.format("en", "plan.change.size", noneUnplanned));
        assertEquals("2 Dateien \u00b7 +8 \u22121", MessageCatalogue.format("de", "plan.change.size", noneUnplanned));
    }
}
