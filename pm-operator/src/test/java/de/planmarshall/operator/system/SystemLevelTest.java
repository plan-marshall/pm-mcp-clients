/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import de.planmarshall.api.system.CleanupSummary;
import de.planmarshall.api.system.ModelMetric;
import de.planmarshall.api.system.RuntimeMetrics;
import de.planmarshall.api.system.SystemSnapshot;
import de.planmarshall.api.system.SystemWindow;
import de.planmarshall.api.system.WorkMetrics;
import de.planmarshall.api.system.WorkerMetric;
import de.planmarshall.api.system.WorkerState;
import de.planmarshall.operator.tui.TuiRow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SystemLevel")
class SystemLevelTest {

    static SystemSnapshot sampleSnapshot() {
        var runtime = new RuntimeMetrics(
                            "0.9.1",
                            "2026-10-02 (a1b2c3d)",
                            "today 08:14",
                            22320L,
                            432013312L,
                            1073741824L,
                            713031680L,
                            4.0,
                            2469606195L,
                            40802189312L,
                            "13:40",
                            true,
                            "127.0.0.1:7420",
                            "192.168.1.20:7421",
                            2,
                            0,
                            null,
                            "0.10.0",
                            "2026-10-16",
                            "today 08:14",
                            "/api/v1/system/release-notes?version=0.10.0",
                            true,
                            null);
        var work = new WorkMetrics(
                            Map.of("running", 3, "waiting_for_you", 1, "held", 1, "paused", 2),
                            Map.of("running", 1, "waiting_for_you", 1),
                            4,
                            6,
                            3,
                            List.of());
        var workers = List.of(
                            new WorkerMetric("planner", "claude", "opus-4.5", WorkerState.BUSY, "fix-relay"),
                            new WorkerMetric("implementer", "claude", "sonnet-4.5", WorkerState.BUSY, "relay-docs"),
                            new WorkerMetric("implementer", "codex", "gpt-5", WorkerState.IDLE, null),
                            new WorkerMetric("implementer", "claude", "sonnet-4.5", WorkerState.BUSY, "token-usage"),
                            new WorkerMetric("reviewer", "claude", "sonnet-4.5", WorkerState.IDLE, null));
        var jobs = Map.<String, Object>of(
                            "build", Map.of("slots_used", 1, "slots_total", 2, "queued", 1),
                            "test", Map.of("slots_used", 0, "slots_total", 1, "queued", 0),
                            "locks_held", 3);
        var models = List.of(
                            new ModelMetric("claude:opus-4.5", "planner", 42, 2020000L, 1900000L, 120000L, 0, 1,
                                                false, 0),
                            new ModelMetric("claude:sonnet-4.5", "implementer", 118, 3610000L, 3200000L, 410000L, 0,
                                                0, false, 0),
                            new ModelMetric("codex:gpt-5", "implementer", 12, 0L, 0L, 0L, 1, 0, true, 12));
        var cleanup = new CleanupSummary(12, 2, 40, 5, 1, 1932735283L, 4, "today 03:00", "tomorrow 03:00");
        return new SystemSnapshot(runtime, work, workers, jobs, models, cleanup, SystemWindow.HOUR, null);
    }

    private static SystemLevel level() {
        return SystemLevel.root(sampleSnapshot(), SystemWindow.HOUR, "model", null);
    }

    @Test
    @DisplayName("the root shows the six figures in their fixed order")
    void showsTheSixFigures() {
        var rows = level().rows("en");

        assertEquals(List.of(SystemLevel.ROW_RUNTIME, SystemLevel.ROW_WORK, SystemLevel.ROW_WORKERS,
                                                SystemLevel.ROW_JOBS, SystemLevel.ROW_MODELS, SystemLevel.ROW_CLEANUP),
                            rows.stream().map(TuiRow::id).toList());
        assertEquals(SystemLevel.Figure.RUNTIME, SystemLevel.figureOf(rows.get(0).id()));
        assertEquals(SystemLevel.Figure.CLEANUP, SystemLevel.figureOf(rows.get(5).id()));
        assertNull(SystemLevel.figureOf("sys:workers:0"));
    }

    @Test
    @DisplayName("the figures name the runtime, the work, the jobs, the models, and the cleanup")
    void rendersTheFigures() {
        var rows = level().rows("en");

        assertTrue(rows.get(0).line().contains("running since today 08:14"), rows.get(0).line());
        assertTrue(rows.get(0).line().contains("0.10.0 available"), rows.get(0).line());
        assertTrue(rows.get(0).details().contains("CPU 4 %"), rows.get(0).details());
        assertTrue(rows.get(0).details().contains("127.0.0.1:7420"), rows.get(0).details());

        assertTrue(rows.get(1).line().contains("3 running"), rows.get(1).line());
        assertTrue(rows.get(1).line().contains("1 waiting for you"), rows.get(1).line());
        assertTrue(rows.get(1).details().contains("4 running \u00b7 6 queued"), rows.get(1).details());

        assertEquals(5, rows.get(2).details().split("\n").length);

        assertTrue(rows.get(3).line().contains("locks  3 held"), rows.get(3).line());
        assertTrue(rows.get(3).details().contains("build  1 of 2 slots \u00b7 1 queued"), rows.get(3).details());
        assertTrue(rows.get(3).details().contains("test  0 of 1 slot"), rows.get(3).details());

        assertTrue(rows.get(4).line().contains("172 calls"), rows.get(4).line());
        assertTrue(rows.get(4).line().contains("\u2265 5.63 M tokens"), rows.get(4).line());
        assertTrue(rows.get(4).line().contains("\u2265 5.10 M in"), rows.get(4).line());

        assertTrue(rows.get(5).line().contains("due: 12 plans"), rows.get(5).line());
        assertTrue(rows.get(5).details().contains("4 kept for good"), rows.get(5).details());
    }

    @Test
    @DisplayName("a figure the runtime cannot measure says why instead of showing a zero")
    void namesAnUnavailableFigure() {
        var runtime = new RuntimeMetrics("0.9.1", "", "", 60L, 0L, 0L, 0L, null, 0L, 0L, "", false, "off", "off", 0,
                            0, null, null, null, null, null, true, null);
        var snapshot = new SystemSnapshot(runtime, new WorkMetrics(Map.of(), Map.of(), 0, 0, 0, List.of()),
                            List.of(), Map.of(), List.of(),
                            new CleanupSummary(0, 0, 0, 0, 0, 0L, 0, "today 03:00", "tomorrow 03:00"),
                            SystemWindow.HOUR, null);

        var rows = SystemLevel.root(snapshot, SystemWindow.HOUR, "model", null).rows("en");

        assertTrue(rows.get(0).details().contains("CPU not available"), rows.get(0).details());
        assertTrue(rows.get(1).line().contains("none running"), rows.get(1).line());
        assertTrue(rows.get(4).line().contains("No model calls"), rows.get(4).line());
        assertTrue(rows.get(5).line().contains("nothing due"), rows.get(5).line());
    }

    @Test
    @DisplayName("a negative CPU sentinel is unavailable, never a percentage")
    void treatsANegativeCpuAsUnavailable() {
        var base = sampleSnapshot().runtime();
        var runtime = new RuntimeMetrics(base.version(), base.build(), base.startedAt(), base.uptimeSeconds(),
                            base.heapUsedBytes(), base.heapMaxBytes(), base.residentMemoryBytes(), -1.0,
                            base.machineStoreBytes(), base.diskFreeBytes(), base.measuredAt(),
                            base.socketListener(), base.loopbackListener(), base.networkListener(),
                            base.pairedDevicesCount(), base.relaysBehindCount(), base.relaysBehindVersion(),
                            base.newerVersion(), base.newerVersionDate(), base.newerVersionCheckedAt(),
                            base.releaseNotesLink(), base.upgradeSupported(), base.upgradeUnsupportedReason());
        var snapshot = new SystemSnapshot(runtime, sampleSnapshot().work(), sampleSnapshot().workers(),
                            sampleSnapshot().jobs(), sampleSnapshot().models(), sampleSnapshot().cleanup(),
                            SystemWindow.HOUR, null);

        var rows = SystemLevel.root(snapshot, SystemWindow.HOUR, "model", null).rows("en");

        assertTrue(rows.get(0).details().contains("CPU not available"), rows.get(0).details());
        assertFalse(rows.get(0).details().contains("CPU -1"), rows.get(0).details());
    }

    @Test
    @DisplayName("the models group per model or per role without reading the runtime again")
    void groupsTheModels() {
        var perModel = level().rows("en", SystemLevel.Section.MODELS);
        var perRole = level().withChoice(SystemWindow.HOUR, "role", null).rows("en", SystemLevel.Section.MODELS);

        assertEquals(3, perModel.size());
        assertTrue(perModel.get(0).line().contains("claude:opus-4.5"), perModel.get(0).line());
        assertTrue(perModel.get(2).line().contains("not measured"), perModel.get(2).line());

        assertEquals(2, perRole.size());
        assertTrue(perRole.get(0).line().contains("planner"), perRole.get(0).line());
        assertTrue(perRole.get(1).line().contains("implementer"), perRole.get(1).line());
    }

    @Test
    @DisplayName("the jobs section leads to the locks, and no lock says so")
    void rendersJobsAndLocks() {
        var jobs = level().rows("en", SystemLevel.Section.JOBS);
        assertEquals(3, jobs.size());
        assertTrue(jobs.getLast().openable());
        assertEquals(SystemLevel.ROW_LOCKS, jobs.getLast().id());

        var locks = level().rows("en", SystemLevel.Section.LOCKS);
        assertTrue(locks.getFirst().line().contains("3 held"), locks.getFirst().line());

        var empty = level().withSnapshot(new SystemSnapshot(sampleSnapshot().runtime(), sampleSnapshot().work(),
                            sampleSnapshot().workers(), Map.<String, Object>of("locks_held", 0),
                            sampleSnapshot().models(), sampleSnapshot().cleanup(), SystemWindow.HOUR, null))
                            .rows("en", SystemLevel.Section.LOCKS);
        assertTrue(empty.getFirst().line().contains("none held"), empty.getFirst().line());
    }

    @Test
    @DisplayName("the workers and the cleanup sections list what their figures count")
    void rendersWorkersAndCleanup() {
        var workers = level().rows("en", SystemLevel.Section.WORKERS);
        assertEquals(5, workers.size());
        assertTrue(workers.get(0).line().contains("planner"), workers.get(0).line());
        assertTrue(workers.get(0).line().contains("fix-relay"), workers.get(0).line());

        var cleanup = level().rows("en", SystemLevel.Section.CLEANUP);
        assertEquals(4, cleanup.size());
        assertTrue(cleanup.get(0).line().contains("due:"), cleanup.get(0).line());
    }

    @Test
    @DisplayName("the window cycles hour, today, 7 days, and the grouping swaps model and role")
    void cyclesTheChoices() {
        assertEquals(SystemWindow.TODAY, level().nextWindow());
        assertEquals(SystemWindow.SEVEN_DAYS, level().withChoice(SystemWindow.TODAY, "model", null).nextWindow());
        assertEquals(SystemWindow.HOUR, level().withChoice(SystemWindow.SEVEN_DAYS, "model", null).nextWindow());

        assertEquals("role", level().nextGroupBy());
        assertEquals("model", level().withChoice(SystemWindow.HOUR, "role", null).nextGroupBy());
    }

    @Test
    @DisplayName("the header names the runtime and the project, the footer names the toggles' targets")
    void namesTheHeaderAndFooter() {
        assertEquals("System \u00b7 pm-mcpd 0.9.1", level().breadcrumb("en"));
        var withProject = level().withChoice(SystemWindow.HOUR, "model", "pm-mcp-clients");
        assertTrue(withProject.breadcrumb("en").contains("pm-mcp-clients"), withProject.breadcrumb("en"));
        assertTrue(level().breadcrumb("en", SystemLevel.Section.WORKERS).contains("Workers"));

        var footer = level().footer("en");
        assertTrue(footer.contains("w window"), footer);
        assertTrue(footer.contains("b by role"), footer);
        assertTrue(footer.contains("p per project"), footer);
        assertTrue(withProject.footer("en").contains("b by role"), withProject.footer("en"));
        assertTrue(withProject.footer("en").contains("p all projects"), withProject.footer("en"));
        assertTrue(level().withChoice(SystemWindow.HOUR, "role", null).footer("en").contains("b by model"));
    }

    @Test
    @DisplayName("the section ids round-trip and a level without a snapshot says it could not read one")
    void mapsSectionsAndTheFailure() {
        for (var section : SystemLevel.Section.values()) {
            assertEquals(section, SystemLevel.sectionOf(SystemLevel.levelId(section)), section.name());
        }
        assertEquals(SystemLevel.LEVEL_ID, SystemLevel.levelId(SystemLevel.Section.ROOT));

        var unavailable = SystemLevel.unavailable(SystemWindow.HOUR, "model", null);
        assertNull(unavailable.snapshot());
        assertFalse(unavailable.upgradeSupported());
        var rows = unavailable.rows("en");
        assertEquals(1, rows.size());
        assertNotNull(rows.getFirst().line());
        assertEquals("System", unavailable.breadcrumb("en"));
    }
}
