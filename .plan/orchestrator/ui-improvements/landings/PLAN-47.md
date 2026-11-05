# Landing analysis: PLAN-47 web-actions-by-state

PR #311 `f6504d56` (squash-merged 2026-11-02T14:41Z by the operator; head `b0da29b6`; 35 files, +2196/-1198; PR #308 from the same branch was closed unmerged). Lane `rb-47-web-actions-by-state`: landing message filed 14:05Z as `merge_state=open` (the lane stopped at "Wait and resume later"); the operator merged 36 minutes later. Analysis by a read-only sub-agent (code read at the merge commit, nothing built or run).

## Merge rule

**No CodeRabbit review of any head exists** (verified: `pulls/311/reviews` and `pulls/308/reviews` empty; issue comments are only "Review limit reached" / "rate limited" at 11:32, 13:05, 13:14 on #308 and 13:25 on #311). The merge is OUTSIDE the rule (at least one CodeRabbit review). The lane asked for a wait and the operator merged: recorded as the operator's decision, not a lane error. Consequence: nothing of 35 files was reviewed by the required reviewer; the sub-agent's own review stands in (defects below). Sonar: gate passed, 0 new issues (comment 13:38Z; lane account).

## Three layers

| Deliverable | Implemented | Correct |
|---|---|---|
| D1 acts by state | MET: tables `actions/byState.ts:63-198`, `byState.test.ts:58-82` iterate every row and can fail; offered acts win (`:258-278`) | yes for the static tables; an unknown offered act is never marked destructive |
| D2 ActionBar and ConfirmDialog | MET for ConfirmDialog (`ConfirmDialog.test.ts`), the typed name, `acts` with roles; revoke-all-others asks for the name (`SettingsView.svelte:434-443`) | unenrol typed name is NOT wired: deferred to PLAN-59 by the spec; the plan's end dialogue keeps its own (no danger consequence list, not modal) |
| D3 system, devices, shared control panel | PARTLY: specs per state exist; the "no danger background outside an open dialog" helper exists (`helpers/acts.js:12-27`) | **draining banner lists and counts down: NOT met** (the mock never sends `drain_remaining_items`, no countdown logic, no test); `ControlActionsBar` removed, PLAN-37's Escape on window, opener restore with a first-enabled fallback and 44 px are carried, **the PR 297 focus-after-busy finding is NOT handled and the lane report never mentions it** |

Files beyond the Expected Surface (all in area): `components.adoc`, ten pm-api `messages/*.properties`, `api/client.ts`, `stores/runtime.ts`, `routes/+layout.svelte`, `components/Dialog.svelte`, five unit-test files, `helpers/acts.js`, `theme-readability.spec.js`. The lane's drift note omits the catalogues, the document, `theme-readability.spec.js` and the tests. The catalogue diff only adds keys (en/de symmetrical); every key `byState.ts` names exists.

## Defects and leads (verified by the orchestrator where marked V)

1. **V** An epic's cancel runs without confirmation: `chooseEpic` (`WorkControlPanel.svelte:263-282`) sends everything but next/common_files/archive straight to `handleAction`; the table marks cancel destructive and the design says destructive acts ask through ConfirmDialog. Inherited from the old bar, now against the new text; no test.
2. **V (path read)** Focus after busy: `confirmPause/End/Raise/Overlap` call `closeDialog()` then `handleAction()`, which sets `busy` before the tick; the bar disables every act while busy, so the opener is disabled, the first-enabled fallback finds nothing and focus drops to `body`; nothing restores it when `busy` clears. The orchestrator's message to the lane required the fix and a failing test.
3. **V** The disabled reason of an overflow act is only a `title` (`ActionBar.svelte:215-220`); disabled items are skipped by keyboard navigation (`:72`): an epic's archive in `landing` is unreachable by keyboard and screen reader (PLAN-37's `aria-describedby` regressed).
4. Overflow menu items carry no 44 px target (`ActionBar.svelte:260-269`).
5. The draining acts are unguarded (`+layout.svelte:50`): "Stop now" without confirmation, no busy, no `.catch` on withdraw.
6. Dead code: `SystemView.svelte:368,404-428` (`withdrawStop`, `withdraw_stop`/`stop_now`/`start`) can never run.
7. A latent wrong act id: an API-offered `decision`, `resolve_hold`, `report` or `follow_up` would be POSTed as a plan act (`WorkControlPanel.svelte:259`).
8. `devices.spec.js:113-118` was loosened (accepts the revoked view or the pair button).
9. `ConfirmDialog` has no busy guard (a double confirm; Escape closes during the request).
10. `expectNothingRedAtRest` is a one-shot evaluate and is silently disabled by any mounted `role=dialog`.
11. Lane observations: (a) the banner shows a count only: a real part of lead D3; (b) `/projects` and (d) "Open the decision" are assigned to PLAN-59 and PLAN-55 by the spec, not gaps; (c) the end dialogue stays its own: documented in `interaction-affordances.adoc`, a soft gap.

## Routing

All of 1 to 10 and the PR 297 focus finding: new staged PLAN-96 `web-acts-residue` (WS-03). The drain list and countdown need mock data (`drain_remaining_items`): PLAN-96 D3 names the mock side (a one-file change in `MockRuntimeEngine`; check the overlap with PLAN-95).
