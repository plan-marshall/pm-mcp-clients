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

## Deliverables

The documents are the specification; read them first: `doc/design/web-app/navigation.adoc` (#lenses, #levels, #up, #kinds,
#phone, #scope), `doc/design/web-app/README.adoc` (#shell), `doc/design/web-app/components.adoc` (#breadcrumb, #fixed-places),
`doc/design/web-app/components-tags.adoc` (#kind-mark), `doc/plans/web-moving-between-plans.adoc`, and the D6 documents (Q6:
the lens segment).

1. **The rail's lens from the route, and the phone's bar**
 - What:
   - `activeNavRoute` in `shell/nav.ts` returns the lens of the route by the kinds table of `navigation.adoc#lenses` (a
     project page is Projects; an epic, a plan, a task and a side job are Running work; a decision is Decisions; a report is
     Reports; the cleanup is System; the machine settings are Settings), and an object opened in place leaves the rail where
     it is.
   - The bottom bar is built from the lens model: Running work · Decisions · More, with **What waits** as the first item the
     moment PLAN-49 registers its route (one entry of the model, no further change here). Reports moves into More, its unread
     count shows on More, and More is active while the current route's lens is one the sheet holds. The sheet keeps every
     destination and the language switch and the theme choice (the rail foot's controls); a side column takes the bar's place
     on the phone; targets are `44 px`.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `nav.test.ts`, `NavRail.test.ts` and a new `BottomBar.test.ts` show the kind-to-lens table, the bar's items, Reports and its
     count in More, More active for a lens it holds, and the theme and language controls still in the sheet. Evidence order:
     `test-first`.
   - `chrome.spec.js` shows the bar on the phone and Reports in More. Evidence order: `test-first`.
2. **Up and the one breadcrumb**
 - What:
   - A chain function (`pm-web-app/src/lib/navigation/chain.ts`, new) gives the levels of a route, from the lens down (project,
     epic, plan, task), each with `kind`, title and address, from what the app already loads: a plan's project and epic from
     its process (`ProcessTree.project`, `epic`), an epic's project from its resource.
   - The top bar draws one breadcrumb: lens › project › epic › plan, each segment with its `KindMark` and a title (not a slug);
     every segment but the last pushes the parent's address as Up does, and the lens segment as D6 settled; on a phone an
     identifier is shortened in its middle. Outside the lenses it draws its group and title (the cleanup as `Machine › System ›
     Cleanup`).
   - Up fills the `up` slot PLAN-142 left in the head of the first visible column: the parent's `KindMark` and name, accessible name
     `web.nav.up`, a push of the parent's address, the same at every width, absent at a lens list.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `chain.test.ts`, `Breadcrumb.test.ts`, `TopBar` and `Up` tests: a plan of an epic, a plan of no epic, a project's plan
     in the ended mode, a phone, each segment's address, and Up pushing the parent. Evidence order: `test-first`.
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
   Up, the breadcrumb segments, the bar on the phone.
 - Persona: plan-marshall:persona-integration-tester
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - `work-list.spec.js` passes with `PM_MOCK_WEB_PORT` and `PM_WEB_PORT` set; it waits on the address and the DOM. Evidence order:
     `test-first`.
   - Browser check (Playwright MCP) on `running-work` at `1440 × 900` and `390 × 844`, German and English, both themes. Evidence
     order: `none`.
   - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for
     a failed read, never `0`); a confirmed `0` or every open issue dismissed. Evidence order: `none`.

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`): `./mvnw -pl pm-api,pm-web-app verify`, `./mvnw -pl pm-api,pm-web-app verify -Ppre-commit` (review every rewrite),
  `PM_MOCK_WEB_PORT=<mock port> PM_WEB_PORT=<app port> ./mvnw -pl pm-web-e2e verify -Pe2e` at the head.
- No new dependency or plugin. Web standards of `CLAUDE.md`; no static `style` attribute; every new text from the catalogue in
  English and German; the text guard baseline does not grow. Pre-1.0: no shim for the removed Backs or keys.
- Frontend tests on Vitest with Testing Library (`pm-dev-frontend:jest-testing`).
- Ports of this plan: mock web port 21440, app port 21441 (`20000 + 10 × 144`) (`AGENTS.md` § Ports). Other plans run at the same
  time: never stop or reuse a process this worktree did not start.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.
- The merge rule: the merge question is asked only when every review thread has a reply and is resolved and every review body is
  answered, and some head has had one CodeRabbit review. The pull request body states thread count, resolved count and review-body
  items from `ci pr comments`, never from memory. Merge only on the operator's word. Commits end with
  `Co-Authored-By: plan-marshall <noreply@cuioss.de>`; the pull request body carries no generator footer. If CodeRabbit refuses on
  quota, stop and ask the operator.
- ZERO Sonar findings (the operator, 2026-11-05): the pull request merges with 0 open new-code issues, every open issue fixed in
  a commit of THIS pull request. State `sonar_open_new_issues=N` in the landing message. A dismissal needs the operator's word.

## Claim Labels

- OBSERVED: `activeNavRoute` maps plan, epic, decomposition and start onto the overview, the cleanup onto the system, the machine settings onto the settings, the project onboarding onto the projects; `BOTTOM_IDS` is `running`, `decisions`, `reports`; `pathOf` builds a group › title trail from static keys — read at `pm-web-app/src/lib/shell/nav.ts:120,136-205`
- OBSERVED: `RAIL_GROUPS` has no What waits entry and no `needs` route exists — read at `pm-web-app/src/lib/shell/nav.ts:42-112`, `pm-web-app/src/lib/routes.ts:20-39`
- OBSERVED: `BottomBar` holds Running, Decisions, Reports and More, and its sheet carries the language and theme controls — read at `pm-web-app/src/lib/shell/BottomBar.svelte:1-60`
- OBSERVED: `TopBar` builds its crumbs from `pathOf` and draws `Breadcrumb` with no `KindMark` and no `href` — read at `pm-web-app/src/lib/shell/TopBar.svelte:29,42-48`; `Breadcrumb.svelte` (`Crumb` has `label`, `href`, `id`)
- OBSERVED: `goBack` is called by `EpicView.svelte:171`, `PlanView.svelte:457`, `SettingsView.svelte:345`; the Back controls are at `EpicView.svelte:554`, `PlanView.svelte:712`, `OverviewView.svelte:645`, `SettingsView.svelte:351`, `MachineSettingsView.svelte:385,648`, `CompletionReportView.svelte:146`, `ProjectOnboardingView.svelte:421`, `EpicAdoptView.svelte:34`, `EpicDecompositionView.svelte:269`, `CleanupView.svelte:333` (in-place `preview.back` at :382)
- OBSERVED: `ProcessTree` carries `project` and `epic` references — read at `pm-api/src/main/java/de/planmarshall/api/process/ProcessTree.java:44-60`
- OBSERVED: `KindMark`, `Link`, `RowLink` do not exist under `pm-web-app/src/lib/components/` — read by `git ls-files pm-web-app/src/lib/components`
- OBSERVED: the language and theme controls stand in the rail foot and the More sheet by README option A — read at `doc/design/web-app/README.adoc:147,153,431`
- HYPOTHESIS: a project's plans and epics, in the order served, are readable from the data `getProjectDetail` already returns, so the work list needs no new endpoint — confirm/refute at `pm-web-app/src/lib/api/client.ts` § `getProjectDetail` (:2730) and `ProjectDetailResponse` (verify-at-outline)
- HYPOTHESIS: an epic's project is known from `getEpicResource` — confirm/refute at `pm-web-app/src/lib/api/client.ts` § `getEpicResource` (:3171) and `EpicResource` (verify-at-outline)
- HYPOTHESIS: no view other than the ten named renders an in-page Back — confirm/refute by `git grep -n "common.actions.back\|web.common.back\|web.settings.back" -- pm-web-app/src/lib/views` (verify-at-outline)
- Verify-first clause: re-read the D6 documents (Q6, the lens segment) and PLAN-142's `up` slot at the then-current HEAD. If PLAN-49 has landed, add What waits to the bar and the rail in deliverable 1; if PLAN-130 has not, stop and loop back.

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

## Dependencies and Sequencing

- Depends on: `PLAN-142` (the `up` slot, the engine), `PLAN-143` (the address scheme and the history writer); the D1 foundation part with `KindMark`, `RowLink` and `Link`; `PLAN-130` landed (`NavRail.svelte`, `chrome.spec.js`); the D6 documents (Q6).
- Overlaps with:
  - `PLAN-49` (parked): `shell/nav.ts`, `NavRail.svelte`, `BottomBar.svelte`, `routes.ts`, `+layout.svelte`, `chrome.spec.js`. This plan lands first; PLAN-49 D3 then adds only What waits, its count, the tab title and the tab icon.
  - `PLAN-130` (staged): `NavRail.svelte`, `chrome.spec.js`. It lands first.
  - `PLAN-85` (parked): `NavRail.svelte`, `chrome.spec.js`.
  - `PLAN-60` (parked): `activeNavRoute` for the machine settings.
  - `PLAN-52`, `PLAN-55`, `PLAN-58` (parked): `OverviewView`, `PlanView`, `EpicView`. They follow.
- Adjacent to: `PLAN-83` (superseded: its acceptance is deliverable 4); the views package (the cleanup's in-place hosting of objects); `PLAN-145`, `PLAN-146` (the Intent tabs are placed by PLAN-55 and PLAN-58).

## Re-Grounding at Restart

Re-read every file above at the then-current HEAD (this spec was grounded at `1cc0e10e`). Re-run `corpus surfaces` against everything that landed since (PLAN-130, PLAN-142, PLAN-143 and the D1 foundation first). Settle each HYPOTHESIS.

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-144-web-shell-up-breadcrumb-and-work-list.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger write. The session reports its outcome through its PR and its inbox message (`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary).

## Staging Note 2026-11-10

The packages this spec names by placeholder were staged on 2026-11-10: KindMark and Link are PLAN-147; RowLink is PLAN-148.
