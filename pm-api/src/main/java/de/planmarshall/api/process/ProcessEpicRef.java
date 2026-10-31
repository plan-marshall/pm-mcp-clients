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
 * Reference to a plan's parent epic in the process tree (UC-PLN-1).
 *
 * @param epicId unique identifier of the epic
 * @param title  display title of the epic
 * @param link   hypermedia link to the epic's process view, or {@code null}
 */
public record ProcessEpicRef(
        String epicId,
        String title,
        @Nullable String link
) {
    public ProcessEpicRef {
        Objects.requireNonNull(epicId, "epicId");
        title = title != null ? title : epicId;
    }

    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("epic_id", epicId);
        map.put("title", title);
        if (link != null) {
            map.put("link", link);
        }
        return map;
    }

    public String toJson() {
        return JsonTree.write(toMap());
    }

    public static ProcessEpicRef fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var epicId = JsonTree.string(map, "epic_id");
        if (epicId == null) {
            epicId = JsonTree.string(map, "id");
        }
        if (epicId == null) {
            throw new JsonFormatException("Missing epic_id in ProcessEpicRef");
        }
        var title = JsonTree.string(map, "title");
        var link = JsonTree.string(map, "link");
        return new ProcessEpicRef(epicId, title != null ? title : epicId, link);
    }

    public static ProcessEpicRef fromJson(String json) throws IOException {
        return fromMap(JsonTree.asObject(JsonTree.parse(json)));
    }
}
