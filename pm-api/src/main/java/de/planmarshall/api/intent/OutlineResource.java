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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * A plan's outline record as both clients show it (Store Schemas, Outline Record; design
 * <em>Intent and Outline</em>): the deliverables with the files they affect and the criteria they
 * satisfy, and the added dependencies with their justification.
 *
 * @param planId            the plan's id
 * @param revision          the outline record's revision, from 1
 * @param summary           the outline's summary
 * @param deliverables      the deliverables, at least one
 * @param addedDependencies the dependencies the outline adds
 */
public record OutlineResource(
        String planId,
        int revision,
        String summary,
        List<DeliverableRecord> deliverables,
        List<DependencyJustification> addedDependencies
) {

    public static final String FIELD_PLAN_ID = "plan_id";
    public static final String FIELD_REVISION = "revision";
    public static final String FIELD_SUMMARY = "summary";
    public static final String FIELD_DELIVERABLES = "deliverables";
    public static final String FIELD_ADDED_DEPENDENCIES = "added_dependencies";

    private static final String TYPE = "OutlineResource";
    private static final Set<String> KNOWN_FIELDS = Set.of(
            FIELD_PLAN_ID, FIELD_REVISION, FIELD_SUMMARY, FIELD_DELIVERABLES, FIELD_ADDED_DEPENDENCIES);

    public OutlineResource {
        Objects.requireNonNull(planId, FIELD_PLAN_ID);
        Objects.requireNonNull(summary, FIELD_SUMMARY);
        if (planId.isBlank()) {
            throw new IllegalArgumentException("plan_id must not be blank");
        }
        if (revision < 1) {
            throw new IllegalArgumentException("revision must be at least 1");
        }
        if (summary.isBlank()) {
            throw new IllegalArgumentException("summary must not be blank");
        }
        deliverables = requireDeliverables(deliverables);
        var numbers = new HashSet<>(deliverables.stream().map(DeliverableRecord::number).toList());
        validateDependencies(deliverables, numbers);
        addedDependencies = requireDependencies(addedDependencies, numbers);
    }

    private static void validateDependencies(List<DeliverableRecord> deliverables, Set<Integer> numbers) {
        for (var deliverable : deliverables) {
            for (var dependency : deliverable.dependsOn()) {
                if (!numbers.contains(dependency) || dependency == deliverable.number()) {
                    throw new IllegalArgumentException("deliverable " + deliverable.number()
                            + " depends on no other deliverable of the outline: " + dependency);
                }
            }
        }
    }

    private static List<DeliverableRecord> requireDeliverables(List<DeliverableRecord> value) {
        var records = value == null ? List.<DeliverableRecord>of() : List.copyOf(value);
        if (records.isEmpty()) {
            throw new IllegalArgumentException("deliverables must not be empty");
        }
        var numbers = new HashSet<Integer>();
        for (var deliverable : records) {
            if (!numbers.add(deliverable.number())) {
                throw new IllegalArgumentException("duplicate deliverable number: " + deliverable.number());
            }
        }
        return records;
    }

    private static List<DependencyJustification> requireDependencies(List<DependencyJustification> value,
                                                                     Set<Integer> numbers) {
        var dependencies = value == null ? List.<DependencyJustification>of() : List.copyOf(value);
        for (var dependency : dependencies) {
            if (!numbers.contains(dependency.deliverable())) {
                throw new IllegalArgumentException(
                        "added dependency " + dependency.name() + " names no deliverable");
            }
        }
        return dependencies;
    }

    /**
     * @param number the deliverable's number
     * @return the deliverable, or {@code null}
     */
    public @Nullable DeliverableRecord deliverable(int number) {
        return deliverables.stream().filter(deliverable -> deliverable.number() == number).findFirst()
                .orElse(null);
    }

    /**
     * @param criterionId the criterion's id
     * @return the deliverables that satisfy the criterion
     */
    public List<DeliverableRecord> satisfying(String criterionId) {
        return deliverables.stream().filter(deliverable -> deliverable.satisfies(criterionId)).toList();
    }

    /**
     * @return every affected file of every deliverable
     */
    public List<DeliverableRecord.AffectedFile> affectedFiles() {
        return deliverables.stream().flatMap(deliverable -> deliverable.affectedFiles().stream()).toList();
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_PLAN_ID, planId);
        map.put(FIELD_REVISION, revision);
        map.put(FIELD_SUMMARY, summary);
        map.put(FIELD_DELIVERABLES, deliverables.stream().map(DeliverableRecord::toMap).toList());
        map.put(FIELD_ADDED_DEPENDENCIES,
                addedDependencies.stream().map(DependencyJustification::toMap).toList());
        return map;
    }

    /**
     * @return the compact JSON representation
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Writes this outline record to a streaming generator.
     *
     * @param generator the generator
     * @throws IOException on write failure
     */
    public void writeTo(JsonGenerator generator) throws IOException {
        generator.writeStartObject();
        generator.writeStringField(FIELD_PLAN_ID, planId);
        generator.writeNumberField(FIELD_REVISION, revision);
        generator.writeStringField(FIELD_SUMMARY, summary);
        IntentJson.writeArray(generator, FIELD_DELIVERABLES, deliverables, DeliverableRecord::writeTo);
        IntentJson.writeArray(generator, FIELD_ADDED_DEPENDENCIES, addedDependencies,
                DependencyJustification::writeTo);
        generator.writeEndObject();
    }

    /**
     * @param map the input map
     * @return the parsed outline record
     * @throws IOException if a required field is missing or a closed value is unknown
     */
    public static OutlineResource fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        IntentJson.requireOnlyKnown(map, KNOWN_FIELDS, TYPE);
        try {
            return new OutlineResource(
                    IntentJson.requireText(map, FIELD_PLAN_ID, TYPE),
                    IntentJson.requireInt(map, FIELD_REVISION, 1, Integer.MAX_VALUE, TYPE),
                    IntentJson.requireText(map, FIELD_SUMMARY, TYPE),
                    IntentJson.readEach(IntentJson.readObjects(map, FIELD_DELIVERABLES),
                            DeliverableRecord::fromMap),
                    IntentJson.readEach(IntentJson.readObjects(map, FIELD_ADDED_DEPENDENCIES),
                            DependencyJustification::fromMap));
        } catch (IllegalArgumentException e) {
            throw new JsonFormatException("Invalid " + TYPE + ": " + e.getMessage(), e);
        }
    }

    /**
     * @param json the JSON object string
     * @return the parsed outline record
     * @throws IOException if the value is not an object or a field is malformed
     */
    public static OutlineResource fromJson(String json) throws IOException {
        var object = JsonTree.asObject(JsonTree.parse(json));
        if (object == null) {
            throw new JsonFormatException("Expected JSON object for " + TYPE);
        }
        return fromMap(object);
    }

    /**
     * Reads one outline record from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return the parsed outline record
     * @throws IOException on format failure
     */
    public static OutlineResource readFrom(JsonParser parser) throws IOException {
        return fromMap(IntentJson.readObject(parser, TYPE));
    }
}
