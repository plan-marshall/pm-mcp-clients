# PLAN-42: Terminal: One Rule for Verb Modes, Help Everywhere

epic: ui-improvements
workstream: WS-05

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

View verbs open full screen in a terminal and print with `--print` or without one; `help [<verb>]` works in shell and console, `--help` on every subcommand, `?` at the prompt (CLI-UX-S-05; CLI-UX-P-04, CLI-UX-P-06).

Source of this plan (read before scoping):

- `doc/claude_review/cli/ux/plan.adoc#CLI-UX-S-05`
- `doc/claude_review/cli/ux/proposals.adoc#CLI-UX-P-04`
- `doc/claude_review/cli/ux/proposals.adoc#CLI-UX-P-06`

## Deliverables

1. **Verb modes**
   - What: `--print` on the view verbs; full screen by default in a terminal; `epic <id>` opens full screen.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:javadoc, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-dev-java-cui:cui-logging
   - Done when:
     - Transcript tests: `epic <id>` full screen; `epic <id> --print` prints (evidence order: `test-first`)
     - Terminal check (screen capture) shows this deliverable's change as specified on: `fully-onboarded`: `help`, `help plan`, `plan --help`, `epic relay-hardening`, `epic relay-hardening --print`; before and after captures recorded (evidence order: `none`)
2. **Help**
   - What: `help [<verb>]` in shell and console, all verbs in groups plus "The process designer is in the web app."; `--help`/`-h` on every subcommand; `?` equals `help` at the prompt.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:javadoc, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-dev-java-cui:cui-logging
   - Done when:
     - A test: `help` lists every verb; `help plan` equals `plan --help` (evidence order: `test-first`)
     - Terminal check (screen capture) shows this deliverable's change as specified on: `fully-onboarded`: `help`, `help plan`, `plan --help`, `epic relay-hardening`, `epic relay-hardening --print`; before and after captures recorded (evidence order: `none`)
3. **Verb-mode table**
   - What: One table in `01-commands.adoc`: verb, terminal mode, printed mode, JSON.
   - Persona: plan-marshall:persona-documenter
   - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation
   - Done when:
     - The document passes `doc-review` (under the runbook lane its checker `trace.py` does not run, so `pm-documents:ref-documentation` `docs review` and `pm-documents:ref-asciidoc` `validate` and `verify-links` stand in for it, clean, as PLAN-06 and PLAN-07 ran them) (evidence order: `none`)

4. **The plural and zero forms of `system.confirm.what` are asserted again, and a refused initial state read of `config <aspect>` is covered (the residue of PLAN-171; the operator's D0: done first)**
   - What: PLAN-171 (#510, `76315c63`) drew every count of `RuntimeActDialogueTest` from 2..9 (`RuntimeActDialogueTest.java:113-118`) and dropped the parent's assertion `"Running now: 3 plans · 1 epic · 4 tasks · 2 jobs · 5 workers"`, so the singular (`one {# epic}`) and zero (`=0 {}`) branches of the ICU message `system.confirm.what` (`system.en.properties:68`, rendered by `SystemView.java:653`) are asserted by no test (`landings/PLAN-171.md` L3-1). A fixed-case test restores them: one case with every count 1 and one with a zero count, in `en` and `de`, expected text literal (it is the contract); the drawn test stays as it is. PLAN-171 also replaced `OnboardCommandTest.ConfigAspect.reportsARefusedStep` (every request answered 409, so the initial state read was refused) by a test that answers the state read and refuses only the step POST (`ConfigCommandTest.java:629-641`): a test is added for `config <aspect>` when the initial state read itself is refused (L3-2).
   - Persona: plan-marshall:persona-module-tester
   - Skills: pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
   - Done when:
     - The fixed-case test is run once with the `one` branch of `system.confirm.what` changed to `other` in the pack and fails; the failing run is recorded (evidence order: `mutation-after`).
     - The refused-state-read test fails when the verb prints nothing for a refused state read, run once with that behaviour mutated (evidence order: `mutation-after`).
     - `./mvnw -pl pm-operator verify` passes and the number of tests is higher than before by the added tests (evidence order: `none`).
5. **The documentation corrections of PLAN-167's landing: three false statements in three repositories (the operator's decision of 2026-11-20: folded in here; done first)**
   - What: `landings/PLAN-167.md` L3-1 to L3-3, each re-read at `origin/main` on 2026-11-20 and found still in place. (a) **This repository:** `doc/Traceability.adoc` lines 140, 208, 271 and 440 name code that does not exist, against the document's own rule "a line is never invented": PM-WF-19 `pm-operator` (`decision-log export`) and PM-SKILL-4 `pm-operator` (the harness bundle installer and its shim skill) become "none in this repository"; PM-CRED-2 loses `and credentials migrate`; PM-TECH-4 loses `and Seatbelt` (Landlock stays: `Confinement.java` has it; the code has no `credentials migrate`, no decision-log verb, no `shim`, no Seatbelt). (b) **`../pm-mcp-core`:** `doc/specification/harness-profiles.adoc:13` links `../../../plan-marshall-documentation/doc/specification/harness-integration.adoc`, the family index that `plan-marshall-documentation` #192 deleted; it is retargeted to `https://github.com/plan-marshall/pm-mcp-clients/blob/main/doc/specification/harness-integration.adoc` (anchors kept), and the same file is searched for other links to deleted parts. (c) **`../plan-marshall-documentation`:** `doc/use-cases/01-onboarding.adoc:298` links `../specification/harness-integration/02-bundle-manifest.adoc`, which is trimmed to the `#bundle-content` block; the link is retargeted to the clients URL of the moved part. Link lines and the four parentheticals only; no other line changes.
   - Persona: plan-marshall:persona-documenter
   - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation
   - Done when:
     - `doc/Traceability.adoc` names no code that a search of `pm-operator` and `pm-exec` main does not find: the lane re-runs the four searches (`decision-log`, `credentials migrate`, `shim`, `Seatbelt`) and records the result (evidence order: `none`).
     - A search of `../pm-mcp-core` for links to the deleted `harness-integration.adoc` finds none, and every retargeted URL resolves against `main` of this repository (evidence order: `none`).
     - `git diff` of each of the three pull requests shows only the lines named above, file by file (evidence order: `none`); the documentation repository's `trace.py` reports `BROKEN LINKS 0` on the merged head (evidence order: `none`).

## Standing Constraints

- ZERO Sonar findings (the operator, 2026-11-05): the pull request merges with 0 open new-code issues (SonarCloud project `plan-marshall_pm-mcp-clients`), read at the pull request head after the push (`unknown` for a failed read, never `0`); every open issue is fixed in a commit of THIS pull request, and a dismissal is made only on the operator's selection. State `sonar_open_new_issues=N` in the landing message. This is a merge gate, not a deliverable's Done-when.
- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-operator verify -Pintegration-tests`, `./mvnw -pl pm-api verify`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- CUI standards (`CLAUDE.md`, `PLAN-77`): a test that takes input data draws it from `Generators` with `@EnableGeneratorController` (the expected value of a contract stays literal); a log call at INFO or above goes through a `LogRecord` of the module's own `LogMessages` class (one per module that logs, never one for the repository) with a `LogAsserts` test; `@NullMarked` applies in a module that declares JSpecify (`pm-operator`, `pm-mock-runtime`); until `PLAN-77` has landed the test-scope CUI dependencies are missing in `pm-api`, `pm-exec`, `pm-relay` and `pm-mock-runtime`, and a test there follows plain JUnit and reports the departure, never adds the dependency itself.
- Java 25, Lombok, records; no generic `Exception`/`RuntimeException`; JUnit 5 only, no Mockito (CLAUDE.md).
- Every new operator text goes through `MessageCatalogue` into `operator.en.properties` and `operator.de.properties`.
- Terminal check of the rendered output through the screen capture harness of `cli-pty-dialogue-harness` (`ScreenCaptureIT`, command line in `doc/design/terminal-client/27-tui-testing.adoc`): write this plan's captures to `pm-operator/src/test/resources/screen-captures/{plan}.txt`, run the set BEFORE the change and copy `pm-operator/target/screens/{plan}/` to `.plan/temp/terminal/{plan}/before/`, then run it AFTER and copy to `.../after/`. Capture at 120 × 30 and 80 × 24 in `en_US.UTF-8` and `de_DE.UTF-8` unless the target names other sizes or environments. Read every `.txt` and `.attrs.txt` with the file tools and compare them with the deliverable; the screen text shows no raw id, link name, state id, `-` placeholder, ISO time or HTTP status, and no text of a previous level. Never run `pm-operator` in the agent's own shell: the operator gate refuses a coding agent by design (`PM-SEC-3`) and is never worked around; only the harness, which clears the environment like every PTY test, runs it. If the harness is not available, the terminal Done-when clauses are NOT met — a blocker, never skipped.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/cli/ux/plan.adoc#CLI-UX-S-05`, `doc/claude_review/cli/ux/proposals.adoc#CLI-UX-P-04` — read at `doc/claude_review/cli/ux/plan.adoc`
  - verdict: corroborated | checked_at: 71f84bb | by: ui-improvements/cleanup | rescoped: n/a | evidence: cli/ux/plan.adoc:47-48 CLI-UX-S-05; cli/ux/proposals.adoc:81 CLI-UX-P-04 and :115 CLI-UX-P-06 present
- Re-grounded note at HEAD 5348e89e (2026-10-30): `cli-gate-scope` (PLAN-14, shipped, PR #277) added a minimal
  `--print` to `plan` on the operator's decision (its terminal check needed `plan fix-relay --print`) and documented
  it in `01-commands`, `16-plan-view` and the `pm-operator` README. Treat `plan <id> --print` as present; the other
  view verbs still lack it. PLAN-14 also changed `PmConsole`, `PlanCommand`, `EpicCommand` and the operator catalogue
  pair that this spec declares: re-read them at HEAD before the first deliverable.
  - verdict: contradicted | checked_at: 71f84bb | by: ui-improvements/cleanup | rescoped: yes | evidence: plan --print exists PlanCommand:119 and is documented 01-commands.adoc:118 and 16-plan-view.adoc:204 but pm-operator/README.md never mentions --print; find also has it FindCommand:120
- Re-grounded note at HEAD 94bbefe1 (cleanup 2026-11-02): HelpCommand/HelpCommandTest still absent (created by this plan, correct). --print exists only on PlanCommand; PlanCommand, EpicCommand, SystemCommand, PmConsole, PmOperator changed again via PLAN-26 (SystemView model in operator/system/) and PLAN-91 (interrupt exit 130), so re-read before D1.
  - verdict: contradicted | checked_at: 71f84bb | by: ui-improvements/cleanup | rescoped: no | evidence: HelpCommand and HelpCommandTest are absent as stated; but --print is not only on PlanCommand: FindCommand.java:120 has it; console help is now printHelp PmConsole:620-628
- HOLD (design audit 2026-11-02): the help 'lists every verb' would fix the extra top-level verbs (start, onboard, enrol, unenrol) into tests and docs that decision D26 folds in or removes. Hold until the D26 clients package, or rescope to the design command tree (terminal-client/01-commands).
  - verdict: contradicted | checked_at: 71f84bb | by: ui-improvements/cleanup | rescoped: yes | evidence: HOLD cleared: PLAN-97 landed (#369 2fc2c214); PmOperator.java:43-66 registers 24 verbs and none is named start onboard enrol or unenrol (EnrolHarnessCommand is enrol-harness)
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.
  - verdict: unverifiable | checked_at: 71f84bb | by: ui-improvements/cleanup | rescoped: n/a | evidence: Process clause; no code fact

- Design audit 002 (2026-11-02, `design-audit-002`, archived): the D26 package now exists as `doc/plans/operator-command-tree.adoc` (merged with PR #310, `28634b2a`; open). The hold stands until PLAN-97 deliverable 3 (the package `operator-command-tree`) ships; PLAN-97 was staged on 2026-11-04 on the operator's word. The command tree dropped the `abort` row, spells `runtime stop|restart [--when-done|--now]` and `runtime stop --withdraw`, and has no `--drain`.
  - verdict: contradicted | checked_at: 71f84bb | by: ui-improvements/cleanup | rescoped: yes | evidence: Command tree still has --drain: 01-commands.adoc:92 :94 :158 (alias of --when-done); RuntimeCommand.java:147 :261 :326; --withdraw :159 exists; abort row dropped

## Expected Surface

- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/PmOperator.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/PmConsole.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/EpicCommand.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/PlanCommand.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/SystemCommand.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ReportsCommand.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/HelpCommand.java` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/HelpCommandTest.java` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `doc/design/terminal-client/01-commands.adoc` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/operator.en.properties` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/operator.de.properties` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/StatusCommand.java` — present at HEAD aa3a9be3
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/PlansCommand.java` — present at HEAD aa3a9be3
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/QuestionsCommand.java` — present at HEAD aa3a9be3
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/DrainCommand.java` — present at HEAD aa3a9be3
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/AcceptEditCommand.java` — present at HEAD aa3a9be3
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ConfigCommand.java` — present at HEAD aa3a9be3
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/WebCommand.java` — present at HEAD aa3a9be3
- OBSERVED: `pm-operator/src/test/resources/screen-captures/PLAN-42.txt` — absent; created by this plan at HEAD aa3a9be3

- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/OperatorVerbs.java` — present at HEAD 2fc2c214 (fold 2026-11-08)
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/PmOperatorTest.java` — present at HEAD 2fc2c214 (fold 2026-11-08)
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ProjectsCommand.java` — present at `cc0d62af` (added by the cleanup of 2026-11-10)
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/NeedsCommand.java` — present at `cc0d62af` (added by the cleanup of 2026-11-10)
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/overview/OverviewFullScreenApp.java` — present at `cc0d62af` (added by the cleanup of 2026-11-10)
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/epicview/EpicViewLevel.java` — present at `cc0d62af` (added by the cleanup of 2026-11-10)
- OBSERVED: `doc/design/terminal-client/04-console.adoc` — present at `cc0d62af` (added by the cleanup of 2026-11-10)
- OBSERVED: `pm-operator/README.md` — present at `cc0d62af` (added by the cleanup of 2026-11-10)
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/RuntimeActDialogueTest.java` — present at `76315c63` (deliverable 4, 2026-11-20)
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ConfigCommandTest.java` — present at `76315c63` (deliverable 4)
- OBSERVED: `doc/Traceability.adoc` — present at `origin/main` 2026-11-20 (deliverable 5)
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/OperatorVerbsTest.java` — present at `cc0d62af` (added by the cleanup of 2026-11-10)
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/PmConsoleTest.java` — present at `cc0d62af` (added by the cleanup of 2026-11-10)
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/EpicCommandTest.java` — present at `cc0d62af` (added by the cleanup of 2026-11-10)
- OBSERVED: `doc/design/terminal-client/19-actions.adoc` — present at `dc811fb3` (added by the cleanup of 2026-11-14)
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/FindCommand.java` — present at `dc811fb3` (added by the cleanup of 2026-11-14)

## Dependencies and Sequencing

- Depends on: `PLAN-30` (`cli-texts-and-errors`), `PLAN-36` (`cli-needs-verb`)
- Overlaps with: `cli-texts-and-errors` and `cli-needs-verb` touch `PmOperator.java`: runs after them
- Adjacent to: `landings/PLAN-171.md` and `landings/PLAN-167.md` (the sources of deliverables 4 and 5, folded in on the operator's word of 2026-11-20; PLAN-168 runs on `PlanCommandTest.java` and `ConfigCommandTest.java`: rebase over it)

## Re-grounding 2026-11-07 (cleanup)

Checked at `aa3a9be3`. The HOLD's condition (PLAN-97 deliverable 3, the command tree) is met: 22 verbs, no `start`/`onboard`. Design conflict to resolve at outline: `01-commands.adoc:142` says `help` exists only inside the console and is not a shell verb and that `epic <id>` prints, while the deliverables ask for `help` in the shell and the console and `epic <id>` opening full screen; line 122 lists `help [<verb>]`. `HelpCommand` is absent, `--print` exists only on `PlanCommand` (:118). PLAN-30 and PLAN-36 (dependencies) are still open.

## Fold 2026-11-08 (PLAN-118 landing)

Source: `landings/PLAN-118.md` (layer 2), read at `2fc2c214`. PLAN-118 derived the completer and `help` from the registered subcommands (`OperatorVerbs`) and compares the design's verb set with the registry (`PmOperatorTest#registersTheDesignsVerbs`). Two console-only sets remain and disagree: `PmOperatorTest.java:268` `{help, exit}` and `OperatorVerbs.java:32` `{exit, quit, help, tui}`; `quit` and `tui` are not named in `01-commands.adoc`. Since this plan rewrites `help` and the verb listing: keep ONE console-only set (in `OperatorVerbs`, read by the test), and name every console-only word in the design. Expected Surface extended in the same act (`OperatorVerbs.java`, `PmOperatorTest.java`).

## Re-grounding 2026-11-10 (cleanup, at `cc0d62af`)

This section supersedes the spec text and earlier notes where they differ.

- The HOLD (claim 3) is cleared: PLAN-97 (#358) landed; 23 verbs registered, none of start/onboard/enrol/unenrol.
- Claim 1 narrower: `--print` exists only on `PlanCommand:118`; `pm-operator/README.md` does not document it.
- Claim 5 corrected: `--drain` still exists as the alias of `--when-done` (`RuntimeCommand.java:147,261,326`,
  `01-commands.adoc`).
- **OPEN DESIGN CONFLICT (operator):** `01-commands.adoc:149` says `help` is console-only and `epic <id>` PRINTS the epic;
  D1/D2 say the opposite (full screen by default, `help` in the shell). The AGREED model for projects/system is "full
  screen in the console" (`01-commands.adoc:38,91`). Decide before emit, or D3 states the new rule and the docs change.
- D1 needs an epic factory in `InitialSelection` (only plan, explorerRoot, system exist); "view verbs" must be listed
  (plans, plan, projects, system, status, epic, reports, needs, questions, find?). `--help` exists only on the root
  (`mixinStandardHelpOptions`, `PmOperator.java:40`); the console's `help <verb>` is a stub. The designer line is the
  existing key `operator.help.designer`. The console help body (`operator.console.help.body`) lists neither `needs` nor
  `!`; `?` at the prompt is not handled. Console-only words: `help, exit, quit, tui` (`OperatorVerbs.java:32`) plus `!`.
- Sequencing: after PLAN-30 (declared) and PLAN-84 (running: `PmOperator`, `PmConsole`, `01-commands.adoc`; the mode
  table must include `find`).
- Disposition: AMEND after the operator's design decision.

## Re-Grounding 2026-11-14 (cleanup)

Verdicts persisted per claim at `dc811fb3` (`corpus set-verdict`, by `ui-improvements/cleanup`). Read before the outline:

- Claim 1 contradicted: plan <id> --print exists (PlanCommand:118) and is documented in 01-commands:118 and 16-plan-view:204, but pm-operator/README.md does not mention --print; find also has it (FindCommand:120)
- Claim 3 contradicted: HOLD cleared: PLAN-97 #358 (d8efe35f) landed; PmOperator registers 24 verbs (incl. find), none named start/onboard/enrol/unenrol top-level
- Claim 5 contradicted: command tree still lists --drain as alias of --when-done: RuntimeCommand.java:147,261,326 and 01-commands.adoc:92,94,158; --withdraw exists :159; abort dropped
- Already done: `plan --print` (#277) and `find --print` (#397); the other view verbs still lack it. The HOLD on PLAN-97 is cleared (#358). `--drain` is still an alias of `--when-done` (`RuntimeCommand.java:147,261,326`). The verify-first clause names a HYPOTHESIS that does not exist.

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-42-cli-verb-modes-and-help.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.

**Amendment 2026-11-20 (deliverable 5 only):** this plan also edits, on their own branches and in their own pull requests, `../pm-mcp-core` (`doc/specification/harness-profiles.adoc`, one link line) and `../plan-marshall-documentation` (`doc/use-cases/01-onboarding.adoc`, one link line), each under that repository's own gates; it edits nothing else there. Where a sibling repository has no plan-marshall CI provider configured, the lane asks the operator how to open and merge the pull request, as PLAN-167 did (its option "use `gh` directly there" is the usual answer).

## PLAN-127 Landing Residue (Amendment 2026-11-13)

- `epic next` now has `--raise-scope`, `--no-raise` and `--no-input` (`EpicCommand.java:466-485`), `ActionSupport.FlagPair.RAISE` and
  `ActionSupport.contradicts`. **New item:** `terminal-client/01-commands.adoc:65` lists only `[--raise-scope]` and `19-actions.adoc:31`
  lacks `--no-input`: bring both documents to the three flags (verb modes are this plan's). Re-read `EpicCommand` at HEAD.
