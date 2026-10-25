# PLAN-02: Terminal: PTY Tests for Every Dialogue That Acts

epic: ui-improvements
workstream: WS-04

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Prove the acting dialogues in a real pseudo-terminal: add `/mock/acts` to the mock, add one `*DialogueIT` per acting dialogue with the three cases (`q` first sends nothing; unknown input re-asks; the main path arrives), and count the dialogue paths in the coverage gate (CLI-CC-S-01; CLI-CC-P-07).

Source of this plan (read before scoping):

- `doc/claude_review/cli/cross-cutting/plan.adoc#CLI-CC-S-01`
- `doc/claude_review/cli/cross-cutting/proposals.adoc#CLI-CC-P-07`
- `doc/claude_review/cli/use-cases/analysis.adoc#tests`

## Deliverables

1. **`/mock/acts`**
   - What: The mock records every operator act since the last reset and serves it at `GET /mock/acts`.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:junit-core
   - Done when:
     - A mock test sends two acts and reads them back; a reset empties the list (evidence order: `test-first`)
     - Terminal check (screen capture) shows this deliverable's change as specified on: the `baseline` set: `status`, `plans`, the plan view of `fully-onboarded`; before and after captures recorded (evidence order: `none`)
2. **Dialogue ITs**
   - What: `PauseDialogueIT`, `CancelDialogueIT`, `AnswerDialogueIT`, `OnboardingDialogueIT`, `ProjectSetupDialogueIT`, `CleanupDialogueIT`, `RuntimeStopDialogueIT` on `PtySession` against `MockRuntime`, each with the three cases, reading the act from `/mock/acts`.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-java:junit-integration, pm-dev-java:junit-core
   - Done when:
     - Each IT is seen failing for its known defect; the ones the current code fails carry `@Disabled` naming the plan that fixes them, the rest run green (evidence order: `test-first`)
     - Terminal check (screen capture) shows this deliverable's change as specified on: the `baseline` set: `status`, `plans`, the plan view of `fully-onboarded`; before and after captures recorded (evidence order: `none`)
3. **Coverage of dialogue paths**
   - What: Mark the IT classes so their dialogue paths count in the 80 % gate of the module.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-java:junit-integration, pm-dev-java:junit-core
   - Done when:
     - `./mvnw -pl pm-operator verify -Pcoverage,integration-tests` reports the dialogue classes (evidence order: `none`)
     - Terminal check (screen capture) shows this deliverable's change as specified on: the `baseline` set: `status`, `plans`, the plan view of `fully-onboarded`; before and after captures recorded (evidence order: `none`)
4. **Screen capture for review**
   - What: `ScreenCaptureIT` on `PtySession` against `MockRuntime` runs a named capture set from `pm-operator/src/test/resources/screen-captures/{set}.txt` (one capture per line: name, scenario, mock states to `goto`, command, keys to send, columns × rows, locale, extra environment such as `TERM=dumb`, `NO_COLOR=1`, `LANG=C`) and writes per capture `pm-operator/target/screens/{set}/{name}.txt` (the screen text) and `{name}.attrs.txt` (per row the cell attributes bold, dim, reverse and colour index, from `AnsiScreen`), plus the stdout of a printed verb. It is selected by a property (for example `-Dpm.screens={set}`) and skipped without it; the environment is cleared like every PTY test, with the comment naming `PM-SEC-3`. The command line is documented in `doc/design/terminal-client/27-tui-testing.adoc`.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-java:junit-integration, pm-dev-java:junit-core
   - Done when:
     - A capture set `baseline` (`status`, `plans`, the plan view of `fully-onboarded` at 120 × 30 and 80 × 24, `en_US.UTF-8` and `de_DE.UTF-8`) writes its files; a planted wrong expectation in a sample review is caught by reading them (evidence order: `test-first`)
     - Terminal check (screen capture) shows this deliverable's change as specified on: the `baseline` set: `status`, `plans`, the plan view of `fully-onboarded`; before and after captures recorded (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-operator verify -Pintegration-tests`, `./mvnw -pl pm-mock-runtime verify`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- Java 25, Lombok, records; no generic `Exception`/`RuntimeException`; JUnit 5 only, no Mockito (CLAUDE.md).
- Every new operator text goes through `MessageCatalogue` into `operator.en.properties` and `operator.de.properties`.
- The review expects these ITs to fail today. They are committed `@Disabled` with the name of the plan that fixes them, so the build stays green; each fixing plan removes its `@Disabled` (orchestrator decision, recorded in the epic).
- Terminal check of the rendered output through the screen capture harness of `cli-pty-dialogue-harness` (`ScreenCaptureIT`, command line in `doc/design/terminal-client/27-tui-testing.adoc`): write this plan's captures to `pm-operator/src/test/resources/screen-captures/{plan}.txt`, run the set BEFORE the change and copy `pm-operator/target/screens/{plan}/` to `.plan/temp/terminal/{plan}/before/`, then run it AFTER and copy to `.../after/`. Capture at 120 × 30 and 80 × 24 in `en_US.UTF-8` and `de_DE.UTF-8` unless the target names other sizes or environments. Read every `.txt` and `.attrs.txt` with the file tools and compare them with the deliverable; the screen text shows no raw id, link name, state id, `-` placeholder, ISO time or HTTP status, and no text of a previous level. Never run `pm-operator` in the agent's own shell: the operator gate refuses a coding agent by design (`PM-SEC-3`) and is never worked around; only the harness, which clears the environment like every PTY test, runs it. If the harness is not available, the terminal Done-when clauses are NOT met — a blocker, never skipped.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/cli/cross-cutting/plan.adoc#CLI-CC-S-01`, `doc/claude_review/cli/cross-cutting/proposals.adoc#CLI-CC-P-07` — read at `doc/claude_review/cli/cross-cutting/plan.adoc`
- OBSERVED: `PtySession`, `TuiCapabilityIT` and `TranscriptDialogueIT` exist — read at `pm-operator/src/test/java/de/planmarshall/operator/transcript/`
- HYPOTHESIS: the mock has no act log today — confirm/refute at `pm-mock-runtime/src/main/java/de/planmarshall/mock/http/` (verify-at-outline)
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/http/` — present at HEAD c4dc9bb4
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/pom.xml` — present at HEAD c4dc9bb4
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/runtime/` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/ScreenCaptureIT.java` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-operator/src/test/resources/screen-captures/` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `doc/design/terminal-client/27-tui-testing.adoc` — present at HEAD c4dc9bb4

## Dependencies and Sequencing

- Depends on: none
- Overlaps with: `mock-scenario-states` and `mock-cli-fixes` touch the same engine (one file shared at most)
- Adjacent to: none

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-02-cli-pty-dialogue-harness.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
