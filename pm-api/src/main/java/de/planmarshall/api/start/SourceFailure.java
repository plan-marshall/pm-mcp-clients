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
import java.time.DateTimeException;
import java.time.Instant;
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
 * Why a source of a start failed, the closed {@link SourceFailureCause} with what the client needs to
 * show it (State Catalogue, Source Units; design <em>Starting Work</em>, the sources).
 * <p>
 * {@code resets_at} is the instant the forge's rate limit resets, carried as an <em>epoch second</em>
 * ({@code 1760884800}) so a client can compare it with its own clock without parsing a timestamp. It
 * is present exactly for {@link SourceFailureCause#RATE_LIMITED}, which is never retried by the
 * client or the server on its own: the operator tries again after that instant, or adds a credential
 * for the forge whose limit is higher.
 *
 * @param cause    the closed cause
 * @param detail   what the client names beside the cause (a forge, a file, a time), or {@code null}
 * @param resetsAt when the rate limit resets, or {@code null} for every cause but
 *                 {@link SourceFailureCause#RATE_LIMITED}
 */
public record SourceFailure(
        SourceFailureCause cause,
        @Nullable String detail,
        @Nullable Instant resetsAt
) {

    public static final String FIELD_CAUSE = "cause";
    public static final String FIELD_DETAIL = "detail";
    public static final String FIELD_RESETS_AT = "resets_at";

    private static final Set<String> KNOWN_FIELDS = Set.of(FIELD_CAUSE, FIELD_DETAIL, FIELD_RESETS_AT);

    public SourceFailure {
        Objects.requireNonNull(cause, FIELD_CAUSE);
        if (cause.namesReset() && resetsAt == null) {
            throw new IllegalArgumentException("A rate_limited failure names the instant it resets");
        }
        if (!cause.namesReset() && resetsAt != null) {
            throw new IllegalArgumentException("Only a rate_limited failure names a reset instant: " + cause.wireValue());
        }
    }

    /**
     * @param cause the cause
     * @return a failure of that cause without a detail or a reset
     */
    public static SourceFailure of(SourceFailureCause cause) {
        return new SourceFailure(cause, null, null);
    }

    /**
     * @param cause    the cause
     * @param detail   the detail
     * @return a failure of that cause with a detail
     */
    public static SourceFailure of(SourceFailureCause cause, String detail) {
        return new SourceFailure(cause, detail, null);
    }

    /**
     * @param resetsAt when the forge's rate limit resets
     * @param forge    the forge that refused the fetch
     * @return a rate-limited failure
     */
    public static SourceFailure rateLimited(Instant resetsAt, String forge) {
        return new SourceFailure(SourceFailureCause.RATE_LIMITED, forge, resetsAt);
    }

    /**
     * @return {@code true} if the operator may try the gathering again
     */
    public boolean retryable() {
        return cause.retryable();
    }

    /**
     * @return the reset instant as epoch seconds, or {@code null}
     */
    public @Nullable Long resetsAtEpochSecond() {
        return resetsAt != null ? resetsAt.getEpochSecond() : null;
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_CAUSE, cause.wireValue());
        if (detail != null) {
            map.put(FIELD_DETAIL, detail);
        }
        if (resetsAt != null) {
            map.put(FIELD_RESETS_AT, resetsAt.getEpochSecond());
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
     * @throws IOException if the cause is missing or unknown, or the reset does not fit the cause
     */
    public static SourceFailure fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        StartJson.requireOnlyKnown(map, KNOWN_FIELDS, "SourceFailure");
        var cause = SourceFailureCause.fromWireOrNull(JsonTree.string(map, FIELD_CAUSE));
        if (cause == null) {
            throw new JsonFormatException(
                    "Unknown " + FIELD_CAUSE + " in SourceFailure: " + JsonTree.string(map, FIELD_CAUSE));
        }
        var reset = StartJson.optLong(map, FIELD_RESETS_AT);
        try {
            return new SourceFailure(cause, JsonTree.string(map, FIELD_DETAIL),
                    reset != null ? Instant.ofEpochSecond(reset) : null);
        } catch (IllegalArgumentException | DateTimeException e) {
            throw new JsonFormatException("Invalid SourceFailure: " + e.getMessage(), e);
        }
    }

    /**
     * @param json the JSON object string
     * @return the parsed failure
     * @throws IOException if the value is not an object or a field is malformed
     */
    public static SourceFailure fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for SourceFailure");
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
    public static SourceFailure readFrom(JsonParser parser) throws IOException {
        var map = JsonTree.asObject(JsonTree.readValue(parser));
        if (map == null) {
            throw new JsonFormatException("Expected JSON object for SourceFailure");
        }
        return fromMap(map);
    }
}
