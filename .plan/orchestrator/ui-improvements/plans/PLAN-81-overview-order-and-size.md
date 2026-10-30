# PLAN-81: Shared: Running Work in a Stable Order, and a Plan's Size of Change

> **SUPERSEDED on 2026-10-30 (structure review, operator decision): do not emit.** The contract and mock half moved to `PLAN-82` (same overview records and the same mock engine); the terminal half moved to `PLAN-34` (the overview keeps the served order, the plan view and the completion level show the size); the web halves moved to `PLAN-52` (running work keeps the served order), `PLAN-55` (plan page head line) and `PLAN-54` (completion report line). The text below is kept for the record and for those plans to read.

epic: ui-improvements
workstream: WS-01

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Carry out the work package `overview-order-and-size` (`doc/plans/overview-order-and-size.adoc`, merged with PR #278 on 2026-10-30; serves UC-PLN-4 and UC-PLN-1; PM-ARCH-11). Two changes, both already written into the shared design (`doc/design/shared/plan-overview.adoc` § item rows, `plan-view.adoc` § Change So Far, `completion-report.adoc`) and into the specification of the runtime (`plan-marshall-documentation`, `runtime-model/01-processes-and-transport.adoc#overview-resource`; `pm-mcp-core`, `store-schemas/13-leases-integrity-testing.adoc#change-size`):

- **Order.** Running work is served in one stable order: what needs the operator first, then the newest started (`created_at`); projects by the start of their oldest active work. Activity moves no row. Both clients keep the order served. The terminal client sorts by last activity itself (`OverviewFormatting`) and stops doing so; the web app already shows the list as served. The mock runtime serves the stable order, and a test shows that an activity event moves no row.
- **Size.** `plan.change.size` (`12 files · +340 −27 · 2 unplanned`) stands in the head of the plan view (`change so far`) and in the completion report (`Change`). The contract carries the size and the count of unplanned changes, both absent before the first capture; a plan without a measured size leaves the line out. Git measured it and the runtime serves it; a client adds nothing up.

This is the first of the three packages of PR #278 and the others depend on it: `PLAN-82` (`needs-and-find-contract`) and `PLAN-83` (`web-moving-between-plans`).

## Deliverables

1. **The order, served and kept**
   - What: the mock runtime serves projects, epics and plans of the overview resource in the stable order (needs the operator first, then `created_at` newest first; projects by the start of their oldest active work); `pm-operator` removes the activity sort of `OverviewFormatting` and keeps the order served; the web app is checked to show the list as served (it does today: confirm, change nothing that works).
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:javadoc, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-dev-java-cui:cui-logging
   - Done when:
     - A test in `pm-mock-runtime` and one in `pm-operator`: an activity event changes no row's position; a row moves when its item starts, ends, or starts or stops needing the operator (evidence order: `test-first`)
     - Terminal check (screen capture) shows this deliverable's change as specified on: `running-work`: the overview root and the project level after an activity event; before and after captures recorded (evidence order: `none`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `running-work`: the Running Work page keeps its rows through an activity event; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
2. **The size of change**
   - What: the contract (`pm-api`) carries `change` on the plan and on the completion report (files, insertions, deletions, unplanned count; absent before the first capture); the mock serves it for the plans of the scenarios that have a measured size and omits it for one that has none; the plan view of both clients shows `change so far` and the completion report `Change` from the catalogue key `plan.change.size` (English and German, in `plan_view.{en,de}.properties` as the design states); no client adds anything up.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:javadoc, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-dev-java-cui:cui-logging, pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - A contract test (record round trip, absent size) and a catalogue test (both languages, plural forms, no unplanned part at zero) (evidence order: `test-first`)
     - Component tests of the web plan view and completion report: the line is present with a size and absent without one (evidence order: `test-first`)
     - Terminal check (screen capture) shows this deliverable's change as specified on: `running-work` and `fully-onboarded`: `plan <id> --print` and the completion report with and without a size; before and after captures recorded (evidence order: `none`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `running-work`: the plan page head and the completion report with and without a size; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
     - The row of this package in `doc/plans/README.adoc` is set to `done` in the same pull request (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through Maven and npm scripts only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-api verify`, `./mvnw -pl pm-mock-runtime verify`, `./mvnw -pl pm-operator verify -Pintegration-tests`, `./mvnw -pl pm-web-app verify`, `./mvnw -pl pm-web-e2e verify`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- CUI standards (`CLAUDE.md`, `PLAN-77`): a test that takes input data draws it from `Generators` with `@EnableGeneratorController` (the expected value of a contract stays literal); a log call at INFO or above goes through a `LogRecord` of the module's own `LogMessages` class (one per module that logs, never one for the repository) with a `LogAsserts` test; `@NullMarked` applies in a module that declares JSpecify (`pm-operator`, `pm-mock-runtime`); until `PLAN-77` has landed the test-scope CUI dependencies are missing in `pm-api`, `pm-exec`, `pm-relay` and `pm-mock-runtime`, and a test there follows plain JUnit and reports the departure, never adds the dependency itself.
- Frontend standards (`CLAUDE.md`, `PLAN-80`): `pm-dev-frontend` (`javascript`, `css`, `jest-testing`, `lint-config`, `javascript-security`) and `pm-dev-frontend-cui` (`cui-javascript-project`) apply to `pm-web-app` and `pm-web-e2e`; text from the runtime or from a model reaches the DOM through text-treating sinks only (no `innerHTML`, `outerHTML`, `insertAdjacentHTML`, `{@html}`); a test waits on a condition, never on an arbitrary timer; the tests run on Vitest with Testing Library and the `jest-testing` standards apply as method.
- Ports of this plan: mock web port `20810`, app port `20811` (`AGENTS.md` § Ports, made passable by `shared-port-assignment`, PLAN-76). Pass them to every mock, app and e2e start; other plans may run at the same time, so never stop or reuse a process this worktree did not start, and an assigned port that is in use is a blocker, never a reason to take another port.
- Java 25, Lombok, records; no generic `Exception`/`RuntimeException`; JUnit 5 only, no Mockito (CLAUDE.md).
- Every new text goes through the message catalogue (`plan_view.en.properties` and `plan_view.de.properties` in `pm-api`); the key and the wording are those of `doc/design/shared/plan-view.adoc`.
- The documents are already merged. A text that a deliverable finds wrong in them is reported and fixed in the same pull request, never worked around in code.
- Browser check of the rendered output, with the Playwright MCP server of `opencode.json`, following `AGENTS.md` § Browser checks: start the mock (`./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario <scenario> --web-port 20810"`), then `PM_MOCK_WEB_PORT=20810 ./pm-web-app/start.sh -p 20811` (read the port from `pm-web-app/target/pm-web-app.port`); the MCP browser starts with an empty profile, so pair again every session with a fresh single-use code (`POST http://127.0.0.1:20810/mock/pairing`, then open `<app>/pair#code=<CODE>`), never through the `Network:` URL; reach a state with `POST /mock/goto/<state>` and a reload. Check each target in `de` and `en`, dark and light, at 1440 × 900 and 390 × 844 (`browser_resize`). Read the page through `browser_snapshot` (the accessibility tree is the evidence of text and structure) and keep one `browser_take_screenshot` per target; the server writes to its `--output-dir` `.plan/temp/playwright-mcp/` (`opencode.json`), so name every file `{plan}-{target}-{before|after}` and never write to the repository root. Take a baseline snapshot BEFORE the change and one AFTER, and record both paths in the progress record. The visible text of a checked view shows no raw enum, state id, `{placeholder}`, ISO time or emoji outside model-written blocks. Stop the app (`./pm-web-app/stop.sh`) and the mock afterwards. If the Playwright MCP server is not available in the run, the browser Done-when clauses are NOT met — a blocker reported as such, never replaced by a unit test or skipped.
- Terminal check of the rendered output through the screen capture harness of `cli-pty-dialogue-harness` (`ScreenCaptureIT`, command line in `doc/design/terminal-client/27-tui-testing.adoc`): write this plan's captures to `pm-operator/src/test/resources/screen-captures/{plan}.txt`, run the set BEFORE the change and copy `pm-operator/target/screens/{plan}/` to `.plan/temp/terminal/{plan}/before/`, then run it AFTER and copy to `.../after/`. Capture at 120 × 30 and 80 × 24 in `en_US.UTF-8` and `de_DE.UTF-8` unless the target names other sizes or environments. Read every `.txt` and `.attrs.txt` with the file tools and compare them with the deliverable; the screen text shows no raw id, link name, state id, `-` placeholder, ISO time or HTTP status, and no text of a previous level. Never run `pm-operator` in the agent's own shell: the operator gate refuses a coding agent by design (`PM-SEC-3`) and is never worked around; only the harness, which clears the environment like every PTY test, runs it. If the harness is not available, the terminal Done-when clauses are NOT met — a blocker, never skipped.
- Write-set discipline: the main checkout of this repository is never edited by the run; work only in the plan's worktree.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the terminal client sorts projects and plans by last activity in `pm-operator/src/main/java/de/planmarshall/operator/overview/OverviewFormatting.java`; the web app shows the list as served — stated by the ThinkRail analysis (`plan-marshall-documentation`, `doc/articles/thinkrail.adoc`, § Where It Touches Our Documents), read 2026-10-30
- OBSERVED: the design documents already state the order and the line: `doc/design/shared/plan-overview.adoc` (item rows), `plan-view.adoc` (#change-size), `completion-report.adoc` — merged in PR #278 at `07104678`
- HYPOTHESIS: the contract has no `change` field on `ActivePlanSummary` or on `PlanCompletionReport`, and the plan detail record is `EpicProcessPlan` or a record beside it — confirm/refute at the files (verify-at-outline); the record that carries a plan's detail for `plan <id>` and the plan page decides where `change` goes
- HYPOTHESIS: the mock's overview already serves the scenario's order unchanged, so "stable order" is a sort the mock applies for the first time — confirm/refute at `MockRuntimeEngine` (verify-at-outline)
- Verify-first clause: before each deliverable, re-read the named files at the current HEAD and confirm or refute every claim above; `PLAN-34` (display layer) and `PLAN-50` (column grid) touch `overview/`, `PLAN-55` reworks the plan page head, `PLAN-54` the report views. A refuted claim re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/overview/OverviewFormatting.java` — present at HEAD 07104678
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/overview/OverviewFormattingTest.java` — present at HEAD 07104678
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/planview/PlanViewLevel.java` — present at HEAD 07104678
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/planview/PlanViewFormatting.java` — present at HEAD 07104678
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/planview/PlanCompletionLevel.java` — present at HEAD 07104678
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/overview/ActivePlanSummary.java` — present at HEAD 07104678
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/report/PlanCompletionReport.java` — present at HEAD 07104678
- OBSERVED: `pm-api/src/main/resources/messages/plan_view.en.properties` — present at HEAD 07104678
- OBSERVED: `pm-api/src/main/resources/messages/plan_view.de.properties` — present at HEAD 07104678
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` — present at HEAD 07104678
- OBSERVED: `pm-web-app/src/lib/views/PlanView.svelte` — present at HEAD 07104678
- OBSERVED: `pm-web-app/src/lib/views/CompletionReportView.svelte` — present at HEAD 07104678
- OBSERVED: `pm-web-app/src/lib/views/OverviewView.svelte` — present at HEAD 07104678
- OBSERVED: `pm-web-e2e/tests/overview.spec.js` — present at HEAD 07104678
- OBSERVED: `pm-web-e2e/tests/plan-view.spec.js` — present at HEAD 07104678
- OBSERVED: `doc/plans/README.adoc` — present at HEAD 07104678
- OBSERVED: `pm-operator/src/test/resources/screen-captures/` — present at HEAD 07104678

## Dependencies and Sequencing

- Depends on: `PLAN-76` (`shared-port-assignment`)
- Overlaps with: `PLAN-34` (`overview/`, display layer: run one after the other), `PLAN-55` (`PlanView.svelte` head), `PLAN-54` (report views), `PLAN-52` (`OverviewView.svelte`), `PLAN-09` (`OverviewView.svelte`), `PLAN-50` (`overview/`)
- Adjacent to: `PLAN-82` and `PLAN-83` (depend on this plan)

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-81-overview-order-and-size.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
