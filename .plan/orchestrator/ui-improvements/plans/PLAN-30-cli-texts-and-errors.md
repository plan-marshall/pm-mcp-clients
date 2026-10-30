# PLAN-30: Terminal: Every Text Through the Catalogue, One Error Pattern

epic: ui-improvements
workstream: WS-05

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Move every fixed string of the dialogues into the catalogue, give Picocli a resource bundle and custom handlers, render every refusal by error code in three lines with a fix, set the exit codes, and guard against literals (CLI-CC-S-03 and CLI-UX-S-03; CLI-CC-P-01, CLI-CC-P-04, CLI-UX-P-05).

Source of this plan (read before scoping):

- `doc/claude_review/cli/cross-cutting/plan.adoc#CLI-CC-S-03`
- `doc/claude_review/cli/ux/plan.adoc#CLI-UX-S-03`
- `doc/claude_review/cli/cross-cutting/proposals.adoc#CLI-CC-P-01`
- `doc/claude_review/cli/cross-cutting/proposals.adoc#CLI-CC-P-04`
- `doc/claude_review/cli/ux/proposals.adoc#CLI-UX-P-05`

## Deliverables

1. **Strings into the catalogue**
   - What: Every string of `AnswerCommand`, `OnboardCommand`, `WorkActionDialogue`, `CleanupCommand`, `RuntimeCommand` in `operator.en/de.properties`.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:javadoc, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-dev-java-cui:cui-logging
   - Done when:
     - `LiteralTextGuardTest` (extended) scans for literals passed to `print`, `println`, `printf`, `readLine`, `askRaw` and fails on one (evidence order: `test-first`). Re-grounded at HEAD cc5794c9: its render-call pattern already includes `prompt.askRaw` (`LiteralTextGuardTest.java:61-81`) and its guarded list already names `StartDialogue`, `ChatDialogue`, `WorkActionDialogue` and `HoldDialogue`, so the extension is the guarded list, not the pattern: add `AnswerCommand`, `CleanupCommand` and `RuntimeCommand` (and the sub-packages) to it, and clear what it then finds.
     - Terminal check (screen capture) shows this deliverable's change as specified on: `running-work` in `de_DE.UTF-8` and `en_US.UTF-8`: `answer Q-999 1`, `plan nope`, `bogus`, `pm-operator -h`, with exit codes; before and after captures recorded (evidence order: `none`)
2. **RefusalRenderer**
   - What: `OperatorRefusedException` carries the `pm-api` error code; `RefusalRenderer` prints `✗` line (the title), cause, `→ fix` from the shared set `PLAN-86` (`ErrorTexts` over `error.<code>.title`, `.cause`, `.fix`; no `operator.refusal.*` family is added); unknown code "The runtime refused: <message>" over `error.unknown.*`; never an HTTP status, a JSON body or an exception class.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:javadoc, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-dev-java-cui:cui-logging
   - Done when:
     - A unit test: `RefusalRenderer` prints the three lines for a known code and the unknown fallback for any other, in both languages, and never an HTTP status, a JSON body or an exception class (the check that every code has its texts belongs to `PLAN-86`) (evidence order: `test-first`)
     - Terminal check (screen capture) shows this deliverable's change as specified on: `running-work` in `de_DE.UTF-8` and `en_US.UTF-8`: `answer Q-999 1`, `plan nope`, `bogus`, `pm-operator -h`, with exit codes; before and after captures recorded (evidence order: `none`)
3. **Picocli in the operator's language**
   - What: `CommandLine.setResourceBundle` for usage; an `IParameterExceptionHandler` prints `✗ bogus is not a verb. → help lists them.`
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:javadoc, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-dev-java-cui:cui-logging
   - Done when:
     - In `de_DE.UTF-8`, `bogus` and `pm-operator -h` print no English word outside the glossary (evidence order: `test-first`)
     - Terminal check (screen capture) shows this deliverable's change as specified on: `running-work` in `de_DE.UTF-8` and `en_US.UTF-8`: `answer Q-999 1`, `plan nope`, `bogus`, `pm-operator -h`, with exit codes; before and after captures recorded (evidence order: `none`)
4. **Exit codes**
   - What: `1` refused, `2` usage, `3` needs input, as in `01-commands.adoc#exit-codes`.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:javadoc, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-dev-java-cui:cui-logging
   - Done when:
     - `ExitCodesTest` checks success, refusal and usage error of every verb (evidence order: `test-first`)
     - Terminal check (screen capture) shows this deliverable's change as specified on: `running-work` in `de_DE.UTF-8` and `en_US.UTF-8`: `answer Q-999 1`, `plan nope`, `bogus`, `pm-operator -h`, with exit codes; before and after captures recorded (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-operator verify -Pintegration-tests`, `./mvnw -pl pm-api verify`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- CUI standards (`CLAUDE.md`, `PLAN-77`): a test that takes input data draws it from `Generators` with `@EnableGeneratorController` (the expected value of a contract stays literal); a log call at INFO or above goes through a `LogRecord` of the module's own `LogMessages` class (one per module that logs, never one for the repository) with a `LogAsserts` test; `@NullMarked` applies in a module that declares JSpecify (`pm-operator`, `pm-mock-runtime`); until `PLAN-77` has landed the test-scope CUI dependencies are missing in `pm-api`, `pm-exec`, `pm-relay` and `pm-mock-runtime`, and a test there follows plain JUnit and reports the departure, never adds the dependency itself.
- Java 25, Lombok, records; no generic `Exception`/`RuntimeException`; JUnit 5 only, no Mockito (CLAUDE.md).
- Every new operator text goes through `MessageCatalogue` into `operator.en.properties` and `operator.de.properties`.
- Terminal check of the rendered output through the screen capture harness of `cli-pty-dialogue-harness` (`ScreenCaptureIT`, command line in `doc/design/terminal-client/27-tui-testing.adoc`): write this plan's captures to `pm-operator/src/test/resources/screen-captures/{plan}.txt`, run the set BEFORE the change and copy `pm-operator/target/screens/{plan}/` to `.plan/temp/terminal/{plan}/before/`, then run it AFTER and copy to `.../after/`. Capture at 120 × 30 and 80 × 24 in `en_US.UTF-8` and `de_DE.UTF-8` unless the target names other sizes or environments. Read every `.txt` and `.attrs.txt` with the file tools and compare them with the deliverable; the screen text shows no raw id, link name, state id, `-` placeholder, ISO time or HTTP status, and no text of a previous level. Never run `pm-operator` in the agent's own shell: the operator gate refuses a coding agent by design (`PM-SEC-3`) and is never worked around; only the harness, which clears the environment like every PTY test, runs it. If the harness is not available, the terminal Done-when clauses are NOT met — a blocker, never skipped.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/cli/cross-cutting/plan.adoc#CLI-CC-S-03`, `doc/claude_review/cli/ux/plan.adoc#CLI-UX-S-03` — read at `doc/claude_review/cli/cross-cutting/plan.adoc`
  - verdict: corroborated | checked_at: cc5794c9 | by: ui-improvements/cleanup | rescoped: n/a | evidence: origin/main doc/claude_review/cli/cross-cutting/plan.adoc:31-32 [#CLI-CC-S-03] and cli/ux/plan.adoc:28-29 [#CLI-UX-S-03] exist
- OBSERVED: `LiteralTextGuardTest` exists in `pm-operator` — read at `pm-operator/src/test/java/de/planmarshall/operator/LiteralTextGuardTest.java`
  - verdict: corroborated | checked_at: cc5794c9 | by: ui-improvements/cleanup | rescoped: n/a | evidence: git ls-tree origin/main: pm-operator/src/test/java/de/planmarshall/operator/LiteralTextGuardTest.java exists
- HYPOTHESIS: `LiteralTextGuardTest` does not cover dialogues and `askRaw` — confirm/refute at its source (verify-at-outline)
  - verdict: contradicted | checked_at: cc5794c9 | by: ui-improvements/cleanup | rescoped: yes | evidence: LiteralTextGuardTest.java:61-81 RENDER_CALL includes prompt.askRaw; GUARDED lists StartDialogue, ChatDialogue, WorkActionDialogue, HoldDialogue. Uncovered: AnswerCommand, CleanupCommand, RuntimeCommand, sub-packages. D1 now names the guarded list as the extension
- Exit codes (structure review, 2026-10-30): this plan owns the check of the one table in `01-commands.adoc#exit-codes` for every verb (`ExitCodesTest`); `PLAN-17` (exit `3` for `--no-input`) and `PLAN-26` (exit `1` on a failed read) implement their verbs from the same table before this plan runs, and this plan fails on a verb that breaks it.
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/AnswerCommand.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/OnboardCommand.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/WorkActionDialogue.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/CleanupCommand.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/RuntimeCommand.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/OperatorRefusedException.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/RefusalRenderer.java` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/PmOperator.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/LiteralTextGuardTest.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/RefusalRendererTest.java` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ExitCodesTest.java` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `doc/design/terminal-client/01-commands.adoc` — present at HEAD c4dc9bb4
- OBSERVED: `doc/design/terminal-client/13-texts-operator.adoc` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/operator.en.properties` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/operator.de.properties` — present at HEAD c4dc9bb4

## Dependencies and Sequencing

- Depends on: `PLAN-21` (`cli-answer-dialogue`), `PLAN-22` (`cli-state-dialogue-onboarding`, shipped), `PLAN-23` (`cli-cleanup-dialogue`), `PLAN-17` (`cli-runtime-act-confirm`), `PLAN-86` (`shared-error-texts`)
- Overlaps with: touches the dialogue classes of WS-04: runs after them
- Adjacent to: none

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-30-cli-texts-and-errors.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
