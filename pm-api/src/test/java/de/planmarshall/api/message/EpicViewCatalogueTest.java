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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifies the texts of the epic view (UC-PLN-5, design {@code epic-view.adoc#texts}): the keys the
 * shared layout needs exist in both languages, and every parameterized text formats as the design
 * writes it.
 */
@DisplayName("EpicViewCatalogueTest")
class EpicViewCatalogueTest {

    /**
     * The epic view's texts are split over two areas: the head and the row vocabulary belong to
     * {@code overview}, the sections and the web app's labels to {@code epic_view}.
     */
    private static Map<String, String> epicTexts(String locale) {
        var merged = new LinkedHashMap<String, String>();
        merged.putAll(MessageCatalogue.getMessages(locale, "overview"));
        merged.putAll(MessageCatalogue.getMessages(locale, "epic_view"));
        return merged;
    }

    @Test
    @DisplayName("should carry every epic view key in English and German")
    void shouldCarryEveryEpicViewKeyInBothLanguages() {
        var en = epicTexts("en");
        var de = epicTexts("de");

        String[] requiredKeys = {
                "overview.epic.state",
                "overview.epic.at_once",
                "overview.epic.counts.staged",
                "overview.epic.counts.running",
                "overview.epic.counts.pausing",
                "overview.epic.counts.paused",
                "overview.epic.counts.shipped",
                "overview.epic.counts.cancelled",
                "overview.epic.counts.abandoned",
                "overview.epic.counts.held",
                "overview.epic.status",
                "overview.epic.waits.dependency",
                "overview.epic.waits.lock",
                "overview.epic.waits.scope",
                "overview.epic.waits.overlap",
                "overview.epic.waits.overlap_files",
                "overview.epic.waits.none",
                "overview.epic.mode",
                "overview.epic.event.landed",
                "overview.epic.event.ended_held",
                "overview.epic.event.ended_restaged",
                "overview.epic.event.ended_left",
                "overview.epic.event.started",
                "overview.epic.event.can_start",
                "overview.epic.event.overlap_found",
                "overview.epic.event.through",
                "overview.epic.event.mode",
                "overview.epic.sources",
                "overview.epic.landings",
                "overview.epic.findings",
                "overview.epic.findings_counts",
                "overview.epic.watches",
                "overview.epic.watches_counts",
                "overview.epic.events",
                "overview.epic.tokens",
                "overview.epic.tokens_scope",
                "overview.epic.tokens_detail",
                "overview.epic.tokens_own",
                "epic.views.workstreams",
                "epic.views.landing_count",
                "epic.views.next_plan",
                "epic.views.no_events",
                "epic.views.no_sources",
                "epic.views.not_measured",
                "epic.views.not_measured_all",
                "epic.views.mode",
                "epic.views.open_plan",
                "epic.views.pr"
        };

        for (String key : requiredKeys) {
            assertTrue(en.containsKey(key), "Key '" + key + "' must exist in the English epic view texts");
            assertTrue(de.containsKey(key), "Key '" + key + "' must exist in the German epic view texts");
            assertNotNull(en.get(key), "Value for '" + key + "' must not be null");
            assertNotNull(de.get(key), "Value for '" + key + "' must not be null");
            assertFalse(en.get(key).isBlank(), "Value for '" + key + "' must not be blank in English");
            assertFalse(de.get(key).isBlank(), "Value for '" + key + "' must not be blank in German");
        }
    }

    @Test
    @DisplayName("should keep the German epic view keys exactly the English set")
    void shouldKeepGermanKeysExactlyTheEnglishSet() {
        var enKeys = new TreeSet<>(epicTexts("en").keySet());
        var deKeys = new TreeSet<>(epicTexts("de").keySet());

        var missingInDe = new TreeSet<>(enKeys);
        missingInDe.removeAll(deKeys);
        var extraneousInDe = new TreeSet<>(deKeys);
        extraneousInDe.removeAll(enKeys);

        assertTrue(missingInDe.isEmpty(), "Keys missing in German epic view texts: " + missingInDe);
        assertTrue(extraneousInDe.isEmpty(), "Keys in German epic view texts but not in English: " + extraneousInDe);
    }

    @Test
    @DisplayName("should format the head, status, and wait texts of the closed lists")
    void shouldFormatHeadStatusAndWaitTexts() {
        assertEquals("running", MessageCatalogue.format("en", "overview.epic.state", Map.of("state", "running")));
        assertEquals("läuft", MessageCatalogue.format("de", "overview.epic.state", Map.of("state", "running")));
        assertEquals("waiting for you",
                MessageCatalogue.format("en", "overview.epic.state", Map.of("state", "waiting")));
        assertEquals("unknown_state", MessageCatalogue.format("en", "overview.epic.state", Map.of("state", "unknown_state")));

        assertEquals("2 of 3 at once",
                MessageCatalogue.format("en", "overview.epic.at_once", Map.of("running", 2, "scope", 3)));
        assertEquals("2 von 3 gleichzeitig",
                MessageCatalogue.format("de", "overview.epic.at_once", Map.of("running", 2, "scope", 3)));

        assertEquals("1 running", MessageCatalogue.format("en", "overview.epic.counts.running", Map.of("count", 1)));
        assertEquals("2 laufen", MessageCatalogue.format("de", "overview.epic.counts.running", Map.of("count", 2)));
        assertEquals("3 staged", MessageCatalogue.format("en", "overview.epic.counts.staged", Map.of("count", 3)));

        assertEquals("paused by you",
                MessageCatalogue.format("en", "overview.epic.status", Map.of("status", "paused")));
        assertEquals("von dir angehalten",
                MessageCatalogue.format("de", "overview.epic.status", Map.of("status", "paused")));
        assertEquals("handed back",
                MessageCatalogue.format("en", "overview.epic.status", Map.of("status", "handed_back")));
        assertEquals("the row's own status",
                MessageCatalogue.format("en", "overview.epic.status", Map.of("status", "the row's own status")));

        assertEquals("waits: overlap with relay-auth-v2",
                MessageCatalogue.format("en", "overview.epic.waits.overlap", Map.of("plan", "relay-auth-v2")));
        assertEquals("wartet: Überschneidung mit relay-auth-v2",
                MessageCatalogue.format("de", "overview.epic.waits.overlap", Map.of("plan", "relay-auth-v2")));
        assertEquals("after relay-auth-v2",
                MessageCatalogue.format("en", "overview.epic.waits.dependency", Map.of("plan", "relay-auth-v2")));
        assertEquals("waits for the lock on landing-a",
                MessageCatalogue.format("en", "overview.epic.waits.lock", Map.of("lock", "landing-a")));
        assertEquals("waits for a free place: 3 of 3 taken",
                MessageCatalogue.format("en", "overview.epic.waits.scope", Map.of("running", 3, "scope", 3)));
        assertEquals("can start", MessageCatalogue.format("en", "overview.epic.waits.none", Map.of()));

        assertEquals("1 file in common with relay-auth-v2: pm-api/src/A.java",
                MessageCatalogue.format("en", "overview.epic.waits.overlap_files",
                        Map.of("files", 1, "plan", "relay-auth-v2", "paths", "pm-api/src/A.java")));
        assertEquals("2 gemeinsame Dateien mit relay-auth-v2: a, b",
                MessageCatalogue.format("de", "overview.epic.waits.overlap_files",
                        Map.of("files", 2, "plan", "relay-auth-v2", "paths", "a, b")));

        assertEquals("automatic", MessageCatalogue.format("en", "overview.epic.mode", Map.of("mode", "automatic")));
        assertEquals("von Hand", MessageCatalogue.format("de", "overview.epic.mode", Map.of("mode", "manual")));
    }

    @Test
    @DisplayName("should format the event texts of what the epic did by itself")
    void shouldFormatEventTexts() {
        assertEquals("relay-cursor-order landed; relay-reconnect-metrics re-ordered",
                MessageCatalogue.format("en", "overview.epic.event.landed",
                        Map.of("plan", "relay-cursor-order", "change", "reorder",
                                "plans", "relay-reconnect-metrics")));
        assertEquals("relay-cursor-order ist gelandet; relay-reconnect-metrics neu geordnet",
                MessageCatalogue.format("de", "overview.epic.event.landed",
                        Map.of("plan", "relay-cursor-order", "change", "reorder",
                                "plans", "relay-reconnect-metrics")));
        assertEquals("relay-cursor-order landed; no staged plan changed",
                MessageCatalogue.format("en", "overview.epic.event.landed",
                        Map.of("plan", "relay-cursor-order", "change", "none", "plans", "")));

        assertEquals("relay-auth-v2 ended (cancelled); relay-token-rotation held",
                MessageCatalogue.format("en", "overview.epic.event.ended_held",
                        Map.of("plan", "relay-auth-v2", "outcome", "cancelled",
                                "plans", "relay-token-rotation")));
        assertEquals("relay-auth-v2 ended (cancelled); staged again as relay-auth-v3",
                MessageCatalogue.format("en", "overview.epic.event.ended_restaged",
                        Map.of("plan", "relay-auth-v2", "outcome", "cancelled", "row", "relay-auth-v3")));
        assertEquals("relay-auth-v2 ended (cancelled); the staged plans stay as they are",
                MessageCatalogue.format("en", "overview.epic.event.ended_left",
                        Map.of("plan", "relay-auth-v2", "outcome", "cancelled")));

        assertEquals("started relay-token-store (2 of 3 at once)",
                MessageCatalogue.format("en", "overview.epic.event.started",
                        Map.of("plan", "relay-token-store", "running", 2, "scope", 3)));
        assertEquals("can start now: relay-token-store",
                MessageCatalogue.format("en", "overview.epic.event.can_start", Map.of("plans", "relay-token-store")));
        assertEquals("relay-cursor-order and relay-docs changed 3 files in common",
                MessageCatalogue.format("en", "overview.epic.event.overlap_found",
                        Map.of("plan", "relay-cursor-order", "other", "relay-docs", "files", 3)));
        assertEquals("relay-hardening is through: the completion review is ready",
                MessageCatalogue.format("en", "overview.epic.event.through",
                        Map.of("epic", "relay-hardening", "review", "ready")));
        assertEquals("relay-hardening is through: archiving runs its completion review",
                MessageCatalogue.format("en", "overview.epic.event.through",
                        Map.of("epic", "relay-hardening", "review", "other")));
        assertEquals("you switched automatic off",
                MessageCatalogue.format("en", "overview.epic.event.mode", Map.of("mode", "manual")));
        assertEquals("du hast den automatischen Start eingeschaltet",
                MessageCatalogue.format("de", "overview.epic.event.mode", Map.of("mode", "automatic")));
    }

    @Test
    @DisplayName("should format the registers, landings, and token scope")
    void shouldFormatRegistersLandingsAndTokenScope() {
        assertEquals("3 new · 4 open · 2 kept",
                MessageCatalogue.format("en", "overview.epic.findings_counts",
                        Map.of("new", 3, "open", 4, "kept", 2)));
        assertEquals("3 neue · 4 offene · 2 behaltene",
                MessageCatalogue.format("de", "overview.epic.findings_counts",
                        Map.of("new", 3, "open", 4, "kept", 2)));
        assertEquals("1 open · 0 kept",
                MessageCatalogue.format("en", "overview.epic.watches_counts", Map.of("open", 1, "kept", 0)));

        assertEquals("1 landing", MessageCatalogue.format("en", "epic.views.landing_count", Map.of("count", 1)));
        assertEquals("2 landings", MessageCatalogue.format("en", "epic.views.landing_count", Map.of("count", 2)));
        assertEquals("2 Landungen", MessageCatalogue.format("de", "epic.views.landing_count", Map.of("count", 2)));
        assertEquals("1 task not measured",
                MessageCatalogue.format("en", "epic.views.not_measured", Map.of("count", 1)));

        assertEquals("4 plans and the epic's own tasks",
                MessageCatalogue.format("en", "overview.epic.tokens_scope", Map.of("plans", 4)));
        assertEquals("1 plan and the epic's own tasks",
                MessageCatalogue.format("en", "overview.epic.tokens_scope", Map.of("plans", 1)));
        assertEquals("2 Pläne und die eigenen Tasks des Epics",
                MessageCatalogue.format("de", "overview.epic.tokens_scope", Map.of("plans", 2)));

        assertEquals("Automatic: on", MessageCatalogue.format("en", "epic.views.mode", Map.of("mode", "on")));
        assertEquals("Automatik: aus", MessageCatalogue.format("de", "epic.views.mode",
                Map.of("mode", MessageCatalogue.format("de", "epic.views.mode_off", Map.of()))));
        assertEquals("#211", MessageCatalogue.format("en", "epic.views.pr", Map.of("number", 211)));
    }

    @Test
    @DisplayName("should keep the wait reason texts for every closed reason")
    void shouldKeepAWaitTextForEveryClosedReason() {
        var en = epicTexts("en");
        String[] reasons = {"dependency", "lock", "scope", "overlap", "stale", "held"};
        for (String reason : reasons) {
            var key = "epic.views.wait." + reason;
            assertTrue(en.containsKey(key), "A wait text for the closed reason '" + reason + "' must exist");
            var formatted = MessageCatalogue.format("en", key, Map.of("plan", "relay-docs", "other", "relay-auth-v2"));
            assertFalse(formatted.isBlank(), "The wait text for '" + reason + "' must format to a line");
        }
    }
}
