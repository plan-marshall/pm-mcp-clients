# PLAN-37: Web: Accessibility Baseline and axe-core

epic: ui-improvements
workstream: WS-02

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Lay the accessibility baseline of CC-P-07 and add the automated audit with axe-core in `pm-web-e2e` (operator decision) (CC-S-05).

Source of this plan (read before scoping):

- `doc/claude_review/cross-cutting/plan.adoc#CC-S-05`
- `doc/claude_review/cross-cutting/proposals.adoc#CC-P-07`
- `doc/claude_review/cross-cutting/findings.adoc#CC-F-07`

## Deliverables

1. **Escape and focus**
   - What: Escape closes every panel, popover and dialog (pause panel, chat, overflow menu); focus returns to the opener.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript
   - Done when:
     - e2e keyboard checks in `workspace.spec.js` per panel (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `running-work` → `control.ready`: the pause panel, the chat and the overflow close with Escape (`browser_press_key`) and focus returns; disabled controls state their reason; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
2. **Reasons, labels, sizes**
   - What: A disabled control states its reason in its advisory and through `aria-describedby`; icon-only controls have a visible label from tablet width up and an `aria-label` always; minimum text 12 px; no uppercase transform; touch targets 44 px on the phone.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
   - Done when:
     - Unit tests on `WorkControlPanel` and `ChatPanel` for reason and label (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `running-work` → `control.ready`: the pause panel, the chat and the overflow close with Escape (`browser_press_key`) and focus returns; disabled controls state their reason; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
3. **axe-core audit**
   - What: Add `@axe-core/playwright` to `pm-web-e2e` and run it on every declared view; serious issues fail the run.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript
   - Done when:
     - `PM_MOCK_WEB_PORT=20370 PM_WEB_PORT=20371 ./mvnw -pl pm-web-e2e verify -Pe2e` passes with the audit on every declared view (evidence order: `none`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `running-work` → `control.ready`: the pause panel, the chat and the overflow close with Escape (`browser_press_key`) and focus returns; disabled controls state their reason; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-web-app verify`, `PM_MOCK_WEB_PORT=20370 PM_WEB_PORT=20371 ./mvnw -pl pm-web-e2e verify -Pe2e`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- Ports of this plan: mock web port `20370`, app port `20371` (`AGENTS.md` § Ports, made passable by `shared-port-assignment`, PLAN-76). Pass them to every mock, app and e2e start; other plans may run at the same time, so never stop or reuse a process this worktree did not start, and an assigned port that is in use is a blocker, never a reason to take another port.
- axe-core is the one new dependency the operator approved for this plan.
- Browser check of the rendered output, with the Playwright MCP server of `opencode.json`, following `AGENTS.md` § Browser checks: start the mock (`./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario <scenario> --web-port 20370"`), then `PM_MOCK_WEB_PORT=20370 ./pm-web-app/start.sh -p 20371` (read the port from `pm-web-app/target/pm-web-app.port`); the MCP browser starts with an empty profile, so pair again every session with a fresh single-use code (`POST http://127.0.0.1:20370/mock/pairing`, then open `<app>/pair#code=<CODE>`), never through the `Network:` URL; reach a state with `POST /mock/goto/<state>` and a reload. Check each target in `de` and `en`, dark and light, at 1440 × 900 and 390 × 844 (`browser_resize`). Read the page through `browser_snapshot` (the accessibility tree is the evidence of text and structure) and keep one `browser_take_screenshot` per target; the server writes to its `--output-dir` `.plan/temp/playwright-mcp/` (`opencode.json`), so name every file `{plan}-{target}-{before|after}` and never write to the repository root. Take a baseline snapshot BEFORE the change and one AFTER, and record both paths in the progress record. The visible text of a checked view shows no raw enum, state id, `{placeholder}`, ISO time or emoji outside model-written blocks. Stop the app (`./pm-web-app/stop.sh`) and the mock afterwards. If the Playwright MCP server is not available in the run, the browser Done-when clauses are NOT met — a blocker reported as such, never replaced by a unit test or skipped.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/cross-cutting/plan.adoc#CC-S-05`, `doc/claude_review/cross-cutting/proposals.adoc#CC-P-07` — read at `doc/claude_review/cross-cutting/plan.adoc`
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/components/WorkControlPanel.svelte` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/components/ChatPanel.svelte` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/components/WorkControlPanel.test.ts` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-e2e/tests/workspace.spec.js` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-e2e/tests/helpers/a11y.js` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-web-e2e/package.json` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-e2e/package-lock.json` — present at HEAD c4dc9bb4

## Dependencies and Sequencing

- Depends on: `PLAN-31` (`web-core-components-containers`), `PLAN-76` (`shared-port-assignment`)
- Overlaps with: none known at staging
- Adjacent to: none

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-37-web-accessibility-baseline.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
