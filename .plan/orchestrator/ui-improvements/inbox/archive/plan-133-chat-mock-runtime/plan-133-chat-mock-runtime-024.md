envelope_version=1
sender_type=plan
sender_id=plan-133-chat-mock-runtime
epic=ui-improvements
kind=landing
created=2026-11-13T12:29:28Z

## What landed

plan-133-chat-mock-runtime shipped as #424 (merged, squash a139ec62 via the merge queue).

```landing-facts
schema=landing-facts/1
plan_id=plan-133-chat-mock-runtime
epic=ui-improvements
pr=#424
merge_state=merged
cleanup_owed=false
deliverables_total=8
deliverables_done=8
total_tokens=63890339
total_wall_seconds=197039.0
harness=claude
model=claude-sonnet-5-5
effort=low
plugin_version=0.1.2106
executor_version=0.1.2106
steps=finalize-step-sync-baseline:done,finalize-step-simplify:done,functional-review:done,code-review:done,architecture-refresh:done,pre-push-quality-gate:done,push:done,create-pr:done,ci-verify:done,automatic-review:done,sonar-roundtrip:done,branch-cleanup:done,lessons-capture:done,finalize-step-preference-emitter:done,record-metrics:done,finalize-step-print-phase-breakdown:done,emit-landing:done,archive-plan:done
unresolved_review_threads=0
review_body_findings_total=3
review_body_findings_handled=3
sonar_open_new_issues=0
step.functional-review.deliverables_met=5
step.functional-review.deliverables_met_with_residuals=3
step.functional-review.deliverables_not_met=0
step.functional-review.criteria_unevidenced=0
step.code-review.blocking_count=0
step.code-review.advisory_count=8
step.code-review.rules_cited=8
```

## Residue

- The merge head 87e50608 was not itself reviewed by CodeRabbit (quota spent). CodeRabbit reviewed heads f1d98258, a83dcea9 and 0e017638; every finding of those reviews was fixed or accepted, answered and resolved (16 of 16 threads), 0 pending findings, Sonar 0 open new-code issues and 0 hotspots confirmed at the merge head, CI green. This follows the operator's standing rule (one CodeRabbit review of some head suffices).
- The web client chat migration was left out of this plan and is tracked in lesson 2026-11-12-23-001 (also holds two mock-only advisories: the non-atomic play-identity check in ChatRegistry and the route parser reading the first segment named `chat`).
- The earlier head 0e017638 was pushed while the PR was CONFLICTING with main: no CI and no Sonar analysis ran on it, and the CI wait reported green from the older runs. Caught by the Sonar step; fixed by merging main into the branch (78d21a9b, one conflict in MockRuntimeEngine). See candidate-lesson messages 13 and 14.
- The code-review loop-back budget (3 rounds) was exhausted and the operator granted two more rounds; five old entry-chat tests were removed with the operator's acceptance (accepted_test_removals).
- After the baseline merge and the last fix tasks, simplify, functional-review and code-review were re-run at the merge head 87e50608 rather than waived.
