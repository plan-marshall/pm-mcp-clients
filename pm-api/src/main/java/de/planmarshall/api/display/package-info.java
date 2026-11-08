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
 * The display layer: the one place that turns a server value into the word, the title, the time
 * and the duration the operator reads ({@code doc/design/shared/texts-and-languages.adoc}).
 *
 * <p>A client never prints a raw state id, a raw id without a title, an ISO string or a second
 * duration format of its own; it reads every one of them through {@link
 * de.planmarshall.api.display.Display}.
 */
package de.planmarshall.api.display;
