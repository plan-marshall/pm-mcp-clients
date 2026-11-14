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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import de.planmarshall.api.testsupport.FakeRuntimeServer;
import de.planmarshall.api.testsupport.RuntimeFixture;
import de.planmarshall.mock.StandardScenarios;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import picocli.CommandLine;
import picocli.CommandLine.Model.OptionSpec;

@DisplayName("EpicCommand")
@EnableGeneratorController
class EpicCommandTest {

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
        return execute(new PrintWriter(out), args);
    }

    private int execute(PrintWriter console, String... args) {
        var context = new OperatorContext(fixture.environment(), tempDir, true);
        return PmOperator.execute(context, console, new PrintWriter(err), args);
    }

    private void serveEpicProcess(String epicId, String body) throws IOException {
        var processPath = "/api/v1/epics/" + epicId + "/process";
        fixture.start((request, response) -> {
            if (processPath.equals(request.target())) {
                response.json(200, body);
            } else {
                response.send(404, null, "");
            }
        });
    }

    @Test
    @DisplayName("epic <id> prints a malformed process body as it came")
    void malformedProcessBodyFallsBack() throws Exception {
        var epicId = Generators.letterStrings(6, 12).next();
        var body = "{\"epic_id\":\"" + epicId + "\",\"running_plans\":[";
        serveEpicProcess(epicId, body);

        int code = execute("epic", epicId);
        assertAll(
                            () -> assertEquals(0, code, err.toString()),
                            () -> assertTrue(out.toString().contains(body), out.toString())
        );
    }

    @Test
    @DisplayName("epic <id> names a plan's done and planned phase states, never unknown")
    void epicProcessNamesDoneAndPlanned() throws Exception {
        var epicId = Generators.letterStrings(6, 12).next();
        var body = "{\"epic_id\":\"" + epicId + "\",\"project\":\"pm-mcp-clients\",\"state\":\"running\","
                            + "\"running_plans\":[{\"plan_id\":\"fix-relay\",\"title\":\"Fix the relay\","
                            + "\"current_phase\":\"execute\",\"phase_state\":\"done\"}],"
                            + "\"staged_plans\":[{\"plan_id\":\"relay-docs\",\"title\":\"Docs\","
                            + "\"current_phase\":\"plan\",\"phase_state\":\"planned\"}]}";
        serveEpicProcess(epicId, body);

        int code = execute("epic", epicId);
        var output = out.toString();
        assertAll(
                            () -> assertEquals(0, code, err.toString()),
                            () -> assertTrue(output.contains("Fix the relay"), output),
                            () -> assertTrue(output.contains("execute \u00b7 done"), output),
                            () -> assertTrue(output.contains("plan \u00b7 planned"), output),
                            () -> assertFalse(output.contains("\u00b7 unknown"), output)
        );
    }

    @Test
    @DisplayName("epic <id> does not hide a failure of the process view behind the raw body")
    void processViewFailureIsNotSwallowed() throws Exception {
        var epicId = Generators.letterStrings(6, 12).next();
        var failure = Generators.letterStrings(8, 16).next();
        var body = "{\"epic_id\":\"" + epicId + "\",\"running_plans\":[]}";
        serveEpicProcess(epicId, body);
        var console = new FailingOnceWriter(out, epicId, failure);

        int code = execute(console, "epic", epicId);
        var accepted = console.accepted();
        assertAll(
                            () -> assertEquals(PmOperator.EXIT_FAILURE, code, accepted),
                            () -> assertTrue(err.toString().contains(failure), err.toString()),
                            () -> assertFalse(accepted.contains(body), accepted)
        );
    }

    private static final String EPIC_RESOURCE = """
                        {"epic_id":"relay-hardening","project_id":"pm-mcp-clients",
                         "display_name":"pm-mcp-clients","state":"running","auto_start":true,
                         "overlap_max_files":0,"scope":3,"running":3,
                         "workstreams":[
                           {"id":"WS-1","name":"Reconnect","open":true,"rows":[
                             {"plan_id":"relay-reconnect-backoff","title":"relay-reconnect-backoff",
                              "status":"shipped","pull_request_number":211},
                             {"plan_id":"relay-cursor-order","title":"relay-cursor-order","status":"running"}]},
                           {"id":"WS-2","name":"Authentication","open":true,"rows":[
                             {"plan_id":"relay-docs","title":"relay-docs","status":"staged",
                              "wait_reason":"overlap","wait_plan_id":"relay-auth-v2"}]}],
                         "sources":["2 documents","1 issue"],"landings":1,
                         "findings":{"new":3,"open":4,"kept":2},"watches":{"new":1,"open":4,"kept":0},
                         "tokens":{"total":{"total":3860000,"input":3520000,"output":340000,"measured":true,
                                            "floor":true,"measured_tasks":4,"not_measured_tasks":1},
                                   "own":{"total":120000,"input":110000,"output":10000,"measured":true,
                                          "floor":false,"measured_tasks":1,"not_measured_tasks":0},
                                   "plans":[{"plan_id":"relay-reconnect-backoff",
                                             "figure":{"total":1400000,"input":1300000,"output":100000,
                                                       "measured":true,"floor":false,"measured_tasks":2,
                                                       "not_measured_tasks":0}}]},
                         "events":[{"at":"2026-10-16T12:40:00Z","text":"relay-auth-v2 asks a decision"}]}
                        """;

    @Test
    @DisplayName("epic <id> prints the epic as a tree from its resource")
    void epicResourceTree() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/epics/relay-hardening".equals(request.target())) {
                response.json(200, EPIC_RESOURCE);
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("epic", "relay-hardening");
        var output = out.toString();
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(output.contains("relay-hardening \u00b7 epic"), output),
                            () -> assertTrue(output.contains("automatic"), output),
                            () -> assertTrue(output.contains("3 of 3 at once"), output),
                            () -> assertTrue(output.contains("WS-1 Reconnect")
                                                || output.contains("WS-1  Reconnect"), output),
                            () -> assertTrue(output.contains("relay-reconnect-backoff"), output),
                            () -> assertTrue(output.contains("#211"), output),
                            () -> assertTrue(output.contains("waits: overlap with relay-auth-v2"), output),
                            () -> assertTrue(output.contains("2 documents, 1 issue"), output),
                            () -> assertTrue(output.contains("Findings"), output),
                            () -> assertTrue(output.contains("3.86 M"), output),
                            () -> assertTrue(output.contains("Events"), output),
                            () -> assertTrue(output.contains("relay-auth-v2 asks a decision"), output)
        );
    }

    @Test
    @DisplayName("epic <id> shows an unknown running count as ? of 3 at once")
    void epicResourceUnknownRunning() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/epics/relay-hardening".equals(request.target())) {
                response.json(200, EPIC_RESOURCE.replace("\"scope\":3,\"running\":3", "\"scope\":3"));
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("epic", "relay-hardening");

        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(out.toString().contains("? of 3 at once"), out.toString())
        );
    }

    @Test
    @DisplayName("epic <id> shows a known running count as 2 of 3 at once")
    void epicResourceKnownRunning() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/epics/relay-hardening".equals(request.target())) {
                response.json(200, EPIC_RESOURCE.replace("\"scope\":3,\"running\":3", "\"scope\":3,\"running\":2"));
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("epic", "relay-hardening");

        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(out.toString().contains("2 of 3 at once"), out.toString())
        );
    }

    @Test
    @DisplayName("epic <id> --tokens prints the figure per plan and the epic's own tasks")
    void epicResourceTokens() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/epics/relay-hardening".equals(request.target())) {
                response.json(200, EPIC_RESOURCE);
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("epic", "relay-hardening", "--tokens");
        var output = out.toString();
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(output.contains("plan relay-reconnect-backoff"), output),
                            () -> assertTrue(output.contains("1.40 M"), output),
                            () -> assertTrue(output.contains("the epic's own tasks"), output)
        );
    }

    @Test
    @DisplayName("epic list lists running epics")
    void epicList() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/epics".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, """
                                    [
                                      {"id":"ep-001","state":"running","title":"Hardening Relay"},
                                      {"id":"ep-002","state":"staged","title":"Storage Migration"}
                                    ]
                                    """);
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("epic");
        var output = out.toString();
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(output.contains("Epics")),
                            () -> assertTrue(output.contains("ep-001")),
                            () -> assertTrue(output.contains("Hardening Relay")),
                            () -> assertTrue(output.contains("ep-002"))
        );
    }

    @Test
    @DisplayName("epic add creates an epic from its sources")
    void epicAdd() throws Exception {
        var outcome = StartTestSupport.run(StandardScenarios.START_EPIC, null,
                            "epic", "add", "--project", "pm-mcp-clients", "--title", "Refactor Auth",
                            "--id", "ep-auth", "--description", "Desc", "--no-input");
        assertAll(
                            () -> assertEquals(0, outcome.code(), outcome.rendered()),
                            () -> assertTrue(outcome.rendered().contains("ep-auth has started"),
                                                outcome.rendered()));
    }

    @Test
    @DisplayName("epic add with project adopts an epic from the ledger")
    void epicAddWithProject() throws Exception {
        var outcome = StartTestSupport.run(StandardScenarios.START_EPIC_ADOPT, null,
                            "epic", "add", "--project", "pm-mcp-clients", "--adopt", "relay-hardening",
                            "--no-input");
        assertAll(
                            () -> assertEquals(0, outcome.code(), outcome.rendered()),
                            () -> assertTrue(outcome.rendered().contains("relay-hardening adopted from the ledger"),
                                                outcome.rendered()));
    }

    @Test
    @DisplayName("epic <id> names an unknown count in the wait of a row that waits for the scope")
    void epicResourceScopeWaitUnknown() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/epics/relay-hardening".equals(request.target())) {
                response.json(200, EPIC_RESOURCE.replace("\"scope\":3,\"running\":3", "\"scope\":3")
                                    .replace("\"wait_reason\":\"overlap\",\"wait_plan_id\":\"relay-auth-v2\"",
                                                        "\"wait_reason\":\"scope\""));
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("epic", "relay-hardening");

        assertAll(
                            () -> assertEquals(0, code, err.toString()),
                            () -> assertTrue(out.toString().contains("? of 3 taken"), out.toString())
        );
    }

    @Test
    @DisplayName("epic next reads the epic, then triggers its next plan and names the plan it started")
    void epicNext() throws Exception {
        var epicId = Generators.letterStrings(6, 12).next();
        var planId = Generators.letterStrings(6, 12).next();
        var epicPath = "/api/v1/epics/" + epicId;
        var server = fixture.start((request, response) -> {
            if (epicPath.equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, """
                                    {"epic_id":"%s","project_id":"pm-mcp-clients","display_name":"pm-mcp-clients",
                                     "state":"running","auto_start":false,"overlap_max_files":0,
                                     "scope":3,"running":3,
                                     "workstreams":[{"id":"WS-1","name":"W","open":true,"rows":[
                                       {"plan_id":"%s","title":"%s","status":"staged","wait_reason":"scope"}]}],
                                     "sources":[],"landings":0,"findings":{"new":0,"open":0,"kept":0},
                                     "watches":{"new":0,"open":0,"kept":0},"events":[]}
                                    """.formatted(epicId, planId, planId));
            } else if ((epicPath + "/actions/next").equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, "{\"subject_id\":\"" + epicId + "\",\"subject_kind\":\"epic\","
                                    + "\"state\":\"running\",\"actions\":[]}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("epic", "next", epicId, "--raise-scope");

        var targets = server.requests().stream().map(FakeRuntimeServer.Request::target).toList();
        assertAll(
                            () -> assertEquals(0, code, err.toString()),
                            () -> assertEquals(epicPath, targets.getFirst(), targets.toString()),
                            () -> assertTrue(targets.contains(epicPath + "/actions/next"),
                                                "the next plan was never triggered: " + targets),
                            () -> assertTrue(out.toString().contains(planId), out.toString())
        );
    }

    /**
     * {@code epic next} reads the epic and asks to raise a full scope by one; the flags answer the
     * question and {@code raise_scope} is sent only as the operator's answer (19-actions.adoc).
     */
    @Nested
    @DisplayName("epic next")
    class Next {

        private final String epicId = Generators.letterStrings(6, 12).next();
        private final String planId = Generators.letterStrings(6, 12).next();

        private String epic(String counts, String waitReason) {
            return """
                                {"epic_id":"%s","project_id":"pm-mcp-clients","display_name":"pm-mcp-clients",
                                 "state":"running","auto_start":false,"overlap_max_files":0,%s,
                                 "workstreams":[{"id":"WS-1","name":"W","open":true,"rows":[
                                   {"plan_id":"%s","title":"%s","status":"staged","wait_reason":"%s"}]}],
                                 "sources":[],"landings":0,"findings":{"new":0,"open":0,"kept":0},
                                 "watches":{"new":0,"open":0,"kept":0},"events":[]}
                                """.formatted(epicId, counts, planId, planId, waitReason);
        }

        private FakeRuntimeServer serve(String epicBody) throws IOException {
            var epicPath = "/api/v1/epics/" + epicId;
            var nextPath = epicPath + "/actions/next";
            return fixture.start((request, response) -> {
                if (epicPath.equals(request.target()) && "GET".equals(request.method())) {
                    response.json(200, epicBody);
                } else if (nextPath.equals(request.target()) && "POST".equals(request.method())) {
                    response.json(200, "{\"subject_id\":\"" + epicId + "\",\"subject_kind\":\"epic\","
                                        + "\"state\":\"running\",\"actions\":[]}");
                } else {
                    response.send(404, null, "");
                }
            });
        }

        private List<String> posted(FakeRuntimeServer server) {
            return server.requests().stream()
                                .filter(request -> "POST".equals(request.method()))
                                .map(FakeRuntimeServer.Request::bodyText)
                                .toList();
        }

        @Test
        @DisplayName("a plain run reads the epic and starts its next plan without raising the scope")
        void plainRun() throws Exception {
            var server = serve(epic("\"scope\":3,\"running\":2", "none"));

            int code = execute("epic", "next", epicId);
            var bodies = posted(server);
            assertAll(
                                () -> assertEquals(0, code, err.toString()),
                                () -> assertEquals("/api/v1/epics/" + epicId, server.requests().getFirst().target(),
                                                    server.requests().toString()),
                                () -> assertEquals(List.of("{\"raise_scope\":false}"), bodies),
                                () -> assertTrue(out.toString().contains(planId), out.toString()),
                                () -> assertTrue(out.toString().contains("3 of 3 at once"), out.toString())
            );
        }

        @Test
        @DisplayName("--raise-scope answers yes on a full scope and the started line names the raised scope")
        void raiseOnFullScope() throws Exception {
            var server = serve(epic("\"scope\":3,\"running\":3", "scope"));

            int code = execute("epic", "next", epicId, "--raise-scope");
            assertAll(
                                () -> assertEquals(0, code, err.toString()),
                                () -> assertEquals(List.of("{\"raise_scope\":true}"), posted(server)),
                                () -> assertTrue(out.toString().contains(planId), out.toString()),
                                () -> assertTrue(out.toString().contains("4 of 4 at once"), out.toString())
            );
        }

        @Test
        @DisplayName("--raise-scope on an unknown count names ? of the raised scope, never running plus one")
        void raiseOnUnknownCount() throws Exception {
            var server = serve(epic("\"scope\":3", "scope"));

            int code = execute("epic", "next", epicId, "--raise-scope");
            var output = out.toString();
            assertAll(
                                () -> assertEquals(0, code, err.toString()),
                                () -> assertEquals(List.of("{\"raise_scope\":true}"), posted(server)),
                                () -> assertTrue(output.contains("? of 4 at once"), output),
                                () -> assertFalse(output.contains("4 of 4 at once"), output)
            );
        }

        @Test
        @DisplayName("--no-raise answers no on a full scope and sends nothing")
        void noRaiseOnFullScope() throws Exception {
            var server = serve(epic("\"scope\":3,\"running\":3", "scope"));

            int code = execute("epic", "next", epicId, "--no-raise");
            assertAll(
                                () -> assertEquals(0, code, err.toString()),
                                () -> assertTrue(posted(server).isEmpty(), server.requests().toString())
            );
        }

        @Test
        @DisplayName("--no-input without an answer stops on a full scope with exit 3 and sends nothing")
        void noInputStops() throws Exception {
            var server = serve(epic("\"scope\":3,\"running\":3", "scope"));

            int code = execute("epic", "next", epicId, "--no-input");
            assertAll(
                                () -> assertEquals(3, code, out.toString()),
                                () -> assertTrue(posted(server).isEmpty(), server.requests().toString())
            );
        }

        @Test
        @DisplayName("--no-input without an answer stops on an unknown count with exit 3 and sends nothing")
        void noInputStopsOnUnknownCount() throws Exception {
            var server = serve(epic("\"scope\":3", "none"));

            int code = execute("epic", "next", epicId, "--no-input");
            assertAll(
                                () -> assertEquals(3, code, out.toString()),
                                () -> assertTrue(posted(server).isEmpty(), server.requests().toString())
            );
        }

        @Test
        @DisplayName("--raise-scope with --no-raise is a usage error and sends nothing")
        void bothFlagsRejected() throws Exception {
            var server = serve(epic("\"scope\":3,\"running\":3", "scope"));

            int code = execute("epic", "next", epicId, "--raise-scope", "--no-raise");
            assertAll(
                                () -> assertEquals(2, code, err.toString()),
                                () -> assertTrue(err.toString().contains(
                                                                        "--raise-scope and --no-raise are mutually exclusive."),
                                                    err.toString()),
                                () -> assertTrue(server.requests().isEmpty(), server.requests().toString())
            );
        }

        @Test
        @DisplayName("the options carry their descriptions from the catalogue")
        void optionDescriptionsFromCatalogue() {
            var context = new OperatorContext(fixture.environment(), tempDir, true);
            var spec = new CommandLine(new EpicCommand.NextCommand(context)).getCommandSpec();
            assertAll(
                                () -> assertEquals(
                                                    "When the scope is full, answer yes: raise it by one and start the next plan",
                                                    String.join(" ", spec.findOption("--raise-scope").description())),
                                () -> assertEquals("When the scope is full, answer no: leave it and start no plan",
                                                    String.join(" ", spec.findOption("--no-raise").description())),
                                () -> assertEquals(
                                                    "Ask nothing: without --raise-scope or --no-raise, a full scope stops here",
                                                    String.join(" ", spec.findOption("--no-input").description()))
            );
        }

        @ParameterizedTest(name = "locale {0}")
        @ValueSource(strings = {"en_US.UTF-8", "de_DE.UTF-8"})
        @DisplayName("every description key an option declares is in the catalogue bundle")
        void everyOptionKeyInBundle(String language) {
            var environment = new HashMap<>(fixture.environment());
            environment.put("LANG", language);
            var context = new OperatorContext(environment, tempDir, true);
            var spec = new CommandLine(new EpicCommand.NextCommand(context)).getCommandSpec();
            var declared = spec.options().stream()
                                .map(OptionSpec::descriptionKey)
                                .filter(key -> key != null && !key.isEmpty())
                                .toList();
            var bundle = spec.resourceBundle();

            assertFalse(declared.isEmpty(), "NextCommand declares no description key");
            assertNotNull(bundle, "no catalogue bundle is set");
            List<Executable> checks = declared.stream()
                                .<Executable>map(key -> () -> assertTrue(bundle.containsKey(key),
                                                    "option description key not in the catalogue bundle: " + key))
                                .toList();
            assertAll(checks);
            assertEquals(Set.copyOf(declared), Set.copyOf(EpicCommand.NextCommand.optionKeys()));
        }
    }

    @Test
    @DisplayName("epic auto updates auto mode")
    void epicAuto() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/epics/ep-001/auto".equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("epic", "auto", "ep-001", "on");
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(out.toString().contains("✓ Automatic for ep-001: on"))
        );
    }

    @Test
    @DisplayName("epic auto updates common files")
    void epicAutoCommonFiles() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/epics/ep-001/auto".equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("epic", "auto", "ep-001", "--common-files", "5");
        assertEquals(0, code);
        assertTrue(out.toString().contains("✓ Common files allowed for ep-001: 5"));
    }

    @Test
    @DisplayName("epic decompose prints the decomposition tree")
    void epicDecompose() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/epics/ep-001".equals(request.target())) {
                // the decomposition is read in the project the runtime's epic names
                response.json(200, "{\"epic_id\":\"ep-001\",\"project_id\":\"pm-mcp-clients\"}");
            } else if ("/api/v1/projects/pm-mcp-clients/epics/ep-001/decomposition".equals(request.target())
                                && "GET".equals(request.method())) {
                response.json(200, """
                                    {"epic_id":"ep-001","status":"review","revision":1,
                                     "workstreams":[{"id":"WS-1","name":"Reconnect","order":1,
                                       "staged_plans":[{"plan_id":"relay-cursor-order","title":"relay-cursor-order",
                                         "expected_surface":["pm-relay"],"dependencies":[],"status":"staged"}]}],
                                     "findings":[]}
                                    """);
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("epic", "decompose", "ep-001");
        var output = out.toString();
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(output.contains("decomposition")),
                            () -> assertTrue(output.contains("Reconnect")),
                            () -> assertTrue(output.contains("relay-cursor-order"))
        );
    }

    @Test
    @DisplayName("epic archive archives ended epic")
    void epicArchive() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/epics/ep-001/archive".equals(request.target()) && "POST".equals(request.method())) {
                response.send(200, null, "");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("epic", "archive", "ep-001");
        assertEquals(0, code);
        assertTrue(out.toString().contains("✓ ep-001 archived"));
    }

    @Test
    @DisplayName("epic chat communicates with epic")
    void epicChat() throws Exception {
        var answered = new AtomicBoolean();
        var chat = """
                            {"id":"C-2","subject":{"kind":"epic","id":"ep-001"},
                             "role":{"name":"decompose","model":"sonnet"},"scope":"epic","state":"chat.turn",
                             "context":{"knows":[],"as_of":"2026-10-16T10:00:00Z"},"turns":[
                               {"turn_id":"T-1","kind":"message","author":"operator","status":"complete",
                                "format":"plain","untrusted":false,"text":"status please","steps":[],"refs":[],
                                "at":"2026-10-16T10:00:00Z"},
                               {"turn_id":"T-2","kind":"message","author":"decompose","status":"complete",
                                "format":"markdown","untrusted":false,"model":"sonnet",
                                "text":"the epic runs two plans","steps":[],"refs":[],
                                "at":"2026-10-16T10:01:00Z"}]}
                            """;
        fixture.start((request, response) -> {
            if ("/api/v1/epics/ep-001/chat/turns".equals(request.target())) {
                answered.set(true);
                response.json(202, "{\"turn_id\":\"T-1\"}");
            } else if ("/api/v1/epics/ep-001/chat".equals(request.target())) {
                response.json(200, chat);
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("epic", "chat", "ep-001", "--message", "status please");
        assertEquals(0, code);
        assertTrue(answered.get(), "the message was not sent as a chat turn");
        assertTrue(out.toString().contains("the epic runs two plans"), out.toString());
    }

    @Test
    @DisplayName("epic cancel cancels epic")
    void epicCancel() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/epics/ep-001/cancel".equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("epic", "cancel", "ep-001", "--reason", "Scope cancelled");
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(out.toString().contains("✓ ep-001 cancelled"))
        );
    }

    @Nested
    @DisplayName("epic cancel with contradictory flags")
    class CancelFlags {

        private final String epicId = Generators.letterStrings(6, 12).next();
        private final String reason = Generators.letterStrings(8, 16).next();

        private FakeRuntimeServer serveCancel() throws IOException {
            var path = "/api/v1/epics/" + epicId + "/cancel";
            return fixture.start((request, response) -> {
                if (path.equals(request.target()) && "POST".equals(request.method())) {
                    response.json(200, "{\"ok\":true}");
                } else {
                    response.send(404, null, "");
                }
            });
        }

        @Test
        @DisplayName("--with-plans with --without-plans is a usage error and sends nothing")
        void plansFlagsRejected() throws Exception {
            var server = serveCancel();

            int code = execute("epic", "cancel", epicId, "--reason", reason, "--with-plans", "--without-plans");
            assertAll(
                                () -> assertEquals(2, code, err.toString()),
                                () -> assertTrue(err.toString().contains(
                                                                        "--with-plans and --without-plans are mutually exclusive."),
                                                    err.toString()),
                                () -> assertTrue(server.requests().isEmpty(), server.requests().toString())
            );
        }

        @Test
        @DisplayName("--close-pr with --keep-pr is a usage error and sends nothing")
        void pullRequestFlagsRejected() throws Exception {
            var server = serveCancel();

            int code = execute("epic", "cancel", epicId, "--reason", reason, "--close-pr", "--keep-pr");
            assertAll(
                                () -> assertEquals(2, code, err.toString()),
                                () -> assertTrue(err.toString().contains(
                                                                        "--close-pr and --keep-pr are mutually exclusive."),
                                                    err.toString()),
                                () -> assertTrue(server.requests().isEmpty(), server.requests().toString())
            );
        }

        @Test
        @DisplayName("--with-plans alone cancels the epic's plans as well")
        void withPlansAlone() throws Exception {
            var server = serveCancel();

            int code = execute("epic", "cancel", epicId, "--reason", reason, "--with-plans");
            var body = server.requests().getFirst().bodyText();
            assertAll(
                                () -> assertEquals(0, code, err.toString()),
                                () -> assertTrue(body.contains("\"with_plans\":true"), body)
            );
        }

        @Test
        @DisplayName("--without-plans alone leaves the epic's plans running")
        void withoutPlansAlone() throws Exception {
            var server = serveCancel();

            int code = execute("epic", "cancel", epicId, "--reason", reason, "--without-plans");
            var body = server.requests().getFirst().bodyText();
            assertAll(
                                () -> assertEquals(0, code, err.toString()),
                                () -> assertTrue(body.contains("\"with_plans\":false"), body)
            );
        }
    }
}
