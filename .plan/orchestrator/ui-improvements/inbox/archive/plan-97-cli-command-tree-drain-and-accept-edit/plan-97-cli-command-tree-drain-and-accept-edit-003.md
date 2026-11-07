envelope_version=1
sender_type=plan
sender_id=plan-97-cli-command-tree-drain-and-accept-edit
epic=ui-improvements
kind=candidate-lesson
created=2026-11-07T14:47:54Z

## Candidate lesson: the queue_wait forbidden reroute does not model a repository with a required merge queue

- **Source**: PLAN-97 (plan-97-cli-command-tree-drain-and-accept-edit), PR #358, finalize merge.
- **Suggested component**: `plan-marshall:tools-integration-ci` (`pr safe-merge`) and `plan-marshall:phase-6-finalize` (`record-merge`, conflict gate routing).
- **Suggested category**: bug.

### What happened (two symptoms, one root)

1. The conflict gate forced the safe-merge route (`queue_wait: forbidden`), but `pr safe-merge` refused because `main` has a required merge queue: a direct merge is not possible there.
2. The actual way through was: rebase first, then let the merge queue merge. `record-merge` then refused `action` on the merge-queue route, although that is exactly the sequence the reroute had produced. The honest record needed `mark-step-done` directly, bypassing `record-merge`.

### Rule worth considering

- When the repository has a required merge queue, the `queue_wait: forbidden` reroute should become "rebase, then enqueue", not "safe-merge" (which cannot succeed), and `record-merge` should accept the rebase-then-queue sequence as a valid merge-queue action.
- Until fixed: on a merge-queue repository, expect safe-merge to refuse; rebase, enqueue, and record the step with `mark-step-done` stating the route taken.
