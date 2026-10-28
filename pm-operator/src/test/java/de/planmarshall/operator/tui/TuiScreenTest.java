/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.tui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * What one frame of a written screen shows: every row of it is cleared, so a level change leaves no
 * text of the level before it (CLI-UX-S-01). The case drives the real driver on an in-memory
 * terminal, because the residue is a property of what the terminal ends up showing, not of the
 * lines the layout returns.
 */
@DisplayName("TuiScreen")
class TuiScreenTest {

    private static final int COLUMNS = 80;
    private static final int ROWS = 24;

    @Test
    @DisplayName("a level change leaves no text of the previous level on the screen")
    void levelChangeLeavesNoResidue() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(COLUMNS, ROWS)) {
            var overview = new TuiScreen("Running work \u00b7 projects", TuiLevel.Kind.OVERVIEW,
                                List.of(TuiRow.openable("project-a", "OVERVIEW-ROW-a project",
                                                    "details of the project row"),
                                                    TuiRow.openable("project-b", "OVERVIEW-ROW-b project",
                                                                        "details of the second project")),
                                0, "", false, true, 7);
            var activity = new TuiScreen("fix-relay \u203a TASK-003", TuiLevel.Kind.PLAN_ACTIVITY,
                                List.of(TuiRow.openable("step-a", "ACTIVITY-ROW-a step",
                                                    "details of the activity row")),
                                0, "", false, true, 0);
            var shown = new AtomicReference<>(overview);
            var thread = new Thread(() -> {
                try {
                    new TuiScreenDriver(virtual.terminal(), "en").run(shown::get, action -> {
                        if (action == TuiKeyAction.MOVE_DOWN) {
                            shown.set(activity);
                        }
                        return action != TuiKeyAction.LEAVE;
                    });
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
            thread.start();
            await(() -> virtual.alternateScreenActive()
                                && String.join("\n", virtual.screen().lines()).contains("OVERVIEW-ROW-a"));

            var before = String.join("\n", virtual.screen().lines());
            assertTrue(before.contains("E ended work"), before);

            virtual.type("j");
            await(() -> String.join("\n", virtual.screen().lines()).contains("ACTIVITY-ROW-a"));

            var after = String.join("\n", virtual.screen().lines());
            assertFalse(after.contains("OVERVIEW-ROW"), "the previous level's rows remain:\n" + after);
            assertFalse(after.contains("E ended work"), "the previous footer's tail remains:\n" + after);
            assertFalse(after.contains("filter"), "a word of the previous footer remains:\n" + after);

            virtual.type("q");
            thread.join(5_000L);
            assertFalse(thread.isAlive(), "the driver did not leave the view");
        }
    }

    private static void await(BooleanSupplier condition) throws InterruptedException {
        var deadline = System.nanoTime() + 5_000_000_000L;
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                fail("the view did not reach the expected state in time");
            }
            Thread.sleep(20L);
        }
    }
}
