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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.StringWriter;

import de.planmarshall.operator.overview.OverviewFullScreenApp;
import de.planmarshall.operator.tui.VirtualScreenTerminal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@DisplayName("ActionsFullScreenTest (VirtualScreenTerminal)")
@EnabledOnOs({OS.MAC, OS.LINUX})
class ActionsFullScreenTest {

    private ControlFixture control;

    @BeforeEach
    void setUp() throws Exception {
        control = ControlFixture.create();
    }

    @AfterEach
    void tearDown() throws Exception {
        control.close();
    }

    @Test
    @DisplayName("a below a plan row opens its actions; Esc closes them and the row's keys wait")
    void shouldOpenPlanActionsBelowRow() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(160, 45)) {
            var context = control.context(virtual);
            var thread = openProject(context, virtual);
            select(virtual, "Fix Relay Transport");
            virtual.type("a");
            ControlFixture.await(() -> ControlFixture.shows(virtual, "Pause\u2026")
                                && ControlFixture.shows(virtual, "Settings\u2026"));
            var screen = virtual.screen().lines();
            assertTrue(screen.stream().anyMatch(line -> line.startsWith("      p  Pause\u2026")), screen.toString());
            assertTrue(screen.stream().anyMatch(line -> line.contains("Cancel\u2026")), screen.toString());
            assertTrue(screen.stream().anyMatch(line -> line.contains("Chat\u2026")), screen.toString());
            assertTrue(screen.stream().anyMatch(line -> line.contains("Settings\u2026")), screen.toString());
            // While the menu is open the view's keys do not act: Esc closes the menu, not the level.
            virtual.type("\u001b");
            ControlFixture.await(() -> !ControlFixture.shows(virtual, "Pause\u2026"));
            assertTrue(ControlFixture.shows(virtual, "by plan"));

            virtual.type("a");
            ControlFixture.await(() -> ControlFixture.shows(virtual, "Pause\u2026"));
            virtual.type("a");
            ControlFixture.await(() -> !ControlFixture.shows(virtual, "Pause\u2026"));

            ControlFixture.delay(200L);
            virtual.type("q");
            ControlFixture.join(thread);
        }
    }

    @Test
    @DisplayName("p pauses with its point, and the view says where the plan holds")
    void shouldPause() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(160, 45)) {
            var context = control.context(virtual);
            var out = new StringWriter();
            var thread = openProject(context, virtual, out);
            select(virtual, "Fix Relay Transport");
            virtual.type("a");
            ControlFixture.await(() -> ControlFixture.shows(virtual, "Pause\u2026"));
            virtual.type("p");
            virtual.type("2\r");
            ControlFixture.await(() -> out.toString().contains("fix-relay pausing after the task"));
            assertTrue(control.requests().stream().anyMatch(
                                                    request -> request.endsWith("/api/v1/plans/fix-relay/actions/pause")),
                                control.requests().toString());
            assertTrue(control.bodies().stream().anyMatch(body -> body.contains("\"point\":\"task\"")),
                                control.bodies().toString());
            ControlFixture.delay(200L);
            virtual.type("q");
            ControlFixture.join(thread);
        }
    }

    @Test
    @DisplayName("s starts a paused plan in place, and the row follows")
    void shouldStartInPlace() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(160, 45)) {
            var context = control.context(virtual);
            var out = new StringWriter();
            var thread = openProject(context, virtual, out);
            select(virtual, "Relay Metrics");
            virtual.type("a");
            ControlFixture.await(() -> ControlFixture.shows(virtual, "Start"));
            virtual.type("s");
            ControlFixture.await(() -> out.toString().contains("relay-metrics started"));
            assertTrue(control.requests().stream().anyMatch(
                                                    request -> request.endsWith("/api/v1/plans/relay-metrics/actions/start")),
                                control.requests().toString());
            ControlFixture.delay(200L);
            virtual.type("q");
            ControlFixture.join(thread);
        }
    }

    @Test
    @DisplayName("a held plan shows the acts of its hold first, and an act resolves it")
    void shouldResolveHold() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(160, 45)) {
            var context = control.context(virtual);
            var out = new StringWriter();
            var thread = openProject(context, virtual, out);
            select(virtual, "Relay Cursor Order");
            virtual.type("a");
            ControlFixture.await(() -> ControlFixture.shows(virtual, "Retry now")
                                && ControlFixture.shows(virtual, "Accept the change\u2026")
                                && ControlFixture.shows(virtual, "Abort the plan\u2026"));
            virtual.type("r");
            ControlFixture.await(() -> out.toString().contains("relay-cursor-order goes on"));
            assertTrue(control.requests().stream().anyMatch(
                                                    request -> request.endsWith(
                                                                        "/api/v1/plans/relay-cursor-order/holds/H-0001/resolve")),
                                control.requests().toString());
            assertTrue(control.bodies().stream().anyMatch(body -> body.contains("\"act\":\"retry\"")),
                                control.bodies().toString());
            ControlFixture.delay(200L);
            virtual.type("q");
            ControlFixture.join(thread);
        }
    }

    @Test
    @DisplayName("a in the plan view opens the plan's actions below the header")
    void shouldOpenActionsInPlanView() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(160, 45)) {
            var context = control.context(virtual);
            var thread = ControlFixture.start(control.app(context, virtual,
                                OverviewFullScreenApp.InitialSelection.plan("fix-relay")), new StringWriter());
            ControlFixture.await(() -> virtual.alternateScreenActive()
                                && ControlFixture.shows(virtual, "phase execute"));
            virtual.type("a");
            ControlFixture.await(() -> ControlFixture.shows(virtual, "Pause\u2026"));
            virtual.type("\u001b");
            ControlFixture.await(() -> !ControlFixture.shows(virtual, "Pause\u2026"));
            ControlFixture.delay(200L);
            virtual.type("q");
            ControlFixture.join(thread);
        }
    }

    @Test
    @DisplayName("a on ended work offers the read-only chat alone")
    void shouldOfferEndedChatAlone() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(160, 45)) {
            var context = control.context(virtual);
            var thread = ControlFixture.start(control.app(context, virtual,
                                OverviewFullScreenApp.InitialSelection.DEFAULT), new StringWriter());
            ControlFixture.await(() -> virtual.alternateScreenActive()
                                && ControlFixture.shows(virtual, "pm-mcp-clients"));
            virtual.type("E");
            ControlFixture.await(() -> ControlFixture.shows(virtual, "Fix Relay Transport")
                                && ControlFixture.shows(virtual, "merged"));
            virtual.type("a");
            ControlFixture.await(() -> ControlFixture.shows(virtual, "has ended: its view, and a chat about it"));
            var screen = virtual.screen().lines();
            assertTrue(screen.stream().anyMatch(line -> line.contains("Chat")), screen.toString());
            assertFalse(screen.stream().anyMatch(line -> line.contains("Pause\u2026")), screen.toString());
            ControlFixture.delay(200L);
            virtual.type("\u001b");
            ControlFixture.delay(150L);
            virtual.type("q");
            ControlFixture.join(thread);
        }
    }

    @Test
    @DisplayName("a on an epic row in the project's by-epic view opens its actions below that row")
    void shouldOpenEpicActionsInProject() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(160, 45)) {
            var context = control.context(virtual);
            var thread = ControlFixture.start(control.app(context, virtual,
                                                    new OverviewFullScreenApp.InitialSelection("pm-mcp-clients", "epic", false)),
                                new StringWriter());
            ControlFixture.await(() -> virtual.alternateScreenActive()
                                && ControlFixture.shows(virtual, "by epic"));
            virtual.type("a");
            ControlFixture.await(() -> ControlFixture.shows(virtual, "Next plan: relay-reconnect-metrics"));
            var screen = virtual.screen().lines();
            assertTrue(screen.stream().anyMatch(line -> line.startsWith("      n  Next plan:")),
                                screen.toString());
            ControlFixture.delay(200L);
            virtual.type("\u001b");
            ControlFixture.delay(150L);
            virtual.type("q");
            ControlFixture.join(thread);
        }
    }

    @Test
    @DisplayName("a refused action names the state and what the state allows")
    void shouldRefuseAnActionTheStateDoesNotOffer() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(160, 45)) {
            var context = control.context(virtual);
            var out = new StringWriter();
            var thread = openProject(context, virtual, out);
            select(virtual, "Relay Metrics");
            virtual.type("a");
            ControlFixture.await(() -> ControlFixture.shows(virtual, "Start"));
            // The paused plan offers no pause; the menu has no entry for it, so the key is inert.
            virtual.type("p");
            ControlFixture.delay(250L);
            assertTrue(ControlFixture.shows(virtual, "Start"));
            assertFalse(control.requests().stream().anyMatch(
                                request -> request.endsWith("/api/v1/plans/relay-metrics/actions/pause")));
            ControlFixture.delay(150L);
            virtual.type("\u001b");
            ControlFixture.delay(150L);
            virtual.type("q");
            ControlFixture.join(thread);
        }
    }


    @Test
    @DisplayName("the arrows move the menu's own selection and Enter runs the selected entry")
    void shouldNavigateTheMenu() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(160, 45)) {
            var context = control.context(virtual);
            var out = new StringWriter();
            var thread = openProject(context, virtual, out);
            select(virtual, "Relay Metrics");
            virtual.type("a");
            ControlFixture.await(() -> ControlFixture.shows(virtual, "Start"));
            var down = org.jline.keymap.KeyMap.key(virtual.terminal(),
                                org.jline.utils.InfoCmp.Capability.key_down);
            virtual.type(down != null && !down.isEmpty() ? down : "\u001b[B");
            ControlFixture.delay(150L);
            virtual.type("\r");
            ControlFixture.await(() -> out.toString().contains("Abandon relay-metrics?"));
            assertFalse(control.requests().stream().anyMatch(
                                                    request -> request.endsWith("/api/v1/plans/relay-metrics/actions/start")),
                                control.requests().toString());
            // The dialogue asked its reason; an empty one is refused and nothing is sent.
            virtual.type("\r");
            ControlFixture.await(() -> out.toString().contains("The reason is required."));
            ControlFixture.delay(150L);
            virtual.type("q");
            ControlFixture.join(thread);
        }
    }

    private Thread openProject(OperatorContext context, VirtualScreenTerminal virtual) throws Exception {
        return openProject(context, virtual, new StringWriter());
    }

    private Thread openProject(OperatorContext context, VirtualScreenTerminal virtual, StringWriter out)
                        throws Exception {
        var thread = ControlFixture.start(control.app(context, virtual,
                            new OverviewFullScreenApp.InitialSelection("pm-mcp-clients", null, false)), out);
        ControlFixture.await(() -> virtual.alternateScreenActive()
                            && ControlFixture.shows(virtual, "by plan"));
        return thread;
    }

    private static void select(VirtualScreenTerminal virtual, String title) throws Exception {
        for (int step = 0; step < 8; step++) {
            if (virtual.screen().lines().stream()
                                .anyMatch(line -> line.startsWith("\u25b6") && line.contains(title))) {
                return;
            }
            virtual.type("j");
            ControlFixture.delay(60L);
        }
        fail("the row " + title + " was not reached: " + virtual.screen().lines());
    }
}
