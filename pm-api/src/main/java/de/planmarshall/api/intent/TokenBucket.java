/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.intent;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * One bucket of the {@code over_size} distribution: the tasks whose spent tokens fall between two
 * multiples of their predicted size, and how many of them the proposed multiple would have flagged
 * (Model Work, A Worker Off Track; design <em>Intent and Outline</em>).
 * <p>
 * The bounds are multiples of each task's own predicted cost, never absolute token counts, because a
 * task's prediction is the reference of the {@code over_size} signal. A bucket whose tasks are all
 * beyond the multiple carries all of them as flagged; a bucket straddling it carries only the share
 * the runtime computed.
 *
 * @param label        the bucket's display label
 * @param fromMultiple the inclusive lower bound, a multiple of the task's predicted cost
 * @param toMultiple   the exclusive upper bound, a multiple of the task's predicted cost
 * @param taskCount    the tasks in the bucket
 * @param flaggedTasks the tasks the proposed multiple would have flagged
 */
public record TokenBucket(
        String label,
        double fromMultiple,
        double toMultiple,
        int taskCount,
        int flaggedTasks
) {

    public static final String FIELD_LABEL = "label";
    public static final String FIELD_FROM_MULTIPLE = "from_multiple";
    public static final String FIELD_TO_MULTIPLE = "to_multiple";
    public static final String FIELD_TASK_COUNT = "task_count";
    public static final String FIELD_FLAGGED_TASKS = "flagged_tasks";

    private static final String TYPE = "TokenBucket";
    private static final Set<String> KNOWN_FIELDS = Set.of(
            FIELD_LABEL, FIELD_FROM_MULTIPLE, FIELD_TO_MULTIPLE, FIELD_TASK_COUNT, FIELD_FLAGGED_TASKS);

    public TokenBucket {
        Objects.requireNonNull(label, FIELD_LABEL);
        if (label.isBlank()) {
            throw new IllegalArgumentException("label must not be blank");
        }
        if (!Double.isFinite(fromMultiple) || !Double.isFinite(toMultiple)) {
            throw new IllegalArgumentException("the bounds must be finite");
        }
        if (fromMultiple < 0 || toMultiple <= fromMultiple) {
            throw new IllegalArgumentException("the bounds must satisfy 0 <= from < to");
        }
        if (taskCount < 0 || flaggedTasks < 0 || flaggedTasks > taskCount) {
            throw new IllegalArgumentException("flagged_tasks must lie between 0 and task_count");
        }
    }

    /**
     * @return {@code true} if the bucket holds a flagged task
     */
    public boolean flagged() {
        return flaggedTasks > 0;
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_LABEL, label);
        map.put(FIELD_FROM_MULTIPLE, fromMultiple);
        map.put(FIELD_TO_MULTIPLE, toMultiple);
        map.put(FIELD_TASK_COUNT, taskCount);
        map.put(FIELD_FLAGGED_TASKS, flaggedTasks);
        return map;
    }

    /**
     * @return the compact JSON representation
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Writes this bucket to a streaming generator.
     *
     * @param generator the generator
     * @throws IOException on write failure
     */
    public void writeTo(JsonGenerator generator) throws IOException {
        generator.writeStartObject();
        generator.writeStringField(FIELD_LABEL, label);
        generator.writeNumberField(FIELD_FROM_MULTIPLE, fromMultiple);
        generator.writeNumberField(FIELD_TO_MULTIPLE, toMultiple);
        generator.writeNumberField(FIELD_TASK_COUNT, taskCount);
        generator.writeNumberField(FIELD_FLAGGED_TASKS, flaggedTasks);
        generator.writeEndObject();
    }

    /**
     * @param map the input map
     * @return the parsed bucket
     * @throws IOException if a required field is missing or out of range
     */
    public static TokenBucket fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        IntentJson.requireOnlyKnown(map, KNOWN_FIELDS, TYPE);
        try {
            return new TokenBucket(
                    IntentJson.requireText(map, FIELD_LABEL, TYPE),
                    IntentJson.requireDouble(map, FIELD_FROM_MULTIPLE, TYPE),
                    IntentJson.requireDouble(map, FIELD_TO_MULTIPLE, TYPE),
                    IntentJson.requireInt(map, FIELD_TASK_COUNT, 0, Integer.MAX_VALUE, TYPE),
                    IntentJson.requireInt(map, FIELD_FLAGGED_TASKS, 0, Integer.MAX_VALUE, TYPE));
        } catch (IllegalArgumentException e) {
            throw new JsonFormatException("Invalid " + TYPE + ": " + e.getMessage(), e);
        }
    }

    /**
     * @param json the JSON object string
     * @return the parsed bucket
     * @throws IOException if the value is not an object or a field is malformed
     */
    public static TokenBucket fromJson(String json) throws IOException {
        var object = JsonTree.asObject(JsonTree.parse(json));
        if (object == null) {
            throw new JsonFormatException("Expected JSON object for " + TYPE);
        }
        return fromMap(object);
    }

    /**
     * Reads one bucket from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return the parsed bucket
     * @throws IOException on format failure
     */
    public static TokenBucket readFrom(JsonParser parser) throws IOException {
        return fromMap(IntentJson.readObject(parser, TYPE));
    }
}
