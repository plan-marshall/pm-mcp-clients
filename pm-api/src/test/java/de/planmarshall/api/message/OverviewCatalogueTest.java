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

@DisplayName("OverviewCatalogueTest")
class OverviewCatalogueTest {

    @Test
    @DisplayName("should load overview area in English and German with identical key sets")
    void shouldLoadOverviewAreaInEnglishAndGerman() {
        var en = MessageCatalogue.getMessages("en", "overview");
        var de = MessageCatalogue.getMessages("de", "overview");

        assertFalse(en.isEmpty(), "English overview messages must not be empty");
        assertFalse(de.isEmpty(), "German overview messages must not be empty");

        var enKeys = new TreeSet<>(en.keySet());
        var deKeys = new TreeSet<>(de.keySet());

        assertEquals(enKeys, deKeys, "Keys in English and German overview message packs must match exactly");
    }

    @Test
    @DisplayName("should contain all specification keys from plan-overview.adoc and 15-overview.adoc")
    void shouldContainAllSpecificationKeys() {
        var en = MessageCatalogue.getMessages("en", "overview");

        String[] requiredKeys = {
                "overview.running.title",
                "overview.projects.empty",
                "overview.project.counts",
                "overview.project.setup",
                "overview.project.left",
                "overview.view.by_plan",
                "overview.view.by_epic",
                "overview.kind.plan",
                "overview.kind.epic",
                "overview.group.without_epic",
                "overview.needs.count",
                "overview.needs.decision",
                "overview.needs.waiver_request",
                "overview.needs.operator_wait",
                "overview.needs.integrity_pause",
                "overview.needs.stalled",
                "overview.needs.condition",
                "overview.needs.held",
                "overview.needs.landing_failed",
                "overview.needs.findings",
                "overview.needs.setup",
                "overview.state.running",
                "overview.state.pausing",
                "overview.state.paused",
                "overview.state.waiting",
                "overview.state.held",
                "overview.time.running",
                "overview.time.in_phase",
                "overview.progress.tasks",
                "overview.progress.plans",
                "overview.epic.plans",
                "overview.plan.belongs_to",
                "overview.plan.pull_request",
                "overview.plan.off_track",
                "overview.group.alongside",
                "overview.landing.removal",
                "overview.landing.unit",
                "overview.connection.lost",
                "overview.fallback.ended",
                "overview.ended.title",
                "overview.ended.open",
                "overview.ended.empty",
                "overview.ended.outcome",
                "overview.ended.epic_end",
                "overview.ended.reason",
                "overview.ended.time",
                "overview.ended.page",
                "overview.ended.newer",
                "overview.ended.older",
                "overview.ended.as_it_ended",
                "operator.overview.keys.move",
                "operator.overview.keys.open",
                "operator.overview.keys.back",
                "operator.overview.keys.filter",
                "operator.overview.keys.epic",
                "operator.overview.keys.waits",
                "operator.overview.keys.switch_view",
                "operator.overview.keys.help",
                "operator.overview.keys.leave",
                "operator.overview.more_above",
                "operator.overview.more_below",
                "operator.overview.connected",
                "operator.overview.meanwhile",
                "operator.overview.too_narrow",
                "operator.overview.keys.actions",
                "operator.overview.keys.ended",
                "operator.overview.keys.reports",
                "operator.overview.ended_view_chat",
                "operator.status.ended_title",
                "operator.status.pull_request",
                "operator.status.reason",
                "operator.status.runtime"
        };

        for (String key : requiredKeys) {
            assertTrue(en.containsKey(key), "Key '" + key + "' must exist in overview.en.properties");
            assertNotNull(en.get(key), "Value for '" + key + "' must not be null");
            assertFalse(en.get(key).isBlank(), "Value for '" + key + "' must not be blank");
        }
    }

    @Test
    @DisplayName("should format parameterized overview messages")
    void shouldFormatParameterizedOverviewMessages() {
        var offTrackEn = MessageCatalogue.format("en", "overview.plan.off_track", Map.of("signal", "repeating"));
        assertEquals("off track: repeating", offTrackEn);

        var offTrackDe = MessageCatalogue.format("de", "overview.plan.off_track", Map.of("signal", "repeating"));
        assertEquals("vom Weg ab: repeating", offTrackDe);

        var runningTitle = MessageCatalogue.format("en", "overview.running.title", Map.of());
        assertEquals("Running work", runningTitle);

        var countsEn1 = MessageCatalogue.format("en", "overview.project.counts", Map.of("plans", 1, "epics", 2));
        assertEquals("1 plan · 2 epics", countsEn1);

        var countsDe = MessageCatalogue.format("de", "overview.project.counts", Map.of("plans", 3, "epics", 1));
        assertEquals("3 Pläne · 1 Epic", countsDe);

        var runtimeMsg = MessageCatalogue.format("en", "operator.status.runtime", Map.of(
                "version", "0.9.1",
                "time", "08:14",
                "workers", 2,
                "slots", 1,
                "total", 2
        ));
        assertEquals("Runtime 0.9.1, running since 08:14 · 2 workers · 1 of 2 build slots in use", runtimeMsg);

        var pausingTask = MessageCatalogue.format("en", "overview.state.pausing", Map.of("point", "task"));
        assertEquals("pausing after the task", pausingTask);

        var outcomeMerged = MessageCatalogue.format("en", "overview.ended.outcome", Map.of("outcome", "merged"));
        assertEquals("merged", outcomeMerged);

        // Verify newly added keys for waiver and waits_for_you
        assertEquals("waiver", MessageCatalogue.format("en", "overview.needs.waiver", Map.of()));
        assertEquals("Waiver", MessageCatalogue.format("de", "overview.needs.waiver", Map.of()));
        assertEquals("waits for you", MessageCatalogue.format("en", "overview.needs.waits_for_you", Map.of()));
        assertEquals("wartet auf dich", MessageCatalogue.format("de", "overview.needs.waits_for_you", Map.of()));
    }

    @Test
    @DisplayName("should normalize locale and handle fallback languages")
    void shouldNormalizeLocale() {
        assertEquals("en", MessageCatalogue.normalizeLocale(null));
        assertEquals("en", MessageCatalogue.normalizeLocale(""));
        assertEquals("en", MessageCatalogue.normalizeLocale("   "));
        assertEquals("en", MessageCatalogue.normalizeLocale("en"));
        assertEquals("de", MessageCatalogue.normalizeLocale("de"));
        assertEquals("en", MessageCatalogue.normalizeLocale("en-US"));
        assertEquals("de", MessageCatalogue.normalizeLocale("de_DE"));
        assertEquals("en", MessageCatalogue.normalizeLocale("fr-FR"));
        assertEquals("en", MessageCatalogue.normalizeLocale("es_ES"));
    }

    @Test
    @DisplayName("should handle ICU parser edge cases and branch coverage")
    void shouldHandleIcuEdgeCases() {
        // Fallback when key is missing completely
        assertEquals("unknown.key", MessageCatalogue.format("en", "unknown.key", Map.of()));

        // Template without braces
        assertEquals("Running work", MessageCatalogue.format("en", "overview.running.title", null));

        // Plural exact match (=0) vs one vs other
        var reports0 = MessageCatalogue.format("en", "operator.overview.keys.reports", Map.of("count", 0));
        assertEquals("reports", reports0);
        var reports1 = MessageCatalogue.format("en", "operator.overview.keys.reports", Map.of("count", 1));
        assertEquals("1 report", reports1);
        var reports5 = MessageCatalogue.format("en", "operator.overview.keys.reports", Map.of("count", 5));
        assertEquals("5 reports", reports5);
        var reportsInvalid = MessageCatalogue.format("en", "operator.overview.keys.reports", Map.of("count", "not_a_num"));
        assertEquals("reports", reportsInvalid);

        // Select other fallback
        var pausingPhase = MessageCatalogue.format("en", "overview.state.pausing", Map.of("point", "other_point"));
        assertEquals("pausing after the phase", pausingPhase);

        // Ended outcome other fallback
        var outcomeOther = MessageCatalogue.format("en", "overview.ended.outcome", Map.of("outcome", "unknown_outcome"));
        assertEquals("ended", outcomeOther);
    }
}
