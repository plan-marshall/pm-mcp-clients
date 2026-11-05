# Landing analysis: PLAN-96 web-acts-residue

PR #320 `6cf1aac0` (squash-merged 2026-11-05T06:44Z; head `5127d97e` = `eea5204e` plus a merge of main plus a formatting commit; 33 files). Lane `rb-96-web-acts-residue`: landing message filed 2026-11-05T06:46Z. Analysis by a read-only sub-agent (code read at the merge commit, nothing built or run); the unresolved-thread list was read by the orchestrator (GraphQL) and three findings were re-read at `6cf1aac0`.

## Merge rule

NOT SATISFIED. CodeRabbit reviewed `75a627c2` (review `5777956093`: 12 inline, 1 body follow-up, 6 nitpicks: all handled, 12 threads fixed and resolved, issue comment `6646514210` disposes the 7 body items, the timer-shrink test declined with a reason) and `eea5204e` (review `5778430658`: 4 inline threads plus 1 outside-diff body finding): **that second review was never handled, no fix and no reply**. `e89bc61f` has 15 reply-only reviews. No review exists for the final head `5127d97e` (the CodeRabbit check is neutral: "review rate limited"). The lane message says "all review threads resolved" and "every one fixed or answered": FALSE (V: GraphQL shows four unresolved threads: `MockRuntimeEngine.java:1330`, `:8383`, `SettingsView.svelte:159`, `:189`); it also lists reviews of `9a794b1`, which the reviews API does not show. SonarCloud: 0 new issues. Third lane in a row with a false "all handled" claim (PLAN-89 #314, PLAN-94, PLAN-96); PLAN-92's lane claimed a later CodeRabbit run that did not exist.

## Three layers

| Done-when | Implemented | Correct |
|---|---|---|
| D1a epic cancel dialog, focus after pause and after busy, second confirm, Stop now / withdraw refusal (`test-first`) | MET; order `mutation-after` for "focus when the opener is disabled" and "Stop now" | see defects |
| D1b browser check | not verifiable: no artifact (the spec asks none to be listed) | |
| D2a reason as described-by, arrow-key reach | MET (`ActionBar.svelte` ~232, 245; disabled items no longer filtered) | one `span` inside `role="menu"` |
| D2b row-menu button measured against its cell at 1440 and 390; 44 px (`mutation-after`) | MET, partly weaker: `data-table.spec.js` measures the `/dev/components` page, not a product view; `control.spec.js` `box.height >= 44` | |
| D3a list, count, count falls | MET (`layout.test.ts` 3 tests, `system.spec.js` awaits "1 left"); the shrink test sets the store directly, not through `handleLifecycleEvent` | |
| D3b dead handlers and rows | MET (no `withdrawStop`/`stop_now`/`withdraw_upgrade` in `SystemView`; the `upgrading`/`stopped` rows and `system.action.start*` keys removed) | the design sentence "each one step to its view" was deleted from `system.adoc`, not implemented |
| D4a-c each fails when reverted | MET (logs `211121`, `211231`, `212051` fail, `014042` green) | D4a scope PARTLY: only `decision` is filtered from the bar; any other offered id is still posted via `handleAction` |
| D4 Sonar record | MET (0) | |

Files outside the Expected Surface (plausibly needed): four `pm-api` `*.properties`, four `doc/design/shared/*`, `reports-and-system.json`, `SystemScenarioTest`, `ScenarioStateContractTest`, `client.ts`, `ConfirmDialog.test.ts`, `layout.test.ts`, `SettingsView.test.ts`, the new `data-table.spec.js`. Listed but untouched: `stores/runtime.ts`, `interaction-affordances.adoc`. Cited build logs exist and match the pass/fail pattern.

## Unhandled review findings (all still present at `6cf1aac0`)

1. **V** `MockRuntimeEngine.java:1330` (unresolved): a scheduled `runtime.draining` event still fires after a withdrawal; `syncDrainItems` (`:7260`) has no lifecycle or generation guard and `handleDeleteStopRuntime` (~8420) does not cancel `pendingTasks`; the web store sets `isDraining: true` on any such event.
2. **V** `MockRuntimeEngine.java:8383` (unresolved): the default drain lists four items but `scheduleDrainShrink` schedules `seconds <= 3` (three steps): a four-item drain stays at one item and `DRAINING`; a two-blocker drain empties and shows "0 left: running work".
3. **V** `SettingsView.svelte:159` (unresolved): `loadData()` (lines 77-97) has no timeout; `getDevices` and `getStatus` run unbounded in `Promise.all` (revoke recovery).
4. `SettingsView.svelte:189` (unresolved): no sequence guard: an older `loadData` can overwrite `devices`.
5. `+layout.svelte:122-125` (a RESOLVED thread with no reply, comment `4475255422`, "remains open"): `network_offline` is not treated as an unknown outcome, no bounded refresh, no test of a lost response; only `TimeoutError`/`AbortError` are classified.
6. **V** `+layout.svelte:94`, review-body finding (no thread): `client.ts:2971` `withdrawStop()` passes no signal: a pending DELETE keeps `drainingBusy` true and disables both drain acts.

## Defects (at `6cf1aac0`)

- MED `ConfirmDialog.svelte:47` `confirm()` busy guard cannot be tested through the button: `Button.svelte:47` already sets `disabled={disabled||loading}`, so the "double confirm" tests (`ConfirmDialog.test.ts`, `layout.test.ts`, `SettingsView.test.ts`) pass without the guard.
- MED the `TimeoutError` path (`stop_now_unknown`) and the revoke-timeout `loadData` path have no tests.
- LOW `WorkControlPanel.svelte:382-386` `chooseEpic('cancel')` does not clear `failure` (shown in the dialog at ~1038: an old failure appears when it opens); `:759-768` `openHold` opens the dialog after a failed hold read; `:366`, `:895` show an unknown running count as `0` in the raise dialog; the Shift+Tab-from-dialog-box fix in the end-dialog trap (~273) is untested.

## Leads (not verified)

- Whether the real `pm-mcpd` accepts `{with_plans, reason}` for an epic cancel (the mock ignores them), whether it sends `drain_remaining_items` and re-emits `runtime.draining` as work ends.
- Whether `/dev/components` is reachable in the e2e dev server (likely: `npm run dev`) and `pm_locale` is the right storage key.
- The browser-check claims (focus on the first enabled act, 2-to-1 shrink) have no artifact.

## Routing

Findings 1 and 2: PLAN-95 deliverable 6 (the mock; PLAN-95 edits `MockRuntimeEngine.java` and PLAN-96 has landed). Findings 3 to 6, the defects, the test gaps and the D4a scope: PLAN-100 (`web-acts-second-residue`, staged 2026-11-05). The unanswered threads are answered in those two pull requests.
