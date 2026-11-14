envelope_version=1
sender_type=plan
sender_id=plan-133-chat-mock-runtime
epic=ui-improvements
kind=candidate-lesson
created=2026-11-13T12:26:08Z

# Candidate lesson: a success criterion that cannot be evidenced before the PR exists

**Plan:** plan-133-chat-mock-runtime (PR #424, merged as a139ec62)
**Signal:** functional-review Q-Gate (6-finalize)
**Class:** the outline gave deliverable 8 a success criterion "open Sonar issue count at the pull request head is recorded (0 or every issue dismissed)". The functional review runs before the push, where no PR and no Sonar analysis of the head exist, so the criterion is structurally unevidenced at every pre-push review and needs operator acceptance each time.

## Records of the class

- 841960 (unevidenced_criterion D8.C2) and 018cb6 (residual D8.R1): first review round; operator-accepted at 12:46.
- 4e6174 (unevidenced_criterion D8.C2, reobserved 3 times) and 48f73b (residual D8.R1, reobserved 3 times): later rounds; operator-accepted again at 19:01 via an AskUserQuestion answer.
- 2339ea (verdict_refused): D8.C1 cited CI runs that predated the head, part of the same pre-push evidence gap.

## Observation

The same unprovable criterion was raised and accepted in each functional-review round, costing an operator interaction each time. The sonar-roundtrip step after the push is what actually reads the count.

## Candidate corrective action

At outline time, place post-push facts (Sonar count at the head, CI status of the head, review-bot state) as criteria of the post-push finalize steps rather than of a deliverable the pre-push functional review grades — or let the functional review classify such criteria as deferred-to-post-push instead of unevidenced.
