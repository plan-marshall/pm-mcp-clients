# Implementation Plan: `integration-layers` (Integration-Test Layers in `pm-relay` and `pm-operator`)

This implementation plan governs the implementation of the `integration-layers` work package (`doc/plans/integration-layers.adoc`, Roadmap Milestone 2) in `pm-relay` and `pm-operator`.

It operates strictly under the rules and lifecycle defined in [`doc/agy/runbook.md`](runbook.md).

---

## 1. Scope & Objectives

The goal is to formalize and complete the process-level integration test layers (`*IT`) for `pm-relay` and `pm-operator` against the fake runtime / stub daemon, validating stdio hygiene, termination behavior, relay rules, operator security checks, and runtime-free verbs.

### Target Work Package
- Specification: `doc/plans/integration-layers.adoc` (Roadmap Milestone 2).
- Requirements: `PM-ARCH-3`, `PM-SEC-3`, `PM-SEC-4`.
- Specifications: MCP Tools § Errors & Protocol Testing; Module Structure § Test Placement.
- Watch: Runtime Model; CLI & Security.
- Primary Modules: `pm-relay` (`de.planmarshall.relay.*`), `pm-operator` (`de.planmarshall.operator.*`).

### Core Requirements
1. **`pm-relay` Integration Layer (`PmMcpProcessIT.java`)**:
   - **`stdout` Hygiene**: Ensure strictly newline-framed JSON-RPC on standard output under all conditions: normal forwarding, stream notifications, stub daemon errors, unknown methods, and abrupt daemon socket close. Zero non-JSON or debug text ever touches `stdout`.
   - **Relay Termination**: Clean termination upon `stdin` EOF (within time budget), upon host process termination / signal, and orderly exit when daemon socket disconnects.
   - **Relay Rules at Process Level**: Verify protocol compliance against the stub daemon: initialize handshake, method dispatch, notifications framing, and exit code invariants (including worker environment refusal `PM_MCP_JOB_ID`).
2. **`pm-operator` Integration Layer (`PmOperatorProcessIT.java`)**:
   - **Operator Checks at Process Level (`PM-SEC-3`)**: Verify `OperatorGate` at the real process level: interactive terminal requirement across all operator command verbs, and explicit refusal (exit code 4) when coding-agent host signals (`ANTIGRAVITY_AGENT`, `CLAUDE_CODE`, `OPENCODE_CLIENT`, `CODEX_*`) are injected in the environment.
   - **Runtime-Free Verbs at Process Level**: Validate commands that operate without a running daemon (`--help`, `--version`, `status` reporting "runtime not running" with exit code 0 and zero state footprint under `PM_MCP_BASE`).
   - **File Access Security (`PM-SEC-4`)**: Verify process-level enforcement of secure file permissions (0600 token permissions, 0700 locks directory).
3. **Execution Profiles**:
   - Ensure clean execution under `./mvnw verify -Pintegration-tests` (and `./mvnw verify -Pnative,integration-tests`).

---

## 2. Concurrency & Non-Overlapping Verification

This plan executes concurrently in Wave 6 alongside:
- `doc/agy/api-contract-plan.md` (Modules: `pm-api:de.planmarshall.api.resource.*`)
- `doc/agy/installation-layout-plan.md` (Modules: `pm-api:de.planmarshall.api.layout.*`, `pm-operator:layout/**`)

### Module Disjointness Matrix
| Module / Path | `integration-layers` (This Plan) | `api-contract` | `installation-layout` | Conflict Risk |
|---|---|---|---|---|
| `pm-relay/**` | **Sole Owner** | None | None | **Zero** |
| `pm-operator/src/test/**/PmOperatorProcessIT.java` | **Sole Owner** | None | None | **Zero** |
| `pm-api:de.planmarshall.api.resource.*` | None | **Sole Owner** | None | **Zero** |
| `pm-api:de.planmarshall.api.layout.*` | None | None | **Sole Owner** | **Zero** |
| `pm-operator/src/main/**/layout/**` | None | None | **Sole Owner** | **Zero** |
| `pm-mock-runtime/**` | None | None | None | **Zero** |
| `pm-web-app/**` | None | None | None | **Zero** |

---

## 3. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [ ] 1. Isolated worktree created on topic branch off latest `main` (`git worktree add -b feat/integration-layers /Users/oliver/git/worktrees/pm-mcp-clients-integration-layers main`).
- [ ] 2. Domain skills & engineering personas loaded for each task type:
  - `persona-implementer` & `persona-module-tester` for process lifecycle, stdio pipes, and integration testing
  - `persona-security-expert` for `PM-SEC-3` coding-agent host signal boundaries and `PM-SEC-4` file permission invariants
  - `persona-documenter` for documentation, ADRs, and roadmap updates
  - `persona-code-reviewer` for finding evaluation & adversarial refutation
- [ ] 3. Incremental implementation following the 5-Phase technical recipe with local tests passing.
- [ ] 4. Final completion report written (`doc/agy/integration-layers-plan-report.md`), initial plan moved to `doc/agy/implemented/integration-layers-plan.md`, and package marked done in `doc/plans/README.adoc`. (Do NOT move report to `verified/` — post-landing verification moves it).
- [ ] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit`).
- [ ] 6. PR opened (with `--label "skip-bot-review"` if authorized), 5-minute review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [ ] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [ ] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/integration-layers-plan-report.md` by orchestrator session.

---

## 4. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Trace & Audit** | `plan-marshall-persona-documenter` | `plan-marshall-ref-code-quality` | Audit current `PmMcpProcessIT` and `PmOperatorProcessIT` against `doc/plans/integration-layers.adoc`. |
| **Phase 2: Relay Integration Suite** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-module-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:junit-integration` | Add comprehensive stdio hygiene, EOF termination, daemon abrupt disconnect, and error response cases in `pm-relay`. |
| **Phase 3: Operator Integration Suite** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-security-expert` | `pm-dev-java:java-security`<br>`pm-dev-java:junit-integration` | Add process-level operator checks (`PM-SEC-3` host signals) and runtime-free verbs in `pm-operator`. |
| **Phase 4: Multi-Platform & Performance** | `plan-marshall-persona-module-tester` | `pm-dev-java:junit-core` | Verify startup budgets (PM-TECH-3 50ms) and platform compatibility (macOS & Linux). |
| **Phase 5: Quality Gate & Review** | `plan-marshall-persona-code-reviewer` | `pm-dev-java:java-security` | Run full `./mvnw verify -Pintegration-tests`, verify zero failures, pre-commit validation. |

---

## 5. Phased Technical Recipe

### Phase 1: Trace Matrix & Existing Suite Audit
- Cross-reference existing `PmMcpProcessIT` and `PmOperatorProcessIT` against:
  - `PM-ARCH-3`: Process boundaries & stdio pipe discipline.
  - `PM-SEC-3`: Interactive terminal enforcement & coding-agent host signal detection (exit code 4).
  - `PM-SEC-4`: Secure file permissions on token and state directories.
  - Runtime Model § CLI & Security.

### Phase 2: `pm-relay` Process IT Hardening (`PmMcpProcessIT`)
- Add comprehensive test cases in `PmMcpProcessIT.java`:
  - **`stdout` Framing & Purity Under Faults**: When the daemon returns JSON-RPC error responses or HTTP 500, verify that output is strictly newline-delimited JSON-RPC error objects.
  - **Abrupt Socket Disconnect**: When daemon closes the Unix socket unexpectedly mid-stream, verify relay exits cleanly without hanging.
  - **Immediate EOF**: Stdin closed immediately without sending `initialize` -> clean exit 0 without stdout output.
  - **Malformed Stdin**: Garbage sent on stdin -> JSON-RPC parse error response sent on stdout, relay continues or exits according to spec.

### Phase 3: `pm-operator` Process IT Hardening (`PmOperatorProcessIT`)
- Add comprehensive test cases in `PmOperatorProcessIT.java`:
  - **Runtime-Free Verbs**:
    - `pm-operator --help`: exits 0, prints help, touches nothing in filesystem.
    - `pm-operator --version`: exits 0, prints version string (`pm-operator 0.1.0-SNAPSHOT`), touches nothing.
    - `pm-operator status` without daemon: exits 0, reports `runtime   not running\n`, zero files created in `PM_MCP_BASE`.
  - **Coding-Agent Host Detection (`PM-SEC-3`)**:
    - Test all host signal variables across verbs: `ANTIGRAVITY_AGENT`, `CLAUDE_CODE`, `OPENCODE_CLIENT`, `CODEX_*`.
    - Verify exit code is strictly 4 and stderr reports `operator refused: coding-agent environment detected (<host>)`.
  - **Interactive Terminal Gate**:
    - Verify non-interactive execution of operator verbs (`plan`, `epic`, `start`, `onboard`, `reports`, `enrol`, `unenrol`, `accept-edit`) exits 4 with message `operator refused: interactive terminal required`.

### Phase 4: Integration Verification
- Execute full integration test suite:
  ```bash
  ./mvnw verify -Pintegration-tests -pl pm-relay,pm-operator
  ```
- Confirm all tests pass on both packaged JARs and verify startup timing thresholds.

### Phase 5: Verification & Completion Report
- Move plan to `doc/agy/implemented/integration-layers-plan.md`.
- Mark `integration-layers` package `done` in `doc/plans/README.adoc`.
- Write completion report to `doc/agy/integration-layers-plan-report.md`.
- Ensure `./mvnw verify -Ppre-commit` passes cleanly.
