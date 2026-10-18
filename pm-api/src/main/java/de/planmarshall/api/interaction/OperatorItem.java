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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * One row of a list a state shows (the check lines of a probe, the sources of an entry): the
 * {@code items} member of {@link OperatorStateResource}. Each item has its own {@code id}, its
 * {@code facts}, and the {@code links} offered for it.
 *
 * @param id    the id of the item, symbolised within the state's list
 * @param facts the typed values the item shows
 * @param links the operator links offered for the item
 */
public record OperatorItem(
        String id,
        Map<String, Object> facts,
        List<OperatorLink> links
) {
    public static final String FIELD_ID = "id";
    public static final String FIELD_FACTS = "facts";
    public static final String FIELD_LINKS = OperatorLink.FIELD_LINKS;

    public OperatorItem {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank in OperatorItem");
        }
        facts = facts == null ? Map.of() : Map.copyOf(facts);
        links = links == null ? List.of() : List.copyOf(links);
    }

    /**
     * Converts to a JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_ID, id);
        if (!facts.isEmpty()) {
            map.put(FIELD_FACTS, facts);
        }
        if (!links.isEmpty()) {
            map.put(FIELD_LINKS, OperatorLink.toMaps(links));
        }
        return map;
    }

    /**
     * Writes this item to a streaming generator.
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
     * @return parsed item
     * @throws IOException on format failure
     */
    public static OperatorItem fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var id = JsonTree.string(map, FIELD_ID);
        if (id == null || id.isBlank()) {
            throw new JsonFormatException("Missing " + FIELD_ID + " in OperatorItem");
        }
        return new OperatorItem(id, JsonTree.object(map, FIELD_FACTS), OperatorLink.fromArray(map.get(FIELD_LINKS)));
    }

    /**
     * Reads one item from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return parsed item
     * @throws IOException on format failure
     */
    public static OperatorItem readFrom(JsonParser parser) throws IOException {
        var map = JsonTree.asObject(JsonTree.readValue(parser));
        if (map == null) {
            throw new JsonFormatException("Expected JSON object for OperatorItem");
        }
        return fromMap(map);
    }

    /**
     * @param array the {@code items} member of a state, may be {@code null}
     * @return the parsed items, empty if the member is absent
     * @throws IOException if the member is no array of item objects
     */
    public static List<OperatorItem> fromArray(Object array) throws IOException {
        if (array == null) {
            return List.of();
        }
        var list = JsonTree.asArray(array);
        if (list == null) {
            throw new JsonFormatException("Expected a JSON array of operator items");
        }
        var items = new ArrayList<OperatorItem>(list.size());
        for (var element : list) {
            var object = JsonTree.asObject(element);
            if (object == null) {
                throw new JsonFormatException("Expected an operator item object in the items array");
            }
            items.add(fromMap(object));
        }
        return List.copyOf(items);
    }
}
