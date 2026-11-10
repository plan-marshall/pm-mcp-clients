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
  - verdict: contradicted | checked_at: cc0d62af | by: ui-improvements/cleanup | rescoped: yes | evidence: 25 of 140 test classes annotated, 22 use Generators; ~115 remain (Re-grounding 2026-11-10)
- OBSERVED: `pm-operator` main code has no logger (`CuiLogger`, `java.util.logging`, SLF4J: none) — read by a repository-wide search at HEAD 7e2b4b61
  - verdict: corroborated | checked_at: cc0d62af | by: ui-improvements/cleanup | rescoped: n/a | evidence: no logger in pm-operator main; pom declares cui-test-generator and juli-logger
- HYPOTHESIS: most `pm-operator` tests assert catalogue texts and screen text, so the share of classes that can use generated input is small — confirm/refute by the before count (verify-at-outline); a finding that fewer than a third of the classes take incidental input is reported to the operator, not forced
  - verdict: unverifiable | checked_at: cc0d62af | by: ui-improvements/cleanup | rescoped: n/a | evidence: outline-time count
- Re-grounded note at HEAD e83a9ee2 (2026-10-31): `cui-standards-foundation` (PLAN-77, shipped, PR #282) landed the rule in `CLAUDE.md` and converted the two small modules as the pilot; `pm-operator` already declared the CUI test artifacts. Follow the pilot's pattern: draw `Generators` values inside the test or a `@BeforeEach`, not in field initialisers (flagged as not seed-controlled); a class whose values are the contract is listed input-free, not converted; expected values that derive from a generated input are re-derived from it, and contract values (catalogue texts, screen text, scripted answers) stay literal. All 11 `package-info.java` of this module carry `@NullMarked` since PLAN-77.
  - verdict: corroborated | checked_at: cc0d62af | by: ui-improvements/cleanup | rescoped: n/a | evidence: 11 package-info.java all @NullMarked
- Pull request size (PLAN-78's run, 2026-10-31): the required reviewer CodeRabbit refuses a pull request of more than 100 files (it refused PLAN-78's 197-file first PR, and the optional Sourcery refuses above 150,000 diff characters), and a pull request it never reviewed cannot be merged. This plan converts about 116 test classes: its set will exceed 100 files. The `pre-commit` gate also rewrites the import grouping of main sources that drifted on `main` (171 `pm-api` files in PLAN-78's run): such a rewrite is not part of this plan and is restored from `origin/main` before the pull request (the operator authorised exactly that in PLAN-78). If the converted set still passes 100 files, stop and ask the operator to split the plan in two (unit tests; integration and transcript tests, which are this spec's two deliverables) before opening the pull request.
  - verdict: unverifiable | checked_at: cc0d62af | by: ui-improvements/cleanup | rescoped: n/a | evidence: consistent: ~115 conversions exceed 100 files; split 79a/79b
- HOLD (design audit 2026-11-02): it rewrites the tests of Drain, AcceptEdit, Start, Onboard and Enrol, which the D25 (drain: signal the pid of state/runtime.json, withdraw as 'runtime stop --withdraw'), D26 and R7-7 (accept-edit enrolment <project_id>) package changes. Sequence it after that package; also sequence it with PLAN-92 (shared handler tests).
  - verdict: contradicted | checked_at: cc0d62af | by: ui-improvements/cleanup | rescoped: yes | evidence: HOLD cleared: PLAN-97 #358 landed; orphan Enrol/Start/Onboard tests remain
- Verify-first clause: before each deliverable, re-read the test tree at the current HEAD; `PLAN-77` must have landed. The terminal plans in flight edit these files; re-read which of them landed. A refuted claim re-scopes the deliverable and is reported.
  - verdict: unverifiable | checked_at: cc0d62af | by: ui-improvements/cleanup | rescoped: n/a | evidence: process clause

- Design audit 002 (2026-11-02, archived): sequence this plan after the packages `operator-drain-signal` (D25), `operator-command-tree` (D26) and `accept-edit-enrolment` (R7-7), all open in `doc/plans/` at `28634b2a`; they are the three deliverables of PLAN-97 (staged 2026-11-04 on the operator's word): sequence this plan after PLAN-97. PLAN-92 has shipped (`c7d9dcb8`): its handler tests are the base of the rewrite.
  - verdict: corroborated | checked_at: cc0d62af | by: ui-improvements/cleanup | rescoped: n/a | evidence: PLAN-97 #358 landed; c7d9dcb8 ancestor of main

## Expected Surface

- OBSERVED: `pm-operator/src/test/java/` — present at HEAD 7e2b4b61

## Dependencies and Sequencing

- Depends on: `PLAN-77` (`cui-standards-foundation`)
- Overlaps with: every terminal plan that edits a `pm-operator` test (PLAN-14, 17, 20, 21, 22, 23, 30, 34, 36, 38, 42, 44, 48, 50 among them): runs when none of them is running
- Adjacent to: `PLAN-78` (`pm-api` tests)

## Re-grounding 2026-11-07 (cleanup)

Checked at `aa3a9be3`. The before-count is 23 of 132 test classes (111 `*Test` + 21 `*IT`) carrying `@EnableGeneratorController` (added by PLAN-26/91/92/94, #357, #358), not 0 of 116; about 109 remain. The HOLD on PLAN-97 is cleared (#358 landed) and PLAN-92 shipped. PR 362 also swept `nonEmptyStrings` to `nonBlankStrings` in the `pm-api` tests: use `nonBlankStrings`.

## Fold 2026-11-08 (PLAN-109 landing)

Source: `landings/PLAN-109.md` (layer 2), a CodeRabbit nitpick on PR #367 that the lane left unanswered and the three-layer reading confirmed. `control.plan.settings` is `Plan-Einstellungen` in `pm-api/src/main/resources/messages/control.de.properties:16` (at `56b43a02`), but no test asserts the German value: `ActionMenusTest` reads locale `en` (`:58`, `:78`, `:105` assert `startsWith("Plan settings")`) and `ControlCatalogueTest` checks key presence and parity only. A revert to `Einstellungen` would pass and bring back the clash with the rail's "Einstellungen" the rename removed. When this plan rewrites `ActionMenusTest`, add one German-locale assertion that entry 3's label starts with `Plan-Einstellungen` (expected catalogue text stays literal). Surface unchanged: the file lies below the claimed `pm-operator/src/test/java/`.

## Fold 2026-11-08 (PLAN-116 landing)

Source: `landings/PLAN-116.md` (layer 2), read at `6c105a86`. Three test gaps in files this plan rewrites anyway; surface unchanged (all below `pm-operator/src/test/java/`):

1. `transcript/ConsoleSettingsTranscriptTest.java:72-83` checks each device name and each age as separate substrings of the whole render, so swapped Firefox/Safari ages still pass (CodeRabbit nitpick on PR #368, unanswered). Assert each device's age on its own rendered row, for both clocks.
2. `PmConsoleBehaviourTest.java:758-763` waits for the second refresh to reach `TIMED_WAITING`, which a lock wait (`PmConsole.java:693` `tryLock(timeout)`) and `EntryFlow.bounded`'s `thread.join(millis)` both produce: without the lock the test can still pass. Make the check discriminate the lock wait (e.g. the lock's queue, `hasQueuedThreads`, or the waiting thread's blocker), and check the negative over the window rather than at one instant.
3. No test shows the injected aspect list reaches the dialogue through `EntryFlow.java:292` and `PmConsole.java:314` (reverting either fails nothing): add one dialogue-level assertion with a list of another size.

## Fold 2026-11-08 (PLAN-118 landing)

Source: `landings/PLAN-118.md` (layer 2), read at `2fc2c214`. Two test gaps in files this plan rewrites anyway; surface unchanged (all below `pm-operator/src/test/java/`):

1. `DrainCommandTest.java:619-650` builds the expected drain bound from the production constants (`DEFAULT_DRAIN_TIMEOUT_SECONDS`, `GRACE_PERIOD`, `MARGIN`), so a changed constant passes. Assert the contract literally (15 s without config, the configured value + 5 s + 10 s), and cover the String-valued `drain_timeout_seconds` branch (`DrainCommand.java:205-208`) and a non-object `machine-config.json`.
2. Nothing pins that `PmConsole` builds its completer from `OperatorVerbs.completer()` (`PmConsole.java:137`): a completer reverted to a hand list passes `OperatorVerbsTest`. Add an assertion on the console's own completer.

## Re-grounding 2026-11-10 (cleanup, at `cc0d62af`)

This section supersedes the spec text and earlier notes where they differ.

- Claim 0 contradicted (counts): at `cc0d62af` 25 of 140 test classes carry `@EnableGeneratorController`, 22 use
  `Generators`; 118 `*Test` + 22 `*IT` (110 unit outside `transcript/`, 30 IT/transcript) — about 115 remain. Recent
  landings (NeedsCommandTest, NeedsIT, AnswerRefusalTest, AnswerMultipartTest, AnswerSanitizationTest, ProjectCommandTest
  additions) added more literal-input tests.
- Claim 5 (HOLD on the Drain/AcceptEdit/Start/Onboard/Enrol tests) cleared: PLAN-97 (#358) landed; `EnrolCommandTest`,
  `StartCommandTest`, `OnboardCommandTest` exist without a matching production class — delete or re-home them.
- Folds: PLAN-116 item 1 DROPPED (fixed by PLAN-34 `40dc879e`, `ConsoleSettingsTranscriptTest:72-83`); item 2 lines
  `PmConsole.java:340,702`; item 3 `EntryFlow.java:292`, `PmConsole.java:309`. PLAN-118 items 1-2 still open. PLAN-109
  German assertion still open (`ActionMenusTest` en only).
- **Split (pull-request size, CodeRabbit refuses > 100 files):** emit as two halves — 79a unit tests (~86 files),
  79b integration and transcript tests (~29).
- Sequencing: only when no terminal plan runs (PLAN-123, PLAN-84 running; PLAN-30/42 next), last in the terminal queue.
- Disposition: AMEND (counts, split) and HOLD until the terminal queue is empty.

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
