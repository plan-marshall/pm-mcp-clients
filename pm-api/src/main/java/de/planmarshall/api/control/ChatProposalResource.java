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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * The one change a chat's role proposes, read back old against new (design <em>Chatting with the
 * Planning Role</em>, {@code chat.read-back}).
 *
 * <p>Nothing changes without <em>Accept</em>; a rejected proposal is gone. The <em>applies</em>
 * line names where it takes effect ({@code after the current task}, {@code when fix-relay starts
 * again}), or the archive where the one proposal is a follow-up plan.
 *
 * @param summary a one-sentence description of the change
 * @param before  the line before the change, or {@code null}
 * @param after   the line after the change, or {@code null}
 * @param applies where the change takes effect, or {@code null}
 */
public record ChatProposalResource(
        String summary,
        @Nullable String before,
        @Nullable String after,
        @Nullable String applies
) {

    public ChatProposalResource {
        Objects.requireNonNull(summary, "summary");
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("summary", summary);
        if (before != null) {
            map.put("before", before);
        }
        if (after != null) {
            map.put("after", after);
        }
        if (applies != null) {
            map.put("applies", applies);
        }
        return map;
    }

    /**
     * @return the compact JSON representation
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * @param map the input map
     * @return the parsed proposal
     * @throws IOException if a required field is missing
     */
    public static ChatProposalResource fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var summary = JsonTree.string(map, "summary");
        if (summary == null) {
            throw new JsonFormatException("Missing summary in ChatProposalResource");
        }
        return new ChatProposalResource(summary, JsonTree.string(map, "before"),
                JsonTree.string(map, "after"), JsonTree.string(map, "applies"));
    }

    /**
     * @param json the JSON object string
     * @return the parsed proposal
     * @throws IOException if the value is not an object
     */
    public static ChatProposalResource fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for ChatProposalResource");
        }
        return fromMap(obj);
    }
}
