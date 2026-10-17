# Implementation Plan: UC-STA-1..5 Starting Work (Plan Start, Intake & Recipes)

This plan coordinates the end-to-end implementation of use cases `UC-STA-1: Start a Plan`, `UC-STA-2: Start an Epic`, `UC-STA-3: Bring in Documents`, `UC-STA-4: Bring in an Issue`, and `UC-STA-5: Run a Recipe` across all client and runtime modules in `pm-mcp-clients`.

It operationalizes the technical recipe from [`doc/meta/implement_use-cases.md`](file:///Users/oliver/git/pm-mcp-clients/doc/meta/implement_use-cases.md) under the execution discipline of [`doc/agy/runbook.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/runbook.md).

---

## 1. Traceability & Context

| Dimension | Reference |
|---|---|
| **Use Cases** | [`plan-marshall-documentation/doc/use-cases/05-starting-work.adoc#UC-STA-1`](file:///Users/oliver/git/plan-marshall-documentation/doc/use-cases/05-starting-work.adoc) (Start a Plan), [`#UC-STA-2`](file:///Users/oliver/git/plan-marshall-documentation/doc/use-cases/05-starting-work.adoc) (Start an Epic), [`#UC-STA-3`](file:///Users/oliver/git/plan-marshall-documentation/doc/use-cases/05-starting-work.adoc) (Bring in Documents), [`#UC-STA-4`](file:///Users/oliver/git/plan-marshall-documentation/doc/use-cases/05-starting-work.adoc) (Bring in an Issue), [`#UC-STA-5`](file:///Users/oliver/git/plan-marshall-documentation/doc/use-cases/05-starting-work.adoc) (Run a Recipe) |
| **Specifications** | [`01-processes-and-transport.adoc#starting-work`](file:///Users/oliver/git/plan-marshall-documentation/doc/specification/runtime-model/01-processes-and-transport.adoc), [`09-plan-instance.adoc#start-contract`](file:///Users/oliver/git/pm-mcp-core/doc/specification/workflow-dsl/09-plan-instance.adoc), [`06-event-catalogue.adoc`](file:///Users/oliver/git/plan-marshall-documentation/doc/specification/runtime-model/06-event-catalogue.adoc), [`01-conventions-generic-conditions.adoc#intake`](file:///Users/oliver/git/pm-mcp-core/doc/specification/state-catalogue/01-conventions-generic-conditions.adoc) |
| **Local Client Design** | Shared: [`doc/design/shared/starting-work.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/design/shared/starting-work.adoc)<br>CLI: [`doc/design/terminal-client/17-start.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/design/terminal-client/17-start.adoc)<br>Web: [`doc/design/web-app/README.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/design/web-app/README.adoc) |
| **Roadmap Packages** | [`doc/plans/start-contract.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/plans/start-contract.adoc), [`doc/plans/cli-start.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/plans/cli-start.adoc), [`doc/plans/web-start.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/plans/web-start.adoc) |
| **Implementation Recipe** | [`doc/meta/implement_use-cases.md`](file:///Users/oliver/git/pm-mcp-clients/doc/meta/implement_use-cases.md) |
| **Operational SOP** | [`doc/agy/runbook.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/runbook.md) |

---

## 2. Parallel Isolation & Non-Overlap Invariants (vs `UC-PLN-3`)

`UC-STA-1..5` executes concurrently with `UC-PLN-3: Design a Plan's Process` (`feat/uc-pln-3-designer`). Strict non-overlap is guaranteed across all repository modules:

1. **Contracts & API (`pm-api`)**:
   - `UC-STA-1..5`: Owns `de.planmarshall.api.start.*` and `messages/start.{en,de}.properties`.
   - `UC-PLN-3`: Owns `de.planmarshall.api.designer.*` and `messages/designer.{en,de}.properties`.
   - Zero shared model classes, enums, or resource properties.
2. **Mock Runtime (`pm-mock-runtime`)**:
   - `UC-STA-1..5`: Routes `/api/v1/projects/{id}/plans/start`, `/api/v1/projects/{id}/epics/start`, `/api/v1/intake/*`, `/api/v1/recipes`, scenario `scenarios/start.json`.
   - `UC-PLN-3`: Routes `/api/v1/workflows`, `/api/v1/projects/{id}/workflows`, `/api/v1/workflows/catalogue`, scenario `scenarios/designer.json`.
   - Disjoint route dispatch tables and scenario files.
3. **CLI Client (`pm-operator`)**:
   - `UC-STA-1..5`: Owns `StartCommand.java` (`start plan`, `start epic`, `--recipe`, `--doc`, `--issue`).
   - `UC-PLN-3`: Zero CLI changes (web-only specification per `UC-PLN-3`).
   - Disjoint Picocli verb classes.
4. **Web Client (`pm-web-app`)**:
   - `UC-STA-1..5`: `StartWorkModal.svelte`, `RecipeSelector.svelte`, `SourceIntakePanel.svelte`.
   - `UC-PLN-3`: `ProcessDesigner.svelte`, `BlockPalette.svelte`, `WorkflowEditor.svelte`.
   - Disjoint Svelte components and route views.
5. **E2E & Integration Tests**:
   - `UC-STA-1..5`: `pm-web-e2e/tests/start.spec.js` and `StartCommandTest.java`.
   - `UC-PLN-3`: `pm-web-e2e/tests/designer.spec.js`.

---

## 3. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created on topic branch off latest `main` (`git worktree add -b feat/uc-sta-start-work /Users/oliver/git/worktrees/pm-mcp-clients-uc-sta main`).
- [x] 2. Domain skills & engineering personas loaded for each task type:
  - `persona-implementer` & `persona-module-tester` for code & unit tests
  - `persona-integration-tester` for multi-module/integration tests
  - `persona-security-expert` for untrusted path validation and input sanitization
  - `persona-documenter` for documentation & ADRs
  - `persona-code-reviewer` for finding evaluation & adversarial refutation
- [x] 3. Incremental implementation following the 6-Phase technical recipe with local tests passing.
- [x] 4. Final completion report written (`doc/agy/uc-sta-plan-report.md`) and initial plan moved to `doc/agy/implemented/uc-sta-plan.md`. (Do NOT move report to `verified/` — post-landing verification moves it).
- [x] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit` for code/config; skipped for standalone documentation changes).
- [x] 6. PR opened (with `--label "skip-bot-review"` if authorized), 5-minute review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [x] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [x] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/uc-sta-plan-report.md` by orchestrator session.

---

## 4. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Trace** | `plan-marshall-persona-documenter` | `plan-marshall-ref-code-quality` | Verify start request payloads, source intake documents, recipe parameters, issue references, and event catalogue keys. |
| **Phase 2: `pm-api`** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-module-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:junit-core` | Immutable Java 25 records, Jackson streaming parsing, bilingual resource bundles (`start.{en,de}.properties`). |
| **Phase 3: Mock Runtime** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-integration-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:cui-cui-http-testing` | Stateful scenario JSON (`start.json`), dynamic route handlers, SSE broadcast (`plan.started`, `intake.created`). |
| **Phase 4: `pm-operator`** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-security-expert` | `pm-dev-java:java-core`<br>`pm-dev-java:java-security` | Subcommands `start plan`, `start epic`, recipe selection, path/issue flags, output safety, terminal refusal gates (`PM-SEC-3`). |
| **Phase 5: `pm-web-app`** | `plan-marshall-persona-implementer`<br>`pm-dev-frontend:javascript` | `pm-dev-frontend:css`<br>`pm-dev-frontend:jest-testing` | Start work modal, recipe dropdown, file attachment/doc picker, issue URL input, Vitest tests. |
| **Phase 6: Verification** | `plan-marshall-persona-integration-tester` | `pm-dev-java:junit-integration` | `PmOperatorProcessIT` process execution, Playwright E2E browser test suite (`start.spec.js`). |
| **Phase 7: Review Gate** | `plan-marshall-persona-code-reviewer` | `pm-dev-java:java-security` | Adversarial refutation of candidate bot findings, 100% resolution bar. |

---

## 5. Phased Task Breakdown

### Phase 1: Trace Matrix & Specification Alignment
- Load `plan-marshall-persona-documenter`.
- Map start mechanisms:
  - Start plan: project ID, prompt/objective, optional recipe, input documents, issue reference.
  - Start epic: project ID, epic objective, workstream hints.
  - Bring in documents: local relative paths within project checkout.
  - Bring in an issue: GitHub/GitLab issue URL or issue key.
  - Run a recipe: recipe ID and parameter mapping.
- Verify event catalogue keys: `plan.started`, `epic.started`, `intake.created`, `intake.updated`.

### Phase 2: Contracts, Models & Message Catalogue (`pm-api`)
- Load `plan-marshall-persona-implementer` and `pm-dev-java:java-core`.
- Create records in `de.planmarshall.api.start.*`:
  - `StartPlanRequest.java`: Project ID, objective, recipe ID, parameters, document paths, issue ref.
  - `StartEpicRequest.java`: Project ID, objective, auto-start flag.
  - `PlanStartResult.java`: Created plan ID, initial status, redirect link.
  - `RecipeRef.java`: Recipe key, display name, description, required parameters.
  - `IntakeDraft.java`: Draft intake session with uploaded sources and prompt text.
  - `SourceInput.java`: Kind (`DOC`, `ISSUE`, `LESSON`), location, title.
- Create bilingual catalogues: `messages/start.en.properties` and `messages/start.de.properties`.
- Unit tests with `plan-marshall-persona-module-tester`: `StartRecordsTest.java`, `StartCatalogueTest.java`.

### Phase 3: Mock Runtime Scenarios & Dynamic Endpoints (`pm-mock-runtime`)
- Load `plan-marshall-persona-implementer` and `pm-dev-java:cui-cui-http-testing`.
- Create scenario fixture `scenarios/start.json`:
  - Enrolled projects ready for plan start, inventory of available recipes (`agentfile-hygiene`, `code-review`, `lesson-cleanup`), draft intake sessions.
- Implement endpoints in `MockRuntimeEngine.java`:
  - `POST /api/v1/projects/{projectId}/plans/start`
  - `POST /api/v1/projects/{projectId}/epics/start`
  - `GET /api/v1/recipes`
  - `POST /api/v1/intake/draft`
- Unit tests: `StartScenarioTest.java`.

### Phase 4: Terminal Client Integration (`pm-operator`)
- Load `plan-marshall-persona-implementer` and `pm-dev-java:java-security`.
- Implement `StartCommand.java`:
  - `start plan <projectId> [--recipe <recipe>] [--doc <path>] [--issue <url>] [--prompt <text>]`
  - `start epic <projectId> [--prompt <text>]`
- Enforce path validation and sanitization via `OutputSafety.stripControlSequences`.
- Unit and process tests: `StartCommandTest.java` and `PmOperatorProcessIT.java`.

### Phase 5: Web Application Views (`pm-web-app`)
- Load `plan-marshall-persona-implementer` and `pm-dev-frontend:javascript`.
- Extend `client.ts` with start work API methods and recipe queries.
- Create `StartWorkModal.svelte`: Modal triggered from Project Explorer or Overview to start plans/epics.
- Create `RecipeSelector.svelte`: Searchable recipe palette with parameter inputs.
- Create `SourceIntakePanel.svelte`: Input fields for attaching markdown documents and issue links.
- Vitest unit tests: `StartWorkModal.test.ts`, `RecipeSelector.test.ts`.

### Phase 6: Verification & End-to-End Suite
- Load `plan-marshall-persona-integration-tester`.
- Write Playwright E2E suite: `pm-web-e2e/tests/start.spec.js`.
- Execute `./mvnw verify -Ppre-commit` and ensure 100% clean reactor compilation.

---

## 6. Definition of Done
1. All 5 use cases (`UC-STA-1..5`) completely implemented across CLI and Web clients.
2. Invariant "Both clients, one state" verified via real-time SSE event subscriptions (`plan.started`).
3. 100% of user-facing messages externalized in bilingual property bundles (`start.{en,de}.properties`).
4. Full reactor build `./mvnw verify -Ppre-commit` passes with zero regressions.
5. Completion report documented at `doc/agy/uc-sta-plan-report.md` and initial plan archived to `doc/agy/implemented/uc-sta-plan.md`.
