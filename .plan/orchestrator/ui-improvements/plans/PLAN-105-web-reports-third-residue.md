# PLAN-105: Web: What the Reports Second Residue Plan Left Open

epic: ui-improvements
workstream: WS-03

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane AFTER the operator's decision on deliverable 1 (below).
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.
> Staged 2026-11-06 from the landing analysis of PLAN-102 (`landings/PLAN-102.md`: PR #340 merged with 0 unresolved threads and 0 Sonar issues, but five CodeRabbit nitpicks of a review BODY unhandled, four threads resolved while CodeRabbit's last word was open, an unfiltered mark-all that contradicts the design, and evidence nobody can open). One pull request on purpose: each pull request costs a CodeRabbit review. Keep it under 100 files.

## Objective

PLAN-102 made the reports page stable across reloads and gave the row menu a focus ring and a phone target, but its unfiltered "mark all" now sends `{ all: true }` against a design that says "never everything", leaves the load-older control and the retained rows in a state the server does not share, loses a report that arrives during the request and guards nothing against a second click; the five nitpicks of its first review were never touched; the row menu's contrast, 44 px and focus-order tests cannot fail; and the lane resolved four threads where CodeRabbit had the last word. This plan makes mark-all keep one rule across design, code and tests, makes the row-menu and heading tests able to fail, and answers every open review item where the reviewer reads it.

## Deliverables

1. **Mark all has one rule, in design, code and tests**
   - DECISION PENDING (the operator): the design `doc/design/shared/reports.adoc` lines ~182 and ~184 says _all shown_ "sends the ids the list shows, never 'everything': a report that arrived after the list was drawn and is not shown yet stays unread", and the button reads "Mark all shown read". PLAN-102 deliverable 1c required that an unfiltered mark-all leave no unread report behind and shipped `markAllReportsRead(undefined)` which sends `{ all: true }` (merged, `ReportsView.svelte` ~523). The orchestrator writes the operator's answer INTO THIS SECTION before the plan is emitted; a lane that finds no recorded answer stops and files an Operator Question.
   - What: (a) Whichever rule is chosen, the design, the button label (`report.action.mark_all_read` en and de), the request and the tests say the same thing. (b) With `all`: `hasOlder` and `olderCount` are reset or reloaded after the response (today "Show N older" stays beside "0 unread", and the new test asserts that ghost control), a `report.sent` that arrives while the request is in flight is neither removed from the list nor counted as read (`ReportsView.svelte` ~541-552 filters `items` to the retained rows and sets `unreadCount = 0`; the server's `all` does not mark a report that arrived later), and `refreshReportsCount()` and the list agree. With the ids: the lane drops the `all` form and its test. (c) The failure rollback (~562-571) deletes only the markers THIS request created, whether or not a detail is open (today it deletes every shown id's marker only when a detail is open, so a previously read and retained report turns unread-looking after a failed request; CodeRabbit's follow-up on `:509` asked for exactly this). (d) After a reload `countedReadIds` is cleared for non-pending ids while a retained read row stays, so a later filtered mark-all counts that row again (`newlyRead = shownIds.filter(id => !countedReadIds.has(id))`): the count must not go below the server's (PLAN-99 thread `:504`, never fixed). (e) The button is disabled or loading while a request is in flight (`Button` at ~884 gets neither; the first `finally` removes the second request's `pendingReadIds`; the PLAN-102 reply "button is guarded" was false).
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing, pm-documents:ref-asciidoc
   - Done when:
     - Tests that fail before, one per clause (b) to (e): load-older reset after mark-all; a deferred-response test in which a report arrives during the request; a failed request after a retained read keeps the marker; the reload then filtered mark-all case keeps the count equal to the server's; a double click sends one request (evidence order: `test-first`)
     - `doc/design/shared/reports.adoc`, the catalogue (en, de) and the request agree on one rule, and `git grep -n "never \"everything\"\|never 'everything'" -- doc/design` names it only if the chosen rule is the ids (evidence order: `none`)
2. **The row menu's tests can fail, and the five nitpicks are answered**
   - What: CodeRabbit review `5794258226` (on `77ffd42f`) holds five nitpicks in its BODY that PLAN-102 never handled, and the landing message does not mention: (N1) `RowMenu.svelte` ~133-135 the menu-target guard of `onWindowKeydown` cannot fire (`onMenuKeydown` stops propagation first): remove it, or give it a test that fails when it is reverted. PLAN-102's verify-first clause was to report this refutation of its hypothesis (Escape masked by the window handler) and re-scope; say so in the pull request. Add the missing Escape-on-the-trigger test. (N2) `ReportsView.test.ts` ~1330-1345: the narrow-layout test asserts `h1` only; assert the `h2` section headings with a fixture that holds the conditional sections and Tokens, and the `h3` of Deliverables and Decisions at the wide layout (`CompletionReportView.test.ts` pins `titleLevel` 2 only). (N3) `RowMenu.test.ts` ~225-245: record `document.activeElement` INSIDE the action callback and assert it is the trigger. (N4) `RowMenu.test.ts` ~274-287: the contrast test hard-codes `#2563EB`/`#60A5FA` and `#FFFFFF`/`#1C2230`, colours the product does not use; read `--color-brand` and the surface from `tokens.css` (`#5B4BDB` light, `#8B7EF9` dark at `95a2d855`) for both themes, so a token change can fail it. (N5) `RowMenu.test.ts` ~290-294: scope the 44 px assertion to the block of `@media (max-width: 767px)`. Also: the `:focus` and `:focus-visible` rules of `RowMenu.svelte` ~216-232 are identical, so a mouse open draws the ring (decide and test); the `/dev/components` fixture sits in `{#each THEMES}` and its light and dark copies share accessible names ("Row menu (More)" twice; CodeRabbit follow-up on `VchU`): put the theme into the label; the unreachable fixture overflow CodeRabbit offered an issue for (`WVrR`): open it or fix it.
   - Persona: plan-marshall:persona-module-tester
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
   - Done when:
     - Each of N1 to N5 fails when the code it covers is reverted: the ring colour, the 44 px rule moved out of the phone query, the action running before focus returns, the in-menu Escape handling, a section heading level changed (evidence order: `mutation-after`; cite the reverted code)
     - The fixture's accessible names are unique on the page (evidence order: `test-first`)
3. **The heading outline holds while a report loads, and is written once**
   - What: At 390 px while the detail loads or errors there is no `h1` (the list is hidden and its heading is already an `h2`; the `h1` exists only once the report has loaded, `CompletionReportView.svelte` ~134-138); PLAN-102's reply on `VchI` read the spec as "requirements for the mounted report" but the spec says "exactly one `h1` at every width". Give the loading and the error state of the detail an `h1`. Every heading is written as a doubled `{#if titleLevel === 1}` block (about ten, `CompletionReportView.svelte` ~135-540); use `<svelte:element this={`h${n}`}>` as `Card.svelte` does. The design paragraph that records where the `h1` lives (`doc/design/shared/completion-report.adoc`) sits on the line after the "The head" bullet without a blank line, so it renders as part of that bullet: set it apart.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing, pm-documents:ref-asciidoc
   - Done when:
     - Tests that fail before: one `h1` at 390 px while the detail loads and while it fails; no heading level skipped at either width, across every section of a full fixture (evidence order: `test-first`)
4. **Every item is answered where the reviewer reads it, and the evidence can be opened**
   - What: (a) PLAN-102 resolved four threads while CodeRabbit's last word was open: `ReportsView.svelte:509` (rollback; defect of deliverable 1c), `:550` (reply "already triggers `refreshReportsCount()`" does not answer the lost report), `:566` (reply "guarded against concurrent triggers" was false), `:875` (spec misread) and `dev/components/+page.svelte:225`. Reply IN each thread with the true state after this plan's change; a thread CodeRabbit answered last is answered again, not resolved by you. (b) The PLAN-99 review `5782656169` on PR 325 (five body findings) and its ten threads got no reply from PLAN-102 although its deliverable 4a asked for it: reply in each thread or comment with the commit or the reason. (c) The PLAN-102 D3 `mutation-after` evidence was one mutation, twelve of whose thirteen failures are older tests, with the mutated code unrecorded, and no mutation for the row menu or `reports.spec.js`; the e2e log `173256` is truncated at test 320 of 371 and ran before the two later heads. This plan's evidence names the reverted code in the pull request body and attaches (or quotes) the log lines, since `.plan/local/…` logs are not readable from the pull request. (d) The landing message carries `unresolved_review_threads=N` and `review_body_findings_total/handled` as lines of its landing-facts block.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript
   - Done when:
     - The pull request lists every thread and body item it answers with the commit or reason, and none of its own is unresolved at the head (list the threads through the GraphQL `reviewThreads` read for your own pull request and for PRs 340 and 325) (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only for the gate (CLAUDE.md); the web unit tests run through the module's npm scripts under Maven (`pm-web-app` `test:ci`), the browser tests through `pm-web-e2e`.
- No new dependency or plugin.
- Web standards (CLAUDE.md): ES modules, no `innerHTML`/`{@html}`, no arbitrary timer wait in a test (`waitForTimeout`, `setTimeout` that only passes time), the npm scripts `lint`, `format`, `test`, `test:ci-strict`, `quality` stay.
- The design document is updated in the same pull request as the code (UX standing rule of the epic).
- Ports of this plan: mock web port `21050`, app port `21051`. Other plans may run at the same time: never stop or reuse a process this worktree did not start (PLAN-102's first e2e attempt died on an occupied `21021`).
- Every `test-first` clause in your landing message cites a FAILING run and a PASSING run of the named test (a passing log is not a failing run); every `mutation-after` clause cites the reverted code, and the evidence is readable from the pull request.
- ZERO Sonar findings (the operator, 2026-11-05): this project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`) — treat it as `sonar: configured`. The pull request merges with 0 open new-code issues, every open issue fixed in a commit of THIS pull request (never on a side branch), none dismissed without the operator's word, an undecidable read is a stop ("Wait and resume later"). State `sonar_open_new_issues=N` in the landing message.
- Merge rule (the operator, 2026-11-02): at least one CodeRabbit review of some head with its findings handled; later heads need no review, re-trigger once and never wait. Handle the findings of EVERY CodeRabbit review that exists, a late one included, and read the review BODIES (Follow-ups and Nitpicks as well as the outside-diff section) as well as the threads: PLAN-102 handled the threads of its reviews and none of the five nitpicks of the first, and reported "10 threads addressed". Reply IN the thread on GitHub: a list in your pull request body or in a progress file does not answer a thread, and a thread whose last comment is CodeRabbit's is not resolved by you. Before you merge, list the unresolved threads of your own pull request through the GraphQL `reviewThreads` read and state `unresolved_review_threads=N` at the head, and `review_body_findings_total/handled`, as lines of the landing-facts block of your landing message.

## Claim Labels

- OBSERVED: `ReportsView.svelte` (~523) calls `markAllReportsRead(undefined)` for the unfiltered case, which sends `{ all: true }` (`client.ts` ~2878-2880); `doc/design/shared/reports.adoc` lines 182 and 184 say the opposite; the unfiltered branch (~538-553) leaves `hasOlder` and `olderCount` untouched; the catch (~562-571) deletes the markers of every shown id only `if (activeDetailPlanId !== null)`; `Button` at ~884 has no `disabled` or `loading`; `RowMenu.svelte` ~133-135 holds the guard, `RowMenu.test.ts` ~225-245 and ~274-294 hold the tests of N3 to N5 with the hard-coded colours; `ReportsView.test.ts` ~1328-1345 asserts `h1` only; the five nitpicks of review `5794258226` have no reply and `9457c68d` touched none of `RowMenu.*` — read by the orchestrator at `95a2d855` (the merge commit of PR 340)
- OBSERVED: PR 340 has 10 threads, 0 unresolved; SonarCloud reports 0 new issues; the real tokens are `--color-brand: #5B4BDB` (light) and `#8B7EF9` (dark) in `pm-web-app/src/styles/tokens.css` — read through GraphQL, the bot comment and git on 2026-11-06
- HYPOTHESIS: a `report.sent` during the unfiltered request vanishes from the list while staying unread on the server; `loadReports` clearing `countedReadIds` makes a later filtered mark-all count a retained read row twice (the PLAN-99 `:504` reply's premise is false after any reload); the shipped ring passes 3:1 (about 6.0:1 and 4.9:1 by arithmetic); at 390 px the opened menu sits under the fixed bottom navigation (a screenshot of the lane, not in the pull request) — by a read-only analysing agent's reading at `95a2d855`, not run
- Source: `landings/PLAN-102.md`, `landings/PLAN-99.md`.
- Verify-first clause: before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/views/ReportsView.svelte` — present at HEAD 95a2d855
- OBSERVED: `pm-web-app/src/lib/views/ReportsView.test.ts` — present at HEAD 95a2d855
- OBSERVED: `pm-web-app/src/lib/views/CompletionReportView.svelte` — present at HEAD 95a2d855
- OBSERVED: `pm-web-app/src/lib/views/CompletionReportView.test.ts` — present at HEAD 95a2d855
- OBSERVED: `pm-web-app/src/lib/components/RowMenu.svelte` — present at HEAD 95a2d855
- OBSERVED: `pm-web-app/src/lib/components/RowMenu.test.ts` — present at HEAD 95a2d855
- OBSERVED: `pm-web-app/src/lib/templates/TriagePage.svelte` — present at HEAD 95a2d855
- OBSERVED: `pm-web-app/src/routes/dev/components/+page.svelte` — present at HEAD 95a2d855
- OBSERVED: `pm-web-app/src/lib/api/client.ts` — present at HEAD 95a2d855
- OBSERVED: `pm-web-app/src/styles/tokens.css` — present at HEAD 95a2d855
- OBSERVED: `pm-api/src/main/resources/messages/common.en.properties` — present at HEAD 95a2d855
- OBSERVED: `pm-api/src/main/resources/messages/common.de.properties` — present at HEAD 95a2d855
- OBSERVED: `doc/design/shared/reports.adoc` — present at HEAD 95a2d855
- OBSERVED: `doc/design/shared/completion-report.adoc` — present at HEAD 95a2d855

## Dependencies and Sequencing

- Depends on: `PLAN-102` (shipped), `PLAN-99` (shipped), the operator's decision on deliverable 1
- Overlaps with: none known at staging (`PLAN-100`'s pull request #331 shares no file with this surface)
- Should land before: `PLAN-45`, `PLAN-72`, `PLAN-73`, `PLAN-75` (same files)
- Adjacent to: `PLAN-74` (design leftovers of the row menu, deliverable 3 there)

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-105-web-reports-third-residue.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
