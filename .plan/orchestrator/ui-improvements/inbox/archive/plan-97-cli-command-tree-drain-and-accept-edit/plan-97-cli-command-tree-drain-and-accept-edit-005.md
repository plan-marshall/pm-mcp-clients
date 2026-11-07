envelope_version=1
sender_type=plan
sender_id=plan-97-cli-command-tree-drain-and-accept-edit
epic=ui-improvements
kind=candidate-lesson
created=2026-11-07T14:48:11Z

## Candidate lesson: squash merge with PR_BODY as commit message lands the review bot's summary in the commit

- **Source**: PLAN-97 (plan-97-cli-command-tree-drain-and-accept-edit), PR #358, merge (merge commit d8efe35f).
- **Suggested component**: `plan-marshall:tools-integration-ci` (merge path) / `plan-marshall:phase-6-finalize`.
- **Suggested category**: bug.

### What happened

- The repository's squash default uses `PR_BODY` as the squash commit message. CodeRabbit writes its generated summary into the PR body, so a plain squash merge would put that bot summary into the landed commit message on `main`.
- It was handled by temporarily setting the PR body to the intended commit message before the merge.

### Rule worth considering

- The merge path should pass an explicit commit body (built from the plan's own summary) instead of relying on the repository default, or detect `squash_merge_commit_message: PR_BODY` and warn when the body carries bot-generated sections.
- Until fixed: before merging on such a repository, check the PR body; set it to the intended commit message if a bot has added content.
