/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.interaction;

import java.util.Arrays;
import java.util.List;

/**
 * The closed enumeration of the sources of verbatim text the runtime carries to a client: text that
 * does not come from the runtime is never a message key and is carried verbatim, marked with its
 * source. It is the closed list of the Hypermedia Format specification (Texts of the Runtime).
 * <p>
 * Text of a {@code model}, a {@code provider}, a {@code harness}, or a {@code repository} is
 * untrusted; a client shows verbatim text as plain text, never as markup or a link it follows.
 */
public enum TextSource {

    /** A provider's answer. */
    PROVIDER("provider"),

    /** A harness's own error text. */
    HARNESS("harness"),

    /** A model's text (a question, an option, a rationale). */
    MODEL("model"),

    /** The operator's own words or note. */
    OPERATOR("operator"),

    /** Text drawn from the repository (a title from a request, a file content). */
    REPOSITORY("repository");

    private final String wire;

    TextSource(String wire) {
        this.wire = wire;
    }

    /**
     * @return the wire representation of this source
     */
    public String wire() {
        return wire;
    }

    /**
     * Resolves a wire value to its source.
     *
     * @param wire the wire representation
     * @return the matching source, or {@code null} if none matches
     */
    public static TextSource fromWireOrNull(String wire) {
        if (wire == null) {
            return null;
        }
        for (var source : values()) {
            if (source.wire.equals(wire)) {
                return source;
            }
        }
        return null;
    }

    /**
     * Resolves a wire value to its source.
     *
     * @param wire the wire representation
     * @return the matching source
     * @throws IllegalArgumentException if no source matches (fail-closed)
     */
    public static TextSource fromWire(String wire) {
        var source = fromWireOrNull(wire);
        if (source == null) {
            throw new IllegalArgumentException("Unknown text source: " + wire);
        }
        return source;
    }

    /**
     * @return the wire representations of every source, in catalogue order
     */
    public static List<String> wireValues() {
        return Arrays.stream(values()).map(TextSource::wire).toList();
    }
}
