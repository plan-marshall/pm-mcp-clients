# Landing analysis: PLAN-99 web-reports-residue

PR #325 `bbf67794` (squash-merged 2026-11-05T12:36Z; head `50b4024b`; 8 files). Lane `rb-99-web-reports-residue`: landing message filed 2026-11-05T12:38Z. Analysis by a read-only sub-agent (code read at the merge commit, nothing built or run); thread list, Sonar bot comment, review body and five code points re-read by the orchestrator.

## Merge rule, thread count, Sonar policy

- First landing message to state both numbers. **Unresolved threads: 0 of 10 (V: GraphQL), as stated. Sonar: 0 new issues (V: bot comment = check run of head `50b4024b`), as stated (`sonar_open_new_issues=0`).** Both rules conform.
- CodeRabbit: one review, `5782656169` on `db8a154c`, 10 inline threads (all resolved) and **5 findings in the BODY that no thread answers (V) and the code does not handle**: follow-up `RowMenu.svelte:71-76` (`choose()` closes without returning focus: `closeMenu()` not `closeMenu(true)`), follow-up `ReportsView.svelte:516-520` (reload after the unfiltered mark-all instead of clearing the list), nitpick `RowMenu.test.ts:21-25` (assert the visible label), nitpick `RowMenu.test.ts:49-88` (mixed disabled/enabled keyboard case: the disabled filter has no test), nitpick `ReportsView.test.ts:1137-1143` (`waitFor` the `.report-title`). The lane's "all 10 threads answered and resolved" is true of the threads; four of them were resolved while the bot kept the substance open (`ReportsView:504` double subtraction "left open as a conditional concern", `:509` twice, event ordering; `:832` the remount below).

## Three layers

| Done-when | Implemented | Correct |
|---|---|---|
| D1a mark-all under a filter | MET (`ReportsView.svelte` ~495-515 posts `matchingItems`; test fails before) | the unfiltered branch posts only loaded ids, zeroes the count and empties the list: older unread rows stay unread, the load-older control disappears (~517-521; pre-existing, now flagged) |
| D1b resize re-measure | MET (`<svelte:window onresize>`; non-vacuous) | unthrottled, window resize only (minor) |
| D1c no empty filter slot | MET by duplicating `<TriagePage>` in `{#if items.length > 0 && !isLoading}` / `{:else}` (~806-832, V) | **HIGH: remount**: `loadReports()` sets `isLoading`, so every reload (retry, a `report.sent`/`report.read` without ids) and a mark-all that empties the list destroys and refetches the list, the open `CompletionReportView`, the filter and the heading focus; CodeRabbit raised it on the last thread |
| D1d one `h1` | MET only at >= 1100 px | **HIGH (a11y)**: `CompletionReportView.svelte:133` h1 to h2; below 1100 px `TriagePage` hides the list when a report is selected, so the page has NO `h1` at 390 px; the title equals its own `.section-title` h2s (flat outline) |
| D1e names | MET (toggle "Show the text: subject", trigger "Report actions: subject") | the popup (`role=menu`) is unnamed |
| D1 order | `test-first` MET (log `maven-2026-11-05-081359.log`: 5 new tests fail at base) | |
| D1 browser check | PARTIAL: 13 yml snapshots (`.plan/temp/playwright-mcp/page-2026-11-05T08-23-05Z..08-24-52Z`), 0 screenshots; en filter plus mark-all and de names; nothing for 390 px, dark or a "before"; the snapshots predate the commit | |
| D2a e2e wrap and fit | code present; fixture bodies added to `reports-and-system.json` | no log contains the new test; CI runs no e2e: unverifiable |
| D2b focus, D2c sentinel, D2d wait | MET, non-vacuous (no timer; no `waitForTimeout`) | D2b passes because the DOM flushed before the query (fragile, I) |
| D2e (revert `markRead` on open fails the "opening marks read" assertion) | NOT ADDRESSED (only a prettier reflow near it; the message is silent) | |
| D2 order `mutation-after` | one mutation claimed (isConnected fallback), citing "task-583", and "task-693" for D3: no log for either (V) | |
| D3 decision, no raw `menuitem` in the view | MET formally (`components.adoc#row-menu`, `RowMenu.svelte` extracted) | the doc says `DataTable` "provides a dedicated menu column": it draws an `IconButton` and reports the click (V); **HIGH (a11y)** `RowMenu.svelte:213-215` `.row-menu__item:focus { outline: none }` beats the global `button:focus-visible`: the only cue is a background (`#F0F0EC` on `#FFFFFF`; `#1E2433` on `#1C2230` dark) (V; contrast I); items have no 44 px phone target although `components.adoc` and `ActionBar.svelte` ~302-305 require it (the lane's reply that menu entries are exempt is contradicted by ActionBar); a third copy of the ActionBar menu logic; no `/dev/components` fixture and no picture although the doc says every component has one; the Escape branch is masked by `onWindowKeydown` (~131): reverting it passes (I) |
| D4 Sonar, threads | MET (0; 10 of 10 resolved) | the review body ignored (above) |

Files outside the Expected Surface: none named by the analyst except the mock fixture (needed for D2a). Time dependence: none (no timers, no clock; the `rep-0142`/`rep-0143` body change touches no golden).

## Lane claims not holding or unverifiable

Browser check "met" for all combinations (13 snapshots, no screenshot, no before/after); "12 Playwright tests passed at `ddb434c6`" (no e2e log contains the D2a test); the mutation tasks "task-583" and "task-693" (no log); D2e not reported; "D3 hypothesis confirmed" rests on a wrong reading of `DataTable`; `isFiltered` uses the raw query while `matchingItems` uses tokens (a punctuation-only query takes the filtered path and shows everything).

## Routing

All of it into PLAN-102 `web-reports-second-residue` (WS-03, 21020/21021, staged): the remount, the outline, the unfiltered mark-all, the row menu (focus ring, 44 px, focus return, name, fixture and picture, the doc), the tests, D2e and the mutation evidence, the browser check with screenshots, the review-body findings and a reply in every thread. Rows: PLAN-99 `shipped`.
