/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.onboarding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * One item of the automatic run's result (UC-ONB-2; State Catalogue, Automatic Onboarding): the
 * aspect the run decided, what it decided, and the {@link AutoResultReason} it decided it for. The
 * result of {@code auto.result} is a list of these items, one per aspect of the first run.
 *
 * @param aspect the aspect the run decided
 * @param value  what the run decided, as the operator reads it on the result line
 * @param reason why the run decided it that way
 */
public record AutoResultItem(
        OnboardingAspect aspect,
        String value,
        AutoResultReason reason
) {
    private static final String ASPECT = "aspect";
    private static final String VALUE = "value";
    private static final String REASON = "reason";

    public AutoResultItem {
        Objects.requireNonNull(aspect, ASPECT);
        Objects.requireNonNull(value, VALUE);
        Objects.requireNonNull(reason, REASON);
    }

    /**
     * Converts to a JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(ASPECT, aspect.wireValue());
        map.put(VALUE, value);
        map.put(REASON, reason.wireValue());
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
     * Parses from a JSON tree map.
     *
     * @param map map representation
     * @return parsed item
     * @throws IOException on format failure
     */
    public static AutoResultItem fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var aspect = OnboardingAspect.fromWire(JsonTree.string(map, ASPECT));
        if (aspect == null) {
            throw new JsonFormatException("Unknown or missing aspect in AutoResultItem");
        }
        var value = JsonTree.string(map, VALUE);
        if (value == null) {
            throw new JsonFormatException("Missing value in AutoResultItem");
        }
        var reason = AutoResultReason.fromWire(JsonTree.string(map, REASON));
        if (reason == null) {
            throw new JsonFormatException("Unknown or missing reason in AutoResultItem");
        }
        return new AutoResultItem(aspect, value, reason);
    }

    /**
     * Parses from a JSON string.
     *
     * @param json JSON string
     * @return parsed item
     * @throws IOException on format failure
     */
    public static AutoResultItem fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for AutoResultItem");
        }
        return fromMap(obj);
    }

    /**
     * @param json an array of automatic-run result items
     * @return the parsed items, in source order
     * @throws IOException on format failure
     */
    public static List<AutoResultItem> listFromJson(String json) throws IOException {
        var array = JsonTree.asArray(JsonTree.parse(json));
        if (array == null) {
            throw new JsonFormatException("Expected JSON array for AutoResultItem list");
        }
        var items = new ArrayList<AutoResultItem>(array.size());
        for (var element : array) {
            var obj = JsonTree.asObject(element);
            if (obj != null) {
                items.add(fromMap(obj));
            }
        }
        return List.copyOf(items);
    }

    /**
     * @param items the automatic-run result items
     * @return the items as JSON tree maps, in order
     */
    public static List<Map<String, Object>> toMaps(List<AutoResultItem> items) {
        return items.stream().map(AutoResultItem::toMap).toList();
    }
}
