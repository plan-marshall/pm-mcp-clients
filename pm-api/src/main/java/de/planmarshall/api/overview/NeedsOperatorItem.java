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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
 * @param blocks  what it blocks: one value of {@code plan}, {@code epic}, {@code setup},
 *                {@code entry}, {@code task} or {@code nothing}, never absent; a value this version
 *                does not know reads as {@link #BLOCKS_OTHER}
 */
public record NeedsOperatorItem(
        NeedsOperatorKind kind,
        @Nullable String link,
        @Nullable WorkLocation project,
        @Nullable WorkLocation epic,
        @Nullable WorkLocation plan,
        @Nullable Instant since,
        String blocks
) {

    /**
     * The values the one vocabulary of "what it blocks" carries, the same list a decision carries.
     */
    public static final Set<String> BLOCKS_VALUES =
            Set.of("plan", "epic", "setup", "entry", "task", "nothing");

    /**
     * The word for a value this version does not know: a runtime that serves a value added after
     * this client was built still renders a row, as an unknown kind does through
     * {@link NeedsOperatorKind#OTHER}, instead of failing the whole list or claiming the item
     * blocks no work. No runtime serves this value.
     */
    public static final String BLOCKS_OTHER = "other";

    private static final String BLOCKS_KEY = "blocks";

    public NeedsOperatorItem {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(blocks, BLOCKS_KEY);
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
        map.put(BLOCKS_KEY, blocks);
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
        var wire = JsonTree.string(map, "kind");
        if (wire == null || wire.isBlank()) {
            throw new JsonFormatException("Missing kind in NeedsOperatorItem");
        }
        var kind = NeedsOperatorKind.fromWireOrOther(wire);
        return new NeedsOperatorItem(
                kind,
                JsonTree.string(map, "link"),
                WorkLocation.fromValue(map.get("project")),
                WorkLocation.fromValue(map.get("epic")),
                WorkLocation.fromValue(map.get("plan")),
                instantOf(JsonTree.string(map, "since")),
                blocksOf(map));
    }

    /**
     * Reads what the item blocks: an absent value is refused, because the contract carries one; a
     * value outside the vocabulary reads as {@link #BLOCKS_OTHER}, so a value added after this
     * version was built never fails the whole list.
     *
     * @param map the map representation
     * @return the value of the vocabulary, or {@link #BLOCKS_OTHER}
     * @throws JsonFormatException when the item carries no {@code blocks}
     */
    private static String blocksOf(Map<String, Object> map) throws JsonFormatException {
        var wire = JsonTree.string(map, BLOCKS_KEY);
        if (wire == null || wire.isBlank()) {
            throw new JsonFormatException("Missing blocks in NeedsOperatorItem");
        }
        var value = wire.trim().toLowerCase(Locale.ROOT);
        return BLOCKS_VALUES.contains(value) ? value : BLOCKS_OTHER;
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
