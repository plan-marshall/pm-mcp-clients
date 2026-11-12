# PLAN-64: Web: Every Onboarding State Has a Screen

epic: ui-improvements
workstream: WS-03

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Replace the generic state card of the onboarding by a screen registry with a build check, render a state without a screen as the error pattern (never its id), and move the `entry.*` states into the settings frame (UC-S-02; UC-P-01).

Source of this plan (read before scoping):

- `doc/claude_review/use-cases/plan.adoc (UC-S-02)`
- `doc/claude_review/use-cases/proposals.adoc#UC-P-01`
- `doc/claude_review/use-cases/findings-onboarding-entry.adoc`

## Deliverables

1. **Registry**
   - What: `lib/views/onboarding/screens.ts` maps a state id or pattern (`*.consent`) to a screen; the `{:else}` chain of `OnboardingView.svelte` goes.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - `screens.registry.test.ts` reads every state of `onboarding.json` and fails for a state without a screen (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `onboarding`: every state of the scenario; `entry.lan-open`; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
2. **Missing screen at runtime**
   - What: A state without a screen shows "This screen is not available in the web app", the terminal command, and a way back.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - A unit test with an unknown state: no state id in the text (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `onboarding`: every state of the scenario; `entry.lan-open`; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
3. **Entry states in settings**
   - What: `entry.*` renders inside the settings frame; `entry.lan-open` shows the LAN URL, the QR code and the fingerprint, with no step bar.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:javascript-security
   - Done when:
     - `entry.spec.js` on `entry.lan-open` (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `onboarding`: every state of the scenario; `entry.lan-open`; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-web-app verify`, `PM_MOCK_WEB_PORT=20640 PM_WEB_PORT=20641 ./mvnw -pl pm-web-e2e verify -Pe2e`, `./mvnw -pl pm-api verify`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- Frontend standards (`CLAUDE.md`, `PLAN-80`): `pm-dev-frontend` (`javascript`, `css`, `jest-testing`, `lint-config`, `javascript-security`) and `pm-dev-frontend-cui` (`cui-javascript-project`) apply to `pm-web-app` and `pm-web-e2e`; text from the runtime or from a model reaches the DOM through text-treating sinks only (no `innerHTML`, `outerHTML`, `insertAdjacentHTML`, `{@html}`); a test waits on a condition, never on an arbitrary timer; the tests run on Vitest with Testing Library and the `jest-testing` standards apply as method.
- Ports of this plan: mock web port `20640`, app port `20641` (`AGENTS.md` § Ports, made passable by `shared-port-assignment`, PLAN-76). Pass them to every mock, app and e2e start; other plans may run at the same time, so never stop or reuse a process this worktree did not start, and an assigned port that is in use is a blocker, never a reason to take another port.
- Every view this plan touches uses the design system v2 components (VD-S-06): no plain text button, no acting link, no state pill, no uppercase label, no emoji, no own page width.
- Every server value on a touched view passes through the display layer and the formatter of WS-02.
- Views this plan touches pass the text guard of `web-text-guard-report`: the baseline file may only shrink, never grow.
- The design document of `doc/design/` that this plan changes is updated in the same pull request (UX-S-13, VD-S-09).
- Browser check of the rendered output, with the Playwright MCP server of `opencode.json`, following `AGENTS.md` § Browser checks: start the mock (`./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario <scenario> --web-port 20640"`), then `PM_MOCK_WEB_PORT=20640 ./pm-web-app/start.sh -p 20641` (read the port from `pm-web-app/target/pm-web-app.port`); the MCP browser starts with an empty profile, so pair again every session with a fresh single-use code (`POST http://127.0.0.1:20640/mock/pairing`, then open `<app>/pair#code=<CODE>`), never through the `Network:` URL; reach a state with `POST /mock/goto/<state>` and a reload. Check each target in `de` and `en`, dark and light, at 1440 × 900 and 390 × 844 (`browser_resize`). Read the page through `browser_snapshot` (the accessibility tree is the evidence of text and structure) and keep one `browser_take_screenshot` per target; the server writes to its `--output-dir` `.plan/temp/playwright-mcp/` (`opencode.json`), so name every file `{plan}-{target}-{before|after}` and never write to the repository root. Take a baseline snapshot BEFORE the change and one AFTER, and record both paths in the progress record. The visible text of a checked view shows no raw enum, state id, `{placeholder}`, ISO time or emoji outside model-written blocks. Stop the app (`./pm-web-app/stop.sh`) and the mock afterwards. If the Playwright MCP server is not available in the run, the browser Done-when clauses are NOT met — a blocker reported as such, never replaced by a unit test or skipped.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/use-cases/plan.adoc (UC-S-02)`, `doc/claude_review/use-cases/proposals.adoc#UC-P-01` — read at `doc/claude_review/use-cases/plan.adoc`
  - verdict: corroborated | checked_at: 1cc0e10ec2ca617853b99a23dcfed8e5e2fed1bb | by: ui-improvements/cleanup | rescoped: n/a | evidence: use-cases/plan.adoc:26 UC-S-02; proposals.adoc:16 UC-P-01; onboarding.adoc:42 one route renders the reported state; re-grounded vs D1-D5 2026-11-10 (Amendment)
- Re-grounded note at HEAD 94bbefe1 (cleanup 2026-11-02): screens.ts and screens.registry.test.ts still absent (created by this plan, correct); OnboardingView, SettingsView and onboarding.* catalogue changed since the anchor (PLAN-90 time/locale, a11y): re-read before D1.
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface


- OBSERVED: `pm-web-app/src/lib/views/OnboardingView.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/OnboardingView.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/OnboardingView.scenarios.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/onboarding/screens.ts` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/views/onboarding/screens.registry.test.ts` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/HarnessChoice.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/ErrorState.svelte` — present at HEAD 1cc0e10e (read; owned by PLAN-45)
- OBSERVED: `pm-web-e2e/tests/onboarding.spec.js` — present at HEAD 1cc0e10e
- OBSERVED: `pm-mock-runtime/src/main/resources/scenarios/onboarding.json` — present at HEAD 1cc0e10e (read)
- OBSERVED: `doc/design/web-app/onboarding.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/web-app/onboarding-texts.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/onboarding.en.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/onboarding.de.properties` — present at HEAD 1cc0e10e

## Dependencies and Sequencing

- Depends on: `PLAN-45` (`web-error-loading-pattern`), `PLAN-60` (`web-settings-one-place`), `PLAN-76` (`shared-port-assignment`)
- Overlaps with: none known at staging
- Adjacent to: none

## Design Review 2026-11-06 (Amendment)

Source: `inbox/archive/design-review/design-review-010.md` § Per-plan instructions (messages 005, 007, 008, 009). **Gate: D1
and D3 merged** (documents `AGREED`) and the D1 foundation / D3 brand assets packages; `web-app/onboarding.adoc` is edited by
D3 and D4 only after the running `PLAN-104` has landed. Queue status: `parked`; the orchestrator re-grounds this spec when it
is restarted. The Expected Surface above is NOT widened now (a parked row is not in the disjointness gate population). When
restarted (together with `PLAN-66`):

- Welcome and done figures (the full mascot figure; on "done" the ticked clipboard), the chosen brand icon in the header (005).
- "Abandon" in the overflow through `ConfirmDialog`; dialogs via `Dialog` (007).
- `SecretField`, `TextField`, `RadioGroup` / `CheckGroup`; the stray `/>` in `HarnessChoice.svelte:151-152` goes (008).
- The role `transcribe` in the roles step belongs to the D4 voice package, not to this plan.

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-64-web-onboarding-screen-registry.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.

- OBSERVED: `pm-web-app/src/lib/views/OnboardingView.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/OnboardingView.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/OnboardingView.scenarios.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/onboarding/screens.ts` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/views/onboarding/screens.registry.test.ts` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/HarnessChoice.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/ErrorState.svelte` — present at HEAD 1cc0e10e (read; owned by PLAN-45)
- OBSERVED: `pm-web-e2e/tests/onboarding.spec.js` — present at HEAD 1cc0e10e
- OBSERVED: `pm-mock-runtime/src/main/resources/scenarios/onboarding.json` — present at HEAD 1cc0e10e (read)
- OBSERVED: `doc/design/web-app/onboarding.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/web-app/onboarding-texts.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/onboarding.en.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/onboarding.de.properties` — present at HEAD 1cc0e10e

## Operator Decisions 2026-11-10 (Amendment)

Recorded in the epic's decision log (AskUserQuestion, 2026-11-10). They settle the open points above and supersede any text here that differs.

- **`entry.lan-open`:** no web screen; every `entry.*` state lands on the missing-screen fallback with the terminal command (deliverable 2). Former deliverable 3 stays removed.

## PLAN-130 Landing Residue (Amendment 2026-11-11)

Routed here by the operator (AskUserQuestion, 2026-11-11) from `landings/PLAN-130.md`. `onboarding.adoc:43` asks for the brand icon at
`28` px beside the wordmark in the onboarding header. At `078b2ec5` the only header carrying it is the rail head
(`NavRail.svelte:91,98`), and the rail is hidden below `768` px (`NavRail.svelte:440`); no top bar or view draws `BrandMark`, so a
phone shows the onboarding without the icon.

- Done when (added): the onboarding header shows `BrandMark` (colour, `28` px) beside the wordmark below `768` px as well, with one
  mark per screen; a component or e2e test at a phone width fails when the mark is missing.
