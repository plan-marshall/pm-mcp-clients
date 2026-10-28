# PLAN-21: Terminal: A Working Answer Dialogue

epic: ui-improvements
workstream: WS-04

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Make `answer` work: own the terminal correctly, let refusals reach the renderer, render options, votes, dissent, evidence and the range from the envelope, move every string into the catalogue, and add the direct form `answer Q-007 2` (CLI-UC-S-04; CLI-UC-P-03).

Source of this plan (read before scoping):

- `doc/claude_review/cli/use-cases/plan.adoc#CLI-UC-S-04`
- `doc/claude_review/cli/use-cases/proposals.adoc#CLI-UC-P-03`
- `doc/claude_review/cli/use-cases/findings-plans-control.adoc#CLI-UC-F-10`

## Deliverables

1. **Terminal ownership and errors**
   - What: `AnswerCommand` borrows the console's scoped terminal (no try-with-resources) and opens its own only in the shell; no `catch (IOException | RuntimeException)` that writes to a closed terminal.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:junit-core
   - Done when:
     - A unit test answers twice in one console session (evidence order: `test-first`)
     - Terminal check (screen capture) shows this deliverable's change as specified on: `running-work` → `decisions.open`: `answer Q-007` (options, votes, dissent, evidence, prompt), Enter, `answer Q-007 2`, `answer WR-2`; in `de_DE.UTF-8` no English word outside the glossary; before and after captures recorded (evidence order: `none`)
2. **Content from the envelope**
   - What: Options, votes, dissent, recommendation, the evidence hint (file, lines, summary), the range `1-N`; every string as `operator.decision.*`.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-java:junit-integration, pm-dev-java:junit-core
   - Done when:
     - `AnswerDialogueIT` enabled: option, own words, waiver, later (evidence order: `test-first`)
     - Terminal check (screen capture) shows this deliverable's change as specified on: `running-work` → `decisions.open`: `answer Q-007` (options, votes, dissent, evidence, prompt), Enter, `answer Q-007 2`, `answer WR-2`; in `de_DE.UTF-8` no English word outside the glossary; before and after captures recorded (evidence order: `none`)
3. **Direct answer**
   - What: `answer Q-007 2` sends without a dialogue and prints `✓ Q-007 answered: Rebase onto main`.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-java:junit-integration, pm-dev-java:junit-core
   - Done when:
     - `AnswerDialogueIT`: the direct form and its act in `/mock/acts` (evidence order: `test-first`)
     - Terminal check (screen capture) shows this deliverable's change as specified on: `running-work` → `decisions.open`: `answer Q-007` (options, votes, dissent, evidence, prompt), Enter, `answer Q-007 2`, `answer WR-2`; in `de_DE.UTF-8` no English word outside the glossary; before and after captures recorded (evidence order: `none`)
     - Browser regression check (Playwright MCP) on `fully-onboarded` and `running-work`: the web views that render the changed catalogue areas (decisions) show the new texts with no missing or raw key; snapshots and screenshots recorded (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-operator verify -Pintegration-tests`, `./mvnw -pl pm-api verify`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- Ports of this plan: mock web port `20210`, app port `20211` (`AGENTS.md` § Ports, made passable by `shared-port-assignment`, PLAN-76). Pass them to every mock, app and e2e start; other plans may run at the same time, so never stop or reuse a process this worktree did not start, and an assigned port that is in use is a blocker, never a reason to take another port.
- Java 25, Lombok, records; no generic `Exception`/`RuntimeException`; JUnit 5 only, no Mockito (CLAUDE.md).
- Every new operator text goes through `MessageCatalogue` into `operator.en.properties` and `operator.de.properties`.
- A dialogue fixed here enables its `*DialogueIT` of `cli-pty-dialogue-harness` (removes the `@Disabled` that names this plan).
- Browser check of the rendered output, with the Playwright MCP server of `opencode.json`, following `AGENTS.md` § Browser checks: start the mock (`./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario <scenario> --web-port 20210"`), then `PM_MOCK_WEB_PORT=20210 ./pm-web-app/start.sh -p 20211` (read the port from `pm-web-app/target/pm-web-app.port`); the MCP browser starts with an empty profile, so pair again every session with a fresh single-use code (`POST http://127.0.0.1:20210/mock/pairing`, then open `<app>/pair#code=<CODE>`), never through the `Network:` URL; reach a state with `POST /mock/goto/<state>` and a reload. Check each target in `de` and `en`, dark and light, at 1440 × 900 and 390 × 844 (`browser_resize`). Read the page through `browser_snapshot` (the accessibility tree is the evidence of text and structure) and keep one `browser_take_screenshot` per target; the server writes to its `--output-dir` `.plan/temp/playwright-mcp/` (`opencode.json`), so name every file `{plan}-{target}-{before|after}` and never write to the repository root. Take a baseline snapshot BEFORE the change and one AFTER, and record both paths in the progress record. The visible text of a checked view shows no raw enum, state id, `{placeholder}`, ISO time or emoji outside model-written blocks. Stop the app (`./pm-web-app/stop.sh`) and the mock afterwards. If the Playwright MCP server is not available in the run, the browser Done-when clauses are NOT met — a blocker reported as such, never replaced by a unit test or skipped.
- Terminal check of the rendered output through the screen capture harness of `cli-pty-dialogue-harness` (`ScreenCaptureIT`, command line in `doc/design/terminal-client/27-tui-testing.adoc`): write this plan's captures to `pm-operator/src/test/resources/screen-captures/{plan}.txt`, run the set BEFORE the change and copy `pm-operator/target/screens/{plan}/` to `.plan/temp/terminal/{plan}/before/`, then run it AFTER and copy to `.../after/`. Capture at 120 × 30 and 80 × 24 in `en_US.UTF-8` and `de_DE.UTF-8` unless the target names other sizes or environments. Read every `.txt` and `.attrs.txt` with the file tools and compare them with the deliverable; the screen text shows no raw id, link name, state id, `-` placeholder, ISO time or HTTP status, and no text of a previous level. Never run `pm-operator` in the agent's own shell: the operator gate refuses a coding agent by design (`PM-SEC-3`) and is never worked around; only the harness, which clears the environment like every PTY test, runs it. If the harness is not available, the terminal Done-when clauses are NOT met — a blocker, never skipped.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/cli/use-cases/plan.adoc#CLI-UC-S-04`, `doc/claude_review/cli/use-cases/proposals.adoc#CLI-UC-P-03` — read at `doc/claude_review/cli/use-cases/plan.adoc`
  - verdict: corroborated | checked_at: cc5794c9 | by: ui-improvements/cleanup | rescoped: n/a | evidence: origin/main doc/claude_review/cli/use-cases/plan.adoc:41-42 [#CLI-UC-S-04] The Answer Dialogue; proposals.adoc:61-62 CLI-UC-P-03
- HYPOTHESIS: `AnswerCommand` closes the console's terminal through try-with-resources and swallows the error — confirm/refute at `pm-operator/src/main/java/de/planmarshall/operator/AnswerCommand.java` (verify-at-outline)
  - verdict: corroborated | checked_at: cc5794c9 | by: ui-improvements/cleanup | rescoped: n/a | evidence: AnswerCommand.java:228 try (var terminal = context.createTerminal()); OperatorContext.java:220-222 returns the console's scoped terminal; catch at ~:240 prints message, returns 1
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/AnswerCommand.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/Answers.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/AnswersTest.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/AnswerDialogueIT.java` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `doc/design/terminal-client/14-transcripts-decisions.adoc` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/operator.en.properties` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/operator.de.properties` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/decisions.en.properties` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/decisions.de.properties` — present at HEAD c4dc9bb4

## Dependencies and Sequencing

- Depends on: `PLAN-10` (`cli-choice-prompt-pause`), `PLAN-76` (`shared-port-assignment`)
- Overlaps with: none known at staging
- Adjacent to: none

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-21-cli-answer-dialogue.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
