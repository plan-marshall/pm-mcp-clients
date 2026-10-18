# Completion Report: `exec-read-denied-paths` (`pm-exec` Confinement Read-Denied Paths)

Companion report of the implementation plan
[`doc/agy/implemented/exec-read-denied-paths-plan.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/implemented/exec-read-denied-paths-plan.md),
executed under [`doc/agy/runbook.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/runbook.md) on branch
`feat/exec-read-denied-paths`.

---

## 1. Executive Summary

The work package `exec-read-denied-paths` (`doc/plans/implemented/exec-read-denied-paths.adoc`, Roadmap
Milestone 4) extends the job launcher `pm-exec` from one read-denied path to several, so that the runtime can
withhold not only `<PM_MCP_BASE>` but also the user's session-bus socket and, without the container-engine
opt-in, the container engine's sockets from every job's Landlock read set.

- **CLI (`Command.java`)**: `--deny-read <path>` is now repeatable; `Command.Launch` carries
  `List<Path> deniedPaths` in argument order, beside the backwards-compatible `deniedBase()` accessor that
  names the first path (`<PM_MCP_BASE>` by convention).
- **Read set (`ReadSet.java`)**: `enumerate(root, deniedPaths)` canonicalizes, validates, and deduplicates
  every denied path, then walks the tree breadth-first, carving out a hole around each denied path while
  keeping its siblings readable. Directories on the way down receive no rule (a job can traverse but not list
  them), as does each denied path itself; an entry whose real path (symbolic links resolved) lies inside a
  denied path or contains one is left out as well.
- **Confinement (`LinuxConfinement.java`, `Launcher.java`)**: the parsed paths are wired through unchanged;
  the applied ABI, the `landlock_abi` value, and the launch report line are untouched. ABI 9's
  `LANDLOCK_ACCESS_FS_RESOLVE_UNIX` is already granted with the read set, so a denied socket is refused from
  ABI 9 on.
- **Invariants**: `pm-exec` keeps zero dependencies on product modules (enforcer rule
  `no-product-module`) and zero external JSON libraries (only its built-in `Json`).

The behavior is guarded by unit suites (`CommandTest`, `ReadSetTest`, `LauncherTest`, `LinuxConfinementTest`)
and the process-level `PmExecIT`, and was additionally exercised against a real Linux Landlock kernel
(ABI 8) inside a container (see §3.3).

---

## 2. Delivered Artifacts

All changes lie in the single module `pm-exec`; no other module of the repository was touched (the plan's
module-disjointness matrix holds).

### 2.1 Production code (`pm-exec/src/main/java/de/planmarshall/exec/`)

| File | Purpose |
|---|---|
| `Command.java` | `Command.Launch` now carries `List<Path> deniedPaths` instead of `Optional<Path> deniedBase`; `--deny-read` is collected in the argument loop without the former "given twice" refusal; `deniedBase()` returns the first denied path for backward compatibility; the `DENY_READ` constant documents the socket/base use. |
| `ReadSet.java` | `enumerate(Path root, List<Path> deniedPaths)` canonicalizes, validates (every denied path strictly below the root), and deduplicates the holes, then enumerates the read set breadth-first: a denied path gets no rule, an ancestor directory is descended into but gets no rule, any other entry gets a rule unless its real path overlaps a denied path. Private helpers `isAncestorOfAny` and the multi-path `overlaps`. |
| `LinuxConfinement.java` | Passes `launch.deniedPaths()` to `ReadSet.enumerate`; still refuses a launch without any `--deny-read` before the first native call. |
| `Confinement.java` | Unchanged (listed for completeness): the classification and `landlock_abi` reporting are not affected by the number of holes. |
| `Launcher.java` | Unchanged (listed for completeness): the parsed launch already flows to `LinuxConfinement`; the report line and exit codes are unchanged. |
| `package-info.java` | Protocol documentation updated to `[--deny-read <path>]...` with the base, session-bus socket, and container-engine socket examples. |

### 2.2 Tests (`pm-exec/src/test/java/de/planmarshall/exec/`)

| File | Purpose |
|---|---|
| `CommandTest.java` | A launch with several `--deny-read` paths in order, the single-path case, a bare launch (`deniedPaths()` and `deniedBase()` empty), and a relative `--deny-read` refusal. The former `--deny-read`-twice refusal is removed (repeatable is the new contract). |
| `ReadSetTest.java` | `enumerate` with one denied path, two disjoint paths, only the second path, a nested pair, a denied path given as a symbolic link (canonicalized), a duplicate, a missing path (fail-closed), the root itself, and a path outside the root beside a valid one. The fixture gained `run/user/1000/{bus, docker.sock, systemd/}` beside `home/alice/.plan-marshall-mcp`. |
| `LauncherTest.java` | A Linux launch with two `--deny-read` paths: both holes get no rule, the ancestor directories get no rule, the siblings stay readable, and the report line still says `confined`/ABI 3. |
| `LinuxConfinementTest.java` | A multi-hole launch asserting the rules that reach the kernel via the fake kernel; the launch helper now takes a `List<Path>`. |
| `PmExecIT.java` | A new Linux gated case launching `pm-exec` as a child with `<PM_MCP_BASE>` plus a `secrets` directory denied: both are unreadable, their siblings (`sibling.txt`, `checkout/`) stay readable, and the report line is unchanged. |

### 2.3 Documentation

| File | Change |
|---|---|
| `doc/plans/README.adoc` | `exec-read-denied-paths` status set to `done`; link retargeted to `implemented/`. |
| `doc/plans/implemented/exec-read-denied-paths.adoc` | The work-package file, moved to the implemented archive. |
| `doc/plans/implemented/README.adoc` | New index row with the delivered artifacts. |
| `doc/agy/implemented/exec-read-denied-paths-plan.md` | The implementation plan, moved to the archive once its items are checked off. |
| `doc/agy/implemented/README.md` | New archive row linking the plan and this report. |
| `doc/agy/exec-read-denied-paths-plan-report.md` | This report (moved to `verified/` by the orchestrator after landing). |

---

## 3. Test & Verification Results

### 3.1 Trace matrix (Phase 1)

Built from the package's `== Trace` links before code, mapped to destinations:

| Source | Statement | Destination |
|---|---|---|
| `PM-SEC-5` (Confinement & Isolation) | A job's read set leaves out the session bus and, without the opt-in, the container engine's sockets | `Command.DENY_READ` (repeatable), `ReadSet.enumerate`, `LinuxConfinement.apply` |
| Job Runtime § Session Bus | The read set leaves out `$XDG_RUNTIME_DIR/bus` | `ReadSet` multi-hole enumeration; `ReadSetTest.disjoint`/`secondOnly`; `PmExecIT.severalDeniedPaths` |
| Job Runtime § Container Engine | Without the opt-in the socket paths stay out of the read set | same, plus `package-info` protocol examples |
| Job Runtime § Linux Filesystem Confinement | Read set as rules on `/` minus the path to the holes, siblings readable, the way-down not listable | `ReadSet` BFS; `ReadSetTest`, `LinuxConfinementTest.severalDeniedPaths`, real-kernel smoke (`ls` refusals) |
| `PM-WATCH-JOB-35` | Multi-hole Landlock read-set enumeration | `ReadSet.enumerate(root, List<Path>)` |
| `PM-IMPL-7` | `pm-exec` stays free of product modules and external JSON libraries | Enforcer `no-product-module`; only `Json.java` parses JSON |

The package row is not split or extended: the matrix closes within `pm-exec`.

### 3.2 Unit tests

`./mvnw test -pl pm-exec` — **BUILD SUCCESS**, 79 tests, 0 failures, 0 errors, 3 skipped
(the three `LinuxCallsTest` cases, which are `@EnabledOnOs(LINUX)` and are skipped on macOS).

Key suites: `CommandTest` (4 accepts / 11 refuses), `ReadSetTest` (9 enumerate / 3 canonical),
`LauncherTest` (5 probe / 5 launch), `LinuxConfinementTest` (8), `LandlockTest` (22).

### 3.3 Integration tests

`./mvnw verify -Pintegration-tests -pl pm-exec` — **BUILD SUCCESS**, 13 tests, 0 failures, 8 skipped on
macOS (the `Landlock` nested class is `@EnabledOnOs(LINUX)`).

Because the local host is macOS, the Linux-only assertions were additionally exercised against a real
Landlock kernel by running the compiled `pm-exec` classes in `eclipse-temurin:25-jdk` on Docker (kernel
reporting **Landlock ABI 8**, `confinement: confined`):

| Scenario | Argument vector (abridged) | Observed |
|---|---|---|
| Read `<PM_MCP_BASE>/run/runtime.token` | `--deny-read base --deny-read secrets --deny-read run/user/1000/bus` | `Permission denied`, exit 1 |
| Read `secrets/token` | same | `Permission denied`, exit 1 |
| Read sibling `sibling.txt` | same | `sibling-canary`, exit 0 |
| Read sibling `checkout/readme.md` | same | `checkout-canary`, exit 0 |
| Read `run/user/1000/systemd/x` | same | `systemd-entry`, exit 0 |
| Write own project | same, `--write project` | file created, exit 0 |
| Write into `<PM_MCP_BASE>` | same | `Permission denied`, exit 2 |
| List `/tree`, `/tree/run`, `/tree/run/user/1000` | same | `cannot open directory ... Permission denied` (the way down is not listable, traversal still works) |
| List `checkout`, `run/user/1000/systemd` | same | listed |
| Nested `--deny-read base --deny-read base/run` | | denied path unreadable, sibling readable |

The nested case confirms the documented invariant: a hole covered by an ancestor's hole adds nothing and
does not break the rest of the set.

### 3.4 Domain invariants

| Invariant | Status |
|---|---|
| Java 25 (class file major 69), modern language features | Held |
| `pm-exec` depends on no product module | Held — enforced by the `no-product-module` enforcer rule (skipped nowhere in this module) |
| No external JSON library | Held — `Json.java` unchanged and still the only JSON handling |
| Every path absolute and normalized at parse time | Held — `Command.absolute` |
| Denied paths canonical and strictly below the root | Held — `ReadSet.enumerate` validates after `canonical` |
| License headers, Javadoc, Checkstyle | Held — part of `verify -Ppre-commit` (§4) |
| Launch report line and ABI reporting unchanged | Held — `LauncherTest`, `PmExecIT` |

---

## 4. Build & Quality Evidence

Code and configuration changed (`pm-exec/**`, `doc/**`), so the full pre-commit gate ran:

```bash
./mvnw verify -Ppre-commit
```

Result: **BUILD SUCCESS**, with a clean `git status` (no OpenRewrite reformats, no untracked artifacts).

---

## 5. Notes & Follow-Ups

- The runtime side (`pm-mcp-core`) that computes and passes the socket paths lies outside this repository;
  `pm-exec` now accepts them. The `--deny-read` protocol addition is backward compatible: a single path, as
  the runtime passes today, behaves exactly as before.
- Below Landlock ABI 9 the filesystem rules do not govern `connect(2)` to a pathname socket, so the withheld
  `DBUS_SESSION_BUS_ADDRESS`/`XDG_RUNTIME_DIR` remains the guard there; the smoke host ran ABI 8, matching
  the specification's accepted residual risk.
- `deniedBase()` is kept as a convenience accessor; no production caller depended on the removed
  `Optional<Path>` shape.
