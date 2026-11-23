# PLAN-180: The Standing Sonar Debt, Part 2 of 3: `pm-api` Tests, `JsonTree` and the Last Production Issues

epic: ui-improvements
workstream: WS-04

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Staged on the operator's word of
> 2026-11-23 (decision 4, proposal 1), which carries out the part the operator cut from PLAN-175 (`landings/PLAN-175.md`): part 1 of
> 3 landed as #530 (`f00da06e`, the `pm-api` production code). **The operator approved one new dependency for it on 2026-11-23:
> Awaitility (`org.awaitility:awaitility`, test scope), see the Standing Constraints.** This spec is SELF-SUFFICIENT: the emitted
> command is a one-line pointer and carries no brief. It shares no file with the web plans and none with PLAN-174 (#533).

## Objective

The default branch carries standing Sonar code smells although the quality gate is green for every pull request (the zero policy covers new code only). Part 1 cleared the `pm-api` production code; this part clears the `pm-api` tests, the three production issues part 1 left (`java:S6878`, `java:S1168`, `java:S1075`) and replaces `Thread.sleep` in the tests with condition waits. Part 3 (PLAN-181) takes the other modules; the `pm-operator` part follows PLAN-174.

## Deliverables

Read first: `PLAN-175-sonar-standing-debt.md` (the rule families, the shape of each fix and the split guard: a pull request stays under 100 files, CodeRabbit refuses above that) and `landings/PLAN-175.md` (what part 1 left, with its file and line). The open list is read by the lane at the start of each deliverable through the repository's provider, never from the numbers below: `python3 .plan/execute-script.py plan-marshall:workflow-integration-sonar:sonar read_open_issues --project plan-marshall_pm-mcp-clients` (the default branch) and `--pr N` for the pull request. **The numbers of 2026-11-22 (`pm-api` 45 = 3 main + 42 test) and a capped search of 2026-11-23 (500 issues, `pm-api` main 98) disagree; the first step of the run is to read the true count and to say which one holds.**

The work was done once on the local branch `backup/plan-175-full` (base `5c1c2730`, which is behind `main`): commits `ad7e2854` (the `pm-api` tests, 14 files), `8055c795` (Awaitility instead of `Thread.sleep`, 6 files and the POMs), `f6a484f3` (`JsonTree` reads an optional member as `Optional`, 23 files). The run may take each from the branch (a branch ref is shared by worktrees) but treats it as a lead: each change is re-checked against the head, the build and the Sonar read decide, and the work is redone from the Sonar read where the branch is absent or has drifted.

1. **The three production issues part 1 left**
 - What: `java:S6878` at `HttpResponse.java:48` (undeclared in part 1's landing message; fixable without a contract change, as `PosixSpawn.java:99-118` shows); `java:S1168` at `JsonTree.java:276` (the route through `Optional` changes a public signature: accepted, pre-1.0, no shim; every caller in `pm-api`, `pm-operator`, `pm-relay` and `pm-mock-runtime` follows, and the `fromMap` signatures that lose `throws IOException` are listed in the pull request); `java:S1075` at `RuntimeClient.java:34` (contract-bound: fix it where the contract allows, otherwise it is NOT dismissed by the run: the operator is asked, see the Standing Constraints). Part 1's other leftovers are read and fixed where small: the live `cui-rewrite:disable` at `Libc.java:91`, the wire-key constant reused as a default at `EpicProcess.java:51`, and the 33 pre-existing `@SuppressWarnings`/`NOSONAR` sites (each is removed or justified in the pull request, none is left silently).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:java-null-safety, pm-dev-java:javadoc, pm-dev-java-cui:cui-logging
 - Done when:
   - `read_open_issues` of the pull request names no open issue under `pm-api/src/main` and no hotspot; after the merge and Sonar's analysis of the merge commit (`analysis_revision` equals the merge commit) the default branch names none either (`unknown` while the analysis has not run, never `0`). Evidence order: `none`.
   - `./mvnw -pl pm-api -am verify` passes, the number of tests is not lower than before, and the callers of the changed signatures compile through `./mvnw verify` of the affected modules. Evidence order: `none`.
2. **The `pm-api` tests**
 - What: every open issue in `pm-api/src/test` at the read (`ProcessTreeTest.java` carried the most): the argument order of assertions (`java:S3415`), a lambda of `assertThrows` that holds one throwing call (`java:S5778`), too many assertions in one test split into tests (`java:S5961`), duplicated literals, thrown exceptions declared and never thrown (`java:S1130`), restricted identifiers (`java:S6213`) and the rest the read names; the shape of each is in PLAN-175's deliverable 2. No assertion is loosened (a literal turned into `contains` or `isNotNull`, a removed assertion): read in the diff by the lane and stated in the landing message.
 - Persona: plan-marshall:persona-module-tester
 - Skills: pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - `read_open_issues` names no open issue under `pm-api/src/test`, on the pull request and (after the analysis of the merge commit) on the default branch. Evidence order: `none`.
   - `./mvnw -pl pm-api verify` passes, the number of tests is not lower than before, and no assertion is loosened (read in the diff). Evidence order: `none`.
3. **Condition waits instead of `Thread.sleep` (Awaitility)**
 - What: the tests of `pm-api` that wait by `Thread.sleep` (the files of `8055c795`: `DetachedStartTest.java`, `OnDemandStartTest.java`, `RuntimeRecordTest.java`, `CliProcess.java`, `FakeRuntimeServer.java`, `StagedDaemon.java`) wait on the condition they need with Awaitility, with a stated timeout. The dependency is added by this deliverable only as the operator approved it: `org.awaitility:awaitility`, version `4.3.0` managed in the root POM's `dependencyManagement` (the parent does not manage it) at `test` scope, declared in `pm-api` only; the root POM's enforcer allow-list for the test classpath names it, and `BuildGuardsIT`'s fixtures still prove each guard. No other module gets it in this plan (the `pm-operator` plan names its own use).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-dev-java:manage-maven-profiles
 - Done when:
   - `git grep -n "Thread.sleep" -- pm-api/src/test` is empty; `./mvnw -pl pm-api verify` and `BuildGuardsIT` pass; the dependency appears in the POMs exactly as stated above and nowhere else. Evidence order: `none`.
   - The run states in the landing message that the dependency was approved by the operator on 2026-11-23 and which POMs changed. Evidence order: `none`.
4. **The zero read for `pm-api`**
 - What: after the last pull request of this plan is merged and Sonar has analysed the merge commit, read the default branch for `pm-api` and report the count, by module and by rule, in the landing message.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core
 - Done when:
   - `read_open_issues` of the default branch names no open issue under `pm-api` (main and test), or every remaining issue is named with its key, its rule and the operator's selection; the read is `unknown` while the analysis of the merge commit has not run, never `0`. Evidence order: `none`.

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`): `./mvnw -pl pm-api -am verify`, then the full `./mvnw verify` and `./mvnw verify -Ppre-commit` (review every rewrite) at the head; `-Pcoverage` at the head (minimum 80 % per module); `-Pintegration-tests` for `BuildGuardsIT` after the POM change.
- **The only new dependency is Awaitility `4.3.0`, test scope, as approved by the operator on 2026-11-23** (decision 4); no other dependency or plugin is added, and a different version or scope needs the operator's word.
- Code standards of `CLAUDE.md`: Java 25, no generic `Exception` in production code, `CuiLogger` through `LogMessages`, JUnit 5 only (no Mockito, PowerMock or Hamcrest), input test data from `Generators` with `@EnableGeneratorController`. Pre-1.0: no shim and no deprecation cycle for a changed public signature.
- No behaviour change: no wire name, no catalogue text and no resource under `src/main/resources` changes; a fix that would change behaviour is reported, not made.
- Each pull request stays under 100 files and 20k lines (CodeRabbit gives no review above that): the deliverables are cut into pull requests accordingly and rebased on `main` in order.
- A Sonar issue is fixed in a pushed commit; a dismissal (`java:S1075` or any rule the contract binds) is made only on the operator's selection and recorded as `operator-selected: {issue-key}: {reason}` in the progress record, then transmitted through `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. The lane never records a dismissal of its own.
- ZERO Sonar findings on the new code of every pull request (the operator, 2026-11-05); state `sonar_open_new_issues=N` in the landing message (`unknown` for a failed read, never `0`).
- The merge rule: the run merges only when every review thread has a reply and is resolved and every review body is answered, and some head has had one CodeRabbit review. The pull request body states thread count, resolved count and review-body items from `ci pr comments`, never from memory. The run merges the pull request itself once these gates hold (`CLAUDE.md`, Git Workflow). Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`; the pull request body carries no generator footer. If CodeRabbit refuses on quota, stop and ask the operator.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch. The branch `backup/plan-175-full` is read, never moved, reset or deleted.

## Claim Labels

- OBSERVED: the branch `backup/plan-175-full` exists locally only, based at `5c1c2730`, 8 commits ahead of `origin/main`, with the commits named above — read by `git log origin/main..backup/plan-175-full` and `git branch -r --list "origin/*175*"` (empty)
  - verdict: corroborated | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: git log origin/main..backup/plan-175-full lists 8 commits (af8a73b5, ad7e2854, 0ad1febd, 00850b31, 69865745, 71706e2f, 8055c795, f6a484f3), merge-base 5c1c2730; git branch -r --list origin/*175* is empty (origin/main 3ae8a305)
- OBSERVED: the branch's commits `ad7e2854`, `8055c795`, `f6a484f3` and the module commits touch no file of the open pull request #533 except `OnboardingScenarioTest.java` (a `pm-mock-runtime` test, deliverable of PLAN-181) — read by `gh pr view 533 --json files` against `git show --name-only`
  - verdict: corroborated | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: gh pr view 533 lists 90 files; comm against git show --name-only of f6a484f3, 8055c795, ad7e2854, 00850b31, 69865745, 71706e2f shows 0 shared; 0ad1febd shares OnboardingScenarioTest.java only
- OBSERVED: the branch adds `org.awaitility:awaitility` `4.3.0` at test scope to the root POM's `dependencyManagement` and to `pm-api/pom.xml` (and a change in `pm-operator/pom.xml`, not part of this plan) and widens the enforcer allow-list — read by `git diff 5c1c2730 backup/plan-175-full -- pom.xml pm-api/pom.xml pm-operator/pom.xml`
  - verdict: corroborated | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: git diff 5c1c2730 backup/plan-175-full -- pom.xml pm-api/pom.xml: awaitility 4.3.0 test scope in root dependencyManagement and pm-api; the enforcer comment widened; pm-operator/pom.xml also differs
- OBSERVED: `pm-operator` and `pm-web-app` are touched by the branch only as stated (`pm-operator`: the POM; `pm-web-app`: 9 files) — read by `git diff --name-only`
  - verdict: corroborated | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: git diff --name-only 5c1c2730..backup/plan-175-full -- pm-operator pom.xml lists pm-operator/pom.xml and pom.xml; pm-web-app 9 files per git show --name-only 00850b31
- HYPOTHESIS: the default branch's `pm-api/src/main` carries 3 open issues after part 1 (`HttpResponse.java:48` `java:S6878`, `JsonTree.java:276` `java:S1168`, `RuntimeClient.java:34` `java:S1075`), not the 98 a capped search of 2026-11-23 listed — confirm/refute by `sonar read_open_issues --project plan-marshall_pm-mcp-clients` at the start (verify-at-outline)
  - verdict: unverifiable | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: two reads disagree: the PLAN-175 landing says pm-api main 3 open (2026-11-22), the capped search of 2026-11-23 lists 98 under pm-api/src/main; settled by read_open_issues at the start
- HYPOTHESIS: the parent POM does not manage Awaitility — confirm/refute in the resolved effective POM of `pm-api` (verify-at-outline)
  - verdict: unverifiable | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: not re-read here; the branch's root POM comment says Awaitility is not managed by the parent; settled at outline
- Verify-first clause: before each deliverable re-read the named files and the Sonar list at the then-current HEAD; PLAN-174 (#533), PLAN-181 and the web plans edit other files, but `runtime`, `JsonTree` callers in `pm-operator` and `pm-mock-runtime` follow a changed signature. A refuted claim re-scopes the deliverable and is reported, never silently worked around.
  - verdict: unverifiable | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: procedural verify-first clause; PLAN-174 running

## Expected Surface

- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/http/HttpResponse.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/json/JsonTree.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/runtime/RuntimeClient.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/json/JsonTreeTest.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/process/ProcessTreeTest.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/runtime/DetachedStartTest.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/runtime/OnDemandStartTest.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/runtime/RuntimeRecordTest.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/testsupport/CliProcess.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/testsupport/FakeRuntimeServer.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/testsupport/StagedDaemon.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/decision/DecisionRecordsTest.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/event/EventEnvelopeTest.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/host/ProfileDocumentReaderTest.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/http/UnixSocketHttpClientTest.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/interaction/OperatorStateResourceTest.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/onboarding/OnboardingRecordsTest.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/overview/OverviewRecordsTest.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-api/pom.xml` — present at HEAD 3ae8a305
- OBSERVED: `pom.xml` — present at HEAD 3ae8a305

## Dependencies and Sequencing

- Depends on: `PLAN-175` part 1 (shipped, #530).
- Overlaps with: `PLAN-181` (staged: `JsonTree` callers in `pm-mock-runtime` follow a changed signature, `OnboardingScenarioTest.java` is also in open PR #533), `PLAN-174` (running: `pm-operator` callers of a changed `JsonTree` signature; no shared file by the file lists). Either order with `PLAN-181`; a multi-file overlap with a running plan is the operator's decision at emit.
- Adjacent to: the `pm-operator` Sonar part (a later plan, after PLAN-174 lands; it names its own use of Awaitility).

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-180-sonar-standing-debt-pm-api-part-2.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger write. The session reports its outcome through its PR and its inbox message (`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary).
