# PLAN-102: Web: What the Reports Residue Plan Left Open

epic: ui-improvements
workstream: WS-03

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.
> Staged 2026-11-05 from the landing analysis of PLAN-99 (`landings/PLAN-99.md`: PR #325 merged with 0 unresolved threads and 0 Sonar issues, but five CodeRabbit findings of the review BODY unhandled, a remount of the whole reports page, no `h1` at phone width, an invisible focus ring in the row menu, and evidence the lane cites but nobody can read). One pull request on purpose: each pull request costs a CodeRabbit review. Keep it under 100 files.

## Objective

PLAN-99 made the reports list act on what it shows, but its filter fix remounts the page on every reload, the heading move leaves the phone view without an `h1`, the new row menu has no visible focus cue and no 44 px phone target, and five review-body findings and the unread half of its own evidence were never handled. This plan makes the list stable across reloads, the heading outline true at every width, the row menu a component that follows the design it was written into, and every test named here fail when its code is reverted.

## Deliverables

1. **The reports page keeps its state across a reload; the headings are true at every width**
   - What: (a) `ReportsView.svelte` (~806-832) renders two `<TriagePage>` branches in `{#if items.length > 0 && !isLoading}` / `{:else}`; `loadReports()` (~376) sets `isLoading`, so a retry, a `report.sent`/`report.read` event without ids and a mark-all that empties the list remount the list, the open report, the filter field and the heading focus. Render one `<TriagePage>` and move the "no empty filter slot" rule inside it (a conditional slot, not a second page); the open report and the filter query survive a reload. (b) Exactly one `h1` at every width: below 1100 px `TriagePage` hides the list when a report is selected, and `CompletionReportView.svelte` (~133) is an `h2` now, so the detail view at 390 px has none. Decide in the design where the page `h1` lives in the detail view, and make the title and its section headings a real outline (the title no longer equals the `.section-title` `h2`s). (c) The unfiltered mark-all (~517-521) posts only the loaded ids but empties the list and the count, and the load-older control disappears: mark every unread report the server holds (or reload after it and keep the count true), and keep the load-older control while older reports exist (CodeRabbit follow-up `ReportsView.svelte:516-520`). (d) `isFiltered` uses the raw query while `matchingItems` uses tokens: one definition, so a punctuation-only query is not "filtered" yet shows everything. (e) The two threads left open as "conditional" (`:504` the double subtraction after an overlapping reload, `:509` the event ordering): fix with a test, or answer with the reason the order cannot occur.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
   - Done when:
     - Tests that fail before: a reload keeps the open report mounted and the filter text; at 390 px with a report open there is one `h1`; the unfiltered mark-all leaves no unread report behind and the load-older control stays; a punctuation-only query shows what the count says (evidence order: `test-first`)
     - The design records where the `h1` lives in the detail view (evidence order: `none`)
2. **The row menu follows the design it was written into**
   - What: (a) `RowMenu.svelte` (~213-215) `.row-menu__item:focus { outline: none }` beats the global `button:focus-visible`: the only cue is a background of near-equal luminance; give the item a visible focus ring in light and dark (measured contrast of the cue against its surface, 3:1). (b) Menu items take the 44 px phone target (`components.adoc` "A phone gets 44 px"; `ActionBar.svelte` ~302-305): the lane's reply that menu entries are exempt is contradicted by the ActionBar; decide whether `RowMenu` reuses the ActionBar's menu (a third copy of that logic stands now) or is the shared component, and record the reason. (c) `choose()` (~71-76) returns the focus: `closeMenu(true)` (CodeRabbit follow-up); the popup gets an accessible name; the Escape branch is not masked by `onWindowKeydown` (~131). (d) `/dev/components` gets a `RowMenu` fixture and the design its picture (the document says every component has both), and the sentence "DataTable provides a dedicated menu column" is corrected to what it does (draws an `IconButton`, reports the click).
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
   - Done when:
     - Tests that fail before: the focused item has a visible focus style distinct from its hover; a phone entry measures 44 px; choosing an entry returns focus to the trigger; the popup has a name; the Escape branch fails when reverted (evidence order: `test-first`)
     - The design and the fixture are in the same pull request, with a browser check (Playwright MCP, `AGENTS.md` § Browser checks) of the menu at 1440 × 900 and 390 × 844, `de` and `en`, light and dark, before and after, with SCREENSHOTS kept (evidence order: `none`)
3. **The tests can fail, and the evidence is readable**
   - What: (a) The three nitpicks of the review body: `RowMenu.test.ts` (~21-25) asserts the visible label, (~49-88) a mixed disabled/enabled keyboard case, `ReportsView.test.ts` (~1137-1143) waits on `.report-title`. (b) PLAN-99 D2e, never reported: the `reports.spec.js` mutation (revert `markRead` on open) fails the "opening marks read" assertion too. (c) The mutation evidence of D2a/c/d and of the keyboard tests is a log in the pull request: PLAN-99 cited two task names without a log. (d) The `reports.spec.js` wrap and fit case (D2a of PLAN-99) has an e2e run that contains it (`PM_MOCK_WEB_PORT=21020 PM_WEB_PORT=21021 ./mvnw -pl pm-web-e2e verify -Pe2e`), cited by the log path. (e) `unreadCount` and the retained-open-row path of the filtered mark-all each have a test; the focus test after mark-read does not depend on an unflushed DOM (wait on the condition).
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
   - Done when:
     - Each item fails when the code it covers is reverted (evidence order: `mutation-after`), with the log path in the pull request
4. **Every finding is answered**
   - What: (a) The five findings of CodeRabbit review `5782656169` body and the ten resolved threads of PR 325 each get a reply naming the commit or a stated reason (the four resolved while the bot kept the substance open first: `ReportsView:504`, `:509` twice, `:832`). (b) Read the open-issue count at the pull request head.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript
   - Done when:
     - The pull request lists the threads and the body findings with the commit or reason of each, and none is unresolved at the head (list them through the GraphQL `reviewThreads` read) (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md): `./mvnw -pl pm-web-app verify` and, with `PM_MOCK_WEB_PORT=21020 PM_WEB_PORT=21021`, `./mvnw -pl pm-web-e2e verify -Pe2e` pass at the head (a failure beyond the base names is yours).
- No new dependency or plugin.
- Web standards (CLAUDE.md): ES modules, flat ESLint config, no `innerHTML`/`{@html}`, no `waitForTimeout` and no `setTimeout` that only passes time; the npm scripts `lint`, `format`, `test`, `test:ci-strict` and `quality` stay.
- Every user-facing text goes through the catalogue (`$t(...)`) and sits in both `en` and `de`; the text guard (`text-guard-baseline.json`) only shrinks.
- The design document is updated in the same pull request as the code (UX standing rule of the epic).
- Ports of this plan: mock web port `21020`, app port `21021`. Other plans may run at the same time: never stop or reuse a process this worktree did not start.
- Browser check with the Playwright MCP server of `opencode.json`, following `AGENTS.md` § Browser checks: start the mock (`./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario reports-and-system --web-port 21020"`), then `PM_MOCK_WEB_PORT=21020 ./pm-web-app/start.sh -p 21021`; pair again every session with a fresh single-use code. Keep SCREENSHOTS, not only snapshots.
- ZERO Sonar findings (the operator, 2026-11-05): this project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`) — treat it as `sonar: configured`. The pull request merges with 0 open new-code issues, every open issue fixed in a commit of THIS pull request (never on a side branch), none dismissed without the operator's word, an undecidable read is a stop ("Wait and resume later"). State `sonar_open_new_issues=N` in the landing message.
- Merge rule (the operator, 2026-11-02): at least one CodeRabbit review of some head with its findings handled; later heads need no review, re-trigger once and never wait. Handle the findings of EVERY CodeRabbit review that exists, a late one included, and read the review BODIES as well as the threads (PLAN-99 resolved its ten threads and left five body findings). Before you merge, list the unresolved threads of your own pull request through the GraphQL `reviewThreads` read and state `unresolved_review_threads=N` at the head in your landing message.

## Claim Labels

- OBSERVED: `RowMenu.svelte` `.row-menu__item:focus { outline: none }` (~213-215), `choose()` calls `closeMenu()` without the focus argument (~74), `CompletionReportView.svelte` ~133 is an `h2`, `ReportsView.svelte` renders `<TriagePage>` twice in an `{#if}`/`{:else}` (~806-832) — read by the orchestrator at `bbf67794`
- OBSERVED: the review body of CodeRabbit `5782656169` carries 2 follow-ups and 3 nitpicks; PR 325 has 10 threads, 0 unresolved — read through `gh api` on 2026-11-05
- HYPOTHESIS: the page remounts on every reload, the phone detail view has no `h1`, the unfiltered mark-all leaves older rows unread, the Escape branch is masked by `onWindowKeydown`, the focus-ring contrast, the 44 px rule applying to menu entries — by a read-only analysing agent's reading at `bbf67794`, not run
- HYPOTHESIS: the cited mutation tasks and the D2a e2e run left no log — searched under `.plan/local` by the analysing agent
- Source: `landings/PLAN-99.md`, `landings/PLAN-54.md`.
- Verify-first clause: before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/views/ReportsView.svelte` — present at HEAD bbf67794
- OBSERVED: `pm-web-app/src/lib/views/ReportsView.test.ts` — present at HEAD bbf67794
- OBSERVED: `pm-web-app/src/lib/views/CompletionReportView.svelte` — present at HEAD bbf67794
- OBSERVED: `pm-web-app/src/lib/components/RowMenu.svelte` — present at HEAD bbf67794
- OBSERVED: `pm-web-app/src/lib/components/RowMenu.test.ts` — present at HEAD bbf67794
- OBSERVED: `pm-web-app/src/lib/components/ActionBar.svelte` — present at HEAD bbf67794
- OBSERVED: `pm-web-app/src/lib/templates/TriagePage.svelte` — present at HEAD bbf67794
- OBSERVED: `pm-web-e2e/tests/reports.spec.js` — present at HEAD bbf67794
- OBSERVED: `doc/design/web-app/components.adoc` — present at HEAD bbf67794
- OBSERVED: `pm-mock-runtime/src/main/resources/scenarios/reports-and-system.json` — present at HEAD bbf67794

## Dependencies and Sequencing

- Depends on: `PLAN-99` (shipped)
- Overlaps with: `PLAN-100` (running; `ActionBar.svelte` is in its surface through deliverable 3: the two plans edit different parts, whichever lands second rebases), `PLAN-101` (staged; disjoint views)
- Should land before: `PLAN-45`, `PLAN-52`, `PLAN-72`, `PLAN-73`, `PLAN-75` (same files as PLAN-99, per `corpus cross-check` on 2026-11-05)
- Adjacent to: none

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-102-web-reports-second-residue.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
