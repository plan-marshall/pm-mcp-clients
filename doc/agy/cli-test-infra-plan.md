# Implementation Plan: `cli-test-infra` (Test Infrastructure of the Terminal Client)

This implementation plan governs the implementation of the `cli-test-infra` work package (`doc/plans/cli-test-infra.adoc`, epic clients) in `pm-operator` and `pm-mock-runtime`.

It operates strictly under the rules and lifecycle defined in [`doc/agy/runbook.md`](runbook.md).

---

## 1. Scope & Objectives

The goal is to deliver the transcript and dialogue test infrastructure for `pm-operator` against `pm-mock-runtime`: driving interactive terminal verbs with scripted inputs, asserting rendered output against recorded transcripts with readable diffs, and verifying JLine 4 terminal behavior in JVM and native modes.

### Target Work Package
- Specification: `doc/plans/cli-test-infra.adoc`.
- Requirements: `PM-TEST-7`.
- Specifications: Runtime Model § Mock Runtime; Terminal Client, Transcripts; CLI & Security § Operator Channel Isolation.
- Primary Modules: `pm-operator` (test scope), `pm-mock-runtime` (scenario execution in tests).

### Core Requirements
1. **Interactive Terminal Test Seam (`OperatorContext` / `OperatorGate`)**:
   - Establish a secure, test-only terminal harness that satisfies `OperatorGate`'s interactivity requirements in automated tests without opening security holes in production.
   - Scripted key-press and input driver feeding `LineReader` / JLine 4.
2. **Transcript Engine & Comparison**:
   - Transcript recording and verification engine comparing actual terminal render buffers with expected `.transcript` files.
   - Strict diff-on-mismatch: Any drift produces a readable unified diff rather than silent overwriting.
   - Normalization of ANSI escapes, timestamps, and terminal window sizing (80x24 standard).
3. **Initial Transcript Coverage**:
   - Deliver transcript tests for existing verbs against `pm-mock-runtime` scenarios:
     - `pm-operator status` dialogue against mock status scenario.
     - `pm-operator web pair` dialogue against mock pairing scenario.
4. **JVM and Native Verification**:
   - Transcript tests executable in both standard unit/integration test runs (`-Pintegration-tests`) and compatible with native image.

---

## 2. Concurrency & Non-Overlapping Verification

This plan executes concurrently in Wave 7 alongside:
- `doc/agy/operator-harness-and-roles-verbs-plan.md` (Modules: `pm-operator:src/main/**`, `pm-api:resource`)
- `doc/agy/operator-interaction-contract-plan.md` (Modules: `pm-api:interaction`, `pm-mock-runtime:engine`)

### Module Disjointness Matrix
| Module / Path | `cli-test-infra` (This Plan) | `operator-harness-and-roles-verbs` | `operator-interaction-contract` | Conflict Risk |
|---|---|---|---|---|
| `pm-operator:src/test/**/transcript/**` | **Sole Owner** | None | None | **Zero** |
| `pm-operator:src/main/**` | None | **Sole Owner** | None | **Zero** |
| `pm-api:de.planmarshall.api.resource.*` | None | **Sole Owner** | None | **Zero** |
| `pm-api:de.planmarshall.api.interaction.*` | None | None | **Sole Owner** | **Zero** |
| `pm-mock-runtime/**` | Test Scope Consumer | None | **Sole Owner** | **Zero** |

---

## 3. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created on topic branch off latest `main` (`git worktree add -b feat/cli-test-infra /Users/oliver/git/worktrees/pm-mcp-clients-cli-test-infra main`).
- [ ] 2. Domain skills & engineering personas loaded for each task type:
  - `persona-module-tester` & `persona-integration-tester` for terminal test harness, pseudo-terminal simulation, and transcript assertion
  - `persona-security-expert` for test seam containment ensuring no production security bypass
  - `persona-documenter` for transcript documentation, ADRs, and roadmap updates
  - `persona-code-reviewer` for finding evaluation & adversarial refutation
- [ ] 3. Incremental implementation following the 5-Phase technical recipe with local tests passing.
- [ ] 4. Final completion report written (`doc/agy/cli-test-infra-plan-report.md`), initial plan moved to `doc/agy/implemented/cli-test-infra-plan.md`, and package marked done in `doc/plans/README.adoc`. (Do NOT move report to `verified/` — post-landing verification moves it).
- [ ] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit`).
- [ ] 6. PR opened (NEVER use `skip-bot-review`), 5-minute review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [ ] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [ ] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/cli-test-infra-plan-report.md` by orchestrator session.

---

## 4. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Architecture & Seam** | `plan-marshall-persona-integration-tester` | `pm-dev-java:java-core` | Define the pseudo-terminal / test terminal seam in `pm-operator` test scope. |
| **Phase 2: Transcript Engine** | `plan-marshall-persona-module-tester` | `pm-dev-java:junit-core` | Implement transcript comparator with unified diff reporting and ANSI escape normalization. |
| **Phase 3: Dialogue Test Cases** | `plan-marshall-persona-integration-tester` | `pm-dev-java:cui-cui-testing` | Drive interactive dialogue tests for `status` and `web pair` against `pm-mock-runtime`. |
| **Phase 4: JLine 4 & Native Profile** | `plan-marshall-persona-implementer` | `pm-dev-java:java-core` | Verify terminal resize, Ctrl+C handling, and native-image classpath boundaries. |
| **Phase 5: Quality Gate & Review** | `plan-marshall-persona-code-reviewer` | `pm-dev-java:java-security` | Run full unit and integration tests, pre-commit validation, bot review resolution. |

---

## 5. Phased Technical Recipe

### Phase 1: Interactive Terminal Test Seam
- Create `TranscriptTerminal` and `TerminalTestDriver` in `pm-operator/src/test/java/de/planmarshall/operator/transcript/`:
  - Implements JLine `Terminal` interface backed by in-memory input/output streams.
  - Injects into `OperatorContext` via `TerminalProvider` functional interface.
  - Explicitly bounds test interactivity flag strictly to test execution.

### Phase 2: Transcript File Format & Comparator
- Implement `TranscriptAssert.java`:
  - Reads `.transcript` files containing expected user prompts and terminal renders.
  - Strips transient attributes (PIDs, exact timestamps) using regex normalizers.
  - Emits clear unified diff when actual terminal output diverges from expected transcript.

### Phase 3: Dialogue Tests Against Mock Runtime
- Create `StatusDialogueTranscriptTest.java`:
  - Starts `MockRuntime` with standard status scenario.
  - Executes `pm-operator status` through test driver.
  - Asserts rendered screen against `status.transcript`.
- Create `WebPairDialogueTranscriptTest.java`:
  - Drives pairing code display and confirmation prompt.
  - Asserts output against `web-pair.transcript`.

### Phase 4: Integration Verification
- Verify integration tests pass under `-Pintegration-tests`:
  ```bash
  ./mvnw verify -Pintegration-tests -pl pm-operator
  ```

### Phase 5: Verification & Report
- Run `./mvnw verify -Ppre-commit`.
- Archive plan to `doc/agy/implemented/cli-test-infra-plan.md`.
- Mark package `done` in `doc/plans/README.adoc`.
- Author completion report at `doc/agy/cli-test-infra-plan-report.md`.
