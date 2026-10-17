# Implementation Plan: UC-STA-6 Decompose an Epic into Workstreams & Staged Plans

This implementation plan governs the client-side and simulated-runtime implementation of **UC-STA-6: Decompose an Epic into Workstreams and Staged Plans**, conforming to the operational runbook in [`doc/agy/runbook.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/runbook.md) and the technical recipe in [`doc/meta/implement_use-cases.md`](file:///Users/oliver/git/pm-mcp-clients/doc/meta/implement_use-cases.md).

---

## 1. Context & Core Requirements

- **Use Case**: [`UC-STA-6: Decompose an Epic`](file:///Users/oliver/git/plan-marshall-documentation/doc/use-cases/05-starting-work.adoc#UC-STA-6)
- **Goal**: Get from an epic's starting sources to its workstreams and staged plans, cut the way the operator wants them, before any plan starts.
- **Specification**:
  - Epic Coordination: [`doc/specification/model-work/08-epic-coordination.adoc#decomposition-review`](https://github.com/plan-marshall/pm-mcp-core/blob/main/doc/specification/model-work/08-epic-coordination.adoc#decomposition-review)
  - Surface Disjointness: [`doc/specification/model-work/08-epic-coordination.adoc#epic-surface-disjointness`](https://github.com/plan-marshall/pm-mcp-core/blob/main/doc/specification/model-work/08-epic-coordination.adoc#epic-surface-disjointness)
  - Decomposition States: [`doc/specification/state-catalogue/17-epic-decomposition-and-review.adoc#state-catalogue-decomposition`](https://github.com/plan-marshall/pm-mcp-core/blob/main/doc/specification/state-catalogue/17-epic-decomposition-and-review.adoc#state-catalogue-decomposition)
- **Core Workflow**:
  - `decomposition.draft`: Planning role `decompose` drafts workstreams and staged plans (objective, deliverables, surface, dependencies, ordering).
  - `decomposition.check`: Server validation verifies surface disjointness of concurrent plans, acyclic dependency graph, and workstream integrity.
  - `decomposition.review`: Interactive review tree with per-plan actions (`keep`, `edit`, `drop`, `add`), operator-word instructions, rewrite with read-back comparison (`decomposition.read-back`), and planning role chat.
  - `decomposition.commit`: Agreed workstreams and staged plans written and committed to the epic ledger.

---

## 2. Parallel Isolation & Non-Overlap Invariants (vs `UC-STA-7/9` & `UC-STA-8`)

`UC-STA-6` runs concurrently with `UC-STA-7 & UC-STA-9: Git Plan Sources` and `UC-STA-8: Lesson Plan Sources`. Strict structural non-overlap is guaranteed across all modules:

1. **Contracts & API (`pm-api`)**:
   - `UC-STA-6`: Owns package `de.planmarshall.api.decomposition.*` and bilingual catalogues `messages/decomposition.{en,de}.properties`.
   - `UC-STA-7/9`: Owns package `de.planmarshall.api.source.git.*` and `messages/git_sources.{en,de}.properties`.
   - `UC-STA-8`: Owns package `de.planmarshall.api.source.lesson.*` and `messages/lesson_source.{en,de}.properties`.
   - Zero shared records or property keys.
2. **Mock Runtime (`pm-mock-runtime`)**:
   - `UC-STA-6`: Scenario `scenarios/decomposition.json` and routes under `/api/v1/projects/{id}/epics/{epic_id}/decomposition/*`.
   - `UC-STA-7/9`: Scenario `scenarios/git_sources.json` and routes under `/api/v1/projects/{id}/sources/pr/*` and `.../sources/local-changes/*`.
   - `UC-STA-8`: Scenario `scenarios/lesson_source.json` and routes under `/api/v1/projects/{id}/sources/lessons/*`.
   - Disjoint dispatch branches and scenario files.
3. **CLI Client (`pm-operator`)**:
   - `UC-STA-6`: Owns `DecomposeCommand.java` (`epic decompose`, `epic review`).
   - `UC-STA-7/9`: Owns `GitSourceCommand.java`.
   - `UC-STA-8`: Owns `LessonSourceCommand.java`.
   - Disjoint Picocli verb classes.
4. **Web Client (`pm-web-app`)**:
   - `UC-STA-6`: Route view `EpicDecompositionView.svelte` at `/epics/[id]/decomposition`.
   - `UC-STA-7/9`: Modals `GitSourceIntakeModal.svelte`, `PrAdoptPanel.svelte`, `LocalChangesDiffPreview.svelte`.
   - `UC-STA-8`: Modals `LessonSourceModal.svelte`, `LessonPicker.svelte`.
   - Disjoint Svelte components.
5. **E2E & Integration Tests**:
   - `UC-STA-6`: `pm-web-e2e/tests/decomposition.spec.js` and `DecomposeCommandTest.java`.
   - `UC-STA-7/9`: `pm-web-e2e/tests/git-sources.spec.js` and `GitSourceCommandTest.java`.
   - `UC-STA-8`: `pm-web-e2e/tests/lesson-source.spec.js` and `LessonSourceCommandTest.java`.

---

## 3. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created on topic branch off latest `main` (`git worktree add -b feat/uc-sta-6-decomposition /Users/oliver/git/worktrees/pm-mcp-clients-uc-sta-6 main`).
- [x] 2. Domain skills & engineering personas loaded for each task type:
  - `persona-implementer` & `persona-module-tester` for code & unit tests
  - `persona-integration-tester` for multi-module/integration tests
  - `persona-security-expert` for terminal gates and safe text rendering
  - `persona-documenter` for documentation & ADRs
  - `persona-code-reviewer` for finding evaluation & adversarial refutation
- [x] 3. Incremental implementation following the 6-Phase technical recipe with local tests passing.
- [x] 4. Final completion report written (`doc/agy/uc-sta-6-plan-report.md`) and initial plan moved to `doc/agy/implemented/uc-sta-6-plan.md`. (Do NOT move report to `verified/` — post-landing verification moves it).
- [x] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit` for code/config; skipped for standalone documentation changes).
- [ ] 6. PR opened (with `--label "skip-bot-review"` if authorized), 5-minute review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [ ] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [ ] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/uc-sta-6-plan-report.md` by orchestrator session.

---

## 4. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Trace** | `plan-marshall-persona-documenter` | `plan-marshall-ref-code-quality` | Verify decomposition state machine (`decomposition.draft` to `decomposition.commit`), surface disjointness validation rules, and cycle detection. |
| **Phase 2: `pm-api`** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-module-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:junit-core` | Immutable Java 25 records, Jackson streaming parsing, bilingual resource bundles (`messages/decomposition.{en,de}.properties`). Proactive boundary testing for codec edge cases. |
| **Phase 3: Mock Runtime** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-integration-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:cui-cui-http-testing` | Stateful scenario JSON (`scenarios/decomposition.json`), dynamic route handlers, SSE broadcast (`epic.decomposition_updated`, `epic.launchable`). |
| **Phase 4: `pm-operator`** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-security-expert` | `pm-dev-java:java-core`<br>`pm-dev-java:java-security` | Subcommands `epic decompose`, `epic review`, interactive review tree, instruction prompt, PM-SEC-3 confirmation gates. |
| **Phase 5: `pm-web-app`** | `plan-marshall-persona-implementer`<br>`pm-dev-frontend:javascript` | `pm-dev-frontend:css`<br>`pm-dev-frontend:jest-testing` | `EpicDecompositionView.svelte` at `/epics/[id]/decomposition`, workstream cards, staged plan surface conflict tags, read-back diff comparison. |
| **Phase 6: Verification** | `plan-marshall-persona-integration-tester` | `pm-dev-java:junit-integration` | `PmOperatorProcessIT` process execution, Playwright E2E browser test suite (`decomposition.spec.js`). |
| **Phase 7: Review Gate** | `plan-marshall-persona-code-reviewer` | `pm-dev-java:java-security` | Adversarial refutation of candidate bot findings, 100% resolution bar. |

---

## 5. Phased Task Breakdown

### Phase 1: Trace Matrix & Specification Alignment
- Map `UC-STA-6` against `pm-mcp-core` specifications:
  - Epic ledger state representation
  - Staging-time surface disjointness checks
  - Workstream tree hierarchy
  - Read-back comparison (`decomposition.read-back`)
  - Acyclic dependency validation

### Phase 2: Contracts, Models & Message Catalogue (`pm-api`)
- Create records in `de.planmarshall.api.decomposition.*`:
  - `EpicDecomposition.java`: Epic ID, status, revision, list of workstreams, list of check findings.
  - `WorkstreamRecord.java`: ID, name, description, order, list of staged plans.
  - `StagedPlanRecord.java`: Plan ID, title, objective, expected surface, dependencies, status.
  - `DecompositionCheckFinding.java`: Severity, rule, affected plans, explanation message.
  - `DecompositionInstructRequest.java`: Plan ID, operator words instruction, action (`edit`/`add`/`rewrite`).
  - `DecompositionCommitResult.java`: Status, committed ledger revision, launchable plan count.
- Bilingual catalogues: `messages/decomposition.en.properties` and `messages/decomposition.de.properties`.
- Unit tests: `DecompositionRecordsTest.java` (including null, empty, and malformed boundary tests) and `DecompositionCatalogueTest.java`.

### Phase 3: Mock Runtime Scenarios & Dynamic Endpoints (`pm-mock-runtime`)
- Fixture `scenarios/decomposition.json`:
  - Epic with multiple workstreams, staged plans with surface overlaps, valid decomposition, commit state.
- Dynamic endpoints in `MockRuntimeEngine.java`:
  - `GET /api/v1/projects/{id}/epics/{epic_id}/decomposition`
  - `POST /api/v1/projects/{id}/epics/{epic_id}/decomposition/check`
  - `POST /api/v1/projects/{id}/epics/{epic_id}/decomposition/instruct`
  - `POST /api/v1/projects/{id}/epics/{epic_id}/decomposition/commit`
- SSE broadcast: `epic.decomposition_updated`, `epic.launchable`.
- Unit tests: `DecompositionScenarioTest.java`.

### Phase 4: CLI Client Commands (`pm-operator`)
- Implement `DecomposeCommand.java` with subcommands:
  - `epic decompose <epic-id>`: Initiate or review decomposition tree.
  - `epic review <epic-id>`: Interactive plan review (keep, edit, drop, agree).
  - `epic instruct <epic-id> --plan <plan-id>`: Pass operator words for rewrite.
- Integration tests: `DecomposeCommandTest.java` and `PmOperatorProcessIT.java`.

### Phase 5: Web Application Views (`pm-web-app`)
- Implement `EpicDecompositionView.svelte` at `/epics/[id]/decomposition`:
  - Workstream groupings and ordered plan nodes.
  - Conflict warning banner showing non-disjoint plan surfaces.
  - Instruction modal for editing/adding plans with read-back comparison.
  - Agree & Commit button with auto-launch status indication.
- Vitest unit tests: `EpicDecompositionView.test.ts`.

### Phase 6: Verification & End-to-End Suite
- Playwright E2E suite: `pm-web-e2e/tests/decomposition.spec.js`.
- Full reactor build verification: `./mvnw verify -Ppre-commit`.

---

## 6. Definition of Done
1. `UC-STA-6` (Decompose an Epic) fully implemented end-to-end across all 5 modules.
2. Staging surface disjointness and dependency cycle validation operational in mock runtime and UI.
3. 100% of user-facing messages externalized in bilingual property bundles (`decomposition.{en,de}.properties`).
4. Full reactor build `./mvnw verify -Ppre-commit` passes with zero regressions.
5. Completion report documented at `doc/agy/uc-sta-6-plan-report.md` and initial plan archived to `doc/agy/implemented/uc-sta-6-plan.md`.
