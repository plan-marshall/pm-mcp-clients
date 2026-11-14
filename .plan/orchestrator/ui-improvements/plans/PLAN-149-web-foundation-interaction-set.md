# PLAN-149: Web Foundation: Layers, the One Act Menu, SegmentedControl, Toggle and Control Conformity (D1 Foundation, Part 3)

epic: ui-improvements
workstream: WS-02

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Drafted 2026-11-10 from the AGREED D1 documents. SELF-SUFFICIENT: the emitted command is a one-line pointer.

## Objective

D1 settled how an act is offered. This part delivers the interaction set: the layer tokens, one act menu for `RowMenu` and the `ActionBar` overflow (with `disabledReason` and keyboard access to disabled entries),
the new `SegmentedControl` and `Toggle`, and the conformity of `Button`, `IconButton`, `Dialog` and `ConfirmDialog` to the agreed rules (the legacy `small` size and the `visibleLabel` go; a disabled act with a reason
stays reachable by keyboard). The fields (PLAN-150) and the lint (PLAN-151) build on it.

## Deliverables

Read first: `doc/design/web-app/components.adoc` (§ `#controls`, `#button`, `#icon-button`, `#action-bar`, `#row-menu`, `#act-menu`, `#row-menu-placement`, `#layers`, `#segmented-control`, `#toggle`, `#dialog`,
`#confirm-dialog`, `#hierarchy`), `interaction-affordances.adoc`, `README.adoc` § `#control-tokens`, `shared/reports.adoc` § `#list`, and the pictures `web-component-{button,icon-button,action-bar,row-menu,segmented-control,toggle,dialog}.svg`.

1. **Layer tokens**
 - What:
   - `tokens.css`: `--layer-sticky` `1`, `--layer-bar` `20`, `--layer-menu` `30`, `--layer-toast` `90`, `--layer-dialog` `100`.
   - The components that already own a layer read the tokens: `DataTable.svelte:158` (sticky), `BottomBar.svelte:361` (bar), `RowMenu.svelte:249` and `ActionBar.svelte:274` (menu; the bar's `10` becomes the menu layer),
     `Toast.svelte:73` (toast), `Dialog.svelte:166` (dialog), `CommandPalette.svelte:515` (dialog).
   - A static test scans every `.svelte` and `.css` file for a numeric `z-index` and fails for any file not on a named allowlist. The allowlist holds exactly the modals and views that still draw their own layer
     and names the part or plan that removes each: `FindingsReviewModal`, `HoldResolutionModal`, `ConfinedFilePicker`, `GrantModal`, `LanListenerModal`, `StartWorkModal`, `EpicDecompositionView`, `SystemView`, `CleanupView` (the V parts);
     `PlanSettingsModal` (PLAN-55); `GitSourceIntakeModal`, `LessonSourceModal` (PLAN-53); `ProjectOnboardingView`, `ProjectExplorerView` (PLAN-59); `MachineSettingsView` (PLAN-60); `ProcessDesigner` (PLAN-88).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `tokens.test.ts` asserts the five values and the order (toast below dialog, menu above bar); the scan test fails when a component writes a number, and fails when an allowlisted file loses its reason. Evidence order: `test-first`.
   - A browser check of stacking: a menu above the bottom bar, a toast below a dialog (evidence order: `none`).
 - Implements: `components.adoc` § `#layers`; `README.adoc` § `#control-tokens`.
2. **One act menu**
 - What (`RowMenu` and `ActionBar` serve existing consumers; their fields and edges are enumerated, one criterion each):
   - Extract the menu both components draw today into one implementation (`ActMenu`): `ArrowDown` and `ArrowUp` wrap and include disabled entries; `Home`, `End`; `Escape` and `Tab` close and return the focus to the trigger;
     activation closes, returns the focus and runs the act; a destructive entry opens its `ConfirmDialog`; the focus ring is ≥ 3:1 in both themes; 44 px on a phone. A disabled entry keeps its reason as described-by text and, while it has the focus,
     shows the reason as a second line under its label. `RowMenu` takes `disabledReason` per entry (today `RowMenu.svelte:89-91,129` drops disabled entries).
   - Placement is unchanged (PLAN-129 owns the scroll guard in `RowMenu`; land after it).
   - `RowMenu` (consumers: `ReportsView.svelte` and the dev page) keeps `items` (`id`, `label`, `destructive`, `disabled`, `disabledReason`, `advisory`, `onclick`), `label`, `advisory`, `icon`, `size`, `disabled`. `ActionBar` (consumers: `WorkControlPanel.svelte`,
     `SettingsView.svelte`, `SystemView.svelte`, `routes/+layout.svelte`, the dev page) keeps `acts` (`id`, `label`, `role`, `destructive`, `disabled`, `disabledReason`, `advisory`), `busy`, `onAct`. Edges, each with one assertion: no items (no trigger is drawn);
     one entry; all entries disabled (the menu still opens and each reason is reachable); a disabled entry with and without a reason; a destructive entry; a menu taller than the room (capped, scrolls); opening upward; a disabled trigger; `busy` (every entry inert).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `RowMenu.test.ts`, `ActionBar.test.ts` and a new `ActMenu.test.ts` assert every listed key, entry state and edge; each fails on a revert of its line. Evidence order: `mutation-after`.
   - The Playwright run of `row-menu.spec.js` and of a keyboard walk over a menu with a disabled entry is cited from the pull request (Playwright is not run in CI). Evidence order: `none`.
 - Implements: `components.adoc` § `#row-menu`, `#act-menu`, `#action-bar`.
3. **`SegmentedControl` and `Toggle`**
 - What:
   - `SegmentedControl.svelte`: two to five options (`options` of `id`, `label`), `value`, `label`, `onChange(id)`. `role="radiogroup"` with `aria-checked`; the arrow keys move and wrap; the chosen option sits on `--color-brand-subtle`, never a solid brand fill; one tab stop.
   - `Toggle.svelte`: a `<button aria-pressed>` with `pressed`, `label` (names the thing, not the state), `icon`, `onToggle(pressed)`; the pressed state is drawn by the icon and the pressed ground, never by colour alone.
   - Both reach 24 px and 44 px on a phone. The hand-written switches of `OverviewView`, `DecisionView`, `PlanView` and `SystemView` are NOT moved here; their plans and PLAN-153 adopt the components.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `SegmentedControl.test.ts`: roles and `aria-checked`, wrapping arrows, `Home`/`End`, one tab stop, an option count outside 2..5 refused by the type. `Toggle.test.ts`: `aria-pressed`, icon change, label not state. Fail on a revert. Evidence order: `test-first`.
 - Implements: `components.adoc` § `#segmented-control`, `#toggle`.
4. **`Button`, `IconButton`, `Dialog`, `ConfirmDialog` conform**
 - What (`Button` has about 40 consumers; its fields and edges are enumerated, one criterion each):
   - `Button`: the legacy size `small` goes; its 19 call sites become `sm` (`CompletionReportView` 2, `DecisionView` 6, `ProjectExplorerView` 2, `ReportsView` 9). A disabled button with a `disabledReason` takes `aria-disabled` instead of the native `disabled`,
     stays in the tab order, shows the reason as its advisory and `aria-describedby`, and activating it does nothing; without a reason it keeps the native `disabled`. `loading` keeps the label, shows a spinner in place of the icon, and ignores a second click.
     Edges: each variant × each size; icon only with a hidden label; disabled with and without a reason; loading; `type="submit"` inside a form (a disabled-with-reason button does not submit).
   - `IconButton`: `visibleLabel` goes (callers `DataTable.svelte:121` and `ReportsView.svelte:1171`); the icon never shows its label; the accessible name is always its own `label`.
   - `shared/reports.adoc` § `#list` row-menu bullet ("with the word _Report actions_ beside the glyph from the tablet width up") is corrected in this pull request (operator decision D-4, assumed: `components.adoc` wins).
   - `Dialog`, `ConfirmDialog`: read the rules of `components.adoc` § `#dialog`, `#confirm-dialog` against `Dialog.test.ts` and `ConfirmDialog.test.ts` and fill every missing assertion: radius 12 px, `--shadow-overlay`, the layer token, the title at `--text-xl` as the accessible name,
     the focus held inside, the footer right-aligned with the ghost cancel on the left of the confirming act, `Escape` closes and returns the focus, `danger` shows the consequences in the danger colour and the one filled danger control, `busy` ignores a second confirm and holds `Escape` and cancel,
     `confirmDisabled`. Code changes only where a rule is unmet. `ConfirmDialog`'s typed-name input is PLAN-150's.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-documents:ref-asciidoc
 - Done when:
   - `Button.test.ts`, `IconButton` tests (in `Button.test.ts`), `Dialog.test.ts`, `ConfirmDialog.test.ts` assert every listed field and edge; each fails on a revert. Evidence order: `mutation-after`.
   - `git grep -nE 'size="small"|visibleLabel' -- pm-web-app/src` is empty. Evidence order: `none`.
 - Implements: `components.adoc` § `#button`, `#icon-button`, `#dialog`, `#confirm-dialog`, `#interaction-states`; `shared/reports.adoc` § `#list`.
5. **Fixtures and browser evidence**
 - What: fixtures of the act menu (open, with a disabled entry and its reason, upward), `SegmentedControl` (three options), `Toggle` (off, on), `Button` (every variant × size, disabled with reason, loading) and a stacked layers fixture, on the dev page in both themes and locales.
 - Persona: plan-marshall:persona-integration-tester
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - Browser check (Playwright MCP) of `/dev/components` at 1440×900 and 390×844, `de` and `en`, both themes; screenshots kept in the plan's working area, re-taken after any later visual fix (the pull request lists them after the push; not a gate before it) (evidence order: `none`).

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`): `./mvnw -pl pm-web-app verify`, `-Ppre-commit` (review every rewrite), and at the head `PM_MOCK_WEB_PORT=<mock port> PM_WEB_PORT=<app port> ./mvnw -pl pm-web-e2e verify -Pe2e`.
- No new dependency or plugin. Web standards of `CLAUDE.md`: ES modules, flat ESLint, no `innerHTML`/`outerHTML`/`insertAdjacentHTML`/`{@html}` with runtime or model text, no timer waits in tests, the npm scripts stay; no static `style` attribute (`lib/csp.test.ts`).
- Every component obeys `components.adoc` § `#rules` and § `#new-component`; every new text comes from the catalogue in English and German; the text guard baseline does not grow.
- The design documents are updated in the same pull request wherever a deliverable says so.
- Ports of this plan: mock web port 21490, app port 21491 (`20000 + 10 × 149`) (`AGENTS.md` § Ports). Never stop or reuse a process this worktree did not start.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.
- Every `test-first` clause cites a FAILING and a PASSING run; every `mutation-after` clause cites the reverted code and the failing assertion from a log quoted in the pull request body.
- **The merge rule:**
  - At least one CodeRabbit review of some head with its findings handled; every review BODY answered where the reviewer reads it; every thread replied and resolved. If CodeRabbit refuses on quota, stop and ask the operator.
  - The pull request body states `unresolved_review_threads=N` and `review_body_findings_total/handled`, read through `ci pr comments`.
  - The run merges the pull request itself once these gates hold, without a further word of the operator (`CLAUDE.md`, Git Workflow, #423). Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`. The pull request body carries no generator footer.
- ZERO Sonar findings (the operator, 2026-11-05): 0 open new-code issues, every open issue fixed in a commit of THIS pull request; state `sonar_open_new_issues=N` in the landing message.
  A dismissal is made only on the operator's selection through `sonar_rest transition`; the lane never records one of its own.

## Claim Labels

- OBSERVED: `tokens.css` declares no `--layer-*` token; numeric `z-index` is written in the components `DataTable` (1), `BottomBar` (20), `RowMenu` (30), `ActionBar` (10), `Toast` (90), `Dialog` (100), `CommandPalette` (100) — read by `git grep -n z-index -- pm-web-app/src`
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: tokens.css has no --layer-*; z-index DataTable:158=1 BottomBar:361=20 RowMenu:261=30 (drift) ActionBar:274=10 Toast:73=90 Dialog:166=100 CommandPalette:515=100
- OBSERVED: the further `z-index` literals sit in `FindingsReviewModal` 50, `PlanSettingsModal` 50, `GrantModal` 60, `GitSourceIntakeModal` 50, `LanListenerModal` 100, `HoldResolutionModal` 50, `ConfinedFilePicker` 60, `LessonSourceModal` 50, `StartWorkModal` 50, `EpicDecompositionView` 50, `SystemView` 100, `ProjectOnboardingView` 100 and 110, `CleanupView` 100, `MachineSettingsView` 100, `ProjectExplorerView` 100 (twice), `ProcessDesigner` 100 — same read
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: All 18 further literals match: Findings/PlanSettings/GitSource/Hold/Lesson/StartWork/EpicDecomp 50; Grant/Picker 60; Lan/System/Cleanup/Machine/ProcessDesigner 100; Onboarding 100+110; Explorer 100x2
- OBSERVED: `RowMenu.svelte` filters disabled entries out of the keyboard walk (lines 89-91) and returns on a disabled item (129); `ActionBar.svelte` draws its own `role="menu"` (line 223) — read at both files
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: RowMenu.svelte menuEntries filters disabled at :100-102 and openMenu/item returns on disabled at :141 (drift from 89-91,129); ActionBar.svelte:223 role=menu
- OBSERVED: `Button.svelte` passes the native `disabled` (`disabled={disabled || loading}`), types `size` as `'sm'|'md'|'lg'|'small'`, and `size="small"` occurs 19 times (`CompletionReportView` 2, `DecisionView` 6, `ProjectExplorerView` 2, `ReportsView` 9) — read at `Button.svelte` and by `git grep`
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: Button.svelte:47 disabled={disabled||loading}; :21 size sm|md|lg|small; size=small x19 (CompletionReport 2 Decision 6 ProjectExplorer 2 Reports 9)
- OBSERVED: `visibleLabel` is passed by `DataTable.svelte:121` and `ReportsView.svelte:1171` — read by `git grep visibleLabel`
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: visibleLabel DataTable.svelte:121; ReportsView now passes it at :1290 (was 1171) to RowMenu; RowMenu.svelte:27 and IconButton.svelte:22 also declare it; dev page passes it 3x
- OBSERVED: `RowMenu` is used by `ReportsView.svelte` and the dev page; `ActionBar` by `WorkControlPanel.svelte`, `SettingsView.svelte`, `SystemView.svelte`, `routes/+layout.svelte` and the dev page — read by `git grep "<RowMenu\|<ActionBar"`
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: RowMenu consumers ReportsView:1285 + dev page; ActionBar consumers WorkControlPanel:824,828 SettingsView:375,450 SystemView:782 routes/+layout.svelte:334 dev page
- OBSERVED: no `SegmentedControl`, `Toggle` or `ActMenu` exists; `role="radiogroup"` is hand-written in `OverviewView.svelte:782` and `DecisionView.svelte`, `aria-pressed` in `StepBar.svelte` and `PlanView.svelte` — read by `git grep`
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: No SegmentedControl/Toggle/ActMenu tracked; radiogroup OverviewView:802 (was 782) DecisionView:1038; aria-pressed StepBar:52 PlanView:926
- OBSERVED: `reports.adoc:148` has the word beside the glyph "from the tablet width up" while `components.adoc` § `#icon-button` says `IconButton` never shows its label — read in both documents
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: shared/reports.adoc:148 still has word beside glyph from tablet width up; components.adoc:161 says IconButton never shows its label
- HYPOTHESIS: `Dialog.svelte` and `ConfirmDialog.svelte` already meet most of § `#dialog` and § `#confirm-dialog` — confirm/refute at `Dialog.test.ts` and `ConfirmDialog.test.ts` (verify-at-outline)
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: Dialog.svelte has radius-lg 12px shadow-overlay text-xl labelledby focus trap Escape danger list footer right; ConfirmDialog has busy confirmDisabled typedName; tests 14 and 9
- Verify-first clause: re-read `RowMenu`, `ActionBar`, `Button` and every consumer at the current HEAD after PLAN-129 and PLAN-45; add any new consumer's fields and edges to deliverables 2 and 4.
  - verdict: unverifiable | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: Procedural verify-first instruction for the implementing session, not a code fact; no new RowMenu or ActionBar consumer at HEAD

## Expected Surface

- OBSERVED: `pm-web-app/src/styles/tokens.css` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/styles/tokens.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/ActMenu.svelte` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/ActMenu.test.ts` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/RowMenu.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/RowMenu.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/ActionBar.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/ActionBar.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/SegmentedControl.svelte` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/SegmentedControl.test.ts` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/Toggle.svelte` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/Toggle.test.ts` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/Button.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/Button.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/IconButton.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/Dialog.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/Dialog.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/ConfirmDialog.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/ConfirmDialog.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/DataTable.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/Toast.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/shell/BottomBar.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/shell/CommandPalette.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/ReportsView.svelte` — present at HEAD 1cc0e10e (one prop, `visibleLabel`, and nine `size` values)
- OBSERVED: `pm-web-app/src/lib/views/CompletionReportView.svelte` — present at HEAD 1cc0e10e (two `size` values)
- OBSERVED: `pm-web-app/src/lib/views/DecisionView.svelte` — present at HEAD 1cc0e10e (six `size` values)
- OBSERVED: `pm-web-app/src/lib/views/ProjectExplorerView.svelte` — present at HEAD 1cc0e10e (two `size` values)
- OBSERVED: `pm-web-app/src/routes/dev/components/+page.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/row-menu.spec.js` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/shared/reports.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/StatusLabel.svelte` — present at `dc811fb3` (added by the cleanup of 2026-11-14)
- OBSERVED: `pm-web-app/src/lib/components/StatusLabel.test.ts` — present at `dc811fb3` (added by the cleanup of 2026-11-14)
- OBSERVED: `pm-web-app/src/lib/components/IconButton.test.ts` — present at `dc811fb3` (added by the cleanup of 2026-11-14)
- OBSERVED: `doc/design/web-app/components.adoc` — present at `dc811fb3` (added by the cleanup of 2026-11-14)
- OBSERVED: `doc/design/web-app/components-tags.adoc` — present at `dc811fb3` (added by the cleanup of 2026-11-14)
- OBSERVED: `doc/design/web-app/README.adoc` — present at `dc811fb3` (added by the cleanup of 2026-11-14)

## Dependencies and Sequencing

- Depends on: `PLAN-148` (the dev page and `tokens.css` chain); `PLAN-129` landed (it edits `RowMenu.svelte`, `Button.svelte`, `ReportsView.svelte`); `PLAN-45` landed.
- Overlaps with:
  - `PLAN-129` (staged): `RowMenu`, `Button`, `ReportsView`, `CompletionReportView`.
  - `PLAN-45` (running): `views/`, the dev page.
  - `PLAN-51`, `PLAN-59` (parked): `DecisionView`, `ProjectExplorerView`; the `size` value change is one word, and they re-ground on it.
  - `PLAN-53`, `PLAN-55`, `PLAN-58`, `PLAN-60`, `PLAN-88` (parked): the modals and views on the layer allowlist.
- Adjacent to: `FilterInput` (untouched); the hand-written switches of `OverviewView`, `DecisionView`, `PlanView`, `SystemView`; `TriagePage` (D2).
- Unblocks: PLAN-150 (`Toggle` for `SecretField`); 52 (`disabledReason` in `RowMenu`), 55, 58, 53, 59, 60, 64 (hierarchy, layers); PLAN-153, PLAN-155, PLAN-156.

## Re-Grounding at Restart

Re-read every file above at the then-current HEAD. Run `corpus surfaces` against what landed since (PLAN-45, PLAN-129, PLAN-147, PLAN-148). Settle each HYPOTHESIS. Re-count the `z-index` literals and the 19 `size` values; re-check the operator's answer to D-4.

## PLAN-133 Landing Residue (Amendment 2026-11-13)

- Premise: since `a139ec62` the web chat (entry, plan, epic, review, ended work) was broken against the mock until `PLAN-161`
  (chat bridge). RESOLVED 2026-11-14: `PLAN-161` (#429, `dc811fb3`) landed and removed `PLAN-148`'s `test.fixme`; no `fixme` remains in
  `pm-web-e2e/tests`. Still open: a chat that is not seeded is never OPENED by the clients (GET only) — `PLAN-135`/`PLAN-138` own that;
  an e2e failure on an unseeded chat is theirs, name it in the pull request. An e2e failure at this plan's head that traces
  to the chat contract is not this plan's to fix: name it in the pull request with `PLAN-161` as owner.
- The scenario fixtures are validated at load against the B1 catalogue (unknown chat keys fail closed): a fixture edit in a `chats` block
  must satisfy it.

## PLAN-148 Landing Residue (Amendment 2026-11-14)

Operator decision 2026-11-14 ("Fold into PLAN-149"). PLAN-148 (#427, `25137445`) added `StatusLabel`'s list slot, with two defects that
must be fixed before its first adopter (`PLAN-49`, then 51, 52, 55, 62, 154):

- **D6 (new) — `StatusLabel`'s list slot works in a template.**
  - The prop is named `slot` (`StatusLabel.svelte:13`), Svelte's reserved attribute: `<StatusLabel slot />` directly inside a snippet or a
    component's children is a compile error (`slot_attribute_invalid`, svelte 5.57 `validate_slot_attribute`). Rename it (e.g. `fixed`)
    and update `components-tags.adoc` / `components.adoc` and the dev page.
  - The "fixed-width state slot" (`:47-57`, `inline-grid; grid-template-columns: auto minmax(0,1fr)`) has no width, so it shrink-wraps
    and the dots do not stand one under the other (`components-tags.adoc:102`). Give it a width from a token (the longest state word of
    both locales, or a named size); if the design names none, stop and ask.
  - Tests: one TEMPLATE fixture (a `.svelte` test harness rendering the label inside a snippet and inside a component's children) that
    compiles; a browser check of three labels of different word length in a list, both themes, en/de, measuring that the dots' x-positions
    are equal. `StatusLabel.test.ts:49-67` pins CSS strings only: replace those with the measured check.
  - Persona: plan-marshall:persona-implementer. Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing.
  - Done when: the template fixture compiles and renders; the browser check shows equal dot positions (evidence order `none`); the rename
    fails on a revert of the old prop name in the fixture (evidence order `test-first`).
- Surface: `pm-web-app/src/lib/components/StatusLabel.svelte`, `pm-web-app/src/lib/components/StatusLabel.test.ts` (present at `25137445`).

## Finding from `analyze-fix-build-time` (#432), 2026-11-14

- `routes/dev/components/+page.test.ts` now sets `testTimeout` 20 s (it renders the whole showroom and passed 5 s beside the full
  suite). This plan adds fixtures to that page: replace the per-file setting with one policy for heavy render tests (a named project or
  a shared helper in the Vitest config), or split the page test by section.

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-149-web-foundation-interaction-set.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message.
The orchestrator owns every other ledger write (`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary).

## Parked Gate Check 2026-11-14 (cleanup)

`PLAN-142` and `PLAN-143`, unparked 2026-11-14, may run alongside this plan (operator). The shared files are `ReportsView.svelte`, `routes/dev/components/+page.svelte`, `doc/design/web-app/components.adoc`, `CommandPalette`, `DecisionView` and `ProjectExplorerView`. The second to land rebases.
