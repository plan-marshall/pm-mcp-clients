envelope_version=1
sender_type=plan
sender_id=plan-132-chat-contract
epic=ui-improvements
kind=landing
created=2026-11-11T18:23:32Z

## What landed

plan-132-chat-contract shipped as #409 (merged, squash commit 572dc7399fbb125e11997656f61be0f557568cf1 on main, through the merge queue).

```landing-facts
schema=landing-facts/1
plan_id=plan-132-chat-contract
epic=ui-improvements
pr=#409
merge_state=merged
cleanup_owed=false
deliverables_total=5
deliverables_done=5
total_tokens=44543886
total_wall_seconds=116491
harness=claude
model=claude-sonnet-5-5
effort=unknown
plugin_version=0.1.2090
executor_version=0.1.2090
steps=finalize-step-sync-baseline:done,finalize-step-simplify:done,functional-review:done,code-review:done,architecture-refresh:done,pre-push-quality-gate:done,push:done,create-pr:done,ci-verify:done,automatic-review:done,sonar-roundtrip:done,branch-cleanup:done,lessons-capture:done,finalize-step-preference-emitter:done,record-metrics:done,finalize-step-print-phase-breakdown:done
unresolved_review_threads=unknown
review_body_findings_total=unknown
review_body_findings_handled=unknown
sonar_open_new_issues=0
step.functional-review.deliverables_met=5
step.functional-review.deliverables_met_with_residuals=0
step.functional-review.deliverables_not_met=0
step.functional-review.criteria_unevidenced=0
step.code-review.blocking_count=0
step.code-review.advisory_count=13
step.code-review.rules_cited=4
```

## Residue

- Merged under the `barrier-ask-override` authorization (gap class `review-barrier-gap`, granted at head 5abbd760). At the final head CodeRabbit was refused for quota (awaitable, 56 minutes) and Sourcery refused for size. The operator chose "Merge anyway" on the ruling that a merge needs one CodeRabbit review of the PR, not of every later head. CodeRabbit's earlier review of the PR stands; the final head 5abbd760 itself has no bot review.
- The PR was split first: the import-order rewrite landed separately as #410, so #409 carries the chat contract alone. The split was the operator's answer to the structural refusal (size cap) of the review bots.
- SonarCloud new-code issues at the final head 5abbd760: 0 (confirmed). A Sonar fix loop-back of 34 findings preceded it; no unresolved Sonar findings remain.
- CI is green on the final head 5abbd760.
- Functional review: closed on the operator's override at c5cd818c (the design-table edit and the ProcessTreeTest split were accepted), and clean again at 5abbd760 (5 of 5 deliverables met, 8 advisory findings). Accepted test removal: ProcessTreeTest::shouldRoundTripProcessTree (15 assertions, dropped).
- Code review: clean at 5abbd760, 0 blocking and 13 advisory findings.
- `unresolved_review_threads`, `review_body_findings_total` and `review_body_findings_handled` are `unknown`: the lifecycle producer records none of them and this step did not re-read the PR to supply them. `effort` is `unknown`: the stamp carries no effort and the landing-time refresh could not measure the model (`model_unknown`).
