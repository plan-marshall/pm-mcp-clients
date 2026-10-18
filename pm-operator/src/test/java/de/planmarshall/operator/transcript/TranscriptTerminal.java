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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.jline.terminal.Size;
import org.jline.terminal.Sized;
import org.jline.terminal.impl.DumbTerminal;

/**
 * The terminal a transcript test drives: a JLine terminal over the scripted input and the captured
 * output of {@link TerminalTestDriver}, pinned to the 80x24 grid the transcripts are recorded at so
 * that line wrapping never depends on the machine that runs the test. Its terminal type is
 * {@code dumb}, so JLine does not paint cursor movements into the transcript.
 */
public final class TranscriptTerminal extends DumbTerminal {

    /** The width of a transcript, in columns. */
    public static final int COLUMNS = 80;

    /** The height of a transcript, in rows. */
    public static final int ROWS = 24;

    private static final String NAME = "pm-transcript";
    private static final String TYPE = "dumb";

    TranscriptTerminal(InputStream input, OutputStream output) throws IOException {
        super(NAME, TYPE, input, output, StandardCharsets.UTF_8);
    }

    @Override
    public Size getSize() {
        return Size.of(COLUMNS, ROWS);
    }

    @Override
    public void setSize(Sized size) {
        // pinned: a transcript is always recorded at COLUMNS x ROWS
    }
}
