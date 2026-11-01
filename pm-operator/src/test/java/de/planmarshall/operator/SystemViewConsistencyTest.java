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

    private static SystemSnapshot sampleSnapshot() {
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
                            LOOPBACK,
                            NETWORK,
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
                            new WorkerMetric("implementer", "codex", "gpt-5", WorkerState.IDLE, null));
        var jobs = Map.<String, Object>of(
                            "build", Map.of("slots_used", 1, "slots_total", 2, "queued", 1),
                            "test", Map.of("slots_used", 0, "slots_total", 1, "queued", 0),
                            "locks_held", 3);
        var models = List.of(
                            new ModelMetric("claude:opus-4.5", "planner", 42, 2020000L, 1900000L, 120000L,
                                                0, 1, false, 0),
                            new ModelMetric("claude:sonnet-4.5", "implementer", 118, 3610000L, 3200000L,
                                                410000L, 0, 0, false, 0),
                            new ModelMetric("codex:gpt-5", "implementer", 12, 0L, 0L, 0L, 1, 0, true, 12));
        var cleanup = new CleanupSummary(12, 2, 40, 5, 1, 1932735283L, 4, "today 03:00", "tomorrow 03:00");
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

    private static String listenersLine(String lang) {
        return MessageCatalogue.format(lang, "system.runtime.listeners_console",
                            Map.of("loopback", LOOPBACK, "loopback_address", LOOPBACK,
                                                "lan", NETWORK, "lan_address", NETWORK, "devices", 2));
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
                                Map.of("total", TokenFormat.formatTokens(2020000L, false, lang),
                                                    "in", TokenFormat.formatTokens(1900000L, false, lang),
                                                    "out", TokenFormat.formatTokens(120000L, false, lang)));
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
        var codex = MessageCatalogue.format("en", "system.models.not_measured", Map.of("model", "codex"));
        assertEquals(
                            fullScreenLines(snapshot, "en").stream().filter(line -> line.contains(codex)).count(),
                            printed(snapshot, "en").lines().filter(line -> line.contains(codex)).count(),
                            "both views state the reason: " + fullScreenLines(snapshot, "en"));
        assertEquals(1L,
                            printed(snapshot, "en").lines().filter(line -> line.contains(codex)).count(),
                            printed(snapshot, "en"));
    }
}
