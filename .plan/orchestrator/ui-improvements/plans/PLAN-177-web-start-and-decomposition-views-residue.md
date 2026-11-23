# PLAN-177: Web: The Start Wizard's Ready Step and the Decomposition Review Do What They Show (Residue of PLAN-155)

epic: ui-improvements
workstream: WS-03

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Staged on the operator's word of
> 2026-11-23 (decision 2, the rest, proposal 1): the five layer-3 majors of the PLAN-155 landing (#531) and its minors and test
> gaps. This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief. **It starts after PLAN-144
> has landed** (PLAN-144 removes the Back of `EpicDecompositionView`, `EpicAdoptView` and others) and is sequenced with PLAN-176.

## Objective

PLAN-155 put the start steps and the epic decomposition on the shared vocabulary and left views that look right and do less than they show. The recipe's ready step has a primary act and an Enter handler that nothing wires, so in the product they are inert; its parameter step grew a free-text field the restyle was not to add; the decomposition review shows an order and a deliverables row the wire has no fields for, merges a new Objective into the instruction on send and sends nothing when it is unchanged; and Drop marks a plan but Agree commits it. This plan makes each of them do what it shows, against the wire, with tests that fail when the production line is reverted.

## Deliverables

The documents are the specification; read them first: `doc/design/web-app/components.adoc` (#wizard, #compose, the primary rule: one primary per screen), `start.adoc` and `epic-decomposition.adoc` (the steps and the review) of `doc/design/web-app/`, and `landings/PLAN-155.md` for every finding with its file and line. The `Dialog` IME point (`Dialog.svelte`, Escape cancelling an IME candidate) is deliverable 4 of PLAN-176 and not repeated here.

1. **The start wizard's ready step works**
 - What:
   - `StartView` passes no `onSubmit` to `RecipeReadyView` (`StartView.svelte:1045-1075`), so the view's primary `ready-start-act` and its Enter handler (`RecipeReadyView.svelte:45-49,161-172`) are inert in the product; the real primaries stay in the `WizardPage` slot (`StartView.svelte:1444-1461`). Decide at outline, against `components.adoc`, which one is the screen's primary: wire the view's act and remove the slot's duplicate, or remove the view's act and keep the slot's; never two primaries on one screen. Enter submits what the primary submits.
   - A recipe parameter with no options and `available !== false` renders a free-text `TextField` whose value flows through `onParameter` into the parameters map and the `set` act (`RecipeReadyView.svelte:82-90`, `StartView.svelte:1050`); the base showed the "no options" message. A restyle adds no input path: restore the message, unless the engine publishes a free-text parameter (read `pm-mcp-core` `state-catalogue` for the recipe's parameters at outline); if it does, say so in the PR and ask the operator, do not decide it.
   - The Enter guard of `RecipeReadyView` checks `event.isComposing` only (`:46`); `Compose.svelte:156-168` also handles the Safari confirm (`isComposing` false, `keyCode` 229): use the same guard, once, in a shared function.
   - The adopt step draws one primary Adopt per epic (`EpicAdoptView.svelte:46-58`), several primaries on one screen and none in the `WizardPage` slot: one primary for the step.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `StartView.test.ts` and `RecipeReadyView.test.ts` drive the step through `StartView` without injecting `onSubmit`: the primary starts the recipe by click and by Enter, there is exactly one primary on the screen, a parameter with no options shows the message and no field, and the IME confirm does not submit; each assertion fails with the production line reverted. Evidence order: `test-first`.
   - `EpicAdoptView.test.ts`: two adoptable epics show one primary. Evidence order: `test-first`.
2. **The decomposition review tells the truth**
 - What:
   - The review's Order row shows `plan.order ?? stream.order ?? 1`, so every plan of a workstream shows the workstream's number as its own order, and the Deliverables row is always `—`: the wire has no such fields (`StagedPlanRecord.java:40-47`, `DecompositionReviewView.svelte:99-105,122`). Show what the wire carries; drop a row the wire cannot fill, or ask the engine side for the field (a finding for the operator, not a decision here).
   - The new Objective field is merged into the instruction on send (`DecompositionReviewView.svelte:43-65`, `objective\ninstruction`), where the base sent the instruction only; and an unchanged objective with an empty instruction leaves the send enabled while `submitInstruct` returns silently (`:59`). Send what the base sent; an act that cannot send is disabled with its reason (`components.adoc` § disabled acts).
   - Drop writes only the local `droppedPlans`, and Agree calls `checkDecomposition` and `commitDecomposition` without it (`EpicDecompositionView.svelte:190-215`): a plan marked dropped and dimmed is still committed (pre-existing, in the code the restyle touched). Send the drops with the check and the commit, or do not offer Drop; settle against the engine's decomposition contract at outline.
   - `checkStateOf` has no branch for locked, dropped or committed plans (`EpicDecompositionView.svelte:111-130`): such a plan shows the success tone and "checked", and a plan with dependencies shows "waiting" though its checks passed. A failed `submitInstruct` draws its error behind the dialog backdrop (`:312-316`); the Add a plan label carries a `+ ` glyph (`:464`, `components.adoc:337`: no glyph as a label) and the acts that open a dialog end in the ellipsis; the Compose send is named `Edit` and `Draft again` beside buttons of the same name and is disabled with no reason (`DecompositionReviewView.svelte:270,273`).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `DecompositionReviewView.test.ts` and `EpicDecompositionView.test.ts` are fed the plan records the daemon sends (`StagedPlanRecord` fields only): the order row, the objective send, a dropped plan not committed, the states of locked, dropped and committed plans, the error inside the dialog, and the disabled reason of the send; each fails with its production line reverted. Evidence order: `test-first`.
3. **The tests that could not fail**
 - What:
   - `RecipeReadyView.test.ts` passes only because it injects `onSubmit`, and pins the free-text path (`:29-35,48-52`): rewrite it with deliverable 1. The "focus held" clause of PLAN-155 D1 is asserted in neither the view test nor `decomposition.spec.js:37-41` (Escape only): assert the focus on open and after Escape, and Tab inside the dialog.
   - The D5 "mutation evidence" of PLAN-155 mutated the tests' own expectations (`StartView.test.ts:282`, `MockRuntimeEngineTest.java:810`), not the production lines: re-run the mutation on the production line for each (`btn--primary` of the start act, the `scope_not_found` code) and record the failing run; a pin that already existed is said to be one.
   - `Compose.svelte:58-61` has new props with no direct test in `Compose.test.ts` (it is not in the diff of PLAN-155): add one per prop; the adopt-failure test asserts the cause text, not only that `epic-adopt-failed` exists.
 - Persona: plan-marshall:persona-module-tester
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - Each rewritten or added test is shown to fail with the production line it names reverted, with the failing run cited. Evidence order: `mutation-after`.
4. **Browser evidence**
 - What: `decomposition.spec.js` and `start.spec.js` on the scenarios PLAN-155 added (`running-work.json` carries the epic `platform-setup`; check the overview counts, the web e2e and the CLI tests that read that scenario still pass): the start of a recipe from the ready step by click and Enter, the decomposition review with a dropped plan, and the targets below.
 - Persona: plan-marshall:persona-integration-tester
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - The e2e cases pass with `PM_MOCK_WEB_PORT` and `PM_WEB_PORT` set and wait on the address and the DOM; the full `-Pe2e` run at the head is green and cited. Evidence order: `test-first`.
   - Browser check (Playwright MCP): the ready step, the adopt step and the decomposition review in `en` and `de`, light and dark, at `1440 × 900` and `390 × 844`, taken after the last visual commit. Evidence order: `none`.

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`): `./mvnw -pl pm-api,pm-web-app verify`, `./mvnw -pl pm-api,pm-web-app verify -Ppre-commit` (review every rewrite), `PM_MOCK_WEB_PORT=<mock port> PM_WEB_PORT=<app port> ./mvnw -pl pm-web-e2e verify -Pe2e` at the head, and `-Pcoverage` at the head after the last code commit (minimum 80 % branch for `pm-web-app`).
- No new dependency or plugin. Web standards of `CLAUDE.md`; no static `style` attribute; every new text from the catalogue in English and German; the text guard baseline does not grow. Pre-1.0: no shim.
- A test is fed with the values the wire sends and the engine's records (`StagedPlanRecord`), never with a value the code invented, and every new or rewritten assertion is shown to fail with the production line reverted.
- A finding that needs a product or engine decision (a free-text parameter, a field the wire lacks, the Drop contract) is reported to the operator in the pull request and in the landing message, never decided in the code.
- Ports of this plan: mock web port 21770, app port 21771 (`20000 + 10 × 177`) (`AGENTS.md` § Ports). Other plans run at the same time: never stop or reuse a process this worktree did not start.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.
- The merge rule: the run merges only when every review thread has a reply and is resolved and every review body is answered, and some head has had one CodeRabbit review. The pull request body states thread count, resolved count and review-body items from `ci pr comments`, never from memory. The run merges the pull request itself once these gates hold (`CLAUDE.md`, Git Workflow). Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`; the pull request body carries no generator footer. If CodeRabbit refuses on quota, stop and ask the operator.
- ZERO Sonar findings (the operator, 2026-11-05): the pull request merges with 0 open new-code issues, every open issue fixed in a commit of THIS pull request; state `sonar_open_new_issues=N` in the landing message (`unknown` for a failed read, never `0`). A dismissal needs the operator's word.
- A `test-first` clause cites a failing run at the plan's start and a passing run; where the base-sha instrument is unavailable, say so and do not relabel the clause.

## Claim Labels

- OBSERVED: `StartView` renders `RecipeReadyView` for the parameters and ready steps without an `onSubmit` — read at `pm-web-app/src/lib/views/StartView.svelte:1045-1075`
  - verdict: corroborated | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: git grep RecipeReadyView in StartView.svelte finds 1045 and 1054; lines 1040-1078 carry no onSubmit (origin/main 3ae8a305)
- OBSERVED: `RecipeReadyView` renders a free-text `TextField` for a parameter with no options — read at `pm-web-app/src/lib/views/RecipeReadyView.svelte:82-90`
  - verdict: corroborated | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: RecipeReadyView.svelte:82-90 else-if parameter.available !== false renders a TextField with oninput -> onParameter (origin/main 3ae8a305)
- OBSERVED: the Enter guard of `RecipeReadyView` reads `isComposing` only — read at `pm-web-app/src/lib/views/RecipeReadyView.svelte:46`, against `pm-web-app/src/lib/components/Compose.svelte:156-168`
  - verdict: corroborated | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: RecipeReadyView.svelte:46 key Enter && !event.isComposing -> onSubmit; Compose.svelte:156-161 handles the Safari confirm (origin/main 3ae8a305)
- OBSERVED: the review's Order row reads `plan.order ?? stream.order ?? 1` — read at `pm-web-app/src/lib/views/DecompositionReviewView.svelte:122`
  - verdict: corroborated | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: DecompositionReviewView.svelte:121-123 value String(plan.order ?? stream.order ?? 1) (origin/main 3ae8a305)
- OBSERVED: the Objective is merged into the instruction on send — read at `pm-web-app/src/lib/views/DecompositionReviewView.svelte:43-65`
  - verdict: corroborated | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: DecompositionReviewView.svelte:55-60 objectiveChanged sends the trimmed objective as the text, empty text returns silently; the merged objective+instruction branch is :43-54 per the PLAN-155 landing, not re-read
- OBSERVED: `drop` writes the local `droppedPlans` only — read at `pm-web-app/src/lib/views/EpicDecompositionView.svelte:190-215`
  - verdict: corroborated | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: EpicDecompositionView.svelte:194-198 drop only appends to the local droppedPlans; agree's calls per the PLAN-155 landing (:200-215), not re-read (origin/main 3ae8a305)
- OBSERVED: `checkStateOf` has no branch for locked, dropped or committed plans — read at `pm-web-app/src/lib/views/EpicDecompositionView.svelte:111-130`
  - verdict: unverifiable | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: EpicDecompositionView.svelte:111-118 first branch (conflicts -> danger) read; the missing locked/dropped/committed branches are the landing's reading, settled at outline
- OBSERVED: `EpicAdoptView` draws one primary Adopt per epic — read at `pm-web-app/src/lib/views/EpicAdoptView.svelte:46-58`
  - verdict: corroborated | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: EpicAdoptView.svelte:46-52 each epic carries its own Button variant=primary (origin/main 3ae8a305)
- HYPOTHESIS: the wire has no per-plan order and no deliverables field — confirm/refute at `pm-api/src/main/java/de/planmarshall/api/` `StagedPlanRecord.java:40-47` (verify-at-outline)
  - verdict: unverifiable | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: StagedPlanRecord fields not re-read here; the PLAN-155 landing read them at :40-47; settled at outline
- HYPOTHESIS: the engine's decomposition check and commit accept the dropped plans — confirm/refute at `pm-mcp-core` `doc/specification/state-catalogue/` (the decomposition state) and `pm-web-app/src/lib/api/client.ts` `checkDecomposition`, `commitDecomposition` (verify-at-outline)
  - verdict: unverifiable | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: the engine's decomposition contract for dropped plans is not shown by reading; settled at outline
- Verify-first clause: before each deliverable re-read the named files at the then-current HEAD; PLAN-144, PLAN-176 and PLAN-156 are the plans that edit the same files (`EpicDecompositionView`, `EpicAdoptView`, `Dialog`). A refuted claim re-scopes the deliverable and is reported, never silently worked around.
  - verdict: unverifiable | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: procedural verify-first clause; PLAN-144 running

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/views/StartView.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/views/StartView.test.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/views/StartView.scenarios.test.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/views/RecipeReadyView.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/views/RecipeReadyView.test.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/views/EpicAdoptView.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/views/EpicAdoptView.test.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/views/EpicDecompositionView.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/views/EpicDecompositionView.test.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/views/DecompositionReviewView.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/views/DecompositionReviewView.test.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/components/Compose.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/components/Compose.test.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/templates/WizardPage.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/api/client.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-api/src/main/resources/messages/start.en.properties` — present at HEAD 3ae8a305
- OBSERVED: `pm-api/src/main/resources/messages/start.de.properties` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-e2e/tests/decomposition.spec.js` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-e2e/tests/start.spec.js` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-e2e/tests/theme-readability.spec.js` — present at HEAD 3ae8a305

## Dependencies and Sequencing

- Depends on: `PLAN-155` (shipped); `PLAN-144` (landed first: it removes the Back of `EpicDecompositionView` and `EpicAdoptView`).
- Overlaps with: `PLAN-176` (staged: `Dialog.svelte` IME, `runtime.ts`), `PLAN-137` (parked: `StartView`, `EpicView` chat places), `PLAN-135`, `PLAN-136` (staged: `client.ts`). Sequenced after `PLAN-144`; either order with the others.
- Adjacent to: `PLAN-53` (shipped, the start wizard), the `StartWorkModal` removal of PLAN-156 (no importer left).

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-177-web-start-and-decomposition-views-residue.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger write. The session reports its outcome through its PR and its inbox message (`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary).
