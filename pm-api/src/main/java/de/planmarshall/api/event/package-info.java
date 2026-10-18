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
 * The event envelope and the closed event catalogue of the local API: the envelope shared by every
 * client stream ({@code event}, {@code id}, {@code data} with {@code event}, {@code at}, {@code scope}),
 * the closed enumeration of the catalogue's event names, and the closed enumeration of the reasons the
 * terminal {@code stream.closed} event carries.
 * <p>
 * Specification: {@code pm-mcp-core/doc/specification/runtime-model/06-event-catalogue.adoc} and
 * {@code plan-marshall-documentation/doc/specification/runtime-model/01-processes-and-transport.adoc}
 * (Events per Scope).
 */
package de.planmarshall.api.event;
