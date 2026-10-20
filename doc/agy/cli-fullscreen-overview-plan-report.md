# Completion Report: `cli-fullscreen-overview` (Full-Screen Overview `plans` in `pm-operator`)

**Status:** Implemented & verified locally under [`doc/meta/verification-protocol.md`](../meta/verification-protocol.md)
**Plan:** [`doc/agy/implemented/cli-fullscreen-overview-plan.md`](implemented/cli-fullscreen-overview-plan.md)
**Branch:** `feat/cli-fullscreen-overview`
**Governed by:** [`doc/agy/runbook.md`](runbook.md) and [`doc/meta/verification-protocol.md`](../meta/verification-protocol.md)

---

## 1. Executive Summary

This plan completes the full-screen implementation of `pm-operator plans` (`UC-PLN-4: See What Runs, Across Projects`, `doc/plans/implemented/cli-overview.adoc`), operationalizing the full-screen terminal design in [`doc/design/terminal-client/15-overview.adoc`](../design/terminal-client/15-overview.adoc) and [`doc/design/shared/plan-overview.adoc`](../design/shared/plan-overview.adoc) using the TUI foundation landed in `feat/cli-fullscreen-foundation` (#124).

Key deliverables include:
1. **Level 1 — Projects Overview Level (`ProjectsOverviewLevel`)**:
   - Renders running work across all active projects with attention ordering (`! <count>` markers first), recent activity relative timestamps, active counts (`3 plans · 1 epic`), and setup state detection (`setup needs you`).
   - Handles navigation keys: `↑`/`↓` (`k`/`j`), `PgUp`/`PgDn` (`g`/`G`), `Enter`/`→`/`l` (opens project detail level; setup hand-off hook), `E` (opens project ended work), `/` (filter), `r` (runtime reload), `?` (help overlay), and `q` (leaves full-screen view).
2. **Level 2 — Project Detail Level (`ProjectWorkLevel`, By Plan & By Epic)**:
   - **By Plan view**: renders two-line plan rows on `>= 100` columns (title, phase & state; running duration, phase duration, task progress, epic membership, off-track signal, PR number with plain URL revealed on `?`) and single-line compact rows on `60..99` columns; renders `alongside` group for ongoing/failed landings and setup runs.
   - **By Epic view**: renders epic rows with running/staged counts and progress, child plans nested under `├─` / `└─` branches, and `without epic` plans (including orphan plans with unreferenced or closed epics) grouped below.
   - Keys: `Tab` toggles between `by plan` and `by epic` preserving selection anchor by ID; `e` on a plan jumps to its parent epic in `by epic` view; `!` and `Enter` drill-down provide navigation and inspection hooks for upcoming view integration; `E` opens ended work for the project; `Esc`/`←`/`h` returns to Projects level.
3. **Ended Work Level (`EndedWorkLevel`, `E`)**:
   - Renders ended work items with localized outcome (`merged`, `no change`, `handed back`, `returned`, `cancelled`, `abandoned`, `released`, `aborted`, `completed` in EN & DE), PR number, and relative end times.
   - Automatic pagination: loads subsequent pages (`GET /api/v1/projects/{id}/ended?page=<n>`) as the operator scrolls to the bottom (`G`/`PgDn`), with `refreshFirstPage` retaining previously loaded older pages across live SSE updates.
   - `Esc`/`←`/`h` returns to the exact caller level (Projects level or Project detail level).
4. **Live SSE Events, Held Notices & State Persistence (`OverviewFullScreenApp`)**:
   - Subscribes to `/api/v1/events` (`project.*`, `phase.changed`, `activity.changed`, `needs_operator.changed`, `hold.*`, `condition.*`, `epic.row_changed`, `task.off_track`), refreshing data and redrawing on the single TUI thread while keeping selection anchored by item ID.
   - Persists `(projectId, projectView, selectedId)` to `$XDG_STATE_HOME/pm-operator/overview.json` on `q` exit with strict `0600` file / `0700` directory permissions, restoring state on next launch with fallback if the saved item has ended.
   - Holds console arrival notices via `ConsoleNoticeBuffer` while full-screen is active, printing coalesced summary on exit.
5. **Interactive Launch Gate & Fallback Wiring (`PlansCommand`)**:
   - Launches `OverviewFullScreenApp` when `TuiScreenDriver.canOpenFullScreen(...)` is true (interactive terminal, `>= 60` columns, `console.full_screen != false`, no `--json`), honouring `--project`, `--by`/`--by-epic`, and `--ended` CLI flags.
   - Prints `operator.overview.too_narrow` and delegates to hierarchical `StatusCommand` tree output when terminal is `< 60` columns, or non-interactive.

---

## 2. Delivered Artifacts

### 2.1 Production Code (`pm-operator`)

| File | Responsibility |
|---|---|
| `de.planmarshall.operator.overview.package-info.java` | Package documentation and architectural overview of full-screen view models and application. |
| `de.planmarshall.operator.overview.OverviewFormatting.java` | Text formatting utilities: duration formatting (`formatDuration`), relative activity formatting (`formatRelativeActivity`), phase state localization (`formatPhaseState`), attention marker badges (`formatAttentionMarker`), OSC 8 hyperlink generation, and control character sanitization. |
| `de.planmarshall.operator.overview.ProjectsOverviewLevel.java` | Level 1 view model for projects overview: row construction, attention sorting (`needsOperator` first), empty state handling (`overview.projects.empty`), and setup row precedence. |
| `de.planmarshall.operator.overview.ProjectWorkLevel.java` | Level 2 view model for project running work (`BY_PLAN` and `BY_EPIC` modes): nested tree branches (`├─`/`└─`), `without epic` grouping, `alongside` landings, and setup rows. |
| `de.planmarshall.operator.overview.EndedWorkLevel.java` | View model for ended work: outcomes, terminal reasons, relative end timestamps, and auto-pagination merge logic (`appendOlderPage`). |
| `de.planmarshall.operator.overview.OverviewFullScreenApp.java` | Full-screen application coordinator: state store persistence, notice buffer lifecycle, SSE event listener thread, data refreshing, and driver execution. |
| `de.planmarshall.operator.PlansCommand.java` | Full-screen launch gate via `canOpenFullScreen`, argument passing (`--project`, `--by`, `--ended`), notice buffer flushing, and fallback to `StatusCommand`. |
| `de.planmarshall.operator.OperatorContext.java` | Context wiring exposing the notice buffer. |
| `de.planmarshall.operator.OperatorUrlUtil.java` | URL segment encoding helper. |
| `de.planmarshall.operator.PmConsole.java` | Console notice buffer integration for full-screen hand-offs. |

### 2.2 Test Code (`pm-operator`)

| File | Test Scope & Verification |
|---|---|
| `OverviewFormattingTest.java` | Unit tests for duration, relative time, phase state localization in EN & DE, attention badges, sort comparator transitivity, and OSC 8 links. (18 tests) |
| `ProjectsOverviewLevelTest.java` | Unit tests for Level 1 row ordering, attention badge prioritization, empty state message, and metadata. (4 tests) |
| `ProjectWorkLevelTest.java` | Unit tests for `BY_PLAN` and `BY_EPIC` row formatting, tree branches, alongside landings, setup rows, orphan plan grouping, and detail help visibility. (6 tests) |
| `EndedWorkLevelTest.java` | Unit tests for ended work pagination, German outcome localization and sanitization, refreshFirstPage older item retention, PR numbers, and empty page handling. (6 tests) |
| `OverviewFullScreenAppTest.java` | Unit tests for event classification, notice buffering, live event handling, level switching, state persistence, view navigation actions, and error handling. (14 tests) |
| `OverviewFullScreenTest.java` | End-to-end `VirtualScreenTerminal` tests covering layout at 120x30, Enter/Tab/e/Esc navigation, / filter, E pagination, 80x24 responsive widths, 50x20 too-narrow fallback, live SSE updates with anchored selection, 0600 overview.json persistence round-trip, notice buffer draining, reports/system views, help/drill-down toggles, direct project and ended startup, and SSE notice event categories. (12 tests) |
| `PmOperatorProcessIT.java` | Subprocess IT testing non-interactive execution of `pm-operator plans` against mock runtime, verifying hierarchical tree fallback output. (1 test) |

---

## 3. Test & Verification Results

### 3.1 Unit & Screen Test Suite
All 60 overview-specific tests pass cleanly:
- `OverviewFormattingTest`: 18/18 passed
- `ProjectsOverviewLevelTest`: 4/4 passed
- `ProjectWorkLevelTest`: 6/6 passed
- `EndedWorkLevelTest`: 6/6 passed
- `OverviewFullScreenAppTest`: 14/14 passed
- `OverviewFullScreenTest`: 12/12 passed

Full `pm-operator` test suite:
- **Total Tests Run**: 710
- **Failures**: 0
- **Errors**: 0
- **Skipped**: 0
- **Status**: `BUILD SUCCESS`

### 3.2 Pre-Commit Gate & Code Quality
- Gate command: `./mvnw verify -Ppre-commit`
- Reactor build, Checkstyle, OpenRewrite formatting, Enforcer rules, Javadoc generation, and all module unit/integration tests verified.
- 100% resolution of automated review findings: all SonarCloud issues resolved (0 bugs, 0 vulnerabilities, 0 code smells, cognitive complexity reduced under threshold, streams adopted, test coverage expanded across views).

---

## 4. Domain Invariants Maintained

1. **Terminal Safety Boundary**:
   - All external strings (titles, plan names, project names, error messages) rendered in TUI cells are sanitized via `OutputSafety.stripControlSequences(...)` and newline/tab removal to prevent ANSI escape sequence injection.
2. **Strict File Permissions (`0600` / `0700`)**:
   - `$XDG_STATE_HOME/pm-operator/overview.json` is written atomically with POSIX permissions `rw-------` (`0600`), and parent directory created with `rwx------` (`0700`).
3. **Module Isolation & Banned Dependencies**:
   - Sole owner of `de.planmarshall.operator.overview.*` and `PlansCommand.java`.
   - Read-only consumer of `pm-api` messages and `pm-mock-runtime`.
   - Never modifies `pm-web-app` or `pm-mock-runtime`.
4. **Bilingual Message Catalogue**:
   - All user-facing strings are looked up via `MessageCatalogue` from `messages/overview.{en,de}.properties` and `messages/operator.{en,de}.properties`. No hardcoded model-facing or English-only literals in production rendering code.
5. **Responsive Column Discipline**:
   - At `>= 100` columns: two-line detailed rows.
   - At `60..99` columns: single-line compact rows with `?` revealing details.
   - Below 60 columns: graceful fallback to `StatusCommand` hierarchical tree output with `operator.overview.too_narrow` notice.
