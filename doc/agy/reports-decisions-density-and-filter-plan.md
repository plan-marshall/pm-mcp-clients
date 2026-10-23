# Implementation Plan: `reports-decisions-density-and-filter` (Use the Width, Then Find in It)

`/reports` and `/decisions` are single-column lists inside hard width caps (`900px` and `800px`). On a normal desktop the right half of the window is empty, each card stretches a sparse row across the full measure, and opening a report replaces the list instead of using the space beside it. Neither view offers a way to find an item other than scrolling, even though both already hold the data client-side.

This plan gives both views a deliberate layout choice — `comfortable`, `compact`, and `split` — and a shared **filter-as-you-type** control that searches every piece of text the view can show, without a network request. Reports and decisions are the required adopters; the filter component is shared so the overview, the epic view, and other multi-element lists can adopt it without inventing their own matching rules.

The work is confined to `pm-web-app` plus the message catalogue. No API, record, or server change: the filter runs over the items already loaded, exactly as requested.

---

## 1. Traceability & Context

| Dimension | Reference |
|---|---|
| **Use Cases** | [`UC-PLN-6`](../../../plan-marshall-documentation/doc/use-cases/03-plans.adoc#UC-PLN-6) (Catch Up on What Happened), [`UC-PLN-2`](../../../plan-marshall-documentation/doc/use-cases/03-plans.adoc#UC-PLN-2) (Decide What a Plan Asks), [`UC-PLN-4`](../../../plan-marshall-documentation/doc/use-cases/03-plans.adoc#UC-PLN-4) (See What Runs, Across Projects) |
| **Requirements** | `PM-SEC-7` (both clients as dependable views), `PM-WORK-7` (worker activity and signals, where relevant to report text) |
| **Reports today** | [`ReportsView.svelte`](../../pm-web-app/src/lib/views/ReportsView.svelte) — `groupedReports`, `.reports-view { max-width: 900px }`, list-or-detail switch, pagination |
| **Decisions today** | [`DecisionView.svelte`](../../pm-web-app/src/lib/views/DecisionView.svelte) — `.decisions-container { max-width: 800px }`, single `.decisions-list` |
| **Existing tests** | [`ReportsView.test.ts`](../../pm-web-app/src/lib/views/ReportsView.test.ts), [`DecisionView.test.ts`](../../pm-web-app/src/lib/views/DecisionView.test.ts) |
| **E2E** | [`pm-web-e2e/tests/reports.spec.js`](../../pm-web-e2e/tests/reports.spec.js), [`pm-web-e2e/tests/decisions.spec.js`](../../pm-web-e2e/tests/decisions.spec.js), [`pm-web-e2e/tests/mobile.spec.js`](../../pm-web-e2e/tests/mobile.spec.js) |
| **Catalogue** | `reports.{en,de}.properties`, `decisions.{en,de}.properties`, `common.{en,de}.properties` under [`pm-api/src/main/resources/messages`](../../pm-api/src/main/resources/messages) |
| **Design tokens** | [`tokens.css`](../../pm-web-app/src/styles/tokens.css), [`base.css`](../../pm-web-app/src/styles/base.css) |
| **Operational SOP** | [`runbook.md`](runbook.md) |

### 1.1 Finding Analysis

| # | Finding | Root cause | Correction |
|---|---|---|---|
| **F1** | `/reports` and `/decisions` waste a lot of space. | Both views cap their single column (`900px` / `800px`) and stack every card at full width with `--space-5` padding. Reports replace the list with the completion report; decisions render every evidence block and form at full height. Wide screens are unused. | Add a persisted layout choice: `comfortable` (today), `compact` (denser list), `split` (list left, detail right). Remove the hard cap in `split`; keep a readable measure in the single-column modes. |
| **F2** | There is no filter-as-you-type. | Both views render all loaded items with no query state; only pagination and scrolling exist. | Add a shared `FilterInput` and matching utility over the loaded items. |
| **F3** | “All potentially visible text” is ambiguous. | Each view shows different fields; a filter over titles alone would miss bodies, projects, options, evidence, and statuses. | Define an explicit field matrix per view and test each representative field. |
| **F4** | The filter must be client-only and may assume data is loaded. | Reports are paginated (`hasOlder`); decisions are loaded in one call. | Filter only loaded items, never fetch on keystroke; show a “loaded reports only” hint while `hasOlder` is true and let “Load older” widen the set. |
| **F5** | The same need exists in other multi-element views. | The matching rules would be duplicated if each view implemented its own. | Ship one `filterText` utility and one `FilterInput`; adopt them in reports and decisions first, then in the overview, epic, and other list surfaces that already hold their data. |

---

## 2. Scope & Objectives

### Shared primitives

1. `filterText.ts`:
   - `normalizeText(value)`: Unicode NFD, strip combining marks, lowercase, collapse whitespace;
   - `tokenize(query)`: normalized whitespace-split tokens;
   - `matchesQuery(query, values)`: every token must appear as a substring of at least one normalized value (AND across tokens, OR across fields);
   - `buildHaystack(values)`: joins the visible fields with a separator once per item, so filtering does not re-normalize on every keystroke.
2. `FilterInput.svelte`:
   - labelled `<input type="search">`, `role="search"` wrapper, `data-testid="filter-input"`;
   - clear button, `Esc` clears, `/` focuses when the focus is not already in an input/textarea;
   - result-count live region (`aria-live="polite"`);
   - no network access.
3. `viewLayout.ts`:
   - `type ViewLayout = 'comfortable' | 'compact' | 'split'`;
   - guarded `localStorage` read/write under `pm_view_layout_<view>`;
   - default `comfortable`; `split` only offered for views with a detail target.
4. `ViewLayoutToggle.svelte`:
   - a three-option segmented control, `aria-pressed`, keyboard operable, `data-testid="layout-<mode>"`.

### Reports (`/reports`)

5. Layout modes:
   - `comfortable`: current grouped tree and cards, readable measure;
   - `compact`: tighter padding/gaps, metadata on one line, body clamped to two lines with a disclosure;
   - `split`: left list pane (approximately `minmax(320px, 420px)`) and right detail pane; opening a report sets `activeDetailPlanId` and renders `CompletionReportView` in the right pane instead of replacing the list; the list stays scrollable and the selected row is marked.
6. On narrow viewports (`< 960px`), `split` collapses to the single-column behavior; no horizontal scrolling.
7. Reports filter:
   - over `from.project`, `from.epic`, `from.plan`, `title`, `subject`, `kind`, `from.phase`, `from.role`, `from.harness`, `from.model`, `body`, `summary`, `status`, localized unread/read state, and the rendered created date;
   - group headers are rebuilt from the matching items and empty groups are dropped;
   - unread counts stay global, not filtered;
   - while `hasOlder` is true, show `report.filter.loaded_only`; typing never triggers a fetch.
8. Decisions filter:
   - over `title`, `subject`, `scope`, `description`, `asking_phase`, `asking_role`, `blocks`, `reversibility`, waiver `gate`, `result`, `reason`, each option's label/description, the recommendation, evidence values, part questions, and any note;
   - a filtered-out selected decision clears the split selection;
   - waiver and standard decisions use the same matching rules.
9. Adoption in other multi-element views:
   - `OverviewView.svelte`: the running-work plan/epic lists and the ended-work page;
   - `EpicView.svelte`: workstream plan lists;
   - one `FilterInput` per view, same matching rules, same result-count and empty-state behavior;
   - single-record views (settings, system, cleanup) get no filter.

### Non-Goals

- No server-side search, no new endpoint, no query parameter that changes the fetch, no pagination semantics change.
- No reordering of decisions or reports by relevance; filter preserves the served order.
- No fuzzy matching, stemming, regex input, or highlighting in this plan; substring matching over normalized text is the contract.
- No change to report/decision actions, forms, notifications, or data models.
- No redesign of `CompletionReportView` itself; only its placement in split mode.

---

## 3. Isolation & Boundaries

| Repository | Worktree | Branch | Scope |
|---|---|---|---|
| `pm-mcp-clients` | `/Users/oliver/git/worktrees/pm-mcp-clients-reports-decisions-density-filter` | `feat/reports-decisions-density-filter` | `pm-web-app`, message catalogue, tests, design note, report |

> **Pre-flight**: `git checkout main && git pull --rebase origin main` before creating the worktree.

**Sole-owner files**: `ReportsView.svelte`, `DecisionView.svelte`, `OverviewView.svelte`, `EpicView.svelte`, the new shared components/utilities, their tests, `pm-web-e2e/tests/{reports,decisions,mobile}.spec.js`, `reports.{en,de}.properties`, `decisions.{en,de}.properties`, `common.{en,de}.properties`, the new design note, and the report.

**No touch**: `pm-api` records, `pm-mock-runtime` scenario data (the existing fixtures already contain enough text), `pm-mcp-server`, the plan-view selection work, the advisory-title layer, and the brand mark.

---

## 4. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [ ] 1. Isolated worktree created at `/Users/oliver/git/worktrees/pm-mcp-clients-reports-decisions-density-filter` on branch `feat/reports-decisions-density-filter` off latest `main`.
- [ ] 2. Domain skills & engineering personas loaded where applicable:
  - `plan-marshall-persona-implementer` & `pm-dev-frontend:javascript` for the shared primitives and views;
  - `pm-dev-frontend:css` for grid/container-query layouts and the compact density;
  - `plan-marshall-persona-module-tester` & `pm-dev-frontend:jest-testing` for matching and component tests;
  - `plan-marshall-persona-integration-tester` for Playwright and mobile coverage;
  - `plan-marshall-persona-security-expert` for rendering untrusted report/decision text;
  - `plan-marshall-persona-documenter` for the design note and report;
  - `plan-marshall-persona-code-reviewer` for review triage.
- [ ] 3. Incremental implementation with tests following the phased technical recipe.
- [ ] 4. Completion report written (`doc/agy/reports-decisions-density-and-filter-plan-report.md`), plan moved to `doc/agy/implemented/`, links rebased, dashboard updated.
- [ ] 5. Pre-commit passed: `./mvnw verify -Ppre-commit`.
- [ ] 6. PR opened (never `skip-bot-review` on worker PRs); review windows and all bot/Sonar findings resolved.
- [ ] 7. Enqueued in the merge queue, landed on `main`, worktree and branch cleaned up.
- [ ] 8. Post-landing verification performed and the report moved to `doc/agy/verified/` by the orchestrator.

> [!IMPORTANT]
> **Plan archival link rule**: this plan is written at `doc/agy/`; its links use `../design`, `../../pm-web-app`, `../../pm-api`, `../../pm-web-e2e`, `../../../plan-marshall-documentation`, and `implemented/<name>.md`. When Step 4 moves it, rebase every link and re-run the internal-link check.

---

## 5. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Shared primitives** | `plan-marshall-persona-implementer` | `pm-dev-frontend:javascript` | Normalization, token AND matching, filter input semantics, layout persistence. |
| **Phase 2: Reports layout & filter** | `plan-marshall-persona-implementer` | `pm-dev-frontend:css`, `pm-dev-frontend:javascript` | Split/compact/comfortable, grouping after filter, pagination hint. |
| **Phase 3: Decisions layout & filter** | `plan-marshall-persona-implementer` | `pm-dev-frontend:css`, `pm-dev-frontend:javascript` | Split list/detail, waiver and standard fields, selection clearing. |
| **Phase 4: Other list views** | `plan-marshall-persona-implementer` | `pm-dev-frontend:javascript` | Overview, epic, and explorer adoption without duplicate matching rules. |
| **Phase 5: Tests & a11y** | `plan-marshall-persona-module-tester` | `pm-dev-frontend:jest-testing`, `plan-marshall-persona-integration-tester` | Unit, component, mobile, keyboard, live-region coverage. |
| **Phase 6: Docs & review** | `plan-marshall-persona-documenter` | `plan-marshall-persona-code-reviewer` | Design note, copy, report, finding triage. |

---

## 6. Phased Technical Recipe

### Phase 1: Shared Filter and Layout Primitives (`pm-web-app`)

- [ ] **1.1 `src/lib/utils/filterText.ts`**
  - `normalizeText`: `value.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase().replace(/\s+/g, ' ').trim()`; guard `null`/`undefined`.
  - `matchesQuery(query, values)`: empty/whitespace query matches everything; every token must match at least one value.
  - `buildHaystack(values)`: one normalized string per item; keep the raw values for rendering separate.
  - Export the functions with JSDoc; no DOM access.
- [ ] **1.2 `src/lib/components/FilterInput.svelte`**
  - Props: `value`, `label`, `placeholder`, `resultCount`, `onchange(value)`, optional `testid`.
  - Render `<div role="search">` with a visible or visually-hidden label, `<input type="search">`, and a clear button.
  - `oninput` emits immediately (client-only; no debounce unless profiling shows a need for large lists; if added, cap at 100 ms and test it).
  - `Esc` clears; `/` focuses unless the event target is an input, textarea, select, or contenteditable.
  - `aria-live="polite"` result text, e.g. `report.filter.results`.
- [ ] **1.3 `src/lib/stores/viewLayout.ts`**
  - `ViewLayout` union; `readLayout(view, fallback)`, `writeLayout(view, layout)` with `try/catch` around `localStorage` and an SSR guard.
  - `createViewLayout(view, fallback)` returning a Svelte store with persistence.
  - Storage keys: `pm_view_layout_reports`, `pm_view_layout_decisions`, `pm_view_layout_overview`, `pm_view_layout_epic`.
- [ ] **1.4 `src/lib/components/ViewLayoutToggle.svelte`**
  - Props: `layout`, `modes`, `onchange`.
  - Three buttons with `aria-pressed`, visible labels/icons, `data-testid="layout-{mode}"`.
  - Keyboard: native buttons; no roving tabindex needed.
- [ ] **1.5 Catalogue** in `common.{en,de}.properties`:
  - `common.filter.label=Filter`, `common.filter.placeholder=Filter as you type`, `common.filter.clear=Clear filter`, `common.filter.results={count, plural, =1 {1 match} other {{count} matches}}`, `common.filter.empty=Nothing matches the filter.`, `common.layout.label=Layout`, `common.layout.comfortable=Comfortable`, `common.layout.compact=Compact`, `common.layout.split=Split`.

### Phase 2: Reports (`ReportsView.svelte`)

- [ ] **2.1 Layout**
  - Add `const layout = createViewLayout('reports', 'comfortable')`.
  - `data-layout={$layout}` on `.reports-view`.
  - `comfortable`: `max-width: 68ch`-class measure (keep the existing `900px` as an upper bound), current spacing.
  - `compact`: reduce card padding to `--space-2 --space-3`, gaps to `--space-1`, hide the body by default and expose it through a disclosure; keep the header metadata on one line.
  - `split`: CSS grid `grid-template-columns: minmax(320px, 420px) minmax(0, 1fr)`; list pane scrolls independently; right pane hosts `CompletionReportView`; at `< 960px` collapse to one column and the existing list-or-detail behavior.
  - Remove the hard `max-width: 900px` only in `split`; retain a readable measure in the other modes.
- [ ] **2.2 Detail placement**
  - `activeDetailPlanId` remains the source of truth.
  - `comfortable`/`compact`: keep today's replacement behavior.
  - `split`: render the list and the detail side by side; `closeDetail()` clears the id and leaves the list in place.
  - Mark the active report card with `aria-current`/`report-card--active`.
- [ ] **2.3 Filter**
  - Add `query` state and a `FilterInput` above the tree.
  - Build a haystack per report from the field matrix in §2; filter `items` first, then rebuild `groupedReports` from the matches so empty project/epic groups disappear.
  - Result count uses the filtered item count; the unread pill stays based on all loaded items.
  - `hasOlder`: show `report.filter.loaded_only` while a query is active; `Load older` remains available and appends to the loaded set without clearing the query.
  - No matches: `common.filter.empty` replaces the tree; keep the pagination bar visible.
- [ ] **2.4 Catalogue** in `reports.{en,de}.properties`:
  - `report.filter.label`, `report.filter.placeholder`, `report.filter.loaded_only`, `report.filter.no_matches`.
  - Keep all copy in both locales.

### Phase 3: Decisions (`DecisionView.svelte`)

- [ ] **3.1 Layout**
  - Add `const layout = createViewLayout('decisions', 'comfortable')`.
  - `comfortable`: current cards.
  - `compact`: reduced padding/gaps, evidence collapsed behind a disclosure, badge row kept, action bar visible.
  - `split`: `grid-template-columns: minmax(300px, 380px) minmax(0, 1fr)`; left pane lists compact decision summaries (title, subject, blocks, reversibility, waiting time); right pane renders the selected decision's full card; at `< 960px` collapse to one column.
  - `selectedDecisionId` drives the split; selecting a summary sets it; a filtered-out selection is cleared and the right pane shows a non-error placeholder.
- [ ] **3.2 Filter**
  - Add `query` state and a `FilterInput` above the list.
  - Build the haystack from the field matrix in §2; filter `decisions` before rendering.
  - Keep notification banners and notice lines outside the filtered list (they are view chrome, not items).
  - Result count uses the filtered decision count; the header count stays the open-decision count.
- [ ] **3.3 Catalogue** in `decisions.{en,de}.properties`:
  - `decisions.filter.label`, `decisions.filter.placeholder`, `decisions.filter.no_matches`, `decisions.split.select`, `decisions.layout.*` if not shared.

### Phase 4: Adoption in Other Multi-Element Views

- [ ] **4.1 `OverviewView.svelte`**
  - Add a filter to the running-work plan and epic lists and to the ended-work page.
  - Fields: plan id/title/project/epic/phase/state; epic id/title/project/state; ended item title/subject/outcome/reason/date.
  - Do not filter the project summary tiles; they are navigation, not list items.
- [ ] **4.2 `EpicView.svelte`**
  - Add a filter to the workstream plan lists.
  - Fields: plan id/title/status/waits-for/phase/state and the workstream name.
- [ ] **4.3 Assess the remaining views**
  - Record in the report which multi-element views adopted the component and which deliberately did not (single-record settings/system/cleanup, forms, pickers).
  - Do not add a filter to a view that has only one record or is a form.

### Phase 5: Tests and Accessibility

- [ ] **5.1 `filterText.test.ts`**
  - case folding, diacritics (`Ärger` matches `arger`), whitespace collapse, multi-token AND, token split across fields, `null`/`undefined`, empty query.
- [ ] **5.2 `ReportsView.test.ts`**
  - filter by project, epic, plan, body, status, and created date;
  - multi-token query across fields;
  - group headers disappear when empty and reappear when cleared;
  - no-match empty state;
  - `hasOlder` hint and “Load older” retaining the query;
  - layout toggle switches modes and persists across a remount;
  - split mode keeps the list mounted when a detail opens and marks the active card;
  - unread count unaffected by the filter.
- [ ] **5.3 `DecisionView.test.ts`**
  - filter by title, subject, description, option label, evidence, waiver gate, and reason;
  - filtered-out selection clears the split detail;
  - compact mode keeps actions reachable;
  - notification banners remain outside the filtered list;
  - no-match empty state.
- [ ] **5.4 `OverviewView.test.ts` / `EpicView.test.ts`**
  - one representative field per list and the no-match state; no regression in the unfiltered order.
- [ ] **5.5 Playwright**
  - `reports.spec.js`: type a body phrase, assert the matching card and the count; assert a non-matching phrase shows the empty state; toggle `split`, open a report, assert the list is still visible; reload and assert the layout persisted.
  - `decisions.spec.js`: filter by an option/evidence phrase; assert the matching decision; toggle `compact`; assert an action remains clickable.
  - `mobile.spec.js`: the filter is usable at phone width, `split` collapses, and no horizontal scroll appears.
- [ ] **5.6 Accessibility**
  - label and `role="search"` on the filter; result count in a polite live region; `Esc` clears; `/` focuses only outside editable fields; layout toggle announces the pressed mode; focus remains visible in all three modes.

### Phase 6: Verification, Docs, Report

- [ ] **6.1 Verification**
  - `cd pm-web-app && npm test -- filterText ReportsView DecisionView OverviewView EpicView`;
  - `npm run check`, `npm run lint`;
  - the Playwright reports/decisions/mobile specs;
  - `./mvnw verify -Ppre-commit`.
- [ ] **6.2 Design note** `doc/design/web-app/view-density-and-filtering.adoc`:
  - the three layout modes and their breakpoints;
  - the filter contract (normalized substring, AND tokens, loaded data only, no network);
  - the field matrices per view;
  - the adoption rule for future multi-element views.
- [ ] **6.3 Report** `doc/agy/reports-decisions-density-and-filter-plan-report.md`: before/after width evidence, the field matrices, the test commands and results, mobile and keyboard evidence, and the adoption list.
- [ ] **6.4 Archive**: move the plan to `doc/agy/implemented/`, rebase links, update the dashboard, keep the report for Step 8.

---

## 7. Acceptance Criteria

| Finding | Acceptance criterion | Evidence |
|---|---|---|
| **F1** | `/reports` and `/decisions` offer `comfortable`, `compact`, and `split`; `split` uses the horizontal space and keeps the list visible beside the detail; the choice persists per view. | Component tests + Playwright layout/reload assertions. |
| **F1** | At `< 960px` the split collapses without horizontal scrolling. | `mobile.spec.js` and a component viewport test. |
| **F2** | Typing filters the loaded items as you type, with a result count and an explicit no-match state. | Reports/decisions/overview/epic component tests. |
| **F3** | Every field in the published matrix participates: a query matching only that field finds the item. | One test per representative field; at least one body/option/evidence field per view. |
| **F4** | Filtering makes no network request; reports show the loaded-only hint while `hasOlder`; “Load older” widens the set without clearing the query. | Spy on `apiClient` in tests; `hasOlder` assertion. |
| **F5** | One shared `filterText` + `FilterInput` powers reports, decisions, overview, and epic; no view has its own matching rules. | Diff review and import check. |
| **—** | Existing actions, forms, notifications, pagination, unread counts, and served order are unchanged when the filter is empty. | Regression tests plus Playwright. |
| **—** | Keyboard and screen-reader behavior pass; `npm run check`, `npm run lint`, focused tests, Playwright, and `./mvnw verify -Ppre-commit` pass. | CI / report. |

---

## 8. Risk & Rollback Notes

- **Split-view state**: opening a report in split mode must not unmount the list, or scroll position and the filter are lost. Keep one `activeDetailPlanId` and one list instance.
- **Decisions are interactive**: a split detail still contains forms and chat. Ensure only one instance of a decision is mounted at a time to avoid duplicate form state.
- **Filter plus pagination**: filtering only loaded items is the requirement; label it rather than silently implying a global search. Never auto-fetch on keystroke.
- **Result-count semantics**: distinguish “matches” from “open decisions” and “unread reports”; do not repurpose the header counts.
- **Performance**: build haystacks once per item list, not per keystroke; keep the filter synchronous for the current fixture sizes.
- **Layout persistence**: guard `localStorage` (private mode, SSR) and fall back to `comfortable`; an unknown stored value falls back rather than throwing.
- **CSS**: use existing tokens and container/grid queries; do not introduce a second breakpoint vocabulary.
- **Rollback**: remove the layout toggle and filter inputs, restore the single-column CSS; no data or API change to undo.
