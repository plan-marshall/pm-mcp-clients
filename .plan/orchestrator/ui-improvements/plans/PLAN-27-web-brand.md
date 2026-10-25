# PLAN-27: Web: Brand Mark, Pairing Page, Favicon

epic: ui-improvements
workstream: WS-02

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Give the product its brand presence of VD-P-08: the mark in the brand colour, the pairing page as the first screen a buyer sees, empty states with the mark, and a favicon with an attention dot (VD-S-05).

Source of this plan (read before scoping):

- `doc/claude_review/visual-design/plan.adoc#VD-S-05`
- `doc/claude_review/visual-design/proposals.adoc#VD-P-08`

## Deliverables

1. **BrandMark**
   - What: `BrandMark.svelte` in the brand colour at 24 px in a 28 px square for the rail head and 64 px for the pairing page.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
   - Done when:
     - Unit tests for both sizes (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: a CLEAN profile (`--isolated`, AGENTS.md): `/pair` shows the mark, the wordmark, the code and the QR; after pairing the favicon carries the attention dot; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
2. **Pairing page**
   - What: `PairingView.svelte`: the mark at 64 px, the wordmark, the pairing code at `--text-3xl` monospace, the QR code.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript
   - Done when:
     - `pairing.spec.js` finds the mark, the code and the QR (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: a CLEAN profile (`--isolated`, AGENTS.md): `/pair` shows the mark, the wordmark, the code and the QR; after pairing the favicon carries the attention dot; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
3. **Favicon and design**
   - What: The favicon is the mark, with a small attention dot while the inbox has items; `brand-mark.adoc` states the brand colour and these uses.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
   - Done when:
     - A unit test toggles the favicon dot from the attention count (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: a CLEAN profile (`--isolated`, AGENTS.md): `/pair` shows the mark, the wordmark, the code and the QR; after pairing the favicon carries the attention dot; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-web-app verify`, `./mvnw -pl pm-web-e2e verify -Pintegration-tests`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- The rail head itself comes with `web-app-shell`; this plan provides the mark it uses.
- Browser check of the rendered output, with the Playwright MCP server of `opencode.json`, following `AGENTS.md` § Browser checks: start the mock (`./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario <scenario> --web-port 7420"`), then `./pm-web-app/start.sh` (read the port from `pm-web-app/target/pm-web-app.port`); the MCP browser starts with an empty profile, so pair again every session with a fresh single-use code (`POST http://127.0.0.1:7420/mock/pairing`, then open `<app>/pair#code=<CODE>`), never through the `Network:` URL; reach a state with `POST /mock/goto/<state>` and a reload. Check each target in `de` and `en`, dark and light, at 1440 × 900 and 390 × 844 (`browser_resize`). Read the page through `browser_snapshot` (the accessibility tree is the evidence of text and structure) and keep one `browser_take_screenshot` per target; the server writes to its `--output-dir` `.plan/temp/playwright-mcp/` (`opencode.json`), so name every file `{plan}-{target}-{before|after}` and never write to the repository root. Take a baseline snapshot BEFORE the change and one AFTER, and record both paths in the progress record. The visible text of a checked view shows no raw enum, state id, `{placeholder}`, ISO time or emoji outside model-written blocks. Stop the app (`./pm-web-app/stop.sh`) and the mock afterwards. If the Playwright MCP server is not available in the run, the browser Done-when clauses are NOT met — a blocker reported as such, never replaced by a unit test or skipped.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/visual-design/plan.adoc#VD-S-05`, `doc/claude_review/visual-design/proposals.adoc#VD-P-08` — read at `doc/claude_review/visual-design/plan.adoc`
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/components/BrandMark.svelte` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/components/BrandMark.test.ts` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/views/PairingView.svelte` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/static/brand-mark.svg` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/app.html` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-e2e/tests/pairing.spec.js` — present at HEAD c4dc9bb4
- OBSERVED: `doc/design/web-app/brand-mark.adoc` — present at HEAD c4dc9bb4

## Dependencies and Sequencing

- Depends on: `PLAN-12` (`web-design-tokens-v2`), `PLAN-19` (`web-ui-icon-set`)
- Overlaps with: none known at staging
- Adjacent to: none

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-27-web-brand.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
