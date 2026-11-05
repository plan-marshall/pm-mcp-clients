# Landing analysis: PLAN-95 mock-runtime-coderabbit-residue

PR #327 `9bf4b936` (squash-merged 2026-11-05T15:44Z; head `36a234f8`; 8 files). Lane `rb-95-mock-runtime-coderabbit-residue`: landing message filed 2026-11-05T15:47Z. Analysis by a read-only sub-agent (code read at the merge commit, nothing built or run); thread lists, the Sonar bot comment, the lock structure, the generation key and the lock-order thread conversation re-read by the orchestrator.

## Merge rule, thread count, Sonar policy

- The landing message states both numbers (`sonar_open_new_issues=0`, `unresolved_review_threads=0`) and **both hold (V)**: 15 threads, 0 unresolved, every thread has a lane reply and a bot reply; Sonar bot comment: Quality Gate passed, 0 new issues. The lane read the mailbox message of 2026-11-05 and acted on D7 in commit `d549d0b6`.
- CodeRabbit review `5784893187` on `96d58326` (body 11068 chars, 16 actionable, 3 nitpicks, 1 follow-up): the 15 inline threads were answered within two minutes (12:33-12:35Z) with deferrals ("out of scope", "not required for the mock", "pre-existing") and NO commit; the bot withdrew or accepted most (V). **Three were resolved although the bot's last word keeps the concern**: `60EE-P` (the lock-order finding: "independent and remains valid"), `60EE9o` (stale-generation reject in `broadcastEvent`) and `60EE-Z` (the withdrawal test has no post-withdrawal boundary). The body's follow-up (`goToState` should notify live clients of a removed operator-created decision, `MockRuntimeEngine.java` ~1221-1225) and nitpicks (`MockRuntimeEngineTest` ~1019-1024 seed an act before the rejected switch; ~1149-1157 count completed finds; `DialogueSourceTest:96` use `equals`) have no reply and no change (V by the analyst). The bot offered follow-up issues on about seven threads: none was opened (I).
- PR 320: threads `:1329/:1330` and `:8379/:8383` are accepted by the bot ("Addressed in ddce9f4") and resolved; the two `SettingsView` threads of PR 320 are resolved too (PLAN-100 answers them in PR #331, still open).

## Three layers

| Done-when | Implemented | Correct |
|---|---|---|
| D1 stores cleared, create act recorded | MET (fail-before logs 081316/081338/081508 exist) | ok |
| D2 atomic switch, facts validated | MOSTLY: `validateStateFacts` runs first, `fixtureObject` rejects a non-object; the `broadcastReset` decision is commented at ~1221 and pinned by `jumpEmitsNoResetAndPreservesEventOrder` | a non-array `facts.kept`/`facts.residue` still throws in `applyCleanupFacts` (~7491-7499) AFTER `currentScenario` and the stores changed: the switch is not atomic (thread 60EE8V deferred); the pin test checks that no reset arrives, not the order of the state's own events |
| D3 locks, decision location, started epics indexed, no `NeedsOperatorItem` id | MOSTLY (pm-api untouched, `needs-and-find.adoc:46` changed) | the `serveNeedsOperatorPage` clause ("do it or answer with a reason") has no code and no reason anywhere |
| D4 setup flow | MET (test-first log 084908) | setup completion is not gated on readiness (thread 60EE96 deferred) |
| D5 `effectiveWebPort` | MET | the only `webPort > 0 ? webPort : 7420` is inside the helper |
| D6 drain follows its withdrawal and its own list | MOSTLY: withdrawal cancels pending tasks, the lifecycle check drops late events (V: ~1349), `scheduleDrainShrink(drainItems.size())` steps once per remaining item, the four-item drain emits 4,3,2,1,0 (log 091551) | see defects 1-4 |
| D7 Sonar of PLAN-93 | MET: no `record` identifier remains (`durationsJson` at :373, :398), the literal is the constant, the bare-integer path kept with the design line `machine-settings.adoc` ~189 ("A bare number of days is what an older client sends"); Sonar 0 | |

## Defects (at `9bf4b936`)

1. **HIGH (V on the code, I on frequency): lock-order inversion, new in this PR.** `handlePostStopRuntime` holds `systemLock` (:8511) and then calls `cancelPendingTasks()` (:8525, `synchronized` on the engine, :1289) and `scheduleDrainShrink` (`synchronized (this)`, :8571); `handleDeleteStopRuntime` does the same (:8611, :8614). `goToState`, `reset` and `setScenario` hold the engine monitor and take `systemLock` through `initDefaultSystem` (:7325). A stop or withdraw concurrent with `/mock/goto`, `/mock/reset` or a scenario switch can deadlock. No test.
2. **MED: the generation guard is dead code.** `syncDrainItems` (:7421) reads `drain_generation`, and no producer sets that key (grep: that line only). The lane's reply claims a generation guard; stale events are stopped by the lifecycle check and `cancelPendingTasks` only; the bot asked for a stale-generation reject in `broadcastEvent` before buffering.
3. **MED: `cancelPendingTasks()` cancels ALL of the scenario's tasks** (:8525 at drain start, :8614 at withdrawal), including the state's own events and its `auto` transition; the bot accepted because `system.draining` has none; the drain-start cancel exists to keep the state's 800 ms event out of the two-item test.
4. **MED: "ends" is not delivered.** The four-item drain empties the list but `lifecycleState` stays DRAINING (`shrinkDrain` ~8590-8606 never transitions); the spec says it "empties and ends DRAINING"; the test asserts an empty list only.
5. **MED:** the switch's atomicity gap for kept/residue (D2 above); `keptItems.clear()` runs before the parse.
6. **LOW:** the new tests use no `Generators` input (CLAUDE.md); the drain tests run on the real clock (~5 s each) although the package-private `shrinkDrain()` hook exists; the withdrawal test can fail falsely if the DELETE takes longer than 800 ms; the concurrent-find test can run zero finds.
7. **LOW:** `broadcastEvent` now drops any `runtime.draining` sent through `/mock/broadcast` unless the state is DRAINING (:1349): undocumented.

## Lane claims false or unverifiable

"All 7 deliverables completed" and "Residue: none": overstated (the needs-page clause, "ends", about twelve deferred findings, the body's follow-up and nitpicks). Test-first for D1-D6 is shown only by build logs (they exist and show the right failures); each commit combines test and fix. The PR body names `setupNeedsSummary` and `EnrolledProject.Readiness.READY`; the code has neither (`ProjectReadiness.READY`; invented detail, I). The PR body has no D7 section and no explicit "no id field" statement beyond the doc line. The landing message cites D7 lines 8830/8850/8857; the actual lines are 8836, 8856, 8863. Sourcery and CodeRabbit re-triggers answered rate-limit notices. No browser check is claimed. Overlap with the other session's #329 and #334: nothing lost (the only nullness delta is `@Nullable` on `StartedEpic.projectId`).

## Routing

All of it into PLAN-97 new deliverable 4, "The mock's drain, locks and switch" (PLAN-97 edits `MockRuntimeEngine.java` for the drain signal and was waiting for PLAN-95): the lock order and a test of a stop racing a reset, the dead generation guard (emit the key or drop it, and reject stale events in `broadcastEvent`), the cancel scope, the meaning of "ends", the atomic switch for kept/residue, the test timing and `Generators`, the needs-page clause, the body follow-up and nitpicks, the reply in each of the three resolved-while-open threads and the follow-up issues the bot offered (open them or record why not). Row PLAN-95 `shipped`.
