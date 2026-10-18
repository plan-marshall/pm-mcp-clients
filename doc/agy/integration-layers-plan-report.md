# Completion Report: `integration-layers` — Integration-Test Layers in `pm-relay` and `pm-operator`

Companion report of the implementation plan
[`doc/agy/implemented/integration-layers-plan.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/implemented/integration-layers-plan.md),
executed under [`doc/agy/runbook.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/runbook.md) on branch
`feat/integration-layers`. It is the work package `integration-layers` (Roadmap Milestone 2,
`doc/plans/integration-layers.adoc`).

---

## 1. Executive Summary

The work package formalizes the process-level integration test layers (`*IT`) of the two client binaries that
carry an operator-visible process boundary, hardening the two existing `*IT` classes against the requirements
they trace to:

- **`pm-relay` (`PmMcpProcessIT`)** — the `pm-mcp serve` STDIO relay as a real process against the fake runtime on
  a Unix socket. New cases pin `stdout` framing purity under daemon faults (a JSON-RPC error response and an HTTP
  500), the immediate-EOF termination path (exit `0`, no output), the answer to a request whose daemon stream
  breaks mid-response (`runtime_unavailable`, exit cleanly on EOF), and the JSON-RPC `-32600` parse error for a
  malformed `stdin` line while the relay keeps relaying valid requests.
- **`pm-operator` (`PmOperatorProcessIT`)** — the terminal client and operator verbs as a real process. New cases
  pin the runtime-free verbs (`--version`, `--help`) as exit-`0` and zero-footprint under `PM_MCP_BASE`, the
  coding-agent host detection (`PM-SEC-3`, exit code `4`, host-named diagnostic) for every host signal
  (`ANTIGRAVITY_AGENT`, `CLAUDE_CODE`, `OPENCODE_CLIENT`, `CODEX_*`) across verbs, and the interactive-terminal
  gate of the inspection verbs (`config`, `questions`, `settings`). The existing `status`-without-runtime case
  already covers the third runtime-free verb.

No production code changed: the package delivers the guarding process tests the requirements demand and the
`Verify -Pintegration-tests` exit of `doc/plans/integration-layers.adoc`. The process-IT layer now proves the
stdout-hygiene and operator-gate invariants against the packaged JAR/native binary as a real child process, not
only in-process.

---

## 2. Delivered Artifacts

| File | Purpose |
|---|---|
| `pm-relay/src/test/java/de/planmarshall/relay/PmMcpProcessIT.java` | Four new cases: `stdoutFramingUnderFaults`, `abruptSocketDisconnect`, `immediateEof`, `malformedStdin` (8 process cases total) |
| `pm-operator/src/test/java/de/planmarshall/operator/PmOperatorProcessIT.java` | Four new cases: `versionIsRuntimeFree`, `helpIsRuntimeFree`, `refusesCodingAgentHostSignals`, `inspectionVerbsRefuseWithoutTerminal` (24 process cases total) |
| `doc/agy/implemented/integration-layers-plan.md` | The archived implementation plan (this work package) |
| `doc/agy/integration-layers-plan-report.md` | This completion report |
| `doc/plans/README.adoc`, `doc/plans/implemented/integration-layers.adoc` | The work package marked `done` and archived |

Both test classes run under the `-Pintegration-tests` profile only (Failsafe `*IT` naming,
`@EnabledOnOs({OS.MAC, OS.LINUX})`), matching Module Structure § Test Placement.

---

## 3. Phase 1 — Trace Matrix & Specification Alignment

Rows are the requirement bullets, specification sections, and watch items the package proves; a row the
package does not carry is marked `out of slice` with its reason.

| Kind | ID / anchor | Statement (short) | Destinations | Covered by |
|---|---|---|---|---|
| REQ | `PM-ARCH-3` bullet 2 | The relay forwards MCP messages both ways, preserving JSON-RPC `id` | code, test | `Relay`; `PmMcpProcessIT.hygieneAndTermination`, `stdoutFramingUnderFaults`, `abruptSocketDisconnect` |
| REQ | `PM-ARCH-3` bullet 4 | MCP served only over a user-private Unix socket | code, test | `RuntimeAccess`; the fake runtime's Unix socket in every relay IT |
| REQ | `PM-ARCH-3` bullet 5 | `stdout` reserved for newline-framed JSON-RPC, diagnostics on `stderr` | code, test | `Relay.emit`/`diagnostic`; `hygieneAndTermination`, `stdoutFramingUnderFaults`, `malformedStdin`, `immediateEof` |
| REQ | `PM-ARCH-3` bullet 6 | Relay accepts the frontier protocol and the session-opening `initialize` | code, test | `ProtocolData`; `hygieneAndTermination` (initialize handshake) |
| REQ | `PM-SEC-3` bullet 1 | Operator-only acts excluded from the model surface | code, test | `OperatorVerb`/`OperatorGate`; `refusesCodingAgentHostSignals`, all `*RefusesWithoutTerminal` |
| REQ | `PM-SEC-3` bullet 3 (CLI) | An operator act is accepted only from an interactive terminal with no agent marker | code, test | `OperatorGate.verify`; `PmOperatorProcessIT` terminal and host-signal refusals |
| REQ | `PM-SEC-4` bullet 8 | Strict `stdout` hygiene with diagnostics on `stderr` | code, test | `Relay`; `PmMcpProcessIT` hygiene and fault cases |
| REQ | `PM-SEC-4` bullet 9 | Child subprocess `stdout`/`stderr` redirected, never inherited | code, test | `pm-exec`/job launcher (own module); `RuntimeFixture` spawns no inheriting child |
| REQ | `PM-SEC-4` bullet 10 | Every file `0600`, every directory `0700`; `0700` base protects the rest | code, test | `RuntimeFixture.writeToken`, `OnDemandStart`; `detachedStart` asserts `rwx------` on `locks/` |
| SPEC | MCP Tools § Errors & Protocol Testing — End-to-End STDIO Integration Testing | The relay layer verifies forwarding against its stub daemon over real `stdin`/`stdout`; the operator layer serves no MCP | test | `PmMcpProcessIT` (stdio relay), `PmOperatorProcessIT` (binary, no MCP) |
| SPEC | MCP Tools § Errors & Protocol Testing — Protocol errors vs. domain failures | Malformed JSON-RPC is a JSON-RPC `error` (`-32700`/`-32600`), not a `CallToolResult` | code, test | `Relay.accept` (`INVALID_REQUEST`); `malformedStdin` |
| SPEC | Module Structure § Test Placement | Integration tests are `*IT` under `-Pintegration-tests` in the four layers | code, test | Both `*IT` classes and the `integration-tests` profile wiring |
| WATCH | `PM-WATCH-CLI-14` | Operator checks refuse before anything is sent; every verb refuses without a terminal and with each agent marker | test | `refusesCodingAgentHostSignals`, `inspectionVerbsRefuseWithoutTerminal`, all `*RefusesWithoutTerminal` (stdout empty, zero requests) |
| WATCH | `PM-WATCH-RUN-5` | Without a runtime the runtime-free verbs start nothing and write nothing | test | `statusWithoutRuntime`, `versionIsRuntimeFree`, `helpIsRuntimeFree` (`Files.notExists(base)`) |
| WATCH | `PM-WATCH-RUN-4` | A client refuses an insecure `run/`/`runtime.token` and leaks no token | test | Existing `detachedStart` (`locks/` mode) and `RuntimeFixture` (written `0600`/`0700`); the per-use check is unit-guarded in `pm-api` |
| WATCH | `PM-WATCH-RUN-10` | A cancel of an answered request is dropped | test | `out of slice` for the IT layer: guarded by `RelayTest` (in-process relay rules); the IT exercises forwarding, not the cancel rule |
| WATCH | `PM-WATCH-RUN-11` | Identity arguments never appear in a tool schema | test | `out of slice` for the IT layer: guarded in-process (`RelayTest`/`PmMcpTest`); the daemon-side schema is a `pm-mcp-server` concern |
| WATCH | `PM-WATCH-RUN-16` | The listen stream stays silent and `initialize` issues no session | test | `out of slice`: the runtime/daemon side (release-shape layer, `pm-e2e`), not the relay stub |
| WATCH | `PM-WATCH-CLI-21`, `PM-WATCH-CLI-27` | The web listener route table is a closed allowlist; a listener exists only while enabled | test | `out of slice`: web listener is runtime-side (`pm-mcp-server`); the client sends only the operator verbs |
| WATCH | `PM-WATCH-CLI-13` | Weakening an enforcement rule needs the terminal | test | `out of slice`: bundle-installer/change-plan surface, no installer verb in this package |

**Corrections.** None. The plan's Phase 2 wording ("relay exits cleanly without hanging" on an abrupt daemon
socket close) is honoured as the closed relay rule: the relay answers the pending request at once with
`runtime_unavailable` (rule 3) and then ends on `stdin` EOF; the test asserts both, so the observation is clean
termination, not an early exit on the daemon's close. No requirement or specification text needed correction.

---

## 4. Test & Verification Results

| Suite | Result |
|---|---|
| `pm-relay` `PmMcpProcessIT` (8 cases: 4 pre-existing + 4 new) | Pass |
| `pm-operator` `PmOperatorProcessIT` (24 cases: 20 pre-existing + 4 new) | Pass |
| `./mvnw verify -Pintegration-tests -pl pm-relay,pm-operator` | `BUILD SUCCESS` |

New coverage detail:

- `stdoutFramingUnderFaults` — a daemon HTTP 500 becomes a single newline-framed JSON-RPC error object with the
  request's `id` (`-32603`, `runtime_error: HTTP 500`); a daemon JSON-RPC error (`-32601`) is forwarded
  verbatim; `stdout` holds exactly the two frames and nothing else.
- `abruptSocketDisconnect` — a response head announcing 100 bytes followed by a truncated body and a closed
  connection makes the relay answer the pending request with the `-32000` / `runtime_unavailable` error on
  `stdout`, log the read failure on `stderr`, and exit `0` within the budget once `stdin` closes (no hang).
- `immediateEof` — `stdin` closed before any request: exit `0`, empty `stdout`, empty `stderr`.
- `malformedStdin` — a non-JSON line yields the `-32600` / `invalid_request` frame with a `null` `id`; a following
  `initialize` is still served, proving the relay continues; `stdout` holds exactly the two frames.
- `versionIsRuntimeFree` / `helpIsRuntimeFree` — exit `0`, the expected text (`pm-operator <version>` /
  `Usage: pm-operator`), and nothing created below `PM_MCP_BASE`.
- `refusesCodingAgentHostSignals` — each of `ANTIGRAVITY_AGENT`→`antigravity`, `CLAUDE_CODE`→`claude`,
  `OPENCODE_CLIENT`→`opencode`, `CODEX_HOME`→`codex` run against `reports` and `plan fix-relay` exits `4` with
  `operator refused: coding-agent environment detected (<host>)` and writes nothing to `stdout`.
- `inspectionVerbsRefuseWithoutTerminal` — `config`, `questions`, and `settings` without a terminal exit `4` with
  `operator refused: interactive terminal required`.

**Startup budget (`PM-TECH-3`, 50 ms).** The first-forward / first-request medians are written to
`target/verification-results/` by the existing `startupToFirstForward` and `startupToFirstRequest` cases. The
budget is asserted for the native binary; these runs are JVM mode (relay median ≈ 309 ms) and are recorded as
`pass: false` informationally, exactly as the pre-existing cases intend. The native image is not built for this
package (`-Pnative`), which only the release/verification run exercises; no source change here can regress it.

---

## 5. Domain Invariants

1. **Test-only slice**: no production source changed; `pm-relay` and `pm-operator` main classes are untouched.
2. **IT placement**: both classes keep the `*IT` suffix, `@EnabledOnOs({OS.MAC, OS.LINUX})`, and the
   `integration-tests` Failsafe profile; nothing moved into the unit (`*Test`) layer.
3. **Real process boundary**: every case runs the packaged binary as a child process (`BinaryUnderTest`), with a
   cleared environment and the fake runtime on a Unix socket — never an in-process call.
4. **AAA and determinism**: each new case arranges the daemon/verb, acts by writing one framed line or invoking
   the binary, and asserts the observable `stdout`/`stderr`/exit code; responses are read one at a time so
   ordering is deterministic, and every case ends by closing `stdin` (or relies on the verb exiting).
5. **Security boundaries** (`PM-SEC-3`): the host-signal case asserts the refusal names the detected host and that
   nothing reached `stdout`; it complements the in-process matched control (`PmOperatorTest`, interactive context
   sends the request).
6. **Zero footprint** (`PM-ARCH-7`/`PM-WATCH-RUN-5`): `--help`, `--version`, and `status` leave `PM_MCP_BASE`
   absent (`Files.notExists`), so a read-only verb never creates the tree.

---

## 6. Build & Quality Evidence

- `./mvnw verify -Pintegration-tests -pl pm-relay,pm-operator` → `BUILD SUCCESS`; `pm-relay` 8/8 and `pm-operator`
  24/24 Failsafe cases pass (`pm-relay` 9.1 s, `pm-operator` 24.7 s), zero failures/errors/skips.
- `./mvnw verify -Ppre-commit` (full reactor: OpenRewrite, licence headers, Checkstyle, Enforcer, Javadoc, unit
  suites) is run before the PR; it reformats the two test files through the house style if needed and is committed.
- No flaky retries and no network access: the fake runtime binds a Unix socket under the process temporary
  directory; every spawned process is bound by `CliProcess`'s `destroyForcibly` on close.

---

## 7. Process Adherence

- **Personas adopted**: `persona-integration-tester` (process lifecycle, stdio pipes, Failsafe `*IT`),
  `persona-module-tester` (AAA, determinism, boundary and fault cases), `persona-security-expert` (`PM-SEC-3`
  host signals, `PM-SEC-4` stdout hygiene and file modes), `persona-documenter` (this report, roadmap status),
  `persona-code-reviewer` (finding triage at the review window).
- **Workflow**: isolated worktree `pm-mcp-clients-integration-layers` on `feat/integration-layers`; incremental
  test-compile and `-Pintegration-tests` runs for the two modules; the pre-commit gate before the PR; the plan
  archived to `doc/agy/implemented/` and the work package marked `done` in `doc/plans/README.adoc`.
- **Scope note**: the package deliberately owns only the two `*IT` classes; the daemon-side and web-listener
  integration layers and the release-shape `pm-e2e` layer are separate packages/modules (matrix rows above).
