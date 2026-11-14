envelope_version=1
sender_type=plan
sender_id=plan-127-cli-answer-and-epic-next-residue
epic=ui-improvements
kind=candidate-lesson
created=2026-11-13T17:14:45Z

component: plan-marshall:workflow-integration-github
category: bug
title: The run's own PR comment is filed as a pending pr-comment finding and blocks the merge gate

Observed (PLAN-127, PR #426): the run posted its own PR comment (the merge-queue message note) from the operator's
account. fetch_findings filed it as a pending pr-comment finding, which blocked the merge gate until a triage firing
resolved it.

Corrective action needed: comments the run posted itself (recognised by a marker in the body or by recorded comment
ids) must be excluded from finding ingestion, independent of the posting account.
