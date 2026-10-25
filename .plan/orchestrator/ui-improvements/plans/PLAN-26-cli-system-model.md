# PLAN-26: Terminal: One System Model

epic: ui-improvements
workstream: WS-04

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Render the printed and the full-screen system views from one `SystemSnapshot`, put the lifecycle state on the first line, say each fact once, and exit `1` on every failed read (CLI-UC-S-08 without the config decision; CLI-UC-P-06).

Source of this plan (read before scoping):

- `doc/claude_review/cli/use-cases/plan.adoc#CLI-UC-S-08`
- `doc/claude_review/cli/use-cases/proposals.adoc#CLI-UC-P-06`

## Deliverables

1. **SystemSnapshot**
   - What: One model and one formatter for both system views.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:junit-core
   - Done when:
     - `SystemFullScreenTest` and a printed-view test show the same listeners and token units (evidence order: `test-first`)
2. **Lifecycle line and repeats**
   - What: First line `draining · 2 plans finish · stop in 4 min`, `restarting`, `stopped`; one line per job kind; "not measured" once with its reason.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:junit-core
   - Done when:
     - A test on `system.draining`: the first line says draining (evidence order: `test-first`)
3. **Exit codes**
   - What: A failed read exits `1` in every read verb.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:junit-core
   - Done when:
     - A unit test per read verb with a refused request (evidence order: `test-first`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-operator verify -Pintegration-tests`, `./mvnw -pl pm-api verify`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- Java 25, Lombok, records; no generic `Exception`/`RuntimeException`; JUnit 5 only, no Mockito (CLAUDE.md).
- Every new operator text goes through `MessageCatalogue` into `operator.en.properties` and `operator.de.properties`.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/cli/use-cases/plan.adoc#CLI-UC-S-08`, `doc/claude_review/cli/use-cases/proposals.adoc#CLI-UC-P-06` — read at `doc/claude_review/cli/use-cases/plan.adoc`
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/system/` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/SystemCommand.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/SystemSnapshot.java` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/system/` — present at HEAD c4dc9bb4
- OBSERVED: `doc/design/terminal-client/25-system.adoc` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/system.en.properties` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/system.de.properties` — present at HEAD c4dc9bb4

## Dependencies and Sequencing

- Depends on: `PLAN-17` (`cli-runtime-act-confirm`), `PLAN-11` (`mock-cli-fixes`)
- Overlaps with: `cli-runtime-act-confirm` touches `system/SystemLevel.java`: runs after it
- Adjacent to: none

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-26-cli-system-model.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
