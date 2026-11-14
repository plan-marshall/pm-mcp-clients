/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.changes;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.overview.ChangeSize;

/**
 * The payload of {@code plan.change_captured}: a plan's change was captured, with the new
 * {@code head} and the size the plan now carries
 * ({@code 06-event-catalogue.adoc}, {@code 01-processes-and-transport.adoc#plan-changes}).
 * <p>
 * A client that sees it reads the plan's list again; the payload names the figures of the size so
 * an overview can move without the read.
 *
 * @param planId          the plan whose change was captured
 * @param head            the commit the capture measured
 * @param filesTotal      the number of changed files
 * @param insertionsTotal the number of inserted lines
 * @param deletionsTotal  the number of deleted lines
 * @param unplannedTotal  the number of changed files beyond the plan's declared footprint
 */
public record PlanChangeCapturedEvent(
        String planId,
        String head,
        int filesTotal,
        int insertionsTotal,
        int deletionsTotal,
        int unplannedTotal
) {

    /** The name of the event this payload belongs to. */
    public static final String EVENT_NAME = "plan.change_captured";

    private static final String FIELD_PLAN_ID = "plan_id";
    private static final String FIELD_HEAD = "head";
    private static final String FIELD_FILES_TOTAL = "files_total";
    private static final String FIELD_INSERTIONS_TOTAL = "insertions_total";
    private static final String FIELD_DELETIONS_TOTAL = "deletions_total";
    private static final String FIELD_UNPLANNED_TOTAL = "unplanned_total";

    public PlanChangeCapturedEvent {
        Objects.requireNonNull(planId, "planId");
        Objects.requireNonNull(head, "head");
        if (filesTotal < 0 || insertionsTotal < 0 || deletionsTotal < 0 || unplannedTotal < 0) {
            throw new IllegalArgumentException("A size of change is never negative");
        }
    }

    /**
     * @return the size the plan now carries, the same four figures the list adds up to
     */
    public ChangeSize size() {
        return new ChangeSize(filesTotal, insertionsTotal, deletionsTotal, unplannedTotal);
    }

    /**
     * @return the map representation, in the order of the catalogue's payload
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_PLAN_ID, planId);
        map.put(FIELD_HEAD, head);
        map.put(FIELD_FILES_TOTAL, filesTotal);
        map.put(FIELD_INSERTIONS_TOTAL, insertionsTotal);
        map.put(FIELD_DELETIONS_TOTAL, deletionsTotal);
        map.put(FIELD_UNPLANNED_TOTAL, unplannedTotal);
        return map;
    }

    /**
     * @return the JSON representation
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * @param map the payload, without the envelope's own members
     * @return the parsed payload
     * @throws IOException if a required member is missing
     */
    public static PlanChangeCapturedEvent fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var planId = JsonTree.string(map, FIELD_PLAN_ID);
        var head = JsonTree.string(map, FIELD_HEAD);
        if (planId == null || head == null) {
            throw new JsonFormatException("A captured change names its plan and head");
        }
        return new PlanChangeCapturedEvent(
                planId,
                head,
                requireFigure(map, FIELD_FILES_TOTAL),
                requireFigure(map, FIELD_INSERTIONS_TOTAL),
                requireFigure(map, FIELD_DELETIONS_TOTAL),
                requireFigure(map, FIELD_UNPLANNED_TOTAL));
    }

    /**
     * @param json the JSON object
     * @return the parsed payload
     * @throws IOException on parse failure or a missing member
     */
    public static PlanChangeCapturedEvent fromJson(String json) throws IOException {
        var map = JsonTree.asObject(JsonTree.parse(json));
        if (map == null) {
            throw new JsonFormatException("Expected a JSON object for " + EVENT_NAME);
        }
        return fromMap(map);
    }

    private static int requireFigure(Map<String, Object> map, String field) throws IOException {
        var value = JsonTree.integer(map, field);
        if (value == null) {
            throw new JsonFormatException("Missing " + field + " in " + EVENT_NAME);
        }
        return value;
    }
}
