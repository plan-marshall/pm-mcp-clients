/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.message;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Checks the chat's texts of the {@code control} and {@code decisions} packs against the key tables of the
 * design, read from the repository: {@code doc/design/shared/work-actions-chat-texts.adoc#texts} for every
 * {@code control.chat.*} key and {@code doc/design/shared/decisions-texts.adoc#texts} for every
 * {@code decision.chat.*} key. The table texts are the expectation, so they are compared verbatim.
 */
@DisplayName("Chat texts: the packs hold the design tables' keys with their texts verbatim")
class ChatTextsCatalogueTest {

    private static final Path DESIGN = moduleDirectory().resolve("../doc/design/shared").normalize();
    private static final Path CHAT_TEXTS = DESIGN.resolve("work-actions-chat-texts.adoc");
    private static final Path DECISION_TEXTS = DESIGN.resolve("decisions-texts.adoc");

    private static final Pattern KEY_CELL = Pattern.compile("^\\|`([a-z0-9_.]+)`$");
    private static final String TABLE_DELIMITER = "|===";

    private record Row(String key, String english, String german) {
    }

    private static Path moduleDirectory() {
        var basedir = System.getProperty("basedir");
        return basedir != null ? Path.of(basedir) : Path.of("").toAbsolutePath();
    }

    /**
     * Reads the rows of the first table of the document whose key starts with the prefix: a key cell
     * {@code |`key`} followed by the English and the German cell.
     */
    private static List<Row> rows(Path document, String prefix) throws IOException {
        var lines = Files.readAllLines(document, StandardCharsets.UTF_8);
        var cells = new ArrayList<String>();
        var inTable = false;
        for (var line : lines) {
            if (TABLE_DELIMITER.equals(line.strip())) {
                if (inTable) {
                    break;
                }
                inTable = true;
            } else if (inTable && line.startsWith("|")) {
                cells.add(line);
            }
        }
        var rows = new ArrayList<Row>();
        for (var i = 0; i + 2 < cells.size(); i++) {
            var matcher = KEY_CELL.matcher(cells.get(i));
            if (matcher.matches() && matcher.group(1).startsWith(prefix)) {
                rows.add(new Row(matcher.group(1), cells.get(i + 1).substring(1), cells.get(i + 2).substring(1)));
                i += 2;
            }
        }
        return rows;
    }

    static Stream<Arguments> controlRows() throws IOException {
        return rows(CHAT_TEXTS, "control.chat.").stream().map(row -> Arguments.of(row.key(), row));
    }

    static Stream<Arguments> decisionRows() throws IOException {
        return rows(DECISION_TEXTS, "decision.chat.").stream().map(row -> Arguments.of(row.key(), row));
    }

    private static void assertRow(String area, Row row) {
        var en = MessageCatalogue.getMessages("en", area);
        var de = MessageCatalogue.getMessages("de", area);

        assertAll(row.key(),
                () -> assertEquals(row.english(), en.get(row.key()), "English text of " + row.key()),
                () -> assertEquals(row.german(), de.get(row.key()), "German text of " + row.key()));
    }

    @Test
    @DisplayName("both tables are read and yield rows, so an unreadable table never passes")
    void tablesYieldRows() throws IOException {
        assertAll(
                () -> assertTrue(Files.isRegularFile(CHAT_TEXTS), CHAT_TEXTS.toString()),
                () -> assertTrue(Files.isRegularFile(DECISION_TEXTS), DECISION_TEXTS.toString()),
                () -> assertFalse(rows(CHAT_TEXTS, "control.chat.").isEmpty(), "no control.chat.* row read"),
                () -> assertFalse(rows(DECISION_TEXTS, "decision.chat.").isEmpty(), "no decision.chat.* row read"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("controlRows")
    @DisplayName("each control.chat.* key of the table is in the en and de packs with the table's text")
    void controlChatKey(String key, Row row) {
        assertRow("control", row);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("decisionRows")
    @DisplayName("each decision.chat.* key of the table is in the en and de packs with the table's text")
    void decisionChatKey(String key, Row row) {
        assertRow("decisions", row);
    }

    @Test
    @DisplayName("the control packs hold no control.chat.* key the table does not name")
    void noControlChatKeyBeyondTheTable() throws IOException {
        var tableKeys = new TreeSet<String>();
        rows(CHAT_TEXTS, "control.chat.").forEach(row -> tableKeys.add(row.key()));

        for (var locale : List.of("en", "de")) {
            var packKeys = new TreeSet<String>();
            MessageCatalogue.getMessages(locale, "control").keySet().stream()
                    .filter(key -> key.startsWith("control.chat.")).forEach(packKeys::add);
            assertEquals(tableKeys, packKeys, locale);
        }
    }
}
