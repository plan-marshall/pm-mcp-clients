# PLAN-90: Web: Green e2e Baseline and the Time Residue of PLAN-16

epic: ui-improvements
workstream: WS-03

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.
> Staged 2026-11-01 from the open findings of the landing analyses. One pull request on purpose: each pull request costs a
> CodeRabbit review (quota-limited), so the open aspects of one area are collected here. Keep the pull request under 100 files
> (CodeRabbit refuses more); if it would exceed that, split it by deliverable and say so in the progress record.

## Objective

Make the web e2e suite green on `main` and finish what the time-format plan left: seven e2e tests fail before any change of a pull request (named by three lane runs), which blocks the first Done-when of the accessibility baseline and hides new failures, and the formatter of `PLAN-16` keeps its words and units in code and leaves keys unused. One pull request, because each pull request costs a CodeRabbit review.

## Deliverables

1. **The e2e suite on `main` is green**
   - What: The seven e2e tests that fail on `main` before any change of the pull request are diagnosed one by one, as code wrong or test wrong, and fixed where the design says the fault is. The names (reported by the runs of `PLAN-46`, `PLAN-37` and `PLAN-46`'s follow-up): `epic-view.spec.js` `writes the tokens as a floor with its coverage and the split per plan` (:190, the token split reads `0.34 M` where the spec expects `340 k`), `plan-view.spec.js` `views plan process tree, task activity, and off-track indicator`, `navigates to epic view, verifies running and staged plans, and returns` and `discloses the token breakdown by phase and role`, `showroom.spec.js` `walks the six-phase process tree of the running plan`, and the text guard for `plan` in `de` and `en`. The first step re-measures them on the current `main`; a name that passes now is reported and not touched. A text-guard baseline (`pm-web-e2e/tests/text-guard-baseline.json`) only shrinks.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - The list of failing names at the base is recorded first, with the log path; each fault is classified in the progress record as code wrong or test wrong, with the design sentence that decides it (evidence order: `test-first`)
     - `PM_MOCK_WEB_PORT=20900 PM_WEB_PORT=20901 ./mvnw -pl pm-web-e2e verify -Pe2e` reports no failure (evidence order: `none`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `running-work`: the epic view token split and the plan view process tree, in `de` and `en`; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
2. **The time and unit residue of PLAN-16**
   - What: The framing words and unit marks that live in `lib/display/format.ts` move to catalogue keys (`common.time.*`, `common.{en,de}.properties`); `relative` has the `since 14:02` form the design names; the activity day header exists where a row has a date even when the wire gives none; the unused `project.time.*` keys of `projects.{en,de}.properties` are either read by the formatter or removed.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - `format.test.ts` has one case per form (framing word, unit mark, `since`, day header) and each fails before the change (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `running-work`: the overview, the plan view and the activity list in `de` and `en`, no raw key and no ISO time (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

3. **The words of the system view's model column and its load failure**
   - What: `lib/views/SystemView.svelte` states two things in English on every page: the model column's status words (`retry`/`retries`, `error`/`errors`, `ok`, lines 1107, 1113 and 1118 at `be4995e5`) are literals in the markup, and the load failure text is read once (`loadError = $t('system.view.load_failed')`, line 148) and kept as a string, so it keeps the language it had when the error occurred and shows the key when the catalogue has not arrived. The words come from the catalogue (the terminal view already has `system.models.error` and `system.models.retry` in `system.{en,de}.properties`; reuse them and add `system.models.ok` where none exists), and the load failure is kept as a state (a flag) and read through `$t` where it renders. This file is also the reference view of `PLAN-43` (running): take the current `main` before the first edit, change only these lines, and rebase when `PLAN-43` lands first.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - `SystemView.test.ts` has one case per word (retry, error, ok in `de`) and one for the load failure (switch the language after a failed load: the text follows) and each fails before the change (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `fully-onboarded`: `/system`, the models table in `de` and `en`; baseline and after snapshots plus screenshots recorded (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-web-app verify`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- Frontend standards (`CLAUDE.md`, `PLAN-80`): `pm-dev-frontend` (`javascript`, `css`, `jest-testing`, `lint-config`, `javascript-security`) and `pm-dev-frontend-cui` (`cui-javascript-project`) apply to `pm-web-app` and `pm-web-e2e`; text from the runtime or from a model reaches the DOM through text-treating sinks only (no `innerHTML`, `outerHTML`, `insertAdjacentHTML`, `{@html}`); a test waits on a condition, never on an arbitrary timer; the tests run on Vitest with Testing Library and the `jest-testing` standards apply as method.
- Ports of this plan: mock web port `20900`, app port `20901` (`AGENTS.md` § Ports, made passable by `shared-port-assignment`, PLAN-76). Pass them to every mock, app and e2e start; other plans may run at the same time, so never stop or reuse a process this worktree did not start, and an assigned port that is in use is a blocker, never a reason to take another port.
- Every view this plan touches uses the design system v2 components (VD-S-06): no plain text button, no acting link, no state pill, no uppercase label, no emoji, no own page width.
- Every server value on a touched view passes through the display layer and the formatter of WS-02.
- Views this plan touches pass the text guard of `web-text-guard-report`: the baseline file may only shrink, never grow.
- The design document of `doc/design/` that this plan changes is updated in the same pull request (UX-S-13, VD-S-09).
- Browser check of the rendered output, with the Playwright MCP server of `opencode.json`, following `AGENTS.md` § Browser checks: start the mock (`./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario <scenario> --web-port 20900"`), then `PM_MOCK_WEB_PORT=20900 ./pm-web-app/start.sh -p 20901` (read the port from `pm-web-app/target/pm-web-app.port`); the MCP browser starts with an empty profile, so pair again every session with a fresh single-use code (`POST http://127.0.0.1:20900/mock/pairing`, then open `<app>/pair#code=<CODE>`), never through the `Network:` URL; reach a state with `POST /mock/goto/<state>` and a reload. Check each target in `de` and `en`, dark and light, at 1440 × 900 and 390 × 844 (`browser_resize`). Read the page through `browser_snapshot` (the accessibility tree is the evidence of text and structure) and keep one `browser_take_screenshot` per target; the server writes to its `--output-dir` `.plan/temp/playwright-mcp/` (`opencode.json`), so name every file `{plan}-{target}-{before|after}` and never write to the repository root. Take a baseline snapshot BEFORE the change and one AFTER, and record both paths in the progress record. The visible text of a checked view shows no raw enum, state id, `{placeholder}`, ISO time or emoji outside model-written blocks. Stop the app (`./pm-web-app/stop.sh`) and the mock afterwards. If the Playwright MCP server is not available in the run, the browser Done-when clauses are NOT met — a blocker reported as such, never replaced by a unit test or skipped.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the seven failing names above are the ones three lane runs recorded on `main` with the same text (`landings/PLAN-46.md`, the PLAN-37 observation `rb-37-web-accessibility-baseline-001`, the PLAN-46 follow-up message); they were not re-run by the orchestrator — read in the landing reports
- OBSERVED: `epic-view.spec.js:190` expects `≥ 3.86 M`, `3.52 M in` and `340 k out` — read at `pm-web-e2e/tests/epic-view.spec.js`
- HYPOTHESIS: the `epic-view.spec.js` mismatch is `PLAN-16`'s formatter choosing `M` for 340000, so the test or the design's number rule is stale — confirm/refute at `pm-web-app/src/lib/display/format.ts` and the number rule in `doc/design/shared/texts-and-languages.adoc` (verify-at-outline)
- HYPOTHESIS: the `plan-view.spec.js`, `showroom.spec.js` and text-guard failures are older breadcrumb and text drift, not one cause — confirm/refute per test (verify-at-outline)
- OBSERVED: `project.time.just_now`, `project.time.minutes` and `project.time.hours` exist in `projects.en.properties` (:158-160); whether anything reads them is the PLAN-16 run's account — confirm by `git grep` (verify-at-outline)
- Source: `landings/PLAN-16.md` (residue), the Watch "Pre-existing red tests on `main`" of `epic.md`.
- Not in this plan, the operator's call or another plan's: `npm run format:check` on 42 files (touches most views, after the view plans), the dark-half of the contrast audit (`PLAN-85`, `PLAN-70`), the `waiting for_you` lead of `SystemView` below.
- Four lane reports about `SystemView.svelte` (PLAN-17 and PLAN-26 runs), read by the orchestrator at `be4995e5` (2026-11-01), not run: (a) the model column writes `retry`/`retries`, `error`/`errors` and `ok` as literals (:1107, :1113, :1118): CONFIRMED, deliverable 3; (b) the load failure is stored as a string at the time of the error (:148), so it does not follow a language switch: CONFIRMED as a mechanism, deliverable 3 (whether it shows English on the German page depends on the catalogue state at that moment, so the test switches the language after the failure); (c) the tokens read `0.12 M` where `120 k` is the rule: REFUTED for this file at `lib/display/format.ts` (`tokenUnit` gives `120 k` below a million); the `0.34 M` of the `epic-view.spec.js:190` failure is a separate path that deliverable 1 re-measures; (d) a raw `waiting for_you` state id from a `replace` call: NOT reproduced (no `replace(` call in the file), left out.
- Verify-first clause: before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-web-e2e/tests/epic-view.spec.js` — present at HEAD 1ce32d80
- OBSERVED: `pm-web-e2e/tests/plan-view.spec.js` — present at HEAD 1ce32d80
- OBSERVED: `pm-web-e2e/tests/showroom.spec.js` — present at HEAD 1ce32d80
- OBSERVED: `pm-web-e2e/tests/theme-readability.spec.js` — present at HEAD 1ce32d80
- OBSERVED: `pm-web-e2e/tests/text-guard-baseline.json` — present at HEAD 1ce32d80
- OBSERVED: `pm-web-app/src/lib/display/format.ts` — present at HEAD 1ce32d80
- OBSERVED: `pm-web-app/src/lib/display/format.test.ts` — present at HEAD 1ce32d80
- OBSERVED: `pm-api/src/main/resources/messages/common.en.properties` — present at HEAD 1ce32d80
- OBSERVED: `pm-api/src/main/resources/messages/common.de.properties` — present at HEAD 1ce32d80
- OBSERVED: `pm-api/src/main/resources/messages/projects.en.properties` — present at HEAD 1ce32d80
- OBSERVED: `pm-api/src/main/resources/messages/projects.de.properties` — present at HEAD 1ce32d80
- OBSERVED: `pm-web-app/src/lib/views/SystemView.svelte` — present at HEAD be4995e5
- OBSERVED: `pm-web-app/src/lib/views/SystemView.test.ts` — present at HEAD be4995e5
- OBSERVED: `pm-api/src/main/resources/messages/system.en.properties` — present at HEAD be4995e5
- OBSERVED: `pm-api/src/main/resources/messages/system.de.properties` — present at HEAD be4995e5

## Dependencies and Sequencing

- Depends on: `PLAN-16` (shipped)
- Overlaps with: `PLAN-35` (running; check `lib/display/` and `common.*` at the outline), `PLAN-37` (running; the same seven failures are the base of its third deliverable, so landing this plan clears its gate command), `PLAN-34`, `PLAN-73` (`common.*`), `PLAN-45` (`common.*`)
- Adjacent to: `PLAN-70` and `PLAN-85` (theme and the contrast audit; untouched)

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-90-web-green-baseline-and-time-residue.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
