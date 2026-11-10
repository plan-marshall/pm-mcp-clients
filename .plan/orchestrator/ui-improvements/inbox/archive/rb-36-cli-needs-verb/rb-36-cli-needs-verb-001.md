envelope_version=1
sender_type=plan
sender_id=rb-36-cli-needs-verb
epic=ui-improvements
kind=landing
created=2026-11-10T06:32:03Z

```landing-facts
schema=landing-facts/1
plan_id=rb-36-cli-needs-verb
epic=ui-improvements
pr=#391
merge_state=merged
cleanup_owed=false
deliverables_total=3
deliverables_done=3
total_tokens=unknown
harness=antigravity
model=unknown
effort=unknown
plugin_version=0.1.0
executor_version=0.1.2090
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
unresolved_review_threads=0
review_body_findings_total=0
review_body_findings_handled=0
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
step.sonar-roundtrip.quality_gate_status=ok
```

guard: none on this harness (unenforced)

# Landing Report: PLAN-36 (CLI "Needs You" in Every Surface)

## 1. Deliverables

- **Deliverable 1: The verb** (commit `1bb6042e`)
  - `NeedsCommandTest` on `running-work`: met. Test name: `NeedsCommandTest`; order: `test-first`; failing run: `d5439199` (`f1703339ba364e2f8dff98d7127221db.log`); passed at `1bb6042e`.
  - Terminal check (screen capture) on `running-work`: met. Order: `none`. Artifacts: `.plan/temp/terminal/36-cli-needs-verb/after/` and `.plan/temp/terminal/PLAN-36/after/`.

- **Deliverable 2: Overview and key** (commit `81d7f22e`)
  - PTY test in `running-work`: met. Test name: `NeedsIT` (`exclamationOnWaitingRowOpensDecision`, `exclamationOnNonWaitingRowJumpsToWaitingRow`); order: `test-first`; failing run: `1bb6042e` (`168242088330432a838122ec6fea6fe8.log`); passed at `81d7f22e`.
  - Terminal check (screen capture) on `running-work`: met. Order: `none`. Artifacts: `.plan/temp/terminal/36-cli-needs-verb/after/` and `.plan/temp/terminal/PLAN-36/after/`.

- **Deliverable 3: One count** (commit `bbedc7ec`)
  - Assert same counts across four surfaces: met. Test name: `NeedsOneCountTest` (`sameCountsAcrossFourSurfaces`); order: `test-first`; failing run: `81d7f22e` (`task-1094.log`); passed at `bbedc7ec`.
  - Terminal check (screen capture) on `running-work`: met. Order: `none`. Artifacts: `.plan/temp/terminal/36-cli-needs-verb/after/` and `.plan/temp/terminal/PLAN-36/after/`.
  - Browser regression check on `fully-onboarded` and `running-work`: met. Order: `none`. Artifacts: `.plan/temp/playwright-mcp/PLAN-36-*-after.*`.
  - Open Sonar issue read at PR head: met. Order: `none`. Confirmed 0 open issues, 0 hotspots, quality gate OK.

- **Pushed Heads and Test Runs:**
  - `7e4a43ea`: `verify -Ppre-commit` passed; `verify -Pintegration-tests` passed
  - `fcab0a03`: `verify -Ppre-commit` passed (PR #391 created)
  - `5bf25b10`: `verify -Ppre-commit` passed (unit test coverage boost to 89.9%)
  - `fb64802e`: `verify -Ppre-commit` passed (sync origin/main)
  - `01c7430b`: `verify -Ppre-commit` passed (sync origin/main)

## 2. Personas and Skills

- **Step Personas:**
  - Setup and planning: `plan-marshall:persona-plan-orchestrator`
  - D1 implementation: `plan-marshall:persona-implementer`
  - D2 integration testing: `plan-marshall:persona-integration-tester`
  - D3 implementation: `plan-marshall:persona-implementer`
  - Self-review: `plan-marshall:persona-code-reviewer`
  - Final integration & merge: `plan-marshall:persona-integration-tester`
- **Domain Skills Applied:**
  - D1: `pm-dev-java:java-core`, `pm-dev-java:java-lombok`, `pm-dev-java:javadoc`, `pm-dev-java:java-null-safety`, `pm-dev-java:junit-core`, `pm-dev-java-cui:cui-testing`, `pm-dev-java-cui:cui-logging`
  - D2: `pm-dev-java:junit-integration`, `pm-dev-java:junit-core`, `pm-dev-java-cui:cui-testing`
  - D3: `pm-dev-java:java-core`, `pm-dev-java:java-lombok`, `pm-dev-java:javadoc`, `pm-dev-java:java-null-safety`, `pm-dev-java:junit-core`, `pm-dev-java-cui:cui-testing`, `pm-dev-java-cui:cui-logging`

## 3. Hypotheses

- **D1 Hypothesis:** `NeedsCommand` and `NeedsCommandTest` absent, `MachineApiPaths.NEEDS_OPERATOR` present and served on `running-work` — confirmed, `pm-api/src/main/java/de/planmarshall/api/MachineApiPaths.java:26`, `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java:5925`.
- **D2 Hypothesis:** `!` not bound on `OVERVIEW` root, `openWhatWaits` does not open decision/hold/setup or jump to next waiting row, and "Needs you" group absent — confirmed, `pm-operator/src/main/java/de/planmarshall/operator/tui/TuiKeyRegistry.java:579`, `pm-operator/src/main/java/de/planmarshall/operator/overview/OverviewFullScreenApp.java:1178`, `pm-operator/src/main/java/de/planmarshall/operator/overview/ProjectsOverviewLevel.java:63`.
- **D3 Hypothesis:** status `needs_operator` and badges omit machine items `credential` and `cleanup_due`, `ConsolePrompt` and `ConsoleNotice` lack handlers — confirmed, `pm-operator/src/main/java/de/planmarshall/operator/ConsolePrompt.java:116`, `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java:5670`.

## 4. Findings

- **Pre-PR Self-Review Round 1:**
  - Finding 1: generic `Exception` catch and TODO comment in `OverviewFullScreenApp.java` — fixed in `fcab0a03`.
  - Finding 2: unlocalized blocks and since text formatting in `OverviewFormatting.java` — fixed in `fcab0a03`.
- **Pre-PR Self-Review Round 2:**
  - Verified specific `IOException` catch and localized `MessageCatalogue` formatting — open: none (clear).
- **Sonar Findings:**
  - 19 initial code smells on `fcab0a03` -> 15 fixed in `12de36ff`.
  - 4 remaining code smells on `12de36ff` -> resolved in `c4fea84e`.
  - Quality Gate coverage shortfall (70.6% on new code) -> added targeted unit tests across `pm-mock-runtime` and `pm-operator` in `5bf25b10`, increasing new code coverage to 89.9%.
  - Merge head `01c7430b`: 0 open issues, 0 hotspots, Quality Gate OK.
- **CI & PR Review Findings:**
  - GitHub Actions CI: 20/20 checks passed (`final_status: success`).
  - PR comments / threads: 0 unresolved threads, 0 review body findings.

## 5. Reviewers

- **Required bot (`coderabbit`):**
  - Quota-limited (`quota_limited: true`). Triggered at 2026-11-09T15:43:30Z, 19:47:31Z, 20:20:48Z, 21:12:11Z, 21:44:40Z, 22:12:43Z; refused on free OSS review quota. No inline review threads or actionable findings posted.
- **Optional bot (`sourcery`):**
  - Quota-limited. Refused on character diff budget.
- **Standing Review Authorization:**
  - Applied for `coderabbit` per §7: bot is quota-limited, re-triggered after every push, all review surfaces clear (0 unresolved threads, 0 review body findings), CI green, build gate green, and Sonar gate confirmed OK (0 open issues, 89.9% coverage).

## 6. Operator Questions

- **Question 1:**
  - Text: `"'git status --porcelain' printed '?? messages/'. Runbook lane setup rule 1 requires the checkout to be completely clean. How should we proceed?"`
  - Options: `["(Recommended) Remove the stray untracked 'messages/' directory so the working tree is clean", "Leave 'messages/' and abort the runbook lane"]`
  - Selected: `"(Recommended) Remove the stray untracked 'messages/' directory so the working tree is clean"`
- **Question 2:**
  - Text: `"'git -C .plan/local/worktrees/36-cli-needs-verb status --porcelain' shows spotless formatting modifications in ProjectCommand.java and ProjectCommandTest.java from the pre-commit build after merging main. PR #391 is already merged to main (commit 36fb27d9). How should we proceed?"`
  - Options: `["(Recommended) Discard the build-formatting modifications (git restore .) and remove the worktree", "Leave the worktree for manual inspection"]`
  - Selected: `"(Recommended) Discard the build-formatting modifications (git restore .) and remove the worktree"`
- Free-text messages: none.

## 7. Rule Check

- `guard: none on this harness (unenforced)`
- `spec_source`: `ledger_worktree`
- `executor-skew`: `not_applicable` (executed via `.plan/execute-script.py`)
- All section 1 rules observed:
  - Command execution: single command per call, no shell operators (`&&`, `;`, `|`), no backgrounding of builds/waits.
  - File access: read, edit, and write operations performed solely via platform file tools.
  - Command routing: all scripts executed via `.plan/execute-script.py`.
  - CI operations: executed via `plan-marshall:tools-integration-ci:ci`.
  - Build execution: executed via `plan-marshall:build-maven:maven`.

## 8. Build Server

- Setup preflight: `ready`
- Build mechanisms:
  - `daemon_longpoll`: 6 builds
    - `7e4a43ea` (`verify -Ppre-commit`): success (job `0ce0d268153c44c5ba35952d7185bbd8`)
    - `7e4a43ea` (`verify -Pintegration-tests`): success (job `085600c0250047b1981e8cb8c7b8849d`)
    - `fcab0a03` (`verify -Ppre-commit`): success (job `721516e879a9415893b8e5792ea7945d`)
    - `5bf25b10` (`verify -Ppre-commit`): success (job `75871ae32bf84b7280ec962eeae6e229`)
    - `fb64802e` (`verify -Ppre-commit`): success (job `4d2ea8d7f67b48c8a01c4c171196e2df`)
    - `01c7430b` (`verify -Ppre-commit`): success (job `d4c138a74db443619c008c928c9dc36e`)
  - `in_process_fallback`: 0
- Fallbacks / retries: none; all builds completed successfully via daemon long-poll.
- Killed jobs: none.

## 9. Residue

None. Deliverables D1, D2, and D3 are fully met, tested, and verified. PR #391 is merged to `main` at `36fb27d9db7520bc995d56b50b86b5fe1efb47fe`. The feature branch and local worktree have been cleaned up and pruned.
