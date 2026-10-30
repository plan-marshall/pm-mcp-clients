# PLAN-88: Web: Process Designer in Three Panes

epic: ui-improvements
workstream: WS-03

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Give the process designer three panes with the results above the text, each finding linked to its line (designer part of UX-S-11; UX-P-10). The designer was the second half of `PLAN-54` (reports and designer); the structure review of 2026-10-30 split it off, because the two views share nothing but the review source: different views, different documents, different catalogue areas, different test files. `PLAN-54` keeps the reports.

Source of this plan (read before scoping):

- `doc/claude_review/ux/proposals.adoc#UX-P-10`
- `doc/claude_review/ux/findings.adoc#UX-F-15`

## Deliverables

1. **Designer**
   - What: Three resizable panes; the results panel docked above the text, each finding linked to its line; installed units as a compact list with an overflow menu; "Back to the designer" removed.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - `designer.spec.js` on `designer.unit-invalid`: results above the text; a finding jumps to its line (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `designer` → `designer.unit-invalid`; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through npm scripts and Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-web-app verify`, `./mvnw -pl pm-web-e2e verify`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- Frontend standards (`CLAUDE.md`, `PLAN-80`): `pm-dev-frontend` (`javascript`, `css`, `jest-testing`, `lint-config`, `javascript-security`) and `pm-dev-frontend-cui` (`cui-javascript-project`) apply to `pm-web-app` and `pm-web-e2e`; text from the runtime or from a model reaches the DOM through text-treating sinks only (no `innerHTML`, `outerHTML`, `insertAdjacentHTML`, `{@html}`); a test waits on a condition, never on an arbitrary timer; the tests run on Vitest with Testing Library and the `jest-testing` standards apply as method.
- Ports of this plan: mock web port `20880`, app port `20881` (`AGENTS.md` § Ports, made passable by `shared-port-assignment`, PLAN-76). Pass them to every mock, app and e2e start; other plans may run at the same time, so never stop or reuse a process this worktree did not start, and an assigned port that is in use is a blocker, never a reason to take another port.
- Every view this plan touches uses the design system v2 components (VD-S-06): no plain text button, no acting link, no state pill, no uppercase label, no emoji, no own page width.
- Every server value on a touched view passes through the display layer and the formatter of WS-02.
- Views this plan touches pass the text guard of `web-text-guard-report`: the baseline file may only shrink, never grow.
- The design document of `doc/design/` that this plan changes is updated in the same pull request (UX-S-13, VD-S-09): `doc/design/web-app/process-designer.adoc`.
- Write-set discipline: the main checkout of this repository is never edited by the run; work only in the plan's worktree.
- Browser check of the rendered output, with the Playwright MCP server of `opencode.json`, following `AGENTS.md` § Browser checks: start the mock (`./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario <scenario> --web-port 20880"`), then `PM_MOCK_WEB_PORT=20880 ./pm-web-app/start.sh -p 20881` (read the port from `pm-web-app/target/pm-web-app.port`); the MCP browser starts with an empty profile, so pair again every session with a fresh single-use code (`POST http://127.0.0.1:20880/mock/pairing`, then open `<app>/pair#code=<CODE>`), never through the `Network:` URL; reach a state with `POST /mock/goto/<state>` and a reload. Check each target in `de` and `en`, dark and light, at 1440 × 900 and 390 × 844 (`browser_resize`). Read the page through `browser_snapshot` (the accessibility tree is the evidence of text and structure) and keep one `browser_take_screenshot` per target; the server writes to its `--output-dir` `.plan/temp/playwright-mcp/` (`opencode.json`), so name every file `{plan}-{target}-{before|after}` and never write to the repository root. Take a baseline snapshot BEFORE the change and one AFTER, and record both paths in the progress record. The visible text of a checked view shows no raw enum, state id, `{placeholder}`, ISO time or emoji outside model-written blocks. Stop the app (`./pm-web-app/stop.sh`) and the mock afterwards. If the Playwright MCP server is not available in the run, the browser Done-when clauses are NOT met — a blocker reported as such, never replaced by a unit test or skipped.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/ux/proposals.adoc#UX-P-10`, `doc/claude_review/ux/findings.adoc#UX-F-15` — read at `doc/claude_review/ux/proposals.adoc` (the designer part; the claim was corroborated at `cc5794c9` for the former `PLAN-54`)
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before the deliverable, re-read the named files at the current HEAD and confirm or refute every claim above; a refuted claim re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/views/ProcessDesigner.svelte` — present at HEAD 07104678
- OBSERVED: `pm-web-app/src/lib/components/WorkflowValidationResults.svelte` — present at HEAD 07104678
- OBSERVED: `pm-web-e2e/tests/designer.spec.js` — present at HEAD 07104678
- OBSERVED: `doc/design/web-app/process-designer.adoc` — present at HEAD 07104678
- OBSERVED: `pm-api/src/main/resources/messages/designer.en.properties` — present at HEAD 07104678
- OBSERVED: `pm-api/src/main/resources/messages/designer.de.properties` — present at HEAD 07104678

## Dependencies and Sequencing

- Depends on: `PLAN-43` (`web-page-templates`), `PLAN-76` (`shared-port-assignment`, shipped)
- Overlaps with: none known
- Adjacent to: `PLAN-54` (reports; the other half of the former plan)

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-88-web-designer-panes.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
