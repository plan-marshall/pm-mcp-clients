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
import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * One question of a plan's builder check (Phase Workflows, Builder Check; design <em>Intent and
 * Outline</em>).
 * <p>
 * The builder check reads the intent record as someone who must build from it and cannot ask. Each
 * question is sorted: {@link Sort#ANSWERED} names the record item that answers it, {@link Sort#MINOR}
 * is the builder's own call, and {@link Sort#OPERATOR} is a question only the operator can answer,
 * where the builder's recommendation is marked. Its resolution state says whether the operator
 * answered it, left it deferred, or it is still open.
 *
 * @param id          the question's id ({@code BQ-n})
 * @param text        the question
 * @param sort        who can answer it
 * @param recommended {@code true} if the builder marked a recommendation
 * @param state       whether the question is open, answered, or deferred
 * @param answer      the operator's answer, required once {@link State#ANSWERED}
 */
public record BuilderQuestion(
        String id,
        String text,
        Sort sort,
        boolean recommended,
        State state,
        @Nullable String answer
) {

    public static final String FIELD_ID = "id";
    public static final String FIELD_TEXT = "text";
    public static final String FIELD_SORT = "sort";
    public static final String FIELD_RECOMMENDED = "recommended";
    public static final String FIELD_STATE = "state";
    public static final String FIELD_ANSWER = "answer";

    private static final String TYPE = "BuilderQuestion";
    private static final Set<String> KNOWN_FIELDS = Set.of(
            FIELD_ID, FIELD_TEXT, FIELD_SORT, FIELD_RECOMMENDED, FIELD_STATE, FIELD_ANSWER);

    /** The closed list of who can answer a builder question. */
    public enum Sort {

        /** Only the operator can answer it; the builder may recommend an answer. */
        OPERATOR("operator"),

        /** The builder's own call, marked as a decision of the model. */
        MINOR("minor"),

        /** The record answers it, naming the item. */
        ANSWERED("answered");

        private final String wireValue;

        Sort(String wireValue) {
            this.wireValue = wireValue;
        }

        /**
         * @return the wire value used in representations
         */
        public String wireValue() {
            return wireValue;
        }

        /**
         * @return {@code true} if only the operator can answer the question
         */
        public boolean needsOperator() {
            return this == OPERATOR;
        }

        /**
         * @param value the wire value
         * @return the matching sort, or {@code null}
         */
        public static Sort fromWireOrNull(String value) {
            if (value == null) {
                return null;
            }
            for (var sort : values()) {
                if (sort.wireValue.equalsIgnoreCase(value.trim())) {
                    return sort;
                }
            }
            return null;
        }

        /**
         * @param value the wire value
         * @return the matching sort
         * @throws IllegalArgumentException if no sort carries that value (fail-closed)
         */
        public static Sort fromWire(String value) {
            var sort = fromWireOrNull(value);
            if (sort == null) {
                throw new IllegalArgumentException("Unknown builder question sort: " + value);
            }
            return sort;
        }

        /**
         * @return the wire values of every sort, in catalogue order
         */
        public static List<String> wireValues() {
            return Arrays.stream(values()).map(Sort::wireValue).toList();
        }
    }

    /** The closed list of a builder question's resolution states. */
    public enum State {

        /** The question still waits for an answer. */
        OPEN("open"),

        /** The question is answered. */
        ANSWERED("answered"),

        /** The question is deferred; the plan continues without it. */
        DEFERRED("deferred");

        private final String wireValue;

        State(String wireValue) {
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
         * @return the matching state, or {@code null}
         */
        public static State fromWireOrNull(String value) {
            if (value == null) {
                return null;
            }
            for (var state : values()) {
                if (state.wireValue.equalsIgnoreCase(value.trim())) {
                    return state;
                }
            }
            return null;
        }

        /**
         * @param value the wire value
         * @return the matching state
         * @throws IllegalArgumentException if no state carries that value (fail-closed)
         */
        public static State fromWire(String value) {
            var state = fromWireOrNull(value);
            if (state == null) {
                throw new IllegalArgumentException("Unknown builder question state: " + value);
            }
            return state;
        }

        /**
         * @return the wire values of every state, in catalogue order
         */
        public static List<String> wireValues() {
            return Arrays.stream(values()).map(State::wireValue).toList();
        }
    }

    public BuilderQuestion {
        Objects.requireNonNull(id, FIELD_ID);
        Objects.requireNonNull(text, FIELD_TEXT);
        Objects.requireNonNull(sort, FIELD_SORT);
        Objects.requireNonNull(state, FIELD_STATE);
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (text.isBlank()) {
            throw new IllegalArgumentException("text must not be blank");
        }
        if (state == State.ANSWERED && (answer == null || answer.isBlank())) {
            throw new IllegalArgumentException("an answered question requires its answer");
        }
        if (state != State.ANSWERED && answer != null) {
            throw new IllegalArgumentException("only an answered question carries an answer");
        }
    }

    /**
     * @return {@code true} if the question is still open
     */
    public boolean open() {
        return state == State.OPEN;
    }

    /**
     * @return {@code true} if only the operator can answer the question
     */
    public boolean needsOperator() {
        return sort.needsOperator();
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_ID, id);
        map.put(FIELD_TEXT, text);
        map.put(FIELD_SORT, sort.wireValue());
        map.put(FIELD_RECOMMENDED, recommended);
        map.put(FIELD_STATE, state.wireValue());
        if (answer != null) {
            map.put(FIELD_ANSWER, answer);
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
     * Writes this question to a streaming generator.
     *
     * @param generator the generator
     * @throws IOException on write failure
     */
    public void writeTo(JsonGenerator generator) throws IOException {
        generator.writeStartObject();
        generator.writeStringField(FIELD_ID, id);
        generator.writeStringField(FIELD_TEXT, text);
        generator.writeStringField(FIELD_SORT, sort.wireValue());
        generator.writeBooleanField(FIELD_RECOMMENDED, recommended);
        generator.writeStringField(FIELD_STATE, state.wireValue());
        IntentJson.writeOptionalString(generator, FIELD_ANSWER, answer);
        generator.writeEndObject();
    }

    /**
     * @param map the input map
     * @return the parsed question
     * @throws IOException if a required field is missing or a closed value is unknown
     */
    public static BuilderQuestion fromMap(Map<String, Object> object) throws IOException {
        Objects.requireNonNull(object, "map");
        IntentJson.requireOnlyKnown(object, KNOWN_FIELDS, TYPE);
        try {
            return new BuilderQuestion(
                    IntentJson.requireText(object, FIELD_ID, TYPE),
                    IntentJson.requireText(object, FIELD_TEXT, TYPE),
                    Sort.fromWire(IntentJson.requireText(object, FIELD_SORT, TYPE)),
                    IntentJson.requireBoolean(object, FIELD_RECOMMENDED, TYPE),
                    State.fromWire(IntentJson.requireText(object, FIELD_STATE, TYPE)),
                    IntentJson.optionalText(object, FIELD_ANSWER, TYPE));
        } catch (IllegalArgumentException e) {
            throw new JsonFormatException("Invalid " + TYPE + ": " + e.getMessage(), e);
        }
    }

    /**
     * @param json the JSON object string
     * @return the parsed question
     * @throws IOException if the value is not an object or a field is malformed
     */
    public static BuilderQuestion fromJson(String json) throws IOException {
        var object = JsonTree.asObject(JsonTree.parse(json));
        if (object == null) {
            throw new JsonFormatException("Expected JSON object for " + TYPE);
        }
        return fromMap(object);
    }

    /**
     * Reads one question from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return the parsed question
     * @throws IOException on format failure
     */
    public static BuilderQuestion readFrom(JsonParser parser) throws IOException {
        return fromMap(IntentJson.readObject(parser, TYPE));
    }
}
