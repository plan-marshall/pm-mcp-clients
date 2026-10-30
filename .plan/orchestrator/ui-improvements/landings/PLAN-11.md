# Landing Analysis: PLAN-11 — Mock: Plan Details per Id, Routes, Lifecycle State

epic: ui-improvements
workstream: WS-01
pr: #269 (merge commit `bae3a8c8835fca95825ae4d4fc5f2763e0c4697e`, branch `feature/11-mock-cli-fixes`)

> Landing record for one shipped plan. Source: inbox message `rb-11-mock-cli-fixes-001.md` (runbook lane, harness
> `opencode`, model `deepseek-v4.1-flash/high`, executor `0.1.2032`), analysed 2026-10-30 against the merged PR, the CI
> checks, the merge diff and the Sonar gate. The browser artifacts, the per-head build logs and the three self-review
> rounds are the run's account and are not re-run here.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 Plan details per id (two ids differ, unknown id `404 plan_not_found`) | shipped-as-specified | `PlanDetailTest` (+130) in the merge diff; `MockRuntimeEngine` (+272/-31) serves the plan per id. `test-first` red run recorded (job and log named). The browser clause is recorded as 8+8 snapshots and two screenshots under `.plan/temp/playwright-mcp/`. |
| D2 Routes and lifecycle state (`/api/v1/plans` in every scenario; `lifecycle_state` for each `system.*`; `kind` from `CleanupKind`) | shipped-as-specified | `ScenarioStateContractTest` (+91) and `FixtureConsistencyTest` (+27); the four scenarios without a plan list gained one (`designer`, `onboarding`, `projects-and-start`, `reports-and-system`, 5 lines each). The lifecycle half was green at base, as the re-grounded spec framed it: the test now pins every `system.*` state and the draining blocker count. The `kind="item"` residue is fixed (the kept kind is from the closed set). |
| D3 README socket name | shipped-as-specified | `pm-mock-runtime/README.adoc` names `run/runtime.sock`; the root and operator READMEs corrected too (undeclared, same wrong name). |
| D3 open-issue read | met | `open_issue_count=0`, `open_issue_rules=none` in the landing; corroborated below. |

`deliverables_done=3` of 3 is consistent with the diff.

**Surface delta** (computed here from `git diff bae3a8c8^1 bae3a8c8`, because `inbox landing-check` answers
`unmeasured`): declared 7 entries, realized 21 files. Four declared entries were changed (`MockRuntimeEngine.java`,
`reports-and-system.json`, `README.adoc`, the `test/.../runtime/` directory with three files). Three declared entries
are untouched: the `mock/http/` directory and the two e2e specs `plan-view.spec.js` and `system.spec.js` (declared
"touched only if an assertion changes"; none did). Fifteen files are undeclared: three scenario files
(`designer`, `onboarding`, `projects-and-start`), `PlanViewScenarioTest` and `StartScenarioTest` (under
`mock/`, not `mock/runtime/`), `plan_view.{de,en}.properties` in `pm-api`, the root and operator READMEs, and six
`pm-web-app` files (`client.ts`, `runtime.ts`, `runtime.test.ts`, `PlanView.svelte`, `PlanView.test.ts`). The
`pm-web-app` files exist because the browser clause needs the app to render "not found" and the draining banner; the
spec named a Playwright check but declared no app file. The PLAN-03 fixture watch predicted this under-declaration. Insertions 748, deletions 31.

## Metrics and Anomalies

- Tokens: `unknown` (not measured on this harness, recorded as unobserved, not as zero). `plugin_version` `unknown`.
  `executor_version` `0.1.2032`.
- Anomalies:
  - Executor skew at the start (0.1.2028 against 0.1.2032); the operator chose "regenerate the executor now", the
    re-check read `match`.
  - Self-review used its three rounds plus one confirmation round the operator asked for; step `self-review` is
    recorded `budget-exhausted`. Round 3 and the confirmation round still found real defects (a broken e2e spec, a dead
    branch, fixture drift), so the budget was short for this plan's size.
  - One quality-gate run hit the 822 s daemon bound (head `92fcae2b`) and was re-run with `--timeout 1800`, green.
  - Read-class shell use (`grep`, `tail`, `ls`) and one `;` / pipe join while reading build logs; no file written
    through it. Guard `none` on this harness.

## Routing and Merge Behavior

- Review: CodeRabbit (required, quota-limited) reviewed four heads (`2fabdbe2`, `71b86c65`, `f86f4846`, `65d201ef`) and
  refused on quota four times; every finding fixed or answered, every inline thread resolved. It did not review the
  merge head `b6701025`; the merge ran under the standing review authorization. Sourcery refused on its quota
  (optional). One finding was answered, not fixed (see Follow-Ups).
- CI/merge: `ci checks status --pr-number 269` is `success`, 20 checks, all `pass` or `skipping` (the CodeRabbit check
  reads `NEUTRAL`). `origin/main` carries `bae3a8c8`.
- Sonar: `sonar_rest gate-status --project plan-marshall_pm-mcp-clients --pr 269` answers `gate_status: OK`, six
  conditions OK (new coverage 84.5, duplicated lines 0.0, hotspots reviewed 100). A confirmed `0`, corroborated.

## Reconciliation Actions

- [x] row `status` → `shipped` — `orchestrator queue --transition PLAN-11 --status shipped`
- [x] row `pr` stamped `269`
- [x] row `landing` stamped `landings/PLAN-11.md`
- [x] row `plan_marshall_plan_id` stamped `11-mock-cli-fixes` (the sender's derived name)
- [x] epic.md narrative reconciled against the queue rows
- [x] defects and watches opened, updated or retired (see Follow-Ups)
- [x] resume anchor updated in `resume_anchor.md`
- [x] `queue-view.md` regenerated
- [x] inbox message `rb-11-mock-cli-fixes-001.md` archived after this record

## Follow-Ups

- **Incomplete landing facts** (`total_tokens`, `plugin_version` unknown): recurrence on the Open Defect.
- **`kind="item"` residue retired:** the kept cleanup item now writes a `CleanupKind` member (D2); the watch closes.
- **Mock epic routes still stale:** `actions/cancel` ignores `with_plans` and `actions/next` is a no-op; the run did not
  touch them (not in the spec). Still no plan owns them; the watch stays.
- **Four `system.*` lifecycle states stay invisible in the web app:** PLAN-11 made the mock serve them and pinned them
  in a test, and the app now shows the draining banner with the remaining count (`Stopping when what runs is done · 2
  left`); `system.running`, `stopped`, `restarting` and `force-stop` still change no view. The watch stays; its owner
  decision is still open (PLAN-39 does not take it; PLAN-09 or a new plan, at the latest PLAN-41).
- **Under-declared web side of a mock change:** PLAN-03's watch predicted this ("PLAN-11 is likely to repeat the
  under-declaration"); six `pm-web-app` files and four scenario files were changed beyond the declaration. The watch
  stays for PLAN-47, 50, 52, 55, 58, 62; PLAN-11 is struck from its list.
- **Residue named by the run (accepted, not fixed):**
  - The drain-reconcile race after the event stream connects (CodeRabbit follow-up): the status read is the load-time
    truth, closing the gap needs a connection-established signal the SSE client does not expose.
  - A drain started through `POST /api/v1/operator/runtime/stop` leaves the status without `drain_remaining_count`
    (only the `goto` path carries it).
  - A paused plan's synthesized current node is `waiting_for_operator`; `NodeState` has no paused value.
  - The draining banner's `what` stays generic ("running work").
  - `lifecycleState.name().toLowerCase()` is default-locale (pre-existing).
  - The synthesized plan's activity body is the template's for a listed plan without its own activity fixture.
  Recorded as a Watch for the plans that read the mock plan detail (PLAN-20, 47, 52, 55).
- **Self-review budget:** the confirmation round the operator asked for was productive; noted, no action.
- **Branch and worktree:** no `feature/11*` branch remains on the remote.
