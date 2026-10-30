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
 * The full-screen epic view of {@code pm-operator} ({@code doc/design/shared/epic-view.adoc},
 * {@code doc/design/terminal-client/19-actions.adoc#epic-view}, UC-PLN-5): the epic's head with its
 * state, mode, and scope; the workstreams and their plans with what each waits for; the sources,
 * landings, findings, watches, tokens, and events.
 *
 * <p>The classes here are view models: they turn the read-only epic resource of {@code pm-api} into
 * rows for the shared TUI stack, keep which nodes are open, and never talk to the runtime
 * themselves. Every external text passes through {@code OutputSafety} before it becomes a cell.
 */
@NullMarked
package de.planmarshall.operator.epicview;

import org.jspecify.annotations.NullMarked;
