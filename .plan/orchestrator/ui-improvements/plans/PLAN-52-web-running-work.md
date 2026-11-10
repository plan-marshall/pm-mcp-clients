# PLAN-52: Web: Running Work Across Projects

epic: ui-improvements
workstream: WS-03

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Rework running work (UX-P-09): all projects, fixed columns, row menu as icon button, epic group heads with progress, background jobs in the page head, ended work as a data table; plus the overview consistency of UC-P-06 and CC-P-08 (by plan and by epic from one list; counts from the rendered rows; read-only chat for ended work) (UX-S-09, overview part of UC-S-07 and CC-S-06).

Source of this plan (read before scoping):

- `doc/claude_review/ux/plan.adoc (UX-S-09)`
- `doc/claude_review/ux/proposals.adoc#UX-P-09`
- `doc/claude_review/use-cases/proposals.adoc#UC-P-06`
- `doc/claude_review/cross-cutting/proposals.adoc#CC-P-08`
- `doc/claude_review/ux/findings.adoc#UX-F-13`

## Deliverables

1. **Rows and groups**
   - What: Grouped by project then epic (toggle by plan / by epic kept); the project sidebar becomes a filter chip row; rows: kind icon, title, state, phase, since, progress, needs you; the row menu an icon button.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:javascript-security
   - Done when:
     - `overview.spec.js`: columns align at 1440 px; the row menu is an icon button (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `fully-onboarded` and `running-work` → `overview.running`: aligned columns at 1440 px, by plan and by epic, ended work table; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
2. **One list, one count**
   - What: By plan and by epic render from one list; head counts are `$derived` from the rendered list or say "showing n of m".
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - A unit test asserts both groupings hold the same plans and the epic head count equals its rows (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `fully-onboarded` and `running-work` → `overview.running`: aligned columns at 1440 px, by plan and by epic, ended work table; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
3. **Ended work and alongside**
   - What: Ended work is a sortable `DataTable` tab of the page, with "Chat about it" (read-only) per row; background jobs in a small block in the page head; the rail item always opens running work.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:javascript-security
   - Done when:
     - `overview.spec.js`: aligned ended-work columns; the rail item opens running work after ended work was open (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `fully-onboarded` and `running-work` → `overview.running`: aligned columns at 1440 px, by plan and by epic, ended work table; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-web-app verify`, `PM_MOCK_WEB_PORT=20520 PM_WEB_PORT=20521 ./mvnw -pl pm-web-e2e verify -Pe2e`, `./mvnw -pl pm-api verify`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- Frontend standards (`CLAUDE.md`, `PLAN-80`): `pm-dev-frontend` (`javascript`, `css`, `jest-testing`, `lint-config`, `javascript-security`) and `pm-dev-frontend-cui` (`cui-javascript-project`) apply to `pm-web-app` and `pm-web-e2e`; text from the runtime or from a model reaches the DOM through text-treating sinks only (no `innerHTML`, `outerHTML`, `insertAdjacentHTML`, `{@html}`); a test waits on a condition, never on an arbitrary timer; the tests run on Vitest with Testing Library and the `jest-testing` standards apply as method.
- Ports of this plan: mock web port `20520`, app port `20521` (`AGENTS.md` § Ports, made passable by `shared-port-assignment`, PLAN-76). Pass them to every mock, app and e2e start; other plans may run at the same time, so never stop or reuse a process this worktree did not start, and an assigned port that is in use is a blocker, never a reason to take another port.
- Every view this plan touches uses the design system v2 components (VD-S-06): no plain text button, no acting link, no state pill, no uppercase label, no emoji, no own page width.
- Every server value on a touched view passes through the display layer and the formatter of WS-02.
- Views this plan touches pass the text guard of `web-text-guard-report`: the baseline file may only shrink, never grow.
- The design document of `doc/design/` that this plan changes is updated in the same pull request (UX-S-13, VD-S-09).
- Browser check of the rendered output, with the Playwright MCP server of `opencode.json`, following `AGENTS.md` § Browser checks: start the mock (`./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario <scenario> --web-port 20520"`), then `PM_MOCK_WEB_PORT=20520 ./pm-web-app/start.sh -p 20521` (read the port from `pm-web-app/target/pm-web-app.port`); the MCP browser starts with an empty profile, so pair again every session with a fresh single-use code (`POST http://127.0.0.1:20520/mock/pairing`, then open `<app>/pair#code=<CODE>`), never through the `Network:` URL; reach a state with `POST /mock/goto/<state>` and a reload. Check each target in `de` and `en`, dark and light, at 1440 × 900 and 390 × 844 (`browser_resize`). Read the page through `browser_snapshot` (the accessibility tree is the evidence of text and structure) and keep one `browser_take_screenshot` per target; the server writes to its `--output-dir` `.plan/temp/playwright-mcp/` (`opencode.json`), so name every file `{plan}-{target}-{before|after}` and never write to the repository root. Take a baseline snapshot BEFORE the change and one AFTER, and record both paths in the progress record. The visible text of a checked view shows no raw enum, state id, `{placeholder}`, ISO time or emoji outside model-written blocks. Stop the app (`./pm-web-app/stop.sh`) and the mock afterwards. If the Playwright MCP server is not available in the run, the browser Done-when clauses are NOT met — a blocker reported as such, never replaced by a unit test or skipped.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/ux/plan.adoc (UX-S-09)`, `doc/claude_review/ux/proposals.adoc#UX-P-09` — read at `doc/claude_review/ux/plan.adoc`
  - verdict: corroborated | checked_at: 1cc0e10ec2ca617853b99a23dcfed8e5e2fed1bb | by: ui-improvements/cleanup | rescoped: n/a | evidence: `doc/claude_review/ux/plan.adoc` UX-S-09 and `ux/proposals.adoc:325` UX-P-09 exist.; re-grounded vs D1-D5 2026-11-10 (Amendment)
- Re-grounded note at HEAD 07104678 (2026-10-30): PR #278 changed the order rule in `doc/design/shared/plan-overview.adoc`: running work is shown in the order the runtime serves (needs the operator first, then the newest started; projects by the start of their oldest active work); activity moves no row and a client does not sort again; the time beside a row only says how recently it was active. The rows of this page keep that order (`PLAN-82` serves it in the contract and the mock; the former `PLAN-81` is superseded); a sort by activity in this plan's rows is a defect. `PLAN-83` adds a work list beside a plan and an epic that reuses these rows and markers.
- Re-grounded note at HEAD 94bbefe1 (cleanup 2026-11-02): PLAN-43 and PLAN-35 shipped, so the page-template and data-component dependencies are met; OverviewView, ChatPanel, mobile.spec.js and overview.spec.js were touched by PLAN-90 (e2e baseline, time residue) and the a11y baseline: re-read.
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.
- HYPOTHESIS: the ended-work list can be paged by the runtime through an existing endpoint the client already calls — confirm/refute at `pm-web-app/src/lib/api/client.ts` § ended work and `pm-api/src/main/java/de/planmarshall/api/overview` (verify-at-outline)

## Expected Surface


- OBSERVED: `pm-web-app/src/lib/views/OverviewView.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/OverviewView.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/stores/runtime.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/stores/runtime.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/api/client.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/ChatPanel.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/overview.spec.js` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/mobile.spec.js` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/shared/plan-overview.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/overview.en.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/overview.de.properties` — present at HEAD 1cc0e10e

## Dependencies and Sequencing

- Depends on: `PLAN-43` (`web-page-templates`), `PLAN-35` (`web-data-components`), `PLAN-03` (`mock-scenario-states`, shipped), `PLAN-76` (`shared-port-assignment`, shipped), `PLAN-82` (the served order in the mock)
- Overlaps with: none known at staging
- Adjacent to: none

## Design Review 2026-11-06 (Amendment)

Source: `inbox/archive/design-review/design-review-010.md` § Per-plan instructions (messages 003, 006, 007, 009). **Hold.
Gate: D1 and D2 merged** (documents `AGREED`) and the D2 navigation engine; its ended-work chat waits for the D4 web chat
package. Queue status: `parked`; the orchestrator re-grounds this spec when it is restarted. The Expected Surface above is NOT
widened now (a parked row is not in the disjointness gate population). When restarted:

- Running work goes onto the D2 engine: project list column, running / ended and by plan / by epic as URL modes, follow mode
  (003). **Its filter chip row (deliverable 1) and its ended-work `DataTable` tab (deliverable 3) are dropped** — the
  "project sidebar becomes a filter chip row" contradicts the project list column.
- Row slots (006), `RowLink` and `RowMenu` at the row (007), the ended-work chat read-only (009).
- Message 002: the start-page toggle is gone (`PLAN-110`); this plan never reintroduces it.

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-52-web-running-work.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.

## Re-grounding 2026-11-10 against the agreed D1–D5 designs (Amendment)

Source: `doc/design/shared/plan-overview.adoc` (#levels, #project-row, #item-rows, #ended, #persistence), `doc/design/web-app/navigation.adoc` (#levels, #url, #kinds), `doc/design/web-app/components.adoc` (#row-link, #row-menu), `doc/design/web-app/components-tags.adoc#tag-places`, `doc/design/web-app/chat.adoc#everywhere`. HEAD 1cc0e10e.

1. **Running work as the lens.** `OverviewView.svelte` becomes the Running work lens on the column engine: the projects with running work as the list column, then the project's work; by plan / by epic, running / ended and follow are modes of the address (`group=epic`, `mode=ended`, `follow=on`, `?project={id}`); a bare `/overview` is filled from the remembered place by the engine. Each row is a `RowLink` in the slots of every row (`KindMark`, title, `StatusLabel`, `PhaseLabel`, `ReasonBadge`, since, progress) with its acts in a `RowMenu`.
   - Done when: `overview.spec.js` on `running-work` → `overview.running`: the columns align at 1440 px, the row menu is an icon button, by plan and by epic hold the same plans (evidence order: `test-first`); the browser check on `fully-onboarded` and `running-work` (evidence order: `none`).
2. **One list, one count.** Unchanged: by plan and by epic render from one list; head counts are `$derived` from the rendered rows or say "showing n of m".
   - Done when: a unit test asserts both groupings hold the same plans and the epic head count equals its rows (evidence order: `test-first`).
3. **Ended work and alongside.** `mode=ended` lists every project with ended work, newest first, a page at a time (_Newer_, _Older_; the runtime pages); a row carries the outcome, the plan's pull request, the reason and the end time; the group `alongside` shows a running or failed short landing and a running onboarding or setup run on the project's row. A selection the list does not hold is a pinned row (`web.nav.pinned.*`).
   - Done when: `overview.spec.js`: aligned ended-work columns, paging, an ended plan opens read-only as it ended (evidence order: `test-first`); the open-issue read at the head (evidence order: `none`).
4. **Chat about ended work.** The row's _Chat_ and the ended plan's chat open the D4 `Chat` side column, read-only, with a working composer; its one proposal is a follow-up plan.
   - Done when: `overview.spec.js` opens it from a row (evidence order: `test-first`).

Removed or replaced: the filter chip row (the project list column replaces the sidebar); the sortable `DataTable` tab of ended work; the background-jobs block in the page head; "the rail item always opens running work" (conflicts with `plan-overview.adoc#persistence`; the operator decides, default: dropped); the start-page toggle (never reintroduced, `PLAN-110`).

Implements: `doc/design/shared/plan-overview.adoc` § Two Levels, A Project, Ended Work, Where the Operator Left It; `doc/design/web-app/navigation.adoc` § Levels as Columns, Addresses and History.

Dependencies: D1 foundation (RowLink, RowMenu, KindMark, StatusLabel, PhaseLabel, ReasonBadge), D2 navigation engine (column engine, modes in the address, remembered-place fill), and for deliverable 4 the D4 web chat package. It shares the running-work store with `PLAN-49`. Sequence after `PLAN-45`. `PLAN-82` and `PLAN-76` have shipped.

New claim bullets:

New claims of this re-grounding are in `## Claim Labels`.

## Operator Decisions 2026-11-10 (Amendment)

Recorded in the epic's decision log (AskUserQuestion, 2026-11-10). They settle the open points above and supersede any text here that differs.

- **Rail item (G7):** the design wins: a bare `/overview` is filled from the remembered place (`navigation.adoc#url`, `plan-overview.adoc#persistence`); the clause "the rail item opens running work after ended work was open" is dropped.
- **Chat adoption:** `PLAN-137` (web chat places) replaces every `ChatPanel` call site and the project-onboarding chat drawer and deletes `ChatPanel`. This plan drops its own chat deliverable or chat lines; it only places the `Chat` side column in its redesigned layout where the design shows it.
