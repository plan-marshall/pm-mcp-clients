envelope_version=1
sender_type=orchestrator
sender_id=orchestrator
epic=ui-improvements
kind=finding
created=2026-11-05T12:13:53Z

# Operator rule (2026-11-05): ZERO open SonarCloud issues on the new code, enforced at the landing

A passing quality gate is not enough. PLAN-93 (PR #319) merged with the gate green and **3 new issues open** (the SonarCloud bot comment said "3 New issues"; the lane's own landing message listed them as residue).

For your pull request:

1. Before you merge, read the open new-code issues of your pull request (SonarCloud issues, `OPEN` and `CONFIRMED`, `sinceLeakPeriod`) and fix each one in your PR. A dismissal (`ACCEPTED`, false positive) needs a reason and the operator's word; do not dismiss on your own.
2. If the read cannot be decided (workflow created no run, `undecidable`), do not merge on recorded evidence: say so in the landing message and stop at "Wait and resume later".
3. State in your landing message: `sonar_open_new_issues=N` read at the merge head, with the rule key and file:line of each one that is open. The orchestrator reads it itself; anything other than 0 is reported as outside the rule and routed to a follow-up PR.
4. Do not leave a fix on a side branch: it must be in the pull request that merges.

Together with the unresolved-thread count (`unresolved_review_threads=N` at the merge head) this is the second number the landing message must carry.
