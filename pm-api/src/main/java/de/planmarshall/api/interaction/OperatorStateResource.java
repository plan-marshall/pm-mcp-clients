/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.interaction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import de.planmarshall.api.config.ConfigOrigin;
import de.planmarshall.api.id.DottedIdentifier;
import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * The operator representation of a scope's current state, the resource {@code pm-api} serves to both
 * operator clients over the local API (Hypermedia Format, Operator Representation): the scope, the
 * state (its id, node id, kind, and the configuration level it writes), its progress, its title and
 * texts, its facts, the values it proposes, the rows of a list it shows, the revision of the decision
 * or setting an act must name, and the operator links an act follows.
 * <p>
 * On the wire the state fields sit under the {@code state} object, the shape the specification
 * describes ({@code "state": {"state_id": …, "node_id": …, "kind": …, "level": …}}); the Java record
 * keeps them as its own components. The representation is written and read fail-closed against a
 * closed schema: every member of every object is one of the known fields, the scope kind and the
 * configuration level are catalogue members, the state id satisfies its grammar, and a link id is
 * unique within the state (its items' links included).
 *
 * @param scope    the scope the state belongs to
 * @param stateId  the id of the state ({@code <phase>.<step>[.<substate>]})
 * @param nodeId   the path of state ids from the scope's unit, or {@code null}
 * @param kind     the kind of the state ({@code operator_wait} among them), or {@code null}
 * @param level    the configuration level an act of the state writes ({@code machine}, {@code project}, {@code plan}), or {@code null}
 * @param progress the progress of the state where its unit counts its steps, or {@code null}
 * @param title    the title of the state, or {@code null}
 * @param texts    the texts of the state
 * @param facts    the typed values the state shows
 * @param proposal the values the state preselects for the operator
 * @param items    the rows of a list the state shows
 * @param revision the revision of the state's decision or setting an act must name, or {@code null}
 * @param links    the operator links the state offers
 */
public record OperatorStateResource(
        OperatorScope scope,
        String stateId,
        @Nullable String nodeId,
        @Nullable String kind,
        @Nullable ConfigOrigin level,
        @Nullable OperatorProgress progress,
        @Nullable OperatorText title,
        List<OperatorText> texts,
        Map<String, Object> facts,
        List<OperatorProposal> proposal,
        List<OperatorItem> items,
        @Nullable Long revision,
        List<OperatorLink> links
) {
    public static final String FIELD_SCOPE = "scope";
    public static final String FIELD_STATE = "state";
    public static final String FIELD_STATE_ID = "state_id";
    public static final String FIELD_NODE_ID = "node_id";
    public static final String FIELD_KIND = "kind";
    public static final String FIELD_LEVEL = "level";
    public static final String FIELD_PROGRESS = "progress";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_TEXTS = "texts";
    public static final String FIELD_FACTS = "facts";
    public static final String FIELD_PROPOSAL = "proposal";
    public static final String FIELD_ITEMS = "items";
    public static final String FIELD_REVISION = "revision";
    public static final String FIELD_LINKS = OperatorLink.FIELD_LINKS;

    private static final Set<String> KNOWN_FIELDS = Set.of(
            FIELD_SCOPE, FIELD_STATE, FIELD_PROGRESS, FIELD_TITLE, FIELD_TEXTS, FIELD_FACTS, FIELD_PROPOSAL,
            FIELD_ITEMS, FIELD_REVISION, FIELD_LINKS);
    private static final Set<String> KNOWN_STATE_FIELDS = Set.of(
            FIELD_STATE_ID, FIELD_NODE_ID, FIELD_KIND, FIELD_LEVEL);

    /** The configuration levels a state's {@code level} may name. */
    private static final Set<ConfigOrigin> CONFIG_LEVELS =
            Set.of(ConfigOrigin.MACHINE, ConfigOrigin.PROJECT, ConfigOrigin.PLAN);

    public OperatorStateResource {
        Objects.requireNonNull(scope, FIELD_SCOPE);
        if (!DottedIdentifier.stateId(stateId)) {
            throw new IllegalArgumentException("Invalid state id: " + stateId);
        }
        if (level != null && !CONFIG_LEVELS.contains(level)) {
            throw new IllegalArgumentException("A state writes configuration at machine, project, or plan: " + level.wire());
        }
        if (revision != null && revision < 0) {
            throw new IllegalArgumentException("revision cannot be negative: " + revision);
        }
        texts = texts == null ? List.of() : List.copyOf(texts);
        facts = JsonTree.immutableMap(facts);
        proposal = proposal == null ? List.of() : List.copyOf(proposal);
        items = items == null ? List.of() : List.copyOf(items);
        links = links == null ? List.of() : List.copyOf(links);
        requireUniqueLinkIds(links, items);
    }

    /**
     * @param value a candidate state id
     * @return {@code true} if the value satisfies the state id grammar
     */
    public static boolean matchesStateIdGrammar(String value) {
        return DottedIdentifier.stateId(value);
    }

    private static void requireUniqueLinkIds(List<OperatorLink> links, List<OperatorItem> items) {
        var seen = new HashSet<String>();
        for (var link : links) {
            if (!seen.add(link.id())) {
                throw new IllegalArgumentException("Duplicate operator link id in state: " + link.id());
            }
        }
        for (var item : items) {
            for (var link : item.links()) {
                if (!seen.add(link.id())) {
                    throw new IllegalArgumentException("Duplicate operator link id in state: " + link.id());
                }
            }
        }
    }

    /**
     * Converts to a JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var state = new LinkedHashMap<String, Object>();
        state.put(FIELD_STATE_ID, stateId);
        if (nodeId != null) {
            state.put(FIELD_NODE_ID, nodeId);
        }
        if (kind != null) {
            state.put(FIELD_KIND, kind);
        }
        if (level != null) {
            state.put(FIELD_LEVEL, level.wire());
        }

        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_SCOPE, scope.toMap());
        map.put(FIELD_STATE, state);
        if (progress != null) {
            map.put(FIELD_PROGRESS, progress.toMap());
        }
        if (title != null) {
            map.put(FIELD_TITLE, title.toMap());
        }
        if (!texts.isEmpty()) {
            map.put(FIELD_TEXTS, texts.stream().map(OperatorText::toMap).toList());
        }
        if (!facts.isEmpty()) {
            map.put(FIELD_FACTS, facts);
        }
        if (!proposal.isEmpty()) {
            map.put(FIELD_PROPOSAL, proposal.stream().map(OperatorProposal::toMap).toList());
        }
        if (!items.isEmpty()) {
            map.put(FIELD_ITEMS, items.stream().map(OperatorItem::toMap).toList());
        }
        if (revision != null) {
            map.put(FIELD_REVISION, revision);
        }
        map.put(FIELD_LINKS, OperatorLink.toMaps(links));
        return map;
    }

    /**
     * Serializes to a JSON string.
     *
     * @return JSON string
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Writes this resource to a streaming generator.
     *
     * @param generator the generator
     * @throws IOException on write failure
     */
    public void writeTo(JsonGenerator generator) throws IOException {
        JsonTree.writeValue(generator, toMap());
    }

    /**
     * Parses from a JSON tree map.
     *
     * @param map input map
     * @return parsed resource
     * @throws IOException on format failure
     */
    public static OperatorStateResource fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        requireOnlyKnown(map, KNOWN_FIELDS, "OperatorStateResource");

        var scopeMap = JsonTree.object(map, FIELD_SCOPE);
        if (scopeMap == null) {
            throw new JsonFormatException("Missing " + FIELD_SCOPE + " in OperatorStateResource");
        }
        var scope = OperatorScope.fromMap(scopeMap);

        var stateMap = JsonTree.object(map, FIELD_STATE);
        if (stateMap == null) {
            throw new JsonFormatException("Missing " + FIELD_STATE + " in OperatorStateResource");
        }
        requireOnlyKnown(stateMap, KNOWN_STATE_FIELDS, "OperatorStateResource state");

        var stateId = JsonTree.string(stateMap, FIELD_STATE_ID);
        if (!DottedIdentifier.stateId(stateId)) {
            throw new JsonFormatException("Invalid or missing " + FIELD_STATE_ID + " in OperatorStateResource: " + stateId);
        }

        ConfigOrigin level = null;
        if (stateMap.get(FIELD_LEVEL) != null) {
            level = ConfigOrigin.fromWireOrNull(JsonTree.string(stateMap, FIELD_LEVEL));
            if (level == null || !CONFIG_LEVELS.contains(level)) {
                throw new JsonFormatException(
                        "Invalid " + FIELD_LEVEL + " in OperatorStateResource: " + JsonTree.string(stateMap, FIELD_LEVEL));
            }
        }

        OperatorProgress progress = null;
        var progressMap = JsonTree.objectOrNull(map, FIELD_PROGRESS, "OperatorStateResource");
        if (progressMap != null) {
            progress = OperatorProgress.fromMap(progressMap);
        }

        OperatorText title = null;
        var titleMap = JsonTree.objectOrNull(map, FIELD_TITLE, "OperatorStateResource");
        if (titleMap != null) {
            title = OperatorText.fromMap(titleMap);
        }

        Long revision = null;
        if (map.get(FIELD_REVISION) != null) {
            revision = JsonTree.integralNumber(map, FIELD_REVISION, 0, Long.MAX_VALUE);
        }

        try {
            return new OperatorStateResource(
                    scope,
                    stateId,
                    JsonTree.string(stateMap, FIELD_NODE_ID),
                    JsonTree.string(stateMap, FIELD_KIND),
                    level,
                    progress,
                    title,
                    textsFromArray(map.get(FIELD_TEXTS)),
                    JsonTree.objectOrNull(map, FIELD_FACTS, "OperatorStateResource"),
                    proposalFromArray(map.get(FIELD_PROPOSAL)),
                    OperatorItem.fromArray(map.get(FIELD_ITEMS)),
                    revision,
                    OperatorLink.fromArray(map.get(FIELD_LINKS)));
        } catch (IllegalArgumentException e) {
            throw new JsonFormatException("Invalid OperatorStateResource: " + e.getMessage(), e);
        }
    }

    /**
     * Parses from a JSON string.
     *
     * @param json JSON string
     * @return parsed resource
     * @throws IOException on format failure
     */
    public static OperatorStateResource fromJson(String json) throws IOException {
        return fromMap(objectOf(JsonTree.parse(json)));
    }

    /**
     * Reads one resource from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return parsed resource
     * @throws IOException on format failure
     */
    public static OperatorStateResource readFrom(JsonParser parser) throws IOException {
        return fromMap(objectOf(JsonTree.readValue(parser)));
    }

    private static void requireOnlyKnown(Map<String, Object> map, Set<String> known, String type)
            throws IOException {
        for (var key : map.keySet()) {
            if (!known.contains(key)) {
                throw new JsonFormatException("Unknown field in " + type + ": " + key);
            }
        }
    }

    private static List<OperatorText> textsFromArray(Object array) throws IOException {
        if (array == null) {
            return List.of();
        }
        var list = JsonTree.asArray(array);
        if (list == null) {
            throw new JsonFormatException("Expected a JSON array of operator texts");
        }
        var texts = new ArrayList<OperatorText>(list.size());
        for (var element : list) {
            var object = JsonTree.asObject(element);
            if (object == null) {
                throw new JsonFormatException("Expected an operator text object in the texts array");
            }
            texts.add(OperatorText.fromMap(object));
        }
        return List.copyOf(texts);
    }

    private static List<OperatorProposal> proposalFromArray(Object array) throws IOException {
        if (array == null) {
            return List.of();
        }
        var list = JsonTree.asArray(array);
        if (list == null) {
            throw new JsonFormatException("Expected a JSON array of operator proposal values");
        }
        var proposal = new ArrayList<OperatorProposal>(list.size());
        for (var element : list) {
            var object = JsonTree.asObject(element);
            if (object == null) {
                throw new JsonFormatException("Expected an operator proposal object in the proposal array");
            }
            proposal.add(OperatorProposal.fromMap(object));
        }
        return List.copyOf(proposal);
    }

    private static Map<String, Object> objectOf(Object parsed) throws IOException {
        var map = JsonTree.asObject(parsed);
        if (map == null) {
            throw new JsonFormatException("Expected JSON object for OperatorStateResource");
        }
        return map;
    }
}
