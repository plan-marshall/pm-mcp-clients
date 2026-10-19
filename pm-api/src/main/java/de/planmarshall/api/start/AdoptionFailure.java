/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.start;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Why an epic's ledger cannot be adopted (State Catalogue, {@code start-epic.adopt-failed}; design
 * <em>Starting Work</em>, adopting an epic from elsewhere).
 * <p>
 * The cause is shown with <em>Try again</em> (after the ledger was repaired through git),
 * <em>Back</em> to the list of sources, and <em>Abandon</em>. Nothing is created and the ledger is
 * never rewritten.
 *
 * @param epicId the epic the adoption was for
 * @param reason the closed reason
 * @param file   the failing file for {@link AdoptionFailureReason#LEDGER_UNREADABLE}, or {@code null}
 * @param detail the failure detail, or {@code null}
 */
public record AdoptionFailure(
        String epicId,
        AdoptionFailureReason reason,
        @Nullable String file,
        @Nullable String detail
) {

    public static final String FIELD_EPIC_ID = "epic_id";
    public static final String FIELD_REASON = "reason";
    public static final String FIELD_FILE = "file";
    public static final String FIELD_DETAIL = "detail";

    private static final Set<String> KNOWN_FIELDS = Set.of(FIELD_EPIC_ID, FIELD_REASON, FIELD_FILE, FIELD_DETAIL);

    public AdoptionFailure {
        Objects.requireNonNull(epicId, FIELD_EPIC_ID);
        Objects.requireNonNull(reason, FIELD_REASON);
        if (reason == AdoptionFailureReason.LEDGER_UNREADABLE && (file == null || file.isBlank())) {
            throw new IllegalArgumentException("An unreadable ledger names the failing file");
        }
    }

    /**
     * @param epicId the epic
     * @param file   the failing file
     * @param detail the read failure
     * @return an unreadable-ledger failure
     */
    public static AdoptionFailure unreadable(String epicId, String file, @Nullable String detail) {
        return new AdoptionFailure(epicId, AdoptionFailureReason.LEDGER_UNREADABLE, file, detail);
    }

    /**
     * @param epicId the epic
     * @param detail the finding
     * @return an invalid-ledger failure
     */
    public static AdoptionFailure invalid(String epicId, @Nullable String detail) {
        return new AdoptionFailure(epicId, AdoptionFailureReason.LEDGER_INVALID, null, detail);
    }

    /**
     * @param epicId the epic
     * @param detail the terminal reason
     * @return a terminal-ledger failure
     */
    public static AdoptionFailure terminal(String epicId, @Nullable String detail) {
        return new AdoptionFailure(epicId, AdoptionFailureReason.LEDGER_TERMINAL, null, detail);
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_EPIC_ID, epicId);
        map.put(FIELD_REASON, reason.wireValue());
        if (file != null) {
            map.put(FIELD_FILE, file);
        }
        if (detail != null) {
            map.put(FIELD_DETAIL, detail);
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
     * Writes this failure to a streaming generator.
     *
     * @param generator the generator
     * @throws IOException on write failure
     */
    public void writeTo(JsonGenerator generator) throws IOException {
        JsonTree.writeValue(generator, toMap());
    }

    /**
     * @param map the input map
     * @return the parsed failure
     * @throws IOException if a required field is missing or a field is malformed
     */
    public static AdoptionFailure fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        StartJson.requireOnlyKnown(map, KNOWN_FIELDS, "AdoptionFailure");
        var epicId = JsonTree.string(map, FIELD_EPIC_ID);
        if (epicId == null) {
            throw new JsonFormatException("Missing required field for AdoptionFailure: epic_id");
        }
        var reason = AdoptionFailureReason.fromWireOrNull(JsonTree.string(map, FIELD_REASON));
        if (reason == null) {
            throw new JsonFormatException("Unknown " + FIELD_REASON + " in AdoptionFailure: "
                    + JsonTree.string(map, FIELD_REASON));
        }
        try {
            return new AdoptionFailure(epicId, reason, JsonTree.string(map, FIELD_FILE),
                    JsonTree.string(map, FIELD_DETAIL));
        } catch (IllegalArgumentException e) {
            throw new JsonFormatException("Invalid AdoptionFailure: " + e.getMessage(), e);
        }
    }

    /**
     * @param json the JSON object string
     * @return the parsed failure
     * @throws IOException if the value is not an object or a field is malformed
     */
    public static AdoptionFailure fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for AdoptionFailure");
        }
        return fromMap(obj);
    }

    /**
     * Reads one failure from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return the parsed failure
     * @throws IOException on format failure
     */
    public static AdoptionFailure readFrom(JsonParser parser) throws IOException {
        var map = JsonTree.asObject(JsonTree.readValue(parser));
        if (map == null) {
            throw new JsonFormatException("Expected JSON object for AdoptionFailure");
        }
        return fromMap(map);
    }
}
