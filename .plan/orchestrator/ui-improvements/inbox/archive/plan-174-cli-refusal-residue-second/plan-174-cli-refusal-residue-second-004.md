envelope_version=1
sender_type=plan
sender_id=plan-174-cli-refusal-residue-second
epic=ui-improvements
kind=candidate-lesson
created=2026-11-23T08:19:18Z

# Leaf skipped its kind=change ledger row; 5->6 refused with execute_commits_unrecorded

**Source signal:** refused phase transition in 5-execute (plan-174-cli-refusal-residue-second).

**Observation:** One implementation leaf committed its change but did not record the matching `kind=change` row in
the change ledger. The 5-execute -> 6-finalize transition then refused with `execute_commits_unrecorded` and the
orchestrator had to repair the ledger.

**Corrective rule (proposed):** the ledger row is part of a leaf's done-criteria; a leaf's return should be checked
(or the leaf should verify) that every commit it made has its `kind=change` row before it reports success, so the gap
surfaces at the task, not at the phase transition.

**Reusability:** every plan that executes through leaves with commits.
