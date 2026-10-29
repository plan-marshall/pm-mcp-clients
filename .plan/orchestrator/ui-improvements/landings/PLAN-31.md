# Landing Analysis: PLAN-31 — Web: Core Components, Containers

epic: ui-improvements
workstream: WS-02
pr: #266 (merge commit `0351ce78e2717ee5ab04bd51aeaed4a59aa8e1b0`, branch `feature/31-web-core-components-containers`)

> Landing record for one shipped plan. Source: inbox message `rb-31-web-core-components-containers-001.md` (runbook
> lane, harness `opencode`, model `deepseek-v4.1-flash/high`, executor `0.1.2032`), analysed 2026-10-29 against the
> merged PR, the CI checks, the merge diff and the Sonar gate. The browser artifacts, the per-head build logs and the
> three self-review rounds are the run's account and are not re-run here.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 Card and KeyValueList (head with actions; no card in a card) | shipped-as-specified | `Card.svelte` (+147), `KeyValueList.svelte` (+49) and their tests are in the merge diff. The nested card renders a divider and its `Detail` head (the dev-warning-or-divider clause is met by the divider). `test-first` red run recorded. |
| D2 Tabs and Breadcrumb (keyboard; middle shortening at phone width) | shipped-as-specified | `Tabs.svelte` (+166), `Breadcrumb.svelte` (+135), `shortenMiddle.ts` (+23) with tests. Counts use `CountBadge`; only `id` crumbs are shortened (a CodeRabbit finding). `test-first` red run recorded. |
| D3 Dialog, Toast, AttentionStrip | shipped-as-specified | `Dialog.svelte` (+228), `Toast.svelte` (+106), `AttentionStrip.svelte` (+54) with tests. Escape closes and focus returns (browser snapshots show the opener `[active]`). `test-first` red run recorded. |
| D3b Open-issue read | met | `open_issue_count=0`, `open_issue_rules=none` in the landing; one confirmed `typescript:S5906` at `acbeae9b`'s predecessor was fixed in `d04ffe00`. Corroborated below. |

`deliverables_done=3` of 3 is consistent with the diff. The development page `/dev/components` was extended, not
re-created (+153 lines), exactly as the re-grounded spec asked; `lib/dev/devRouteExclusion.ts` was left untouched.

**Surface delta** (computed here from `git diff 0351ce78^1 0351ce78`, because `inbox landing-check` answers
`unmeasured`): declared 8 (seven components and the dev page), realized 23. All eight declared files were changed;
none declared is untouched. Fifteen files are undeclared and all are named in the message: seven component tests,
`lib/utils/shortenMiddle.ts` and its test, four test fixtures under `src/test/fixtures/`, and `common.{de,en}.properties`
(+36 each; the spec's Standing Constraints name the `dev.*` keys of that pack). Nothing outside `pm-web-app` and the
`pm-api` message pack. Insertions only (2045), no deletion.

## Metrics and Anomalies

- Tokens: `unknown` (not measured on this harness, recorded as unobserved, not as zero). `plugin_version` `unknown`.
  `executor_version` `0.1.2032`.
- Anomalies:
  - **Main-checkout write:** the two `common.*.properties` edits were first applied in the main checkout by mistake,
    reverted by hand and re-applied in the worktree; the main checkout was verified clean. A repeat of a rule the
    runbook lane already states. The merged diff contains the properties once. Recorded.
  - Executor skew at the start (0.1.2028 against 0.1.2032); the operator answered "already regenerated in parallel
    session", the re-check read `match`.
  - The first two routed builds answered `execution_failed` with an empty inner log; every later build ran
    `--execution-mode in_process` (the preflight had answered `ready`). Seen on PLAN-15, 19 and 27.
  - One flake in the first merge-head CI run, `pm-operator` `TuiScreenDriverTest.shutdownHookRestoresOnce`, a module
    this plan does not touch; passed on re-run.
  - One shell pipe used while reading review output; no file written through it. Guard `none` on this harness.

## Routing and Merge Behavior

- Review: CodeRabbit refused on quota twice. Its one formal review covers `d04ffe00`: 4 inline findings, 8 follow-ups,
  4 nits, all fixed in `2b16afe`, all four threads resolved ("Addressed in commit 2b16afe"). It never reviewed `2b16afe`
  or the merge head `acbeae9b`. Sourcery refused on its quota (optional). The merge ran under the standing review
  authorization: the lane waited 90 minutes and re-triggered once. This is the second plan of the epic that merged
  without a review of the final head (PLAN-08 and PLAN-27 earlier, the latter with a review of the merge head).
- CI/merge: `ci checks status --pr-number 266` is `success`, 20 checks, all `pass` or `skipping`. `origin/main` carries
  `0351ce78` after #267; the organisation workflows moved to v0.43.0 in #268 straight after.
- Sonar: `sonar_rest gate-status --project plan-marshall_pm-mcp-clients --pr 266` answers `gate_status: OK`, five
  conditions OK (duplicated lines 0.0, hotspots reviewed 100). A confirmed `0`, corroborated.

## Reconciliation Actions

- [x] row `status` → `shipped` — `orchestrator queue --transition PLAN-31 --status shipped`
- [x] row `pr` stamped `266`
- [x] row `landing` stamped `landings/PLAN-31.md`
- [x] row `plan_marshall_plan_id` stamped `31-web-core-components-containers` (the sender's derived name)
- [x] epic.md narrative reconciled against the queue rows
- [x] defects and watches opened, updated or retired (see Follow-Ups)
- [x] resume anchor updated in `resume_anchor.md`
- [x] `queue-view.md` regenerated
- [x] inbox message `rb-31-web-core-components-containers-001.md` archived after this record

## Follow-Ups

- **Incomplete landing facts** (`total_tokens`, `plugin_version` unknown): recurrence on the Open Defect.
- **Main-checkout write:** a second report of a run editing the main checkout (PLAN-10's `test-results/` was the
  first, different cause). Added to the existing watch.
- **Modals are not migrated:** the nine `*Modal.svelte` components still stand beside the new `Dialog`; the spec
  keeps that for the view plans (VD-S-06). Check at PLAN-43 and the view plans that name a modal.
- **Components unused in views:** all seven components have no caller yet beyond the dev page; PLAN-39 (shell),
  PLAN-43 (templates) and the view plans bring them in. `Toast` has no host or store: whichever plan raises the first
  toast must own it. Recorded as a Watch.
- **Accepted findings** (lane): a focused act inside a 4-second toast is removed by the timer; a danger dialog with an
  empty `consequences` list draws no list.
- **PLAN-39, PLAN-37 and PLAN-35 are unblocked on their PLAN-31 dependency.** PLAN-39 and PLAN-35 re-read at HEAD
  before emit (the dev page and `common.{de,en}.properties` changed again).
- **Branch and worktree:** no `feature/31*` branch remains on the remote.
