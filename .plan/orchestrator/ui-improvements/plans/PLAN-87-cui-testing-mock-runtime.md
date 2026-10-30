# PLAN-87: Java: pm-mock-runtime Tests on Generated Input Data

epic: ui-improvements
workstream: WS-01

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Convert the tests of `pm-mock-runtime` to the `pm-dev-java-cui:cui-testing` standard that `PLAN-77` introduced: input data from `Generators`, `@EnableGeneratorController` on every class that takes input, the expected value of a contract left literal. This is the part of the former deliverable 4 of `PLAN-77` that covered the module with the most tests (43 classes at HEAD 7e2b4b61); the structure review of 2026-10-30 moved it out, so that the test conversions stand as one plan per module: `PLAN-77` (`pm-relay`, `pm-exec`, the pilot), `PLAN-78` (`pm-api`), `PLAN-79` (`pm-operator`) and this one. The mock's tests assert scenario fixtures, wire names and screen-visible texts; the work is to generate the incidental input (ids, names, paths, counts, free text) and keep those contracts literal.

## Deliverables

1. **pm-mock-runtime tests on generated input**
   - What: the test classes of `pm-mock-runtime` (`*Test.java` and `*IT.java`, 43 at HEAD 7e2b4b61, re-counted at outline) take their input data from `Generators`; each class that takes input carries `@EnableGeneratorController`. Expected values of a contract stay literal: scenario ids and fixtures, wire names, route paths, golden texts. A class that takes no input data is listed in the progress record with that reason, never silently skipped. The `LogAsserts` test of `MockRuntimeExtension` that `PLAN-77` writes is kept as it is.
   - Persona: plan-marshall:persona-module-tester
   - Skills: pm-dev-java:junit-core, pm-dev-java:junit-integration, pm-dev-java-cui:cui-testing
   - Done when:
     - A count is recorded before and after: classes with `@EnableGeneratorController`, classes listed as input-free, number of tests (evidence order: `none`)
     - `./mvnw -pl pm-mock-runtime verify` passes and the number of tests is not lower than before (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-mock-runtime verify`, `./mvnw -pl pm-operator verify -Pintegration-tests` (the operator drives the mock in its transcript tests).
- No new dependency or plugin: `cui-test-generator` and `cui-test-juli-logger` come from `PLAN-77`; if `pm-mock-runtime/pom.xml` does not declare them at the head, the plan stops as a blocker.
- The mock logs only through the `LogMessages` holder of `PLAN-77` (one per module); no second holder is created here.
- Behaviour is unchanged. A test that fails after conversion exposes a wrong assumption about a generated value or a real defect; report it, never loosen the assertion to make it pass.
- The scenario fixtures (`*.json`) are contracts of the mock and stay as they are.
- No browser check and no terminal check: the plan changes no rendered output. No ports are assigned.
- Write-set discipline: the main checkout of this repository is never edited by the run; work only in the plan's worktree.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: no `pm-mock-runtime` test class uses `Generators` or `@EnableGeneratorController` and `pm-mock-runtime/pom.xml` declares neither `cui-test-generator` nor `cui-test-juli-logger` — read by the analysis of 2026-10-30 at HEAD 7e2b4b61 (`PLAN-77` adds the dependencies)
- OBSERVED: the module has 43 `*Test.java` and `*IT.java` classes — counted at HEAD 7e2b4b61
- HYPOTHESIS: most mock tests assert scenario fixtures and wire texts, so the share of classes that can use generated input is well below 43 — confirm/refute by the before count (verify-at-outline); a finding that fewer than a third of the classes take incidental input is reported to the operator, not forced
- Verify-first clause: before the deliverable, re-read `pm-mock-runtime/pom.xml` and the test tree at the current HEAD; `PLAN-77` must have landed. `PLAN-82` and any other plan that edits the mock engine adds tests there: re-read which landed. A refuted claim re-scopes the deliverable and is reported.

## Expected Surface

- OBSERVED: `pm-mock-runtime/src/test/java/` — present at HEAD 07104678

## Dependencies and Sequencing

- Depends on: `PLAN-77` (`cui-standards-foundation`)
- Overlaps with: every plan that adds or edits a `pm-mock-runtime` test (`PLAN-82` and others): run it when none of them is running
- Adjacent to: `PLAN-78` (`pm-api` tests), `PLAN-79` (`pm-operator` tests)

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-87-cui-testing-mock-runtime.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
