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
import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * One EARS-form criterion of a plan's intent record, with the deliverables that satisfy it and, after
 * the self-review, its verdict and the reviewer's reason (Phase Workflows, The Intent Record; design
 * <em>Intent and Outline</em>).
 * <p>
 * A criterion is shown before the self-review without a {@code status}; the four fields the clients
 * rely on are the id ({@code AC-n}), the criterion in EARS form, the deliverable numbers that satisfy
 * it, and the verdict once it exists. A verdict that is not {@link CriterionStatus#MET} carries its
 * reason, so a client never shows an unexplained miss.
 *
 * @param id           the criterion's id ({@code AC-1} to {@code AC-9999})
 * @param text         the criterion in EARS form
 * @param satisfies    the numbers of the outline deliverables that satisfy it
 * @param status       the self-review's verdict, or {@code null} before the review
 * @param statusReason the reviewer's reason, required for a non-met verdict
 */
public record IntentCriterion(
        String id,
        String text,
        List<Integer> satisfies,
        @Nullable CriterionStatus status,
        @Nullable String statusReason
) {

    public static final String FIELD_ID = "id";
    public static final String FIELD_TEXT = "text";
    public static final String FIELD_SATISFIES = "satisfies";
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_STATUS_REASON = "status_reason";

    private static final String TYPE = "IntentCriterion";
    private static final Set<String> KNOWN_FIELDS = Set.of(
            FIELD_ID, FIELD_TEXT, FIELD_SATISFIES, FIELD_STATUS, FIELD_STATUS_REASON);

    public IntentCriterion {
        Objects.requireNonNull(id, FIELD_ID);
        Objects.requireNonNull(text, FIELD_TEXT);
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (text.isBlank()) {
            throw new IllegalArgumentException("text must not be blank");
        }
        satisfies = satisfies == null ? List.of() : List.copyOf(satisfies);
        if (satisfies.stream().anyMatch(number -> number == null || number < 1)) {
            throw new IllegalArgumentException("satisfies must name deliverable numbers of at least 1");
        }
        if (status != null && status.requiresReason() && (statusReason == null || statusReason.isBlank())) {
            throw new IllegalArgumentException("status " + status.wireValue() + " requires its reason");
        }
        if (status == null && statusReason != null) {
            throw new IllegalArgumentException("a reason without a status is refused");
        }
    }

    /**
     * @return {@code true} if the self-review judged the criterion met
     */
    public boolean met() {
        return status == CriterionStatus.MET;
    }

    /**
     * @return {@code true} if the self-review judged the criterion not met
     */
    public boolean notMet() {
        return status == CriterionStatus.NOT_MET;
    }

    /**
     * @return {@code true} if the plan could not verify the criterion
     */
    public boolean notVerified() {
        return status == CriterionStatus.NOT_VERIFIED;
    }

    /**
     * @return {@code true} if the self-review has run for this criterion
     */
    public boolean evaluated() {
        return status != null;
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_ID, id);
        map.put(FIELD_TEXT, text);
        map.put(FIELD_SATISFIES, satisfies);
        if (status != null) {
            map.put(FIELD_STATUS, status.wireValue());
        }
        if (statusReason != null) {
            map.put(FIELD_STATUS_REASON, statusReason);
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
     * Writes this criterion to a streaming generator.
     *
     * @param generator the generator
     * @throws IOException on write failure
     */
    public void writeTo(JsonGenerator generator) throws IOException {
        generator.writeStartObject();
        generator.writeStringField(FIELD_ID, id);
        generator.writeStringField(FIELD_TEXT, text);
        generator.writeArrayFieldStart(FIELD_SATISFIES);
        for (var number : satisfies) {
            generator.writeNumber(number);
        }
        generator.writeEndArray();
        IntentJson.writeOptionalString(generator, FIELD_STATUS, status != null ? status.wireValue() : null);
        IntentJson.writeOptionalString(generator, FIELD_STATUS_REASON, statusReason);
        generator.writeEndObject();
    }

    /**
     * @param map the input map
     * @return the parsed criterion
     * @throws IOException if a required field is missing or a closed value is unknown
     */
    public static IntentCriterion fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        IntentJson.requireOnlyKnown(map, KNOWN_FIELDS, TYPE);
        var statusText = IntentJson.optionalText(map, FIELD_STATUS, TYPE);
        final CriterionStatus status;
        try {
            status = statusText != null ? CriterionStatus.fromWire(statusText) : null;
        } catch (IllegalArgumentException e) {
            throw new JsonFormatException("Invalid " + TYPE + ": " + e.getMessage(), e);
        }
        try {
            return new IntentCriterion(
                    IntentJson.requireText(map, FIELD_ID, TYPE),
                    IntentJson.requireText(map, FIELD_TEXT, TYPE),
                    IntentJson.readInts(map, FIELD_SATISFIES, 1, 99),
                    status,
                    IntentJson.optionalText(map, FIELD_STATUS_REASON, TYPE));
        } catch (IllegalArgumentException e) {
            throw new JsonFormatException("Invalid " + TYPE + ": " + e.getMessage(), e);
        }
    }

    /**
     * @param json the JSON object string
     * @return the parsed criterion
     * @throws IOException if the value is not an object or a field is malformed
     */
    public static IntentCriterion fromJson(String json) throws IOException {
        var object = JsonTree.asObject(JsonTree.parse(json));
        if (object == null) {
            throw new JsonFormatException("Expected JSON object for " + TYPE);
        }
        return fromMap(object);
    }

    /**
     * Reads one criterion from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return the parsed criterion
     * @throws IOException on format failure
     */
    public static IntentCriterion readFrom(JsonParser parser) throws IOException {
        return fromMap(IntentJson.readObject(parser, TYPE));
    }
}
