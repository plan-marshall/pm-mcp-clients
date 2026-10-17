# Implementation Plan: UC-PRJ-1..3 Projects (Onboard, Explore, Settings, Release)

This plan coordinates the end-to-end implementation of use cases `UC-PRJ-1: Onboard a Project`, `UC-PRJ-2: Explore Projects`, and `UC-PRJ-3: Release a Project` across all client and runtime modules in `pm-mcp-clients`.

It operationalizes the technical recipe from [`doc/meta/implement_use-cases.md`](file:///Users/oliver/git/pm-mcp-clients/doc/meta/implement_use-cases.md) under the execution discipline of [`doc/agy/runbook.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/runbook.md).

---

## 1. Traceability & Context

| Dimension | Reference |
|---|---|
| **Use Cases** | [`plan-marshall-documentation/doc/use-cases/04-projects.adoc#UC-PRJ-1`](file:///Users/oliver/git/plan-marshall-documentation/doc/use-cases/04-projects.adoc) (Onboard), [`#UC-PRJ-2`](file:///Users/oliver/git/plan-marshall-documentation/doc/use-cases/04-projects.adoc) (Explore Projects), [`#UC-PRJ-3`](file:///Users/oliver/git/plan-marshall-documentation/doc/use-cases/04-projects.adoc) (Release a Project) |
| **Specifications** | [`01-processes-and-transport.adoc#project-explorer`](file:///Users/oliver/git/plan-marshall-documentation/doc/specification/runtime-model/01-processes-and-transport.adoc), [`02-enrolment-and-project-lifecycle.adoc#project-onboarding`](file:///Users/oliver/git/pm-mcp-core/doc/specification/credentials-and-enrolment/02-enrolment-and-project-lifecycle.adoc), [`#project-release`](file:///Users/oliver/git/pm-mcp-core/doc/specification/credentials-and-enrolment/02-enrolment-and-project-lifecycle.adoc), [`#enrolment-lifecycle-release`](file:///Users/oliver/git/pm-mcp-core/doc/specification/credentials-and-enrolment/02-enrolment-and-project-lifecycle.adoc), [`15-project-onboarding.adoc`](file:///Users/oliver/git/pm-mcp-core/doc/specification/state-catalogue/15-project-onboarding.adoc), [`12-machine-stores.adoc#explorer-store`](file:///Users/oliver/git/pm-mcp-core/doc/specification/store-schemas/12-machine-stores.adoc) |
| **Local Client Design** | Shared: [`doc/design/shared/project-explorer.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/design/shared/project-explorer.adoc), [`project-onboarding.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/design/shared/project-onboarding.adoc)<br>CLI: [`doc/design/terminal-client/20-projects.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/design/terminal-client/20-projects.adoc)<br>Web: [`doc/design/web-app/README.adoc#structure`](file:///Users/oliver/git/pm-mcp-clients/doc/design/web-app/README.adoc) |
| **Roadmap Packages** | [`doc/plans/project-contract.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/plans/project-contract.adoc), [`doc/plans/cli-projects.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/plans/cli-projects.adoc), [`doc/plans/web-projects.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/plans/web-projects.adoc) |
| **Implementation Recipe** | [`doc/meta/implement_use-cases.md`](file:///Users/oliver/git/pm-mcp-clients/doc/meta/implement_use-cases.md) |
| **Operational SOP** | [`doc/agy/runbook.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/runbook.md) |

---

## 2. Parallel Isolation & Non-Overlap Invariants (vs `UC-PLN-1 / UC-PLN-5`)

`UC-PRJ-1..3` executes concurrently with `UC-PLN-1 / UC-PLN-5: Plan View & Epic View` (`feat/uc-pln-1-plan-view`). Strict non-overlap is guaranteed across all repository modules:

1. **Contracts & API (`pm-api`)**:
   - `UC-PRJ-1..3`: Owns `de.planmarshall.api.project.*` and `messages/projects.{en,de}.properties`.
   - `UC-PLN-1 / UC-PLN-5`: Owns `de.planmarshall.api.process.*` and `messages/plan_view.{en,de}.properties`.
   - Zero shared model classes, enums, or resource properties.
2. **Mock Runtime (`pm-mock-runtime`)**:
   - `UC-PRJ-1..3`: Routes `/api/v1/projects?all`, `/api/v1/explorer/favourites`, `/api/v1/project-onboardings`, `/api/v1/projects/{id}/release`, scenario `scenarios/projects.json`.
   - `UC-PLN-1 / UC-PLN-5`: Routes `/api/v1/plans/{id}/process`, `/api/v1/plans/{id}/tasks/{taskId}/activity`, scenario `scenarios/plan-view.json`.
   - Disjoint route dispatch tables and scenario files.
3. **CLI Client (`pm-operator`)**:
   - `UC-PRJ-1..3`: Owns `ProjectCommand.java` (`project <id>`, `projects`, `project release`, `project unenrol`).
   - `UC-PLN-1 / UC-PLN-5`: Extends `PlanCommand.java` and `EpicCommand.java`.
   - Disjoint Picocli verb classes.
4. **Web Client (`pm-web-app`)**:
   - `UC-PRJ-1..3`: Route `/projects` with `ProjectExplorerView.svelte` and `ProjectOnboardingModal.svelte`.
   - `UC-PLN-1 / UC-PLN-5`: Routes `/plans/[id]` with `PlanView.svelte` and `/epics/[id]` with `EpicView.svelte`.
   - Disjoint Svelte components and route views.
5. **E2E & Integration Tests**:
   - `UC-PRJ-1..3`: `pm-web-e2e/tests/projects.spec.js` and `ProjectCommandTest.java`.
   - `UC-PLN-1 / UC-PLN-5`: `pm-web-e2e/tests/plan-view.spec.js` and `PlanViewCommandTest.java`.

---

## 3. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created on topic branch off latest `main` (`git worktree add -b feat/uc-prj-projects /Users/oliver/git/worktrees/pm-mcp-clients-uc-prj main`).
- [ ] 2. Domain skills & engineering personas loaded for each task type:
  - `persona-implementer` & `persona-module-tester` for code & unit tests
  - `persona-integration-tester` for multi-module/integration tests
  - `persona-security-expert` for path validation & project authorization checks
  - `persona-documenter` for documentation & ADRs
  - `persona-code-reviewer` for finding evaluation & adversarial refutation
- [ ] 3. Incremental implementation following the 6-Phase technical recipe with local tests passing.
- [ ] 4. Final completion report written (`doc/agy/uc-prj-plan-report.md`) and initial plan moved to `doc/agy/implemented/uc-prj-plan.md`.
- [ ] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit` for code/config; skipped for standalone documentation changes).
- [ ] 6. PR opened (with `--label "skip-bot-review"` if authorized), 5-minute review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [ ] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.

---

## 4. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Trace** | `plan-marshall-persona-documenter` | `plan-marshall-ref-code-quality` | Verify explorer DTO schema, project onboarding state machine, release semantics, catalogue keys. |
| **Phase 2: `pm-api`** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-module-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:junit-core` | Immutable Java 25 records, Jackson streaming parsing, bilingual resource bundles (`projects.{en,de}.properties`). |
| **Phase 3: Mock Runtime** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-integration-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:cui-cui-http-testing` | Stateful scenario JSON (`projects.json`), dynamic route handlers, SSE broadcast (`project.changed`, `project.enrolled`, `project.released`). |
| **Phase 4: `pm-operator`** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-security-expert` | `pm-dev-java:java-core`<br>`pm-dev-java:java-security` | `ProjectCommand` (`projects`, `project <id>`, `project release`, `project unenrol`), output safety, favorites ordering. |
| **Phase 5: `pm-web-app`** | `plan-marshall-persona-implementer`<br>`pm-dev-frontend:javascript` | `pm-dev-frontend:css`<br>`pm-dev-frontend:jest-testing` | Project explorer view, favourites/recent/all sections, onboarding dialog, release confirmation modal, Vitest tests. |
| **Phase 6: Verification** | `plan-marshall-persona-integration-tester` | `pm-dev-java:junit-integration` | `PmOperatorProcessIT` process execution, Playwright E2E browser test suite (`projects.spec.js`). |
| **Phase 7: Review Gate** | `plan-marshall-persona-code-reviewer` | `pm-dev-java:java-security` | Adversarial refutation of candidate bot findings, 100% resolution bar. |

---

## 5. Phased Task Breakdown

### Phase 1: Traceability & Design Alignment
- [ ] **1.1 Design Specification Audit**: Review `doc/design/shared/project-explorer.adoc`, `project-onboarding.adoc`, and `doc/plans/project-contract.adoc`:
  - Resource routes: `GET /api/v1/projects?all=true`, `PUT /api/v1/explorer/favourites`, `POST /api/v1/project-onboardings`, `GET /api/v1/projects/{id}/explorer`, `POST /api/v1/projects/{id}/release`, `POST /api/v1/projects/{id}/unenrol`.
  - SSE events: `project.changed`, `project.enrolled`, `project.released`, `entry.moved`.
  - Closed enumerations: `ProjectReadiness` (`READY`, `SETUP_NEEDS`, `RELEASED`), `ProjectOnboardingState` (`CHECKOUT`, `CONSENT`, `DETECT`, `PROBE`, `IDENTITY`, `ENROLMENT`), `ReleaseWaitStatus`.
  - Extract the complete set of keys needed for `messages/projects.{en,de}.properties`.

### Phase 2: `pm-api` Contracts & Message Catalogue
- [ ] **2.1 Project Data Records**: Implement immutable Java 25 records in `de.planmarshall.api.project`:
  - `EnrolledProject`: `projectId`, `displayName`, `remoteUrl`, `readiness`, `setupNeedsSummary`, `needsOperatorCount`, `activePlansCount`, `activeEpicsCount`, `lastActive`, `isFavourite`, `isReleased`.
  - `ProjectExplorer`: `favourites`, `recent`, `allProjects`, `beingAdded`.
  - `BeingAddedProject`: `checkoutPath`, `currentStep`, `stepNumber`, `totalSteps`, `continueLink`, `abandonLink`.
  - `FavouritesUpdateRequest`: ordered list of `projectIds`.
  - `ProjectOnboarding`: `onboardingId`, `checkoutPath`, `state`, `detectedDomains`, `discoveredModules`, `credentialsNeeded`.
  - `ProjectReleaseRequest`: `projectId`, `confirmRelease`, `force`.
  - `ProjectReleaseResult`: `projectId`, `plansEndedCount`, `epicsEndedCount`, `entriesDeletedCount`, `releasedAt`.
- [ ] **2.2 Project Events**: Implement SSE event payload records:
  - `ProjectChangedEvent`, `ProjectEnrolledEvent`, `ProjectReleasedEvent`.
- [ ] **2.3 Streaming Codecs**: Implement Jackson streaming codecs (`JsonParser` / `JsonGenerator`) via `JsonTree`.
- [ ] **2.4 Message Catalogue Entries**:
  - Populate `pm-api/src/main/resources/messages/projects.en.properties` with keys from `project-explorer.adoc#texts`.
  - Populate `pm-api/src/main/resources/messages/projects.de.properties` with German translations.
  - Register `"projects"` in `MessageCatalogue.SUPPORTED_AREAS`.
- [ ] **2.5 Unit Tests**:
  - `ProjectRecordsTest.java`: Round-trip serialization and deserialization of explorer models, onboarding state, and release responses.
  - `ProjectsCatalogueTest.java`: Key presence and ICU parameter validation across English and German bundles.

### Phase 3: `pm-mock-runtime` Scenarios & Handlers
- [ ] **3.1 Scenario Definition**: Create `pm-mock-runtime/src/main/resources/scenarios/projects.json`:
  - Enrolled projects: favourites (`★ pm-mcp-clients`, `★ pm-mcp-core`), recent (`plan-marshall`, `cui-http`), and released projects.
  - Being-added project fixture (`~/git/cui-parent`, step 2 of 10: Identity).
  - Setup-needs project fixture (`plan-marshall`, setup needs: credential sonar).
  - Release confirmation fixtures with active plans and epics.
- [ ] **3.2 Dynamic Route Handlers**: Extend `MockRuntimeEngine.java`:
  - `GET /api/v1/projects?all=true`: Returns full explorer list.
  - `PUT /api/v1/explorer/favourites`: Updates favourites ordering.
  - `POST /api/v1/project-onboardings`: Creates new project onboarding entry.
  - `POST /api/v1/projects/{id}/release`: Performs project release and broadcasts `project.released`.
  - `POST /api/v1/projects/{id}/unenrol`: Unenrols project if no work is active.
- [ ] **3.3 Scenario Registration**:
  - Add `PROJECTS = "projects"` constant in `StandardScenarios.java` and register in `ALL`.
- [ ] **3.4 Scenario Tests**:
  - `ProjectsScenarioTest.java` asserting scenario loading, REST responses, favourites reordering, and release execution.

### Phase 4: `pm-operator` Terminal Client Implementation
- [ ] **4.1 Projects Command Implementation**:
  - Implement `ProjectCommand.java`:
    - `pm-operator projects`: Renders 4-part explorer view: Favourites (★), Recent, Being added, and All projects with name filter (`--filter <name>`).
    - `pm-operator project <id>`: Shows project details, readiness, active work links, and setup status.
    - `pm-operator project release <id>`: Two-step confirmation dialogue listing what will end before executing release.
    - `pm-operator project unenrol <id>`: Unenrolment with safety check (`PM-SEC-3`).
- [ ] **4.2 Output Safety**:
  - Sanitize all project display names, checkout paths, remotes, and setup messages using `OutputSafety.stripControlSequences`.
- [ ] **4.3 CLI Unit & Transcript Tests**:
  - `ProjectCommandTest.java`: Validate explorer table formatting, filter arguments, release confirmation, and exit codes.

### Phase 5: `pm-web-app` Client Implementation
- [ ] **5.1 API Client Extensions**:
  - Add `getProjectExplorer()`, `updateFavourites(projectIds)`, `startProjectOnboarding(path)`, `releaseProject(id)`, `unenrolProject(id)` to `pm-web-app/src/lib/api/client.ts`.
  - Extend SSE event listener to handle `project.changed` and `project.released`.
- [ ] **5.2 Project Explorer View (`ProjectExplorerView.svelte`)**:
  - Layout matching `doc/design/shared/project-explorer.adoc`:
    - Favourites section with drag-to-reorder or star toggle.
    - Recent section with relative duration badges.
    - "Being added" cards with `[Continue]` and `[Abandon]` action buttons.
    - Filterable "All projects" list with sorting by name or most used.
    - `[+ Add a project]` button opening onboarding flow.
- [ ] **5.3 Release Confirmation Modal**:
  - Detailed impact modal showing plans, epics, and entries that will end, requiring explicit confirmation.
- [ ] **5.4 Responsive Styling**:
  - Responsive project cards adapting down to mobile width (320px).
- [ ] **5.5 Vitest Tests**:
  - `ProjectExplorerView.test.ts` verifying rendering, filtering, favourite toggling, onboarding launch, and release modal.

### Phase 6: Integration Testing & Verification
- [ ] **6.1 Playwright E2E Suite**: Create `pm-web-e2e/tests/projects.spec.js`:
  - Test viewing projects explorer.
  - Test filtering projects by name.
  - Test marking/unmarking a project as favourite.
  - Test releasing a project and verifying released status badge.
- [ ] **6.2 Operator Process IT**:
  - Add process test in `PmOperatorProcessIT.java` executing `projects` and `project release` against running `pm-mock-runtime`.
- [ ] **6.3 Domain Invariants Verification**:
  - Verify against checklist in Section 1 of `doc/meta/implement_use-cases.md`.

### Phase 7: Completion Report, Pre-Commit, Review Gate & Landing (Runbook Discipline)
- [ ] **7.1 Final Completion Report**:
  - Generate comprehensive completion report in `doc/agy/uc-prj-plan-report.md`.
- [ ] **7.2 Plan Archival**:
  - Move `doc/agy/uc-prj-plan.md` to `doc/agy/implemented/uc-prj-plan.md` and record entry in `doc/agy/implemented/README.md`.
- [ ] **7.3 Pre-Commit Gate**:
  - Run `./mvnw verify -Ppre-commit` to validate OpenRewrite formatting, Checkstyle, and full test suite.
- [ ] **7.4 Commit & PR**:
  - Commit changes with Conventional Commits message (`feat(projects): implement UC-PRJ-1..3 project explorer, onboarding, and release`).
  - Open PR via `gh pr create` (with `--label "skip-bot-review"` if authorized).
- [ ] **7.5 5-Minute Review Window**:
  - Schedule 300s timer (`schedule` tool) and await CI, SonarCloud, Sourcery, and CodeRabbit.
- [ ] **7.6 100% Findings Resolution**:
  - Evaluate all automated findings with `persona-code-reviewer` adversarial refutation and resolve 100% of issues.
- [ ] **7.7 Merge Queue & Cleanup**:
  - Enqueue PR via `gh pr merge --auto --squash`.
  - Upon merge, synchronize `main`, remove worktree and delete topic branch.
- [ ] **7.8 Roadmap Archival**:
  - Update `doc/plans/README.adoc` and move completed package documentation (`project-contract.adoc`, `cli-projects.adoc`, `web-projects.adoc`) to `doc/plans/implemented/`.
