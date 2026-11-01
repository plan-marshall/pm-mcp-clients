# PLAN-78: Java: pm-api Tests on Generated Input Data

epic: ui-improvements
workstream: WS-01

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Convert the tests of `pm-api` to the `pm-dev-java-cui:cui-testing` standard that `PLAN-77` introduced: input data from `Generators`, `@EnableGeneratorController` on every class that takes input, the expected value of a contract left literal. `pm-api` carries the shared catalogue, the display vocabulary and the HTTP client over the Unix socket, so most of its tests assert exact texts and wire names; the work is to separate the incidental inputs (ids, names, paths, counts, free text) from those contracts, not to replace the contracts.

## Deliverables

1. **pm-api tests on generated input**
   - What: the 71 test classes of `pm-api` (`*Test.java` and `*IT.java` at HEAD 7e2b4b61, re-counted at outline) take their input data from `Generators`; each class that takes input carries `@EnableGeneratorController`; `@ParameterizedTest` with 3 or more variants of generated types uses `@GeneratorsSource` where a typed generator exists. Expected values of a contract stay literal: catalogue texts, wire names, enum members, golden fixtures. The shared `FakeRuntimeServer` test support keeps its fixed routes. A class that takes no input data is listed in the progress record with that reason, never silently skipped.
   - Persona: plan-marshall:persona-module-tester
   - Skills: pm-dev-java:junit-core, pm-dev-java:junit-integration, pm-dev-java-cui:cui-testing
   - Done when:
     - A count over `pm-api` is recorded before and after: classes with `@EnableGeneratorController`, classes listed as input-free, number of tests (evidence order: `none`)
     - `./mvnw -pl pm-api verify` passes and the number of tests is not lower than before (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-api verify`, `./mvnw -pl pm-operator verify` (the operator reads the `pm-api` test jar).
- No new dependency or plugin. `cui-test-generator` and `cui-test-juli-logger` come from `PLAN-77`; if `pm-api/pom.xml` does not declare them at the head, the plan stops as a blocker.
- `pm-api` has no logging in main code and no `LogMessages` class is created here.
- The `pm-api` test jar is used by other modules (the stub daemon): its public test support keeps its signatures; a changed signature is a finding, not a free edit.
- Behaviour is unchanged. A test that fails after conversion exposes a wrong assumption about a generated value or a real defect; report it, never loosen the assertion to make it pass.
- Tests of the message catalogue assert the key set and the text of each key against the resource files; those keys and texts are contracts and stay literal.
- No browser check and no terminal check: the plan changes no rendered output. No ports are assigned.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: no `pm-api` test class uses `Generators` or `@EnableGeneratorController` (0 of 71 `*Test.java` and `*IT.java`) and `pm-api/pom.xml` declares no `de.cuioss` artifact — read at HEAD 7e2b4b61
- OBSERVED: the `pm-api` test jar carries `FakeRuntimeServer`, a Unix-socket HTTP server other modules start — read at `pm-api/src/test/java/de/planmarshall/api/testsupport/FakeRuntimeServer.java`
- HYPOTHESIS: most `pm-api` tests assert contract values (catalogue texts, wire names), so the share of classes that can use generated input is well below 71 — confirm/refute by the before count of Deliverable 1 (verify-at-outline); a finding that fewer than a third of the classes take incidental input is reported to the operator, not forced
- Re-grounded note at HEAD e83a9ee2 (2026-10-31): `cui-standards-foundation` (PLAN-77, shipped, PR #282) landed what this plan waited for: `pm-api/pom.xml` declares `cui-test-generator` and `cui-test-juli-logger` (test scope), the root POM's `jackson-core-only` enforcer rule admits the test stack (the operator chose that in the run), and `CLAUDE.md` states the rule. The pilot shows the pattern: draw `Generators` values inside the test or a `@BeforeEach`, not in field initialisers (the run flags those as not seed-controlled); a class whose values are the contract (`LandlockTest`) is listed input-free, not converted; expected values that derive from a generated input are re-derived from it, and contract values (wire names, codes, golden texts) stay literal. Do not touch the root POM's guard again; a banned artifact is a finding.
- Verify-first clause: before the deliverable, re-read `pm-api/pom.xml` and the test tree at the current HEAD; `PLAN-77` must have landed. A refuted claim re-scopes the deliverable and is reported.

## Expected Surface

- OBSERVED: `pm-api/src/test/java/` — present at HEAD 7e2b4b61

## Dependencies and Sequencing

- Depends on: `PLAN-77` (`cui-standards-foundation`)
- Overlaps with: every plan that adds or edits a `pm-api` test: re-read at HEAD before emit; none of the running plans declares a `pm-api` test directory-wide
- Adjacent to: `PLAN-79` (`pm-operator` tests)

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-78-cui-testing-api.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
