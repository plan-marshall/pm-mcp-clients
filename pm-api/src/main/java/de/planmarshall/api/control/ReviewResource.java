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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * A review of an epic's findings or watches (design <em>Reviewing Findings or Watches</em>,
 * UC-CTL-5): its items with their dispositions, its progress, and the drafts of a promotion or an
 * edit.
 *
 * @param reviewId    the review's identifier
 * @param epicId      the epic whose register is reviewed
 * @param register    {@code findings} or {@code watches}
 * @param items       the items, the open ones first
 * @param decided     how many items are decided
 * @param total       how many items there are
 * @param state       the review's state ({@code open}, {@code item}, {@code draft-plan},
 *                    {@code read-back})
 * @param draftedPlan the plan drafted for a promoted item, or {@code null}
 * @param readBack    the read-back of an edit or a redraft, or {@code null}
 */
public record ReviewResource(
        String reviewId,
        String epicId,
        String register,
        List<ReviewItemResource> items,
        int decided,
        int total,
        String state,
        @Nullable DraftedPlan draftedPlan,
        @Nullable ReviewReadBackResource readBack
) {

    public ReviewResource {
        Objects.requireNonNull(reviewId, "reviewId");
        Objects.requireNonNull(epicId, "epicId");
        register = register != null && !register.isBlank() ? register : "findings";
        items = items != null ? List.copyOf(items) : List.of();
        state = state != null && !state.isBlank() ? state : "open";
    }

    /**
     * @return the items not decided yet
     */
    public List<ReviewItemResource> openItems() {
        return items.stream().filter(item -> item.disposition() == ReviewDisposition.OPEN).toList();
    }

    /**
     * @param itemId the item id
     * @return the item with that id, or {@code null}
     */
    public @Nullable ReviewItemResource item(String itemId) {
        for (var item : items) {
            if (item.itemId().equals(itemId)) {
                return item;
            }
        }
        return null;
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("review_id", reviewId);
        map.put("epic_id", epicId);
        map.put("register", register);
        map.put("items", items.stream().map(ReviewItemResource::toMap).toList());
        map.put("decided", decided);
        map.put("total", total);
        map.put("state", state);
        if (draftedPlan != null) {
            map.put("drafted_plan", draftedPlan.toMap());
        }
        if (readBack != null) {
            map.put("read_back", readBack.toMap());
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
     * @return the parsed review
     * @throws IOException if a required field is missing
     */
    public static ReviewResource fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var reviewId = JsonTree.string(map, "review_id");
        var epicId = JsonTree.string(map, "epic_id");
        if (reviewId == null || epicId == null) {
            throw new JsonFormatException("Missing required fields for ReviewResource: review_id, epic_id");
        }
        var decided = ControlJson.optInteger(map, "decided");
        var total = ControlJson.optInteger(map, "total");
        var drafted = JsonTree.asObject(map.get("drafted_plan"));
        var readBack = JsonTree.asObject(map.get("read_back"));
        return new ReviewResource(reviewId, epicId, JsonTree.string(map, "register"),
                readItems(map), decided != null ? decided : 0, total != null ? total : 0,
                JsonTree.string(map, "state"),
                drafted != null ? DraftedPlan.fromMap(drafted) : null,
                readBack != null ? ReviewReadBackResource.fromMap(readBack) : null);
    }

    /**
     * @param json the JSON object string
     * @return the parsed review
     * @throws IOException if the value is not an object
     */
    public static ReviewResource fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for ReviewResource");
        }
        return fromMap(obj);
    }

    private static List<ReviewItemResource> readItems(Map<String, Object> map) throws IOException {
        var items = new ArrayList<ReviewItemResource>();
        for (var object : ControlJson.readObjects(map, "items")) {
            items.add(ReviewItemResource.fromMap(object));
        }
        return items;
    }
}
