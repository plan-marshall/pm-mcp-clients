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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import org.jspecify.annotations.Nullable;

import de.planmarshall.mock.MockRuntime;
import de.planmarshall.mock.MockRuntimeConfig;
import de.planmarshall.mock.StandardScenarios;
import de.planmarshall.operator.transcript.TerminalTestDriver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The acts of the folded verb {@code git-source} through their design verb: a pull request is
 * adopted and the local changes are captured as sources of {@code plan add}
 * ({@code --pull-request}, {@code --local-changes}; 18-starting.adoc), against the start entry of
 * the mock runtime. Its observation, screening and acceptance are states of the start, never
 * options of a verb.
 */
@EnableGeneratorController
@DisplayName("Git sources through plan add --pull-request and --local-changes")
class GitSourceCommandTest {

    private static final String PROJECT = "pm-mcp-clients";
    private static final String ENTRY = "ent-3f9a2c41";

    /**
     * @return the number of a pull request; the case state decides what the runtime observes
     */
    private static String pullRequestNumber() {
        return String.valueOf(Generators.integers(1, 99_999).next());
    }

    /**
     * @param stateId a case state of the projects-and-start scenario
     * @return the scenario reference that starts the mock runtime at that state
     */
    private static String caseOf(String stateId) {
        return StandardScenarios.PROJECTS_AND_START + "#" + stateId;
    }

    @Test
    @DisplayName("plan add --pull-request stores the pull request with its changed files untrusted and starts")
    void pullRequestAdopted() throws Exception {
        var outcome = run(StandardScenarios.START_PULL_REQUEST, null,
                            "plan", "add", "--project", PROJECT, "--pull-request", "214", "--no-input");

        assertAll(
                            () -> assertEquals(0, outcome.code(), outcome.rendered()),
                            () -> assertTrue(outcome.rendered().contains("untrusted"), outcome.rendered()),
                            () -> assertTrue(outcome.rendered().contains("has started"), outcome.rendered()));
    }

    @Test
    @DisplayName("plan add --local-changes where the landing commits nothing stops with exit 3")
    void localChangesWithheld() throws Exception {
        var outcome = run(StandardScenarios.START_LOCAL_CHANGES, null,
                            "plan", "add", "--project", PROJECT, "--local-changes", "--no-input");

        assertAll(
                            () -> assertEquals(StartDialogue.EXIT_MISSING_INPUT, outcome.code()),
                            () -> assertTrue(outcome.rendered().contains("landing.files"), outcome.rendered()));
    }

    @Test
    @DisplayName("a flagged external source is accepted with the operator's reason and the plan starts")
    void flaggedSourceAccepted() throws Exception {
        var reason = Generators.letterStrings(8, 24).next();

        var outcome = run(StandardScenarios.START_ISSUE_FAILED, "r\nu\n" + reason + "\nr\n\n\n",
                            "plan", "add", "--project", PROJECT, "--entry", ENTRY);

        assertAll(
                            () -> assertEquals(0, outcome.code(), outcome.rendered()),
                            () -> assertTrue(outcome.rendered().contains("flagged"), outcome.rendered()),
                            () -> assertTrue(outcome.rendered().contains(reason), outcome.rendered()),
                            () -> assertTrue(outcome.rendered().contains("has started"), outcome.rendered()));
    }

    @Test
    @DisplayName("a flagged external source stops a --no-input start with exit 3")
    void flaggedSourceStopsNoInput() throws Exception {
        var outcome = run(StandardScenarios.START_ISSUE_FAILED, null,
                            "plan", "add", "--project", PROJECT, "--entry", ENTRY, "--no-input");

        assertEquals(StartDialogue.EXIT_MISSING_INPUT, outcome.code(), outcome.rendered());
    }

    @Test
    @DisplayName("plan add --pull-request whose pull request is not open stops a --no-input start with exit 3")
    void pullRequestNotOpen() throws Exception {
        var outcome = run(caseOf("start.pull-request-not-open"), null,
                            "plan", "add", "--project", PROJECT, "--entry", ENTRY, "--pull-request", pullRequestNumber(),
                            "--no-input");

        assertAll(
                            () -> assertEquals(StartDialogue.EXIT_MISSING_INPUT, outcome.code(), outcome.rendered()),
                            () -> assertTrue(outcome.rendered().contains("not open"), outcome.rendered()),
                            () -> assertFalse(outcome.rendered().contains("has started"), outcome.rendered()));
    }

    @Test
    @DisplayName("plan add --pull-request whose pull request is flagged is accepted with the operator's reason")
    void flaggedPullRequestAccepted() throws Exception {
        var reason = Generators.letterStrings(8, 24).next();

        var outcome = run(caseOf("start.pull-request-flagged"), "u\n" + reason + "\n\n\n\n",
                            "plan", "add", "--project", PROJECT, "--entry", ENTRY, "--pull-request", pullRequestNumber());

        assertAll(
                            () -> assertEquals(0, outcome.code(), outcome.rendered()),
                            () -> assertTrue(outcome.rendered().contains("flagged"), outcome.rendered()),
                            () -> assertTrue(outcome.rendered().contains(reason), outcome.rendered()),
                            () -> assertTrue(outcome.rendered().contains("has started"), outcome.rendered()));
    }

    @Test
    @DisplayName("plan add --pull-request whose pull request is flagged stops a --no-input start with exit 3")
    void flaggedPullRequestStopsNoInput() throws Exception {
        var outcome = run(caseOf("start.pull-request-flagged"), null,
                            "plan", "add", "--project", PROJECT, "--entry", ENTRY, "--pull-request", pullRequestNumber(),
                            "--no-input");

        assertAll(
                            () -> assertEquals(StartDialogue.EXIT_MISSING_INPUT, outcome.code(), outcome.rendered()),
                            () -> assertTrue(outcome.rendered().contains("flagged"), outcome.rendered()),
                            () -> assertFalse(outcome.rendered().contains("has started"), outcome.rendered()));
    }

    @Test
    @DisplayName("plan add --local-changes where the landing commits captures the local changes and starts")
    void localChangesCaptured() throws Exception {
        var outcome = run(caseOf("start.local-changes-captured"), null,
                            "plan", "add", "--project", PROJECT, "--entry", ENTRY, "--local-changes", "--no-input");

        assertAll(
                            () -> assertEquals(0, outcome.code(), outcome.rendered()),
                            () -> assertTrue(outcome.rendered().contains("2 files changed"), outcome.rendered()),
                            () -> assertFalse(outcome.rendered().contains("landing.files"), outcome.rendered()),
                            () -> assertTrue(outcome.rendered().contains("has started"), outcome.rendered()));
    }

    @Test
    @DisplayName("git-source is no verb: it is reported with the usage exit code")
    void gitSourceIsUnknown() {
        var out = new StringWriter();
        var err = new StringWriter();
        var context = new OperatorContext(new HashMap<>(), Path.of("."));

        var code = PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err), "git-source",
                            "adopt-pr", "214");

        assertEquals(2, code, err.toString());
    }

    private static Outcome run(String scenario, @Nullable String input, String... args) throws Exception {
        try (var runtime = MockRuntime.start(MockRuntimeConfig.builder().scenario(scenario).build());
             var driver = TerminalTestDriver.create()) {
            recordLiveRuntime(runtime);
            var environment = new HashMap<>(runtime.environment());
            environment.put("LANG", "en_US.UTF-8");
            environment.put("USER", "operator");
            driver.project(environment, runtime.base().getParent());
            if (input == null) {
                driver.endOfInput();
            } else {
                driver.type(input);
            }
            var code = TranscriptVerbRunner.run(driver.context(), driver.writer(), driver.writer(), args);
            return new Outcome(code, driver.render());
        }
    }

    private static void recordLiveRuntime(MockRuntime runtime) throws IOException {
        var self = ProcessHandle.current();
        var start = self.info().startInstant().orElseThrow();
        Files.createDirectories(runtime.paths().stateDir());
        Files.writeString(runtime.paths().runtimeRecord(),
                            "{\"pid\":" + self.pid() + ",\"start_instant\":\"" + start + "\"}",
                            StandardCharsets.UTF_8);
    }

    private record Outcome(int code, String rendered) {
    }
}
