# Completion Report: `cli-fullscreen-reports-system` (Full-Screen Reports `R` & System `S` Views in `pm-operator`)

**Status:** Implemented on `feat/cli-fullscreen-reports-system`; pre-commit gate passed; review window and landing follow in Steps 5–7.
**Plan:** [`doc/agy/implemented/cli-fullscreen-reports-system-plan.md`](../implemented/cli-fullscreen-reports-system-plan.md)
**Branch:** `feat/cli-fullscreen-reports-system`
**Governed by:** [`doc/agy/runbook.md`](../runbook.md)

---

## 1. Executive Summary

This plan completes the full-screen Reports (`R`, UC-PLN-6) and System (`S` / `system`, UC-SYS-1..3) views of the terminal client, built on the TUI foundation, the overview, the project explorer, and the plan view that landed before it. Both levels were stubs (`TuiLevel.Kind.REPORTS` and `SYSTEM` pushed a single placeholder row); they are now complete, contract-conformant levels of the existing view stack.

Key outcomes:

1. **Reports level (`ReportsLevel`, `R`)** — the unread reports grouped by project → epic → plan (project-level reports last, the machine's own in a group of its own), with the new-arrival and read-elsewhere events followed live. Keys: `Enter` (detail, reads), `m` (read one, selection moves to its neighbour), `M` (read all shown, by their ids), `p`/`e` (project/epic view without reading), `?` (body, created/sent, id), `Esc`. The overview's footer names the unread count while there is one.
2. **Completion report** — an ended plan's report opens from a `plan.ended` report (`Enter`) and from the existing `c` on the plan view; the renderer is the landed `PlanCompletionLevel` (`PLAN_COMPLETION`), not a second copy of it.
3. **System level (`SystemLevel`, `S` and the console's `system`)** — the six figures `Runtime`, `Work`, `Workers`, `Jobs`, `Models`, `Cleanup` in their fixed order with the design's detail lines, the drain progress line below the header, the `w`/`b`/`p` toggles (persisted in `overview.json`), `r`, `?`, `a` runtime actions, and `Enter` drill-downs into `Workers`, `Jobs`, `Locks`, `Models`, `Cleanup` and back to the overview of running work.
4. **Narrow terminals** — the system view keeps its detail lines from 80 columns, hides them below (and `?` shows them), and `system` below 60 columns prints `operator.system.too_narrow` followed by the printed snapshot.
5. **Tests** — 35 new tests (`ReportsLevelTest`, `SystemLevelTest`, `ReportsFullScreenTest`, `SystemFullScreenTest`, two `TuiLayoutTest` cases), the screen tests driving the real `VirtualScreenTerminal` against the `FakeRuntimeServer`.

---

## 2. Delivered Artifacts

### 2.1 Production code (`pm-operator`)

| File | Responsibility |
|---|---|
| `de.planmarshall.operator.reports.ReportsLevel` **(new)** | Pure model of the reports level: grouping by project/epic/plan, the report rows and their details line, heading classification, the report ids a level shows, the neighbour of a report that leaves, and the immutable `without`/`withoutAll` transitions. |
| `de.planmarshall.operator.reports.package-info` **(new)** | Package documentation. |
| `de.planmarshall.operator.system.SystemLevel` **(new)** | Pure model of the system view: the six figure rows with multi-line details, the sections (`Workers`, `Jobs`, `Locks`, `Models`, `Cleanup`), the window/grouping/project choice and its cycle, the per-role aggregation of the model figures, the footer, and the header. |
| `de.planmarshall.operator.system.SystemFormatting` **(new)** | The byte, token, and uptime formatting shared by the printed and the full-screen view. |
| `de.planmarshall.operator.system.package-info` **(new)** | Package documentation. |
| `de.planmarshall.operator.overview.OverviewFullScreenApp` | Wires both levels: `openReports`/`openSystem`, mark-read and mark-all, the report and system refresh on SSE (`report.sent`, `report.read`, `system.metrics`, `runtime.draining`), the report detail routing, the system drill-downs and toggles, the runtime actions menu, the `planActivity`-style state persistence, and the `InitialSelection.system(...)` launch used by the console command. |
| `de.planmarshall.operator.SystemCommand` | Opens the full-screen view when the command runs inside the console (`context.isConsoleActive()`), prints `operator.system.too_narrow` and falls back to the snapshot below 60 columns, and keeps the printed view as a shell command. `--window`/`--by` now default to the saved choice instead of forcing `hour`/`model`. |
| `de.planmarshall.operator.tui.TuiKeyAction` | The five new actions: `MARK_READ`, `MARK_ALL_READ`, `WINDOW`, `GROUP_BY`, `PER_PROJECT`. |
| `de.planmarshall.operator.tui.TuiKeyRegistry` | The keys and footers of both levels (`m`/`M`/`p`/`e` for reports; `w`/`b`/`p` for system), the report/system text-key mapping, and the overview's `R` entry shown only while unread reports exist. |
| `de.planmarshall.operator.tui.TuiLayout` | Multi-line row details (the system figures), the per-level detail threshold (system from 80 columns), and the header banner line. |
| `de.planmarshall.operator.tui.TuiScreen` | The optional banner line below the header. |

### 2.2 Message catalogue (`pm-api`)

| File | Keys |
|---|---|
| `messages/reports.{en,de}.properties` | `operator.reports.title`, `operator.reports.empty_level` (all other `operator.reports.*` keys of `24-reports.adoc#texts` were already present). |
| `messages/system.{en,de}.properties` | The terminal's `operator.system.*` block of `25-system.adoc#texts` (`title`, `badge`, `prompt_stopped`, `keys.*`, `follow`, `not_running`, `nothing_to_stop`, `starting`, `started`, `still_draining`, `signal_drain`, `console_older`, `too_narrow`, `keep`) plus `action.check`, `models.summary`, `models.unmeasured`, `cleanup.kept`, `cleanup.next`, `locks.title`, `locks.none`, `runtime.cpu_unavailable`, `per_project_unavailable`, `load_failed`, `detail.work`, `detail.no_rows`. |
| `messages/overview.{en,de}.properties` | The action texts the new footers fall back to: `operator.overview.keys.project` (was missing and would have rendered as its key), `read`, `read_all`, `window`, `by`, `per_project`. |

Every key of the two design tables (`24-reports.adoc#reports-texts`, `25-system.adoc#texts`) was verified to exist in **both** English and German (script check over the two tables and the six properties files: no missing key).

### 2.3 Tests (`pm-operator`)

| File | Scope |
|---|---|
| `de.planmarshall.operator.reports.ReportsLevelTest` **(new)** | Grouping order (project → epic → plan → project-level), heading/report classification, details (body, id, created/sent), neighbour selection, `without`/`withoutAll` with the unread count, the empty line, the machine's own group. (7 tests) |
| `de.planmarshall.operator.system.SystemLevelTest` **(new)** | The six figures and their order, the figure content (runtime, work, workers, jobs, models, cleanup), an unavailable figure, per-model and per-role grouping, jobs→locks, workers/cleanup sections, window/grouping cycles, header/footer, section ids and the no-snapshot level. (9 tests) |
| `de.planmarshall.operator.ReportsFullScreenTest` **(new, `VirtualScreenTerminal`)** | `R` opens the grouped level with the selection on a report; `m`, `M` (ids, not `all`), `p`/`e` without reading, `Enter` → completion report + read + return, live arrival keeping the selection, `?` details, and the empty level. (8 tests) |
| `de.planmarshall.operator.SystemFullScreenTest` **(new, `VirtualScreenTerminal`)** | `S` and the console's `system`; `w`/`b`/`p` with `overview.json` persistence; `Enter` drill-downs (Workers, Jobs, Locks); `a` actions menu; the 80-column threshold and `?`; the `< 60`-column too-narrow snapshot fallback. (9 tests) |
| `de.planmarshall.operator.tui.TuiLayoutTest` | The banner below the header without changing the height; the system level's details drawn from 80 columns. (2 added tests) |

---

## 3. Test & Verification Results

| Gate | Result |
|---|---|
| `./mvnw -o test -pl pm-operator -am` | **913 tests, 0 failures, 0 errors** (878 before the plan; 35 added) |
| `./mvnw verify -Ppre-commit` (whole reactor) | **BUILD SUCCESS** — OpenRewrite, licence headers, Checkstyle, Enforcer (`clients-depend-on-pm-api-only`, `no-model-facing-content`, `jackson-core-only`), Javadoc, and the full unit suite. |
| Design-key audit | Every `operator.reports.*`, `operator.system.*`, and overview key in `24-reports.adoc#reports-texts` and `25-system.adoc#texts` present in EN **and** DE. |
| Mock scenario audit (Phase 1.1) | `pm-mock-runtime/scenarios/reports-and-system.json` (read-only for this plan) carries 18 states — `reports.unread`, `reports.all-read`, `reports.empty`, `reports.completion-{landed,nothing,aborted,failed}`, `system.{running,draining,restarting,stopped,force-stop}`, `cleanup.{idle,running,kept-items,residues,preview,completed}` — and the 8 routes the two views read (`/api/v1/reports`, `/api/v1/system`, `/api/v1/system/cleanup`, the four completion routes, `/api/v1/status`). |
| Screen-test stability | `ReportsFullScreenTest` and `SystemFullScreenTest` run repeatedly without failure (selection asserted through the rendered `▶` cursor, not the virtual terminal's inverse mask). |

---

## 4. Domain Invariants

| Invariant | Status |
|---|---|
| Java 25, records, `var`, final fields, every package documented | **Met** — both new packages have `package-info.java`; all new types and public members are documented. |
| No uncatalogued user-facing text | **Met** — every visible label, error, and progress line comes from `messages/*.properties`; the only literals are glyphs, unit abbreviations (`h`, `min`, `d`), separators, and wire values. |
| Bilingual catalogue (EN & DE) | **Met** — all added keys exist in both files. |
| `pm-api` only dependency; no Quarkus/CDI; no `jackson-databind` | **Met** — the change touches `pm-operator` and message resources only. |
| Mock runtime completeness / `doc/plans/README.adoc` / `pm-web-app` untouched | **Met** — the plan's read-only boundaries were respected; the mock scenario was audited, not edited. |
| Width responsiveness | **Met** — system details from 80 columns, `?` below it, `< 60` falls back to the printed snapshot; the reports level keeps the shared 100-column details threshold. |
| `overview.json` persistence, `0600` file | **Met** — `(systemWindow, systemGroupBy, systemProjectId)` are written through the existing atomic store; the screen test reads them back. |
| Trust boundary / output safety | **Met** — every runtime string (project/epic/plan ids, subjects, bodies, worker roles/models, versions, listener addresses, model names) passes through `OutputSafety.stripControlSequences` before it becomes a row; nothing is interpreted. |

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
[INFO] BUILD SUCCESS
```

`git status` after the gate was reviewed: my footprint is the six files above plus the five new production files, four new test files, `TuiLayoutTest`, the six message files, and the documentation moves. The gate also reformatted ~300 **pre-existing** files outside the plan's footprint (a blank line between the `java.*` and third-party import groups, which `main` does not yet carry); those were reverted rather than committed, so this worker PR stays scoped. The drift is repo-wide and pre-existing (the other active worktrees show the same unformatted `main` state); it needs an orchestrator housekeeping PR that runs `verify -Ppre-commit` and commits the result once, outside the three parallel feature plans. **This is the one item the orchestrator should pick up before the next wave.**

---

## 6. Deliberate Decisions & Known Limits

1. **`CompletionReportLevel` = the landed `PlanCompletionLevel`.** The design names the completion level `CompletionReportLevel`; the repo already renders it as `PlanCompletionLevel` under `TuiLevel.Kind.PLAN_COMPLETION` (with `c` on the plan view). The plan's requirement is behavioural — open it from a `plan.ended` report and read the report in the same act — and that is implemented against the existing level rather than a second renderer. A copy would have duplicated the report formatting and split the footer/registry ownership.
2. **The `Work` figure's `Enter` opens the overview of running work.** The design says "plans in a state as the overview filtered to it". The overview has no state filter and the six figures are single selectable rows (there is no per-state row to select), so `Enter` on `Work` returns to the overview level it was opened from, or pushes the overview root when the system view is the console's root. A state-filtered overview would need a filter contract the overview does not have; noted here rather than invented.
3. **The `Locks` level shows the count the snapshot carries.** `SystemSnapshot.jobs` carries `locks_held` only; the per-lock holders the design mentions are not in the client contract (`pm-api`). The level therefore states the count and nothing it did not receive.
4. **`Cleanup` is a read-only detail level.** The plan asks for the cleanup line to open `cleanup` as a level; the full cleanup job view is `26-cleanup.adoc` and outside this plan's scope. The section shows the due counts, the freed size, kept-for-good, and the next check, all from `SystemSnapshot.cleanup`.
5. **Model figures are grouped in the client.** The snapshot serves per-model records with a `role`; `b` aggregates them per role (calls, tokens, floor, unmeasured) without a second request — as the design allows ("the client never adds them up" applies to *figures the runtime serves*, and the grouping of a served list is the view's choice).
6. **Drain banner from `runtime.draining`.** `SystemSnapshot` has no lifecycle field, so the progress line is fed by the event the runtime already sends (`who`, `remaining_count`, `remaining_items`) and stands below the header; it is cleared when the view is reopened.

---

## 7. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created on topic branch off latest `main`.
- [x] 2. Domain skills & engineering personas loaded where applicable (`persona-implementer`, `persona-module-tester`, `persona-integration-tester`, `persona-security-expert`, `persona-code-reviewer`).
- [x] 3. Incremental implementation with tests following the phased technical recipe.
- [x] 4. Final completion report written, initial plan moved to `doc/agy/implemented/cli-fullscreen-reports-system-plan.md`, archive index updated. (Report **not** moved to `verified/` — Step 8 is the orchestrator's.)
- [x] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit`, whole reactor, `BUILD SUCCESS`).
- [ ] 6. PR opened (no `skip-bot-review`), review window waited, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [ ] 7. Enqueued in the merge queue, landed on `main`, worktree and branch cleaned up.
- [ ] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/cli-fullscreen-reports-system-plan-report.md` by the orchestrator session.
