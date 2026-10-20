# Implementation Plan: `mock-runtime-running-work` (Scenario 2: Consolidated `running-work.json` for Overview, Process View, Decisions & Control)

This implementation plan governs **Scenario 2 (`running-work.json`)** of the 5-scenario mock-runtime architecture: consolidating `running-work.json`, `plan-view.json`, `decisions.json`, `decomposition.json`, and the 9 `control*.json` stub files into a single, comprehensive, multi-state `running-work.json` scenario covering all contract cases for `overview-contract`, `process-contract`, `decision-contract`, and `control-contract`.

It operationalizes [`doc/meta/implement_use-cases.md`](../meta/implement_use-cases.md) under the execution discipline of [`doc/agy/runbook.md`](runbook.md).

---

## 1. Traceability & Context

| Dimension | Reference |
|---|---|
| **Use Cases** | [`UC-PLN-1..6`](../../../plan-marshall-documentation/doc/use-cases/03-plans.adoc), [`UC-DEC-1..3`](../../../plan-marshall-documentation/doc/use-cases/05-decisions.adoc), [`UC-CTL-1..7`](../../../plan-marshall-documentation/doc/use-cases/06-controlling-work.adoc) |
| **Specifications** | [`05-mock-runtime.adoc`](../../../plan-marshall-documentation/doc/specification/runtime-model/05-mock-runtime.adoc), [`04-operator-api.adoc`](../../../plan-marshall-documentation/doc/specification/runtime-model/04-operator-api.adoc) |
| **Local Client Design** | [`doc/design/shared/overview.adoc`](../design/shared/overview.adoc), [`doc/design/shared/process-view.adoc`](../design/shared/process-view.adoc), [`doc/design/shared/decisions.adoc`](../design/shared/decisions.adoc), [`doc/design/shared/controlling-work.adoc`](../design/shared/controlling-work.adoc) |
| **Roadmap Packages** | [`doc/plans/implemented/overview-contract.adoc`](../plans/implemented/overview-contract.adoc), [`doc/plans/implemented/process-contract.adoc`](../plans/implemented/process-contract.adoc), [`doc/plans/implemented/decision-contract.adoc`](../plans/implemented/decision-contract.adoc), [`doc/plans/implemented/control-contract.adoc`](../plans/implemented/control-contract.adoc) |
| **Prerequisite Plan** | [`doc/agy/mock-runtime-foundation-and-consolidation-plan.md`](mock-runtime-foundation-and-consolidation-plan.md) |
| **Implementation Recipe** | [`doc/meta/implement_use-cases.md`](../meta/implement_use-cases.md) |
| **Operational SOP** | [`doc/agy/runbook.md`](runbook.md) |

---

## 2. Scope & Objectives

1. **Consolidate 14 Work/Process/Intent/Decision/Control Files into Scenario 2 (`running-work.json`)**:
   - Merge `running-work.json`, `plan-view.json`, `intent.json` (landed by `intent-views`), `decisions.json`, `decomposition.json`, `control.json`, `control-pause.json`, `control-holds.json`, `control-settings.json`, `control-epic.json`, `control-decomposition.json`, `control-chat.json`, `control-review.json`, and `control-tokens.json` into **`pm-mock-runtime/src/main/resources/scenarios/running-work.json`**.
   - Delete the 13 superseded JSON files from `src/main/resources/scenarios/` (using the legacy alias table pre-registered in `ScenarioRegistry` by `mock-runtime-foundation-and-consolidation-plan.md`).
2. **Replace `{"status": "ready"}` Stubs with Declarative `OperatorStateResource` States & Routes**:
   - Ensure every state in `running-work.json` carries a valid `OperatorStateResource` (`scope`, `state`, `summary`, `links`, `data`) and every scheduled SSE event is a valid `EventEnvelope`.
   - Move hardcoded Java fixtures in `MockRuntimeEngine` (decisions, process views, intent/outline views, control settings, holds, chat read-backs, review findings, epic decomposition) into declarative `routes` and state `data` in `running-work.json`.
3. **Complete All Missing Contract Cases Across Overview, Process View, Intent, Decisions & Control**:
   - **Overview cases (`overview-contract.adoc`)**:
     - `overview.running` (default rich multi-project workspace with attention badges, epics, plans, alongside jobs, paused plan excluded from attention badge, worker off track)
     - `overview.empty` (onboarded machine with no projects/work)
     - `overview.setup-only` (project with setup question only)
     - `overview.plan-ending` (selected plan transitioning to ended with every outcome: landed, nothing_to_land, aborted, failed)
   - **Process & Intent View cases (`process-contract.adoc`, `intent-views.adoc`)**:
     - Routes and states for plans in each phase (`init`, `refine`, `outline`, `plan`, `execute`, `finalize`), intent and outline records with criterion results and `over_size` spread, phase loop-back, sub-workflow nested 3 levels deep, branch not taken, pruned lane, new workflow instance version arriving while open, live task activity growing via `task.activity` SSE events, ended task activity, failed tool call, worker off track, and all token accounting states (`complete`, `partial`, `not_reported`, `none_yet`, live rising).
   - **Decision cases (`decision-contract.adoc`)**:
     - Blocking and non-blocking decisions, single-part and multi-part questions, every reversibility (`easy`, `costly`, `one_way`) and subject, with and without recommendation, own-words interpretation outcomes (`option`, `option_with_condition`, `own_option`, `unclear`, `delegation`), waiver with required reason vs refused blank reason, decision answered from another client (`decision.answered` + `decision.closed`), and decision withdrawn / asked anew (`Q-007` -> `Q-009`).
   - **Control cases (`control-contract.adoc`)**:
     - Pause/resume (`now`, `after_task`, `after_phase`, withdraw pause), holds (`place`, `release`), plan settings revision guard, epic control (`auto`, capacity, add/remove/reorder plans), epic decomposition (`check`, `instruct`, `commit`), plan/epic chat (`instruct`, `accept`, `reject`), and review findings disposition.

---

## 3. Concurrency & Isolation

Depends on Wave 1 (`mock-runtime-foundation-and-consolidation`). Independent of Waves 2a, 2c, and 2d.

### Boundary Rules
- **Sole Owner of**: `pm-mock-runtime/src/main/resources/scenarios/running-work.json` (and deletion of `plan-view.json`, `intent.json`, `decisions.json`, `decomposition.json`, `control*.json`), plus `RunningWorkScenarioTest.java`, `PlanViewScenarioTest.java`, `IntentScenarioTest.java`, `DecisionsScenarioTest.java`, `DecompositionScenarioTest.java`, and `ControlScenarioTest.java`.
- **No Touch**: `ScenarioRegistry.java` / `StandardScenarios.java` (owned by `mock-runtime-foundation-and-consolidation-plan.md`), `doc/plans/README.adoc`, `pm-operator`, and `pm-web-app`.

---

## 4. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created at `/Users/oliver/git/worktrees/pm-mcp-clients-mock-running-work` on branch `feat/mock-runtime-running-work` off latest `main`.
- [x] 2. Domain skills & engineering personas loaded where applicable:
  - `persona-implementer` & `persona-module-tester` for code & unit tests
  - `persona-integration-tester` for multi-module/integration tests
  - `persona-security-expert` for trust boundaries & input validation
  - `persona-documenter` for documentation & ADRs
  - `persona-code-reviewer` for finding evaluation & adversarial refutation
- [x] 3. Incremental implementation with tests following the phased technical recipe.
- [x] 4. Final completion report written (`doc/agy/mock-runtime-running-work-plan-report.md`) and initial plan moved to `doc/agy/implemented/mock-runtime-running-work-plan.md`. (Do NOT move report to `verified/` — Step 8 is reserved for orchestrator).
- [x] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit` for code/config; skipped for standalone documentation).
- [x] 6. PR opened (NEVER use `skip-bot-review` on worker PRs; reserved for orchestrator docs PRs), review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [x] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [x] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/mock-runtime-running-work-plan-report.md` by orchestrator session.

---

## 5. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Overview, Process & Intent Fixtures** | `plan-marshall-persona-implementer` | `pm-dev-java:java-core` | Author declarative overview, process view, and intent states/routes in `running-work.json`. |
| **Phase 2: Decisions & Control States** | `plan-marshall-persona-implementer` | `pm-dev-java:java-core` | Consolidate decisions, decomposition, and all 8 control areas into `running-work.json`. |
| **Phase 3: Scenario Contract Tests** | `plan-marshall-persona-module-tester`<br>`plan-marshall-persona-integration-tester` | `pm-dev-java:junit-core` | Verify all overview, process, intent, decision, and control cases via `pm-api` codecs. |
| **Phase 4: Review Gate** | `plan-marshall-persona-code-reviewer` | `pm-dev-java:java-security` | Pre-commit gate and 100% resolution of automated review findings. |

---

## 6. Phased Technical Recipe

### Phase 1: Overview, Process View & Intent States and Routes in `running-work.json`
- [x] **1.1 Overview States & Routes**:
  - Populate `running-work.json` with `OperatorStateResource` states (`overview.running`, `overview.empty`, `overview.setup-only`, `overview.plan-ending`) and `GET /api/v1/overview` representations covering all 10 cases in `overview-contract.adoc`.
- [x] **1.2 Process View, Intent & Task Activity Routes**:
  - Merge `plan-view.json` and `intent.json` routes (`/api/v1/plans/{id}/process`, `/api/v1/plans/{id}/intent`, `/api/v1/plans/{id}/outline`, `/api/v1/epics/{id}/process`, `/api/v1/epics/{id}/intent`, and `/api/v1/plans/{id}/tasks/{taskId}/activity`) and states (`process.running`, `process.loopback`, `process.nested-3-deep`, `process.pruned-lane`, `process.new-version`, `process.failed-tool`, `process.off-track`, `process.tokens-partial`, `process.tokens-none`, `intent.ready`) in `running-work.json`, with `EventEnvelope` events for live task activity and token updates.

### Phase 2: Decisions, Decomposition & Control Consolidation
- [x] **2.1 Decision States & Routes**:
  - Consolidate `decisions.json` into `running-work.json` (`decisions.open`, `decisions.answered-elsewhere`, `decisions.withdrawn`) with full `DecisionEnvelope` fixtures covering blocking/non-blocking, single/multi-part, all reversibility levels, own-words interpretation, and waivers.
- [x] **2.2 Control & Decomposition States & Routes**:
  - Consolidate `decomposition.json` and the 9 `control*.json` files into `running-work.json` states (`control.ready`, `control.pause`, `control.holds`, `control.settings`, `control.epic`, `control.decomposition`, `control.chat`, `control.review`, `control.tokens`) and declarative routes, seeding `MockRuntimeEngine`'s stateful control maps from `running-work.json` routes rather than hardcoded Java literals.
  - Delete `plan-view.json`, `intent.json`, `decisions.json`, `decomposition.json`, and `control*.json`.

### Phase 3: Contract Verification Tests
- [x] **3.1 Expand Test Suites**:
  - Update `RunningWorkScenarioTest`, `PlanViewScenarioTest`, `DecisionsScenarioTest`, `DecompositionScenarioTest`, and `ControlScenarioTest` to assert every required case in `overview-contract.adoc`, `process-contract.adoc`, `decision-contract.adoc`, and `control-contract.adoc`.
- [x] **3.2 Pre-Commit & Operational Gate**:
  - Run `./mvnw verify -Ppre-commit` and execute PR/review workflow per [`doc/agy/runbook.md`](runbook.md).
