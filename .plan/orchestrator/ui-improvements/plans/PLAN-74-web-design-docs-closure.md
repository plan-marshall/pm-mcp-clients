# PLAN-74: Web: Design Documents Closed

epic: ui-improvements
workstream: WS-03

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Close the design documents of the web review: rewrite "Visual Language", update view density and interaction affordances, add `components.adoc`, add the next-action rule to the principles, and mark every proposal done or dropped with a reason (UX-S-13, VD-S-09).

Source of this plan (read before scoping):

- `doc/claude_review/ux/plan.adoc (UX-S-13)`
- `doc/claude_review/visual-design/plan.adoc#VD-S-09`
- `doc/claude_review/visual-design/proposals.adoc#VD-P-09`

## Deliverables

1. **Web-app design**
   - What: `doc/design/web-app/README.adoc`: structure, principles with the next-action rule, Visual Language v2; `view-density-and-filtering.adoc`: templates replace layout modes; `interaction-affordances.adoc`: overflow menu, keyboard.
   - Persona: plan-marshall:persona-documenter
   - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation
   - Done when:
     - The documents pass `doc-review` (evidence order: `none`)
2. **Components document**
   - What: New `doc/design/web-app/components.adoc` with the specifications of VD-P-05 and one picture per component.
   - Persona: plan-marshall:persona-documenter
   - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation
   - Done when:
     - The document exists, is linked from the README, and passes `doc-review` (evidence order: `none`)
3. **Proposal status**
   - What: Every proposal of `doc/claude_review/{ux,visual-design,cross-cutting,use-cases}/proposals.adoc` is listed as done (with its plan) or dropped (with a reason) in `doc/design/web-app/README.adoc`.
   - Persona: plan-marshall:persona-documenter
   - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation
   - Done when:
     - No proposal is unaccounted for (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- No new dependency or plugin unless this spec names it as approved by the operator.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/ux/plan.adoc (UX-S-13)`, `doc/claude_review/visual-design/plan.adoc#VD-S-09` — read at `doc/claude_review/ux/plan.adoc`
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `doc/design/web-app/README.adoc` — present at HEAD c4dc9bb4
- OBSERVED: `doc/design/web-app/view-density-and-filtering.adoc` — present at HEAD c4dc9bb4
- OBSERVED: `doc/design/web-app/interaction-affordances.adoc` — present at HEAD c4dc9bb4
- OBSERVED: `doc/design/web-app/components.adoc` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `doc/design/web-app/theme-readability.adoc` — present at HEAD c4dc9bb4
- OBSERVED: `doc/design/web-app/brand-mark.adoc` — present at HEAD c4dc9bb4

## Dependencies and Sequencing

- Depends on: `PLAN-72` (`web-token-cleanup-lint`), `PLAN-73` (`web-text-guard-failing`)
- Overlaps with: none known at staging
- Adjacent to: none

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-74-web-design-docs-closure.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
