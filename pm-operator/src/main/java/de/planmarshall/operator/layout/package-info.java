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
 * The installation layout as the operator binary sees it: the {@code pm-operator} of a release
 * resolves its own {@code <PM_MCP_HOME>} through the switched {@code PATH} links and starts the
 * runtime {@code pm-mcpd} and the relay {@code pm-mcp} of that same version directory by absolute
 * path (PM-ARCH-1, PM-ARCH-7).
 */
@NullMarked
package de.planmarshall.operator.layout;

import org.jspecify.annotations.NullMarked;
