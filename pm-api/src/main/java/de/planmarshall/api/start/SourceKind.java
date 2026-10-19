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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

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

    /** The kinds an epic's start offers: descriptions, documents, and issues. */
    private static final Set<SourceKind> EPIC_KINDS = EnumSet.of(DESCRIPTION, DOCUMENTS, ISSUE);

    /** The kinds that come from outside the machine and are screened once stored. */
    private static final Set<SourceKind> EXTERNAL_KINDS = EnumSet.of(ISSUE, PULL_REQUEST);

    /** The kinds that stand only beside descriptions and documents, and never beside each other. */
    private static final Set<SourceKind> ALONE_KINDS = EnumSet.of(LOCAL_CHANGES, PULL_REQUEST);

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
     * @return {@code true} if the kind's text comes from outside the machine and is screened once
     *         stored
     */
    public boolean external() {
        return EXTERNAL_KINDS.contains(this);
    }

    /**
     * @return {@code true} if the kind is stored as a document file or a directory tree
     */
    public boolean documents() {
        return this == DOCUMENTS;
    }

    /**
     * @return {@code true} if the kind is offered to an epic's start
     */
    public boolean offeredToEpic() {
        return EPIC_KINDS.contains(this);
    }

    /**
     * The kinds a start offers beside the sources already in its list (State Catalogue,
     * {@code start.sources}; design <em>Starting Work</em>, the list of sources).
     * <p>
     * An epic offers description, documents, and issue. A plan also offers lesson (only where the
     * project has unclaimed lessons), local changes (only where the checkout has some and the
     * project's landing in force commits), and pull request. A pull request or the local changes
     * stand only beside descriptions and documents, never both and never beside an issue or a
     * lesson; once one of them is in the list no issue or lesson may be added.
     *
     * @param entryKind        the kind of the entry
     * @param existing         the kinds already in the list
     * @param lessonsAvailable whether the project has unclaimed lessons
     * @param localChanges     whether the checkout has local changes
     * @param landingCommits   whether the project's landing in force commits (not {@code landing.files})
     * @return the kinds that may be added, in catalogue order
     */
    public static List<SourceKind> offered(EntryKind entryKind, Set<SourceKind> existing,
                                           boolean lessonsAvailable, boolean localChanges, boolean landingCommits) {
        var present = existing == null ? Set.<SourceKind>of() : existing;
        var alone = present.contains(LOCAL_CHANGES) || present.contains(PULL_REQUEST);
        var offered = new ArrayList<SourceKind>();
        for (var kind : values()) {
            if (!kind.offeredToEpic() && entryKind.epic()) {
                continue;
            }
            if (present.contains(kind) && kind != DESCRIPTION) {
                continue;
            }
            if (kind == DESCRIPTION || kind == DOCUMENTS) {
                offered.add(kind);
                continue;
            }
            if (alone) {
                continue;
            }
            if (kind == ISSUE && present.contains(LESSON)) {
                continue;
            }
            if (kind == LESSON && (!lessonsAvailable || present.contains(ISSUE))) {
                continue;
            }
            if (kind == LOCAL_CHANGES && (!localChanges || !landingCommits)) {
                continue;
            }
            if (ALONE_KINDS.contains(kind) && !onlyDescriptionsAndDocuments(present)) {
                continue;
            }
            offered.add(kind);
        }
        return List.copyOf(offered);
    }

    /**
     * @param present the kinds in the list
     * @return {@code true} if the list holds descriptions and documents only (possibly nothing)
     */
    public static boolean onlyDescriptionsAndDocuments(Set<SourceKind> present) {
        return present == null || present.stream().allMatch(kind -> kind == DESCRIPTION || kind == DOCUMENTS);
    }

    /**
     * Resolves a kind from its wire value, ignoring case.
     *
     * @param value the wire value
     * @return the matching kind, or {@code null} if none matches
     */
    public static SourceKind fromWireOrNull(String value) {
        if (value == null) {
            return null;
        }
        var clean = value.trim();
        return Arrays.stream(values())
                .filter(kind -> kind.wireValue.equalsIgnoreCase(clean))
                .findFirst()
                .orElse(null);
    }

    /**
     * Resolves a kind from its wire value, ignoring case.
     *
     * @param value the wire value
     * @return the matching kind
     * @throws IllegalArgumentException if no kind carries that value
     */
    public static SourceKind fromWire(String value) {
        var kind = fromWireOrNull(value);
        if (kind == null) {
            throw new IllegalArgumentException("Unknown source kind: " + value);
        }
        return kind;
    }

    /**
     * @return the wire values of every kind, in catalogue order
     */
    public static List<String> wireValues() {
        return Arrays.stream(values()).map(SourceKind::wireValue).toList();
    }
}
