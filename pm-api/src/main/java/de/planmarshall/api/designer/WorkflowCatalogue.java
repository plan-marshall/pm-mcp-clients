/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.designer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * The catalogue of building blocks of a scope (UC-PLN-3).
 * <p>
 * Served as {@code GET /api/v1/workflows/catalogue} (the machine scope) or
 * {@code GET /api/v1/projects/{id}/workflows/catalogue} (a project's). The catalogue carries the
 * runtime's version and a digest; when the digest the app holds differs from the one a units'
 * record or a placing names, the app loads the catalogue again.
 *
 * @param scope          {@code "machine"}, or the id of the project the catalogue was resolved for
 * @param runtimeVersion the runtime's version
 * @param digest         the digest of the catalogue
 * @param blocks         the blocks, grouped by kind, in the order a unit is written
 */
public record WorkflowCatalogue(
        String scope,
        String runtimeVersion,
        String digest,
        List<BuildingBlock> blocks
) {
    /** The scope value of a catalogue resolved for the machine. */
    public static final String SCOPE_MACHINE = "machine";

    private static final String FIELD_SCOPE = "scope";
    private static final String FIELD_RUNTIME_VERSION = "runtime_version";
    private static final String FIELD_DIGEST = "digest";
    private static final String FIELD_BLOCKS = "blocks";

    /**
     * Compact constructor validating required properties.
     */
    public WorkflowCatalogue {
        scope = scope != null ? scope : SCOPE_MACHINE;
        runtimeVersion = runtimeVersion != null ? runtimeVersion : "";
        digest = digest != null ? digest : "";
        blocks = blocks != null ? List.copyOf(blocks) : List.of();
    }

    /**
     * @return this catalogue as a JSON tree
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_SCOPE, scope);
        map.put(FIELD_RUNTIME_VERSION, runtimeVersion);
        map.put(FIELD_DIGEST, digest);
        var blockList = new ArrayList<Map<String, Object>>(blocks.size());
        for (var block : blocks) {
            blockList.add(block.toMap());
        }
        map.put(FIELD_BLOCKS, blockList);
        return map;
    }

    /**
     * @return this catalogue as compact JSON
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Parses a catalogue from a JSON tree.
     *
     * @param map the input tree
     * @return the catalogue
     * @throws IOException on a malformed tree
     */
    public static WorkflowCatalogue fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var blocks = new ArrayList<BuildingBlock>();
        for (var blockMap : DesignerJson.objectList(map, FIELD_BLOCKS)) {
            blocks.add(BuildingBlock.fromMap(blockMap));
        }
        return new WorkflowCatalogue(
                JsonTree.string(map, FIELD_SCOPE),
                JsonTree.string(map, FIELD_RUNTIME_VERSION),
                JsonTree.string(map, FIELD_DIGEST),
                blocks
        );
    }

    /**
     * Parses a catalogue from JSON.
     *
     * @param json the JSON text
     * @return the catalogue
     * @throws IOException on a malformed document
     */
    public static WorkflowCatalogue fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected a JSON object for WorkflowCatalogue");
        }
        return fromMap(obj);
    }
}
