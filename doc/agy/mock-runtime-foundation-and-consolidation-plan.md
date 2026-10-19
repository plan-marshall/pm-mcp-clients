# Implementation Plan: `mock-runtime-foundation-and-consolidation` (Core Engine, Closed Schema, Scoped SSE, Stub Retirement & 5-Scenario Architecture)

This implementation plan governs the foundational remediation of `pm-mock-runtime` identified in the mock-runtime review: enforcing the closed scenario schema and `pm-api` codec validation in `ScenarioLoader`, fixing request dispatch precedence in `MockRuntimeEngine`, implementing scoped SSE streams with `Last-Event-ID` replay and `reset`, adding `/mcp` JSON-RPC support to retire `FakeRuntimeServer` in `pm-api` test-support, and restructuring the 60 fragmented scenario files into **5 rich, functional scenarios** in `StandardScenarios` and `pm-mock-runtime/start.sh`.

It operationalizes [`doc/meta/implement_use-cases.md`](../meta/implement_use-cases.md) under the execution discipline of [`doc/agy/runbook.md`](runbook.md).

---

## 1. Traceability & Context

| Dimension | Reference |
|---|---|
| **Specifications** | [`05-mock-runtime.adoc`](../../../plan-marshall-documentation/doc/specification/runtime-model/05-mock-runtime.adoc), [`04-operator-api.adoc`](../../../plan-marshall-documentation/doc/specification/runtime-model/04-operator-api.adoc) |
| **Roadmap Packages** | [`doc/plans/implemented/mock-runtime.adoc`](../plans/implemented/mock-runtime.adoc), [`doc/plans/implemented/operator-interaction-contract.adoc`](../plans/implemented/operator-interaction-contract.adoc) |
| **Target Modules** | `pm-mock-runtime`, `pm-api` (test-support cleanup), `pm-relay` (IT migration), `pm-operator` (IT migration) |
| **Implementation Recipe** | [`doc/meta/implement_use-cases.md`](../meta/implement_use-cases.md) |
| **Operational SOP** | [`doc/agy/runbook.md`](runbook.md) |

---

## 2. Scope & Objectives

1. **5-Scenario Architecture & Interactive Selector (`StandardScenarios`, `ScenarioRegistry`, `pm-mock-runtime/start.sh`)**:
   - Restructure the user-facing scenario catalog from 60 micro-scenarios into **5 rich, cohesive, multi-state scenarios**:
     1. `onboarding` — Guided & automatic machine onboarding, entry & browser pairing, device management, LAN listener, and machine settings.
     2. `running-work` — Multi-project overview, running/paused/ended plans & epics, process views & live task activity, operator decisions & waivers, and plan/epic control.
     3. `projects-and-start` — Project explorer, enrolment, git/store inspection, and starting plans & epics from all intake sources.
     4. `reports-and-system` — Reports inbox, plan completion reports, runtime lifecycle (drain/stop/restart), and cleanup/kept items.
     5. `designer` — Workflow & unit catalogue, inspection, validation, placement, and landing.
   - Enhance `ScenarioRegistry` to support composite `(scenario, state)` resolution (e.g. `running-work#plan.paused` or legacy scenario aliases mapped to `{scenario, state}` during migration) so tests can jump directly to any case state while `pm-mock-runtime/start.sh` confronts the user with **only the 5 rich scenarios** (showing each scenario's name and description, defaulting to `running-work`).
2. **Closed Schema & `pm-api` Codec Validation (`ScenarioLoader.java`)**:
   - Enforce the closed schema of [`05-mock-runtime.adoc#scenario-files`](../../../plan-marshall-documentation/doc/specification/runtime-model/05-mock-runtime.adoc): reject unknown top-level, state-level, route-level, link-level, and event-level fields with `ScenarioValidationException` naming the file, state/route, and field.
   - Validate `link.error` values against `ApiErrorCode.fromWire(...)`.
   - Validate static `routes` representations against `pm-api` resource codecs for known operator API paths (`/api/v1/overview`, `/api/v1/projects`, `/api/v1/reports`, `/api/v1/system`, `/api/v1/workflows`, etc.).
   - Require `OperatorStateResource` (`state.state_id`), operator `links` matching, and `EventEnvelope` / `EventName` validation on every state (removing the silent `return` bypass in `ScenarioLoader.validateOperatorState` once all 5 consolidated scenarios are migrated).
3. **Request Dispatch Precedence & Strict `mock_no_route` (`MockRuntimeEngine.java`)**:
   - Reorder `MockRuntimeEngine.dispatch` so that current-state `links` (`handleStateLink`) and scenario-defined `routes` (`scenario.route(...)`) are evaluated **before** stateful domain helper handlers, ensuring scenario definitions are never shadowed by Java defaults.
   - Scope stateful domain handlers to the routes/capabilities declared by the active scenario so unoffered endpoints in a scenario properly return `404 mock_no_route` naming the current state and offered links.
4. **Scoped SSE Event Streams, `EventEnvelope` Ring Buffer, `Last-Event-ID` Replay & `reset` (`MockRuntimeEngine.java`, `MockEventStream.java`)**:
   - Support the scoped SSE streams required by `operator-interaction-contract.adoc`:
     - `/api/v1/events` (machine-wide)
     - `/api/v1/plans/{id}/events` (plan-scoped)
     - `/api/v1/entries/{id}/events` (entry-scoped)
     - `/api/v1/reports/events` (reports-scoped)
     - `/api/v1/plans/{id}/tasks/{taskId}/activity/events` (task-activity-scoped)
   - Wrap all emitted events in valid `EventEnvelope` payloads (`id`, `event`, `scope`, `revision`, `at`, `data`) and filter stream delivery by `EventScope`.
   - Maintain a bounded ring buffer of emitted `EventEnvelope` items per stream scope; when a client connects with `Last-Event-ID`, replay subsequent buffered events in order, or emit `event: reset` if `Last-Event-ID` predates the ring buffer or follows a `/mock/reset` / `/mock/scenario` switch.
5. **`/mcp` JSON-RPC Support & Stub-Daemon Retirement (`pm-mock-runtime`, `pm-relay`, `pm-api`)**:
   - Add `/mcp` JSON-RPC request handling (`initialize`, `tools/list`, `tools/call`, and configurable scenario responses/SSE notifications) to `MockRuntimeEngine`.
   - Migrate `PmMcpProcessIT`, `PmMcpTest`, and `RelayTest` in `pm-relay` to run against `pm-mock-runtime`.
   - Adapt `RuntimeFixture` in `pm-api` test-support to delegate to `MockRuntime` (or migrate remaining tests) and remove the duplicate `FakeRuntimeServer` socket server as required by `mock-runtime.adoc`.

---

## 3. Concurrency & Isolation

This plan is the **Wave 1 Foundation** for the mock-runtime remediation series (`mock-runtime-onboarding-and-entry`, `mock-runtime-running-work`, `mock-runtime-projects-and-start`, `mock-runtime-reports-system-designer`).

### Boundary Rules
- `pm-mock-runtime`: Core engine (`MockRuntimeEngine`, `MockEventStream`, `ScenarioLoader`, `ScenarioRegistry`, `StandardScenarios`, `start.sh`).
- `pm-relay` & `pm-api`: Migration of `PmMcpProcessIT` to `MockRuntime` and retirement of `FakeRuntimeServer`.

---

## 4. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [ ] 1. Isolated worktree created at `/Users/oliver/git/worktrees/pm-mcp-clients-mock-runtime-foundation` on branch `feat/mock-runtime-foundation` off latest `main`.
- [ ] 2. Domain skills & engineering personas loaded where applicable:
  - `persona-implementer` & `persona-module-tester` for code & unit tests
  - `persona-integration-tester` for multi-module/integration tests
  - `persona-security-expert` for trust boundaries & input validation
  - `persona-documenter` for documentation & ADRs
  - `persona-code-reviewer` for finding evaluation & adversarial refutation
- [ ] 3. Incremental implementation with tests following the phased technical recipe.
- [ ] 4. Final completion report written (`doc/agy/mock-runtime-foundation-and-consolidation-plan-report.md`) and initial plan moved to `doc/agy/implemented/mock-runtime-foundation-and-consolidation-plan.md`. (Do NOT move report to `verified/` — Step 8 is reserved for orchestrator).
- [ ] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit` for code/config; skipped for standalone documentation).
- [ ] 6. PR opened (NEVER use `skip-bot-review` on worker PRs; reserved for orchestrator docs PRs), review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [ ] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [ ] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/mock-runtime-foundation-and-consolidation-plan-report.md` by orchestrator session.

---

## 5. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: 5-Scenario Catalog & Launcher** | `plan-marshall-persona-implementer` | `pm-dev-java:java-core` | `StandardScenarios` 5 primary constants, `ScenarioRegistry` alias/state resolution, `start.sh` 5-option menu. |
| **Phase 2: Closed Schema & Codec Loader** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-module-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:junit-core` | Closed JSON schema enforcement, `ApiErrorCode` check, route codec validation in `ScenarioLoader`. |
| **Phase 3: Scoped SSE & `Last-Event-ID`** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-module-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:junit-core` | Scoped SSE streams, `EventEnvelope` ring buffer, `Last-Event-ID` replay, `reset` event on eviction/reset. |
| **Phase 4: Dispatch Order & `/mcp` Relay** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-integration-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:junit-integration` | State links & routes precedence, `/mcp` JSON-RPC endpoint, `pm-relay` IT migration, `FakeRuntimeServer` retirement. |
| **Phase 5: Review Gate** | `plan-marshall-persona-code-reviewer` | `pm-dev-java:java-security` | Full reactor pre-commit verification and 100% resolution of review findings. |

---

## 6. Phased Technical Recipe

### Phase 1: 5-Scenario Architecture & Interactive Selector (`StandardScenarios`, `ScenarioRegistry`, `start.sh`)
- [ ] **1.1 Define the 5 Primary Scenarios in `StandardScenarios`**:
  - Define the 5 canonical primary scenarios:
    1. `ONBOARDING = "onboarding"`
    2. `RUNNING_WORK = "running-work"`
    3. `PROJECTS_AND_START = "projects-and-start"`
    4. `REPORTS_AND_SYSTEM = "reports-and-system"`
    5. `DESIGNER = "designer"`
  - Provide `PRIMARY_SCENARIOS` list in `StandardScenarios` and support `<scenario>#<state>` notation (plus a legacy alias table in `ScenarioRegistry`) so existing tests referencing fine-grained scenario constants resolve seamlessly to the target scenario and initial state.
- [ ] **1.2 Update `pm-mock-runtime/start.sh` Interactive Menu**:
  - Update `pm-mock-runtime/start.sh` to present only the **5 primary scenarios** (with their short names and descriptions extracted from the scenario JSON, defaulting to `running-work`) instead of listing 60 files.
  - Still allow `-s <name|name#state|path>` on the command line for jumping to any specific state or custom scenario file.

### Phase 2: Closed Schema & `pm-api` Codec Validation in `ScenarioLoader`
- [ ] **2.1 Enforce Closed JSON Schema**:
  - Reject unknown keys at the root object (`format_version`, `name`, `description`, `start`, `pairing_code`, `routes`, `states`), state object (`representation`, `links`, `events`, `auto`), route object (`status`, `representation`, `headers`), link object (`method`, `href`, `status`, `next`, `delay_ms`, `error`), and event object (`after_ms`, `event`, `data`).
- [ ] **2.2 Validate `link.error` and `routes`**:
  - Verify `link.error` (when present) decodes via `ApiErrorCode.fromWire(...)`; throw `ScenarioValidationException` naming the file, state, and link if unknown.
  - Validate static `routes` representations against `pm-api` codecs for known endpoints (`RuntimeStatus`, `OverviewResource`, `ProjectListResource`, `ReportListResource`, `SystemStatusResource`, `CleanupStatusResource`, `WorkflowCatalogueResource`).
- [ ] **2.3 Unit Tests in `ScenarioLoaderTest`**:
  - Add fail-first unit tests asserting that unknown top-level/state/link keys, unknown `link.error` codes, and malformed route representations throw `ScenarioValidationException` naming the offending file, state/route, and field.

### Phase 3: Scoped SSE Streams, `EventEnvelope` Ring Buffer, `Last-Event-ID` Replay & `reset`
- [ ] **3.1 Stream Scope & Ring Buffer**:
  - Associate each `MockEventStream` with its subscription scope (`machine` for `/api/v1/events`, `plan:<id>` for `/api/v1/plans/{id}/events`, `entry:<id>` for `/api/v1/entries/{id}/events`, `reports` for `/api/v1/reports/events`, `activity:<planId>` for `/api/v1/plans/{id}/activity/events`).
  - Maintain a ring buffer (e.g. capacity 64) of sequenced `EventEnvelope` items (`id`, `event`, `scope`, `revision`, `at`, `data`).
  - Ensure all `broadcastEvent` calls construct a valid `EventEnvelope` and deliver only to streams whose scope matches (`machine` stream receives machine/global events; scoped streams receive events matching their scope).
- [ ] **3.2 `Last-Event-ID` Replay & `reset` Event**:
  - Pass `MockHttpRequest` into `handleEventStream(request, response, scope)`.
  - If `Last-Event-ID` header is present:
    - If the ID is found in the current scenario's ring buffer, immediately replay all subsequent buffered events for that scope in order.
    - If the ID is not in the ring buffer (evicted, unknown, or after `/mock/reset` / `/mock/scenario`), immediately send an SSE `event: reset` frame with an `EventEnvelope` (`EventName.RESET`).
- [ ] **3.3 Unit & Integration Tests**:
  - Add tests in `OperatorInteractionScenarioTest` and `MockRuntimeEngineTest` verifying scoped stream filtering, `Last-Event-ID` replay of missed events, and `reset` emission when `Last-Event-ID` is stale or after `/mock/reset`.

### Phase 4: Request Dispatch Precedence, `/mcp` JSON-RPC Endpoint & Stub-Daemon Retirement
- [ ] **4.1 Fix Dispatch Order in `MockRuntimeEngine`**:
  - Evaluate current-state `links` (`handleStateLink`) and scenario static `routes` (`scenario.route(...)`) **before** falling back to stateful domain handlers.
  - Guard stateful domain handlers so they only serve endpoints belonging to the active scenario's domain or declared routes; otherwise fall through to `handleNoRoute` (`404 mock_no_route` with `state` and `offered_links`).
- [ ] **4.2 Add `/mcp` JSON-RPC Handler & Migrate `pm-relay`**:
  - Implement `/mcp` (`POST /mcp`) handling in `MockRuntimeEngine` supporting `Mcp-Method` (`initialize`, `tools/list` with SSE chunked progress + message, and default JSON-RPC responses or scenario-configured routes).
  - Migrate `PmMcpProcessIT` in `pm-relay` to use `MockRuntime` and remove `FakeRuntimeServer`.
- [ ] **4.3 Pre-Commit & Operational Gate**:
  - Run `./mvnw verify -Ppre-commit` and execute PR/review workflow per [`doc/agy/runbook.md`](runbook.md).
