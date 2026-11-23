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
   - `EpicProcessPlan.java:26` names the closed vocabulary of `PlanActivity` (`running`, `pausing`, `paused`, `waiting_for_you`, `held`, `staged`, `ended`) and not "planned" or "done". The mock's own use of `STATE_DONE` (`MockRuntimeEngine.java:483,1072,11892,11971`) is read first: where it is a phase NODE state it stays, where it is a plan's `phase_state` it is a defect of the mock and is changed to the vocabulary.
   - Whether the record validates `phase_state` against `PlanActivity` on read (today it is a free string, `EpicProcessPlan.java:79`) is decided at outline from the engine's contract: a closed set on the wire is checked in the reader like every other closed set in `pm-api`; if the engine's catalogue names more states than `PlanActivity` has, that is reported in the pull request and the landing message and not decided here.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:javadoc, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - `ProcessRecordsReadingTest.java` (Generators, `@EnableGeneratorController`): a record with each `PlanActivity` wire value reads, and a value outside the set is refused (if the reader validates, per the outline's decision) or the Javadoc names exactly the set (if not); the test fails with its production line reverted. Evidence order: `test-first`.
2. **The mock's epic `cancel` and `next` do what they answer**
 - What:
   - `cancel` with `with_plans` set (`PlanActionRequest.withPlans`, `PlanActionRequest.java:29,37`): the epic ends (as today) and, where `with_plans` is true, its plans end too and the plans' own reads and events show it; where it is false or absent the plans are left as they are (`MockRuntimeEngine.java:5866` writes only `controlEpicStates`).
   - `next` (`ACTION_NEXT`, offered at `:5606`, handled by the `default` branch at `:5867-5870`): the mock either starts the epic's next staged row, with the event the clients react to, or the answer says nothing started. Which one the engine's contract means is read first (`pm-mcp-core` state catalogue and the daemon specification's epic action row, verify-at-outline); a finding that needs an engine decision is reported in the pull request and the landing message, never decided silently.
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
  - verdict: corroborated | checked_at: fcf877285a08c0d442fc3f07bfcb5f5515088ea9 | by: ui-improvements/analyze | rescoped: n/a | evidence: EpicProcessPlan.java:26 @param phaseState: activity state ("running", "staged", "planned", "done") (origin/main fcf87728)
- OBSERVED: `PlanActivity` has seven constants and `parse` throws for any other value, so "done" and "planned" are refused — read at `pm-api/src/main/java/de/planmarshall/api/process/PlanActivity.java:20-27,66-69`
  - verdict: corroborated | checked_at: fcf877285a08c0d442fc3f07bfcb5f5515088ea9 | by: ui-improvements/analyze | rescoped: n/a | evidence: PlanActivity.java:20-27 seven constants (running, pausing, paused, waiting_for_you, held, staged, ended); parse throws IllegalArgumentException 'Unknown PlanActivity' at :66-69 (origin/main fcf87728)
- OBSERVED: the scenarios serve `phase_state` values `running`, `paused`, `waiting_for_you`, `staged` and `ended` only — read by `grep phase_state pm-mock-runtime/src/main/resources/scenarios/*.json`
  - verdict: corroborated | checked_at: fcf877285a08c0d442fc3f07bfcb5f5515088ea9 | by: ui-improvements/analyze | rescoped: n/a | evidence: grep phase_state over scenarios/*.json: running 33, paused 6, waiting_for_you 5, staged 2, ended 1; no done, no planned (origin/main fcf87728)
- OBSERVED: the mock's epic action handler writes `controlEpicStates` for `cancel` and ignores `actionRequest.withPlans()`; `next` falls to the default branch — read at `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java:5866-5870`
  - verdict: corroborated | checked_at: fcf877285a08c0d442fc3f07bfcb5f5515088ea9 | by: ui-improvements/analyze | rescoped: n/a | evidence: MockRuntimeEngine.java:5866 case ACTION_CANCEL -> controlEpicStates.put(epicId, "ended") with no use of actionRequest.withPlans(); :5867-5870 default branch comment 'the epic rests between its events' covers next (origin/main fcf87728)
- HYPOTHESIS: the mock's `STATE_DONE` at `MockRuntimeEngine.java:483,1072,11892,11971` is a phase node state and not a plan's `phase_state` — confirm/refute by reading the four sites (verify-at-outline)
  - verdict: unverifiable | checked_at: fcf877285a08c0d442fc3f07bfcb5f5515088ea9 | by: ui-improvements/analyze | rescoped: n/a | evidence: STATE_DONE sites at :483,1072,11892,11971 not individually classified here; :11971 reads STATE_DONE.equals(phaseState), settled at outline
- HYPOTHESIS: the engine's contract gives an epic `cancel` with `with_plans` and an epic `next` an effect on the epic's plans and rows — confirm/refute in `../pm-mcp-core/doc/specification/state-catalogue.adoc` and the daemon specification's epic action row (verify-at-outline; if it names none, report it)
  - verdict: unverifiable | checked_at: fcf877285a08c0d442fc3f07bfcb5f5515088ea9 | by: ui-improvements/analyze | rescoped: n/a | evidence: pm-mcp-core state catalogue and the daemon's epic action row not read for cancel with_plans and next here; settled at outline
- Verify-first clause: before each deliverable re-read the named files at the then-current HEAD; PLAN-181 (`MockRuntimeEngine.java`) and PLAN-62 (staged; its outline confirms whether it edits `EpicProcessPlan`) touch the same files. A refuted claim re-scopes the deliverable and is reported, never silently worked around.
  - verdict: unverifiable | checked_at: fcf877285a08c0d442fc3f07bfcb5f5515088ea9 | by: ui-improvements/analyze | rescoped: n/a | evidence: procedural verify-first clause; PLAN-181 running, PLAN-62 staged

## Expected Surface

- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/process/EpicProcessPlan.java` — present at HEAD fcf87728
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/process/PlanActivity.java` — present at HEAD fcf87728
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/process/ProcessRecordsReadingTest.java` — present at HEAD fcf87728
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/control/PlanActionRequest.java` — present at HEAD fcf87728
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` — present at HEAD fcf87728
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/ControlScenarioTest.java` — present at HEAD fcf87728

## Dependencies and Sequencing

- Depends on: nothing shipped is missing. Sequenced AFTER `PLAN-181` (running: it edits `pm-mock-runtime`, the largest part `MockRuntimeEngine.java`); emit it when PLAN-181 has landed.
- Overlaps with: `PLAN-62` (staged: its outline may confirm an edit of `EpicProcessPlan`; if it does, deliverable 1 is read again then), `PLAN-180` (running: `pm-api` tests, no shared file).
- Adjacent to: the engine's epic action rows (a finding if they name no effect).

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-183-pm-api-and-mock-hygiene-residue.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger write. The session reports its outcome through its PR and its inbox message (`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary).
