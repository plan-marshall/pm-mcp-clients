envelope_version=1
sender_type=plan
sender_id=plan-127-cli-answer-and-epic-next-residue
epic=ui-improvements
kind=candidate-lesson
created=2026-11-13T17:14:29Z

component: plan-marshall:manage-status
category: bug
title: loop-back close --waive-refire cannot waive after a context-only rebase (close_head_not_validated)

Observed (PLAN-127): `manage-status loop-back close --waive-refire` refused with close_head_not_validated because the
closed step was not validated at the new head. After a rebase onto main that changed only context (no change to the
plan's own diff), there is therefore no way to waive, and every review re-fires.

Corrective action needed: a documented, auditable waive path for a context-only rebase (e.g. accept the waive when the
plan's patch-ids are unchanged between the validated head and the new head), or explicit documentation that a rebase
always re-fires every review.
