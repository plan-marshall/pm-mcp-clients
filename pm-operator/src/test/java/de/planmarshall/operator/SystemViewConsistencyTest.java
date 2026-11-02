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
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.report.TokenFormat;
import de.planmarshall.api.system.CleanupSummary;
import de.planmarshall.api.system.ModelMetric;
import de.planmarshall.api.system.RuntimeMetrics;
import de.planmarshall.api.system.SystemSnapshot;
import de.planmarshall.api.system.SystemWindow;
import de.planmarshall.api.system.WorkMetrics;
import de.planmarshall.api.system.WorkerMetric;
import de.planmarshall.api.system.WorkerState;
import de.planmarshall.api.testsupport.RuntimeFixture;
import de.planmarshall.operator.system.SystemLevel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@EnableGeneratorController
@DisplayName("SystemSnapshot renders one model in the printed and the full-screen view")
class SystemViewConsistencyTest {

    private static final String LOOPBACK = "127.0.0.1:7420";
    private static final String NETWORK = "192.168.1.20:7421";

    private final String version = Generators.letterStrings(3, 6).next();
    private final String build = Generators.letterStrings(6, 12).next();
    private final String startedAt = Generators.letterStrings(4, 8).next();
    private final String measuredAt = Generators.letterStrings(3, 5).next();
    private final String plannerRole = Generators.letterStrings(4, 8).next();
    private final String implementerRole = Generators.letterStrings(4, 8).next();
    private final String claudeHarness = Generators.letterStrings(3, 6).next();
    private final String codexHarness = Generators.letterStrings(3, 6).next();
    private final String firstModel = Generators.letterStrings(3, 6).next();
    private final String secondModel = Generators.letterStrings(3, 6).next();
    private final String thirdModel = Generators.letterStrings(3, 6).next();
    private final String planTitle = Generators.letterStrings(4, 10).next();
    private final String secondPlanTitle = Generators.letterStrings(4, 10).next();
    private final int pairedDevices = Generators.integers(1, 9).next();
    private final int calls = Generators.integers(1, 500).next();
    private final long totalTokens = Generators.integers(1_000, 9_000_000).next();
    private final long inputTokens = totalTokens / 2;
    private final long outputTokens = totalTokens - inputTokens;

    @TempDir
    Path tempDir;

    private RuntimeFixture fixture;

    @BeforeEach
    void setUp() throws IOException {
        fixture = RuntimeFixture.create();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (fixture != null) {
            fixture.close();
        }
    }

    private SystemSnapshot sampleSnapshot() {
        var uptimeSeconds = Generators.integers(60, 100_000).next().longValue();
        var heapUsed = Generators.integers(1_000_000, 1_000_000_000).next().longValue();
        var heapMax = heapUsed * 2;
        var resident = Generators.integers(1_000_000, 1_000_000_000).next().longValue();
        var cpuPercent = Generators.integers(1, 100).next().doubleValue();
        var storeBytes = Generators.integers(1_000_000, 1_000_000_000).next().longValue();
        var diskFree = Generators.integers(1_000_000, 1_000_000_000).next().longValue();
        var runningPlans = Generators.integers(1, 9).next();
        var waitingPlans = Generators.integers(1, 9).next();
        var heldPlans = Generators.integers(1, 9).next();
        var pausedPlans = Generators.integers(1, 9).next();
        var buildSlots = Generators.integers(1, 4).next();
        var locksHeld = Generators.integers(1, 9).next();
        var plansCleaned = Generators.integers(0, 40).next();
        var runtime = new RuntimeMetrics(
                            version,
                            build,
                            startedAt,
                            uptimeSeconds,
                            heapUsed,
                            heapMax,
                            resident,
                            cpuPercent,
                            storeBytes,
                            diskFree,
                            measuredAt,
                            true,
                            LOOPBACK,
                            NETWORK,
                            pairedDevices,
                            0,
                            null,
                            "0.10.0",
                            "2026-10-16",
                            "today 08:14",
                            "/api/v1/system/release-notes?version=0.10.0",
                            true,
                            null);
        var work = new WorkMetrics(
                            Map.of("running", runningPlans, "waiting_for_you", waitingPlans,
                                                "held", heldPlans, "paused", pausedPlans),
                            Map.of("running", runningPlans, "waiting_for_you", waitingPlans),
                            runningPlans,
                            waitingPlans,
                            runningPlans,
                            List.of());
        var workers = List.of(
                            new WorkerMetric(plannerRole, claudeHarness, firstModel, WorkerState.BUSY, planTitle),
                            new WorkerMetric(implementerRole, claudeHarness, secondModel, WorkerState.BUSY,
                                                secondPlanTitle),
                            new WorkerMetric(implementerRole, codexHarness, thirdModel, WorkerState.IDLE, null));
        var jobs = Map.<String, Object>of(
                            "build", Map.of("slots_used", 1, "slots_total", buildSlots, "queued", 1),
                            "test", Map.of("slots_used", 0, "slots_total", 1, "queued", 0),
                            "locks_held", locksHeld);
        var models = List.of(
                            new ModelMetric(claudeHarness + ":" + firstModel, plannerRole, calls, totalTokens,
                                                inputTokens, outputTokens, 0, 1, false, 0),
                            new ModelMetric(claudeHarness + ":" + secondModel, implementerRole, calls,
                                                totalTokens, inputTokens, outputTokens, 0, 0, false, 0),
                            new ModelMetric(codexHarness + ":" + thirdModel, implementerRole, calls, 0L, 0L, 0L,
                                                1, 0, true, calls));
        var cleanup = new CleanupSummary(plansCleaned, 2, 40, 5, 1, 1932735283L, 4, "today 03:00",
                            "tomorrow 03:00");
        return new SystemSnapshot(runtime, work, workers, jobs, models, cleanup, SystemWindow.HOUR, null);
    }

    private OperatorContext context(String lang) {
        var environment = new HashMap<>(fixture.environment());
        environment.put("LANG", lang);
        environment.remove("LC_ALL");
        environment.remove("LC_MESSAGES");
        return new OperatorContext(environment, tempDir, true, ClientEnvironment.capture(environment));
    }

    private String printed(SystemSnapshot snapshot, String lang) {
        var out = new StringWriter();
        new SystemCommand(context(lang)).printSnapshot(new PrintWriter(out), snapshot);
        return out.toString();
    }

    private static List<String> fullScreenLines(SystemSnapshot snapshot, String lang) {
        return SystemLevel.root(snapshot, snapshot.window(), "model", null).rows(lang).stream()
                            .flatMap(row -> {
                                var lines = new ArrayList<String>();
                                lines.add(row.line());
                                if (row.details() != null) {
                                    lines.addAll(List.of(row.details().split("\n")));
                                }
                                return lines.stream();
                            })
                            .map(String::strip)
                            .toList();
    }

    private String listenersLine(String lang) {
        return MessageCatalogue.format(lang, "system.runtime.listeners_console",
                            Map.of("loopback", LOOPBACK, "loopback_address", LOOPBACK,
                                                "lan", NETWORK, "lan_address", NETWORK, "devices", pairedDevices));
    }

    @Test
    @DisplayName("both views show the runtime's listeners as the catalogue states them")
    void bothViewsShowTheSameListeners() {
        var snapshot = sampleSnapshot();
        for (var lang : List.of("en", "de")) {
            var expected = listenersLine(lang);
            assertTrue(fullScreenLines(snapshot, lang).contains(expected),
                                lang + " full screen: " + fullScreenLines(snapshot, lang));
            assertTrue(printed(snapshot, lang).contains(expected),
                                lang + " printed: " + printed(snapshot, lang));
        }
    }

    @Test
    @DisplayName("both views show the same token units, the split of every model and of the summary")
    void bothViewsShowTheSameTokenUnits() {
        var snapshot = sampleSnapshot();
        for (var lang : List.of("en", "de")) {
            var split = MessageCatalogue.format(lang, "common.tokens.split",
                                Map.of("total", TokenFormat.formatTokens(totalTokens, false, lang),
                                                    "in", TokenFormat.formatTokens(inputTokens, false, lang),
                                                    "out", TokenFormat.formatTokens(outputTokens, false, lang)));
            var printedText = printed(snapshot, lang);
            assertTrue(printedText.contains(split), lang + " printed: " + printedText);
            assertTrue(fullScreenLines(snapshot, lang).stream().anyMatch(line -> line.contains(split)),
                                lang + " full screen: " + fullScreenLines(snapshot, lang));
        }
    }

    @Test
    @DisplayName("both views say a model's tokens are not measured once, with the harness that left them out")
    void bothViewsNameTheReasonOnce() {
        var snapshot = sampleSnapshot();
        var codex = MessageCatalogue.format("en", "system.models.not_measured",
                            Map.of("model", codexHarness));
        assertEquals(
                            fullScreenLines(snapshot, "en").stream().filter(line -> line.contains(codex)).count(),
                            printed(snapshot, "en").lines().filter(line -> line.contains(codex)).count(),
                            "both views state the reason: " + fullScreenLines(snapshot, "en"));
        assertEquals(1L,
                            printed(snapshot, "en").lines().filter(line -> line.contains(codex)).count(),
                            printed(snapshot, "en"));
    }
}
