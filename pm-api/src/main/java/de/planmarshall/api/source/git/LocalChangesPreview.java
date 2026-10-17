/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.source.git;

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
 * The preview of the checkout's uncommitted changes before they are captured (UC-STA-9).
 * <p>
 * The preview shows every changed file with its line counts and answers the landing precondition:
 * local changes can only be captured where the landing in force commits ({@code PM-WF-25}). Where it
 * does not, {@link #eligible()} is {@code false} and {@link #reason()} names the landing that stands
 * in the way.
 *
 * @param status        the state the preview reached ({@code previewed})
 * @param branch        the branch the changes are against, or {@code null} when it reports none
 * @param changedFiles  the changed files with their line counts
 * @param landing       the project's landing in force, or {@code null} when it reports none
 * @param landingCommits whether the landing in force commits the captured snapshot
 * @param eligible      whether the local changes may be captured
 * @param reason        why they may not, or {@code null} while they may
 */
public record LocalChangesPreview(
        String status,
        @Nullable String branch,
        List<ChangedFile> changedFiles,
        @Nullable String landing,
        boolean landingCommits,
        boolean eligible,
        @Nullable String reason
) {

    /** The status a successful preview reaches. */
    public static final String STATUS_PREVIEWED = "previewed";

    private static final String FIELD_STATUS = "status";
    private static final String FIELD_BRANCH = "branch";
    private static final String FIELD_CHANGED_FILES = "changed_files";
    private static final String FIELD_LANDING = "landing";
    private static final String FIELD_LANDING_COMMITS = "landing_commits";
    private static final String FIELD_ELIGIBLE = "eligible";
    private static final String FIELD_REASON = "reason";

    public LocalChangesPreview {
        status = status == null || status.isBlank() ? STATUS_PREVIEWED : status;
        changedFiles = changedFiles == null ? List.of() : List.copyOf(changedFiles);
    }

    /**
     * @return the total lines added across the changed files
     */
    public int totalAdditions() {
        return changedFiles.stream().mapToInt(ChangedFile::additions).sum();
    }

    /**
     * @return the total lines removed across the changed files
     */
    public int totalDeletions() {
        return changedFiles.stream().mapToInt(ChangedFile::deletions).sum();
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_STATUS, status);
        if (branch != null) {
            map.put(FIELD_BRANCH, branch);
        }
        map.put(FIELD_CHANGED_FILES, changedFiles.stream().map(ChangedFile::toMap).toList());
        if (landing != null) {
            map.put(FIELD_LANDING, landing);
        }
        map.put(FIELD_LANDING_COMMITS, landingCommits);
        map.put(FIELD_ELIGIBLE, eligible);
        if (reason != null) {
            map.put(FIELD_REASON, reason);
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
     * @param map the input map
     * @return the parsed preview
     * @throws IOException if a field is malformed
     */
    public static LocalChangesPreview fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var changedFiles = new ArrayList<ChangedFile>();
        var raw = JsonTree.asArray(map.get(FIELD_CHANGED_FILES));
        if (raw != null) {
            for (var item : raw) {
                var obj = JsonTree.asObject(item);
                if (obj != null) {
                    changedFiles.add(ChangedFile.fromMap(obj));
                }
            }
        }
        return new LocalChangesPreview(JsonTree.string(map, FIELD_STATUS), JsonTree.string(map, FIELD_BRANCH),
                changedFiles, JsonTree.string(map, FIELD_LANDING),
                Boolean.TRUE.equals(JsonTree.bool(map, FIELD_LANDING_COMMITS)),
                Boolean.TRUE.equals(JsonTree.bool(map, FIELD_ELIGIBLE)),
                JsonTree.string(map, FIELD_REASON));
    }

    /**
     * @param json the JSON object string
     * @return the parsed preview
     * @throws IOException if the value is not an object or a field is malformed
     */
    public static LocalChangesPreview fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for LocalChangesPreview");
        }
        return fromMap(obj);
    }
}
