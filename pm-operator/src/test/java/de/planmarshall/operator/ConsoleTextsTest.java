/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.operator.tui.TuiKeyAction;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The console's texts in both languages: every badge, every notice kind, and the report lines come
 * from the catalogue, and the German pack renders as well as the English one.
 */
@DisplayName("console texts in both languages")
class ConsoleTextsTest {

    @Test
    @DisplayName("every hold notice kind renders in English and German")
    void holdNotices() {
        for (var kind : List.of("operator_wait", "integrity_pause", "stalled", "condition", "held",
                            "landing_failed", "findings", "setup", "custom")) {
            var en = ConsoleNotice.formatHoldNotice("en", kind, "fix-relay", "detail");
            var de = ConsoleNotice.formatHoldNotice("de", kind, "fix-relay", "detail");
            assertTrue(en.startsWith("\u25c6 "), en);
            assertTrue(de.startsWith("\u25c6 "), de);
            assertTrue(en.contains("fix-relay"), en);
            assertTrue(de.contains("fix-relay"), de);
        }
    }

    @Test
    @DisplayName("questions, waivers, pairing, fresh codes, and answered-elsewhere render in both languages")
    void noticeVariants() {
        assertTrue(ConsoleNotice.formatQuestion("de", "Q-1", "plan x", "blocks", "text", "option")
                            .contains("Frage Q-1"));
        assertTrue(ConsoleNotice.formatWaiverRequest("de", "WR-1", "plan x", "gate").contains("Waiver-Anfrage"));
        assertTrue(ConsoleNotice.formatWaiverRequest("de", "WR-1", "plan x", null).contains("Waiver-Anfrage"));
        assertTrue(ConsoleNotice.formatPairedNotice("de", "iPad").contains("Gekoppelt"));
        assertTrue(ConsoleNotice.formatFreshCodeNotice("de", "CODE", "12:00", "http://x").contains("CODE"));
        assertTrue(ConsoleNotice.formatAnsweredElsewhere("de", "Q-1", "web", "iPad", "yes").contains("Q-1"));
        assertTrue(ConsoleNotice.formatAnsweredElsewhere("de", "Q-1", "console", null, "yes").contains("Q-1"));
    }

    @Test
    @DisplayName("every event type of the pushed catalogue is dispatched")
    void eventDispatch() {
        var events = List.of(
                            Map.<String, Object>of("type", "question", "id", "Q-1"),
                            Map.<String, Object>of("type", "decision", "id", "Q-2"),
                            Map.<String, Object>of("type", "waiver_request", "id", "WR-1"),
                            Map.<String, Object>of("type", "paired", "label", "iPad"),
                            Map.<String, Object>of("type", "fresh_code", "code", "C", "url", "u"),
                            Map.<String, Object>of("type", "operator_wait", "scope", "plan x"),
                            Map.<String, Object>of("type", "integrity_pause", "scope", "plan x"),
                            Map.<String, Object>of("type", "stalled", "scope", "plan x"),
                            Map.<String, Object>of("type", "condition", "scope", "plan x"),
                            Map.<String, Object>of("type", "held", "scope", "epic x"),
                            Map.<String, Object>of("type", "landing_failed", "scope", "epic x"),
                            Map.<String, Object>of("type", "findings", "scope", "epic x"),
                            Map.<String, Object>of("type", "setup", "scope", "project x"),
                            Map.<String, Object>of("type", "report", "id", "r-1"),
                            Map.<String, Object>of("type", "report.sent", "count", 2));
        for (var event : events) {
            assertNotNull(ConsoleNotice.fromEventMap("de", event), "no notice for " + event);
            assertNotNull(ConsoleNotice.fromEventMap("en", event), "no notice for " + event);
        }
    }

    @Test
    @DisplayName("the shared overview keys render in both languages")
    void overviewKeys() {
        for (var action : TuiKeyAction.values()) {
            var key = action.textKey();
            var en = MessageCatalogue.format("en", key, Map.of("view", "by epic", "count", 2,
                                "events", "1 decision", "name", "fix-relay"));
            var de = MessageCatalogue.format("de", key, Map.of("view", "nach Epic", "count", 2,
                                "events", "1 Entscheidung", "name", "fix-relay"));
            assertNotEquals(key, en, key);
            assertNotEquals(key, de, key);
        }
        assertEquals("2 more", MessageCatalogue.format("en", "operator.overview.more_above", Map.of("count", 2)));
        assertEquals("2 weitere", MessageCatalogue.format("de", "operator.overview.more_below", Map.of("count", 2)));
        assertEquals("5 reports", MessageCatalogue.format("en", "operator.overview.keys.reports", Map.of("count", 5)));
        assertEquals("5 Berichte", MessageCatalogue.format("de", "operator.overview.keys.reports", Map.of("count", 5)));
    }

    @Test
    @DisplayName("the report line names the leaf, the subject, and the report id")
    void reportNotices() {
        assertEquals("\u25aa fix-relay: merged (#214) \u00b7 reports open r-1",
                            ConsoleNotice.formatReportNotice("en", Map.of("id", "r-1",
                                                "from", "project > epic > fix-relay", "subject", "merged (#214)")));
        assertEquals("\u25aa 3 new reports \u00b7 pm \u00b7 reports",
                            ConsoleNotice.formatReportNotice("en", Map.of("count", 3, "project", "pm")));
        assertEquals("\u25aa plan: New report",
                            ConsoleNotice.formatReportNotice("en", Map.of()));
        assertEquals("\u25aa docs: no change",
                            ConsoleNotice.formatReportNotice("en", Map.of("plan", "docs", "summary", "no change")));
    }

    @Test
    @DisplayName("every badge kind renders in both languages, and the zero count yields none")
    void badges() {
        for (var kind : List.of("question", "decision", "waiver_request", "operator_wait", "integrity_pause",
                            "stalled", "condition", "held", "landing_failed", "findings", "setup", "custom")) {
            assertNotNull(ConsolePrompt.formatBadge("en", kind, 1), kind);
            assertNotNull(ConsolePrompt.formatBadge("de", kind, 2), kind);
        }
        assertNull(ConsolePrompt.formatBadge("en", "question", 0));
        assertEquals("2 Fragen", ConsolePrompt.formatBadge("de", "question", 2));
        assertEquals("3 stehen", ConsolePrompt.formatBadge("de", "stalled", 3));
        assertNotEquals(ConsolePrompt.formatBadge("de", "stalled", 3), ConsolePrompt.formatBadge("en", "stalled", 3));
        assertEquals("1 Landen fehlgeschlagen", ConsolePrompt.formatBadge("de", "landing_failed", 1));
    }

    @Test
    @DisplayName("the prompt, the truncation, and the reconnecting line render in both languages")
    void prompts() {
        assertEquals("pm \u203a ", ConsolePrompt.format("de", null, false));
        assertEquals("pm (verbinde neu) \u203a ", ConsolePrompt.format("de", null, true));
        assertEquals("pm [2 Fragen \u00b7 1 Waiver-Anfrage] \u203a ",
                            ConsolePrompt.format("de", List.of("2 Fragen", "1 Waiver-Anfrage"), false));
        assertEquals("pm [a \u00b7 b \u00b7 c \u00b7 +2] \u203a ",
                            ConsolePrompt.format("en", List.of("a", "b", "c", "d", "e"), false));
        assertEquals("Einrichtung 3/7", ConsolePrompt.formatMachineSetupBadge("de", 3, 7));
        assertEquals("Einrichtung pm 5/9", ConsolePrompt.formatProjectSetupBadge("de", "pm", 5, 9));
    }
}
