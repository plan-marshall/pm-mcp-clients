# PLAN-50: Terminal: One Column Grid, Say It Once

epic: ui-improvements
workstream: WS-05

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Lay every row of the overview, plan view, reports and system view on one grid (marker, need, state, kind and title, facts) and remove the repeats (CLI-VD-S-04; CLI-VD-P-03, CLI-VD-P-04).

Source of this plan (read before scoping):

- `doc/claude_review/cli/visual-design/plan.adoc#CLI-VD-S-04`
- `doc/claude_review/cli/visual-design/proposals.adoc#CLI-VD-P-03`
- `doc/claude_review/cli/visual-design/proposals.adoc#CLI-VD-P-04`
- `doc/claude_review/cli/visual-design/findings.adoc`

## Deliverables

1. **RowLayout**
   - What: `RowLayout` with the five columns; task rows get the state column; the system view a label and a sub-label column; truncation cuts facts first, the title last, never ids.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:javadoc, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-dev-java-cui:cui-logging
   - Done when:
     - `*FullScreenTest`: task and phase states start in one column (evidence order: `test-first`)
     - Terminal check (screen capture) shows this deliverable's change as specified on: `running-work` at 120 × 30, 100 × 30 and 80 × 24: plan view (task and phase state columns), overview, system, reports; before and after captures recorded (evidence order: `none`)
2. **Say it once**
   - What: Remove the repeats of CLI-VD-F-04; transcripts updated.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:javadoc, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-dev-java-cui:cui-logging
   - Done when:
     - No row repeats a fact of its own row or its details line (transcripts) (evidence order: `test-first`)
     - Terminal check (screen capture) shows this deliverable's change as specified on: `running-work` at 120 × 30, 100 × 30 and 80 × 24: plan view (task and phase state columns), overview, system, reports; before and after captures recorded (evidence order: `none`)

## Standing Constraints

- ZERO Sonar findings (the operator, 2026-11-05): the pull request merges with 0 open new-code issues (SonarCloud project `plan-marshall_pm-mcp-clients`), read at the pull request head after the push (`unknown` for a failed read, never `0`); every open issue is fixed in a commit of THIS pull request, and a dismissal is made only on the operator's selection. State `sonar_open_new_issues=N` in the landing message. This is a merge gate, not a deliverable's Done-when.
- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-operator verify -Pintegration-tests`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- CUI standards (`CLAUDE.md`, `PLAN-77`): a test that takes input data draws it from `Generators` with `@EnableGeneratorController` (the expected value of a contract stays literal); a log call at INFO or above goes through a `LogRecord` of the module's own `LogMessages` class (one per module that logs, never one for the repository) with a `LogAsserts` test; `@NullMarked` applies in a module that declares JSpecify (`pm-operator`, `pm-mock-runtime`); until `PLAN-77` has landed the test-scope CUI dependencies are missing in `pm-api`, `pm-exec`, `pm-relay` and `pm-mock-runtime`, and a test there follows plain JUnit and reports the departure, never adds the dependency itself.
- Java 25, Lombok, records; no generic `Exception`/`RuntimeException`; JUnit 5 only, no Mockito (CLAUDE.md).
- Every new operator text goes through `MessageCatalogue` into `operator.en.properties` and `operator.de.properties`.
- Terminal check of the rendered output through the screen capture harness of `cli-pty-dialogue-harness` (`ScreenCaptureIT`, command line in `doc/design/terminal-client/27-tui-testing.adoc`): write this plan's captures to `pm-operator/src/test/resources/screen-captures/{plan}.txt`, run the set BEFORE the change and copy `pm-operator/target/screens/{plan}/` to `.plan/temp/terminal/{plan}/before/`, then run it AFTER and copy to `.../after/`. Capture at 120 × 30 and 80 × 24 in `en_US.UTF-8` and `de_DE.UTF-8` unless the target names other sizes or environments. Read every `.txt` and `.attrs.txt` with the file tools and compare them with the deliverable; the screen text shows no raw id, link name, state id, `-` placeholder, ISO time or HTTP status, and no text of a previous level. Never run `pm-operator` in the agent's own shell: the operator gate refuses a coding agent by design (`PM-SEC-3`) and is never worked around; only the harness, which clears the environment like every PTY test, runs it. If the harness is not available, the terminal Done-when clauses are NOT met — a blocker, never skipped.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/cli/visual-design/plan.adoc#CLI-VD-S-04`, `doc/claude_review/cli/visual-design/proposals.adoc#CLI-VD-P-03` — read at `doc/claude_review/cli/visual-design/plan.adoc`
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: visual-design/plan.adoc:40-41 CLI-VD-S-04; proposals.adoc:76-77 P-03 and :112 P-04
- Re-grounded note at HEAD 94bbefe1 (cleanup 2026-11-02): RowLayout still not created: operator/layout/ holds only OperatorLayout.java and package-info; system/ now has SystemView, SystemLevel, SystemFormatting from PLAN-26 (spec's system view grid work must start from those); transcript/ resources now include console-settings, plan-view, status etc. already updated by PLAN-26/PLAN-91.
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: RowLayout absent everywhere; layout/ holds only OperatorLayout and package-info (installation layout); system/ has SystemView, SystemLevel, SystemFormatting. RowLayout belongs in tui/
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.
  - verdict: unverifiable | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: process clause

## Expected Surface

- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/layout/` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/tui/TuiRow.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/overview/` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/planview/` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/reports/` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/system/` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/test/resources/transcript/` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/tui/TuiLayout.java` — present at HEAD aa3a9be3
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/overview/ProjectsOverviewLevel.java` — present at `cc0d62af` (added by the cleanup of 2026-11-10)
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/system/SystemLevel.java` — present at `cc0d62af` (added by the cleanup of 2026-11-10)
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/reports/ReportsLevel.java` — present at `cc0d62af` (added by the cleanup of 2026-11-10)
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/tui/TuiLayoutTest.java` — present at `cc0d62af` (added by the cleanup of 2026-11-10)
- OBSERVED: `doc/design/terminal-client/15-overview.adoc` — present at `cc0d62af` (added by the cleanup of 2026-11-10)
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/tui/RowLayout.java` — absent at `dc811fb3`; created by this plan (added by the cleanup of 2026-11-14)

## Dependencies and Sequencing

- Depends on: `PLAN-44` (`cli-colour-roles-and-brand`)
- Overlaps with: none known at staging
- Adjacent to: none

## Re-grounding 2026-11-07 (cleanup)

Checked at `aa3a9be3`. `RowLayout` appears nowhere (`operator/layout/` holds `OperatorLayout` and `package-info` only). The epic and projects rows (`epicview/`, `projects/`) also use the grid beyond the four views the Objective lists; the `*FullScreenTest` classes exist.

## Re-grounding 2026-11-10 (cleanup, at `cc0d62af`)

This section supersedes the spec text and earlier notes where they differ.

- **Wrong package:** `layout/` is the INSTALLATION layout (`layout/package-info.java`: resolving `<PM_MCP_HOME>`); put
  `RowLayout` in `tui/` beside `TuiLayout`/`TuiRow`.
- **D2 narrower:** fixed already — the system jobs line and the double "not measured" (PLAN-26 #299, `SystemView.java:
  369-370,473,508-515`), "ended ended" (PLAN-23 #381, `CleanupCommand:707-709`). Left: `done · success`
  (`plan.leaf.task`, `PlanViewFormatting.taskLine`), "2 of 3 tasks done" twice (`PlanViewLevel:575,625`), the report age
  twice (`ReportsLevel:198` and its details line).
- **OPEN DESIGN CONFLICT:** `15-overview.adoc:61-90` fixes need 14 (cut to 12) and state 12, but closed state words are
  longer (`waiting for you` 15+2, `pausing after the task` 22, `running · run 3`, German `Landen fehlgeschlagen` 21), and
  the design's own wireframes (`:98-127`) put the state in the facts area; 80-column behaviour is undefined. Settle by a
  `15-overview.adoc` amendment (design deliverable) before implementing.
- `‖` (paused) belongs in PLAN-44's glyph table; state glyphs come from `NodeState`. Existing grids to unify:
  `ProjectWorkLevel:331`, `ProjectsOverviewLevel:182` (PLAN-36 needs rows), `ReportsLevel:198`, `SystemView:360,492`.
- Needs PLAN-44's styled-segment row model. Stale: the PLAN-77 clause, the verify-first clause.
- Sequencing: after PLAN-123 (multi-file) and PLAN-44.
- Disposition: AMEND (design amendment of the grid); HOLD for PLAN-44.

## Re-Grounding 2026-11-14 (cleanup)

Verdicts persisted per claim at `dc811fb3` (`corpus set-verdict`, by `ui-improvements/cleanup`). Read before the outline:

- Already done: the system view's double "not measured" (#299). `RowLayout` belongs in `tui/` (absent).

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-50-cli-column-grid.md
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

## PLAN-127 Landing Residue (Amendment 2026-11-13)

- Re-ground at `98cea294`: the overview next-page row is this plan's (#416 reply: "PLAN-50 owns the rows"); `rowIdOf` identity is now
  kind, link, place and `since`; `OverviewFullScreenApp` +242 and `ProjectsOverviewLevel` changed. The startup-notice baseline seeds from the
  first needs page only (`OverviewFullScreenApp.java:526-535`, mock page size 20): with paging, seed from every page.
