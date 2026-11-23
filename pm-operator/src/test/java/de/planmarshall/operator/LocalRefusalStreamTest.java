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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import org.jline.terminal.impl.DumbTerminal;

import de.planmarshall.api.control.PlanSettingsRecord;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.system.CleanupItemEntry;
import de.planmarshall.api.system.CleanupJob;
import de.planmarshall.api.system.CleanupJobState;
import de.planmarshall.api.system.CleanupProjectItem;
import de.planmarshall.api.system.CleanupSummary;
import de.planmarshall.api.testsupport.FakeRuntimeServer;
import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The local refusals of verbs run as one shell command go to stderr and stdout carries none of them
 * ({@code 03-dialogues.adoc#refusals}): a refusal this client finds itself, before or without a
 * request, exits {@code 1} with its line on stderr.
 */
@DisplayName("Local refusals of one-shot verbs")
@EnableGeneratorController
class LocalRefusalStreamTest {

    private static final String ONE_LEVEL = "Name one level: --project or --plan, not both.";
    private static final String PLAN_BLANK = "--plan needs a plan id.";
    private static final String PLAN_EXCLUSIVE = "--plan cannot be combined with the other options of this verb.";

    @TempDir
    Path tempDir;

    private RuntimeFixture fixture;
    private StringWriter out;
    private StringWriter err;

    @BeforeEach
    void setUp() throws IOException {
        fixture = RuntimeFixture.create();
        PmOperatorTest.writeLiveRecord(fixture);
        out = new StringWriter();
        err = new StringWriter();
    }

    @AfterEach
    void tearDown() throws IOException {
        fixture.close();
    }

    private int execute(String... args) {
        return execute(fixture.environment(), args);
    }

    private int execute(Map<String, String> environment, String... args) {
        var context = new OperatorContext(environment, tempDir, true,
                            ClientEnvironment.capture(environment),
                            () -> new DumbTerminal(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream()),
                            null);
        return PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err), args);
    }

    private void assertRefusedOnStderr(int code, String line) {
        assertAll(
                            () -> assertEquals(1, code, out.toString() + err),
                            () -> assertTrue(err.toString().contains(line), err.toString()),
                            () -> assertFalse(out.toString().contains(line), out.toString())
        );
    }

    private static String word() {
        return Generators.letterStrings(6, 12).next().toLowerCase(Locale.ROOT);
    }

    @Test
    @DisplayName("config show of a key that is not set refuses on stderr")
    void showOfAnUnknownKey() throws Exception {
        var key = "zz" + word();
        fixture.start((request, response) -> response.json(200,
                            "[{\"key\":\"web.port\",\"value\":7420,\"origin\":\"default\",\"revision\":1}]"));

        int code = execute("config", "show", key);

        assertRefusedOnStderr(code, "Configuration key " + key + " is not set.");
    }

    static Stream<Arguments> twoLevels() {
        var key = "web." + word();
        var value = word();
        var project = word();
        var plan = word();
        var cases = new ArrayList<Arguments>();
        for (var verb : List.of("set", "unset")) {
            var head = "set".equals(verb) ? List.of("config", verb, key, value) : List.of("config", verb, key);
            cases.add(Arguments.of(verb + " --project --plan", concat(head, "--project", project, "--plan", plan)));
            cases.add(Arguments.of(verb + " --machine --project", concat(head, "--machine", "--project", project)));
            cases.add(Arguments.of(verb + " --machine --plan", concat(head, "--machine", "--plan", plan)));
        }
        return cases.stream();
    }

    private static String[] concat(List<String> head, String... tail) {
        var all = new ArrayList<>(head);
        all.addAll(List.of(tail));
        return all.toArray(new String[0]);
    }

    @ParameterizedTest(name = "config {0}")
    @MethodSource("twoLevels")
    @DisplayName("a write that names more than one level refuses on stderr before any request")
    void moreThanOneLevel(String name, String[] args) throws Exception {
        var server = fixture.start((request, response) -> response.send(404, null, ""));

        int code = execute(args);

        assertRefusedOnStderr(code, ONE_LEVEL);
        assertTrue(server.requests().isEmpty(), name + " sent " + server.requests());
    }

    static Stream<Arguments> planRefusals() {
        var plan = word();
        return Stream.of(
                            Arguments.of("roles --plan ''", new String[]{"config", "roles", "--plan", " "}, PLAN_BLANK),
                            Arguments.of("providers --plan ''", new String[]{"config", "providers", "--plan", " "},
                                                PLAN_BLANK),
                            Arguments.of("plan-defaults --plan ''",
                                                new String[]{"config", "plan-defaults", "--plan", " "}, PLAN_BLANK),
                            Arguments.of("roles --plan --reset-all",
                                                new String[]{"config", "roles", "--plan", plan, "--reset-all"}, PLAN_EXCLUSIVE),
                            Arguments.of("providers --plan --verify",
                                                new String[]{"config", "providers", "--plan", plan, "--verify"}, PLAN_EXCLUSIVE),
                            Arguments.of("plan-defaults --plan --yes",
                                                new String[]{"config", "plan-defaults", "--plan", plan, "--yes"}, PLAN_EXCLUSIVE));
    }

    @ParameterizedTest(name = "config {0}")
    @MethodSource("planRefusals")
    @DisplayName("a blank --plan or --plan beside the verb's other options refuses on stderr before any request")
    void planOptionRefusals(String name, String[] args, String line) throws Exception {
        var server = fixture.start((request, response) -> response.send(404, null, ""));

        int code = execute(args);

        assertRefusedOnStderr(code, line);
        assertTrue(server.requests().isEmpty(), name + " sent " + server.requests());
    }

    @Test
    @DisplayName("runtime stop of a live runtime that does not answer refuses on stderr")
    void stopOfARuntimeThatDoesNotAnswer() {
        int code = execute("runtime", "stop", "--now");

        assertRefusedOnStderr(code, "The runtime is not answering.");
    }

    @Test
    @DisplayName("cleanup --delete --exclude of an item the open job does not carry refuses on stderr")
    void cleanupExcludeOutsideTheJob() throws Exception {
        var item = word();
        var items = List.of(new CleanupItemEntry(item, "plan", item, "merged", "12 Sep", 1024L, 1, 0, null));
        var project = new CleanupProjectItem(word(), 1, 0, 1, 0, 1024L, items);
        var summary = new CleanupSummary(1, 0, 1, 0, 0, 1024L, 0, "today 03:00", "tomorrow 03:00");
        var job = new CleanupJob("cln-" + word(), CleanupJobState.OPEN, "today 03:00", summary, List.of(project),
                            Map.of());
        var server = fixture.start((request, response) -> response.json(200, JsonTree.write(job.toMap())));
        var unknown = "plan:zz" + word();

        int code = execute("cleanup", "--delete", "--exclude", unknown, "--yes");

        assertRefusedOnStderr(code, unknown + " is no item of the open cleanup.");
        var deletes = server.requests().stream().map(FakeRuntimeServer.Request::target)
                            .filter(target -> target.endsWith("/delete")).toList();
        assertTrue(deletes.isEmpty(), "no delete may be sent: " + deletes);
    }

    @Test
    @DisplayName("plan settings --set overtaken by another client refuses on stderr without --no-input")
    void planSettingsConflict() throws Exception {
        var plan = word();
        var current = "method." + word();
        var settingsPath = "/api/v1/plans/" + plan + "/settings";
        fixture.start((request, response) -> {
            if (settingsPath.equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, new PlanSettingsRecord(plan, 3, List.of()).toJson());
            } else if (settingsPath.equals(request.target())) {
                response.json(409, "{\"applied\":false,\"conflict\":true,\"revision\":4,\"current_value\":\""
                                    + current + "\"}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("plan", "settings", plan, "--set", "method=method." + word());

        assertRefusedOnStderr(code, "Changed meanwhile: it is now " + current + ". Yours was not applied.");
    }

    @Test
    @DisplayName("--project ? with no enrolled project refuses on stderr")
    void projectSelectorWithoutProjects() throws Exception {
        fixture.start((request, response) -> response.json(200, "{\"projects\":[]}"));

        int code = execute("questions", "--project", "?");

        assertRefusedOnStderr(code, "No project is enrolled yet.");
    }

    @Test
    @DisplayName("plan add that resolves no project and finds none enrolled refuses on stderr")
    void startWithoutProjects() throws Exception {
        fixture.start((request, response) -> response.json(200, "{\"projects\":[]}"));
        var environment = new HashMap<>(fixture.environment());
        environment.remove("PM_PROJECT");

        int code = execute(environment, "plan", "add", "--description", word());

        assertRefusedOnStderr(code, "No project is enrolled yet.");
    }
}
