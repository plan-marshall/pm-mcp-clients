# Implementation Plan: `operator-interaction-contract` (What Every Operator Interaction Shares: Contract and Mock Runtime)

This implementation plan governs the implementation of the `operator-interaction-contract` work package (`doc/plans/implemented/operator-interaction-contract.adoc`, epic clients) in `pm-api` and `pm-mock-runtime`.

It operates strictly under the rules and lifecycle defined in [`doc/agy/runbook.md`](../runbook.md).

---

## 1. Scope & Objectives

The goal is to implement the foundation contracts shared by all operator interactions across terminal and web clients in `pm-api`, and back them in `pm-mock-runtime`:
1. The unified operator state representation and operator link scheme (`{id, rel, method, href, params, preselected, confirm}`).
2. The event envelope and closed event catalogue (`Last-Event-ID`, `stream.closed`, per-scope event streams).
3. Configuration levels and origin tracking (`default`, `derived`, `machine`, `team`, `project`, `plan`).
4. Client inputs (`Accept-Language`, login environment) and mock runtime scenario integration.

### Target Work Package
- Specification: `doc/plans/operator-interaction-contract.adoc`.
- Requirements: `PM-ARCH-11`.
- Specifications: Hypermedia Format § Operator Representation, § Operator Links, § Texts of the Runtime; Runtime Model § Local API, § Events per Scope, § Event Catalogue, § Mock Runtime; Workflow DSL; CLI & Security; Store Schemas.
- Primary Modules: `pm-api` (`de.planmarshall.api.interaction.*`, `de.planmarshall.api.event.*`), `pm-mock-runtime`.

### Core Requirements
1. **Operator State & Link Scheme (`pm-api:de.planmarshall.api.interaction`)**:
   - `OperatorStateResource.java`: `scope`, `state_id`, `node_id`, `kind`, `level`, `progress`, `title`, `texts`, `facts`, `proposal`, `items`, `links`.
   - `OperatorLink.java`: `{id, rel, method, href, params, preselected, confirm}`.
   - Refusals: `act_not_available` (409) for stale state links; `conflict` (409) for acts against older revisions.
2. **Event Envelope & Catalogue (`pm-api:de.planmarshall.api.event`)**:
   - `EventEnvelope.java`: `{event, id, data: {event, at, scope}}`.
   - `EventName.java`: Closed enumeration of event types across runtime, plan, epic, workspace, machine, entry, reports, and task activity.
   - `StreamClosedEvent.java`: Closed enumeration of termination reasons (`normal`, `reset`, `timeout`, `shutdown`).
3. **Configuration Levels & Origin (`pm-api:de.planmarshall.api.config`)**:
   - `ConfigOrigin.java`: Closed enumeration (`default`, `derived`, `machine`, `team`, `project`, `plan`).
   - `ConfigValueResource.java`: Key, value, origin, overridden origin, revision.
4. **Mock Runtime Integration (`pm-mock-runtime`)**:
   - Scenario models updated to key operator links by `id`.
   - Event generator emitting catalogue-conforming events.
   - Mock HTTP endpoints serving state transitions and event streams.

---

## 2. Concurrency & Non-Overlapping Verification

This plan executes concurrently in Wave 7 alongside:
- `doc/agy/operator-harness-and-roles-verbs-plan.md` (Modules: `pm-operator:src/main/**`, `pm-api:resource.harness`)
- `doc/agy/cli-test-infra-plan.md` (Modules: `pm-operator:src/test/**/transcript/**`, `pm-mock-runtime:testsupport`)

### Module Disjointness Matrix
| Module / Path | `operator-interaction-contract` (This Plan) | `operator-harness-and-roles-verbs` | `cli-test-infra` | Conflict Risk |
|---|---|---|---|---|
| `pm-api:de.planmarshall.api.interaction.*` | **Sole Owner** | None | None | **Zero** |
| `pm-api:de.planmarshall.api.event.*` | **Sole Owner** | None | None | **Zero** |
| `pm-api:de.planmarshall.api.config.*` | **Sole Owner** | None | None | **Zero** |
| `pm-mock-runtime:scenario/**` | **Sole Owner** | None | Read-only Fixture | **Zero** |
| `pm-operator:src/main/**` | None | **Sole Owner** | None | **Zero** |
| `pm-operator:src/test/**/transcript/**` | None | None | **Sole Owner** | **Zero** |

---

## 3. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created on topic branch off latest `main` (`git worktree add -b feat/operator-interaction-contract /Users/oliver/git/worktrees/pm-mcp-clients-operator-interaction-contract main`).
- [x] 2. Domain skills & engineering personas loaded for each task type:
  - `persona-implementer` & `persona-module-tester` for Java 25 records and Jackson streaming codecs
  - `persona-security-expert` for closed enumerations, input sanitization, and fail-closed parser boundaries
  - `persona-documenter` for documentation, ADRs, and roadmap updates
  - `persona-code-reviewer` for finding evaluation & adversarial refutation
- [x] 3. Incremental implementation following the 6-Phase technical recipe with local tests passing.
- [x] 4. Final completion report written (`doc/agy/operator-interaction-contract-plan-report.md`), initial plan moved to `doc/agy/implemented/operator-interaction-contract-plan.md`, and package marked done in `doc/plans/README.adoc`. (Do NOT move report to `verified/` — post-landing verification moves it).
- [x] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit`).
- [x] 6. PR opened (NEVER use `skip-bot-review`), 5-minute review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [x] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [x] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/operator-interaction-contract-plan-report.md` by orchestrator session.

---

## 4. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Trace & Schema** | `plan-marshall-persona-documenter` | `plan-marshall-ref-code-quality` | Map `PM-ARCH-11` and Hypermedia Format to Java records and closed enums. |
| **Phase 2: Operator State & Links** | `plan-marshall-persona-implementer` | `pm-dev-java:java-core` | Implement `OperatorStateResource`, `OperatorLink`, and Jackson streaming codecs. |
| **Phase 3: Event Envelope & Streams** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-security-expert` | `pm-dev-java:java-core`<br>`pm-dev-java:java-security` | Implement `EventEnvelope`, closed `EventName`, and `StreamClosedEvent`. |
| **Phase 4: Config Levels & Origins** | `plan-marshall-persona-implementer` | `pm-dev-java:java-core` | Implement `ConfigOrigin` and `ConfigValueResource` with revision tracking. |
| **Phase 5: Mock Runtime Integration** | `plan-marshall-persona-module-tester` | `pm-dev-java:cui-cui-testing` | Wire interaction models into `pm-mock-runtime` scenarios and mock endpoints. |
| **Phase 6: Quality Gate & Review** | `plan-marshall-persona-code-reviewer` | `pm-dev-java:java-security` | Pre-commit validation, verify zero warnings, review bot finding resolution. |

---

## 5. Phased Technical Recipe

### Phase 1: Trace Matrix & Specification Alignment
- Map `doc/plans/operator-interaction-contract.adoc` to:
  - `PM-ARCH-11`: Shared operator interaction state and link schema.
  - Hypermedia Format § Operator Representation and § Operator Links.
  - Runtime Model § Event Catalogue and § Local API.

### Phase 2: Operator State & Link Records
- Implement `OperatorLink.java`:
  - `id`: unique link identifier.
  - `rel`: relation name.
  - `method`: HTTP method (`GET`, `POST`, `PUT`, `DELETE`).
  - `href`: target endpoint URI.
  - `params`: optional parameter map.
  - `preselected`: boolean default selection flag.
  - `confirm`: optional confirmation descriptor.
- Implement `OperatorStateResource.java`:
  - Immutable record with full metadata, facts, proposal, and links.
  - Streaming codecs without `jackson-databind`.

### Phase 3: Event Catalogue & Streaming Envelopes
- Implement closed `EventName.java` enumeration covering all runtime scopes.
- Implement `EventEnvelope.java` and `StreamClosedEvent.java`.
- Validate SSE framing and `Last-Event-ID` parsing.

### Phase 4: Configuration Hierarchy & Origins
- Implement `ConfigOrigin.java` enum (`default`, `derived`, `machine`, `team`, `project`, `plan`).
- Implement `ConfigValueResource.java` capturing value, origin, overridden origin, and revision.

### Phase 5: Mock Runtime Updates
- Update `MockRuntimeEngine` in `pm-mock-runtime` to serve state responses using `OperatorStateResource` and keyed `OperatorLink` IDs.
- Add test scenarios verifying client state transitions and event emissions.

### Phase 6: Verification & Report
- Run `./mvnw verify -Ppre-commit`.
- Archive plan to `doc/agy/implemented/operator-interaction-contract-plan.md`.
- Mark package `done` in `doc/plans/README.adoc`.
- Author completion report at `doc/agy/operator-interaction-contract-plan-report.md`.
