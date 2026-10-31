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
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * One model allowed on one enrolled harness, with its source (UC-ONB-5; Model Catalogue).
 *
 * @param harnessId the client id of the harness the model belongs to
 * @param modelId   the model id
 * @param source    where the entry comes from, {@code probe} or {@code operator}
 */
public record ModelCatalogueEntry(
        String harnessId,
        String modelId,
        String source
) {
    public ModelCatalogueEntry {
        Objects.requireNonNull(harnessId, "harnessId");
        Objects.requireNonNull(modelId, "modelId");
        source = source != null && !source.isBlank() ? source : "probe";
    }

    /**
     * Converts to a JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("harness_id", harnessId);
        map.put("model_id", modelId);
        map.put("source", source);
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
     * @return parsed record
     * @throws IOException on format failure
     */
    public static ModelCatalogueEntry fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var harnessId = JsonTree.string(map, "harness_id");
        var modelId = JsonTree.string(map, "model_id");
        if (modelId == null) {
            modelId = JsonTree.string(map, "id");
        }
        if (harnessId == null || modelId == null) {
            throw new JsonFormatException("Missing harness_id or model_id in ModelCatalogueEntry");
        }
        return new ModelCatalogueEntry(harnessId, modelId, JsonTree.string(map, "source"));
    }

    /**
     * Parses from a JSON string.
     *
     * @param json JSON string
     * @return parsed record
     * @throws IOException on format failure
     */
    public static ModelCatalogueEntry fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for ModelCatalogueEntry");
        }
        return fromMap(obj);
    }

    /**
     * @param json an array of catalogue entries
     * @return the parsed entries, in source order
     * @throws IOException on format failure
     */
    public static List<ModelCatalogueEntry> listFromJson(String json) throws IOException {
        var array = JsonTree.asArray(JsonTree.parse(json));
        if (array == null) {
            throw new JsonFormatException("Expected JSON array for ModelCatalogueEntry list");
        }
        var entries = new ArrayList<ModelCatalogueEntry>();
        for (var element : array) {
            var obj = JsonTree.asObject(element);
            if (obj != null) {
                entries.add(fromMap(obj));
            }
        }
        return entries;
    }

    /**
     * @param entry a catalogue entry
     * @param other the entry to compare with
     * @return {@code true} when both name the same harness and model
     */
    public boolean sameModel(@Nullable ModelCatalogueEntry other) {
        return other != null && harnessId.equals(other.harnessId()) && modelId.equals(other.modelId());
    }
}
