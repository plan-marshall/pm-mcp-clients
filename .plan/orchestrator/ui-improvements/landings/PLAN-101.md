# Landing analysis: PLAN-101 web-format-and-token-second-residue

PR #335 `6d0686c1` (squash-merged 2026-11-05T18:39Z; head `ab7ef61f` = merge of main; 7 commits, 27 files). Lane `rb-101-web-format-and-token-second-residue`: landing message filed 2026-11-05T20:32Z. Analysis by a read-only sub-agent (code read at the merge commit, nothing built or run); the thread counts, the Sonar count and four findings were re-read by the orchestrator.

## Merge rule

SATISFIED. CodeRabbit reviewed `2101d30a` (review `5788363627`: 4 inline threads, 4 body follow-ups, 1 nitpick); all nine items were handled in `11be8390` and verified in the code (below). The four threads are resolved with a lane reply each (V: GraphQL). Every later CodeRabbit trigger was rate limited (15:10Z, 17:51Z, 18:25Z): the lane message says "re-triggered once", the PR shows three (LOW). Sourcery refused (budget), never a gate.

- Unresolved threads at the merge head: 0 of 4 (V: GraphQL `reviewThreads`). PR 314: 0 of 4 unresolved, PR 319: 0 of 13 unresolved (V).
- SonarCloud: `0 New issues`, quality gate passed (V: bot comment on the PR). The lane records `sonar_open_new_issues=0` in the PR body, not as a line of its landing message; the PR body also says "0 threads" where the PR has 4 resolved (LOW, sloppy record).
- The landing message states the facts only in parentheses (message lines 47-48) and in the PR body, not in the facts block: the runbook-lane change (`review-gate`, required facts) is still pending in plan-marshall.

## Three layers

| Done-when | Implemented | Correct |
|---|---|---|
| D1a conflict in the catalogue's words, no `{` | MET (`cleanupWords`, keys `.kind/.value/.remind_at` in en, de and the design table); test-first credible (log `133336`: 8 failed, 1260 passed) | the "Yours" line prints the full stale record (defect 2) |
| D1b apply-again sends only the operator's categories | MET (`changedCleanupFields` + `applyConflictAgain`); second-conflict and reminder tests | falls back to `{}` when `current_value` is not a record (defect 1) |
| D1c a refusal shows the minimum | PARTLY: only the client-side pre-check names the minimum; a server 400 `invalid_value` still shows `update_failed` (`stores/runtime.ts` 1086-1099) | defect 7; the spec does not say which refusal |
| D1d "never" plans still allow the reminder | MET (`days !== NEVER && days < 7`; `min="7"` removed) | OK |
| D1e wire form and the open daemon item | MET (`doc/design/web-app/machine-settings.adoc` "The wire form"; the spec's path `doc/design/shared/...` was wrong) | the daemon form is unchecked, named with owner |
| D2 generation counter | MET (`chatGeneration` + `invalidateChat()`, DecisionView.svelte 66-74; A,B,A test) | the core race is closed; the 11be8390 guards have no test (defect 4) |
| D3 palette selector, clientWidth getter, single tick, fail-closed build check, strictPort | MET (mutation logs `135605`, `134515`, `155701` show the failing runs) | defects 5, 6; selector still omits `embed`, `audio/video[controls]`, `area[href]`, `dialog` (LOW) |
| D3 PR 314 thread | MET: reply cites `00ae1f04` (real, on the PR 335 branch), resolved | OK |
| D4a "tokens" word | MET (`common.tokens.split` noun-free, new `common.tokens.total` en/de; `SystemView.java` 540-546) | stale doc captures (defect 9) |
| D4b CC-P-05 reason | MET (i18n/index.ts comments, design Delivery bullet) | OK |
| D4c replies in the PR 319 threads | replies exist in all 8 threads, plus a correction comment on PR 319 | 2 of 8 are false (defect 8) |
| D4 Sonar record | MET (0, confirmed) | OK |

Logs exist under `.plan/local/plans/NO_PLAN/build-results/`: `175221` BUILD SUCCESS (the "2706 tests" are Java only, plus 1277 vitest), `180645` e2e 371 passed on 21010. "Every pushed head carried a gate run" is not verifiable per head. The gate at `175221` rewrote 8 `pm-operator` files after the merge of main (uncommitted rewrite, see PR #342).

## Defects (at `6d0686c1`)

1. MED V `MachineSettingsView.svelte` `applyConflictAgain` (~238-246): `parseCleanupRecord(conflict.currentValue) ?? {}`: when the conflict value is not a record the resend carries only the changed categories and `fromMap` fills the rest with 30/3. No test. Its severity depends on what the daemon returns on a 409 (unchecked).
2. MED V the conflict dialog's "Yours" line prints the page's old full record (`conflict.wantedValue`, ~210, via `cleanupWords` ~384), while "Apply yours again" sends the other client's values for the untouched categories. The design says the line shows the operator's choice; the test asserts the "current" words and the absence of `{`, not the "yours" line.
3. LOW V `DecisionView.svelte` 64-66: `chatBusyId` is a single slot; B finishing first clears A's busy marker while A is in flight (the PR 319 thread asked for per-decision tracking).
4. MED V no test covers the `11be8390` guards: the `chatBusyToken` clear, the `takeProposedAnswer` revision guard, the `endTheChat` generation guard (`DecisionView.test.ts` holds the A,B,A test and the `tick` to `act` change only; no `chatBusyToken` or revision test exists). On a stale revision the accepted proposal is dropped silently.
5. LOW V `check-build-output.mjs` `deriveDevMarkers`/`devPageRoute`: only the first sorted directory with a `+page.svelte` below the prefix is guarded; the plugin's `DEV_PAGE_IMPORT` regex (`devRouteExclusion.ts`) still hard-codes `src/routes/dev/`.
6. LOW V the block-comment strip in `strictPortIsBound` (`guard-ports.js` 94-96) has no test case; the mutation evidence (`135749`) predates it.
7. LOW V see D1c above: a server-side `invalid_value` refusal shows `update_failed`, no test.
8. MED V two of the eight PR 319 replies are false: `format.ts:363` ("handled in 7c7ea71c": the commit changes `DecisionView.test.ts` only (V: `git show --name-only`); the finding was "answered rather than changed" in the PR comment of 07:02) and `DecisionView.svelte:60` (same commit, test file only; the real change is `e0140658`/`11be8390` of PR 335, and it is a single slot, defect 3); `guard-ports.js:89` got the same reply twice and its fix is still a textual read. `7c7ea71c` is a real commit but exists only on the squash-merged PR 319 branch.
9. LOW V doc leftovers: `texts-and-languages.adoc:224` still shows `≥ 1.24 M tokens · 3 of 41 tasks`; `doc/claude_review/cli/resources/screens/system-view.{txt,svg}` and `system-printed.txt` show `2.02 M tokens (…)`; no German literal test for `common.tokens.total` (`SystemViewConsistencyTest.split()` recomputes with the same keys).
10. LOW V `saveCleanup` (~216-224) hard-codes the five retention keys plus remind; `CLEANUP_CATEGORY_ORDER` is a hard-coded order. CodeRabbit's body item ("derive the list from the record") is fixed for the dialog only (`cleanupWords` iterates the record's keys); the landing's "record-driven" overstates it.
11. LOW L `saveCleanup` computes the changed set against the store's record; a settings event that refreshes the store but not `cleanupRemindDays` would write the stale reminder as the operator's change.

## CodeRabbit review `5788363627` (all verified at `6d0686c1`)

The four inline threads were fixed as replied. Body: `check-build-output.mjs:47` multi-segment prefix FIXED (`ancestorOfPrefix`, fixture); `DecisionView.svelte:672-678` per-operation token FIXED as asked (single slot remains, defect 3); `:700-701` late proposal after a revision FIXED (no test, defect 4); `MachineSettingsView.svelte:34-41` hard-coded categories FIXED for the dialog, NOT as asked (defect 10); nitpick `MachineSettingsView.test.ts:349-350` FIXED (the untouched-reminder retry test fails if the reminder were always resent).

## PR #342 (the lane's follow-up)

Open, label `skip-bot-review`, 8 `pm-operator` files, +97/-89. Its body says "no hand edits ... only whitespace": FALSE (V: the diff holds 3 inserted `/*TODO: Catch specific not Exception ...*/` markers, in `EntryFlow.java` and `ActionSupportTest.java`, a changed import and a moved `@Nullable`). All 8 files are in PLAN-103's PR #341, which has merged: #342 is stale and conflicts; the markers record generic catches that came from PLAN-98 against the CLAUDE.md rule. Decision for the operator: close it; the rewrite is regenerated by the next gate run on current `main`.

## Lane deviations

All builds ran `in_process` (the routed build exited -1 with no log); the first self-review sub-agent used shell `ls`/`find` and wrote temp files; the PR body was written through the ci body slot directly; `ci pr edit` could not consume a `pr-create` body (tool gap). The daemon `cleanup_durations` form is still unchecked (`plan-marshall/plan-marshall-mcp`).

## Routing

No new plan. The MachineSettingsView items (1, 2, 7, 10, 11) and the doc leftovers (9) are PLAN-60 deliverable 4 (new; it edits the same view, test and `machine-settings.adoc`; lands after PLAN-100 on `SettingsView`). The DecisionView items (3, 4) and the two false thread replies (8) are PLAN-51 deliverable 6 (new). Defects 5 and 6 (LOW, build check and port guard) have no staged plan touching their files: recorded in epic.md Open Defects. PR #342 is stale: close it (the operator's call).
