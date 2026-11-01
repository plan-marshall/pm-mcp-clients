/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.start;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import de.planmarshall.api.interaction.OperatorItem;
import de.planmarshall.api.interaction.OperatorLink;
import de.planmarshall.api.interaction.OperatorLinkRelation;
import de.planmarshall.api.interaction.OperatorScope;
import de.planmarshall.api.interaction.OperatorScopeKind;
import de.planmarshall.api.interaction.OperatorStateResource;
import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.source.git.ChangedFile;

/**
 * The representation of an entry's state for both clients (State Catalogue, Starting from the
 * Clients, Recipe Choice, and Source Units; design <em>Starting Work</em>), on the scheme of the
 * operator-interaction contract: the scope (the entry), the state, its facts, its items, and the
 * operator links an act follows.
 * <p>
 * The states are {@code start.choose}, {@code start.sources}, {@code start.ready},
 * {@code start.recipe}, {@code start.recipe-create}, {@code start.refused},
 * {@code start-epic.adopt}, {@code start-epic.adopt-failed}, {@code recipe.choose},
 * {@code recipe.parameters}, and {@code recipe.create}. The derivation is the contract:
 * <ul>
 *   <li>{@code start.choose} offers <em>a plan from sources</em>, <em>a recipe</em> (disabled with
 *       {@code start.choose.no_recipe} where the project offers none), and <em>abandon</em>;</li>
 *   <li>{@code start.sources} offers only the kinds that combine with those already in the list,
 *       each refused kind carrying its message key and arguments, the chat once a source is stored
 *       and every source from outside the machine is screened, and <em>Continue</em> only once
 *       every source is stored, screened, or accepted;</li>
 *   <li>{@code start.ready} shows the title and the derived id, the id of a recipe's plan and of a
 *       plan from a pull request as not editable, and asks no landing; an epic's ready step shows
 *       the project's {@code epic.auto_start} as the value in force with its origin, never asking
 *       it, and never asks the {@code parallelization_scope};</li>
 *   <li>{@code recipe.choose} and {@code recipe.parameters} offer the recipes and their resolved
 *       parameters, a parameter without an option carrying its reason;</li>
 *   <li>{@code start.refused} shows what is missing and the one action that goes on where it is
 *       fixed;</li>
 *   <li>{@code start-epic.adopt} offers the adoptable ledgers, and {@code start-epic.adopt-failed}
 *       the cause with <em>Try again</em>, <em>Back</em>, and <em>Abandon</em>.</li>
 * </ul>
 * The record carries the operator state and the start's own members side by side; on the wire the
 * operator state's members sit at the top level and the start's members beside them.
 *
 * @param operator        the operator state (scope, state id, title, facts, items, links)
 * @param entry           the entry the state shows, or {@code null} outside the source states
 * @param ready           the ready step, or {@code null} outside {@code start.ready}
 * @param chat            the chat link of the entry, or {@code null} where no chat is offered
 * @param refusal         the refusal of the setup gate, or {@code null}
 * @param adoptionFailure why an adoption failed, or {@code null}
 * @param recipes         the recipes offered at {@code start.recipe} and {@code recipe.choose}
 */
public record StartStateResource(
        OperatorStateResource operator,
        @Nullable EntryResource entry,
        @Nullable StartReadyResource ready,
        @Nullable StartChatResource chat,
        @Nullable StartRefusal refusal,
        @Nullable AdoptionFailure adoptionFailure,
        List<RecipeRef> recipes
) {

    public static final String FIELD_START = "start";
    public static final String FIELD_ENTRY = "entry";
    public static final String FIELD_READY = "ready";
    public static final String FIELD_CHAT = "chat";
    public static final String FIELD_REFUSAL = "refusal";
    public static final String FIELD_ADOPTION_FAILURE = "adoption_failure";
    public static final String FIELD_RECIPES = "recipes";

    /** The message key of the chat's role not qualifying. */
    public static final String CHAT_ROLE_UNQUALIFIED = "start.chat.role_unqualified";

    /** The link id that opens the sources of a plan. */
    public static final String LINK_SOURCES = "sources";

    /** The link id that opens the recipe choice. */
    public static final String LINK_RECIPE = "recipe";

    /** The link id that leaves the entry. */
    public static final String LINK_ABANDON = "abandon";

    /** The link id that goes on to the ready step. */
    public static final String LINK_START = "start";

    /** The link id that creates the work. */
    public static final String LINK_CREATE = "create";

    /** The link id that goes back to the sources. */
    public static final String LINK_BACK = "back";

    /** The link id that retries an adoption. */
    public static final String LINK_RETRY = "retry";

    /** The link id that starts a new epic instead of adopting one. */
    public static final String LINK_NEW = "new";

    /** The link id that opens the chat. */
    public static final String LINK_CHAT = "chat";

    private static final String ITEM_SOURCE_ID = "source_id";
    private static final String ITEM_KIND = "kind";
    private static final String ITEM_REFERENCE = "reference";
    private static final String ITEM_STATE = "state";
    private static final String ITEM_TRUST = "trust";
    private static final String ITEM_TITLE = "title";
    private static final String ITEM_VERDICT = "verdict";
    private static final String ITEM_FAILURE = "failure";
    private static final String ITEM_SPANS = "spans";
    private static final String ITEM_DISPOSITION = "disposition";
    private static final String ITEM_DISPOSITION_REASON = "disposition_reason";
    private static final String ITEM_CHANGED_FILES = "changed_files";
    private static final String TYPE = "StartStateResource";

    private static final Set<String> KNOWN_FIELDS = Set.of(
            OperatorStateResource.FIELD_SCOPE, OperatorStateResource.FIELD_STATE, OperatorStateResource.FIELD_PROGRESS,
            OperatorStateResource.FIELD_TITLE, OperatorStateResource.FIELD_TEXTS, OperatorStateResource.FIELD_FACTS,
            OperatorStateResource.FIELD_PROPOSAL, OperatorStateResource.FIELD_ITEMS,
            OperatorStateResource.FIELD_REVISION, OperatorStateResource.FIELD_LINKS, FIELD_START);

    private static final Set<String> KNOWN_START_FIELDS = Set.of(
            FIELD_ENTRY, FIELD_READY, FIELD_CHAT, FIELD_REFUSAL, FIELD_ADOPTION_FAILURE, FIELD_RECIPES);

    public StartStateResource {
        Objects.requireNonNull(operator, "operator");
        recipes = recipes == null ? List.of() : List.copyOf(recipes);
        if (ready != null && !operator.stateId().equals(StartState.READY.wireValue())) {
            throw new IllegalArgumentException("Only the ready state carries a ready step");
        }
        if (refusal != null && !operator.stateId().equals(StartState.REFUSED.wireValue())) {
            throw new IllegalArgumentException("Only the refused state carries a refusal");
        }
        if (adoptionFailure != null && !operator.stateId().equals(StartState.EPIC_ADOPT_FAILED.wireValue())) {
            throw new IllegalArgumentException("Only the adoption-failed state carries its cause");
        }
    }

    /**
     * Derives the representation of an entry's state.
     *
     * @param entry       the entry
     * @param ready       the ready step, required for {@code start.ready}
     * @param chat        the chat link, required where a chat is offered
     * @param refusal     the refusal, required for {@code start.refused}
     * @param failure     the adoption failure, required for {@code start-epic.adopt-failed}
     * @param recipes     the recipes offered in the project
     * @return the state representation
     */
    public static StartStateResource of(EntryResource entry, @Nullable StartReadyResource ready,
                                        @Nullable StartChatResource chat, @Nullable StartRefusal refusal, @Nullable AdoptionFailure failure,
                                        List<RecipeRef> recipes) {
        Objects.requireNonNull(entry, FIELD_ENTRY);
        var facts = factsOf(entry, ready, refusal, failure, recipes);
        var items = itemsOf(entry);
        var links = linksOf(entry, chat, recipes);
        var scope = new OperatorScope(OperatorScopeKind.ENTRY, entry.entryId(), entry.projectId(), null);
        var operator = new OperatorStateResource(scope, entry.state().wireValue(), null, "operator_wait", null,
                null, null, List.of(), facts, List.of(), items, null, links);
        return new StartStateResource(operator, entry, ready, chat, refusal, failure, recipes);
    }

    /**
     * @return the entry's id
     */
    public String entryId() {
        return operator.scope().id();
    }

    /**
     * @return the start state
     */
    public StartState state() {
        return StartState.fromWire(operator.stateId());
    }

    /**
     * @param linkId the link id
     * @return the offered link, or {@code null}
     */
    public @Nullable OperatorLink link(String linkId) {
        return operator.links().stream().filter(link -> link.id().equals(linkId)).findFirst().orElse(null);
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>(operator.toMap());
        var start = new LinkedHashMap<String, Object>();
        if (entry != null) {
            start.put(FIELD_ENTRY, entry.toMap());
        }
        if (ready != null) {
            start.put(FIELD_READY, ready.toMap());
        }
        if (chat != null) {
            start.put(FIELD_CHAT, chat.toMap());
        }
        if (refusal != null) {
            start.put(FIELD_REFUSAL, refusal.toMap());
        }
        if (adoptionFailure != null) {
            start.put(FIELD_ADOPTION_FAILURE, adoptionFailure.toMap());
        }
        if (!recipes.isEmpty()) {
            start.put(FIELD_RECIPES, recipes.stream().map(RecipeRef::toMap).toList());
        }
        map.put(FIELD_START, start);
        return map;
    }

    /**
     * @return the compact JSON representation
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Writes this state to a streaming generator.
     *
     * @param generator the generator
     * @throws IOException on write failure
     */
    public void writeTo(JsonGenerator generator) throws IOException {
        JsonTree.writeValue(generator, toMap());
    }

    /**
     * @param map the input map
     * @return the parsed state
     * @throws IOException if a required field is missing or a closed value is unknown
     */
    public static StartStateResource fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        StartJson.requireOnlyKnown(map, KNOWN_FIELDS, TYPE);
        var operator = OperatorStateResource.fromMap(operatorFields(map));
        var start = JsonTree.objectOrNull(map, FIELD_START, TYPE);
        if (start != null) {
            StartJson.requireOnlyKnown(start, KNOWN_START_FIELDS, TYPE + ".start");
        }
        var startFields = start != null ? start : Map.<String, Object>of();
        var entryMap = JsonTree.objectOrNull(startFields, FIELD_ENTRY, TYPE);
        var readyMap = JsonTree.objectOrNull(startFields, FIELD_READY, TYPE);
        var chatMap = JsonTree.objectOrNull(startFields, FIELD_CHAT, TYPE);
        var refusalMap = JsonTree.objectOrNull(startFields, FIELD_REFUSAL, TYPE);
        var failureMap = JsonTree.objectOrNull(startFields, FIELD_ADOPTION_FAILURE, TYPE);
        var recipes = StartJson.readEach(StartJson.readObjects(startFields, FIELD_RECIPES), RecipeRef::fromMap);
        try {
            return new StartStateResource(operator, entryMap != null ? EntryResource.fromMap(entryMap) : null,
                    readyMap != null ? StartReadyResource.fromMap(readyMap) : null,
                    chatMap != null ? StartChatResource.fromMap(chatMap) : null,
                    refusalMap != null ? StartRefusal.fromMap(refusalMap) : null,
                    failureMap != null ? AdoptionFailure.fromMap(failureMap) : null, recipes);
        } catch (IllegalArgumentException e) {
            throw new JsonFormatException("Invalid StartStateResource: " + e.getMessage(), e);
        }
    }

    /**
     * @param json the JSON object string
     * @return the parsed state
     * @throws IOException if the value is not an object or a field is malformed
     */
    public static StartStateResource fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for StartStateResource");
        }
        return fromMap(obj);
    }

    /**
     * Reads one state from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return the parsed state
     * @throws IOException on format failure
     */
    public static StartStateResource readFrom(JsonParser parser) throws IOException {
        var map = JsonTree.asObject(JsonTree.readValue(parser));
        if (map == null) {
            throw new JsonFormatException("Expected JSON object for StartStateResource");
        }
        return fromMap(map);
    }

    private static Map<String, Object> operatorFields(Map<String, Object> map) {
        var operator = new LinkedHashMap<String, Object>();
        for (var key : List.of(OperatorStateResource.FIELD_SCOPE, OperatorStateResource.FIELD_STATE,
                OperatorStateResource.FIELD_PROGRESS, OperatorStateResource.FIELD_TITLE,
                OperatorStateResource.FIELD_TEXTS, OperatorStateResource.FIELD_FACTS,
                OperatorStateResource.FIELD_PROPOSAL, OperatorStateResource.FIELD_ITEMS,
                OperatorStateResource.FIELD_REVISION, OperatorStateResource.FIELD_LINKS)) {
            if (map.containsKey(key)) {
                operator.put(key, map.get(key));
            }
        }
        return operator;
    }

    private static Map<String, Object> factsOf(EntryResource entry, @Nullable StartReadyResource ready,
                                               @Nullable StartRefusal refusal, @Nullable AdoptionFailure failure, List<RecipeRef> recipes) {
        var facts = new LinkedHashMap<String, Object>();
        facts.put("kind", entry.kind().wireValue());
        facts.put("project_id", entry.projectId());
        facts.put("can_continue", entry.canContinue());
        facts.put("source_count", Integer.toString(entry.sourceCount()));
        if (entry.title() != null) {
            facts.put(ITEM_TITLE, entry.title());
        }
        if (entry.planId() != null) {
            facts.put("plan_id", entry.planId());
        }
        if (ready != null) {
            facts.put(FIELD_READY, ready.toMap());
        }
        if (refusal != null) {
            facts.put(FIELD_REFUSAL, refusal.toMap());
        }
        if (failure != null) {
            facts.put(FIELD_ADOPTION_FAILURE, failure.toMap());
        }
        if (!recipes.isEmpty()) {
            facts.put("recipe_count", Integer.toString(recipes.size()));
        }
        var recipeChoice = entry.state() == StartState.CHOOSE || entry.state() == StartState.RECIPE
                || entry.state() == StartState.RECIPE_CHOOSE;
        if (entry.kind() == EntryKind.PLAN && recipeChoice && recipes.isEmpty()) {
            facts.put("no_recipe_reason", Map.of("key", "start.choose.no_recipe",
                    "args", Map.of("project", entry.projectId())));
        }
        return facts;
    }

    private static List<OperatorItem> itemsOf(EntryResource entry) {
        return entry.sources().stream().map(StartStateResource::itemOf).toList();
    }

    private static OperatorItem itemOf(SourceResource source) {
        var facts = new LinkedHashMap<String, Object>();
        facts.put(ITEM_SOURCE_ID, source.sourceId());
        facts.put(ITEM_KIND, source.kind().wireValue());
        facts.put(ITEM_REFERENCE, source.reference());
        facts.put(ITEM_STATE, source.state().wireValue());
        facts.put(ITEM_TRUST, source.trust().wireValue());
        if (source.title() != null) {
            facts.put(ITEM_TITLE, source.title());
        }
        if (source.verdict() != null) {
            facts.put(ITEM_VERDICT, source.verdict().wireValue());
        }
        if (source.failure() != null) {
            facts.put(ITEM_FAILURE, source.failure().toMap());
        }
        if (!source.spans().isEmpty()) {
            facts.put(ITEM_SPANS, source.spans().stream().map(ScreeningSpan::toMap).toList());
        }
        if (source.disposition() != null) {
            facts.put(ITEM_DISPOSITION, source.disposition().wireValue());
        }
        if (source.dispositionReason() != null) {
            facts.put(ITEM_DISPOSITION_REASON, source.dispositionReason());
        }
        if (!source.changedFiles().isEmpty()) {
            facts.put(ITEM_CHANGED_FILES, source.changedFiles().stream().map(ChangedFile::toMap).toList());
        }
        return new OperatorItem(source.sourceId(), facts, List.of());
    }

    private static List<OperatorLink> linksOf(EntryResource entry, @Nullable StartChatResource chat,
                                              List<RecipeRef> recipes) {
        return switch (entry.state()) {
            case CHOOSE -> chooseLinks(entry);
            case SOURCES -> sourcesLinks(entry, chat);
            case READY -> readyLinks(entry);
            case RECIPE -> recipeChooseLinks(entry, recipes);
            case RECIPE_CHOOSE -> recipeChooseLinks(entry, recipes);
            case RECIPE_PARAMETERS -> recipeParameterLinks(entry);
            case RECIPE_CREATE, RECIPE_STEP_CREATE -> List.of(createLink(entry));
            case REFUSED -> refusedLinks(entry);
            case EPIC_ADOPT -> epicAdoptLinks(entry);
            case EPIC_ADOPT_FAILED -> epicAdoptFailedLinks(entry);
        };
    }

    private static List<OperatorLink> chooseLinks(EntryResource entry) {
        return List.of(
                link(LINK_SOURCES, OperatorLinkRelation.NEXT, base(entry) + LINK_SOURCES, true),
                link(LINK_RECIPE, OperatorLinkRelation.ALT, base(entry) + LINK_RECIPE, false),
                abandonLink(entry));
    }

    private static List<OperatorLink> sourcesLinks(EntryResource entry, @Nullable StartChatResource chat) {
        var links = new ArrayList<OperatorLink>();
        entry.kindOffers().stream().filter(SourceKindOffer::available).forEach(offer -> {
            var id = "add_" + offer.kind().wireValue();
            links.add(link(id, OperatorLinkRelation.NEXT, base(entry) + id, false));
        });
        entry.sources().stream().map(source -> "remove:" + source.sourceId())
                .forEach(id -> links.add(link(id, OperatorLinkRelation.ABORT, base(entry) + id, false)));
        if (chat != null) {
            links.add(link(LINK_CHAT, OperatorLinkRelation.CONSULT, base(entry) + LINK_CHAT, false));
        }
        links.add(new OperatorLink(LINK_START, OperatorLinkRelation.NEXT, OperatorLink.METHOD_POST,
                base(entry) + LINK_START, Map.of(), entry.canContinue(), null));
        links.add(abandonLink(entry));
        return links;
    }

    private static List<OperatorLink> readyLinks(EntryResource entry) {
        return List.of(
                link(LINK_CREATE, OperatorLinkRelation.NEXT, base(entry) + LINK_CREATE, true),
                backLink(entry),
                abandonLink(entry));
    }

    private static List<OperatorLink> recipeChooseLinks(EntryResource entry, List<RecipeRef> recipes) {
        var links = new ArrayList<OperatorLink>();
        recipes.stream().map(recipe -> "choose:" + recipe.key())
                .forEach(id -> links.add(link(id, OperatorLinkRelation.NEXT, base(entry) + id, false)));
        links.add(link(LINK_SOURCES, OperatorLinkRelation.ALT, base(entry) + LINK_SOURCES, false));
        return links;
    }

    private static List<OperatorLink> recipeParameterLinks(EntryResource entry) {
        return List.of(
                link("set", OperatorLinkRelation.SUBMIT, base(entry) + "set", true),
                backLink(entry));
    }

    private static List<OperatorLink> refusedLinks(EntryResource entry) {
        var links = new ArrayList<OperatorLink>();
        if (entry.kind().epic()) {
            links.add(backLink(entry));
        }
        links.add(abandonLink(entry));
        return links;
    }

    private static List<OperatorLink> epicAdoptLinks(EntryResource entry) {
        var links = new ArrayList<OperatorLink>();
        entry.adoptableEpics().stream().map(epic -> "adopt:" + epic.epicId())
                .forEach(id -> links.add(link(id, OperatorLinkRelation.NEXT, base(entry) + id, false)));
        links.add(link(LINK_NEW, OperatorLinkRelation.ALT, base(entry) + LINK_NEW, false));
        links.add(abandonLink(entry));
        return links;
    }

    private static List<OperatorLink> epicAdoptFailedLinks(EntryResource entry) {
        return List.of(
                link(LINK_RETRY, OperatorLinkRelation.RETRY, base(entry) + LINK_RETRY, true),
                backLink(entry),
                abandonLink(entry));
    }

    private static OperatorLink createLink(EntryResource entry) {
        return link(LINK_CREATE, OperatorLinkRelation.NEXT, base(entry) + LINK_CREATE, true);
    }

    private static OperatorLink backLink(EntryResource entry) {
        return link(LINK_BACK, OperatorLinkRelation.ALT, base(entry) + LINK_BACK, false);
    }

    private static OperatorLink abandonLink(EntryResource entry) {
        return link(LINK_ABANDON, OperatorLinkRelation.ABORT, base(entry) + LINK_ABANDON, false);
    }

    private static String base(EntryResource entry) {
        return "/api/v1/entries/" + entry.entryId() + "/links/";
    }

    private static OperatorLink link(String id, OperatorLinkRelation relation, String href, boolean preselected) {
        return new OperatorLink(id, relation, OperatorLink.METHOD_POST, href, Map.of(), preselected, null);
    }

}
