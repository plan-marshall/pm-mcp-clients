# Landing Analysis: PLAN-06 — Shared: One Display Vocabulary for Both Clients

epic: ui-improvements
workstream: WS-01
pr: #249 (https://github.com/plan-marshall/pm-mcp-clients/pull/249), merged as `a9513530`

> Landing record for one shipped plan. Written by `analyze` (inbox scan, message
> `rb-06-shared-display-vocabulary-001.md`). The message was checked against the PR and the merge diff
> `2108ef7c..a9513530`.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 Time, duration and title rule in the shared design | shipped-modified | `doc/design/shared/texts-and-languages.adoc` (+80): § Titles and Ids, § Time and Duration, and a documented carve-out for the two-part `state.*`/`unit.*` key shape (operator decision 3). The `doc-review` checker could not run under the lane; the operator accepted an equivalent review (decision 4) |
| D2 State words in the shared catalogue | shipped-modified (scope grew) | `common.{en,de}.properties` (+67 each). `StateWordsCompletenessTest` (+142) and `StateEnumsTest` (+123) in `pm-api`; `StateVocabularyScenarioTest` (+121) in `pm-mock-runtime`. Browser check over 10 routes × 2 locales × 2 themes × 2 widths, before and after. Sonar `0`. **Scope grew by operator decision 5**: five of the eleven kinds had no closed set in the contract, so `pm-api` gained `EpicState`, `LandingState`, `CleanupKind`, `ModelStatus` and `GateKind`, and `PlanActivity` gained `STAGED` and `ENDED` |

Beyond the declared surface: the five enums and `api/decision/package-info.java` in `pm-api`, and the scenario test in `pm-mock-runtime`. The display layers will read these enums: PLAN-09 (web) and PLAN-34 (terminal). PLAN-03 (in flight) changes the mock's scenarios and engine but not `src/test/.../scenario/`, so there was no collision.

## Metrics and Anomalies

- Tokens: `unknown`; `plugin_version` `unknown`. Harness opencode, `deepseek-v4.1-flash/high`. Executor regenerated to 0.1.2021 at the operator's choice, then `executor-skew` answered `could_not_check`.
- Duration: from about 16:20Z on 2026-10-27 to the merge at 02:15Z on 2026-10-28.
- Anomalies:
  - **Build server: every build ran routed** (`daemon_longpoll`), after one expiry at the learned bound (752 s). The run then passed `--timeout 2400` throughout. The bare-`mvn` failure did not recur. The learned bound is now the limit: it is below a full `verify -Ppre-commit` (663–793 s), as PLAN-07 also hit.
  - Self-review `budget-exhausted`: every item from the three rounds was closed, but no round reported clean.
  - Shell reads, reads outside the repository (daemon job logs, `NO_PLAN/build-results`) and sub-agent shell use are disclosed by the run.
  - Commit bodies of five commits carry a literal `\n`. Cosmetic, and it stays: no force-push.

## Routing and Merge Behavior

- Review: CodeRabbit refused three times, then reviewed `1949b4fc` (4 threads, all fixed in `e74237a` and resolved). It was refused again after the last push, and the standing authorization was applied: the last two heads were not reviewed. Sourcery was out of budget until 2026-11-02.
- CI/merge: one duplicate-run Sonar flake (non-main branch access), green on re-run. Merged with `main` merged in (`2d1004ce`). No conflicts with PLAN-07 or PLAN-76.
- Sonar: `0` confirmed at `2d1004ce`.

## Reconciliation Actions

- [x] row `status` → `shipped`
- [x] row `pr` stamped — `249`
- [x] row `landing` stamped — `landings/PLAN-06.md`
- [x] row `plan_marshall_plan_id` stamped — `06-shared-display-vocabulary`
- [x] epic.md: Open Defect recurrence; the build-server watch narrowed from bare `mvn` (fixed) to the learned timeout bound; new watches for the `phase_state` drift and the mock's `kind="item"`; the operator decisions on key shape and the new enums recorded
- [x] resume anchor updated, `queue-view.md` regenerated

## Follow-Ups

- Raw ISO instants on the plan, reports and settings views are pre-existing and need client code. PLAN-09 (`web-display-layer`) names `PlanView`, `ReportsView` and `MachineSettingsView`; PLAN-34 covers the terminal. No fold.
- `EpicProcessPlan` documents `phase_state` as `running/staged/planned/done`, the scenarios serve `staged`/`ended`, and `PlanActivity.parse("done")` throws. Pre-existing contract drift: recorded as a watch for PLAN-34, the next plan to touch the `pm-api` display contract.
- The mock writes a kept item with `kind="item"` (`MockRuntimeEngine:7462`), outside `CleanupKind`. Recorded as a watch for PLAN-11 (`mock-cli-fixes`).
- Two-part keys (`unit.epic`, `unit.phase-6-finalize`) rest on the carve-out alone, and nothing checks key shape. Recorded in the decision; no plan.
