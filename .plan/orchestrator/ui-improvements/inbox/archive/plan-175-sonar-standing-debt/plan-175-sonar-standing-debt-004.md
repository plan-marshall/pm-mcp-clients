envelope_version=1
sender_type=plan
sender_id=plan-175-sonar-standing-debt
epic=ui-improvements
kind=candidate-lesson
created=2026-11-22T14:35:37Z

## Candidate lesson: build-daemon queueing behind other plans' builds made phase-5 leaves hand back on a running compile

Source: script-failure cluster context and the phase-5 decision records of PLAN-175.

What happened: the build daemon queued this plan's builds for 5 to 15 minutes behind other plans' builds. Three phase-5 leaves handed back while their compile was still running (for example TASK-18: compile job ca6bcc22... queued 300 s, TASK-18/19/20 edits complete on disk but uncommitted, last step of each left open). The orchestrator closed the owed tasks with the verification-owed hold and re-dispatched after a green pm-api build. One such hand-back was counted as a `verification_handback` boundary (prescribed, not lesson-bearing on its own); the cost was the extra re-dispatches and the uncommitted state between them.

Suggested direction (for the orchestrator to judge): with several plans running in parallel on one daemon, a per-task compile can wait longer than the leaf's synchronous budget. Options are to batch the per-task compiles of consecutive tasks on one module into one build (as was done for module-tests, entry 551d3e), or to let the leaf commit its completed edits before handing back so the hand-back leaves no uncommitted state.

Evidence: decision log of plan-175-sonar-standing-debt, entries 3d3240 (TASK-18 hand-back) and 551d3e (module-tests deferred to the last task); signal-scan boundary_cause_counts verification_handback 1.
