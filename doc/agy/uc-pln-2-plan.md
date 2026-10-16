# Implementation Plan: UC-PLN-2 Decide What a Plan Asks (Decisions & Waivers)

This plan coordinates the end-to-end implementation of use case `UC-PLN-2: Decide What a Plan Asks` (Decisions, Waivers, Answers in Own Words, and Quorum Voting) across all client and runtime modules in `pm-mcp-clients`.

It operationalizes the technical recipe from [`doc/meta/implement_use-cases.md`](file:///Users/oliver/git/pm-mcp-clients/doc/meta/implement_use-cases.md) under the execution discipline of [`doc/agy/runbook.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/runbook.md).

---

## 1. Traceability & Context

| Dimension | Reference |
|---|---|
| **Use Case** | [`plan-marshall-documentation/doc/use-cases/03-plans.adoc#UC-PLN-2`](file:///Users/oliver/git/plan-marshall-documentation/doc/use-cases/03-plans.adoc) (with chat link to [`UC-CTL-1`](file:///Users/oliver/git/plan-marshall-documentation/doc/use-cases/06-controlling-work.adoc#UC-CTL-1)) |
| **Specifications** | [`06-envelopes-questions-deadlines.adoc#decision-structure`](file:///Users/oliver/git/pm-mcp-core/doc/specification/model-work/06-envelopes-questions-deadlines.adoc), [`#own-words`](file:///Users/oliver/git/pm-mcp-core/doc/specification/model-work/06-envelopes-questions-deadlines.adoc), [`10-decision-quorum.adoc`](file:///Users/oliver/git/pm-mcp-core/doc/specification/model-work/10-decision-quorum.adoc), [`14-plan-instance-waivers-questions.adoc#cli-answers`](file:///Users/oliver/git/pm-mcp-core/doc/specification/store-schemas/14-plan-instance-waivers-questions.adoc), [`16-decision.adoc#state-catalogue-decision`](file:///Users/oliver/git/pm-mcp-core/doc/specification/state-catalogue/16-decision.adoc), [`01-processes-and-transport.adoc#local-api`](file:///Users/oliver/git/plan-marshall-documentation/doc/specification/runtime-model/01-processes-and-transport.adoc), [`06-event-catalogue.adoc`](file:///Users/oliver/git/plan-marshall-documentation/doc/specification/runtime-model/06-event-catalogue.adoc) |
| **Local Client Design** | Shared: [`doc/design/shared/decisions.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/design/shared/decisions.adoc)<br>CLI: [`doc/design/terminal-client/17-decisions.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/design/terminal-client/17-decisions.adoc), [`14-transcripts-decisions.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/design/terminal-client/14-transcripts-decisions.adoc)<br>Web: [`doc/design/web-app/README.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/design/web-app/README.adoc) |
| **Roadmap Packages** | [`doc/plans/decision-contract.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/plans/decision-contract.adoc), [`doc/plans/cli-decisions.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/plans/cli-decisions.adoc), [`doc/plans/web-decisions.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/plans/web-decisions.adoc) |
| **Implementation Recipe** | [`doc/meta/implement_use-cases.md`](file:///Users/oliver/git/pm-mcp-clients/doc/meta/implement_use-cases.md) |
| **Operational SOP** | [`doc/agy/runbook.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/runbook.md) |

---

## 2. Parallel Isolation & Non-Overlap Invariants (vs `UC-SYS-1..3`)

`UC-PLN-2` executes concurrently with `UC-SYS-1..3: System View, Runtime Lifecycle & Cleanup` (`feat/uc-sys-cleanup`). Strict non-overlap is guaranteed across all repository modules:

1. **Contracts & API (`pm-api`)**:
   - `UC-PLN-2`: Owns `de.planmarshall.api.decision.*` and `messages/decisions.{en,de}.properties`.
   - `UC-SYS-1..3`: Owns `de.planmarshall.api.system.*` and `messages/system.{en,de}.properties`.
   - Zero shared model classes, enums, or resource properties.
2. **Mock Runtime (`pm-mock-runtime`)**:
   - `UC-PLN-2`: Routes `/api/v1/decisions*`, scenario `scenarios/decisions.json`.
   - `UC-SYS-1..3`: Routes `/api/v1/system*` and `/api/v1/operator/runtime*`, scenario `scenarios/system.json`.
   - Disjoint route dispatch tables and scenario files.
3. **CLI Client (`pm-operator`)**:
   - `UC-PLN-2`: Owns `QuestionsCommand.java` and `AnswerCommand.java`.
   - `UC-SYS-1..3`: Owns `SystemCommand.java`, `DrainCommand.java`, and `CleanupCommand.java`.
   - Disjoint Picocli verb classes.
4. **Web Client (`pm-web-app`)**:
   - `UC-PLN-2`: Route `/decisions` or questions modal/panel with `DecisionView.svelte`.
   - `UC-SYS-1..3`: Route `/system` with `SystemView.svelte` and `CleanupView.svelte`.
   - Disjoint Svelte components and route views.
5. **E2E & Integration Tests**:
   - `UC-PLN-2`: `pm-web-e2e/tests/decisions.spec.js` and `QuestionsCommandTest.java`.
   - `UC-SYS-1..3`: `pm-web-e2e/tests/system.spec.js` and `SystemCommandTest.java`.

---

## 3. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [ ] 1. Isolated worktree created on topic branch off latest `main` (`git worktree add -b feat/uc-pln-2-decisions /Users/oliver/git/worktrees/pm-mcp-clients-uc-pln-2 main`).
- [ ] 2. Domain skills & engineering personas loaded for each task type:
  - `persona-implementer` & `persona-module-tester` for code & unit tests
  - `persona-integration-tester` for multi-module/integration tests
  - `persona-security-expert` for untrusted text sanitization & output safety
  - `persona-documenter` for documentation & ADRs
  - `persona-code-reviewer` for finding evaluation & adversarial refutation
- [ ] 3. Incremental implementation following the 6-Phase technical recipe with local tests passing.
- [ ] 4. Final completion report written (`doc/agy/uc-pln-2-plan-report.md`) and initial plan moved to `doc/agy/implemented/uc-pln-2-plan.md`.
- [ ] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit` for code/config; skipped for standalone documentation changes).
- [ ] 6. PR opened (with `--label "skip-bot-review"` if authorized), 5-minute review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [ ] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.

---

## 4. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Trace** | `plan-marshall-persona-documenter` | `plan-marshall-ref-code-quality` | Verify decision envelope schema, part dependencies, quorum vote metrics, catalogue keys. |
| **Phase 2: `pm-api`** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-module-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:junit-core` | Immutable Java 25 records, Jackson streaming parsing, bilingual resource bundles (`decisions.{en,de}.properties`). |
| **Phase 3: Mock Runtime** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-integration-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:cui-cui-http-testing` | Stateful scenario JSON (`decisions.json`), dynamic route handlers, SSE broadcast (`decision.opened`, `decision.answered`, `decision.closed`). |
| **Phase 4: `pm-operator`** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-security-expert` | `pm-dev-java:java-core`<br>`pm-dev-java:java-security` | `QuestionsCommand`, `AnswerCommand`, output safety, JLine interactive prompts, quorum voting layout. |
| **Phase 5: `pm-web-app`** | `plan-marshall-persona-implementer`<br>`pm-dev-frontend:javascript` | `pm-dev-frontend:css`<br>`pm-dev-frontend:jest-testing` | Decision card/modal, option selection, own-words input, quorum vote badges, Vitest tests. |
| **Phase 6: Verification** | `plan-marshall-persona-integration-tester` | `pm-dev-java:junit-integration` | `PmOperatorProcessIT` process execution, Playwright E2E browser test suite (`decisions.spec.js`). |
| **Phase 7: Review Gate** | `plan-marshall-persona-code-reviewer` | `pm-dev-java:java-security` | Adversarial refutation of candidate bot findings, 100% resolution bar. |

---

## 5. Phased Task Breakdown

### Phase 1: Traceability & Design Alignment
- [ ] **1.1 Design Specification Audit**: Review `doc/design/shared/decisions.adoc` and `doc/plans/decision-contract.adoc`:
  - Resource routes: `GET /api/v1/decisions`, `GET /api/v1/decisions/{id}`, `POST /api/v1/decisions/{id}/answer`, `POST /api/v1/decisions/{id}/interpret`, `POST /api/v1/decisions/{id}/withdraw`.
  - SSE events: `GET /api/v1/decisions/events` with events `decision.opened`, `decision.changed`, `decision.answered`, `decision.closed`.
  - Closed enumerations: `DecisionScope` (`PLAN`, `EPIC`, `PROJECT_SETUP`, `MACHINE`), `Reversibility` (`REVERSIBLE`, `COSTLY`, `IRREVERSIBLE`), `AnswerType` (`ONE_OPTION`, `SEVERAL_OPTIONS`, `VALUE`), `InterpretationType` (`OPTION`, `OPTION_WITH_CONDITION`, `OWN_OPTION`, `DELEGATION`, `UNCLEAR`), `QuorumBand` (`UNANIMOUS`, `CLEAR_MAJORITY`, `SPLIT`, `NO_CONSENSUS`).
  - Waiver request support: `waive` or `decline` actions.
  - Extract the complete set of keys needed for `messages/decisions.{en,de}.properties`.

### Phase 2: `pm-api` Contracts & Message Catalogue
- [ ] **2.1 Decision Data Records**: Implement immutable Java 25 records in `de.planmarshall.api.decision`:
  - `DecisionId`: Scope coordinate and question identifier (`scopeId/questionId`).
  - `DecisionEnvelope`: `id`, `revision`, `title`, `header`, `subject`, `scope`, `askingPhase`, `askingRole`, `blocks`, `reversibility`, `description`, `evidence`, `parts`, `deliveredAt`.
  - `DecisionPart`: `partId`, `question`, `answerType`, `options`, `recommendation`, `recommendationRationale`, `recommendingRole`, `dependency`, `allowOwnWords`, `allowNote`.
  - `DecisionOption`: `key`, `label`, `tradeoffDescription`, `consequences`, `previewDiff`, `quorumSupport`.
  - `QuorumSupport`: `votesCount`, `totalVotes`, `modelsCount`, `band`, `dissentSummary`, `votes`.
  - `QuorumVote`: `role`, `model`, `confidencePercent`.
  - `DecisionAnswerRequest`: `partAnswers` (map of partId to chosen options or values), `ownWords`, `note`, `channel`, `device`.
  - `DecisionInterpretation`: `interpretationType`, `mappedOptionKey`, `condition`, `readBackText`.
- [ ] **2.2 Decision Events**: Implement SSE event payload records:
  - `DecisionOpenedEvent`, `DecisionChangedEvent`, `DecisionAnsweredEvent`, `DecisionClosedEvent`.
- [ ] **2.3 Streaming Codecs**: Implement Jackson streaming codecs (`JsonParser` / `JsonGenerator`) via `JsonTree`.
- [ ] **2.4 Message Catalogue Entries**:
  - Populate `pm-api/src/main/resources/messages/decisions.en.properties` with keys from `decisions.adoc#texts`.
  - Populate `pm-api/src/main/resources/messages/decisions.de.properties` with German translations.
  - Register `"decisions"` in `MessageCatalogue.SUPPORTED_AREAS`.
- [ ] **2.5 Unit Tests**:
  - `DecisionRecordsTest.java`: Round-trip serialization and deserialization of decision envelopes, quorum support, and answer payloads.
  - `DecisionCatalogueTest.java`: Key presence and ICU parameter validation across English and German bundles.

### Phase 3: `pm-mock-runtime` Scenarios & Handlers
- [ ] **3.1 Scenario Definition**: Create `pm-mock-runtime/src/main/resources/scenarios/decisions.json`:
  - Open decisions across scopes (active plan question, setup question, waiver request).
  - Quorum vote fixtures with unanimous, clear majority, and split consensus bands.
  - Evidence fixtures (diffs, file lines, finding links).
  - Waiver request fixture with decline/waive flows.
- [ ] **3.2 Dynamic Route Handlers**: Extend `MockRuntimeEngine.java`:
  - `GET /api/v1/decisions`: Returns open decisions list sorted newest first.
  - `GET /api/v1/decisions/{id}`: Returns full decision envelope.
  - `POST /api/v1/decisions/{id}/answer`: Records operator answer and broadcasts `decision.answered` / `decision.closed`.
  - `POST /api/v1/decisions/{id}/interpret`: Simulates role interpretation of own words.
  - `POST /api/v1/decisions/{id}/withdraw`: Closes decision as withdrawn ("none fits").
  - `GET /api/v1/decisions/events`: SSE stream for decisions.
- [ ] **3.3 Scenario Registration**:
  - Add `DECISIONS = "decisions"` constant in `StandardScenarios.java` and register in `ALL`.
- [ ] **3.4 Scenario Tests**:
  - `DecisionsScenarioTest.java` asserting scenario loading, REST responses, answering flows, and SSE event emissions.

### Phase 4: `pm-operator` Terminal Client Implementation
- [ ] **4.1 Questions & Answer Commands Refactoring**:
  - Enhance `QuestionsCommand.java` to list open decisions with subject, blocking status, and reversibility.
  - Enhance `AnswerCommand.java` supporting interactive JLine prompts:
    - Renders what is asked, why now, and evidence.
    - Options numbered with recommendation markings (`← recommended`) and quorum counts (`2 of 3 votes · 2 models`).
    - Dissent summary display.
    - Support for choosing options, typing own words, or selecting `none fits` / `later`.
    - Support for waiver requests (`waive` / `decline`).
- [ ] **4.2 Output Safety**:
  - Sanitize all decision titles, headers, descriptions, and option texts using `OutputSafety.stripControlSequences`.
- [ ] **4.3 CLI Unit & Transcript Tests**:
  - `DecisionsCommandTest.java`: Validate questions table layout, interactive answering, waiver handling, and exit codes.

### Phase 5: `pm-web-app` Client Implementation
- [ ] **5.1 API Client Extensions**:
  - Add `getDecisions()`, `getDecision(id)`, `answerDecision(id, answer)`, `interpretDecision(id, text)`, and `withdrawDecision(id)` to `pm-web-app/src/lib/api/client.ts`.
  - Extend SSE event listener to handle `decision.opened`, `decision.answered`, and `decision.closed`.
- [ ] **5.2 Decision View / Modal (`DecisionView.svelte`)**:
  - Display open decision cards with title, subject path, asking phase/role, and reversibility badge.
  - Evidence disclosure toggle (showing diffs or finding details).
  - Parts renderer: options list, recommended option styling, quorum support badges, dissent quote.
  - Input forms: option select, own-words textarea with read-back confirmation.
  - Waiver request view with explicit `Waive` and `Decline` action buttons.
- [ ] **5.3 Navigation Frame Integration**:
  - Header attention badge linking to open decisions.
  - Announcement banner when a new decision opens while app is active.
- [ ] **5.4 Responsive Styling**:
  - Mobile card layout (320px+) with accessible touch targets and responsive forms.
- [ ] **5.5 Vitest Tests**:
  - `DecisionView.test.ts` verifying rendering, option selection, own-words flow, and waiver actions.

### Phase 6: Integration Testing & Verification
- [ ] **6.1 Playwright E2E Suite**: Create `pm-web-e2e/tests/decisions.spec.js`:
  - Test viewing open decisions list.
  - Test answering a multi-option decision with recommendation.
  - Test answering a decision using own words with read-back.
  - Test resolving a waiver request.
  - Test cross-client update when decision is answered elsewhere via SSE.
- [ ] **6.2 Operator Process IT**:
  - Add process test in `PmOperatorProcessIT.java` executing `questions` and `answer` against running `pm-mock-runtime`.
- [ ] **6.3 Domain Invariants Verification**:
  - Verify against checklist in Section 1 of `doc/meta/implement_use-cases.md`.

### Phase 7: Completion Report, Pre-Commit, Review Gate & Landing (Runbook Discipline)
- [ ] **7.1 Final Completion Report**:
  - Generate comprehensive completion report in `doc/agy/uc-pln-2-plan-report.md`.
- [ ] **7.2 Plan Archival**:
  - Move `doc/agy/uc-pln-2-plan.md` to `doc/agy/implemented/uc-pln-2-plan.md` and record entry in `doc/agy/implemented/README.md`.
- [ ] **7.3 Pre-Commit Gate**:
  - Run `./mvnw verify -Ppre-commit` to validate OpenRewrite formatting, Checkstyle, and full test suite.
- [ ] **7.4 Commit & PR**:
  - Commit changes with Conventional Commits message (`feat(decisions): implement UC-PLN-2 decide what a plan asks`).
  - Open PR via `gh pr create` (with `--label "skip-bot-review"` if authorized).
- [ ] **7.5 5-Minute Review Window**:
  - Schedule 300s timer (`schedule` tool) and await CI, SonarCloud, Sourcery, and CodeRabbit.
- [ ] **7.6 100% Findings Resolution**:
  - Evaluate all automated findings with `persona-code-reviewer` adversarial refutation and resolve 100% of issues.
- [ ] **7.7 Merge Queue & Cleanup**:
  - Enqueue PR via `gh pr merge --auto --squash`.
  - Upon merge, synchronize `main`, remove worktree and delete topic branch.
- [ ] **7.8 Roadmap Archival**:
  - Update `doc/plans/README.adoc` and move completed package documentation (`decision-contract.adoc`, `cli-decisions.adoc`, `web-decisions.adoc`) to `doc/plans/implemented/`.
