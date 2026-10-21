# Implementation Plan: `cli-fullscreen-projects` (Full-Screen Project Explorer `projects` in `pm-operator`)

This implementation plan governs the completion of the full-screen Project Explorer (`projects` without `--list`, and `--project ?` selector) for `UC-PRJ-1..3` (`doc/plans/implemented/cli-projects.adoc`) in `pm-operator`, building on [`doc/agy/cli-fullscreen-foundation-plan.md`](cli-fullscreen-foundation-plan.md) and [`doc/agy/cli-fullscreen-overview-plan.md`](cli-fullscreen-overview-plan.md).

It operationalizes [`doc/meta/implement_use-cases.adoc`](../meta/implement_use-cases.adoc) under the execution discipline of [`doc/agy/runbook.md`](runbook.md).

---

## 1. Traceability & Context

| Dimension | Reference |
|---|---|
| **Use Cases** | [`plan-marshall-documentation/doc/use-cases/04-projects.adoc#UC-PRJ-1`](../../../plan-marshall-documentation/doc/use-cases/04-projects.adoc#UC-PRJ-1), [`UC-PRJ-2`](../../../plan-marshall-documentation/doc/use-cases/04-projects.adoc#UC-PRJ-2), [`UC-PRJ-3`](../../../plan-marshall-documentation/doc/use-cases/04-projects.adoc#UC-PRJ-3) |
| **Specifications** | [`project-contract.adoc`](../plans/implemented/project-contract.adoc) |
| **Local Client Design** | Shared: [`doc/design/shared/project-explorer.adoc`](../design/shared/project-explorer.adoc), [`project-onboarding.adoc`](../design/shared/project-onboarding.adoc)<br>CLI: [`doc/design/terminal-client/17-projects.adoc`](../design/terminal-client/17-projects.adoc) (`#explorer`, `#commands`, `#list`, `#texts`), [`02-levels.adoc#addressing`](../design/terminal-client/02-levels.adoc) (`--project ?`) |
| **Roadmap Package** | [`doc/plans/implemented/cli-projects.adoc`](../plans/implemented/cli-projects.adoc) |
| **Prerequisite Plans** | [`doc/agy/cli-fullscreen-foundation-plan.md`](cli-fullscreen-foundation-plan.md), [`doc/agy/cli-fullscreen-overview-plan.md`](cli-fullscreen-overview-plan.md), [`doc/agy/mock-runtime-projects-and-start-plan.md`](mock-runtime-projects-and-start-plan.md) |
| **Implementation Recipe** | [`doc/meta/implement_use-cases.adoc`](../meta/implement_use-cases.adoc) |
| **Operational SOP** | [`doc/agy/runbook.md`](runbook.md) |

---

## 2. Scope & Objectives

1. **Full-Screen Project Explorer (`ProjectsExplorerLevel`, `17-projects.adoc#explorer`)**:
   - Invoked when `pm-operator projects` (or console `projects`) runs on an interactive full-screen-capable terminal without `--list`; `--list` or non-interactive/narrow terminals print the scrollback list (`17-projects.adoc#list`).
   - Renders the four explorer groups:
     - *Being added*: in-progress project onboarding runs not yet enrolled, showing checkout path and current onboarding step (`② Identity`, etc.).
     - *Favourites*: starred projects (`★ <project_id>`) in user-defined order with readiness (`ready` / `setup needs: …`), `! <count>` attention badge, active plan/epic counts, and last activity.
     - *Recent*: recently active non-favourite projects.
     - *All projects · by name* (or *by most used* when toggled with `s`): all enrolled and `released` projects (unenrolled projects are omitted).
2. **Inline Project Actions Menu & State-Specific Offerings (`17-projects.adoc#explorer`)**:
   - `Enter` on a project expands its actions inline below the row (and `Enter` on an action executes it), while direct shortcut keys also work from the project row:
     - **Ready project**: `e  Add an epic`, `p  Add a plan`, `r  Running work` (opens the project's level in `OverviewFullScreenApp`), `u  Setup` (leaves view and runs `project setup <id>`), `*  Remove from favourites` / `Add to favourites`; if an open start entry exists, offers `c  Continue starting` first.
     - **Not-ready project (`setup needs: …`)**: offers `c  Continue the setup` first; shows `e` and `p` disabled with the reason (`<id> is not ready: <reason>`).
     - **Being-added project**: offers `c  Continue` and `x  Abandon…` (asks confirmation and deletes the un-enrolled onboarding run).
     - **Released project**: offers `a  Enrol again` (runs onboarding from `project.identity` with current values) and `E  Ended work` (opens `EndedWorkLevel` for the project); omits `r  Running work`.
3. **Favourites, Reordering, Sorting & Filtering Keys**:
   - `*`: toggles favourite status of the selected project (`POST` / `DELETE` or `PATCH` on project favourites endpoint).
   - `J` / `K`: moves a selected favourite project down or up within the *Favourites* list.
   - `s`: toggles *All projects* sort order between `by name` and `by most used`.
   - `/`: filters projects by name or ID, flattening the groups into a single matching list (`Esc` clears the filter).
   - `+`: leaves the view and launches `project add`.
4. **`--project ?` Interactive Selector (`02-levels.adoc#addressing`)**:
   - Passing `--project ?` to any verb that accepts `--project` displays the explorer's project list (favourites first) to pick the target project interactively before running the verb.
5. **Screen Tests (JVM & Native)**:
   - Every scenario of `project-contract` verified both as a `projects --list` / `project add` transcript test and as a `VirtualScreenTerminal` screen test of `projects`.

---

## 3. Concurrency & Isolation

- **Depends on**: [`doc/agy/cli-fullscreen-foundation-plan.md`](cli-fullscreen-foundation-plan.md), [`doc/agy/cli-fullscreen-overview-plan.md`](cli-fullscreen-overview-plan.md), and [`doc/agy/mock-runtime-projects-and-start-plan.md`](mock-runtime-projects-and-start-plan.md).
- **Parallelizable with**: `cli-fullscreen-plan-view`, `cli-fullscreen-reports-system`, and all `web-*` plans.

### Boundary Rules
- **Sole Owner of**: `de.planmarshall.operator.projects.*`, `ProjectsCommand.java` full-screen launch, and `--project ?` resolution in `pm-operator`.
- **Read-Only Consumer of**: `pm-mock-runtime` (`projects-and-start.json` owned by `mock-runtime-projects-and-start-plan.md`) and `pm-api` message bundles (`messages/operator.{en,de}.properties` and `messages/projects.{en,de}.properties`).
- **No Touch**: `doc/plans/README.adoc`, `pm-mock-runtime`, and `pm-web-app`.

---

## 4. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created at `/Users/oliver/git/worktrees/pm-mcp-clients-cli-fullscreen-projects` on branch `feat/cli-fullscreen-projects` off latest `main`.
- [x] 2. Domain skills & engineering personas loaded where applicable:
  - `persona-implementer` & `persona-module-tester` for code & unit tests
  - `persona-integration-tester` for multi-module/integration tests
  - `persona-security-expert` for trust boundaries & input validation
  - `persona-documenter` for documentation & ADRs
  - `persona-code-reviewer` for finding evaluation & adversarial refutation
- [x] 3. Incremental implementation with tests following the phased technical recipe.
- [x] 4. Final completion report written (`doc/agy/cli-fullscreen-projects-plan-report.md`) and initial plan moved to `doc/agy/implemented/cli-fullscreen-projects-plan.md`. (Do NOT move report to `verified/` — Step 8 is reserved for orchestrator).
- [x] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit` for code/config; skipped for standalone documentation).
- [ ] 6. PR opened (NEVER use `skip-bot-review` on worker PRs; reserved for orchestrator docs PRs), review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [ ] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [ ] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/cli-fullscreen-projects-plan-report.md` by orchestrator session.

---

## 5. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Catalogue & Contract Check** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-integration-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:junit-core` | Verify `operator.projects.*` / `project.explorer.*` keys and `projects-and-start` mock states. |
| **Phase 2: Explorer View Model** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-module-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:junit-core` | Groups (`Being added`, `Favourites`, `Recent`, `All`), inline actions per state, sort `s`, filter `/`. |
| **Phase 3: Actions & `--project ?`** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-security-expert` | `pm-dev-java:java-core`<br>`pm-dev-java:java-security` | Favourite toggle/reorder (`*`, `J`, `K`), dialogue hand-offs (`+`, `e`, `p`, `u`, `c`, `x`, `a`), `--project ?`. |
| **Phase 4: Screen & Process Tests** | `plan-marshall-persona-integration-tester` | `pm-dev-java:junit-integration` | `VirtualScreenTerminal` tests for every explorer group, action menu, and key; `PmOperatorProcessIT`. |
| **Phase 5: Review Gate** | `plan-marshall-persona-code-reviewer` | `pm-dev-java:java-security` | Pre-commit gate, `LiteralTextGuardTest`, and 100% resolution of automated review findings. |

---

## 6. Phased Technical Recipe

### Phase 1: Catalogue & `projects-and-start` Scenario Verification
- [x] **1.1 Message Catalogue & Scenario Audit**:
  - Verify all keys in `doc/design/terminal-client/20-texts-projects.adoc` and `17-projects.adoc#texts` exist in `pm-api/src/main/resources/messages/operator.{en,de}.properties` and `projects.{en,de}.properties`, and verify the `projects.*` states in `projects-and-start.json` (delivered by `mock-runtime-projects-and-start-plan.md`).

### Phase 2: `ProjectsExplorerLevel` View Model (`de.planmarshall.operator.projects`)
- [x] **2.1 Grouped Project List & Filtering**:
  - Build `ProjectsExplorerLevel` rendering `Being added`, `Favourites` (`★`), `Recent`, and `All projects · by name` / `by most used`.
  - Implement `/` filter mode that collapses the four section headers into one flat filtered list matching project name or ID, and `Esc` to restore the grouped view.
- [x] **2.2 Inline Action Menus per Project State**:
  - Implement `Enter` on any project row to expand/collapse its state-specific action list (`ready`, `ready with open entry`, `not ready`, `being added`, `released`), plus disabled `e`/`p` explanation when a project is not ready.

### Phase 3: Key Actions, Hand-Offs & `--project ?`
- [x] **3.1 Explorer Key Bindings**:
  - Wire `*` (toggle favourite), `J`/`K` (move favourite down/up), `s` (toggle sort order), `r` (push `ProjectWorkLevel` in Overview), `E` on a released project (push `EndedWorkLevel`), and `+` / `e` / `p` / `u` / `c` / `x` / `a` to suspend/leave the full-screen view and invoke the corresponding dialogue (`project add`, `epic add`, `plan add`, `project setup`, abandon confirmation, re-enrol).
- [x] **3.2 `ProjectsCommand` & `--project ?` Integration**:
  - Update `ProjectsCommand`: when `--list` is not set and `TuiScreenDriver.canOpenFullScreen` is true, open `ProjectsExplorerLevel`; otherwise print the scrollback list.
  - Implement shared `--project ?` resolver across verbs accepting `--project`, presenting the explorer list (favourites first) to pick the project ID interactively.

### Phase 4: Screen Tests & Integration Verification
- [x] **4.1 `ProjectsExplorerFullScreenTest` (`VirtualScreenTerminal`)**:
  - Assert rendering of `Being added`, `Favourites`, `Recent`, and `All projects` groups at `120x30`.
  - Assert `Enter` inline action menus for each project state (ready, not-ready with disabled `e`/`p` reason, open start entry, being-added with `c`/`x`, and released with `a`/`E`).
  - Assert `*` favourite toggle, `J`/`K` favourite reordering, `s` sort toggle, `/` flat filtering, `r` transition to the project's Overview level, and `--project ?` selection.
- [ ] **4.2 Pre-Commit & Operational Gate**:
  - Run `./mvnw verify -Ppre-commit` and execute PR/review workflow per [`doc/agy/runbook.md`](runbook.md).
