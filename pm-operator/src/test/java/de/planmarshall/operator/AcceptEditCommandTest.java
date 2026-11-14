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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.DumbTerminal;

import de.planmarshall.api.resource.AcceptEditResult;
import de.planmarshall.api.resource.StoreDigestComparison;
import de.planmarshall.api.testsupport.FakeRuntimeServer;
import de.planmarshall.api.testsupport.RuntimeFixture;
import de.planmarshall.operator.tui.VirtualScreenTerminal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/**
 * {@code pm-operator accept-edit <scope> ...} in the forms of the specification
 * (02-operator-channel.adoc#operator-accept-edit): each form reads its digest, shows it, and sends the
 * acceptance only after a typed confirmation.
 */
@DisplayName("AcceptEditCommand")
@EnabledOnOs({OS.MAC, OS.LINUX})
@EnableGeneratorController
class AcceptEditCommandTest {

    private static final String RECORDED_SHA =
                        "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    private static final String CURRENT_SHA =
                        "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";
    private static final Duration TIMEOUT = Duration.ofSeconds(20);
    private static final Duration POLL = Duration.ofMillis(20);

    @TempDir
    Path tempDir;

    private RuntimeFixture fixture;
    private StringWriter out;
    private StringWriter err;
    private String project;

    @BeforeEach
    void setUp() throws Exception {
        fixture = RuntimeFixture.create();
        PmOperatorTest.writeLiveRecord(fixture);
        out = new StringWriter();
        err = new StringWriter();
        project = Generators.letterStrings(4, 12).next().toLowerCase(Locale.ROOT);
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

    private static String mismatching(String store) {
        return new StoreDigestComparison(store, 1248L, RECORDED_SHA, 1512L, CURRENT_SHA,
                            "size and digest changed").toJson();
    }

    private static String accepted() {
        return new AcceptEditResult(AcceptEditResult.STATUS_ACCEPTED, CURRENT_SHA).toJson();
    }

    /**
     * @param digestPath the path of the digest request the form must send
     * @param acceptPath the path of the acceptance the form must send
     * @param digestBody the digest answer, {@code null} for a store the runtime does not know
     */
    private static FakeRuntimeServer.Handler digestAndAccept(String digestPath, String acceptPath,
                        String digestBody, int acceptStatus, String acceptBody) {
        return (request, response) -> {
            if ("GET".equals(request.method()) && request.target().equals(digestPath)) {
                if (digestBody == null) {
                    response.json(404, "{\"error\":\"store_not_found\"}");
                } else {
                    response.json(200, digestBody);
                }
            } else if ("POST".equals(request.method()) && request.target().equals(acceptPath)) {
                response.json(acceptStatus, acceptBody);
            } else {
                response.send(404, null, "");
            }
        };
    }

    private String enrolmentDigest() {
        return "/api/v1/projects/" + project + "/enrolment/digest";
    }

    private String enrolmentAccept() {
        return "/api/v1/projects/" + project + "/enrolment/accept-edit";
    }

    private String enrolmentFile() {
        return "enrolments/" + project + ".json";
    }

    private FakeRuntimeServer enrolmentRuntime(int acceptStatus, String acceptBody) throws IOException {
        return fixture.start(digestAndAccept(enrolmentDigest(), enrolmentAccept(), mismatching(enrolmentFile()),
                            acceptStatus, acceptBody));
    }

    private static long posts(FakeRuntimeServer server) {
        return server.requests().stream().filter(request -> "POST".equals(request.method())).count();
    }

    @Nested
    @DisplayName("accept-edit enrolment <project_id>")
    class Enrolment {

        @Test
        @DisplayName("sends the acceptance only after the confirmation")
        void acceptsAfterTheConfirmation() throws Exception {
            var server = enrolmentRuntime(200, accepted());

            assertEquals(0, executeInteractive("y\n", "accept-edit", "enrolment", project), err.toString());

            var digest = server.awaitRequest(Duration.ofSeconds(1));
            var accept = server.awaitRequest(Duration.ofSeconds(1));
            assertAll(
                                () -> assertEquals("GET", digest.method()),
                                () -> assertEquals(enrolmentDigest(), digest.target()),
                                () -> assertEquals("POST", accept.method()),
                                () -> assertEquals(enrolmentAccept(), accept.target()),
                                () -> assertTrue(accept.bodyText().contains(CURRENT_SHA), accept.bodyText()),
                                () -> assertTrue(accept.bodyText().contains(enrolmentFile()), accept.bodyText()),
                                () -> assertTrue(out.toString().contains("Store: " + enrolmentFile()), out.toString()),
                                () -> assertTrue(out.toString().contains("1,248 bytes"), out.toString()),
                                () -> assertTrue(out.toString().contains("1,512 bytes"), out.toString()),
                                () -> assertTrue(out.toString().contains(RECORDED_SHA), out.toString()),
                                () -> assertTrue(out.toString().contains("Edit accepted"), out.toString()));
        }

        @Test
        @DisplayName("a declined confirmation sends no acceptance")
        void declinedSendsNothing() throws Exception {
            var server = enrolmentRuntime(200, accepted());

            assertEquals(1, executeInteractive("n\n", "accept-edit", "enrolment", project));

            assertAll(
                                () -> assertTrue(out.toString().contains("nothing was sent"), out.toString()),
                                () -> assertEquals(0, posts(server), "requests: " + server.requests()));
        }

        @Test
        @DisplayName("an end of input never confirms and sends no acceptance")
        void endOfInputSendsNothing() throws Exception {
            var server = enrolmentRuntime(200, accepted());

            assertEquals(1, executeInteractive("", "accept-edit", "enrolment", project));

            assertAll(
                                () -> assertTrue(out.toString().contains("nothing was sent"), out.toString()),
                                () -> assertEquals(0, posts(server), "requests: " + server.requests()));
        }

        @Test
        @DisplayName("Ctrl+C at the confirmation leaves with 130 and sends no acceptance")
        void interruptLeavesWith130() throws Exception {
            var server = enrolmentRuntime(200, accepted());
            try (var virtual = VirtualScreenTerminal.create(160, 40)) {
                var context = new OperatorContext(fixture.environment(), tempDir, true,
                                    ClientEnvironment.capture(fixture.environment()), virtual::terminal, null);

                var code = interruptedAt(virtual.terminal(), "Store: " + enrolmentFile(),
                                    () -> PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err),
                                                        "accept-edit", "enrolment", project));

                assertAll(
                                    () -> assertEquals(130, code, out.toString()),
                                    () -> assertEquals(0, posts(server), "requests: " + server.requests()));
            }
        }

        @Test
        @DisplayName("--no-input stops at the confirmation with 3 and sends nothing")
        void noInputStops() throws Exception {
            var server = enrolmentRuntime(200, accepted());

            assertEquals(3, execute("accept-edit", "enrolment", project, "--no-input"));

            assertAll(
                                () -> assertTrue(out.toString().contains("needs your confirmation"), out.toString()),
                                () -> assertFalse(out.toString().contains("--force"), out.toString()),
                                () -> assertEquals(0, posts(server), "requests: " + server.requests()));
        }

        @Test
        @DisplayName("a stale confirmation is refused with an explanation")
        void staleConfirmation() throws Exception {
            enrolmentRuntime(409, "{\"error\":\"conflict\"}");

            assertEquals(1, executeInteractive("y\n", "accept-edit", "enrolment", project));
            assertTrue(err.toString().contains("changed since it was shown"), err.toString());
        }

        @Test
        @DisplayName("a matching enrolment file reports no edit and sends nothing")
        void matchingSendsNothing() throws Exception {
            var matching = new StoreDigestComparison(enrolmentFile(), 1248L, RECORDED_SHA, 1248L, RECORDED_SHA,
                                null).toJson();
            var server = fixture.start(digestAndAccept(enrolmentDigest(), enrolmentAccept(), matching, 200, accepted()));

            assertEquals(0, execute("accept-edit", "enrolment", project));
            assertAll(
                                () -> assertTrue(out.toString().contains("No manual edit detected"), out.toString()),
                                () -> assertEquals(0, posts(server)));
        }

        @Test
        @DisplayName("the server-reported path is sanitized before display")
        void sanitizesThePath() throws Exception {
            var unsafe = new StoreDigestComparison("\u001B[31m/evil\u001B[0m", 1L, RECORDED_SHA, 2L, CURRENT_SHA,
                                null).toJson();
            fixture.start(digestAndAccept(enrolmentDigest(), enrolmentAccept(), unsafe, 200, accepted()));

            assertEquals(0, executeInteractive("y\n", "accept-edit", "enrolment", project));
            assertFalse(out.toString().contains("\u001B[31m"), out.toString());
        }
    }

    @Nested
    @DisplayName("the store forms")
    class StoreForms {

        @Test
        @DisplayName("plan <plan_id> <store> reads and accepts the store below the plan's scope directory")
        void planForm() throws Exception {
            var plan = Generators.letterStrings(4, 12).next().toLowerCase(Locale.ROOT);
            var store = "plans/" + plan + "/mcp/status.json";
            var server = fixture.start(digestAndAccept(
                                "/api/v1/projects/" + project + "/stores/digest?path=plans%2F" + plan + "%2Fmcp%2Fstatus.json",
                                "/api/v1/projects/" + project + "/stores/accept-edit", mismatching(store), 200, accepted()));

            assertEquals(0, executeInteractive("y\n", "accept-edit", "plan", plan, "mcp/status.json",
                                "--project", project), err.toString());

            assertEquals(1, posts(server), "requests: " + server.requests());
            assertTrue(server.requests().getLast().bodyText().contains(store), server.requests().getLast().bodyText());
        }

        @Test
        @DisplayName("epic <epic_id> <store> reads the store below the epic's scope directory")
        void epicForm() throws Exception {
            var epic = Generators.letterStrings(4, 12).next().toLowerCase(Locale.ROOT);
            var server = fixture.start(digestAndAccept(
                                "/api/v1/projects/" + project + "/stores/digest?path=epics%2F" + epic + "%2Fstatus.json",
                                "/api/v1/projects/" + project + "/stores/accept-edit",
                                mismatching("epics/" + epic + "/status.json"), 200, accepted()));

            assertEquals(0, executeInteractive("y\n", "accept-edit", "epic", epic, "status.json", "--project", project),
                                err.toString());
            assertEquals(1, posts(server));
        }

        @Test
        @DisplayName("workspace <store> reads the store below the project store root")
        void workspaceForm() throws Exception {
            var server = fixture.start(digestAndAccept(
                                "/api/v1/projects/" + project + "/stores/digest?path=grants.jsonl",
                                "/api/v1/projects/" + project + "/stores/accept-edit", mismatching("grants.jsonl"), 200,
                                accepted()));

            assertEquals(0, executeInteractive("y\n", "accept-edit", "workspace", "grants.jsonl", "--project", project),
                                err.toString());
            assertEquals(1, posts(server));
        }

        @Test
        @DisplayName("lesson <lesson_id> reads the lesson's file of the lesson store")
        void lessonForm() throws Exception {
            var lesson = Generators.letterStrings(4, 12).next().toLowerCase(Locale.ROOT);
            var server = fixture.start(digestAndAccept(
                                "/api/v1/projects/" + project + "/stores/digest?path=lessons%2F" + lesson + ".md",
                                "/api/v1/projects/" + project + "/stores/accept-edit",
                                mismatching("lessons/" + lesson + ".md"), 200, accepted()));

            assertEquals(0, executeInteractive("y\n", "accept-edit", "lesson", lesson, "--project", project),
                                err.toString());
            assertEquals(1, posts(server));
        }

        @Test
        @DisplayName("a missing store is reported")
        void missingStore() throws Exception {
            fixture.start(digestAndAccept("/api/v1/projects/" + project + "/stores/digest?path=grants.jsonl",
                                "/api/v1/projects/" + project + "/stores/accept-edit", null, 200, accepted()));

            assertEquals(1, execute("accept-edit", "workspace", "grants.jsonl", "--project", project));
            assertTrue(err.toString().contains("Store not found: grants.jsonl"), err.toString());
        }
    }

    @Nested
    @DisplayName("usage")
    class Usage {

        @Test
        @DisplayName("the old form <store path> --project <id> is a usage error and sends nothing")
        void oldFormIsAUsageError() throws Exception {
            var server = fixture.start((request, response) -> response.send(200, null, ""));

            assertEquals(2, execute("accept-edit", ".plan/marshal.json", "--project", project));
            assertEquals(0, server.requests().size(), "requests: " + server.requests());
        }

        @Test
        @DisplayName("--force is no option of the forms")
        void forceIsGone() throws Exception {
            var server = fixture.start((request, response) -> response.send(200, null, ""));

            assertEquals(2, execute("accept-edit", "enrolment", project, "--force"));
            assertEquals(0, server.requests().size(), "requests: " + server.requests());
        }

        @Test
        @DisplayName("the verb without its scope form is a usage error")
        void bareVerbIsAUsageError() {
            assertEquals(2, execute("accept-edit"));
            assertTrue(err.toString().contains("enrolment"), err.toString());
        }

        @Test
        @DisplayName("a store form without --project is a usage error")
        void storeFormNeedsTheProject() {
            assertEquals(2, execute("accept-edit", "workspace", "grants.jsonl"));
            assertTrue(err.toString().contains("--project"), err.toString());
        }
    }

    /**
     * Runs the verb and raises the terminal's interrupt as soon as the comparison is printed, so the
     * confirmation's read is interrupted where the operator would press {@code Ctrl+C}; the raise
     * repeats until the verb returns, because the reader installs its handler only once it reads.
     */
    private int interruptedAt(Terminal terminal, String marker, Verb verb) throws Exception {
        var code = new AtomicInteger(-1);
        var failure = new AtomicReference<Throwable>();
        var runner = Thread.ofPlatform().name("accept-edit-under-test").start(() -> {
            try {
                code.set(verb.run());
            } catch (IOException failed) {
                failure.set(failed);
            }
        });
        var deadline = System.nanoTime() + TIMEOUT.toNanos();
        while (runner.isAlive() && System.nanoTime() < deadline) {
            if (out.toString().contains(marker)) {
                terminal.raise(Terminal.Signal.INT);
            }
            LockSupport.parkNanos(POLL.toNanos());
        }
        runner.join(TimeUnit.SECONDS.toMillis(1));
        assertFalse(runner.isAlive(), "the verb did not leave after the interrupt: " + out);
        assertNull(failure.get(), "the verb failed: " + failure.get());
        return code.get();
    }

    @FunctionalInterface
    private interface Verb {
        int run() throws IOException;
    }
}
