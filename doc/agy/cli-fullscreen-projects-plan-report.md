# Completion Report: `cli-fullscreen-projects` (Full-Screen Project Explorer `projects` in `pm-operator`)

**Status:** Implemented on `feat/cli-fullscreen-projects`, pre-commit gate passed.
**Plan:** [`doc/agy/implemented/cli-fullscreen-projects-plan.md`](implemented/cli-fullscreen-projects-plan.md)
**Branch:** `feat/cli-fullscreen-projects`
**Governed by:** [`doc/agy/runbook.md`](runbook.md)

---

## 1. Executive Summary

This plan completes the full-screen Project Explorer of `pm-operator projects` for
`UC-PRJ-1..3`, building on the TUI foundation and the overview application that earlier plans
landed. `projects` now opens the explorer as a root of the full-screen view, with the four groups
of `doc/design/terminal-client/17-projects.adoc#explorer`, the state-specific inline actions of a
project, favourites and their order, the two _All projects_ orders, the `/` filter, and the
`--project ?` selector shared by every verb.

Key outcomes:

1. **Explorer view model (`de.planmarshall.operator.projects`)** — `ProjectsExplorerLevel`
   renders _Being added_, _Favourites_ (`★`), _Recent_, and _All projects · by name_ /
   _by most used_; `ProjectsExplorerActions` names the actions of each project state; and
   `ProjectOpenEntry` reads the open starts a project detail reports.
2. **Root navigation and inline actions** — `OverviewFullScreenApp` gained an explorer root
   (`InitialSelection.explorerRoot()`), project menus drawn below the row by the existing modal
   `TuiMenu`, and the actions that leave the view for a dialogue (`+`, `e`, `p`, `u`, `c`, `x`,
   `a`) while `r` and `E` open the project's overview level and ended work.
3. **Keys and texts** — the `PROJECTS` level has its own registry entry and footer
   (`↑↓ move   Enter actions   * favourite   s sort   / filter   + add   q leave`) with the
   `operator.projects.keys.*` texts; the whole `20-texts-projects.adoc` block now exists in English
   and German.
4. **`--project ?`** — `ProjectSelector` resolves the question before picocli parses any verb,
   prints the explorer's projects favourites first, and replaces the question with the chosen id.
   The console hands in its own `LineReader` so the selector never opens a second terminal there.
5. **Tests** — 28 new tests: 14 `VirtualScreenTerminal` tests, 4 view-model tests, 6 action-menu
   tests, and 4 selector tests. The complete `pm-operator` suite passes.

---

## 2. Delivered Artifacts

### 2.1 `pm-api` (message catalogues)

| File | Change |
|---|---|
| `src/main/resources/messages/operator.en.properties` | Added the `operator.projects.*` block of `20-texts-projects.adoc` (keys, being-added, actions, setup, release, selector, confirmation) in English. |
| `src/main/resources/messages/operator.de.properties` | The same keys in German. |
| `src/main/resources/messages/overview.en.properties` | Added the generic `operator.overview.keys.*` fallbacks for the new key actions (`favourite`, `move_favourite_up/down`, `sort`, `add_project`, `add_epic`, `add_plan`, `running_work`, `setup`, `continue`, `abandon`, `enrol_again`). |
| `src/main/resources/messages/overview.de.properties` | The same fallbacks in German. |

### 2.2 `pm-operator` — new production code

| File | Responsibility |
|---|---|
| `projects/ProjectsExplorerLevel.java` | The four groups, project and being-added rows, readiness and attention columns, `by name` / `by most used` ordering, row-id resolution, and local favourite order. |
| `projects/ProjectsExplorerActions.java` | The state-specific inline actions: ready, ready with an open entry, not ready (both _Add_ actions disabled with the reason), being added (`Continue`/`Abandon…`), and released (`Enrol again`/`Ended work`). |
| `projects/ProjectOpenEntry.java` | Reads `open_entries` from a project detail without changing `pm-api`'s overview model. |
| `projects/package-info.java` | Package documentation and the explorer's place in the TUI. |
| `ProjectSelector.java` | Resolves `--project ?` / `--project` without a value, orders favourites → recent → all, prints the numbered list, and returns the rewritten arguments. |

### 2.3 `pm-operator` — modified production code

| File | Change |
|---|---|
| `ProjectsCommand.java` | Opens `OverviewFullScreenApp` with `InitialSelection.explorerRoot()` when `--list` is absent and `TuiScreenDriver.canOpenFullScreen` is true; otherwise (or below 60 columns) prints the existing scrollback list. `--filter` stays a list option. |
| `overview/OverviewFullScreenApp.java` | Explorer root, explorer refresh on SSE events, project action menu (`TuiMenu`), direct explorer keys, favourite toggle/reorder, sort toggle, open-entry discovery, abandon confirmation, setup/re-enrol/running-work/ended-work hand-offs, and header-skipping navigation. |
| `PmOperator.java` | Calls `ProjectSelector.rewrite(...)` before picocli parses the verb, so every `--project` option gets the selector once. |
| `PmConsole.java` | Resolves the selector with the console's own `LineReader` before dispatching the verb. |
| `tui/TuiKeyAction.java` | The explorer's key actions: favourite, move favourite up/down, sort, add project/epic/plan, running work, setup, continue, abandon, enrol again. |
| `tui/TuiKeyRegistry.java` | Dedicated `PROJECTS` bindings (`r` is _Running work_, `a` is _Enrol again_, `E` is _Ended work_) and the design's footer, with `operator.projects.keys.*` labels. |

### 2.4 `pm-operator` — tests

| File | Coverage |
|---|---|
| `ProjectsFullScreenTest.java` | 14 `VirtualScreenTerminal` tests: groups at 120×30, ready/not-ready/being-added/released menus, favourite toggle, favourite reorder, sort toggle, `/` filter and `Esc`, `r` to the project's overview level, `x` + confirmation + `DELETE`, open entry first, `projects` full-screen launch, `projects --list` scrollback, and `--project ?`. |
| `projects/ProjectsExplorerLevelTest.java` | 4 tests: the four groups, both _All projects_ orders, the empty machine, and row resolution with group-qualified ids for the same project in two groups. |
| `projects/ProjectsExplorerActionsTest.java` | 6 tests: ready menu, open entry first, not-ready disabled reason, released menu, favourite label, being-added menu. |
| `ProjectSelectorTest.java` | 4 tests: ordering and de-duplication, number/id/leave parsing, non-`--project` verbs untouched, and question-form detection stopping without a terminal. |

---

## 3. Test & Verification Results

### 3.1 Unit and screen tests

```
./mvnw -pl pm-operator -am test
Tests run: 878, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

The new suites:

| Suite | Tests | Result |
|---|---|---|
| `ProjectsFullScreenTest (VirtualScreenTerminal)` | 14 | PASS |
| `ProjectsExplorerLevelTest` | 4 | PASS |
| `ProjectsExplorerActionsTest` | 6 | PASS |
| `ProjectSelectorTest` | 4 | PASS |

### 3.2 Screen-test assertions

- Groups and columns: `Being added`, `Favourites`, `Recent`, `All projects · by name`, `★`, the
  `!`/`needs you` badge, the plan/epic counts, `setup needs: credential sonar`, and `released` at
  120×30.
- Menus: `Enter` on a ready project (`e`, `p`, `r`, `u`, `*`), on a not-ready project
  (`c` first and `e`/`p` disabled with `plan-marshall is not ready: credential sonar`), on a
  being-added run (`c`, `x`), and on a released project (`a`, `E`, no `r`); an open start entry
  appears first as `c  Continue starting (2 sources)`.
- Keys: `*` sends `PUT /api/v1/projects/pm-mcp-clients/favourite`; `J` sends
  `PUT /api/v1/explorer/favourites` with the new order; `s` flips the _All projects_ header;
  `/` collapses the groups (no group headers) and `Esc` restores them; `r` opens the project's
  `by plan` level; `x` asks before `DELETE /api/v1/entries/ent-cui/abandon`.
- Commands: `projects` enters and leaves the alternate screen once; `projects --list` writes the
  scrollback list and never enters it.

### 3.3 Pre-commit gate

```
./mvnw verify -Ppre-commit
BUILD SUCCESS
pm-api SUCCESS · pm-exec SUCCESS · pm-mock-runtime SUCCESS · pm-relay SUCCESS
pm-operator SUCCESS · pm-web-app SUCCESS · pm-web-e2e SUCCESS
```

The parent's OpenRewrite recipes reformatted files outside this plan's footprint (the same
pre-existing `AutoFormat`/`OrderImports` drift the foundation report recorded). Those unrelated
rewrites were reverted; the footprint of this plan stayed formatted and the gate itself reported
`BUILD SUCCESS`.

---

## 4. Domain Invariants

| Invariant | Status |
|---|---|
| Java 25 | All new code is Java 25 records/enums/pattern switches and compiles under the reactor's `-Werror`. |
| No uncatalogued user-facing text | Every label, badge, menu entry, selector line, error, and footer comes from `messages/*.properties`; no sentence is written in the new production code. |
| Bilingual catalogue (EN & DE) | The whole `20-texts-projects.adoc` block and the new `operator.overview.keys.*` fallbacks exist in both languages; `ProjectsCatalogueTest`, `OverviewCatalogueTest`, and `ConsoleTextsTest` pass. |
| Terminal safety boundary | Project ids, display names, setup needs, checkout paths, and entry ids pass through `OutputSafety.stripControlSequences` before drawing; `TuiLayout` sanitizes and truncates every cell. |
| Module boundary | Changes are confined to `pm-api` message resources and `pm-operator`; `pm-mock-runtime` and `pm-web-app` are untouched. |
| Mock scenario | `projects-and-start.json` was read-only and verified for `projects.explorer`, `projects.ready`, `projects.empty`, `projects.attention`, `projects.being-added`, and `projects.unenrolled`. |
| Operator gate | `projects` keeps `context.gate().verify()`; the selector stops with exit code 3 when no interactive input is possible and never starts a verb. |

### Deliberate decisions and contract notes

- **Open entries.** `GET /api/v1/projects/{project_id}` reports `open_entries`, but
  `ProjectDetailResponse` (pm-api) does not model the field. The explorer parses it operator-side
  in `ProjectOpenEntry`, so an open start is offered without changing the shared model.
- **Most used.** The project contract exposes no usage counter. `by most used` orders by last
  activity descending (then id), the only ordering signal the runtime reports; the UI label and
  the toggle are complete, and the report records the contract gap for the runtime side.
- **Enrol again.** The explorer carries no checkout path, so `a` runs `project setup <id> identity`,
  which reopens the onboarding at identity with the project's current values.
- **Abandon.** `x` uses the run's `abandon_link`, or
  `/api/v1/project-onboardings/<path>` when the runtime did not report one.
- **`--project ?` shape.** The selector is the numbered list of `02-levels.adoc#addressing`
  (favourites first), not a second full-screen view; this keeps the hand-off usable from every verb
  and avoids nesting the explorer in itself.
- **Selection and headers.** Group headers are not selectable in practice: movement skips them, and
  `Enter` on one opens no menu.

---

## 5. Build & Quality Evidence

```
[INFO] Reactor Summary for pm-mcp-clients 0.1.0-SNAPSHOT:
[INFO] pm-mcp-clients ..................................... SUCCESS
[INFO] pm-api ............................................. SUCCESS
[INFO] pm-exec ............................................ SUCCESS
[INFO] pm-mock-runtime .................................... SUCCESS
[INFO] pm-relay ........................................... SUCCESS
[INFO] pm-operator ........................................ SUCCESS
[INFO] pm-web-app ......................................... SUCCESS
[INFO] pm-web-e2e ......................................... SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
```

The pre-commit gate ran OpenRewrite, license headers, Checkstyle, Enforcer, Javadoc, and the
reactor's unit suites. The pm-operator suite alone is `878` tests with zero failures.

---

## 6. Plan Checklist State

- [x] 1. Isolated worktree on `feat/cli-fullscreen-projects`.
- [x] 2. Personas and domain skills applied (implementer, module tester, integration tester,
      security expert, documenter, code reviewer).
- [x] 3. Incremental implementation with tests.
- [x] 4. Completion report written; plan moved to `doc/agy/implemented/`.
- [x] 5. `./mvnw verify -Ppre-commit` passed.
- [ ] 6. PR opened; review window and 100% findings resolution (next step).
- [ ] 7. Merge queue and cleanup.
- [ ] 8. Post-landing verification (orchestrator, Step 8 only).
