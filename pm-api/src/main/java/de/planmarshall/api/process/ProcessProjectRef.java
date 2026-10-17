/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.process;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Reference to a plan's project in the process tree (UC-PLN-1).
 *
 * @param projectId   project ID
 * @param displayName project display name
 * @param link        link to project resource, or {@code null}
 */
public record ProcessProjectRef(
        String projectId,
        String displayName,
        @Nullable String link
) {
    public ProcessProjectRef {
        Objects.requireNonNull(projectId, "projectId");
        displayName = displayName != null ? displayName : projectId;
    }

    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("project_id", projectId);
        map.put("display_name", displayName);
        if (link != null) {
            map.put("link", link);
        }
        return map;
    }

    public String toJson() {
        return JsonTree.write(toMap());
    }

    public static ProcessProjectRef fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var projectId = JsonTree.string(map, "project_id");
        if (projectId == null) {
            projectId = JsonTree.string(map, "id");
        }
        if (projectId == null) {
            throw new JsonFormatException("Missing project_id in ProcessProjectRef");
        }
        var displayName = JsonTree.string(map, "display_name");
        var link = JsonTree.string(map, "link");
        return new ProcessProjectRef(projectId, displayName != null ? displayName : projectId, link);
    }
}
