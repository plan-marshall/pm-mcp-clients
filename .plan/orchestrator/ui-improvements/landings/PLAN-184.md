# PLAN-184 landing record

epic: ui-improvements
plan: rb-184-cli-plan-settings-refusal-causes (runbook lane, harness opencode, `deepseek-v4.1-flash/high`, effort high; `total_tokens` `unknown`, `plugin_version` 0.1.2183 from the lane text; executor 0.1.2183)
pr: #587, squash `9011e6d1` (18 files, +419/−97) on `51e3bcc9`, merge queue (position 1, merge-group run 45757220035 per the message), merged 2026-11-26T05:01:18Z
analysed: 2026-11-26, by sub-agent in three layers, by reading only (no build); `PlanSettingsDialogue.java:248-255` (`refusalReason`) and `PlanSettingsUpdateResult.java:80-83` (`resultOrNull`, no `@Nullable`) re-read by the orchestrator at `9011e6d1`

Message `rb-184-cli-plan-settings-refusal-causes-001` (revision 1, 2026-11-26T05:02:29Z): `merge_state=merged`, `deliverables_done=2` of 2, 0 unresolved threads, 2 of 2 review-body findings handled, `sonar_open_new_issues=0`, `self-review: clear`. No operator question. Binding scope: the spec's `## Re-Grounding 2026-11-25` with the "At emit (163e2a49)" bullet.

**Merge corroborated:** `gh pr view 587` merged 05:01:18Z, merge commit `9011e6d1b23ce5be1b2e63d601db71ab22b42837`, now `origin/main`'s head. Diff: 17 code files, 1 `doc/` file.

**Sonar.** PR 587: 0 issues (SonarCloud search by the sub-agent), gate OK. Default branch read 2026-11-26 after the `main` task `AaIWTII6yu25SKZIp0Bi` (submitted 05:20:42Z, SUCCESS, after this merge at 05:01:18Z): gate OK, coverage on new code 90.2 %, `count_status: confirmed`, **3 open**, the three PLAN-181 keys, none in a file this PR touched (`MockRuntimeEngine.java` is touched at `:10690` — the import form — while the open key sits at `:11112`, unchanged since PLAN-181).

**SNAPSHOT deploy of `9011e6d1`:** `unknown`.

## Layer 1: specified

| D | Verdict |
|---|---|
| D1 the refusal names the runtime's cause, the invented pair gone | met: DW1 `test-first` by job `080e5355…` (12 named failures; tests and fix share `2ef0f0ca`, so only the log shows the order) — `PlanCommandTest.java:1222,1245,1260,1275` (de), `ControlCommandTest.java:428,446` (en); the revision conflict stays covered (`ControlCommandTest.java:212,243`). DW2 `mutation-after`: `settingsRetriedWriteNamesTheCause` (`ControlCommandTest.java:465`, job `3208c441…`), the dialogue's coded 409/400 (`ControlDialoguesTest.java:1159,1184`, jobs `2c83c699…`, `ac3a8562…`). DW3: whole-tree `verify` 5684 tests (`48d43530…`), coverage and pre-commit green; the exact `-pl` run was not made (disclosed); `base_branch_fixed` 3 keys in en and de, `does_not_fit` gone from both |
| D2 the open code is reported | met: PR § "The open question" and message § 6 name the three places and choose no code; `02-levels.adoc:108` states the rule for both paths (validate/verify-links/docs review cited, not re-run) |

Every D1 "What" item traces: the 422 arm removed; coded 409 split from a settings result (`PlanCommand.java:1738-1743`, `PlanSettingsUpdateResult.java:83-97`); `RefusalRenderer` renders the cause (`:1759-1760`); `ApiErrorCode.BASE_BRANCH_FIXED` (`:100`); the `WorkActionsClient` 409 hole closed (`:247-252`); "not one of" first (`:1755-1758`). `act_not_available` handled generically, untested. True count: 2 of 2.

## Layer 2: implemented

All 18 files trace (D1: 16, D2: `02-levels.adoc` in place of `01-commands.adoc`, whose row names no refusal). Outside the surface but explained: `ControlDialoguesTest`, `ControlRecordsTest`, `ApiContractTest` (D1); `MockRuntimeEngine.java:10690`, `OnboardingScenarioTest.java:24,902` (the quality gate's import form); `ChatRegistry.java:1219-1221` (a 3-line Javadoc, merge leftover). **Stale or false claims:** the `857dd6e0` commit message (the parent had the qualified `java.util.regex.Pattern`, from #569; corrected in the PR body, kept out of `main` by the squash); the PR body's "this PR completes the pair" for `control.chat.line.decision_opened` (already on `main` from #582, not in the diff); the CodeRabbit "producer fix in `2189ae58`" (net #582's; only the dialogue-retry half is this PR's, `PlanSettingsDialogue.java:225-228`, test `ControlDialoguesTest.java:1240`); `ChatContextNarrativeTest` cited as a guard, not in the diff; the message's "touched `ChatRegistry.java`" (Javadoc only).

## Layer 3: correct (reported to the operator; no fix plan staged)

No blocker, no major. Clean: no generic catch (the new one is `IOException _`), no `System.out` (picocli's `getErr()`), German texts correct, en/de parity, no dead key.

| # | Severity | Finding | Where |
|---|---|---|---|
| L3-1 | minor | a refusal without a code (the open `invalid_value`-on-400 case) shows the runtime's own reason in the dialogue — English on a German screen; D1 forbade it (PLAN-174 L3-7), the design line `02-levels.adoc:108` documents it. The one-shot is clean | `PlanSettingsDialogue.java:248-255` |
| L3-2 | minor | the 3 new dialogue tests sit in the outer class without `@EnableGeneratorController` and take hand-written input; the `--yes` test likewise (CLAUDE.md Generators rule) | `ControlDialoguesTest.java:71,1159-1270`; `ControlCommandTest.java:465-485` |
| L3-3 | minor | `resultOrNull` returns `null` by contract without `@Nullable` (the package is not `@NullMarked`, pre-existing) | `PlanSettingsUpdateResult.java:83` |
| L3-4 | minor | the 400 branch builds `OperatorRefusedException` (parses the body) and parses it again for `reason`, against the spec's "reuse it" | `WorkActionsClient.java:253-258` |
| L3-5 | info | § 9 residue confirmed: on `--yes`, a second conflict or a too-late result prints the neutral line with exit 1; a first write answered too-late ends neutral; the one-shot never says "too late" | `PlanCommand.java:1686-1694,1710` |
| L3-6 | info | the dialogue's mirror of L3-5: a second conflict or too-late on retry asks the value again silently (pre-existing, not in the residue) | `PlanSettingsDialogue.java:225-229` |
| L3-7 | info | the discriminator holds (body with `code` and `state` → `null`, both shapes tested `ControlRecordsTest.java:178`); a flat `code` beside `applied`/`conflict` would read as a result | `PlanSettingsUpdateResult.java:86-93` |
| L3-8 | info | constants declared between methods | `PlanSettingsUpdateResult.java:62-72` |

**Question for the Runtime Model owners (with D2's open code), unconfirmed:** the specification lists `conflict` as a coded 409 "carrying the current revision" (`runtime-model/01-processes-and-transport.adoc:237`). If a runtime answered a settings revision conflict with an `error` key, `resultOrNull` returns `null` and the one-shot renders `error.conflict.*` instead of the conflict path (`--yes` retry, `--no-input` exit 3). The contract record and the mock send `{"applied":false,"conflict":true,…}` without `error`; the daemon has no settings code yet.

## Residue for the plans that touch the same surface

Shared code files with this merge: PLAN-194 (running) and PLAN-196 (staged) `control.{en,de}.properties`; PLAN-42 (parked branch) `PlanCommand.java` — its rebase now also meets this merge's `applySettings` changes; PLAN-197 (staged) none in code. `SettingsView.svelte:735` still reads `invalid_value` (the open question).
