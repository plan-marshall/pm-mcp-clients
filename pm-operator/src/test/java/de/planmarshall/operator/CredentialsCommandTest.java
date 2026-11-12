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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
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
    private final List<String> requests = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        fixture = RuntimeFixture.create();
        PmOperatorTest.writeLiveRecord(fixture);
        out = new StringWriter();
        err = new StringWriter();
        requests.clear();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (fixture != null) {
            fixture.close();
        }
    }

    private OperatorContext context(String typed) {
        var first = new AtomicBoolean(true);
        var rendered = new ByteArrayOutputStream();
        return new OperatorContext(fixture.environment(), tempDir, true,
                            ClientEnvironment.capture(fixture.environment()),
                            () -> new DumbTerminal(new ByteArrayInputStream(
                                                                    (first.getAndSet(false) ? typed : "").getBytes(StandardCharsets.UTF_8)),
                                                rendered),
                            null);
    }

    private int run(OperatorContext context, String... args) {
        return PmOperator.execute(context, new PrintWriter(out, true), new PrintWriter(err, true), args);
    }

    private void present() throws Exception {
        stub("github", 2, false);
    }

    /**
     * Serves the credential entry, its store, and the machine settings' later run of the
     * credentials aspect with the provider check it leads to, as the mock fixture models it.
     *
     * @param failed whether the check reports a failure instead of the plans it released
     */
    private void verification(boolean failed) throws Exception {
        stub("os", 1, failed);
    }

    private void stub(String tokenPath, int dropped, boolean failed) throws Exception {
        var current = new AtomicReference<>("settings.choose");
        fixture.start((request, response) -> {
            requests.add(request.method() + " " + request.target());
            if ("/api/v1/credentials/github".equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, "{\"status\":\"present\",\"token_path\":\"" + tokenPath + "\"}");
            } else if ("/api/v1/credentials/github".equals(request.target()) && "PUT".equals(request.method())) {
                response.json(200, "{\"derived_tokens_dropped\":" + dropped + "}");
            } else if ("/api/v1/credentials/github".equals(request.target()) && "DELETE".equals(request.method())) {
                response.send(204, null, "");
            } else if ("/api/v1/onboarding/state".equals(request.target())) {
                response.json(200, state(current.get(), failed));
            } else if ("/api/v1/onboarding/step".equals(request.target())) {
                current.set(nextOf(current.get()));
                response.json(200, state(current.get(), failed));
            } else {
                response.send(404, null, "");
            }
        });
    }

    private static String nextOf(String state) {
        return switch (state) {
            case "settings.choose" -> "store.reconfigure";
            case "store.reconfigure" -> "settings.store-credential";
            case "settings.store-credential" -> "settings.verify-providers";
            case "settings.verify-providers" -> "providers.verified";
            default -> "settings.choose";
        };
    }

    private static String state(String stateId, boolean failed) {
        return switch (stateId) {
            case "store.reconfigure" -> "{\"state\":{\"state_id\":\"store.reconfigure\","
                                + "\"kind\":\"operator_wait\"},"
                                + "\"facts\":{\"summary\":\"Credentials \u00b7 A new token for github\","
                                + "\"current\":\"os\"},"
                                + "\"links\":[{\"id\":\"set\",\"preselected\":true}]}";
            case "settings.store-credential" -> "{\"state\":{\"state_id\":\"settings.store-credential\","
                                + "\"kind\":\"operator_wait\"},"
                                + "\"facts\":{\"summary\":\"The token is stored\",\"current\":\"os\"},"
                                + "\"links\":[{\"id\":\"stored\",\"preselected\":true}]}";
            case "settings.verify-providers" -> "{\"state\":{\"state_id\":\"settings.verify-providers\","
                                + "\"kind\":\"operator_wait\"},"
                                + "\"facts\":{\"summary\":\"Providers \u00b7 Checking the providers that use github\","
                                + "\"kind\":\"verify_providers\",\"current\":\"os\","
                                + "\"providers\":[{\"provider\":\"github\",\"ready\":true}]},"
                                + "\"links\":[{\"id\":\"yes\",\"preselected\":true}]}";
            case "providers.verified" -> failed
                                ? "{\"state\":{\"state_id\":\"providers.verified\",\"kind\":\"operator_wait\"},"
                                + "\"facts\":{\"summary\":\"The provider check is done\",\"current\":\"os\","
                                + "\"failed\":true,\"cause\":\"401 Invalid token\"},"
                                + "\"links\":[{\"id\":\"done\",\"preselected\":true}]}"
                                : "{\"state\":{\"state_id\":\"providers.verified\",\"kind\":\"operator_wait\"},"
                                + "\"facts\":{\"summary\":\"The provider check is done\",\"current\":\"os\","
                                + "\"plans\":\"fix-relay\"},"
                                + "\"links\":[{\"id\":\"done\",\"preselected\":true}]}";
            default -> "{\"state\":{\"state_id\":\"settings.choose\",\"kind\":\"operator_wait\"},"
                                + "\"facts\":{\"summary\":\"Machine settings\"},"
                                + "\"links\":[{\"id\":\"reconfigure:credentials\",\"rel\":\"alt\","
                                + "\"preselected\":true}]}";
        };
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
        assertTrue(requests.stream().noneMatch(request -> request.contains("/api/v1/config/")),
                            requests.toString());
    }

    @Test
    @DisplayName("rotate checks the providers that use the key and names the plans that go on")
    void rotateChecksTheProviders() throws Exception {
        verification(false);

        var code = run(context("s3cret\n"), "credentials", "rotate", "github", "--yes");
        var text = out.toString();

        assertEquals(0, code, text + " | " + err);
        assertTrue(text.contains("Consent remembered: check the providers with their tokens"), text);
        assertTrue(text.contains("\u2713 github verified. 1 plan that waited for it goes on: fix-relay"), text);
        assertTrue(requests.contains("POST /api/v1/onboarding/step"), requests.toString());
    }

    @Test
    @DisplayName("a failed provider check prints the failure and keeps the hold")
    void rotateReportsAFailedCheck() throws Exception {
        verification(true);

        var code = run(context("s3cret\n"), "credentials", "rotate", "github", "--yes");
        var text = out.toString();

        assertEquals(0, code, text + " | " + err);
        assertTrue(text.contains("The check failed: 401 Invalid token. The hold stays."), text);
        assertTrue(text.contains("Token github stored"), text);
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
