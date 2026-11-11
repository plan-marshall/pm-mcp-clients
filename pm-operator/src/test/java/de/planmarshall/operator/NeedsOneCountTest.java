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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.cuioss.test.generator.junit.EnableGeneratorController;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.jline.terminal.impl.DumbTerminal;

import de.planmarshall.api.MachineApiPaths;
import de.planmarshall.api.MachinePaths;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonNumber;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.overview.NeedsOperatorPage;
import de.planmarshall.api.overview.ProjectOverviewResponse;
import de.planmarshall.mock.MockRuntime;
import de.planmarshall.mock.MockRuntimeConfig;
import de.planmarshall.mock.StandardScenarios;
import de.planmarshall.operator.overview.ProjectsOverviewLevel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Proves "One count" from {@code needs_operator} across the four surfaces (PLAN-36 D3):
 * Overview, {@code status}, {@code questions}, {@code needs}, and the prompt badges.
 */
@DisplayName("One count from needs_operator across four surfaces")
@EnableGeneratorController
class NeedsOneCountTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("asserts the same counts across needs, overview, status, questions, and badges on running-work")
    void sameCountsAcrossFourSurfaces() throws Exception {
        try (var runtime = MockRuntime.start(MockRuntimeConfig.builder()
                                 .webPort(0)
                                 .scenario(StandardScenarios.RUNNING_WORK)
                                 .build())) {
            var environment = Map.of(MachinePaths.ENV_BASE, runtime.base().toString());
            var context = new OperatorContext(
                                environment,
                                tempDir,
                                true,
                                ClientEnvironment.capture(environment),
                                () -> new DumbTerminal(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream()),
                                null
            );

            // 1. Surface 1: 'needs' verb
            var outNeeds = new StringWriter();
            var errNeeds = new StringWriter();
            int exitNeeds = PmOperator.execute(context, new PrintWriter(outNeeds), new PrintWriter(errNeeds), "needs");
            assertEquals(0, exitNeeds, "needs should exit 0");
            var needsOutput = outNeeds.toString();
            assertTrue(needsOutput.contains("Needs you · 12"), "needs should report 12 items: " + needsOutput);

            // 2. Surface 2: Overview root ("Needs you" group)
            var needsRes = context.runtime(true).send(HttpRequest.get(MachineApiPaths.NEEDS_OPERATOR));
            assertEquals(200, needsRes.status());
            var page = NeedsOperatorPage.fromJson(needsRes.bodyText());
            var projRes = context.runtime(true).send(HttpRequest.get("/api/v1/projects"));
            var projects = ProjectOverviewResponse.fromJson(projRes.bodyText()).projects();
            var overviewLevel = new ProjectsOverviewLevel(projects, page);
            var overviewRows = overviewLevel.rows("en", context.clock());

            assertEquals(12, page.total(), "overview needs page total should be 12");
            var needsGroupHeader = overviewRows.getFirst();
            assertEquals(ProjectsOverviewLevel.GROUP_NEEDS_YOU_ID, needsGroupHeader.id());
            assertTrue(needsGroupHeader.line().contains("Needs you · 12"),
                                "overview group header should report Needs you · 12: " + needsGroupHeader.line());

            // 3. Surface 3: 'status'
            var outStatus = new StringWriter();
            var errStatus = new StringWriter();
            int exitStatus = PmOperator.execute(context, new PrintWriter(outStatus), new PrintWriter(errStatus), "status", "--json");
            assertEquals(0, exitStatus, "status --json should exit 0");
            var statusMap = JsonTree.asObject(JsonTree.parse(outStatus.toString()));
            assertNotNull(statusMap);
            var statusNeeds = JsonTree.asObject(statusMap.get("needs_operator"));
            assertNotNull(statusNeeds, "status must carry needs_operator");

            // Sum of items in needs_operator must match total 12
            int statusTotal = statusNeeds.values().stream()
                                .mapToInt(NeedsOneCountTest::intVal)
                                .sum();
            assertEquals(12, statusTotal, "status needs_operator counts sum should be 12");

            // 4. Surface 4: 'questions'
            var outQuestions = new StringWriter();
            var errQuestions = new StringWriter();
            int exitQuestions = PmOperator.execute(context, new PrintWriter(outQuestions), new PrintWriter(errQuestions), "questions");
            assertEquals(0, exitQuestions, "questions should exit 0");
            var questionsOutput = outQuestions.toString();
            assertTrue(questionsOutput.contains("Open Questions · 8 pending"),
                                "questions should report 8 pending: " + questionsOutput);
            int decisionCount = intVal(statusNeeds.get("decision"));
            int waiverCount = intVal(statusNeeds.get("waiver_request"));
            assertEquals(8, decisionCount + waiverCount,
                                "decision + waiver_request count in status needs_operator must agree with questions count (8)");
        }
    }

    @Test
    @DisplayName("badges read needs_operator and gain credential and cleanup_due commands")
    void badgesAndCommandsReadNeedsOperator() throws Exception {
        try (var runtime = MockRuntime.start(MockRuntimeConfig.builder()
                                 .webPort(0)
                                 .scenario(StandardScenarios.RUNNING_WORK)
                                 .build())) {
            var environment = Map.of(MachinePaths.ENV_BASE, runtime.base().toString());
            var context = new OperatorContext(
                                environment,
                                tempDir,
                                true,
                                ClientEnvironment.capture(environment),
                                () -> new DumbTerminal(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream()),
                                null
            );

            var outStatus = new StringWriter();
            var errStatus = new StringWriter();
            int exitStatus = PmOperator.execute(context, new PrintWriter(outStatus), new PrintWriter(errStatus), "status", "--json");
            assertEquals(0, exitStatus, "status --json should exit 0");
            var statusMap = JsonTree.asObject(JsonTree.parse(outStatus.toString()));
            assertNotNull(statusMap);

            var badges = EntryFlow.extractWaitingConsoleBadges("en", statusMap);
            var badgeTexts = badges.stream().map(ConsoleBadge::text).toList();

            assertAll(
                                () -> assertTrue(badgeTexts.contains("6 questions"), "badges should have 6 questions: " + badgeTexts),
                                () -> assertTrue(badgeTexts.contains("2 waiver requests"), "badges should have 2 waiver requests: " + badgeTexts),
                                () -> assertTrue(badgeTexts.contains("1 credential"), "badges should have 1 credential: " + badgeTexts),
                                () -> assertTrue(badgeTexts.contains("1 cleanup due"), "badges should have 1 cleanup due: " + badgeTexts),
                                () -> assertTrue(badgeTexts.contains("1 integrity pause"), "badges should have 1 integrity pause: " + badgeTexts),
                                () -> assertTrue(badgeTexts.contains("1 held"), "badges should have 1 held: " + badgeTexts)
            );

            int badgesTotal = badges.stream().mapToInt(ConsoleBadge::count).sum();
            assertEquals(12, badgesTotal, "sum of badge counts must equal 12");
        }
    }

    @Test
    @DisplayName("pushed needs_operator.changed for the machine adds notices for the items it adds")
    void pushedNeedsOperatorChangedNotices() {
        var differ = new NeedsOperatorNotices();
        var addition = differ.noticesOf("en", "needs_operator.changed",
                            Map.of("kind", "credential", "count", 1,
                                                "needs_operator", Map.of("count", 1, "items", List.of(
                                                                    Map.of("kind", "credential",
                                                                                        "link", "/api/v1/credentials/github")))));
        assertEquals(1, addition.size());
        assertTrue(addition.getFirst().text().contains("Credential github needs attention"),
                            addition.getFirst().text());

        var cleanup = differ.noticesOf("en", "needs_operator.changed",
                            Map.of("kind", "cleanup_due", "count", 2,
                                                "needs_operator", Map.of("count", 2, "items", List.of(
                                                                    Map.of("kind", "credential",
                                                                                        "link", "/api/v1/credentials/github"),
                                                                    Map.of("kind", "cleanup_due",
                                                                                        "link", "/api/v1/system/cleanup")))));
        assertEquals(1, cleanup.size());
        assertTrue(cleanup.getFirst().text().contains("Cleanup is due"), cleanup.getFirst().text());

        // A credential without a key names the catalogue's own word, never an English literal.
        var noKey = new NeedsOperatorNotices().noticesOf("de", "needs_operator.changed",
                            Map.of("kind", "credential", "count", 1,
                                                "needs_operator", Map.of("count", 1, "items", List.of(
                                                                    Map.of("kind", "credential")))));
        assertTrue(noKey.getFirst().text().contains("unbekannt"), noKey.getFirst().text());
    }

    private static int intVal(Object v) {
        if (v instanceof Number n) {
            return n.intValue();
        }
        if (v instanceof JsonNumber jn) {
            return (int) jn.longValue();
        }
        return 0;
    }
}
