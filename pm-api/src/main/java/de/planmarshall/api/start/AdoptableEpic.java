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
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * An epic offered for adoption at an epic's start (State Catalogue, {@code start-epic.adopt}; design
 * <em>Starting Work</em>, adopting an epic from elsewhere).
 * <p>
 * An epic whose git-tracked ledger exists but has no machine scope is offered above the list of
 * sources instead of a new epic, at most ten of them. Adopting takes no sources: the epic's machine
 * store is created from its ledger, the ledger is never rewritten, and the run ends at
 * <em>Started</em>.
 *
 * @param epicId the epic's id
 * @param title  the epic's title from its header
 */
public record AdoptableEpic(
        String epicId,
        String title
) {

    public static final String FIELD_EPIC_ID = "epic_id";
    public static final String FIELD_TITLE = "title";

    private static final Set<String> KNOWN_FIELDS = Set.of(FIELD_EPIC_ID, FIELD_TITLE);

    public AdoptableEpic {
        Objects.requireNonNull(epicId, FIELD_EPIC_ID);
        Objects.requireNonNull(title, FIELD_TITLE);
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_EPIC_ID, epicId);
        map.put(FIELD_TITLE, title);
        return map;
    }

    /**
     * @return the compact JSON representation
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Writes this offer to a streaming generator.
     *
     * @param generator the generator
     * @throws IOException on write failure
     */
    public void writeTo(JsonGenerator generator) throws IOException {
        JsonTree.writeValue(generator, toMap());
    }

    /**
     * @param map the input map
     * @return the parsed offer
     * @throws IOException if a required field is missing
     */
    public static AdoptableEpic fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        StartJson.requireOnlyKnown(map, KNOWN_FIELDS, "AdoptableEpic");
        var epicId = JsonTree.string(map, FIELD_EPIC_ID);
        var title = JsonTree.string(map, FIELD_TITLE);
        if (epicId == null || title == null) {
            throw new JsonFormatException("Missing required fields for AdoptableEpic: epic_id, title");
        }
        return new AdoptableEpic(epicId, title);
    }

    /**
     * @param json the JSON object string
     * @return the parsed offer
     * @throws IOException if the value is not an object or a field is missing
     */
    public static AdoptableEpic fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for AdoptableEpic");
        }
        return fromMap(obj);
    }

    /**
     * Reads one offer from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return the parsed offer
     * @throws IOException on format failure
     */
    public static AdoptableEpic readFrom(JsonParser parser) throws IOException {
        var map = JsonTree.asObject(JsonTree.readValue(parser));
        if (map == null) {
            throw new JsonFormatException("Expected JSON object for AdoptableEpic");
        }
        return fromMap(map);
    }
}
