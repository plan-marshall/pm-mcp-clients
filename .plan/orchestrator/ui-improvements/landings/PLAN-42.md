# PLAN-42 landing record

epic: ui-improvements
status: OPEN (checkpoint 1 of the run; the plan has not landed, no PR exists, the final landing is appended below when it comes)

## Checkpoint 1 (2026-11-23, analysed by sub-agent by reading, no build)

Message `rb-42-cli-verb-modes-and-help-001` (kind landing, `merge_state=open`, `pr=n/a`, `deliverables_done=2` of 7, harness opencode, model deepseek-v4.1-flash/high, tokens/plugin_version/Sonar `unknown`). The operator answered the plan's two questions: one PR for the whole plan, then "checkpoint now and resume later". Branch `feature/42-cli-verb-modes-and-help`, head `90757a51` (pushed; 4 commits `7b081721` D0, `37ea9e17` D4, `90757a51` D5, `19cce18b` D6, not 5 as the message says), merge-base equals `origin/main` `4d57b6fa`, nothing half-edited, no reference to a D1 to D3 symbol: safe to leave. The progress file `.plan/temp/rb-42-cli-verb-modes-and-help/progress.md` of the worktree names D1 as next.

### Layer 1: specified

| Deliverable | Done-when | Message | Verdict |
|---|---|---|---|
| D0 | printed path feeds `--file` | met | accurate |
| D0 | capture identity of the key-driven open | met | accurate for `ChangesLevel`; the app wiring is untested (L3-3) |
| D0 | hunk text, zero-file text, exit codes | met | partly: flag combinations pinned for `--tokens` and `--activity` only (L3-4) |
| D0 | clause (f), the colour sentence of `16-plan-view.adoc:114` | reported met | NOT met (L3-1) |
| D4 | singular/zero test, refused-state-read test | met | accurate; no baseline test count is stated |
| D5 | four searches, sibling check | met | accurate |
| D5 | diff only four lines, `trace.py` `BROKEN LINKS 0` | not met | accurate: `trace.py` not run, the lane cannot reach a sibling repository's script |
| D6 | stream and German texts, test-first | not met | accurate: tests written after the fix, no failing-before run |
| D6 | `reports read` all refused | met | accurate |
| D6 | `ExitCodesTest` success rows | not met | accurate, but the message omits the `drain` row (L3-2) |
| D1, D2, D3 | not started | not started | accurate |

### Layer 2: implemented

Every test the message names exists at the cited name. The `Traceability.adoc` edit is exactly the four lines (122, 178, 245, 384). `01-commands.adoc:118-122` splits the `plan --changes` entry; `plan.changes.none` is in en, de and the `plan-view.adoc` table; the German tree keys sit in `plan_view.{en,de}.properties`. Outside the declared surface: the `reports open` 404 handling (inside spec item (c), untested), the `SystemView.runningNow` `strip()` (the D4 fix) and `ConfigCommand --no-input` printing `common.conflict.changed` on stdout too.

### Layer 3: correct (reported to the operator; no fix plan staged)

| # | Severity | Finding | Where |
|---|---|---|---|
| L3-1 | major | D0 clause (f) not done: the sentence that removed and added lines "take the terminal's colours ... and keep their `−`" still stands, now false for the one-shot (ASCII `-`); the lane reconstructed (f) from a spec it read truncated at 2000 characters and reported it met | `doc/design/terminal-client/16-plan-view.adoc:114`, `doc/design/shared/plan-view.adoc:187` |
| L3-2 | major | `ExitCodesTest`: nine no-argument success rows, five fixed; `drain` still no-argument and not mentioned; `credentials`, `recipe`, `roles` admitted; the new "no parent help" guard only applies to rows with arguments, so it cannot fail for the four left; `web devices` passes with `devices` dropped (admitted) | `ExitCodesTest.java:255-258, 666-670` |
| L3-3 | major | the capture-identity guard is tested at the primitive only (`level.showFile(stale, answerReadAgainst)`); replacing `answerReadAgainst` by `changes.answer()` at apply time passes every test, which is the original defect; the `answer()` read is unsynchronised and the field non-volatile | `OverviewFullScreenApp.java:2826,2843`, `ChangesLevel.java:73` |
| L3-4 | minor | `refusesConflictingModes` pins `--tokens` and `--activity`; `--components` and `--watch` untested | `PlanCommandTest`, `Changes` |
| L3-5 | minor | `--no-input` stop prints two stdout lines, unpinned, `work-actions.adoc:187` names one | `ConfigCommand.java:735-738` |
| L3-6 | minor | `listTitlesAreGerman` asserts `"Epics:"`, identical in English and German, cannot fail | `PlanCommandTest.InGerman` |
| L3-7 | minor | no test for the `sub_workflow` kind or `titleNamesItsKind`; `startsWith("Phase")` without the trailing space drops the prefix of "Phasenplan" | `PlanCommand.java` ~307 |
| L3-8 | minor | "so that the output feeds `patch`" overstated: breadcrumb and file row first, no `---`/`+++` header | design doc |
| L3-9 | minor | printed path is padded and sanitised; control characters or two spaces do not round-trip into `--file` | `ChangesLevel.printableFileRow` |
| L3-10 | minor | `plan-view.adoc:190` does not say the one-shot now exits 1 on stderr | doc |
| L3-11 | minor | `reports open` 404-with-body and `reports read` non-empty `rawCode` untested | `ReportsCommand` |
| L3-12 to 15 | nit | literal test input instead of `Generators`; `planview` imports `OutputSafety` of the root package (cycle); `PlanCommand.refuse()` duplicates refusal helpers; dead "list moved" branch in `printOneFile` | |

Checked and fine: the D4 leading-separator fix is a real defect and the test catches it; the refused-state-read test is non-vacuous; stream moves match `03-dialogues.adoc#refusals`; no generic `Exception`, `System.out`, Mockito or new logging.

### Gates not run

Sonar open-issue count `unknown` (no PR), `-Pintegration-tests`, `-pl pm-api verify`, the e2e suite. They belong to the final PR.

### Residue the resumed run carries

D1, D2, D3 in full (D1 and D2 collide with the lines D0 edited: `01-commands.adoc:118-122` and the `:159` "help console-only"; `PmOperatorTest` and `ExitCodesTest.registryCoversEveryRegisteredVerb` change when `help` is added); D0 (f) and L3-3; D6 rows (`drain`, `credentials`, `recipe`, `roles`) or an amendment; D5's `trace.py` run; a before/after test count for D4.

### Slot and emit

PLAN-42 keeps its row `running` (the operator resumes it with the same lane command, no new emit). Nothing else is emittable: the CLI chain (44, 48, 50, 63) and PLAN-184 stay behind PLAN-42's real landing (10, 14, 3 and 3 shared files with its surface; PLAN-63 verifies help and verb modes, `CLI-UX-V-05`); every web plan shares 5 to 14 files with PLAN-144 or PLAN-181; PLAN-183 shares two files with PLAN-181 and is declared after it.
