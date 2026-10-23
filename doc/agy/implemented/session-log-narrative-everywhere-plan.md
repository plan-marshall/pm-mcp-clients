# Implementation Plan: `session-log-narrative-everywhere` (Every Process-Tree Item Narrates on the Right)

The plan view already has the structure the operator wants: a process tree on the left and a narrative on the right. The defect is that only **leaf tasks** feed the right side. A phase or sub-workflow row toggles its children, but selecting it shows nothing, so an already-completed phase such as `init`, `refine`, `outline`, or `plan` looks inert. On `plans/session-log` this is most visible because most of the tree is done.

This plan makes the left tree a complete navigation surface. Every visible item — phase, sub-workflow, task group, leaf task — becomes selectable and produces a right-hand narrative. A leaf task keeps its live activity feed. A node shows its own state, timing, run count, child tasks, and the activity of the child tasks it already carries, so a done phase explains what it was and what ran inside it. The plan also closes the mock-data gap that hides `session-log`: neither canonical scenario currently serves `GET /api/v1/plans/session-log/process`.

The positive observation — *“the tree on the left with narratives on the right”* — is preserved as the governing layout; this plan repairs its completeness, not its shape.

The work is confined to `pm-mcp-clients`. The runtime contract is unchanged: no new endpoint, no new record, no server change. A node narrative is derived from the `ProcessTree` the view already has.

---

## 1. Traceability & Context

| Dimension | Reference |
|---|---|
| **Use Cases** | [`UC-PLN-1`](../../../../plan-marshall-documentation/doc/use-cases/03-plans.adoc#UC-PLN-1) (Follow a Plan Down to Its Parts), [`UC-PLN-4`](../../../../plan-marshall-documentation/doc/use-cases/03-plans.adoc#UC-PLN-4) (See What Runs, Across Projects), [`UC-PLN-5`](../../../../plan-marshall-documentation/doc/use-cases/03-plans.adoc#UC-PLN-5) (Follow an Epic) |
| **Requirements** | `PM-ARCH-11` (the plan's process as a structure), `PM-WORK-7` (worker activity and off-track signals), `PM-SEC-1` (activity as untrusted text), `PM-SEC-7` (both clients as dependable views) |
| **Process view today** | [`PlanView.svelte`](../../../pm-web-app/src/lib/views/PlanView.svelte) — `selectTask`, `loadTaskActivity`, the two-panel template, and the tree rows |
| **Current tests** | [`PlanView.test.ts`](../../../pm-web-app/src/lib/views/PlanView.test.ts), [`pm-web-e2e/tests/plan-view.spec.js`](../../../pm-web-e2e/tests/plan-view.spec.js) |
| **Mock scenarios** | [`running-work.json`](../../../pm-mock-runtime/src/main/resources/scenarios/running-work.json), [`fully-onboarded.json`](../../../pm-mock-runtime/src/main/resources/scenarios/fully-onboarded.json) |
| **Catalogue** | [`plan_view.en.properties`](../../../pm-api/src/main/resources/messages/plan_view.en.properties), [`plan_view.de.properties`](../../../pm-api/src/main/resources/messages/plan_view.de.properties) |
| **Process contract** | [`ProcessTree.java`](../../../pm-api/src/main/java/de/planmarshall/api/process/ProcessTree.java), [`ProcessNode.java`](../../../pm-api/src/main/java/de/planmarshall/api/process/ProcessNode.java), [`LeafTask.java`](../../../pm-api/src/main/java/de/planmarshall/api/process/LeafTask.java) |
| **Operational SOP** | [`runbook.md`](../runbook.md) |

### 1.1 Finding Analysis

| # | Finding | Root cause | Correction |
|---|---|---|---|
| **F1** | On `plans/session-log`, already-done items on the left show no narrative on the right. | **UI cause.** Only `.task-row` calls `selectTask`. Phase and sub-workflow rows call `toggleNode` and nothing else. The inspector is bound to `selectedTaskId`, so a node with no leaf tasks has no selectable descendant and the panel stays at *“Select a task on the left…”*. Done phases such as `init`, `refine`, `outline`, and `plan` carry no leaf tasks at all. | Make every tree row selectable; give node selection its own narrative panel. |
| **F2** | The same symptom could also be mock data. | **Mock cause.** The route tables of `running-work.json` and `fully-onboarded.json` have `fix-relay` and `docs-typo` process routes, but no `GET /api/v1/plans/session-log/process`. The plan is listed in the overview yet cannot be opened from the scenario. `fix-relay` also exposes activity for only `TASK-001` and `TASK-003`; `TASK-002` is done with no activity. | Add the `session-log` process and its task activities to both scenarios, plus a done task without activity, so both paths are exercised. |
| **F3** | The panel cannot distinguish “nothing selected” from “this part has no narrative”. | The empty state is task-only: `plan.activity.select_task` assumes the operator must select a task, even when the selected subject is a phase that has no tasks. A task with no entries uses a separate `plan.activity.empty`, but there is no node-level equivalent. | Add explicit node and empty-narrative states, each naming what is shown and why it is empty. |
| **F4** | Reloading or refreshing the process loses the inspected item. | The selection lives only in component state and is re-derived from “running task, else first task”. | Keep the selection when its subject still exists after a refresh; fall back deliberately and keep the active row visibly selected. |
| **F5** | The positive structure must survive. | The two-panel layout is already right. | Preserve the left tree / right narrative split, the tree order, the expand/collapse toggles, and all current leaf-task behavior. |

**Conclusion.** This is primarily a UI defect (F1) amplified by a mock-data gap (F2). The fix is owned by `pm-web-app` plus its canonical scenarios; it does not need a server-side narrative endpoint.

---

## 2. Scope & Objectives

1. Replace the task-only selection with a selection union:
   - `{ kind: 'task', taskId }` for a leaf task;
   - `{ kind: 'node', nodeId }` for a phase, sub-workflow, or task group.
2. Make the **row body** of every tree item selectable, while keeping the chevron/glyph button as an independent expand/collapse control. A click on the body never toggles; a click on the toggle never selects.
3. Add `NodeNarrative.svelte`:
   - header: node title, kind, state, run count, duration, `waits_for`, `prune_reason`;
   - a child-task list with status, role, harness/model, attempt, duration, tokens, and whether activity exists;
   - selecting a child task switches the inspector to that task's live feed;
   - for a node with no child tasks, an explicit *“completed without task-level activity”* narrative rather than a blank panel.
4. Keep `loadTaskActivity` and the live `subscribeTaskActivity` path exactly as they are for task selection; unsubscribe when a node is selected.
5. Default selection: running task, else running node, else first visible node. Preserve a valid selection across `loadPlanProcess(false)` refreshes.
6. Keep the left tree filtering of `not_in_plan` nodes and the pruned box unchanged.
7. Add the `session-log` process plus task activities to `running-work.json` and `fully-onboarded.json`, and add at least one done task without an activity route.
8. Add catalogue keys in English and German for node selection, node narrative headings, and the two empty states.
9. Add unit, scenario-contract, and Playwright coverage.
10. Document the selection semantics in `doc/design/web-app/plan-view-interaction.adoc`.

### Non-Goals

- No new API endpoint, no new `pm-api` record, no server change.
- No redesign of the process tree, the plan header, the token breakdown, the control panel, or the epic view.
- No change to the live-activity event format or to `stripControlSequences`.
- No aggregation of free-text narratives into a fabricated phase summary: a node narrative reports only what the process resource and its child tasks actually contain.
- No reactivation of the workflow default-units sibling plan.

---

## 3. Isolation & Boundaries

| Repository | Worktree | Branch | Scope |
|---|---|---|---|
| `pm-mcp-clients` | `/Users/oliver/git/worktrees/pm-mcp-clients-session-log-narrative` | `feat/session-log-narrative` | `pm-web-app`, `pm-mock-runtime` scenarios, `pm-api` catalogue, tests, design note, report |

> **Pre-flight**: `git checkout main && git pull --rebase origin main` before creating the worktree. Never branch from stale `main`.

**Sole-owner files**: `PlanView.svelte`, the new node-narrative component, `PlanView.test.ts`, the new component test, `running-work.json`, `fully-onboarded.json`, `plan_view.{en,de}.properties`, `pm-web-e2e/tests/plan-view.spec.js`, the new design note, and the completion report.

**No touch**: `pm-api` process records, `pm-mcp-server`, `pm-operator`, `pm-relay`, `pm-web-app` reports/decisions/layout work, the brand mark, and the advisory-title layer.

---

## 4. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created at `/Users/oliver/git/worktrees/pm-mcp-clients-session-log-narrative` on branch `feat/session-log-narrative` off latest `main`.
- [x] 2. Domain skills & engineering personas loaded where applicable:
  - `plan-marshall-persona-implementer` & `pm-dev-frontend:javascript` for the selection model and node narrative;
  - `plan-marshall-persona-module-tester` & `pm-dev-frontend:jest-testing` for unit and component tests;
  - `plan-marshall-persona-integration-tester` for the scenario-contract and Playwright path;
  - `plan-marshall-persona-security-expert` for untrusted activity text and safe rendering;
  - `plan-marshall-persona-documenter` for the interaction note and report;
  - `plan-marshall-persona-code-reviewer` for review triage.
- [x] 3. Incremental implementation with tests following the phased technical recipe.
- [x] 4. Completion report written (`doc/agy/session-log-narrative-everywhere-plan-report.md`), plan moved to `doc/agy/implemented/`, links rebased, dashboard updated.
- [x] 5. Pre-commit passed: `./mvnw verify -Ppre-commit` (covers the web-app and scenario tests).
- [ ] 6. PR opened (never `skip-bot-review` on worker PRs); review windows and all bot/Sonar findings resolved.
- [ ] 7. Enqueued in the merge queue, landed on `main`, worktree and branch cleaned up.
- [ ] 8. Post-landing verification performed and the report moved to `doc/agy/verified/` by the orchestrator.

> [!IMPORTANT]
> **Plan archival link rule**: this plan moved from `doc/agy/` to `doc/agy/implemented/`. Its links are rebased (`../../../pm-web-app`, `../../../pm-mock-runtime`, `../../../pm-api`, `../../../../plan-marshall-documentation`, `../runbook.md`); a sibling link inside `implemented/` is plain. The internal-link check was run after the move.

---

## 5. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Selection model** | `plan-marshall-persona-implementer` | `pm-dev-frontend:javascript` | Selection union, toggle/select separation, keyboard behavior, refresh stability. |
| **Phase 2: Node narrative** | `plan-marshall-persona-implementer` | `pm-dev-frontend:javascript` | State/timing/child-task rendering, empty states, safe text. |
| **Phase 3: Mock scenarios** | `plan-marshall-persona-integration-tester` | `pm-dev-frontend:javascript` | `session-log` process and activity routes, scenario validation, done-without-activity case. |
| **Phase 4: Tests** | `plan-marshall-persona-module-tester` | `pm-dev-frontend:jest-testing` | Component, scenario-contract, and Playwright coverage. |
| **Phase 5: Docs & review** | `plan-marshall-persona-documenter` | `plan-marshall-persona-code-reviewer` | Interaction note, catalogue copy, report, finding triage. |

---

## 6. Phased Technical Recipe

### Phase 1: Selection Model and Node Selection (`pm-web-app`)

- [x] **1.1 Introduce the selection union**
  - Add `type TreeSelection = { kind: 'task'; taskId: string } | { kind: 'node'; nodeId: string }`.
  - Replace `selectedTaskId` with `selection = $state<TreeSelection | null>(null)`; derive `selectedTaskId = $derived(selection?.kind === 'task' ? selection.taskId : null)` so the existing activity `$effect` keeps working unchanged.
  - Derive `selectedNode = $derived(findNode(plan?.nodes ?? [], selection?.kind === 'node' ? selection.nodeId : null))`.
- [x] **1.2 Make the row body selectable**
  - Keep the current `.toggle-btn` as the expand/collapse control.
  - Wrap the `.node-info` content of top-level nodes and sub-nodes in a `<button type="button" class="node-select" data-testid="select-node-{node.node_id}">`.
  - Add `aria-pressed={isSelected}` and `aria-current={isSelected ? 'true' : undefined}`; keep the visible row styling by resetting button appearance in CSS.
  - `onclick={() => selectNode(node.node_id)}` selects; it never toggles.
  - Keep `.task-row` selection as `selectTask(task.task_id)`.
- [x] **1.3 Default and refresh behavior**
  - On initial load: running task → running node → first visible node.
  - On refresh (`loadPlanProcess(false)`): keep the selection when the task/node still exists; otherwise fall back by the same order.
  - When a node is selected, unsubscribe from task activity and clear `taskActivity`.
- [x] **1.4 Keyboard and focus**
  - The row body is a native button: `Enter`/`Space` select, `Tab` moves between toggle and body.
  - The selected row keeps a visible focus ring and the existing selected-state background.
  - Do not add `tabindex` to non-interactive rows or nest buttons.

### Phase 2: Node Narrative (`pm-web-app`)

- [x] **2.1 Add `NodeNarrative.svelte`**
  - Props: `node: ProcessNode`, `plan: ProcessTree`, `onSelectTask(taskId: string)`.
  - Header: title, kind badge, localized state (reuse the same state-text mapping as the tree; extract `getNodeStateText` into a small shared helper so the tree and the panel cannot drift), run count, duration, `waits_for`, `prune_reason`.
  - Child-task section: one row per leaf task with `task_id`, role, harness/model, attempt, status, duration, tokens, and an activity-availability marker. Selecting a row calls `onSelectTask`.
  - Empty section: when the node has no leaf tasks, render `plan.node.no_activity` with the node's state and duration, e.g. “This phase completed without task-level activity; its duration is recorded above.” Do not render an empty feed.
  - Sub-nodes: list their titles and states as a compact outline, with the same select-node affordance.
  - Every free-text value passes through `stripControlSequences`; no `{@html}`.
- [x] **2.2 Render the inspector**
  - `{#if selection?.kind === 'task'}` keeps the existing `taskActivity` feed and off-track banner.
  - `{:else if selectedNode}` renders `<NodeNarrative … />`.
  - `{:else}` renders `plan.activity.select_item` (replacing the task-only wording).
  - Keep `data-testid="task-inspector"` on the section, and add `data-testid="node-narrative"` and `data-selected-kind={selection?.kind ?? 'none'}` so tests can assert the state without relying on copy.
- [x] **2.3 Selection affordance in the tree**
  - Selected node rows get `node-row--selected`, mirroring `task-row--selected`.
  - The currently auto-selected item is scrolled into view only by the existing `jumpToCurrentPosition`; no automatic scroll on ordinary selection.

### Phase 3: Catalogue (`pm-api`)

- [x] **3.1 Add English keys** to `plan_view.en.properties`:
  - `plan.node.select=Inspect this part`
  - `plan.node.narrative=Part narrative`
  - `plan.node.child_tasks=Tasks in this part`
  - `plan.node.no_activity=This part has no task-level activity. Its state and duration are shown above.`
  - `plan.node.has_activity=Activity recorded`
  - `plan.node.no_activity_link=No activity recorded`
  - `plan.node.run={count, plural, =1 {1 run} other {{count} runs}}`
  - `plan.activity.select_item=Select a part or a task on the left to inspect it.`
- [x] **3.2 Add the German equivalents** in `plan_view.de.properties`; keep every key in both files.
- [x] **3.3 Update `catalogue.test.ts`** only if it enumerates the changed keys; otherwise verify the existing catalogue parity test covers the new keys.

### Phase 4: Mock Scenarios (`pm-mock-runtime`)

- [x] **4.1 Add the `session-log` process route** to `running-work.json`:
  - `GET /api/v1/plans/session-log/process` with a realistic tree: `init`, `refine`, `outline`, `plan` all `done` with no leaf tasks; `execute` `running` with one `done` leaf task (`TASK-001`), one `done` leaf task without activity (`TASK-002`), and one `running` leaf task (`TASK-003`); `finalize` `planned`;
  - a `current_position_path` ending at the running task;
  - project `pm-mcp-core`, matching the overview entry.
- [x] **4.2 Add task-activity routes** for `session-log` `TASK-001` and `TASK-003` with text, tool, file-changed, link, and question entries; deliberately omit `TASK-002` so the “no activity recorded” branch is real.
- [x] **4.3 Mirror the route in `fully-onboarded.json`** so the showroom scenario can open the same plan and the tree/narrative structure appears there as well.
- [x] **4.4 Keep `fix-relay` honest**:
  - leave `TASK-002` without activity (done, no route) or add a route and update the test expectation deliberately; document which choice was made.
  - ensure `TASK-001` activity still proves a done task narrates.
- [x] **4.5 Scenario validation**: run the existing scenario contract/validation tests; every state and route must still validate.

### Phase 5: Tests

- [x] **5.1 `PlanView.test.ts`**
  - clicking a done phase (`node-init`) renders `node-narrative` with its title, state, and duration, and does **not** call `getTaskActivity`;
  - clicking a done leaf task renders its activity entries;
  - clicking a done task without activity renders the node- or task-level empty state without an error;
  - the expand toggle changes `aria-expanded`/chevron but leaves the selection unchanged;
  - refresh keeps a valid node selection and falls back when the node disappears;
  - the default selection is the running task, and a plan with no running task selects the first node;
  - the selected row carries `aria-current`/`node-row--selected`.
- [x] **5.2 `NodeNarrative.test.ts`**
  - header fields, run counts (`1 run` / `N runs`), waits-for and prune reason;
  - child task rows and their selection callback;
  - no-activity copy for a done node with no leaf tasks;
  - control-sequence stripping in any free-text field.
- [x] **5.3 Scenario contract**
  - assert `GET /api/v1/plans/session-log/process` exists in both scenarios and resolves through the scenario loader;
  - assert every leaf task with an `activity_link` has a matching route, except the deliberately-activity-less `TASK-002` (allowlist it in the test with a comment).
- [x] **5.4 Playwright (`pm-web-e2e/tests/plan-view.spec.js`)**
  - open `session-log` from the overview (`[data-testid="plan-card"]` matching the title);
  - assert the tree and the right inspector are both visible (the positive structure);
  - click `select-node-init`; assert `node-narrative` contains `init` and its state, and that the panel is not the “select” empty state;
  - click the done `TASK-001`; assert `activity-entry` content;
  - click the toggle of `execute`; assert the selection and the right panel do not change;
  - keep the existing `fix-relay` assertions green.
- [x] **5.5 Accessibility**
  - assert the row body is a button with an accessible name and `aria-current` when selected;
  - assert the toggle remains separately announced (`plan.view.toggle_node`);
  - assert no nested interactive elements in a row.

### Phase 6: Documentation, Verification, Report

- [x] **6.1 Design note** `doc/design/web-app/plan-view-interaction.adoc`:
  - the two-panel rule (tree left, narrative right);
  - node selection vs. task selection vs. expand/collapse;
  - what a node narrative contains and what it deliberately does not invent;
  - the empty states.
- [x] **6.2 Verification**
  - `cd pm-web-app && npm test -- PlanView NodeNarrative` (or the repository's Vitest invocation);
  - `npm run check`, `npm run lint`;
  - scenario tests and the Playwright `plan-view` spec;
  - `./mvnw verify -Ppre-commit`.
- [ ] **6.3 Report** `doc/agy/session-log-narrative-everywhere-plan-report.md`: the root-cause proof (UI + mock), screenshots or test output for a done phase and a done task, the `session-log` route, the exact commands run, and the result of each acceptance criterion.
- [x] **6.4 Archive**: move the plan to `doc/agy/implemented/`, rebase links, update the dashboard, keep the report for Step 8.

---

## 7. Acceptance Criteria

| Finding | Acceptance criterion | Evidence |
|---|---|---|
| **F1** | Clicking any visible phase, sub-workflow, or task group opens a right-hand narrative for that item; no tree item leaves the panel at “select a task”. | `PlanView.test.ts` + Playwright `session-log` walkthrough. |
| **F2** | `GET /api/v1/plans/session-log/process` exists in `running-work.json` and `fully-onboarded.json`, and the plan opens from the overview. | Scenario-contract test and Playwright open. |
| **F2** | A done leaf task with an activity route shows its entries; a done leaf task without one shows the explicit empty state. | Unit tests for both cases. |
| **F3** | The panel distinguishes “nothing selected”, “node with no task-level activity”, and “task has no entries”. | Three distinct assertions/copy keys. |
| **F4** | A refresh keeps a still-existing selection; a vanished subject falls back deterministically. | `PlanView.test.ts`. |
| **F5** | The left tree and right narrative remain side by side, with the existing order, toggles, glyphs, and leaf-task feed. | Existing plus new Playwright assertions. |
| **—** | No server or `pm-api` process-record change; no `{@html}`; all activity text sanitized. | Diff review and security persona sign-off. |
| **—** | `npm run check`, `npm run lint`, the focused tests, the Playwright spec, and `./mvnw verify -Ppre-commit` pass. | CI / report. |

---

## 8. Risk & Rollback Notes

- **Selection/expand coupling**: the most likely regression is a row body that also toggles, or a toggle that also selects. The tests assert both directions independently.
- **Nested buttons**: do not wrap the toggle inside the selectable body; keep them siblings in `.node-row`. A nested button is invalid HTML and breaks keyboard behavior.
- **Wide node trees**: rendering a node narrative over many child tasks is cheap because it reuses loaded process data; no new fetch and no new subscription.
- **Live feed**: only task selection subscribes. A node selection must unsubscribe, or a background task stream keeps appending to a feed the operator cannot see.
- **Mock route duplication**: the two scenarios are separate fixtures; a route added to one is not automatically in the other. The contract test checks both.
- **Untrusted text**: `waits_for`, `prune_reason`, and activity content are external text; render as text and strip control sequences.
- **Rollback**: revert `PlanView.svelte` to the task-only selection and drop `NodeNarrative.svelte`; the process contract and the server are untouched.
