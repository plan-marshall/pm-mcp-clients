# Completion Report: `operator-harness-and-roles-verbs`

Operator verbs of harness enrolment and roles (Roadmap Milestone 4), implemented in `pm-operator` and `pm-api` under [`doc/agy/runbook.md`](runbook.md).

---

## 1. Executive Summary

This work package delivers the operator CLI verbs for harness enrolment and role management, backed by strict `pm-api` contract records and guarded by the `OperatorGate` (`PM-SEC-3`):

* `pm-operator enrol-harness <client>` resolves the headless CLI of a client profile (`claude`, `opencode`, `codex`, `antigravity`) through the absolute entries of the login `PATH`, computes its SHA-256 digest, runs the profile's version probe with a bounded timeout, a bounded output, and a minimal environment, and shows path, version, and digest side by side with any recorded values before the operator confirms. Nothing is recorded before the confirmation; `--yes` skips the confirmation but never the operator gate. A profile without a version probe (antigravity) is recorded with the version `unknown` instead of running an unsupported flag.
* `pm-operator roles list|show|qualify` reads the effective role set and shows the source set, class, form, effective harness/model/effort with the level each comes from, the qualification state, and whether the start-failure guard holds; `qualify` shows the configuration, corpus topics, item counts, and the expected tokens and time first and runs only after confirmation.

The resource contracts (`HarnessEnrolmentResource`, `RoleSummaryResource`, `RoleDetailResource`, and the closed `RoleQualificationState`) live in `pm-api:de.planmarshall.api.resource` with `toMap`/`toJson`/`writeTo`/`fromMap`/`fromJson`/`readFrom` codecs on `jackson-core` (never `jackson-databind`), fail-closed validation at the wire boundary, and bilingual catalogue entries (EN & DE).

The importable API these verbs consume (`harness-and-roles-api` in `plan-marshall-mcp`, module `pm-mcp-server` package `mcp.server.operator`) records the authoritative enrolment and writes the qualification records; this package owns the client side of that contract and its resource records, which the runtime side consumes from `pm-api`.

---

## 2. Delivered Artifacts

### `pm-api` — resource contracts (`de.planmarshall.api.resource`)

| File | Content |
|---|---|
| `HarnessEnrolmentResource.java` (new) | Client id, normalized absolute executable path, version, lowercase-hex SHA-256 digest, and a closed `Status` (`detected`, `enrolled`, `updated`). Streaming codecs and fail-closed validation (absolute path, closed client-id pattern, 64 lowercase hex characters). |
| `RoleQualificationState.java` (new) | Closed enum `qualified`, `qualified_fresh_only`, `unqualified`, `invalid`, with `wire()`, `fromWire`, `requireWire`, `wireValues`. |
| `RoleSummaryResource.java` (new) | Role, display name, description, capabilities, source set, class, form, effective harness/model/effort with their levels, qualification state, start-failure guard; tolerant list reader accepting a bare array or `{"roles": […]}`. |
| `RoleDetailResource.java` (new) | Role, display name, description, advisory tool set, narrowed effort, qualification (state, reason, criteria), and configuration parameters (deep-copied and JSON-normalized); nested `NarrowedEffort` and `Qualification` records. |
| `messages/enrolment.{en,de}.properties` (modified) | 19 new `enrolment.harness.*` keys for the side-by-side table, confirmation, success, and refusal messages. |
| `messages/operator.{en,de}.properties` (modified) | 46 new `operator.roles.*` keys for list headers/rows, show detail, and qualify preview/measurements. |

### `pm-operator` — verbs

| File | Content |
|---|---|
| `HarnessDiscovery.java` (new) | Client catalogue and version probe read from `HostDetection` (the shipped profiles' single source); resolves the executable through the absolute entries of `PATH` (relative entries are never executed), resolves symlinks, computes the SHA-256 digest before and after the probe (a binary that changes while probed is refused), and runs the version probe with a 10-second timeout, an 8 KiB output bound, a bounded reader, and a minimal allowlisted environment. A profile that declares no version probe (antigravity) is recorded with the version `unknown`. Reports not-found and probe-failure as distinct exceptions. |
| `EnrolHarnessCommand.java` (new) | `enrol-harness <client> [-y\|--yes] [--no-input]`. Gate first, then discovery, then the recorded/detected comparison table, then confirmation (unless `--yes`), then `POST /api/v1/harnesses/{client}/enrol` with the detected resource. A recorded read that is not `404` is refused; `--no-input` without `--yes` exits `3`; unknown client exits `2`; not-found/probe/refusal exit `1`; `PM-SEC-3` exits `4`. |
| `RolesCommand.java` (new) | `roles list [--project]`, `roles show <role> [--project]`, `roles qualify <role> [--project] [--yes] [--no-input]`. `list` renders the effective role set; `show` renders the advisory tool set, narrowed effort, qualification, and configuration parameters; `qualify` previews the configuration and corpus facts, confirms (unless `--yes`), posts `POST /api/v1/roles/{role}/qualify` with an idempotency key, prints the measurements and record path, treats `202` as still running, and refuses a successful status without usable measurements. Runtime-provided strings are stripped of terminal control sequences before they are printed. |
| `PmOperator.java` (modified) | Registers `EnrolHarnessCommand` and `RolesCommand` in the command tree. |

### Tests

| File | Content |
|---|---|
| `pm-api/.../resource/HarnessRolesResourcesTest.java` (new) | 11 tests: JSON and streaming round-trips for all three records, path normalization, digest/role validation, wire-boundary refusals, list parsing, parameter normalization/immutability, and the closed state catalogue. |
| `pm-api/.../message/OperatorRolesCatalogueTest.java` (new) | EN/DE key parity, required-role-key coverage, bilingual formatting. |
| `pm-api/.../message/EnrolCatalogueTest.java` (modified) | Adds the 19 harness keys and bilingual formatting assertions. |
| `pm-operator/.../HarnessDiscoveryTest.java` (new) | 8 tests: absolute PATH resolution and digest equality, relative-entry refusal, missing executable, failing/empty probe, first-line fallback, the antigravity no-probe path, the restricted probe environment, and the closed client catalogue. |
| `pm-operator/.../EnrolHarnessCommandTest.java` (new) | 11 tests: discovery and record flow, request body, recorded/detected comparison and re-baseline, declined/interactive/`--no-input` confirmation, unknown client, missing executable, failing probe, unsupported version, a non-`404` recorded read, and the `PM-SEC-3` coding-agent refusal (with a marker proving the probe never ran). |
| `pm-operator/.../RolesCommandTest.java` (new) | 12 tests: list rendering, project query, empty set, refusal, show detail, missing role, qualify preview and measurements (with the idempotency key), a `202` still-running answer, an unusable completion body, confirmation paths, and refusal reason. |
| `pm-operator/.../PmOperatorProcessIT.java` (modified) | Adds `enrol-harness`/`roles` coding-agent host refusals and process-level terminal-refusal tests asserting exit code `4` and empty stdout. |

### Documentation

| File | Content |
|---|---|
| `doc/agy/operator-harness-and-roles-verbs-plan-report.md` (new) | This report. |
| `doc/agy/implemented/operator-harness-and-roles-verbs-plan.md` (moved) | Archived plan with the workflow checklist. |
| `doc/agy/implemented/README.md` (modified) | Records the implemented plan. |
| `doc/plans/README.adoc` (modified) | Marks `operator-harness-and-roles-verbs` as `done` and points at `implemented/`. |
| `doc/plans/implemented/operator-harness-and-roles-verbs.adoc` (moved) | Archived work package. |

---

## 3. Trace Matrix

| Requirement / Specification | Destination |
|---|---|
| `PM-WORK-1` Roles as configuration | `RoleSummaryResource`, `RoleDetailResource`, `RolesCommand.ListCommand`, `RolesCommand.ShowCommand` |
| `PM-WORK-6` Role qualification | `RoleQualificationState`, `RolesCommand.QualifyCommand`, `operator.roles.qualify.*` catalogue keys |
| `PM-SEC-3` Operator channel isolation | `OperatorGate` enforcement in `EnrolHarnessCommand.call` and every `RolesCommand` subcommand; `PmOperatorProcessIT` exit-code-4 tests |
| `PM-EXEC-1` Harness enrolment | `HarnessEnrolmentResource`, `HarnessDiscovery`, `EnrolHarnessCommand` |
| CLI & Security § Harness Enrolment | Side-by-side path/version/digest display, confirmation, `--yes`, `--no-input` behaviour, `harness_version_unsupported` handling |
| CLI & Security § Roles | `roles list\|show\|qualify` output, advisory tool set, narrowed effort, qualification state, read-only `list`/`show`, confirmed `qualify` |
| CLI & Security § Command Tree | Verbs registered at the top level of `pm-operator` without an `operator` sub-level |

---

## 4. Test & Verification Results

Targeted unit runs while implementing:

```
pm-api     HarnessRolesResourcesTest        10/10
pm-api     EnrolCatalogueTest                3/3
pm-api     OperatorRolesCatalogueTest        3/3
pm-operator EnrolHarnessCommandTest         10/10
pm-operator RolesCommandTest                10/10
pm-operator HarnessDiscoveryTest             6/6
```

Full module suites before the pre-commit gate:

```
pm-api,pm-operator   Tests run: 443, Failures: 0, Errors: 0, Skipped: 0
```

Process integration tests (targeted, `-Pintegration-tests`):

```
pm-operator as a process   Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
  enrolHarnessRefusesWithoutTerminal
  rolesRefusesWithoutTerminal
  refusesCodingAgentHostSignals (now also drives enrol-harness and roles)
```

Full repository pre-commit gate:

```
./mvnw verify -Ppre-commit
[INFO] BUILD SUCCESS
```

The gate ran the OpenRewrite recipes (formatting and unused-import/fully-qualified-name clean-ups applied and committed), license headers, the enforcer dependency rules, Javadoc generation, and the complete unit test suite across the reactor.

---

## 5. Domain Invariants

* **Java 25**: All new code compiles on the repository's Java 25 release with `jspecify` nullness annotations and record patterns.
* **No `jackson-databind`**: The new records use `jackson-core` streaming only; `ApiContractTest.noJacksonDatabind` continues to guard the classpath, and `HarnessRolesResourcesTest` exercises `writeTo`/`readFrom`.
* **Bilingual catalogue**: Every new user-facing string is a key in both `en` and `de` packs; `OperatorRolesCatalogueTest` asserts exact key parity and `EnrolCatalogueTest` asserts the harness keys and bilingual formatting.
* **`PM-SEC-3` gate**: `EnrolHarnessCommand` and all `RolesCommand` subcommands call `OperatorGate.verify()` first; the process ITs assert exit `4` with empty stdout for a non-interactive terminal and for every coding-agent host signal.
* **Fail-closed wire boundary**: Unknown status/state/qualification values, relative paths, malformed digests, missing or non-boolean guard values, malformed tool/capability arrays, and out-of-pattern role/client identifiers are refused with `JsonFormatException` or `IllegalArgumentException`; a `roles`/`qualify` success body without usable measurements is a failure.
* **No capture of harness secrets**: neither the resource nor the discovery stores an environment value or credential; the probe runs with a small allowlisted environment so a probed executable cannot read an inherited secret, and only path, version, and digest are transmitted.
* **No uncatalogued strings**: the new verbs take their texts from the `pm-api` message catalogue.

---

## 6. Endpoint Contract Assumed by the Client

The runtime endpoints are owned by the parallel `harness-and-roles-api` package of `plan-marshall-mcp`. The client side uses:

* `GET /api/v1/harnesses/{client_id}` — the recorded `HarnessEnrolmentResource`, or `404` when none is recorded.
* `POST /api/v1/harnesses/{client_id}/enrol` — records the enrolment and answers the recorded resource.
* `GET /api/v1/roles` (optional `?project=<project_id>`) — a role list (a bare array or `{"roles": […]}`).
* `GET /api/v1/roles/{role}` (optional `?project=`) — a `RoleDetailResource`.
* `POST /api/v1/roles/{role}/qualify` (optional `?project=`) — runs the grading and answers the measurements; carries an `Idempotency-Key` header, and a `202` is shown as still running.

The request and response bodies are the `pm-api` records above, so both sides share one schema; the endpoint paths are documented here for the runtime side to align with. No `pm-mock-runtime` scenario was added because this package owns `pm-operator` and `pm-api` only and the module matrix marks `pm-mock-runtime` as a read-only test fixture; the verbs are exercised against `FakeRuntimeServer` and the process ITs.

---

## 7. Build & Quality Evidence

* `./mvnw verify -Ppre-commit` — `BUILD SUCCESS`, no uncommitted reformatted sources after the OpenRewrite run.
* `./mvnw verify -pl pm-operator -am -Pintegration-tests -Dit.test=…` — targeted process ITs green.
* Zero test failures across `pm-api` and `pm-operator`.
* Human confirmation gates default to No and never send a request when declined or when `--no-input` is set without `--yes`.

---

## 8. Notes for the Orchestrator (Step 8)

* The initial plan has been moved to `doc/agy/implemented/operator-harness-and-roles-verbs-plan.md`; this report intentionally remains in `doc/agy/` until post-landing verification moves it to `doc/agy/verified/`.
* `doc/plans/operator-harness-and-roles-verbs.adoc` is archived under `doc/plans/implemented/` and its row in `doc/plans/README.adoc` is `done`.
* The runtime-side `harness-and-roles-api` package should confirm the five endpoint paths listed in §6; a mismatch is a client-contract change confined to the URL constants of `EnrolHarnessCommand` and `RolesCommand`.
