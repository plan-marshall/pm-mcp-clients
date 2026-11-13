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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Reads the catalogue keys a design document defines, so a catalogue test derives the keys it
 * checks from the document instead of keeping a second list by hand: every cell of a table of the
 * document ({@code |===} … {@code |===}) that holds exactly one key in backticks
 * ({@code |`overview.needs.waiver`}), in every table of the document, in the document's order.
 *
 * <p>The documents are read from the repository beside the module (the {@code basedir} property
 * Maven sets, else the working directory), at {@code ../doc/design/<relative>}.
 */
final class SpecificationTables {

    private static final String TABLE_DELIMITER = "|===";
    private static final Pattern KEY_CELL = Pattern.compile("^\\|`([a-z][a-z0-9_]*(?:\\.[a-z0-9_]+)+)`$");

    private SpecificationTables() {
    }

    /**
     * @param relative the document's path below {@code doc/design/}
     * @return the keys of every key cell of every table of the document, in its order
     * @throws IOException when the document cannot be read
     */
    static List<String> keyCells(String relative) throws IOException {
        var keys = new ArrayList<String>();
        var inTable = false;
        for (var line : Files.readAllLines(document(relative), StandardCharsets.UTF_8)) {
            var stripped = line.strip();
            if (TABLE_DELIMITER.equals(stripped)) {
                inTable = !inTable;
            } else if (inTable) {
                var matcher = KEY_CELL.matcher(stripped);
                if (matcher.matches()) {
                    keys.add(matcher.group(1));
                }
            }
        }
        return keys;
    }

    private static Path document(String relative) {
        var basedir = System.getProperty("basedir");
        var module = basedir != null ? Path.of(basedir) : Path.of("").toAbsolutePath();
        return module.resolve("../doc/design/" + relative).normalize();
    }
}
