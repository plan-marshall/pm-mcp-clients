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
 * The one chat with the planning role, for every subject it is opened on: a plan, an epic, an entry,
 * a decision below its scope, and ended work (PM-WF-24; UC-CTL-1).
 * <p>
 * The package holds the chat as {@code GET …/chat} reads it ({@link de.planmarshall.api.chat.ChatResource}
 * with its turns, its open proposal and what the role knows), the plan's {@code queued_revision}, and
 * the closed lists the wire uses. Every record is read through the closed schema of its type: an
 * unknown member, a missing required member, and an unknown value of a closed list are refused with a
 * {@link de.planmarshall.api.json.JsonFormatException}, never defaulted.
 * <p>
 * Specification: {@code 01-processes-and-transport.adoc#chat-routes} and
 * {@code 06-event-catalogue.adoc#chat-events} of the Runtime &amp; Interaction Model; the states of
 * {@code state-catalogue.adoc#chat}; design {@code doc/design/web-app/chat.adoc#contract}.
 */
@NullMarked
package de.planmarshall.api.chat;

import org.jspecify.annotations.NullMarked;
