# Implementation Plan: `cli-fullscreen-actions` (Full-Screen Actions `a`, Epic View & Hold Resolution in `pm-operator`)

This implementation plan governs the execution of the `cli-actions` work package (`doc/plans/cli-actions.adoc`) — including the full-screen actions menu (`a`), the full-screen Epic View (`UC-PLN-5`), hold resolution (`UC-CTL-4`), plan/epic control dialogues (`UC-CTL-1..3`, `UC-CTL-5`), and ended-work chat — building on [`doc/agy/cli-fullscreen-foundation-plan.md`](cli-fullscreen-foundation-plan.md), [`doc/agy/cli-fullscreen-overview-plan.md`](cli-fullscreen-overview-plan.md), [`doc/agy/cli-fullscreen-plan-view-plan.md`](cli-fullscreen-plan-view-plan.md), and [`doc/agy/cli-start-plan.md`](cli-start-plan.md).

It operationalizes [`doc/meta/implement_use-cases.md`](../meta/implement_use-cases.md) under the execution discipline of [`doc/agy/runbook.md`](runbook.md).

---

## 1. Traceability & Context

| Dimension | Reference |
|---|---|
| **Use Cases** | [`UC-CTL-1` to `UC-CTL-5`](../../../plan-marshall-documentation/doc/use-cases/06-controlling-work.adoc), [`UC-PLN-5`](../../../plan-marshall-documentation/doc/use-cases/03-plans.adoc#UC-PLN-5) (Follow an Epic) |
| **Specifications** | [`control-contract.adoc`](../plans/implemented/control-contract.adoc), [`state-catalogue.adoc#chat`](../../../pm-mcp-core/doc/specification/state-catalogue.adoc), [`01-processes-and-transport.adoc#ended-work`](../../../plan-marshall-documentation/doc/specification/runtime-model/01-processes-and-transport.adoc) |
| **Local Client Design** | Shared: [`doc/design/shared/work-actions.adoc`](../design/shared/work-actions.adoc) (`#settings`, `#chat-ended`), [`epic-view.adoc`](../design/shared/epic-view.adoc)<br>CLI: [`doc/design/terminal-client/19-actions.adoc`](../design/terminal-client/19-actions.adoc) (`#full-screen`, `#epic-view`, `#holds`, `#review`, `#chat`, `#chat-ended`, `#texts`), [`22-texts-actions.adoc`](../design/terminal-client/22-texts-actions.adoc) |
| **Roadmap Package** | [`doc/plans/cli-actions.adoc`](../plans/cli-actions.adoc) (`open`) |
| **Prerequisite Plans** | [`doc/agy/cli-fullscreen-foundation-plan.md`](cli-fullscreen-foundation-plan.md), [`doc/agy/cli-fullscreen-overview-plan.md`](cli-fullscreen-overview-plan.md), [`doc/agy/cli-fullscreen-plan-view-plan.md`](cli-fullscreen-plan-view-plan.md), [`doc/agy/cli-start-plan.md`](cli-start-plan.md), [`doc/agy/mock-runtime-running-work-plan.md`](mock-runtime-running-work-plan.md) |
| **Implementation Recipe** | [`doc/meta/implement_use-cases.md`](../meta/implement_use-cases.md) |
| **Operational SOP** | [`doc/agy/runbook.md`](runbook.md) |

---

## 2. Scope & Objectives

1. **Full-Screen Actions Menu (`a` below a Row or Header, `19-actions.adoc#full-screen`)**:
   - Pressing `a` on a plan or epic row in the Overview (`ProjectWorkLevel`), inside `PlanViewLevel`, inside `EpicViewLevel`, or on an item in `EndedWorkLevel` expands the state-appropriate actions menu inline (`plan.actions`, `epic.actions`):
     - **Plan actions by state**: *Pause…* (with pause point: after the task / after the phase), *Start / Resume*, *Cancel…* (or *Abort…* on an integrity pause), *Abandon…*, *Settings…* (`config <aspect> --plan`), *Chat…* (`h`), and hold resolution acts when held; on an ended plan, offers *Chat…* alone (`<name> has ended: its view, and a chat about it`).
     - **Epic actions by state**: *Next work…* (raising scope or leaving it), *Automatic on/off* (`u` in the menu / `epic auto <id> on|off`, including rows waiting for an overlap), *Decompose…* (opens decomposition of `cli-start`), *Review findings / watches…*, *Archive…* (with archive check lines), *Cancel…*, *Chat…* (`h`), and hold resolution acts when held; on an ended epic, offers *Chat…* alone.
   - Selecting an action that requires a multi-step dialogue (confirmation, pause point, settings, review, chat, decomposition) cleanly suspends the full-screen view via `TuiScreenDriver`, runs the dialogue, and resumes or refreshes the view.
2. **Full-Screen Epic View (`EpicViewLevel`, `19-actions.adoc#epic-view` & `UC-PLN-5`)**:
   - Opened on `Enter` on an epic in the Overview, via `e` from a member plan's `PlanViewLevel`, or via `epic <epic_id>` in the console (while non-interactive / scrollback prints the tree, including `epic <epic_id> --tokens`).
   - Header path: `<project_id> › <epic_id>` with the epic's mode (`auto` / `manual`) and state.
   - Expandable/collapsible nodes (like `PlanViewLevel`):
     - *Workstreams and plans* with their status (`Enter` on a plan opens its `PlanViewLevel`, whose `e` returns to this `EpicViewLevel`).
     - *Sources* as stored.
     - *Landings* with PR number/link and the epic's landing analysis.
     - *Findings* and *Watches* with open item counts and `Enter` opening the review dialogue (`#review`).
     - *Tokens*: sum of the epic's plans and its own tasks (decompose, landing, review) with split, `≥` floor, and unmeasured task count (never money, no budget of its own); opens like a node to show per-plan token figures.
     - *Events*: newest first (all events in the full-screen view; ten newest in the scrollback tree), including epic reactions.
   - Keys: `a` for epic actions, `p` for one-key jump to the project's Overview level (`operator.plan.keys.project`), `Enter` to open a plan or expand a section, and `Esc` to return to where the epic view was opened.
3. **Hold Inspection & Resolution (`19-actions.adoc#holds` & `UC-CTL-4`)**:
   - Displays what holds a plan, an epic, or an alongside landing (`stalled`, `condition`, `integrity_pause`, `held`, `landing_failed`), with the specific resolution acts offered by the runtime; after an act completes, either the work resumes or the remaining hold is shown with what still holds it.
4. **Direct Verbs, Plan Settings (`UC-CTL-5`), Review & Chat (`19-actions.adoc#review`, `#chat`, `#chat-ended`)**:
   - Complete and verify direct verbs (`plan pause|start|cancel|abandon|chat`, `config <aspect> --plan` with conflict handling, `epic next|decompose|archive|cancel|findings|watches|chat|auto`) and their dialogues:
     - Review dialogue (`review.open`, `review.item`, `review.keep` as finding or watch, `review.drop`, edit of a drafted plan, read-back, `review.commit`, and `h` chat).
     - Chat dialogue opened from verbs, `a` menu (`h`), and review, with scope attribution and revision applied at the next task boundary.
     - Chat about ended work (`plan chat` / `epic chat` / `a` on ended work), where accepting a follow-up plan opens `plan add` at its sources with the description stored.
5. **Transcript & Screen Tests (JVM & Native)**:
   - Every scenario of `control-contract` passes as a transcript test of the verbs and as a `VirtualScreenTerminal` screen test of the actions (`a`), hold resolution, and `EpicViewLevel` in the full-screen view, in the JVM and native.

---

## 3. Concurrency & Isolation

- **Depends on**: [`doc/agy/cli-fullscreen-foundation-plan.md`](cli-fullscreen-foundation-plan.md), [`doc/agy/cli-fullscreen-overview-plan.md`](cli-fullscreen-overview-plan.md), [`doc/agy/cli-fullscreen-plan-view-plan.md`](cli-fullscreen-plan-view-plan.md), [`doc/agy/cli-start-plan.md`](cli-start-plan.md) (for shared `ChatDialogue` and decomposition), and [`doc/agy/mock-runtime-running-work-plan.md`](mock-runtime-running-work-plan.md).
- **Parallelizable with**: `web-decisions-and-actions-plan.md` (save the 1-line `cli-actions` status update in `doc/plans/README.adoc` and moving `doc/plans/cli-actions.adoc` to `doc/plans/implemented/cli-actions.adoc`).

### Boundary Rules
- **Sole Owner of**: `EpicViewLevel`, inline actions overlay (`a`), hold resolution dialogues, `PlanCommand` / `EpicCommand` / `ConfigCommand` control flows in `pm-operator`, and moving `doc/plans/cli-actions.adoc` to `doc/plans/implemented/cli-actions.adoc`.
- **Read-Only Consumer of**: `pm-mock-runtime` (`running-work.json` `control.*` states owned by `mock-runtime-running-work-plan.md`) and `pm-api` message bundles (`messages/operator.{en,de}.properties` and `messages/control.{en,de}.properties`).
- **No Touch**: `pm-mock-runtime` and `pm-web-app`.

---

## 4. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created at `/Users/oliver/git/worktrees/pm-mcp-clients-cli-fullscreen-actions` on branch `feat/cli-fullscreen-actions` off latest `main`.
- [ ] 2. Domain skills & engineering personas loaded where applicable:
  - `persona-implementer` & `persona-module-tester` for code & unit tests
  - `persona-integration-tester` for multi-module/integration tests
  - `persona-security-expert` for trust boundaries & input validation
  - `persona-documenter` for documentation & ADRs
  - `persona-code-reviewer` for finding evaluation & adversarial refutation
- [ ] 3. Incremental implementation with tests following the phased technical recipe.
- [ ] 4. Final completion report written (`doc/agy/cli-fullscreen-actions-plan-report.md`), initial plan moved to `doc/agy/implemented/cli-fullscreen-actions-plan.md`, and package `cli-actions` marked done in `doc/plans/README.adoc` and moved to `doc/plans/implemented/cli-actions.adoc`. (Do NOT move report to `verified/` — Step 8 is reserved for orchestrator).
- [ ] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit` for code/config; skipped for standalone documentation).
- [ ] 6. PR opened (NEVER use `skip-bot-review` on worker PRs; reserved for orchestrator docs PRs), review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [ ] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [ ] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/cli-fullscreen-actions-plan-report.md` by orchestrator session.

---

## 5. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Catalogue & Scenarios** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-integration-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:junit-core` | Verify `22-texts-actions.adoc` and `19-actions.adoc#texts` in EN/DE bundles and `running-work#control.*` states. |
| **Phase 2: `EpicViewLevel` (`UC-PLN-5`)** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-module-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:junit-core` | Full-screen Epic View nodes (workstreams/plans, sources, landings, registers, tokens, all events) & `p`/`a`/`Enter`/`Esc`. |
| **Phase 3: Inline Actions (`a`) & Holds** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-security-expert` | `pm-dev-java:java-core`<br>`pm-dev-java:java-security` | `a` menu below rows/headers per plan/epic/landing/ended state, `u` auto toggle, hold resolution acts. |
| **Phase 4: Review, Settings & Chat** | `plan-marshall-persona-implementer` | `pm-dev-java:java-core` | `config <aspect> --plan` conflict retry, findings/watches review dialogue, running & ended-work chat follow-up to `plan add`. |
| **Phase 5: Transcript & Screen Tests** | `plan-marshall-persona-integration-tester` | `pm-dev-java:junit-integration` | `VirtualScreenTerminal` tests for `a` menus, `EpicViewLevel`, holds, and transcript tests for all `control-contract` scenarios. |
| **Phase 6: Review Gate** | `plan-marshall-persona-code-reviewer` | `pm-dev-java:java-security` | Pre-commit gate, `LiteralTextGuardTest`, and 100% resolution of automated review findings. |

---

## 6. Phased Technical Recipe

### Phase 1: Catalogue & Contract Audit
- [ ] **1.1 Message Catalogue Audit**:
  - Verify all keys in `doc/design/terminal-client/22-texts-actions.adoc`, `19-actions.adoc#texts`, and `doc/design/shared/work-actions-*.adoc` exist in `pm-api/src/main/resources/messages/` in both English and German.
- [ ] **1.2 Control States Audit**:
  - Verify the `control.*` states in `running-work.json` (`control.ready`, `control.pause`, `control.holds`, `control.settings`, `control.epic`, `control.review`, `control.chat`, `control.tokens`, `control.decomposition`, delivered by `mock-runtime-running-work-plan.md`) cover all transitions needed by both verb transcripts and full-screen tests.

### Phase 2: Full-Screen Epic View (`EpicViewLevel`, `UC-PLN-5`)
- [ ] **2.1 `EpicViewLevel` Tree & Sections**:
  - Render `<project_id> › <epic_id>` header with mode and status, and expandable sections for Workstreams & Plans, Sources, Landings, Findings & Watches, Tokens (expandable per-plan + epic's own tasks with `≥` floor), and Events (all events in full-screen, 10 newest in scrollback tree).
  - Wire `Enter` on a plan row to push `PlanViewLevel` (whose `e` returns to `EpicViewLevel`), `Enter` on Findings/Watches to launch the review dialogue, `p` to jump in one key to `ProjectWorkLevel`, `a` to open `epic.actions`, and `Esc` to return to the caller level.

### Phase 3: Full-Screen Actions Menu (`a`) & Hold Resolution (`UC-CTL-4`)
- [ ] **3.1 State-Aware Inline Actions (`plan.actions`, `epic.actions`)**:
  - Implement `a` in `ProjectWorkLevel`, `PlanViewLevel`, `EpicViewLevel`, and `EndedWorkLevel`, rendering only the actions valid for the selected item's state (running, pausing, paused, waiting, held, landing failed, ended).
  - Support `u` in the epic menu to toggle `epic auto` on/off and display rows waiting for an overlap.
- [ ] **3.2 Hold Resolution (`hold.show`, `hold.resolve`, `hold.apply`)**:
  - On a held plan, epic, or failed alongside landing, show the hold details and resolution acts; after executing an act, refresh the state and either resume work or display the remaining hold.

### Phase 4: Plan Settings (`UC-CTL-5`), Review Dialogue & Ended-Work Chat
- [ ] **4.1 Plan Settings & Conflict Handling**:
  - Verify `config <aspect> --plan <id>` and the `a` → *Settings…* menu handle refusals and mid-edit value conflicts (`conflict`: apply again or keep).
- [ ] **4.2 Review Dialogue & Chat Follow-Up**:
  - Complete the findings/watches review dialogue (keep as finding/watch, drop, edit drafted plan, read-back, `h` chat, commit).
  - Complete running-work chat (applying revision at next task boundary) and ended-work chat (`plan chat`, `epic chat`, and `a` → *Chat…* on ended work, transitioning a follow-up plan into `plan add` at its sources with the description stored).

### Phase 5: Transcript & Screen Tests
- [ ] **5.1 `ActionsFullScreenTest` & `EpicViewFullScreenTest` (`VirtualScreenTerminal`)**:
  - Assert `a` inline action menus across every plan, epic, landing, and ended-work state, including `u` auto toggle, pause-point selection, hold resolution acts, and dialogue suspension/resumption.
  - Assert `EpicViewLevel` opening from Overview and from a plan (`e`), expanding workstreams, sources, landings, tokens, and all events, jumping to project (`p`), and returning via `Esc`.
- [ ] **5.2 Pre-Commit & Operational Gate**:
  - Run `./mvnw verify -Ppre-commit`, move `doc/plans/cli-actions.adoc` to `doc/plans/implemented/cli-actions.adoc`, update `doc/plans/README.adoc`, and execute the PR/review workflow per [`doc/agy/runbook.md`](runbook.md).
