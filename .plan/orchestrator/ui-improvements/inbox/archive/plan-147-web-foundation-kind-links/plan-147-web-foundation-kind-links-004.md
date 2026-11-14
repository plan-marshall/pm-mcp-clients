envelope_version=1
sender_type=plan
sender_id=plan-147-web-foundation-kind-links
epic=ui-improvements
kind=candidate-lesson
created=2026-11-12T18:55:26Z

id=2026-11-12-18-004
component=plan-marshall:phase-6-finalize
category=bug
status=active
created=2026-11-12

# branch-cleanup run as a leaf cannot remove the worktree: the leaf's cwd is inside the removal target

## What happened

Plan plan-147-web-foundation-kind-links. After PR 420 was merged by the merge queue (dea405d8), branch-cleanup, dispatched as a leaf, pulled the main checkout and released the merge lock, but logged (2026-11-12T18:50:11Z):

```
worktree removal and local branch prune NOT performed in this dispatch because the leaf cwd
is inside the worktree (worktree-remove refuses cwd_inside_removal_target, not forceable)
and the plan dir is still in the worktree
```

The worktree removal, the branch prune and moving the plan directory back to the main checkout had to be done afterwards by the main context.

## Why it is wrong

A dispatched leaf inherits the cwd pinned to the plan's worktree, so the worktree-removal sub-step of branch-cleanup can never succeed when branch-cleanup runs as a leaf. The guard `cwd_inside_removal_target` is correct; the dispatch shape is what makes the step's own last action impossible.

## Rule

The post-merge worktree removal (and the plan-dir move back to the main checkout) belongs to the main-context orchestrator, after the leaf returns, with the orchestrator's cwd on the main checkout. A branch-cleanup leaf should return an explicit `worktree_removal_owed` signal rather than a WARNING.

## Suggested fix in the component

Split the post-merge tail: the leaf merges and returns; the orchestrator re-pins cwd to the main checkout, moves the plan dir back and runs worktree-remove plus branch prune as a documented step, so it is never silently left undone.
