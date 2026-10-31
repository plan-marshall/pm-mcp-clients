/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */

/**
 * The system view of the full-screen terminal client: the runtime, the work, the workers, the
 * jobs, the models, and the cleanup ({@code doc/design/terminal-client/25-system.adoc#full-screen},
 * UC-SYS-1..3).
 *
 * <p>{@link de.planmarshall.operator.system.SystemLevel} turns one snapshot the runtime served into
 * rows and keeps the operator's window, grouping, and project; it never reads the runtime itself.
 * {@link de.planmarshall.operator.system.SystemFormatting} holds the figure formatting the printed
 * and the full-screen view share.
 */
@NullMarked
package de.planmarshall.operator.system;

import org.jspecify.annotations.NullMarked;
