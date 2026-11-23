envelope_version=1
sender_type=plan
sender_id=plan-174-cli-refusal-residue-second
epic=ui-improvements
kind=landing
created=2026-11-23T08:21:19Z

## What landed

plan-174-cli-refusal-residue-second shipped as #533 (merged via the merge queue as fcf87728 on main).

```landing-facts
schema=landing-facts/1
plan_id=plan-174-cli-refusal-residue-second
epic=ui-improvements
pr=#533
merge_state=merged
cleanup_owed=false
deliverables_total=6
deliverables_done=6
total_tokens=87005282
total_wall_seconds=222272.0
harness=claude
model=claude-opus-5-5
effort=low
plugin_version=0.1.2173
executor_version=0.1.2173
steps=finalize-step-sync-baseline:done,finalize-step-simplify:done,functional-review:done,code-review:done,architecture-refresh:done,pre-push-quality-gate:done,push:done,create-pr:done,ci-verify:done,automatic-review:done,sonar-roundtrip:done,branch-cleanup:done,lessons-capture:done,finalize-step-preference-emitter:done,record-metrics:done,finalize-step-print-phase-breakdown:done,emit-landing:done,archive-plan:pending
unresolved_review_threads=0
review_body_findings_total=3
review_body_findings_handled=3
sonar_open_new_issues=0
step.functional-review.deliverables_met=6
step.functional-review.deliverables_met_with_residuals=0
step.functional-review.deliverables_not_met=0
step.functional-review.criteria_unevidenced=0
step.code-review.blocking_count=0
step.code-review.advisory_count=4
step.code-review.rules_cited=4
step.branch-cleanup.merge_mechanism=merge_queue
```

## Residue

- Engine facts read by the client, not confirmed against pm-mcpd (doc/design/terminal-client/19-actions.adoc): (1) the 404 refusal body is a JSON object with code/message; (2) project_id/part identify a work action's target; (3) an empty-body 404 means a missing route. The empty-body 404 is tested only against the mock runtime. Confirm against pm-mcpd in pm-e2e.
- plan settings: the "value does not fit" arm keys on status 422 with code invalid_value, which ApiErrorCode does not define (the spec puts invalid_value on a 400 for another record), so against a conforming runtime the neutral line shows instead. A coded 409 settings refusal (axis_bound, team_owned, plan_ended) shows the neutral line; the refused --yes retry has no test. Settling the code belongs in the catalogue and the Runtime Model spec.
- reports read/open and answer word every 404 as "not found"; a codeless 404 is a missing route. reports read now exits 1 on a refused read (behaviour change).
- The mock runtime's own English ("today 03:00", "merged (#214)", "no change") still shows on the German PLAN-174 captures.
- LiteralTextGuardTest stays a heuristic; five edge shapes CodeRabbit named are not covered (none occurs in the scanned sources). LiteralTextGuardTest::guardedSources lost two assertions of a removed hand-kept list (operator-accepted).
- The operator granted extra review rounds three times (functional-review twice, code-review twice) and decided one rebase; the rebase onto main hit a content conflict in ReportsCommandTest that the merge-tree probes had not predicted.
