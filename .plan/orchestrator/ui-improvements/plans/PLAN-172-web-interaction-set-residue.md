# PLAN-172: Web Foundation: The Interaction Set's Residue (PLAN-149 D0, Dropped by PLAN-150)

epic: ui-improvements
workstream: WS-02

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Staged on the operator's word of 2026-11-19 ("do 1", the PLAN-150 residue) from the operator's decision of 2026-11-18 (handle-open: two plans in series, this D0 plan first, then the field fix `PLAN-173`), `landings/PLAN-149.md` and `landings/PLAN-150.md`. PLAN-150 landed (#452, `a692def2`) **without** its D0, which its spec folded in from the PLAN-149 landing on 2026-11-15: this plan is that D0 as a plan of its own. This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

The interaction components that every field and view builds on have six defects that no plan owns: a disabled-with-reason `Button` or `IconButton` still activates through `on:click`, the `loading` state draws spinner and icon together, the consequences of a danger `ConfirmDialog` are not in the danger colour, `Dialog` takes Escape in the capture phase so a menu inside a dialog never sees it, the `ConfirmDialog` is rendered inside the row menu's DOM, and `StatusLabel` carries an invented width literal. `Toggle` also lacks the disabled reason the field system needs. This plan fixes them first, each with a test that fails before the fix, so the field fixes of `PLAN-173` and the views packages (`PLAN-88`, `PLAN-153`, `PLAN-155`, `PLAN-156`, `PLAN-157`) build on components that do what the design says.

## Deliverables

Read first:
- The D0 text of `plans/PLAN-150-web-foundation-field-system.md` ("Folded from the PLAN-149 landing 2026-11-15") and its "Not in D0" paragraph; `landings/PLAN-149.md` and `landings/PLAN-150.md` (L3-6).
- `doc/design/web-app/components.adoc` (`#confirm-dialog`, `#layers`, the IconButton paragraph at `:163`), `components-fields.adoc`.
- Every position below was re-read at `origin/main` `5f7074b7` (2026-11-19) by the orchestrator; a refuted claim re-scopes the deliverable and is reported.

1. **A disabled-with-reason `Button` or `IconButton` does not activate**
   - What: `Button.svelte:66-72` registers `on:click={handleClick}` and a forwarding `on:click` on one element; `stopPropagation()` does not stop the second listener on the same element, so only `onclick` rest-prop consumers are blocked. `ActionBar.svelte:81,94` call `chooseAct` through `on:click`, so a primary or secondary act that carries a disabled reason can still reach `onAct`; `IconButton.svelte:54-59` has the same shape and `DataTable` uses `on:click` on it (L3-6 of PLAN-150). Activation is blocked at the source for both components, for `on:click` and `onclick` consumers alike (the old tests use `onclick` and never exercise the forwarded path).
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
   - Done when:
     - A test fails at `5f7074b7` and passes: a click on a disabled-with-reason `ActionBar` primary and on a disabled `IconButton`, through `on:click`, calls nothing (evidence order: `test-first`).
     - A test: a click through `onclick` on the same two calls nothing, and an enabled one still activates (evidence order: `test-first`).
     - The first test is run once with the fix reverted and fails (evidence order: `mutation-after`).
2. **`loading` replaces the icon, and `Toggle` has a disabled reason**
   - What: `Button.svelte:74-79` renders the spinner and the icon together when `loading` and `icon` are both set. With `loading`, only the spinner shows. `Toggle.svelte` has `disabled` but no `disabledReason` (re-read: 0 occurrences); `SecretField` (the field plan) needs a `Toggle` that can say why it is disabled: add `disabledReason` with the same activation rule and the same advisory as `Button` (deliverable 1).
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
   - Done when:
     - A test fails at `5f7074b7` and passes: an `icon` button with `loading` shows the spinner and no icon (evidence order: `test-first`).
     - A test fails at `5f7074b7` and passes: a `Toggle` with `disabled` and a `disabledReason` shows the reason as its advisory and does not call `onToggle` through `on:click` or `onclick` (evidence order: `test-first`).
3. **A danger confirmation's consequences are in the danger colour, and Escape closes the inner thing first**
   - What: (a) the design (`components.adoc#confirm-dialog`) puts the consequences of a danger `ConfirmDialog` in the danger colour; `Dialog.svelte:199-210` colours the title only and `.dialog__consequences` is `--color-text-secondary`. The test asserts the colour token, or the design is changed and the landing message says which (the lane decides at outline, with the design text open). (b) `Dialog.svelte:119-123` listens on the window in the capture phase and stops propagation, so an Escape inside a menu that lives in a dialog closes the dialog and never reaches the menu, where the design closes the menu first. The lane decides the order, implements it and tests it; the `defaultPrevented` rule of the two templates (`TriagePage`, `WizardPage`) is part of the decision.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
   - Done when:
     - A test fails at `5f7074b7` and passes: a danger `ConfirmDialog` renders its consequences in the danger colour token (or the design line says otherwise and the test pins that; evidence order: `test-first`).
     - A test fails at `5f7074b7` and passes: an Escape pressed inside an open menu that lives in a dialog closes the menu and leaves the dialog open; a second Escape closes the dialog (evidence order: `test-first`).
     - The `TriagePage` and `WizardPage` tests that use `defaultPrevented` still pass, or are changed with the decision stated (evidence order: `none`).
4. **The `ConfirmDialog` leaves the menu's DOM**
   - What: `ActMenu.svelte` renders `ConfirmDialog` inside `.row-menu` (`:240`; the menu list is at `:185`): a click inside the dialog bubbles through the row, `position: fixed` can be trapped by a transformed ancestor, and its `Dialog.takeFocus` records the menu item as the return target. It is rendered outside the menu. After confirm and after cancel the focus returns to a sensible control (the row's menu button or the control that opened it). The confirmation's title and consequences are both the entry label today; they get their own texts in `en` and `de`, or the landing message says why not.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
   - Done when:
     - A test fails at `5f7074b7` and passes: the open confirmation is not a descendant of the row menu, and a click inside it does not reach the row's click handler (evidence order: `test-first`).
     - A test: after confirm and after cancel the focus is on the control the design names (evidence order: `test-first`).
     - The `en` and `de` texts for the confirmation's title and consequences exist, or the reason is stated (evidence order: `none`).
5. **The `StatusLabel` width is a token, and the stale design references are corrected**
   - What: `StatusLabel.svelte:12,43,49` carries the literal `8.5rem` for the fixed state slot, a value no token holds (PLAN-149 landing). **The operator decides the token: its name and its value; the lane asks at outline with the existing width tokens listed**, then replaces the literal. `doc/design/web-app/components.adoc:163` still says the `IconButton` tests live in `Button.test.ts` (they live in `IconButton.test.ts`); `components.adoc#layers` lists nothing for `SideColumn` (60) or the gap-strip popover while saying no component writes a number of its own: both lines are corrected.
   - Persona: plan-marshall:persona-documenter
   - Skills: pm-dev-frontend:css, pm-documents:ref-asciidoc, pm-documents:ref-documentation
   - Done when:
     - `git grep -n "8.5rem" -- pm-web-app/src` names no line in `StatusLabel.svelte` and the token the operator chose exists in `tokens.css` with a test that pins it (evidence order: `none` for the grep; `test-first` for the token test).
     - A reader of `components.adoc` finds no line the code contradicts at `:163` and in `#layers`, checked by the lane (evidence order: `none`).

Not in this plan, named so they are not lost (each stays in `epic.md` as an Open Defect or Watch, owners named there): the field defects of PLAN-150 (`PLAN-173`, the next plan in this series); the duplicated `.act-menu*` and `.row-menu__*` CSS and the legacy class names `RowMenu.test.ts` greps; the allowlist's stale-entry check and the reason "the V parts" for `SideColumn`; the global `testTimeout: 20_000` of `vite.config.ts`; the dev page's English literals; the five-option German `SegmentedControl` case.

## Standing Constraints

- ZERO Sonar findings (the operator, 2026-11-05): 0 open new-code issues and 0 hotspots, read at the pull request head after the push (`unknown` for a failed read, never `0`); a dismissal is made only on the operator's selection. This is a merge gate.
- Build and test through Maven only (`CLAUDE.md`): `./mvnw verify -pl pm-web-app -am` and, because components change, `./mvnw verify -Pe2e -pl pm-web-e2e -am` pass at the head; `-Pcoverage` runs as its own invocation and keeps the 80 % floor (the module's branch margin is 0.14 pt: the new code is covered, not just reached).
- No new dependency or plugin. ES modules only; no `innerHTML`, `outerHTML`, `insertAdjacentHTML` or `{@html}`; no arbitrary timer wait in a test (`CLAUDE.md` Web Standards).
- Components use the design system's tokens only: no raw colour, no raw size (the operator's token decision of deliverable 5 is the one exception that creates a token); text from the catalogue in `en` and `de`; the vocabulary lint (`npm run lint:vocabulary`) stays at its baseline and may only shrink.
- Every fix has a test that fails before it (`test-first`); the order of each Done-when is the order the evidence has: a `none` Done-when is never described as failing, and "fail-first" is used only for `test-first` and `mutation-after` rows.
- Browser check of the changed components with the dev page (light and dark, en and de, two sizes), the screenshots read; the landing message states what each shows.
- **The merge rule:**
  - At least one CodeRabbit review of some head, with its findings handled. If CodeRabbit refuses on quota, stop and ask the operator.
  - Every review thread replied and resolved, and every review body item (outside-diff, nitpick, additional comments) answered on the pull request, read from GitHub's thread state, never from a count kept in memory. Every reply answers its own finding.
  - The run merges the pull request itself once these gates hold (`CLAUDE.md`, Git Workflow). Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`. The pull request body carries no generator footer.
- The landing verdict is given per Done-when clause, never per deliverable, and the landing message reports `deliverables_done` as the number of deliverables whose every Done-when clause is met; a number the lane did not recount is `unknown`; a changed printed or visible format is named in the landing message.
- Pull request size: about 20 files; if the changed set passes 100 files, stop and ask the operator to split.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.
- The mailbox read refuses a plan id with a leading digit (`invalid_target_plan`): report it, never read it as no mail.

## Claim Labels

- OBSERVED: `Button.svelte` registers `on:click={handleClick}` and a forwarding `on:click` on one element — read at `pm-web-app/src/lib/components/Button.svelte:66-72` at `5f7074b7`; `IconButton.svelte:54-59` has the same two listeners
- OBSERVED: `ActionBar.svelte:81,94` pass `chooseAct(...)` through `on:click` for the primary and each secondary act, with `disabledReason` set — read at `5f7074b7`
- OBSERVED: `Button.svelte:74-79` renders `{#if loading}` the spinner and `{#if icon}` the icon in sequence, not exclusively
- OBSERVED: `Toggle.svelte` has the props `pressed`, `label`, `icon`, `pressedIcon`, `disabled`, `onToggle` and no `disabledReason` — read at `Toggle.svelte:11-16` (0 occurrences of `disabledReason`)
- OBSERVED: `.dialog--danger .dialog__title` is `var(--color-danger)` and `.dialog__consequences` is `var(--color-text-secondary)` — read at `Dialog.svelte:199-210`; the capture listener is `window.addEventListener('keydown', onKeydown, true)` at `:119-123`
- OBSERVED: `ActMenu.svelte` imports `ConfirmDialog` (`:16`) and renders it at `:240`, after the menu list at `:185` inside `.row-menu`
- OBSERVED: `StatusLabel.svelte` carries `inline-size: 8.5rem` at `:49` (and the docs at `:12,43`); `components.adoc:163` says the `IconButton` tests live in `Button.test.ts`
- HYPOTHESIS: the danger colour of the consequences is what the design says — read at `components.adoc#confirm-dialog` by the PLAN-149 landing, not re-read here (verify-at-outline; if the design says otherwise the deliverable changes the design)
- HYPOTHESIS: the ConfirmDialog sits inside `.row-menu` in the DOM (the `{#if}` at `ActMenu.svelte:240` may render it inside a portal) — confirm/refute at `ActMenu.svelte:180-260` (verify-at-outline)
- HYPOTHESIS: no token holds the width of `8.5rem` — confirm/refute against `tokens.css` (verify-at-outline; the operator decides the token either way)
- Verify-first clause: before each deliverable, re-read the named lines at the then-current HEAD; `PLAN-168`, `PLAN-169` and `PLAN-171` run elsewhere (Java and views), see Dependencies. A refuted claim re-scopes the deliverable and is reported.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/components/Button.svelte`
- OBSERVED: `pm-web-app/src/lib/components/Button.test.ts`
- OBSERVED: `pm-web-app/src/lib/components/IconButton.svelte`
- OBSERVED: `pm-web-app/src/lib/components/IconButton.test.ts`
- OBSERVED: `pm-web-app/src/lib/components/ActionBar.svelte`
- OBSERVED: `pm-web-app/src/lib/components/ActionBar.test.ts`
- OBSERVED: `pm-web-app/src/lib/components/ActMenu.svelte`
- OBSERVED: `pm-web-app/src/lib/components/ActMenu.test.ts`
- OBSERVED: `pm-web-app/src/lib/components/Dialog.svelte`
- OBSERVED: `pm-web-app/src/lib/components/Dialog.test.ts`
- OBSERVED: `pm-web-app/src/lib/components/ConfirmDialog.svelte`
- OBSERVED: `pm-web-app/src/lib/components/ConfirmDialog.test.ts`
- OBSERVED: `pm-web-app/src/lib/components/Toggle.svelte`
- OBSERVED: `pm-web-app/src/lib/components/Toggle.test.ts`
- OBSERVED: `pm-web-app/src/lib/components/StatusLabel.svelte`
- OBSERVED: `pm-web-app/src/styles/tokens.css`
- OBSERVED: `doc/design/web-app/components.adoc`
- HYPOTHESIS: `pm-web-app/src/lib/styles/tokens.test.ts` (the test that pins the token; the path is read at outline)
- HYPOTHESIS: `pm-api/src/main/resources/messages/common.en.properties` and `common.de.properties` (the confirmation's own title and consequences texts; verify-at-outline which pack)
- HYPOTHESIS: `pm-web-app/src/lib/components/StatusLabel.test.ts`

## Dependencies and Sequencing

- Depends on: `PLAN-149` (shipped, #445) and `PLAN-150` (shipped, #452).
- Overlaps with (read by `corpus cross-check` at staging, 2026-11-19; shipped, parked and superseded plans are not in the disjointness population): **no running plan** (PLAN-168, 169 and 171 are Java and views of other components), so it is emittable. Staged: `PLAN-173` (`tokens.css`, `tokens.test.ts`, `common.en.properties`; it follows this plan, so no clash), `PLAN-135` (`components.adoc`, `common.en.properties`), `PLAN-136` (`tokens.css`), `PLAN-144` (`common.en.properties`): whichever lands second rebases and keeps both sides.
- Adjacent to: **`PLAN-173` (the field fixes) follows this plan**: `SecretField` needs `Toggle`'s `disabledReason` (deliverable 2) and every field that uses `Button` inherits deliverable 1; it is emitted after this plan has landed. The views packages that build on the field system (`PLAN-88`, `PLAN-153`, `PLAN-155`, `PLAN-156`, `PLAN-157`) wait for `PLAN-173`.

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-172-web-interaction-set-residue.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports its outcome through its PR and its inbox message. The inbox exception's qualifiers and the sole sanctioned write mechanism are stated in `persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
