# Completion Report: `installation-layout` (Installation Layout Seen from the Binaries)

Companion report of the implementation plan
[`doc/agy/implemented/installation-layout-plan.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/implemented/installation-layout-plan.md),
executed under [`doc/agy/runbook.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/runbook.md) on branch
`feat/installation-layout`.

---

## 1. Executive Summary

The work package `installation-layout` (`doc/plans/implemented/installation-layout.adoc`, Roadmap Milestone
2) implements and verifies the installation layout as the client binaries see it: the versioned installation
directory that is `<PM_MCP_HOME>`, the two `PATH` symbolic links that are switched at an upgrade, and the
sibling binaries that a binary starts by absolute path in its own version directory.

- **Layout resolver (`pm-api`, `de.planmarshall.api.layout`)**: one resolver, `InstallationLayout`, replaces
  the former `PmMcpHome`. It resolves `<PM_MCP_HOME>` from the `PM_MCP_HOME` environment variable when set and
  otherwise from the real path of the running executable's `bin/` parent (every symbolic link resolved, so a
  binary reached through a `PATH` link - or a chain of two - finds its own installation); derives the version
  token and the installation root that holds the versioned directories side by side; and locates every sibling
  binary by its absolute path in the same `bin/`.
- **Runtime discovery through the switched link (`pm-operator`)**: `OperatorLayout` wraps the resolver as the
  operator sees it (`runtimeExecutable()`, `relayExecutable()`, `version()`, `runtimeUpgradable()`), and
  `OperatorContext` supplies the resolved `pm-mcpd` to the on-demand start. After the links are switched to a
  new release, both binaries resolve and start the new version's `pm-mcpd`. The runtime owns the upgrade
  refusal (it knows the two `PATH` links); the operator's layout exposes `runtimeUpgradable()` for it.
- **Refusals (`PM-SEC-4`)**: a layout that cannot be resolved (unknown executable, an executable that is not
  one of the four release binaries, an executable outside a `bin/` directory, a dangling link) and an
  installation directory, its `bin/` directory, or its root writable by group or others are refused.
- **Tests**: `InstallationLayoutTest` (17 unit tests in `pm-api`), the staged multi-version
  `SwitchedSymlinkLayoutIT` (5 integration tests in `pm-operator`) that runs two mock binaries, switches the
  links atomically, and proves that a same-named executable earlier on `PATH` is never started.

The layout shape follows the specification's *Installation root* bullet: `<install_root>/<version>/` is the
`<PM_MCP_HOME>`, and the two `PATH` links point into its `bin/`. Because the resolver derives the home as the
parent of the `bin/` directory that holds the running executable, it also resolves the plan's
`${PM_MCP_HOME}/versions/<version>/` nesting; a unit test pins that shape as well.

---

## 2. Delivered Artifacts

### 2.1 Production code

| Module | File | Purpose |
|---|---|---|
| `pm-api` | `src/main/java/de/planmarshall/api/layout/InstallationLayout.java` | The installation layout: `resolve`/`current` (environment variable or real path of the executable's `bin/` parent; the running executable must be one of the four release binaries), `resolveHome`/`resolveVersion` (the two steps), `home()`/`installRoot()`/`version()`/`resolvedFromBinary()`, `binary(name)`/`locateSibling(name)` (siblings by absolute path), and `runtimeUpgradable()`/`upgradabilityRefusal()`. Refuses an unresolvable layout and one whose installation directory, `bin/` directory, or root is group-/other-writable, with an `InstallationLayoutException`. Names the four binaries (`pm-mcp`, `pm-operator`, `pm-mcpd`, `pm-exec`). |
| `pm-api` | `src/main/java/de/planmarshall/api/layout/InstallationLayoutException.java` | The refusal of a layout that cannot be resolved or is insecure. |
| `pm-api` | `src/main/java/de/planmarshall/api/layout/package-info.java` | Package documentation and the specification reference. |
| `pm-api` | `src/main/java/de/planmarshall/api/runtime/RuntimeAccess.java` | The on-demand start now resolves `pm-mcpd` through `InstallationLayout` (the switched link); a second overload lets the operator pass its own executable source. |
| `pm-api` | `src/main/java/de/planmarshall/api/runtime/package-info.java` | `PM_MCP_HOME` resolution moved to the `layout` package. |
| `pm-api` | `src/main/java/de/planmarshall/api/runtime/PmMcpHome.java` | **Removed**: superseded by `InstallationLayout` (one resolver; no duplicate layout logic). |
| `pm-operator` | `src/main/java/de/planmarshall/operator/layout/OperatorLayout.java` | The layout as the operator sees it: `current`/`resolve`, `runtimeExecutable()` (`<PM_MCP_HOME>/bin/pm-mcpd`), `relayExecutable()`, `version()`, `installed()`, `runtimeUpgradable()`/`upgradabilityRefusal()`. |
| `pm-operator` | `src/main/java/de/planmarshall/operator/layout/package-info.java` | Package documentation. |
| `pm-operator` | `src/main/java/de/planmarshall/operator/OperatorContext.java` | Builds the runtime client's executable source from `OperatorLayout` (the resolved `pm-mcpd` of the operator's own installation). |

### 2.2 Tests

| Module | File | Purpose |
|---|---|---|
| `pm-api` | `src/test/java/de/planmarshall/api/layout/InstallationLayoutTest.java` | 30 tests: environment and executable resolution, a chain of two symlinks, the versioned directory and its installation root, the `versions/<version>` nesting, `resolveHome`/`resolveVersion`, the version-token grammar (13 parameterized cases), sibling location, and the refusals (unknown executable, root, outside `bin/`, dangling link, group-/other-writable home and root) plus runtime upgradability. |
| `pm-api` | `src/test/java/de/planmarshall/api/runtime/DetachedStartTest.java` | Uses `InstallationLayout.ENV_HOME` (constant moved). |
| `pm-api` | `src/test/java/de/planmarshall/api/runtime/PmMcpHomeTest.java` | **Removed**: its resolution coverage lives in `InstallationLayoutTest`. |
| `pm-operator` | `src/test/java/de/planmarshall/operator/layout/OperatorLayoutTest.java` | 5 tests: the runtime/relay siblings and the version of a staged installation, the running process's layout, and the refusals of an unversioned, an environment-named, and an insecure installation. |
| `pm-operator` | `src/test/java/de/planmarshall/operator/layout/SwitchedSymlinkLayoutIT.java` | 5 staged integration tests: two versioned directories and two `PATH` links; both entry binaries resolve the layout and start the `pm-mcpd` of the same version directory by absolute path; after an atomic link switch both resolve and start the new version; a lookalike `pm-mcpd`/`pm-exec` earlier on `PATH` is never started; an unversioned installation and a group-/other-writable root are refused. |

### 2.3 Documentation & archival

| File | Change |
|---|---|
| `doc/plans/README.adoc` | `installation-layout` status set to `done`; link retargeted to `implemented/`. |
| `doc/plans/implemented/installation-layout.adoc` | The work-package file, moved to the implemented archive. |
| `doc/plans/implemented/README.adoc` | New index row with the delivered artifacts. |
| `doc/agy/implemented/installation-layout-plan.md` | The implementation plan, moved to the archive with its checklist complete through step 5. |
| `doc/agy/implemented/README.md` | New archive row linking the plan and this report. |
| `doc/agy/installation-layout-plan-report.md` | This report (moved to `verified/` by the orchestrator after landing). |

---

## 3. Test & Verification Results

### 3.1 Trace matrix (Phase 1)

Built from the package's `== Trace` links and the roadmap item before code (Phase 1), each element mapped to
its destination:

| Kind | Source | Statement (short) | Destinations | Task |
|---|---|---|---|---|
| REQ | `PM-ARCH-1` bullet 17 | The four binaries of one release are installed side by side and start each other by path, on an identical release version | `InstallationLayout.locateSibling`/`binary`; `InstallationLayoutTest.Siblings`; `SwitchedSymlinkLayoutIT.resolvesAndStartsSibling` | Phase 2-4 |
| REQ | `PM-ARCH-1` bullet 13-14 (direct file reads, only `runtime-start.lock`) | The CLI binaries read only the closed list and take no lock but `runtime-start.lock` | `out of slice`: the layout adds no file read and no lock; `RuntimeAccess` still takes only `runtime-start.lock` | — |
| REQ | `PM-ARCH-6` (filesystem persistence; the installation holds no store) | Durable state lives in `<PM_MCP_BASE>`; the installation directory holds no store or configuration | `out of slice` (no store touched): `InstallationLayout` resolves paths only and reads no store; asserted by the class contract | — |
| REQ | `PM-ARCH-7` (upgrade & version mismatch) | The installation is upgraded by the runtime; an installation that is not runtime-upgradable is refused with its reason | `InstallationLayout.runtimeUpgradable`/`upgradabilityRefusal`; `OperatorLayout.runtimeUpgradable`/`upgradabilityRefusal`; `InstallationLayoutTest.Upgradability`, `OperatorLayoutTest` (the runtime owns the upgrade itself) | Phase 3, 5 |
| REQ | `PM-IMPL-7` (locks; the CLI takes no store lock) | Every store lock is the runtime's; the CLI's only lock is `runtime-start.lock` | `out of slice`: unchanged; the layout takes no lock | — |
| REQ | `PM-DIST-1` (single version authority) | One release is one version; a binary starts siblings of its own version directory | `InstallationLayout.version`/`locateSibling`; `InstallationLayoutTest.installationRootHoldsVersions`; `SwitchedSymlinkLayoutIT.resolvesNewInstallationAfterSwitch` | Phase 2, 4 |
| SPEC | CLI & Security § Installation Layout (Installation directory) | `<PM_MCP_HOME>/` holds `bin/pm-mcp`, `bin/pm-operator`, `bin/pm-mcpd`, `bin/pm-exec`, read-only after installation | `InstallationLayout` binary-name constants and `binary()` | Phase 2 |
| SPEC | CLI & Security § Installation Layout (Installation root) | A release is installed into `<install_root>/<version>/`, which is its `<PM_MCP_HOME>`; runtime-upgradable when versioned, resolved from the binary, in a user-writable root | `InstallationLayout.resolve`/`installRoot`/`runtimeUpgradable`; `InstallationLayoutTest.Upgradability`; `SwitchedSymlinkLayoutIT` | Phase 2, 4, 5 |
| SPEC | CLI & Security § Installation Layout (Resolution of `PM_MCP_HOME`) | The variable when set; otherwise the parent of the executable's directory with every symbolic link resolved | `InstallationLayout.resolve`/`resolveHome`; `InstallationLayoutTest.Resolution` (env, blank, symlink chain of two) | Phase 2, 3 |
| SPEC | CLI & Security § Installation Layout (Siblings by path, never by `PATH`) | A binary starts its sibling by absolute path in `<PM_MCP_HOME>/bin/`; no `PATH` lookup | `InstallationLayout.locateSibling`; `SwitchedSymlinkLayoutIT.ignoresExecutablesEarlierOnPath` | Phase 2, 4 |
| SPEC | CLI & Security § Installation Layout (Two `PATH` entries) | Exactly two links, `pm-mcp` and `pm-operator`, into a user bin directory on `PATH` | `SwitchedSymlinkLayoutIT` stages the two links and switches them | Phase 4 |
| SPEC | CLI & Security § Installation Layout (Upgrade) | A new release is unpacked beside the current one and both links are switched; the next command resolves the new installation | `InstallationLayout.resolve`; `SwitchedSymlinkLayoutIT.resolvesNewInstallationAfterSwitch` | Phase 4 |
| SPEC | Filesystem Layout § Machine & Runtime | The binaries live apart from `<PM_MCP_BASE>` in `<PM_MCP_HOME>/bin/`, which holds no configuration | `InstallationLayout.binary`; the resolver reads no store | Phase 2 |
| WATCH | `PM-WATCH-CLI-10` | A sibling is started by path, never through `PATH`; a `PM_MCP_HOME` from an unresolved symlink would point at the user bin directory | `SwitchedSymlinkLayoutIT.ignoresExecutablesEarlierOnPath` (fixture: a `pm-mcpd`/`pm-exec` earlier on `PATH` writing a sentinel; both binaries reached through a user-bin symlink and a chain of two symlinks) and `InstallationLayoutTest.symlinkChain`; `DetachedStartTest` spawns the staged `pm-mcpd` by its absolute path | Phase 3, 4 |
| WATCH | `PM-WATCH-CLI-11` | An upgrade never mixes releases: after the switch the new `pm-mcp`/`pm-operator` resolve and start the new `pm-mcpd` | `SwitchedSymlinkLayoutIT.resolvesNewInstallationAfterSwitch` | Phase 4 |
| WATCH | `PM-WATCH-MOD-2` | The release is exactly four binaries with complete digests and SBOM | `out of slice` (release-assembly packaging); `InstallationLayout` names the four binaries | — |

The package row is not split or extended: the matrix closes within `pm-api:…layout.*` and
`pm-operator:layout/**`.

### 3.2 Unit tests

`./mvnw test -pl pm-api,pm-operator` - **BUILD SUCCESS**: `pm-api` 454 tests, `pm-operator` 428 tests,
0 failures, 0 errors.

Key new suites: `InstallationLayoutTest` - 34 tests (including 13 parameterized version-token cases and two
permission refusals), `OperatorLayoutTest` - 5 tests; both 0 failures. New-code line coverage of the core
classes is 91.5% (`InstallationLayout` 89.2%, `InstallationLayoutException` 100%, `RuntimeAccess` 100%,
`OperatorLayout` 100%).

### 3.3 Integration tests

`./mvnw verify -Pintegration-tests -pl pm-api,pm-operator` - **BUILD SUCCESS**:

| Suite | Tests | Result |
|---|---|---|
| `SwitchedSymlinkLayoutIT` (`pm-operator`) | 5 | 0 failures |
| `PmOperatorProcessIT` (`pm-operator`) | 20 | 0 failures |
| `BuildGuardsIT` (`pm-api`) | 4 | 0 failures |

The staged `SwitchedSymlinkLayoutIT` runs on macOS and Linux (`/bin/sh`): it stages `install/0.1.0/bin` and
`install/0.2.0/bin` with mock launchers, places the two `PATH` links in `local/bin`, resolves the layout of the
operator through the link, starts both entry binaries through the link, then switches the links atomically
(`rename(2)`) to `0.2.0` and re-runs both, asserting that each started the `pm-mcpd` of its own version
directory by absolute path.

### 3.4 Domain invariants

| Invariant | Status |
|---|---|
| Java 25 / 26 modern language features | Held - `record`, text blocks, `List.getFirst()`; no preview features |
| Clients depend on `pm-api` only, never on Quarkus | Held - enforcer `clients-depend-on-pm-api-only` green; `pm-api` gained no dependency |
| `pm-api` depends on `jackson-core` only | Held - enforcer `jackson-core-only` green; the layout uses the JDK only |
| No model-facing content in a public artifact | Held - enforcer `no-model-facing-content` green |
| Path resolution in one resolver | Held - `InstallationLayout` is the single resolver (`PmMcpHome` removed) |
| Siblings started by absolute path, never `PATH` | Held - `locateSibling`; `SwitchedSymlinkLayoutIT.ignoresExecutablesEarlierOnPath` |
| Symlink traversal safety, absolute-path confinement | Held - real-path resolution; an executable that is not a release binary, an executable outside `bin/`, and a group-/other-writable installation, `bin/`, or root are refused |
| License headers, Javadoc, Checkstyle, OpenRewrite | Held - part of `verify -Ppre-commit` (§4) |

---

## 4. Build & Quality Evidence

Code and configuration changed (`pm-api/**`, `pm-operator/**`), so the full pre-commit gate ran:

```bash
./mvnw verify -Ppre-commit
```

Result: **BUILD SUCCESS** (`pm-api`, `pm-exec`, `pm-relay`, `pm-operator`, `pm-mock-runtime`, `pm-web-app`,
`pm-web-e2e` all green) with a clean `git status` after the OpenRewrite/license pass.

The CI gate on the pull request is the reusable Maven build (`./mvnw verify -Dmaven.compiler.release=25|26`,
`integration-tests`, and `./mvnw verify -Psonar` with `sonar.qualitygate.wait=true`). All three pass, and the
SonarCloud quality gate reports **passed** with new-code line coverage 91.5% on the core classes.

Two further gates of the package's _Done_ wording, recorded here for completeness:

- **`./mvnw verify -Pcoverage`**: the modules build and their tests pass, but `pm-api`'s bundle-level
  `jacoco:check` (branch coverage ≥ 0.80) fails at 0.71. This is pre-existing and unrelated to this package:
  the same ratio is 0.710 without `InstallationLayout` (whose 80 branches are 63 covered). The `coverage`
  profile is not part of the repository's CI; the enforced gate is the SonarCloud quality gate, which passes.
- **Native build**: the `pm-operator` POM defines a GraalVM `native` profile, but no `native-image` toolchain
  is available in this environment and the profile is not part of the CI build. The change adds no
  reflection or resource metadata and no native-only code path: `InstallationLayout` uses the JDK
  `java.nio.file` API only, and `OperatorContext` only passes an absolute path to the existing on-demand
  start. The native build therefore remains an unrun, environment-bound gate.

---

## 5. Notes & Follow-Ups

- **Layout shape.** The plan sketched `${PM_MCP_HOME}/versions/<version>/`; the specification's current
  *Installation root* bullet states `<install_root>/<version>/` is the `<PM_MCP_HOME>`. The resolver derives
  the home as the parent of the `bin/` directory that holds the running executable, so both shapes resolve
  identically; the staged IT uses the specification's shape and a unit test pins the `versions/<version>`
  nesting.
- **Runtime-owned refusal.** The runtime (`pm-mcp-core`) owns the upgrade and its `upgrade_not_possible`
  refusal, because it knows the two `PATH` links; the client's `InstallationLayout`/`OperatorLayout` expose
  `runtimeUpgradable()`/`upgradabilityRefusal()` for it, and no client-side pre-check duplicates the runtime's
  decision.
- **Out of slice.** `PM-ARCH-6`, `PM-IMPL-7`, and `PM-WATCH-MOD-2` are recorded `out of slice` above: the
  layout holds no store, takes no lock, and does not assemble the release (that is the runtime/assembly side);
  `InstallationLayout` only names the four binaries.

---

## 6. Review Responses

Findings from the review bots and SonarCloud are resolved after the pull request is opened (runbook Step 6).

| # | Source | Finding | Disposition |
|---|---|---|---|
| 1 | SonarCloud `java:S5998` (MAJOR BUG) | The version regular expression `\d+(\.\d+)*([-+]…)?` repeats a repetition and can overflow the stack on a long input | **Fixed**: the pattern is replaced by a linear parser (`numericPrefixEnd`/`isVersionSuffix`), which does not backtrack |
| 2 | SonarCloud `java:S1845` (BLOCKER) | The field `version` clashes with the constant `VERSION` | **Fixed**: the constant is gone with the pattern; the accessor stays `version()` |
| 3 | SonarCloud `java:S1192` (CRITICAL) | The literal `"the installation root "` is repeated four times | **Fixed**: extracted to the `INSTALLATION_ROOT` constant |
| 4 | SonarCloud `java:S7467` (MINOR) | Unused exception binding in two `catch` blocks | **Fixed**: unnamed `_` patterns |
| 5 | SonarCloud quality gate | `new_coverage` was 66.3% (minimum 80%) | **Fixed**: `OperatorLayoutTest` and the parameterized version-token cases raise new-code line coverage to 90.2% on the core classes |
| 6 | Self-review (code-reviewer lens) | The client-side `runtime upgrade` pre-check (`OperatorContext.installationUpgradabilityRefusal` + the `RuntimeCommand` guard) is unreachable: a versioned installation resolved from the binary is always upgradable, so the branch was dead and untested | **Removed**: the runtime owns the upgrade refusal; `OperatorLayout` keeps `runtimeUpgradable()` as the operator-facing contract |
| 7 | CodeRabbit | `resolve` accepts any executable in a `bin/` directory as an installation (a JVM's `$JAVA_HOME/bin/java` included) | **Fixed**: the running executable's real name must be one of the four release binaries, else `InstallationLayoutException`; test `notAReleaseBinary` |
| 8 | CodeRabbit (security, CWE-276) | The group-/other-write check covered only `home` and `installRoot`, not the `bin/` directory that holds the started executables | **Fixed**: `bin` is added to the check, and the message names all three; test `insecureBinDirectory` |
| 9 | CodeRabbit | `OperatorContext.installationUpgradabilityRefusal` treats a refused insecure layout as undetermined (finding on commit 6327d50) | **Resolved by removal**: the method and the `RuntimeCommand` guard were removed (finding 6); the runtime owns the refusal, so the finding no longer applies |
| 10 | CodeRabbit | Record the `verify -Pcoverage` and native-build results before keeping the package row at `done` | **Addressed**: §4 records both. The `coverage` profile's `pm-api` bundle `jacoco:check` is pre-existing red (0.71 branch; 0.710 without this change) and is not part of CI; the `native` profile is not run (no GraalVM toolchain) and the change adds no native-image metadata |
