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
 * The full-screen overview ({@code pm-operator plans}, {@code doc/design/terminal-client/15-overview.adoc}
 * and {@code doc/design/shared/plan-overview.adoc}).
 *
 * <p>{@link de.planmarshall.operator.overview.ProjectsOverviewLevel},
 * {@link de.planmarshall.operator.overview.ProjectWorkLevel}, and
 * {@link de.planmarshall.operator.overview.EndedWorkLevel} build the pure {@link de.planmarshall.operator.tui.TuiRow}
 * view models for Level 1 (projects), Level 2 (a project's work by plan or by epic), and Ended Work.
 * {@link de.planmarshall.operator.overview.OverviewFullScreenApp} drives the alternate-screen loop,
 * SSE live updates, state persistence ({@code overview.json}), and held-notice drain.
 */
@NullMarked
package de.planmarshall.operator.overview;

import org.jspecify.annotations.NullMarked;
