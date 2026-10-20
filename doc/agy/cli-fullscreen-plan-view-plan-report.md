# Completion Report: `cli-fullscreen-plan-view`

**Plan:** [`cli-fullscreen-plan-view-plan.md`](implemented/cli-fullscreen-plan-view-plan.md)
**Branch:** `feat/cli-fullscreen-plan-view`
**Use case:** UC-PLN-1 — *Follow a Plan Down to Its Parts*
**Worktree:** `/Users/oliver/git/worktrees/pm-mcp-clients-cli-fullscreen-plan-view`

---

## 1. Executive Summary

The full-screen Plan View is implemented in `pm-operator` as the third level of the existing
full-screen stack. It opens on `Enter` from a plan row in the Overview, from an epic's view, from
Ended Work, and directly through `pm-operator plan <plan_id>` when the console is interactive and
no scrollback-only flag is given. The view renders the plan's process as an expandable tree, keeps
the selection anchored on its node id while the runtime advances, streams a worker's activity as
plain sanitized text, and offers one-key jumps to the project (`p`), the epic (`e`), and an ended
plan's completion report (`c`), with `Esc` returning to the exact caller level.

The implementation is confined to the plan's boundary: the new
`de.planmarshall.operator.planview` package, the plan-view integration in
`OverviewFullScreenApp`/`PlanCommand`, and the shared TUI seams that the view needs. `pm-api` is
consumed read-only except for its message catalogues; `pm-mock-runtime` and `pm-web-app` are
untouched. `doc/plans/README.adoc` is untouched as required by the plan invariant.

---

## 2. Delivered Artifacts

### 2.1 New package `de.planmarshall.operator.planview`

| File | Purpose |
|---|---|
| `PlanViewLevel.java` | The expandable/collapsible process tree, the path to the current position, the summary and tokens lines, the token breakdown by phase and role, leaf details, lane-node toggling, and the dynamic footer. |
| `WorkerActivityLevel.java` | The live worker-at-work level: role, harness, model, attempt, duration and tokens; off-track banner; gap and server-only notes; plain-text activity entries. |
| `PlanCompletionLevel.java` | A read-only ended plan's completion report as a full-screen level. |
| `PlanViewFormatting.java` | Pure formatting helpers (sanitizing, durations, state labels, tokens, task lines, off-track text). |
| `package-info.java` | Package contract: view models only, read-only runtime data, `OutputSafety` at every cell. |

### 2.2 `pm-operator` integration

| File | Change |
|---|---|
| `PlanCommand.java` | Launches the full-screen plan view for interactive `plan <id>` without scrollback flags; otherwise keeps the transcript-compatible scrollback tree. |
| `OverviewFullScreenApp.java` | Registers `PlanViewLevel`, `WorkerActivityLevel` and `PlanCompletionLevel`; opens plans from the Overview, epic view and Ended Work; wires `p`, `e`, `c`, `.`, `*`, `n`, `t`, `h`/`←`, `l`/`→`, `Esc`; refreshes the tree and activity on SSE events; hands a waiting node to the open questions after leaving the alternate screen; persists the plan's project on exit. |
| `TuiKeyAction.java` | Adds `CLOSE`, `EXPAND_ALL`, `WHERE`, `NON_LANE`, `TOKENS`, `PROJECT`, `COMPLETION`. |
| `TuiKeyRegistry.java` | Plan-level bindings and footer entries; plan-specific labels from `operator.plan.keys.*`; activity and completion levels. |
| `TuiLevel.java` | Adds `PLAN_ACTIVITY` and `PLAN_COMPLETION` level kinds. |
| `TuiScreen.java` / `TuiLayout.java` | Optional per-level footer; all body lines are padded to the terminal width so a shrinking tree or token panel leaves no old rows behind. |
| `TuiScreenDriver.java` | Per-level key maps (so `h` is *close* in the plan and *back* elsewhere); full redraw when the row count changes. |
| `TuiViewStack.java` | Selection by row id, last visible id, row count, and level lookup for caller checks. |
| `PmOperator.java` | `execute` is public so the full-screen view can hand an operator decision back to the console. |

### 2.3 `pm-api` message catalogues (the only `pm-api` changes)

- `messages/plan_view.{en,de}.properties`: the `operator.plan.keys.*`, header, tree, token,
  activity and completion texts of the terminal plan view.
- `messages/operator.{en,de}.properties`: the shared footer keys added by the new actions
  (`close`, `expand_all`, `where`, `not_in_plan`, `tokens`, `project`, `completion`).

### 2.4 Tests

| Test | Coverage |
|---|---|
| `PlanViewLevelTest` | Opens the current path, breadcrumb, open/close/parent, open-all, `.`, `n`, `t`, ended footer, no-epic footer, waiting node hand-off. |
| `WorkerActivityLevelTest` | Head, entries, off-track banner, control-sequence stripping, empty stream and last-row anchor. |
| `PlanCompletionLevelTest` | Outcome, reason, deliverables, decisions, tokens, sanitization. |
| `PlanViewCatalogueTest` | Every terminal plan-view key in EN and DE; all `plan-view` scenario process states against the closed `NodeState` list. |
| `PlanViewFullScreenTest` (10 screen tests) | Direct `plan <id>` open, tree keys, lane toggle, token toggle, worker activity with an injected ANSI sequence, `p`/`e` jumps, Overview open and `Esc` return, ended plan and `c`, live SSE selection anchoring, and the narrow-terminal scrollback fallback. |
| `PlanViewTranscriptTest` + `plan-view.transcript` | The scrollback `plan fix-relay --all` transcript against the `plan-view` mock scenario. |
| `NativeTranscriptIT` | The same transcript on the native binary when `-Pnative` has built it. |

---

## 3. Test & Verification Results

- `./mvnw verify -Ppre-commit`: **BUILD SUCCESS** across all eight reactor modules
  (`pm-api`, `pm-exec`, `pm-mock-runtime`, `pm-relay`, `pm-operator`, `pm-web-app`,
  `pm-web-e2e`). OpenRewrite, license headers, checkstyle, enforcer rules, Javadoc, and the full
  unit-test suite passed.
- `pm-operator` surefire summary: **741 tests, 0 failures, 0 errors, 0 skipped**.
- New tests: 30 (10 plan-level, 10 screen, 5 activity, 2 completion, 2 catalogue, 1 transcript).
- The transcript was recorded with `-Dpm.transcript.update=true` and re-run in compare mode.

> **Pre-commit workspace note:** the repository-wide OpenRewrite pass also reformats a large set
> of pre-existing `pm-api` and test sources that this plan does not own. Those generated changes
> were reverted after the green run, so the delivered diff stays within the plan's boundary
> (`pm-operator` + the four message catalogues). Re-running the gate reproduces the same
> unrelated formatting churn; no such change is part of this PR.

---

## 4. Domain Invariants

| Invariant | Status |
|---|---|
| Java 25 / modern language features | Passed (records, pattern matching, `var`, text blocks). |
| No uncatalogued user-facing strings | Passed (`LiteralTextGuardTest`; every new text is in EN and DE). |
| Bilingual catalogue in EN and DE | Passed (`PlanViewCatalogueTest` + `ConsoleTextsTest`). |
| Terminal output sanitizer boundary | Passed: every runtime text in `PlanViewLevel`, `WorkerActivityLevel`, and `PlanCompletionLevel` passes through `OutputSafety.stripControlSequences`/`TuiLayout.sanitizeCell`; the injected-ANSI screen test proves it. |
| Read-only consumption of `pm-api` and `pm-mock-runtime` | Passed: no resource or code change outside the four message catalogues and `pm-operator`. |
| `pm-web-app` and `doc/plans/README.adoc` untouched | Passed. |
| Process states are the closed contract list | Passed (`PlanViewCatalogueTest`). |
| Selection anchored by node/task id | Passed (`TuiViewStack` id selection + screen test). |
| Alternate screen and cursor restored | Passed (`VirtualScreenTerminal` alternation and cursor assertions). |

---

## 5. Behavioural Notes

- `e` opens the plan's epic as the project's *by epic* view with the epic selected. When the plan
  was opened from that same view, `e` pops back to it instead of pushing a duplicate level.
- `p` and `e` push a level; repeated `Esc` unwinds to the plan view and then to the exact caller
  (project, epic view, ended work). A plan opened by name is the root, so `Esc` leaves the view.
- A node in `waiting_for_operator` leaves the alternate screen and prints the plan's open
  questions, so `answer <id>` opens the decision without corrupting the full-screen frame.
- The token panel aggregates per-phase and per-role totals from the leaf tasks the process
  resource carries; the raw components are shown for the plan total, which is the granularity the
  current process resource reports.
- `n` hides the `not_in_plan` lane nodes; `*` opens every level below the selected node; `.`
  reopens the path to the plan's current position and returns the selection there.

---

## 6. Follow-ups for the Orchestrator (Step 8)

- Run the 4-pillar post-landing verification and move this report to
  `doc/agy/verified/cli-fullscreen-plan-view-plan-report.md`.
- Update the archive index row added to `doc/agy/implemented/README.md` with the landed PR number.
- `doc/plans/README.adoc` was intentionally not touched, per the plan's explicit boundary rule.
