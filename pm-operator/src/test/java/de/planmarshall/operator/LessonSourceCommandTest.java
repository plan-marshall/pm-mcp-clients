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

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;

import de.planmarshall.api.source.lesson.LessonPremiseCheckResult;
import de.planmarshall.api.source.lesson.LessonPremiseEvidence;
import de.planmarshall.api.source.lesson.PremiseEvidenceKind;
import de.planmarshall.api.testsupport.FakeRuntimeServer;
import de.planmarshall.api.testsupport.RuntimeFixture;
import de.planmarshall.mock.MockRuntime;
import de.planmarshall.mock.MockRuntimeConfig;
import de.planmarshall.mock.StandardScenarios;
import de.planmarshall.operator.transcript.TerminalTestDriver;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * {@code lesson check <lesson_id>}, the premise check of a lesson on its own, and the start of a
 * plan from a lesson, which is {@code plan add --lesson <lesson_id>} (UC-STA-8, 18-starting.adoc).
 */
@EnableGeneratorController
@DisplayName("LessonSourceCommand")
@EnabledOnOs({OS.MAC, OS.LINUX})
class LessonSourceCommandTest {

    private static final String PROJECT = "pm-mcp-clients";
    private static final String HOLDS = new LessonPremiseCheckResult("les-0007",
                        LessonPremiseCheckResult.STATUS_HOLDS, List.of(), null).toJson();
    private static final String BROKEN = new LessonPremiseCheckResult("les-0009",
                        LessonPremiseCheckResult.STATUS_BROKEN,
                        List.of(new LessonPremiseEvidence(PremiseEvidenceKind.FILE, "src/Old.java", "src/Old.java",
                                            null)),
                        null).toJson();

    private RuntimeFixture fixture;
    private StringWriter out;
    private StringWriter err;

    @BeforeEach
    void setUp() throws IOException {
        fixture = RuntimeFixture.create();
        out = new StringWriter();
        err = new StringWriter();
    }

    @AfterEach
    void tearDown() throws IOException {
        fixture.close();
    }

    private int run(String... args) {
        var context = new OperatorContext(fixture.environment(), fixture.base().getParent());
        return PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err), args);
    }

    private FakeRuntimeServer lessonServer() throws IOException {
        var self = ProcessHandle.current();
        Files.createDirectories(fixture.paths().stateDir());
        Files.writeString(fixture.paths().runtimeRecord(), "{\"pid\":" + self.pid() + ",\"start_instant\":\""
                            + self.info().startInstant().orElseThrow() + "\"}");
        return fixture.start((request, response) -> {
            var target = request.target();
            if (target.endsWith("les-0007/premise-check")) {
                response.json(200, HOLDS);
            } else if (target.endsWith("les-0009/premise-check")) {
                response.json(200, BROKEN);
            } else {
                response.send(404, null, "Not found: " + target);
            }
        });
    }

    @Test
    @DisplayName("lesson without subcommands prints usage")
    void lessonUsage() {
        assertEquals(0, run("lesson"));
        assertTrue(out.toString().contains("Usage: pm-operator lesson"), out.toString());
    }

    @Test
    @DisplayName("lesson check requires the lesson id")
    void lessonCheckRequiresId() {
        assertEquals(1, run("lesson", "check"));
        assertTrue(out.toString().contains("Lesson id required."), out.toString());
    }

    @Test
    @DisplayName("lesson check reports a holding premise")
    void lessonCheckHolds() throws Exception {
        lessonServer();

        assertEquals(0, run("lesson", "check", "les-0007", "--project", PROJECT));
        assertTrue(out.toString().contains("the premise holds"), out.toString());
    }

    @Test
    @DisplayName("lesson check reports the missing files and symbols of a broken premise")
    void lessonCheckBroken() throws Exception {
        lessonServer();

        assertEquals(1, run("lesson", "check", "les-0009", "--project", PROJECT));
        var output = out.toString();
        assertTrue(output.contains("the premise is broken"), output);
        assertTrue(output.contains("src/Old.java"), output);
    }

    @Test
    @DisplayName("lesson check claims nothing and starts nothing")
    void lessonCheckStartsNothing() throws Exception {
        var server = lessonServer();

        run("lesson", "check", "les-0009", "--project", PROJECT);

        var targets = server.requests().stream().map(FakeRuntimeServer.Request::target).toList();
        assertAll(
                            () -> assertTrue(targets.stream().anyMatch(target -> target.endsWith("les-0009/premise-check")),
                                                targets.toString()),
                            () -> assertTrue(targets.stream().noneMatch(target -> target.endsWith("/claim")
                                                                    || target.endsWith("/retire") || target.endsWith("/start")),
                                                targets.toString()));
    }

    @Test
    @DisplayName("plan add --lesson starts a plan from the lesson")
    void planAddFromALesson() throws Exception {
        var lesson = "L-" + Generators.integers(1, 999).next();

        try (var runtime = MockRuntime.start(MockRuntimeConfig.builder()
                                 .scenario(StandardScenarios.START_SOURCES).build());
             var driver = TerminalTestDriver.create()) {
            var self = ProcessHandle.current();
            Files.createDirectories(runtime.paths().stateDir());
            Files.writeString(runtime.paths().runtimeRecord(), "{\"pid\":" + self.pid() + ",\"start_instant\":\""
                                + self.info().startInstant().orElseThrow() + "\"}", StandardCharsets.UTF_8);
            var environment = new HashMap<>(runtime.environment());
            environment.put("LANG", "en_US.UTF-8");
            driver.project(environment, runtime.base().getParent());
            driver.endOfInput();

            var code = TranscriptVerbRunner.run(driver.context(), driver.writer(), driver.writer(),
                                "plan", "add", "--project", PROJECT, "--lesson", lesson, "--no-input");

            var rendered = driver.render();
            assertAll(
                                () -> assertEquals(0, code, rendered),
                                () -> assertTrue(rendered.contains("Lesson"), rendered),
                                () -> assertTrue(rendered.contains("has started"), rendered));
        }
    }

    @Test
    @DisplayName("start plan --lesson is no verb: it is reported with the usage exit code")
    void startPlanLessonIsUnknown() {
        assertEquals(2, run("start", "plan", PROJECT, "--lesson", "les-0007"));
    }
}
