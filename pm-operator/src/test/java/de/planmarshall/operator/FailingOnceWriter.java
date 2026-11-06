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

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * A console writer that fails once with an unchecked exception: the first text it is given that
 * contains the trigger is refused with an {@link IllegalStateException} carrying the given message;
 * every write before and after it reaches the sink. A verb's view that writes the trigger therefore
 * breaks with a failure that is no parse failure, so a test can tell a catch of the parse failure
 * from a catch that swallows every failure of the view.
 */
final class FailingOnceWriter extends PrintWriter {

    private final StringWriter sink;
    private final String trigger;
    private final String message;
    private boolean armed = true;

    /**
     * @param sink    the writer the accepted text goes to
     * @param trigger the text whose first write fails
     * @param message the message of the failure
     */
    FailingOnceWriter(StringWriter sink, String trigger, String message) {
        super(sink);
        this.sink = sink;
        this.trigger = trigger;
        this.message = message;
    }

    @Override
    public void write(String text, int offset, int length) {
        if (armed && text.substring(offset, offset + length).contains(trigger)) {
            armed = false;
            throw new IllegalStateException(message);
        }
        super.write(text, offset, length);
    }

    /**
     * @return the text the writer accepted so far
     */
    String accepted() {
        flush();
        return sink.toString();
    }
}
