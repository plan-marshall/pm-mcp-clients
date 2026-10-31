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
 * The reports of the full-screen view: the unread reports grouped by project, epic, and plan
 * ({@code doc/design/terminal-client/24-reports.adoc#full-screen}, UC-PLN-6).
 *
 * <p>The levels here are models only: they turn what the runtime serves into rows and never reach
 * the runtime themselves. The view stack, the keys, and the terminal belong to
 * {@code de.planmarshall.operator.overview} and {@code de.planmarshall.operator.tui}.
 */
@NullMarked
package de.planmarshall.operator.reports;

import org.jspecify.annotations.NullMarked;
