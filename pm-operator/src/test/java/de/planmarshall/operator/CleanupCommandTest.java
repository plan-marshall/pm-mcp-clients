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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.system.CleanupItemEntry;
import de.planmarshall.api.system.CleanupJob;
import de.planmarshall.api.system.CleanupJobState;
import de.planmarshall.api.system.CleanupProjectItem;
import de.planmarshall.api.system.CleanupSummary;
import de.planmarshall.api.testsupport.RuntimeFixture;
import de.planmarshall.operator.dialogue.ChoicePrompt;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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

    private int executeWithLocale(Clock clock, String locale, String... args) {
        var clientEnv = new ClientEnvironment(locale, false, List.of(), Map.of());
        var context = new OperatorContext(fixture.environment(), tempDir, true,
                            clientEnv, null, null, clock);
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
                            () -> assertTrue(output.contains("Kept for good 4 (cleanup --kept)"))
        );
    }

    @Test
    @DisplayName("cleanup prints technical residue when no job is open")
    void cleanupNoJob() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        var noJobJson = """
                            {
                              "open": false,
                              "next_check_at": "tomorrow 03:00",
                              "kept_count": 2
                            }
                            """;

        fixture.start((request, response) -> {
            if ("/api/v1/system/cleanup".equals(request.target())) {
                response.json(200, noJobJson);
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("cleanup");
        var output = out.toString();
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(output.contains("Cleanup · Nothing is due. The next check is tomorrow 03:00.")),
                            () -> assertTrue(output.contains("Kept for good 2 (cleanup --kept) · durations 30 days, reminder 3 days")),
                            () -> assertTrue(output.contains("Removed by itself · last sweep today 03:00")),
                            () -> assertTrue(output.contains("temporary files                       214 · 340 MB"))
        );
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
                            () -> assertTrue(output.contains("2) ◇ pm-mcp-clients › relay-auth"))
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
                            () -> assertTrue(out.toString().contains("✓ 1 items go back to the cleanup; due at the next check where past its duration."))
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

    @Test
    @DisplayName("cleanup --ignore rejects --exclude")
    void cleanupIgnoreRejectsExclude() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        int code = execute("cleanup", "--ignore", "--exclude", "plan:foo");
        assertEquals(2, code);
        assertTrue(err.toString().contains("--exclude goes with --delete only."), err.toString());
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
        assertTrue(out.toString().contains("✓ 1 items go back to the cleanup; due at the next check where past its duration."));
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
    @DisplayName("the reminder's date is counted from the clock and in German carries the dot after the day")
    void cleanupRemindCountsFromTheClockGerman() throws Exception {
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

        executeWithLocale(clock, "de", "cleanup", "--remind");

        assertTrue(out.toString().contains("16. Okt."), out.toString());
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
                            () -> assertEquals("ask me again in 3 days", choices.get(1).label()),
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
    @DisplayName("a project-filtered cleanup prints every item beyond twenty and omits the more line")
    void projectPaginationShowsAllItemsForSpecificProject() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        var job = jobOf(25);
        fixture.start((request, response) -> {
            if (request.target().startsWith("/api/v1/system/cleanup") && "GET".equals(request.method())) {
                response.json(200, JsonTree.write(job.toMap()));
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("cleanup", "--project", "pm-mcp-clients", "--no-input");

        var output = out.toString();
        assertAll(
                            () -> assertEquals(0, code, err.toString()),
                            () -> assertTrue(output.contains("25) ▸ item-25"), output),
                            () -> assertFalse(output.contains("more: cleanup --project"), output)
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
