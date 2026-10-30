envelope_version=1
sender_type=plan
sender_id=rb-20-cli-actions-by-state
epic=ui-improvements
kind=landing
created=2026-10-30T17:17:31Z

```landing-facts
schema=landing-facts/1
plan_id=rb-20-cli-actions-by-state
epic=ui-improvements
pr=#273
merge_state=merged
cleanup_owed=false
deliverables_total=1
deliverables_done=1
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash-free/high,deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2032
steps=setup:done,implement:done,build-gate:done,self-review:budget-exhausted,pr:done,reviews:done,merge:done
step.sonar-roundtrip.open_issue_count=0
step.sonar-roundtrip.open_issue_rules=none
```

guard: none on this harness (unenforced)

## Deliverables

**D1 — "Menus from links", 1 of 1 deliverable fully met.** Head landed at `af18ec8e`, merge commit `a07a09b60202a0b89639a521e1ec7f1e7729a3af`.

- **Done-when (a) — `ActionMenusTest` has one case per plan state of `19-actions.adoc`.** Met, `order: test-first`. The failing run is commit `9f50f230` (the test written and left uncommitted in the working tree), surefire `maven-2026-10-29-155346.log`: `waitingPlan:88 [Pause…, Cancel…, Chat…, Settings…] ==> expected: <5> but was: <4>`, 1 failure of 13. Green at `0334ab8a` (scoped run `maven-2026-10-29-155720.log`, 18 tests). At the head the class covers `runningPlan`, `waitingPlan`, `waitingForYouPlan`, `waitingPlanWithoutDecision`, `pausingPlan`, `pausedPlan`, `pausedPlanKeepsItsOrder`, `held`, `integrityPause`, `ended`, `landing`, `epic`, plus `ActionsCatalogueTest` (the `!` key is one character, never `a`, unique in every menu in both languages).
- **Done-when (b) — terminal check on `running-work` → `control.ready`, the `a` menu of a waiting plan and of a paused plan, before and after.** Met, `order: none`. Capture set `pm-operator/src/test/resources/screen-captures/20-cli-actions-by-state.txt` (8 captures: 120×30 and 80×24, `en_US.UTF-8` and `de_DE.UTF-8`). Before, at base `4c39c0f8` (`maven-2026-10-29-160237.log`): `p Pause… / x Cancel… / h Chat… / o Settings…`, no decision. After, re-taken at the merge head `af18ec8e` (`maven-2026-10-30-153726.log`) and byte-identical to the run at `e5669363` (`maven-2026-10-30-093632.log`): `!  Decide Q-009…` first, then Pause/Cancel/Chat/Settings; German `!  Entscheide Q-009…`; the paused plan's menu is `Start / Abandon / Chat / Settings` in both. Screens in `.plan/temp/terminal/20-cli-actions-by-state/{before,after,after-merged,after-head}/`.
- **Done-when (c) — the open-issue read at the pull request head.** Met, `order: none`. Read at `af18ec8e` after CI on that head: `count_status: confirmed`, `open_issue_count: 0`, `analysis_revision` = `head_revision` = `af18ec8e`; `sonar-gate` → `merge_allowed: true`.

Per-head test runs against a pushed head: `verify -Ppre-commit` at `af18ec8e` (2449 tests, `maven-2026-10-30-152049.log`), `verify` at `af18ec8e` (2449 tests, `maven-2026-10-30-152930.log`); the spec's own check `pm-operator verify -Pintegration-tests` at `2f9d680e` (68 tests, `maven-2026-10-29-200128.log`); the capture set at `af18ec8e` (`maven-2026-10-30-153726.log`).

## Personas and skills

- Persona for D1: `plan-marshall:persona-implementer`.
- Skills named by the deliverable: `pm-dev-java:java-core`, `pm-dev-java:java-lombok`, `pm-dev-java:junit-core`. `skills added: none`.

## Hypotheses

- **Confirmed** — "`ActionMenus.plan` already puts the acts of a hold first and drops cancel/abandon where a hold ends the plan; what is missing is the waiting plan's decision entry and its catalogue text": `pm-operator/src/main/java/de/planmarshall/operator/actions/ActionMenus.java:48-65` (hold acts first, no decision entry) and `pm-api/src/main/resources/messages/operator.en.properties:455-487` (no `operator.actions.key.decide`, no `operator.actions.decide`). Both were added by this plan.

## Findings

**Self-review (six rounds; the operator authorized rounds four and five).**

- Round 1, 11 findings: fixed 1 (dispatch route fallback + state gate), 2 (client read + `waiting_for_you` tests), 3 (Javadoc), 5 (read gated), 6 (paused/pausing split), 8 (`menuDecision` lifecycle), 9 (capture header); refuted 4 (the ellipsis — corrected again in round 2); accepted 7 (the paused capture cannot prove the state gate; a unit test does), 10 (pre-existing `-` placeholder in the plan view); open 11 (Done-when c, closed by the head read).
- Round 2, 5 open, all LOW: fixed 1 (404 fallback test), 2 (German du-form `Entscheide`), 3 (the fourth ended branch), 5 (trailing ellipsis); 4 closed by the head gate runs; 7 reported.
- Round 3, 3 new LOW: fixed 2 (one key per relation), 3 (Javadoc names the fallback); carried 1 (the `decide` dispatch has no unit test).
- Round 4, 2 new LOW: fixed 2 (refused-read test), 4 (the `!` named in the key sentence); 1, 3, 5, 6 carried.
- Round 5, 2 new LOW: fixed 1 (`pausedPlan` asserts all four entries), 2 (the module integration build run at the head); 3, 4, 5, 6 carried.
- Round 6: nothing open that this change introduced and that is fixable in scope; the carried items are listed under Residue.

**CI.** Green at `2f9d680e`, `8a3cbea5` and `af18ec8e`. One earlier pull_request run (`40942362256`) had `sonar-build` fail on "Organization is not allowed to access data from non main branches" and was re-run; the newest runs were green on their own.

**PR comments.**

- CodeRabbit 09:09:02Z review, one Nitpick: "add a full-screen test for the `decide` entry" — answered in a PR comment as an accepted coverage gap (the branch re-enters the static `PmOperator.execute` under `OperatorGate.verify()`, so a unit test needs a dispatch seam; the PTY path belongs to CLI-UC-S-04 / PLAN-21).
- CodeRabbit 11:39:15Z review, one inline Minor: a silently failed decision read left the menu looking complete — fixed in `8a3cbea5` with the new catalogue text `operator.actions.decision_unreadable` (both languages and the design table), thread `PRRT_kwDOVBOtss6x8sMd` replied and resolved.
- CodeRabbit `af18ec8e` summary remark: "derive the waiting-state value from the authoritative enum" — answered in a PR comment: `de.planmarshall.api.control.WorkActions` carries `state` as a plain `String` by contract, so no enum exists at this call site; the two literals stay in one constant with the Javadoc naming their meaning and `ActionMenusTest` pinning both states and the paused/pausing cases.

## Reviewers

| Reviewer | Role | State | Head reviewed | Trigger and wait history |
|:---|:---|:---|:---|:---|
| coderabbit | required, `quota_limited: true` per resolve | reviewed | `af18ec8e` | Refused on quota eight times between 20:16:56Z and 07:12:41Z; accepted at 09:03:33Z and reviewed `2f9d680e` at 09:09:02Z; a second review at 11:39:15Z on `e5669363`; refused at 12:06:36Z; after the 90-minute wait it reviewed `8a3cbea5` at 14:04:53Z–14:09:55Z (check-run success, 0 annotations); after the merge of main it reviewed `af18ec8e` and named it in its summary marker; the 15:38Z re-trigger was answered "Already reviewed the last commit". |
| sourcery | optional, `quota_limited: true` | refused | — | Refused at 20:17:04Z on its review budget (250,000 diff characters per 7 days; next review in 3 days 16 hours). Optional, does not gate. |

**Standing review authorization (quota-limited reviewer).** Reviewer `coderabbit`; the resolve result marks it `quota_limited: true`. Head reviewed: `af18ec8e` — its summary comment carries `final_review_risk_coverage:{"sourceCommitId":"af18ec8e…","coveredCommitId":"af18ec8e…","kind":"reviewed"}` with **Merge Risk Low** and no actionable comments, published as the summary comment rather than as a formal review record (`ci pr reviews` lists the formal reviews on `2f9d680e` and `e5669363`). Current head: `af18ec8e`. Findings: the 09:09 nitpick (answered), the 11:39 inline (fixed in `8a3cbea5`, thread resolved), the `af18ec8e` summary remark (answered) — every finding handled, no thread open. Re-triggered once after the last push (15:38Z). Every other gate condition holds on `af18ec8e`: CI green, `verify` green, Sonar `merge_allowed: true`.

## Operator questions

1. "How should the run continue?" at the three-round self-review limit — options {open the PR with these reported as open, run a fourth review round, stop before the PR}; selected **"Run a fourth review round"**.
2. The same question at the four-round limit, the same options; selected **"Run a fifth review round"**.
3. "PR #273 … is at head af18ec8e … Do I enqueue #273 into the merge queue?" — options {Enqueue #273 into the merge queue, Stop before the merge}; selected **"Enqueue #273 into the merge queue"**.
4. "PR #273 was dequeued from the merge queue. The merge group failed on infrastructure, not on the change. … How should the run continue?" — options {Re-enqueue #273 now, Stop and report as blocked}; selected **"Re-enqueue #273 now"**.

Free-text operator messages: "continue" (resume after the server restart).

## Rule check

`guard: none` on this harness (unenforced) — the resolve result reported no guard, so this section reports the run's own actions rather than a guard log.

- **Shell file operations issued by the run:** two — `cat >> .plan/temp/rb-20-cli-actions-by-state/progress.md` to append the queue facts, and `rm -rf` on `.plan/temp/terminal/20-cli-actions-by-state/after-head` (the run's own capture output, to re-take it with the correct reactor). Everything else went through Read/Write/Edit and the scripts.
- **Routed builds:** five, all `in_process` — `verify -Ppre-commit`, `verify`, the capture set twice (the first with `-pl pm-operator` alone, which failed on `No tests matching pattern`, and the second with `-pl pm-api,pm-operator`) and the failed first capture attempt itself. No build was submitted to the daemon.
- **Spec read:** `spec_source: ledger_worktree`; the setup-time read carried `spec_differ: true` (the main checkout's copy differed), so this run worked from the ledger worktree's copy throughout.
- **Executor skew:** `match` — executor `0.1.2032`, lane text `0.1.2032`, read at setup in the worktree.
- **Restarts:** one — the harness server restarted while a `wait-for-comments` poll was running; the resume re-read the lane text and re-read the review surfaces, and nothing was re-run twice.

## Build server

Preflight `ready`; every build fell back to `in_process` with reason `execution_failed` — the routed build answered `execution_failed` because the daemon could not start `mvn` (empty job log). No job was killed.

## Residue

Open, all LOW, none of them a gate:

- `decide` dispatch has no unit test (the branch re-enters the static `PmOperator.execute` under `OperatorGate.verify()`); CLI-UC-S-04 / PLAN-21 owns the PTY path.
- The paused capture cannot prove the state gate; `ActionMenusTest.pausedPlan` does, and `pausingPlan`'s assertions were thin before this change and stay thin.
- The pre-existing `-` placeholder in the plan view.
- The German captures carry English runtime vocabulary (`finalize`, `phase init`), which comes from the runtime, not the terminal client.
- The capture set must build `pm-api` in the same reactor: `-pl pm-operator` alone resolves the registry's `pm-api` snapshot and renders raw message keys.
