envelope_version=1
sender_type=plan
sender_id=plan-133-chat-mock-runtime
epic=ui-improvements
kind=candidate-lesson
created=2026-11-13T12:26:44Z

# Candidate lesson: CI wait reports green on stale runs while the PR is CONFLICTING

**Plan:** plan-133-chat-mock-runtime (PR #424, merged as a139ec62)
**Signal:** script-failure cluster / finalize process (reported by the orchestrator of this run)
**Class:** after the baseline on `main` moved, PR #424 became CONFLICTING. GitHub does not run the PR workflows on a conflicting head, so the newest check runs belonged to an older head. The finalize CI wait (`ci_complete_precondition`) took those runs as the head's and reported CI green; Sonar had no analysis of the actual head at all.

## Records of the class

- `ci_complete_precondition` reported CI complete and green while `mergeable` was CONFLICTING and the runs were for a previous head SHA.
- Consequence: the "zero open new-code Sonar issues at the head" gate (see the D8 criterion message, 4e6174 / 48f73b) could not be read for the head until the conflict was resolved and a new push triggered an analysis.

## Observation

"No failing checks" and "checks for this head passed" are different facts. The wait treated the absence of new runs as completion.

## Candidate corrective action

The CI completion precondition must (1) read the PR's mergeable state first and return a distinct `conflicting` outcome, never green, when it is CONFLICTING, and (2) match check runs to the current head SHA, treating "no runs for this head" as pending or not-triggered rather than complete. Sonar readiness likewise requires an analysis whose revision equals the head.
