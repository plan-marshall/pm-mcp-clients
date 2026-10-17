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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * What the provider answered about a pull request before it is adopted (UC-STA-7).
 * <p>
 * The observation itself already performed the checks the workflow needs: the pull request is open,
 * its head branch is writable, the provider is not rate limited, and the title and body were
 * screened. Every changed file that comes from outside the machine is marked untrusted unless the
 * operator is its author.
 *
 * @param url          the pull request's URL
 * @param number       the pull request's number
 * @param title        its title
 * @param author       who opened it
 * @param branch       its head branch
 * @param state        {@code open} or {@code closed}
 * @param writable     whether its head branch may be written
 * @param changedFiles the files it touches
 * @param screening    the screening verdict of its external text
 * @param rateLimit    the provider's rate limit
 */
public record PullRequestObservation(
        String url,
        int number,
        String title,
        String author,
        String branch,
        String state,
        boolean writable,
        List<ChangedFile> changedFiles,
        ScreeningOutcome screening,
        RateLimitStatus rateLimit
) {

    /** The state of an open pull request. */
    public static final String STATE_OPEN = "open";

    /** The state of a closed pull request. */
    public static final String STATE_CLOSED = "closed";

    private static final String FIELD_URL = "url";
    private static final String FIELD_NUMBER = "number";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_AUTHOR = "author";
    private static final String FIELD_BRANCH = "branch";
    private static final String FIELD_STATE = "state";
    private static final String FIELD_WRITABLE = "writable";
    private static final String FIELD_CHANGED_FILES = "changed_files";
    private static final String FIELD_SCREENING = "screening";
    private static final String FIELD_RATE_LIMIT = "rate_limit";

    public PullRequestObservation {
        Objects.requireNonNull(url, "url");
        if (number < 0) {
            number = 0;
        }
        title = title == null ? "" : title;
        author = author == null ? "" : author;
        branch = branch == null ? "" : branch;
        state = state == null || state.isBlank() ? STATE_OPEN : state;
        changedFiles = changedFiles == null ? List.of() : List.copyOf(changedFiles);
        screening = screening == null ? ScreeningOutcome.CLEAN : screening;
        rateLimit = rateLimit == null ? RateLimitStatus.unlimited() : rateLimit;
    }

    /**
     * @return {@code true} if the pull request is open
     */
    public boolean open() {
        return STATE_OPEN.equalsIgnoreCase(state);
    }

    /**
     * @return the number of changed files that count as untrusted
     */
    public int untrustedCount() {
        return (int) changedFiles.stream().filter(ChangedFile::untrusted).count();
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_URL, url);
        map.put(FIELD_NUMBER, number);
        map.put(FIELD_TITLE, title);
        map.put(FIELD_AUTHOR, author);
        map.put(FIELD_BRANCH, branch);
        map.put(FIELD_STATE, state);
        map.put(FIELD_WRITABLE, writable);
        map.put(FIELD_CHANGED_FILES, changedFiles.stream().map(ChangedFile::toMap).toList());
        map.put(FIELD_SCREENING, screening.wireValue());
        map.put(FIELD_RATE_LIMIT, rateLimit.toMap());
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
     * @return the parsed observation
     * @throws IOException if a required field is missing
     */
    public static PullRequestObservation fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var url = JsonTree.string(map, FIELD_URL);
        if (url == null) {
            throw new JsonFormatException("Missing required field for PullRequestObservation: url");
        }
        var changedFiles = ChangedFile.listFrom(map, FIELD_CHANGED_FILES);
        var number = JsonTree.integer(map, FIELD_NUMBER);
        var screeningValue = JsonTree.string(map, FIELD_SCREENING);
        var rateLimitMap = JsonTree.object(map, FIELD_RATE_LIMIT);
        return new PullRequestObservation(url, number != null ? number : 0,
                JsonTree.string(map, FIELD_TITLE), JsonTree.string(map, FIELD_AUTHOR),
                JsonTree.string(map, FIELD_BRANCH), JsonTree.string(map, FIELD_STATE),
                Boolean.TRUE.equals(JsonTree.bool(map, FIELD_WRITABLE)), changedFiles,
                screeningValue != null ? ScreeningOutcome.fromWire(screeningValue) : ScreeningOutcome.CLEAN,
                rateLimitMap != null ? RateLimitStatus.fromMap(rateLimitMap) : RateLimitStatus.unlimited());
    }

    /**
     * @param json the JSON object string
     * @return the parsed observation
     * @throws IOException if the value is not an object or a field is malformed
     */
    public static PullRequestObservation fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for PullRequestObservation");
        }
        return fromMap(obj);
    }
}
