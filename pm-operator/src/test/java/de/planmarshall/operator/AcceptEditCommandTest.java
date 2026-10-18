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
import org.jline.terminal.impl.DumbTerminal;

import de.planmarshall.api.resource.AcceptEditResult;
import de.planmarshall.api.resource.StoreDigestComparison;
import de.planmarshall.api.testsupport.FakeRuntimeServer;
import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("AcceptEditCommand")
class AcceptEditCommandTest {

    private static final String RECORDED_SHA =
                        "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    private static final String CURRENT_SHA =
                        "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";
    private static final String STORE = ".plan/marshal.json";
    private static final String PROJECT = "pm-mcp-clients";

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

    private int execute(String... args) {
        var context = new OperatorContext(fixture.environment(), tempDir, true);
        return PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err), args);
    }

    private int executeInteractive(String input, String... args) {
        var inStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        var outStream = new ByteArrayOutputStream();
        OperatorContext.TerminalProvider termProvider = () -> new DumbTerminal(inStream, outStream);
        var context = new OperatorContext(fixture.environment(), tempDir, true,
                            ClientEnvironment.capture(fixture.environment()), termProvider, null);
        return PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err), args);
    }

    private FakeRuntimeServer runtime(FakeRuntimeServer.Handler handler) throws IOException {
        return fixture.start(handler);
    }

    private static String mismatching() {
        return new StoreDigestComparison(STORE, 1248L, RECORDED_SHA, 1512L, CURRENT_SHA, true,
                            "size and digest changed").toJson();
    }

    private static String matching() {
        return new StoreDigestComparison(STORE, 1248L, RECORDED_SHA, 1248L, RECORDED_SHA, false, null).toJson();
    }

    private static String accepted() {
        return new AcceptEditResult(AcceptEditResult.STATUS_ACCEPTED, CURRENT_SHA).toJson();
    }

    private static FakeRuntimeServer.Handler digestAndAccept(String digestBody, int acceptStatus,
                        String acceptBody) {
        return (request, response) -> {
            if ("GET".equals(request.method()) && request.target().contains("/stores/digest")) {
                if (digestBody == null) {
                    response.json(404, "{\"error\":\"store_not_found\"}");
                } else {
                    response.json(200, digestBody);
                }
            } else if ("POST".equals(request.method()) && request.target().endsWith("/stores/accept-edit")) {
                response.json(acceptStatus, acceptBody);
            } else {
                response.send(404, null, "");
            }
        };
    }

    @Test
    @DisplayName("accept-edit renders the side-by-side comparison and re-baselines on --force")
    void acceptsEditWithForce() throws Exception {
        var server = runtime(digestAndAccept(mismatching(), 200, accepted()));

        assertEquals(0, execute("accept-edit", STORE, "--project", PROJECT, "--force"));
        var output = out.toString();
        assertAll(
                            () -> assertTrue(output.contains("Store: " + STORE), output),
                            () -> assertTrue(output.contains("Recorded"), output),
                            () -> assertTrue(output.contains("Current"), output),
                            () -> assertTrue(output.contains("1,248 bytes"), output),
                            () -> assertTrue(output.contains("1,512 bytes"), output),
                            () -> assertTrue(output.contains(RECORDED_SHA), output),
                            () -> assertTrue(output.contains(CURRENT_SHA), output),
                            () -> assertTrue(output.contains("Edit accepted"), output)
        );

        var digestRequest = server.awaitRequest(Duration.ofSeconds(1));
        assertAll(
                            () -> assertEquals("GET", digestRequest.method()),
                            () -> assertEquals("/api/v1/projects/" + PROJECT + "/stores/digest?path=.plan%2Fmarshal.json",
                                                digestRequest.target())
        );
        var acceptRequest = server.awaitRequest(Duration.ofSeconds(1));
        assertAll(
                            () -> assertEquals("POST", acceptRequest.method()),
                            () -> assertEquals("/api/v1/projects/" + PROJECT + "/stores/accept-edit",
                                                acceptRequest.target()),
                            () -> assertTrue(acceptRequest.bodyText().contains(CURRENT_SHA), acceptRequest.bodyText()),
                            () -> assertTrue(acceptRequest.bodyText().contains(STORE), acceptRequest.bodyText())
        );
    }

    @Test
    @DisplayName("a declined confirmation sends no acceptance (PM-SEC-3)")
    void declinedSendsNothing() throws Exception {
        var server = runtime(digestAndAccept(mismatching(), 200, accepted()));

        assertEquals(1, executeInteractive("n\n", "accept-edit", STORE, "--project", PROJECT));
        assertTrue(out.toString().contains("nothing was sent"), out.toString());
        assertAll(
                            () -> assertEquals(1, server.requests().size(), "only the digest read may be sent"),
                            () -> assertTrue(server.requests().getFirst().target().contains("/stores/digest"))
        );
    }

    @Test
    @DisplayName("a confirmed interactive answer sends the acceptance")
    void confirmedSendsAcceptance() throws Exception {
        var server = runtime(digestAndAccept(mismatching(), 200, accepted()));

        assertEquals(0, executeInteractive("y\n", "accept-edit", STORE, "--project", PROJECT));
        assertTrue(out.toString().contains("Edit accepted"), out.toString());
        assertEquals(2, server.requests().size());
        assertTrue(server.requests().getLast().target().endsWith("/stores/accept-edit"));
    }

    @Test
    @DisplayName("--no-input refuses without sending and names --force")
    void noInputRefusesWithoutSending() throws Exception {
        var server = runtime(digestAndAccept(mismatching(), 200, accepted()));

        assertEquals(3, execute("accept-edit", STORE, "--project", PROJECT, "--no-input"));
        assertTrue(out.toString().contains("Confirmation required (--force)"), out.toString());
        assertEquals(1, server.requests().size());
    }

    @Test
    @DisplayName("a store matching its digest reports no edit and sends nothing")
    void matchingStoreSendsNothing() throws Exception {
        var server = runtime(digestAndAccept(matching(), 200, accepted()));

        assertEquals(0, execute("accept-edit", STORE, "--project", PROJECT, "--force"));
        assertTrue(out.toString().contains("No manual edit detected"), out.toString());
        assertEquals(1, server.requests().size());
    }

    @Test
    @DisplayName("a missing store is reported")
    void missingStore() throws Exception {
        runtime(digestAndAccept(null, 200, accepted()));

        assertEquals(1, execute("accept-edit", STORE, "--project", PROJECT, "--force"));
        assertTrue(err.toString().contains("Store not found: " + STORE), err.toString());
    }

    @Test
    @DisplayName("a stale confirmation is refused with an explanation")
    void staleConfirmation() throws Exception {
        runtime(digestAndAccept(mismatching(), 409, "{\"error\":\"confirmation_mismatch\"}"));

        assertEquals(1, execute("accept-edit", STORE, "--project", PROJECT, "--force"));
        assertTrue(err.toString().contains("changed since it was shown"), err.toString());
    }

    @Test
    @DisplayName("the project id is derived from the store path when --project is omitted")
    void derivesProjectFromPath() throws Exception {
        var server = runtime(digestAndAccept(mismatching(), 200, accepted()));

        assertEquals(0, execute("accept-edit", PROJECT + "/" + STORE, "--force"));
        var digestRequest = server.awaitRequest(Duration.ofSeconds(1));
        assertEquals("/api/v1/projects/" + PROJECT + "/stores/digest?path=.plan%2Fmarshal.json",
                            digestRequest.target());
    }

    @Test
    @DisplayName("a missing project id and no leading segment refuses with an explanation")
    void missingProjectId() {
        assertEquals(3, execute("accept-edit", "marshal.json", "--force"));
        assertTrue(err.toString().contains("--project is required"), err.toString());
    }

    @Test
    @DisplayName("the server-reported store path is sanitized before display")
    void sanitizesServerReportedPath() throws Exception {
        var unsafe = new StoreDigestComparison("\u001B[31m/evil\u001B[0m", 1L, RECORDED_SHA, 2L, CURRENT_SHA, true,
                            null).toJson();
        runtime((request, response) -> {
            if ("GET".equals(request.method())) {
                response.json(200, unsafe);
            } else {
                response.json(200, accepted());
            }
        });

        assertEquals(0, execute("accept-edit", STORE, "--project", PROJECT, "--force"));
        assertFalse(out.toString().contains("\u001B[31m"), out.toString());
    }
}
