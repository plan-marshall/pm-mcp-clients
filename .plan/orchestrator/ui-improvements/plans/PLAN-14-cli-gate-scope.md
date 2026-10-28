# PLAN-14: Terminal: The Operator Gate on Acts Only

epic: ui-improvements
workstream: WS-05

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Apply the operator gate (`PM-SEC-3`) to acts only, enter the console without it and check it when the console dispatches an act, say honestly what the guard is, and test it per verb (CLI-CC-S-02; CLI-CC-P-05 "now"). The development switch is not built: it waits on the operator's security decision.

Source of this plan (read before scoping):

- `doc/claude_review/cli/cross-cutting/plan.adoc#CLI-CC-S-02`
- `doc/claude_review/cli/cross-cutting/proposals.adoc#CLI-CC-P-05`
- `doc/claude_review/cli/cross-cutting/findings.adoc#CLI-CC-F-07`
- `doc/claude_review/cli/cross-cutting/findings.adoc#CLI-CC-F-08`

## Deliverables

1. **Gate on act paths**
   - What: `gate().verify()` in the commands that send an act (answer, plan and epic acts, enrol, release, unenrol, credentials, config set/unset, cleanup acts, runtime acts, web pair and revoke, accept-edit); removed from read-only paths; the console dispatcher checks acts.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:junit-core
   - Done when:
     - A test per verb: with `CLAUDECODE=1`, read-only verbs run and act verbs refuse with exit `4` (evidence order: `test-first`)
     - Terminal check (screen capture) shows this deliverable's change as specified on: with `CLAUDECODE=1` in the capture environment: `questions`, `reports`, `projects --list`, `plan fix-relay --print` print; `answer Q-007`, `plan pause fix-relay`, `runtime stop`, `cleanup` show the refusal text and exit `4`; before and after captures recorded (evidence order: `none`)
2. **Honest refusal**
   - What: The refusal text through the catalogue: "Operator acts are refused here: this terminal belongs to a coding agent (CLAUDECODE is set). Run pm-operator in a terminal of your own."
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:junit-core
   - Done when:
     - `OperatorGateTest` keeps the marker matrix and asserts the text in `en` and `de` (evidence order: `test-first`)
     - Terminal check (screen capture) shows this deliverable's change as specified on: with `CLAUDECODE=1` in the capture environment: `questions`, `reports`, `projects --list`, `plan fix-relay --print` print; `answer Q-007`, `plan pause fix-relay`, `runtime stop`, `cleanup` show the refusal text and exit `4`; before and after captures recorded (evidence order: `none`)
3. **PTY tests say why**
   - What: The PTY tests that clear the environment carry a comment naming `PM-SEC-3`; the open decision on `PM_OPERATOR_DEV_ALLOW_AGENT` is written in the pull request for the operator.
   - Persona: plan-marshall:persona-documenter
   - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation
   - Done when:
     - The pull request states the decision as open (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-operator verify -Pintegration-tests`, `./mvnw -pl pm-api verify`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- Java 25, Lombok, records; no generic `Exception`/`RuntimeException`; JUnit 5 only, no Mockito (CLAUDE.md).
- Every new operator text goes through `MessageCatalogue` into `operator.en.properties` and `operator.de.properties`.
- Security-relevant: the host rule that denies `pm-operator` to the model stays; no switch that weakens the gate is built here.
- Terminal check of the rendered output through the screen capture harness of `cli-pty-dialogue-harness` (`ScreenCaptureIT`, command line in `doc/design/terminal-client/27-tui-testing.adoc`): write this plan's captures to `pm-operator/src/test/resources/screen-captures/{plan}.txt`, run the set BEFORE the change and copy `pm-operator/target/screens/{plan}/` to `.plan/temp/terminal/{plan}/before/`, then run it AFTER and copy to `.../after/`. Capture at 120 × 30 and 80 × 24 in `en_US.UTF-8` and `de_DE.UTF-8` unless the target names other sizes or environments. Read every `.txt` and `.attrs.txt` with the file tools and compare them with the deliverable; the screen text shows no raw id, link name, state id, `-` placeholder, ISO time or HTTP status, and no text of a previous level. Never run `pm-operator` in the agent's own shell: the operator gate refuses a coding agent by design (`PM-SEC-3`) and is never worked around; only the harness, which clears the environment like every PTY test, runs it. If the harness is not available, the terminal Done-when clauses are NOT met — a blocker, never skipped.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/cli/cross-cutting/plan.adoc#CLI-CC-S-02`, `doc/claude_review/cli/cross-cutting/proposals.adoc#CLI-CC-P-05` — read at `doc/claude_review/cli/cross-cutting/plan.adoc`
  - verdict: corroborated | checked_at: cc5794c9 | by: ui-improvements/cleanup | rescoped: n/a | evidence: origin/main doc/claude_review/cli/cross-cutting/plan.adoc:21-22 [#CLI-CC-S-02] The Gate Scope exists; proposals.adoc:97 [#CLI-CC-P-05]
- HYPOTHESIS: the client calls the gate on read-only verbs (`questions`, `reports`, `projects --list`, views) — confirm/refute at `pm-operator/src/main/java/de/planmarshall/operator/OperatorGate.java` and its callers (verify-at-outline)
  - verdict: corroborated | checked_at: cc5794c9 | by: ui-improvements/cleanup | rescoped: n/a | evidence: QuestionsCommand.java:54, ReportsCommand.java:65, ProjectsCommand.java:68 each call context.gate().verify() unconditionally, incl. read-only/list paths
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/OperatorGate.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/PmOperator.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/PmConsole.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/OperatorContext.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/OperatorGateTest.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/PtySession.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/OperatorGateScopeTest.java` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-api/src/main/resources/messages/operator.en.properties` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/operator.de.properties` — present at HEAD c4dc9bb4

## Dependencies and Sequencing

- Depends on: `PLAN-02` (`cli-pty-dialogue-harness`)
- Overlaps with: none known at staging
- Adjacent to: none

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-14-cli-gate-scope.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
