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

0. **The residue of PLAN-163: the one-shot list prints a path `--file` accepts, the key-driven file open checks its capture, and the changes text and design agree with the code (the operator's D0: done first)**
   - What: `landings/PLAN-163.md`, re-read at `fcf87728` on 2026-11-23. (a) `plan <id> --changes` prints `ChangesLevel.abbreviatedPath` (`ChangesLevel.java:370-377`) through `PlanCommand.printChanges`/`printRows` (`PlanCommand.java:326-343,391-396`), while `--file` needs the exact path (`:362-368`): the one-shot prints each file's full, sanitised path (the full-screen row keeps its abbreviation), and `plan <id> --changes --file <printed path>` returns the hunks for every printed row. (b) `OverviewFullScreenApp.changesOpenSelected` (`:2817-2847`) reads the file outside the capture the key was pressed on and `showFile` accepts any path in the current list; `readOpenFile` (`:1896-1913`, guard `:1906`) checks `changes.answer() == fresh`. The key-driven open takes the answer it read against and applies `showFile` only if the level still holds it. (c) The one-shot's piped hunks lose tabs (`TuiLayout.sanitizeCell:136-138`) and carry a two-space indent and a U+2212 prefix (`PlanCommand.java:393`, `ChangesLevel.java:408,423-429`): the one-shot prints each hunk line with its original text (control sequences stripped, tabs kept) and one marker character, so the output feeds `patch`; where the design cannot allow an ASCII `-`, `16-plan-view.adoc:114` drops "so that the operator can pipe them" instead. (d) A captured list with zero files prints the header only (`ChangesLevel.java:126-134`): it prints the catalogue text `plan.changes.none` (new key, both packs, a row in the table of `doc/design/shared/plan-view.adoc` near `:266`). (e) `PlanCommand.java:341-342` exits 0 for `unavailable`, and `--file ""` prints the whole list: `unavailable` refuses on stderr with exit 1, a blank `--file` is refused as `--plan ""` is (`ConfigCommandTest.configSetRefusesABlankPlan`), `not_captured` stays exit 0 (a state, not a refusal; the operator may say otherwise), and `--changes` combined with `--activity`, `--tokens`, `--components` or `--watch` is refused on stderr, not silently won. (f) `16-plan-view.adoc:114` (the colour sentence is not implemented: no per-cell style channel) and `:165` (the Live paragraph says nothing of the changes level's live read) are corrected to what the code does. Carried, not folded (off this plan's surface): cut lines in full-screen rows (`TuiLayout.rowLines:374-`), positional line ids (`ChangesLevel.java:407`), `ScenarioLoader` importing `MockRuntimeEngine` (`ScenarioLoader.java:69,279`, `pm-mock-runtime`). Fixed since the landing and dropped: the English literal `plan.changes.file_not_found` (catalogued at `PlanCommand.java:385`, `OverviewFullScreenApp.java:2831`) and the `01-commands.adoc:117-122` synopsis.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:javadoc, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-documents:ref-asciidoc
   - Done when:
     - A test prints `plan x --changes` for a captured list with a path of four or more segments, takes the printed path and runs `--file` on it: it gets the hunks; it fails against the abbreviated print (evidence order: `test-first`).
     - A test captures between the file read and `showFile` and shows the old hunks are not applied; it fails when the identity check is removed (evidence order: `mutation-after`).
     - Tests pin the hunk text (tab and prefix), the zero-file text, and the exit codes of `unavailable`, a blank `--file` and the flag combinations, each of `--activity`, `--tokens`, `--components` and `--watch` with `--changes` (Checkpoint 1 amendment: the first run pinned two of the four); the empty-list text exists in en and de (evidence order: `test-first`).
     - Clause (f), restated here because a reader cut the long What line above at 2000 characters and the first run reported it met without doing it (Checkpoint 1 amendment): `doc/design/terminal-client/16-plan-view.adoc:114` no longer says removed and added lines "take the terminal's colours" nor that they keep their `−`; it says what the code does (the full-screen rows carry no per-cell style, the one-shot prints an ASCII `-`); `doc/design/shared/plan-view.adoc:187` says the same; `:165` says the changes level reads live; `doc/design/shared/plan-view.adoc:190` says the one-shot exits 1 on stderr for `unavailable`; the sentence "so that the output feeds `patch`" says what the output is (breadcrumb and file row first, no `---`/`+++` header) (evidence order: `none`, a search of the four files for the old wording returns nothing).
     - A test drives the key-driven open through the key path of `OverviewFullScreenApp` (not `level.showFile` alone), captures between the file read and the apply, and shows the old hunks are not applied; it fails when `answerReadAgainst` (`OverviewFullScreenApp.java:2826,2843`) is replaced by `changes.answer()` at apply time, run once with that mutation and the failing run recorded; the read of `answer` at `:2826` is safe against the thread that replaces it (the field is non-volatile, `ChangesLevel.java:73`) (evidence order: `mutation-after`).
     - `16-plan-view.adoc:114,165` and `01-commands.adoc:118-122` pass `pm-documents:ref-asciidoc` `validate` and `verify-links` (evidence order: `none`).
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
5. **The documentation correction of PLAN-167's landing: four false Traceability entries in this repository (the operator's decision of 2026-11-20: folded in here; done first; parts (b) and (c) were closed by the third corpus review, see the note)**
   - Re-read at `origin/main` `f707b231` on 2026-11-21 (the third corpus review, #522 here, #536 in `pm-mcp-core`, #198 in `plan-marshall-documentation`, merged 2026-11-20 outside the lanes): **(a) is still in place, at new lines 122, 178, 245 and 384** (was 140, 208, 271, 440); **(b) is closed** (`harness-profiles.adoc:13` now links the clients URL of `harness-integration.adoc`); **(c) is closed** (`01-onboarding.adoc` links the clients URL of `02-bundle-manifest.adoc`; the `#bundle-content` link left is valid). The lane does only (a) and re-checks (b) and (c) by one search each; the original text below is kept for the wording of (a).
   - What: `landings/PLAN-167.md` L3-1 to L3-3, each re-read at `origin/main` on 2026-11-20 and found still in place. (a) **This repository:** `doc/Traceability.adoc` lines 140, 208, 271 and 440 (now 122, 178, 245 and 384, see above) name code that does not exist, against the document's own rule "a line is never invented": PM-WF-19 `pm-operator` (`decision-log export`) and PM-SKILL-4 `pm-operator` (the harness bundle installer and its shim skill) become "none in this repository"; PM-CRED-2 loses `and credentials migrate`; PM-TECH-4 loses `and Seatbelt` (Landlock stays: `Confinement.java` has it; the code has no `credentials migrate`, no decision-log verb, no `shim`, no Seatbelt). (b) **`../pm-mcp-core`:** `doc/specification/harness-profiles.adoc:13` links `../../../plan-marshall-documentation/doc/specification/harness-integration.adoc`, the family index that `plan-marshall-documentation` #192 deleted; it is retargeted to `https://github.com/plan-marshall/pm-mcp-clients/blob/main/doc/specification/harness-integration.adoc` (anchors kept), and the same file is searched for other links to deleted parts. (c) **`../plan-marshall-documentation`:** `doc/use-cases/01-onboarding.adoc:298` links `../specification/harness-integration/02-bundle-manifest.adoc`, which is trimmed to the `#bundle-content` block; the link is retargeted to the clients URL of the moved part. Link lines and the four parentheticals only; no other line changes.
   - Persona: plan-marshall:persona-documenter
   - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation
   - Done when:
     - `doc/Traceability.adoc` names no code that a search of `pm-operator` and `pm-exec` main does not find: the lane re-runs the four searches (`decision-log`, `credentials migrate`, `shim`, `Seatbelt`) and records the result (evidence order: `none`).
     - A search of `../pm-mcp-core` for links to the deleted `harness-integration.adoc` finds none, and every retargeted URL resolves against `main` of this repository (evidence order: `none`); no edit is expected there (closed by #536).
     - `git diff` of the pull request shows only the four lines named above (evidence order: `none`); ~~the documentation repository's `trace.py` reports `BROKEN LINKS 0` on its main head~~ (Checkpoint 1 amendment: dropped from the lane, no sanctioned path of the lane runs a sibling repository's script; the landing says that `trace.py` was not run and the operator runs it at the documentation repository's own gate) (evidence order: `none`).

6. **The minor residue of PLAN-174 on the files this plan edits (the operator's D0: done first, after deliverable 0)**
   - What: `landings/PLAN-174.md`, re-read at `fcf87728` on 2026-11-23. (a) L1-4: `config set` prints a conflict on stdout in every case (`ConfigCommand.java:739-749`) and `accept-edit` prints its decline on stdout (`AcceptEditCommand.java:329-332`), against `03-dialogues.adoc` "every refusal ... stderr" and against `plan settings` (`PlanCommand.java:1655-1666`: the `--no-input` stop on stdout, an overtaken change on stderr): `config set` and `unset` follow the `plan settings` rule, the `accept-edit` decline text goes to stderr; `PlanCommand.java:1655-1666` is NOT edited (the `plan settings` refusal belongs to the separate plan the operator decided on 2026-11-23). (b) L2-1 and L2-5: the `plan <id>` tree prints English in German (`kindPrefix` `PlanCommand.java:584,587`, "run N" `:609`, "tasks: %d of %d done" `:617`, "waits: " `:620`, "off-track: " `:650`, "waits slot" `:672`; the check `title.startsWith("phase ")` at `:626` depends on the English prefix and moves with it): every text goes through `MessageCatalogue` in both packs, and German tests pin the tree, `operator.plan.running_title`, `operator.epic.running_title` and `operator.plan.tree.pull_request`. (c) L3-2 and L3-3: when every id of `reports read` is refused it prints the success line "0 reports marked read." (`ReportsCommand.java:290`): that line is printed only when at least one id was marked; a 404 is worded by its body as `PlanCommand.refuseRead` does (`:173`), not always as "not found" (`ReportsCommand.java:284,337-338`). (d) L2-4: the nine no-argument success rows of `ExitCodesTest` (status, web, drain, credentials, project, client, recipe, lesson, roles) name an argument that does more than print the parent help. Off this plan's surface and not folded: `ConfigCleanupCommand:436-438`, `EnrolHarnessCommand:122`, `RolesCommand:359`, `DecomposeCommand:274-279` and its misnamed `out` parameter, `CleanupCommand:1229`, `StartApi` (server text on a German screen), the `AnswerCommand` and `ProjectCommand` 404 wording, `ScreenCaptureIT` comparing no capture.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-dev-java-cui:cui-logging
   - Done when:
     - Tests with generated inputs pin the stream of each conflict and decline line and the German tree and list texts; each fails on the English literal (evidence order: `test-first`; Checkpoint 1 amendment: where the fix is already committed, `mutation-after` is accepted: each test is run once with its stream line moved back to stdout or its text back to the English literal, and the failing run is recorded; `listTitlesAreGerman` pins a text that differs between the packs, `operator.epic.running_title` is `Epics:` in both and pins nothing).
     - The `reports read` test with every id refused expects no success line and exit 1; it fails when the line is printed again (evidence order: `mutation-after`).
     - The `ExitCodesTest` success rows (Checkpoint 1 amendment, replaces "fail when the verb is run with no arguments"): every row that can name an argument does, and the row fails when that argument is dropped (`status --json`, `web devices`, `project favourite <id>`, `client status`, `lesson check <id> --project <id>` are done; `web devices` still passes with `devices` dropped because `web`'s no-argument run is its own default action: the row is exempt and the guard asserts that default output instead); `drain` (`ExitCodesTest.java:255-258`) is added to the rows; a row whose only invocation needs an installation layout or an answer shape the shared setup does not serve (`credentials`, `recipe`, `roles`) is listed in the test with its reason, and the "no parent help" guard (`ExitCodesTest.java:666-670`) also runs for the rows without arguments (evidence order: `mutation-after`).

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
  - verdict: corroborated | checked_at: fcf877285a08c0d442fc3f07bfcb5f5515088ea9 | by: ui-improvements/analyze | rescoped: n/a | evidence: plan.adoc:47-48 CLI-UX-S-05; proposals.adoc:81 (P-04), :115 (P-06) (origin/main fcf87728)
- Re-grounded note at HEAD 5348e89e (2026-10-30): `cli-gate-scope` (PLAN-14, shipped, PR #277) added a minimal
  `--print` to `plan` on the operator's decision (its terminal check needed `plan fix-relay --print`) and documented
  it in `01-commands`, `16-plan-view` and the `pm-operator` README. Treat `plan <id> --print` as present; the other
  view verbs still lack it. PLAN-14 also changed `PmConsole`, `PlanCommand`, `EpicCommand` and the operator catalogue
  pair that this spec declares: re-read them at HEAD before the first deliverable.
  - verdict: contradicted | checked_at: fcf877285a08c0d442fc3f07bfcb5f5515088ea9 | by: ui-improvements/analyze | rescoped: yes | evidence: --print only on PlanCommand.java:153 and FindCommand.java:120; not on EpicCommand, StatusCommand, SystemCommand, ReportsCommand, NeedsCommand, QuestionsCommand, ProjectsCommand (--list :57); pm-operator/README.md has no --print (origin/main fcf87728)
- Re-grounded note at HEAD 94bbefe1 (cleanup 2026-11-02): HelpCommand/HelpCommandTest still absent (created by this plan, correct). --print exists only on PlanCommand; PlanCommand, EpicCommand, SystemCommand, PmConsole, PmOperator changed again via PLAN-26 (SystemView model in operator/system/) and PLAN-91 (interrupt exit 130), so re-read before D1.
  - verdict: contradicted | checked_at: fcf877285a08c0d442fc3f07bfcb5f5515088ea9 | by: ui-improvements/analyze | rescoped: yes | evidence: HelpCommand and HelpCommandTest absent; --print also on FindCommand:120; console help PmConsole.printHelp :638-646, help <verb> prints the stub operator.console.help.for, pinned by PmConsoleTest.java:154-186 (origin/main fcf87728)
- HOLD (design audit 2026-11-02): the help 'lists every verb' would fix the extra top-level verbs (start, onboard, enrol, unenrol) into tests and docs that decision D26 folds in or removes. Hold until the D26 clients package, or rescope to the design command tree (terminal-client/01-commands).
  - verdict: contradicted | checked_at: fcf877285a08c0d442fc3f07bfcb5f5515088ea9 | by: ui-improvements/analyze | rescoped: yes | evidence: HOLD premise gone: PmOperator.java:45-70 registers 24 verbs, none of start/onboard/enrol/unenrol (origin/main fcf87728)
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.
  - verdict: unverifiable | checked_at: fcf877285a08c0d442fc3f07bfcb5f5515088ea9 | by: ui-improvements/analyze | rescoped: n/a | evidence: procedural verify-first clause; it names a HYPOTHESIS that does not exist in the spec (origin/main fcf87728)

- Design audit 002 (2026-11-02, `design-audit-002`, archived): the D26 package now exists as `doc/plans/operator-command-tree.adoc` (merged with PR #310, `28634b2a`; open). The hold stands until PLAN-97 deliverable 3 (the package `operator-command-tree`) ships; PLAN-97 was staged on 2026-11-04 on the operator's word. The command tree dropped the `abort` row, spells `runtime stop|restart [--when-done|--now]` and `runtime stop --withdraw`, and has no `--drain`.
  - verdict: contradicted | checked_at: fcf877285a08c0d442fc3f07bfcb5f5515088ea9 | by: ui-improvements/analyze | rescoped: yes | evidence: --drain still exists at RuntimeCommand.java:151,264,329 (alias of --when-done :154,267,332); 01-commands.adoc:92,94,160 list --drain; doc/plans/operator-command-tree.adoc absent; AbortCommandTest exists (origin/main fcf87728)

- OBSERVED: `plan <id> --changes` prints the abbreviated path while `--file` needs the exact one — read at `pm-operator/src/main/java/de/planmarshall/operator/planview/ChangesLevel.java:370-377` and `pm-operator/src/main/java/de/planmarshall/operator/PlanCommand.java:339-343,362-368`
  - verdict: corroborated | checked_at: fcf877285a08c0d442fc3f07bfcb5f5515088ea9 | by: ui-improvements/analyze | rescoped: n/a | evidence: ChangesLevel.java:370-377 abbreviatedPath used by pathCell :337-349; PlanCommand.java:339-343 prints level.rows via printRows :391-396; --file needs the exact path :362-368 (origin/main fcf87728)
- OBSERVED: the key-driven file open applies `showFile` with no capture-identity check while `readOpenFile` guards it — read at `pm-operator/src/main/java/de/planmarshall/operator/overview/OverviewFullScreenApp.java:2817-2847,1896-1913`
  - verdict: corroborated | checked_at: fcf877285a08c0d442fc3f07bfcb5f5515088ea9 | by: ui-improvements/analyze | rescoped: n/a | evidence: OverviewFullScreenApp.java:2817-2847 changesOpenSelected, showFile at :2840 inside synchronized with no answer-identity check; readOpenFile :1896-1913 guards at :1906 (origin/main fcf87728)
- OBSERVED: `reports read` prints "0 reports marked read." when every id is refused, and `config set` / `accept-edit` print conflict and decline lines on stdout — read at `pm-operator/src/main/java/de/planmarshall/operator/ReportsCommand.java:267-293`, `ConfigCommand.java:739-749` and `AcceptEditCommand.java:329-332`
  - verdict: corroborated | checked_at: fcf877285a08c0d442fc3f07bfcb5f5515088ea9 | by: ui-improvements/analyze | rescoped: n/a | evidence: ReportsCommand.java:290 prints the success line when all ids are refused; ConfigCommand.java:739-749 and AcceptEditCommand.java:329-332 print to stdout (origin/main fcf87728)
- OBSERVED: the `plan <id>` tree prints English literals in German (`kindPrefix`, "tasks: %d of %d done", "waits: ", "off-track: ") — read at `pm-operator/src/main/java/de/planmarshall/operator/PlanCommand.java:584,587,617,620,650,672`
  - verdict: corroborated | checked_at: fcf877285a08c0d442fc3f07bfcb5f5515088ea9 | by: ui-improvements/analyze | rescoped: n/a | evidence: PlanCommand.java:584,587 kindPrefix 'phase '/'sub-workflow ', :617 'tasks: %d of %d done', :620 'waits: ', :650 'off-track: ', :672 'waits slot' (origin/main fcf87728)

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
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/planview/ChangesLevel.java` — present at HEAD fcf87728 (re-grounding 2026-11-23)
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/planview/ChangesLevelTest.java` — present at HEAD fcf87728 (re-grounding 2026-11-23)
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/overview/OverviewChangesKeysTest.java` — present at HEAD fcf87728 (re-grounding 2026-11-23)
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ExitCodesTest.java` — present at HEAD fcf87728 (re-grounding 2026-11-23)
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/PicocliLanguageTest.java` — present at HEAD fcf87728 (re-grounding 2026-11-23)
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/PlanCommandTest.java` — present at HEAD fcf87728 (re-grounding 2026-11-23)
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ReportsCommandTest.java` — present at HEAD fcf87728 (re-grounding 2026-11-23)
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/FindCommandTest.java` — present at HEAD fcf87728 (re-grounding 2026-11-23)
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/StatusCommandTest.java` — present at HEAD fcf87728 (re-grounding 2026-11-23)
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/NeedsCommandTest.java` — present at HEAD fcf87728 (re-grounding 2026-11-23)
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/QuestionsCommandTest.java` — present at HEAD fcf87728 (re-grounding 2026-11-23)
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/SystemCommandTest.java` — present at HEAD fcf87728 (re-grounding 2026-11-23)
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/EpicViewFullScreenTest.java` — present at HEAD fcf87728 (re-grounding 2026-11-23)
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/OverviewFullScreenTest.java` — present at HEAD fcf87728 (re-grounding 2026-11-23)
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/LocalRefusalStreamTest.java` — present at HEAD fcf87728 (re-grounding 2026-11-23)
- OBSERVED: `doc/design/terminal-client/16-plan-view.adoc` — present at HEAD fcf87728 (re-grounding 2026-11-23)
- OBSERVED: `doc/design/shared/plan-view.adoc` — present at HEAD fcf87728 (re-grounding 2026-11-23)
- OBSERVED: `doc/design/terminal-client/15-overview.adoc` — present at HEAD fcf87728 (re-grounding 2026-11-23)
- OBSERVED: `doc/design/terminal-client/17-projects.adoc` — present at HEAD fcf87728 (re-grounding 2026-11-23)
- OBSERVED: `doc/design/terminal-client/24-reports.adoc` — present at HEAD fcf87728 (re-grounding 2026-11-23)
- OBSERVED: `doc/design/terminal-client/25-system.adoc` — present at HEAD fcf87728 (re-grounding 2026-11-23)

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

## Re-Grounding 2026-11-22 (cleanup, at `7b60cec0`)

Verdicts persisted per claim by `ui-improvements/cleanup` (`corpus set-verdict`, origin/main `7b60cec0`; sibling repos read at their local refs only). Contradicted claims are stamped `rescoped: yes` with the corrections below; read before the outline:

- Claims 1, 2, 3 and 5 contradicted (`rescoped: yes`, scope unchanged). `plan --print` exists (`PlanCommand:148`) and so does `find --print` (`FindCommand:120`), but no other view verb has it and `pm-operator/README.md` names none; there is no `HelpCommand`/`HelpCommandTest` (console help is `PmConsole.printHelp :637`); the HOLD premise is gone (`PmOperator.java:45-70` registers 24 verbs, none of start, onboard, enrol, unenrol; PLAN-97 shipped); `--drain` still exists (`RuntimeCommand:151,266,331`) as an alias of `--when-done`, the `abort` row was dropped and `doc/plans/operator-command-tree.adoc` is gone.
- D1, D2, D3 and D4 are NOT done (no `--print` on epic/status/system/reports/needs/questions/projects, `--help` only on the root, `EpicCommand.java:95-127` has no full-screen path). D5(b) and (c) are closed at the sibling `origin/main` refs (read locally, re-check on a fetch); D5(a) is still open at `Traceability.adoc:122,178,245,384`.
- Still open decisions: `01-commands.adoc:159` says `help` is console-only and `epic <id>` prints; D1/D2 require the opposite.
- PLAN-174 edits `ConfigCommandTest`, `PlanCommand`, `EpicCommand`, `PmOperatorTest`, `operator.*.properties`: emit after it lands. **Landed 2026-11-23 (#533, `fcf87728`); re-grounded below.**

## Re-Grounding 2026-11-23 (analyze, at `fcf87728`)

PLAN-174 landed (#533, `fcf87728`; `PmOperatorTest` and `EpicCommandTest` were NOT changed by it, 16 of this plan's declared paths were). Verdicts re-persisted per claim by `ui-improvements/analyze` (`corpus set-verdict`). Read before the outline:

- The premises of deliverables 1 to 5 hold at `fcf87728`. Claims 1, 2 and 5 are still contradicted as before (`rescoped: yes`), with moved lines: `--print` is on `PlanCommand.java:153` and `FindCommand.java:120` only, not on `EpicCommand`, `StatusCommand`, `SystemCommand`, `ReportsCommand`, `NeedsCommand`, `QuestionsCommand` or `ProjectsCommand` (`ProjectsCommand` has `--list`, `:57`); `HelpCommand` and `HelpCommandTest` are absent; console help is `PmConsole.printHelp` (`:638-646`) and `help <verb>` still prints the stub `operator.console.help.for` (pinned by `PmConsoleTest.java:154-186`); `--drain` stands at `RuntimeCommand.java:151,264,329`. `01-commands.adoc:159` still says `help` is console-only and `epic <id>` prints; `:139` lists `help [<verb>]` under "console only".
- Anchors moved by #533: `PlanCommand`: `call` `:180-256`, `runFullScreen` `:268-312`; `EpicCommand`: `call` `:90-143`, the id branch `:94-126` (its 404 now `PlanCommand.refuseRead(...)`, `:121-124`); `OverviewFullScreenApp`: `InitialSelection` `:148-190`, `openEpicView` `:778`; `OperatorVerbs.java:33` and `PmOperatorTest.java:272,280` (the console-only sets still disagree); `PmConsole.dispatchCommand` `:418-445` (no `?` branch).
- **Deliverable 2 mechanics (a hint, not a decision):** `PmOperator.applyResourceBundle` (`:239-244`) and the `pm-operator.<verb>.usage.description` keys (`operator.en.properties:827-847`) already localise each verb's help; `mixinStandardHelpOptions` is only on `PmOperator.java:43`, so `--help` on every subcommand is one `CommandSpec` change in the recursion of `applyResourceBundle` or an edit per `@Command` (about 24 classes). A registered `help` subcommand makes `ExitCodesTest.registryCoversEveryRegisteredVerb` (`:637-648`) demand a success, a refusal and a usage row and `PmOperatorTest.registersTheDesignsVerbs` stop subtracting `help`; the `-h`/`--help` assertions at `ExitCodesTest:670-671` and `PicocliLanguageTest` change. The `LiteralTextGuardTest` guard scans every file below `src/main/java/.../operator`: a new `HelpCommand` needs catalogue texts, not literals.
- Deliverable 4: `ConfigCommandTest.java:629-641` is stale, the refused-step test is at `:677-698`; no test refuses the initial `/api/v1/onboarding/state` read of `config <aspect>` (every handler answers 200), `ConfigAspectRun.java:189-190` renders the `OperatorRefusedException` (the mutation target); `RuntimeActDialogueTest.java:115-120` still draws 2..9 and no test names a singular or zero `system.confirm.what` (`system.en.properties:68`, rendered at `SystemView.java:653`). Deliverable 5: `doc/Traceability.adoc` lines 122, 178, 245 and 384 are still false and untouched; (b) and (c) are closed at the sibling `origin/main` refs.
- Folded on 2026-11-23 by the operator's fold rule (2026-11-18: a landing's residue folds as a D0 into the next plan touching its surface): deliverable 0 (PLAN-163's residue, kept only what is still real at `fcf87728`) and deliverable 6 (PLAN-174's minors on this plan's files). PLAN-174's major L3-1 (`plan settings`, 422 `invalid_value`, `PlanCommand.java:1681-1760`) is NOT in this plan: the operator staged a separate plan for it on 2026-11-23 (`cli-plan-settings-refusal-causes`, after this plan; the regions are more than 1200 lines apart, so no textual conflict is expected).
- **Scope-bloat rationale (7 deliverables, proceeding unsplit):** deliverables 4 and 5 were already folded in "done first" by the operator, 0 and 6 are the operator's D0 fold rule, and each deliverable is its own pull request (about 55 to 70 files in all, +4 to 8k lines: no pull request is near the 100-file or 20k-line review limit). Order: 0, 6, 4, 5 first (they edit tests and texts the others rely on), then 1 and 2 (both edit `PmOperator`, `PmConsole`, `PmOperatorTest` and `01-commands.adoc`: one after the other), and 3 (the table) last.

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

## Checkpoint 1 Amendment (2026-11-23, analyze, branch `feature/42-cli-verb-modes-and-help` at `90757a51`; the operator's word: "amend 1 to 4")

The first run was checkpointed by the operator after deliverables 0, 6, 4 and 5 (`landings/PLAN-42.md`). The run resumes with the same lane command, reads this copy, and continues at deliverable 1. State at the checkpoint, so nothing is redone:

- **D4: done** (`37ea9e17`); the only open point is a before/after test count (2884 after, no baseline recorded): the lane records both at the next full verify.
- **D5: done** (`90757a51`) except the `trace.py` Done-when, now dropped (amendment 4).
- **D0 (`7b081721`): (a) to (e) done; (f) NOT done and the capture-identity wiring untested**: both are Done-whens above (amendments 1 and the new bullets).
- **D6 (`19cce18b`): `reports read`, the streams and the German texts done; the test-first evidence and the `ExitCodesTest` rows are re-stated above** (amendments 2 and 3).
- **D1, D2, D3 not started.** D1 and D2 touch lines D0 edited (`01-commands.adoc:118-122`, the `:159` statement that `help` is console-only); `PmOperatorTest` and `ExitCodesTest.registryCoversEveryRegisteredVerb` change when `help` is added. Order stays: D1 and D2 first, D3 last.
- Carried by the same run, minor, no Done-when of their own unless the operator says so: `ConfigCommand --no-input` prints two stdout lines and `work-actions.adoc:187` names one (pin it and say so); no test for the `sub_workflow` kind or `titleNamesItsKind`, whose `startsWith("Phase")` lost its trailing space ("Phasenplan"); `reports open` 404-with-body and `reports read` non-empty `rawCode` untested; the printed path of the one-shot is padded and sanitised (a path with two spaces or control characters does not round-trip into `--file`: say so in the design text or print it unpadded).
- Landing gates for the final pull request, none run so far: the Sonar open-issue count, `-Pintegration-tests`, `-pl pm-api verify`.
