envelope_version=1
sender_type=plan
sender_id=web-field-system-fixes
epic=ui-improvements
kind=landing
created=2026-11-21T18:47:08Z

## What landed

web-field-system-fixes (PLAN-173) shipped as #525 (merged, merge queue, 88cc8cb3).

```landing-facts
schema=landing-facts/1
plan_id=web-field-system-fixes
epic=ui-improvements
pr=#525
merge_state=merged
cleanup_owed=false
deliverables_total=6
deliverables_done=6
total_tokens=39630413
total_wall_seconds=87045.0
harness=claude
model=claude-opus-5-5
effort=low
plugin_version=0.1.2173
executor_version=0.1.2173
steps=finalize-step-sync-baseline:done,finalize-step-simplify:done,functional-review:done,code-review:done,architecture-refresh:done,pre-push-quality-gate:done,push:done,create-pr:done,ci-verify:done,automatic-review:done,sonar-roundtrip:done,branch-cleanup:done,lessons-capture:done,finalize-step-preference-emitter:done,record-metrics:done,finalize-step-print-phase-breakdown:done,emit-landing:done,archive-plan:pending
unresolved_review_threads=0
review_body_findings_total=1
review_body_findings_handled=1
sonar_open_new_issues=0
step.functional-review.deliverables_met=4
step.functional-review.deliverables_met_with_residuals=2
step.functional-review.deliverables_not_met=0
step.functional-review.criteria_unevidenced=0
step.code-review.blocking_count=0
step.code-review.advisory_count=6
step.code-review.rules_cited=5
```

## Residue

- Accepted advisory residue (PR body follow-ups): a composition ended by mouse/blur skips the next Enter once; a confirmed leave replays popstate as a goto push; the field-contract describe block is repeated in seven test files; DemoFormFixture's IME wiring and overlapping-saves guard are not test-pinned; PlanViewCatalogueTest's required-key list lacks plan.activity.kind.egress; dev-page sample literals untranslated in de (pre-existing); the column-engine fixture side column overlays /dev/components at 390 px (pre-existing).
- CodeRabbit comment 20fb6d (egress label strings are never read: ActivityEntryKind has no EGRESS constant) was taken into account as a follow-up for the change that owns the egress entry.
- Scope deviation accepted by the operator: plan_view.{en,de}.properties gained plan.activity.kind.egress because main was red after #522.
- The squash commit on main carries the PR body (repo squash default PR_BODY; the merge queue has no per-enqueue message override), including the CodeRabbit summary; the repo default was left unchanged as an operator decision.
- Code review took 9 firings (7 loop-back rounds, 4 operator-granted); Sonar 0 issues / 0 hotspots at the merged head; Sourcery (optional) was on hard quota throughout.
- Sonar count here covers issues and hotspots (read_open_issues at the head: 0 and 0).
