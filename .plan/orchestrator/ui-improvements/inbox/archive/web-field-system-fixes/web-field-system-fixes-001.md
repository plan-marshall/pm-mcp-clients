envelope_version=1
sender_type=plan
sender_id=web-field-system-fixes
epic=ui-improvements
kind=candidate-lesson
created=2026-11-21T18:45:24Z

## Candidate: pre-merge findings gate counts the run's own squash-message PR comment

Component: plan-marshall:phase-6-finalize (branch-cleanup.md, pre-merge findings gate) / plan-marshall:automatic-review

Observation: branch-cleanup.md § "Squash-message composition" prescribes posting a squash-message limitation comment on the merge-queue path. That comment is posted from the operator account just before the pre-merge gate, and the pr-comment fetch then files it as a pending pr-comment finding. The store gate blocked the merge on the run's own comment, costing a loop-back and a triage dispatch.

Suggested correction: exempt comments the run itself authored for a prescribed purpose from the pr-comment findings gate (e.g. tag the prescribed comment with a marker the fetch skips, or record its id and auto-resolve it), or order the comment after the gate.

Reusable: yes - affects every merge-queue finalize that follows the squash-message prescription.
