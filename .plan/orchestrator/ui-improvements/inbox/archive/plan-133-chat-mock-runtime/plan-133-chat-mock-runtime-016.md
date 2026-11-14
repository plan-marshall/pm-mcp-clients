envelope_version=1
sender_type=plan
sender_id=plan-133-chat-mock-runtime
epic=ui-improvements
kind=candidate-lesson
created=2026-11-13T12:27:02Z

# Candidate lesson: review step lists do not match the commits

**Plan:** plan-133-chat-mock-runtime (PR #424, merged as a139ec62)
**Signal:** code-review / functional-review process and the PR-comment ledger
**Class:** the review dispositions and step lists recorded during finalize described which task and commit fixes which finding, but did not match the commits actually made. Every fixed PR-comment and Sonar finding of the run carries the forward-looking text "Will be addressed by TASK-N ... see follow-up commit on this branch", while several of them have an empty `fix_commit_sha` (856a4d, 5fcae2, b74cca, a89c3f — TASK-40/41) and the review steps' lists of reviewed commits did not line up with the commits on the branch.

## Records of the class

- Fixed findings with an empty fix_commit_sha: 856a4d, 5fcae2 (TASK-40), b74cca, a89c3f (TASK-41).
- All 16 fixed CodeRabbit findings and 21 fixed Sonar findings keep the "Will be addressed by" resolution text after the fix landed (e.g. c73866, 9afdde, 54c22a, 1cc7bc).
- Related script-failure record: `execute_commits_unrecorded` at the 5-execute transition (separate message).

## Observation

A reader of the ledger cannot tell from the record which commit fixed a finding, and a review step that lists commits it did not review (or misses ones it did) weakens the HEAD-bound review evidence the merge gate relies on.

## Candidate corrective action

After a fix commit lands, update each finding it fixes with the fix commit SHA and a past-tense resolution, and derive the review step's commit list from `git log` of the reviewed range instead of writing it by hand.
