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
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.jline.terminal.impl.DumbTerminal;

import de.planmarshall.api.MachineApiPaths;
import de.planmarshall.api.MachinePaths;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonNumber;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.overview.NeedsOperatorKind;
import de.planmarshall.api.overview.NeedsOperatorPage;
import de.planmarshall.api.overview.ProjectOverviewResponse;
import de.planmarshall.mock.MockRuntime;
import de.planmarshall.mock.MockRuntimeConfig;
import de.planmarshall.mock.StandardScenarios;
import de.planmarshall.operator.overview.OverviewFormatting;
import de.planmarshall.operator.overview.ProjectsOverviewLevel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

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
    @DisplayName("asserts the same counts per kind across needs, overview, status, questions, and badges")
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

            // The one list: every surface is compared against it, per kind, not as a total.
            var needsRes = context.runtime(true).send(HttpRequest.get(MachineApiPaths.NEEDS_OPERATOR));
            assertEquals(200, needsRes.status());
            var page = NeedsOperatorPage.fromJson(needsRes.bodyText());
            var perKind = new TreeMap<String, Integer>();
            for (var item : page.items()) {
                perKind.merge(item.kind().value(), 1, Integer::sum);
            }
            assertEquals(18, page.total(), "the list's own total");

            // Surface 1: 'needs'
            var outNeeds = new StringWriter();
            var errNeeds = new StringWriter();
            assertEquals(0, PmOperator.execute(context, new PrintWriter(outNeeds), new PrintWriter(errNeeds), "needs"));
            var needsOutput = outNeeds.toString();
            assertTrue(needsOutput.contains("Needs you · 18"), "needs should report 18 items: " + needsOutput);
            assertEquals(perKind, kindCountsInList(needsOutput, page),
                                "every kind appears in the list as often as the list says: " + needsOutput);

            // Surface 2: the overview's "Needs you" group
            var projRes = context.runtime(true).send(HttpRequest.get("/api/v1/projects"));
            var projects = ProjectOverviewResponse.fromJson(projRes.bodyText()).projects();
            var overviewRows = new ProjectsOverviewLevel(projects, page).rows("en", context.clock());
            assertEquals(ProjectsOverviewLevel.GROUP_NEEDS_YOU_ID, overviewRows.getFirst().id());
            assertTrue(overviewRows.getFirst().line().contains("Needs you · 18"), overviewRows.getFirst().line());
            assertEquals(page.items().size(),
                                overviewRows.stream().filter(row -> row.id().startsWith(ProjectsOverviewLevel.NEEDS_ITEM_PREFIX)).count(),
                                "the group carries one row per item");

            // Surface 3: 'status --json'
            var outStatus = new StringWriter();
            assertEquals(0, PmOperator.execute(context, new PrintWriter(outStatus), new PrintWriter(new StringWriter()),
                                "status", "--json"));
            var statusMap = JsonTree.asObject(JsonTree.parse(outStatus.toString()));
            assertNotNull(statusMap);
            var statusNeeds = JsonTree.asObject(statusMap.get("needs_operator"));
            assertNotNull(statusNeeds, "status must carry needs_operator");
            assertEquals(perKind, kindCountsOfMap(statusNeeds), "status counts every kind as the list does");

            // Surface 4: 'questions'
            var outQuestions = new StringWriter();
            assertEquals(0, PmOperator.execute(context, new PrintWriter(outQuestions), new PrintWriter(new StringWriter()),
                                "questions"));
            var expectedAsks = perKind.getOrDefault("decision", 0) + perKind.getOrDefault("waiver", 0);
            assertTrue(outQuestions.toString().contains("Open Questions · " + expectedAsks + " pending"),
                                "questions counts the decisions and waivers of the list: " + outQuestions);

            // Surface 5: the prompt badges
            var badges = EntryFlow.extractWaitingConsoleBadges("en", statusMap);
            var badgeKinds = new TreeMap<String, Integer>();
            for (var badge : badges) {
                if (badge.itemKind() != null) {
                    badgeKinds.merge(badge.itemKind(), badge.count(), Integer::sum);
                }
            }
            assertEquals(perKind, badgeKinds, "the badges count every kind as the list does");
        }
    }

    @Test
    @DisplayName("the badges read needs_operator, one per kind of the list")
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
                                () -> assertEquals(2, badges.stream()
                                                                        .filter(badge -> "waiver".equals(badge.itemKind()))
                                                                        .mapToInt(ConsoleBadge::count).sum(),
                                                    "the badges count the list's 2 waivers: " + badgeTexts),
                                () -> assertTrue(badgeTexts.contains("1 credential"), "badges should have 1 credential: " + badgeTexts),
                                () -> assertTrue(badgeTexts.contains("1 cleanup due"), "badges should have 1 cleanup due: " + badgeTexts),
                                () -> assertTrue(badgeTexts.contains("1 integrity pause"), "badges should have 1 integrity pause: " + badgeTexts),
                                () -> assertTrue(badgeTexts.contains("1 held"), "badges should have 1 held: " + badgeTexts),
                                () -> assertEquals(12, badgeTexts.size(),
                                                    "one badge per kind of the list, and none invented: " + badgeTexts)
            );

            int badgesTotal = badges.stream().mapToInt(ConsoleBadge::count).sum();
            assertEquals(18, badgesTotal, "sum of badge counts must equal 18");
        }
    }

    @ParameterizedTest(name = "{0} raises one notice for the item it adds")
    @EnumSource(value = NeedsOperatorKind.class,
                        names = {"CREDENTIAL", "CLEANUP_DUE", "SETUP", "FINDINGS"})
    @DisplayName("every kind no other event announces is announced by the change")
    void everyUnannouncedKindRaisesOneNotice(NeedsOperatorKind kind) {
        var differ = new NeedsOperatorNotices();
        var link = "/api/v1/items/" + Generators.nonEmptyStrings().next();
        differ.noticesOf("en", "needs_operator.changed", Map.of("kind", "none", "count", 0));
        var notices = differ.noticesOf("en", "needs_operator.changed",
                            Map.of("kind", "none", "count", 1,
                                                "needs_operator", Map.of("count", 1,
                                                                    "items", List.of(Map.of("kind", kind.value(),
                                                                                        "link", link)))));
        assertEquals(1, notices.size(), kind + " must be announced once: " + notices);
        assertEquals(kind.value(), notices.getFirst().kind());
        assertFalse(notices.getFirst().text().isBlank());
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

    /**
     * Counts the kinds the rendered {@code needs} list names, one badge cell per row, so a kind
     * that moved its rows into another kind's cell is visible and a matching total is not.
     *
     * @param output the command's output
     * @param page   the page the list was rendered from
     * @return the count per kind, as the rendered badge cells carry them
     */
    private static TreeMap<String, Integer> kindCountsInList(String output, NeedsOperatorPage page) {
        var counts = new TreeMap<String, Integer>();
        for (var kind : page.items().stream().map(item -> item.kind()).distinct().toList()) {
            var label = "! " + OverviewFormatting.formatNeedsItemKind(kind, "en");
            var rows = output.lines().filter(line -> line.stripLeading().startsWith(label)).count();
            counts.put(kind.value(), (int) rows);
        }
        return counts;
    }

    private static TreeMap<String, Integer> kindCountsOfMap(Map<String, Object> needs) {
        var counts = new TreeMap<String, Integer>();
        for (var entry : needs.entrySet()) {
            var count = intVal(entry.getValue());
            if (count > 0) {
                counts.put(entry.getKey(), count);
            }
        }
        return counts;
    }
}
