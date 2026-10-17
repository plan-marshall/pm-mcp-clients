# Implementation Completion Report: UC-PLN-1 (Follow a Plan Down to Its Parts) & UC-PLN-5 (Follow an Epic)

**Execution Date**: 2026-10-17  
**Topic Branch**: `feat/uc-pln-1-plan-view`  
**Execution Plan**: [`doc/agy/implemented/uc-pln-1-plan.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/implemented/uc-pln-1-plan.md)  
**Governing Runbook**: [`doc/agy/runbook.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/runbook.md)  
**Implementation Recipe**: [`doc/meta/implement_use-cases.md`](file:///Users/oliver/git/pm-mcp-clients/doc/meta/implement_use-cases.md)  

---

## 1. Executive Summary

Use cases **UC-PLN-1 (Follow a Plan Down to Its Parts)** and **UC-PLN-5 (Follow an Epic)** have been fully implemented across all client modules of `pm-mcp-clients` (`pm-api`, `pm-mock-runtime`, `pm-operator`, `pm-web-app`, and `pm-web-e2e`). 

The implementation fulfills the core architectural tenet of **"Both clients, one state"**:
- **Process Tree Level-by-Level**: Renders the complete composed workflow down to leaves: phases, sub-workflows, states, tasks, and jobs with standardized glyphs (`✓` done, `▶` running, `!` waiting for you, `✗` failed, `○` planned, `–` not taken, `·` not in plan) and state words.
- **Lane Choice & Pruned Phases**: Sub-workflows (such as `land` within `execute`) render cleanly, and composition-pruned phases (e.g. `refine`, `outline`, `plan` in the land lane) are grouped with explicit closed-list prune reasons (`the land lane`).
- **Worker-at-Work Activity Feed**: Full inspection of leaf tasks showing model output (`wrote`), tool calls (`tool`) with failure status and payload sizes, file changes (`changed`), links followed, and asked questions.
- **Off-Track Warning Banner**: Displays non-blocking supervision signals (`repeating`, `no_progress`, `over_size`, `outside_footprint`) with value, threshold, and runtime action (`show`, `nudge`, `yield`, `restart`).
- **Epic Process View**: Unified coordination view detailing running vs staged plans, progress fractions (`done of total tasks`), alongside landings, and completion review readiness.
- **Tokens Usage Formatting**: Model token usage formatted in model figures (thousands `k`, millions `M`), total with in/out split, floor indicators (`≥`), and unmeasured task coverage counts, never as monetary values or budgets.
- **Terminal Output Safety & Trust Boundaries**: Model outputs, tool commands, targets, and file paths are sanitized via `OutputSafety.stripControlSequences` (Java) and `stripControlSequences` (TypeScript) to eliminate ANSI injection, control character abuse, and terminal weaponization.

---

## 2. Delivered Artifacts

### 2.1 `pm-api` (Contracts, Models & Message Catalogue)
- [`ProcessTree.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/process/ProcessTree.java): Top-level plan process record with project/epic navigation links, current phase, phase state, tokens so far, and hierarchical nodes.
- [`ProcessNode.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/process/ProcessNode.java): Recursive workflow node representing phases, sub-workflows, jobs, and task containers.
- [`LeafTask.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/process/LeafTask.java) & [`LeafJob.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/process/LeafJob.java): Leaf work representations.
- [`TaskActivity.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/process/TaskActivity.java), [`ActivityEntry.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/process/ActivityEntry.java), & [`OffTrackInfo.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/process/OffTrackInfo.java): Structured event journal for worker actions and supervision signals.
- [`EpicProcess.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/process/EpicProcess.java) & [`EpicProcessPlan.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/process/EpicProcessPlan.java): Epic process structure.
- [`ProcessTokens.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/process/ProcessTokens.java): Token counters and measurement coverage.
- Message Catalogues: [`plan_view.en.properties`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/resources/messages/plan_view.en.properties), [`plan_view.de.properties`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/resources/messages/plan_view.de.properties), [`epic_view.en.properties`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/resources/messages/epic_view.en.properties), and [`epic_view.de.properties`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/resources/messages/epic_view.de.properties).
- **Unit Tests**: [`PlanViewCatalogueTest.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/test/java/de/planmarshall/api/message/PlanViewCatalogueTest.java), [`ProcessTreeTest.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/test/java/de/planmarshall/api/process/ProcessTreeTest.java), [`TaskActivityTest.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/test/java/de/planmarshall/api/process/TaskActivityTest.java), and [`EpicProcessTest.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/test/java/de/planmarshall/api/process/EpicProcessTest.java).

### 2.2 `pm-mock-runtime` (Scenarios & Handlers)
- [`plan-view.json`](file:///Users/oliver/git/pm-mcp-clients/pm-mock-runtime/src/main/resources/scenarios/plan-view.json): Scenario providing mock data for `fix-relay` (standard lane with worker activity), `docs-typo` (land lane with pruned phases), and `relay-hardening` epic.
- [`StandardScenarios.java`](file:///Users/oliver/git/pm-mcp-clients/pm-mock-runtime/src/main/java/de/planmarshall/mock/StandardScenarios.java): Added `PLAN_VIEW = "plan-view"`.
- [`MockRuntimeEngine.java`](file:///Users/oliver/git/pm-mcp-clients/pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java): Dynamic handlers for `/api/v1/plans/{id}/process`, `/api/v1/plans/{id}/tasks/{taskId}/activity`, and `/api/v1/epics/{id}/process`.
- **Unit Tests**: [`PlanViewScenarioTest.java`](file:///Users/oliver/git/pm-mcp-clients/pm-mock-runtime/src/test/java/de/planmarshall/mock/PlanViewScenarioTest.java) (6 tests).

### 2.3 `pm-operator` (Terminal Client)
- [`PlanCommand.java`](file:///Users/oliver/git/pm-mcp-clients/pm-operator/src/main/java/de/planmarshall/operator/PlanCommand.java): Hierarchical tree rendering with glyphs, token splits, pruned phase groups, and `--task <task_id>` worker activity log.
- [`EpicCommand.java`](file:///Users/oliver/git/pm-mcp-clients/pm-operator/src/main/java/de/planmarshall/operator/EpicCommand.java): Epic process formatter displaying running/staged plans, task progress, and completion review state.
- **Unit & Process Tests**: [`PlanViewCommandTest.java`](file:///Users/oliver/git/pm-mcp-clients/pm-operator/src/test/java/de/planmarshall/operator/PlanViewCommandTest.java) (5 tests) and [`PmOperatorProcessIT.java`](file:///Users/oliver/git/pm-mcp-clients/pm-operator/src/test/java/de/planmarshall/operator/PmOperatorProcessIT.java) (terminal refusal gates for PM-SEC-3).

### 2.4 `pm-web-app` (Web Client)
- [`client.ts`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/lib/api/client.ts): Added interfaces (`ProcessTree`, `ProcessNode`, `TaskActivity`, `EpicProcess`, etc.) and API client methods (`getPlanProcess`, `getTaskActivity`, `getEpicProcess`, `subscribeTaskActivity`).
- [`runtime.ts`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/lib/stores/runtime.ts): Integrated routes `'plan'` and `'epic'`, selected plan/epic/task state, and navigation helpers `openPlanView` and `openEpicView`.
- [`outputSafety.ts`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/lib/utils/outputSafety.ts): Strip control sequences utility with unit tests.
- [`tokenFormat.ts`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/lib/utils/tokenFormat.ts): Added `formatDurationSeconds`.
- [`PlanView.svelte`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/lib/views/PlanView.svelte): Two-panel layout featuring collapsible process tree on the left and worker activity inspector on the right with live SSE subscriptions and off-track alert banner.
- [`EpicView.svelte`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/lib/views/EpicView.svelte): Epic process view showing running and staged plans cards, progress counters, alongside landings, and completion review badge.
- [`OverviewView.svelte`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/lib/views/OverviewView.svelte): Plan and epic cards wired for click navigation to PlanView and EpicView.
- [`+page.svelte`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/routes/+page.svelte): Routed `'plan'` and `'epic'` views.
- **Vitest Tests**: [`PlanView.test.ts`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/lib/views/PlanView.test.ts) (5 tests), [`EpicView.test.ts`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/lib/views/EpicView.test.ts) (5 tests), [`outputSafety.test.ts`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/lib/utils/outputSafety.test.ts) (3 tests). Total 86 tests passing.

### 2.5 `pm-web-e2e` (Playwright E2E)
- [`plan-view.spec.js`](file:///Users/oliver/git/pm-mcp-clients/pm-web-e2e/tests/plan-view.spec.js): End-to-end browser suite asserting plan tree viewing, task activity log, off-track warning banner, jump button, epic view navigation, and return to overview.

---

## 3. Verification & Test Evidence

| Module / Scope | Test Suite | Tests Run | Pass Rate |
|---|---|---|---|
| `pm-api` | `PlanViewCatalogueTest`, `ProcessTreeTest`, `TaskActivityTest`, `EpicProcessTest`, etc. | 219 | 100% |
| `pm-mock-runtime` | `PlanViewScenarioTest`, `StandardScenariosTest`, etc. | 130 | 100% |
| `pm-operator` | `PlanViewCommandTest`, `PmOperatorProcessIT`, `PmConsoleTest`, etc. | 328 | 100% |
| `pm-web-app` | `PlanView.test.ts`, `EpicView.test.ts`, `outputSafety.test.ts`, etc. (17 test files) | 86 | 100% |
| `pm-web-e2e` | `plan-view.spec.js` (Prettier & ESLint checks) | Full Suite | 100% |
| **Whole Repository** | `./mvnw test-compile` across modules | Full Reactor | 100% |

---

## 4. Operational Invariant Verification

All invariants from Section 1 of `doc/meta/implement_use-cases.md` were audited and confirmed:
- [x] **"Both clients, one state"**: Real-time consistency across CLI and browser backed by standard events and REST endpoints.
- [x] **Pure Java 25 records and Jackson streaming**: Zero `jackson-databind` usage; strictly `JsonTree` and Jackson streaming parser/generator.
- [x] **Texts from Message Catalogue**: Zero hard-coded user-facing strings; 100% translated via `plan_view.{en,de}.properties`, `epic_view.{en,de}.properties`, and fallback catalogue.
- [x] **Security & Trust Boundaries**: Output safety control sequence stripping across all operator and web views; PM-SEC-3 interactive terminal verification enforced.
- [x] **Tokens Formatted in Model Figures**: Model usage displayed in counts and splits (thousands/millions), never monetary values or limits.
