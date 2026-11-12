/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.mock.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;

@DisplayName("CleanupFixtures")
@EnableGeneratorController
class CleanupFixturesTest {

    @ParameterizedTest(name = "without {0}")
    @ValueSource(strings = {"item_id", "kind", "label"})
    @DisplayName("skips a kept entry that lacks one of its required members and keeps the complete one")
    void skipsAnIncompleteKeptEntry(String missing) {
        var complete = keptEntry();
        var incomplete = new LinkedHashMap<>(keptEntry());
        incomplete.remove(missing);

        var kept = CleanupFixtures.keptItems(List.of(incomplete, complete));

        assertEquals(1, kept.size(), "only the complete entry is kept");
        assertEquals(complete.get("item_id"), kept.getFirst().id());
        assertEquals(complete.get("label"), kept.getFirst().title());
    }

    @Test
    @DisplayName("skips a residue entry without a step and reads a missing count and size as zero")
    void residueDefaults() {
        var step = Generators.letterStrings(4, 10).next();
        var lastRun = Generators.letterStrings(4, 10).next();

        var residues = CleanupFixtures.residues(List.of(
                Map.of("count", Generators.integers(1, 9).next()),
                Map.of("step", step, "last_run", lastRun)));

        assertEquals(1, residues.size(), "the entry without a step is skipped");
        var residue = residues.getFirst();
        assertEquals(step, residue.step());
        assertEquals(0, residue.count());
        assertEquals(0L, residue.bytes());
        assertEquals(lastRun, residue.lastSweep());
    }

    @Test
    @DisplayName("reads the count and the size a residue entry names")
    void residueValues() {
        var count = Generators.integers(1, 99).next();
        var bytes = Generators.longs(1L, 1_000_000L).next();

        var residue = CleanupFixtures.residues(List.of(Map.of("step", "logs", "count", count, "bytes", bytes)))
                .getFirst();

        assertEquals(count, residue.count());
        assertEquals(bytes, residue.bytes());
        assertNull(residue.lastSweep());
    }

    private static Map<String, Object> keptEntry() {
        return Map.of(
                "item_id", Generators.letterStrings(4, 10).next(),
                "kind", "plan",
                "label", Generators.letterStrings(4, 10).next(),
                "reason", "ignored",
                "kept_at", "2 Oct",
                "kept_by", "the console");
    }
}
