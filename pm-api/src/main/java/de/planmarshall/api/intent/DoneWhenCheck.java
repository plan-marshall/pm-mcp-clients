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
import java.util.Arrays;
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
 * One done-when check of an epic's intent, with its verdict and the evidence the completion review
 * gives (Model Work, Epic Intent; design <em>Intent and Outline</em>).
 * <p>
 * Before the completion review the check is {@link Verdict#PENDING}; afterwards it is met or not met,
 * and either verdict carries the evidence the operator can inspect. A check is never judged from a
 * plan row's status.
 *
 * @param text     the check, which the operator can run on the result
 * @param verdict  the completion review's verdict
 * @param evidence the evidence, at least one entry once the check is decided
 */
public record DoneWhenCheck(String text, Verdict verdict, List<String> evidence) {

    public static final String FIELD_TEXT = "text";
    public static final String FIELD_VERDICT = "verdict";
    public static final String FIELD_EVIDENCE = "evidence";

    private static final String TYPE = "DoneWhenCheck";
    private static final Set<String> KNOWN_FIELDS = Set.of(FIELD_TEXT, FIELD_VERDICT, FIELD_EVIDENCE);

    /** The closed list of a done-when check's verdicts. */
    public enum Verdict {

        /** The completion review has not run yet. */
        PENDING("pending"),

        /** The check is met, with its evidence. */
        MET("met"),

        /** The check is not met, with its evidence. */
        NOT_MET("not_met");

        private final String wireValue;

        Verdict(String wireValue) {
            this.wireValue = wireValue;
        }

        /**
         * @return the wire value used in representations
         */
        public String wireValue() {
            return wireValue;
        }

        /**
         * @return {@code true} if the completion review decided the check
         */
        public boolean decided() {
            return this != PENDING;
        }

        /**
         * @param value the wire value
         * @return the matching verdict, or {@code null}
         */
        public static Verdict fromWireOrNull(String value) {
            if (value == null) {
                return null;
            }
            for (var verdict : values()) {
                if (verdict.wireValue.equalsIgnoreCase(value.trim())) {
                    return verdict;
                }
            }
            return null;
        }

        /**
         * @param value the wire value
         * @return the matching verdict
         * @throws IllegalArgumentException if no verdict carries that value (fail-closed)
         */
        public static Verdict fromWire(String value) {
            var verdict = fromWireOrNull(value);
            if (verdict == null) {
                throw new IllegalArgumentException("Unknown done-when verdict: " + value);
            }
            return verdict;
        }

        /**
         * @return the wire values of every verdict, in catalogue order
         */
        public static List<String> wireValues() {
            return Arrays.stream(values()).map(Verdict::wireValue).toList();
        }
    }

    public DoneWhenCheck {
        Objects.requireNonNull(text, FIELD_TEXT);
        Objects.requireNonNull(verdict, FIELD_VERDICT);
        if (text.isBlank()) {
            throw new IllegalArgumentException("text must not be blank");
        }
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
        if (evidence.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new IllegalArgumentException("evidence must not carry a blank entry");
        }
        if (verdict.decided() && evidence.isEmpty()) {
            throw new IllegalArgumentException("a decided check requires its evidence");
        }
        if (!verdict.decided() && !evidence.isEmpty()) {
            throw new IllegalArgumentException("a pending check carries no evidence");
        }
    }

    /**
     * @return {@code true} if the check is met
     */
    public boolean met() {
        return verdict == Verdict.MET;
    }

    /**
     * @return {@code true} if the completion review decided the check
     */
    public boolean decided() {
        return verdict.decided();
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_TEXT, text);
        map.put(FIELD_VERDICT, verdict.wireValue());
        map.put(FIELD_EVIDENCE, evidence);
        return map;
    }

    /**
     * @return the compact JSON representation
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Writes this check to a streaming generator.
     *
     * @param generator the generator
     * @throws IOException on write failure
     */
    public void writeTo(JsonGenerator generator) throws IOException {
        generator.writeStartObject();
        generator.writeStringField(FIELD_TEXT, text);
        generator.writeStringField(FIELD_VERDICT, verdict.wireValue());
        IntentJson.writeStrings(generator, FIELD_EVIDENCE, evidence);
        generator.writeEndObject();
    }

    /**
     * @param map the input map
     * @return the parsed check
     * @throws IOException if a required field is missing or a closed value is unknown
     */
    public static DoneWhenCheck fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        IntentJson.requireOnlyKnown(map, KNOWN_FIELDS, TYPE);
        try {
            return new DoneWhenCheck(
                    IntentJson.requireText(map, FIELD_TEXT, TYPE),
                    Verdict.fromWire(IntentJson.requireText(map, FIELD_VERDICT, TYPE)),
                    IntentJson.readStrings(map, FIELD_EVIDENCE));
        } catch (IllegalArgumentException e) {
            throw new JsonFormatException("Invalid " + TYPE + ": " + e.getMessage(), e);
        }
    }

    /**
     * @param json the JSON object string
     * @return the parsed check
     * @throws IOException if the value is not an object or a field is malformed
     */
    public static DoneWhenCheck fromJson(String json) throws IOException {
        var object = JsonTree.asObject(JsonTree.parse(json));
        if (object == null) {
            throw new JsonFormatException("Expected JSON object for " + TYPE);
        }
        return fromMap(object);
    }

    /**
     * Reads one check from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return the parsed check
     * @throws IOException on format failure
     */
    public static DoneWhenCheck readFrom(JsonParser parser) throws IOException {
        return fromMap(IntentJson.readObject(parser, TYPE));
    }
}
