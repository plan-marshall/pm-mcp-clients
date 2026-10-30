# PLAN-34: Terminal: Display Layer in pm-api

epic: ui-improvements
workstream: WS-05

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Add `Display` to `pm-api` (words, titles, time, duration on the shared rule) and use it in every view, printed tree and dialogue of the client, so no raw id, link name, `-` placeholder or ISO time reaches the operator and one plan has one title everywhere (CLI-CC-S-04 steps 2 to 4; CLI-CC-P-02, CLI-CC-P-03).

Source of this plan (read before scoping):

- `doc/claude_review/cli/cross-cutting/plan.adoc#CLI-CC-S-04`
- `doc/claude_review/cli/cross-cutting/proposals.adoc#CLI-CC-P-02`
- `doc/claude_review/cli/cross-cutting/proposals.adoc#CLI-CC-P-03`
- `doc/claude_review/cli/cross-cutting/findings.adoc`

## Deliverables

1. **The served order and the size of change in the terminal client**
   - What: `OverviewFormatting` keeps the order the runtime serves and sorts nothing by activity (the time beside a row only says how recently it was active); the plan view (`PlanViewLevel`) and the completion level (`PlanCompletionLevel`) show `change so far   12 files · +340 −27 · 2 unplanned` and `Change` from the catalogue key `plan.change.size` of `PLAN-82` when the plan carries a size, and leave the line out when it carries none (`doc/design/shared/plan-view.adoc#change-size`, `completion-report.adoc`).
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:javadoc, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-dev-java-cui:cui-logging
   - Done when:
     - A test in `pm-operator`: an activity event changes no row's position in the overview or the project level; the plan view and the completion level show the size, and no line without one (evidence order: `test-first`)
     - Terminal check (screen capture) shows this deliverable's change as specified on: `running-work`: the overview root, the project level after an activity event, `plan <id> --print` and the completion report with and without a size; before and after captures recorded (evidence order: `none`)
2. **Display**
   - What: `de.planmarshall.api.display.Display`: `word(kind, value)`, `title(…)`, `time(…)`, `duration(…)` on the `state.*` keys and the rule of `shared/texts-and-languages.adoc`; a raw id without text is a build failure.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:javadoc, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-dev-java-cui:cui-logging
   - Done when:
     - Table tests of `Display.duration` and `Display.time` in both languages (evidence order: `test-first`)
     - Terminal check (screen capture) shows this deliverable's change as specified on: `running-work` in both locales: overview, plan view, printed epic, reports, system, ended work, `questions` — one title per plan, no raw id, link name, `-` placeholder or ISO time; before and after captures recorded (evidence order: `none`)
3. **Used everywhere**
   - What: Overview, plan view, epic, projects, reports, system, printed trees and dialogues use it; one clock for ages.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-java:junit-integration, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
   - Done when:
     - `TuiCapabilityIT` and transcripts: no `remediation.ci-failure`, `ci_check:`, `(step current)`, lone `-`, link name or ISO time (evidence order: `test-first`)
     - Terminal check (screen capture) shows this deliverable's change as specified on: `running-work` in both locales: overview, plan view, printed epic, reports, system, ended work, `questions` — one title per plan, no raw id, link name, `-` placeholder or ISO time; before and after captures recorded (evidence order: `none`)
     - Browser regression check (Playwright MCP) on `fully-onboarded` and `running-work`: the web views that render the changed catalogue areas (common) show the new texts with no missing or raw key; snapshots and screenshots recorded (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-operator verify -Pintegration-tests`, `./mvnw -pl pm-api verify`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- CUI standards (`CLAUDE.md`, `PLAN-77`): a test that takes input data draws it from `Generators` with `@EnableGeneratorController` (the expected value of a contract stays literal); a log call at INFO or above goes through a `LogRecord` of the module's own `LogMessages` class (one per module that logs, never one for the repository) with a `LogAsserts` test; `@NullMarked` applies in a module that declares JSpecify (`pm-operator`, `pm-mock-runtime`); until `PLAN-77` has landed the test-scope CUI dependencies are missing in `pm-api`, `pm-exec`, `pm-relay` and `pm-mock-runtime`, and a test there follows plain JUnit and reports the departure, never adds the dependency itself.
- Ports of this plan: mock web port `20340`, app port `20341` (`AGENTS.md` § Ports, made passable by `shared-port-assignment`, PLAN-76). Pass them to every mock, app and e2e start; other plans may run at the same time, so never stop or reuse a process this worktree did not start, and an assigned port that is in use is a blocker, never a reason to take another port.
- Java 25, Lombok, records; no generic `Exception`/`RuntimeException`; JUnit 5 only, no Mockito (CLAUDE.md).
- Every new operator text goes through `MessageCatalogue` into `operator.en.properties` and `operator.de.properties`.
- `pm-api` stays free of `jackson-databind` and of any product module (build-enforced rules).
- Browser check of the rendered output, with the Playwright MCP server of `opencode.json`, following `AGENTS.md` § Browser checks: start the mock (`./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario <scenario> --web-port 20340"`), then `PM_MOCK_WEB_PORT=20340 ./pm-web-app/start.sh -p 20341` (read the port from `pm-web-app/target/pm-web-app.port`); the MCP browser starts with an empty profile, so pair again every session with a fresh single-use code (`POST http://127.0.0.1:20340/mock/pairing`, then open `<app>/pair#code=<CODE>`), never through the `Network:` URL; reach a state with `POST /mock/goto/<state>` and a reload. Check each target in `de` and `en`, dark and light, at 1440 × 900 and 390 × 844 (`browser_resize`). Read the page through `browser_snapshot` (the accessibility tree is the evidence of text and structure) and keep one `browser_take_screenshot` per target; the server writes to its `--output-dir` `.plan/temp/playwright-mcp/` (`opencode.json`), so name every file `{plan}-{target}-{before|after}` and never write to the repository root. Take a baseline snapshot BEFORE the change and one AFTER, and record both paths in the progress record. The visible text of a checked view shows no raw enum, state id, `{placeholder}`, ISO time or emoji outside model-written blocks. Stop the app (`./pm-web-app/stop.sh`) and the mock afterwards. If the Playwright MCP server is not available in the run, the browser Done-when clauses are NOT met — a blocker reported as such, never replaced by a unit test or skipped.
- Terminal check of the rendered output through the screen capture harness of `cli-pty-dialogue-harness` (`ScreenCaptureIT`, command line in `doc/design/terminal-client/27-tui-testing.adoc`): write this plan's captures to `pm-operator/src/test/resources/screen-captures/{plan}.txt`, run the set BEFORE the change and copy `pm-operator/target/screens/{plan}/` to `.plan/temp/terminal/{plan}/before/`, then run it AFTER and copy to `.../after/`. Capture at 120 × 30 and 80 × 24 in `en_US.UTF-8` and `de_DE.UTF-8` unless the target names other sizes or environments. Read every `.txt` and `.attrs.txt` with the file tools and compare them with the deliverable; the screen text shows no raw id, link name, state id, `-` placeholder, ISO time or HTTP status, and no text of a previous level. Never run `pm-operator` in the agent's own shell: the operator gate refuses a coding agent by design (`PM-SEC-3`) and is never worked around; only the harness, which clears the environment like every PTY test, runs it. If the harness is not available, the terminal Done-when clauses are NOT met — a blocker, never skipped.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/cli/cross-cutting/plan.adoc#CLI-CC-S-04`, `doc/claude_review/cli/cross-cutting/proposals.adoc#CLI-CC-P-02` — read at `doc/claude_review/cli/cross-cutting/plan.adoc`
  - verdict: corroborated | checked_at: cc5794c9 | by: ui-improvements/cleanup | rescoped: n/a | evidence: origin/main: cross-cutting/plan.adoc:40 [#CLI-CC-S-04]; cross-cutting/proposals.adoc:22 [#CLI-CC-P-02] and :40 [#CLI-CC-P-03] (all under doc/claude_review/cli)
- Re-grounded note at HEAD 07104678 (2026-10-30): the former `PLAN-81` (superseded) moved its terminal half here: deliverable 1 removes the sort of projects and plans by last activity from `OverviewFormatting` (running work is shown in the order the runtime serves) and shows the plan's size of change in the plan view and the completion level of the terminal client; `PLAN-82` builds the served order and the size in the contract and the mock and adds records to the `overview/` package of `pm-api`, so this plan waits for it. Re-read `OverviewFormatting` at HEAD before the first deliverable.
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/display/` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/display/` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/overview/` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/planview/` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/epicview/` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/reports/` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/system/` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/projects/` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/common.en.properties` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/common.de.properties` — present at HEAD c4dc9bb4

## Dependencies and Sequencing

- Depends on: `PLAN-06` (`shared-display-vocabulary`, shipped), `PLAN-26` (`cli-system-model`), `PLAN-76` (`shared-port-assignment`, shipped), `PLAN-82` (the served order and the size in the contract and the mock)
- Overlaps with: `cli-system-model` touches `system/`: runs after it
- Adjacent to: none

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-34-cli-display-layer.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
