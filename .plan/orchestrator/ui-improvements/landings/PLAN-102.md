# Landing analysis: PLAN-102 web-reports-second-residue

PR #340 `95a2d855` (squash-merged 2026-11-06T02:20Z; head `e7700bc0` = merge of main; 7 commits, 13 files). Lane `rb-102-web-reports-second-residue`: landing message filed 2026-11-06T02:22Z. Analysis by a read-only sub-agent (code read at the merge commit, logs read, nothing built or run); the thread count, the Sonar count and the central findings (marked V*) were re-read by the orchestrator.

## Merge rule

PARTLY SATISFIED. CodeRabbit reviewed `77ffd42f` (review `5794258226`: 8 inline threads and FIVE NITPICKS IN THE BODY) and `e7700bc0` (review `5794751973`: 2 threads, no body items). The five body nitpicks are NOT HANDLED and the landing message does not mention them (V*: `RowMenu.svelte` ~133-135 still holds the guard; `9457c68d` touches `CompletionReportView.*`, `ReportsView.*` and the dev page only; `RowMenu.test.ts` still hard-codes the colours and the unscoped 44 px regex; the narrow test asserts `h1` only):

- N1 `RowMenu.svelte` 133-135 unreachable menu-target Escape guard: not handled.
- N2 `ReportsView.test.ts` narrow test must assert `h2` section headings: not handled.
- N3 `RowMenu.test.ts` ~240 focus asserted when the action runs: not handled.
- N4 `RowMenu.test.ts` ~279 contrast from the real tokens: not handled (the test uses `#2563EB`/`#60A5FA` and `#FFFFFF`/`#1C2230`; the product uses `#5B4BDB`/`#8B7EF9`).
- N5 `RowMenu.test.ts` ~292 44 px scoped to the phone query: not handled.

- Unresolved threads at the merge head: 0 of 10 (V: GraphQL). The lane did not state the count. That zero is not "handled": six threads were resolved by the lane while CodeRabbit's last comment was an open request or a refutation (defects 2, 4, 6, 11).
- SonarCloud: `0 New issues`, gate passed (V: bot comment). The facts block carries `step.sonar-roundtrip.open_issue_count=0`, not `sonar_open_new_issues`.
- The lane re-triggered CodeRabbit about seven times (rule: once). The PR body still reads "D4.1 pending, D4.2 pending" at merge.

## Three layers

| Done-when | Implemented | Correct |
|---|---|---|
| D1 reload keeps open report and filter | MET: one `TriagePage` with `hasFilters` (`ReportsView.svelte` ~844-864); log `164709` shows four new tests failing before | the list pane still swaps to a Skeleton on every reload (defect 15); the filter-text half of the test cannot discriminate (the query lives in the view) |
| D1 one `h1` at every width, outline true | MET for a loaded report (`titleLevel={isNarrow ? 1 : 2}`, threshold 1100 matches `TriagePage`) | no `h1` while the detail loads or errors at 390 px (defect 11); outline not pinned by tests (N2) |
| D1 design records where the `h1` lives | MET (`completion-report.adoc`) | renders inside the previous bullet (defect 14) |
| D1 unfiltered mark-all leaves none unread, load-older stays | `{ all: true }` sent (`client.ts` ~2878); load-older condition relaxed | contradicts `reports.adoc` 182/184 and the button label (defect 1, V*); `hasOlder`/`olderCount` untouched (defect 3, V*); a report arriving in flight vanishes (defect 2); rollback over-deletes (defect 4, V*); no click guard (defect 6, V*) |
| D1 one tokenization | MET in the view (`queryTokens`) | local; `utils/filterText.ts` unchanged (defect 16) |
| D1 PLAN-99 `:504`/`:509` threads | NOT done (no reply on PR 325 after 12:36Z) | the `:504` premise is false after a reload (defect 5) |
| D2 focus ring, 44 px phone target, focus returns on `choose()`, popup name | MET in code (`RowMenu.svelte` 216-232, `@media (max-width: 767px)`, `closeMenu(true)` before `onclick`, `aria-label={label}` localized by callers) | the three tests cannot fail on their reverts (N3, N4, N5); the real ring contrast passes (about 6.0:1 and 4.9:1 by arithmetic) |
| D2 Escape branch not masked | a guard was added | the hypothesis is REFUTED (`onMenuKeydown` stops propagation first); the lane did not report or re-scope (defect 8, V*) |
| D2 design, SVG, fixture, browser check | MET in the PR; `routes/dev` excluded from production by `devRouteExclusion` | 16 local screenshots, after-only, not in the PR; the opened menu sits under the bottom navigation at 390 px; fixture names duplicate across the theme blocks (defect 13) |
| D3 tests can fail, `mutation-after` | three nitpicks done, two tests added; log `172946` 13 failures | defect 9 |
| D4 every finding answered | 10 of 10 PR 340 threads resolved; Sonar 0 | defects 2, 4, 6, 10 and the Replies table |

## Defects (at `95a2d855`)

1. HIGH V* Unfiltered mark-all now sends `{ all: true }` (`ReportsView.svelte` ~523) while `doc/design/shared/reports.adoc` 182 says "_All shown_ sends the ids the list shows, never 'everything': a report that arrived after the list was drawn and is not shown yet stays unread" and 184 repeats it; the button still reads "Mark all shown read". The spec D1c mandated "everything"; the standing rule says the design changes in the same PR; CodeRabbit asked for the reconciliation (reply on `WVrM`, unanswered). A PRODUCT RULE: decided by the operator 2026-11-06, option A (ids shown, the design stands; PLAN-105 D1). Scenario: the operator sees 5 of 12 rows, clicks, and the 7 unseen reports are marked read.
2. MED V A `report.sent` arriving while the unfiltered request is in flight disappears (~541-552 filters `items` to the retained rows and sets `unreadCount = 0`; the server's `all` does not mark a later report). The follow-up on `:550` is unanswered and the thread was resolved by the lane (its reply "already triggers `refreshReportsCount()`" does not answer it). No deferred-response test.
3. MED V* After `all:true` `hasOlder` and `olderCount` are untouched (~538-553): "Show 5 older" stands beside "0 unread"; the new test asserts the ghost control and no row or count.
4. MED V* The failure rollback (~562-571) deletes the shown ids' markers only `if (activeDetailPlanId !== null)`: a report read and retained before the request turns unread-looking after a failed request; with the detail closed during the request, the pre-marked rows stay "read". CodeRabbit's follow-up on `:509` asked for "roll back the markers created by the request even when the detail has closed; preserve those that existed" and is unanswered.
5. MED V Double subtraction on a filtered mark-all after a reload: `loadReports` (~81-83) clears `countedReadIds` for non-pending ids yet keeps a retained read row, so `newlyRead` counts it again. Scenario: open A (3 to 2), a `report.sent` without ids reloads, filter to A and B and mark all: the count shows 0 where the server has 1. The PLAN-99 `:504` thread (spec D1e) is neither fixed nor tested.
6. MED V* The mark-all `Button` (~884) has no `disabled`/`loading`; the lane replied "guarded against concurrent triggers" (false, refuted by CodeRabbit at 01:02Z, unanswered) and resolved the thread. Consequence: the first `finally` removes the second request's `pendingReadIds`.
7. MED V* N1 to N5 not handled and not mentioned (Merge rule).
8. MED V* D2 Escape: the guard is dead and the Escape test fires on `document.body` (window handler only), so reverting the guard changes nothing; the spec's "fails when reverted" and its verify-first clause (report the refuted hypothesis) were not honoured; PR body and landing present it as "unmasked Escape key". No test covers Escape on the trigger.
9. MED V D3 `mutation-after` is thin and unreadable: log `172946` shows one mutation (`markRead` on open removed), 12 of its 13 failures are older PLAN-99 tests, the new `unreadCount` test passes under it, the mutated source is not recorded; no mutation for RowMenu D2a/c/d or `reports.spec.js` (D3b not done). The e2e log `173256` is truncated at test 320 of 371 with no summary, ran at 17:32Z before `77ffd42f` and `9457c68d`; the attempt before died on "http://localhost:21021 is already used". All logs live in a gitignored `.plan/local/...` path cited by bare file name.
10. MED V D4(a) not done as specified: no reply on any of the ten PR 325 threads or the five body findings of review `5782656169`; the PR body says "All 10 review threads resolved with corresponding commits" and its five "Findings" are PLAN-99's residue items, not the five body findings.
11. LOW V At 390 px while the detail loads or errors there is no `h1` (the list is hidden and its heading is an `h2`); the reply on `VchI` misreads the spec ("exactly one `h1` at every width").
12. LOW V The outline is not pinned by tests (N2); every heading is a doubled `{#if titleLevel === 1}` block (about ten, `CompletionReportView.svelte` 135-540) where `<svelte:element this={...}>` as in `Card.svelte` would do.
13. LOW V `/dev/components`: the fixture sits in `{#each THEMES}`, so light and dark copies share accessible names ("Row menu (More)" twice); CodeRabbit's follow-up on `VchU` is unaddressed; the offered follow-up issue for the fixture popup overflow (`WVrR`) was not opened.
14. LOW V/L Design leftovers: the `completion-report.adoc` paragraph attached to the "The head" bullet; `components.adoc` 59 names "PLAN-100" in lasting text; `RowMenu` skips disabled entries on arrow keys where `components.adoc` 80 says the ActionBar's reach them with their reason; the SVG draws "Copy link" and "Archive" which the product menu lacks, and its annotation labels probably clip (L).
15. LOW V A reload replaces the whole list pane with a Skeleton (`isLoading`, ~67-70): an open RowMenu, scroll position and focus in the list are lost on every `report.sent` without ids or retry.
16. LOW V/L The new token filter is local to `ReportsView`; `utils/filterText.ts` is unchanged, other filterable views still treat "..." as a filter.
17. LOW V `RowMenu.svelte` 216-232: the `:focus` and `:focus-visible` rules are identical, so a mouse open draws the ring.
18. LOW V The `unreadCount` test ("D3e") duplicates "counts a repeated read event only once".

## Replies (D4)

PR 340 threads: `Vcgr` (`CompletionReportView:385`) fixed in `9457c68d`: true. `Vcgy` (`:509`) "fixed; rolls it back": partly (pre-mark real, rollback over-deletes; follow-up unanswered). `Vcg4` (`:523`) true. `Vcg_` (`:550`) wrong on the stated point; follow-up unanswered. `VchC` (`:566`) "button is guarded": FALSE. `VchI` (`:875`) misreads the spec; resolved by the lane after CodeRabbit conceded. `VchM` true. `VchU` partly (distinct within a theme only). `WVrM` true to the spec, design unreconciled. `WVrR` out of scope, accepted, no issue opened. PR 325: no reply on any thread (defect 10).

## PLAN-100 overlap (PR #331, still running)

No shared file with PR 340 (V: `gh pr view 331 --json files`). One soft coupling: `components.adoc` 59 justifies "RowMenu separate from ActionBar" with PLAN-100's refactor.

## Lane deviations

The e2e claim "passed" rests on a truncated log at a pre-review head; "scoped Escape handling" and "Playwright 16 screenshots retained" are local, after-only, not in the PR; D4(a) per-thread replies not done; the landing states neither the thread count nor the review-body nitpicks. Fifth consecutive lane to report "all handled" with review-body items open.

## Routing

PLAN-105 `web-reports-third-residue` (WS-03, 21050/21051; new, staged 2026-11-06, mark-all rule decided by the operator 2026-11-06: option A, ids shown, as designed): D1 mark-all rule, `hasOlder`, in-flight arrival, rollback, double count, click guard (defects 1-6); D2 the five nitpicks and the row-menu tests, the dead guard, focus rule, fixture names (7, 8, 13, 17, 18); D3 heading outline while loading, written once (11, 12, 14 first half); D4 replies, evidence, landing facts (9, 10). PLAN-45 new deliverable 4: the list-pane Skeleton swap (15). PLAN-74 new deliverable 3: design leftovers and the shared filter rule (14 second half, 16).
