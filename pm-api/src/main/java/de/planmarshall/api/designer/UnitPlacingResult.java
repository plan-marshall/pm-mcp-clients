/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.designer;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * The result of placing or removing a workflow unit (UC-PLN-3).
 * <p>
 * A placing in a project is a short landing: the lines of the landing (written, committed, landed)
 * turn into ✓ as they happen. A placing on the machine is in force at once. The result names the
 * unit's author, release, and digest from what was placed, and the resulting catalogue digest of
 * each scope the act changed.
 *
 * @param unit            the unit's id
 * @param target          {@code "machine"} or the project's id
 * @param status          the terminal status ({@link #STATUS_WRITTEN}, {@link #STATUS_COMMITTED}, {@link #STATUS_LANDED}, or {@link #STATUS_FAILED})
 * @param steps           the landing steps that happened, in order ({@code written}, {@code committed}, {@code landed})
 * @param catalogueDigest the resulting catalogue digest of the target span, or {@code null}
 * @param author          the author from what was placed, or {@code null}
 * @param release         the release from what was placed, or {@code null}
 * @param digest          the digest of what was placed, or {@code null}
 * @param reason          the reason of a failed landing, or {@code null}
 */
public record UnitPlacingResult(
        String unit,
        String target,
        String status,
        List<String> steps,
        String catalogueDigest,
        String author,
        String release,
        String digest,
        String reason
) {
    /** Status: written to the checkout (under {@code landing.files}) and not yet committed. */
    public static final String STATUS_WRITTEN = "written";
    /** Status: committed in the project's checkout. */
    public static final String STATUS_COMMITTED = "committed";
    /** Status: landed, in force from now on. */
    public static final String STATUS_LANDED = "landed";
    /** Status: the landing failed; the unit is not in force. */
    public static final String STATUS_FAILED = "failed";

    /** Landing step: written. */
    public static final String STEP_WRITTEN = "written";
    /** Landing step: committed. */
    public static final String STEP_COMMITTED = "committed";
    /** Landing step: landed. */
    public static final String STEP_LANDED = "landed";

    private static final String FIELD_UNIT = "unit";
    private static final String FIELD_TARGET = "target";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_STEPS = "steps";
    private static final String FIELD_CATALOGUE_DIGEST = "catalogue_digest";
    private static final String FIELD_AUTHOR = "author";
    private static final String FIELD_RELEASE = "release";
    private static final String FIELD_DIGEST = "digest";
    private static final String FIELD_REASON = "reason";

    /**
     * Compact constructor validating required properties.
     */
    public UnitPlacingResult {
        Objects.requireNonNull(unit, FIELD_UNIT);
        Objects.requireNonNull(target, FIELD_TARGET);
        status = status != null ? status : STATUS_WRITTEN;
        steps = steps != null ? List.copyOf(steps) : List.of();
    }

    /**
     * @return this result as a JSON tree
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_UNIT, unit);
        map.put(FIELD_TARGET, target);
        map.put(FIELD_STATUS, status);
        map.put(FIELD_STEPS, steps);
        if (catalogueDigest != null) {
            map.put(FIELD_CATALOGUE_DIGEST, catalogueDigest);
        }
        if (author != null) {
            map.put(FIELD_AUTHOR, author);
        }
        if (release != null) {
            map.put(FIELD_RELEASE, release);
        }
        if (digest != null) {
            map.put(FIELD_DIGEST, digest);
        }
        if (reason != null) {
            map.put(FIELD_REASON, reason);
        }
        return map;
    }

    /**
     * @return this result as compact JSON
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Parses a result from a JSON tree.
     *
     * @param map the input tree
     * @return the result
     * @throws IOException on a malformed tree
     */
    public static UnitPlacingResult fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var unit = JsonTree.string(map, FIELD_UNIT);
        var target = JsonTree.string(map, FIELD_TARGET);
        if (unit == null || target == null) {
            throw new JsonFormatException("UnitPlacingResult requires a unit and a target");
        }
        return new UnitPlacingResult(
                unit,
                target,
                JsonTree.string(map, FIELD_STATUS),
                DesignerJson.stringList(map, FIELD_STEPS),
                JsonTree.string(map, FIELD_CATALOGUE_DIGEST),
                JsonTree.string(map, FIELD_AUTHOR),
                JsonTree.string(map, FIELD_RELEASE),
                JsonTree.string(map, FIELD_DIGEST),
                JsonTree.string(map, FIELD_REASON)
        );
    }

    /**
     * Parses a result from JSON.
     *
     * @param json the JSON text
     * @return the result
     * @throws IOException on a malformed document
     */
    public static UnitPlacingResult fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected a JSON object for UnitPlacingResult");
        }
        return fromMap(obj);
    }
}
