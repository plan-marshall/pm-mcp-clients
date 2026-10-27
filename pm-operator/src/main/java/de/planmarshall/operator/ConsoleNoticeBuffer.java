/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.message.MessageCatalogue;

/**
 * Holds the notices that arrive while a full-screen view is open
 * (doc/design/terminal-client/15-overview.adoc#full-screen-live).
 *
 * <p>A notice printed above the prompt while the alternate screen is active would corrupt the
 * view. The console therefore offers every notice here first: while the buffer is active the
 * notice is only counted, and when the view leaves, one coalesced line says what arrived
 * meanwhile, the {@code operator.overview.meanwhile} text. A line that is not a counted notice —
 * the pairing confirmation and a fresh code — is kept verbatim and printed after that line.
 *
 * <p>The buffer is used from the event-stream thread and from the console thread. The whole
 * transition — accepting a notice and draining the buffer when the view leaves — is guarded by one
 * lock, so a notice cannot be accepted after the drain has read the counters and then be dropped.
 */
public final class ConsoleNoticeBuffer {

    /** The message parameter name of the place the operator was in, shared by its three uses. */
    private static final String WHERE = "where";

    /** What kind of item arrived while the view was open. */
    public enum Category {
        /** A decision or waiver request. */
        DECISION,
        /** A report. */
        REPORT,
        /** Anything else that waits for the operator. */
        NEED
    }

    /**
     * What the buffer held when the view left.
     *
     * @param summary the localized meanwhile line, empty when nothing was held
     * @param lines   the lines kept verbatim, in arrival order
     */
    public record Drained(String summary, List<String> lines) {

        /**
         * @param summary the meanwhile line
         * @param lines   the verbatim lines
         */
        public Drained {
            Objects.requireNonNull(summary, "summary");
            lines = List.copyOf(lines);
        }
    }

    private boolean active;
    private int decisions;
    private int reports;
    private int needs;
    private final List<String> deferred = new ArrayList<>();

    /** Opens the buffer: from now on notices are held instead of printed. */
    public synchronized void activate() {
        active = true;
    }

    /** Closes the buffer: notices are printed again. The held items stay until drained. */
    public synchronized void deactivate() {
        active = false;
    }

    /** @return {@code true} while a full-screen view is open */
    public synchronized boolean isActive() {
        return active;
    }

    /**
     * Offers one notice to the buffer.
     *
     * @param category the kind of item
     * @return {@code true} when the notice was held and must not be printed; {@code false} when the
     *         caller is to print it
     */
    public boolean hold(Category category) {
        return hold(category, 1);
    }

    /**
     * Offers {@code count} notices of one kind to the buffer.
     *
     * @param category the kind of item
     * @param count    how many items the notice represents, at least one
     * @return {@code true} when the notices were held
     */
    public synchronized boolean hold(Category category, int count) {
        Objects.requireNonNull(category, "category");
        if (!active) {
            return false;
        }
        int items = Math.max(1, count);
        switch (category) {
            case DECISION -> decisions += items;
            case REPORT -> reports += items;
            case NEED -> needs += items;
        }
        return true;
    }

    /**
     * Keeps a line verbatim while the view is open.
     *
     * @param line the line to print after the view leaves
     * @return {@code true} when the line was held; {@code false} when the caller is to print it
     */
    public synchronized boolean holdLine(String line) {
        if (!active) {
            return false;
        }
        deferred.add(Objects.requireNonNull(line, "line"));
        return true;
    }

    /**
     * @return {@code true} when at least one item or line was held since the last drain
     */
    public synchronized boolean hasHeldNotices() {
        return decisions > 0 || reports > 0 || needs > 0 || !deferred.isEmpty();
    }

    /**
     * Builds the one line of what arrived meanwhile, returns the verbatim lines, and clears the
     * buffer in the same transition.
     *
     * @param locale the interface locale
     * @return the meanwhile summary and the verbatim lines
     */
    public synchronized Drained drain(String locale) {
        return drain(locale, overviewPlace(locale));
    }

    /**
     * Builds the one line of what arrived while the operator was at {@code where}, returns the
     * verbatim lines, and clears the buffer in the same transition.
     *
     * @param locale the interface locale
     * @param where  the localized place the operator was in, as the view's own header names it
     * @return the meanwhile summary and the verbatim lines
     */
    public synchronized Drained drain(String locale, String where) {
        Objects.requireNonNull(where, WHERE);
        var parts = new ArrayList<String>();
        if (decisions > 0) {
            parts.add(MessageCatalogue.format(locale, "operator.overview.meanwhile.decisions",
                                Map.of("count", decisions)));
        }
        if (reports > 0) {
            parts.add(MessageCatalogue.format(locale, "operator.overview.meanwhile.reports",
                                Map.of("count", reports)));
        }
        if (needs > 0) {
            parts.add(MessageCatalogue.format(locale, "operator.overview.meanwhile.needs",
                                Map.of("count", needs)));
        }
        var lines = List.copyOf(deferred);
        var summary = summarize(locale, parts, where);
        decisions = 0;
        reports = 0;
        needs = 0;
        deferred.clear();
        return new Drained(summary, lines);
    }

    /** Clears the held items without printing them. */
    public synchronized void clear() {
        decisions = 0;
        reports = 0;
        needs = 0;
        deferred.clear();
    }

    /**
     * @param locale the interface locale
     * @param parts  the already localized item summaries
     * @param where  the localized place the operator was in
     * @return the composed meanwhile line, or an empty string when there is nothing to say
     */
    public static String summarize(String locale, List<String> parts, String where) {
        if (parts == null || parts.isEmpty()) {
            return "";
        }
        return meanwhile(locale, where, String.join(", ", parts));
    }

    /**
     * The one line a view leaves behind when nothing arrived meanwhile: the same sentence as the
     * summary, so the operator always reads where they were and what is new, also when nothing is
     * ({@code doc/design/terminal-client/15-overview.adoc#full-screen-live}).
     *
     * @param locale the interface locale
     * @param where  the localized place the operator was in, as the view's own header names it
     * @return the line to print after leaving the view
     */
    public static String leavingLine(String locale, String where) {
        var events = MessageCatalogue.format(locale, "operator.overview.meanwhile.nothing", Map.of());
        return meanwhile(locale, where, events);
    }

    /**
     * @param locale the interface locale
     * @param where  the localized place the operator was in
     * @param events the localized "what arrived" part of the line
     * @return the meanwhile line
     */
    private static String meanwhile(String locale, String where, String events) {
        Objects.requireNonNull(where, WHERE);
        return MessageCatalogue.format(locale, "operator.overview.meanwhile",
                            Map.of(WHERE, where, "events", events));
    }

    /**
     * @param locale the interface locale
     * @return the place the overview itself names, used when no level names a narrower one
     */
    public static String overviewPlace(String locale) {
        return MessageCatalogue.format(locale, "operator.overview.where.overview", Map.of());
    }
}
