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
 * One action of a plan or an epic with its availability and, when disabled, the reason.
 *
 * <p>An action that applies but cannot run now is offered disabled with its reason (for example
 * {@code the runtime is reconnecting}); an action that does not apply is not offered at all.
 *
 * @param action    the action, one of {@link PlanAction} or an epic action value
 * @param available {@code true} if the action can run now
 * @param reason    why the action cannot run, or {@code null} when it is available
 */
public record ActionAvailability(
        String action,
        boolean available,
        @Nullable String reason
) {
    public ActionAvailability {
        Objects.requireNonNull(action, "action");
    }

    /**
     * @param action the action
     * @return an available action with no reason
     */
    public static ActionAvailability enabled(String action) {
        return new ActionAvailability(action, true, null);
    }

    /**
     * @param action the action
     * @param reason why it cannot run now
     * @return a disabled action with its reason
     */
    public static ActionAvailability disabled(String action, String reason) {
        return new ActionAvailability(action, false, reason);
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("action", action);
        map.put("available", available);
        if (reason != null) {
            map.put("reason", reason);
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
     * @param map map representation
     * @return parsed availability
     * @throws IOException on format failure
     */
    public static ActionAvailability fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var action = JsonTree.string(map, "action");
        if (action == null) {
            action = JsonTree.string(map, "name");
        }
        if (action == null) {
            throw new JsonFormatException("Missing action in ActionAvailability");
        }
        var available = Boolean.TRUE.equals(JsonTree.bool(map, "available"));
        var reason = JsonTree.string(map, "reason");
        return new ActionAvailability(action, available, reason);
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed availability
     * @throws IOException on format failure
     */
    public static ActionAvailability fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for ActionAvailability");
        }
        return fromMap(obj);
    }
}
