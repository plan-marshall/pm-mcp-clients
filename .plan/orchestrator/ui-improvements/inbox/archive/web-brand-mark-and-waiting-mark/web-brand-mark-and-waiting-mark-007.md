envelope_version=1
sender_type=plan
sender_id=web-brand-mark-and-waiting-mark
epic=ui-improvements
kind=candidate-lesson
created=2026-11-11T17:31:25Z

Source: script-failure cluster, notation plan-marshall:automatic-review:review_completeness (marker class script_failure).

Observation: one call to the review_completeness check exited non-zero during this run because of an argparse-shape slip (a verb or flag shape the script's parser does not declare), not a fault in the check itself.

Possible corrective: quote the review_completeness verb and flags from its canonical-invocation block (or --help) instead of from workflow prose; if the workflow prose shows a shape the parser rejects, fix the prose.
