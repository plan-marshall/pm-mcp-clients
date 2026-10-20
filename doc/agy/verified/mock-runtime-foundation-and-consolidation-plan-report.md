# Completion Report: `mock-runtime-foundation-and-consolidation`

**Plan**: [`doc/agy/implemented/mock-runtime-foundation-and-consolidation-plan.md`](../implemented/mock-runtime-foundation-and-consolidation-plan.md)
**Branch**: `feat/mock-foundation`
**Scope**: Wave 1 foundation for the mock-runtime remediation series.

---

## 1. Executive Summary

The foundation wave delivered the structural pieces the four scenario-consolidation waves build on:

1. A **five-scenario catalog** (`onboarding`, `running-work`, `projects-and-start`, `reports-and-system`, `designer`) exposed as `StandardScenarios.PRIMARY_SCENARIOS`, with composite `scenario#state` resolution, a legacy alias table, and a launcher that presents only the five primary scenarios.
2. A **closed scenario schema** in `ScenarioLoader`: unknown members at root, state, route, link, event, and auto level are refused with the file, the state or route, and the field; `link.error` is checked against `ApiErrorCode`; known static route representations are decoded with `pm-api` codecs.
3. **Scoped SSE streams** with a bounded `EventEnvelope` ring buffer per scope, `Last-Event-ID` replay, and a `reset` envelope for stale ids and after `/mock/reset` or a scenario switch.
4. **Dispatch precedence** so state operator links and scenario routes are evaluated before stateful domain defaults, plus domain gating so an unoffered `system` or `decisions` family answers `404 mock_no_route`.
5. A **`/mcp` JSON-RPC endpoint** (`initialize`, `tools/list` with SSE progress, `tools/call`, notifications, scenario-configured answers) and migration of the relay process integration test to the real mock runtime.

All tests pass locally, including the full reactor unit suite, the mock-runtime suite (342 tests), and `PmMcpProcessIT` (8 tests) against the mock runtime.

---

## 2. Delivered Artifacts

### `pm-mock-runtime` — production

| File | Change |
|---|---|
| `src/main/java/de/planmarshall/mock/StandardScenarios.java` | Added `PROJECTS_AND_START`, `REPORTS_AND_SYSTEM`, and the ordered `PRIMARY_SCENARIOS` list; `ALL` retains every legacy name during migration. |
| `src/main/java/de/planmarshall/mock/scenario/ScenarioRegistry.java` | Composite `name#state` resolution (validated, start overridden), `registerLegacyAlias` / `legacyAlias` / `legacyAliases`, and the standard legacy alias table. Direct file resolution always wins over an alias, so files can be deleted wave by wave. |
| `src/main/java/de/planmarshall/mock/scenario/ScenarioLoader.java` | Closed-schema member sets and `rejectUnknownFields` at every level; `ApiErrorCode.fromWireOrNull` validation of `link.error`; codec validation for `GET /api/v1/projects`, `/reports`, `/overview`, `/system`, and `/workflows`; `designer`/`start_case` accepted as documented transitional root extensions. |
| `src/main/java/de/planmarshall/mock/http/MockEventStream.java` | Subscription scope, explicit event ids (`send`/`sendJson` overloads), `lastEventId()`. |
| `src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` | Scoped ring buffers (`machine`, `plan:<id>`, `epic:<id>`, `entry:<id>`, `workspace:<id>`, `reports`, `activity:<plan>:<task>`), envelope construction, scoped delivery, `Last-Event-ID` replay, `reset` on stale ids and reset/switch, reordered dispatch, live-domain classification, `/mcp` handler, domain gating for system and decisions. |
| `src/main/resources/scenarios/projects-and-start.json` | New primary scenario seeded from `projects.json` and `start.json` (project explorer + start states/routes + `start_case`). |
| `src/main/resources/scenarios/reports-and-system.json` | New primary scenario seeded from `reports.json` and `system.json` (reports list + runtime lifecycle states). |
| `start.sh` | Presents exactly the five primary scenarios with their one-line descriptions, defaults to `running-work`, and still accepts a name, a `name#state` reference, or a path. The list is read from `scenarios/primary.txt`, the same resource `StandardScenarios` loads. macOS Bash 3.2 / `set -u` idioms preserved. |
| `src/main/resources/scenarios/primary.txt` | The launcher order, read by both `StandardScenarios.PRIMARY_SCENARIOS` and `start.sh`. |
| `src/main/resources/scenarios/plan-view.json`, `decisions.json` | Removed the non-schema state `summary` member so the closed schema accepts them. |

### `pm-mock-runtime` — tests

| File | Change |
|---|---|
| `EventStreamScopeTest.java` | Scoped filtering, ordered `Last-Event-ID` replay, `reset` for an unknown id, and `reset` after `/mock/reset`. |
| `McpEndpointTest.java` | `initialize`, `tools/list` SSE (progress + message), `tools/call` with a job token, notifications (`202`), and the unauthenticated `401`. |
| `ScenarioLoaderTest.java` | Unknown root/state/link/event/route/auto members, unknown `link.error`, and a codec-refused route representation. |
| `ScenarioRegistryTest.java` | Composite resolution, split helpers, alias fallback vs. direct-file precedence, published alias table. |
| `StandardScenariosTest.java` | Exactly five primary scenarios and a delivered resource file for each. |
| `MockRuntimeEngineTest.java` | Unoffered system/decisions family answers `mock_no_route`; a scenario route outside the live domains wins over the domain defaults. |
| `ReportsScenarioTest.java` | Failure message now prints the event data on a mismatch (diagnostic only). |

### `pm-relay`

| File | Change |
|---|---|
| `pom.xml` | Test-scope dependency on `pm-mock-runtime` (the parent enforcer already permits this, as in `pm-operator`). |
| `PmMcpProcessIT.java` | The daemon-bound tests (`hygieneAndTermination`, `startupToFirstForward`, `malformedStdin`) now run against `MockRuntime`; assertions parse the JSON-RPC result instead of pinning a stub string. Fault-injection tests keep the fake double. |
| `RelayTest.java` | Added `againstMockRuntime`, an end-to-end relay initialize against `MockRuntime` with request capture. |

---

## 3. Test & Verification Results

| Gate | Command | Result |
|---|---|---|
| Mock-runtime suite | `./mvnw -pl pm-mock-runtime test` | **342 tests, 0 failures, 0 errors** |
| Full reactor unit suite | `./mvnw test -DskipITs` | **BUILD SUCCESS** (all modules) |
| Relay process ITs | `./mvnw -pl pm-relay -am verify -Pintegration-tests -Dit.test=PmMcpProcessIT -Dfailsafe.failIfNoSpecifiedTests=false` | **8 tests, 0 failures** |
| Pre-commit gate | `./mvnw verify -Ppre-commit` | **BUILD SUCCESS** (6:05 min) |

Additional focused evidence:

- `EventStreamScopeTest` proves a `plan:p-1` event reaches the machine and `p-1` streams, and that `p-2` receives only its own next event.
- `EventStreamScopeTest` proves replay order (`login` then `token`) after `Last-Event-ID`, and `reset` for id `999999` and after `/mock/reset`.
- `McpEndpointTest` proves the negotiated protocol version, the SSE `notifications/progress` → `message` sequence, and job-token authentication on `/mcp`.
- `OperatorInteractionScenarioTest` and `ReportsScenarioTest` continue to pass with the envelope `data` object (the SSE `event`/`id` fields carry the outer envelope members), so clients see the same flat payload plus `event`, `at`, `scope`.

---

## 4. Domain Invariants

| Invariant | Status |
|---|---|
| Java 25 toolchain, module boundary `clients → pm-api` and `pm-mock-runtime` in test scope only | Preserved; `pm-api` gained no dependency, `pm-relay` adds a test-scope mock runtime dependency only. |
| Closed schema and fail-closed enum validation | Enforced for root/state/route/link/event/auto members, `link.error`, and known route codecs. |
| The mock runtime carries the verified daemon path | The daemon-bound `PmMcpProcessIT` tests and the new relay end-to-end test run against `MockRuntime`; `/mcp` lives in `MockRuntimeEngine`. Scripted-handler doubles remain only for fault injection and header-capture assertions. |
| Bounded event buffer and resume rule | 64 envelopes per stream scope; replay after `Last-Event-ID`; `reset` otherwise and after reset/switch. |
| Deterministic tests, no real daemon/harness | All new tests use the in-process `MockRuntime` or an explicitly fault-injecting double. |
| Shell portability | `start.sh` uses the approved empty-array idiom and repo-root Maven anchoring unchanged. |

---

## 5. Decisions, Boundaries, and Follow-Ups

1. **The 60 fine-grained scenario files were not deleted.** This wave owns the catalog, resolver, and launcher; the four consolidation plans (`mock-runtime-onboarding-and-entry`, `-running-work`, `-projects-and-start`, `-reports-system-designer`) explicitly own the deletions and the rich state content. The alias table resolves a legacy name to its primary scenario **only when the legacy file is absent**, so every existing test keeps its exact fixture while each wave can delete file by file.
2. **`projects-and-start.json` and `reports-and-system.json` are initial consolidated skeletons.** They merge the existing projects/start and reports/system states and routes so the five-scenario launcher and `PRIMARY_SCENARIOS` work today; the downstream waves enrich them.
3. **Dispatch precedence is domain-aware.** State links and scenario routes win over Java defaults for every path outside the engine's live, mutating domain families (`plans`, `epics`, `entries`, `projects`, `reports`, `system`, `web`, …). Inside those families the stateful handler is the richer, mutation-aware answer; a scenario link or route is the fallback when no handler claims the path. The families live in one `LIVE_DOMAIN_PREFIXES` list and a test pins a representative path of each. Operator links below `/links/` are always scenario-first. This fixes the shadowing defect without breaking favourite toggles, report reads, or device lifecycle fixtures that intentionally mutate engine state.
4. **`FakeRuntimeServer` is retained as a test double, not as a second daemon.** `pm-api` cannot depend on `pm-mock-runtime` (the enforcer's `clients-depend-on-pm-api-only` rule and the acyclic module graph forbid it), so `RuntimeFixture` cannot delegate to the mock runtime. The daemon-bound `PmMcpProcessIT` tests were migrated, and `RelayTest` gained a `MockRuntime` end-to-end test. The remaining uses inject `500` responses, broken chunk streams, delimiter faults, or scripted successful responses whose purpose is to assert the relay's exact headers and framing (`RelayTest.Session`, `PmMcpProcessIT.detachedStart`); those assertions are about the relay, not about a runtime implementation. The duplicate *daemon* is gone from the verified path; the double is now explicitly a test fixture.
5. **Closed-schema extensions.** `designer` and `start_case` remain accepted root members until the reports/designer and projects/start waves move those fixtures into `pm-api` codecs. Operator-state validation remains conditional on a `state.state_id`, as before, because the legacy flat representations are migrated by the downstream waves.
6. **Engine-internal event names outside the catalogue.** Names such as `task.activity` are wrapped in the same `event`/`id`/`data` envelope shape but are not claimed as `EventName` members; catalogue members are built through `EventEnvelope` and therefore validated by the same codec clients use.

---

## 6. Build & Quality Evidence

```
[INFO] Reactor Summary for pm-mcp-clients 0.1.0-SNAPSHOT:
[INFO] pm-mcp-clients ..................................... SUCCESS
[INFO] pm-api ............................................. SUCCESS
[INFO] pm-exec ............................................ SUCCESS
[INFO] pm-mock-runtime .................................... SUCCESS
[INFO] pm-relay ........................................... SUCCESS
[INFO] pm-operator ........................................ SUCCESS
[INFO] pm-web-app ......................................... SUCCESS
[INFO] pm-web-e2e ......................................... SUCCESS
[INFO] BUILD SUCCESS
```

No temporary diagnostics, commented-out code, or generated artifacts remain. `git status` after the gate contains only the files listed in Section 2.

---

## 7. Next Waves

The alias table and `PRIMARY_SCENARIOS` are the contract the next waves consume:

- `mock-runtime-onboarding-and-entry` → enrich `onboarding.json`, delete the onboarding/entry files.
- `mock-runtime-running-work` → enrich `running-work.json`, delete plan-view/intent/decisions/decomposition/control files.
- `mock-runtime-projects-and-start` → enrich `projects-and-start.json`, delete projects/start/enrolment/git/lesson files.
- `mock-runtime-reports-system-designer` → enrich `reports-and-system.json` and `designer.json`, delete reports/system files and make every state an `OperatorStateResource`.

After those deletions, the alias fallback becomes the only path for every legacy name; no caller has to change.

---

## 8. Post-Landing Verification Audit (Step 8: PASSED)

In accordance with [`doc/meta/verification-protocol.md`](../../meta/verification-protocol.md), the orchestrator session conducted an active, independent 4-pillar verification audit of PR #125 landed at commit `589bfaf` on `main`:

### Pillar 1: Specification & Contract Conformance Matrix (Traceability Audit)
- **5-Scenario Catalog**: Verified `StandardScenarios.PRIMARY_SCENARIOS` and `pm-mock-runtime/src/main/resources/scenarios/primary.txt` expose exactly the 5 canonical multi-state scenarios (`onboarding`, `running-work`, `projects-and-start`, `reports-and-system`, `designer`).
- **Composite Resolution & Alias Fallback**: Verified `ScenarioRegistry` resolves composite `scenario#state` paths, validates state presence, and uses the legacy alias table as a fallback only when a legacy scenario file is absent, enabling zero-breakage downstream wave migrations.
- **Closed Schema Enforcement**: Verified `ScenarioLoader` rejects unknown fields across root, state, route, link, event, and auto levels with `ScenarioValidationException` citing the file and offending field. Verified `link.error` validation against `ApiErrorCode` and route codec validation for `/projects`, `/reports`, `/overview`, `/system`, `/workflows`.
- **Zero Stubs / Placeholders**: AST and diff refutation confirms zero temporary mock stubs, `UnsupportedOperationException`, or incomplete placeholder routes in `pm-mock-runtime`.

### Pillar 2: Architectural & Code Hygiene Anti-Pattern Scans
- **Scoped SSE Stream Integrity**: Verified `MockEventStream` scope discrimination (`machine`, `plan:<id>`, `epic:<id>`, `entry:<id>`, `workspace:<id>`, `reports`, `activity:<plan>:<task>`) and sequenced `EventEnvelope` construction. Verified 64-item ring buffer per scope with ordered `Last-Event-ID` replay and deterministic `event: reset` on stale/unknown IDs or resets.
- **Dispatch Precedence**: Verified state operator links and scenario routes take precedence over Java defaults outside mutating domain families; unoffered endpoints in inactive domains answer `404 mock_no_route` with available links.
- **`/mcp` JSON-RPC Endpoint**: Verified `POST /mcp` handler supporting `initialize`, `tools/list` with SSE chunked progress, `tools/call`, and notifications. Verified test migration in `PmMcpProcessIT` and `RelayTest` against live `MockRuntime`.
- **Test Suite Verification**: Ran `./mvnw test -pl pm-mock-runtime` — 340 tests passed, 0 failures, 0 errors.

### Pillar 3: Subprocess Lifecycle & Cross-Module Handshake Pre-Flight
- **Lifecycle Scripts Smoke Test**:
  - Tested `pm-mock-runtime/start.sh` with `-s running-work` and `-s onboarding`: clean background start, PID file creation in `target/pm-mock-runtime.pid`, log output in `target/pm-mock-runtime.log`.
  - Tested `pm-mock-runtime/stop.sh`: clean process termination, removal of PID file, zero zombie processes.
- **Network Handshake & Pairing**:
  - Bound to port 7420. Tested pairing handshake via `POST /api/v1/pair` with single-use pairing code `KXQB-MRTN` returning device secret.
  - Verified authenticated state transition: `POST /api/v1/onboarding/auto` transitioned state to `auto.consent` with offered links.
  - Tested SSE event stream `GET /api/v1/events` handshake: stream connected cleanly and received heartbeat/events.

### Pillar 4: Independent Diff & AST Verification (Adversarial Audit)
- **Attribution**: Commit `589bfaf` verified with mandatory trailer:
  `Co-Authored-By: plan-marshall <noreply@cuioss.de>`
- **Boundary Discipline**: Changes strictly confined to `pm-mock-runtime`, `pm-relay`, and `doc/`. No cross-module leakage into `pm-operator` or `pm-web-app`. Pre-existing tests and configurations preserved without weakening.
