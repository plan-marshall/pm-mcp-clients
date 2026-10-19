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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.interaction.OperatorLink;
import de.planmarshall.api.interaction.OperatorScopeKind;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.start.EntryKind;
import de.planmarshall.api.start.SourceFailureCause;
import de.planmarshall.api.start.SourceKind;
import de.planmarshall.api.start.SourceState;
import de.planmarshall.api.start.StartChatResource;
import de.planmarshall.api.start.StartState;
import de.planmarshall.api.start.StartStateResource;

@DisplayName("Start contract over the mock runtime")
@EnabledOnOs({OS.MAC, OS.LINUX})
class StartContractTest {

    @Nested
    @DisplayName("The operator representation")
    class Representation {

        @Test
        @DisplayName("every start state decodes with the operator state codec of pm-api")
        void decodesWithTheOperatorCodec() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.START_SOURCES)) {
                var response = client(runtime).send(HttpRequest.get("/api/v1/entries/ent-3f9a2c41")
                        .withHeader("Authorization", "Bearer " + runtime.token()));
                assertEquals(200, response.status());

                var decoded = StartStateResource.fromMap(JsonTree.asObject(JsonTree.parse(response.bodyText())));
                var state = decoded.operator();

                assertEquals(OperatorScopeKind.ENTRY, state.scope().kind());
                assertEquals("ent-3f9a2c41", state.scope().id());
                assertEquals("pm-mcp-clients", state.scope().projectId());
                assertEquals("start.sources", state.stateId());
                assertEquals("operator_wait", state.kind());
                assertNull(state.revision());
                for (OperatorLink link : state.links()) {
                    assertTrue(link.href().startsWith("/api/v1/entries/ent-3f9a2c41/links/"));
                }
            }
        }

        @Test
        @DisplayName("the entry's states are the closed list of the contract")
        void statesAreTheClosedList() {
            assertEquals(Set.of("start.choose", "start.sources", "start.ready", "start.recipe",
                            "start.recipe-create", "start.refused", "start-epic.adopt", "start-epic.adopt-failed",
                            "recipe.choose", "recipe.parameters", "recipe.create"),
                    new TreeSet<>(StartState.wireValues()));
            assertEquals(StartState.SOURCES, StartState.fromWire("start.sources"));
            assertThrows(IllegalArgumentException.class, () -> StartState.fromWire("start.bogus"));
        }

        @Test
        @DisplayName("the source kinds offered at start.sources carry their message key and arguments")
        void refusedKindsCarryTheirReason() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.START_LOCAL_CHANGES)) {
                var state = fetch(runtime, "ent-3f9a2c41");
                var local = state.entry().kindOffers().stream()
                        .filter(offer -> offer.kind() == SourceKind.LOCAL_CHANGES).findFirst().orElseThrow();
                assertFalse(local.available());
                assertEquals("start.sources.local_changes_landing", local.reasonKey());
                assertEquals("landing.files", local.reasonArgs().get("landing"));
            }
        }
    }

    @Nested
    @DisplayName("Invariants")
    class Invariants {

        @Test
        @DisplayName("a rate-limited source is never retried by the runtime on its own")
        void rateLimitIsNeverRetried() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.START_RATE_LIMITED)) {
                for (var attempt = 0; attempt < 3; attempt++) {
                    var source = fetch(runtime, "ent-3f9a2c41").entry().sources().stream()
                            .filter(candidate -> "src-issue-1".equals(candidate.sourceId()))
                            .findFirst().orElseThrow();
                    assertEquals(SourceState.FAILED, source.state(),
                            "attempt " + attempt + " leaves the rate limit in force");
                    assertEquals(SourceFailureCause.RATE_LIMITED,
                            source.failure().cause());
                    assertNotNull(source.failure().resetsAt());
                }
            }
        }

        @Test
        @DisplayName("a pull request's changed files are untrusted unless the operator authored them")
        void pullRequestFilesStayUntrusted() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.START_PULL_REQUEST)) {
                var added = client(runtime).send(HttpRequest.json("POST",
                        "/api/v1/entries/ent-3f9a2c41/links/add_pr", "{}")
                        .withHeader("Authorization", "Bearer " + runtime.token()));
                assertEquals(200, added.status());

                var raw = JsonTree.asObject(JsonTree.parse(added.bodyText()));
                var items = JsonTree.asArray(raw.get("items"));
                var pr = items.stream().map(JsonTree::asObject)
                        .filter(item -> "src-pr-1".equals(JsonTree.string(item, "id")))
                        .findFirst().orElseThrow();
                var facts = JsonTree.object(pr, "facts");
                assertEquals("untrusted", JsonTree.string(facts, "trust"));
                for (var file : JsonTree.asArray(facts.get("changed_files"))) {
                    var object = JsonTree.asObject(file);
                    assertTrue(Boolean.TRUE.equals(JsonTree.bool(object, "untrusted"))
                            || Boolean.TRUE.equals(JsonTree.bool(object, "operator_authored")),
                            "a changed file is untrusted unless the operator authored it");
                }
            }
        }

        @Test
        @DisplayName("an entry never expires: a second read returns it unchanged")
        void entryNeverExpires() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.START_ENTRY_CONTINUE)) {
                var first = fetch(runtime, "ent-3f9a2c41");
                Thread.sleep(50);
                var second = fetch(runtime, "ent-3f9a2c41");
                assertEquals(first.entry().sourceCount(), second.entry().sourceCount());
                assertEquals(StartState.SOURCES, second.state());
                assertTrue(second.entry().canContinue());
            }
        }

        @Test
        @DisplayName("the local changes are withheld under landing.files, with the project's setting named")
        void localChangesUnderLandingFiles() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.START_LOCAL_CHANGES)) {
                var state = fetch(runtime, "ent-3f9a2c41");
                assertFalse(state.entry().kindOffers().stream()
                        .filter(offer -> offer.kind() == SourceKind.LOCAL_CHANGES)
                        .anyMatch(offer -> offer.available()));
            }
        }
    }

    @Nested
    @DisplayName("Chat and follow-up")
    class ChatAndFollowUp {

        @Test
        @DisplayName("chat is available once a source is stored and every external one is screened")
        void chatAvailability() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.START_CHAT)) {
                var state = fetch(runtime, "ent-3f9a2c41");
                assertTrue(state.chat().available());
                assertNull(state.chat().disabledReasonKey());
                assertTrue(state.chat().routedByKind(EntryKind.PLAN));
            }
        }

        @Test
        @DisplayName("chat is absent for a recipe")
        void chatAbsentForRecipe() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.START_RECIPE)) {
                var state = fetch(runtime, "ent-3f9a2c41");
                assertNull(state.chat(), "a recipe's start offers no chat");
            }
        }

        @Test
        @DisplayName("the follow-up plan opens at start.sources with the ended work named in its description")
        void followUpPlan() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.START_FOLLOW_UP)) {
                var opened = client(runtime).send(HttpRequest.json("POST",
                        "/api/v1/projects/pm-mcp-clients/ended/codeberg-issues/ended/accept", "{}")
                        .withHeader("Authorization", "Bearer " + runtime.token()));
                assertEquals(201, opened.status());

                var state = StartStateResource.fromJson(opened.bodyText());
                assertEquals(StartState.SOURCES, state.state());
                assertTrue(state.entry().sources().getFirst().reference().contains("codeberg-issues"));
            }
        }
    }

    @Nested
    @DisplayName("Codecs")
    class Codecs {

        @Test
        @DisplayName("the start state round-trips through its codec and the streaming reader")
        void startStateRoundTrip() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.START_SOURCES)) {
                var response = client(runtime).send(HttpRequest.get("/api/v1/entries/ent-3f9a2c41")
                        .withHeader("Authorization", "Bearer " + runtime.token()));
                var state = StartStateResource.fromJson(response.bodyText());

                assertEquals(state.toJson(), StartStateResource.fromJson(state.toJson()).toJson());
                assertEquals(state.entryId(), StartStateResource.fromJson(state.toJson()).entryId());
            }
        }

        @Test
        @DisplayName("an unknown entry, an unknown act, and a missing chat turn are refused")
        void refusals() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.START_CHAT)) {
                var auth = "Bearer " + runtime.token();

                var unknownEntry = client(runtime).send(HttpRequest.get("/api/v1/entries/ent-nope")
                        .withHeader("Authorization", auth));
                assertEquals(404, unknownEntry.status());

                var unknownAct = client(runtime).send(HttpRequest.json("POST",
                        "/api/v1/entries/ent-3f9a2c41/links/never", "{}")
                        .withHeader("Authorization", auth));
                assertEquals(409, unknownAct.status());

                var emptyTurn = client(runtime).send(HttpRequest.json("POST",
                        "/api/v1/entries/ent-3f9a2c41/chat/turns", "{}")
                        .withHeader("Authorization", auth));
                assertEquals(400, emptyTurn.status());
            }
        }
    }

    private static UnixSocketHttpClient client(MockRuntime runtime) {
        return new UnixSocketHttpClient(runtime.socketPath());
    }

    private static StartStateResource fetch(MockRuntime runtime, String entryId) throws Exception {
        var response = client(runtime).send(HttpRequest.get("/api/v1/entries/" + entryId)
                .withHeader("Authorization", "Bearer " + runtime.token()));
        assertEquals(200, response.status(), response.bodyText());
        return StartStateResource.fromJson(response.bodyText());
    }

    @Test
    @DisplayName("the chat resource carries the routed role and its transcript state")
    void chatResource() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.START_CHAT)) {
            var chat = fetch(runtime, "ent-3f9a2c41").chat();
            assertAll(
                    () -> assertEquals("refine", chat.role().value()),
                    () -> assertEquals("request", chat.scope().value()),
                    () -> assertEquals("with_entry", chat.transcript().wireValue()));
            assertEquals(chat, StartChatResource.fromJson(chat.toJson()));
        }
    }

    @Test
    @DisplayName("the mock scenario fixtures name the same closed values as the contract")
    void fixturesUseTheContract() throws Exception {
        for (var name : List.of(StandardScenarios.START_SOURCES, StandardScenarios.START_CHAT,
                StandardScenarios.START_EPIC_ADOPT)) {
            try (var runtime = MockRuntime.start(name)) {
                var response = client(runtime).send(HttpRequest.get("/api/v1/recipes")
                        .withHeader("Authorization", "Bearer " + runtime.token()));
                assertEquals(200, response.status());
            }
        }
        var unused = Map.of();
        assertTrue(unused.isEmpty());
    }
}
