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
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import de.planmarshall.api.decomposition.EpicDecomposition;
import de.planmarshall.api.testsupport.FakeRuntimeServer;
import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("DecomposeCommand")
@EnableGeneratorController
class DecomposeCommandTest {

    /** The project the runtime's epic names in the fixtures below. */
    private static final String PROJECT = "pm-mcp-clients";
    private static final String EPIC = "/api/v1/epics/ep-001";
    private static final String BASE = "/api/v1/projects/" + PROJECT + "/epics/ep-001/decomposition";
    private static final String EPIC_RESOURCE = "{\"epic_id\":\"ep-001\",\"project_id\":\"" + PROJECT + "\"}";

    private static final String CUT_WITH_OVERLAP = """
                        {"epic_id":"relay-hardening","status":"check","revision":3,
                         "workstreams":[{"id":"WS-1","name":"Reconnect","order":1,
                           "staged_plans":[{"plan_id":"relay-cursor-order","title":"relay-cursor-order",
                             "expected_surface":["pm-relay/src/cursor","pm-api/src/cursor"],"dependencies":[],
                             "status":"staged"}]},
                         {"id":"WS-2","name":"Authentication","order":2,
                           "staged_plans":[{"plan_id":"relay-auth-v2","title":"relay-auth-v2",
                             "objective":"a token exchange","expected_surface":["pm-api/src/cursor"],
                             "dependencies":[],"status":"staged"}]}],
                         "findings":[{"severity":"error","rule":"surface_overlap",
                           "affected_plans":["relay-cursor-order","relay-auth-v2"],
                           "message":"surface overlaps relay-auth-v2 (pm-api/src/cursor)"}]}
                        """;

    private static final String CUT_PASSING = """
                        {"epic_id":"relay-hardening","status":"review","revision":3,
                         "workstreams":[{"id":"WS-1","name":"Reconnect","order":1,
                           "staged_plans":[{"plan_id":"relay-cursor-order","title":"relay-cursor-order",
                             "expected_surface":["pm-relay/src/cursor"],"dependencies":[],"status":"staged"}]}],
                         "findings":[]}
                        """;

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
    void tearDown() throws Exception {
        if (fixture != null) {
            fixture.close();
        }
    }

    private int execute(String... args) {
        var context = new OperatorContext(fixture.environment(), tempDir, true);
        return PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err), args);
    }

    private int executeInGerman(String... args) {
        var environment = new HashMap<>(fixture.environment());
        environment.put("LANG", "de_DE.UTF-8");
        var context = new OperatorContext(environment, tempDir, true);
        return PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err), args);
    }

    /**
     * @return whether the request read the epic, answered with the fixture's epic
     */
    private static boolean answersEpic(FakeRuntimeServer.Request request, FakeRuntimeServer.Response response)
                        throws IOException {
        if (EPIC.equals(request.target())) {
            response.json(200, EPIC_RESOURCE);
            return true;
        }
        return false;
    }

    @Test
    @DisplayName("epic decompose prints the workstreams, plans, and the failing check")
    void decomposePrintsTree() throws Exception {
        fixture.start((request, response) -> {
            if (answersEpic(request, response)) {
                return;
            }
            if (BASE.equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, CUT_WITH_OVERLAP);
            } else {
                response.send(404, null, "");
            }
        });

        var code = execute("epic", "decompose", "ep-001");
        var output = out.toString();
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(output.contains("relay-hardening · decomposition"), output),
                            () -> assertTrue(output.contains("WS-1")),
                            () -> assertTrue(output.contains("relay-auth-v2")),
                            () -> assertTrue(output.contains("1 check fails"), output)
        );
    }

    @Test
    @DisplayName("the decomposition is read in the project the runtime's epic names")
    void decomposeReadsTheEpicsOwnProject() throws Exception {
        var project = Generators.letterStrings(5, 12).next().toLowerCase();
        var targets = new CopyOnWriteArrayList<String>();
        fixture.start((request, response) -> {
            targets.add(request.target());
            if (EPIC.equals(request.target())) {
                response.json(200, "{\"epic_id\":\"ep-001\",\"project_id\":\"" + project + "\"}");
            } else {
                response.json(200, CUT_PASSING);
            }
        });

        var code = execute("epic", "decompose", "ep-001");

        assertAll(
                            () -> assertEquals(0, code, err.toString()),
                            () -> assertTrue(targets.contains("/api/v1/projects/" + project + "/epics/ep-001/decomposition"),
                                                targets.toString())
        );
    }

    @Test
    @DisplayName("--project names the project, and the epic is not looked up")
    void projectOptionNamesTheProject() throws Exception {
        var project = Generators.letterStrings(5, 12).next().toLowerCase();
        var targets = new CopyOnWriteArrayList<String>();
        fixture.start((request, response) -> {
            targets.add(request.target());
            response.json(200, CUT_PASSING);
        });

        var code = execute("epic", "decompose", "ep-001", "--project", project);

        assertAll(
                            () -> assertEquals(0, code, err.toString()),
                            () -> assertFalse(targets.contains(EPIC), targets.toString()),
                            () -> assertTrue(targets.contains("/api/v1/projects/" + project + "/epics/ep-001/decomposition"),
                                                targets.toString())
        );
    }

    @ParameterizedTest(name = "epic {0} names an unknown epic")
    @ValueSource(strings = {"decompose", "review", "instruct"})
    @DisplayName("an epic the runtime does not know is not found, and no decomposition is touched")
    void unknownEpic(String verb) throws Exception {
        var targets = new CopyOnWriteArrayList<String>();
        fixture.start((request, response) -> {
            targets.add(request.target());
            response.json(404, "{\"error\":\"scope_not_found\"}");
        });

        var code = "instruct".equals(verb)
                            ? execute("epic", verb, "ep-001", "--instruction", "words", "--action", "rewrite")
                            : execute("epic", verb, "ep-001");

        assertAll(
                            () -> assertEquals(1, code),
                            () -> assertTrue(out.toString().contains("Epic ep-001 not found."), out.toString()),
                            () -> assertTrue(targets.stream().noneMatch(target -> target.contains("/decomposition")),
                                                targets.toString())
        );
    }

    @ParameterizedTest(name = "an epic answered {0} is refused")
    @ValueSource(strings = {"500", "200"})
    @DisplayName("an epic the runtime refuses, or one that names no project, is refused")
    void epicWithoutProject(int status) throws Exception {
        fixture.start((request, response) -> response.json(status, "{\"epic_id\":\"ep-001\"}"));

        var code = execute("epic", "decompose", "ep-001");

        assertAll(
                            () -> assertEquals(1, code),
                            () -> assertTrue(err.toString().contains("epic ep-001"), err.toString())
        );
    }

    @Test
    @DisplayName("the decomposition speaks the interface language")
    void decomposeInGerman() throws Exception {
        fixture.start((request, response) -> {
            if (!answersEpic(request, response)) {
                response.json(200, CUT_PASSING);
            }
        });

        var code = executeInGerman("epic", "decompose", "ep-001");

        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(out.toString().contains("relay-hardening · Zerlegung"), out.toString()),
                            () -> assertTrue(out.toString().contains("jede Prüfung besteht"), out.toString())
        );
    }

    @Test
    @DisplayName("epic decompose drafts again with an instruction")
    void decomposeWithInstruction() throws Exception {
        var bodies = new CopyOnWriteArrayList<String>();
        fixture.start((request, response) -> {
            if (answersEpic(request, response)) {
                return;
            }
            if (BASE.equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, CUT_WITH_OVERLAP);
            } else if ((BASE + "/instruct").equals(request.target()) && "POST".equals(request.method())) {
                bodies.add(request.bodyText());
                response.json(200, CUT_WITH_OVERLAP);
            } else {
                response.send(404, null, "");
            }
        });

        var code = execute("epic", "decompose", "ep-001", "--instruction", "split the token work off");
        assertEquals(0, code);
        assertEquals(1, bodies.size(), bodies.toString());
        assertTrue(bodies.getFirst().contains("split the token work off"));
        assertTrue(bodies.getFirst().contains("\"rewrite\""));
    }

    @Test
    @DisplayName("the tree names a plan's dependencies, a locked plan, and a changed one")
    void treeNamesDependenciesLockAndChange() throws Exception {
        var cut = """
                            {"epic_id":"relay-hardening","status":"review","revision":3,
                             "workstreams":[{"id":"WS-1","name":"Reconnect","order":1,
                               "staged_plans":[
                                 {"plan_id":"a-plan","title":"a","expected_surface":[],"dependencies":["b-plan"],"status":"locked"},
                                 {"plan_id":"c-plan","title":"c","expected_surface":["x"],"dependencies":[],"status":"changed"}]}],
                             "findings":[{"severity":"warning","rule":"r","affected_plans":[],"message":"a warning"}]}
                            """;
        fixture.start((request, response) -> {
            if (!answersEpic(request, response)) {
                response.json(200, cut);
            }
        });

        var code = execute("epic", "decompose", "ep-001");

        var output = out.toString();
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(output.contains("after b-plan"), output),
                            () -> assertTrue(output.contains("locked"), output),
                            () -> assertTrue(output.contains("changed"), output),
                            () -> assertTrue(output.contains("! a warning"), output),
                            () -> assertTrue(output.contains("—"), output)
        );
    }

    @Test
    @DisplayName("epic review --agree commits a cut whose checks pass")
    void reviewAgreesAndCommits() throws Exception {
        fixture.start((request, response) -> {
            if (answersEpic(request, response)) {
                return;
            }
            if ((BASE + "/check").equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, CUT_PASSING);
            } else if ((BASE + "/commit").equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, "{\"status\":\"committed\",\"ledger_revision\":4,\"launchable_plan_count\":1}");
            } else {
                response.send(404, null, "");
            }
        });

        var code = execute("epic", "review", "ep-001", "--agree");
        var output = out.toString();
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(output.contains("every check passes"), output),
                            () -> assertTrue(output.contains("relay-hardening: 1 workstream, 1 plan committed"), output)
        );
    }

    @Test
    @DisplayName("epic review without --agree checks and commits nothing")
    void reviewWithoutAgree() throws Exception {
        var commits = new CopyOnWriteArrayList<String>();
        fixture.start((request, response) -> {
            if (answersEpic(request, response)) {
                return;
            }
            if (request.target().endsWith("/commit")) {
                commits.add(request.target());
            }
            response.json(200, CUT_PASSING);
        });

        var code = execute("epic", "review", "ep-001");

        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertEquals(List.of(), commits)
        );
    }

    @Test
    @DisplayName("a commit the runtime refuses names its reason, not a failing check")
    void reviewCommitRefusedNamesTheRuntimesReason() throws Exception {
        var reason = Generators.letterStrings(8, 14).next();
        fixture.start((request, response) -> {
            if (answersEpic(request, response)) {
                return;
            }
            if (request.target().endsWith("/commit")) {
                response.json(409, "{\"error\":\"" + reason + "\"}");
            } else {
                response.json(200, CUT_PASSING);
            }
        });

        var code = execute("epic", "review", "ep-001", "--agree");

        assertAll(
                            () -> assertEquals(1, code),
                            () -> assertTrue(err.toString().contains(reason), err.toString()),
                            () -> assertFalse(out.toString().contains("cannot be agreed"), out.toString()),
                            () -> assertFalse(out.toString().contains("committed"), out.toString())
        );
    }

    @Test
    @DisplayName("epic review --agree refuses a cut whose checks fail")
    void reviewRefusesFailingCut() throws Exception {
        fixture.start((request, response) -> {
            if (answersEpic(request, response)) {
                return;
            }
            if ((BASE + "/check").equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, CUT_WITH_OVERLAP);
            } else {
                response.send(404, null, "");
            }
        });

        var code = execute("epic", "review", "ep-001", "--agree");
        assertEquals(1, code);
        assertTrue(out.toString().contains("1 check fails"));
    }

    @Test
    @DisplayName("epic instruct shows the read-back, old against new")
    void instructShowsReadBack() throws Exception {
        var rewrite = """
                            {"epic_id":"relay-hardening","status":"read_back","revision":3,
                             "workstreams":[{"id":"WS-1","name":"Reconnect","order":1,
                               "staged_plans":[{"plan_id":"relay-cursor-order","title":"relay-cursor-order",
                                 "expected_surface":["pm-relay/src/cursor","pm-api/src/cursor"],"dependencies":[],
                                 "status":"staged"}]},
                             {"id":"WS-2","name":"Authentication","order":2,
                               "staged_plans":[{"plan_id":"relay-auth-v2","title":"relay-auth-v2",
                                 "objective":"the exchange belongs into pm-relay",
                                 "expected_surface":["pm-relay/src/relay-auth-v2"],"dependencies":[],
                                 "status":"changed"}]}],
                             "findings":[]}
                            """;
        var bodies = new CopyOnWriteArrayList<String>();
        fixture.start((request, response) -> {
            if (answersEpic(request, response)) {
                return;
            }
            if (BASE.equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, CUT_WITH_OVERLAP);
            } else if ((BASE + "/instruct").equals(request.target()) && "POST".equals(request.method())) {
                bodies.add(request.bodyText());
                response.json(200, rewrite);
            } else {
                response.send(404, null, "");
            }
        });

        var code = execute("epic", "instruct", "ep-001", "--plan", "relay-auth-v2", "--instruction",
                            "the exchange belongs into pm-relay");
        var output = out.toString();
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(output.contains("old against new"), output),
                            () -> assertTrue(output.contains("- Surface"), output),
                            () -> assertTrue(output.contains("+ Surface"), output),
                            () -> assertTrue(bodies.getFirst().contains("relay-auth-v2"))
        );
    }

    @Test
    @DisplayName("an added plan reads back as itself, none against new, not as the first plan of the cut")
    void addReadsBackTheAddedPlan() throws Exception {
        var added = Generators.letterStrings(6, 12).next().toLowerCase();
        var objective = Generators.letterStrings(10, 20).next();
        var after = CUT_PASSING.replace("\"status\":\"staged\"}]}]",
                            "\"status\":\"staged\"},{\"plan_id\":\"" + added + "\",\"title\":\"t\",\"objective\":\""
                                                + objective + "\",\"expected_surface\":[],\"dependencies\":[],\"status\":\"new\"}]}]");
        fixture.start((request, response) -> {
            if (answersEpic(request, response)) {
                return;
            }
            response.json(200, request.target().endsWith("/instruct") ? after : CUT_PASSING);
        });

        var code = execute("epic", "instruct", "ep-001", "--action", "add", "--instruction", "add a plan");

        var output = out.toString();
        assertAll(
                            () -> assertEquals(0, code, err.toString()),
                            () -> assertTrue(output.contains(added + ": old against new"), output),
                            () -> assertTrue(output.contains("- Objective      —"), output),
                            () -> assertTrue(output.contains("+ Objective      " + objective), output),
                            () -> assertFalse(output.contains("relay-cursor-order: old against new"), output)
        );
    }

    @Test
    @DisplayName("a rewrite that adds no plan shows the cut again instead of claiming every check passes")
    void rewriteShowsTheCut() throws Exception {
        fixture.start((request, response) -> {
            if (!answersEpic(request, response)) {
                response.json(200, request.target().endsWith("/instruct") ? CUT_PASSING : CUT_WITH_OVERLAP);
            }
        });

        var code = execute("epic", "instruct", "ep-001", "--action", "rewrite", "--instruction", "split it");

        var output = out.toString();
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(output.contains("relay-hardening · decomposition"), output),
                            () -> assertTrue(output.contains("WS-1  Reconnect"), output),
                            () -> assertFalse(output.contains("old against new"), output)
        );
    }

    @Test
    @DisplayName("a rewrite that adds several plans shows the whole cut, not only the first added plan")
    void rewriteAddingSeveralPlansShowsTheCut() throws Exception {
        var first = Generators.letterStrings(6, 12).next().toLowerCase();
        var second = first + "x";
        var extra = "{\"plan_id\":\"%s\",\"title\":\"t\",\"objective\":\"o\",\"expected_surface\":[],"
                            + "\"dependencies\":[],\"status\":\"new\"}";
        var after = CUT_PASSING.replace("\"status\":\"staged\"}]}]",
                            "\"status\":\"staged\"}," + extra.formatted(first) + "," + extra.formatted(second) + "]}]");
        fixture.start((request, response) -> {
            if (!answersEpic(request, response)) {
                response.json(200, request.target().endsWith("/instruct") ? after : CUT_PASSING);
            }
        });

        var code = execute("epic", "instruct", "ep-001", "--action", "rewrite", "--instruction", "split it");

        var output = out.toString();
        assertAll(
                            () -> assertEquals(0, code, err.toString()),
                            () -> assertTrue(output.contains(first), output),
                            () -> assertTrue(output.contains(second), output),
                            () -> assertFalse(output.contains("old against new"), output)
        );
    }

    @Test
    @DisplayName("words for a plan the cut no longer holds say so")
    void instructForAPlanNotInTheCut() throws Exception {
        var plan = Generators.letterStrings(6, 12).next().toLowerCase();
        fixture.start((request, response) -> {
            if (!answersEpic(request, response)) {
                response.json(200, CUT_PASSING);
            }
        });

        var code = execute("epic", "instruct", "ep-001", "--plan", plan, "--instruction", "drop it");

        assertTrue(out.toString().contains("the plan " + plan + " is not part of the cut"), out.toString());
        assertEquals(0, code);
    }

    @Test
    @DisplayName("words the runtime refuses exit 1 with its refusal")
    void instructRefused() throws Exception {
        fixture.start((request, response) -> {
            if (answersEpic(request, response)) {
                return;
            }
            if (request.target().endsWith("/instruct")) {
                response.json(404, "{\"error\":\"scope_not_found\"}");
            } else {
                response.json(200, CUT_PASSING);
            }
        });

        var code = execute("epic", "instruct", "ep-001", "--plan", "relay-cursor-order", "--instruction", "x");

        assertAll(
                            () -> assertEquals(1, code),
                            () -> assertTrue(err.toString().contains("epic instruct"), err.toString())
        );
    }

    @Test
    @DisplayName("epic instruct requires words, and a plan to edit")
    void instructRequiresWordsAndAPlan() {
        var noWords = execute("epic", "instruct", "ep-001", "--plan", "relay-auth-v2");
        var noPlan = execute("epic", "instruct", "ep-001", "--instruction", "words");

        assertAll(
                            () -> assertEquals(1, noWords),
                            () -> assertTrue(out.toString().contains("What should change?"), out.toString()),
                            () -> assertEquals(1, noPlan),
                            () -> assertTrue(out.toString().contains("--plan is required"), out.toString())
        );
    }

    @Test
    @DisplayName("a decomposition the runtime cannot read is its refusal")
    void decompositionRefused() throws Exception {
        fixture.start((request, response) -> {
            if (!answersEpic(request, response)) {
                response.json(500, "{\"error\":\"internal\"}");
            }
        });

        var decompose = execute("epic", "decompose", "ep-001");
        var review = execute("epic", "review", "ep-001");

        assertAll(
                            () -> assertEquals(1, decompose),
                            () -> assertEquals(1, review),
                            () -> assertTrue(err.toString().contains("epic decompose"), err.toString()),
                            () -> assertTrue(err.toString().contains("epic review"), err.toString())
        );
    }

    @Test
    @DisplayName("the read-back of the dialogue is in its language")
    void readBackInGerman() throws Exception {
        var writer = new StringWriter();
        var before = EpicDecomposition.fromJson(CUT_WITH_OVERLAP);
        var after = EpicDecomposition.fromJson(CUT_WITH_OVERLAP.replace("a token exchange", "ein Austausch"));

        DecompositionSupport.printReadBack(new PrintWriter(writer), "de", before, after, "relay-auth-v2");

        assertTrue(writer.toString().contains("+ Ziel"), writer.toString());
    }
}
