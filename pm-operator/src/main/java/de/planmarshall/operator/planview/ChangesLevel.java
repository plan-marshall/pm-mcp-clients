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
import java.util.LinkedHashMap;
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
import de.planmarshall.operator.OutputSafety;
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
    private static final String EMPTY_ROW_ID = "changes:none";
    private static final String HUNK_HEADER = "@@ -";
    private static final String HUNK_MARKER = " @@";
    private static final String RANGE = ",";
    private static final String REMOVED = "\u2212";
    private static final String ADDED = "+";
    private static final String CONTEXT = " ";
    /** The marker of a printed removed line: an ASCII {@code -}, so that the output feeds patch. */
    private static final String PIPED_REMOVED = "-";
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
    private PlanChangesAnswer answer;
    private @Nullable ChangeSize size;
    private @Nullable String openPath;
    private @Nullable PlanChangeFile openFile;
    private @Nullable PlanChangedFile openRow;

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

    /** @return {@code true} while a file's hunks are shown */
    public boolean hasOpenFile() {
        return openFile != null;
    }

    /**
     * @param locale the interface locale
     * @return the header: the plan, and the list's title with the head and the plan's figures; a
     *         refusal names the plan alone, having no head to name
     */
    public String breadcrumb(String locale) {
        if (!(answer instanceof PlanChangesCaptured)) {
            return PlanViewFormatting.clean(planId);
        }
        return PlanViewFormatting.clean(planId) + PlanViewFormatting.PATH_SEPARATOR + title(locale);
    }

    /**
     * @param locale the interface locale
     * @return the list, a file's hunks below its row when it was opened, or the refusal's text
     */
    public List<TuiRow> rows(String locale) {
        var state = stateRows(locale);
        if (state != null) {
            return state;
        }
        var captured = (PlanChangesCaptured) answer;
        if (captured.files().isEmpty()) {
            return List.of(emptyRow(locale));
        }
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
     * The list as {@code plan <id> --changes} prints it: each file's full, sanitised path, so that
     * the path a reader copies is the one {@code --file} accepts
     * ({@code doc/design/terminal-client/16-plan-view.adoc#changes}).
     *
     * @param locale the interface locale
     * @return the list in its printed form, or the refusal's text
     */
    public List<TuiRow> printableRows(String locale) {
        var state = stateRows(locale);
        if (state != null) {
            return state;
        }
        var captured = (PlanChangesCaptured) answer;
        if (captured.files().isEmpty()) {
            return List.of(emptyRow(locale));
        }
        var rows = new ArrayList<TuiRow>();
        for (var file : captured.files()) {
            rows.add(printableFileRow(file, locale));
        }
        return List.copyOf(rows);
    }

    /**
     * @param locale the interface locale
     * @return the state text of a refusal, or {@code null} when the answer holds a capture
     */
    private @Nullable List<TuiRow> stateRows(String locale) {
        if (answer instanceof PlanChangesNotCaptured) {
            return List.of(TuiRow.leaf(REFUSAL_ROW_ID,
                                MessageCatalogue.format(locale, "plan.changes.not_captured", Map.of())));
        }
        if (answer instanceof PlanChangesUnavailable) {
            return List.of(TuiRow.leaf(REFUSAL_ROW_ID,
                                MessageCatalogue.format(locale, "plan.changes.unavailable", Map.of())));
        }
        return null;
    }

    private static TuiRow emptyRow(String locale) {
        return TuiRow.leaf(EMPTY_ROW_ID, MessageCatalogue.format(locale, "plan.changes.none", Map.of()));
    }

    /**
     * Replaces the list after a capture ({@code plan-view.adoc} § Live): an open file stays open
     * while its row is unchanged, and its hunks are read again only when the row changed, or when an
     * earlier read did not land.
     *
     * @param fresh   the list the runtime answered after the capture
     * @param figures the plan's figures the capture's read answered; a read that did not land keeps
     *                the last figures, a landed read with none clears them
     * @return {@code true} when an open file's hunks must be read again
     */
    public boolean update(PlanChangesAnswer fresh, PlanChangeSizes.Probe figures) {
        var previousRow = openRow;
        var previousSize = size;
        this.answer = Objects.requireNonNull(fresh, "fresh");
        this.size = figures.landed() ? figures.size() : previousSize;
        if (openPath == null) {
            return false;
        }
        var row = answer instanceof PlanChangesCaptured captured ? rowOf(captured, openPath) : null;
        if (row == null) {
            // The file is gone from the list: the row and its hunks go with it.
            openPath = null;
            openFile = null;
            openRow = null;
            return true;
        }
        if (openFile != null && row.equals(previousRow)) {
            return false;
        }
        // The hunks wait for the read: a read that did not land is asked for again on the next capture.
        openFile = null;
        openRow = row;
        return true;
    }

    /**
     * @param path   the file's path
     * @param locale the interface locale
     * @return the row the list serves for the path, or {@code null} when it names none
     */
    public @Nullable TuiRow fileRowOf(String path, String locale) {
        if (!(answer instanceof PlanChangesCaptured captured)) {
            return null;
        }
        var row = rowOf(captured, path);
        return row != null ? fileRow(row, locale) : null;
    }

    /**
     * @param path   the file's path
     * @param locale the interface locale
     * @return the printed row the list serves for the path, or {@code null} when it names none
     */
    public @Nullable TuiRow printableFileRowOf(String path, String locale) {
        if (!(answer instanceof PlanChangesCaptured captured)) {
            return null;
        }
        var row = rowOf(captured, path);
        return row != null ? printableFileRow(row, locale) : null;
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
     * Shows a file's hunks below its row, while the list it was read against is still the one this
     * level holds. A capture that landed between the read and this call replaces the list, so the
     * older read is dropped rather than shown under the new rows
     * ({@code doc/design/terminal-client/16-plan-view.adoc#changes}).
     *
     * @param file              the file the runtime answered
     * @param answerReadAgainst the list that was held when the read started
     * @return {@code true} when the list is unchanged, names the file, and the file is now the open one
     */
    public boolean showFile(PlanChangeFile file, PlanChangesAnswer answerReadAgainst) {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(answerReadAgainst, "answerReadAgainst");
        if (answer != answerReadAgainst) {
            return false;
        }
        if (!(answer instanceof PlanChangesCaptured captured)) {
            return false;
        }
        var row = rowOf(captured, file.path());
        if (row == null) {
            return false;
        }
        openPath = file.path();
        openFile = file;
        openRow = row;
        return true;
    }

    /**
     * @param captured the list
     * @param path     the file's path
     * @return the row the list serves for the path, or {@code null} when it names none
     */
    private static @Nullable PlanChangedFile rowOf(PlanChangesCaptured captured, String path) {
        return captured.files().stream()
                            .filter(candidate -> candidate.path().equals(path))
                            .findFirst().orElse(null);
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
        openRow = null;
        return true;
    }

    /**
     * @return the row of the open file, or {@code null} when no file is open
     */
    public @Nullable String openFileRowId() {
        return openPath != null ? FILE_PREFIX + openPath : null;
    }

    /** @return the path of the open file, or {@code null} when no file is open */
    public @Nullable String openFilePath() {
        return openPath;
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
     * The section the scrollback prints for one file in its printed form: the file's full path and
     * its hunks with their own text, one ASCII marker and no indent, so that the output feeds
     * {@code patch} ({@code plan <id> --changes --file <path>}).
     *
     * @param locale the interface locale
     * @return the open file's row and hunks in their printed form, or an empty list when none is open
     */
    public List<TuiRow> openFilePrintableRows(String locale) {
        if (openPath == null || openFile == null || !(answer instanceof PlanChangesCaptured captured)) {
            return List.of();
        }
        var rows = new ArrayList<TuiRow>();
        captured.files().stream()
                            .filter(file -> file.path().equals(openPath))
                            .findFirst()
                            .ifPresent(file -> rows.add(printableFileRow(file, locale)));
        appendPrintableHunkRows(rows, openFile, locale);
        return List.copyOf(rows);
    }

    /**
     * @param locale the interface locale
     * @return the list's title: {@code changes · at <head> · <size>}, without the size when the
     *         plan carries none
     */
    private String title(String locale) {
        var params = new LinkedHashMap<String, Object>();
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

    /**
     * The printed form of a file's row: its full, sanitised path in the column, so that the path a
     * reader copies is the one {@code --file} accepts. A rename names the full path it came from
     * after the counts, so that the leading path stays the one the file route takes.
     *
     * @param file   the changed file
     * @param locale the interface locale
     * @return the row as the one-shot prints it
     */
    private TuiRow printableFileRow(PlanChangedFile file, String locale) {
        var line = new StringBuilder();
        line.append(mark(file)).append(MARK_SEPARATOR);
        line.append(inColumn(TuiLayout.sanitizeCell(file.path()))).append(COUNT_SEPARATOR);
        appendCounts(line, file, locale);
        var renamedFrom = file.renamedFrom();
        if (renamedFrom != null) {
            line.append(MARK_SEPARATOR).append(ARROW).append(TuiLayout.sanitizeCell(renamedFrom));
        }
        var text = line.toString();
        return file.binary() ? TuiRow.leaf(FILE_PREFIX + file.path(), text)
                            : TuiRow.openable(FILE_PREFIX + file.path(), text, null);
    }

    private void appendCounts(StringBuilder line, PlanChangedFile file, String locale) {
        if (file.binary()) {
            line.append(MessageCatalogue.format(locale, "plan.changes.binary", Map.of()));
            return;
        }
        line.append(ADDED).append(file.added());
        if (file.removed() > 0) {
            line.append(' ').append(REMOVED).append(file.removed());
        }
        if (file.unplanned()) {
            line.append(MARK_SEPARATOR).append(MessageCatalogue.format(locale, "plan.changes.unplanned", Map.of()));
        }
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
        if (renamedFrom == null) {
            return abbreviatedPath(file.path());
        }
        var fromName = abbreviatedName(renamedFrom);
        if (!fromName.equals(abbreviatedName(file.path()))) {
            // The names differ: both fit beside the counts.
            return abbreviatedName(file.path()) + ARROW + fromName;
        }
        // A move inside the project keeps the name: the row names the directory it came from.
        return abbreviatedName(file.path()) + ARROW + "\u2026/" + parentName(renamedFrom) + "/\u2026";
    }

    /**
     * @param path the file's path
     * @return the name of the directory that holds the file
     */
    private static String parentName(String path) {
        var safe = TuiLayout.sanitizeCell(path);
        var cut = safe.lastIndexOf('/');
        if (cut < 0) {
            return safe;
        }
        var parent = safe.substring(0, cut);
        var parentCut = parent.lastIndexOf('/');
        return parentCut < 0 ? parent : parent.substring(parentCut + 1);
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

    private void appendPrintableHunkRows(List<TuiRow> rows, PlanChangeFile file, String locale) {
        int hunkIndex = 0;
        for (var hunk : file.hunks()) {
            rows.add(TuiRow.leaf(HUNK_PREFIX + hunkIndex, hunkHeader(hunk)));
            int lineIndex = 0;
            for (var line : hunk.lines()) {
                rows.add(TuiRow.leaf(LINE_PREFIX + hunkIndex + ":" + lineIndex++,
                                    printableMarker(line.kind()) + pipedText(line.text())));
            }
            hunkIndex++;
        }
        if (file.truncated()) {
            rows.add(TuiRow.leaf(TRUNCATED_ROW_ID, MessageCatalogue.format(locale, "plan.changes.truncated",
                                Map.of("lines", file.linesOmitted()))));
        }
    }

    /**
     * @param text the line the runtime answered
     * @return the line's original text, its control sequences removed and its tabs kept, so that the
     *         printed hunk can be piped
     */
    private static String pipedText(String text) {
        return OutputSafety.stripControlSequences(text);
    }

    private static String printableMarker(PlanChangeLineKind kind) {
        return switch (kind) {
            case ADDED -> ADDED;
            case REMOVED -> PIPED_REMOVED;
            case CONTEXT -> CONTEXT;
        };
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
