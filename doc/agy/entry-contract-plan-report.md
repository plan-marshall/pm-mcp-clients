# Implementation Completion Report: `entry-contract` (Entry: Contract and Mock Runtime)

**Execution Date**: 2026-10-19
**Topic Branch**: `feat/entry-contract`
**Execution Plan**: [`doc/agy/implemented/entry-contract-plan.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/implemented/entry-contract-plan.md)
**Work Package**: [`doc/plans/implemented/entry-contract.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/plans/implemented/entry-contract.adoc)
**Governing Runbook**: [`doc/agy/runbook.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/runbook.md)

---

## 1. Executive Summary

The `entry-contract` work package delivers the client-side contract of the machine entry, pairing,
the loopback and LAN listeners, the web credential entry, and paired-device management in `pm-api`,
backed by the `entry-<case>` scenarios and handlers of `pm-mock-runtime`. It serves **UC-ENT-1**
(Enter and Connect), **UC-ENT-2** (Manage Paired Devices), and the web-credential slice of
**UC-ONB-8**.

The package closes the gaps left by the earlier end-to-end entry plans at the contract level:

- **The entry**: `RuntimeEntryResource` carries the runtime status the entry reads — version,
  lifecycle, host, the onboarding's progress, both listeners with their states, ports, URLs and the
  LAN certificate fingerprint, and the paired-device count.
- **The listeners**: the closed `ListenerState` catalogue (`open`, `closed`, `port_occupied`),
  `ListenerResource`, and a spec-faithful `listener.changed` payload (`listener`, `state`, `port`)
  replacing the earlier `enabled`/`url`/`fingerprint` shape.
- **Pairing**: `PairingCode` enforces the eight-character alphabet `BCDFGHJKLMNPQRSTVWXZ`,
  `PairingResource` is the answer of `POST /api/v1/web/pairings`, and the mock runtime consumes a
  code on redemption and issues a fresh one on the next request (`03-web-server.adoc#fresh-code`).
- **The devices**: `PairedDeviceResource` (with `paired_by`), `DeviceListResource`,
  `DeviceRenamedEvent`, and `DeviceRevokedEvent` carry the list, the rename, and the revocation
  shapes; the mock runtime serves the live list, broadcasts the events, and ends the device's
  stream with `stream.closed` / `device_revoked`.
- **The credential entry**: `WebCredentialResource` carries the entry status and the
  `loopback_only` rule without ever carrying the secret.
- **The loopback `Host`**: `LoopbackHost` accepts `127.0.0.1`, `localhost`, and `[::1]` with any
  port or none, and refuses foreign, zoned, IPv4-mapped, and malformed values.
- **The mock scenarios**: nine `entry-<case>` scenarios (un-onboarded, active onboarding, paired
  browser, SSH session, fresh code, LAN conflict, LAN credential refusal, device lifecycle, stream
  close), each loadable and asserted by `EntryScenarioTest`.

No client-module code (`pm-operator`, `pm-web-app`) was changed: this package owns the contract and
its mock, as the work package states.

---

## 2. Delivered Artifacts

### 2.1 `pm-api` — package `de.planmarshall.api.entry`

New records, values, and catalogues (each with `toMap`/`fromMap`/`toJson`/`fromJson`/`writeTo`/`readFrom` codecs):

- [`RuntimeEntryResource.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/entry/RuntimeEntryResource.java): the runtime-status slice of the entry, deriving legacy listeners from the `web` object when no `listeners` array is present.
- [`OnboardingProgress.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/entry/OnboardingProgress.java): state, step, steps, completion.
- [`ListenerState.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/entry/ListenerState.java): the closed `open`/`closed`/`port_occupied` list.
- [`ListenerResource.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/entry/ListenerResource.java): one listener with port, URL, and fingerprint.
- [`PairingCode.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/entry/PairingCode.java): grammar, normalization, display, generation, constant-time match.
- [`PairingResource.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/entry/PairingResource.java): code, URL, lifetime.
- [`PairedDeviceResource.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/entry/PairedDeviceResource.java): `device_id`, label, `created_at`, `last_used_at`, `paired_by`, `paired_from`.
- [`DeviceListResource.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/entry/DeviceListResource.java): reads the wire array and the object with a `devices` member.
- [`DeviceRenamedEvent.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/entry/DeviceRenamedEvent.java) and [`DeviceRevokedEvent.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/entry/DeviceRevokedEvent.java): `device_id`, `label`, `paired_from`.
- [`WebCredentialResource.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/entry/WebCredentialResource.java): the closed `present`/`not_found`/`stored` status and `keychain`/`secret-service`/`file` store, and the `loopback_only` flag.
- [`LoopbackHost.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/entry/LoopbackHost.java): the loopback `Host` validator of `03-web-server.adoc#loopback-host`.

Modified:

- [`ListenerChangedEvent.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/entry/ListenerChangedEvent.java): aligned to the Event Catalogue (`listener`, `state`, `port`).
- [`ListenerKind.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/entry/ListenerKind.java): added `fromWireOrNull` and `wireValues`.
- `PairingToken`, `PairingRedemptionRequest`, `PairingRedemptionResponse`, `LanListenerConfig`, `ConnectionStatus`, `DevicePairedEvent`: added the streaming `writeTo`/`readFrom` codecs.
- [`package-info.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/entry/package-info.java): now names the status, listeners, pairing, devices, and credential entry.

Tests:

- [`EntryRecordsTest.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/test/java/de/planmarshall/api/entry/EntryRecordsTest.java): round-trips (including streaming), legacy listener derivation, negative parsing, and `LoopbackHost`.
- [`EntryCataloguesTest.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/test/java/de/planmarshall/api/entry/EntryCataloguesTest.java): the closed listener, state, stream-close, pairing-code, error-code, and credential catalogues compared verbatim with the specification.

### 2.2 `pm-mock-runtime`

Scenarios (resources `scenarios/`):

- Enhanced [`entry.json`](file:///Users/oliver/git/pm-mcp-clients/pm-mock-runtime/src/main/resources/scenarios/entry.json): version, onboarding progress, LAN port, and the device fixture with `paired_by`.
- [`entry-unonboarded.json`](file:///Users/oliver/git/pm-mcp-clients/pm-mock-runtime/src/main/resources/scenarios/entry-unonboarded.json)
- [`entry-onboarding.json`](file:///Users/oliver/git/pm-mcp-clients/pm-mock-runtime/src/main/resources/scenarios/entry-onboarding.json)
- [`entry-paired-browser.json`](file:///Users/oliver/git/pm-mcp-clients/pm-mock-runtime/src/main/resources/scenarios/entry-paired-browser.json)
- [`entry-ssh-session.json`](file:///Users/oliver/git/pm-mcp-clients/pm-mock-runtime/src/main/resources/scenarios/entry-ssh-session.json)
- [`entry-fresh-code.json`](file:///Users/oliver/git/pm-mcp-clients/pm-mock-runtime/src/main/resources/scenarios/entry-fresh-code.json)
- [`entry-lan-conflict.json`](file:///Users/oliver/git/pm-mcp-clients/pm-mock-runtime/src/main/resources/scenarios/entry-lan-conflict.json)
- [`entry-lan-credentials.json`](file:///Users/oliver/git/pm-mcp-clients/pm-mock-runtime/src/main/resources/scenarios/entry-lan-credentials.json)
- [`entry-device-lifecycle.json`](file:///Users/oliver/git/pm-mcp-clients/pm-mock-runtime/src/main/resources/scenarios/entry-device-lifecycle.json)
- [`entry-stream-close.json`](file:///Users/oliver/git/pm-mcp-clients/pm-mock-runtime/src/main/resources/scenarios/entry-stream-close.json)

Handlers and registration:

- [`StandardScenarios.java`](file:///Users/oliver/git/pm-mcp-clients/pm-mock-runtime/src/main/java/de/planmarshall/mock/StandardScenarios.java): the `ENTRY_*` constants, `ENTRY_CASES`, and their registration in `ALL`.
- [`MockRuntimeEngine.java`](file:///Users/oliver/git/pm-mcp-clients/pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java):
  - scenario-seeded paired devices and LAN listener state;
  - single-use pairing code and deterministic fresh-code generation;
  - `pairing_invalid` on a wrong, consumed, or absent code;
  - `listener.changed` with `state` and `port`, including `port_occupied`;
  - `device.renamed` / `device.revoked` with `paired_from`, and `stream.closed` with the catalogued reasons;
  - `paired_by` in the device list and the rename answer;
  - a scenario override for the LAN open (the `web_listener_conflict` case) and for the LAN credential refusal (`loopback_only`);
  - the `listeners` array and `lan_port` in the runtime status.

Tests:

- [`EntryScenarioTest.java`](file:///Users/oliver/git/pm-mcp-clients/pm-mock-runtime/src/test/java/de/planmarshall/mock/EntryScenarioTest.java): 24 cases, including one per scenario, the fresh-code cycle, the LAN conflict, the credential rule on both listeners, the device lifecycle, and the stream termination.
- [`MockRuntimeEngineTest.java`](file:///Users/oliver/git/pm-mcp-clients/pm-mock-runtime/src/test/java/de/planmarshall/mock/runtime/MockRuntimeEngineTest.java): updated for the single-use code and `pairing_invalid`.

### 2.3 Documentation

- Plan moved to [`doc/agy/implemented/entry-contract-plan.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/implemented/entry-contract-plan.md).
- Work package moved to [`doc/plans/implemented/entry-contract.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/plans/implemented/entry-contract.adoc) and marked `done` in [`doc/plans/README.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/plans/README.adoc).
- Archive index updated in [`doc/agy/implemented/README.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/implemented/README.md).

---

## 3. Design Decisions & Reconciliation

| Decision | Rationale |
|---|---|
| `listener.changed` now carries `listener`, `state`, `port` | `06-event-catalogue.adoc#listener.changed` defines exactly these three members; the earlier `enabled`/`url`/`fingerprint` shape disagreed with the catalogue. |
| The pairing code is consumed on redemption and the next request issues a fresh one | `03-web-server.adoc#fresh-code` and the single-use rule of `#web-pairing`. The runtime's transcript scenario keeps its initial code until it is redeemed, so `web pair` remains stable. |
| A credential is accepted on the mock's web listener (loopback) and refused on the network listener through a scenario override | The mock serves one loopback TCP listener; `entry-lan-credentials` represents a request that arrived on the LAN listener with the `loopback_only` refusal. |
| Scenario fixtures seed devices and the LAN state through the existing `GET /api/v1/web/devices` / `GET /api/v1/status` route entries | Keeps the closed scenario schema (`format_version: 1`) unchanged while letting each `entry-<case>` start in its own state. |
| The LAN open and credential routes can be overridden by a scenario route | Lets the mock express a refused open (`web_listener_conflict`) and a listener-restricted refusal (`loopback_only`) without inventing a second TLS listener. |

---

## 4. Test & Verification Results

| Module | Tests run | Failures | Errors | Skipped |
|---|---|---|---|---|
| `pm-api` | 535 | 0 | 0 | 0 |
| `pm-mock-runtime` | 242 | 0 | 0 | 0 |

Beyond the new contract tests, the full `pm-api`, `pm-mock-runtime`, and `pm-operator` suites were
run to prove that the corrected `listener.changed` shape, the single-use code, and the credential
rule do not regress an existing client.

---

## 5. Domain Invariants

- **Java 25 records and closed enums**: every new contract type is an immutable record or a closed
  enum; no Lombok, no mutable state.
- **Jackson streaming only**: codecs use `JsonTree` on `jackson-core`; `jackson-databind` is not
  used.
- **Closed catalogues fail closed**: unknown listener kinds, states, stream-close reasons,
  credential statuses/stores, and codes are refused (`IllegalArgumentException` or
  `JsonFormatException`), and `EntryCataloguesTest` compares every list with the specification.
- **No secret leaves the runtime**: `WebCredentialResource` carries status and store, never the
  secret; `PairingResource` carries the code, never the device secret.
- **Loopback trust boundary**: `LoopbackHost` accepts only the listener's own origin; the
  credential rule is expressed as `loopback_only` on the network listener.
- **Dependency boundaries**: `pm-api` and `pm-mock-runtime` keep their allowed dependencies; the
  enforcer rules `clients-depend-on-pm-api-only` and `jackson-core-only` passed.

---

## 6. Build & Quality Evidence

`./mvnw verify -Ppre-commit` completed with **BUILD SUCCESS** across all eight reactor modules
(`pm-mcp-clients`, `pm-api`, `pm-exec`, `pm-relay`, `pm-mock-runtime`, `pm-operator`, `pm-web-app`,
`pm-web-e2e`) in 4 min 53 s. The gate exercised OpenRewrite, licence headers, Checkstyle, the
enforcer rules, Javadoc validation, the full Java test suite, the web app's npm build/Vitest/ESLint,
and the E2E lint pass. `git status` showed only the intended code, scenario, test, and documentation
changes after the gate.
