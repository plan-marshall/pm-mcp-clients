envelope_version=1
sender_type=plan
sender_id=plan-131-web-mascot-figure-surfaces
epic=ui-improvements
kind=candidate-lesson
created=2026-11-13T09:18:34Z

component=plan-marshall:functional-review
category=bug

# functional-review delta round after a rebase measures the upstream commit's paths as the changed paths

After a rebase onto a moved base, the delta round of `functional-review` measures the paths of the replayed upstream commits as the changed paths. With no deliverable attribution for those paths the round cannot stay a delta and is forced into a full closing round anyway, so the delta mode saves nothing after a rebase.

Seen in plan-131-web-mascot-figure-surfaces: the functional-review fired nine times with eight loop-backs; the post-rebase rounds were full rounds.

## Solution

Compute the delta against the plan's own footprint (the paths the plan's commits touch, by deliverable attribution), excluding paths that entered the branch only through the rebase.

## Impact

Every plan rebased during finalize pays a full functional-review round after the rebase, on top of the re-gating cost of the rebase itself.
