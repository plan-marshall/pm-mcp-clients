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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map;

import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.system.CleanupDurations;
import de.planmarshall.api.system.CleanupKind;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The durations {@code cleanup} prints are one mapping from {@link CleanupKind} to the word shown and
 * the duration it reads, and that mapping is total: a constant the mapping leaves out turns this
 * class red instead of printing a missing column.
 */
@DisplayName("cleanup durations: one mapping over CleanupKind")
class CleanupDurationsKindsTest {

    @Test
    @DisplayName("every constant of CleanupKind carries a label and its own duration")
    void everyKindHasALabelAndADuration() {
        var displays = CleanupCommand.kindDisplays();
        // One distinct figure per kind, so a mapping that reads another kind's duration shows up as
        // a column carrying the wrong number rather than passing on a shared default.
        var figures = new CleanupDurations(11, 12, 13, 14, 15, 3);

        assertAll(
                            () -> assertEquals(EnumSet.allOf(CleanupKind.class), displays.keySet()),
                            () -> assertTrue(displays.values().stream().allMatch(display -> !display.label().isBlank())),
                            () -> assertEquals(EnumSet.allOf(CleanupKind.class).size(),
                                                new LinkedHashSet<>(displays.values().stream()
                                                                    .map(display -> display.days().applyAsInt(figures))
                                                                    .toList()).size(),
                                                "each kind reads a duration of its own")
        );
    }

    @Test
    @DisplayName("the labels are distinct, so one kind never stands in for another")
    void theLabelsAreDistinct() {
        var labels = CleanupCommand.kindDisplays().values().stream()
                            .map(CleanupCommand.KindDisplay::label).toList();

        assertEquals(labels.size(), new LinkedHashSet<>(labels).size(), labels.toString());
    }

    @Test
    @DisplayName("the rendered durations name one column per constant, each with its own figure")
    void theRenderedDurationsCarryOneColumnPerKind() {
        var differing = new CleanupDurations(31, 32, 33, 34, 35, 3);
        // Keyed by the constant rather than by position, so a reordering cannot re-pair a label and
        // a figure without the assertion naming it.
        var figures = Map.of(CleanupKind.PLAN, 31, CleanupKind.EPIC, 32, CleanupKind.LOGS, 33,
                            CleanupKind.READ_REPORTS, 34, CleanupKind.UNENROLLED_PROJECT, 35);

        var text = CleanupCommand.durationsText("en", differing);

        var expected = new LinkedHashSet<String>();
        var displays = CleanupCommand.kindDisplays();
        for (var kind : CleanupKind.values()) {
            var label = displays.get(kind).label();
            expected.add(MessageCatalogue.format("en", "system.cleanup.durations.kind", Map.of("kind", label))
                                + " " + figures.get(kind) + " days");
        }
        assertAll(
                            () -> assertEquals(expected.size(), CleanupKind.values().length),
                            () -> expected.forEach(column -> assertTrue(text.contains(column),
                                                column + " is missing from: " + text))
        );
    }
}
