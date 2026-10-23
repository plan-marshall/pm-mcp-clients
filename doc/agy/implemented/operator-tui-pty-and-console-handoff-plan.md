# Implementation Plan: `operator-tui-pty-and-console-handoff` (Prove the TUI on a Real Terminal, Then Fix the Console Hand-Off)

The full-screen terminal views exist, but the way the interactive console reaches them is broken. Running `pm-operator plans` directly on a pseudo-terminal opens the alternate screen and draws the overview. Typing `plans` (or `tui`) inside `pm-operator` does not: the console prints the large status tree instead. That is the reported “garbled (too much data) CLI”, and it reproduces independently of VS Code.

The cause is a second JLine terminal. `PmConsole.dispatchCommand` hands `plans` to a nested `PmOperator.execute`, which reaches `PlansCommand`; `PlansCommand` calls `context.createTerminal()` again. JLine's first system terminal owns the process terminal; the second `TerminalBuilder` falls back to `DumbTerminal` with type `dumb` and `0 × 0` columns. `TuiScreenDriver.canOpenFullScreen` then rejects it, and `PlansCommand` falls back to the status tree. The console's own hint is printed from the *first* terminal, so it promises a view the hand-off cannot open.

At the same time, the TUI has no end-to-end test on a real terminal. `VirtualScreenTerminal` is an in-memory JLine terminal with scripted input; `PseudoTerminal` is a real `script(1)` PTY but is non-interactive, pinned to 80×24, and used only for the line-oriented transcripts. Neither launches the full-screen app as a process, sends keys mid-run, and asserts the rendered screen. That is the missing structure this plan adds first, following the way other TUI projects (OpenCode among them) test: drive a real pseudo-terminal, inject keys, parse the ANSI screen, and assert the visible result and the restoration on exit.

**This is not a wrong-terminal problem.** `TERM=xterm-256color` with `TERM_PROGRAM=vscode` is supported, and direct `plans` works at 80×24 and 120×30. The defect is the console hand-off creating a second, dumb terminal.

---

## 1. Traceability & Context

| Dimension | Reference |
|---|---|
| **Use Cases** | [`UC-PLN-1`](../../../../plan-marshall-documentation/doc/use-cases/03-plans.adoc#UC-PLN-1) (Follow a Plan Down to Its Parts), [`UC-PLN-4`](../../../../plan-marshall-documentation/doc/use-cases/03-plans.adoc#UC-PLN-4) (See What Runs, Across Projects), [`UC-ENT-1`](../../../../plan-marshall-documentation/doc/use-cases/02-entry.adoc#UC-ENT-1) (Enter and Connect) |
| **Requirements** | `PM-SEC-3` (interactive-terminal gate), `PM-TEST-7` (transcript/PTY coverage), `PM-SEC-7` (dependable views) |
| **Console hand-off** | [`PmConsole.java`](../../../pm-operator/src/main/java/de/planmarshall/operator/PmConsole.java) — `dispatchCommand`, `runOperatorVerb`, `printFullScreenHint`, `enterFullScreen` |
| **Full-screen entry** | [`PlansCommand.java`](../../../pm-operator/src/main/java/de/planmarshall/operator/PlansCommand.java), [`PlanCommand.java`](../../../pm-operator/src/main/java/de/planmarshall/operator/PlanCommand.java), [`ProjectsCommand.java`](../../../pm-operator/src/main/java/de/planmarshall/operator/ProjectsCommand.java), [`SystemCommand.java`](../../../pm-operator/src/main/java/de/planmarshall/operator/SystemCommand.java), [`PmOperator.java`](../../../pm-operator/src/main/java/de/planmarshall/operator/PmOperator.java) (`--tui`) |
| **Terminal ownership** | [`OperatorContext.java`](../../../pm-operator/src/main/java/de/planmarshall/operator/OperatorContext.java) — `createTerminal()`, `defaultTerminal(...)` |
| **Capability gate** | [`TuiScreenDriver.java`](../../../pm-operator/src/main/java/de/planmarshall/operator/tui/TuiScreenDriver.java) — `canOpenFullScreen`, `fullScreenConfigured` |
| **View application** | [`OverviewFullScreenApp.java`](../../../pm-operator/src/main/java/de/planmarshall/operator/overview/OverviewFullScreenApp.java) |
| **In-memory harness** | [`VirtualScreenTerminal.java`](../../../pm-operator/src/test/java/de/planmarshall/operator/tui/VirtualScreenTerminal.java) |
| **PTY helper today** | [`PseudoTerminal.java`](../../../pm-operator/src/test/java/de/planmarshall/operator/transcript/PseudoTerminal.java) — `script(1)`, fixed 80×24, read-all |
| **Existing ITs** | [`PmOperatorProcessIT.java`](../../../pm-operator/src/test/java/de/planmarshall/operator/PmOperatorProcessIT.java), [`NativeTranscriptIT.java`](../../../pm-operator/src/test/java/de/planmarshall/operator/transcript/NativeTranscriptIT.java), [`TranscriptDialogueIT.java`](../../../pm-operator/src/test/java/de/planmarshall/operator/transcript/TranscriptDialogueIT.java) |
| **Terminal design** | [`15-overview.adoc`](../../design/terminal-client/15-overview.adoc), [`04-console.adoc`](../../design/terminal-client/04-console.adoc) |
| **Catalogue** | `operator.{en,de}.properties`, `overview.{en,de}.properties` under [`pm-api/src/main/resources/messages`](../../../pm-api/src/main/resources/messages) |
| **Operational SOP** | [`runbook.md`](../runbook.md) |

### 1.1 Reproduction and Finding Analysis

Reproduced on `main` against `pm-mock-runtime` with `fully-onboarded`, under a pseudo-terminal with a real window size:

| Probe | Result |
|---|---|
| `pm-operator plans`, 120×30, `TERM=xterm-256color` | Alternate screen entered (`ESC[?1049h`), header, rows, footer drawn; `q` exits and restores the screen. |
| `pm-operator` → `plans`, same terminal | **No alternate screen.** 1,667 bytes of console output, ending in the status tree and a new `pm (reconnecting) ›` prompt. |
| Second `TerminalBuilder.builder().system(true).ffm(true).dumb(true).build()` | `type=dumb cols=0 rows=0 class=org.jline.terminal.impl.DumbTerminal`; the first is `xterm-256color 120×30 class=FfmUnixSysTerminal`. |

| # | Finding | Root cause | Correction |
|---|---|---|---|
| **F1** | Typing `plans` or `tui` in the console prints the status tree instead of the TUI. | `PmConsole` dispatches the verb through a nested `PmOperator.execute`; `PlansCommand` opens a **second** JLine terminal, which is dumb and 0 columns, so `canOpenFullScreen` is false. | Reuse the console's terminal for the whole full-screen dispatch; never create a second terminal while the console is active. |
| **F2** | The greeting promises “Type plans or tui to open the full-screen overview”, but the hand-off cannot open it. | `printFullScreenHint` tests the console's first terminal; `PlansCommand` tests its second one. | One shared availability verdict, computed from the terminal that will actually draw. |
| **F3** | The TUI has no process-level, interactive test. | `VirtualScreenTerminal` is in-memory; `PseudoTerminal` is non-interactive and fixed-size, used only for line transcripts. | Add a real-PTY session harness with key injection, screen parsing, and exit/restoration assertions. |
| **F4** | Notices can interleave with the full-screen view. | `PmConsole.enterFullScreen()`/`leaveFullScreen()` exist but no caller uses them. | Bracket the full-screen verbs in the console with the notice buffer. |
| **F5** | A fallback is silent, so the operator cannot tell a configuration/width refusal from a broken view. | `PlansCommand` delegates to `status` without a reason. | Print one catalogue line naming the reason (not for `--json`), and keep the fallback behavior. |
| **F6** | The reported environment could be blamed incorrectly. | VS Code sets `TERM_PROGRAM=vscode` but a normal `xterm-256color`; the code does not branch on `TERM_PROGRAM`, and direct `plans` works there. | Add a VS Code-shaped PTY case to prove it; fix F1, not the terminal. |

---

## 2. Scope & Objectives

### Part A: TUI Capability Test Infrastructure

1. Extract the ANSI screen parser from `VirtualScreenTerminal` into a reusable test-support type (`AnsiScreen`), so both the in-memory terminal and the real-PTY session parse the same way.
2. Add `PtySession` (test scope only):
   - starts a real process on a pseudo-terminal via the existing `script(1)` approach, with a configurable `columns × rows` and `TERM`;
   - keeps stdin open for **mid-run key injection** (`send("plans\n")`, `send("q")`);
   - runs a reader thread that accumulates raw output;
   - exposes `awaitText(...)` with a timeout, `screen()`, `rawOutput()`, `alternateScreenEntries/Exits`, `cursorVisible()`, `exitCode()`, and `awaitExit(...)`;
   - cleans up the child and the PTY in `close()`.
3. Add `TuiCapabilityIT` (`@EnabledOnOs(MAC, LINUX)`, IT profile) covering:
   - direct `plans` at 120×30 and 80×24;
   - `--tui`;
   - `plan fix-relay`, `projects`, `system`;
   - console `plans` and console `tui` (the F1 regression);
   - `plans --json` (no alternate screen);
   - `TERM=dumb` and 50×20 (documented fallbacks);
   - `console.full_screen=false` (fallback);
   - VS Code-shaped env (`TERM_PROGRAM=vscode`, `TERM=xterm-256color`, 80×24);
   - restoration after `q` (alternate screen exited, cursor visible, prompt returned, `exit` succeeds);
   - native binary when `target/pm-operator` is built (otherwise the JVM path; record the mode).
4. Keep the existing in-memory tests for layout, scrolling, resize, and renderer exceptions. The new harness adds the process-level proof; it does not replace them.

### Part B: Fix the Console Hand-Off

5. `OperatorContext`:
   - a scoped terminal (set only around full-screen console dispatch);
   - `createTerminal()` returns the scoped terminal when set, otherwise the provider's terminal;
   - `ownsTerminal(Terminal)` distinguishes an owned terminal from a borrowed one.
6. `PmConsole`:
   - keep the console's `Terminal` in scope;
   - for the full-screen verbs (`plans`, `tui`, `plan`, `projects`, `system`), set the scoped terminal, run the verb, then clear it in `finally`;
   - call `enterFullScreen()` before and `leaveFullScreen(out)` after, so notices are held and summarised;
   - do not hold notices for `--json` or for a verb that cannot open full screen;
   - keep `tui` as the alias for `plans`.
7. Full-screen entry points (`PlansCommand`, `PlanCommand`, `ProjectsCommand`, `SystemCommand`, `PmOperator.runFullScreen`):
   - close the terminal only when the context owns it;
   - never close the console's terminal.
8. Availability and diagnostics:
   - add a small `FullScreenAvailability` result (available + reason: non-interactive, dumb, configured-off, too narrow);
   - share it between `canOpenFullScreen` and the fallback message;
   - when a requested `plans`/`plan`/`projects`/`system` cannot open, print one catalogue line naming the reason to stderr, unless `--json`;
   - keep the existing status/tree fallback as the output.
9. Prompt restoration:
   - after the full-screen app returns, the console redraws its prompt and remains usable; assert it in the PTY test. If the test shows a stale line, flush/redraw the `LineReader` explicitly.
10. Documentation:
   - update `15-overview.adoc` with the console hand-off and the availability reasons;
   - update `04-console.adoc` with the hand-off lifecycle (already partly described) and the notice hold;
   - update `pm-operator/README.md` with the reproducible manual check (`pm-mock-runtime`, then `pm-operator`, then `plans`/`tui`);
   - add `27-tui-testing.adoc` describing the PTY harness and what each layer tests.

### Non-Goals

- No redesign of the TUI screens, keys, levels, or layout.
- No change to the TUI renderer/Display logic unless the new tests expose a concrete defect.
- No Windows PTY support; the operator remains POSIX-only.
- No change to the `PM-SEC-3` gate or the coding-agent refusal.
- No new production dependency; the harness stays test-scope and uses the OS `script(1)`.
- No change to the default `pm-operator` invocation beyond making the documented `plans`/`tui` hand-off work and its hint truthful.

---

## 3. Isolation & Boundaries

| Repository | Worktree | Branch | Scope |
|---|---|---|---|
| `pm-mcp-clients` | `/Users/oliver/git/worktrees/pm-mcp-clients-operator-tui-pty-handoff` | `fix/operator-tui-pty-handoff` | `pm-operator` main + test, `pm-api` catalogue keys, terminal design docs, README, report |

> **Pre-flight**: `git checkout main && git pull --rebase origin main` before creating the worktree.

**Sole-owner files**: `PmConsole.java`, `OperatorContext.java`, `PlansCommand.java`, `PlanCommand.java`, `ProjectsCommand.java`, `SystemCommand.java`, `PmOperator.java`, `TuiScreenDriver.java` (availability only), the new `PtySession`/`AnsiScreen`/`TuiCapabilityIT`, `VirtualScreenTerminal.java` (extraction only), `operator.{en,de}.properties`, `overview.{en,de}.properties`, `15-overview.adoc`, `04-console.adoc`, `pm-operator/README.md`, `27-tui-testing.adoc` (planned as 16; `16-plan-view.adoc` owns that number), and the report.

**No touch**: TUI content levels, the web app, `pm-mcp-server`, the runtime/mock scenario data, the operator gate, and the transcript files (unless a new diagnostic line changes one deliberately; prefer not to).

---

## 4. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created at `/Users/oliver/git/worktrees/pm-mcp-clients-operator-tui-pty-handoff` on branch `fix/operator-tui-pty-handoff` off latest `main`.
- [x] 2. Domain skills & engineering personas loaded where applicable:
  - `plan-marshall-persona-implementer` & `pm-dev-java:java-core` for the terminal ownership and console hand-off;
  - `plan-marshall-persona-module-tester` & `pm-dev-java:junit-core` for the unit-level ownership tests;
  - `plan-marshall-persona-integration-tester` & `pm-dev-java:junit-integration` for the PTY capability suite;
  - `plan-marshall-persona-security-expert` for the `PM-SEC-3` gate and terminal input/output boundaries;
  - `plan-marshall-persona-documenter` for the terminal design and README;
  - `plan-marshall-persona-code-reviewer` for review triage.
- [x] 3. Incremental implementation with tests following the phased technical recipe; the PTY regression is written **before** the fix and must fail first.
- [x] 4. Completion report written (`doc/agy/operator-tui-pty-and-console-handoff-plan-report.md`), plan moved to `doc/agy/implemented/`, links rebased, dashboard updated.
- [x] 5. Pre-commit passed: `./mvnw verify -Ppre-commit`; the PTY suite under `./mvnw -pl pm-operator -Pintegration-tests verify`.
- [ ] 6. PR opened (never `skip-bot-review` on worker PRs); review windows and all bot/Sonar findings resolved.
- [ ] 7. Enqueued in the merge queue, landed on `main`, worktree and branch cleaned up.
- [ ] 8. Post-landing verification performed and the report moved to `doc/agy/verified/` by the orchestrator.

> [!IMPORTANT]
> **Plan archival link rule**: this plan is written at `doc/agy/`; its links use `../design`, `../../pm-operator`, `../../pm-api`, `../../../../plan-marshall-documentation`, and `<name>.md` after the move. When Step 4 moves it, rebase every link and re-run the internal-link check.

---

## 5. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: PTY harness** | `plan-marshall-persona-integration-tester` | `pm-dev-java:junit-integration` | Real PTY, key injection, screen parsing, timeouts, restoration. |
| **Phase 2: Failing regression** | `plan-marshall-persona-module-tester` | `pm-dev-java:junit-core` | Console `plans`/`tui` reproduces F1 before the fix. |
| **Phase 3: Ownership fix** | `plan-marshall-persona-implementer` | `pm-dev-java:java-core` | Scoped terminal, borrowed-vs-owned close, no second terminal. |
| **Phase 4: Notices/diagnostics** | `plan-marshall-persona-implementer` | `pm-dev-java:java-core`, `plan-marshall-persona-security-expert` | Notice hold, availability reasons, truthful hint. |
| **Phase 5: Verification** | `plan-marshall-persona-integration-tester` | `pm-dev-java:junit-integration`, `pm-dev-java:junit-core` | Matrix across verbs, sizes, TERM, VS Code shape, native. |
| **Phase 6: Docs & review** | `plan-marshall-persona-documenter` | `plan-marshall-persona-code-reviewer` | Design, README, harness note, report, triage. |

---

## 6. Phased Technical Recipe

### Phase 1: Reusable PTY Harness and Screen Parser (test scope)

- [x] **1.1 Extract `AnsiScreen`**
  - Move the ANSI parser out of `VirtualScreenTerminal.Screen` into `de.planmarshall.operator.tui.AnsiScreen` (or `transcript.AnsiScreen`), keeping the same supported subset: cursor addressing/movement, erase, SGR inverse, alternate-screen modes 1049/47/1047, cursor mode 25, OSC, string introducers.
  - `VirtualScreenTerminal` delegates to it; its public assertions (`lines()`, inverted rows, alternate counts, cursor visibility) stay unchanged.
  - Add parser tests for the sequences the app emits, wrapping, and wide glyphs if not already covered.
- [x] **1.2 Add `PtySession`** under `pm-operator/src/test/java/de/planmarshall/operator/transcript/`:
  - `static PtySession start(List<String> command, Map<String,String> env, Path cwd, int columns, int rows)`.
  - On macOS: `/usr/bin/script -q /dev/null /bin/sh -c "<stty rows R cols C; exec …>"`.
  - On Linux: `/usr/bin/script -q -e -c "<stty …; exec …>" /dev/null`.
  - Pin `TERM` and the environment; clear inherited agent signals as the existing `PseudoTerminal` does.
  - Keep stdin open; a writer thread or a synchronized `send(String)` writes keys and flushes.
  - A reader thread accumulates output into a thread-safe buffer.
  - `awaitText(String, Duration)` polls the parsed screen; never use a bare fixed sleep for the main assertion.
  - `screen()` parses the accumulated bytes at the configured size.
  - `awaitExit(Duration)` and `exitCode()`.
  - `close()` destroys the child and closes the PTY streams; idempotent.
  - `Assumptions.assumeTrue(Files.isExecutable(Path.of("/usr/bin/script")))` at the test boundary.
- [x] **1.3 Prove the harness on a known-good case**
  - `pm-operator plans` at 120×30 must show the alternate screen and the “Running work · projects” header.
  - This test passes before the fix and validates the harness itself.

### Phase 2: The Failing Regression (F1)

- [x] **2.1 Console hand-off test**
  - Start `pm-operator` with **no arguments** on a 120×30 PTY.
  - `awaitText("pm ›")` (the console prompt).
  - `send("plans\n")`.
  - `awaitText("Running work")` and assert `alternateScreenEntries() == 1`.
  - `send("q")`; `awaitText("pm ›")`; assert the alternate screen was exited and the cursor is visible.
  - `send("exit\n")`; assert exit code 0.
  - Before the fix this fails at `awaitText("Running work")` because `PlansCommand` fell back to the status tree. Record the captured screen in the report as the reproduction.
- [x] **2.2 Same for `tui`**
  - Assert the alias opens the same view.
- [x] **2.3 Keep the failing test in the suite** and make it green in Phase 3; do not delete it after the fix.

### Phase 3: Terminal Ownership and Console Hand-Off

- [x] **3.1 `OperatorContext`**
  - Add a scoped terminal field and:
    - `public void useTerminal(Terminal terminal)`;
    - `public void clearTerminal()`;
    - `public boolean ownsTerminal(Terminal terminal)` — `false` for the scoped terminal, `true` otherwise.
  - `createTerminal()` returns the scoped terminal when set, otherwise `terminalProvider.createTerminal()`.
  - Keep the provider path unchanged for every direct CLI run.
  - Unit tests: scoped returns the same instance; `ownsTerminal` is false for it and true for a provider terminal; clearing restores the provider.
- [x] **3.2 `PmConsole`**
  - Keep the console's `Terminal` in a local (already created in `run()`).
  - Replace the `plans`/`tui` special case with a `runFullScreenVerb(line, out, reader, terminal)`:
    - parse the verb and decide whether it is a full-screen verb (`plans`, `tui`, `plan`, `projects`, `system`);
    - if the line contains `--json`, run it normally;
    - otherwise `context.useTerminal(terminal)`, `enterFullScreen()`, run `runOperatorVerb`, `leaveFullScreen(out)`, `context.clearTerminal()` in a `finally`;
    - after return, flush and let the REPL’s next `readLine` redraw the prompt.
  - Keep `tui` rewriting to `plans` plus its trailing arguments.
  - Only print the full-screen hint when the **same** availability check says the hand-off can open; otherwise print nothing (or a single “not available here” line if the design calls for it).
- [x] **3.3 Full-screen entry points**
  - In `PlansCommand`, `PlanCommand`, `ProjectsCommand`, `SystemCommand`, and `PmOperator.runFullScreen`, replace the unconditional close with:
    ```java
    var terminal = context.createTerminal();
    try { … } finally { if (context.ownsTerminal(terminal)) { terminal.close(); } }
    ```
  - Do not change the actual view construction or the `canOpenFullScreen` call, except to use the shared availability result.
  - Audit any other command that opens `OverviewFullScreenApp`; apply the same ownership rule.
- [x] **3.4 Test the ownership directly**
  - A fake `TerminalProvider` that returns a `VirtualScreenTerminal` and counts creations:
    - console dispatch of `plans` creates exactly one terminal (the console's), not two;
    - the terminal is not closed by the nested verb;
    - a direct `PlansCommand` run still closes its own terminal.
  - A `PmConsole` unit test can assert the scoped terminal is set during dispatch and cleared afterwards.

### Phase 4: Notice Buffer, Diagnostics, and Prompt Restoration

- [x] **4.1 Notice hold**
  - The full-screen bracket calls `enterFullScreen()`/`leaveFullScreen(out)`.
  - Add a test: an event that arrives while the view is open is not printed into the alternate screen; after `q`, the console prints one coalesced meanwhile line (reuse `ConsoleNoticeBufferTest` patterns).
- [x] **4.2 Availability reasons**
  - Add a small `FullScreenAvailability` (`available`, `reason` enum: `NON_INTERACTIVE`, `DUMB_TERM`, `CONFIGURED_OFF`, `TOO_NARROW`).
  - `TuiScreenDriver.availability(context, terminal, minColumns)` computes it; `canOpenFullScreen` delegates to it.
  - `PlansCommand`/`PlanCommand`/`ProjectsCommand`/`SystemCommand` print the matching catalogue line to stderr when they cannot open and `--json` is absent.
  - Catalogue keys, English and German:
    - `operator.tui.unavailable.non_interactive=…`
    - `operator.tui.unavailable.dumb_terminal=…`
    - `operator.tui.unavailable.configured_off=…`
    - `operator.tui.unavailable.too_narrow=…`
  - Do not print a reason in `--json` mode; keep stdout clean.
  - Keep the status/tree fallback as the visible output.
- [x] **4.3 Prompt restoration**
  - The PTY test asserts the prompt returns after `q` and that `exit` works.
  - If a stale line or a missing prompt appears, fix it in `PmConsole` (flush the writer and request a line redraw) rather than in the view.
- [x] **4.4 VS Code-shaped case**
  - Add a PTY test with `TERM_PROGRAM=vscode`, `TERM=xterm-256color`, 80×24; assert the TUI opens and `q` restores. This records that the reported environment is supported.

### Phase 5: Full Test Matrix

- [x] **5.1 `TuiCapabilityIT`** (IT profile, macOS/Linux):
  - `plans` 120×30, 80×24 → alternate screen, header, rows, footer, exit 0.
  - `--tui` → same.
  - `plan fix-relay` → plan view (title/task rows), `q` restores.
  - `projects` and `system` → their root levels.
  - console `plans` and console `tui` → the F1 regression, prompt restored.
  - `plans --json` → JSON on stdout, no alternate screen.
  - 50×20 → too-narrow text, no alternate screen, exit 0.
  - `TERM=dumb` → no alternate screen, reason line, status fallback.
  - `console.full_screen=false` (write a minimal `machine-config.json` into the fixture base) → no alternate screen, reason line.
  - `TERM_PROGRAM=vscode` 80×24 → TUI opens.
  - Native mode when built; record `native` vs `jvm` in the verification result.
- [x] **5.2 Unit tests**
  - `OperatorContext` scoped/owned terminal.
  - `TuiScreenDriver.availability` for every reason (mirrors the existing `canOpenFullScreen` tests).
  - `PmConsole` dispatch uses one terminal and clears the scope.
  - `PlansCommand` does not close a borrowed terminal.
- [x] **5.3 Flake control**
  - Use `awaitText` with generous timeouts (e.g., 15 s) and a short settle only after a condition is observed.
  - Capture raw output on failure and include it in the assertion message.
  - Close every process/stream in `finally`; never leave a child on the test machine.

### Phase 6: Documentation, Verification, Report

- [x] **6.1 `15-overview.adoc`**: the console hand-off, one terminal per process, availability reasons, and the notice hold.
- [x] **6.2 `04-console.adoc`**: the `plans`/`tui` lifecycle (prompt → alternate screen → prompt) and what is restored.
- [x] **6.3 `27-tui-testing.adoc`** (planned as `16-tui-testing.adoc`; the number was taken): the three layers — pure model/layout tests, `VirtualScreenTerminal` in-memory rendering, `PtySession` process-level capability — and the screen-snapshot approach borrowed from other TUIs (OpenCode drives a real PTY and asserts rendered state).
- [x] **6.4 `pm-operator/README.md`**: a copy-paste manual check:
  - start `pm-mock-runtime` with `fully-onboarded`;
  - run `./pm-operator/start.sh`;
  - type `plans` or `tui`;
  - `q` returns to the prompt;
  - note the VS Code integrated terminal is supported (a real TTY), while the “Run” button is not.
- [x] **6.5 Verification**
  - `./mvnw -pl pm-operator -am test`;
  - `./mvnw -pl pm-operator -Pintegration-tests verify` (the PTY suite; report both JVM and native modes);
  - `./mvnw verify -Ppre-commit`;
  - manual PTY run if useful, with the captured screen in the report.
- [x] **6.6 Report**: the failing-before/passing-after screen for console `plans`, the second-terminal proof (`type=dumb`, `0×0`), the matrix, the availability reasons, and the restoration evidence.

---

## 7. Acceptance Criteria

| Finding | Acceptance criterion | Evidence |
|---|---|---|
| **F1** | Typing `plans` or `tui` in the console opens the full-screen overview on a terminal that supports it. | `TuiCapabilityIT` console tests; failing-before/passing-after capture. |
| **F1** | Exactly one terminal is created per process; the console's terminal is reused and never closed by the nested verb. | `OperatorContext`/`PmConsole` unit tests and a terminal-provider call count. |
| **F2** | The hint is printed only when the same terminal that will draw can open the view. | Unit test with available/unavailable verdicts. |
| **F3** | A real-PTY harness can start the process, inject keys mid-run, parse the screen, and assert exit/restoration. | `PtySession` + `TuiCapabilityIT`. |
| **F4** | Notices that arrive during a full-screen view are held and summarised after `q`; they never corrupt the alternate screen. | Notice-buffer integration test and PTY assertion. |
| **F5** | A refused full-screen request prints one catalogue line naming the reason, except under `--json`. | Availability tests for dumb, narrow, config-off, non-interactive. |
| **F6** | The VS Code-shaped environment (`TERM_PROGRAM=vscode`, `xterm-256color`, 80×24) opens the TUI. | PTY case in `TuiCapabilityIT`. |
| **—** | Direct `plans`, `--tui`, `plan`, `projects`, and `system` keep their behavior; narrow/dumb/config fallbacks still produce the status/tree output and exit 0. | Full matrix. |
| **—** | `q` restores the alternate screen, cursor, and console prompt; `exit` then exits 0. | PTY restoration assertions. |
| **—** | `./mvnw verify -Ppre-commit` and the IT profile pass; the native run is recorded when the binary exists. | CI / report. |

---

## 8. Risk & Rollback Notes

- **Terminal lifetime**: the central risk is closing the console's terminal or leaking a borrowed one. The ownership predicate and the one-terminal-created test are the guard; every `finally` must consult it.
- **JLine reader state**: the console's `LineReader` and the full-screen driver share one terminal reader. `TuiScreenDriver` restores raw attributes in `finally`; the console must flush and redraw its prompt. The PTY test asserts the prompt, not just the screen content.
- **PTY flakiness**: `script(1)` behavior differs between macOS and util-linux. Reuse the existing detection, pin the size with `stty`, use condition-based waits, and skip with an assumption if `script` is missing. Do not add a native PTY dependency.
- **Notice timing**: an event can arrive between `enterFullScreen()` and the view's first draw. The buffer is designed for this; assert the after-`q` summary rather than exact mid-screen bytes.
- **Transcripts**: adding a diagnostic line could break a recorded transcript. Prefer printing the reason only when a full-screen verb is requested and refused, not in the ordinary status path; if a transcript changes, update it deliberately.
- **Native**: no production change beyond the shared code; run the PTY smoke on the native binary when built and record the mode if it is not.
- **Rollback**: remove the scoped terminal and ownership checks, restoring the second-terminal behavior; the PTY harness and the failing test stay as the regression record. The view code is untouched.
