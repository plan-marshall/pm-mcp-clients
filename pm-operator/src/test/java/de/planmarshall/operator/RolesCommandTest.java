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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.jline.terminal.impl.DumbTerminal;

import de.planmarshall.api.resource.RoleDetailResource;
import de.planmarshall.api.resource.RoleQualificationState;
import de.planmarshall.api.resource.RoleSummaryResource;
import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("RolesCommand")
@EnabledOnOs({OS.MAC, OS.LINUX})
class RolesCommandTest {

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

    private int execute(String input, String... args) {
        return executeIn(fixture.environment(), input, args);
    }

    private int executeInGerman(String input, String... args) {
        var environment = new HashMap<>(fixture.environment());
        environment.put("LANG", "de_DE.UTF-8");
        return executeIn(environment, input, args);
    }

    private int executeIn(Map<String, String> environment, String input, String... args) {
        var context = new OperatorContext(environment, tempDir, true,
                            ClientEnvironment.capture(environment),
                            () -> new DumbTerminal(
                                                new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                                                new ByteArrayOutputStream()),
                            _ -> {
                            });
        return PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err), args);
    }

    private static RoleSummaryResource summary(String role, String qualification) {
        return new RoleSummaryResource(role, role, role + " role", List.of("pm_wait", "pm_do"),
                            "shipped", "writer", "fresh", "claude", "opus-4.5", "high",
                            "project", "machine", "shipped",
                            RoleQualificationState.requireWire(qualification), false);
    }

    private static RoleDetailResource detail(String role) {
        return new RoleDetailResource(role, role, role + " role",
                            List.of("pm_wait", "pm_do", "edit"),
                            new RoleDetailResource.NarrowedEffort("high", "the harness narrows high to medium"),
                            new RoleDetailResource.Qualification(RoleQualificationState.QUALIFIED,
                                                "record valid", List.of("correctness", "tool use")),
                            Map.of(
                                                "source_set", "shipped",
                                                "class", "writer",
                                                "form", "fresh",
                                                "harness", "claude",
                                                "model", "opus-4.5",
                                                "effort", "high",
                                                "corpus_topics", List.of("java", "testing"),
                                                "item_counts", "fresh 12, warm 12",
                                                "expected_tokens", "42000",
                                                "expected_time", "6 minutes"));
    }

    @Test
    @DisplayName("list renders the effective role set with qualification and guard")
    void listsRoles() throws Exception {
        var server = fixture.start((request, response) -> {
            if ("/api/v1/roles".equals(request.target())) {
                response.json(200, "{\"roles\":["
                                    + summary("implementer", "qualified").toJson() + ","
                                    + summary("judge", "unqualified").toJson() + "]}");
            } else {
                response.send(404, null, "");
            }
        });

        assertEquals(0, execute("", "roles", "list"));

        var output = out.toString();
        assertTrue(output.contains("Roles (2)"), output);
        assertTrue(output.contains("implementer"), output);
        assertTrue(output.contains("judge"), output);
        assertTrue(output.contains("shipped"), output);
        assertTrue(output.contains("qualified"), output);
        assertTrue(output.contains("unqualified"), output);
        assertEquals("/api/v1/roles", server.awaitRequest(Duration.ofSeconds(1)).target());
    }

    @Test
    @DisplayName("list passes the project and reports an empty set")
    void listsProjectRoles() throws Exception {
        var server = fixture.start((request, response) -> response.json(200, "{\"roles\":[]}"));

        assertEquals(0, execute("", "roles", "list", "--project", "my-service"));

        assertEquals("No roles.", out.toString().strip());
        assertEquals("/api/v1/roles?project=my-service",
                            server.awaitRequest(Duration.ofSeconds(1)).target());
    }

    @Test
    @DisplayName("list reports a refusal")
    void listRefusal() throws Exception {
        fixture.start((request, response) -> response.json(500, "{\"code\":\"internal_fault\"}"));

        assertEquals(1, execute("", "roles", "list"));
        assertTrue(err.toString().contains("HTTP 500"), err.toString());
    }

    @Test
    @DisplayName("show renders the advisory tool set, narrowed effort and qualification")
    void showsRole() throws Exception {
        var server = fixture.start((request, response) -> {
            if ("/api/v1/roles/implementer?project=my-service".equals(request.target())) {
                response.json(200, detail("implementer").toJson());
            } else {
                response.send(404, null, "");
            }
        });

        assertEquals(0, execute("", "roles", "show", "implementer", "--project", "my-service"));

        var output = out.toString();
        assertTrue(output.contains("Role: implementer"), output);
        assertTrue(output.contains("Advisory tool set: pm_wait, pm_do, edit"), output);
        assertTrue(output.contains("Narrowed effort: high — the harness narrows high to medium"), output);
        assertTrue(output.contains("Qualification: qualified"), output);
        assertTrue(output.contains("Criteria: correctness, tool use"), output);
        assertTrue(output.contains("Configuration parameters:"), output);
        assertEquals("/api/v1/roles/implementer?project=my-service",
                            server.awaitRequest(Duration.ofSeconds(1)).target());
    }

    @Test
    @DisplayName("show reports a missing role")
    void showNotFound() throws Exception {
        fixture.start((request, response) -> response.json(404, "{\"error\":\"scope_not_found\"}"));

        assertEquals(1, execute("", "roles", "show", "missing"));
        assertTrue(err.toString().contains("Role missing not found."), err.toString());
    }

    @Test
    @DisplayName("qualify --yes shows the configuration and prints the measurements")
    void qualifiesRole() throws Exception {
        var server = fixture.start((request, response) -> {
            if ("GET".equals(request.method())) {
                response.json(200, detail("implementer").toJson());
            } else {
                response.json(200, "{\"fresh_agreement\":0.86,\"warm_agreement\":0.81,"
                                    + "\"warm_fresh_gap\":5,\"qualified_fresh\":true,"
                                    + "\"qualified_warm\":true,\"record_path\":"
                                    + "\"/base/projects/my-service/roles/qualification/implementer.json\"}");
            }
        });

        assertEquals(0, execute("", "roles", "qualify", "implementer", "--project", "my-service", "--yes"));

        var output = out.toString();
        assertTrue(output.contains("Qualify role implementer"), output);
        assertTrue(output.contains("Corpus topics: java, testing"), output);
        assertTrue(output.contains("✓ Qualification of implementer finished"), output);
        assertTrue(output.contains("Fresh agreement: 0.86"), output);
        assertTrue(output.contains("Warm-fresh gap: 5"), output);
        assertTrue(output.contains("Record: /base/projects/my-service/roles/qualification/implementer.json"),
                            output);

        server.awaitRequest(Duration.ofSeconds(1));
        var post = server.awaitRequest(Duration.ofSeconds(1));
        assertEquals("POST", post.method());
        assertEquals("/api/v1/roles/implementer/qualify?project=my-service", post.target());
        assertTrue(post.header("Idempotency-Key") != null && !post.header("Idempotency-Key").isBlank(),
                            "the qualification carries an idempotency key");
    }

    @Test
    @DisplayName("qualify reports a still-running qualification without claiming success")
    void qualifyPending() throws Exception {
        fixture.start((request, response) -> {
            if ("GET".equals(request.method())) {
                response.json(200, detail("implementer").toJson());
            } else {
                response.json(202, "{\"status\":\"running\"}");
            }
        });

        assertEquals(1, execute("", "roles", "qualify", "implementer", "--yes"));
        assertTrue(out.toString().contains("Qualification of implementer is still running"), out.toString());
    }

    @Test
    @DisplayName("qualify reports an unusable completion body as a failure")
    void qualifyIncomplete() throws Exception {
        fixture.start((request, response) -> {
            if ("GET".equals(request.method())) {
                response.json(200, detail("implementer").toJson());
            } else {
                response.json(200, "{\"unrelated\":true}");
            }
        });

        assertEquals(1, execute("", "roles", "qualify", "implementer", "--yes"));
        assertTrue(err.toString().contains("answered no usable measurements"), err.toString());
    }

    @Test
    @DisplayName("qualify without --yes needs confirmation in --no-input mode")
    void qualifyRequiresConfirmation() throws Exception {
        var server = fixture.start((request, response) -> response.json(200, detail("implementer").toJson()));

        assertEquals(3, execute("", "roles", "qualify", "implementer", "--no-input"));

        assertTrue(out.toString().contains("Qualification confirmation required (--yes)"), out.toString());
        assertEquals(1, server.requests().size(), "no qualification request may be sent");
    }

    @Test
    @DisplayName("a declined qualification sends nothing")
    void declinesQualification() throws Exception {
        var server = fixture.start((request, response) -> response.json(200, detail("implementer").toJson()));

        assertEquals(1, execute("n\n", "roles", "qualify", "implementer"));

        assertTrue(out.toString().contains("Qualification was not run."), out.toString());
        assertEquals(1, server.requests().size(), "no qualification request may be sent");
    }

    @Test
    @DisplayName("an interactive yes runs the qualification")
    void confirmsQualification() throws Exception {
        var server = fixture.start((request, response) -> {
            if ("GET".equals(request.method())) {
                response.json(200, detail("implementer").toJson());
            } else {
                response.json(200, "{\"fresh_agreement\":0.86,"
                                    + "\"record_path\":\"/base/projects/my-service/roles/qualification/implementer.json\"}");
            }
        });

        assertEquals(0, execute("y\n", "roles", "qualify", "implementer"));

        server.awaitRequest(Duration.ofSeconds(1));
        assertEquals("POST", server.awaitRequest(Duration.ofSeconds(1)).method());
    }

    @Test
    @DisplayName("qualify reports a refusal reason")
    void qualifyRefusal() throws Exception {
        fixture.start((request, response) -> {
            if ("GET".equals(request.method())) {
                response.json(200, detail("implementer").toJson());
            } else {
                response.json(409, "{\"error\":\"scorer_not_independent\"}");
            }
        });

        assertEquals(1, execute("", "roles", "qualify", "implementer", "--yes"));
        assertTrue(err.toString().contains("scorer_not_independent"), err.toString());
    }

    @Test
    @DisplayName("roles speaks the interface language: list and the qualification's preview in German")
    void speaksTheInterfaceLanguage() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/roles".equals(request.target())) {
                response.json(200, "{\"roles\":[]}");
            } else {
                response.json(200, detail("implementer").toJson());
            }
        });

        assertEquals(0, executeInGerman("", "roles", "list"));
        assertEquals(3, executeInGerman("", "roles", "qualify", "implementer", "--no-input"));

        var output = out.toString();
        assertAll(
                            () -> assertTrue(output.contains("Keine Rollen."), output),
                            () -> assertTrue(output.contains("Rolle implementer qualifizieren"), output),
                            () -> assertTrue(output.contains("Qualifizierung jetzt ausführen?"), output),
                            () -> assertFalse(output.contains("Qualify role"), output)
        );
    }

    @ParameterizedTest(name = "the confirmation answered {0} runs nothing")
    @ValueSource(strings = {"yeah", "j", "u", ""})
    @DisplayName("the confirmation takes only the keys it offers: another answer is named and asked again")
    void confirmationTakesOnlyItsKeys(String answer) throws Exception {
        var server = fixture.start((request, response) -> response.json(200, detail("implementer").toJson()));

        assertEquals(1, execute(answer + "\nn\n", "roles", "qualify", "implementer"));

        assertAll(
                            () -> assertTrue(out.toString().contains(answer.isEmpty() ? "Type y or n."
                                                : answer + " is not offered here: y, n, q"), out.toString()),
                            () -> assertTrue(out.toString().contains("Qualification was not run."), out.toString()),
                            () -> assertEquals(1, server.requests().size(), "no qualification request may be sent")
        );
    }

    @ParameterizedTest(name = "the confirmation left with ''{0}'' sends nothing")
    @ValueSource(strings = {"q\n", ""})
    @DisplayName("q, or the end of the input, at the confirmation leaves and sends nothing")
    void confirmationLeft(String input) throws Exception {
        var server = fixture.start((request, response) -> response.json(200, detail("implementer").toJson()));

        assertEquals(3, execute(input, "roles", "qualify", "implementer"));

        assertAll(
                            () -> assertEquals(1, server.requests().size(), "no qualification request may be sent"),
                            () -> assertFalse(out.toString().contains("Qualification was not run."), out.toString())
        );
    }

    @Test
    @DisplayName("a German operator's y is not offered: the German keys are named")
    void germanRefusesY() throws Exception {
        var server = fixture.start((request, response) -> response.json(200, detail("implementer").toJson()));

        assertEquals(1, executeInGerman("y\nn\n", "roles", "qualify", "implementer"));

        assertAll(
                            () -> assertTrue(out.toString().contains("j, n, v"), out.toString()),
                            () -> assertEquals(1, server.requests().size(), "no qualification request may be sent")
        );
    }

    @Test
    @DisplayName("a German operator confirms with j")
    void germanConfirmsWithJ() throws Exception {
        var server = fixture.start((request, response) -> {
            if ("GET".equals(request.method())) {
                response.json(200, detail("implementer").toJson());
            } else {
                response.json(201, "{\"qualified_fresh\":true,\"record_path\":\"/r/implementer.json\"}");
            }
        });

        assertEquals(0, executeInGerman("j\n", "roles", "qualify", "implementer"));

        server.awaitRequest(Duration.ofSeconds(1));
        assertEquals("POST", server.awaitRequest(Duration.ofSeconds(1)).method());
    }

    @ParameterizedTest(name = "a refusal {0} {1} names ''{2}''")
    @CsvSource(delimiter = '|', value = {
                        "400|{\"reason\":\"corpus_missing\"}|corpus_missing",
                        "404|{\"code\":\"role_unknown\"}|role_unknown",
                        "409|{}|refused: ",
                        "409|not json|refused: "})
    @DisplayName("a refused qualification names the runtime's reason, error or code, and no reason it did not give")
    void refusalNamesTheRuntimesReason(int status, String body, String named) throws Exception {
        fixture.start((request, response) -> {
            if ("GET".equals(request.method())) {
                response.json(200, detail("implementer").toJson());
            } else {
                response.json(status, body);
            }
        });

        assertEquals(1, execute("", "roles", "qualify", "implementer", "--yes"));

        assertTrue(err.toString().contains(named), err.toString());
    }

    @ParameterizedTest(name = "a qualification answered {0} is incomplete")
    @ValueSource(strings = {"", "not json", "{\"fresh_agreement\":0.9,\"record_path\":\" \"}",
                        "{\"fresh_agreement\":\"high\",\"record_path\":\"/r.json\"}"})
    @DisplayName("an answer without a record or without a measurement claims no finished qualification")
    void incompleteAnswers(String body) throws Exception {
        fixture.start((request, response) -> {
            if ("GET".equals(request.method())) {
                response.json(200, detail("implementer").toJson());
            } else {
                response.json(200, body);
            }
        });

        assertEquals(1, execute("", "roles", "qualify", "implementer", "--yes"));

        assertAll(
                            () -> assertTrue(err.toString().contains("answered no usable measurements"), err.toString()),
                            () -> assertFalse(out.toString().contains("finished"), out.toString())
        );
    }

    @Test
    @DisplayName("a role the runtime cannot read, or a qualification it fails, exits 1 without running")
    void runtimeFailures() throws Exception {
        var posts = new AtomicInteger();
        fixture.start((request, response) -> {
            if ("POST".equals(request.method())) {
                posts.incrementAndGet();
            }
            response.json(500, "{\"error\":\"internal\"}");
        });

        assertEquals(1, execute("", "roles", "qualify", "implementer", "--yes"));

        assertAll(
                            () -> assertTrue(err.toString().contains("Failed to qualify implementer: HTTP 500"), err.toString()),
                            () -> assertEquals(0, posts.get())
        );
    }

    @Test
    @DisplayName("a qualification the runtime fails with 500 is its refusal")
    void qualificationFailsWith500() throws Exception {
        fixture.start((request, response) -> {
            if ("GET".equals(request.method())) {
                response.json(200, detail("implementer").toJson());
            } else {
                response.json(500, "{\"error\":\"internal\"}");
            }
        });

        assertEquals(1, execute("", "roles", "qualify", "implementer", "--yes"));

        assertTrue(err.toString().contains("roles qualify implementer"), err.toString());
    }

    @Test
    @DisplayName("the preview leaves out what the role names nothing for")
    void previewLeavesOutWhatIsAbsent() throws Exception {
        var bare = new RoleDetailResource("judge", "judge", "judge role", List.of(), null,
                            new RoleDetailResource.Qualification(RoleQualificationState.UNQUALIFIED, null, List.of()),
                            Map.of("corpus_topics", List.of()));
        fixture.start((request, response) -> response.json(200, bare.toJson()));

        assertEquals(3, execute("", "roles", "qualify", "judge", "--no-input"));

        var output = out.toString();
        assertAll(
                            () -> assertTrue(output.contains("Qualify role judge"), output),
                            () -> assertFalse(output.contains("Corpus topics"), output),
                            () -> assertFalse(output.contains("Expected"), output)
        );
    }
}
