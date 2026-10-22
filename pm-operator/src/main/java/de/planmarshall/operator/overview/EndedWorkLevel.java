/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.overview;

import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.overview.EndedWorkItem;
import de.planmarshall.api.overview.EndedWorkPage;
import de.planmarshall.operator.tui.TuiLevel;
import de.planmarshall.operator.tui.TuiRow;

/**
 * Ended work level of the full-screen overview ({@code doc/design/terminal-client/15-overview.adoc#ended-work}).
 * Supports incremental auto-pagination on scroll.
 */
public final class EndedWorkLevel {

    /** Empty ended work row ID. */
    public static final String EMPTY_ENDED_ROW_ID = "__ended_empty__";

    private static final String DOT_SEPARATOR = " \u00b7 ";

    private final String projectId;
    private final List<EndedWorkItem> items = new ArrayList<>();
    private int currentPage;
    private int totalPages;
    private boolean hasOlder;

    /**
     * @param initial the first page response
     */
    public EndedWorkLevel(EndedWorkPage initial) {
        Objects.requireNonNull(initial, "initial");
        this.projectId = initial.projectId();
        this.currentPage = initial.page();
        this.totalPages = initial.totalPages();
        this.hasOlder = initial.hasOlder();
        this.items.addAll(initial.items());
    }

    /** @return project ID of this ended work level */
    public String projectId() {
        return projectId;
    }

    /** @return accumulated items across all loaded pages */
    public List<EndedWorkItem> items() {
        return List.copyOf(items);
    }

    /** @return current page number */
    public int currentPage() {
        return currentPage;
    }

    /** @return total pages reported by the runtime */
    public int totalPages() {
        return totalPages;
    }

    /** @return {@code true} if older pages remain on the server */
    public boolean hasOlder() {
        return hasOlder;
    }

    /**
     * Appends a newly fetched older page of items.
     *
     * @param olderPage the older page response
     */
    public void appendOlderPage(EndedWorkPage olderPage) {
        Objects.requireNonNull(olderPage, "olderPage");
        this.currentPage = olderPage.page();
        this.totalPages = olderPage.totalPages();
        this.hasOlder = olderPage.hasOlder();
        for (var item : olderPage.items()) {
            if (items.stream().noneMatch(existing -> existing.id().equals(item.id()))) {
                items.add(item);
            }
        }
    }

    /**
     * Refreshes the first page of items, preserving any previously loaded older pages.
     *
     * @param freshPage page 1 response
     */
    public void refreshFirstPage(EndedWorkPage freshPage) {
        Objects.requireNonNull(freshPage, "freshPage");
        this.totalPages = Math.max(this.totalPages, freshPage.totalPages());
        if (this.currentPage <= 1) {
            this.hasOlder = freshPage.hasOlder();
        }
        var existingById = new LinkedHashMap<String, EndedWorkItem>();
        for (var item : freshPage.items()) {
            existingById.put(item.id(), item);
        }
        for (var item : items) {
            existingById.putIfAbsent(item.id(), item);
        }
        items.clear();
        items.addAll(existingById.values());
    }

    /**
     * Creates the {@link TuiLevel} representation.
     *
     * @param locale target locale
     * @return the ended work level
     */
    public TuiLevel level(String locale) {
        var title = MessageCatalogue.format(locale, "overview.ended.title", Map.of())
                            + DOT_SEPARATOR
                            + OverviewFormatting.clean(projectId);
        return TuiLevel.of(TuiLevel.Kind.ENDED, projectId, title);
    }

    /**
     * Builds the visible {@link TuiRow} items.
     *
     * @param locale      target locale
     * @param clock       reference clock
     * @param helpVisible whether the help overlay is active (to reveal plain PR URLs)
     * @return list of TUI rows
     */
    public List<TuiRow> rows(String locale, Clock clock, boolean helpVisible) {
        if (items.isEmpty()) {
            var emptyText = MessageCatalogue.format(locale, "overview.ended.empty",
                                Map.of("project", OverviewFormatting.clean(projectId)));
            return List.of(new TuiRow(EMPTY_ENDED_ROW_ID, emptyText, null, false, false));
        }

        return items.stream()
                            .map(item -> formatItemRow(locale, clock, helpVisible, item))
                            .toList();
    }

    private static TuiRow formatItemRow(String locale, Clock clock, boolean helpVisible, EndedWorkItem item) {
        boolean isEpic = "epic".equalsIgnoreCase(item.kind());
        var kindText = formatKind(locale, isEpic);
        var title = OverviewFormatting.clean(item.title());
        var outcomeText = formatOutcome(locale, item, isEpic);
        var endedTime = OverviewFormatting.formatEndedTime(locale, item.endedAt(), clock);
        var prText = item.pullRequestNumber() != null ? "#" + item.pullRequestNumber() : null;
        var right = formatRight(endedTime, prText);

        var line = String.format("%s  %-36s %-12s %s", kindText, title, outcomeText, right).stripTrailing();
        var details = formatDetails(item, helpVisible);

        return new TuiRow(item.id(), line, details, false, true);
    }

    private static String formatKind(String locale, boolean isEpic) {
        var glyph = isEpic ? OverviewFormatting.EPIC_GLYPH : OverviewFormatting.PLAN_GLYPH;
        var key = isEpic ? "overview.kind.epic" : "overview.kind.plan";
        return glyph + " " + MessageCatalogue.format(locale, key, Map.of());
    }

    private static String formatOutcome(String locale, EndedWorkItem item, boolean isEpic) {
        if (isEpic) {
            var rawReason = item.terminalReason() != null
                                ? item.terminalReason().trim().toLowerCase(Locale.ROOT).replace('-', '_')
                                : "completed";
            var reason = OverviewFormatting.clean(rawReason);
            return MessageCatalogue.format(locale, "overview.ended.epic_end", Map.of("end", reason));
        }
        var rawOutcome = item.outcome() != null
                            ? item.outcome().trim().toLowerCase(Locale.ROOT).replace('-', '_')
                            : "completed";
        var outcome = OverviewFormatting.clean(rawOutcome);
        return MessageCatalogue.format(locale, "overview.ended.outcome", Map.of("outcome", outcome));
    }

    private static String formatRight(String endedTime, @Nullable String prText) {
        if (prText != null && !endedTime.isEmpty()) {
            return prText + DOT_SEPARATOR + endedTime;
        }
        if (prText != null) {
            return prText;
        }
        return endedTime;
    }

    private static @Nullable String formatDetails(EndedWorkItem item, boolean helpVisible) {
        var detailParts = new ArrayList<String>();
        if (item.reason() != null && !item.reason().isBlank()) {
            detailParts.add(OverviewFormatting.clean(item.reason()));
        }
        if (helpVisible && item.pullRequestUrl() != null && !item.pullRequestUrl().isBlank()) {
            detailParts.add(OverviewFormatting.clean(item.pullRequestUrl()));
        }
        return detailParts.isEmpty() ? null : String.join(DOT_SEPARATOR, detailParts);
    }
}
