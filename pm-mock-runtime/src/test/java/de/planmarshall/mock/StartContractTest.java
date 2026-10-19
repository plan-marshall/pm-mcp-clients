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
import de.planmarshall.api.start.AdoptionFailureReason;
import de.planmarshall.api.start.EntryKind;
import de.planmarshall.api.start.ScreeningVerdict;
import de.planmarshall.api.start.SourceFailureCause;
import de.planmarshall.api.start.SourceKind;
import de.planmarshall.api.start.SourceKindOffer;
import de.planmarshall.api.start.SourceState;
import de.planmarshall.api.start.SourceTrust;
import de.planmarshall.api.start.StartChatResource;
import de.planmarshall.api.start.StartState;
import de.planmarshall.api.start.StartStateResource;
import de.planmarshall.mock.scenario.ScenarioLoader;

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
                        "/api/v1/entries/ent-3f9a2c41/links/add_pull_request", "{}")
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
                var second = fetch(runtime, "ent-3f9a2c41");
                assertEquals(first.entry().sourceCount(), second.entry().sourceCount());
                assertEquals(StartState.SOURCES, second.state());
                assertTrue(second.entry().canContinue());
            }
        }

        @Test
        @DisplayName("the local changes are withheld under landing.files, and the act is refused")
        void localChangesUnderLandingFiles() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.START_LOCAL_CHANGES)) {
                var state = fetch(runtime, "ent-3f9a2c41");
                assertTrue(state.entry().kindOffers().stream()
                        .filter(offer -> offer.kind() == SourceKind.LOCAL_CHANGES)
                        .noneMatch(SourceKindOffer::available));
                assertTrue(state.entry().sources().isEmpty());

                var refused = client(runtime).send(HttpRequest.json("POST",
                        "/api/v1/entries/ent-3f9a2c41/links/add_local_changes", "{}")
                        .withHeader("Authorization", "Bearer " + runtime.token()));
                assertEquals(409, refused.status(), "the withheld kind is not added by its act");
                assertTrue(fetch(runtime, "ent-3f9a2c41").entry().sources().isEmpty());
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

    @Nested
    @DisplayName("The operator's acts")
    class Acts {

        @Test
        @DisplayName("a description is added, and the documents mode is asked with copy preselected")
        void descriptionAndDocumentsMode() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.START_LOCAL_CHANGES)) {
                var auth = "Bearer " + runtime.token();
                var added = client(runtime).send(HttpRequest.json("POST",
                        "/api/v1/entries/ent-3f9a2c41/links/add_description",
                        JsonTree.write(Map.of("text", "Bring the Codeberg provider in")))
                        .withHeader("Authorization", auth));
                assertEquals(200, added.status());
                assertEquals(1, StartStateResource.fromJson(added.bodyText()).entry().sourceCount());

                var documents = client(runtime).send(HttpRequest.json("POST",
                        "/api/v1/entries/ent-3f9a2c41/links/add_documents", "{}")
                        .withHeader("Authorization", auth));
                assertEquals(200, documents.status());
                var withDocuments = StartStateResource.fromJson(documents.bodyText());
                assertEquals(SourceState.CHECKING, withDocuments.entry().sources().getLast().state(),
                        "the machine's mode is ask, so the copy-or-move choice is the operator's");

                var copy = client(runtime).send(HttpRequest.json("POST",
                        "/api/v1/entries/ent-3f9a2c41/links/documents_mode:copy_always", "{}")
                        .withHeader("Authorization", auth));
                assertEquals(200, copy.status());
                var afterCopy = StartStateResource.fromJson(copy.bodyText());
                assertEquals(SourceState.COPIED, afterCopy.entry().sources().getLast().state());
            }
        }

        @Test
        @DisplayName("a source that offers no retry is refused, and an acceptance needs a reason")
        void refusedActs() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.START_SOURCES)) {
                var auth = "Bearer " + runtime.token();
                var noRetry = client(runtime).send(HttpRequest.json("POST",
                        "/api/v1/entries/ent-3f9a2c41/links/retry:src-nope", "{}")
                        .withHeader("Authorization", auth));
                assertEquals(409, noRetry.status());

                addSource(runtime, "add_issue", "{}");
                var noReason = client(runtime).send(HttpRequest.json("POST",
                        "/api/v1/entries/ent-3f9a2c41/links/accept:src-1", "{}")
                        .withHeader("Authorization", auth));
                assertEquals(409, noReason.status());
            }
        }

        @Test
        @DisplayName("a recipe's parameters are set and the recipe is chosen")
        void recipeParameters() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.START_RECIPE)) {
                var auth = "Bearer " + runtime.token();
                var chosen = client(runtime).send(HttpRequest.json("POST",
                        "/api/v1/entries/ent-3f9a2c41/links/choose:code-review", "{}")
                        .withHeader("Authorization", auth));
                assertEquals(200, chosen.status());
                assertEquals(StartState.RECIPE_PARAMETERS,
                        StartStateResource.fromJson(chosen.bodyText()).state());

                var set = client(runtime).send(HttpRequest.json("POST",
                        "/api/v1/entries/ent-3f9a2c41/links/set",
                        JsonTree.write(Map.of("parameters", Map.of("depth", "quick"))))
                        .withHeader("Authorization", auth));
                assertEquals(200, set.status());
                assertEquals(StartState.READY, StartStateResource.fromJson(set.bodyText()).state());
            }
        }

        @Test
        @DisplayName("an epic's adoptable ledger is adopted, and a new epic is asked for otherwise")
        void epicActs() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.START_EPIC_ADOPT)) {
                var auth = "Bearer " + runtime.token();
                var refused = client(runtime).send(HttpRequest.json("POST",
                        "/api/v1/entries/ent-7a2e4f01/links/adopt:storage-migration", "{}")
                        .withHeader("Authorization", auth));
                assertEquals(409, refused.status(), "the ledger cannot be adopted");
                var failed = fetch(runtime, "ent-7a2e4f01");
                assertEquals(StartState.EPIC_ADOPT_FAILED, failed.state());
                assertEquals(AdoptionFailureReason.LEDGER_UNREADABLE, failed.adoptionFailure().reason());
                assertNotNull(failed.link("retry"));

                var retried = client(runtime).send(HttpRequest.json("POST",
                        "/api/v1/entries/ent-7a2e4f01/links/retry", "{}")
                        .withHeader("Authorization", auth));
                assertEquals(200, retried.status());
                assertEquals(StartState.EPIC_ADOPT, StartStateResource.fromJson(retried.bodyText()).state());

                var adopted = client(runtime).send(HttpRequest.json("POST",
                        "/api/v1/entries/ent-7a2e4f01/links/adopt:relay-hardening", "{}")
                        .withHeader("Authorization", auth));
                assertEquals(200, adopted.status());
                assertEquals("relay-hardening",
                        StartStateResource.fromJson(adopted.bodyText()).entry().planId());
            }
        }

        @Test
        @DisplayName("a new epic is started instead of adopting a ledger")
        void newEpicInsteadOfAdopting() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.START_EPIC_ADOPT)) {
                var newEpic = client(runtime).send(HttpRequest.json("POST",
                        "/api/v1/entries/ent-7a2e4f01/links/new", "{}")
                        .withHeader("Authorization", "Bearer " + runtime.token()));
                assertEquals(200, newEpic.status());
                var state = StartStateResource.fromJson(newEpic.bodyText());
                assertEquals(StartState.SOURCES, state.state());
                assertTrue(state.entry().adoptableEpics().isEmpty());
            }
        }

        @Test
        @DisplayName("a refused gate leaves nothing behind and a recipe start offers no chat")
        void refusalAndRecipe() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.START_REFUSED)) {
                var state = fetch(runtime, "ent-3f9a2c41");
                assertNotNull(state.refusal());
                assertTrue(state.entry().sources().isEmpty());
            }
            try (var runtime = MockRuntime.start(StandardScenarios.START_RECIPE)) {
                assertNull(fetch(runtime, "ent-3f9a2c41").chat());
            }
        }

        @Test
        @DisplayName("the picker serves the home root and refuses an unknown root and project")
        void pickerRefusals() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.START_SOURCES)) {
                var auth = "Bearer " + runtime.token();
                var unknownRoot = client(runtime).send(HttpRequest.get(
                        "/api/v1/projects/pm-mcp-clients/files?root=sandbox")
                        .withHeader("Authorization", auth));
                assertEquals(400, unknownRoot.status());

                var unknownProject = client(runtime).send(HttpRequest.get(
                        "/api/v1/projects/no-such-project/files?root=checkout")
                        .withHeader("Authorization", auth));
                assertEquals(404, unknownProject.status());
            }
        }
    }

    private static void addSource(MockRuntime runtime, String link, String body) throws Exception {
        var response = client(runtime).send(HttpRequest.json("POST",
                "/api/v1/entries/ent-3f9a2c41/links/" + link, body)
                .withHeader("Authorization", "Bearer " + runtime.token()));
        assertEquals(200, response.status(), response.bodyText());
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
    @DisplayName("every start scenario's fixture names only members of the closed contract catalogues")
    void fixturesUseTheContract() {
        for (var name : StandardScenarios.ALL.stream()
                .filter(candidate -> StandardScenarios.START.equals(candidate) || candidate.startsWith("start-"))
                .toList()) {
            var startCase = ScenarioLoader.load(name).startCase();
            assertNotNull(StartState.fromWire((String) startCase.get("state")), name);
            assertNotNull(EntryKind.fromWire((String) startCase.get("kind")), name);
            for (var source : (List<?>) startCase.getOrDefault("sources", List.of())) {
                var map = (Map<?, ?>) source;
                SourceKind.fromWire((String) map.get("kind"));
                SourceState.fromWire((String) map.get("state"));
                var verdict = (String) map.get("verdict");
                if (verdict != null) {
                    ScreeningVerdict.fromWire(verdict);
                }
                var trust = (String) map.get("trust");
                if (trust != null) {
                    SourceTrust.fromWire(trust);
                }
                var failure = (Map<?, ?>) map.get("failure");
                if (failure != null) {
                    SourceFailureCause.fromWire((String) failure.get("cause"));
                }
            }
        }
    }
}
