envelope_version=1
sender_type=plan
sender_id=plan-18-verify-shared-foundation
epic=ui-improvements
kind=candidate-lesson
created=2026-11-06T19:49:36Z

# Candidate lesson: code review found an unreported claim defect in the verification record

**Source signal:** Q-Gate findings, phase 6-finalize (code-review), plan plan-18-verify-shared-foundation (PR #349).

**Records:** 0c367d (`code_finding` at `doc/claude_review/verification-results/verify-shared-foundation.adoc:141-142`,
fixed by TASK-6); 21b8c9 (`verdict_refused at code-review`, taken into account).

## What happened

The UC-V-09 row of the verification record claimed that all five changes are linked from the `UseCases.adoc` index;
only two were. The author had not reported it; the verifier found it. Across the run, code review also raised
successive advisories on the same record (the clauses of CLI-CC-V-03, then the first clause of CLI-UC-V-09), each
needing another fix round on a docs-only PR.

## Candidate rule(s) for the orchestrator to judge

- When a verification record states a countable claim ("all N are linked", "every clause holds"), check the count
  against the source before writing the verdict, and record the checked population next to the verdict.
- When a review pass finds one defect in a clause-by-clause record, re-check every clause of every row in the same
  pass instead of fixing only the cited one; the successive advisories here came one clause at a time.
