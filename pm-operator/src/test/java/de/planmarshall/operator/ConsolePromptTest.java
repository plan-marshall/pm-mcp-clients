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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ConsolePrompt")
class ConsolePromptTest {

    @Nested
    @DisplayName("prompt formatting")
    class PromptFormatting {

        @Test
        @DisplayName("renders default prompt when badges are null or empty")
        void defaultPrompt() {
            assertAll(
                                                  () -> assertEquals("pm › ", ConsolePrompt.format(null, false)),
                                                  () -> assertEquals("pm › ", ConsolePrompt.format(Collections.emptyList(), false))
            );
        }

        @Test
        @DisplayName("renders reconnecting prompt when reconnecting")
        void reconnectingPrompt() {
            assertAll(
                                                  () -> assertEquals("pm (reconnecting) › ", ConsolePrompt.format(null, true)),
                                                  () -> assertEquals("pm (reconnecting) › ", ConsolePrompt.format(List.of("1 question"), true))
            );
        }

        @Test
        @DisplayName("renders single badge")
        void singleBadge() {
            var prompt = ConsolePrompt.format(List.of("1 question"), false);
            assertEquals("pm [1 question] › ", prompt);
        }

        @Test
        @DisplayName("renders two and three badges joined by middle dot")
        void twoAndThreeBadges() {
            var two = ConsolePrompt.format(List.of("1 question", "setup 3/7"), false);
            var three = ConsolePrompt.format(List.of("2 questions", "1 waiver request", "setup 3/7"), false);

            assertAll(
                                                  () -> assertEquals("pm [1 question · setup 3/7] › ", two),
                                                  () -> assertEquals("pm [2 questions · 1 waiver request · setup 3/7] › ", three)
            );
        }

        @Test
        @DisplayName("truncates more than three badges to first three and plus remainder count")
        void truncateMoreThanThreeBadges() {
            var badges = List.of(
                                                  "2 questions",
                                                  "1 waiver request",
                                                  "1 stalled",
                                                  "1 needs a fix",
                                                  "setup 3/7"
            );
            var prompt = ConsolePrompt.format(badges, false);
            assertEquals("pm [2 questions · 1 waiver request · 1 stalled · +2] › ", prompt);
        }
    }

    @Nested
    @DisplayName("badge kind formatting")
    class BadgeKindFormatting {

        @Test
        @DisplayName("formats question badges singular and plural")
        void questionBadges() {
            assertAll(
                                                  () -> assertEquals("1 question", ConsolePrompt.formatBadge("question", 1)),
                                                  () -> assertEquals("1 question", ConsolePrompt.formatBadge("decision", 1)),
                                                  () -> assertEquals("3 questions", ConsolePrompt.formatBadge("question", 3)),
                                                  () -> assertEquals("2 questions", ConsolePrompt.formatBadge("decision", 2))
            );
        }

        @Test
        @DisplayName("formats waiver request badges singular and plural")
        void waiverRequestBadges() {
            assertAll(
                                                  () -> assertEquals("1 waiver request", ConsolePrompt.formatBadge("waiver_request", 1)),
                                                  () -> assertEquals("4 waiver requests", ConsolePrompt.formatBadge("waiver_request", 4))
            );
        }

        @Test
        @DisplayName("formats operator wait badges")
        void operatorWaitBadges() {
            assertAll(
                                                  () -> assertEquals("1 waits for you", ConsolePrompt.formatBadge("operator_wait", 1)),
                                                  () -> assertEquals("2 wait for you", ConsolePrompt.formatBadge("operator_wait", 2))
            );
        }

        @Test
        @DisplayName("formats integrity pause badges")
        void integrityPauseBadges() {
            assertAll(
                                                  () -> assertEquals("1 integrity pause", ConsolePrompt.formatBadge("integrity_pause", 1)),
                                                  () -> assertEquals("2 integrity pauses", ConsolePrompt.formatBadge("integrity_pause", 2))
            );
        }

        @Test
        @DisplayName("formats hold and status badges")
        void holdBadges() {
            assertAll(
                                                  () -> assertEquals("1 stalled", ConsolePrompt.formatBadge("stalled", 1)),
                                                  () -> assertEquals("2 needs a fix", ConsolePrompt.formatBadge("condition", 2)),
                                                  () -> assertEquals("1 held", ConsolePrompt.formatBadge("held", 1)),
                                                  () -> assertEquals("1 landing failed", ConsolePrompt.formatBadge("landing_failed", 1)),
                                                  () -> assertEquals("3 new findings", ConsolePrompt.formatBadge("findings", 3)),
                                                  () -> assertEquals("1 setup", ConsolePrompt.formatBadge("setup", 1)),
                                                  () -> assertEquals("2 custom", ConsolePrompt.formatBadge("custom", 2))
            );
        }

        @Test
        @DisplayName("returns null when count is zero or negative")
        void zeroOrNegativeCount() {
            assertAll(
                                                  () -> assertNull(ConsolePrompt.formatBadge("question", 0)),
                                                  () -> assertNull(ConsolePrompt.formatBadge("question", -1))
            );
        }

        @Test
        @DisplayName("formats machine setup badge")
        void machineSetupBadge() {
            assertEquals("setup 3/7", ConsolePrompt.formatMachineSetupBadge(3, 7));
        }

        @Test
        @DisplayName("formats project setup badge")
        void projectSetupBadge() {
            assertEquals("setup pm-mcp-clients 5/9", ConsolePrompt.formatProjectSetupBadge("pm-mcp-clients", 5, 9));
        }
    }
}
