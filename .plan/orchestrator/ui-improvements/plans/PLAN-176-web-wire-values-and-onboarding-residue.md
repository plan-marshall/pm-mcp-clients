# PLAN-176: Web: Wire Values and the Onboarding and Dialog Residue of PLAN-154, 156 and 59

epic: ui-improvements
workstream: WS-03

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Staged on the operator's word of
> 2026-11-23 (decision 2, proposal 1): the layer-3 majors of the landings of PLAN-154, PLAN-59 and PLAN-156 and the residue those
> landings named. This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief. **It starts after
> PLAN-144 has landed** (PLAN-144 edits `CompletionReportView`, `ProjectOnboardingView`, `ProjectExplorerView`, `ReportsView`,
> `routes.ts` and `runtime.ts`, the same files).

## Objective

Three landings left the web app showing values the wire never sends and doing less than its design says. The completion report names deliverable states that do not exist on the wire, so a real not-done deliverable reads the unknown word; the project explorer labels operator items as a setup need, also on ready projects; the project onboarding cannot record the identity of a project being added and has no address of its own; and the modal layer carries the unmet browser evidence of PLAN-156, an IME hole in `Dialog`, two orphaned components and a lost confirmation. This plan fixes these against the wire and the design, with tests fed with the values the wire sends.

## Deliverables

The documents are the specification; read them first: `doc/design/web-app/reports.adoc`, `completion-report.adoc` (the deliverable states), `project-explorer.adoc` (#project-page, #actions), `project-onboarding.adoc` (the identity step, #build-environment), `components.adoc` (the dialog), and the engine's `phase-workflows/08-dynamic-re-planning.adoc` and `state-catalogue/15-project-onboarding.adoc` in `plan-marshall/pm-mcp-core`. The landing records `landings/PLAN-154.md`, `PLAN-156.md` and `PLAN-59.md` hold the findings with their file and line.

1. **The completion report shows the states the wire sends**
 - What:
   - `CompletionReportView` maps deliverable states through `displayState('task', d.state)` and an icon function that knows `done` and `dropped` (`CompletionReportView.svelte:111-116,423`); the catalogue has `state.task.not_done` and `state.task.dropped` (`common.en.properties:328-329`), values no wire sends: the mock sends `not_started` (`reports-and-system.json:388,1169,1777`) and the engine's outline task and deliverable states are read at outline (`08-dynamic-re-planning.adoc`; `infeasible` is one). Map every state the wire sends to a catalogue key in English and German, and an unknown state to the catalogue's neutral word, never the raw id; drop `not_done` and `dropped` unless the wire sends them.
   - The tokens section prints raw wire codes (`CompletionReportView.svelte:252,656`: the coverage reason `terminated`, the waste class except `no_product`): use the existing `report.tokens.unmeasured_reason.*` keys and add the waste class keys; `report.completion.waste_share` says "of measured" only when coverage is partial.
   - The head heading shows `data.title` where the report has one (`client.ts:1082`), not the plan id three times (`:354,362,378`); the deliverable-state column is wide enough for the German words; the three dead `report.from.open_project`, `open_epic`, `open_plan` keys leave (`reports.*.properties:159-161`); `phase: 'refine'` of the empty-deliverables text is the phase the report names.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `CompletionReportView.test.ts` is fed every state the mock scenarios send and the states settled from the engine, and shows each one's catalogue word in English and German and the neutral word for an unknown state; a test fed with `not_started` fails at the plan's start HEAD. Evidence order: `test-first`.
   - The text-guard and the catalogue tests show no raw wire code in the tokens section. Evidence order: `none`.
2. **The project explorer's needs marker says what it counts**
 - What:
   - `ReasonBadge reason="setup"` renders for any `needs_operator_count > 0`, ready projects included (`ProjectExplorerView.svelte:960-961,1277-1278`); `EnrolledProject.needs_operator_count` counts operator items, not setup needs (`EnrolledProject.java:31`). Show the marker with the reason the count has (the operator items, as the epic and plan rows do), and `setup` only for a project that is not ready.
   - The "(most used)" heading prints an order no field of `pm-api` or the mock carries; name the order the runtime serves, or leave the claim out. An epic row reads "N plans · 0 epics" (`:1338-1343`): fix its count; plan rows carry the needs marker, ended rows their outcome.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `ProjectExplorerView.test.ts`: a ready project with two operator items shows the operator-item marker and no `setup` word; a not-ready project shows `setup`; the epic row and the plan rows as stated. Evidence order: `test-first`.
3. **The project onboarding records its identity and has an address**
 - What:
   - A run of a project being added cannot record its identity: `saveIdentity` returns without a `projectId` (`ProjectOnboardingView.svelte:201`) and Save is disabled with "records its identity in the terminal client until the project is enrolled" (`:680-681`); `runOfBeingAdded` drops every link (`:372-381`). The engine says the choices of the onboarding are operator waits served to both clients (`15-project-onboarding.adoc`), the mock serves `links.set` on the entry (`MockRuntimeEngine.java:4872-4884`) and the client has the entry-link call (`client.ts:3281`, `/api/v1/entries/{id}/links/{act}`). Carry the entry's links on the being-added run and record the identity through them; if the real engine refuses a `set` before enrolment, stop and report it to the operator (the sentence on the page is then true and stays, with its reason).
   - The needs-terminal card shows at steps 1 to 3 of every being-added run and never clears (`:112-118`: `needsTerminal` is true whenever `build_environment.captured_at` is absent, and `runOfBeingAdded` never carries one); show it at the step the design names (③), after a read that can carry the environment; the pilot checkbox shows only while the lifecycle gate holds (`:669-675`).
   - The run has an address of its own: `openProjectOnboarding` pushes `project-onboarding`, `/projects/onboard` (`runtime.ts:2033-2043`, `routes.ts:297`), so a reload or a second tab shows the "which checkout" form, not the run, and the path collides with a project whose id is `onboard`. Give each run a route of its own (the design says so) written by the one history writer, restore the run from it on a reload, and keep `/projects/{id}` for the project.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - `ProjectOnboardingView.test.ts`, `routes.test.ts` and `runtime.test.ts`: the identity of a being-added run is recorded through the entry link with the exact body; a reload at the run's address shows the run; the address of a project whose id is `onboard` still opens the project; the card shows at ③ and clears after a read that carries the environment. Evidence order: `test-first`.
   - `projects.spec.js` shows the run's address surviving a reload. Evidence order: `test-first`.
4. **The modal layer: what PLAN-156 left**
 - What:
   - `Dialog.svelte` closes on an Escape that cancels an IME candidate (`onKeydown`, `event.isComposing` is read in `Compose.svelte:161` only): ignore the key while composing, the same way.
   - `web.confirm.destructive.title` and `.consequence` (en and de) are asserted by no test (PLAN-156 D6(c)); the Escape yield is pinned only against removing it, not against its focus-keying (a menu open in the dialog with focus elsewhere must not swallow the Escape); the review modal's "walks its acts with Tab" test passes with no trap (`FindingsReviewModal.test.ts`): make it fail without one; `utils/loopback.ts` has a test.
   - `SourceIntakePanel.svelte` and `RecipeSelector.svelte` have no production importer since `StartWorkModal` went (`git grep` finds their own tests only); remove both with their tests and the orphaned keys `start.modal.title`, `start.action.start`, `start.action.cancel`, `start.field.title`, `lesson_source.start_from_lesson` (confirm at outline that no other plan's surface needs them: PLAN-53 shipped).
   - `LanListenerModal`'s turn-off is `secondary` and a LAN browser can turn its own listener off without a confirmation (`LanListenerModal.svelte:80`): restore the danger variant and the confirmation for a session that is not loopback (`machine-settings.adoc:247`); the picker's selected root has a visible state (`ConfinedFilePicker.svelte`); an owner that closes while the hold confirmation is open does not leave the page at `overflow: hidden` (`Dialog.svelte` `takeFocus`, `HoldResolutionModal.svelte`).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `Dialog.test.ts`, `FindingsReviewModal.test.ts`, `HoldResolutionModal.test.ts`, `LanListenerModal.test.ts`, `ConfinedFilePicker.test.ts` and `loopback.test.ts`: each item above has a test that fails at the plan's start HEAD for the new behaviour. Evidence order: `test-first`.
   - The removed components and keys are absent (`git grep` empty) and the catalogue tests pass. Evidence order: `none`.
5. **Browser evidence**
 - What: the evidence PLAN-156 and PLAN-59 owed, taken after the last visual fix: the grant question opened in a browser, the picker from `projects-and-start`, a phone baseline and phone after-shots of the four modals, and the project onboarding at steps ①–③ and the completion report with a not-done deliverable, in `en` and `de`, light and dark, at `1440 × 900` and `390 × 844`.
 - Persona: plan-marshall:persona-integration-tester
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - A `GrantModal` e2e case exists and passes with `PM_MOCK_WEB_PORT` and `PM_WEB_PORT` set; it waits on the address and the DOM. Evidence order: `test-first`.
   - Browser check (Playwright MCP) on the targets above; the screenshots are taken after the last visual commit and named `{plan}-{target}-{before|after}`. Evidence order: `none`.

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`): `./mvnw -pl pm-api,pm-web-app verify`, `./mvnw -pl pm-api,pm-web-app verify -Ppre-commit` (review every rewrite), `PM_MOCK_WEB_PORT=<mock port> PM_WEB_PORT=<app port> ./mvnw -pl pm-web-e2e verify -Pe2e` at the head, and `-Pcoverage` run at the head after the last code commit (minimum 80 % branch for `pm-web-app`; PLAN-156 ended at 80.86 %).
- No new dependency or plugin. Web standards of `CLAUDE.md`; no static `style` attribute; every new text from the catalogue in English and German; the text guard baseline does not grow. Pre-1.0: no shim for a removed key or component.
- Frontend tests on Vitest with Testing Library (`pm-dev-frontend:jest-testing`); a test is fed with the values the wire sends, never with a value the code invented.
- Ports of this plan: mock web port 21760, app port 21761 (`20000 + 10 × 176`) (`AGENTS.md` § Ports). Other plans run at the same time: never stop or reuse a process this worktree did not start.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.
- The merge rule: the run merges only when every review thread has a reply and is resolved and every review body is answered, and some head has had one CodeRabbit review. The pull request body states thread count, resolved count and review-body items from `ci pr comments`, never from memory. The run merges the pull request itself once these gates hold (`CLAUDE.md`, Git Workflow). Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`; the pull request body carries no generator footer. If CodeRabbit refuses on quota, stop and ask the operator.
- ZERO Sonar findings (the operator, 2026-11-05): the pull request merges with 0 open new-code issues, every open issue fixed in a commit of THIS pull request; state `sonar_open_new_issues=N` in the landing message (`unknown` for a failed read, never `0`). A dismissal needs the operator's word.
- Evidence of a clause says what its order is: a `test-first` clause cites a failing run at the plan's start and a passing run; where the base-sha instrument is unavailable, say so and do not relabel the clause.

## Claim Labels

- OBSERVED: the completion report maps deliverable states through `displayState('task', …)` with the keys `state.task.not_done` and `state.task.dropped`, and the mock sends `not_started` — read at `pm-web-app/src/lib/views/CompletionReportView.svelte:111-116,423`, `pm-api/src/main/resources/messages/common.en.properties:326-332`, `pm-mock-runtime/src/main/resources/scenarios/reports-and-system.json:388,1169,1777`
  - verdict: corroborated | checked_at: 3db842df57c89ed737c5503ef40a2da00b9c7bc9 | by: ui-improvements/analyze | rescoped: n/a | evidence: CompletionReportView.svelte:103-106,407 icon knows only done/dropped, displayState('task'); common.en.properties:328-329 not_done/dropped; mock sends not_started at reports-and-system.json:388,1169,1777,2311 (origin/main 3db842df)
- HYPOTHESIS: the engine's deliverable and task states are `done`, `open`, `infeasible`, `deferred`, `removed` and none is `not_done` or `dropped` — confirm/refute at `pm-mcp-core` `doc/specification/phase-workflows/08-dynamic-re-planning.adoc` (verify-at-outline)
  - verdict: unverifiable | checked_at: 3db842df57c89ed737c5503ef40a2da00b9c7bc9 | by: ui-improvements/analyze | rescoped: n/a | evidence: engine spec in pm-mcp-core, not readable from this repo; settled at outline
- OBSERVED: the explorer renders `ReasonBadge reason="setup"` for any `needs_operator_count > 0` — read at `pm-web-app/src/lib/views/ProjectExplorerView.svelte:960-961,1277-1278`
  - verdict: corroborated | checked_at: 3db842df57c89ed737c5503ef40a2da00b9c7bc9 | by: ui-improvements/analyze | rescoped: n/a | evidence: ProjectExplorerView.svelte:932-933,1249-1250 ReasonBadge setup for needs_operator_count > 0; EnrolledProject.java:31 (origin/main 3db842df)
- OBSERVED: `saveIdentity` returns without a `projectId` and Save is disabled for it — read at `pm-web-app/src/lib/views/ProjectOnboardingView.svelte:201,680-681`
  - verdict: corroborated | checked_at: 3db842df57c89ed737c5503ef40a2da00b9c7bc9 | by: ui-improvements/analyze | rescoped: n/a | evidence: ProjectOnboardingView.svelte:201 saveIdentity returns without projectId; Save disabled :677-678 (origin/main 3db842df)
- OBSERVED: `needsTerminal` is true whenever `build_environment.captured_at` is absent at steps 1 to 3 — read at `pm-web-app/src/lib/views/ProjectOnboardingView.svelte:112-118`
  - verdict: corroborated | checked_at: 3db842df57c89ed737c5503ef40a2da00b9c7bc9 | by: ui-improvements/analyze | rescoped: n/a | evidence: ProjectOnboardingView.svelte:115-117 needsTerminal unchanged (origin/main 3db842df)
- OBSERVED: `openProjectOnboarding` pushes the route `project-onboarding`, whose path is `/projects/onboard` — read at `pm-web-app/src/lib/stores/runtime.ts:2033-2043` and `pm-web-app/src/lib/routes.ts:297`
  - verdict: corroborated | checked_at: 3db842df57c89ed737c5503ef40a2da00b9c7bc9 | by: ui-improvements/analyze | rescoped: n/a | evidence: runtime.ts:2087-2101 openProjectOnboarding pushRoute project-onboarding; routes.ts:297 /projects/onboard; matchPath fixed paths before /projects/([^/]+) (origin/main 3db842df)
- HYPOTHESIS: the real engine accepts `links.set` on a being-added run's entry before enrolment — confirm/refute at `pm-mcp-core` `doc/specification/state-catalogue/15-project-onboarding.adoc` and the engine's entry-link handling (verify-at-outline; a refusal stops deliverable 3's first item)
  - verdict: unverifiable | checked_at: 3db842df57c89ed737c5503ef40a2da00b9c7bc9 | by: ui-improvements/analyze | rescoped: n/a | evidence: real-engine acceptance of links.set before enrolment not shown by reading; mock serves it at MockRuntimeEngine.java:4930-4945,5001-5002
- OBSERVED: `Dialog.svelte` does not read `isComposing`; only `Compose.svelte` does — read by `git grep -n isComposing -- pm-web-app/src/lib/components`
  - verdict: corroborated | checked_at: 3db842df57c89ed737c5503ef40a2da00b9c7bc9 | by: ui-improvements/analyze | rescoped: n/a | evidence: Dialog.svelte:106-123 Escape without isComposing; Compose.svelte:161 reads it; wording 'only Compose' loose (ModelsChoice.svelte:419, NumberField.svelte:192, RecipeReadyView.svelte:46, forms/index.ts:44,70) (origin/main 3db842df)
- OBSERVED: `SourceIntakePanel.svelte` and `RecipeSelector.svelte` are imported by their own tests only — read by `git grep -ln "SourceIntakePanel\|RecipeSelector" -- pm-web-app/src`
  - verdict: corroborated | checked_at: 3db842df57c89ed737c5503ef40a2da00b9c7bc9 | by: ui-improvements/analyze | rescoped: n/a | evidence: git grep SourceIntakePanel|RecipeSelector finds only the two .test.ts files (origin/main 3db842df)
- OBSERVED: `LanListenerModal`'s turn-off button is `variant="secondary"` — read at `pm-web-app/src/lib/components/LanListenerModal.svelte:80`
  - verdict: corroborated | checked_at: 3db842df57c89ed737c5503ef40a2da00b9c7bc9 | by: ui-improvements/analyze | rescoped: n/a | evidence: LanListenerModal.svelte:80 variant=secondary (origin/main 3db842df)
- OBSERVED: the client has the entry-link call `/api/v1/entries/{id}/links/{act}` — read at `pm-web-app/src/lib/api/client.ts:3281`
  - verdict: corroborated | checked_at: 3db842df57c89ed737c5503ef40a2da00b9c7bc9 | by: ui-improvements/analyze | rescoped: n/a | evidence: client.ts:3283 actEntry builds /api/v1/entries/{id}/links/{act} (origin/main 3db842df)
- Verify-first clause: before each deliverable re-read the named files at the then-current HEAD; PLAN-144 (D0 and the Back removal), PLAN-154, 156 and 59 have landed or must have landed first, and PLAN-85 (staged) edits `routes.ts` and `SettingsView`. A refuted claim re-scopes the deliverable and is reported, never silently worked around.
  - verdict: unverifiable | checked_at: 3db842df57c89ed737c5503ef40a2da00b9c7bc9 | by: ui-improvements/analyze | rescoped: n/a | evidence: procedural verify-first clause; PLAN-144 (#542), 154, 156, 59 and 181 (#544) have landed

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/views/CompletionReportView.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/views/CompletionReportView.test.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/views/ProjectExplorerView.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/views/ProjectExplorerView.test.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/views/ProjectOnboardingView.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/views/ProjectOnboardingView.test.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/stores/runtime.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/stores/runtime.test.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/routes.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/routes.test.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/api/client.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/components/Dialog.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/components/Dialog.test.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/components/HoldResolutionModal.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/components/HoldResolutionModal.test.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/components/FindingsReviewModal.test.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/components/LanListenerModal.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/components/LanListenerModal.test.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/components/ConfinedFilePicker.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/components/ConfinedFilePicker.test.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/components/SourceIntakePanel.svelte` — present at HEAD 3ae8a305; removed by this plan
- OBSERVED: `pm-web-app/src/lib/components/SourceIntakePanel.test.ts` — present at HEAD 3ae8a305; removed by this plan
- OBSERVED: `pm-web-app/src/lib/components/RecipeSelector.svelte` — present at HEAD 3ae8a305; removed by this plan
- OBSERVED: `pm-web-app/src/lib/components/RecipeSelector.test.ts` — present at HEAD 3ae8a305; removed by this plan
- OBSERVED: `pm-web-app/src/lib/utils/loopback.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/utils/loopback.test.ts` — absent at HEAD 3ae8a305; created by this plan
- OBSERVED: `pm-api/src/main/resources/messages/common.en.properties` — present at HEAD 3ae8a305
- OBSERVED: `pm-api/src/main/resources/messages/common.de.properties` — present at HEAD 3ae8a305
- OBSERVED: `pm-api/src/main/resources/messages/reports.en.properties` — present at HEAD 3ae8a305
- OBSERVED: `pm-api/src/main/resources/messages/reports.de.properties` — present at HEAD 3ae8a305
- OBSERVED: `pm-api/src/main/resources/messages/projects.en.properties` — present at HEAD 3ae8a305
- OBSERVED: `pm-api/src/main/resources/messages/projects.de.properties` — present at HEAD 3ae8a305
- OBSERVED: `pm-api/src/main/resources/messages/onboarding.en.properties` — present at HEAD 3ae8a305
- OBSERVED: `pm-api/src/main/resources/messages/onboarding.de.properties` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-e2e/tests/projects.spec.js` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-e2e/tests/reports.spec.js` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-e2e/tests/control.spec.js` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-e2e/tests/grant-modal.spec.js` — absent at HEAD 3ae8a305; created by this plan
- OBSERVED: `pm-web-app/src/lib/navigation/history.ts` — present at HEAD 3db842df (added 2026-11-23: `writeAddress`, the one history writer, for deliverable 3's run address)
- OBSERVED: `pm-web-app/src/lib/shell/nav.ts` — present at HEAD 3db842df (added 2026-11-23: `:164` maps `project-onboarding` to `projects`)
- OBSERVED: `pm-web-app/src/lib/shell/nav.test.ts` — present at HEAD 3db842df (added 2026-11-23: `:74`)
- OBSERVED: `pm-web-app/src/lib/routeTitles.test.ts` — present at HEAD 3db842df (added 2026-11-23)

## Dependencies and Sequencing

- Depends on: `PLAN-144` (landed first: it edits `CompletionReportView`, `ProjectOnboardingView`, `ProjectExplorerView`, `ReportsView`, `routes.ts`, `runtime.ts`); `PLAN-154`, `PLAN-156`, `PLAN-59` (shipped).
- Overlaps with: `PLAN-85` (staged: `routes.ts`, `SettingsView.svelte`, `chrome.spec.js`), `PLAN-135`, `PLAN-136` (staged: `runtime.ts`, `routes.ts`, `common.*`). This plan is sequenced after `PLAN-144` and either order with those.
- Adjacent to: PLAN-155's open items (`Dialog` IME is the one this plan takes; the rest of that landing stays with its entry), PLAN-146's `OverSizeChart` on the setup tab (not mounted, not in this plan).

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-176-web-wire-values-and-onboarding-residue.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger write. The session reports its outcome through its PR and its inbox message (`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary).

## Re-Grounding 2026-11-23 (analyze, at `3db842df`)

PLAN-144 (#542, `43f0e416`) and PLAN-181 (#544, `3db842df`) have landed, so the "after PLAN-144" precondition holds. All nine observed claims hold, none is contradicted, no deliverable is rescoped; claims 1 and 6 stay verify-at-outline (engine), claim 11 (verify-first) is procedural and satisfied. Moved lines: `CompletionReportView.svelte` `:103-106` and `:407`; `ProjectExplorerView.svelte` `:932-933` and `:1249-1250`; `ProjectOnboardingView.svelte` Save `:677-678` (`:201` and `:115-117` unchanged); `runtime.ts` `:2087-2101`; `client.ts` `:3283`; mock `links.set` `MockRuntimeEngine.java:4930-4945,5001-5002`. Every "fails at `3ae8a305`" reads "fails at the plan's start HEAD".

PLAN-144 residue this plan must know (read at `3db842df`):

- The in-page Back and `onClose` are gone from `CompletionReportView` and `project-back` is gone from `ProjectOnboardingView`: a run is left by the rail only. `CompletionReportView.test.ts:487-510` still feeds the invented `not_done` and `dropped` states and is replaced by deliverable 1; `CompletionReportView` keeps its own in-page crumb (`report-breadcrumb`, `:327-340`) beside the top bar's trail: deliverable 1 decides whether it stays.
- `project-onboarding` has no entry in `navigation/chain.ts` (it handles `task`, `plan`, `epic`, `overview`) and no Up (`upOf` returns null for a non-lens route); the top bar shows the fallback "Projects" crumb plus `ROUTE_TITLES['project-onboarding']` (`routes.ts:258`). Deliverable 3's run address is written through `navigation/history.ts` `writeAddress` and `routes.ts` (fixed path `:297`, `matchPath`, `formatAddress`; a fixed path is matched before `/projects/([^/]+)`); `runtime.ts` must restore `selectedOnboardingId` from it (the router, `runtime.ts` ~1735-1775, does not). `routes.ts:742` says the `/projects/onboard` addresses "belong to another plan": this one. `pm-web-e2e/tests/projects.spec.js:116` asserts `toHaveURL(/\/projects\/onboard$/)` and changes with the route. Whether the run gets a chain entry and an Up target is deliverable 3's decision, stated in its landing.
- PLAN-144 added the keys `project.explorer.plans_count`, `epics_count` and `needs_you` (`projects.*.properties:173-175`), with no importer found: deliverable 2 may reuse them (confirm at outline).
- `Dialog.svelte:114-118` already keys the Escape yield to the focused element (`Dialog.test.ts:218,313`); deliverable 4 re-reads the "pinned only against removing it" wording at outline and adds the IME case. The claim "only `Compose.svelte` reads `isComposing`" was loose: `ModelsChoice.svelte:419`, `NumberField.svelte:192`, `RecipeReadyView.svelte:46` and `forms/index.ts:44,70` read it too, and `forms/index.ts` may offer a shared guard to reuse.
- Four files added to the Expected Surface above: `navigation/history.ts`, `shell/nav.ts`, `shell/nav.test.ts`, `routeTitles.test.ts`.
