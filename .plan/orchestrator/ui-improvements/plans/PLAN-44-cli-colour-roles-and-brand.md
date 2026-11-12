# PLAN-44: Terminal: Style and Glyph Classes, Colour Roles Applied, Brand Line

epic: ui-improvements
workstream: WS-05

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.
> Scope note (cleanup 2026-11-02): 6 deliverables reach the split guard. Kept as one plan because the style classes, the glyph classes and the colour roles share one set of files; the lane may split by deliverable at outline when a pull request would pass 100 files, and says so in the progress record.

## Objective

Add `tui/Style.java` (role → SGR, switched off without a TTY, with `NO_COLOR`, `TERM=dumb` or `console.colour=false`) and `tui/Glyphs.java` (UTF-8 and ASCII tables), forbid raw `ESC[` and glyph literals elsewhere, apply the colour roles in every view, printed tree and dialogue, and add the product name to every full-screen header and the console greeting (CLI-VD-S-02, CLI-VD-S-03, CLI-VD-S-05; CLI-VD-P-01, CLI-VD-P-02, CLI-VD-P-05). The classes were the former `PLAN-38` (superseded by the structure review of 2026-10-30): they had no consumer but this plan, and both passes touch every view package.

Source of this plan (read before scoping):

- `doc/claude_review/cli/visual-design/plan.adoc#CLI-VD-S-03`
- `doc/claude_review/cli/visual-design/plan.adoc#CLI-VD-S-05`
- `doc/claude_review/cli/visual-design/proposals.adoc#CLI-VD-P-05`

## Deliverables

1. **Style** (from the former `PLAN-38`, superseded: its classes had no consumer but this plan)
   - What: Five roles and two weights on the 16 ANSI colours; no background colours except the selection; `tui/Style.java` (role → SGR, switched off without a TTY, with `NO_COLOR`, `TERM=dumb` or `console.colour=false`).
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:javadoc, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-dev-java-cui:cui-logging
   - Done when:
     - `StyleTest`: each role's SGR and every switch-off rule (evidence order: `test-first`)
     - Terminal check (screen capture) shows this deliverable's change as specified on: `running-work`: overview and plan view with `LANG=C` (no byte above `0x7F`), with `NO_COLOR=1` and with `TERM=dumb` (attributes); before and after captures recorded (evidence order: `none`)
2. **Glyphs** (from the former `PLAN-38`)
   - What: `tui/Glyphs.java`: the UTF-8 table and the ASCII table of CLI-VD-P-02; ASCII when the locale is not UTF-8.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-java:junit-integration, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
   - Done when:
     - `GlyphsTest`; with `LANG=C` every view writes no byte above `0x7F` (`TuiCapabilityIT`) (evidence order: `test-first`)
     - Terminal check (screen capture) shows this deliverable's change as specified on: `running-work`: overview and plan view with `LANG=C` (no byte above `0x7F`), with `NO_COLOR=1` and with `TERM=dumb` (attributes); before and after captures recorded (evidence order: `none`)
3. **Guard** (from the former `PLAN-38`)
   - What: A test fails on a raw `ESC[` or a glyph literal outside `Style` and `Glyphs`; existing users moved.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:javadoc, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-dev-java-cui:cui-logging
   - Done when:
     - The guard fails on a planted literal (evidence order: `test-first`)
     - Terminal check (screen capture) shows this deliverable's change as specified on: `running-work`: overview and plan view with `LANG=C` (no byte above `0x7F`), with `NO_COLOR=1` and with `TERM=dumb` (attributes); before and after captures recorded (evidence order: `none`)
4. **Attributes in AnsiScreen**
   - What: `AnsiScreen` keeps each cell's attribute (bold, dim, reverse, colour index).
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:javadoc, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-dev-java-cui:cui-logging
   - Done when:
     - `AnsiScreenTest` reads attributes back (evidence order: `test-first`)
     - Terminal check (screen capture) shows this deliverable's change as specified on: `running-work` at 120 × 30: overview, plan view, epic, reports, system and the console greeting — the attributes of `!`, `✓`, `failed`, header, details; the brand line; before and after captures recorded (evidence order: `none`)
5. **Roles applied**
   - What: Overview, plan view, epic, projects, reports, system, printed trees and dialogues (`✓` green, `✗` red, the step line and the prompt question bold); expected screens updated.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-java:junit-integration, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
   - Done when:
     - `TuiCapabilityIT` in `running-work`: `!` cells yellow, `✓` green, `failed` red, header bold, details dim; `NO_COLOR=1` writes no colour; `TERM=dumb` no attribute (evidence order: `test-first`)
     - Terminal check (screen capture) shows this deliverable's change as specified on: `running-work` at 120 × 30: overview, plan view, epic, reports, system and the console greeting — the attributes of `!`, `✓`, `failed`, header, details; the brand line; before and after captures recorded (evidence order: `none`)
6. **Brand line**
   - What: `plan-marshall  Running work · … ● connected` in every full-screen header; the greeting `plan-marshall 0.9.1 · runtime running`.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-java:junit-integration, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
   - Done when:
     - `TuiCapabilityIT` and the console transcript (evidence order: `test-first`)
     - Terminal check (screen capture) shows this deliverable's change as specified on: `running-work` at 120 × 30: overview, plan view, epic, reports, system and the console greeting — the attributes of `!`, `✓`, `failed`, header, details; the brand line; before and after captures recorded (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-operator verify -Pintegration-tests`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- CUI standards (`CLAUDE.md`, `PLAN-77`): a test that takes input data draws it from `Generators` with `@EnableGeneratorController` (the expected value of a contract stays literal); a log call at INFO or above goes through a `LogRecord` of the module's own `LogMessages` class (one per module that logs, never one for the repository) with a `LogAsserts` test; `@NullMarked` applies in a module that declares JSpecify (`pm-operator`, `pm-mock-runtime`); until `PLAN-77` has landed the test-scope CUI dependencies are missing in `pm-api`, `pm-exec`, `pm-relay` and `pm-mock-runtime`, and a test there follows plain JUnit and reports the departure, never adds the dependency itself.
- Java 25, Lombok, records; no generic `Exception`/`RuntimeException`; JUnit 5 only, no Mockito (CLAUDE.md).
- Every new operator text goes through `MessageCatalogue` into `operator.en.properties` and `operator.de.properties`.
- Terminal check of the rendered output through the screen capture harness of `cli-pty-dialogue-harness` (`ScreenCaptureIT`, command line in `doc/design/terminal-client/27-tui-testing.adoc`): write this plan's captures to `pm-operator/src/test/resources/screen-captures/{plan}.txt`, run the set BEFORE the change and copy `pm-operator/target/screens/{plan}/` to `.plan/temp/terminal/{plan}/before/`, then run it AFTER and copy to `.../after/`. Capture at 120 × 30 and 80 × 24 in `en_US.UTF-8` and `de_DE.UTF-8` unless the target names other sizes or environments. Read every `.txt` and `.attrs.txt` with the file tools and compare them with the deliverable; the screen text shows no raw id, link name, state id, `-` placeholder, ISO time or HTTP status, and no text of a previous level. Never run `pm-operator` in the agent's own shell: the operator gate refuses a coding agent by design (`PM-SEC-3`) and is never worked around; only the harness, which clears the environment like every PTY test, runs it. If the harness is not available, the terminal Done-when clauses are NOT met — a blocker, never skipped.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/cli/visual-design/plan.adoc#CLI-VD-S-03`, `doc/claude_review/cli/visual-design/plan.adoc#CLI-VD-S-05` — read at `doc/claude_review/cli/visual-design/plan.adoc`
  - verdict: corroborated | checked_at: cc0d62af | by: ui-improvements/cleanup | rescoped: n/a | evidence: visual-design/plan.adoc:29-30,50-51; proposals.adoc:120-121
- Re-grounded note at HEAD 94bbefe1 (cleanup 2026-11-02): Style.java, Glyphs.java and their tests are still absent in pm-operator tui/; tui/ now also holds TuiScreenDriver, TuiLayout, TuiMenu etc. and operator/system sources changed (PLAN-26); transcript/ now has IT classes (CancelDialogueIT, SummaryLineIT, PseudoTerminal, PtySession); Expected Surface needs no removal.
  - verdict: corroborated | checked_at: cc0d62af | by: ui-improvements/cleanup | rescoped: n/a | evidence: Style/Glyphs/tests absent; named tui/transcript classes present
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.
  - verdict: unverifiable | checked_at: cc0d62af | by: ui-improvements/cleanup | rescoped: n/a | evidence: verify-first names a HYPOTHESIS that does not exist

## Expected Surface

- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/tui/Style.java` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/tui/Glyphs.java` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/tui/StyleTest.java` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/tui/GlyphsTest.java` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/tui/RawEscapeGuardTest.java` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/tui/` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/tui/AnsiScreen.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/tui/AnsiScreenTest.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/overview/` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/planview/` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/epicview/` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/reports/` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/system/` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/projects/` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/PmConsole.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ActionSupport.java` — present at HEAD aa3a9be3
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/AnswerCommand.java` — present at HEAD aa3a9be3
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/CleanupCommand.java` — present at HEAD aa3a9be3
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ClientCommand.java` — present at HEAD aa3a9be3
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/EpicCommand.java` — present at HEAD aa3a9be3
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/PlanCommand.java` — present at HEAD aa3a9be3
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ReportsCommand.java` — present at HEAD aa3a9be3
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/RuntimeCommand.java` — present at HEAD aa3a9be3
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ConsolePrompt.java` — present at HEAD aa3a9be3
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/overview/OverviewFormatting.java` — present at HEAD aa3a9be3
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/overview/ProjectWorkLevel.java` — present at HEAD aa3a9be3
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/tui/TuiLayout.java` — present at HEAD aa3a9be3
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/tui/TuiScreenDriver.java` — present at HEAD aa3a9be3
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/StatusCommand.java` — present at `cc0d62af` (added by the cleanup of 2026-11-10)
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ConsoleNotice.java` — present at `cc0d62af` (added by the cleanup of 2026-11-10)
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ConsoleBadge.java` — present at `cc0d62af` (added by the cleanup of 2026-11-10)
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ClientEnvironment.java` — present at `cc0d62af` (added by the cleanup of 2026-11-10)
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/tui/TuiRow.java` — present at `cc0d62af` (added by the cleanup of 2026-11-10)
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/tui/TuiKeyRegistry.java` — present at `cc0d62af` (added by the cleanup of 2026-11-10)
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/process/NodeState.java` — present at `cc0d62af` (added by the cleanup of 2026-11-10)
- OBSERVED: `pm-api/src/main/resources/messages/system.en.properties` — present at `cc0d62af` (added by the cleanup of 2026-11-10)
- OBSERVED: `doc/design/terminal-client/03-dialogues.adoc` — present at `cc0d62af` (added by the cleanup of 2026-11-10)
- OBSERVED: `doc/design/terminal-client/05-transcripts-entry.adoc` — present at `cc0d62af` (added by the cleanup of 2026-11-10)
- OBSERVED: `doc/design/terminal-client/15-overview.adoc` — present at `cc0d62af` (added by the cleanup of 2026-11-10)
- OBSERVED: `doc/design/terminal-client/04-console.adoc` — present at `cc0d62af` (added by the cleanup of 2026-11-10)

## Dependencies and Sequencing

- Depends on: `PLAN-07` (`cli-terminal-design-docs`, shipped), `PLAN-34` (`cli-display-layer`), `PLAN-15` (`cli-footer-and-help-overlay`, shipped)
- Overlaps with: touches every view package: runs after `cli-display-layer`; `PLAN-50` (the column grid) follows it
- Adjacent to: none

## Re-grounding 2026-11-07 (cleanup)

Checked at `aa3a9be3`. `tui/Style.java`, `Glyphs.java` and `RawEscapeGuardTest` are absent; the only raw `ESC[` literals are the PAGE_UP/PAGE_DOWN constants in `TuiKeyRegistry:124-125`; glyph literals (✓ ✗ ▸ ◇ ● ○) sit in the command classes added to the surface. No brand line or `plan-marshall 0.9.1` greeting exists. The Objective's "dialogues" are those command classes, not only the view packages.

## Re-grounding 2026-11-10 (cleanup, at `cc0d62af`)

This section supersedes the spec text and earlier notes where they differ.

- **D4 "Attributes in AnsiScreen" is DONE** (PLAN-02 #232 `1864d8a3`: `AnsiScreen` `CellStyle`, `AnsiScreenTest:78-84`,
  `ScreenCaptureIT:157,167` `.attrs.txt`): drop it; keep its terminal check inside the roles deliverable.
- **Split recommended (6 deliverables, ~35 files):** 44a Style, Glyphs, guard and migration (check: `LANG=C`,
  `NO_COLOR`, `TERM=dumb` captures); 44b roles applied with a styled-segment row model, brand line and greeting, docs.
- **OPEN DESIGN DECISIONS (operator):** (1) ASCII of ✓: `ok` (`03-dialogues.adoc:349`) vs `+` (`shared/plan-view.adoc:56-61`,
  `NodeState.asciiGlyph`); (2) "line drawing keeps the terminal's line set" (`03-dialogues.adoc:322`) vs "ASCII only /
  no byte above 0x7F under LANG=C"; and the German catalogue under `LANG=C`; (3) kind tints plan = cyan, epic = magenta
  (`shared/plan-overview.adoc:114-124`) vs five roles with cyan = running (`03-dialogues.adoc:282-315`); (4) the greeting:
  `plan-marshall <version> · runtime running` (`15-overview.adoc:54`) vs `✓ Runtime running (pm-mcpd 0.9.0)`
  (`05-transcripts-entry.adoc`, `operator.en.properties:127,152`, `EntryFlow:143`); (5) `console.colour` has no definition
  (only `03-dialogues.adoc:320`).
- Glyph table gaps: `‖` (paused), `◆`, `▪`, `–`, `—`, `•`, `!`, the box-drawing set; no spinner exists. A second glyph
  table exists in pm-api `NodeState` (used by `PlanCommand:369`, `PlanViewLevel:600,609,656-663`): absorb it.
- Rows are one plain string (`TuiRow`; `TuiLayout:384,435` inverts whole lines): colouring cells needs a styled-segment
  row model — state the decision (PLAN-50 depends on it).
- Guard (D3): detect `\uXXXX` escapes of glyph code points; scope to `src/main`; exempt input-key constants
  (`TuiKeyRegistry:119,124-125`, `TuiScreenDriver:555`) and say what OSC literals do (`PmConsole:523,851`,
  `TerminalTitle:23`, `OverviewFormatting:360`).
- Surface: ~35 more files (commands, dialogues, epicview/planview/overview/projects levels, `ActionMenus`) — the
  paths added below are the shared seams; the per-command sites are re-listed when amending after PLAN-123.
- Stale: the PLAN-77 CUI clause and the verify-first "HYPOTHESIS above" (none exists).
- Sequencing: strictly after PLAN-123 (multi-file overlap) and after the PLAN-36 residue plan (NeedsCommand,
  OverviewFormatting, ConsoleNotice, TuiKeyRegistry footer).
- Disposition: AMEND and SPLIT after the operator's five design decisions; HOLD for PLAN-123.

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-44-cli-colour-roles-and-brand.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.

## Merge Rule (Amendment 2026-11-12)

Added by the orchestrator after PLAN-32 (#415) merged without an operator question because its spec had no merge clause. It supersedes
any text here that differs.

- The pull request is merged only after at least one CodeRabbit review of some head with its findings handled. A lane's "standing review
  authorization" does not replace it. If CodeRabbit refuses on quota, stop and ask the operator.
- The run merges only when every review thread has a reply and is resolved, and every review body item (outside-diff,
  nitpick, "additional comments") has a reply on the PR, not only a fix.
- The pull request body states the thread count, the resolved count and the review-body items with their count
  (`unresolved_review_threads=`, `review_body_findings_total=`, `review_body_findings_handled=`), read through `ci pr comments`.
- The run merges the pull request itself once these gates hold, without a further word of the operator (`CLAUDE.md`, Git Workflow, #423). Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`. The pull
  request body carries no generator footer.
