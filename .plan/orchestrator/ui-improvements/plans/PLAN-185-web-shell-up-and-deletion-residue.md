# PLAN-185: Web Shell Up, Work List and Deletion Fall-Back: the PLAN-144 Residue

epic: ui-improvements
workstream: WS-03

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Staged on the operator's word of 2026-11-23 (the PLAN-144 fix plan, proposal 1) for the four major layer-3 findings of PLAN-144's landing (`landings/PLAN-144.md`, L3-1 to L3-4) and its dead code. This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief. **It adds no dependency.** Emit it after PLAN-176 has landed (see Dependencies and Sequencing).

## Objective

PLAN-144 (#542) shipped the web shell's Up, breadcrumb and work list, and four things it claims are wrong at the head. The store falls back from a deleted object on events that nothing emits (`plan.deleted`, `epic.removed`, …), and the one real trigger it listens for, `decision.closed`, also fires when the operator answers the decision in view, so an answer jumps to `/decisions` with "<id> no longer exists". An e2e spec still clicks a Back button the plan removed. Up is missing on a plan and an epic below 1024 px although the design says it is the same at every width. And the work list of a plan of an epic lists the whole project instead of the epic's plans. The result: a deleted object is recognised by the signal the specification actually gives, an answered decision stays where it is, Up is on every page at every width, the work list shows the siblings of the open object, the e2e suite passes, and the code PLAN-144 left unused is gone.

## Deliverables

Read first: `landings/PLAN-144.md` (Layer 3), `doc/design/shared/navigation.adoc` (`:56-65` the columns, `:111`, `:172` Up "behaves the same at every width", `:266`), `doc/design/shared/cleanup.adoc:219` and `cleanup-texts.adoc:189` (what a client shows for a deleted plan or epic), and `doc/plans/web-moving-between-plans.adoc`.

1. **The e2e spec follows the removed Back**
 - **Re-grounded 2026-11-24 at `e0e95c05`: already done by PLAN-177 (#552).** `overview.spec.js:182-184` now ends the leg on the rail's `nav-overview-btn` and asserts the `.plan-card`; no Back is clicked. What is left of this deliverable is to confirm it under `-Pe2e` together with deliverable 3's browser run; no code change is expected. The text below is the original reading at `a1e33f94`.
 - What: `pm-web-e2e/tests/overview.spec.js:183-185` clicks `getByRole('button', {name:/Back/i})` and expects it visible; #542 removed that button from `OverviewView.svelte`. The ended-work leg ends by Up, the breadcrumb or the browser's `page.goBack()` (the line `:203` already uses `goBack`) and then asserts the `.plan-card`. No other e2e test clicks a removed Back (read: `epic-view.spec.js:83` and `chrome.spec.js:90` assert `back-btn` has count 0; `start-back` is the wizard Back that stays).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:lint-config
 - Done when:
   - `overview.spec.js` passes under `-Pe2e` and clicks no control the web app no longer renders; no `waitForTimeout`. Evidence order: `none`.
2. **A deleted object is recognised by what the specification gives; an answered decision stays**
 - What:
   - Delete the four invented deletion specs of `runtime.ts:1388-1418` (`plan.deleted`/`plan.removed`, `epic.deleted`/`epic.removed`, `report.deleted`/`report.detail.deleted`, `task.deleted`/`task.removed`) and the test that fires the invented names (`runtime.test.ts:1645-1675`). The event vocabulary (`EventName.java`) and the event catalogue (`06-event-catalogue.adoc`) carry no per-subject deletion event; the only related events are `cleanup.progress` and `cleanup.done` (counts only, no ids).
   - `decision.closed` (`handleDecisionEvent`, `runtime.ts:1267-1294`): the fall-back no longer fires for a decision the operator answered, waived or declined; `DecisionView.svelte:620-648` already handles `decision.closed` and `decision.answered` itself. The store keeps a fall-back for a decision only where the closure is a withdrawal the view does not own, or drops it; which of the two is read from `DecisionView` and settled at the outline.
   - For a plan, an epic, a report and a task on screen: on `cleanup.done` (or on a not-found read of the shown subject) the store re-probes the shown object and, if it is gone, falls back to the target the specification names (the project's ended work, `cleanup.adoc:219`) with the cleanup's text (`cleanup-texts.adoc:189`: "{item} was deleted by the cleanup (by {who}, {time})"). Whether the catalogue already carries that text or a key is added to `common.{en,de}.properties` (a catalogue text, not a dependency), and whether `web.nav.gone` stays for another caller, is read at the outline.
   - NOT in this deliverable and reported only: `decision.closed` carries `closure` in the specification (`06-event-catalogue.adoc:87`, mock `running-work.json`) while `DecisionClosedEvent.java:28` and the client read `outcome`. That divergence is a finding in the pull request and the landing message, not changed here.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - `runtime.test.ts`: an event named like none of the real events (`plan.deleted`) changes nothing; an answered decision in view (`decision.answered` then `decision.closed`, the order the mock sends, `DecisionsScenarioTest.java:295-300`) leaves the address on the decision and shows no "no longer exists"; a shown plan, epic, report or task whose re-probe after `cleanup.done` is not found falls back with the cleanup text and the target the specification names; each fails with its production line reverted. Evidence order: `test-first`.
3. **Up is on a plan and an epic at every width**
 - What: Up renders only in `WorkListColumn.svelte:181-185` and `TriagePage.svelte:530-536`; `PlanView.svelte:777-780` and `EpicView.svelte:680-683` do not use `TriagePage`, and `WorkListColumn` is `display:none` below 1024 px (`:280-284`), so a plan and an epic have no Up at 1023 px and below (`navigation.adoc:172`, `:266`). The first visible column's head carries the `UpLink` at every width: below 1024 px it renders in the object column of `PlanView` and `EpicView`, and it is never shown twice at 1024 px and above. The top bar's breadcrumb stays and is not the Up control.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing, pm-dev-frontend:css
 - Done when:
   - `PlanView.test.ts` and `EpicView.test.ts`: the Up link is present with the parent's href at the narrow and at the wide layout, once in each; `work-list.spec.js:148-157` (today it only asserts the column is hidden on a phone) asserts `up-link` visible with its href at 390x844; each fails with its production line reverted. Evidence order: `test-first`.
   - Browser evidence under the port block, 1440x900 and 390x844, English and German: a plan of an epic and an epic, screenshot paths in the pull request; the layout is read from the screenshot, not the DOM.
4. **The work list shows the siblings of the open object**
 - What: `WorkListColumn.svelte:22-28,146-155` takes `projectId`, `currentPlanId` and `currentEpicId` and lists all `detail.plans` plus all `detail.epics`; the design says each column lists the children of the item selected to its left, so opening a child shows its siblings (`navigation.adoc:56-65`). One optional prop (the parent epic id; `ActivePlanSummary.epic_id` exists, `client.ts:296`; `PlanView` has `plan.epic.epic_id`, `PlanView.svelte:826`, `chain.ts:42`): for a plan of an epic the list is the epic's plans; for an epic, or a plan without an epic, it is the project's epics and epic-less plans. Whether `detail.plans` carries staged and ended plans is read first; a gap is reported, not worked around. `PlanView` and `EpicView` keep their hand-built flex row; moving them into `TriagePage` as the column engine is a design choice and is NOT made here.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - `WorkListColumn.test.ts`: a plan of an epic lists that epic's plans only; an epic and an epic-less plan list the project's epics and epic-less plans; the current row keeps `aria-current`; each case fails with its production line reverted. Evidence order: `test-first`.
5. **What PLAN-144 left unused goes**
 - What:
   - `closeOpenObject` and `replaceOpenObject` (`history.ts:304,317`; `runtime.ts:1023,1033`) have no production caller; remove them with their tests (`history.test.ts:8,14,271,275`, `runtime.test.ts:13,30,1689-1696`). `openObject` itself stays (live at `runtime.ts:641,1673,1778`).
   - `OverviewView.svelte:563` `closeEndedWork` has no caller and no test.
   - `CompletionReportView.svelte:28,31` declares an `onClose` prop that nothing reads; remove it, and `closeDetail` of `ReportsView.svelte:364` (declared and passed at `:1313`, never called) with it. The `ReportsView` tests that poke `runtimeStore.update(...)` instead of the close path (`ReportsView.test.ts:1075,1195,1275,1306,2204`) are NOT rewritten here and are reported.
   - The double blank line at `runtime.ts:1518-1519`.
   - **NOT removed:** `web.settings.back` (read at `SettingsView.svelte:1659`, the `rotate-cancel` button; `OnboardingCatalogueTest.java:79`, `machine-settings.adoc:114,344`): PLAN-144's finding L3-7 is refuted at the head.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - `git grep` names none of `closeOpenObject`, `replaceOpenObject`, `closeEndedWork` and no `onClose` prop of `CompletionReportView` outside this plan's removals; `./mvnw -pl pm-api,pm-web-app verify` passes with the test count lower by exactly the removed tests, which the pull request lists by name. Evidence order: `none`.

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`): `./mvnw -pl pm-api,pm-web-app verify`, `./mvnw -pl pm-web-e2e -am verify -Pe2e` at the head (`src/lib/stores` and the views change), then the full `./mvnw verify` and `./mvnw verify -Ppre-commit` (review every rewrite) at the head. No new dependency or plugin. Pre-1.0: no shim.
- Web standards of `CLAUDE.md`: ES modules, no `innerHTML`/`{@html}`, no `waitForTimeout` or time-passing `setTimeout`, the five npm scripts stay. Text through the catalogue (`pm-api` resources, en and de); no hard-coded text.
- A test is fed with the values the wire sends (the real event names of `EventName.java`); every new or rewritten assertion is shown to fail with the production line reverted, the failing run cited; a `test-first` clause cites a failing run at the plan's start.
- No wire name, scenario JSON or catalogue text changes beyond what a deliverable names; a fix that would change another behaviour is reported, not made. A finding that needs an engine or specification decision (a per-subject deletion event, `closure` versus `outcome`) is reported in the pull request and the landing message, never decided silently.
- The merge rule: the run merges only when every review thread has a reply and is resolved and every review body is answered, and some head has had one CodeRabbit review. The pull request body states thread count, resolved count and review-body items from `ci pr comments`, never from memory. The run merges the pull request itself once these gates hold (`CLAUDE.md`, Git Workflow). Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`; the pull request body carries no generator footer. If CodeRabbit refuses on quota, stop and ask the operator.
- ZERO Sonar findings on the new code (the operator, 2026-11-05); state `sonar_open_new_issues=N` in the landing message (`unknown` for a failed read, never `0`).
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch. Ports for the browser check: mock runtime 21850, app 21851 (this plan's block).

## Claim Labels

- OBSERVED: the four invented deletion specs and the test that fires them — read at `pm-web-app/src/lib/stores/runtime.ts:1388-1418` and `runtime.test.ts:1645-1675`
  - verdict: corroborated | checked_at: a1e33f94673381bc9543082e0cf637208472ecf1 | by: ui-improvements/analyze | rescoped: n/a | evidence: runtime.ts:1388-1418 four specs; EventName.java:55-284 has no delete/remove event for plan, epic, report or task (only source.removed:211); 06-event-catalogue.adoc lists none; the mock emits only cleanup.done (MockRuntimeEngine.java:11198) (main a1e33f94)
- OBSERVED: `handleDecisionEvent` sends everything but `decision.opened` through one branch, so `decision.closed` after an answer falls back with "no longer exists" — read at `runtime.ts:1267-1294`; the mock sends `answered` then `closed` (`DecisionsScenarioTest.java:295-300`)
  - verdict: corroborated | checked_at: a1e33f94673381bc9543082e0cf637208472ecf1 | by: ui-improvements/analyze | rescoped: n/a | evidence: runtime.ts:1283-1288 fallBackFromDeletedObject when route is decision and id matches; DecisionView.svelte:620-648 already handles closed and answered itself; a race with the view's own next-decision replace (DecisionView.svelte:1212-1237) unverified (main a1e33f94)
- OBSERVED: `overview.spec.js:183-185` clicks a Back button #542 removed — read at `pm-web-e2e/tests/overview.spec.js:183-185`
  - verdict: corroborated | checked_at: a1e33f94673381bc9543082e0cf637208472ecf1 | by: ui-improvements/analyze | rescoped: n/a | evidence: overview.spec.js:183-185 getByRole button Back expected visible; no other e2e test clicks a removed Back (epic-view.spec.js:83, chrome.spec.js:90 assert count 0) (main a1e33f94)
- OBSERVED: Up renders only in `WorkListColumn` and `TriagePage`, and the column is `display:none` below 1024 px — read at `WorkListColumn.svelte:181-185,280-284`, `TriagePage.svelte:530-536`; `PlanView.svelte:777-780` and `EpicView.svelte:680-683` do not use `TriagePage`
  - verdict: corroborated | checked_at: a1e33f94673381bc9543082e0cf637208472ecf1 | by: ui-improvements/analyze | rescoped: n/a | evidence: the four ranges read; navigation.adoc:172 and :266 say Up is the same at every width; work-list.spec.js:148-157 asserts only that the column is hidden (main a1e33f94)
- OBSERVED: `WorkListColumn` lists all `detail.plans` plus all `detail.epics` — read at `WorkListColumn.svelte:22-28,146-155`
  - verdict: corroborated | checked_at: a1e33f94673381bc9543082e0cf637208472ecf1 | by: ui-improvements/analyze | rescoped: n/a | evidence: props projectId, currentPlanId, currentEpicId; navigation.adoc:56-65 says each column lists the children of the item to its left; ActivePlanSummary.epic_id at client.ts:296 and plan.epic.epic_id at PlanView.svelte:826 supply the parent (main a1e33f94)
- OBSERVED: `closeOpenObject`/`replaceOpenObject`, `closeEndedWork`, the `onClose` prop of `CompletionReportView` and `closeDetail` have no production caller — read by `git grep` over `pm-web-app/src`
  - verdict: corroborated | checked_at: a1e33f94673381bc9543082e0cf637208472ecf1 | by: ui-improvements/analyze | rescoped: n/a | evidence: history.ts:304,317 and runtime.ts:1023,1033 have only test callers; OverviewView.svelte:563 none; CompletionReportView.svelte:28,31 declared never read; ReportsView.svelte:364,1313 declared and passed only (main a1e33f94)
- OBSERVED: `web.settings.back` is used — read at `SettingsView.svelte:1659`, `OnboardingCatalogueTest.java:79`
  - verdict: corroborated | checked_at: a1e33f94673381bc9543082e0cf637208472ecf1 | by: ui-improvements/analyze | rescoped: n/a | evidence: PLAN-144 L3-7 refuted: the key is the rotate-cancel button's label and is named in machine-settings.adoc:114,344 (main a1e33f94)
- HYPOTHESIS: `cleanup.done` carries no ids, so the store re-probes the shown subject instead of reading the id from the event — confirm/refute in `06-event-catalogue.adoc:44-45` and the store's `cleanup.*` handling (verify-at-outline; the web app has no `cleanup.*` handler in non-test code today)
  - verdict: unverifiable | checked_at: a1e33f94673381bc9543082e0cf637208472ecf1 | by: ui-improvements/analyze | rescoped: n/a | evidence: the payload is counts only per the catalogue read; what a not-found read of a plan, epic, report and task returns is not read here
- HYPOTHESIS: the cleanup text of `cleanup-texts.adoc:189` already has a catalogue key, and `web.nav.gone` has no other caller — confirm/refute by `git grep` over `pm-api/src/main/resources/messages` and `pm-web-app/src` (verify-at-outline)
  - verdict: unverifiable | checked_at: a1e33f94673381bc9543082e0cf637208472ecf1 | by: ui-improvements/analyze | rescoped: n/a | evidence: not grepped here
- HYPOTHESIS: `detail.plans` carries staged and ended plans, so the epic-scoped list is complete — confirm/refute at `ProjectExplorer` detail read (verify-at-outline)
  - verdict: unverifiable | checked_at: a1e33f94673381bc9543082e0cf637208472ecf1 | by: ui-improvements/analyze | rescoped: n/a | evidence: reported unverified by the grounding read
- Verify-first clause: before each deliverable re-read the named files at the then-current HEAD; PLAN-176, PLAN-177 and PLAN-181 touch `pm-web-app` and may have moved `runtime.ts` or the views. A refuted claim re-scopes the deliverable and is reported, never silently worked around.
  - verdict: unverifiable | checked_at: a1e33f94673381bc9543082e0cf637208472ecf1 | by: ui-improvements/analyze | rescoped: n/a | evidence: procedural verify-first clause; PLAN-176 and PLAN-177 running

## Expected Surface

- OBSERVED: `pm-web-e2e/tests/overview.spec.js` — present at HEAD a1e33f94
- OBSERVED: `pm-web-e2e/tests/work-list.spec.js` — present at HEAD a1e33f94
- OBSERVED: `pm-web-app/src/lib/stores/runtime.ts` — present at HEAD a1e33f94
- OBSERVED: `pm-web-app/src/lib/stores/runtime.test.ts` — present at HEAD a1e33f94
- OBSERVED: `pm-web-app/src/lib/navigation/history.ts` — present at HEAD a1e33f94
- OBSERVED: `pm-web-app/src/lib/navigation/history.test.ts` — present at HEAD a1e33f94
- OBSERVED: `pm-web-app/src/lib/components/WorkListColumn.svelte` — present at HEAD a1e33f94
- OBSERVED: `pm-web-app/src/lib/components/WorkListColumn.test.ts` — present at HEAD a1e33f94
- OBSERVED: `pm-web-app/src/lib/views/PlanView.svelte` — present at HEAD a1e33f94
- OBSERVED: `pm-web-app/src/lib/views/PlanView.test.ts` — present at HEAD a1e33f94
- OBSERVED: `pm-web-app/src/lib/views/EpicView.svelte` — present at HEAD a1e33f94
- OBSERVED: `pm-web-app/src/lib/views/EpicView.test.ts` — present at HEAD a1e33f94
- OBSERVED: `pm-web-app/src/lib/views/OverviewView.svelte` — present at HEAD a1e33f94
- OBSERVED: `pm-web-app/src/lib/views/CompletionReportView.svelte` — present at HEAD a1e33f94
- OBSERVED: `pm-web-app/src/lib/views/ReportsView.svelte` — present at HEAD a1e33f94
- OBSERVED: `pm-web-app/src/lib/views/DecisionView.svelte` — present at HEAD a1e33f94

## Dependencies and Sequencing

- Depends on: nothing shipped is missing. Sequenced AFTER `PLAN-176` (running): the declared surfaces share four files (`history.ts`, `runtime.ts`, `runtime.test.ts`, `CompletionReportView.svelte`, read 2026-11-23 at `a1e33f94`), a multi-file overlap, so it is emitted when PLAN-176 has landed. `PLAN-177` (running) shares no file with it. Emit it when the surface read at emit shows no multi-file overlap with a running plan.
- Overlaps with: `PLAN-85` (staged: two files, `runtime.ts` and `runtime.test.ts`; it edits `NavRail`, `BottomBar`, `nav.ts`, `routes.ts`, which this plan does not), `PLAN-179` (staged: two files, `EpicView.svelte` and `EpicView.test.ts`; sequenced after this plan or the operator decides), `PLAN-182` (staged: one file, `runtime.ts`, which does not block).
- Adjacent to: PLAN-144's D5 browser evidence (not recorded by that plan; the browser clause of deliverable 3 here covers the Up and the work list only).

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-185-web-shell-up-and-deletion-residue.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger write. The session reports its outcome through its PR and its inbox message (`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary).
