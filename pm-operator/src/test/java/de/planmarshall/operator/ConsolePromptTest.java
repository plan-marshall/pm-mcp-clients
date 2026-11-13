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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.overview.NeedsOperatorKind;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@EnableGeneratorController
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
                                () -> assertEquals("1 waiver request", ConsolePrompt.formatBadge("waiver", 1)),
                                () -> assertEquals("4 waiver requests", ConsolePrompt.formatBadge("waiver", 4))
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
                                () -> assertEquals("2 another reason", ConsolePrompt.formatBadge("custom", 2))
            );
        }

        @Test
        @DisplayName("names a kind it has no badge for by the catalogue's word, never the wire word")
        void unknownKindBadgeUsesTheCatalogueWord() {
            var badge = ConsolePrompt.formatBadge("quota_exhausted", 1);

            assertAll(
                                () -> assertEquals("1 another reason", badge),
                                () -> assertFalse(badge.contains("quota_exhausted"), badge)
            );
        }

        @Test
        @DisplayName("names a kind that happens to name another catalogue key as another reason")
        void kindNamingAnotherKeyReadsAsAnotherReason() {
            var title = ConsolePrompt.formatBadge("title", 1);
            var generated = "zz" + Generators.letterStrings(6, 12).next();
            var unknown = ConsolePrompt.formatBadge(generated, 1);

            assertAll(
                                () -> assertEquals("1 another reason", title),
                                () -> assertFalse(title.contains("What waits"), title),
                                () -> assertEquals("1 another reason", unknown),
                                () -> assertFalse(unknown.contains(generated), unknown)
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

        @Test
        @DisplayName("a project name's control sequence never reaches the prompt")
        void projectSetupBadgeIsClean() {
            var badge = ConsolePrompt.formatProjectSetupBadge("cui\u001b[31mparent", 2, 10);

            assertAll(
                                () -> assertFalse(badge.contains("\u001b"), badge),
                                () -> assertTrue(badge.contains("parent"), badge)
            );
        }
    }

    /**
     * The badge key is derived from {@link NeedsOperatorKind}: every word the enum reads as a kind
     * reads that kind's badge, so the badges cannot drift from the contract's kinds and aliases.
     */
    @Nested
    @DisplayName("badge key derived from the contract's kind")
    class BadgeKeyDerivation {

        @Test
        @DisplayName("the alias waiver_request reads the waiver badge in both languages")
        void waiverRequestReadsTheWaiverBadge() {
            assertAll(
                                () -> assertEquals(ConsolePrompt.badgeKey("waiver"), ConsolePrompt.badgeKey("waiver_request")),
                                () -> assertEquals("2 waiver requests", ConsolePrompt.formatBadge("en", "waiver_request", 2)),
                                () -> assertEquals(ConsolePrompt.formatBadge("en", "waiver", 1),
                                                    ConsolePrompt.formatBadge("en", "waiver_request", 1)),
                                () -> assertEquals(ConsolePrompt.formatBadge("en", "waiver", 2),
                                                    ConsolePrompt.formatBadge("en", "waiver_request", 2)),
                                () -> assertEquals(ConsolePrompt.formatBadge("de", "waiver", 1),
                                                    ConsolePrompt.formatBadge("de", "waiver_request", 1)),
                                () -> assertEquals(ConsolePrompt.formatBadge("de", "waiver", 2),
                                                    ConsolePrompt.formatBadge("de", "waiver_request", 2))
            );
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = NeedsOperatorKind.class, mode = EnumSource.Mode.EXCLUDE, names = "OTHER")
        @DisplayName("every kind of the contract has a badge of its own, by its wire word and by its name")
        void everyKindHasItsOwnBadge(NeedsOperatorKind kind) {
            assertAll(
                                () -> assertNotEquals(ConsolePrompt.BADGE_FALLBACK, ConsolePrompt.badgeKey(kind.value()),
                                                    kind.value()),
                                () -> assertEquals(ConsolePrompt.badgeKey(kind.value()), ConsolePrompt.badgeKey(kind.name()),
                                                    kind.name())
            );
        }

        @Test
        @DisplayName("a wire word in another case or with surrounding blanks reads the badge of the canonical word")
        void caseAndBlanksReadTheCanonicalBadge() {
            var kinds = Arrays.stream(NeedsOperatorKind.values())
                                .filter(kind -> kind != NeedsOperatorKind.OTHER)
                                .toArray(NeedsOperatorKind[]::new);
            var canonical = Generators.fixedValues(kinds).next().value();
            var cased = new StringBuilder();
            for (var letter : canonical.toCharArray()) {
                cased.append(Generators.booleans().next() ? Character.toUpperCase(letter) : letter);
            }
            var blanks = " ".repeat(Generators.integers(1, 3).next());
            var variant = blanks + cased + blanks;

            assertAll(
                                () -> assertEquals(ConsolePrompt.badgeKey(canonical), ConsolePrompt.badgeKey(variant), variant),
                                () -> assertEquals(ConsolePrompt.formatBadge("en", canonical, 2),
                                                    ConsolePrompt.formatBadge("en", variant, 2), variant),
                                () -> assertEquals(ConsolePrompt.formatBadge("de", canonical, 2),
                                                    ConsolePrompt.formatBadge("de", variant, 2), variant)
            );
        }
    }
}
