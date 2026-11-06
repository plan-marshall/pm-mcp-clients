envelope_version=1
sender_type=plan
sender_id=plan-18-verify-shared-foundation
epic=ui-improvements
kind=candidate-lesson
created=2026-11-06T19:49:50Z

# Candidate lesson: CodeRabbit hourly quota forced two operator escalations; barrier grant refused an earlier answer

**Source signal:** automatic-review (signal count 1), plan plan-18-verify-shared-foundation (PR #349). Six CodeRabbit
pr-comment findings were filed and triaged (e.g. 2308e5, fixed by TASK-5: the `:docs:` attribute now points at the
verified use-case commit be5750dd instead of `main`).

## What happened

CodeRabbit allows one review per hour. Each fix round produced a new head, and the re-review could not come within the
quota window:

1. The re-review wait timed out and needed an operator decision.
2. The pre-merge barrier then asked again. The operator had already answered the re-review question, but the barrier's
   grant refused that earlier answer because it had been given to a different question (re-review timeout versus
   pre-merge barrier). The operator had to decide a second time for the same underlying fact.

The operator's standing rule (project memory) is that a merge needs one CodeRabbit review, not one on the last head;
later unreviewed heads are acceptable, re-trigger once, never wait.

## Candidate rule(s) for the orchestrator to judge

- The pre-merge barrier could read "at least one completed CodeRabbit review on this PR" as satisfied without asking,
  when the project's policy says so, instead of escalating per head.
- When a re-review timeout and the pre-merge barrier ask about the same fact (no review on the current head), one
  operator answer should satisfy both, or the first prompt should be phrased so its answer is the grant the barrier
  needs.
