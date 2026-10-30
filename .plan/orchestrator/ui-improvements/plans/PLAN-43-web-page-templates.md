# PLAN-43: Web: Four Page Templates and One Reference View

epic: ui-improvements
workstream: WS-03

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Build the four page templates of UX-P-03 (triage list, object page, wizard, dashboard) and move one view per template as reference: Decisions, Plan, Start, System (UX-S-04).

Source of this plan (read before scoping):

- `doc/claude_review/ux/plan.adoc (UX-S-04)`
- `doc/claude_review/ux/proposals.adoc#UX-P-03`
- `doc/claude_review/visual-design/proposals.adoc#VD-P-04`

## Deliverables

1. **Templates**
   - What: `lib/templates/TriagePage.svelte`, `ObjectPage.svelte`, `WizardPage.svelte`, `DashboardPage.svelte` with slots and the widths of UX-P-03; `doc/design/web-app/view-density-and-filtering.adoc` states that templates replace the layout modes (moved here from `PLAN-74`).
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - Unit tests per template: slots render, width as specified, triage split below 1100 px (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `fully-onboarded`: `/system` on the `DashboardPage` template at 1440 and 390 px (the other three templates are proved by their unit tests and by the plans that move their views); baseline and after snapshots plus screenshots recorded (evidence order: `none`)
2. **Reference views**
   - What: `SystemView` sits on the `DashboardPage` template without changing its content, as the one reference view. `DecisionView` (`TriagePage`), `PlanView` and `EpicView` (`ObjectPage`) and `StartView` (`WizardPage`) are moved by the plans that rebuild them (`PLAN-51`, `PLAN-55`, `PLAN-58`, `PLAN-53`), so none of them is moved twice (structure review of 2026-10-30).
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - The existing e2e spec of the system view passes unchanged (evidence order: `none`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `fully-onboarded`: `/system` on the `DashboardPage` template at 1440 and 390 px (the other three templates are proved by their unit tests and by the plans that move their views); baseline and after snapshots plus screenshots recorded (evidence order: `none`)
3. **No own page width**
   - What: A unit test greps view styles and fails on a `max-width` on the page root of a view that sits on a template; the list of such views grows as `PLAN-51`, `PLAN-53`, `PLAN-55` and `PLAN-58` move theirs, and each of those plans adds its view to the test.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - The test fails on a view with its own width before the move and passes for `SystemView` after (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `fully-onboarded`: `/system` on the `DashboardPage` template at 1440 and 390 px (the other three templates are proved by their unit tests and by the plans that move their views); baseline and after snapshots plus screenshots recorded (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-web-app verify`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- Frontend standards (`CLAUDE.md`, `PLAN-80`): `pm-dev-frontend` (`javascript`, `css`, `jest-testing`, `lint-config`, `javascript-security`) and `pm-dev-frontend-cui` (`cui-javascript-project`) apply to `pm-web-app` and `pm-web-e2e`; text from the runtime or from a model reaches the DOM through text-treating sinks only (no `innerHTML`, `outerHTML`, `insertAdjacentHTML`, `{@html}`); a test waits on a condition, never on an arbitrary timer; the tests run on Vitest with Testing Library and the `jest-testing` standards apply as method.
- Ports of this plan: mock web port `20430`, app port `20431` (`AGENTS.md` § Ports, made passable by `shared-port-assignment`, PLAN-76). Pass them to every mock, app and e2e start; other plans may run at the same time, so never stop or reuse a process this worktree did not start, and an assigned port that is in use is a blocker, never a reason to take another port.
- Every view this plan touches uses the design system v2 components (VD-S-06): no plain text button, no acting link, no state pill, no uppercase label, no emoji, no own page width.
- Every server value on a touched view passes through the display layer and the formatter of WS-02.
- Views this plan touches pass the text guard of `web-text-guard-report`: the baseline file may only shrink, never grow.
- The design document of `doc/design/` that this plan changes is updated in the same pull request (UX-S-13, VD-S-09).
- Browser check of the rendered output, with the Playwright MCP server of `opencode.json`, following `AGENTS.md` § Browser checks: start the mock (`./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario <scenario> --web-port 20430"`), then `PM_MOCK_WEB_PORT=20430 ./pm-web-app/start.sh -p 20431` (read the port from `pm-web-app/target/pm-web-app.port`); the MCP browser starts with an empty profile, so pair again every session with a fresh single-use code (`POST http://127.0.0.1:20430/mock/pairing`, then open `<app>/pair#code=<CODE>`), never through the `Network:` URL; reach a state with `POST /mock/goto/<state>` and a reload. Check each target in `de` and `en`, dark and light, at 1440 × 900 and 390 × 844 (`browser_resize`). Read the page through `browser_snapshot` (the accessibility tree is the evidence of text and structure) and keep one `browser_take_screenshot` per target; the server writes to its `--output-dir` `.plan/temp/playwright-mcp/` (`opencode.json`), so name every file `{plan}-{target}-{before|after}` and never write to the repository root. Take a baseline snapshot BEFORE the change and one AFTER, and record both paths in the progress record. The visible text of a checked view shows no raw enum, state id, `{placeholder}`, ISO time or emoji outside model-written blocks. Stop the app (`./pm-web-app/stop.sh`) and the mock afterwards. If the Playwright MCP server is not available in the run, the browser Done-when clauses are NOT met — a blocker reported as such, never replaced by a unit test or skipped.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/ux/plan.adoc (UX-S-04)`, `doc/claude_review/ux/proposals.adoc#UX-P-03` — read at `doc/claude_review/ux/plan.adoc`
  - verdict: corroborated | checked_at: cc5794c9 | by: ui-improvements/cleanup | rescoped: n/a | evidence: doc/claude_review/ux/plan.adoc:35-37 UX-S-04 maps to UX-P-03; ux/proposals.adoc:140-141 '== UX-P-03: Four Page Templates' exists
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/templates/` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-web-app/src/lib/views/SystemView.svelte` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/templates/pageWidth.test.ts` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `doc/design/web-app/view-density-and-filtering.adoc` — present at HEAD c4dc9bb4

## Dependencies and Sequencing

- Depends on: `PLAN-39` (`web-app-shell`), `PLAN-76` (`shared-port-assignment`)
- Overlaps with: none known at staging
- Adjacent to: none

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-43-web-page-templates.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
