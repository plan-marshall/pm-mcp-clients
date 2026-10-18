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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
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
        var environment = fixture.environment();
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
        var server = fixture.start((request, response) -> response.json(200, detail("implementer").toJson()));

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
}
