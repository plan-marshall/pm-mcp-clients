# Implementation Plan: `entry-contract` (Entry: Contract and Mock Runtime)

This implementation plan governs the implementation of the `entry-contract` work package (`doc/plans/entry-contract.adoc`, epic clients) in `pm-api` and `pm-mock-runtime`.

It operates strictly under the rules and lifecycle defined in [`doc/agy/runbook.md`](runbook.md).

---

## 1. Scope & Objectives

The goal is to implement the client-side contract for machine entry, pairing, network listeners, and device management in `pm-api`, and back them with scenario support in `pm-mock-runtime`:
1. **The Entry (UC-ENT-1)**: Reading runtime status, onboarding progress, loopback and LAN listener states, pairing endpoint (`POST /api/v1/web/pairings`, CLI-only), fresh code after redemption, and loopback `Host` validation.
2. **Web Credentials (UC-ONB-8)**: Loopback-only entry of secrets; LAN listener rejection with `loopback_only` (403/409).
3. **Paired Devices Management (UC-ENT-2)**: Listing paired devices (`GET /api/v1/web/devices`), renaming (`PATCH`), revoking (`DELETE /api/v1/web/devices/{device_id}`), revoking all others (`DELETE /api/v1/web/devices?others`), toggling LAN listener (`PUT`/`DELETE /api/v1/web/listeners/lan`), and termination events (`device.paired`, `device.renamed`, `device.revoked`, `stream.closed`).
4. **Mock Runtime Scenarios (`entry-<case>`)**: Un-onboarded machine, active onboarding, paired browser, SSH session, fresh pairing code, LAN listener conflict, device lifecycle, and stream termination.

### Target Work Package
- Specification: `doc/plans/entry-contract.adoc`.
- Use Cases: `UC-ENT-1`, `UC-ENT-2`, `UC-ONB-8`.
- Specifications: CLI & Security § The Terminal Client as the Entry, § Loopback and LAN Listeners Side by Side, § Host Validation, § Pairing Protocol, § Credentials in the Web App, § Devices in Both Clients; Runtime Model § Local API, § Event Catalogue.
- Design: `doc/design/terminal-client/05-transcripts-entry.adoc#t-entry`, `doc/design/shared/paired-devices.adoc`.
- Primary Modules: `pm-api` (`de.planmarshall.api.entry.*`), `pm-mock-runtime`.

---

## 2. Concurrency & Isolation

This plan executes concurrently in Wave 8 alongside:
- `onboarding-contract` (`pm-api:de.planmarshall.api.onboarding.*`, `pm-mock-runtime:scenarios/onboarding-*.json`)
- `control-contract` (`pm-api:de.planmarshall.api.control.*`, `pm-mock-runtime:scenarios/control-*.json`)

**Boundary Rules**:
- `pm-api`: All new classes must reside strictly within package `de.planmarshall.api.entry` (and subpackages if needed). No modifications to sibling contract packages.
- `pm-mock-runtime`: All new/modified scenario files must follow the `entry-*.json` naming convention. All unit tests must reside in `EntryScenarioTest.java` or `EntryContractTest.java`.
- No shared file edits across work packages; each work package owns its disjoint package hierarchy.

---

## 3. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [ ] 1. Isolated worktree created at `/Users/oliver/git/worktrees/pm-mcp-clients-entry-contract` on branch `feat/entry-contract`.
- [ ] 2. Domain skills & engineering personas loaded:
  - `persona-implementer` & `persona-module-tester` for Java 25 records and Jackson streaming codecs.
  - `persona-security-expert` for loopback trust boundary, pairing tokens, and device revocation.
  - `persona-integration-tester` for mock runtime scenarios and HTTP/SSE validation.
  - `persona-code-reviewer` for review bot resolution.
- [ ] 3. Incremental implementation following the 5-Phase technical recipe with local tests passing.
- [ ] 4. Final completion report written (`doc/agy/entry-contract-plan-report.md`), initial plan moved to `doc/agy/implemented/entry-contract-plan.md`, and package marked done in `doc/plans/README.adoc`. (Do NOT move report to `verified/` — post-landing verification moves it).
- [ ] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit`).
- [ ] 6. PR opened (NEVER use `skip-bot-review`), review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [ ] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [ ] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/entry-contract-plan-report.md` by orchestrator session.

---

## 4. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Trace & Schema** | `plan-marshall-persona-documenter` | `plan-marshall-ref-code-quality` | Map `UC-ENT-1`, `UC-ENT-2`, `UC-ONB-8` and web server specifications to record definitions. |
| **Phase 2: API Records & Codecs** | `plan-marshall-persona-implementer` | `pm-dev-java:java-core` | Implement immutable records in `pm-api:de.planmarshall.api.entry` with streaming `jackson-core` codecs. |
| **Phase 3: Closed Catalogues & Security** | `plan-marshall-persona-security-expert` | `pm-dev-java:java-security` | Listener states, stream-close reasons, pairing code grammars, fail-closed loopback guards. |
| **Phase 4: Mock Scenarios & Tests** | `plan-marshall-persona-integration-tester` | `pm-dev-java:junit-core` | Scenarios (`entry-<case>`), mock routing, and `EntryScenarioTest`. |
| **Phase 5: Quality Gate & Review** | `plan-marshall-persona-code-reviewer` | `pm-dev-java:java-security` | Pre-commit validation, clean git status, automated review bot finding triage. |

---

## 5. Phased Technical Recipe

### Phase 1: Trace Matrix & Specification Alignment
- Map `doc/plans/entry-contract.adoc` to:
  - `UC-ENT-1`: Terminal client entry, listener discovery, pairing initiation.
  - `UC-ENT-2`: Paired devices list, rename, revoke, revoke-others.
  - `UC-ONB-8`: Loopback-only secret input, LAN listener rejection.

### Phase 2: `pm-api` Entry Records & Codecs
- Package: `de.planmarshall.api.entry`.
- Records:
  - `RuntimeEntryResource.java`, `ListenerResource.java`, `ListenerKind.java`, `ListenerState.java`.
  - `PairingResource.java`, `PairingCode.java`.
  - `PairedDeviceResource.java`, `DeviceListResource.java`.
  - `WebCredentialResource.java`.
- Jackson-core streaming codecs (`writeTo`, `readFrom`, `toMap`, `fromMap`) with zero `jackson-databind`.

### Phase 3: Closed Catalogues & Fail-Closed Validation
- Closed enumerations for listener kinds, listener states, and revocation reasons.
- Verbatim comparison tests in `EntryCataloguesTest.java`.
- Strict validation of fingerprint formats, device ids, and local API paths.

### Phase 4: Mock Runtime Scenario Implementation
- Under `pm-mock-runtime/src/main/resources/scenarios/`:
  - Enhance `entry.json` and provide specialized case scenarios (`entry-unonboarded.json`, `entry-paired.json`, etc.).
  - Route handlers for `/api/v1/web/pairings`, `/api/v1/web/listeners/*`, `/api/v1/web/devices/*`.
  - Unit tests in `EntryScenarioTest.java` verifying pairing, revocation, and stream closure.

### Phase 5: Verification & Completion Report
- `./mvnw verify -Ppre-commit` passing cleanly.
- Author `doc/agy/entry-contract-plan-report.md`.
- Move plan to `doc/agy/implemented/entry-contract-plan.md` and mark done in `doc/plans/README.adoc`.
