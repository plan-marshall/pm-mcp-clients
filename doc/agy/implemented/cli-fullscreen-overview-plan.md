# Implementation Plan: `cli-fullscreen-overview` (Full-Screen Overview `plans` in `pm-operator`)

This implementation plan governs the completion of the full-screen `plans` view for `UC-PLN-4: See What Runs, Across Projects` (`doc/plans/implemented/cli-overview.adoc`) in `pm-operator`, building on [`doc/agy/cli-fullscreen-foundation-plan.md`](cli-fullscreen-foundation-plan.md).

It operationalizes [`doc/meta/implement_use-cases.md`](../meta/implement_use-cases.md) under the execution discipline of [`doc/agy/runbook.md`](runbook.md).

---

## 1. Traceability & Context

| Dimension | Reference |
|---|---|
| **Use Case** | [`plan-marshall-documentation/doc/use-cases/03-plans.adoc#UC-PLN-4`](../../../plan-marshall-documentation/doc/use-cases/03-plans.adoc#UC-PLN-4) |
| **Specifications** | [`01-processes-and-transport.adoc#overview-resource`](../../../plan-marshall-documentation/doc/specification/runtime-model/01-processes-and-transport.adoc), [`#needs-operator`](../../../plan-marshall-documentation/doc/specification/runtime-model/01-processes-and-transport.adoc), [`#ended-work`](../../../plan-marshall-documentation/doc/specification/runtime-model/01-processes-and-transport.adoc), [`06-event-catalogue.adoc`](../../../plan-marshall-documentation/doc/specification/runtime-model/06-event-catalogue.adoc) |
| **Local Client Design** | Shared: [`doc/design/shared/plan-overview.adoc`](../design/shared/plan-overview.adoc)<br>CLI: [`doc/design/terminal-client/15-overview.adoc`](../design/terminal-client/15-overview.adoc) (`#full-screen`, `#full-screen-layout`, `#full-screen-project`, `#full-screen-keys`, `#full-screen-live`, `#overview-persistence`, `#ended`) |
| **Roadmap Package** | [`doc/plans/implemented/cli-overview.adoc`](../plans/implemented/cli-overview.adoc) |
| **Prerequisite Plans** | [`doc/agy/cli-fullscreen-foundation-plan.md`](cli-fullscreen-foundation-plan.md), [`doc/agy/mock-runtime-running-work-plan.md`](mock-runtime-running-work-plan.md) |
| **Implementation Recipe** | [`doc/meta/implement_use-cases.md`](../meta/implement_use-cases.md) |
| **Operational SOP** | [`doc/agy/runbook.md`](runbook.md) |

---

## 2. Scope & Objectives

1. **Level 1 — Projects Overview Level (`ProjectsOverviewLevel`)**:
   - Renders `Running work · projects` header with connection indicator (`● connected`), unread reports count in footer (`R <n> reports` when `> 0`), and rows ordered with attention (`! <count>`) first, then recent activity (`15-overview.adoc#full-screen-layout`).
   - Keys: `↑`/`↓` (`k`/`j`), `PgUp`/`PgDn` (`g`/`G`), `Enter`/`→`/`l` (open selected project level; or on a project with `setup needs you`, hand off to project setup), `E` (open ended work for selected project), `S` (system view hook), `R` (unread reports hook), `/` (filter), `r` (reload from runtime), `?` (help & row details), `q` (leave to prompt).
2. **Level 2 — Project Detail Level (`ProjectWorkLevel`, By Plan & By Epic)**:
   - Renders `Running work · <project_id> · by plan` and `Running work · <project_id> · by epic` (`15-overview.adoc#full-screen-project`).
   - **By Plan view**: two-line rows on `>= 100` columns (line 1: `! <needs>` marker, `▸ plan`, title, phase & state; line 2: running duration, phase duration, tasks progress, epic membership `in ◇ <epic>`, off-track signal `off track: <signal>`, PR number `#<n>` with OSC 8 hyperlink where supported) and single-line compact rows on `60..99` columns (with `?` showing full details and PR URL); *alongside* group below plans for short landings (`! landing failed` / `running`) and ongoing project setup.
   - **By Epic view**: epic rows (`◇ epic`, title, `<n> running · <m> staged`, duration & plan progress) with nested plan rows (`├─` / `└─`) and a `without epic` group below.
   - Keys: `Tab` toggles between `by plan` and `by epic` while keeping selection anchored by ID; `e` on a plan jumps to its epic in `by epic` view; `!` leaves or suspends the view to open what waits for the operator (`answer`); `Enter` opens the selected plan or epic view (delegating to `PlanViewLevel` / `EpicViewLevel` or fallback verb); `a` opens the actions menu hook; `E` opens ended work for the project; `Esc`/`←`/`h` returns to Projects level.
3. **Ended Work Level (`EndedWorkLevel`, `E`)**:
   - Renders `Ended work · <project_id>` (`15-overview.adoc#ended`) with outcome (`merged`, `no change`, `handed back`, `returned`, `cancelled`, `abandoned`, `released`, `aborted`, `completed`), PR number, relative end time, and second-line cancellation/abandonment reason.
   - Automatically fetches older pages (`GET /api/v1/projects/{id}/ended?page=<n>`) as the operator scrolls down (`has_older`).
   - `Esc`/`←`/`h` returns to the level from which `E` was opened (Projects level or Project Detail level).
4. **Live SSE Updates, Held Notices & `overview.json` Persistence**:
   - Subscribes to `/api/v1/events` (`project.work_started`, `project.work_ended`, `project.changed`, `phase.changed`, `activity.changed`, `needs_operator.changed`, `hold.*`, `condition.*`, `epic.row_changed`, `task.off_track`), updating the model on the background stream and redrawing on the single TUI thread while keeping selection anchored by item ID.
   - Persists `(projectId, projectView, selectedId)` to `$XDG_STATE_HOME/pm-operator/overview.json` on exit and restores on next `plans` launch (falling back gracefully if the saved plan/project has ended).
   - Holds console arrival notices via `ConsoleNoticeBuffer` while open and prints `While the overview was open: ...` on `q`.
5. **`PlansCommand` Integration & Fallback**:
   - When `TuiScreenDriver.canOpenFullScreen(...)` is true (interactive terminal, `>= 60` columns, `console.full_screen != false`, no `--json`), `pm-operator plans` and console `plans` launch the full-screen view (honouring `--project`, `--by`, `--ended` as initial level/view).
   - Below 60 columns, prints `operator.overview.too_narrow` and falls back to `StatusCommand` tree output; on non-interactive or `console.full_screen = false`, prints the `status` tree directly.
6. **Screen & Process Tests (JVM & Native)**:
   - Every scenario of `overview-contract` verified both as a `status` transcript test and as a `VirtualScreenTerminal` screen test of `plans`, plus `PmOperatorProcessIT` execution.

---

## 3. Concurrency & Isolation

- **Depends on**: [`doc/agy/cli-fullscreen-foundation-plan.md`](cli-fullscreen-foundation-plan.md) and [`doc/agy/mock-runtime-running-work-plan.md`](mock-runtime-running-work-plan.md).
- **Parallelizable with**: `cli-fullscreen-projects`, `cli-fullscreen-reports-system`, and all `web-*` plans once `cli-fullscreen-foundation` has landed on `main`.

### Boundary Rules
- **Sole Owner of**: `de.planmarshall.operator.overview.*` and `PlansCommand.java` wiring in `pm-operator`; shares row-formatting helpers with `StatusCommand.java`.
- **Read-Only Consumer of**: `pm-mock-runtime` (`running-work.json` owned by `mock-runtime-running-work-plan.md`) and `pm-api` message bundles (`messages/overview.{en,de}.properties` and `messages/operator.{en,de}.properties`).
- **No Touch**: `doc/plans/README.adoc` (already updated for `cli-*` packages by `cli-fullscreen-foundation-plan.md`), `pm-mock-runtime`, and `pm-web-app`.

---

## 4. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created at `/Users/oliver/git/worktrees/pm-mcp-clients-cli-fullscreen-overview` on branch `feat/cli-fullscreen-overview` off latest `main`.
- [x] 2. Domain skills & engineering personas loaded where applicable:
  - `persona-implementer` & `persona-module-tester` for code & unit tests
  - `persona-integration-tester` for multi-module/integration tests
  - `persona-security-expert` for trust boundaries & input validation
  - `persona-documenter` for documentation & ADRs
  - `persona-code-reviewer` for finding evaluation & adversarial refutation
- [x] 3. Incremental implementation with tests following the phased technical recipe.
- [x] 4. Final completion report written (`doc/agy/cli-fullscreen-overview-plan-report.md`) and initial plan moved to `doc/agy/implemented/cli-fullscreen-overview-plan.md`. (Do NOT move report to `verified/` — Step 8 is reserved for orchestrator).
- [x] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit` for code/config; skipped for standalone documentation).
- [x] 6. PR opened (NEVER use `skip-bot-review` on worker PRs; reserved for orchestrator docs PRs), review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [x] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [x] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/cli-fullscreen-overview-plan-report.md` by orchestrator session.

---

## 5. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Catalogue & Contract Check** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-integration-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:junit-core` | Verify `overview.*` / `operator.overview.*` keys and `running-work` SSE states from `mock-runtime-running-work`. |
| **Phase 2: Overview View Models** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-module-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:junit-core` | Pure view models for Projects, Project (by-plan / by-epic / alongside), and Ended Work levels. |
| **Phase 3: Full-Screen Rendering** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-security-expert` | `pm-dev-java:java-core`<br>`pm-dev-java:java-security` | Bind models to `TuiScreenDriver`, 100-col / 60-col responsive rows, OSC 8 PR links, `OutputSafety`. |
| **Phase 4: Live Stream & State** | `plan-marshall-persona-implementer` | `pm-dev-java:java-core` | SSE live updates keeping selection by ID, `OverviewStateStore` restore/save, `ConsoleNoticeBuffer` summary. |
| **Phase 5: Screen & Process Tests** | `plan-marshall-persona-integration-tester` | `pm-dev-java:junit-integration` | `VirtualScreenTerminal` tests for every key & `overview-contract` scenario; `PmOperatorProcessIT`. |
| **Phase 6: Review Gate** | `plan-marshall-persona-code-reviewer` | `pm-dev-java:java-security` | Pre-commit gate, `LiteralTextGuardTest`, and 100% resolution of automated review findings. |

---

## 6. Phased Technical Recipe

### Phase 1: Catalogue & `running-work` Scenario Verification
- [x] **1.1 Message Catalogue & Scenario Audit**:
  - Verify all keys in `doc/design/shared/plan-overview.adoc#texts` and `doc/design/terminal-client/15-overview.adoc#overview-texts` exist in `overview.{en,de}.properties` and `operator.{en,de}.properties`, and verify the `overview.*` states in `running-work.json` (delivered by `mock-runtime-running-work-plan.md`).

### Phase 2: Overview View Models (`de.planmarshall.operator.overview`)
- [x] **2.1 `ProjectsOverviewLevel`**:
  - Build rows from `ProjectOverviewResponse` and unread report count (`GET /api/v1/reports`), sorting attention projects first (`! <count>`), then active projects, then `setup needs you`.
  - Implement key handlers: `↑`/`↓`/`k`/`j`, `PgUp`/`PgDn`/`g`/`G`, `Enter`/`→`/`l` (push `ProjectWorkLevel`), `E` (push `EndedWorkLevel`), `/` filter, `r` reload, `?` help/details, `q` exit.
- [x] **2.2 `ProjectWorkLevel` (By Plan & By Epic)**:
  - Build `BY_PLAN` rows from `ProjectDetailResponse` (plans with attention markers, phase, state, durations, task counts, epic label, off-track signal, PR number, plus `alongside` landings and setup row).
  - Build `BY_EPIC` rows from `ProjectDetailResponse` (epics with running/staged counts, durations, plan progress, nested child plan rows `├─`/`└─`, and `without epic` plans).
  - Implement `Tab` view toggle preserving selected plan/epic ID, `e` jump to parent epic in `BY_EPIC` view, navigation hooks for `!` and `Enter` drill-down pending upcoming view integration, `E` to push `EndedWorkLevel`, and `Esc`/`←`/`h` to pop back to `ProjectsOverviewLevel`.
- [x] **2.3 `EndedWorkLevel`**:
  - Build ended work rows from `EndedWorkPage` (`GET /api/v1/projects/{id}/ended?page=1`), loading subsequent pages on scroll when `hasOlder()` is true.
  - Implement `/` filter, `?` details (including PR URL and full reason), and `Esc`/`←`/`h` back to the caller level.

### Phase 3: Layout, Responsive Widths & `PlansCommand` Wiring
- [x] **3.1 Responsive Row Formatting**:
  - At `>= 100` columns, render two-line plan/epic rows (main line + detail line); at `60..99` columns, omit the detail line and surface it in `?` help/details; at `< 60` columns, print `operator.overview.too_narrow` and delegate to `StatusCommand`.
  - Format PR numbers (`#214`) conforming to the terminal safety boundary (sanitized via `OutputSafety.stripControlSequences` without raw escape sequences in cells, with plain URL in `?` row details, and `OverviewFormatting.osc8Link` utility).
- [x] **3.2 `PlansCommand` & `PmConsole` Integration**:
  - Update `PlansCommand` to launch `OverviewFullScreenApp` via `TuiScreenDriver` when `canOpenFullScreen` is true, passing `--project`, `--by` / `--by-epic`, and `--ended` as initial state overrides; otherwise delegate to `StatusCommand`.

### Phase 4: Live Updates, Held Notices & `overview.json` Persistence
- [x] **4.1 Live Event Loop**:
  - Connect SSE listener while `plans` is open; on `project.*`, `phase.changed`, `activity.changed`, `needs_operator.changed`, `hold.*`, `condition.*`, `epic.row_changed`, `task.off_track`, refresh affected model data, keep selection anchored by ID (or nearest neighbour if removed), and mark `TuiScreenDriver` dirty.
- [x] **4.2 State Restoration & Saving**:
  - On `plans` startup (when no CLI `--project` flag is given), read `OverviewSavedState` from `OverviewStateStore` and open directly at the stored project, view (`by plan` / `by epic`), and selected plan/epic ID; fall back to `ProjectsOverviewLevel` if the stored project/item is no longer active.
  - On `q` exit, save current `(projectId, projectView, selectedId)` to `OverviewStateStore` and flush `ConsoleNoticeBuffer` (`operator.overview.meanwhile`).

### Phase 5: Screen Tests & Integration Verification
- [x] **5.1 `OverviewFullScreenTest` (`VirtualScreenTerminal`)**:
  - Assert `ProjectsOverviewLevel` layout, header, `▶` selection, `!` badges, and footer keys at `120x30`.
  - Assert `Enter` into `pm-mcp-clients`, `Tab` switching between `by plan` and `by epic` keeping selection, `e` jumping from a plan to its epic, and `/` filtering rows and clearing on `Esc`.
  - Assert `E` opening `EndedWorkLevel`, scrolling to load page 2, and `Esc` returning to the exact caller level.
  - Assert width thresholds: `80x24` omits detail lines (`?` shows them); `50x20` prints `operator.overview.too_narrow` and the tree fallback.
  - Assert live SSE update reordering rows while keeping selection on the selected plan ID, and `q` restoring the alternate screen and printing `While the overview was open: ...`.
  - Assert persistence round-trip via `overview.json` and fallback when the saved plan has ended.
- [x] **5.2 `PmOperatorProcessIT` & Pre-Commit Gate**:
  - Add process integration test in `PmOperatorProcessIT` for `plans` and run `./mvnw verify -Ppre-commit`.
