envelope_version=1
sender_type=plan
sender_id=web-brand-mark-and-waiting-mark
epic=ui-improvements
kind=landing
created=2026-11-11T17:33:57Z

```landing-facts
schema=landing-facts/1
plan_id=web-brand-mark-and-waiting-mark
epic=ui-improvements
pr=#414
merge_state=merged
cleanup_owed=false
deliverables_total=7
deliverables_done=7
total_tokens=14572367
total_wall_seconds=31535.0
harness=claude
model=claude-sonnet-5-5
effort=low
plugin_version=0.1.2090
executor_version=0.1.2090
unresolved_review_threads=unknown
review_body_findings_total=unknown
review_body_findings_handled=unknown
sonar_open_new_issues=0
step.functional-review.deliverables_met=6
step.functional-review.deliverables_met_with_residuals=1
step.functional-review.deliverables_not_met=0
step.functional-review.criteria_unevidenced=1
step.code-review.blocking_count=0
step.code-review.advisory_count=1
step.code-review.rules_cited=1
steps=finalize-step-sync-baseline:done,finalize-step-simplify:done,functional-review:done,code-review:done,architecture-refresh:done,pre-push-quality-gate:done,push:done,create-pr:done,ci-verify:done,automatic-review:done,sonar-roundtrip:done,branch-cleanup:done,lessons-capture:done,finalize-step-preference-emitter:done,record-metrics:done,finalize-step-print-phase-breakdown:done
```

## Summary

Star-badge `BrandMark`, five `--color-mascot-*` tokens, redrawn browser-tab icons, a new `WaitingMark` component, Playwright specs and an S12 screenshot spec. PR #414 was squash-merged through the merge queue as `078b2ec5`. All 7 deliverables are done; deliverable 7 was operator-accepted.

## Residue

- **Operator-accepted functional-review findings (4):** three weakened tests (BrandMark, EmptyState, favicon) and the Sonar-count criterion. Accepted by the operator at HEAD `73ef0500`; 10 test removals recorded. One finding (kind marks not rendered) was fixed as TASK-14.
- **Pre-push gate deviation:** the per-bundle arms were not re-run. The step was recorded on the whole-tree `verify -Ppre-commit` and whole-tree `test` runs at the same HEAD.
- **Code-review advisory (1, unfixed):** the header comment of `pm-web-app/src/lib/favicon.ts` still says "light-theme brand colour".
- **BrandMark caller search incomplete:** it covered the 3 planned callers, `svelte-check` and vitest only; the architecture search does not cover `.svelte` files.
- **390 px rail-head caveat** from the S12 screenshots.
- **Sonar hotspots were not counted;** only new-code issues (0, confirmed).
- **Squash message:** the repo merge-commit-message default was not set, so the queue-built squash message may carry each commit's trailer instead of exactly one.
- **Screenshots** (40 files plus 3 comparison sheets under `pm-web-e2e/test-results/s12-brand/`) are not committed. The S12 finding is in the PR body.
- **Unmeasured landing facts:** unresolved review threads and review-body findings are not measured by the lifecycle.
