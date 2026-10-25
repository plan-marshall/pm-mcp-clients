# PLAN-37: Web: Accessibility Baseline and axe-core

epic: ui-improvements
workstream: WS-02

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Lay the accessibility baseline of CC-P-07 and add the automated audit with axe-core in `pm-web-e2e` (operator decision) (CC-S-05).

Source of this plan (read before scoping):

- `doc/claude_review/cross-cutting/plan.adoc#CC-S-05`
- `doc/claude_review/cross-cutting/proposals.adoc#CC-P-07`
- `doc/claude_review/cross-cutting/findings.adoc#CC-F-07`

## Deliverables

1. **Escape and focus**
   - What: Escape closes every panel, popover and dialog (pause panel, chat, overflow menu); focus returns to the opener.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript
   - Done when:
     - e2e keyboard checks in `workspace.spec.js` per panel (evidence order: `test-first`)
2. **Reasons, labels, sizes**
   - What: A disabled control states its reason in its advisory and through `aria-describedby`; icon-only controls have a visible label from tablet width up and an `aria-label` always; minimum text 12 px; no uppercase transform; touch targets 44 px on the phone.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
   - Done when:
     - Unit tests on `WorkControlPanel` and `ChatPanel` for reason and label (evidence order: `test-first`)
3. **axe-core audit**
   - What: Add `@axe-core/playwright` to `pm-web-e2e` and run it on every declared view; serious issues fail the run.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript
   - Done when:
     - `./mvnw -pl pm-web-e2e verify -Pintegration-tests` passes with the audit on every declared view (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-web-app verify`, `./mvnw -pl pm-web-e2e verify -Pintegration-tests`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- axe-core is the one new dependency the operator approved for this plan.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/cross-cutting/plan.adoc#CC-S-05`, `doc/claude_review/cross-cutting/proposals.adoc#CC-P-07` — read at `doc/claude_review/cross-cutting/plan.adoc`
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/components/WorkControlPanel.svelte` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/components/ChatPanel.svelte` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/components/WorkControlPanel.test.ts` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-e2e/tests/workspace.spec.js` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-e2e/tests/helpers/a11y.js` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-web-e2e/package.json` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-e2e/package-lock.json` — present at HEAD c4dc9bb4

## Dependencies and Sequencing

- Depends on: `PLAN-31` (`web-core-components-containers`)
- Overlaps with: none known at staging
- Adjacent to: none

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-37-web-accessibility-baseline.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
