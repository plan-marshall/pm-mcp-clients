# Implementation Plan: `mock-runtime-reports-system-designer` (Scenarios 4 & 5: `reports-and-system.json`, `designer.json`, Strict Loader Enforcement & Web Scenario Sharing)

This implementation plan governs **Scenario 4 (`reports-and-system.json`)** and **Scenario 5 (`designer.json`)** of the 5-scenario mock-runtime architecture, removes the remaining non-standard `"designer"` top-level schema bypass and the `validateOperatorState` early-return bypass in `ScenarioLoader`, and wires direct scenario JSON consumption into `pm-web-app` Vitest tests.

It operationalizes [`doc/meta/implement_use-cases.adoc`](../meta/implement_use-cases.adoc) under the execution discipline of [`doc/agy/runbook.md`](runbook.md).

---

## 1. Traceability & Context

| Dimension | Reference |
|---|---|
| **Use Cases** | [`UC-PLN-5`](../../../plan-marshall-documentation/doc/use-cases/03-plans.adoc#UC-PLN-5), [`UC-SYS-1..3`](../../../plan-marshall-documentation/doc/use-cases/07-system.adoc), [`UC-DSG-1..5`](../../../plan-marshall-documentation/doc/use-cases/08-designer.adoc) |
| **Specifications** | [`05-mock-runtime.adoc`](../../../plan-marshall-documentation/doc/specification/runtime-model/05-mock-runtime.adoc), [`04-operator-api.adoc`](../../../plan-marshall-documentation/doc/specification/runtime-model/04-operator-api.adoc) |
| **Local Client Design** | [`doc/design/shared/reports.adoc`](../design/shared/reports.adoc), [`doc/design/shared/system-and-cleanup.adoc`](../design/shared/system-and-cleanup.adoc), [`doc/design/shared/workflow-designer.adoc`](../design/shared/workflow-designer.adoc) |
| **Roadmap Packages** | [`doc/plans/implemented/report-contract.adoc`](../plans/implemented/report-contract.adoc), [`doc/plans/implemented/system-contract.adoc`](../plans/implemented/system-contract.adoc), [`doc/plans/implemented/designer-contract.adoc`](../plans/implemented/designer-contract.adoc), [`doc/plans/implemented/web-app-foundation.adoc`](../plans/implemented/web-app-foundation.adoc) |
| **Prerequisite Plans** | [`doc/agy/mock-runtime-foundation-and-consolidation-plan.md`](mock-runtime-foundation-and-consolidation-plan.md), Waves 2a–2c |
| **Implementation Recipe** | [`doc/meta/implement_use-cases.adoc`](../meta/implement_use-cases.adoc) |
| **Operational SOP** | [`doc/agy/runbook.md`](runbook.md) |

---

## 2. Scope & Objectives

1. **Consolidate `reports.json` & `system.json` into Scenario 4 (`reports-and-system.json`)**:
   - Merge `reports.json` and `system.json` into **`pm-mock-runtime/src/main/resources/scenarios/reports-and-system.json`** and delete the old `reports.json` and `system.json` files (mapping their legacy names in `ScenarioRegistry` to `reports-and-system#<state>`).
   - Populate `reports-and-system.json` with full `OperatorStateResource` states, `routes`, and `EventEnvelope` events covering:
     - **Reports cases (`report-contract.adoc`)**: unread & read reports across all report kinds (`plan_completed`, `epic_completed`, `cleanup_completed`, `attention_summary`, `system_notice`), empty inbox, mark single/all read, start follow-up from report, and full `PlanCompletionReport` fixtures (every outcome: `landed`, `nothing_to_land`, `aborted`, `failed`; waivers; phase timeline; token breakdown).
     - **System & Cleanup cases (`system-contract.adoc`)**: `system.running`, `system.draining` (with active blockers and `can_withdraw_stop`), `system.stopped`, `system.restarting`, `system.force-stop`, `cleanup.idle`, `cleanup.preview`, `cleanup.running`, `cleanup.completed`, `cleanup.kept-items` (keep/release), and `cleanup.residues` (inspect/remove residue).
2. **Upgrade Scenario 5 (`designer.json`) & Eliminate Top-Level `"designer"` and `"start_case"` Fields**:
   - Remove the non-standard top-level `"designer"` and `"start_case"` fields from `Scenario.java` and `ScenarioLoader.java`.
   - Store all workflow catalogue, workflow detail, unit detail, validation, placement, and landing fixtures in standard `routes` and `OperatorStateResource` states (`designer.ready`, `designer.empty-catalogue`, `designer.no-project`, `designer.unit-invalid`, `designer.digest-conflict`, `designer.placed`, `designer.landed-pr`, `designer.landed-direct`, `designer.removed`) inside `designer.json`, and seed `DesignerRegistry` from those standard `routes` and states.
3. **Remove the `validateOperatorState` Bypass in `ScenarioLoader`**:
   - Now that all **5 scenario files** (`onboarding.json`, `running-work.json`, `projects-and-start.json`, `reports-and-system.json`, `designer.json`) have 100% `OperatorStateResource` states, remove the early `return` bypass in `ScenarioLoader.validateOperatorState` so any state in any scenario missing a valid `OperatorStateResource` (`state.state_id`) fails scenario loading immediately.
4. **Web App Scenario-Driven `fetch` Test Helper (`pm-web-app`)**:
   - Implement a scenario-driven `fetch` test helper (`pm-web-app/src/lib/api/scenarioMockFetch.ts`) that imports any of the 5 scenario JSON files directly from `../pm-mock-runtime/src/main/resources/scenarios/<name>.json` and serves its `routes` and `states` representations using the same routing rules (`05-mock-runtime.adoc#scenario-files` & `web-app-foundation.adoc`).

---

## 3. Concurrency & Isolation

Runs as **Mock Runtime Wave 3** after Wave 1 (`mock-runtime-foundation-and-consolidation`) and Waves 2a–2c (`mock-runtime-onboarding-and-entry`, `mock-runtime-running-work`, `mock-runtime-projects-and-start`) so that removing `startCase` and `designer` and enforcing strict `validateOperatorState` in `ScenarioLoader` validates all 5 consolidated scenario files cleanly.

### Boundary Rules
- **Sole Owner of**: `pm-mock-runtime/src/main/resources/scenarios/reports-and-system.json`, `designer.json`, deletion of `reports.json` and `system.json`, `DesignerRegistry.java`, `Scenario.java` & `ScenarioLoader.java` (final strict validation & removal of `startCase`/`designer`), `pm-web-app/src/lib/api/scenarioMockFetch.ts`, and `ReportsScenarioTest.java`, `SystemScenarioTest.java`, `DesignerScenarioTest.java`, and `StandardScenariosTest.java`.
- **No Touch**: `doc/plans/README.adoc` and `pm-operator`.

---

## 4. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created at `/Users/oliver/git/worktrees/pm-mcp-clients-mock-reports-system-designer` on branch `feat/mock-runtime-reports-system-designer` off latest `main`.
- [ ] 2. Domain skills & engineering personas loaded where applicable:
  - `persona-implementer` & `persona-module-tester` for code & unit tests
  - `persona-integration-tester` for multi-module/integration tests
  - `persona-security-expert` for trust boundaries & input validation
  - `persona-documenter` for documentation & ADRs
  - `persona-code-reviewer` for finding evaluation & adversarial refutation
- [ ] 3. Incremental implementation with tests following the phased technical recipe.
- [ ] 4. Final completion report written (`doc/agy/mock-runtime-reports-system-designer-plan-report.md`) and initial plan moved to `doc/agy/implemented/mock-runtime-reports-system-designer-plan.md`. (Do NOT move report to `verified/` — Step 8 is reserved for orchestrator).
- [ ] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit` for code/config; skipped for standalone documentation).
- [ ] 6. PR opened (NEVER use `skip-bot-review` on worker PRs; reserved for orchestrator docs PRs), review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [ ] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [ ] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/mock-runtime-reports-system-designer-plan-report.md` by orchestrator session.

---

## 5. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: `reports-and-system.json`** | `plan-marshall-persona-implementer` | `pm-dev-java:java-core` | Consolidate reports, completion reports, system lifecycle, and cleanup states/routes into `reports-and-system.json`. |
| **Phase 2: `designer.json` & Strict Loader** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-module-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:junit-core` | Migrate `designer.json` off top-level `designer` key; remove `startCase` & `designer` and enforce mandatory `OperatorStateResource` in `ScenarioLoader`. |
| **Phase 3: Web Scenario Helper & Tests** | `plan-marshall-persona-module-tester`<br>`plan-marshall-persona-integration-tester` | `pm-dev-frontend:javascript`<br>`pm-dev-frontend:jest-testing` | Add Vitest scenario `fetch` helper in `pm-web-app`; verify exactly 5 scenario files exist in `scenarios/`. |
| **Phase 4: Review Gate** | `plan-marshall-persona-code-reviewer` | `pm-dev-java:java-security` | Pre-commit gate and 100% resolution of automated review findings. |

---

## 6. Phased Technical Recipe

### Phase 1: Author Consolidated `reports-and-system.json`
- [ ] **1.1 Reports & Completion Report States and Routes**:
  - Create `pm-mock-runtime/src/main/resources/scenarios/reports-and-system.json` containing `GET /api/v1/reports` and `/api/v1/plans/{id}/report` routes and `OperatorStateResource` states (`reports.unread`, `reports.all-read`, `reports.empty`, `reports.completion-landed`, `reports.completion-nothing`, `reports.completion-aborted`, `reports.completion-failed`) with `EventEnvelope` `report.arrived` and `report.read` events.
- [ ] **1.2 System Lifecycle & Cleanup States and Routes**:
  - Add `/api/v1/system`, `/api/v1/cleanup`, `/api/v1/cleanup/keep`, and `/api/v1/cleanup/residues` routes and `OperatorStateResource` states (`system.running`, `system.draining`, `system.stopped`, `system.restarting`, `cleanup.idle`, `cleanup.running`, `cleanup.completed`, `cleanup.kept`, `cleanup.residues`) into `reports-and-system.json`.
  - Delete `reports.json` and `system.json`.

### Phase 2: Migrate `designer.json` & Enforce Mandatory `OperatorStateResource` Validation
- [ ] **2.1 Refactor `DesignerRegistry` & `designer.json`**:
  - Move the custom `"designer"` top-level object into standard `routes` (`GET /api/v1/workflows/catalogue`, `GET /api/v1/workflows`, `GET /api/v1/projects/{id}/workflows`) and `OperatorStateResource` states (`designer.ready`, `designer.no-project`, `designer.unit-invalid`, `designer.digest-conflict`, `designer.placed`, `designer.landed-pr`, `designer.landed-direct`, `designer.removed`) inside `designer.json`.
  - Remove `designer` and `startCase` from `Scenario` and `ScenarioLoader`.
- [ ] **2.2 Remove `validateOperatorState` Early-Return Bypass**:
  - Update `ScenarioLoader.validateOperatorState` so that any state whose `representation` is not a valid `OperatorStateResource` (with `state.state_id`) throws `ScenarioValidationException`.
  - Add a test in `StandardScenariosTest` asserting that `pm-mock-runtime/src/main/resources/scenarios/` contains **exactly the 5 canonical `.json` files** (`onboarding.json`, `running-work.json`, `projects-and-start.json`, `reports-and-system.json`, `designer.json`) and that every state in all 5 files decodes as an `OperatorStateResource`.

### Phase 3: Web App Scenario-Driven `fetch` Helper & Verification
- [ ] **3.1 Vitest Scenario Router in `pm-web-app`**:
  - Add `pm-web-app/src/lib/api/scenarioMockFetch.ts` (and a Vitest unit test) that loads any of the 5 scenario JSON files directly from `pm-mock-runtime/src/main/resources/scenarios/` and serves `routes`, current-state `representation`, and state `links` transitions (`05-mock-runtime.adoc` line 76).
- [ ] **3.2 Pre-Commit & Operational Gate**:
  - Run `./mvnw verify -Ppre-commit` and execute PR/review workflow per [`doc/agy/runbook.md`](runbook.md).
