# Implementation Plan: `installation-layout` (Installation Layout Seen from the Binaries)

This implementation plan governs the implementation of the `installation-layout` work package (`doc/plans/installation-layout.adoc`, Roadmap Milestone 2) in `pm-api` and `pm-operator`.

It operates strictly under the rules and lifecycle defined in [`doc/agy/runbook.md`](runbook.md).

---

## 1. Scope & Objectives

The goal is to implement and verify the installation layout seen from the client binaries:
- The user symlinks of `pm-mcp` and `pm-operator` pointing to the active versioned directory.
- Resolution of `PM_MCP_HOME` through switched symlinks.
- Sibling binary resolution by absolute path across version directories.
- Staged testing of symlink switching across versioned releases.

### Target Work Package
- Specification: `doc/plans/installation-layout.adoc` (Roadmap Milestone 2).
- Requirements: `PM-ARCH-1`, `PM-ARCH-7`.
- Specifications: CLI & Security § Installation Layout; Filesystem Layout § Machine & Runtime.
- Primary Modules: `pm-api` (`de.planmarshall.api.layout`), `pm-operator` (`layout`).

### Core Requirements
1. **Layout Contract & Resolution (`InstallationLayout.java`)**:
   - Locates `PM_MCP_HOME` from the real path of the running executable (or fallback to environment variable `PM_MCP_HOME`).
   - Identifies the versioned installation directory: `${PM_MCP_HOME}/versions/<version>/`.
   - Locates sibling binaries (`pm-mcp` locates `pm-operator`, and vice-versa) by absolute path within the same version directory, regardless of whether invoked via a symlink.
2. **Symlink Switch Integrity**:
   - When user symlinks at `${PM_MCP_HOME}/bin/pm-mcp` and `${PM_MCP_HOME}/bin/pm-operator` are updated to point to a new release (`v0.2.0` replacing `v0.1.0`), subsequent invocations resolve `PM_MCP_HOME` and version correctly through the newly switched symlinks.
3. **Staging Integration Test (`SwitchedSymlinkLayoutIT.java`)**:
   - Stages two distinct version directories (`versions/0.1.0` and `versions/0.2.0`).
   - Populates mock binary launchers for `pm-mcp` and `pm-operator`.
   - Creates symlinks in `bin/` and verifies resolution.
   - Switches symlinks to `0.2.0` and proves both binaries resolve `PM_MCP_HOME` and find their siblings by absolute path in the new version.

---

## 2. Concurrency & Non-Overlapping Verification

This plan executes concurrently in Wave 6 alongside:
- `doc/agy/api-contract-plan.md` (Module: `pm-api:de.planmarshall.api.resource`)
- `doc/agy/integration-layers-plan.md` (Modules: `pm-relay`, `pm-operator:it`)

### Module Disjointness Matrix
| Module / Path | `installation-layout` (This Plan) | `api-contract` | `integration-layers` | Conflict Risk |
|---|---|---|---|---|
| `pm-api:de.planmarshall.api.layout.*` | **Sole Owner** | None | None | **Zero** |
| `pm-operator:layout/**` | **Sole Owner** | None | None | **Zero** |
| `pm-api:de.planmarshall.api.resource.*` | None | **Sole Owner** | None | **Zero** |
| `pm-relay/**` | None | None | **Sole Owner** | **Zero** |
| `pm-operator:it/**` | None | None | **Sole Owner** | **Zero** |
| `pm-mock-runtime/**` | None | None | None | **Zero** |
| `pm-web-app/**` | None | None | None | **Zero** |

---

## 3. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created on topic branch off latest `main` (`git worktree add -b feat/installation-layout /Users/oliver/git/worktrees/pm-mcp-clients-installation-layout main`).
- [x] 2. Domain skills & engineering personas loaded for each task type:
  - `persona-implementer` & `persona-module-tester` for path resolution, symlink traversal, and unit tests
  - `persona-security-expert` for symlink path traversal safety, permission checks, and absolute path confinement
  - `persona-integration-tester` for multi-version staging tests and symlink switch verification
  - `persona-documenter` for documentation and package status updates
  - `persona-code-reviewer` for finding evaluation & adversarial refutation
- [x] 3. Incremental implementation following the 6-Phase technical recipe with local tests passing.
- [x] 4. Final completion report written (`doc/agy/installation-layout-plan-report.md`), initial plan moved to `doc/agy/implemented/installation-layout-plan.md`, and package marked done in `doc/plans/README.adoc`. (Do NOT move report to `verified/` — post-landing verification moves it).
- [x] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit`).
- [ ] 6. PR opened (with `--label "skip-bot-review"` if authorized), 5-minute review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [ ] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [ ] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/installation-layout-plan-report.md` by orchestrator session.

---

## 4. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Trace & Design** | `plan-marshall-persona-documenter` | `plan-marshall-ref-code-quality` | Map `PM-ARCH-1`, `PM-ARCH-7`, and CLI & Security § Installation Layout to resolution rules. |
| **Phase 2: Layout Resolver** | `plan-marshall-persona-implementer` | `pm-dev-java:java-core` | Implement `InstallationLayout.java` in `pm-api:de.planmarshall.api.layout`. |
| **Phase 3: Sibling Resolution** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-security-expert` | `pm-dev-java:java-security` | Secure absolute path resolution; real path canonicalization; prevent symlink escape. |
| **Phase 4: Unit Testing** | `plan-marshall-persona-module-tester` | `pm-dev-java:junit-core` | `InstallationLayoutTest`: test normal, symlinked, nested, and environment-override paths. |
| **Phase 5: Staging IT** | `plan-marshall-persona-integration-tester` | `pm-dev-java:junit-integration` | `SwitchedSymlinkLayoutIT`: stages two versioned dirs, tests symlink switch and sibling execution. |
| **Phase 6: Quality Gate & Review** | `plan-marshall-persona-code-reviewer` | `pm-dev-java:java-security` | Full unit test verification, pre-commit validation, zero warnings. |

---

## 5. Phased Technical Recipe

### Phase 1: Trace Matrix & Specification Alignment
- Map `doc/plans/installation-layout.adoc` to:
  - `PM-ARCH-1`: Directory layout and environment variables (`PM_MCP_HOME`, `PM_MCP_BASE`).
  - `PM-ARCH-7`: Upgrades and switched symlinks.
  - Sibling binary resolution invariants.

### Phase 2: Implementation of `InstallationLayout.java`
- Create package `de.planmarshall.api.layout` in `pm-api`:
  - `InstallationLayout.java`:
    - `resolveHome(Path executablePath)`: traverses real path to find enclosing `versions/<ver>/..` or falls back to `${PM_MCP_HOME}`.
    - `resolveVersion(Path executablePath)`: extracts version token from path.
    - `locateSibling(Path executablePath, String siblingName)`: finds sibling binary in the same version directory by absolute path.
    - Validates executable permissions and path boundaries.

### Phase 3: Unit Tests (`InstallationLayoutTest.java`)
- Author comprehensive unit tests in `pm-api`:
  - Direct execution from versioned directory.
  - Execution through single and multi-level symlinks.
  - Sibling binary discovery.
  - Fallback when executed outside standard layout (e.g. during Maven tests).

### Phase 4: Staged Symlink Switch Integration Test
- Author `SwitchedSymlinkLayoutIT.java` in `pm-operator`:
  - Stage temporary directory structure:
    ```
    test-home/
      bin/pm-mcp -> ../versions/0.1.0/bin/pm-mcp
      bin/pm-operator -> ../versions/0.1.0/bin/pm-operator
      versions/
        0.1.0/bin/{pm-mcp, pm-operator}
        0.2.0/bin/{pm-mcp, pm-operator}
    ```
  - Verify that invoking through `bin/pm-mcp` resolves version `0.1.0` and finds `0.1.0/bin/pm-operator`.
  - Atomically switch symlinks to point to `versions/0.2.0`.
  - Verify that subsequent invocations resolve `0.2.0` and find `0.2.0/bin/pm-operator`.

### Phase 5: Documentation & Archival
- Move `doc/plans/installation-layout.adoc` to `doc/plans/implemented/installation-layout.adoc`.
- Mark `installation-layout` as `done` in `doc/plans/README.adoc`.
- Write completion report `doc/agy/installation-layout-plan-report.md`.
- Move plan to `doc/agy/implemented/installation-layout-plan.md` and update `doc/agy/implemented/README.md`.

---

## 6. Verification Commands

```bash
# Compile and run unit tests in pm-api and pm-operator
./mvnw test -pl pm-api,pm-operator -Dtest=*InstallationLayout*,*SwitchedSymlink*

# Run full pre-commit verification
./mvnw verify -Ppre-commit
```
