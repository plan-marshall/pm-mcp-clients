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
import java.util.Locale;

import de.planmarshall.api.resource.ApiErrorCode;
import de.planmarshall.api.resource.ErrorTexts;
import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@EnableGeneratorController
@DisplayName("ClientCommand and DrainCommand")
class ClientCommandTest {

    @TempDir
    Path tempDir;

    private RuntimeFixture fixture;
    private StringWriter out;
    private StringWriter err;

    @BeforeEach
    void setUp() throws IOException {
        fixture = RuntimeFixture.create();
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

    @Test
    @DisplayName("a refused client status renders the refusal on stderr and exits 1")
    void clientStatusRefusal() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        fixture.start((request, response) -> response.json(500, "{\"error\":\"internal_fault\"}"));

        int code = execute("client", "status");

        var title = ErrorTexts.title("en", ApiErrorCode.INTERNAL_FAULT);
        assertAll(
                            () -> assertEquals(1, code),
                            () -> assertTrue(err.toString().contains("✗ " + title), err.toString()),
                            () -> assertFalse(out.toString().contains(title), out.toString()),
                            () -> assertFalse(err.toString().contains("Failed to query"), err.toString()));
    }

    @Test
    @DisplayName("a German client install failure prints one grammatical sentence with its cause")
    void germanClientInstallFailure() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        fixture.start((request, response) -> response.json(500, "{\"error\":\"internal_fault\"}"));
        var environment = new HashMap<>(fixture.environment());
        environment.put("LANG", "de_DE.UTF-8");
        var context = new OperatorContext(environment, tempDir, true);

        int code = PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err), "client", "install");

        assertAll(
                            () -> assertEquals(1, code),
                            () -> assertTrue(err.toString().contains("Die Client-Bundles konnten nicht installiert werden: "
                                                + ErrorTexts.title("de", ApiErrorCode.INTERNAL_FAULT)), err.toString()),
                            () -> assertFalse(err.toString().contains("install werden"), err.toString()),
                            () -> assertFalse(out.toString().contains("Client-Bundles"), out.toString()));
    }

    @Test
    @DisplayName("client status lists client bundles")
    void clientStatus() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        fixture.start((request, response) -> {
            if ("/api/v1/client/bundles".equals(request.target())) {
                response.json(200, """
                                    [
                                      {"name":"claude","status":"installed","version":"2.4.1"},
                                      {"name":"codex","status":"available"}
                                    ]
                                    """);
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("client", "status");
        var output = out.toString();
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(output.contains("Client Bundles:")),
                            () -> assertTrue(output.contains("claude")),
                            () -> assertTrue(output.contains("installed")),
                            () -> assertTrue(output.contains("codex"))
        );
    }

    @Test
    @DisplayName("client install installs client bundle")
    void clientInstall() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        fixture.start((request, response) -> {
            if ("/api/v1/client/install".equals(request.target()) && "POST".equals(request.method())) {
                response.json(201, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("client", "install", "codex");
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(out.toString().contains("✓ Client bundle installation completed"))
        );
    }

    @Test
    @DisplayName("client update updates client bundle")
    void clientUpdate() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        fixture.start((request, response) -> {
            if ("/api/v1/client/update".equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("client", "update", "codex");
        assertEquals(0, code);
        assertTrue(out.toString().contains("✓ Client bundle update completed"));
    }

    @Test
    @DisplayName("client remove removes client bundle")
    void clientRemove() throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        fixture.start((request, response) -> {
            if ("/api/v1/client/remove".equals(request.target()) && "POST".equals(request.method())) {
                response.send(204, null, "");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("client", "remove", "codex");
        assertEquals(0, code);
        assertTrue(out.toString().contains("✓ Client bundle removal completed"));
    }

    @ParameterizedTest(name = "a German client {0} prints its success line in German")
    @CsvSource(delimiter = '|', value = {
                        "install|201|✓ Die Client-Bundles wurden installiert|✓ Client bundle installation completed",
                        "update|200|✓ Die Client-Bundles wurden aktualisiert|✓ Client bundle update completed",
                        "remove|204|✓ Die Client-Bundles wurden entfernt|✓ Client bundle removal completed"})
    @DisplayName("the success line of client install, update and remove comes from the catalogue in the operator's language")
    void germanClientSuccessLine(String verb, int status, String german, String english) throws Exception {
        PmOperatorTest.writeLiveRecord(fixture);
        fixture.start((request, response) -> {
            if (("/api/v1/client/" + verb).equals(request.target()) && "POST".equals(request.method())) {
                response.send(status, null, "");
            } else {
                response.send(404, null, "");
            }
        });
        var environment = new HashMap<>(fixture.environment());
        environment.put("LANG", "de_DE.UTF-8");
        var context = new OperatorContext(environment, tempDir, true);
        var harness = Generators.letterStrings(4, 12).next().toLowerCase(Locale.ROOT);

        int code = PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err), "client", verb, harness);

        assertAll(
                            () -> assertEquals(0, code, err.toString()),
                            () -> assertTrue(out.toString().contains(german), out.toString()),
                            () -> assertFalse(out.toString().contains(english), out.toString()),
                            () -> assertEquals("", err.toString()));
    }

    @Test
    @DisplayName("client status reports not running when runtime unavailable")
    void clientStatusWhenNotRunning() {
        int code = execute("client", "status");
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(out.toString().contains("runtime   not running"))
        );
    }

    @Test
    @DisplayName("drain reports that there is nothing to drain when no runtime is recorded")
    void drainWhenNotRunning() {
        int code = execute("drain");
        assertAll(
                            () -> assertEquals(0, code),
                            () -> assertTrue(out.toString()
                                                .contains("The runtime is not running; there is nothing to drain."))
        );
    }
}
