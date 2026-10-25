# PLAN-25: Web: Core Components, Controls

epic: ui-improvements
workstream: WS-02

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Build the control components of design system v2 and the development page that shows every component in both themes (VD-S-03, first half; VD-P-05).

Source of this plan (read before scoping):

- `doc/claude_review/visual-design/plan.adoc#VD-S-03`
- `doc/claude_review/visual-design/proposals.adoc#VD-P-05`

## Deliverables

1. **Button and IconButton**
   - What: `Button` with `primary`, `secondary`, `ghost`, `danger` (only inside a danger dialog), sizes `sm`/`md`/`lg`, optional icon, disabled with its reason as advisory and `aria-describedby`; `IconButton` square, ghost, tooltip from the `X.title` advisory.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
   - Done when:
     - Unit tests per variant, size and the disabled reason (evidence order: `test-first`)
2. **StatusLabel, CountBadge, Chip**
   - What: `StatusLabel` (8 px dot + state word), `CountBadge` (attention fill for "needs you", neutral otherwise), `Chip` (monospace id, copy on click).
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
   - Done when:
     - Unit tests per state colour and the copy act (evidence order: `test-first`)
3. **ActionBar style**
   - What: The visual `ActionBar`: one primary, up to three secondary, an overflow `IconButton` with a menu; the behaviour (acts by state) comes with `web-actions-by-state`.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
   - Done when:
     - A unit test renders five acts and finds one primary, three secondary, one in the overflow (evidence order: `test-first`)
4. **Development page**
   - What: `routes/dev/components/+page.svelte`, built only in development, shows each component in both themes.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
   - Done when:
     - The production build holds no `/dev` route (evidence order: `test-first`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-web-app verify`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- Components only: no view is migrated here (VD-S-06 runs with the view plans).
- Update `doc/design/web-app/interaction-affordances.adoc` for the overflow menu if it states controls.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/visual-design/plan.adoc#VD-S-03`, `doc/claude_review/visual-design/proposals.adoc#VD-P-05` — read at `doc/claude_review/visual-design/plan.adoc`
- OBSERVED: `pm-web-app/src/lib/components/Button.svelte` exists today — read at `pm-web-app/src/lib/components/`
- HYPOTHESIS: today's `Button` is used by few views and has no variants of VD-P-05 — confirm/refute at `pm-web-app/src/lib/components/Button.svelte` (verify-at-outline)
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/components/Button.svelte` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/components/IconButton.svelte` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/StatusLabel.svelte` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/CountBadge.svelte` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/Chip.svelte` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/ActionBar.svelte` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/Button.test.ts` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-web-app/src/routes/dev/components/+page.svelte` — absent at HEAD c4dc9bb4; created by this plan

## Dependencies and Sequencing

- Depends on: `PLAN-12` (`web-design-tokens-v2`), `PLAN-19` (`web-ui-icon-set`)
- Overlaps with: none known at staging
- Adjacent to: none

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-25-web-core-components-controls.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
