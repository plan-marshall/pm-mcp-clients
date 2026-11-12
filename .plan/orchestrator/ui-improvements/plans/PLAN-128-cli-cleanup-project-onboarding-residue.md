# PLAN-128: Terminal: Cleanup, Project Add and the Step Overview, Made Correct (PLAN-23, PLAN-121, PLAN-119 Residue)

epic: ui-improvements
workstream: WS-04

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Staged on the operator's decision of 2026-11-10
> from `landings/PLAN-23.md`, `landings/PLAN-121.md` and `landings/PLAN-119.md`. This spec is SELF-SUFFICIENT.

## Objective

Three terminal dialogues landed with defects that change what the operator sees or what is sent:

- **Cleanup (PLAN-23, #381).** A decided job is not handled, and the `… N more` line is not printed.
- **`project add` and the release confirmation (PLAN-121, #390).** `project add --no-input --pilot` exits 1 with a raw message. The start
  body is shaped for the mock, not the contract. The release and unenrol confirmation lacks the "What ends / What stays / Afterwards" block.
- **The onboarding step overview (PLAN-119, #377).** With a reduced aspect list, an excluded aspect's state renders "Done".

Make each do what the design says, with tests that can fail, and answer the review items still open on those pull requests.

## Deliverables

1. **The cleanup dialogue (PLAN-23 residue)**
 - What:
   - An already-decided job: `CleanupCommand.java:778` prints "The runtime is stopping; nothing new starts." for every delete refusal.
     Show the decision as `26-cleanup.adoc:174` says; handle a 409 for a decided job.
   - `glyphForKind` (`:985-990`) knows `plan`, `epic` and `project` only; `unenrolled_project`, `read_reports` and `logs` get no glyph.
   - `--exclude` ids are not validated against the job (`:455-459` strips `kind:`; the design `:106` refuses with `not_in_job`, a key that
     exists at `:861,908` for the dialogue); without `--delete`, `--exclude` prints an English refusal (`:383`) and `i` returns 1.
   - A non-interactive delete without `--yes` prints the preview and sends the delete (`:477-481`). Unreachable behind the operator gate
     today; make it refuse (exit 3) so it stays safe if the gate is relaxed.
   - `… N more`: the key `operator.cleanup.more` exists only in the design's text table (`26-cleanup.adoc:317`), not in the catalogue, and nothing prints it.
     Add the key (en, de), print the line, and make items beyond the page addressable by `x` / `o`.
   - The leave-out mark wraps at 80 columns (92 en, 96 de) and titles over 18 characters overflow.
   - Tests: `r`, `i`, `o` through `decide` and the posted job id; `o` on a plan or epic; an uppercase prefix; `deletePreview` beyond a plan item.
     Input from `Generators`.
   - The AGREED `26-cleanup.adoc` was edited by the lane without a recorded approval (the confirmation lost "back" and `?`; 7 new keys miss the
     text table). Restore or approve each edit (OPEN DECISION 7).
   - NOT here (carried elsewhere): the English literals of `CleanupCommand` and the invented figures (PLAN-30 D1); the reminder day in UTC (PLAN-123).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-documents:ref-asciidoc
 - Done when:
   - Tests: a decided job, `--exclude` of an unknown id, a non-interactive delete without `--yes`, and 25 items on a 20-item page each assert the
     text and the posted body; each fails on a revert. Evidence order: `test-first`.
2. **`project add` and the release confirmation (PLAN-121 residue)**
 - What:
   - `ProjectCommand.java:222-234` does not catch `StartPrompt.MissingInputException` (a `RuntimeException`, `StartPrompt.java:254`), so
     `--no-input --pilot` at any choice state after identity ends in `PmOperator`'s raw handler, exit 1. Catch it like `HoldDialogue` does, print the
     catalogue text, exit 3, and close the started entry.
   - The start body is `{path, pilot}` (`:204`). The specification says a project onboarding takes the build environment captured from the
     operator's process and is addressed at `/api/v1/entries/{id}` (`cli-and-security/01-cli-and-installer.adoc:129-131`;
     `runtime-model/01-processes-and-transport.adoc:266`). Read the daemon contract first (`../plan-marshall-documentation`; the real route in
     `plan-marshall-mcp`) and send the contract's body: the build environment, the checkout path resolved absolutely. `resolveSource` (`:266-301`)
     prefers a `project_id` the contract does not return and guesses one from the path basename. Use the entry; remove the guess and the mock's
     `project_id` shortcut (`MockRuntimeEngine`, added by PLAN-121). `pilot` is not a contract field: remove it, or map it to the identity
     answer only.
   - The release and unenrol confirmation (`:484-499`, `:609-620`) is one bare line. Print the "What ends / What stays / Afterwards" block of
     `17-projects.adoc` § Release (the keys exist, `projects.en.properties:43-62`); ask the reason AFTER the block; re-ask on anything but y/n
     ("Type y or n."); `?` explains rather than declines; select the yes key by language prefix (`de_DE`, `de-AT`), not `"de".equals(locale)`
     (`:613`); the default reason "Released by operator" (`:457,490`) comes from the catalogue.
   - `q` or Ctrl+D in `project add` exits 0 (`:225-228`); `01-commands.adoc` says 3 (`ProjectSetupDialogueIT:96-108` pins 0): fix both.
   - The "argument no state asked for" report (`01-commands.adoc:172`) is not implemented: implement it or remove the sentence (OPEN DECISION 8).
   - Dead types: `EnrolRequest`, `EnrolResult` (`pm-api`) have no operator caller; remove with their texts and tests, or keep with a reason.
     `EnrolCommandTest` has no `@EnableGeneratorController` and its `enrolStripsControlSequences` is vacuous (the path is no longer printed).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-documents:ref-asciidoc
 - Done when:
   - Tests: `--no-input --pilot` at a later choice state exits 3 with a catalogue text and closes the entry; the start body carries the build
     environment; a release without `--yes` prints the block and re-asks on a typo; each fails on a revert. Evidence order: `test-first`.
   - `git grep -n "Released by operator" -- pm-operator/src/main` is empty. Evidence order: `none`.
3. **The step overview with a reduced aspect list (PLAN-119 residue)**
 - What:
   - `StateDialogue.machineStepOf` (`StateDialogue.java:665-675`) returns `DONE` for a state of an aspect not in the injected list, so
     an excluded aspect reads "Done". The lane's reply "the runtime never emits it" has no source. Implement the operator's choice
     (OPEN DECISION 2), with a test per branch.
   - `JsonTree.positiveInteger` accepts `"12"`, `1e1` and `3.0` against its Javadoc; `OnboardingProgress.fromMap` still truncates `steps`.
   - The two-aspect test asserts only a count from a hand-built frame (CodeRabbit nitpick, #377).
   - A public five-argument `StateDialogue` constructor has no production caller; `OnboardingSource.aspects()` is public.
   - `StateDialogueTest` and `DialogueSourceTest`: the seven total-steps variants become `@ParameterizedTest`; input from `Generators`.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - With a two-aspect list, a state of an excluded aspect renders as the chosen option says; fails on a revert. Evidence order: `test-first`.
4. **Open review items answered**
 - What: #377 has a CodeRabbit review-body nitpick that no comment answers, and thread 2 (the excluded aspect) was resolved contested:
   reply with the commit. #381 has the thread at `:423` (an already-decided job) only partly fixed: reply with the commit. Counts from the
   landings, to be re-read.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:junit-core
 - Done when:
   - `ci pr comments --pr-number 377` and `--pr-number 381` read 0 unresolved threads and no unanswered review body. Evidence order: `none`.
   - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open
     (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed. Evidence order: `none`.

5. **`config cleanup` and the overview's Cleanup row (PLAN-32 residue, folded 2026-11-12 on the operator's decision)**
 - What:
   - The `config` overview's last row (`ConfigCommand.java:233-238` at `948c51a1`) runs the top-level `cleanup` job view. The design
     (`08-transcripts-later.adoc:179`, `26-cleanup.adoc:29,38,177-182`) opens the durations editor `config cleanup`
     (`settings.cleanup-durations`, `settings.record`), and no `config cleanup` subcommand exists. Add `config cleanup [--project <id>|.]
     [--plans|--epics|--logs <days>|never]` as `26-cleanup.adoc#t-durations` draws it, and point the row at it.
   - The mock: plain-writable `cleanup.*` configuration keys (`MockRuntimeEngine.java:7409`) shadow the durations of `/machine/settings`.
     There are two sources of truth, and the 7-day minimum is bypassed. Keep one source and refuse a value under the minimum as the
     specification says.
   - The row's value `{days} days` has no plural form ("1 days"): the key is PLAN-30's (D6); this deliverable only uses it.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-documents:ref-asciidoc
 - Done when:
   - `ConfigCommandTest`: selecting the Cleanup row opens `config cleanup`; `config cleanup --plans 3` is refused under the minimum;
     `--project .` writes the project's value. A mock test: a `cleanup.*` write and `/machine/settings` agree. Each fails on a revert.
     Evidence order: `test-first`.
   - Terminal check of `config cleanup` (machine and `--project`), en and de. Evidence order: `none`.

## Standing Constraints

The terminal check of this plan captures cleanup with a 25-item job, `project add` with a later-state refusal, the release block and
a reduced aspect list, in `en` and `de`.

- Build and test through Maven only (`CLAUDE.md`). Besides the session's own checks, `./mvnw -pl pm-api,pm-mock-runtime,pm-operator verify`
  and `./mvnw -pl pm-api,pm-mock-runtime,pm-operator verify -Pintegration-tests` pass at the head, and so does
  `./mvnw -pl pm-api,pm-mock-runtime,pm-operator verify -Ppre-commit`, with every rewrite committed.
- No new dependency or plugin. Input test data comes from `Generators` with `@EnableGeneratorController`; expected catalogue text stays
  literal. Every new text is in en and de.
- Never catch or throw generic `Exception`/`RuntimeException` in production code; package-private over public; no compatibility shims.
- Terminal check through the screen capture harness (`ScreenCaptureIT`), saved as
  `pm-operator/src/test/resources/screen-captures/PLAN-128.txt`. Never run `pm-operator` in the agent's own shell.
- Ports of this plan: per `AGENTS.md` § Ports formula (`20000 + 10 × 128`): mock web port 21280, app port 21281. Other plans run at the same
  time: never stop or reuse a process this worktree did not start.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.
- **The merge rule:**
  - The merge question is asked only when every review thread on this pull request has a reply and is resolved, and every review body is
    answered; at least one CodeRabbit review of some head must exist (if CodeRabbit refuses on quota, stop and ask the operator).
  - The pull request body states the thread count, the resolved count and the review-body items with their count, read through `ci pr comments`.
  - The pull request is merged only on the operator's word. Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`.
    The pull request body carries no generator footer.
- ZERO Sonar findings (the operator, 2026-11-05): the pull request merges with 0 open new-code issues (SonarCloud project
  `plan-marshall_pm-mcp-clients`), every open issue fixed in a commit of THIS pull request. State `sonar_open_new_issues=N` in the
  landing message. A dismissal needs the operator's word.

## Claim Labels

- OBSERVED, read at `1cc0e10e` on 2026-11-10:
  - `CleanupCommand.java:778` prints "The runtime is stopping…" for a delete refusal; `:985-990` `glyphForKind` knows three kinds; `:455-459` strips
    `kind:` from `--exclude`; `:477-481` sends the delete after only printing the preview.
  - `operator.cleanup.more` occurs in `26-cleanup.adoc:317` and in no `.properties` file.
  - `ProjectCommand.java:204` posts `{path, pilot}`; `:222-234` does not catch `MissingInputException`; `:266-301` guesses a project id from the
    path basename; `:457,490` use the literal "Released by operator"; `:613` uses `"de".equals(locale)`.
  - `StateDialogue.java:665-675` `machineStepOf` falls through to `DONE`.
  - `StateDialogueTest` and `DialogueSourceTest` carry no `@ParameterizedTest` and no `@EnableGeneratorController`.
- OBSERVED, from the landings and not re-read: #377 has one unanswered review-body nitpick and a contested thread 2; #381's thread `:423` is
  partly fixed; the AGREED `26-cleanup.adoc` was edited without a recorded approval.
- HYPOTHESIS: the real daemon's `POST /api/v1/project-onboardings` takes the build environment in the body and returns an entry (continue link),
  not a `project_id`. Confirm or refute at `../plan-marshall-documentation` § `cli-and-security/01-cli-and-installer.adoc:129-131`,
  `runtime-model/01-processes-and-transport.adoc:266` and the route in `plan-marshall-mcp` (verify-at-outline).
- HYPOTHESIS: the runtime never sends a state of an excluded aspect. Confirm or refute at the onboarding state catalogue of the specification and
  `OnboardingSource` (verify-at-outline).
- Verify-first clause: re-read each named line at the current HEAD before editing. PLAN-123 changes `CleanupCommand` (zone and German dates) and
  `StateDialogue` (unit lookup); PLAN-84 changes the catalogue pairs.

## Expected Surface

- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/CleanupCommand.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ProjectCommand.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/dialogue/StateDialogue.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/dialogue/ChoicePrompt.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/StartPrompt.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ProjectOnboardingSource.java` — HYPOTHESIS (named by the landing); sweep at outline
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/resource/EnrolRequest.java` — present at `1cc0e10e`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/resource/EnrolResult.java` — present at `1cc0e10e`
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/resource/EnrolRecordsTest.java` — present at `1cc0e10e`
- OBSERVED: `pm-api/src/main/resources/messages/operator.en.properties` — present at `1cc0e10e`
- OBSERVED: `pm-api/src/main/resources/messages/operator.de.properties` — present at `1cc0e10e`
- OBSERVED: `pm-api/src/main/resources/messages/projects.en.properties` — present at `1cc0e10e`
- OBSERVED: `pm-api/src/main/resources/messages/projects.de.properties` — present at `1cc0e10e`
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` — present at `1cc0e10e`
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/EnrolScenarioTest.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/CleanupCommandTest.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ProjectCommandTest.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/EnrolCommandTest.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/dialogue/StateDialogueTest.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/DialogueSourceTest.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/CleanupDialogueIT.java` — HYPOTHESIS (named by the landing); sweep at outline
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/ProjectSetupDialogueIT.java` — HYPOTHESIS (named by the landing); sweep at outline
- OBSERVED: `doc/design/terminal-client/26-cleanup.adoc` — present at `1cc0e10e`
- OBSERVED: `doc/design/terminal-client/17-projects.adoc` — present at `1cc0e10e`
- OBSERVED: `doc/design/terminal-client/01-commands.adoc` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ConfigCommand.java` — present at `948c51a1` (D5, PLAN-32 residue)
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ConfigCommandTest.java` — present at `948c51a1` (D5)
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/http/MockConfigRoutes.java` — present at `948c51a1` (D5)
- OBSERVED: `doc/design/terminal-client/08-transcripts-later.adoc` — present at `948c51a1` (D5, read)
- OBSERVED: `pm-operator/src/test/resources/screen-captures/PLAN-128.txt` — absent; created by this plan

## Dependencies and Sequencing

- Depends on: `PLAN-160` (the `config` residue: the same `ConfigCommand.java` overview; this plan runs after it), `PLAN-23` (shipped, #381), `PLAN-121` (shipped, #390), `PLAN-119` (shipped, #377), `PLAN-123` (running: `CleanupCommand`, `StateDialogue`),
  `PLAN-84` (running: the catalogue pairs, `01-commands.adoc`)
- Overlaps with: `PLAN-30` (`CleanupCommand` literals, `01-commands.adoc`, the catalogue pairs: this plan runs BEFORE it), `PLAN-44` (the glyph guard moves
  the glyph literals of `CleanupCommand`: starts after), `PLAN-126` and `PLAN-127` (the catalogue pairs: run in series),
  `PLAN-79` (rewrites every `pm-operator` test: last)
- Adjacent to: `PLAN-33` (verifies V-06 and the cleanup checks; records the commit it verified, does not wait)

## Hand-Off Command

Lane: `runbook`.

/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-128-cli-cleanup-project-onboarding-residue.md

## Write-Boundary

The session implementing this spec touches only its own repository source and tests (and, where a deliverable says so, the sibling repository
`plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message. The
orchestrator owns every other ledger write. The qualifiers of the inbox exception, and the sole sanctioned write mechanism, are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.

## Operator Decisions 2026-11-10 (Amendment)

Recorded in the epic's decision log (AskUserQuestion, 2026-11-10). They settle the open points above and supersede any text here that differs.

- **Excluded aspect (decision 2):** a state of an aspect not in the injected list shows no step name (the overview lists only the injected aspects); never `Done`.
- **`26-cleanup.adoc` (decision 7):** the AGREED text wins: the confirmation gets back `back` and `?`, and the 7 keys the code added are entered in the document's text table; the lane's other unapproved edits revert to the AGREED wording.
- **Argument no state asked for (decision 8):** implemented as `01-commands.adoc:172` says; the sentence stays.

## PLAN-32 Residue Fold 2026-11-12 (Amendment)

From `landings/PLAN-32.md` (operator, AskUserQuestion): deliverable 5 adds `config cleanup` and points the overview's Cleanup row at it,
and makes the mock's durations one source. Surface +4 paths (`ConfigCommand.java`, `ConfigCommandTest.java`, `MockConfigRoutes.java`,
`08-transcripts-later.adoc` read). Runs after `PLAN-160` and `PLAN-126`.
