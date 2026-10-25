# PLAN-03: Mock: Every Scenario State Reaches Its Views

epic: ui-improvements
workstream: WS-01

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Make `pm-mock-runtime` a test bed in which a `POST /mock/goto/<state>` changes the API resource the state concerns, so a conformance walk is possible for all use cases, and keep its fixtures consistent across resources. Web review step UC-S-01 (proposal UC-P-08).

Source of this plan (read before scoping):

- `doc/claude_review/use-cases/plan.adoc (UC-S-01)`
- `doc/claude_review/use-cases/proposals.adoc#UC-P-08`
- `doc/claude_review/use-cases/analysis.adoc#mock`
- `doc/claude_review/cross-cutting/findings.adoc#CC-F-09`

## Deliverables

1. **State facts applied by goto**
   - What: Each state of `running-work`, `projects-and-start` and `reports-and-system` carries the domain facts it stands for (plan instance, holds, start registry state) and `goto` applies them to the engine.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:junit-core
   - Done when:
     - `ScenarioStateContractTest` walks every state of the three scenarios and asserts that the resource the state concerns differs from the start state, or that the state is declared equal on purpose (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `running-work`, `projects-and-start`, `reports-and-system`: after each `POST /mock/goto/<state>` and a reload, the view the state concerns shows the state's facts (holds, plan instance, start registry); baseline and after snapshots plus screenshots recorded (evidence order: `none`)
2. **Fixture consistency**
   - What: Counts agree across resources: overview groupings, epic counts, findings counts, cleanup summary and details, role counts (the fixture rows of CC-F-09).
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:junit-core
   - Done when:
     - `FixtureConsistencyTest` fails on each pair of CC-F-09 before the fixture fix and passes after it (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `running-work`, `projects-and-start`, `reports-and-system`: after each `POST /mock/goto/<state>` and a reload, the view the state concerns shows the state's facts (holds, plan instance, start registry); baseline and after snapshots plus screenshots recorded (evidence order: `none`)
3. **README and scenario descriptions**
   - What: Replace the stale list of bundled scenarios in `pm-mock-runtime/README.adoc` with the six of `primary.txt`; correct the use-case ids in the scenario descriptions (`UC-CTL-6`, `UC-CTL-7`, `UC-DEC-*` do not exist; decisions are UC-PLN-2).
   - Persona: plan-marshall:persona-documenter
   - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation
   - Done when:
     - No scenario description and no README line names a use-case id that does not exist in `plan-marshall-documentation/doc/use-cases/` (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-mock-runtime verify`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- Existing e2e specs and `pm-operator` PTY tests that run on the mock keep passing; a changed fixture value they assert is updated in the same pull request, never weakened.
- Browser check of the rendered output, with the Playwright MCP server of `opencode.json`, following `AGENTS.md` § Browser checks: start the mock (`./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario <scenario> --web-port 7420"`), then `./pm-web-app/start.sh` (read the port from `pm-web-app/target/pm-web-app.port`); the MCP browser starts with an empty profile, so pair again every session with a fresh single-use code (`POST http://127.0.0.1:7420/mock/pairing`, then open `<app>/pair#code=<CODE>`), never through the `Network:` URL; reach a state with `POST /mock/goto/<state>` and a reload. Check each target in `de` and `en`, dark and light, at 1440 × 900 and 390 × 844 (`browser_resize`). Read the page through `browser_snapshot` (the accessibility tree is the evidence of text and structure) and keep one `browser_take_screenshot` per target; the server writes to its `--output-dir` `.plan/temp/playwright-mcp/` (`opencode.json`), so name every file `{plan}-{target}-{before|after}` and never write to the repository root. Take a baseline snapshot BEFORE the change and one AFTER, and record both paths in the progress record. The visible text of a checked view shows no raw enum, state id, `{placeholder}`, ISO time or emoji outside model-written blocks. Stop the app (`./pm-web-app/stop.sh`) and the mock afterwards. If the Playwright MCP server is not available in the run, the browser Done-when clauses are NOT met — a blocker reported as such, never replaced by a unit test or skipped.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/use-cases/plan.adoc (UC-S-01)`, `doc/claude_review/use-cases/proposals.adoc#UC-P-08` — read at `doc/claude_review/use-cases/plan.adoc`
- HYPOTHESIS: most views read domain data from the engine, and `goto` changes only the offered links — confirm/refute at `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` § `goto` handling (verify-at-outline)
- HYPOTHESIS: the e2e specs of `pm-web-e2e` carry their own fixtures and do not depend on the engine fixtures this plan changes — confirm/refute at `pm-web-e2e/tests/control.spec.js` (verify-at-outline)
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/StartRegistry.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-mock-runtime/src/main/resources/scenarios/running-work.json` — present at HEAD c4dc9bb4
- OBSERVED: `pm-mock-runtime/src/main/resources/scenarios/projects-and-start.json` — present at HEAD c4dc9bb4
- OBSERVED: `pm-mock-runtime/src/main/resources/scenarios/reports-and-system.json` — present at HEAD c4dc9bb4
- OBSERVED: `pm-mock-runtime/src/main/resources/scenarios/fully-onboarded.json` — present at HEAD c4dc9bb4
- OBSERVED: `pm-mock-runtime/README.adoc` — present at HEAD c4dc9bb4
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/runtime/ScenarioStateContractTest.java` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/runtime/FixtureConsistencyTest.java` — absent at HEAD c4dc9bb4; created by this plan

## Dependencies and Sequencing

- Depends on: none
- Overlaps with: `mock-cli-fixes` and `cli-pty-dialogue-harness` touch the same engine; they run after this plan
- Adjacent to: none

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-03-mock-scenario-states.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
