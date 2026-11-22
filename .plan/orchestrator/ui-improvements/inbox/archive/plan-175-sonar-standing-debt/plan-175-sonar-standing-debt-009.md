envelope_version=1
sender_type=plan
sender_id=plan-175-sonar-standing-debt
epic=ui-improvements
kind=landing
created=2026-11-22T14:38:24Z

## What landed

plan-175-sonar-standing-debt shipped as #530 (merged) — partial: deliverable 1 of 8 (pm-api production code) only.

```landing-facts
schema=landing-facts/1
plan_id=plan-175-sonar-standing-debt
epic=ui-improvements
pr=#530
merge_state=merged
cleanup_owed=false
deliverables_total=8
deliverables_done=1
total_tokens=27369718
total_wall_seconds=103635.0
harness=claude
model=claude-sonnet-5-5
effort=medium
plugin_version=0.1.2173
executor_version=0.1.2173
steps=finalize-step-sync-baseline:done,finalize-step-simplify:done,functional-review:done,code-review:done,architecture-refresh:done,pre-push-quality-gate:done,push:done,create-pr:done,ci-verify:done,automatic-review:done,sonar-roundtrip:done,branch-cleanup:done,lessons-capture:done,finalize-step-preference-emitter:done,record-metrics:done,finalize-step-print-phase-breakdown:done,emit-landing:in_progress,archive-plan:pending
unresolved_review_threads=unknown
review_body_findings_total=unknown
review_body_findings_handled=unknown
sonar_open_new_issues=0
step.code-review.blocking_count=0
step.code-review.advisory_count=1
step.code-review.rules_cited=1
step.functional-review.may_close=operator_override
step.functional-review.acceptance=operator_override
step.sonar-roundtrip.new_code_issue_count=0
step.sonar-roundtrip.count_status=confirmed
step.branch-cleanup.merge_mechanism=merge_queue
step.finalize-step-sync-baseline.action=noop
```

## Residue

- **Partial delivery.** Only deliverable 1 of 8 (pm-api production code, D1) landed in PR #530 (merge commit f00da06e3cb94e6d506156ec95b0ea09848566cb, via the merge queue). Deliverables 2 to 8 were retired from THIS plan by recorded operator decisions, not completed: PR 2 carries the pm-api tests, Awaitility and JsonTree (commits kept on the local branch `backup/plan-175-full`); PR 3 carries pm-mock-runtime, pm-web-app, pm-relay and pm-exec; deliverables 4 (pm-operator) and 8 (the final zero read on main) move to a follow-up plan after PLAN-174 lands. Tasks: 17 done, TASK-6, TASK-7 and TASK-14 `infeasible` (descoped). The plan title's "493 open issues down to zero" is NOT met by this landing.
- **Functional review closed by operator override, not by its own verifier.** `functional-review` looped back three times and was closed via `loop-back close` (D1.C3: the PR's own Sonar read accepted as checked after the push; override heads d5198e88 and 6ce6a953). No functional verdict facts (`deliverables_met` etc.) were recorded, so none are transcribed.
- **Self-introduced Sonar issues.** The Sonar read of the first push showed 44 new-code issues the plan itself introduced (43 java:S1845, 1 java:S3415); they were fixed in four fix tasks before merge. The `sonar-roundtrip` record at head 6ce6a953 reads 0 new-code issues (confirmed); per the orchestrator, the later Sonar read of PR #530 at the merged head showed 0 new-code issues and 0 security hotspots, which is the basis of `sonar_open_new_issues=0` (the lifecycle step itself does not count hotspots).
- **Review-thread counts not measured.** No finalize step records `unresolved_review_threads` / `review_body_findings_*`; `automatic-review` reports 1 comment found, 1 reviewed, 1 refused-structural. The three keys are written `unknown`, so this landing drains INCOMPLETE at them.
- **Runtime stamp.** `refresh-runtime-stamp` returned `refreshed: false` (`model_unknown`); `model`/`effort` are the start-time self-reported stamp, not a measurement.
- **Open residue on main for the follow-up plan.** java:S1075 on `RuntimeClient.STATUS_PATH` (and, after PR 2, nothing else in pm-api), plus the pm-operator items, java:S112 and java:S1313.
