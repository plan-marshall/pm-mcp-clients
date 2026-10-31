# PLAN-79: Java: pm-operator Tests on Generated Input Data

epic: ui-improvements
workstream: WS-04

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Convert the tests of `pm-operator` to the `pm-dev-java-cui:cui-testing` standard that `PLAN-77` introduced: input data from `Generators`, `@EnableGeneratorController` on every class that takes input, the expected value of a contract left literal. `pm-operator` already declares `cui-test-generator` and `cui-test-juli-logger` and uses neither. Most of its tests are transcripts, screen captures and catalogue assertions whose expected text is the contract; the work is to generate the incidental input (ids, names, paths, counts, free text) and keep those texts literal.

## Deliverables

1. **pm-operator unit tests on generated input**
   - What: the unit tests of `pm-operator` (`*Test.java` outside `transcript/`) take their input data from `Generators` and carry `@EnableGeneratorController` where they take input. Expected values of a contract stay literal. A class that takes no input data is listed in the progress record with that reason.
   - Persona: plan-marshall:persona-module-tester
   - Skills: pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
   - Done when:
     - A count is recorded before and after for the unit tests: classes with `@EnableGeneratorController`, classes listed as input-free, number of tests (evidence order: `none`)
     - `./mvnw -pl pm-operator verify` passes and the number of tests is not lower than before (evidence order: `none`)
2. **pm-operator integration and transcript tests**
   - What: the `*IT.java` and the `transcript/` tests (PTY, screen captures, dialogue ITs) generate the incidental input of a scenario (a typed id, a name entered at a prompt) where the scenario does not depend on the fixture's fixed value; the mock scenario ids, the screen text and the scripted answers that are the contract stay literal. The harness (`DialogueHarness`, `PtySession`, `ScreenCaptureIT`) is not rewritten.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-java:junit-integration, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
   - Done when:
     - A count is recorded before and after for the transcript and IT classes, as in deliverable 1 (evidence order: `none`)
     - `./mvnw -pl pm-operator verify -Pintegration-tests` passes and the screen-capture set is byte-identical before and after (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-operator verify -Pintegration-tests`.
- No new dependency or plugin: `cui-test-generator` is already declared by `pm-operator`.
- `pm-operator` does not log: no `LogMessages` class and no `LogAsserts` test is created here.
- Behaviour is unchanged; the operator gate refuses a coding agent by design (`PM-SEC-3`), so only the harness runs `pm-operator`, never the agent's own shell. A test that fails after conversion exposes a wrong assumption about a generated value or a real defect; report it, never loosen the assertion.
- Sequencing: this plan rewrites test files that every terminal plan also edits. It is emitted when no terminal plan is running, or in two halves (unit tests, then integration and transcript tests) between terminal plans; the operator decides at emit. A test file a running plan changes is left out and named in the progress record.
- No browser check; the terminal captures are the harness's, compared byte for byte. No ports are assigned.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: no `pm-operator` test class uses `Generators` or `@EnableGeneratorController` (0 of 116 `*Test.java` and `*IT.java`) although `pm-operator/pom.xml` declares `cui-test-generator` and `cui-test-juli-logger` — read at HEAD 7e2b4b61
- OBSERVED: `pm-operator` main code has no logger (`CuiLogger`, `java.util.logging`, SLF4J: none) — read by a repository-wide search at HEAD 7e2b4b61
- HYPOTHESIS: most `pm-operator` tests assert catalogue texts and screen text, so the share of classes that can use generated input is small — confirm/refute by the before count (verify-at-outline); a finding that fewer than a third of the classes take incidental input is reported to the operator, not forced
- Re-grounded note at HEAD e83a9ee2 (2026-10-31): `cui-standards-foundation` (PLAN-77, shipped, PR #282) landed the rule in `CLAUDE.md` and converted the two small modules as the pilot; `pm-operator` already declared the CUI test artifacts. Follow the pilot's pattern: draw `Generators` values inside the test or a `@BeforeEach`, not in field initialisers (flagged as not seed-controlled); a class whose values are the contract is listed input-free, not converted; expected values that derive from a generated input are re-derived from it, and contract values (catalogue texts, screen text, scripted answers) stay literal. All 11 `package-info.java` of this module carry `@NullMarked` since PLAN-77.
- Pull request size (PLAN-78's run, 2026-10-31): the required reviewer CodeRabbit refuses a pull request of more than 100 files (it refused PLAN-78's 197-file first PR, and the optional Sourcery refuses above 150,000 diff characters), and a pull request it never reviewed cannot be merged. This plan converts about 116 test classes: its set will exceed 100 files. The `pre-commit` gate also rewrites the import grouping of main sources that drifted on `main` (171 `pm-api` files in PLAN-78's run): such a rewrite is not part of this plan and is restored from `origin/main` before the pull request (the operator authorised exactly that in PLAN-78). If the converted set still passes 100 files, stop and ask the operator to split the plan in two (unit tests; integration and transcript tests, which are this spec's two deliverables) before opening the pull request.
- Verify-first clause: before each deliverable, re-read the test tree at the current HEAD; `PLAN-77` must have landed. The terminal plans in flight edit these files; re-read which of them landed. A refuted claim re-scopes the deliverable and is reported.

## Expected Surface

- OBSERVED: `pm-operator/src/test/java/` — present at HEAD 7e2b4b61

## Dependencies and Sequencing

- Depends on: `PLAN-77` (`cui-standards-foundation`)
- Overlaps with: every terminal plan that edits a `pm-operator` test (PLAN-14, 17, 20, 21, 22, 23, 30, 34, 36, 38, 42, 44, 48, 50 among them): runs when none of them is running
- Adjacent to: `PLAN-78` (`pm-api` tests)

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-79-cui-testing-operator.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
