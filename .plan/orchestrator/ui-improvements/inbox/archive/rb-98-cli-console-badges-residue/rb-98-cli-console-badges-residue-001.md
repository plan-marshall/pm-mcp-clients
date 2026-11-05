envelope_version=1
sender_type=plan
sender_id=rb-98-cli-console-badges-residue
epic=ui-improvements
kind=landing
created=2026-11-05T14:54:13Z

```landing-facts
schema=landing-facts/1
plan_id=rb-98-cli-console-badges-residue
epic=ui-improvements
pr=#332
merge_state=merged
cleanup_owed=false
deliverables_total=5
deliverables_done=5
total_tokens=unknown
harness=antigravity
model=unknown
effort=unknown
plugin_version=0.1.0
executor_version=0.1.2036
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
step.sonar-roundtrip.open_issue_count=0
step.sonar-roundtrip.open_issue_rules=none
```

guard: none on this harness (unenforced)

# Landing Report: PLAN-98 (CLI Console Badges Residue)

## 1. Deliverables

- **Deliverable 1: Clock decoupling**
  - **Done-when 1 (Items a-d)**: met
    - `WebCommand.formatLastUsed`: accepts `Clock clock` with default `Clock.systemUTC()`; `Instant.now(clock)` decoupled from wall clock.
    - Golden transcript matching: `TranscriptNormalizer` normalizes `last used` timestamps with lookbehind `(?<=last used )\d{1,2} [A-Z][a-z]{2} <time>` to `<ago>`.
    - Tests: `WebDevicesCommandTest.formatLastUsedWithFixedClock` (order `test-first`, passed at `5e0300b3`), `WebDevicesCommandTest.machineSettingsClockMovedForward120Days` (order `mutation-after`, passed at `5e0300b3`).

- **Deliverable 2: Bounded & structured project badges**
  - **Done-when 1 (Items a-e)**: met
    - `EntryFlow.bounded`: 5-second timeout on reading project onboardings and status reads.
    - `readProjectSetupConsoleBadges`: ignores malformed entries, falls back to previous project badges on transport errors.
    - `ConsoleBadge`: immutable record `(Kind kind, String text, @Nullable String projectId, int stepNumber, int totalSteps)` distinguishing `MACHINE_SETUP`, `PROJECT_SETUP`, and `WAITING`.
    - `PmConsole`: prompt redrawing and SSE event stream serialized under `badgeRefreshLock`.
    - Terminal check: `ScreenCaptureIT` executed before and after in 120x30 and 80x24 across English and German locales.
    - Tests: `PmConsoleBehaviourTest.aMalformedEntryLeavesTheOthers`, `aFailedReadKeepsThePreviousBadges`, `aStalledRuntimeLetsThePromptReturnWithinTheBound`, `twoConcurrentRefreshesNeverInterleave`, `aProjectNamedQuestionFixOrWith40DigitsRaisesNoWarningAndNoException` (order `test-first`, passed at `878d1475`).

- **Deliverable 3: Step list, setup badge, and Enter decision**
  - **Done-when 1 (Items a-d)**: met
    - Total steps derived dynamically from `aspects.size()` (never hardcoded 7).
    - Unfinished machine setup badge matched by `Kind.MACHINE_SETUP` instead of template substring.
    - Enter on project setup badge continues project onboarding (`continueProjectOnboarding`).
    - Tests: `EntryFlowBadgesTest.aTestChangesTheBadgeTemplateInTheCatalogueAndEnterStillActs`, `enterOnAProjectBadgeContinuesProjectOnboarding`, `EntryFlowTest.theBadgeTotalFollowsTheAspects` (order `test-first` and `mutation-after`, passed at `62b250d8`).

- **Deliverable 4: Clauses PLAN-94 met only in part**
  - **Done-when 1 (Items a-h)**: met
    - Interactive devices refusal, exit code 1, and act-failed messages tested (`WebDevicesCommandTest`).
    - `OverviewFullScreenApp`: rootRows pairing by id verified (`ProjectsFullScreenTest.rootRowsMatchOriginalRowOrderAndPreserveAllElements`).
    - `StateDialogueTest`: active setup states verified without pinning default mapping.
    - `revokePromptReached`: tested on failed reads and end of input in `WebDevicesCommandTest`.
    - `ActionSupport.leaveBack`: call sites verified (`ActionSupportTest.leaveBackTestedAtCallSites`).
    - JLine raw mode decision: documented in `04-console.adoc#console-raw-mode-decision`.
    - Terminal safety: `finally` blocks in `PlansCommand`, `PlanCommand`, `ProjectsCommand`, `SystemCommand`, and `PmOperator` preserve terminal ownership across scoped lending and close terminals safely without masking exceptions (`OperatorContextTerminalTest`).
    - Passed at: `fa8077ff`, `2117cbe5`, `972d5cec`.

- **Deliverable 5: CodeRabbit threads, analysis leads, Sonar**
  - **Done-when 1 (Items a-e)**: met
    - Answered all 8 residue review threads on PR 321.
    - Investigated and documented 3 analysis leads (MockRuntimeEngine project_id matching, SocketChannel interrupt behavior, JLine EOF translation).
    - SonarCloud gate: 0 open issues confirmed on PR head (`972d5cec` and `483c10c33dbaae97294f674231d27b7005ac7128`).
    - CodeRabbit review on PR #332: all 10 review threads triaged, code improvements committed (`483c10c3`), and all threads answered and marked resolved.

## 2. Evidence Table

| Check | Target | Status | Notes |
|---|---|---|---|
| D1 Clock test | `WebDevicesCommandTest.formatLastUsedWithFixedClock` | Pass | Test-first; commit `5e0300b3` |
| D1 Clock mutation | `WebDevicesCommandTest.machineSettingsClockMovedForward120Days` | Pass | Mutation-after; commit `5e0300b3` |
| D2 Badges tests | `PmConsoleBehaviourTest` (5 new tests) | Pass | Test-first; commit `878d1475` |
| D2 Terminal capture | `ScreenCaptureIT` | Pass | Before/after 120x30 and 80x24 EN/DE |
| D3 Steps & Enter | `EntryFlowBadgesTest` & `EntryFlowTest` | Pass | Test-first / mutation-after; commit `62b250d8` |
| D4 Partial clauses | `WebDevicesCommandTest`, `OperatorContextTerminalTest`, `ActionSupportTest` | Pass | Commits `fa8077ff`, `2117cbe5`, `972d5cec` |
| D5 Sonar Gate | SonarCloud Analysis | Pass | 0 confirmed open issues at head |
| D5 CodeRabbit | 10 PR #332 threads | Pass | All 10 answered and resolved |
| CI Build | GitHub Actions (Java 25, 26, ITs, Sonar) | Pass | Run `42044230044` / `42044230121` |
| Merge | `main` | Merged | Commit `6be3c48fdcec0787ce1bdd7f5d3b9534577925c2` (PR #332) |
