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
 * The closed list of the causes a source of a start fails with (State Catalogue, Source Units;
 * design <em>Starting Work</em>, the sources).
 * <p>
 * The wire value is the one of the message key {@code start.cause.<wire>}. The list is closed and
 * fails closed: a cause read from the wire that is not a member is refused by
 * {@link #fromWire(String)}.
 * <p>
 * {@link #RATE_LIMITED} carries the instant the limit resets and is <em>never</em> retried by the
 * client or the server on its own: only the operator's <em>Try again</em> after that instant, or a
 * credential for the forge, fetches the source again. {@link #NO_LOCAL_CHANGES} offers no retry at
 * all, because the checkout's change set is what it is.
 */
public enum SourceFailureCause {

    /** A file is not readable. */
    NOT_READABLE("not_readable"),

    /** A file is larger than the compiled bound. */
    TOO_LARGE("too_large"),

    /** A directory entry is a link that leads outside the chosen root. */
    LINK_OUTSIDE("link_outside"),

    /** The issue or pull request does not exist. */
    NOT_FOUND("not_found"),

    /** The operator refused the grant to read another repository. */
    NOT_GRANTED("not_granted"),

    /** The forge has no credential and the issue is not public. */
    NO_CREDENTIAL("no_credential"),

    /** The forge refused the fetch for its rate limit; {@code resets_at} names the reset. */
    RATE_LIMITED("rate_limited"),

    /** The provider could not be reached. */
    PROVIDER_UNAVAILABLE("provider_unavailable"),

    /** The reference names no forge this machine knows. */
    UNKNOWN_FORGE("unknown_forge"),

    /** The gathering was cancelled. */
    CANCELLED("cancelled"),

    /** The path is outside the checkout and the operator's home directory. */
    OUTSIDE_ALLOWED("outside_allowed"),

    /** The screener failed. */
    SCREENING_FAILED("screening_failed"),

    /** No configuration of the role {@code screener} qualifies. */
    NO_SCREENER("no_screener"),

    /** The pull request is not open. */
    NOT_OPEN("not_open"),

    /** The pull request's head branch is not writable by the agent identity. */
    HEAD_NOT_WRITABLE("head_not_writable"),

    /** A lesson was claimed by another plan meanwhile. */
    LESSON_CLAIMED("lesson_claimed"),

    /** A lesson was retired meanwhile. */
    LESSON_RETIRED("lesson_retired"),

    /** The lesson store is damaged. */
    LESSON_STORE_DAMAGED("lesson_store_damaged"),

    /** The checkout has no local changes any more. */
    NO_LOCAL_CHANGES("no_local_changes");

    private final String wireValue;

    SourceFailureCause(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * @return the wire value used in representations and message keys
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * @return {@code true} if the operator may try the gathering again from this cause
     */
    public boolean retryable() {
        return this != NO_LOCAL_CHANGES;
    }

    /**
     * @return {@code true} if the cause is a rate limit that names when it resets
     */
    public boolean namesReset() {
        return this == RATE_LIMITED;
    }

    /**
     * Resolves a cause from its wire value, ignoring case.
     *
     * @param value the wire value
     * @return the matching cause, or {@code null} if none matches
     */
    public static SourceFailureCause fromWireOrNull(String value) {
        if (value == null) {
            return null;
        }
        for (var cause : values()) {
            if (cause.wireValue.equalsIgnoreCase(value.trim())) {
                return cause;
            }
        }
        return null;
    }

    /**
     * Resolves a cause from its wire value, ignoring case.
     *
     * @param value the wire value
     * @return the matching cause
     * @throws IllegalArgumentException if no cause carries that value (fail-closed)
     */
    public static SourceFailureCause fromWire(String value) {
        var cause = fromWireOrNull(value);
        if (cause == null) {
            throw new IllegalArgumentException("Unknown source failure cause: " + value);
        }
        return cause;
    }

    /**
     * @return the wire values of every cause, in catalogue order
     */
    public static List<String> wireValues() {
        return Arrays.stream(values()).map(SourceFailureCause::wireValue).toList();
    }
}
