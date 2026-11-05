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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.impl.DumbTerminal;

import de.planmarshall.api.message.MessageCatalogue;
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
        fixture.start((request, response) -> {
            if ("/api/v1/status".equals(request.target())) {
                try {
                    new CountDownLatch(1).await(4, TimeUnit.SECONDS);
                } catch (InterruptedException _) {
                    Thread.currentThread().interrupt();
                }
                response.json(200, "{\"status\":\"ready\",\"onboarded\":true}");
            } else if ("/api/v1/project-onboardings".equals(request.target())) {
                try {
                    new CountDownLatch(1).await(4, TimeUnit.SECONDS);
                } catch (InterruptedException _) {
                    Thread.currentThread().interrupt();
                }
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
            var console = new PmConsole(context);
            assertTimeoutPreemptively(Duration.ofSeconds(6), () -> console.refreshSetupBadge("en", terminal));
        }
    }

    @Test
    @DisplayName("Enter on a project badge addresses the project id returned by the daemon")
    void enterOnAProjectBadgeAddressesDaemonProjectId() throws Exception {
        var exactSetupTarget = new AtomicReference<String>();
        fixture.start((request, response) -> {
            if (request.target().startsWith("/api/v1/projects/") && request.target().endsWith("/setup")) {
                exactSetupTarget.set(request.target());
                response.send(404, null, "");
            } else if ("/api/v1/status".equals(request.target())) {
                response.json(200, "{\"status\":\"ready\",\"onboarded\":true}");
            } else if ("/api/v1/project-onboardings".equals(request.target())) {
                response.json(200, """
                                    {"onboardings":[{"project_id":"custom-daemon-id",
                                                     "checkout_path":"/work/other-dir-name",
                                                     "current_step":"Identity",
                                                     "step_number":3,"total_steps":10}]}
                                    """);
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
            assertEquals("/api/v1/projects/custom-daemon-id/setup", exactSetupTarget.get(),
                                "Enter on project badge must target daemon project_id, not checkout directory name");
        }
    }

    @Test
    @DisplayName("a stalled runtime lets the prompt return within the bound")
    void aStalledRuntimeLetsThePromptReturnWithinTheBound() throws Exception {
        fixture.start((request, response) -> {
            if ("/api/v1/status".equals(request.target())) {
                response.json(200, "{\"status\":\"ready\",\"onboarded\":true}");
            } else if ("/api/v1/project-onboardings".equals(request.target())) {
                try {
                    new CountDownLatch(1).await(10, TimeUnit.SECONDS);
                } catch (InterruptedException _) {
                    Thread.currentThread().interrupt();
                }
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
            var console = new PmConsole(context);
            assertTimeoutPreemptively(Duration.ofSeconds(8), () -> console.refreshSetupBadge("en", terminal));
        }
    }

    @Test
    @DisplayName("two concurrent refreshes never interleave")
    void twoConcurrentRefreshesNeverInterleave() throws Exception {
        var inFlight = new AtomicInteger();
        var maxConcurrent = new AtomicInteger();
        fixture.start((request, response) -> {
            var current = inFlight.incrementAndGet();
            maxConcurrent.accumulateAndGet(current, Math::max);
            try {
                new CountDownLatch(1).await(100, TimeUnit.MILLISECONDS);
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
            } finally {
                inFlight.decrementAndGet();
            }
            if ("/api/v1/status".equals(request.target())) {
                response.json(200, "{\"status\":\"ready\",\"onboarded\":true}");
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
            var console = new PmConsole(context);
            var t1 = new Thread(() -> console.refreshSetupBadge("en", terminal));
            var t2 = new Thread(() -> console.refreshBadges(terminal));
            t1.start();
            t2.start();
            t1.join();
            t2.join();
        }
        assertEquals(1, maxConcurrent.get(),
                            "refreshSetupBadge and refreshBadges must be serialized by badgeRefreshLock");
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
    @DisplayName("a test changes the badge template in the catalogue and Enter still acts")
    void aTestChangesTheBadgeTemplateInTheCatalogueAndEnterStillActs() throws Exception {
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
            MessageCatalogue.setForTest("en", "operator.console.badge.setup", "Installation {done}/{total}");
            try (var printWriter = new PrintWriter(rendered)) {
                var console = new PmConsole(context);
                console.refreshSetupBadge("en", terminal);
                console.handleEmptyEnter(printWriter, reader, terminal);
                assertTrue(onboardingRequested.get(), "Enter must still act when machine setup badge template is changed in catalogue");
            } finally {
                MessageCatalogue.clearCacheForTest();
            }
        }
    }

    @Test
    @DisplayName("Enter on a project badge continues project onboarding")
    void enterOnAProjectBadgeContinuesProjectOnboarding() throws Exception {
        var projectSetupRequested = new AtomicBoolean(false);
        fixture.start((request, response) -> {
            if ("/api/v1/projects/cui-http/setup".equals(request.target())) {
                projectSetupRequested.set(true);
                response.send(404, null, "");
            } else if ("/api/v1/status".equals(request.target())) {
                response.json(200, "{\"status\":\"ready\",\"onboarded\":true}");
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
            var console = new PmConsole(context, List.of(ConsoleBadge.projectSetup("cui-http", "setup cui-http 5/10")));
            console.handleEmptyEnter(new PrintWriter(rendered), reader, terminal);
            assertTrue(projectSetupRequested.get(), "Enter on project badge must continue project onboarding");
        }
    }
}
