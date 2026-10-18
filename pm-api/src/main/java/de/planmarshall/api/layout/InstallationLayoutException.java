/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.layout;

import java.io.IOException;
import java.io.Serial;

/**
 * The installation layout cannot be resolved: the running executable is unknown, it does not lie in
 * the {@code bin/} directory of an installation, a symbolic link on its way is dangling, or the
 * installation directory is writable by group or others (PM-SEC-4).
 */
public class InstallationLayoutException extends IOException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * @param message the diagnostic
     */
    public InstallationLayoutException(String message) {
        super(message);
    }

    /**
     * @param message the diagnostic
     * @param cause   the cause
     */
    public InstallationLayoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
