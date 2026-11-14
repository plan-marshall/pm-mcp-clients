envelope_version=1
sender_type=plan
sender_id=plan-147-web-foundation-kind-links
epic=ui-improvements
kind=candidate-lesson
created=2026-11-12T18:55:23Z

id=2026-11-12-18-002
component=plan-marshall:phase-6-finalize
category=improvement
status=active
created=2026-11-12

# Functional review blocks at every head on a criterion that can only be evidenced after the push (PR screenshots, sonar_open_new_issues line)

## What happened

Plan plan-147-web-foundation-kind-links. Deliverable 7 carried criterion D7.C3: the pull request body must show the screenshots and the line `sonar_open_new_issues=N`. Neither can exist before the push (no PR, no Sonar analysis at a PR head; TASK-18 was logged infeasible with `no_pr_found`). The functional review runs before the push, so it graded D7.C3 as blocking at every head:

- fe80246 (09:40), 5c99f67 (10:03), afa9257 (12:47), eb67093 (15:06), c4e592c (17:04)

Each time `may_close=false` and the round needed a fresh operator override close naming the same finding. The evidence was in fact produced later by create-pr and sonar-roundtrip.

## Rule

A success criterion whose evidence only exists after the push (PR body content, Sonar read at the PR head, CI result, bot review) must not be graded as a blocking pre-push finding. Either:

- the plan author places such evidence under the finalize steps that produce it (create-pr, sonar-roundtrip) instead of a deliverable success criterion, or
- the functional review classifies it as `owed_after_push` (deferred to a named post-push step) rather than blocking, and verifies it once at the landing.

## Suggested fix in the component

Give the functional-review rubric a deferred-evidence category keyed to the post-push step that will produce it, and let phase-4 planning flag criteria that reference the PR body or the Sonar PR read so they are routed there. One operator close per head is pure overhead.
