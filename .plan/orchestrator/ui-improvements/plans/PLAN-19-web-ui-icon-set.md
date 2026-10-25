# PLAN-19: Web: Own UI Icon Set, No Emoji

epic: ui-improvements
workstream: WS-02

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Draw the UI icon set of VD-P-06 as SVG on the workflow icon contract, replace every emoji in the interface, and give harnesses without a fetched icon a monogram (VD-S-02).

Source of this plan (read before scoping):

- `doc/claude_review/visual-design/plan.adoc#VD-S-02`
- `doc/claude_review/visual-design/proposals.adoc#VD-P-06`
- `doc/claude_review/visual-design/findings.adoc#VD-F-08`

## Deliverables

1. **UI icons**
   - What: About 40 icons (navigation, acts, states, kinds of VD-P-06) in `lib/icons/ui/*.svg`, 24 px grid, 1.5 px stroke, round caps, `currentColor`, indexed by `lib/icons/uiIcons.ts` and rendered by `lib/components/Icon.svelte`; the licence gate of `workflow-visual-language.adoc` applied.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
   - Done when:
     - A unit test asserts every icon's grid, stroke and `currentColor`, and that `Icon` renders each name (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `fully-onboarded`: header and `/system/cleanup` show icons with labels and no emoji; `onboarding` → `harness.choose` shows a monogram; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
2. **No emoji**
   - What: Every emoji in the interface (header ✎ 🖥 ⚙ 🌙 ☀️, cleanup 🧹) is an icon with a label.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
   - Done when:
     - The text guard finds no `\p{Extended_Pictographic}` on any view; the baseline loses every emoji entry (evidence order: `none`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `fully-onboarded`: header and `/system/cleanup` show icons with labels and no emoji; `onboarding` → `harness.choose` shows a monogram; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
3. **Harness monograms**
   - What: `HarnessChoice.svelte` shows the fetched icon, else a monogram in a 24 px rounded square (`harness-icons.adoc`).
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
   - Done when:
     - A unit test renders a harness without an icon and finds the monogram (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `fully-onboarded`: header and `/system/cleanup` show icons with labels and no emoji; `onboarding` → `harness.choose` shows a monogram; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-web-app verify`, `./mvnw -pl pm-web-e2e verify -Pintegration-tests`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- Own icons only (operator decision): no icon library dependency.
- Browser check of the rendered output, with the Playwright MCP server of `opencode.json`, following `AGENTS.md` § Browser checks: start the mock (`./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario <scenario> --web-port 7420"`), then `./pm-web-app/start.sh` (read the port from `pm-web-app/target/pm-web-app.port`); the MCP browser starts with an empty profile, so pair again every session with a fresh single-use code (`POST http://127.0.0.1:7420/mock/pairing`, then open `<app>/pair#code=<CODE>`), never through the `Network:` URL; reach a state with `POST /mock/goto/<state>` and a reload. Check each target in `de` and `en`, dark and light, at 1440 × 900 and 390 × 844 (`browser_resize`). Read the page through `browser_snapshot` (the accessibility tree is the evidence of text and structure) and keep one `browser_take_screenshot` per target, both saved under `.plan/temp/browser/{plan}/` — never the repository root. Take a baseline snapshot BEFORE the change and one AFTER, and record both paths in the progress record. The visible text of a checked view shows no raw enum, state id, `{placeholder}`, ISO time or emoji outside model-written blocks. Stop the app (`./pm-web-app/stop.sh`) and the mock afterwards. If the Playwright MCP server is not available in the run, the browser Done-when clauses are NOT met — a blocker reported as such, never replaced by a unit test or skipped.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/visual-design/plan.adoc#VD-S-02`, `doc/claude_review/visual-design/proposals.adoc#VD-P-06` — read at `doc/claude_review/visual-design/plan.adoc`
- OBSERVED: workflow icons exist in `pm-web-app/src/lib/icons/workflows/` with `workflowIcons.ts` — read at `pm-web-app/src/lib/icons/`
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/icons/ui/` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-web-app/src/lib/icons/uiIcons.ts` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-web-app/src/lib/icons/uiIcons.test.ts` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/Icon.svelte` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/HarnessChoice.svelte` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/routes/+layout.svelte` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/views/CleanupView.svelte` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-e2e/tests/text-guard-baseline.json` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `doc/design/web-app/workflow-visual-language.adoc` — present at HEAD c4dc9bb4

## Dependencies and Sequencing

- Depends on: `PLAN-12` (`web-design-tokens-v2`)
- Overlaps with: none known at staging
- Adjacent to: none

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-19-web-ui-icon-set.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
