# Implementation Completion Report: UC-PLN-4 See What Runs, Across Projects

**Execution Date**: 2026-10-16  
**Topic Branch**: `feat/uc-pln-4-running-work`  
**Execution Plan**: [`doc/agy/implemented/uc-pln-4-plan.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/implemented/uc-pln-4-plan.md)  
**Governing Runbook**: [`doc/agy/runbook.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/runbook.md)  
**Implementation Recipe**: [`doc/meta/implement_use-cases.md`](file:///Users/oliver/git/pm-mcp-clients/doc/meta/implement_use-cases.md)  

---

## 1. Executive Summary

Use case **UC-PLN-4: See What Runs, Across Projects** (Overview of Running Work) has been implemented across the entire `pm-mcp-clients` repository, fulfilling all requirements for viewing active plans, epics, alongside landings, attention markers, and ended work across multiple enrolled projects from both the terminal client (`pm-operator`) and browser web app (`pm-web-app`), with authoritative state coordination provided by `pm-api` and simulated via `pm-mock-runtime`.

Key achievements include:
- **"Both clients, one state"**: Real-time overview of running work across projects, plans, and epics with synchronized representation in CLI (`pm-operator status`, `pm-operator plans`) and web (`OverviewView.svelte`).
- **Attention & Safety Priority**: Projects and plans needing operator attention (`!`) are sorted to the top and clearly distinguished with attention badges naming the specific reason (`decision`, `stalled`, `waiver`, `setup`). Off-track warnings highlight repeating cycles or drift.
- **Hierarchical & Epic Grouping**: Both clients support switching between flat plan-centric views and structured epic-centric views (`by plan` vs `by epic` / `pm-operator status --by-epic`), showing running vs staged plan counters.
- **Alongside Landings & Ended Work Archive**: Short landings (e.g. git removals, automated fixes) are surfaced alongside active work, and historical ended plans and epics are accessible via a paged view with outcome badges (`merged`, `cancelled`, `aborted`, etc.) and bidirectional pagination (`Newer` / `Older`).
- **Security & Output Safety**: Control sequence and ANSI stripping via `OutputSafety.stripControlSequences` prevents terminal escape injection from uncurated project names or plan titles.
- **Bilingual ICU Message Catalogue**: All user-facing strings are externalized in `messages/overview.{en,de}.properties` and formatted via `MessageCatalogue` with full ICU plural and select block support.
- **Zero Regressions & Comprehensive Verification**: 100% test pass rate across `pm-api` (150 tests), `pm-mock-runtime` (101 tests), `pm-operator` (272 unit tests, 4 process IT tests), `pm-web-app` (38 vitest tests), and `pm-web-e2e` (20 Playwright E2E tests).

---

## 2. Delivered Artifacts

### 2.1 `pm-api` (Contracts, Events & Message Catalogue)
- [`NeedsOperatorKind.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/overview/NeedsOperatorKind.java): Enum of all operator intervention conditions (`DECISION`, `WAIVER_REQUEST`, `OPERATOR_WAIT`, `INTEGRITY_PAUSE`, `STALLED`, `CONDITION`, `HELD`, `LANDING_FAILED`, `FINDINGS`, `SETUP`).
- [`PlanActivity.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/overview/PlanActivity.java), [`EndedOutcome.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/overview/EndedOutcome.java), [`EpicTerminalReason.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/overview/EpicTerminalReason.java): Domain enums for plan execution phases, terminal outcomes, and epic completion states.
- [`AlongsideLanding.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/overview/AlongsideLanding.java): Immutable record representing background landings alongside active work.
- [`ProjectSummary.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/overview/ProjectSummary.java), [`ProjectOverviewResponse.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/overview/ProjectOverviewResponse.java): Container records for project listings with attention count badges and active counters.
- [`ActivePlanSummary.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/overview/ActivePlanSummary.java), [`ActiveEpicSummary.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/overview/ActiveEpicSummary.java), [`ProjectDetailResponse.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/overview/ProjectDetailResponse.java): Detail records for plans, parent epics, staged plans, and alongside landings.
- [`EndedWorkItem.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/overview/EndedWorkItem.java), [`EndedWorkPage.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/overview/EndedWorkPage.java): Paged archive records for ended plans and epics.
- [`ProjectWorkChangedEvent.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/overview/ProjectWorkChangedEvent.java), [`PhaseChangedEvent.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/overview/PhaseChangedEvent.java), [`ActivityChangedEvent.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/overview/ActivityChangedEvent.java), [`NeedsOperatorChangedEvent.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/overview/NeedsOperatorChangedEvent.java), [`TaskOffTrackEvent.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/overview/TaskOffTrackEvent.java): Real-time SSE event representations.
- [`overview.en.properties`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/resources/messages/overview.en.properties) & [`overview.de.properties`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/resources/messages/overview.de.properties): Bilingual property bundles providing all UI/terminal text keys.
- [`MessageCatalogue.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/message/MessageCatalogue.java): Added ICU plural/select parser and registered `"overview"` area.
- **Unit Tests**: [`OverviewCatalogueTest.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/test/java/de/planmarshall/api/message/OverviewCatalogueTest.java) and [`OverviewRecordsTest.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/test/java/de/planmarshall/api/overview/OverviewRecordsTest.java).

### 2.2 `pm-mock-runtime` (Scenarios & Dynamic Handlers)
- [`running-work.json`](file:///Users/oliver/git/pm-mcp-clients/pm-mock-runtime/src/main/resources/scenarios/running-work.json): Comprehensive scenario fixture modeling 3 projects (`pm-mcp-clients`, `pm-mcp-core`, `plan-marshall`), active plans with attention markers, epics with staged plans, alongside landings, and paged ended work.
- [`StandardScenarios.java`](file:///Users/oliver/git/pm-mcp-clients/pm-mock-runtime/src/main/java/de/planmarshall/mock/StandardScenarios.java): Added `RUNNING_WORK = "running-work"` constant and registered in `ALL`.
- [`MockRuntimeEngine.java`](file:///Users/oliver/git/pm-mcp-clients/pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java): Dynamic handlers for `GET /api/v1/projects`, `GET /api/v1/projects/{projectId}`, and `GET /api/v1/projects/{projectId}/ended?page={page}` with dynamic sorting (needs-operator first) and pagination calculation.
- **Tests**: [`RunningWorkScenarioTest.java`](file:///Users/oliver/git/pm-mcp-clients/pm-mock-runtime/src/test/java/de/planmarshall/mock/RunningWorkScenarioTest.java) and updated [`StandardScenariosTest.java`](file:///Users/oliver/git/pm-mcp-clients/pm-mock-runtime/src/test/java/de/planmarshall/mock/StandardScenariosTest.java).

### 2.3 `pm-operator` (Terminal Client)
- [`OutputSafety.java`](file:///Users/oliver/git/pm-mcp-clients/pm-operator/src/main/java/de/planmarshall/operator/OutputSafety.java): Pure regex-based ANSI and terminal control sequence sanitizer.
- [`StatusCommand.java`](file:///Users/oliver/git/pm-mcp-clients/pm-operator/src/main/java/de/planmarshall/operator/StatusCommand.java): Enhanced `status` with hierarchical project tree rendering, `--by-epic` grouping, `--project <id>` filtering, `--ended` pagination, and attention marker formatting.
- [`PlansCommand.java`](file:///Users/oliver/git/pm-mcp-clients/pm-operator/src/main/java/de/planmarshall/operator/PlansCommand.java): Dedicated command for listing running plans across projects.
- [`PmOperator.java`](file:///Users/oliver/git/pm-mcp-clients/pm-operator/src/main/java/de/planmarshall/operator/PmOperator.java) & [`PmConsole.java`](file:///Users/oliver/git/pm-mcp-clients/pm-operator/src/main/java/de/planmarshall/operator/PmConsole.java): Registered `plans` subcommand.
- **Unit & Process Tests**: [`OverviewCommandTest.java`](file:///Users/oliver/git/pm-mcp-clients/pm-operator/src/test/java/de/planmarshall/operator/OverviewCommandTest.java) (6 tests) and added `statusWithProjectsOverview` in [`PmOperatorProcessIT.java`](file:///Users/oliver/git/pm-mcp-clients/pm-operator/src/test/java/de/planmarshall/operator/PmOperatorProcessIT.java).

### 2.4 `pm-web-app` (Web Client)
- [`client.ts`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/lib/api/client.ts): Added `getProjects()`, `getProjectDetail(projectId)`, `getEndedWork(projectId, page)`, and overview types.
- [`runtime.ts`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/lib/stores/runtime.ts): Added `'overview'` route and SSE event subscriptions (`project.work_ended`, `phase.changed`, `activity.changed`, `needs_operator.changed`, `task.off_track`).
- [`OverviewView.svelte`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/lib/views/OverviewView.svelte): Two-level responsive view with sidebar project tabs and attention badges, main panel with by-plan / by-epic toggles, plan cards, epic cards, alongside landings, and ended work pagination.
- [`+layout.svelte`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/routes/+layout.svelte) & [`+page.svelte`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/routes/+page.svelte): Routed `'overview'` view state and added header navigation button.
- [`i18n/index.ts`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/lib/i18n/index.ts): Fallback translations and client-side ICU plural/select parser.
- **Vitest Tests**: [`OverviewView.test.ts`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/lib/views/OverviewView.test.ts) (6 tests).

### 2.5 `pm-web-e2e` (Playwright E2E)
- [`overview.spec.js`](file:///Users/oliver/git/pm-mcp-clients/pm-web-e2e/tests/overview.spec.js): End-to-end browser suite validating project listing, project switching, by-plan vs by-epic view toggling, attention and off-track badge display, and ended work table navigation with pagination.

---

## 3. Verification & Test Evidence

| Module / Scope | Test Suite | Tests Run | Pass Rate |
|---|---|---|---|
| `pm-api` | `OverviewCatalogueTest`, `OverviewRecordsTest`, etc. | 150 | 100% |
| `pm-mock-runtime` | `RunningWorkScenarioTest`, `StandardScenariosTest`, etc. | 101 | 100% |
| `pm-operator` | `OverviewCommandTest`, `PmOperatorProcessIT`, etc. | 276 | 100% |
| `pm-web-app` | `OverviewView.test.ts`, `SettingsView.test.ts`, etc. | 38 | 100% |
| `pm-web-e2e` | `overview.spec.js`, `devices.spec.js`, `workspace.spec.js`, etc. | 20 | 100% |
| Full Reactor | `./mvnw verify` (all 8 modules) | 585+ | 100% |

---

## 4. Architecture & Domain Invariants Verification

- **Pure Java 25 Records**: All new data representations (`ProjectSummary`, `ActivePlanSummary`, `EndedWorkItem`, etc.) are immutable records.
- **Jackson Streaming Only**: Zero usage of `jackson-databind`; streaming `JsonParser` and `JsonGenerator` via `JsonTree` and custom codecs.
- **No Uncatalogued Strings**: All UI and CLI texts bound to `MessageCatalogue` and bilingual resource bundles (`overview.en.properties` & `overview.de.properties`).
- **Strict Dependency Boundaries**: `pm-operator` and `pm-mock-runtime` depend only on `pm-api`. Enforcer rule `clients-depend-on-pm-api-only` passed without violations.
- **Output Safety**: Terminal outputs are stripped of ANSI and terminal control sequences before display via `OutputSafety.stripControlSequences`.
- **Responsive Layout**: Web client adapts seamlessly between wide desktop table layouts and responsive cards down to mobile width (320px).
