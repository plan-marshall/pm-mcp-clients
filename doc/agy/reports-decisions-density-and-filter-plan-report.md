# Completion Report: `reports-decisions-density-and-filter`

**Plan**: [`doc/agy/reports-decisions-density-and-filter-plan.md`](implemented/reports-decisions-density-and-filter-plan.md)
**Branch**: `feat/reports-decisions-density-filter`
**Worktree**: `/Users/oliver/git/worktrees/pm-mcp-clients-reports-decisions-density-filter`
**Date**: 2026-10-23

---

## 1. Executive Summary

`/reports` and `/decisions` no longer waste half a desktop window, and both can be searched without scrolling.

Both views now offer three layouts — `comfortable` (the earlier single column), `compact` (denser rows, one header line, body clamped to two lines), and `split` (list left, detail right) — and remember the choice per view in `localStorage`. A shared filter-as-you-type control and matching utility power reports and decisions, and were adopted by the overview's running/ended lists and the epic's workstream list. The filter runs over loaded items only: no endpoint, record, fetch, or pagination change was made.

All acceptance criteria are met. `./mvnw verify -Ppre-commit` passed with `BUILD SUCCESS`; 513 unit/component tests and the reports/decisions/mobile Playwright specs pass.

---

## 2. Delivered Artifacts

### New shared primitives

| File | Purpose |
|---|---|
| `pm-web-app/src/lib/utils/filterText.ts` | `normalizeText` (NFD, combining marks stripped, lower case, whitespace collapsed), `tokenize`, `buildHaystack`, `matchesHaystack`, `matchesQuery` |
| `pm-web-app/src/lib/utils/filterText.test.ts` | 10 tests: case, diacritics, whitespace, null/undefined, empty query, multi-token AND, cross-field tokens, field-boundary guard |
| `pm-web-app/src/lib/components/FilterInput.svelte` | Labelled `role="search"` input, immediate emission, clear button, `Esc` clears, `/` focuses outside editable fields, polite live result count |
| `pm-web-app/src/lib/stores/viewLayout.ts` | `ViewLayout` union, guarded `localStorage` read/write under `pm_view_layout_<view>`, `createViewLayout` persisted store, unknown value falls back |
| `pm-web-app/src/lib/stores/viewLayout.test.ts` | Persistence, fallback, unknown stored value, store set/update |
| `pm-web-app/src/lib/components/ViewLayoutToggle.svelte` | Segmented three-option control, `aria-pressed`, `data-testid="layout-<mode>"` |

### Views

| File | Change |
|---|---|
| `pm-web-app/src/lib/views/ReportsView.svelte` | `data-layout`, three layouts, split list + `CompletionReportView` detail (list stays mounted, active card marked, read-in-place rows retained until close), filter over the published field matrix, rebuilt groups with empty groups dropped, loaded-only hint, compact body clamp/disclosure |
| `pm-web-app/src/lib/views/DecisionView.svelte` | Three layouts, split summary list + single mounted detail card, filter over the published field matrix, filtered-out selection cleared, banners/notices outside the filtered list |
| `pm-web-app/src/lib/views/OverviewView.svelte` | Shared filter on running plans, running epics, and the ended-work page; project tiles deliberately unfiltered |
| `pm-web-app/src/lib/views/EpicView.svelte` | Shared filter on workstream plans; empty workstreams dropped; a folded workstream opens while filtering |
| `pm-web-app/src/lib/api/client.ts` | Optional presentational fields mirroring the wire (`from.phase/role/harness/model`, report `title/status`); no behaviour change |

### Tests and E2E

| File | Coverage |
|---|---|
| `pm-web-app/src/lib/views/ReportsView.test.ts` | +10 tests: five field groups, status, multi-token, group rebuild/restore, no-match, unread count and zero requests while typing, loaded-only hint + “Show older” retaining the query, layout persistence, split list/active card, `Esc` |
| `pm-web-app/src/lib/views/DecisionView.test.ts` | +9 tests: title/subject/description, option + evidence, waiver gate/reason, no-match + open count, banners outside the filter, split summary/single detail, filtered-out selection clearing, compact actions, layout persistence |
| `pm-web-app/src/lib/views/OverviewView.test.ts` | +5 tests: plan filter, epic/state fields, no-match, epic identifier, ended-work title/outcome/reason/date |
| `pm-web-app/src/lib/views/EpicView.test.ts` | +4 tests: plan/status/workstream name, wait reason + overlap file, no-match + dropped workstream, folded workstream opens while filtering |
| `pm-web-e2e/tests/reports.spec.js` | +2 tests: filtering and no-match state with the loaded-only hint; split keeps the list beside the detail and persists across reload |
| `pm-web-e2e/tests/decisions.spec.js` | +1 test: option-phrase filter and compact-mode action reachability |
| `pm-web-e2e/tests/mobile.spec.js` | +2 viewport tests (320×568, 375×667): filter usable, split collapses to one column, no horizontal overflow |

### Catalogue and documentation

- `pm-api/src/main/resources/messages/common.{en,de}.properties`: filter and layout labels, counts, empty state.
- `reports.{en,de}.properties`: report filter copy, loaded-only hint, no-match, read/unread state, body disclosure, split placeholder.
- `decisions.{en,de}.properties`: decision filter copy, no-match, split placeholder.
- `doc/design/web-app/view-density-and-filtering.adoc`: the three modes, the 960 px breakpoint, the filter contract, the field matrices, the adoption rule, and rollback.
- `doc/design/web-app/README.adoc`: links the new design note.

---

## 3. Field Matrices (implemented)

| View | Fields searched |
|---|---|
| Reports | project (+title), epic (+title), plan (+title), phase, role, harness, model, title, subject, kind, body, summary, status, localized unread/read, rendered created/sent time, `created_at` |
| Decisions | id, title, header, subject, scope, phase, role, blocks (raw + localized), reversibility (raw + localized), description, change, waiver gate/result/reason, serialized evidence, delivered time, localized waiver title, each part question/recommendation (+ option label)/rationale/role/answer type/dependency, each option key/label/tradeoff/consequences/dissent, typed notes |
| Overview plans | plan id, title, project name/id, epic id/title, phase, state, off-track signal |
| Overview epics | epic id, title, project name/id, needs-operator kind, nested plan ids/titles |
| Overview ended work | id, kind, title, outcome, terminal reason, reason, ended date (+ relative) |
| Epic workstreams | workstream id/name, plan id, title, status, wait reason, wait plan id, change, objective, deliverables, surface, dependencies, overlap files |

---

## 4. Before / After Width Evidence

| View | Before | After |
|---|---|---|
| Reports | Single column hard-capped at `900px`; detail replaced the list | `comfortable` keeps the readable 900 px bound; `compact` tightens padding to `--space-2 --space-3`; `split` removes the cap and uses `minmax(320px, 420px) minmax(0, 1fr)` — the list keeps its measure, the detail takes the rest |
| Decisions | Single column hard-capped at `800px`; every card at full height | `split` removes the cap and uses `minmax(300px, 380px) minmax(0, 1fr)`; the selected decision is the only mounted card |
| Narrow screens | — | `max-width: 959px`: one column, list or detail (never both), no horizontal scroll (asserted at 320 px and 375 px) |

The Playwright split test asserts that `.reports-tree` and `.report-card--active` remain visible beside `.completion-report`, and that `data-layout="split"` survives a reload.

---

## 5. Test & Verification Results

| Check | Result |
|---|---|
| `npm test -- filterText ReportsView DecisionView OverviewView EpicView viewLayout` | 14 + 22 + 25 + 16 + 21 + 4 tests pass |
| Full `npm test` (pm-web-app) | **513 tests, 56 files, all passed** |
| `npm run check` (`svelte-check`) | **0 errors, 0 warnings** |
| `npm run lint` (ESLint) | **clean** |
| Playwright `reports.spec.js`, `decisions.spec.js`, `mobile.spec.js` | **26 tests passed** |
| `./mvnw verify -Ppre-commit` | **BUILD SUCCESS** (8:48 min); clean `git status` after the gate |

### Accessibility evidence

- Filter: visible label bound with `for`, wrapper `role="search"`, result count in `aria-live="polite"`, clear button labelled, `Esc` clears, `/` focuses only outside input/textarea/select/contenteditable.
- Layout toggle: native buttons in a labelled `role="group"`, `aria-pressed` on the active mode, `data-testid="layout-<mode>"`, focus ring from the shared tokens.
- Split selection: `aria-current="true"` on the active report card and the active decision summary.
- Keyboard-only operation is covered by the component tests (`Esc`, button activation) and by the browser’s native button semantics for the toggle.

---

## 6. Domain Invariants

- **No API, record, or server change**: only optional presentational fields were added to the frontend types; the mock runtime fixtures were untouched.
- **No uncatalogued strings**: every new label, hint, placeholder, and empty state exists in English and German.
- **Client-only filtering**: tests assert `apiClient.getReports` is not called while typing.
- **Served order preserved**: matching filters an array; nothing is re-sorted.
- **Counts keep their meaning**: unread reports and open decisions stay global; the filter reports its own match count.
- **Responsive down to 320 px**: asserted by the mobile Playwright tests.
- **Existing behaviour unchanged on an empty query**: the full pre-existing unit and E2E suites pass unchanged.

---

## 7. Adoption List

**Adopted the shared `FilterInput` + `filterText`:**

- `ReportsView` (required)
- `DecisionView` (required)
- `OverviewView` — running plans, running epics, ended-work page
- `EpicView` — workstream plans

**Deliberately did not adopt:**

- `SettingsView`, `SystemView`, `CleanupView`, `MachineSettingsView`: single-record/detail surfaces, not multi-item lists.
- Forms, wizards, and pickers (`StartWorkModal`, `RecipeSelector`, onboarding steps, `BlockPalette`, source pickers): their list is the input, not a result set to search.
- The overview’s project tiles: navigation choosing what the main panel shows, not list items.
- `ProjectExplorerView`, `PlanView`, `CompletionReportView`, `EpicDecompositionView`, `ProcessDesigner`: single-record or structured detail surfaces; a future plan may adopt the filter where they render genuine multi-element lists.

**Rule for future views**: import the shared components, publish the field matrix in the design note, and keep header counts distinct from match counts.

---

## 8. Rollback

Remove the layout toggle and filter slots, restore the single-column CSS, and delete the new primitives. No stored record, endpoint, or payload changes; the `pm_view_layout_*` keys left in `localStorage` are harmless.
