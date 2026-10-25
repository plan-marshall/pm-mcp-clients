# PLAN-11: Mock: Plan Details per Id, Routes, Lifecycle State

epic: ui-improvements
workstream: WS-01

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Close the mock gaps the CLI review found: plan details per id with `404 plan_not_found` for an unknown id, the routes the clients call in every scenario, the lifecycle state of `system.*` in `/api/v1/status`, and the socket name in the README (CLI-UC-S-09, CLI-UC-P-08).

Source of this plan (read before scoping):

- `doc/claude_review/cli/use-cases/plan.adoc#CLI-UC-S-09`
- `doc/claude_review/cli/use-cases/proposals.adoc#CLI-UC-P-08`
- `doc/claude_review/cli/cross-cutting/findings.adoc#CLI-CC-F-10`

## Deliverables

1. **Plan details per id**
   - What: The mock returns the details of the plan with the requested id and `404 plan_not_found` for an unknown id.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:junit-core
   - Done when:
     - A mock test asks for two plan ids and an unknown id and gets two different plans and a `404 plan_not_found` (evidence order: `test-first`)
2. **Routes and lifecycle state**
   - What: `/api/v1/plans` answers in every scenario (an empty list on purpose where the scenario has none); `/api/v1/status` carries `lifecycle_state` for the `system.*` states (`draining`, `restarting`, `stopped`).
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:junit-core
   - Done when:
     - The scenario contract test of `mock-scenario-states` covers the routes and the lifecycle state of every `system.*` state (evidence order: `test-first`)
3. **README socket name**
   - What: `pm-mock-runtime/README.adoc` names `run/runtime.sock`, not `pm-mcpd.sock`.
   - Persona: plan-marshall:persona-documenter
   - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation
   - Done when:
     - The README names the socket the mock creates (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-mock-runtime verify`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/cli/use-cases/plan.adoc#CLI-UC-S-09`, `doc/claude_review/cli/use-cases/proposals.adoc#CLI-UC-P-08` — read at `doc/claude_review/cli/use-cases/plan.adoc`
- HYPOTHESIS: the mock returns the same plan for every plan id and has no routes for `/api/v1/config/<aspect>` and `/api/v1/roles` — confirm/refute at `pm-mock-runtime/src/main/java/de/planmarshall/mock/http/` (verify-at-outline)
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/http/` — present at HEAD c4dc9bb4
- OBSERVED: `pm-mock-runtime/src/main/resources/scenarios/reports-and-system.json` — present at HEAD c4dc9bb4
- OBSERVED: `pm-mock-runtime/README.adoc` — present at HEAD c4dc9bb4
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/runtime/` — present at HEAD c4dc9bb4

## Dependencies and Sequencing

- Depends on: `PLAN-03` (`mock-scenario-states`)
- Overlaps with: `mock-scenario-states` (same engine and README): runs after it
- Adjacent to: none

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-11-mock-cli-fixes.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
