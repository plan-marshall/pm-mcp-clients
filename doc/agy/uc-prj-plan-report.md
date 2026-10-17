# Implementation Completion Report: UC-PRJ-1..3 Project Onboarding, Explorer, Settings & Release

**Execution Date**: 2026-10-17  
**Topic Branch**: `feat/uc-prj-projects`  
**Execution Plan**: [`doc/agy/implemented/uc-prj-plan.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/implemented/uc-prj-plan.md)  
**Governing Runbook**: [`doc/agy/runbook.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/runbook.md)  
**Implementation Recipe**: [`doc/meta/implement_use-cases.md`](file:///Users/oliver/git/pm-mcp-clients/doc/meta/implement_use-cases.md)  

---

## 1. Executive Summary

Use cases **UC-PRJ-1 (Project Onboarding & Setup)**, **UC-PRJ-2 (Project Explorer & Management)**, and **UC-PRJ-3 (Project Settings, Favourites & Release)** have been comprehensively implemented across all 5 tiers of `pm-mcp-clients` (`pm-api`, `pm-mock-runtime`, `pm-operator`, `pm-web-app`, and `pm-web-e2e`), strictly adhering to the architectural and operational guidelines defined in `doc/design/shared/project-explorer.adoc`, `doc/plans/implemented/project-contract.adoc`, `doc/plans/implemented/cli-projects.adoc`, and `doc/plans/implemented/web-projects.adoc`.

Key deliverables and operational invariants achieved:
- **"Both clients, one state"**: Real-time cross-client consistency between terminal (`pm-operator`) and browser (`pm-web-app`), synchronized via standard REST endpoints and SSE lifecycle events (`project.changed`, `project.enrolled`, `project.released`).
- **4-Part Project Explorer Layout**: Both clients render the authoritative 4-part hierarchy:
  1. **Being added**: Active onboarding processes with progress step indicator (`step_number/total_steps`) and `[Continue]` / `[Abandon]` operations.
  2. **Favourites**: Operator-pinned projects with prominent star (`★`) markers and instant toggle.
  3. **Recent**: Projects active within the last 30 days, sorted by activity recency with relative time formats (`just now`, `X hours ago`, `X days ago`).
  4. **All Projects**: Filterable directory sorted by name or activity frequency, displaying readiness state badges (`ready`, `setup needs: <summary>`, `released`).
- **Project Lifecycle Operations**:
  - **Onboarding (`add <path>`)**: Path-based onboarding launch transitioning checkout paths to active onboarding records.
  - **Setup (`setup <id> [<step>]`)**: Setup inspection and step advancement for projects with setup needs.
  - **Favourites (`favourite <id> [--off]`)**: Reversible pinning/unpinning of projects.
  - **Release (`release <id> [--reason <text>] [--yes] [--force]`)**: Two-step impact analysis detailing ending plans and epics before marking a project as released (prohibiting new work).
  - **Unenrolment (`unenrol <id> [--yes]`)**: Complete unenrollment with fail-fast HTTP 409 guard against projects with active plans or epics.
- **Strict Security & Output Safety**: Control sequence stripping via `OutputSafety.stripControlSequences` across all project IDs, display names, paths, remotes, and summaries; operator isolation enforced via `OperatorGate` (`PM-SEC-3`).
- **Pure Java 25 & Jackson Streaming**: Pure records with streaming JSON trees (`JsonTree`), zero `jackson-databind`, zero reflection overhead.
- **Bilingual Message Catalogue**: 100% of user-facing CLI and web strings externalized in `messages/projects.{en,de}.properties` and resolved via `MessageCatalogue`.
- **Zero Regressions & Full Pre-Commit Gate**: 100% test pass rate across all reactor modules with `./mvnw verify -Ppre-commit` passing cleanly.

---

## 2. Delivered Artifacts by Module

### 2.1 `pm-api` (Contracts, Models & Message Catalogue)
- [`EnrolledProject.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/project/EnrolledProject.java): Immutable Java 25 record representing enrolled project state, counts, readiness, timestamps, and favourite flag.
- [`ProjectExplorer.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/project/ProjectExplorer.java): Consolidated 4-part explorer response DTO with streaming JSON serialization/deserialization.
- [`BeingAddedProject.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/project/BeingAddedProject.java): Onboarding progress tracking record.
- [`FavouritesUpdateRequest.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/project/FavouritesUpdateRequest.java): Reorder/update favourite project IDs payload.
- [`ProjectOnboarding.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/project/ProjectOnboarding.java): Onboarding launch contract.
- [`ProjectReleaseRequest.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/project/ProjectReleaseRequest.java) & [`ProjectReleaseResult.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/project/ProjectReleaseResult.java): Two-step project release contract.
- [`ProjectChangedEvent.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/project/ProjectChangedEvent.java), [`ProjectEnrolledEvent.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/project/ProjectEnrolledEvent.java), [`ProjectReleasedEvent.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/project/ProjectReleasedEvent.java): SSE lifecycle event records.
- [`ProjectReadiness.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/project/ProjectReadiness.java), [`ProjectOnboardingState.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/project/ProjectOnboardingState.java), [`ReleaseWaitStatus.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/project/ReleaseWaitStatus.java): Type-safe enums.
- [`projects.en.properties`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/resources/messages/projects.en.properties) & [`projects.de.properties`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/resources/messages/projects.de.properties): Comprehensive bilingual catalogues registered in `MessageCatalogue.SUPPORTED_AREAS`.
- **Tests**: [`ProjectRecordsTest.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/test/java/de/planmarshall/api/project/ProjectRecordsTest.java) (9 tests), [`ProjectsCatalogueTest.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/test/java/de/planmarshall/api/message/ProjectsCatalogueTest.java) (3 tests).

### 2.2 `pm-mock-runtime` (Scenarios & Handlers)
- [`projects.json`](file:///Users/oliver/git/pm-mcp-clients/pm-mock-runtime/src/main/resources/scenarios/projects.json): Rich scenario fixture with 2 favourites, 2 recent projects, 1 being-added project, and 6 total projects with diverse readiness states (`ready`, `setup_needs`, `released`).
- [`StandardScenarios.java`](file:///Users/oliver/git/pm-mcp-clients/pm-mock-runtime/src/main/java/de/planmarshall/mock/StandardScenarios.java): Registered `PROJECTS = "projects"` in scenario inventory.
- [`MockRuntimeEngine.java`](file:///Users/oliver/git/pm-mcp-clients/pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java): Dynamic handlers for `/api/v1/projects`, `/api/v1/projects/favourites`, `/api/v1/projects/{id}/favourite`, `/api/v1/projects/onboarding`, `/api/v1/projects/{id}/release`, `/api/v1/projects/{id}` (DELETE with 409 conflict checks), and `/api/v1/projects/onboarding/{id}/abandon`.
- **Tests**: [`ProjectsScenarioTest.java`](file:///Users/oliver/git/pm-mcp-clients/pm-mock-runtime/src/test/java/de/planmarshall/mock/ProjectsScenarioTest.java) (5 tests), [`StandardScenariosTest.java`](file:///Users/oliver/git/pm-mcp-clients/pm-mock-runtime/src/test/java/de/planmarshall/mock/StandardScenariosTest.java).

### 2.3 `pm-operator` (Terminal Client)
- [`ProjectsCommand.java`](file:///Users/oliver/git/pm-mcp-clients/pm-operator/src/main/java/de/planmarshall/operator/ProjectsCommand.java): 4-part explorer view, `--filter <text>`, relative duration calculations, and dual support for JSON object maps and legacy arrays.
- [`ProjectCommand.java`](file:///Users/oliver/git/pm-mcp-clients/pm-operator/src/main/java/de/planmarshall/operator/ProjectCommand.java): Comprehensive project inspection and subcommands:
  - `project <id>`: Detailed readiness, needs operator count, work, and remote inspection.
  - `project add <path>`: Launch onboarding.
  - `project setup <id> [<step>]`: Inspect/advance setup steps.
  - `project favourite <id> [--off]`: Toggle favourite star.
  - `project release <id> [--reason <text>] [--yes] [--force] [--no-input]`: Interactive/non-interactive release with impact summary.
  - `project unenrol <id> [--yes] [--no-input]`: Safe unenrollment with conflict reporting.
- **Unit & Process Tests**: [`ProjectCommandTest.java`](file:///Users/oliver/git/pm-mcp-clients/pm-operator/src/test/java/de/planmarshall/operator/ProjectCommandTest.java) (13 tests), [`PmOperatorProcessIT.java`](file:///Users/oliver/git/pm-mcp-clients/pm-operator/src/test/java/de/planmarshall/operator/PmOperatorProcessIT.java) (verifying PM-SEC-3 interactive terminal refusal).

### 2.4 `pm-web-app` (Web Client)
- [`client.ts`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/lib/api/client.ts): Added project interfaces and client endpoints (`getProjectExplorer`, `updateFavourites`, `toggleFavourite`, `startProjectOnboarding`, `releaseProject`, `unenrolProject`, `abandonOnboarding`).
- [`runtime.ts`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/lib/stores/runtime.ts): Integrated `currentRoute = 'projects'`, SSE subscriptions for `project.changed`, `project.enrolled`, and `project.released`.
- [`ProjectExplorerView.svelte`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/lib/views/ProjectExplorerView.svelte): Full 4-part view with star toggling, live filter, sort dropdown, being-added action cards, and modals for Add, Release, Unenrol, and Abandon.
- [`+layout.svelte`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/routes/+layout.svelte) & [`+page.svelte`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/routes/+page.svelte): Projects nav link and route binding.
- [`routes/projects/+page.svelte`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/routes/projects/+page.svelte): Standalone projects route.
- **Vitest Tests**: [`ProjectExplorerView.test.ts`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/lib/views/ProjectExplorerView.test.ts) (7 tests).

### 2.5 `pm-web-e2e` (Playwright E2E)
- [`projects.spec.js`](file:///Users/oliver/git/pm-mcp-clients/pm-web-e2e/tests/projects.spec.js): End-to-end browser test suite verifying explorer rendering, search filtering, favourite toggling, and project release interactions.

---

## 3. Verification & Test Evidence

| Module / Scope | Test Suite | Tests Run | Pass Rate |
|---|---|---|---|
| `pm-api` | `ProjectRecordsTest`, `ProjectsCatalogueTest`, etc. | 222 | 100% |
| `pm-mock-runtime` | `ProjectsScenarioTest`, `StandardScenariosTest`, etc. | 129 | 100% |
| `pm-operator` | `ProjectCommandTest`, `PmOperatorProcessIT`, etc. | 331 | 100% |
| `pm-web-app` | `ProjectExplorerView.test.ts`, client tests, etc. | 80 | 100% |
| `pm-web-e2e` | `projects.spec.js` (ESLint & structure) | Clean | 100% |
| **Whole Repository** | `./mvnw verify -Ppre-commit` (all 8 reactor modules) | Full Suite | 100% |

---

## 4. Operational Invariant Verification

All invariants from Section 1 of `doc/meta/implement_use-cases.md` were audited and confirmed:
- [x] **"Both clients, one state"**: Real-time consistency across CLI and browser backed by standard events and REST endpoints.
- [x] **Texts from Message Catalogue**: Zero hard-coded english prose; 100% translated via `projects.{en,de}.properties`.
- [x] **Security & Trust Boundaries**: Output safety control sequence stripping across all operator views; PM-SEC-3 interactive terminal verification enforced.
- [x] **Pure Java 25 & Jackson Streaming**: Jackson streaming `JsonTree` and pure records without `jackson-databind`.
- [x] **Clean Compile & Quality Gates**: OpenRewrite formatting, Checkstyle, SpotBugs, and Enforcer pass with zero errors.
