# PLAN-06: Shared Display Vocabulary for Both Clients

epic: ui-improvements
workstream: WS-01

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Give both clients one vocabulary for server values: display words for every enum value and state id in the shared catalogue, one time and duration rule, and one id-to-title rule, specified in `shared/texts-and-languages.adoc`. This is the convergence both reviews call for (web CC-P-01, CC-P-04; CLI CLI-CC-P-02, CLI-CC-P-03, CLI-CC-S-04 step 1).

Source of this plan (read before scoping):

- `doc/claude_review/cross-cutting/proposals.adoc#CC-P-01`
- `doc/claude_review/cross-cutting/proposals.adoc#CC-P-04`
- `doc/claude_review/cli/cross-cutting/proposals.adoc#CLI-CC-P-02`
- `doc/claude_review/cli/cross-cutting/proposals.adoc#CLI-CC-P-03`
- `doc/claude_review/cli/cross-cutting/analysis.adoc#shared`

## Deliverables

1. **Time, duration, and title rule in the shared design**
   - What: Specify in `doc/design/shared/texts-and-languages.adoc` the time and duration table of CLI-CC-P-03 (English and German), the web styles `relative`, `short`, `clock` of CC-P-04 as renderings of the same rule, the rule "no ISO string, ever", and the id-to-title rule (title first, id dim or as a chip beside it).
   - Persona: plan-marshall:persona-documenter
   - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation
   - Done when:
     - The section exists and passes the `doc-review` skill (evidence order: `none`)
2. **State words in the shared catalogue**
   - What: Add `state.<kind>.<value>` keys in `en` and `de` for every enum value the `pm-api` contract carries and the six mock scenarios use (plan, epic, task, worker, model status, landing, cleanup kind, activity kind, decision reversibility, gate kind, off-track signal), plus `unit.<id>` plain names for workflow unit ids.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:junit-core
   - Done when:
     - A `pm-api` unit test enumerates the enum values of the contract and of the six scenarios and fails for a value without a key in `en` or `de` (evidence order: `test-first`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-api verify`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- Keys only and the specification: no client code changes here; `web-display-layer` and `cli-display-layer` consume the keys.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/cross-cutting/proposals.adoc#CC-P-01`, `doc/claude_review/cross-cutting/proposals.adoc#CC-P-04` — read at `doc/claude_review/cross-cutting/proposals.adoc`
- OBSERVED: the shared catalogue lives in `pm-api/src/main/resources/messages/` as `<area>.<locale>.properties` — read at `pm-api/src/main/resources/messages/common.en.properties`
- HYPOTHESIS: the web app reads the same keys through `GET /api/v1/messages/<locale>/<area>` — confirm/refute at `pm-web-app/src/lib/i18n/index.ts` § catalogue load (verify-at-outline)
- HYPOTHESIS: a `state.*` key family does not yet exist in the catalogue — confirm/refute by searching `pm-api/src/main/resources/messages/` for `state.` (verify-at-outline)
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `doc/design/shared/texts-and-languages.adoc` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/common.en.properties` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/common.de.properties` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/message/StateWordsCompletenessTest.java` — absent at HEAD c4dc9bb4; created by this plan

## Dependencies and Sequencing

- Depends on: none
- Overlaps with: none known at staging
- Adjacent to: none

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-06-shared-display-vocabulary.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
