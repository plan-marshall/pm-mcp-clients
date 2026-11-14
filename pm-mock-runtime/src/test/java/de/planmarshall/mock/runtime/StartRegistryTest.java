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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.config.ConfigOrigin;
import de.planmarshall.api.start.AdoptionFailureReason;
import de.planmarshall.api.start.EntryKind;
import de.planmarshall.api.start.EntryOpenRequest;
import de.planmarshall.api.start.ScreeningVerdict;
import de.planmarshall.api.start.SourceKind;
import de.planmarshall.api.start.SourceKindOffer;
import de.planmarshall.api.start.SourceState;
import de.planmarshall.api.start.SourceTrust;
import de.planmarshall.api.start.StartState;
import de.planmarshall.mock.scenario.RouteDefinition;
import de.planmarshall.mock.scenario.Scenario;
import de.planmarshall.mock.scenario.StateDefinition;

@DisplayName("StartRegistry")
@EnableGeneratorController
class StartRegistryTest {

    private static final String BASE_STATE = "running_work.active";
    private static final String START_STATE = "start.state";

    @Test
    @DisplayName("seeds the entry and the recipes from the start state merged over the base state")
    void seedsFromTheStateFixture() {
        var projectId = Generators.letterStrings(3, 8).next().toLowerCase();
        var entryId = "ent-" + Generators.letterStrings(4, 10).next().toLowerCase();
        var recipeKey = "recipe-" + Generators.letterStrings(3, 8).next().toLowerCase();
        var seeded = new StartRegistry(_ -> {
        });
        seeded.init(scenario(
                Map.of("start", Map.of("recipes", List.of(Map.of("key", recipeKey, "name", recipeKey)))),
                Map.of("start", Map.of("project_id", projectId, "kind", "plan", "state", "start.sources",
                        "entry_id", entryId, "sources", List.of()))), START_STATE);

        var entry = seeded.find(entryId);
        assertNotNull(entry);
        assertEquals(projectId, entry.entry().projectId());
        assertEquals(1, seeded.recipes(projectId).size());
        assertEquals(recipeKey, seeded.recipes(projectId).getFirst().key());
    }

    @Test
    @DisplayName("falls back to the GET /api/v1/recipes route when the state names no recipe")
    void seedsRecipesFromTheRoute() {
        var projectId = Generators.letterStrings(3, 8).next().toLowerCase();
        var entryId = "ent-" + Generators.letterStrings(4, 10).next().toLowerCase();
        var recipeKey = "recipe-" + Generators.letterStrings(3, 8).next().toLowerCase();
        var seeded = new StartRegistry(_ -> {
        });
        seeded.init(scenario(Map.of(),
                Map.of("start", Map.of("project_id", projectId, "kind", "plan", "state", "start.sources",
                        "entry_id", entryId)),
                Map.of("GET /api/v1/recipes", RouteDefinition.of(Map.of("recipes",
                        List.of(Map.of("key", recipeKey, "name", recipeKey)))))), START_STATE);

        assertNotNull(seeded.find(entryId));
        assertEquals(recipeKey, seeded.recipes(projectId).getFirst().key());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = SourceKind.class, names = {"ISSUE", "PULL_REQUEST", "LESSON", "LOCAL_CHANGES"})
    @DisplayName("adds a source the state scripts no outcome for as the kind's default source")
    void addsTheDefaultSource(SourceKind kind) throws Exception {
        var entryId = seedPlanEntry();

        var added = registry.act(entryId, "add_" + kind.wireValue(), Map.of());

        assertNotNull(added);
        var sources = added.entry().sources();
        assertEquals(1, sources.size());
        assertEquals(kind, sources.getFirst().kind());
        assertFalse(sources.getFirst().reference().isBlank(), "the default source names what it brings in");
    }

    @Test
    @DisplayName("refuses a source kind no start carries with act_not_available")
    void refusesAnUnknownSourceKind() {
        var entryId = seedPlanEntry();
        var unknownKind = Generators.letterStrings(4, 10).next().toLowerCase();

        var refused = assertThrows(StartRegistry.ActRefused.class,
                () -> registry.act(entryId, "add_" + unknownKind, Map.of()));

        assertEquals("act_not_available", refused.error());
        assertTrue(registry.find(entryId).entry().sources().isEmpty(), "a refused add adds nothing");
    }

    @Test
    @DisplayName("asks how documents are brought in, then moves them as the operator chooses")
    void documentsModeAskedThenMoved() throws Exception {
        var entryId = seedPlanEntry();

        var asked = registry.act(entryId, "add_documents", Map.of());
        assertNotNull(asked);
        assertEquals(SourceState.CHECKING, asked.entry().sources().getFirst().state());

        var moved = registry.act(entryId, "documents_mode:move", Map.of());

        assertNotNull(moved);
        assertEquals(SourceState.MOVED, moved.entry().sources().getFirst().state());
    }

    @Test
    @DisplayName("refuses a documents mode that is neither copy nor move with invalid_arguments")
    void refusesAnUnknownDocumentsMode() throws Exception {
        var entryId = seedPlanEntry();
        registry.act(entryId, "add_documents", Map.of());
        var unknownMode = Generators.letterStrings(4, 10).next().toLowerCase();

        var refused = assertThrows(StartRegistry.ActRefused.class,
                () -> registry.act(entryId, "documents_mode:" + unknownMode, Map.of()));

        assertEquals("invalid_arguments", refused.error());
        assertEquals(SourceState.CHECKING, registry.find(entryId).entry().sources().getFirst().state(),
                "a refused mode leaves the document waiting for the operator's choice");
    }

    @Nested
    @DisplayName("Adopting an epic from elsewhere")
    class Adopting {

        @Test
        @DisplayName("adopts an offered epic: the start ends created and broadcasts epic.started")
        void adoptsTheEpic() throws Exception {
            var epicId = lowerLetters();
            var title = lowerLetters();
            var entryId = seedEpicEntry(epicId, title, null);

            var adopted = registry.act(entryId, "adopt:" + epicId, Map.of());

            assertNotNull(adopted);
            assertEquals(StartState.READY, adopted.entry().state());
            assertEquals(epicId, adopted.entry().planId());
            assertEquals(title, adopted.entry().title());
            assertEquals(1, events.size());
            assertEquals("epic.started", events.getFirst().name());
            assertEquals(epicId, events.getFirst().data().get("epic_id"));
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(AdoptionFailureReason.class)
        @DisplayName("a ledger that cannot be adopted answers the start in start-epic.adopt-failed with its cause")
        void showsTheFailedAdoption(AdoptionFailureReason reason) throws Exception {
            var epicId = lowerLetters();
            var file = lowerLetters() + ".json";
            var entryId = seedEpicEntry(epicId, lowerLetters(),
                    Map.of("epic_id", epicId, "reason", reason.wireValue(), "file", file));

            var failed = registry.act(entryId, "adopt:" + epicId, Map.of());

            assertNotNull(failed, "a failed adoption is the start's state, not a refused act");
            assertEquals(StartState.EPIC_ADOPT_FAILED, failed.entry().state());
            assertNotNull(failed.adoptionFailure());
            assertEquals(epicId, failed.adoptionFailure().epicId());
            assertEquals(reason, failed.adoptionFailure().reason());
            assertTrue(events.isEmpty(), "nothing is created, so no epic.started is broadcast");
        }

        @Test
        @DisplayName("Try again after a failed adoption offers the adoptable epics anew")
        void triesAgain() throws Exception {
            var epicId = lowerLetters();
            var entryId = seedEpicEntry(epicId, lowerLetters(),
                    Map.of("epic_id", epicId, "reason", AdoptionFailureReason.LEDGER_TERMINAL.wireValue()));
            registry.act(entryId, "adopt:" + epicId, Map.of());

            var again = registry.act(entryId, "retry", Map.of());

            assertNotNull(again);
            assertEquals(StartState.EPIC_ADOPT, again.entry().state());
            assertNull(again.adoptionFailure());
        }

        @Test
        @DisplayName("Back after a failed adoption offers the adoptable epics without the failure's cause")
        void goesBackFromTheFailedAdoption() throws Exception {
            var epicId = lowerLetters();
            var entryId = seedEpicEntry(epicId, lowerLetters(),
                    Map.of("epic_id", epicId, "reason", AdoptionFailureReason.LEDGER_INVALID.wireValue()));
            registry.act(entryId, "adopt:" + epicId, Map.of());

            var back = registry.act(entryId, "back", Map.of());

            assertNotNull(back);
            assertEquals(StartState.EPIC_ADOPT, back.entry().state());
            assertNull(back.entry().adoptionFailure(), "the entry no longer carries the failed adoption");
            assertNull(back.adoptionFailure());
            assertEquals(epicId, back.entry().adoptableEpics().getFirst().epicId());
        }

        @Test
        @DisplayName("refuses to adopt an epic the ledger does not offer with act_not_available")
        void refusesAnEpicNotOffered() {
            var entryId = seedEpicEntry(lowerLetters(), lowerLetters(), null);
            var other = lowerLetters() + "-other";

            var refused = assertThrows(StartRegistry.ActRefused.class,
                    () -> registry.act(entryId, "adopt:" + other, Map.of()));

            assertEquals("act_not_available", refused.error());
        }

        @Test
        @DisplayName("an epic that started offers no further adoption")
        void refusesAdoptionOnceStarted() throws Exception {
            var epicId = lowerLetters();
            var entryId = seedEpicEntry(epicId, lowerLetters(), null);
            registry.act(entryId, "adopt:" + epicId, Map.of());

            var refused = assertThrows(StartRegistry.ActRefused.class,
                    () -> registry.act(entryId, "adopt:" + epicId, Map.of()));

            assertEquals("act_not_available", refused.error());
            assertEquals(1, events.size(), "the epic started once");
        }

        @Test
        @DisplayName("Start a new epic leaves the adoption for the list of sources")
        void startsANewEpicInstead() throws Exception {
            var entryId = seedEpicEntry(lowerLetters(), lowerLetters(), null);

            var fresh = registry.act(entryId, "new", Map.of());

            assertNotNull(fresh);
            assertEquals(StartState.SOURCES, fresh.entry().state());
            assertTrue(fresh.entry().adoptableEpics().isEmpty());
        }
    }

    @Nested
    @DisplayName("Opening an entry")
    class Opening {

        @Test
        @DisplayName("opens nothing in a project the runtime does not know")
        void unknownProject() {
            seedProject(lowerLetters(), Map.of());

            assertNull(registry.open(lowerLetters() + "-unknown", EntryOpenRequest.of(EntryKind.PLAN)));
        }

        @Test
        @DisplayName("a plan from sources first chooses between sources and a recipe")
        void planChoosesFirst() {
            var projectId = lowerLetters();
            seedProject(projectId, Map.of());

            var opened = registry.open(projectId, EntryOpenRequest.of(EntryKind.PLAN));

            assertNotNull(opened);
            assertEquals(StartState.CHOOSE, opened.entry().state());
            assertEquals(projectId, opened.entry().projectId());
        }

        @Test
        @DisplayName("a plan opened with a recipe goes to its sources with the recipe and no chat")
        void planWithRecipe() {
            var projectId = lowerLetters();
            var recipeKey = "recipe-" + lowerLetters();
            seedProject(projectId, Map.of("recipes", List.of(Map.of("key", recipeKey, "name", recipeKey))));

            var opened = registry.open(projectId,
                    new EntryOpenRequest(EntryKind.PLAN, null, null, recipeKey, null));

            assertNotNull(opened);
            assertEquals(StartState.SOURCES, opened.entry().state());
            assertNotNull(opened.entry().recipe());
            assertEquals(recipeKey, opened.entry().recipe().key());
            assertFalse(opened.entry().chatAvailable(), "a recipe's start offers no chat");
        }

        @Test
        @DisplayName("an epic opens on the adoption where the ledger holds epics of elsewhere")
        void epicWithAdoptableEpics() {
            var projectId = lowerLetters();
            var epicId = lowerLetters();
            seedProject(projectId, Map.of("adoptable_epics", List.of(Map.of("epic_id", epicId, "title", epicId))));

            var opened = registry.open(projectId, EntryOpenRequest.of(EntryKind.EPIC));

            assertNotNull(opened);
            assertEquals(StartState.EPIC_ADOPT, opened.entry().state());
            assertEquals(epicId, opened.entry().adoptableEpics().getFirst().epicId());
        }

        @Test
        @DisplayName("an epic opens on its sources where no epic waits for adoption")
        void epicWithoutAdoptableEpics() {
            var projectId = lowerLetters();
            seedProject(projectId, Map.of());

            var opened = registry.open(projectId, EntryOpenRequest.of(EntryKind.EPIC));

            assertNotNull(opened);
            assertEquals(StartState.SOURCES, opened.entry().state());
        }
    }

    @Nested
    @DisplayName("Creating the work")
    class Creating {

        @Test
        @DisplayName("creates the plan under the id the operator set and broadcasts plan.started")
        void createsUnderTheOperatorsId() throws Exception {
            var entryId = seedPlanEntry();
            registry.act(entryId, "add_description", Map.of("text", lowerLetters()));
            var planId = lowerLetters();

            var created = registry.act(entryId, "create", Map.of("plan_id", "  " + planId + " "));

            assertNotNull(created);
            assertEquals(StartState.READY, created.entry().state());
            assertEquals(planId, created.entry().planId());
            assertEquals("plan.started", events.getLast().name());
            assertEquals(planId, events.getLast().data().get("plan_id"));
        }

        @Test
        @DisplayName("derives the id from the title where the operator set none")
        void derivesTheIdFromTheTitle() throws Exception {
            var entryId = seedPlanEntry();
            var first = lowerLetters();
            var second = lowerLetters();
            registry.act(entryId, "add_description", Map.of("text", lowerLetters()));

            var created = registry.act(entryId, "create",
                    Map.of("title", first + " " + second, "plan_id", " "));

            assertNotNull(created);
            assertEquals(first + " " + second, created.entry().title());
            assertEquals(first + "-" + second, created.entry().planId());
        }

        @Test
        @DisplayName("creates an epic and broadcasts epic.started")
        void createsAnEpic() throws Exception {
            var entryId = seedEpicEntry(null, null, null);
            registry.act(entryId, "add_description", Map.of("text", lowerLetters()));
            var epicId = lowerLetters();

            var created = registry.act(entryId, "create", Map.of("plan_id", epicId));

            assertNotNull(created);
            assertEquals(epicId, created.entry().planId());
            assertEquals("epic.started", events.getLast().name());
            assertEquals(epicId, events.getLast().data().get("epic_id"));
        }
    }

    @Nested
    @DisplayName("An epic's ready step")
    class EpicReady {

        @Test
        @DisplayName("shows the project's values in force as the state names them")
        void showsTheProjectsValues() {
            var scope = Generators.integers(2, 9).next();
            var overlap = Generators.integers(1, 20).next();
            var entryId = seedEntry(Map.of("kind", "epic", "state", "start.ready", "title", lowerLetters(),
                    "epic_auto_start", false, "epic_auto_start_origin", ConfigOrigin.PROJECT.wire(),
                    "parallelization_scope", scope, "overlap_max_files", overlap));

            var ready = registry.find(entryId).ready();

            assertNotNull(ready);
            assertEquals(Boolean.FALSE, ready.epicAutoStart());
            assertEquals(ConfigOrigin.PROJECT, ready.epicAutoStartOrigin());
            assertEquals(scope, ready.parallelizationScope());
            assertEquals(overlap, ready.overlapMaxFiles());
        }

        @Test
        @DisplayName("falls back to the machine's defaults for values out of their range")
        void fallsBackToTheDefaults() {
            var entryId = seedEntry(Map.of("kind", "epic", "state", "start.ready", "title", lowerLetters(),
                    "epic_auto_start_origin", "origin-" + lowerLetters(),
                    "parallelization_scope", 0, "overlap_max_files", -1));

            var ready = registry.find(entryId).ready();

            assertNotNull(ready);
            assertEquals(Boolean.TRUE, ready.epicAutoStart(), "epic.auto_start defaults to on");
            assertEquals(ConfigOrigin.MACHINE, ready.epicAutoStartOrigin());
            assertEquals(1, ready.parallelizationScope(), "a scope below one is the machine default 1");
            assertEquals(0, ready.overlapMaxFiles(), "a negative overlap is the default 0");
        }
    }

    @Nested
    @DisplayName("Trying a failed source again")
    class Retrying {

        @Test
        @DisplayName("Try again on a rate-limited issue fetches and screens it")
        void retriesARateLimitedIssue() throws Exception {
            var sourceId = "src-" + lowerLetters();
            var entryId = seedPlanEntryWith(List.of(Map.of("source_id", sourceId, "kind", "issue",
                    "state", "failed", "failure", Map.of("cause", "rate_limited", "forge", "Codeberg"))));

            var retried = registry.act(entryId, "retry:" + sourceId, Map.of());

            assertNotNull(retried);
            var source = retried.entry().sources().getFirst();
            assertEquals(SourceState.STORED, source.state());
            assertEquals(SourceTrust.TRUSTED, source.trust());
            assertEquals(ScreeningVerdict.CLEAN, source.verdict());
            assertNull(source.failure());
        }

        @Test
        @DisplayName("local changes that are gone offer no Try again")
        void refusesANonRetryableFailure() {
            var sourceId = "src-" + lowerLetters();
            var entryId = seedPlanEntryWith(List.of(Map.of("source_id", sourceId, "kind", "local_changes",
                    "state", "failed", "failure", Map.of("cause", "no_local_changes"))));

            var refused = assertThrows(StartRegistry.ActRefused.class,
                    () -> registry.act(entryId, "retry:" + sourceId, Map.of()));

            assertEquals("act_not_available", refused.error());
            assertEquals(SourceState.FAILED, registry.find(entryId).entry().sources().getFirst().state());
        }

        @ParameterizedTest(name = "{0} after {1}")
        @CsvSource({"lesson, lesson_store_damaged, STORED", "documents, not_readable, COPIED"})
        @DisplayName("Try again on a source of this machine stores it in the state of its kind")
        void retriesALocalSource(String kind, String cause, SourceState expected) throws Exception {
            var sourceId = "src-" + lowerLetters();
            var entryId = seedPlanEntryWith(List.of(Map.of("source_id", sourceId, "kind", kind,
                    "state", "failed", "failure", Map.of("cause", cause))));

            var retried = registry.act(entryId, "retry:" + sourceId, Map.of());

            assertNotNull(retried);
            var source = retried.entry().sources().getFirst();
            assertEquals(expected, source.state(), "only documents are copied; a lesson is stored");
            assertNull(source.failure());
            assertNull(source.verdict(), "a source of this machine is never screened");
        }

        @Test
        @DisplayName("refuses Try again for a source the entry does not hold")
        void refusesAnUnknownSource() {
            var entryId = seedPlanEntry();

            var refused = assertThrows(StartRegistry.ActRefused.class,
                    () -> registry.act(entryId, "retry:src-" + lowerLetters(), Map.of()));

            assertEquals("act_not_available", refused.error());
        }
    }

    @Nested
    @DisplayName("The chat about the request")
    class Chat {

        @Test
        @DisplayName("an entry the state scripts no chat for derives the chat from its sources")
        void derivedFromTheSources() {
            var waitingId = seedPlanEntry();
            var waiting = registry.find(waitingId).entry();
            var storedId = seedEntry(Map.of("sources", List.of(storedDescription())));
            var stored = registry.find(storedId).entry();

            assertFalse(waiting.chatAvailable());
            assertEquals("start.chat.waits", waiting.chatDisabledReason(), "no source is stored yet");
            assertTrue(stored.chatAvailable(), "a stored source of this machine opens the chat");
            assertNull(stored.chatDisabledReason());
        }

        private static Map<String, Object> storedDescription() {
            return Map.of("source_id", "src-" + lowerLetters(), "kind", "description", "state", "stored",
                    "reference", lowerLetters());
        }
    }

    @Nested
    @DisplayName("The kinds a plan is not offered")
    class RefusedKinds {

        @Test
        @DisplayName("names the landing, the missing local changes and the missing lessons as the reasons")
        void namesTheReasons() {
            var entryId = seedEntry(Map.of("landing", "landing.files", "lessons_available", false,
                    "local_changes", false));

            var offers = registry.find(entryId).entry().kindOffers();

            assertEquals(SourceKindOffer.REASON_LOCAL_CHANGES_LANDING, reasonOf(offers, SourceKind.LOCAL_CHANGES));
            assertEquals("start.sources.no_lessons", reasonOf(offers, SourceKind.LESSON));
        }

        @Test
        @DisplayName("names missing local changes where the landing commits")
        void namesMissingLocalChanges() {
            var entryId = seedEntry(Map.of("local_changes", false));

            var offers = registry.find(entryId).entry().kindOffers();

            assertEquals("start.sources.no_local_changes", reasonOf(offers, SourceKind.LOCAL_CHANGES));
        }

        @Test
        @DisplayName("an issue in the list leaves the lesson, the local changes and the pull request alone")
        void issueLeavesTheOthersAlone() throws Exception {
            var entryId = seedPlanEntry();

            var withIssue = registry.act(entryId, "add_issue", Map.of());

            assertNotNull(withIssue);
            var offers = withIssue.entry().kindOffers();
            assertEquals(SourceKindOffer.REASON_ALONE, reasonOf(offers, SourceKind.LESSON));
            assertEquals(SourceKindOffer.REASON_ALONE, reasonOf(offers, SourceKind.PULL_REQUEST));
            assertEquals(SourceKindOffer.REASON_ALONE, reasonOf(offers, SourceKind.LOCAL_CHANGES));
        }

        private static String reasonOf(List<SourceKindOffer> offers, SourceKind kind) {
            return offers.stream().filter(offer -> offer.kind() == kind).findFirst().orElseThrow().reasonKey();
        }
    }

    @Test
    @DisplayName("answers nothing for an entry it does not hold")
    void unknownEntry() {
        seedPlanEntry();
        var unknown = "ent-" + lowerLetters() + "-unknown";

        assertAll(
                () -> assertNull(registry.find(unknown)),
                () -> assertNull(registry.act(unknown, "abandon", Map.of())));
    }

    /** The registry the act tests drive, seeded by {@link #seedPlanEntry()}. */
    private StartRegistry registry;

    /** The events the registry broadcast since it was seeded. */
    private final List<StartRegistry.StartEvent> events = new ArrayList<>();

    private static String lowerLetters() {
        return Generators.letterStrings(4, 10).next().toLowerCase(Locale.ROOT);
    }

    /**
     * Seeds an epic's entry in the sources step.
     *
     * @param epicId  the adoptable epic, or {@code null} for none
     * @param title   its title
     * @param failure the scripted adoption failure, or {@code null}
     * @return the entry's id
     */
    private String seedEpicEntry(@Nullable String epicId, @Nullable String title,
            @Nullable Map<String, Object> failure) {
        var fixture = new LinkedHashMap<String, Object>();
        fixture.put("kind", "epic");
        fixture.put("sources", List.of());
        if (epicId != null) {
            fixture.put("adoptable_epics", List.of(Map.of("epic_id", epicId, "title", title)));
        }
        if (failure != null) {
            fixture.put("adoption_failure", failure);
        }
        return seedEntry(fixture);
    }

    private String seedPlanEntryWith(List<Map<String, Object>> sources) {
        return seedEntry(Map.of("sources", sources));
    }

    /**
     * Seeds an entry in the sources step; the fixture's members override the plan defaults.
     *
     * @param members the members of the start fixture
     * @return the entry's id
     */
    private String seedEntry(Map<String, Object> members) {
        var entryId = "ent-" + lowerLetters();
        var fixture = new LinkedHashMap<String, Object>();
        fixture.put("project_id", lowerLetters());
        fixture.put("kind", "plan");
        fixture.put("state", "start.sources");
        fixture.put("entry_id", entryId);
        fixture.putAll(members);
        seedRegistry(fixture);
        return entryId;
    }

    /**
     * Seeds a project that holds no entry.
     *
     * @param projectId the project
     * @param members   further members of the start fixture
     */
    private void seedProject(String projectId, Map<String, Object> members) {
        var fixture = new LinkedHashMap<String, Object>();
        fixture.put("project_id", projectId);
        fixture.putAll(members);
        seedRegistry(fixture);
    }

    private void seedRegistry(Map<String, Object> fixture) {
        events.clear();
        registry = new StartRegistry(events::add);
        registry.init(scenario(Map.of(), Map.of("start", fixture)), START_STATE);
    }

    /**
     * Seeds a plan's entry in the sources step with no source and no scripted add outcome.
     *
     * @return the entry's id
     */
    private String seedPlanEntry() {
        return seedEntry(Map.of("sources", List.of()));
    }

    private static Scenario scenario(Map<String, Object> baseFixture, Map<String, Object> startFixture) {
        return scenario(baseFixture, startFixture, Map.of());
    }

    private static Scenario scenario(Map<String, Object> baseFixture, Map<String, Object> startFixture,
            Map<String, RouteDefinition> routes) {
        var states = new LinkedHashMap<String, StateDefinition>();
        if (!baseFixture.isEmpty()) {
            states.put(BASE_STATE, state(BASE_STATE, baseFixture));
        }
        if (!startFixture.isEmpty()) {
            states.put(START_STATE, state(START_STATE, startFixture));
        }
        return new Scenario(1, "registry-test", "StartRegistry test", START_STATE, "CODE", routes,
                states);
    }

    private static StateDefinition state(String stateId, Map<String, Object> fixture) {
        var representation = new LinkedHashMap<String, Object>();
        representation.put("scope", Map.of("kind", "workspace", "project_id", "p"));
        representation.put("state", Map.of("state_id", stateId, "kind", "operator_wait"));
        representation.put("facts", Map.of("runtime", fixture));
        return new StateDefinition(representation, Map.of(), List.of(), null);
    }
}
