/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.changes;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * One line of a hunk. The text comes from the project and is never markup: a client shows it as
 * plain text and never follows it as a link.
 *
 * @param kind what the line is: context, removed or added
 * @param text the line's text, without the diff's leading marker
 */
public record PlanChangeLine(PlanChangeLineKind kind, String text) {

    private static final String FIELD_KIND = "kind";
    private static final String FIELD_TEXT = "text";

    public PlanChangeLine {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(text, "text");
    }

    /**
     * @return the map representation, in the order of the wire contract
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_KIND, kind.wire());
        map.put(FIELD_TEXT, text);
        return map;
    }

    /**
     * @return the JSON representation
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * @param map the map representation
     * @return the parsed line
     * @throws IOException if a required member is missing
     */
    public static PlanChangeLine fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var kindWire = JsonTree.string(map, FIELD_KIND);
        if (kindWire == null) {
            throw new JsonFormatException("Missing " + FIELD_KIND + " in a changed line");
        }
        var text = JsonTree.string(map, FIELD_TEXT);
        if (text == null) {
            throw new JsonFormatException("Missing " + FIELD_TEXT + " in a changed line");
        }
        return new PlanChangeLine(PlanChangeLineKind.fromWire(kindWire), text);
    }
}
