envelope_version=1
sender_type=plan
sender_id=plan-133-chat-mock-runtime
epic=ui-improvements
kind=candidate-lesson
created=2026-11-13T12:25:12Z

# Candidate lesson: unbounded waits on an event stream in tests

**Plan:** plan-133-chat-mock-runtime (PR #424, merged as a139ec62)
**Signal:** automated review (CodeRabbit)
**Class:** a test reads events from a stream the runtime keeps open (SSE) in a loop whose only exit is "N events seen". When the expected event never comes, the blocking read hangs until the stream closes, so the test reports nothing instead of failing promptly, and the build stalls.

## Records of the class

- 7b9ddb (DecisionsScenarioTest.java:333): the wait for `decision.answered` called `sse.next()` with no timeout; the 20-event guard never runs if no event arrives. Fixed in TASK-36 with a timeout that closes the exchange.

## Observation

This plan added timing modes to the mock, which makes "the event did not come" a real outcome of a regression. A loop counter is not a bound when the read inside it blocks.

## Candidate corrective action

Every read from an open event stream in a test is bounded by a timeout that closes the exchange (and restores the interrupt flag if interrupted), so a missing event fails the test with a message instead of hanging it.
