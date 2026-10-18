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
 * The installation layout seen from the client binaries: the resolution of {@code <PM_MCP_HOME>}
 * through the two switched {@code PATH} symbolic links, the versioned installation directory, the
 * sibling binaries beside the running one, and whether the installation is runtime-upgradable
 * (PM-ARCH-1, PM-ARCH-7).
 * <p>
 * Specification: {@code doc/specification/cli-and-security/01-cli-and-installer.adoc} (CLI &amp;
 * Security, Installation Layout).
 */
package de.planmarshall.api.layout;
