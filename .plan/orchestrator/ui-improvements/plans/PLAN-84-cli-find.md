# PLAN-84: Terminal: Go to Work by Name (the `find` Command)

epic: ui-improvements
workstream: WS-04

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

The terminal half of "go to work by name" (UC-PLN-8, `AGREED`) from the work package `needs-and-find` (`doc/plans/needs-and-find.adoc`: "one terminal command"). The operator gives a part of an id or title, or a pull request number; the client lists the matches by kind (project, epic, plan, decision, report), running work first, ended work marked as ended; the operator opens one where it belongs; no match says so and keeps the name. The matches come from `GET /api/v1/find?q=` as `PLAN-82` builds the contract and the mock; the client searches nothing itself and loads no archive. Titles come from the project and are plain text, control sequences removed.

The web half is `PLAN-46` (the command palette behind the top bar's search button). The "what waits" half of the same package in the terminal is `PLAN-36`.

## Deliverables

1. **The command**
   - What: `find <text>` in the shell and in the console (`/` is not taken; the key at the prompt is fixed in the design `PLAN-82` writes): prints the matches grouped by kind in the order served, each with its title, where it belongs (project › epic › plan) and the command that opens it, ended work marked; an empty text is refused with the catalogue text; no match says so and repeats the text typed. In a terminal the matches are a choice: selecting one opens it where it belongs (the plan view, the epic view, the decision, the project, the report); `--print` prints and exits.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:javadoc, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-dev-java-cui:cui-logging
   - Done when:
     - `FindCommandTest` on the mock: matches by id, title and pull request number, grouped by kind, running first, ended marked; no match keeps the text; empty text refused; control sequences in a title removed (evidence order: `test-first`)
     - A PTY test: `find relay` in the console lists the matches and selecting the first opens its view (evidence order: `test-first`)
     - Terminal check (screen capture) shows this deliverable's change as specified on: `running-work`: `find relay`, `find 214`, `find nothing-here`, the console `find`; before and after captures recorded (evidence order: `none`)
2. **Texts and design**
   - What: every text of the command goes through `MessageCatalogue` into `operator.en.properties` and `operator.de.properties`; the command is listed in `doc/design/terminal-client/01-commands.adoc` (verb, mode, `--print`) and the console entry in `04-console.adoc`, as the design `PLAN-82` accepted says.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-documents:ref-asciidoc
   - Done when:
     - A catalogue test: every key of the command in both languages, no literal text in the command (the `LiteralTextGuardTest` list includes it) (evidence order: `test-first`)
     - The row of the package `needs-and-find` in `doc/plans/README.adoc` is set to `done` in the pull request that completes it: this plan, `PLAN-36`, `PLAN-46` and `PLAN-49` are the four consumers, and the last of them to land sets it (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-operator verify -Pintegration-tests`, `./mvnw -pl pm-api verify`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- CUI standards (`CLAUDE.md`, `PLAN-77`): a test that takes input data draws it from `Generators` with `@EnableGeneratorController` (the expected value of a contract stays literal); a log call at INFO or above goes through a `LogRecord` of the module's own `LogMessages` class (one per module that logs, never one for the repository) with a `LogAsserts` test; `@NullMarked` applies in a module that declares JSpecify (`pm-operator`, `pm-mock-runtime`); until `PLAN-77` has landed the test-scope CUI dependencies are missing in `pm-api`, `pm-exec`, `pm-relay` and `pm-mock-runtime`, and a test there follows plain JUnit and reports the departure, never adds the dependency itself.
- Java 25, Lombok, records; no generic `Exception`/`RuntimeException`; JUnit 5 only, no Mockito (CLAUDE.md).
- `find` is a read: it runs under a host marker (the gate guards acts only, `PLAN-14`); opening a match is a view, and an act reached from it passes the gate as it does today.
- Ports: none assigned; the captures run through the harness against the mock.
- Terminal check of the rendered output through the screen capture harness of `cli-pty-dialogue-harness` (`ScreenCaptureIT`, command line in `doc/design/terminal-client/27-tui-testing.adoc`): write this plan's captures to `pm-operator/src/test/resources/screen-captures/{plan}.txt`, run the set BEFORE the change and copy `pm-operator/target/screens/{plan}/` to `.plan/temp/terminal/{plan}/before/`, then run it AFTER and copy to `.../after/`. Capture at 120 × 30 and 80 × 24 in `en_US.UTF-8` and `de_DE.UTF-8` unless the target names other sizes or environments. Read every `.txt` and `.attrs.txt` with the file tools and compare them with the deliverable; the screen text shows no raw id, link name, state id, `-` placeholder, ISO time or HTTP status, and no text of a previous level. Never run `pm-operator` in the agent's own shell: the operator gate refuses a coding agent by design (`PM-SEC-3`) and is never worked around; only the harness, which clears the environment like every PTY test, runs it. If the harness is not available, the terminal Done-when clauses are NOT met — a blocker, never skipped.
- Write-set discipline: the main checkout of this repository is never edited by the run; work only in the plan's worktree.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the package states "one terminal command" for find and the web app's search behind the top bar's search button — read at `doc/plans/needs-and-find.adoc` at HEAD 07104678
- OBSERVED: no `find` command exists in `pm-operator` — searched at HEAD 07104678 (`FindCommand` absent)
- HYPOTHESIS: the console key and the verb mode of `find` are fixed by the design of `PLAN-82` (deliverable 1), and `PLAN-42` (verb modes and help) lists every verb in one table — confirm/refute at the accepted design and `01-commands.adoc` (verify-at-outline)
- Verify-first clause: before each deliverable, re-read the named files at the current HEAD and confirm or refute every claim above; `PLAN-82` must have landed (the contract and the mock), `PLAN-36` and `PLAN-42` edit `PmOperator`, `PmConsole`, the command listing and the catalogue pair. A refuted claim re-scopes the deliverable and is reported.

## Expected Surface

- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/FindCommand.java` — absent at HEAD 07104678; created by this plan
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/FindCommandTest.java` — absent at HEAD 07104678; created by this plan
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/PmOperator.java` — present at HEAD 07104678
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/PmConsole.java` — present at HEAD 07104678
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/LiteralTextGuardTest.java` — present at HEAD 07104678
- OBSERVED: `pm-api/src/main/resources/messages/operator.en.properties` — present at HEAD 07104678
- OBSERVED: `pm-api/src/main/resources/messages/operator.de.properties` — present at HEAD 07104678
- OBSERVED: `doc/design/terminal-client/01-commands.adoc` — present at HEAD 07104678
- OBSERVED: `doc/design/terminal-client/04-console.adoc` — present at HEAD 07104678
- OBSERVED: `doc/plans/README.adoc` — present at HEAD 07104678
- OBSERVED: `pm-operator/src/test/resources/screen-captures/` — present at HEAD 07104678

## Dependencies and Sequencing

- Depends on: `PLAN-82` (`needs-and-find-contract`), `PLAN-14` (`cli-gate-scope`, shipped), `PLAN-76` (`shared-port-assignment`, shipped)
- Overlaps with: `PLAN-36` (`PmOperator`, `PmConsole`, the catalogue pair), `PLAN-42` (`PmConsole`, `01-commands.adoc`), `PLAN-48` (`01-commands.adoc`), `PLAN-30` (`01-commands.adoc`, catalogue): run one after the other
- Adjacent to: `PLAN-46` (the web half)

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-84-cli-find.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
