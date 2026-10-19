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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * The entry of a plan or an epic while it starts (State Catalogue, Starting from the Clients;
 * design <em>Starting Work</em>): its id and kind, the project that opened it, the state it stands
 * in, the sources gathered so far, the kinds still offered, whether it may continue, and the recipe
 * a plan's entry runs.
 * <p>
 * The entry is addressed at the top level by its id once opened. It never expires: it has no time
 * limit and no sweep removes it, and it stays among the project's open entries until its scope is
 * created or the operator abandons it.
 *
 * @param entryId          the entry's id ({@code ent-<hex>})
 * @param kind             the kind of work the entry starts
 * @param projectId        the project the entry belongs to
 * @param state            the state the entry stands in
 * @param title            the title proposed from the sources, or {@code null}
 * @param planId           the plan id proposed from the title, or {@code null}
 * @param sources          the sources gathered so far
 * @param kindOffers       every kind the start knows, with whether it is offered now and, where it
 *                         is not, the message key and arguments of the reason
 * @param canContinue      whether every source is stored, screened, or accepted
 * @param chatAvailable    whether the start chat may be opened
 * @param chatDisabledReason why it may not, or {@code null}
 * @param recipe           the recipe a plan's entry runs, or {@code null}
 * @param adoptableEpics   the epics offered for adoption, empty unless the entry is an epic's
 * @param adoptionFailure  why an adoption failed, or {@code null}
 */
public record EntryResource(
        String entryId,
        EntryKind kind,
        String projectId,
        StartState state,
        @Nullable String title,
        @Nullable String planId,
        List<SourceResource> sources,
        List<SourceKindOffer> kindOffers,
        boolean canContinue,
        boolean chatAvailable,
        @Nullable String chatDisabledReason,
        @Nullable RecipeRef recipe,
        List<AdoptableEpic> adoptableEpics,
        @Nullable AdoptionFailure adoptionFailure
) {

    public static final String FIELD_ENTRY_ID = "entry_id";
    public static final String FIELD_KIND = "kind";
    public static final String FIELD_PROJECT_ID = "project_id";
    public static final String FIELD_STATE = "state";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_PLAN_ID = "plan_id";
    public static final String FIELD_SOURCES = "sources";
    public static final String FIELD_KIND_OFFERS = "kind_offers";
    public static final String FIELD_CAN_CONTINUE = "can_continue";
    public static final String FIELD_CHAT_AVAILABLE = "chat_available";
    public static final String FIELD_CHAT_DISABLED_REASON = "chat_disabled_reason";
    public static final String FIELD_RECIPE = "recipe";
    public static final String FIELD_ADOPTABLE_EPICS = "adoptable_epics";
    public static final String FIELD_ADOPTION_FAILURE = "adoption_failure";

    private static final String TYPE = "EntryResource";

    private static final Set<String> KNOWN_FIELDS = Set.of(
            FIELD_ENTRY_ID, FIELD_KIND, FIELD_PROJECT_ID, FIELD_STATE, FIELD_TITLE, FIELD_PLAN_ID,
            FIELD_SOURCES, FIELD_KIND_OFFERS, FIELD_CAN_CONTINUE,
            FIELD_CHAT_AVAILABLE, FIELD_CHAT_DISABLED_REASON, FIELD_RECIPE, FIELD_ADOPTABLE_EPICS,
            FIELD_ADOPTION_FAILURE);

    public EntryResource {
        Objects.requireNonNull(entryId, FIELD_ENTRY_ID);
        Objects.requireNonNull(kind, FIELD_KIND);
        Objects.requireNonNull(projectId, FIELD_PROJECT_ID);
        Objects.requireNonNull(state, FIELD_STATE);
        sources = sources == null ? List.of() : List.copyOf(sources);
        kindOffers = kindOffers == null ? List.of() : List.copyOf(kindOffers);
        adoptableEpics = adoptableEpics == null ? List.of() : List.copyOf(adoptableEpics);
        if (recipe != null && kind.epic()) {
            throw new IllegalArgumentException("An epic's entry carries no recipe");
        }
        if (chatAvailable && chatDisabledReason != null) {
            throw new IllegalArgumentException("An available chat names no reason it is disabled");
        }
        if (!chatAvailable && recipe == null && chatDisabledReason == null) {
            throw new IllegalArgumentException("A disabled chat names its reason");
        }
    }

    /**
     * @return the number of sources gathered so far
     */
    public int sourceCount() {
        return sources.size();
    }

    /**
     * @return {@code true} if the entry is an epic's
     */
    public boolean epic() {
        return kind.epic();
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_ENTRY_ID, entryId);
        map.put(FIELD_KIND, kind.wireValue());
        map.put(FIELD_PROJECT_ID, projectId);
        map.put(FIELD_STATE, state.wireValue());
        if (title != null) {
            map.put(FIELD_TITLE, title);
        }
        if (planId != null) {
            map.put(FIELD_PLAN_ID, planId);
        }
        map.put(FIELD_SOURCES, sources.stream().map(SourceResource::toMap).toList());
        map.put(FIELD_KIND_OFFERS, kindOffers.stream().map(SourceKindOffer::toMap).toList());
        map.put(FIELD_CAN_CONTINUE, canContinue);
        map.put(FIELD_CHAT_AVAILABLE, chatAvailable);
        if (chatDisabledReason != null) {
            map.put(FIELD_CHAT_DISABLED_REASON, chatDisabledReason);
        }
        if (recipe != null) {
            map.put(FIELD_RECIPE, recipe.toMap());
        }
        if (!adoptableEpics.isEmpty()) {
            map.put(FIELD_ADOPTABLE_EPICS, adoptableEpics.stream().map(AdoptableEpic::toMap).toList());
        }
        if (adoptionFailure != null) {
            map.put(FIELD_ADOPTION_FAILURE, adoptionFailure.toMap());
        }
        return map;
    }

    /**
     * @return the compact JSON representation
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Writes this entry to a streaming generator.
     *
     * @param generator the generator
     * @throws IOException on write failure
     */
    public void writeTo(JsonGenerator generator) throws IOException {
        JsonTree.writeValue(generator, toMap());
    }

    /**
     * @param map the input map
     * @return the parsed entry
     * @throws IOException if a required field is missing or a closed value is unknown
     */
    public static EntryResource fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        StartJson.requireOnlyKnown(map, KNOWN_FIELDS, TYPE);
        var entryId = JsonTree.string(map, FIELD_ENTRY_ID);
        var projectId = JsonTree.string(map, FIELD_PROJECT_ID);
        if (entryId == null || projectId == null) {
            throw new JsonFormatException("Missing required fields for EntryResource: entry_id, project_id");
        }
        var kind = EntryKind.fromWireOrNull(JsonTree.string(map, FIELD_KIND));
        if (kind == null) {
            throw new JsonFormatException(
                    "Unknown " + FIELD_KIND + " in EntryResource: " + JsonTree.string(map, FIELD_KIND));
        }
        var state = StartState.fromWireOrNull(JsonTree.string(map, FIELD_STATE));
        if (state == null) {
            throw new JsonFormatException(
                    "Unknown " + FIELD_STATE + " in EntryResource: " + JsonTree.string(map, FIELD_STATE));
        }
        var recipeMap = JsonTree.objectOrNull(map, FIELD_RECIPE, TYPE);
        var failureMap = JsonTree.objectOrNull(map, FIELD_ADOPTION_FAILURE, TYPE);
        try {
            return new EntryResource(entryId, kind, projectId, state, JsonTree.string(map, FIELD_TITLE),
                    JsonTree.string(map, FIELD_PLAN_ID), readSources(map), readOffers(map),
                    StartJson.bool(map, FIELD_CAN_CONTINUE),
                    StartJson.bool(map, FIELD_CHAT_AVAILABLE), JsonTree.string(map, FIELD_CHAT_DISABLED_REASON),
                    recipeMap != null ? RecipeRef.fromMap(recipeMap) : null,
                    readAdoptable(map),
                    failureMap != null ? AdoptionFailure.fromMap(failureMap) : null);
        } catch (IllegalArgumentException e) {
            throw new JsonFormatException("Invalid EntryResource: " + e.getMessage(), e);
        }
    }

    /**
     * @param json the JSON object string
     * @return the parsed entry
     * @throws IOException if the value is not an object or a field is malformed
     */
    public static EntryResource fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for EntryResource");
        }
        return fromMap(obj);
    }

    /**
     * Reads one entry from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return the parsed entry
     * @throws IOException on format failure
     */
    public static EntryResource readFrom(JsonParser parser) throws IOException {
        var map = JsonTree.asObject(JsonTree.readValue(parser));
        if (map == null) {
            throw new JsonFormatException("Expected JSON object for EntryResource");
        }
        return fromMap(map);
    }

    private static List<SourceResource> readSources(Map<String, Object> map) throws IOException {
        return StartJson.readEach(StartJson.readObjects(map, FIELD_SOURCES), SourceResource::fromMap);
    }

    private static List<AdoptableEpic> readAdoptable(Map<String, Object> map) throws IOException {
        return StartJson.readEach(StartJson.readObjects(map, FIELD_ADOPTABLE_EPICS), AdoptableEpic::fromMap);
    }

    private static List<SourceKindOffer> readOffers(Map<String, Object> map) throws IOException {
        return StartJson.readEach(StartJson.readObjects(map, FIELD_KIND_OFFERS), SourceKindOffer::fromMap);
    }
}
