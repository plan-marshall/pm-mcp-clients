# Completion Report: `web-reports-system-and-designer`

**Plan:** [`doc/agy/implemented/web-reports-system-and-designer-plan.md`](implemented/web-reports-system-and-designer-plan.md)
**Work packages:** `web-reports`, `web-system`, `web-designer` (`doc/plans/implemented/`)
**Branch:** `feat/web-reports-system-and-designer`
**Worktree:** `/Users/oliver/git/worktrees/pm-mcp-clients-web-reports-system-designer`

## 1. Executive Summary

The three remaining views of the web app are complete. Reports now render the whole `from` provenance as
independent, non-reading links, open every subject kind at its view (completion report, plan, epic,
project, start, cleanup), disable the detail of a subject the cleanup deleted, and announce arrivals in a
coalesced polite live region that leaves the tab title alone. The system view remembers its window,
grouping, and project per browser, links its work figures to what they count, and runs the whole upgrade
state machine (available notice, pre-upgrade checks and consent, progress, withdraw, failure, and the
reload that waits for unsent text). Cleanup items open read-only with their checkbox selection kept, and a
report's cleanup detail highlights the job it names. The process designer gained browser file upload,
paste and template intake, a per-browser draft with a leave guard, line numbers with two-way text/structure
selection, a two-way frontmatter panel (author, date/release, signature drop, locked interface), a
side-by-side placement difference with affected projects, landing progress, retry, and conflict comparison,
and a read-only phone mode that keeps Validate.

The client also gained wire normalisers for the records the runtime actually serves (report origin, the
completion report's token lines, the system snapshot, model metrics, and the cleanup job). Before this
change the views read a spelling the runtime does not send, so tokens rendered as `NaN`, report paths fell
into *This machine*, and cleanup jobs looked empty; the end-to-end suites now assert the real wire shape.

Every user-facing text is a catalogue key in English and German; `npm run check` reports zero errors and
zero warnings. No file in `pm-operator`, `pm-mock-runtime`, `routes.ts`, or `AppRouter.svelte` was changed.

## 2. Delivered Artifacts

### 2.1 `pm-web-app` views and shared code

| File | Change |
|---|---|
| `src/lib/views/ReportsView.svelte` | Coalesced polite (`aria-live="polite"`) arrival notice in a visually hidden live region; project/epic/plan provenance rendered as independent links that navigate without reading; one `reportCard` snippet for all groups; all detail targets (`plan.ended`→completion, `plan.*`→plan, `epic.*`→epic, `project.*`→projects, `source.*`/`subject_kind=start`→start, `system.cleanup*`→cleanup); disabled detail plus `report.detail.deleted_by_cleanup` for a deleted subject. |
| `src/lib/views/SystemView.svelte` | `pm_system_filters` persistence and restore (window, grouping, project); project filter fed by the project explorer; clickable `Work` figures that open the overview on plans or epics or the project list; full upgrade state machine: available notice with release notes, check list (busy workers, active builds, dirty worktrees or their absence), consent screen, step list, withdraw, `Upgrade now`, failure panel with cause and retry/dismiss, and a post-upgrade reload guarded by `hasUnsentInput()`. |
| `src/lib/views/CleanupView.svelte` | Unchecked candidates and the open preview kept on `runtimeStore`; in-place read-only preview with Back; highlight of the item a report's detail named (or of the summary); selection survives preview round-trips; selection reset after a delete. |
| `src/lib/views/ProcessDesigner.svelte` | Intake modes (installed, upload, paste, template); `pm_designer_draft` persistence and restore; `beforeunload` plus in-app leave dialogue; line-number gutter with finding marks and active line; two-way text/structure selection and finding-to-line jump; frontmatter panel synced both ways (name, description, type, entries, exits, author, modified, release, signature), first-edit date/release and signature drop, author remembered per device, interface locked when replacing; placement target choice, side-by-side line diff, affected projects, landing steps, retry, conflict comparison, result line; `< 640px` read-only mode with Validate retained. |
| `src/lib/views/OverviewView.svelte` | Consumes `overviewFocus` once, so a system figure opens the overview on the plans or the epics. |
| `src/lib/components/WorkflowValidationResults.svelte` | Findings are buttons that move the text to their line. |
| `src/lib/stores/runtime.ts` | Cleanup selection/preview/highlight and overview focus; report arrivals unwrap the runtime's `{report: …}` SSE envelope. |
| `src/lib/stores/unsentInput.ts` | **New.** Document-level tracking store and synchronous `hasUnsentInput()` scan for the post-upgrade reload guard. |
| `src/lib/utils/lineDiff.ts` | **New.** LCS line diff with paired change rows and a bounded fallback for oversized texts. |
| `src/lib/utils/frontmatter.ts` | **New.** Parse/serialise a unit's frontmatter, create one, and apply the date/release/signature edit. |
| `src/lib/api/client.ts` | Normalisers for report origin and deleted-subject flag, completion token usage/breakdown (`label`/`tokens`/`input_tokens`/`details`), system snapshot (upgrade, heap, listeners, models), and cleanup job/kept items (`project_items`, `size_bytes`, `how`); `withdrawUpgrade` falls back to the operator route; optional dirty-worktree and release-notes fields. |

### 2.2 Tests

| File | Change |
|---|---|
| `src/lib/views/ReportsView.test.ts` | Path links without reading, epic detail with read, deleted-subject disabled detail, coalesced polite arrival and unchanged title. |
| `src/lib/views/SystemView.test.ts` | Filter memory/restore, work figure links, consent checks, withdraw, failure cause/retry, unsent-text reload wait. |
| `src/lib/views/CleanupView.test.ts` | Read-only preview preserving checks and the report highlight. |
| `src/lib/views/ProcessDesigner.test.ts` | Paste/template intake, draft restore, frontmatter two-way edit and signature drop, structure-to-line and finding-to-line jumps, diff/confirm placement, conflict comparison, phone read-only. |
| `src/lib/utils/lineDiff.test.ts`, `frontmatter.test.ts`, `src/lib/stores/unsentInput.test.ts`, `src/lib/api/client.normalise.test.ts` | **New** unit suites for the utilities and the wire normalisers. |

### 2.3 `pm-api` message catalogue

New EN/DE keys in both locales for report provenance and deleted subjects, work-figure links, the upgrade
notice/checks/consent/withdraw/reload, the cleanup preview, and the designer intake, frontmatter, progress,
and failure texts. `catalogue.test.ts` confirms parity and that every looked-up key exists.

### 2.4 `pm-web-e2e`

| File | Change |
|---|---|
| `tests/reports.spec.js` | Provenance navigation without reading, epic and project detail targets, broadcast-driven polite arrival notice. |
| `tests/system.spec.js` | Filter persistence across reload, work figure link, consent/checks/progress/withdraw, cleanup preview with the unchecked item kept; strict-mode fixes for the dashboard assertions. |
| `tests/designer.spec.js` | Placement through the diff/confirmation dialogue, uploaded file intake, draft across reload, frontmatter edit, finding jump, leave guard, phone read-only. |

### 2.5 Documentation

The text tables of `doc/design/shared/reports.adoc`, `system-texts.adoc`, `cleanup-texts.adoc`, and
`doc/design/web-app/process-designer-texts.adoc` list the new keys.

## 3. Test & Verification Results

| Gate | Result |
|---|---|
| `cd pm-web-app && npm run check` | 0 errors, 0 warnings |
| `cd pm-web-app && npm test` | 48 files, **393 tests passed** |
| `cd pm-web-app && npm run lint` | clean |
| `cd pm-web-e2e && npx playwright test tests/reports.spec.js tests/system.spec.js tests/designer.spec.js` | **27 tests passed** |
| `./mvnw verify -Ppre-commit` | **BUILD SUCCESS** (all 8 reactor modules) |

The pre-commit run applied the repository's OpenRewrite formatting to unrelated Java sources outside this
plan's boundary (`pm-operator`, `pm-api` main/test). Those rewrite-only edits were reverted after the run so
the change set stays within the plan's sole-owner and no-touch rules; the build result above is the run that
validated the delivered web sources and the catalogue resources.

## 4. Domain Invariants

- **No uncatalogued strings:** all new copy is a `$t(...)` key in EN and DE; the catalogue guard passes.
- **Bilingual catalogue:** EN/DE key sets are identical for all six changed properties files.
- **No money, no budget:** token figures keep the existing `formatSplit`/`formatTokens` helpers.
- **Trust boundaries:** uploaded and pasted unit text is rendered as text in a textarea; no HTML sink was
  added; the file is read in the browser and never sent until Validate/Place.
- **Responsive layout:** the designer switches to a read-only phone mode below 640 px, keeps Validate, and
  hides editing/placing; the diff collapses to one column at that width.
- **Module boundary:** no change under `pm-operator` or `pm-mock-runtime`; `routes.ts` and `AppRouter.svelte`
  are untouched.
- **Determinism:** new unit tests clear the mocked `localStorage` per test; the draft/author keys are the
  documented `pm_designer_draft`/`pm_designer_author`, the filters key `pm_system_filters`.

## 5. Build & Quality Evidence

`./mvnw verify -Ppre-commit` completed with `BUILD SUCCESS` after the web-app type check, unit tests, lint,
the e2e module's install/lint, licence and enforcer rules, Javadoc, and the full Java test suite. The working
tree contains only the intended source, catalogue, test, and documentation changes listed above.
