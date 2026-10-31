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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * One item of a {@code needs_operator} summary: its kind and a link to it.
 *
 * @param kind the closed kind of the wait
 * @param link the path of the item, an absolute path of the local API, or {@code null}
 */
public record NeedsOperatorItemRef(NeedsOperatorKind kind, @Nullable String link) {

    public NeedsOperatorItemRef {
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
        return map;
    }

    /**
     * Parses one item reference.
     *
     * @param map the map representation
     * @return the parsed reference
     * @throws IOException on format failure
     */
    public static NeedsOperatorItemRef fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var kind = NeedsOperatorKind.fromValue(JsonTree.string(map, "kind"));
        if (kind == null) {
            throw new JsonFormatException(
                    "Unknown kind in NeedsOperatorItemRef: " + JsonTree.string(map, "kind"));
        }
        return new NeedsOperatorItemRef(kind, JsonTree.string(map, "link"));
    }
}
