# PLAN-187: Web: The Decomposition Offers No Drop, as the Terminal Does (the PLAN-177 Residue)

epic: ui-improvements
workstream: WS-03

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Staged on the operator's word of
> 2026-11-24 ("as the command line does"), which settles PLAN-177's layer-3 finding L3-1 (`landings/PLAN-177.md`): the
> decomposition contract has no act that drops a plan, so the web app withdraws Drop instead of faking it with a rewrite.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

The web app offers _Drop_ on a staged plan in two places, the Start wizard's decomposition review and the epic's decomposition view. Neither has a wire act behind it: both send `instruct {plan_id: null, action: 'rewrite', instruction: "Drop the plan X from the cut."}`. Against the mock a null plan id rewrites the **first open plan**: its objective becomes the drop sentence, its status `changed`, the "dropped" plan stays, and Agree commits the corrupted cut. The terminal client already settles the contract: "The decomposition contract has no act that drops a plan, so no drop key is offered" (`EpicDecompositionDialogue.java:287-289`). The result of this plan: no client offers Drop; a plan the operator does not want is removed by an instruction the operator writes and reads back (_Decompose again_ / the instruct composer), exactly as in the terminal; the tests that pinned the fake drop are gone; and the design says what the code does.

## Deliverables

The documents are the specification; read them first: `doc/design/shared/starting-work.adoc` (the review's _Keep_, _Edit_, _Drop_ at `:206`; the later-run states at `:212`), `doc/design/shared/starting-work-texts.adoc`, `doc/design/shared/work-actions.adoc:225` (_Decompose again_ takes an instruction such as "drop the docs plan"), the terminal's `pm-operator/src/main/java/.../EpicDecompositionDialogue.java:287-289`, and `landings/PLAN-177.md` (L3-1, L3-2, L3-3 and the lane's §9).

1. **Drop leaves both decomposition views**
 - What: remove the Drop act and its local state from `EpicDecompositionView.svelte` (`droppedPlans` `:31`, the drop/undo functions `:220-247` including the rewrite sent before check and commit, the button `:454-456`, the local dimming `:123` and the Agree gate's reading of dropped plans `:507`) and from the Start wizard (`DecompositionReviewView.svelte` `onDrop` `:22,212-214`; `StartView.svelte` `dropPlan` `:852-854` and its wiring `:1160`). The wire's own `dropped` status of a plan dropped on a later run stays shown as today (`decomposition.check.dropped`, `StagedPlanRecord` status `dropped`); only the client's fake act goes. Remove the now unused keys `decomposition.drop` and `start.decomposition.drop` (en and de; check `pm-operator` and `pm-api` Java for a user first — none at `adb0d310`) and their entry in `catalogue.test.ts:335`. Nothing replaces Drop in the UI: the instruct composer the views already carry is the way to ask for a plan's removal, and its answer is read back before Agree.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `EpicDecompositionView.test.ts`, `DecompositionReviewView.test.ts` and `StartView.test.ts` assert that no plan offers a Drop act (by role and name, in English and German) and that a plan the wire reports `dropped` still reads as dropped; the three tests that pinned the fake drop (`EpicDecompositionView.test.ts:366-399`, `DecompositionReviewView.test.ts:85-102`, `StartView.test.ts:310-346` drop leg) are removed or rewritten to the new behaviour; `git grep -n "Drop the plan" -- pm-web-app/src` is empty; each new assertion fails with its production line reverted. Evidence order: `test-first`.
   - The Agree gate counts only the findings the wire reports for the cut (no client-side dropped list). Evidence order: `none`.
   - `decomposition.spec.js:76-88` (the drop leg) is rewritten to the new behaviour (no Drop control; a plan the wire reports `dropped` still reads as dropped), and `decomposition.spec.js` and `start.spec.js` pass under `-Pe2e` at the head with `PM_MOCK_WEB_PORT` and `PM_WEB_PORT` set (waits on the address and the DOM, no `waitForTimeout`). Evidence order: `test-first`.
   - Browser check (Playwright MCP) of the epic decomposition and the Start wizard's review in `en` and `de`, light and dark, at `1440 × 900` and `390 × 844`, after the last visual commit. Evidence order: `none`.
2. **The design says what the clients do**
 - What: `starting-work.adoc:206` lists _Drop_ among a plan's acts: replace it with the terminal's wording (a plan is kept or edited; removing one is an instruction to the decomposition, read back before agreement) and remove `start.decomposition.drop` / `Drop` from `starting-work-texts.adoc:317-318`. Fold in PLAN-177's text drift in the same file: `starting-work-texts.adoc:57-59` still prints `start.gate.slow` as "within 20 seconds" while the catalogue takes `{seconds}`, and the four new keys `start.decomposition.send`, `.send_draft`, `.instruct_held`, `.new_plan` are missing from the table (the `:313/321/349` rows are correct: refuted at the PLAN-177 landing).
 - Persona: plan-marshall:persona-documenter
 - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation
 - Done when:
   - Every key `starting-work-texts.adoc` lists exists in the catalogue and every catalogue `start.decomposition.*` key the views use is listed; a search of the shared design for a decomposition Drop act finds none. Evidence order: `none`.

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`): `./mvnw -pl pm-api,pm-web-app verify`, `./mvnw -pl pm-api,pm-web-app verify -Ppre-commit` (review every rewrite; the gate is not idempotent on main: revert rewrites of files outside this plan or commit them separately on the operator's word), `PM_MOCK_WEB_PORT=<mock port> PM_WEB_PORT=<app port> ./mvnw -pl pm-web-e2e verify -Pe2e` at the head, and `-Pcoverage` at the head after the last code commit (minimum 80 % per module). The full `./mvnw verify` passes (the CLI modules read the same catalogue).
- No new dependency or plugin. Web standards of `CLAUDE.md`; no static `style` attribute; every text from the catalogue in English and German; the text guard baseline does not grow. Pre-1.0: no shim for a removed key or act.
- The engine's contract is the contract: no client act without a wire act behind it; a test is fed with what the engine sends and the mock sends it too. Do not change the mock's `instruct` handling or add a drop act anywhere (an engine drop act is a `pm-mcp-core` decision, not this plan's).
- Ports of this plan: mock web port 21870, app port 21871 (`20000 + 10 × 187`) (`AGENTS.md` § Ports). Other plans run at the same time: never stop or reuse a process this worktree did not start.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.
- The merge rule: the run merges only when every review thread has a reply and is resolved and every review body is answered, and some head has had one CodeRabbit review. The pull request body states thread count, resolved count and review-body items from `ci pr comments`, never from memory. The self-review round runs BEFORE the pull request (PLAN-177 ran it after the merge). The run merges the pull request itself once these gates hold (`CLAUDE.md`, Git Workflow). Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`; the pull request body carries no generator footer. If CodeRabbit refuses on quota, stop and ask the operator.
- ZERO Sonar findings (the operator, 2026-11-05): the pull request merges with 0 open new-code issues, every open issue fixed in a commit of THIS pull request; state `sonar_open_new_issues=N` in the landing message (`unknown` for a failed read, never `0`). A dismissal needs the operator's word.
- A `test-first` clause cites a failing run at the plan's start; where the base-sha instrument is unavailable, say so and do not relabel the clause.

## Claim Labels

- OBSERVED: the epic decomposition sends a drop as a null-plan rewrite before check and commit — read at `pm-web-app/src/lib/views/EpicDecompositionView.svelte:237-247` (orchestrator re-read at `e0e95c05`, 2026-11-24)
- OBSERVED: the mock resolves a null plan id to the first open plan and rewrites its objective — read at `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java:4673-4679,4688-4700` (orchestrator re-read at `e0e95c05`)
- OBSERVED: the terminal offers no drop because the contract has none — read at `EpicDecompositionDialogue.java:287-289` (orchestrator re-read at `e0e95c05`)
- OBSERVED: the Start wizard drops through the same rewrite — read at `pm-web-app/src/lib/views/StartView.svelte:852-854,1160` and `DecompositionReviewView.svelte:22,212-214` (read at `adb0d310`)
- OBSERVED: `decomposition.drop` and `start.decomposition.drop` have no Java user — read by `git grep` over `pm-operator` and `pm-api/src/main/java` at `adb0d310` (0 lines); the web uses are `EpicDecompositionView.svelte:456`, `DecompositionReviewView.svelte:214`, `catalogue.test.ts:335`
- Verify-first clause: before each deliverable re-read the named files at the then-current HEAD (PLAN-185 and PLAN-178 are running on other web surfaces; PLAN-137, parked, touches `StartView`). A refuted claim re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/views/EpicDecompositionView.svelte` — present at HEAD adb0d310
- OBSERVED: `pm-web-app/src/lib/views/EpicDecompositionView.test.ts` — present at HEAD adb0d310
- OBSERVED: `pm-web-app/src/lib/views/DecompositionReviewView.svelte` — present at HEAD adb0d310
- OBSERVED: `pm-web-app/src/lib/views/DecompositionReviewView.test.ts` — present at HEAD adb0d310
- OBSERVED: `pm-web-app/src/lib/views/StartView.svelte` — present at HEAD adb0d310
- OBSERVED: `pm-web-app/src/lib/views/StartView.test.ts` — present at HEAD adb0d310
- OBSERVED: `pm-web-app/src/lib/i18n/catalogue.test.ts` — present at HEAD adb0d310
- OBSERVED: `pm-api/src/main/resources/messages/decomposition.en.properties` — present at HEAD adb0d310
- OBSERVED: `pm-api/src/main/resources/messages/decomposition.de.properties` — present at HEAD adb0d310
- OBSERVED: `pm-api/src/main/resources/messages/start.en.properties` — present at HEAD adb0d310
- OBSERVED: `pm-api/src/main/resources/messages/start.de.properties` — present at HEAD adb0d310
- OBSERVED: `pm-web-e2e/tests/decomposition.spec.js` — present at HEAD adb0d310
- OBSERVED: `pm-web-e2e/tests/start.spec.js` — present at HEAD adb0d310
- OBSERVED: `doc/design/shared/starting-work.adoc` — present at HEAD adb0d310
- OBSERVED: `doc/design/shared/starting-work-texts.adoc` — present at HEAD adb0d310

## Dependencies and Sequencing

- Depends on: `PLAN-177` (#552, shipped 2026-11-24: the decomposition views as they stand).
- Overlaps with: `PLAN-137` (parked: `StartView.svelte`, `start.*`), `PLAN-135`, `PLAN-155`, `PLAN-158` (staged or parked: `StartView` / decomposition files; decided at emit by the surface read). No shared file with the running PLAN-183, 186, 185, 178.
- Adjacent to: an engine drop act in `pm-mcp-core` (not this plan; if the engine ever gains one, a later plan offers Drop again on that act).

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-187-web-withdraw-decomposition-drop.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger write. The session reports its outcome through its PR and its inbox message (`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary).
