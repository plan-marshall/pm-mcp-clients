# Completion Report: `session-log-narrative-everywhere`

**Plan:** [`session-log-narrative-everywhere-plan.md`](implemented/session-log-narrative-everywhere-plan.md)
**Branch:** `feat/session-log-narrative`
**Worktree:** `/Users/oliver/git/worktrees/pm-mcp-clients-session-log-narrative`

---

## 1. Executive Summary

The plan view's left tree is now a complete navigation surface. Every visible item — phase, sub-workflow, task group, or leaf task — selects and produces a right-hand narrative. Leaf tasks keep their live activity feed; phases and sub-workflows show their own state, timing, run count, `waits_for`, `prune_reason`, the tasks they carry, and a compact outline of their sub-parts.

The root cause was confirmed as a UI limitation: only `.task-row` called the selection function, while phase and sub-workflow rows only toggled their children. The empty state was also task-only, and a refresh could lose the inspected subject. The mock gap was confirmed separately: neither canonical scenario served `GET /api/v1/plans/session-log/process`.

No server, endpoint, `pm-api` process record, or live-event format changed. A node narrative is derived from the `ProcessTree` the view already holds.

---

## 2. Delivered Artifacts

### `pm-web-app`

| File | Change |
|---|---|
| `src/lib/views/PlanView.svelte` | `TreeSelection` union (`task` / `node`); selectable row bodies as native buttons; independent expand toggles with `aria-expanded`; recursive tree rendering at every nesting depth; deliberate default/refresh fallback; stale-activity generation guard; node narrative inspector branch; shared state helper; selected-row affordances. |
| `src/lib/components/NodeNarrative.svelte` | New narrative panel: title, kind, state, run count, duration, waits-for, prune reason, carried tasks with activity markers, sub-part outline, explicit no-activity state. |
| `src/lib/utils/processNode.ts` | New shared `getNodeGlyph` / `getNodeStateText`, used by both the tree and the narrative. |
| `src/lib/views/PlanView.test.ts` | Node selection, done-task activity, done-task empty state, toggle independence, refresh stability/fallback, nested sub-workflow depth, stale activity-load ordering, default selection, accessibility. |
| `src/lib/components/NodeNarrative.test.ts` | Header fields, run counts, child-task callback, sub-node callback, no-activity copy, control-sequence stripping. |
| `src/lib/api/scenarioContracts.test.ts` | New contract test over both canonical scenarios: `session-log` process and activity routes, activity-link/route consistency, deliberate activity-less task. |

### `pm-api` (catalogue only, no process-record change)

| File | Change |
|---|---|
| `src/main/resources/messages/plan_view.en.properties` | New node-selection, node-narrative, child-task, run-count, waits-for, prune-reason, attempt, and empty-state keys. |
| `src/main/resources/messages/plan_view.de.properties` | German equivalents; key sets stay identical. |
| `src/test/java/de/planmarshall/api/message/PlanViewCatalogueTest.java` | Required-key list extended with the new keys. |

### `pm-mock-runtime`

| File | Change |
|---|---|
| `src/main/resources/scenarios/running-work.json` | `GET /api/v1/plans/session-log/process`; `TASK-001` and `TASK-003` activity routes; done phases with no leaf tasks; `TASK-002` done without activity. |
| `src/main/resources/scenarios/fully-onboarded.json` | The same `session-log` route and activity routes, so the showroom scenario exercises the same structure. |

`fix-relay` `TASK-002` decision: it stays a **done task without worker activity**. The stale `activity_link` was removed (no route existed for it), so the “No activity recorded” marker and the task's empty feed are both truthful. The contract test asserts every remaining `activity_link` has a matching route and that the named activity-less tasks have neither.

### Documentation

| File | Change |
|---|---|
| `doc/design/web-app/plan-view-interaction.adoc` | New interaction note: two-panel rule, select/toggle separation, default and refresh semantics, node-narrative content, three empty states, untrusted-text rule, text keys. |
| `doc/design/web-app/README.adoc` | Links the new interaction note from the plan-view entry. |

### End-to-end

| File | Change |
|---|---|
| `pm-web-e2e/tests/plan-view.spec.js` | Opens `session-log` from the overview, asserts the positive two-panel structure, narrates a done phase, opens a done task's feed, and proves the expand toggle leaves the selection and inspector unchanged. Existing `fix-relay` assertions remain. |

---

## 3. Test & Verification Results

All commands were run in the plan worktree.

### Web app

```text
$ cd pm-web-app
$ npm test
Test Files  56 passed (56)
Tests       490 passed (490)
```

Focused suites:

```text
$ npm test -- PlanView NodeNarrative scenarioContracts catalogue
Test Files  4 passed (4)
Tests       37 passed (37)
```

```text
$ npm run check
svelte-check found 0 errors and 0 warnings

$ npm run lint
(no output; exit 0)

$ npx prettier --check src/lib/views/PlanView.svelte src/lib/components/NodeNarrative.svelte \
    src/lib/views/PlanView.test.ts src/lib/components/NodeNarrative.test.ts \
    src/lib/api/scenarioContracts.test.ts src/lib/utils/processNode.ts
All matched files use Prettier code style!
```

### Mock runtime / scenario contracts

```text
$ ./mvnw test -pl pm-mock-runtime -am \
    -Dtest='ScenarioLoaderTest,ScenarioRegistryTest,StandardScenariosTest,FullyOnboardedScenarioTest' \
    -Dsurefire.failIfNoSpecifiedTests=false
Tests run: 56, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

The web-app scenario contract suite (above) additionally loads both canonical scenario files through the same `ScenarioMockFetch` the app uses and walks every process route.

### Playwright

```text
$ cd pm-web-e2e
$ npx playwright test tests/plan-view.spec.js
5 passed (23.3s)
```

The new `session-log` walkthrough passed, including:

- the tree and the right inspector both visible;
- `select-node-init` renders `node-narrative` with `init`, `done`, and a duration;
- the panel is not the “select something” state;
- `TASK-001` opens its activity entries;
- collapsing `execute` leaves `data-selected-kind="task"` and the same inspector head.

### Repository pre-commit gate

```text
$ ./mvnw verify -Ppre-commit
...
[INFO] Reactor Summary for pm-mcp-clients 0.1.0-SNAPSHOT:
[INFO] pm-mcp-clients ..................................... SUCCESS
[INFO] pm-api ............................................. SUCCESS
[INFO] pm-exec ............................................ SUCCESS
[INFO] pm-mock-runtime .................................... SUCCESS
[INFO] pm-relay ........................................... SUCCESS
[INFO] pm-operator ........................................ SUCCESS
[INFO] pm-web-app ......................................... SUCCESS
[INFO] pm-web-e2e ......................................... SUCCESS
[INFO] BUILD SUCCESS
```

The gate completed with a clean working tree: no uncommitted OpenRewrite reformatting and no untracked build artifacts.

---

## 4. Acceptance Criteria

| Finding | Criterion | Result |
|---|---|---|
| **F1** | Any visible phase, sub-workflow, task group, or task opens a right-hand narrative; no tree item leaves the panel at “select a task”. | **Met.** Row bodies are native buttons; `data-selected-kind` is `node` or `task`; default selection always names a visible item. Unit and Playwright evidence above. |
| **F2** | `GET /api/v1/plans/session-log/process` exists in both canonical scenarios and the plan opens from the overview. | **Met.** Route added to `running-work.json` and `fully-onboarded.json`; contract test and Playwright open `session-log` from the overview. |
| **F2** | A done leaf task with activity shows entries; a done leaf task without activity shows the explicit empty state. | **Met.** `TASK-001` (session-log, fix-relay) shows entries; `TASK-002` (both) shows the explicit task empty state. |
| **F3** | The panel distinguishes nothing selected, node with no task-level activity, and task with no entries. | **Met.** Three distinct keys: `plan.activity.select_item`, `plan.node.no_activity`, `plan.activity.empty`. |
| **F4** | A refresh keeps a still-existing selection; a vanished subject falls back deterministically. | **Met.** `PlanView.test.ts` keeps `init` across `plan.changed` and falls back to the running task when `init` disappears. |
| **F5** | The left tree and right narrative remain side by side, with the existing order, toggles, glyphs, and leaf feed. | **Met.** Layout, order, expand/collapse, glyphs, and the existing task feed are unchanged; Playwright asserts both panels visible. |
| — | No server or `pm-api` process-record change; no `{@html}`; all activity text sanitized. | **Met.** `git diff` touches no process record or server; the narrative contains no `{@html}` and strips control sequences from title, waits-for, prune reason, sub-node titles, task labels, and values. |
| — | `npm run check`, `npm run lint`, focused tests, Playwright spec, and `./mvnw verify -Ppre-commit` pass. | **Met.** Evidence in section 3. |

---

## 5. Domain Invariants

| Invariant | Status |
|---|---|
| Java 25 / released toolchain | Unchanged; pre-commit reactor passed. |
| No uncatalogued user-facing strings | Both new components and the modified view look up every text through `$t`; the catalogue guard passed. |
| Bilingual catalogue in EN & DE | Every new key exists in both files; `PlanViewCatalogueTest` and the web-app catalogue parity test passed. |
| Scenario completeness and validation | Both canonical scenarios load and validate; the scenario-contract test walks every process route. |
| No new server endpoint or process record | Confirmed by diff: `pm-api` changes are catalogue/test only. |
| Security: untrusted text and safe rendering | `stripControlSequences` on all free text; no `{@html}`; no new network fetch or subscription. |
| Live feed discipline | Only task selections subscribe; node selection clears `taskActivity` and unsubscribes through the existing `$effect`. |
| Responsive layout | Existing two-column stack rule at `768px` unchanged; the new narrative uses the same tokens. |

---

## 6. Notes for Step 8 (Orchestrator)

- This report remains in `doc/agy/` for the orchestrator's post-landing verification; it is **not** moved to `doc/agy/verified/` by this session.
- The plan is moved to [`doc/agy/implemented/`](implemented/) with its internal links rebased.
- The only deliberate fixture adjustment beyond the plan text is the removal of the stale `fix-relay` `TASK-002` `activity_link`; the task remains done and activity-less, matching finding F2 and the plan's “done task without activity” requirement.
