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

/**
 * The programmatic entry of {@code pm-operator} for the transcript and dialogue tests
 * (PM-TEST-7). Those tests live in {@link de.planmarshall.operator.transcript} and cannot reach
 * the package-private {@link PmOperator#execute}; this test-scope runner forwards to it, so the
 * command line is assembled exactly as the binary assembles it.
 */
public final class TranscriptVerbRunner {

    private TranscriptVerbRunner() {
    }

    /**
     * @param context the operator context, usually one a {@code TerminalTestDriver} supplied
     * @param out     standard output
     * @param err     standard error
     * @param args    the arguments of the verb
     * @return the exit code
     */
    public static int run(OperatorContext context, PrintWriter out, PrintWriter err, String... args) {
        return PmOperator.execute(context, out, err, args);
    }
}
