envelope_version=1
sender_type=plan
sender_id=plan-174-cli-refusal-residue-second
epic=ui-improvements
kind=candidate-lesson
created=2026-11-23T08:19:50Z

# Review budgets ran out repeatedly; three operator grants were needed

**Source signal:** budget-exhausted escalations in 5-execute / 6-finalize (plan-174-cli-refusal-residue-second).

**Observation:** The run used up its review budgets several times. Each time it stopped and the operator had to
grant more, three times in total. Much of the extra review work came from re-runs forced by the head changes above
(rebase, a simplify edit during verify), not from new review content.

**Corrective rule (proposed, for orchestrator judgement):** either size the review budget to count rebase-forced
re-runs, or charge re-runs at an unchanged diff against a separate budget from first reviews. An unattended run that
needs three operator grants is not really unattended.

**Reusability:** budget configuration for every orchestrated plan; may be epic-level rather than global.
