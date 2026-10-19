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
 * The configuration levels and origins of the local API: the closed set of levels an effective
 * value is resolved from, and the value with the origin it overrides and its revision.
 * <p>
 * Specification: {@code doc/specification/runtime-model/01-processes-and-transport.adoc} (Local API,
 * {@code GET /api/v1/config}) and {@code pm-mcp-core/doc/specification/store-schemas/08-project-configuration-local.adoc}
 * (Project Values over Machine Values).
 */
package de.planmarshall.api.config;
