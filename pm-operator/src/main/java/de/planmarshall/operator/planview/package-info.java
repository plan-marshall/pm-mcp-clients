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
 * The full-screen plan view of {@code pm-operator}
 * ({@code doc/design/terminal-client/16-plan-view.adoc}): the process tree with its expandable
 * phases, sub-workflows, leaves and tasks, the live worker activity of a task, and an ended plan's
 * completion report.
 *
 * <p>The classes here are view models: they turn the read-only process and activity resources of
 * {@code pm-api} into rows for the shared TUI stack. They never talk to the runtime themselves and
 * every external text passes through {@code OutputSafety} before it becomes a cell.
 */
@NullMarked
package de.planmarshall.operator.planview;

import org.jspecify.annotations.NullMarked;
