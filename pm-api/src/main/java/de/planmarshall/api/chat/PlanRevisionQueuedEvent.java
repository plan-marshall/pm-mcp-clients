/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.chat;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.event.EventName;
import de.planmarshall.api.json.JsonTree;

/**
 * The payload of {@code plan.revision_queued} ({@code 06-event-catalogue.adoc}, Overview): the plan's
 * {@code queued_revision} as it is now, present or absent.
 * <p>
 * It is sent when a revision accepted in a chat is queued, when the chat that proposed it ends, and
 * when it leaves the queue, applied, refused or lapsed; in the last case {@code queued_revision} is
 * absent. It is the only event that carries {@code queued_revision}: {@code activity.changed} does not.
 *
 * @param planId          the plan's id
 * @param queuedRevision  the plan's queued revision, or {@code null} when none is queued
 */
public record PlanRevisionQueuedEvent(String planId, @Nullable QueuedRevision queuedRevision) {

    /** The event's name in the catalogue. */
    public static final String EVENT_NAME = EventName.PLAN_REVISION_QUEUED.wire();

    /** The wire name of the plan's id. */
    public static final String FIELD_PLAN_ID = "plan_id";
    /** The wire name of the queued revision. */
    public static final String FIELD_QUEUED_REVISION = "queued_revision";

    private static final String TYPE = "PlanRevisionQueuedEvent";
    private static final Set<String> KNOWN_FIELDS = Set.of(FIELD_PLAN_ID, FIELD_QUEUED_REVISION);

    /**
     * @param planId         the plan's id, not blank
     * @param queuedRevision the queued revision, or {@code null}
     */
    public PlanRevisionQueuedEvent {
        Objects.requireNonNull(planId, FIELD_PLAN_ID);
        if (planId.isBlank()) {
            throw new IllegalArgumentException("plan_id must not be blank");
        }
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_PLAN_ID, planId);
        if (queuedRevision != null) {
            map.put(FIELD_QUEUED_REVISION, queuedRevision.toMap());
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
     * @return the parsed payload
     * @throws IOException if a member is unknown, missing, or malformed
     */
    public static PlanRevisionQueuedEvent fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        ChatJson.requireOnlyKnown(map, KNOWN_FIELDS, TYPE);
        var planId = ChatJson.requireText(map, FIELD_PLAN_ID, TYPE);
        var queuedRevision = ChatJson.optionalRecord(map, FIELD_QUEUED_REVISION, QueuedRevision::fromMap, TYPE);
        return ChatJson.construct(TYPE, () -> new PlanRevisionQueuedEvent(planId, queuedRevision));
    }

    /**
     * @param json the JSON object string
     * @return the parsed payload
     * @throws IOException if the value is not an object or a member is malformed
     */
    public static PlanRevisionQueuedEvent fromJson(String json) throws IOException {
        return fromMap(ChatJson.parseObject(json, TYPE));
    }
}
