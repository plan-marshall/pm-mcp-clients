# PLAN-45: Web: One Error Pattern, One Loading Pattern

epic: ui-improvements
workstream: WS-03

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

No path ends in a raw error code or a loading text without end: the error pattern by error code, request time limits, and skeletons that name what loads (UX-S-06; UX-P-08).

Source of this plan (read before scoping):

- `doc/claude_review/ux/plan.adoc (UX-S-06)`
- `doc/claude_review/ux/proposals.adoc#UX-P-08`
- `doc/claude_review/ux/findings.adoc#UX-F-06`
- `doc/claude_review/ux/findings.adoc#UX-F-14`

## Deliverables

1. **Error texts by code**
   - What: `ErrorState` shows the title, the cause and the fix of the code from the shared set `PLAN-86` (`error.<code>.title`, `.cause`, `.fix` in `de` and `en`, read through `displayError(code)` of `PLAN-09`); an unknown code uses `error.unknown.*` and shows the code under "Details". This plan writes no error text: a missing key is reported to `PLAN-86`, not added here.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:javadoc, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-dev-java-cui:cui-logging
   - Done when:
     - A unit test: `ErrorState` for a known code shows its title, cause and fix in `de` and `en`; for an unknown code the fallback and the code under "Details" (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `/plans/does-not-exist`; `/start` for an unknown project; stopping the mock while a view loads shows the pattern within 15 s; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
2. **Time limits**
   - What: `lib/api/client.ts`: 15 s per request, 30 s for a check, ending in an error with code `timeout`.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - A unit test with a request that never answers gets `timeout` after the limit (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `/plans/does-not-exist`; `/start` for an unknown project; stopping the mock while a view loads shows the pattern within 15 s; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
3. **Views on the pattern**
   - What: Every view shows `ErrorState` for a failed load and `Skeleton` while loading, with the loading text as an `<area>.loading` key that names what loads (the keys formerly planned in `PLAN-09` D4: this plan owns every loading text); `plan_not_found` and `project_not_found` show title, cause and an act.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:javascript-security
   - Done when:
     - e2e: `/plans/does-not-exist` shows the pattern; stopping the mock shows it within 15 s with "Try again" (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `/plans/does-not-exist`; `/start` for an unknown project; stopping the mock while a view loads shows the pattern within 15 s; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-web-app verify`, `PM_MOCK_WEB_PORT=20450 PM_WEB_PORT=20451 ./mvnw -pl pm-web-e2e verify -Pe2e`, `./mvnw -pl pm-api verify`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- CUI standards (`CLAUDE.md`, `PLAN-77`): a test that takes input data draws it from `Generators` with `@EnableGeneratorController` (the expected value of a contract stays literal); a log call at INFO or above goes through a `LogRecord` of the module's own `LogMessages` class (one per module that logs, never one for the repository) with a `LogAsserts` test; `@NullMarked` applies in a module that declares JSpecify (`pm-operator`, `pm-mock-runtime`); until `PLAN-77` has landed the test-scope CUI dependencies are missing in `pm-api`, `pm-exec`, `pm-relay` and `pm-mock-runtime`, and a test there follows plain JUnit and reports the departure, never adds the dependency itself.
- Frontend standards (`CLAUDE.md`, `PLAN-80`): `pm-dev-frontend` (`javascript`, `css`, `jest-testing`, `lint-config`, `javascript-security`) and `pm-dev-frontend-cui` (`cui-javascript-project`) apply to `pm-web-app` and `pm-web-e2e`; text from the runtime or from a model reaches the DOM through text-treating sinks only (no `innerHTML`, `outerHTML`, `insertAdjacentHTML`, `{@html}`); a test waits on a condition, never on an arbitrary timer; the tests run on Vitest with Testing Library and the `jest-testing` standards apply as method.
- Ports of this plan: mock web port `20450`, app port `20451` (`AGENTS.md` § Ports, made passable by `shared-port-assignment`, PLAN-76). Pass them to every mock, app and e2e start; other plans may run at the same time, so never stop or reuse a process this worktree did not start, and an assigned port that is in use is a blocker, never a reason to take another port.
- Every view this plan touches uses the design system v2 components (VD-S-06): no plain text button, no acting link, no state pill, no uppercase label, no emoji, no own page width.
- Every server value on a touched view passes through the display layer and the formatter of WS-02.
- Views this plan touches pass the text guard of `web-text-guard-report`: the baseline file may only shrink, never grow.
- The design document of `doc/design/` that this plan changes is updated in the same pull request (UX-S-13, VD-S-09).
- Browser check of the rendered output, with the Playwright MCP server of `opencode.json`, following `AGENTS.md` § Browser checks: start the mock (`./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario <scenario> --web-port 20450"`), then `PM_MOCK_WEB_PORT=20450 ./pm-web-app/start.sh -p 20451` (read the port from `pm-web-app/target/pm-web-app.port`); the MCP browser starts with an empty profile, so pair again every session with a fresh single-use code (`POST http://127.0.0.1:20450/mock/pairing`, then open `<app>/pair#code=<CODE>`), never through the `Network:` URL; reach a state with `POST /mock/goto/<state>` and a reload. Check each target in `de` and `en`, dark and light, at 1440 × 900 and 390 × 844 (`browser_resize`). Read the page through `browser_snapshot` (the accessibility tree is the evidence of text and structure) and keep one `browser_take_screenshot` per target; the server writes to its `--output-dir` `.plan/temp/playwright-mcp/` (`opencode.json`), so name every file `{plan}-{target}-{before|after}` and never write to the repository root. Take a baseline snapshot BEFORE the change and one AFTER, and record both paths in the progress record. The visible text of a checked view shows no raw enum, state id, `{placeholder}`, ISO time or emoji outside model-written blocks. Stop the app (`./pm-web-app/stop.sh`) and the mock afterwards. If the Playwright MCP server is not available in the run, the browser Done-when clauses are NOT met — a blocker reported as such, never replaced by a unit test or skipped.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/ux/plan.adoc (UX-S-06)`, `doc/claude_review/ux/proposals.adoc#UX-P-08` — read at `doc/claude_review/ux/plan.adoc`
  - verdict: corroborated | checked_at: cc5794c9 | by: ui-improvements/cleanup | rescoped: n/a | evidence: doc/claude_review/ux/plan.adoc:47-49 UX-S-06 maps to UX-P-08; ux/proposals.adoc:314-315 '== UX-P-08: One Error Pattern, One Loading Pattern' exists
- Re-grounded note at HEAD 5f9e60da (2026-10-31): `web-display-layer` (PLAN-09, shipped, PR #287) landed `displayError(code)` (tested, both languages, the unknown fallback) and the enum guard, and `shared-error-texts` (PLAN-86, shipped) the `error.<code>.*` texts; `displayError` has no production caller yet, so `ErrorState` is its first one. The loading texts (`<area>.loading`) are this plan's, not PLAN-09's. The enum guard fails on a field of `enumFields.ts` printed without `displayState`: an error view must not print a code any other way.
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/api/client.ts` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/api/client.test.ts` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/views/` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-e2e/tests/chrome.spec.js` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/common.en.properties` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/common.de.properties` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/resource/ErrorTextsTest.java` — present at HEAD 1b6c6ae3 (created by `PLAN-86`, which owns the completeness check of the error texts: every `ApiErrorCode` member has title, cause and fix in both languages); this plan builds no second one

## Dependencies and Sequencing

- Depends on: `PLAN-35` (`web-data-components`), `PLAN-39` (`web-app-shell`, shipped), `PLAN-76` (`shared-port-assignment`, shipped), `PLAN-86` (`shared-error-texts`), `PLAN-09` (`displayError`)
- Overlaps with: none known at staging
- Adjacent to: none

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-45-web-error-loading-pattern.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
