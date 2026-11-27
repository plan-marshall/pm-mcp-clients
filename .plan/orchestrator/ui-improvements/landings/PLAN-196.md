# PLAN-196 landing record

epic: ui-improvements
plan: rb-196-web-plan-page-residue (runbook lane, harness opencode, `deepseek-v4.1-flash/high`, effort high; `total_tokens` `unknown`, `plugin_version` `unknown`; executor 0.1.2183)
pr: #594, squash `aa0ca871` (22 files, +1339/−130) on `273e4d56`, merged 2026-11-27T16:14:53Z; branch head `391ce686` has the squash's tree; 21 branch commits
analysed: 2026-11-27, by sub-agent in three layers, by reading only (no build)

Message `rb-196-web-plan-page-residue-001` (2026-11-27T16:17:03Z):
- Facts: `merge_state=merged`, `deliverables_done=3` of 4, 0 unresolved threads, 16 of 16 review-body findings handled, `sonar_open_new_issues=0`, `self-review: clear`.
- Two operator questions, both answered "Merge, thread check read as a false positive". The thread `PRRT_kwDOVBOtss68X6lV` is in fact resolved.

**Gates.**
- CI: 14 checks pass, 6 skipped, none failed. CI runs no Playwright.
- Review threads: 43 (CodeRabbit 14, Sourcery 29), 0 unresolved. CodeRabbit reviewed 9 heads, `9a1cfa7b` … `1810ccbe`.

**Sonar.**
- PR 594: 0 issues, gate OK, coverage on new code 94.3 %.
- Default branch, read 2026-11-27 after the `main` task `AaId2mxKMsW91k37Z6Jw` (submitted 16:33:03Z, SUCCESS, after this merge): gate OK, coverage on new code 90.3 %, `count_status: confirmed`, **3 open**. They are the three PLAN-181 keys, none in a file this PR touched.

## Layer 1: specified

| Done-when | Verdict |
|---|---|
| D1(a) decision and report `open=`, attention slot, no `div` in the state slot, task 3 of 5, unknown `applies.key` | met: `PlanView.test.ts:2099-2267`; `PlanView.svelte:1349,1409,317`. "Task 3 of 5" is mutation-after (its code pre-dates the plan) |
| D1(b) one decision read across two refreshes | met: `:2295`; effect `PlanView.svelte:751` |
| D1(c) e2e: opens in place, Back returns, a load of `open=` closes | met at `9a1cfa7b` only (`plan-view.spec.js:115-137`). 8 later commits changed `PlanView`, `DecisionView` and `Link`; no e2e run at the merge head |
| D2 loading state, `#deliverable-N`, `file=` on return, feed stop | met: `:2740,2761,2708,2791`. The loading test is weak (L3-4) |
| D3(a) one Close, conflict acts as `Button`s, display names | met: `PlanSettingsModal.test.ts:228,243,262` |
| D3(b) a foreign write between two fields conflicts | met: `WorkControlPanel.test.ts:722,811`; `WorkControlPanel.svelte:645` |
| D3(c) `git grep control.settings.use_inherited` has no hit; every dialogue key documented | partly. 0 hits outside `.plan/`, but 7 hits in the ledger files that `main` carries (`epic.md`, `landings/PLAN-55.md`, the spec itself). The literal clause cannot be met while the spec quotes the key; the substance is met. All 12 keys documented, en/de parity |
| D4(a) Changes list bounds asserted at a 720 px column | partly: `pageWidth.test.ts:182` is a regex over the CSS (`PlanChanges.svelte:487` `clamp(280px, 30%, 360px)`). Nothing measures at 720 px |
| D4(b) 80 screenshots, all after the last visual commit | **not met**. 44 after-shots and 40 before-shots. The 40 state shots (11-26 09:47-09:50) predate `63c1e804` and `b68d5956`. The 4 target shots (12:23) predate 8 commits that changed rendering, and are in en-light only. Same defect as PLAN-55 |
| D4(c) the two design documents read as built | met: `plan-view-interaction.adoc:19-21,122,130-131`, `plan-view.adoc:54,237` |

**True count:** 7 of 10 met (D1(c) only at an earlier head), 2 partly, 1 not met. The message's "D4 met" overstates it.

## Layer 2: implemented

All 22 files trace to a deliverable.
- Outside the Expected Surface:
  - Explained by the message: `DecisionView.svelte` (`inPlace`), `Link.svelte` with its test (`onactivate`, fragments), `display/index.ts` with its test, `runtime.test.ts`.
  - Not in the message's drift list: `work-actions-chat-texts.adoc`.
- In the surface but untouched: `ObjectPage.svelte`, `PlanIntentPanel.svelte`, `navigation/history.ts`, `mobile.spec.js`. D2's address restore went to `runtime.ts` `selectObjectTab` (`:2196`), not `history.ts`.

**Stale or false claims:**
1. PR body: "After (last visual commit b68d5956)" is false. The shots predate it, and it was not the last visual commit.
2. PR body: "Every Done-when is met" contradicts its own Notes.
3. Message: "D4 met" (see above).
4. Message: lists `WorkControlPanel.svelte` as drift, but it is in the surface (spec `:155`). It omits `work-actions-chat-texts.adoc`.
5. Message: the e2e is "green at the head" only at `9a1cfa7b`.
6. Message: D3 asks for a report of runtime values that have no display name, and the message gives none. In `running-work`, `main`, `standard`, `native`, `GitHub`, `Claude Code - sonnet-5.5` and `Codex - gpt-5.3` render raw.
7. Message: names `ae08851b` for the fragment link. The anchor and the replace came in `63c1e804`, `b68d5956` and `9d8c7281`.
8. Info:
   - §3 "corroborated by reading the head" reads the run's own change back as corroboration.
   - `review_body_findings_total=16` counts CodeRabbit only.

## Layer 3: correct (reported to the operator; no fix plan staged)

No blocker, no major. No `{@html}`, `innerHTML` or timer wait added. en/de parity holds for the 8 `control.aspect.*` keys and `control.chat.revision_queued_unscheduled`.

The revision fix is correct:
- The form's token is set on open and on Keep current, and advanced only by the dialogue's own applied answer (`WorkControlPanel.svelte:613,654,697`).
- Apply again sends the conflict's revision.
- Save is disabled during a conflict.

| # | Severity | Finding | Where |
|---|---|---|---|
| L3-1 | minor | The in-place decision closes itself at once when the envelope has no `plan`, which is optional on `DecisionEnvelope`. The head link still offers it, because it comes from the server-filtered `getDecisions({plan})` | `DecisionView.svelte:423-424`; `client.ts:1193` |
| L3-2 | minor | Close calls `history.back()` when this document pushed. Today every in-plan move replaces, so it holds; a future push (PLAN-190's task) would make Close step back to that entry instead | `PlanView.svelte:594-618` |
| L3-3 | info | `closingInPlace` is re-armed by an effect that reads the whole `$runtimeStore`. A second close while `back()` is pending replaces the forward entry. Benign | `PlanView.svelte:574-585` |
| L3-4 | minor | The Intent loading test uses `findBy`, which waits, so it never sees the first paint. It fails on a revert only because the testid is new | `PlanView.test.ts:2753` |
| L3-5 | minor | An aspect the catalogue lacks renders as the unknown word, so two unknown fields look alike and lose their id. Prefixed aspects show the raw suffix ("Role implement"). The spec allows a raw fallback only for choices | `display/index.ts:204,208` |
| L3-6 | minor | The D4(b) screenshots are stale (Layer 1), including the settings labels changed in `825ebbd4` | `.plan/temp/playwright-mcp/196-*` |
| L3-7 | info | With the fragment target absent, the default anchor pushes an unmarked hash entry | `Link.svelte:78,102,110` |
| L3-8 | info | While this plan waits, decision events of any plan trigger a read of this plan's decision, outside the counted refresh path | `PlanView.svelte:688` |
| L3-9 | info | `pageWidth.test.ts` pins the CSS text by regex | `pageWidth.test.ts:182` |
| L3-10 | info | Two statements on one line | `WorkControlPanel.test.ts:833` |
| L3-11 | info | The fragment test leaves `location.hash` set for later tests | `Link.test.ts` |
| L3-12 | info | The in-place `<aside>` has no accessible name | `PlanView.svelte:1942` |
| L3-13 | info | `control.settings.close` reads "Close" in en and "Schließe die Einstellungen" in de. Pre-existing, now documented | `work-actions-texts.adoc` |

The message also reports a pre-existing 60 s flake in `routes/dev/components/+page.test.ts`. It is not in the diff and cannot be checked by reading.

## Residue for the plans that touch the same surface

18 code files touched:
- `control.{en,de}.properties`
- `PlanView.svelte` and its test, `DecisionView.svelte`
- `Link.svelte` and its test, `PlanChanges.svelte`
- `PlanSettingsModal.svelte` and its test, `WorkControlPanel.svelte` and its test
- `display/index.ts` and its test, `runtime.ts` and its test
- `pageWidth.test.ts`, `plan-view.spec.js`

General:
- `applyRoute` now clears `openObject`, `changeFile` and `phaseNode` on every `pushRoute` (`runtime.ts:1721-1728`). A push that needs `node=` or `file=` must set it afterwards.
- `PlanView` grew by about 150 lines above the tree, so every `PlanView` line reference past about `:250` taken before `aa0ca871` is off.

Per plan:
- **PLAN-136**, 9 shared code files:
  - The queued-revision line now sits in the attention slot (`PlanView.svelte:1409`). An unknown `applies` renders `revision_queued_unscheduled`.
  - D3's in-place forms can reuse `PlanView`'s in-place column and `DecisionView`'s `inPlace`.
  - `WorkControlPanel.svelte:764` has moved by about +22 lines.
  - `plan.revision_queued` is already handled by `PlanView`.
  - Re-read before emit.
- **PLAN-62**, 5 shared code files:
  - The leaf toggle is now around `:1678`.
  - "`node=` restored on return to Process" is still open: `switchTab` (`history.ts:291`) drops `node`, and this merge restores only `file=` (`runtime.ts:2196`). Mirror it.
  - Re-read before emit.
- **PLAN-190**, 5 shared code files:
  - The task push collides with the `applyRoute` resets.
  - The in-place `<aside>` is a second object column that hides `.plan-view` at ≤767 px.
  - A task push breaks `closeInPlace` (L3-2).
  - The task-level off-track banner did not move to the attention slot.
- **PLAN-179**, 1 shared code file (`WorkControlPanel.svelte`). Already stale since PLAN-55: "`PlanIntentPanel`/`OutlinePanel`/`BuilderCheckList` mounted nowhere". `PlanView` mounts them and passes `deliverableHref` (`:1629`). `runtime.ts:1521` is now `:1721`.
