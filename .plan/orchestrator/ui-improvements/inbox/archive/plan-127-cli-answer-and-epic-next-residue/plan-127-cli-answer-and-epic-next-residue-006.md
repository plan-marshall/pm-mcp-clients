envelope_version=1
sender_type=plan
sender_id=plan-127-cli-answer-and-epic-next-residue
epic=ui-improvements
kind=candidate-lesson
created=2026-11-13T17:14:36Z

component: plan-marshall:manage-change-ledger
category: improvement
title: After a rebase the change ledger needs remap before 5-execute can complete (execute_commits_unrecorded)

Observed (PLAN-127): after rebasing onto main, `manage-status transition --completed 5-execute` refused with
execute_commits_unrecorded until `manage-change-ledger remap` was run, with patch-id mapping plus an explicit map file
for the commits whose context shifted (patch-id changed).

Corrective action needed: document remap as a mandatory step of the rebase procedure (or run it automatically after a
rebase), and name `remap` in the execute_commits_unrecorded refusal.
