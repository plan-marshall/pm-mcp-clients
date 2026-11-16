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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Collectors;

import de.planmarshall.api.changes.PlanChangeFile;
import de.planmarshall.api.changes.PlanChangesAnswer;
import de.planmarshall.api.overview.ChangeSize;
import de.planmarshall.operator.tui.TuiRow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ChangesLevel")
class ChangesLevelTest {

    /** The escape, the line separator and a bidi override a worker or the project could plant. */
    private static final String ESCAPE = "\u001b[2J";
    private static final String LINE_SEPARATOR = "\u2028";
    private static final String BIDI_OVERRIDE = "\u202e";

    private static final String LIST = "{\"plan_id\":\"fix-relay\",\"state\":\"captured\","
                        + "\"head\":\"9f3c2ab41d7e\",\"base_ref\":\"main\",\"files\":["
                        + "{\"path\":\"pm-relay/src/main/java/de/planmarshall/relay/Transport.java\","
                        + "\"status\":\"modified\",\"added\":48,\"removed\":12,\"binary\":false,\"unplanned\":false},"
                        + "{\"path\":\"pm-relay/src/test/java/de/planmarshall/relay/TransportTest.java\","
                        + "\"status\":\"added\",\"added\":96,\"removed\":0,\"binary\":false,\"unplanned\":false},"
                        + "{\"path\":\"pm-relay/src/main/java/de/planmarshall/relay/Frame.java\","
                        + "\"renamed_from\":\"pm-relay/src/main/java/de/planmarshall/relay/Packet.java\","
                        + "\"status\":\"renamed\",\"added\":3,\"removed\":3,\"binary\":false,\"unplanned\":false},"
                        + "{\"path\":\"doc/resources/relay.png\",\"status\":\"modified\",\"added\":0,\"removed\":0,"
                        + "\"binary\":true,\"unplanned\":true}]}";

    private static final String NOT_CAPTURED = "{\"plan_id\":\"fix-relay\",\"state\":\"not_captured\"}";

    private static final String UNAVAILABLE = "{\"plan_id\":\"fix-relay\",\"state\":\"unavailable\","
                        + "\"reason\":\"the captured commit 4c1d7e0 is no longer in the repository\"}";

    private static final String FILE = "{\"path\":\"pm-relay/src/main/java/de/planmarshall/relay/Frame.java\","
                        + "\"hunks\":[{\"old_start\":20,\"old_len\":2,\"new_start\":20,\"new_len\":2,\"lines\":["
                        + "{\"kind\":\"context\",\"text\":\"    private final String name;\"},"
                        + "{\"kind\":\"removed\",\"text\":\"    // the packet \\u2028name\"},"
                        + "{\"kind\":\"added\",\"text\":\"\\u001b[2J    // the frame \\u202ename\"}]}],"
                        + "\"binary\":false,\"truncated\":false}";

    private static final String CUT_FILE = "{\"path\":\"pm-relay/src/main/java/de/planmarshall/relay/Frame.java\","
                        + "\"hunks\":[{\"old_start\":20,\"old_len\":1,\"new_start\":20,\"new_len\":1,\"lines\":["
                        + "{\"kind\":\"context\",\"text\":\"    private final String name;\"}]}],"
                        + "\"binary\":false,\"truncated\":true,\"lines_omitted\":120}";

    private static ChangesLevel level() throws Exception {
        return new ChangesLevel("fix-relay", PlanChangesAnswer.fromJson(LIST), new ChangeSize(4, 147, 15, 1));
    }

    private static String text(ChangesLevel level) {
        return level.rows("en").stream().map(TuiRow::line).collect(Collectors.joining("\n"));
    }

    @Nested
    @DisplayName("The list")
    class TheList {

        @Test
        @DisplayName("the head names the plan, the capture's short head and the plan's own figures")
        void breadcrumb() throws Exception {
            var level = level();

            assertAll(
                                () -> assertEquals("fix-relay \u203a changes \u00b7 at 9f3c2ab \u00b7 4 files \u00b7 +147 \u221215"
                                                    + " \u00b7 1 unplanned", level.breadcrumb("en")),
                                () -> assertEquals("fix-relay \u203a \u00c4nderungen \u00b7 bei 9f3c2ab \u00b7 4 Dateien"
                                                    + " \u00b7 +147 \u221215 \u00b7 1 ungeplant", level.breadcrumb("de")));
        }

        @Test
        @DisplayName("without a size of change the head ends at the capture, without a dangling separator")
        void breadcrumbWithoutSize() throws Exception {
            var level = new ChangesLevel("fix-relay", PlanChangesAnswer.fromJson(LIST), null);

            assertEquals("fix-relay \u203a changes \u00b7 at 9f3c2ab", level.breadcrumb("en"));
        }

        @Test
        @DisplayName("one row per file, with its mark, its counts, the path it came from and its marks")
        void rows() throws Exception {
            var rows = level().rows("en");

            assertAll(
                                () -> assertEquals(4, rows.size()),
                                () -> assertTrue(rows.get(0).line().startsWith("M  pm-relay/\u2026/relay/Transport.java"),
                                                    rows.get(0).line()),
                                () -> assertTrue(rows.get(0).line().endsWith("+48 \u221212"), rows.get(0).line()),
                                () -> assertTrue(rows.get(1).line().startsWith("A  pm-relay/\u2026/relay/TransportTest.java"),
                                                    rows.get(1).line()),
                                () -> assertTrue(rows.get(1).line().endsWith("+96"), rows.get(1).line()),
                                () -> assertTrue(rows.get(2).line().startsWith("R  "), rows.get(2).line()),
                                () -> assertTrue(rows.get(2).line().contains("\u2026/Frame.java"), rows.get(2).line()),
                                () -> assertTrue(rows.get(2).line().contains("\u2026/relay/\u2026"),
                                                    "a renamed file names where it came from"),
                                () -> assertFalse(rows.get(2).line().contains("\u2026/Frame.java \u2190 \u2026/Frame.java"),
                                                    "a move inside the project never names itself as its origin"),
                                () -> assertTrue(rows.get(3).line().startsWith("B  doc/resources/relay.png"),
                                                    rows.get(3).line()),
                                () -> assertTrue(rows.get(3).line().contains("binary"), rows.get(3).line()),
                                () -> assertTrue(rows.get(3).line().contains("unplanned"), rows.get(3).line()));
        }

        @Test
        @DisplayName("only a file with hunks opens, never a binary one")
        void opens() throws Exception {
            var level = level();

            assertAll(
                                () -> assertEquals("pm-relay/src/main/java/de/planmarshall/relay/Transport.java",
                                                    level.fileOf(ChangesLevel.FILE_PREFIX
                                                                        + "pm-relay/src/main/java/de/planmarshall/relay/Transport.java")),
                                () -> assertNull(level.fileOf(ChangesLevel.FILE_PREFIX + "doc/resources/relay.png"),
                                                    "a binary file answers no lines"),
                                () -> assertNull(level.fileOf("summary:plan")));
        }
    }

    @Nested
    @DisplayName("A file")
    class AFile {

        @Test
        @DisplayName("its hunks open below its row, with their header and their markers")
        void hunks() throws Exception {
            var level = level();

            assertTrue(level.showFile(PlanChangeFile.fromJson(FILE)));
            var text = text(level);

            assertAll(
                                () -> assertTrue(level.hasOpenFile()),
                                () -> assertTrue(text.contains("@@ -20,2 +20,2 @@"), text),
                                () -> assertTrue(text.contains("\u2212    // the packet name"), text),
                                () -> assertTrue(text.contains("+    // the frame name"), text),
                                () -> assertTrue(text.contains("     private final String name;"), text),
                                () -> assertEquals(4 + 4, level.rows("en").size(), "the list, plus the file's four lines"));
        }

        @Test
        @DisplayName("a cut file says how many lines the runtime left out")
        void truncated() throws Exception {
            var level = level();

            assertTrue(level.showFile(PlanChangeFile.fromJson(CUT_FILE)));
            var text = text(level);

            assertAll(
                                () -> assertTrue(text.contains("120 more lines not shown"), text),
                                () -> assertTrue(text.contains("@@ -20,1 +20,1 @@"), text));
        }

        @Test
        @DisplayName("closing the file leaves the list")
        void closes() throws Exception {
            var level = level();
            level.showFile(PlanChangeFile.fromJson(FILE));

            assertAll(
                                () -> assertTrue(level.closeFile()),
                                () -> assertFalse(level.closeFile(), "there is nothing left to close"),
                                () -> assertEquals(4, level.rows("en").size()));
        }

        @Test
        @DisplayName("a file the list does not name opens nothing")
        void refusesAnUnknownPath() throws Exception {
            var level = level();
            var elsewhere = FILE.replace("relay/Frame.java", "relay/Nowhere.java");

            assertAll(
                                () -> assertFalse(level.showFile(PlanChangeFile.fromJson(elsewhere))),
                                () -> assertEquals(4, level.rows("en").size()));
        }

        @Test
        @DisplayName("the scrollback section is the file's row and its hunks, never the whole list")
        void openFileRows() throws Exception {
            var level = level();
            level.showFile(PlanChangeFile.fromJson(FILE));

            var rows = level.openFileRows("en");

            assertAll(
                                () -> assertEquals(5, rows.size()),
                                () -> assertTrue(rows.getFirst().line().startsWith("R  "), rows.getFirst().line()),
                                () -> assertTrue(rows.get(1).line().startsWith("@@"), rows.get(1).line()));
        }
    }

    @Nested
    @DisplayName("When there is nothing to list")
    class NothingToShow {

        @Test
        @DisplayName("not_captured prints its text, never an empty list")
        void notCaptured() throws Exception {
            var level = new ChangesLevel("fix-relay", PlanChangesAnswer.fromJson(NOT_CAPTURED), null);

            assertAll(
                                () -> assertEquals(1, level.rows("en").size()),
                                () -> assertEquals("No changes captured yet", level.rows("en").getFirst().line()),
                                () -> assertEquals("Noch keine \u00c4nderungen erfasst", level.rows("de").getFirst().line()),
                                () -> assertEquals("fix-relay", level.breadcrumb("en"),
                                                    "a refusal has no head to name"));
        }

        @Test
        @DisplayName("unavailable prints its text, never an empty list")
        void unavailable() throws Exception {
            var level = new ChangesLevel("fix-relay", PlanChangesAnswer.fromJson(UNAVAILABLE), null);

            assertAll(
                                () -> assertEquals(1, level.rows("en").size()),
                                () -> assertEquals("The changes are no longer in the repository",
                                                    level.rows("en").getFirst().line()),
                                () -> assertEquals("fix-relay", level.breadcrumb("en"),
                                                    "a refusal has no head to name"));
        }
    }

    @Nested
    @DisplayName("Live")
    class Live {

        @Test
        @DisplayName("a new capture keeps an open file whose row did not change, and names the new head")
        void keepsAnUnchangedRow() throws Exception {
            var level = level();
            level.showFile(PlanChangeFile.fromJson(FILE));
            var fresh = LIST.replace("\"head\":\"9f3c2ab41d7e\"", "\"head\":\"a1b2c3d4e5f6\"");

            assertAll(
                                () -> assertFalse(level.update(PlanChangesAnswer.fromJson(fresh)),
                                                    "the open file's row did not change"),
                                () -> assertTrue(level.hasOpenFile()),
                                () -> assertTrue(text(level).contains("@@ -20,2 +20,2 @@"), text(level)),
                                () -> assertTrue(level.breadcrumb("en").contains("a1b2c3d4e5f"),
                                                    level.breadcrumb("en")));
        }

        @Test
        @DisplayName("a new capture whose row changed asks for the file again")
        void asksForAChangedRow() throws Exception {
            var level = level();
            level.showFile(PlanChangeFile.fromJson(FILE));
            var fresh = LIST.replace("\"status\":\"renamed\",\"added\":3,\"removed\":3",
                                "\"status\":\"renamed\",\"added\":5,\"removed\":3");

            assertAll(
                                () -> assertTrue(level.update(PlanChangesAnswer.fromJson(fresh)),
                                                    "the open file's row changed"),
                                () -> assertFalse(level.hasOpenFile(), "the hunks wait for the read"),
                                () -> assertEquals(4, level.rows("en").size(), "the list is shown again"));
        }

        @Test
        @DisplayName("a capture that lapses shows the refusal's text, never the stale list")
        void refusalAfterACapture() throws Exception {
            var level = level();

            level.update(PlanChangesAnswer.fromJson(UNAVAILABLE));

            assertAll(
                                () -> assertEquals(1, level.rows("en").size()),
                                () -> assertEquals("The changes are no longer in the repository",
                                                    level.rows("en").getFirst().line()),
                                () -> assertEquals("fix-relay", level.breadcrumb("en")));
        }
    }

    @Nested
    @DisplayName("Untrusted text")
    class UntrustedText {

        @Test
        @DisplayName("a control sequence in a path is stripped")
        void inAPath() throws Exception {
            var planted = LIST.replace("relay/Transport.java",
                                "relay/\\u001B[2JTransport\\u2028.java");
            var level = new ChangesLevel("fix-relay", PlanChangesAnswer.fromJson(planted),
                                new ChangeSize(4, 147, 15, 1));

            var text = text(level);

            assertAll(
                                () -> assertFalse(text.contains(ESCAPE), text),
                                () -> assertFalse(text.contains(LINE_SEPARATOR), text),
                                () -> assertTrue(text.contains("relay/Transport.java"), text));
        }

        @Test
        @DisplayName("a control sequence and a bidi override in a line are stripped, and its indent stays")
        void inALine() throws Exception {
            var level = level();
            level.showFile(PlanChangeFile.fromJson(FILE));

            var text = text(level);

            assertAll(
                                () -> assertFalse(text.contains(ESCAPE), text),
                                () -> assertFalse(text.contains(LINE_SEPARATOR), text),
                                () -> assertFalse(text.contains(BIDI_OVERRIDE), text),
                                () -> assertTrue(text.contains("+    // the frame name"), text));
        }
    }
}
