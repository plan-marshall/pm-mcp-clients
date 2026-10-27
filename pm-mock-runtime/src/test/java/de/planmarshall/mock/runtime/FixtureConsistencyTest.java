/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.mock.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.planmarshall.api.control.EpicResource;
import de.planmarshall.api.control.ReviewResource;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.mock.StandardScenarios;
import de.planmarshall.mock.http.MockHttpRequest;
import de.planmarshall.mock.http.MockHttpResponse;
import de.planmarshall.mock.scenario.ScenarioRegistry;

/**
 * The fixture consistency pairs of CC-F-09: a number the mock shows in one place is the number it
 * shows in the other. Each assertion names the two resources the app reads, so a fixture edit that
 * breaks the agreement fails here instead of on a conformance walk.
 */
@DisplayName("FixtureConsistencyTest")
class FixtureConsistencyTest {

    private static final String AUTH = "Bearer test-token";

    /** The suffix every onboarding summary follows when it names a role count. */
    private static final String ROLE_COUNT_SUFFIX = " roles";

    @Nested
    @DisplayName("The cleanup summary and its details")
    class Cleanup {

        @Test
        @DisplayName("the summary counts and the freed bytes are the sum of the project items")
        void summaryIsTheSumOfTheProjectItems() throws Exception {
            try (var engine = engine(StandardScenarios.REPORTS_AND_SYSTEM)) {
                assertCleanupAgrees(engine, "/api/v1/system/cleanup");
            }
        }

        @Test
        @DisplayName("the preview state's own job is the sum of the project items")
        void previewStateAppliesItsOwnJob() throws Exception {
            try (var engine = engine(StandardScenarios.REPORTS_AND_SYSTEM)) {
                var before = JsonTree.number(JsonTree.object(get(engine, "/api/v1/system/cleanup"), "summary"),
                        "freed_bytes").longValue();
                engine.goToState("cleanup.preview");
                var after = JsonTree.number(JsonTree.object(get(engine, "/api/v1/system/cleanup"), "summary"),
                        "freed_bytes").longValue();
                assertNotEquals(before, after, "the preview state must apply its own job");
                assertCleanupAgrees(engine, "/api/v1/system/cleanup");
            }
        }

        @Test
        @DisplayName("the delete response frees what the job's summary says")
        void deleteResponseMatchesTheSummary() throws Exception {
            try (var engine = engine(StandardScenarios.REPORTS_AND_SYSTEM)) {
                var summary = JsonTree.object(get(engine, "/api/v1/system/cleanup"), "summary");
                assertNotNull(summary);
                var deleted = JsonTree.asObject(JsonTree.parse(
                        body(engine, "POST", "/api/v1/system/cleanup/cln-0012/delete")));
                assertNotNull(deleted);
                assertEquals(JsonTree.number(summary, "freed_bytes").longValue(),
                        JsonTree.number(deleted, "freed_bytes").longValue(), "freed_bytes");
            }
        }

        @Test
        @DisplayName("the delete response leaves out an unchecked item's size and counts")
        void deleteResponseLeavesOutTheUncheckedItem() throws Exception {
            try (var engine = engine(StandardScenarios.REPORTS_AND_SYSTEM)) {
                var job = get(engine, "/api/v1/system/cleanup");
                var summary = JsonTree.object(job, "summary");
                assertNotNull(summary);
                var exclusion = itemsOf(job).stream()
                        .filter(entry -> "token-usage".equals(JsonTree.string(entry, "id")))
                        .findFirst();
                assertTrue(exclusion.isPresent(), "the fixture knows no token-usage item");
                var entry = exclusion.get();
                var body = JsonTree.write(Map.of("unchecked_ids", List.of("token-usage")))
                        .getBytes(StandardCharsets.UTF_8);
                var deleted = JsonTree.asObject(JsonTree.parse(
                        body(engine, "POST", "/api/v1/system/cleanup/cln-0012/delete", body)));
                assertNotNull(deleted);
                assertEquals(JsonTree.number(summary, "freed_bytes").longValue()
                        - JsonTree.number(entry, "size_bytes").longValue(),
                        JsonTree.number(deleted, "freed_bytes").longValue(), "freed_bytes");
            }
        }

        @Test
        @DisplayName("the system snapshot carries the same cleanup summary as the job")
        void snapshotCarriesTheSameSummary() throws Exception {
            try (var engine = engine(StandardScenarios.REPORTS_AND_SYSTEM)) {
                var job = get(engine, "/api/v1/system/cleanup");
                var summary = JsonTree.object(job, "summary");
                var snapshot = JsonTree.object(get(engine, "/api/v1/system"), "cleanup");
                assertNotNull(summary);
                assertNotNull(snapshot);
                for (var key : List.of("plans_count", "epics_count", "logs_count", "reports_count",
                        "projects_count", "freed_bytes", "kept_for_good_count")) {
                    assertEquals(summary.get(key), snapshot.get(key), key);
                }
            }
        }
    }

    @Nested
    @DisplayName("The epic's counts")
    class EpicCounts {

        @Test
        @DisplayName("the running count is the number of running rows")
        void runningIsTheNumberOfRunningRows() throws Exception {
            try (var engine = engine(StandardScenarios.RUNNING_WORK)) {
                var epic = epic(engine);
                assertEquals(rowsWithStatus(epic, "running"), epic.running(), "running");
            }
        }

        @Test
        @DisplayName("the landings count is the number of shipped rows")
        void landingsIsTheNumberOfShippedRows() throws Exception {
            try (var engine = engine(StandardScenarios.RUNNING_WORK)) {
                var epic = epic(engine);
                assertEquals(rowsWithStatus(epic, "shipped"), epic.landings(), "landings");
            }
        }

        @Test
        @DisplayName("the epic list's running count is the epic resource's running count")
        void listAgreesWithTheResource() throws Exception {
            try (var engine = engine(StandardScenarios.RUNNING_WORK)) {
                var epic = epic(engine);
                var listed = JsonTree.integer(epicListItem(engine, epic.epicId()), "running_plans_count");
                assertEquals(epic.running(), listed, "running_plans_count");
            }
        }
    }

    @Nested
    @DisplayName("The findings register")
    class Findings {

        @Test
        @DisplayName("the epic's findings counts are the register's item states")
        void countsAreTheRegisterItemStates() throws Exception {
            try (var engine = engine(StandardScenarios.RUNNING_WORK)) {
                var epic = epic(engine);
                var review = ReviewResource.fromJson(body(engine,
                        "/api/v1/epics/relay-hardening/review?register=findings"));
                var states = review.items().stream().map(item -> item.state()).toList();
                assertEquals(countOf(states, "new"), epic.findings().newCount(), "new");
                assertEquals(countOf(states, "open"), epic.findings().open(), "open");
                assertEquals(countOf(states, "kept"), epic.findings().kept(), "kept");
            }
        }
    }

    @Nested
    @DisplayName("The role counts")
    class Roles {

        @Test
        @DisplayName("the completed roles step names the number the machine settings show")
        void stepSummaryNamesTheSettingsNumber() throws Exception {
            try (var engine = engine(StandardScenarios.ONBOARDING)) {
                engine.goToState("settings.record");
                var state = get(engine, "/api/v1/onboarding/state");
                var summaries = JsonTree.object(state, "step_summaries");
                assertNotNull(summaries);
                var step = String.valueOf(summaries.get("4"));
                var settings = get(engine, "/api/v1/machine/settings");
                var roles = aspect(settings, "roles");
                assertNotNull(roles);
                assertEquals(leadingNumber(JsonTree.string(roles, "summary")), leadingNumber(step));
            }
        }

        @Test
        @DisplayName("no onboarding state names a role count the machine settings do not")
        void everyStateNamesTheSettingsNumber() throws Exception {
            try (var engine = engine(StandardScenarios.ONBOARDING)) {
                var settings = get(engine, "/api/v1/machine/settings");
                var roles = aspect(settings, "roles");
                assertNotNull(roles);
                var expected = leadingNumber(JsonTree.string(roles, "summary"));
                var named = 0;
                for (var stateId : new TreeSet<>(engine.currentScenario().states().keySet())) {
                    engine.goToState(stateId);
                    var raw = body(engine, "/api/v1/onboarding/state");
                    var at = raw.indexOf(ROLE_COUNT_SUFFIX);
                    while (at > 0) {
                        var end = at;
                        var begin = end;
                        while (begin > 0 && Character.isDigit(raw.charAt(begin - 1))) {
                            begin--;
                        }
                        if (begin < end) {
                            named++;
                            assertEquals(expected, raw.substring(begin, end),
                                    stateId + " names a role count of its own");
                        }
                        at = raw.indexOf(ROLE_COUNT_SUFFIX, at + ROLE_COUNT_SUFFIX.length());
                    }
                }
                assertTrue(named > 0, "no onboarding state names a role count at all");
            }
        }
    }

    @Nested
    @DisplayName("The overview groupings")
    class Overview {

        @Test
        @DisplayName("a project's work counts are the rows its resource lists")
        void projectCountsAreTheRowsItLists() throws Exception {
            try (var engine = engine(StandardScenarios.RUNNING_WORK)) {
                var overview = get(engine, "/api/v1/overview");
                var project = JsonTree.asObject(JsonTree.asArray(overview.get("projects")).getFirst());
                assertNotNull(project);
                var detail = get(engine, "/api/v1/projects/" + JsonTree.string(project, "project_id"));
                assertEquals(size(detail, "plans"), JsonTree.integer(project, "active_plans_count"),
                        "active_plans_count");
                assertEquals(size(detail, "epics"), JsonTree.integer(project, "active_epics_count"),
                        "active_epics_count");
            }
        }
    }

    private static void assertCleanupAgrees(MockRuntimeEngine engine, String path) throws IOException {
        var job = get(engine, path);
        var summary = JsonTree.object(job, "summary");
        assertNotNull(summary);
        var items = JsonTree.asArray(job.get("project_items"));
        assertNotNull(items);
        var plans = 0;
        var epics = 0;
        var logs = 0;
        var reports = 0;
        var bytes = 0L;
        for (var element : items) {
            var item = JsonTree.asObject(element);
            assertNotNull(item);
            plans += JsonTree.integer(item, "plans_count");
            epics += JsonTree.integer(item, "epics_count");
            logs += JsonTree.integer(item, "logs_count");
            reports += JsonTree.integer(item, "read_reports_count");
            bytes += JsonTree.number(item, "total_bytes").longValue();
        }
        assertEquals(JsonTree.integer(summary, "plans_count"), plans, "plans_count");
        assertEquals(JsonTree.integer(summary, "epics_count"), epics, "epics_count");
        assertEquals(JsonTree.integer(summary, "logs_count"), logs, "logs_count");
        assertEquals(JsonTree.integer(summary, "reports_count"), reports, "reports_count");
        assertEquals(JsonTree.number(summary, "freed_bytes").longValue(), bytes, "freed_bytes");
    }

    private static EpicResource epic(MockRuntimeEngine engine) throws IOException {
        return EpicResource.fromJson(body(engine, "/api/v1/epics/relay-hardening"));
    }

    private static Map<String, Object> epicListItem(MockRuntimeEngine engine, String epicId)
            throws IOException {
        var list = JsonTree.asArray(get(engine, "/api/v1/epics").get("epics"));
        assertNotNull(list);
        for (var element : list) {
            var item = JsonTree.asObject(element);
            if (item != null && epicId.equals(epicIdOf(item))) {
                return item;
            }
        }
        throw new AssertionError("The epic list names no " + epicId);
    }

    private static Map<String, Object> aspect(Map<String, Object> settings, String aspect) {
        var aspects = JsonTree.asArray(settings.get("aspects"));
        assertNotNull(aspects);
        for (var element : aspects) {
            var entry = JsonTree.asObject(element);
            if (entry != null && aspect.equals(JsonTree.string(entry, "aspect"))) {
                return entry;
            }
        }
        throw new AssertionError("The machine settings name no " + aspect + " aspect");
    }

    private static String epicIdOf(Map<String, Object> item) {
        var epicId = JsonTree.string(item, "epic_id");
        return epicId != null ? epicId : JsonTree.string(item, "id");
    }

    private static int rowsWithStatus(EpicResource epic, String status) {
        var count = 0;
        for (var workstream : epic.workstreams()) {
            for (var row : workstream.rows()) {
                if (status.equals(row.status())) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int countOf(List<String> states, String state) {
        return (int) states.stream().filter(state::equals).count();
    }

    private static int size(Map<String, Object> map, String member) {
        var list = JsonTree.asArray(map.get(member));
        assertNotNull(list);
        return list.size();
    }

    private static String leadingNumber(String text) {
        assertNotNull(text);
        var end = 0;
        while (end < text.length() && Character.isDigit(text.charAt(end))) {
            end++;
        }
        assertTrue(end > 0, "no number in: " + text);
        return text.substring(0, end);
    }

    private static MockRuntimeEngine engine(String scenarioName) {
        var registry = new ScenarioRegistry();
        return new MockRuntimeEngine(registry, registry.resolve(scenarioName), "test-token", "test-secret");
    }

    private static Map<String, Object> get(MockRuntimeEngine engine, String path) throws IOException {
        return JsonTree.asObject(JsonTree.parse(body(engine, path)));
    }

    private static String body(MockRuntimeEngine engine, String path) throws IOException {
        return body(engine, "GET", path);
    }

    private static String body(MockRuntimeEngine engine, String method, String path) throws IOException {
        return body(engine, method, path, new byte[0]);
    }

    private static String body(MockRuntimeEngine engine, String method, String path, byte[] payload)
            throws IOException {
        var request = new MockHttpRequest(method, path, Map.of("Authorization", AUTH), payload,
                System.nanoTime());
        var out = new ByteArrayOutputStream();
        engine.dispatch(request, new MockHttpResponse(out), false);
        var raw = out.toString(StandardCharsets.UTF_8);
        var split = raw.indexOf("\r\n\r\n");
        assertTrue(split >= 0, "no response body for " + path);
        return raw.substring(split + 4);
    }

    private static List<Map<String, Object>> itemsOf(Map<String, Object> job) {
        var projectItems = JsonTree.asArray(job.get("project_items"));
        assertNotNull(projectItems);
        var out = new ArrayList<Map<String, Object>>();
        for (var element : projectItems) {
            var item = JsonTree.asObject(element);
            assertNotNull(item);
            out.addAll(items(item));
        }
        return out;
    }

    private static List<Map<String, Object>> items(Map<String, Object> projectItem) {
        var list = JsonTree.asArray(projectItem.get("items"));
        assertNotNull(list);
        var out = new ArrayList<Map<String, Object>>();
        for (var element : list) {
            var item = JsonTree.asObject(element);
            assertNotNull(item);
            out.add(item);
        }
        return out;
    }
}
