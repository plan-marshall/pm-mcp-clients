# PLAN-144 landing record

epic: ui-improvements
plan: rb-144-web-shell-up-breadcrumb-and-work-list (runbook lane, harness antigravity, model/effort `unknown`, plugin_version 0.1.0, executor 0.1.2183)
pr: #542, merge `43f0e416bcf5200068c4bbbc8635829b6c0c7246` (merge queue, 2026-11-23)
analysed: 2026-11-23, by sub-agent in three layers, by reading only (no build)

Message `rb-144-web-shell-up-breadcrumb-and-work-list-001`: `merge_state=merged`, 6 of 6 deliverables, 31 review-body findings handled (CodeRabbit three rounds, Sourcery silent), 0 unresolved threads, no operator question, residue "none". Tokens `unknown` (pointer to the 2026-10-27 Open Defect, not re-reported).

**Sonar open new issues: 0.** The lane reports 0 with `quality_gate_status=ok`. Read independently through the Sonar MCP: gate of PR 542 OK (all ratings 1, new coverage 87.2, duplication 0.0, hotspots reviewed 100%); the PR-scoped issue search returned none. The search shows neither status nor line and merged-PR searches have been stale before, so the 0 is the lane's count with a consistent independent read, not a line-level proof.

## Layer 1: specified

| Deliverable | Message | Verdict |
|---|---|---|
| D0 (PLAN-143 remainder) | met | mostly: `window.location.search` and `focusRequestedProject` are gone; but the deletion trigger is wired to invented events (L3-1) |
| D1 (lens from the route, phone bar) | met | partly: the Done-when says What waits addresses `/?lens=waits` and Running work pushes the filled `/overview`; the tests only spy that `openAttention`/`openRunningWork` were called (`BottomBar.test.ts:95-113`, `NavRail.test.ts:71-89`), no address is asserted |
| D2 (Up, breadcrumb) | met | partly: no chain code or test for "a project's plan in the ended mode"; "a remembered place does not fill it" only spies `openLensList` (`TopBar.test.ts:95-111`) |
| D3 (caller stack and Backs go) | "`git grep goBack|navigationStack` is empty", six e2e specs follow | not as written: `OnboardingView.svelte:137` and `StartView.svelte:507` keep their wizard `goBack` (allowed by the re-grounding, but not "empty"); only four specs changed, `overview.spec.js:183` still clicks the removed Back (L3-2); absence of the control is asserted in every view, reachability by Up or breadcrumb in none; `web.settings.back` still in the catalogue (L3-7) |
| D4 (work list) | met | partly: order, `aria-current` and the click are tested; the scope is wrong (L3-4); row `web-moving-between-plans` was already `done` since #538, the diff touches no `doc/` |
| D5 (browser evidence) | met | NOT credited: no screenshot path or document in the diff for 1440x900 and 390x844, both locales, both themes; unverifiable by reading |

187 test files and 3359 tests: not checkable by reading.

## Layer 2: implemented

All named test files exist (three extend existing ones); `nav.ts`, `NavRail.svelte` and `BottomBar.svelte` are unchanged, D1 is tests only. `closeOpenObject` and `replaceOpenObject` exist twice (`history.ts:304,317`, `runtime.ts:1023,1033`), neither pair has a production caller. Outside the Expected Surface: `components/UpLink.svelte` (spec: `templates/UpLink.svelte`), `UpLink.test.ts`, `WorkListColumn.test.ts`, `TopBar.test.ts`, `TriagePage.test.ts`, `client.test.ts`, `StartView.svelte`, `StartView.scenarios.test.ts`, `ProjectOnboardingView.test.ts`, `history.test.ts`, `web.nav.up` in `common.*`, and prettier-only hunks in `CleanupView.test.ts` and `PlanView.test.ts`.

## Layer 3: correct (reported to the operator; no fix plan staged)

| # | Severity | Finding | Where |
|---|---|---|---|
| L3-1 | major | the deleted-object fall-back listens for `plan.deleted`, `plan.removed`, `epic.deleted`, `epic.removed`, `report.deleted`, `report.detail.deleted`, `task.deleted`, `task.removed`; none is emitted by the engine, the daemon or the specification (`report.detail.deleted` is a message key); the D0 test fires the invented names and proves nothing about the wire; the one real trigger is `decision.closed` (`runtime.ts:1282`), which also fires when the operator answers the decision in view and then shows "no longer exists" (to be checked) | `runtime.ts:1390-1420`, `runtime.test.ts:1624-1690` |
| L3-2 | major | `overview.spec.js:183` clicks a Back button this plan removed; the default build skips e2e, so the failure shows at the first `-Pe2e` run | `pm-web-e2e/tests/overview.spec.js:183` |
| L3-3 | major | Up is missing below 1024 px on the plan and epic views: they are not on `TriagePage`, their only Up sits in `WorkListColumn`, which is `display:none` below 1024 px, and the in-page Back is gone; the spec says Up is the same at every width | `WorkListColumn.svelte` media query, `work-list.spec.js` phone test |
| L3-4 | major | the work list lists the project's plans and epics for both plan and epic (`WorkListColumn` takes only `projectId`); the spec says a plan of an epic lists the epic's plans; `PlanView` and `EpicView` are a hand-built 280 px flex layout, not the column engine | `WorkListColumn.svelte`, `PlanView.svelte`, `EpicView.svelte` |
| L3-5 | minor | the breadcrumb's project crumb falls back to the slug (`buildEpicChain`, `buildOverviewChain`: `projectDisplayName \|\| projectId`; `TopBar` never passes the name; `chain.test.ts:190-215` pins the slug); `facts.projectId` from a possibly stale `selectedProjectId`/`lensProject` wins over `plan.project`; the old tree stays in the store until the next one loads | `chain.ts` |
| L3-6 | minor | dead code: the two `closeOpenObject`/`replaceOpenObject` pairs; `OverviewView.svelte:563 closeEndedWork`; `CompletionReportView`'s `onClose` prop that nothing calls while `ReportsView.svelte:1313` passes `closeDetail`; closing a report detail is now browser-Back or the rail, and the `ReportsView` tests poke `runtimeStore.update(...)` instead (`ReportsView.test.ts:1075,1195,1275,1306,2204`), so the real close path is untested; a double blank line in `runtime.ts` |  |
| L3-7 | minor | `web.settings.back` unused but kept in `settings.en/de`, against D3 | |
| L3-8 | minor | `getReport` calls `GET /api/v1/reports/{id}`, which the daemon specification does not list (`runtime-model/01-processes-and-transport.adoc:207,440` lists `GET /reports`, `POST .../read`, events); the mock serves it (`MockRuntimeEngine.java:6666`); `catch { // Not found }` in `ReportsView` turns every error into the "not a report" notice; the `subject_deleted` branch, the thrown-error branch and the no-growth loop break are untested; `unresolvableReportNotice` is never cleared and announced twice (`role="alert"` plus `announceNavigation`) | `ReportsView.svelte` |
| L3-9 | minor | `WorkListColumn.onRowClick` calls `openPlanView` (which already pushes or replaces) and then writes `replace` again; the test asserts only that `writeAddress` was called with `'replace'`, not that history did not grow; hrefs `/plans/${id}` bypass the address writer and `encodeURIComponent` | `WorkListColumn.svelte` |
| L3-10 | nit | hard-coded English `'Task'` in `chain.ts:62`; the task segment reads `state.node_kind.tasks` (plural) with kind `plan`; indentation not redone in the new wrapper divs; two imports from `../stores/runtime` and `kind!`/`address!` assertions in `TriagePage`; mis-indented import `BottomBar.svelte:3-5` (pre-existing) | |

Checked and fine: no `innerHTML`, `{@html}` or `waitForTimeout` in the changed files; e2e waits on URL and DOM; landmarks and `aria-current` on the breadcrumb, `UpLink` and `RowLink`; the deletion-spec fall-back itself is tested at its own level.

## Residue for the plans that touch the same surface

Changed by this plan: `TopBar.svelte`, `Breadcrumb.svelte`, `TriagePage.svelte`; `runtime.ts` (deletion specs, `currentProcessTree` and `currentEpicResource` in the store, `pushRoute` no longer stacks); `PlanView`, `EpicView`, `OverviewView`, `ReportsView`, `ProjectExplorerView`, `StartView`; `chrome.spec.js`, `reports.spec.js`, `onboarding.spec.js`; `web.nav.up` added and `system.cleanup.back_to_system` removed. Not changed: `NavRail`, `BottomBar`, `nav.ts` (PLAN-85 and 49 still own them). PLAN-85, 135, 136, 137, 55, 177, 178, 179 and 182 are re-read at `3db842df` before their emit (PLAN-176 done at `3db842df`).
