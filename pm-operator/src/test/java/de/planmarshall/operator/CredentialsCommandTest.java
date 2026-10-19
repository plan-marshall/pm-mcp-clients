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
import org.jline.terminal.impl.DumbTerminal;

import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The credential verbs (02-operator-channel.adoc#operator-credential-cli): a secret is read with the
 * echo off into a {@code char[]} and sent in one request; nothing of it is printed.
 */
@DisplayName("CredentialsCommand")
class CredentialsCommandTest {

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

    private void present() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/credentials/github".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, "{\"status\":\"present\",\"token_path\":\"github\"}");
            } else if ("/api/v1/credentials/github".equals(request.target()) && "PUT".equals(request.method())) {
                response.json(200, "{\"derived_tokens_dropped\":2}");
            } else if ("/api/v1/credentials/github".equals(request.target()) && "DELETE".equals(request.method())) {
                response.send(204, null, "");
            } else if ("/api/v1/config/providers".equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, "{\"ok\":true}");
            } else {
                response.send(404, null, "");
            }
        });
    }

    @Test
    @DisplayName("rotate reads the secret with the echo off and reports what was dropped")
    void rotate() throws Exception {
        present();
        var code = run(context("s3cret\n"), "credentials", "rotate", "github", "--yes");
        var text = out.toString();
        assertEquals(0, code);
        assertTrue(text.contains("A new token for github"), text);
        assertTrue(text.contains("Token github stored"), text);
        assertTrue(text.contains("2 tokens derived from the old one dropped"), text);
        assertTrue(text.contains("github verified."), text);
    }

    @Test
    @DisplayName("set writes another kind under the same key")
    void set() throws Exception {
        present();
        var code = run(context("s3cret\n"), "credentials", "set", "github", "--kind", "classic", "--yes");
        assertEquals(0, code);
        assertTrue(out.toString().contains("Another kind of token for github"), out.toString());
        assertTrue(out.toString().contains("classic"), out.toString());
    }

    @Test
    @DisplayName("rotate of an unknown key names the set verb and exits non-zero")
    void rotateUnknown() throws Exception {
        fixture.start((request, response) -> response.send(404, null, ""));
        var code = run(context(""), "credentials", "rotate", "github", "--yes");
        assertEquals(PmOperator.EXIT_FAILURE, code);
        assertTrue(out.toString().contains("No entry github exists"), out.toString());
    }

    @Test
    @DisplayName("status reports a present entry")
    void status() throws Exception {
        present();
        var code = run(context(""), "credentials", "status", "github");
        assertEquals(0, code);
        assertTrue(out.toString().contains("entry"), out.toString());
        assertTrue(out.toString().contains("github"), out.toString());
    }

    @Test
    @DisplayName("status of a missing entry exits non-zero")
    void statusMissing() throws Exception {
        fixture.start((request, response) -> response.send(404, null, ""));
        var code = run(context(""), "credentials", "status", "github");
        assertEquals(PmOperator.EXIT_FAILURE, code);
        assertTrue(out.toString().contains("No entry github exists"), out.toString());
    }

    @Test
    @DisplayName("remove retires the entry")
    void remove() throws Exception {
        present();
        var code = run(context(""), "credentials", "remove", "github");
        assertEquals(0, code);
        assertTrue(out.toString().contains("github"), out.toString());
    }

    @Test
    @DisplayName("remove reports a failing runtime")
    void removeFailed() throws Exception {
        fixture.start((request, response) -> response.send(500, null, ""));
        var code = run(context(""), "credentials", "remove", "github");
        assertEquals(PmOperator.EXIT_FAILURE, code);
        assertTrue(out.toString().contains("HTTP 500"), out.toString());
    }

    @Test
    @DisplayName("an empty secret stores nothing and exits non-zero")
    void emptySecret() throws Exception {
        present();
        var code = run(context("\n"), "credentials", "rotate", "github", "--yes");
        assertEquals(PmOperator.EXIT_FAILURE, code);
        assertTrue(out.toString().contains("Failed to read the secret"), out.toString());
    }

    @Test
    @DisplayName("the confirmation's back leaves without a request")
    void back() throws Exception {
        present();
        var code = run(context("s3cret\nb\n"), "credentials", "rotate", "github");
        assertEquals(0, code);
        assertTrue(out.toString().contains("Token (not shown while you type)"), out.toString());
    }

    @Test
    @DisplayName("a failing store reports the status")
    void storeFailed() throws Exception {
        fixture.start((request, response) -> {
            if ("GET".equals(request.method())) {
                response.json(200, "{\"status\":\"present\"}");
            } else {
                response.send(500, null, "");
            }
        });
        var code = run(context("s3cret\n"), "credentials", "rotate", "github", "--yes");
        assertEquals(PmOperator.EXIT_FAILURE, code);
        assertTrue(out.toString().contains("HTTP 500"), out.toString());
    }

    @Test
    @DisplayName("the verb without a subcommand prints its usage")
    void usage() throws Exception {
        var code = run(context(""), "credentials");
        assertEquals(0, code);
        assertTrue(out.toString().contains("credentials") || err.toString().contains("credentials"));
    }
}
