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
    @DisplayName("every constant of CleanupKind carries a label and a duration")
    void everyKindHasALabelAndADuration() {
        var displays = CleanupCommand.kindDisplays();

        assertAll(
                            () -> assertEquals(EnumSet.allOf(CleanupKind.class), displays.keySet()),
                            () -> assertTrue(displays.values().stream().allMatch(display -> !display.label().isBlank())),
                            () -> assertTrue(displays.values().stream()
                                                                    .allMatch(display -> display.days().applyAsInt(
                                                                                        CleanupDurations.defaults()) >= 0),
                                                displays.toString())
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

        var text = CleanupCommand.durationsText("en", differing);

        var expected = new LinkedHashSet<String>();
        var displays = CleanupCommand.kindDisplays();
        var figures = new int[]{31, 32, 33, 34, 35};
        var index = 0;
        for (var kind : CleanupKind.values()) {
            var label = displays.get(kind).label();
            expected.add(MessageCatalogue.format("en", "system.cleanup.durations.kind", Map.of("kind", label))
                                + " " + figures[index] + " days");
            index++;
        }
        assertAll(
                            () -> assertEquals(expected.size(), CleanupKind.values().length),
                            () -> expected.forEach(column -> assertTrue(text.contains(column),
                                                column + " is missing from: " + text))
        );
    }
}
