# PLAN-53: Web: Start as a Wizard With a Working Gate

epic: ui-improvements
workstream: WS-03

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Make starting work a wizard (UX-P-07) with one primary act per step, Back on every step, a setup gate with progress and a time limit, the error pattern for an unknown project, and a lazy entry (UX-S-10, start part of UC-S-09; UC-P-07).

Source of this plan (read before scoping):

- `doc/claude_review/ux/plan.adoc (UX-S-10)`
- `doc/claude_review/ux/proposals.adoc#UX-P-07`
- `doc/claude_review/use-cases/proposals.adoc#UC-P-07`
- `doc/claude_review/use-cases/findings-projects-starting.adoc`
- `doc/claude_review/ux/findings.adoc#UX-F-12`

## Deliverables

1. **Gate**
   - What: "Checking the project's setup…" with a spinner; after 20 s the cause and "Try again"; a refused gate with its reason and act; a start for an unknown project shows the error pattern with "Choose a project".
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript
   - Done when:
     - `start.spec.js`: a gate that does not answer shows its cause after 20 s; `/start` without a project (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `projects-and-start` → `start.sources`, `start.refused`; `/start` without a project; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
2. **Steps 1 to 3**
   - What: What (two cards), Sources (source cards with state; one "Add a source" menu; the description as a source card with one "Add"), Ready (title, id, sources, what happens next; primary "Start the plan"); footer Back · Discard (overflow) · primary.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript
   - Done when:
     - `start.spec.js`, `git-sources.spec.js`, `lesson-source.spec.js`: one primary per step; Back works (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `projects-and-start` → `start.sources`, `start.refused`; `/start` without a project; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
3. **Lazy entry**
   - What: The entry is created when the first source is added; leaving without a source leaves no entry.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
   - Done when:
     - A unit test of `lib/stores/start.ts`; e2e on `projects-and-start` → `start.sources` (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `projects-and-start` → `start.sources`, `start.refused`; `/start` without a project; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-web-app verify`, `PM_MOCK_WEB_PORT=20530 PM_WEB_PORT=20531 ./mvnw -pl pm-web-e2e verify -Pe2e`, `./mvnw -pl pm-api verify`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- Ports of this plan: mock web port `20530`, app port `20531` (`AGENTS.md` § Ports, made passable by `shared-port-assignment`, PLAN-76). Pass them to every mock, app and e2e start; other plans may run at the same time, so never stop or reuse a process this worktree did not start, and an assigned port that is in use is a blocker, never a reason to take another port.
- Every view this plan touches uses the design system v2 components (VD-S-06): no plain text button, no acting link, no state pill, no uppercase label, no emoji, no own page width.
- Every server value on a touched view passes through the display layer and the formatter of WS-02.
- Views this plan touches pass the text guard of `web-text-guard-report`: the baseline file may only shrink, never grow.
- The design document of `doc/design/` that this plan changes is updated in the same pull request (UX-S-13, VD-S-09).
- Browser check of the rendered output, with the Playwright MCP server of `opencode.json`, following `AGENTS.md` § Browser checks: start the mock (`./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario <scenario> --web-port 20530"`), then `PM_MOCK_WEB_PORT=20530 ./pm-web-app/start.sh -p 20531` (read the port from `pm-web-app/target/pm-web-app.port`); the MCP browser starts with an empty profile, so pair again every session with a fresh single-use code (`POST http://127.0.0.1:20530/mock/pairing`, then open `<app>/pair#code=<CODE>`), never through the `Network:` URL; reach a state with `POST /mock/goto/<state>` and a reload. Check each target in `de` and `en`, dark and light, at 1440 × 900 and 390 × 844 (`browser_resize`). Read the page through `browser_snapshot` (the accessibility tree is the evidence of text and structure) and keep one `browser_take_screenshot` per target; the server writes to its `--output-dir` `.plan/temp/playwright-mcp/` (`opencode.json`), so name every file `{plan}-{target}-{before|after}` and never write to the repository root. Take a baseline snapshot BEFORE the change and one AFTER, and record both paths in the progress record. The visible text of a checked view shows no raw enum, state id, `{placeholder}`, ISO time or emoji outside model-written blocks. Stop the app (`./pm-web-app/stop.sh`) and the mock afterwards. If the Playwright MCP server is not available in the run, the browser Done-when clauses are NOT met — a blocker reported as such, never replaced by a unit test or skipped.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/ux/plan.adoc (UX-S-10)`, `doc/claude_review/ux/proposals.adoc#UX-P-07` — read at `doc/claude_review/ux/plan.adoc`
  - verdict: corroborated | checked_at: cc5794c9 | by: ui-improvements/cleanup | rescoped: n/a | evidence: origin/main doc/claude_review/ux/plan.adoc:68 UX-S-10 (Start as a wizard); ux/proposals.adoc:284 [#UX-P-07] exists
- HYPOTHESIS: the start view hangs on "checking" after a jump to a `start.*` state (UC-F-36) because the gate has no time limit — confirm/refute at `pm-web-app/src/lib/stores/start.ts` (verify-at-outline)
  - verdict: corroborated | checked_at: cc5794c9 | by: ui-improvements/cleanup | rescoped: n/a | evidence: No gate time limit: start.ts (58 lines) has no gate; StartView.svelte:120-160 load() has no timeout; client.ts:2917-2945 request() fetch has no signal. Cause sits in StartView/client, not start.ts
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/views/StartView.svelte` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/components/SourceList.svelte` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/components/SourceIntakePanel.svelte` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/components/RecipeSelector.svelte` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/stores/start.ts` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-e2e/tests/start.spec.js` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-e2e/tests/git-sources.spec.js` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-e2e/tests/lesson-source.spec.js` — present at HEAD c4dc9bb4
- OBSERVED: `doc/design/shared/starting-work.adoc` — present at HEAD c4dc9bb4
- OBSERVED: `doc/design/shared/starting-work-texts.adoc` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/start.en.properties` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/start.de.properties` — present at HEAD c4dc9bb4

## Dependencies and Sequencing

- Depends on: `PLAN-43` (`web-page-templates`), `PLAN-45` (`web-error-loading-pattern`), `PLAN-76` (`shared-port-assignment`)
- Overlaps with: none known at staging
- Adjacent to: none

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-53-web-start-wizard.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
