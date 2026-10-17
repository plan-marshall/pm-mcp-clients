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
import java.nio.file.Files;
import java.util.List;
import org.jline.terminal.impl.DumbTerminal;

import de.planmarshall.api.source.lesson.LessonClaimResult;
import de.planmarshall.api.source.lesson.LessonPremiseCheckResult;
import de.planmarshall.api.source.lesson.LessonPremiseEvidence;
import de.planmarshall.api.source.lesson.LessonRetirementNotice;
import de.planmarshall.api.source.lesson.PremiseEvidenceKind;
import de.planmarshall.api.testsupport.FakeRuntimeServer;
import de.planmarshall.api.testsupport.RuntimeFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

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

    private int runWithInput(String input, String... args) {
        var terminal = dumbTerminal(input);
        var clientEnv = ClientEnvironment.capture(fixture.environment());
        var context = new OperatorContext(fixture.environment(), fixture.base().getParent(), true, clientEnv,
                            () -> terminal, null);
        return PmOperator.execute(context, new PrintWriter(out), new PrintWriter(err), args);
    }

    private static DumbTerminal dumbTerminal(String input) {
        try {
            return new DumbTerminal(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                                new ByteArrayOutputStream());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private FakeRuntimeServer runtime(FakeRuntimeServer.Handler handler) throws IOException {
        var self = ProcessHandle.current();
        Files.createDirectories(fixture.paths().stateDir());
        Files.writeString(fixture.paths().runtimeRecord(), "{\"pid\":" + self.pid() + ",\"start_instant\":\""
                            + self.info().startInstant().orElseThrow() + "\"}");
        return fixture.start(handler);
    }

    private FakeRuntimeServer lessonServer() throws IOException {
        return runtime((request, response) -> {
            var target = request.target();
            if (target.endsWith("les-0007/premise-check")) {
                response.json(200, HOLDS);
            } else if (target.endsWith("les-0009/premise-check")) {
                response.json(200, BROKEN);
            } else if (target.endsWith("les-0007/claim")) {
                response.json(200, new LessonClaimResult("les-0007", LessonClaimResult.STATUS_CLAIMED,
                                    "lesson-les-0007").toJson());
            } else if (target.endsWith("les-0009/claim")) {
                response.json(200, new LessonClaimResult("les-0009", LessonClaimResult.STATUS_CLAIMED,
                                    "lesson-les-0009").toJson());
            } else if (target.endsWith("les-0009/retire")) {
                response.json(200, new LessonRetirementNotice("les-0009", true, "premise broken").toJson());
            } else if (target.endsWith("/plans/start")) {
                response.json(201, "{\"id\":\"lesson-plan\",\"kind\":\"plan\",\"title\":\"From a lesson\","
                                    + "\"state\":\"started\",\"follow_href\":\"/api/v1/plans/lesson-plan\"}");
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
    @DisplayName("start plan --lesson claims a holding lesson and then starts the plan")
    void startPlanFromHoldingLesson() throws Exception {
        var server = lessonServer();

        assertEquals(0, run("start", "plan", PROJECT, "--lesson", "les-0007", "--title", "From a lesson"));
        var output = out.toString();
        assertTrue(output.contains("les-0007 is claimed by lesson-les-0007"), output);
        assertTrue(output.contains("lesson-plan has started"), output);

        var targets = server.requests().stream().map(FakeRuntimeServer.Request::target).toList();
        assertTrue(targets.get(0).endsWith("les-0007/premise-check"), targets.toString());
        assertTrue(targets.get(1).endsWith("les-0007/claim"), targets.toString());
        assertTrue(targets.get(2).endsWith("/plans/start"), targets.toString());
    }

    @Test
    @DisplayName("start plan --lesson --use-anyway carries the broken premise into the plan")
    void startPlanFromBrokenLessonWithOverride() throws Exception {
        var server = lessonServer();

        assertEquals(0, run("start", "plan", PROJECT, "--lesson", "les-0009", "--use-anyway",
                            "--title", "From a lesson"));
        assertTrue(out.toString().contains("les-0009 is claimed"), out.toString());

        var claim = server.requests().stream()
                            .filter(request -> request.target().endsWith("les-0009/claim"))
                            .findFirst().orElseThrow();
        assertTrue(claim.bodyText().contains("\"use_anyway\":true"), claim.bodyText());
    }

    @Test
    @DisplayName("start plan --lesson asks for the three-way recovery and claims when the operator uses it anyway")
    void startPlanFromBrokenLessonUseAnywayChoice() throws Exception {
        var server = lessonServer();

        assertEquals(0, runWithInput("1\n", "start", "plan", PROJECT, "--lesson", "les-0009",
                            "--title", "From a lesson"));
        assertTrue(out.toString().contains("What should happen with les-0009?"), out.toString());
        assertTrue(out.toString().contains("les-0009 is claimed"), out.toString());

        var claim = server.requests().stream()
                            .filter(request -> request.target().endsWith("les-0009/claim"))
                            .findFirst().orElseThrow();
        assertTrue(claim.bodyText().contains("\"use_anyway\":true"), claim.bodyText());
    }

    @Test
    @DisplayName("start plan --lesson retires the lesson when the operator chooses retirement")
    void startPlanFromBrokenLessonRetire() throws Exception {
        var server = lessonServer();

        assertEquals(0, runWithInput("2\n", "start", "plan", PROJECT, "--lesson", "les-0009"));
        assertTrue(out.toString().contains("les-0009 is retired"), out.toString());

        var targets = server.requests().stream().map(FakeRuntimeServer.Request::target).toList();
        assertTrue(targets.stream().anyMatch(target -> target.endsWith("les-0009/retire")), targets.toString());
        assertTrue(targets.stream().noneMatch(target -> target.endsWith("/plans/start")), targets.toString());
    }

    @Test
    @DisplayName("start plan --lesson leaves the lesson untouched when the operator declines")
    void startPlanFromBrokenLessonRemove() throws Exception {
        var server = lessonServer();

        assertEquals(0, runWithInput("3\n", "start", "plan", PROJECT, "--lesson", "les-0009"));
        assertTrue(out.toString().contains("stays unclaimed"), out.toString());

        var targets = server.requests().stream().map(FakeRuntimeServer.Request::target).toList();
        assertTrue(targets.stream().noneMatch(target -> target.endsWith("les-0009/claim")), targets.toString());
        assertTrue(targets.stream().noneMatch(target -> target.endsWith("/plans/start")), targets.toString());
    }
}
