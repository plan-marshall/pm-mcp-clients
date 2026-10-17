# Implementation Completion Report: UC-ENT-1 Enter and Connect (Pairing, Listeners, Device Handshake)

**Execution Date**: 2026-10-17
**Topic Branch**: `feat/uc-ent-1-connect`
**Execution Plan**: [`doc/agy/implemented/uc-ent-1-plan.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/implemented/uc-ent-1-plan.md)
**Governing Runbook**: [`doc/agy/runbook.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/runbook.md)
**Implementation Recipe**: [`doc/meta/implement_use-cases.md`](file:///Users/oliver/git/pm-mcp-clients/doc/meta/implement_use-cases.md)

---

## 1. Executive Summary

Use case **UC-ENT-1: Enter and Connect** (pairing, the loopback and LAN listeners, the connection status, the device handshake, and the fresh code after a redemption) has been completed across `pm-api`, `pm-mock-runtime`, `pm-operator`, and `pm-web-app`, together with the loopback-only credential rule of **UC-ONB-8**, and covered by `pm-web-e2e`.

A substantial part of the entry already existed and was verified (the pairing page `PairingView.svelte`, `web-pairing.json`, `WebCommand` pairing/QR, `EntryFlow`, the paired-devices contract). This plan closed the remaining gaps against the design and specification, which are the source of truth:

- **Entry contract package** (`de.planmarshall.api.entry`): `PairingToken`, `PairingRedemptionRequest`, `PairingRedemptionResponse`, `LanListenerConfig`, `ConnectionStatus`, `DevicePairedEvent`, `ListenerChangedEvent`, and the closed `ListenerKind` list, each with a codec round-trip test.
- **The two SSE events the entry depends on**: `device.paired` (with `paired_from`) and `listener.changed`, now emitted by the mock runtime and consumed by both clients — the terminal console asks for a **fresh code** on `device.paired` (`03-web-server.adoc#fresh-code`).
- **The LAN listener beside loopback**: `PUT /api/v1/web/listeners/lan` returns the certificate fingerprint and refuses a colliding port with `web_listener_conflict`; the CLI reports the LAN URL and fingerprint on `web enable --lan`.
- **CLI-only and loopback-only boundaries**: `POST /api/v1/web/pairings` and the LAN-listener open refuse a web origin with `cli_only`; `PUT /api/v1/credentials/{key}` refuses the LAN listener with `loopback_only` (UC-ONB-8).
- **Web client**: `ConnectionStatusBanner.svelte` (loopback vs network indicator), `LanListenerModal.svelte` (fingerprint and turning the network access off), and `CredentialEntry.svelte` (loopback-only credential entry), with Vitest coverage.
- **Bilingual catalogue**: `messages/entry.{en,de}.properties`, registered as a supported area.

### Design-faithful reconciliation (recorded deviation)

The plan named several artifacts that diverge from the repository's already-verified design and the canonical command tree (`doc/design/terminal-client/01-commands.adoc`). Per the operator's decision, the implementation is **hybrid/design-faithful**: it follows the design and the canonical verbs, and creates the plan-named files only where they are new and non-conflicting.

| Plan artifact | Delivered as |
|---|---|
| `de.planmarshall.api.entry.*` records | Created as named (new package). |
| `messages/entry.{en,de}.properties` | Created as named; pairing texts stay in `pairing.*` per the design. |
| `scenarios/entry.json` | Created as named. |
| `entry.spec.js` | Created as named. |
| `PairingModal.svelte` | Not duplicated: the pairing page already exists as `PairingView.svelte` (`/pair`). |
| `ConnectionStatusBanner.svelte`, `LanListenerModal.svelte` | Created as named (new). |
| `EnterCommand.java`, `PairCommand.java`, `listeners lan`, `status connection` | Not created: the canonical tree already provides `web open`/`web pair` (the entry), `web enable --lan` (open the LAN listener), `web devices listener-off` (close it). The capabilities were added to those verbs instead. |
| `PmOperatorProcessIT.java` entry case | Not added: operator verbs require an interactive terminal (`PM-SEC-3`), so `web enable --lan` is covered by a unit test rather than a no-TTY process test. |

Deferred (out of the delivered slice, owned by open packages `onboarding-contract`/`web-onboarding`): the full web onboarding credentials workflow beyond the loopback-only boundary, and rendering the LAN fingerprint on the unauthenticated pairing page (the fingerprint is surfaced in the terminal and after pairing via the connection banner/modal).

---

## 2. Delivered Artifacts

### 2.1 `pm-api` (Contracts, Events & Message Catalogue)
- [`entry/package-info.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/entry/package-info.java)
- [`ListenerKind.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/entry/ListenerKind.java): the closed listener list (`loopback`, `lan`).
- [`PairingToken.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/entry/PairingToken.java), [`PairingRedemptionRequest.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/entry/PairingRedemptionRequest.java), [`PairingRedemptionResponse.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/entry/PairingRedemptionResponse.java): the pairing handshake.
- [`LanListenerConfig.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/entry/LanListenerConfig.java), [`ConnectionStatus.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/entry/ConnectionStatus.java): listener and connection state.
- [`DevicePairedEvent.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/entry/DevicePairedEvent.java), [`ListenerChangedEvent.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/entry/ListenerChangedEvent.java): the two SSE events.
- [`entry.en.properties`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/resources/messages/entry.en.properties) & [`entry.de.properties`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/resources/messages/entry.de.properties): bilingual catalogue.
- [`MessageCatalogue.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/message/MessageCatalogue.java): registered the `entry` area.
- **Tests**: [`EntryRecordsTest.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/test/java/de/planmarshall/api/entry/EntryRecordsTest.java) (codec round-trips, closed list) and [`EntryCatalogueTest.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/test/java/de/planmarshall/api/message/EntryCatalogueTest.java) (EN/DE parity, required keys, ICU formatting).

### 2.2 `pm-mock-runtime` (Scenarios & Dynamic Handlers)
- [`entry.json`](file:///Users/oliver/git/pm-mcp-clients/pm-mock-runtime/src/main/resources/scenarios/entry.json): the entry scenario (machine not onboarded, pairing, network listener with fingerprint).
- [`StandardScenarios.java`](file:///Users/oliver/git/pm-mcp-clients/pm-mock-runtime/src/main/java/de/planmarshall/mock/StandardScenarios.java): `ENTRY = "entry"` registered in `ALL`.
- [`MockRuntimeEngine.java`](file:///Users/oliver/git/pm-mcp-clients/pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java): CLI-only guard on `POST /api/v1/web/pairings`; `PUT /api/v1/web/listeners/lan` with fingerprint and `web_listener_conflict`; `GET /api/v1/status/connection`; `loopback_only` on `PUT /api/v1/credentials/{key}`; `device.paired` on redemption; `listener.changed` on open/close.
- **Tests**: [`EntryScenarioTest.java`](file:///Users/oliver/git/pm-mcp-clients/pm-mock-runtime/src/test/java/de/planmarshall/mock/EntryScenarioTest.java) (7 tests) and updated [`StandardScenariosTest.java`](file:///Users/oliver/git/pm-mcp-clients/pm-mock-runtime/src/test/java/de/planmarshall/mock/StandardScenariosTest.java).

### 2.3 `pm-operator` (Terminal Client)
- [`PmConsole.java`](file:///Users/oliver/git/pm-mcp-clients/pm-operator/src/main/java/de/planmarshall/operator/PmConsole.java): recognizes the `device.paired` event name and asks for a fresh code above the prompt (`03-web-server.adoc#fresh-code`).
- [`WebCommand.java`](file:///Users/oliver/git/pm-mcp-clients/pm-operator/src/main/java/de/planmarshall/operator/WebCommand.java): `web enable --lan` reports the LAN URL and certificate fingerprint from `GET /api/v1/status/connection`.
- **Tests**: extended [`PmOperatorTest.java`](file:///Users/oliver/git/pm-mcp-clients/pm-operator/src/test/java/de/planmarshall/operator/PmOperatorTest.java) (`enable --lan` fingerprint case).

### 2.4 `pm-web-app` (Web Client)
- [`ConnectionStatusBanner.svelte`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/lib/components/ConnectionStatusBanner.svelte): loopback vs network indicator, refreshed on `listener.changed`; wired into [`+layout.svelte`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/routes/+layout.svelte).
- [`LanListenerModal.svelte`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/lib/components/LanListenerModal.svelte): network access state, certificate fingerprint, and turning the network access off.
- [`CredentialEntry.svelte`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/lib/components/CredentialEntry.svelte): loopback-only credential entry (UC-ONB-8), naming `pm-operator credentials set <key>` on the network.
- [`SettingsView.svelte`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/lib/views/SettingsView.svelte): network-access details button and the credential entry section.
- [`client.ts`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/lib/api/client.ts): `getConnectionStatus()`, `setCredential()`.
- [`i18n/index.ts`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/lib/i18n/index.ts): entry fallback messages.
- **Vitest tests**: [`ConnectionStatusBanner.test.ts`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/lib/components/ConnectionStatusBanner.test.ts), [`LanListenerModal.test.ts`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/lib/components/LanListenerModal.test.ts), [`CredentialEntry.test.ts`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/lib/components/CredentialEntry.test.ts), and an added case in [`SettingsView.test.ts`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/lib/views/SettingsView.test.ts).

### 2.5 `pm-web-e2e` (Playwright E2E)
- [`entry.spec.js`](file:///Users/oliver/git/pm-mcp-clients/pm-web-e2e/tests/entry.spec.js): connection banner in loopback mode, network-access modal with fingerprint, credential entry on loopback, and the pairing page for an unpaired browser.

---

## 3. Verification & Test Evidence

| Module / Scope | Test Suite | Tests Run | Pass Rate |
|---|---|---|---|
| `pm-api` | `EntryRecordsTest`, `EntryCatalogueTest`, catalogue & record suites | 250 | 100% |
| `pm-exec` | (unchanged) | 71 | 100% |
| `pm-relay` | (unchanged) | 25 | 100% |
| `pm-operator` | `PmOperatorTest`, `PmConsoleTest`, `EntryFlowTest`, `WebDevicesCommandTest`, etc. | 338 | 100% |
| `pm-mock-runtime` | `EntryScenarioTest`, `PairedDevicesScenarioTest`, `MockRuntimeEngineTest`, `StandardScenariosTest`, etc. | 142 | 100% |
| `pm-web-app` | Vitest (`ConnectionStatusBanner`, `LanListenerModal`, `CredentialEntry`, `SettingsView`, suite) | 102 | 100% |
| `pm-web-e2e` | ESLint + Prettier (`entry.spec.js`) | n/a | Pass |

Playwright E2E is skipped by default in the pre-commit profile (`skipPlaywrightTests=true`) and runs under `-Pe2e`.

---

## 4. Build & Quality Evidence

- **Pre-commit gate**: `./mvnw verify -Ppre-commit` → **BUILD SUCCESS** for all eight reactor modules (`pm-mcp-clients`, `pm-api`, `pm-exec`, `pm-relay`, `pm-operator`, `pm-mock-runtime`, `pm-web-app`, `pm-web-e2e`), total time ≈ 3 min 31 s. This exercised OpenRewrite, licence-header checks, Checkstyle/Enforcer (`clients-depend-on-pm-api-only`, `no-model-facing-content`), Javadoc validation, the full Java test suite, the `pm-web-app` npm build + Vitest + ESLint, and the `pm-web-e2e` ESLint pass.
- **No unexpected rewrites**: after the gate, `git status` showed only the intended new/modified files.
- **Web client**: `npm run test:ci` → 21 files, 102 tests passed; `npm run lint` clean.

---

## 5. Architecture Invariants Verification

- **Pure Java 25 records**: all new `de.planmarshall.api.entry` types are immutable records or a closed enum.
- **Jackson streaming only**: codecs use `JsonTree` (`JsonParser`/`JsonGenerator`); no `jackson-databind`.
- **No uncatalogued strings**: new user-facing texts live in `entry.{en,de}.properties` and are read through `MessageCatalogue` (CLI) and `$t(...)` (web).
- **Strict dependency boundaries**: `pm-operator` and `pm-mock-runtime` depend on `pm-api` only; the enforcer rule passed with no violations.
- **Both clients, one state**: the `device.paired` and `listener.changed` events are emitted once by the runtime and observed by the terminal console and the browser; the mock scenario serves both.
- **Security boundaries (persona-security-expert lens)**: pairing issuance and the LAN-listener open are CLI-only (`cli_only`); credential entry is loopback-only (`loopback_only`); the LAN fingerprint is shown to allow the operator to compare the self-signed certificate.

---

## 6. Continuous Improvement Retrospective

- **Plan vs. reality**: the plan named CLI verbs and a package that had partly diverged from the already-verified design. Recording an explicit "plan → delivered" mapping up front (as in §1) avoided duplicating the canonical command tree.
- **Test-count pins**: `StandardScenariosTest` pins `ALL.size()`; adding a scenario requires updating it. A future refinement could assert the set of names instead of a count.
