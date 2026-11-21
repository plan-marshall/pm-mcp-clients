envelope_version=1
sender_type=plan
sender_id=web-field-system-fixes
epic=ui-improvements
kind=candidate-lesson
created=2026-11-21T18:45:31Z

## Candidate: branch-cleanup prescribes a merge-commit-message setting the ci repo surface cannot perform

Component: plan-marshall:phase-6-finalize (branch-cleanup.md) / plan-marshall:tools-integration-ci (ci repo surface)

Observation: branch-cleanup.md prescribes setting the repository's merge-commit-message default "through the CI abstraction (ci repo surface)", but the ci repo surface exposes no verb for it. The prescription is therefore unexecutable as written. In this repository the squash default is PR_BODY, so review-bot summaries embedded in PR bodies land in the squash commits on main.

Suggested correction: either add a ci repo verb that reads and sets the squash/merge commit-message default (GitHub: squash_merge_commit_title / squash_merge_commit_message), or change branch-cleanup.md to name the actual route (operator action, or a health-check that reports the default).

Reusable: yes - every repository with a PR_BODY squash default is affected; the doc/surface mismatch applies across projects.
