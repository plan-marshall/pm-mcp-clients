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
 * One finding of a decomposition's checks ({@code decomposition.check}, UC-STA-6).
 *
 * @param planId   the plan concerned, or {@code null} for a cut-wide finding
 * @param severity {@code error} (a failing check) or {@code warning}
 * @param message  the reason, as the client shows it
 */
public record DecompositionCheck(
        @Nullable String planId,
        String severity,
        String message
) {

    /** A failing check; {@code Agree} is disabled while one is present. */
    public static final String SEVERITY_ERROR = "error";

    /** A note that does not block the cut. */
    public static final String SEVERITY_WARNING = "warning";

    public DecompositionCheck {
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(message, "message");
    }

    /**
     * @return {@code true} if the check fails and blocks the cut
     */
    public boolean isError() {
        return SEVERITY_ERROR.equals(severity);
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        if (planId != null) {
            map.put("plan_id", planId);
        }
        map.put("severity", severity);
        map.put("message", message);
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
     * @return the parsed finding
     * @throws IOException if a required field is missing
     */
    public static DecompositionCheck fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var severity = JsonTree.string(map, "severity");
        var message = JsonTree.string(map, "message");
        if (severity == null || message == null) {
            throw new JsonFormatException("Missing required fields for DecompositionCheck: severity, message");
        }
        return new DecompositionCheck(JsonTree.string(map, "plan_id"), severity, message);
    }

    /**
     * @param json the JSON object string
     * @return the parsed finding
     * @throws IOException if the value is not an object
     */
    public static DecompositionCheck fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for DecompositionCheck");
        }
        return fromMap(obj);
    }
}
