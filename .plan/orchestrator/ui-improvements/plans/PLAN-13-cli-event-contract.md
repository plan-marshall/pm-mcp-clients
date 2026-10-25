# PLAN-13: Terminal: One Event Contract, Pushed Notices, Badges

epic: ui-improvements
workstream: WS-04

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Make pushed notices reach the console: map the SSE event name to a notice through one table in `pm-api`, add the "answered elsewhere" notice, and refresh the prompt badges from `needs_operator` (CLI-UC-S-05; CLI-UC-P-04).

Source of this plan (read before scoping):

- `doc/claude_review/cli/use-cases/plan.adoc#CLI-UC-S-05`
- `doc/claude_review/cli/use-cases/proposals.adoc#CLI-UC-P-04`

## Deliverables

1. **Event kinds in pm-api**
   - What: A table of event names and notice kinds in `pm-api` (extend `event/EventName.java` or add `events/EventKind.java`, whichever the code supports); `ConsoleNotice.fromEventMap` takes the event name first, `type` only as a fallback.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:junit-core
   - Done when:
     - `ConsoleNoticeTest` with named events (`decision.opened`, `decision.answered`, `hold.opened`, `landing.failed`, `report.sent`) (evidence order: `test-first`)
     - Terminal check (screen capture) shows this deliverable's change as specified on: the console in `fully-onboarded` (prompt badge) and after a `POST /mock/broadcast` of `decision.opened` and of `decision.answered` from `web`; before and after captures recorded (evidence order: `none`)
2. **Answered elsewhere and badges**
   - What: The notice "– Q-007 was answered in the web app (iPad): Rebase onto main. Nothing was sent from here." with the option label; badges `pm [3 questions · 1 hold] ›` from `needs_operator`, at most three then `+N`, refreshed per event.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-java:junit-integration, pm-dev-java:junit-core
   - Done when:
     - `ConsoleNoticeIT` broadcasts through `/mock/broadcast` and finds the notice above the prompt (evidence order: `test-first`)
     - Terminal check (screen capture) shows this deliverable's change as specified on: the console in `fully-onboarded` (prompt badge) and after a `POST /mock/broadcast` of `decision.opened` and of `decision.answered` from `web`; before and after captures recorded (evidence order: `none`)
3. **Web alignment note**
   - What: Write in the pull request which web event handling should read the same table; the web change itself is not part of this plan.
   - Persona: plan-marshall:persona-documenter
   - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation
   - Done when:
     - The pull request names the web files that read event names (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-operator verify -Pintegration-tests`, `./mvnw -pl pm-api verify`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- Java 25, Lombok, records; no generic `Exception`/`RuntimeException`; JUnit 5 only, no Mockito (CLAUDE.md).
- Every new operator text goes through `MessageCatalogue` into `operator.en.properties` and `operator.de.properties`.
- Terminal check of the rendered output through the screen capture harness of `cli-pty-dialogue-harness` (`ScreenCaptureIT`, command line in `doc/design/terminal-client/27-tui-testing.adoc`): write this plan's captures to `pm-operator/src/test/resources/screen-captures/{plan}.txt`, run the set BEFORE the change and copy `pm-operator/target/screens/{plan}/` to `.plan/temp/terminal/{plan}/before/`, then run it AFTER and copy to `.../after/`. Capture at 120 × 30 and 80 × 24 in `en_US.UTF-8` and `de_DE.UTF-8` unless the target names other sizes or environments. Read every `.txt` and `.attrs.txt` with the file tools and compare them with the deliverable; the screen text shows no raw id, link name, state id, `-` placeholder, ISO time or HTTP status, and no text of a previous level. Never run `pm-operator` in the agent's own shell: the operator gate refuses a coding agent by design (`PM-SEC-3`) and is never worked around; only the harness, which clears the environment like every PTY test, runs it. If the harness is not available, the terminal Done-when clauses are NOT met — a blocker, never skipped.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/cli/use-cases/plan.adoc#CLI-UC-S-05`, `doc/claude_review/cli/use-cases/proposals.adoc#CLI-UC-P-04` — read at `doc/claude_review/cli/use-cases/plan.adoc`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/event/EventName.java` exists — read at `pm-api/src/main/java/de/planmarshall/api/event/`
- HYPOTHESIS: the console reads `type` from the event data and the runtime sends none — confirm/refute at `pm-operator/src/main/java/de/planmarshall/operator/ConsoleNotice.java` § `fromEventMap` (verify-at-outline)
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/event/EventName.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/event/` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ConsoleNotice.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ConsoleNoticeBuffer.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ConsolePrompt.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ConsoleNoticeTest.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/ConsoleNoticeIT.java` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `doc/design/terminal-client/04-console.adoc` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/operator.en.properties` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/operator.de.properties` — present at HEAD c4dc9bb4

## Dependencies and Sequencing

- Depends on: `PLAN-02` (`cli-pty-dialogue-harness`)
- Overlaps with: none known at staging
- Adjacent to: none

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-13-cli-event-contract.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
