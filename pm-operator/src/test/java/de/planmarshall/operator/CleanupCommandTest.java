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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.system.CleanupItemEntry;
import de.planmarshall.api.system.CleanupJob;
import de.planmarshall.api.system.CleanupJobState;
import de.planmarshall.api.system.CleanupProjectItem;
import de.planmarshall.api.system.CleanupSummary;
import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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

    private CleanupJob sampleJob() {
        var items1 = List.of(
                            new CleanupItemEntry("fix-relay", "plan", "fix-relay", "merged (#214)", "12 Sep", 88080384L, 6, 0, null),
                            new CleanupItemEntry("docs-typo", "plan", "docs-typo", "no change", "11 Sep", 2097152L, 1, 0, null),
                            new CleanupItemEntry("relay-hardening", "epic", "relay-hardening", "completed", "10 Sep", 12582912L, 0, 0, null)
        );
        var prj1 = new CleanupProjectItem("pm-mcp-clients", 2, 1, 6, 0, 102760448L, items1);
        var summary = new CleanupSummary(12, 2, 40, 5, 1, 1932735283L, 4, "today 03:00", "tomorrow 03:00");
        return new CleanupJob("cln-0007", CleanupJobState.OPEN, "today 03:00", summary, List.of(prj1), Map.of());
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
                            () -> assertTrue(out.toString().contains("✓ Nothing was deleted. The same items come back 16 Oct."))
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
                            () -> assertTrue(out.toString().contains("✓ Nothing was deleted. 3 items are kept for good; cleanup --kept lists them, cleanup --release gives them back."))
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
                            () -> assertTrue(output.contains("Delete 12 plans, 2 epics, 29 logs, 5 read reports, and 1 unenrolled project?")),
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
                            () -> assertTrue(output.contains("✓ Deleted 12 plans, 2 epics, 29 logs, 5 read reports, and 1 unenrolled project; 1.7 GB freed."))
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
        assertEquals(1, code);
        assertTrue(err.toString().contains("--exclude is for --delete only"));
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
}
