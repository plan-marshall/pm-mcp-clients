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
 * The full-screen TUI framework of the terminal client (doc/design/terminal-client/15-overview.adoc).
 *
 * <p>The package is split in two halves. {@link de.planmarshall.operator.tui.TuiLevel},
 * {@link de.planmarshall.operator.tui.TuiRow}, {@link de.planmarshall.operator.tui.TuiKeyRegistry},
 * {@link de.planmarshall.operator.tui.TuiViewStack}, and
 * {@link de.planmarshall.operator.tui.TuiLayout} are pure Java: they model the levels, the rows,
 * the keys, the selection, the filter, the overlays, and the drawing, and they have no dependency
 * on a terminal. {@link de.planmarshall.operator.tui.TuiScreenDriver} is the only class that talks
 * to JLine: it owns the alternate screen, the key reader, the display, and the restoration in a
 * {@code finally}.
 *
 * <p>The split is deliberate. The drawing library can be replaced without the model, and every
 * navigation, filter, key, and layout rule can be tested without a terminal. The driver is tested
 * against the virtual terminal of the test harness.
 */
package de.planmarshall.operator.tui;
