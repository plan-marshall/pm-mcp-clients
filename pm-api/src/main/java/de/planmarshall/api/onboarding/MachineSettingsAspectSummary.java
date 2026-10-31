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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * One line of the machine settings' overview: an aspect, a short summary of what it recorded,
 * and its current value (UC-ONB-10, {@code settings.choose}).
 *
 * @param aspect  the aspect
 * @param summary a short summary of what the aspect recorded
 * @param current the current value shown for the aspect, or {@code null}
 */
public record MachineSettingsAspectSummary(
        OnboardingAspect aspect,
        String summary,
        @Nullable String current
) {
    public MachineSettingsAspectSummary {
        Objects.requireNonNull(aspect, "aspect is required");
        summary = summary != null ? summary : aspect.wireValue();
    }

    /**
     * Converts to a JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("aspect", aspect.wireValue());
        map.put("step", aspect.step());
        map.put("summary", summary);
        if (current != null) {
            map.put("current", current);
        }
        return map;
    }

    /**
     * Parses from a JSON tree map.
     *
     * @param map map representation
     * @return parsed record
     * @throws IOException on format failure
     */
    public static MachineSettingsAspectSummary fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var aspect = OnboardingAspect.fromWire(JsonTree.string(map, "aspect"));
        if (aspect == null) {
            throw new JsonFormatException("Missing or unknown aspect in MachineSettingsAspectSummary");
        }
        return new MachineSettingsAspectSummary(aspect,
                JsonTree.string(map, "summary"),
                JsonTree.string(map, "current"));
    }
}
