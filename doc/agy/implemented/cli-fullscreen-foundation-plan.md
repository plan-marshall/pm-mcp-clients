# Implementation Plan: `cli-fullscreen-foundation` (Terminal Full-Screen TUI Framework, Screen Test Harness & Hygiene)

This implementation plan governs the foundational full-screen TUI framework in `pm-operator`, the virtual-terminal screen-test harness, cross-view design alignment, and CLI/plan hygiene remediation identified in the TUI review.

It operationalizes [`doc/meta/implement_use-cases.md`](../meta/implement_use-cases.md) under the execution discipline of [`doc/agy/runbook.md`](runbook.md).

---

## 1. Traceability & Context

| Dimension | Reference |
|---|---|
| **Use Cases** | Foundation for [`UC-PLN-4`](../../../plan-marshall-documentation/doc/use-cases/03-plans.adoc#UC-PLN-4), [`UC-PLN-1`](../../../plan-marshall-documentation/doc/use-cases/03-plans.adoc#UC-PLN-1), [`UC-PLN-5`](../../../plan-marshall-documentation/doc/use-cases/03-plans.adoc#UC-PLN-5), [`UC-PLN-6`](../../../plan-marshall-documentation/doc/use-cases/03-plans.adoc#UC-PLN-6), [`UC-PRJ-1..3`](../../../plan-marshall-documentation/doc/use-cases/04-projects.adoc), [`UC-SYS-1..3`](../../../plan-marshall-documentation/doc/use-cases/07-system.adoc), [`UC-CTL-1..5`](../../../plan-marshall-documentation/doc/use-cases/06-controlling-work.adoc) |
| **Specifications** | [`12-machine-stores.adoc#machine-config`](../../../pm-mcp-core/doc/specification/store-schemas/12-machine-stores.adoc) (`console.full_screen`, `console.notify`), [`02-operator-channel.adoc`](../../../plan-marshall-documentation/doc/specification/cli-and-security/02-operator-channel.adoc) |
| **Local Client Design** | [`doc/design/terminal-client/04-console.adoc`](../design/terminal-client/04-console.adoc), [`15-overview.adoc#full-screen-build`](../design/terminal-client/15-overview.adoc), [`README.adoc#open-points`](../design/terminal-client/README.adoc) |
| **Roadmap Packages** | Prerequisite for reopening & completing `cli-overview`, `cli-plan-view`, `cli-projects`, `cli-reports`, `cli-system`, and `cli-actions` in [`doc/plans/README.adoc`](../plans/README.adoc) |
| **Implementation Recipe** | [`doc/meta/implement_use-cases.md`](../meta/implement_use-cases.md) |
| **Operational SOP** | [`doc/agy/runbook.md`](runbook.md) |

---

## 2. Scope & Objectives

1. **Cross-View Design & Plan Index Alignment (`doc/design/terminal-client/`, `doc/plans/`)**:
   - Specify the cross-view level stack and root navigation between `projects` (Explorer) and `plans` (Overview), `Esc` return semantics, and `p`/`e`/`S`/`R`/`E` level transitions.
   - Establish a canonical per-level Key Registry table and narrow-terminal thresholds across all views (`< 60` columns fallback to tree/snapshot; `< 80` or `< 100` detail compacting; UTF-8 display-width truncation rules).
   - Specify full-screen disconnect/reconnect header states (`● connected` vs reconnecting/stopped) with full snapshot resync on reconnect, and dialogue hand-off (suspending alternate screen to run a `LineReader` dialogue and resuming cleanly).
   - Fix the 6 broken links in `doc/plans/README.adoc` and the 386 broken relative links in `doc/plans/implemented/*.adoc`, and record the full-screen status of `cli-overview`, `cli-plan-view`, `cli-projects`, `cli-reports`, and `cli-system`.
2. **JLine 4 Full-Screen TUI Engine (`de.planmarshall.operator.tui`)**:
   - `TuiScreenDriver`: JLine 4 `Display`, `BindingReader`, and `KeyMap` on the terminal's alternate screen (`enter_ca_mode` / `exit_ca_mode`), hidden cursor (`cursor_invisible` / `cursor_normal`), keypad application mode (`keypad_xmit` / `keypad_local`), and `WINCH` resize signal handling, with guaranteed restoration in a `finally` block (including on error, exception, or `INT`/`HUP`).
   - `TuiViewStack` & `TuiLevel`: stack-based level navigation (`push`, `pop`, `replaceRoot`, `jumpTo`) preserving origin for `Esc` return and supporting clean suspension/resumption when handing off to a console dialogue or verb.
   - `TuiKeyRegistry`: declarative key bindings per level with automated footer generation and a guard test ensuring no footer ever advertises an unbound key and no conflicting bindings exist within a level.
   - `TuiLayout`: common header (path + connection badge), scrollable list viewport (`▶` + inverted selection, `↑ n more` / `↓ n more` indicators), `/` substring filter bar, `?` help & row-detail overlay, and width-aware cell truncation using `OutputSafety`.
3. **Held Notices, `console.full_screen`, and State Persistence (`overview.json`)**:
   - `ConsoleNoticeBuffer`: hook in `PmConsole` that holds SSE arrival notices and suppresses mid-screen `printAbove` corruption while a full-screen view is active, emitting the coalesced `operator.overview.meanwhile` summary upon leaving.
   - `OverviewStateStore`: reads and writes `$XDG_STATE_HOME/pm-operator/overview.json` via `OperatorStateDirectory` (holding project, view mode, selected item ID, and system view window/group settings; zero plan content; strict `0600`/`0700` permissions).
   - `console.full_screen` check: honours machine configuration, `TERM=dumb`, non-interactive streams, and `< 60` column width to fall back to the scrollback tree.
4. **Virtual-Terminal Screen-Test Harness (`de.planmarshall.operator.tui.test`)**:
   - `VirtualScreenTerminal`: in-memory virtual terminal backed by JLine `ExternalTerminal` at configurable dimensions (`120x30`, `80x24`, `70x20`, `50x20`), feeding scripted key sequences, parsing `Display` output, and asserting exact screen lines, inverted selection row, and alternate-screen restoration after each key press in both JVM and native modes.
5. **CLI Drift & Defect Fixes**:
   - Fix `{time}` placeholder bug in `EntryFlow.printReportsArrivalNotice` (passes `since` where `report.arrival.line` expects `{time}`) and update `console-status.transcript`.
   - Reconcile `--depth <n>` in `PlanCommand` / `EpicCommand` with the design (`16-plan-view.adoc#tree`) and remove or wire unused `--watch` options on commands where the design does not specify them.

---

## 3. Concurrency & Isolation

This plan is the **Wave 1 Foundation** for all subsequent terminal full-screen plans (`cli-fullscreen-overview`, `cli-fullscreen-plan-view`, `cli-fullscreen-projects`, `cli-fullscreen-reports-system`, `cli-fullscreen-actions`).

- **Must start after**: the currently in-flight [`doc/agy/cli-start-plan.md`](cli-start-plan.md) and [`doc/agy/web-start-plan.md`](web-start-plan.md) have landed on `main` (because `cli-start-plan.md` and `web-start-plan.md` are actively editing `doc/plans/README.adoc` and `pm-api/src/main/resources/messages/operator.{en,de}.properties`).

### Boundary Rules
- `pm-operator`: Sole owner of new package `de.planmarshall.operator.tui.*`, `EntryFlow.java` `{time}` fix, `PmConsole.java` notice-holding hook, and `OperatorStateDirectory.java` `overview.json` path.
- `pm-api`: Shared TUI framework message keys in `messages/operator.{en,de}.properties` (edited only after `cli-start` and `web-start` land).
- `doc/design/terminal-client/` & `doc/plans/`: Cross-view TUI conventions and broken-link fixes in `doc/plans/README.adoc` and `doc/plans/implemented/*.adoc` (edited only after `cli-start` and `web-start` land).

---

## 4. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created at `/Users/oliver/git/worktrees/pm-mcp-clients-cli-fullscreen-foundation` on branch `feat/cli-fullscreen-foundation` off latest `main`.
- [x] 2. Domain skills & engineering personas loaded where applicable:
  - `persona-implementer` & `persona-module-tester` for code & unit tests
  - `persona-integration-tester` for multi-module/integration tests
  - `persona-security-expert` for trust boundaries & input validation
  - `persona-documenter` for documentation & ADRs
  - `persona-code-reviewer` for finding evaluation & adversarial refutation
- [x] 3. Incremental implementation with tests following the phased technical recipe.
- [x] 4. Final completion report written (`doc/agy/cli-fullscreen-foundation-plan-report.md`), initial plan moved to `doc/agy/implemented/cli-fullscreen-foundation-plan.md`, and package status updated in `doc/plans/README.adoc`. (Do NOT move report to `verified/` — Step 8 is reserved for orchestrator).
- [x] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit` for code/config; skipped for standalone documentation).
- [x] 6. PR opened (NEVER use `skip-bot-review` on worker PRs; reserved for orchestrator docs PRs), review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [ ] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [ ] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/cli-fullscreen-foundation-plan-report.md` by orchestrator session.

---

## 5. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Design & Link Hygiene** | `plan-marshall-persona-documenter` | `pm-documents:ref-asciidoc` | Specify level stack, key registry, narrow/disconnect/dialogue rules; fix broken links in `doc/plans/`. |
| **Phase 2: Catalogue & CLI Fixes** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-module-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:junit-core` | Common TUI message keys in EN/DE, `{time}` arrival notice fix, `--depth` / `--watch` cleanup. |
| **Phase 3: TUI Engine & Layout** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-security-expert` | `pm-dev-java:java-core`<br>`pm-dev-java:java-security` | `TuiScreenDriver`, `TuiViewStack`, `TuiKeyRegistry`, `TuiLayout`, `OutputSafety` cell rendering, `finally` restore. |
| **Phase 4: Notices & Persistence** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-security-expert` | `pm-dev-java:java-security` | `ConsoleNoticeBuffer` in `PmConsole`, `OverviewStateStore` (`overview.json`), `console.full_screen` check. |
| **Phase 5: Screen-Test Harness** | `plan-marshall-persona-module-tester`<br>`plan-marshall-persona-integration-tester` | `pm-dev-java:junit-core` | `VirtualScreenTerminal` at multiple widths, native-image alternate-screen verification test. |
| **Phase 6: Review Gate** | `plan-marshall-persona-code-reviewer` | `pm-dev-java:java-security` | Pre-commit gate, `LiteralTextGuardTest`, and 100% resolution of automated review findings. |

---

## 6. Phased Technical Recipe

### Phase 1: Traceability, Design Alignment & Plan Index Hygiene
- [x] **1.1 Cross-View Design Clarifications**: Update `doc/design/terminal-client/15-overview.adoc` and `04-console.adoc` to explicitly define:
  - Level stack navigation between Overview (`plans`), Explorer (`projects`), Plan View, Epic View, Reports (`R`), and System (`S`), and how `Esc` pops to the caller level.
  - Narrow-terminal rules across all views (`< 60` columns prints scrollback fallback; `< 80` / `< 100` omits detail lines; `?` displays full row details; single-line width truncation with `…`).
  - Header connection state on SSE disconnect/reconnect (`● connected` vs reconnecting indicator) and automatic snapshot refresh on reconnect.
  - Dialogue hand-off lifecycle: exiting alternate screen before running a `LineReader` dialogue and re-entering alternate screen (with fresh snapshot) when returning.
- [x] **1.2 Fix Broken Plan Links**:
  - Fix the 6 broken links in `doc/plans/README.adoc` (`cli-onboarding.adoc`, `cli-plan-view.adoc`, `web-plan-view.adoc`).
  - Fix the 386 broken relative links inside `doc/plans/implemented/*.adoc` caused by moving files one directory deeper.

### Phase 2: `pm-api` Common TUI Catalogue Keys & CLI Defect Fixes
- [x] **2.1 Fix `{time}` Arrival Notice Bug**:
  - Write a failing unit test in `EntryFlowTest` asserting that `printReportsArrivalNotice` formats `report.arrival.line` with no unresolved `{time}` placeholder.
  - Fix `EntryFlow.printReportsArrivalNotice` to pass `"time", since` in `Map.of("count", page.unreadCount(), "time", since)`.
  - Update `pm-operator/src/test/resources/transcript/console-status.transcript` line 3.
- [x] **2.2 CLI Flag Drift Cleanup**:
  - Align `PlanCommand` / `EpicCommand` `--depth` option with `--depth <n>` (integer depth, preserving legacy aliases if any existing test uses them) per `16-plan-view.adoc#tree`.
  - Clean up unused `--watch` fields on commands where the design does not specify `--watch`, keeping `--watch` where specified (`status`, `system`, `plan --activity`).
- [x] **2.3 Common TUI Message Keys**:
  - Ensure all shared keys in `15-overview.adoc#overview-texts` (`operator.overview.keys.*`, `operator.overview.more_above`, `operator.overview.more_below`, `operator.overview.connected`, `operator.overview.meanwhile`, `operator.overview.too_narrow`) exist in `pm-api/src/main/resources/messages/operator.{en,de}.properties` with matching ICU placeholders.
  - Add unit test assertions in `ConsoleTextsTest` / `OverviewCatalogueTest`.

### Phase 3: Full-Screen TUI Engine (`de.planmarshall.operator.tui`)
- [x] **3.1 View Model & Stack Abstractions**:
  - Implement `TuiLevel`, `TuiRow`, `TuiKeyBinding`, `TuiKeyRegistry`, and `TuiViewStack` as pure Java 25 classes/records independent of JLine terminal I/O.
  - Support `/` filter mode (`Esc` clears filter before popping level), `?` help/detail overlay toggle, and selection anchored by row ID across model refreshes.
- [x] **3.2 Layout & Width-Safe Renderer (`TuiLayout`)**:
  - Render standard 3-region screen: header (`<Breadcrumb> ... ● connected` + horizontal rule), scrollable body with `▶` and ANSI reverse-video on the selected row plus `↑ n more` / `↓ n more` scroll indicators, and footer rule + key hints from `TuiKeyRegistry`.
  - Sanitize all dynamic text through `OutputSafety.stripControlSequences` and truncate to terminal column width without splitting surrogate pairs or wide glyphs.
- [x] **3.3 JLine 4 Display Driver (`TuiScreenDriver`)**:
  - Manage alternate screen (`Capability.enter_ca_mode` / `exit_ca_mode`), cursor visibility, keypad mode, raw mode, and `Signal.WINCH` handler.
  - Single drawing loop polling `BindingReader.readBinding(keyMap, null, false)` with short timeout / dirty flag wakeup for live SSE updates; restore all terminal state in `finally` even when runtime or rendering throws an exception.

### Phase 4: Held Notices, `console.full_screen`, and `overview.json` Persistence
- [x] **4.1 Held Notices in `PmConsole`**:
  - Add `ConsoleNoticeBuffer` to `PmConsole`: while a full-screen view is active, queue incoming notices (decisions, reports) instead of calling `reader.printAbove`, and on view exit print `operator.overview.meanwhile` if any arrived.
- [x] **4.2 State Persistence (`OverviewStateStore`)**:
  - Add `overviewFile()` (`$XDG_STATE_HOME/pm-operator/overview.json`) to `OperatorStateDirectory`.
  - Implement Jackson streaming read/write for `OverviewSavedState` (`projectId`, `projectView`, `selectedId`, `systemWindow`, `systemGroupBy`, `systemProjectId`) using atomic write and `OperatorStateDirectory` permission checks.
- [x] **4.3 Full-Screen Eligibility Gate**:
  - Implement `TuiScreenDriver.canOpenFullScreen(OperatorContext, Terminal, int minColumns)` checking `context.isInteractiveTerminal()`, terminal type (`!= dumb`), `console.full_screen` setting (default `true`), `NO_COLOR` compatibility, and terminal width `>= minColumns`.

### Phase 5: Virtual-Terminal Screen-Test Harness & Verification
- [x] **5.1 `VirtualScreenTerminal` Test Harness**:
  - Implement a reusable test helper in `pm-operator/src/test/java/de/planmarshall/operator/tui/` wrapping an `ExternalTerminal` with scripted key input, resize simulation, and screen snapshot extraction (plain lines + selected/inverted row index + alternate-screen enter/exit tracking).
- [x] **5.2 Framework Unit & Screen Tests**:
  - Test `TuiViewStack` push/pop/jump and filter/help overlays.
  - Test `TuiKeyRegistry` collision detection and footer generation.
  - Test `TuiScreenDriver` on `VirtualScreenTerminal` at `120x30`, `80x24`, and `< 60` columns (asserting fallback and `too_narrow` message), resize redraw on `WINCH`, and guaranteed `exit_ca_mode` restoration when an exception is thrown mid-render.
  - Test `OverviewStateStore` round-trip and corrupt-file fallback, and `ConsoleNoticeBuffer` `meanwhile` summary formatting.
- [x] **5.3 Pre-Commit & Operational Gate**:
  - Run `./mvnw verify -Ppre-commit` and execute PR/review workflow per [`doc/agy/runbook.md`](runbook.md).
