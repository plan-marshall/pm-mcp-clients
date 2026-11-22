envelope_version=1
sender_type=plan
sender_id=plan-175-sonar-standing-debt
epic=ui-improvements
kind=candidate-lesson
created=2026-11-22T14:35:45Z

## Candidate lesson: no-progress reclassification on TASK-4 — one task over a very large file exhausted a whole envelope

Source: script-failure cluster `plan-marshall:phase-5-execute` (signal-scan class no_progress_reclassification, the B7 `voluntary_checkpoint -> error` marker in the decision log).

What happened: TASK-4 fixed java:S3776 (cognitive complexity) methods in `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java`, a file of more than 10,000 lines. Envelope 2 first returned blocked/context_budget_exhausted at TASK-4 step 7; the re-dispatched envelope again closed no task (in_progress=1, completed_delta=0, tokens=419037), and the no-progress check reclassified the voluntary checkpoint as an error. The operator chose a narrowed re-dispatch: finish the 7 remaining S3776 methods in small compiled batches, then the scenario tests, then the finalize steps, with an escalation if a further run closes no task. That worked.

Suggested rule: when a task's surface is one very large file with many independent findings in it, phase-4 planning should split the task by batch of methods (each batch compiled and committed on its own) rather than one task for the whole file, so an envelope always closes something.

Evidence: decision log of plan-175-sonar-standing-debt, entries d22417, a394d2, ad786c.
