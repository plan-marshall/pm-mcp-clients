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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import de.cuioss.test.generator.junit.EnableGeneratorController;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.jline.terminal.impl.DumbTerminal;

import de.planmarshall.api.overview.NeedsOperatorPage;
import de.planmarshall.api.testsupport.RuntimeFixture;
import de.planmarshall.operator.ClientEnvironment;
import de.planmarshall.operator.ConsoleNoticeBuffer;
import de.planmarshall.operator.OperatorContext;
import de.planmarshall.operator.OverviewStateStore;
import de.planmarshall.operator.tui.TuiLevel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The "Needs you" rows follow the item, never their position: an item that arrives above the
 * selected row must not move the selection or what {@code !} opens
 * ({@code doc/design/shared/needs-and-find.adoc#waits}).
 */
@EnableGeneratorController
@DisplayName("What waits: the selection follows the item")
class NeedsSelectionTest {

    private static final String FIRST_PAGE = """
                    {"page":1,"total_pages":2,"has_newer":false,"has_older":true,"total":3,"items":[
                      {"kind":"decision","link":"/api/v1/decisions/Q-001","plan":{"id":"plan-a","title":"Plan A"},
                       "since":"2026-10-16T15:05:00Z","blocks":"plan"},
                      {"kind":"decision","link":"/api/v1/decisions/Q-002","plan":{"id":"plan-b","title":"Plan B"},
                       "since":"2026-10-16T15:06:00Z","blocks":"plan"}]}
                    """;

    private static final String SECOND_PAGE = """
                    {"page":1,"total_pages":2,"has_newer":false,"has_older":true,"total":3,"items":[
                      {"kind":"condition","link":"/api/v1/plans/plan-x","plan":{"id":"plan-x","title":"Plan X"},
                       "since":"2026-10-16T15:00:00Z","blocks":"plan"},
                      {"kind":"decision","link":"/api/v1/decisions/Q-001","plan":{"id":"plan-a","title":"Plan A"},
                       "since":"2026-10-16T15:05:00Z","blocks":"plan"},
                      {"kind":"decision","link":"/api/v1/decisions/Q-002","plan":{"id":"plan-b","title":"Plan B"},
                       "since":"2026-10-16T15:06:00Z","blocks":"plan"}]}
                    """;

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-16T16:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path tempDir;

    private RuntimeFixture fixture;
    private DumbTerminal terminal;

    @BeforeEach
    void setUp() throws Exception {
        fixture = RuntimeFixture.create();
        writeLiveRecord();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (fixture != null) {
            fixture.close();
        }
    }

    @Test
    @DisplayName("an item inserted above the selection leaves ! on the item the operator chose")
    void insertedItemKeepsTheSelection() throws Exception {
        var reads = new AtomicInteger();
        fixture.start((request, response) -> {
            if ("/api/v1/machine/needs-operator".equals(request.target())) {
                response.json(200, reads.getAndIncrement() == 0 ? FIRST_PAGE : SECOND_PAGE);
            } else if ("/api/v1/status".equals(request.target())) {
                response.json(200, """
                                    {"status":"ready","onboarded":true,"version":"0.9.0","needs_operator":{}}
                                    """);
            } else if ("/api/v1/projects".equals(request.target())) {
                response.json(200, "{\"projects\":[]}");
            } else if ("/api/v1/reports".equals(request.target())) {
                response.json(200, "{\"unread_count\":0}");
            } else {
                response.send(404, null, "");
            }
        });

        var context = context();
        var app = new OverviewFullScreenApp(context, terminal, OverviewFullScreenApp.InitialSelection.DEFAULT,
                            CLOCK, new OverviewStateStore(context.stateDirectory()), new ConsoleNoticeBuffer());
        app.loadInitialData(context.runtime(true));

        var chosen = rowOf(app, "Plan B");
        app.stack().select(chosen);
        assertEquals(chosen, app.stack().selected().id());

        // The runtime serves a page with an item above B; the view rebuilds the level from it.
        app.applyRefreshedData(TuiLevel.Kind.OVERVIEW, null, null, null);

        var selected = app.stack().selected();
        assertNotNull(selected);
        assertEquals(chosen, selected.id(), "the selection follows the item, not its row");
        var item = app.selectedNeedsItem();
        assertNotNull(item, "! must still find what waits in the selected row");
        assertEquals("/api/v1/decisions/Q-002", item.link());
    }

    @Test
    @DisplayName("the row id is the item's identity, not its position")
    void rowIdIsTheIdentity() throws Exception {
        var page = NeedsOperatorPage.fromJson(FIRST_PAGE);
        var level = new ProjectsOverviewLevel(List.of(), page);
        var rows = level.rows("en", CLOCK);
        assertEquals("/api/v1/decisions/Q-001", level.needsItemOf(rows.get(1).id()).link());
        assertEquals("/api/v1/decisions/Q-002", level.needsItemOf(rows.get(2).id()).link());
    }

    /**
     * Marks the fixture's runtime as live, as every operator test that reads the runtime does.
     *
     * @throws IOException when the record cannot be written
     */
    private void writeLiveRecord() throws IOException {
        var self = ProcessHandle.current();
        Files.createDirectories(fixture.paths().stateDir());
        Files.writeString(fixture.paths().runtimeRecord(), "{\"pid\":" + self.pid() + ",\"start_instant\":\""
                            + self.info().startInstant().orElseThrow() + "\"}");
    }
    private static String rowOf(OverviewFullScreenApp app, String title) {
        return app.stack().visibleRows().stream()
                            .filter(row -> row.line().contains(title))
                            .findFirst()
                            .orElseThrow(() -> new AssertionError("no row names " + title
                                                + ": " + app.stack().visibleRows()))
                            .id();
    }

    private OperatorContext context() throws IOException {
        var environment = fixture.environment();
        var rendered = new ByteArrayOutputStream();
        var created = new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered);
        this.terminal = created;
        return new OperatorContext(environment, tempDir, true,
                            ClientEnvironment.capture(environment), () -> created, null);
    }
}
