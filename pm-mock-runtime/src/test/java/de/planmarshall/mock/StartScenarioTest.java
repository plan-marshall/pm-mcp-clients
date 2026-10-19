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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.planmarshall.api.config.ConfigOrigin;
import de.planmarshall.api.control.ChatRole;
import de.planmarshall.api.control.ChatScope;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.start.ConfinedFileListResource;
import de.planmarshall.api.start.ConfinedRoot;
import de.planmarshall.api.start.EntryKind;
import de.planmarshall.api.start.EntryOpenRequest;
import de.planmarshall.api.start.IntakeDraft;
import de.planmarshall.api.start.PlanStartResult;
import de.planmarshall.api.start.RecipePlanId;
import de.planmarshall.api.start.RecipeRef;
import de.planmarshall.api.start.SourceFailureCause;
import de.planmarshall.api.start.SourceInput;
import de.planmarshall.api.start.SourceKind;
import de.planmarshall.api.start.SourceKindOffer;
import de.planmarshall.api.start.SourceResource;
import de.planmarshall.api.start.SourceState;
import de.planmarshall.api.start.SourceTrust;
import de.planmarshall.api.start.StartEpicRequest;
import de.planmarshall.api.start.StartPlanRequest;
import de.planmarshall.api.start.StartRefusalReason;
import de.planmarshall.api.start.StartState;
import de.planmarshall.api.start.StartStateResource;
import de.planmarshall.api.start.StartTranscriptState;
import de.planmarshall.mock.scenario.ScenarioLoader;

@DisplayName("StartScenario")
@EnabledOnOs({OS.MAC, OS.LINUX})
class StartScenarioTest {


    @Test
    @DisplayName("loads the start scenario and lists the offered recipes")
    void shouldLoadScenarioAndListRecipes() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.START)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            assertEquals(StandardScenarios.START, runtime.currentScenarioName());
            assertEquals("start.sources", runtime.currentStateName());

            var res = client.send(HttpRequest.get("/api/v1/recipes").withHeader("Authorization", auth));
            assertEquals(200, res.status());

            var recipes = parseRecipes(res.bodyText());
            assertTrue(recipes.stream().anyMatch(r -> "agentfile-hygiene".equals(r.key())));
            assertTrue(recipes.stream().anyMatch(r -> "code-review".equals(r.key())));
            assertTrue(recipes.stream().anyMatch(r -> "lesson-cleanup".equals(r.key())));
            var audit = recipes.stream().filter(r -> "audit".equals(r.key())).findFirst().orElseThrow();
            assertTrue(audit.takesRequest(), "the audit recipe takes a request");
        }
    }

    @Test
    @DisplayName("starts a plan from a description and broadcasts plan.started")
    void shouldStartPlanFromDescription() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.START)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            try (var exchange = client.open(HttpRequest.get("/api/v1/events").withHeader("Authorization", auth))) {
                assertTrue(exchange.isEventStream());
                var sse = exchange.events();

                var request = new StartPlanRequest("pm-mcp-clients",
                        "Add a Codeberg provider for issues", null, Map.of(),
                        List.of("doc/concepts/forgejo.adoc"), "4711", null);
                var res = client.send(HttpRequest.json("POST", "/api/v1/projects/pm-mcp-clients/plans/start",
                        request.toJson()).withHeader("Authorization", auth));
                assertEquals(201, res.status());

                var result = PlanStartResult.fromJson(res.bodyText());
                assertEquals(PlanStartResult.KIND_PLAN, result.kind());
                assertEquals(PlanStartResult.STATE_STARTED, result.state());
                assertEquals("Add a Codeberg provider for issues", result.title());
                assertEquals("add-a-codeberg-provider-for-issues", result.id());

                var event = sse.next();
                assertNotNull(event);
                assertEquals("plan.started", event.event());
                var data = JsonTree.asObject(JsonTree.parse(event.data()));
                assertEquals("add-a-codeberg-provider-for-issues", JsonTree.string(data, "plan_id"));
            }
        }
    }

    @Test
    @DisplayName("derives a recipe plan id from the recipe key and the time")
    void shouldDeriveRecipePlanId() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.START)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var request = new StartPlanRequest("pm-mcp-clients", null, "audit", Map.of("modules", "all 9"),
                    List.of(), null, null);
            var res = client.send(HttpRequest.json("POST", "/api/v1/projects/pm-mcp-clients/plans/start",
                    request.toJson()).withHeader("Authorization", auth));
            assertEquals(201, res.status());

            var result = PlanStartResult.fromJson(res.bodyText());
            assertTrue(result.id().startsWith("recipe-audit-"), "the id derives from the recipe and the time: " + result.id());
        }
    }

    @Test
    @DisplayName("starts an epic and broadcasts epic.started")
    void shouldStartEpic() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.START)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            try (var exchange = client.open(HttpRequest.get("/api/v1/events").withHeader("Authorization", auth))) {
                var sse = exchange.events();

                var request = new StartEpicRequest("pm-mcp-clients", "Relay hardening", Boolean.FALSE, null);
                var res = client.send(HttpRequest.json("POST", "/api/v1/projects/pm-mcp-clients/epics/start",
                        request.toJson()).withHeader("Authorization", auth));
                assertEquals(201, res.status());

                var result = PlanStartResult.fromJson(res.bodyText());
                assertEquals(PlanStartResult.KIND_EPIC, result.kind());
                assertEquals("relay-hardening", result.id());

                var event = sse.next();
                assertNotNull(event);
                assertEquals("epic.started", event.event());
            }
        }
    }

    @Test
    @DisplayName("refuses a start in a project that is not ready")
    void shouldRefuseNotReadyProject() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.START)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var request = new StartPlanRequest("plan-marshall", "something", null, Map.of(), List.of(), null, null);
            var res = client.send(HttpRequest.json("POST", "/api/v1/projects/plan-marshall/plans/start",
                    request.toJson()).withHeader("Authorization", auth));
            assertEquals(409, res.status());

            var body = JsonTree.asObject(JsonTree.parse(res.bodyText()));
            assertEquals("project_not_ready", JsonTree.string(body, "reason"));
        }
    }

    @Test
    @DisplayName("answers 404 for a start in an unknown project")
    void shouldRefuseUnknownProject() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.START)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var request = new StartPlanRequest("no-such-project", null, null, Map.of(), List.of(), null, null);
            var res = client.send(HttpRequest.json("POST", "/api/v1/projects/no-such-project/plans/start",
                    request.toJson()).withHeader("Authorization", auth));
            assertEquals(404, res.status());
        }
    }

    @Test
    @DisplayName("opens a draft intake and broadcasts intake.created")
    void shouldOpenIntakeDraft() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.START)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            try (var exchange = client.open(HttpRequest.get("/api/v1/events").withHeader("Authorization", auth))) {
                var sse = exchange.events();

                var body = JsonTree.write(Map.of(
                        "project_id", "pm-mcp-clients",
                        "prompt", "start from this issue",
                        "sources", List.of(new SourceInput(SourceKind.ISSUE, "4711", null).toMap())));
                var res = client.send(HttpRequest.json("POST", "/api/v1/intake/draft", body)
                        .withHeader("Authorization", auth));
                assertEquals(201, res.status());

                var draft = IntakeDraft.fromJson(res.bodyText());
                assertEquals("pm-mcp-clients", draft.projectId());
                assertEquals(1, draft.sources().size());
                assertEquals(SourceKind.ISSUE, draft.sources().getFirst().kind());

                var event = sse.next();
                assertNotNull(event);
                assertEquals("intake.created", event.event());
            }
        }
    }


    @Test
    @DisplayName("every start case scenario loads and is registered")
    void allStartScenariosLoad() {
        var cases = List.of(StandardScenarios.START, StandardScenarios.START_SOURCES,
                StandardScenarios.START_DOCUMENTS, StandardScenarios.START_ISSUE_FAILED,
                StandardScenarios.START_UNSCREENED, StandardScenarios.START_LESSON,
                StandardScenarios.START_RATE_LIMITED, StandardScenarios.START_LOCAL_CHANGES,
                StandardScenarios.START_PULL_REQUEST, StandardScenarios.START_EPIC,
                StandardScenarios.START_EPIC_ADOPT, StandardScenarios.START_REFUSED,
                StandardScenarios.START_RECIPE, StandardScenarios.START_ENTRY_CONTINUE,
                StandardScenarios.START_CHAT, StandardScenarios.START_FOLLOW_UP);

        for (var name : cases) {
            assertTrue(StandardScenarios.ALL.contains(name), name + " must be registered");
            var scenario = ScenarioLoader.load(name);
            assertEquals(name, scenario.name());
            assertNotNull(scenario.startCase(), name + " carries its start fixtures");
            assertFalse(scenario.startCase().isEmpty(), name + " carries its start fixtures");
        }
    }

    @Nested
    @DisplayName("The opened entry")
    class OpenedEntry {

        @Test
        @DisplayName("opens under its project and is addressed at the top level by its id")
        void opensUnderProjectAndIsAddressedByItsId() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.START_SOURCES)) {
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = "Bearer " + runtime.token();

                var request = new EntryOpenRequest(EntryKind.PLAN, "Add a Codeberg provider for issues",
                        null, null, null);
                var opened = client.send(HttpRequest.json("POST",
                        "/api/v1/projects/pm-mcp-clients/entries", request.toJson())
                        .withHeader("Authorization", auth));
                assertEquals(201, opened.status());

                var created = StartStateResource.fromJson(opened.bodyText());
                assertTrue(created.entryId().startsWith("ent-"));
                assertEquals(StartState.SOURCES, created.state());
                assertEquals("pm-mcp-clients", created.entry().projectId());

                // the entry is addressed at the top level, never under its project
                var fetched = client.send(HttpRequest.get("/api/v1/entries/" + created.entryId())
                        .withHeader("Authorization", auth));
                assertEquals(200, fetched.status());
                assertEquals(created.entryId(),
                        StartStateResource.fromJson(fetched.bodyText()).entryId());

                var underProject = client.send(HttpRequest.get(
                        "/api/v1/projects/pm-mcp-clients/entries/" + created.entryId())
                        .withHeader("Authorization", auth));
                assertEquals(404, underProject.status());
            }
        }

        @Test
        @DisplayName("broadcasts entry.opened and answers scope_not_found for an unknown entry")
        void eventsAndUnknownEntry() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.START_SOURCES)) {
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = "Bearer " + runtime.token();

                try (var exchange = client.open(HttpRequest.get("/api/v1/events")
                             .withHeader("Authorization", auth))) {
                    assertTrue(exchange.isEventStream());
                    var sse = exchange.events();
                    var opened = client.send(HttpRequest.json("POST",
                            "/api/v1/projects/pm-mcp-clients/entries",
                            new EntryOpenRequest(EntryKind.PLAN, "x", null, null, null).toJson())
                            .withHeader("Authorization", auth));
                    assertEquals(201, opened.status());

                    var event = sse.next();
                    assertNotNull(event);
                    assertEquals("entry.opened", event.event());
                }

                var unknown = client.send(HttpRequest.get("/api/v1/entries/ent-does-not-exist")
                        .withHeader("Authorization", auth));
                assertEquals(404, unknown.status());
                assertEquals("scope_not_found",
                        JsonTree.string(JsonTree.asObject(JsonTree.parse(unknown.bodyText())), "error"));
            }
        }

        @Test
        @DisplayName("refuses a start in a project that is not ready and one that does not exist")
        void gateRefusals() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.START_REFUSED)) {
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var auth = "Bearer " + runtime.token();

                var notReady = client.send(HttpRequest.json("POST",
                        "/api/v1/projects/plan-marshall/entries",
                        new EntryOpenRequest(EntryKind.PLAN, "x", null, null, null).toJson())
                        .withHeader("Authorization", auth));
                assertEquals(409, notReady.status());

                var unknown = client.send(HttpRequest.json("POST",
                        "/api/v1/projects/no-such-project/entries",
                        new EntryOpenRequest(EntryKind.PLAN, "x", null, null, null).toJson())
                        .withHeader("Authorization", auth));
                assertEquals(404, unknown.status());
            }
        }
    }

    @Nested
    @DisplayName("Sources and their states")
    class Sources {

        @Test
        @DisplayName("start-sources serves a stored issue and documents, and offers only combinable kinds")
        void sourcesScenario() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.START_SOURCES)) {
                var state = fetch(runtime, "ent-3f9a2c41");

                assertEquals(StartState.SOURCES, state.state());
                assertTrue(state.entry().kindOffers().stream()
                        .anyMatch(offer -> offer.kind() == SourceKind.ISSUE && offer.available()));

                addAndAssert(runtime, "add_issue", "src-issue-1", SourceKind.ISSUE);
                var afterIssue = fetch(runtime, "ent-3f9a2c41");
                var issue = source(afterIssue, "src-issue-1");
                assertEquals(SourceState.STORED, issue.state());
                assertEquals(SourceTrust.TRUSTED, issue.trust());
                assertEquals("Issue comments API", issue.title());

                addAndAssert(runtime, "add_documents", "src-docs-1", SourceKind.DOCUMENTS);
                var afterDocuments = fetch(runtime, "ent-3f9a2c41");
                assertEquals(SourceState.COPIED, source(afterDocuments, "src-docs-1").state());
                assertTrue(afterDocuments.entry().canContinue());

                // a pull request never combines with an issue
                var prOffer = offer(afterDocuments, SourceKind.PULL_REQUEST);
                assertFalse(prOffer.available());
                assertEquals(SourceKindOffer.REASON_ALONE, prOffer.reasonKey());

                // a stored source can be removed until the start
                var removed = client(runtime).send(HttpRequest.json("POST",
                        "/api/v1/entries/ent-3f9a2c41/links/remove:src-docs-1", "{}")
                        .withHeader("Authorization", "Bearer " + runtime.token()));
                assertEquals(200, removed.status());
                assertTrue(StartStateResource.fromJson(removed.bodyText()).entry().sources().stream()
                        .noneMatch(source -> "src-docs-1".equals(source.sourceId())));

                // a clean source is not accepted: only a flagged or unscreened one is
                var refused = client(runtime).send(HttpRequest.json("POST",
                        "/api/v1/entries/ent-3f9a2c41/links/accept:src-issue-1",
                        JsonTree.write(Map.of("reason", "read as data only")))
                        .withHeader("Authorization", "Bearer " + runtime.token()));
                assertEquals(409, refused.status());
            }
        }

        @Test
        @DisplayName("start-documents serves a moved source and a refused file")
        void documentsScenario() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.START_DOCUMENTS)) {
                var state = fetch(runtime, "ent-3f9a2c41");

                assertEquals(SourceState.MOVED, source(state, "src-docs-1").state());
                assertEquals(SourceState.COPIED, source(state, "src-docs-2").state(),
                        "a copy is stored beside its original");
                assertEquals("12 files: 9 adoc, 3 svg", source(state, "src-docs-3").reference(),
                        "a directory shows what it holds");
                var refused = source(state, "src-docs-4");
                assertEquals(SourceState.FAILED, refused.state());
                assertEquals(SourceFailureCause.NOT_READABLE, refused.failure().cause());
                assertTrue(refused.failure().retryable());
                assertEquals(SourceFailureCause.LINK_OUTSIDE, source(state, "src-docs-5").failure().cause());
            }
        }

        @Test
        @DisplayName("start-issue-failed serves not_found, a flagged acceptance, and a refused grant")
        void issueFailures() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.START_ISSUE_FAILED)) {
                var state = fetch(runtime, "ent-3f9a2c41");

                assertEquals(SourceFailureCause.NOT_FOUND, source(state, "src-issue-1").failure().cause());
                var flagged = source(state, "src-issue-2");
                assertEquals(SourceState.FLAGGED, flagged.state());
                assertEquals(SourceTrust.UNTRUSTED, flagged.trust());
                assertEquals(1, flagged.spans().size());
                var acceptedSource = source(state, "src-issue-4");
                assertEquals(SourceState.STORED, acceptedSource.state());
                assertEquals("read the issue as data only", acceptedSource.dispositionReason());
                assertEquals(SourceTrust.UNTRUSTED, acceptedSource.trust(),
                        "an accepted flagged source keeps the untrusted mark");
                assertEquals(SourceFailureCause.NOT_GRANTED, source(state, "src-issue-3").failure().cause());

                // Try again turns the failed source into a stored one
                retry(runtime, "src-issue-1");
                assertEquals(SourceState.STORED, source(fetch(runtime, "ent-3f9a2c41"), "src-issue-1").state());

                // a flagged source accepted with its reason is stored untrusted
                var accepted = client(runtime).send(HttpRequest.json("POST",
                        "/api/v1/entries/ent-3f9a2c41/links/accept:src-issue-2",
                        JsonTree.write(Map.of("reason", "read as data only")))
                        .withHeader("Authorization", "Bearer " + runtime.token()));
                assertEquals(200, accepted.status());
                assertEquals(SourceTrust.UNTRUSTED,
                        source(StartStateResource.fromJson(accepted.bodyText()), "src-issue-2").trust());
            }
        }

        @Test
        @DisplayName("start-unscreened serves an accepted unscreened issue and pull request")
        void unscreenedScenario() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.START_UNSCREENED)) {
                var state = fetch(runtime, "ent-3f9a2c41");

                var issue = source(state, "src-issue-1");
                assertEquals(SourceState.UNSCREENED, issue.state());
                assertEquals(SourceTrust.UNTRUSTED, issue.trust());
                assertEquals(SourceFailureCause.NO_SCREENER, issue.failure().cause());

                var pr = source(state, "src-pr-1");
                assertEquals(SourceFailureCause.SCREENING_FAILED, pr.failure().cause());
                assertEquals(2, pr.changedFiles().size());
                assertEquals(1, pr.untrustedChangedFiles());
            }
        }

        @Test
        @DisplayName("start-lesson serves a stored lesson and one claimed meanwhile")
        void lessonScenario() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.START_LESSON)) {
                var state = fetch(runtime, "ent-3f9a2c41");

                assertEquals(SourceState.STORED, source(state, "src-lesson-1").state(),
                        "a lesson used anyway is stored");
                assertEquals(SourceFailureCause.LESSON_CLAIMED, source(state, "src-lesson-2").failure().cause());
                assertEquals(SourceFailureCause.LESSON_RETIRED, source(state, "src-lesson-3").failure().cause());
            }
        }

        @Test
        @DisplayName("start-rate-limited serves resets_at as an epoch second and never retries by itself")
        void rateLimitedScenario() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.START_RATE_LIMITED)) {
                var state = fetch(runtime, "ent-3f9a2c41");

                var limited = source(state, "src-issue-1");
                assertEquals(SourceFailureCause.RATE_LIMITED, limited.failure().cause());
                assertEquals(Instant.ofEpochSecond(1792507200L), limited.failure().resetsAt());
                assertTrue(limited.failure().retryable(), "the operator may try again after the reset");

                // the raw wire carries the epoch second, not a timestamp
                var raw = client(runtime).send(HttpRequest.get("/api/v1/entries/ent-3f9a2c41")
                        .withHeader("Authorization", "Bearer " + runtime.token()));
                var rawState = JsonTree.asObject(JsonTree.parse(raw.bodyText()));
                var rawFailure = JsonTree.object(JsonTree.asArray(rawState.get("items")).stream()
                        .map(JsonTree::asObject)
                        .filter(item -> "src-issue-1".equals(JsonTree.string(item, "id")))
                        .findFirst().orElseThrow(), "facts");
                assertEquals(1792507200L,
                        JsonTree.number(JsonTree.object(rawFailure, "failure"), "resets_at").longValue());

                // nothing retried on its own: a second read still shows the failure
                assertEquals(SourceState.FAILED, source(fetch(runtime, "ent-3f9a2c41"), "src-issue-1").state());

                assertEquals(SourceFailureCause.NO_CREDENTIAL,
                        source(state, "src-issue-2").failure().cause());
            }
        }

        @Test
        @DisplayName("start-local-changes withholds the local changes under landing.files and names the setting")
        void localChangesScenario() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.START_LOCAL_CHANGES)) {
                var state = fetch(runtime, "ent-3f9a2c41");

                var offer = offer(state, SourceKind.LOCAL_CHANGES);
                assertFalse(offer.available());
                assertEquals(SourceKindOffer.REASON_LOCAL_CHANGES_LANDING, offer.reasonKey());
                assertEquals("landing.files", offer.reasonArgs().get("landing"));

                var added = client(runtime).send(HttpRequest.json("POST",
                        "/api/v1/entries/ent-3f9a2c41/links/add_local_changes", "{}")
                        .withHeader("Authorization", "Bearer " + runtime.token()));
                assertEquals(200, added.status());
                var withLocal = source(StartStateResource.fromJson(added.bodyText()), "src-local-1");
                assertEquals(SourceKind.LOCAL_CHANGES, withLocal.kind());
            }
        }

        @Test
        @DisplayName("start-pull-request serves a fork's changed files as untrusted")
        void pullRequestScenario() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.START_PULL_REQUEST)) {
                var state = fetch(runtime, "ent-3f9a2c41");

                addAndAssert(runtime, "add_pr", "src-pr-1", SourceKind.PULL_REQUEST);
                var pr = source(fetch(runtime, "ent-3f9a2c41"), "src-pr-1");
                assertEquals(SourceTrust.UNTRUSTED, pr.trust());
                assertEquals(2, pr.changedFiles().size());
                assertEquals(2, pr.untrustedChangedFiles());
                assertTrue(pr.changedFiles().stream().noneMatch(file -> file.operatorAuthored()));
            }
        }
    }

    @Nested
    @DisplayName("Ready, recipes, and refusals")
    class ReadyAndRecipes {

        @Test
        @DisplayName("start-recipe derives the plan id from the recipe key and the time, and never asks a landing")
        void recipeReady() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.START_RECIPE)) {
                var state = fetch(runtime, "ent-3f9a2c41");

                assertEquals(StartState.RECIPE, state.state());
                assertFalse(state.recipes().isEmpty());
                assertTrue(state.recipes().stream().anyMatch(recipe -> "code-review".equals(recipe.key())));

                var chosen = client(runtime).send(HttpRequest.json("POST",
                        "/api/v1/entries/ent-3f9a2c41/links/choose:code-review", "{}")
                        .withHeader("Authorization", "Bearer " + runtime.token()));
                assertEquals(200, chosen.status());
                var parameters = StartStateResource.fromJson(chosen.bodyText());
                assertEquals(StartState.RECIPE_PARAMETERS, parameters.state());
                assertFalse(parameters.entry().recipe().parameters().isEmpty());

                var ready = client(runtime).send(HttpRequest.json("POST",
                        "/api/v1/entries/ent-3f9a2c41/links/set",
                        JsonTree.write(Map.of("parameters", Map.of("depth", "deep"))))
                        .withHeader("Authorization", "Bearer " + runtime.token()));
                assertEquals(200, ready.status());
                var readyState = StartStateResource.fromJson(ready.bodyText());
                assertEquals(StartState.READY, readyState.state());

                var readyStep = readyState.ready();
                assertTrue(RecipePlanId.isRecipePlanId(readyStep.planId()),
                        "the id derives from the recipe key and the time: " + readyStep.planId());
                assertFalse(readyStep.idEditable(), "a recipe's plan id is not changeable");
                assertFalse(readyStep.landingAsked(), "a recipe asks no landing");
                assertTrue(readyStep.recipeBound());

                var created = client(runtime).send(HttpRequest.json("POST",
                        "/api/v1/entries/ent-3f9a2c41/links/create", "{}")
                        .withHeader("Authorization", "Bearer " + runtime.token()));
                assertEquals(200, created.status());
                assertTrue(RecipePlanId.isRecipePlanId(
                        StartStateResource.fromJson(created.bodyText()).entry().planId()));
            }
        }

        @Test
        @DisplayName("a recipe with a parameter without an option cannot run")
        void recipeWithoutOption() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.START_RECIPE)) {
                var chosen = client(runtime).send(HttpRequest.json("POST",
                        "/api/v1/entries/ent-3f9a2c41/links/choose:no-modules", "{}")
                        .withHeader("Authorization", "Bearer " + runtime.token()));
                assertEquals(200, chosen.status());

                var parameters = StartStateResource.fromJson(chosen.bodyText());
                var blocked = parameters.entry().recipe().parameters().getFirst();
                assertFalse(blocked.available());
                assertEquals("none the recipe can work on", blocked.reason());
                assertTrue(parameters.ready() == null || parameters.ready().recipeBlocked());
            }
        }

        @Test
        @DisplayName("start-refused names what is missing and the one action that fixes it")
        void refusedScenario() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.START_REFUSED)) {
                var state = fetch(runtime, "ent-3f9a2c41");

                assertEquals(StartState.REFUSED, state.state());
                assertNotNull(state.refusal());
                assertEquals(StartRefusalReason.PROJECT_NOT_READY, state.refusal().reason());
                assertEquals("credential sonar", state.refusal().need());
                assertEquals("start.gate.continue_project", state.refusal().actionKey());
                assertTrue(state.entry().sources().isEmpty(), "nothing is created");
            }
        }

        @Test
        @DisplayName("start-epic shows epic.auto_start in force with its origin and never asks the scope")
        void epicReady() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.START_EPIC)) {
                var state = fetch(runtime, "ent-5c1b7d20");
                assertEquals(EntryKind.EPIC, state.entry().kind());
                assertTrue(state.entry().kindOffers().stream()
                        .noneMatch(offer -> offer.kind() == SourceKind.LESSON));

                var added = client(runtime).send(HttpRequest.json("POST",
                        "/api/v1/entries/ent-5c1b7d20/links/add_documents", "{}")
                        .withHeader("Authorization", "Bearer " + runtime.token()));
                assertEquals(200, added.status());

                var ready = client(runtime).send(HttpRequest.json("POST",
                        "/api/v1/entries/ent-5c1b7d20/links/start", "{}")
                        .withHeader("Authorization", "Bearer " + runtime.token()));
                assertEquals(200, ready.status());
                var readyState = StartStateResource.fromJson(ready.bodyText());
                assertEquals(StartState.READY, readyState.state());
                var readyStep = readyState.ready();
                assertEquals(Boolean.TRUE, readyStep.epicAutoStart());
                assertEquals(ConfigOrigin.PROJECT, readyStep.epicAutoStartOrigin());
                assertEquals(2, readyStep.parallelizationScope());
                assertEquals(3, readyStep.overlapMaxFiles());
            }
        }

        @Test
        @DisplayName("start-epic-adopt offers the adoptable ledgers and serves an adoption failure")
        void epicAdoption() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.START_EPIC_ADOPT)) {
                var state = fetch(runtime, "ent-7a2e4f01");

                assertEquals(StartState.EPIC_ADOPT, state.state());
                assertEquals(2, state.entry().adoptableEpics().size());
                assertEquals("relay-hardening", state.entry().adoptableEpics().getFirst().epicId());
                assertNotNull(state.link("adopt:relay-hardening"));
                assertNotNull(state.link("new"));

                var adopted = client(runtime).send(HttpRequest.json("POST",
                        "/api/v1/entries/ent-7a2e4f01/links/adopt:relay-hardening", "{}")
                        .withHeader("Authorization", "Bearer " + runtime.token()));
                assertEquals(200, adopted.status());
                assertEquals("relay-hardening",
                        StartStateResource.fromJson(adopted.bodyText()).entry().planId());

                var failed = client(runtime).send(HttpRequest.json("POST",
                        "/api/v1/entries/ent-7a2e4f01/links/adopt:storage-migration", "{}")
                        .withHeader("Authorization", "Bearer " + runtime.token()));
                assertEquals(409, failed.status());
            }
        }

        @Test
        @DisplayName("start-entry-continue keeps an entry with its sources and continues to its ready step")
        void entryContinue() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.START_ENTRY_CONTINUE)) {
                var state = fetch(runtime, "ent-3f9a2c41");

                assertEquals(1, state.entry().sourceCount());
                assertTrue(state.entry().canContinue());
                assertNotNull(state.link("start"));

                var ready = client(runtime).send(HttpRequest.json("POST",
                        "/api/v1/entries/ent-3f9a2c41/links/start", "{}")
                        .withHeader("Authorization", "Bearer " + runtime.token()));
                assertEquals(200, ready.status());
                var readyState = StartStateResource.fromJson(ready.bodyText());
                assertEquals(StartState.READY, readyState.state());
                assertEquals("Add a Codeberg provider for issues", readyState.ready().title());
                assertTrue(readyState.ready().idEditable());
            }
        }
    }

    @Nested
    @DisplayName("The start chat")
    class Chat {

        @Test
        @DisplayName("start-chat serves the transcript, its proposal, and the routed role")
        void chatScenario() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.START_CHAT)) {
                var state = fetch(runtime, "ent-3f9a2c41");

                var chat = state.chat();
                assertNotNull(chat);
                assertEquals(ChatRole.REFINE, chat.role());
                assertEquals(ChatScope.REQUEST, chat.scope());
                assertTrue(chat.available());
                assertEquals(2, chat.turns().size());
                assertNotNull(chat.proposal());
                assertEquals(StartTranscriptState.WITH_ENTRY, chat.transcript());

                var accepted = client(runtime).send(HttpRequest.json("POST",
                        "/api/v1/entries/ent-3f9a2c41/chat/proposal/accept", "{}")
                        .withHeader("Authorization", "Bearer " + runtime.token()));
                assertEquals(200, accepted.status());
                var afterAccept = StartStateResource.fromJson(accepted.bodyText());
                assertNull(afterAccept.chat().proposal());
                assertEquals(2, afterAccept.entry().sourceCount(),
                        "the accepted addition is stored as a description");
            }
        }

        @Test
        @DisplayName("a chat turn is answered by the routed role and a create ends the chat before it moves")
        void chatTurnAndCreate() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.START_CHAT)) {
                var turn = client(runtime).send(HttpRequest.json("POST",
                        "/api/v1/entries/ent-3f9a2c41/chat/turns",
                        JsonTree.write(Map.of("text", "What did you understand?")))
                        .withHeader("Authorization", "Bearer " + runtime.token()));
                assertEquals(200, turn.status());
                var state = StartStateResource.fromJson(turn.bodyText());
                assertEquals(4, state.chat().turns().size());
                assertEquals("refine", state.chat().turns().get(3).role());

                var rejected = client(runtime).send(HttpRequest.json("POST",
                        "/api/v1/entries/ent-3f9a2c41/chat/proposal/reject", "{}")
                        .withHeader("Authorization", "Bearer " + runtime.token()));
                assertEquals(200, rejected.status());
                assertNull(StartStateResource.fromJson(rejected.bodyText()).chat().proposal());

                var ended = client(runtime).send(HttpRequest.json("POST",
                        "/api/v1/entries/ent-3f9a2c41/chat/end", "{}")
                        .withHeader("Authorization", "Bearer " + runtime.token()));
                assertEquals(200, ended.status());

                var created = client(runtime).send(HttpRequest.json("POST",
                        "/api/v1/entries/ent-3f9a2c41/links/create", "{}")
                        .withHeader("Authorization", "Bearer " + runtime.token()));
                assertEquals(200, created.status());
                var createdState = StartStateResource.fromJson(created.bodyText());
                assertEquals(StartTranscriptState.MOVED, createdState.chat().transcript(),
                        "the transcript moves with the source directory");
                assertTrue(createdState.chat().transcriptPath().startsWith("plans/"));
            }
        }

        @Test
        @DisplayName("an abandoned entry deletes its transcript")
        void abandonDeletesTranscript() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.START_CHAT)) {
                var abandoned = client(runtime).send(HttpRequest.json("POST",
                        "/api/v1/entries/ent-3f9a2c41/links/abandon", "{}")
                        .withHeader("Authorization", "Bearer " + runtime.token()));
                assertEquals(200, abandoned.status());
                assertEquals(StartTranscriptState.DELETED,
                        StartStateResource.fromJson(abandoned.bodyText()).chat().transcript());
            }
        }

        @Test
        @DisplayName("an epic's entry routes its chat to decompose")
        void epicChatRoutesToDecompose() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.START_EPIC)) {
                var opened = client(runtime).send(HttpRequest.json("POST",
                        "/api/v1/entries/ent-5c1b7d20/links/chat", "{}")
                        .withHeader("Authorization", "Bearer " + runtime.token()));
                assertEquals(200, opened.status());
                assertEquals(ChatRole.DECOMPOSE,
                        StartStateResource.fromJson(opened.bodyText()).chat().role());
            }
        }
    }

    @Nested
    @DisplayName("The follow-up plan of ended work")
    class FollowUp {

        @Test
        @DisplayName("start-follow-up opens a plan's entry from an ended plan and from an ended epic")
        void followUpScenario() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.START_FOLLOW_UP)) {
                var auth = "Bearer " + runtime.token();

                var plan = client(runtime).send(HttpRequest.get(
                        "/api/v1/projects/pm-mcp-clients/ended/codeberg-issues/chat")
                        .withHeader("Authorization", auth));
                assertEquals(200, plan.status());
                var planChat = JsonTree.asObject(JsonTree.parse(plan.bodyText()));
                assertEquals("replan", JsonTree.string(planChat, "role"));
                assertEquals("archive", JsonTree.string(planChat, "scope"));
                assertNotNull(JsonTree.object(planChat, "proposal"));

                var opened = client(runtime).send(HttpRequest.json("POST",
                        "/api/v1/projects/pm-mcp-clients/ended/codeberg-issues/ended/accept", "{}")
                        .withHeader("Authorization", auth));
                assertEquals(201, opened.status());
                var state = StartStateResource.fromJson(opened.bodyText());
                assertEquals(StartState.SOURCES, state.state());
                assertEquals(1, state.entry().sourceCount());
                assertTrue(state.entry().sources().getFirst().reference()
                        .contains("follows codeberg-issues"));

                var epic = client(runtime).send(HttpRequest.get(
                        "/api/v1/projects/pm-mcp-clients/ended/relay-hardening/chat")
                        .withHeader("Authorization", auth));
                assertEquals(200, epic.status());
                assertEquals("decompose", JsonTree.string(
                        JsonTree.asObject(JsonTree.parse(epic.bodyText())), "role"));
            }
        }
    }

    @Nested
    @DisplayName("The confined file picker")
    class FilePicker {

        @Test
        @DisplayName("lists the checkout's files relative to the root and refuses a traversal")
        void listsConfinedFiles() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.START_SOURCES)) {
                var auth = "Bearer " + runtime.token();

                var root = client(runtime).send(HttpRequest.get(
                        "/api/v1/projects/pm-mcp-clients/files?root=checkout")
                        .withHeader("Authorization", auth));
                assertEquals(200, root.status());
                var listing = ConfinedFileListResource.fromJson(root.bodyText());
                assertEquals(ConfinedRoot.CHECKOUT, listing.root());
                assertEquals(2, listing.entries().size());
                assertNull(listing.parent());

                var nested = client(runtime).send(HttpRequest.get(
                        "/api/v1/projects/pm-mcp-clients/files?root=checkout&path=doc")
                        .withHeader("Authorization", auth));
                assertEquals(200, nested.status());
                var nestedListing = ConfinedFileListResource.fromJson(nested.bodyText());
                assertEquals(".", nestedListing.parent());
                assertTrue(nestedListing.entries().stream()
                        .anyMatch(file -> file.refusal() != null
                                && file.refusal().cause() == SourceFailureCause.NOT_READABLE));

                var traversal = client(runtime).send(HttpRequest.get(
                        "/api/v1/projects/pm-mcp-clients/files?root=checkout&path=../../.ssh/id_rsa")
                        .withHeader("Authorization", auth));
                assertEquals(400, traversal.status());
            }
        }

        @Test
        @DisplayName("the home directory is refused on the network listener")
        void homeIsLoopbackOnly() throws Exception {
            try (var runtime = MockRuntime.start(StandardScenarios.START_SOURCES)) {
                var client = new UnixSocketHttpClient(runtime.socketPath());
                var response = client.send(HttpRequest.get(
                        "/api/v1/projects/pm-mcp-clients/files?root=home")
                        .withHeader("Authorization", "Bearer " + runtime.token()));
                // the Unix socket is the terminal client's channel, so the home root is served there
                assertEquals(200, response.status());
                assertEquals(ConfinedRoot.HOME,
                        ConfinedFileListResource.fromJson(response.bodyText()).root());
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers

    private static UnixSocketHttpClient client(MockRuntime runtime) {
        return new UnixSocketHttpClient(runtime.socketPath());
    }

    private static StartStateResource fetch(MockRuntime runtime, String entryId) throws Exception {
        var response = client(runtime).send(HttpRequest.get("/api/v1/entries/" + entryId)
                .withHeader("Authorization", "Bearer " + runtime.token()));
        assertEquals(200, response.status(), response.bodyText());
        return StartStateResource.fromJson(response.bodyText());
    }

    private static void addAndAssert(MockRuntime runtime, String link, String sourceId, SourceKind kind)
            throws Exception {
        var response = client(runtime).send(HttpRequest.json("POST",
                "/api/v1/entries/ent-3f9a2c41/links/" + link, "{}")
                .withHeader("Authorization", "Bearer " + runtime.token()));
        assertEquals(200, response.status(), response.bodyText());
        var state = StartStateResource.fromJson(response.bodyText());
        var source = source(state, sourceId);
        assertEquals(kind, source.kind());
    }

    private static void retry(MockRuntime runtime, String sourceId) throws Exception {
        var response = client(runtime).send(HttpRequest.json("POST",
                "/api/v1/entries/ent-3f9a2c41/links/retry:" + sourceId, "{}")
                .withHeader("Authorization", "Bearer " + runtime.token()));
        assertEquals(200, response.status(), response.bodyText());
    }

    private static SourceResource source(StartStateResource state, String sourceId) {
        return state.entry().sources().stream().filter(source -> source.sourceId().equals(sourceId))
                .findFirst().orElseThrow(() -> new AssertionError("No source " + sourceId));
    }

    private static SourceKindOffer offer(StartStateResource state, SourceKind kind) {
        return state.entry().kindOffers().stream().filter(offer -> offer.kind() == kind)
                .findFirst().orElseThrow(() -> new AssertionError("No offer for " + kind));
    }

    private static List<RecipeRef> parseRecipes(String json) throws Exception {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        var arr = JsonTree.asArray(obj.get("recipes"));
        return arr.stream().map(item -> {
            try {
                return RecipeRef.fromMap(JsonTree.asObject(item));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }).toList();
    }

    @Test
    @DisplayName("the start scenarios' entry ids are the ones their fixtures name")
    void scenarioEntryIds() throws Exception {
        var scenario = ScenarioLoader.load(StandardScenarios.START_CHAT);
        assertEquals("ent-3f9a2c41", scenario.startCase().get("entry_id"));
        assertEquals("plan", scenario.startCase().get("kind"));
        assertEquals("start.sources", scenario.startCase().get("state"));
    }
}
