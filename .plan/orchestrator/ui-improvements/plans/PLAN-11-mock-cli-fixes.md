# PLAN-11: Mock: Plan Details per Id, Routes, Lifecycle State

epic: ui-improvements
workstream: WS-01

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Close the mock gaps the CLI review found: plan details per id with `404 plan_not_found` for an unknown id, the routes the clients call in every scenario, the lifecycle state of `system.*` in `/api/v1/status`, and the socket name in the README (CLI-UC-S-09, CLI-UC-P-08).

Source of this plan (read before scoping):

- `doc/claude_review/cli/use-cases/plan.adoc#CLI-UC-S-09`
- `doc/claude_review/cli/use-cases/proposals.adoc#CLI-UC-P-08`
- `doc/claude_review/cli/cross-cutting/findings.adoc#CLI-CC-F-10`

## Deliverables

1. **Plan details per id**
   - What: The mock returns the details of the plan with the requested id and `404 plan_not_found` for an unknown id.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:junit-core
   - Done when:
     - A mock test asks for two plan ids and an unknown id and gets two different plans and a `404 plan_not_found` (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `running-work` → `/plans/<unknown id>` shows not found; `reports-and-system` → `system.draining` shows the draining lifecycle; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
2. **Routes and lifecycle state**
   - What: `/api/v1/plans` answers in every scenario (an empty list on purpose where the scenario has none); `/api/v1/status` carries `lifecycle_state` for the `system.*` states (`draining`, `restarting`, `stopped`). Re-grounded at HEAD cc5794c9: `mock-scenario-states` already made `goto` apply `lifecycle_state` (`reports-and-system.json` carries `running`, `draining`, `stopped`, `upgrading` and `stopping`; `MockRuntimeEngine` reads it from a state's facts), so this plan adds only what a read of the mock at HEAD still lacks: check `restarting` first, and that `/api/v1/status` serves the value in each state. Also fold in a residue of `mock-scenario-states`: `MockRuntimeEngine.java:7708` (HEAD a2f4d2c4; `cli-choice-prompt-pause` moved it from 7663) writes a kept cleanup item with `kind` `item`, which is outside the closed set `CleanupKind` (`plan`, `epic`, `logs`, `read_reports`, `unenrolled_project`) — write a member of that set.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:junit-core
   - Done when:
     - The scenario contract test of `mock-scenario-states` covers the routes and the lifecycle state of every `system.*` state (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `running-work` → `/plans/<unknown id>` shows not found; `reports-and-system` → `system.draining` shows the draining lifecycle; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
3. **README socket name**
   - What: `pm-mock-runtime/README.adoc` names `run/runtime.sock`, not `pm-mcpd.sock`.
   - Persona: plan-marshall:persona-documenter
   - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation
   - Done when:
     - The README names the socket the mock creates (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-mock-runtime verify`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- Ports of this plan: mock web port `20110`, app port `20111` (`AGENTS.md` § Ports, made passable by `shared-port-assignment`, PLAN-76). Pass them to every mock, app and e2e start; other plans may run at the same time, so never stop or reuse a process this worktree did not start, and an assigned port that is in use is a blocker, never a reason to take another port.
- Browser check of the rendered output, with the Playwright MCP server of `opencode.json`, following `AGENTS.md` § Browser checks: start the mock (`./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario <scenario> --web-port 20110"`), then `PM_MOCK_WEB_PORT=20110 ./pm-web-app/start.sh -p 20111` (read the port from `pm-web-app/target/pm-web-app.port`); the MCP browser starts with an empty profile, so pair again every session with a fresh single-use code (`POST http://127.0.0.1:20110/mock/pairing`, then open `<app>/pair#code=<CODE>`), never through the `Network:` URL; reach a state with `POST /mock/goto/<state>` and a reload. Check each target in `de` and `en`, dark and light, at 1440 × 900 and 390 × 844 (`browser_resize`). Read the page through `browser_snapshot` (the accessibility tree is the evidence of text and structure) and keep one `browser_take_screenshot` per target; the server writes to its `--output-dir` `.plan/temp/playwright-mcp/` (`opencode.json`), so name every file `{plan}-{target}-{before|after}` and never write to the repository root. Take a baseline snapshot BEFORE the change and one AFTER, and record both paths in the progress record. The visible text of a checked view shows no raw enum, state id, `{placeholder}`, ISO time or emoji outside model-written blocks. Stop the app (`./pm-web-app/stop.sh`) and the mock afterwards. If the Playwright MCP server is not available in the run, the browser Done-when clauses are NOT met — a blocker reported as such, never replaced by a unit test or skipped.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/cli/use-cases/plan.adoc#CLI-UC-S-09`, `doc/claude_review/cli/use-cases/proposals.adoc#CLI-UC-P-08` — read at `doc/claude_review/cli/use-cases/plan.adoc`
  - verdict: corroborated | checked_at: cc5794c9 | by: ui-improvements/cleanup | rescoped: n/a | evidence: origin/main doc/claude_review/cli/use-cases/plan.adoc:92 [#CLI-UC-S-09] Mock Fixes; proposals.adoc:137 [#CLI-UC-P-08] exist
- HYPOTHESIS: the mock returns the same plan for every plan id and has no routes for `/api/v1/config/<aspect>` and `/api/v1/roles` — confirm/refute at `pm-mock-runtime/src/main/java/de/planmarshall/mock/http/` (verify-at-outline)
  - verdict: corroborated | checked_at: cc5794c9 | by: ui-improvements/cleanup | rescoped: n/a | evidence: MockRuntimeEngine.java:8263-8275 serves the fix-relay process copy for any plan id; no /api/v1/config or /api/v1/roles route in pm-mock-runtime/src/main (git grep empty)
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/http/` — present at HEAD c4dc9bb4
- OBSERVED: `pm-mock-runtime/src/main/resources/scenarios/reports-and-system.json` — present at HEAD c4dc9bb4
- OBSERVED: `pm-mock-runtime/README.adoc` — present at HEAD c4dc9bb4
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/runtime/` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-e2e/tests/plan-view.spec.js` — present at HEAD cc5794c9; it asserts mock fixture values (`mock-scenario-states` had to edit it for that reason), so a plan detail that differs per id can change what it reads; touched only if an assertion it holds changes
- OBSERVED: `pm-web-e2e/tests/system.spec.js` — present at HEAD cc5794c9; it exercises the system view's lifecycle modal on the mock, so a lifecycle state the mock newly serves can change what it reads; touched only if an assertion it holds changes

## Dependencies and Sequencing

- Depends on: `PLAN-03` (`mock-scenario-states`), `PLAN-76` (`shared-port-assignment`)
- Overlaps with: `mock-scenario-states` (same engine and README): runs after it
- Adjacent to: none

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-11-mock-cli-fixes.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
