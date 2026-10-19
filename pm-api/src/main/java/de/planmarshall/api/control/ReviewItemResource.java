/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.control;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * One item of an epic's findings or watches review (design <em>Reviewing Findings or Watches</em>).
 *
 * <p>An item from outside the machine carries {@code untrusted} and its text is shown as text,
 * never rendered.
 *
 * @param itemId    the item id ({@code F-0004})
 * @param register  {@code findings} or {@code watches}
 * @param statement what it says
 * @param rules     the rules a fix or a check must hold
 * @param origin    where it came from
 * @param trust     {@code trusted} or {@code untrusted}
 * @param state     the item's disposition
 * @param proposal  the planning role's proposal, or {@code null}
 */
public record ReviewItemResource(
        String itemId,
        String register,
        String statement,
        List<String> rules,
        String origin,
        String trust,
        String state,
        @Nullable String proposal
) {

    public ReviewItemResource {
        Objects.requireNonNull(itemId, "itemId");
        register = register != null && !register.isBlank() ? register : "findings";
        statement = statement != null ? statement : "";
        rules = rules != null ? List.copyOf(rules) : List.of();
        trust = trust != null && !trust.isBlank() ? trust : "trusted";
        state = state != null && !state.isBlank() ? state : ReviewDisposition.OPEN.value();
    }

    /**
     * @return the disposition as a closed enum
     */
    public ReviewDisposition disposition() {
        return ReviewDisposition.parse(state);
    }

    /**
     * @return {@code true} if the item came from outside the machine
     */
    public boolean isUntrusted() {
        return "untrusted".equals(trust);
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("item_id", itemId);
        map.put("register", register);
        map.put("statement", statement);
        map.put("rules", rules);
        map.put("origin", origin);
        map.put("trust", trust);
        map.put("state", state);
        if (proposal != null) {
            map.put("proposal", proposal);
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
     * @param map the input map
     * @return the parsed item
     * @throws IOException if a required field is missing
     */
    public static ReviewItemResource fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var itemId = JsonTree.string(map, "item_id");
        if (itemId == null) {
            throw new JsonFormatException("Missing item_id in ReviewItemResource");
        }
        var state = JsonTree.string(map, "state");
        if (state != null && !state.isBlank() && ReviewDisposition.fromValue(state) == null) {
            throw new JsonFormatException("Unknown state in ReviewItemResource: " + state);
        }
        return new ReviewItemResource(itemId, JsonTree.string(map, "register"),
                JsonTree.string(map, "statement"), ControlJson.readStrings(map.get("rules")),
                JsonTree.string(map, "origin"), JsonTree.string(map, "trust"),
                state, JsonTree.string(map, "proposal"));
    }

    /**
     * @param json the JSON object string
     * @return the parsed item
     * @throws IOException if the value is not an object
     */
    public static ReviewItemResource fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for ReviewItemResource");
        }
        return fromMap(obj);
    }
}
