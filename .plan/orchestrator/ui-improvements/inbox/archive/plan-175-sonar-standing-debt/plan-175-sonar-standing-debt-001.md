envelope_version=1
sender_type=plan
sender_id=plan-175-sonar-standing-debt
epic=ui-improvements
kind=candidate-lesson
created=2026-11-22T14:35:11Z

## Candidate lesson: a java:S1192 constant extraction can introduce java:S1845 BLOCKERs that only the post-push Sonar read shows

Source: automatic-review signal (review-bot and Sonar findings remediated in run), PLAN-175 part 1 of 3, PR #530 (merged as f00da06e).

What happened: the plan's own java:S1192 fixes (duplicated string literals) extracted UPPER_CASE constants next to fields of the same word in lower case (for example a `project` field beside a new `PROJECT` constant). Sonar reports that pair as java:S1845 (members differing only by capitalization, BLOCKER). The post-push Sonar read of the pull request found 43 java:S1845 issues plus 1 java:S3415. The phase-5 leaves could not see them before the push, so finalize looped back to phase 5 and four fix tasks (TASK-17 to TASK-20) renamed the constants with a `_KEY` suffix (for example `PROJECT_KEY`). A code-review advisory afterwards noted that `EpicProcess.java:51` now defaults a missing project with the wire-key constant `PROJECT_KEY` rather than a default-value constant.

Suggested rule: when a refactor fixes one Sonar rule mechanically (here S1192), check before the push the rule family that fix can introduce (S1845 naming clashes, S3415 argument order) — either by naming the extracted constant with a role suffix from the start (`_KEY`, `_FIELD`) or by a pre-push Sonar analysis of the touched files. The post-push read is the first place these become visible today, which costs a finalize loop-back.

Evidence: decision log of plan-175-sonar-standing-debt (loop-back 6-finalize -> 5-execute at 2026-11-22T12:11Z, code-review advisory 99d04a).
