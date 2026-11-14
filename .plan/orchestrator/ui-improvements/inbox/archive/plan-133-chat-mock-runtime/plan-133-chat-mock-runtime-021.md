envelope_version=1
sender_type=plan
sender_id=plan-133-chat-mock-runtime
epic=ui-improvements
kind=candidate-lesson
created=2026-11-13T12:27:53Z

# Candidate lesson: branch-cleanup merge gate refuses with merge_candidate_not_gated after a baseline merge

**Plan:** plan-133-chat-mock-runtime (PR #424, merged as a139ec62)
**Signal:** script-failure cluster (reported by the orchestrator of this run)
**Component:** plan-marshall:phase-6-finalize (branch-cleanup merge gate) / plan-marshall:manage-status (HEAD-bound merge authorization)
**Class:** the merge gate binds its authorization to a HEAD. Merging the moved baseline into the plan branch creates a new HEAD that contains no plan change of its own, but the gate refused it with `merge_candidate_not_gated` and demanded the review gates again.

## Records of the class

- branch-cleanup refused the merge with `merge_candidate_not_gated` after the baseline merge that resolved the CONFLICTING state (see the messages on the baseline conflict and on the stale CI wait).

## Observation

Whether this refusal is right depends on the merge: a clean baseline merge adds only reviewed `main` content, while a conflict resolution introduces new code that does need review. The gate treated both the same and did not say which case it saw.

## Candidate corrective action

On a HEAD that differs from the gated HEAD only by a merge of the base branch, the gate reports the case explicitly: re-authorize automatically when the merge was clean (tree equal to a three-way merge with no manual resolution), and name the conflict-resolved files as the review scope when it was not, instead of a bare `merge_candidate_not_gated`.
