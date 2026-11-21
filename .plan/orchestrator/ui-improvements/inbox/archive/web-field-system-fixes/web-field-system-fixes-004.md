envelope_version=1
sender_type=plan
sender_id=web-field-system-fixes
epic=ui-improvements
kind=candidate-lesson
created=2026-11-21T18:45:45Z

## Candidate: loop-back admit is a manage-status verb, not a phase-6-finalize script

Component: plan-marshall:phase-6-finalize (loop-back docs) / plan-marshall:manage-status

Observation: during the pre-merge loop-back the admit call was first attempted under a phase-6-finalize script notation. The real call is `manage-status loop-back admit`. The finalize docs describing the loop-back do not point to that verb clearly enough, so the call was guessed from the surrounding text and rejected by argparse (a verb-paraphrase case).

Suggested correction: wherever phase-6-finalize prescribes a loop-back, quote the exact command in a canonical-invocation block (`python3 .plan/execute-script.py plan-marshall:manage-status:manage-status loop-back admit ...`) instead of describing it in prose.

Reusable: yes - applies to every finalize loop-back.
