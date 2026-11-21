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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.DumbTerminal;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.control.ReviewResource;
import de.planmarshall.api.host.HostDetection;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.resource.ApiErrorCode;
import de.planmarshall.api.resource.ErrorTexts;
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
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import picocli.CommandLine.Command;

/**
 * Checks the exit codes of every registered verb across success, refusal and usage, and the causes
 * of the exit codes 130, 3 and 4, as specified in
 * {@code doc/design/terminal-client/01-commands.adoc#exit-codes}.
 *
 * <p>The cases live in one registry keyed by verb name. The registry is checked against the verbs
 * {@link PmOperator} registers: a registered verb without a success, a refusal and a usage case
 * fails the suite, and so does a registry entry that names no registered verb.
 *
 * <p>A refusal row asserts exit 1, nothing on stdout and the three lines {@link RefusalRenderer}
 * renders on stderr (title, cause, fix), so a refusal that reaches {@link PmOperator}'s generic
 * one-line handler fails its row. Four refusals print a line of their own instead, and their rows
 * assert that catalogue line exactly, which the generic handler's {@code verb: message} form does not
 * match either: {@code drain} sends no request to the runtime and names the process it cannot
 * identify, the read of {@code credentials status} and the digest read of {@code accept-edit}
 * keep the line that names their key or store, with the refusal's title as its cause, and the
 * refused first read of {@code answer} names the decision it could not read and that nothing was sent.
 */
@EnableGeneratorController
@DisplayName("Exit codes of operator verbs")
@EnabledOnOs({OS.MAC, OS.LINUX})
class ExitCodesTest {

    /** The placeholder of an argument that takes the test's generated id. */
    private static final String ID = "{id}";

    /** The placeholder of an argument that takes the harness client the test prepared. */
    private static final String CLIENT = "{client}";

    /** The placeholder of the unknown option a usage row passes; generated at run time. */
    private static final String UNKNOWN_OPTION_TOKEN = "{unknown-option}";

    /** The refusal every refusal row's runtime answers with. */
    private static final ApiErrorCode REFUSAL = ApiErrorCode.INTERNAL_FAULT;

    private static final String GLYPH_REFUSED = "✗ ";
    private static final String GLYPH_FIX = "→ ";
    private static final Duration INTERRUPT_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration POLL = Duration.ofMillis(20);

    /** The cases of every verb, keyed by verb name. */
    private static final Map<String, VerbCases> REGISTRY = registry();

    private final String id = Generators.letterStrings(4, 10).next();

    @TempDir
    Path tempDir;

    private RuntimeFixture fixture;
    private StringWriter out;
    private StringWriter err;
    private final Map<String, String> extraEnvironment = new HashMap<>();
    private @Nullable String client;
    private byte[] terminalInput = new byte[0];
    private @Nullable FakeRuntimeServer server;

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
        if (fixture != null) {
            fixture.close();
        }
    }

    // ---------------------------------------------------------------------------------------------
    // The registry
    // ---------------------------------------------------------------------------------------------

    /** Prepares the machine and the runtime a case runs against. */
    @FunctionalInterface
    interface Setup {
        /**
         * @param test the running test
         * @throws IOException when the machine cannot be prepared
         */
        void apply(ExitCodesTest test) throws IOException;
    }

    /**
     * One invocation of a verb.
     *
     * @param label    the name of the case
     * @param setup    what the case runs against
     * @param args     the arguments, {@link #ID} and {@link #CLIENT} replaced at run time
     * @param ownLines the lines a refusal that renders no runtime refusal prints on stderr, or
     *                 {@code null} for the three lines of {@link RefusalRenderer}
     */
    record Row(String label, Setup setup, List<String> args,
                        @Nullable Function<ExitCodesTest, List<String>> ownLines) {
    }

    /**
     * The cases of one verb.
     *
     * @param success the invocations that exit 0
     * @param refusal the invocations that exit 1 with a refusal
     * @param usage   the invocations that exit 2
     */
    record VerbCases(List<Row> success, List<Row> refusal, List<Row> usage) {
    }

    /** Collects the cases of one verb. */
    private static final class Cases {
        private final String verb;
        private final List<Row> success = new ArrayList<>();
        private final List<Row> refusal = new ArrayList<>();
        private final List<Row> usage = new ArrayList<>();

        private Cases(String verb) {
            this.verb = verb;
            usage.add(new Row(verb + " --<unknown>", ExitCodesTest::noRuntime,
                                List.of(verb, UNKNOWN_OPTION_TOKEN), null));
        }

        /** Replaces the default usage case, for a verb whose required parameter picocli checks first. */
        Cases usage(String... args) {
            usage.clear();
            usage.add(new Row(String.join(" ", args), ExitCodesTest::noRuntime, List.of(args), null));
            return this;
        }

        Cases success(Setup setup, String... args) {
            success.add(new Row(String.join(" ", args), setup, List.of(args), null));
            return this;
        }

        Cases refusal(String... args) {
            refusal.add(new Row(String.join(" ", args), ExitCodesTest::refuseEverything, List.of(args), null));
            return this;
        }

        Cases refusal(Setup setup, @Nullable Function<ExitCodesTest, List<String>> ownLines, String... args) {
            refusal.add(new Row(String.join(" ", args), setup, List.of(args), ownLines));
            return this;
        }

        VerbCases build() {
            return new VerbCases(List.copyOf(success), List.copyOf(refusal), List.copyOf(usage));
        }

        String verb() {
            return verb;
        }
    }

    private static Map<String, VerbCases> registry() {
        var all = List.of(
                            new Cases("status")
                                                .success(ExitCodesTest::noRuntime, "status")
                                                .refusal("status"),
                            new Cases("plans")
                                                .success(ExitCodesTest::serveReads, "plans", "--json")
                                                .refusal("plans"),
                            new Cases("web")
                                                .success(ExitCodesTest::noRuntime, "web")
                                                .refusal("web", "devices"),
                            new Cases("runtime")
                                                .success(ExitCodesTest::serveReads, "runtime", "upgrade", "--notes")
                                                .refusal("runtime", "upgrade", "--notes"),
                            new Cases("drain")
                                                .success(ExitCodesTest::noRuntime, "drain")
                                                .refusal(ExitCodesTest::recordUnidentifiableRuntime,
                                                                    ExitCodesTest::drainNotIdentifiable, "drain"),
                            new Cases("config")
                                                .success(ExitCodesTest::serveReadsAndLeave, "config")
                                                .refusal("config")
                                                .refusal("config", "show"),
                            new Cases("credentials")
                                                .success(ExitCodesTest::noRuntime, "credentials")
                                                .refusal(ExitCodesTest::refuseEverything,
                                                                    test -> List.of(MessageCatalogue.format(test.locale(),
                                                                                        "operator.credentials.status_failed",
                                                                                        Map.of("key", test.id, "reason", test.refusalTitle()))),
                                                                    "credentials", "status", ID),
                            new Cases("project")
                                                .success(ExitCodesTest::noRuntime, "project")
                                                .refusal("project", ID),
                            new Cases("projects")
                                                .success(ExitCodesTest::serveReads, "projects")
                                                .refusal("projects"),
                            new Cases("client")
                                                .success(ExitCodesTest::noRuntime, "client")
                                                .refusal("client", "status"),
                            new Cases("plan")
                                                .success(ExitCodesTest::serveReads, "plan", "watches", ID)
                                                .refusal("plan", ID)
                                                .refusal("plan", "findings", ID)
                                                .refusal("plan", "watches", ID)
                                                .refusal("plan", "settings", ID),
                            new Cases("epic")
                                                .success(ExitCodesTest::serveReads, "epic", "findings", ID)
                                                .refusal("epic", ID)
                                                .refusal("epic", "findings", ID)
                                                .refusal("epic", "watches", ID),
                            new Cases("recipe")
                                                .success(ExitCodesTest::noRuntime, "recipe")
                                                .refusal("recipe", "run", ID, "--project", ID, "--no-input"),
                            new Cases("lesson")
                                                .success(ExitCodesTest::noRuntime, "lesson")
                                                .refusal("lesson", "check", ID),
                            new Cases("questions")
                                                .success(ExitCodesTest::serveReads, "questions")
                                                .refusal("questions"),
                            new Cases("answer")
                                                .usage("answer", ID, UNKNOWN_OPTION_TOKEN)
                                                .success(ExitCodesTest::serveReads, "answer", ID, "later")
                                                .refusal(ExitCodesTest::serveDecisionRefuseActs, null, "answer", ID, "later")
                                                .refusal(ExitCodesTest::refuseEverything,
                                                                    test -> List.of(MessageCatalogue.format(test.locale(),
                                                                                        "operator.decision.parts_unreadable",
                                                                                        Map.of("id", test.id))),
                                                                    "answer", ID, "1"),
                            new Cases("reports")
                                                .success(ExitCodesTest::serveReads, "reports")
                                                .refusal("reports"),
                            new Cases("system")
                                                .success(ExitCodesTest::serveReads, "system", "--json")
                                                .refusal("system"),
                            new Cases("cleanup")
                                                .success(ExitCodesTest::serveReads, "cleanup", "--kept")
                                                .refusal("cleanup")
                                                .refusal("cleanup", "--kept"),
                            new Cases("enrol-harness")
                                                .success(ExitCodesTest::serveReadsWithHarness, "enrol-harness", CLIENT, "--yes")
                                                .refusal(ExitCodesTest::refuseEverythingWithHarness, null, "enrol-harness", CLIENT),
                            new Cases("roles")
                                                .success(ExitCodesTest::noRuntime, "roles")
                                                .refusal("roles", "list")
                                                .refusal("roles", "show", ID),
                            new Cases("accept-edit")
                                                .success(ExitCodesTest::serveReads, "accept-edit", "workspace", ID, "--project", ID)
                                                .refusal(ExitCodesTest::refuseEverything,
                                                                    test -> List.of(MessageCatalogue.format(test.locale(),
                                                                                        "enrolment.accept.failed",
                                                                                        Map.of("path", test.id, "reason", test.refusalTitle()))),
                                                                    "accept-edit", "workspace", ID, "--project", ID),
                            new Cases("needs")
                                                .success(ExitCodesTest::serveReads, "needs", "--json")
                                                .refusal("needs"),
                            new Cases("find")
                                                .success(ExitCodesTest::serveReads, "find", ID, "--print")
                                                .refusal("find", ID)
        );
        var registry = new LinkedHashMap<String, VerbCases>();
        for (var cases : all) {
            registry.put(cases.verb(), cases.build());
        }
        return Map.copyOf(registry);
    }

    static List<String> registeredVerbs() {
        return Arrays.stream(PmOperator.class.getAnnotation(Command.class).subcommands())
                            .map(type -> type.getAnnotation(Command.class).name())
                            .toList();
    }

    /**
     * @param kind which cases of a verb
     * @return one argument set per case of every registered verb, and one per registered verb that
     *         the registry has no case of this kind for, which fails
     */
    private static Stream<Arguments> rows(Function<VerbCases, List<Row>> kind) {
        return registeredVerbs().stream().flatMap(verb -> {
            var cases = REGISTRY.get(verb);
            var rows = cases == null ? List.<Row>of() : kind.apply(cases);
            if (rows.isEmpty()) {
                return Stream.of(Arguments.of(verb + ": no case", verb, -1));
            }
            return IntStream.range(0, rows.size())
                                .mapToObj(index -> Arguments.of(rows.get(index).label(), verb, index));
        });
    }

    static Stream<Arguments> successRows() {
        return rows(VerbCases::success);
    }

    static Stream<Arguments> refusalRows() {
        return rows(VerbCases::refusal);
    }

    static Stream<Arguments> usageRows() {
        return rows(VerbCases::usage);
    }

    private static Row row(String verb, int index, Function<VerbCases, List<Row>> kind) {
        if (index < 0) {
            fail("the registry of ExitCodesTest has no such case for the registered verb " + verb);
        }
        return kind.apply(REGISTRY.get(verb)).get(index);
    }

    // ---------------------------------------------------------------------------------------------
    // Setups
    // ---------------------------------------------------------------------------------------------

    private static void noRuntime(ExitCodesTest test) {
        // nothing runs: no runtime record, no runtime
    }

    private static void refuseEverything(ExitCodesTest test) throws IOException {
        PmOperatorTest.writeLiveRecord(test.fixture);
        test.server = test.fixture.start((request, response) -> response.send(500, null,
                            "{\"error\":\"internal_fault\"}"));
    }

    private static void refuseEverythingWithHarness(ExitCodesTest test) throws IOException {
        test.prepareHarness();
        refuseEverything(test);
    }

    /** Answers the decision of the test's id with one part, and refuses every act on it. */
    private static void serveDecisionRefuseActs(ExitCodesTest test) throws IOException {
        PmOperatorTest.writeLiveRecord(test.fixture);
        var decision = "/api/v1/decisions/" + test.id;
        test.server = test.fixture.start((request, response) -> {
            if (decision.equals(request.target()) && "GET".equals(request.method().toUpperCase(Locale.ROOT))) {
                response.json(200, test.decision());
            } else {
                response.send(500, null, "{\"error\":\"internal_fault\"}");
            }
        });
    }

    private static void serveReads(ExitCodesTest test) throws IOException {
        PmOperatorTest.writeLiveRecord(test.fixture);
        var decision = "/api/v1/decisions/" + test.id;
        var digest = new StoreDigestComparison(test.id, 1, test.id, 1, test.id, null).toJson();
        var review = new ReviewResource(test.id, test.id, "findings", List.of(), 0, 0, "open", null, null).toJson();
        test.server = test.fixture.start((request, response) -> {
            var target = request.target();
            var get = "GET".equals(request.method().toUpperCase(Locale.ROOT));
            if ("/api/v1/projects".equals(target)) {
                response.json(200, "{\"projects\":[]}");
            } else if ("/api/v1/reports".equals(target)) {
                response.json(200, "{\"unread_count\":0,\"items\":[]}");
            } else if ("/api/v1/config".equals(target)) {
                response.json(200, "[]");
            } else if (decision.equals(target) && get) {
                response.json(200, test.decision());
            } else if (target.startsWith(decision + "/")) {
                response.json(200, "{\"ok\":true}");
            } else if (target.contains("/stores/digest")) {
                response.json(200, digest);
            } else if (target.startsWith("/api/v1/epics/") && get) {
                response.json(200, review);
            } else if (target.startsWith("/api/v1/decisions") && get) {
                response.json(200, "[]");
            } else if ("/api/v1/system/cleanup/kept".equals(target)) {
                response.json(200, "[]");
            } else if (target.startsWith("/api/v1/system/release-notes")) {
                response.send(200, null, "release notes");
            } else if (target.startsWith("/api/v1/harnesses/") && get) {
                response.send(404, null, "");
            } else {
                response.json(200, "{}");
            }
        });
    }

    /** Serves the reads, and the operator types {@code q} at the first prompt the verb draws. */
    private static void serveReadsAndLeave(ExitCodesTest test) throws IOException {
        test.terminalInput = "q\n".getBytes(StandardCharsets.UTF_8);
        serveReads(test);
    }

    private static void serveReadsWithHarness(ExitCodesTest test) throws IOException {
        test.prepareHarness();
        serveReads(test);
    }

    /** Records a runtime under this process's id with a start instant this process does not have. */
    private static void recordUnidentifiableRuntime(ExitCodesTest test) throws IOException {
        var self = ProcessHandle.current();
        var start = self.info().startInstant().orElseThrow().minus(Duration.ofDays(1));
        Files.createDirectories(test.fixture.paths().stateDir());
        Files.writeString(test.fixture.paths().runtimeRecord(),
                            "{\"pid\":" + self.pid() + ",\"start_instant\":\"" + start + "\"}");
    }

    private static List<String> drainNotIdentifiable(ExitCodesTest test) {
        return List.of(MessageCatalogue.format(test.locale(), "operator.drain.not_identifiable",
                            Map.of("pid", String.valueOf(ProcessHandle.current().pid()))));
    }

    /**
     * Puts a probe of the first supported harness client on the path of the run, which answers its
     * version probe.
     */
    private void prepareHarness() throws IOException {
        var bin = Files.createDirectories(tempDir.resolve("bin"));
        for (var candidate : HostDetection.clientIds()) {
            var host = HostDetection.host(candidate).orElse(null);
            if (host == null) {
                continue;
            }
            var probe = host.versionProbe();
            var executable = probe.isEmpty() ? candidate : probe.getFirst();
            var script = bin.resolve(executable);
            Files.writeString(script, "#!/bin/sh\necho '1.0.0'\n");
            assertTrue(script.toFile().setExecutable(true), "cannot make " + script + " executable");
            extraEnvironment.put("PATH", bin.toString());
            client = candidate;
            return;
        }
        fail("no supported harness client carries a profile");
    }

    // ---------------------------------------------------------------------------------------------
    // Running
    // ---------------------------------------------------------------------------------------------

    private Map<String, String> environment() {
        var environment = new HashMap<>(fixture.environment());
        environment.putAll(extraEnvironment);
        return environment;
    }

    private String locale() {
        return ClientEnvironment.capture(environment()).locale();
    }

    private String refusalTitle() {
        return ErrorTexts.title(locale(), REFUSAL);
    }

    /** @return a decision of the test's id with one part of one option, as the runtime serves it */
    private String decision() {
        return """
                            {
                              "id": "%s", "revision": 1, "title": "%s", "subject": "plan %s",
                              "scope": "plan", "blocks": "plan", "reversibility": "reversible",
                              "plan": "%s", "project": "%s",
                              "parts": [{"part_id": "1", "question": "%s", "options": [{"key": "1", "label": "%s"}]}]
                            }
                            """.formatted(id, id, id, id, id, id, id);
    }

    /**
     * @return the line a verb that would open a full-screen view prints on stderr when the terminal of
     *         the run is dumb, before it prints to the scrollback
     */
    private String fullScreenNotice() {
        return MessageCatalogue.format(locale(), "operator.tui.unavailable.dumb_terminal", Map.of());
    }

    /** @return the lines on stderr, without the full-screen notice a dumb terminal prints first */
    private List<String> errLinesAfterNotice() {
        var lines = errLines();
        return !lines.isEmpty() && lines.getFirst().equals(fullScreenNotice()) ? lines.subList(1, lines.size()) : lines;
    }

    private int execute(String... args) {
        var environment = environment();
        var input = terminalInput;
        var context = new OperatorContext(
                            environment,
                            tempDir,
                            true,
                            ClientEnvironment.capture(environment),
                            () -> new DumbTerminal(new ByteArrayInputStream(input), new ByteArrayOutputStream()),
                            _ -> {
                            }
        );
        return PmOperator.execute(context, new PrintWriter(out, true), new PrintWriter(err, true), args);
    }

    private String unknownOption = "";

    private int run(Row row) throws IOException {
        row.setup().apply(this);
        unknownOption = "--" + Generators.letterStrings(6, 12).next().toLowerCase(Locale.ROOT);
        var args = row.args().stream()
                            .map(arg -> switch (arg) {
                                case ID -> id;
                                case CLIENT -> {
                                    assertNotNull(client, "the case names a harness client but prepared none");
                                    yield client;
                                }
                                case UNKNOWN_OPTION_TOKEN -> unknownOption;
                                default -> arg;
                            })
                            .toArray(String[]::new);
        return execute(args);
    }

    private List<String> errLines() {
        return err.toString().lines().toList();
    }

    private String streams() {
        return " stdout: [" + out + "] stderr: [" + err + "]";
    }

    // ---------------------------------------------------------------------------------------------
    // The tests
    // ---------------------------------------------------------------------------------------------

    @Test
    @DisplayName("the registry names exactly the registered verbs, each with a success, a refusal and a usage case")
    void registryCoversEveryRegisteredVerb() {
        assertEquals(new TreeSet<>(registeredVerbs()), new TreeSet<>(REGISTRY.keySet()),
                            "the verbs of the registry differ from the verbs PmOperator registers");
        assertAll(REGISTRY.entrySet().stream().map(entry -> (Executable) () -> {
            var cases = entry.getValue();
            assertFalse(cases.success().isEmpty(), entry.getKey() + " has no success case");
            assertFalse(cases.refusal().isEmpty(), entry.getKey() + " has no refusal case");
            assertFalse(cases.usage().isEmpty(), entry.getKey() + " has no usage case");
        }));
    }

    @Nested
    @DisplayName("Exit code 0: Success")
    class Success {

        @ParameterizedTest(name = "{0}")
        @MethodSource("de.planmarshall.operator.ExitCodesTest#successRows")
        @DisplayName("Every registered verb has a success that exits 0 with nothing on stderr")
        void successExitsZero(String label, String verb, int index) throws IOException {
            var row = row(verb, index, VerbCases::success);

            var code = run(row);

            assertEquals(0, code, label + " did not exit 0." + streams());
            assertEquals(List.of(), errLinesAfterNotice(), label + " printed on stderr." + streams());
        }

        @Test
        @DisplayName("-h and --help exit 0")
        void helpExitsZero() {
            assertAll(
                                () -> assertEquals(0, execute("-h")),
                                () -> assertEquals(0, execute("--help"))
            );
        }

        @Test
        @DisplayName("-V and --version exit 0")
        void versionExitsZero() {
            assertAll(
                                () -> assertEquals(0, execute("-V")),
                                () -> assertEquals(0, execute("--version"))
            );
        }
    }

    @Nested
    @DisplayName("Exit code 1: Refusals")
    class Refusals {

        @ParameterizedTest(name = "{0}")
        @MethodSource("de.planmarshall.operator.ExitCodesTest#refusalRows")
        @DisplayName("Every registered verb renders its refusal on stderr in three lines and exits 1")
        void refusalRendersThreeLines(String label, String verb, int index) throws IOException {
            var row = row(verb, index, VerbCases::refusal);

            var code = run(row);

            assertEquals(1, code, label + " did not exit 1." + streams());
            assertEquals("", out.toString(), label + " printed on stdout." + streams());
            var ownLines = row.ownLines();
            if (ownLines != null) {
                assertEquals(ownLines.apply(ExitCodesTest.this), errLines(),
                                    label + " did not print its own refusal line." + streams());
                return;
            }
            var lines = errLinesAfterNotice();
            assertEquals(3, lines.size(), label + " did not render the three refusal lines." + streams());
            assertAll(
                                () -> assertEquals(GLYPH_REFUSED + ErrorTexts.title(locale(), REFUSAL), lines.get(0),
                                                    label + ": title line." + streams()),
                                () -> assertEquals(ErrorTexts.cause(locale(), REFUSAL), lines.get(1),
                                                    label + ": cause line." + streams()),
                                () -> assertTrue(lines.get(2).startsWith(GLYPH_FIX),
                                                    label + ": fix line." + streams())
            );
        }
    }

    @Nested
    @DisplayName("Exit code 2: Usage errors")
    class UsageErrors {

        @Test
        @DisplayName("Unknown top-level verb exits 2 and names the verb")
        void unknownVerbExitsTwo() {
            var verb = "x" + Generators.letterStrings(6, 12).next().toLowerCase(Locale.ROOT);

            var code = execute(verb);

            assertEquals(2, code, streams());
            assertEquals(MessageCatalogue.format(locale(), "operator.cli.unknown_verb", Map.of("verb", verb)),
                                err.toString().strip(), streams());
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("de.planmarshall.operator.ExitCodesTest#usageRows")
        @DisplayName("Every registered verb exits 2 on an unknown option and names the option")
        void unknownOptionExitsTwo(String label, String verb, int index) throws IOException {
            var row = row(verb, index, VerbCases::usage);

            var code = run(row);

            assertEquals(2, code, label + " did not exit 2." + streams());
            assertEquals(MessageCatalogue.format(locale(), "operator.cli.unknown_option",
                                Map.of("option", unknownOption)), err.toString().strip(), label + streams());
        }
    }

    @Nested
    @DisplayName("Exit code 3: Choice not answered (--no-input)")
    class NeedsInput {

        @Test
        @DisplayName("An unasked choice prints its question and the Needs a choice line, sends nothing and exits 3")
        void unaskedChoiceExitsThree() throws Exception {
            serveReads(ExitCodesTest.this);

            var code = execute("cleanup", "--release", "--all", "--no-input");

            assertEquals(3, code, streams());
            assertEquals(List.of(
                                MessageCatalogue.format(locale(), "system.cleanup.kept.confirm_all", Map.of("count", 0)),
                                MessageCatalogue.format(locale(), "operator.common.needs_choice", Map.of("option", "--yes"))),
                                out.toString().lines().toList(), streams());
            assertEquals("", err.toString(), streams());
            assertNoAct();
        }
    }

    @Nested
    @DisplayName("Exit code 4: Operator security checks")
    class OperatorChecks {

        @Test
        @DisplayName("Act command in a coding agent environment exits 4 and names the marker")
        void agentEnvironmentExitsFour() throws Exception {
            PmOperatorTest.writeLiveRecord(fixture);
            var marker = "CLAUDE_CODE";
            extraEnvironment.put(marker, "1");

            var code = execute("runtime", "stop");

            assertEquals(4, code, streams());
            assertEquals("stop: " + MessageCatalogue.format(locale(), OperatorGate.AGENT_REFUSED_KEY,
                                Map.of(OperatorGate.MARKER_PARAM, marker)), err.toString().strip(), streams());
            assertEquals("", out.toString(), streams());
        }

        @Test
        @DisplayName("Act command on a non-interactive terminal exits 4 and names the terminal")
        void nonInteractiveTerminalExitsFour() throws Exception {
            PmOperatorTest.writeLiveRecord(fixture);
            var environment = environment();
            var context = new OperatorContext(
                                environment,
                                tempDir,
                                false,
                                ClientEnvironment.capture(environment),
                                () -> new DumbTerminal(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream()),
                                _ -> {
                                }
            );

            var code = PmOperator.execute(context, new PrintWriter(out, true), new PrintWriter(err, true),
                                "runtime", "stop");

            assertEquals(4, code, streams());
            assertEquals("stop: " + MessageCatalogue.format(locale(), OperatorGate.TERMINAL_REFUSED_KEY, Map.of()),
                                err.toString().strip(), streams());
            assertEquals("", out.toString(), streams());
        }
    }

    @Nested
    @DisplayName("Exit code 130: Interrupted")
    class Interrupted {

        @Test
        @DisplayName("Ctrl+C at a confirmation exits 130, prints no refusal and sends nothing")
        void interruptAtConfirmationExitsOneThirty() throws Exception {
            serveReads(ExitCodesTest.this);
            try (var virtual = VirtualScreenTerminal.create(160, 40)) {
                var environment = environment();
                environment.put("LANG", "en_US.UTF-8");
                var context = new OperatorContext(environment, tempDir, true,
                                    ClientEnvironment.capture(environment), virtual::terminal, null);

                var code = interruptedAt(virtual, "Revoke?", () -> PmOperator.execute(context,
                                    new PrintWriter(out, true), new PrintWriter(err, true),
                                    "web", "devices", "revoke", id, "--interactive"));

                assertEquals(StartPrompt.EXIT_INTERRUPTED, code, streams());
                assertEquals("", err.toString(), streams());
                assertNoAct();
            }
        }

        /**
         * Runs a verb and raises the terminal's interrupt as soon as its prompt is drawn, repeating
         * the raise until the verb returns, because the reader installs its handler only once it reads.
         */
        private int interruptedAt(VirtualScreenTerminal virtual, String marker, Verb verb) throws Exception {
            var code = new AtomicInteger(-1);
            var failure = new AtomicReference<RuntimeException>();
            var runner = Thread.ofPlatform().name("verb-under-test").start(() -> {
                try {
                    code.set(verb.run());
                } catch (RuntimeException failed) {
                    failure.set(failed);
                }
            });
            var deadline = System.nanoTime() + INTERRUPT_TIMEOUT.toNanos();
            var prompted = false;
            while (runner.isAlive() && System.nanoTime() < deadline) {
                if (out.toString().contains(marker) || String.join("\n", virtual.screen().lines()).contains(marker)) {
                    prompted = true;
                    virtual.terminal().raise(Terminal.Signal.INT);
                    virtual.type("\u0003");
                }
                LockSupport.parkNanos(POLL.toNanos());
            }
            runner.join(TimeUnit.SECONDS.toMillis(1));
            assertFalse(runner.isAlive(), "the verb did not leave after the interrupt." + streams());
            assertNull(failure.get(), "the verb failed: " + failure.get());
            assertTrue(prompted, marker + " was never drawn, the interrupt was not sent." + streams());
            return code.get();
        }
    }

    /** A verb run on its own thread. */
    @FunctionalInterface
    private interface Verb {
        /** @return the exit code */
        int run();
    }

    private void assertNoAct() {
        assertNotNull(server, "no runtime was started");
        var sent = server.requests().stream()
                            .filter(request -> !"GET".equals(request.method().toUpperCase(Locale.ROOT)))
                            .toList();
        assertTrue(sent.isEmpty(), "expected no act at the runtime, got " + sent);
    }
}
