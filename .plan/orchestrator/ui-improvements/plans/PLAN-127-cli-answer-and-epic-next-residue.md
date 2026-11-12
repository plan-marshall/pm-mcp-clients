# PLAN-127: Terminal: The Answer Verb and `epic next`, Made Correct (PLAN-124, PLAN-120 Residue)

epic: ui-improvements
workstream: WS-04

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Staged on the operator's decision of
> 2026-11-10 from `landings/PLAN-124.md`, `landings/PLAN-120.md` and PLAN-33's finding 3. This spec is SELF-SUFFICIENT.

## Objective

PLAN-124 (#385, `be8c8403`) made the answer dialogue ask every part and sanitize through one writer. It left minor defects that
change what is sent or shown: a dissenter named from the wrong vote, a multi-part guard that fails open, an unscoped 404 fallback,
a German fallback that reads "von das Projekt", sanitizer gaps, and tests that cannot fail. PLAN-120 (#382, `c0db368d`) fixed the
terminal's reading of an unknown running count, but the `epic next` VERB still never reads the epic and never asks, against
`19-actions.adoc:188-190`. PLAN-33 found that the waiver path has no `q`-first and no unknown-input PTY case. Close these.
The printing and exit of REFUSALS is not here: it belongs to the refusal renderer of PLAN-30 (its amendment of 2026-11-10).

## Deliverables

1. **The answer verb reads and sends what the decision says**
 - What:
   - `dissentForSummary` (`AnswerCommand.java:983-1000`) names the first voter of any disagreeing option that has a reason,
     before it matches the summary text. Match by the summary first; keep the order only as the fallback.
   - The multi-part direct-key guard (`~:392-401`) returns "no refusal" when the envelope could not be read, so a single key is
     sent for a multi-part decision. Fail closed: refuse with a catalogue text and a non-zero exit.
   - `sendAnswer` (`:430-436`) retries the unscoped `/api/v1/questions/{id}/answers` on any 404, also when `--plan` scoped the
     request. Retry only when no `--plan` was given.
   - `showDecision` (`:144-157`) parses the body of any non-404 answer as a decision (a 401 or 500 becomes an empty decision).
     Treat a non-200 as a failed read.
   - An option key of ten or more digits aborts every view with a `NumberFormatException` (`:1130-1134`; `:1251` already bounds
     the digit count): bound it everywhere.
   - UNCLEAR with no clarification question is posted as understood, and a typo at the read-back silently drops the reading
     (`~:1428-1457`, verify the lines): ask again.
   - The multi-part confirmation title ignores a costly decision (`:1265` `confirm_title_parts`): follow `confirmTitleKey`.
   - EOF at a prompt exits 0 with no "Left" line (`:~783-785,1228-1230,591-593`): print the line; the exit code is OPEN DECISION 5.
   - The German fallback `decision.project.fallback=das Projekt` (`decisions.de.properties:110`) is used after "von" and "in"
     (`:13,54,63,96,97,101,190`): "das Setup von das Projekt". Choose a fallback that reads right in every template
     (for example `dem Projekt`), or one key per case; pin all templates in `de`.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - Tests: a dissent attributed to the voter whose reason matches the summary; a failed envelope read refuses a direct key on
     a multi-part id; a `--plan` 404 sends nothing unscoped; a 500 on the first GET is a failed read; a 10-digit key does not
     throw; each fails on a revert. Evidence order: `test-first`.
   - A German test asserts the six templates of `decisions.de.properties` with no project on the envelope read grammatically.
     Evidence order: `test-first`.
2. **A sanitizer that removes what is not text, and tests that can fail**
 - What:
   - `TerminalSanitizer.sanitize` (`TerminalSanitizer.java:49-70`) keeps everything that is not an ISO control character, so bidi
     controls (U+202A-202E, U+2066-2069), zero-width characters and tag characters (U+E0000-E007F) in model text pass. Remove the
     chosen set (OPEN DECISION 3) from the answer verb's output, with a test per class of character.
   - `AnswerSanitizationTest`: the direct-path test cannot fail (its line already used `stripControlSequences`); make it fail on a
     revert. Add sink unit tests (LF/TAB collapse, exception-message sanitizing). Make the four paths and the three nouns
     `@ParameterizedTest`. Input from `Generators` with `@EnableGeneratorController` in `AnswerMultipartTest`,
     `AnswerRefusalTest`, `AnswerSanitizationTest`, `AnswerDialogueIT`.
   - `AnswerDialogueIT.waiverAnswers` (`:102-118`, PLAN-33 finding 3, CLI-CC-V-06) covers only `y`: add `q` first (nothing sent) and an
     unknown input (re-asked, nothing sent) on `WR-2`.
   - `AcceptEditDialogueIT` (2 tests, PLAN-33 result for CLI-CC-V-06, `verify-cli-acts.adoc` at `344515f9`) has no `q`-first case:
     add `q` first (nothing sent, `/mock/acts` empty) and an unknown input (re-asked, nothing sent), so every dialogue that acts
     has the three PTY cases.
   - The mock README (`pm-mock-runtime/README.adoc:137`) documents the `recordActBodies` flag PLAN-124 added.
 - Persona: plan-marshall:persona-module-tester
 - Skills: pm-dev-java:junit-core, pm-dev-java:junit-integration, pm-dev-java-cui:cui-testing, pm-dev-java:java-security
 - Done when:
   - A hostile envelope with bidi, zero-width and tag characters prints none of them on the dialogue, the `--no-input` and the
     direct paths; fails on a revert. Evidence order: `test-first`.
   - `WR-2` with `q` first posts nothing; with an unknown input it re-asks and posts nothing. Evidence order: `test-first`.
3. **`epic next` asks, as the design says**
 - What:
   - `EpicCommand.NextCommand` (`EpicCommand.java:396-428`) posts `raise_scope` from its flag (`:420`), never reads the epic, and
     ignores `--no-raise` (`:409-410`). Per `19-actions.adoc:188-190`: read the epic; when no plan is launchable and the scope is
     full or the running count is unknown, ask to raise by one (`control.epic.scope_full` / `control.epic.scope_unknown`, reuse
     `WorkActionDialogue.java:297-316`); `--raise-scope` answers yes, `--no-raise` no, `--no-input` without either stops with
     exit 3; `raise_scope` is sent only on the operator's answer. The started line names the raised scope. If the operator prefers
     the design softened instead, see OPEN DECISION 6.
   - Texts through the catalogue: the success literal (`:423`) and the option descriptions (`:406-410`).
   - `EpicCommand.java:~191` (the printed epic's wait text) has no test; `control.epic.next_invalid` is pinned in `en` only
     (`ControlCatalogueTest:125`); `ControlCatalogueTest:55-66` hard-codes `requiredKeys`: derive it from the specification's key list
     (CodeRabbit thread (a) on #382).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - Tests for unknown count, full scope, `--raise-scope`, `--no-raise`, `--no-input` and a plain run assert the posted body and
     the exit code; each fails on a revert. Evidence order: `test-first`.
   - `next_invalid` is pinned in `en` and `de`, and the printed-epic wait text is covered. Evidence order: `test-first`.
4. **Open review items answered**
 - What: #382 has two threads unresolved and unanswered on its final head (`ControlCatalogueTest:61`, `WorkActionDialogue:307`):
   reply with the commit that fixes each and resolve. #385 has two threads that CodeRabbit resolved without a reply
   (`MockRuntimeEngine`, `OperatorRefusedException`): reply with the reason. Counts from the landings, to be re-read.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:junit-core
 - Done when:
   - `ci pr comments --pr-number 382` and `--pr-number 385` read 0 unresolved threads and no unanswered review body.
     Evidence order: `none`.
   - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open
     (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed. Evidence order: `none`.

5. **The needs surfaces made consistent (PLAN-126 residue, folded 2026-11-12 on the operator's decision)**
 - What: read `landings/PLAN-126.md`; lines are as read at `496ac925`.
   - **The first addition is announced.** `NeedsOperatorNotices.java:88-96` takes the first summary of a place as a baseline and returns no
     notice, and `lastSeen` is never seeded. A credential arriving as the first event after the console starts raises no notice,
     notification or bell. The class Javadoc (`:39-41`) says the opposite. Seed `lastSeen` from the status read's `needs_operator` at console
     start (the console and `OverviewFullScreenApp`'s own instance), keep the baseline rule only for what the status read already showed,
     and correct the Javadoc. CodeRabbit review `5870515770` ("Seed the baseline") raised it.
   - **`!` is the printed derivation.** `OverviewFullScreenApp.openNeedsItemAct` (`:1396-1470`) derives the act separately from
     `OverviewFormatting.formatNeedsItemCommand` (`:463`), against PLAN-126's "one derivation serves `needs`, the rows and `!`". Cases that
     differ today: a plan's `landing_failed` prints `epic <e>` but `!` opens the plan's hold dialogue (`:1428`); an epic-only
     `condition`/`stalled`/`waits_for_you` prints `epic <id>` but `openPlanAct` (`:1463`) does nothing; a decision without a link falls back to
     a bare `answer` (`:1424`). Make `!` run exactly the printed command, from one function.
   - **The printed command resolves.** `landing_failed` with a plan only prints `epic <planId>` (`OverviewFormatting.java:485-493`), an id no
     epic has. `held` with a plan prints `plan <id>`, while `04-console.adoc:76-80` says `epic <id>`. Print what the design table says and
     what resolves. If the table itself names an unresolvable command, stop and ask. PLAN-126's spec pinned `epic <planId>`: that pin is wrong.
   - **Names and pages.** `04-console.adoc:74-82` names the badge key `waiver_request` while the contract's kind is `waiver` (both render):
     one name, as the contract says. `needs --page` and the runtime's `?page=` (added by PLAN-126) are in neither `01-commands.adoc:124` nor
     `doc/design/shared/needs-and-find.adoc`: document them, and make `--page` beyond the last page say so instead of "Nothing waits" while
     `total` > 0. `overview.needs.other` goes into the design's text table. An `OTHER` notice prints the raw word `other`: use the catalogue.
     The subject's fallback to a raw id (`OverviewFormatting.java:395`) follows `Display.title`'s rule.
   - **The mock serves one list.** `MockRuntimeEngine.mergeMachineNeeds` (`:5807`) is a fallback that emits `waiver_request` where the list
     path emits `waiver`: remove it or make it emit the contract's kind.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-documents:ref-asciidoc
 - Done when:
   - `ConsoleNeedsNoticeTest`: a credential that arrives as the first event after a start whose status read showed none raises one notice;
     an item the status read already showed raises none. Fails on a revert. Evidence order: `test-first`.
   - A test drives `!` on every kind and asserts it runs the printed command (one parameterised case per kind, including the three cases
     above). Fails on a revert. Evidence order: `test-first`.
   - Terminal check of the needs group and `needs` at 120 × 30 and 80 × 24, en and de, taken at the PR head (PLAN-126's capture was taken
     before its last change and is stale). Evidence order: `none`.
6. **The tests PLAN-126 left weak, the raw-mode sibling, and the review items**
 - What:
   - **`find` lease.** The shell-path lease fix in `FindCommand.open` (`:394-405`) has no test (PLAN-126 D5 not met). Model one on
     `FindCommandTest.leaveIsNotFinished`: a DumbTerminal, input `1\n`, the chosen number reaching the reader, and the terminal supplier called
     exactly once. If that cannot reproduce the shell path, say why in the landing.
   - **`SecretReader` raw mode.** `SecretReader.java:56-66` keeps ISIG on after `enterRawMode`: a Ctrl+C while the echo is off ends the JVM
     before the `finally` restore, and the terminal can stay echo-off. Handle it as `FindCommand.readField` does (`:421-440`: ISIG cleared,
     Ctrl+C read as a key, the attributes restored), with a `SecretReaderTest` case that fails on a revert.
   - **D4 of PLAN-126.** `NeedsIT` starts at project level (`:40`, `:59`), so nothing calls `openNeedsItemAct` (only `openWhatWaits`,
     `OverviewFullScreenApp.java:1264-1270`): add a start at the overview. `needsItemFormattingExhaustive` becomes a `@ParameterizedTest` per
     kind. `NeedsOneCountTest.sameCountsAcrossFourSurfaces` pins only the total: assert the counts per kind. `NeedsCommandTest:73-76` asserts
     catalogue text, not lowercase substrings. `OverviewFormattingTest:299,314` drop the `fr`→`en` fallback assertions. `NeedsIT.selectByTitle`
     loses its always-null `marker` parameter. `TuiCapabilityIT:245` derives the level list instead of hard-coding it. `ConsoleNeedsNoticeTest`,
     `NeedsSelectionTest` and `OverviewLoadFailureTest` draw input from `Generators`. `rowIdOf` must not collide for two items of one kind and link.
   - **Refusals.** `LoadRefusal.of(.., IOException)` prints a raw, unlocalised exception message as the cause: print the catalogue cause.
     The refusal fields of `OverviewFullScreenApp` are written by the refresh thread and read by the render thread: make them safe to share.
   - **Javadoc artefacts.** A stray `/**` inside `PmConsole.notifyOperator`'s Javadoc (`PmConsole.java:885`); in `MockRuntimeEngine.java:778-780`
     the merge put `NEEDS_PAGE_SIZE` between the operator-channel Javadoc and `OPERATOR_FIELD`; `NeedsOperatorNotices.java:49` says the place of
     a due cleanup is empty (the code gives the "machine" word); `TuiKeyRegistry.keepCells` lost `@param more`; `ConsoleBadge.java` has no
     newline at the end.
   - **#416.** Three CodeRabbit body items have no reply: the additional comment on `TuiCapabilityIT:245` (review `5867641766`), and
     "status stale after answer" and "Seed the baseline" (review `5870515770`). The mock-page outside-diff item of `5870515770` was fixed in
     `33182e1b`; its only reply sits on an unrelated thread. Reply to each on #416 with the commit of this plan that settles it, or with the
     reason it is declined. Check "status stale after answer" against the head first.
 - Persona: plan-marshall:persona-module-tester
 - Skills: pm-dev-java:junit-core, pm-dev-java:junit-integration, pm-dev-java-cui:cui-testing
 - Done when:
   - The `find` lease test and the `SecretReader` Ctrl+C test each fail on a revert of their fix. Evidence order: `mutation-after` for
     `find` (the fix exists), `test-first` for `SecretReader`.
   - `NeedsOneCountTest` fails when one kind's count changes and the total does not. Evidence order: `mutation-after`.
   - Landing obligation (rule 4): `ci pr comments --pr-number 416` shows a reply to each of the four items. Report it in the landing message.

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`). Besides the session's own checks, `./mvnw -pl pm-api,pm-mock-runtime,pm-operator verify`
  and `./mvnw -pl pm-api,pm-mock-runtime,pm-operator verify -Pintegration-tests` pass at the head, and so does
  `./mvnw -pl pm-api,pm-mock-runtime,pm-operator verify -Ppre-commit`, with every rewrite committed.
- No new dependency or plugin. Input test data comes from `Generators` with `@EnableGeneratorController`; expected catalogue text stays
  literal. Every new text is in en and de.
- Never catch or throw generic `Exception`/`RuntimeException` in production code; package-private over public; no compatibility shims.
- Terminal check through the screen capture harness (`ScreenCaptureIT`), saved as
  `pm-operator/src/test/resources/screen-captures/PLAN-127.txt`: a multi-part decision, a hostile envelope shown cleaned, and
  `epic next` asking, in `en` and `de`. Never run `pm-operator` in the agent's own shell.
- Ports of this plan: per `AGENTS.md` § Ports formula (`20000 + 10 × 127`): mock web port 21270, app port 21271. Other plans run at the same
  time: never stop or reuse a process this worktree did not start.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.
- **The merge rule:**
  - The merge question is asked only when every review thread on this pull request has a reply and is resolved, and every review
    body is answered; at least one CodeRabbit review of some head must exist (if CodeRabbit refuses on quota, stop and ask the operator).
  - The pull request body states the thread count, the resolved count and the review-body items with their count, read through
    `ci pr comments`, never from memory. A landing message whose review counts disagree with the PR is a defect of the run.
  - The pull request is merged only on the operator's word. Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`.
    The pull request body carries no generator footer.
- ZERO Sonar findings (the operator, 2026-11-05): the pull request merges with 0 open new-code issues (SonarCloud project
  `plan-marshall_pm-mcp-clients`), every open issue fixed in a commit of THIS pull request. State `sonar_open_new_issues=N` in the
  landing message. A dismissal needs the operator's word.

## Claim Labels

- OBSERVED, read at `1cc0e10e` on 2026-11-10:
  - `AnswerCommand.java:983-1000` `dissentForSummary` takes `findFirst()` over disagreeing options' voters with a reason before it matches the summary.
  - `AnswerCommand.java ~392-401` returns no refusal when `envelope == null`.
  - `AnswerCommand.java:430-436` retries the unscoped answers path on any 404.
  - `AnswerCommand.java:144-157` parses the body of a non-404 as a decision; `:1130-1134` uses `\d+` and `Integer.parseInt`.
  - `decisions.de.properties:110` is `das Projekt`; the templates at `:13,54,63,96,97,101,190` put `{project}` after "von"/"in".
  - `TerminalSanitizer.java:49-70` keeps any character that is not `isISOControl`.
  - `EpicCommand.java:406-410,420,423`: `--no-raise` is declared and unused; the payload is the flag; the success line is an English literal.
  - `AnswerSanitizationTest`, `AnswerMultipartTest`, `AnswerRefusalTest` and `AnswerDialogueIT` carry no `@EnableGeneratorController`.
  - `AnswerDialogueIT.waiverAnswers` types `y\ncovered by CI\n` only.
- OBSERVED, from the landings and not re-read: the direct-path sanitization test is vacuous (`landings/PLAN-124.md` layer 2 D1); #382 has 2 unresolved
  threads and #385 has 2 resolved without a reply.
- HYPOTHESIS: the runtime treats `raise_scope` as the operator's answer only; whether the real daemon reads the epic's running count is
  not shown. Confirm or refute at the API specification (`GET /api/v1/epics/{epic_id}`) and `EpicResource` (verify-at-outline).
- Verify-first clause: re-read each named line at the current HEAD before editing. PLAN-123 changes `AnswerCommand` (`waiverGate()` seam,
  `:1559-1563` in its amendment) and the printed epic in `EpicCommand`; line numbers move.

## Expected Surface

- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/AnswerCommand.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/TerminalSanitizer.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/EpicCommand.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/WorkActionDialogue.java` — present at `1cc0e10e`
- OBSERVED: `pm-api/src/main/resources/messages/decisions.en.properties` — present at `1cc0e10e`
- OBSERVED: `pm-api/src/main/resources/messages/decisions.de.properties` — present at `1cc0e10e`
- OBSERVED: `pm-api/src/main/resources/messages/control.en.properties` — present at `1cc0e10e`
- OBSERVED: `pm-api/src/main/resources/messages/control.de.properties` — present at `1cc0e10e`
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/message/ControlCatalogueTest.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/AnswerMultipartTest.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/AnswerRefusalTest.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/AnswerSanitizationTest.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/TerminalSanitizerTest.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/EpicCommandTest.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ControlDialoguesTest.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/AnswerDialogueIT.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/AcceptEditDialogueIT.java` — present at `344515f9` (accept-edit gap of CLI-CC-V-06, folded 2026-11-10)
- OBSERVED: `pm-mock-runtime/README.adoc` — present at `1cc0e10e`
- OBSERVED: `doc/design/terminal-client/19-actions.adoc` — present at `1cc0e10e` (only if OPEN DECISION 6 softens the sentence)
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/NeedsOperatorNotices.java` — present at `496ac925` (D5/D6, PLAN-126 residue, folded 2026-11-12)
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/overview/OverviewFullScreenApp.java` — present at `496ac925` (D5/D6, PLAN-126 residue, folded 2026-11-12)
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/overview/OverviewFormatting.java` — present at `496ac925` (D5/D6, PLAN-126 residue, folded 2026-11-12)
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/FindCommand.java` — present at `496ac925` (D5/D6, PLAN-126 residue, folded 2026-11-12)
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/SecretReader.java` — present at `496ac925` (D5/D6, PLAN-126 residue, folded 2026-11-12)
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/PmConsole.java` — present at `496ac925` (D5/D6, PLAN-126 residue, folded 2026-11-12)
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/NeedsCommand.java` — present at `496ac925` (D5/D6, PLAN-126 residue, folded 2026-11-12)
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` — present at `496ac925` (D5/D6, PLAN-126 residue, folded 2026-11-12)
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/FindCommandTest.java` — present at `496ac925` (D5/D6, PLAN-126 residue, folded 2026-11-12)
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/SecretReaderTest.java` — present at `496ac925` (D5/D6, PLAN-126 residue, folded 2026-11-12)
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ConsoleNeedsNoticeTest.java` — present at `496ac925` (D5/D6, PLAN-126 residue, folded 2026-11-12)
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/NeedsOneCountTest.java` — present at `496ac925` (D5/D6, PLAN-126 residue, folded 2026-11-12)
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/NeedsCommandTest.java` — present at `496ac925` (D5/D6, PLAN-126 residue, folded 2026-11-12)
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/overview/NeedsSelectionTest.java` — present at `496ac925` (D5/D6, PLAN-126 residue, folded 2026-11-12)
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/overview/OverviewFormattingTest.java` — present at `496ac925` (D5/D6, PLAN-126 residue, folded 2026-11-12)
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/NeedsIT.java` — present at `496ac925` (D5/D6, PLAN-126 residue, folded 2026-11-12)
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/TuiCapabilityIT.java` — present at `496ac925` (D5/D6, PLAN-126 residue, folded 2026-11-12)
- OBSERVED: `doc/design/terminal-client/04-console.adoc` — present at `496ac925` (D5/D6, PLAN-126 residue, folded 2026-11-12)
- OBSERVED: `doc/design/terminal-client/01-commands.adoc` — present at `496ac925` (D5/D6, PLAN-126 residue, folded 2026-11-12)
- OBSERVED: `pm-operator/src/test/resources/screen-captures/PLAN-127.txt` — absent; created by this plan

## Dependencies and Sequencing

- Depends on: `PLAN-124` (shipped, #385), `PLAN-120` (shipped, #382), `PLAN-123` (running: `AnswerCommand` waiver seam, the printed epic in `EpicCommand`)
- Overlaps with: `PLAN-30` (`AnswerCommand`, `OperatorRefusedException`, `EpicCommand` literals: this plan runs BEFORE it),
  `PLAN-126` (shipped, #416, `496ac925`: this plan carries its residue), `PLAN-128` (`EpicCommand`, the catalogue pairs: run in series), `PLAN-133` and `PLAN-160` (running: `MockRuntimeEngine.java`; the second to land rebases), `PLAN-44` (`AnswerCommand`: starts after),
  `PLAN-79` (rewrites every `pm-operator` test: last)
- Adjacent to: `PLAN-33` (verifies the answer verb; records the commit it verified, does not wait)

## Hand-Off Command

Lane: `runbook`.

/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-127-cli-answer-and-epic-next-residue.md

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under
`.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger write. The qualifiers of
the inbox exception, and the sole sanctioned write mechanism, are stated in `persona-plan-orchestrator/standards/orchestration-model.md`
§ Ledger Write-Boundary.

## Operator Decisions 2026-11-10 (Amendment)

Recorded in the epic's decision log (AskUserQuestion, 2026-11-10). They settle the open points above and supersede any text here that differs.

- **Sanitizer scope (decision 3):** strip bidi controls, zero-width characters (including U+200D) and tag characters from all operator-visible model text; the emoji-joiner loss is accepted.
- **EOF at an answer prompt (decision 5):** exit 0 with the `Left` line.
- **`epic next` (decision 6):** the verb reads the epic and asks as `19-actions.adoc:190` says; `--no-raise` is honoured.

## PLAN-126 Residue Fold 2026-11-12 (Amendment)

From `landings/PLAN-126.md` (operator, AskUserQuestion — "Fold into PLAN-127"; side items "Both in the residue plan"): deliverable 5 makes the
needs surfaces consistent (the first addition announced, `!` as the printed command, resolvable commands, names and pages, one mock list);
deliverable 6 strengthens PLAN-126's tests, fixes the `SecretReader` raw-mode sibling and two Javadoc artefacts, and answers the four #416
review items. Surface +19 paths. The plan is now large for the runbook lane: re-check the lane by the size rule at emit.
