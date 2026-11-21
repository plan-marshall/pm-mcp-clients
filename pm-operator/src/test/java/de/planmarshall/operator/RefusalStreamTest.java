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
import java.util.stream.Stream;

import de.planmarshall.api.resource.ApiErrorCode;
import de.planmarshall.api.resource.ErrorTexts;
import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * One stream rule: a refusal of a verb run as one shell command goes to stderr, and the verbs that
 * already printed there keep doing so. The interactive console keeps its one stream and is covered
 * by its own transcript tests.
 */
@DisplayName("a refusal of a one-shot verb goes to stderr")
@EnabledOnOs({OS.MAC, OS.LINUX})
@EnableGeneratorController
class RefusalStreamTest {

    @TempDir
    Path tempDir;

    private RuntimeFixture fixture;
    private StringWriter out;
    private StringWriter err;

    @BeforeEach
    void setUp() throws IOException {
        System.setProperty("pm.test.no.browser", "true");
        fixture = RuntimeFixture.create();
        out = new StringWriter();
        err = new StringWriter();
    }

    @AfterEach
    void tearDown() throws IOException {
        System.clearProperty("pm.test.no.browser");
        fixture.close();
    }

    private int execute(String... args) {
        var context = new OperatorContext(
                            fixture.environment(),
                            tempDir,
                            true,
                            ClientEnvironment.capture(fixture.environment()),
                            null,
                            _ -> {
                            }
        );
        return PmOperator.execute(context, new PrintWriter(out, true), new PrintWriter(err, true), args);
    }

    private void refuseEverything() throws IOException {
        PmOperatorTest.writeLiveRecord(fixture);
        var detail = Generators.letterStrings(6, 20).next();
        fixture.start((request, response) -> response.send(404, null,
                            "{\"error\":\"scope_not_found\",\"message\":\"" + detail + "\"}"));
    }

    @Test
    @DisplayName("plan <unknown id> prints its refusal on stderr and nothing on stdout")
    void planRefusalIsOnStderr() throws Exception {
        refuseEverything();
        var id = Generators.letterStrings(4, 10).next();
        assertEquals(1, execute("plan", id));
        assertTrue(err.toString().contains("\u2717"), err.toString());
        assertTrue(err.toString().contains("No plan, epic, decision, report or entry carries this id."), err.toString());
        assertFalse(out.toString().contains("\u2717"), out.toString());
    }

    @Test
    @DisplayName("epic <unknown id> prints its refusal on stderr and nothing on stdout")
    void epicRefusalIsOnStderr() throws Exception {
        refuseEverything();
        var id = Generators.letterStrings(4, 10).next();
        assertEquals(1, execute("epic", id));
        assertTrue(err.toString().contains("\u2717"), err.toString());
        assertTrue(err.toString().contains("No plan, epic, decision, report or entry carries this id."), err.toString());
        assertFalse(out.toString().contains("\u2717"), out.toString());
    }

    @Test
    @DisplayName("config cleanup prints its refusal on stderr and nothing on stdout")
    void configCleanupRefusalIsOnStderr() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        fixture.start((request, response) -> response.send(500, null, ""));

        assertEquals(1, execute("config", "cleanup"));
        assertTrue(err.toString().contains("\u2717"), err.toString());
        assertFalse(out.toString().contains("\u2717"), out.toString());
    }

    /**
     * @return one refused runtime read or act per one-shot site, the printers of the cause-less
     *         failure family among them; every site prints the three-line refusal, or its own kept
     *         line with the refusal's text as the cause, on stderr
     */
    static Stream<Arguments> refusedSites() {
        var key = "pm.refusal." + Generators.letterStrings(4, 10).next();
        var id = Generators.letterStrings(4, 10).next();
        return Stream.of(
                            Arguments.of("client status", List.of("client", "status")),
                            Arguments.of("client install", List.of("client", "install")),
                            Arguments.of("config", List.of("config")),
                            Arguments.of("config set", List.of("config", "set", key, id)),
                            Arguments.of("config unset", List.of("config", "unset", key)),
                            Arguments.of("config consents", List.of("config", "consents")),
                            Arguments.of("config documents", List.of("config", "documents", "--mode", "copy")),
                            Arguments.of("roles show", List.of("roles", "show", id)),
                            Arguments.of("roles qualify", List.of("roles", "qualify", id, "--yes")),
                            Arguments.of("credentials status", List.of("credentials", "status", id)),
                            Arguments.of("credentials remove", List.of("credentials", "remove", id)),
                            Arguments.of("web devices", List.of("web", "devices")),
                            Arguments.of("accept-edit", List.of("accept-edit", "workspace", id, "--project", id)),
                            Arguments.of("epic decompose", List.of("epic", "decompose", id, "--project", id)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("refusedSites")
    @DisplayName("a refused runtime answer prints its cause on stderr, nothing on stdout, and exits 1")
    void refusedSiteNamesItsCauseOnStderr(String site, List<String> args) throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        fixture.start((request, response) -> response.send(500, null, "{\"error\":\"internal_fault\"}"));
        var cause = ErrorTexts.title("en", ApiErrorCode.INTERNAL_FAULT);

        assertEquals(1, execute(args.toArray(new String[0])), site + ": " + err);
        assertTrue(err.toString().contains(cause), site + " must name the refusal on stderr: " + err);
        assertFalse(err.toString().contains("HTTP"), site + " must not name an HTTP status: " + err);
        assertFalse(out.toString().contains(cause), site + " must print no refusal on stdout: " + out);
        assertFalse(out.toString().contains("✗"), site + " must print no refusal on stdout: " + out);
    }

    /**
     * The mock's empty-body 404 stands in for the real daemon's here: what the daemon answers for an
     * unknown plan or epic is not tested against the daemon itself.
     */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"plan", "epic", "epic decompose"})
    @DisplayName("an empty-body 404 renders the same refusal for plan <id>, epic <id> and epic decompose")
    void emptyBodyNotFoundRendersTheSameRefusal(String verb) throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        fixture.start((request, response) -> response.send(404, null, ""));
        var id = Generators.letterStrings(4, 10).next();
        var args = "epic decompose".equals(verb)
                            ? new String[]{"epic", "decompose", id, "--project", id}
                            : new String[]{verb, id};

        assertEquals(1, execute(args));
        assertTrue(err.toString().contains("✗ This runtime does not know the request"), err.toString());
        assertFalse(err.toString().contains("No plan, epic, decision, report or entry carries this id."),
                            err.toString());
        assertFalse(out.toString().contains("✗"), out.toString());
    }

    @Test
    @DisplayName("answer <unknown id> prints its not-found line on stderr")
    void answerUnknownIdIsOnStderr() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        fixture.start((request, response) -> response.send(404, null, ""));
        var id = "Q-" + Generators.letterStrings(4, 10).next();

        assertEquals(1, execute("answer", id));
        assertTrue(err.toString().contains(id), err.toString());
        assertFalse(out.toString().contains(id), out.toString());
    }

    @Test
    @DisplayName("plan settings --set without a value prints its line on stderr")
    void planSettingsValueRequiredIsOnStderr() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        fixture.start((request, response) -> response.json(200, "{\"plan_id\":\"p\",\"revision\":1,\"values\":[]}"));
        var plan = Generators.letterStrings(4, 10).next();

        assertEquals(1, execute("plan", "settings", plan, "--set", Generators.letterStrings(4, 10).next()));
        assertTrue(err.toString().contains("Give the value as --set aspect=value."), err.toString());
        assertFalse(out.toString().contains("Give the value"), out.toString());
    }

    @Test
    @DisplayName("reports read without ids prints its local refusal on stderr")
    void reportsReadWithoutIdsIsOnStderr() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        fixture.start((request, response) -> response.send(500, null, ""));

        assertEquals(1, execute("reports", "read"));
        assertTrue(err.toString().contains("✗ Give the report ids, or --all."), err.toString());
        assertFalse(out.toString().contains("Give the report ids"), out.toString());
    }

    @Test
    @DisplayName("a verb that already printed its refusal on stderr keeps doing so (the positive control)")
    void refusalThatAlreadyWentToStderrStays() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        fixture.start((request, response) -> response.send(500, null,
                            "{\"error\":\"internal_error\",\"message\":\"the runtime failed\"}"));

        assertEquals(1, execute("roles", "list"));
        assertTrue(err.toString().contains("the runtime failed"), err.toString());
        assertFalse(out.toString().contains("the runtime failed"), out.toString());
    }

    /** The machine settings of a runtime that remembers one consent. */
    private static final String SETTINGS_WITH_CONSENT = "{\"revision\":1,\"consents\":[{\"kind\":\"verify_providers\","
                        + "\"status\":\"remembered\",\"remember\":true}]}";

    private static final String INTERNAL_FAULT_BODY = "{\"error\":\"internal_fault\"}";

    private void assertRefusedWithCause(int code, String cause) {
        assertEquals(1, code, out + "\n" + err);
        assertTrue(err.toString().contains(cause), "the refusal names its cause on stderr: " + err);
        assertFalse(out.toString().contains(cause), "stdout carries none of it: " + out);
        assertFalse(out.toString().contains("✗"), "stdout carries none of it: " + out);
    }

    @Test
    @DisplayName("config consents --withdraw whose withdrawal the runtime refuses names the cause on stderr")
    void refusedWithdrawalIsOnStderr() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        fixture.start((request, response) -> {
            if ("GET".equals(request.method())) {
                response.json(200, SETTINGS_WITH_CONSENT);
            } else {
                response.send(500, null, INTERNAL_FAULT_BODY);
            }
        });

        int code = execute("config", "consents", "--withdraw", "verify_providers", "--yes");

        assertRefusedWithCause(code, ErrorTexts.title("en", ApiErrorCode.INTERNAL_FAULT));
    }

    @Test
    @DisplayName("config documents whose update the runtime refuses names the cause on stderr")
    void refusedDocumentsUpdateIsOnStderr() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        fixture.start((request, response) -> {
            if ("GET".equals(request.method())) {
                response.json(200, SETTINGS_WITH_CONSENT);
            } else {
                response.send(500, null, INTERNAL_FAULT_BODY);
            }
        });

        int code = execute("config", "documents", "--mode", "copy");

        assertRefusedWithCause(code, ErrorTexts.title("en", ApiErrorCode.INTERNAL_FAULT));
    }

    @Test
    @DisplayName("bare config of a runtime without machine settings whose value read is refused names the cause on stderr")
    void refusedValueReadOfBareConfigIsOnStderr() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        fixture.start((request, response) -> {
            if ("/api/v1/config".equals(request.target())) {
                response.send(500, null, INTERNAL_FAULT_BODY);
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("config");

        assertRefusedWithCause(code, ErrorTexts.title("en", ApiErrorCode.INTERNAL_FAULT));
    }

    @Test
    @DisplayName("bare config whose values the runtime serves unreadable refuses on stderr")
    void unreadableValuesOfBareConfigAreOnStderr() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        var body = Generators.letterStrings(6, 20).next();
        fixture.start((request, response) -> {
            if ("/api/v1/config".equals(request.target())) {
                response.send(200, "application/json", body);
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("config");

        assertRefusedWithCause(code, "The runtime did not report its configuration values.");
    }

    @Test
    @DisplayName("config consents of a runtime that serves no machine settings renders that refusal on stderr")
    void consentsWithoutMachineSettingsIsOnStderr() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        fixture.start((request, response) -> response.send(404, null, ""));

        int code = execute("config", "consents");

        assertRefusedWithCause(code, "✗ This runtime does not know the request");
    }

    @Test
    @DisplayName("roles show refused with a 4xx code names the code's text as the cause, never the status")
    void refusedRoleReadNamesTheCodeText() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        fixture.start((request, response) -> response.send(409, null, "{\"error\":\"conflict\"}"));
        var role = Generators.letterStrings(4, 10).next();

        int code = execute("roles", "show", role);

        assertRefusedWithCause(code, "Failed to read role " + role + ": "
                            + ErrorTexts.title("en", ApiErrorCode.CONFLICT));
        assertFalse(err.toString().contains("409"), err.toString());
    }

    @Test
    @DisplayName("epic decompose of an epic that names no project refuses in the operator's locale")
    void epicWithoutProjectIsRefusedInTheLocale() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        fixture.start((request, response) -> response.json(200, "{}"));
        var epic = Generators.letterStrings(4, 10).next();
        var environment = new HashMap<>(fixture.environment());
        environment.put("LANG", "de_DE.UTF-8");
        var context = new OperatorContext(environment, tempDir, true, ClientEnvironment.capture(environment), null,
                            _ -> {
                            });

        int code = PmOperator.execute(context, new PrintWriter(out, true), new PrintWriter(err, true),
                            "epic", "decompose", epic);

        assertRefusedWithCause(code, "Epic " + epic + ": das Epic der Runtime nennt kein Projekt");
    }
}
