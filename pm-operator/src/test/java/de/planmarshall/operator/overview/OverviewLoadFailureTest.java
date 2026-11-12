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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.jline.terminal.impl.DumbTerminal;

import de.planmarshall.api.resource.ErrorTexts;
import de.planmarshall.api.testsupport.RuntimeFixture;
import de.planmarshall.operator.ClientEnvironment;
import de.planmarshall.operator.ConsoleNoticeBuffer;
import de.planmarshall.operator.OperatorContext;
import de.planmarshall.operator.OverviewStateStore;
import de.planmarshall.operator.tui.TuiRow;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * A refused read of the overview's own data says so: the level shows the refusal text and never the
 * empty-state text ({@code doc/design/terminal-client/15-overview.adoc}, PLAN-33 finding 2b).
 */
@EnableGeneratorController
@DisplayName("The overview says when a read was refused")
class OverviewLoadFailureTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-16T16:00:00Z"), ZoneOffset.UTC);
    private static final String REFUSAL = "{\"error\":\"cli_only\",\"message\":\"not from an interactive terminal\"}";

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
        if (terminal != null) {
            terminal.close();
        }
        if (fixture != null) {
            fixture.close();
        }
    }

    @Test
    @DisplayName("a refused projects read shows the refusal, not 'Nothing runs'")
    void refusedProjectsReadShowsTheRefusal() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/projects".equals(request.target())) {
                response.send(403, null, REFUSAL);
            } else if ("/api/v1/machine/needs-operator".equals(request.target())) {
                response.json(200, "{\"page\":1,\"total_pages\":1,\"has_older\":false,\"total\":0,\"items\":[]}");
            } else if ("/api/v1/status".equals(request.target())) {
                response.json(200, """
                                    {"status":"ready","onboarded":true,"version":"0.9.0","needs_operator":{}}
                                    """);
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

        var rows = app.stack().visibleRows();
        var text = lines(rows);
        assertTrue(text.contains(ErrorTexts.title("en", "cli_only")), "the refusal title is missing: " + text);
        assertFalse(text.contains("Nothing runs"), "the empty-state text must never stand for a refusal: " + text);
    }

    @Test
    @DisplayName("a refused needs read shows the refusal beside the projects")
    void refusedNeedsReadShowsTheRefusal() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/projects".equals(request.target())) {
                response.json(200, """
                                    {"projects":[{"project_id":"pm-mcp-clients","display_name":"pm-mcp-clients",
                                     "active_plans_count":1,"active_epics_count":0,"needs_operator_count":0,
                                     "setup_needs_operator":false,"last_activity_at":"2026-10-16T15:36:00Z"}]}
                                    """);
            } else if ("/api/v1/machine/needs-operator".equals(request.target())) {
                response.send(403, null, REFUSAL);
            } else if ("/api/v1/status".equals(request.target())) {
                response.json(200, """
                                    {"status":"ready","onboarded":true,"version":"0.9.0","needs_operator":{}}
                                    """);
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

        var text = lines(app.stack().visibleRows());
        assertTrue(text.contains(ErrorTexts.title("en", "cli_only")), "the refusal is missing: " + text);
        assertTrue(text.contains("pm-mcp-clients"), "the projects keep their rows: " + text);
    }

    private static String lines(List<TuiRow> rows) {
        return String.join("\n", rows.stream().map(TuiRow::line).toList());
    }

    private void writeLiveRecord() throws IOException {
        var self = ProcessHandle.current();
        Files.createDirectories(fixture.paths().stateDir());
        Files.writeString(fixture.paths().runtimeRecord(), "{\"pid\":" + self.pid() + ",\"start_instant\":\""
                            + self.info().startInstant().orElseThrow() + "\"}");
    }

    private OperatorContext context() throws IOException {
        var environment = fixture.environment();
        var rendered = new ByteArrayOutputStream();
        terminal = new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered);
        var created = terminal;
        return new OperatorContext(environment, tempDir, true,
                            ClientEnvironment.capture(environment), () -> created, null);
    }
}
