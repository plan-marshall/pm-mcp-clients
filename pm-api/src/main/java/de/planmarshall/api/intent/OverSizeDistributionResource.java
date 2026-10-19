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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * The distribution of a role's plan-task tokens against their predicted size, and the share the
 * proposed {@code over_size} multiple would have flagged (Model Work, A Worker Off Track; design
 * <em>Intent and Outline</em>).
 * <p>
 * The operator changes a role's {@code over_size} multiple and needs to see what that would have
 * done: the buckets are the tasks' spent tokens as a multiple of each task's own prediction, and
 * {@link #flaggedShare()} is the share of past tasks the proposed multiple would have flagged. A
 * client never recomputes a bucket; it shows the runtime's distribution and flags only the buckets
 * that carry a change.
 *
 * @param projectId       the project whose role is measured
 * @param role            the role whose multiple is changed
 * @param currentMultiple the multiple in force
 * @param proposedMultiple the multiple the operator considers
 * @param buckets         the distribution's buckets, in ascending order and not overlapping
 */
public record OverSizeDistributionResource(
        String projectId,
        String role,
        double currentMultiple,
        double proposedMultiple,
        List<TokenBucket> buckets
) {

    public static final String FIELD_PROJECT_ID = "project_id";
    public static final String FIELD_ROLE = "role";
    public static final String FIELD_CURRENT_MULTIPLE = "current_multiple";
    public static final String FIELD_PROPOSED_MULTIPLE = "proposed_multiple";
    public static final String FIELD_BUCKETS = "buckets";

    private static final String TYPE = "OverSizeDistributionResource";
    private static final Set<String> KNOWN_FIELDS = Set.of(
            FIELD_PROJECT_ID, FIELD_ROLE, FIELD_CURRENT_MULTIPLE, FIELD_PROPOSED_MULTIPLE, FIELD_BUCKETS);

    public OverSizeDistributionResource {
        Objects.requireNonNull(projectId, FIELD_PROJECT_ID);
        Objects.requireNonNull(role, FIELD_ROLE);
        if (projectId.isBlank()) {
            throw new IllegalArgumentException("project_id must not be blank");
        }
        if (role.isBlank()) {
            throw new IllegalArgumentException("role must not be blank");
        }
        requirePositive(currentMultiple, FIELD_CURRENT_MULTIPLE);
        requirePositive(proposedMultiple, FIELD_PROPOSED_MULTIPLE);
        buckets = buckets == null ? List.of() : List.copyOf(buckets);
        if (buckets.isEmpty()) {
            throw new IllegalArgumentException("buckets must not be empty");
        }
        for (var i = 1; i < buckets.size(); i++) {
            if (buckets.get(i).fromMultiple() < buckets.get(i - 1).toMultiple()) {
                throw new IllegalArgumentException("buckets must be ordered and must not overlap");
            }
        }
    }

    private static void requirePositive(double value, String name) {
        if (!Double.isFinite(value) || value <= 0) {
            throw new IllegalArgumentException(name + " must be a positive finite number");
        }
    }

    /**
     * @return the number of tasks in the distribution
     */
    public int totalTasks() {
        return buckets.stream().mapToInt(TokenBucket::taskCount).sum();
    }

    /**
     * @return the number of tasks the proposed multiple would have flagged
     */
    public int flaggedTasks() {
        return buckets.stream().mapToInt(TokenBucket::flaggedTasks).sum();
    }

    /**
     * @return the share of tasks the proposed multiple would have flagged, from 0 to 1
     */
    public double flaggedShare() {
        var total = totalTasks();
        return total == 0 ? 0.0 : (double) flaggedTasks() / total;
    }

    /**
     * @return the buckets that carry a flagged task
     */
    public List<TokenBucket> flaggedBuckets() {
        return buckets.stream().filter(TokenBucket::flagged).toList();
    }

    /**
     * @return {@code true} if the proposal would flag more tasks than the current multiple
     */
    public boolean flagsMore() {
        return proposedMultiple < currentMultiple;
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_PROJECT_ID, projectId);
        map.put(FIELD_ROLE, role);
        map.put(FIELD_CURRENT_MULTIPLE, currentMultiple);
        map.put(FIELD_PROPOSED_MULTIPLE, proposedMultiple);
        map.put(FIELD_BUCKETS, buckets.stream().map(TokenBucket::toMap).toList());
        return map;
    }

    /**
     * @return the compact JSON representation
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Writes this distribution to a streaming generator.
     *
     * @param generator the generator
     * @throws IOException on write failure
     */
    public void writeTo(JsonGenerator generator) throws IOException {
        generator.writeStartObject();
        generator.writeStringField(FIELD_PROJECT_ID, projectId);
        generator.writeStringField(FIELD_ROLE, role);
        generator.writeNumberField(FIELD_CURRENT_MULTIPLE, currentMultiple);
        generator.writeNumberField(FIELD_PROPOSED_MULTIPLE, proposedMultiple);
        IntentJson.writeArray(generator, FIELD_BUCKETS, buckets, TokenBucket::writeTo);
        generator.writeEndObject();
    }

    /**
     * @param map the input map
     * @return the parsed distribution
     * @throws IOException if a required field is missing or a closed value is unknown
     */
    public static OverSizeDistributionResource fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        IntentJson.requireOnlyKnown(map, KNOWN_FIELDS, TYPE);
        try {
            return new OverSizeDistributionResource(
                    IntentJson.requireText(map, FIELD_PROJECT_ID, TYPE),
                    IntentJson.requireText(map, FIELD_ROLE, TYPE),
                    IntentJson.requireDouble(map, FIELD_CURRENT_MULTIPLE, TYPE),
                    IntentJson.requireDouble(map, FIELD_PROPOSED_MULTIPLE, TYPE),
                    IntentJson.readEach(IntentJson.readObjects(map, FIELD_BUCKETS), TokenBucket::fromMap));
        } catch (IllegalArgumentException e) {
            throw new JsonFormatException("Invalid " + TYPE + ": " + e.getMessage(), e);
        }
    }

    /**
     * @param json the JSON object string
     * @return the parsed distribution
     * @throws IOException if the value is not an object or a field is malformed
     */
    public static OverSizeDistributionResource fromJson(String json) throws IOException {
        var object = JsonTree.asObject(JsonTree.parse(json));
        if (object == null) {
            throw new JsonFormatException("Expected JSON object for " + TYPE);
        }
        return fromMap(object);
    }

    /**
     * Reads one distribution from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return the parsed distribution
     * @throws IOException on format failure
     */
    public static OverSizeDistributionResource readFrom(JsonParser parser) throws IOException {
        return fromMap(IntentJson.readObject(parser, TYPE));
    }
}
