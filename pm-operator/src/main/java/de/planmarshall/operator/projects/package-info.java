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
 * The full-screen Project Explorer ({@code pm-operator projects},
 * {@code doc/design/terminal-client/17-projects.adoc} and {@code doc/design/shared/project-explorer.adoc}).
 *
 * <p>{@link de.planmarshall.operator.projects.ProjectsExplorerLevel} composes the four groups
 * (_Being added_, _Favourites_, _Recent_, _All projects_) into pure
 * {@link de.planmarshall.operator.tui.TuiRow}s; {@code ProjectsExplorerActions} names the
 * state-specific actions of a project; {@link de.planmarshall.operator.projects.ProjectOpenEntry}
 * reads the open starts a project detail reports, so a project with an unfinished start offers
 * _Continue starting_ first.
 */
@NullMarked
package de.planmarshall.operator.projects;

import org.jspecify.annotations.NullMarked;
