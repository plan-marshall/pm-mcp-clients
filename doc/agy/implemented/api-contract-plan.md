# Implementation Plan: `api-contract` (Milestone 2 API Contract in `pm-api`)

This implementation plan governs the implementation of the `api-contract` work package (`doc/plans/api-contract.adoc`, Roadmap Milestone 2) in `pm-api`.

It operates strictly under the rules and lifecycle defined in [`doc/agy/runbook.md`](../runbook.md).

---

## 1. Scope & Objectives

The goal is to formalize and deliver the core daemon API resource contracts, link relations, and closed error codes for `/api/v1` in `pm-api` using the streaming API of `jackson-core` without `jackson-databind`.

### Target Work Package
- Specification: `doc/plans/api-contract.adoc` (Roadmap Milestone 2).
- Requirements: `PM-ARCH-1`, `PM-IMPL-1`, `PM-IMPL-3`.
- Primary Module: `pm-api` (`de.planmarshall.api.resource`).

### Core Requirements
1. **Daemon Resource Records**:
   - `RuntimeStatusResource.java`: runtime state, version, PID, start time, socket path, links.
   - `WebAccessResource.java`: web UI status, port, loopback-only flag, paired devices count, links.
   - `RuntimeStopResource.java`: stop mode (immediate / drain), timeout, links.
   - `CredentialStatusResource.java`: credential availability, token path, expiry, links.
   - `RuntimeEventStreamResource.java`: event stream status, active listeners, last event ID, links.
2. **Link Relations & Structure**:
   - `LinkRelation.java`: closed set of link relations (`self`, `status`, `stop`, `web`, `credentials`, `events`).
   - `ResourceLink.java`: href, rel, method, templated.
3. **Closed Error Codes Enumeration**:
   - `ApiErrorCode.java`: closed list of local API error codes with their HTTP status:
     - `bad_request` (400), `unauthorized` (401), `forbidden` (403), `not_found` (404), `conflict` (409), `precondition_failed` (412), `aspect_owned` (409), `onboarding_incomplete` (412), `act_not_available` (409), `rate_limit_exceeded` (429), `internal_error` (500).
   - Fails closed on unknown codes read from the wire.
4. **Jackson-Core Streaming Codecs**:
   - Pure `jackson-core` streaming serialization and deserialization (`JsonParser`, `JsonGenerator`).
   - Zero `jackson-databind` usage (enforced by enforcer rule `jackson-core-only`).
5. **TOON Decision**:
   - Resolve `PM-WATCH-MOD-4`: JSON-only local API over Unix socket. Document resolution in documentation and completion report.

---

## 2. Concurrency & Non-Overlapping Verification

This plan executes concurrently in Wave 6 alongside:
- `doc/agy/installation-layout-plan.md` (Modules: `pm-api:de.planmarshall.api.layout`, `pm-operator:layout`)
- `doc/agy/integration-layers-plan.md` (Modules: `pm-relay`, `pm-operator:it`)

### Module Disjointness Matrix
| Module / Path | `api-contract` (This Plan) | `installation-layout` | `integration-layers` | Conflict Risk |
|---|---|---|---|---|
| `pm-api:de.planmarshall.api.resource.*` | **Sole Owner** | None | None | **Zero** |
| `pm-api:de.planmarshall.api.layout.*` | None | **Sole Owner** | None | **Zero** |
| `pm-operator:layout/**` | None | **Sole Owner** | None | **Zero** |
| `pm-relay/**` | None | None | **Sole Owner** | **Zero** |
| `pm-operator:it/**` | None | None | **Sole Owner** | **Zero** |
| `pm-mock-runtime/**` | None | None | None | **Zero** |
| `pm-web-app/**` | None | None | None | **Zero** |

---

## 3. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created on topic branch off latest `main` (`git worktree add -b feat/api-contract /Users/oliver/git/worktrees/pm-mcp-clients-api-contract main`).
- [x] 2. Domain skills & engineering personas loaded for each task type:
  - `persona-implementer` & `persona-module-tester` for Java 25 records and Jackson streaming codecs
  - `persona-security-expert` for strict input validation, closed error code boundaries, and fail-closed parsing
  - `persona-documenter` for documentation, ADRs, and roadmap updates
  - `persona-code-reviewer` for finding evaluation & adversarial refutation
- [x] 3. Incremental implementation following the 6-Phase technical recipe with local tests passing.
- [x] 4. Final completion report written (`doc/agy/api-contract-plan-report.md`), initial plan moved to `doc/agy/implemented/api-contract-plan.md`, and package marked done in `doc/plans/README.adoc`. (Do NOT move report to `verified/` — post-landing verification moves it).
- [x] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit`).
- [x] 6. PR opened (with `--label "skip-bot-review"` if authorized), 5-minute review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [ ] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [ ] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/api-contract-plan-report.md` by orchestrator session.

---

## 4. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Trace & Schema** | `plan-marshall-persona-documenter` | `plan-marshall-ref-code-quality` | Map `PM-ARCH-1`, `PM-IMPL-1`, `PM-IMPL-3`, and Runtime Model § Local API to resource contracts. |
| **Phase 2: Error Codes & Links** | `plan-marshall-persona-implementer` | `pm-dev-java:java-core` | Implement `ApiErrorCode.java` with HTTP status mapping and `LinkRelation.java` closed enum. |
| **Phase 3: Resource Records** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-module-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:junit-core` | Immutable Java 25 records with streaming codecs (`RuntimeStatusResource`, `WebAccessResource`, etc.). |
| **Phase 4: Streaming Codecs** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-security-expert` | `pm-dev-java:java-core`<br>`pm-dev-java:java-security` | Fail-closed Jackson-core streaming codecs without databind. |
| **Phase 5: Unit & Boundary Tests** | `plan-marshall-persona-module-tester` | `pm-dev-java:junit-core` | Round-trip tests, unknown error code refusal, enforcer guard validation. |
| **Phase 6: Quality Gate & Review** | `plan-marshall-persona-code-reviewer` | `pm-dev-java:java-security` | Full unit test verification, pre-commit validation, zero compiler warnings. |

---

## 5. Phased Technical Recipe

### Phase 1: Trace Matrix & Specification Alignment
- Map `doc/plans/api-contract.adoc` to:
  - `PM-ARCH-1`: Local daemon API format.
  - `PM-IMPL-1`: Jackson streaming without databind.
  - `PM-IMPL-3`: Closed enumeration of API error codes.
  - Resolve `PM-WATCH-MOD-4`: JSON-only wire format for local socket API.

### Phase 2: Error Codes & Link Relations
- Implement `ApiErrorCode.java`:
  - Enumerate all specification error codes with status: `BAD_REQUEST`(400), `UNAUTHORIZED`(401), `FORBIDDEN`(403), `NOT_FOUND`(404), `CONFLICT`(409), `PRECONDITION_FAILED`(412), `ASPECT_OWNED`(409), `ONBOARDING_INCOMPLETE`(412), `ACT_NOT_AVAILABLE`(409), `RATE_LIMIT_EXCEEDED`(429), `INTERNAL_ERROR`(500).
  - String wire mapping: `fromWire(String code)` throws `IllegalArgumentException` on unknown string (fail-closed).
- Implement `LinkRelation.java` and `ResourceLink.java`.

### Phase 3: Resource Contracts & Codecs
- Implement resource records in `de.planmarshall.api.resource`:
  - `RuntimeStatusResource.java`
  - `WebAccessResource.java`
  - `RuntimeStopResource.java`
  - `CredentialStatusResource.java`
  - `RuntimeEventStreamResource.java`
- Implement Jackson-core streaming serialization/deserialization for each record.

### Phase 4: Unit Testing & Boundary Verification
- Author `ApiContractTest.java`:
  - Round-trip serialization/deserialization for each resource record.
  - Comparison of `ApiErrorCode` values with specification catalogue.
  - Unknown error code read test: verifies refusal.
  - Malformed JSON handling: verifies clean exception.

### Phase 5: Documentation & Archival
- Move `doc/plans/api-contract.adoc` to `doc/plans/implemented/api-contract.adoc`.
- Mark `api-contract` as `done` in `doc/plans/README.adoc`.
- Write completion report `doc/agy/api-contract-plan-report.md`.
- Move plan to `doc/agy/implemented/api-contract-plan.md` and update `doc/agy/implemented/README.md`.

---

## 6. Verification Commands

```bash
# Compile and run unit tests in pm-api
./mvnw test -pl pm-api -Dtest=*ApiContract*,*Resource*

# Run full pre-commit verification
./mvnw verify -Ppre-commit
```
