# PLAN-83: Web: The Work List Beside a Plan and an Epic

epic: ui-improvements
workstream: WS-03

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

The work list of the work package `web-moving-between-plans` (`doc/plans/web-moving-between-plans.adoc`, merged with PR #278 on 2026-10-30; serves UC-PLN-1 and UC-PLN-2; PM-ARCH-11). In a window of the split breakpoint or wider (`doc/design/web-app/view-density-and-filtering.adoc#breakpoint`), the plan view and the epic view hold a collapsible list of the running work beside them: the same rows and markers as the overview, in the order served, the open item marked with `aria-current`. Moving to a neighbour takes one step. **Design first**: the list is not in the design yet; this plan writes it into the web app's design and has the operator accept it before building it.

The two other parts of that package were moved by the structure review of 2026-10-30, so that each stands with the work of its own kind: "Add a plan" on a project's row is deliverable 4 of `PLAN-59` (the project row is rebuilt there), and "only what is new, and together" for the notifications of decisions is deliverable 3 of `PLAN-51` (the decisions view). The package's row in `doc/plans/README.adoc` is set to `done` by the last of the three plans to land.

## Deliverables

1. **The work list beside a plan and an epic**
   - What: write the design of the work list (placement, the collapse and its remembered state per browser, the rows and markers of the overview, the open item and `aria-current`, a narrow window without it), have the operator accept it, then build it on the plan view and the epic view with the existing running-work store; the list keeps the order served (`PLAN-82`) and never sorts again.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security, pm-documents:ref-asciidoc
   - Done when:
     - The design is in `doc/design/web-app/` and the operator's acceptance is recorded in the progress record, verbatim (evidence order: `none`)
     - A component test of the list: rows, markers, `aria-current` on the open item, the collapse kept across a reload, the served order after an activity event (evidence order: `test-first`)
     - An e2e test: from a plan, one click on a neighbour opens its plan view; the same on an epic (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `running-work`: a plan view and an epic view at 1440 × 900 with the list, at 390 × 844 without it; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
     - The row of the package `web-moving-between-plans` in `doc/plans/README.adoc` is set to `done` if `PLAN-59` D4 and `PLAN-51` D3 have landed (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through npm scripts and Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-web-app verify`, `./mvnw -pl pm-web-e2e verify`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- Frontend standards (`CLAUDE.md`, `PLAN-80`): `pm-dev-frontend` (`javascript`, `css`, `jest-testing`, `lint-config`, `javascript-security`) and `pm-dev-frontend-cui` (`cui-javascript-project`) apply to `pm-web-app` and `pm-web-e2e`; text from the runtime or from a model reaches the DOM through text-treating sinks only (no `innerHTML`, `outerHTML`, `insertAdjacentHTML`, `{@html}`); a test waits on a condition, never on an arbitrary timer; the tests run on Vitest with Testing Library and the `jest-testing` standards apply as method.
- Ports of this plan: mock web port `20830`, app port `20831` (`AGENTS.md` § Ports, made passable by `shared-port-assignment`, PLAN-76). Pass them to every mock, app and e2e start; other plans may run at the same time, so never stop or reuse a process this worktree did not start, and an assigned port that is in use is a blocker, never a reason to take another port.
- A project's name and a plan's title may come from the project: plain text only, control sequences removed.
- The work list is added with the design accepted. If the operator does not accept it, the list is reported as not built, never built from an unaccepted design.
- Every new text goes through the message catalogue; the keys and the wording are those of the accepted design.
- Write-set discipline: the main checkout of this repository is never edited by the run; work only in the plan's worktree.
- Browser check of the rendered output, with the Playwright MCP server of `opencode.json`, following `AGENTS.md` § Browser checks: start the mock (`./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario <scenario> --web-port 20830"`), then `PM_MOCK_WEB_PORT=20830 ./pm-web-app/start.sh -p 20831` (read the port from `pm-web-app/target/pm-web-app.port`); the MCP browser starts with an empty profile, so pair again every session with a fresh single-use code (`POST http://127.0.0.1:20830/mock/pairing`, then open `<app>/pair#code=<CODE>`), never through the `Network:` URL; reach a state with `POST /mock/goto/<state>` and a reload. Check each target in `de` and `en`, dark and light, at 1440 × 900 and 390 × 844 (`browser_resize`). Read the page through `browser_snapshot` (the accessibility tree is the evidence of text and structure) and keep one `browser_take_screenshot` per target; the server writes to its `--output-dir` `.plan/temp/playwright-mcp/` (`opencode.json`), so name every file `{plan}-{target}-{before|after}` and never write to the repository root. Take a baseline snapshot BEFORE the change and one AFTER, and record both paths in the progress record. The visible text of a checked view shows no raw enum, state id, `{placeholder}`, ISO time or emoji outside model-written blocks. Stop the app (`./pm-web-app/stop.sh`) and the mock afterwards. If the Playwright MCP server is not available in the run, the browser Done-when clauses are NOT met — a blocker reported as such, never replaced by a unit test or skipped.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the web app's plan view has the path to its project and no neighbours — `PlanView.svelte` holds no sibling navigation — stated by the ThinkRail analysis (`doc/articles/thinkrail.adoc`), read 2026-10-30
- OBSERVED: the split breakpoint is defined at `doc/design/web-app/view-density-and-filtering.adoc#breakpoint` — read at HEAD 07104678
- Verify-first clause: before the deliverable, re-read the named files at the current HEAD and confirm or refute every claim above; `PLAN-52`, `PLAN-55` and `PLAN-58` (the running-work, plan and epic pages) edit the same views. A refuted claim re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/views/PlanView.svelte` — present at HEAD 07104678
- OBSERVED: `pm-web-app/src/lib/views/EpicView.svelte` — present at HEAD 07104678
- OBSERVED: `pm-web-app/src/lib/views/OverviewView.svelte` — present at HEAD 07104678
- OBSERVED: `pm-web-app/src/lib/shell/` — present at HEAD 07104678; the work list component is created by this plan
- OBSERVED: `pm-web-e2e/tests/plan-view.spec.js` — present at HEAD 07104678
- OBSERVED: `pm-web-e2e/tests/epic-view.spec.js` — present at HEAD 07104678
- OBSERVED: `doc/design/web-app/` — present at HEAD 07104678; the work list design is created by this plan
- OBSERVED: `doc/plans/README.adoc` — present at HEAD 07104678

## Dependencies and Sequencing

- Depends on: `PLAN-82` (the order the list keeps, served by the contract and the mock), `PLAN-39` (`web-app-shell`, shipped), `PLAN-76` (`shared-port-assignment`, shipped)
- Overlaps with: `PLAN-52` (`OverviewView.svelte`), `PLAN-55` (`PlanView.svelte`), `PLAN-58` (`EpicView.svelte`), `PLAN-09` (`OverviewView.svelte`): run one after the other
- Adjacent to: `PLAN-59` D4 and `PLAN-51` D3 (the other two parts of the package)

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-83-web-moving-between-plans.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
