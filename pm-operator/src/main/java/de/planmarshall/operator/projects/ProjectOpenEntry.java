/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.projects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.json.JsonTree;

/**
 * One open start of a project, as {@code GET /api/v1/projects/{project_id}} reports it under
 * {@code open_entries} ({@code doc/plans/implemented/overview-contract.adoc}).
 *
 * <p>The terminal discovers an open entry when it opens a project's actions; the entry itself
 * stays owned by the start contract.
 *
 * @param entryId     the id of the open entry
 * @param kind        the entry's kind ({@code plan} or {@code epic})
 * @param sourceCount the number of sources it holds
 */
public record ProjectOpenEntry(String entryId, String kind, int sourceCount) {

    private static final String FIELD_ENTRY_ID = "entry_id";
    private static final String FIELD_KIND = "kind";
    private static final String FIELD_SOURCE_COUNT = "source_count";

    /**
     * @param entryId     the entry id
     * @param kind        the kind
     * @param sourceCount the number of sources
     */
    public ProjectOpenEntry {
        Objects.requireNonNull(entryId, "entryId");
        kind = kind == null || kind.isBlank() ? "plan" : kind;
        sourceCount = Math.max(0, sourceCount);
    }

    /**
     * @param body the JSON object of a project detail
     * @return its open entries, never {@code null}
     */
    public static List<ProjectOpenEntry> listFromProjectDetail(Map<String, Object> body) {
        var raw = JsonTree.asArray(body.get("open_entries"));
        if (raw == null) {
            return List.of();
        }
        var entries = new ArrayList<ProjectOpenEntry>();
        for (var item : raw) {
            var map = JsonTree.asObject(item);
            var entryId = map != null ? JsonTree.string(map, FIELD_ENTRY_ID) : null;
            if (map != null && entryId != null && !entryId.isBlank()) {
                var kind = JsonTree.string(map, FIELD_KIND);
                var count = JsonTree.integer(map, FIELD_SOURCE_COUNT);
                entries.add(new ProjectOpenEntry(entryId, kind, count != null ? count : 0));
            }
        }
        return List.copyOf(entries);
    }
}
