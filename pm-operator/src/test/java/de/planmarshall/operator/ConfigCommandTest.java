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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.planmarshall.api.testsupport.RuntimeFixture;

@DisplayName("ConfigCommand")
class ConfigCommandTest {

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

    @Test
    @DisplayName("bare config prints machine settings")
    void bareConfig() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/config".equals(request.target())) {
                response.json(200, "{\"language.interface\":\"en\",\"web.port\":7420}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("config");
        assertAll(
                () -> assertEquals(0, code),
                () -> assertTrue(out.toString().contains("Machine Settings:")),
                () -> assertTrue(out.toString().contains("language.interface"))
        );
    }

    @Test
    @DisplayName("config show displays effective configuration")
    void configShow() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/config/web.port".equals(request.target())) {
                response.json(200, "{\"key\":\"web.port\",\"value\":7420,\"origin\":\"machine\"}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("config", "show", "web.port");
        assertAll(
                () -> assertEquals(0, code),
                () -> assertTrue(out.toString().contains("\"web.port\""))
        );
    }

    @Test
    @DisplayName("config set sets plain configuration key")
    void configSetPlainKey() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/config".equals(request.target()) && "PUT".equals(request.method())) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("config", "set", "custom.timeout", "60");
        assertAll(
                () -> assertEquals(0, code),
                () -> assertTrue(out.toString().contains("✓ Config custom.timeout set to 60"))
        );
    }

    @Test
    @DisplayName("config set refuses aspect-owned keys with 409 aspect_owned")
    void configSetRefusesAspectOwned() {
        int code = execute("config", "set", "language.interface", "de");
        assertAll(
                () -> assertEquals(1, code),
                () -> assertTrue(err.toString().contains("409 aspect_owned")),
                () -> assertTrue(err.toString().contains("pm-operator config language"))
        );

        err.getBuffer().setLength(0);
        int code2 = execute("config", "set", "plan_defaults.method", "test-first");
        assertAll(
                () -> assertEquals(1, code2),
                () -> assertTrue(err.toString().contains("409 aspect_owned")),
                () -> assertTrue(err.toString().contains("pm-operator config plan-defaults"))
        );
    }

    @Test
    @DisplayName("config unset unsets configuration key")
    void configUnset() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/config/custom.flag".equals(request.target()) && "DELETE".equals(request.method())) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("config", "unset", "custom.flag");
        assertAll(
                () -> assertEquals(0, code),
                () -> assertTrue(out.toString().contains("✓ Config custom.flag unset"))
        );
    }

    @Test
    @DisplayName("config harnesses stops with code 3 on --no-input without consent")
    void configHarnessesNoInputNeedsConsent() {
        int code = execute("config", "harnesses", "--add", "codex", "--no-input");
        assertAll(
                () -> assertEquals(3, code),
                () -> assertTrue(out.toString().contains("Needs a choice: --consent scan_harnesses"))
        );
    }

    @Test
    @DisplayName("config harnesses updates harnesses successfully")
    void configHarnessesSuccess() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/config/harnesses".equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("config", "harnesses", "--add", "codex", "--remove", "cursor",
                "--default", "codex", "--no-icons", "--consent", "scan_harnesses");
        assertEquals(0, code);
        assertTrue(out.toString().contains("✓ Harnesses updated"));
    }

    @Test
    @DisplayName("config language updates language settings")
    void configLanguage() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/config/language".equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("config", "language", "--interface", "de", "--model", "same");
        assertEquals(0, code);
        assertTrue(out.toString().contains("✓ Language settings updated"));
    }

    @Test
    @DisplayName("config models updates allowed models")
    void configModels() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/config/models".equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("config", "models", "--allow", "anthropic:claude-3-5-sonnet", "--no-advice");
        assertEquals(0, code);
        assertTrue(out.toString().contains("✓ Models updated"));
    }

    @Test
    @DisplayName("config roles updates role assignments")
    void configRoles() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/config/roles".equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("config", "roles", "--defaults", "--set", "architect=anthropic:claude-3-5-sonnet:high",
                "--reset", "auditor", "--reset-all");
        assertEquals(0, code);
        assertTrue(out.toString().contains("✓ Roles updated"));
    }

    @Test
    @DisplayName("config credentials updates credential store")
    void configCredentials() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/config/credentials".equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("config", "credentials", "--store", "os");
        assertEquals(0, code);
        assertTrue(out.toString().contains("✓ Credential store updated: os"));
    }

    @Test
    @DisplayName("config providers updates provider settings")
    void configProviders() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/config/providers".equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("config", "providers", "--use", "github", "--without", "gitlab", "--verify");
        assertEquals(0, code);
        assertTrue(out.toString().contains("✓ Providers updated"));
    }

    @Test
    @DisplayName("config consents shows remembered consents")
    void configConsents() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/config/consents".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, "[\"scan_harnesses\",\"external_web\"]");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("config", "consents");
        assertEquals(0, code);
        assertTrue(out.toString().contains("Remembered consents:"));
        assertTrue(out.toString().contains("scan_harnesses"));
    }

    @Test
    @DisplayName("config consents shows empty message when none remembered")
    void configConsentsEmpty() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/config/consents".equals(request.target())) {
                response.json(200, "[]");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("config", "consents");
        assertEquals(0, code);
        assertTrue(out.toString().contains("No consent is remembered."));
    }

    @Test
    @DisplayName("config documents updates documents mode")
    void configDocuments() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/config/documents".equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("config", "documents", "--mode", "copy");
        assertEquals(0, code);
        assertTrue(out.toString().contains("✓ Documents mode updated: copy"));
    }

    @Test
    @DisplayName("config plan-defaults applies defaults with --yes")
    void configPlanDefaults() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/config/plan-defaults".equals(request.target())) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });

        int code = execute("config", "plan-defaults", "--landing", "pull-request", "--method", "test-first",
                "--asking", "key-points", "--posture", "standard", "--yes");
        assertAll(
                () -> assertEquals(0, code),
                () -> assertTrue(out.toString().contains("Plans · How should plans run on this machine?")),
                () -> assertTrue(out.toString().contains("✓ Landing   pull-request")),
                () -> assertTrue(out.toString().contains("✓ Method    test-first")),
                () -> assertTrue(out.toString().contains("✓ Asking    key-points")),
                () -> assertTrue(out.toString().contains("✓ Plan defaults recorded"))
        );
    }
}
