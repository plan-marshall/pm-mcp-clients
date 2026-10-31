/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.overview;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * One item of the one list of everything that waits (UC-PLN-7).
 * <p>
 * The runtime derives it on read from the item's own place; a client keeps the order it is served
 * in and sorts nothing.
 *
 * @param kind    the closed kind of the wait
 * @param link    the path of the item itself, an absolute path of the local API, or {@code null}
 * @param project the project it belongs to, or {@code null} for the machine's own items
 * @param epic    the epic it belongs to, or {@code null}
 * @param plan    the plan it belongs to, or {@code null}
 * @param since   when it started to wait, or {@code null} when the runtime measures none
 * @param blocks  what it blocks: {@code plan}, {@code epic}, or {@code project}; absent when it
 *                blocks no work
 */
public record NeedsOperatorItem(
        NeedsOperatorKind kind,
        @Nullable String link,
        @Nullable WorkLocation project,
        @Nullable WorkLocation epic,
        @Nullable WorkLocation plan,
        @Nullable Instant since,
        @Nullable String blocks
) {

    public NeedsOperatorItem {
        Objects.requireNonNull(kind, "kind");
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("kind", kind.value());
        if (link != null) {
            map.put("link", link);
        }
        if (project != null) {
            map.put("project", project.toMap());
        }
        if (epic != null) {
            map.put("epic", epic.toMap());
        }
        if (plan != null) {
            map.put("plan", plan.toMap());
        }
        if (since != null) {
            map.put("since", since.toString());
        }
        if (blocks != null) {
            map.put("blocks", blocks);
        }
        return map;
    }

    /**
     * @return the JSON representation
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Parses one item.
     *
     * @param map the map representation
     * @return the parsed item
     * @throws IOException on format failure
     */
    public static NeedsOperatorItem fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var kind = NeedsOperatorKind.fromValue(JsonTree.string(map, "kind"));
        if (kind == null) {
            throw new JsonFormatException("Unknown kind in NeedsOperatorItem: " + JsonTree.string(map, "kind"));
        }
        return new NeedsOperatorItem(
                kind,
                JsonTree.string(map, "link"),
                WorkLocation.fromValue(map.get("project")),
                WorkLocation.fromValue(map.get("epic")),
                WorkLocation.fromValue(map.get("plan")),
                instantOf(JsonTree.string(map, "since")),
                JsonTree.string(map, "blocks"));
    }

    /**
     * Parses one item from a JSON object.
     *
     * @param json the JSON object
     * @return the parsed item
     * @throws IOException on format failure
     */
    public static NeedsOperatorItem fromJson(String json) throws IOException {
        var map = JsonTree.asObject(JsonTree.parse(json));
        if (map == null) {
            throw new JsonFormatException("Expected JSON object for NeedsOperatorItem");
        }
        return fromMap(map);
    }

    /**
     * @param value an RFC 3339 timestamp, or {@code null}
     * @return the instant, or {@code null} when absent or unreadable
     */
    static @Nullable Instant instantOf(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException _) {
            return null;
        }
    }
}
