envelope_version=1
sender_type=plan
sender_id=plan-18-verify-shared-foundation
epic=ui-improvements
kind=candidate-lesson
created=2026-11-06T19:50:00Z

# Candidate lesson: sonar-roundtrip cannot decide on a docs-only PR because CI skips Sonar

**Source signal:** operator-supplied run context for plan plan-18-verify-shared-foundation (PR #349); finalize step
`sonar-roundtrip`.

## What happened

PR #349 changed only documentation. The repository's CI skips the build and the `sonar-build` job for docs-only
changes, so SonarCloud never analysed the PR. The `sonar-roundtrip` finalize step could therefore only fail as
undecidable (no analysis to read), and it needed an operator decision twice in the run.

## Candidate rule(s) for the orchestrator to judge

- For a docs-only footprint, `sonar-roundtrip` could detect that CI skipped the Sonar job (the check run is absent or
  skipped) and record "not applicable: no analysis owed" instead of escalating as undecidable. "Sonar was not run
  because nothing analysable changed" and "Sonar result could not be read" are different facts.
- Alternatively a docs-only plan could prune `sonar-roundtrip` at compose time (the `no_code_delta` predicate reads
  exactly this).
- The project's zero-Sonar-findings-at-landing rule should state what counts for a PR that Sonar never analyses.
