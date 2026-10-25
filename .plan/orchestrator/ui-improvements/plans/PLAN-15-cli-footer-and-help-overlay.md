# PLAN-15: Terminal: Clean Rows, a Footer and a Help That Never Lie

epic: ui-improvements
workstream: WS-05

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Clear every row of a frame so no text of the previous level remains, fill the footer by priority ending with `? more`, and rebuild the help overlay as a box with all bindings (CLI-UX-S-01, CLI-UX-S-02; CLI-UX-P-02).

Source of this plan (read before scoping):

- `doc/claude_review/cli/ux/plan.adoc#CLI-UX-S-01`
- `doc/claude_review/cli/ux/plan.adoc#CLI-UX-S-02`
- `doc/claude_review/cli/ux/proposals.adoc#CLI-UX-P-02`

## Deliverables

1. **Clear every row**
   - What: `TuiScreen` pads or erases every row of a written frame.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:junit-core
   - Done when:
     - `TuiScreenTest.levelChangeLeavesNoResidue` on `VirtualScreenTerminal` (evidence order: `test-first`)
2. **Footer by priority**
   - What: `TuiKeyRegistry.Binding` has a priority; the footer fills the width by priority, ends with `? more`, never cuts a key.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:junit-core
   - Done when:
     - `TuiKeyRegistryTest`: at 80 columns `a`, `Enter`, `Esc`, `?`, `q` stay (evidence order: `test-first`)
3. **Help overlay**
   - What: A box with grouped bindings (move, open, act, views, leave) and the row details; the first `Esc` closes it; the footer shows `Esc close`.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-java:junit-integration, pm-dev-java:junit-core
   - Done when:
     - `TuiCapabilityIT.helpOverlayShowsAllKeys` and `footerHasNoResidue` per level (evidence order: `test-first`)
4. **Mockups**
   - What: Every footer mockup of `15-overview.adoc` shows `? help` and `q leave`.
   - Persona: plan-marshall:persona-documenter
   - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation
   - Done when:
     - The document passes `doc-review` (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-operator verify -Pintegration-tests`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- Java 25, Lombok, records; no generic `Exception`/`RuntimeException`; JUnit 5 only, no Mockito (CLAUDE.md).
- Every new operator text goes through `MessageCatalogue` into `operator.en.properties` and `operator.de.properties`.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/cli/ux/plan.adoc#CLI-UX-S-01`, `doc/claude_review/cli/ux/plan.adoc#CLI-UX-S-02` — read at `doc/claude_review/cli/ux/plan.adoc`
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/tui/TuiScreen.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/tui/TuiKeyRegistry.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/tui/TuiLayout.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/tui/TuiScreenDriver.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/tui/TuiKeyRegistryTest.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/tui/TuiScreenTest.java` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/TuiCapabilityIT.java` — present at HEAD c4dc9bb4
- OBSERVED: `doc/design/terminal-client/15-overview.adoc` — present at HEAD c4dc9bb4

## Dependencies and Sequencing

- Depends on: `PLAN-02` (`cli-pty-dialogue-harness`), `PLAN-05` (`cli-summary-line-and-start-script`)
- Overlaps with: `cli-summary-line-and-start-script` touches `TuiScreenDriver.java`: runs after it
- Adjacent to: none

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-15-cli-footer-and-help-overlay.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
