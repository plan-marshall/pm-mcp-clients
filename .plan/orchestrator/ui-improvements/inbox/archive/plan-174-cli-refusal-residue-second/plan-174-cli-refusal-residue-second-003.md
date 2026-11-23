envelope_version=1
sender_type=plan
sender_id=plan-174-cli-refusal-residue-second
epic=ui-improvements
kind=candidate-lesson
created=2026-11-23T08:19:10Z

# Mutation-run failures file findings that block the 5->6 transition

**Source signal:** Q-Gate / build findings in 5-execute (plan-174-cli-refusal-residue-second).

**Observation:** The plan proved its tests by a mutation run (break the production code on purpose, show the test
fails). The build/test failures of that run were filed automatically into the findings store as `build-error` /
`test-failure` findings. These were intended failures, but they blocked the 5-execute -> 6-finalize transition until
each was resolved by hand.

**Corrective rule (proposed):** a deliberate mutation/red run either needs a way to mark its failures as expected
(not filed, or filed pre-resolved), or the workflow must state that its findings are resolved as `accepted` right
after the run restores the code. Otherwise every "prove the test fails" step costs a blocked transition.

**Reusability:** any plan that uses red-then-green or mutation evidence (common for "tests that fail" work).
