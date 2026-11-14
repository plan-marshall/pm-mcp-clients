envelope_version=1
sender_type=plan
sender_id=plan-127-cli-answer-and-epic-next-residue
epic=ui-improvements
kind=candidate-lesson
created=2026-11-13T17:14:43Z

component: plan-marshall:automatic-review
category: bug
title: Merge gate refuses (merge_participation_unproven) on an optional bot that refused structurally

Observed (PLAN-127, PR #426): the merge gate refused with merge_participation_unproven because an OPTIONAL bot
(sourcery, refused_structural on PR size) counted as unproven, although the roster names only coderabbit as required.
Only an operator-recorded barrier-ask-override grant over review-barrier-gap let the enqueue through.

Corrective action needed: participation proof should be required for roster-required bots only; an optional bot's
structural refusal should be recorded as a known non-participation, not as an unproven gap.
