# PLAN-187 landing record

epic: ui-improvements
plan: rb-187-web-withdraw-decomposition-drop (runbook lane, harness antigravity, `model`, `effort`, `total_tokens` `unknown`; `plugin_version` 0.1.0, executor 0.1.2183)
pr: #561, squash `d0bfd854` (15 files), merge queue, merged 2026-11-24T17:43:08Z
analysed: 2026-11-24, by sub-agent in three layers, by reading only (no build); `firstChangedPlan` (`EpicDecompositionView.svelte:241-290`), the Keep button (`:403-412`), `StartView.svelte:1149` and `doc/design/terminal-client/21-texts-starting.adoc:200-206` re-read by the orchestrator at `d0bfd854`

Message `rb-187-web-withdraw-decomposition-drop-001` (revision 0): `merge_state=merged`, `deliverables_done=2` of 2, 0 unresolved threads, 2 of 2 review-body findings handled (CodeRabbit `outside-diff-1`/`-2`, declined with a reply `#issuecomment-7134823819`: Keep/Edit stay on wire-dropped plans as the terminal offers them), `sonar_open_new_issues=0`, `self-review: clear` (round 1 found a pre-existing i18n reactivity bug, fixed in `493d77f9`). `landing-check`: `complete: false`, missing `total_tokens`, `model`, `effort` (the 2026-10-27 Open Defect; this harness reports no model).

**Review numbers re-read** (`runbook_lane review-gate --pr-number 561`, after the merge): 0 unresolved threads, 2 review-body findings, equal to the message; `handled` reads 0 there (`body_findings_undisposed`: the gate reads no disposition line after the merge). Not re-verified beyond the message.

**Sonar.** PR: `sonar_rest gate-status --pr 561` OK (duplication 0.0, hotspots 100 %). Default branch: `main` analysis 2026-11-24T18:01:30Z (after the merge): `read_open_issues` `confirmed`, the same 9 keys as `landings/PLAN-186.md`, none in `pm-web-app`, `pm-api` resources or `doc/`. This landing added no default-branch issue.

## Layer 1: specified

| D | Verdict |
|---|---|
| D1 Drop leaves both decomposition views | met: `droppedPlans`, `keep()`/`drop()`, the pre-agree rewrite and the Drop button gone from `EpicDecompositionView`; no `onDrop`/`plan-drop` in `DecompositionReviewView`; `dropPlan` gone from `StartView`; `decomposition.drop` and `start.decomposition.drop` removed en/de and listed in `RETIRED_ACT_KEYS` (`catalogue.test.ts:40-41`) |
| DW1.2 the Agree gate counts only wire findings | met trivially: the gate (`disabled={!everyCheckPasses(cut) \|\| busy}`, `:470`) never read client state at the base; the spec's premise ("`:507` reads dropped plans") was stale and the lane did not say so |
| DW1.3 e2e rewritten | rewritten; the runs are the lane's (not shown by reading) |
| DW1.4 browser check | not shown by reading (gitignored screenshots) |
| D2 the design says what the clients do | met for `doc/design/shared/` (`starting-work.adoc:206`, `starting-work-texts.adoc` parity with the catalogue); **missed** `doc/design/terminal-client/21-texts-starting.adoc:204` (`operator.start.decomposition.drop` "drop"/"verwirf", L3-3) |

True count by reading: 2 of 2 (Done-whens 4 of 5 shown; the browser check not shown).

## Layer 2: implemented

Every changed file traces to a deliverable; the `$t`-argument fix in `DecompositionReviewView.svelte:70-107` is the disclosed self-review fix. Real guards: the `plan-drop-*` testids are null, the German `entfernt`/`verworfen` labels, the `.status-label--neutral` on a dropped card, `RETIRED_ACT_KEYS`. Weak: the English `toHaveTextContent('dropped')` checks are vacuous (the fixture's plan is titled `relay-dropped`); the `queryByRole('button', {name: 'Drop'})` checks pass whatever the markup does (the label no longer exists); "agree checks and commits directly" cannot fail on a revert. Gaps: no test for a wire-dropped plan that carries an error finding; the e2e fabricates `status: 'dropped'` through `page.route` (the mock never sends one), not disclosed.

Lane claims: §3 hypotheses confirmed (`MockRuntimeEngine.java:4703,4709`; `EpicDecompositionDialogue.java:288-289`); §4 declined findings confirmed (the terminal's `openPlan` `:291-309` offers k/e/b for any plan); DW1.2 cited `:325`, the gate is `:329` (mislocation); §9 "None in plan scope" refuted in part (below).

## Layer 3: correct (reported to the operator; no fix plan staged)

No blocker. The i18n reactivity fix is correct (legacy-mode component; `$t` now passed as an argument).

| # | Severity | Finding | Where | in lane §9 |
|---|---|---|---|---|
| L3-1 | minor, near major | a removal by instruction is invisible in the read-back the design now cites ("removing a plan is an instruction … read back before agreement"): `firstChangedPlan` walks only `after`, so a plan missing from the new cut is never found and it falls back to the first plan, whose rows are equal (the read-back shows "—"); in the Start wizard a null-plan instruction finds no `after` and sets no read-back, replacing the cut silently. Re-read. Engine removal behaviour not checked. | `EpicDecompositionView.svelte:241-290`, `StartView.svelte:818-835` | no |
| L3-2 | minor | Keep is a dead act: the Epic view's Keep button lost its `onclick` (no handler, `:403-411`), the Start wizard passes `onKeep={() => {}}` (`:1149`); against the plan's own rule "no client act without a wire act". Re-read. | `EpicDecompositionView.svelte:403-411`, `StartView.svelte:1149`, `DecompositionReviewView.svelte:208` | no |
| L3-3 | minor | the terminal design still lists `operator.start.decomposition.drop` ("drop"/"verwirf"); no code defines `operator.start.*` keys and the terminal offers no drop key. Re-read. | `doc/design/terminal-client/21-texts-starting.adoc:204` | no |
| L3-4 | info | the design says later runs mark plans `unchanged`/`changed`/`new`/`dropped` and grey locked ones; the Review view shows only `dropped`. Pre-existing, not claimed. | `starting-work.adoc:212`, `DecompositionReviewView.svelte:169-185` | no |
| L3-5 | info | the mock still rewrites the first open plan on a null plan id (dev only; the spec forbade touching the mock) | `MockRuntimeEngine.java:4703` | no |
| L3-6 | info | the Start wizard commits without a `checkDecomposition` call (the Epic view checks first); pre-existing | `StartView.svelte` `commitDecomposition` | no |

Residual references: none of `droppedPlans`, `dropPlan`, `plan-drop-*`, `decomposition.drop` in `pm-web-app`, `pm-api`, `pm-operator` beyond the retired-key guard; `start.decomposition.deliverables` and `.order` are unused catalogue entries (dead content, not a Done-when miss).

Conformance: no `{@html}`, no timer wait, en/de parity, no dependency, no shim, mock untouched: conforms. "A test is fed with what the engine sends": deviates (the fabricated e2e `dropped`, unavoidable, undisclosed). Builds, e2e, coverage, CI: the lane's, not re-run.

## Residue for the plans that touch the same surface

- **L3-1 and L3-2** (the decomposition views' read-back and Keep): no staged plan carries `EpicDecompositionView`/`StartView`/`DecompositionReviewView`; a fix plan only on the operator's word.
- **L3-3**: a trivial doc change (skip-bot-review) or a fold into the next terminal-design plan, on the operator's word.
- The `-Pe2e` pre-existing failures (`projects.spec.js:112`, `theme-readability.spec.js` onboarding route) recur, already recorded at PLAN-185; PLAN-182 touches `theme-readability.spec.js`.
- The vacuous English assertions: a fold candidate with L3-1/L3-2.
