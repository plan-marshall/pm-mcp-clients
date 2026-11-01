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
 * One deliverable of a plan's outline record (Store Schemas, Outline Record; design <em>Intent and
 * Outline</em>): what it does, the files it affects with the intent of each, the deliverables it
 * depends on, the criteria it satisfies, and how it is verified.
 *
 * @param number       the deliverable's number, from 1
 * @param title        the deliverable's title
 * @param intentGloss  one sentence of what the deliverable does
 * @param affectedFiles the files the deliverable touches, each with its intent
 * @param dependsOn    the numbers of the deliverables it depends on
 * @param satisfies    the ids of the intent criteria it satisfies
 * @param verification how the deliverable is verified, or {@code null}
 */
public record DeliverableRecord(
        int number,
        String title,
        String intentGloss,
        List<AffectedFile> affectedFiles,
        List<Integer> dependsOn,
        List<String> satisfies,
        @Nullable String verification
) {

    public static final String FIELD_NUMBER = "number";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_INTENT_GLOSS = "intent_gloss";
    public static final String FIELD_AFFECTED_FILES = "affected_files";
    public static final String FIELD_DEPENDS_ON = "depends_on";
    public static final String FIELD_SATISFIES = "satisfies";
    public static final String FIELD_VERIFICATION = "verification";

    private static final String TYPE = "DeliverableRecord";
    private static final Set<String> KNOWN_FIELDS = Set.of(
            FIELD_NUMBER, FIELD_TITLE, FIELD_INTENT_GLOSS, FIELD_AFFECTED_FILES, FIELD_DEPENDS_ON,
            FIELD_SATISFIES, FIELD_VERIFICATION);

    /** The closed list of what a deliverable does to an affected file. */
    public enum FileIntent {

        /** The file is read only. */
        READ("read"),

        /** The file is created. */
        WRITE_NEW("write-new"),

        /** The file is replaced. */
        WRITE_REPLACE("write-replace"),

        /** The file is written conditionally, when its gate fires. */
        WRITE_CONDITIONAL("write-conditional"),

        /** The file is deleted. */
        DELETE("delete");

        private final String wireValue;

        FileIntent(String wireValue) {
            this.wireValue = wireValue;
        }

        /**
         * @return the wire value used in representations
         */
        public String wireValue() {
            return wireValue;
        }

        /**
         * @return {@code true} if the intent changes the file
         */
        public boolean writes() {
            return this != READ;
        }

        /**
         * @param value the wire value
         * @return the matching intent, or {@code null}
         */
        public static FileIntent fromWireOrNull(String value) {
            if (value == null) {
                return null;
            }
            for (var intent : values()) {
                if (intent.wireValue.equalsIgnoreCase(value.trim())) {
                    return intent;
                }
            }
            return null;
        }

        /**
         * @param value the wire value
         * @return the matching intent
         * @throws IllegalArgumentException if no intent carries that value (fail-closed)
         */
        public static FileIntent fromWire(String value) {
            var intent = fromWireOrNull(value);
            if (intent == null) {
                throw new IllegalArgumentException("Unknown file intent: " + value);
            }
            return intent;
        }

        /**
         * @return the wire values of every intent, in catalogue order
         */
        public static List<String> wireValues() {
            return Arrays.stream(values()).map(FileIntent::wireValue).toList();
        }
    }

    /**
     * One affected file of a deliverable.
     *
     * @param path   the repository-relative path
     * @param intent what the deliverable does to it
     */
    public record AffectedFile(String path, FileIntent intent) {

        private static final String FIELD_PATH = "path";
        private static final String FIELD_INTENT = "intent";
        private static final String TYPE = "AffectedFile";
        private static final Set<String> KNOWN_FIELDS = Set.of(FIELD_PATH, FIELD_INTENT);

        public AffectedFile {
            Objects.requireNonNull(path, FIELD_PATH);
            Objects.requireNonNull(intent, FIELD_INTENT);
            if (path.isBlank()) {
                throw new IllegalArgumentException("path must not be blank");
            }
        }

        /**
         * @return the map representation
         */
        public Map<String, Object> toMap() {
            var map = new LinkedHashMap<String, Object>();
            map.put(FIELD_PATH, path);
            map.put(FIELD_INTENT, intent.wireValue());
            return map;
        }

        private void writeTo(JsonGenerator generator) throws IOException {
            generator.writeStartObject();
            generator.writeStringField(FIELD_PATH, path);
            generator.writeStringField(FIELD_INTENT, intent.wireValue());
            generator.writeEndObject();
        }

        private static AffectedFile fromMap(Map<String, Object> map) throws IOException {
            IntentJson.requireOnlyKnown(map, KNOWN_FIELDS, TYPE);
            try {
                return new AffectedFile(
                        IntentJson.requireText(map, FIELD_PATH, TYPE),
                        FileIntent.fromWire(IntentJson.requireText(map, FIELD_INTENT, TYPE)));
            } catch (IllegalArgumentException e) {
                throw new JsonFormatException("Invalid " + TYPE + ": " + e.getMessage(), e);
            }
        }
    }

    public DeliverableRecord {
        Objects.requireNonNull(title, FIELD_TITLE);
        Objects.requireNonNull(intentGloss, FIELD_INTENT_GLOSS);
        if (number < 1 || number > 99) {
            throw new IllegalArgumentException("number must be between 1 and 99");
        }
        if (title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        if (intentGloss.isBlank()) {
            throw new IllegalArgumentException("intent_gloss must not be blank");
        }
        affectedFiles = affectedFiles == null ? List.of() : List.copyOf(affectedFiles);
        dependsOn = dependsOn == null ? List.of() : List.copyOf(dependsOn);
        if (dependsOn.stream().anyMatch(value -> value == null || value < 1 || value > 99)) {
            throw new IllegalArgumentException("depends_on must name deliverables between 1 and 99");
        }
        satisfies = satisfies == null ? List.of() : List.copyOf(satisfies);
        if (satisfies.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new IllegalArgumentException("satisfies must not carry a blank criterion id");
        }
    }

    /**
     * @return the files the deliverable changes
     */
    public List<AffectedFile> writtenFiles() {
        return affectedFiles.stream().filter(file -> file.intent().writes()).toList();
    }

    /**
     * @param criterionId the criterion's id
     * @return {@code true} if the deliverable satisfies the criterion
     */
    public boolean satisfies(String criterionId) {
        return satisfies.contains(criterionId);
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_NUMBER, number);
        map.put(FIELD_TITLE, title);
        map.put(FIELD_INTENT_GLOSS, intentGloss);
        map.put(FIELD_AFFECTED_FILES, affectedFiles.stream().map(AffectedFile::toMap).toList());
        map.put(FIELD_DEPENDS_ON, dependsOn);
        map.put(FIELD_SATISFIES, satisfies);
        if (verification != null) {
            map.put(FIELD_VERIFICATION, verification);
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
     * Writes this deliverable to a streaming generator.
     *
     * @param generator the generator
     * @throws IOException on write failure
     */
    public void writeTo(JsonGenerator generator) throws IOException {
        generator.writeStartObject();
        generator.writeNumberField(FIELD_NUMBER, number);
        generator.writeStringField(FIELD_TITLE, title);
        generator.writeStringField(FIELD_INTENT_GLOSS, intentGloss);
        IntentJson.writeArray(generator, FIELD_AFFECTED_FILES, affectedFiles, AffectedFile::writeTo);
        generator.writeArrayFieldStart(FIELD_DEPENDS_ON);
        for (var dependency : dependsOn) {
            generator.writeNumber(dependency);
        }
        generator.writeEndArray();
        IntentJson.writeStrings(generator, FIELD_SATISFIES, satisfies);
        IntentJson.writeOptionalString(generator, FIELD_VERIFICATION, verification);
        generator.writeEndObject();
    }

    /**
     * @param map the input map
     * @return the parsed deliverable
     * @throws IOException if a required field is missing or a closed value is unknown
     */
    public static DeliverableRecord fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        IntentJson.requireOnlyKnown(map, KNOWN_FIELDS, TYPE);
        try {
            return new DeliverableRecord(
                    IntentJson.requireInt(map, FIELD_NUMBER, 1, 99, TYPE),
                    IntentJson.requireText(map, FIELD_TITLE, TYPE),
                    IntentJson.requireText(map, FIELD_INTENT_GLOSS, TYPE),
                    IntentJson.readEach(IntentJson.readObjects(map, FIELD_AFFECTED_FILES), AffectedFile::fromMap),
                    IntentJson.readInts(map, FIELD_DEPENDS_ON, 1, 99),
                    IntentJson.readStrings(map, FIELD_SATISFIES),
                    IntentJson.optionalText(map, FIELD_VERIFICATION, TYPE));
        } catch (IllegalArgumentException e) {
            throw new JsonFormatException("Invalid " + TYPE + ": " + e.getMessage(), e);
        }
    }

    /**
     * @param json the JSON object string
     * @return the parsed deliverable
     * @throws IOException if the value is not an object or a field is malformed
     */
    public static DeliverableRecord fromJson(String json) throws IOException {
        var object = JsonTree.asObject(JsonTree.parse(json));
        if (object == null) {
            throw new JsonFormatException("Expected JSON object for " + TYPE);
        }
        return fromMap(object);
    }

    /**
     * Reads one deliverable from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return the parsed deliverable
     * @throws IOException on format failure
     */
    public static DeliverableRecord readFrom(JsonParser parser) throws IOException {
        return fromMap(IntentJson.readObject(parser, TYPE));
    }
}
