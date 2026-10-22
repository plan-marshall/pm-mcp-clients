# Implementation Plan: `completion-own-calls-change-map` (Own Calls & Change Map in the Completion Report)

This plan coordinates the end-to-end implementation and comprehensive verification of work package [`doc/plans/completion-own-calls-change-map.adoc`](../plans/completion-own-calls-change-map.adoc) across all client and runtime modules in `pm-mcp-clients`.

It operationalizes the technical recipe from [`doc/meta/implement_use-cases.adoc`](../meta/implement_use-cases.adoc) under the execution discipline of [`doc/agy/runbook.md`](runbook.md) and [`doc/meta/emit_rules.adoc`](../meta/emit_rules.adoc).

---

## 1. Traceability & Context

| Dimension | Reference |
|---|---|
| **Roadmap Package** | [`doc/plans/completion-own-calls-change-map.adoc`](../plans/completion-own-calls-change-map.adoc) (Roadmap Milestone 10; Client Work) |
| **Use Cases** | [`plan-marshall-documentation/doc/use-cases/03-plans.adoc#UC-PLN-6`](../../../plan-marshall-documentation/doc/use-cases/03-plans.adoc#UC-PLN-6) (Completion Report, variation *What a plan decided on its own*) |
| **Requirements** | [`PM-WF-21`](../../../plan-marshall-documentation/doc/requirements/02-workflow.adoc#PM-WF-21), [`PM-WF-11`](../../../plan-marshall-documentation/doc/requirements/02-workflow.adoc#PM-WF-11), [`PM-WF-13`](../../../plan-marshall-documentation/doc/requirements/02-workflow.adoc#PM-WF-13) |
| **Specifications** | [`01-processes-and-transport.adoc#completion-report`](../../../plan-marshall-documentation/doc/specification/runtime-model/01-processes-and-transport.adoc#completion-report), [`03-q-gate-and-execute.adoc#own-calls`](../../../pm-mcp-core/doc/specification/phase-workflows/03-q-gate-and-execute.adoc#own-calls), [`05-finalize-review-metrics-descriptor.adoc#change-map`](../../../pm-mcp-core/doc/specification/phase-workflows/05-finalize-review-metrics-descriptor.adoc#change-map) |
| **Local Client Design** | Shared: [`doc/design/shared/completion-report.adoc#layout`](../design/shared/completion-report.adoc#layout)<br>CLI: [`doc/design/terminal-client/24-reports.adoc#completion`](../design/terminal-client/24-reports.adoc#completion)<br>Web: [`doc/design/web-app/README.adoc`](../design/web-app/README.adoc) |
| **Implementation Recipe** | [`doc/meta/implement_use-cases.adoc`](../meta/implement_use-cases.adoc) |
| **Operational SOP** | [`doc/agy/runbook.md`](runbook.md) |

---

## 2. Scope & Objectives

1. **Design Updates (`doc/design/shared/completion-report.adoc#layout`)**:
   - Add the two rows *Own calls* and *Change map* to the shared completion report layout.
   - *Own calls*: apart from Decisions, shortcuts first with their limit and the condition that ends them, each with self-review judgment (`consistent` or `conflicts` with the item named).
   - *Change map*: fixed-width text diagram rendered by the runtime, `partial` or `indeterminate` with cause, and lists one step away.
2. **Contracts & API (`pm-api`)**:
   - Extend `CompletionReportResource` (or companion records) with `own_calls` list (`OwnCallRecord`: kind, description, shortcut limit/condition, judgment, conflict_target) and `change_map` (`ChangeMapResource`: diagram text, state `ok`/`partial`/`indeterminate`, cause).
   - Java 25 immutable records with canonical constructor null-checks.
   - Pure Jackson streaming codecs (`jackson-core`), zero databind.
   - Message keys in `messages/reports.{en,de}.properties` (or `messages/operator.{en,de}.properties`).
3. **Mock Runtime Scenarios (`pm-mock-runtime`)**:
   - Update `reports-and-system.json` / `running-work.json` with a fixture plan containing:
     - A design call, a shortcut, a replaced dependency.
     - A partial change map text diagram.
   - Contract tests covering a plan with own calls and one without.
4. **CLI Client Display (`pm-operator`)**:
   - Render the two rows in `PlanReportCommand` (scrollback) and in `CompletionReportLevel` (full-screen).
   - Text diagram displayed in fixed-width font without reformatting or redrawing.
5. **Web Client Display (`pm-web-app`)**:
   - Render the two rows in `CompletionReportView.svelte`.
   - Own calls list with status chips and self-review judgments.
   - Fixed-width `<pre>` block for change map text diagram.
   - Zero `fallbackMessages`, 100% catalogue strings.
6. **Governance Index**:
   - Update `doc/plans/README.adoc` marking `completion-own-calls-change-map` as `done`.

---

## 3. Concurrency & Isolation

- **Depends on**: Landed PR #154 (`web-reports-system-and-designer`), PR #158 (`mock-runtime-reports-system-designer`), and PR #160 (`cli-fullscreen-projects`).
- **Parallelizable with**: `cli-fullscreen-reports-system` and `client-profile-schema`.
- **Conflict Avoidance**: In `pm-operator`, touches `PlanReportCommand.java` and `CompletionReportLevel.java`. Coordinate with `cli-fullscreen-reports-system` if merging concurrently.

---

## 4. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [ ] 1. Isolated worktree created at `/Users/oliver/git/worktrees/pm-mcp-clients-completion-own-calls-change-map` on branch `feat/completion-own-calls-change-map` off latest `main`.
- [ ] 2. Domain skills & engineering personas loaded for each task type:
  - `persona-implementer` & `persona-module-tester` for code & unit tests
  - `persona-integration-tester` for multi-module/integration tests
  - `persona-security-expert` for text sanitization & output safety
  - `persona-documenter` for documentation & ADRs
  - `persona-code-reviewer` for finding evaluation & adversarial refutation
- [ ] 3. Incremental implementation following the 6-Phase technical recipe with local tests passing.
- [ ] 4. Final completion report written (`doc/agy/completion-own-calls-change-map-plan-report.md`) and initial plan moved to `doc/agy/implemented/completion-own-calls-change-map-plan.md`.
- [ ] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit` for code/config; skipped for standalone documentation changes).
- [ ] 6. PR opened (NEVER use `skip-bot-review` on worker PRs), review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [ ] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [ ] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/completion-own-calls-change-map-plan-report.md` by orchestrator session.

---

## 5. Phased Technical Recipe

### Phase 1: Design Alignment
- Update `doc/design/shared/completion-report.adoc#layout` adding *Own calls* and *Change map* specifications.

### Phase 2: pm-api Contract & Messages
- Implement `OwnCallRecord`, `ChangeMapRecord`, and extend `CompletionReportRecord`.
- Add streaming parser/generator in `pm-api`.
- Add message keys in `messages/reports.{en,de}.properties`.
- Add round-trip unit tests in `pm-api`.

### Phase 3: pm-mock-runtime Fixtures
- Add own calls and change map data to `reports-and-system.json`.
- Add test coverage for `GET /api/v1/plans/{plan_id}/completion` verifying both empty and populated states.

### Phase 4: pm-operator Rendering
- Update completion report rendering in `pm-operator` to output the two rows.
- Ensure terminal safety boundary sanitizes external diagram text.

### Phase 5: pm-web-app Rendering
- Update `CompletionReportView.svelte` to display own calls and change map diagram.
- Ensure Paraglide message catalogue resolution with 0 fallback messages.
- Pass `npm run check` and Vitest tests.

### Phase 6: Verification & Finalize
- Run `./mvnw verify -Ppre-commit`.
- Open PR, resolve review comments, merge to `main`.
