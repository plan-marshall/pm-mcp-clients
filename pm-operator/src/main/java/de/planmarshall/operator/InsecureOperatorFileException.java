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

import java.io.IOException;
import java.io.Serial;
import java.nio.file.Path;

/**
 * Thrown when an operator state file or directory violates security constraints
 * (symbolic links, insecure permissions, wrong file type).
 */
public class InsecureOperatorFileException extends IOException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient Path path;

    /**
     * @param path    the offending path
     * @param message the detail message
     */
    public InsecureOperatorFileException(Path path, String message) {
        super(path + " " + message);
        this.path = path;
    }

    /**
     * @return the offending path
     */
    public Path path() {
        return path;
    }
}
