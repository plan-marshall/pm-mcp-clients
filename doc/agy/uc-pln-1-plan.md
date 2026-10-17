# Implementation Plan: UC-PLN-1 / UC-PLN-5 Follow a Plan Down to Its Parts & Follow an Epic

This plan coordinates the end-to-end implementation of use cases `UC-PLN-1: Follow a Plan Down to Its Parts` and `UC-PLN-5: Follow an Epic` (the Plan View, Process Tree, Sub-Workflow Traversal, Leaf States, and Live Worker Activity) across all client and runtime modules in `pm-mcp-clients`.

It operationalizes the technical recipe from [`doc/meta/implement_use-cases.md`](file:///Users/oliver/git/pm-mcp-clients/doc/meta/implement_use-cases.md) under the execution discipline of [`doc/agy/runbook.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/runbook.md).

---

## 1. Traceability & Context

| Dimension | Reference |
|---|---|
| **Use Cases** | [`plan-marshall-documentation/doc/use-cases/03-plans.adoc#UC-PLN-1`](file:///Users/oliver/git/plan-marshall-documentation/doc/use-cases/03-plans.adoc) (Plan View), [`#UC-PLN-5`](file:///Users/oliver/git/plan-marshall-documentation/doc/use-cases/03-plans.adoc) (Follow an Epic) |
| **Specifications** | [`01-processes-and-transport.adoc#process-resource`](file:///Users/oliver/git/plan-marshall-documentation/doc/specification/runtime-model/01-processes-and-transport.adoc), [`#process-events`](file:///Users/oliver/git/plan-marshall-documentation/doc/specification/runtime-model/01-processes-and-transport.adoc), [`06-event-catalogue.adoc`](file:///Users/oliver/git/plan-marshall-documentation/doc/specification/runtime-model/06-event-catalogue.adoc), [`02-workflow-state-store.adoc#plan-history`](file:///Users/oliver/git/pm-mcp-core/doc/specification/store-schemas/02-workflow-state-store.adoc), [`03-findings-and-review-records.adoc#phase-sub-workflows`](file:///Users/oliver/git/pm-mcp-core/doc/specification/store-schemas/03-findings-and-review-records.adoc), [`09-plan-instance.adoc`](file:///Users/oliver/git/pm-mcp-core/doc/specification/workflow-dsl/09-plan-instance.adoc), [`01-profile-schema.adoc#worker-activity`](file:///Users/oliver/git/pm-mcp-core/doc/specification/client-profiles/01-profile-schema.adoc), [`03-supervision-and-recycling.adoc#worker-off-track`](file:///Users/oliver/git/pm-mcp-core/doc/specification/model-work/03-supervision-and-recycling.adoc) |
| **Local Client Design** | Shared: [`doc/design/shared/plan-view.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/design/shared/plan-view.adoc), [`epic-view.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/design/shared/epic-view.adoc)<br>CLI: [`doc/design/terminal-client/16-plan-view.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/design/terminal-client/16-plan-view.adoc), [`19-actions.adoc#epic-view`](file:///Users/oliver/git/pm-mcp-clients/doc/design/terminal-client/19-actions.adoc)<br>Web: [`doc/design/web-app/README.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/design/web-app/README.adoc) |
| **Roadmap Packages** | [`doc/plans/process-contract.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/plans/process-contract.adoc), [`doc/plans/cli-plan-view.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/plans/cli-plan-view.adoc), [`doc/plans/web-plan-view.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/plans/web-plan-view.adoc) |
| **Implementation Recipe** | [`doc/meta/implement_use-cases.md`](file:///Users/oliver/git/pm-mcp-clients/doc/meta/implement_use-cases.md) |
| **Operational SOP** | [`doc/agy/runbook.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/runbook.md) |

---

## 2. Parallel Isolation & Non-Overlap Invariants (vs `UC-PRJ-1..3`)

`UC-PLN-1 / UC-PLN-5` executes concurrently with `UC-PRJ-1..3: Projects (Onboard, Explore, Settings, Release)` (`feat/uc-prj-projects`). Strict non-overlap is guaranteed across all repository modules:

1. **Contracts & API (`pm-api`)**:
   - `UC-PLN-1 / UC-PLN-5`: Owns `de.planmarshall.api.process.*` and `messages/plan_view.{en,de}.properties`, `messages/epic_view.{en,de}.properties`.
   - `UC-PRJ-1..3`: Owns `de.planmarshall.api.project.*` and `messages/projects.{en,de}.properties`.
   - Zero shared model classes, enums, or resource properties.
2. **Mock Runtime (`pm-mock-runtime`)**:
   - `UC-PLN-1 / UC-PLN-5`: Routes `/api/v1/plans/{id}/process`, `/api/v1/plans/{id}/tasks/{taskId}/activity`, `/api/v1/epics/{id}/process`, scenario `scenarios/plan-view.json`.
   - `UC-PRJ-1..3`: Routes `/api/v1/projects?all`, `/api/v1/explorer/favourites`, `/api/v1/project-onboardings`, `/api/v1/projects/{id}/release`, scenario `scenarios/projects.json`.
   - Disjoint route dispatch tables and scenario files.
3. **CLI Client (`pm-operator`)**:
   - `UC-PLN-1 / UC-PLN-5`: Extends `PlanCommand.java` (process tree rendering, live task activity) and `EpicCommand.java`.
   - `UC-PRJ-1..3`: Owns `ProjectCommand.java` (`project <id>`, `projects`, `project release`).
   - Disjoint Picocli verb classes.
4. **Web Client (`pm-web-app`)**:
   - `UC-PLN-1 / UC-PLN-5`: Routes `/plans/[id]` with `PlanView.svelte` and `/epics/[id]` with `EpicView.svelte`.
   - `UC-PRJ-1..3`: Route `/projects` with `ProjectExplorerView.svelte` and `ProjectOnboardingModal.svelte`.
   - Disjoint Svelte components and route views.
5. **E2E & Integration Tests**:
   - `UC-PLN-1 / UC-PLN-5`: `pm-web-e2e/tests/plan-view.spec.js` and `PlanViewCommandTest.java`.
   - `UC-PRJ-1..3`: `pm-web-e2e/tests/projects.spec.js` and `ProjectCommandTest.java`.

---

## 3. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created on topic branch off latest `main` (`git worktree add -b feat/uc-pln-1-plan-view /Users/oliver/git/worktrees/pm-mcp-clients-uc-pln-1 main`).
- [ ] 2. Domain skills & engineering personas loaded for each task type:
  - `persona-implementer` & `persona-module-tester` for code & unit tests
  - `persona-integration-tester` for multi-module/integration tests
  - `persona-security-expert` for untrusted text sanitization & output safety (worker activity, tools, files)
  - `persona-documenter` for documentation & ADRs
  - `persona-code-reviewer` for finding evaluation & adversarial refutation
- [ ] 3. Incremental implementation following the 6-Phase technical recipe with local tests passing.
- [ ] 4. Final completion report written (`doc/agy/uc-pln-1-plan-report.md`) and initial plan moved to `doc/agy/implemented/uc-pln-1-plan.md`.
- [ ] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit` for code/config; skipped for standalone documentation changes).
- [ ] 6. PR opened (with `--label "skip-bot-review"` if authorized), 5-minute review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [ ] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.

---

## 4. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Trace** | `plan-marshall-persona-documenter` | `plan-marshall-ref-code-quality` | Verify process tree schema, node states, sub-workflow nesting, worker activity events, catalogue keys. |
| **Phase 2: `pm-api`** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-module-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:junit-core` | Immutable Java 25 records, Jackson streaming parsing, bilingual resource bundles (`plan_view.{en,de}.properties`, `epic_view.{en,de}.properties`). |
| **Phase 3: Mock Runtime** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-integration-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:cui-cui-http-testing` | Stateful scenario JSON (`plan-view.json`), dynamic route handlers, SSE broadcast (`state.entered`, `activity`, `task.off_track`). |
| **Phase 4: `pm-operator`** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-security-expert` | `pm-dev-java:java-core`<br>`pm-dev-java:java-security` | `PlanCommand` (`plan <id>`, `plan <id> --task <taskId>`), `EpicCommand`, tree indentation, glyphs, output safety. |
| **Phase 5: `pm-web-app`** | `plan-marshall-persona-implementer`<br>`pm-dev-frontend:javascript` | `pm-dev-frontend:css`<br>`pm-dev-frontend:jest-testing` | Process tree hierarchy view, expandable nodes, worker activity stream, off-track indicator, Vitest tests. |
| **Phase 6: Verification** | `plan-marshall-persona-integration-tester` | `pm-dev-java:junit-integration` | `PmOperatorProcessIT` process execution, Playwright E2E browser test suite (`plan-view.spec.js`). |
| **Phase 7: Review Gate** | `plan-marshall-persona-code-reviewer` | `pm-dev-java:java-security` | Adversarial refutation of candidate bot findings, 100% resolution bar. |

---

## 5. Phased Task Breakdown

### Phase 1: Traceability & Design Alignment
- [ ] **1.1 Design Specification Audit**: Review `doc/design/shared/plan-view.adoc`, `epic-view.adoc`, and `doc/plans/process-contract.adoc`:
  - Resource routes: `GET /api/v1/plans/{id}/process`, `GET /api/v1/plans/{id}/tasks/{taskId}/activity`, `GET /api/v1/plans/{id}/tasks/{taskId}/activity/events`, `GET /api/v1/epics/{id}/process`.
  - SSE events: `state.entered`, `state.left`, `subworkflow.called`, `subworkflow.returned`, `step.opened`, `step.closed`, `instance.changed`, `activity`, `task.off_track`.
  - Closed enumerations: `NodeKind` (`PHASE`, `SUB_WORKFLOW`, `STATE`, `LEAF`), `NodeState` (`DONE`, `RUNNING`, `WAITING_FOR_OPERATOR`, `FAILED`, `PLANNED`, `NOT_TAKEN`, `NOT_IN_PLAN`), `PruneReason` (`LANE_CHOICE`, `UNNEEDED_STEP`), `TaskStatus`, `OffTrackSignal`, `OffTrackAction`.
  - Extract the complete set of keys needed for `messages/plan_view.{en,de}.properties` and `messages/epic_view.{en,de}.properties`.

### Phase 2: `pm-api` Contracts & Message Catalogue
- [ ] **2.1 Process Tree Data Records**: Implement immutable Java 25 records in `de.planmarshall.api.process`:
  - `ProcessTree`: `planId`, `project`, `epic`, `activity`, `needsOperator`, `instanceVersion`, `tokensSoFar`, `nodes`, `currentPositionPath`.
  - `ProcessNode`: `nodeId`, `title`, `kind`, `state`, `runCount`, `currentRun`, `durationSeconds`, `pruneReason`, `subNodes`, `leafTasks`, `links`.
  - `LeafTask`: `taskId`, `role`, `harness`, `model`, `attempt`, `status`, `durationSeconds`, `tokensSoFar`, `activityLink`, `offTrack`.
  - `TaskActivity`: `taskId`, `role`, `harness`, `model`, `attempt`, `durationSeconds`, `tokensSoFar`, `entries`, `offTrack`.
  - `ActivityEntry`: `timestamp`, `kind` (`MODEL_TEXT`, `TOOL_CALL`, `FILES_CHANGED`, `QUESTIONS_ASKED`), `toolName`, `toolTarget`, `toolFailed`, `content`, `files`, `questions`.
  - `OffTrackInfo`: `signal` (`REPEATING`, `STALL`, `DRIFT`), `action` (`RESTARTED`, `YIELDED`).
  - `EpicProcess`: `epicId`, `project`, `title`, `state`, `runningPlans`, `stagedPlans`, `alongsideLandings`.
- [ ] **2.2 Process Events**: Implement SSE event payload records:
  - `StateEnteredEvent`, `StateLeftEvent`, `SubworkflowCalledEvent`, `ActivityEvent`, `TaskOffTrackEvent`.
- [ ] **2.3 Streaming Codecs**: Implement Jackson streaming codecs (`JsonParser` / `JsonGenerator`) via `JsonTree`.
- [ ] **2.4 Message Catalogue Entries**:
  - Populate `pm-api/src/main/resources/messages/plan_view.en.properties` with keys from `plan-view.adoc#texts`.
  - Populate `pm-api/src/main/resources/messages/plan_view.de.properties` with German translations.
  - Populate `pm-api/src/main/resources/messages/epic_view.{en,de}.properties`.
  - Register `"plan_view"` and `"epic_view"` in `MessageCatalogue.SUPPORTED_AREAS`.
- [ ] **2.5 Unit Tests**:
  - `ProcessTreeTest.java`: Round-trip serialization and deserialization of tree hierarchy, leaf states, and worker activity entries.
  - `PlanViewCatalogueTest.java`: Key presence and ICU parameter validation across English and German bundles.

### Phase 3: `pm-mock-runtime` Scenarios & Handlers
- [ ] **3.1 Scenario Definition**: Create `pm-mock-runtime/src/main/resources/scenarios/plan-view.json`:
  - Full plan process tree (`fix-relay`): phases `init`, `refine`, `outline`, `plan`, `execute` (with nested sub-workflows), and `finalize`.
  - Land lane plan (`docs-typo`): with pruned phases (`not in this plan: refine, outline, plan`).
  - Live activity entries for running worker tasks with tool calls, diffs, and off-track restart signals.
  - Epic process view with staged and running plans.
- [ ] **3.2 Dynamic Route Handlers**: Extend `MockRuntimeEngine.java`:
  - `GET /api/v1/plans/{id}/process`: Returns complete node hierarchy with current execution cursor.
  - `GET /api/v1/plans/{id}/tasks/{taskId}/activity`: Returns worker activity log.
  - `GET /api/v1/plans/{id}/tasks/{taskId}/activity/events`: SSE stream for live activity.
  - `GET /api/v1/epics/{id}/process`: Returns epic process structure.
- [ ] **3.3 Scenario Registration**:
  - Add `PLAN_VIEW = "plan-view"` constant in `StandardScenarios.java` and register in `ALL`.
- [ ] **3.4 Scenario Tests**:
  - `PlanViewScenarioTest.java` asserting scenario loading, REST responses, node expansion, and activity streaming.

### Phase 4: `pm-operator` Terminal Client Implementation
- [ ] **4.1 Plan & Epic Commands Refactoring**:
  - Update `PlanCommand.java`:
    - `pm-operator plan <plan_id>`: Renders indented process tree with glyphs (`✓` done, `▶` running, `!` waiting, `○` planned, `–` not taken, `·` not in plan).
    - `pm-operator plan <plan_id> --task <task_id>`: Shows leaf worker activity log with model output, tools called, files changed, and off-track status.
    - Path navigation header: `project › epic › plan`.
  - Update `EpicCommand.java`:
    - `pm-operator epic <epic_id>`: Shows epic status, active plans, staged plans, and completion review status.
- [ ] **4.2 Output Safety**:
  - Sanitize all model text, tool names, parameters, and file paths using `OutputSafety.stripControlSequences`.
- [ ] **4.3 CLI Unit & Transcript Tests**:
  - `PlanViewCommandTest.java`: Validate indented tree output, task activity printing, glyph rendering, and argument parsing.

### Phase 5: `pm-web-app` Client Implementation
- [ ] **5.1 API Client Extensions**:
  - Add `getPlanProcess(planId)`, `getTaskActivity(planId, taskId)`, `getEpicProcess(epicId)` to `pm-web-app/src/lib/api/client.ts`.
  - Extend SSE event listener to handle process tree updates (`state.entered`, `activity`).
- [ ] **5.2 Plan View (`PlanView.svelte`)**:
  - Two-panel responsive layout:
    - Left/Main panel: Interactive tree of phases, sub-workflows, and states with expansion toggles and progress indicator.
    - Right/Detail panel: Leaf task inspector showing worker-at-work live activity feed, model metadata, tool calls, and off-track warnings.
  - Jump-to-current-position shortcut button.
  - One-step navigation links to parent project, parent epic, and completion report.
- [ ] **5.3 Epic View (`EpicView.svelte`)**:
  - Epic overview card: running vs staged plans list, overlap indicators, completion review trigger.
- [ ] **5.4 Responsive Styling**:
  - Adapt from side-by-side desktop view to stacked mobile accordion (320px+).
- [ ] **5.5 Vitest Tests**:
  - `PlanView.test.ts` and `EpicView.test.ts` verifying rendering, node toggling, activity feed updates, and off-track warning display.

### Phase 6: Integration Testing & Verification
- [ ] **6.1 Playwright E2E Suite**: Create `pm-web-e2e/tests/plan-view.spec.js`:
  - Test viewing plan process tree.
  - Test expanding sub-workflow nodes.
  - Test viewing worker activity stream with tool calls.
  - Test off-track warning badge display.
  - Test navigation back to epic and project views.
- [ ] **6.2 Operator Process IT**:
  - Add process test in `PmOperatorProcessIT.java` executing `plan <id>` and `epic <id>` against running `pm-mock-runtime`.
- [ ] **6.3 Domain Invariants Verification**:
  - Verify against checklist in Section 1 of `doc/meta/implement_use-cases.md`.

### Phase 7: Completion Report, Pre-Commit, Review Gate & Landing (Runbook Discipline)
- [ ] **7.1 Final Completion Report**:
  - Generate comprehensive completion report in `doc/agy/uc-pln-1-plan-report.md`.
- [ ] **7.2 Plan Archival**:
  - Move `doc/agy/uc-pln-1-plan.md` to `doc/agy/implemented/uc-pln-1-plan.md` and record entry in `doc/agy/implemented/README.md`.
- [ ] **7.3 Pre-Commit Gate**:
  - Run `./mvnw verify -Ppre-commit` to validate OpenRewrite formatting, Checkstyle, and full test suite.
- [ ] **7.4 Commit & PR**:
  - Commit changes with Conventional Commits message (`feat(plan-view): implement UC-PLN-1 and UC-PLN-5 plan and epic views`).
  - Open PR via `gh pr create` (with `--label "skip-bot-review"` if authorized).
- [ ] **7.5 5-Minute Review Window**:
  - Schedule 300s timer (`schedule` tool) and await CI, SonarCloud, Sourcery, and CodeRabbit.
- [ ] **7.6 100% Findings Resolution**:
  - Evaluate all automated findings with `persona-code-reviewer` adversarial refutation and resolve 100% of issues.
- [ ] **7.7 Merge Queue & Cleanup**:
  - Enqueue PR via `gh pr merge --auto --squash`.
  - Upon merge, synchronize `main`, remove worktree and delete topic branch.
- [ ] **7.8 Roadmap Archival**:
  - Update `doc/plans/README.adoc` and move completed package documentation (`process-contract.adoc`, `cli-plan-view.adoc`, `web-plan-view.adoc`) to `doc/plans/implemented/`.
