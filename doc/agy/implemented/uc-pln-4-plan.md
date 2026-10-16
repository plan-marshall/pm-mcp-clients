# Implementation Plan: UC-PLN-4 See What Runs, Across Projects

This plan coordinates the end-to-end implementation of use case `UC-PLN-4: See What Runs, Across Projects` (the Overview of Running Work) across all client and runtime modules in `pm-mcp-clients`.

It operationalizes the technical recipe from [`doc/meta/implement_use-cases.md`](file:///Users/oliver/git/pm-mcp-clients/doc/meta/implement_use-cases.md) under the execution discipline of [`doc/agy/runbook.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/runbook.md).

---

## 1. Traceability & Context

| Dimension | Reference |
|---|---|
| **Use Case** | [`plan-marshall-documentation/doc/use-cases/03-plans.adoc#UC-PLN-4`](file:///Users/oliver/git/plan-marshall-documentation/doc/use-cases/03-plans.adoc) |
| **Specifications** | [`01-processes-and-transport.adoc#overview-resource`](file:///Users/oliver/git/plan-marshall-documentation/doc/specification/runtime-model/01-processes-and-transport.adoc), [`#needs-operator`](file:///Users/oliver/git/plan-marshall-documentation/doc/specification/runtime-model/01-processes-and-transport.adoc), [`#ended-work`](file:///Users/oliver/git/plan-marshall-documentation/doc/specification/runtime-model/01-processes-and-transport.adoc), [`06-event-catalogue.adoc`](file:///Users/oliver/git/plan-marshall-documentation/doc/specification/runtime-model/06-event-catalogue.adoc) |
| **Local Client Design** | Shared: [`doc/design/shared/plan-overview.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/design/shared/plan-overview.adoc)<br>CLI: [`doc/design/terminal-client/15-overview.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/design/terminal-client/15-overview.adoc)<br>Web: [`doc/design/web-app/README.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/design/web-app/README.adoc) |
| **Roadmap Packages** | [`doc/plans/overview-contract.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/plans/overview-contract.adoc), [`doc/plans/cli-overview.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/plans/cli-overview.adoc), [`doc/plans/web-overview.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/plans/web-overview.adoc) |
| **Implementation Recipe** | [`doc/meta/implement_use-cases.md`](file:///Users/oliver/git/pm-mcp-clients/doc/meta/implement_use-cases.md) |
| **Operational SOP** | [`doc/agy/runbook.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/runbook.md) |

---

## 2. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created on topic branch off latest `main` (`git worktree add -b feat/uc-pln-4-running-work ...`).
- [/] 2. Domain skills & engineering personas loaded for each task type:
  - `persona-implementer` & `persona-module-tester` for code & unit tests
  - `persona-integration-tester` for multi-module/integration tests
  - `persona-security-expert` for trust boundaries & input validation
  - `persona-documenter` for documentation & ADRs
  - `persona-code-reviewer` for finding evaluation & adversarial refutation
- [ ] 3. Incremental implementation following the 6-Phase technical recipe with local tests passing.
- [ ] 4. Final completion report written (`doc/agy/uc-pln-4-plan-report.md`) and initial plan moved to `doc/agy/implemented/uc-pln-4-plan.md`.
- [ ] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit` for code/config; skipped for standalone documentation changes).
- [ ] 6. PR opened (with `--label "skip-bot-review"` if authorized), 5-minute review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [ ] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.

---

## 3. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Trace** | `plan-marshall-persona-documenter` | `plan-marshall-ref-code-quality` | Verify endpoint schemas, event types, link relations, catalogue text keys. |
| **Phase 2: `pm-api`** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-module-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:junit-core` | Immutable Java 25 records, Jackson streaming parsing, bilingual resource bundles. |
| **Phase 3: Mock Runtime** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-integration-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:cui-cui-http-testing` | Stateful scenario JSON (`running-work.json`), dynamic route handlers, SSE broadcast. |
| **Phase 4: `pm-operator`** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-security-expert` | `pm-dev-java:java-core`<br>`pm-dev-java:java-security` | Picocli subcommands (`plans`, `status`), `OutputSafety`, JLine prompts, layout rendering. |
| **Phase 5: `pm-web-app`** | `plan-marshall-persona-implementer`<br>`pm-dev-frontend:javascript` | `pm-dev-frontend:css`<br>`pm-dev-frontend:jest-testing` | Running Work view, by-plan/by-epic views, attention badges, ended work modal, Vitest tests. |
| **Phase 6: Verification** | `plan-marshall-persona-integration-tester` | `pm-dev-java:junit-integration` | `PmOperatorProcessIT` process execution, Playwright E2E browser test suite. |
| **Phase 7: Review Gate** | `plan-marshall-persona-code-reviewer` | `pm-dev-java:java-security` | Adversarial refutation of candidate bot findings, 100% resolution bar. |

---

## 4. Phased Task Breakdown

### Phase 1: Traceability & Design Alignment
- [x] **1.1 Design Specification Audit**: Review `doc/design/shared/plan-overview.adoc` and verify:
  - Resource routes: `GET /api/v1/projects`, `GET /api/v1/projects/{id}`, `GET /api/v1/projects/{id}/ended`.
  - Closed enumerations: `NeedsOperatorKind` (`decision`, `waiver`, `waits_for_you`, `stalled`, `condition`, `integrity_pause`, `held`, `landing_failed`, `setup`), `PlanActivity` (`running`, `pausing`, `paused`, `waiting_for_you`, `held`), `HoldKind`, `EndedOutcome`, `EpicTerminalReason`.
  - SSE events: `project.work_started`, `project.work_ended`, `project.changed`, `phase.changed`, `activity.changed`, `needs_operator.changed`, `condition.raised`, `condition.cleared`, `hold.opened`, `hold.changed`, `hold.cleared`, `epic.row_changed`, `task.off_track`.
  - Text keys: Extract the complete set of keys needed for `messages/overview.{en,de}.properties`.

### Phase 2: `pm-api` Contracts & Message Catalogue
- [x] **2.1 Data Records**: Implement immutable Java 25 records in `de.planmarshall.api.overview`:
  - `ProjectSummary` (`projectId`, `displayName`, `activePlansCount`, `activeEpicsCount`, `needsOperatorCount`, `needsOperatorSummary`, `setupNeedsOperator`)
  - `ProjectOverviewResponse` (list of `ProjectSummary`)
  - `ActivePlanSummary` (`planId`, `title`, `currentPhase`, `phaseState`, `phaseEnteredAt`, `runningSince`, `tasksDone`, `tasksTotal`, `epicId`, `pullRequestNumber`, `offTrackSignal`, `needsOperatorMarker`)
  - `ActiveEpicSummary` (`epicId`, `title`, `runningSince`, `runningPlansCount`, `stagedPlansCount`, `needsOperatorMarker`, `plans`)
  - `ProjectDetailResponse` (`projectId`, `plans`, `epics`, `alongsideLandings`)
  - `EndedWorkItem` (`id`, `kind`, `title`, `outcome`, `terminalReason`, `pullRequestNumber`, `reason`, `endedAt`)
  - `EndedWorkPage` (`items`, `page`, `totalPages`, `hasNewer`, `hasOlder`)
- [x] **2.2 Overview Events**: Implement event payload records:
  - `ProjectWorkChangedEvent`, `PhaseChangedEvent`, `ActivityChangedEvent`, `NeedsOperatorChangedEvent`, `TaskOffTrackEvent`.
- [x] **2.3 Streaming Codecs**: Implement Jackson streaming codecs via `JsonTree`.
- [x] **2.4 Message Catalogue Entries**:
  - Populate `pm-api/src/main/resources/messages/overview.en.properties` with keys from `plan-overview.adoc#texts`.
  - Populate `pm-api/src/main/resources/messages/overview.de.properties` with German translations.
  - Register `"overview"` in `MessageCatalogue.SUPPORTED_AREAS`.
- [x] **2.5 Unit Tests**:
  - `OverviewRecordsTest.java`: Round-trip serialization and deserialization.
  - `OverviewCatalogueTest.java`: Key presence and ICU parameter validation across English and German bundles.

### Phase 3: `pm-mock-runtime` Scenarios & Handlers
- [x] **3.1 Scenario Definition**: Create `pm-mock-runtime/src/main/resources/scenarios/running-work.json`:
  - Projects: `pm-mcp-clients` (plans needing operator: decision, stalled hold; epic with staged plans), `pm-mcp-core` (running plan, worker off track), `plan-marshall` (setup question only).
  - Ended work fixtures for each project with diverse outcomes (`merged`, `cancelled`, `aborted`).
- [x] **3.2 Dynamic Route Handlers**: Extend `MockRuntimeEngine.java`:
  - `GET /api/v1/projects`: Returns active projects sorted with needs-operator first, then by activity.
  - `GET /api/v1/projects/{projectId}`: Returns plans, epics, and short landings.
  - `GET /api/v1/projects/{projectId}/ended`: Returns paginated ended work items.
  - Broadcast appropriate SSE events on state changes.
- [x] **3.3 Scenario Registration**:
  - Add `RUNNING_WORK = "running-work"` constant in `StandardScenarios.java` and register in `ALL`.
- [x] **3.4 Scenario Tests**:
  - `RunningWorkScenarioTest.java` asserting scenario loading, REST responses, and event emissions.

### Phase 4: `pm-operator` Terminal Client Implementation
- [x] **4.1 Status & Plans Commands**:
  - `pm-operator status`: Hierarchical overview tree of running projects, plans, and epics with attention markers (`!`).
  - `pm-operator status --project <id>`: Filter running work to a single project.
  - `pm-operator status --by-epic`: Group project plans under their parent epics.
  - `pm-operator status --ended`: Show ended work page.
  - `pm-operator plans`: Interactive listing / selection view.
- [x] **4.2 Output Safety & Visual Distinction**:
  - Render glyphs (`▸` for plan, `◇` for epic, `!` for needs operator) with ANSI fallback.
  - Sanitize all project names, plan titles, and reasons using `OutputSafety.stripControlSequences`.
  - Bind all labels, durations, progress formats, and badges to `MessageCatalogue`.
- [x] **4.3 CLI Unit & Transcript Tests**:
  - `OverviewCommandTest.java`: Validate table layout, glyph formatting, arguments, and exit codes using `FakeRuntimeServer`.

### Phase 5: `pm-web-app` Client Implementation
- [x] **5.1 API Client Extensions**:
  - Add `getProjects()`, `getProjectDetail(id)`, and `getEndedWork(id, page)` to `pm-web-app/src/lib/api/client.ts`.
  - Extend SSE event listener to handle overview updates (`project.changed`, `phase.changed`, `activity.changed`, `needs_operator.changed`).
- [x] **5.2 Running Work View (`OverviewView.svelte`)**:
  - Implement two-level view:
    - Left column / top bar: Projects list with `!` attention count badge.
    - Main panel: Project detail with toggle for `by plan` vs `by epic`.
  - Plan card/row: Phase indicator, duration in phase, progress (`7 of 12 tasks`), off-track warning badge, pull request link.
  - Epic card/row: Running and staged plan counters (`2 running · 3 staged`), nested plans.
  - Alongside section: Short landings (e.g. `removing moved documents`).
- [x] **5.3 Ended Work Modal / View**:
  - Dedicated paged table showing ended plans and epics with outcome badges (`merged`, `cancelled`, etc.), reason, timestamp, and pagination buttons (`Newer` / `Older`).
- [x] **5.4 Responsive Design**:
  - Responsive layout adapting down to mobile width (320px) using responsive cards/accordion.
- [x] **5.5 Vitest Tests**:
  - `OverviewView.test.ts` verifying project selection, view switching (`by-plan` / `by-epic`), badge display, and ended work pagination.

### Phase 6: Integration Testing & Verification
- [x] **6.1 Playwright E2E Suite**: Create `pm-web-e2e/tests/overview.spec.js`:
  - Test viewing projects list and selecting a project.
  - Test toggling between `by plan` and `by epic` views.
  - Test viewing attention markers (`!`) and off-track indicator.
  - Test navigating to ended work, verifying outcome display and page navigation.
- [x] **6.2 Operator Process IT**:
  - Add process test in `PmOperatorProcessIT.java` executing `status` against running `pm-mock-runtime`.
- [x] **6.3 Domain Invariants Verification**:
  - Verify against checklist in Section 5 of `doc/meta/implement_use-cases.md`.

### Phase 7: Completion Report, Pre-Commit, Review Gate & Landing (Runbook Discipline)
- [x] **7.1 Final Completion Report**:
  - Generate comprehensive completion report in `doc/agy/uc-pln-4-plan-report.md`.
- [x] **7.2 Plan Archival**:
  - Move `doc/agy/uc-pln-4-plan.md` to `doc/agy/implemented/uc-pln-4-plan.md` and record entry in `doc/agy/implemented/README.md`.
- [x] **7.3 Pre-Commit Gate**:
  - Run `./mvnw verify -Ppre-commit` to validate OpenRewrite formatting, Checkstyle, and full test suite.
- [x] **7.4 Commit & PR**:
  - Commit changes with Conventional Commits message (`feat(overview): implement UC-PLN-4 see what runs across projects`).
  - Open PR via `gh pr create` (with `--label "skip-bot-review"` if authorized).
- [x] **7.5 5-Minute Review Window**:
  - Schedule 300s timer (`schedule` tool) and await CI, SonarCloud, Sourcery, and CodeRabbit.
- [x] **7.6 100% Findings Resolution**:
  - Evaluate all automated findings with `persona-code-reviewer` adversarial refutation and resolve 100% of issues.
- [x] **7.7 Merge Queue & Cleanup**:
  - Enqueue PR via `gh pr merge --auto --squash`.
  - Upon merge, synchronize `main`, remove worktree and delete topic branch.
- [x] **7.8 Roadmap Archival**:
  - Update `doc/plans/README.adoc` and move completed package documentation to `doc/plans/implemented/`.
