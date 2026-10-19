# Implementation Plan: UC-ENT-1 Enter and Connect (Pairing, Listeners, Device Handshake)

This plan coordinates the end-to-end implementation of use case `UC-ENT-1: Enter and Connect` and credential entry on loopback listener from `UC-ONB-8: Choose the Providers of the Machine` across all client and runtime modules in `pm-mcp-clients`.

It operationalizes the technical recipe from [`doc/meta/implement_use-cases.md`](file:///Users/oliver/git/pm-mcp-clients/doc/meta/implement_use-cases.md) under the execution discipline of [`doc/agy/runbook.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/runbook.md).

---

## 1. Traceability & Context

| Dimension | Reference |
|---|---|
| **Use Cases** | [`plan-marshall-documentation/doc/use-cases/02-entry.adoc#UC-ENT-1`](file:///Users/oliver/git/plan-marshall-documentation/doc/use-cases/02-entry.adoc) (Enter and Connect), [`01-onboarding.adoc#UC-ONB-8`](file:///Users/oliver/git/plan-marshall-documentation/doc/use-cases/01-onboarding.adoc) (Credentials via loopback only) |
| **Specifications** | [`01-cli-and-installer.adoc#start`](file:///Users/oliver/git/plan-marshall-documentation/doc/specification/cli-and-security/01-cli-and-installer.adoc), [`03-web-server.adoc#two-listeners`](file:///Users/oliver/git/plan-marshall-documentation/doc/specification/cli-and-security/03-web-server.adoc), [`#loopback-host`](file:///Users/oliver/git/plan-marshall-documentation/doc/specification/cli-and-security/03-web-server.adoc), [`#web-pairing`](file:///Users/oliver/git/plan-marshall-documentation/doc/specification/cli-and-security/03-web-server.adoc), [`#fresh-code`](file:///Users/oliver/git/plan-marshall-documentation/doc/specification/cli-and-security/03-web-server.adoc), [`#web-credentials`](file:///Users/oliver/git/plan-marshall-documentation/doc/specification/cli-and-security/03-web-server.adoc), [`01-processes-and-transport.adoc#local-api`](file:///Users/oliver/git/plan-marshall-documentation/doc/specification/runtime-model/01-processes-and-transport.adoc), [`06-event-catalogue.adoc`](file:///Users/oliver/git/plan-marshall-documentation/doc/specification/runtime-model/06-event-catalogue.adoc) |
| **Local Client Design** | Shared: [`doc/design/shared/paired-devices.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/design/shared/paired-devices.adoc)<br>CLI: [`doc/design/terminal-client/05-transcripts-entry.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/design/terminal-client/05-transcripts-entry.adoc)<br>Web: [`doc/design/web-app/README.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/design/web-app/README.adoc) |
| **Roadmap Packages** | [`doc/plans/implemented/entry-contract.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/plans/implemented/entry-contract.adoc), [`doc/plans/cli-onboarding.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/plans/cli-onboarding.adoc), [`doc/plans/web-onboarding.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/plans/web-onboarding.adoc) |
| **Implementation Recipe** | [`doc/meta/implement_use-cases.md`](file:///Users/oliver/git/pm-mcp-clients/doc/meta/implement_use-cases.md) |
| **Operational SOP** | [`doc/agy/runbook.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/runbook.md) |

---

## 2. Parallel Isolation & Non-Overlap Invariants (vs `UC-CTL-1..5`)

`UC-ENT-1` executes concurrently with `UC-CTL-1..5: Controlling Work` (`feat/uc-ctl-control`). Strict non-overlap is guaranteed across all repository modules:

1. **Contracts & API (`pm-api`)**:
   - `UC-ENT-1`: Owns `de.planmarshall.api.entry.*` and `messages/entry.{en,de}.properties`.
   - `UC-CTL-1..5`: Owns `de.planmarshall.api.control.*` and `messages/control.{en,de}.properties`.
   - Zero shared model classes, enums, or resource properties.
2. **Mock Runtime (`pm-mock-runtime`)**:
   - `UC-ENT-1`: Routes `/api/v1/web/pairings`, `/api/v1/web/listeners/lan`, scenario `scenarios/entry.json`.
   - `UC-CTL-1..5`: Routes `/api/v1/plans/{id}/actions/*`, `/api/v1/epics/{id}/actions/*`, scenario `scenarios/control.json`.
   - Disjoint route dispatch tables and scenario files.
3. **CLI Client (`pm-operator`)**:
   - `UC-ENT-1`: Owns `EnterCommand.java` and `PairCommand.java`.
   - `UC-CTL-1..5`: Owns `ControlCommand.java`.
   - Disjoint Picocli verb classes.
4. **Web Client (`pm-web-app`)**:
   - `UC-ENT-1`: `PairingModal.svelte`, `ConnectionStatusBanner.svelte`, `LanListenerModal.svelte`.
   - `UC-CTL-1..5`: `ControlActionsBar.svelte`, `HoldResolutionModal.svelte`, `PlanSettingsModal.svelte`.
   - Disjoint Svelte components and route views.
5. **E2E & Integration Tests**:
   - `UC-ENT-1`: `pm-web-e2e/tests/entry.spec.js` and `EnterCommandTest.java`.
   - `UC-CTL-1..5`: `pm-web-e2e/tests/control.spec.js` and `ControlCommandTest.java`.

---

## 3. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created on topic branch off latest `main` (`git worktree add -b feat/uc-ent-1-connect /Users/oliver/git/worktrees/pm-mcp-clients-uc-ent-1 main`).
- [x] 2. Domain skills & engineering personas loaded for each task type:
  - `persona-implementer` & `persona-module-tester` for code & unit tests
  - `persona-integration-tester` for multi-module/integration tests
  - `persona-security-expert` for pairing token security and loopback-only validation
  - `persona-documenter` for documentation & ADRs
  - `persona-code-reviewer` for finding evaluation & adversarial refutation
- [x] 3. Incremental implementation following the 6-Phase technical recipe with local tests passing.
- [x] 4. Final completion report written (`doc/agy/uc-ent-1-plan-report.md`) and initial plan moved to `doc/agy/implemented/uc-ent-1-plan.md`.
- [x] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit` for code/config; skipped for standalone documentation changes).
- [x] 6. PR opened (with `--label "skip-bot-review"` if authorized), 5-minute review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [x] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [x] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/uc-ent-1-plan-report.md`.

---

## 4. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Trace** | `plan-marshall-persona-documenter` | `plan-marshall-ref-code-quality` | Verify pairing handshake, loopback vs LAN listeners, fresh code generation, and event catalogue keys. |
| **Phase 2: `pm-api`** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-module-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:junit-core` | Immutable Java 25 records, Jackson streaming parsing, bilingual resource bundles (`entry.{en,de}.properties`). |
| **Phase 3: Mock Runtime** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-integration-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:cui-cui-http-testing` | Stateful scenario JSON (`entry.json`), dynamic route handlers, SSE broadcast (`device.paired`, `listener.changed`). |
| **Phase 4: `pm-operator`** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-security-expert` | `pm-dev-java:java-core`<br>`pm-dev-java:java-security` | Subcommands `enter`, `pair`, `listeners lan [--open|--close]`, QR code ASCII rendering, terminal output safety. |
| **Phase 5: `pm-web-app`** | `plan-marshall-persona-implementer`<br>`pm-dev-frontend:javascript` | `pm-dev-frontend:css`<br>`pm-dev-frontend:jest-testing` | Pairing code redemption view, loopback vs LAN indicators, credentials submission restriction (`loopback_only`), Vitest tests. |
| **Phase 6: Verification** | `plan-marshall-persona-integration-tester` | `pm-dev-java:junit-integration` | `PmOperatorProcessIT` process execution, Playwright E2E browser test suite (`entry.spec.js`). |
| **Phase 7: Review Gate** | `plan-marshall-persona-code-reviewer` | `pm-dev-java:java-security` | Adversarial refutation of candidate bot findings, 100% resolution bar. |

---

## 5. Phased Task Breakdown

### Phase 1: Trace Matrix & Specification Alignment
- Load `plan-marshall-persona-documenter`.
- Verify pairing protocol:
  - Terminal client requests pairing token (`POST /api/v1/web/pairings`, CLI-only).
  - Code redemption in web app generates device secret and triggers `device.paired` event with `paired_from`.
  - Terminal client receives fresh pairing code immediately after redemption.
  - LAN listener (`PUT /api/v1/web/listeners/lan`, CLI-only) returns TLS certificate fingerprint or refuses with `web_listener_conflict`.
  - Inbound credentials entry refused on LAN listener with error `loopback_only`.

### Phase 2: Contracts, Models & Message Catalogue (`pm-api`)
- Load `plan-marshall-persona-implementer` and `pm-dev-java:java-core`.
- Create records in `de.planmarshall.api.entry.*`:
  - `PairingToken.java`: Short-lived pairing token with expiration timestamp.
  - `PairingRedemptionRequest.java`: Redemption payload with device label and client type.
  - `PairingRedemptionResponse.java`: Issued device secret and API endpoint URL.
  - `LanListenerConfig.java`: Port, TLS fingerprint, enabled state.
  - `ConnectionStatus.java`: Listener states (loopback, LAN) and active device counts.
  - `DevicePairedEvent.java` & `ListenerChangedEvent.java`: SSE event records.
- Create bilingual catalogues: `messages/entry.en.properties` and `messages/entry.de.properties`.
- Unit tests with `plan-marshall-persona-module-tester`: `EntryRecordsTest.java`, `EntryCatalogueTest.java`.

### Phase 3: Mock Runtime Scenarios & Dynamic Endpoints (`pm-mock-runtime`)
- Load `plan-marshall-persona-implementer` and `pm-dev-java:cui-cui-http-testing`.
- Create scenario fixture `scenarios/entry.json`:
  - Machine not onboarded, browser paired on loopback, LAN listener open with fingerprint, code redemption flow.
- Implement endpoints in `MockRuntimeEngine.java`:
  - `POST /api/v1/web/pairings` (CLI-only guard)
  - `POST /api/v1/web/pairings/redeem`
  - `PUT /api/v1/web/listeners/lan` & `DELETE /api/v1/web/listeners/lan`
  - `GET /api/v1/status/connection`
- Unit tests: `EntryScenarioTest.java`.

### Phase 4: Terminal Client Integration (`pm-operator`)
- Load `plan-marshall-persona-implementer` and `pm-dev-java:java-security`.
- Implement `EnterCommand.java` and `PairCommand.java`:
  - `pair`: Displays pairing code and ASCII QR code for web client pairing.
  - `listeners lan --open [--port <port>]`: Opens LAN listener, displaying certificate fingerprint.
  - `listeners lan --close`: Closes LAN listener while keeping loopback open.
  - `status connection`: Displays listener addresses and active paired devices.
- Unit and process tests: `EnterCommandTest.java` and `PmOperatorProcessIT.java`.

### Phase 5: Web Application Views (`pm-web-app`)
- Load `plan-marshall-persona-implementer` and `pm-dev-frontend:javascript`.
- Extend `client.ts` with pairing redemption and listener status queries.
- Create `PairingModal.svelte`: Form to enter pairing code displayed in the terminal.
- Create `ConnectionStatusBanner.svelte`: Visual indicator of loopback vs LAN connection mode.
- Restrict sensitive provider credentials entry to loopback connections, displaying `loopback_only` warning on LAN.
- Vitest unit tests: `PairingModal.test.ts`, `ConnectionStatusBanner.test.ts`.

### Phase 6: Verification & End-to-End Suite
- Load `plan-marshall-persona-integration-tester`.
- Write Playwright E2E suite: `pm-web-e2e/tests/entry.spec.js`.
- Execute `./mvnw verify -Ppre-commit` and ensure 100% clean reactor compilation.

---

## 6. Definition of Done
1. `UC-ENT-1` (Enter and Connect) and loopback credential restriction (`UC-ONB-8`) completely implemented across CLI and Web clients.
2. Invariant "Both clients, one state" verified via real-time SSE event subscriptions (`device.paired`, `listener.changed`).
3. 100% of user-facing messages externalized in bilingual property bundles (`entry.{en,de}.properties`).
4. Full reactor build `./mvnw verify -Ppre-commit` passes with zero regressions.
5. Completion report documented at `doc/agy/uc-ent-1-plan-report.md` and initial plan archived.
