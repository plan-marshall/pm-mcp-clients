# PLAN-186: The Standing Sonar Debt, Last Part: `pm-operator`

epic: ui-improvements
workstream: WS-04

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Staged on the operator's word of
> 2026-11-23 ("stage the sonar plan"), which carries out the zero-issue decision of 2026-11-20 ("Make a plan for fixing. We have
> a 0 issue policy") for the one module PLAN-175 held back: its deliverable 4 waited for PLAN-174 (shipped, #533) and was never
> staged afterwards. Parts 1 to 3 landed as #530 (PLAN-175), #543 (PLAN-180) and #544/#547 (PLAN-181).
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief. **It adds no dependency.**

## Objective

The default branch carries 113 open Sonar code smells (read 2026-11-23 at `232db343`): 110 in `pm-operator` and the three
issues of PLAN-181 that the operator selected for dismissal and dismisses by hand. This plan clears the 110 in `pm-operator`,
98 in production code and 12 in tests, without a behaviour change: no catalogue text, no screen output, no exit code and no
golden capture moves. After it lands the default branch reads zero, or every remaining issue is named with its key, rule and the
operator's selection.

## Deliverables

Read first: `PLAN-175-sonar-standing-debt.md` (the rule families and the shape of each fix), `landings/PLAN-175.md`,
`landings/PLAN-180.md` and `landings/PLAN-181.md` (what the earlier parts did, and the transmission defect of dismissals). The
open list is read by the lane at the start of each deliverable through the repository's provider, never from a number in this
spec: `python3 .plan/execute-script.py plan-marshall:workflow-integration-sonar:sonar read_open_issues --project
plan-marshall_pm-mcp-clients` (the default branch) and `--pr N` for the pull request. The read of 2026-11-23 by rule:
`java:S1192` 36, `java:S3776` 19, `java:S4449` 6, `java:S3358` 5, `java:S1130` 5, `java:S6206` 4, `java:S135` 4, `java:S9358` 3,
`java:S7467` 3, and 1 or 2 each of `S1172`, `S107`, `S1905`, `S127`, `S2925`, `S6213`, `S1488`, `S106`, `S3398`, `S1125`,
`S2589`, `S6885`, `S5778`, `S1481`, `S1075`, `S125`, `S1068`, `S1066`, `S9412`. The files with the most:
`OverviewFullScreenApp.java` 14, `StatusCommand.java` 13, `WorkActionDialogue.java` 9.

1. **`pm-operator` production code**
 - What: every open issue under `pm-operator/src/main` at the read (33 files, 98 issues on 2026-11-23): duplicated literals become
   constants (`java:S1192`); cognitive complexity is reduced by extracting methods without changing the order of effects or of
   output lines (`java:S3776`); nullable parameters are annotated (`java:S4449`); nested ternaries are unfolded (`java:S3358`);
   exceptions that are not thrown leave the `throws` clause (`java:S1130`); and the rest the read names. A `System.out` /
   `System.err` use where the CLI writes its own output is allowed by `CLAUDE.md`; such a finding (`java:S106`) is put to the
   operator for dismissal rather than rewritten. No text of `operator.*.properties` or `messages/*.properties`, no picocli option
   or verb name, no exit code and no rendered line changes.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:java-null-safety, pm-dev-java-cui:cui-logging, plan-marshall:ref-code-quality
 - Done when:
   - `read_open_issues` names no open issue under `pm-operator/src/main` on the pull request, and on the default branch after
     Sonar's analysis of the merge commit (`analysis_revision` equal to it; `unknown` while the analysis has not run, never `0`),
     or every remaining one is named with its key and the operator's selection. Evidence order: `none`.
   - `./mvnw -pl pm-operator -am verify` passes with no fewer tests than before, and `git diff` names no file under
     `src/main/resources` and no file under `src/test/resources/screen-captures`. Evidence order: `none`.
2. **`pm-operator` tests**
 - What: every open issue under `pm-operator/src/test` at the read (9 files, 12 issues on 2026-11-23), among them the
   `Thread.sleep(100)` of `PmOperatorProcessIT.java:222` (`java:S2925`), which waits on the condition through Awaitility (already a
   test-scoped dependency of `pm-operator`, `pom.xml:118-121`), and the issue at `PmOperatorProcessIT.java:122` (`java:S9412`). No
   assertion is loosened (a literal turned into `contains` or `isNotNull`, a removed assertion), no test is removed, and input data
   stays generated (`Generators`, `@EnableGeneratorController`): read in the diff by the lane and stated in the landing message.
 - Persona: plan-marshall:persona-module-tester
 - Skills: pm-dev-java:junit-core, pm-dev-java:junit-integration, pm-dev-java-cui:cui-testing
 - Done when:
   - `read_open_issues` names no open issue under `pm-operator/src/test` on the pull request and on the default branch after the
     analysis of the merge commit, or every remaining one is named with its key and the operator's selection. Evidence order:
     `none`.
   - `./mvnw -pl pm-operator -am verify` and `./mvnw verify -Pintegration-tests -pl pm-operator -am` pass, and
     `git grep -n "Thread.sleep" -- pm-operator/src/test` is empty. Evidence order: `none`.
3. **The zero read of the repository**
 - What: after the pull request is merged and Sonar has analysed the merge commit, read the default branch for the whole project
   and report the count by module and by rule in the landing message. The three PLAN-181 issues `AaFGfsYupdXNQ-N4ArY-`
   (`java:S1313`), `AaGRMDtGxhXuOwwhaS0K` (`java:S4449`) and `AaEcTdY0MJNX7b6L9QAy` (`java:S112`) are the operator's to dismiss
   by hand; the read says whether they are still open.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core
 - Done when:
   - `read_open_issues` of the default branch names no open issue, or every remaining issue is named with its key, its rule and
     the operator's selection; the read is `unknown` while the analysis of the merge commit has not run, never `0`. Evidence
     order: `none`.

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`): the module builds named above, then the full `./mvnw verify` and
  `./mvnw verify -Ppre-commit` (review every rewrite; it is known not to be idempotent and may touch files outside this plan:
  revert those) at the head; `-Pcoverage` at the head (minimum 80 % per module).
- **No new dependency or plugin.** Awaitility is already a test dependency of `pm-operator`. Code standards of `CLAUDE.md`:
  Java 25, no generic `Exception` / `RuntimeException` caught or thrown in production code, `CuiLogger` through `LogMessages`,
  JUnit 5 only (no Mockito, PowerMock or Hamcrest), input test data from `Generators` with `@EnableGeneratorController`.
  Pre-1.0: no shim.
- No behaviour change: no catalogue text, verb or option name, exit code, rendered line or golden capture changes; a fix that
  would change behaviour is reported, not made. No `NOSONAR`, no `@SuppressWarnings`, no Sonar exclusion.
- One pull request for deliverables 1 and 2, under 100 files and 20k lines (CodeRabbit gives no review above that); split into
  production and tests only if that limit would be passed. Rebase on `main` before every push.
- A Sonar issue is fixed in a pushed commit; a dismissal is made only on the operator's selection and recorded as
  `operator-selected: {issue-key}: {reason}` in the progress record. The transmission through
  `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}` is
  known to answer HTTP 400 (PLAN-181); when it does, the lane names the key, rule and reason in the pull request body and the
  landing message so the operator can dismiss it by hand, and does not count it as fixed. The lane never records a dismissal of
  its own.
- ZERO Sonar findings on the new code of the pull request (the operator, 2026-11-05); state `sonar_open_new_issues=N` in the
  landing message (`unknown` for a failed read, never `0`).
- No e2e run is needed (`pm-web-app` and `pm-web-e2e` are not touched). Other plans run at the same time: never stop or reuse a
  process this worktree did not start.
- The merge rule: the run merges only when every review thread has a reply and is resolved and every review body is answered,
  and some head has had one CodeRabbit review. The pull request body states thread count, resolved count and review-body items
  from `ci pr comments`, never from memory. The run merges the pull request itself once these gates hold (`CLAUDE.md`, Git
  Workflow). Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`; the pull request body carries no generator
  footer. If CodeRabbit refuses on quota, stop and ask the operator.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch. The branch of the
  parked PLAN-42 (`feature/42-cli-verb-modes-and-help`) is never touched, rebased or merged by this run.

## Claim Labels

- OBSERVED: the default branch carries 113 open issues, 110 under `pm-operator` (98 under `src/main` in 33 files, 12 under `src/test` in 9 files) and three outside it (`pm-mock-runtime` 2, `pm-exec` 1, the PLAN-181 dismissals) — read on 2026-11-23 by `sonar read_open_issues --project plan-marshall_pm-mcp-clients` (`count_status: confirmed`, `open_issue_count: 113`)
- OBSERVED: all 42 files with an open issue exist at `origin/main` `232db343` — read by `git cat-file -e origin/main:{path}` for each
- OBSERVED: `PmOperatorProcessIT.java:222` calls `Thread.sleep(100)` and `pm-operator/pom.xml:118-121` declares `org.awaitility:awaitility` at test scope — read at `232db343`
- OBSERVED: the parked PLAN-42 branch `feature/42-cli-verb-modes-and-help` (4 commits, unmerged) changes three of the 42 files: `ConfigCommand.java`, `PlanCommand.java`, `overview/OverviewFullScreenApp.java` — read by `git diff --name-only $(git merge-base origin/main feature/42-cli-verb-modes-and-help) feature/42-cli-verb-modes-and-help`
- OBSERVED: the running PLAN-176 (PR #549) changes one of the 42 files, `PlanCommand.java` — read by `git diff --name-only origin/main...fix/176-web-wire-values-and-onboarding-residue`
- HYPOTHESIS: every `java:S106` finding sits where the CLI writes its own output, which `CLAUDE.md` allows — confirm/refute at the finding's line at outline (verify-at-outline)
- Verify-first clause: before each deliverable re-read the named files and the Sonar list at the then-current HEAD; PLAN-176 (`PlanCommand.java`) and, when resumed, PLAN-42 (`ConfigCommand`, `PlanCommand`, `OverviewFullScreenApp`) edit the same files, and the CLI chain (PLAN-44, 48, 50, 63, 184) follows. A refuted claim re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ActionSupport.java` — present at HEAD 232db343
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/AnswerCommand.java` — present at HEAD 232db343
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ClientCommand.java` — present at HEAD 232db343
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ConfigCommand.java` — present at HEAD 232db343
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ConsoleNoticeBuffer.java` — present at HEAD 232db343
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/DecomposeCommand.java` — present at HEAD 232db343
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/DirectFileAccess.java` — present at HEAD 232db343
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/EntryFlow.java` — present at HEAD 232db343
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/EpicCommand.java` — present at HEAD 232db343
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/HoldDialogue.java` — present at HEAD 232db343
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/LessonSourceCommand.java` — present at HEAD 232db343
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/OperatorStateDirectory.java` — present at HEAD 232db343
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/PlanCommand.java` — present at HEAD 232db343
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/PlanSettingsDialogue.java` — present at HEAD 232db343
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/PmConsole.java` — present at HEAD 232db343
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/QrCodeRenderer.java` — present at HEAD 232db343
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ReviewDialogue.java` — present at HEAD 232db343
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/RuntimeCommand.java` — present at HEAD 232db343
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/StatusCommand.java` — present at HEAD 232db343
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/WorkActionDialogue.java` — present at HEAD 232db343
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/WorkChatDialogue.java` — present at HEAD 232db343
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/actions/ActionMenus.java` — present at HEAD 232db343
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/actions/ActionsMenu.java` — present at HEAD 232db343
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/actions/WorkActionsClient.java` — present at HEAD 232db343
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/epicview/EpicViewFormatting.java` — present at HEAD 232db343
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/epicview/EpicViewLevel.java` — present at HEAD 232db343
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/overview/OverviewFullScreenApp.java` — present at HEAD 232db343
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/reports/ReportsLevel.java` — present at HEAD 232db343
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/system/SystemLevel.java` — present at HEAD 232db343
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/tui/TuiLayout.java` — present at HEAD 232db343
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/tui/TuiMenu.java` — present at HEAD 232db343
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/tui/TuiScreenDriver.java` — present at HEAD 232db343
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/tui/TuiViewStack.java` — present at HEAD 232db343
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ActionsFullScreenTest.java` — present at HEAD 232db343
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/CredentialsCommandTest.java` — present at HEAD 232db343
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/PmOperatorProcessIT.java` — present at HEAD 232db343
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ReportsFullScreenTest.java` — present at HEAD 232db343
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/SystemFullScreenTest.java` — present at HEAD 232db343
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/epicview/EpicViewLevelTest.java` — present at HEAD 232db343
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/MockMachine.java` — present at HEAD 232db343
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/PseudoTerminal.java` — present at HEAD 232db343
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/tui/TuiScreenDriverTest.java` — present at HEAD 232db343

## Dependencies and Sequencing

- Depends on: `PLAN-174` (shipped, #533), `PLAN-175` part 1 (#530), `PLAN-180` (#543), `PLAN-181` (#544, #547).
- Overlaps with: `PLAN-176` (running, PR #549: `PlanCommand.java`, one file, which does not block); `PLAN-42` (parked, branch
  unmerged: `ConfigCommand.java`, `PlanCommand.java`, `OverviewFullScreenApp.java`; this plan does not wait for it, and whichever
  of the two merges second rebases); the CLI chain `PLAN-44`, `48`, `50`, `63`, `184` and `PLAN-138` to `140` (staged: they edit
  `pm-operator` and are re-grounded after this plan lands).
- Adjacent to: `pm-mock-runtime` and `pm-exec`, whose three remaining issues are the operator's dismissals (not edited here).

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-186-sonar-standing-debt-pm-operator.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger write. The session reports its outcome through its PR and its inbox message (`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary).
