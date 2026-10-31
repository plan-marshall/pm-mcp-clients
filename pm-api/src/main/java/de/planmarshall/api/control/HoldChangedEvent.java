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
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * A server-sent event of a hold opening, changing, or clearing (UC-CTL-4).
 *
 * <p>The event catalogue names the three events {@code hold.opened}, {@code hold.changed}, and
 * {@code hold.cleared}; the one carrying the change names it in {@link #event()}.
 *
 * @param event       the event name
 * @param holdId      the hold's identifier
 * @param subjectId   the plan or epic that is held
 * @param subjectKind {@code "plan"} or {@code "epic"}
 * @param kind        the hold's kind, or {@code null} when it cleared
 * @param cause       what holds the subject, or {@code null} when it cleared
 */
public record HoldChangedEvent(
        String event,
        String holdId,
        String subjectId,
        String subjectKind,
        @Nullable String kind,
        @Nullable String cause
) {

    /** Event name when a hold opens. */
    public static final String EVENT_OPENED = "hold.opened";

    /** Event name when a hold's cause or attempts change. */
    public static final String EVENT_CHANGED = "hold.changed";

    /** Event name when a hold clears. */
    public static final String EVENT_CLEARED = "hold.cleared";

    public HoldChangedEvent {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(holdId, "holdId");
        Objects.requireNonNull(subjectId, "subjectId");
        subjectKind = subjectKind != null && !subjectKind.isBlank() ? subjectKind : "plan";
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("hold_id", holdId);
        map.put("subject_id", subjectId);
        map.put("subject_kind", subjectKind);
        if (kind != null) {
            map.put("kind", kind);
        }
        if (cause != null) {
            map.put("cause", cause);
        }
        return map;
    }

    /**
     * Serializes to JSON string.
     *
     * @return JSON string
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Parses from JSON tree map.
     *
     * @param event the event name
     * @param map   map representation
     * @return parsed event
     * @throws IOException on format failure
     */
    public static HoldChangedEvent fromMap(String event, Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var holdId = JsonTree.string(map, "hold_id");
        if (holdId == null) {
            throw new JsonFormatException("Missing hold_id in HoldChangedEvent");
        }
        var subjectId = JsonTree.string(map, "subject_id");
        if (subjectId == null) {
            subjectId = JsonTree.string(map, "plan_id");
        }
        if (subjectId == null) {
            subjectId = holdId;
        }
        return new HoldChangedEvent(event, holdId, subjectId,
                JsonTree.string(map, "subject_kind"),
                JsonTree.string(map, "kind"),
                JsonTree.string(map, "cause"));
    }

    /**
     * Parses from JSON string.
     *
     * @param event the event name
     * @param json  JSON string
     * @return parsed event
     * @throws IOException on format failure
     */
    public static HoldChangedEvent fromJson(String event, String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for HoldChangedEvent");
        }
        return fromMap(event, obj);
    }
}
