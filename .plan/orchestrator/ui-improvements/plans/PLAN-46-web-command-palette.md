# PLAN-46: Web: Command Palette, Jump by Name

epic: ui-improvements
workstream: WS-03

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Reach a plan, an epic, a decision, a project or a report by id, title or pull request with `⌘K`/`Ctrl+K` or `/` (UX-S-03; UX-P-02; serves UC-PLN-8).

Source of this plan (read before scoping):

- `doc/claude_review/ux/plan.adoc (UX-S-03)`
- `doc/claude_review/ux/proposals.adoc#UX-P-02`
- `doc/claude_review/use-cases/proposals.adoc#UC-P-10`

## Deliverables

1. **Search index**
   - What: `lib/stores/searchIndex.ts` is the client of the runtime's search (`GET /api/v1/find?q=`, built by `PLAN-82`): it sends the text (debounced, a newer text cancels the older request, an empty text sends nothing), and holds the matches in the order served (running work first, ended work marked, by kind: project, epic, plan, decision, report; a pull request number finds its plan). It builds no index from the stores and loads no archive: the runtime answers over running and ended work.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - `searchIndex.test.ts`: "relay" finds the epic, its plans and decisions; "214" finds the plan of PR 214 (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `fully-onboarded`: `⌘K`/`Ctrl+K`, "relay", `Enter`; "214" finds the plan of PR 214; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
2. **Palette**
   - What: `lib/shell/CommandPalette.svelte`: one field, results grouped by kind, acts such as "Start a plan in …", `Enter` opens; "no match" keeps the name.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:javascript-security
   - Done when:
     - `chrome.spec.js`: `⌘K`, "relay", `Enter` opens the first match (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `fully-onboarded`: `⌘K`/`Ctrl+K`, "relay", `Enter`; "214" finds the plan of PR 214; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-web-app verify`, `PM_MOCK_WEB_PORT=20460 PM_WEB_PORT=20461 ./mvnw -pl pm-web-e2e verify -Pe2e`, `./mvnw -pl pm-api verify`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- Frontend standards (`CLAUDE.md`, `PLAN-80`): `pm-dev-frontend` (`javascript`, `css`, `jest-testing`, `lint-config`, `javascript-security`) and `pm-dev-frontend-cui` (`cui-javascript-project`) apply to `pm-web-app` and `pm-web-e2e`; text from the runtime or from a model reaches the DOM through text-treating sinks only (no `innerHTML`, `outerHTML`, `insertAdjacentHTML`, `{@html}`); a test waits on a condition, never on an arbitrary timer; the tests run on Vitest with Testing Library and the `jest-testing` standards apply as method.
- Ports of this plan: mock web port `20460`, app port `20461` (`AGENTS.md` § Ports, made passable by `shared-port-assignment`, PLAN-76). Pass them to every mock, app and e2e start; other plans may run at the same time, so never stop or reuse a process this worktree did not start, and an assigned port that is in use is a blocker, never a reason to take another port.
- Every view this plan touches uses the design system v2 components (VD-S-06): no plain text button, no acting link, no state pill, no uppercase label, no emoji, no own page width.
- Every server value on a touched view passes through the display layer and the formatter of WS-02.
- Views this plan touches pass the text guard of `web-text-guard-report`: the baseline file may only shrink, never grow.
- The design document of `doc/design/` that this plan changes is updated in the same pull request (UX-S-13, VD-S-09).
- UC-PLN-8 is `AGREED` (`plan-marshall-documentation` PR #135, 2026-10-30). The search itself is the runtime's (`PLAN-82` builds the contract and the mock); if `PLAN-82` has not landed, this plan stops as a blocker. The palette enables the search button of the top bar that `PLAN-39` shipped disabled (`web.frame.search.pending`) and removes that text. A title in the results is plain text, control sequences removed.
- Browser check of the rendered output, with the Playwright MCP server of `opencode.json`, following `AGENTS.md` § Browser checks: start the mock (`./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario <scenario> --web-port 20460"`), then `PM_MOCK_WEB_PORT=20460 ./pm-web-app/start.sh -p 20461` (read the port from `pm-web-app/target/pm-web-app.port`); the MCP browser starts with an empty profile, so pair again every session with a fresh single-use code (`POST http://127.0.0.1:20460/mock/pairing`, then open `<app>/pair#code=<CODE>`), never through the `Network:` URL; reach a state with `POST /mock/goto/<state>` and a reload. Check each target in `de` and `en`, dark and light, at 1440 × 900 and 390 × 844 (`browser_resize`). Read the page through `browser_snapshot` (the accessibility tree is the evidence of text and structure) and keep one `browser_take_screenshot` per target; the server writes to its `--output-dir` `.plan/temp/playwright-mcp/` (`opencode.json`), so name every file `{plan}-{target}-{before|after}` and never write to the repository root. Take a baseline snapshot BEFORE the change and one AFTER, and record both paths in the progress record. The visible text of a checked view shows no raw enum, state id, `{placeholder}`, ISO time or emoji outside model-written blocks. Stop the app (`./pm-web-app/stop.sh`) and the mock afterwards. If the Playwright MCP server is not available in the run, the browser Done-when clauses are NOT met — a blocker reported as such, never replaced by a unit test or skipped.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/ux/plan.adoc (UX-S-03)`, `doc/claude_review/ux/proposals.adoc#UX-P-02` — read at `doc/claude_review/ux/plan.adoc`
  - verdict: corroborated | checked_at: cc5794c9 | by: ui-improvements/cleanup | rescoped: n/a | evidence: doc/claude_review/ux/plan.adoc:30-32 UX-S-03 maps to UX-P-02; ux/proposals.adoc:73-74 UX-P-02 (shell, command palette) exists
- Re-grounded at HEAD 07104678 (2026-10-30) after PR #278, PLAN-39 (PR #274) and `plan-marshall-documentation` PR #135: UC-PLN-8 is `AGREED` ("Go to Work by Name": a part of an id, a title or a pull request number; matches by kind, running work first; ended work marked; no match says so and keeps the name) and the specification serves the search (`runtime-model/01-processes-and-transport.adoc#find-work`), so the client index of this spec is replaced by a client of `GET /api/v1/find`. `TopBar.svelte` exists with the search button disabled; the shell directory `lib/shell/` exists. The package `needs-and-find` designs the view; `PLAN-84` is the terminal half.
- Re-grounded at HEAD 1651554b (2026-10-31), after `PLAN-82` landed (PR #284): `PLAN-82` shipped `GET /api/v1/find`, the matches
  (`FindKind`, `FindMatch`, `FindPage`) and the mock, and the operator accepted the design this plan implements:
  `doc/design/web-app/needs-and-find.adoc` § "The Search Button of the Top Bar" (`#search`) and
  `doc/design/shared/needs-and-find.adoc` § "Going to Work by Name" (`#find`). Read both before scoping; they fix what the
  deliverables above leave open: the panel opens over the view and is never a route; `Ctrl+K` and `/` (while nothing in a field
  has the focus) open it, `Esc` closes it, the field has the focus on open; the request goes out when the text pauses, never per
  keystroke, and the last answer wins; matches open with `↑`, `↓`, `Enter` or the pointer and the selection is anchored to the
  match's kind and id, not to its row; every title is a text node; on the phone the panel covers the view. OBSERVED (grep of
  `pm-api/src/main/resources/messages/*.properties` at HEAD 1651554b): the catalogues hold `web.frame.search` and
  `web.frame.search.pending` and NONE of the texts the designs name for this panel: `web.frame.search.title` (web design) and every
  `overview.find.*` row of the shared design's Texts table (`title`, `hint`, `empty`, `ended`, `no_text`, the `kind.*` words and
  the rest of that table). OBSERVED: no staged spec named them before this note, so they had no owner. DECISION OF THE ORCHESTRATOR
  (open to the operator): this plan adds them, in `en` and `de`, with the wording of the design's tables, and retires
  `web.frame.search.pending`; `PLAN-84` (terminal `find`) reads them at its head, or adds the ones still missing if it runs first.
  Both plans therefore declare `overview.{en,de}.properties`; they do not run at the same time.
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/stores/searchIndex.ts` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-web-app/src/lib/stores/searchIndex.test.ts` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-web-app/src/lib/shell/CommandPalette.svelte` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-web-app/src/lib/shell/TopBar.svelte` — present at HEAD 07104678 (created by PLAN-39, PR #274); this plan enables its search button
- OBSERVED: `pm-web-e2e/tests/chrome.spec.js` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/common.en.properties` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/common.de.properties` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/overview.en.properties` — present at HEAD 1651554b; this plan adds the `overview.find.*` rows
- OBSERVED: `pm-api/src/main/resources/messages/overview.de.properties` — present at HEAD 1651554b; this plan adds the `overview.find.*` rows

## Dependencies and Sequencing

- Depends on: `PLAN-39` (`web-app-shell`, shipped), `PLAN-08` (`use-case-documents`, shipped), `PLAN-76` (`shared-port-assignment`, shipped), `PLAN-82` (`needs-and-find-contract`)
- Overlaps with: `PLAN-84` (the `overview.{en,de}.properties` pair, where the find texts are added): run one after the other
- Adjacent to: none

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-46-web-command-palette.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
