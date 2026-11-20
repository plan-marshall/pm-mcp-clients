# PLAN-175: The Standing Sonar Debt: 493 Open Issues Down to Zero

epic: ui-improvements
workstream: WS-04

> Staged plan spec, one shippable unit of work, ready for hand-off on the `plan-marshall` lane (large: six deliverables, 173 files, cut into pull requests by module). Staged on the operator's word of 2026-11-20 ("yes. Make a plan for fixing. We have a 0 issue policy") from the provider read of the project on 2026-11-20 (`sonar read_open_issues --project plan-marshall_pm-mcp-clients`, default branch, `count_status: confirmed`; recorded in `epic.md` Watches). This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

The zero-findings policy (the operator, 2026-11-05) covers the new code of every pull request, and the quality gate has been green for every analysed pull request; the project nevertheless carries **493 open issues** on its default branch, all code smells (no bug, no vulnerability, no hotspot), **224 critical, 140 major, 120 minor, 8 info and one blocker** (`java:S1314`, `pm-api/src/main/java/de/planmarshall/api/posix/SpawnPlan.java:107`), older than the recent work and owned by no plan. This plan brings the count to zero without suppressing a single one: each issue is fixed in code, or, where it is a false positive, put to the operator with its evidence (a dismissal is made only on the operator's selection). Behaviour does not change.

## Deliverables

Read first:
- The open list itself, read by the lane at the start of each deliverable through the repository's provider (never through the SonarQube MCP, which cannot see this project): `python3 .plan/execute-script.py plan-marshall:workflow-integration-sonar:sonar read_open_issues --project plan-marshall_pm-mcp-clients` names each issue's key, severity, rule, file and line; **the list moves while other plans land, so the lane works from its own read, not from the counts below.**
- The counts below are the read of 2026-11-20 (493 issues in 173 files): `pm-api` 182 (98 files; main 141, test 41), `pm-mock-runtime` 165 (15 files; `MockRuntimeEngine.java` alone 106, `FullyOnboardedScenarioTest.java` 34), `pm-operator` 117 (44 files; `OverviewFullScreenApp.java` 14, `StatusCommand.java` 13, `PlanCommand.java` 10, `WorkActionDialogue.java` 9), `pm-web-app` 17 (9 files, `client.ts` 8), `pm-relay` 9, `pm-exec` 3. By rule: `java:S1192` (duplicated literal) 181, `java:S3776` (cognitive complexity) 39, `java:S1612` 36, `java:S4449` 23, `java:S7467` 22, `java:S1130` 18, `java:S2589` 17, `java:S3415` 14, `java:S3358` 12, `java:S5778` 11, `java:S6213` 10, then smaller families (`java:S135`, `S1135`, `S2925`, `typescript:S6571` 7, `S5961`, `S6878`, `S6206`, `S1168`, `S2386`, `typescript:S3776` 3 and about twenty single or double rules).
- The pull requests are cut by deliverable (the split guard: each stays under 100 files; CodeRabbit refuses above that). The progress record says so.

1. **The blocker and the `pm-api` production code**
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-null-safety, pm-dev-java:javadoc, pm-dev-java-cui:cui-logging
   - What: first the one BLOCKER (`java:S1314` at `SpawnPlan.java:107`), then every open issue in `pm-api/src/main` (141 issues, 83 files at the read): duplicated literals become constants (`java:S1192`, 79 in `pm-api`), cognitive complexity is reduced by extracting methods without changing the order of effects (`java:S3776`), unused exception variables and thrown declarations go (`java:S7467`, `java:S1130`), a record is used where the rule asks and the class is a value (`java:S6878`, `java:S6206`), a method returns an empty collection and not `null` (`java:S1168`), and so on for the remaining rules; the fix for each issue is the one the provider's message names unless it changes behaviour or a public contract (a wire name, a message key, a JSON field): then the issue is not fixed by that route and the lane says which and why.
   - Done when:
     - `read_open_issues` of the default branch, read after this pull request is merged and Sonar's analysis of the merge commit has run (`analysis_revision` equals the merge commit), names no open issue under `pm-api/src/main` (evidence order: `none`; `unknown` while the analysis has not run, never `0`).
     - `./mvnw -pl pm-api verify` passes and the number of tests is not lower than before (evidence order: `none`); no test assertion is edited by this deliverable (evidence order: `none`, read in the diff).
     - The pull request's own Sonar read (`read_open_issues --pr N`) names no open new-code issue and no hotspot (evidence order: `none`).
2. **The `pm-api` tests**
   - Persona: plan-marshall:persona-module-tester
   - Skills: pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
   - What: every open issue in `pm-api/src/test` (41 issues, 15 files at the read; `ProcessTreeTest.java` 12): argument order of assertions (`java:S3415`), a lambda of `assertThrows` that holds one throwing call (`java:S5778`), too many assertions in one test split into tests (`java:S5961`), duplicated literals as constants; the expected value of a contract stays literal; no assertion is loosened.
   - Done when:
     - After the merge and the analysis of the merge commit, `read_open_issues` names no open issue under `pm-api/src/test` (evidence order: `none`).
     - `./mvnw -pl pm-api verify` passes, the number of tests is not lower than before, and no assertion is loosened (a literal turned into `contains`/`isNotNull`, a removed assertion): read in the diff by the lane and stated in the landing message (evidence order: `none`).
3. **`pm-mock-runtime`**
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
   - What: every open issue in `pm-mock-runtime` (165 issues, 15 files at the read: `MockRuntimeEngine.java` 106, `FullyOnboardedScenarioTest.java` 34, `ScenarioLoader.java` 5, `StartRegistry.java` 3, `MockRuntime.java` 3, `DesignerRegistry.java` 2 and the rest): duplicated literals (`java:S1192`, 65), lambdas as method references (`java:S1612`, 36), nullability (`java:S4449`, 17), always-true or always-false conditions (`java:S2589`, 14: each is read before it is removed, because a condition the analysis calls constant may guard a branch a scenario reaches), cognitive complexity (`java:S3776`, 10), `TODO` tags (`java:S1135`: done or turned into a named follow-up, not deleted). `MockRuntimeEngine.java` is the file many plans edit: the lane merges `origin/main` immediately before it starts on it and keeps both sides.
   - Done when:
     - After the merge and the analysis of the merge commit, `read_open_issues` names no open issue under `pm-mock-runtime` (evidence order: `none`).
     - `./mvnw -pl pm-mock-runtime verify` passes, the number of tests is not lower than before, the scenario JSON files and every wire name are unchanged (`git diff` names no file under `src/main/resources`), and no assertion is loosened (evidence order: `none`).
4. **`pm-operator`**
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
   - What: every open issue in `pm-operator` (117 issues, 44 files at the read: `OverviewFullScreenApp.java` 14, `StatusCommand.java` 13, `PlanCommand.java` 10, `WorkActionDialogue.java` 9, `EpicCommand.java` 5, `WorkChatDialogue.java` 5; main 103, test 14): duplicated literals (`java:S1192`, 37), cognitive complexity (`java:S3776`, 20), nested ternaries (`java:S3358`, 8), thrown declarations (`java:S1130`, 7), nullability (`java:S4449`, 6), and the rest. **The printed text, the exit codes and the screen captures do not change** (the captures are compared byte for byte). **This deliverable starts after the running `PLAN-174` has landed** (it edits `PlanCommand`, `ConfigCommand`, `EpicCommand`, `ReportsCommand`, `ProjectsCommand` and the operator tests); the lane re-reads the open list at that point, because `PLAN-174` removes some of these issues and adds none.
   - Done when:
     - After the merge and the analysis of the merge commit, `read_open_issues` names no open issue under `pm-operator` (evidence order: `none`).
     - `./mvnw -pl pm-operator verify` and `-Pintegration-tests` (separate invocations) pass, the screen-capture set is byte-identical before and after, the number of tests is not lower than before (evidence order: `none`).
5. **`pm-web-app`, `pm-relay` and `pm-exec`**
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
   - What: every open issue in `pm-web-app` (17 issues, 9 files: `client.ts` 8, `scenarioMockFetch.ts` 2; `typescript:S6571` 7, `typescript:S3776` 3), `pm-relay` (9 issues, 5 files) and `pm-exec` (3 issues: `NativeKernel.java` 2, `PmExecIT.java` 1). **`pm-web-app`'s branch-coverage margin is 0.14 pt: a refactor adds no uncovered branch** (the lane runs `-Pcoverage` and records the percentage); the running `PLAN-173` edits `forms/index.ts` and the field components: the lane re-reads the open list when it starts.
   - Done when:
     - After the merge and the analysis of the merge commit, `read_open_issues` names no open issue under `pm-web-app`, `pm-relay` or `pm-exec` (evidence order: `none`).
     - `./mvnw verify -pl pm-web-app -am`, `-pl pm-relay`, `-pl pm-exec` and the `-Pe2e` suite for the web module pass, `-Pcoverage` keeps the 80 % floor and its branch percentage is recorded, the number of tests is not lower than before (evidence order: `none`).
6. **Zero is read, and stays the policy**
   - Persona: plan-marshall:persona-module-tester
   - Skills: pm-dev-java:junit-core
   - What: after the last merge, the project's open issue count is read on the default branch and is zero in every module; the landing message states the count per module before and after, by the provider read (never by the SonarQube MCP). An issue the lane could not fix without a behaviour or contract change, or that is a false positive, is not suppressed (`@SuppressWarnings`, `// NOSONAR`, an exclusion in `.github/project.yml` or the POM): it is listed with its key, rule, file, line and the evidence and **put to the operator, who selects the dismissal** (`sonar_rest transition`, `wontfix` or `falsepositive`, recorded as `{issue-key}: {reason}`); the plan then ends with those issues named, not hidden.
   - Done when:
     - `read_open_issues` of the default branch after the last merge reports `open_issue_count: 0` with `count_status: confirmed`, or names exactly the issues the operator has been asked about and not yet dismissed (evidence order: `none`).
     - `git grep -n -E "NOSONAR|@SuppressWarnings" -- pm-api pm-mock-runtime pm-operator pm-relay pm-exec pm-web-app` names no line this plan added (evidence order: `none`).

## Standing Constraints

- **Behaviour does not change.** No printed text, exit code, wire name, message key, JSON field, public API or scenario file changes; a fix that would change one is not made and the issue goes to deliverable 6. A refactor of cognitive complexity (`java:S3776`) preserves the order of side effects and is covered by the module's existing tests; where no test covers the code, a test is added first (`test-first` against the unrefactored code: it passes before and after).
- **No suppression.** No `@SuppressWarnings`, no `// NOSONAR`, no exclusion, no change to the quality profile, no lowering of a threshold; a dismissal is made only on the operator's selection (`CLAUDE.md`, the zero-findings rule).
- Build and test through Maven only (`CLAUDE.md`): the module's `verify`, `-Pintegration-tests` for `pm-operator` and `pm-exec`, `-Pe2e` for `pm-web-app`, `-Pcoverage` where named (separate invocations); the quality gate `./mvnw verify -Ppre-commit` rewrites files: every rewrite is reviewed, and a file this plan does not own is restored from `origin/main`.
- No new dependency or plugin. Java rules of `CLAUDE.md`: records where the rule asks and the class is a value, `var` for obvious types, `@NullMarked`, `CuiLogger` and `LogMessages` (a `System.out` or `System.err` outside a CLI's own output is itself a finding of `java:S106`).
- **Sonar is read through the repository's provider, never through the SonarQube MCP**: `sonar read_open_issues --project plan-marshall_pm-mcp-clients [--pr N]` for the open count, `sonar_rest gate-status` for the quality gate; `unknown` for an undecidable read, never `0`. Each pull request merges with zero new-code issues and zero hotspots (a merge gate).
- The order of each Done-when is the order the evidence has: a `none` Done-when is never described as failing.
- **The merge rule:**
  - At least one CodeRabbit review of the head that is merged, with its findings handled. If CodeRabbit refuses on quota, pauses or refuses above 100 files, stop and ask the operator.
  - Every review thread replied and resolved, and every review body item answered ON THE PULL REQUEST, one reply per finding, read from GitHub's thread state; a finding CodeRabbit re-verified and refused to mark addressed is fixed or answered with a reason after its rebuttal.
  - The run merges the pull request itself once these gates hold (`CLAUDE.md`, Git Workflow). Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`. The pull request body carries no generator footer.
- The landing verdict is given per Done-when clause, never per deliverable; `deliverables_done` is the number of deliverables whose every clause is met; a number the lane did not recount is `unknown`.
- Work only in the session's own worktree; files are read and edited with the file tools, one command per call.
- Pull request size: one per deliverable, each under 100 files; if a deliverable's changed set passes 100 files, split it by directory and say so.
- The mailbox read refuses a plan id with a leading digit (`invalid_target_plan`): report it, never read it as no mail.

## Claim Labels

- OBSERVED: the default branch carries 493 open issues, all `CODE_SMELL`, 224 critical, 140 major, 120 minor, 8 info, 1 blocker, 0 hotspots; by module `pm-api` 182, `pm-mock-runtime` 165, `pm-operator` 117, `pm-web-app` 17, `pm-relay` 9, `pm-exec` 3; 100 in test code — read by `sonar read_open_issues --project plan-marshall_pm-mcp-clients` (`count_status: confirmed`) and `sonar_rest hotspots` on 2026-11-20 at `origin/main` `05e6c683`
- OBSERVED: the blocker is `java:S1314` at `pm-api/src/main/java/de/planmarshall/api/posix/SpawnPlan.java:107` — read in the same list
- OBSERVED: the quality gate is `OK` for every analysed pull request of the recent work and the files those pull requests changed carry no open issue — read by `sonar_rest gate-status --pr N` and a search of the open list (`landings/` and the Watch entry of 2026-11-20)
- HYPOTHESIS: the issues are older than the recent work (no creation date was in the read) — confirm/refute with the issue's creation date from the provider at outline; it does not change the plan
- HYPOTHESIS: every open issue can be fixed without a behaviour or contract change — a share of `java:S2589` (a condition the analysis calls constant), `java:S4449` (nullability of a public record or API) and `java:S1135` (`TODO` tags) may not; those go to deliverable 6 with their evidence
- HYPOTHESIS: Sonar's analysis of the default branch runs on the merge commit in the CI build (`.github/project.yml`: "its Automatic Analysis is off, the CI build analyses") so `analysis_revision` equals the merge commit shortly after the merge — confirm at the first merge; if not, the read is `unknown` and the orchestrator completes it at the landing
- Verify-first clause: before each deliverable, re-read the open list for its module at the then-current HEAD; plans in flight edit the same files (see Dependencies). A refuted claim re-scopes the deliverable and is reported.

## Expected Surface

The files that carry the issues at the read of 2026-11-20 (173 files); the list moves with the list of issues.

- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/PmVersion.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/control/ActionAvailability.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/control/ArchiveCheckLine.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/control/ChatProposalResource.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/control/ControlJson.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/control/DecompositionCheck.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/control/DecompositionResource.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/control/EpicResource.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/control/EpicTokens.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/control/HoldRecord.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/control/HoldResolutionRequest.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/control/HoldResolutionResult.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/control/PlanActionRequest.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/control/PlanSettingsUpdateRequest.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/control/PlanSettingsUpdateResult.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/control/PlanSettingsValue.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/control/PlanTokenFigure.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/control/WorkActions.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/decision/DecisionAnsweredEvent.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/decision/DecisionClosedEvent.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/decision/DecisionEnvelope.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/decision/DecisionOption.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/decision/DecisionPart.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/decision/QuorumSupport.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/decision/QuorumVote.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/decomposition/DecompositionCheckFinding.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/decomposition/DecompositionInstructRequest.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/decomposition/DecompositionValidator.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/decomposition/EpicDecomposition.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/decomposition/StagedPlanRecord.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/entry/OnboardingProgress.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/entry/RuntimeEntryResource.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/files/package-info.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/host/HostDetection.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/host/ProfileDocumentReader.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/http/BodyInputStreams.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/http/HttpResponse.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/http/SseReader.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/http/UnixSocketHttpClient.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/id/DottedIdentifier.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/interaction/OperatorStateResource.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/json/JsonTree.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/message/MessageCatalogue.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/onboarding/AutoResultItem.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/overview/ActiveEpicSummary.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/overview/ActivePlanSummary.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/overview/ActivityChangedEvent.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/overview/AlongsideLanding.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/overview/ProjectDetailResponse.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/posix/Libc.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/posix/PosixPlatform.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/posix/PosixSpawn.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/posix/SpawnPlan.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/process/ActivityEntry.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/process/ActivityEvent.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/process/EpicProcess.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/process/EpicProcessPlan.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/process/LeafJob.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/process/OffTrackInfo.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/process/ProcessNode.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/process/ProcessTokens.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/process/ProcessTree.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/process/TokensChangedEvent.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/runtime/OnDemandStart.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/runtime/PosixDaemonLauncher.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/runtime/RuntimeClient.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/runtime/RuntimeRecord.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/runtime/RuntimeTokenFile.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/source/lesson/LessonClaimResult.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/source/lesson/LessonPremiseCheckResult.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/source/lesson/UnclaimedLessonSummary.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/system/CleanupDecidedEvent.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/system/CleanupDurations.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/system/CleanupItemEntry.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/system/CleanupJob.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/system/CleanupKeptItem.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/system/CleanupProjectItem.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/system/CleanupResidue.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/system/RuntimeMetrics.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/system/RuntimeStatus.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/system/SystemSnapshot.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/system/WorkMetrics.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/system/WorkerMetric.java`
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/decision/DecisionRecordsTest.java`
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/event/EventEnvelopeTest.java`
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/host/ProfileDocumentReaderTest.java`
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/http/UnixSocketHttpClientTest.java`
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/interaction/OperatorStateResourceTest.java`
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/json/JsonTreeTest.java`
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/onboarding/OnboardingRecordsTest.java`
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/overview/OverviewRecordsTest.java`
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/process/ProcessTreeTest.java`
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/runtime/DetachedStartTest.java`
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/runtime/OnDemandStartTest.java`
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/runtime/RuntimeRecordTest.java`
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/testsupport/CliProcess.java`
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/testsupport/FakeRuntimeServer.java`
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/testsupport/StagedDaemon.java`
- OBSERVED: `pm-exec/src/main/java/de/planmarshall/exec/NativeKernel.java`
- OBSERVED: `pm-exec/src/test/java/de/planmarshall/exec/PmExecIT.java`
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/MockRuntime.java`
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/StandardScenarios.java`
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/http/MockEventStream.java`
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/http/MockHttpResponse.java`
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/http/MockHttpServer.java`
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/DesignerRegistry.java`
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java`
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/StartRegistry.java`
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/scenario/ScenarioLoader.java`
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/scenario/ScenarioRegistry.java`
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/ControlScenarioTest.java`
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/DecisionsScenarioTest.java`
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/FullyOnboardedScenarioTest.java`
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/OnboardingScenarioTest.java`
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/RunningWorkScenarioTest.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ActionSupport.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/AnswerCommand.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/CleanupCommand.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ClientCommand.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ConfigCommand.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ConsoleNoticeBuffer.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/DecomposeCommand.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/DirectFileAccess.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/EntryFlow.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/EpicCommand.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/HoldDialogue.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/LessonSourceCommand.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/OperatorStateDirectory.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/PlanCommand.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/PlanSettingsDialogue.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/PmConsole.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/QrCodeRenderer.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ReviewDialogue.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/RuntimeCommand.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/StatusCommand.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/WorkActionDialogue.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/WorkChatDialogue.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/actions/ActionMenus.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/actions/ActionsMenu.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/actions/WorkActionsClient.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/epicview/EpicViewFormatting.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/epicview/EpicViewLevel.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/overview/OverviewFullScreenApp.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/reports/ReportsLevel.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/system/SystemLevel.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/tui/TuiLayout.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/tui/TuiMenu.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/tui/TuiScreenDriver.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/tui/TuiViewStack.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ActionsFullScreenTest.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ConfigCommandTest.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/CredentialsCommandTest.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/PmOperatorProcessIT.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ReportsFullScreenTest.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/SystemFullScreenTest.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/epicview/EpicViewLevelTest.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/MockMachine.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/PseudoTerminal.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/tui/TuiScreenDriverTest.java`
- OBSERVED: `pm-relay/src/main/java/de/planmarshall/relay/PmMcp.java`
- OBSERVED: `pm-relay/src/main/java/de/planmarshall/relay/ProtocolData.java`
- OBSERVED: `pm-relay/src/main/java/de/planmarshall/relay/Relay.java`
- OBSERVED: `pm-relay/src/test/java/de/planmarshall/relay/PmMcpProcessIT.java`
- OBSERVED: `pm-relay/src/test/java/de/planmarshall/relay/RelayTest.java`
- OBSERVED: `pm-web-app/src/lib/api/client.ts`
- OBSERVED: `pm-web-app/src/lib/api/scenarioMockFetch.ts`
- OBSERVED: `pm-web-app/src/lib/i18n/index.ts`
- OBSERVED: `pm-web-app/src/lib/icons/workflowIcons.ts`
- OBSERVED: `pm-web-app/src/lib/routes.ts`
- OBSERVED: `pm-web-app/src/lib/stores/runtime.ts`
- OBSERVED: `pm-web-app/src/lib/views/CleanupView.test.ts`
- OBSERVED: `pm-web-app/src/lib/views/PairingView.scenarios.test.ts`
- OBSERVED: `pm-web-app/src/lib/views/PlanView.test.ts`

## Dependencies and Sequencing

- Depends on: none. **Deliverable 4 starts after `PLAN-174` has landed** (it edits the same operator command classes); deliverable 5 re-reads the web files after `PLAN-173` has landed.
- Overlaps with (read by `corpus cross-check` at staging, 2026-11-20; shipped, parked and superseded plans are not in the disjointness population): **the running `PLAN-174` shares 7 files with it (all `pm-operator`, so deliverable 4, which therefore starts after PLAN-174 has landed; deliverables 1, 2, 3 and 5 share no file with a running plan)**; the running `PLAN-173` shares none. Staged: `PLAN-44` (22 files), `PLAN-42` (8), `PLAN-140` (7), `PLAN-138` (6), `PLAN-144` (4), `PLAN-48` (4), `PLAN-50` (4), `PLAN-135` (3), `PLAN-155` (2), `PLAN-157` (2) and single files of `PLAN-136`, `PLAN-139`, `PLAN-153`, `PLAN-59`, `PLAN-62`, `PLAN-85`: whichever lands second rebases and keeps both sides.
- Adjacent to: every staged plan that edits these files (the CLI chain `PLAN-42`, `PLAN-44`, `PLAN-48`, `PLAN-138` to `PLAN-140`, the web plans): whichever lands second rebases and keeps both sides; a plan that lands first removes or moves issues, which the next read shows.

## Hand-Off Command

Lane: `plan-marshall` (large: six deliverables, 173 files across several pull requests; the operator's size rule of 2026-11-20).

```text
/plan-marshall task="implement .plan/orchestrator/ui-improvements/plans/PLAN-175-sonar-standing-debt.md"
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports its outcome through its PRs and its inbox message. The inbox exception's qualifiers and the sole sanctioned write mechanism are stated in `persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
