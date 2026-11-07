envelope_version=1
sender_type=plan
sender_id=plan-18-verify-shared-foundation
epic=ui-improvements
kind=candidate-lesson
created=2026-11-06T19:49:27Z

# Candidate lesson: operator-accepted functional-review gap re-blocks at every new head

**Source signal:** Q-Gate findings, phase 6-finalize (functional-review), plan plan-18-verify-shared-foundation (PR #349).

**Records:** 847864, c9b62e, ae76cc (accepted at head 04684919); eebb4c, b0f4ea (accepted again at head 55bac8cb, `reobserved_count: 1`).

## What happened

Deliverable 2 carried the success criterion "the result file passes `doc-review`". The `doc-review` skill lives in
`plan-marshall-documentation` and was not run on the result file; only a reduced read-only review, the AsciiDoc
validator and the link verifier ran. The functional review flagged the criterion as unevidenced
(`unevidenced_criterion at D2.C2`, `deliverable_not_met at D2`, `verdict_refused`). The operator accepted the gap at
head 04684919. Every later push produced a new head, and because the operator override is bound to the head, the
functional review re-raised the same gap (`unevidenced_criterion` plus a new `residual at D2.R1`) and needed the same
operator decision again at head 55bac8cb.

## Candidate rule(s) for the orchestrator to judge

- A plan spec should not name a success criterion that the executing repository cannot run (here: a skill that lives
  in a sibling repository). Either name the reduced check that will actually run, or mark the criterion as owed by a
  follow-up in the owning repository.
- Plan-marshall side: an operator acceptance of a gap whose evidence did not change between heads could carry forward
  (bound to the gap class and the unchanged evidence) instead of re-blocking at each head. Today the head-bound
  override costs one operator round per push.
