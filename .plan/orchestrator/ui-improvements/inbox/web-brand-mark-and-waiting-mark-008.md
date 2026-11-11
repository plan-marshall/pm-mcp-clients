envelope_version=1
sender_type=plan
sender_id=web-brand-mark-and-waiting-mark
epic=ui-improvements
kind=candidate-lesson
created=2026-11-11T17:31:32Z

Source: script-failure cluster, notation plan-marshall:manage-findings:manage-findings (marker class script_failure).

Observation: one manage-findings call exited non-zero during this run because of an argparse-shape slip (a verb or flag shape the parser does not declare, e.g. a missing --phase or --resolution vs --status), not a fault in the script.

Possible corrective: quote manage-findings verbs and flags from its canonical-invocation block (or --help, e.g. qgate list requires --phase) instead of from workflow prose.
