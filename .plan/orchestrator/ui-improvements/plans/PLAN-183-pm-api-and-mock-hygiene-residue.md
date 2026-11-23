# PLAN-183: `pm-api` and the Mock: the Plan Phase-State Vocabulary and the Epic Cancel and Next Actions

epic: ui-improvements
workstream: WS-01

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Staged on the operator's word of 2026-11-23 (decision 9, proposal 1) for two entries of `epic.md` whose named owners shipped without fixing them: the `EpicProcessPlan` phase-state vocabulary (PLAN-06 residue, owner PLAN-34 shipped as #371) and the mock's epic action routes (PLAN-10 residue, owner PLAN-11 shipped). This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief. **It adds no dependency.** Emit it after PLAN-181 lands: PLAN-181 edits `MockRuntimeEngine.java` (see Dependencies and Sequencing).

## Objective

Two small residues of the shared contract and its mock stay open because no plan owns them. First, `EpicProcessPlan` documents a plan's `phase_state` as "running", "staged", "planned" or "done" (`EpicProcessPlan.java:26`), while the closed vocabulary of a plan's activity is `PlanActivity` (`running`, `pausing`, `paused`, `waiting_for_you`, `held`, `staged`, `ended`), the scenarios serve nothing else, and `PlanActivity.parse("done")` throws. A reader trusting the Javadoc writes a state the contract refuses. Second, the mock's epic action handler ignores what the request says: `cancel` with `with_plans` ends the epic and leaves its plans as they were, and `next` falls to the default branch ("the epic rests between its events"), so a client that acts on an epic sees a success that changed nothing the next read can show. The result: the vocabulary is written down once and the mock does what the epic action answers claim, or the answer says it did not.

## Deliverables

Read first: the two `epic.md` entries (Open Defects: "`EpicProcessPlan` documents `phase_state`…" and "Mock epic-action routes (PLAN-10 residue)…") and `doc/design/shared/work-actions.adoc` (`#cancel`, the epic's actions).

1. **A plan's phase state is the closed activity vocabulary**
 - What:
   - `EpicProcessPlan.java:26` names the closed vocabulary of `PlanActivity` (`running`, `pausing`, `paused`, `waiting_for_you`, `held`, `staged`, `ended`) and not "planned" or "done". The mock's `STATE_DONE` (`MockRuntimeEngine.java:518`) is classified at the re-read: `:12214` (`phaseNodes`) and `:12289` (`phaseNodeState`) yield a phase NODE state, and `NodeState` has `DONE` and `PLANNED` (`NodeState.java:18,22`), so they stay; `:1107` (`revisionWindowOpen`) compares a plan's `phase_state` with done/ended/merged and reads the vocabulary the plan changes to; `:12135` (`planStateOf`) maps an epic row's shipped/merged/landed status to `"done"`, which `planRowSummary` (`:12125`) and `synthesizedPlan` (`:12184-12197`, `copy.put(KEY_PHASE_STATE, ...)`) serve as a plan `phase_state`, a value `PlanActivity.parse` refuses. That site is the mock's defect and changes to the vocabulary (`ended` for a landed row, read at the outline against the plan's own end state `STATE_ENDED`, `:5937`).
   - Whether the record validates `phase_state` against `PlanActivity` on read (today it is a free string, `EpicProcessPlan.java:79`) is decided at outline from the engine's contract: a closed set on the wire is checked in the reader like every other closed set in `pm-api`; if the engine's catalogue names more states than `PlanActivity` has, that is reported in the pull request and the landing message and not decided here.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:javadoc, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - `ProcessRecordsReadingTest.java` (Generators, `@EnableGeneratorController`): a record with each `PlanActivity` wire value reads, and a value outside the set is refused (if the reader validates, per the outline's decision) or the Javadoc names exactly the set (if not); the test fails with its production line reverted; the existing `ProcessRecordsReadingTest.java:244-247` feeds `"phase_state":"done"` and asserts it is kept, and changes with the decision (a refusal test, or an updated value). A test of the mock proves `synthesizedPlan` no longer serves `done`. Evidence order: `test-first`.
2. **The mock's epic `cancel` and `next` do what they answer**
 - What:
   - `cancel` with `with_plans` set (`PlanActionRequest.withPlans`, `PlanActionRequest.java:29,37`): the epic ends (as today) and, where `with_plans` is true, its plans end too and the plans' own reads and events show it; where it is false or absent the plans are left as they are (`MockRuntimeEngine.java:5983` writes only `controlEpicStates`).
   - `next` (`ACTION_NEXT`, offered in `buildEpicActions` at `:5665`, handled by the `default` branch of `handleEpicAction` at `:5984-5986`): the mock either starts the epic's next staged row, with the event the clients react to, or the answer says nothing started. The engine's contract is settled by the re-read (2026-11-23): `pm-mcp-core` `doc/specification/model-work/08-epic-coordination.adoc:228` (cancel: "cancel them too, each with its own outcome `cancelled` ..., or let them run on detached") and `:223` (next plan: starts the next launchable row now, its plan is created; with no launchable row it answers why, per row: `dependency`, `lock`, `scope`, `overlap`, `stale`, `held`); the daemon specification lists `next` (`raise_scope`) and `cancel` (`reason`, `with_plans`) (`01-processes-and-transport.adoc:397`); the client design says the same (`doc/design/shared/work-actions.adoc:222,262-273`). The mock does what these say, and a refinement that needs an engine decision (the plan's `phase_state` after a cancel is named by neither document; the plan's own cancel writes `STATE_ENDED`, `:5937`) is reported in the pull request and the landing message, never decided silently.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - `ControlScenarioTest.java`: an epic `cancel` with `with_plans: true` shows its plans ended on the next read and `with_plans: false` leaves them; `next` shows what the outline settled (a started row with its event, or an answer that says none started); each fails with its production line reverted. Evidence order: `test-first`.

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`): `./mvnw -pl pm-api,pm-mock-runtime verify`, then the full `./mvnw verify` and `./mvnw verify -Ppre-commit` (review every rewrite) at the head; `-Pcoverage` at the head (minimum 80 % per module).
- No new dependency or plugin. Code standards of `CLAUDE.md`: Java 25, no generic `Exception` in production code, `CuiLogger` through `LogMessages`, JUnit 5 only (no Mockito, PowerMock or Hamcrest), input test data from `Generators` with `@EnableGeneratorController` (the expected value of a contract stays literal). Pre-1.0: no shim.
- A test is fed with the values the wire sends; every new or rewritten assertion is shown to fail with the production line reverted, the failing run cited; a `test-first` clause cites a failing run at the plan's start.
- No scenario JSON, wire name or catalogue text changes beyond what a deliverable names; a fix that would change another behaviour is reported, not made.
- A finding that needs an engine or design decision (the closed set of the phase state, what the epic `next` does) is reported in the pull request and the landing message, never decided silently.
- The merge rule: the run merges only when every review thread has a reply and is resolved and every review body is answered, and some head has had one CodeRabbit review. The pull request body states thread count, resolved count and review-body items from `ci pr comments`, never from memory. The run merges the pull request itself once these gates hold (`CLAUDE.md`, Git Workflow). Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`; the pull request body carries no generator footer. If CodeRabbit refuses on quota, stop and ask the operator.
- ZERO Sonar findings on the new code (the operator, 2026-11-05); state `sonar_open_new_issues=N` in the landing message (`unknown` for a failed read, never `0`).
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch. No port is needed: no browser check.

## Claim Labels

- OBSERVED: `EpicProcessPlan` documents `phase_state` as "running", "staged", "planned", "done" — read at `pm-api/src/main/java/de/planmarshall/api/process/EpicProcessPlan.java:26`
  - verdict: corroborated | checked_at: a1e33f94673381bc9543082e0cf637208472ecf1 | by: ui-improvements/analyze | rescoped: n/a | evidence: EpicProcessPlan.java:26 Javadoc phase_state running/staged/planned/done and :79 free string, no change since #530 (origin/main a1e33f94)
- OBSERVED: `PlanActivity` has seven constants and `parse` throws for any other value, so "done" and "planned" are refused — read at `pm-api/src/main/java/de/planmarshall/api/overview/PlanActivity.java:21-27,63-69`
  - verdict: corroborated | checked_at: a1e33f94673381bc9543082e0cf637208472ecf1 | by: ui-improvements/analyze | rescoped: n/a | evidence: PlanActivity.java (path api/overview/, not api/process/) seven constants :21-27, parse throws Unknown PlanActivity :63-69 (origin/main a1e33f94)
- OBSERVED: the scenarios serve `phase_state` values `running`, `paused`, `waiting_for_you`, `staged` and `ended` only — read by `grep phase_state pm-mock-runtime/src/main/resources/scenarios/*.json`
  - verdict: corroborated | checked_at: a1e33f94673381bc9543082e0cf637208472ecf1 | by: ui-improvements/analyze | rescoped: n/a | evidence: grep phase_state scenarios/*.json: running 33, paused 6, waiting_for_you 5, staged 2, ended 1; no done, no planned (origin/main a1e33f94)
- OBSERVED: the mock's epic action handler writes `controlEpicStates` for `cancel` and ignores `actionRequest.withPlans()`; `next` falls to the default branch — read at `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java:5983-5986`
  - verdict: corroborated | checked_at: a1e33f94673381bc9543082e0cf637208472ecf1 | by: ui-improvements/analyze | rescoped: n/a | evidence: MockRuntimeEngine.java:5983 case ACTION_CANCEL -> controlEpicStates.put(epicId, STATE_ENDED) with no use of withPlans(); :5984-5986 default covers next; handleEpicAction :5951-5994 (origin/main a1e33f94)
- OBSERVED: the mock's `STATE_DONE` serves a plan's `phase_state` of `"done"` at `MockRuntimeEngine.java:12135` (`planStateOf` via `synthesizedPlan`), while `:12214` and `:12289` yield a phase node state and `:1107` reads a plan state — read at the four sites
  - verdict: corroborated | checked_at: a1e33f94673381bc9543082e0cf637208472ecf1 | by: ui-improvements/analyze | rescoped: n/a | evidence: MockRuntimeEngine.java:12135 planStateOf maps shipped/merged/landed to done, served as phase_state by synthesizedPlan :12184-12197; :12214,:12289 are node states; :1107 reads a plan state (origin/main a1e33f94)
- OBSERVED: the engine's contract gives an epic `cancel` with `with_plans` and an epic `next` an effect on the epic's plans and rows — read at `../pm-mcp-core/doc/specification/model-work/08-epic-coordination.adoc:223,228` and the daemon specification's `01-processes-and-transport.adoc:397`
  - verdict: corroborated | checked_at: a1e33f94673381bc9543082e0cf637208472ecf1 | by: ui-improvements/analyze | rescoped: n/a | evidence: model-work/08-epic-coordination.adoc:228 cancel cancels the plans too or lets them run on, :223 next starts the next launchable row or answers why per row; 01-processes-and-transport.adoc:397 lists next and cancel with_plans (origin/main a1e33f94)
- Verify-first clause: before each deliverable re-read the named files at the then-current HEAD; PLAN-62 (staged; it does not name `EpicProcessPlan`, `phase_state` or `PlanActivity`; whether its outline edits them is unverified) may touch the same files. A refuted claim re-scopes the deliverable and is reported, never silently worked around.
  - verdict: unverifiable | checked_at: a1e33f94673381bc9543082e0cf637208472ecf1 | by: ui-improvements/analyze | rescoped: n/a | evidence: procedural verify-first clause; PLAN-180 and PLAN-181 landed, PLAN-62 does not name EpicProcessPlan, whether its outline edits it is unverified

## Re-Grounding 2026-11-23 (analyze, at `a1e33f94`)

PLAN-181 (#544, #547) refactored `MockRuntimeEngine.java` after this spec was staged, so every line number of it moved; all of them are replaced above. Findings of the re-read: (1) the `PlanActivity` path was `api/overview/`, not `api/process/` (the file has no `process/` twin); (2) the `STATE_DONE` hypothesis is refuted in part: `:12135` serves a plan `phase_state` of `done` (the defect), `:12214` and `:12289` are node states and stay, `:1107` reads a plan state; (3) the engine's contract is read (`model-work/08-epic-coordination.adoc:223,228`) and it promises both effects, so the epic `cancel` with `with_plans` and `next` are mock defects, not an engine gap; (4) `ControlScenarioTest.java` has a nested `Epic` class (`:265`) and no `with_plans` or epic-action assertion yet; `ProcessRecordsReadingTest.java:244-247` feeds `done`; (5) no other staged spec edits `EpicProcessPlan` (PLAN-09, PLAN-81 and PLAN-175 only name it); (6) nothing in `pm-api` changed `EpicProcessPlan`, `PlanActivity`, `PlanActionRequest` or `ProcessRecordsReadingTest` since staging.

## Expected Surface

- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/process/EpicProcessPlan.java` — present at HEAD a1e33f94
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/overview/PlanActivity.java` — present at HEAD a1e33f94
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/process/ProcessRecordsReadingTest.java` — present at HEAD a1e33f94
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/control/PlanActionRequest.java` — present at HEAD a1e33f94
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` — present at HEAD a1e33f94
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/ControlScenarioTest.java` — present at HEAD a1e33f94

## Dependencies and Sequencing

- Depends on: nothing shipped is missing. `PLAN-181` (#544, #547) and `PLAN-180` (#543) have landed; the line numbers of `MockRuntimeEngine.java` here are the re-read at `a1e33f94`.
- Overlaps with: `PLAN-62` (staged: its outline may confirm an edit of `EpicProcessPlan`; if it does, deliverable 1 is read again then), `PLAN-180` (landed: no shared file).
- Adjacent to: the engine's epic action rows (a finding if they name no effect).

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-183-pm-api-and-mock-hygiene-residue.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger write. The session reports its outcome through its PR and its inbox message (`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary).
