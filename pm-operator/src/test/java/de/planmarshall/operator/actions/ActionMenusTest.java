/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import de.planmarshall.api.control.ActionAvailability;
import de.planmarshall.api.control.HoldRecord;
import de.planmarshall.api.control.WorkActions;
import de.planmarshall.operator.tui.TuiMenuEntry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ActionMenus")
class ActionMenusTest {

    private static WorkActions actions(String kind, String state, String holdKind, String... offered) {
        var list = new ArrayList<ActionAvailability>();
        for (var action : offered) {
            list.add(ActionAvailability.enabled(action));
        }
        return new WorkActions("subject", kind, state, "task", holdKind, "epic-1", list);
    }

    private static HoldRecord hold(String... acts) {
        return new HoldRecord("H-1", "subject", "plan", "condition", "the plan reads cui-http", 3,
                            Instant.parse("2026-10-16T10:14:00Z"), null, null, null, null, null,
                            List.of(acts), null);
    }

    @Test
    @DisplayName("a running plan offers Pause, Cancel, Chat, and Settings, in that order")
    void runningPlan() {
        var menu = ActionMenus.plan("en", actions("plan", "running", null, "pause", "cancel", "chat",
                            "settings"), null, null, "row");
        var labels = menu.entries().stream().map(TuiMenuEntry::label).toList();
        assertEquals(4, labels.size());
        assertTrue(labels.get(0).startsWith("Pause"), labels.toString());
        assertEquals("\u2026", labels.get(0).substring(labels.get(0).length() - 1));
        assertTrue(labels.get(1).startsWith("Cancel"));
        assertTrue(labels.get(2).startsWith("Chat"));
        assertTrue(labels.get(3).startsWith("Settings"));
        assertEquals("p", menu.entries().get(0).trigger());
        assertEquals("x", menu.entries().get(1).trigger());
        assertEquals("h", menu.entries().get(2).trigger());
        assertEquals("o", menu.entries().get(3).trigger());
        assertEquals(ActionsMenu.Subject.PLAN, menu.subject());
    }

    @Test
    @DisplayName("a paused plan offers Start and Abandon; a pausing one Start and Cancel")
    void pausedAndPausing() {
        var paused = ActionMenus.plan("en", actions("plan", "paused", null, "start", "abandon", "chat",
                            "settings"), null, null, null);
        assertEquals("s", paused.entries().get(0).trigger());
        assertTrue(paused.entries().get(0).label().startsWith("Start"));
        assertEquals("x", paused.entries().get(1).trigger());
        assertTrue(paused.entries().get(1).label().startsWith("Abandon"));

        var pausing = ActionMenus.plan("en", actions("plan", "pausing", null, "start", "cancel", "chat"),
                            null, null, null);
        assertEquals("s", pausing.entries().get(0).trigger());
        assertTrue(pausing.entries().get(1).label().startsWith("Cancel"));
    }

    @Test
    @DisplayName("a disabled action stays in the menu with the runtime's reason")
    void disabled() {
        var offered = new ArrayList<ActionAvailability>();
        offered.add(ActionAvailability.disabled("chat", "no configuration of replan qualifies yet"));
        var menu = ActionMenus.plan("en", new WorkActions("subject", "plan", "running", null, null, null,
                            offered), null, null, null);
        assertFalse(menu.entries().getFirst().enabled());
        assertEquals("no configuration of replan qualifies yet", menu.entries().getFirst().reason());
    }

    @Test
    @DisplayName("a held plan shows the acts of its hold first and drops its own Cancel")
    void held() {
        var menu = ActionMenus.plan("en", actions("plan", "held", "condition", "pause", "cancel", "chat",
                            "settings"), hold("retry", "grant", "end_plan"), null, null);
        var labels = menu.entries().stream().map(TuiMenuEntry::label).toList();
        assertTrue(labels.get(0).startsWith("Retry now"), labels.toString());
        assertTrue(labels.get(1).startsWith("Grant read access"), labels.toString());
        assertTrue(labels.get(2).startsWith("End the plan"), labels.toString());
        assertTrue(labels.stream().noneMatch(label -> label.startsWith("Cancel")), labels.toString());
        assertTrue(labels.stream().anyMatch(label -> label.startsWith("Pause")), labels.toString());
        assertEquals(labels.size(), new HashSet<>(labels).size());
        var triggers = menu.entries().stream().map(TuiMenuEntry::trigger).toList();
        assertEquals(triggers.size(), new HashSet<>(triggers).size(), triggers.toString());
    }

    @Test
    @DisplayName("an integrity pause offers Accept the change and Abort the plan")
    void integrityPause() {
        var menu = ActionMenus.plan("en", actions("plan", "held", "integrity_pause", "pause", "cancel"),
                            hold("retry", "accept", "abort"), null, null);
        var labels = menu.entries().stream().map(TuiMenuEntry::label).toList();
        assertTrue(labels.stream().anyMatch(label -> label.startsWith("Accept the change")), labels.toString());
        assertTrue(labels.stream().anyMatch(label -> label.startsWith("Abort the plan")), labels.toString());
        var triggers = menu.entries().stream().map(TuiMenuEntry::trigger).toList();
        assertTrue(triggers.contains("c"), triggers.toString());
        assertTrue(triggers.contains("x"), triggers.toString());
    }

    @Test
    @DisplayName("an epic offers its state's actions with the next plan and the counts")
    void epic() {
        var menu = ActionMenus.epic("en",
                            actions("epic", "running", null, "next", "auto", "common_files", "decompose",
                                                "findings", "watches", "chat", "archive", "cancel"),
                            "relay-token-store", List.of(), true, 2, 3, 5, "row");
        var labels = menu.entries().stream().map(TuiMenuEntry::label).toList();
        assertTrue(labels.get(0).contains("Next plan: relay-token-store"), labels.toString());
        assertTrue(labels.get(1).contains("Automatic: on"), labels.toString());
        assertTrue(labels.get(2).contains("Common files allowed: 2"), labels.toString());
        assertTrue(labels.get(3).startsWith("Decompose again"), labels.toString());
        assertTrue(labels.get(4).contains("Findings review (3)"), labels.toString());
        assertTrue(labels.get(5).contains("Watches review (5)"), labels.toString());
        assertEquals("n", menu.entries().get(0).trigger());
        assertEquals("u", menu.entries().get(1).trigger());
        assertEquals("m", menu.entries().get(2).trigger());
        assertEquals("d", menu.entries().get(3).trigger());
        assertEquals("f", menu.entries().get(4).trigger());
        assertEquals("w", menu.entries().get(5).trigger());
        assertEquals("A", menu.entries().get(7).trigger());
    }

    @Test
    @DisplayName("an epic with nothing launchable names why, and a landing's archive is disabled")
    void epicNothingAndArchiveDisabled() {
        var offered = new ArrayList<ActionAvailability>();
        offered.add(ActionAvailability.enabled("next"));
        offered.add(ActionAvailability.disabled("archive", "the ledger is landing"));
        var menu = ActionMenus.epic("en",
                            new WorkActions("subject", "epic", "landing", null, null, null, offered),
                            null, List.of("relay-docs waits: overlap with relay-auth-v2"), false, 0, 0, 0, null);
        assertTrue(menu.entries().get(0).label().contains("No plan can start"), menu.entries().get(0).label());
        assertTrue(menu.entries().get(0).label().contains("overlap with relay-auth-v2"));
        assertFalse(menu.entries().get(1).enabled());
        assertEquals("the ledger is landing", menu.entries().get(1).reason());
        assertTrue(menu.entries().get(1).label().startsWith("Archive"));
    }

    @Test
    @DisplayName("an ended item offers the read-only chat alone")
    void ended() {
        var menu = ActionMenus.ended("en", "fix-relay", "fix-relay", "row");
        assertEquals(1, menu.entries().size());
        var label = menu.entries().getFirst().label();
        assertTrue(label.contains("fix-relay has ended: its view, and a chat about it"), label);
        assertEquals("h", menu.entries().getFirst().trigger());
        assertTrue(menu.ended());
    }

    @Test
    @DisplayName("a landing offers retry, its pull request, and giving up the removal")
    void landing() {
        var menu = ActionMenus.landing("en", "remediation", "landing_failed", "landing:remediation");
        assertEquals(3, menu.entries().size());
        assertTrue(menu.entries().get(0).label().startsWith("Retry"));
        assertTrue(menu.entries().get(1).label().startsWith("Show the pull request"));
        assertTrue(menu.entries().get(2).label().startsWith("Give up the removal"));
        assertEquals(ActionsMenu.Subject.LANDING, menu.subject());
    }

    @Test
    @DisplayName("the mnemonics are the catalogue's: German differs from English, and none is a")
    void germanMnemonics() {
        var en = ActionMenus.plan("en", actions("plan", "running", null, "pause", "cancel", "chat",
                            "settings"), null, null, null);
        var de = ActionMenus.plan("de", actions("plan", "running", null, "pause", "cancel", "chat",
                            "settings"), null, null, null);
        assertEquals("p", en.entries().get(0).trigger());
        assertEquals("h", de.entries().get(0).trigger());
        assertEquals("o", en.entries().get(3).trigger());
        assertEquals("e", de.entries().get(3).trigger());
        assertEquals("h", en.entries().get(2).trigger());
        assertEquals("t", de.entries().get(2).trigger());
        for (var menu : List.of(en, de)) {
            assertTrue(menu.entries().stream().noneMatch(entry -> "a".equals(entry.trigger())),
                                menu.entries().toString());
        }
        assertNotNull(de.selected());
    }
}
