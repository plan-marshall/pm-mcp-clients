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
 * The {@code over_size} distributions of a project, one per role whose multiple the project sets
 * (Model Work, A Worker Off Track; design <em>Intent and Outline</em>).
 * <p>
 * A role's multiple is changed one role at a time, but the project page shows every role's
 * distribution side by side, so the runtime answers the project's whole set in one read and the
 * client draws one chart per entry. The roles keep the runtime's order, and a role without measured
 * tasks carries a distribution without buckets rather than being left out: that is the answered
 * absence the client shows as its empty text, not a chart of invented zero bars.
 * <p>
 * Every entry names the same project, and no role appears twice: half-replaced or duplicated answers
 * are refused rather than drawn.
 *
 * @param projectId     the project whose roles are measured
 * @param distributions the distribution of each role, in the runtime's order
 */
public record OverSizeDistributionsResource(
        String projectId,
        List<OverSizeDistributionResource> distributions
) {

    public static final String FIELD_PROJECT_ID = "project_id";
    public static final String FIELD_DISTRIBUTIONS = "distributions";

    private static final String TYPE = "OverSizeDistributionsResource";
    private static final Set<String> KNOWN_FIELDS = Set.of(FIELD_PROJECT_ID, FIELD_DISTRIBUTIONS);

    public OverSizeDistributionsResource {
        Objects.requireNonNull(projectId, FIELD_PROJECT_ID);
        if (projectId.isBlank()) {
            throw new IllegalArgumentException("project_id must not be blank");
        }
        distributions = distributions == null ? List.of() : List.copyOf(distributions);
        var roles = new HashSet<String>();
        for (var distribution : distributions) {
            if (!projectId.equals(distribution.projectId())) {
                throw new IllegalArgumentException(
                        "every distribution must name the project " + projectId + ", not " + distribution.projectId());
            }
            if (!roles.add(distribution.role())) {
                throw new IllegalArgumentException("duplicate role: " + distribution.role());
            }
        }
    }

    /**
     * @param role the role
     * @return that role's distribution, or {@code null} where the project sets no multiple for it
     */
    public @Nullable OverSizeDistributionResource of(String role) {
        return distributions.stream()
                .filter(distribution -> distribution.role().equals(role))
                .findFirst()
                .orElse(null);
    }

    /**
     * @return the roles the project sets a multiple for, in the served order
     */
    public List<String> roles() {
        return distributions.stream().map(OverSizeDistributionResource::role).toList();
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_PROJECT_ID, projectId);
        map.put(FIELD_DISTRIBUTIONS, distributions.stream().map(OverSizeDistributionResource::toMap).toList());
        return map;
    }

    /**
     * @return the compact JSON representation
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Writes these distributions to a streaming generator.
     *
     * @param generator the generator
     * @throws IOException on write failure
     */
    public void writeTo(JsonGenerator generator) throws IOException {
        generator.writeStartObject();
        generator.writeStringField(FIELD_PROJECT_ID, projectId);
        IntentJson.writeArray(generator, FIELD_DISTRIBUTIONS, distributions, OverSizeDistributionResource::writeTo);
        generator.writeEndObject();
    }

    /**
     * @param map the input map
     * @return the parsed distributions
     * @throws IOException if a required field is missing or a closed value is unknown
     */
    public static OverSizeDistributionsResource fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        IntentJson.requireOnlyKnown(map, KNOWN_FIELDS, TYPE);
        try {
            return new OverSizeDistributionsResource(
                    IntentJson.requireText(map, FIELD_PROJECT_ID, TYPE),
                    IntentJson.readEach(IntentJson.readObjects(map, FIELD_DISTRIBUTIONS),
                            OverSizeDistributionResource::fromMap));
        } catch (IllegalArgumentException e) {
            throw new JsonFormatException("Invalid " + TYPE + ": " + e.getMessage(), e);
        }
    }

    /**
     * @param json the JSON object string
     * @return the parsed distributions
     * @throws IOException if the value is not an object or a field is malformed
     */
    public static OverSizeDistributionsResource fromJson(String json) throws IOException {
        var object = JsonTree.asObject(JsonTree.parse(json));
        if (object == null) {
            throw new JsonFormatException("Expected JSON object for " + TYPE);
        }
        return fromMap(object);
    }

    /**
     * Reads the distributions from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return the parsed distributions
     * @throws IOException on format failure
     */
    public static OverSizeDistributionsResource readFrom(JsonParser parser) throws IOException {
        return fromMap(IntentJson.readObject(parser, TYPE));
    }
}
