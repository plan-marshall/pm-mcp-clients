# PLAN-99: Web: What the Reports Plan Left Open

epic: ui-improvements
workstream: WS-03

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.
> Staged 2026-11-05 from the landing analysis of PLAN-54 (`landings/PLAN-54.md`: PR #313 conforms to the merge rule, but two defects, a spec deviation and several tests that cannot fail are open). One pull request on purpose: each pull request costs a CodeRabbit review. Keep it under 100 files.

## Objective

PLAN-54 gave the reports page an unread dot, a row menu, a machine group and the completion report's `Change` line. Its review was handled, but "mark all shown read" marks rows the filter hides, the clamped body has no control after a breakpoint change, the filter slot and the headings are not right, and the tests of the behaviour added in the last commits cannot fail. This plan fixes the defects and makes the tests able to fail.

## Deliverables

1. **The reports list does what its controls say**
   - What: (a) `markAllShownRead` (`ReportsView.svelte` ~496-499) posts every loaded item (`items.map`) and empties the list; it marks only the rows the filter shows (`matchingItems`), keeps the others, and the count and the retained-read bookkeeping follow. (b) The "show the text" control is measured again when the width changes (`ReportsView.svelte` ~164-178 measures on `items`, `query` and `activeDetailPlanId` only): after a breakpoint change a body that starts to wrap has its control, and one that stops wrapping loses it. (c) `<div slot="filters">` (~887) is rendered only when there is a filter to show, so `TriagePage`'s `$$slots.filters` is false otherwise and no empty container is drawn while loading or with no items. (d) One `<h1>` is on screen when the detail is open beside the list (`ReportsView.svelte` ~904 and `h1.report-title` of `CompletionReportView.svelte`): check the heading rules of the template and fix the level of one of them. (e) The show/hide control carries the row it belongs to in its accessible name, and the row menu has a label.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
   - Done when:
     - A test per item (a) to (e) that fails before: with a filter active, mark-all posts only the shown ids and keeps the hidden rows; a resize adds and removes the control; no filter container exists with no filter; one `h1`; the control and the menu have distinct names (evidence order: `test-first`)
     - Browser check (Playwright MCP, `AGENTS.md` § Browser checks) on `fully-onboarded` → `/reports` with a filter and mark-all, and a breakpoint change from 1440 × 900 to 390 × 844, `de` and `en`, light and dark, before and after (evidence order: `none`)
2. **The tests of the reports page can fail**
   - What: (a) The behaviour of commit `f5c01d3` (the control only where the line hides text) is covered in a browser: `scrollHeight` is 0 in jsdom, so a unit test keeps the control; add an assertion in `pm-web-e2e` over a report whose body wraps and one whose body fits, measured, no timer wait (the review's request for it was answered with a reason; the reason was that no fixture wraps — add a fixture that does, or measure a long body). (b) Focus after mark-read: the successor row, and the heading when the successor is gone (`chooseMarkRead`, ~760-785), including the `isConnected` fallback, asserted in `ReportsView.test.ts`. (c) The machine group sentinel (`'\u0000machine'`) is tested against a project named `__machine__` and a `system.` kind. (d) The `reports.spec.js` case "marks a single report read from its row menu and removes it from view" reads `initialCount` after only the row is visible, while its sibling waits for "6 unread reports" (the scenario announces a report and its read a moment after the list is drawn): wait on the same condition, no timer. (e) The reason of the unread half of the evidence is completed: the `reports.spec.js` mutation (revert `markRead` on open) fails the "opening marks read" assertion too.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
   - Done when:
     - Each item fails when the code it covers is reverted (evidence order: `mutation-after`)
3. **The row menu follows the component the spec names, or the spec is changed**
   - What: PLAN-54's spec took the row menu from `DataTable`; the view builds its own from `IconButton` and a raw `<button role="menuitem">` (`ReportsView.svelte` ~1176-1209), against the standing constraint "no plain text button". Read `DataTable` and the shared menu components: if one can carry a row menu for non-table rows, use it; if none can, move the menu into a shared component under `pm-web-app/src/lib/components/` (documented in `doc/design/web-app/components.adoc`) or change the constraint in the design with the reason. Do not leave the raw button.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
   - Done when:
     - The decision (reuse, new shared component, or design change) is recorded with its reason, and no raw `role="menuitem"` button remains in `ReportsView.svelte` unless the design says why (`git grep`) (evidence order: `none`)
     - The row menu keeps its keyboard behaviour: arrows, Escape returns focus to the trigger, Tab leaves; unit tests fail when it is reverted (evidence order: `mutation-after`)
4. **The open-issue read and the review**
   - What: the open Sonar issue count at the pull request head is recorded, and every CodeRabbit review of the pull request is handled before the merge, the review bodies and every thread, including a late one: list the unresolved threads through the GraphQL `reviewThreads` read at the end and handle each.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript
   - Done when:
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is fixed or dismissed with a stated reason (evidence order: `none`)
     - The pull request lists no unresolved review thread at the head (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md): `./mvnw -pl pm-web-app verify` and, with `PM_MOCK_WEB_PORT=20990 PM_WEB_PORT=20991`, `./mvnw -pl pm-web-e2e verify -Pe2e` pass at the head (a failure beyond the base names is yours).
- No new dependency or plugin.
- Web standards (CLAUDE.md): ES modules, flat ESLint config, no `innerHTML`/`{@html}`, no `waitForTimeout` and no `setTimeout` that only passes time; the npm scripts `lint`, `format`, `test`, `test:ci-strict` and `quality` stay.
- Every user-facing text goes through the catalogue (`$t(...)`) and sits in both `en` and `de`; the text guard (`text-guard-baseline.json`) only shrinks.
- The design document is updated in the same pull request as the code (UX standing rule of the epic).
- Ports of this plan: mock web port `20990`, app port `20991`. Other plans may run at the same time: never stop or reuse a process this worktree did not start.
- Browser check with the Playwright MCP server of `opencode.json`, following `AGENTS.md` § Browser checks: start the mock (`./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario fully-onboarded --web-port 20990"`), then `PM_MOCK_WEB_PORT=20990 ./pm-web-app/start.sh -p 20991`; pair again every session with a fresh single-use code.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`) — treat it as `sonar: configured`. An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason.
- Merge rule (the operator, 2026-11-02): at least one CodeRabbit review of some head with its findings handled; later heads need no review, re-trigger once and never wait. Handle the findings of EVERY CodeRabbit review that exists, a late one included, and read the review BODIES as well as the threads.

## Claim Labels

- OBSERVED: `markAllShownRead` posts `items.map((i) => i.id)` and empties `items`, while `matchingItems` is the filtered subset; no resize observer exists in `ReportsView.svelte`; the overflow measurement reads `items`, `query` and `activeDetailPlanId` — read by the orchestrator at `96286321`
- HYPOTHESIS: the unconditional filter slot draws an empty container, and two `<h1>` are on screen with the detail open — by a read-only analysing agent's reading, not checked in a browser
- HYPOTHESIS: the `reports.spec.js` row-menu case can flake like its sibling — a lead, not a verified failure
- HYPOTHESIS: `DataTable` cannot carry a row menu for the card layout — not checked
- Source: `landings/PLAN-54.md`.
- Verify-first clause: before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/views/ReportsView.svelte` — present at HEAD 159c310c
- OBSERVED: `pm-web-app/src/lib/views/ReportsView.test.ts` — present at HEAD 159c310c
- OBSERVED: `pm-web-app/src/lib/views/CompletionReportView.svelte` — present at HEAD 159c310c
- OBSERVED: `pm-web-app/src/lib/views/CompletionReportView.test.ts` — present at HEAD 159c310c
- OBSERVED: `pm-web-app/src/lib/templates/TriagePage.svelte` — present at HEAD 159c310c
- OBSERVED: `pm-web-app/src/lib/templates/TriagePage.test.ts` — present at HEAD 159c310c
- OBSERVED: `pm-web-e2e/tests/reports.spec.js` — present at HEAD 159c310c
- OBSERVED: `pm-web-e2e/tests/mobile.spec.js` — present at HEAD 159c310c

## Dependencies and Sequencing

- Depends on: `PLAN-54` (shipped)
- Overlaps with: `PLAN-93` (running; `format.ts`, the catalogue and the e2e specs — `reports.spec.js` is not in its surface), `PLAN-96` (running; `ActionBar`, `DataTable`: deliverable 3 here reads `DataTable` — read PLAN-96's landing first if it has merged), `PLAN-95` (`MockRuntimeEngine.java`, only if the long-body fixture of deliverable 2 is added to the mock)
- Should land before (same files, per `corpus cross-check` on 2026-11-05): `PLAN-45`, `PLAN-52`, `PLAN-72`, `PLAN-73`, `PLAN-75` (all staged: read this plan's landing first)
- Adjacent to: none

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-99-web-reports-residue.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
