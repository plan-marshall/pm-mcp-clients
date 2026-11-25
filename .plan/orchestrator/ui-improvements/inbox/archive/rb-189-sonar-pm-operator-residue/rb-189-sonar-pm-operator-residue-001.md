envelope_version=1
sender_type=plan
sender_id=rb-189-sonar-pm-operator-residue
epic=ui-improvements
kind=landing
created=2026-11-25T13:28:48Z

```landing-facts
schema=landing-facts/1
plan_id=rb-189-sonar-pm-operator-residue
epic=ui-improvements
pr=#571
merge_state=merged
cleanup_owed=false
deliverables_total=2
deliverables_done=2
total_tokens=unknown
harness=antigravity
model=unknown
effort=unknown
plugin_version=0.1.0
executor_version=0.1.2183
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
unresolved_review_threads=0
review_body_findings_total=1
review_body_findings_handled=1
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
step.sonar-roundtrip.quality_gate_status=ok
```

guard: none on this harness (unenforced)

## 1. Deliverables

- **Deliverable 1 — The six open `pm-operator` issues and the misstated nullness:** met
  - **Done-when 1 (PR & default-branch Sonar clear):** met (`order: none`).
    - PR #571 read at pushed merge-ready head `0749216c9de56c8ae071d796a8d66612e6c11a44`: `sonar read_open_issues --pr 571` returned `count_status: confirmed`, `open_issue_count: 0`, `hotspots: []`, `analysis_revision == head_revision == 0749216c9de56c8ae071d796a8d66612e6c11a44`, and `sonar_rest gate-status --pr 571` returned `gate_status: OK` (`new_coverage: 87.6%`, `new_duplicated_lines_density: 0.0%`).
    - Post-merge `main` analysis: `sonar_rest ce-status --branch main` returned `AaIS4S1WZJZ1WS7XxNfv` (`status: SUCCESS`, `branch: main`, `submittedAt: 2026-11-25T13:24:36Z`, `executedAt: 2026-11-25T13:24:43Z`) after merge commit `2b6939fe3d3ad6f07b9cb77af81e895d0d979f0c` landed at `2026-11-25T13:01Z`. Post-merge `sonar read_open_issues --project plan-marshall_pm-mcp-clients` returned `count_status: confirmed`, `open_issue_count: 3`, clearing all 6 `pm-operator` keys (`AaFfVbmag_p_FqfrTT28`, `AaFfVblVg_p_FqfrTT2o`, `AaFfVbnQg_p_FqfrTT3J`, `AaFfVbnQg_p_FqfrTT3K`, `AaFfVblug_p_FqfrTT20`, `AaFMIQUlEHNTGUcqN-Y-` via operator-selected `@SuppressWarnings("java:S1075")` on `LessonSourceCommand`).
    - Commits: `70902cd8`, `668e2156`, `141e56be`, `64861fb8`, `0749216c`, merged as `2b6939fe3d3ad6f07b9cb77af81e895d0d979f0c`.
  - **Done-when 2 (Builds & resource/capture diff guard):** met (`order: none`).
    - Pushed head `70902cd8`: `verify -Pintegration-tests -pl pm-operator` passed (`131` tests, `task-157`), `verify -Ppre-commit -pl pm-operator` passed (`task-164/170`), `verify -Pcoverage -pl pm-operator --timeout 1421` passed (`task-191/195`).
    - Pushed head `668e2156`: `verify -Ppre-commit -pl pm-operator` passed (`721s`, `cc4f08ce1813423993d9fa782b8b0022`), `verify -Pintegration-tests -pl pm-operator` passed on rerun (`378s`, `40099fdd06c74c7c8a6cdfeb71aa3bd4`; initial run `e2eb6c0d06cf4a34938e9eb10c6480fd` failed `1/131` in `InterruptDialogueIT.projectSetupInterrupt` PTY `^C` timing flake).
    - Pushed head `141e56be`: full `verify` passed (`1339s`, `aeef569efd1448b183993828b299d63d`).
    - Pushed head `64861fb8`: `verify -Ppre-commit -pl pm-operator` passed (`421s`, `601ea750254d41d8b003a3ce430de676`), `verify -Pintegration-tests -pl pm-operator -am` passed (`328s`, `e95f9d6bcc344bc18bcdb41a55fe5b10`), full `verify` passed (`692s`, `42729cf766314397bec00f6856d1d2d9`).
    - Pushed head `0749216c` (merge commit head): full `verify` passed (`527s`, `70c4a9e4e87442c59242937da8cc8401`), `verify -Pintegration-tests -pl pm-operator -am` passed (`277s`, `1107` tests executed, `8bfc1b813b31444591c1fa284573e7da`).
    - `git diff --name-only origin/main...HEAD` changed only 9 `.java` files (`0` under `src/main/resources`, `0` under `src/test/resources/screen-captures`); 2 tests added, 0 removed/relaxed.
  - **Done-when 3 (Test reachability & added tests):** met (`order: none` — both added tests were written at `20f971fe` prior to production refactoring in `task-135`, where both branches already existed on `main` under the no-behaviour-change invariant; no line-removal mutation run was executed).
    - `HoldDialogue.execute`: `ControlDialoguesTest` (`holdRetry`, `holdAccept`, `holdStillHeld`), `ControlDialoguesTest.HoldDialogueBehaviour` (`notHeld`, `notOfferedAskedAgain`, `wireNameNotOffered`, `germanLeave`, `preselectedActOfGoneHoldIsAsked`, `preselectedActNotOfferedIsAsked`, `preselectedActOfNamedHold`, `endPlanSendsReason`, `endPlanBlankReasonRefused`, `declinedSendsNothing`, `refusedAct`, `stillHeldThenGone`, `noInputStops`, `showPullRequest`), `InterruptExitCodeTest`, `ActionsFullScreenTest.shouldResolveHold`.
    - `WorkChatDialogue.execute`: `ControlDialoguesTest` (`chatTurn`, `chatProposalAccept`, `chatEndedFollowUp`, `chatAnsweredProposalAsksForAMessage`, `chatReject`), `WorkChatDialogueTest` (`unofferedInputAtTheProposal`, added `noProposalNoticePrintedOnceAcrossTurns`).
    - `WorkActionDialogue` `findings`/`watches`: `ControlDialoguesTest` (`workActionFindings`, added `workActionWatches`).
    - `OverviewFullScreenApp.openWhatWaits`: `OverviewWaitsKeysTest` (`NeedsItems.opensTheSubjectsAct`, `jumpsToTheNextWaitingRow`, `projectSetup`, `WorkLevels.heldPlanOpensItsHold`, `WorkLevels.planWithoutHoldOpensItsDecision`, `WorkLevels.unreadablePlanOpensItsActions`, `WorkLevels.quietPlanJumps`, `WorkLevels.epicWithFindingsOpensThem`, `WorkLevels.epicWithoutFindingsOpensActions`, `WorkLevels.planOfEpic`, `WorkLevels.planView`), `OverviewFullScreenAppTest` (`bangOnANeedsRowRunsThePrintedCommand`, `keyHandlerNavigationAndHelp`), `EpicViewFullScreenTest.shouldOpenTheHold`.

- **Deliverable 2 — The default-branch read:** met (`order: none`).
  - Post-merge `sonar_rest ce-status --branch main`: `AaIS4S1WZJZ1WS7XxNfv` `SUCCESS` (`executedAt: 2026-11-25T13:24:43Z` > merge `13:01Z`).
  - `sonar read_open_issues --project plan-marshall_pm-mcp-clients`: `count_status: confirmed`, `open_issue_count: 3`, `hotspots: 0`.
  - Counts by module: `pm-operator`: `0`; `pm-mock-runtime`: `2`; `pm-exec`: `1`.
  - Counts by rule: `java:S1313`: `1`; `java:S4449`: `1`; `java:S112`: `1`.
  - Open keys:
    1. `AaFGfsYupdXNQ-N4ArY-` (`java:S1313`, MINOR) — `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java:11006` (PLAN-181)
    2. `AaGRMDtGxhXuOwwhaS0K` (`java:S4449`, MAJOR) — `pm-mock-runtime/src/main/java/de/planmarshall/mock/MockRuntime.java:245` (PLAN-181)
    3. `AaEcTdY0MJNX7b6L9QAy` (`java:S112`, MAJOR) — `pm-exec/src/main/java/de/planmarshall/exec/NativeKernel.java:229` (PLAN-181)

## 2. Personas and skills

- Deliverable 1: `Persona: plan-marshall:persona-implementer`; declared skills `pm-dev-java:java-core`, `pm-dev-java:java-null-safety`, `plan-marshall:ref-code-quality` (loaded along with `persona-module-tester`, `pm-dev-java:junit-core` for test additions, and `persona-code-reviewer` for self-review and PR comment triage).
- Deliverable 2: `Persona: plan-marshall:persona-implementer`; declared skills `pm-dev-java:java-core`.
- Surface drift: `skills added: none` (only `pm-operator` was modified, including `ReviewDialogue.java` in `de.planmarshall.operator` for shared package-private `REGISTER_FINDINGS`/`REGISTER_WATCHES` constants and two `pm-operator` test files).

## 3. Hypotheses

- `HoldDialogue.execute` `java:S3776` drops below threshold by extraction without reordering effects — confirmed, `HoldDialogue.java:102-141` (extracted `preselectedActFor`, `applyAct`, `StepOutcome`).
- `WorkChatDialogue.execute` `java:S3776` drops below threshold by extraction without reordering effects — confirmed, `WorkChatDialogue.java:125-160` (extracted `showTranscript`, `askMessage`, `reportNoProposalOnce`).
- `openWhatWaits` is called only from one inner class so relocation avoids cascading `S3398` — confirmed, `OverviewFullScreenApp.java:1374` had a single caller at `OverviewFullScreenApp.java:2513` (`OverviewKeyHandler`); relocated `openWhatWaits` + 7 wait-only helpers to `OverviewKeyHandler` and made `openActions()` package-private.

## 4. Findings

- Self-review Round 1 (`self` subagent at `70902cd8`): `0` confirmed findings, `0` pre-existing test assertions removed/relaxed, `0` docs/comments made false, `0` unexplained files.
- CI on `70902cd8` & `668e2156`: `build` & `sonar-build` failed due to pre-existing `origin/main` (`20f971fe`) `pm-api` `IntentCatalogueTest.shouldCarryExactlyTheDocumentKeys`; resolved by merging `origin/main` (`52aa9e4b`) in `141e56be`.
- CI on `141e56be`: `build / quarkus-alignment` failed transiently with HTTP 429 fetching `quarkus-bom-3.40.1.pom` from Maven Central (green on `64861fb8` & `0749216c`).
- PR #571 Sonar at `141e56be`: `2` open `java:S1192` findings (`AaISdgXoCkxhvXI3u3ow`, `AaISdgXoCkxhvXI3u3ox` on `WorkActionDialogue.java:504`) fixed in `64861fb8` by removing `ACTION_FINDINGS`/`ACTION_WATCHES` so each literal appears only twice; `0` open findings at `64861fb8` and `0749216c`.
- CodeRabbit inline thread `PRRT_kwDOVBOtss671bjl` (`coderabbit inline inline-1`, `WorkChatDialogueTest.java:136` mock fixture never settled replies within `CHAT_REPLY_BOUND`): fixed in `668e2156`, replied and resolved.
- Mailbox reads (1st, 2nd, and 3rd): `orchestrator inbox read --slug ui-improvements --plan-id 189-sonar-pm-operator-residue` failed with `error: invalid_target_plan` (script regex rejects digit-leading `plan_id`), so `mailbox_state` was not `present`.

## 5. Reviewers

- `coderabbit` (`required_bots`, `quota_limited=true`, `review_complete_after_handling=true`):
  - Attempt 1 (`07:59:05+01:00` on `70902cd8`): refused (`Review limit reached` / `Review rate limited` at `06:59:11Z`); waited 90 minutes.
  - Attempt 2 (`09:30:11+01:00` on `70902cd8`): refused (`Review rate limited` at `08:30:31Z`); waited 90 minutes.
  - Attempt 3 (`11:01:18+01:00` on `70902cd8`): reviewed (`PRR_kwDOVBOtss8AAAABaO_RTw` at `10:05:53Z`; summary marker `coveredCommitId=70902cd814195e004b2c8b6420468d24a6db6dc6`, `kind=reviewed`). One inline finding (`coderabbit inline inline-1`) fixed in `668e2156`, replied to and resolved; not re-triggered on `668e2156`/`141e56be`/`64861fb8`/`0749216c` per `review_complete_after_handling=true`. `review-gate` at `0749216c`: `merge_allowed: true`, `reason: all_handled`.
- `sourcery` (`optional_bots`, `quota_limited=true`, `review_complete_after_handling=false`):
  - Triggered at `07:59:20+01:00` on `70902cd8`: refused (weekly diff-char budget exhausted, `1 day and 5 hours` at `06:59:30Z`); non-gating optional reviewer.

## 6. Operator questions

1. **Question:** `Sonar issue AaFMIQUlEHNTGUcqN-Y- (java:S1075) flags LessonSourceCommand.java:45 PATH_PROJECTS_PREFIX = "/api/v1/projects/", which is already a named constant. Sibling command classes on main (AcceptEditCommand.java:66, ConfigCommand.java:68, CredentialsCommand.java:55) carry class-level @SuppressWarnings("java:S1075"). How should this finding be handled?`
   - **Options:** `(Recommended) Add class-level @SuppressWarnings("java:S1075") on LessonSourceCommand`, `Dismiss AaFMIQUlEHNTGUcqN-Y- in Sonar`, `Leave AaFMIQUlEHNTGUcqN-Y- open and stop`
   - **Selected:** `(Recommended) Add class-level @SuppressWarnings("java:S1075") on LessonSourceCommand (matches AcceptEditCommand:66, ConfigCommand:68, CredentialsCommand:55 on main)`
2. **Question:** `PR #571 (head 70902cd8) is blocked by a pre-existing failure on origin/main (20f971fe): pm-api IntentCatalogueTest.shouldCarryExactlyTheDocumentKeys fails...`
   - **Options:** `Pause/wait here until origin/main fixes IntentCatalogueTest (and CodeRabbit 90-minute window opens at 09:29:11+01:00)`, `Stop this run now with PR #571 left open`
   - **Selected:** `(Recommended) Pause/wait here until origin/main fixes IntentCatalogueTest (and CodeRabbit 90-minute window opens at 09:29:11+01:00), then merge origin/main and continue PR #571`
- **Free-text operator messages:** none.

## 7. Rule check

- `guard: none on this harness (unenforced)` (`guard_log`: `.plan/temp/guard.log` does not exist).
- `spec_source: ledger_worktree` (`spec_differ` absent).
- `executor-skew: match` (`executor_version: 0.1.2183`, `lane_version: 0.1.2183`).
- Own actions recorded: `0` shell file read/search violations; `0` direct `gh`/`mvn` calls (all CI via `ci`, all builds via `maven` / `build_server`, `19` routed builds including failed pre-existing `pm-api` checks and 1 `InterruptDialogueIT` PTY flake rerun); `1` `self` subagent dispatched (Round 1 self-review); `0` killed jobs or killed subagents; worktree & local/remote feature branch cleaned up via sanctioned commands and main checkout updated to `2b6939fe`.

## 8. Build server

- Setup preflight: `ready` (`build-routing`: `executor_in_worktree: present`, `auto`).
- Counts by mechanism: `daemon_longpoll: 19`, `in_process_fallback: 0`, `killed: 0`.
- Fallbacks / retries: `none` (`task-181/185` timed out at daemon 471s learned bound during `-Pcoverage`, then succeeded cleanly in `task-191/195` with `--timeout 1421`; initial `-Pintegration-tests` run `e2eb6c0d` failed 1/131 in `InterruptDialogueIT.projectSetupInterrupt` PTY flake and passed on rerun `40099fdd` + final `8bfc1b81`).
- Runtime stamp note: landing-time `resolve` returned `model: unknown`, `effort: unknown` (`runtime_stamp_reason` absent).

## 9. Residue

- All 6 target `pm-operator` Sonar keys and `PlanSettingsDialogue.tooLate` nullness contract are cleared on `main` (`2b6939fe`).
- 3 pre-existing PLAN-181 Sonar issues remain open on `main` for manual operator dismissal (`AaFGfsYupdXNQ-N4ArY-`, `AaGRMDtGxhXuOwwhaS0K`, `AaEcTdY0MJNX7b6L9QAy`).
- Tooling note: `orchestrator inbox read --plan-id 189-sonar-pm-operator-residue` fails with `invalid_target_plan` because its regex (`^[a-z][a-z0-9-]*$`) rejects digit-leading plan IDs.
