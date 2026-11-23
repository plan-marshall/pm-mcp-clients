envelope_version=1
sender_type=plan
sender_id=plan-174-cli-refusal-residue-second
epic=ui-improvements
kind=candidate-lesson
created=2026-11-23T08:19:35Z

# Conflict probes go stale when main moves between probe and rebase

**Source signal:** rebase conflict in 6-finalize branch-cleanup (plan-174-cli-refusal-residue-second).

**Observation:** The `merge-tree` / baseline-reconcile probes reported no content conflict, but the actual rebase
conflicted in `ReportsCommandTest`. `main` had moved (another plan merged) between the probe and the rebase, so the
probe answered for a base that no longer existed.

**Corrective rule (proposed):** a conflict probe's verdict is only valid for the base SHA it probed. Record that SHA
with the verdict and re-probe (or treat the verdict as void) when `origin/main` has moved before the rebase; never
read "no conflict" as a promise about the rebase that follows later.

**Reusability:** every finalize run under concurrent merges (N parallel plans on one epic).
