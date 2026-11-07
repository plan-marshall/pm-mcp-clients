# Landing analysis: PLAN-106 web-acts-third-residue

epic: ui-improvements
workstream: WS-03
pr: 360

PR #360 `3bdd02a2` (squash-merged 2026-11-07T09:04:54Z through the merge queue; head `d7ee5789`; 9 commits, 20 files, +437/-50). Lane `rb-106-web-acts-third-residue` (runbook lane; harness antigravity; model, effort and tokens `unknown`): landing message filed 2026-11-07. Analysis by a read-only sub-agent (code read at the merge commit, nothing built or run); the thread counts, the review list, the Sonar status and the garbled reply were re-read by the orchestrator (marked V*).

## Merge rule

SATISFIED, with a weak answer to the nitpicks. CodeRabbit reviewed `8e6045a6` (review `5808245758`, 2026-11-06T23:10Z: 3 actionable items, 4 nitpicks, no follow-ups, no outside-diff section); the lane fixed all in `07dfee88` (next morning, 07:47), replied in the three threads, and CodeRabbit confirmed each (three empty-body bot reviews on `07dfee88`, V*). No review exists on a later head; the CodeRabbit check at `d7ee5789` is `neutral` (rate limited).

- Unresolved threads at the merge head (V*: GraphQL): PR 360 0 of 3, PR 331 0 of 10, PR 320 0 of 16. The lane states `unresolved_review_threads=0`: true.
- Body items: N1 (epic `decision` exclusion) handled, N2 (`? of 3 at once` badge) handled, N3 (omitted `running` field) PARTLY (asserts the badge, not the raise dialogue), N4 (`network_offline` withdraw test) handled. No written answer to the nitpicks on the PR (a nitpick has no thread): answered by the commit and the lane message only. The PR 331 outside-diff item and N3, N5 of review `5789601500` are answered in issue comment `6687245961`.
- SonarCloud (V*: check run `SonarCloud Code Analysis` SUCCESS, bot comment on `d7ee5789`): Quality Gate passed, 0 new issues, 0 accepted, 0 hotspots; rules left open: none. The lane's claim matches. CI: 14 checks green; `deploy-snapshot`, `covering-run-check`, `auto-merge` skipped.
- Lane message vs reality: "3 threads answered and resolved, 4 body suggestions implemented, unresolved=0" is true; "quality gate passed locally (2,719 tests)" cites a log from `0dea3ed4`, four commits before the head (`d7ee5789` itself fixed a type error in `byState.test.ts` the lane gate had not caught). Facts block: `sonar_open_new_issues` missing under its spec name (the `step.sonar-roundtrip.*` pair is equivalent); `review_body_findings_total=4` counts the nitpicks only.

## Three layers

| Done-when | Implemented | Correct |
|---|---|---|
| D1 act ids derived from the tables, `decision` excluded explicitly (test-first) | MET (`byState.ts` `tableActIds`, `handledActIds`; `WorkControlPanel.svelte:129-132`; `git grep ACTS_HANDLED` empty) | old and new sets identical, no behaviour change. The panel test drives the plan subject only (an injected `custom_inspect` act); the epic subject is checked by id-set equality. The failing run is cited as `899d9943`, an unrelated main commit (test and fix were committed together). `handledActIds('epic')` is compared to the table with and without the exclusion, a tautology covered by the later injection block |
| D2 unknown running count a real state, or the dead branch goes (test-first) | PARTLY: kept as reachable (`client.ts:598` `running?: number \| null`, `EpicView.svelte:583` `?`, two tests in `EpicView.test.ts`, captures `106-scope-unknown-{en,de}`) | the spec asked to check whether the real daemon omits `running` or to say `not checkable`; neither is in the PR. The design was not updated (`epic-view.adoc` ~47, ~81 and `work-actions.adoc:222` never say when the count is unknown). The correcting reply on PR 331 thread `4484094188` lost its code spans (V*: "because  was typed as ,") |
| D3 each changed/new test fails when its code is reverted, reverted code named (mutation-after) | MET (guard tests renamed, `runtime.test.ts` +2 tests, `ConfirmDialog.test.ts` comment corrected; the PR body names three mutations at `runtime.ts:687`, `:778`, `:420` with the failing assertions) | the mutation logs are in a git-ignored directory; the view guards (`drainingBusy`, `revoking`) are still reached by no test (the claim was withdrawn, which the spec allows) |
| D4 unknown withdraw has its own text (en, de); a stale refresh does not overwrite a later withdraw (test-first) | MET (`withdraw_unknown` in `system.{en,de}.properties:98`; `+layout.svelte` ~136-141; `advanceLifecycleEpoch()` `runtime.ts:674`; `control.de.properties:210` "Das Epic"; `system-texts.adoc`, `work-actions-texts.adoc` updated) | en/de, timeout, offline and the stale snapshot are tested without timer waits. TWO REVIEW FIXES OF THIS PR ARE UNPINNED: the epoch advance in `SystemView.svelte:281,289` and `isStopped: true` after stop-now success (`layout.test.ts:768` passes through the rejected path); both can be reverted with the suite green |
| D5 every item answered, none unresolved; Sonar 0 recorded | MET | see Merge rule; the nitpicks are not in the PR body's response list |

## Defects (at `3bdd02a2`)

1. MED V D2 closed on speculation: no check whether the daemon omits `running`, no design text for `?` and for when the count is unknown.
2. MED V two review fixes of this PR are unpinned (`SystemView.svelte` epoch advance, stop-now `isStopped` success path): the defect class this plan existed to close.
3. LOW-MED V the build gate and the 2,719 tests are cited at `0dea3ed4`, not at the final head; the D1 failing run is an unrelated commit; test-first failures carry no quoted assertion.
4. LOW V N3 half done: the omitted-`running` test asserts the badge, not the raise dialogue.
5. LOW V D1 epic subject not driven through the panel.
6. LOW V `handledActIds` equality asserted against both table variants (tautology).
7. LOW V the PR 331 reply `4493497437` is garbled.
8. LOW V German "Epik" remains in `epic_view.de.properties:32,33,35`.
9. LOW V landing facts: `sonar_open_new_issues` by name missing, `review_body_findings_total` counts the nitpicks only, model/effort/tokens `unknown`.

## Metrics and anomalies

- 9 commits; one substantive CodeRabbit round (3 items, 4 nitpicks) fixed about 8.5 hours after the review; six re-triggers rate-limited; merged 43 minutes after the head commit and 25 after the Sonar comment, through the merge queue.
- Fifth lane in a row whose evidence is partly in git-ignored logs a PR reader cannot open (the PR body quotes the assertions for the mutation clauses only).

## Reconciliation Actions

- [x] row `status` → `shipped`; `pr` = 360; `landing` = `landings/PLAN-106.md`; `plan_marshall_plan_id` = `106-web-acts-third-residue`
- [x] lane message `rb-106-web-acts-third-residue-001` archived
- [x] epic.md Decisions entry; resume anchor; `queue-view.md`; ledger committed
- [x] PLAN-109 and PLAN-110 unblocked (their gate was this landing)

## Follow-Ups

- **PLAN-117 `web-acts-fourth-residue`** (WS-03, runbook lane, staged, after PLAN-110 and PLAN-109 have landed because they touch `runtime.ts` and `work-actions.adoc`): defects 1 (check `plan-marshall-mcp` for `running`, write the rule and the `?` state into `epic-view.adoc` and `work-actions.adoc`), 2 (pin the epoch advance and the stop-now success path by tests that fail when reverted), 4-6 (the three test gaps), 7 (a clean reply on PR 331 `4484094188`), 8 (Epik).
- PLAN-111 (running) touches `+layout.svelte`/`layout.test.ts`: it must merge `origin/main` after `d7ee5789` before its pull request is final.
- Process: the next lane re-runs the gate at the final head and states `sonar_open_new_issues` by name (already in the standing constraints; the lane does not meet it).
