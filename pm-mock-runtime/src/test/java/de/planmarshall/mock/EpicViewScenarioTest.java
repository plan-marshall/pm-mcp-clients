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
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.planmarshall.api.control.EpicResource;
import de.planmarshall.api.control.EpicWaitReason;
import de.planmarshall.api.control.EpicWorkstream;
import de.planmarshall.api.control.WorkRow;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.HttpResponse;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.json.JsonTree;

/**
 * The epic as a resource in the scenarios both clients use for UC-PLN-5 (specification
 * <em>The Epic as a Resource</em>): the header, the workstreams with their waits, the sources,
 * landings, registers, tokens, and history, plus the epic-scoped event stream. Every answer decodes
 * with the {@code pm-api} codec of the contract, which is what a schema warning would break.
 */
@DisplayName("EpicViewScenario")
@EnabledOnOs({OS.MAC, OS.LINUX})
class EpicViewScenarioTest {

    private static final String AUTH_HEADER = "Authorization";
    private static final String EPIC_PATH = "/api/v1/epics/relay-hardening";
    private static final String EPIC_EVENTS_PATH = EPIC_PATH + "/events";

    @Test
    @DisplayName("running-work serves the epic's header and its two workstreams with their rows")
    void runningWorkServesTheEpicHeaderAndWorkstreams() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
            var epic = EpicResource.fromJson(get(runtime, EPIC_PATH).bodyText());

            assertEquals("relay-hardening", epic.epicId());
            assertEquals("pm-mcp-clients", epic.projectId());
            assertEquals("pm-mcp-clients", epic.displayName());
            assertEquals("running", epic.state());
            assertTrue(epic.autoStart());
            assertEquals(3, epic.scope());
            assertEquals(3, epic.running());

            assertEquals(List.of("WS-1", "WS-2"),
                    epic.workstreams().stream().map(EpicWorkstream::id).toList());
            var reconnect = epic.workstreams().getFirst();
            assertEquals("Reconnect", reconnect.name());
            assertTrue(reconnect.open());
            assertEquals(List.of("relay-reconnect-backoff", "relay-cursor-order",
                            "relay-reconnect-docs", "relay-reconnect-metrics"),
                    reconnect.rows().stream().map(WorkRow::planId).toList());

            var backoff = row(epic, "relay-reconnect-backoff");
            assertEquals("shipped", backoff.status());
            assertEquals(211, backoff.pullRequestNumber());
            assertEquals("https://github.com/plan-marshall/pm-mcp-clients/pull/211",
                    backoff.pullRequestUrl());
        }
    }

    @Test
    @DisplayName("running-work names every staged wait, its sources, landings, registers, tokens, and events")
    void runningWorkNamesTheWaitsAndTheSections() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
            var epic = EpicResource.fromJson(get(runtime, EPIC_PATH).bodyText());

            assertEquals(EpicWaitReason.NONE, row(epic, "relay-reconnect-metrics").waits());

            var rotation = row(epic, "relay-token-rotation");
            assertEquals(EpicWaitReason.DEPENDENCY, rotation.waits());
            assertEquals("relay-auth-v2", rotation.waitPlanId());

            var docs = row(epic, "relay-docs");
            assertEquals(EpicWaitReason.OVERLAP, docs.waits());
            assertEquals("relay-auth-v2", docs.waitPlanId());
            assertFalse(docs.overlapFiles().isEmpty(), "an overlap names the files in common");

            assertEquals(List.of("2 documents", "1 issue"), epic.sources());
            assertEquals(2, epic.landings());
            assertEquals(3, epic.findings().newCount());
            assertEquals(4, epic.findings().open());
            assertEquals(2, epic.findings().kept());
            assertEquals(4, epic.watches().open());

            assertTrue(epic.tokens().total().measured());
            assertTrue(epic.tokens().total().floor());
            assertTrue(epic.tokens().total().notMeasuredTasks() > 0);
            assertFalse(epic.tokens().plans().isEmpty());

            assertEquals(3, epic.events().size());
            assertEquals("relay-auth-v2 asks a decision", epic.events().getFirst().text());
        }
    }

    @Test
    @DisplayName("the epic answers its own map shape unchanged on a second read")
    void theEpicRoundTripsThroughItsContract() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
            var first = EpicResource.fromJson(get(runtime, EPIC_PATH).bodyText());
            var second = EpicResource.fromJson(get(runtime, EPIC_PATH).bodyText());

            assertEquals(first, second, "the derived resource is the same on every read");
            assertEquals(first, EpicResource.fromJson(first.toJson()),
                    "the resource survives its own codec without losing a field");
        }
    }

    @Test
    @DisplayName("fully-onboarded serves the showroom epic with its third workstream")
    void fullyOnboardedServesTheShowroomEpic() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.FULLY_ONBOARDED)) {
            var epic = EpicResource.fromJson(get(runtime, EPIC_PATH).bodyText());

            assertEquals(3, epic.workstreams().size());
            var observability = epic.workstreams().get(2);
            assertEquals("WS-3", observability.id());
            assertEquals("Observability", observability.name());
            assertEquals("paused", row(epic, "relay-metrics").status());
            assertEquals("staged", row(epic, "relay-metrics-docs").status());
        }
    }

    @Test
    @DisplayName("an unknown epic answers scope_not_found and invents no resource")
    void unknownEpicIsNotFound() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
            var response = get(runtime, "/api/v1/epics/no-such-epic");

            assertEquals(404, response.status());
            var body = JsonTree.asObject(JsonTree.parse(response.bodyText()));
            assertNotNull(body);
            assertEquals("epic_not_found", JsonTree.string(body, "error"));
        }
    }

    @Test
    @DisplayName("the epic-scoped stream carries its own row changes and no other epic's")
    void epicScopedStreamCarriesItsOwnEvents() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            try (var epicStream = client.open(
                         HttpRequest.get(EPIC_EVENTS_PATH).withHeader(AUTH_HEADER, auth));
                 var otherStream = client.open(
                         HttpRequest.get("/api/v1/epics/relay-docs-epic/events").withHeader(AUTH_HEADER, auth))) {
                assertTrue(epicStream.isEventStream());
                var epicEvents = epicStream.events();
                var otherEvents = otherStream.events();

                runtime.engine().broadcast("epic.row_changed", Map.of(
                        "scope", Map.of("kind", "epic", "id", "relay-hardening"),
                        "epic_id", "relay-hardening",
                        "plan_id", "relay-reconnect-metrics",
                        "status", "running"));

                var scoped = assertTimeoutPreemptively(Duration.ofSeconds(5), epicEvents::next);
                assertNotNull(scoped);
                assertEquals("epic.row_changed", scoped.event());
                var data = JsonTree.asObject(JsonTree.parse(scoped.data()));
                assertEquals("relay-hardening", JsonTree.string(data, "epic_id"));
                assertEquals("relay-reconnect-metrics", JsonTree.string(data, "plan_id"));

                // The other epic's stream did not receive that event: its first event is its own.
                runtime.engine().broadcast("epic.row_changed", Map.of(
                        "scope", Map.of("kind", "epic", "id", "relay-docs-epic"),
                        "epic_id", "relay-docs-epic",
                        "plan_id", "relay-docs",
                        "status", "handed_back"));

                var other = assertTimeoutPreemptively(Duration.ofSeconds(5), otherEvents::next);
                assertNotNull(other);
                assertEquals("epic.row_changed", other.event());
                var otherData = JsonTree.asObject(JsonTree.parse(other.data()));
                assertEquals("relay-docs-epic", JsonTree.string(otherData, "epic_id"));
                assertEquals("relay-docs", JsonTree.string(otherData, "plan_id"));
            }
        }
    }

    private static WorkRow row(EpicResource epic, String planId) {
        return epic.workstreams().stream()
                .flatMap(workstream -> workstream.rows().stream())
                .filter(row -> planId.equals(row.planId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no row for " + planId));
    }

    private static HttpResponse get(MockRuntime runtime, String path) throws Exception {
        return new UnixSocketHttpClient(runtime.socketPath())
                .send(HttpRequest.get(path).withHeader(AUTH_HEADER, "Bearer " + runtime.token()));
    }
}
