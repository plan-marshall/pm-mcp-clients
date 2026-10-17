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

import java.util.Arrays;

/**
 * The closed list of kinds a source of a start can be (UC-STA-1 to UC-STA-5).
 * <p>
 * The wire value is the one the representation and the message catalogue use
 * ({@code start.source.<wire>}, {@code source-<wire>}).
 */
public enum SourceKind {

    /** The operator's own description of the work. */
    DESCRIPTION("description"),

    /** Files or one directory brought in as documents. */
    DOCUMENTS("documents"),

    /** An issue of the project or of another repository. */
    ISSUE("issue"),

    /** An unclaimed lesson of the project. */
    LESSON("lesson"),

    /** The checkout's local changes against its branch. */
    LOCAL_CHANGES("local_changes"),

    /** A pull request of the project's own repository. */
    PULL_REQUEST("pull_request");

    private final String wireValue;

    SourceKind(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * @return the wire value used in representations and message keys
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Resolves a kind from its wire value, ignoring case.
     *
     * @param value the wire value
     * @return the matching kind
     * @throws IllegalArgumentException if no kind carries that value
     */
    public static SourceKind fromWire(String value) {
        if (value != null) {
            var clean = value.trim();
            return Arrays.stream(values())
                    .filter(kind -> kind.wireValue.equalsIgnoreCase(clean))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown source kind: " + value));
        }
        throw new IllegalArgumentException("Source kind must not be null");
    }
}
