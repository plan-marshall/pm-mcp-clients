/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.source.git;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * The request that has a plan take over a pull request in the {@code adopt} lane (UC-STA-7).
 *
 * @param projectId             the enrolled project
 * @param prUrl                 the pull request's number or URL
 * @param screeningBypassReason why a flagged or unscreened pull request is adopted anyway, or
 *                              {@code null} while the text is clean
 */
public record PrAdoptRequest(
        String projectId,
        String prUrl,
        @Nullable String screeningBypassReason
) {

    private static final String FIELD_PROJECT_ID = "project_id";
    private static final String FIELD_PR_URL = "pr_url";
    private static final String FIELD_SCREENING_BYPASS_REASON = "screening_bypass_reason";

    public PrAdoptRequest {
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(prUrl, "prUrl");
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_PROJECT_ID, projectId);
        map.put(FIELD_PR_URL, prUrl);
        if (screeningBypassReason != null) {
            map.put(FIELD_SCREENING_BYPASS_REASON, screeningBypassReason);
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
     * @return the parsed request
     * @throws IOException if a required field is missing
     */
    public static PrAdoptRequest fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var projectId = JsonTree.string(map, FIELD_PROJECT_ID);
        var prUrl = JsonTree.string(map, FIELD_PR_URL);
        if (projectId == null || prUrl == null) {
            throw new JsonFormatException("Missing required fields for PrAdoptRequest: project_id or pr_url");
        }
        return new PrAdoptRequest(projectId, prUrl, JsonTree.string(map, FIELD_SCREENING_BYPASS_REASON));
    }

    /**
     * @param json the JSON object string
     * @return the parsed request
     * @throws IOException if the value is not an object or a field is missing
     */
    public static PrAdoptRequest fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for PrAdoptRequest");
        }
        return fromMap(obj);
    }
}
