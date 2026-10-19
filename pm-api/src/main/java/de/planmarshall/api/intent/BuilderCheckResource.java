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
 * The builder check of a plan's intent record: the questions a builder would still need answered,
 * each with who can answer it and whether it is resolved (Phase Workflows, Builder Check; design
 * <em>Intent and Outline</em>).
 * <p>
 * The check adds evidence and never holds a plan: a check that did not run says so with
 * {@link Resolution#NOT_RUN}, and the operator questions the relaunch could not take remain in the
 * outline review as open questions.
 *
 * @param resolution whether the check is pending, answered, or did not run
 * @param questions  the questions the builder asked
 */
public record BuilderCheckResource(Resolution resolution, List<BuilderQuestion> questions) {

    public static final String FIELD_RESOLUTION = "resolution";
    public static final String FIELD_QUESTIONS = "questions";

    private static final String TYPE = "BuilderCheckResource";
    private static final Set<String> KNOWN_FIELDS = Set.of(FIELD_RESOLUTION, FIELD_QUESTIONS);

    /** The closed list of a builder check's resolution states. */
    public enum Resolution {

        /** The check waits for the operator's answers. */
        PENDING("pending"),

        /** The check is resolved; open questions may remain and are shown with the outline. */
        ANSWERED("answered"),

        /** The check did not run, with its reason in the questions' absence and the plan's history. */
        NOT_RUN("not_run");

        private final String wireValue;

        Resolution(String wireValue) {
            this.wireValue = wireValue;
        }

        /**
         * @return the wire value used in representations
         */
        public String wireValue() {
            return wireValue;
        }

        /**
         * @param value the wire value
         * @return the matching resolution, or {@code null}
         */
        public static Resolution fromWireOrNull(String value) {
            if (value == null) {
                return null;
            }
            for (var resolution : values()) {
                if (resolution.wireValue.equalsIgnoreCase(value.trim())) {
                    return resolution;
                }
            }
            return null;
        }

        /**
         * @param value the wire value
         * @return the matching resolution
         * @throws IllegalArgumentException if no resolution carries that value (fail-closed)
         */
        public static Resolution fromWire(String value) {
            var resolution = fromWireOrNull(value);
            if (resolution == null) {
                throw new IllegalArgumentException("Unknown builder check resolution: " + value);
            }
            return resolution;
        }

        /**
         * @return the wire values of every resolution, in catalogue order
         */
        public static List<String> wireValues() {
            return Arrays.stream(values()).map(Resolution::wireValue).toList();
        }
    }

    public BuilderCheckResource {
        Objects.requireNonNull(resolution, FIELD_RESOLUTION);
        questions = questions == null ? List.of() : List.copyOf(questions);
    }

    /**
     * @return the questions that still wait for an answer
     */
    public List<BuilderQuestion> openQuestions() {
        return questions.stream().filter(BuilderQuestion::open).toList();
    }

    /**
     * @return the open questions only the operator can answer
     */
    public List<BuilderQuestion> operatorQuestions() {
        return openQuestions().stream().filter(BuilderQuestion::needsOperator).toList();
    }

    /**
     * @return the open questions the builder marked with a recommendation
     */
    public List<BuilderQuestion> recommendedQuestions() {
        return openQuestions().stream().filter(BuilderQuestion::recommended).toList();
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_RESOLUTION, resolution.wireValue());
        map.put(FIELD_QUESTIONS, questions.stream().map(BuilderQuestion::toMap).toList());
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
        generator.writeStringField(FIELD_RESOLUTION, resolution.wireValue());
        IntentJson.writeArray(generator, FIELD_QUESTIONS, questions, BuilderQuestion::writeTo);
        generator.writeEndObject();
    }

    /**
     * @param map the input map
     * @return the parsed check
     * @throws IOException if a required field is missing or a closed value is unknown
     */
    public static BuilderCheckResource fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        IntentJson.requireOnlyKnown(map, KNOWN_FIELDS, TYPE);
        final Resolution resolution;
        try {
            resolution = Resolution.fromWire(IntentJson.requireText(map, FIELD_RESOLUTION, TYPE));
        } catch (IllegalArgumentException e) {
            throw new JsonFormatException("Invalid " + TYPE + ": " + e.getMessage(), e);
        }
        var questions = IntentJson.readEach(IntentJson.readObjects(map, FIELD_QUESTIONS),
                BuilderQuestion::fromMap);
        return new BuilderCheckResource(resolution, questions);
    }

    /**
     * @param json the JSON object string
     * @return the parsed check
     * @throws IOException if the value is not an object or a field is malformed
     */
    public static BuilderCheckResource fromJson(String json) throws IOException {
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
    public static BuilderCheckResource readFrom(JsonParser parser) throws IOException {
        return fromMap(IntentJson.readObject(parser, TYPE));
    }
}
