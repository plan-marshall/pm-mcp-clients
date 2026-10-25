# PLAN-55: Web: The Plan Page on the Object Template

epic: ui-improvements
workstream: WS-03

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Rework the plan page of UX-P-05: head with title, id chip, state sentence and acts, an attention strip when the state asks, tabs (Process, Intent, Activity, Tokens, Settings), tree rows without repeated kinds, and activity rows without raw tags (UX-S-07).

Source of this plan (read before scoping):

- `doc/claude_review/ux/plan.adoc (UX-S-07)`
- `doc/claude_review/ux/proposals.adoc#UX-P-05`
- `doc/claude_review/ux/ux-plan-page.svg`
- `doc/claude_review/ux/findings.adoc#UX-F-07`
- `doc/claude_review/ux/findings.adoc#UX-F-08`
- `doc/claude_review/ux/findings.adoc#UX-F-09`

## Deliverables

1. **Head and attention strip**
   - What: Kind, path, title, id chip, state sentence ("Running · execute · for 2 h 10 min · task 3 of 5"), PR chip, the acts of `byState`; the strip shows "Waits for your decision … · Open the decision" and the off-track banner.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript
   - Done when:
     - `plan-view.spec.js` on `control.ready` and `decisions.open` (evidence order: `test-first`)
2. **Tabs and Settings tab**
   - What: Tabs Process (default), Intent, Activity, Tokens, Settings; `PlanSettingsModal` becomes the Settings tab.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
   - Done when:
     - Unit tests: tab switch keeps state; the modal is gone (evidence order: `test-first`)
3. **Tree and activity rows**
   - What: Tree rows: icon, name without the kind, state word, time; "Jump to now" icon button in the tree head; activity rows: local time, kind icon, tool and target; empty `LINK` rows do not render.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
   - Done when:
     - `NodeNarrative.test.ts` and a `PlanView` test: no `TOOL_CALL`/`PHASE` text; no empty row (evidence order: `test-first`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-web-app verify`, `./mvnw -pl pm-web-e2e verify -Pintegration-tests`, `./mvnw -pl pm-api verify`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- Every view this plan touches uses the design system v2 components (VD-S-06): no plain text button, no acting link, no state pill, no uppercase label, no emoji, no own page width.
- Every server value on a touched view passes through the display layer and the formatter of WS-02.
- Views this plan touches pass the text guard of `web-text-guard-report`: the baseline file may only shrink, never grow.
- The design document of `doc/design/` that this plan changes is updated in the same pull request (UX-S-13, VD-S-09).
- Keep the tree on the left and the narrative on the right (operator finding in `doc/findings-web.txt`).
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/ux/plan.adoc (UX-S-07)`, `doc/claude_review/ux/proposals.adoc#UX-P-05` — read at `doc/claude_review/ux/plan.adoc`
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/views/PlanView.svelte` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/views/PlanView.test.ts` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/components/NodeNarrative.svelte` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/components/PlanSettingsModal.svelte` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/components/PlanSettingsModal.test.ts` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-e2e/tests/plan-view.spec.js` — present at HEAD c4dc9bb4
- OBSERVED: `doc/design/shared/plan-view.adoc` — present at HEAD c4dc9bb4
- OBSERVED: `doc/design/web-app/plan-view-interaction.adoc` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/plan_view.en.properties` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/plan_view.de.properties` — present at HEAD c4dc9bb4

## Dependencies and Sequencing

- Depends on: `PLAN-43` (`web-page-templates`), `PLAN-47` (`web-actions-by-state`)
- Overlaps with: none known at staging
- Adjacent to: none

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-55-web-plan-page.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
