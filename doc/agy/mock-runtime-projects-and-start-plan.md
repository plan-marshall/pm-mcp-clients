# Implementation Plan: `mock-runtime-projects-and-start` (Scenario 3: Consolidated `projects-and-start.json` for Projects, Enrolment & Starting Work)

This implementation plan governs **Scenario 3 (`projects-and-start.json`)** of the 5-scenario mock-runtime architecture: consolidating the 20 fragmented `projects.json`, `enrolment.json`, `git_sources.json`, `lesson_source.json`, `start.json`, and `start-*.json` files into a single, comprehensive `projects-and-start.json` scenario, and eliminating the non-standard top-level `"start_case"` JSON schema bypass in `ScenarioLoader` and `StartRegistry`.

It operationalizes [`doc/meta/implement_use-cases.md`](../meta/implement_use-cases.md) under the execution discipline of [`doc/agy/runbook.md`](runbook.md).

---

## 1. Traceability & Context

| Dimension | Reference |
|---|---|
| **Use Cases** | [`UC-PRJ-1..4`](../../../plan-marshall-documentation/doc/use-cases/04-projects.adoc), [`UC-STR-1..8`](../../../plan-marshall-documentation/doc/use-cases/03-plans.adoc) |
| **Specifications** | [`05-mock-runtime.adoc`](../../../plan-marshall-documentation/doc/specification/runtime-model/05-mock-runtime.adoc), [`04-operator-api.adoc`](../../../plan-marshall-documentation/doc/specification/runtime-model/04-operator-api.adoc) |
| **Local Client Design** | [`doc/design/shared/projects.adoc`](../design/shared/projects.adoc), [`doc/design/shared/starting-work.adoc`](../design/shared/starting-work.adoc) |
| **Roadmap Packages** | [`doc/plans/implemented/project-contract.adoc`](../plans/implemented/project-contract.adoc), [`doc/plans/implemented/start-contract.adoc`](../plans/implemented/start-contract.adoc) |
| **Prerequisite Plans** | [`doc/agy/implemented/cli-start-plan.md`](implemented/cli-start-plan.md) (landed, PR #118), [`doc/agy/implemented/web-start-plan.md`](implemented/web-start-plan.md) (landed, PR #117), [`doc/agy/implemented/mock-runtime-foundation-and-consolidation-plan.md`](implemented/mock-runtime-foundation-and-consolidation-plan.md) (landed, PR #125), [`doc/agy/implemented/mock-runtime-fully-onboarded-scenario-plan.md`](implemented/mock-runtime-fully-onboarded-scenario-plan.md) (landed, PR #143) |
| **Implementation Recipe** | [`doc/meta/implement_use-cases.md`](../meta/implement_use-cases.md) |
| **Operational SOP** | [`doc/agy/runbook.md`](runbook.md) |

---

## 2. Scope & Objectives

1. **Consolidate 20 Project & Start Files into Scenario 3 (`projects-and-start.json`)**:
   - Merge `projects.json`, `enrolment.json`, `git_sources.json`, `lesson_source.json`, `start.json`, `start-chat.json`, `start-documents.json`, `start-entry-continue.json`, `start-epic-adopt.json`, `start-epic.json`, `start-follow-up.json`, `start-issue-failed.json`, `start-lesson.json`, `start-local-changes.json`, `start-pull-request.json`, `start-rate-limited.json`, `start-recipe.json`, `start-refused.json`, `start-sources.json`, and `start-unscreened.json` into **`pm-mock-runtime/src/main/resources/scenarios/projects-and-start.json`**.
   - Delete the 20 superseded JSON files from `src/main/resources/scenarios/` (using the legacy alias table pre-registered in `ScenarioRegistry` by `mock-runtime-foundation-and-consolidation-plan.md`).
2. **Migrate `StartRegistry` Off Non-Standard `"start_case"` Top-Level Property**:
   - Store each start case (`start.sources`, `start.chat`, `start.documents`, `start.entry-continue`, `start.epic`, `start.epic-adopt`, `start.follow-up`, `start.issue-failed`, `start.lesson`, `start.local-changes`, `start.pull-request`, `start.rate-limited`, `start.recipe`, `start.refused`, `start.unscreened`) as a first-class state in `projects-and-start.json` whose `OperatorStateResource.data` and `links` carry the case fixture, and seed `StartRegistry` from the active state's `OperatorStateResource` and `routes` (so `mock-runtime-reports-system-designer-plan.md` can remove the legacy `startCase` field from `Scenario` / `ScenarioLoader` in Wave 3).
3. **Complete All Missing `projects-<case>` and `start-<case>` States & Routes**:
   - Cover all ~18 cases in `project-contract.adoc`:
     - Enrolled projects (`ready`, `attention_needed`, `being_added`, `unenrolled`), favourite toggling and reordering, project detail overview, git file tree & file content reader, `.plan/marshal.json` store digest & `accept-edit` confirmation mismatch vs success, enrolment scanning/adding/removing, and ended-work archive browsing & chat.
   - Cover all 15+ cases in `start-contract.adoc` with valid `OperatorStateResource` representations, `OperatorLink` entries, and `EventEnvelope` events.

---

## 3. Concurrency & Isolation

- **Prerequisites Satisfied**: All prerequisites (`cli-start`, `web-start`, `mock-runtime-foundation`, `mock-runtime-onboarding-and-entry`, `mock-runtime-running-work`, and `mock-runtime-fully-onboarded`) have landed on `main`.
- **Parallelizable with**: `web-decisions-and-actions-plan.md` and `cli-fullscreen-actions-plan.md`.

### Boundary Rules
- **Sole Owner of**: `pm-mock-runtime/src/main/resources/scenarios/projects-and-start.json`, `StartRegistry.java`, deletion of the 20 superseded project/start JSON files, and `ProjectsScenarioTest.java`, `EnrolScenarioTest.java`, `GitSourceScenarioTest.java`, `LessonSourceScenarioTest.java`, `StartScenarioTest.java`, and `StartContractTest.java`.
- **No Touch**: `Scenario.java` / `ScenarioLoader.java` / `ScenarioRegistry.java` / `StandardScenarios.java`, `doc/plans/README.adoc`, `pm-operator`, and `pm-web-app`.

---

## 4. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [ ] 1. Isolated worktree created at `/Users/oliver/git/worktrees/pm-mcp-clients-mock-projects-start` on branch `feat/mock-runtime-projects-and-start` off latest `main`.
- [ ] 2. Domain skills & engineering personas loaded where applicable:
  - `persona-implementer` & `persona-module-tester` for code & unit tests
  - `persona-integration-tester` for multi-module/integration tests
  - `persona-security-expert` for trust boundaries & input validation
  - `persona-documenter` for documentation & ADRs
  - `persona-code-reviewer` for finding evaluation & adversarial refutation
- [ ] 3. Incremental implementation with tests following the phased technical recipe.
- [ ] 4. Final completion report written (`doc/agy/mock-runtime-projects-and-start-plan-report.md`) and initial plan moved to `doc/agy/implemented/mock-runtime-projects-and-start-plan.md`. (Do NOT move report to `verified/` — Step 8 is reserved for orchestrator).
- [ ] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit` for code/config; skipped for standalone documentation).
- [ ] 6. PR opened (NEVER use `skip-bot-review` on worker PRs; reserved for orchestrator docs PRs), review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [ ] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [ ] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/mock-runtime-projects-and-start-plan-report.md` by orchestrator session.

---

## 5. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: `StartRegistry` Migration** | `plan-marshall-persona-implementer` | `pm-dev-java:java-core` | Read start fixtures in `StartRegistry` from `OperatorStateResource` states & `routes` instead of `start_case`. |
| **Phase 2: Projects & Start Consolidation** | `plan-marshall-persona-implementer` | `pm-dev-java:java-core` | Build `projects-and-start.json` combining all project explorer, enrolment, git/lesson sources, and start states. |
| **Phase 3: Scenario Contract Tests** | `plan-marshall-persona-module-tester`<br>`plan-marshall-persona-integration-tester` | `pm-dev-java:junit-core` | Verify all project and start contract cases against `projects-and-start.json`. |
| **Phase 4: Review Gate** | `plan-marshall-persona-code-reviewer` | `pm-dev-java:java-security` | Pre-commit gate and 100% resolution of automated review findings. |

---

## 6. Phased Technical Recipe

### Phase 1: Migrate `StartRegistry` Off Non-Standard `"start_case"` Field
- [ ] **1.1 Refactor `StartRegistry` to Read State `data` and `routes`**:
  - Update `StartRegistry` so that instead of reading `scenario.startCase()`, it initializes from the active state's `OperatorStateResource` (`data` map and `links`) and scenario `routes`.

### Phase 2: Author Consolidated `projects-and-start.json`
- [ ] **2.1 Consolidate Project Explorer, Enrolment & Source States**:
  - Author `projects-and-start.json` with full `OperatorStateResource` states for project browsing (`projects.ready`, `projects.empty`, `projects.attention`, `projects.being-added`, `projects.unenrolled`, `enrol.scan`, `enrol.digest-mismatch`, `sources.git`, `sources.lessons`) and declarative `routes` for `/api/v1/projects`, `/api/v1/enrol`, `/api/v1/recipes`, etc.
- [ ] **2.2 Consolidate All 15 `start.*` States**:
  - Include `start.sources`, `start.chat`, `start.documents`, `start.entry-continue`, `start.epic`, `start.epic-adopt`, `start.follow-up`, `start.issue-failed`, `start.lesson`, `start.local-changes`, `start.pull-request`, `start.rate-limited`, `start.recipe`, `start.refused`, and `start.unscreened` in `projects-and-start.json` with complete `OperatorLink` lists and `EventEnvelope` events.
  - Delete the 20 superseded JSON files.

### Phase 3: Contract Verification Tests
- [ ] **3.1 Expand Test Suites**:
  - Update `ProjectsScenarioTest`, `EnrolScenarioTest`, `GitSourceScenarioTest`, `LessonSourceScenarioTest`, `StartScenarioTest`, and `StartContractTest` to verify every case in `project-contract.adoc` and `start-contract.adoc` against `projects-and-start.json`.
- [ ] **3.2 Pre-Commit & Operational Gate**:
  - Run `./mvnw verify -Ppre-commit` and execute PR/review workflow per [`doc/agy/runbook.md`](runbook.md).
