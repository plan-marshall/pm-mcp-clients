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

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;


import org.jspecify.annotations.Nullable;

import de.planmarshall.api.chat.ChatSubject;
import de.planmarshall.api.chat.ChatSubjectKind;
import de.planmarshall.api.config.ConfigOrigin;
import de.planmarshall.api.interaction.OperatorStateResource;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.onboarding.DocumentsMode;
import de.planmarshall.api.source.git.ChangedFile;
import de.planmarshall.api.start.AdoptableEpic;
import de.planmarshall.api.start.AdoptionFailure;
import de.planmarshall.api.start.AdoptionFailureReason;
import de.planmarshall.api.start.ConfinedFile;
import de.planmarshall.api.start.ConfinedFileListResource;
import de.planmarshall.api.start.ConfinedPath;
import de.planmarshall.api.start.ConfinedRoot;
import de.planmarshall.api.start.EntryKind;
import de.planmarshall.api.start.EntryOpenRequest;
import de.planmarshall.api.start.EntryResource;
import de.planmarshall.api.start.RecipeParameter;
import de.planmarshall.api.start.RecipePlanId;
import de.planmarshall.api.start.RecipeRef;
import de.planmarshall.api.start.ScreeningSpan;
import de.planmarshall.api.start.ScreeningVerdict;
import de.planmarshall.api.start.SourceFailure;
import de.planmarshall.api.start.SourceFailureCause;
import de.planmarshall.api.start.SourceKind;
import de.planmarshall.api.start.SourceKindOffer;
import de.planmarshall.api.start.SourceResource;
import de.planmarshall.api.start.SourceState;
import de.planmarshall.api.start.SourceTrust;
import de.planmarshall.api.start.StartReadyResource;
import de.planmarshall.api.start.StartRefusal;
import de.planmarshall.api.start.StartRefusalReason;
import de.planmarshall.api.start.StartState;
import de.planmarshall.api.start.StartStateResource;
import de.planmarshall.api.start.UnscreenedDisposition;
import de.planmarshall.mock.scenario.Scenario;

/**
 * The live state of the starts of the mock runtime (UC-STA-1 to UC-STA-5, UC-STA-7 to UC-STA-9):
 * the open entries, their sources, the recipes offered in a project, the adoptable epics, the ended
 * work, and the confined file listing of the picker.
 * <p>
 * An entry's chat is not held here: it is served by {@link ChatRegistry} on the entry's chat routes,
 * and an entry's state carries no chat member. The registry only derives whether the entry offers its
 * chat yet ({@code chat_available}), names the ended work whose chats are about the archived record
 * ({@link #endedSubjects()}), and opens the start of the follow-up plan such a chat proposed and the
 * operator accepted ({@link #openFollowUp(String, String)}).
 * <p>
 * The state is seeded from the active state's operator representation and the scenario's routes, and
 * then driven by the operator's acts: every act returns the entry's new {@link StartStateResource}, so
 * a client reads the same contract it would read from the runtime. Nothing is persisted; a scenario
 * switch or a reset re-seeds the state.
 * <p>
 * The registry holds the two invariants the contract is about: a rate-limited source is never
 * retried by the runtime on its own (only the operator's <em>Try again</em> does), and a pull
 * request's changed files stay untrusted unless the operator authored them.
 */
final class StartRegistry {

    private static final String KEY_KEY = "key";
    private static final String KEY_KIND = "kind";
    private static final String KEY_ENTRY_ID = "entry_id";
    private static final String KEY_STATE = "state";
    private static final String KEY_PROJECT_ID = "project_id";
    private static final String KEY_SOURCES = "sources";
    private static final String KEY_RECIPES = "recipes";
    private static final String KEY_TITLE = "title";
    private static final String KEY_LANDING = "landing";
    private static final String KEY_DOCUMENTS_MODE = "documents_mode";
    private static final String KEY_LESSONS_AVAILABLE = "lessons_available";
    private static final String KEY_LOCAL_CHANGES = "local_changes";
    private static final String KEY_EPIC_AUTO_START = "epic_auto_start";
    private static final String KEY_EPIC_AUTO_START_ORIGIN = "epic_auto_start_origin";
    private static final String KEY_PARALLELIZATION_SCOPE = "parallelization_scope";
    private static final String KEY_OVERLAP_MAX_FILES = "overlap_max_files";
    private static final String KEY_REFUSAL = "refusal";
    private static final String KEY_ADOPTABLE_EPICS = "adoptable_epics";
    private static final String KEY_ADOPTION_FAILURE = "adoption_failure";
    private static final String KEY_ADD_OUTCOMES = "add_outcomes";
    private static final String KEY_ENDED_WORK = "ended_work";
    /** The head of every refusal of a defective ended-work fixture. */
    private static final String MALFORMED_ENDED_WORK = "Malformed facts.runtime.start." + KEY_ENDED_WORK;
    /** The refusal of an ended item that is no object or names no subject. */
    private static final String MALFORMED_ENDED_ITEM = MALFORMED_ENDED_WORK
            + ": each item needs a subject_id and a recognized subject_kind";
    private static final String KEY_OUTCOME = "outcome";
    private static final String KEY_NEED = "need";
    private static final String KEY_REASON = "reason";
    private static final String KEY_AVAILABLE = "available";
    private static final String KEY_TEXT = "text";
    private static final String KEY_SUBJECT_ID = "subject_id";
    private static final String KEY_SUBJECT_KIND = "subject_kind";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_FORGE = "forge";
    private static final String KEY_RESETS_AT = "resets_at";
    private static final String KEY_REFERENCE = "reference";
    private static final String KEY_CHANGED_FILES = "changed_files";
    private static final String KEY_DIRECTORY = "directory";
    private static final String KEY_PATH = "path";
    private static final String KEY_SIZE = "size";
    private static final String KEY_NAME = "name";
    private static final String KEY_READABLE = "readable";
    private static final String KEY_REFUSAL_FILE = "refusal";
    private static final String KEY_DIGEST = "digest";
    private static final String KEY_VERDICT = "verdict";
    private static final String KEY_TRUST = "trust";
    private static final String KEY_DISPOSITION = "disposition";
    private static final String KEY_DISPOSITION_REASON = "disposition_reason";
    private static final String KEY_SPANS = "spans";
    private static final String KEY_FAILURE = "failure";
    private static final String KEY_DETAIL = "detail";
    private static final String KEY_SOURCE_ID = "source_id";
    private static final String KEY_OBJECTIVE = "objective";
    private static final String KEY_RECIPE_KEY = "recipe_key";
    private static final String KEY_PARAMETERS = "parameters";

    private static final String STATE_SOURCES = StartState.SOURCES.wireValue();
    private static final String STATE_CHOOSE = StartState.CHOOSE.wireValue();
    private static final String STATE_READY = StartState.READY.wireValue();
    private static final String STATE_RECIPE = StartState.RECIPE.wireValue();
    private static final String STATE_REFUSED = StartState.REFUSED.wireValue();
    private static final String STATE_EPIC_ADOPT = StartState.EPIC_ADOPT.wireValue();
    private static final String STATE_EPIC_ADOPT_FAILED = StartState.EPIC_ADOPT_FAILED.wireValue();
    private static final String STATE_RECIPE_PARAMETERS = StartState.RECIPE_PARAMETERS.wireValue();
    private static final String LANDING_FILES = "landing.files";
    private static final String LANDING_PULL_REQUEST = "landing.pull-request";
    private static final String PREFIX_ENTRY = "ent-";
    private static final String ADD_PREFIX = "add_";
    private static final String PATH_SEPARATOR = "/";
    private static final String EVENT_EPIC_STARTED = "epic.started";
    private static final String EVENT_PLAN_STARTED = "plan.started";
    private static final String EVENT_ENTRY_ABANDONED = "entry.abandoned";
    private static final String KEY_UNTRUSTED = "untrusted";
    private static final String KEY_OPERATOR_AUTHORED = "operator_authored";
    private static final String KEY_ADDITIONS = "additions";
    private static final String KEY_DELETIONS = "deletions";
    private static final String KEY_PLAN_ID = "plan_id";
    private static final String KEY_EPIC_ID = "epic_id";
    private static final String KEY_START = "start";
    private static final String KEY_END = "end";
    private static final String KEY_OPTIONS = "options";
    private static final String KEY_ORIGIN = "origin";
    private static final String KEY_TAKES_REQUEST = "takes_request";
    private static final String KEY_PROPOSED = "proposed";
    private static final String KEY_VALUE = "value";
    private static final String KEY_CAUSE = "cause";
    private static final long DEFAULT_RESET_EPOCH = 1_792_507_200L;
    private static final String FIXTURE_RUNTIME = "runtime";
    private static final String FIXTURE_START = "start";
    private static final String FIXTURE_BASE_STATE = "running_work.active";
    private static final String TYPE = "start fixture";
    private static final String DESCRIPTION_REFERENCE = "A description";
    private static final String DEFAULT_DOCUMENT = "doc/concepts/forgejo.adoc";
    private static final String DOC_DIRECTORY = "doc/concepts";
    private static final String C_NOT_READABLE = "not_readable";
    private static final String C_LINK_OUTSIDE = "link_outside";
    private static final String DEFAULT_ISSUE = "https://codeberg.org/forgejo/forgejo/issues/4711";
    private static final String DEFAULT_PULL_REQUEST = "https://github.com/plan-marshall/pm-mcp-clients/pull/214";
    private static final String DEFAULT_WORK = "started-work";
    private static final String DEFAULT_PROJECT = "pm-mcp-clients";

    /** The callback a start uses to tell its clients of an event. */
    private final Consumer<StartEvent> broadcaster;

    private final Map<String, StartEntry> entries = new LinkedHashMap<>();
    private final Map<String, List<RecipeRef>> projectRecipes = new LinkedHashMap<>();
    private final Map<String, String> projectLandings = new LinkedHashMap<>();
    private final Map<String, Boolean> projectAutoStart = new LinkedHashMap<>();
    private final Map<String, ConfigOrigin> projectAutoStartOrigin = new LinkedHashMap<>();
    private final Map<String, Integer> projectScope = new LinkedHashMap<>();
    private final Map<String, Integer> projectOverlap = new LinkedHashMap<>();
    private final Map<String, EndedWork> endedWork = new LinkedHashMap<>();
    private final List<AdoptableEpic> adoptableEpics = new ArrayList<>();
    private String documentsMode = DocumentsMode.ASK.wireValue();
    private int entryCounter;
    private int sourceCounter;

    /**
     * @param broadcaster the callback that broadcasts a start event
     */
    StartRegistry(Consumer<StartEvent> broadcaster) {
        this.broadcaster = Objects.requireNonNull(broadcaster, "broadcaster");
    }

    /**
     * One event a start broadcasts.
     *
     * @param name the event name
     * @param data the payload
     */
    record StartEvent(String name, Map<String, Object> data) {
    }

    /**
     * The mutable state of one entry.
     */
    private static final class StartEntry {
        private final String entryId;
        private EntryKind kind;
        private String projectId;
        private String state;
        private @Nullable String title;
        private @Nullable String planId;
        private @Nullable String objective;
        private @Nullable String recipeKey;
        private @Nullable RecipeRef recipe;
        private List<RecipeParameter> parameters = List.of();
        private final List<SourceResource> sources = new ArrayList<>();
        private final List<SourceKindOffer> offers = new ArrayList<>();
        private boolean documentsModeAsked;
        private boolean canContinue;
        private boolean chatAvailable;
        private @Nullable String chatDisabledReason;
        private @Nullable StartRefusal refusal;
        private @Nullable AdoptionFailure adoptionFailure;
        private @Nullable AdoptionFailure pendingAdoptionFailure;
        private List<AdoptableEpic> adoptable = List.of();
        private List<RecipeRef> recipes = List.of();
        private @Nullable String landing;
        private boolean lessonsAvailable;
        private boolean localChanges;
        private final Map<SourceKind, Map<String, Object>> addOutcomes = new LinkedHashMap<>();
        private boolean created;

        StartEntry(String entryId, EntryKind kind, String projectId, String state) {
            this.entryId = entryId;
            this.kind = kind;
            this.projectId = projectId;
            this.state = state;
        }
    }

    /**
     * One ended plan or epic, whose chat is about the archived record.
     *
     * @param subjectId the ended plan or epic
     * @param kind      whether it is a plan or an epic
     * @param projectId the project it ran in, where its follow-up plan starts
     * @param outcome   the wire value of the outcome it ended with, {@code null} where the fixture names none
     */
    private record EndedWork(String subjectId, EntryKind kind, String projectId, @Nullable String outcome) {
    }

    /**
     * Seeds the registry from the active scenario and the state that seeds the engine.
     * <p>
     * The fixture is the operator representation's {@code facts.runtime.start} of that state,
     * merged over the scenario's base state {@code running_work.active} so a case state declares
     * only what differs.
     *
     * @param scenario the active scenario
     * @param stateId  the state whose operator facts the engine applies, may be {@code null}
     */
    synchronized void init(Scenario scenario, @Nullable String stateId) {
        entries.clear();
        projectRecipes.clear();
        projectLandings.clear();
        projectAutoStart.clear();
        projectAutoStartOrigin.clear();
        projectScope.clear();
        projectOverlap.clear();
        endedWork.clear();
        adoptableEpics.clear();
        entryCounter = 0;
        sourceCounter = 0;
        documentsMode = DocumentsMode.ASK.wireValue();
        var fixture = startFixtureOf(scenario, stateId);
        if (fixture.isEmpty()) {
            return;
        }
        seedDefaults(scenario, fixture);
        endedWork.putAll(endedWorkOf(fixture));
        seedEntry(fixture);
    }

    /**
     * @param scenario the active scenario
     * @param stateId  the state whose operator facts the engine applies, may be {@code null}
     * @return the start fixture of that state, merged over the scenario's base state
     */
    private static Map<String, Object> startFixtureOf(Scenario scenario, @Nullable String stateId) {
        var fixture = new LinkedHashMap<String, Object>();
        mergeStartFixture(runtimeFixtureOf(scenario, FIXTURE_BASE_STATE), fixture);
        mergeStartFixture(runtimeFixtureOf(scenario, stateId), fixture);
        return fixture;
    }

    private static void mergeStartFixture(@Nullable Map<String, Object> runtime, Map<String, Object> target) {
        var start = runtime != null ? JsonTree.asObject(runtime.get(FIXTURE_START)) : null;
        if (start != null) {
            target.putAll(start);
        }
    }

    private static @Nullable Map<String, Object> runtimeFixtureOf(Scenario scenario, String stateId) {
        var state = scenario.state(stateId);
        if (state == null) {
            return null;
        }
        var representation = JsonTree.asObject(state.representation());
        if (representation == null) {
            return null;
        }
        var facts = JsonTree.asObject(representation.get(OperatorStateResource.FIELD_FACTS));
        return facts != null ? JsonTree.asObject(facts.get(FIXTURE_RUNTIME)) : null;
    }

    /**
     * @param fixture the state's start fixture
     * @return the project the fixture names, {@code pm-mcp-clients} when it names none
     */
    private static String projectOf(Map<String, Object> fixture) {
        var projectId = JsonTree.string(fixture, KEY_PROJECT_ID);
        return projectId != null ? projectId : DEFAULT_PROJECT;
    }

    private void seedDefaults(Scenario scenario, Map<String, Object> fixture) {
        var projectId = projectOf(fixture);
        var landing = JsonTree.string(fixture, KEY_LANDING);
        projectLandings.put(projectId, landing != null ? landing : LANDING_PULL_REQUEST);
        var mode = DocumentsMode.fromWire(JsonTree.string(fixture, KEY_DOCUMENTS_MODE));
        documentsMode = mode != null ? mode.wireValue() : DocumentsMode.ASK.wireValue();
        var autoStart = JsonTree.bool(fixture, KEY_EPIC_AUTO_START);
        projectAutoStart.put(projectId, autoStart == null || autoStart);
        var originWire = JsonTree.string(fixture, KEY_EPIC_AUTO_START_ORIGIN);
        var origin = originWire != null ? ConfigOrigin.fromWireOrNull(originWire) : null;
        projectAutoStartOrigin.put(projectId, origin != null ? origin : ConfigOrigin.MACHINE);
        var scope = JsonTree.integer(fixture, KEY_PARALLELIZATION_SCOPE);
        projectScope.put(projectId, scope != null && scope > 0 ? scope : 1);
        var overlap = JsonTree.integer(fixture, KEY_OVERLAP_MAX_FILES);
        projectOverlap.put(projectId, overlap != null && overlap >= 0 ? overlap : 0);
        projectRecipes.put(projectId, recipeFixtures(scenario, fixture).stream()
                .map(StartRegistry::recipeFrom).toList());
        adoptableEpics.addAll(objects(fixture.get(KEY_ADOPTABLE_EPICS)).stream()
                .map(StartRegistry::adoptableFrom).toList());
    }

    /**
     * @param scenario the active scenario
     * @param fixture  the state's start fixture
     * @return the recipes the state names, or the scenario's {@code GET /api/v1/recipes} route when
     *         the state names none
     */
    private static List<Map<String, Object>> recipeFixtures(Scenario scenario, Map<String, Object> fixture) {
        var recipes = objects(fixture.get(KEY_RECIPES));
        if (!recipes.isEmpty()) {
            return recipes;
        }
        var route = scenario.route("GET", "/api/v1/recipes");
        var representation = route != null ? JsonTree.asObject(route.representation()) : null;
        return representation != null ? objects(representation.get(KEY_RECIPES)) : List.of();
    }

    private static RecipeRef recipeFrom(Map<String, Object> map) {
        var parameters = objects(map.get(KEY_PARAMETERS)).stream()
                .filter(parameter -> JsonTree.string(parameter, KEY_NAME) != null)
                .map(StartRegistry::parameterFrom).toList();
        return new RecipeRef(JsonTree.string(map, KEY_KEY), JsonTree.string(map, KEY_NAME),
                JsonTree.string(map, KEY_DESCRIPTION), JsonTree.string(map, KEY_ORIGIN),
                Boolean.TRUE.equals(JsonTree.bool(map, KEY_TAKES_REQUEST)), parameters);
    }

    private static RecipeParameter parameterFrom(Map<String, Object> parameter) {
        var rawOptions = JsonTree.asArray(parameter.get(KEY_OPTIONS));
        var options = rawOptions == null ? List.<String>of()
                : rawOptions.stream().filter(Objects::nonNull).map(String::valueOf).toList();
        return new RecipeParameter(JsonTree.string(parameter, KEY_NAME), JsonTree.string(parameter, KEY_VALUE),
                options, Boolean.TRUE.equals(JsonTree.bool(parameter, KEY_PROPOSED)),
                !Boolean.FALSE.equals(JsonTree.bool(parameter, KEY_AVAILABLE)),
                JsonTree.string(parameter, KEY_REASON));
    }

    private static AdoptableEpic adoptableFrom(Map<String, Object> map) {
        return new AdoptableEpic(JsonTree.string(map, KEY_EPIC_ID), JsonTree.string(map, KEY_TITLE));
    }

    /**
     * Refuses the ended work a state's start fixture names before any store is touched, so a scenario
     * switch or a jump to a state with a defective fixture fails instead of half-seeding the engine.
     *
     * @param scenario the scenario
     * @param stateId  the state whose start fixture is read, may be {@code null}
     * @throws IllegalStateException if an ended item is not an object, lacks a subject id or a recognized
     *                               subject kind, or two ended items share a subject id
     */
    static void validateEndedWork(Scenario scenario, @Nullable String stateId) {
        endedWorkOf(startFixtureOf(scenario, stateId));
    }

    /**
     * Reads the ended work of a start fixture, keyed by subject id. An ended item that is not an object
     * or has no subject id or recognized subject kind is refused, and so are two ended items with the
     * same subject id, whatever their kind: the later one would replace the earlier one silently.
     *
     * @param fixture the state's start fixture
     * @return the ended work by subject id, in fixture order
     * @throws IllegalStateException if an ended item is not an object, lacks a subject id or a recognized
     *                               subject kind, or two ended items share a subject id
     */
    private static Map<String, EndedWork> endedWorkOf(Map<String, Object> fixture) {
        requireObjectItems(fixture.get(KEY_ENDED_WORK));
        var projectId = projectOf(fixture);
        var read = new LinkedHashMap<String, EndedWork>();
        for (var object : objects(fixture.get(KEY_ENDED_WORK))) {
            var subjectId = JsonTree.string(object, KEY_SUBJECT_ID);
            var subjectKind = EntryKind.fromWireOrNull(JsonTree.string(object, KEY_SUBJECT_KIND));
            if (subjectId == null || subjectKind == null) {
                throw new IllegalStateException(MALFORMED_ENDED_ITEM);
            }
            var ended = new EndedWork(subjectId, subjectKind, projectId, JsonTree.string(object, KEY_OUTCOME));
            if (read.putIfAbsent(subjectId, ended) != null) {
                throw new IllegalStateException(MALFORMED_ENDED_WORK
                        + ": the subject_id '" + subjectId + "' is named twice");
            }
        }
        return read;
    }

    /**
     * Refuses an ended-work array with an element that is not an object, which {@link #objects(Object)}
     * would skip silently; an absent or non-array value is left to the reader.
     *
     * @param endedWork the fixture's ended-work value, may be {@code null}
     * @throws IllegalStateException if an element of the array is not an object
     */
    private static void requireObjectItems(@Nullable Object endedWork) {
        var items = JsonTree.asArray(endedWork);
        if (items == null) {
            return;
        }
        for (var item : items) {
            if (JsonTree.asObject(item) == null) {
                throw new IllegalStateException(MALFORMED_ENDED_ITEM);
            }
        }
    }

    private void seedEntry(Map<String, Object> fixture) {
        var projectId = JsonTree.string(fixture, KEY_PROJECT_ID);
        var kind = EntryKind.fromWireOrNull(JsonTree.string(fixture, KEY_KIND));
        var state = JsonTree.string(fixture, KEY_STATE);
        if (projectId == null || kind == null || state == null) {
            return;
        }
        var entryId = JsonTree.string(fixture, KEY_ENTRY_ID);
        var entry = new StartEntry(entryId != null ? entryId : nextEntryId(), kind, projectId, state);
        entry.title = JsonTree.string(fixture, KEY_TITLE);
        entry.objective = JsonTree.string(fixture, KEY_OBJECTIVE);
        entry.landing = projectLandings.getOrDefault(projectId, LANDING_PULL_REQUEST);
        var lessons = JsonTree.bool(fixture, KEY_LESSONS_AVAILABLE);
        entry.lessonsAvailable = lessons == null || lessons;
        var local = JsonTree.bool(fixture, KEY_LOCAL_CHANGES);
        entry.localChanges = local == null || local;
        entry.recipes = projectRecipes.getOrDefault(projectId, List.of());
        entry.adoptable = kind.epic() ? List.copyOf(adoptableEpics) : List.of();
        seedEntryRefusal(entry, fixture, projectId);
        seedEntryRecipe(entry, fixture);
        seedEntryOutcomes(entry, fixture);
        entry.sources.addAll(objects(fixture.get(KEY_SOURCES)).stream()
                .map(this::sourceFromQuietly).toList());
        refresh(entry);
        entries.put(entry.entryId, entry);
    }

    private void seedEntryRefusal(StartEntry entry, Map<String, Object> fixture, String projectId) {
        var refusal = JsonTree.object(fixture, KEY_REFUSAL);
        if (refusal != null) {
            entry.refusal = refusalFrom(refusal, projectId);
        }
        var failure = JsonTree.object(fixture, KEY_ADOPTION_FAILURE);
        if (failure != null) {
            entry.pendingAdoptionFailure = adoptionFailureFrom(failure);
        }
    }

    private void seedEntryRecipe(StartEntry entry, Map<String, Object> fixture) {
        var recipeKey = JsonTree.string(fixture, KEY_RECIPE_KEY);
        if (recipeKey != null) {
            entry.recipeKey = recipeKey;
            entry.recipe = entry.recipes.stream().filter(recipe -> recipe.key().equals(recipeKey))
                    .findFirst().orElse(null);
        }
    }

    private void seedEntryOutcomes(StartEntry entry, Map<String, Object> fixture) {
        var outcomes = JsonTree.object(fixture, KEY_ADD_OUTCOMES);
        if (outcomes == null) {
            return;
        }
        Arrays.stream(SourceKind.values()).forEach(kindValue -> {
            var outcome = JsonTree.object(outcomes, kindValue.wireValue());
            if (outcome != null) {
                entry.addOutcomes.put(kindValue, outcome);
            }
        });
    }

    private SourceResource sourceFromQuietly(Map<String, Object> map) {
        try {
            return sourceFrom(map);
        } catch (ActRefused e) {
            throw new IllegalStateException("Invalid start source fixture: " + e.detail(), e);
        }
    }

    /**
     * Opens an entry under its project.
     *
     * @param projectId the project
     * @param request   the opening request
     * @return the opened entry, or {@code null} if the gate refuses and nothing is created
     */
    synchronized @Nullable StartStateResource open(String projectId, EntryOpenRequest request) {
        if (!projectLandings.containsKey(projectId)) {
            return null;
        }
        var entry = new StartEntry(nextEntryId(), request.kind(), projectId, openingState(request));
        entry.title = request.title();
        entry.objective = request.objective();
        entry.landing = projectLandings.get(projectId);
        entry.lessonsAvailable = true;
        entry.localChanges = true;
        entry.recipes = projectRecipes.getOrDefault(projectId, List.of());
        entry.adoptable = request.kind().epic() ? List.copyOf(adoptableEpics) : List.of();
        if (request.recipeKey() != null) {
            entry.recipeKey = request.recipeKey();
            entry.recipe = entry.recipes.stream().filter(recipe -> recipe.key().equals(request.recipeKey()))
                    .findFirst().orElse(null);
        }
        entry.chatAvailable = true;
        entry.chatDisabledReason = null;
        if (request.kind().epic() && !entry.adoptable.isEmpty()) {
            entry.state = STATE_SOURCES;
        }
        refresh(entry);
        entries.put(entry.entryId, entry);
        return representation(entry);
    }

    /**
     * @param entryId the entry's id
     * @return the entry's current representation, or {@code null} for an unknown entry
     */
    synchronized @Nullable StartStateResource find(String entryId) {
        var entry = entries.get(entryId);
        return entry != null ? representation(entry) : null;
    }

    /**
     * Applies an operator act of an entry.
     *
     * @param entryId the entry's id
     * @param linkId  the link id
     * @param body    the act's body
     * @return the entry's new representation, or {@code null} for an unknown entry
     * @throws ActRefused if the entry does not offer the act, or the act is refused
     */
    synchronized @Nullable StartStateResource act(String entryId, String linkId, Map<String, Object> body)
            throws ActRefused {
        var entry = entries.get(entryId);
        if (entry == null) {
            return null;
        }
        if ("abandon".equals(linkId)) {
            return abandon(entry);
        }
        if (linkId.startsWith("remove:")) {
            removeSource(entry, linkId.substring("remove:".length()));
        } else if (linkId.startsWith("retry:")) {
            retrySource(entry, linkId.substring("retry:".length()));
        } else if (linkId.startsWith("accept:")) {
            acceptSource(entry, linkId.substring("accept:".length()), body);
        } else if (linkId.startsWith("adopt:")) {
            adopt(entry, linkId.substring("adopt:".length()));
        } else if (linkId.startsWith("choose:")) {
            chooseRecipe(entry, linkId.substring("choose:".length()));
        } else if (linkId.startsWith("documents_mode:")) {
            documentsMode(entry, linkId.substring("documents_mode:".length()));
        } else if (linkId.startsWith(ADD_PREFIX)) {
            addSourceAct(entry, linkId.substring(ADD_PREFIX.length()), body);
        } else {
            switch (linkId) {
                case StartStateResource.LINK_SOURCES -> leaveRecipe(entry);
                case StartStateResource.LINK_RECIPE ->
                    entry.state = entry.recipes.isEmpty() ? STATE_SOURCES : STATE_RECIPE;
                case StartStateResource.LINK_START -> continueToReady(entry);
                case "back" -> leaveRecipe(entry);
                case "create" -> create(entry, body);
                case "set" -> setParameters(entry, body);
                case "retry" -> retryAdoption(entry);
                case "new" -> {
                    entry.adoptable = List.of();
                    entry.state = STATE_SOURCES;
                }
                default -> throw new ActRefused("act_not_available", "The entry does not offer " + linkId);
            }
        }
        refresh(entry);
        return representation(entry);
    }

    /**
     * @param entryId the entry's id
     * @return {@code true} if the entry opens an epic, whose chat is routed to {@code decompose}
     */
    synchronized boolean isEpicEntry(String entryId) {
        var entry = entries.get(entryId);
        return entry != null && entry.kind.epic();
    }

    /**
     * Applies a description an entry's chat proposed and the operator accepted: it replaces the entry's
     * description, so the entry's state shows the new one as its description source.
     *
     * @param entryId the entry's id
     * @param text    the description after the change; {@code null} or blank changes nothing
     * @return {@code true} if the entry is open and its description changed
     */
    synchronized boolean applyDescription(String entryId, @Nullable String text) {
        var entry = entries.get(entryId);
        if (entry == null || text == null || text.isBlank()) {
            return false;
        }
        entry.sources.removeIf(source -> source.kind() == SourceKind.DESCRIPTION);
        storeDescription(entry, text);
        refresh(entry);
        return true;
    }

    /**
     * Opens the start of a new plan from the follow-up plan a chat about ended work proposed and the
     * operator accepted: a plan's entry in the project the ended work ran in, at {@code start.sources},
     * with the accepted description stored as its description source. The description names the ended
     * plan or epic; one that does not is followed by {@code follows <id>}.
     *
     * @param subjectId the ended plan or epic
     * @param text      the accepted description, {@code null} or blank read as none
     * @return {@code true} if the subject is ended work of a seeded project and the entry was opened
     */
    synchronized boolean openFollowUp(String subjectId, @Nullable String text) {
        var ended = endedWork.get(subjectId);
        // A project the state does not seed has no landing; a start there is one open() refuses too.
        if (ended == null || !projectLandings.containsKey(ended.projectId())) {
            return false;
        }
        var projectId = ended.projectId();
        var description = text == null ? "" : text.strip();
        if (!description.contains(subjectId)) {
            description = (description + " follows " + subjectId).strip();
        }
        var entry = new StartEntry(nextEntryId(), EntryKind.PLAN, projectId, STATE_SOURCES);
        entry.landing = projectLandings.get(projectId);
        entry.lessonsAvailable = true;
        entry.localChanges = true;
        entry.recipes = projectRecipes.getOrDefault(projectId, List.of());
        entry.chatAvailable = true;
        storeDescription(entry, description);
        refresh(entry);
        entries.put(entry.entryId, entry);
        return true;
    }

    /**
     * Records a plan or an epic that ended while the runtime runs, so a follow-up plan its chat proposes
     * opens a start like the follow-up of seeded ended work.
     *
     * @param kind      a plan or an epic
     * @param subjectId the plan's or the epic's id
     * @param projectId the project the work ran in, {@code null} if the end named none
     * @param outcome   the wire value of the outcome it ended with, {@code null} if none was named
     */
    synchronized void endWork(EntryKind kind, String subjectId, @Nullable String projectId,
            @Nullable String outcome) {
        if (projectId != null) {
            endedWork.put(subjectId, new EndedWork(subjectId, kind, projectId, outcome));
        }
    }

    /**
     * @return the ended plans and epics of the active state, as the subjects of their chats, each with the
     *         wire value of the outcome it ended with, {@code null} where the fixture names none
     */
    synchronized Map<ChatSubject, @Nullable String> endedSubjects() {
        var subjects = new LinkedHashMap<ChatSubject, @Nullable String>();
        endedWork.values().forEach(ended -> subjects.put(new ChatSubject(
                        ended.kind().epic() ? ChatSubjectKind.EPIC : ChatSubjectKind.PLAN, ended.subjectId()),
                ended.outcome()));
        return subjects;
    }

    /**
     * Lists the confined files of a project's picker.
     *
     * @param projectId the project
     * @param root      the root
     * @param path      the directory, relative to the root
     * @param isWeb     whether the request arrived on the web listener
     * @return the listing, or {@code null} for an unknown project
     * @throws ActRefused if the root is loopback-only and the request arrived on the network
     */
    synchronized @Nullable ConfinedFileListResource listFiles(String projectId, ConfinedRoot root, @Nullable String path,
            boolean isWeb) throws ActRefused {
        if (!projectLandings.containsKey(projectId)) {
            return null;
        }
        if (root.loopbackOnly() && isWeb) {
            throw new ActRefused("loopback_only", "the operator's home directory is not shown to other devices");
        }
        var relative = path == null ? "" : path;
        if (!relative.isEmpty() && !".".equals(relative)) {
            try {
                ConfinedPath.resolve(Path.of("/root"), relative);
            } catch (IllegalArgumentException e) {
                throw new ActRefused("invalid_arguments", e.getMessage());
            }
        }
        var files = objects(confinedFixture().get(relative)).stream()
                .filter(object -> JsonTree.string(object, KEY_NAME) != null
                        && JsonTree.string(object, KEY_PATH) != null)
                .map(this::confinedFileFrom).toList();
        return new ConfinedFileListResource(projectId, root, relative, parentOf(relative), files, false);
    }

    private static @Nullable String parentOf(String relative) {
        if (relative.isEmpty()) {
            return null;
        }
        var slash = relative.lastIndexOf(PATH_SEPARATOR);
        return slash > 0 ? relative.substring(0, slash) : ".";
    }

    private ConfinedFile confinedFileFrom(Map<String, Object> object) {
        var refusalMap = JsonTree.object(object, KEY_REFUSAL_FILE);
        return new ConfinedFile(JsonTree.string(object, KEY_NAME), JsonTree.string(object, KEY_PATH),
                Boolean.TRUE.equals(JsonTree.bool(object, KEY_DIRECTORY)),
                longOrZero(JsonTree.number(object, KEY_SIZE)),
                !Boolean.FALSE.equals(JsonTree.bool(object, KEY_READABLE)),
                refusalMap != null ? failureFromOrNull(refusalMap) : null);
    }

    private @Nullable SourceFailure failureFromOrNull(Map<String, Object> map) {
        try {
            return failureFrom(map);
        } catch (ActRefused e) {
            throw new IllegalStateException("Invalid start case failure: " + e.detail(), e);
        }
    }

    /**
     * @param projectId the project
     * @return the recipes offered in the project
     */
    synchronized List<RecipeRef> recipes(String projectId) {
        return projectRecipes.getOrDefault(projectId, List.of());
    }

    /**
     * @param projectId the project
     * @param recipes   the recipes it offers
     */
    synchronized void setRecipes(String projectId, @Nullable List<RecipeRef> recipes) {
        projectRecipes.put(projectId, recipes == null ? List.of() : List.copyOf(recipes));
    }

    /**
     * @param projectId the project
     * @return the project's landing in force
     */
    synchronized String landing(String projectId) {
        return projectLandings.getOrDefault(projectId, LANDING_PULL_REQUEST);
    }

    // ---------------------------------------------------------------------------------------------
    // Acts

    private String openingState(EntryOpenRequest request) {
        if (request.kind().epic() || request.recipeKey() != null) {
            return STATE_SOURCES;
        }
        // A plan first chooses between a start from sources and a recipe (design "Starting Work").
        return STATE_CHOOSE;
    }

    private void addSourceAct(StartEntry entry, String kindWire, Map<String, Object> body) throws ActRefused {
        var kind = SourceKind.fromWireOrNull(kindWire);
        if (kind == null) {
            throw new ActRefused("act_not_available", "No source kind carries the value " + kindWire);
        }
        var present = entry.sources.stream().map(SourceResource::kind)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        var offered = SourceKind.offered(entry.kind, present, entry.lessonsAvailable, entry.localChanges,
                !LANDING_FILES.equals(entry.landing));
        if (!offered.contains(kind)) {
            throw new ActRefused("act_not_available", "The start does not offer the kind " + kindWire
                    + " in its current list");
        }
        switch (kind) {
            case DESCRIPTION -> addDescription(entry, body);
            case DOCUMENTS -> addDocuments(entry);
            default -> addSource(entry, kind);
        }
    }

    private void addDescription(StartEntry entry, Map<String, Object> body) {
        var text = JsonTree.string(body, KEY_TEXT);
        if (text == null || text.isBlank()) {
            text = entry.objective;
        }
        if (text == null || text.isBlank()) {
            text = DESCRIPTION_REFERENCE;
        }
        storeDescription(entry, text);
    }

    private void storeDescription(StartEntry entry, @Nullable String text) {
        if (text == null) {
            return;
        }
        entry.sources.add(new SourceResource(nextSourceId(), SourceKind.DESCRIPTION, firstLine(text), null,
                SourceState.STORED, SourceTrust.TRUSTED, null, null, List.of(), null, null, List.of(), null));
        if (entry.title == null) {
            entry.title = firstLine(text);
        }
        entry.objective = text;
    }

    private void addDocuments(StartEntry entry) throws ActRefused {
        var outcome = entry.addOutcomes.get(SourceKind.DOCUMENTS);
        if (outcome != null) {
            entry.sources.add(sourceFrom(outcome));
            return;
        }
        if (DocumentsMode.ASK.wireValue().equals(documentsMode)) {
            entry.documentsModeAsked = true;
            entry.sources.add(new SourceResource(nextSourceId(), SourceKind.DOCUMENTS, DEFAULT_DOCUMENT, null,
                    SourceState.CHECKING, SourceTrust.TRUSTED, null, null,
                    List.of(), null, null, List.of(), null));
            return;
        }
        var moved = DocumentsMode.MOVE.wireValue().equals(documentsMode);
        entry.sources.add(new SourceResource(nextSourceId(), SourceKind.DOCUMENTS, DEFAULT_DOCUMENT, null,
                moved ? SourceState.MOVED : SourceState.COPIED,
                SourceTrust.TRUSTED, null, null, List.of(), null, null, List.of(), null));
    }

    private void documentsMode(StartEntry entry, String mode) throws ActRefused {
        var always = mode.endsWith("_always");
        var clean = always ? mode.substring(0, mode.length() - "_always".length()) : mode;
        if (!DocumentsMode.COPY.wireValue().equals(clean) && !DocumentsMode.MOVE.wireValue().equals(clean)) {
            throw new ActRefused("invalid_arguments", "Unknown documents mode: " + mode);
        }
        if (always) {
            documentsMode = clean;
        }
        var moved = DocumentsMode.MOVE.wireValue().equals(clean);
        var replaced = new ArrayList<SourceResource>();
        for (var source : entry.sources) {
            if (source.kind() == SourceKind.DOCUMENTS && source.state() == SourceState.CHECKING) {
                replaced.add(new SourceResource(source.sourceId(), source.kind(), source.reference(),
                        source.title(), moved ? SourceState.MOVED : SourceState.COPIED, source.trust(),
                        source.verdict(), source.failure(), source.spans(), source.disposition(),
                        source.dispositionReason(), source.changedFiles(), source.digest()));
            } else {
                replaced.add(source);
            }
        }
        entry.sources.clear();
        entry.sources.addAll(replaced);
        entry.documentsModeAsked = false;
    }

    private void addSource(StartEntry entry, SourceKind kind) throws ActRefused {
        var outcome = entry.addOutcomes.get(kind);
        if (outcome != null) {
            entry.sources.add(sourceFrom(outcome));
            return;
        }
        entry.sources.add(defaultSource(kind));
    }

    private SourceResource defaultSource(SourceKind kind) {
        var id = nextSourceId();
        return switch (kind) {
            case ISSUE -> new SourceResource(id, kind, DEFAULT_ISSUE, "Issue comments API", SourceState.STORED,
                    SourceTrust.TRUSTED, ScreeningVerdict.CLEAN, null, List.of(), null, null, List.of(),
                    "sha256:issue");
            case PULL_REQUEST -> SourceResource.pullRequest(id, DEFAULT_PULL_REQUEST,
                    "Add the Codeberg provider",
                    List.of(new ChangedFile("src/main/java/de/planmarshall/api/start/SourceResource.java", 42, 0,
                            true, false)));
            case LESSON -> new SourceResource(id, kind, "les-0007", "Retire lessons when a plan lands",
                    SourceState.STORED, SourceTrust.TRUSTED, null, null, List.of(), null, null, List.of(),
                    "sha256:lesson");
            case LOCAL_CHANGES -> new SourceResource(id, kind, "2 files changed", null, SourceState.STORED,
                    SourceTrust.TRUSTED, null, null, List.of(), null, null,
                    List.of(new ChangedFile("pm-api/src/main/java/A.java", 4, 1, false, true),
                            new ChangedFile("pm-api/src/main/java/B.java", 2, 0, false, true)), "sha256:local");
            case DOCUMENTS, DESCRIPTION -> new SourceResource(id, kind, DEFAULT_DOCUMENT, null,
                    SourceState.STORED, SourceTrust.TRUSTED, null, null, List.of(), null, null, List.of(), null);
        };
    }

    private void removeSource(StartEntry entry, String sourceId) {
        entry.sources.removeIf(source -> source.sourceId().equals(sourceId));
    }

    private void retrySource(StartEntry entry, String sourceId) throws ActRefused {
        if (entry.sources.stream().noneMatch(source -> source.sourceId().equals(sourceId))) {
            throw new ActRefused("act_not_available", "No source with the id " + sourceId);
        }
        var replaced = new ArrayList<SourceResource>();
        for (var source : entry.sources) {
            if (!source.sourceId().equals(sourceId)) {
                replaced.add(source);
                continue;
            }
            if (source.failure() == null || !source.failure().retryable()) {
                throw new ActRefused("act_not_available", "The source offers no retry");
            }
            replaced.add(new SourceResource(source.sourceId(), source.kind(), source.reference(), source.title(),
                    source.kind().external() ? SourceState.STORED : SourceState.COPIED,
                    source.kind().external() ? SourceTrust.TRUSTED : source.trust(),
                    source.kind().external() ? ScreeningVerdict.CLEAN : null, null, List.of(), null, null,
                    source.changedFiles(), source.digest()));
        }
        entry.sources.clear();
        entry.sources.addAll(replaced);
    }

    private void acceptSource(StartEntry entry, String sourceId, Map<String, Object> body) throws ActRefused {
        var target = entry.sources.stream().filter(source -> source.sourceId().equals(sourceId)).findFirst()
                .orElseThrow(() -> new ActRefused("act_not_available", "No source with the id " + sourceId));
        if (target.state() != SourceState.FLAGGED && target.state() != SourceState.UNSCREENED) {
            throw new ActRefused("act_not_available", "Only a flagged or unscreened source is accepted");
        }
        var reason = JsonTree.string(body, KEY_REASON);
        if (reason == null || reason.isBlank()) {
            throw new ActRefused("invalid_arguments", "An acceptance records the operator's reason");
        }
        var replaced = new ArrayList<SourceResource>();
        for (var source : entry.sources) {
            if (!source.sourceId().equals(sourceId)) {
                replaced.add(source);
                continue;
            }
            replaced.add(new SourceResource(source.sourceId(), source.kind(), source.reference(), source.title(),
                    SourceState.STORED, SourceTrust.UNTRUSTED, source.verdict(), null, source.spans(),
                    UnscreenedDisposition.ACCEPT_UNTRUSTED, reason, source.changedFiles(), source.digest()));
        }
        entry.sources.clear();
        entry.sources.addAll(replaced);
    }

    private void continueToReady(StartEntry entry) throws ActRefused {
        if (!canContinue(entry)) {
            throw new ActRefused("act_not_available", "Continue waits for a stored and screened source");
        }
        entry.state = STATE_READY;
    }

    private static void leaveRecipe(StartEntry entry) {
        entry.recipe = null;
        entry.recipeKey = null;
        entry.planId = null;
        entry.parameters = List.of();
        entry.state = STATE_SOURCES;
    }

    private void chooseRecipe(StartEntry entry, String recipeKey) throws ActRefused {
        var recipe = entry.recipes.stream().filter(candidate -> candidate.key().equals(recipeKey)).findFirst()
                .orElseThrow(() -> new ActRefused("act_not_available", "No recipe with the key " + recipeKey));
        if (!recipeKey.equals(entry.recipeKey)) {
            entry.planId = null;
        }
        entry.recipeKey = recipeKey;
        entry.recipe = recipe;
        entry.parameters = recipe.parameters();
        entry.state = recipe.parameters().isEmpty() ? STATE_READY : STATE_RECIPE_PARAMETERS;
    }

    private void setParameters(StartEntry entry, Map<String, Object> body) {
        var values = JsonTree.object(body, KEY_PARAMETERS);
        var parameters = new ArrayList<RecipeParameter>();
        for (var parameter : entry.parameters) {
            var value = values != null ? JsonTree.string(values, parameter.name()) : null;
            parameters.add(new RecipeParameter(parameter.name(), value != null ? value : parameter.value(),
                    parameter.options(), parameter.proposed(), parameter.available(), parameter.reason()));
        }
        entry.parameters = List.copyOf(parameters);
        entry.state = STATE_READY;
    }

    private void create(StartEntry entry, Map<String, Object> body) {
        var title = JsonTree.string(body, KEY_TITLE);
        if (title != null && !title.isBlank()) {
            entry.title = title.strip();
        }
        entry.planId = createdId(entry, JsonTree.string(body, KEY_PLAN_ID));
        entry.created = true;
        entry.state = STATE_READY;
        var idKey = entry.kind.epic() ? KEY_EPIC_ID : KEY_PLAN_ID;
        var event = entry.kind.epic() ? EVENT_EPIC_STARTED : EVENT_PLAN_STARTED;
        var name = entry.title != null ? entry.title : "";
        broadcaster.accept(new StartEvent(event, Map.of(idKey, entry.planId, KEY_PROJECT_ID,
                entry.projectId, KEY_TITLE, name, KEY_ENTRY_ID, entry.entryId)));
    }

    private @Nullable String createdId(StartEntry entry, @Nullable String requestedPlanId) {
        if (entry.recipe != null) {
            return planIdOf(entry);
        }
        if (requestedPlanId != null && !requestedPlanId.isBlank()) {
            return requestedPlanId.strip();
        }
        if (entry.planId != null) {
            return entry.planId;
        }
        var title = entry.title != null ? entry.title : DEFAULT_WORK;
        return slug(title);
    }

    private StartStateResource abandon(StartEntry entry) {
        broadcaster.accept(new StartEvent(EVENT_ENTRY_ABANDONED, Map.of(KEY_ENTRY_ID, entry.entryId,
                KEY_PROJECT_ID, entry.projectId)));
        var representation = representation(entry);
        entries.remove(entry.entryId);
        return representation;
    }

    private void adopt(StartEntry entry, String epicId) throws ActRefused {
        var offered = entry.adoptable.stream().anyMatch(epic -> epic.epicId().equals(epicId));
        if (!offered) {
            throw new ActRefused("act_not_available", "No adoptable epic with the id " + epicId);
        }
        if (entry.pendingAdoptionFailure != null
                && entry.pendingAdoptionFailure.epicId().equals(epicId)) {
            entry.adoptionFailure = entry.pendingAdoptionFailure;
            entry.state = STATE_EPIC_ADOPT_FAILED;
            throw new ActRefused("adoption_failed", "The ledger of " + epicId + " cannot be adopted");
        }
        entry.planId = epicId;
        entry.title = entry.adoptable.stream().filter(epic -> epic.epicId().equals(epicId))
                .map(AdoptableEpic::title).findFirst().orElse(epicId);
        entry.created = true;
        broadcaster.accept(new StartEvent(EVENT_EPIC_STARTED, Map.of(KEY_EPIC_ID, epicId,
                KEY_PROJECT_ID, entry.projectId, KEY_TITLE, entry.title, KEY_ENTRY_ID, entry.entryId)));
    }

    private void retryAdoption(StartEntry entry) {
        entry.adoptionFailure = null;
        entry.state = STATE_SOURCES;
    }

    // ---------------------------------------------------------------------------------------------
    // Derivation

    private void refresh(StartEntry entry) {
        entry.canContinue = canContinue(entry);
        entry.offers.clear();
        var present = entry.sources.stream().map(SourceResource::kind)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        var offered = SourceKind.offered(entry.kind, present, entry.lessonsAvailable, entry.localChanges,
                !LANDING_FILES.equals(entry.landing));
        Arrays.stream(SourceKind.values())
                .filter(kind -> !entry.kind.epic() || kind.offeredToEpic())
                .filter(kind -> kind == SourceKind.DESCRIPTION || !present.contains(kind))
                .forEach(kind -> entry.offers.add(offerOf(entry, kind, offered)));
        refreshChatAvailability(entry);
        if (entry.recipe == null && entry.recipeKey != null) {
            entry.recipe = entry.recipes.stream().filter(recipe -> recipe.key().equals(entry.recipeKey))
                    .findFirst().orElse(null);
        }
        if (entry.recipe != null && entry.parameters.isEmpty()) {
            entry.parameters = entry.recipe.parameters();
        }
    }

    private static SourceKindOffer offerOf(StartEntry entry, SourceKind kind, List<SourceKind> offered) {
        if (offered.contains(kind)) {
            return SourceKindOffer.available(kind);
        }
        return SourceKindOffer.refused(kind, refusalKey(entry, kind), refusalArgs(entry, kind));
    }

    /**
     * Derives whether the entry offers its chat yet: a recipe's start offers none, every other start
     * once a source is stored and every source from outside the machine is screened. The chat itself is
     * {@link ChatRegistry}'s.
     */
    private static void refreshChatAvailability(StartEntry entry) {
        if (entry.recipe != null) {
            // a recipe's start offers no chat at all
            entry.chatAvailable = false;
            entry.chatDisabledReason = null;
            return;
        }
        var chatReady = chatReady(entry);
        entry.chatAvailable = chatReady;
        entry.chatDisabledReason = chatReady ? null : "start.chat.waits";
    }

    private static String refusalKey(StartEntry entry, SourceKind kind) {
        if (kind == SourceKind.LOCAL_CHANGES && LANDING_FILES.equals(entry.landing)) {
            return SourceKindOffer.REASON_LOCAL_CHANGES_LANDING;
        }
        if (kind == SourceKind.LOCAL_CHANGES && !entry.localChanges) {
            return "start.sources.no_local_changes";
        }
        if (kind == SourceKind.LESSON && !entry.lessonsAvailable) {
            return "start.sources.no_lessons";
        }
        if (kind == SourceKind.PULL_REQUEST || kind == SourceKind.LOCAL_CHANGES) {
            return SourceKindOffer.REASON_ALONE;
        }
        if (kind == SourceKind.ISSUE || kind == SourceKind.LESSON) {
            return SourceKindOffer.REASON_ALONE;
        }
        if (entry.kind.epic()) {
            return "start.sources.epic_kind";
        }
        return SourceKindOffer.REASON_ALONE;
    }

    private static Map<String, Object> refusalArgs(StartEntry entry, SourceKind kind) {
        if (kind == SourceKind.LOCAL_CHANGES && LANDING_FILES.equals(entry.landing)) {
            return Map.of(KEY_LANDING, LANDING_FILES);
        }
        if (kind == SourceKind.PULL_REQUEST || kind == SourceKind.LOCAL_CHANGES) {
            return Map.of(KEY_KIND, kind.wireValue());
        }
        return Map.of();
    }

    private static boolean chatReady(StartEntry entry) {
        var stored = entry.sources.stream().anyMatch(source -> source.state().settled()
                || source.state() == SourceState.FLAGGED || source.state() == SourceState.UNSCREENED);
        var externalOpen = entry.sources.stream().anyMatch(source -> source.kind().external()
                && source.verdict() == null && source.disposition() == null);
        return stored && !externalOpen;
    }

    private static boolean canContinue(StartEntry entry) {
        if (entry.sources.isEmpty()) {
            return false;
        }
        for (var source : entry.sources) {
            if (!source.state().settled() && source.state() != SourceState.FLAGGED
                    && source.state() != SourceState.UNSCREENED) {
                return false;
            }
            if (source.kind().external() && source.verdict() == null && source.disposition() == null) {
                return false;
            }
        }
        return true;
    }

    private StartStateResource representation(StartEntry entry) {
        var planId = planIdOf(entry);
        var ready = readyOf(entry, planId);
        var effective = effectiveState(entry);
        var resource = new EntryResource(entry.entryId, entry.kind, entry.projectId,
                StartState.fromWire(effective), entry.title,
                planId, List.copyOf(entry.sources), List.copyOf(entry.offers), entry.canContinue,
                entry.chatAvailable, entry.chatDisabledReason, entry.recipe,
                entry.kind.epic() ? entry.adoptable : List.of(), entry.adoptionFailure);
        return StartStateResource.of(resource, STATE_READY.equals(effective) ? ready : null, null,
                STATE_REFUSED.equals(effective) ? refusalOf(entry) : null,
                STATE_EPIC_ADOPT_FAILED.equals(effective) ? entry.adoptionFailure : null,
                entry.recipes);
    }

    private String effectiveState(StartEntry entry) {
        if (entry.created) {
            return STATE_READY;
        }
        if (entry.refusal != null) {
            return STATE_REFUSED;
        }
        if (STATE_EPIC_ADOPT_FAILED.equals(entry.state)) {
            return STATE_EPIC_ADOPT_FAILED;
        }
        if (entry.kind.epic() && !entry.adoptable.isEmpty() && STATE_SOURCES.equals(entry.state)
                && entry.sources.isEmpty() && entry.planId == null) {
            return STATE_EPIC_ADOPT;
        }
        if (entry.documentsModeAsked) {
            return STATE_SOURCES;
        }
        return entry.state;
    }

    private @Nullable String planIdOf(StartEntry entry) {
        if (entry.planId != null) {
            return entry.planId;
        }
        if (entry.recipe != null) {
            entry.planId = RecipePlanId.derive(entry.recipeKey, LocalDateTime.now(ZoneOffset.UTC));
            return entry.planId;
        }
        if (entry.title != null) {
            return slug(entry.title);
        }
        return null;
    }

    private StartReadyResource readyOf(StartEntry entry, @Nullable String planId) {
        var id = idOf(entry, planId);
        if (entry.kind.epic()) {
            return new StartReadyResource(entry.title, id, true, true, null, List.of(), entry.landing, false,
                    projectAutoStart.getOrDefault(entry.projectId, true),
                    projectAutoStartOrigin.getOrDefault(entry.projectId, ConfigOrigin.MACHINE),
                    projectScope.getOrDefault(entry.projectId, 1),
                    projectOverlap.getOrDefault(entry.projectId, 0));
        }
        var idEditable = entry.recipe == null && entry.sources.stream()
                .noneMatch(source -> source.kind() == SourceKind.PULL_REQUEST);
        return new StartReadyResource(entry.title, id, idEditable, entry.recipe == null, entry.recipe,
                entry.parameters, entry.landing, false, null, null, null, null);
    }

    private static String idOf(StartEntry entry, @Nullable String planId) {
        if (planId != null) {
            return planId;
        }
        var title = entry.title != null ? entry.title : DEFAULT_WORK;
        return slug(title);
    }

    private StartRefusal refusalOf(StartEntry entry) {
        if (entry.refusal != null) {
            return entry.refusal;
        }
        return StartRefusal.machineNotOnboarded(entry.projectId, "providers");
    }

    // ---------------------------------------------------------------------------------------------
    // Seeding helpers

    private SourceResource sourceFrom(Map<String, Object> map) throws ActRefused {
        var kindValue = JsonTree.string(map, KEY_KIND);
        var kind = SourceKind.fromWireOrNull(kindValue);
        if (kind == null) {
            throw new ActRefused("invalid_arguments", "Unknown source kind in " + TYPE + ": " + kindValue);
        }
        var state = SourceState.fromWireOrNull(JsonTree.string(map, KEY_STATE));
        var trust = SourceTrust.fromWireOrNull(JsonTree.string(map, KEY_TRUST));
        var verdictWire = JsonTree.string(map, KEY_VERDICT);
        var verdict = verdictWire != null ? ScreeningVerdict.fromWireOrNull(verdictWire) : null;
        var dispositionWire = JsonTree.string(map, KEY_DISPOSITION);
        var disposition = dispositionWire != null ? UnscreenedDisposition.fromWireOrNull(dispositionWire) : null;
        var failureMap = JsonTree.object(map, KEY_FAILURE);
        var failure = failureMap != null ? failureFrom(failureMap) : null;
        var referenceValue = JsonTree.string(map, KEY_REFERENCE);
        var sourceIdValue = JsonTree.string(map, KEY_SOURCE_ID);
        return new SourceResource(sourceIdValue != null ? sourceIdValue : nextSourceId(), kind,
                referenceValue != null ? referenceValue : kind.wireValue(), JsonTree.string(map, KEY_TITLE),
                state != null ? state : SourceState.STORED,
                defaultTrust(trust, kind), verdict, failure,
                objects(map.get(KEY_SPANS)).stream().map(StartRegistry::spanFrom).toList(), disposition,
                JsonTree.string(map, KEY_DISPOSITION_REASON),
                objects(map.get(KEY_CHANGED_FILES)).stream().map(StartRegistry::changedFileFrom).toList(),
                JsonTree.string(map, KEY_DIGEST));
    }

    private static SourceTrust defaultTrust(@Nullable SourceTrust trust, SourceKind kind) {
        if (trust != null) {
            return trust;
        }
        return kind.external() ? SourceTrust.UNTRUSTED : SourceTrust.TRUSTED;
    }

    private static ScreeningSpan spanFrom(Map<String, Object> span) {
        var start = JsonTree.integer(span, KEY_START);
        var end = JsonTree.integer(span, KEY_END);
        var text = JsonTree.string(span, KEY_TEXT);
        var reason = JsonTree.string(span, KEY_REASON);
        return new ScreeningSpan(start != null ? start : 0, end != null ? end : 0,
                text != null ? text : "", reason != null ? reason : "");
    }

    private static ChangedFile changedFileFrom(Map<String, Object> file) {
        var path = JsonTree.string(file, KEY_PATH);
        var additions = JsonTree.integer(file, KEY_ADDITIONS);
        var deletions = JsonTree.integer(file, KEY_DELETIONS);
        return new ChangedFile(path != null ? path : "", additions != null ? additions : 0,
                deletions != null ? deletions : 0, Boolean.TRUE.equals(JsonTree.bool(file, KEY_UNTRUSTED)),
                Boolean.TRUE.equals(JsonTree.bool(file, KEY_OPERATOR_AUTHORED)));
    }

    private SourceFailure failureFrom(Map<String, Object> map) throws ActRefused {
        var causeValue = JsonTree.string(map, KEY_CAUSE);
        var cause = SourceFailureCause.fromWireOrNull(causeValue);
        if (cause == null) {
            throw new ActRefused("invalid_arguments", "Unknown cause in " + TYPE + ": " + causeValue);
        }
        var detail = JsonTree.string(map, KEY_DETAIL);
        if (!cause.namesReset()) {
            return new SourceFailure(cause, detail, null);
        }
        var resetsAt = JsonTree.number(map, KEY_RESETS_AT);
        var epoch = resetsAt != null ? resetsAt.longValue() : DEFAULT_RESET_EPOCH;
        var forge = JsonTree.string(map, KEY_FORGE);
        return SourceFailure.rateLimited(Instant.ofEpochSecond(epoch), forge != null ? forge : "the forge");
    }

    private StartRefusal refusalFrom(Map<String, Object> map, String projectId) {
        var reason = StartRefusalReason.fromWireOrNull(JsonTree.string(map, KEY_REASON));
        var need = JsonTree.string(map, KEY_NEED);
        if (reason == StartRefusalReason.MACHINE_NOT_ONBOARDED) {
            return StartRefusal.machineNotOnboarded(projectId, need != null ? need : "providers");
        }
        return StartRefusal.projectNotReady(projectId, need != null ? need : "credential sonar");
    }

    private AdoptionFailure adoptionFailureFrom(Map<String, Object> map) {
        var epicId = JsonTree.string(map, KEY_EPIC_ID);
        var reason = AdoptionFailureReason.fromWireOrNull(JsonTree.string(map, KEY_REASON));
        var file = JsonTree.string(map, "file");
        var detail = JsonTree.string(map, KEY_DETAIL);
        var id = epicId != null ? epicId : "relay-hardening";
        if (reason == AdoptionFailureReason.LEDGER_UNREADABLE) {
            return AdoptionFailure.unreadable(id, file != null ? file : "epic.json", detail);
        }
        if (reason == AdoptionFailureReason.LEDGER_TERMINAL) {
            return AdoptionFailure.terminal(id, detail);
        }
        return AdoptionFailure.invalid(id, detail);
    }

    private Map<String, Object> confinedFixture() {
        return Map.of(
                "", List.of(
                        Map.of(KEY_NAME, "doc", KEY_PATH, "doc", KEY_DIRECTORY, true, KEY_SIZE, 0,
                                KEY_READABLE, true),
                        Map.of(KEY_NAME, "pom.xml", KEY_PATH, "pom.xml", KEY_DIRECTORY, false, KEY_SIZE, 18432,
                                KEY_READABLE, true)),
                "doc", List.of(
                        Map.of(KEY_NAME, "concepts", KEY_PATH, DOC_DIRECTORY, KEY_DIRECTORY, true, KEY_SIZE, 0,
                                KEY_READABLE, true),
                        Map.of(KEY_NAME, "forgejo.adoc", KEY_PATH, "doc/forgejo.adoc", KEY_DIRECTORY, false,
                                KEY_SIZE, 8192, KEY_READABLE, true),
                        Map.of(KEY_NAME, "secret.key", KEY_PATH, "doc/secret.key", KEY_DIRECTORY, false,
                                KEY_SIZE, 0, KEY_READABLE, false,
                                KEY_REFUSAL_FILE, Map.of(KEY_CAUSE, C_NOT_READABLE))),
                DOC_DIRECTORY, List.of(
                        Map.of(KEY_NAME, "forgejo.adoc", KEY_PATH, DEFAULT_DOCUMENT, KEY_DIRECTORY,
                                false, KEY_SIZE, 8192, KEY_READABLE, true),
                        Map.of(KEY_NAME, "outside", KEY_PATH, "doc/concepts/outside", KEY_DIRECTORY, false,
                                KEY_SIZE, 0, KEY_READABLE, false,
                                KEY_REFUSAL_FILE, Map.of(KEY_CAUSE, C_LINK_OUTSIDE))));
    }

    // ---------------------------------------------------------------------------------------------
    // Small helpers

    private String nextEntryId() {
        entryCounter++;
        return PREFIX_ENTRY + String.format("%08x", 0x3f9a2c41 + entryCounter);
    }

    private String nextSourceId() {
        sourceCounter++;
        return "src-" + sourceCounter;
    }

    private static String firstLine(@Nullable String text) {
        if (text == null) {
            return DESCRIPTION_REFERENCE;
        }
        var trimmed = text.strip();
        var newline = trimmed.indexOf('\n');
        var line = newline >= 0 ? trimmed.substring(0, newline).strip() : trimmed;
        return line.isBlank() ? DESCRIPTION_REFERENCE : line;
    }

    private static String slug(String text) {
        var builder = new StringBuilder();
        var pendingDash = false;
        for (var c : text.toLowerCase(Locale.ROOT).toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                if (pendingDash && !builder.isEmpty()) {
                    builder.append('-');
                }
                builder.append(c);
                pendingDash = false;
            } else {
                pendingDash = true;
            }
        }
        return builder.isEmpty() ? DEFAULT_WORK : builder.toString();
    }

    private static List<Map<String, Object>> objects(@Nullable Object raw) {
        var array = JsonTree.asArray(raw);
        if (array == null) {
            return List.of();
        }
        var objects = new ArrayList<Map<String, Object>>();
        for (var item : array) {
            var object = JsonTree.asObject(item);
            if (object != null) {
                objects.add(object);
            }
        }
        return objects;
    }

    private static long longOrZero(@Nullable Number number) {
        return number != null ? number.longValue() : 0L;
    }

    /**
     * An act the entry does not offer, or one it refuses.
     */
    static final class ActRefused extends Exception {

        private static final long serialVersionUID = 1L;

        private final String error;
        private final String message;

        ActRefused(String error, String message) {
            super(message);
            this.error = error;
            this.message = message;
        }

        String error() {
            return error;
        }

        String detail() {
            return message;
        }
    }
}
