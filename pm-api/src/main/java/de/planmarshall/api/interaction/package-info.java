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
 * The operator representation of the local API: the state a client shows (its scope, state, progress,
 * title and texts, facts, proposal, items, and the links an act follows) and the operator link
 * {@code {id, rel, method, href, params, preselected, confirm}} — the contract both operator clients
 * build on.
 * <p>
 * Specification: {@code pm-mcp-core/doc/specification/hypermedia-format/01-representation-and-link-forms.adoc}
 * (Operator Representation, Operator Links, Texts of the Runtime) and
 * {@code plan-marshall-documentation/doc/specification/runtime-model/01-processes-and-transport.adoc}
 * (Local API).
 */
package de.planmarshall.api.interaction;
