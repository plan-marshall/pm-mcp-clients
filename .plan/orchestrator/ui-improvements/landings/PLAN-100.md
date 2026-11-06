# Landing analysis: PLAN-100 web-acts-second-residue

PR #331 `1a36ed5e` (squash-merged 2026-11-06T08:34Z; head `aa376f23` = merge of main; 15 commits, 23 files). Lane `rb-100-web-acts-second-residue`: landing message filed 2026-11-06T08:36Z (amended 08:37Z). Analysis by a read-only sub-agent (code read at the merge commit, logs and PNGs read, nothing built or run); the thread count, the Sonar count and the central findings (marked V*) were re-read by the orchestrator.

## Merge rule

PARTLY SATISFIED. CodeRabbit left THREE reviews with bodies; the landing names two.

- `5789601500` (on `94951b1a`): 6 inline threads (all replied with real commits), 5 follow-ups, 5 nitpicks: handled or answered, see the table.
- `5790414863` (on `d88d6e49`): 4 inline threads, all fixed in `3efd10dc`, confirmed in-thread.
- `5793110125` (on `2ce7f9f1`, 2026-11-05T22:33Z): ONE outside-diff item, `WorkControlPanel.svelte:167-169` "Derive the handled action IDs from the action tables": NOT HANDLED, no reply, not in the landing (V*: `PLAN_ACTS_HANDLED`/`EPIC_ACTS_HANDLED` are still hand lists at `1a36ed5e`, ~132-157).
- Unresolved threads at the merge head: 0 of 10 (V*: GraphQL). The landing does not state the count.
- SonarCloud: `0 New issues`, gate passed (V*: bot comment); the facts block carries `step.sonar-roundtrip.open_issue_count=0`.
- The lane re-triggered CodeRabbit about eight times (rule: once).

Review-body items: F1 `pairBrowser` lifecycle fixed but untested; F2 device events during a load answered with a reason (CodeRabbit's second option not weighed); F3 destroy guard fixed, untested; F4 count-only drain fixed (event path tested, init path not); F5 abort in catch fixed, untested; N1 fixed; N2 fixed; N3 (view guards without the dialog) declined in a comment and absent from the landing; N4 fixed; N5 half (the signal, not a pending read); review 3 not handled.

## Three layers

| Done-when | Implemented | Correct |
|---|---|---|
| D1a devices/status read bounded | MET (`SettingsView.svelte` ~98-127, `AbortSignal.any` with a 15 s timeout, passed to fetch) | the 15 s firing is never waited out, only `toHaveBeenCalledWith(15_000)` |
| D1b a stale read cannot overwrite | MET (`++loadSequence`, guards in try, catch, finally; log `115928` shows the failing run) | not for local edits (defect 8) |
| D1c `withdrawStop(signal)` bounded | MET (`client.ts` ~2975-2990, `+layout.svelte` ~131) | an unknown withdraw is drawn as a failure (defect 7); timeouts scattered (defect 10) |
| D1d `network_offline` as unknown plus bounded refresh | MET (`+layout.svelte` ~67-95) | races with an act's success (defect 6) |
| D2 guard tests can fail | ConfirmDialog guard test fails when reverted (log `104112`) | the two VIEW-level guard tests cannot reach the view guard (defect 4); the shrink test shows green in the mutation run |
| D2 Shift+Tab, `data-table.spec.js` product-view | MET (tests exist, no timers) | the e2e mutation log has 9 failures with no assertion text and no recorded mutated source |
| D3a, b, d | MET, tests fail before (log `105611`) | |
| D3c unknown running count not shown as 0 | present in the panel | DEAD in the product (defect 2, V*) |
| D3e only handled ids posted | works today | the hand lists mirror `byState.ts` (defect 1, V*) |
| D3f decision, browser check | decision in `system.adoc` `<<one-step>>`; 32 captures in gitignored `.plan/temp/playwright-mcp/100-*` | 14 of 16 pairs byte-identical, the new states appear in no capture; not in the PR |
| D4a PR 320 threads | 5 answered or pointed | thread `z3dUn`: CodeRabbit's last word "remains open" (it could not find `27a4d0d0` on PR 320), left resolved |
| D4 PR lists threads and the Sonar read | NOT MET: the PR body is stale ("PLAN-95 has not merged", "recorded after CI is green" never edited) | |

## Defects (at `1a36ed5e`)

1. MED V* Review `5793110125` not handled and not answered (above). Scenario: someone adds an act to `PLAN_ACTS`; `actsFor` returns it and the filter silently drops it; no test asserts "handled = table ids minus `decision`".
2. MED V* D3c is dead in the product: `EpicView.svelte` ~608-609 passes `scopeKnown={resource !== null}` and `running={resource?.running ?? null}`; `EpicResource.running` is typed `number` (`client.ts:598`), so `running === null` only while `resource === null`, which is when Next plan is disabled. The reply on thread `4484094188` ("there the unknown count does ask") is wrong. L: if the real daemon omits `running`, every Next plan on a scoped epic opens the raise dialogue (the old `?? 0` started the plan).
3. MED V `3efd10dc` regression: a failed epic-resource read leaves `resource = null` (`EpicView.svelte` ~158-160), Next plan stays disabled with "The epic is still being read", no error, no retry.
4. MED V D2 not met for the view-level guards, and the test comments say the opposite: `layout.test.ts` "refuses a stop-now confirm…" and `SettingsView.test.ts` "refuses a confirm…" never reach `drainingBusy`/`revoking` (`ConfirmDialog` refuses first); their comment "either alone must keep the request single" is false; CodeRabbit's N3 asked for this and was declined without fixing the text.
5. MED V Four fixes of CodeRabbit have no test and can be reverted with the suite green: `pairBrowser` lifecycle, the destroy guard, abort-in-catch, `onDestroy` abort; N5 half.
6. LOW V `refreshLifecycle` races with an act: the epoch is bumped only by `handleLifecycleEvent`; a withdraw that succeeds on retry writes `isDraining:false` without bumping, a stale snapshot can re-show the banner (under 5 s).
7. LOW V The unknown withdraw is drawn as a definite failure with the raw `network_offline` or the browser's English text in a `de` UI; no `withdraw_unknown` text (stop-now has one).
8. LOW V `SettingsView`: `revokeTimedOut` omits `network_offline` and does not re-read; a successful revoke edits `devices` without bumping `loadSequence`, an older load can re-add the row; device events during a load are replaced by the snapshot (F2).
9. LOW V F2's cheap option (drop the snapshot if an event arrived after the read started) was not weighed, although `+layout.svelte` builds that epoch pattern in the same PR.
10. LOW V Timeouts scattered across `SettingsView`, `+layout.svelte` (three) and `client.ts` (two); the client's `signal ?? AbortSignal.timeout(...)` fallback in `withdrawStop` is never used; PLAN-45 D2 collides with all of them.
11. LOW V Evidence and hand-off: stale PR body, build gate cited at `94951b1a`, own threads and reviews 2-3 not listed, captures and logs only on the main checkout.
12. LOW V `control.epic.next_waiting` (de) says "Die Epik"; `control.*.de` uses "das Epic".
13. LOW V The truncated stop-now response body: answered with a reason, CodeRabbit offered an issue, none opened, no plan owns it.

Retracted lane claim: "`vi.spyOn(AbortSignal,'timeout')` is not restored" is false as a leak (`setupTests.ts:44-47` `vi.restoreAllMocks()`).

## Replies

PR 331: every inline thread has a true reply with a real commit, except the reply on `4484094188` (asserts an unreachable path). PR 320: five threads replied or pointed to PLAN-95 (merged); `z3dUn` has CodeRabbit's last word open. Cited commits resolve through `gh api` and are reachable only via `refs/pull/331/head`.

## Overlap with running plans

PLAN-105 shares `client.ts` (changed ~43-50, 2207-2214, 2971-2990): merge main before landing. PLAN-97: no file; soft warning that the web treats `drain_remaining_items: []` beside `drain_remaining_count: N` as N and `system.adoc` now says an item is a name, while `25-system.adoc` and the CLI drain output must agree. PLAN-104 and PLAN-18: none.

## Lane deviations

"All handled" with review 3 open, N3 and N5 not fixed, a false claim about the spy, D3c shipped dead with a reply asserting it is reachable, a stale PR body, edits made first in the main checkout (reverted, recorded by the lane). Sixth consecutive lane to report "all handled" with a review-body item open. Positive: bodies of reviews 1 and 2 read and answered item by item, no timers and no `innerHTML`, the sequence guard has a discriminating test.

## Routing

PLAN-106 `web-acts-third-residue` (WS-03, 21060/21061; new, staged 2026-11-06): defects 1, 2, 4, 5 (the pieces outside SettingsView), 6, 7, 11, 12 and the reply to review `5793110125`. PLAN-45 new deliverable 5: one client-level time limit and one truncated-body contract (10, 13, and the `network_offline` revoke half of 8). PLAN-60 new deliverable 5: the SettingsView loads (8, 9 and the missing SettingsView tests of 5). PLAN-58 new deliverable 4: the epic-resource failure state with retry (3). Open outside the repository: the real `pm-mcpd` accepting `{with_plans, reason}` and sending `drain_remaining_items`, and a runtime that serves an id per drain item (owner `plan-marshall/plan-marshall-mcp`, no plan there).
