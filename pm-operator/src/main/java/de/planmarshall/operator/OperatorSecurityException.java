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

import java.io.Serial;

/**
 * Thrown when an operator command fails the operator gate checks: no attached interactive
 * terminal, or a coding-agent environment signal was detected.
 */
public final class OperatorSecurityException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int exitCode;

    /**
     * @param message diagnostic message
     */
    public OperatorSecurityException(String message) {
        this(message, OperatorGate.EXIT_OPERATOR_CHECKS);
    }

    /**
     * @param message  diagnostic message
     * @param exitCode process exit code to return
     */
    public OperatorSecurityException(String message, int exitCode) {
        super(message);
        this.exitCode = exitCode;
    }

    /**
     * @return the exit code
     */
    public int exitCode() {
        return exitCode;
    }
}
