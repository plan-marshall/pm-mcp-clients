# Implementation Plan: UC-STA-7 & UC-STA-9 Git Plan Sources (Adopt PR & Capture Local Changes)

This implementation plan governs the client-side and simulated-runtime implementation of **UC-STA-7: Adopt a Pull Request** and **UC-STA-9: Capture Local Changes**, conforming to the operational runbook in [`doc/agy/runbook.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/runbook.md) and the technical recipe in [`doc/meta/implement_use-cases.md`](file:///Users/oliver/git/pm-mcp-clients/doc/meta/implement_use-cases.md).

---

## 1. Context & Core Requirements

- **Use Cases**:
  - [`UC-STA-7: Adopt a Pull Request`](file:///Users/oliver/git/plan-marshall-documentation/doc/use-cases/05-starting-work.adoc#UC-STA-7)
  - [`UC-STA-9: Capture Local Changes`](file:///Users/oliver/git/plan-marshall-documentation/doc/use-cases/05-starting-work.adoc#UC-STA-9)
- **Goal**:
  - Adopt an external pull request to have a plan take over its branch in the `adopt` lane.
  - Hand uncommitted working tree changes to a plan that captures them as a snapshot commit and lands them in the `land` lane.
- **Specification**:
  - PR Observation & Screening: [`pm-mcp-core/doc/specification/model-work/07-untrusted-text-screening.adoc`](https://github.com/plan-marshall/pm-mcp-core/blob/main/doc/specification/model-work/07-untrusted-text-screening.adoc)
  - Adopt & Land Lanes: [`pm-mcp-core/doc/specification/phase-workflows/02-planning-lanes-phases-3-4.adoc`](https://github.com/plan-marshall/pm-mcp-core/blob/main/doc/specification/phase-workflows/02-planning-lanes-phases-3-4.adoc)
  - Local Changes Preconditions: In force landing must NOT be `landing.files` (`PM-WF-25`).
- **Core Workflow**:
  - **Adopt PR**: Input PR URL or number -> observe via provider (writable head, open status, rate limiting check) -> level-1 validate title & body -> untrusted text screening -> store changed files marked untrusted unless operator-authored -> plan created in `adopt` lane.
  - **Capture Local Changes**: Preview diff against branch with changed file line counts -> confirm gate -> snapshot commit recorded -> plan created in `land` lane (calls land, then finalize).

---

## 2. Parallel Isolation & Non-Overlap Invariants (vs `UC-STA-6` & `UC-STA-8`)

`UC-STA-7 & UC-STA-9` executes concurrently with `UC-STA-6: Decompose an Epic` and `UC-STA-8: Lesson Plan Sources`. Strict non-overlap is guaranteed across all modules:

1. **Contracts & API (`pm-api`)**:
   - `UC-STA-7/9`: Owns package `de.planmarshall.api.source.git.*` and bilingual catalogues `messages/git_sources.{en,de}.properties`.
   - `UC-STA-6`: Owns package `de.planmarshall.api.decomposition.*` and `messages/decomposition.{en,de}.properties`.
   - `UC-STA-8`: Owns package `de.planmarshall.api.source.lesson.*` and `messages/lesson_source.{en,de}.properties`.
   - Zero shared records or property keys.
2. **Mock Runtime (`pm-mock-runtime`)**:
   - `UC-STA-7/9`: Scenario `scenarios/git_sources.json` and routes under `/api/v1/projects/{id}/sources/pr/*` and `.../sources/local-changes/*`.
   - `UC-STA-6`: Scenario `scenarios/decomposition.json` and routes under `/api/v1/projects/{id}/epics/{epic_id}/decomposition/*`.
   - `UC-STA-8`: Scenario `scenarios/lesson_source.json` and routes under `/api/v1/projects/{id}/sources/lessons/*`.
   - Disjoint dispatch branches and scenario fixtures.
3. **CLI Client (`pm-operator`)**:
   - `UC-STA-7/9`: Owns `GitSourceCommand.java` (`start plan --adopt-pr <url|num>`, `start plan --local-changes`).
   - `UC-STA-6`: Owns `DecomposeCommand.java`.
   - `UC-STA-8`: Owns `LessonSourceCommand.java`.
   - Disjoint Picocli verb classes.
4. **Web Client (`pm-web-app`)**:
   - `UC-STA-7/9`: Modals `GitSourceIntakeModal.svelte`, `PrAdoptPanel.svelte`, `LocalChangesDiffPreview.svelte`.
   - `UC-STA-6`: Route view `EpicDecompositionView.svelte`.
   - `UC-STA-8`: Modals `LessonSourceModal.svelte`, `LessonPicker.svelte`.
   - Disjoint Svelte components.
5. **E2E & Integration Tests**:
   - `UC-STA-7/9`: `pm-web-e2e/tests/git-sources.spec.js` and `GitSourceCommandTest.java`.
   - `UC-STA-6`: `pm-web-e2e/tests/decomposition.spec.js` and `DecomposeCommandTest.java`.
   - `UC-STA-8`: `pm-web-e2e/tests/lesson-source.spec.js` and `LessonSourceCommandTest.java`.

---

## 3. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created on topic branch off latest `main` (`git worktree add -b feat/uc-sta-7-9-git-sources /Users/oliver/git/worktrees/pm-mcp-clients-uc-sta-7-9 main`).
- [x] 2. Domain skills & engineering personas loaded for each task type:
  - `persona-implementer` & `persona-module-tester` for code & unit tests
  - `persona-integration-tester` for multi-module/integration tests
  - `persona-security-expert` for untrusted PR content screening and terminal confirmation gates
  - `persona-documenter` for documentation & ADRs
  - `persona-code-reviewer` for finding evaluation & adversarial refutation
- [x] 3. Incremental implementation following the 6-Phase technical recipe with local tests passing.
- [x] 4. Final completion report written (`doc/agy/uc-sta-7-9-plan-report.md`) and initial plan moved to `doc/agy/implemented/uc-sta-7-9-plan.md`. (Do NOT move report to `verified/` — post-landing verification moves it).
- [x] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit` for code/config; skipped for standalone documentation changes).
- [x] 6. PR opened (with `--label "skip-bot-review"` if authorized), 5-minute review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [x] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [x] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/uc-sta-7-9-plan-report.md` by orchestrator session.

---

## 4. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Trace** | `plan-marshall-persona-documenter` | `plan-marshall-ref-code-quality` | Verify PR adoption state machine (`source-pr.name` to `source-pr.screen`), local changes diff preview states, and `PM-WF-25` landing axis checks. |
| **Phase 2: `pm-api`** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-module-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:junit-core` | Immutable Java 25 records, Jackson streaming parsing, bilingual resource bundles (`messages/git_sources.{en,de}.properties`). Proactive boundary tests for codec edge cases. |
| **Phase 3: Mock Runtime** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-integration-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:cui-cui-http-testing` | Stateful scenario JSON (`scenarios/git_sources.json`), dynamic route handlers, SSE broadcast (`source.pr_observed`, `source.local_changes_captured`). |
| **Phase 4: `pm-operator`** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-security-expert` | `pm-dev-java:java-core`<br>`pm-dev-java:java-security` | Subcommands `start plan --adopt-pr`, `start plan --local-changes`, diff formatting, PM-SEC-3 confirmation gates. |
| **Phase 5: `pm-web-app`** | `plan-marshall-persona-implementer`<br>`pm-dev-frontend:javascript` | `pm-dev-frontend:css`<br>`pm-dev-frontend:jest-testing` | `GitSourceIntakeModal.svelte`, `PrAdoptPanel.svelte`, `LocalChangesDiffPreview.svelte`, Vitest test coverage. |
| **Phase 6: Verification** | `plan-marshall-persona-integration-tester` | `pm-dev-java:junit-integration` | `PmOperatorProcessIT` process execution, Playwright E2E browser test suite (`git-sources.spec.js`). |
| **Phase 7: Review Gate** | `plan-marshall-persona-code-reviewer` | `pm-dev-java:java-security` | Adversarial refutation of candidate bot findings, 100% resolution bar. |

---

## 5. Phased Task Breakdown

### Phase 1: Trace Matrix & Specification Alignment
- Map `UC-STA-7` and `UC-STA-9` contracts:
  - PR observation: Open status, head branch writability, rate limiting reset headers.
  - Untrusted text screening: Flagged/unscreened/clean outcomes.
  - Local changes: Preview of modified files and line counts.
  - Landing precondition: Refusal when `landing.files` is in force (`PM-WF-25`).

### Phase 2: Contracts, Models & Message Catalogue (`pm-api`)
- Create records in `de.planmarshall.api.source.git.*`:
  - `PullRequestObservation.java`: URL, number, title, author, branch, writable, changed files list, rate limit status.
  - `PrAdoptRequest.java`: Project ID, PR URL, screening bypass reason if flagged.
  - `PrAdoptResult.java`: Plan ID, lane (`adopt`), branch, status.
  - `LocalChangesPreview.java`: Status, modified files with additions/deletions counts, eligible landing check.
  - `LocalChangesCaptureRequest.java`: Project ID, confirmation flag.
  - `LocalChangesCaptureResult.java`: Plan ID, lane (`land`), snapshot commit SHA.
- Bilingual catalogues: `messages/git_sources.en.properties` and `messages/git_sources.de.properties`.
- Unit tests: `GitSourceRecordsTest.java` (with boundary test coverage for malformed inputs) and `GitSourceCatalogueTest.java`.

### Phase 3: Mock Runtime Scenarios & Dynamic Endpoints (`pm-mock-runtime`)
- Fixture `scenarios/git_sources.json`:
  - Projects with open PRs, rate-limited PRs, uncommitted local changes, and `landing.files` refusal scenario.
- Dynamic endpoints in `MockRuntimeEngine.java`:
  - `POST /api/v1/projects/{id}/sources/pr/observe`
  - `POST /api/v1/projects/{id}/sources/pr/adopt`
  - `GET /api/v1/projects/{id}/sources/local-changes/preview`
  - `POST /api/v1/projects/{id}/sources/local-changes/capture`
- SSE broadcast: `source.pr_observed`, `source.local_changes_captured`.
- Unit tests: `GitSourceScenarioTest.java`.

### Phase 4: CLI Client Commands (`pm-operator`)
- Implement `GitSourceCommand.java`:
  - `start plan --adopt-pr <url|number>`: Observation, screening verdict, interactive confirmation.
  - `start plan --local-changes`: Shows diff summary, checks `landing.files` precondition, prompts confirmation.
- Integration tests: `GitSourceCommandTest.java` and `PmOperatorProcessIT.java`.

### Phase 5: Web Application Views (`pm-web-app`)
- Implement components:
  - `GitSourceIntakeModal.svelte`: Modal supporting PR Adoption and Local Changes tabs.
  - `PrAdoptPanel.svelte`: PR URL input, live observation status card, screening warning badge.
  - `LocalChangesDiffPreview.svelte`: File tree diff preview with line additions/deletions and land lane indicator.
- Vitest unit tests: `GitSourceIntakeModal.test.ts`.

### Phase 6: Verification & End-to-End Suite
- Playwright E2E suite: `pm-web-e2e/tests/git-sources.spec.js`.
- Full reactor build verification: `./mvnw verify -Ppre-commit`.

---

## 6. Definition of Done
1. `UC-STA-7` (Adopt a Pull Request) and `UC-STA-9` (Capture Local Changes) fully implemented across all 5 modules.
2. PR observation, screening flags, and local changes diff previews operating faithfully in CLI and web UI.
3. 100% of user-facing messages externalized in bilingual property bundles (`git_sources.{en,de}.properties`).
4. Full reactor build `./mvnw verify -Ppre-commit` passes with zero regressions.
5. Completion report documented at `doc/agy/uc-sta-7-9-plan-report.md` and initial plan archived to `doc/agy/implemented/uc-sta-7-9-plan.md`.
