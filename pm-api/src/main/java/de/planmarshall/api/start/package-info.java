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
 * Data contracts of starting work: the entry of a plan or an epic, its sources and their states, the
 * listing of files for the picker, the recipe a plan runs, the start's chat, and the follow-up plan of
 * a chat about ended work (UC-STA-1 to UC-STA-5, UC-STA-7 to UC-STA-9, UC-CTL-1).
 * <p>
 * An entry is opened under its project ({@code POST /api/v1/projects/{project_id}/entries}) and is
 * addressed at the top level by its id from then on ({@code /api/v1/entries/{entry_id}}, its events at
 * {@code /api/v1/entries/{entry_id}/events}); only the opening request names the project.
 * <p>
 * The package holds the closed catalogues of the entry kinds ({@link de.planmarshall.api.start.EntryKind}),
 * the source kinds, states, trusts, and screening verdicts, the failure causes and their rate-limit
 * semantics, the operator states of the start, the recipe's plan id derivation, and the confinement of
 * the file picker. Every record carries a {@code jackson-core} tree codec and a streaming
 * {@code writeTo}/{@code readFrom} pair; no {@code jackson-databind} is involved.
 * <p>
 * The states of the entry are represented on the operator-interaction scheme: the scope is the entry,
 * the state ids are {@code start.choose}, {@code start.sources}, {@code start.ready},
 * {@code start.recipe}, {@code start.recipe-create}, {@code start.refused}, {@code start-epic.adopt},
 * {@code start-epic.adopt-failed}, {@code recipe.choose}, {@code recipe.parameters}, and
 * {@code recipe.create}, and the acts are operator links.
 * <p>
 * Specification: {@code pm-mcp-core/doc/specification/state-catalogue/09-bootstrap-and-lessons.adoc}
 * (Starting from the Clients, Recipe Choice, Source Units),
 * {@code pm-mcp-core/doc/specification/state-catalogue.adoc#chat} (Chatting with the Planning Role),
 * {@code pm-mcp-core/doc/specification/model-work/07-untrusted-text-screening.adoc} (Untrusted Text
 * Screening), and {@code plan-marshall-documentation/doc/specification/runtime-model/01-processes-and-transport.adoc}
 * (Local API). Design: {@code doc/design/shared/starting-work.adoc}.
 */
package de.planmarshall.api.start;
