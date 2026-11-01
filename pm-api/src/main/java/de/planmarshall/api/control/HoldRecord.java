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
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * What holds a plan, an epic, or a landing ({@code hold.show}, UC-CTL-4).
 *
 * @param holdId          the identifier of the hold
 * @param subjectId       the plan, epic, or landing that is held
 * @param subjectKind     {@code "plan"}, {@code "epic"}, or {@code "landing"}
 * @param kind            the hold's kind, one of {@link HoldKind}
 * @param cause           why it does not go on
 * @param attempts        how often it was tried
 * @param lastTriedAt     when it was last tried, or {@code null}
 * @param filePath        the tampered file (an integrity pause), or {@code null}
 * @param recordedSize    the recorded size of the tampered file, or {@code null}
 * @param recordedDigest  the recorded SHA-256 digest of the tampered file, or {@code null}
 * @param currentSize     the current size of the tampered file, or {@code null}
 * @param currentDigest   the current SHA-256 digest of the tampered file, or {@code null}
 * @param acts            the acts that resolve it, in wire order
 * @param pullRequestUrl  the pull request an epic's failed landing names, or {@code null}
 */
public record HoldRecord(
        String holdId,
        String subjectId,
        String subjectKind,
        String kind,
        String cause,
        int attempts,
        @Nullable Instant lastTriedAt,
        @Nullable String filePath,
        @Nullable Long recordedSize,
        @Nullable String recordedDigest,
        @Nullable Long currentSize,
        @Nullable String currentDigest,
        List<String> acts,
        @Nullable String pullRequestUrl
) {
    public HoldRecord {
        Objects.requireNonNull(holdId, "holdId");
        Objects.requireNonNull(subjectId, "subjectId");
        subjectKind = subjectKind == null || subjectKind.isBlank() ? "plan" : subjectKind;
        cause = cause != null ? cause : "";
        acts = acts != null ? List.copyOf(acts) : List.of();
    }

    /**
     * @param act the act value
     * @return {@code true} if the hold offers the act
     */
    public boolean offers(String act) {
        return acts.contains(act);
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("hold_id", holdId);
        map.put("subject_id", subjectId);
        map.put("subject_kind", subjectKind);
        map.put("kind", kind);
        map.put("cause", cause);
        map.put("attempts", attempts);
        if (lastTriedAt != null) {
            map.put("last_tried_at", lastTriedAt.toString());
        }
        if (filePath != null) {
            map.put("file_path", filePath);
        }
        if (recordedSize != null) {
            map.put("recorded_size", recordedSize);
        }
        if (recordedDigest != null) {
            map.put("recorded_digest", recordedDigest);
        }
        if (currentSize != null) {
            map.put("current_size", currentSize);
        }
        if (currentDigest != null) {
            map.put("current_digest", currentDigest);
        }
        map.put("acts", acts);
        if (pullRequestUrl != null) {
            map.put("pull_request_url", pullRequestUrl);
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
     * @return parsed hold
     * @throws IOException on format failure
     */
    public static HoldRecord fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var holdId = JsonTree.string(map, "hold_id");
        if (holdId == null) {
            holdId = JsonTree.string(map, "id");
        }
        if (holdId == null) {
            throw new JsonFormatException("Missing hold_id in HoldRecord");
        }
        var subjectId = JsonTree.string(map, "subject_id");
        if (subjectId == null) {
            subjectId = JsonTree.string(map, "plan_id");
        }
        if (subjectId == null) {
            subjectId = JsonTree.string(map, "epic_id");
        }
        if (subjectId == null) {
            subjectId = holdId;
        }

        var acts = new ArrayList<String>();
        var list = JsonTree.asArray(map.get("acts"));
        if (list != null) {
            for (var element : list) {
                if (element instanceof String text) {
                    acts.add(text);
                }
            }
        }

        var lastTriedStr = JsonTree.string(map, "last_tried_at");
        var attempts = JsonTree.integer(map, "attempts");

        return new HoldRecord(
                holdId,
                subjectId,
                JsonTree.string(map, "subject_kind"),
                JsonTree.string(map, "kind"),
                JsonTree.string(map, "cause"),
                attempts != null ? attempts : 0,
                lastTriedStr != null ? Instant.parse(lastTriedStr) : null,
                JsonTree.string(map, "file_path"),
                JsonTree.number(map, "recorded_size") != null ? JsonTree.number(map, "recorded_size").longValue() : null,
                JsonTree.string(map, "recorded_digest"),
                JsonTree.number(map, "current_size") != null ? JsonTree.number(map, "current_size").longValue() : null,
                JsonTree.string(map, "current_digest"),
                acts,
                JsonTree.string(map, "pull_request_url")
        );
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed hold
     * @throws IOException on format failure
     */
    public static HoldRecord fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for HoldRecord");
        }
        return fromMap(obj);
    }
}
