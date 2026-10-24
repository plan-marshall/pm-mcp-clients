# Completion Report: `reports-decisions-density-and-filter`

**Plan**: [`doc/agy/reports-decisions-density-and-filter-plan.md`](implemented/reports-decisions-density-and-filter-plan.md)
**Branch**: `feat/reports-decisions-density-filter`
**Worktree**: `/Users/oliver/git/worktrees/pm-mcp-clients-reports-decisions-density-filter`
**Date**: 2026-10-23
**Step 8 status**: **FAILED — the split-view race is remediated and verified, but the unread-count double-decrement remains**; see §10 and `reports-unread-count-reconciliation-plan.md`.

---

## 1. Executive Summary

`/reports` and `/decisions` no longer waste half a desktop window, and both can be searched without scrolling.

Both views now offer three layouts — `comfortable` (the earlier single column), `compact` (denser rows, one header line, body clamped to two lines), and `split` (list left, detail right) — and remember the choice per view in `localStorage`. A shared filter-as-you-type control and matching utility power reports and decisions, and were adopted by the overview's running/ended lists and the epic's workstream list. The filter runs over loaded items only: no endpoint, record, fetch, or pagination change was made.

All acceptance criteria are met except the split view's active-card retention, which fails when the read event arrives before the request result (see §10). `./mvnw verify -Ppre-commit` passed with `BUILD SUCCESS`; the unit/component suites pass, while the live reports split Playwright case remains the Step 8 blocker.

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
| Playwright `reports.spec.js`, `decisions.spec.js`, `mobile.spec.js` | **1 failed** — the reports split active-card case; the other reports, decisions, and mobile cases pass |
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

## 8. Review Window

- CI on PR #191: build gate, both JDK builds, Quarkus alignment, integration tests, dependency review — **all pass**.
- SonarCloud: **Quality Gate passed, 0 new issues** (an initial run raised six `S5906` test-assertion findings; fixed in `990087b` with `toHaveLength`, re-analysed clean).
- CodeRabbit: automatic review and three explicit `@coderabbitai review` requests were answered with “Review rate limited” (the organisation’s free OSS quota was exhausted). No findings were emitted, so none remained unaddressed.
- Sourcery: skipped with the account’s 250,000-character weekly review budget exhausted; no findings were emitted.

Both bots were retried across more than an hour; their remaining quota is outside this worktree’s control. All checks that could run are green.

---

## 9. Rollback

Remove the layout toggle and filter slots, restore the single-column CSS, and delete the new primitives. No stored record, endpoint, or payload changes; the `pm_view_layout_*` keys left in `localStorage` are harmless.

---

## 10. Post-Landing Verification (Step 8)

**Verdict: FAILED.** The original split-view race is remediated and verified, but the same live flow exposed a remaining unread-count double-decrement (7 → 5 for one read). The report stays in `doc/agy/`; Step 8 remains open for the count reconciliation.

### Pillars 1, 2, and 4

| Pillar | Result | Evidence |
|---|---|---|
| **1 — Traceability** | Pass | The three layouts, shared filter, field matrices, loaded-only hint, overview and epic adoption, and the 960 px collapse match UC-PLN-2/4/6 and the plan. No API, record, server, or pagination change. |
| **2 — Anti-patterns** | Pass | No fallback dictionary; all new text in EN/DE; no `{@html}`; layout/filter state is client-only; existing responsive and routing rules unchanged. |
| **4 — Independent diff** | Pass | Changes stay in `pm-web-app`, `pm-api` catalogue, `pm-web-e2e`, and design docs; no test weakening; no unrelated reverts; the commit trailer is present. |

### Pillar 3 — the failure

On the first pass, the live `reports.spec.js` split-layout case failed on `.report-card--active`: `markRead` set the retain marker after the request, so a `report.read` event that arrived first filtered the row out. That race is fixed and verified by the retained-race remediation.

The same live flow now shows the remaining count defect: the local unread pill drops by two for one read (7 → 5). `handleEvent('report.read')` decrements `unreadCount` and `markRead` decrements it again on success. The split Playwright case and the other reports/decisions/mobile cases pass; the count is the Step 8 blocker.

### Remediation

[`implemented/reports-decisions-density-and-filter-remediation-plan.md`](implemented/reports-decisions-density-and-filter-remediation-plan.md) targets the exact race: set the retain marker before the request, roll it back on failure, and cover the event-before-resolution ordering in a component test plus the split Playwright case.

**Outcome (2026-10-24, branch `fix/reports-split-retain-race`).** `markRead` records the retained marker before `apiClient.markReportRead` and removes it in the catch path. The new component test holds the request open, emits `report.read` for the id, and asserts the card and its read state survive the event and the resolution; it fails against the previous ordering and passes against the fix. The failed-read test asserts the error banner, the still-active card, the restored unread state, and an unchanged count. The live `reports.spec.js` suite is **10 passed**, including the split case that failed this verdict, and `./mvnw verify -Ppre-commit` is green. Evidence: [`verified/reports-decisions-density-and-filter-remediation-plan-report.md`](verified/reports-decisions-density-and-filter-remediation-plan-report.md).

The retain-race remediation is verified and archived. This parent report remains open because the adjacent unread-count double-decrement is still real: `handleEvent` decrements and `markRead` decrements again for the same read. [`reports-unread-count-reconciliation-plan.md`](reports-unread-count-reconciliation-plan.md) records the remaining fix; Step 8 is re-run after it lands.
