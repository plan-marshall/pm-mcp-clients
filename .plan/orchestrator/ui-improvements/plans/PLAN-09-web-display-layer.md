# PLAN-09: Web: Display Layer and Catalogue Fixes

epic: ui-improvements
workstream: WS-02

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Add the display layer of CC-P-01 so that no view prints an API enum, code or unit id directly, fix every broken string of CC-F-05, and remove terminal key hints from web texts (CC-S-02; CC-P-01, CC-P-02). The error texts are `PLAN-86`'s and the loading texts `PLAN-45`'s.

Source of this plan (read before scoping):

- `doc/claude_review/cross-cutting/plan.adoc#CC-S-02`
- `doc/claude_review/cross-cutting/proposals.adoc#CC-P-01`
- `doc/claude_review/cross-cutting/proposals.adoc#CC-P-02`
- `doc/claude_review/cross-cutting/findings.adoc#CC-F-02`
- `doc/claude_review/cross-cutting/findings.adoc#CC-F-05`

## Deliverables

1. **Display module**
   - What: `lib/display/index.ts` with `displayState(kind, value)`, `displayError(code)` and `displayUnit(id)` on the `state.*` and `unit.*` keys of `shared-display-vocabulary` and, for `displayError`, on the shared error texts of `PLAN-86` (`error.<code>.title`, `.cause`, `.fix`; this plan writes none of them); `lib/display/enumFields.ts` lists the enum fields.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - Unit tests per function, in `de` and `en`, including an unknown value (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `fully-onboarded`: `/system`, `/system/cleanup`, overview, `/reports`, a completion report, an epic, `/start`, `/settings/machine` show plain state words and the CC-F-05 strings as expected; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
2. **Enum guard**
   - What: A unit test parses every `.svelte` file and fails when a field of `enumFields.ts` is interpolated without `displayState`.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - The test fails on `SystemView.svelte` before the views change and passes after (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `fully-onboarded`: `/system`, `/system/cleanup`, overview, `/reports`, a completion report, an epic, `/start`, `/settings/machine` show plain state words and the CC-F-05 strings as expected; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
3. **Views through the display layer**
   - What: `SystemView`, `PlanView`, `OverviewView`, `CleanupView`, `ReportsView`, `CompletionReportView`, `EpicView`, `StartView`, `MachineSettingsView` print enums, codes and unit ids only through the display layer.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - The text-guard baseline loses every raw-enum and state-id entry of these views (evidence order: `none`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `fully-onboarded`: `/system`, `/system/cleanup`, overview, `/reports`, a completion report, an epic, `/start`, `/settings/machine` show plain state words and the CC-F-05 strings as expected; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
4. **Catalogue fixes**
   - What: Every string of the table of CC-F-05 reads as expected in `de` and `en`; no terminal key hint in a web text; the mock serves the same keys. (Loading texts as `<area>.loading` keys moved to `PLAN-45`, which owns the loading pattern.)
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:javadoc, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-dev-java-cui:cui-logging
   - Done when:
     - A catalogue test asserts each corrected string of CC-F-05 in both locales (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `fully-onboarded`: `/system`, `/system/cleanup`, overview, `/reports`, a completion report, an epic, `/start`, `/settings/machine` show plain state words and the CC-F-05 strings as expected; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-web-app verify`, `PM_MOCK_WEB_PORT=20090 PM_WEB_PORT=20091 ./mvnw -pl pm-web-e2e verify -Pe2e`, `./mvnw -pl pm-api verify`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- CUI standards (`CLAUDE.md`, `PLAN-77`): a test that takes input data draws it from `Generators` with `@EnableGeneratorController` (the expected value of a contract stays literal); a log call at INFO or above goes through a `LogRecord` of the module's own `LogMessages` class (one per module that logs, never one for the repository) with a `LogAsserts` test; `@NullMarked` applies in a module that declares JSpecify (`pm-operator`, `pm-mock-runtime`); until `PLAN-77` has landed the test-scope CUI dependencies are missing in `pm-api`, `pm-exec`, `pm-relay` and `pm-mock-runtime`, and a test there follows plain JUnit and reports the departure, never adds the dependency itself.
- Frontend standards (`CLAUDE.md`, `PLAN-80`): `pm-dev-frontend` (`javascript`, `css`, `jest-testing`, `lint-config`, `javascript-security`) and `pm-dev-frontend-cui` (`cui-javascript-project`) apply to `pm-web-app` and `pm-web-e2e`; text from the runtime or from a model reaches the DOM through text-treating sinks only (no `innerHTML`, `outerHTML`, `insertAdjacentHTML`, `{@html}`); a test waits on a condition, never on an arbitrary timer; the tests run on Vitest with Testing Library and the `jest-testing` standards apply as method.
- Ports of this plan: mock web port `20090`, app port `20091` (`AGENTS.md` § Ports, made passable by `shared-port-assignment`, PLAN-76). Pass them to every mock, app and e2e start; other plans may run at the same time, so never stop or reuse a process this worktree did not start, and an assigned port that is in use is a blocker, never a reason to take another port.
- Views this plan touches pass the text guard of `web-text-guard-report`: the baseline file may only shrink, never grow.
- Browser check of the rendered output, with the Playwright MCP server of `opencode.json`, following `AGENTS.md` § Browser checks: start the mock (`./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario <scenario> --web-port 20090"`), then `PM_MOCK_WEB_PORT=20090 ./pm-web-app/start.sh -p 20091` (read the port from `pm-web-app/target/pm-web-app.port`); the MCP browser starts with an empty profile, so pair again every session with a fresh single-use code (`POST http://127.0.0.1:20090/mock/pairing`, then open `<app>/pair#code=<CODE>`), never through the `Network:` URL; reach a state with `POST /mock/goto/<state>` and a reload. Check each target in `de` and `en`, dark and light, at 1440 × 900 and 390 × 844 (`browser_resize`). Read the page through `browser_snapshot` (the accessibility tree is the evidence of text and structure) and keep one `browser_take_screenshot` per target; the server writes to its `--output-dir` `.plan/temp/playwright-mcp/` (`opencode.json`), so name every file `{plan}-{target}-{before|after}` and never write to the repository root. Take a baseline snapshot BEFORE the change and one AFTER, and record both paths in the progress record. The visible text of a checked view shows no raw enum, state id, `{placeholder}`, ISO time or emoji outside model-written blocks. Stop the app (`./pm-web-app/stop.sh`) and the mock afterwards. If the Playwright MCP server is not available in the run, the browser Done-when clauses are NOT met — a blocker reported as such, never replaced by a unit test or skipped.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/cross-cutting/plan.adoc#CC-S-02`, `doc/claude_review/cross-cutting/proposals.adoc#CC-P-01` — read at `doc/claude_review/cross-cutting/plan.adoc`
  - verdict: corroborated | checked_at: cc5794c9 | by: ui-improvements/cleanup | rescoped: n/a | evidence: origin/main doc/claude_review/cross-cutting/plan.adoc:22-27 states CC-S-02 (CC-P-01, CC-P-02); proposals.adoc:10 [#CC-P-01], :26 [#CC-P-02]
- HYPOTHESIS: views put API fields into the markup and no single display function exists — confirm/refute at `pm-web-app/src/lib/views/SystemView.svelte` (verify-at-outline)
  - verdict: corroborated | checked_at: cc5794c9 | by: ui-improvements/cleanup | rescoped: n/a | evidence: pm-web-app/src/lib/views/SystemView.svelte:951,955-956 print w.role and w.state.toLowerCase() raw; git grep displayState|displayError|displayUnit in pm-web-app/src finds nothing; no lib/display dir
- HYPOTHESIS: the strings of CC-F-05 live in the shared catalogue, not in the views — confirm/refute by searching `pm-api/src/main/resources/messages/` for `entfernen` and `Frage mich nach` (verify-at-outline)
  - verdict: corroborated | checked_at: cc5794c9 | by: ui-improvements/cleanup | rescoped: n/a | evidence: Strings sit in the catalogue: start.de.properties:176 'r) entfernen'; settings.de.properties:100 web.settings.cleanup.remind (spelled 'Früge mich nach'); view only composes keys at MachineSettingsView.svelte:335-336
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/display/index.ts` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-web-app/src/lib/display/enumFields.ts` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-web-app/src/lib/display/index.test.ts` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-web-app/src/lib/display/enumGuard.test.ts` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-web-app/src/lib/views/SystemView.svelte` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/views/PlanView.svelte` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/views/OverviewView.svelte` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/views/CleanupView.svelte` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/views/ReportsView.svelte` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/views/CompletionReportView.svelte` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/views/EpicView.svelte` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/views/StartView.svelte` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/views/MachineSettingsView.svelte` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-e2e/tests/text-guard-baseline.json` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-api/src/main/resources/messages/system.en.properties` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/system.de.properties` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/plan_view.en.properties` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/plan_view.de.properties` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/overview.en.properties` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/overview.de.properties` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/start.en.properties` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/start.de.properties` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/settings.en.properties` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/settings.de.properties` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/onboarding.en.properties` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/onboarding.de.properties` — present at HEAD c4dc9bb4

## Dependencies and Sequencing

- Depends on: `PLAN-06` (`shared-display-vocabulary`, shipped), `PLAN-01` (`web-text-guard-report`, shipped), `PLAN-76` (`shared-port-assignment`, shipped), `PLAN-86` (`shared-error-texts`)
- Overlaps with: none known at staging
- Adjacent to: none

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-09-web-display-layer.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
