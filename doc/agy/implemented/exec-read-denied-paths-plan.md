# Implementation Plan: `exec-read-denied-paths` (`pm-exec` Confinement Read-Denied Paths)

This implementation plan governs the implementation of the `exec-read-denied-paths` work package (`doc/plans/exec-read-denied-paths.adoc`, Roadmap Milestone 4) in `pm-exec`.

It operates strictly under the rules and lifecycle defined in [`doc/agy/runbook.md`](runbook.md).

---

## 1. Scope & Objectives

The goal is to extend `pm-exec` so it can take multiple read-denied paths beside `<PM_MCP_BASE>` (specifically the session bus socket and container engine sockets without opt-in) and enumerate the Landlock read set around all of them.

### Target Work Package
- Specification: `doc/plans/exec-read-denied-paths.adoc` (Milestone 4).
- Requirements: `PM-SEC-5` (Confinement & Isolation), `PM-IMPL-7`.
- Primary Module: `pm-exec`.

### Core Requirements
1. **Multiple Denied Paths**: `pm-exec` accepts multiple `--deny-read <path>` arguments on its CLI (or a list of denied paths alongside `<PM_MCP_BASE>`), representing:
   - `<PM_MCP_BASE>` (plan-marshall base directory)
   - Session bus socket (e.g. `/run/user/<uid>/bus` or `$DBUS_SESSION_BUS_ADDRESS`)
   - Container engine sockets (e.g. `/var/run/docker.sock`, `/run/podman/podman.sock`, `/run/user/<uid>/podman/podman.sock`)
2. **Multi-Hole Read-Set Enumeration**: `ReadSet.java` enumerates the read set from the filesystem root down to all denied paths, carving out every directory containing or overlapping any of the denied paths while ensuring sibling entries remain readable.
3. **Landlock Confinement Application**: `LinuxConfinement.java` applies Landlock read rules for the multi-path read set.
4. **Hermetic Boundary & Module Invariants**: `pm-exec` maintains zero dependencies on product modules (`pm-api`, `pm-operator`, etc.) and zero external JSON libraries (only its built-in minimal JSON parser).

---

## 2. Concurrency & Non-Overlapping Verification

This plan executes concurrently in Wave 5 alongside:
- `doc/agy/profile-documents-plan.md` (Module: `pm-api:api.host`, `classpath:/profiles/`)
- `doc/agy/operator-enrolment-verbs-plan.md` (Modules: `pm-operator`, `pm-mock-runtime`, `pm-api:api.resource`)

### Module Disjointness Matrix
| Module / Path | `exec-read-denied-paths` (This Plan) | `profile-documents` | `operator-enrolment-verbs` | Conflict Risk |
|---|---|---|---|---|
| `pm-exec/**` | **Sole Owner** | None | None | **Zero** |
| `pm-api:api.host` | None | **Sole Owner** | None | **Zero** |
| `pm-api:api.resource` | None | None | **Sole Owner** | **Zero** |
| `pm-operator/**` | None | None | **Sole Owner** | **Zero** |
| `pm-mock-runtime/**` | None | None | **Sole Owner** | **Zero** |
| `pm-web-app/**` | None | None | None | **Zero** |

---

## 3. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created on topic branch off latest `main` (`git worktree add -b feat/exec-read-denied-paths /Users/oliver/git/worktrees/pm-mcp-clients-exec-read-denied-paths main`).
- [x] 2. Domain skills & engineering personas loaded for each task type:
  - `persona-implementer` & `persona-module-tester` for confinement algorithms and unit tests
  - `persona-security-expert` for Landlock filesystem security, socket isolation, and permission checks
  - `persona-integration-tester` for `PmExecIT` and Linux confinement integration tests
  - `persona-documenter` for documentation and package status updates
  - `persona-code-reviewer` for finding evaluation & adversarial refutation
- [x] 3. Incremental implementation following the 6-Phase technical recipe with local tests passing.
- [x] 4. Final completion report written (`doc/agy/exec-read-denied-paths-plan-report.md`), initial plan moved to `doc/agy/implemented/exec-read-denied-paths-plan.md`, and package marked done in `doc/plans/README.adoc`. (Do NOT move report to `verified/` — post-landing verification moves it).
- [x] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit`).
- [x] 6. PR opened (with `--label "skip-bot-review"` if authorized), 5-minute review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [x] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [x] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/exec-read-denied-paths-plan-report.md` by orchestrator session.

---

## 4. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Trace & Design** | `plan-marshall-persona-documenter` | `plan-marshall-ref-code-quality` | Map `PM-SEC-5`, Job Runtime § Session Bus & § Container Engine to read set algorithm requirements. |
| **Phase 2: Command & Argument Parsing** | `plan-marshall-persona-implementer` | `pm-dev-java:java-core` | Extend `Command.java` to support multiple `--deny-read <path>` flags, validation against root. |
| **Phase 3: Multi-Hole ReadSet Algorithm** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-module-tester` | `pm-dev-java:java-security`<br>`pm-dev-java:junit-core` | Extend `ReadSet.java` to enumerate read set around multiple denied paths; handle nested prefixes and symlinks. |
| **Phase 4: Confinement Wiring** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-security-expert` | `pm-dev-java:java-security` | Connect multi-denied paths through `Launcher.java`, `Confinement.java`, `LinuxConfinement.java`. |
| **Phase 5: Unit & Integration Verification** | `plan-marshall-persona-module-tester`<br>`plan-marshall-persona-integration-tester` | `pm-dev-java:junit-core` | `ReadSetTest`, `CommandTest`, `LauncherTest`, `LinuxConfinementTest`, `PmExecIT`. |
| **Phase 6: Quality Gate & Review** | `plan-marshall-persona-code-reviewer` | `pm-dev-java:java-security` | Pre-commit validation, zero compiler warnings, 100% test pass. |

---

## 5. Phased Technical Recipe

### Phase 1: Trace Matrix & Specification Alignment
- Map `doc/plans/exec-read-denied-paths.adoc` to:
  - `PM-SEC-5`: System socket confinement.
  - Job Runtime § Session Bus: socket path exclusion.
  - Job Runtime § Container Engine: socket path exclusion unless opt-in.
  - `PM-WATCH-JOB-35`: Multi-hole Landlock read set enumeration.

### Phase 2: Argument Parsing (`Command.java`)
- Update `Command.java`:
  - Change `Command.Launch` from `Optional<Path> deniedBase` to `List<Path> deniedPaths`.
  - Allow multiple `--deny-read <path>` arguments in the argument loop.
  - Provide backwards-compatible accessor `deniedBase()` returning the first denied path if present.
  - Unit tests in `CommandTest.java` verifying single, multiple, and zero `--deny-read` arguments.

### Phase 3: Multi-Path Read Set Algorithm (`ReadSet.java`)
- Update `ReadSet.java`:
  - `static List<Path> enumerate(Path root, List<Path> deniedPaths) throws UsageException`:
    - Canonicalize all denied paths.
    - Check that every denied path starts with and is strictly below `root`.
    - Traverse directories from `root` downward. At each directory level, include entries that do not overlap ANY denied path in `deniedPaths`.
    - If an entry contains or overlaps one or more denied paths, descend into it rather than adding it as a blanket read rule.
    - Ensure directories on the descent paths receive no rules (jobs cannot list them).
  - Unit tests in `ReadSetTest.java`:
    - Disjoint denied paths (e.g. `/run/user/1000/bus` and `/var/run/docker.sock`).
    - Nested denied paths (e.g. `/a/b` and `/a/b/c`).
    - Sibling paths remain readable while denied paths are excluded.
    - Edge cases: denied path does not exist, symlinks pointing inside/outside.

### Phase 4: Confinement Integration
- Update `Confinement.java` and `LinuxConfinement.java`:
  - Pass `List<Path> deniedPaths` to `ReadSet.enumerate`.
  - Ensure ABI and probe outputs report Landlock status unchanged.
- Update `Launcher.java`:
  - Wire parsed `deniedPaths` into confinement execution.

### Phase 5: Verification & Integration Tests
- Run unit test suites:
  - `CommandTest`
  - `ReadSetTest`
  - `LauncherTest`
  - `LinuxConfinementTest`
  - `NativeKernelTest`
- Run integration tests:
  - `PmExecIT` on Linux (or mock kernel on macOS): assert denied paths unreadable, sibling paths readable.

### Phase 6: Documentation & Plan Finalization
- Move `doc/plans/exec-read-denied-paths.adoc` to `doc/plans/implemented/exec-read-denied-paths.adoc`.
- Mark `exec-read-denied-paths` as `done` in `doc/plans/README.adoc`.
- Write completion report `doc/agy/exec-read-denied-paths-plan-report.md`.
- Move plan to `doc/agy/implemented/exec-read-denied-paths-plan.md` and update `doc/agy/implemented/README.md`.

---

## 6. Verification Commands

```bash
# Compile and run unit tests in pm-exec
./mvnw test -pl pm-exec

# Run full pre-commit verification
./mvnw verify -Ppre-commit
```
