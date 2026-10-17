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

import java.util.Arrays;
import java.util.List;

/**
 * The closed enumeration of building-block categories of the process designer (UC-PLN-3).
 * <p>
 * The catalogue of the runtime offers every block under exactly one of these kinds; the web app
 * groups the palette by them ({@code designer.blocks.group}). The wire value is the lower-case
 * form used on {@code /api/v1/workflows/catalogue}.
 */
public enum BlockKind {

    /** A keyword or clause of the language ({@code state}, {@code call}, {@code declare}, …). */
    KEYWORD("keyword"),

    /** A primitive of {@code do}, {@code auto}, {@code job}, or {@code guard}. */
    PRIMITIVE("primitive"),

    /** A guard primitive with what it reads, including {@code depends_on_config}. */
    GUARD("guard"),

    /** A link relation with its forms and its default form. */
    LINK_RELATION("link_relation"),

    /** A condition kind ({@code in_worktree}, {@code has_open_findings}, …). */
    CONDITION("condition"),

    /** A sub-workflow (a phase among them), reusable or compiled only. */
    SUB_WORKFLOW("sub_workflow"),

    /** A role of the effective role set of the scope. */
    ROLE("role"),

    /** A declarable parameter of the unit open, with its compiled bounds. */
    PARAMETER("parameter");

    private final String wire;

    BlockKind(String wire) {
        this.wire = wire;
    }

    /**
     * @return the wire representation of this kind
     */
    public String wire() {
        return wire;
    }

    /**
     * Resolves a wire value to its kind.
     *
     * @param wire the wire representation
     * @return the matching kind, or {@code null} if none matches
     */
    public static BlockKind fromWire(String wire) {
        if (wire == null) {
            return null;
        }
        for (var kind : values()) {
            if (kind.wire.equalsIgnoreCase(wire)) {
                return kind;
            }
        }
        return null;
    }

    /**
     * @param wire the wire representation
     * @return the matching kind
     * @throws IllegalArgumentException if no kind matches
     */
    public static BlockKind requireWire(String wire) {
        var kind = fromWire(wire);
        if (kind == null) {
            throw new IllegalArgumentException("Unknown building-block kind: " + wire);
        }
        return kind;
    }

    /**
     * @return the wire representations of every kind, in declaration order
     */
    public static List<String> wireValues() {
        return Arrays.stream(values()).map(BlockKind::wire).toList();
    }
}
