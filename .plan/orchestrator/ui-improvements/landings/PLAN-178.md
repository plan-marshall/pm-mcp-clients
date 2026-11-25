# PLAN-178 landing record

epic: ui-improvements
plan: rb-178-web-dialog-form-and-designer-correctness (runbook lane, harness opencode, model deepseek-v4.1-flash/high, plugin and executor 0.1.2183; `total_tokens` `unknown`: the 2026-10-27 Open Defect, not re-reported)
pr: #557, squash `e22b302d`, merge queue, merged 2026-11-24T20:14:26Z
analysed: 2026-11-25, by sub-agent in three layers, by reading only (no build); the remove dialog's return focus (`ProcessDesigner.svelte:1484-1492`), `Dialog.svelte:74-79,174-184` and the nine pm-operator/mock hunks re-read by the orchestrator at `e22b302d`

Message `rb-178-web-dialog-form-and-designer-correctness-001` (revision 1): `merge_state=merged`, `deliverables_done=3` of 4 (D4's green `-Pe2e` run not met: 11 failures), 0 unresolved threads, 12 of 12 review-body findings handled, `sonar_open_new_issues=0`, `self-review: clear`. `landing-check`: `complete: false`, missing `total_tokens` only.

**Review numbers re-read** (`runbook_lane review-gate --pr-number 557`, after the merge): 0 unresolved threads, 12 review-body findings, equal to the message; `handled` reads 0 there (`body_findings_undisposed`: no disposition line after the merge). The leaf traced all twelve named items to code or tests.

**Sonar.** PR: `gate-status --pr 557` OK. Default branch: `main` analysis 2026-11-24T20:32:09Z (after the merge): `read_open_issues` `confirmed`, the same 9 keys as `landings/PLAN-186.md`; this landing added none. (The later `main` commit `20f971fe`, #570, is docs-only; its build and Sonar analysis were skipped: see the Open Defect "main is red at 20f971fe".)

## Layer 1: specified

| D | Verdict |
|---|---|
| D1 focus, DataTable region, cleanup notice | met (`SystemView.svelte:709,1276`; `DataTable.svelte:69`; `CleanupView.svelte:179`); the optional "tab stop only when the region scrolls" skipped, not reported |
| D2 designer dialogs | met: compare token and `retireComparison` (`ProcessDesigner.svelte:849-880,970`), `designer.place.compare_failed` en/de, removal through `ConfirmDialog` (`:993-1030,1479`), `Dialog` `size`/`footer` (`Dialog.svelte:195,259`), `:global` override gone |
| D3 field key, NumberField, SecretField | met in behaviour; method differs (declared): `controlForKey` compares name/id/testid (`forms/index.ts:35`) instead of an escaped selector; the Tab-order test drives `focus()`, not Tab (partly) |
| D4 browser evidence | **not met** on the green `-Pe2e` clause (11 failures; plausibly `main`'s own, the `/projects/onboarding` rename recorded at PLAN-185, not proven); screenshots not shown by reading |

True count by reading: 3 of 4, equal to the message. Spec flaw: D4's green-run clause depends on `main` being green. The lane's "refuted" cascade reading misses Svelte's scoping hash (`.dialog.svelte-x` is (0,2,0)); the outcome is the same.

## Layer 2: implemented

Every web, catalogue, doc, gallery, fixture and e2e file traces to a deliverable. **Nine untraced files**: whitespace-only continuation-indent rewrites by `-Ppre-commit` in `pm-operator` (`EntryFlow`, `PlanCommand`, `ActionMenus`, `OverviewFullScreenApp`, `SystemLevel`, three tests) and `MockRuntimeEngine.java` (1-5 lines each, re-read), against the spec's "pm-operator not touched here"; admitted in the narrative, not the PR body. Catalogue: en/de parity holds; `designer.leave.discard.title` and `plan.activity.kind.egress` have no reader left. Weak tests: the `modalWhen` test is in no cited failing run; NumberField's "blur" case fires `change`; the remove Cancel test asserts no focus.

## Layer 3: correct (reported to the operator; no fix plan staged)

No blocker, no major.

| # | Severity | Finding | Where | in Residue |
|---|---|---|---|---|
| L3-1 | minor | the remove `ConfirmDialog` returns focus to the units heading on Cancel/Escape/refusal while the Remove… opener stays mounted (overrides `Dialog.svelte:77`'s `returnFocusElement ?? activeElement`). Re-read. | `ProcessDesigner.svelte:1489` | no |
| L3-2 | minor | focus recovery runs only when `footer` is false (re-read `Dialog.svelte:179`); in the place dialog Compare/Retry swap the footer buttons and focus falls to body | `Dialog.svelte:179`, `ProcessDesigner.svelte:1610-1660` | no |
| L3-3 | minor | Retry after a failed compare shows no loading and keeps the stale failure text until the answer | `ProcessDesigner.svelte:1626-1640` | no |
| L3-4 | minor | SecretField's `storeFailed` clears only when the next store starts, not on typing; a caller's `error=""` hides it | `SecretField.svelte:90-94,157` | no |
| L3-5 | minor | the DataTable region carries the same name as its table (a landmark named twice; `landmark-unique` with two equal labels not shown by reading) | `DataTable.svelte:69` | no |
| L3-6 | info | the `egress` sentence removed from `plan-view.adoc:161` because the client has no such kind; whether the daemon still emits `egress` is not shown | `doc/design/shared/plan-view.adoc:161` | PR body only |

Checked clean: the focus trap and nested-dialog guard, form keys (no throw; Enter and IME untouched; no double commit), the compare token and its reset, the failed-compare retry target, `removeUnit`'s pinned scope.

Conformance: no `{@html}`, no timer wait, en/de parity, no dependency, no static style: conforms. Write set: deviates (the nine whitespace files). Failing runs, e2e, Sonar count, screenshots: the lane's, not re-run.

## Residue for the plans that touch the same surface

- L3-1 to L3-5: no staged plan carries `ProcessDesigner`/`Dialog`/`SecretField`/`DataTable`; a fix plan only on the operator's word.
- The nine whitespace rewrites: possible merge friction for the pm-operator plans (PLAN-189 running; the CLI chain re-grounded against `e22b302d`).
- The `-Pe2e` red run: the route rename already recorded at PLAN-185 (PLAN-182's #569 fixes `projects.spec.js` and the onboarding address).
