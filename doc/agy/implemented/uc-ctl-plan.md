# Implementation Plan: UC-CTL-1..5 Controlling Work (Plan & Epic Actions, Holds, Settings)

This plan coordinates the end-to-end implementation of use cases `UC-CTL-1: Control a Plan`, `UC-CTL-2: Control an Epic`, `UC-CTL-3: Review an Epic's Findings or Watches`, `UC-CTL-4: Resolve What Holds a Plan`, and `UC-CTL-5: Change a Plan's Settings` across all client and runtime modules in `pm-mcp-clients`.

It operationalizes the technical recipe from [`doc/meta/implement_use-cases.md`](file:///Users/oliver/git/pm-mcp-clients/doc/meta/implement_use-cases.md) under the execution discipline of [`doc/agy/runbook.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/runbook.md).

---

## 1. Traceability & Context

| Dimension | Reference |
|---|---|
| **Use Cases** | [`plan-marshall-documentation/doc/use-cases/06-controlling-work.adoc#UC-CTL-1`](file:///Users/oliver/git/plan-marshall-documentation/doc/use-cases/06-controlling-work.adoc) (Control a Plan), [`#UC-CTL-2`](file:///Users/oliver/git/plan-marshall-documentation/doc/use-cases/06-controlling-work.adoc) (Control an Epic), [`#UC-CTL-3`](file:///Users/oliver/git/plan-marshall-documentation/doc/use-cases/06-controlling-work.adoc) (Review Findings or Watches), [`#UC-CTL-4`](file:///Users/oliver/git/plan-marshall-documentation/doc/use-cases/06-controlling-work.adoc) (Resolve Holds), [`#UC-CTL-5`](file:///Users/oliver/git/plan-marshall-documentation/doc/use-cases/06-controlling-work.adoc) (Plan Settings) |
| **Specifications** | [`02-plan-pause-and-stalled-plans.adoc#plan-pause`](file:///Users/oliver/git/pm-mcp-core/doc/specification/runtime-lifecycle/02-plan-pause-and-stalled-plans.adoc), [`01-processes-and-transport.adoc#epic-resource`](file:///Users/oliver/git/plan-marshall-documentation/doc/specification/runtime-model/01-processes-and-transport.adoc), [`06-event-catalogue.adoc`](file:///Users/oliver/git/plan-marshall-documentation/doc/specification/runtime-model/06-event-catalogue.adoc), [`04-plan-finalize-terminal.adoc#plan-end`](file:///Users/oliver/git/pm-mcp-core/doc/specification/state-catalogue/04-plan-finalize-terminal.adoc), [`01-conventions-generic-conditions.adoc#holds`](file:///Users/oliver/git/pm-mcp-core/doc/specification/state-catalogue/01-conventions-generic-conditions.adoc), [`08-epic-coordination.adoc#epic-actions`](file:///Users/oliver/git/pm-mcp-core/doc/specification/model-work/08-epic-coordination.adoc), [`09-plan-instance.adoc#plan-overrides`](file:///Users/oliver/git/pm-mcp-core/doc/specification/workflow-dsl/09-plan-instance.adoc) |
| **Local Client Design** | Shared: [`doc/design/shared/work-actions.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/design/shared/work-actions.adoc), [`epic-view.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/design/shared/epic-view.adoc)<br>CLI: [`doc/design/terminal-client/19-actions.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/design/terminal-client/19-actions.adoc)<br>Web: [`doc/design/web-app/README.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/design/web-app/README.adoc) |
| **Roadmap Packages** | [`doc/plans/control-contract.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/plans/control-contract.adoc), [`doc/plans/cli-actions.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/plans/cli-actions.adoc), [`doc/plans/implemented/web-actions.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/plans/implemented/web-actions.adoc) |
| **Implementation Recipe** | [`doc/meta/implement_use-cases.md`](file:///Users/oliver/git/pm-mcp-clients/doc/meta/implement_use-cases.md) |
| **Operational SOP** | [`doc/agy/runbook.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/runbook.md) |

---

## 2. Parallel Isolation & Non-Overlap Invariants (vs `UC-ENT-1`)

`UC-CTL-1..5` executes concurrently with `UC-ENT-1: Enter and Connect` (`feat/uc-ent-1-connect`). Strict non-overlap is guaranteed across all repository modules:

1. **Contracts & API (`pm-api`)**:
   - `UC-CTL-1..5`: Owns `de.planmarshall.api.control.*` and `messages/control.{en,de}.properties`.
   - `UC-ENT-1`: Owns `de.planmarshall.api.entry.*` and `messages/entry.{en,de}.properties`.
   - Zero shared model classes, enums, or resource properties.
2. **Mock Runtime (`pm-mock-runtime`)**:
   - `UC-CTL-1..5`: Routes `/api/v1/plans/{id}/actions/*`, `/api/v1/epics/{id}/actions/*`, `/api/v1/plans/{id}/holds/*`, `/api/v1/plans/{id}/settings`, scenario `scenarios/control.json`.
   - `UC-ENT-1`: Routes `/api/v1/web/pairings`, `/api/v1/web/listeners/lan`, scenario `scenarios/entry.json`.
   - Disjoint route dispatch tables and scenario files.
3. **CLI Client (`pm-operator`)**:
   - `UC-CTL-1..5`: Owns `ControlCommand.java` (`pause`, `resume`, `stop`, `retry`, `cancel`, `resolve`, `plan-settings`).
   - `UC-ENT-1`: Owns `EnterCommand.java` (`enter`, `pair`, `listeners`).
   - Disjoint Picocli verb classes.
4. **Web Client (`pm-web-app`)**:
   - `UC-CTL-1..5`: `ControlActionsBar.svelte`, `HoldResolutionModal.svelte`, `PlanSettingsModal.svelte`.
   - `UC-ENT-1`: `PairingModal.svelte`, `ConnectionStatusBanner.svelte`.
   - Disjoint Svelte components and route views.
5. **E2E & Integration Tests**:
   - `UC-CTL-1..5`: `pm-web-e2e/tests/control.spec.js` and `ControlCommandTest.java`.
   - `UC-ENT-1`: `pm-web-e2e/tests/entry.spec.js` and `EnterCommandTest.java`.

---

## 3. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created on topic branch off latest `main` (`git worktree add -b feat/uc-ctl-control /Users/oliver/git/worktrees/pm-mcp-clients-uc-ctl main`).
- [x] 2. Domain skills & engineering personas loaded for each task type:
  - `persona-implementer` & `persona-module-tester` for code & unit tests
  - `persona-integration-tester` for multi-module/integration tests
  - `persona-security-expert` for action authorization checks and output sanitization
  - `persona-documenter` for documentation & ADRs
  - `persona-code-reviewer` for finding evaluation & adversarial refutation
- [x] 3. Incremental implementation following the 6-Phase technical recipe with local tests passing.
- [x] 4. Final completion report written (`doc/agy/uc-ctl-plan-report.md`) and initial plan moved to `doc/agy/implemented/uc-ctl-plan.md`.
- [x] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit` for code/config; skipped for standalone documentation changes).
- [ ] 6. PR opened (with `--label "skip-bot-review"` if authorized), 5-minute review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [ ] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [ ] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/uc-ctl-plan-report.md`.

---

## 4. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Trace** | `plan-marshall-persona-documenter` | `plan-marshall-ref-code-quality` | Verify actions availability per state, hold resolution kinds, settings revision conflicts, and event catalogue keys. |
| **Phase 2: `pm-api`** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-module-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:junit-core` | Immutable Java 25 records, Jackson streaming parsing, bilingual resource bundles (`control.{en,de}.properties`). |
| **Phase 3: Mock Runtime** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-integration-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:cui-cui-http-testing` | Stateful scenario JSON (`control.json`), dynamic route handlers, SSE broadcast (`hold.opened`, `hold.changed`, `hold.cleared`). |
| **Phase 4: `pm-operator`** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-security-expert` | `pm-dev-java:java-core`<br>`pm-dev-java:java-security` | Subcommands `pause`, `resume`, `stop`, `retry`, `cancel`, `resolve`, `plan-settings`, output safety, terminal confirmation gates (`PM-SEC-3`). |
| **Phase 5: `pm-web-app`** | `plan-marshall-persona-implementer`<br>`pm-dev-frontend:javascript` | `pm-dev-frontend:css`<br>`pm-dev-frontend:jest-testing` | Actions bar, pause point selection, hold resolution modal, plan settings form with conflict detection, Vitest tests. |
| **Phase 6: Verification** | `plan-marshall-persona-integration-tester` | `pm-dev-java:junit-integration` | `PmOperatorProcessIT` process execution, Playwright E2E browser test suite (`control.spec.js`). |
| **Phase 7: Review Gate** | `plan-marshall-persona-code-reviewer` | `pm-dev-java:java-security` | Adversarial refutation of candidate bot findings, 100% resolution bar. |

---

## 5. Phased Task Breakdown

### Phase 1: Trace Matrix & Specification Alignment
- Load `plan-marshall-persona-documenter`.
- Map all actions across states:
  - Plan running: `pause` (now, after task, after phase), `cancel`, `settings`.
  - Plan paused: `resume`, `cancel`, `abandon`.
  - Plan held: `resolve` (with act per hold kind: `stalled`, `condition`, `integrity_pause`, `held`, `landing_failed`), `cancel`.
  - Epic running/waiting/held: `pause`, `resume`, `cancel` (with choice for plans), `auto` toggle.
- Verify event catalogue keys: `hold.opened`, `hold.changed`, `hold.cleared`, `plan.paused`, `plan.resumed`, `plan.cancelled`.

### Phase 2: Contracts, Models & Message Catalogue (`pm-api`)
- Load `plan-marshall-persona-implementer` and `pm-dev-java:java-core`.
- Create records in `de.planmarshall.api.control.*`:
  - `PlanAction.java`: Type-safe enum of actions (`PAUSE`, `RESUME`, `STOP`, `RETRY`, `CANCEL`, `ABANDON`).
  - `ActionAvailability.java`: Available boolean with disabled reason when false.
  - `PlanActionRequest.java`: Request payload with pause point or cancellation reason.
  - `HoldRecord.java`: Hold details (id, kind, cause, attempts, available acts).
  - `HoldResolutionRequest.java`: Hold resolution payload (`act`, parameters).
  - `PlanSettingsRecord.java`: Revision-stamped settings with aspect overrides.
  - `PlanSettingsUpdateRequest.java`: Settings update with revision conflict guard (`409 Conflict`).
- Create bilingual catalogues: `messages/control.en.properties` and `messages/control.de.properties`.
- Unit tests with `plan-marshall-persona-module-tester`: `ControlRecordsTest.java`, `ControlCatalogueTest.java`.

### Phase 3: Mock Runtime Scenarios & Dynamic Endpoints (`pm-mock-runtime`)
- Load `plan-marshall-persona-implementer` and `pm-dev-java:cui-cui-http-testing`.
- Create scenario fixture `scenarios/control.json`:
  - Plan running, plan paused at task boundary, plan held by integrity pause, plan with custom settings.
  - Epic with automatic mode on/off and held rows.
- Implement endpoints in `MockRuntimeEngine.java`:
  - `POST /api/v1/plans/{id}/actions/{action}`
  - `POST /api/v1/epics/{id}/actions/{action}`
  - `POST /api/v1/plans/{id}/holds/{holdId}/resolve`
  - `GET /api/v1/plans/{id}/settings` and `PUT /api/v1/plans/{id}/settings` (with revision conflict check)
- Unit tests: `ControlScenarioTest.java`.

### Phase 4: Terminal Client Integration (`pm-operator`)
- Load `plan-marshall-persona-implementer` and `pm-dev-java:java-security`.
- Implement `ControlCommand.java`:
  - `pause <id> [--point <now|task|phase>]`
  - `resume <id>`
  - `cancel <id> [--reason <text>] [--yes]`
  - `resolve <id> <holdId> --act <act>`
  - `plan-settings <id> [--set <aspect=value>]`
- Enforce PM-SEC-3 interactive terminal confirmation gates with `--yes` override.
- Unit and process tests: `ControlCommandTest.java` and `PmOperatorProcessIT.java`.

### Phase 5: Web Application Views (`pm-web-app`)
- Load `plan-marshall-persona-implementer` and `pm-dev-frontend:javascript`.
- Extend `client.ts` with control API methods and event subscriptions.
- Create `ControlActionsBar.svelte`: Action buttons dynamically rendered based on `ActionAvailability`.
- Create `HoldResolutionModal.svelte`: Form to inspect hold cause and trigger resolution act.
- Create `PlanSettingsModal.svelte`: Inspect/update plan settings with revision conflict warning.
- Vitest unit tests: `ControlActionsBar.test.ts`, `HoldResolutionModal.test.ts`.

### Phase 6: Verification & End-to-End Suite
- Load `plan-marshall-persona-integration-tester`.
- Write Playwright E2E suite: `pm-web-e2e/tests/control.spec.js`.
- Execute `./mvnw verify -Ppre-commit` and ensure 100% clean reactor compilation.

---

## 6. Definition of Done
1. All 5 use cases (`UC-CTL-1..5`) completely implemented across CLI and Web clients.
2. Invariant "Both clients, one state" verified via real-time SSE event subscriptions.
3. 100% of user-facing messages externalized in bilingual property bundles (`control.{en,de}.properties`).
4. Full reactor build `./mvnw verify -Ppre-commit` passes with zero regressions.
5. Completion report documented at `doc/agy/uc-ctl-plan-report.md` and initial plan archived.
