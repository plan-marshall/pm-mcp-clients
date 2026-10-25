# PLAN-24: Terminal: Decision Record for a Proof of Presence

epic: ui-improvements
workstream: WS-05

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Write the decision record for replacing the operator gate with a proof of presence a model cannot produce (three options, cheapest first), let the operator decide, change `PM-SEC-3` and `L-1`, and plan the runtime and client work as a work package (CLI-CC-S-06; CLI-CC-P-05 "later").

Source of this plan (read before scoping):

- `doc/claude_review/cli/cross-cutting/plan.adoc#CLI-CC-S-06`
- `doc/claude_review/cli/cross-cutting/proposals.adoc#CLI-CC-P-05`

## Deliverables

1. **Decision record**
   - What: An ADR in `plan-marshall-documentation` with the options paired-device confirmation, OS user-presence check, operator-only keychain token; the runtime verifies the proof per act.
   - Persona: plan-marshall:persona-documenter
   - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation
   - Done when:
     - The ADR exists and the pull request records the operator's decision (evidence order: `none`)
2. **Requirement change**
   - What: `PM-SEC-3` and `L-1` state the decided proof.
   - Persona: plan-marshall:persona-documenter
   - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation
   - Done when:
     - The requirement documents pass `doc-review` (evidence order: `none`)
3. **Work package**
   - What: A work package file in `doc/plans/` and its row in `doc/plans/README.adoc`.
   - Persona: plan-marshall:persona-documenter
   - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation
   - Done when:
     - The package is listed `open` with its dependencies (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- No new dependency or plugin unless this spec names it as approved by the operator.
- The ADR and the requirement change land in `plan-marshall-documentation` through its own pull request.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/cli/cross-cutting/plan.adoc#CLI-CC-S-06`, `doc/claude_review/cli/cross-cutting/proposals.adoc#CLI-CC-P-05` — read at `doc/claude_review/cli/cross-cutting/plan.adoc`
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `doc/plans/README.adoc` — present at HEAD c4dc9bb4
- OBSERVED: `doc/plans/proof-of-presence.adoc` — absent at HEAD c4dc9bb4; created by this plan

## Dependencies and Sequencing

- Depends on: `PLAN-14` (`cli-gate-scope`)
- Overlaps with: none known at staging
- Adjacent to: none

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-24-cli-proof-of-presence-decision.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
