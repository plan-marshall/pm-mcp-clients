# Implementation Plan: `operator-enrolment-verbs` (Operator Enrolment Verbs & Store Integrity)

This implementation plan governs the implementation of the `operator-enrolment-verbs` work package (`doc/plans/operator-enrolment-verbs.adoc`, Roadmap Milestone 3) in `pm-operator`, `pm-api`, and `pm-mock-runtime`.

It operates strictly under the rules and lifecycle defined in [`doc/agy/runbook.md`](runbook.md).

---

## 1. Scope & Objectives

The goal is to implement the operator CLI verbs for project enrolment and store integrity verification:
- `pm-operator enrol` with `--pilot` flag.
- `pm-operator unenrol`.
- `pm-operator accept-edit`: displays the store file path, recorded size and SHA-256 side-by-side with current size and SHA-256 before prompting the operator for confirmation. It strictly never sends confirmation unless explicitly confirmed by the operator (PM-SEC-3 gate).

### Target Work Package
- Specification: `doc/plans/operator-enrolment-verbs.adoc` (Milestone 3).
- Requirements: `PM-EXEC-2`, `PM-SEC-3` (Terminal Confirmation Gate), `PM-IMPL-7`.
- Specifications: CLI & Security § Enrolment, Lifecycle Gate & Release; § Command Tree; Store Schemas § Store Digest Verification.
- Primary Modules: `pm-operator`, `pm-api` (`api.resource`), `pm-mock-runtime`.

### Core Requirements
1. **API Contracts (`pm-api:de.planmarshall.api.resource`)**:
   - `EnrolRequest.java` (project path, pilot flag) and `EnrolResult.java` (status, project ID, pilot mode).
   - `UnenrolRequest.java` (project ID) and `UnenrolResult.java` (status).
   - `StoreDigestComparison.java` (store path, recorded size, recorded sha256, current size, current sha256, mismatch details).
   - `AcceptEditRequest.java` (store path, confirmed sha256) and `AcceptEditResult.java` (status, new sha256).
   - JSON streaming codecs using `jackson-core`.
   - Localized message bundle `messages/enrolment.{en,de}.properties` registered in `MessageCatalogue.java`.
2. **Mock Runtime Scenario & Dynamic Endpoints (`pm-mock-runtime`)**:
   - Scenario `scenarios/enrolment.json` registered in `StandardScenarios.java`.
   - Dynamic endpoints:
     - `POST /api/v1/projects/{id}/enrol` (or `/api/v1/enrol`)
     - `POST /api/v1/projects/{id}/unenrol`
     - `GET /api/v1/projects/{id}/stores/digest`
     - `POST /api/v1/projects/{id}/stores/accept-edit`
   - Broadcast events: `project.enroled`, `project.unenroled`, `store.edit_accepted`.
3. **Operator CLI Verbs (`pm-operator`)**:
   - Subcommand `pm-operator enrol [--pilot] [path]`
   - Subcommand `pm-operator unenrol [project-id]`
   - Subcommand `pm-operator accept-edit <store-path> [--force]`
   - Side-by-side comparison table formatting in terminal:
     ```
     Store: .plan/marshal.json
     Metric      Recorded                                                          Current
     -----------------------------------------------------------------------------------------------------------------
     Size:       1,248 bytes                                                       1,512 bytes
     SHA-256:    e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855   2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824
     ```
   - Terminal confirmation prompt: `Confirm edit acceptance? [y/N]` (strictly defaults to No; never sends confirmation if refused).
4. **Testing Suite**:
   - Unit tests for records and codecs (`EnrolRecordsTest.java`, `EnrolCatalogueTest.java`).
   - Mock runtime scenario tests (`EnrolScenarioTest.java`).
   - CLI verb tests (`EnrolCommandTest.java`, `AcceptEditCommandTest.java`).
   - Integration tests in `PmOperatorProcessIT.java`.

---

## 2. Concurrency & Non-Overlapping Verification

This plan executes concurrently in Wave 5 alongside:
- `doc/agy/exec-read-denied-paths-plan.md` (Module: `pm-exec`)
- `doc/agy/profile-documents-plan.md` (Module: `pm-api:api.host`, `classpath:/profiles/`)

### Module Disjointness Matrix
| Module / Path | `operator-enrolment-verbs` (This Plan) | `exec-read-denied-paths` | `profile-documents` | Conflict Risk |
|---|---|---|---|---|
| `pm-operator/**` | **Sole Owner** | None | None | **Zero** |
| `pm-mock-runtime/**` | **Sole Owner** | None | None | **Zero** |
| `pm-api:api.resource` | **Sole Owner** | None | None | **Zero** |
| `pm-api:messages/enrolment.*`| **Sole Owner** | None | None | **Zero** |
| `pm-api:api.host` | None | None | **Sole Owner** | **Zero** |
| `pm-exec/**` | None | **Sole Owner** | None | **Zero** |
| `pm-web-app/**` | None | None | None | **Zero** |

---

## 3. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created on topic branch off latest `main` (`git worktree add -b feat/operator-enrolment-verbs /Users/oliver/git/worktrees/pm-mcp-clients-operator-enrolment-verbs main`).
- [ ] 2. Domain skills & engineering personas loaded for each task type:
  - `persona-implementer` & `persona-module-tester` for commands, models, and unit tests
  - `persona-security-expert` for PM-SEC-3 confirmation gate, store digest validation, and safe terminal display
  - `persona-integration-tester` for mock runtime routes and `PmOperatorProcessIT`
  - `persona-documenter` for documentation and package status updates
  - `persona-code-reviewer` for finding evaluation & adversarial refutation
- [ ] 3. Incremental implementation following the 6-Phase technical recipe with local tests passing.
- [ ] 4. Final completion report written (`doc/agy/operator-enrolment-verbs-plan-report.md`), initial plan moved to `doc/agy/implemented/operator-enrolment-verbs-plan.md`, and package marked done in `doc/plans/README.adoc`. (Do NOT move report to `verified/` — post-landing verification moves it).
- [ ] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit`).
- [ ] 6. PR opened (with `--label "skip-bot-review"` if authorized), 5-minute review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [ ] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [ ] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/operator-enrolment-verbs-plan-report.md` by orchestrator session.

---

## 4. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Trace & Contracts** | `plan-marshall-persona-documenter` | `plan-marshall-ref-code-quality` | Map `PM-EXEC-2`, `PM-SEC-3`, Store Digest Verification to API schemas and Picocli options. |
| **Phase 2: `pm-api` Contracts** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-module-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:junit-core` | Immutable Java 25 records in `de.planmarshall.api.resource`, Jackson-streaming codecs, bilingual properties. |
| **Phase 3: Mock Runtime** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-integration-tester` | `pm-dev-java:cui-cui-http-testing` | Scenario `scenarios/enrolment.json`, dynamic route handlers, SSE events. |
| **Phase 4: `pm-operator` Commands** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-security-expert` | `pm-dev-java:java-core`<br>`pm-dev-java:java-security` | Subcommands `enrol`, `unenrol`, `accept-edit`; side-by-side terminal table; PM-SEC-3 confirmation gate. |
| **Phase 5: Process & Integration Tests** | `plan-marshall-persona-integration-tester` | `pm-dev-java:junit-integration` | `PmOperatorProcessIT`, `EnrolCommandTest`, `AcceptEditCommandTest`. |
| **Phase 6: Quality Gate & Review** | `plan-marshall-persona-code-reviewer` | `pm-dev-java:java-security` | Full unit test verification, pre-commit validation, zero warnings. |

---

## 5. Phased Technical Recipe

### Phase 1: Trace Matrix & Specification Alignment
- Map `doc/plans/operator-enrolment-verbs.adoc` to:
  - `PM-EXEC-2`: Project enrolment and pilot execution.
  - `PM-SEC-3`: Terminal confirmation gate for security-sensitive state mutations.
  - Store Schemas § Store Digest Verification: recorded SHA-256 vs current file SHA-256.

### Phase 2: Contracts & Message Catalogue (`pm-api`)
- Create records in `de.planmarshall.api.resource`:
  - `EnrolRequest.java` and `EnrolResult.java`
  - `UnenrolRequest.java` and `UnenrolResult.java`
  - `StoreDigestComparison.java`
  - `AcceptEditRequest.java` and `AcceptEditResult.java`
- Codecs: Jackson-core streaming serialization and deserialization.
- Register `enrolment` in `MessageCatalogue.java`:
  - Add `resources/messages/enrolment.en.properties`
  - Add `resources/messages/enrolment.de.properties`

### Phase 3: Mock Runtime Simulation (`pm-mock-runtime`)
- Author `src/main/resources/scenarios/enrolment.json` with pre-seeded projects and stores.
- Register in `StandardScenarios.java`.
- Implement dynamic endpoints in `MockRuntimeEngine.java`:
  - Handle `POST /api/v1/enrol` (or `/api/v1/projects/{id}/enrol`)
  - Handle `POST /api/v1/projects/{id}/unenrol`
  - Handle `GET /api/v1/projects/{id}/stores/digest`
  - Handle `POST /api/v1/projects/{id}/stores/accept-edit`
- Unit tests in `EnrolScenarioTest.java`.

### Phase 4: Operator Verbs (`pm-operator`)
- Implement Picocli subcommands:
  - `EnrolCommand.java`: `--pilot` flag, project directory validation, daemon request.
  - `UnenrolCommand.java`: confirmation gate, daemon request.
  - `AcceptEditCommand.java`: fetches store digest comparison, renders side-by-side table, requests interactive operator confirmation via standard input, sends request only on `y`/`yes`.
- Register in `PmOperator.java` `subcommands = { ... }`.
- Unit tests in `EnrolCommandTest.java` and `AcceptEditCommandTest.java`.

### Phase 5: Verification & Integration Tests
- Verify refusal when confirmation is denied.
- Verify accept-edit execution when confirmed.
- Add test scenarios to `PmOperatorProcessIT.java`.

### Phase 6: Documentation & Plan Finalization
- Move `doc/plans/operator-enrolment-verbs.adoc` to `doc/plans/implemented/operator-enrolment-verbs.adoc`.
- Mark `operator-enrolment-verbs` as `done` in `doc/plans/README.adoc`.
- Write completion report `doc/agy/operator-enrolment-verbs-plan-report.md`.
- Move plan to `doc/agy/implemented/operator-enrolment-verbs-plan.md` and update `doc/agy/implemented/README.md`.

---

## 6. Verification Commands

```bash
# Compile and run unit tests in pm-operator and pm-api
./mvnw test -pl pm-operator,pm-api,pm-mock-runtime -Dtest=*Enrol*,*AcceptEdit*

# Run full pre-commit verification
./mvnw verify -Ppre-commit
```
