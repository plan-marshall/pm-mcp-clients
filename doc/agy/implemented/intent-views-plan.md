# Implementation Plan: `intent-views` (Intent, Outline, and Their Contracts)

This implementation plan governs the execution of the design and contract phase of the `intent-views` work package (`doc/plans/intent-views.adoc`, Milestones 8, 10, 11, 12) in `pm-api`, `pm-mock-runtime`, and companion design documentation.

It operates strictly under the rules and lifecycle defined in [`doc/agy/runbook.md`](runbook.md).

---

## 1. Scope & Objectives

The goal is to establish the design specifications, client-side resource contracts, and mock runtime scenarios for structured intent, outline records, builder checks, and `over_size` token distributions:

1. **Companion Shared Design (`doc/design/shared/intent-and-outline.adoc`)**:
   - Comprehensive design specification covering the plan intent record, outline record, epic intent, builder checks, and token distribution views across both clients.
   - Formal specification of EARS-form criteria, deliverable mappings, decision separation (operator vs. model calls), and self-review evaluation verdicts (`met`, `not_met`, `not_verified`).
2. **`pm-api` Intent & Outline Contract Records (`de.planmarshall.api.intent`)**:
   - `PlanIntentResource`: Goal, EARS criteria with deliverable mappings, non-goals, operator decisions, open questions, deferred items, and self-review evaluation states (`met`, `not_met`, `not_verified` with reason).
   - `OutlineResource`: Deliverables, affected files, intents, criteria satisfied, justified added dependencies, interfaces, and key link verification.
   - `BuilderCheckResource`: Open questions, operator recommendations, and resolution state.
   - `EpicIntentResource`: Destination, done-when checks (with evidence), non-goals, unspecified aspects, and per-plan done-when criteria.
   - `OverSizeDistributionResource`: Distribution of task tokens against predicted bounds and flagged task share when an `over_size` multiplier is adjusted.
3. **High-Performance Streaming Codecs (Zero Databind)**:
   - Memory-safe, allocation-efficient `jackson-core` streaming serializers and deserializers for all new records.
4. **Mock Runtime Scenarios & API Endpoints**:
   - Scenarios in `pm-mock-runtime`: `intent-plan.json`, `intent-epic.json`, `intent-outline.json`, `intent-oversize.json`.
   - Route handlers: `/api/v1/plans/{plan_id}/intent`, `/api/v1/plans/{plan_id}/outline`, `/api/v1/epics/{epic_id}/intent`, `/api/v1/projects/{project_id}/metrics/oversize`.
5. **Contract & Scenario Test Suites**:
   - 100% round-trip codec test coverage, negative parsing assertions, and mock runtime scenario validation.

### Target Work Package & References
- Specification: `doc/plans/intent-views.adoc`.
- Requirements: `PM-WF-10` (intent/outline/builder check), `PM-WF-13` (results per criterion), `PM-WF-15` (epic intent), `PM-WORK-7` (`over_size`).
- Specifications: Phase Workflows § The Intent Record, § Builder Check; Store Schemas § Outline Record; Model Work § Epic Intent.
- Design: `doc/design/shared/plan-view.adoc`, `epic-view.adoc`, `decisions.adoc`, `completion-report.adoc`.
- Primary Modules: `pm-api` (`de.planmarshall.api.intent.*`), `pm-mock-runtime`, `doc/design/shared/`.

---

## 2. Concurrency & Isolation

This plan executes concurrently in Wave 10 alongside:
- `cli-start` (`pm-operator:src/**`)
- `web-start` (`pm-web-app:src/**`)

### Boundary Rules
- `pm-api`: Sole owner of new package `de.planmarshall.api.intent` and its codecs. Zero modifications to sibling packages.
- `pm-mock-runtime`: Sole owner of `/intent`, `/outline` route handlers and `intent-*.json` scenario fixtures.
- `doc/design/shared/`: Sole owner of `intent-and-outline.adoc`.
- `pm-operator` and `pm-web-app`: No touch (owned by `cli-start` and `web-start`; client-side views for intent/outline will bind against these contracts in Wave 11).

---

## 3. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created at `/Users/oliver/git/worktrees/pm-mcp-clients-intent-views` on branch `feat/intent-views`.
- [x] 2. Domain skills & engineering personas loaded:
  - `persona-documenter` for companion design specification in `doc/design/shared/intent-and-outline.adoc`.
  - `persona-implementer` & `pm-dev-java:java-core` for Java 25 immutable records and Jackson streaming codecs.
  - `persona-module-tester` & `pm-dev-java:junit-core` for codec round-trip tests and scenario assertions.
  - `persona-code-reviewer` for review bot resolution.
- [x] 3. Incremental implementation following the 5-Phase technical recipe with local tests passing.
- [x] 4. Final completion report written (`doc/agy/intent-views-plan-report.md`), initial plan moved to `doc/agy/implemented/intent-views-plan.md`, and package marked done in `doc/plans/README.adoc`. (Do NOT move report to `verified/` — Step 8 is reserved for orchestrator).
- [x] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit`).
- [x] 6. PR opened (NEVER use `skip-bot-review`), review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [x] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [x] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/intent-views-plan-report.md` by orchestrator session.

---

## 4. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Design Documentation** | `plan-marshall-persona-documenter` | `plan-marshall-ref-asciidoc` | Author `doc/design/shared/intent-and-outline.adoc` defining records, criteria, and metrics. |
| **Phase 2: Intent & Outline Records** | `plan-marshall-persona-implementer` | `pm-dev-java:java-core` | Implement immutable records in `pm-api:de.planmarshall.api.intent`. |
| **Phase 3: Jackson Streaming Codecs** | `plan-marshall-persona-implementer` | `pm-dev-java:java-core` | Build `writeTo` and `readFrom` streaming codecs complying with zero databind rule. |
| **Phase 4: Mock Scenarios & Routes** | `plan-marshall-persona-integration-tester` | `pm-dev-java:junit-core` | Build `/api/v1/plans/{id}/intent`, `/outline` routes and `intent-*.json` scenarios. |
| **Phase 5: Quality Gate & Review** | `plan-marshall-persona-code-reviewer` | `pm-dev-java:java-security` | Run pre-commit checks, verify round-trip serialization, and resolve bot findings. |

---

## 5. Phased Technical Recipe

### Phase 1: Shared Design Specification
- Author `doc/design/shared/intent-and-outline.adoc` documenting:
  - Intent record structure (goal, criteria, non-goals, decisions, builder check).
  - Outline record structure (deliverables, files, criteria mapping, dependencies).
  - Epic intent and done-when verification.
  - `over_size` token distribution model.

### Phase 2: `pm-api` Contract Records
- Package: `de.planmarshall.api.intent`.
- Records:
  - `PlanIntentResource.java`, `IntentCriterion.java`, `CriterionStatus.java` (`met`, `not_met`, `not_verified`).
  - `OutlineResource.java`, `DeliverableRecord.java`, `DependencyJustification.java`.
  - `BuilderCheckResource.java`, `BuilderQuestion.java`.
  - `EpicIntentResource.java`, `DoneWhenCheck.java`.
  - `OverSizeDistributionResource.java`, `TokenBucket.java`.

### Phase 3: Jackson Streaming Codecs
- Implement streaming codecs in `IntentJson.java` using `JsonGenerator` and `JsonParser`.
- Add unit tests in `IntentContractTest.java` verifying complete serialization/deserialization fidelity.

### Phase 4: Mock Runtime Scenarios & Endpoints
- Author scenario files under `pm-mock-runtime/src/main/resources/scenarios/`:
  - `intent-plan.json` (plan intent with self-review results).
  - `intent-outline.json` (outline deliverables and affected files).
  - `intent-epic.json` (epic intent with done-when evidence).
  - `intent-oversize.json` (token distribution buckets).
- Register routes in `MockRuntimeEngine.java`.
- Validate with `IntentScenarioTest.java`.

### Phase 5: Verification & Review Resolution
- Run full pre-commit verification (`./mvnw verify -Ppre-commit`).
- Submit PR without `skip-bot-review` and resolve all bot review feedback.
