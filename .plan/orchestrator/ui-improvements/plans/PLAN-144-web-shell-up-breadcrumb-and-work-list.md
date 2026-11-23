# PLAN-144: Web: The Lens from the Route, Up and the Breadcrumb, the Phone's Bar and the Work List (D2 Navigation Engine, Part 3)

epic: ui-improvements
workstream: WS-02

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Staged on the operator's decision of
> 2026-11-10: the D2 navigation engine is split into three parts (this is the third). With PLAN-142 it SUPERSEDES PLAN-83: the work
> list beside a plan and an epic, PLAN-83's only deliverable, is deliverable 4 here. This spec is SELF-SUFFICIENT: the emitted
> command is a one-line pointer and carries no brief.

## Objective

PLAN-142 built the column engine and PLAN-143 the address scheme. This plan wires them into the shell the operator sees: the rail shows
the lens of the current route, the top bar draws the one breadcrumb with a `KindMark` per segment and Up sits in the head of
the first visible column, the caller stack and the in-page Backs are removed, the phone's bar becomes What waits · Running
work · Decisions · More with Reports inside More, and a plan and an epic open beside the list of their siblings. Theme and
language stay in the rail foot and the More sheet (the operator, 2026-11-10; README option A).

Deliverable 0 folds in the remainder of PLAN-143 on the operator's word of 2026-11-23 (the fix plan Q7 is not staged as a plan of its own): the unwired deleted-object fall-back and three views that do not follow the address scheme.

## Deliverables

The documents are the specification; read them first: `doc/design/web-app/navigation.adoc` (#lenses, #levels, #up, #kinds,
#phone, #scope), `doc/design/web-app/README.adoc` (#shell), `doc/design/web-app/components.adoc` (#breadcrumb, #fixed-places),
`doc/design/web-app/components-tags.adoc` (#kind-mark), `doc/plans/web-moving-between-plans.adoc`, and `doc/design/web-app/navigation.adoc`
(#up :169-176, #url :216-219). D6 merged (#403, `8568b56e`) and settled Q6 (Up, the lens segment and What waits's address); the
deliverables below state its rulings.

0. **The remainder of PLAN-143 (folded in, operator 2026-11-23)**
 - What: four gaps of the address scheme that PLAN-143's landing left (`landings/PLAN-143.md`: D2, D4, L3-2, L3-3), each re-read at `3ae8a305` and still open; the scheme itself stays PLAN-143's.
   - **The deleted-object fall-back is wired.** `fallBackFromDeletedObject` (`runtime.ts:1003`) has no production caller. Call it where the runtime learns that the object an address names no longer exists (a plan, an epic, a decision, a report dropped from the served data), so the address falls back to the object's parent by a replace and the Back button does not return to the dead address. Settle the exact trigger against `history.ts` and PLAN-143's push-and-replace rule at outline.
   - **`OverviewView` follows the address, not only its mount.** The lens fields (group, scope, Ended Work, the focus) are read in `onMount` only (`OverviewView.svelte:283`), so a Running work click while Ended Work is open, or Back and Forward between two overview addresses, changes the bar and not the view. Read them from the store reactively.
   - **`ReportsView` opens a report outside the first page.** `/reports/{id}` is looked up in the first loaded page only (`ReportsView.svelte:400-413`, `items.find`), the view stays on the list without a notice, and `showDetail` picks the first row of a plan, so a second report of one plan is rewritten to the first report's id. Resolve a report that is not in the loaded page (by its id, or by loading on), keep every report of a plan addressable, and say so when a requested report cannot be opened. The old `/reports?plan=` link (`routes.ts`) and the list-to-detail replace (Back leaves Reports) are read at outline and settled with it.
   - **`ProjectExplorerView` drops its `?project=` reader.** Since PLAN-59 (#538) `/projects/{id}` is the project's address and `/projects?project=` is rewritten (`routes.test.ts:341`); `focusRequestedProject`, `focusedFilter`, the `window.location.search` read (`ProjectExplorerView.svelte:193`) and the popstate listener are unreachable and untested. Remove them.
   - Not in this deliverable, re-read and settled: `switchTab` has callers (`runtime.ts:1972,1993`); the start address without a place is the What waits list and a bare `/overview` keeps the overview (`rememberedPlace.ts:198-212`, PLAN-49) — PLAN-143's L3-1 is fixed. HYPOTHESIS: closing or replacing an `open=` object is still missing (PLAN-143 D2, no function does it) — confirm/refute at `navigation/history.ts` and `runtime.ts` (verify-at-outline); if it is missing, add it to this deliverable.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - `runtime.test.ts` drives a removed object through the store and shows the address replaced by its parent's; `OverviewView.test.ts` shows a second overview address changing the view; `ReportsView.test.ts` opens a report outside the first page, keeps two reports of one plan apart and shows the notice for an unresolvable one; `git grep -n "window.location.search" -- pm-web-app/src/lib/views/ProjectExplorerView.svelte` is empty and `focusRequestedProject` is gone. Evidence order: `test-first`.
   - `navigation.spec.js` or `reports.spec.js` shows the deep link of a report beyond the first page. Evidence order: `test-first`.
1. **The rail's lens from the route, and the phone's bar**
 - What:
   - `activeNavRoute` in `shell/nav.ts` returns the lens of the route by the kinds table of `navigation.adoc#lenses` (a
     project page is Projects; an epic, a plan, a task and a side job are Running work; a decision is Decisions; a report is
     Reports; the cleanup is System; the machine settings are Settings), and an object opened in place leaves the rail where
     it is.
   - The bottom bar is built from the lens model: Running work · Decisions · More, with **What waits** as the first item the
     moment PLAN-49 registers its route (one entry of the model, no further change here). The lens model's What waits entry, in
     the rail and in the bar, addresses `/?lens=waits` (D6: an address that names the lens, never filled from the remembered
     place, also on a reload); its Running work entry, in the rail and in the bar, opens `/overview` filled from the remembered
     place by a push, through `PLAN-143`'s Running work function. Reports moves into More, its unread
     count shows on More, and More is active while the current route's lens is one the sheet holds. The sheet keeps every
     destination and the language switch and the theme choice (the rail foot's controls); a side column takes the bar's place
     on the phone; targets are `44 px`.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `nav.test.ts`, `NavRail.test.ts` and a new `BottomBar.test.ts` show the kind-to-lens table, the bar's items, Reports and its
     count in More, More active for a lens it holds, the theme and language controls still in the sheet, What waits's entry
     addressing `/?lens=waits` (once registered), and Running work's entry pushing the filled `/overview`. Evidence order:
     `test-first`.
   - `chrome.spec.js` shows the bar on the phone and Reports in More. Evidence order: `test-first`.
2. **Up and the one breadcrumb**
 - What:
   - A chain function (`pm-web-app/src/lib/navigation/chain.ts`, new) gives the levels of a route, from the lens down (project,
     epic, plan, task), each with `kind`, title and address, from what the app already loads: a plan's project and epic from
     its process (`ProcessTree.project`, `epic`), an epic's project from its resource.
   - The top bar draws one breadcrumb: lens › project › epic › plan, each segment with its `KindMark` and a title (not a slug);
     every segment but the last pushes the parent's address as Up does (the project segment of a plan in Running work goes to
     the project's level of Running work, not to the project page); the lens segment pushes the lens's own list with nothing
     selected — the **unfilled** lens address, written with the app's mark by `PLAN-143`'s writer, so a reload keeps it unfilled
     (D6 Q6; `navigation.adoc#up` :171-172, `#url` :219); on a phone an identifier is shortened in its middle. Outside the lenses it draws its group and title (the cleanup as `Machine › System ›
     Cleanup`).
   - Up fills the `up` slot PLAN-142 left in the head of the first visible column: the parent's `KindMark` and name, accessible name
     `web.nav.up`, a push of the parent's address, the same at every width, absent at a lens list. Up from a project of a lens
     pushes the lens's unfilled list, as the lens segment does, never filled from the remembered place.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `chain.test.ts`, `Breadcrumb.test.ts`, `TopBar` and `Up` tests: a plan of an epic, a plan of no epic, a project's plan
     in the ended mode, a phone, each segment's address, and Up pushing the parent; the lens segment and Up from a project push
     the unfilled lens list with the app's mark, and a remembered place does not fill it. Evidence order: `test-first`.
3. **The caller stack and the in-page Backs go**
 - What:
   - Remove `navigationStack`, `NavigationEntry` and `goBack` from `runtime.ts` and the stack push of `pushRoute`.
   - Remove the in-page Back of `PlanView`, `EpicView`, `SettingsView`, `MachineSettingsView`, `CompletionReportView`,
     `OverviewView`, `ProjectOnboardingView`, `EpicAdoptView`, `EpicDecompositionView` and `CleanupView`
     (`system.cleanup.back_to_system`). What remains: the Back of a wizard, the cancel of a dialogue, and the cleanup's
     `system.cleanup.preview.back` (_Back to the job_, which the views package completes with the in-place hosting).
   - Remove the catalogue keys no view uses afterwards (`common.actions.back*`, `web.settings.back`, `web.common.back` where
     unused, `system.cleanup.back_to_system`), in English and German, and keep the text guard baseline from growing.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - `git grep -n "goBack\|navigationStack" -- pm-web-app/src` is empty and each named view's test asserts the control gone and the
     same place reachable by Up or the breadcrumb. Evidence order: `test-first`.
   - The e2e specs that clicked those Backs follow (`plan-view.spec.js`, `epic-view.spec.js`, `overview.spec.js`,
     `onboarding.spec.js`, `system.spec.js`, `projects.spec.js`). Evidence order: `test-first`.
4. **The work list beside a plan and an epic (PLAN-83's acceptance)**
 - What:
   - The plan and the epic route are placed on the column engine as a chain: the column to the left of the plan lists its
     siblings with it selected (its epic's plans, or its project's work), in the order the runtime serves (PLAN-82) and never
     sorted again, each row an `aria-current` link; a click on a sibling is a replace (a sibling in the same column) and opens
     its plan view; the same for an epic.
   - The existing `PlanView` and `EpicView` stand in the object column as they are (their rework is PLAN-55, 58 and 62); a
     narrow window shows one column and no strip.
   - The row component is a small `WorkListColumn` that PLAN-52 grows into the lens's list column.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - Unit tests show the list in the order served, the open item marked, one step to a neighbour opening its plan view, and the same on an
     epic. Evidence order: `test-first`.
   - The row `web-moving-between-plans` of `doc/plans/README.adoc` is set `done` by the last of this plan, the `web-decision-announcer`
     plan and `PLAN-59` D4 (state in the landing message which it is). Evidence order: `none`.
5. **Browser evidence**
 - What: `pm-web-e2e/tests/work-list.spec.js` (new) on `running-work`: from `fix-relay` one step to its neighbour and back by
   Up, the breadcrumb segments, the lens segment opening the unfilled list and a reload of it staying unfilled, the bar on the
   phone.
 - Persona: plan-marshall:persona-integration-tester
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - `work-list.spec.js` passes with `PM_MOCK_WEB_PORT` and `PM_WEB_PORT` set; it waits on the address and the DOM. Evidence order:
     `test-first`.
   - Browser check (Playwright MCP) on `running-work` at `1440 × 900` and `390 × 844`, German and English, both themes. Evidence
     order: `none`.

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`): `./mvnw -pl pm-api,pm-web-app verify`, `./mvnw -pl pm-api,pm-web-app verify -Ppre-commit` (review every rewrite),
  `PM_MOCK_WEB_PORT=<mock port> PM_WEB_PORT=<app port> ./mvnw -pl pm-web-e2e verify -Pe2e` at the head.
- No new dependency or plugin. Web standards of `CLAUDE.md`; no static `style` attribute; every new text from the catalogue in
  English and German; the text guard baseline does not grow. Pre-1.0: no shim for the removed Backs or keys.
- Frontend tests on Vitest with Testing Library (`pm-dev-frontend:jest-testing`).
- Ports of this plan: mock web port 21440, app port 21441 (`20000 + 10 × 144`) (`AGENTS.md` § Ports). Other plans run at the same
  time: never stop or reuse a process this worktree did not start.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.
- The merge rule: the run merges only when every review thread has a reply and is resolved and every review body is
  answered, and some head has had one CodeRabbit review. The pull request body states thread count, resolved count and review-body
  items from `ci pr comments`, never from memory. The run merges the pull request itself once these gates hold, without a further word of the operator (`CLAUDE.md`, Git Workflow, #423). Commits end with
  `Co-Authored-By: plan-marshall <noreply@cuioss.de>`; the pull request body carries no generator footer. If CodeRabbit refuses on
  quota, stop and ask the operator.
- ZERO Sonar findings (the operator, 2026-11-05): the pull request merges with 0 open new-code issues, every open issue fixed in
  a commit of THIS pull request. State `sonar_open_new_issues=N` in the landing message. A dismissal needs the operator's word.

## Claim Labels

- OBSERVED: `activeNavRoute` maps plan, epic, decomposition and start onto the overview, the cleanup onto the system, the machine settings onto the settings, the project onboarding onto the projects; `BOTTOM_IDS` is `running`, `decisions`, `reports`; `pathOf` builds a group › title trail from static keys — read at `pm-web-app/src/lib/shell/nav.ts:120,136-205`
  - verdict: contradicted | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: yes | evidence: BOTTOM_IDS is needs, running, decisions; activeNavRoute mapping holds; D1 bar work largely on main (#461, #459); nav.ts unchanged since 7b60cec0 (origin/main 3ae8a305)
- OBSERVED: `RAIL_GROUPS` has no What waits entry and no `needs` route exists — read at `pm-web-app/src/lib/shell/nav.ts:42-112`, `pm-web-app/src/lib/routes.ts:20-39`
  - verdict: contradicted | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: yes | evidence: RAIL_GROUPS has a needs item (route inbox, What waits) first; inbox route exists; PLAN-49 shipped (#461); nav.ts unchanged since 7b60cec0 (origin/main 3ae8a305)
- OBSERVED: `BottomBar` holds Running, Decisions, Reports and More, and its sheet carries the language and theme controls — read at `pm-web-app/src/lib/shell/BottomBar.svelte:1-60`
  - verdict: contradicted | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: yes | evidence: BottomBar BOTTOM_ITEMS needs, running, decisions then More; Reports in More with its count; sheet keeps language and theme; BottomBar.svelte unchanged since 7b60cec0 (origin/main 3ae8a305)
- OBSERVED: `TopBar` builds its crumbs from `pathOf` and draws `Breadcrumb` with no `KindMark` and no `href` — read at `pm-web-app/src/lib/shell/TopBar.svelte:29,42-48`; `Breadcrumb.svelte` (`Crumb` has `label`, `href`, `id`)
  - verdict: corroborated | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: TopBar builds crumbs from pathOf, label and id only, no KindMark, no href; the only change since 7b60cec0 is the loopback helper (PLAN-156) (origin/main 3ae8a305)
- OBSERVED: `goBack` is called by `EpicView.svelte:171`, `PlanView.svelte:457`, `SettingsView.svelte:345`; the Back controls are at `EpicView.svelte:554`, `PlanView.svelte:712`, `OverviewView.svelte:645`, `SettingsView.svelte:351`, `MachineSettingsView.svelte:385,648`, `CompletionReportView.svelte:146`, `ProjectOnboardingView.svelte:421`, `EpicAdoptView.svelte:34`, `EpicDecompositionView.svelte:269`, `CleanupView.svelte:333` (in-place `preview.back` at :382)
  - verdict: contradicted | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: yes | evidence: goBack callers PlanView:593 and SettingsView:1064; remaining in-page Backs Overview:828, Plan:862, Settings:1070,1671, CompletionReport:317, ProjectOnboarding:539, EpicAdopt:37, Decomposition:324, Cleanup:434; the list shrinks, the work is the same (origin/main 3ae8a305)
- OBSERVED: `ProcessTree` carries `project` and `epic` references — read at `pm-api/src/main/java/de/planmarshall/api/process/ProcessTree.java:44-60`
  - verdict: corroborated | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: ProcessTree record has ProcessProjectRef project and nullable ProcessEpicRef epic; pm-api unchanged in that file (origin/main 3ae8a305)
- OBSERVED: `KindMark`, `Link`, `RowLink` do not exist under `pm-web-app/src/lib/components/` — read by `git ls-files pm-web-app/src/lib/components`
  - verdict: contradicted | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: yes | evidence: KindMark, Link and RowLink exist in components/ (#420, #427); TopBar still does not use them (origin/main 3ae8a305)
- OBSERVED: the language and theme controls stand in the rail foot and the More sheet by README option A — read at `doc/design/web-app/README.adoc:147,153,431`
  - verdict: corroborated | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: README.adoc rail foot and More sheet carry language and theme (lines shifted by +2 with PLAN-156); NavRail and BottomBar match (origin/main 3ae8a305)
- HYPOTHESIS: a project's plans and epics, in the order served, are readable from the data `getProjectDetail` already returns, so the work list needs no new endpoint — confirm/refute at `pm-web-app/src/lib/api/client.ts` § `getProjectDetail` (:2730) and `ProjectDetailResponse` (verify-at-outline)
  - verdict: corroborated | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: client.ts ProjectDetailResponse has plans and epics; client.ts unchanged since 7b60cec0 (origin/main 3ae8a305)
- HYPOTHESIS: an epic's project is known from `getEpicResource` — confirm/refute at `pm-web-app/src/lib/api/client.ts` § `getEpicResource` (:3171) and `EpicResource` (verify-at-outline)
  - verdict: corroborated | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: client.ts EpicResource has project_id; getEpicResource present; unchanged (origin/main 3ae8a305)
- HYPOTHESIS: no view other than the ten named renders an in-page Back — confirm/refute by `git grep -n "common.actions.back\|web.common.back\|web.settings.back" -- pm-web-app/src/lib/views` (verify-at-outline)
  - verdict: corroborated | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: remaining in-page Backs are the named views plus StartView (:1387) and OnboardingView's wizard onBack (:290); key is common.action.back (origin/main 3ae8a305)
- OBSERVED: D6 settled Up and the lens segment (the lens's own list with nothing selected, pushed with the app's mark, a reload keeps it unfilled) and What waits's address `/?lens=waits` — read at `doc/design/web-app/navigation.adoc:171-172,216-219`, `doc/design/web-app/README.adoc:445-449`, `doc/design/web-app/needs-and-find.adoc:26` (`4a84f727`, D6 merged `8568b56e`)
  - verdict: corroborated | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: navigation.adoc #up and #url, README.adoc, needs-and-find.adoc unchanged in substance (origin/main 3ae8a305)
- Verify-first clause: re-read `navigation.adoc#up` and `#url`, PLAN-142's `up` slot and PLAN-143's unfilled-lens and Running-work functions at the then-current HEAD. If PLAN-49 has landed, add What waits to the bar and the rail in deliverable 1; if PLAN-130 has not, stop and loop back.
  - verdict: unverifiable | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: procedural; PLAN-49, 130, 143 shipped; PLAN-59, 154, 156 shipped (origin/main 3ae8a305)

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/shell/nav.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/shell/nav.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/shell/NavRail.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/shell/NavRail.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/shell/BottomBar.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/shell/BottomBar.test.ts` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/shell/TopBar.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/shell/AppShell.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/Breadcrumb.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/Breadcrumb.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/navigation/chain.ts` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/navigation/chain.test.ts` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/templates/UpLink.svelte` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/WorkListColumn.svelte` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/stores/runtime.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/stores/runtime.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/PlanView.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/EpicView.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/SettingsView.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/MachineSettingsView.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/CompletionReportView.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/OverviewView.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/ProjectOnboardingView.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/EpicAdoptView.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/EpicDecompositionView.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/CleanupView.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/api/client.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/common.en.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/common.de.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/settings.en.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/system.en.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/work-list.spec.js` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-e2e/tests/chrome.spec.js` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/mobile.spec.js` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/plan-view.spec.js` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/epic-view.spec.js` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/overview.spec.js` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/system.spec.js` — present at HEAD 1cc0e10e
- OBSERVED: `doc/plans/README.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `doc/plans/web-moving-between-plans.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/settings.de.properties` — present at `dc811fb3` (added by the cleanup of 2026-11-14)
- OBSERVED: `pm-api/src/main/resources/messages/system.de.properties` — present at `dc811fb3` (added by the cleanup of 2026-11-14)
- OBSERVED: `pm-web-e2e/tests/onboarding.spec.js` — present at `dc811fb3` (added by the cleanup of 2026-11-14)
- OBSERVED: `pm-web-e2e/tests/projects.spec.js` — present at `dc811fb3` (added by the cleanup of 2026-11-14)
- OBSERVED: `pm-web-app/src/lib/views/PlanView.test.ts` — present at `dc811fb3` (added by the cleanup of 2026-11-14)
- OBSERVED: `pm-web-app/src/lib/views/EpicView.test.ts` — present at `dc811fb3` (added by the cleanup of 2026-11-14)
- OBSERVED: `pm-web-app/src/lib/views/SettingsView.test.ts` — present at `dc811fb3` (added by the cleanup of 2026-11-14)
- OBSERVED: `pm-web-app/src/lib/views/MachineSettingsView.test.ts` — present at `dc811fb3` (added by the cleanup of 2026-11-14)
- OBSERVED: `pm-web-app/src/lib/views/CompletionReportView.test.ts` — present at `dc811fb3` (added by the cleanup of 2026-11-14)
- OBSERVED: `pm-web-app/src/lib/views/OverviewView.test.ts` — present at `dc811fb3` (added by the cleanup of 2026-11-14)
- OBSERVED: `pm-web-app/src/lib/views/ProjectOnboardingView.test.ts` — present at `dc811fb3` (added by the cleanup of 2026-11-14)
- OBSERVED: `pm-web-app/src/lib/views/EpicDecompositionView.test.ts` — present at `dc811fb3` (added by the cleanup of 2026-11-14)
- OBSERVED: `pm-web-app/src/lib/views/CleanupView.test.ts` — present at `dc811fb3` (added by the cleanup of 2026-11-14)
- OBSERVED: `pm-web-app/src/lib/templates/TriagePage.svelte` — present at `0b9402ec` (PLAN-142 landed; folded by the analyze of 2026-11-15)
- OBSERVED: `pm-web-app/src/lib/templates/TriagePage.test.ts` — present at `0b9402ec` (folded by the analyze of 2026-11-15)
- OBSERVED: `pm-web-app/src/lib/templates/SideColumn.svelte` — present at `0b9402ec` (folded by the analyze of 2026-11-15)
- OBSERVED: `pm-web-app/src/lib/views/EpicAdoptView.test.ts` — present at HEAD 7b60cec0 (cleanup 2026-11-22, declared surface understated)
- OBSERVED: `pm-web-app/src/lib/views/ReportsView.svelte` — present at HEAD 3ae8a305 (folded D0, 2026-11-23)
- OBSERVED: `pm-web-app/src/lib/views/ReportsView.test.ts` — present at HEAD 3ae8a305 (folded D0, 2026-11-23)
- OBSERVED: `pm-web-app/src/lib/views/ProjectExplorerView.svelte` — present at HEAD 3ae8a305 (folded D0, 2026-11-23)
- OBSERVED: `pm-web-app/src/lib/views/ProjectExplorerView.test.ts` — present at HEAD 3ae8a305 (folded D0, 2026-11-23)
- OBSERVED: `pm-web-app/src/lib/navigation/history.ts` — present at HEAD 3ae8a305 (folded D0, 2026-11-23)
- OBSERVED: `pm-web-app/src/lib/routes.ts` — present at HEAD 3ae8a305 (folded D0, 2026-11-23)
- OBSERVED: `pm-web-e2e/tests/reports.spec.js` — present at HEAD 3ae8a305 (folded D0, 2026-11-23)

## Dependencies and Sequencing

- Depends on: `PLAN-142` (the `up` slot, the engine), `PLAN-143` (the address scheme and the history writer; its remainder is deliverable 0); the D1 foundation part with `KindMark`, `RowLink` and `Link`; `PLAN-130` landed (`NavRail.svelte`, `chrome.spec.js`); the D6 documents (Q6) — merged (#403, `8568b56e`), satisfied.
- Overlaps with:
  - `PLAN-49` (parked): `shell/nav.ts`, `NavRail.svelte`, `BottomBar.svelte`, `routes.ts`, `+layout.svelte`, `chrome.spec.js`. This plan lands first; PLAN-49 D3 then adds only What waits, its count, the tab title and the tab icon.
  - `PLAN-130` (staged): `NavRail.svelte`, `chrome.spec.js`. It lands first.
  - `PLAN-85` (parked): `NavRail.svelte`, `chrome.spec.js`.
  - `PLAN-60` (parked): `activeNavRoute` for the machine settings.
  - `PLAN-52`, `PLAN-55`, `PLAN-58` (parked): `OverviewView`, `PlanView`, `EpicView`. They follow.
- Adjacent to: `PLAN-83` (superseded: its acceptance is deliverable 4); the views package (the cleanup's in-place hosting of objects); `PLAN-145`, `PLAN-146` (the Intent tabs are placed by PLAN-55 and PLAN-58).

## Re-Grounding at Restart

Re-read every file above at the then-current HEAD (this spec was grounded at `1cc0e10e`). Re-run `corpus surfaces` against everything that landed since (PLAN-130, PLAN-142, PLAN-143 and the D1 foundation first). Settle each HYPOTHESIS.

## Re-Grounding 2026-11-14 (cleanup)

Verdicts persisted per claim at `dc811fb3` (`corpus set-verdict`, by `ui-improvements/cleanup`). Read before the outline:

- Claim 6 contradicted: KindMark.svelte, Link.svelte and RowLink.svelte all exist in components/ at HEAD (PLAN-147 #420, PLAN-148 #427)
- KindMark, Link and RowLink exist (#420, #427).

## Re-Grounding 2026-11-22 (cleanup, at `7b60cec0`)

Verdicts persisted per claim by `ui-improvements/cleanup` (`corpus set-verdict`, origin/main `7b60cec0`; sibling repos read at their local refs only). Contradicted claims are stamped `rescoped: yes` with the corrections below; read before the outline:

- Claims 0, 1, 2, 4 and 6 contradicted (`rescoped: yes`): **D1 is mostly DONE** (What waits first in the bar and rail, `nav.ts` `BOTTOM_IDS`, `RAIL_GROUPS` `needs`/`inbox`; Reports moved into More with its count: PLAN-49 #461, PLAN-52 #459; `BottomBar.test.ts` EXISTS, so it is extended, not created); **D3 partly** (EpicView Back gone #456, `MachineSettingsView` deleted #457: its row and test leave the surface; `goBack` callers are only `PlanView:593` and `SettingsView:1066`, the Back controls that remain are `Overview:828`, `Plan:862`, `Settings:1072,1673`, `CompletionReport:150`, `ProjectOnboarding:426`, `EpicAdopt:37`, `Decomposition:324`, `Cleanup:434`); KindMark, Link and RowLink already exist (#420, #427) and are the dependency line's.
- NOT done: D2 (no `chain.ts`, no `UpLink`, no `web.nav.up` key, nothing fills `TriagePage`'s `up`, `TopBar` draws no KindMark), D4 (no `WorkListColumn`; PlanView and EpicView are not on TriagePage), D5. The folded shell-height item is open (`AppShell` `min-block-size: 100vh`, `TriagePage:579`); the modal boundary (`inert`/`aria-modal`) is absent. The Back-key names are `common.action.back` (singular).
- The PLAN-143 fix plan (uncalled `fallBackFromDeletedObject`, `ProjectExplorerView.svelte:87` reading `location.search`) is still undecided and sequenced against this plan.

## Re-Grounding 2026-11-23 (analyze, at `3ae8a305`)

The three landings of 2026-11-23 (PLAN-59 #538, PLAN-154 #539, PLAN-156 #536) touched this spec's surface (`TopBar.svelte` only by the shared loopback helper, `runtime.ts`, `SettingsView.svelte`, `ProjectOnboardingView.svelte`, `CompletionReportView.svelte`, `chrome.spec.js`, `common.*`, the README); the shell files (`nav.ts`, `NavRail`, `BottomBar`, `Breadcrumb`), `client.ts` and `navigation.adoc` did not move since `7b60cec0`. Verdicts re-stamped per claim at `3ae8a305` (`corpus set-verdict`, by `ui-improvements/analyze`):

- Claims 0, 1, 2, 4, 6 stay contradicted with their `rescoped: yes` corrections; claim 4's lists are refreshed: `goBack` callers are `PlanView.svelte:593` and `SettingsView.svelte:1064`; the in-page Back controls that remain are `OverviewView:828`, `PlanView:862`, `SettingsView:1070,1671`, `CompletionReportView:317`, `ProjectOnboardingView:539`, `EpicAdoptView:37`, `EpicDecompositionView:324`, `CleanupView:434` (the key is `common.action.back`, and `system.cleanup.back_to_system`); `StartView` and `OnboardingView` keep their own wizard `goBack`, which stays. `MachineSettingsView` is deleted.
- Claim 3 holds: `TopBar` still builds its crumbs from `pathOf`, with no `KindMark` and no `href`.
- Deliverable 0 is new: the PLAN-143 fix plan Q7 was decided on 2026-11-23 as a fold into this plan; its premises were re-read at `3ae8a305` (see D0; two of the old premises are stale and said so there).
- Sequencing: PLAN-59 (which edits `routes.ts` and `runtime.ts`) has landed, so this plan no longer waits for it; PLAN-174 (CLI) is disjoint (0 shared paths by `corpus surfaces`).

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-144-web-shell-up-breadcrumb-and-work-list.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger write. The session reports its outcome through its PR and its inbox message (`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary).

## Staging Note 2026-11-10

The packages this spec names by placeholder were staged on 2026-11-10: KindMark and Link are PLAN-147; RowLink is PLAN-148.

## Folded from the PLAN-142 landing 2026-11-15 (analyze)

PLAN-142 merged as `0b9402ec` (#442) and handed three things to the shell that this spec did not yet carry. Each was re-read on the merged tree; fold them into the shell work, they widen the surface by `TriagePage.svelte`, `TriagePage.test.ts` and `SideColumn.svelte` (declared above):
- **A definite shell height, so each column scrolls on its own.** `TriagePage.svelte:439-440` declares `max-block-size: 100%; overflow-y: auto` per column, which resolves to no cap while `AppShell.svelte` gives only `min-block-size: 100vh`. Give the shell a definite block size and prove a viewport-tall column scrolls inside itself, not the page. A measured precondition of `row-menu.spec.js` broke when the cap was tried in PLAN-142, so run it.
- **The phone chat overlay as a modal boundary.** `SideColumn.svelte:70` makes the phone overlay `position: fixed` inside `.triage-page__window`, which animates `transform`; the background stays tabbable and unmarked (no `inert`, no `aria-modal`). The overlay belongs at the shell level above the bottom bar, with the background inert and the focus kept inside.
- **A correction to deliverable 2's wording:** the `up` slot PLAN-142 left is the `up` snippet prop of `TriagePage.svelte` (`:78`, `:369`), in the head of the first visible column, as `navigation.adoc#up` says. `ObjectPage.svelte` has no `up` slot. Fill the `TriagePage` prop.

## Unparked 2026-11-14 (cleanup, operator)

Emit after `PLAN-142` and `PLAN-143` land (D6 met, `PLAN-130` shipped).
- Re-ground at outline: claim 6 is stale, since `KindMark`, `Link` and `RowLink` exist.
- It overlaps `PLAN-49` and `PLAN-85` on `NavRail`/`chrome.spec`; 144 goes first.
