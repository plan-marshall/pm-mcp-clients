envelope_version=1
sender_type=plan
sender_id=plan-147-web-foundation-kind-links
epic=ui-improvements
kind=landing
created=2026-11-12T18:57:07Z

## What landed

plan-147-web-foundation-kind-links shipped as #420 (merged, squash commit dea405d8 through the merge queue).

```landing-facts
schema=landing-facts/1
plan_id=plan-147-web-foundation-kind-links
epic=ui-improvements
pr=#420
merge_state=merged
cleanup_owed=false
deliverables_total=8
deliverables_done=8
total_tokens=39303942
total_wall_seconds=103119.0
harness=claude
model=claude-sonnet-5-5
effort=unknown
plugin_version=0.1.2106
executor_version=0.1.2106
steps=finalize-step-sync-baseline:done,finalize-step-simplify:done,functional-review:done,code-review:done,architecture-refresh:done,pre-push-quality-gate:done,push:done,create-pr:done,ci-verify:done,automatic-review:done,sonar-roundtrip:done,branch-cleanup:done,lessons-capture:done,finalize-step-preference-emitter:done,record-metrics:done,finalize-step-print-phase-breakdown:done
unresolved_review_threads=0
review_body_findings_total=14
review_body_findings_handled=14
sonar_open_new_issues=0
step.functional-review.deliverables_met=2
step.functional-review.deliverables_met_with_residuals=6
step.functional-review.deliverables_not_met=0
step.functional-review.criteria_unevidenced=1
step.code-review.blocking_count=0
step.code-review.advisory_count=11
step.code-review.rules_cited=8
```

## Residue

- D7.C3 (the pull request records eight screenshots and the Sonar line `sonar_open_new_issues=0`) was blocking in the functional review at every head after the first push, because it can only be evidenced after the push. The operator accepted it as an override at each of five heads (fe80246, 5c99f67, afa9257, eb67093, c4e592c). At the merged head c4e592c1 the Sonar half is evidenced (confirmed 0 new-code issues, analysis revision equal to head revision) and stated in the PR body; the 20 browser screenshots stay in the plan's working area, are not attached to the PR, and were taken before the last three fix commits.
- CodeRabbit refused a re-review at the final heads for quota; its earlier review of the branch is on record and all its findings are handled. The pre-merge barrier looped back once because CodeRabbit edited its persistent summary comment (finding 4c745a), re-dispositioned as informational.
- `effort` is `unknown`: the plan carries no effort stamp. The landing lists the steps recorded before this one; `emit-landing` and `archive-plan` follow it.
- The squash commit message is the repository default (PR title plus the whole PR body, including CodeRabbit's summary block), accepted by the operator because the merge queue cannot take a per-merge message.
- Review-cycle cost: after the first push the plan went through three loop-back cycles (14 PR review findings; ExternalLink accepted `https:host/path`; Link accepted `//app.invalid/settings`), each re-running both reviews, the pre-push gate, CI, Sonar and the bot review. Seven candidate lessons went to this epic's inbox.
