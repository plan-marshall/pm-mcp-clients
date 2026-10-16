# Implementation Plan: UC-ENT-2 Manage Paired Devices

This plan coordinates the end-to-end implementation of use case `UC-ENT-2: Manage Paired Devices` across all client and runtime modules in `pm-mcp-clients`.

It operationalizes the technical recipe from [`doc/meta/implement_use-cases.md`](file:///Users/oliver/git/pm-mcp-clients/doc/meta/implement_use-cases.md) under the execution discipline of [`doc/agy/runbook.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/runbook.md).

---

## 1. Traceability & Context

| Dimension | Reference |
|---|---|
| **Use Case** | [`plan-marshall-documentation/doc/use-cases/02-entry.adoc#UC-ENT-2`](file:///Users/oliver/git/plan-marshall-documentation/doc/use-cases/02-entry.adoc) |
| **Specifications** | [`03-web-server.adoc#devices`](file:///Users/oliver/git/plan-marshall-documentation/doc/specification/cli-and-security/03-web-server.adoc), [`01-processes-and-transport.adoc#local-api`](file:///Users/oliver/git/plan-marshall-documentation/doc/specification/runtime-model/01-processes-and-transport.adoc), [`06-event-catalogue.adoc`](file:///Users/oliver/git/plan-marshall-documentation/doc/specification/runtime-model/06-event-catalogue.adoc) |
| **Local Client Design** | Shared: [`doc/design/shared/paired-devices.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/design/shared/paired-devices.adoc)<br>CLI: [`doc/design/terminal-client/08-transcripts-later.adoc#t-devices`](file:///Users/oliver/git/pm-mcp-clients/doc/design/terminal-client/08-transcripts-later.adoc)<br>Web: [`doc/design/web-app/machine-settings.adoc#devices`](file:///Users/oliver/git/pm-mcp-clients/doc/design/web-app/machine-settings.adoc) |
| **Roadmap Package** | [`doc/plans/entry-contract.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/plans/entry-contract.adoc) |
| **Implementation Recipe** | [`doc/meta/implement_use-cases.md`](file:///Users/oliver/git/pm-mcp-clients/doc/meta/implement_use-cases.md) |
| **Operational SOP** | [`doc/agy/runbook.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/runbook.md) |

---

## 2. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [ ] 1. Isolated worktree created on topic branch off latest `main` (`git worktree add -b feat/uc-ent-2-paired-devices ...`).
- [ ] 2. Domain skills & engineering personas loaded for each task type:
  - `persona-implementer` & `persona-module-tester` for code & unit tests
  - `persona-integration-tester` for multi-module/integration tests
  - `persona-security-expert` for trust boundaries & input validation
  - `persona-documenter` for documentation & ADRs
  - `persona-code-reviewer` for finding evaluation & adversarial refutation
- [ ] 3. Incremental implementation following the 6-Phase technical recipe with local tests passing.
- [ ] 4. Pre-commit check passed (`./mvnw verify -Ppre-commit` for code/config; skipped for documentation-only changes).
- [ ] 5. PR opened, 5-minute review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [ ] 6. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.

---

## 3. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Trace** | `plan-marshall-persona-documenter` | `plan-marshall-ref-code-quality` | Verify endpoint schemas, event schemas, link relations, catalogue text keys. |
| **Phase 2: `pm-api`** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-module-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:junit-core` | Immutable Java 25 records, Jackson streaming parsing, bilingual resource bundles. |
| **Phase 3: Mock Runtime** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-integration-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:cui-cui-http-testing` | Stateful scenario JSON (`paired-devices.json`), dynamic route handlers, SSE broadcast. |
| **Phase 4: `pm-operator`** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-security-expert` | `pm-dev-java:java-core`<br>`pm-dev-java:java-security` | Picocli subcommands (`web devices rename/revoke/listener-off`), `OutputSafety`, JLine prompts. |
| **Phase 5: `pm-web-app`** | `plan-marshall-persona-implementer`<br>`pm-dev-frontend:javascript` | `pm-dev-frontend:css`<br>`pm-dev-frontend:jest-testing` | Devices card in settings, rename/revoke modals, dedicated `#revoked-page`, Vitest tests. |
| **Phase 6: Verification** | `plan-marshall-persona-integration-tester` | `pm-dev-java:junit-integration` | `PmOperatorProcessIT` process execution, Playwright E2E browser test suite. |
| **Phase 7: Review Gate** | `plan-marshall-persona-code-reviewer` | `pm-dev-java:java-security` | Adversarial refutation of candidate bot findings, 100% resolution bar. |

---

## 4. Phased Task Breakdown

### Phase 1: Traceability & Design Alignment
- [ ] **1.1 Design Specification Audit**: Review `doc/design/shared/paired-devices.adoc` and verify:
  - Resource routes: `GET /api/v1/web/devices`, `PATCH /api/v1/web/devices/{id}`, `DELETE /api/v1/web/devices/{id}`, `DELETE /api/v1/web/devices?others`, `DELETE /api/v1/web/listeners/lan`.
  - SSE events: `device.renamed`, `device.revoked`, `stream.closed` (reasons: `device_revoked`, `listener_closed`, `device_expired`).
  - Text keys: Extract the complete set of keys needed for `messages/devices.{en,de}.properties`.

### Phase 2: `pm-api` Contracts & Message Catalogue
- [ ] **2.1 Data Records**: Implement immutable Java 25 record `PairedDevice` (and container `PairedDevicesResponse`) in package `de.planmarshall.api.web` with fields:
  - `deviceId` (`String`, non-null)
  - `label` (`String`, non-null)
  - `pairedFrom` (`String`: `loopback` | `network`)
  - `createdAt` (`Instant`, non-null)
  - `lastUsedAt` (`Instant`, non-null)
- [ ] **2.2 Event Records**: Implement event payload records:
  - `DeviceRenamedEvent` (`deviceId`, `label`)
  - `DeviceRevokedEvent` (`deviceId`)
  - `StreamClosedEvent` (`reason`, `details`)
- [ ] **2.3 Streaming Codecs**: Implement streaming JSON read/write methods via Jackson streaming (`jackson-core`).
- [ ] **2.4 Message Catalogue Entries**:
  - Populate `pm-api/src/main/resources/messages/devices.en.properties` with keys specified in `paired-devices.adoc#texts`.
  - Populate `pm-api/src/main/resources/messages/devices.de.properties` with German translations.
- [ ] **2.5 Unit Tests**:
  - `PairedDeviceTest.java`: Serialization / deserialization round-trip tests.
  - `DevicesCatalogueTest.java`: Key presence and ICU parameter validation across both English and German bundles.

### Phase 3: `pm-mock-runtime` Scenarios & Handlers
- [ ] **3.1 Scenario Definition**: Create `pm-mock-runtime/src/main/resources/scenarios/paired-devices.json`:
  - Initial devices: Loopback browser (`this one`), Network tablet, Remote CI box.
  - Listener states: Loopback active, LAN active on port 44321.
- [ ] **3.2 Dynamic Route Handlers**: Extend `MockHttpHandler.java` (or add modular device handlers):
  - Handle `GET /api/v1/web/devices` -> returns active paired device list with hypermedia links.
  - Handle `PATCH /api/v1/web/devices/{deviceId}` -> updates label, broadcasts `device.renamed`.
  - Handle `DELETE /api/v1/web/devices/{deviceId}` -> removes device, broadcasts `device.revoked` and `stream.closed`.
  - Handle `DELETE /api/v1/web/devices?others` -> removes all except current caller, broadcasts events.
  - Handle `DELETE /api/v1/web/listeners/lan` -> deactivates LAN listener, leaves loopback open.
- [ ] **3.3 Scenario Registration**:
  - Add `PAIRED_DEVICES = "paired-devices"` constant in `StandardScenarios.java`.
  - Add to `StandardScenarios.ALL`.
- [ ] **3.4 Scenario Tests**:
  - Add `PairedDevicesScenarioTest.java` verifying scenario loads cleanly and responds correctly to mutation endpoints.

### Phase 4: `pm-operator` Terminal Client Implementation
- [ ] **4.1 Command Refinement**: Expand `WebCommand.DevicesCommand` in `pm-operator`:
  - `pm-operator web devices`: Tabular output formatted per `doc/design/terminal-client/08-transcripts-later.adoc#t-devices`.
  - `pm-operator web devices rename <id|index> <newLabel>`: PATCH request, formatted success message.
  - `pm-operator web devices revoke <id|index>`: JLine confirmation prompt (or `--yes`), DELETE request.
  - `pm-operator web devices revoke --others`: JLine confirmation prompt with target device count, bulk DELETE.
  - `pm-operator web devices listener-off`: Closes LAN listener, prints confirmation.
- [ ] **4.2 Output Safety & Catalogue Binding**:
  - Sanitize device labels and external strings using `OutputSafety`.
  - Resolve all terminal prompts, tables, headers, and notices via `MessageCatalogue`.
- [ ] **4.3 CLI Unit & Transcript Tests**:
  - Add unit test suite for `DevicesCommandTest.java` simulating CLI inputs with `FakeRuntimeServer`.

### Phase 5: `pm-web-app` Client Implementation
- [ ] **5.1 API Client Extensions**:
  - Add typed methods in `pm-web-app/src/lib/api/client.ts` (`fetchDevices()`, `renameDevice()`, `revokeDevice()`, `revokeOtherDevices()`, `turnOffLanListener()`).
  - Extend SSE event listener for `device.renamed`, `device.revoked`, and `stream.closed`.
- [ ] **5.2 Settings Component (`DevicesCard.svelte`)**:
  - Implement devices section inside Settings view per `doc/design/web-app/machine-settings.adoc#devices`.
  - Include "this one" badge for active browser session.
  - Inline / modal editing for device label.
  - Revocation confirmation dialogs (single device and bulk "Revoke all others").
  - LAN listener indicator and "Turn network listener off" action button.
- [ ] **5.3 Dedicated Revoked Device Screen (`RevokedView.svelte`)**:
  - Screen displayed when session receives `stream.closed` (reason `device_revoked` or `device_expired`).
  - Explains who revoked the device and when, with a clear button to re-pair.
- [ ] **5.4 Vitest Component Tests**:
  - Add `DevicesCard.test.ts` and `RevokedView.test.ts` verifying rendering, interactions, modal confirmations, and store updates.

### Phase 6: Integration Testing & Verification
- [ ] **6.1 Playwright E2E Suite**: Create `pm-web-e2e/tests/devices.spec.js`:
  - Test viewing device list and identifying current device.
  - Test renaming a device and verifying updated label in list.
  - Test revoking another device and asserting removal from list.
  - Test revoking current device and asserting immediate transition to `#revoked-page`.
  - Test turning off network listener.
- [ ] **6.2 Operator Process IT**:
  - Add `WebDevicesIT.java` in `pm-operator` validating Picocli CLI invocations against running `pm-mock-runtime`.
- [ ] **6.3 Domain Invariants Verification**:
  - Verify against checklist in Section 5 of `doc/meta/implement_use-cases.md`.

### Phase 7: Pre-Commit, Review Gate & Landing (Runbook Discipline)
- [ ] **7.1 Pre-Commit Gate**:
  - Run `./mvnw verify -Ppre-commit` to validate OpenRewrite formatting, Checkstyle, and full test suite.
- [ ] **7.2 Commit & PR**:
  - Commit changes with Conventional Commits message (`feat(web,operator): implement UC-ENT-2 manage paired devices`).
  - Open PR via `gh pr create`.
- [ ] **7.3 5-Minute Review Window**:
  - Schedule 300s timer (`schedule` tool) and await CI, SonarCloud, Sourcery, and CodeRabbit.
- [ ] **7.4 100% Findings Resolution**:
  - Evaluate all automated findings with `persona-code-reviewer` adversarial refutation and resolve 100% of issues.
- [ ] **7.5 Merge Queue & Cleanup**:
  - Enqueue PR via `gh pr merge --auto --squash`.
  - Upon merge, synchronize `main`, remove worktree and delete topic branch.
- [ ] **7.6 Roadmap Archival**:
  - Update `doc/plans/README.adoc` and move completed package documentation to `doc/plans/implemented/`.
