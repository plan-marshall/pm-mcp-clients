/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.event;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * The closed list of the streams an event is sent on, as the Event Catalogue of the Runtime Model
 * specification names them ({@code 06-event-catalogue.adoc#event-catalogue}): the runtime-wide stream,
 * a plan's, an epic's, a project's workspace scope's, the machine scope's, an entry's, the reports
 * stream and a task's activity stream. Each {@link EventName} declares the streams it is sent on
 * through {@link EventName#streams()}.
 * <p>
 * The six streams of a scope are tied to their {@link EventScopeKind} ({@link #scopeKind()},
 * {@link #of(EventScopeKind)}), so the two lists cannot drift apart unnoticed.
 * <p>
 * The list is closed and fails closed: a letter that is not a member is refused by
 * {@link #fromWire(String)}.
 */
public enum EventStream {

    /** The runtime-wide stream. */
    RUNTIME("R", EventScopeKind.RUNTIME),

    /** A plan's stream. */
    PLAN("P", EventScopeKind.PLAN),

    /** An epic's stream. */
    EPIC("E", EventScopeKind.EPIC),

    /** A project's workspace scope's stream. */
    WORKSPACE("W", EventScopeKind.WORKSPACE),

    /** The machine scope's stream. */
    MACHINE("M", EventScopeKind.MACHINE),

    /** An entry's stream. */
    ENTRY("N", EventScopeKind.ENTRY),

    /** The reports stream. */
    REPORTS("Rep", null),

    /** A task's activity stream. */
    ACTIVITY("A", null);

    private final String wire;
    private final @Nullable EventScopeKind scopeKind;

    EventStream(String wire, @Nullable EventScopeKind scopeKind) {
        this.wire = wire;
        this.scopeKind = scopeKind;
    }

    /**
     * @return the letter the catalogue names the stream by
     */
    public String wire() {
        return wire;
    }

    /**
     * @return the kind of scope this stream is the stream of, or {@code null} for the reports and the
     *         activity stream, which belong to no {@link EventScopeKind}
     */
    public @Nullable EventScopeKind scopeKind() {
        return scopeKind;
    }

    /**
     * @param kind a kind of scope
     * @return the stream of that kind of scope; every {@link EventScopeKind} has exactly one
     */
    public static EventStream of(EventScopeKind kind) {
        Objects.requireNonNull(kind, "kind");
        for (var stream : values()) {
            if (stream.scopeKind == kind) {
                return stream;
            }
        }
        throw new IllegalStateException("No event stream for scope kind " + kind.wire());
    }

    /**
     * @param wire the letter, may be {@code null}
     * @return the stream, or {@code null} when the letter names none
     */
    public static @Nullable EventStream fromWireOrNull(@Nullable String wire) {
        if (wire == null) {
            return null;
        }
        for (var stream : values()) {
            if (stream.wire.equals(wire)) {
                return stream;
            }
        }
        return null;
    }

    /**
     * @param wire the letter
     * @return the stream
     * @throws IllegalArgumentException if the letter names no stream (fail-closed)
     */
    public static EventStream fromWire(String wire) {
        Objects.requireNonNull(wire, "wire");
        var stream = fromWireOrNull(wire);
        if (stream == null) {
            throw new IllegalArgumentException("Unknown event stream: " + wire);
        }
        return stream;
    }

    /**
     * @return the letters of every stream, in catalogue order
     */
    public static List<String> wireValues() {
        return Arrays.stream(values()).map(EventStream::wire).toList();
    }
}
