# Implementation Plan: `cli-fullscreen-plan-view` (Full-Screen Plan View in `pm-operator`)

This implementation plan governs the completion of the full-screen Plan View (`UC-PLN-1: Follow a Plan Down to Its Parts`, `doc/plans/implemented/cli-plan-view.adoc`) in `pm-operator`, building on [`doc/agy/cli-fullscreen-foundation-plan.md`](cli-fullscreen-foundation-plan.md) and [`doc/agy/cli-fullscreen-overview-plan.md`](cli-fullscreen-overview-plan.md).

It operationalizes [`doc/meta/implement_use-cases.md`](../meta/implement_use-cases.md) under the execution discipline of [`doc/agy/runbook.md`](runbook.md).

---

## 1. Traceability & Context

| Dimension | Reference |
|---|---|
| **Use Case** | [`plan-marshall-documentation/doc/use-cases/03-plans.adoc#UC-PLN-1`](../../../plan-marshall-documentation/doc/use-cases/03-plans.adoc#UC-PLN-1) |
| **Specifications** | [`process-contract.adoc`](../plans/implemented/process-contract.adoc), [`06-event-catalogue.adoc`](../../../plan-marshall-documentation/doc/specification/runtime-model/06-event-catalogue.adoc) |
| **Local Client Design** | Shared: [`doc/design/shared/plan-view.adoc`](../design/shared/plan-view.adoc)<br>CLI: [`doc/design/terminal-client/16-plan-view.adoc`](../design/terminal-client/16-plan-view.adoc) (`#full-screen`, `#full-screen-tree`, `#full-screen-sub`, `#full-screen-leaf`, `#activity`, `#full-screen-keys`, `#full-screen-live`, `#ended`) |
| **Roadmap Package** | [`doc/plans/implemented/cli-plan-view.adoc`](../plans/implemented/cli-plan-view.adoc) |
| **Prerequisite Plans** | [`doc/agy/cli-fullscreen-foundation-plan.md`](cli-fullscreen-foundation-plan.md), [`doc/agy/cli-fullscreen-overview-plan.md`](cli-fullscreen-overview-plan.md), [`doc/agy/mock-runtime-running-work-plan.md`](mock-runtime-running-work-plan.md) |
| **Implementation Recipe** | [`doc/meta/implement_use-cases.md`](../meta/implement_use-cases.md) |
| **Operational SOP** | [`doc/agy/runbook.md`](runbook.md) |

---

## 2. Scope & Objectives

1. **Level 3 — Plan Process Tree in the Full-Screen View (`PlanViewLevel`)**:
   - Opens on `Enter` from a plan row in the Overview (`ProjectWorkLevel`), an Epic View (`EpicViewLevel`), Ended Work (`EndedWorkLevel`), or Reports (`ReportsLevel`), or when `pm-operator plan <plan_id>` is invoked interactively in the console without scrollback-only flags (`16-plan-view.adoc#full-screen`).
   - Header path: `<project_id> › <epic_id> › <plan_id> › <selected_node>` (or `<project_id> › <plan_id> › <selected_node>` when the plan belongs to no epic), plus state, hold kind (if held), tokens so far (`≥ 410 k tokens` / `none yet`), and PR `#<n>`.
   - Expandable/collapsible tree of the six phases and lane-selected phases (with the land lane's `land` shown as a sub-workflow inside `execute`), sub-workflows marked `sub-workflow`, and phase/node states from the catalogue.
2. **Navigation & Action Keys (`16-plan-view.adoc#full-screen-keys`)**:
   - `→`/`l` opens the selected node one level; `←`/`h` closes the selected node or moves to its parent; `*` opens every level below the selected node; `.` jumps to where the plan currently executes; `n` shows or hides nodes not in this plan's lane.
   - `Enter` on a leaf toggles its inline details (`#full-screen-leaf`); on a task of a leaf opens its live worker activity level (`#activity`); on a node waiting for the operator leaves/suspends the view and opens the decision (`answer`).
   - `t` toggles the token breakdown by phase and by role with raw components (`--tokens [--components]` equivalent).
   - `p` jumps in one key to the plan's project in the Overview (`ProjectWorkLevel`); `e` jumps in one key to the plan's epic view (omitted from footer when the plan has no epic); `c` on an ended plan opens its completion report level; `a` opens the plan's actions (or hold resolution acts on a held node); `Esc` returns to the exact level from which the plan view was opened (project, epic view, ended work, or reports).
3. **Worker Activity Level (`WorkerActivityLevel`, `16-plan-view.adoc#activity`)**:
   - Displays the task's role, harness, model, attempt, duration, and tokens in the sub-header, and streams the worker's activity lines live as plain sanitized text (`OutputSafety.stripControlSequences`), never interpreted as markup or terminal control codes.
   - Displays off-track signal and server remediation when a worker is flagged off track; `Enter` on an off-track plan in the Overview lands directly here.
4. **Ended Plan View (`16-plan-view.adoc#ended`)**:
   - Read-only full-screen view of an ended plan showing `ended: <outcome>` in the header, the tree as it ended, `c` for its completion report, `p` and `e` for project/epic navigation, and `a` offering _Chat…_ alone.
5. **Live SSE Updates & Selection Anchoring**:
   - Handles process/task/token SSE events (`phase.changed`, `activity.changed`, `task.*`, `tokens.*`, `needs_operator.changed`, `hold.*`), keeping the selection anchored on its node ID while the tree opens, closes, or advances.
6. **Screen Tests (JVM & Native)**:
   - Every scenario of `process-contract` verified both as a `plan` transcript test and as a `VirtualScreenTerminal` screen test of the full-screen view, including reaching `p` (project) and `e` (epic) in one key from overview, epic view, and ended work, and returning via `Esc`.

---

## 3. Concurrency & Isolation

- **Depends on**: [`doc/agy/cli-fullscreen-foundation-plan.md`](cli-fullscreen-foundation-plan.md), [`doc/agy/cli-fullscreen-overview-plan.md`](cli-fullscreen-overview-plan.md), and [`doc/agy/mock-runtime-running-work-plan.md`](mock-runtime-running-work-plan.md).
- **Parallelizable with**: `cli-fullscreen-projects`, `cli-fullscreen-reports-system`, and all `web-*` plans.

### Boundary Rules
- **Sole Owner of**: `de.planmarshall.operator.planview.*` and `PlanCommand.java` full-screen launch integration in `pm-operator`; registers `PlanViewLevel` into the `OverviewFullScreenApp` level router.
- **Read-Only Consumer of**: `pm-mock-runtime` (`running-work.json` process states owned by `mock-runtime-running-work-plan.md`) and `pm-api` message bundles (`messages/operator.{en,de}.properties` and `messages/plan_view.{en,de}.properties`).
- **No Touch**: `doc/plans/README.adoc`, `pm-mock-runtime`, and `pm-web-app`.

---

## 4. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created at `/Users/oliver/git/worktrees/pm-mcp-clients-cli-fullscreen-plan-view` on branch `feat/cli-fullscreen-plan-view` off latest `main`.
- [x] 2. Domain skills & engineering personas loaded where applicable:
  - `persona-implementer` & `persona-module-tester` for code & unit tests
  - `persona-integration-tester` for multi-module/integration tests
  - `persona-security-expert` for trust boundaries & input validation
  - `persona-documenter` for documentation & ADRs
  - `persona-code-reviewer` for finding evaluation & adversarial refutation
- [x] 3. Incremental implementation with tests following the phased technical recipe.
- [x] 4. Final completion report written (`doc/agy/cli-fullscreen-plan-view-plan-report.md`) and initial plan moved to `doc/agy/implemented/cli-fullscreen-plan-view-plan.md`. (Do NOT move report to `verified/` — Step 8 is reserved for orchestrator).
- [x] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit` for code/config; skipped for standalone documentation).
- [ ] 6. PR opened (NEVER use `skip-bot-review` on worker PRs; reserved for orchestrator docs PRs), review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [ ] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [ ] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/cli-fullscreen-plan-view-plan-report.md` by orchestrator session.

---

## 5. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Catalogue & Contract Check** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-integration-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:junit-core` | Verify `operator.plan.*` keys in `16-plan-view.adoc#plan-texts` and `running-work#process.*` states. |
| **Phase 2: Plan Tree View Model** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-module-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:junit-core` | Expand/collapse tree model, `.` jump to active node, `*` expand all, `n` toggle non-lane nodes, `t` token panel. |
| **Phase 3: Leaf & Worker Activity** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-security-expert` | `pm-dev-java:java-core`<br>`pm-dev-java:java-security` | Leaf details, `WorkerActivityLevel` live plain-text rendering with `OutputSafety`, off-track jump. |
| **Phase 4: Cross-Level Navigation** | `plan-marshall-persona-implementer` | `pm-dev-java:java-core` | Wire `p` (project), `e` (epic), `c` (completion report), `Enter` (decision hand-off), and `Esc` caller return. |
| **Phase 5: Screen & Process Tests** | `plan-marshall-persona-integration-tester` | `pm-dev-java:junit-integration` | `VirtualScreenTerminal` tests for all `process-contract` scenarios, tree keys, and 1-key `p`/`e` jumps. |
| **Phase 6: Review Gate** | `plan-marshall-persona-code-reviewer` | `pm-dev-java:java-security` | Pre-commit gate, `LiteralTextGuardTest`, and 100% resolution of automated review findings. |

---

## 6. Phased Technical Recipe

### Phase 1: Catalogue & `running-work` Process States Verification
- [x] **1.1 Message Catalogue & Scenario Audit**:
  - Verify all keys in `doc/design/terminal-client/16-plan-view.adoc#plan-texts` (`operator.plan.keys.*`, `operator.plan.header.*`, `operator.plan.leaf.*`, `operator.plan.activity.*`, `operator.plan.tokens.*`) exist in `pm-api/src/main/resources/messages/operator.{en,de}.properties` and `plan_view.{en,de}.properties`, and verify the `process.*` states in `running-work.json` (delivered by `mock-runtime-running-work-plan.md`).

### Phase 2: `PlanViewLevel` Tree Model (`de.planmarshall.operator.planview`)
- [x] **2.1 Hierarchical Node State**:
  - Model the plan's phases, sub-workflows (including `land.*` under `execute` for the land lane), tasks, and leaf details from `PlanViewResource`, opening by default down to the currently active node.
  - Implement `→`/`l` (open node), `←`/`h` (close node or move to parent), `*` (open all descendants), `.` (focus active node), `n` (show/hide skipped lane phases), and `t` (toggle token breakdown table by phase and role with floor `≥` and unmeasured count).
- [x] **2.2 Header & Breadcrumb Formatting**:
  - Format header path `<project> › [<epic> ›] <plan> › <node>` and sub-header with state, hold kind, token total + split, and PR number.
  - Hide `e` in the footer when the plan has no parent epic (`epicId == null`), and show `c` in the footer when the plan has ended.

### Phase 3: Leaf Details & `WorkerActivityLevel`
- [x] **3.1 Leaf Expansion & Decision Hand-Off**:
  - On `Enter` on a leaf node, toggle inline leaf details (inputs, outputs, checks, duration, tokens); on a node waiting for the operator, suspend/leave the view and invoke `AnswerCommand` for that decision.
- [x] **3.2 `WorkerActivityLevel` (Live Task Stream)**:
  - On `Enter` on a task (or when entering an off-track plan from the Overview), push `WorkerActivityLevel` showing role, harness, model, attempt, duration, tokens, off-track banner (if any), and sanitized plain-text activity lines (`OutputSafety.stripControlSequences`), auto-scrolling at bottom unless the operator scrolls up.

### Phase 4: Cross-Level Jumps (`p`, `e`, `c`, `Esc`)
- [x] **4.1 One-Key Jumps & Caller Return**:
  - Wire `p` to push/jump to `ProjectWorkLevel` for the plan's project; wire `e` to push/jump to `EpicViewLevel` for the plan's epic (or pop back if opened from that epic); wire `c` on an ended plan to open its completion report view.
  - Ensure `Esc` from `PlanViewLevel` always returns to the exact level that opened it (Overview project level, Epic view, Ended work, or Reports).

### Phase 5: Screen Tests & Integration Verification
- [x] **5.1 `PlanViewFullScreenTest` (`VirtualScreenTerminal`)**:
  - Verify all `process-contract` scenarios in the full-screen view: phase tree expansion/collapse (`→`, `←`, `*`), `.` jump to active node, `n` non-lane nodes toggle, `t` token breakdown toggle, leaf detail expansion, and `WorkerActivityLevel` plain-text sanitization.
  - Verify live SSE tree update preserving selection on the selected node ID.
  - Verify reaching `p` (project) and `e` (epic) in one key from a plan opened in (a) the overview, (b) an epic's view, and (c) ended work, and `Esc` returning to the origin in each case.
- [x] **5.2 Pre-Commit & Operational Gate**:
  - Run `./mvnw verify -Ppre-commit` and execute PR/review workflow per [`doc/agy/runbook.md`](runbook.md).
