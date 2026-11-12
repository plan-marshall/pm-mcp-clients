# PLAN-143: Web: Addresses, History and the Remembered Place (D2 Navigation Engine, Part 2)

epic: ui-improvements
workstream: WS-02

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Staged on the operator's decision of
> 2026-11-10: the D2 navigation engine is split into three parts (this is the second). With PLAN-144 it SUPERSEDES PLAN-83. This spec
> is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Today the store is the route and the address bar follows it: queries such as `?plan=`, `?decision=` and `?project=` carry
selections, a caller stack (`navigationStack`, `goBack`) records where the operator came from, the start rule is computed
from whether work runs (`landingRoute`), and no address carries a mode, a tab, an object opened in place, a node or a side
column. D2 designed one scheme (`navigation.adoc#url`). This plan builds it: the address model, the push and replace rule in
one writer, the remembered place with its entry points, and the move of the existing views' selections into the scheme. The
caller stack and the in-page Backs go with Up in PLAN-144.

## Deliverables

The documents are the specification; read them first: `doc/design/web-app/navigation.adoc` (#url, #missing, #lenses),
`doc/design/web-app/README.adoc` (#routes, the `/` and `/overview` rows :445-449), `doc/design/shared/plan-overview.adoc`
(#persistence :161-171), `doc/design/web-app/plan-view-interaction.adoc#addresses` (:122-129). D6 merged (#403, `8568b56e`) and settled
Q6 (what the remembered place holds, the entry points, the lens segment) and Q3 (the plan's `tab=`, `node=` and `file=`); the
deliverables below state its rulings.

1. **The address model**
 - What:
   - `routes.ts` gains the objects of the scheme whose view exists: the project page `/projects/{id}`, a task
     `/plans/{id}/tasks/{task}`, one decision `/decisions/{id}`, one report `/reports/{id}`, and `/decomposition`. The query
     vocabulary is typed: `project`, `mode`, `group`, `follow`, `tab`, `open` (`{kind}:{id}`), `node` (ids joined by `/`),
     `file` (a path of the plan's changes), `chat`, `turn`, and `lens` (`/?lens=waits`, the address the app's links to What waits
     write, which names the lens and is never filled). It parses and serialises canonically, drops unknown keys, and carries the
     lens's modes on every address of the lens.
   - The tab keys (D6): a plan's `tab` is `process` (the default), `intent` or `changes`; an epic's `plans` (the default), `intent`,
     `landings`, `reviews` or `events`. `node=` is serialised only with the plan's Process tab and `file=` only with `tab=changes`
     (`/plans/fix-relay?tab=changes&file=pm-relay/pom.xml`); the serialiser drops a key whose tab is not shown. `changes=open` is
     not a key of the scheme (#400 designed it, D6 replaced it with `tab=changes` before any code wrote it) and is dropped as unknown. A task's address
     (`/plans/{id}/tasks/{task}`) carries no tab of the plan; `chat=open` on it names the plan's chat.
   - An object is `/{kind}s/{id}` and the segment after it is always an id; a workflow has a path of its own or a fixed
     segment below its object's id, so a project called `onboard` or an epic called `decompose` is an object.
   - These old addresses are replaced when opened: `/decisions?decision=`, `/reports?plan=`, `/projects?project=`,
     `/epics/decompose`. `/settings/machine`, `/settings/browser` and `/projects/onboard` stay for PLAN-60, PLAN-85 and PLAN-59,
     which own their targets. `/` is registered by PLAN-49 (one line in a route table this plan makes extensible).
   - Titles for the new routes in `ROUTE_TITLES`.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - `routes.test.ts` round-trips every address of `navigation.adoc#url` and `README.adoc#routes` that this plan owns, a project `onboard` and an epic
     `decompose`, every old address, and a malformed escape; `routeTitles.test.ts` covers the new routes. It fails on a revert.
     Evidence order: `test-first`.
   - `routes.test.ts`: each tab value of a plan and of an epic; `node=` serialised with Process and dropped with Intent and Changes;
     `file=` serialised with `tab=changes` only; `changes=open` dropped; `/?lens=waits` round-trips. Evidence order: `test-first`.
2. **One writer of the address, and the push and replace rule**
 - What:
   - A navigation module (`pm-web-app/src/lib/navigation/history.ts`, new) is the only writer: a change of depth, of lens or the
     opening of an object in place is a push; a sibling in the same column, a `j`/`k` move, a node, a tab, a mode or a scope, the
     opening or hiding of a side column and every selection the app makes on its own (a fallback to the parent, filling a bare
     lens, the next decision) is a replace. `syncUrl` becomes its internal; `+layout.svelte` writes through it.
   - **The app's mark** (D6 Q6): every entry the writer pushes or replaces carries the app's mark in `history.state`. `syncUrl` already
     writes `{ path }` as the state of every push and replace (`runtime.ts:920-931` at `4a84f727`); the writer keeps such a mark and
     exposes one predicate, "this load carries the app's mark", read once at start. A load whose state has none is the app opened from
     outside it (a typed address, a bookmark, a new tab). The removal of the pairing fragment (`dropCodeFragment`, `runtime.ts:226`,
     today `replaceState(null, …)`) must not count as the app's mark for the fill decision of that load, and the entry the app leaves
     afterwards carries the mark.
   - **A tab switch** is a replace that drops the other tab's key (`node=` leaving Process, `file=` leaving Changes); the view keeps
     the other tab's selection in memory while the object stays open (`plan-view-interaction.adoc#addresses`).
   - `popstate` reads path and query back into the store (selection, mode, tab, `open`, `node`, `file`, `chat`).
   - An object deleted while shown falls back to its parent, replacing the address, with the notice `web.nav.gone`
     (`{name} no longer exists.`) in the store and the frame's live region.
   - An object opened in place is opened by a push and closed by the history's back; an address loaded with `open=` closes by
     replacing it with the caller's. A pairing code in the fragment is redeemed first and never written to the history
     (kept as today).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - `history.test.ts` has one case per change kind in `navigation.adoc#url` asserting push or replace, back and forward
     restoring the selection, the deleted-object fallback, and the pairing fragment never written; every written entry carries
     the app's mark; a tab switch replaces and drops the other tab's key; `layout.test.ts` follows.
     Evidence order: `test-first`.
3. **The remembered place and its entry points** (D6 Q6, `plan-overview.adoc#persistence`, `navigation.adoc#url` :216-219)
 - What:
   - A store `pm-web-app/src/lib/navigation/rememberedPlace.ts` (new) holds the place: the project, the grouping (by plan, by
     epic), and the selected plan or epic — **never the mode** (running or ended) and never the follow mode, so the rail opens
     running work. It keeps the existing storage keys `pm_overview_selected_project` and `pm_overview_view_mode` and adds the
     selected object; `OverviewView` reads and writes through its API (the smallest edit; PLAN-52 rewrites the view later and
     drops `pm_overview_show_ended` and `pm_overview_ended_page`).
   - The fill applies at **two entry points only**: (a) a load **without the app's mark** in `history.state` at `/` or at the
     bare `/overview` opens where the operator left it, **by a replace**; without a remembered place `/` stays on What waits;
     (b) a **click on Running work** (the rail's, or the bottom bar's on a phone) opens `/overview` filled from the place, **by a
     push** of the filled address. Never filled: a full address (it wins), `/?lens=waits` (also on a reload), and the lens's own
     list pushed by Up or the breadcrumb's lens segment (`PLAN-144`), which carries the mark, so a reload keeps it unfilled.
   - Fall-backs: a remembered plan that has ended falls back to its project; a project that no longer has running work falls back
     to the list of projects.
   - `landingRoute` and `openStartPage` (the rail's brand click) are replaced by one exported start rule: the remembered place
     (at a mark-less load only), else What waits at `/` once PLAN-49 registers it, and until then the present rule (the overview
     while work runs or waits, the projects otherwise). PLAN-49 replaces that fallback in one place.
   - One exported function for the lens's unfilled list address (the address Up and the lens segment push), which `PLAN-144`
     calls, and one for the Running work click (the filled push), which the rail's entry calls here (`NavRail.svelte`) and the
     bottom bar's entry calls once `PLAN-144` rebuilds the bar.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - `rememberedPlace.test.ts` and `runtime.test.ts`: each fall-back, the mode not remembered (ended work open, then the rail opens
     running work), a mark-less load at `/` and at `/overview` filled by a replace and a marked reload not, a rail click filled by a
     push, `/?lens=waits` never filled, and the lens's unfilled list (Up, lens segment) not filled on a reload. Evidence order:
     `test-first`.
4. **The existing views read and write the scheme**
 - What:
   - `DecisionView` focuses the decision of `/decisions/{id}`, `ReportsView` the report of `/reports/{id}` (its detail is today
     keyed by plan id, `selectedReportDetailPlanId`), `ProjectExplorerView` the project of `/projects/{id}`, `OverviewView` the
     scope, group and mode from `?project=`, `group=epic`, `mode=ended`, `PlanView` the task of `/plans/{id}/tasks/{task}`.
     Their own content and layout do not change; each keeps reading the store field it reads today, fed from the address.
   - New route pages for the new paths (each mounts `AppRouter` and adopts the address); `routes/epics/decompose/+page.svelte`
     goes, `/decomposition` replaces it.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - The views' tests assert the selection from the address and the old address replaced. Evidence order: `test-first`.
   - `decisions.spec.js`, `reports.spec.js`, `projects.spec.js`, `overview.spec.js` and `plan-view.spec.js` follow. Evidence order: `test-first`.
5. **Browser evidence**
 - What: a new `pm-web-e2e/tests/navigation.spec.js` on the mock: every owned address loads its route, a deep link opens, back
   and forward move through the selections, an old address is replaced, a fresh context at `/overview` is filled from the
   remembered place by a replace, a click on Running work is filled by a push and opens running work after ended work was open, a
   reload of a marked `/overview` stays as it is, `/?lens=waits` is never filled, and `/plans/fix-relay?tab=changes&file=…` drops
   `file=` on a switch to Process.
 - Persona: plan-marshall:persona-integration-tester
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - `navigation.spec.js` passes with `PM_MOCK_WEB_PORT` and `PM_WEB_PORT` set; it waits on the address and the DOM, never on time.
     Evidence order: `test-first`.
   - Browser check (Playwright MCP) on `running-work` of a deep link, the history and the fill, in German and English. Evidence
     order: `none`.
   - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for
     a failed read, never `0`); a confirmed `0` or every open issue dismissed. Evidence order: `none`.

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`): `./mvnw -pl pm-web-app verify`, `./mvnw -pl pm-web-app verify -Ppre-commit` (review every rewrite),
  `PM_MOCK_WEB_PORT=<mock port> PM_WEB_PORT=<app port> ./mvnw -pl pm-web-e2e verify -Pe2e` at the head.
- No new dependency or plugin. Web standards of `CLAUDE.md`; no static `style` attribute; every new text from the catalogue in
  English and German; the text guard baseline does not grow. Pre-1.0: no compatibility shim for an old address beyond the replacement the design states.
- Frontend tests on Vitest with Testing Library (`pm-dev-frontend:jest-testing`).
- Ports of this plan: mock web port 21430, app port 21431 (`20000 + 10 × 143`) (`AGENTS.md` § Ports). Other plans run at the same
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

- OBSERVED: `AppRoute` has no project-page, task, decisions-detail or reports-detail route and no `/`; `FIXED_ROUTE_PATHS` and `PARAMETERIZED_PATHS` do not match `/plans/{id}/tasks/{task}`, `/decisions/{id}`, `/reports/{id}` or `/projects/{id}` — read at `pm-web-app/src/lib/routes.ts:20-39,167-211`
- OBSERVED: `pathForRoute` writes `/reports?plan=`, `/projects?project=`, `/decisions?decision=` and `/epics/decompose` — read at `pm-web-app/src/lib/routes.ts:307-335`
- OBSERVED: the caller stack and the writers are `navigationStack` (:777), `pushRoute` (:849), `goBack` (:869), `syncUrl` (:906), `adoptRouteFromLocation` (:941) — read at `pm-web-app/src/lib/stores/runtime.ts`
- OBSERVED: `landingRoute` (:294) and `openStartPage` (:335) exist at HEAD and the rail's brand click calls `openStartPage` — read at `pm-web-app/src/lib/stores/runtime.ts:294-345`, `pm-web-app/src/lib/shell/NavRail.svelte:89`. (The re-grounding report of PLAN-49 states they no longer exist: it is wrong at this HEAD.)
- OBSERVED: `syncUrl` writes `{ path }` as the history state and the layout's effect sets the address from the store on every route change — read at `pm-web-app/src/lib/stores/runtime.ts:906-917`, `pm-web-app/src/routes/+layout.svelte:234-250`
- OBSERVED: `OverviewView` stores `pm_overview_view_mode`, `pm_overview_selected_project`, `pm_overview_show_ended`, `pm_overview_ended_page` and restores the ended section — read at `pm-web-app/src/lib/views/OverviewView.svelte:191-215,341-354,394`; no key holds the selected plan or epic
- OBSERVED: the reports detail is keyed by plan id (`selectedReportDetailPlanId`) — read at `pm-web-app/src/lib/stores/runtime.ts:79,794`, `pm-web-app/src/lib/views/ReportsView.svelte:270-298`
- OBSERVED: `follow` is not implemented anywhere in the views — read by `git grep -n -i follow -- pm-web-app/src/lib/views/OverviewView.svelte pm-web-app/src/lib/views/PlanView.svelte` (no mode); this plan only parses and serialises it
- OBSERVED: each route has its own `+page.svelte` mounting `AppRouter`; `projects/[id]/start`, `projects/onboard`, `epics/decompose`, `plans/[id]` exist — read at `pm-web-app/src/routes/**`
- HYPOTHESIS: the report's detail address `/reports/{id}` takes the report id while the completion report is keyed by plan id, so the view must look the plan up in the report record (`from.plan`) — confirm/refute at `pm-web-app/src/lib/views/ReportsView.svelte` § `selectedReportDetailPlanId` and `doc/design/shared/completion-report.adoc:103` (verify-at-outline)
- HYPOTHESIS: every new path needs its own `+page.svelte` (no catch-all or fallback page) — confirm/refute at `pm-web-app/vite.config.ts` § adapter and `src/routes/+layout.ts` (verify-at-outline)
- HYPOTHESIS: `CommandPalette.svelte` navigates through `navigateTo` with the same params and needs no change beyond the new routes — confirm/refute at `pm-web-app/src/lib/shell/CommandPalette.svelte` § `navigateTo` (verify-at-outline)
- OBSERVED: `syncUrl` writes `{ path }` as the history state of every push and replace and returns early for an unchanged address; `dropCodeFragment` replaces the state with `null` — read at `pm-web-app/src/lib/stores/runtime.ts:920-931,224-227` (`4a84f727`)
- OBSERVED: D6 settled the place (project, grouping, selected plan or epic; never the mode), the two fill entry points (a mark-less load at `/` or bare `/overview` by a replace; a click on Running work by a push), the unfilled lens list of Up and the lens segment, `/?lens=waits`, and the plan's `tab=`/`node=`/`file=` rules — read at `doc/design/shared/plan-overview.adoc:164-171`, `doc/design/web-app/navigation.adoc:178-222`, `doc/design/web-app/README.adoc:445-449`, `doc/design/web-app/plan-view-interaction.adoc:122-129` (`4a84f727`, D6 merged `8568b56e`)
- Verify-first clause: re-read `navigation.adoc#url`, `plan-overview.adoc#persistence` and `plan-view-interaction.adoc#addresses` at the then-current HEAD. If the entry points or the remembered place's content differ from this spec, the documents win and the spec is re-scoped.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/routes.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/routes.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/routeTitles.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/stores/runtime.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/stores/runtime.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/navigation/history.ts` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/navigation/history.test.ts` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/navigation/rememberedPlace.ts` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/navigation/rememberedPlace.test.ts` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/routes/+layout.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/routes/layout.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/AppRouter.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/DecisionView.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/DecisionView.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/ReportsView.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/ReportsView.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/ProjectExplorerView.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/ProjectExplorerView.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/OverviewView.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/OverviewView.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/PlanView.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/PlanView.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/shell/NavRail.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/shell/CommandPalette.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/routes/decisions/+page.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/routes/decisions/[id]/+page.svelte` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/routes/reports/[id]/+page.svelte` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/routes/projects/[id]/+page.svelte` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/routes/plans/[id]/tasks/[task]/+page.svelte` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/routes/decomposition/+page.svelte` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/routes/epics/decompose/+page.svelte` — present at HEAD 1cc0e10e; removed by this plan
- OBSERVED: `pm-api/src/main/resources/messages/common.en.properties` — present at HEAD 1cc0e10e (`web.nav.gone`)
- OBSERVED: `pm-api/src/main/resources/messages/common.de.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/navigation.spec.js` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-e2e/tests/decisions.spec.js` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/reports.spec.js` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/projects.spec.js` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/overview.spec.js` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/plan-view.spec.js` — present at HEAD 1cc0e10e

## Dependencies and Sequencing

- Depends on: `PLAN-45` landed (it holds `routes/+layout.svelte` and `views/`); `PLAN-129` landed (`ReportsView`); the D6 documents (Q6) merged — satisfied (#403, `8568b56e`). No D1 foundation dependency.
- Overlaps with:
  - `PLAN-45` (running): `routes/+layout.svelte`, the `views/` directory.
  - `PLAN-129` (staged): `ReportsView.svelte`, `ReportsView.test.ts`.
  - `PLAN-142`: `ReportsView.svelte`. Series: never at the same time.
  - `PLAN-49`, `51`, `52`, `55`, `58`, `59` (parked): they rewrite the views whose address reads this plan touches; they follow. `PLAN-49` registers `/` in the route table this plan makes extensible and replaces the start rule's fallback.
- Adjacent to: `PLAN-144` (removes `navigationStack`, `goBack` and the in-page Backs; left alone here so the app keeps a Back until Up arrives; it pushes the lens's unfilled list through this plan's function); `PLAN-135..137` (the chat's `open=` forms of status cards and reference chips wait for this plan; until it lands the pushed address is their only form); `PLAN-55`, `PLAN-58`, `PLAN-62` (they write `tab=`, `node=` and `file=` through this plan's writer); `PLAN-60` (`/settings/machine` to `?tab=setup`) and `PLAN-85` (`/settings/browser`) own their old-address replacements; `PLAN-59` owns `/projects/onboard`.

## Re-Grounding at Restart

Re-read every file above at the then-current HEAD (this spec was grounded at `1cc0e10e`). Re-run `corpus surfaces` against everything that landed since (PLAN-45 and PLAN-129 first). Settle each HYPOTHESIS. Confirm `landingRoute` and `openStartPage` still exist (PLAN-110's claimed removal is not at this HEAD).

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-143-web-addresses-and-remembered-place.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger write. The session reports its outcome through its PR and its inbox message (`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary).
