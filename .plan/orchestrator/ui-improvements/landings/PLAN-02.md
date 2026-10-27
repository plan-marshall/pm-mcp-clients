# Landing Analysis: PLAN-02 — Terminal: PTY Dialogue Harness

epic: ui-improvements
workstream: WS-04
pr: #232 (https://github.com/plan-marshall/pm-mcp-clients/pull/232), merged as `1864d8a3`

> Landing record for one shipped plan. Written by `analyze` (inbox scan, message
> `rb-02-cli-pty-dialogue-harness-001.md`) after checking the message against the PR, the
> merge diff `65973291..1864d8a3`, the code on `main` and the lane's progress file
> `.plan/temp/rb-02-cli-pty-dialogue-harness/progress.md`.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 `/mock/acts` | shipped-as-specified | `MockRuntimeEngine.java` (+47) and `MockRuntimeEngineTest.java` (+40) in the merge diff; failing run before the change and green run after, both in the progress file |
| D2 Dialogue ITs | shipped-as-specified | Seven `*DialogueIT` classes on `main`, each `@Disabled` naming its fixing plan: PLAN-10 (pause, cancel), PLAN-17 (runtime stop), PLAN-21 (answer), PLAN-22 (onboarding, project setup), PLAN-23 (cleanup). All five plan ids exist in the corpus |
| D3 Coverage of the dialogue paths | shipped-modified (met as written) | `pm-operator/pom.xml` binds the JaCoCo report after failsafe. The report lists the dialogue classes, but the bundle stays at 11.67 % in the combined profile while the ITs are disabled. Open by design until PLAN-10/17/21/22/23 enable them |
| D4 Screen capture for review | shipped-as-specified | `ScreenCaptureIT.java` (+263), `screen-captures/baseline.txt`, design doc `27-tui-testing.adoc` (+79). 12 captures before and after under `.plan/temp/terminal/02-cli-pty-dialogue-harness/` |

Added beyond the declared surface: `pm-mock-runtime/README.adoc` (lists the new endpoint), and three test-scope files in `pm-operator/src/test/java/de/planmarshall/operator/tui/` (`AnsiScreen.java`, `AnsiScreenTest.java`, `VirtualScreenTerminal.java`, extended for the cell attributes). There is no change below `pm-operator/src/main` (checked: the diff there is empty). The declared `pm-mock-runtime/.../mock/http/` directory was not touched; the act log lives in `MockRuntimeEngine`.

## Metrics and Anomalies

- Tokens: `unknown`; the opencode harness measured none.
- Duration: from setup at 2026-10-26T18:35Z to the landing message at 2026-10-27T02:56Z, about 8 h 20 min, mostly spent waiting for the review bots' quota.
- Model: `deepseek-v4.1-flash/high` on opencode; executor 0.1.2017; `plugin_version` unknown.
- Anomalies:
  - `executor-skew` answered `could_not_check` again: the lane text version cannot be derived from the opencode install path. The operator chose to proceed. The lane defect is not fully fixed.
  - Guard: `none` on this harness (unenforced); `.plan/temp/guard.log` does not exist.
  - The main checkout was dirty at setup (`queue/PLAN-01.json`, the orchestrator's uncommitted `launched` transition). This is fixed since: `orchestrator.use_worktree` is on (PR #230).
  - One rule break by the run: the first test edit landed in the main checkout. It was reverted before any commit and re-applied in the worktree.
  - Build server: the one routed build failed with exit -1 because the daemon invoked a bare `mvn`. Every later build ran in-process. This is a build server defect, not a plan defect.
  - Self-review hit its round budget (`self-review:budget-exhausted`); the three open items are by design, see D3 and Follow-Ups.
  - Leftover: a detached worktree `.plan/temp/base-check` at `65973291`, presumably the checkout for the before captures. The landing reports `cleanup_owed=false`.

## Routing and Merge Behavior

- Review: Sourcery reviewed `cdf32d0` twice (19 threads, all answered and resolved). CodeRabbit was refused on quota twice, then reviewed `cfdc1e5`, `0097b11`/`bc494b1` and `67f748a`. All of its findings are fixed, the last ones in `33db0a0`. CodeRabbit stayed silent on the final head `33db0a0`, and the standing review authorization was applied. No thread is unresolved.
- CI/merge: two early `sonar-build` failures (SonarCloud refused the quality-gate read on a non-main branch) went green on re-run. Merged through the merge queue (merge_group run 40440309125 success). There were no rebase conflicts.
- Sonar: `open_issue_count=0`, `open_issue_rules=none`, read at `33db0a0`. Earlier heads had S5976 and two TODO markers, each fixed and re-read. This is consistent with the progress file.

## Reconciliation Actions

- [x] row `status` → `shipped` — `orchestrator queue --transition PLAN-02 --status shipped`
- [x] row `pr` stamped — `orchestrator queue --set-row PLAN-02 --field pr --value 232`
- [x] row `landing` stamped — `orchestrator queue --set-row PLAN-02 --field landing --value landings/PLAN-02.md`
- [x] row `plan_marshall_plan_id` stamped — `orchestrator queue --set-row PLAN-02 --field plan_marshall_plan_id --value 02-cli-pty-dialogue-harness`
- [x] epic.md narrative reconciled: Open Defect for the incomplete landing facts; Watches for the lane skew, the build server's bare `mvn`, the leftover `base-check` worktree, and the five plans that must enable their ITs
- [x] resume anchor updated
- [x] `queue-view.md` regenerated

## Follow-Ups

- The message names PLAN-04 and PLAN-16 as owners of the two baseline defects (the `-` placeholder in the plan view's task line, the relative "today" in the status header). Both are web plans. The terminal owner is PLAN-34 (`cli-display-layer`), whose Done-when already names "no `-` placeholder or ISO time" on the overview, plan view and status screens. No fold is needed.
- PLAN-10, PLAN-17, PLAN-21, PLAN-22 and PLAN-23 each remove the `@Disabled` of their IT; their specs already say so (PLAN-02 standing constraint).
- PLAN-03 (`mock-scenario-states`) is no longer held: PLAN-02 has landed.
