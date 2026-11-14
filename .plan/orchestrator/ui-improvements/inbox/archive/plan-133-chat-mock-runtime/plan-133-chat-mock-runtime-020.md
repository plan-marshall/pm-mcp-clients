envelope_version=1
sender_type=plan
sender_id=plan-133-chat-mock-runtime
epic=ui-improvements
kind=candidate-lesson
created=2026-11-13T12:27:43Z

# Candidate lesson: review_support validate rejects evidence strings with nested class segments

**Plan:** plan-133-chat-mock-runtime (PR #424, merged as a139ec62)
**Signal:** script-failure cluster — notation `plan-marshall:phase-6-finalize:review_support` (in the signal-scan list)
**Component:** plan-marshall:phase-6-finalize (review_support validate)
**Class:** the evidence-string validator of the review verdicts did not accept Java test references with nested class segments — the shape JUnit 5 `@Nested` tests produce, e.g. `StartContractTest.ChatAndFollowUp::followUpPlan` or `StartScenarioTest.Chat::abandonDeletesTranscript` (both appear in this run's functional-review findings). Valid evidence was refused, and the verdict had to be rewritten to fit the parser.

## Records of the class

- review_support validate failures on evidence strings citing `Outer.Inner::method` test locations during the functional/code review rounds.

## Observation

This repository's code standards require `@Nested` test classes, so nested segments are the normal case here, not an edge case.

## Candidate corrective action

The evidence-reference grammar accepts any number of dotted class segments before `::method` (and `$`-separated binary names), with a test over `A::m`, `A.B::m` and `A.B.C::m`.
