# PLAN-08: Use-Case Document Changes of Both Reviews

epic: ui-improvements
workstream: WS-01

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Write the use-case changes both reviews propose into `plan-marshall-documentation` as `DRAFT`, ask the operator to agree, and link the new use cases from the designs that serve them (web UC-S-08, CLI CLI-UC-S-10). The attention inbox, the command palette and the `needs` verb wait on this agreement.

Source of this plan (read before scoping):

- `doc/claude_review/use-cases/proposals.adoc#documents`
- `doc/claude_review/use-cases/plan.adoc (UC-S-08)`
- `doc/claude_review/cli/use-cases/proposals.adoc#documents`
- `doc/claude_review/cli/use-cases/plan.adoc#CLI-UC-S-10`

## Deliverables

1. **New use cases UC-PLN-7 and UC-PLN-8**
   - What: Add UC-PLN-7 "See What Needs You" (text of UC-P-09, plus the terminal variation of the CLI review) and UC-PLN-8 "Go to Work by Name" (UC-P-10) to `03-plans.adoc` as `DRAFT`, and to the index `doc/UseCases.adoc`.
   - Persona: plan-marshall:persona-documenter
   - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation
   - Done when:
     - Both use cases exist with status `DRAFT`, follow the template of `UseCases.adoc`, and are listed in the index (evidence order: `none`)
2. **Variations of UC-PLN-2, UC-PRJ-1, UC-ONB-1, UC-PLN-4**
   - What: Add the variations of UC-P-11 (several open; own answer confirmed), UC-P-12 (needs the terminal), UC-P-13 (not yet onboarded), and the machine-readable variation of UC-PLN-4 from the CLI review.
   - Persona: plan-marshall:persona-documenter
   - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation
   - Done when:
     - Each variation is in its use case, neutral between the clients (evidence order: `none`)
3. **Agreement and design links**
   - What: Ask the operator to agree to the drafts; record the answer in the pull request; link UC-PLN-7 and UC-PLN-8 from `doc/design/README.adoc` and the shared designs that serve them.
   - Persona: plan-marshall:persona-documenter
   - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation
   - Done when:
     - The pull request records the operator's agreement or the requested changes; the changed modules pass `doc-review` (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- No new dependency or plugin unless this spec names it as approved by the operator.
- The main change lands in the sibling repository `plan-marshall-documentation` (`doc/use-cases/`, `doc/UseCases.adoc`) through its own pull request, linked from this repository's pull request; its paths are outside this repository and so are not part of the Expected Surface.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/use-cases/proposals.adoc#documents`, `doc/claude_review/use-cases/plan.adoc (UC-S-08)` — read at `doc/claude_review/use-cases/proposals.adoc`
- OBSERVED: the use cases live in `../plan-marshall-documentation/doc/use-cases/` (`01-onboarding.adoc`, `03-plans.adoc`, `04-projects.adoc`) — read at `doc/claude_review/use-cases/analysis.adoc` § matrix
- HYPOTHESIS: UC-PLN-7 and UC-PLN-8 do not exist yet — confirm/refute at `../plan-marshall-documentation/doc/use-cases/03-plans.adoc` (verify-at-outline)
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `doc/design/README.adoc` — present at HEAD c4dc9bb4
- OBSERVED: `doc/design/shared/plan-overview.adoc` — present at HEAD c4dc9bb4

## Dependencies and Sequencing

- Depends on: none
- Overlaps with: none known at staging
- Adjacent to: `doc/claude_review/` stays as written: the review is evidence, not a living document

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-08-use-case-documents.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
