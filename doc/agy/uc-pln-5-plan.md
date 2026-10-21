# Implementation Plan: UC-PLN-5 Follow an Epic (Epic View, Workstreams, Sources, Landings, and Review)

This plan coordinates the end-to-end implementation and comprehensive verification of use case `UC-PLN-5: Follow an Epic` across all client and runtime modules in `pm-mcp-clients`.

It operationalizes the technical recipe from [`doc/meta/implement_use-cases.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/meta/implement_use-cases.adoc) under the execution discipline of [`doc/agy/runbook.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/runbook.md) and [`doc/meta/emit_rules.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/meta/emit_rules.adoc).

---

## 1. Traceability & Context

| Dimension | Reference |
|---|---|
| **Use Case** | [`plan-marshall-documentation/doc/use-cases/03-plans.adoc#UC-PLN-5`](file:///Users/oliver/git/plan-marshall-documentation/doc/use-cases/03-plans.adoc) (`UC-PLN-5: Follow an Epic`) |
| **Specifications** | [`01-processes-and-transport.adoc#epic-resource`](file:///Users/oliver/git/plan-marshall-documentation/doc/specification/runtime-model/01-processes-and-transport.adoc), [`#event-streams`](file:///Users/oliver/git/plan-marshall-documentation/doc/specification/runtime-model/01-processes-and-transport.adoc), [`04-epic-and-change-ledger.adoc`](file:///Users/oliver/git/pm-mcp-core/doc/specification/store-schemas/04-epic-and-change-ledger.adoc), [`08-epic-coordination.adoc`](file:///Users/oliver/git/pm-mcp-core/doc/specification/model-work/08-epic-coordination.adoc), [`01-profile-schema.adoc#token-usage`](file:///Users/oliver/git/pm-mcp-core/doc/specification/client-profiles/01-profile-schema.adoc) |
| **Local Client Design** | Shared: [`doc/design/shared/epic-view.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/design/shared/epic-view.adoc)<br>CLI: [`doc/design/terminal-client/19-actions.adoc#epic-view`](file:///Users/oliver/git/pm-mcp-clients/doc/design/terminal-client/19-actions.adoc)<br>Web: [`doc/design/web-app/README.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/design/web-app/README.adoc), [`doc/design/shared/work-actions.adoc#epic-actions`](file:///Users/oliver/git/pm-mcp-clients/doc/design/shared/work-actions.adoc) |
| **Roadmap Packages** | [`doc/plans/implemented/control-contract.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/plans/implemented/control-contract.adoc), [`doc/plans/implemented/cli-actions.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/plans/implemented/cli-actions.adoc), [`doc/plans/implemented/web-actions.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/plans/implemented/web-actions.adoc) |
| **Implementation Recipe** | [`doc/meta/implement_use-cases.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/meta/implement_use-cases.adoc) |
| **Operational SOP** | [`doc/agy/runbook.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/runbook.md) |

---

## 2. Parallel Isolation & Non-Overlap Invariants

`UC-PLN-5` executes concurrently with `mock-runtime-reports-system-designer` (Stream A, `pm-mock-runtime` scenarios 4 & 5) and `cli-fullscreen-projects` (Stream B, `pm-operator` Level 2 projects & enrolment). Strict non-overlap is guaranteed across modules:

1. **Contracts & API (`pm-api`)**:
   - `UC-PLN-5`: Owns `de.planmarshall.api.control.EpicResource`, `EpicWorkstream`, `EpicTokens`, `EpicEvent`, and `messages/epic_view.{en,de}.properties`.
   - `mock-runtime-reports-system-designer`: Modifies only report and system/designer scenario models.
   - `cli-fullscreen-projects`: Touches `de.planmarshall.operator.projects.*`.
   - Zero shared files or conflicting types.
2. **Mock Runtime (`pm-mock-runtime`)**:
   - `UC-PLN-5`: Verifies and consumes epic fixtures in `running-work.json` and `fully-onboarded.json` via read-only GET routes (`GET /api/v1/epics/{id}`, `GET /api/v1/epics/{id}/events`).
   - Does not touch `reports-and-system.json` or `designer.json`.
3. **CLI Client (`pm-operator`)**:
   - `UC-PLN-5`: Verifies existing `EpicViewLevel.java` and `EpicCommand.java` without colliding with `de.planmarshall.operator.projects.*`.
4. **Web Client (`pm-web-app`)**:
   - `UC-PLN-5`: Focuses on `EpicView.svelte`, `EpicWorkstreamCard.svelte`, and breadcrumb navigation.
5. **E2E & Integration Tests**:
   - `UC-PLN-5`: Delivers dedicated Playwright test suite `pm-web-e2e/tests/epic-view.spec.js` and validates operator process tests (`ControlEpicTranscriptTest.java`).

---

## 3. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created at `/Users/oliver/git/worktrees/pm-mcp-clients-uc-pln-5` on branch `feat/uc-pln-5-follow-epic` off latest `main`.
- [ ] 2. Domain skills & engineering personas loaded for each task type:
  - `persona-implementer` & `persona-module-tester` for code & unit tests
  - `persona-integration-tester` for multi-module/integration tests
  - `persona-security-expert` for untrusted text sanitization & output safety
  - `persona-documenter` for documentation & ADRs
  - `persona-code-reviewer` for finding evaluation & adversarial refutation
- [ ] 3. Incremental implementation following the 6-Phase technical recipe with local tests passing.
- [ ] 4. Final completion report written (`doc/agy/uc-pln-5-plan-report.md`) and initial plan moved to `doc/agy/implemented/uc-pln-5-plan.md`.
- [ ] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit` for code/config; skipped for standalone documentation changes).
- [ ] 6. PR opened (NEVER use `skip-bot-review` on worker PRs), review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [ ] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [ ] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/uc-pln-5-plan-report.md` by orchestrator session.

---

## 4. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Trace** | `plan-marshall-persona-documenter` | `plan-marshall-ref-code-quality` | Verify epic resource schema, workstreams, status flags, landings, findings/watches, and catalogue texts. |
| **Phase 2: `pm-api`** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-module-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:junit-core` | Immutable Java 25 records, Jackson streaming parsing, bilingual resource bundles (`epic_view.{en,de}.properties`). |
| **Phase 3: Mock Runtime** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-integration-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:cui-cui-http-testing` | Ensure `running-work.json` and `fully-onboarded.json` fixtures supply all required UC-PLN-5 fields and events. |
| **Phase 4: `pm-operator`** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-security-expert` | `pm-dev-java:java-core`<br>`pm-dev-java:java-security` | `EpicViewLevel` screen rendering, tree expansion, token split, and terminal output sanitization. |
| **Phase 5: `pm-web-app`** | `plan-marshall-persona-implementer`<br>`pm-dev-frontend:javascript` | `pm-dev-frontend:css`<br>`pm-dev-frontend:jest-testing` | `EpicView.svelte` layout (head, workstreams, sources, landings, findings, tokens, events), responsive mobile/desktop views. |
| **Phase 6: Verification** | `plan-marshall-persona-integration-tester` | `pm-dev-java:junit-integration` | Author dedicated `pm-web-e2e/tests/epic-view.spec.js` Playwright suite, verify process ITs and reactor gate. |
| **Phase 7: Review Gate** | `plan-marshall-persona-code-reviewer` | `pm-dev-java:java-security` | Adversarial refutation of candidate bot findings, 100% resolution bar. |

---

## 5. Phased Technical Recipe

### Phase 1: Traceability & Design Alignment
- [ ] **1.1 Design Specification Audit**: Review `doc/design/shared/epic-view.adoc`, `plan-marshall-documentation/doc/use-cases/03-plans.adoc#UC-PLN-5`, and `doc/plans/implemented/control-contract.adoc`:
  - Verify layout: head (path `project › epic`, title, state, automatic mode, concurrency, needs-operator count, actions, next plan), workstreams (open/closed, plan status, waits, overlap, jump to plan view), sources, landings, findings/watches, tokens (floor `≥`, coverage, per-plan split), and live event log.
  - Verify variations: ended epic (from Ended Work with read-only chat), epic waiting for operator, and epic intent.
  - Audit message catalogue coverage in `pm-api/src/main/resources/messages/epic_view.{en,de}.properties`.

### Phase 2: `pm-api` Contracts & Message Catalogue
- [ ] **2.1 Epic Resource & Workstream Records**:
  - Verify that `EpicResource`, `EpicWorkstream`, `EpicTokens`, `EpicEvent`, and `EpicWaitReason` in `de.planmarshall.api.control` fully cover all UC-PLN-5 requirements with immutable Java 25 records and Jackson streaming codecs.
- [ ] **2.2 Bilingual Catalogue Verification**:
  - Ensure all epic view titles, section headers, badges, and status labels exist in both `messages/epic_view.en.properties` and `messages/epic_view.de.properties`.
  - Add unit tests verifying 100% key parity and ICU token parameters.

### Phase 3: `pm-mock-runtime` Scenarios & Fixture Verification
- [ ] **3.1 Epic Fixtures in Canonical Scenarios**:
  - Verify that `running-work.json` and `fully-onboarded.json` provide realistic epic models (`relay-hardening`, `clients-epic`) with multiple workstreams, mixed plan statuses (shipped, running, paused, staged with overlap waits), sources, landings with PR links, findings/watches counts, and token figures.
  - Assert that `GET /api/v1/epics/{id}` and SSE event streams deliver clean payloads with zero schema warnings.

### Phase 4: `pm-operator` (CLI Client) Verification & Polish
- [ ] **4.1 TUI Screen & Command Verification**:
  - Verify `EpicViewLevel` renders the full UC-PLN-5 layout in the terminal: path `project › epic`, head with state, mode, plans running/allowed, workstreams tree, sources, landings, findings/watches, token split with `--tokens`, and newest events.
  - Verify keyboard shortcuts: `p` (jump to project), `a` (modal actions menu), `Enter` on plan row (jump to `PlanViewLevel`), `Esc` (return to caller).
  - Ensure all external text passes through `OutputSafety`.

### Phase 5: `pm-web-app` (Web Client) Polish & Unit Tests
- [ ] **5.1 Epic View Component Structure**:
  - Verify `EpicView.svelte` and related subcomponents render cleanly across desktop (>1024px) and mobile (320px) viewports.
  - Ensure 1-step navigation links to project (`/projects`) and plans (`/plans/{id}`).
  - Confirm token formatting handles floor (`≥`), coverage percentage, and per-plan expansion without horizontal scrollbars.
- [ ] **5.2 Svelte Component Tests**:
  - Add or expand `EpicView.test.ts` verifying rendering of workstreams, landing PR links, findings/watches counts, and SSE event updates.

### Phase 6: Integration Testing & Verification Sweep
- [ ] **6.1 Playwright E2E Suite (`epic-view.spec.js`)**:
  - Author comprehensive `pm-web-e2e/tests/epic-view.spec.js` running against `running-work` and `fully-onboarded` mock scenarios:
    1. Navigate to epic view from Running Work overview.
    2. Verify path breadcrumbs and jump back to project.
    3. Verify workstream expansion/collapse and staged plan wait reasons.
    4. Verify 1-step navigation from plan row into Plan View, and back.
    5. Verify landings card with PR external link.
    6. Verify findings and watches review trigger.
    7. Verify live SSE event arrival updates the event list.
- [ ] **6.2 Operator Process ITs**:
  - Run `PmOperatorProcessIT` and `ControlEpicTranscriptTest` to ensure clean terminal execution.
- [ ] **6.3 Reactor Verification Gate**:
  - Run `./mvnw verify -Ppre-commit` and ensure zero failures across all modules.
