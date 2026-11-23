# PLAN-181: The Standing Sonar Debt, Part 3 of 3: `pm-mock-runtime`, `pm-relay`, `pm-exec` and `pm-web-app`

epic: ui-improvements
workstream: WS-04

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Staged on the operator's word of
> 2026-11-23 (decision 4, proposal 1), which carries out the part the operator cut from PLAN-175 (`landings/PLAN-175.md`): part 1
> of 3 landed as #530 (`f00da06e`); part 2 is PLAN-180 (`pm-api`). The `pm-operator` part follows PLAN-174 and is not in this plan.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief. **It adds no dependency.**
> Emit it where the web plans do not run (see Dependencies and Sequencing): its `pm-web-app` files are shared with them.

## Objective

The default branch carries standing Sonar code smells although the quality gate is green for every pull request (the zero policy covers new code only). Parts 1 and 2 clear `pm-api`; this part clears the open issues of `pm-mock-runtime` (the largest: duplicated literals, always-true expressions, nullable annotations, restricted identifiers), `pm-relay`, `pm-exec` and the `pm-web-app` sources and tests, without a behaviour change: no scenario JSON, no wire name and no catalogue text moves.

## Deliverables

Read first: `PLAN-175-sonar-standing-debt.md` (the rule families, the shape of each fix and the split guard: a pull request stays under 100 files, CodeRabbit refuses above that) and `landings/PLAN-175.md`. The open list is read by the lane at the start of each deliverable through the repository's provider, never from a number in a spec: `python3 .plan/execute-script.py plan-marshall:workflow-integration-sonar:sonar read_open_issues --project plan-marshall_pm-mcp-clients` (the default branch) and `--pr N` for the pull request. **The numbers of 2026-11-22 and a capped search of 2026-11-23 (500 issues: `pm-mock-runtime` 198, `pm-operator` 166, `pm-api` 126, `pm-web-app` 10) differ from PLAN-175's; the first step of the run is to read the true count per module and to say which one holds.**

The work was done once on the local branch `backup/plan-175-full` (base `5c1c2730`, behind `main`): commits `0ad1febd` (`pm-mock-runtime`, 16 files), `69865745` (`pm-relay`, 5 files), `71706e2f` (`pm-exec`, 2 files) and `00850b31` (`pm-web-app`, 9 files). The run may take each from the branch (a branch ref is shared by worktrees) but treats it as a lead: each change is re-checked against the head, the build and the Sonar read decide, and the work is redone from the Sonar read where the branch is absent or has drifted. The branch is never moved, reset or deleted.

1. **`pm-mock-runtime`**
 - What: every open issue in `pm-mock-runtime` at the read, main and test (the branch's files are listed in the Expected Surface; `MockRuntimeEngine.java` and the scenario tests carry most): duplicated literals become constants (`java:S1192`), always-true expressions go (`java:S2589`), nullable parameters are annotated (`java:S4449`), restricted identifiers are renamed (`java:S6213`), cognitive complexity is reduced by extracting methods without changing the order of effects (`java:S3776`), and the rest the read names. No scenario JSON, wire name or resource under `src/main/resources` changes, and no assertion is loosened (a literal turned into `contains` or `isNotNull`, a removed assertion): read in the diff by the lane and stated in the landing message.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - `read_open_issues` names no open issue under `pm-mock-runtime`, on the pull request and (after Sonar's analysis of the merge commit, `analysis_revision` equal to it) on the default branch (`unknown` while the analysis has not run, never `0`). Evidence order: `none`.
   - `./mvnw -pl pm-mock-runtime -am verify` passes, the number of tests is not lower than before, `git diff` names no file under `src/main/resources`, and the CLI and web modules that run against the mock still pass (`./mvnw verify`). Evidence order: `none`.
2. **`pm-relay` and `pm-exec`**
 - What: every open issue in `pm-relay` (`PmMcp.java`, `ProtocolData.java`, `Relay.java`, `RelayTest.java`, `PmMcpProcessIT.java` on the branch) and in `pm-exec` (`NativeKernel.java`, `PmExecIT.java`); the Linux-only kernel class `LinuxCalls` stays excluded from the coverage report as the root POM states, and no assertion is loosened.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - `read_open_issues` names no open issue under `pm-relay` and `pm-exec`, on the pull request and on the default branch after the analysis of the merge commit. Evidence order: `none`.
   - `./mvnw -pl pm-relay,pm-exec -am verify` and `./mvnw verify -Pintegration-tests -pl pm-relay,pm-exec -am` pass, and the packaged binaries' behaviour tests (`*IT`) pass. Evidence order: `none`.
3. **`pm-web-app` sources and tests**
 - What: every open issue in `pm-web-app` at the read (the branch's nine files: `api/client.ts`, `api/scenarioMockFetch.ts`, `i18n/index.ts`, `icons/workflowIcons.ts`, `routes.ts`, `stores/runtime.ts`, `CleanupView.test.ts`, `PairingView.scenarios.test.ts`, `PlanView.test.ts`; the nested ternary `typescript:S3358` in `scenarioMockFetch.ts:137` is one): the smell is removed by a refactor that changes no behaviour and no wire name; `runtime.ts`, `routes.ts` and `client.ts` are also edited by the web plans, so this deliverable is the last to start where one of them runs (see Dependencies and Sequencing) and is rebased on `main` before every push.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - `read_open_issues` names no open issue under `pm-web-app`, on the pull request and on the default branch after the analysis of the merge commit. Evidence order: `none`.
   - `./mvnw -pl pm-api,pm-web-app verify` passes with the same number of tests or more and the text-guard and vocabulary baselines unchanged; `-Pe2e` at the head is green when `src/lib/api` or `src/lib/stores` changed. Evidence order: `none`.
4. **The zero read for these modules**
 - What: after the last pull request of this plan is merged and Sonar has analysed the merge commit, read the default branch for `pm-mock-runtime`, `pm-relay`, `pm-exec` and `pm-web-app` and report the count, by module and by rule, in the landing message, together with the standing count of `pm-operator` (the part after PLAN-174).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core
 - Done when:
   - `read_open_issues` of the default branch names no open issue under the four modules, or every remaining issue is named with its key, its rule and the operator's selection; the read is `unknown` while the analysis of the merge commit has not run, never `0`. Evidence order: `none`.

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`): the module builds named above, then the full `./mvnw verify` and `./mvnw verify -Ppre-commit` (review every rewrite) at the head; `-Pcoverage` at the head (minimum 80 % per module).
- **No new dependency or plugin.** Awaitility is PLAN-180's, in `pm-api` only. Code standards of `CLAUDE.md`: Java 25, no generic `Exception` in production code, `CuiLogger` through `LogMessages`, JUnit 5 only (no Mockito, PowerMock or Hamcrest), input test data from `Generators` with `@EnableGeneratorController`; web standards of `CLAUDE.md` for `pm-web-app` (no `{@html}`, no timer wait in a test). Pre-1.0: no shim.
- No behaviour change: no scenario JSON, wire name, catalogue text or resource under `src/main/resources` changes; a fix that would change behaviour is reported, not made.
- Each pull request stays under 100 files and 20k lines (CodeRabbit gives no review above that): this plan's four deliverables are four pull requests, rebased on `main` in order.
- A Sonar issue is fixed in a pushed commit; a dismissal is made only on the operator's selection and recorded as `operator-selected: {issue-key}: {reason}` in the progress record, then transmitted through `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. The lane never records a dismissal of its own.
- ZERO Sonar findings on the new code of every pull request (the operator, 2026-11-05); state `sonar_open_new_issues=N` in the landing message (`unknown` for a failed read, never `0`).
- Ports for the `pm-web-app` deliverable's e2e run: mock web port 21810, app port 21811 (`20000 + 10 × 181`) (`AGENTS.md` § Ports). Other plans run at the same time: never stop or reuse a process this worktree did not start.
- The merge rule: the run merges only when every review thread has a reply and is resolved and every review body is answered, and some head has had one CodeRabbit review. The pull request body states thread count, resolved count and review-body items from `ci pr comments`, never from memory. The run merges the pull request itself once these gates hold (`CLAUDE.md`, Git Workflow). Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`; the pull request body carries no generator footer. If CodeRabbit refuses on quota, stop and ask the operator.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.

## Claim Labels

- OBSERVED: the branch `backup/plan-175-full` exists locally only, based at `5c1c2730`, with the commits `0ad1febd`, `69865745`, `71706e2f` and `00850b31` for the four modules — read by `git log origin/main..backup/plan-175-full` and `git branch -r --list "origin/*175*"` (empty)
  - verdict: corroborated | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: git log origin/main..backup/plan-175-full lists 0ad1febd, 69865745, 71706e2f, 00850b31 among 8 commits; no origin/*175* branch (origin/main 3ae8a305)
- OBSERVED: the four commits touch 32 files, all present at `origin/main`, and only `pm-mock-runtime/src/test/java/de/planmarshall/mock/OnboardingScenarioTest.java` is also in the file list of open pull request #533 — read by `git cat-file -e origin/main:{path}` for each and `gh pr view 533 --json files` against `git show --name-only`
  - verdict: corroborated | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: git cat-file -e origin/main:{path} succeeds for all 32 files of the four commits; the only file also in PR #533's list is OnboardingScenarioTest.java
- OBSERVED: a capped search of the default branch on 2026-11-23 listed 500 code smells, `pm-mock-runtime` 198, `pm-operator` 166, `pm-api` 126, `pm-web-app` 10 — read by `sonar_rest search --project plan-marshall_pm-mcp-clients --types CODE_SMELL` (the response is capped at 500, so the true total may be larger)
  - verdict: corroborated | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: sonar_rest search --types CODE_SMELL on 2026-11-23 returned 500 issues: pm-mock-runtime 198, pm-operator 166, pm-api 126, pm-web-app 10 (capped response)
- OBSERVED: the nested ternary smell `typescript:S3358` stands at `pm-web-app/src/lib/api/scenarioMockFetch.ts:137` — read by the same search
  - verdict: corroborated | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: the same search lists typescript:S3358 at pm-web-app/src/lib/api/scenarioMockFetch.ts line 137
- HYPOTHESIS: `pm-relay` and `pm-exec` carry open issues on the default branch (the branch fixed them in 5 and 2 files) — confirm/refute by `sonar read_open_issues` at the start (verify-at-outline; the capped search listed none for them)
  - verdict: unverifiable | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: the capped search listed no issue for pm-relay or pm-exec; the cap may hide them; settled by read_open_issues at the start
- HYPOTHESIS: the packaged binaries' integration tests run under `-Pintegration-tests` without `GRAALVM_HOME` (the native profile is not needed here) — confirm/refute in the root POM profiles (verify-at-outline)
  - verdict: unverifiable | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: root POM profiles not re-read here; settled at outline
- Verify-first clause: before each deliverable re-read the named files and the Sonar list at the then-current HEAD; PLAN-144, 176, 179 and the other web plans edit `runtime.ts`, `routes.ts` and `client.ts`, and PLAN-174 (#533) edits `OnboardingScenarioTest.java`. A refuted claim re-scopes the deliverable and is reported, never silently worked around.
  - verdict: unverifiable | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: procedural verify-first clause; PLAN-174 running

## Expected Surface

- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/MockRuntime.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/StandardScenarios.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/http/MockEventStream.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/http/MockHttpRequest.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/http/MockHttpResponse.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/http/MockHttpServer.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/DesignerRegistry.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/StartRegistry.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/scenario/ScenarioLoader.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/scenario/ScenarioRegistry.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/ControlScenarioTest.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/DecisionsScenarioTest.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/FullyOnboardedScenarioTest.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/OnboardingScenarioTest.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/RunningWorkScenarioTest.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-relay/src/main/java/de/planmarshall/relay/PmMcp.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-relay/src/main/java/de/planmarshall/relay/ProtocolData.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-relay/src/main/java/de/planmarshall/relay/Relay.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-relay/src/test/java/de/planmarshall/relay/PmMcpProcessIT.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-relay/src/test/java/de/planmarshall/relay/RelayTest.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-exec/src/main/java/de/planmarshall/exec/NativeKernel.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-exec/src/test/java/de/planmarshall/exec/PmExecIT.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/api/client.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/api/scenarioMockFetch.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/i18n/index.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/icons/workflowIcons.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/routes.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/stores/runtime.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/views/CleanupView.test.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/views/PairingView.scenarios.test.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/views/PlanView.test.ts` — present at HEAD 3ae8a305

## Dependencies and Sequencing

- Depends on: `PLAN-175` part 1 (shipped, #530).
- Overlaps with: `PLAN-174` (running: `OnboardingScenarioTest.java`, one file, which does not block), `PLAN-144`, `PLAN-176`, `PLAN-177`, `PLAN-178`, `PLAN-179`, `PLAN-85` (the `pm-web-app` files `runtime.ts`, `routes.ts` and `client.ts`: deliverable 3 starts after the web plans that run have landed, or on the operator's decision at emit), `PLAN-180` (`JsonTree` callers in `pm-mock-runtime`). Deliverables 1, 2 and 4's other modules are independent of the web plans and can run first.
- Adjacent to: the `pm-operator` Sonar part (a later plan, after PLAN-174 lands).

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-181-sonar-standing-debt-modules-part-3.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger write. The session reports its outcome through its PR and its inbox message (`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary).
