/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.planmarshall.api.MachineApiPaths;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.json.JsonTree;

/**
 * The mock runtime's answers for the two views of UC-PLN-7 and UC-PLN-8: the list of everything that
 * waits, and finding work by name.
 */
@DisplayName("NeedsAndFindScenarioTest")
@EnabledOnOs({OS.MAC, OS.LINUX})
class NeedsAndFindScenarioTest {

    private static final String AUTH_HEADER = "Authorization";

    @Nested
    @DisplayName("The list of what waits")
    class TheList {

        @Test
        @DisplayName("serves every kind of the machine in the runtime's order, blocking work first")
        void servesTheListInTheRuntimeOrder() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = "Bearer " + runtime.token();

                var res = client.send(HttpRequest.get(MachineApiPaths.NEEDS_OPERATOR)
                        .withHeader(AUTH_HEADER, auth));
                assertEquals(200, res.status());

                var page = JsonTree.asObject(JsonTree.parse(new String(res.body())));
                var items = JsonTree.asArray(page.get("items"));
                assertEquals(12, items.size(), "one item of each kind of the closed list");

                // The items that block running work first, the oldest first; the machine's own
                // items, which block nothing, after them.
                assertEquals(List.of(
                                "/api/v1/epics/relay-hardening",
                                "/api/v1/plans/drop-legacy-api",
                                "/api/v1/plans/fix-relay",
                                "/api/v1/plans/session-log",
                                "/api/v1/plans/profile-loader",
                                "/api/v1/plans/relay-metrics",
                                "/api/v1/plans/session-log",
                                "/api/v1/plans/fix-relay",
                                "/api/v1/plans/drop-legacy-api",
                                "/api/v1/system/cleanup",
                                "/api/v1/credentials/github",
                                "/api/v1/projects/plan-marshall"),
                        items.stream().map(item -> JsonTree.string(JsonTree.asObject(item), "link")).toList());
            }
        }

        @Test
        @DisplayName("serves an empty list when nothing waits")
        void nothingWaits() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK + "#overview.empty")) {
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = "Bearer " + runtime.token();

                var res = client.send(HttpRequest.get(MachineApiPaths.NEEDS_OPERATOR)
                        .withHeader(AUTH_HEADER, auth));
                assertEquals(200, res.status());
                var page = JsonTree.asObject(JsonTree.parse(new String(res.body())));
                assertTrue(JsonTree.asArray(page.get("items")).isEmpty());
            }
        }

        @Test
        @DisplayName("an item resolved leaves the list and the event is sent")
        void resolvedItemLeaves() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = "Bearer " + runtime.token();

                assertEquals(12, itemCount(client, auth));

                try (var exchange = client.open(HttpRequest.get("/api/v1/events").withHeader(AUTH_HEADER, auth))) {
                    assertTrue(exchange.isEventStream());
                    var sse = exchange.events();

                    var gotoRes = client.send(HttpRequest.json("POST", "/mock/goto/overview.needs-resolved", "{}"));
                    assertEquals(200, gotoRes.status());

                    assertEquals(11, itemCount(client, auth));

                    var event = assertTimeoutPreemptively(Duration.ofSeconds(5), () -> nextNeedsEvent(sse));
                    assertNotNull(event, "the resolved item is announced");
                    assertEquals("needs_operator.changed", JsonTree.string(event, "event"));
                    assertEquals(11, JsonTree.integer(event, "count"));
                }
            }
        }

        @Test
        @DisplayName("serves the machine's summary with its own items")
        void machineSummary() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = "Bearer " + runtime.token();

                var res = client.send(HttpRequest.get(MachineApiPaths.MACHINE).withHeader(AUTH_HEADER, auth));
                assertEquals(200, res.status());
                var machine = JsonTree.asObject(JsonTree.parse(new String(res.body())));
                var summary = JsonTree.asObject(machine.get("needs_operator"));
                assertNotNull(summary);
                assertEquals(2, JsonTree.integer(summary, "count"));
                var items = JsonTree.asArray(summary.get("items"));
                assertEquals(2, items.size());
                assertEquals("credential", JsonTree.string(JsonTree.asObject(items.getFirst()), "kind"));
            }
        }

        private static int itemCount(UnixSocketHttpClient client, String auth) throws Exception {
            var res = client.send(HttpRequest.get(MachineApiPaths.NEEDS_OPERATOR).withHeader(AUTH_HEADER, auth));
            assertEquals(200, res.status());
            return JsonTree.asArray(JsonTree.asObject(JsonTree.parse(new String(res.body()))).get("items")).size();
        }

        private static Map<String, Object> nextNeedsEvent(de.planmarshall.api.http.SseReader sse) throws Exception {
            for (int i = 0; i < 40; i++) {
                var event = sse.next();
                if (event == null) {
                    return null;
                }
                if ("needs_operator.changed".equals(event.event())) {
                    return JsonTree.asObject(JsonTree.parse(event.data()));
                }
            }
            return null;
        }
    }

    @Nested
    @DisplayName("Finding work by name")
    class Finding {

        @Test
        @DisplayName("a part of an id or a title matches, ignoring case, running work first")
        void matchesByIdAndTitle() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = "Bearer " + runtime.token();

                var matches = matches(client, auth, "FIX-RELAY");

                assertEquals(2, matches.size(), "the running plan and the ended one");
                assertEquals("fix-relay", JsonTree.string(matches.getFirst(), "id"));
                assertEquals(Boolean.FALSE, matches.getFirst().get("ended"));
                assertEquals("fix-relay-v1", JsonTree.string(matches.get(1), "id"));
                assertEquals(Boolean.TRUE, matches.get(1).get("ended"));
                assertEquals("merged", matches.get(1).get("outcome"));
            }
        }

        @Test
        @DisplayName("a pull request number finds its plan")
        void findsByPullRequestNumber() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = "Bearer " + runtime.token();

                var matches = matches(client, auth, "214");

                assertEquals(2, matches.size(), "the running plan and its ended predecessor both carry #214");
                assertEquals("fix-relay", JsonTree.string(matches.getFirst(), "id"));
                assertEquals(Boolean.FALSE, matches.getFirst().get("ended"));
                assertEquals("plan", JsonTree.string(matches.getFirst(), "kind"));
            }
        }

        @Test
        @DisplayName("matches an epic by its title and keeps running work ahead of ended work")
        void epicsAndOrder() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = "Bearer " + runtime.token();

                var matches = matches(client, auth, "relay-hardening");

                assertTrue(matches.size() >= 2);
                var kinds = matches.stream().map(match -> JsonTree.string(match, "kind")).toList();
                assertEquals("epic", kinds.getFirst(), "running work comes first, by kind: project, epic, plan");
                boolean seenEnded = false;
                for (var match : matches) {
                    var ended = Boolean.TRUE.equals(match.get("ended"));
                    assertFalse(seenEnded && !ended, "ended work never stands before running work");
                    seenEnded = seenEnded || ended;
                }
            }
        }

        @Test
        @DisplayName("no match says so and keeps the text; an empty text is refused")
        void noMatchAndEmptyText() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = "Bearer " + runtime.token();

                var none = client.send(HttpRequest.get(MachineApiPaths.FIND + "?q=zzzzz").withHeader(AUTH_HEADER, auth));
                assertEquals(200, none.status());
                var page = JsonTree.asObject(JsonTree.parse(new String(none.body())));
                assertEquals("zzzzz", JsonTree.string(page, "query"));
                assertTrue(JsonTree.asArray(page.get("matches")).isEmpty());

                var empty = client.send(HttpRequest.get(MachineApiPaths.FIND + "?q=").withHeader(AUTH_HEADER, auth));
                assertEquals(400, empty.status());
                assertEquals("invalid_query", JsonTree.string(
                        JsonTree.asObject(JsonTree.parse(new String(empty.body()))), "error"));

                var missing = client.send(HttpRequest.get(MachineApiPaths.FIND).withHeader(AUTH_HEADER, auth));
                assertEquals(400, missing.status());
            }
        }

        @Test
        @DisplayName("a match names where it belongs")
        void namesWhereItBelongs() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = "Bearer " + runtime.token();

                var matches = matches(client, auth, "fix-relay");
                var project = JsonTree.asObject(matches.getFirst().get("project"));
                assertEquals("pm-mcp-clients", JsonTree.string(project, "id"));
                var epic = JsonTree.asObject(matches.getFirst().get("epic"));
                assertEquals("relay-hardening", JsonTree.string(epic, "id"));
                var plan = JsonTree.asObject(matches.getFirst().get("plan"));
                assertEquals("fix-relay", JsonTree.string(plan, "id"));
            }
        }

        private static List<Map<String, Object>> matches(UnixSocketHttpClient client, String auth, String query)
                throws Exception {
            var res = client.send(HttpRequest.get(MachineApiPaths.FIND + "?q=" + query)
                    .withHeader(AUTH_HEADER, auth));
            assertEquals(200, res.status());
            var page = JsonTree.asObject(JsonTree.parse(new String(res.body())));
            var raw = JsonTree.asArray(page.get("matches"));
            return raw.stream().map(JsonTree::asObject).toList();
        }
    }
}
