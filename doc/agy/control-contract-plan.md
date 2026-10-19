# Implementation Plan: `control-contract` (Controlling Work: Contract and Mock Runtime)

This implementation plan governs the implementation of the `control-contract` work package (`doc/plans/control-contract.adoc`, epic clients) in `pm-api` and `pm-mock-runtime`.

It operates strictly under the rules and lifecycle defined in [`doc/agy/runbook.md`](runbook.md).

---

## 1. Scope & Objectives

The goal is to implement the client-side contract for controlling work (plans and epics, actions, holds, pause, chat, review, decomposition) in `pm-api`, and back them with scenarios in `pm-mock-runtime`:
1. **Plan Controls & Settings (UC-CTL-1..5)**: Plan lifecycle actions (pause, start, cancel, abandon), pause boundaries (`now`, `after_task`, `after_phase`), plan settings per aspect with origin tracking, and revision-based conflict refusal (`409 conflict`).
2. **Holds & Acts (UC-CTL-4)**: Closed hold kinds (`stalled`, `condition`, `integrity_pause`, `held`, `landing_failed`), cause, attempt tracking, and resolution acts (`…/holds/{hold_id}/{act}`). Integrity pause with size/digest verification, accept/abort without diff/restore.
3. **Decomposition & Epic Actions**: Epic resource representation, workstreams, rows with status and waiting causes (`dependency`, `lock`, `scope`, `overlap`, `stale`, `held`), automatic mode switch, and decomposition draft/read-back.
4. **Chat & Planning Role Dialogue**: Chat as an action of running/paused plans and epics; transcript preservation; proposals with accept/reject read-back; chat about ended work.
5. **Findings Review**: Review item representation (id, statement, rules, origin, trust, state), dispositions (`kept`, `watch`, `dropped`, `promoted`), and drafted plan editing.
6. **Tokens, Not Money**: Token metrics per task/plan/epic, floor estimations, and `not_measured` coverage.
7. **Mock Runtime Scenarios (`control-<case>`)**: Extensive scenarios covering pause/resume, hold resolution, decomposition review, chat dialogues, conflict resolution, and token coverage.

### Target Work Package
- Specification: `doc/plans/control-contract.adoc`.
- Use Cases: `UC-CTL-1` through `UC-CTL-5`, `UC-STA-6`, `UC-PLN-5`.
- Specifications: Runtime Model § Pausing and Starting a Plan, § The Epic as a Resource, § Ended Work, § Event Catalogue; State Catalogue § Cancelling and Abandoning Archive, § Holds and Their Acts, § Chatting with the Planning Role, § Decomposition, § Review; Model Work § The Decomposition Is Reviewed, § The Epic Is Told, § An Epic's Actions, § An Epic Runs by Itself, § An Epic's Findings and Watches, § The Review; Workflow DSL § Plan Overrides; CLI & Security § Concurrent Changes of a Setting; Client Profiles § Tokens, Not Money.
- Design: `doc/design/shared/work-actions.adoc`, `doc/design/shared/epic-view.adoc`, `doc/design/shared/starting-work.adoc#decomposition`.
- Primary Modules: `pm-api` (`de.planmarshall.api.control.*`), `pm-mock-runtime`.

---

## 2. Concurrency & Isolation

This plan executes concurrently in Wave 8 alongside:
- `onboarding-contract` (`pm-api:de.planmarshall.api.onboarding.*`, `pm-mock-runtime:scenarios/onboarding-*.json`)
- `entry-contract` (`pm-api:de.planmarshall.api.entry.*`, `pm-mock-runtime:scenarios/entry-*.json`)

**Boundary Rules**:
- `pm-api`: All new classes must reside strictly within package `de.planmarshall.api.control` (and subpackages if needed). No modifications to sibling contract packages.
- `pm-mock-runtime`: All new/modified scenario files must follow the `control-*.json` naming convention. All unit tests must reside in `ControlScenarioTest.java` or `ControlContractTest.java`.
- No shared file edits across work packages; each work package owns its disjoint package hierarchy.

---

## 3. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [ ] 1. Isolated worktree created at `/Users/oliver/git/worktrees/pm-mcp-clients-control-contract` on branch `feat/control-contract`.
- [ ] 2. Domain skills & engineering personas loaded:
  - `persona-implementer` & `persona-module-tester` for Java 25 records and Jackson streaming codecs.
  - `persona-security-expert` for hold integrity boundaries, revision conflict guards, and untrusted chat content.
  - `persona-integration-tester` for mock runtime scenarios and HTTP/SSE validation.
  - `persona-code-reviewer` for review bot resolution.
- [ ] 3. Incremental implementation following the 5-Phase technical recipe with local tests passing.
- [ ] 4. Final completion report written (`doc/agy/control-contract-plan-report.md`), initial plan moved to `doc/agy/implemented/control-contract-plan.md`, and package marked done in `doc/plans/README.adoc`. (Do NOT move report to `verified/` — post-landing verification moves it).
- [ ] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit`).
- [ ] 6. PR opened (NEVER use `skip-bot-review`), review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [ ] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [ ] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/control-contract-plan-report.md` by orchestrator session.

---

## 4. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Trace & Schema** | `plan-marshall-persona-documenter` | `plan-marshall-ref-code-quality` | Map `UC-CTL-1..5`, `UC-STA-6`, `UC-PLN-5` to control schemas and action records. |
| **Phase 2: API Records & Codecs** | `plan-marshall-persona-implementer` | `pm-dev-java:java-core` | Implement immutable records in `pm-api:de.planmarshall.api.control` with streaming `jackson-core` codecs. |
| **Phase 3: Closed Catalogues & Security** | `plan-marshall-persona-security-expert` | `pm-dev-java:java-security` | Closed enumerations (`HoldKind`, `PausePoint`, `ReviewDisposition`, `EpicWaitReason`), conflict resolution. |
| **Phase 4: Mock Scenarios & Tests** | `plan-marshall-persona-integration-tester` | `pm-dev-java:junit-core` | Scenarios (`control-<case>`), mock routing, and `ControlScenarioTest`. |
| **Phase 5: Quality Gate & Review** | `plan-marshall-persona-code-reviewer` | `pm-dev-java:java-security` | Pre-commit validation, clean git status, automated review bot finding triage. |

---

## 5. Phased Technical Recipe

### Phase 1: Trace Matrix & Specification Alignment
- Map `doc/plans/control-contract.adoc` to:
  - `UC-CTL-1..5`: Pause points, hold resolution, plan settings, epic controls.
  - Holds: closed kinds (`stalled`, `condition`, `integrity_pause`, `held`, `landing_failed`).
  - Chat: dialogue turns, read-backs, transcript preservation.
  - Decomposition: draft representation, plan checks, rewrites.

### Phase 2: `pm-api` Control Records & Codecs
- Package: `de.planmarshall.api.control`.
- Records:
  - `HoldResource.java`, `HoldKind.java`, `HoldResolution.java`.
  - `PlanControlResource.java`, `PausePoint.java`, `PlanSettingsResource.java`.
  - `DecompositionResource.java`, `EpicResource.java`, `EpicWaitReason.java`.
  - `ChatTurnResource.java`, `ChatReadBackResource.java`.
  - `ReviewItemResource.java`, `ReviewDisposition.java`.
- Jackson-core streaming codecs (`writeTo`, `readFrom`, `toMap`, `fromMap`) with zero `jackson-databind`.

### Phase 3: Closed Catalogues & Fail-Closed Validation
- Closed enumerations for hold kinds, pause points, waiting reasons, dispositions.
- Verbatim comparison tests in `ControlCataloguesTest.java`.
- Strict validation of hold acts, revision monotonicity, and immutable proposal trees.

### Phase 4: Mock Runtime Scenario Implementation
- Under `pm-mock-runtime/src/main/resources/scenarios/`:
  - Enhance `control.json` and provide specialized case scenarios (`control-pause.json`, `control-holds.json`, `control-chat.json`, etc.).
  - Route handlers for `/api/v1/plans/{plan_id}/...`, `/api/v1/epics/{epic_id}/...`, `/api/v1/holds/...`.
  - Unit tests in `ControlScenarioTest.java` verifying state transitions and acts.

### Phase 5: Verification & Completion Report
- `./mvnw verify -Ppre-commit` passing cleanly.
- Author `doc/agy/control-contract-plan-report.md`.
- Move plan to `doc/agy/implemented/control-contract-plan.md` and mark done in `doc/plans/README.adoc`.
