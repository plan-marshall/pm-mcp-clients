envelope_version=1
sender_type=plan
sender_id=web-reports-fourth-residue
epic=ui-improvements
kind=landing
created=2026-11-10T06:50:31Z

## What landed

web-reports-fourth-residue shipped as #394 (merged) — the reports mark-all list, the terminal client, the mock and the review answers now tell the same truth.

```landing-facts
schema=landing-facts/1
plan_id=web-reports-fourth-residue
epic=ui-improvements
pr=#394
merge_state=merged
cleanup_owed=false
deliverables_total=9
deliverables_done=9
total_tokens=8653549
total_wall_seconds=62853
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2090
steps=finalize-step-sync-baseline:done,finalize-step-simplify:done,functional-review:done,code-review:done,architecture-refresh:done,pre-push-quality-gate:done,push:done,create-pr:done,ci-verify:done,automatic-review:done,sonar-roundtrip:done,branch-cleanup:done,lessons-capture:done,finalize-step-preference-emitter:done,record-metrics:done,finalize-step-print-phase-breakdown:done,emit-landing:done,archive-plan:pending
unresolved_review_threads=0
review_body_findings_total=4
review_body_findings_handled=4
sonar_open_new_issues=0
step.functional-review.deliverables_met=9
step.functional-review.deliverables_met_with_residuals=0
step.functional-review.deliverables_not_met=0
step.functional-review.criteria_unevidenced=0
step.code-review.blocking_count=0
step.code-review.advisory_count=3
step.code-review.rules_cited=3
step.branch-cleanup.merge_state=merged
step.branch-cleanup.cleanup_owed=false
step.branch-cleanup.merge_mechanism=merge_queue
```

## Residue

- `plugin_version` is `unknown`: the plan's status stamp carries `executor_version` but no `bundle_version` field.
- The functional review was closed by an operator override at head `39c540e8` on the three weakened-test removals the plan's own D1/D3e/D7/D8 rewrites produced (recorded in `accepted_test_removals`); the later heads re-ran the review to a clean full round.
- CodeRabbit reviewed head `46f85869` (its review-body nitpicks are all handled); later heads were `skipping` because the run exhausted the included review allowance, which the merge rule permits ("later heads need no review").
- `pm-web-e2e` is not part of the Maven reactor: the 375-test Playwright suite was run locally against the assigned ports (21070/21071) and passed; CI covers only the ports guard.
