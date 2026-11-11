envelope_version=1
sender_type=plan
sender_id=web-brand-mark-and-waiting-mark
epic=ui-improvements
kind=candidate-lesson
created=2026-11-11T17:31:08Z

Source: 6-finalize functional-review, unevidenced_criterion at D7.C3 (finding 1dcb74) and verdict_refused (finding 090b6b), both accepted by operator close.

Observation: a spec criterion asked that the PR record the open Sonar issue count at its head. Functional review runs before create-pr and sonar-roundtrip, so the count can never be evidenced at review time; the review kept the criterion unevidenced, the verifier refused the met_with_residuals grading, and the loop ended only when the operator accepted the criterion.

Possible corrective: do not write a deliverable criterion whose evidence exists only after the PR is created (Sonar count in the PR body, CI result, review outcome); state it as a landing obligation of the finalize steps instead, so functional review does not loop on it.
