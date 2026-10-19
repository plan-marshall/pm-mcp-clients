/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.control;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * The archive check of an epic (design <em>Archiving: Is It Through?</em>): its lines, the counts it
 * carries, and whether the completion review still runs first.
 *
 * <p>On an epic whose completion review has not run yet, a manual one, {@link #completionReviewFirst()}
 * is set and the checks are replaced by the line {@code control.archive.review_first}; only an open
 * finding holds the archive, while the kept findings and the watches are carried with the ledger.
 *
 * @param epicId                the epic checked
 * @param lines                 the check lines
 * @param carriedFindings       how many kept findings are carried into the archive
 * @param carriedWatches        how many watches are carried into the archive
 * @param completionReviewFirst {@code true} while the completion review still runs first
 */
public record ArchiveCheck(
        String epicId,
        List<ArchiveCheckLine> lines,
        int carriedFindings,
        int carriedWatches,
        boolean completionReviewFirst
) {

    public ArchiveCheck {
        Objects.requireNonNull(epicId, "epicId");
        lines = lines != null ? List.copyOf(lines) : List.of();
    }

    /**
     * @return {@code true} if every line passes and the archive is offered
     */
    public boolean passed() {
        return !completionReviewFirst && lines.stream().allMatch(ArchiveCheckLine::passed);
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("epic_id", epicId);
        map.put("lines", lines.stream().map(ArchiveCheckLine::toMap).toList());
        map.put("carried_findings", carriedFindings);
        map.put("carried_watches", carriedWatches);
        map.put("completion_review_first", completionReviewFirst);
        map.put("passed", passed());
        return map;
    }

    /**
     * @return the compact JSON representation
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * @param map the input map
     * @return the parsed check
     * @throws IOException if a required field is missing
     */
    public static ArchiveCheck fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var epicId = JsonTree.string(map, "epic_id");
        if (epicId == null) {
            throw new JsonFormatException("Missing epic_id in ArchiveCheck");
        }
        var carriedFindings = ControlJson.optInteger(map, "carried_findings");
        var carriedWatches = ControlJson.optInteger(map, "carried_watches");
        return new ArchiveCheck(epicId, readLines(map.get("lines")),
                carriedFindings != null ? carriedFindings : 0,
                carriedWatches != null ? carriedWatches : 0,
                ControlJson.bool(map, "completion_review_first"));
    }

    /**
     * @param json the JSON object string
     * @return the parsed check
     * @throws IOException if the value is not an object
     */
    public static ArchiveCheck fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for ArchiveCheck");
        }
        return fromMap(obj);
    }

    private static List<ArchiveCheckLine> readLines(@Nullable Object raw) throws IOException {
        var lines = new ArrayList<ArchiveCheckLine>();
        for (var object : ControlJson.readObjects(raw)) {
            lines.add(ArchiveCheckLine.fromMap(object));
        }
        return lines;
    }
}
