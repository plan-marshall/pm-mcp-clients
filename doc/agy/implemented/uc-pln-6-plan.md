# Implementation Plan: UC-PLN-6 Catch Up on What Happened (Reports & Completion Reports)

This plan coordinates the end-to-end implementation of use case `UC-PLN-6: Catch Up on What Happened` across all client and runtime modules in `pm-mcp-clients`.

It operationalizes the technical recipe from [`doc/meta/implement_use-cases.md`](file:///Users/oliver/git/pm-mcp-clients/doc/meta/implement_use-cases.md) under the execution discipline of [`doc/agy/runbook.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/runbook.md).

---

## 1. Traceability & Context

| Dimension | Reference |
|---|---|
| **Use Case** | [`plan-marshall-documentation/doc/use-cases/03-plans.adoc#UC-PLN-6`](file:///Users/oliver/git/plan-marshall-documentation/doc/use-cases/03-plans.adoc) |
| **Specifications** | [`01-processes-and-transport.adoc#reports`](file:///Users/oliver/git/plan-marshall-documentation/doc/specification/runtime-model/01-processes-and-transport.adoc), [`#completion-report`](file:///Users/oliver/git/plan-marshall-documentation/doc/specification/runtime-model/01-processes-and-transport.adoc), [`06-event-catalogue.adoc#event-catalogue`](file:///Users/oliver/git/plan-marshall-documentation/doc/specification/runtime-model/06-event-catalogue.adoc), [`12-machine-stores.adoc#report-journal`](file:///Users/oliver/git/pm-mcp-core/doc/specification/store-schemas/12-machine-stores.adoc), [`01-profile-schema.adoc#token-usage`](file:///Users/oliver/git/pm-mcp-core/doc/specification/client-profiles/01-profile-schema.adoc) |
| **Local Client Design** | Shared: [`doc/design/shared/reports.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/design/shared/reports.adoc)<br>Completion: [`doc/design/shared/completion-report.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/design/shared/completion-report.adoc)<br>CLI: [`doc/design/terminal-client/24-reports.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/design/terminal-client/24-reports.adoc)<br>Web: [`doc/design/web-app/README.adoc#structure`](file:///Users/oliver/git/pm-mcp-clients/doc/design/web-app/README.adoc) |
| **Roadmap Packages** | [`doc/plans/report-contract.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/plans/report-contract.adoc), [`doc/plans/cli-reports.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/plans/cli-reports.adoc), [`doc/plans/web-reports.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/plans/web-reports.adoc) |
| **Implementation Recipe** | [`doc/meta/implement_use-cases.md`](file:///Users/oliver/git/pm-mcp-clients/doc/meta/implement_use-cases.md) |
| **Operational SOP** | [`doc/agy/runbook.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/runbook.md) |

---

## 2. Parallel Isolation & Non-Overlap Invariants (vs `UC-PLN-4`)

`UC-PLN-6` executes in parallel with `UC-PLN-4: See What Runs, Across Projects` (`feat/uc-pln-4-running-work`). Strict non-overlap is guaranteed across all repository modules:

1. **Contracts & API (`pm-api`)**:
   - `UC-PLN-4`: Owns `de.planmarshall.api.overview.*` and `messages/overview.{en,de}.properties`.
   - `UC-PLN-6`: Owns `de.planmarshall.api.report.*` and `messages/reports.{en,de}.properties`.
   - Zero shared model classes or resource properties.
2. **Mock Runtime (`pm-mock-runtime`)**:
   - `UC-PLN-4`: Routes `/api/v1/projects*`, scenario `scenarios/running-work.json`.
   - `UC-PLN-6`: Routes `/api/v1/reports*` and `/api/v1/plans/{id}/completion`, scenario `scenarios/reports.json`.
   - Disjoint route dispatch tables and scenario files.
3. **CLI Client (`pm-operator`)**:
   - `UC-PLN-4`: Owns `StatusCommand.java` and `PlansCommand.java`.
   - `UC-PLN-6`: Owns `ReportsCommand.java` and `PlanCommand.ReportCommand`.
   - Disjoint Picocli verb classes.
4. **Web Client (`pm-web-app`)**:
   - `UC-PLN-4`: Route `/` or `/overview` with `OverviewView.svelte`.
   - `UC-PLN-6`: Route `/reports` with `ReportsView.svelte`, and plan completion modal/view.
   - Disjoint Svelte components and route views.
5. **E2E & Integration Tests**:
   - `UC-PLN-4`: `pm-web-e2e/tests/overview.spec.js` and `OverviewCommandTest.java`.
   - `UC-PLN-6`: `pm-web-e2e/tests/reports.spec.js` and `ReportsCommandTest.java`.

---

## 3. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created on topic branch off latest `main` (`git worktree add -b feat/uc-pln-6-reports /Users/oliver/git/worktrees/pm-mcp-clients-uc-pln-6 main`).
- [x] 2. Domain skills & engineering personas loaded for each task type:
  - `persona-implementer` & `persona-module-tester` for code & unit tests
  - `persona-integration-tester` for multi-module/integration tests
  - `persona-security-expert` for untrusted text sanitization & output safety
  - `persona-documenter` for documentation & ADRs
  - `persona-code-reviewer` for finding evaluation & adversarial refutation
- [ ] 3. Incremental implementation following the 6-Phase technical recipe with local tests passing.
- [ ] 4. Final completion report written (`doc/agy/uc-pln-6-plan-report.md`) and initial plan moved to `doc/agy/implemented/uc-pln-6-plan.md`.
- [ ] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit` for code/config; skipped for standalone documentation changes).
- [ ] 6. PR opened (with `--label "skip-bot-review"` if authorized), 5-minute review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [ ] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.

---

## 4. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Trace** | `plan-marshall-persona-documenter` | `plan-marshall-ref-code-quality` | Verify report kinds enum, completion report schemas, token usage metrics, catalogue keys. |
| **Phase 2: `pm-api`** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-module-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:junit-core` | Immutable Java 25 records, Jackson streaming parsing, bilingual resource bundles (`reports.{en,de}.properties`). |
| **Phase 3: Mock Runtime** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-integration-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:cui-cui-http-testing` | Stateful scenario JSON (`reports.json`), dynamic route handlers, SSE broadcast (`report.sent`, `report.read`). |
| **Phase 4: `pm-operator`** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-security-expert` | `pm-dev-java:java-core`<br>`pm-dev-java:java-security` | `ReportsCommand`, `PlanCommand.ReportCommand`, output safety, hierarchical tree grouping, unread count line. |
| **Phase 5: `pm-web-app`** | `plan-marshall-persona-implementer`<br>`pm-dev-frontend:javascript` | `pm-dev-frontend:css`<br>`pm-dev-frontend:jest-testing` | Reports view, grouped list, quiet notice banner, completion report modal/view, token formatting, Vitest tests. |
| **Phase 6: Verification** | `plan-marshall-persona-integration-tester` | `pm-dev-java:junit-integration` | `PmOperatorProcessIT` process execution, Playwright E2E browser test suite (`reports.spec.js`). |
| **Phase 7: Review Gate** | `plan-marshall-persona-code-reviewer` | `pm-dev-java:java-security` | Adversarial refutation of candidate bot findings, 100% resolution bar. |

---

## 5. Phased Task Breakdown

### Phase 1: Traceability & Design Alignment
- [x] **1.1 Design Specification Audit**: Review `doc/design/shared/reports.adoc` and `doc/design/shared/completion-report.adoc`:
  - Resource routes: `GET /api/v1/reports?state=unread&older=true`, `POST /api/v1/reports/{id}/read`, `POST /api/v1/reports/read`, `GET /api/v1/plans/{id}/completion`.
  - SSE events: `GET /api/v1/reports/events` with events `report.sent`, `report.read`.
  - Closed enumeration of 15 report kinds: `plan.started`, `plan.ended`, `plan.revised`, `plan.hold_released`, `epic.launchable`, `epic.waits_overlap`, `epic.overlap_found`, `epic.recut_applied`, `epic.through`, `epic.ended`, `project.onboarded`, `project.released`, `source.flagged`, `decision.closed_elsewhere`, `task.off_track`, plus `system.cleanup_due`.
  - Completion report structure: outcome, reason, ran duration/start/end/paused, deliverables (states: done, not done, dropped), re-planning history, decisions asked and answered, tokens usage (total, split in/out, coverage, floor `≥`, by phase, by role, by model, waste breakdown).
  - Extract the complete set of keys needed for `messages/reports.{en,de}.properties`.

### Phase 2: `pm-api` Contracts & Message Catalogue
- [x] **2.1 Report Data Records**: Implement immutable Java 25 records in `de.planmarshall.api.report`:
  - `ReportKind`: Closed enum matching specification.
  - `ReportFrom`: `projectId`, `epicId`, `planId` with optional display titles and hypermedia links.
  - `ReportItem`: `id` (`rep-NNNN`), `from`, `kind`, `subject`, `subjectParams`, `body`, `createdAt`, `sentAt`, `readAt`, `readChannel`, `readDevice`, `detailLink`.
  - `ReportsPage`: `items`, `unreadCount`, `oldestUnreadTime`, `hasOlder`, `olderCount`.
  - `ReadReportsRequest`: list of `reportIds` and boolean `all`.
  - `ReadReportsResponse`: `markedCount`.
- [x] **2.2 Completion Report & Token Records**:
  - `PlanCompletionReport`: `planId`, `title`, `from`, `outcome`, `terminalReason`, `pullRequestNumber`, `reason`, `startedAt`, `endedAt`, `durationSeconds`, `pausedSeconds`, `pausedBy`, `deliverables`, `replanningRecords`, `decisionRecords`, `tokenUsage`.
  - `DeliverableSummary`: `name`, `state` (`DONE`, `NOT_DONE`, `DROPPED`).
  - `TokenUsageRecord`: `totalTokens`, `inputTokens`, `outputTokens`, `durationSeconds`, `tasksMeasured`, `tasksTotal`, `isFloor`, `byPhase`, `byRole`, `byModel`, `wasteRecords`.
  - `TokenBreakdownItem`: `label`, `tokens`, `inputTokens`, `outputTokens`, `unmeasuredTasksCount`.
  - `TokenWasteRecord`: `wasteClass` (`NO_PRODUCT`, `RETRIES`, `REMEDIATION`), `tokens`, `sharePercent`, `tasksCount`, `causeSummary`.
- [x] **2.3 Report Events**: Implement SSE event payload records:
  - `ReportSentEvent`, `ReportReadEvent`.
- [x] **2.4 Streaming Codecs**: Implement Jackson streaming codecs (`JsonParser` / `JsonGenerator`) via `JsonTree`.
- [x] **2.5 Message Catalogue Entries**:
  - Populate `pm-api/src/main/resources/messages/reports.en.properties` with keys from `reports.adoc#texts` and `completion-report.adoc#texts`.
  - Populate `pm-api/src/main/resources/messages/reports.de.properties` with German translations.
  - Register `"report"` in `MessageCatalogue.SUPPORTED_AREAS`.
- [x] **2.6 Unit Tests**:
  - `ReportRecordsTest.java`: Round-trip serialization and deserialization of reports and completion reports.
  - `ReportCatalogueTest.java`: Key presence and ICU parameter validation across English and German bundles; test asserting every `ReportKind` has its subject key.

### Phase 3: `pm-mock-runtime` Scenarios & Handlers
- [x] **3.1 Scenario Definition**: Create `pm-mock-runtime/src/main/resources/scenarios/reports.json`:
  - Rich unread report fixtures across 2 projects, 1 epic, and 1 standalone plan.
  - Fixtures for every report kind.
  - Pagination fixture (multiple pages of reports with `older`).
  - Completion report fixtures for ended plans (`fix-relay`, `docs-typo`) with full, partial, and unmeasured token figures.
- [x] **3.2 Dynamic Route Handlers**: Extend `MockRuntimeEngine.java`:
  - `GET /api/v1/reports`: Returns unread reports (paged, newest first) with `unread_count` and oldest unread timestamp.
  - `POST /api/v1/reports/{id}/read`: Marks single report read and broadcasts `report.read` SSE event.
  - `POST /api/v1/reports/read`: Marks specified report IDs as read.
  - `GET /api/v1/plans/{id}/completion`: Returns completion report for ended plans; returns 409/400 for active plans.
  - `GET /api/v1/reports/events`: SSE stream for reports.
- [x] **3.3 Scenario Registration**:
  - Add `REPORTS = "reports"` constant in `StandardScenarios.java` and register in `ALL`.
- [x] **3.4 Scenario Tests**:
  - `ReportsScenarioTest.java` asserting scenario loading, REST responses, read transitions, and completion report representations.

### Phase 4: `pm-operator` Terminal Client Implementation
- [x] **4.1 Reports Command Refactoring**:
  - Update `ReportsCommand.java` to use typed `pm-api` records and `MessageCatalogue`.
  - Hierarchical grouping: group unread reports by project, then epic, then plan.
  - Show created timestamp and sent timestamp (only if differing by >1 min).
  - Support `reports --project <id>`, `reports --older`.
  - Refactor `reports read <id>...` and `reports read --all` (marking only previously listed reports read).
  - Implement `reports open <id>` executing the detail command and marking report read.
- [x] **4.2 Plan Report Command**:
  - Implement `plan report <plan_id>` in `PlanCommand.ReportCommand`.
  - Render outcome, reason, ran time, deliverables list with unicode marks (`✓`, `○`, `–`), re-planning, and decisions.
  - Implement `--tokens` section and `--components` raw token metrics.
  - Format numbers according to token number specifications (e.g. `≥ 412 k`, `13 % of measured`).
- [x] **4.3 Console Arrival Notice & Quiet Push**:
  - Print unread reports arrival summary line on startup (`▪ 5 unread reports since yesterday 18:40 · reports shows them`).
  - Print quiet notice line above prompt when new report SSE arrives while console is open.
- [x] **4.4 Output Safety**:
  - Sanitize all untrusted text in subjects, bodies, and plan titles using `OutputSafety.stripControlSequences`.
- [x] **4.5 CLI Unit & Transcript Tests**:
  - Refactor `ReportsCommandTest.java` asserting table grouping, read commands, arrival line, and exit codes.

### Phase 5: `pm-web-app` Client Implementation
- [x] **5.1 API Client Extensions**:
  - Add `getReports(older?: boolean)`, `markReportRead(reportId: string)`, `markAllReportsRead(ids: string[])`, `getPlanCompletion(planId: string)` to `pm-web-app/src/lib/api/client.ts`.
  - Extend SSE event listener to handle `report.sent` and `report.read`.
- [x] **5.2 Reports View (`ReportsView.svelte`)**:
  - Create `/reports` view:
    - Unread reports count header with `[Mark all shown read]` button.
    - Grouped list by project, epic, and plan.
    - Report card: from path with links, glyph/color by subject type, subject, body, created/sent times.
    - Actions: `[Show the details]` (opens detail and marks read), `[Mark read]`.
    - `[Show older]` pagination button when older reports exist.
    - Empty state: "Nothing new. Reports you have read are not shown any more."
- [x] **5.3 Navigation Frame Integration**:
  - Add "Reports" entry in navigation header/sidebar with unread count badge.
  - Arrival banner when opening app with unread reports: `5 unread reports since yesterday 18:40 [Show them]`.
  - Quiet notice: polite live region (`aria-live="polite"`) for incoming reports while app is open.
- [x] **5.4 Completion Report View / Modal**:
  - Layout matching `doc/design/shared/completion-report.adoc`.
  - Outcome badge, run duration, deliverables checklist, re-planning history, decisions summary.
  - Tokens section with total, split in/out, coverage, phase/role/model breakdown, and waste share.
- [x] **5.5 Responsive Styling**:
  - Mobile card layout (320px+) with stacked actions and accessible touch targets.
- [x] **5.6 Vitest Tests**:
  - `ReportsView.test.ts` verifying rendering, grouping, mark-read interaction, live SSE updates, and pagination.

### Phase 6: Integration Testing & Verification
- [x] **6.1 Playwright E2E Suite**: Create `pm-web-e2e/tests/reports.spec.js`:
  - Test viewing unread reports list grouped by project/epic/plan.
  - Test marking a single report read and verifying removal from list.
  - Test marking all shown read.
  - Test opening report detail leading to plan completion report.
  - Test live removal when report is marked read via SSE.
- [x] **6.2 Operator Process IT**:
  - Add process test in `PmOperatorProcessIT.java` executing `reports` and `reports read --all` against running `pm-mock-runtime`.
- [x] **6.3 Domain Invariants Verification**:
  - Verify against checklist in Section 1 of `doc/meta/implement_use-cases.md`.

### Phase 7: Completion Report, Pre-Commit, Review Gate & Landing (Runbook Discipline)
- [x] **7.1 Final Completion Report**:
  - Generate comprehensive completion report in `doc/agy/uc-pln-6-plan-report.md`.
- [x] **7.2 Plan Archival**:
  - Move `doc/agy/uc-pln-6-plan.md` to `doc/agy/implemented/uc-pln-6-plan.md` and record entry in `doc/agy/implemented/README.md`.
- [x] **7.3 Pre-Commit Gate**:
  - Run `./mvnw verify -Ppre-commit` to validate OpenRewrite formatting, Checkstyle, and full test suite.
- [ ] **7.4 Commit & PR**:
  - Commit changes with Conventional Commits message (`feat(reports): implement UC-PLN-6 catch up on what happened`).
  - Open PR via `gh pr create` (with `--label "skip-bot-review"` if authorized).
- [ ] **7.5 5-Minute Review Window**:
  - Schedule 300s timer (`schedule` tool) and await CI, SonarCloud, Sourcery, and CodeRabbit.
- [ ] **7.6 100% Findings Resolution**:
  - Evaluate all automated findings with `persona-code-reviewer` adversarial refutation and resolve 100% of issues.
- [ ] **7.7 Merge Queue & Cleanup**:
  - Enqueue PR via `gh pr merge --auto --squash`.
  - Upon merge, synchronize `main`, remove worktree and delete topic branch.
- [x] **7.8 Roadmap Archival**:
  - Update `doc/plans/README.adoc` and move completed package documentation (`report-contract.adoc`, `cli-reports.adoc`, `web-reports.adoc`) to `doc/plans/implemented/`.
