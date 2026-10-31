/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonNumber;

@EnableGeneratorController
class SystemRecordsTest {

    @Test
    void systemEnums() {
        assertEquals(SystemWindow.HOUR, SystemWindow.fromValue("hour"));
        assertEquals(SystemWindow.TODAY, SystemWindow.fromValue("today"));
        assertEquals(SystemWindow.SEVEN_DAYS, SystemWindow.fromValue("7d"));
        assertEquals(SystemWindow.SEVEN_DAYS, SystemWindow.fromValue("week"));

        assertEquals(RuntimeLifecycleState.RUNNING, RuntimeLifecycleState.fromValue("running"));
        assertEquals(RuntimeLifecycleState.DRAINING, RuntimeLifecycleState.fromValue("draining"));
        assertEquals(RuntimeLifecycleState.STOPPING, RuntimeLifecycleState.fromValue("stopping"));
        assertEquals(RuntimeLifecycleState.UPGRADING, RuntimeLifecycleState.fromValue("upgrading"));
        assertEquals(RuntimeLifecycleState.STOPPED, RuntimeLifecycleState.fromValue("stopped"));

        assertEquals(WorkerState.BUSY, WorkerState.fromValue("busy"));
        assertEquals(WorkerState.IDLE, WorkerState.fromValue("idle"));
        assertEquals(WorkerState.STARTING, WorkerState.fromValue("starting"));
        assertEquals(WorkerState.RESTARTING, WorkerState.fromValue("restarting"));

        assertEquals(CleanupJobState.OPEN, CleanupJobState.fromValue("open"));
        assertEquals(CleanupJobState.POSTPONED, CleanupJobState.fromValue("postponed"));
        assertEquals(CleanupJobState.DELETING, CleanupJobState.fromValue("deleting"));
        assertEquals(CleanupJobState.DONE, CleanupJobState.fromValue("done"));

        assertEquals(StopAction.DRAIN, StopAction.fromValue("drain"));
        assertEquals(StopAction.NOW, StopAction.fromValue("now"));

        assertEquals(PostStopAction.EXIT, PostStopAction.fromValue("exit"));
        assertEquals(PostStopAction.RESTART, PostStopAction.fromValue("restart"));
    }

    @Test
    void runtimeMetricsRoundTrip() throws Exception {
        var version = Generators.nonEmptyStrings().next();
        var build = Generators.nonEmptyStrings().next();
        var startedAt = Generators.nonEmptyStrings().next();
        var uptimeSeconds = Generators.integers(0, 1_000_000).next();
        var heapUsed = (long) Generators.integers(0, 1_000_000).next();
        var heapMax = (long) Generators.integers(1_000_000, 2_000_000).next();
        var residentMemory = (long) Generators.integers(0, 1_000_000).next();
        var cpuPercent = Generators.integers(0, 100).next() / 100.0;
        var machineStore = (long) Generators.integers(0, 1_000_000).next();
        var diskFree = (long) Generators.integers(0, 1_000_000).next();
        var peakLabel = Generators.nonEmptyStrings().next();
        var loopback = Generators.nonEmptyStrings().next();
        var network = Generators.nonEmptyStrings().next();
        var pairedDevices = Generators.integers(0, 20).next();
        var newerVersion = Generators.nonEmptyStrings().next();
        var newerVersionDate = Generators.nonEmptyStrings().next();
        var upgradeSeenAt = Generators.nonEmptyStrings().next();
        var releaseNotesUrl = Generators.nonEmptyStrings().next();

        var metrics = new RuntimeMetrics(
                version,
                build,
                startedAt,
                uptimeSeconds,
                heapUsed,
                heapMax,
                residentMemory,
                cpuPercent,
                machineStore,
                diskFree,
                peakLabel,
                true,
                loopback,
                network,
                pairedDevices,
                0,
                null,
                newerVersion,
                newerVersionDate,
                upgradeSeenAt,
                releaseNotesUrl,
                true,
                null
        );

        var json = metrics.toJson();
        assertNotNull(json);

        var restored = RuntimeMetrics.fromJson(json);
        assertEquals(metrics.version(), restored.version());
        assertEquals(metrics.build(), restored.build());
        assertEquals(metrics.uptimeSeconds(), restored.uptimeSeconds());
        assertEquals(metrics.heapUsedBytes(), restored.heapUsedBytes());
        assertEquals(metrics.heapMaxBytes(), restored.heapMaxBytes());
        assertEquals(metrics.residentMemoryBytes(), restored.residentMemoryBytes());
        assertEquals(metrics.cpuPercent(), restored.cpuPercent());
        assertEquals(metrics.machineStoreBytes(), restored.machineStoreBytes());
        assertEquals(metrics.diskFreeBytes(), restored.diskFreeBytes());
        assertEquals(metrics.loopbackListener(), restored.loopbackListener());
        assertEquals(metrics.networkListener(), restored.networkListener());
        assertEquals(metrics.pairedDevicesCount(), restored.pairedDevicesCount());
        assertEquals(metrics.newerVersion(), restored.newerVersion());
        assertTrue(restored.upgradeSupported());
    }

    @Test
    void workMetricsRoundTrip() throws Exception {
        var work = new WorkMetrics(
                Map.of("running", 3, "waiting", 1, "held", 1, "paused", 2),
                Map.of("running", 1, "waiting", 1),
                4,
                6,
                3,
                List.of("recovering-proj")
        );

        var json = work.toJson();
        var restored = WorkMetrics.fromJson(json);

        assertEquals(work.plansByState(), restored.plansByState());
        assertEquals(work.epicsByState(), restored.epicsByState());
        assertEquals(4, restored.tasksRunning());
        assertEquals(6, restored.tasksQueued());
        assertEquals(3, restored.activeProjectsCount());
        assertEquals(List.of("recovering-proj"), restored.recoveringProjects());
    }

    @Test
    void workerMetricRoundTrip() throws Exception {
        var role = Generators.nonEmptyStrings().next();
        var harness = Generators.nonEmptyStrings().next();
        var model = Generators.nonEmptyStrings().next();
        var planTitle = Generators.nonEmptyStrings().next();

        var worker = new WorkerMetric(
                role,
                harness,
                model,
                WorkerState.BUSY,
                planTitle
        );

        var json = worker.toJson();
        var restored = WorkerMetric.fromJson(json);

        assertEquals(role, restored.role());
        assertEquals(harness, restored.harness());
        assertEquals(model, restored.model());
        assertEquals(WorkerState.BUSY, restored.state());
        assertEquals(planTitle, restored.currentPlanTitle());
    }

    @Test
    void modelMetricRoundTrip() throws Exception {
        var model = new ModelMetric(
                "claude:opus-4.5",
                "planner",
                42,
                2020000L,
                1900000L,
                120000L,
                0,
                1,
                false,
                0
        );

        var json = model.toJson();
        var restored = ModelMetric.fromJson(json);

        assertEquals("claude:opus-4.5", restored.harnessAndModel());
        assertEquals(42, restored.callsCount());
        assertEquals(2020000L, restored.tokensTotal());
        assertEquals(1, restored.retriesCount());
        assertFalse(restored.isFloor());
    }

    @Test
    void cleanupJobRoundTrip() throws Exception {
        var summary = new CleanupSummary(
                12,
                2,
                40,
                5,
                1,
                1800000000L,
                4,
                "today 03:00",
                "tomorrow 03:00"
        );

        var item = new CleanupItemEntry(
                "plan:fix-relay",
                "plan",
                "fix-relay",
                "merged (#214)",
                "12 Sep",
                88080384L,
                6,
                0,
                null
        );

        var projectItem = new CleanupProjectItem(
                "pm-mcp-clients",
                2,
                1,
                20,
                3,
                100000000L,
                List.of(item)
        );

        var job = new CleanupJob(
                "cln-0012",
                CleanupJobState.OPEN,
                "today 03:00",
                summary,
                List.of(projectItem),
                Map.of("delete", "/api/v1/system/cleanup/cln-0012/delete")
        );

        var json = job.toJson();
        var restored = CleanupJob.fromJson(json);

        assertEquals("cln-0012", restored.jobId());
        assertEquals(CleanupJobState.OPEN, restored.state());
        assertEquals(12, restored.summary().plansCount());
        assertEquals(1, restored.projectItems().size());
        assertEquals("pm-mcp-clients", restored.projectItems().getFirst().projectId());
        assertEquals(1, restored.projectItems().getFirst().items().size());
        assertEquals("plan:fix-relay", restored.projectItems().getFirst().items().getFirst().id());
    }

    @Test
    void cleanupDurationsRoundTrip() throws Exception {
        var durations = new CleanupDurations(30, 30, 14, 30, 60, 3);
        var json = durations.toJson();
        var restored = CleanupDurations.fromJson(json);

        assertEquals(30, restored.plansAfterDays());
        assertEquals(14, restored.logsAfterDays());
        assertEquals(60, restored.unenrolledAfterDays());
        assertEquals(3, restored.remindAfterDays());
    }

    @Test
    void systemSnapshotRoundTrip() throws Exception {
        var runtime = new RuntimeMetrics(
                "0.9.1",
                "commit-123",
                "today 08:00",
                3600,
                1000L,
                2000L,
                3000L,
                2.5,
                4000L,
                5000L,
                "12:00",
                true,
                "127.0.0.1:7420",
                "off",
                0,
                0,
                null,
                null,
                null,
                null,
                null,
                true,
                null
        );

        var work = new WorkMetrics(Map.of("running", 1), Map.of(), 1, 0, 1, List.of());
        var workers = List.of(new WorkerMetric("impl", "claude", "sonnet-4.5", WorkerState.IDLE, null));
        var summary = new CleanupSummary(0, 0, 0, 0, 0, 0L, 0, null, null);
        var snapshot = new SystemSnapshot(runtime, work, workers, Map.of("locks", 0), List.of(), summary, SystemWindow.HOUR, null);

        var json = snapshot.toJson();
        var restored = SystemSnapshot.fromJson(json);

        assertEquals("0.9.1", restored.runtime().version());
        assertEquals(SystemWindow.HOUR, restored.window());
        assertEquals(1, restored.workers().size());
        assertEquals("sonnet-4.5", restored.workers().getFirst().model());
    }

    @Test
    void eventsRoundTrip() throws Exception {
        var metricsEvent = new SystemMetricsEvent(Map.of("cpu_percent", 5.0, "tasks_running", 2));
        var restoredMetrics = SystemMetricsEvent.fromJson(metricsEvent.toJson());
        assertEquals(String.valueOf(metricsEvent.metrics().get("tasks_running")), String.valueOf(restoredMetrics.metrics().get("tasks_running")));

        var drainEvent = new RuntimeDrainingEvent("the console", 2, List.of("task-1", "job-2"), true, "14:02");
        var restoredDrain = RuntimeDrainingEvent.fromJson(drainEvent.toJson());
        assertEquals("the console", restoredDrain.who());
        assertEquals(2, restoredDrain.remainingCount());
        assertTrue(restoredDrain.withdrawable());

        var decidedEvent = new CleanupDecidedEvent("cln-0012", "delete", "the console", 5, 1000000L, "14:15");
        var restoredDecided = CleanupDecidedEvent.fromJson(decidedEvent.toJson());
        assertEquals("cln-0012", restoredDecided.jobId());
        assertEquals("delete", restoredDecided.decision());
        assertEquals(5, restoredDecided.deletedCount());
    }

    @Test
    void runtimeStatusRoundTripAndValidation() throws Exception {
        var status = new RuntimeStatus(
                "0.9.1",
                RuntimeLifecycleState.DRAINING,
                "the console",
                true,
                2,
                List.of("task-1", "task-2"),
                "today 14:00",
                "0.10.0",
                "today 13:00",
                null,
                true,
                null,
                "success"
        );

        var json = status.toJson();
        var restored = RuntimeStatus.fromJson(json);

        assertEquals("0.9.1", restored.version());
        assertEquals(RuntimeLifecycleState.DRAINING, restored.state());
        assertEquals("the console", restored.drainingBy());
        assertTrue(restored.drainWithdrawable());
        assertEquals(2, restored.drainRemainingCount());
        assertEquals(List.of("task-1", "task-2"), restored.drainRemainingItems());
        assertEquals("today 14:00", restored.drainStartedAt());
        assertEquals("0.10.0", restored.newerVersion());
        assertTrue(restored.upgradeSupported());
        assertEquals("success", restored.lastUpgradeResult());

        // Defaults in constructor
        var minimal = new RuntimeStatus("0.9.1", null, null, false, 0, null, null, null, null, null, false, null, null);
        assertEquals(RuntimeLifecycleState.RUNNING, minimal.state());
        assertTrue(minimal.drainRemainingItems().isEmpty());

        // Missing version throws
        assertThrows(JsonFormatException.class, () -> RuntimeStatus.fromMap(Map.of("state", "running")));
    }

    @Test
    void cleanupKeptItemRoundTripAndValidation() throws Exception {
        var item = new CleanupKeptItem("token-usage", "plan", "pm-mcp-core › token-usage", "exclude", "13 Oct", "the console");
        var json = item.toJson();
        var restored = CleanupKeptItem.fromJson(json);

        assertEquals("token-usage", restored.id());
        assertEquals("plan", restored.kind());
        assertEquals("pm-mcp-core › token-usage", restored.title());
        assertEquals("exclude", restored.how());
        assertEquals("13 Oct", restored.keptAt());
        assertEquals("the console", restored.who());

        // parseList
        var listJson = "[" + json + "]";
        var list = CleanupKeptItem.parseList(listJson);
        assertEquals(1, list.size());
        assertEquals("token-usage", list.getFirst().id());

        // parseList with empty/null array
        assertTrue(CleanupKeptItem.parseList("[]").isEmpty());
        assertTrue(CleanupKeptItem.parseList("{}").isEmpty());

        // Missing fields throw
        assertThrows(JsonFormatException.class, () -> CleanupKeptItem.fromMap(Map.of("kind", "plan", "title", "t")));
        assertThrows(JsonFormatException.class, () -> CleanupKeptItem.fromMap(Map.of("id", "i", "title", "t")));
        assertThrows(JsonFormatException.class, () -> CleanupKeptItem.fromMap(Map.of("id", "i", "kind", "k")));
    }

    @Test
    void cleanupResidueRoundTripAndValidation() throws Exception {
        var residue = new CleanupResidue("temporary files", 214, 356515840L, "today 03:00");
        var json = residue.toJson();
        var restored = CleanupResidue.fromJson(json);

        assertEquals("temporary files", restored.step());
        assertEquals(214, restored.count());
        assertEquals(356515840L, restored.bytes());
        assertEquals("today 03:00", restored.lastSweep());

        assertThrows(JsonFormatException.class, () -> CleanupResidue.fromMap(Map.of("count", 1)));
    }

    @Test
    void cleanupJobOpenedEventRoundTripAndValidation() throws Exception {
        var summary = new CleanupSummary(1, 0, 0, 0, 1, 1000L, 0, null, null);
        var job = new CleanupJob("cln-0001", CleanupJobState.OPEN, null, summary, List.of(), Map.of());
        var event = new CleanupJobOpenedEvent(job);

        // Nested job format
        var restored = CleanupJobOpenedEvent.fromJson(event.toJson());
        assertEquals("cln-0001", restored.job().jobId());

        // Flat job format
        var flatEvent = CleanupJobOpenedEvent.fromMap(job.toMap());
        assertEquals("cln-0001", flatEvent.job().jobId());

        // Missing job throws
        assertThrows(JsonFormatException.class, () -> CleanupJobOpenedEvent.fromMap(Map.of("other", "value")));
    }

    @Test
    void systemSnapshotValidation() {
        var runtime = new RuntimeMetrics("0.9.1", "", "", 0, 0L, 0L, 0L, null, 0L, 0L, "", true, "off", "off", 0, 0, null, null, null, null, null, true, null);
        var work = new WorkMetrics(Map.of(), Map.of(), 0, 0, 0, List.of());
        var summary = new CleanupSummary(0, 0, 0, 0, 0, 0L, 0, null, null);

        assertThrows(JsonFormatException.class, () -> SystemSnapshot.fromMap(Map.of("work", work.toMap(), "cleanup", summary.toMap())));
        assertThrows(JsonFormatException.class, () -> SystemSnapshot.fromMap(Map.of("runtime", runtime.toMap(), "cleanup", summary.toMap())));
        assertThrows(JsonFormatException.class, () -> SystemSnapshot.fromMap(Map.of("runtime", runtime.toMap(), "work", work.toMap())));
    }

    @Test
    void runtimeMetricsCpuPercentAndValidation() throws Exception {
        // Missing version throws
        assertThrows(JsonFormatException.class, () -> RuntimeMetrics.fromMap(Map.of("uptime_seconds", 100)));

        // CPU variations: JsonNumber, String, invalid String
        var mapWithJsonNum = Map.<String, Object>of(
                "version", "0.9.1",
                "cpu_percent", new JsonNumber("12.5")
        );
        var r1 = RuntimeMetrics.fromMap(mapWithJsonNum);
        assertEquals(12.5, r1.cpuPercent());

        var mapWithString = Map.<String, Object>of(
                "version", "0.9.1",
                "cpu_percent", "8.0"
        );
        var r2 = RuntimeMetrics.fromMap(mapWithString);
        assertEquals(8.0, r2.cpuPercent());

        var mapWithInvalidString = Map.<String, Object>of(
                "version", "0.9.1",
                "cpu_percent", "invalid"
        );
        var r3 = RuntimeMetrics.fromMap(mapWithInvalidString);
        assertNull(r3.cpuPercent());
    }
}
