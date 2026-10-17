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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * A single entry in a worker's live activity stream (UC-PLN-1).
 *
 * @param timestamp  timestamp string (e.g. "14:02:11" or ISO timestamp)
 * @param kind       entry kind (text, tool_call, file_changed, question, answer_ack, link)
 * @param toolName   name of the tool called (e.g. "read", "bash"), or {@code null}
 * @param toolTarget target operand of the tool or link (e.g. file path, command, link identifier), or {@code null}
 * @param toolFailed whether the tool invocation failed
 * @param content    text output or explanation, or {@code null}
 * @param size       size summary of the tool result (e.g. "4 KiB"), or {@code null}
 * @param files      list of modified files, or empty list
 * @param questions  list of questions asked, or empty list
 */
public record ActivityEntry(
        String timestamp,
        String kind,
        @Nullable String toolName,
        @Nullable String toolTarget,
        boolean toolFailed,
        @Nullable String content,
        @Nullable String size,
        List<String> files,
        List<String> questions
) {
    public ActivityEntry {
        Objects.requireNonNull(timestamp, "timestamp");
        Objects.requireNonNull(kind, "kind");
        files = files != null ? List.copyOf(files) : List.of();
        questions = questions != null ? List.copyOf(questions) : List.of();
    }

    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("timestamp", timestamp);
        map.put("kind", kind);
        if (toolName != null) {
            map.put("tool_name", toolName);
        }
        if (toolTarget != null) {
            map.put("tool_target", toolTarget);
        }
        if (toolFailed) {
            map.put("tool_failed", true);
        }
        if (content != null) {
            map.put("content", content);
        }
        if (size != null) {
            map.put("size", size);
        }
        if (!files.isEmpty()) {
            map.put("files", files);
        }
        if (!questions.isEmpty()) {
            map.put("questions", questions);
        }
        return map;
    }

    public String toJson() {
        return JsonTree.write(toMap());
    }

    public static ActivityEntry fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var timestamp = JsonTree.string(map, "timestamp");
        if (timestamp == null) {
            timestamp = JsonTree.string(map, "time");
        }
        if (timestamp == null) {
            timestamp = "";
        }
        var kind = JsonTree.string(map, "kind");
        if (kind == null) {
            kind = "text";
        }
        var toolName = JsonTree.string(map, "tool_name");
        var toolTarget = JsonTree.string(map, "tool_target");
        var toolFailed = Boolean.TRUE.equals(map.get("tool_failed"));
        var content = JsonTree.string(map, "content");
        var size = JsonTree.string(map, "size");

        List<String> files = new ArrayList<>();
        if (map.get("files") instanceof List<?> list) {
            for (var item : list) {
                if (item != null) {
                    files.add(item.toString());
                }
            }
        }

        List<String> questions = new ArrayList<>();
        if (map.get("questions") instanceof List<?> list) {
            for (var item : list) {
                if (item != null) {
                    questions.add(item.toString());
                }
            }
        }

        return new ActivityEntry(
                timestamp,
                kind,
                toolName,
                toolTarget,
                toolFailed,
                content,
                size,
                files,
                questions
        );
    }

    public static ActivityEntry fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for ActivityEntry");
        }
        return fromMap(obj);
    }
}
