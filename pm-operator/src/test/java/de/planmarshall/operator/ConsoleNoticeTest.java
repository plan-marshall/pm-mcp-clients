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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.planmarshall.api.event.EventKind;
import de.planmarshall.api.message.MessageCatalogue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@EnableGeneratorController
@DisplayName("ConsoleNotice")
class ConsoleNoticeTest {

    /** The console's prompt marker a forged line would imitate. */
    private static final String PROMPT_MARKER = "pm ›";
    /** The indent of the catalogue's question body template. */
    private static final String BODY_INDENT = "  ";

    @Nested
    @DisplayName("untrusted line breaks cannot forge a prompt line")
    class ForgedPromptLines {

        @Test
        @DisplayName("prints every line of a multi-line question body behind the indent")
        void questionBodyLinesAreIndented() {
            var first = Generators.letterStrings(3, 12).next();
            var forged = Generators.letterStrings(3, 12).next();
            var text = first + "\n" + PROMPT_MARKER + " " + forged + "\n" + PROMPT_MARKER;

            var notice = ConsoleNotice.formatQuestion("Q-007", "plan fix-relay", null, text, null);
            var lines = notice.split("\n");

            assertAll(
                                () -> assertTrue(notice.contains("\n" + BODY_INDENT + first + "\n"), notice),
                                () -> assertTrue(notice.contains("\n" + BODY_INDENT + PROMPT_MARKER + " " + forged + "\n"), notice),
                                () -> assertTrue(notice.contains("\n" + BODY_INDENT + PROMPT_MARKER + "\n"), notice),
                                () -> assertTrue(List.of(lines).stream().noneMatch(line -> line.startsWith(PROMPT_MARKER)), notice)
            );
        }

        @Test
        @DisplayName("keeps a line break of the summary and the recommendation on the notice's own line")
        void summaryAndRecommendationStayOnOneLine() {
            var summary = Generators.letterStrings(3, 12).next();
            var option = Generators.letterStrings(3, 12).next();

            var notice = ConsoleNotice.formatQuestion("Q-007", "plan fix-relay",
                                summary + "\n" + PROMPT_MARKER + " x", "body", option + "\n" + PROMPT_MARKER + " y");
            var lines = notice.split("\n");

            assertAll(
                                () -> assertEquals(4, lines.length, notice),
                                () -> assertTrue(lines[0].endsWith(" · " + summary + " " + PROMPT_MARKER + " x"), notice),
                                () -> assertEquals(BODY_INDENT + "Recommended: " + option + " " + PROMPT_MARKER + " y", lines[2], notice),
                                () -> assertTrue(List.of(lines).stream().noneMatch(line -> line.startsWith(PROMPT_MARKER)), notice)
            );
        }

        @Test
        @DisplayName("keeps a line break of a hold's detail on the notice's own line")
        void holdDetailStaysOnOneLine() {
            var detail = Generators.letterStrings(3, 12).next();

            var notice = ConsoleNotice.formatHoldNotice("stalled", "fix-relay", detail + "\n" + PROMPT_MARKER + " z");

            assertEquals("◆ Plan fix-relay stalled: " + detail + " " + PROMPT_MARKER + " z", notice);
        }
    }

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
                                () -> assertTrue(notice.contains("◆ Waiver request WR-2 · plan fix-relay · gate CI check · integration-tests")),
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
                                () -> assertEquals("◆ another reason · plan-x: detail",
                                                    ConsoleNotice.formatHoldNotice("custom", "plan-x", "detail"))
            );
        }

        @Test
        @DisplayName("names a kind it has no notice for by the catalogue's word of the other kind, never the wire word")
        void otherKindNamedByTheCatalogue() {
            var notice = ConsoleNotice.formatHoldNotice("quota_exhausted", "plan-x", "detail");

            assertAll(
                                () -> assertEquals("◆ another reason · plan-x: detail", notice),
                                () -> assertFalse(notice.contains("quota_exhausted"), notice)
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
                                // One check mark: the catalogue line carries its own, and the notice adds none.
                                () -> assertEquals("✓ New code: KXQB-MRTN, valid until 14:32. The previous code no longer works.",
                                                    notice.lines().findFirst().orElseThrow()),
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

            var wrNotice = ConsoleNotice.fromEventMap(wrEvent);

            assertAll(
                                () -> assertNotNull(ConsoleNotice.fromEventMap(qEvent)),
                                () -> assertNotNull(wrNotice),
                                () -> assertTrue(wrNotice.contains("◆ Waiver request WR-1 · plan fix-relay"), wrNotice)
            );
        }

        @Test
        @DisplayName("reads the legacy type waiver_request as the waiver kind")
        void legacyWaiverTypeIsTheWaiverKind() {
            assertAll(
                                () -> assertEquals(EventKind.WAIVER, EventKind.fromType("waiver_request")),
                                () -> assertEquals("waiver", EventKind.WAIVER.notice())
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
        @DisplayName("a paired event without a label names the device unknown, never null")
        void pairedWithoutLabel() {
            var withNull = new HashMap<String, Object>();
            withNull.put("type", "paired");
            withNull.put("label", null);
            var unknown = MessageCatalogue.format("en", "common.unknown", Map.of());

            assertAll(
                                () -> assertEquals("✓ Paired: " + unknown, ConsoleNotice.fromEventMap(withNull)),
                                () -> assertEquals("✓ Paired: " + unknown,
                                                    ConsoleNotice.fromEventMap(Map.of("type", "paired", "label", "  ")))
            );
        }

        @Test
        @DisplayName("a fresh code event that names no code raises no notice instead of a code null")
        void freshCodeWithoutCode() {
            assertAll(
                                () -> assertNull(ConsoleNotice.fromEventMap(Map.of("type", "fresh_code", "valid_until", "15:00"))),
                                () -> assertNull(ConsoleNotice.fromEventMap(Map.of("type", "fresh_code", "code", " ")))
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
                                                                        "◆ Waiver request WR-2 · plan fix-relay · gate CI check · integration-tests"),
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
            var unnamedWaiver = ConsoleNotice.fromEventMap("en", null, Map.of(
                                "type", "decision", "id", "WR-2", "scope", "plan fix-relay",
                                "is_waiver", true, "waiver_gate", "sonar"));

            assertAll(
                                () -> assertEquals("◆ Plan fix-relay stalled: worker died", unnamed),
                                () -> assertEquals("◆ Plan fix-relay waits for you: token approval", wait),
                                () -> assertNotNull(unknownName),
                                () -> assertTrue(unknownName.contains("◆ Question Q-001 · plan fix-relay"), unknownName),
                                () -> assertNotNull(unnamedWaiver),
                                () -> assertTrue(unnamedWaiver.contains("◆ Waiver request WR-2"), unnamedWaiver)
            );
        }

        @Test
        @DisplayName("names an epic's operator wait as the epic's, from the hold and from the envelope")
        void epicOperatorWait() {
            var fromHold = ConsoleNotice.fromEventMap("en", "hold.opened", Map.of(
                                "hold_id", "h-2",
                                "subject_id", "storage-v2",
                                "subject_kind", "epic",
                                "kind", "operator_wait",
                                "detail", "the decomposition"));
            var fromEnvelope = ConsoleNotice.fromEventMap("en", null, Map.of(
                                "event", "hold.opened",
                                "scope", Map.of("kind", "epic", "id", "storage-v2"),
                                "data", Map.of("kind", "operator_wait", "detail", "the decomposition")));
            var german = ConsoleNotice.fromEventMap("de", "hold.opened", Map.of(
                                "subject_id", "storage-v2",
                                "subject_kind", "epic",
                                "kind", "operator_wait",
                                "detail", "die Zerlegung"));
            var plan = ConsoleNotice.fromEventMap("en", "hold.opened", Map.of(
                                "subject_id", "fix-relay",
                                "subject_kind", "plan",
                                "kind", "operator_wait",
                                "detail", "token approval"));

            assertAll(
                                () -> assertEquals("◆ Epic storage-v2 waits for you: the decomposition", fromHold),
                                () -> assertEquals("◆ Epic storage-v2 waits for you: the decomposition", fromEnvelope),
                                () -> assertEquals("◆ Epic storage-v2 wartet auf dich: die Zerlegung", german),
                                () -> assertEquals("◆ Plan fix-relay waits for you: token approval", plan)
            );
        }

        @Test
        @DisplayName("takes the scope the envelope declares when the data names none")
        void envelopeScope() {
            var byId = ConsoleNotice.fromEventMap("en", null, Map.of(
                                "event", "hold.opened",
                                "scope", Map.of("kind", "plan", "id", "fix-relay"),
                                "data", Map.of("kind", "stalled", "reason", "worker died")));
            var byProject = ConsoleNotice.fromEventMap("en", null, Map.of(
                                "event", "hold.opened",
                                "scope", Map.of("kind", "project", "project_id", "pm-mcp-clients"),
                                "data", Map.of("kind", "setup", "detail", "a credential")));
            var byTopLevelProject = ConsoleNotice.fromEventMap("en", null, Map.of(
                                "type", "setup", "project", "pm-mcp-clients", "detail", "a credential"));
            var none = ConsoleNotice.fromEventMap("en", null, Map.of(
                                "event", "hold.opened",
                                "scope", Map.of("kind", "machine"),
                                "data", Map.of("kind", "cleanup_due")));

            assertAll(
                                () -> assertEquals("◆ Plan fix-relay stalled: worker died", byId),
                                () -> assertEquals("◆ Project pm-mcp-clients: the setup needs you: a credential", byProject),
                                () -> assertEquals("◆ Project pm-mcp-clients: the setup needs you: a credential",
                                                    byTopLevelProject),
                                () -> assertEquals("◆ Cleanup is due", none)
            );
        }

        @Test
        @DisplayName("names a credential by its key, by its link, or as unknown")
        void credentialKey() {
            var byLink = ConsoleNotice.fromEventMap("en", null, Map.of(
                                "type", "credential", "link", "/api/v1/credentials/sonar"));
            var byDetail = ConsoleNotice.fromEventMap("en", null, Map.of(
                                "type", "credential", "detail", "github"));
            var unnamed = ConsoleNotice.fromEventMap("en", null, Map.of(
                                "type", "credential", "link", "/api/v1/projects/pm-mcp-clients"));

            assertAll(
                                () -> assertEquals("◆ Credential sonar needs attention", byLink),
                                () -> assertEquals("◆ Credential github needs attention", byDetail),
                                () -> assertEquals("◆ Credential unknown needs attention", unnamed)
            );
        }

        @Test
        @DisplayName("shows a channel, a blocking kind and a recommendation without a catalogue text as sent")
        void valuesWithoutCatalogueText() {
            var answered = ConsoleNotice.fromEventMap("en", "decision.answered", Map.of(
                                "id", "Q-007", "answer", "Rebase", "channel", "mobile"));
            var question = ConsoleNotice.fromEventMap("en", "decision.opened", Map.of("decision", Map.of(
                                "id", "Q-008",
                                "subject", "epic storage-v2",
                                "blocks", "landing-train",
                                "recommendation", "rebase",
                                "parts", List.of(Map.of(
                                                    "recommendation", "3",
                                                    "options", List.of(Map.of("key", "1", "label", "Merge")))))));
            var noAnswerChannel = ConsoleNotice.fromEventMap("en", "decision.answered", Map.of(
                                "id", "Q-009", "answer", "Merge"));

            assertAll(
                                () -> assertEquals("– Q-007 was answered in the mobile: Rebase. Nothing was sent from here.",
                                                    answered),
                                () -> assertNotNull(question),
                                () -> assertTrue(question.contains("◆ Question Q-008 · epic storage-v2 · landing-train"), question),
                                () -> assertTrue(question.contains("Recommended: rebase"), question),
                                () -> assertNull(noAnswerChannel)
            );
        }

        @Test
        @DisplayName("takes the last step of a report's origin and names the place of many")
        void reportOrigins() {
            var arrow = ConsoleNotice.fromEventMap("en", "report.sent", Map.of(
                                "from", "pm-mcp-clients › fix-relay", "subject", "merged"));
            var project = ConsoleNotice.fromEventMap("en", "report.sent", Map.of(
                                "project", "pm-mcp-clients", "summary", "done"));
            var many = ConsoleNotice.fromEventMap("en", "report.sent", Map.of(
                                "count", 4, "scope", "relay-hardening"));
            var one = ConsoleNotice.fromEventMap("en", "report.sent", Map.of(
                                "count", 1, "from", "fix-relay", "subject", "merged"));

            assertAll(
                                () -> assertEquals("▪ fix-relay: merged", arrow),
                                () -> assertEquals("▪ pm-mcp-clients: done", project),
                                () -> assertEquals("▪ 4 new reports · relay-hardening · reports", many),
                                () -> assertEquals("▪ fix-relay: merged", one)
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
