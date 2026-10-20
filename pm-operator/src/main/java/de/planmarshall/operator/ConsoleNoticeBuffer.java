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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import de.planmarshall.api.message.MessageCatalogue;

/**
 * Holds the notices that arrive while a full-screen view is open
 * (doc/design/terminal-client/15-overview.adoc#full-screen-live).
 *
 * <p>A notice printed above the prompt while the alternate screen is active would corrupt the
 * view. The console therefore offers every notice here first: while the buffer is active the
 * notice is only counted, and when the view leaves, one coalesced line says what arrived
 * meanwhile, the {@code operator.overview.meanwhile} text.
 *
 * <p>The buffer is safe to use from the event-stream thread and from the view's thread: the
 * counters are atomic, and reading and clearing them happens at the moment the view leaves.
 */
public final class ConsoleNoticeBuffer {

    /** What kind of item arrived while the view was open. */
    public enum Category {
        /** A decision or waiver request. */
        DECISION,
        /** A report. */
        REPORT,
        /** Anything else that waits for the operator. */
        NEED
    }

    private final AtomicBoolean active = new AtomicBoolean(false);
    private final AtomicInteger decisions = new AtomicInteger();
    private final AtomicInteger reports = new AtomicInteger();
    private final AtomicInteger needs = new AtomicInteger();

    /** Opens the buffer: from now on notices are held instead of printed. */
    public void activate() {
        active.set(true);
    }

    /** Closes the buffer: notices are printed again. The held counts stay until drained. */
    public void deactivate() {
        active.set(false);
    }

    /** @return {@code true} while a full-screen view is open */
    public boolean isActive() {
        return active.get();
    }

    /**
     * Offers a notice to the buffer.
     *
     * @param category the kind of item
     * @return {@code true} when the notice was held and must not be printed; {@code false} when the
     *         caller is to print it
     */
    public boolean hold(Category category) {
        Objects.requireNonNull(category, "category");
        if (!active.get()) {
            return false;
        }
        switch (category) {
            case DECISION -> decisions.incrementAndGet();
            case REPORT -> reports.incrementAndGet();
            case NEED -> needs.incrementAndGet();
        }
        return true;
    }

    /**
     * @return {@code true} when at least one notice was held since the last drain
     */
    public boolean hasHeldNotices() {
        return decisions.get() > 0 || reports.get() > 0 || needs.get() > 0;
    }

    /**
     * Builds the one line of what arrived meanwhile and clears the buffer.
     *
     * @param locale the interface locale
     * @return the line, or an empty string when nothing arrived
     */
    public String drainSummary(String locale) {
        int decisionCount = decisions.getAndSet(0);
        int reportCount = reports.getAndSet(0);
        int needCount = needs.getAndSet(0);
        var parts = new ArrayList<String>();
        if (decisionCount > 0) {
            parts.add(MessageCatalogue.format(locale, "operator.overview.meanwhile.decisions",
                                Map.of("count", decisionCount)));
        }
        if (reportCount > 0) {
            parts.add(MessageCatalogue.format(locale, "operator.overview.meanwhile.reports",
                                Map.of("count", reportCount)));
        }
        if (needCount > 0) {
            parts.add(MessageCatalogue.format(locale, "operator.overview.meanwhile.needs",
                                Map.of("count", needCount)));
        }
        return summarize(locale, parts);
    }

    /** Clears the held notices without printing them. */
    public void clear() {
        decisions.set(0);
        reports.set(0);
        needs.set(0);
    }

    /**
     * @param locale the interface locale
     * @param parts  the already localized item summaries
     * @return the composed meanwhile line, or an empty string when there is nothing to say
     */
    public static String summarize(String locale, List<String> parts) {
        if (parts == null || parts.isEmpty()) {
            return "";
        }
        var events = String.join(", ", parts);
        return MessageCatalogue.format(locale, "operator.overview.meanwhile",
                            Map.of("events", events));
    }
}
