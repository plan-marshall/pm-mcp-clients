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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ConsoleNotice")
class ConsoleNoticeTest {

    @Nested
    @DisplayName("control sequence stripping and output safety")
    class OutputSafety {

        @Test
        @DisplayName("handles null and empty strings safely")
        void nullAndEmpty() {
            assertAll(
                                () -> assertEquals("", ConsoleNotice.stripControlSequences(null)),
                                () -> assertEquals("", ConsoleNotice.stripControlSequences(""))
            );
        }

        @Test
        @DisplayName("leaves clean text intact")
        void cleanText() {
            var text = "Plain text with\nnewlines and\ttabs.";
            assertEquals(text, ConsoleNotice.stripControlSequences(text));
        }

        @Test
        @DisplayName("strips ANSI color escapes and control characters")
        void stripsAnsiAndControl() {
            var raw = "\u001B[31;1mRed Alert\u001B[0m\u0007 \u0008and bell";
            assertEquals("Red Alert and bell", ConsoleNotice.stripControlSequences(raw));
        }
    }

    @Nested
    @DisplayName("notice block formatting")
    class NoticeFormatting {

        @Test
        @DisplayName("formats question with summary and recommendation")
        void questionNoticeFull() {
            var notice = ConsoleNotice.formatQuestion(
                                "Q-007", "plan fix-relay", "blocks the plan",
                                "The change conflicts with main in RelayServer.java.",
                                "2) Rebase onto main");

            assertAll(
                                () -> assertTrue(notice.contains("◆ Question Q-007 · plan fix-relay · blocks the plan")),
                                () -> assertTrue(notice.contains("The change conflicts with main in RelayServer.java.")),
                                () -> assertTrue(notice.contains("Recommended: 2) Rebase onto main")),
                                () -> assertTrue(notice.contains("answer Q-007   ·   questions"))
            );
        }

        @Test
        @DisplayName("formats question without summary or recommendation")
        void questionNoticeMinimal() {
            var notice = ConsoleNotice.formatQuestion(
                                "Q-001", "epic storage-v2", null, "Proceed with migration?", null);

            assertAll(
                                () -> assertTrue(notice.contains("◆ Question Q-001 · epic storage-v2")),
                                () -> assertTrue(notice.contains("Proceed with migration?")),
                                () -> assertTrue(notice.contains("answer Q-001   ·   questions"))
            );
        }

        @Test
        @DisplayName("formats waiver request")
        void waiverRequest() {
            var notice = ConsoleNotice.formatWaiverRequest(
                                "WR-2", "plan fix-relay", "ci_check:integration-tests");

            assertAll(
                                () -> assertTrue(notice.contains("◆ Waiver request WR-2 · plan fix-relay · gate ci_check:integration-tests")),
                                () -> assertTrue(notice.contains("answer WR-2   ·   questions"))
            );
        }

        @Test
        @DisplayName("holds in English")
        void holdNotices() {
            assertAll(
                                () -> assertEquals("◆ Plan fix-relay waits for you: token approval",
                                                    ConsoleNotice.formatHoldNotice("operator_wait", "fix-relay", "token approval")),
                                () -> assertEquals("◆ Integrity pause · plan fix-relay: checksum mismatch",
                                                    ConsoleNotice.formatHoldNotice("integrity_pause", "fix-relay", "checksum mismatch")),
                                () -> assertEquals("◆ Plan fix-relay stalled: worker died",
                                                    ConsoleNotice.formatHoldNotice("stalled", "fix-relay", "worker died")),
                                () -> assertEquals("◆ Plan fix-relay needs a fix: broken symlink",
                                                    ConsoleNotice.formatHoldNotice("condition", "fix-relay", "broken symlink")),
                                () -> assertEquals("◆ Epic storage-v2: 1 held for you",
                                                    ConsoleNotice.formatHoldNotice("held", "storage-v2", "1 held for you")),
                                () -> assertEquals("◆ Epic storage-v2: the landing failed: git conflict",
                                                    ConsoleNotice.formatHoldNotice("landing_failed", "storage-v2", "git conflict")),
                                () -> assertEquals("◆ Epic storage-v2: 3 new findings",
                                                    ConsoleNotice.formatHoldNotice("findings", "storage-v2", "3 new findings")),
                                () -> assertEquals("◆ Project plan-marshall: the setup needs you: token needed",
                                                    ConsoleNotice.formatHoldNotice("setup", "plan-marshall", "token needed")),
                                () -> assertEquals("◆ custom · plan-x: detail",
                                                    ConsoleNotice.formatHoldNotice("custom", "plan-x", "detail"))
            );
        }

        @Test
        @DisplayName("formats paired notice")
        void pairedNotice() {
            var notice = ConsoleNotice.formatPairedNotice("local (Firefox on mbp-oliver)");
            assertEquals("✓ Paired: local (Firefox on mbp-oliver)", notice);
        }

        @Test
        @DisplayName("formats fresh code notice")
        void freshCodeNotice() {
            var notice = ConsoleNotice.formatFreshCodeNotice("KXQB-MRTN", "14:32", "http://127.0.0.1:7420/pair#code=KXQB-MRTN");
            assertAll(
                                () -> assertTrue(notice.contains("✓ New code: KXQB-MRTN, valid until 14:32")),
                                () -> assertTrue(notice.contains("The previous code no longer works.")),
                                () -> assertTrue(notice.contains("http://127.0.0.1:7420/pair#code=KXQB-MRTN"))
            );
        }

        @Test
        @DisplayName("formats answered elsewhere notice")
        void answeredElsewhere() {
            var withDevice = ConsoleNotice.formatAnsweredElsewhere("Q-007", "web app", "iPad", "Rebase onto main");
            var withoutDevice = ConsoleNotice.formatAnsweredElsewhere("Q-008", "console", null, "Abort");

            assertAll(
                                () -> assertEquals("– Q-007 was answered in the web app (iPad): Rebase onto main. Nothing was sent from here.", withDevice),
                                () -> assertEquals("– Q-008 was answered in the console: Abort. Nothing was sent from here.", withoutDevice)
            );
        }
    }

    @Nested
    @DisplayName("event map dispatch")
    class EventMapDispatch {

        @Test
        @DisplayName("dispatches question and waiver events")
        void questionAndWaiver() {
            var qEvent = Map.<String, Object>of(
                                "type", "question",
                                "id", "Q-001",
                                "scope", "plan fix-relay",
                                "summary", "merge conflict",
                                "text", "Conflicts in File.java",
                                "recommendation", "Rebase"
            );
            var wrEvent = Map.<String, Object>of(
                                "type", "waiver_request",
                                "id", "WR-1",
                                "scope", "plan fix-relay",
                                "gate", "sonar"
            );

            assertAll(
                                () -> assertNotNull(ConsoleNotice.fromEventMap(qEvent)),
                                () -> assertNotNull(ConsoleNotice.fromEventMap(wrEvent))
            );
        }

        @Test
        @DisplayName("dispatches paired and fresh code events")
        void pairedAndFreshCode() {
            var pairedEvent = Map.<String, Object>of(
                                "type", "paired",
                                "label", "local browser"
            );
            var freshEvent = Map.<String, Object>of(
                                "type", "fresh_code",
                                "code", "NEWCODE1",
                                "valid_until", "15:00",
                                "url", "http://127.0.0.1:7420/pair#code=NEWCODE1"
            );

            assertAll(
                                () -> assertTrue(ConsoleNotice.fromEventMap(pairedEvent).contains("✓ Paired: local browser")),
                                () -> assertTrue(ConsoleNotice.fromEventMap(freshEvent).contains("✓ New code: NEWCODE1"))
            );
        }

        @Test
        @DisplayName("returns null for null map or unknown event type")
        void nullOrUnknown() {
            assertAll(
                                () -> assertNull(ConsoleNotice.fromEventMap(null)),
                                () -> assertNull(ConsoleNotice.fromEventMap(Map.of("type", "unknown_ping")))
            );
        }

        @Test
        @DisplayName("dispatches report notice events")
        void reportNoticeDispatch() {
            var multi = Map.<String, Object>of(
                                "type", "report.sent",
                                "count", 3,
                                "project", "pm-mcp-clients"
            );
            var single = Map.<String, Object>of(
                                "type", "report",
                                "id", "rep-101",
                                "from", "pm-mcp-clients > relay-hardening > fix-relay",
                                "subject", "merged (#214)"
            );
            var singleSummary = Map.<String, Object>of(
                                "type", "report",
                                "plan", "docs-typo",
                                "summary", "no change"
            );
            var fallback = Map.<String, Object>of(
                                "type", "report"
            );

            assertAll(
                                () -> assertEquals("▪ 3 new reports · pm-mcp-clients · reports", ConsoleNotice.fromEventMap(multi)),
                                () -> assertEquals("▪ fix-relay: merged (#214) · reports open rep-101", ConsoleNotice.fromEventMap(single)),
                                () -> assertEquals("▪ docs-typo: no change", ConsoleNotice.fromEventMap(singleSummary)),
                                () -> assertEquals("▪ plan: New report", ConsoleNotice.fromEventMap(fallback))
            );
        }
    }

    @Nested
    @DisplayName("event name dispatch")
    class NamedEventDispatch {

        @Test
        @DisplayName("maps decision.opened from the envelope to the question notice")
        void decisionOpened() {
            var event = Map.<String, Object>of(
                                "event", "decision.opened",
                                "id", 42,
                                "scope", Map.of("kind", "plan", "id", "fix-relay"),
                                "data", Map.of("decision", Map.of(
                                                    "id", "Q-007",
                                                    "subject", "plan fix-relay",
                                                    "blocks", "plan",
                                                    "description", "The change conflicts with main in RelayServer.java.",
                                                    "parts", List.of(Map.of(
                                                                        "part_id", "1",
                                                                        "question", "How should the conflict be resolved?",
                                                                        "options", List.of(
                                                                                            Map.of("key", "1", "label", "Merge main into the branch"),
                                                                                            Map.of("key", "2", "label", "Rebase onto main")),
                                                                        "recommendation", "2")))));

            var notice = ConsoleNotice.fromEventMap("en", "decision.opened", event);

            assertAll(
                                () -> assertNotNull(notice),
                                () -> assertTrue(notice.contains("◆ Question Q-007 · plan fix-relay · blocks the plan"), notice),
                                () -> assertTrue(notice.contains("The change conflicts with main in RelayServer.java."), notice),
                                () -> assertTrue(notice.contains("Recommended: 2) Rebase onto main"), notice)
            );
        }

        @Test
        @DisplayName("maps a waiver request that arrives as decision.opened to the waiver notice")
        void waiverOpened() {
            var event = Map.<String, Object>of("decision", Map.of(
                                "id", "WR-2",
                                "subject", "plan fix-relay",
                                "is_waiver", true,
                                "waiver_gate", "ci_check:integration-tests"));

            var notice = ConsoleNotice.fromEventMap("en", "decision.opened", event);

            assertAll(
                                () -> assertNotNull(notice),
                                () -> assertTrue(notice.contains(
                                                    "◆ Waiver request WR-2 · plan fix-relay · gate ci_check:integration-tests"),
                                                    notice),
                                () -> assertTrue(notice.contains("answer WR-2"), notice)
            );
        }

        @Test
        @DisplayName("maps decision.answered from the web app to the answered-elsewhere notice")
        void decisionAnswered() {
            var event = Map.<String, Object>of(
                                "id", "Q-007",
                                "answer", "Rebase onto main",
                                "channel", "web",
                                "device", "iPad");

            var notice = ConsoleNotice.fromEventMap("en", "decision.answered", event);
            var ownConsole = ConsoleNotice.fromEventMap("en", "decision.answered", Map.of(
                                "id", "Q-008",
                                "answer", "Abort",
                                "channel", "cli"));

            assertAll(
                                () -> assertEquals("– Q-007 was answered in the web app (iPad): Rebase onto main. Nothing was sent from here.",
                                                    notice),
                                () -> assertNull(ownConsole)
            );
        }

        @Test
        @DisplayName("maps hold.opened to the notice of the hold's kind")
        void holdOpened() {
            var event = Map.<String, Object>of(
                                "hold_id", "h-1",
                                "subject_id", "fix-relay",
                                "subject_kind", "plan",
                                "kind", "stalled",
                                "cause", "worker died");

            var notice = ConsoleNotice.fromEventMap("en", "hold.opened", event);

            assertEquals("◆ Plan fix-relay stalled: worker died", notice);
        }

        @Test
        @DisplayName("maps landing.failed to the landing-failed notice")
        void landingFailed() {
            var event = Map.<String, Object>of("epic", "storage-v2", "reason", "git conflict");

            var notice = ConsoleNotice.fromEventMap("en", "landing.failed", event);

            assertEquals("◆ Epic storage-v2: the landing failed: git conflict", notice);
        }

        @Test
        @DisplayName("maps report.sent from the report payload to the quiet report line")
        void reportSent() {
            var event = Map.<String, Object>of("report", Map.of(
                                "id", "rep-101",
                                "from", "pm-mcp-clients > relay-hardening > fix-relay",
                                "subject", "merged (#214)"));

            var notice = ConsoleNotice.fromEventMap("en", "report.sent", event);

            assertEquals("▪ fix-relay: merged (#214) · reports open rep-101", notice);
        }

        @Test
        @DisplayName("keeps the type field as the fallback of an unnamed or unknown event")
        void typeFallback() {
            var unnamed = ConsoleNotice.fromEventMap("en", null, Map.of(
                                "type", "stalled", "scope", "fix-relay", "reason", "worker died"));
            var wait = ConsoleNotice.fromEventMap("en", null, Map.of(
                                "type", "operator_wait", "scope", "fix-relay", "detail", "token approval"));
            var unknownName = ConsoleNotice.fromEventMap("en", "task.off_track", Map.of(
                                "type", "question", "id", "Q-001", "scope", "plan fix-relay"));

            assertAll(
                                () -> assertEquals("◆ Plan fix-relay stalled: worker died", unnamed),
                                () -> assertEquals("◆ Plan fix-relay waits for you: token approval", wait),
                                () -> assertNotNull(unknownName),
                                () -> assertTrue(unknownName.contains("◆ Question Q-001 · plan fix-relay"), unknownName)
            );
        }

        @Test
        @DisplayName("prints no notice for an event with no notice kind")
        void noNoticeForUnmappedEvents() {
            assertAll(
                                () -> assertNull(ConsoleNotice.fromEventMap("en", "task.off_track", Map.of())),
                                () -> assertNull(ConsoleNotice.fromEventMap("en", "hold.cleared", Map.of("hold_id", "h-1")))
            );
        }
    }
}
