# PLAN-45: Web: One Error Pattern, One Loading Pattern

epic: ui-improvements
workstream: WS-03

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

No path ends in a raw error code or a loading text without end: the error pattern by error code, request time limits, and skeletons that name what loads (UX-S-06; UX-P-08).

Source of this plan (read before scoping):

- `doc/claude_review/ux/plan.adoc (UX-S-06)`
- `doc/claude_review/ux/proposals.adoc#UX-P-08`
- `doc/claude_review/ux/findings.adoc#UX-F-06`
- `doc/claude_review/ux/findings.adoc#UX-F-14`

## Deliverables

1. **Error texts by code**
   - What: `ErrorState` shows the title, the cause and the fix of the code from the shared set `PLAN-86` (`error.<code>.title`, `.cause`, `.fix` in `de` and `en`, read through `displayError(code)` of `PLAN-09`); an unknown code uses `error.unknown.*` and shows the code under "Details". This plan writes no error text: a missing key is reported to `PLAN-86`, not added here.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:javadoc, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-dev-java-cui:cui-logging
   - Done when:
     - A unit test: `ErrorState` for a known code shows its title, cause and fix in `de` and `en`; for an unknown code the fallback and the code under "Details" (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `/plans/does-not-exist`; `/start` for an unknown project; stopping the mock while a view loads shows the pattern within 15 s; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
2. **Time limits**
   - What: `lib/api/client.ts`: 15 s per request, 30 s for a check, ending in an error with code `timeout`.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - A unit test with a request that never answers gets `timeout` after the limit (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `/plans/does-not-exist`; `/start` for an unknown project; stopping the mock while a view loads shows the pattern within 15 s; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
3. **Views on the pattern**
   - What: Every view shows `ErrorState` for a failed load and `Skeleton` while loading, with the loading text as an `<area>.loading` key that names what loads (the keys formerly planned in `PLAN-09` D4: this plan owns every loading text); `plan_not_found` and `project_not_found` show title, cause and an act.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:javascript-security
   - Done when:
     - e2e: `/plans/does-not-exist` shows the pattern; stopping the mock shows it within 15 s with "Try again" (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `/plans/does-not-exist`; `/start` for an unknown project; stopping the mock while a view loads shows the pattern within 15 s; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)
4. **A reload keeps the list on screen (from the landing of PLAN-102)**
   - What: `ReportsView.svelte` (~67-70) replaces the whole list pane with a `Skeleton` on every `loadReports()` (a `report.sent` without ids, a retry): an open `RowMenu`, the scroll position and the focus inside the list are lost, and PLAN-102's stable-page fix covered the detail and the filter only (`landings/PLAN-102.md` defect 15). Give the loading pattern of deliverable 3 a refresh form that keeps the rows, the scroll position and an open menu while the data reloads, and use it on the reports list.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
   - Done when:
     - A test that fails before: a reload with a row menu open and the list scrolled keeps both (evidence order: `test-first`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)
5. **One client-level time limit and one truncated-body contract (from the landing of PLAN-100)**
   - What: PLAN-100 bounded the settings and drain requests with six scattered 15 s constants (`SettingsView` `LOAD_TIMEOUT_MS`; `+layout.svelte` `WITHDRAW_TIMEOUT_MS`, `STOP_NOW_TIMEOUT_MS`, `STATE_REFRESH_TIMEOUT_MS`; `client.ts` `REVOKE_TIMEOUT_MS`, `WITHDRAW_TIMEOUT_MS`) and a `signal ?? AbortSignal.timeout(...)` fallback in `withdrawStop` that the product never uses (`landings/PLAN-100.md` defect 10). Deliverable 2 introduces the client-level limit: collapse these into it and delete the dead fallback. A truncated response body (`res.json()` SyntaxError) gets one error contract in `request()` and is an unknown outcome for the acts that have one (defect 13: CodeRabbit offered an issue, none was opened). A lost response (`network_offline`) is unknown for revoke too: `revokeTimedOut` in `SettingsView.svelte` omits it and does not re-read the list (defect 8).
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
   - Done when:
     - Tests that fail before: a truncated body ends in the unknown outcome; a lost revoke response re-reads the list; no per-call 15 s constant remains (`git grep -n "15_000" -- pm-web-app/src` names only the client's limit) (evidence order: `test-first`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)
## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-web-app verify`, `PM_MOCK_WEB_PORT=20450 PM_WEB_PORT=20451 ./mvnw -pl pm-web-e2e verify -Pe2e`, `./mvnw -pl pm-api verify`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- CUI standards (`CLAUDE.md`, `PLAN-77`): a test that takes input data draws it from `Generators` with `@EnableGeneratorController` (the expected value of a contract stays literal); a log call at INFO or above goes through a `LogRecord` of the module's own `LogMessages` class (one per module that logs, never one for the repository) with a `LogAsserts` test; `@NullMarked` applies in a module that declares JSpecify (`pm-operator`, `pm-mock-runtime`); until `PLAN-77` has landed the test-scope CUI dependencies are missing in `pm-api`, `pm-exec`, `pm-relay` and `pm-mock-runtime`, and a test there follows plain JUnit and reports the departure, never adds the dependency itself.
- Frontend standards (`CLAUDE.md`, `PLAN-80`): `pm-dev-frontend` (`javascript`, `css`, `jest-testing`, `lint-config`, `javascript-security`) and `pm-dev-frontend-cui` (`cui-javascript-project`) apply to `pm-web-app` and `pm-web-e2e`; text from the runtime or from a model reaches the DOM through text-treating sinks only (no `innerHTML`, `outerHTML`, `insertAdjacentHTML`, `{@html}`); a test waits on a condition, never on an arbitrary timer; the tests run on Vitest with Testing Library and the `jest-testing` standards apply as method.
- Ports of this plan: mock web port `20450`, app port `20451` (`AGENTS.md` § Ports, made passable by `shared-port-assignment`, PLAN-76). Pass them to every mock, app and e2e start; other plans may run at the same time, so never stop or reuse a process this worktree did not start, and an assigned port that is in use is a blocker, never a reason to take another port.
- Every view this plan touches uses the design system v2 components (VD-S-06): no plain text button, no acting link, no state pill, no uppercase label, no emoji, no own page width.
- Every server value on a touched view passes through the display layer and the formatter of WS-02.
- Views this plan touches pass the text guard of `web-text-guard-report`: the baseline file may only shrink, never grow.
- The design document of `doc/design/` that this plan changes is updated in the same pull request (UX-S-13, VD-S-09).
- Browser check of the rendered output, with the Playwright MCP server of `opencode.json`, following `AGENTS.md` § Browser checks: start the mock (`./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario <scenario> --web-port 20450"`), then `PM_MOCK_WEB_PORT=20450 ./pm-web-app/start.sh -p 20451` (read the port from `pm-web-app/target/pm-web-app.port`); the MCP browser starts with an empty profile, so pair again every session with a fresh single-use code (`POST http://127.0.0.1:20450/mock/pairing`, then open `<app>/pair#code=<CODE>`), never through the `Network:` URL; reach a state with `POST /mock/goto/<state>` and a reload. Check each target in `de` and `en`, dark and light, at 1440 × 900 and 390 × 844 (`browser_resize`). Read the page through `browser_snapshot` (the accessibility tree is the evidence of text and structure) and keep one `browser_take_screenshot` per target; the server writes to its `--output-dir` `.plan/temp/playwright-mcp/` (`opencode.json`), so name every file `{plan}-{target}-{before|after}` and never write to the repository root. Take a baseline snapshot BEFORE the change and one AFTER, and record both paths in the progress record. The visible text of a checked view shows no raw enum, state id, `{placeholder}`, ISO time or emoji outside model-written blocks. Stop the app (`./pm-web-app/stop.sh`) and the mock afterwards. If the Playwright MCP server is not available in the run, the browser Done-when clauses are NOT met — a blocker reported as such, never replaced by a unit test or skipped.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/ux/plan.adoc (UX-S-06)`, `doc/claude_review/ux/proposals.adoc#UX-P-08` — read at `doc/claude_review/ux/plan.adoc`
  - verdict: corroborated | checked_at: 0913d6f3 | by: ui-improvements/analyze | rescoped: n/a | evidence: ux/plan.adoc:47-49 UX-S-06; proposals.adoc:314-322 UX-P-08
- Re-grounded note at HEAD 5f9e60da (2026-10-31): `web-display-layer` (PLAN-09, shipped, PR #287) landed `displayError(code)` (tested, both languages, the unknown fallback) and the enum guard, and `shared-error-texts` (PLAN-86, shipped) the `error.<code>.*` texts; `displayError` has no production caller yet, so `ErrorState` is its first one. The loading texts (`<area>.loading`) are this plan's, not PLAN-09's. The enum guard fails on a field of `enumFields.ts` printed without `displayState`: an error view must not print a code any other way.
  - verdict: corroborated | checked_at: 0913d6f3 | by: ui-improvements/analyze | rescoped: n/a | evidence: display/index.ts:115 displayError, no production caller; error.en.properties:7-185
- Re-grounded note at HEAD 94bbefe1 (cleanup 2026-11-02): ErrorState.svelte and Skeleton.svelte now exist (PLAN-35, #302); ErrorState does not yet call displayError (grep: only lib/display/index.ts and its test), so D1 adapts an existing component and is its first caller. PLAN-90/#291 touched every view and client.ts (time format, e2e baseline): re-read.
  - verdict: corroborated | checked_at: 0913d6f3 | by: ui-improvements/analyze | rescoped: n/a | evidence: ErrorState/Skeleton exist, ErrorState has one message prop and no displayError; used only by ReportsView:922,925 and the showroom
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.
  - verdict: corroborated | checked_at: 0913d6f3 | by: ui-improvements/analyze | rescoped: n/a | evidence: verify-first procedure holds; refutations (no timeout/network codes in the closed error catalogue, plan_not_found mock-only, one-sentence ErrorState, Retry key, five 15 s constants) need operator decisions before emit

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/api/client.ts` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/api/client.test.ts` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/views/` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-e2e/tests/chrome.spec.js` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/common.en.properties` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/common.de.properties` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/resource/ErrorTextsTest.java` — present at HEAD 1b6c6ae3 (created by `PLAN-86`, which owns the completeness check of the error texts: every `ApiErrorCode` member has title, cause and fix in both languages); this plan builds no second one
- OBSERVED: `pm-web-app/src/lib/components/ErrorState.svelte` — present at HEAD aa3a9be3
- OBSERVED: `pm-web-app/src/lib/components/Skeleton.svelte` — present at HEAD aa3a9be3
- OBSERVED: `pm-web-app/src/lib/views/SettingsView.svelte` — present at HEAD aa3a9be3
- OBSERVED: `pm-web-app/src/routes/+layout.svelte` — present at HEAD aa3a9be3
- OBSERVED: `pm-web-app/src/lib/display/index.ts` — present at HEAD aa3a9be3
- OBSERVED: `doc/design/web-app/components.adoc` — present at `0913d6f3` (added by the re-grounding of 2026-11-10)
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/resource/ApiErrorCode.java` — present at `0913d6f3` (added by the re-grounding of 2026-11-10)
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` — present at `0913d6f3` (added by the re-grounding of 2026-11-10)
- OBSERVED: `pm-web-app/src/lib/components/ErrorState.test.ts` — present at `0913d6f3` (added by the re-grounding of 2026-11-10)
- OBSERVED: `pm-web-app/src/lib/components/Skeleton.test.ts` — present at `0913d6f3` (added by the re-grounding of 2026-11-10)
- OBSERVED: `pm-web-app/src/lib/display/index.test.ts` — present at `0913d6f3` (added by the re-grounding of 2026-11-10)
- OBSERVED: `pm-web-app/src/lib/i18n/catalogue.test.ts` — present at `0913d6f3` (added by the re-grounding of 2026-11-10)
- OBSERVED: `pm-web-app/src/lib/views/ListenerOffView.svelte` — present at `0913d6f3` (added by the re-grounding of 2026-11-10)
- OBSERVED: `pm-web-app/src/lib/views/PlanView.svelte` — present at `0913d6f3` (added by the re-grounding of 2026-11-10)
- OBSERVED: `pm-web-app/src/lib/views/ReportsView.test.ts` — present at `0913d6f3` (added by the re-grounding of 2026-11-10)
- OBSERVED: `pm-web-app/src/lib/views/SettingsView.test.ts` — present at `0913d6f3` (added by the re-grounding of 2026-11-10)
- OBSERVED: `pm-web-app/src/routes/layout.test.ts` — present at `0913d6f3` (added by the re-grounding of 2026-11-10)
- OBSERVED: `pm-web-app/src/routes/dev/components/+page.svelte` — present at `0913d6f3` (added by the re-grounding of 2026-11-10)
- OBSERVED: `pm-web-e2e/tests/text-guard-baseline.json` — present at `0913d6f3` (added by the re-grounding of 2026-11-10)
- OBSERVED: `pm-web-e2e/tests/error-pattern.spec.js` — absent at `0913d6f3`; created by this plan (the not-found and stopped-mock paths)

## Dependencies and Sequencing

- Depends on: `PLAN-35` (`web-data-components`), `PLAN-39` (`web-app-shell`, shipped), `PLAN-76` (`shared-port-assignment`, shipped), `PLAN-86` (`shared-error-texts`), `PLAN-09` (`displayError`)
- Overlaps with: `PLAN-105` (reports view and row menu; whichever starts second rebases)
- Adjacent to: none

## Design Review 2026-11-06 (Amendment)

Source: `inbox/archive/design-review/design-review-010.md` § Per-plan instructions (messages 008, 009). **Waits for nothing
new**; queue status stays `staged`. Field, form and chat errors use the same `displayError` codes (PLAN-09) as this plan's
load errors, so the codes this plan settles are the ones the field system (`Field` … `FormStatus`) and the `Chat`
component consume; keep the code set closed and documented where those later plans can import it. No Expected Surface change.

## Re-grounding 2026-11-07 (cleanup)

Checked at `aa3a9be3`. Deliverable 5's list of six time-limit constants needs correcting: `STATE_REFRESH_TIMEOUT_MS` in `+layout.svelte:80` is 5 s, not 15 s, and is not one of them; the 15 s limits are `client.ts:2810,2975`, `SettingsView:41`, `+layout.svelte:121,148`. `ErrorState.svelte` has no `displayError` call; `lib/api/client.ts` has no `timeout` code; `ReportsView.svelte:900-901` still swaps the whole list for a `Skeleton` while `isLoading` (the spec's ~67-70 is outdated, `loadReports` is at :386); `revokeTimedOut` (`SettingsView:171`) still omits `network_offline`. PLAN-111 and PLAN-106 touched `+layout.svelte` and the withdraw paths: re-read before outline.

## Amendment 2026-11-10 (re-grounding at `0913d6f3`; operator decisions of 2026-11-10)

This section supersedes the spec text, the 2026-11-06 amendment and the 2026-11-07 note where they differ. All
dependencies (PLAN-35, -39, -76, -86, -09) and PLAN-107 have landed.

**Operator decisions (2026-11-10, asked one by one):**

1. **Client codes get a client key area.** The client's own failures form a closed client code set — `timeout`,
   `network_offline`, `unauthorized` — exported from one module the views import, with texts
   `common.error.<code>.title`, `.cause`, `.fix` in en and de. The daemon's `error.*` catalogue and `ApiErrorCode` stay
   closed (`ErrorTextsTest` unchanged). `displayError` resolves a client code from the client area and a daemon code
   from `error.*`; an unknown code falls back as today. `client.ts` throws these codes as typed errors, never
   `Error('network_offline')` (`:3120`) / `Error('unauthorized')` (`:3130`) with the message printed raw.
2. **The mock answers the real not-found codes.** `MockRuntimeEngine.java:349,493` answer `scope_not_found` and
   `project_not_enrolled` (which have texts) instead of `plan_not_found` / `project_not_found`; `PlanView.svelte:198-201`
   drops its own `plan.view.not_found` mapping and shows the `ErrorState` of `scope_not_found`; the mock tests that pin
   the old codes (`PlanDetailTest` and the like) follow. No new `ApiErrorCode` member.
3. **ErrorState keeps the AGREED one-sentence form** (`components.adoc` VD-P-05): title = `title`, the one sentence =
   `fix`, the one act; the `cause` and the code fold under _Details_ with a new shared key `common.action.details`
   (en "Details", de "Details"), drawn for every error. `ErrorState` calls `displayError` itself (or takes its result);
   `components.adoc` § ErrorState states the mapping in the same PR.
4. **"Try again", per the design** (`texts-and-languages.adoc:94,136`): add `common.action.retry` (en "Try again",
   de "Erneut versuchen"), move all eight views from `web.common.retry` to it, retire `web.common.retry`, update
   `catalogue.test.ts:384`. The e2e clause asserts "Try again".

**Corrections from the re-grounding:**

- **Loading texts (D3):** most `<area>.loading` keys already exist (`report.list.loading`, `plan.view.loading`,
  `epic.view.loading`, `system.view.loading`, `decisions.list.loading`, …); wire them to `Skeleton` and add only the
  missing ones. Today only `ReportsView` uses `ErrorState`/`Skeleton`; `PlanView.svelte:633-635`, `StartView.svelte:153`,
  `CleanupView`, `EpicView`, `ProjectExplorerView`, `DecisionView` print their own error lines.
- **D4 (reload without a Skeleton flash):** `ReportsView.svelte:386-387` and `:920-923` (the "~67-70" and ":900-901"
  references are stale). Since PLAN-107 (#394) every mark-all reloads the first page (`ReportsView.test.ts:370`), so the
  flash is hit on every mark-all; the refresh keeps the list and shows a quiet in-place cue. Overlap note: PLAN-105 and
  PLAN-107 have landed; re-read `ReportsView` before editing.
- **"30 s for a check" (D2):** the checks are `checkDecomposition` (`client.ts:2454`), `checkLessonPremise` (`:2477`),
  `checkUpgrade` (`:3029`), `getEpicArchiveCheck` (`:3179`); every other request 15 s. Streams and long-lived calls
  (the SSE connect, `CONNECT_TIMEOUT_MS` `:3927`) are out of scope.
- **D5 constants:** five, not six — `client.ts:2810` `REVOKE_TIMEOUT_MS`, `client.ts:2975` `WITHDRAW_TIMEOUT_MS`,
  `SettingsView.svelte:41` `LOAD_TIMEOUT_MS`, `+layout.svelte:121` `WITHDRAW_TIMEOUT_MS`, `+layout.svelte:148`
  `STOP_NOW_TIMEOUT_MS`; `+layout.svelte:80` `STATE_REFRESH_TIMEOUT_MS` (5 s) stays as is. The Done-when grep becomes
  `git grep -n "15_000" -- pm-web-app/src ':!*.test.ts'` naming only the client's limit and `ListenerOffView.svelte:15`
  (a retry interval, not a request limit). The tests asserting `AbortSignal.timeout(15_000)` (`client.test.ts:599`,
  `SettingsView.test.ts:289`, `layout.test.ts:404`) and the two `withdrawStop()` calls without a signal
  (`client.test.ts:296,596`) are rewritten with the fallback's removal.
- **Truncated body (D5):** the unguarded `res.json()` sites are `client.ts:3139`, `:2192` (`pair`) and `:3579`; all three
  end in a typed client error.
- **Skills:** D1 is a Svelte/TS component — `pm-dev-frontend:javascript`, `pm-dev-frontend:jest-testing`,
  `pm-dev-frontend:css` (not the Java list).
- **Not split:** five deliverables stay; the decisions above remove the open questions that made D1/D3 heavy.
  Rationale recorded by the orchestrator.
- **Sequencing:** PLAN-123 (terminal) and PLAN-45 both add `common.*` keys; rebase whichever lands second.

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-45-web-error-loading-pattern.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
