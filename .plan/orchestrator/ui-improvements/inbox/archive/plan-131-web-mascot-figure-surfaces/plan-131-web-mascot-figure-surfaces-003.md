envelope_version=1
sender_type=plan
sender_id=plan-131-web-mascot-figure-surfaces
epic=ui-improvements
kind=candidate-lesson
created=2026-11-13T09:18:18Z

component=plan-marshall:manage-status
category=bug

# A dispatched leaf's Bash cwd reset makes manage-status write verbs fail from the main checkout

In a dispatched leaf the Bash cwd resets to the main checkout between calls. Write verbs of `manage-status` (for example `mark-step-done`) then fail with `file_not_found` / `not_visible_from_this_scope` for a plan whose directory lives in its worktree, unless `cd <worktree> &&` is in the same call. Read verbs fall back and succeed, so the failure only shows on the first write.

Seen in plan-131-web-mascot-figure-surfaces during finalize while the plan dir was worktree-resident.

## Solution

Either resolve the plan directory for write verbs the same way read verbs fall back (worktree-resident plan located through the structural probe), or document the required worktree-scoped invocation for dispatched leaves and return a message that names it instead of `file_not_found`.

## Impact

Every dispatched finalize step that records `mark-step-done` while the plan is worktree-resident is exposed, and the error text does not point at the cause.
