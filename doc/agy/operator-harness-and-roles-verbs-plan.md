# Implementation Plan: `operator-harness-and-roles-verbs` (Operator Verbs of Harness Enrolment and Roles)

This implementation plan governs the implementation of the `operator-harness-and-roles-verbs` work package (`doc/plans/operator-harness-and-roles-verbs.adoc`, Roadmap Milestone 4) in `pm-operator` and `pm-api`.

It operates strictly under the rules and lifecycle defined in [`doc/agy/runbook.md`](runbook.md).

---

## 1. Scope & Objectives

The goal is to deliver the operator CLI verbs for harness enrolment (`pm-operator enrol-harness <client>`) and role management (`pm-operator roles list|show|qualify`), backed by strict contract records in `pm-api` and guarded by `OperatorGate` (`PM-SEC-3`).

### Target Work Package
- Specification: `doc/plans/operator-harness-and-roles-verbs.adoc` (Roadmap Milestone 4).
- Requirements: `PM-WORK-1`, `PM-WORK-6`, `PM-SEC-3`.
- Specifications: CLI & Security § Credentials & Operator; Model Work § Roles & Configuration; CLI & Security § Command Tree.
- Watch: CLI & Security; Model Work.
- Primary Modules: `pm-operator` (`de.planmarshall.operator.*`), `pm-api` (`de.planmarshall.api.resource.*`).

### Core Requirements
1. **API Resource Contracts (`pm-api:de.planmarshall.api.resource`)**:
   - `HarnessEnrolmentResource.java`: Client name, executable path, version, binary digest, status.
   - `RoleSummaryResource.java`: Role identifier, display name, description, capabilities.
   - `RoleDetailResource.java`: Advisory tool set, narrowed effort, qualification criteria, configuration parameters.
   - Jackson streaming serialization & deserialization without `jackson-databind`.
2. **Operator Verbs (`pm-operator`)**:
   - `pm-operator enrol-harness <client>`:
     - Discovers client binary (`claude`, `opencode`, `codex`, `antigravity`).
     - Displays path, version, and SHA-256 digest side-by-side before operator confirmation (`--yes` bypass).
     - Refuses non-interactive or automated coding-agent hosts via `OperatorGate` (`PM-SEC-3`, exit code 4).
   - `pm-operator roles list`:
     - Lists all available roles for the current project.
   - `pm-operator roles show <role>`:
     - Displays full role details, advisory tool sets, and narrowed effort estimates.
   - `pm-operator roles qualify <role>`:
     - Evaluates role suitability against project profile.
3. **Security & Gate Invariants**:
   - Enforce `OperatorGate` on `enrol-harness` and `roles` verbs.
   - Strictly exit 4 outside an interactive terminal or when coding-agent host signals are detected.
4. **Testing**:
   - Unit tests for command parsing, formatting, and confirmation in `pm-operator`.
   - Process integration tests in `PmOperatorProcessIT` verifying process exit codes and stdio hygiene.

---

## 2. Concurrency & Non-Overlapping Verification

This plan executes concurrently in Wave 7 alongside:
- `doc/agy/cli-test-infra-plan.md` (Modules: `pm-operator:src/test/**/transcript/**`, `pm-mock-runtime:testsupport`)
- `doc/agy/operator-interaction-contract-plan.md` (Modules: `pm-api:de.planmarshall.api.interaction.*`, `pm-mock-runtime`)

### Module Disjointness Matrix
| Module / Path | `operator-harness-and-roles-verbs` (This Plan) | `cli-test-infra` | `operator-interaction-contract` | Conflict Risk |
|---|---|---|---|---|
| `pm-operator:de.planmarshall.operator.Harness*` | **Sole Owner** | None | None | **Zero** |
| `pm-operator:de.planmarshall.operator.Roles*` | **Sole Owner** | None | None | **Zero** |
| `pm-api:de.planmarshall.api.resource.Harness*` | **Sole Owner** | None | None | **Zero** |
| `pm-api:de.planmarshall.api.resource.Role*` | **Sole Owner** | None | None | **Zero** |
| `pm-operator:src/test/**/transcript/**` | None | **Sole Owner** | None | **Zero** |
| `pm-api:de.planmarshall.api.interaction.*` | None | None | **Sole Owner** | **Zero** |
| `pm-mock-runtime/**` | None | Read-only Test Fixture | **Sole Owner** | **Zero** |

---

## 3. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [ ] 1. Isolated worktree created on topic branch off latest `main` (`git worktree add -b feat/operator-harness-and-roles-verbs /Users/oliver/git/worktrees/pm-mcp-clients-operator-harness-and-roles-verbs main`).
- [ ] 2. Domain skills & engineering personas loaded for each task type:
  - `persona-implementer` & `persona-module-tester` for Picocli verbs, resource records, and Jackson streaming codecs
  - `persona-security-expert` for `PM-SEC-3` operator gate, binary digest computation, and confirmation boundaries
  - `persona-documenter` for documentation, ADRs, and roadmap updates
  - `persona-code-reviewer` for finding evaluation & adversarial refutation
- [ ] 3. Incremental implementation following the 5-Phase technical recipe with local tests passing.
- [ ] 4. Final completion report written (`doc/agy/operator-harness-and-roles-verbs-plan-report.md`), initial plan moved to `doc/agy/implemented/operator-harness-and-roles-verbs-plan.md`, and package marked done in `doc/plans/README.adoc`. (Do NOT move report to `verified/` — post-landing verification moves it).
- [ ] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit`).
- [ ] 6. PR opened (NEVER use `skip-bot-review`), 5-minute review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [ ] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [ ] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/operator-harness-and-roles-verbs-plan-report.md` by orchestrator session.

---

## 4. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Trace & Schema** | `plan-marshall-persona-documenter` | `plan-marshall-ref-code-quality` | Map `PM-WORK-1`, `PM-WORK-6`, `PM-SEC-3` to command definitions and API schemas. |
| **Phase 2: API Resource Contracts** | `plan-marshall-persona-implementer` | `pm-dev-java:java-core` | Implement immutable records in `pm-api` with streaming Jackson codecs without databind. |
| **Phase 3: Picocli Commands** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-security-expert` | `pm-dev-java:java-core`<br>`pm-dev-java:java-security` | Implement `EnrolHarnessCommand` and `RolesCommand` (`list`, `show`, `qualify`) with `OperatorGate`. |
| **Phase 4: Unit & Process ITs** | `plan-marshall-persona-module-tester` | `pm-dev-java:junit-core`<br>`pm-dev-java:junit-integration` | Add command execution tests, digest calculation tests, and `PmOperatorProcessIT` gate verification. |
| **Phase 5: Quality Gate & Review** | `plan-marshall-persona-code-reviewer` | `pm-dev-java:java-security` | Pre-commit validation, verify zero warnings, review bot finding resolution. |

---

## 5. Phased Technical Recipe

### Phase 1: Trace Matrix & Specification Alignment
- Map `doc/plans/operator-harness-and-roles-verbs.adoc` to:
  - `PM-WORK-1`: Roles list/show/qualify, advisory tool set, narrowed effort.
  - `PM-WORK-6`: Harness binary path, version, and digest display prior to enrolment.
  - `PM-SEC-3`: `OperatorGate` enforcement (exit 4 for non-interactive or coding agent hosts).

### Phase 2: `pm-api` Resource Contracts
- Implement `HarnessEnrolmentResource.java`:
  - `client`: client name (`claude`, `opencode`, `codex`, `antigravity`).
  - `executablePath`: normalized absolute path.
  - `version`: detected version string.
  - `sha256Digest`: hexadecimal binary digest.
- Implement `RoleSummaryResource.java` and `RoleDetailResource.java`:
  - `role`: role identifier.
  - `advisoryToolSet`: list of recommended MCP tools.
  - `narrowedEffort`: effort category and description.
  - `qualification`: validation status and diagnostics.
- Jackson-core streaming serialization (`writeTo`) and deserialization (`readFrom`).

### Phase 3: `pm-operator` Commands
- Implement `EnrolHarnessCommand`:
  - Subcommand `enrol-harness <client>`.
  - Discovers binary location, computes digest, prompts confirmation (bypassed with `--yes`).
  - Sends enrolment request to runtime daemon.
- Implement `RolesCommand`:
  - Subcommands: `list`, `show <role>`, `qualify <role>`.
  - Formats advisory tool sets and narrowed effort clearly in terminal output.
  - Annotated with `@OperatorVerb` to enforce `OperatorGate`.

### Phase 4: Unit & Process Integration Tests
- Unit tests:
  - `EnrolHarnessCommandTest`: confirmation flow, digest display, rejection on invalid clients.
  - `RolesCommandTest`: list formatting, show detail formatting, qualify output.
  - `HarnessResourceTest`: Jackson streaming round-trip tests and fail-closed validation.
- Integration tests in `PmOperatorProcessIT`:
  - `enrol-harness` and `roles` refuse non-interactive execution (exit code 4).
  - Coding-agent host signals refused with host diagnostic message.

### Phase 5: Verification & Report
- Run `./mvnw verify -Ppre-commit`.
- Archive plan to `doc/agy/implemented/operator-harness-and-roles-verbs-plan.md`.
- Mark package `done` in `doc/plans/README.adoc`.
- Author completion report at `doc/agy/operator-harness-and-roles-verbs-plan-report.md`.
