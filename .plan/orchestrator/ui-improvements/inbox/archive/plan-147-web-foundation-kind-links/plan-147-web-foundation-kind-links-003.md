envelope_version=1
sender_type=plan
sender_id=plan-147-web-foundation-kind-links
epic=ui-improvements
kind=candidate-lesson
created=2026-11-12T18:55:24Z

id=2026-11-12-18-003
component=plan-marshall:phase-6-finalize
category=improvement
status=active
created=2026-11-12

# branch-cleanup with use_merge_queue=true escalates on a squash message the merge queue can never take

## What happened

Plan plan-147-web-foundation-kind-links, PR 420, `use_merge_queue=true`. With every gate clean (barrier, ADR, CI snapshot, pre-merge consent valid at c4e592c1) branch-cleanup stopped before the enqueue (2026-11-12T18:25:00Z) and returned `escalate_ask`, because the squash message (title + Summary + one trailer) could not be set: `ci pr merge-queue` has no message flag and the ci repo surface has no merge-commit-message verb. The operator answered "Enqueue with the repo default" and a third branch-cleanup firing did the merge. The merged commit dea405d8 therefore carries the repository's squash default: the PR title plus the whole PR body, including CodeRabbit's appended summary block.

## Why it matters

Under a merge queue the message can never be set by the client, so the escalation can never be answered in any way but "use the default". It costs an operator turn and a full branch-cleanup re-fire on every merge-queue plan.

## Rule

When `use_merge_queue=true`, branch-cleanup must not try to set or escalate on the squash message. The message is decided by what the PR title and body contain when the queue merges.

## Suggested fix in the component

- Treat "merge queue uses the repository squash default" as a known property, not an escalation.
- Since the body becomes the commit message, make sure the PR body is in commit-message shape before enqueue (for example, keep bot-appended summary blocks out of the body, or set the repository's squash default to title only / title + commit details), or record which body shape the repo is configured for.
