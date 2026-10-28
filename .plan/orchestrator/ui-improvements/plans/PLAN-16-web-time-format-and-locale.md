# PLAN-16: Web: One Time Format and a Full Locale Switch

epic: ui-improvements
workstream: WS-02

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

One formatter for time, duration, tokens and bytes on the shared rule, every raw time and inline number format replaced, a locale switch that refreshes every text, and the recorded interface language applied on first load (CC-S-04; CC-P-04, CC-P-05, VD-P-07). The formatting helpers of VD-S-04 are built here, once.

Source of this plan (read before scoping):

- `doc/claude_review/cross-cutting/plan.adoc#CC-S-04`
- `doc/claude_review/cross-cutting/proposals.adoc#CC-P-04`
- `doc/claude_review/cross-cutting/proposals.adoc#CC-P-05`
- `doc/claude_review/visual-design/proposals.adoc#VD-P-07`
- `doc/claude_review/cross-cutting/findings.adoc#CC-F-10`

## Deliverables

1. **Formatter**
   - What: `lib/display/format.ts`: `formatTime(iso, style)` with `relative`, `short`, `clock` through `Intl`, `formatDuration`, `formatTokens(n, floor)`, `formatBytes`; replaces `lib/utils/tokenFormat.ts`.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
   - Done when:
     - Table tests per style and locale, following `shared/texts-and-languages.adoc` (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `fully-onboarded`: `/reports`, overview, plan activity, paired devices show one time format with the absolute time on hover; switch `de` → `en` → `de` without reload; `onboarding` → `language.choose`; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
2. **Every view formatted**
   - What: Lists use `relative` with `short` on hover; details use `short`; the activity stream uses `clock` with the date on the first row of each day.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
   - Done when:
     - The text-guard baseline holds no ISO time (evidence order: `none`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `fully-onboarded`: `/reports`, overview, plan activity, paired devices show one time format with the absolute time on hover; switch `de` → `en` → `de` without reload; `onboarding` → `language.choose`; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
3. **Locale switch and recorded language**
   - What: `setLocale` replaces the cached catalogue only after the new one loaded; the cache key includes the catalogue version of `/api/v1/status`; summaries are catalogue messages with plural rules; the recorded interface language sets the locale on first load.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
   - Done when:
     - A unit test switches `de` → `en` → `de` and finds no word of the other locale in a summary (evidence order: `test-first`)
     - An e2e check: after onboarding with "Deutsch for both", a fresh browser opens in German (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `fully-onboarded`: `/reports`, overview, plan activity, paired devices show one time format with the absolute time on hover; switch `de` → `en` → `de` without reload; `onboarding` → `language.choose`; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-web-app verify`, `PM_MOCK_WEB_PORT=20160 PM_WEB_PORT=20161 ./mvnw -pl pm-web-e2e verify -Pe2e`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- Ports of this plan: mock web port `20160`, app port `20161` (`AGENTS.md` § Ports, made passable by `shared-port-assignment`, PLAN-76). Pass them to every mock, app and e2e start; other plans may run at the same time, so never stop or reuse a process this worktree did not start, and an assigned port that is in use is a blocker, never a reason to take another port.
- Views this plan touches pass the text guard of `web-text-guard-report`: the baseline file may only shrink, never grow.
- Browser check of the rendered output, with the Playwright MCP server of `opencode.json`, following `AGENTS.md` § Browser checks: start the mock (`./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario <scenario> --web-port 20160"`), then `PM_MOCK_WEB_PORT=20160 ./pm-web-app/start.sh -p 20161` (read the port from `pm-web-app/target/pm-web-app.port`); the MCP browser starts with an empty profile, so pair again every session with a fresh single-use code (`POST http://127.0.0.1:20160/mock/pairing`, then open `<app>/pair#code=<CODE>`), never through the `Network:` URL; reach a state with `POST /mock/goto/<state>` and a reload. Check each target in `de` and `en`, dark and light, at 1440 × 900 and 390 × 844 (`browser_resize`). Read the page through `browser_snapshot` (the accessibility tree is the evidence of text and structure) and keep one `browser_take_screenshot` per target; the server writes to its `--output-dir` `.plan/temp/playwright-mcp/` (`opencode.json`), so name every file `{plan}-{target}-{before|after}` and never write to the repository root. Take a baseline snapshot BEFORE the change and one AFTER, and record both paths in the progress record. The visible text of a checked view shows no raw enum, state id, `{placeholder}`, ISO time or emoji outside model-written blocks. Stop the app (`./pm-web-app/stop.sh`) and the mock afterwards. If the Playwright MCP server is not available in the run, the browser Done-when clauses are NOT met — a blocker reported as such, never replaced by a unit test or skipped.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/cross-cutting/plan.adoc#CC-S-04`, `doc/claude_review/cross-cutting/proposals.adoc#CC-P-04` — read at `doc/claude_review/cross-cutting/plan.adoc`
  - verdict: corroborated | checked_at: cc5794c9 | by: ui-improvements/cleanup | rescoped: n/a | evidence: doc/claude_review/cross-cutting/plan.adoc:34-38 states CC-S-04 (CC-P-04, CC-P-05); proposals.adoc:46 [#CC-P-04] and :58 [#CC-P-05] exist
- HYPOTHESIS: `lib/i18n/index.ts` caches catalogues in `localStorage` (`pm_i18n_messages_<locale>`) and a stale cache causes CC-F-10 — confirm/refute at `pm-web-app/src/lib/i18n/index.ts` (verify-at-outline)
  - verdict: unverifiable | checked_at: cc5794c9 | by: ui-improvements/cleanup | rescoped: n/a | evidence: pm-web-app/src/lib/i18n/index.ts:9 pm_i18n_messages_ and :169 localStorage hold; that the stale cache causes CC-F-10 needs a runtime repro (findings.adoc:172 itself says 'likely')
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/display/format.ts` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-web-app/src/lib/display/format.test.ts` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-web-app/src/lib/i18n/index.ts` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/i18n/index.test.ts` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/utils/tokenFormat.ts` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/utils/tokenFormat.test.ts` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/views/` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/components/` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-e2e/tests/text-guard-baseline.json` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-web-e2e/tests/onboarding.spec.js` — present at HEAD c4dc9bb4

## Dependencies and Sequencing

- Depends on: `PLAN-06` (`shared-display-vocabulary`), `PLAN-09` (`web-display-layer`), `PLAN-76` (`shared-port-assignment`)
- Overlaps with: touches most views; runs after `web-display-layer` (same views)
- Adjacent to: none

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-16-web-time-format-and-locale.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
