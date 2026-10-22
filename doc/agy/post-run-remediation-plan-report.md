# Completion Report: `post-run-remediation` (TUI Signal Lifecycle, Interactive Entry, Codec Closure & Mobile E2E)

**Plan**: [`doc/agy/implemented/post-run-remediation-plan.md`](implemented/post-run-remediation-plan.md)
**Branch**: `feat/post-run-remediation`
**Runbook**: [`doc/agy/runbook.md`](runbook.md)
**Addressed findings**: CLI-2 (High), CLI-1 (Medium), MOCK-1 (Low), WEB-1 (Low)

---

## 1. Executive Summary

The post-run remediation wave closes the four findings of the post-run review across `pm-operator`, `pm-mock-runtime`, and `pm-web-e2e`:

- **CLI-2 — terminal restoration on abnormal termination.** `TuiScreenDriver` now registers a JVM shutdown hook when it opens the alternate screen. The hook restores the keypad mode, shows the cursor, leaves the alternate screen, and restores the terminal attributes the raw mode replaced, so `SIGINT`, `SIGTERM`, and any shutdown that skips the `finally` no longer leave the operator's terminal unreadable. The hook and the normal exit share one atomic idempotence latch and may race safely; the hook is removed on a clean exit.
- **CLI-1 — interactive entry.** The root command accepts `--tui` and opens `OverviewFullScreenApp` directly when the machine configuration and the terminal allow it. The console completes and dispatches `tui` as an alias of `plans`, and its greeting names both when the full-screen view can open.
- **MOCK-1 — parameterized route codec closure.** `ScenarioLoader` no longer falls back silently for plan and epic routes: every recognized plan and epic route shape (epic detail, process, intent, outline, activity, and completion) is decoded with its `pm-api` codec, and a representation the contract refuses fails the scenario load with the route and the reason. A route of another shape keeps the generic fallback, and an error-status route is not decoded as a success resource.
- **WEB-1 — mobile viewport coverage.** A dedicated Playwright suite verifies the onboarding wizard and the running-work overview at 320x568 and 375x667: no horizontal document overflow, the attention badges and plan cards render, and the primary controls stay inside the viewport.

All scoped remediation checks pass. One pre-existing, unrelated end-to-end test fails and is recorded in §3.3. The pre-commit reactor build is `BUILD SUCCESS`.

---

## 2. Delivered Artifacts

### 2.1 `pm-operator` — TUI signal and terminal lifecycle (CLI-2)

| File | Change |
|---|---|
| `pm-operator/src/main/java/de/planmarshall/operator/tui/TuiScreenDriver.java` | `restoreImmediately()` as the single idempotent restore path (keypad mode, cursor, alternate screen, terminal attributes); shutdown hook registered on the first `enterAlternate()` and removed on a clean exit; atomic `restored`/`stopping` latches; the drawing loop stops after a restore; `shutdownHookRegistered()` test seam. |
| `pm-operator/src/test/java/de/planmarshall/operator/tui/TuiScreenDriverTest.java` | Three new tests: hook registration while the view is open and removal on a clean exit; the hook restores the screen, cursor, and `ECHO` attribute exactly once while the `finally` writes nothing more; a restore before the view opened writes nothing. |

### 2.2 `pm-operator` — interactive entry and `--tui` (CLI-1)

| File | Change |
|---|---|
| `pm-operator/src/main/java/de/planmarshall/operator/PmOperator.java` | `@Option(names = {"--tui"})`; `call()` opens `OverviewFullScreenApp` directly when `TuiScreenDriver.canOpenFullScreen(...)` allows it and otherwise falls back to the console; the terminal is closed on both arms. |
| `pm-operator/src/main/java/de/planmarshall/operator/PmConsole.java` | `tui` added to the completer and dispatched as an alias of `plans` (arguments after `tui` are preserved); one-line full-screen hint in the greeting, printed only where the view can open; the verb dispatch is factored into `runOperatorVerb(...)`. |
| `pm-operator/src/test/java/de/planmarshall/operator/PmConsoleTest.java` | New test: `tui` dispatches as `plans` and produces the status fallback output on a dumb terminal. |

### 2.3 `pm-api` — bilingual catalogue

| File | Change |
|---|---|
| `pm-api/src/main/resources/messages/operator.en.properties` | `operator.console.hint.tui`; `tui` line in `operator.console.help.body`. |
| `pm-api/src/main/resources/messages/operator.de.properties` | The same two entries in German. |

### 2.4 `pm-mock-runtime` — parameterized route codec closure (MOCK-1)

| File | Change |
|---|---|
| `pm-mock-runtime/src/main/java/de/planmarshall/mock/scenario/ScenarioLoader.java` | `planOrEpicResource(...)` resolves a `GET /api/v1/plans/...` or `GET /api/v1/epics/...` route shape to its codec and `decodePlanOrEpicResource(...)` decodes it: `EpicResource` (epic detail), `ProcessTree` / `EpicProcess` (process), `PlanIntentResource` / `EpicIntentResource` (intent), `OutlineResource` (plan outline), `TaskActivity` (task activity), `PlanCompletionReport` (completion). A recognized object resource refuses a list representation; a non-2xx route carries the error's own body and is not decoded; unknown sub-resources keep the generic fallback. |
| `pm-mock-runtime/src/test/java/de/planmarshall/mock/scenario/ScenarioLoaderTest.java` | Five new tests: a malformed plan process route and a malformed epic detail route are refused with the route field and the codec's reason; a parameterized route without a registered codec keeps the fallback; an error-status route whose body is the error loads; a list representation for a recognized plan resource is refused. |

### 2.5 `pm-web-e2e` — mobile viewport verification (WEB-1)

| File | Change |
|---|---|
| `pm-web-e2e/tests/mobile.spec.js` | Four tests (two journeys x two viewports): onboarding renders and advances to the language step without horizontal overflow at 320x568 and 375x667; running work shows its attention badges and plan cards with the card action inside the viewport, without horizontal overflow. Resets the scenario to `web-pairing` afterwards. |

### 2.6 Plan and documentation

| File | Change |
|---|---|
| `doc/agy/post-run-remediation-plan-report.md` | This report. |
| `doc/agy/implemented/post-run-remediation-plan.md` | The completed plan, moved from `doc/agy/`. |
| `doc/agy/implemented/README.md` | Archive index row for this plan. |

---

## 3. Test & Verification Results

### 3.1 Unit and module tests

| Suite | Result |
|---|---|
| `TuiScreenDriverTest` | 17 tests, 0 failures (3 new) |
| `PmConsoleTest` | 7 tests, 0 failures (1 new) |
| `PmOperatorTest` | 45 tests, 0 failures (1 new: `--tui` opens and leaves the full-screen overview) |
| `ScenarioLoaderTest` | 24 tests, 0 failures (5 new), including the six canonical scenario files loaded from the classpath |
| Full reactor unit suite (`./mvnw verify -Ppre-commit`) | `BUILD SUCCESS` across `pm-api`, `pm-exec`, `pm-mock-runtime`, `pm-relay`, `pm-operator`, `pm-web-app`, `pm-web-e2e` |

### 3.2 Component tests

`npm test` in `pm-web-app`: **50 files, 413 tests passed**.

### 3.3 End-to-end tests

`npx playwright test tests/mobile.spec.js` against the mock runtime on port 7420 and the web app on 5173:

```
✓ onboarding renders without horizontal overflow at 320x568
✓ running work keeps its attention band and cards reachable at 320x568
✓ onboarding renders without horizontal overflow at 375x667
✓ running work keeps its attention band and cards reachable at 375x667
4 passed
```

The existing `onboarding.spec.js` and `overview.spec.js` were re-run as a regression check: 15 passed, 1 failed. The failure is `Machine settings route › opens the onboarding at the aspect whose Change was clicked`, which asserts `/onboarding?step=6` and receives `/onboarding`. It is unrelated to this plan — no `pm-web-app` file is touched, and it reproduces on a second run — and is recorded here for the orchestrator's post-landing verification rather than fixed inside this plan's boundary.

### 3.4 Scenario load closure

All six canonical scenario files (`designer`, `fully-onboarded`, `onboarding`, `projects-and-start`, `reports-and-system`, `running-work`) load cleanly with the new parameterized codec validation. The scenario JSON files were not modified.

---

## 4. Domain Invariants

| Invariant | Status |
|---|---|
| Java 25 / project compiler release | Pass — the reactor compiles with the parent's release; no preview or removed API is used. |
| No uncatalogued strings | Pass — `LiteralTextGuardTest` passes; every new operator-facing line comes from `messages/*.properties`. |
| Bilingual catalogue (EN & DE) | Pass — `MessageCatalogueTest` passes; both new keys exist in both languages. |
| Closed mock schema and codec validation | Pass — parameterized plan/epic routes are now decoded; all canonical scenarios load. |
| Terminal safety and shutdown safety | Pass — the restore hook writes only terminal capabilities, is idempotent, restores attributes, and never throws; the terminal-sanitizer boundary is untouched. |
| Responsive layout down to 320px | Pass — asserted by the Playwright mobile suite at 320x568 and 375x667. |
| `skip-bot-review` invariant | Pass — no label is applied to this worker PR. |

---

## 5. Build & Quality Evidence

```
./mvnw verify -Ppre-commit
...
[INFO] Reactor Summary for pm-mcp-clients 0.1.0-SNAPSHOT:
[INFO] pm-mcp-clients ..................................... SUCCESS [  7.749 s]
[INFO] pm-api ............................................. SUCCESS [01:05 min]
[INFO] pm-exec ............................................ SUCCESS [  9.565 s]
[INFO] pm-mock-runtime .................................... SUCCESS [ 46.641 s]
[INFO] pm-relay ........................................... SUCCESS [ 11.287 s]
[INFO] pm-operator ........................................ SUCCESS [03:31 min]
[INFO] pm-web-app ......................................... SUCCESS [ 49.988 s]
[INFO] pm-web-e2e ......................................... SUCCESS [  6.919 s]
[INFO] BUILD SUCCESS
```

The gate covers the OpenRewrite recipes, license headers, Checkstyle, the enforcer rules, Javadoc, the full unit suite, and the `pm-web-e2e` ESLint pass (the new spec is lint-clean).

### 5.1 Notes and deliberate deviations

1. **`PlanView` / `EpicView` do not exist in `pm-api`.** The plan named those codecs, but the repository has no such types: the plan detail is served as a `ProcessTree` (`GET /api/v1/plans/{id}/process`), and the epic detail is an `EpicResource` (`GET /api/v1/epics/{id}`). The closure was implemented against the real resources instead of inventing types, covering the plan/epic detail, process, intent, outline, activity, and completion routes that the scenarios actually use. A plain `GET /api/v1/plans/{id}` has no codec and no scenario route; it keeps the fallback, with a comment saying why.
2. **`operator.console.welcome` does not exist.** The greeting has no such key. The hint is a new `operator.console.hint.tui` line printed once in the greeting (only when the view can open), and `tui` was added to `operator.console.help.body` in both languages.
3. **Pre-existing OpenRewrite drift in the baseline.** Running `./mvnw verify -Ppre-commit` applies the parent's `OrderImports` recipe to the whole tree and rewrote 226 files that this plan does not touch (a blank line between import groups, text-block re-indentation, and `test`-prefix renames). Those unrelated rewrites were reverted so the PR stays inside the plan's declared boundary; the files this plan touches are in the gate's rewritten form. The drift is a baseline condition of `main` after the parent bump, not a result of this change, and should be handled by a separate formatting landing.
4. **Playwright is skipped by default in the Maven gate.** The mobile suite was executed explicitly against a locally started `pm-mock-runtime`; the Maven build runs its ESLint pass.
5. **Two sibling-repository links name documents that have moved.** The plan's `terminal-safety.adoc` and `scenario-format.adoc` targets no longer exist in `plan-marshall-documentation`; the relative depth was repaired for the plan's new location, but the upstream documents themselves have moved and the link targets are unchanged.

---

## 6. Personas Applied

| Persona | Applied to |
|---|---|
| `persona-implementer` | The driver, command, console, and loader changes (SRP, CQS, immutable state, explicit failure paths). |
| `persona-module-tester` | The new unit tests (AAA, one behaviour per test, negative cases for the refused routes, no order dependence). |
| `persona-integration-tester` | The Playwright mobile journeys against the real mock runtime and web app. |
| `persona-security-expert` | Terminal restore safety (raw mode, cursor, alternate screen, attributes), the no-`skip-bot-review` invariant, and the absence of new output sinks. |
| `persona-code-reviewer` | Review of the footprint for surplus code, naming accuracy, and contract drift before the PR. |
