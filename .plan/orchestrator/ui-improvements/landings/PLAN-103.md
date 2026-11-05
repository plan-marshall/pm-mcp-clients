# Landing analysis: PLAN-103 cli-console-second-residue

PR #341 `24523d67` (squash-merged 2026-11-05T22:02Z; head `42034a6f`; 9 commits, 27 files). Lane `rb-103-cli-console-second-residue`: landing message filed 2026-11-05T22:04Z. Analysis by a read-only sub-agent (code read at the merge commit, logs read, nothing built or run); the thread counts, the Sonar count and the central findings (marked V*) were re-read by the orchestrator.

## Merge rule

PARTLY SATISFIED. CodeRabbit reviewed `ccf9e063` (review `5791531265`: 4 inline threads, all fixed in `5968a986`, replied in the thread and resolved) and `e1e05fe5` (review `5792262598`, 45 minutes before the merge). FIVE BODY ITEMS ARE UNHANDLED AND UNANSWERED (V*: PR 341 holds only `@coderabbitai review` triggers and rate-limit notices as issue comments; the lane's reviews have empty bodies): follow-up `EntryFlow.java:657-660`, follow-up `StateDialogue.java:47-51`, nitpicks `EntryFlowTest` ~589-590 and `PmConsoleBehaviourTest` ~420-421, nitpick `StateDialogueTest.java:245`. The lane message says "all findings fixed" and "Residue: none": FALSE.

- Unresolved threads at the merge head: PR 341 0 of 4, PR 321 0 of 14, PR 332 0 of 10 (V: GraphQL). The lane did not state the count (the facts block has `open_issue_count=0` only).
- SonarCloud: `0 New issues` (V: bot comment; check run `SonarCloud Code Analysis` success). The first head had 6 smells (`S1135`, `S2093`, `S2925`, `S8491`); two of the fixes were not real (defects 6 and 11).
- Lane message wrong in places: clause 8 cites a PASSING log (`181345`, BUILD SUCCESS) as a "failing run"; a test name differs; the same review id is cited for two heads.

## Three layers

| Done-when | Implemented | Correct |
|---|---|---|
| D1 200 without `onboardings` keeps cached badges | MET (`EntryFlow.java:636-638`; test-first credible, log `164548` against unchanged main shows the failures) | the spec never asked whether the real daemon omits the list for an empty list |
| D1 prompt not delayed by a stalled refresh | MET (`PmConsole.java:338-369`, `683-718`, `tryLock(5s)` plus remaining budget) | defects 6, 11, 12 |
| D1 badge total follows the aspects, one source | MET for `EntryFlow` (`STEP_KEYS`, `MACHINE_STEPS`, `STEP_WHAT_KEYS` gone) | not one source overall (defect 8) |
| D1(d) Enter on a project badge addresses the run | NOT MET in substance | defect 1 |
| D1 specific catches | MET to the letter (the named lines) | other generic catches remain (defect 13) |
| D1(f) test-only surface leaves production | PARTLY | new test-only surface added (defect 7) |
| D2 design: Enter rule, raw-mode decision | MET (`04-console.adoc:24` and `[#console-raw-mode-decision]` ~239 match the code) | the decision of D1(d) is not in the design |
| D2 German row | PARTLY: the pattern hard-codes `(?:last used|zuletzt benutzt)`, only a synthetic test | defect 4 |
| D2 clock test | MET in code (two fixed clocks) | the "fails before" evidence is the reactor, not a reverted clock read (defect 5) |
| D3 tests (F3, F4, N1-N6) | N1, N3, N4, N6, `reconnectLatch` done; N5 weak; F3 partial; F4 NOT done | defects 7, 9, 10, 14 |
| D3 mutation-after, 7 items | MET: logs `175720`…`180240` each show a distinct failing test | the reverted code is not recorded; the tests are pre-existing |
| D3 clause 8 terminal capture differs | NOT AS CLAIMED | defect 3 |
| D4 PR 321 threads, 10 body findings | the 8 threads really answered; the 10 findings answered in ONE list comment, not each | section "Replies" |
| D4 Sonar 0, thread count | 0 per the bot; count not stated | |

## Defects (at `24523d67`)

1. HIGH V* Enter on a project badge still addresses the checkout directory name: `EntryFlow.java:657-664` reads `JsonTree.string(map, "project_id")`; `BeingAddedProject` has only `checkoutPath`, `currentStep`, `stepNumber`, `totalSteps`, `continueLink`, `abandonLink` (V*: read the record); the test `enterOnAProjectBadgeAddressesDaemonProjectId` writes `project_id` into its own fixture. `project-onboarding.adoc:77` says a pre-enrolment run is entry-scoped; CodeRabbit's last reply on `:712` said the same ("does not address pre-enrolment continuation when project_id is absent"). The spec's verify-first clause was not honoured; the reason is only in a thread reply.
2. HIGH V five CodeRabbit body items unanswered (above); follow-up 1 is real: `PmConsole.java:288` builds `project setup "…"` and `parseArgs` (~899-923) reads no backslash escapes.
3. MED V the D3 clause 8 evidence is not probative: `before` is the `status-only` scenario, `after` is `fully-onboarded` (different badge line and daemon version); the `failing` and `recovered` captures of `after` are byte-identical; `ScreenCaptureIT` asserts no content.
4. MED V the German row: no catalogue read, no German transcript line.
5. MED V the clock test's failing logs `172358`/`172518` ran `-pl pm-operator` against a registry-snapshot `pm-api` ("Seven steps.", ages from the real clock); `172622` (`-pl pm-api,pm-operator`) passes in the same 21 s; there is no mutation run with the console reading the system clock.
6. MED V `EntryFlow.bounded` (~574-601) lost its `RuntimeException` path in `6f0252fe`, the "fix" of the S1135 flag on the rewrite's TODO marker (the marker was deleted together with the behaviour): a worker exception kills the thread, prints a stack trace and returns `null`; callers dereference it. No realistic trigger found (`JsonTree.number`, `BeingAddedProject.fromMap` throw IOException only); no test. The worker is interrupted on timeout only; a worker blocked in socket I/O may leak one thread per refresh (L).
7. MED V new test-only surface in production: `MessageCatalogue.setForTest`/`clearCacheForTest` (public API of `pm-api`, mutating a JVM-wide static), `StateDialogue.PROJECT_STATES` (V*: a hand-written 16-id list, read only by a test, a second hand list beside the switch), the `PmConsole` constructors `(context, initialBadges)` and `(…, machineSteps)`, `EntryFlow.getStepName(…)` (hard-codes `OnboardingAspect.values()`).
8. MED/LOW V the step count is not one source: `PmConsole.java:98,102` takes `machineSteps` from a throw-away `new EntryFlow(context)` (the real flow is created at :148); `onboard.entry.intro` is hard-coded; one session prints "7 steps." (`console-settings.transcript:5`) and "Seven steps." (:20); `05-transcripts-entry.adoc:145,160,477`, `web-app/onboarding.adoc:72` and the claude_review screens still show "Seven steps.".
9. LOW V `EntryFlowTest.java:623` is a tautology (`contains("8") && contains("Language")`).
10. LOW V padding tests added for Sonar coverage in `ccf9e063`: hand-written literals, no `@EnableGeneratorController`; `refreshBadgesUpdatesBadgesAndWindowTitle` asserts no title.
11. LOW V the S2925 "fix" is a sleep: `new CountDownLatch(1).await(4, SECONDS)` in the stalled-server handlers; the deadline test has 1 s of margin against the 5 s budget.
12. LOW V the `Math.max(1L, bound.toMillis())` clamp (`EntryFlow.java:587`) is pinned by no test.
13. LOW V other generic catches in `pm-operator/src/main`: `EpicCommand:108`, `PlanCommand:146,178` (`catch (Exception`); `TuiScreenDriver:232`, `OverviewStateStore:95`, `AnswerCommand:247`, `WebCommand:486,681` (`catch (IOException | RuntimeException`); 14 `/*TODO: Catch specific…*/` markers repo-wide (pm-api `Libc:91`, `ScenarioLoader:566`, `MockRuntimeEngine` x7, plus the operator files); a stale `// cui-rewrite:disable` at `EntryFlow.java:732`.
14. LOW V N5 weak: `ActionSupportTest.leaveBackPreservesNonInterruptExitCodes` duplicates `theLeaveBackMapping`; the chat sites' non-interrupt codes are not covered.
15. LOW V formatting churn of the pre-commit rewrite is committed (`ConsoleBadge.java:27-31` column 0, `PmConsole.java:237`, 28-space continuations).

## Replies

PR 321: the eight threads (`EntryFlow` 63, 551, 568, 570, 578; `PmConsole` 65, 268, 318) each carry the lane's reply in the thread, a CodeRabbit confirmation and are resolved; all cited SHAs are real commits and the code supports them (the `:268` reply leans on the invented `project_id`, defect 1). The six nitpicks of comment `6648885809` got comment `6666409615`: item 3 (`RuntimeActDialogueTest`, "verified … cleanly terminate") is unsupported (no code change; the earlier answer declined it), item 6 claims tests "added" that existed. The ten findings F1-F4, N1-N6 of PR 332's review were answered in ONE list comment (`6666428986`), the spec asked for each: F1 fixed (single source incomplete), F2 fixed, F3 mostly, F4 NOT fixed (the reply says "derives the aspect list dynamically": `PROJECT_STATES` is a hand list and the test is hand-written; V*), N1 fixed, N2 fixed (evidence murky), N3 fixed, N4 fixed, N5 weak, N6 fixed (log `180040`). Thread `PR 332 EntryFlow.java:712` was resolved with a reply; CodeRabbit wrote that the pre-enrolment continuation "remains deferred, as agreed"; the decision is not in the design.

## PR #342

The PLAN-101 lane's follow-up (pre-commit rewrite of the same eight files): its body said "only whitespace" (false: three TODO markers); every line not already in `24523d67` is a TODO marker on a catch that #341 deleted. Stale and conflicting: CLOSED by the operator's word on 2026-11-05.

## Lane deviations

All builds in-process fallback; "Residue: none" and "Operator Questions: none asked" contradicted by the above. Third consecutive lane to report "done" for items GitHub or the repository does not show.

## Routing

PLAN-104 `cli-console-third-residue` (WS-04, 21040/21041; new, staged 2026-11-05): D1 the addressing by the contract (defects 1, 2-follow-up-1), D2 the five body items and the test-only surface (2, 6, 7, 9-12), D3 the step count in code, copy and design (8), D4 the German row, the clock mutation and the terminal evidence (3-5), D5 the generic catches in `pm-operator/src/main` and the formatting (13, 15; `EpicCommand` and `PlanCommand` go to PLAN-97 deliverable 5, which rewrites those files), D6 the replies and the landing facts. N5 (14) stays an open watch.
