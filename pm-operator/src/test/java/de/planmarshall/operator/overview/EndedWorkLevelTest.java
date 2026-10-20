/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.overview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import de.planmarshall.api.overview.EndedWorkItem;
import de.planmarshall.api.overview.EndedWorkPage;
import de.planmarshall.operator.tui.TuiLevel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EndedWorkLevel")
class EndedWorkLevelTest {

    private final Instant baseInstant = Instant.parse("2026-10-20T12:00:00Z");
    private final Clock clock = Clock.fixed(baseInstant, ZoneOffset.UTC);

    @Test
    @DisplayName("empty ended work page returns non-selectable empty row")
    void emptyPage() {
        var page = new EndedWorkPage("proj-a", List.of(), 1, 1, false, false);
        var level = new EndedWorkLevel(page);
        var rows = level.rows("en", clock, false);

        assertEquals(1, rows.size());
        assertEquals(EndedWorkLevel.EMPTY_ENDED_ROW_ID, rows.getFirst().id());
        assertFalse(rows.getFirst().openable());
        assertTrue(rows.getFirst().line().contains("Nothing in proj-a has ended yet"));
    }

    @Test
    @DisplayName("level metadata contains project breadcrumb")
    void levelMetadata() {
        var page = new EndedWorkPage("proj-a", List.of(), 1, 1, false, false);
        var level = new EndedWorkLevel(page).level("en");

        assertEquals(TuiLevel.Kind.ENDED, level.kind());
        assertTrue(level.title().contains("Ended work"));
        assertTrue(level.title().contains("proj-a"));
    }

    @Test
    @DisplayName("ended plan and epic rows render outcomes, PR numbers and timestamps")
    void endedItemsFormatting() {
        var planItem = new EndedWorkItem("pln-1", "plan", "Plan Completed", "merged", null,
                            101, "https://github.com/org/repo/pull/101", "Merged successfully", baseInstant.minusSeconds(3600));
        var epicItem = new EndedWorkItem("epc-1", "epic", "Epic Done", null, "completed",
                            null, null, "All plans verified", baseInstant.minusSeconds(7200));

        var page = new EndedWorkPage("proj-a", List.of(planItem, epicItem), 1, 2, false, true);
        var level = new EndedWorkLevel(page);
        var rows = level.rows("en", clock, false);

        assertEquals(2, rows.size());

        var planRow = rows.get(0);
        assertEquals("pln-1", planRow.id());
        assertTrue(planRow.line().contains("▸ plan"));
        assertTrue(planRow.line().contains("Plan Completed"));
        assertTrue(planRow.line().contains("merged"));
        assertTrue(planRow.line().contains("#101"));
        assertNotNull(planRow.details());
        assertTrue(planRow.details().contains("Merged successfully"));
        assertFalse(planRow.details().contains("https://"));

        // With help visible: URL revealed
        var helpRows = level.rows("en", clock, true);
        assertTrue(helpRows.get(0).details().contains("https://github.com/org/repo/pull/101"));

        var epicRow = rows.get(1);
        assertEquals("epc-1", epicRow.id());
        assertTrue(epicRow.line().contains("◇ epic"));
        assertTrue(epicRow.line().contains("Epic Done"));
        assertTrue(epicRow.line().contains("completed"));
    }

    @Test
    @DisplayName("appendOlderPage advances pagination state and appends deduplicated items")
    void appendOlderPage() {
        var item1 = new EndedWorkItem("pln-1", "plan", "Plan 1", "merged", null, null, null, null, baseInstant.minusSeconds(3600));
        var item2 = new EndedWorkItem("pln-2", "plan", "Plan 2", "abandoned", null, null, null, null, baseInstant.minusSeconds(7200));

        var page1 = new EndedWorkPage("proj-a", List.of(item1), 1, 2, false, true);
        var level = new EndedWorkLevel(page1);

        assertEquals(1, level.currentPage());
        assertTrue(level.hasOlder());
        assertEquals(1, level.items().size());

        var page2 = new EndedWorkPage("proj-a", List.of(item1, item2), 2, 2, true, false);
        level.appendOlderPage(page2);

        assertEquals(2, level.currentPage());
        assertFalse(level.hasOlder());
        assertEquals(2, level.items().size());
        assertEquals("pln-2", level.items().get(1).id());
    }

    @Test
    @DisplayName("localizes outcomes in German and sanitizes raw reason tokens")
    void germanOutcomeLocalization() {
        var planItem = new EndedWorkItem("pln-1", "plan", "Plan Clean", "no-change", null,
                            null, null, null, baseInstant.minusSeconds(3600));
        var epicItem = new EndedWorkItem("epc-1", "epic", "Epic Cancelled", null, "cancelled",
                            null, null, null, baseInstant.minusSeconds(7200));

        var page = new EndedWorkPage("proj-a", List.of(planItem, epicItem), 1, 1, false, false);
        var level = new EndedWorkLevel(page);
        var rows = level.rows("de", clock, false);

        assertEquals(2, rows.size());
        assertTrue(rows.get(0).line().contains("ohne Änderung"));
        assertTrue(rows.get(1).line().contains("abgebrochen"));
    }

    @Test
    @DisplayName("refreshFirstPage preserves older items while updating page 1")
    void refreshFirstPagePreservesOlder() {
        var item1 = new EndedWorkItem("pln-1", "plan", "Plan 1", "merged", null, null, null, null, baseInstant.minusSeconds(3600));
        var item2 = new EndedWorkItem("pln-2", "plan", "Plan 2", "abandoned", null, null, null, null, baseInstant.minusSeconds(7200));

        var page1 = new EndedWorkPage("proj-a", List.of(item1), 1, 2, false, true);
        var level = new EndedWorkLevel(page1);

        var page2 = new EndedWorkPage("proj-a", List.of(item2), 2, 2, true, false);
        level.appendOlderPage(page2);
        assertEquals(2, level.items().size());

        // Refresh with fresh item 0 on page 1
        var item0 = new EndedWorkItem("pln-0", "plan", "Plan Fresh", "merged", null, null, null, null, baseInstant.minusSeconds(60));
        var freshPage1 = new EndedWorkPage("proj-a", List.of(item0, item1), 1, 2, false, true);
        level.refreshFirstPage(freshPage1);

        assertEquals(3, level.items().size());
        assertEquals("pln-0", level.items().get(0).id());
        assertEquals("pln-1", level.items().get(1).id());
        assertEquals("pln-2", level.items().get(2).id());
    }
}
