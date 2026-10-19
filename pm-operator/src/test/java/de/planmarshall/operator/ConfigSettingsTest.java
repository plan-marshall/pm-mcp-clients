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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import org.jline.terminal.impl.DumbTerminal;

import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The machine settings of the terminal (UC-ONB-10): the settings screen, the withdrawal of a
 * remembered consent, and the documents mode with the concurrent-change conflict.
 */
@DisplayName("config settings, consents, and documents")
class ConfigSettingsTest {

    private static final String SETTINGS = """
                        {"revision":4,
                         "aspects":[{"aspect":"language","step":1,"summary":"Deutsch, models English"},
                                    {"aspect":"harness","step":2,"summary":"Claude Code"}],
                         "language_interface":"de","language_model":"en","documents_mode":"ask",
                         "cleanup_durations":{"plans_after_days":30,"epics_after_days":30,"logs_after_days":30,"reports_after_days":30,"unenrolled_after_days":30,"remind_after_days":3},
                         "plan_defaults":{"landing":"landing.pull-request","method":"method.test-first","asking":"ask.key-points"},
                         "consents":[{"kind":"scan_harnesses","status":"remembered","remember":true,"channel":"terminal"}],
                         "credentials":[{"key":"github","provider":"GitHub","kind":"fine-grained","expires_in_days":9,"expired":false}]}
                        """;

    @TempDir
    Path tempDir;

    private RuntimeFixture fixture;
    private StringWriter out;
    private StringWriter err;

    @BeforeEach
    void setUp() throws Exception {
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

    private OperatorContext context(String typed) throws IOException {
        var input = new ByteArrayInputStream(typed.getBytes(StandardCharsets.UTF_8));
        var rendered = new ByteArrayOutputStream();
        return new OperatorContext(fixture.environment(), tempDir, true,
                            ClientEnvironment.capture(fixture.environment()),
                            () -> new DumbTerminal(input, rendered), null);
    }

    private int run(OperatorContext context, String... args) {
        return PmOperator.execute(context, new PrintWriter(out, true), new PrintWriter(err, true), args);
    }

    private void settings(int updateStatus) throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/machine/settings".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, SETTINGS);
            } else if ("/api/v1/machine/settings".equals(request.target()) && "PUT".equals(request.method())) {
                if (updateStatus == 409) {
                    response.json(409, "{\"applied\":false,\"conflict\":true,\"revision\":5,\"current_value\":\"copy\"}");
                } else {
                    response.json(200, "{\"applied\":true,\"conflict\":false,\"revision\":5}");
                }
            } else {
                response.send(404, null, "");
            }
        });
    }

    @Test
    @DisplayName("the settings screen lists aspects, a token, the consents, and the documents mode")
    void settingsScreen() throws Exception {
        settings(200);
        var code = run(context("q\n"), "config");
        var text = out.toString();
        assertEquals(0, code);
        assertTrue(text.contains("How this machine is set up"), text);
        assertTrue(text.contains("Language"), text);
        assertTrue(text.contains("github"), text);
        assertTrue(text.contains("expires in 9 days"), text);
        assertTrue(text.contains("scan_harnesses"), text);
        assertTrue(text.contains("Documents"), text);
    }

    @Test
    @DisplayName("a step of the settings opens its aspect")
    void opensAspect() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/machine/settings".equals(request.target())) {
                response.json(200, SETTINGS);
            } else if ("/api/v1/config/language".equals(request.target())) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });
        var code = run(context("1\nq\n"), "config");
        assertEquals(0, code);
        assertTrue(out.toString().contains("Language settings updated"), out.toString());
    }

    @Test
    @DisplayName("the consents are listed and one is withdrawn on confirmation")
    void withdrawsConsent() throws Exception {
        settings(200);
        var code = run(context("\n"), "config", "consents", "--withdraw", "scan_harnesses");
        var text = out.toString();
        assertEquals(0, code);
        assertTrue(text.contains("The consents you said not to ask again"), text);
        assertTrue(text.contains("Withdraw \"look for harnesses\"?"), text);
        assertTrue(text.contains("1 consent withdrawn"), text);
    }

    @Test
    @DisplayName("a kept consent applies nothing")
    void keepsConsent() throws Exception {
        settings(200);
        var code = run(context("k\n"), "config", "consents", "--withdraw", "scan_harnesses");
        assertEquals(0, code);
        assertFalse(out.toString().contains("consent withdrawn"), out.toString());
    }

    @Test
    @DisplayName("an unknown consent kind names that none is remembered")
    void unknownConsent() throws Exception {
        settings(200);
        var code = run(context(""), "config", "consents", "--withdraw", "unknown_kind");
        assertEquals(PmOperator.EXIT_FAILURE, code);
        assertTrue(out.toString().contains("No consent is remembered"), out.toString());
    }

    @Test
    @DisplayName("documents mode is chosen and recorded")
    void documentsMode() throws Exception {
        settings(200);
        var code = run(context("\n"), "config", "documents", "--mode", "copy");
        var text = out.toString();
        assertEquals(0, code);
        assertTrue(text.contains("How should documents be taken into a plan?"), text);
        assertTrue(text.contains("Documents mode updated: copy"), text);
    }

    @Test
    @DisplayName("a change made meanwhile is applied again on request")
    void documentsConflictReapplied() throws Exception {
        var updates = new AtomicInteger();
        fixture.start((request, response) -> {
            if ("GET".equals(request.method())) {
                response.json(200, SETTINGS);
            } else if (updates.getAndIncrement() == 0) {
                response.json(409, "{\"applied\":false,\"conflict\":true,\"revision\":5,\"current_value\":\"copy\"}");
            } else {
                response.json(200, "{\"applied\":true,\"conflict\":false,\"revision\":6}");
            }
        });
        var code = run(context(""), "config", "documents", "--mode", "copy", "--yes");
        var text = out.toString();
        assertEquals(0, code);
        assertTrue(text.contains("Changed meanwhile: it is now copy"), text);
        assertTrue(text.contains("Documents mode updated: copy"), text);
    }

    @Test
    @DisplayName("a change made meanwhile is kept when the operator keeps the current value")
    void documentsConflictKept() throws Exception {
        fixture.start((request, response) -> {
            if ("GET".equals(request.method())) {
                response.json(200, SETTINGS);
            } else {
                response.json(409, "{\"applied\":false,\"conflict\":true,\"revision\":5,\"current_value\":\"copy\"}");
            }
        });
        var code = run(context("k\n"), "config", "documents", "--mode", "move");
        var text = out.toString();
        assertEquals(0, code);
        assertTrue(text.contains("Changed meanwhile: it is now copy"), text);
        assertTrue(text.contains("No setting changed"), text);
    }

    @Test
    @DisplayName("Enter on the documents prompt keeps the current mode and records nothing")
    void documentsUnchanged() throws Exception {
        settings(200);
        var code = run(context("\n"), "config", "documents");
        assertEquals(0, code);
        assertTrue(out.toString().contains("Nothing was changed"), out.toString());
    }

    @Test
    @DisplayName("a settings read that fails is reported")
    void settingsFailure() throws Exception {
        fixture.start((request, response) -> response.send(500, null, ""));
        var code = run(context(""), "config");
        assertEquals(PmOperator.EXIT_FAILURE, code);
        assertTrue(out.toString().contains("Unable to load settings: HTTP 500"), out.toString());
    }
}
