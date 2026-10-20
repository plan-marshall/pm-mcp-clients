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
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    private final String projectId;
    private final List<EndedWorkItem> items = new ArrayList<>();
    private int currentPage;
    private int totalPages;
    private boolean hasOlder;
    private boolean hasNewer;

    /**
     * @param initial the first page response
     */
    public EndedWorkLevel(EndedWorkPage initial) {
        Objects.requireNonNull(initial, "initial");
        this.projectId = initial.projectId();
        this.currentPage = initial.page();
        this.totalPages = initial.totalPages();
        this.hasOlder = initial.hasOlder();
        this.hasNewer = initial.hasNewer();
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
        this.hasNewer = olderPage.hasNewer();
        for (var item : olderPage.items()) {
            if (items.stream().noneMatch(existing -> existing.id().equals(item.id()))) {
                items.add(item);
            }
        }
    }

    /**
     * Creates the {@link TuiLevel} representation.
     *
     * @param locale target locale
     * @return the ended work level
     */
    public TuiLevel level(String locale) {
        var title = MessageCatalogue.format(locale, "overview.ended.title", Map.of())
                            + " \u00b7 "
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

        var rows = new ArrayList<TuiRow>(items.size());
        for (var item : items) {
            boolean isEpic = "epic".equalsIgnoreCase(item.kind());
            var kindText = (isEpic ? OverviewFormatting.EPIC_GLYPH : OverviewFormatting.PLAN_GLYPH)
                                + " "
                                + MessageCatalogue.format(locale, isEpic ? "overview.kind.epic" : "overview.kind.plan", Map.of());
            var title = OverviewFormatting.clean(item.title());

            String outcomeText;
            if (isEpic) {
                var reason = item.terminalReason() != null ? item.terminalReason() : "completed";
                outcomeText = MessageCatalogue.format(locale, "overview.ended.epic_end", Map.of("end", reason));
            } else {
                var outcome = item.outcome() != null ? item.outcome() : "completed";
                outcomeText = MessageCatalogue.format(locale, "overview.ended.outcome", Map.of("outcome", outcome));
            }

            var endedTime = OverviewFormatting.formatEndedTime(locale, item.endedAt(), clock);
            var prText = item.pullRequestNumber() != null ? "#" + item.pullRequestNumber() : null;

            String right;
            if (prText != null && !endedTime.isEmpty()) {
                right = prText + " \u00b7 " + endedTime;
            } else if (prText != null) {
                right = prText;
            } else {
                right = endedTime;
            }

            var line = String.format("%s  %-36s %-12s %s", kindText, title, outcomeText, right).stripTrailing();

            var detailParts = new ArrayList<String>();
            if (item.reason() != null && !item.reason().isBlank()) {
                detailParts.add(OverviewFormatting.clean(item.reason()));
            }
            if (helpVisible && item.pullRequestUrl() != null && !item.pullRequestUrl().isBlank()) {
                detailParts.add(OverviewFormatting.clean(item.pullRequestUrl()));
            }
            var details = detailParts.isEmpty() ? null : String.join(" \u00b7 ", detailParts);

            rows.add(new TuiRow(item.id(), line, details, false, true));
        }

        return rows;
    }
}
