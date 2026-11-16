/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.planview;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.changes.PlanChangeFile;
import de.planmarshall.api.changes.PlanChangeHunk;
import de.planmarshall.api.changes.PlanChangeLineKind;
import de.planmarshall.api.changes.PlanChangedFile;
import de.planmarshall.api.changes.PlanChangesAnswer;
import de.planmarshall.api.changes.PlanChangesCaptured;
import de.planmarshall.api.changes.PlanChangesNotCaptured;
import de.planmarshall.api.changes.PlanChangesUnavailable;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.overview.ChangeSize;
import de.planmarshall.operator.tui.TuiLayout;
import de.planmarshall.operator.tui.TuiRow;

/**
 * The plan's changes as a level of the view: the list of the last capture, and a file's hunks
 * below the row that was opened ({@code doc/design/terminal-client/16-plan-view.adoc#changes},
 * {@code doc/design/shared/plan-view.adoc#changes}).
 *
 * <p>Nothing is computed here but the text: the head's figures are the plan's own size of change
 * ({@link ChangeSize}), never a sum over the rows, and every path and line was written by a worker
 * or comes from the project, so each is stripped of its control sequences before it becomes a row.
 */
public final class ChangesLevel {

    /** Prefix of the row id of one changed file. */
    public static final String FILE_PREFIX = "changes:file:";
    /** The row id of a refusal: the view says so instead of showing an empty list. */
    public static final String REFUSAL_ROW_ID = "changes:refusal";
    private static final String HUNK_PREFIX = "changes:hunk:";
    private static final String LINE_PREFIX = "changes:line:";
    private static final String TRUNCATED_ROW_ID = "changes:truncated";
    private static final String HUNK_HEADER = "@@ -";
    private static final String HUNK_MARKER = " @@";
    private static final String RANGE = ",";
    private static final String REMOVED = "\u2212";
    private static final String ADDED = "+";
    private static final String CONTEXT = " ";
    private static final String MARK_ADDED = "A";
    private static final String MARK_MODIFIED = "M";
    private static final String MARK_DELETED = "D";
    private static final String MARK_RENAMED = "R";
    private static final String MARK_BINARY = "B";
    private static final String ARROW = " \u2190 ";
    private static final String MARK_SEPARATOR = "  ";
    private static final String COUNT_SEPARATOR = "  ";
    /** The columns the path takes before the counts, so the counts stand in one column. */
    private static final int PATH_WIDTH = 44;
    private static final int SHORT_HEAD = 7;

    private final String planId;
    private final PlanChangesAnswer answer;
    private final @Nullable ChangeSize size;
    private @Nullable String openPath;
    private @Nullable PlanChangeFile openFile;

    /**
     * @param planId the plan whose changes these are
     * @param answer the answer the runtime served
     * @param size   the size of change the plan carries, or {@code null} when it carries none
     */
    public ChangesLevel(String planId, PlanChangesAnswer answer, @Nullable ChangeSize size) {
        this.planId = Objects.requireNonNull(planId, "planId");
        this.answer = Objects.requireNonNull(answer, "answer");
        this.size = size;
    }

    /** @return the plan id */
    public String planId() {
        return planId;
    }

    /** @return the answer the runtime served */
    public PlanChangesAnswer answer() {
        return answer;
    }

    /** @return {@code true} when the plan has an open file */
    public boolean hasOpenFile() {
        return openPath != null;
    }

    /**
     * @param locale the interface locale
     * @return the header: the plan, and the list's title with the head and the plan's figures
     */
    public String breadcrumb(String locale) {
        return PlanViewFormatting.clean(planId) + PlanViewFormatting.PATH_SEPARATOR + title(locale);
    }

    /**
     * @param locale the interface locale
     * @return the list, a file's hunks below its row when it was opened, or the refusal's text
     */
    public List<TuiRow> rows(String locale) {
        if (answer instanceof PlanChangesNotCaptured) {
            return List.of(TuiRow.leaf(REFUSAL_ROW_ID,
                                MessageCatalogue.format(locale, "plan.changes.not_captured", Map.of())));
        }
        if (answer instanceof PlanChangesUnavailable) {
            return List.of(TuiRow.leaf(REFUSAL_ROW_ID,
                                MessageCatalogue.format(locale, "plan.changes.unavailable", Map.of())));
        }
        var captured = (PlanChangesCaptured) answer;
        var rows = new ArrayList<TuiRow>();
        for (var file : captured.files()) {
            rows.add(fileRow(file, locale));
            if (openPath != null && openPath.equals(file.path()) && openFile != null) {
                appendHunkRows(rows, openFile, locale);
            }
        }
        return List.copyOf(rows);
    }

    /**
     * @param rowId the selected row, or {@code null}
     * @return the path of the file the row names, or {@code null} when the row is no file that
     *         holds hunks
     */
    public @Nullable String fileOf(@Nullable String rowId) {
        if (rowId == null || !rowId.startsWith(FILE_PREFIX) || !(answer instanceof PlanChangesCaptured)) {
            return null;
        }
        var path = rowId.substring(FILE_PREFIX.length());
        var file = ((PlanChangesCaptured) answer).files().stream()
                            .filter(candidate -> candidate.path().equals(path))
                            .findFirst().orElse(null);
        // A binary file answers no lines: its row shows `binary` and opens nothing.
        return file != null && !file.binary() ? path : null;
    }

    /**
     * Shows a file's hunks below its row. A file the list does not name is ignored: the level shows
     * only what its own list holds.
     *
     * @param file the file the runtime answered
     * @return {@code true} when the file belongs to the list and is now the open one
     */
    public boolean showFile(PlanChangeFile file) {
        Objects.requireNonNull(file, "file");
        if (!(answer instanceof PlanChangesCaptured captured)) {
            return false;
        }
        var known = captured.files().stream().anyMatch(entry -> entry.path().equals(file.path()));
        if (!known) {
            return false;
        }
        openPath = file.path();
        openFile = file;
        return true;
    }

    /**
     * @return {@code true} when a file's hunks were closed
     */
    public boolean closeFile() {
        if (openPath == null) {
            return false;
        }
        openPath = null;
        openFile = null;
        return true;
    }

    /**
     * @return the row of the open file, or {@code null} when no file is open
     */
    public @Nullable String openFileRowId() {
        return openPath != null ? FILE_PREFIX + openPath : null;
    }

    /**
     * The section the scrollback prints for one file: its row and its hunks, never the whole list
     * ({@code plan <id> --changes --file <path>}).
     *
     * @param locale the interface locale
     * @return the open file's row and hunks, or an empty list when no file is open
     */
    public List<TuiRow> openFileRows(String locale) {
        if (openPath == null || openFile == null || !(answer instanceof PlanChangesCaptured captured)) {
            return List.of();
        }
        var rows = new ArrayList<TuiRow>();
        captured.files().stream()
                            .filter(file -> file.path().equals(openPath))
                            .findFirst()
                            .ifPresent(file -> rows.add(fileRow(file, locale)));
        appendHunkRows(rows, openFile, locale);
        return List.copyOf(rows);
    }

    /**
     * @param locale the interface locale
     * @return the list's title: {@code changes · at <head> · <size>}, without the size when the
     *         plan carries none
     */
    private String title(String locale) {
        var params = new java.util.LinkedHashMap<String, Object>();
        params.put("head", shortHead());
        params.put("size", sizeText(locale));
        var title = MessageCatalogue.format(locale, "plan.changes.title", params);
        if (size != null) {
            return title;
        }
        // The plan carries no figures: the title ends at the head, without a dangling separator.
        var separator = PlanViewFormatting.DOT_SEPARATOR;
        return title.endsWith(separator) ? title.substring(0, title.length() - separator.length()).strip() : title;
    }

    private String shortHead() {
        if (!(answer instanceof PlanChangesCaptured captured)) {
            return "";
        }
        var head = PlanViewFormatting.clean(captured.head());
        return head.length() <= SHORT_HEAD ? head : head.substring(0, SHORT_HEAD);
    }

    private String sizeText(String locale) {
        return size != null ? MessageCatalogue.format(locale, "plan.change.size", size.toMap()) : "";
    }

    private TuiRow fileRow(PlanChangedFile file, String locale) {
        var line = new StringBuilder();
        line.append(mark(file)).append(MARK_SEPARATOR);
        line.append(inColumn(pathCell(file))).append(COUNT_SEPARATOR);
        if (file.binary()) {
            line.append(MessageCatalogue.format(locale, "plan.changes.binary", Map.of()));
        } else {
            line.append(ADDED).append(file.added());
            if (file.removed() > 0) {
                line.append(' ').append(REMOVED).append(file.removed());
            }
        }
        if (file.unplanned()) {
            line.append(MARK_SEPARATOR).append(MessageCatalogue.format(locale, "plan.changes.unplanned", Map.of()));
        }
        var row = TuiRow.openable(FILE_PREFIX + file.path(), line.toString(), null);
        return file.binary() ? TuiRow.leaf(FILE_PREFIX + file.path(), line.toString()) : row;
    }

    /**
     * @param cell the path cell
     * @return the cell padded to the column, and never cut: a rename's two paths stay whole
     */
    private static String inColumn(String cell) {
        return TuiLayout.displayWidth(cell) < PATH_WIDTH ? TuiLayout.pad(cell, PATH_WIDTH) : cell;
    }

    private static String pathCell(PlanChangedFile file) {
        var renamedFrom = file.renamedFrom();
        if (renamedFrom != null) {
            // Both names stay visible where the path column is narrow: the names it carries.
            return abbreviatedName(file.path()) + ARROW + abbreviatedName(renamedFrom);
        }
        return abbreviatedPath(file.path());
    }

    /**
     * @param path the file's path
     * @return the path as it fits the column: its first segment, an ellipsis, and its last two
     */
    private static String abbreviatedPath(String path) {
        var safe = TuiLayout.sanitizeCell(path);
        var segments = safe.split("/");
        if (segments.length <= 3) {
            return safe;
        }
        return segments[0] + "/\u2026/" + segments[segments.length - 2] + "/" + segments[segments.length - 1];
    }

    /**
     * @param path the file's path
     * @return its last segment with an ellipsis in front, so both paths of a rename fit the column
     */
    private static String abbreviatedName(String path) {
        var safe = TuiLayout.sanitizeCell(path);
        var cut = safe.lastIndexOf('/');
        return cut < 0 ? safe : "\u2026/" + safe.substring(cut + 1);
    }

    private static String mark(PlanChangedFile file) {
        if (file.binary()) {
            return MARK_BINARY;
        }
        return switch (file.status()) {
            case ADDED -> MARK_ADDED;
            case MODIFIED -> MARK_MODIFIED;
            case DELETED -> MARK_DELETED;
            case RENAMED -> MARK_RENAMED;
        };
    }

    private void appendHunkRows(List<TuiRow> rows, PlanChangeFile file, String locale) {
        int hunkIndex = 0;
        for (var hunk : file.hunks()) {
            rows.add(TuiRow.leaf(HUNK_PREFIX + hunkIndex, hunkHeader(hunk)));
            int lineIndex = 0;
            for (var line : hunk.lines()) {
                rows.add(TuiRow.leaf(LINE_PREFIX + hunkIndex + ":" + lineIndex++,
                                    marker(line.kind()) + TuiLayout.sanitizeCell(line.text())));
            }
            hunkIndex++;
        }
        if (file.truncated()) {
            rows.add(TuiRow.leaf(TRUNCATED_ROW_ID, MessageCatalogue.format(locale, "plan.changes.truncated",
                                Map.of("lines", file.linesOmitted()))));
        }
    }

    private static String hunkHeader(PlanChangeHunk hunk) {
        return HUNK_HEADER + hunk.oldStart() + RANGE + hunk.oldLen()
                            + " +" + hunk.newStart() + RANGE + hunk.newLen() + HUNK_MARKER;
    }

    private static String marker(PlanChangeLineKind kind) {
        return switch (kind) {
            case ADDED -> ADDED;
            case REMOVED -> REMOVED;
            case CONTEXT -> CONTEXT;
        };
    }
}
