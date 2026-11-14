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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.system.CleanupDurations;
import de.planmarshall.api.system.CleanupItemEntry;
import de.planmarshall.api.system.CleanupJob;
import de.planmarshall.api.system.CleanupJobState;
import de.planmarshall.api.system.CleanupProjectItem;
import de.planmarshall.api.system.CleanupResidue;
import de.planmarshall.api.system.CleanupSummary;
import de.planmarshall.api.testsupport.RuntimeFixture;
import de.planmarshall.operator.dialogue.ChoicePrompt;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import picocli.CommandLine;

@EnableGeneratorController
@DisplayName("CleanupCommand")
class CleanupCommandTest {

    @TempDir
    Path tempDir;

    private RuntimeFixture fixture;
    private StringWriter out;
    private StringWriter err;

    @BeforeEach
    void setUp() throws IOException {
        fixture = RuntimeFixture.create();
        out = new StringWriter();
        err = new StringWriter();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (fixture != null) {
            fixture.close();
        }
    }

    private int execute(String... args) {
        var context = new OperatorContext(fixture.environment(), tempDir, true);
        return PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err), args);
    }

    private int execute(Clock clock, String... args) {
        var context = new OperatorContext(fixture.environment(), tempDir, true,
                            ClientEnvironment.capture(fixture.environment()), null, null, clock);
        return PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err), args);
    }

    private CleanupJob sampleJob() {
        var items1 = List.of(
                            new CleanupItemEntry("fix-relay", "plan", "fix-relay", "merged (#214)", "12 Sep", 88080384L, 6, 0, null),
                            new CleanupItemEntry("docs-typo", "plan", "docs-typo", "no change", "11 Sep", 2097152L, 1, 0, null),
                            new CleanupItemEntry("relay-hardening", "epic", "relay-hardening", "completed", "10 Sep", 12582912L, 0, 0, null)
        );
        var prj1 = new CleanupProjectItem("pm-mcp-clients", 2, 1, 6, 0, 102760448L, items1);
        var items2 = List.of(
                            new CleanupItemEntry("token-usage", "plan", "token-usage", "merged (#88)", "9 Sep", 125829120L, 11, 0, null)
        );
        var prj2 = new CleanupProjectItem("pm-mcp-core", 1, 0, 11, 0, 125829120L, items2);
        var summary = new CleanupSummary(12, 2, 40, 5, 1, 1932735283L, 4, "today 03:00", "tomorrow 03:00");
        return new CleanupJob("cln-0007", CleanupJobState.OPEN, "today 03:00", summary, List.of(prj1, prj2), Map.of());
    }

    @Test
    @DisplayName("cleanup reports not running when runtime is absent")
    void cleanupNotRunning() {
        int code = execute("cleanup");
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(out.toString().contains("runtime   not running"))
        );
    }

    @Test
    @DisplayName("cleanup prints open job details")
    void cleanupOpenJob() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        var job = sampleJob();
        var jsonText = JsonTree.write(job.toMap());

        fixture.start((request, response) -> {
            if ("/api/v1/system/cleanup".equals(request.target())) {
                response.json(200, jsonText);
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("cleanup");
        var output = out.toString();
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(output.contains("Cleanup cln-0007 · due since today 03:00")),
                            () -> assertTrue(output.contains("Summary   12 plans · 2 epics · 40 logs · 5 read reports · 1 unenrolled project")),
                            () -> assertTrue(output.contains("frees 1.8 GB")),
                            () -> assertTrue(output.contains("pm-mcp-clients")),
                            () -> assertTrue(output.contains("fix-relay")),
                            () -> assertTrue(output.contains("Kept for good 4 (cleanup --kept)")),
                            () -> assertFalse(output.contains("durations"), output)
        );
    }

    @Test
    @DisplayName("the open job's footer names the runtime's durations and leaves out a sweep it does not name")
    void cleanupOpenJobFooterFollowsTheRuntime() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        int kept = Generators.integers(1, 99).next();
        var item = new CleanupItemEntry("fix-relay", "plan", "fix-relay", "merged", "12 Sep", 1L, 0, 0, null);
        var summary = new CleanupSummary(1, 0, 0, 0, 0, 1L, kept, null, "tomorrow 03:00");
        var job = new CleanupJob("cln-0007", CleanupJobState.OPEN, "today 03:00", summary,
                            List.of(new CleanupProjectItem("pm-mcp-clients", 1, 0, 0, 0, 1L, List.of(item))), Map.of());
        var body = new LinkedHashMap<>(job.toMap());
        body.put("durations", CleanupDurations.defaults().toMap());
        fixture.start((request, response) -> response.json(200, JsonTree.write(body)));

        int code = execute("cleanup");

        var output = out.toString();
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(output.contains("Kept for good " + kept
                                                + " (cleanup --kept) · durations 30 days, reminder 3 days" + System.lineSeparator()), output),
                            () -> assertFalse(output.contains("null"), output)
        );
    }

    @Test
    @DisplayName("cleanup prints the runtime's kept count, durations and residue when no job is open")
    void cleanupNoJob() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        int kept = Generators.integers(1, 99).next();
        int temp = Generators.integers(100, 999).next();
        int rows = Generators.integers(100, 999).next();
        var noJob = Map.<String, Object>of(
                            "open", false,
                            "next_check_at", "tomorrow 03:00",
                            "kept_count", kept,
                            "durations", CleanupDurations.defaults().toMap(),
                            "residue", List.of(
                                                new CleanupResidue("temp", temp, 356515840L, "today 03:00").toMap(),
                                                new CleanupResidue("rows", rows, 0L, "today 03:00").toMap()));

        fixture.start((request, response) -> {
            if ("/api/v1/system/cleanup".equals(request.target())) {
                response.json(200, JsonTree.write(noJob));
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("cleanup");
        var output = out.toString();
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(output.contains("Cleanup · Nothing is due. The next check is tomorrow 03:00."), output),
                            () -> assertTrue(output.contains("Kept for good " + kept
                                                + " (cleanup --kept) · durations 30 days, reminder 3 days"), output),
                            () -> assertTrue(output.contains("Removed by itself · last sweep today 03:00"), output),
                            () -> assertTrue(output.contains(String.format("  %-37s %3d · 340 MB", "temporary files", temp)), output),
                            () -> assertTrue(output.contains(String.format("  %-37s %3d%n", "old rows of the workspace", rows)), output)
        );
    }

    @Test
    @DisplayName("nothing due prints no figure the runtime did not send")
    void cleanupNoJobInventsNothing() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        fixture.start((request, response) ->
                            response.json(200, "{\"open\":false,\"next_check_at\":\"tomorrow 03:00\"}"));

        int code = execute("cleanup");
        var output = out.toString();
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(output.contains("Cleanup · Nothing is due. The next check is tomorrow 03:00."), output),
                            () -> assertFalse(output.contains("Kept for good"), output),
                            () -> assertFalse(output.contains("durations"), output),
                            () -> assertFalse(output.contains("Removed by itself"), output),
                            () -> assertFalse(output.contains("temporary files"), output)
        );
    }

    @Test
    @DisplayName("residue steps of different sweeps name their own time, and one not run says so")
    void residueNamesEachStepsSweep() {
        int count = Generators.integers(1, 99).next();
        var writer = new StringWriter();

        CleanupCommand.printResidue(new PrintWriter(writer), List.of(
                            new CleanupResidue("locks", count, 0L, "today 03:00"),
                            new CleanupResidue("lessons", 0, 0L, null)), "en");

        var output = writer.toString();
        assertAll(
                            () -> assertTrue(output.startsWith("Removed by itself" + System.lineSeparator()), output),
                            () -> assertTrue(output.contains(String.format("  %-37s %3d · last sweep today 03:00",
                                                "stale locks and leases, fenced generations", count)), output),
                            () -> assertTrue(output.contains("retired lesson bodies") && output.contains("not run yet"), output)
        );
    }

    @Test
    @DisplayName("durations that differ, or are never, are named per kind")
    void durationsThatDifferAreNamedPerKind() {
        int plans = Generators.integers(31, 365).next();
        var differing = new CleanupDurations(plans, 30, 30, -1, 30, 3);

        var text = CleanupCommand.durationsText("en", differing);

        assertEquals("Archived plans " + plans + " days, Archived epics 30 days, Logs of ended work 30 days, "
                            + "Read reports never, Unenrolled projects 30 days, Ask me again after 3 days", text);
    }

    @Test
    @DisplayName("cleanup --kept lists items kept for good")
    void cleanupKept() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        var keptJson = """
                            [
                              {"id": "token-usage", "kind": "plan", "title": "pm-mcp-core › token-usage", "how": "exclude", "kept_at": "13 Oct", "who": "the console"},
                              {"id": "relay-auth", "kind": "epic", "title": "pm-mcp-clients › relay-auth", "how": "ignore", "kept_at": "2 Oct", "who": "the web app (iPad)"}
                            ]
                            """;

        fixture.start((request, response) -> {
            if ("/api/v1/system/cleanup/kept".equals(request.target())) {
                response.json(200, keptJson);
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("cleanup", "--kept");
        var output = out.toString();
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(output.contains("Kept for good · 2")),
                            () -> assertTrue(output.contains("1) ▸ pm-mcp-core › token-usage")),
                            () -> assertTrue(output.contains("2) ◇ pm-mcp-clients › relay-auth")),
                            () -> assertTrue(output.contains("left out of a delete 13 Oct · the console"), output),
                            () -> assertTrue(output.contains("ignored 2 Oct · the web app (iPad)"), output)
        );
    }

    @Test
    @DisplayName("cleanup --kept refused by the runtime exits 1 and lists nothing")
    void cleanupKeptRefused() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        fixture.start((request, response) -> response.send(500, null, "{\"error\":\"internal\"}"));

        int code = execute("cleanup", "--kept");

        assertAll(
                            () -> assertEquals(1, code),
                            () -> assertFalse(out.toString().contains("Kept for good"), out.toString()),
                            () -> assertFalse(out.toString().contains("Nothing is kept"), out.toString())
        );
    }

    @Test
    @DisplayName("cleanup --release refused at the kept list sends no release")
    void cleanupReleaseRefusedAtTheKeptList() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        var puts = new AtomicInteger();
        fixture.start((request, response) -> {
            if ("PUT".equals(request.method())) {
                puts.incrementAndGet();
            }
            response.send(500, null, "{\"error\":\"internal\"}");
        });

        int code = execute("cleanup", "--release", "1");

        assertAll(
                            () -> assertEquals(1, code),
                            () -> assertEquals(0, puts.get()),
                            () -> assertFalse(out.toString().contains("go back"), out.toString())
        );
    }

    @Test
    @DisplayName("cleanup --release releases specified item")
    void cleanupRelease() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        var keptJson = """
                            [
                              {"id": "token-usage", "kind": "plan", "title": "pm-mcp-core › token-usage", "how": "exclude", "kept_at": "13 Oct", "who": "the console"},
                              {"id": "relay-auth", "kind": "epic", "title": "pm-mcp-clients › relay-auth", "how": "ignore", "kept_at": "2 Oct", "who": "the web app (iPad)"}
                            ]
                            """;

        fixture.start((request, response) -> {
            if ("/api/v1/system/cleanup/kept".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, keptJson);
            } else if ("/api/v1/system/cleanup/kept".equals(request.target()) && "PUT".equals(request.method())) {
                response.json(200, "[{\"id\":\"relay-auth\"}]");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("cleanup", "--release", "1");
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(out.toString().contains("✓ 1 item goes back to the cleanup; due at the next check where past its duration."),
                                                out.toString())
        );
    }

    @Test
    @DisplayName("cleanup --release --all --no-input requires --yes")
    void cleanupReleaseAllNoInput() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        var keptJson = """
                            [
                              {"id": "token-usage", "kind": "plan", "title": "pm-mcp-core › token-usage", "how": "exclude", "kept_at": "13 Oct", "who": "the console"}
                            ]
                            """;

        fixture.start((request, response) -> {
            if ("/api/v1/system/cleanup/kept".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, keptJson);
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("cleanup", "--release", "--all", "--no-input");
        assertAll(
                            () -> assertEquals(3, code),
                            () -> assertTrue(out.toString().contains("Needs a choice: --yes"))
        );
    }

    @Test
    @DisplayName("cleanup --remind postpones open cleanup job")
    void cleanupRemind() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        var job = sampleJob();

        fixture.start((request, response) -> {
            if ("/api/v1/system/cleanup".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, JsonTree.write(job.toMap()));
            } else if ("/api/v1/system/cleanup/cln-0007/remind".equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, "{\"status\":\"postponed\",\"remind_after_days\":3}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("cleanup", "--remind");
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(out.toString().contains("✓ Nothing was deleted. The same items come back"))
        );
    }

    @Test
    @DisplayName("cleanup --ignore keeps all items for good")
    void cleanupIgnore() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        var job = sampleJob();

        fixture.start((request, response) -> {
            if ("/api/v1/system/cleanup".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, JsonTree.write(job.toMap()));
            } else if ("/api/v1/system/cleanup/cln-0007/ignore".equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, "{\"status\":\"done\",\"decision\":\"ignore\"}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("cleanup", "--ignore");
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(out.toString().contains("✓ Nothing was deleted. 4 items are kept for good; cleanup --kept lists them, cleanup --release gives them back."))
        );
    }

    @Test
    @DisplayName("cleanup --delete --no-input requires --yes")
    void cleanupDeleteNoInput() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        var job = sampleJob();

        fixture.start((request, response) -> {
            if ("/api/v1/system/cleanup".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, JsonTree.write(job.toMap()));
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("cleanup", "--delete", "--exclude", "plan:token-usage", "--no-input");
        var output = out.toString();
        assertAll(
                            () -> assertEquals(3, code),
                            () -> assertTrue(output.contains("Delete 11 plans, 2 epics, 29 logs, 5 read reports, and 1 unenrolled project?")),
                            () -> assertTrue(output.contains("1 unchecked item is kept for good.")),
                            () -> assertTrue(output.contains("Needs a choice: --yes"))
        );
    }

    @Test
    @DisplayName("cleanup --delete --yes deletes selected items")
    void cleanupDeleteConfirmed() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        var job = sampleJob();

        fixture.start((request, response) -> {
            if ("/api/v1/system/cleanup".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, JsonTree.write(job.toMap()));
            } else if ("/api/v1/system/cleanup/cln-0007/delete".equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, "{\"status\":\"deleted\",\"freed_bytes\":1825361100}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("cleanup", "--delete", "--exclude", "plan:token-usage", "--yes");
        var output = out.toString();
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(output.contains("✓ Deleted 11 plans, 2 epics, 29 logs, 5 read reports, and 1 unenrolled project; 1.7 GB freed."))
        );
    }

    @Test
    @DisplayName("cleanup --delete refused when runtime stopping")
    void cleanupDeleteRefusedWhenStopping() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        var job = sampleJob();

        fixture.start((request, response) -> {
            if ("/api/v1/system/cleanup".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, JsonTree.write(job.toMap()));
            } else if ("/api/v1/system/cleanup/cln-0007/delete".equals(request.target()) && "POST".equals(request.method())) {
                response.send(409, null, "{\"error\":\"Runtime stopping\"}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("cleanup", "--delete", "--yes");
        assertAll(
                            () -> assertEquals(1, code),
                            () -> assertTrue(out.toString().contains("The runtime is stopping; nothing new starts."))
        );
    }

    @Test
    @DisplayName("cleanup actions when no job is open print Nothing is due")
    void cleanupActionsWhenNoJob() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        var noJobJson = "{\"open\":false,\"next_check_at\":\"tomorrow 03:00\"}";

        fixture.start((request, response) -> {
            if ("/api/v1/system/cleanup".equals(request.target())) {
                response.json(200, noJobJson);
            } else {
                response.send(404, null, "");
            }
        });

        int codeRemind = execute("cleanup", "--remind");
        assertEquals(0, codeRemind);
        assertTrue(out.toString().contains("Nothing is due. The next check is tomorrow 03:00."));

        out.getBuffer().setLength(0);
        int codeIgnore = execute("cleanup", "--ignore");
        assertEquals(0, codeIgnore);
        assertTrue(out.toString().contains("Nothing is due. The next check is tomorrow 03:00."));

        out.getBuffer().setLength(0);
        int codeDelete = execute("cleanup", "--delete");
        assertEquals(0, codeDelete);
        assertTrue(out.toString().contains("Nothing is due. The next check is tomorrow 03:00."));
    }

    @Nested
    @DisplayName("what the acts refuse")
    class Refusals {

        @ParameterizedTest(name = "cleanup {0} --exclude is refused")
        @ValueSource(strings = {"--ignore", "--remind", "--kept", "--release", "--project=pm-mcp-core",
                            "--delete --kept", "--delete --release", "--delete --all", "--delete --remind", "--delete --ignore"})
        @DisplayName("--exclude goes with --delete only and is refused with every other form, nothing sent")
        void excludeIsRefusedWithoutDelete(String form) throws Exception {
            PmOperatorTest.writeLiveRecord(fixture);
            var requests = new AtomicInteger();
            fixture.start((request, response) -> {
                requests.incrementAndGet();
                response.send(404, null, "");
            });
            var args = new ArrayList<String>(List.of("cleanup"));
            args.addAll(List.of(form.split(" ")));
            args.addAll(List.of("--exclude", "plan:" + Generators.letterStrings(4, 9).next()));

            int code = execute(args.toArray(String[]::new));

            assertAll(
                                () -> assertEquals(2, code),
                                () -> assertTrue(err.toString().contains("--exclude goes with --delete only."), err.toString()),
                                () -> assertEquals(0, requests.get())
            );
        }

        @Test
        @DisplayName("an --exclude that names no item of the open job is refused and sends no delete")
        void deleteRefusesAnExcludeOutsideTheJob() throws Exception {
            PmOperatorTest.writeLiveRecord(fixture);
            var job = sampleJob();
            var deletes = new AtomicInteger();
            fixture.start((request, response) -> {
                if (request.target().endsWith("/delete")) {
                    deletes.incrementAndGet();
                }
                response.json(200, JsonTree.write(job.toMap()));
            });
            var unknown = "plan:" + Generators.letterStrings(10, 14).next();

            int code = execute("cleanup", "--delete", "--exclude", unknown, "--yes");

            assertAll(
                                () -> assertEquals(1, code),
                                () -> assertTrue(out.toString().contains(unknown + " is no item of the open cleanup."), out.toString()),
                                () -> assertEquals(0, deletes.get())
            );
        }

        @Test
        @DisplayName("an --exclude by number or kind:id sends the item's own id once")
        void deleteSendsTheExcludedItemsIdOnce() throws Exception {
            PmOperatorTest.writeLiveRecord(fixture);
            var job = sampleJob();
            var sent = new ArrayDeque<String>();
            fixture.start((request, response) -> {
                if (request.target().endsWith("/delete")) {
                    sent.add(request.bodyText());
                    response.json(200, "{\"status\":\"deleted\"}");
                } else {
                    response.json(200, JsonTree.write(job.toMap()));
                }
            });

            int code = execute("cleanup", "--delete", "--exclude", "4", "--exclude", "plan:token-usage", "--yes");

            assertAll(
                                () -> assertEquals(0, code, err.toString()),
                                () -> assertEquals(List.of(JsonTree.write(Map.of("unchecked_ids", List.of("token-usage")))),
                                                    List.copyOf(sent)),
                                () -> assertTrue(out.toString().contains("✓ Deleted 11 plans"), out.toString())
            );
        }

        @Test
        @DisplayName("a cleanup the runtime refuses sends no remind")
        void remindRefusedAtTheJobSendsNothing() throws Exception {
            PmOperatorTest.writeLiveRecord(fixture);
            var reminds = new AtomicInteger();
            fixture.start((request, response) -> {
                if (request.target().endsWith("/remind")) {
                    reminds.incrementAndGet();
                }
                response.send(500, null, "{\"error\":\"internal\"}");
            });

            int code = execute("cleanup", "--remind");

            assertAll(
                                () -> assertEquals(1, code),
                                () -> assertEquals(0, reminds.get()),
                                () -> assertFalse(out.toString().contains("Nothing was deleted"), out.toString())
            );
        }

        @ParameterizedTest(name = "a cleanup answered with {0} is refused")
        @ValueSource(strings = {"[]", "{\"open\":false}", "{\"open\":false,\"next_check_at\":\" \"}"})
        @DisplayName("a cleanup that is no object, or names no next check, is refused rather than stood in for")
        void malformedCleanupIsRefused(String body) throws Exception {
            PmOperatorTest.writeLiveRecord(fixture);
            fixture.start((request, response) -> response.json(200, body));

            int code = execute("cleanup", "--ignore");

            assertAll(
                                () -> assertEquals(1, code),
                                () -> assertFalse(out.toString().contains("tomorrow"), out.toString()),
                                () -> assertTrue(err.toString().contains("cleanup --ignore"), err.toString())
            );
        }
    }

    @Test
    @DisplayName("cleanup --release --all --yes releases all items")
    void cleanupReleaseAllConfirmed() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        var keptJson = """
                            [
                              {"id": "token-usage", "kind": "plan", "title": "pm-mcp-core › token-usage", "how": "exclude", "kept_at": "13 Oct", "who": "the console"}
                            ]
                            """;

        fixture.start((request, response) -> {
            if ("/api/v1/system/cleanup/kept".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, keptJson);
            } else if ("/api/v1/system/cleanup/kept".equals(request.target()) && "PUT".equals(request.method())) {
                response.json(200, "[]");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("cleanup", "--release", "--all", "--yes");
        assertEquals(0, code);
        assertTrue(out.toString().contains("✓ 1 item goes back to the cleanup; due at the next check where past its duration."),
                            out.toString());
    }

    @Test
    @DisplayName("cleanup --release --all on a terminal asks once: n sends nothing, y gives every item back")
    void cleanupReleaseAllAsksOnce() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        var keptJson = """
                            [
                              {"id": "token-usage", "kind": "plan", "title": "token-usage", "how": "exclude", "kept_at": "13 Oct", "who": "the console"},
                              {"id": "relay-auth", "kind": "epic", "title": "relay-auth", "how": "ignore", "kept_at": "2 Oct", "who": "the console"}
                            ]
                            """;
        var puts = new AtomicInteger();
        fixture.start((request, response) -> {
            if ("PUT".equals(request.method())) {
                puts.incrementAndGet();
                response.json(200, "[]");
            } else {
                response.json(200, keptJson);
            }
        });

        int refused = releaseAllAnswering("n\n");
        var refusedOutput = out.toString();
        out.getBuffer().setLength(0);
        int accepted = releaseAllAnswering("y\n");

        assertAll(
                            () -> assertEquals(0, refused),
                            () -> assertTrue(refusedOutput.contains("Give all 2 kept items back to the cleanup?"), refusedOutput),
                            () -> assertFalse(refusedOutput.contains("go back"), refusedOutput),
                            () -> assertEquals(0, accepted),
                            () -> assertEquals(1, puts.get()),
                            () -> assertTrue(out.toString().contains("✓ 2 items go back to the cleanup"), out.toString())
        );
    }

    private int releaseAllAnswering(String answer) {
        var command = new CleanupCommand(new OperatorContext(fixture.environment(), tempDir, true),
                            (state, leave, writer) -> new ChoicePrompt(new BufferedReader(new StringReader(answer)),
                                                writer, "en", state).leaveLabel(leave),
                            (question, state, writer) -> null);
        var commandLine = new CommandLine(command);
        commandLine.setOut(new PrintWriter(out));
        commandLine.setErr(new PrintWriter(err));
        return commandLine.execute("--release", "--all");
    }

    @Test
    @DisplayName("a project-filtered ignore says the whole job is affected")
    void cleanupIgnoreWithAProjectSaysTheWholeJob() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        var job = sampleJob();
        fixture.start((request, response) -> {
            if ("/api/v1/system/cleanup".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, JsonTree.write(job.toMap()));
            } else if (request.target().endsWith("/ignore")) {
                response.json(200, "{\"status\":\"done\"}");
            } else {
                response.send(404, null, "");
            }
        });

        execute("cleanup", "--ignore", "--project", "pm-mcp-clients");

        assertTrue(out.toString().contains("acts on the whole job"), out.toString());
    }

    @Test
    @DisplayName("cleanup --remind names the server's date")
    void cleanupRemindNamesTheServersDate() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        var job = sampleJob();
        fixture.start((request, response) -> {
            if ("/api/v1/system/cleanup".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, JsonTree.write(job.toMap()));
            } else if (request.target().endsWith("/remind")) {
                response.json(200, "{\"status\":\"postponed\",\"remind_at\":\"2026-10-16T03:00:00Z\"}");
            } else {
                response.send(404, null, "");
            }
        });

        execute("cleanup", "--remind");

        assertTrue(out.toString().contains("The same items come back 16 Oct."), out.toString());
    }

    @Test
    @DisplayName("an ISO reminder date is rendered in the client's zone")
    void reminderDateIsRenderedInTheClientsZone() {
        assertAll(
                            () -> assertEquals("16 Oct",
                                                CleanupCommand.formatWhen("2026-10-16T03:00:00Z", "en", ZoneOffset.UTC)),
                            () -> assertEquals("15 Oct",
                                                CleanupCommand.formatWhen("2026-10-16T03:00:00Z", "en",
                                                                    ZoneId.of("America/New_York"))),
                            () -> assertEquals("16 Oct", CleanupCommand.formatWhen("16 Oct", "en", ZoneOffset.UTC))
        );
    }

    @Test
    @DisplayName("the reminder's date is counted from the clock")
    void cleanupRemindCountsFromTheClock() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        var job = sampleJob();
        fixture.start((request, response) -> {
            if ("/api/v1/system/cleanup".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, JsonTree.write(job.toMap()));
            } else if (request.target().endsWith("/remind")) {
                response.json(200, "{\"status\":\"postponed\",\"remind_after_days\":3}");
            } else {
                response.send(404, null, "");
            }
        });
        var clock = Clock.fixed(Instant.parse("2026-10-13T12:00:00Z"), ZoneOffset.UTC);

        execute(clock, "cleanup", "--remind");

        assertTrue(out.toString().contains("The same items come back 16 Oct."), out.toString());
    }

    @Test
    @DisplayName("cleanup --remind without a date names the next check")
    void cleanupRemindWithoutADate() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        var job = sampleJob();
        fixture.start((request, response) -> {
            if ("/api/v1/system/cleanup".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, JsonTree.write(job.toMap()));
            } else if (request.target().endsWith("/remind")) {
                response.json(200, "{\"status\":\"postponed\"}");
            } else {
                response.send(404, null, "");
            }
        });

        execute("cleanup", "--remind");

        assertTrue(out.toString().contains("The same items come back at the next check."), out.toString());
    }

    @Test
    @DisplayName("a project-filtered delete says it removes the whole job")
    void cleanupDeleteWithAProjectNamesTheWholeJob() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        var job = sampleJob();
        fixture.start((request, response) -> {
            if ("/api/v1/system/cleanup".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, JsonTree.write(job.toMap()));
            } else {
                response.send(404, null, "");
            }
        });

        execute("cleanup", "--delete", "--project", "pm-mcp-clients", "--no-input");

        assertTrue(out.toString().contains("The delete removes the whole job"), out.toString());
    }

    @Test
    @DisplayName("cleanup --kept reports when nothing is kept")
    void cleanupKeptEmpty() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        fixture.start((request, response) -> {
            if ("/api/v1/system/cleanup/kept".equals(request.target())) {
                response.json(200, "[]");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("cleanup", "--kept");
        assertEquals(0, code);
        assertTrue(out.toString().contains("Nothing is kept for good."));
    }

    @Test
    @DisplayName("an item's end never carries the word twice")
    void cleanupEndedWordIsNeverDoubled() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        var item = new CleanupItemEntry("fix-relay", "plan", "fix-relay", "merged (#214)",
                            "ended 12 Sep", 88080384L, 6, 0, null);
        var project = new CleanupProjectItem("pm-mcp-clients", 1, 0, 6, 0, 88080384L, List.of(item));
        var summary = new CleanupSummary(1, 0, 6, 0, 0, 88080384L, 0, "today 03:00", "tomorrow 03:00");
        var job = new CleanupJob("cln-0007", CleanupJobState.OPEN, "today 03:00", summary, List.of(project), Map.of());

        fixture.start((request, response) -> {
            if ("/api/v1/system/cleanup".equals(request.target())) {
                response.json(200, JsonTree.write(job.toMap()));
            } else {
                response.send(404, null, "");
            }
        });

        execute("cleanup");
        var output = out.toString();
        assertAll(
                            () -> assertTrue(output.contains("ended 12 Sep"), output),
                            () -> assertFalse(output.contains("ended ended"), output)
        );
    }

    @Test
    @DisplayName("the delete preview follows the items left out")
    void deletePreviewFollowsTheItemsLeftOut() {
        var item = new CleanupItemEntry("fix-relay", "plan", "fix-relay", "merged (#214)",
                            "12 Sep", 88080384L, 6, 0, null);
        var project = new CleanupProjectItem("pm-mcp-clients", 1, 0, 6, 0, 88080384L, List.of(item));
        var summary = new CleanupSummary(3, 2, 40, 5, 1, 1932735283L, 4, "today 03:00", "tomorrow 03:00");
        var job = new CleanupJob("cln-0007", CleanupJobState.OPEN, "today 03:00", summary, List.of(project), Map.of());

        var preview = CleanupCommand.deletePreview(job, List.of("fix-relay"));

        assertAll(
                            () -> assertEquals(2, preview.plans()),
                            () -> assertEquals(2, preview.epics()),
                            () -> assertEquals(34, preview.logs()),
                            () -> assertEquals(5, preview.reports()),
                            () -> assertEquals(1, preview.projects()),
                            () -> assertEquals(1844654899L, preview.freedBytes())
        );
    }

    @Test
    @DisplayName("the leave-out marks by number, id and kind:id, and puts a marked item back")
    void leaveOutMarksAndPutsBack() {
        var items = List.of(
                            new CleanupItemEntry("fix-relay", "plan", "fix-relay", "merged", "12 Sep", 1L, 0, 0, null),
                            new CleanupItemEntry("relay-auth", "epic", "relay-auth", "completed", "10 Sep", 1L, 0, 0, null)
        );
        var leftOut = new LinkedHashSet<String>();

        var unknown = CleanupCommand.toggleLeftOut(items, leftOut, "1 epic:relay-auth");
        assertAll(
                            () -> assertTrue(unknown.isEmpty(), unknown.toString()),
                            () -> assertEquals(Set.of("fix-relay", "relay-auth"), leftOut)
        );

        var freed = CleanupCommand.toggleLeftOut(items, leftOut, "1");
        assertAll(
                            () -> assertTrue(freed.isEmpty(), freed.toString()),
                            () -> assertEquals(Set.of("relay-auth"), leftOut)
        );

        var missing = CleanupCommand.toggleLeftOut(items, leftOut, "9 nope");
        assertAll(
                            () -> assertEquals(List.of("9", "nope"), missing),
                            () -> assertEquals(Set.of("relay-auth"), leftOut)
        );
    }

    @ParameterizedTest(name = "kind {0} is named {1}: and drawn ''{2}''")
    @CsvSource(delimiter = '|', value = {
                        "plan|plan|▸",
                        "epic|epic|◇",
                        "logs|logs|' '",
                        "read_reports|reports|' '",
                        "unenrolled_project|project|▪",
                        "archive|archive|' '"})
    @DisplayName("an item is named by the operator's kind word and drawn with its kind's glyph")
    void kindWordAndGlyph(String wireKind, String word, String glyph) {
        assertAll(
                            () -> assertEquals(word, CleanupCommand.kindWord(wireKind)),
                            () -> assertEquals(glyph, CleanupCommand.glyphForKind(wireKind))
        );
    }

    @Test
    @DisplayName("read reports and an unenrolled project are named reports: and project:, as the commands name them")
    void leaveOutTakesTheOperatorsKindWords() {
        var project = Generators.letterStrings(5, 10).next();
        var items = List.of(
                            new CleanupItemEntry("reports-" + project, "read_reports", "3 read reports", null, null, 0L, 0, 3, null),
                            new CleanupItemEntry(project, "unenrolled_project", project, null, null, 1L, 0, 0, null));
        var leftOut = new LinkedHashSet<String>();

        var unknown = CleanupCommand.toggleLeftOut(items, leftOut, "reports:reports-" + project + " project:" + project);

        assertAll(
                            () -> assertTrue(unknown.isEmpty(), unknown.toString()),
                            () -> assertEquals(Set.of("reports-" + project, project), leftOut)
        );
    }

    @Test
    @DisplayName("cleanup --release by kind word gives the named kept item back")
    void cleanupReleaseByKindWord() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        var keptJson = """
                            [
                              {"id": "token-usage", "kind": "plan", "title": "token-usage", "how": "exclude", "kept_at": "13 Oct", "who": "the console"},
                              {"id": "old-sandbox", "kind": "unenrolled_project", "title": "old-sandbox", "how": "ignore", "kept_at": "2 Oct", "who": "the console"}
                            ]
                            """;
        var sent = new ArrayDeque<String>();
        fixture.start((request, response) -> {
            if ("PUT".equals(request.method())) {
                sent.add(request.bodyText());
                response.json(200, "[]");
            } else {
                response.json(200, keptJson);
            }
        });

        int code = execute("cleanup", "--release", "project:old-sandbox");
        out.getBuffer().setLength(0);
        int byId = execute("cleanup", "--release", "token-usage");

        assertAll(
                            () -> assertEquals(0, code, err.toString()),
                            () -> assertEquals(0, byId, err.toString()),
                            () -> assertEquals(List.of(JsonTree.write(Map.of("item_ids", List.of("token-usage"))),
                                                JsonTree.write(Map.of("item_ids", List.of("old-sandbox")))), List.copyOf(sent)),
                            () -> assertTrue(out.toString().contains("✓ 1 item goes back"), out.toString())
        );
    }

    @Test
    @DisplayName("a kind-prefixed cleanup id opens as the bare work id")
    void kindPrefixedIdOpensAsTheWorkId() {
        var item = new CleanupItemEntry("plan:fix-relay", "plan", "fix-relay", "merged", "12 Sep", 1L, 0, 0, null);

        assertEquals("fix-relay", CleanupCommand.workId(item));
    }

    @Test
    @DisplayName("only a plan and an epic open")
    void onlyWorkOpens() {
        var plan = new CleanupItemEntry("fix-relay", "plan", "fix-relay", "merged", "12 Sep", 1L, 0, 0, null);
        var epic = new CleanupItemEntry("relay-auth", "epic", "relay-auth", "completed", "10 Sep", 1L, 0, 0, null);
        var logs = new CleanupItemEntry("relay-docs", "logs", "relay-docs", null, null, 1L, 3, 0, null);

        assertAll(
                            () -> assertEquals("plan", CleanupCommand.openTarget(plan)),
                            () -> assertEquals("epic", CleanupCommand.openTarget(epic)),
                            () -> assertNull(CleanupCommand.openTarget(logs))
        );
    }

    @Test
    @DisplayName("the printed job marks the items kept for good and shapes each kind")
    void printJobMarksAndShapesEveryKind() {
        var items = List.of(
                            new CleanupItemEntry("fix-relay", "plan", "fix-relay", "merged", "ended 12 Sep", 1L, 2, 0, null),
                            new CleanupItemEntry("relay-docs", "logs", "relay-docs", null, null, 2L, 3, 0, "kept until 24 Oct"),
                            new CleanupItemEntry("3 read reports", "read_reports", "3 read reports", null, null, 0L, 0, 3, "read before 13 Sep")
        );
        var project = new CleanupProjectItem("pm-mcp-clients", 1, 0, 3, 0, 3L, items);
        var unenrolled = new CleanupProjectItem("unenrolled", 0, 0, 0, 1, 1024L, List.of(
                            new CleanupItemEntry("legacy-portal", "unenrolled_project", "legacy-portal", null, null, 1024L, 0, 0, null)));
        var summary = new CleanupSummary(1, 0, 3, 3, 1, 1027L, 0, "today 03:00", "tomorrow 03:00");
        var job = new CleanupJob("cln-0007", CleanupJobState.OPEN, "today 03:00", summary,
                            List.of(project, unenrolled), Map.of());
        var command = new CleanupCommand(new OperatorContext(fixture.environment(), tempDir, true));
        var writer = new StringWriter();

        var printed = command.printJob(new PrintWriter(writer), job, Set.of("fix-relay"));

        var output = writer.toString();
        assertAll(
                            () -> assertEquals(4, printed.size()),
                            () -> assertTrue(output.contains("ended 12 Sep"), output),
                            () -> assertFalse(output.contains("ended ended"), output),
                            () -> assertTrue(output.contains("✎ kept for good"), output),
                            () -> assertTrue(output.contains("relay-docs"), output),
                            () -> assertTrue(output.contains("3 read reports"), output),
                            () -> assertTrue(output.contains("Unenrolled projects"), output),
                            () -> assertTrue(output.contains("legacy-portal"), output)
        );
    }

    @Test
    @DisplayName("the delete preview names the counts and the kept items")
    void previewTextNamesTheCounts() {
        var preview = new CleanupCommand.DeletePreview(11, 2, 29, 5, 1, 1806906163L);

        var text = CleanupCommand.previewText(preview, 1);

        assertAll(
                            () -> assertTrue(text.contains("Delete 11 plans, 2 epics, 29 logs, 5 read reports, and 1 unenrolled project?"), text),
                            () -> assertTrue(text.contains("This frees 1.7 GB. It cannot be undone."), text),
                            () -> assertTrue(text.contains("1 unchecked item is kept for good."), text),
                            () -> assertTrue(CleanupCommand.consentTitle(preview, 1, "en", false).endsWith("Delete?"), text),
                            () -> assertTrue(CleanupCommand.consentTitle(preview, 1, "en", true)
                                                .contains("The delete removes the whole job"), text)
        );
    }

    @Test
    @DisplayName("the decision bar offers d, r, i, x and o in that order")
    void decisionChoicesAreTheFiveKeys() {
        var choices = CleanupCommand.decisionChoices("en");

        assertAll(
                            () -> assertEquals(List.of("d", "r", "i", "x", "o"),
                                                choices.stream().map(ChoicePrompt.Choice::key).toList()),
                            () -> assertEquals("delete…", choices.get(0).label()),
                            () -> assertEquals("leave items out", choices.get(3).label())
        );
    }

    @Test
    @DisplayName("the console dialogue leaves an item out and keeps the delete open")
    void dialogLeavesAnItemOutThenKeepsIt() throws Exception {
        var job = sampleJob();
        var answers = new ArrayDeque<>(List.of("x\n", "q\n"));
        var freeText = new ArrayDeque<>(List.of("1"));
        var command = new CleanupCommand(
                            new OperatorContext(fixture.environment(), tempDir, true),
                            (state, leave, writer) -> new ChoicePrompt(
                                                new BufferedReader(new StringReader(answers.peek() == null ? "q\n" : answers.poll())),
                                                new PrintWriter(out), "en", state).leaveLabel(leave),
                            (question, state, writer) -> freeText.poll());
        PmOperatorTest.writeLiveRecord(fixture);
        fixture.start((request, response) -> response.json(200, JsonTree.write(job.toMap())));

        assertThrows(StartPrompt.StartLeftException.class,
                            () -> command.decide(new PrintWriter(out), new PrintWriter(err), job));

        var output = out.toString();
        assertAll(
                            () -> assertTrue(output.contains("✎ kept for good"), output),
                            () -> assertTrue(output.contains("Left the cleanup. Nothing was sent."), output)
        );
    }

    @Test
    @DisplayName("the dialogue's n returns to the bar and y sends the delete")
    void dialogConfirmHoldsAndSends() throws Exception {
        var job = sampleJob();
        var answers = new ArrayDeque<>(List.of("d\n", "n\n", "d\n", "y\n"));
        var command = new CleanupCommand(
                            new OperatorContext(fixture.environment(), tempDir, true),
                            (state, leave, writer) -> new ChoicePrompt(
                                                new BufferedReader(new StringReader(answers.peek() == null ? "q\n" : answers.poll())),
                                                new PrintWriter(out), "en", state).leaveLabel(leave),
                            (question, state, writer) -> "");
        PmOperatorTest.writeLiveRecord(fixture);
        fixture.start((request, response) -> {
            if ("/api/v1/system/cleanup/cln-0007/delete".equals(request.target())) {
                response.json(200, "{\"status\":\"deleted\"}");
            } else {
                response.json(200, JsonTree.write(job.toMap()));
            }
        });

        int code = command.decide(new PrintWriter(out), new PrintWriter(err), job);

        var output = out.toString();
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(output.contains("This frees"), output),
                            () -> assertTrue(output.contains("✓ Deleted"), output)
        );
    }

    @Test
    @DisplayName("n at the confirmation sends no delete")
    void dialogNoSendsNoDelete() throws Exception {
        var job = sampleJob();
        var deletes = new AtomicInteger();
        var answers = new ArrayDeque<>(List.of("d\n", "n\n", "q\n"));
        var command = new CleanupCommand(
                            new OperatorContext(fixture.environment(), tempDir, true),
                            (state, leave, writer) -> new ChoicePrompt(
                                                new BufferedReader(new StringReader(answers.peek() == null ? "q\n" : answers.poll())),
                                                new PrintWriter(out), "en", state).leaveLabel(leave),
                            (question, state, writer) -> "");
        PmOperatorTest.writeLiveRecord(fixture);
        fixture.start((request, response) -> {
            if (request.target().endsWith("/delete")) {
                deletes.incrementAndGet();
            }
            response.json(200, JsonTree.write(job.toMap()));
        });

        assertThrows(StartPrompt.StartLeftException.class,
                            () -> command.decide(new PrintWriter(out), new PrintWriter(err), job));

        assertEquals(0, deletes.get());
    }

    @Nested
    @DisplayName("the decision in the console")
    class Dialogue {

        @Test
        @DisplayName("o with a plan's number opens it read-only through the plan verb, by its bare work id")
        void openRunsThePlanVerb() throws Exception {
            var planId = Generators.letterStrings(6, 12).next().toLowerCase();
            var items = List.of(new CleanupItemEntry("plan:" + planId, "plan", planId, "merged", "12 Sep", 1L, 0, 0, null));
            var targets = new ArrayDeque<String>();
            PmOperatorTest.writeLiveRecord(fixture);
            fixture.start((request, response) -> {
                targets.add(request.target());
                response.send(404, null, "{\"error\":\"scope_not_found\"}");
            });
            var command = commandAnswering("1");

            command.openItem(new PrintWriter(out), new PrintWriter(err), items);

            assertTrue(targets.stream().anyMatch(target -> target.contains("/" + planId)
                                && !target.contains("plan:")), targets.toString());
        }

        @ParameterizedTest(name = "o answered ''{0}'' opens nothing")
        @NullAndEmptySource
        @ValueSource(strings = "q")
        @DisplayName("o left at once, answered empty, or at the end of input opens nothing and says nothing")
        void openLeftOpensNothing(@Nullable String answer) throws Exception {
            var items = List.of(new CleanupItemEntry("fix-relay", "plan", "fix-relay", "merged", "12 Sep", 1L, 0, 0, null));
            var requests = new AtomicInteger();
            PmOperatorTest.writeLiveRecord(fixture);
            fixture.start((request, response) -> {
                requests.incrementAndGet();
                response.send(404, null, "");
            });

            commandAnswering(answer).openItem(new PrintWriter(out), new PrintWriter(err), items);

            assertAll(
                                () -> assertEquals(0, requests.get()),
                                () -> assertEquals("", out.toString())
            );
        }

        @Test
        @DisplayName("o with a number past the job names it as no item of the job")
        void openOutsideTheJob() {
            var items = List.of(new CleanupItemEntry("fix-relay", "plan", "fix-relay", "merged", "12 Sep", 1L, 0, 0, null));
            var number = String.valueOf(Generators.integers(2, 99).next());

            commandAnswering(number).openItem(new PrintWriter(out), new PrintWriter(err), items);

            assertTrue(out.toString().contains(number + " is no item of the open cleanup."), out.toString());
        }

        @Test
        @DisplayName("x left at once changes nothing the delete counts")
        void leaveOutLeftChangesNothing() throws Exception {
            var job = sampleJob();
            var answers = new ArrayDeque<>(List.of("x\n", "d\n", "y\n"));
            var sent = new ArrayDeque<String>();
            var command = new CleanupCommand(
                                new OperatorContext(fixture.environment(), tempDir, true),
                                (state, leave, writer) -> new ChoicePrompt(
                                                    new BufferedReader(new StringReader(answers.peek() == null ? "q\n" : answers.poll())),
                                                    new PrintWriter(out), "en", state).leaveLabel(leave),
                                (question, state, writer) -> null);
            PmOperatorTest.writeLiveRecord(fixture);
            fixture.start((request, response) -> {
                if (request.target().endsWith("/delete")) {
                    sent.add(request.bodyText());
                }
                response.json(200, "{\"status\":\"deleted\"}");
            });

            int code = command.decide(new PrintWriter(out), new PrintWriter(err), job);

            assertAll(
                                () -> assertEquals(0, code),
                                () -> assertEquals(List.of(JsonTree.write(Map.of("unchecked_ids", List.of()))), List.copyOf(sent)),
                                () -> assertFalse(out.toString().contains("✎ kept for good"), out.toString())
            );
        }

        @ParameterizedTest(name = "the bar''s {0} sends {1}")
        @CsvSource({"r,/remind,The same items come back", "i,/ignore,items are kept for good"})
        @DisplayName("r and i of the bar send their act on the job shown, without the whole-job note")
        void dialogSendsRemindAndIgnore(String key, String act, String done) throws Exception {
            var job = sampleJob();
            var acts = new ArrayDeque<String>();
            var command = new CleanupCommand(
                                new OperatorContext(fixture.environment(), tempDir, true),
                                (state, leave, writer) -> new ChoicePrompt(
                                                    new BufferedReader(new StringReader(key + "\n")),
                                                    new PrintWriter(out), "en", state).leaveLabel(leave),
                                (question, state, writer) -> null);
            PmOperatorTest.writeLiveRecord(fixture);
            fixture.start((request, response) -> {
                if ("POST".equals(request.method())) {
                    acts.add(request.target());
                }
                response.json(200, "{\"status\":\"done\"}");
            });

            int code = command.decide(new PrintWriter(out), new PrintWriter(err), job);

            assertAll(
                                () -> assertEquals(0, code),
                                () -> assertEquals(List.of("/api/v1/system/cleanup/cln-0007" + act), List.copyOf(acts)),
                                () -> assertTrue(out.toString().contains(done), out.toString()),
                                () -> assertFalse(out.toString().contains("acts on the whole job"), out.toString())
            );
        }

        @ParameterizedTest(name = "cleanup {0} refused by the runtime exits 1")
        @CsvSource({"--remind,/remind", "--ignore,/ignore", "--delete --yes,/delete"})
        @DisplayName("an act the runtime refuses exits 1 and claims no result")
        void refusedActExitsOne(String options, String act) throws Exception {
            PmOperatorTest.writeLiveRecord(fixture);
            var job = sampleJob();
            fixture.start((request, response) -> {
                if (request.target().endsWith(act)) {
                    response.send(500, null, "{\"error\":\"internal\"}");
                } else {
                    response.json(200, JsonTree.write(job.toMap()));
                }
            });
            var args = new ArrayList<String>(List.of("cleanup"));
            args.addAll(List.of(options.split(" ")));

            int code = execute(args.toArray(String[]::new));

            assertAll(
                                () -> assertEquals(1, code),
                                () -> assertFalse(out.toString().contains("✓"), out.toString())
            );
        }

        @ParameterizedTest(name = "cleanup {0} refused at the release exits 1")
        @ValueSource(strings = {"--all", "1"})
        @DisplayName("a release the runtime refuses exits 1 and claims no item given back")
        void refusedReleaseExitsOne(String target) throws Exception {
            PmOperatorTest.writeLiveRecord(fixture);
            fixture.start((request, response) -> {
                if ("PUT".equals(request.method())) {
                    response.send(409, null, "{\"error\":\"conflict\"}");
                } else {
                    response.json(200, "[{\"id\":\"token-usage\",\"kind\":\"plan\",\"title\":\"token-usage\"}]");
                }
            });

            int code = execute("cleanup", "--release", target, "--yes");

            assertAll(
                                () -> assertEquals(1, code),
                                () -> assertFalse(out.toString().contains("go back"), out.toString())
            );
        }

        @ParameterizedTest(name = "a remind answered {0} names no date")
        @ValueSource(strings = {"[]", "not json", "{\"remind_at\":\"  \"}"})
        @DisplayName("a remind whose answer names no readable date says the items come back at the next check")
        void remindWithoutAReadableDate(String answer) throws Exception {
            PmOperatorTest.writeLiveRecord(fixture);
            var job = sampleJob();
            fixture.start((request, response) -> {
                if (request.target().endsWith("/remind")) {
                    response.json(200, answer);
                } else {
                    response.json(200, JsonTree.write(job.toMap()));
                }
            });

            int code = execute("cleanup", "--remind");

            assertAll(
                                () -> assertEquals(0, code),
                                () -> assertTrue(out.toString().contains("✓ Nothing was deleted. The same items come back at the next check."),
                                                    out.toString())
            );
        }

        @ParameterizedTest(name = "--delete on a terminal answered {0}")
        @CsvSource({"y,1", "n,0"})
        @DisplayName("--delete on a terminal asks its confirmation, naming the whole job under a project filter")
        void deleteOnATerminalAsks(String answer, int deletes) throws Exception {
            PmOperatorTest.writeLiveRecord(fixture);
            var job = sampleJob();
            var sent = new AtomicInteger();
            fixture.start((request, response) -> {
                if (request.target().endsWith("/delete")) {
                    sent.incrementAndGet();
                    response.json(200, "{\"status\":\"deleted\"}");
                } else {
                    response.json(200, JsonTree.write(job.toMap()));
                }
            });
            var command = new CleanupCommand(new OperatorContext(fixture.environment(), tempDir, true),
                                (state, leave, writer) -> new ChoicePrompt(new BufferedReader(new StringReader(answer + "\n")),
                                                    writer, "en", state).leaveLabel(leave),
                                (question, state, writer) -> null);
            var commandLine = new CommandLine(command);
            commandLine.setOut(new PrintWriter(out));
            commandLine.setErr(new PrintWriter(err));

            int code = commandLine.execute("--delete", "--project", "pm-mcp-clients");

            assertAll(
                                () -> assertEquals(0, code),
                                () -> assertEquals(deletes, sent.get()),
                                () -> assertTrue(out.toString().contains("The delete removes the whole job"), out.toString())
            );
        }

        @Test
        @DisplayName("the preview subtracts every kind left out, a read-reports row by its count")
        void deletePreviewSubtractsEveryKind() {
            int reports = Generators.integers(2, 9).next();
            var items = List.of(
                                new CleanupItemEntry("relay-hardening", "epic", "relay-hardening", "completed", "10 Sep", 10L, 0, 0, null),
                                new CleanupItemEntry("reports-a", "read_reports", "read reports", null, null, 0L, 0, reports, null),
                                new CleanupItemEntry("reports-b", "read_reports", "read reports", null, null, 0L, 0, 0, null),
                                new CleanupItemEntry("relay-docs", "logs", "relay-docs", null, null, 20L, 4, 0, null),
                                new CleanupItemEntry("legacy-portal", "unenrolled_project", "legacy-portal", null, null, 30L, 0, 0, null),
                                new CleanupItemEntry("odd", "archive", "odd", null, null, 0L, 0, 0, null));
            var summary = new CleanupSummary(0, 3, 10, 20, 2, 100L, 0, null, "tomorrow 03:00");
            var job = new CleanupJob("cln-0007", CleanupJobState.OPEN, "today 03:00", summary,
                                List.of(new CleanupProjectItem("pm-mcp-clients", 0, 3, 10, 20, 100L, items)), Map.of());

            var preview = CleanupCommand.deletePreview(job,
                                List.of("relay-hardening", "reports-a", "reports-b", "relay-docs", "legacy-portal", "odd"));

            assertEquals(new CleanupCommand.DeletePreview(0, 2, 6, 20 - reports - 1, 1, 40L), preview);
        }

        @Test
        @DisplayName("durations never offered for every kind are named per kind")
        void durationsAllNever() {
            var text = CleanupCommand.durationsText("en", new CleanupDurations(-1, -1, -1, -1, -1, 3));

            assertTrue(text.startsWith("Archived plans never, Archived epics never"), text);
        }

        @Test
        @DisplayName("a residue entry that is no object is not drawn")
        void residueEntryThatIsNoObject() throws Exception {
            PmOperatorTest.writeLiveRecord(fixture);
            fixture.start((request, response) -> response.json(200,
                                "{\"open\":false,\"next_check_at\":\"tomorrow 03:00\",\"residue\":[1]}"));

            int code = execute("cleanup");

            assertAll(
                                () -> assertEquals(0, code),
                                () -> assertFalse(out.toString().contains("Removed by itself"), out.toString())
            );
        }

        @Test
        @DisplayName("x skips blank tokens, refuses 0, and names a bare id as itself")
        void leaveOutEdges() {
            var items = List.of(new CleanupItemEntry("fix-relay", "plan", "fix-relay", "merged", "12 Sep", 1L, 0, 0, null));
            var leftOut = new LinkedHashSet<String>();

            var unknown = CleanupCommand.toggleLeftOut(items, leftOut, ", 0 fix-relay");

            assertAll(
                                () -> assertEquals(List.of("0"), unknown),
                                () -> assertEquals(Set.of("fix-relay"), leftOut),
                                () -> assertEquals("fix-relay", CleanupCommand.workId(items.getFirst()))
            );
        }

        @ParameterizedTest(name = "x answered ''{0}'' prints nothing")
        @ValueSource(strings = {"", "q"})
        @DisplayName("x left with an empty answer or the leave key marks nothing")
        void leaveOutEmptyOrLeave(String answer) throws Exception {
            var job = sampleJob();
            var answers = new ArrayDeque<>(List.of("x\n", "q\n"));
            var command = new CleanupCommand(
                                new OperatorContext(fixture.environment(), tempDir, true),
                                (state, leave, writer) -> new ChoicePrompt(
                                                    new BufferedReader(new StringReader(answers.peek() == null ? "q\n" : answers.poll())),
                                                    new PrintWriter(out), "en", state).leaveLabel(leave),
                                (question, state, writer) -> answer);

            assertThrows(StartPrompt.StartLeftException.class,
                                () -> command.decide(new PrintWriter(out), new PrintWriter(err), job));

            assertFalse(out.toString().contains("✎ kept for good"), out.toString());
        }

        @Test
        @DisplayName("x names a number past the job back and o returns to the bar")
        void dialogNamesUnknownItemsAndReturnsFromOpen() throws Exception {
            var job = sampleJob();
            var answers = new ArrayDeque<>(List.of("x\n", "o\n", "q\n"));
            var freeText = new ArrayDeque<>(List.of("99", "q"));
            var command = new CleanupCommand(
                                new OperatorContext(fixture.environment(), tempDir, true),
                                (state, leave, writer) -> new ChoicePrompt(
                                                    new BufferedReader(new StringReader(answers.peek() == null ? "q\n" : answers.poll())),
                                                    new PrintWriter(out), "en", state).leaveLabel(leave),
                                (question, state, writer) -> freeText.poll());

            assertThrows(StartPrompt.StartLeftException.class,
                                () -> command.decide(new PrintWriter(out), new PrintWriter(err), job));

            var output = out.toString();
            assertAll(
                                () -> assertTrue(output.contains("99 is no item of the open cleanup."), output),
                                () -> assertTrue(freeText.isEmpty(), "the open question was asked"),
                                () -> assertTrue(answers.isEmpty(), "the bar was asked again after o")
            );
        }

        @ParameterizedTest(name = "cleanup in the console with {0} asks the decision: {1}")
        @CsvSource({"'',true", "--no-input,false"})
        @DisplayName("cleanup in the console asks the decision after printing, and with --no-input only prints")
        void consoleAsksTheDecision(String option, boolean asks) throws Exception {
            PmOperatorTest.writeLiveRecord(fixture);
            var job = sampleJob();
            var acts = new AtomicInteger();
            fixture.start((request, response) -> {
                if ("POST".equals(request.method())) {
                    acts.incrementAndGet();
                }
                response.json(200, JsonTree.write(job.toMap()));
            });
            var context = new OperatorContext(fixture.environment(), tempDir, true);
            context.setConsoleActive(true);
            var command = new CleanupCommand(context,
                                (state, leave, writer) -> new ChoicePrompt(new BufferedReader(new StringReader("q\n")),
                                                    writer, "en", state).leaveLabel(leave),
                                (question, state, writer) -> null);
            var commandLine = new CommandLine(command);
            commandLine.setOut(new PrintWriter(out));
            commandLine.setErr(new PrintWriter(err));

            commandLine.execute(option.isEmpty() ? new String[0] : new String[]{option});

            var output = out.toString();
            assertAll(
                                () -> assertTrue(output.contains("Cleanup cln-0007 · due since today 03:00"), output),
                                () -> assertEquals(asks, output.contains("ask me again"), output),
                                () -> assertEquals(0, acts.get())
            );
        }

        private CleanupCommand commandAnswering(@Nullable String answer) {
            return new CleanupCommand(
                                new OperatorContext(fixture.environment(), tempDir, true),
                                (state, leave, writer) -> new ChoicePrompt(
                                                    new BufferedReader(new StringReader("q\n")), new PrintWriter(out), "en", state),
                                (question, state, writer) -> answer);
        }
    }

    @Test
    @DisplayName("a runtime date in German carries the dot after the day")
    void formatWhenGermanDot() {
        assertEquals("12. Sept.", CleanupCommand.formatWhen("2026-09-12T12:00:00Z", "de", ZoneOffset.UTC));
        assertEquals("12 Sep", CleanupCommand.formatWhen("2026-09-12T12:00:00Z", "en", ZoneOffset.UTC));
    }

    @Test
    @DisplayName("o names the item's view, or refuses a kind that has none")
    void dialogOpensTheItem() {
        var items = List.of(
                            new CleanupItemEntry("relay-docs", "logs", "relay-docs", null, null, 1L, 3, 0, null));
        var command = new CleanupCommand(
                            new OperatorContext(fixture.environment(), tempDir, true),
                            (state, leave, writer) -> new ChoicePrompt(
                                                new BufferedReader(new StringReader("q\n")), new PrintWriter(out), "en", state),
                            (question, state, writer) -> "1");

        command.openItem(new PrintWriter(out), new PrintWriter(err), items);

        var output = out.toString();
        assertAll(
                            () -> assertTrue(output.contains("Only a plan and an epic open"), output),
                            () -> assertTrue(output.contains("relay-docs"), output)
        );
    }

    /** A job whose one project carries {@code count} plan items. */
    private static CleanupJob jobOf(int count) {
        var items = new ArrayList<CleanupItemEntry>();
        long bytes = 0L;
        for (int i = 1; i <= count; i++) {
            var id = String.format("item-%02d", i);
            items.add(new CleanupItemEntry(id, "plan", id, "merged", "12 Sep", 1024L, 0, 0, null));
            bytes += 1024L;
        }
        var project = new CleanupProjectItem("pm-mcp-clients", count, 0, 0, 0, bytes, items);
        var summary = new CleanupSummary(count, 0, 0, 0, 0, bytes, 0, "today 03:00", "tomorrow 03:00");
        return new CleanupJob("cln-0007", CleanupJobState.OPEN, "today 03:00", summary, List.of(project), Map.of());
    }

    private CleanupCommand commandOver(ArrayDeque<String> answers, ArrayDeque<String> freeText, boolean interactive) {
        return new CleanupCommand(
                            new OperatorContext(fixture.environment(), tempDir, interactive),
                            (state, leave, writer) -> new ChoicePrompt(new BufferedReader(
                                                                    new StringReader(answers.peek() == null ? "q\n" : answers.poll())),
                                                new PrintWriter(out), "en", state).leaveLabel(leave),
                            (question, state, writer) -> freeText.poll());
    }

    @Test
    @DisplayName("a delete the runtime refused as already decided prints the decision taken")
    void decidedJobShowsTheDecisionTaken() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        var job = sampleJob();
        fixture.start((request, response) -> {
            if ("/api/v1/system/cleanup".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, JsonTree.write(job.toMap()));
            } else if (request.target().endsWith("/delete")) {
                response.send(409, null,
                                    "{\"error\":\"cleanup_decided\",\"job_id\":\"cln-0007\","
                                                        + "\"decision\":\"delete\",\"who\":\"the web app (iPad)\"}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("cleanup", "--delete", "--yes");

        assertAll(
                            () -> assertEquals(1, code),
                            () -> assertTrue(out.toString().contains(
                                                "This cleanup was decided in the web app (iPad): deleted."), out.toString())
        );
    }

    @Test
    @DisplayName("--exclude of an id the job does not carry is refused and nothing is sent")
    void unknownExcludeIsRefused() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        var job = sampleJob();
        var deletes = new AtomicInteger();
        var unknown = Generators.letterStrings(4, 12).next().toLowerCase(Locale.ROOT);
        fixture.start((request, response) -> {
            if ("/api/v1/system/cleanup".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, JsonTree.write(job.toMap()));
            } else if (request.target().endsWith("/delete")) {
                deletes.incrementAndGet();
                response.json(200, "{\"status\":\"deleted\"}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("cleanup", "--delete", "--exclude", unknown, "--yes");

        assertAll(
                            () -> assertEquals(1, code),
                            () -> assertTrue(out.toString().contains(unknown + " is no item of the open cleanup."),
                                                out.toString()),
                            () -> assertEquals(0, deletes.get(), "no delete was sent")
        );
    }

    @Test
    @DisplayName("a delete answered by neither a terminal nor --yes stops at the confirmation")
    void deleteWithoutATerminalAndWithoutYesStops() {
        var preview = new CleanupCommand.DeletePreview(11, 2, 29, 5, 1, 1806906163L);
        var command = new CleanupCommand(new OperatorContext(fixture.environment(), tempDir, false));

        int code = command.previewThenStop(new PrintWriter(out), preview, 1);

        assertAll(
                            () -> assertEquals(3, code),
                            () -> assertTrue(out.toString().contains("Delete 11 plans, 2 epics, 29 logs, 5 read reports, and 1 unenrolled project?"),
                                                out.toString()),
                            () -> assertTrue(out.toString().contains("Needs a choice: --yes"), out.toString())
        );
    }

    @Test
    @DisplayName("25 items on a 20-item page: the page names the five left and the 25th stays addressable")
    void aPageOfTwentyNamesTheRestAndKeepsItAddressable() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        var job = jobOf(25);
        var posted = new AtomicReference<String>("");
        fixture.start((request, response) -> {
            if ("/api/v1/system/cleanup".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, JsonTree.write(job.toMap()));
            } else if (request.target().endsWith("/delete")) {
                posted.set(request.bodyText());
                response.json(200, "{\"status\":\"deleted\",\"freed_bytes\":0}");
            } else {
                response.send(404, null, "");
            }
        });
        var command = commandOver(new ArrayDeque<>(List.of("x\n", "d\n", "y\n")),
                            new ArrayDeque<>(List.of("25")), true);

        int code = command.decide(new PrintWriter(out), new PrintWriter(err), job);

        var output = out.toString();
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(output.contains("20) ▸ item-20"), output),
                            () -> assertFalse(output.contains("21) ▸ item-21"), output),
                            () -> assertTrue(output.contains("5 more: cleanup --project pm-mcp-clients"), output),
                            () -> assertTrue(posted.get().contains("\"item-25\""), posted.get())
        );
    }

    @Test
    @DisplayName("r posts the reminder to the job the operator saw")
    void remindFromTheDialoguePostsTheJobId() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        var job = sampleJob();
        var targets = new ArrayList<String>();
        fixture.start((request, response) -> {
            targets.add(request.target());
            if (request.target().endsWith("/remind")) {
                response.json(200, "{\"status\":\"postponed\",\"remind_after_days\":3}");
            } else {
                response.json(200, JsonTree.write(job.toMap()));
            }
        });
        var command = commandOver(new ArrayDeque<>(List.of("r\n")), new ArrayDeque<>(), true);

        int code = command.decide(new PrintWriter(out), new PrintWriter(err), job);

        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(targets.contains("/api/v1/system/cleanup/cln-0007/remind"), targets.toString())
        );
    }

    @Test
    @DisplayName("i posts the keep to the job the operator saw")
    void ignoreFromTheDialoguePostsTheJobId() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        var job = sampleJob();
        var targets = new ArrayList<String>();
        fixture.start((request, response) -> {
            targets.add(request.target());
            if (request.target().endsWith("/ignore")) {
                response.json(200, "{\"status\":\"done\",\"decision\":\"ignore\"}");
            } else {
                response.json(200, JsonTree.write(job.toMap()));
            }
        });
        var command = commandOver(new ArrayDeque<>(List.of("i\n")), new ArrayDeque<>(), true);

        int code = command.decide(new PrintWriter(out), new PrintWriter(err), job);

        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(targets.contains("/api/v1/system/cleanup/cln-0007/ignore"), targets.toString())
        );
    }

    @Test
    @DisplayName("o on a plan and on an epic opens the work by its id")
    void dialogueOpensAPlanAndAnEpic() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        var targets = new ArrayList<String>();
        fixture.start((request, response) -> {
            targets.add(request.target());
            response.json(200, "{\"plan_id\":\"x\",\"epic_id\":\"x\",\"nodes\":[]}");
        });
        var items = List.of(
                            new CleanupItemEntry("fix-relay", "plan", "fix-relay", "merged", "12 Sep", 1L, 0, 0, null),
                            new CleanupItemEntry("relay-auth", "epic", "relay-auth", "completed", "10 Sep", 1L, 0, 0, null));
        var freeText = new ArrayDeque<>(List.of("1", "2"));
        var command = new CleanupCommand(new OperatorContext(fixture.environment(), tempDir, false),
                            (state, leave, writer) -> new ChoicePrompt(new BufferedReader(new StringReader("q\n")),
                                                new PrintWriter(out), "en", state),
                            (question, state, writer) -> freeText.poll());

        command.openItem(new PrintWriter(out), new PrintWriter(err), items);
        command.openItem(new PrintWriter(out), new PrintWriter(err), items);

        assertAll(
                            () -> assertTrue(targets.contains("/api/v1/plans/fix-relay/process"), targets.toString()),
                            () -> assertTrue(targets.stream().anyMatch(target -> target.contains("relay-auth")),
                                                targets.toString()),
                            () -> assertFalse(out.toString().contains("is no item of the open cleanup"),
                                                out.toString())
        );
    }

    @Test
    @DisplayName("an uppercase kind prefix names the same item")
    void anUppercaseKindPrefixNamesTheItem() {
        var item = new CleanupItemEntry("fix-relay", "plan", "fix-relay", "merged", "12 Sep", 1L, 0, 0, null);
        var leftOut = new LinkedHashSet<String>();

        var unknown = CleanupCommand.toggleLeftOut(List.of(item), leftOut, "PLAN:fix-relay");

        assertAll(
                            () -> assertTrue(unknown.isEmpty(), unknown.toString()),
                            () -> assertEquals(Set.of("fix-relay"), leftOut),
                            () -> assertEquals("fix-relay", CleanupCommand.workId(
                                                new CleanupItemEntry("PLAN:fix-relay", "plan", "fix-relay",
                                                                    "merged", "12 Sep", 1L, 0, 0, null)))
        );
    }

    @Test
    @DisplayName("the delete preview subtracts a logs entry, a read-reports entry and an unenrolled project")
    void deletePreviewBeyondAPlanItem() {
        var logs = new CleanupItemEntry("logs-relay-docs", "logs", "logs of relay-docs", null, null, 32505856L, 14, 0, null);
        var reports = new CleanupItemEntry("reports-clients", "read_reports", "3 read reports", null, null, 0L, 0, 3, null);
        var unenrolled = new CleanupItemEntry("legacy-portal", "unenrolled_project", "legacy-portal", null, null,
                            1181116000L, 0, 0, null);
        var project = new CleanupProjectItem("pm-mcp-clients", 0, 0, 14, 3, 32505856L, List.of(logs, reports));
        var unenrolledGroup = new CleanupProjectItem("unenrolled", 0, 0, 0, 0, 1181116000L, List.of(unenrolled));
        var summary = new CleanupSummary(0, 0, 40, 5, 1, 1213621856L, 0, "today 03:00", "tomorrow 03:00");
        var job = new CleanupJob("cln-0007", CleanupJobState.OPEN, "today 03:00", summary,
                            List.of(project, unenrolledGroup), Map.of());

        var preview = CleanupCommand.deletePreview(job,
                            List.of("logs-relay-docs", "reports-clients", "legacy-portal"));

        assertAll(
                            () -> assertEquals(26, preview.logs()),
                            () -> assertEquals(2, preview.reports()),
                            () -> assertEquals(0, preview.projects()),
                            () -> assertEquals(0L, preview.freedBytes())
        );
    }

    @Test
    @DisplayName("the leave-out mark never pushes a line past 80 columns, and a long title is cut")
    void theMarkAndTheTitleFitTheWidth() {
        var item = new CleanupItemEntry("x", "plan", "a-title-far-longer-than-eighteen",
                            "merged (#214)", "12 Sep", 88080384L, 6, 0, null);
        var project = new CleanupProjectItem("pm-mcp-clients", 1, 0, 6, 0, 88080384L, List.of(item));
        var summary = new CleanupSummary(1, 0, 6, 0, 0, 88080384L, 0, "today 03:00", "tomorrow 03:00");
        var job = new CleanupJob("cln-0007", CleanupJobState.OPEN, "today 03:00", summary, List.of(project), Map.of());

        for (var locale : List.of("en", "de")) {
            var environment = fixture.environment();
            var context = new OperatorContext(environment, tempDir, true,
                                new ClientEnvironment(locale, false, List.of(), Map.of()));
            var writer = new StringWriter();
            new CleanupCommand(context).printJob(new PrintWriter(writer), job, Set.of("x"));
            var output = writer.toString();
            assertAll(
                                () -> assertTrue(output.contains("✎"), output),
                                () -> assertTrue(output.lines()
                                                                        .filter(line -> line.stripLeading().matches("(?s)[0-9✎…].*"))
                                                                        .allMatch(line -> line.length() <= 80),
                                                    () -> "a line exceeds 80 columns: " + output),
                                () -> assertFalse(output.contains("a-title-far-longer-than-eighteen"), output)
            );
        }
    }
}
