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
import java.util.List;

/**
 * The closed list of what the operator may do with an unscreened source (State Catalogue,
 * {@code source-<kind>.unscreened}).
 * <p>
 * The screening fails closed: where the screener failed or no configuration of the role
 * {@code screener} qualifies, no model reads the source. The operator tries the screening again,
 * removes the source, or accepts it as untrusted with a reason that is recorded.
 */
public enum UnscreenedDisposition {

    /** Run the screener again over the stored copy. */
    TRY_AGAIN("try_again"),

    /** Remove the source; nothing of it is used. */
    REMOVE("remove"),

    /** Accept the source as untrusted, with a reason that is recorded. */
    ACCEPT_UNTRUSTED("accept_untrusted");

    private final String wireValue;

    UnscreenedDisposition(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * @return the wire value used in representations
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * @return {@code true} if the disposition stores a reason that is recorded
     */
    public boolean recordsReason() {
        return this == ACCEPT_UNTRUSTED;
    }

    /**
     * Resolves a disposition from its wire value, ignoring case.
     *
     * @param value the wire value
     * @return the matching disposition, or {@code null} if none matches
     */
    public static UnscreenedDisposition fromWireOrNull(String value) {
        if (value == null) {
            return null;
        }
        for (var disposition : values()) {
            if (disposition.wireValue.equalsIgnoreCase(value.trim())) {
                return disposition;
            }
        }
        return null;
    }

    /**
     * Resolves a disposition from its wire value, ignoring case.
     *
     * @param value the wire value
     * @return the matching disposition
     * @throws IllegalArgumentException if no disposition carries that value (fail-closed)
     */
    public static UnscreenedDisposition fromWire(String value) {
        var disposition = fromWireOrNull(value);
        if (disposition == null) {
            throw new IllegalArgumentException("Unknown unscreened disposition: " + value);
        }
        return disposition;
    }

    /**
     * @return the wire values of every disposition, in catalogue order
     */
    public static List<String> wireValues() {
        return Arrays.stream(values()).map(UnscreenedDisposition::wireValue).toList();
    }
}
