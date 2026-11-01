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

import java.util.List;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.control.HoldChangedEvent;
import de.planmarshall.api.overview.NeedsOperatorChangedEvent;
import de.planmarshall.api.overview.NeedsOperatorKind;
import de.planmarshall.api.report.ReportSentEvent;

/**
 * The one table of event names and the notice each of them becomes (CLI-UC-P-04): the console
 * renders the notice of the kind it names, and a second client — the web app — is meant to read
 * the same list instead of matching event names of its own. That alignment is not part of this
 * change: the web app still matches event names itself.
 *
 * <p>A client resolves the kind of a streamed event by its <em>event name</em> first — the name in
 * the SSE {@code event} field, a member of {@link EventName} — and falls back to the {@code type}
 * member of the event's {@code data} only while the runtime sends it. An event that names no kind
 * here produces no notice; it may still change what waits for the operator
 * ({@link NeedsOperatorChangedEvent}), which is the prompt badge's source and not a notice.
 *
 * <p>{@link #HOLD} is the one family: {@code hold.opened} names no notice itself, so the client
 * resolves the {@code kind} member of the hold's payload through {@link #fromType} — the hold kinds
 * are exactly the operator-wait kinds of {@link NeedsOperatorKind}.
 *
 * <p>{@link #LANDING_FAILED} accepts the event name {@code landing.failed}, which no member of
 * {@link EventName} carries today: the runtime reports a failed landing as a hold of kind
 * {@code landing_failed} ({@code hold.opened}). The name is kept because the event contract of
 * CLI-UC-P-04 names it.
 */
public enum EventKind {

    /**
     * A decision or a waiver request opened; the envelope's {@code is_waiver} selects the notice.
     */
    QUESTION("question", List.of("question", "decision"), List.of(EventName.DECISION_OPENED.wire())),

    /** A waiver request named by the legacy {@code type} member alone. */
    WAIVER_REQUEST("waiver_request", List.of("waiver_request"), List.of()),

    /**
     * A decision was answered in another channel. The console prints it only for a channel that is
     * not its own; see the console's own rendering rules.
     */
    ANSWERED_ELSEWHERE("answered_elsewhere", List.of("answered_elsewhere"),
            List.of(EventName.DECISION_ANSWERED.wire())),

    /** A device paired. */
    PAIRED("paired", List.of("paired"), List.of()),

    /** A fresh pairing code was issued. */
    FRESH_CODE("fresh_code", List.of("fresh_code"), List.of()),

    /** A plan or epic waits for the operator. */
    OPERATOR_WAIT("operator_wait", List.of("operator_wait"), List.of()),

    /** A plan paused for an integrity reason. */
    INTEGRITY_PAUSE(NeedsOperatorKind.INTEGRITY_PAUSE.value(), List.of("integrity_pause"), List.of()),

    /** A plan is stalled. */
    STALLED(NeedsOperatorKind.STALLED.value(), List.of("stalled"), List.of()),

    /** A plan needs a fix. */
    CONDITION(NeedsOperatorKind.CONDITION.value(), List.of("condition"), List.of(EventName.CONDITION_RAISED.wire())),

    /** Epic work is held for the operator. */
    HELD(NeedsOperatorKind.HELD.value(), List.of("held"), List.of()),

    /** An epic's landing failed. */
    LANDING_FAILED(NeedsOperatorKind.LANDING_FAILED.value(), List.of("landing_failed"), List.of("landing.failed")),

    /** New findings arrived. */
    FINDINGS(NeedsOperatorKind.FINDINGS.value(), List.of("findings"), List.of()),

    /** A project's setup needs the operator. */
    SETUP(NeedsOperatorKind.SETUP.value(), List.of("setup"), List.of()),

    /** A report was written: one quiet line, never the bell. */
    REPORT("report", List.of("report", "report.sent"), List.of(ReportSentEvent.EVENT_NAME)),

    /** A hold opened; its notice is the hold's own kind, read from the payload. */
    HOLD("hold", List.of("hold"), List.of(HoldChangedEvent.EVENT_OPENED));

    private final String notice;
    private final List<String> types;
    private final List<String> events;

    EventKind(String notice, List<String> types, List<String> events) {
        this.notice = notice;
        this.types = types;
        this.events = events;
    }

    /**
     * @return the notice kind of the operator's console, or {@link #HOLD} for the hold family
     */
    public String notice() {
        return notice;
    }

    /**
     * @return the {@code type} values the legacy event data names this kind by
     */
    public List<String> types() {
        return types;
    }

    /**
     * @return the SSE event names of this kind
     */
    public List<String> events() {
        return events;
    }

    /**
     * @param eventName the SSE event name, or {@code null}
     * @return the kind the event name names, or {@code null} when it names none
     */
    public static @Nullable EventKind fromEventName(@Nullable String eventName) {
        return lookup(eventName, true);
    }

    /**
     * @param type the {@code type} member of the event data, or a hold's {@code kind}
     * @return the kind the value names, or {@code null} when it names none
     */
    public static @Nullable EventKind fromType(@Nullable String type) {
        return lookup(type, false);
    }

    private static @Nullable EventKind lookup(@Nullable String value, boolean byEventName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (var kind : values()) {
            if ((byEventName ? kind.events : kind.types).contains(value)) {
                return kind;
            }
        }
        return null;
    }
}
