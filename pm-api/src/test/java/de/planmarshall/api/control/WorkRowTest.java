/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.control;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * A row of an epic's work list: which statuses lock it against the decomposition, the defaults of a
 * row that names only its plan, and the change size it carries.
 */
@DisplayName("WorkRow")
@EnableGeneratorController
class WorkRowTest {

    private static WorkRow row(String status) {
        return new WorkRow(Generators.nonBlankStrings().next(), null, status, null, null, null, null, null, null,
                null, null, null, null);
    }

    @ParameterizedTest(name = "a {0} row is locked: {1}")
    @CsvSource({"running, true", "pausing, true", "paused, true", "shipped, true", "waiting for you, true",
            "held, true", "staged, false", "ready, false"})
    @DisplayName("should lock a row that runs, waits, is held or has shipped")
    void shouldLockByStatus(String status, boolean locked) {
        assertEquals(locked, row(status).isLocked());
    }

    @Test
    @DisplayName("should take the plan id as the title, a blank status as staged and absent lists as empty")
    void shouldDefaultAbsentMembers() {
        var row = row(" ".repeat(Generators.integers(0, 3).next()));

        assertAll(
                () -> assertEquals(row.planId(), row.title()),
                () -> assertEquals("staged", row.status()),
                () -> assertTrue(row.overlapFiles().isEmpty()),
                () -> assertTrue(row.deliverables().isEmpty()),
                () -> assertTrue(row.surface().isEmpty()),
                () -> assertTrue(row.dependencies().isEmpty()));
    }

    @Test
    @DisplayName("should carry the change size of a row through its JSON")
    void shouldCarryTheChange() throws Exception {
        var change = "+" + Generators.integers(1, 999).next() + " -" + Generators.integers(1, 999).next();
        var row = new WorkRow(Generators.nonBlankStrings().next(), null, "staged", null, null, null, null, null,
                change, null, null, null, null);

        assertEquals(change, WorkRow.fromJson(row.toJson()).change());
    }

    @Test
    @DisplayName("should refuse JSON that is no object")
    void shouldRefuseJsonThatIsNoObject() {
        var json = JsonTree.write(List.of(Generators.nonBlankStrings().next()));

        assertThrows(JsonFormatException.class, () -> WorkRow.fromJson(json));
    }
}
