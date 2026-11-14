envelope_version=1
sender_type=plan
sender_id=plan-133-chat-mock-runtime
epic=ui-improvements
kind=candidate-lesson
created=2026-11-13T12:24:31Z

# Candidate lesson: Sonar S4449 nullability recurs after a fix

**Plan:** plan-133-chat-mock-runtime (PR #424, merged as a139ec62)
**Signal:** automated review (SonarCloud)
**Class:** a null literal or a nullable value reaches a parameter not declared `@Nullable` (java:S4449). The fix of the first round left the same pattern in other call sites, so the rule fired again in the next round.

## Records of the class

- bbeba1 (round 1, ChatRegistry.java:915): constructor argument could be null; fixed in TASK-26 with one factory for the operator turn that passes no null literal.
- 5fcae2 (round 2, ScenarioLoader.java:691): the nullable state and field of `ScenarioValidationException`; fixed in TASK-40 with JSpecify `@Nullable`.
- b74cca (round 2, ChatRegistry.java:961): `operatorTurn` called with a possibly null text; fixed in TASK-41 with an explicitly guarded argument. This is the very factory introduced by the round-1 fix.

## Observation

The modules are `@NullMarked`, and the compiler with `failOnWarning` does not catch this; only Sonar does, and only at the PR. A round-1 fix that adds a new method moved the finding instead of removing it, costing an extra review round.

## Candidate corrective action

When fixing an S4449 finding, check every call site of the changed or new method for possibly null arguments in the same change, and annotate the true nullability (JSpecify `@Nullable`) rather than re-routing the null through a new helper.
