# PLAN-01: Web: Text Guard in Report Mode

epic: ui-improvements
workstream: WS-02

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Add the text guard of CC-P-09 to the e2e suite in report mode: it collects the visible text of every declared view in `de` and `en` and lists raw enums, state ids, placeholders, ISO times, emoji and words of the other language, without failing; the list is committed as the baseline (CC-S-01). It protects every later web step.

Source of this plan (read before scoping):

- `doc/claude_review/cross-cutting/plan.adoc#CC-S-01`
- `doc/claude_review/cross-cutting/proposals.adoc#CC-P-09`

## Deliverables

1. **Guard helper**
   - What: `pm-web-e2e/tests/helpers/textGuard.js` with the six rules of CC-P-09; content marked `data-model-text` is skipped.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript
   - Done when:
     - A unit-level spec feeds a page with one violation per rule and gets six named violations (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `fully-onboarded`: every route of the rail/header (overview, plan, epic, projects, decisions, reports, designer, system, cleanup, settings) — the raw words the browser shows match the baseline entries the guard lists; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
2. **Guard in the readability spec, report mode**
   - What: `theme-readability.spec.js` runs the guard on every declared view in both locales and writes the violations per view; the committed `pm-web-e2e/tests/text-guard-baseline.json` makes the run pass while the list equals the baseline and fail when a view gains a violation.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript
   - Done when:
     - The baseline lists at least the findings CC-F-01 to CC-F-05 (proof that the guard sees them) (evidence order: `none`)
     - Adding a raw enum to one view makes the spec fail with that view and that word (evidence order: `mutation-after`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `fully-onboarded`: every route of the rail/header (overview, plan, epic, projects, decisions, reports, designer, system, cleanup, settings) — the raw words the browser shows match the baseline entries the guard lists; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-web-e2e verify -Pintegration-tests`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- The baseline grows only in this plan. Every later web plan may only shrink it.
- Browser check of the rendered output, with the Playwright MCP server of `opencode.json`, following `AGENTS.md` § Browser checks: start the mock (`./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario <scenario> --web-port 7420"`), then `./pm-web-app/start.sh` (read the port from `pm-web-app/target/pm-web-app.port`); the MCP browser starts with an empty profile, so pair again every session with a fresh single-use code (`POST http://127.0.0.1:7420/mock/pairing`, then open `<app>/pair#code=<CODE>`), never through the `Network:` URL; reach a state with `POST /mock/goto/<state>` and a reload. Check each target in `de` and `en`, dark and light, at 1440 × 900 and 390 × 844 (`browser_resize`). Read the page through `browser_snapshot` (the accessibility tree is the evidence of text and structure) and keep one `browser_take_screenshot` per target, both saved under `.plan/temp/browser/{plan}/` — never the repository root. Take a baseline snapshot BEFORE the change and one AFTER, and record both paths in the progress record. The visible text of a checked view shows no raw enum, state id, `{placeholder}`, ISO time or emoji outside model-written blocks. Stop the app (`./pm-web-app/stop.sh`) and the mock afterwards. If the Playwright MCP server is not available in the run, the browser Done-when clauses are NOT met — a blocker reported as such, never replaced by a unit test or skipped.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/cross-cutting/plan.adoc#CC-S-01`, `doc/claude_review/cross-cutting/proposals.adoc#CC-P-09` — read at `doc/claude_review/cross-cutting/plan.adoc`
- OBSERVED: `pm-web-e2e/tests/theme-readability.spec.js` exists and proves every declared view is covered — read at `doc/claude_review/use-cases/verification.adoc` § Regression Guard
- HYPOTHESIS: the spec has a list of declared views the guard can iterate — confirm/refute at `pm-web-e2e/tests/theme-readability.spec.js` (verify-at-outline)
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-web-e2e/tests/helpers/textGuard.js` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-web-e2e/tests/theme-readability.spec.js` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-e2e/tests/text-guard-baseline.json` — absent at HEAD c4dc9bb4; created by this plan

## Dependencies and Sequencing

- Depends on: none
- Overlaps with: none known at staging
- Adjacent to: none

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-01-web-text-guard-report.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
