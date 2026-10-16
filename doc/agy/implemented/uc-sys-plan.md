# Implementation Plan: UC-SYS-1..3 System View, Runtime Lifecycle & Cleanup

This plan coordinates the end-to-end implementation of use cases `UC-SYS-1: The Runtime's Lifecycle`, `UC-SYS-2: The System View`, and `UC-SYS-3: Cleanup Work History and Reports` across all client and runtime modules in `pm-mcp-clients`.

It operationalizes the technical recipe from [`doc/meta/implement_use-cases.md`](file:///Users/oliver/git/pm-mcp-clients/doc/meta/implement_use-cases.md) under the execution discipline of [`doc/agy/runbook.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/runbook.md).

---

## 1. Traceability & Context

| Dimension | Reference |
|---|---|
| **Use Cases** | [`plan-marshall-documentation/doc/use-cases/07-system.adoc#UC-SYS-1`](file:///Users/oliver/git/plan-marshall-documentation/doc/use-cases/07-system.adoc) (Lifecycle), [`#UC-SYS-2`](file:///Users/oliver/git/plan-marshall-documentation/doc/use-cases/07-system.adoc) (System View), [`#UC-SYS-3`](file:///Users/oliver/git/plan-marshall-documentation/doc/use-cases/07-system.adoc) (Cleanup) |
| **Specifications** | [`01-startup-recovery-drain.adoc#runtime-lifecycle-operations`](file:///Users/oliver/git/pm-mcp-core/doc/specification/runtime-lifecycle/01-startup-recovery-drain.adoc), [`03-runtime-metrics.adoc#runtime-metrics`](file:///Users/oliver/git/pm-mcp-core/doc/specification/runtime-lifecycle/03-runtime-metrics.adoc), [`03-retention-host-config.adoc#cleanup`](file:///Users/oliver/git/pm-mcp-core/doc/specification/timeouts/03-retention-host-config.adoc), [`12-machine-stores.adoc#cleanup-journal`](file:///Users/oliver/git/pm-mcp-core/doc/specification/store-schemas/12-machine-stores.adoc), [`01-profile-schema.adoc#token-usage`](file:///Users/oliver/git/pm-mcp-core/doc/specification/client-profiles/01-profile-schema.adoc), [`01-processes-and-transport.adoc#local-api`](file:///Users/oliver/git/plan-marshall-documentation/doc/specification/runtime-model/01-processes-and-transport.adoc) |
| **Local Client Design** | Shared: [`doc/design/shared/system.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/design/shared/system.adoc), [`cleanup.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/design/shared/cleanup.adoc)<br>CLI: [`doc/design/terminal-client/25-system.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/design/terminal-client/25-system.adoc), [`26-cleanup.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/design/terminal-client/26-cleanup.adoc)<br>Web: [`doc/design/web-app/README.adoc#structure`](file:///Users/oliver/git/pm-mcp-clients/doc/design/web-app/README.adoc) |
| **Roadmap Packages** | [`doc/plans/system-contract.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/plans/system-contract.adoc), [`doc/plans/cli-system.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/plans/cli-system.adoc), [`doc/plans/web-system.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/plans/web-system.adoc) |
| **Implementation Recipe** | [`doc/meta/implement_use-cases.md`](file:///Users/oliver/git/pm-mcp-clients/doc/meta/implement_use-cases.md) |
| **Operational SOP** | [`doc/agy/runbook.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/runbook.md) |

---

## 2. Parallel Isolation & Non-Overlap Invariants (vs `UC-PLN-2`)

`UC-SYS-1..3` executes concurrently with `UC-PLN-2: Decide What a Plan Asks` (`feat/uc-pln-2-decisions`). Strict non-overlap is guaranteed across all repository modules:

1. **Contracts & API (`pm-api`)**:
   - `UC-SYS-1..3`: Owns `de.planmarshall.api.system.*` and `messages/system.{en,de}.properties`.
   - `UC-PLN-2`: Owns `de.planmarshall.api.decision.*` and `messages/decisions.{en,de}.properties`.
   - Zero shared model classes, enums, or resource properties.
2. **Mock Runtime (`pm-mock-runtime`)**:
   - `UC-SYS-1..3`: Routes `/api/v1/system*` and `/api/v1/operator/runtime*`, scenario `scenarios/system.json`.
   - `UC-PLN-2`: Routes `/api/v1/decisions*`, scenario `scenarios/decisions.json`.
   - Disjoint route dispatch tables and scenario files.
3. **CLI Client (`pm-operator`)**:
   - `UC-SYS-1..3`: Owns `SystemCommand.java`, `DrainCommand.java`, and `CleanupCommand.java`.
   - `UC-PLN-2`: Owns `QuestionsCommand.java` and `AnswerCommand.java`.
   - Disjoint Picocli verb classes.
4. **Web Client (`pm-web-app`)**:
   - `UC-SYS-1..3`: Route `/system` with `SystemView.svelte` and `CleanupView.svelte`.
   - `UC-PLN-2`: Route `/decisions` or questions modal/panel with `DecisionView.svelte`.
   - Disjoint Svelte components and route views.
5. **E2E & Integration Tests**:
   - `UC-SYS-1..3`: `pm-web-e2e/tests/system.spec.js` and `SystemCommandTest.java`.
   - `UC-PLN-2`: `pm-web-e2e/tests/decisions.spec.js` and `QuestionsCommandTest.java`.

---

## 3. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created on topic branch off latest `main` (`git worktree add -b feat/uc-sys-cleanup /Users/oliver/git/worktrees/pm-mcp-clients-uc-sys main`).
- [x] 2. Domain skills & engineering personas loaded for each task type:
  - `persona-implementer` & `persona-module-tester` for code & unit tests
  - `persona-integration-tester` for multi-module/integration tests
  - `persona-security-expert` for untrusted text sanitization & operator authorization
  - `persona-documenter` for documentation & ADRs
  - `persona-code-reviewer` for finding evaluation & adversarial refutation
- [ ] 3. Incremental implementation following the 6-Phase technical recipe with local tests passing.
- [ ] 4. Final completion report written (`doc/agy/uc-sys-plan-report.md`) and initial plan moved to `doc/agy/implemented/uc-sys-plan.md`.
- [ ] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit` for code/config; skipped for standalone documentation changes).
- [ ] 6. PR opened (with `--label "skip-bot-review"` if authorized), 5-minute review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [ ] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.

---

## 4. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Trace** | `plan-marshall-persona-documenter` | `plan-marshall-ref-code-quality` | Verify system snapshot schema, lifecycle state transitions, cleanup job structures, catalogue keys. |
| **Phase 2: `pm-api`** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-module-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:junit-core` | Immutable Java 25 records, Jackson streaming parsing, bilingual resource bundles (`system.{en,de}.properties`). |
| **Phase 3: Mock Runtime** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-integration-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:cui-cui-http-testing` | Stateful scenario JSON (`system.json`), dynamic route handlers, SSE broadcast (`system.metrics`, `runtime.draining`, `cleanup.job_opened`). |
| **Phase 4: `pm-operator`** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-security-expert` | `pm-dev-java:java-core`<br>`pm-dev-java:java-security` | `SystemCommand`, `DrainCommand`, `CleanupCommand`, output safety, terminal formatting of metrics & cleanup jobs. |
| **Phase 5: `pm-web-app`** | `plan-marshall-persona-implementer`<br>`pm-dev-frontend:javascript` | `pm-dev-frontend:css`<br>`pm-dev-frontend:jest-testing` | System view, metric gauges, cleanup review modal, duration settings, Vitest tests. |
| **Phase 6: Verification** | `plan-marshall-persona-integration-tester` | `pm-dev-java:junit-integration` | `PmOperatorProcessIT` process execution, Playwright E2E browser test suite (`system.spec.js`). |
| **Phase 7: Review Gate** | `plan-marshall-persona-code-reviewer` | `pm-dev-java:java-security` | Adversarial refutation of candidate bot findings, 100% resolution bar. |

---

## 5. Phased Task Breakdown

### Phase 1: Traceability & Design Alignment
- [x] **1.1 Design Specification Audit**: Review `doc/design/shared/system.adoc`, `cleanup.adoc`, and `doc/plans/system-contract.adoc`:
  - Resource routes: `GET /api/v1/system?window={window}&project={project}`, `GET /api/v1/system/release-notes?version={v}`, `POST /api/v1/operator/runtime/stop`, `DELETE /api/v1/operator/runtime/stop`, `POST /api/v1/operator/runtime/upgrade`, `GET /api/v1/system/cleanup`, `POST /api/v1/system/cleanup/{id}/delete`, `POST /api/v1/system/cleanup/{id}/remind`, `GET /api/v1/system/cleanup/kept`.
  - SSE events: `system.metrics`, `runtime.draining`, `runtime.drain_withdrawn`, `runtime.stopping`, `runtime.upgrading`, `cleanup.job_opened`, `cleanup.decided`, `cleanup.done`.
  - Closed enumerations: `SystemWindow` (`HOUR`, `TODAY`, `SEVEN_DAYS`), `RuntimeLifecycleState` (`RUNNING`, `DRAINING`, `UPGRADING`), `CleanupJobState` (`OPEN`, `POSTPONED`, `DELETING`, `DONE`), `StopAction` (`DRAIN`, `NOW`), `PostStopAction` (`EXIT`, `RESTART`).
  - Extract the complete set of keys needed for `messages/system.{en,de}.properties`.

### Phase 2: `pm-api` Contracts & Message Catalogue
- [x] **2.1 System & Runtime Data Records**: Implement immutable Java 25 records in `de.planmarshall.api.system`:
  - `SystemSnapshot`: `runtime`, `work`, `workers`, `jobs`, `models`, `cleanupSummary`.
  - `RuntimeMetrics`: `version`, `build`, `startedAt`, `uptimeSeconds`, `heapUsedBytes`, `heapMaxBytes`, `residentMemoryBytes`, `cpuPercent`, `machineStoreBytes`, `diskFreeBytes`, `socketListener`, `loopbackListener`, `networkListener`.
  - `WorkMetrics`: `plansByState`, `epicsByState`, `tasksRunning`, `tasksQueued`, `activeProjectsCount`.
  - `WorkerMetric`: `role`, `harness`, `model`, `state` (`BUSY`, `IDLE`, `STARTING`, `RESTARTING`), `currentPlanTitle`.
  - `ModelMetric`: `harnessAndModel`, `callsCount`, `tokensTotal`, `tokensIn`, `tokensOut`, `errorsCount`, `retriesCount`, `isFloor`.
  - `RuntimeStatus`: `version`, `state`, `drainingBy`, `versionCheck`, `lastUpgradeResult`.
  - `CleanupJob`: `jobId`, `state`, `dueSince`, `summaryByKind`, `freedBytes`, `projectItems`, `links`.
  - `CleanupProjectItem`: `projectId`, `plansCount`, `epicsCount`, `logsCount`, `readReportsCount`, `totalBytes`.
  - `CleanupDurations`: `plansAfterDays`, `epicsAfterDays`, `logsAfterDays`, `reportsAfterDays`, `unenrolledAfterDays`, `remindAfterDays`.
- [x] **2.2 System Events**: Implement SSE event payload records:
  - `SystemMetricsEvent`, `RuntimeDrainingEvent`, `CleanupJobOpenedEvent`, `CleanupDecidedEvent`.
- [x] **2.3 Streaming Codecs**: Implement Jackson streaming codecs (`JsonParser` / `JsonGenerator`) via `JsonTree`.
- [x] **2.4 Message Catalogue Entries**:
  - Populate `pm-api/src/main/resources/messages/system.en.properties` with keys from `system-texts.adoc#texts` and `cleanup-texts.adoc#texts`.
  - Populate `pm-api/src/main/resources/messages/system.de.properties` with German translations.
  - Register `"system"` in `MessageCatalogue.SUPPORTED_AREAS`.
- [x] **2.5 Unit Tests**:
  - `SystemRecordsTest.java`: Round-trip serialization and deserialization of system snapshot, runtime status, and cleanup jobs.
  - `SystemCatalogueTest.java`: Key presence and ICU parameter validation across English and German bundles.

### Phase 3: `pm-mock-runtime` Scenarios & Handlers
- [x] **3.1 Scenario Definition**: Create `pm-mock-runtime/src/main/resources/scenarios/system.json`:
  - Full system snapshot metrics (memory, CPU, disk, listeners, active work, workers).
  - Open cleanup job fixture (`cln-0012`) with multiple projects, ended plans, and freed space calculation.
  - Runtime lifecycle states (running, draining, upgrade check).
- [x] **3.2 Dynamic Route Handlers**: Extend `MockRuntimeEngine.java`:
  - `GET /api/v1/system`: Returns system metrics snapshot with time window filter.
  - `POST /api/v1/operator/runtime/stop`: Enters draining state or stops runtime.
  - `DELETE /api/v1/operator/runtime/stop`: Withdraws drain state.
  - `GET /api/v1/system/cleanup`: Returns current cleanup job or empty state.
  - `POST /api/v1/system/cleanup/{id}/delete`: Executes cleanup deletion.
  - `POST /api/v1/system/cleanup/{id}/remind`: Postpones cleanup job.
  - `GET /api/v1/events`: Broadcasts `system.metrics` and `runtime.draining`.
- [x] **3.3 Scenario Registration**:
  - Add `SYSTEM = "system"` constant in `StandardScenarios.java` and register in `ALL`.
- [x] **3.4 Scenario Tests**:
  - `SystemScenarioTest.java` asserting scenario loading, REST responses, drain controls, and cleanup actions.

### Phase 4: `pm-operator` Terminal Client Implementation
- [x] **4.1 System & Cleanup Commands**:
  - Implement `SystemCommand.java`:
    - Renders 6-part system summary: Runtime, Work, Workers, Jobs, Models (tokens), and Cleanup.
    - Supports `--window` (`hour`, `today`, `7d`), `--project <id>`, and `--json`.
  - Implement `DrainCommand.java`:
    - Handles `pm-operator drain` and `pm-operator drain --withdraw`.
  - Implement `CleanupCommand.java`:
    - Renders cleanup job summary, freed disk space, and interactive confirmation for deleting unkept items.
- [x] **4.2 Output Safety**:
  - Sanitize all system labels, worker names, and error messages using `OutputSafety.stripControlSequences`.
- [x] **4.3 CLI Unit & Transcript Tests**:
  - `SystemCommandTest.java`: Validate metrics layout, drain command arguments, cleanup actions, and exit codes.

### Phase 5: `pm-web-app` Client Implementation
- [x] **5.1 API Client Extensions**:
  - Add `getSystemSnapshot(window?, project?)`, `stopRuntime(when, then)`, `cancelStop()`, `getCleanupJob()`, `executeCleanup(jobId, uncheckIds)`, and `postponeCleanup(jobId)` to `pm-web-app/src/lib/api/client.ts`.
  - Extend SSE event listener to handle `system.metrics` and `runtime.draining`.
- [x] **5.2 System View (`SystemView.svelte`)**:
  - System dashboard:
    - Runtime card: version, uptime, memory gauge, CPU gauge, disk space, active listeners.
    - Work & workers card: active plans, queued tasks, worker matrix (role, model, busy/idle).
    - Models usage card: token breakdown by model/role across time window buttons (`Last hour`, `Today`, `7 days`).
    - Cleanup banner with space freed and `[Review...]` button.
    - Lifecycle action buttons: `[Stop...]`, `[Restart...]`, `[Upgrade...]`.
- [x] **5.3 Cleanup Review View / Modal (`CleanupView.svelte`)**:
  - Displays due cleanup items grouped by project with checkboxes for unchecking kept items.
  - Actions: `[Delete selected]`, `[Remind me later]`, `[Ignore this job]`.
- [x] **5.4 Responsive Styling**:
  - Responsive metric cards adapting down to mobile width (320px).
- [x] **5.5 Vitest Tests**:
  - `SystemView.test.ts` and `CleanupView.test.ts` verifying rendering, metric formatting, lifecycle triggers, and cleanup actions.

### Phase 6: Integration Testing & Verification
- [x] **6.1 Playwright E2E Suite**: Create `pm-web-e2e/tests/system.spec.js`:
  - Test viewing system metrics dashboard.
  - Test toggling metric time windows (`Today` vs `7 days`).
  - Test opening cleanup review modal and executing cleanup.
  - Test runtime drain confirmation.
- [x] **6.2 Operator Process IT**:
  - Add process test in `PmOperatorProcessIT.java` executing `system` and `cleanup` against running `pm-mock-runtime`.
- [x] **6.3 Domain Invariants Verification**:
  - Verify against checklist in Section 1 of `doc/meta/implement_use-cases.md`.

### Phase 7: Completion Report, Pre-Commit, Review Gate & Landing (Runbook Discipline)
- [x] **7.1 Final Completion Report**:
  - Generate comprehensive completion report in `doc/agy/uc-sys-plan-report.md`.
- [x] **7.2 Plan Archival**:
  - Move `doc/agy/uc-sys-plan.md` to `doc/agy/implemented/uc-sys-plan.md` and record entry in `doc/agy/implemented/README.md`.
- [x] **7.3 Pre-Commit Gate**:
  - Run `./mvnw verify -Ppre-commit` to validate OpenRewrite formatting, Checkstyle, and full test suite.
- [ ] **7.4 Commit & PR**:
  - Commit changes with Conventional Commits message (`feat(system): implement UC-SYS-1..3 system view, runtime lifecycle, and cleanup`).
  - Open PR via `gh pr create` (with `--label "skip-bot-review"` if authorized).
- [ ] **7.5 5-Minute Review Window**:
  - Schedule 300s timer (`schedule` tool) and await CI, SonarCloud, Sourcery, and CodeRabbit.
- [ ] **7.6 100% Findings Resolution**:
  - Evaluate all automated findings with `persona-code-reviewer` adversarial refutation and resolve 100% of issues.
- [ ] **7.7 Merge Queue & Cleanup**:
  - Enqueue PR via `gh pr merge --auto --squash`.
  - Upon merge, synchronize `main`, remove worktree and delete topic branch.
- [x] **7.8 Roadmap Archival**:
  - Update `doc/plans/README.adoc` and move completed package documentation (`system-contract.adoc`, `cli-system.adoc`, `web-system.adoc`) to `doc/plans/implemented/`.
