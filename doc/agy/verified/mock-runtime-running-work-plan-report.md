# Completion Report: `mock-runtime-running-work`

**Status:** Implemented, landed on `main` (commit `49d0ef6`, PR #136), and verified under [`doc/meta/verification-protocol.md`](../../meta/verification-protocol.md) (Step 8 PASSED)
**Plan**: [`mock-runtime-running-work-plan.md`](../implemented/mock-runtime-running-work-plan.md)
**Branch**: `feat/mock-runtime-running-work`
**Worktree**: `/Users/oliver/git/worktrees/pm-mcp-clients-mock-running-work`
**Governed by:** [`doc/agy/runbook.md`](../runbook.md) and [`doc/meta/verification-protocol.md`](../../meta/verification-protocol.md)
**Date**: 2026-10-20

---

## 1. Executive Summary

Scenario 2 of the five-scenario mock-runtime architecture is consolidated. The 16 fragmented
`plan-view`, `intent-*`, `decisions`, `decomposition`, and `control*` scenario files are gone; their
cases now live as **38 states and 20 routes in one `running-work.json`**.

The consolidation is not a file merge only:

1. Every state is a complete `OperatorStateResource` (`scope`, `state.state_id`, `facts`, `links`)
   decoded by the `pm-api` codec, and every scheduled event is a complete `EventEnvelope` of the
   closed `EventName` catalogue. The loader's strict operator validation runs over the whole file.
2. The stateful fixtures the engine used to hardcode in Java — plans and their control states, the
   epic resources, holds, plan settings, chats, findings/watches reviews, archive checks, the open
   decisions and their quorum votes, and the epic decompositions — are now declared once in the
   base state's `facts.runtime` fixture and seeded through the existing `pm-api` codecs. A case
   state declares only what differs and inherits the rest.
3. A case state can override any route through `facts.runtime.routes`, so the overview, process,
   activity, and token cases are selected by the state a client starts in (`running-work#<state>`)
   rather than by another scenario file.
4. The legacy scenario constants keep working through the pre-registered `ScenarioRegistry` alias
   table; each resolves to the consolidated state its case lives in.

The overview contract, the process/intent contract, the decision contract, and the control contract
are each covered by a suite that asserts the representation a client receives.

---

## 2. Delivered Artifacts

### 2.1 Scenario data (`pm-mock-runtime`)

| Artifact | Change |
|---|---|
| `src/main/resources/scenarios/running-work.json` | Rewritten: 38 `OperatorStateResource` states and 20 routes; the complete control, decision, and decomposition fixture in `running_work.active`; per-case route overrides. |
| `plan-view.json` | Deleted; merged into `running-work.json`. |
| `intent-plan.json`, `intent-outline.json`, `intent-epic.json`, `intent-oversize.json` | Deleted; their routes merged. |
| `decisions.json`, `decomposition.json` | Deleted; their fixtures and routes merged. |
| `control.json`, `control-pause.json`, `control-holds.json`, `control-settings.json`, `control-epic.json`, `control-decomposition.json`, `control-chat.json`, `control-review.json`, `control-tokens.json` | Deleted; their fixtures and routes merged. |

The consolidated state families are:

- **Overview**: `overview.running`, `overview.empty`, `overview.setup-only`, `overview.plan-ending`
  (with every ended outcome: `merged`, `no_change`, `handed_back`, `aborted`, `cancelled`,
  `abandoned`, `released`, and the epic reasons `completed`, `cancelled`, `released`).
- **Process and activity**: `process.running`, `process.phase.init`, `.refine`, `.outline`, `.plan`,
  `.execute`, `.finalize`, `process.loopback`, `process.nested-3-deep`, `process.pruned-lane`,
  `process.new-version`, `process.failed-tool`, `process.off-track`, `process.ended-task`.
- **Tokens**: `process.tokens-complete`, `process.tokens-partial` (a floor with its coverage),
  `process.tokens-none`, `process.tokens-not-measured` (no total and no floor).
- **Intent**: `intent.ready` (the plan intent, its outline, the epic intent, and the `over_size`
  spread).
- **Decisions**: `decisions.open`, `decisions.answered-elsewhere` (`decision.answered` +
  `decision.closed`), `decisions.withdrawn` (`decision.changed`, re-asked as `Q-009`).
- **Control**: `control.ready`, `control.pause`, `control.pause-withdrawn`, `control.holds`,
  `control.settings`, `control.epic`, `control.decomposition`, `control.chat`, `control.review`,
  `control.tokens`, `control.archive`.

The decision fixture covers plan, epic, project-setup, and machine scopes; blocking and
non-blocking questions; single-part and three-part questions; all three reversibilities; with and
without a recommendation; the two waivers; and the six quorum-vote models.

### 2.2 Engine (`pm-mock-runtime`)

| File | Change |
|---|---|
| `src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` | `activeRoute` resolves a state's `facts.runtime.routes` override before the scenario route; `startRuntimeFixture` merges a case state over the scenario's base fixture; `initDefaultControl`, `initDefaultDecisions`, and `initDefaultDecomposition` seed the stateful maps through the `pm-api` codecs instead of Java literals; a plan setting changed after a plan ended is refused as too late. |

### 2.3 Tests (`pm-mock-runtime`)

| File | Coverage |
|---|---|
| `RunningWorkScenarioTest.java` | Rich workspace, paused plan out of the attention badge, worker off track, alongside landings, ended-work pagination, SSE broadcast, empty machine, setup-only project, plan ending with every outcome, fallback routes. |
| `PlanViewScenarioTest.java` | Process tree with path/project/epic/tokens, pruned lane, each of the six phases, loop-back, nested three levels, new instance version, live and ended activity, failed tool, off-track signal, activity stream, complete/partial/none/not-measured tokens, epic process, dynamic routes. |
| `IntentScenarioTest.java` | Plan intent with self-review and builder check, outline with a missing key link, epic intent, `over_size` distribution, contract-shaped fixtures, every consolidated state decoding as an `OperatorStateResource`. |
| `DecisionsScenarioTest.java` | Open list and filters, blocking decision and quorum, every scope/reversibility, non-blocking question, multi-part question, all five own-words interpretations, waiver reason enforcement, answer, later, withdrawal and replacement, answered-elsewhere and withdrawn states and their events, runtime-added decision. |
| `DecompositionScenarioTest.java` | Draft, control view, surface-overlap check, dependency cycle, instruct + read-back, commit and launchable broadcast, add instruction, 404s. |
| `ControlScenarioTest.java` | Plan states and actions, pause at now/task/phase and withdrawal, cancel/abandon/held end, integrity pause retry/accept/abort, overlap hold release, settings revision guard/conflict/refusal/too-late, epic resource and waits, archive disabled while landing, automatic switched by name, manual epic review first, tokens, chats and proposals, review dispositions and default register, 404s. |
| `StandardScenariosTest.java` | Adapted the Wave 1 control-resource assertion to the consolidated catalog: no `control*.json` remains and every `control*` constant resolves to `running-work`. |

### 2.4 Operator transcript (`pm-operator`)

| File | Change |
|---|---|
| `src/test/resources/transcript/status.transcript` | Re-recorded for the consolidated workspace (Fix Relay Transport, Drop the legacy endpoints, Relay metrics). The change is intentional and was reviewed against the rendered output. |

---

## 3. Test & Verification Results

| Gate | Command | Result |
|---|---|---|
| Consolidated contract suites | `./mvnw -pl pm-mock-runtime test -Dtest='RunningWorkScenarioTest,PlanViewScenarioTest,IntentScenarioTest,DecisionsScenarioTest,DecompositionScenarioTest,ControlScenarioTest,StandardScenariosTest'` | **80 tests, 0 failures** |
| Full mock-runtime suite | `./mvnw -pl pm-mock-runtime test` | **374 tests, 0 failures** |
| Operator start dialogues | `./mvnw -pl pm-operator test -Dtest=StartDialogueTest` | **37 tests, 0 failures** |
| Pre-commit gate | `./mvnw verify -Ppre-commit` | **BUILD SUCCESS** across all eight reactor modules (8:14 min) |

Focused evidence:

- `RunningWorkScenarioTest.emptyMachine` proves the empty-machine state serves no project, plan, or
  decision and that the fixture merge lets the case state empty the base fixture.
- `PlanViewScenarioTest.nestedThreeDeep` proves the current position path walks
  `execute → execute.envelope → execute.envelope.remediation → execute.envelope.remediation.fix →
  fix.apply`.
- `PlanViewScenarioTest.notMeasured` proves the `not_measured` state carries no total and no floor.
- `DecisionsScenarioTest.withdraws` proves the replacement (`Q-009`) survives the withdrawal of
  `Q-007`.
- `ControlScenarioTest.revisionGuard` proves conflict behaviour, a successful apply (revision 8), a
  refused value, and the too-late refusal after the plan ended.

---

## 4. Domain Invariants

| Invariant | Status |
|---|---|
| Closed scenario schema and `pm-api` codec validation | Enforced by the loader; the whole file loads cleanly. |
| Every state is an `OperatorStateResource` | Asserted for all 38 states (`IntentScenarioTest.everyStateDecodes`). |
| Every scheduled event is an `EventEnvelope` of the closed catalogue | Asserted by the loader; `decisions.*` and `process.*` events are built as envelopes. |
| No hardcoded control/decision/decomposition fixtures in Java | Removed; the maps are seeded from `facts.runtime` through `EpicResource`, `ArchiveCheck`, `ReviewResource`, `PlanSettingsRecord`, `HoldRecord`, `ChatReadBackResource`, `DecisionEnvelope`, and `EpicDecomposition`. |
| Java 25 toolchain and module boundaries | Preserved; no new module dependency, no change to `pm-api` or the parent POM. |
| Legacy scenario names keep resolving | Asserted by `StandardScenariosTest` and `ScenarioRegistryTest`. |
| Deterministic tests, no real daemon/harness | All tests use the in-process `MockRuntime`. |

---

## 5. Decisions, Boundaries, and Follow-Ups

1. **Alias targets.** Every legacy name of this wave resolves to
   `running-work#running_work.active`, which carries the complete base fixture. A test or a
   developer selects a case with `running-work#<state>`; the launcher and the five-scenario catalog
   are untouched.
2. **`StandardScenariosTest` adaptation.** The Wave 1 test compared the `control*` constants against
   the `control*.json` files. The plan assigns `StandardScenariosTest` to Wave 3, but deleting the
   files would have left that assertion red, so the method was narrowed to its new invariant: no
   control resource remains and every control constant resolves to `running-work`. Wave 3 can
   replace it with the five-file assertion.
3. **Start scenarios and the decomposition.** `projects-and-start` starts an epic and then shows the
   canonical draft, which the engine previously supplied from a global Java literal. The engine now
   falls back to the declarative fixture of `running-work#running_work.active` when a scenario (such
   as `projects-and-start`) declares no decomposition of its own. Wave 3 (`projects-and-start`) can
   move that fixture into its own scenario when it migrates `StartRegistry`.
4. **Decomposition remove/reorder.** The closed `InstructAction` vocabulary is `edit`, `add`, and
   `rewrite`; a remove or reorder instruction is not expressible in the contract, so the mock does
   not invent one. Add, edit, and rewrite (with read-back and commit) are covered.
5. **OpenRewrite noise.** The pre-commit profile rewrites record-component indentation in every
   `pm-api` and some `pm-operator` files. The gate was run to prove the change, and those unrelated
   rewrites were reverted to keep the plan's module boundaries, as Wave 1 did. The two files in
   `pm-mock-runtime` were reformatted by the same recipe only where this change removed imports.
6. **Speckled text drift.** The status transcript is the only operator fixture affected; it was
   re-recorded and reviewed. No production code in `pm-operator` was touched.
7. **Follow-up.** Wave 3 owns the final `StandardScenariosTest` (exactly five `.json` files) and may
   remove the transitional decomposition fallback once `StartRegistry` reads its fixtures from the
   state.

---

## 6. Build & Quality Evidence

```
[INFO] Reactor Summary for pm-mcp-clients 0.1.0-SNAPSHOT:
[INFO] pm-mcp-clients ..................................... SUCCESS [  9.431 s]
[INFO] pm-api ............................................. SUCCESS [01:24 min]
[INFO] pm-exec ............................................ SUCCESS [ 11.472 s]
[INFO] pm-mock-runtime .................................... SUCCESS [01:43 min]
[INFO] pm-relay ........................................... SUCCESS [ 29.155 s]
[INFO] pm-operator ........................................ SUCCESS [03:07 min]
[INFO] pm-web-app ......................................... SUCCESS [ 52.972 s]
[INFO] pm-web-e2e ........................................ SUCCESS [ 15.060 s]
[INFO] BUILD SUCCESS
[INFO] Total time:  08:14 min
```

After the gate, `git status` names only the artifacts listed in Section 2 (the intended scenario
deletions, `running-work.json`, the engine change, the six contract suites, the one adapted catalog
test, and the one re-recorded transcript).

No temporary diagnostics, commented-out code, or generated artifacts remain. The temporary
fixture-dump test used to capture the wire shapes was deleted before the gate.

---

## 7. Post-Landing Verification Audit (Step 8: PASSED)

In accordance with [`doc/meta/verification-protocol.md`](../../meta/verification-protocol.md), the orchestrator session conducted an active, independent 4-pillar verification audit of PR #136 landed at commit `49d0ef6` on `main`:

### Pillar 1: Specification & Contract Conformance Matrix (Traceability Audit)
- **Scope Verification**: Delivered all 38 `OperatorStateResource` states and 20 routes in `pm-mock-runtime/src/main/resources/scenarios/running-work.json`, consolidating 16 fragmented scenario files across overview, process view, intent, decisions, and control.
- **Placeholder & Stub Audit**: Zero stubs, temporary mock branches, `UnsupportedOperationException`, or `TODO`/`FIXME` markers in production code. Clean production AST.
- **Closed Wire Catalogues**: All scheduled events use `EventEnvelope` with the closed `EventName` catalogue; all state definitions decode cleanly through `pm-api` codecs (`EpicResource`, `ArchiveCheck`, `ReviewResource`, `PlanSettingsRecord`, `HoldRecord`, `ChatReadBackResource`, `DecisionEnvelope`, `EpicDecomposition`).

### Pillar 2: Architectural & Code Hygiene Anti-Pattern Scans
- **Standard Scenarios Architecture**: 16 legacy micro-scenarios completely removed; all legacy names resolve through `ScenarioRegistry` alias table to `running-work#running_work.active`.
- **Closed Schema & Codec Enforcement**: `MockRuntimeEngine` and `ScenarioLoader` strictly validate all 38 states against `pm-api` schema; unknown properties fail closed.
- **Dynamic Route & Fixture Overrides**: Route overrides in `facts.runtime.routes` resolve before scenario-level routes; case states cleanly merge over the base fixture.
- **SonarCloud Gate & Code Quality**: Passed cleanly with 90.3% coverage on new code and 0.0% duplication.

### Pillar 3: Subprocess Lifecycle & Cross-Module Handshake Pre-Flight
- **Consolidated Contract Suites**: Executed `./mvnw test -pl pm-mock-runtime -Dtest='RunningWorkScenarioTest,PlanViewScenarioTest,IntentScenarioTest,DecisionsScenarioTest,DecompositionScenarioTest,ControlScenarioTest,StandardScenariosTest'`: **80 tests run, 0 failures, 0 errors, 0 skipped** (`BUILD SUCCESS`).
- **Full Mock-Runtime Suite**: Executed `./mvnw test -pl pm-mock-runtime`: **374 tests run, 0 failures, 0 errors, 0 skipped** (`BUILD SUCCESS`).
- **Cross-Module Operator Start Dialogues**: Executed `./mvnw test -pl pm-operator -Dtest=StartDialogueTest`: **37 tests run, 0 failures, 0 errors, 0 skipped** (`BUILD SUCCESS`).
- **Subprocess Scripts**: Verified `start.sh` interactive selector correctly exposes `2) running-work` with multi-state description.

### Pillar 4: Independent Diff & AST Verification (Adversarial Audit)
- **Attribution & Metadata**: PR #136 merged into `main` at commit `49d0ef6` by GitHub merge queue. Commit message verified with mandatory trailer:
  `Co-Authored-By: plan-marshall <noreply@cuioss.de>`
- **Boundary Discipline**: Changes strictly confined to `pm-mock-runtime`, `status.transcript` in `pm-operator`, and `doc/agy/`. No unintended drifts.

### Verification Verdict
**PASSED** — All 4 pillars satisfied with complete technical evidence. Report archived to `doc/agy/verified/mock-runtime-running-work-plan-report.md`.
