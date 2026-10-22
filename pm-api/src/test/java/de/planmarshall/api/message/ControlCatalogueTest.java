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

import de.planmarshall.api.control.HoldKind;
import de.planmarshall.api.control.PlanAction;

@DisplayName("ControlCatalogueTest")
class ControlCatalogueTest {

    @Test
    @DisplayName("should load the control area in English and German with identical key sets")
    void shouldLoadControlArea() {
        var en = MessageCatalogue.getMessages("en", "control");
        var de = MessageCatalogue.getMessages("de", "control");

        assertFalse(en.isEmpty(), "English control messages must not be empty");
        assertFalse(de.isEmpty(), "German control messages must not be empty");
        assertEquals(new TreeSet<>(en.keySet()), new TreeSet<>(de.keySet()),
                "Keys in English and German control message packs must match exactly");
    }

    @Test
    @DisplayName("should contain all specification keys from work-actions texts")
    void shouldContainAllSpecificationKeys() {
        var en = MessageCatalogue.getMessages("en", "control");

        String[] requiredKeys = {
                "control.actions.title",
                "control.plan.pause", "control.plan.start", "control.plan.cancel",
                "control.plan.abandon", "control.plan.chat", "control.plan.settings",
                "control.plan.state.paused", "control.plan.state.pausing",
                "control.plan.epic_told", "control.plan.pull_request",
                "control.pause.title", "control.pause.now", "control.pause.after_task",
                "control.pause.after_phase", "control.pause.keep_running",
                "control.end.title", "control.end.what_ends", "control.end.pr_open",
                "control.end.pr_close", "control.end.pr_leave", "control.end.afterwards",
                "control.end.keep", "control.end.action",
                "control.settings.title", "control.settings.group", "control.settings.origin",
                "control.settings.applies", "control.settings.refused", "control.settings.too_late",
                "control.settings.epic_told", "control.settings.close", "control.settings.conflict",
                "control.epic.next", "control.epic.next_none", "control.epic.next_raise",
                "control.epic.next_leave", "control.epic.scope_full", "control.epic.auto",
                "control.epic.auto_already", "control.epic.auto_started", "control.epic.auto_nothing",
                "control.epic.auto_off_done", "control.epic.overlap", "control.epic.decompose_again",
                "control.epic.findings_review", "control.epic.watches_review", "control.epic.chat",
                "control.epic.archive", "control.epic.archive_landing", "control.epic.cancel",
                "control.archive.title", "control.archive.no_plan_active", "control.archive.rows_ended",
                "control.archive.open_findings", "control.archive.ledger_landed",
                "control.archive.completion_reviewed", "control.archive.review_first",
                "control.archive.carried", "control.archive.review_findings", "control.archive.not_yet",
                "control.archive.action",
                "control.cancel_epic.title", "control.cancel_epic.label.plans",
                "control.cancel_epic.plans_too", "control.cancel_epic.plans_on",
                "control.cancel_epic.afterwards", "control.cancel_epic.keep", "control.cancel_epic.action",
                "control.review.title", "control.review.progress", "control.review.statement",
                "control.review.rules", "control.review.origin", "control.review.proposal",
                "control.review.untrusted", "control.review.keep_finding", "control.review.keep_watch",
                "control.review.drop", "control.review.promote", "control.review.edit",
                "control.review.chat", "control.review.accept_remaining", "control.review.stage",
                "control.review.back", "control.review.finish",
                "control.hold.title", "control.hold.cause", "control.hold.tried",
                "control.hold.resolve", "control.hold.act.retry", "control.hold.act.recycle",
                "control.hold.act.accept_change", "control.hold.act.end_plan",
                "control.hold.act.abort_plan", "control.hold.act.cancel_epic",
                "control.hold.released", "control.hold.still_held", "control.hold.cleared",
                "common.conflict.changed", "common.conflict.apply_again", "common.conflict.keep"
        };

        for (String key : requiredKeys) {
            assertTrue(MessageCatalogue.hasKey("en", key), "Key '" + key + "' must exist in the English pack");
            assertTrue(MessageCatalogue.hasKey("de", key), "Key '" + key + "' must exist in the German pack");
            if (key.startsWith("control.")) {
                assertNotNull(en.get(key), "Value for '" + key + "' must not be null");
                assertFalse(en.get(key).isBlank(), "Value for '" + key + "' must not be blank");
            }
        }
    }

    @Test
    @DisplayName("should assert every plan action and hold kind has a key in both packs")
    void shouldHaveKeysForEveryEnumValue() {
        for (var action : PlanAction.values()) {
            var key = "control.plan." + action.value();
            assertTrue(MessageCatalogue.hasKey("en", key), "Missing English key for action: " + key);
            assertTrue(MessageCatalogue.hasKey("de", key), "Missing German key for action: " + key);
        }
        for (var kind : HoldKind.values()) {
            var key = "control.hold.kind." + kind.value();
            assertTrue(MessageCatalogue.hasKey("en", key), "Missing English key for hold kind: " + key);
            assertTrue(MessageCatalogue.hasKey("de", key), "Missing German key for hold kind: " + key);
        }
    }

    @Test
    @DisplayName("should format parameterized control messages in both languages")
    void shouldFormatParameterizedMessages() {
        assertEquals("Pause fix-relay?", MessageCatalogue.format("en", "control.pause.title",
                Map.of("plan", "fix-relay")));
        assertEquals("fix-relay anhalten?", MessageCatalogue.format("de", "control.pause.title",
                Map.of("plan", "fix-relay")));

        assertEquals("pausing after the task", MessageCatalogue.format("en", "control.plan.state.pausing",
                Map.of("point", "task")));
        assertEquals("h\u00e4lt nach der Phase an", MessageCatalogue.format("de", "control.plan.state.pausing",
                Map.of("point", "phase")));

        assertEquals("a condition", MessageCatalogue.format("en", "control.hold.kind.condition", Map.of()));
        assertEquals("eine Integrit\u00e4tspause", MessageCatalogue.format("de", "control.hold.kind.integrity_pause",
                Map.of()));

        assertEquals("Applies where fix-relay next reads it; the plan's version becomes 8.",
                MessageCatalogue.format("en", "control.settings.applies",
                        Map.of("plan", "fix-relay", "version", 8)));
    }
}
