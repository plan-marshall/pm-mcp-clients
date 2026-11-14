envelope_version=1
sender_type=plan
sender_id=plan-133-chat-mock-runtime
epic=ui-improvements
kind=candidate-lesson
created=2026-11-13T12:28:02Z

# Candidate lesson: execute_commits_unrecorded at the 5-execute transition

**Plan:** plan-133-chat-mock-runtime (PR #424, merged as a139ec62)
**Signal:** script-failure cluster — `plan-marshall:manage-status:manage-status` is in the signal-scan list (reported by the orchestrator of this run)
**Component:** plan-marshall:manage-status (phase transition) / plan-marshall:workflow-integration-git (commit verb)
**Class:** commits made outside the git-workflow commit verb (a direct git commit during execution) are not recorded in the plan's commit record, so the transition out of 5-execute refused with `execute_commits_unrecorded`.

## Records of the class

- The 5-execute -> 6-finalize transition refused with `execute_commits_unrecorded` because branch commits had not gone through the commit verb.

## Observation

The refusal is the guard working as designed, but the cost is a stalled transition and a manual reconciliation. The commits were legitimate; only their recording path was skipped.

## Candidate corrective action

Agents commit only through the git-workflow commit verb during execution. The transition refusal should name the unrecorded SHAs and offer the reconciliation verb that records them, so the fix is one call instead of an investigation.
