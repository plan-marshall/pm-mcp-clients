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

import de.planmarshall.api.config.ConfigOrigin;
import de.planmarshall.api.interaction.OperatorItem;
import de.planmarshall.api.interaction.OperatorLink;
import de.planmarshall.api.interaction.OperatorLinkRelation;
import de.planmarshall.api.interaction.OperatorScope;
import de.planmarshall.api.interaction.OperatorScopeKind;
import de.planmarshall.api.interaction.OperatorStateResource;
import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

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
        Objects.requireNonNull(entry, "entry");
        var facts = factsOf(entry, ready, refusal, failure, recipes);
        var items = itemsOf(entry);
        var links = linksOf(entry, chat);
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
        StartJson.requireOnlyKnown(map, KNOWN_FIELDS, "StartStateResource");
        var operator = OperatorStateResource.fromMap(operatorFields(map));
        var start = JsonTree.objectOrNull(map, FIELD_START, "StartStateResource");
        if (start != null) {
            StartJson.requireOnlyKnown(start, KNOWN_START_FIELDS, "StartStateResource.start");
        }
        var startFields = start != null ? start : Map.<String, Object>of();
        var entryMap = JsonTree.objectOrNull(startFields, FIELD_ENTRY, "StartStateResource");
        var readyMap = JsonTree.objectOrNull(startFields, FIELD_READY, "StartStateResource");
        var chatMap = JsonTree.objectOrNull(startFields, FIELD_CHAT, "StartStateResource");
        var refusalMap = JsonTree.objectOrNull(startFields, FIELD_REFUSAL, "StartStateResource");
        var failureMap = JsonTree.objectOrNull(startFields, FIELD_ADOPTION_FAILURE, "StartStateResource");
        var recipes = new ArrayList<RecipeRef>();
        for (var object : StartJson.readObjects(startFields, FIELD_RECIPES)) {
            recipes.add(RecipeRef.fromMap(object));
        }
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
            facts.put("title", entry.title());
        }
        if (entry.planId() != null) {
            facts.put("plan_id", entry.planId());
        }
        if (ready != null) {
            facts.put("ready", ready.toMap());
        }
        if (refusal != null) {
            facts.put("refusal", refusal.toMap());
        }
        if (failure != null) {
            facts.put("adoption_failure", failure.toMap());
        }
        if (!recipes.isEmpty()) {
            facts.put("recipe_count", Integer.toString(recipes.size()));
        }
        return facts;
    }

    private static List<OperatorItem> itemsOf(EntryResource entry) {
        var items = new ArrayList<OperatorItem>();
        for (var source : entry.sources()) {
            var facts = new LinkedHashMap<String, Object>();
            facts.put("source_id", source.sourceId());
            facts.put("kind", source.kind().wireValue());
            facts.put("reference", source.reference());
            facts.put("state", source.state().wireValue());
            facts.put("trust", source.trust().wireValue());
            if (source.title() != null) {
                facts.put("title", source.title());
            }
            if (source.verdict() != null) {
                facts.put("verdict", source.verdict().wireValue());
            }
            if (source.failure() != null) {
                facts.put("failure", source.failure().toMap());
            }
            if (!source.spans().isEmpty()) {
                facts.put("spans", source.spans().stream().map(ScreeningSpan::toMap).toList());
            }
            if (source.disposition() != null) {
                facts.put("disposition", source.disposition().wireValue());
            }
            if (source.dispositionReason() != null) {
                facts.put("disposition_reason", source.dispositionReason());
            }
            if (!source.changedFiles().isEmpty()) {
                facts.put("changed_files", source.changedFiles().stream()
                        .map(file -> file.toMap()).toList());
            }
            items.add(new OperatorItem(source.sourceId(), facts, List.of()));
        }
        return items;
    }

    private static List<OperatorLink> linksOf(EntryResource entry, @Nullable StartChatResource chat) {
        var links = new ArrayList<OperatorLink>();
        var base = "/api/v1/entries/" + entry.entryId() + "/links/";
        switch (entry.state()) {
            case CHOOSE -> {
                links.add(link(LINK_SOURCES, OperatorLinkRelation.NEXT, base + LINK_SOURCES, true, null));
                if (entry.recipe() != null) {
                    links.add(link(LINK_RECIPE, OperatorLinkRelation.ALT, base + LINK_RECIPE, false, null));
                } else {
                    links.add(new OperatorLink(LINK_RECIPE, OperatorLinkRelation.ALT, OperatorLink.METHOD_POST,
                            base + LINK_RECIPE, Map.of(), false, null));
                }
                links.add(link(LINK_ABANDON, OperatorLinkRelation.ABORT, base + LINK_ABANDON, false, null));
            }
            case SOURCES -> {
                for (var offer : entry.kindOffers()) {
                    if (offer.available()) {
                        links.add(link("add_" + offer.kind().wireValue(), OperatorLinkRelation.NEXT,
                                base + "add_" + offer.kind().wireValue(), false, null));
                    }
                }
                for (var source : entry.sources()) {
                    links.add(link("remove:" + source.sourceId(), OperatorLinkRelation.ABORT,
                            base + "remove:" + source.sourceId(), false, null));
                }
                if (chat != null) {
                    links.add(new OperatorLink(LINK_CHAT, OperatorLinkRelation.CONSULT, OperatorLink.METHOD_POST,
                            base + LINK_CHAT, Map.of(), false, null));
                }
                links.add(new OperatorLink(LINK_START, OperatorLinkRelation.NEXT, OperatorLink.METHOD_POST,
                        base + LINK_START, Map.of(), entry.canContinue(), null));
                links.add(link(LINK_ABANDON, OperatorLinkRelation.ABORT, base + LINK_ABANDON, false, null));
            }
            case READY -> {
                links.add(link(LINK_CREATE, OperatorLinkRelation.NEXT, base + LINK_CREATE, true, null));
                links.add(link(LINK_BACK, OperatorLinkRelation.ALT, base + LINK_BACK, false, null));
                links.add(link(LINK_ABANDON, OperatorLinkRelation.ABORT, base + LINK_ABANDON, false, null));
            }
            case RECIPE -> links.add(link(LINK_BACK, OperatorLinkRelation.ALT, base + LINK_BACK, false, null));
            case RECIPE_CHOOSE -> {
                for (var recipe : entry.recipe() != null ? List.of(entry.recipe()) : List.<RecipeRef>of()) {
                    links.add(link("choose:" + recipe.key(), OperatorLinkRelation.NEXT,
                            base + "choose:" + recipe.key(), false, null));
                }
                links.add(link(LINK_BACK, OperatorLinkRelation.ALT, base + LINK_BACK, false, null));
            }
            case RECIPE_PARAMETERS -> {
                links.add(link("set", OperatorLinkRelation.SUBMIT, base + "set", true, null));
                links.add(link(LINK_BACK, OperatorLinkRelation.ALT, base + LINK_BACK, false, null));
            }
            case RECIPE_CREATE, RECIPE_STEP_CREATE ->
                links.add(link(LINK_CREATE, OperatorLinkRelation.NEXT, base + LINK_CREATE, true, null));
            case REFUSED -> {
                if (entry.kind().epic()) {
                    links.add(link(LINK_BACK, OperatorLinkRelation.ALT, base + LINK_BACK, false, null));
                }
                links.add(link(LINK_ABANDON, OperatorLinkRelation.ABORT, base + LINK_ABANDON, false, null));
            }
            case EPIC_ADOPT -> {
                for (var epic : entry.adoptableEpics()) {
                    links.add(link("adopt:" + epic.epicId(), OperatorLinkRelation.NEXT,
                            base + "adopt:" + epic.epicId(), false, null));
                }
                links.add(link(LINK_NEW, OperatorLinkRelation.ALT, base + LINK_NEW, false, null));
                links.add(link(LINK_ABANDON, OperatorLinkRelation.ABORT, base + LINK_ABANDON, false, null));
            }
            case EPIC_ADOPT_FAILED -> {
                links.add(link(LINK_RETRY, OperatorLinkRelation.RETRY, base + LINK_RETRY, true, null));
                links.add(link(LINK_BACK, OperatorLinkRelation.ALT, base + LINK_BACK, false, null));
                links.add(link(LINK_ABANDON, OperatorLinkRelation.ABORT, base + LINK_ABANDON, false, null));
            }
        }
        return links;
    }

    private static OperatorLink link(String id, OperatorLinkRelation relation, String href, boolean preselected,
                                     @Nullable String confirm) {
        return new OperatorLink(id, relation, OperatorLink.METHOD_POST, href, Map.of(), preselected, confirm);
    }

    /**
     * @param value a configuration origin
     * @return the wire value, or {@code null}
     */
    public static @Nullable String originWire(@Nullable ConfigOrigin value) {
        return value != null ? value.wire() : null;
    }
}
