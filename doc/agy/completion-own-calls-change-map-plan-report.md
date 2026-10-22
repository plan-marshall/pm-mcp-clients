# Completion Report: `completion-own-calls-change-map`

Plan: [`doc/agy/implemented/completion-own-calls-change-map-plan.md`](implemented/completion-own-calls-change-map-plan.md)
Work package: [`doc/plans/implemented/completion-own-calls-change-map.adoc`](../plans/implemented/completion-own-calls-change-map.adoc)
Branch: `feat/completion-own-calls-change-map`
Runbook: [`doc/agy/runbook.md`](runbook.md)

---

## Executive Summary

The completion report now carries the two rows the roadmap asked for: **Own calls**, apart from the operator's *Decisions*, and the **Change map** the runtime rendered. The contract gained `own_calls` (kind, description, a shortcut's limit and ending condition, the self-review's judgment and its conflict target) and `change_map` (diagram, state `ok`/`partial`/`indeterminate`, cause). The mock runtime serves a fixture plan with a design call, a shortcut, a replaced dependency, and a partial map; a second plan has neither. Both clients render the rows from the contract and compute nothing: the operator prints the diagram as delivered in the console's fixed-width output, the web app renders it inside a `<pre>` block, and neither redraws or re-wraps it.

All deliverables of the six-phase recipe are complete. Both rows are covered by `pm-api` round-trip and fail-closed tests, a mock-runtime contract pair (populated and empty), operator unit and scrollback tests, and web component tests.

---

## Delivered Artifacts

### Design (`doc/design`)

| File | Change |
|---|---|
| `shared/completion-report.adoc` | Added the *Own calls* and *Change map* rows to the layout transcript and their normative bullets; added the nine `report.completion.own_call_*` / `change_map_*` keys to the bilingual Texts table. |
| `terminal-client/24-reports.adoc` | Added the two rows to the `plan report` transcript and a bullet pinning the as-delivered rendering of the diagram. |

### `pm-api` (contract, catalogue)

| File | Change |
|---|---|
| `.../api/report/OwnCallRecord.java` | New Java 25 record: `kind`, `description`, `limit`, `endsWhen`, `judgment`, `conflictTarget`; canonical constructor null-checks; streaming map codec; fail-closed `fromMap` when `kind` or `judgment` is missing. |
| `.../api/report/ChangeMapRecord.java` | New Java 25 record: `diagram`, `state`, `cause`; canonical constructor null-checks; fail-closed `fromMap` when `diagram` or `state` is missing. |
| `.../api/report/PlanCompletionReport.java` | Two new components (`ownCalls`, `changeMap`), unmodifiable-list normalization, `own_calls`/`change_map` in `toMap`, tolerant `fromMap` parsing, and a 19-argument convenience constructor alongside the existing 17-argument one. |
| `messages/reports.en.properties`, `messages/reports.de.properties` | Own calls, kind select, limit, ending condition, both judgments, change map, state select, and cause in English and German. |
| `.../api/report/ReportRecordsTest.java` | Full-report fixture extended; round-trip test for own calls and the change map; null-member and missing-field fail-closed tests; the minimal report asserts empty own calls and no change map. |
| `.../api/message/ReportCatalogueTest.java` | The nine new keys join the specification-key list; select/parameter formatting is pinned in both languages. |

### `pm-mock-runtime`

| File | Change |
|---|---|
| `scenarios/reports-and-system.json` | Every `fix-relay` completion fixture gains three own calls (shortcut, design call, replaced dependency) and a `partial` change map with cause; `docs-typo` keeps neither. |
| `.../mock/runtime/MockRuntimeEngine.java` | The Java fallback report mirrors the fixture. |
| `.../mock/ReportsScenarioTest.java` | New assertions on the populated completion report; a new test for `docs-typo` covering the empty state. |

### `pm-operator`

| File | Change |
|---|---|
| `.../operator/planview/PlanCompletionLevel.java` | Full-screen rows for *Own calls* (kind, description, shortcut limit/ending, judgment, conflict target) and *Change map* (state, cause, one row per diagram line). |
| `.../operator/PlanCommand.java` | Scrollback rows in `plan report`, matching the design transcript; the diagram's leading spaces are kept while control sequences are stripped. |
| `.../operator/planview/PlanCompletionLevelTest.java` | Row assertions, a control-sequence test on the diagram that proves it is sanitized without being redrawn, and absence in the minimal report. |
| `.../operator/PlanCommandTest.java` | Scrollback assertions for both rows. |

### `pm-web-app`

| File | Change |
|---|---|
| `src/lib/api/client.ts` | `OwnCallRecord`, `ChangeMapRecord`, and their optional members on `PlanCompletionReport`. |
| `src/lib/views/CompletionReportView.svelte` | Own-calls section with kind and judgment chips plus shortcut details; change-map section with a state chip, cause, and a `<pre>` diagram; responsive styles. |
| `src/lib/views/CompletionReportView.test.ts` | Component tests for own calls, judgments, and the as-delivered `<pre>` diagram. |

### Governance

| File | Change |
|---|---|
| `doc/plans/README.adoc` | `completion-own-calls-change-map` marked `done`; the package file moved to `doc/plans/implemented/`. |
| `doc/agy/implemented/README.md` | Archive row added for this plan and its report. |
| `doc/agy/implemented/completion-own-calls-change-map-plan.md` | The plan, with checklist items 1–5 checked. |

---

## Test & Verification Results

| Layer | Command | Result |
|---|---|---|
| `pm-api` unit tests | `./mvnw test -pl pm-api` | 647 tests, 0 failures |
| `pm-mock-runtime` contract tests | `./mvnw test -pl pm-mock-runtime` | 406 tests, 0 failures |
| `pm-operator` unit, dialogue, and virtual-terminal tests | `./mvnw test -pl pm-operator` | 880 tests, 0 failures |
| Web component and catalogue tests | `npm run test` in `pm-web-app` | 413 tests, 0 failures (50 files) |
| Web type check | `npm run check` | 0 errors, 0 warnings |
| Web lint | `npm run lint` | clean |

Targeted coverage added by this change:

- `ReportRecordsTest.shouldRoundTripOwnCallsAndChangeMap` — every field survives encode/decode, including the shortcut's limit, ending condition, judgment, conflict target, and the diagram's embedded newline.
- `ReportRecordsTest.shouldRejectAnOwnCallOrChangeMapMissingItsContractFields` — the codecs fail closed rather than inventing a judgment.
- `ReportsScenarioTest.shouldServeCompletionReportWithoutOwnCalls` and the extended populated test — the contract's two states.
- `PlanCompletionLevelTest.ownCallsAndChangeMap` and `sanitizesDiagram` — both rows render and the diagram is sanitized without being redrawn.
- `CompletionReportView.test.ts` — own calls, judgments, and `<pre>` fidelity.

Playwright E2E was not extended: the change adds rows to an existing route and no new navigation or endpoint; `pm-web-e2e` still builds inside the pre-commit gate.

---

## Domain Invariants

| Invariant | Status |
|---|---|
| Java 25, immutable records, canonical null-checks | Met (`OwnCallRecord`, `ChangeMapRecord`, `PlanCompletionReport`). |
| `jackson-core` streaming only, zero databind | Met: the records use the existing `JsonTree` streaming codec. |
| No uncatalogued user-facing strings | Met: all new labels come from `reports.{en,de}.properties`; the web source adds no literal copy. |
| Bilingual catalogue in EN and DE | Met: the same nine keys exist in both packs, pinned by `ReportCatalogueTest` and the web `catalogue.test.ts` key-parity guard. |
| Client computes nothing | Met: kind, judgment, state, cause, ordering, and the diagram are rendered as delivered. |
| Fixed-width diagram, never redrawn | Met: terminal rows preserve leading spaces and strip only control sequences; the web uses `<pre>` with `white-space: pre`. |
| Terminal output safety boundary | Met: every runtime text passes through `stripControlSequences`/`PlanViewFormatting.clean`; diagram lines preserve layout but lose escape sequences. |
| Mock runtime completeness | Met: `reports-and-system.json` carries the populated fixture and the empty one, and the Java fallback mirrors it. |
| Responsive layout down to 320 px | Met: chips and detail lines wrap; the diagram scrolls horizontally instead of re-wrapping. |

---

## Build & Quality Evidence

`./mvnw verify -Ppre-commit` completed with **BUILD SUCCESS** across all eight reactor modules (`pm-api`, `pm-exec`, `pm-mock-runtime`, `pm-relay`, `pm-operator`, `pm-web-app`, `pm-web-e2e`) in 9 min 51 s. OpenRewrite, the proprietary licence header, Checkstyle, the enforcer rules (including `clients-depend-on-pm-api-only` and `jackson-core-only`), Javadoc, and every unit test ran.

**Pre-existing formatting drift, excluded from this change.** The profile's OpenRewrite step also rewrote 224 files this plan does not touch (import layout and JUnit method-name recipes). That drift is present on `main` before this branch. Committing it here would have mixed a repository-wide reformat into a scoped feature and collided with the parallel `cli-fullscreen-reports-system` worktree, so those unrelated rewrites were reverted; only this plan's footprint carries the change. The gate itself passed with those rewrites applied.
