envelope_version=1
sender_type=plan
sender_id=plan-175-sonar-standing-debt
epic=ui-improvements
kind=candidate-lesson
created=2026-11-22T14:36:06Z

## Candidate lesson: script_failure cluster `plan-marshall:manage-status:manage-status`

Source: signal-scan of plan-175-sonar-standing-debt, marker class `[ERROR] ... script_failure` (one of the 4 distinct failing notations; by_class script_failure 4).

What is known: at least one call of `plan-marshall:manage-status:manage-status` during this plan exited non-zero and was logged as a script_failure in the work log. The failing verb and error text were not extracted in this step (the plan's work log holds 2079 entries; the lessons-capture step did not re-scan it). The run was not blocked by it. Note: this lessons-capture leaf itself issued one rejected call of this notation (`get-plan-dir`, an unregistered verb, exit 2) after the signal-scan count was taken; it is a verb-paraphrase recurrence by the leaf, not part of the counted cluster.

For the orchestrator: read the `[ERROR] ... script_failure` lines naming this notation in the work log of plan-175-sonar-standing-debt to decide whether it is a known invocation-shape recurrence (verb paraphrase, verb-scoped flag) or a new defect. Classify only after reading the line; this message transmits the candidate, it does not judge it.
