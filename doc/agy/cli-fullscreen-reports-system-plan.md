# Implementation Plan: `cli-fullscreen-reports-system` (Full-Screen Reports `R` & System `S` Views in `pm-operator`)

This implementation plan governs the completion of the full-screen Reports (`R`, `UC-PLN-6`) and System (`S` / `system`, `UC-SYS-1..3`) views (`doc/plans/implemented/cli-reports.adoc` and `doc/plans/implemented/cli-system.adoc`) in `pm-operator`, building on [`doc/agy/cli-fullscreen-foundation-plan.md`](cli-fullscreen-foundation-plan.md) and [`doc/agy/cli-fullscreen-overview-plan.md`](cli-fullscreen-overview-plan.md).

It operationalizes [`doc/meta/implement_use-cases.md`](../meta/implement_use-cases.md) under the execution discipline of [`doc/agy/runbook.md`](runbook.md).

---

## 1. Traceability & Context

| Dimension | Reference |
|---|---|
| **Use Cases** | [`UC-PLN-6`](../../../plan-marshall-documentation/doc/use-cases/03-plans.adoc#UC-PLN-6) (Reports & Completion Report), [`UC-SYS-1..3`](../../../plan-marshall-documentation/doc/use-cases/07-system.adoc) (System View, Runtime Lifecycle & Cleanup) |
| **Specifications** | [`report-contract.adoc`](../plans/implemented/report-contract.adoc), [`system-contract.adoc`](../plans/implemented/system-contract.adoc) |
| **Local Client Design** | Shared: [`doc/design/shared/reports.adoc`](../design/shared/reports.adoc), [`completion-report.adoc`](../design/shared/completion-report.adoc), [`system.adoc`](../design/shared/system.adoc), [`cleanup.adoc`](../design/shared/cleanup.adoc)<br>CLI: [`doc/design/terminal-client/24-reports.adoc`](../design/terminal-client/24-reports.adoc) (`#full-screen`, `#completion`), [`25-system.adoc`](../design/terminal-client/25-system.adoc) (`#full-screen`), [`26-cleanup.adoc`](../design/terminal-client/26-cleanup.adoc) |
| **Roadmap Packages** | [`doc/plans/implemented/cli-reports.adoc`](../plans/implemented/cli-reports.adoc), [`doc/plans/implemented/cli-system.adoc`](../plans/implemented/cli-system.adoc) |
| **Prerequisite Plans** | [`doc/agy/cli-fullscreen-foundation-plan.md`](cli-fullscreen-foundation-plan.md), [`doc/agy/cli-fullscreen-overview-plan.md`](cli-fullscreen-overview-plan.md), [`doc/agy/mock-runtime-reports-system-designer-plan.md`](mock-runtime-reports-system-designer-plan.md) |
| **Implementation Recipe** | [`doc/meta/implement_use-cases.md`](../meta/implement_use-cases.md) |
| **Operational SOP** | [`doc/agy/runbook.md`](runbook.md) |

---

## 2. Scope & Objectives

1. **Full-Screen Reports Level (`ReportsLevel`, `R` from Overview, `24-reports.adoc#full-screen`)**:
   - Opened via `R` in the Overview; the Overview's projects-level footer shows `R <n> reports` (`operator.reports.key_count`) while unread reports exist.
   - Header: `Reports · <n> unread` with `● connected`.
   - Lists unread reports grouped hierarchically by `project` → `epic` (`◇ <epic>`) → `plan` (`▸ <plan>`) or `▪ project` (`Onboarded`, etc.).
   - Keys:
     - `Enter`: opens the report's detail in the same view (completion report, plan view, epic view, or project) and marks the report read; `Esc` from that detail returns to `ReportsLevel`.
     - `m`: marks the selected report read; it leaves the level and the selection moves to its neighbour.
     - `M`: marks all reports currently shown in the level read by their IDs.
     - `p`, `e`: opens the report's project or epic view without marking the report read.
     - `?`: shows the report's full detail line (body in full, created and sent timestamps, report ID).
     - `Esc`: returns to the Overview.
   - Live updates: a newly arriving report is inserted at its place without moving the selection; a report marked read elsewhere is removed live.
2. **Full-Screen Completion Report Level (`CompletionReportLevel`, `c` on Ended Plan, `24-reports.adoc#completion`)**:
   - Opened via `c` on an ended plan's view or `Enter` on a plan-ended report in `ReportsLevel` (while `plan report <plan_id>` prints it into the scrollback).
   - Displays the structured completion report (outcome, PR, summary, deliverables, criteria evaluation, token figures, and follow-up links), with `Esc` returning to the caller level.
3. **Full-Screen System Level (`SystemLevel`, `S` from Overview & `system` in console, `25-system.adoc#full-screen`)**:
   - Opened via `S` in the Overview (where `Esc` returns to the Overview) or by typing `system` in the interactive console (where `q` leaves to the prompt).
   - Header: `System · pm-mcpd <version>` with `● connected` (and draining/upgrading progress banner below the header while the runtime drains or upgrades).
   - Six selectable figure rows: `Runtime` (uptime, available upgrade, memory, resident, CPU), `Work` (plans/epics/tasks/projects counts), `Workers` (total/busy/idle), `Jobs` (build/test slots, queued, locks held), `Models` (window, calls, token total & in/out split with `≥` floor), and `Cleanup` (`due: <plans> · <epics> · <logs> · frees <size>`).
   - Keys:
     - `↑`/`↓` (`k`/`j`): move between the six figures.
     - `Enter`/`→`/`l`: follow the selected figure into its detail level (`Workers`, `Jobs`, `Locks`, a model's calls, `Cleanup` level, or `Work` opening the Overview filtered to that state); `Esc` returns to `SystemLevel`.
     - `w`: cycles the `Models` time window (`hour` → `today` → `7d`).
     - `b`: toggles `Models` breakdown between `by model` and `by role`.
     - `p`: toggles between all projects and per-project figures.
     - `a`: opens runtime actions below the `Runtime` row (`Stop…`, `Restart…`, `Upgrade…`, `Check for a newer version` — each only where the runtime offers it); confirmation suspends/leaves the full-screen view to run the verb's dialogue, and `q` after it returns to the view.
     - `r`: re-reads the system snapshot.
     - `?`: shows all keys and the details of the selected figure (what it measures, since when).
     - `q`: leaves the view to the prompt.
   - Narrow & Persistence rules: below 80 columns, detail lines are omitted and shown via `?`; below 60 columns, the view does not open and prints `operator.system.too_narrow` followed by the snapshot; saves `(systemWindow, systemGroupBy, systemProjectId)` in `$XDG_STATE_HOME/pm-operator/overview.json`.
4. **Screen Tests (JVM & Native)**:
   - Every scenario of `report-contract` and `system-contract` verified as a `VirtualScreenTerminal` screen test of `ReportsLevel`, `CompletionReportLevel`, and `SystemLevel`, in addition to the existing verb transcript tests.

---

## 3. Concurrency & Isolation

- **Depends on**: [`doc/agy/cli-fullscreen-foundation-plan.md`](cli-fullscreen-foundation-plan.md), [`doc/agy/cli-fullscreen-overview-plan.md`](cli-fullscreen-overview-plan.md), and [`doc/agy/mock-runtime-reports-system-designer-plan.md`](mock-runtime-reports-system-designer-plan.md).
- **Parallelizable with**: `cli-fullscreen-plan-view`, `cli-fullscreen-projects`, and all `web-*` plans.

### Boundary Rules
- **Sole Owner of**: `de.planmarshall.operator.reports.*`, `de.planmarshall.operator.system.*`, and full-screen wiring in `ReportsCommand.java` / `SystemCommand.java` / `OverviewFullScreenApp` in `pm-operator`.
- **Read-Only Consumer of**: `pm-mock-runtime` (`reports-and-system.json` owned by `mock-runtime-reports-system-designer-plan.md`) and `pm-api` message bundles (`messages/operator.{en,de}.properties`, `messages/reports.{en,de}.properties`, and `messages/system.{en,de}.properties`).
- **No Touch**: `doc/plans/README.adoc`, `pm-mock-runtime`, and `pm-web-app`.

---

## 4. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [ ] 1. Isolated worktree created at `/Users/oliver/git/worktrees/pm-mcp-clients-cli-fullscreen-reports-system` on branch `feat/cli-fullscreen-reports-system` off latest `main`.
- [ ] 2. Domain skills & engineering personas loaded where applicable:
  - `persona-implementer` & `persona-module-tester` for code & unit tests
  - `persona-integration-tester` for multi-module/integration tests
  - `persona-security-expert` for trust boundaries & input validation
  - `persona-documenter` for documentation & ADRs
  - `persona-code-reviewer` for finding evaluation & adversarial refutation
- [ ] 3. Incremental implementation with tests following the phased technical recipe.
- [ ] 4. Final completion report written (`doc/agy/cli-fullscreen-reports-system-plan-report.md`) and initial plan moved to `doc/agy/implemented/cli-fullscreen-reports-system-plan.md`. (Do NOT move report to `verified/` — Step 8 is reserved for orchestrator).
- [ ] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit` for code/config; skipped for standalone documentation).
- [ ] 6. PR opened (NEVER use `skip-bot-review` on worker PRs; reserved for orchestrator docs PRs), review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [ ] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [ ] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/cli-fullscreen-reports-system-plan-report.md` by orchestrator session.

---

## 5. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Catalogue & Contract Check** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-integration-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:junit-core` | Verify `operator.reports.*` and `operator.system.*` keys and `reports-and-system` mock states. |
| **Phase 2: Reports & Completion View** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-module-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:junit-core` | `ReportsLevel` (`R`), `m`/`M`/`Enter`/`p`/`e`/`?`, live arrival/read updates, `CompletionReportLevel` (`c`). |
| **Phase 3: System View & Drill-Down** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-security-expert` | `pm-dev-java:java-core`<br>`pm-dev-java:java-security` | `SystemLevel` (`S` / `system`), `w`/`b`/`p`/`a`/`Enter` drill-downs, 80/60-col rules, `overview.json` state. |
| **Phase 4: Screen & Process Tests** | `plan-marshall-persona-integration-tester` | `pm-dev-java:junit-integration` | `VirtualScreenTerminal` tests for all `report-contract` and `system-contract` scenarios. |
| **Phase 5: Review Gate** | `plan-marshall-persona-code-reviewer` | `pm-dev-java:java-security` | Pre-commit gate, `LiteralTextGuardTest`, and 100% resolution of automated review findings. |

---

## 6. Phased Technical Recipe

### Phase 1: Catalogue & `reports-and-system` Scenario Verification
- [ ] **1.1 Message Catalogue & Scenario Audit**:
  - Verify all keys in `doc/design/terminal-client/24-reports.adoc#texts` and `25-system.adoc#texts` (`operator.reports.*`, `operator.system.*`, `operator.system.too_narrow`) exist in `pm-api/src/main/resources/messages/` in both English and German, and verify the `reports.*` and `system.*` states in `reports-and-system.json` (delivered by `mock-runtime-reports-system-designer-plan.md`).

### Phase 2: `ReportsLevel` (`R`) & `CompletionReportLevel` (`c`)
- [ ] **2.1 `ReportsLevel` Implementation**:
  - Group unread reports by project, epic, and plan; wire `R` in `ProjectsOverviewLevel` (displaying unread count in the footer when `> 0`).
  - Implement `m` (mark selected read, move selection to neighbour), `M` (mark all shown read by IDs), `Enter` (open target detail view and mark read), `p` and `e` (jump to project or epic view without marking read), and `?` (full report body, timestamps, ID).
  - Handle live SSE report arrival (insert in place without moving selection) and external read events.
- [ ] **2.2 `CompletionReportLevel` Implementation**:
  - Render a plan's completion report in full-screen when opened via `Enter` from `ReportsLevel` or `c` from an ended plan's `PlanViewLevel`.

### Phase 3: `SystemLevel` (`S` & `system`)
- [ ] **3.1 `SystemLevel` & Figure Drill-Downs**:
  - Render the six figures (`Runtime`, `Work`, `Workers`, `Jobs`, `Models`, `Cleanup`) and live drain/upgrade progress line below the header.
  - Implement `w` (cycle `hour`/`today`/`7d`), `b` (toggle `model`/`role`), `p` (all projects vs per-project), `r` (reload), `?` (figure details), and `Enter` drill-downs into `Workers`, `Jobs`, `Locks`, `Models`, `Cleanup`, or filtered Overview.
  - Implement `a` inline runtime actions (`Stop…`, `Restart…`, `Upgrade…`, `Check for a newer version`), suspending the full-screen view for confirmation dialogues and returning on `q`.
- [ ] **3.2 Width Thresholds & Persistence**:
  - Omit detail lines below 80 columns (`?` shows them); below 60 columns print `operator.system.too_narrow` with `{columns}` and fall back to the printed snapshot.
  - Persist and restore `(systemWindow, systemGroupBy, systemProjectId)` in `OverviewStateStore` (`overview.json`).

### Phase 4: Screen Tests & Integration Verification
- [ ] **4.1 `ReportsFullScreenTest` & `SystemFullScreenTest` (`VirtualScreenTerminal`)**:
  - Assert `R` opening `ReportsLevel`, grouping by project/epic/plan, `m` single read, `M` mark all shown read, `p`/`e` navigation without marking read, `Enter` opening detail & marking read, `c` opening `CompletionReportLevel`, and live report arrival preserving selection.
  - Assert `S` and `system` opening `SystemLevel`, `w`/`b`/`p` toggles persisted to `overview.json`, `Enter` drill-downs, `a` runtime action menu, 80-col compact mode, and `< 60`-col `too_narrow` snapshot fallback.
- [ ] **4.2 Pre-Commit & Operational Gate**:
  - Run `./mvnw verify -Ppre-commit` and execute PR/review workflow per [`doc/agy/runbook.md`](runbook.md).
