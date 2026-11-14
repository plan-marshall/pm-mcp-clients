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
 * Data contracts, routes, and the event of a plan's changes: the list of the files a plan changed,
 * one file's hunks, and the two refusals before any capture exists or after the captured commit is
 * gone ({@code 01-processes-and-transport.adoc#plan-changes}, PM-ARCH-11).
 */
@NullMarked
package de.planmarshall.api.changes;

import org.jspecify.annotations.NullMarked;
