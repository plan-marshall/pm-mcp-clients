# Implementation Plan: UC-STA-8 Lesson Plan Sources (Bring in a Lesson Learned)

This implementation plan governs the client-side and simulated-runtime implementation of **UC-STA-8: Bring in a Lesson Learned**, conforming to the operational runbook in [`doc/agy/runbook.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/runbook.md) and the technical recipe in [`doc/meta/implement_use-cases.md`](file:///Users/oliver/git/pm-mcp-clients/doc/meta/implement_use-cases.md).

---

## 1. Context & Core Requirements

- **Use Case**: [`UC-STA-8: Bring in a Lesson`](file:///Users/oliver/git/plan-marshall-documentation/doc/use-cases/05-starting-work.adoc#UC-STA-8)
- **Goal**: Start a plan from a lesson recorded by a former plan, verifying that its premise (cited files and symbols) still holds in the current repository.
- **Specification**:
  - Lesson Claim & Retirement: [`pm-mcp-core/doc/specification/store-schemas/10-job-and-lesson-records.adoc#lesson-claim`](https://github.com/plan-marshall/pm-mcp-core/blob/main/doc/specification/store-schemas/10-job-and-lesson-records.adoc#lesson-claim)
  - Source Units: [`pm-mcp-core/doc/specification/state-catalogue/09-bootstrap-and-lessons.adoc#state-catalogue-source`](https://github.com/plan-marshall/pm-mcp-core/blob/main/doc/specification/state-catalogue/09-bootstrap-and-lessons.adoc#state-catalogue-source)
  - Premise Validation: [`pm-mcp-core/doc/specification/phase-workflows/01-planning-phases-1-2.adoc#_plan_source_contracts`](https://github.com/plan-marshall/pm-mcp-core/blob/main/doc/specification/phase-workflows/01-planning-phases-1-2.adoc#_plan_source_contracts)
- **Core Workflow**:
  - `source-lesson.choose`: Lists unclaimed project lessons, sorted newest first.
  - `source-lesson.resolve`: Verifies premise against repository tree (files and symbols exist).
  - `source-lesson.premise`: If premise fails, operator is presented with missing symbol/file evidence and choices: `use it anyway` (carries evidence into plan), `retire` (retires in store), or `remove` (leaves lesson untouched).
  - `source-lesson.store`: Claims lesson for the plan. Lesson is retired upon successful plan landing (`landing.pull-request` merge or `landing.main` push), or released upon other plan exits (`returned`, `handed_back`, `no_change`, `cancelled`, `abandoned`).

---

## 2. Parallel Isolation & Non-Overlap Invariants (vs `UC-STA-6` & `UC-STA-7/9`)

`UC-STA-8` executes concurrently with `UC-STA-6: Decompose an Epic` and `UC-STA-7 & UC-STA-9: Git Plan Sources`. Strict non-overlap is guaranteed across all modules:

1. **Contracts & API (`pm-api`)**:
   - `UC-STA-8`: Owns package `de.planmarshall.api.source.lesson.*` and bilingual catalogues `messages/lesson_source.{en,de}.properties`.
   - `UC-STA-6`: Owns package `de.planmarshall.api.decomposition.*` and `messages/decomposition.{en,de}.properties`.
   - `UC-STA-7/9`: Owns package `de.planmarshall.api.source.git.*` and `messages/git_sources.{en,de}.properties`.
   - Zero shared records or property keys.
2. **Mock Runtime (`pm-mock-runtime`)**:
   - `UC-STA-8`: Scenario `scenarios/lesson_source.json` and routes under `/api/v1/projects/{id}/sources/lessons/*`.
   - `UC-STA-6`: Scenario `scenarios/decomposition.json` and routes under `/api/v1/projects/{id}/epics/{epic_id}/decomposition/*`.
   - `UC-STA-7/9`: Scenario `scenarios/git_sources.json` and routes under `/api/v1/projects/{id}/sources/pr/*` and `.../sources/local-changes/*`.
   - Disjoint dispatch tables and scenario fixtures.
3. **CLI Client (`pm-operator`)**:
   - `UC-STA-8`: Owns `LessonSourceCommand.java` (`start plan --lesson <id>`, `lesson check <id>`).
   - `UC-STA-6`: Owns `DecomposeCommand.java`.
   - `UC-STA-7/9`: Owns `GitSourceCommand.java`.
   - Disjoint Picocli verb classes.
4. **Web Client (`pm-web-app`)**:
   - `UC-STA-8`: Modals `LessonSourceModal.svelte`, `LessonPicker.svelte`, `PremiseEvidenceView.svelte`.
   - `UC-STA-6`: Route view `EpicDecompositionView.svelte`.
   - `UC-STA-7/9`: Modals `GitSourceIntakeModal.svelte`, `PrAdoptPanel.svelte`, `LocalChangesDiffPreview.svelte`.
   - Disjoint Svelte components.
5. **E2E & Integration Tests**:
   - `UC-STA-8`: `pm-web-e2e/tests/lesson-source.spec.js` and `LessonSourceCommandTest.java`.
   - `UC-STA-6`: `pm-web-e2e/tests/decomposition.spec.js` and `DecomposeCommandTest.java`.
   - `UC-STA-7/9`: `pm-web-e2e/tests/git-sources.spec.js` and `GitSourceCommandTest.java`.

---

## 3. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created on topic branch off latest `main` (`git worktree add -b feat/uc-sta-8-lesson-source /Users/oliver/git/worktrees/pm-mcp-clients-uc-sta-8 main`).
- [ ] 2. Domain skills & engineering personas loaded for each task type:
  - `persona-implementer` & `persona-module-tester` for code & unit tests
  - `persona-integration-tester` for multi-module/integration tests
  - `persona-security-expert` for terminal gates and safe text rendering
  - `persona-documenter` for documentation & ADRs
  - `persona-code-reviewer` for finding evaluation & adversarial refutation
- [ ] 3. Incremental implementation following the 6-Phase technical recipe with local tests passing.
- [ ] 4. Final completion report written (`doc/agy/uc-sta-8-plan-report.md`) and initial plan moved to `doc/agy/implemented/uc-sta-8-plan.md`. (Do NOT move report to `verified/` — post-landing verification moves it).
- [ ] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit` for code/config; skipped for standalone documentation changes).
- [ ] 6. PR opened (with `--label "skip-bot-review"` if authorized), 5-minute review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [ ] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [ ] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/uc-sta-8-plan-report.md` by orchestrator session.

---

## 4. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Trace** | `plan-marshall-persona-documenter` | `plan-marshall-ref-code-quality` | Verify lesson selection flow (`source-lesson.choose` to `source-lesson.store`), premise check contracts, and lesson claim/retire lifecycle. |
| **Phase 2: `pm-api`** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-module-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:junit-core` | Immutable Java 25 records, Jackson streaming parsing, bilingual resource bundles (`messages/lesson_source.{en,de}.properties`). Proactive boundary tests for codec edge cases. |
| **Phase 3: Mock Runtime** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-integration-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:cui-cui-http-testing` | Stateful scenario JSON (`scenarios/lesson_source.json`), dynamic route handlers, SSE broadcast (`source.lesson_claimed`, `source.lesson_retired`). |
| **Phase 4: `pm-operator`** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-security-expert` | `pm-dev-java:java-core`<br>`pm-dev-java:java-security` | Subcommands `start plan --lesson <id>`, `lesson check <id>`, premise failure evidence prompt, PM-SEC-3 confirmation gates. |
| **Phase 5: `pm-web-app`** | `plan-marshall-persona-implementer`<br>`pm-dev-frontend:javascript` | `pm-dev-frontend:css`<br>`pm-dev-frontend:jest-testing` | `LessonSourceModal.svelte`, `LessonPicker.svelte`, `PremiseEvidenceView.svelte`, Vitest test coverage. |
| **Phase 6: Verification** | `plan-marshall-persona-integration-tester` | `pm-dev-java:junit-integration` | `PmOperatorProcessIT` process execution, Playwright E2E browser test suite (`lesson-source.spec.js`). |
| **Phase 7: Review Gate** | `plan-marshall-persona-code-reviewer` | `pm-dev-java:java-security` | Adversarial refutation of candidate bot findings, 100% resolution bar. |

---

## 5. Phased Task Breakdown

### Phase 1: Trace Matrix & Specification Alignment
- Map `UC-STA-8` requirements:
  - Unclaimed lesson queries: Newest first, filtering claimed or retired records.
  - Premise verification: AST symbols check, existing file presence.
  - Three-way premise failure options: `use it anyway`, `retire`, `remove`.
  - Claim and retirement lifecycle: Store transaction semantics (`PM-IMPL-7`).

### Phase 2: Contracts, Models & Message Catalogue (`pm-api`)
- Create records in `de.planmarshall.api.source.lesson.*`:
  - `UnclaimedLessonSummary.java`: Lesson ID, title, summary, recorded date, citing plan ID, symbols cited.
  - `LessonPremiseCheckResult.java`: Status (`holds`/`broken`/`unavailable`), evidence list, cause.
  - `LessonPremiseEvidence.java`: Missing file or symbol name, expected path, last verified commit.
  - `LessonClaimRequest.java`: Project ID, lesson ID, use-anyway override flag.
  - `LessonClaimResult.java`: Status (`claimed`/`refused`), assigned plan ID.
  - `LessonRetirementNotice.java`: Lesson ID, retired status, reason.
- Bilingual catalogues: `messages/lesson_source.en.properties` and `messages/lesson_source.de.properties`.
- Unit tests: `LessonSourceRecordsTest.java` (including null, empty, and malformed boundary tests) and `LessonSourceCatalogueTest.java`.

### Phase 3: Mock Runtime Scenarios & Dynamic Endpoints (`pm-mock-runtime`)
- Fixture `scenarios/lesson_source.json`:
  - Projects with valid unclaimed lessons, broken premise lessons, already claimed lessons, and retirement flows.
- Dynamic endpoints in `MockRuntimeEngine.java`:
  - `GET /api/v1/projects/{id}/sources/lessons`
  - `POST /api/v1/projects/{id}/sources/lessons/{lesson_id}/premise-check`
  - `POST /api/v1/projects/{id}/sources/lessons/{lesson_id}/claim`
  - `POST /api/v1/projects/{id}/sources/lessons/{lesson_id}/retire`
- SSE broadcast: `source.lesson_claimed`, `source.lesson_retired`.
- Unit tests: `LessonSourceScenarioTest.java`.

### Phase 4: CLI Client Commands (`pm-operator`)
- Implement `LessonSourceCommand.java`:
  - `start plan --lesson <id>`: Validates premise, prompts if broken, claims lesson.
  - `lesson check <id>`: Standalone premise verification command.
- Integration tests: `LessonSourceCommandTest.java` and `PmOperatorProcessIT.java`.

### Phase 5: Web Application Views (`pm-web-app`)
- Implement components:
  - `LessonSourceModal.svelte`: Modal for searching and selecting unclaimed lessons.
  - `LessonPicker.svelte`: List of available lessons with preview of premise status.
  - `PremiseEvidenceView.svelte`: Displays broken premise warning with action buttons (`Use anyway`, `Retire`, `Cancel`).
- Vitest unit tests: `LessonSourceModal.test.ts`.

### Phase 6: Verification & End-to-End Suite
- Playwright E2E suite: `pm-web-e2e/tests/lesson-source.spec.js`.
- Full reactor build verification: `./mvnw verify -Ppre-commit`.

---

## 6. Definition of Done
1. `UC-STA-8` (Bring in a Lesson) fully implemented end-to-end across all 5 modules.
2. Premise checking and three-way failure recovery operational in CLI and web UI.
3. 100% of user-facing messages externalized in bilingual property bundles (`lesson_source.{en,de}.properties`).
4. Full reactor build `./mvnw verify -Ppre-commit` passes with zero regressions.
5. Completion report documented at `doc/agy/uc-sta-8-plan-report.md` and initial plan archived to `doc/agy/implemented/uc-sta-8-plan.md`.
