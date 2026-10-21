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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringWriter;

import de.planmarshall.operator.overview.OverviewFullScreenApp;
import de.planmarshall.operator.tui.VirtualScreenTerminal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@DisplayName("EpicViewFullScreenTest (VirtualScreenTerminal)")
@EnabledOnOs({OS.MAC, OS.LINUX})
class EpicViewFullScreenTest {

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
    @DisplayName("Enter on an epic opens its view with the head, the workstreams, and every section")
    void shouldOpenEpicView() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(160, 45)) {
            var context = control.context(virtual);
            var thread = ControlFixture.start(control.app(context, virtual,
                                                    new OverviewFullScreenApp.InitialSelection("pm-mcp-clients", "epic", false)),
                                new StringWriter());
            ControlFixture.await(() -> virtual.alternateScreenActive()
                                && ControlFixture.shows(virtual, "by epic"));
            virtual.type("\r");
            ControlFixture.await(() -> ControlFixture.shows(virtual, "WS-1  Reconnect")
                                && ControlFixture.shows(virtual, "relay-auth-v2 asks a decision"));

            var screen = String.join("\n", virtual.screen().lines());
            assertTrue(screen.contains("pm-mcp-clients \u203a relay-hardening"), screen);
            assertTrue(screen.contains("relay-hardening \u00b7 epic"), screen);
            assertTrue(screen.contains("automatic"), screen);
            assertTrue(screen.contains("3 of 3 at once"), screen);
            assertTrue(screen.contains("WS-2  Authentication"), screen);
            assertTrue(screen.contains("relay-token-rotation"), screen);
            assertTrue(screen.contains("Sources"), screen);
            assertTrue(screen.contains("Landings"), screen);
            assertTrue(screen.contains("Findings"), screen);
            assertTrue(screen.contains("Watches"), screen);
            assertTrue(screen.contains("Tokens"), screen);
            assertTrue(screen.contains("Events"), screen);
            assertTrue(screen.contains("a actions"), screen);
            assertTrue(screen.contains("p project"), screen);

            virtual.type("\u001b");
            ControlFixture.await(() -> ControlFixture.shows(virtual, "by epic"));
            ControlFixture.delay(200L);
            virtual.type("q");
            ControlFixture.join(thread);
        }
    }

    @Test
    @DisplayName("e in a plan opens its epic's view, and Esc returns to the plan")
    void shouldOpenFromPlanAndReturn() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(160, 45)) {
            var context = control.context(virtual);
            var thread = ControlFixture.start(control.app(context, virtual,
                                OverviewFullScreenApp.InitialSelection.plan("fix-relay")), new StringWriter());
            ControlFixture.await(() -> virtual.alternateScreenActive()
                                && ControlFixture.shows(virtual, "phase execute"));
            virtual.type("e");
            ControlFixture.await(() -> ControlFixture.shows(virtual, "WS-1  Reconnect")
                                && ControlFixture.shows(virtual, "p project"));
            virtual.type("\u001b");
            ControlFixture.await(() -> ControlFixture.shows(virtual, "phase execute"));
            assertTrue(ControlFixture.shows(virtual, "p project"));
            ControlFixture.delay(200L);
            virtual.type("q");
            ControlFixture.join(thread);
        }
    }

    @Test
    @DisplayName("a opens the epic's actions below the head; a and Esc close it")
    void shouldOpenEpicActions() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(160, 45)) {
            var context = control.context(virtual);
            var thread = openEpic(context, virtual);
            virtual.type("a");
            ControlFixture.await(() -> ControlFixture.shows(virtual, "Next plan: relay-reconnect-metrics")
                                && ControlFixture.shows(virtual, "Automatic: on"));
            var screen = String.join("\n", virtual.screen().lines());
            assertTrue(screen.contains("Findings review (3)"), screen);
            assertTrue(screen.contains("Watches review (4)"), screen);
            assertTrue(screen.contains("Decompose again"), screen);
            assertTrue(screen.contains("Common files allowed: 0"), screen);
            assertTrue(screen.contains("Archive"), screen);
            assertTrue(screen.contains("Cancel"), screen);

            virtual.type("a");
            ControlFixture.await(() -> !ControlFixture.shows(virtual, "Next plan: relay-reconnect-metrics"));
            virtual.type("a");
            ControlFixture.await(() -> ControlFixture.shows(virtual, "Next plan: relay-reconnect-metrics"));
            virtual.type("\u001b");
            ControlFixture.await(() -> !ControlFixture.shows(virtual, "Next plan: relay-reconnect-metrics"));

            ControlFixture.delay(200L);
            virtual.type("q");
            ControlFixture.join(thread);
        }
    }

    @Test
    @DisplayName("u switches automatic off in one act, naming the value it sets")
    void shouldToggleAutomatic() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(160, 45)) {
            var context = control.context(virtual);
            var out = new StringWriter();
            var thread = openEpic(context, virtual, out);
            virtual.type("a");
            ControlFixture.await(() -> ControlFixture.shows(virtual, "Automatic: on"));
            virtual.type("u");
            ControlFixture.await(() -> out.toString().contains("starts no plan by itself any more"));
            assertTrue(control.requests().stream().anyMatch(
                                                    request -> request.endsWith("/api/v1/epics/relay-hardening/actions/auto")),
                                control.requests().toString());
            assertTrue(control.bodies().stream().anyMatch(body -> body.contains("\"enabled\":false")),
                                control.bodies().toString());
            ControlFixture.delay(200L);
            virtual.type("q");
            ControlFixture.join(thread);
        }
    }

    @Test
    @DisplayName("p opens the project in one key, and Esc returns to the epic")
    void shouldJumpToProject() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(160, 45)) {
            var context = control.context(virtual);
            var thread = openEpic(context, virtual);
            virtual.type("p");
            // p pushes the project's level, so Esc returns to the epic.
            ControlFixture.await(() -> ControlFixture.shows(virtual, "by plan"));
            assertTrue(ControlFixture.shows(virtual, "by plan"));
            virtual.type("\u001b");
            ControlFixture.await(() -> ControlFixture.shows(virtual, "WS-1  Reconnect"));
            assertTrue(ControlFixture.shows(virtual, "WS-1  Reconnect"));
            ControlFixture.delay(200L);
            virtual.type("q");
            ControlFixture.join(thread);
        }
    }

    @Test
    @DisplayName("a staged plan opens to its objective, deliverables, and surface")
    void shouldOpenStagedPlan() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(160, 45)) {
            var context = control.context(virtual);
            var thread = openEpic(context, virtual);
            for (int step = 0; step < 5; step++) {
                virtual.type("j");
            }
            ControlFixture.await(() -> virtual.screen().lines().stream()
                                .anyMatch(line -> line.startsWith("\u25b6")
                                                    && line.contains("relay-reconnect-metrics")));
            virtual.type("\r");
            ControlFixture.await(() -> ControlFixture.shows(virtual, "Objective     measure the reconnect")
                                && ControlFixture.shows(virtual, "Surface       pm-relay/src/metrics"));
            assertTrue(ControlFixture.shows(virtual, "Objective     measure the reconnect"));
            assertTrue(ControlFixture.shows(virtual, "Surface       pm-relay/src/metrics"));
            ControlFixture.delay(200L);
            virtual.type("q");
            ControlFixture.join(thread);
        }
    }

    @Test
    @DisplayName("* opens every workstream and section")
    void shouldExpandAll() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(160, 45)) {
            var context = control.context(virtual);
            var thread = openEpic(context, virtual);
            assertFalse(ControlFixture.shows(virtual, "relay-reconnect-backoff  #211"));
            virtual.type("*");
            ControlFixture.await(() -> ControlFixture.shows(virtual, "relay-reconnect-backoff  #211"));
            ControlFixture.delay(200L);
            virtual.type("q");
            ControlFixture.join(thread);
        }
    }

    @Test
    @DisplayName("an ended epic opens read-only and offers the chat alone")
    void shouldOpenEndedEpic() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(160, 45)) {
            var context = control.context(virtual);
            var thread = ControlFixture.start(control.app(context, virtual,
                                OverviewFullScreenApp.InitialSelection.DEFAULT), new StringWriter());
            ControlFixture.await(() -> virtual.alternateScreenActive()
                                && ControlFixture.shows(virtual, "pm-mcp-clients"));
            virtual.type("E");
            ControlFixture.await(() -> ControlFixture.shows(virtual, "relay-docs")
                                && ControlFixture.shows(virtual, "completed"));
            virtual.type("j");
            ControlFixture.await(() -> virtual.screen().lines().stream()
                                .anyMatch(line -> line.startsWith("\u25b6") && line.contains("relay-docs")));
            virtual.type("\r");
            ControlFixture.await(() -> ControlFixture.shows(virtual, "ended")
                                && ControlFixture.shows(virtual, "completed"));
            virtual.type("a");
            ControlFixture.await(() -> ControlFixture.shows(virtual, "has ended: its view, and a chat about it"));
            var screen = String.join("\n", virtual.screen().lines());
            assertTrue(screen.contains("Chat"), screen);
            assertFalse(screen.contains("Next plan:"), screen);
            ControlFixture.delay(200L);
            virtual.type("\u001b");
            ControlFixture.delay(150L);
            virtual.type("q");
            ControlFixture.join(thread);
        }
    }


    @Test
    @DisplayName("! on a held plan shows what holds it")
    void shouldOpenTheHold() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(160, 45)) {
            var context = control.context(virtual);
            var out = new StringWriter();
            var thread = openEpic(context, virtual, out);
            for (int step = 0; step < 3; step++) {
                virtual.type("j");
                ControlFixture.delay(40L);
            }
            ControlFixture.await(() -> virtual.screen().lines().stream()
                                .anyMatch(line -> line.startsWith("\u25b6") && line.contains("relay-cursor-order")));
            virtual.type("!");
            ControlFixture.await(() -> out.toString().contains("holds: an integrity pause"));
            assertTrue(out.toString().contains("references.json"), out.toString());
            virtual.type("\r");
            ControlFixture.delay(300L);
            virtual.type("q");
            ControlFixture.join(thread);
        }
    }

    private Thread openEpic(OperatorContext context, VirtualScreenTerminal virtual) throws Exception {
        return openEpic(context, virtual, new StringWriter());
    }

    private Thread openEpic(OperatorContext context, VirtualScreenTerminal virtual, StringWriter out)
                        throws Exception {
        var thread = ControlFixture.start(control.app(context, virtual,
                                                new OverviewFullScreenApp.InitialSelection("pm-mcp-clients", "epic", false)),
                            out);
        ControlFixture.await(() -> virtual.alternateScreenActive() && ControlFixture.shows(virtual, "by epic"));
        virtual.type("\r");
        ControlFixture.await(() -> ControlFixture.shows(virtual, "WS-1  Reconnect"));
        return thread;
    }

    @Test
    @DisplayName("Esc from the epic view opened at the project returns to the project")
    void shouldReturnToTheCaller() throws Exception {
        try (var virtual = VirtualScreenTerminal.create(160, 45)) {
            var context = control.context(virtual);
            var thread = openEpic(context, virtual);
            virtual.type("\u001b");
            ControlFixture.await(() -> ControlFixture.shows(virtual, "by epic"));
            assertEquals(1, virtual.alternation().entries());
            ControlFixture.delay(150L);
            virtual.type("q");
            ControlFixture.join(thread);
        }
    }
}
