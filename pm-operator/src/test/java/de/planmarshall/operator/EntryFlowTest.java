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
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.impl.DumbTerminal;

import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("EntryFlow")
class EntryFlowTest {

    @TempDir
    Path tempDir;

    private RuntimeFixture fixture;

    @BeforeEach
    void setUp() throws IOException {
        fixture = RuntimeFixture.create();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (fixture != null) {
            fixture.close();
        }
    }

    @Nested
    @DisplayName("static helper methods")
    class StaticHelpers {

        @Test
        @DisplayName("isMachineOnboarded recognizes onboarded flags across shapes")
        void recognizesOnboarded() {
            assertAll(
                                () -> assertTrue(EntryFlow.isMachineOnboarded(Map.of("onboarded", true))),
                                () -> assertTrue(EntryFlow.isMachineOnboarded(Map.of("machine", Map.of("onboarded", true)))),
                                () -> assertTrue(EntryFlow.isMachineOnboarded(Map.of("onboard", Map.of("state", "onboard.completed")))),
                                () -> assertTrue(EntryFlow.isMachineOnboarded(Map.of("onboard", Map.of("completed", true)))),
                                () -> assertFalse(EntryFlow.isMachineOnboarded(Map.of())),
                                () -> assertFalse(EntryFlow.isMachineOnboarded(Map.of("onboarded", false)))
            );
        }

        @Test
        @DisplayName("getCompletedOnboardStep extracts step numbers correctly")
        void extractsCompletedSteps() {
            assertAll(
                                () -> assertEquals(3, EntryFlow.getCompletedOnboardStep(Map.of("onboard", Map.of("step", 3)))),
                                () -> assertEquals(5, EntryFlow.getCompletedOnboardStep(Map.of("onboard", Map.of("completed_steps", 5)))),
                                () -> assertEquals(0, EntryFlow.getCompletedOnboardStep(Map.of()))
            );
        }

        @Test
        @DisplayName("getStepName returns normative names for steps 1 to 7")
        void stepNames() {
            assertAll(
                                () -> assertEquals("Language", EntryFlow.getStepName(1)),
                                () -> assertEquals("Harnesses", EntryFlow.getStepName(2)),
                                () -> assertEquals("Models", EntryFlow.getStepName(3)),
                                () -> assertEquals("Roles", EntryFlow.getStepName(4)),
                                () -> assertEquals("Credentials", EntryFlow.getStepName(5)),
                                () -> assertEquals("Providers", EntryFlow.getStepName(6)),
                                () -> assertEquals("Plans", EntryFlow.getStepName(7)),
                                () -> assertEquals("Step 8", EntryFlow.getStepName(8))
            );
        }

        @Test
        @DisplayName("getWebPort extracts port or defaults to 7420")
        void webPortResolution() {
            assertAll(
                                () -> assertEquals(7420, EntryFlow.getWebPort(Map.of())),
                                () -> assertEquals(7420, EntryFlow.getWebPort(Map.of("web", Map.of()))),
                                () -> assertEquals(8080, EntryFlow.getWebPort(Map.of("web", Map.of("port", 8080))))
            );
        }

        @Test
        @DisplayName("extractWaitingBadges formats badges from needs_operator map")
        void extractsBadges() {
            var status = Map.<String, Object>of(
                                "needs_operator", Map.of(
                                "decision", 2,
                                "waiver_request", 1
            )
            );
            var badges = EntryFlow.extractWaitingBadges(status);
            assertAll(
                                () -> assertTrue(badges.contains("2 questions")),
                                () -> assertTrue(badges.contains("1 waiver request"))
            );
        }
    }

    @Nested
    @DisplayName("entry flow execution")
    class ExecutionFlow {

        @Test
        @DisplayName("desktop onboarded with paired browser shows paired URL")
        void desktopOnboardedPaired() throws Exception {
            fixture.start((request, response) -> {
                if ("/api/v1/status".equals(request.target())) {
                    response.json(200, """
                                        {"status":"ready","onboarded":true,"version":"0.9.0","web":{"enabled":true,"port":7420}}
                                        """);
                } else if ("/api/v1/web/devices".equals(request.target())) {
                    response.json(200, """
                                        [{"device_id":"d1","label":"Firefox on mbp-oliver","paired_from":"loopback","paired_by":"open"}]
                                        """);
                } else {
                    response.send(404, null, "");
                }
            });
            PmOperatorTest.writeLiveRecord(fixture);

            var context = new OperatorContext(fixture.environment(), tempDir, true);
            var writer = new StringWriter();
            var out = new PrintWriter(writer);
            var reader = createReader("");

            var result = new EntryFlow(context).run(out, reader);

            var text = writer.toString();
            assertAll(
                                () -> assertTrue(text.contains("✓ Runtime running")),
                                () -> assertTrue(text.contains("✓ Web app: http://127.0.0.1:7420   (paired: Firefox on mbp-oliver)")),
                                () -> assertEquals("http://127.0.0.1:7420", result.activeUrl())
            );
        }

        @Test
        @DisplayName("the reports arrival notice fills its {time} placeholder")
        void reportsArrivalNoticeFormatsTime() throws Exception {
            fixture.start((request, response) -> {
                if ("/api/v1/status".equals(request.target())) {
                    response.json(200, """
                                        {"status":"ready","onboarded":true,"version":"0.9.0","web":{"enabled":true,"port":7420}}
                                        """);
                } else if ("/api/v1/web/devices".equals(request.target())) {
                    response.json(200, """
                                        [{"device_id":"d1","label":"Firefox on mbp-oliver","paired_from":"loopback","paired_by":"open"}]
                                        """);
                } else if ("/api/v1/reports".equals(request.target())) {
                    response.json(200, """
                                        {"items":[],"unread_count":2,"oldest_unread_time":"2026-10-14T10:00:00Z",
                                         "has_older":false,"older_count":0}
                                        """);
                } else {
                    response.send(404, null, "");
                }
            });
            PmOperatorTest.writeLiveRecord(fixture);

            var context = new OperatorContext(fixture.environment(), tempDir, true);
            var writer = new StringWriter();
            var out = new PrintWriter(writer);

            new EntryFlow(context).run(out, createReader(""));

            var text = writer.toString();
            assertAll(
                                () -> assertTrue(text.contains("2 unread reports since 2026-10-14T10:00:00Z"), text),
                                () -> assertFalse(text.contains("{time}"), text)
            );
        }

        @Test
        @DisplayName("desktop onboarded without paired browser issues code and opens browser")
        void desktopOnboardedUnpairedOpensBrowser() throws Exception {
            var openedUri = new AtomicReference<URI>();
            fixture.start((request, response) -> {
                if ("/api/v1/status".equals(request.target())) {
                    response.json(200, """
                                        {"status":"ready","onboarded":true,"version":"0.9.0","web":{"enabled":false,"port":7420}}
                                        """);
                } else if ("/api/v1/web".equals(request.target()) && "PUT".equals(request.method())) {
                    response.json(200, "{\"enabled\":true,\"port\":7420}");
                } else if ("/api/v1/web/devices".equals(request.target())) {
                    response.json(200, "[]");
                } else if ("/api/v1/web/pairings".equals(request.target()) && "POST".equals(request.method())) {
                    response.json(201, "{\"code\":\"KXQB-MRTN\",\"url\":\"http://127.0.0.1:7420/pair#code=KXQB-MRTN\"}");
                } else {
                    response.send(404, null, "");
                }
            });

            var context = new OperatorContext(fixture.environment(), tempDir, true,
                                ClientEnvironment.capture(fixture.environment()), null, openedUri::set);

            var writer = new StringWriter();
            var out = new PrintWriter(writer);
            var reader = createReader("");

            var result = new EntryFlow(context).run(out, reader);

            var text = writer.toString();
            assertAll(
                                () -> assertTrue(text.contains("✓ Browser opened with a pairing code, valid for 10 minutes")),
                                () -> assertTrue(text.contains("URL    http://127.0.0.1:7420/pair#code=KXQB-MRTN")),
                                () -> assertTrue(text.contains("Code   KXQB-MRTN")),
                                () -> assertEquals(URI.create("http://127.0.0.1:7420/pair#code=KXQB-MRTN"), openedUri.get()),
                                () -> assertEquals("KXQB-MRTN", result.activeCode())
            );
        }

        @Test
        @DisplayName("desktop onboarded when browser open fails shows forwarding directions")
        void desktopOnboardedBrowserOpenFails() throws Exception {
            fixture.start((request, response) -> {
                if ("/api/v1/status".equals(request.target())) {
                    response.json(200, """
                                        {"status":"ready","onboarded":true,"version":"0.9.0","web":{"enabled":true,"port":7420}}
                                        """);
                } else if ("/api/v1/web/devices".equals(request.target())) {
                    response.json(200, "[]");
                } else if ("/api/v1/web/pairings".equals(request.target())) {
                    response.json(201, "{\"code\":\"KXQB-MRTN\",\"url\":\"http://127.0.0.1:7420/pair#code=KXQB-MRTN\"}");
                } else {
                    response.send(404, null, "");
                }
            });

            var context = new OperatorContext(fixture.environment(), tempDir, true,
                                ClientEnvironment.capture(fixture.environment()), null,
                                _ -> {
                                    throw new IOException("Display not found");
                                });

            var writer = new StringWriter();
            var out = new PrintWriter(writer);
            var reader = createReader("");

            new EntryFlow(context).run(out, reader);

            var text = writer.toString();
            assertAll(
                                () -> assertTrue(text.contains("– No browser opened: Display not found")),
                                () -> assertTrue(text.contains("Ways in, from your own computer")),
                                () -> assertTrue(text.contains("ssh -L 7420:127.0.0.1:7420"))
            );
        }

        @Test
        @DisplayName("SSH session onboarded shows SSH forwarding instructions")
        void sshOnboardedForwarding() throws Exception {
            var env = new HashMap<>(fixture.environment());
            env.put("SSH_CONNECTION", "192.168.1.1 50000 192.168.1.2 22");

            fixture.start((request, response) -> {
                if ("/api/v1/status".equals(request.target())) {
                    response.json(200, """
                                        {"status":"ready","onboarded":true,"version":"0.9.0","web":{"enabled":true,"port":7420}}
                                        """);
                } else if ("/api/v1/web/pairings".equals(request.target())) {
                    response.json(201, "{\"code\":\"SSH-CODE1\",\"url\":\"http://127.0.0.1:7420/pair#code=SSH-CODE1\"}");
                } else {
                    response.send(404, null, "");
                }
            });

            var context = new OperatorContext(env, tempDir, true);
            var writer = new StringWriter();
            var out = new PrintWriter(writer);
            var reader = createReader("");

            var result = new EntryFlow(context).run(out, reader);

            var text = writer.toString();
            assertAll(
                                () -> assertTrue(text.contains("– SSH session: no browser opened here")),
                                () -> assertTrue(text.contains("ssh -L 7420:127.0.0.1:7420")),
                                () -> assertTrue(text.contains("Code SSH-CODE1")),
                                () -> assertEquals("SSH-CODE1", result.activeCode())
            );
        }

        @Test
        @DisplayName("unonboarded step 0 with choice 'w' sets up web app pairing")
        void unonboardedChoiceWeb() throws Exception {
            fixture.start((request, response) -> {
                if ("/api/v1/status".equals(request.target())) {
                    response.json(200, """
                                        {"status":"onboarding","onboarded":false,"version":"0.9.0","onboard":{"step":0},"web":{"enabled":true,"port":7420}}
                                        """);
                } else if ("/api/v1/web/pairings".equals(request.target())) {
                    response.json(201, "{\"code\":\"ONB-CODE1\",\"url\":\"http://127.0.0.1:7420/pair#code=ONB-CODE1\"}");
                } else if ("/api/v1/web".equals(request.target())) {
                    response.json(200, "{\"enabled\":true}");
                } else {
                    response.send(404, null, "");
                }
            });

            var openedUri = new AtomicReference<URI>();
            var context = new OperatorContext(fixture.environment(), tempDir, true,
                                ClientEnvironment.capture(fixture.environment()), null, openedUri::set);

            var writer = new StringWriter();
            var out = new PrintWriter(writer);
            var reader = createReader("w\n");

            var result = new EntryFlow(context).run(out, reader);

            var text = writer.toString();
            assertAll(
                                () -> assertTrue(text.contains("Seven steps.")),
                                () -> assertTrue(text.contains("The web app opens at the setup of this machine.")),
                                () -> assertEquals(List.of("setup 0/7"), result.initialBadges()),
                                () -> assertEquals("ONB-CODE1", result.activeCode())
            );
        }

        @Test
        @DisplayName("unonboarded step 0 with choice 'h' hands over to the dialogue, and 'q' leaves it")
        void unonboardedChoiceHereAndQuit() throws Exception {
            fixture.start((request, response) -> {
                if ("/api/v1/status".equals(request.target())) {
                    response.json(200, """
                                        {"status":"onboarding","onboarded":false,"version":"0.9.0","onboard":{"step":0},"web":{"enabled":true,"port":7420}}
                                        """);
                } else if (OnboardCommand.STATE_PATH.equals(request.target())) {
                    response.json(200, """
                                        {"state":"onboard.mode","summary":"Set up this machine for plan-marshall",
                                         "completed_steps":0,
                                         "links":{"begin":{"href":"/api/v1/onboarding/language","method":"POST","next":"language.choose","preselected":true}}}
                                        """);
                } else {
                    response.send(404, null, "");
                }
            });

            var context = new OperatorContext(fixture.environment(), tempDir, true);
            var writer = new StringWriter();
            var out = new PrintWriter(writer);
            var reader = createReader("h\nq\n");

            var result = new EntryFlow(context).run(out, reader);

            var text = writer.toString();
            assertAll(
                                () -> assertTrue(text.contains("Set up this machine for plan-marshall")),
                                () -> assertTrue(text.contains("Nothing was sent.")),
                                () -> assertEquals(List.of("setup 0/7"), result.initialBadges())
            );
        }

        @Test
        @DisplayName("partially onboarded step 2 prints stopped step notice")
        void partiallyOnboardedStep2() throws Exception {
            fixture.start((request, response) -> {
                if ("/api/v1/status".equals(request.target())) {
                    response.json(200, """
                                        {"status":"onboarding","onboarded":false,"version":"0.9.0","onboard":{"step":2},"web":{"enabled":true,"port":7420}}
                                        """);
                } else {
                    response.send(404, null, "");
                }
            });
            PmOperatorTest.writeLiveRecord(fixture);

            var context = new OperatorContext(fixture.environment(), tempDir, true);
            var writer = new StringWriter();
            var out = new PrintWriter(writer);
            var reader = createReader("h\nq\n");

            var result = new EntryFlow(context).run(out, reader);

            var text = writer.toString();
            assertAll(
                                () -> assertTrue(text.contains("This machine is set up up to step 2 of 7; it")),
                                () -> assertTrue(text.contains("stopped at [3/7] Models.")),
                                () -> assertEquals(List.of("setup 2/7"), result.initialBadges())
            );
        }
    }

    private LineReader createReader(String input) throws IOException {
        var inStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        var outStream = new ByteArrayOutputStream();
        var terminal = new DumbTerminal(inStream, outStream);
        return LineReaderBuilder.builder().terminal(terminal).build();
    }
}
