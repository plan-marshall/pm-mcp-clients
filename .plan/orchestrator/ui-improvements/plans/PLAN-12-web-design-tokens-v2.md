# PLAN-12: Web: Design Tokens v2 and the Inter Typeface

epic: ui-improvements
workstream: WS-02

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Introduce the tokens of design system v2 for both themes (colour, type, spacing, radius, elevation, motion), self-host Inter with tabular numerals, keep the old names as aliases, and record the operator's decisions of VD-S-00 in the design (VD-S-00, VD-S-01; VD-P-02, VD-P-03, VD-P-04).

Source of this plan (read before scoping):

- `doc/claude_review/visual-design/plan.adoc#VD-S-01`
- `doc/claude_review/visual-design/proposals.adoc#VD-P-02`
- `doc/claude_review/visual-design/proposals.adoc#VD-P-03`
- `doc/claude_review/visual-design/proposals.adoc#VD-P-04`

## Deliverables

1. **Operator decisions recorded**
   - What: Record in `doc/design/web-app/README.adoc`, with the date 2026-10-25: Inter self-hosted; the own UI icon set; axe-core in `pm-web-e2e`; stylelint in `pm-web-app` (decisions of the operator of this epic).
   - Persona: plan-marshall:persona-documenter
   - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation
   - Done when:
     - The four decisions are in the design with their date (evidence order: `none`)
2. **Tokens v2**
   - What: The colour tokens of VD-P-02 for dark and light, the type scale of VD-P-03, the spacing, radius, elevation, motion and breakpoints of VD-P-04 in `styles/tokens.css`; `base.css` on the new scale; old tokens as aliases (`--color-primary: var(--color-brand)`, …).
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
   - Done when:
     - `styles/tokens.test.ts` asserts every token of VD-P-02/03/04 in both themes and every alias (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `fully-onboarded`: every route of the rail/header (overview, plan, epic, projects, decisions, reports, designer, system, cleanup, settings) in dark and light; Inter is the rendered font (`browser_evaluate` of `getComputedStyle(document.body).fontFamily`); baseline and after snapshots plus screenshots recorded (evidence order: `none`)
3. **Inter, self-hosted**
   - What: Inter variable `woff2` in `pm-web-app/static/fonts/` with its OFL licence file, `font-feature-settings: "tnum", "cv11"`; the system stack as fallback.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
   - Done when:
     - The built app serves the font from its own origin; no external font request in the e2e run (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `fully-onboarded`: every route of the rail/header (overview, plan, epic, projects, decisions, reports, designer, system, cleanup, settings) in dark and light; Inter is the rendered font (`browser_evaluate` of `getComputedStyle(document.body).fontFamily`); baseline and after snapshots plus screenshots recorded (evidence order: `none`)
4. **Contrast audit reset**
   - What: Every text and state colour pair passes WCAG 2.1 AA in both themes; the allowlist of `theme-readability.spec.js` is empty or every entry has its reason; `theme-readability.adoc` lists the new tokens.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript
   - Done when:
     - `PM_MOCK_WEB_PORT=20120 PM_WEB_PORT=20121 ./mvnw -pl pm-web-e2e verify -Pe2e` passes with the reset allowlist (evidence order: `none`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `fully-onboarded`: every route of the rail/header (overview, plan, epic, projects, decisions, reports, designer, system, cleanup, settings) in dark and light; Inter is the rendered font (`browser_evaluate` of `getComputedStyle(document.body).fontFamily`); baseline and after snapshots plus screenshots recorded (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-web-app verify`, `PM_MOCK_WEB_PORT=20120 PM_WEB_PORT=20121 ./mvnw -pl pm-web-e2e verify -Pe2e`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- Ports of this plan: mock web port `20120`, app port `20121` (`AGENTS.md` § Ports, made passable by `shared-port-assignment`, PLAN-76). Pass them to every mock, app and e2e start; other plans may run at the same time, so never stop or reuse a process this worktree did not start, and an assigned port that is in use is a blocker, never a reason to take another port.
- The font file is the one new asset the operator approved for this plan; no other dependency.
- Values of VD-P-02 are proposals: a pair that fails the contrast audit is tuned, and the tuned value is recorded in `theme-readability.adoc`.
- Browser check of the rendered output, with the Playwright MCP server of `opencode.json`, following `AGENTS.md` § Browser checks: start the mock (`./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario <scenario> --web-port 20120"`), then `PM_MOCK_WEB_PORT=20120 ./pm-web-app/start.sh -p 20121` (read the port from `pm-web-app/target/pm-web-app.port`); the MCP browser starts with an empty profile, so pair again every session with a fresh single-use code (`POST http://127.0.0.1:20120/mock/pairing`, then open `<app>/pair#code=<CODE>`), never through the `Network:` URL; reach a state with `POST /mock/goto/<state>` and a reload. Check each target in `de` and `en`, dark and light, at 1440 × 900 and 390 × 844 (`browser_resize`). Read the page through `browser_snapshot` (the accessibility tree is the evidence of text and structure) and keep one `browser_take_screenshot` per target; the server writes to its `--output-dir` `.plan/temp/playwright-mcp/` (`opencode.json`), so name every file `{plan}-{target}-{before|after}` and never write to the repository root. Take a baseline snapshot BEFORE the change and one AFTER, and record both paths in the progress record. The visible text of a checked view shows no raw enum, state id, `{placeholder}`, ISO time or emoji outside model-written blocks. Stop the app (`./pm-web-app/stop.sh`) and the mock afterwards. If the Playwright MCP server is not available in the run, the browser Done-when clauses are NOT met — a blocker reported as such, never replaced by a unit test or skipped.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/visual-design/plan.adoc#VD-S-01`, `doc/claude_review/visual-design/proposals.adoc#VD-P-02` — read at `doc/claude_review/visual-design/plan.adoc`
- OBSERVED: `pm-web-app/src/styles/tokens.css` and `base.css` exist — read at `pm-web-app/src/styles/`
- HYPOTHESIS: `styles/tokens.test.ts` does not exist yet — confirm/refute at `pm-web-app/src/styles/` (verify-at-outline)
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-web-app/src/styles/tokens.css` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/styles/base.css` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/styles/tokens.test.ts` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/static/fonts/` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-web-e2e/tests/theme-readability.spec.js` — present at HEAD c4dc9bb4
- OBSERVED: `doc/design/web-app/README.adoc` — present at HEAD c4dc9bb4
- OBSERVED: `doc/design/web-app/theme-readability.adoc` — present at HEAD c4dc9bb4

## Dependencies and Sequencing

- Depends on: `PLAN-01` (`web-text-guard-report`), `PLAN-76` (`shared-port-assignment`)
- Overlaps with: `web-text-guard-report` also edits `theme-readability.spec.js`: runs after it
- Adjacent to: none

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-12-web-design-tokens-v2.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
