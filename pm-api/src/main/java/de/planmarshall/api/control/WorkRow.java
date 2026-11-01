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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * One row of an epic's workstreams: a plan with its status and, when staged, what it waits for
 * (design <em>Epic View</em>; specification <em>The Epic as a Resource</em>).
 *
 * <p>The status is the queue row's status ({@code staged}, {@code running}, {@code pausing},
 * {@code paused}, {@code shipped}, {@code returned}, {@code cancelled}, {@code abandoned},
 * {@code waiting for you}, {@code held}); a staged row carries its wait reason from
 * {@link EpicWaitReason}. On a later decomposition run {@link #change()} marks the plan
 * {@code unchanged}, {@code changed}, {@code new}, or {@code dropped}.
 *
 * @param planId            the plan identifier
 * @param title             the plan's title
 * @param status            the queue row's status
 * @param waitReason        what the staged plan waits for, or {@code null}
 * @param waitPlanId        the plan a dependency or overlap waits for, or {@code null}
 * @param overlapFiles      the files in common with the plan it overlaps, when a wait is an overlap
 * @param pullRequestNumber a shipped plan's pull request number, or {@code null}
 * @param pullRequestUrl    the pull request's web address, or {@code null}
 * @param change            the plan's change marker on a later run, or {@code null}
 * @param objective         the staged plan's objective, or {@code null}
 * @param deliverables      the staged plan's deliverables
 * @param surface           the staged plan's expected surface
 * @param dependencies      the staged plan's dependencies
 */
public record WorkRow(
        String planId,
        String title,
        String status,
        @Nullable String waitReason,
        @Nullable String waitPlanId,
        List<String> overlapFiles,
        @Nullable Integer pullRequestNumber,
        @Nullable String pullRequestUrl,
        @Nullable String change,
        @Nullable String objective,
        List<String> deliverables,
        List<String> surface,
        List<String> dependencies
) {

    public WorkRow {
        Objects.requireNonNull(planId, "planId");
        title = title != null ? title : planId;
        status = status == null || status.isBlank() ? "staged" : status;
        overlapFiles = overlapFiles != null ? List.copyOf(overlapFiles) : List.of();
        deliverables = deliverables != null ? List.copyOf(deliverables) : List.of();
        surface = surface != null ? List.copyOf(surface) : List.of();
        dependencies = dependencies != null ? List.copyOf(dependencies) : List.of();
    }

    /**
     * @return the wait as a closed enum, or {@code null} where the row can start
     */
    public @Nullable EpicWaitReason waits() {
        return EpicWaitReason.fromValue(waitReason);
    }

    /**
     * @return {@code true} if the decomposition may not touch the row
     */
    public boolean isLocked() {
        return "running".equals(status) || "pausing".equals(status) || "paused".equals(status)
                || "shipped".equals(status) || "waiting for you".equals(status) || "held".equals(status);
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("plan_id", planId);
        map.put("title", title);
        map.put("status", status);
        if (waitReason != null) {
            map.put("wait_reason", waitReason);
        }
        if (waitPlanId != null) {
            map.put("wait_plan_id", waitPlanId);
        }
        if (!overlapFiles.isEmpty()) {
            map.put("overlap_files", overlapFiles);
        }
        if (pullRequestNumber != null) {
            map.put("pull_request_number", pullRequestNumber);
        }
        if (pullRequestUrl != null) {
            map.put("pull_request_url", pullRequestUrl);
        }
        if (change != null) {
            map.put("change", change);
        }
        if (objective != null) {
            map.put("objective", objective);
        }
        if (!deliverables.isEmpty()) {
            map.put("deliverables", deliverables);
        }
        if (!surface.isEmpty()) {
            map.put("surface", surface);
        }
        if (!dependencies.isEmpty()) {
            map.put("dependencies", dependencies);
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
     * @return the parsed row
     * @throws IOException if a required field is missing
     */
    public static WorkRow fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var planId = JsonTree.string(map, "plan_id");
        if (planId == null) {
            throw new JsonFormatException("Missing plan_id in WorkRow");
        }
        return new WorkRow(
                planId,
                JsonTree.string(map, "title"),
                JsonTree.string(map, "status"),
                JsonTree.string(map, "wait_reason"),
                JsonTree.string(map, "wait_plan_id"),
                ControlJson.readStrings(map, "overlap_files"),
                ControlJson.optInteger(map, "pull_request_number"),
                JsonTree.string(map, "pull_request_url"),
                JsonTree.string(map, "change"),
                JsonTree.string(map, "objective"),
                ControlJson.readStrings(map, "deliverables"),
                ControlJson.readStrings(map, "surface"),
                ControlJson.readStrings(map, "dependencies"));
    }

    /**
     * @param json the JSON object string
     * @return the parsed row
     * @throws IOException if the value is not an object
     */
    public static WorkRow fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for WorkRow");
        }
        return fromMap(obj);
    }
}
