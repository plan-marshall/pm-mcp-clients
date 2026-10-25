# PLAN-07: Terminal: Presentation in the Design Documents

epic: ui-improvements
workstream: WS-05

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Write the presentation rules into the terminal design first: colour roles and switch-off rules, the full ASCII table, the column grid, the brand line, and `operator.tui.unavailable.*` (CLI-VD-S-01; CLI-VD-P-06).

Source of this plan (read before scoping):

- `doc/claude_review/cli/visual-design/plan.adoc#CLI-VD-S-01`
- `doc/claude_review/cli/visual-design/proposals.adoc#CLI-VD-P-06`
- `doc/claude_review/cli/visual-design/cli-vd-roles.svg`

## Deliverables

1. **Dialogues document**
   - What: `03-dialogues.adoc#terminal`: the roles of CLI-VD-P-01, the switch-off rules, the ASCII table of CLI-VD-P-02.
   - Persona: plan-marshall:persona-documenter
   - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation
   - Done when:
     - The section exists and passes `doc-review` (evidence order: `none`)
2. **Overview and texts**
   - What: `15-overview.adoc`: the column grid of CLI-VD-P-03 and the brand line of CLI-VD-P-05; `09-texts-common.adoc`: `operator.tui.unavailable.*` in English and German.
   - Persona: plan-marshall:persona-documenter
   - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation
   - Done when:
     - `doc-review` on `doc/design/terminal-client/` is clean (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- No new dependency or plugin unless this spec names it as approved by the operator.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/cli/visual-design/plan.adoc#CLI-VD-S-01`, `doc/claude_review/cli/visual-design/proposals.adoc#CLI-VD-P-06` — read at `doc/claude_review/cli/visual-design/plan.adoc`
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `doc/design/terminal-client/03-dialogues.adoc` — present at HEAD c4dc9bb4
- OBSERVED: `doc/design/terminal-client/15-overview.adoc` — present at HEAD c4dc9bb4
- OBSERVED: `doc/design/terminal-client/09-texts-common.adoc` — present at HEAD c4dc9bb4

## Dependencies and Sequencing

- Depends on: none
- Overlaps with: `cli-footer-and-help-overlay` also edits `15-overview.adoc` (one shared file)
- Adjacent to: none

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-07-cli-terminal-design-docs.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
