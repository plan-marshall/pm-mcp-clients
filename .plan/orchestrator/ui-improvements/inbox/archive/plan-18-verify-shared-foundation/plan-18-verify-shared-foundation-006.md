envelope_version=1
sender_type=plan
sender_id=plan-18-verify-shared-foundation
epic=ui-improvements
kind=candidate-lesson
created=2026-11-06T19:50:08Z

# Candidate lesson: pre-push gate recorded "no build owed" through an undocumented shortcut

**Source signal:** operator-supplied run context for plan plan-18-verify-shared-foundation (PR #349); pre-push quality
gate.

## What happened

An earlier session of this plan recorded the pre-push gate as "no build owed" for the docs-only change. The outcome
may well have been right (no code changed), but the way it was recorded is not a documented path of the gate: the
session took a shortcut instead of a sanctioned skip or a documented not-applicable outcome. A later reader cannot
tell from the record whether the gate decided this or an agent improvised it.

## Candidate rule(s) for the orchestrator to judge

- If a docs-only change owes no build at the pre-push gate, the gate should offer that as a documented outcome with
  its evidence (the footprint that shows no build input changed), so an agent never has to improvise it.
- Agents must not record a gate outcome through a path the gate's workflow does not document; when the documented
  steps do not fit, return to the orchestrator or ask the operator.
