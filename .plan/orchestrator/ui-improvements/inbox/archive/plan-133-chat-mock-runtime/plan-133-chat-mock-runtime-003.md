envelope_version=1
sender_type=plan
sender_id=plan-133-chat-mock-runtime
epic=ui-improvements
kind=candidate-lesson
created=2026-11-13T12:24:42Z

# Candidate lesson: Sonar size limits (complexity, assertions, parameters) caught only at the PR

**Plan:** plan-133-chat-mock-runtime (PR #424, merged as a139ec62)
**Signal:** automated review (SonarCloud)
**Class:** methods and tests written past Sonar's structural limits — cognitive complexity 15 (java:S3776), fewer than 25 assertions per test (java:S5961), at most 7 parameters (java:S107). Nothing in the local build measures these, so they surface only in the first Sonar analysis of the PR and force a refactor round after review.

## Records of the class

- 54c22a: ChatRegistry.playBroadcast cognitive complexity 25 of 15 (TASK-26 split it).
- 61a7af: MockRuntimeEngine.dispatch cognitive complexity 16 of 15 (TASK-28 split it).
- 1cc7bc: ChatScenarioTest.opensReadsTurnsAndEnds 40 assertions (TASK-35 split it); CodeRabbit repeated it as a nitpick in review body 200226.
- 4479b8: ChatReplyEventsTest 31 assertions (TASK-35 split it).
- 9512b5: ChatRegistry.Chat constructor with 8 parameters (TASK-26).

## Observation

All five were written in this plan's implementation tasks and reached the PR intact; the in-run code review did not flag them. The splits were mechanical but each cost a commit, a push and a new review cycle.

## Candidate corrective action

Write new methods within cognitive complexity 15, tests under 25 assertions and constructors with at most 7 parameters from the start (split a scenario test by route, turn and end; use a record or builder for wide constructors), and have the pre-push code review check these three limits explicitly.
