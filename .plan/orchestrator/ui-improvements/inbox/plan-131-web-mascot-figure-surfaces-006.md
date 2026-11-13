envelope_version=1
sender_type=plan
sender_id=plan-131-web-mascot-figure-surfaces
epic=ui-improvements
kind=landing
created=2026-11-13T09:19:54Z

## What landed

plan-131-web-mascot-figure-surfaces shipped as #422 (merged).

```landing-facts
schema=landing-facts/1
plan_id=plan-131-web-mascot-figure-surfaces
epic=ui-improvements
pr=#422
merge_state=merged
cleanup_owed=false
deliverables_total=9
deliverables_done=9
total_tokens=38305897
total_wall_seconds=114769.0
harness=claude
model=claude-sonnet-5-5
effort=medium
plugin_version=0.1.2106
executor_version=0.1.2106
steps=finalize-step-sync-baseline:done,finalize-step-simplify:done,functional-review:done,code-review:done,architecture-refresh:done,pre-push-quality-gate:done,push:done,create-pr:done,ci-verify:done,automatic-review:done,sonar-roundtrip:done,branch-cleanup:done,lessons-capture:done,finalize-step-preference-emitter:done,record-metrics:done,finalize-step-print-phase-breakdown:done
unresolved_review_threads=unknown
review_body_findings_total=unknown
review_body_findings_handled=unknown
sonar_open_new_issues=0
step.branch-cleanup.merge_mechanism=merge_queue
step.functional-review.deliverables_met=8
step.functional-review.deliverables_met_with_residuals=1
step.functional-review.deliverables_not_met=0
step.functional-review.criteria_unevidenced=0
step.code-review.blocking_count=0
step.code-review.advisory_count=4
```

## Residue

- Merge: squash-merged as 6e8c29af2606bcef494319510f2b6b53594d46bd through the merge queue; CI green; Sonar reported 0 new-code issues (2 typescript:S7780 String.raw findings fixed in a fix commit, no dismissal). The Sonar count is the issue count of the sonar-roundtrip step; security hotspots are not counted by that step.
- The three review-gate keys (unresolved_review_threads, review_body_findings_total, review_body_findings_handled) are written unknown: no finalize step of this lifecycle run recorded them. CodeRabbit had one review on record (handled, 0 pending findings at head); its weekly/hourly quota stalled the later reviews, and one review on record is enough per the project rule.
- The steps key lists the 16 steps that had recorded an outcome when this landing was written; emit-landing and archive-plan record after it.
- Token total is a floor and partly interim: the metrics boundary rows were partly recorded from interim/approximate usage in this run, and metrics reported boundary_monotonicity for 6-finalize and a re-entered 5-execute. total_tokens population is mixed. model and effort are self-reported (the runtime-stamp refresh could not measure the model: model_unknown).
- Cost note: a rebase onto a moved origin/main (sibling plan PR 425 from PLAN-128 landed twice on this plan's base during finalize) invalidated every head-dependent gate and cost one full re-gating cycle of about 1.5 hours; functional-review's delta round after the rebase measured the upstream commit's paths as changed paths and forced a full closing round.
- Test-weakening: 6 weakened-test rows were operator-accepted (AskUserQuestion) and needed no re-acceptance once recorded in accepted_removals.
- Advisory code-review findings left open (4): duplicated reconnect machinery (UnreachableView vs ListenerOffView/RevokedView, including FIGURE_HEIGHT=180 in three views); NavRail.svelte:379 258/200 literal vs the mascotFiles.ts bust size; BrandMark.test.ts:226 glyph sweep reads binary files; +layout.svelte:236 shellGone guard untested.
- `npm run format:check` fails on 81 pre-existing files (untouched by this plan). One fix landed outside the plan's footprint: the PmConsole.java Javadoc (1 file).
- Known flaky test on main: pm-operator NeedsItemCommandsTest.everyKindNamesItsSubject failed the first CI run of PR 422 and passed on re-run; filed as a candidate-lesson with four more (review_support binary-path crash, manage-status write verbs from a leaf cwd, rebase re-gating cost, functional-review delta after rebase).
