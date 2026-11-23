# PLAN-178: Web: Focus, Forms and the Designer's Dialogs Are Correct (Residue of PLAN-153, 88 and 173)

epic: ui-improvements
workstream: WS-03

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Staged on the operator's word of
> 2026-11-23 (decision 2, the rest, proposal 1): the layer-3 major of the PLAN-153 landing (#532), the layer-2 major of the
> PLAN-173 landing (#525), the layer-2 and layer-3 findings of the PLAN-88 landing (#528) and the comparison-failure wording
> PLAN-154 deferred. This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief. **It starts
> after PLAN-144 and PLAN-176 have landed** (PLAN-176 edits `Dialog.svelte` and the modal tests, PLAN-144 the Back of `CleanupView`).

## Objective

Three landed plans left correctness gaps that no test pins. After the system view's upgrade consent dialog closes, the focus drops to the page body because the control that opened it is unmounted. The form module builds its first-invalid selector without `CSS.escape`, so a field key like `2fa` or `a.b` makes `submit()` throw with no error shown and no focus move. The process designer reopens a place dialog the person dismissed with Escape, deletes a unit without the confirmation its design promises, and says "leave the designer" in a place where nothing can be left. This plan fixes each and adds the tests that fail when the production line is reverted.

## Deliverables

The documents are the specification; read them first: `doc/design/web-app/components.adoc` (the dialog and the focus rule), `system.adoc`, `process-designer.adoc` and `forms.adoc` of `doc/design/web-app/`, and `landings/PLAN-153.md`, `PLAN-88.md` and `PLAN-173.md` for every finding with its file and line.

1. **Focus returns after the upgrade consent, and the system and cleanup views read right**
 - What:
   - `openUpgradeConsent` sets `upgradePhase = 'consent'` (`SystemView.svelte:288`), which makes `runtimeState` `upgrading` and unmounts the ActionBar and the "update available" panel (`:407-418,752,950-960`); `Dialog.takeFocus` recorded the opener (`Dialog.svelte:50-62`) and focuses it at destroy, but a removed element takes no focus, so after Escape or "Not now" the focus is on the body. Give the focus a place to return to: keep the opener mounted while the consent is open, or hand the dialog a `returnFocusElement` that stays mounted (the overflow menu's trigger, the page heading); the spec of PLAN-153 D2 said "Escape closes and returns the focus", and holds only for stop and restart.
   - `DataTable`'s scroll region is a bare `tabindex="0"` div with no role or name (`DataTable.svelte:64-70`, behind a `svelte-ignore a11y_no_noninteractive_tabindex`): give it `role="region"` with an accessible name; its tab stop is deliberate (the comment cites WCAG 2.1.1 and axe's `scrollable-region-focusable`), so keep it reachable by keyboard and take it only when the region scrolls, if the axe rule allows.
   - The cleanup's success notice passes the English literal `` `${checkedItems.length} items` `` as `{what}` (`CleanupView.svelte:184`), so the German view reads "3 items gelöscht"; use the catalogue phrases the Delete label uses (`deleteWhat`, `:360`), and read `checkedItems` before the `await`, as the `modalWhen` fix does.
   - The tests PLAN-153 missed: the `modalWhen` fix, the back `Link` to `/system` (the route hand-over `navigateTo('system')` gave), the runtime destructive roles of `byState.ts`, and the focus clause for the upgrade dialog.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `SystemView.test.ts`: Escape and "Not now" on the upgrade consent put the focus on an element that is mounted (asserted by `document.activeElement`); `DataTable.test.ts` the region's role and name; `CleanupView.test.ts` the German success notice and the back link; `byState.test.ts` the runtime roles; each fails with its production line reverted. Evidence order: `test-first`.
2. **The designer's dialogs behave**
 - What:
   - `compareForPlacement` awaits a fetch with `isPlacing` false and no loading state, then sets `placeStage` to `'diff'` or `'failed'` unconditionally (`ProcessDesigner.svelte:816-832`): a place dialog dismissed with Escape during the compare reopens (PLAN-88 L3-1). Guard the stage by the dialog's own open state.
   - A comparison that fails after an earlier unconfirmed placement shows `designer.place.failed` ("The landing failed: {reason}. The unit is not in force."), which misleads (PLAN-154's deferred finding, CodeRabbit thread `PRRT_kwDOVBOtss66qgXv`; `ProcessDesigner.svelte:823-828`); today the catch sets the unrelated `system.figure.unavailable`. Add a comparison-failure key in `designer.{en,de}.properties` and replace the old wording the test pins (`ProcessDesigner.test.ts`, `/The landing failed/`).
   - The leave dialog says "Leave the designer?" and "…and leave the designer" though the designer is never left (`designer.en.properties:25,33`; the e2e test pins the old words at `designer.spec.js:204`): say what happens.
   - `removeUnit` deletes the unit's copy at once (`ProcessDesigner.svelte:929-932`) while `designer.remove.title` and `designer.remove.confirm` exist unused and the design promises a confirmation: use `ConfirmDialog` with them. The leave dialog for a discarded draft uses the dialog's danger and consequences (`:1384-1386`).
   - The place dialog's width override `:global([data-testid="place-dialog"])` (`:1962-1964`) loses the cascade to `Dialog.svelte:195` (specificity (0,1,0) against (0,2,0)); the diff dialog stays 560 px and an own width is outside the dialog rule: let the `Dialog` take a size, or drop the override and the claim. Two controls share the accessible name `designer.open.question` (`:993`, `:1040`); the icon `Field` has no id and the file Field's hint is not linked to its raw input (`:1242-1243,1054`); the progress stage renders an empty footer row (`:1499`).
   - The tests PLAN-88 lacked: the `closePlace` guard (`:921`), Escape on the two dialogs and the intake keyboard path have no test that fails without them; the e2e "jumps to the line of a validation finding" asserts `line > 0` (`designer.spec.js:176-198`) and cannot fail: assert the actual line; the design document drifts from the view (`process-designer.adoc:112,267`, the mockups): bring it to the catalogue strings.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-documents:ref-asciidoc
 - Done when:
   - `ProcessDesigner.test.ts`: an Escape during the compare keeps the dialog closed; a failed compare shows the new wording in both languages; remove asks first and deletes only on confirm; the `closePlace` guard, Escape and the intake keys are covered; each fails with its production line reverted. `designer.spec.js` jumps to the line it names. Evidence order: `test-first`.
   - The design document names the catalogue strings and no mockup shows a control the view does not render (a search of the mockups against the catalogue). Evidence order: `none`.
3. **A field key never breaks the submit**
 - What:
   - The form module builds the first-invalid selector without `CSS.escape` (`forms/index.ts:320-322`: ``[name="${firstInvalid}"], #${firstInvalid}, [data-testid="${firstInvalid}"]``): a key like `2fa`, `a.b` or one with a quote makes `querySelector` throw inside `submit()` before `publish()`, so no error shows, the focus does not move and the rejection is unhandled (`DemoFormFixture.svelte:65`) (PLAN-173 D4(b) and L3-1). The `RadioGroup` half is fixed. Escape every part (`CSS.escape` for the id and the attribute values) and say in the pull request that the case reproduces at `3ae8a305`.
   - `NumberField.commit()` clamps and calls `onchange` but fires no `input` (`:170-180`), so an `oninput`-wired form keeps the unclamped value and Enter submits it (PLAN-173 L3-2).
   - The layer-2 minors of PLAN-173: a rejected `onstore` is caught at `SecretField.svelte:143`, which restores the typed value; whether the person is told why is read at outline and fixed if not; the order test never moves focus by Tab (`DemoFormFixture.test.ts:50`); the checkbox-Enter test passes at the base (`CheckField.test.ts:37`); the outline regex of `Field.test.ts:57` has no role-derived guard; `super-secret-token` is still in `SecretField.test.ts:149-160`; the accepted `plan.activity.kind.egress` key is read by nothing (remove it or name its reader).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - `forms.test.ts` is fed keys `2fa`, `a.b` and `a"b` and shows the first invalid field focused and the error shown; `NumberField.test.ts` shows the clamped value reaching an `oninput` consumer; `SecretField.test.ts` shows the rejected store's message; each fails with its production line reverted. Evidence order: `test-first`.
4. **Browser evidence**
 - What: the system upgrade consent (Escape, "Not now", focus on a visible element), the designer's place dialog (Escape during the compare), remove with its confirmation and the leave dialog, and a form with a key that needs escaping on the dev gallery, in `en` and `de`, light and dark, at `1440 × 900` and `390 × 844`, after the last visual commit.
 - Persona: plan-marshall:persona-integration-tester
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - `system.spec.js` and `designer.spec.js` carry the focus and Escape cases, pass with `PM_MOCK_WEB_PORT` and `PM_WEB_PORT` set and wait on the address and the DOM; the full `-Pe2e` run at the head is green and cited. Evidence order: `test-first`.
   - Browser check (Playwright MCP) on the targets above. Evidence order: `none`.

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`): `./mvnw -pl pm-api,pm-web-app verify`, `./mvnw -pl pm-api,pm-web-app verify -Ppre-commit` (review every rewrite), `PM_MOCK_WEB_PORT=<mock port> PM_WEB_PORT=<app port> ./mvnw -pl pm-web-e2e verify -Pe2e` at the head, and `-Pcoverage` at the head after the last code commit (minimum 80 % branch for `pm-web-app`).
- No new dependency or plugin. Web standards of `CLAUDE.md`; no static `style` attribute; every new text from the catalogue in English and German; the text guard baseline does not grow. Pre-1.0: no shim.
- Every new or rewritten assertion is shown to fail with the production line reverted, and the failing run is cited; a `test-first` clause cites a failing run at the plan's start, and where the base-sha instrument is unavailable, say so and do not relabel the clause.
- A finding that needs a product decision (the design's wording for the leave dialog, a size prop of `Dialog`) is reported in the pull request and the landing message, never decided silently.
- Ports of this plan: mock web port 21780, app port 21781 (`20000 + 10 × 178`) (`AGENTS.md` § Ports). Other plans run at the same time: never stop or reuse a process this worktree did not start.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.
- The merge rule: the run merges only when every review thread has a reply and is resolved and every review body is answered, and some head has had one CodeRabbit review. The pull request body states thread count, resolved count and review-body items from `ci pr comments`, never from memory. The run merges the pull request itself once these gates hold (`CLAUDE.md`, Git Workflow). Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`; the pull request body carries no generator footer. If CodeRabbit refuses on quota, stop and ask the operator.
- ZERO Sonar findings (the operator, 2026-11-05): the pull request merges with 0 open new-code issues, every open issue fixed in a commit of THIS pull request; state `sonar_open_new_issues=N` in the landing message (`unknown` for a failed read, never `0`). A dismissal needs the operator's word.

## Claim Labels

- OBSERVED: `openUpgradeConsent` sets `upgradePhase = 'consent'`, which makes `runtimeState` `upgrading` and unmounts the ActionBar and the update panel — read at `pm-web-app/src/lib/views/SystemView.svelte:288,407-418,752,950-960`
  - verdict: corroborated | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: SystemView.svelte:288 upgradePhase = 'consent'; :407-414 runtimeState is 'upgrading' for any phase but idle; the unmounting of the ActionBar and panel is the landing's read (:752,950-960) (origin/main 3ae8a305)
- OBSERVED: `Dialog.takeFocus` records `document.activeElement` at mount and focuses it at destroy — read at `pm-web-app/src/lib/components/Dialog.svelte:50-62`
  - verdict: corroborated | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: Dialog.svelte:54-56 documents that the dialog takes the focus when it opens and gives it back to where it came from; the activeElement record is :52 per the landing (origin/main 3ae8a305)
- OBSERVED: the `DataTable` scroll region is a `div` with `tabindex="0"` and no role or name — read at `pm-web-app/src/lib/components/DataTable.svelte:64-70`
  - verdict: corroborated | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: DataTable.svelte:64-68 div.data-table__scroll tabindex=0 with no role or name, kept by a comment citing WCAG 2.1.1 (origin/main 3ae8a305)
- OBSERVED: the cleanup's success notice passes an English `items` literal — read at `pm-web-app/src/lib/views/CleanupView.svelte:184`
  - verdict: corroborated | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: CleanupView.svelte:184 what: `${checkedItems.length} items` (origin/main 3ae8a305)
- OBSERVED: `compareForPlacement` sets `placeStage` unconditionally after its await — read at `pm-web-app/src/lib/views/ProcessDesigner.svelte:816-832`
  - verdict: corroborated | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: ProcessDesigner.svelte:816-832 compareForPlacement sets placeStage 'failed' or 'diff' after its await with no open-state check (origin/main 3ae8a305)
- OBSERVED: `removeUnit` deletes without a confirmation — read at `pm-web-app/src/lib/views/ProcessDesigner.svelte:929-932`
  - verdict: corroborated | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: ProcessDesigner.svelte:930-933 removeUnit clears the notice and calls apiClient.removeWorkflowUnit with no confirmation in the function; a confirming caller was not found (origin/main 3ae8a305)
- OBSERVED: the first-invalid selector is built without `CSS.escape` — read at `pm-web-app/src/lib/forms/index.ts:320-322`
  - verdict: corroborated | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: forms/index.ts:320-322 document.querySelector(`[name="${firstInvalid}"], #${firstInvalid}, [data-testid="${firstInvalid}"]`) with no CSS.escape (origin/main 3ae8a305)
- OBSERVED: `NumberField.commit()` fires no `input` event — read at `pm-web-app/src/lib/components/NumberField.svelte:170-180`
  - verdict: corroborated | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: NumberField.svelte:170-179 commit clamps, sets input.value, setOwn and onchange; no input event is dispatched (origin/main 3ae8a305)
- HYPOTHESIS: `SecretField` shows no message for a rejected `onstore` (the catch restores the typed value) — confirm/refute at `pm-web-app/src/lib/components/SecretField.svelte:140-155` (verify-at-outline)
  - verdict: unverifiable | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: SecretField.svelte:140-146 the catch restores the typed value; whether a message is shown is read at outline
- HYPOTHESIS: the place dialog stays 560 px by the cascade, `(0,2,0)` of `Dialog.svelte:195` over `(0,1,0)` of the override — confirm/refute at `pm-web-app/src/lib/views/ProcessDesigner.svelte:1962-1964` and `pm-web-app/src/lib/components/Dialog.svelte:195` (verify-at-outline)
  - verdict: unverifiable | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: the cascade (0,2,0) over (0,1,0) is the landing's reading by specificity, not run; settled at outline
- Verify-first clause: before each deliverable re-read the named files at the then-current HEAD; PLAN-176 (`Dialog.svelte`, the modal tests), PLAN-144 (`CleanupView`'s Back) and PLAN-156 (`Dialog`) edit the same files. A refuted claim re-scopes the deliverable and is reported, never silently worked around.
  - verdict: unverifiable | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: procedural verify-first clause; PLAN-144 running, PLAN-176 staged

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/views/SystemView.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/views/SystemView.test.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/views/CleanupView.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/views/CleanupView.test.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/components/DataTable.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/components/DataTable.test.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/components/Dialog.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/actions/byState.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/actions/byState.test.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/views/ProcessDesigner.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/views/ProcessDesigner.test.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/components/WorkflowValidationResults.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/forms/index.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/forms/forms.test.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/components/NumberField.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/components/NumberField.test.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/components/SecretField.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/components/SecretField.test.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/components/CheckField.test.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/components/Field.test.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/dev/DemoFormFixture.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/dev/DemoFormFixture.test.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-api/src/main/resources/messages/designer.en.properties` — present at HEAD 3ae8a305
- OBSERVED: `pm-api/src/main/resources/messages/designer.de.properties` — present at HEAD 3ae8a305
- OBSERVED: `pm-api/src/main/resources/messages/system.en.properties` — present at HEAD 3ae8a305
- OBSERVED: `pm-api/src/main/resources/messages/system.de.properties` — present at HEAD 3ae8a305
- OBSERVED: `doc/design/web-app/process-designer.adoc` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-e2e/tests/system.spec.js` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-e2e/tests/designer.spec.js` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-e2e/tests/theme-readability.spec.js` — present at HEAD 3ae8a305

## Dependencies and Sequencing

- Depends on: `PLAN-153`, `PLAN-88`, `PLAN-173`, `PLAN-154` (shipped); `PLAN-144` and `PLAN-176` (landed first: they edit `CleanupView`, `Dialog.svelte` and the modal tests).
- Overlaps with: `PLAN-177` (staged: independent surface, no shared file besides `client.ts`-free tests), `PLAN-62`, `PLAN-55` (parked: `PlanView`; none shared).
- Adjacent to: PLAN-175's Sonar scope (the smells of `pm-api` and `pm-operator`, not touched here).

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-178-web-dialog-form-and-designer-correctness.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger write. The session reports its outcome through its PR and its inbox message (`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary).
