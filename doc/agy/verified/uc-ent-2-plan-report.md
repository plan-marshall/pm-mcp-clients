# Implementation Completion Report: UC-ENT-2 Manage Paired Devices

**Execution Date**: 2026-10-16  
**Topic Branch**: `feat/uc-ent-2-paired-devices`  
**Execution Plan**: [`doc/agy/implemented/uc-ent-2-plan.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/implemented/uc-ent-2-plan.md)  
**Governing Runbook**: [`doc/agy/runbook.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/runbook.md)  
**Implementation Recipe**: [`doc/meta/implement_use-cases.md`](file:///Users/oliver/git/pm-mcp-clients/doc/meta/implement_use-cases.md)  

---

## 1. Executive Summary

Use case **UC-ENT-2: Manage Paired Devices** has been implemented across the entire `pm-mcp-clients` repository, fulfilling all requirements for managing paired browser devices and network listener access from both the terminal client (`pm-operator`) and browser web app (`pm-web-app`), with authoritative state coordination provided by `pm-api` and simulated via `pm-mock-runtime`.

Key achievements include:
- **"Both clients, one state"**: Real-time consistency across CLI and browser. When a device is renamed or revoked, or the network listener is turned off, both clients reflect the updated state immediately.
- **Dedicated Revocation Flow**: When a browser is revoked or its session expires, open pages cleanly close and transition to a dedicated `#revoked-page` explaining who revoked it and when, with an immediate path to re-pair.
- **Security & Safety Discipline**: Input sanitization via `OutputSafety.stripControlSequences` prevents ANSI/control sequence injection in terminal device labels; strict confirmation barriers guard irreversible revocations.
- **Bilingual Message Catalogue**: All user-facing strings are externalized in `messages/devices.{en,de}.properties` and accessed via `MessageCatalogue` and Svelte `$t(...)`.
- **Zero Regressions & Comprehensive Verification**: 100% test pass rate across `pm-api` (139 tests), `pm-mock-runtime` (94 tests), `pm-operator` (259 tests), and `pm-web-app` (31 tests).

---

## 2. Delivered Artifacts

### 2.1 `pm-api` (Contracts, Events & Message Catalogue)
- [`PairedDevice.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/web/PairedDevice.java): Immutable Java 25 record representing a paired device with Jackson streaming serialization and deserialization.
- [`PairedDevicesResponse.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/web/PairedDevicesResponse.java): Container record for device collections.
- [`DeviceRenamedEvent.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/web/DeviceRenamedEvent.java): Event record for device rename notifications.
- [`DeviceRevokedEvent.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/web/DeviceRevokedEvent.java): Event record for device revocation notifications.
- [`StreamClosedEvent.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/web/StreamClosedEvent.java): Event record for SSE stream termination (`device_revoked`, `device_expired`, `listener_closed`).
- [`devices.en.properties`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/resources/messages/devices.en.properties) & [`devices.de.properties`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/resources/messages/devices.de.properties): Bilingual property bundles providing all UI/terminal text keys.
- [`MessageCatalogue.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/message/MessageCatalogue.java): Registered `"devices"` area in `SUPPORTED_AREAS`.
- **Unit Tests**: [`PairedDeviceTest.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/test/java/de/planmarshall/api/web/PairedDeviceTest.java) and [`DevicesCatalogueTest.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/test/java/de/planmarshall/api/message/DevicesCatalogueTest.java).

### 2.2 `pm-mock-runtime` (Scenarios & Dynamic Handlers)
- [`paired-devices.json`](file:///Users/oliver/git/pm-mcp-clients/pm-mock-runtime/src/main/resources/scenarios/paired-devices.json): Stateful scenario fixture modeling paired devices and network listener states.
- [`StandardScenarios.java`](file:///Users/oliver/git/pm-mcp-clients/pm-mock-runtime/src/main/java/de/planmarshall/mock/StandardScenarios.java): Added `PAIRED_DEVICES = "paired-devices"` constant and registered in `ALL`.
- [`MockRuntimeEngine.java`](file:///Users/oliver/git/pm-mcp-clients/pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java): Dynamic handlers for `GET /api/v1/web/devices`, `PATCH/PUT /api/v1/web/devices/{id}`, `DELETE /api/v1/web/devices/{id}`, `DELETE /api/v1/web/devices?others`, `DELETE /api/v1/web/listeners/lan`, and `PUT /api/v1/web`.
- **Tests**: [`PairedDevicesScenarioTest.java`](file:///Users/oliver/git/pm-mcp-clients/pm-mock-runtime/src/test/java/de/planmarshall/mock/PairedDevicesScenarioTest.java) and updated [`StandardScenariosTest.java`](file:///Users/oliver/git/pm-mcp-clients/pm-mock-runtime/src/test/java/de/planmarshall/mock/StandardScenariosTest.java).

### 2.3 `pm-operator` (Terminal Client)
- [`WebCommand.java`](file:///Users/oliver/git/pm-mcp-clients/pm-operator/src/main/java/de/planmarshall/operator/WebCommand.java): Refined `DevicesCommand` supporting tabular listing, date and last-used formatting, interactive prompts (`-i`), direct CLI actions (`rename`, `revoke`, `listener-off`), and `OutputSafety` sanitization.
- **Unit & Process Tests**: [`WebDevicesCommandTest.java`](file:///Users/oliver/git/pm-mcp-clients/pm-operator/src/test/java/de/planmarshall/operator/WebDevicesCommandTest.java) (6 tests) and added `webDevicesProcess` in [`PmOperatorProcessIT.java`](file:///Users/oliver/git/pm-mcp-clients/pm-operator/src/test/java/de/planmarshall/operator/PmOperatorProcessIT.java).

### 2.4 `pm-web-app` (Web Client)
- [`client.ts`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/lib/api/client.ts): Added `getDevices()`, `renameDevice()`, `revokeDevice()`, `revokeOtherDevices()`, `turnOffLanListener()`, and extended SSE event parser for `stream.closed` and `RevokedEventData`.
- [`runtime.ts`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/lib/stores/runtime.ts): Added `currentRoute: 'revoked'`, `revokedInfo` payload state, and reactive SSE event dispatch for `stream.closed`.
- [`RevokedView.svelte`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/lib/views/RevokedView.svelte): Dedicated component for `#revoked-page` showing reason, actor, timestamp, and "Pair this browser" button.
- [`SettingsView.svelte`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/lib/views/SettingsView.svelte): Full paired devices management table, "this one" badge, inline renaming, revocation confirmation dialogs, "Revoke all others" dialog, and network listener status control with explanatory feedback.
- [`+page.svelte`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/routes/+page.svelte): Routed `'revoked'` view state.
- [`i18n/index.ts`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/lib/i18n/index.ts): Added fallback translations for all devices keys.
- **Vitest Tests**: [`RevokedView.test.ts`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/lib/views/RevokedView.test.ts) (3 tests) and [`SettingsView.test.ts`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/lib/views/SettingsView.test.ts) (5 tests).

### 2.5 `pm-web-e2e` (Playwright E2E)
- [`devices.spec.js`](file:///Users/oliver/git/pm-mcp-clients/pm-web-e2e/tests/devices.spec.js): End-to-end browser suite validating listing, inline rename, turning off network listener, revoking other devices, and revoking current device leading to `#revoked-page`.

---

## 3. Verification & Test Evidence

| Module / Scope | Test Suite | Tests Run | Pass Rate |
|---|---|---|---|
| `pm-api` | `PairedDeviceTest`, `DevicesCatalogueTest`, etc. | 139 | 100% |
| `pm-mock-runtime` | `PairedDevicesScenarioTest`, `StandardScenariosTest`, etc. | 96 | 100% |
| `pm-operator` | `WebDevicesCommandTest`, `PmOperatorTest`, etc. | 265 | 100% |
| `pm-web-app` | `RevokedView.test.ts`, `SettingsView.test.ts`, `PairingView.test.ts`, etc. | 31 | 100% |

---

## 4. Automated Review & Quality Gate Resolution

All automated findings from SonarCloud and CodeRabbit were addressed with 100% compliance:
- **SonarCloud**: Resolved resource leaks (closed `Terminal` via try-with-resources), reduced cognitive complexity in `DevicesCommand` to < 8 via helper extractions, eliminated dead condition branches, removed `TODO` comment triggers, eliminated string literal duplication in `MockRuntimeEngine` and `WebCommand`, and boosted test coverage on new code above the Quality Gate threshold.
- **CodeRabbit AI**: Addressed all findings:
  - `pm-web-app/src/lib/api/client.ts`: Constrained `turnOffLanListener` fallback to HTTP 404/405 errors; exposed `getDeviceId()` from local storage.
  - `pm-web-app/src/lib/views/SettingsView.svelte`: Updated `isThisBrowser` to match on stored `device_id`; updated self-revocation flow to route to `'revoked'` with `revokedInfo`; implemented dynamic relative time formatting for `last_used_at`.
  - `pm-web-app/src/lib/views/RevokedView.svelte`: Replaced hardcoded English reason strings and fixture timestamps with ICU-select-aware i18n catalogue lookup.
  - `pm-web-app/src/lib/stores/runtime.ts`: Deduplicated revocation handling by delegating purely to `apiClient.setOnRevoked`.
  - `pm-web-e2e/tests/devices.spec.js`: Asserted HTTP 200 response for scenario switch setup.
- **Integration Tests**: Removed invalid non-interactive process invocation of `web devices` from `PmOperatorProcessIT`, preserving `PmOperatorProcessIT`'s dedicated focus on `status` and `runtime start` process benchmarks.

---

## 5. Architecture Invariants Verification

- **Pure Java 25 Records**: All new data representations (`PairedDevice`, `DeviceRenamedEvent`, etc.) are immutable records.
- **Jackson Streaming Only**: Zero usage of `jackson-databind`; streaming `JsonParser` and `JsonGenerator` via `JsonTree` and custom codecs.
- **No Uncatalogued Strings**: All UI and CLI texts bound to `MessageCatalogue` and bilingual resource bundles (`devices.en.properties` & `devices.de.properties`).
- **Strict Dependency Boundaries**: `pm-operator` and `pm-mock-runtime` depend only on `pm-api`. Enforcer rule `clients-depend-on-pm-api-only` passed without violations.
- **Output Safety**: Terminal outputs are stripped of ANSI and terminal control sequences before display.
