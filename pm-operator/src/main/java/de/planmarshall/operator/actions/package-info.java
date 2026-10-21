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
 * The actions of a plan, an epic, or a landing in {@code pm-operator}
 * ({@code doc/design/terminal-client/19-actions.adoc#full-screen}, {@code doc/design/shared/work-actions.adoc}).
 *
 * <p>{@link de.planmarshall.operator.actions.ActionsMenu} is the modal menu the full-screen view
 * draws below a row or its head and {@link de.planmarshall.operator.actions.ActionMenus} builds it
 * from what the runtime offers in the subject's state. {@link
 * de.planmarshall.operator.actions.WorkActionsClient} is the fail-closed client of the control
 * contract: every method sends only what the contract defines and raises a refusal rather than
 * inventing an answer.
 */
package de.planmarshall.operator.actions;
