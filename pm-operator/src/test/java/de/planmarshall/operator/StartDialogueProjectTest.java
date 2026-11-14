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
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import de.planmarshall.api.testsupport.RuntimeFixture;
import de.planmarshall.operator.transcript.TerminalTestDriver;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The project a starting verb works in when the verb names none: the working directory or the only
 * enrolled project decides it, and where neither does the projects are listed as {@code --project ?}
 * lists them. A project is never made up (operator decision, TASK-13).
 */
@EnableGeneratorController
@DisplayName("starting dialogue: the project")
class StartDialogueProjectTest {

    private static final String ENTRIES = "/entries";

    private RuntimeFixture fixture;
    private final List<String> opened = new CopyOnWriteArrayList<>();
    private String first;
    private String second;

    @BeforeEach
    void setUp() throws IOException {
        fixture = RuntimeFixture.create();
        PmOperatorTest.writeLiveRecord(fixture);
        // Ids no working directory of the build is named after or starts with.
        first = "zq" + Generators.letterStrings(6, 10).next().toLowerCase(Locale.ROOT);
        second = "zx" + Generators.letterStrings(6, 10).next().toLowerCase(Locale.ROOT);
    }

    @AfterEach
    void tearDown() throws Exception {
        fixture.close();
    }

    private void serve(String projectsBody) throws IOException {
        fixture.start((request, response) -> {
            if ("/api/v1/projects".equals(request.target())) {
                response.json(200, projectsBody);
            } else if (request.target().endsWith(ENTRIES)) {
                opened.add(request.target());
                response.send(500, null, "");
            } else {
                response.send(404, null, "");
            }
        });
    }

    private String twoProjects() {
        return "{\"all_projects\":[{\"project_id\":\"" + first + "\"},{\"project_id\":\"" + second + "\"}]}";
    }

    private Outcome run(String input, String... args) throws IOException {
        try (var driver = TerminalTestDriver.create()) {
            var environment = new HashMap<>(fixture.environment());
            environment.remove("PM_PROJECT");
            environment.put("LANG", "en_US.UTF-8");
            driver.project(environment, fixture.base().getParent());
            driver.type(input);
            driver.endOfInput();
            var code = TranscriptVerbRunner.run(driver.context(), driver.writer(), driver.writer(), args);
            return new Outcome(code, driver.render());
        }
    }

    @Test
    @DisplayName("--no-input stops with exit 3 and names the state when no project resolves, opening nothing")
    void noInputNamesTheState() throws Exception {
        serve(twoProjects());

        var outcome = run("", "plan", "add", "--description", "A change", "--no-input");

        assertAll(
                            () -> assertEquals(StartDialogue.EXIT_MISSING_INPUT, outcome.code(), outcome.rendered()),
                            () -> assertTrue(outcome.rendered().contains("start.project needs Which project?"),
                                                outcome.rendered()),
                            () -> assertEquals(List.of(), opened, "no entry is opened under a made-up project"));
    }

    @Test
    @DisplayName("the projects are listed and the entry opens under the one chosen")
    void listsAndOpensTheChosenProject() throws Exception {
        serve(twoProjects());

        var outcome = run("2\n", "plan", "add", "--description", "A change");

        assertAll(
                            () -> assertTrue(outcome.rendered().contains("Which project?"), outcome.rendered()),
                            () -> assertTrue(outcome.rendered().contains("1) " + first), outcome.rendered()),
                            () -> assertTrue(outcome.rendered().contains("2) " + second), outcome.rendered()),
                            () -> assertEquals(List.of("/api/v1/projects/" + second + ENTRIES), opened));
    }

    @Test
    @DisplayName("leaving the list opens no entry")
    void leavingTheListOpensNothing() throws Exception {
        serve(twoProjects());

        var outcome = run("q\n", "epic", "add", "--title", "An epic");

        assertAll(
                            () -> assertEquals(0, outcome.code(), outcome.rendered()),
                            () -> assertEquals(List.of(), opened));
    }

    @Test
    @DisplayName("the only project the explorer names under all_projects is the one the entry opens under")
    void theOnlyExplorerProjectIsTaken() throws Exception {
        serve("{\"favourites\":[],\"recent\":[],\"all_projects\":[{\"project_id\":\"" + first + "\"}]}");

        var outcome = run("", "plan", "add", "--description", "A change", "--no-input");

        assertAll(
                            () -> assertFalse(outcome.rendered().contains("Which project?"), outcome.rendered()),
                            () -> assertEquals(List.of("/api/v1/projects/" + first + ENTRIES), opened));
    }

    private record Outcome(int code, String rendered) {
    }
}
