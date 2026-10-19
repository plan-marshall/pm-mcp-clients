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

import de.planmarshall.api.id.DottedIdentifier;
import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * One kind of source a start offers, with whether it is offered now and, where it is not, the
 * message key and arguments of the reason (State Catalogue, {@code start.sources}; design
 * <em>Starting Work</em>, the list of sources).
 * <p>
 * The reason is a message key, never prose: {@code start.sources.alone} with its {@code kind}
 * argument where a pull request or the local changes stand alone beside descriptions and documents,
 * {@code start.sources.local_changes_landing} with the project's {@code landing} where the landing
 * in force is {@code landing.files}, and {@code start.choose.no_recipe} where the project offers no
 * recipe.
 *
 * @param kind        the kind of source
 * @param available   whether the kind may be added now
 * @param reasonKey   the message key of the reason, or {@code null} while the kind is available
 * @param reasonArgs  the arguments of the reason message key
 */
public record SourceKindOffer(
        SourceKind kind,
        boolean available,
        @Nullable String reasonKey,
        Map<String, Object> reasonArgs
) {

    public static final String FIELD_KIND = "kind";
    public static final String FIELD_AVAILABLE = "available";
    public static final String FIELD_REASON_KEY = "reason_key";
    public static final String FIELD_REASON_ARGS = "reason_args";

    /** The message key of a pull request or the local changes that stand alone. */
    public static final String REASON_ALONE = "start.sources.alone";

    /** The message key of the local changes refused by a landing that does not commit. */
    public static final String REASON_LOCAL_CHANGES_LANDING = "start.sources.local_changes_landing";

    private static final Set<String> KNOWN_FIELDS =
            Set.of(FIELD_KIND, FIELD_AVAILABLE, FIELD_REASON_KEY, FIELD_REASON_ARGS);

    public SourceKindOffer {
        Objects.requireNonNull(kind, FIELD_KIND);
        reasonArgs = JsonTree.immutableMap(reasonArgs);
        if (available && (reasonKey != null || !reasonArgs.isEmpty())) {
            throw new IllegalArgumentException("An available kind carries no reason: " + kind.wireValue());
        }
        if (!available && reasonKey == null) {
            throw new IllegalArgumentException("A kind that is not offered names its reason: " + kind.wireValue());
        }
    }

    /**
     * @param kind the kind
     * @return an offer of the kind
     */
    public static SourceKindOffer available(SourceKind kind) {
        return new SourceKindOffer(kind, true, null, Map.of());
    }

    /**
     * @param kind      the kind
     * @param reasonKey the message key of the reason
     * @param args      the arguments of the reason
     * @return a refusal of the kind
     */
    public static SourceKindOffer refused(SourceKind kind, String reasonKey, Map<String, Object> args) {
        return new SourceKindOffer(kind, false, reasonKey, args);
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_KIND, kind.wireValue());
        map.put(FIELD_AVAILABLE, available);
        if (reasonKey != null) {
            map.put(FIELD_REASON_KEY, reasonKey);
        }
        if (!reasonArgs.isEmpty()) {
            map.put(FIELD_REASON_ARGS, reasonArgs);
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
     * Writes this offer to a streaming generator.
     *
     * @param generator the generator
     * @throws IOException on write failure
     */
    public void writeTo(JsonGenerator generator) throws IOException {
        JsonTree.writeValue(generator, toMap());
    }

    /**
     * @param map the input map
     * @return the parsed offer
     * @throws IOException if a required field is missing or a field is malformed
     */
    public static SourceKindOffer fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        StartJson.requireOnlyKnown(map, KNOWN_FIELDS, "SourceKindOffer");
        var kind = SourceKind.fromWireOrNull(JsonTree.string(map, FIELD_KIND));
        if (kind == null) {
            throw new JsonFormatException("Unknown " + FIELD_KIND + " in SourceKindOffer: "
                    + JsonTree.string(map, FIELD_KIND));
        }
        var reasonKey = JsonTree.string(map, FIELD_REASON_KEY);
        if (reasonKey != null && !DottedIdentifier.messageKey(reasonKey)) {
            throw new JsonFormatException("Invalid " + FIELD_REASON_KEY + " in SourceKindOffer: " + reasonKey);
        }
        var args = JsonTree.objectOrNull(map, FIELD_REASON_ARGS, "SourceKindOffer");
        try {
            return new SourceKindOffer(kind, StartJson.bool(map, FIELD_AVAILABLE), reasonKey, args);
        } catch (IllegalArgumentException e) {
            throw new JsonFormatException("Invalid SourceKindOffer: " + e.getMessage(), e);
        }
    }

    /**
     * @param json the JSON object string
     * @return the parsed offer
     * @throws IOException if the value is not an object or a field is malformed
     */
    public static SourceKindOffer fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for SourceKindOffer");
        }
        return fromMap(obj);
    }

    /**
     * Reads one offer from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return the parsed offer
     * @throws IOException on format failure
     */
    public static SourceKindOffer readFrom(JsonParser parser) throws IOException {
        var map = JsonTree.asObject(JsonTree.readValue(parser));
        if (map == null) {
            throw new JsonFormatException("Expected JSON object for SourceKindOffer");
        }
        return fromMap(map);
    }
}
