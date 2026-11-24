# PLAN-189: The Sonar Residue of PLAN-186 in `pm-operator`

epic: ui-improvements
workstream: WS-04

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Staged on the operator's word of
> 2026-11-24 ("emit the corresponding plan as fifth run"), the fix plan for the Open Defect "PLAN-186 landed": the default branch
> read 9 open Sonar issues after the analysis of `81e2c1fc`, six of them in `pm-operator`, which PLAN-186 neither fixed nor named
> (`landings/PLAN-186.md`). Emitted as a fifth concurrent run above N=4 on that word.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief. **It adds no dependency.**

## Objective

The zero-issue policy (the operator, 2026-11-20) is not met on the default branch: after PLAN-186 (#556) Sonar reads 9 open
issues, six under `pm-operator/src/main`. This plan clears those six without a behaviour change — five by a code fix, the URI
literal by the operator's selection — fixes the one misstated nullness contract PLAN-186 introduced, and reads the default
branch after the merge, so that it reads exactly the three PLAN-181 keys the operator dismisses by hand, or names every other
remaining key with the operator's selection.

## Deliverables

Read first: `landings/PLAN-186.md` (what PLAN-186 did and what is left) and `PLAN-186-sonar-standing-debt-pm-operator.md` (the
rule families and the shape of each fix). The open list is read by the lane at the start through the repository's provider,
never from this spec: `python3 .plan/execute-script.py plan-marshall:workflow-integration-sonar:sonar read_open_issues --project
plan-marshall_pm-mcp-clients` (default branch) and `--pr N` for the pull request. A pull-request read shows new-code issues
only; it **cannot** show an issue on a line the diff does not rewrite, which is exactly how PLAN-186's residue was missed — so
the default-branch read after the merge's analysis is the deciding read, and a forecast is never reported as a result.

1. **The six open `pm-operator` issues and the misstated nullness**
 - What: the issues the default branch read on 2026-11-24 (after the analysis of `81e2c1fc`), re-read at the start:

   | Key | Rule | Where (at `62155055`) | Expected fix |
   |---|---|---|---|
   | `AaFfVbmag_p_FqfrTT28` | `java:S3776` | `HoldDialogue.java:102` `execute(@Nullable String holdId)` | reduce cognitive complexity below the threshold by extraction, keeping the order of effects and output lines |
   | `AaFfVblVg_p_FqfrTT2o` | `java:S3776` | `WorkChatDialogue.java:125` `execute()` | same |
   | `AaFfVbnQg_p_FqfrTT3J` | `java:S1192` | `WorkActionDialogue.java:180` `"findings"` (also `:500`) | a constant per meaning: the action word and the `ReviewDialogue` kind are one meaning only if the code treats them as one; the message parameter name at `:500` is a different role and is not merged into the same constant without that reason |
   | `AaFfVbnQg_p_FqfrTT3K` | `java:S1192` | `WorkActionDialogue.java:181` `"watches"` (also `:500`) | same |
   | `AaFfVblug_p_FqfrTT20` | `java:S3398` | `overview/OverviewFullScreenApp.java:1374` `openWhatWaits()` | move the private method to the inner class that alone calls it, or report why not |
   | `AaFMIQUlEHNTGUcqN-Y-` | `java:S1075` | `LessonSourceCommand.java:45` `PATH_PROJECTS_PREFIX` | a URI literal that is already a named constant: put to the operator (question tool) with main's precedent named — `AcceptEditCommand.java:66`, `ConfigCommand.java:68`, `CredentialsCommand.java:55` carry a class-level `@SuppressWarnings("java:S1075")` — options: the same class-level suppression, dismissal in Sonar, or leave open and stop |

   Also fix `PlanSettingsDialogue.tooLate(@Nullable String state)` (`:201-202`): the parameter is annotated nullable but the
   body passes it to `Map.of`, which refuses null; the one caller (`:176-177`) checks `restoreState() != null` first. Make the
   contract true (a non-null parameter fed from a null-checked local at the call site), without a new Sonar finding.
   No text of `operator.*.properties` or `messages/*.properties`, no picocli option or verb name, no exit code and no rendered
   line changes. An issue whose fix would change behaviour is reported and put to the operator, not made.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:java-null-safety, plan-marshall:ref-code-quality
 - Done when:
   - `read_open_issues --pr N` names no open issue on the pull request (`sonar_open_new_issues=0`), and the default-branch read
     after Sonar's analysis of the merge commit (a `main` compute-engine task later than the merge, read through `sonar_rest
     ce-status --branch main`) names none of the six keys above as open, except `AaFMIQUlEHNTGUcqN-Y-` when the operator chose
     dismissal and it is named with that selection. While that analysis has not run the read is `unknown`, never `0`. Evidence
     order: `none`.
   - `./mvnw -pl pm-operator -am verify` and `./mvnw verify -Pintegration-tests -pl pm-operator -am` pass with no fewer tests
     than before; `git diff` names no file under `src/main/resources` and none under `src/test/resources/screen-captures`.
     Evidence order: `none`.
   - The existing tests that reach `HoldDialogue.execute`, `WorkChatDialogue.execute`, the `findings`/`watches` actions and
     `openWhatWaits` are named in the landing message (by test class and method); where none reaches an extracted branch, a test
     that fails when that branch's output line is removed is added first. Evidence order: test-first for every added test.
2. **The default-branch read**
 - What: after the merge and Sonar's analysis of the merge commit, read the default branch for the whole project and report
   the count by module and by rule, with every open key named. The three PLAN-181 keys `AaFGfsYupdXNQ-N4ArY-` (`java:S1313`),
   `AaGRMDtGxhXuOwwhaS0K` (`java:S4449`) and `AaEcTdY0MJNX7b6L9QAy` (`java:S112`) are the operator's to dismiss by hand; the
   read says whether they are still open. If the lane ends before the analysis has run, the read is reported as `unknown` with
   the compute-engine state, never as a forecast.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core
 - Done when:
   - The landing message carries the default-branch read with `count_status`, the analysed revision or the compute-engine task
     time against the merge time, and every open key with its rule and file; at most the three PLAN-181 keys (and the S1075 key
     if the operator chose dismissal) remain, or each other one is named with the operator's selection. Evidence order: `none`.

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`): the module builds named above, then the full `./mvnw verify`,
  `./mvnw verify -Ppre-commit` (review every rewrite; it is known not to be idempotent and touches files outside this plan:
  revert those) and `-Pcoverage` (minimum 80 % per module) at the head.
- **No new dependency or plugin.** Java 25, no generic `Exception`/`RuntimeException` caught or thrown in production code,
  `CuiLogger` through `LogMessages`, JUnit 5 only (no Mockito, PowerMock or Hamcrest), input test data from `Generators` with
  `@EnableGeneratorController`, contract values literal. Pre-1.0: no shim.
- No behaviour change: no catalogue text, verb or option name, exit code, rendered line or golden capture changes. No
  `NOSONAR`; a `@SuppressWarnings` only on the operator's selection (the S1075 question), recorded in the progress record and
  the landing message.
- A Sonar issue is fixed in a pushed commit; a dismissal is made only on the operator's selection and recorded as
  `operator-selected: {issue-key}: {reason}`. The transmission through
  `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}` is
  known to answer HTTP 400 (PLAN-181): when it does, the lane names the key, rule and reason in the PR body and the landing
  message so the operator can dismiss it by hand, and does not count it as fixed. The lane never records a dismissal of its own.
  ZERO Sonar findings on the new code of the pull request; state `sonar_open_new_issues=N` in the landing message (`unknown`
  for a failed read, never `0`).
- No e2e run is needed (`pm-web-app` and `pm-web-e2e` are not touched). Other plans run at the same time (this is the fifth
  concurrent run): never stop or reuse a process this worktree did not start.
- The merge rule: the run merges only when every review thread has a reply and is resolved and every review body is answered,
  and some head has had one CodeRabbit review. The pull request body states thread count, resolved count and review-body items
  from `ci pr comments`, never from memory. The run merges the pull request itself once these gates hold (`CLAUDE.md`, Git
  Workflow). Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`; the pull request body carries no generator
  footer. If CodeRabbit refuses on quota, stop and ask the operator.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch. The branch of the
  parked PLAN-42 (`feature/42-cli-verb-modes-and-help`) is never touched, rebased or merged by this run.

## Claim Labels

- OBSERVED: the default branch reads 9 open issues after Sonar's analysis of `81e2c1fc` (the `main` task at 2026-11-24T13:01:56Z, after the merge): the six keys of the table plus the three PLAN-181 keys — read on 2026-11-24 by `sonar read_open_issues --project plan-marshall_pm-mcp-clients` (`count_status: confirmed`)
  - verdict: corroborated | checked_at: b625fda50151f2471f6e968959d762bf55aebc2b | by: ui-improvements/next | rescoped: n/a | evidence: sonar read_open_issues default branch 2026-11-24: confirmed, the same 9 keys; main CE task 18:01:30Z after the d0bfd854 merge
- OBSERVED: `pm-operator` is unchanged between `81e2c1fc` and `62155055` — read by `git diff --stat 81e2c1fc 62155055 -- pm-operator` (empty)
  - verdict: corroborated | checked_at: b625fda50151f2471f6e968959d762bf55aebc2b | by: ui-improvements/next | rescoped: n/a | evidence: git diff --name-only 81e2c1fc b625fda5 -- pm-operator is empty
- OBSERVED: `WorkActionDialogue.java` uses `"findings"` and `"watches"` at `:180`, `:181` and `:500` — read at `62155055`
  - verdict: corroborated | checked_at: b625fda50151f2471f6e968959d762bf55aebc2b | by: ui-improvements/next | rescoped: n/a | evidence: pm-operator unchanged 81e2c1fc..b625fda5; WorkActionDialogue :180/:181 still the open keys at the 18:01:30Z analysis
- OBSERVED: `OverviewFullScreenApp.java:1374` is `private void openWhatWaits()` — read at `62155055`
  - verdict: corroborated | checked_at: b625fda50151f2471f6e968959d762bf55aebc2b | by: ui-improvements/next | rescoped: n/a | evidence: pm-operator unchanged 81e2c1fc..b625fda5; OverviewFullScreenApp :1374 still the open S3398 key
- OBSERVED: `LessonSourceCommand.java:45-46` declares `PATH_PROJECTS_PREFIX = "/api/v1/projects/"` and `SEGMENT_SOURCES_LESSONS`, and `AcceptEditCommand.java:66`, `ConfigCommand.java:68`, `CredentialsCommand.java:55` carry `@SuppressWarnings("java:S1075")` — read at `62155055`
  - verdict: corroborated | checked_at: b625fda50151f2471f6e968959d762bf55aebc2b | by: ui-improvements/next | rescoped: n/a | evidence: pm-operator unchanged 81e2c1fc..b625fda5; LessonSourceCommand :45 still the open S1075 key
- OBSERVED: `PlanSettingsDialogue.tooLate(@Nullable String state)` (`:201-202`) passes `state` to `Map.of`; its caller checks `result.restoreState() != null` (`:176-177`) — read at `81e2c1fc`
  - verdict: corroborated | checked_at: b625fda50151f2471f6e968959d762bf55aebc2b | by: ui-improvements/next | rescoped: n/a | evidence: pm-operator unchanged 81e2c1fc..b625fda5 (PlanSettingsDialogue :176-177, :201-202 as read at 81e2c1fc)
- OBSERVED: none of the running PLAN-178 (`feature/178-…`), PLAN-187 (`fix/187-…`) or PLAN-188 changes a file of this plan's surface — read by `git diff --name-only origin/main...origin/{branch}` on 2026-11-24 (PLAN-188 had no pushed branch yet; its surface is `pm-api` profiles and `ProfileDocumentReaderTest`)
  - verdict: corroborated | checked_at: b625fda50151f2471f6e968959d762bf55aebc2b | by: ui-improvements/next | rescoped: n/a | evidence: running branches 178/182/188 vs the six files: one shared, OverviewFullScreenApp.java with PLAN-178 (hunk @@ -945 only, far from :1374); a single shared file does not block (operator chose PLAN-189 for the slot); the later merge rebases
- HYPOTHESIS: the two `S3776` methods can drop below the threshold by extraction without reordering effects — confirm/refute at `HoldDialogue.java` § `execute` and `WorkChatDialogue.java` § `execute` (verify-at-outline)
  - verdict: unverifiable | checked_at: 62155055827ff283aa33b99c5413ac0dd2c9d553 | by: ui-improvements/next | rescoped: n/a | evidence: needs the extraction at outline
- HYPOTHESIS: `openWhatWaits` is called only from one inner class, so the `S3398` move is a pure relocation — confirm/refute at `OverviewFullScreenApp.java` § callers of `openWhatWaits` (verify-at-outline)
  - verdict: unverifiable | checked_at: 62155055827ff283aa33b99c5413ac0dd2c9d553 | by: ui-improvements/next | rescoped: n/a | evidence: callers of openWhatWaits not swept; the lane checks at outline
- Verify-first clause: before each deliverable re-read the named files and the Sonar list at the then-current HEAD; the parked PLAN-42 edits `OverviewFullScreenApp.java` and the CLI chain follows; a refuted claim re-scopes the deliverable and is reported, never silently worked around.
  - verdict: unverifiable | checked_at: 62155055827ff283aa33b99c5413ac0dd2c9d553 | by: ui-improvements/next | rescoped: n/a | evidence: procedural: the lane's verify-first step

## Expected Surface

- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/HoldDialogue.java` — present at HEAD 62155055
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/WorkChatDialogue.java` — present at HEAD 62155055
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/WorkActionDialogue.java` — present at HEAD 62155055
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/overview/OverviewFullScreenApp.java` — present at HEAD 62155055
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/LessonSourceCommand.java` — present at HEAD 62155055
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/PlanSettingsDialogue.java` — present at HEAD 62155055

## Dependencies and Sequencing

- Depends on: `PLAN-186` (shipped, #556).
- Overlaps with: `PLAN-42` (parked, branch unmerged: `OverviewFullScreenApp.java`; this plan does not wait for it, and whichever
  merges second rebases); the CLI chain `PLAN-44`, `48`, `50`, `63`, `184` and `PLAN-138` to `140` (staged, edit `pm-operator`,
  re-grounded after this plan lands). No running plan shares a file.

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-189-sonar-pm-operator-residue.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under
`.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger write. The
session reports its outcome through its PR and its inbox message (`persona-plan-orchestrator/standards/orchestration-model.md`
§ Ledger Write-Boundary).
