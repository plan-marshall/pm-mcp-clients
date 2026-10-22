# Implementation Plan: `post-run-remediation` (TUI Signal Lifecycle, Interactive Entry, Codec Closure & Mobile E2E)

This implementation plan governs the post-run remediation of findings identified during the post-run review wave ([`doc/meta/post_run_review.adoc`](../meta/post_run_review.adoc)) across `pm-operator`, `pm-mock-runtime`, and `pm-web-e2e`.

It operationalizes [`doc/meta/implement_use-cases.adoc`](../meta/implement_use-cases.adoc) under the execution discipline of [`doc/agy/runbook.md`](runbook.md).

---

## 1. Traceability & Context

| Dimension | Reference |
|---|---|
| **Audit Standard** | [`doc/meta/post_run_review.adoc`](../meta/post_run_review.adoc), [`doc/meta/verification-protocol.adoc`](../meta/verification-protocol.adoc) |
| **Audit Report** | `post_run_wave_review_report.md` (Artifact from Post-Run Review) |
| **Specifications & Design** | CLI: [`doc/design/terminal-client/04-console.adoc`](../design/terminal-client/04-console.adoc) (`#console`), [`15-overview.adoc`](../design/terminal-client/15-overview.adoc) (`#full-screen`), [`doc/specification/terminal-safety.adoc`](../../../plan-marshall-documentation/doc/specification/terminal-safety.adoc)<br>Mock: [`doc/specification/scenario-format.adoc`](../../../plan-marshall-documentation/doc/specification/scenario-format.adoc)<br>Web: [`doc/design/web-app/`](../design/web-app/) (Responsive 320px viewport invariant) |
| **Addressed Findings** | **CLI-2** (High: TUI signal & shutdown hook terminal restoration)<br>**CLI-1** (Medium: interactive entry & `--tui` flag on root command)<br>**MOCK-1** (Low: route codec validation closure for plans and epics)<br>**WEB-1** (Low: mobile viewport Playwright smoke tests) |
| **Implementation Recipe** | [`doc/meta/implement_use-cases.adoc`](../meta/implement_use-cases.adoc) |
| **Operational SOP** | [`doc/agy/runbook.md`](runbook.md) |

---

## 2. Scope & Objectives

1. **TUI Terminal State Restoration & Signal Traps (`pm-operator`, CLI-2)**:
   - In [`TuiScreenDriver.java`](../pm-operator/src/main/java/de/planmarshall/operator/tui/TuiScreenDriver.java), ensure that entering raw mode and alternate screen buffer registers a JVM shutdown hook (`Runtime.getRuntime().addShutdownHook`) and handles `SIGINT`/`SIGTERM` so that any abnormal termination, kill signal, or uncaught exception cleanly invokes `restore()` (restores keypad mode, restores cursor visibility via `\033[?25h`, exits alternate screen via `\033[?1049l`, and restores terminal attributes).
   - Verify that multiple exits (normal `finally` vs. shutdown hook) are idempotent and thread-safe via atomic state flags.
   - Add unit tests verifying shutdown hook registration and deregistration upon normal exit.
2. **Interactive TUI Entry & Console Prompt Guidance (`pm-operator`, CLI-1)**:
   - Add `--tui` option to root [`PmOperator.java`](../pm-operator/src/main/java/de/planmarshall/operator/PmOperator.java) to allow explicit, direct launch of [`OverviewFullScreenApp`](../pm-operator/src/main/java/de/planmarshall/operator/overview/OverviewFullScreenApp.java) without needing the `plans` subcommand.
   - In [`PmConsole.java`](../pm-operator/src/main/java/de/planmarshall/operator/PmConsole.java), add `tui` to auto-completion candidates as an alias for `plans`.
   - Update `operator.console.welcome` / prompt banner in `pm-api` message catalogues to mention `plans` (or `tui`) for entering the full-screen view.
3. **Parameterized Route Codec Closure (`pm-mock-runtime`, MOCK-1)**:
   - In [`ScenarioLoader.java`](../pm-mock-runtime/src/main/java/de/planmarshall/mock/scenario/ScenarioLoader.java), expand `validateRouteRepresentation` to validate route representations matching `/api/v1/plans/{id}` and `/api/v1/epics/{id}` against their `pm-api` codecs (`PlanView` / `EpicView`), eliminating the generic fallback for known route patterns.
   - Verify closed schema validation passes across all scenarios in `src/main/resources/scenarios/`.
4. **Mobile Viewport E2E Verification (`pm-web-e2e`, WEB-1)**:
   - Add targeted mobile viewport assertions (375x667 and 320x568) in [`pm-web-e2e`](../pm-web-e2e/tests/) for critical user journeys:
     - Onboarding flow (`onboarding.spec.js` or dedicated `mobile.spec.js`)
     - Overview attention band and cards (`overview.spec.js`)
   - Assert that no horizontal document overflow (`scrollWidth > clientWidth`) occurs and interactive elements remain accessible.

---

## 3. Concurrency & Isolation

- **Depends on**: All prior wave packages landed on `main`.
- **Parallelizable with**: None (standalone post-run remediation wave).

### Boundary Rules
- **Touches**:
  - `pm-operator/src/main/java/de/planmarshall/operator/tui/TuiScreenDriver.java`
  - `pm-operator/src/main/java/de/planmarshall/operator/PmOperator.java`
  - `pm-operator/src/main/java/de/planmarshall/operator/PmConsole.java`
  - `pm-mock-runtime/src/main/java/de/planmarshall/mock/scenario/ScenarioLoader.java`
  - `pm-web-e2e/tests/`
  - `pm-api/src/main/resources/messages/operator.{en,de}.properties` (if prompt text is updated)
- **No Touch**:
  - `doc/plans/README.adoc` (all packages are `done`)
  - `pm-mock-runtime/src/main/resources/scenarios/*.json` (scenarios remain unchanged)

---

## 4. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created at `/Users/oliver/git/worktrees/pm-mcp-clients-post-run-remediation` on branch `feat/post-run-remediation` off latest `main`.
- [x] 2. Domain skills & engineering personas loaded where applicable:
  - `persona-implementer` & `persona-module-tester` for code & unit tests
  - `persona-integration-tester` for multi-module/integration tests
  - `persona-security-expert` for terminal sanitization & shutdown safety
  - `persona-code-reviewer` for finding evaluation & adversarial refutation
- [x] 3. Incremental implementation with tests following the phased technical recipe.
- [x] 4. Final completion report written (`doc/agy/post-run-remediation-plan-report.md`) and initial plan moved to `doc/agy/implemented/post-run-remediation-plan.md`. (Do NOT move report to `verified/` — Step 8 is reserved for orchestrator).
- [x] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit`).
- [ ] 6. PR opened (no `skip-bot-review`), review window waited, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [ ] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [ ] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/post-run-remediation-plan-report.md` by orchestrator session.

---

## 5. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: TUI Signal & Terminal Lifecycle** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-security-expert` | `pm-dev-java:java-core`<br>`pm-dev-java:junit-core` | `TuiScreenDriver` JVM shutdown hook, signal handling, idempotent cleanup. |
| **Phase 2: Interactive Entry & TUI Flag** | `plan-marshall-persona-implementer` | `pm-dev-java:java-core` | `PmOperator --tui`, `PmConsole` autocompletion and hint. |
| **Phase 3: ScenarioLoader Codec Closure** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-module-tester` | `pm-dev-java:java-core` | Validate parameterized routes in `ScenarioLoader.java`. |
| **Phase 4: Mobile Viewport E2E** | `plan-marshall-persona-integration-tester` | `pm-dev-frontend-javascript` | Playwright 320px/375px viewport checks for horizontal scroll and layout integrity. |
| **Phase 5: Pre-commit & PR Gate** | `plan-marshall-persona-code-reviewer` | `pm-dev-java:java-security` | Full reactor verification and 100% findings resolution. |

---

## 6. Phased Technical Recipe

### Phase 1: TUI Signal & Shutdown Hook Lifecycle (`pm-operator`, CLI-2)
1. In `TuiScreenDriver.java`:
   - Add a private `Thread shutdownHook` field.
   - When entering alternate screen (`enterAlternate()`), register the hook:
     ```java
     shutdownHook = new Thread(this::restoreImmediately, "tui-terminal-restore");
     Runtime.getRuntime().addShutdownHook(shutdownHook);
     ```
   - In `finally` block or `restore()`, deregister the hook cleanly (`Runtime.getRuntime().removeShutdownHook(shutdownHook)` inside try/catch `IllegalStateException`).
   - Implement `restoreImmediately()` so it is completely thread-safe, idempotent, and flushes `exit_ca_mode` and `cursor_normal` to standard out.
2. Unit tests in `TuiScreenDriverTest`:
   - Test hook registration when entering raw/alternate mode and removal upon clean `run()` exit.

### Phase 2: Root Command `--tui` Flag & Console Guidance (`pm-operator`, CLI-1)
1. In `PmOperator.java`:
   - Add `@Option(names = {"--tui"}, description = "Launch full-screen overview TUI directly") boolean tui;`
   - In `call()`: If `tui` is specified and `TuiScreenDriver.canOpenFullScreen(...)` is true, launch `new OverviewFullScreenApp(context, terminal).run()` directly instead of `PmConsole`.
2. In `PmConsole.java`:
   - Add `"tui"` to the `StringsCompleter` list.
   - When the user types `tui`, dispatch directly to `PlansCommand` (which opens `OverviewFullScreenApp`).
   - Include a concise one-line hint in the console greeting noting `plans` (or `tui`) launches the full-screen view.

### Phase 3: Dynamic Route Codec Validation (`pm-mock-runtime`, MOCK-1)
1. In `ScenarioLoader.java:validateRouteRepresentation`:
   - Match routes starting with `GET /api/v1/plans/` and decode using `PlanView.fromMap(map)`.
   - Match routes starting with `GET /api/v1/epics/` and decode using `EpicView.fromMap(map)`.
   - Ensure exceptions are cleanly wrapped in `ScenarioValidationException`.
2. Run `./mvnw test -pl pm-mock-runtime` and assert all scenarios load cleanly.

### Phase 4: Mobile Viewport E2E Tests (`pm-web-e2e`, WEB-1)
1. In `pm-web-e2e/tests/overview.spec.js` (or `pm-web-e2e/tests/mobile.spec.js`):
   - Add a test with `page.setViewportSize({ width: 375, height: 667 })` and `{ width: 320, height: 568 }`.
   - Verify overview cards stack vertically without horizontal overflow:
     ```javascript
     const hasHorizontalScroll = await page.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth);
     expect(hasHorizontalScroll).toBe(false);
     ```
2. Verify in `onboarding.spec.js` that the onboarding wizard renders cleanly at 320px width without clipped buttons.

### Phase 5: Verification Sweep & PR
1. Run full reactor build: `./mvnw verify -Ppre-commit`.
2. Run `npm test` in `pm-web-app`.
3. Open pull request following Step 6 of `doc/agy/runbook.md`.
