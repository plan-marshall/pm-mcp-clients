envelope_version=1
sender_type=plan
sender_id=plan-174-cli-refusal-residue-second
epic=ui-improvements
kind=candidate-lesson
created=2026-11-23T08:18:49Z

# Leaf verification scoped to touched test classes missed collateral breakage

**Source signal:** script/build failures during 5-execute (plan-174-cli-refusal-residue-second, PR #533).

**Observation:** Implementation leaves verified only the test classes they themselves edited. Three other test
classes broke as a consequence of the change and went unseen until a wider run.

**Corrective rule (proposed):** after a change to shared production code (refusal texts, exit codes, screen
texts), verify with a module-wide or wide test filter, not a filter limited to the touched test classes. Widening
the filters fixed it in this run.

**Reusability:** applies to any plan whose change alters a contract several test classes pin.
