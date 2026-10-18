/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.transcript;

import java.util.ArrayList;
import java.util.List;

/**
 * A readable unified diff of a transcript: every line of the expected transcript that the
 * rendered buffer no longer contains is a {@code -} line, every added line a {@code +} line, and
 * three unchanged lines around each change give the reader the place. A drift is shown, never
 * applied.
 */
final class UnifiedDiff {

    private static final int CONTEXT = 3;
    private static final String OLD_FILE = "--- ";
    private static final String NEW_FILE = "+++ ";
    private static final String HUNK = "@@ ";

    private UnifiedDiff() {
    }

    /**
     * @param expectedName the name of the recorded transcript
     * @param expected     the recorded lines
     * @param actualName   the name of the rendered buffer
     * @param actual       the rendered lines
     * @return the unified diff, or an empty string when the lines are equal
     */
    static String between(String expectedName, List<String> expected, String actualName, List<String> actual) {
        return render(expectedName, actualName, walk(expected, actual));
    }

    private enum Kind {
        EQUAL, DELETE, INSERT
    }

    private record Edit(Kind kind, String text, int expectedLine, int actualLine) {
    }

    private static List<Edit> walk(List<String> expected, List<String> actual) {
        var n = expected.size();
        var m = actual.size();
        var lcs = new int[n + 1][m + 1];
        for (var i = n - 1; i >= 0; i--) {
            for (var j = m - 1; j >= 0; j--) {
                lcs[i][j] = expected.get(i).equals(actual.get(j))
                                    ? lcs[i + 1][j + 1] + 1
                                    : Math.max(lcs[i + 1][j], lcs[i][j + 1]);
            }
        }
        var edits = new ArrayList<Edit>();
        var i = 0;
        var j = 0;
        var expectedLine = 1;
        var actualLine = 1;
        while (i < n && j < m) {
            if (expected.get(i).equals(actual.get(j))) {
                edits.add(new Edit(Kind.EQUAL, expected.get(i), expectedLine++, actualLine++));
                i++;
                j++;
            } else if (lcs[i + 1][j] >= lcs[i][j + 1]) {
                edits.add(new Edit(Kind.DELETE, expected.get(i++), expectedLine++, actualLine));
            } else {
                edits.add(new Edit(Kind.INSERT, actual.get(j++), expectedLine, actualLine++));
            }
        }
        while (i < n) {
            edits.add(new Edit(Kind.DELETE, expected.get(i++), expectedLine++, actualLine));
        }
        while (j < m) {
            edits.add(new Edit(Kind.INSERT, actual.get(j++), expectedLine, actualLine++));
        }
        return edits;
    }

    private static String render(String expectedName, String actualName, List<Edit> edits) {
        var changed = new ArrayList<Integer>();
        for (var index = 0; index < edits.size(); index++) {
            if (edits.get(index).kind() != Kind.EQUAL) {
                changed.add(index);
            }
        }
        if (changed.isEmpty()) {
            return "";
        }
        var diff = new StringBuilder(OLD_FILE).append(expectedName).append('\n')
                            .append(NEW_FILE).append(actualName).append('\n');
        var hunkStart = Math.max(0, changed.getFirst() - CONTEXT);
        var hunkEnd = Math.min(edits.size() - 1, changed.getFirst() + CONTEXT);
        for (var index = 1; index < changed.size(); index++) {
            if (changed.get(index) - CONTEXT <= hunkEnd + 1) {
                hunkEnd = Math.min(edits.size() - 1, changed.get(index) + CONTEXT);
            } else {
                appendHunk(diff, edits, hunkStart, hunkEnd);
                hunkStart = Math.max(0, changed.get(index) - CONTEXT);
                hunkEnd = Math.min(edits.size() - 1, changed.get(index) + CONTEXT);
            }
        }
        appendHunk(diff, edits, hunkStart, hunkEnd);
        return diff.toString();
    }

    private static void appendHunk(StringBuilder diff, List<Edit> edits, int start, int end) {
        var first = edits.get(start);
        var expectedCount = 0;
        var actualCount = 0;
        for (var index = start; index <= end; index++) {
            if (edits.get(index).kind() != Kind.INSERT) {
                expectedCount++;
            }
            if (edits.get(index).kind() != Kind.DELETE) {
                actualCount++;
            }
        }
        diff.append(HUNK).append("@@ -").append(first.expectedLine()).append(',').append(expectedCount)
                            .append(" +").append(first.actualLine()).append(',').append(actualCount)
                            .append(" @@").append('\n');
        for (var index = start; index <= end; index++) {
            var edit = edits.get(index);
            diff.append(switch (edit.kind()) {
                case EQUAL -> ' ';
                case DELETE -> '-';
                case INSERT -> '+';
            }).append(edit.text()).append('\n');
        }
    }
}
