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
- OBSERVED: `pm-operator/src/test/resources/screen-captures/PLAN-127.txt` — absent; created by this plan

## Dependencies and Sequencing

- Depends on: `PLAN-124` (shipped, #385), `PLAN-120` (shipped, #382), `PLAN-123` (running: `AnswerCommand` waiver seam, the printed epic in `EpicCommand`)
- Overlaps with: `PLAN-30` (`AnswerCommand`, `OperatorRefusedException`, `EpicCommand` literals: this plan runs BEFORE it),
  `PLAN-126` and `PLAN-128` (`EpicCommand`, the catalogue pairs: run in series), `PLAN-44` (`AnswerCommand`: starts after),
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
