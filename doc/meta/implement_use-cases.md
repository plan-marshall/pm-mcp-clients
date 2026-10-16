# Implementation Guide: Use-Case Integration Across Clients & Runtime

This document establishes the canonical, step-by-step methodology for planning and implementing use cases from `plan-marshall-documentation/doc/use-cases/` into the `pm-mcp-clients` repository.

Whenever a session is instructed:
> **"Plan the use-case `<UC-XXX>` using `doc/meta/implement_use-cases.md`"**

the agent shall follow the phased process and task breakdown defined in this guide.

---

## 1. Architectural Foundations & Invariants

Every use-case implementation in `pm-mcp-clients` must satisfy the following architectural invariants:

1. **Strict Dependency Boundaries**:
   - `pm-operator`, `pm-web-app`, and `pm-mock-runtime` consume domain contracts from `pm-api` only.
   - `pm-operator` and `pm-relay` never depend on each other.
   - `pm-api` uses only Jackson streaming (`jackson-core`), never `jackson-databind`.
   - No Quarkus, no CDI; pure Java 25 records, `@UtilityClass`, `@Value`, `@Builder` (Lombok), final fields.

2. **Both Clients, One State**:
   - Every domain workflow state is modeled for both clients: the terminal console (`pm-operator`) and the browser application (`pm-web-app`).
   - If a client-specific constraint exists (e.g. terminal acts from no paired device, or LAN listener toggle is refused from a network-connected browser), it must be explicitly defined in the shared design (`doc/design/shared/`).

3. **Catalogue-Only Texts**:
   - User-facing text must **never** be hardcoded in application code (`pm-operator` or `pm-web-app`).
   - All text messages, prompts, badges, and titles are sourced from `pm-api/src/main/resources/messages/` in both English (`.en.properties`) and German (`.de.properties`).
   - Automated tests assert that no raw, uncatalogued literals are emitted.

4. **Mock First & Deterministic Testing**:
   - Every client-facing flow must be reproducible against `pm-mock-runtime`.
   - Scenarios in `pm-mock-runtime/src/main/resources/scenarios/` provide the contract fixtures, hypermedia links, and simulated server-sent events (SSE).
   - Integration tests in `pm-operator` and `pm-web-e2e` run against `pm-mock-runtime`, never requiring a live production daemon.

---

## 2. The 6-Phase Implementation Lifecycle

Implementing any use case spans 6 sequential phases across all repository modules:

```
┌─────────────────────────────────────────────────────────────┐
│ Phase 1: Traceability & Design Alignment                    │
│   • Map UC-XXX to doc/design/ and doc/plans/                │
│   • Define API endpoints, payloads, events, and text keys   │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│ Phase 2: pm-api Contracts & Message Catalogue               │
│   • Java 25 Records / DTOs (Jackson streaming / JsonTree)   │
│   • Message keys in messages/<area>.{en,de}.properties      │
│   • Unit tests for codec round-trip and catalogue access    │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│ Phase 3: pm-mock-runtime Scenarios & Handlers               │
│   • Scenario JSON in src/main/resources/scenarios/          │
│   • States, links, auto-transitions, and SSE events         │
│   • Dynamic route handlers in MockHttpHandler (if stateful) │
│   • Register in StandardScenarios & scenario unit tests     │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│ Phase 4: pm-operator (CLI Client) Implementation            │
│   • Picocli verb & subcommand implementation                │
│   • Interactive JLine prompt and non-interactive arguments │
│   • Output safety & MessageCatalogue resolution             │
│   • Unit tests & transcript-style CLI tests                 │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│ Phase 5: pm-web-app (Web Client) Implementation             │
│   • Svelte components & views (desktop + mobile layouts)    │
│   • ApiClient method additions & SSE event subscription     │
│   • $t('key') catalogue binding                             │
│   • Vitest unit & component tests                           │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│ Phase 6: Integration Testing & Verification Sweep           │
│   • Operator process ITs (PmOperatorProcessIT / *IT.java)   │
│   • Playwright E2E specs in pm-web-e2e/tests/*.spec.js      │
│   • Reactor verification: ./mvnw verify -Ppre-commit        │
└─────────────────────────────────────────────────────────────┘
```

---

### Phase 1: Traceability & Design Alignment

1. **Locate Use-Case Definition**:
   - Read the use-case specification in `/Users/oliver/git/plan-marshall-documentation/doc/use-cases/<category>.adoc`.
   - Identify actor, preconditions, happy path, variations, error states, and outcome.

2. **Locate Local Client Design**:
   - Read the corresponding design specification in `doc/design/shared/<topic>.adoc`.
   - Read terminal transcripts in `doc/design/terminal-client/<chapter>.adoc`.
   - Read web design mockups and view behavior in `doc/design/web-app/<chapter>.adoc`.
   - Check if an existing plan package exists in `doc/plans/<topic>-contract.adoc` or `cli-<topic>.adoc`.

3. **Draft the Implementation Matrix**:
   - **REST Endpoints**: Methods, paths, query parameters, request body schemas, response schemas, HTTP status codes.
   - **Server-Sent Events (SSE)**: Event names, trigger points, payload format.
   - **Hypermedia Links**: Link relations (`links`), target state names (`next`), and action URIs (`href`).
   - **Catalogue Keys**: List of all keys needed in `messages/<area>.{en,de}.properties`.

---

### Phase 2: `pm-api` Contracts & Message Catalogue

1. **Data Models / Records**:
   - Implement immutable Java 25 records in `de.planmarshall.api.<domain>`.
   - Use Jackson streaming (`JsonParser` / `JsonGenerator`) or `JsonTree` parsing.
   - Avoid reflection, mutable fields, or external serialization dependencies.

2. **Message Catalogue Expansion**:
   - Add required keys to `pm-api/src/main/resources/messages/<area>.en.properties`.
   - Add matching translations to `pm-api/src/main/resources/messages/<area>.de.properties`.
   - Support ICU-style parameter replacement (e.g. `{label}`, `{time}`, `{count, plural, ...}`).

3. **Testing**:
   - Unit tests verifying JSON serialization and deserialization round-trips.
   - Unit tests verifying message catalogue keys exist in both English and German packs.

---

### Phase 3: `pm-mock-runtime` Scenarios & Handlers

1. **Scenario Definition**:
   - Create or extend `src/main/resources/scenarios/<scenario-name>.json`.
   - Define:
     - `start`: initial state name.
     - `routes`: static route representations (e.g. `GET /api/v1/...`).
     - `states`: map of state representations with `summary`, `links`, and optional `events` or `auto`.
     - `pairing_code`: default pairing code.

2. **Dynamic Route Handlers (if mutable)**:
   - If the use case performs mutations (`POST`, `PATCH`, `PUT`, `DELETE`), update `MockHttpHandler.java` in `pm-mock-runtime` to maintain in-memory state and emit appropriate SSE events.

3. **Registration & Standard Scenarios**:
   - Add the scenario constant to `de.planmarshall.mock.StandardScenarios`.
   - Update `StandardScenarios.ALL`.
   - Add unit/IT tests asserting that the scenario loads cleanly through `ScenarioRegistry` and serves expected HTTP responses.

---

### Phase 4: `pm-operator` Implementation

1. **Command Class Structure**:
   - Create or update the Picocli command class annotated with `@Command` and `@OperatorVerb`.
   - Register subcommand in parent verb or `PmOperator.java`.
   - Support both non-interactive invocation (e.g. `--yes`, `--no-input`) and interactive JLine prompts.

2. **Console Dialogues & Safety**:
   - Format terminal output to match transcripts in `doc/design/terminal-client/`.
   - Sanitize all runtime strings before writing to the terminal using `OutputSafety`.
   - Use `MessageCatalogue` for all messages, formatted according to the current locale.

3. **Testing**:
   - Unit tests with `FakeRuntimeServer` or `RuntimeFixture` validating command line arguments, flags, exit codes, and output formatting.
   - Process integration tests (`*IT.java`) testing native and JAR execution.

---

### Phase 5: `pm-web-app` Implementation

1. **API Client & Reactive Stores**:
   - Add typed API methods to `pm-web-app/src/lib/api/client.ts`.
   - Update Svelte reactive stores (`runtimeStore`, or domain-specific stores) to reflect the state machine and SSE events.

2. **Svelte Components & Views**:
   - Create or update views in `pm-web-app/src/lib/views/` and reusable components in `pm-web-app/src/lib/components/`.
   - Bind texts with `$t('<key>')`.
   - Implement responsive styles (desktop table vs mobile card view, dialog modals, touch targets).

3. **Testing**:
   - Vitest component tests in `src/lib/views/<View>.test.ts` verifying rendering, interactions, API calls, and error states.

---

### Phase 6: Integration Testing & Verification Sweep

1. **Web E2E Tests (Playwright)**:
   - Create `pm-web-e2e/tests/<use-case>.spec.js`.
   - Start test against mock runtime scenario.
   - Test user interaction paths: happy path, validation failures, remote state changes via SSE, edge cases.

2. **Operator Process ITs**:
   - Add/extend integration tests in `pm-operator/src/test/java/.../*IT.java`.
   - Verify process exit codes, stdout/stderr invariants, and security preconditions (`PM-SEC-3`).

3. **Quality Gate Verification**:
   - Run `./mvnw compile`.
   - Run `./mvnw verify -Ppre-commit` and check for any formatting / OpenRewrite changes.
   - Run full `./mvnw verify` to guarantee 0 regressions, all tests passing, and 80%+ coverage.
   - Run `npm test` in `pm-web-app` and Playwright tests in `pm-web-e2e`.

---

## 3. Selected Use-Case for Implementation: `UC-ENT-2: Manage Paired Devices`

### Selection Rationale

1. **Direct Sequel to Foundation Groundwork**:
   The recent sessions completed the browser pairing foundation (`UC-ENT-1`, `PairingView.svelte`, `web-pairing.json`, `WebCommand.java`). Managing paired devices (`UC-ENT-2`) is the direct functional sequel: once a browser is paired, the operator must see it, rename it, revoke it, revoke other devices, or disable the network listener.

2. **Complete Specification & Design**:
   `UC-ENT-2` has a fully detailed design document in `doc/design/shared/paired-devices.adoc`, complete CLI transcripts in `doc/design/terminal-client/08-transcripts-later.adoc#t-devices`, web settings design in `doc/design/web-app/machine-settings.adoc#devices`, and message catalogue specifications in both English and German.

3. **Comprehensive Cross-Module Scope**:
   Implementing `UC-ENT-2` provides the perfect end-to-end template because it touches every single module without blocking or depending on the incomplete workflow DSL:
   - `pm-api`: Device records, HTTP contracts (`GET`, `PATCH`, `DELETE`), events (`device.renamed`, `device.revoked`, `stream.closed`).
   - `pm-mock-runtime`: Stateful mock scenario (`paired-devices.json`) managing connected devices and event broadcasting.
   - `pm-operator`: `web devices` verb refinement with listing, rename dialogue, revocation confirmations, and catalogue text bindings.
   - `pm-web-app`: Devices card in settings, rename dialog, revocation modal, and the dedicated "Revoked Device" screen (`#revoked-page`).
   - `pm-operator` IT: Automated transcript tests for `web devices`.
   - `pm-web-e2e`: Playwright suite testing device listing, renaming, revoking other devices, and immediate logout/revocation handling.

---

## 4. Concrete Task Plan for `UC-ENT-2: Manage Paired Devices`

When instructed to plan or implement `UC-ENT-2`, execute the following task breakdown:

### Task Set 1: `pm-api` Contracts & Message Catalogue
- [ ] **1.1 Data Records**: Define `PairedDevice` record in `pm-api` with `deviceId`, `label`, `pairedFrom` (`loopback` | `network`), `createdAt`, and `lastUsedAt`.
- [ ] **1.2 Device Events**: Define `DeviceRenamedEvent`, `DeviceRevokedEvent`, and `StreamClosedEvent` (reasons: `device_revoked`, `listener_closed`, `device_expired`).
- [ ] **1.3 Message Catalogue**: Add all keys from `doc/design/shared/paired-devices.adoc#texts` into `messages/devices.en.properties` and `messages/devices.de.properties`.
- [ ] **1.4 Unit Tests**: Add unit tests in `pm-api` verifying JSON parsing, serialization, and message catalogue retrieval.

### Task Set 2: `pm-mock-runtime` Devices Scenario & Handler
- [ ] **2.1 Scenario Fixture**: Create `pm-mock-runtime/src/main/resources/scenarios/paired-devices.json` with initial paired devices (e.g. Firefox on loopback, iPad on network, Chrome on build-box) and listener states.
- [ ] **2.2 Route Handlers**: Implement dynamic handlers in `MockHttpHandler.java` for:
  - `GET /api/v1/web/devices` (returns list of devices)
  - `PATCH /api/v1/web/devices/{deviceId}` (renames device, emits `device.renamed`)
  - `DELETE /api/v1/web/devices/{deviceId}` (revokes device, emits `device.revoked` & `stream.closed`)
  - `DELETE /api/v1/web/devices?others` (revokes all other devices)
  - `DELETE /api/v1/web/listeners/lan` (turns network listener off, leaves loopback open)
- [ ] **2.3 Scenario Registry**: Register `PAIRED_DEVICES = "paired-devices"` in `StandardScenarios.java`.
- [ ] **2.4 Scenario Tests**: Add unit tests in `pm-mock-runtime` validating scenario loading, transitions, and HTTP mutation responses.

### Task Set 3: `pm-operator` Terminal Client Integration
- [ ] **3.1 Subcommand Refinement**: Refine `WebCommand.DevicesCommand` to align with the transcript in `08-transcripts-later.adoc#t-devices`:
  - `pm-operator web devices`: Formatted table with indices, labels, `paired_from`, and `last_used`.
  - `pm-operator web devices rename <id|index> <newLabel>`: PATCH request, prints `✓ Renamed: <label>`.
  - `pm-operator web devices revoke <id|index>`: Asks confirmation unless `--yes` is given, DELETE request.
  - `pm-operator web devices revoke --others` (or `--all`): Asks confirmation, voids devices, reports count.
  - `pm-operator web devices listener-off`: Turns off LAN listener, leaves loopback open.
- [ ] **3.2 Message Catalogue Binding**: Replace hardcoded output strings with `MessageCatalogue.get()` using the `devices.*` keys.
- [ ] **3.3 CLI Tests**: Add unit and transcript tests in `pm-operator` validating each device action against mock runtime.

### Task Set 4: `pm-web-app` Client Integration
- [ ] **4.1 ApiClient Methods**: Add `getDevices()`, `renameDevice(id, label)`, `revokeDevice(id)`, `revokeOtherDevices()`, and `turnOffLanListener()` to `pm-web-app/src/lib/api/client.ts`.
- [ ] **4.2 Settings Devices Component**: Enhance `SettingsView.svelte` (or extract `DevicesManager.svelte`):
  - Table displaying label, paired from, last used, and "this one" badge for current device.
  - Rename action with inline or modal editing.
  - Revoke action with confirmation dialog.
  - "Revoke all others" button with confirmation naming count.
  - Network listener status and "Turn network listener off" button.
- [ ] **4.3 Revoked Device View**: Implement the dedicated `#revoked-page` state when the active session receives `stream.closed` (reason `device_revoked` or `device_expired`), displaying who revoked it, when, and a button to re-pair.
- [ ] **4.4 Vitest Tests**: Add component tests verifying device listing, rename dispatch, revoke confirmation, and revoked view display.

### Task Set 5: End-to-End & Integration Tests
- [ ] **5.1 pm-web-e2e Playwright Suite**: Add `pm-web-e2e/tests/devices.spec.js`:
  - Test viewing device list and identifying current device.
  - Test renaming a device and verifying updated label in list.
  - Test revoking another device and verifying it disappears from list.
  - Test revoking current device and verifying immediate transition to Revoked Page.
  - Test turning off network listener.
- [ ] **5.2 pm-operator IT Suite**: Add integration test in `PmOperatorProcessIT.java` executing `web devices` commands against `pm-mock-runtime`.
- [ ] **5.3 Quality Gate Sweep**: Run `./mvnw verify -Ppre-commit` and `./mvnw verify`, ensuring all checks pass cleanly.

---

## 5. Verification Checklist for New Use Cases

Before declaring any use-case implementation complete, verify:

- [ ] All Java code adheres to Java 25 standards, Lombok, and package-private visibility where possible.
- [ ] No uncatalogued, user-facing hardcoded strings remain in either client.
- [ ] Messages are present and verified in both English and German.
- [ ] Mock runtime scenario loads and handles all documented state branches.
- [ ] `pm-operator` respects `PM-SEC-3` security preconditions and provides safe terminal rendering.
- [ ] `pm-web-app` handles responsive layout down to mobile width (320px).
- [ ] Integration tests pass in `pm-web-e2e` and `pm-operator`.
- [ ] Code formatting and OpenRewrite recipes pass cleanly via `./mvnw verify -Ppre-commit`.
