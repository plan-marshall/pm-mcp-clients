# Completion Report: `cli-fullscreen-foundation` (Terminal Full-Screen TUI Framework, Screen Test Harness & Hygiene)

**Status:** Implemented and verified; PR #124 is open and under review (Step 8 is reserved for the orchestrator)
**Plan:** [`doc/agy/implemented/cli-fullscreen-foundation-plan.md`](implemented/cli-fullscreen-foundation-plan.md)
**Branch:** `feat/cli-fullscreen-foundation`
**Governed by:** [`doc/agy/runbook.md`](../runbook.md) and [`doc/meta/verification-protocol.md`](../meta/verification-protocol.md)

---

## 1. Executive Summary

This plan delivered the foundation every terminal full-screen view builds on, plus the hygiene
items the TUI review recorded:

1. **Cross-view design and plan-index hygiene.** The terminal design now specifies the level stack
   and root navigation, the per-level key registry, the narrow-terminal thresholds, the header's
   connection state, and the full-screen → dialogue hand-off. All broken relative links in
   `doc/plans/` and `doc/plans/implemented/` were repaired (159 intra-repository links); the six
   broken index links to `cli-onboarding`, `cli-plan-view`, and `web-plan-view` now point into
   `implemented/`. The full-screen status of the five terminal packages is recorded in the index.
2. **A pure-Java TUI model** (`de.planmarshall.operator.tui`): `TuiLevel`, `TuiRow`,
   `TuiKeyAction`, `TuiKeyRegistry`, `TuiViewStack`, `TuiScreen`, and `TuiLayout`. Navigation,
   selection-by-id, filtering, help, key/footer generation, width-aware truncation, scroll
   indicators, and the inverted selection row are all testable without a terminal.
3. **A JLine 4 screen driver** (`TuiScreenDriver`): alternate screen, hidden cursor, keypad
   application mode, `BindingReader`/`KeyMap`, `Display`, resize handling, filter/dialogue
   suspension, and unconditional restoration in a `finally`.
4. **Notices and persistence:** `ConsoleNoticeBuffer` holds notices while a view is open and emits
   the coalesced `operator.overview.meanwhile` line on leaving; `OverviewStateStore` reads and
   atomically writes `$XDG_STATE_HOME/pm-operator/overview.json` with strict `0600`/`0700`
   permissions and a corrupt-file fallback; `TuiScreenDriver.canOpenFullScreen` honours
   interactivity, `TERM`, `console.full_screen`, `NO_COLOR`, and the 60-column minimum.
5. **A virtual-terminal screen-test harness** (`VirtualScreenTerminal`): an `ExternalTerminal`
   with scripted input and an ANSI parser that yields exact screen lines, inverted rows,
   alternate-screen entry/exit counts, and cursor visibility — used at 120×30, 80×24, and 50×20,
   with a resize test and an exception-restoration test.
6. **CLI/defect hygiene:** the `{time}` placeholder bug in the reports arrival notice is fixed and
   its transcript updated; `plan --depth` is now an integer `<n>`; the unspecified `--depth` and
   `--watch` options of `epic` and the unspecified `--watch` of `plans` are removed; the shared
   overview texts (`operator.overview.keys.*`, `more_above`, `more_below`, `connected`,
   `meanwhile`, and its three counters, `too_narrow`) exist in both languages.

**Key outcome for the next waves:** `cli-fullscreen-overview`, `cli-fullscreen-plan-view`,
`cli-fullscreen-projects`, `cli-fullscreen-reports-system`, and `cli-fullscreen-actions` can now
implement their content as levels of one framework instead of each building terminal handling.

---

## 2. Delivered Artifacts

### 2.1 Documentation

| File | Change |
|---|---|
| `doc/design/terminal-client/15-overview.adoc` | Added *Levels, Roots, and Returning*, *The Key Registry*, *Narrow Terminals*, *The Header's Connection State*, and *Handing Off to a Dialogue*; added the `reload` and `system` key texts. |
| `doc/design/terminal-client/04-console.adoc` | Added the full-screen connection state and the *A Dialogue out of the Full-Screen View* lifecycle. |
| `doc/plans/README.adoc` | Fixed the six broken package links; added the `cli-fullscreen-foundation` package row; added *Full-Screen Status of the Terminal Packages*. |
| `doc/plans/implemented/*.adoc` (27 files) | Repaired the relative links that moved one directory deeper (`../design` → `../../design`, `epic-clients` → `../epic-clients`, sibling-repository links to four levels). |

159 intra-repository broken links were repaired. The remaining relative links to
`plan-marshall-documentation`, `pm-mcp-core`, and `plan-marshall-mcp` resolve from a checkout that
places the repositories beside each other, as the index states.

### 2.2 `pm-api`

| File | Change |
|---|---|
| `src/main/resources/messages/overview.en.properties` | Added `operator.overview.keys.reload`, `operator.overview.keys.system`, and `operator.overview.meanwhile.{decisions,reports,needs}`. |
| `src/main/resources/messages/overview.de.properties` | Same keys in German. |
| `src/test/java/.../message/OverviewCatalogueTest.java` | Requires the two new key texts. |

The `operator.overview.*` texts live in the `overview` area, which is the existing convention
(`OverviewCatalogueTest` is its source of truth), not duplicated in the `operator` area.

### 2.3 `pm-operator` — new production code

| File | Purpose |
|---|---|
| `tui/TuiLevel.java` | A level of the view (kind, item id, title) and the closed `Kind` catalogue. |
| `tui/TuiRow.java` | A row: stable id, line, details, `!` marker, openable. |
| `tui/TuiKeyAction.java` | Closed key-action catalogue whose names derive the footer text keys. |
| `tui/TuiKeyRegistry.java` | Per-level bindings and the footer generated from them; `validateAll()` rejects a duplicate trigger and a footer entry without a binding. |
| `tui/TuiViewStack.java` | Push/pop/replaceRoot/jumpTo, selection by id across refreshes, filter, help, and `Esc` ordering (help → filter → pop). |
| `tui/TuiScreen.java` | One drawing's immutable description. |
| `tui/TuiLayout.java` | Header/badge, body with `▶` + inverse selection, details, `↑ n more`/`↓ n more`, filter bar, help, footer, and width-safe truncation. |
| `tui/TuiScreenDriver.java` | JLine 4 driver: alternate screen, cursor/keypad, `BindingReader`/`KeyMap`, `Display`, `WINCH`, filter-hand-off, and `finally` restoration; `canOpenFullScreen(...)` and `console.full_screen` reading. |
| `tui/package-info.java` | The model/driver split of the package. |
| `ConsoleNoticeBuffer.java` | Holds decisions, reports, and other needs while a view is active; coalesces the meanwhile line. |
| `OverviewSavedState.java` | The persisted state record and its Jackson streaming codec. |
| `OverviewStateStore.java` | Atomic read/write of `overview.json` with permission verification and corrupt-file fallback. |

### 2.4 `pm-operator` — modified production code

| File | Change |
|---|---|
| `EntryFlow.java` | The reports arrival notice now passes `"time", since` and formats in the client locale. |
| `PmConsole.java` | Notices are offered to `ConsoleNoticeBuffer`; `enterFullScreen()`/`leaveFullScreen(...)` and `noticeBuffer()` are the hook the view plans use. |
| `OperatorStateDirectory.java` | New `overviewFile()` (`<root>/overview.json`). |
| `PlanCommand.java` | `--depth <n>` is an `int` (default 3); no alias parsing. |
| `EpicCommand.java` | Removed the unspecified `--depth` and `--watch` options. |
| `PlansCommand.java` | Removed the unspecified `--watch` option. |

### 2.5 Tests

| File | Coverage |
|---|---|
| `tui/TuiViewStackTest.java` | root/push/pop/jumpTo/replaceRoot, refresh anchoring and nearest-row fallback, filter, `Esc` ordering, movement edges, empty stack. |
| `tui/TuiKeyRegistryTest.java` | registry consistency, footer-without-binding and duplicate-trigger guard (positive and negative), localization, switch-view target, closed key set. |
| `tui/TuiLayoutTest.java` | exact height, header/badge, inverse selection, footer, 100/80/60-column thresholds, scroll indicators, filter bar, help overlay, truncation of surrogate pairs and wide glyphs, sanitization. |
| `tui/TuiScreenDriverTest.java` | 120×30 and 80×24 drawing, 50×20 too-narrow fallback, terminal restoration after `q`, restoration when the renderer throws, resize redraw, and the `canOpenFullScreen` gate (interactive, dumb, width, `console.full_screen` true/false/malformed, `NO_COLOR`). |
| `tui/OverviewStateStoreTest.java` | round-trip, partial state, missing/corrupt/oversized file, unknown fields, strict permissions, symlink refusal, insecure permissions. |
| `tui/ConsoleNoticeBufferTest.java` | inactive/inactive holding, coalescing, German summary, empty summary, clear. |
| `tui/VirtualScreenTerminal.java` | The reusable harness: scripted input, ANSI screen parser, inverted rows, alternate-screen tracking, resize. |
| `EntryFlowTest.java` | The arrival notice fills `{time}` and never prints the placeholder. |
| `PmConsoleBehaviourTest.java` | A notice is held during the view and printed as the meanwhile line on leaving. |
| `ConsoleTextsTest.java` | Every shared overview key renders in English and German. |
| `pm-api .../OverviewCatalogueTest.java` | New keys are present. |
| `src/test/resources/transcript/console-status.transcript` | `{time}` → `<timestamp>` (the normalizer masks the instant). |

---

## 3. Test & Verification Results

| Gate | Result |
|---|---|
| `./mvnw -pl pm-operator -am test` | **PASS** — pm-api, pm-mock-runtime, and pm-operator suites green (641 tests in the first full run; the new harness tests added afterwards are part of the pre-commit run). |
| `./mvnw verify -Ppre-commit` | **BUILD SUCCESS** — all seven modules: `pm-api`, `pm-exec`, `pm-relay`, `pm-mock-runtime`, `pm-operator`, `pm-web-app`, `pm-web-e2e`. OpenRewrite, license headers, Checkstyle, Enforcer, Javadoc, and the unit suites passed. First run 06:43 min; the re-run after the review findings 05:52 min. |
| Transcript `console-status` | Updated and green; the arrival line now carries the instant, masked as `<timestamp>` by the transcript normalizer. |
| Screen tests | 120×30, 80×24, and 50×20 virtual terminals; resize and exception restoration. |

### Parent-recipe drift (boundary discipline)

`verify -Ppre-commit` runs the parent's OpenRewrite `AutoFormat`/`OrderImports` recipes. On this
checkout those recipes reformatted **309 files outside this plan's footprint** (record-component
indentation and import-group blank lines). Those rewrites were reviewed, found unrelated to this
plan and destructive to the existing formatting, and reverted, per Pillar 4 of the verification
protocol (*no unintended reverts / boundary discipline*). The gate itself had already reported
`BUILD SUCCESS`; the reverted files are untouched by this work package.

---

## 4. Domain Invariants

| Invariant | Status |
|---|---|
| Java 25 | All new code is Java 25, uses records/enums/pattern-free switches compatible with the configured release, and compiles under `-Werror`. |
| No uncatalogued user-facing strings | Every footer, badge, indicator, meanwhile line, and too-narrow text comes from `messages/overview.{en,de}.properties`; `LiteralTextGuardTest` stays green. |
| Bilingual catalogue (EN & DE) | All new keys exist in both packs; `OverviewCatalogueTest` enforces identical key sets, and `ConsoleTextsTest` formats both. |
| Terminal safety boundary | Every dynamic cell is sanitized through `OutputSafety.stripControlSequences` before drawing; truncation never splits a surrogate pair or a wide glyph. |
| Clean terminal restoration | `TuiScreenDriver` restores attributes, keypad mode, cursor, and the alternate screen in `finally`; the exception test proves it. |
| State outside `<PM_MCP_BASE>` | `overview.json` lives in `$XDG_STATE_HOME/pm-operator/`, is `0600` in a `0700` directory, and holds no plan content. |
| No `jackson-databind` | `OverviewSavedState`/`OverviewStateStore` use `JsonFactory`/`JsonParser`/`JsonGenerator` from `jackson-core` only. |
| Module boundary | Changes are confined to `pm-api` (messages and their test) and `pm-operator` plus the documentation named in the plan. |

---

## 5. Build & Quality Evidence

```
[INFO] Reactor Summary for pm-mcp-clients 0.1.0-SNAPSHOT:
[INFO] pm-mcp-clients ..................................... SUCCESS [  8.828 s]
[INFO] pm-api ............................................. SUCCESS [01:15 min]
[INFO] pm-exec ............................................ SUCCESS [ 16.205 s]
[INFO] pm-relay ........................................... SUCCESS [ 14.104 s]
[INFO] pm-mock-runtime .................................... SUCCESS [ 47.995 s]
[INFO] pm-operator ........................................ SUCCESS [02:54 min]
[INFO] pm-web-app ......................................... SUCCESS [ 52.533 s]
[INFO] pm-web-e2e ......................................... SUCCESS [ 13.496 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  05:52 min
```

### Deliberate decisions

* **`--depth` aliases.** The design names `--depth <n>` only; no existing test or document used
  `compact`/`standard`/`full`, so the aliases were dropped rather than kept as hidden behaviour.
  `epic` has no `--depth` or `--watch` in `01-commands.adoc`, so both were removed there.
* **`console.full_screen` default.** The plan specifies `true` as the default; the machine-store
  JSON schema in `pm-mcp-core` currently states `false`. The gate follows the plan and the design
  (`true` where the terminal allows it); the schema discrepancy is recorded here for the
  orchestrator's Step 8 cross-check and is not silently accommodated in code.
* **`NO_COLOR`.** It does not close the view: the framework uses no colour, only inverse video for
  the selected row, so a `NO_COLOR` terminal still gets the navigation. The gate documents this.
* **No content views in this plan.** The framework, gate, notice buffer, and persistence are
  production-complete; the overview, plan, project, report, and system levels are deliberately the
  next five plans, as the plan's concurrency section states. The driver is exercised through the
  virtual terminal, and the console hook is exercised through `PmConsoleBehaviourTest`.

---

## 6. Automated Review Findings (100% resolved)

CodeRabbit's first review posted 12 findings; all were accepted and fixed in the follow-up
commits, none was refuted.

| # | Finding | Resolution |
|---|---|---|
| 1 | Report said "ready for landing" while the workflow was not landed | Status now says "PR #124 is open and under review"; steps 6 and 7 stay unchecked until the merge |
| 2 | `doc/plans/README.adoc` linked the new plan as `.adoc` | Both links now target the `.md` file |
| 3 | `ConsoleNoticeBuffer` could lose a notice between `hold` and the exit drain | `hold`, `holdLine`, `deactivate`, and the new atomic `drain` are `synchronized`; the transition cannot interleave |
| 4 | `plan --depth 0` silently used the default | A picocli `ITypeConverter` rejects a non-positive or non-numeric value at parse time (exit 2, catalogue text); a test covers both |
| 5 | `report.sent` with `count: 3` was counted once | `hold(Category, count)` receives the validated event count (capped at 10 000) |
| 6 | The `device.paired` message and a fresh code bypassed the buffer | `printAboveOrHold` defers those lines verbatim; `Drained` returns them after the meanwhile line, and a test covers it |
| 7 | `PAGE_UP`/`PAGE_DOWN`/`FIRST`/`LAST` had no catalogue text | Added `operator.overview.keys.{page,first,last}` in EN/DE; `ConsoleTextsTest` now iterates every `TuiKeyAction` |
| 8 | Scroll indicators could overwrite the selected row | The viewport reserves the first and last body lines for the indicators and fits the selection into the lines between them; a regression test covers rows 0, 25, and 39 at 80×12 |
| 9 | `rowsBelow` returned rows above plus visible | Replaced by `distinctRows(lines, from, total)`, which counts only the rows below the window |
| 10 | The driver dispatched actions a level does not bind | `boundOn(screen.kind(), action)` drops them before the handler; a test sends `e` on the overview |
| 11 | A lone `Esc` waited the 1 s default ambiguity timeout | `KeyMap.setAmbiguousTimeout(100 ms)`; a test asserts the configured value |
| 12 | The file-level security tests never reached `verifyFile` | Both tests now create the `0700` directory first, so only the symlink or the `0644` file can make the read fall back |
| 13 | The filter hand-off could leave the alternate screen blank | The driver invalidates its frame cache after `resume(display)`; a test presses `/`, Enter, and asserts the rows are drawn again |

### Additional tests added for the review

`PlanCommandTest.depthMustBePositive`, `ConsoleNoticeBufferTest.countsCoalescedReports`,
`keepsVerbatimLines`, `closedBufferRejectsLine`, `TuiLayoutTest.indicatorsKeepSelectionVisible`,
`TuiScreenDriverTest.unboundKeyIsDropped`, `escapeHasShortAmbiguityTimeout`, and
`filterHandoffRedraws`, and the tightened `OverviewStateStoreTest` security tests.
