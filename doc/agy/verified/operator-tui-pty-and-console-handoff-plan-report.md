# Plan Completion Report: `operator-tui-pty-and-console-handoff`

**Step 8 status:** **PASSED** — four-pillar audit, 58 ITs, 13 PTY cases, and the pre-commit gate complete.

**Plan**: [`operator-tui-pty-and-console-handoff-plan.md`](../implemented/operator-tui-pty-and-console-handoff-plan.md)
**Branch**: `fix/operator-tui-pty-handoff`
**Worktree**: `/Users/oliver/git/worktrees/pm-mcp-clients-operator-tui-pty-handoff`
**Date**: 2026-10-23
**Runbook**: [`runbook.md`](../runbook.md)

---

## 1. Executive Summary

The full-screen terminal views existed, but the interactive console could not reach them. Typing
`plans` or `tui` at the `pm ›` prompt printed the large status tree instead of the alternate-screen
overview. The root cause was a second JLine terminal: `PmConsole` dispatched the verb through a
nested `PmOperator.execute`; `PlansCommand` called `context.createTerminal()` again, JLine's second
system terminal fell back to a `DumbTerminal` with `type=dumb` and `0 × 0`, and
`TuiScreenDriver.canOpenFullScreen` correctly refused it.

The change delivers the missing process-level proof first, then fixes the hand-off:

1. A reusable ANSI screen parser (`AnsiScreen`) now serves the in-memory terminal and a new
   real-PTY session (`PtySession`).
2. `TuiCapabilityIT` starts `pm-operator` (JVM or native) on a `script(1)` pseudo-terminal, injects
   keys mid-run, parses the rendered screen, and asserts the view, the restoration, and every
   documented fallback. The console `plans`/`tui` case was written before the fix and failed first.
3. The console now lends its own terminal to a full-screen dispatch (`OperatorContext.useTerminal`,
   `ownsTerminal`), never creates a second terminal, holds notices while the view is open, and
   closes only a terminal the context owns.
4. One `FullScreenAvailability` verdict (non-interactive, dumb terminal, configured off, too narrow)
   now drives the greeting's hint, the hand-off, and a single reason line on stderr.
5. The design documents and the README describe the hand-off, the reasons, and the three test
   layers.

All acceptance criteria are met. The pre-commit reactor build is green, the unit suite is green, and
the integration-test profile is green (native tests skip because no native binary was built).

---

## 2. Root Causes and Corrections

| # | Finding | Root cause | Correction | Evidence |
|---|---|---|---|---|
| **F1** | Console `plans`/`tui` prints the status tree | Second JLine terminal is dumb (`0 × 0`), so the view refuses | Console scopes its terminal for the dispatch; nested verb reuses it; entry points close only owned terminals | `TuiCapabilityIT.consolePlansHandsOff`, `consoleTuiHandsOff`; `PmConsoleHandoffTest` |
| **F2** | Hint promises a view the hand-off cannot open | Hint and command tested different terminals | Both use `TuiScreenDriver.availability(...)` on the console's terminal | `PmConsole.printFullScreenHint`; unit tests |
| **F3** | No process-level interactive test | Only in-memory terminal and non-interactive `script` helper | `PtySession` + `TuiCapabilityIT` (13 cases) | IT report |
| **F4** | Notices could interleave with the view | `enterFullScreen`/`leaveFullScreen` existed but no caller | Console brackets a full-screen dispatch; `PmConsoleHandoffTest` holds a report and asserts the meanwhile line | `PmConsoleHandoffTest.consolePlansUsesTheConsoleTerminal` |
| **F5** | A refusal was silent | `PlansCommand` delegated to `status` with no reason | `FullScreenAvailability` + `operator.tui.unavailable.*` line on stderr (not for `--json`) | `FullScreenAvailabilityTest`; PTY fallback cases |
| **F6** | The environment could be blamed | The code does not branch on `TERM_PROGRAM`; direct `plans` works in VS Code | Added the VS Code-shaped PTY case | `TuiCapabilityIT.vscodeShapedTerminalOpens` |

### 2.1 Failing Before the Fix (captured)

The console test failed at `awaitText("Running work")`; the captured screen was the status tree and a
new prompt. Excerpt:

```text
Type plans or tui to open the full-screen overview.
=pm › plans
Runtime pm-mcpd 0.9.1, running since today 08:14 · 5 workers · 1 of 2 build slots in use

! 2  pm-mcp-clients · 3 plans · 1 epic
                plan  Fix Relay Transport              execute · running · 167 h 3 min
                            in epic Relay Hardening · 2 of 3 tasks · off track: repeating
                            pull request #214 https://github.com/plan-marshall/pm-mcp-clients/pull/214
     ! decision plan  Drop the legacy endpoints        finalize · waiting for you · 167 h 11 min
...
=pm ›
```

No `ESC[?1049h` was written: `alternateScreenEntries() == 0`.

### 2.2 Passing After the Fix (captured on the same PTY case)

```text
Running work · projects                                                                                     ● connected
───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
▶ ! 2  pm-mcp-clients   3 plans · 1 epic                     6 days ago
  ! 1  plan-marshall    setup needs you                      7 days ago
       pm-mcp-core      2 plans                              6 days ago
───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
↑↓ move   Enter open   E ended work   R 5 reports   / filter   ? help   q leave
```

The test then asserts `alternateScreenEntries() == 1`, sends `q`, waits for `pm ›` again, asserts
the alternate screen was exited and the cursor is visible, sends `exit`, and asserts exit code `0`.

### 2.3 The Second Terminal (the defect's signature)

The finding analysis recorded the second builder's result:

```text
type=dumb cols=0 rows=0 class=org.jline.terminal.impl.DumbTerminal
```

The first terminal was `xterm-256color`, `120 × 30`, `class=FfmUnixSysTerminal`. The new guards are
the ownership predicate (`OperatorContext.ownsTerminal`) and a terminal-provider call count:
`PmConsoleHandoffTest` asserts exactly one creation for console `plans`, and that a direct `plans`
still closes the one it created.

---

## 3. Delivered Artifacts

### 3.1 Production (`pm-operator`)

| File | Change |
|---|---|
| `OperatorContext.java` | Scoped terminal (`useTerminal`, `clearTerminal`), `createTerminal` prefers the scope, `ownsTerminal(Terminal)` distinguishes borrowed from owned |
| `PmConsole.java` | Keeps the console terminal, rewrites `tui` to `plans`, detects the full-screen verbs (`plans`, `plan`, `projects`, `system`), scopes the terminal, brackets the notice buffer (not for `--json` or an unavailable view), flushes and returns to the prompt, uses the shared availability verdict for the hint |
| `PlansCommand.java` | Shared availability at 60 columns, reason line on stderr, ownership-guarded close |
| `PlanCommand.java` | Reason line for the requested plan view; ownership-guarded close |
| `ProjectsCommand.java` | Reason line; ownership-guarded close |
| `SystemCommand.java` | Reason line replaces the system-specific too-narrow text; ownership-guarded close |
| `PmOperator.java` | `--tui` uses the shared verdict and closes only an owned terminal |
| `tui/TuiScreenDriver.java` | `availability(context, terminal, minColumns)`, `canOpenFullScreen` delegates, `printUnavailable(...)` |
| `tui/FullScreenAvailability.java` | New record: `available`, nullable `reason` (`NON_INTERACTIVE`, `DUMB_TERMINAL`, `CONFIGURED_OFF`, `TOO_NARROW`), catalogue `messageKey()` |
| `overview/OverviewFullScreenApp.java` | The confirmation closes a terminal only when both the app created it and the context owns it |

### 3.2 Catalogue (`pm-api`)

| File | Change |
|---|---|
| `messages/operator.en.properties` | `operator.tui.unavailable.{non_interactive,dumb_terminal,configured_off,too_narrow}` |
| `messages/operator.de.properties` | The same four keys in German |

### 3.3 Tests (`pm-operator`, test scope only)

| File | Purpose |
|---|---|
| `tui/AnsiScreen.java` | The shared ANSI parser (extracted from `VirtualScreenTerminal.Screen`) |
| `tui/AnsiScreenTest.java` | Wrapping, cursor addressing/movement, erase, inverse, alternate screen, cursor mode, wide glyphs, OSC |
| `tui/VirtualScreenTerminal.java` | Delegates to `AnsiScreen`; reports any close of the JLine terminal via `isClosed()` |
| `transcript/PtySession.java` | Real `script(1)` PTY, pinned size, key injection, reader thread, `awaitText`, screen/raw accessors, exit code from a side file, idempotent close |
| `transcript/TuiCapabilityIT.java` | 13-case capability matrix on a real PTY |
| `PmConsoleHandoffTest.java` | One terminal for console `plans`, scope set/cleared, borrowed terminal not closed, meanwhile line, direct `plans` closes its own terminal |
| `OperatorContextTerminalTest.java` | Scoped returns the same instance; provider not asked; borrowed/owned; clearing restores the provider |
| `tui/FullScreenAvailabilityTest.java` | Every reason renders EN and DE; available verdict has no reason; inconsistent verdicts rejected |
| `tui/TuiScreenDriverTest.java` | Gate tests extended with the reason precedence |
| `OverviewFullScreenTest.java` | 50×20 asserts the reason on stderr and the tree on stdout |
| `SystemFullScreenTest.java` | Below 60 columns asserts the shared reason on stderr |

### 3.4 Documentation

| File | Change |
|---|---|
| `doc/design/terminal-client/15-overview.adoc` | New *The Hand-Off from the Console* and *Why the View Does Not Open* sections; narrow-terminal row updated |
| `doc/design/terminal-client/04-console.adoc` | New *The Full-Screen Hand-Off* section |
| `doc/design/terminal-client/27-tui-testing.adoc` | New document: the three test layers, the capability matrix, flake control, the real-PTY rationale |
| `doc/design/terminal-client/README.adoc` | Index entries for the console hand-off and the TUI tests |
| `pm-operator/README.md` | Copy-paste manual check with `fully-onboarded`, `plans`/`tui`, `q`, and the VS Code note |

> The plan named the new document `16-tui-testing.adoc`; `16-plan-view.adoc` already owns that
> number, so the document was added as `27-tui-testing.adoc` (the next free number) and indexed.
> The content is exactly what the plan specifies.

---

## 4. Test and Verification Results

### 4.1 Unit Suite

`./mvnw -pl pm-operator -am test`

```text
Tests run: 941, Failures: 0, Errors: 0, Skipped: 0
```

97 test classes, including the new ownership, availability, parser, and hand-off tests.

### 4.2 Integration-Test Profile

`./mvnw -pl pm-operator -Pintegration-tests verify`

```text
completed 58, errors 0, failures 0, skipped 6
```

The 6 skips are the native-only transcript and measurement tests
(`NativeTranscriptIT`, `NativeMeasurementsIT`), skipped with an assumption because
`target/pm-operator` was not built. `TuiCapabilityIT` ran all 13 cases.

The run recorded its mode:

```json
{"item":"tui-capability-jvm","os":"Mac OS X","arch":"aarch64",
 "values":{"columns":120,"mode":"jvm","alternate_screen":true,"rows":30},"pass":true}
```

### 4.3 Capability Matrix (`TuiCapabilityIT`)

| Case | Result |
|---|---|
| `plans` 120×30 | Alternate screen, `Running work`, `q`, exit 0, cursor visible |
| `plans` 80×24 | Alternate screen, `q`, exit 0 |
| `--tui` | Overview opens directly, exit 0 |
| `plan fix-relay` | Plan view on the plan id, `q`, exit 0 |
| `projects` | `All projects`, `q`, exit 0 |
| Console `plans` | Prompt → alternate screen → prompt, one entry, `exit` 0 |
| Console `tui` | The alias opens the same view, exits 0 |
| Console `system` | System view opens and returns to the prompt (`reports-and-system` runtime) |
| `plans --json` | JSON on stdout, no alternate screen |
| 50×20 | `operator.tui.unavailable.too_narrow`, tree, exit 0 |
| `TERM=dumb` | `operator.tui.unavailable.dumb_terminal`, tree, exit 0 |
| `console.full_screen=false` | `operator.tui.unavailable.configured_off`, tree, exit 0 |
| VS Code shape | `TERM_PROGRAM=vscode`, `xterm-256color`, 80×24 opens the view, `q` restores |

### 4.4 Availability Reasons

| Reason | Condition | Key | Checked |
|---|---|---|---|
| `NON_INTERACTIVE` | no interactive terminal | `operator.tui.unavailable.non_interactive` | `TuiScreenDriverTest` |
| `DUMB_TERMINAL` | `getType()` is `dumb`/`dumb-color` | `operator.tui.unavailable.dumb_terminal` | `TuiScreenDriverTest`, PTY `TERM=dumb` |
| `CONFIGURED_OFF` | `console.full_screen=false` | `operator.tui.unavailable.configured_off` | `TuiScreenDriverTest`, PTY config case |
| `TOO_NARROW` | fewer than 60 columns | `operator.tui.unavailable.too_narrow` | `TuiScreenDriverTest`, PTY 50×20 |

The order is the operator's order to act: terminal attachment, terminal type, configuration, width.
`--json` never prints a reason and never enters the alternate screen.

---

## 5. Domain Invariants

| Invariant | Status |
|---|---|
| Java 25 / records / pattern matching style | Kept; `FullScreenAvailability`, `PtySession` and the parser use records, `var`, and switch expressions |
| No uncatalogued user-facing strings | `LiteralTextGuardTest` green; every new line comes from `messages/*.properties` |
| Bilingual catalogue (EN & DE) | Both files carry the four keys; `FullScreenAvailabilityTest` renders each in both locales |
| `PM-SEC-3` gate and coding-agent refusal | Unchanged; the gate refuses non-interactive and agent environments before any view is attempted. The PTY environment deliberately carries no agent signal |
| Terminal input/output trust boundary | Unchanged: runtime text still goes through `TerminalSanitizer`/`OutputSafety`; the new reason lines are constant catalogue text |
| POSIX only, no native PTY dependency | Kept: the harness uses the OS `script(1)` and is test scope only; no production dependency was added |
| One terminal per process | Asserted by the provider call count and by the PTY cases |
| Restoration on `q` | Asserted: alternate screen exited, cursor visible, prompt returned, `exit` 0; `TuiScreenDriver`'s existing `finally` and shutdown hook are unchanged |
| TUI layout, keys, and content levels | Untouched |

---

## 6. Build & Quality Evidence

| Command | Result |
|---|---|
| `./mvnw -pl pm-operator -am test` | `BUILD SUCCESS`, 941 tests, 0 failures |
| `./mvnw -pl pm-operator -Pintegration-tests verify` | `BUILD SUCCESS`, 58 ITs, 0 failures, 6 native-only skips |
| `./mvnw verify -Ppre-commit` | `BUILD SUCCESS` (6:42 min); OpenRewrite, license headers, Checkstyle, Enforcer, Javadoc, and the full unit suite all pass |
| `git status` after pre-commit | Only the intended source, test, catalogue, and documentation files |

The native image was not built in this worktree. The capability suite is written to use
`BinaryUnderTest`, so the same 13 cases run against `target/pm-operator` when it exists; the recorded
mode is `jvm`.

---

## 7. Notes and Risks Carried Forward

* **`PtySession` and `XDG_STATE_HOME`**: a PTY test sets `XDG_STATE_HOME` inside the mock runtime's
  base. Without it, `overview.json` in the operator's real state directory makes `plans` reopen the
  last project (the first capture did exactly that); with it, every case opens at the root and is
  deterministic.
* **Native mode**: run `./mvnw -pl pm-operator -Pnative package -DskipTests` once in CI and re-run
  the IT profile to record the native figures. No production change is needed.
* **`16-tui-testing.adoc` vs `27-tui-testing.adoc`**: the number was moved to avoid the existing
  `16-plan-view.adoc`; see §3.4.
* **No roadmap package**: this is a remediation plan, so no `doc/plans/` package is marked done.
* **Rollback**: remove the scoped terminal and ownership checks to restore the second-terminal
  behavior; `PtySession`, `AnsiScreen`, and the capability suite stay as the regression record.

---

## 8. Archival

The plan is moved to `doc/agy/implemented/operator-tui-pty-and-console-handoff-plan.md`, its links
rebased for the deeper directory, and the archive index row added. The Step 8 audit below moves this
report to `doc/agy/verified/`.

---

## 9. Step 8 Verification (Orchestrator)

**Verdict: PASSED.**

| Pillar | Result | Evidence |
|---|---|---|
| **1 — Traceability** | Pass | The console hand-off, one-terminal ownership, notice hold, availability reasons, PTY capability matrix, and documentation match UC-PLN-1/4, UC-ENT-1, PM-SEC-3, PM-TEST-7, and PM-SEC-7. |
| **2 — Anti-patterns** | Pass | Every new user-facing reason is catalogue text in EN/DE; no literal command output; the terminal trust boundary and `finally` restoration are unchanged; no production PTY dependency; the design-number deviation (`27-tui-testing.adoc`) is recorded. |
| **3 — Subprocess lifecycle** | Pass | `./mvnw -pl pm-operator -Pintegration-tests verify`: 58 tests, 0 failures, 0 errors, 6 native-only skips. `TuiCapabilityIT` ran all 13 PTY cases, including console `plans`/`tui`, VS Code shape, narrow/dumb/config fallbacks, and restoration. `./mvnw verify -Ppre-commit` was green. |
| **4 — Independent diff** | Pass | Changes stay in `pm-operator`, `pm-api` catalogue, terminal design docs, and the README; no test weakening; no unrelated reverts; commit trailer present. |

The report moves to `doc/agy/verified/` and Step 8 is checked off in the plan.
