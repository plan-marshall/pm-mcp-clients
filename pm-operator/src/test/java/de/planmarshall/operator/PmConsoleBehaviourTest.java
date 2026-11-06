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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.impl.DumbTerminal;

import de.planmarshall.api.onboarding.OnboardingAspect;
import de.planmarshall.api.project.BeingAddedProject;
import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The console's own behaviour beyond the typed-verb transcripts: the bare-number hint, the hint on
 * an empty {@code Ctrl+C}, the questions that stay open on {@code Ctrl+D}, and the desktop
 * notification payload, which must not carry the text of the question.
 */
@EnableGeneratorController
@DisplayName("PmConsole behaviour")
class PmConsoleBehaviourTest {

    @TempDir
    Path tempDir;

    private RuntimeFixture fixture;

    @BeforeEach
    void setUp() throws Exception {
        fixture = RuntimeFixture.create();
        PmOperatorTest.writeLiveRecord(fixture);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (fixture != null) {
            fixture.close();
        }
    }

    private OperatorContext context(ByteArrayOutputStream rendered, String typed) {
        var input = new ByteArrayInputStream(typed.getBytes(StandardCharsets.UTF_8));
        return new OperatorContext(fixture.environment(), tempDir, true,
                            ClientEnvironment.capture(fixture.environment()),
                            () -> new DumbTerminal(input, rendered), null);
    }

    private void onboardedWithQuestion() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/status".equals(request.target())) {
                response.json(200, """
                                    {"status":"ready","onboarded":true,"version":"0.9.0",
                                     "needs_operator":{"question":2},
                                     "web":{"enabled":true,"port":7420}}
                                    """);
            } else if ("/api/v1/web/devices".equals(request.target())) {
                response.json(200, "[{\"label\":\"local browser\",\"paired_by\":\"open\"}]");
            } else if ("/api/v1/reports".equals(request.target())) {
                response.json(200, "{\"unread_count\":0}");
            } else {
                response.send(404, null, "");
            }
        });
    }

    @Test
    @DisplayName("a bare number is answered with the not-a-command hint")
    void bareNumber() throws Exception {
        onboardedWithQuestion();
        var rendered = new ByteArrayOutputStream();
        var code = new PmConsole(context(rendered, "2\nexit\n")).run();
        assertEquals(0, code);
        assertTrue(rendered.toString(StandardCharsets.UTF_8).contains("2 is not a command here"),
                            rendered.toString(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Ctrl+D names the open questions once, then leaves on the second one")
    void ctrlDKeepsQuestions() throws Exception {
        onboardedWithQuestion();
        var rendered = new ByteArrayOutputStream();
        var code = new PmConsole(context(rendered, "\u0004\u0004")).run();
        assertEquals(0, code);
        var text = rendered.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("2 questions stay open"), text);
        assertTrue(text.contains("exit or Ctrl+D again leaves"), text);
    }

    @Test
    @DisplayName("a notice is held while the view is open and summarized when it leaves")
    void noticesAreHeldWhileTheViewIsOpen() throws Exception {
        var rendered = new ByteArrayOutputStream();
        try (var terminal = new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered)) {
            var reader = LineReaderBuilder.builder().terminal(terminal).build();
            var context = new OperatorContext(fixture.environment(), tempDir, true,
                                ClientEnvironment.capture(fixture.environment()),
                                () -> new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered), null);
            var console = new PmConsole(context);
            console.enterFullScreen();
            console.applyConsoleEvent("question", Map.of("type", "question", "id", "Q-1",
                                "scope", "plan fix-relay", "summary", "blocks the plan"), reader);
            console.applyConsoleEvent("report", Map.of("type", "report", "id", "r-1",
                                "from", "pm-mcp-clients", "subject", "merged"), reader);

            var printed = new StringWriter();
            console.leaveFullScreen(new PrintWriter(printed));

            assertAll(
                                () -> assertFalse(rendered.toString(StandardCharsets.UTF_8).contains("Question Q-1"),
                                                    rendered.toString(StandardCharsets.UTF_8)),
                                () -> assertTrue(printed.toString().contains(
                                                                        "While the overview was open: 1 decision arrived, 1 new report."),
                                                    printed.toString())
            );
        }
    }

    @Test
    @DisplayName("an envelope decision notifies with the scope the envelope declares")
    void notificationFromEnvelope() throws Exception {
        var rendered = new ByteArrayOutputStream();
        try (var terminal = new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered)) {
            var reader = LineReaderBuilder.builder().terminal(terminal).build();
            var context = new OperatorContext(fixture.environment(), tempDir, true,
                                ClientEnvironment.capture(fixture.environment()),
                                () -> new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered), null);
            new PmConsole(context).applyConsoleEvent("decision.opened", Map.of(
                                                    "event", "decision.opened",
                                                    "id", 7,
                                                    "scope", Map.of("kind", "plan", "id", "fix-relay"),
                                                    "data", Map.of("decision", Map.of(
                                                                        "id", "Q-007", "subject", "plan fix-relay", "blocks", "plan"))),
                                reader);
        }
        var text = rendered.toString(StandardCharsets.UTF_8);
        final var start = text.indexOf("\u001B]9;");
        final var notification = text.substring(start, text.indexOf('\u0007', start));
        assertAll(
                            () -> assertTrue(notification.contains("plan fix-relay"), notification),
                            () -> assertTrue(notification.contains("decision"), notification),
                            () -> assertFalse(notification.contains("Q-007"), notification)
        );
    }

    @Test
    @DisplayName("an event rebuilds the prompt badges and the window title from the status")
    void badgesRefreshFromTheStatus() throws Exception {
        var calls = new AtomicInteger();
        fixture.start((request, response) -> {
            if ("/api/v1/status".equals(request.target())) {
                var count = calls.getAndIncrement() == 0 ? 3 : 2;
                response.json(200, "{\"status\":\"ready\",\"onboarded\":true,"
                                    + "\"needs_operator\":{\"question\":" + count + "}}");
            } else {
                response.send(404, null, "");
            }
        });
        var rendered = new ByteArrayOutputStream();
        try (var terminal = new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered)) {
            var reader = LineReaderBuilder.builder().terminal(terminal).build();
            var context = new OperatorContext(fixture.environment(), tempDir, true,
                                ClientEnvironment.capture(fixture.environment()),
                                () -> new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered), null);
            var console = new PmConsole(context);
            var decision = Map.<String, Object>of("event", "decision.opened",
                                "data", Map.of("decision", Map.of(
                                                    "id", "Q-007", "subject", "plan fix-relay", "blocks", "plan")));

            console.applyConsoleEvent("decision.opened", decision, reader);
            console.applyConsoleEvent("decision.opened", decision, reader);
        }
        var text = rendered.toString(StandardCharsets.UTF_8);
        assertAll(
                            () -> assertTrue(text.contains("pm-operator [3 questions]"), text),
                            () -> assertTrue(text.contains("pm-operator [2 questions]"), text)
        );
    }

    @Test
    @DisplayName("the window title follows the badge the dialogue left behind")
    void theTitleFollowsTheSetupBadge() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/status".equals(request.target())) {
                response.json(200, """
                                    {"status":"onboarding","onboarded":false,"version":"0.9.0","onboard":{"step":3}}
                                    """);
            } else if ("/api/v1/project-onboardings".equals(request.target())) {
                response.json(200, "{\"onboardings\":[]}");
            } else {
                response.send(404, null, "");
            }
        });
        var rendered = new ByteArrayOutputStream();
        try (var terminal = new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered)) {
            var context = new OperatorContext(fixture.environment(), tempDir, true,
                                ClientEnvironment.capture(fixture.environment()),
                                () -> new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered), null);
            new PmConsole(context).refreshSetupBadge("en", terminal);
        }
        assertTrue(rendered.toString(StandardCharsets.UTF_8).contains("pm-operator [setup 3/7]"),
                            rendered.toString(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("a reconnect reads the runtime state again and redraws the title")
    void aReconnectReadsTheState() throws Exception {
        var statusReads = new AtomicInteger();
        var firstOpenLatch = new CountDownLatch(1);
        var reconnectLatch = new CountDownLatch(1);
        fixture.start((request, response) -> {
            if ("/api/v1/status".equals(request.target())) {
                var read = statusReads.getAndIncrement();
                if (read == 0) {
                    firstOpenLatch.countDown();
                    response.json(200, "{\"status\":\"ready\",\"onboarded\":true,"
                                        + "\"needs_operator\":{\"question\":3}}");
                } else {
                    reconnectLatch.countDown();
                    response.json(200, "{\"status\":\"ready\",\"onboarded\":true,"
                                        + "\"needs_operator\":{\"question\":2}}");
                }
            } else if ("/api/v1/project-onboardings".equals(request.target())) {
                response.json(200, "{\"onboardings\":[]}");
            } else if ("/api/v1/events".equals(request.target())) {
                response.events(200).close();
            } else {
                response.send(404, null, "");
            }
        });
        var redrawnLatch = new CountDownLatch(1);
        var rendered = new ByteArrayOutputStream() {
            @Override
            public synchronized void write(byte[] b, int off, int len) {
                super.write(b, off, len);
                if (toString(StandardCharsets.UTF_8).contains("pm-operator [2 questions]")) {
                    redrawnLatch.countDown();
                }
            }

            @Override
            public synchronized void write(int b) {
                super.write(b);
                if (toString(StandardCharsets.UTF_8).contains("pm-operator [2 questions]")) {
                    redrawnLatch.countDown();
                }
            }
        };
        var console = new PmConsole(new OperatorContext(fixture.environment(), tempDir, true,
                            ClientEnvironment.capture(fixture.environment()),
                            () -> new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered), null));
        try (var terminal = new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered)) {
            var reader = LineReaderBuilder.builder().terminal(terminal).build();
            var thread = console.startEventListener(reader, terminal);
            try {
                assertTrue(firstOpenLatch.await(5, TimeUnit.SECONDS), "first-open status read was performed");
                assertTrue(reconnectLatch.await(5, TimeUnit.SECONDS), "reconnect status read was performed");
                assertTrue(redrawnLatch.await(5, TimeUnit.SECONDS), "reconnect redrew the title with updated count");
            } finally {
                console.stop();
                thread.interrupt();
            }
        }
        assertTrue(rendered.toString(StandardCharsets.UTF_8).contains("pm-operator [2 questions]"),
                            rendered.toString(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("the notification payload names the scope and the kind, never the question text")
    void notificationPayload() throws Exception {
        var rendered = new ByteArrayOutputStream();
        try (var terminal = new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered)) {
            var reader = LineReaderBuilder.builder().terminal(terminal).build();
            var context = new OperatorContext(fixture.environment(), tempDir, true,
                                ClientEnvironment.capture(fixture.environment()),
                                () -> new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered), null);
            new PmConsole(context).applyConsoleEvent("question", Map.of(
                                "type", "question", "id", "Q-1", "scope", "plan fix-relay",
                                "summary", "a secret summary", "text", "the text of the question"), reader);
        }
        var text = rendered.toString(StandardCharsets.UTF_8);
        var notification = text.substring(text.indexOf("\u001B]9;"));
        notification = notification.substring(0, notification.indexOf('\u0007'));
        assertTrue(notification.contains("plan fix-relay"), notification);
        assertTrue(notification.contains("decision"), notification);
        assertFalse(notification.contains("a secret summary"), notification);
        assertFalse(notification.contains("the text of the question"), notification);
    }

    @Test
    @DisplayName("a failed read keeps the previous badges")
    void aFailedReadKeepsThePreviousBadges() throws Exception {
        var onboardingsFail = new AtomicBoolean(false);
        fixture.start((request, response) -> {
            if ("/api/v1/status".equals(request.target())) {
                response.json(200, "{\"status\":\"ready\",\"onboarded\":true}");
            } else if ("/api/v1/project-onboardings".equals(request.target())) {
                if (onboardingsFail.get()) {
                    response.send(500, null, "{\"error\":\"runtime error\"}");
                } else {
                    response.json(200, """
                                        {"onboardings":[{"checkout_path":"/tmp/cui-http",
                                                         "current_step":"build",
                                                         "step_number":5,"total_steps":10}]}
                                        """);
                }
            } else {
                response.send(404, null, "");
            }
        });
        var rendered = new ByteArrayOutputStream();
        try (var terminal = new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered)) {
            var context = new OperatorContext(fixture.environment(), tempDir, true,
                                ClientEnvironment.capture(fixture.environment()),
                                () -> new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered), null);
            var console = new PmConsole(context);
            console.refreshSetupBadge("en", terminal);
            assertEquals(List.of("setup cui-http 5/10"), console.currentBadges());

            onboardingsFail.set(true);
            console.refreshSetupBadge("en", terminal);
            assertEquals(List.of("setup cui-http 5/10"), console.currentBadges(),
                                "a failed read must keep the previous badges");
        }
    }

    @Test
    @DisplayName("a 200 without onboardings keeps the cached project badges")
    void a200WithoutOnboardingsKeepsTheCachedProjectBadges() throws Exception {
        var malformedResponse = new AtomicBoolean(false);
        fixture.start((request, response) -> {
            if ("/api/v1/status".equals(request.target())) {
                response.json(200, "{\"status\":\"ready\",\"onboarded\":true}");
            } else if ("/api/v1/project-onboardings".equals(request.target())) {
                if (malformedResponse.get()) {
                    response.json(200, "{}");
                } else {
                    response.json(200, """
                                        {"onboardings":[{"checkout_path":"/tmp/cui-http",
                                                         "current_step":"build",
                                                         "step_number":5,"total_steps":10}]}
                                        """);
                }
            } else {
                response.send(404, null, "");
            }
        });
        var rendered = new ByteArrayOutputStream();
        try (var terminal = new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered)) {
            var context = new OperatorContext(fixture.environment(), tempDir, true,
                                ClientEnvironment.capture(fixture.environment()),
                                () -> new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered), null);
            var console = new PmConsole(context);
            console.refreshSetupBadge("en", terminal);
            assertEquals(List.of("setup cui-http 5/10"), console.currentBadges());

            malformedResponse.set(true);
            console.refreshSetupBadge("en", terminal);
            assertEquals(List.of("setup cui-http 5/10"), console.currentBadges(),
                                "a 200 without onboardings must keep the previous badges");
        }
    }

    @Test
    @DisplayName("the prompt is not delayed past the deadline by a stalled refresh")
    void thePromptIsNotDelayedPastTheDeadlineByAStalledRefresh() throws Exception {
        // The stalemate ends when the test releases it, never on a timer of its own.
        var release = new CountDownLatch(1);
        try {
            fixture.start((request, response) -> {
                if ("/api/v1/status".equals(request.target())
                                    || "/api/v1/project-onboardings".equals(request.target())) {
                    awaitRelease(release);
                    response.json(200, "/api/v1/status".equals(request.target())
                                        ? "{\"status\":\"ready\",\"onboarded\":true}"
                                        : "{\"onboardings\":[]}");
                } else {
                    response.send(404, null, "");
                }
            });
            var rendered = new ByteArrayOutputStream();
            try (rendered; var terminal = new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered)) {
                var context = new OperatorContext(fixture.environment(), tempDir, true,
                                    ClientEnvironment.capture(fixture.environment()),
                                    () -> new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered), null);
                var console = new PmConsole(context);
                var started = System.nanoTime();
                assertTimeoutPreemptively(Duration.ofSeconds(8), () -> console.refreshSetupBadge("en", terminal));
                var elapsed = Duration.ofNanos(System.nanoTime() - started);
                assertTrue(elapsed.compareTo(PmConsole.REFRESH_BOUND.plusSeconds(1)) < 0, elapsed.toString());
            }
        } finally {
            release.countDown();
        }
    }

    private static void awaitRelease(CountDownLatch release) {
        try {
            release.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    @DisplayName("Enter on a being-added project badge addresses the entry the continue link carries")
    void enterOnABeingAddedProjectBadgeAddressesItsEntry() throws Exception {
        // The fixture is the contract's own map: BeingAddedProject has no project id to write into it.
        var entryId = Generators.letterStrings(4, 10).next();
        var checkout = "/work/" + Generators.letterStrings(4, 10).next();
        var entry = new BeingAddedProject(checkout, "Identity", 3, 10,
                            "/api/v1/entries/" + entryId + "/continue", "/api/v1/entries/" + entryId + "/abandon");
        var exactEntryTarget = new AtomicReference<String>();
        fixture.start((request, response) -> {
            if (("/api/v1/entries/" + entryId).equals(request.target())) {
                exactEntryTarget.set(request.target());
                response.send(404, null, "");
            } else if ("/api/v1/status".equals(request.target())) {
                response.json(200, "{\"status\":\"ready\",\"onboarded\":true}");
            } else if ("/api/v1/project-onboardings".equals(request.target())) {
                response.json(200, "{\"onboardings\":[" + entry.toJson() + "]}");
            } else {
                response.send(404, null, "");
            }
        });
        var rendered = new ByteArrayOutputStream();
        try (var terminal = new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered)) {
            var reader = LineReaderBuilder.builder().terminal(terminal).build();
            var context = new OperatorContext(fixture.environment(), tempDir, true,
                                ClientEnvironment.capture(fixture.environment()),
                                () -> new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered), null);
            var console = new PmConsole(context);
            console.refreshSetupBadge("en", terminal);
            console.handleEmptyEnter(new PrintWriter(rendered), reader, terminal);
            assertEquals("/api/v1/entries/" + entryId, exactEntryTarget.get(),
                                "Enter on a being-added badge must address the entry continue_link carries");
        }
    }

    @Test
    @DisplayName("an entry id with a plus reaches the runtime as a plus")
    void entryIdWithAPlusReachesTheRuntimeAsAPlus() throws Exception {
        var entry = new BeingAddedProject("/work/plus-dir-name", "Identity", 3, 10,
                            "/api/v1/entries/ent+a/continue", "/api/v1/entries/ent+a/abandon");
        var exactEntryTarget = new AtomicReference<String>();
        fixture.start((request, response) -> {
            if (request.target().startsWith("/api/v1/entries/")) {
                exactEntryTarget.set(request.target());
                response.send(404, null, "");
            } else if ("/api/v1/status".equals(request.target())) {
                response.json(200, "{\"status\":\"ready\",\"onboarded\":true}");
            } else if ("/api/v1/project-onboardings".equals(request.target())) {
                response.json(200, "{\"onboardings\":[" + entry.toJson() + "]}");
            } else {
                response.send(404, null, "");
            }
        });
        var rendered = new ByteArrayOutputStream();
        try (var terminal = new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered)) {
            var reader = LineReaderBuilder.builder().terminal(terminal).build();
            var context = new OperatorContext(fixture.environment(), tempDir, true,
                                ClientEnvironment.capture(fixture.environment()),
                                () -> new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered), null);
            var console = new PmConsole(context);
            console.refreshSetupBadge("en", terminal);
            console.handleEmptyEnter(new PrintWriter(rendered), reader, terminal);
            assertEquals("/api/v1/entries/ent%2Ba", exactEntryTarget.get(),
                                "a plus in a path segment is a plus, not a space");
        }
    }

    @Test
    @DisplayName("an entry id with a quote or a backslash reaches the runtime unaltered")
    void entryIdWithQuoteOrBackslashReachesTheRuntimeUnaltered() throws Exception {
        var entry = new BeingAddedProject("/work/other-dir-name", "Identity", 3, 10,
                            "/api/v1/entries/ent-%22q%22%5Cb/continue", "/api/v1/entries/ent-%22q%22%5Cb/abandon");
        var exactEntryTarget = new AtomicReference<String>();
        fixture.start((request, response) -> {
            if (request.target().startsWith("/api/v1/entries/")) {
                exactEntryTarget.set(request.target());
                response.send(404, null, "");
            } else if ("/api/v1/status".equals(request.target())) {
                response.json(200, "{\"status\":\"ready\",\"onboarded\":true}");
            } else if ("/api/v1/project-onboardings".equals(request.target())) {
                response.json(200, "{\"onboardings\":[" + entry.toJson() + "]}");
            } else {
                response.send(404, null, "");
            }
        });
        var rendered = new ByteArrayOutputStream();
        try (var terminal = new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered)) {
            var reader = LineReaderBuilder.builder().terminal(terminal).build();
            var context = new OperatorContext(fixture.environment(), tempDir, true,
                                ClientEnvironment.capture(fixture.environment()),
                                () -> new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered), null);
            var console = new PmConsole(context);
            console.refreshSetupBadge("en", terminal);
            console.handleEmptyEnter(new PrintWriter(rendered), reader, terminal);
            assertEquals("/api/v1/entries/ent-%22q%22%5Cb", exactEntryTarget.get(),
                                "an id with a quote or a backslash must reach the command unaltered");
        }
    }

    @Test
    @DisplayName("a typed quoted id keeps its backslash escapes")
    void typedQuotedIdKeepsItsBackslashEscapes() {
        assertEquals(List.of("project", "setup", "--entry", "ent-\"q\"\\b"),
                            PmConsole.parseArgs("project setup --entry \"ent-\\\"q\\\"\\\\b\""));
    }

    @Test
    @DisplayName("a backslash typed outside quotes stays a backslash")
    void typedBackslashOutsideQuotesStays() {
        assertEquals(List.of("web", "devices", "rename", "1", "my\\label"),
                            PmConsole.parseArgs("web devices rename 1 my\\label"));
    }

    @Test
    @DisplayName("a stalled runtime lets the prompt return within the bound")
    void aStalledRuntimeLetsThePromptReturnWithinTheBound() throws Exception {
        var release = new CountDownLatch(1);
        try {
            fixture.start((request, response) -> {
                if ("/api/v1/status".equals(request.target())) {
                    response.json(200, "{\"status\":\"ready\",\"onboarded\":true}");
                } else if ("/api/v1/project-onboardings".equals(request.target())) {
                    awaitRelease(release);
                    response.json(200, "{\"onboardings\":[]}");
                } else {
                    response.send(404, null, "");
                }
            });
            var rendered = new ByteArrayOutputStream();
            try (rendered; var terminal = new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered)) {
                var context = new OperatorContext(fixture.environment(), tempDir, true,
                                    ClientEnvironment.capture(fixture.environment()),
                                    () -> new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered), null);
                var console = new PmConsole(context);
                assertTimeoutPreemptively(Duration.ofSeconds(8), () -> console.refreshSetupBadge("en", terminal));
            }
        } finally {
            release.countDown();
        }
    }

    @Test
    @DisplayName("a refresh that waits for the lock gives up within the refresh bound")
    void aLockedRefreshGivesUpWithinTheBound() throws Exception {
        var release = new CountDownLatch(1);
        var firstInFlight = new CountDownLatch(1);
        try {
            fixture.start((request, response) -> {
                if ("/api/v1/status".equals(request.target())) {
                    firstInFlight.countDown();
                    // Every refresh is held at its own bound, so the lock is held by one refresh
                    // after the other; a caller that queued behind them waits for more than one.
                    awaitRelease(release);
                    response.json(200, "{\"status\":\"ready\",\"onboarded\":true}");
                } else if ("/api/v1/project-onboardings".equals(request.target())) {
                    response.json(200, "{\"onboardings\":[]}");
                } else {
                    response.send(404, null, "");
                }
            });
            var rendered = new ByteArrayOutputStream();
            try (rendered; var terminal = new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered)) {
                var context = new OperatorContext(fixture.environment(), tempDir, true,
                                    ClientEnvironment.capture(fixture.environment()),
                                    () -> new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered), null);
                var console = new PmConsole(context);
                var first = new Thread(() -> console.refreshSetupBadge("en", terminal));
                var second = new Thread(() -> console.refreshBadges(terminal));
                var third = new Thread(() -> console.refreshSetupBadge("en", terminal));
                first.start();
                // The first refresh takes the lock before its status read leaves the process; the
                // other two queue behind it from there.
                assertTrue(firstInFlight.await(5, TimeUnit.SECONDS), "the first refresh must take the lock");
                second.start();
                third.start();

                var started = System.nanoTime();
                third.join(30_000);
                var waited = Duration.ofNanos(System.nanoTime() - started);

                assertTrue(waited.compareTo(PmConsole.REFRESH_BOUND.plusSeconds(2)) < 0,
                                    "a queued refresh must not wait out its predecessors: " + waited);
                release.countDown();
                first.join(30_000);
                second.join(30_000);
            }
        } finally {
            release.countDown();
        }
    }

    @Test
    @DisplayName("two concurrent refreshes never interleave")
    void twoConcurrentRefreshesNeverInterleave() throws Exception {
        var firstEntered = new CountDownLatch(1);
        var releaseFirst = new CountDownLatch(1);
        var statusRequests = new AtomicInteger();
        try {
            fixture.start((request, response) -> {
                if ("/api/v1/status".equals(request.target())) {
                    statusRequests.incrementAndGet();
                    if (firstEntered.getCount() > 0) {
                        firstEntered.countDown();
                        // The first refresh keeps the lock and its read open until the test says so.
                        awaitRelease(releaseFirst);
                    }
                    response.json(200, "{\"status\":\"ready\",\"onboarded\":true}");
                } else if ("/api/v1/project-onboardings".equals(request.target())) {
                    response.json(200, "{\"onboardings\":[]}");
                } else {
                    response.send(404, null, "");
                }
            });
            var rendered = new ByteArrayOutputStream();
            try (rendered; var terminal = new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered)) {
                var context = new OperatorContext(fixture.environment(), tempDir, true,
                                    ClientEnvironment.capture(fixture.environment()),
                                    () -> new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered), null);
                var console = new PmConsole(context);
                var t1 = new Thread(() -> console.refreshSetupBadge("en", terminal));
                var t2 = new Thread(() -> console.refreshBadges(terminal));
                t1.start();
                assertTrue(firstEntered.await(5, TimeUnit.SECONDS), "the first refresh must be in flight");
                t2.start();

                // While the first refresh holds the lock and its read is open, the second must not
                // reach the runtime: a second status request in this window is the interleaving.
                assertTrue(awaitNoSecondStatusRequest(statusRequests),
                                    () -> "the second refresh read the runtime while the first held the lock: "
                                                        + statusRequests.get());

                releaseFirst.countDown();
                t1.join(30_000);
                t2.join(30_000);
            }
        } finally {
            releaseFirst.countDown();
        }
    }

    private static boolean awaitNoSecondStatusRequest(AtomicInteger statusRequests) {
        var deadline = System.nanoTime() + Duration.ofMillis(500).toNanos();
        while (System.nanoTime() < deadline) {
            if (statusRequests.get() > 1) {
                return false;
            }
            LockSupport.parkNanos(Duration.ofMillis(10).toNanos());
        }
        return true;
    }

    @Test
    @DisplayName("the refresh path and the intro line follow an aspect list of another size")
    void refreshPathAndIntroFollowAnotherAspectList() throws Exception {
        var aspects = new ArrayList<>(List.of(OnboardingAspect.values()));
        aspects.add(OnboardingAspect.LANGUAGE);
        assertEquals(8, aspects.size());
        fixture.start((request, response) -> {
            if ("/api/v1/status".equals(request.target())) {
                response.json(200, "{\"status\":\"ready\",\"onboarded\":false,\"onboard\":{\"step\":3}}");
            } else if ("/api/v1/project-onboardings".equals(request.target())) {
                response.json(200, "{\"onboardings\":[]}");
            } else {
                response.send(404, null, "");
            }
        });
        var rendered = new ByteArrayOutputStream();
        try (var terminal = new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered)) {
            var context = new OperatorContext(fixture.environment(), tempDir, true,
                                ClientEnvironment.capture(fixture.environment()),
                                () -> new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered), null);
            var console = new PmConsole(context, new EntryFlow(context, aspects));

            console.refreshSetupBadge("en", terminal);

            assertEquals(List.of("setup 3/8"), console.currentBadges(),
                                "the refresh path must take the machine step count from the flow it runs");
        }

        fixture.close();
        fixture = RuntimeFixture.create();
        fixture.start((request, response) -> {
            if ("/api/v1/status".equals(request.target())) {
                response.json(200, "{\"status\":\"ready\",\"onboarded\":false}");
            } else if ("/api/v1/project-onboardings".equals(request.target())) {
                response.json(200, "{\"onboardings\":[]}");
            } else {
                response.send(404, null, "");
            }
        });
        var intro = new StringWriter();
        var introContext = new OperatorContext(fixture.environment(), tempDir, true);
        new EntryFlow(introContext, aspects).run(new PrintWriter(intro), createReader("q\n"));
        assertTrue(intro.toString().contains("8 steps."),
                            "the intro line must say the run's step count");
    }

    private LineReader createReader(String input) throws Exception {
        var in = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        return LineReaderBuilder.builder().terminal(new DumbTerminal(in, new ByteArrayOutputStream())).build();
    }

    @Test
    @DisplayName("refreshBadges updates the console badges and window title")
    void refreshBadgesUpdatesBadgesAndWindowTitle() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/status".equals(request.target())) {
                response.json(200, "{\"status\":\"ready\",\"onboarded\":true,\"needs_operator\":{}}");
            } else if ("/api/v1/project-onboardings".equals(request.target())) {
                response.json(200, """
                                    {"onboardings":[{"checkout_path":"/tmp/demo-proj","current_step":"build","step_number":1,"total_steps":4}]}
                                    """);
            } else {
                response.send(404, null, "");
            }
        });
        var rendered = new ByteArrayOutputStream();
        try (var terminal = new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered)) {
            var context = new OperatorContext(fixture.environment(), tempDir, true,
                                ClientEnvironment.capture(fixture.environment()),
                                () -> new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered), null);
            var console = new PmConsole(context);
            console.refreshBadges(terminal);
            assertEquals(List.of("setup demo-proj 1/4"), console.currentBadges());
            assertTrue(rendered.toString(StandardCharsets.UTF_8)
                                                    .contains("\u001B]0;pm-operator [setup demo-proj 1/4]\u0007"),
                                "the window title must carry the refreshed badges");
        }
    }

    @Test
    @DisplayName("a project named question-fix or with 40 digits raises no warning and no exception")
    void aProjectNamedQuestionFixOrWith40DigitsRaisesNoWarningAndNoException() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/status".equals(request.target())) {
                response.json(200, "{\"status\":\"ready\",\"onboarded\":true,\"needs_operator\":{}}");
            } else if ("/api/v1/project-onboardings".equals(request.target())) {
                response.json(200, """
                                    {"onboardings":[
                                      {"checkout_path":"/tmp/question-fix",
                                       "current_step":"build","step_number":1,"total_steps":5},
                                      {"checkout_path":"/tmp/project-1234567890123456789012345678901234567890",
                                       "current_step":"build","step_number":1,"total_steps":5}
                                    ]}
                                    """);
            } else {
                response.send(404, null, "");
            }
        });
        var rendered = new ByteArrayOutputStream();
        try (var terminal = new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered)) {
            var context = new OperatorContext(fixture.environment(), tempDir, true,
                                ClientEnvironment.capture(fixture.environment()),
                                () -> new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered), null);
            var console = new PmConsole(context);
            console.refreshSetupBadge("en", terminal);

            var out = new PrintWriter(new StringWriter());
            assertDoesNotThrow(() -> console.handleControlD(out));
            assertFalse(console.hasOpenQuestions(),
                                "project named question-fix must not be classified as open question");
            assertEquals(0, console.questionCount(),
                                "questionCount must be 0 for project setups");
        }
    }

    @Test
    @DisplayName("Enter on the machine badge continues the machine onboarding")
    void enterOnTheMachineBadgeContinuesTheMachineOnboarding() throws Exception {
        var onboardingRequested = new AtomicBoolean(false);
        fixture.start((request, response) -> {
            if (request.target().startsWith("/api/v1/onboarding")) {
                onboardingRequested.set(true);
                response.send(404, null, "");
            } else if ("/api/v1/status".equals(request.target())) {
                response.json(200, "{\"status\":\"ready\",\"onboarded\":false,\"onboard\":{\"step\":3}}");
            } else if ("/api/v1/project-onboardings".equals(request.target())) {
                response.json(200, "{\"onboardings\":[]}");
            } else {
                response.send(404, null, "");
            }
        });
        var rendered = new ByteArrayOutputStream();
        try (var terminal = new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered)) {
            var reader = LineReaderBuilder.builder().terminal(terminal).build();
            var context = new OperatorContext(fixture.environment(), tempDir, true,
                                ClientEnvironment.capture(fixture.environment()),
                                () -> new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered), null);
            try (var printWriter = new PrintWriter(rendered)) {
                var console = new PmConsole(context);
                console.refreshSetupBadge("en", terminal);
                console.handleEmptyEnter(printWriter, reader, terminal);
                assertTrue(onboardingRequested.get(), "Enter must act on the machine badge");
            }
        }
    }

    @Test
    @DisplayName("Enter on a project badge continues project onboarding")
    void enterOnAProjectBadgeContinuesProjectOnboarding() throws Exception {
        var entry = new BeingAddedProject("/work/cui-http", "Own settings", 5, 10,
                            "/api/v1/entries/ent-cui/continue", "/api/v1/entries/ent-cui/abandon");
        var entrySetupRequested = new AtomicBoolean(false);
        fixture.start((request, response) -> {
            if ("/api/v1/entries/ent-cui".equals(request.target())) {
                entrySetupRequested.set(true);
                response.send(404, null, "");
            } else if ("/api/v1/status".equals(request.target())) {
                response.json(200, "{\"status\":\"ready\",\"onboarded\":true}");
            } else if ("/api/v1/project-onboardings".equals(request.target())) {
                response.json(200, "{\"onboardings\":[" + entry.toJson() + "]}");
            } else {
                response.send(404, null, "");
            }
        });
        var rendered = new ByteArrayOutputStream();
        try (var terminal = new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered)) {
            var reader = LineReaderBuilder.builder().terminal(terminal).build();
            var context = new OperatorContext(fixture.environment(), tempDir, true,
                                ClientEnvironment.capture(fixture.environment()),
                                () -> new DumbTerminal(new ByteArrayInputStream(new byte[0]), rendered), null);
            var console = new PmConsole(context);
            console.refreshSetupBadge("en", terminal);
            console.handleEmptyEnter(new PrintWriter(rendered), reader, terminal);
            assertTrue(entrySetupRequested.get(), "Enter on project badge must continue project onboarding");
        }
    }
}
