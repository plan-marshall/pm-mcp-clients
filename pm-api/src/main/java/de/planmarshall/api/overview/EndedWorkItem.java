/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.overview;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Representation of an ended plan or epic in a project's ended work archive (UC-PLN-4).
 *
 * @param id                identifier of the plan or epic
 * @param kind              "plan" or "epic"
 * @param title             title of the ended item
 * @param outcome           plan outcome (e.g. "merged", "cancelled", "aborted"), or {@code null} for epics
 * @param terminalReason    epic terminal reason (e.g. "completed", "cancelled"), or {@code null} for plans
 * @param pullRequestNumber pull request number, or {@code null}
 * @param pullRequestUrl    pull request URL, or {@code null}
 * @param reason            operator-provided explanation, or {@code null}
 * @param endedAt           timestamp when work ended
 */
public record EndedWorkItem(
String id,
String kind,
String title,
@Nullable String outcome,
@Nullable String terminalReason,
@Nullable Integer pullRequestNumber,
@Nullable String pullRequestUrl,
@Nullable String reason,
Instant endedAt
) {
    public EndedWorkItem {
        Objects.requireNonNull(id, "id");
        kind = kind == null || kind.isBlank() ? "plan" : kind;
        title = title == null || title.isBlank() ? id : title;
        Objects.requireNonNull(endedAt, "endedAt");
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("id", id);
        map.put("kind", kind);
        map.put("title", title);
        if (outcome != null) {
            map.put("outcome", outcome);
        }
        if (terminalReason != null) {
            map.put("terminal_reason", terminalReason);
        }
        if (pullRequestNumber != null) {
            map.put("pull_request_number", pullRequestNumber);
        }
        if (pullRequestUrl != null) {
            map.put("pull_request_url", pullRequestUrl);
        }
        if (reason != null) {
            map.put("reason", reason);
        }
        map.put("ended_at", endedAt.toString());
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
     * @return parsed item
     * @throws IOException on format failure
     */
    public static EndedWorkItem fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var id = JsonTree.string(map, "id");
        if (id == null) {
            id = JsonTree.string(map, "item_id");
        }
        if (id == null) {
            id = JsonTree.string(map, "plan_id");
        }
        if (id == null) {
            id = JsonTree.string(map, "epic_id");
        }
        if (id == null) {
            throw new JsonFormatException("Missing id in EndedWorkItem");
        }

        var kind = JsonTree.string(map, "kind");
        var title = JsonTree.string(map, "title");
        var outcome = JsonTree.string(map, "outcome");
        var terminalReason = JsonTree.string(map, "terminal_reason");

        var prNum = JsonTree.number(map, "pull_request_number");
        Integer pullRequestNumber = prNum != null ? prNum.intValue() : null;
        var prUrl = JsonTree.string(map, "pull_request_url");

        var reason = JsonTree.string(map, "reason");
        var endedAtStr = JsonTree.string(map, "ended_at");
        if (endedAtStr == null) {
            throw new JsonFormatException("Missing ended_at in EndedWorkItem");
        }
        Instant endedAt = Instant.parse(endedAtStr);

        return new EndedWorkItem(
                id,
                kind != null ? kind : "plan",
                title != null ? title : id,
                outcome,
                terminalReason,
                pullRequestNumber,
                prUrl,
                reason,
                endedAt
        );
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed item
     * @throws IOException on format failure
     */
    public static EndedWorkItem fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for EndedWorkItem");
        }
        return fromMap(obj);
    }
}
