# Implementation Plan: UC-PLN-3 Design a Plan's Process (Process Designer)

This plan coordinates the end-to-end implementation of use case `UC-PLN-3: Design a Plan's Process` (Process Designer, Building Blocks Palette, Static Validation, Unit Placing & Removing) across all client and runtime modules in `pm-mcp-clients`.

It operationalizes the technical recipe from [`doc/meta/implement_use-cases.md`](file:///Users/oliver/git/pm-mcp-clients/doc/meta/implement_use-cases.md) under the execution discipline of [`doc/agy/runbook.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/runbook.md).

---

## 1. Traceability & Context

| Dimension | Reference |
|---|---|
| **Use Cases** | [`plan-marshall-documentation/doc/use-cases/03-plans.adoc#UC-PLN-3`](file:///Users/oliver/git/plan-marshall-documentation/doc/use-cases/03-plans.adoc) (Design a Plan's Process — Web App Only) |
| **Specifications** | [`04-ast-node-model.adoc#process-designer`](file:///Users/oliver/git/pm-mcp-core/doc/specification/workflow-dsl/04-ast-node-model.adoc), [`01-grammar.adoc#unit-frontmatter`](file:///Users/oliver/git/pm-mcp-core/doc/specification/workflow-dsl/01-grammar.adoc), [`#dsl-reserved-keywords`](file:///Users/oliver/git/pm-mcp-core/doc/specification/workflow-dsl/01-grammar.adoc), [`05-primitives.adoc#primitive-spi-contract`](file:///Users/oliver/git/pm-mcp-core/doc/specification/workflow-dsl/05-primitives.adoc), [`04-ast-node-model.adoc#static-verification-rules`](file:///Users/oliver/git/pm-mcp-core/doc/specification/workflow-dsl/04-ast-node-model.adoc), [`#project-owned-units`](file:///Users/oliver/git/pm-mcp-core/doc/specification/workflow-dsl/04-ast-node-model.adoc), [`09-plan-instance.adoc#composition-rules`](file:///Users/oliver/git/pm-mcp-core/doc/specification/workflow-dsl/09-plan-instance.adoc), [`03-web-server.adoc#web-proxy`](file:///Users/oliver/git/plan-marshall-documentation/doc/specification/cli-and-security/03-web-server.adoc) |
| **Local Client Design** | Web: [`doc/design/web-app/process-designer.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/design/web-app/process-designer.adoc), [`process-designer-texts.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/design/web-app/process-designer-texts.adoc) |
| **Roadmap Packages** | [`doc/plans/implemented/designer-contract.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/plans/implemented/designer-contract.adoc), [`doc/plans/implemented/web-designer.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/plans/implemented/web-designer.adoc) |
| **Implementation Recipe** | [`doc/meta/implement_use-cases.md`](file:///Users/oliver/git/pm-mcp-clients/doc/meta/implement_use-cases.md) |
| **Operational SOP** | [`doc/agy/runbook.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/runbook.md) |

---

## 2. Parallel Isolation & Non-Overlap Invariants (vs `UC-STA-1..5`)

`UC-PLN-3` executes concurrently with `UC-STA-1..5: Starting Work` (`feat/uc-sta-start-work`). Strict non-overlap is guaranteed across all repository modules:

1. **Contracts & API (`pm-api`)**:
   - `UC-PLN-3`: Owns `de.planmarshall.api.designer.*` and `messages/designer.{en,de}.properties`.
   - `UC-STA-1..5`: Owns `de.planmarshall.api.start.*` and `messages/start.{en,de}.properties`.
   - Zero shared model classes, enums, or resource properties.
2. **Mock Runtime (`pm-mock-runtime`)**:
   - `UC-PLN-3`: Routes `/api/v1/workflows`, `/api/v1/projects/{id}/workflows`, `/api/v1/workflows/catalogue`, `/api/v1/workflows/validate`, scenario `scenarios/designer.json`.
   - `UC-STA-1..5`: Routes `/api/v1/projects/{id}/plans/start`, `/api/v1/intake/*`, scenario `scenarios/start.json`.
   - Disjoint route dispatch tables and scenario files.
3. **CLI Client (`pm-operator`)**:
   - `UC-PLN-3`: Zero CLI changes (strictly web-only per specification).
   - `UC-STA-1..5`: Owns `StartCommand.java`.
   - Zero command conflicts.
4. **Web Client (`pm-web-app`)**:
   - `UC-PLN-3`: Route `/designer` with `ProcessDesigner.svelte`, `BlockPalette.svelte`, `WorkflowEditor.svelte`.
   - `UC-STA-1..5`: `StartWorkModal.svelte`, `RecipeSelector.svelte`, `SourceIntakePanel.svelte`.
   - Disjoint Svelte components and route views.
5. **E2E & Integration Tests**:
   - `UC-PLN-3`: `pm-web-e2e/tests/designer.spec.js`.
   - `UC-STA-1..5`: `pm-web-e2e/tests/start.spec.js` and `StartCommandTest.java`.

---

## 3. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created on topic branch off latest `main` (`git worktree add -b feat/uc-pln-3-designer /Users/oliver/git/worktrees/pm-mcp-clients-uc-pln-3 main`).
- [x] 2. Domain skills & engineering personas loaded for each task type:
  - `persona-implementer` & `persona-module-tester` for code & unit tests
  - `persona-integration-tester` for multi-module/integration tests
  - `persona-security-expert` for unit name validation, path traversal prevention, and device-secret auth
  - `persona-documenter` for documentation & ADRs
  - `persona-code-reviewer` for finding evaluation & adversarial refutation
- [x] 3. Incremental implementation following the 6-Phase technical recipe with local tests passing.
- [x] 4. Final completion report written (`doc/agy/uc-pln-3-plan-report.md`) and initial plan moved to `doc/agy/implemented/uc-pln-3-plan.md`. (Do NOT move report to `verified/` — post-landing verification moves it).
- [x] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit` for code/config; skipped for standalone documentation changes).
- [x] 6. PR opened (with `--label "skip-bot-review"` if authorized), 5-minute review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [x] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [x] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/uc-pln-3-plan-report.md` by orchestrator session.

---

## 4. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Trace** | `plan-marshall-persona-documenter` | `plan-marshall-ref-code-quality` | Verify workflow unit frontmatter, building blocks catalogue schema, static validation stages, and event catalogue keys. |
| **Phase 2: `pm-api`** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-module-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:junit-core` | Immutable Java 25 records, Jackson streaming parsing, bilingual resource bundles (`designer.{en,de}.properties`). |
| **Phase 3: Mock Runtime** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-integration-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:cui-cui-http-testing` | Stateful scenario JSON (`designer.json`), dynamic route handlers, SSE broadcast (`unit.placed`, `unit.landed`, `unit.removed`). |
| **Phase 4: `pm-operator`** | `plan-marshall-persona-documenter` | `plan-marshall-ref-code-quality` | Verify that designer remains strictly web-only without polluting CLI command tree. |
| **Phase 5: `pm-web-app`** | `plan-marshall-persona-implementer`<br>`pm-dev-frontend:javascript` | `pm-dev-frontend:css`<br>`pm-dev-frontend:jest-testing` | Two-pane process designer layout, building block palette with drag/insert, syntax-highlighted editor, validation findings list, Vitest tests. |
| **Phase 6: Verification** | `plan-marshall-persona-integration-tester` | `pm-dev-java:junit-integration` | Playwright E2E browser test suite (`designer.spec.js`). |
| **Phase 7: Review Gate** | `plan-marshall-persona-code-reviewer` | `pm-dev-java:java-security` | Adversarial refutation of candidate bot findings, 100% resolution bar. |

---

## 5. Phased Task Breakdown

### Phase 1: Trace Matrix & Specification Alignment
- Load `plan-marshall-persona-documenter`.
- Map designer contracts:
  - Installed units in two scopes: machine (`GET /api/v1/workflows`) and project (`GET /api/v1/projects/{id}/workflows`).
  - Building blocks catalogue in two scopes: machine and project.
  - Closed block kind enum: `primitive`, `guard`, `link_relation`, `condition`, `sub_workflow`, `role`, `parameter`, `keyword`.
  - Validation endpoint `POST /api/v1/workflows/validate`: stages (parse, static rules, compiled invariants, composition).
  - Placing and removing: `PUT`/`DELETE /api/v1/workflows/{unit}` and `PUT`/`DELETE /api/v1/projects/{id}/workflows/{unit}`.
- Verify path traversal prevention: unit names outside canonical grammar rejected with `unit_name_invalid` (`400`).

### Phase 2: Contracts, Models & Message Catalogue (`pm-api`)
- Load `plan-marshall-persona-implementer` and `pm-dev-java:java-core`.
- Create records in `de.planmarshall.api.designer.*`:
  - `WorkflowUnit.java`: Name, frontmatter (display name, description, author, version, signature), group, levels.
  - `WorkflowCatalogue.java`: Scope, runtime version, digest, building blocks list.
  - `BuildingBlock.java`: Kind, name, summary, template text, parameters, constraints.
  - `BlockKind.java`: Closed enum of building block categories.
  - `WorkflowValidationRequest.java`: Workflow text, unit kind, target project ID.
  - `WorkflowValidationResult.java`: Stages status, list of findings, composed process tree representation if valid.
  - `WorkflowFinding.java`: Stage, rule, line, column, severity, message.
  - `UnitPlacingResult.java`: Status (written, committed, landed), resulting catalogue digest.
- Create bilingual catalogues: `messages/designer.en.properties` and `messages/designer.de.properties`.
- Unit tests with `plan-marshall-persona-module-tester`: `DesignerRecordsTest.java`, `DesignerCatalogueTest.java`.

### Phase 3: Mock Runtime Scenarios & Dynamic Endpoints (`pm-mock-runtime`)
- Load `plan-marshall-persona-implementer` and `pm-dev-java:cui-cui-http-testing`.
- Create scenario fixture `scenarios/designer.json`:
  - Machine and project units, building blocks of every kind, validation errors (syntax error, missing frontmatter, forbidden keyword), successful placing and removal.
- Implement endpoints in `MockRuntimeEngine.java`:
  - `GET /api/v1/workflows` and `GET /api/v1/projects/{id}/workflows`
  - `GET /api/v1/workflows/catalogue` and `GET /api/v1/projects/{id}/workflows/catalogue`
  - `POST /api/v1/workflows/validate`
  - `PUT /api/v1/workflows/{unit}` and `DELETE /api/v1/workflows/{unit}`
- Unit tests: `DesignerScenarioTest.java`.

### Phase 4: CLI Isolation Verification
- Confirm `pm-operator` introduces zero commands for process design, keeping the CLI lean while the web app hosts the visual designer.

### Phase 5: Web Application Views (`pm-web-app`)
- Load `plan-marshall-persona-implementer` and `pm-dev-frontend:javascript`.
- Extend `client.ts` with designer API methods (`getWorkflows`, `getWorkflowCatalogue`, `validateWorkflow`, `placeWorkflowUnit`, `removeWorkflowUnit`).
- Create `ProcessDesigner.svelte`: Route `/designer` with side-by-side palette and editor.
- Create `BlockPalette.svelte`: Categorized building blocks with one-click insertion.
- Create `WorkflowValidationResults.svelte`: Real-time findings display with line/column markers.
- Vitest unit tests: `ProcessDesigner.test.ts`, `BlockPalette.test.ts`.

### Phase 6: Verification & End-to-End Suite
- Load `plan-marshall-persona-integration-tester`.
- Write Playwright E2E suite: `pm-web-e2e/tests/designer.spec.js`.
- Execute `./mvnw verify -Ppre-commit` and ensure 100% clean reactor compilation.

---

## 6. Definition of Done
1. `UC-PLN-3` (Design a Plan's Process) completely implemented in `pm-web-app`.
2. Static validation and building blocks catalogue fully modeled in `pm-api` and served by `pm-mock-runtime`.
3. 100% of user-facing messages externalized in bilingual property bundles (`designer.{en,de}.properties`).
4. Full reactor build `./mvnw verify -Ppre-commit` passes with zero regressions.
5. Completion report documented at `doc/agy/uc-pln-3-plan-report.md` and initial plan archived to `doc/agy/implemented/uc-pln-3-plan.md`.
