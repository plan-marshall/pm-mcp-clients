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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jline.terminal.impl.DumbTerminal;

import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.testsupport.FakeRuntimeServer;
import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The operator gate on act paths (PM-SEC-3, CLI-CC-S-02): with a coding-agent marker set, the
 * read-only verbs run and the act verbs refuse with exit code {@code 4} and the catalogue text.
 *
 * <p>Every row of the two matrices is one verb of the client. A read-only row runs in a
 * non-interactive context, so no verb can open a dialogue; an act row runs in an interactive one,
 * so the agent marker alone decides. Both carry {@code CLAUDECODE=1}.
 */
@DisplayName("the operator gate on act paths")
class OperatorGateScopeTest {

    private static final String AGENT = "CLAUDECODE";

    private RuntimeFixture fixture;
    private FakeRuntimeServer server;

    @BeforeEach
    void setUp() throws IOException {
        fixture = RuntimeFixture.create();
        PmOperatorTest.writeLiveRecord(fixture);
        server = fixture.start((request, response) -> response.json(200, "{}"));
    }

    @AfterEach
    void tearDown() throws IOException {
        server.close();
        fixture.close();
    }

    /** The read-only verbs of the client: they read, they send nothing. */
    static List<Arguments> readOnlyVerbs() {
        return asArguments(
                            new String[]{"questions"},
                            new String[]{"reports"},
                            new String[]{"reports", "--older"},
                            new String[]{"projects", "--list"},
                            new String[]{"plan", "fix-relay", "--print"},
                            new String[]{"plan", "report", "fix-relay"},
                            new String[]{"plan", "settings", "fix-relay"},
                            new String[]{"plan", "findings", "fix-relay"},
                            new String[]{"plan", "watches", "fix-relay"},
                            new String[]{"epic", "relay-hardening"},
                            new String[]{"epic", "findings", "relay-hardening"},
                            new String[]{"epic", "watches", "relay-hardening"},
                            new String[]{"roles"},
                            new String[]{"roles", "list"},
                            new String[]{"roles", "show", "implementer"},
                            new String[]{"config"},
                            new String[]{"config", "show"},
                            new String[]{"settings"},
                            new String[]{"settings", "machine"},
                            new String[]{"credentials", "status", "github"},
                            new String[]{"web", "devices"},
                            new String[]{"project", "pm-mcp-clients"},
                            new String[]{"runtime", "upgrade", "--check"},
                            new String[]{"runtime", "upgrade", "--notes"},
                            new String[]{"lesson", "check", "les-0007", "--project", "pm-mcp-clients"});
    }

    /** The act verbs of the client: each of them sends or changes something. */
    static List<Arguments> actVerbs() {
        return asArguments(
                            new String[]{"answer", "Q-007"},
                            new String[]{"plan", "add", "--project", "pm-mcp-clients", "--description", "x"},
                            new String[]{"plan", "chat", "fix-relay", "--message", "hello"},
                            new String[]{"plan", "pause", "fix-relay"},
                            new String[]{"plan", "start", "fix-relay"},
                            new String[]{"plan", "cancel", "fix-relay", "--reason", "because"},
                            new String[]{"plan", "abandon", "fix-relay", "--reason", "because"},
                            new String[]{"plan", "resolve", "fix-relay", "HLD-0001", "--act", "resume"},
                            new String[]{"plan", "settings", "fix-relay", "--set", "models.plan=opus"},
                            new String[]{"epic", "next", "relay-hardening"},
                            new String[]{"epic", "add", "--project", "pm-mcp-clients", "--description", "x"},
                            new String[]{"epic", "archive", "relay-hardening"},
                            new String[]{"epic", "cancel", "relay-hardening", "--reason", "because"},
                            new String[]{"epic", "chat", "relay-hardening", "--message", "hello"},
                            new String[]{"epic", "decompose", "relay-hardening"},
                            new String[]{"epic", "review", "relay-hardening", "--agree"},
                            new String[]{"epic", "instruct", "relay-hardening", "--plan", "relay-auth-v2",
                                                "--instruction", "change it"},
                            new String[]{"enrol", "--pilot", "."},
                            new String[]{"unenrol", "pm-mcp-clients", "--yes"},
                            new String[]{"enrol-harness", "claude", "--yes"},
                            new String[]{"accept-edit", "marshal.json", "--project", "pm-mcp-clients", "--force"},
                            new String[]{"abort", "plan", "fix-relay", "--reason", "because"},
                            new String[]{"credentials", "rotate", "github"},
                            new String[]{"credentials", "remove", "github"},
                            new String[]{"credentials", "set", "github", "--kind", "fine-grained"},
                            new String[]{"config", "set", "language.ui", "de"},
                            new String[]{"config", "unset", "language.ui"},
                            new String[]{"config", "language", "--interface", "de"},
                            new String[]{"config", "consents"},
                            new String[]{"cleanup"},
                            new String[]{"cleanup", "--delete", "--yes"},
                            new String[]{"cleanup", "--kept"},
                            new String[]{"cleanup", "--release", "1"},
                            new String[]{"runtime", "stop"},
                            new String[]{"runtime", "restart"},
                            new String[]{"runtime", "upgrade"},
                            new String[]{"runtime", "upgrade", "--withdraw"},
                            new String[]{"web", "enable"},
                            new String[]{"web", "disable"},
                            new String[]{"web", "pair"},
                            new String[]{"web", "qr"},
                            new String[]{"web", "open"},
                            new String[]{"web", "devices", "revoke", "dev-1"},
                            new String[]{"roles", "qualify", "implementer", "--yes"},
                            new String[]{"project", "setup", "my-service"},
                            new String[]{"project", "add", "."},
                            new String[]{"project", "favourite", "my-service"},
                            new String[]{"project", "release", "my-service", "--reason", "archived", "--yes"},
                            new String[]{"project", "unenrol", "my-service", "--yes"},
                            new String[]{"start", "plan", "pm-mcp-clients", "--prompt", "x"},
                            new String[]{"start", "epic", "pm-mcp-clients", "--prompt", "x"},
                            new String[]{"onboard", "--auto", "--yes"},
                            new String[]{"recipe", "run", "--project", "pm-mcp-clients"},
                            new String[]{"client", "install", "claude"},
                            new String[]{"client", "remove", "claude"},
                            new String[]{"git-source", "adopt-pr", "4711", "--project", "pm-mcp-clients"},
                            new String[]{"git-source", "local-changes", "--project", "pm-mcp-clients"});
    }

    /**
     * @param verbs the verb rows
     * @return one argument set per row, the whole argument vector as its single argument
     */
    private static List<Arguments> asArguments(String[]... verbs) {
        var rows = new ArrayList<Arguments>();
        for (var verb : verbs) {
            rows.add(Arguments.of((Object) verb));
        }
        return rows;
    }

    @ParameterizedTest
    @MethodSource("readOnlyVerbs")
    @DisplayName("a read-only verb runs with a coding-agent marker set")
    void readOnlyVerbRuns(String[] args) {
        var result = run(false, args);

        assertNotEquals(OperatorGate.EXIT_OPERATOR_CHECKS, result.code(),
                            String.join(" ", args) + " was refused: " + result.err());
        assertFalse(result.err().contains(refusalText("en")),
                            String.join(" ", args) + " was refused: " + result.err());
    }

    @ParameterizedTest
    @MethodSource("actVerbs")
    @DisplayName("an act verb refuses with exit code 4 and the refusal text")
    void actVerbRefuses(String[] args) {
        var result = run(true, args);

        assertEquals(OperatorGate.EXIT_OPERATOR_CHECKS, result.code(), String.join(" ", args));
        assertFalse(result.err().isBlank(), String.join(" ", args) + " wrote no refusal");
    }

    @Test
    @DisplayName("the act refusal names the marker that was set, not a fixed variable")
    void actRefusalNamesTheMarker() {
        var result = run(true, "runtime", "stop");

        assertEquals(refusalText("en"), lastLine(result.err()), result.err());
    }

    @ParameterizedTest
    @MethodSource("readOnlyVerbs")
    @DisplayName("the console dispatcher does not take a read-only verb for an act")
    void readOnlyVerbIsNotAnAct(String[] args) {
        assertFalse(OperatorActs.isAct(String.join(" ", args)), String.join(" ", args));
    }

    @ParameterizedTest
    @MethodSource("actVerbs")
    @DisplayName("the console dispatcher takes an act verb for an act")
    void actVerbIsAnAct(String[] args) {
        assertTrue(OperatorActs.isAct(String.join(" ", args)), String.join(" ", args));
    }

    @Test
    @DisplayName("the read-only verbs the review names print with a coding-agent marker set")
    void readOnlyVerbsPrint() {
        var verbs = List.of(
                            new String[]{"questions"},
                            new String[]{"reports"},
                            new String[]{"projects", "--list"},
                            new String[]{"plan", "fix-relay", "--print"});
        for (var args : verbs) {
            var result = run(false, args);

            assertEquals(0, result.code(), String.join(" ", args) + ": " + result.err());
            assertFalse(result.out().isBlank(), String.join(" ", args) + " printed nothing");
        }
    }

    @Test
    @DisplayName("the console entry is not gated: a monitor-only verb still enters")
    void consoleEntryIsNotGated() {
        var environment = markerEnvironment();
        environment.put("LANG", "en_US.UTF-8");
        var context = new OperatorContext(environment, fixture.base().getParent(), true,
                            ClientEnvironment.capture(environment), () -> {
                                throw new IOException("no terminal in this test");
                            }, null);
        var out = new StringWriter();
        var err = new StringWriter();

        var code = PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err));

        assertNotEquals(OperatorGate.EXIT_OPERATOR_CHECKS, code, err.toString());
        assertFalse(err.toString().contains(refusalText("en")), err.toString());
        assertTrue(err.toString().contains("no terminal in this test"), "the console was not entered: " + err);
    }

    @Test
    @DisplayName("the console dispatcher refuses a typed act, sends nothing, and the entry flow sends no act")
    void consoleRefusesAct() throws Exception {
        var requests = new CopyOnWriteArrayList<String>();
        server.close();
        server = fixture.start((request, response) -> {
            requests.add(request.method() + " " + request.target());
            if ("/api/v1/status".equals(request.target())) {
                response.json(200, "{\"status\":\"ready\",\"onboarded\":true,\"version\":\"0.9.0\","
                                    + "\"needs_operator\":{},\"web\":{\"enabled\":true,\"port\":7420}}");
            } else {
                response.json(200, "{}");
            }
        });
        var environment = markerEnvironment();
        var rendered = new ByteArrayOutputStream();
        var typed = "answer Q-007\nexit\n";
        var context = new OperatorContext(environment, fixture.base().getParent(), true,
                            ClientEnvironment.capture(environment),
                            () -> new DumbTerminal(new ByteArrayInputStream(typed.getBytes(StandardCharsets.UTF_8)),
                                                rendered),
                            null);

        var code = new PmConsole(context).run();

        var text = rendered.toString(StandardCharsets.UTF_8);
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(text.contains(refusalText("en")), text),
                            () -> assertFalse(requests.stream().anyMatch(r -> r.contains("/decisions")),
                                                requests.toString()),
                            () -> assertFalse(requests.stream()
                                                                    .anyMatch(r -> r.startsWith("PUT") && r.contains("/api/v1/web")),
                                                requests.toString()),
                            () -> assertFalse(requests.stream().anyMatch(r -> r.contains("pairings")),
                                                requests.toString()));
    }

    /** @return the refusal text the catalogue answers in the given locale */
    static String refusalText(String locale) {
        return MessageCatalogue.format(locale, "operator.gate.agent.refused",
                            Map.of("marker", AGENT));
    }

    private static String lastLine(String text) {
        var lines = new ArrayList<>(List.of(text.strip().split("\n")));
        return lines.getLast().replaceFirst("^[a-z-]+: ", "");
    }

    private Map<String, String> markerEnvironment() {
        var environment = new HashMap<>(fixture.environment());
        environment.put(AGENT, "1");
        environment.put("LANG", "en_US.UTF-8");
        return environment;
    }

    private Result run(boolean interactive, String... args) {
        var environment = markerEnvironment();
        var context = new OperatorContext(environment, fixture.base().getParent(), interactive,
                            ClientEnvironment.capture(environment));
        var out = new StringWriter();
        var err = new StringWriter();
        var code = PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err), args);
        return new Result(code, out.toString(), err.toString());
    }

    /** One run of the command line. */
    private record Result(int code, String out, String err) {
    }
}
