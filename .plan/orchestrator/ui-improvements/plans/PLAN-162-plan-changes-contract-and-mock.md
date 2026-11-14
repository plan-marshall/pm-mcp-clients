# PLAN-162: A Plan's Changes — the Contract and the Mock Runtime

epic: ui-improvements
workstream: WS-01

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Staged on the operator's decision of 2026-11-14
> ("Stage plan-changes rows", cleanup of the parked gates) from the work package `doc/plans/plan-changes.adoc` (index status `open`). This spec is SELF-SUFFICIENT.

## Objective

The operator reads the code a plan changed in either client, also where no pull request exists. The runtime model specifies
`GET /api/v1/plans/{plan_id}/changes`, `GET …/changes/file?path=`, the event `plan.change_captured` and the activity entry
`model_differs`. None of these exist in `pm-api` or the mock today. This plan adds the contract and the mock behaviour, which the terminal
(PLAN-163) and the web (PLAN-164) then read. It covers task 1 of the work package and the contract side of task 4.

## Deliverables

Read first:
- `../plan-marshall-documentation/doc/specification/runtime-model/01-processes-and-transport.adoc` § `plan-changes` (The Plan's Changes): the list, a file, read-never-run, untrusted text, `not_captured`, `unavailable`, live.
- `../plan-marshall-documentation/doc/specification/runtime-model/06-event-catalogue.adoc:68`: `plan.change_captured` {`plan_id`, `head`, `files_total`, `insertions_total`, `deletions_total`, the unplanned count}.
- `doc/design/shared/plan-view.adoc` `#changes` (:172) and `#worker`.
- `pm-api/src/main/java/de/planmarshall/api/event/EventName.java:41`, the comment that `plan.change_captured` "is not a member yet".
- `pm-api/src/main/java/de/planmarshall/api/overview/ChangeSize.java`, the size the plan already carries, which the list must agree with.
- `pm-api/src/main/java/de/planmarshall/api/process/ActivityEntryKind.java`: `text`, `tool_call`, `file_changed`, `question`, `answer_ack`, `link`; no `model_differs`.
- `../pm-mcp-core/doc/specification/harness-profiles/01-profile-schema.adoc` `#served-model`.

1. **The changes contract in `pm-api`**
 - What:
   - Add records and a codec for the list (`head`, `base_ref`, files with `path`, `renamed_from`, `status`, `added`, `removed`, `binary`, `unplanned`) and for a file's hunks (`old_start`, `old_len`, `new_start`, `new_len`, lines marked `context` / `removed` / `added`, `truncated` with the count left out).
   - Model the two refusals, `not_captured` and `unavailable` with its reason, as answers, never as an empty list.
   - Add route builders, `plan.change_captured` to `EventName` (remove the "not a member yet" comment) with its payload record, and `model_differs` to `ActivityEntryKind` with its fields (launch model, served model).
   - Use a new package with `package-info.java` and `@NullMarked`, following the layout of `overview/`.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:java-null-safety, pm-dev-java-cui:cui-testing
 - Done when:
   - A contract test per answer (list, file, binary file, truncated file, `not_captured`, `unavailable`) round-trips the wire JSON literally. Each fails on a revert. Evidence order: `test-first`.
   - `EventName` parses `plan.change_captured`, and `ActivityEntryKind.fromValue("model_differs")` resolves. Both are pinned by a test.

2. **The mock serves the changes**
 - What:
   - Serve the two routes and broadcast `plan.change_captured` in the mock runtime, from scenario data.
   - Fixtures (`running-work.json`): a plan before its first capture (`not_captured`); one with renamed, binary, unplanned and truncated ("cut") files; an ended plan whose captured commit is gone (`unavailable`).
   - A worker's activity holding a `model_differs` entry.
   - The list's totals agree with the plan's `ChangeSize`.
   - A path and a line each carry a planted control sequence, which the clients' tests use to prove the text is shown as text.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java-cui:cui-testing, pm-dev-java:junit-integration
 - Done when:
   - Mock scenario tests read every answer of D1 from the routes, including `not_captured` and `unavailable`. A broadcast `plan.change_captured` is followed by a list read that shows the new head. Each fails on a revert. Evidence order: `test-first`.
   - The scenario load validation accepts the new fixtures and refuses a file whose totals contradict `ChangeSize`.

3. **The work package's status**
 - What: set the status of `doc/plans/plan-changes.adoc` in `doc/plans/README.adoc` to `in progress`, naming PLAN-162..164.
 - Persona: plan-marshall:persona-documenter
 - Skills: pm-documents:ref-asciidoc
 - Done when: the index row says `in progress` and names the three plans.

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`). At the head, `./mvnw verify -Ppre-commit` passes (every rewrite reviewed and committed), and so does `./mvnw -pl pm-api,pm-mock-runtime verify`.
- No new dependency or plugin. In particular, NO diff library: the hunks are served data, and the mock serves them from fixtures.
- Web standards of `CLAUDE.md`. Every changed line and every path is untrusted text: render it through a text sink only, never `{@html}`/`innerHTML`.
- Java: JUnit 5, input from `Generators` with `@EnableGeneratorController`, no Mockito; expected wire and catalogue text stays literal. Never catch or throw generic `Exception`/`RuntimeException`.
- Ports of this plan: per `AGENTS.md` § Ports (`20000 + 10 × 162`): mock web port 21620, app port 21621. Never stop or reuse a process this worktree did not start.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.
- **The merge rule:**
  - At least one CodeRabbit review of some head, with its findings handled. If CodeRabbit refuses on quota, stop and ask the operator.
  - Every review thread replied and resolved, and every review body item answered on the pull request, read from GitHub's thread state, never from a count kept in memory.
  - **Every reply answers its own finding.** Bind each reply to the finding's anchor. A bot's follow-up "not marking as addressed" means the finding is still open (lesson `2026-11-14-14-007`).
  - The run merges the pull request itself once these gates hold (`CLAUDE.md`, Git Workflow). Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`. The pull request body carries no generator footer.
- ZERO Sonar findings: 0 open new-code issues and 0 hotspots, read at the pull request head after the push (`unknown` for a failed read, never `0`). This is a merge gate.
- The landing verdict is given per Done-when clause, never per deliverable (lesson `2026-11-14-14-001`).

## Claim Labels

- OBSERVED: `EventName.java:41` says `plan.change_captured` "is not a member yet" — read at `7adb56c2`
  - verdict: corroborated | checked_at: 7adb56c2 | by: ui-improvements/cleanup | rescoped: n/a | evidence: read at origin/main 7adb56c2 while staging (EventName:41 comment, ActivityEntryKind six kinds, no changes route, ChangeSize + ChangeSizeContractTest)
- OBSERVED: `ActivityEntryKind` has six kinds and no `model_differs` — read at `7adb56c2`
  - verdict: corroborated | checked_at: 7adb56c2 | by: ui-improvements/cleanup | rescoped: n/a | evidence: read at origin/main 7adb56c2 while staging (EventName:41 comment, ActivityEntryKind six kinds, no changes route, ChangeSize + ChangeSizeContractTest)
- OBSERVED: no `changes` route, record or mock handler exists; `git grep change_captured|not_captured` over `pm-api/src/main` finds only the `EventName` comment — read at `7adb56c2`
  - verdict: corroborated | checked_at: 7adb56c2 | by: ui-improvements/cleanup | rescoped: n/a | evidence: read at origin/main 7adb56c2 while staging (EventName:41 comment, ActivityEntryKind six kinds, no changes route, ChangeSize + ChangeSizeContractTest)
- OBSERVED: `overview/ChangeSize.java` exists and is pinned by `ChangeSizeContractTest` — read at `7adb56c2`
  - verdict: corroborated | checked_at: 7adb56c2 | by: ui-improvements/cleanup | rescoped: n/a | evidence: read at origin/main 7adb56c2 while staging (EventName:41 comment, ActivityEntryKind six kinds, no changes route, ChangeSize + ChangeSizeContractTest)
- HYPOTHESIS: the plan-detail resource already carries the size a capture updates, so `plan.change_captured` needs no new field there — confirm/refute at `ProcessTree.java` / `ActivePlanSummary.java` (verify-at-outline)
  - verdict: unverifiable | checked_at: 7adb56c2 | by: ui-improvements/cleanup | rescoped: n/a | evidence: HYPOTHESIS, verify-at-outline

## Expected Surface

- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/event/EventName.java` — present at `7adb56c2`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/process/ActivityEntryKind.java` — present at `7adb56c2`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/process/ActivityEntry.java` — present at `7adb56c2`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/changes/package-info.java` — absent; created by this plan
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/changes/PlanChangesContractTest.java` — absent; created by this plan
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` — present at `7adb56c2`
- OBSERVED: `pm-mock-runtime/src/main/resources/scenarios/running-work.json` — present at `7adb56c2`
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/PlanChangesScenarioTest.java` — absent; created by this plan
- OBSERVED: `doc/plans/README.adoc` — present at `7adb56c2`

## Dependencies and Sequencing

- Depends on: the `overview-order-and-size` contract (`ChangeSize`, shipped through PLAN-82/34). No staged row.
- Overlaps with:
  - `PLAN-140` D-R134 (staged): `MockRuntimeEngine.java` and `running-work.json`. Do not run the two at once; the second to land rebases.
  - `PLAN-30` (running): none.
- Unblocks: `PLAN-163` (terminal), `PLAN-164` (web tab), and through 164, `PLAN-55`.

## Re-Grounding at Restart

- Re-read every file above at the then-current HEAD.
- Check whether `PLAN-140` D-R134 changed the fixtures.
- Settle the HYPOTHESIS.

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-162-plan-changes-contract-and-mock.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/`
other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger write (`persona-plan-orchestrator/standards/orchestration-model.md`
§ Ledger Write-Boundary).
