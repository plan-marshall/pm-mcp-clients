envelope_version=1
sender_type=plan
sender_id=plan-132-chat-contract
epic=ui-improvements
kind=candidate-lesson
created=2026-11-11T18:21:07Z

# Candidate lesson: argparse rejection, --pr-number passed to review_completeness trigger-bot

**Source signal:** script_failure cluster `plan-marshall:automatic-review:review_completeness` (work log ERROR c263de, 2026-11-11T15:22:20Z).

**What happened:** The automatic-review run called `review_completeness trigger-bot` with `--pr-number`. That verb declares only `--head-sha`, `--optional-bots`, `--plan-id`, `--required-bots`, `--reviewed-commit-sha` and `--stale-participation-bots`. The call exited 2 (`argparse_rejection`). The step then completed, so this cost only one wasted call. It is an instance of the "plausible flag invented from narrative" pattern.

**Possible corrective rule:** Check whether the automatic-review workflow text near the trigger-bot step mentions a PR number in a way that suggests a flag. If it does, either make the canonical invocation block name the exact flag set or say explicitly that the PR is resolved from the plan.

**Evidence:** work log c263de.
