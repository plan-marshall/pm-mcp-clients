/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.start;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * The listing of files for the web app's picker, confined to the project's checkout and the
 * operator's home directory ({@code GET /api/v1/projects/{project_id}/files?root=&path=};
 * State Catalogue, Documents as a Source; Runtime &amp; Interaction Model, Local API).
 * <p>
 * The listing is loopback-only for the home directory and for a move, and it never carries an
 * absolute path of the machine: every entry is named relative to its root. A directory that holds
 * more entries than the listing bound is truncated and says so, so a client can ask again rather
 * than read a partial listing as complete.
 *
 * @param projectId the project whose checkout is listed
 * @param root      the root the listing stands in
 * @param path      the directory listed, relative to the root ({@code ""} for the root)
 * @param parent    the parent directory relative to the root, or {@code null} at the root
 * @param entries   the entries of the directory, in name order
 * @param truncated whether the directory holds more entries than the listing shows
 */
public record ConfinedFileListResource(
        String projectId,
        ConfinedRoot root,
        String path,
        @Nullable String parent,
        List<ConfinedFile> entries,
        boolean truncated
) {

    public static final String FIELD_PROJECT_ID = "project_id";
    public static final String FIELD_ROOT = "root";
    public static final String FIELD_PATH = "path";
    public static final String FIELD_PARENT = "parent";
    public static final String FIELD_ENTRIES = "entries";
    public static final String FIELD_TRUNCATED = "truncated";

    private static final Set<String> KNOWN_FIELDS = Set.of(
            FIELD_PROJECT_ID, FIELD_ROOT, FIELD_PATH, FIELD_PARENT, FIELD_ENTRIES, FIELD_TRUNCATED);

    public ConfinedFileListResource {
        Objects.requireNonNull(projectId, FIELD_PROJECT_ID);
        Objects.requireNonNull(root, FIELD_ROOT);
        path = path == null ? "" : path;
        entries = entries == null ? List.of() : List.copyOf(entries);
    }

    /**
     * @return the number of files (not directories) the listing shows
     */
    public long fileCount() {
        return entries.stream().filter(entry -> !entry.directory()).count();
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_PROJECT_ID, projectId);
        map.put(FIELD_ROOT, root.wireValue());
        map.put(FIELD_PATH, path);
        if (parent != null) {
            map.put(FIELD_PARENT, parent);
        }
        map.put(FIELD_ENTRIES, entries.stream().map(ConfinedFile::toMap).toList());
        map.put(FIELD_TRUNCATED, truncated);
        return map;
    }

    /**
     * @return the compact JSON representation
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Writes this listing to a streaming generator.
     *
     * @param generator the generator
     * @throws IOException on write failure
     */
    public void writeTo(JsonGenerator generator) throws IOException {
        JsonTree.writeValue(generator, toMap());
    }

    /**
     * @param map the input map
     * @return the parsed listing
     * @throws IOException if a required field is missing or a closed value is unknown
     */
    public static ConfinedFileListResource fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        StartJson.requireOnlyKnown(map, KNOWN_FIELDS, "ConfinedFileListResource");
        var projectId = JsonTree.string(map, FIELD_PROJECT_ID);
        if (projectId == null) {
            throw new JsonFormatException("Missing required field for ConfinedFileListResource: project_id");
        }
        var root = ConfinedRoot.fromWireOrNull(JsonTree.string(map, FIELD_ROOT));
        if (root == null) {
            throw new JsonFormatException("Unknown " + FIELD_ROOT + " in ConfinedFileListResource: "
                    + JsonTree.string(map, FIELD_ROOT));
        }
        var entries = StartJson.readEach(StartJson.readObjects(map, FIELD_ENTRIES), ConfinedFile::fromMap);
        return new ConfinedFileListResource(projectId, root, JsonTree.string(map, FIELD_PATH),
                JsonTree.string(map, FIELD_PARENT), entries, StartJson.bool(map, FIELD_TRUNCATED));
    }

    /**
     * @param json the JSON object string
     * @return the parsed listing
     * @throws IOException if the value is not an object or a field is malformed
     */
    public static ConfinedFileListResource fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for ConfinedFileListResource");
        }
        return fromMap(obj);
    }

    /**
     * Reads one listing from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return the parsed listing
     * @throws IOException on format failure
     */
    public static ConfinedFileListResource readFrom(JsonParser parser) throws IOException {
        var map = JsonTree.asObject(JsonTree.readValue(parser));
        if (map == null) {
            throw new JsonFormatException("Expected JSON object for ConfinedFileListResource");
        }
        return fromMap(map);
    }
}
