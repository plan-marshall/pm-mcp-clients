/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.overview;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jline.terminal.impl.DumbTerminal;

import de.planmarshall.api.testsupport.FakeRuntimeServer;
import de.planmarshall.api.testsupport.RuntimeFixture;
import de.planmarshall.operator.ClientEnvironment;
import de.planmarshall.operator.ConsoleNoticeBuffer;
import de.planmarshall.operator.OperatorContext;
import de.planmarshall.operator.OverviewStateStore;
import de.planmarshall.operator.tui.TuiKeyAction;
import de.planmarshall.operator.tui.TuiLevel;
import de.planmarshall.operator.tui.TuiRow;
import de.planmarshall.operator.tui.TuiScreenDriver;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The changes level through the real key handler and the live read: {@code d} opens it, {@code Enter}
 * opens a file's hunks, a capture reads the list again (keeping an unchanged file's hunks) and reads
 * a changed file again, and a refused read says so on the status line
 * ({@code doc/design/terminal-client/16-plan-view.adoc#changes}).
 */
@DisplayName("The changes level's keys and its live read")
class OverviewChangesKeysTest {

    private static final String PLAN = "fix-relay";
    private static final String PROJECT = "pm-mcp-clients";
    private static final String LIST_PATH = "/api/v1/plans/" + PLAN + "/changes";
    private static final String FILE_PATH = LIST_PATH + "/file?path=a%2FB.java";
    private static final String PROCESS_PATH = "/api/v1/plans/" + PLAN + "/process";

    @TempDir
    Path tempDir;

    private RuntimeFixture fixture;
    private final Map<String, String> bodies = new LinkedHashMap<>();
    private final Map<String, String> refusals = new LinkedHashMap<>();
    private final List<String> reads = new ArrayList<>();
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-20T12:00:00Z"), ZoneOffset.UTC);
    private OverviewFullScreenApp app;
    private TuiScreenDriver.KeyHandler keys;

    @BeforeEach
    void setUp() throws IOException {
        fixture = RuntimeFixture.create();
        fixture.writeToken("test-token");
        var self = ProcessHandle.current();
        Files.createDirectories(fixture.paths().stateDir());
        Files.writeString(fixture.paths().runtimeRecord(), "{\"pid\":" + self.pid() + ",\"start_instant\":\""
                            + self.info().startInstant().orElseThrow() + "\"}");
        bodies.put("/api/v1/projects", "{\"projects\":[]}");
        bodies.put("/api/v1/machine/needs-operator", "{\"page\":1,\"total_pages\":1,\"has_newer\":false,"
                            + "\"has_older\":false,\"total\":0,\"items\":[]}");
        bodies.put("/api/v1/reports", "{\"unread_count\":0,\"items\":[]}");
        bodies.put("GET /api/v1/machine/needs-operator", bodies.get("/api/v1/machine/needs-operator"));
        bodies.put(PROCESS_PATH, process(12, 340, 27, 2));
        bodies.put(LIST_PATH, list("9f3c2ab41d7e", 2, 1));
        bodies.put(FILE_PATH, file("x"));
        fixture.start(this::handle);
    }

    @AfterEach
    void tearDown() throws IOException {
        fixture.close();
    }

    private static String process(int files, int insertions, int deletions, int unplanned) {
        return "{\"plan_id\":\"" + PLAN + "\",\"current_phase\":\"execute\",\"phase_state\":\"running\","
                            + "\"project\":{\"project_id\":\"" + PROJECT + "\",\"display_name\":\"" + PROJECT + "\"},"
                            + "\"change\":{\"files\":" + files + ",\"insertions\":" + insertions
                            + ",\"deletions\":" + deletions + ",\"unplanned\":" + unplanned + "},"
                            + "\"nodes\":[]}";
    }

    private static String list(String head, int added, int removed) {
        return "{\"plan_id\":\"" + PLAN + "\",\"state\":\"captured\",\"head\":\"" + head + "\","
                            + "\"base_ref\":\"main\",\"files\":[{\"path\":\"a/B.java\",\"status\":\"modified\","
                            + "\"added\":" + added + ",\"removed\":" + removed + ",\"binary\":false,"
                            + "\"unplanned\":false}]}";
    }

    private static String file(String text) {
        return "{\"path\":\"a/B.java\",\"hunks\":[{\"old_start\":1,\"old_len\":1,\"new_start\":1,\"new_len\":1,"
                            + "\"lines\":[{\"kind\":\"context\",\"text\":\"" + text + "\"}]}],\"binary\":false,"
                            + "\"truncated\":false}";
    }

    private void handle(FakeRuntimeServer.Request request, FakeRuntimeServer.Response response) throws IOException {
        var path = request.target();
        reads.add(request.method() + " " + path);
        var body = bodies.get(path);
        if (body == null) {
            body = bodies.get(request.method() + " " + path);
        }
        var refusal = refusals.get(path);
        if (refusal != null) {
            response.send(503, null, refusal);
            return;
        }
        if (body == null) {
            response.send(404, null, "{\"error\":\"target_not_found\"}");
            return;
        }
        response.json(200, body);
    }

    private void open() throws IOException {
        var env = new HashMap<>(fixture.environment());
        env.put("LANG", "en_US.UTF-8");
        var terminal = new DumbTerminal(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream());
        var context = new OperatorContext(env, tempDir, true, ClientEnvironment.capture(env), () -> terminal, null);
        app = new OverviewFullScreenApp(context, terminal,
                            OverviewFullScreenApp.InitialSelection.plan(PLAN), clock,
                            new OverviewStateStore(context.stateDirectory()), new ConsoleNoticeBuffer());
        app.loadInitialData(context.runtime(false));
        keys = app.keyHandler();
        reads.clear();
    }

    private TuiLevel level() {
        return app.stack().level();
    }

    private String rowsText() {
        return app.stack().visibleRows().stream().map(TuiRow::line).reduce("", (all, line) -> all + "\n" + line);
    }

    private String header() {
        return app.buildScreen().breadcrumb();
    }

    private String statusText() {
        return app.buildScreen().rows().stream().map(TuiRow::line)
                            .filter(line -> line.startsWith("  "))
                            .reduce("", (all, line) -> all + "\n" + line);
    }

    private void selectNaming(String text) {
        var row = app.stack().visibleRows().stream().filter(candidate -> candidate.line().contains(text))
                            .findFirst().orElseThrow(() -> new AssertionError("no row names " + text + ": " + rowsText()));
        app.stack().select(row.id());
    }

    @Nested
    @DisplayName("Keys")
    class Keys {

        @Test
        @DisplayName("d opens the list, Enter opens a file's hunks, and Esc closes them before the level")
        void opensAndCloses() throws Exception {
            open();

            keys.handle(TuiKeyAction.CHANGES);

            assertAll(
                                () -> assertEquals(TuiLevel.Kind.PLAN_CHANGES, level().kind()),
                                () -> assertTrue(header().contains("9f3c2ab"), header()),
                                () -> assertTrue(rowsText().contains("M  a/B.java"), rowsText()),
                                () -> assertTrue(rowsText().contains("+2 \u22121"), rowsText()),
                                () -> assertTrue(reads.contains("GET " + LIST_PATH), reads.toString()));

            selectNaming("a/B.java");
            keys.handle(TuiKeyAction.OPEN);

            assertAll(
                                () -> assertTrue(reads.contains("GET " + FILE_PATH), reads.toString()),
                                () -> assertTrue(rowsText().contains("@@ -1,1 +1,1 @@"), rowsText()),
                                () -> assertTrue(rowsText().contains(" x"), rowsText()));

            keys.handle(TuiKeyAction.BACK);

            assertAll(
                                () -> assertFalse(rowsText().contains("@@ -1,1 +1,1 @@"), "the hunks close first"),
                                () -> assertEquals(TuiLevel.Kind.PLAN_CHANGES, level().kind()));

            keys.handle(TuiKeyAction.BACK);

            assertEquals(TuiLevel.Kind.PLAN, level().kind(), "then the level goes");
        }

        @Test
        @DisplayName("a refused list says so on the status line and leaves the plan level")
        void refusedList() throws Exception {
            open();
            bodies.remove(LIST_PATH);

            keys.handle(TuiKeyAction.CHANGES);

            assertAll(
                                () -> assertEquals(TuiLevel.Kind.PLAN, level().kind()),
                                () -> assertTrue(statusText().contains("does not exist"), statusText()));
        }

        @Test
        @DisplayName("a refused file read says so on the status line")
        void refusedFile() throws Exception {
            open();
            keys.handle(TuiKeyAction.CHANGES);
            bodies.remove(FILE_PATH);
            selectNaming("a/B.java");

            keys.handle(TuiKeyAction.OPEN);

            assertAll(
                                () -> assertEquals(TuiLevel.Kind.PLAN_CHANGES, level().kind()),
                                () -> assertTrue(statusText().contains("does not exist"), statusText()),
                                () -> assertFalse(rowsText().contains("@@"), "no hunks of a refused read"));
        }

        @Test
        @DisplayName("a refused file read prints the catalogue's text, never the runtime's own message")
        void refusedFileUsesTheCatalogue() throws Exception {
            open();
            keys.handle(TuiKeyAction.CHANGES);
            bodies.remove(FILE_PATH);
            refusals.put(FILE_PATH, "{\"error\":\"scope_not_found\",\"message\":\"Runtime said: no such file\"}");
            selectNaming("a/B.java");

            keys.handle(TuiKeyAction.OPEN);

            assertAll(
                                () -> assertEquals(TuiLevel.Kind.PLAN_CHANGES, level().kind()),
                                () -> assertFalse(statusText().contains("Runtime said"), statusText()),
                                () -> assertTrue(statusText().contains("does not exist"), statusText()));
        }
    }

    @Nested
    @DisplayName("Live")
    class Live {

        @Test
        @DisplayName("a capture reads the list again, names the new head, and keeps an unchanged file's hunks")
        void keepsAnUnchangedFile() throws Exception {
            open();
            keys.handle(TuiKeyAction.CHANGES);
            selectNaming("a/B.java");
            keys.handle(TuiKeyAction.OPEN);
            reads.clear();
            bodies.put(LIST_PATH, list("a1b2c3d4e5f6", 2, 1));

            app.refreshActiveLevels();

            assertAll(
                                () -> assertTrue(reads.contains("GET " + LIST_PATH), reads.toString()),
                                () -> assertFalse(reads.stream().anyMatch(read -> read.endsWith(FILE_PATH)),
                                                    "the row did not change: no second read of the file"),
                                () -> assertTrue(header().contains("a1b2c3d"), header()),
                                () -> assertTrue(rowsText().contains("@@ -1,1 +1,1 @@"), "the hunks stay"));
        }

        @Test
        @DisplayName("a capture whose row changed reads the file again")
        void readsAChangedFile() throws Exception {
            open();
            keys.handle(TuiKeyAction.CHANGES);
            selectNaming("a/B.java");
            keys.handle(TuiKeyAction.OPEN);
            reads.clear();
            bodies.put(LIST_PATH, list("a1b2c3d4e5f6", 5, 1));
            bodies.put(FILE_PATH, file("y"));

            app.refreshActiveLevels();

            assertAll(
                                () -> assertTrue(reads.stream().anyMatch(read -> read.endsWith(FILE_PATH)),
                                                    reads.toString()),
                                () -> assertTrue(rowsText().contains(" y"), rowsText()),
                                () -> assertTrue(rowsText().contains("+5"), rowsText()));
        }

        @Test
        @DisplayName("a capture whose figures read did not land keeps the plan's last figures")
        void keepsTheFigures() throws Exception {
            open();
            keys.handle(TuiKeyAction.CHANGES);
            bodies.put(LIST_PATH, list("a1b2c3d4e5f6", 2, 1));
            bodies.remove(PROCESS_PATH);

            app.refreshActiveLevels();

            assertAll(
                                () -> assertTrue(header().contains("a1b2c3d"), header()),
                                () -> assertTrue(header().contains("+340"), header()));
        }
    }
}
