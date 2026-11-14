envelope_version=1
sender_type=plan
sender_id=analyze-fix-build-time
epic=ui-improvements
kind=finding
created=2026-11-14T20:26:16Z

# Handover of analyze-fix-build-time (closed 2026-11-14, not finalized by 6-finalize)

The plan is archived without running 6-finalize: its changes were landed by hand as three pull requests, and
part of its own scope was not reached. This document is what a follow-up plan starts from.

## What landed (all on main)

| PR | Content | Main commit |
|---|---|---|
| #434 | build, pm-api, pm-mock-runtime (part 1 of 3) | c6a05fa8 |
| #435 | pm-operator (part 2 of 3) | b6c90174 |
| #436 | pm-web-app (part 3 of 3) | 83d9d150 |

#432 (the whole branch, 226 files) was closed: CodeRabbit refuses above 100 files and Sourcery above 20,000 lines.
Each part merged with CI green, 0 open Sonar issues, 0 hotspots, 0 unresolved review threads. The functional
review findings were fixed with tests; the rest is listed below.

Deliverables done (tasks done): D1 baseline (TASK-001), D2 scripted input (002), D3 injected bounds (003, 004),
D4 start.sh nested build (005), D5 CLAUDE.md guidance (006), D7 pm-api coverage (008, 009, 027), D8 pm-mock-runtime
coverage (010, 011, 028), D9 pm-operator console, dialogues and views (012-014), D10 Sonar exclusions of the root
POM (018), D12 pm-operator commands (015-017). D13 is partly done (019-022, 029).

## Not done: the plan's own pending scope

1. **D6 / TASK-007: re-measure and complete `work/build-time-report.md`.** The report has the baseline
   (sections 1 to 5). The after-the-fixes section exists only as `work/build-time-report-task7-draft.md`, with
   "Before and after" and "Parallel test execution and reactor parallelism" still marked pending. Missing: the
   before/after per module and per changed class, the cost the coverage tests added against the time D2 to D4 saved,
   the decision on `*IT` moves, the measured (not applied) potential of parallel surefire and `-T1`, and the
   pm-web-e2e and quality-sweep findings. Indicative only, not under the D1 protocol: a whole-tree `verify` ran
   in 6:22 min on 2026-11-14 against 10:20 min at baseline, and the quality gate (`-Ppre-commit`) in 10:25 to
   11:13 min.
2. **D11 / TASK-024, 025, 026: review that the new tests are behavioural, not coverage-only.** Three parts
   (pm-api and pm-mock-runtime, pm-operator, pm-web-app plus the whole-tree coverage run), each with a
   mutation-style spot check and a review section in the report. None was started. The functional review
   findings of the three pull requests are not a substitute: they came from CodeRabbit on the final diff.
3. **D13 / TASK-023 and TASK-030: finish the web coverage, then drop the exclusion.** Last measured lcov
   (2026-11-13, `pm-web-app/target/coverage/lcov.info`, before the review fixes): lines 91.3 %, branches 78.7 %,
   so the branch gap to the 80 % minimum is about 1.3 points (of 8,094 branches). TASK-030 lists the views still
   below the threshold (ReportsView, SystemView, EpicView, EpicDecompositionView, CommandPalette,
   CompletionReportView, ProjectOnboardingView, MachineSettingsView, SettingsView, CleanupView and the small
   components). TASK-023 then enables the 80 % thresholds in `pm-web-app/vite.config.ts` and removes
   `sonar.coverage.exclusions=**/*` from `pm-web-app/pom.xml`. The exclusion is therefore planned work, not
   only the gate decision filed in inbox message 003: both can be settled in one plan.

Coverage elsewhere, for reference: pm-operator 94.2 % instructions and 81.8 % branches when `-Pcoverage` and
`-Pintegration-tests` run separately (the combined run reads 63 %, see below).

## Deferred from the reviews of the three pull requests

In the orchestrator inbox of epic `ui-improvements`, sender `analyze-fix-build-time`:

- `-001` (archived): decisions 1 to 5 (chat fixture on `ChatRegistry`, `AnswerCommand` merge design, cleanup
  catalogue copies, `!` on a needs item, start view and chat events) and build findings 6 to 10.
- `-002`: the Generators-hygiene sweep of the pm-operator tests (about 28 findings), the offered-keys and
  duration-kinds derivations, parameterize-test nitpicks, the console-settings transcript capture, a test for
  `refreshPoppedLevel()` after the plan and epic jumps, the quality gate rewriting unrelated files.
- `-003`: one authoritative refresh-event set per stream in the web app, and the web coverage exclusion.
- `-004`: this handover, as a message.

## Facts worth keeping

- **Combined profiles under-report.** `-Pcoverage,integration-tests` in one invocation prepares the agent twice and
  the second run overwrites `jacoco.exec`: pm-operator read 63 % instead of 94.2 % / 81.8 %. Run the profiles
  separately until the build is fixed.
- **The quality gate rewrites files.** `@CsvSource({"''"})` becomes `@ValueSource(strings = {"''"})`, which turns
  an empty answer into two apostrophes (it broke `ControlDialoguesTest.planBackToCut`); the recipe is in the parent
  POM. It also reformats unrelated files (for example `ReportRecordsTest`) on every run: revert them by hand.
- **Splitting a stack of pull requests.** CI runs only for pull requests based on `main`; after a part squash-merges,
  `git rebase --onto origin/main <old part tip>` and push with `--force-with-lease`. CodeRabbit's free quota is
  one review per window (about 30 minutes); re-trigger once after the reset.
- **Sonar kept finding new issues in fixes** (restricted identifier `record`, duplicated literals, a redundant
  null test, unused imports): run the Sonar API for the pull request after every push, not only the gate status.
- The act gate (`OperatorGate`) refuses every act without an interactive terminal with exit 4 before a verb runs, so
  a terminal check inside a verb is dead code.

## Where the artifacts are

The plan directory (this file, `solution_outline.md`, `request.md`, the tasks, `work/`, `logs/`, `metrics.md`) is
moved into the archive of plans on archiving. The branch `feature/analyze-fix-build-time` is removed; its commits
stay reachable through the head ref of the closed #432 (last head b8491360).
