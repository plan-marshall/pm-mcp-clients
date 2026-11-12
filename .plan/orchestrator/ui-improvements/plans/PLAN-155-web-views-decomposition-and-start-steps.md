# PLAN-155: Web Views: The Decomposition and the Start Sub-Steps on the D1 Vocabulary (Views Package, Part 3)

epic: ui-improvements
workstream: WS-03

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Drafted 2026-11-10 from the AGREED D1 documents and `shared/starting-work.adoc` § `#decomposition`, `#adopt`, `#recipe`.
> SELF-SUFFICIENT: the emitted command is a one-line pointer.

## Objective

The decomposition of an epic (`EpicDecompositionView`, `DecompositionReviewView`) and two sub-steps of the start flow that no staged plan owns (`EpicAdoptView`, `RecipeReadyView`) are built from raw buttons, text areas, inputs and a hand-drawn modal.
This plan moves them onto the D1 vocabulary (buttons by the hierarchy, `Compose` for the instruct fields, `StatusLabel` and icons for the checks, `Dialog`/`ConfirmDialog`, `RadioGroup`, `SelectField`). It lands BEFORE PLAN-137, which adopts the chat on the decomposition.

## Deliverables

Read first: `doc/design/web-app/components.adoc` (§ `#hierarchy`, `#labels`, `#dialog`), `components-fields.adoc` (§ `#compose`, `#field-set`, `#form-shape`), `components-tags.adoc` § `#status-label`, `shared/starting-work.adoc` (§ `#decomposition`, `#adopt`, `#recipe`), `shared/starting-work-texts.adoc`.

Not in this plan: `StartView`, its footer and its steps (PLAN-53 owns them, with `RecipeSelector`, `SourceList`, `SourceIntakePanel` and the source modals); the chat in the decomposition (PLAN-137); the column engine (D2).

1. **The decomposition view**
 - What:
   - `EpicDecompositionView.svelte`: the workstream groups, the plans with their check marks (`✓ ✗ ○ ●` of the design sketch) become `StatusLabel` tones with their word (checked, check failed with its reason, waiting for another plan, edited) and an icon, never a glyph alone; the
     seven raw buttons follow the hierarchy: _Agree_ the one primary, disabled with its reason `N checks fail` (`disabledReason`) until every check passes; _Add a plan_, _Draft again…_ and _Keep_/_Edit_/_Drop_ secondary; leaving ghost.
   - The instruct field (the operator's words for _Edit_, _Add a plan_, _Draft again_) is `Compose`; the hand-drawn modal (the `role="dialog"` block, `z-index: 50`) becomes a `Dialog`.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `EpicDecompositionView.test.ts` and `pm-web-e2e/tests/decomposition.spec.js` show: the check states by word, _Agree_ disabled with the count of failing checks, the instruct field through `Compose`, the modal through `Dialog` (focus held, `Escape` closes). Fail on a revert. Evidence order: `test-first`.
   - `npm run lint:vocabulary` reports 0 findings for `EpicDecompositionView.svelte`; no `z-index` or `role="dialog"` remains; it leaves the layer allowlist. Evidence order: `none`.
 - Implements: `components.adoc` § `#hierarchy`, `#dialog`; `components-fields.adoc` § `#compose`; `shared/starting-work.adoc` § `#decomposition`.
2. **The decomposition review**
 - What: `DecompositionReviewView.svelte` (13 raw buttons, 2 text areas): _Keep_, _Edit_, _Drop_ secondary; the read-back of old against new with _Accept_ primary, _Instruct again_ secondary, _Discard_ ghost; the text areas become `Compose` (the operator's words) and `TextArea` (an edited objective);
   old and new are shown with the signs `−` and `+` and no colour of their own (the diff tokens belong to the chat's proposal card). A plan opens to its objective, deliverables, surface, dependencies and order in a `KeyValueList`.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `DecompositionReviewView.test.ts`: the three actions of a plan, the read-back with its three acts, the instruct field, a plan with no deliverables. Fails on a revert. Evidence order: `test-first`.
   - `npm run lint:vocabulary` reports 0 findings for the file (evidence order: `none`).
 - Implements: `components.adoc` § `#hierarchy`; `shared/starting-work.adoc` § `#decomposition`.
3. **Adopt an epic and the recipe's ready step**
 - What:
   - `EpicAdoptView.svelte` (5 raw buttons): _Adopt {epic}_ primary, _Start a new epic_ secondary, _Try again_ (`common.action.retry`) and _Back_ ghost; the failure cause as text under the `ErrorState` pattern.
   - `RecipeReadyView.svelte` (2 buttons, 2 inputs, 1 select): the recipe's parameters through `Field`, `TextField`, `SelectField`; the one primary names the act.
   - Their footers inside `StartView` stay PLAN-53's.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - New `EpicAdoptView.test.ts` and `RecipeReadyView.test.ts` (neither exists): every act by role, a failed adopt with its cause and _Try again_, a recipe parameter edited and submitted with `Enter`. Fail on a revert. Evidence order: `test-first`.
   - `npm run lint:vocabulary` reports 0 findings for both files (evidence order: `none`).
 - Implements: `components.adoc` § `#hierarchy`; `components-fields.adoc` § `#form-shape`; `shared/starting-work.adoc` § `#adopt`, `#recipe`.
4. **Browser evidence**
 - What: the four views in a browser through `StartView` and the decomposition route, desktop and phone, both locales and themes.
 - Persona: plan-marshall:persona-integration-tester
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - Browser check (Playwright MCP) on the scenario `decomposition.spec.js` uses and on `projects-and-start` at 1440×900 and 390×844, `de` and `en`, both themes; baseline and after screenshots kept in the plan's working area, re-taken after any later visual fix (the pull request lists them after the push; not a gate before it); `theme-readability.spec.js` passes for the views (evidence order: `none`).

5. **What PLAN-158 left in the start steps and the mock (from its landing, folded 2026-11-11 on the operator's decision)**
 - What: read `landings/PLAN-158.md` of the epic; lines as read at `4a84f727`.
   - `StartView.svelte:790-800`: the decomposition error with its Retry is a raw `<button data-testid="decomposition-retry">`
     with no test; use the shared `Button` and add the test.
   - The mock's not-found codes are pinned per code, not per site: the project-route decomposition handlers
     (`MockRuntimeEngine.java` ~`:3483,3500,3533,3638`, only the status asserted in `DecompositionScenarioTest:222`), the
     device DELETE (~`:2283`), the chat turns POST (`ControlScenarioTest` asserts the status only), the accept-edit store
     (~`:8250`) and the archive-check target arm (`:5015`): assert the code at each site. An unknown plan answers
     `scope_not_found` for tokens and the plan view but `target_not_found` for its chat (`ControlScenarioTest`
     `unknownSubject`): make plans follow the split epics follow (`:5044,5053`).
 - Persona: plan-marshall:persona-module-tester
 - Skills: pm-dev-frontend:jest-testing, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - The StartView test and each per-site pin fail when their line is reverted (evidence order: `mutation-after`).

Not split: five deliverables; deliverable 5 is PLAN-158's residue the operator folded here (2026-11-11). Rationale recorded by the orchestrator.

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`): `./mvnw -pl pm-web-app verify`, `-Ppre-commit` (review every rewrite), and at the head `PM_MOCK_WEB_PORT=<mock port> PM_WEB_PORT=<app port> ./mvnw -pl pm-web-e2e verify -Pe2e`.
- No new dependency or plugin. Web standards of `CLAUDE.md`: ES modules, flat ESLint, no `innerHTML`/`outerHTML`/`insertAdjacentHTML`/`{@html}` with runtime or model text (a plan's objective or reason from the planning role is untrusted text), no timer waits in tests, the npm scripts stay.
- A pure restyle: no change to what a view shows, sends or stores; where a rule of the design cannot be met without a behaviour change, record it as a finding in the pull request.
- Every new text comes from the catalogue in English and German; the text guard baseline does not grow.
- Ports of this plan: mock web port 21550, app port 21551 (`20000 + 10 × 155`) (`AGENTS.md` § Ports). Never stop or reuse a process this worktree did not start.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.
- Every `test-first` clause cites a FAILING and a PASSING run; every `mutation-after` clause cites the reverted code and the failing assertion from a log quoted in the pull request body.
- **The merge rule:**
  - At least one CodeRabbit review of some head with its findings handled; every review BODY answered where the reviewer reads it; every thread replied and resolved. If CodeRabbit refuses on quota, stop and ask the operator.
  - The pull request body states `unresolved_review_threads=N` and `review_body_findings_total/handled`, read through `ci pr comments`.
  - The run merges the pull request itself once these gates hold, without a further word of the operator (`CLAUDE.md`, Git Workflow, #423). Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`. The pull request body carries no generator footer.
- ZERO Sonar findings (the operator, 2026-11-05): 0 open new-code issues, every open issue fixed in a commit of THIS pull request; state `sonar_open_new_issues=N` in the landing message.
  A dismissal is made only on the operator's selection through `sonar_rest transition`; the lane never records one of its own.

## Claim Labels

- OBSERVED: `EpicDecompositionView.svelte` (713 lines) has 7 raw buttons, 1 `<textarea>`, a hand-drawn `role="dialog"` and `z-index: 50` (line 668), and uses `Button` and `Spinner` — read at the file
- OBSERVED: `DecompositionReviewView.svelte` (398 lines) has 13 raw buttons and 2 `<textarea>`s, and uses `Icon` only; `EpicAdoptView.svelte` (121) has 5 raw buttons and no component; `RecipeReadyView.svelte` (234) has 2 raw buttons, 2 `<input>`s and 1 `<select>` — read at the files
- OBSERVED: `DecompositionReviewView`, `EpicAdoptView` and `RecipeReadyView` are rendered by `StartView.svelte`; `EpicDecompositionView` by `AppRouter.svelte` — read by `git grep -l` of each name
- OBSERVED: `EpicDecompositionView.test.ts` and `DecompositionReviewView.test.ts` exist; `EpicAdoptView` and `RecipeReadyView` have no test; `pm-web-e2e/tests/decomposition.spec.js` exists — read in the directories
- OBSERVED: `PLAN-137` (parked) declares `EpicDecompositionView.svelte` and `DecompositionReviewView.svelte` in its surface for the chat; `PLAN-53` (parked) declares `StartView.svelte`, `SourceList`, `SourceIntakePanel`, `RecipeSelector`, `GitSourceIntakeModal`, `LessonSourceModal` and none of `EpicAdoptView`, `RecipeReadyView`, `DecompositionReviewView` — read by `corpus surfaces`
- HYPOTHESIS: the check marks `✓ ✗ ○ ●` are literal glyphs in the markup today — confirm/refute at `EpicDecompositionView.svelte` § plan row markup (verify-at-outline)
- HYPOTHESIS: `Compose` (PLAN-147) fits the instruct fields without the chat's additions (its `Enter` sends the instruction) — confirm/refute at `components-fields.adoc` § `#compose` and the fields' current submit key (verify-at-outline)
- Verify-first clause: re-read the four views after PLAN-45, PLAN-149 to PLAN-151 landed; take the PLAN-151 lint list as the work list. If PLAN-53 re-grounded and now claims `EpicAdoptView` or `RecipeReadyView`, drop it from deliverable 3.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/views/EpicDecompositionView.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/EpicDecompositionView.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/DecompositionReviewView.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/DecompositionReviewView.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/EpicAdoptView.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/EpicAdoptView.test.ts` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/views/RecipeReadyView.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/RecipeReadyView.test.ts` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-e2e/tests/decomposition.spec.js` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/theme-readability.spec.js` — present at HEAD 1cc0e10e (read)
- OBSERVED: `pm-api/src/main/resources/messages/decomposition.en.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/decomposition.de.properties` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/shared/starting-work.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/StartView.svelte` — present at `4a84f727` (PLAN-158 residue, folded 2026-11-11)
- OBSERVED: `pm-web-app/src/lib/views/StartView.test.ts` — present at `4a84f727` (PLAN-158 residue, folded 2026-11-11)
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` — present at `4a84f727` (PLAN-158 residue, folded 2026-11-11)
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/DecompositionScenarioTest.java` — present at `4a84f727` (PLAN-158 residue, folded 2026-11-11)
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/ControlScenarioTest.java` — present at `4a84f727` (PLAN-158 residue, folded 2026-11-11)

## Dependencies and Sequencing

- Depends on: `PLAN-45` landed; `PLAN-149` (`Dialog`, hierarchy), `PLAN-150` (`Compose` via PLAN-147, `TextArea`, `Field`, `SelectField`), `PLAN-151` (the count).
- Overlaps with:
  - `PLAN-137` (parked): `EpicDecompositionView`, `DecompositionReviewView`. This plan lands FIRST; PLAN-137 is re-grounded on it.
  - `PLAN-53` (parked): `StartView.svelte` renders three of the four views; its footer and steps are untouched here.
  - `PLAN-45` (running): `views/`.
- Adjacent to: `RecipeSelector` (PLAN-53); `WizardPage` (PLAN-137, 53); the chat; D2.
- Unblocks: PLAN-137 (decomposition adoption of the chat on restyled views); the gate "views package implemented" of PLAN-29, 41 and 65.

## Re-Grounding at Restart

Re-read every file above at the then-current HEAD. Run `corpus surfaces` against what landed since. Settle each HYPOTHESIS. Re-run the lint for the four views and re-check which plan claims `EpicAdoptView` and `RecipeReadyView`.

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-155-web-views-decomposition-and-start-steps.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message.
The orchestrator owns every other ledger write (`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary).
