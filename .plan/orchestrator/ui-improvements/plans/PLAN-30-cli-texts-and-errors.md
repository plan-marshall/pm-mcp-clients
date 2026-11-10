# PLAN-30: Terminal: Every Text Through the Catalogue, One Error Pattern

epic: ui-improvements
workstream: WS-05

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Move every fixed string of the dialogues into the catalogue, give Picocli a resource bundle and custom handlers, render every refusal by error code in three lines with a fix, set the exit codes, and guard against literals (CLI-CC-S-03 and CLI-UX-S-03; CLI-CC-P-01, CLI-CC-P-04, CLI-UX-P-05).

Source of this plan (read before scoping):

- `doc/claude_review/cli/cross-cutting/plan.adoc#CLI-CC-S-03`
- `doc/claude_review/cli/ux/plan.adoc#CLI-UX-S-03`
- `doc/claude_review/cli/cross-cutting/proposals.adoc#CLI-CC-P-01`
- `doc/claude_review/cli/cross-cutting/proposals.adoc#CLI-CC-P-04`
- `doc/claude_review/cli/ux/proposals.adoc#CLI-UX-P-05`

## Deliverables

1. **Strings into the catalogue**
   - What: Every string of `AnswerCommand`, `OnboardCommand`, `WorkActionDialogue`, `CleanupCommand`, `RuntimeCommand` in `operator.en/de.properties`.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:javadoc, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-dev-java-cui:cui-logging
   - Done when:
     - `LiteralTextGuardTest` (extended) scans for literals passed to `print`, `println`, `printf`, `readLine`, `askRaw` and fails on one (evidence order: `test-first`). Re-grounded at HEAD cc5794c9: its render-call pattern already includes `prompt.askRaw` (`LiteralTextGuardTest.java:61-81`) and its guarded list already names `StartDialogue`, `ChatDialogue`, `WorkActionDialogue` and `HoldDialogue`, so the extension is the guarded list, not the pattern: add `AnswerCommand`, `CleanupCommand` and `RuntimeCommand` (and the sub-packages) to it, and clear what it then finds.
     - Terminal check (screen capture) shows this deliverable's change as specified on: `running-work` in `de_DE.UTF-8` and `en_US.UTF-8`: `answer Q-999 1`, `plan nope`, `bogus`, `pm-operator -h`, with exit codes; before and after captures recorded (evidence order: `none`)
2. **RefusalRenderer**
   - What: `OperatorRefusedException` carries the `pm-api` error code; `RefusalRenderer` prints `✗` line (the title), cause, `→ fix` from the shared set `PLAN-86` (`ErrorTexts` over `error.<code>.title`, `.cause`, `.fix`; no `operator.refusal.*` family is added); unknown code "The runtime refused: <message>" over `error.unknown.*`; never an HTTP status, a JSON body or an exception class.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:javadoc, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-dev-java-cui:cui-logging
   - Done when:
     - A unit test: `RefusalRenderer` prints the three lines for a known code and the unknown fallback for any other, in both languages, and never an HTTP status, a JSON body or an exception class (the check that every code has its texts belongs to `PLAN-86`) (evidence order: `test-first`)
     - Terminal check (screen capture) shows this deliverable's change as specified on: `running-work` in `de_DE.UTF-8` and `en_US.UTF-8`: `answer Q-999 1`, `plan nope`, `bogus`, `pm-operator -h`, with exit codes; before and after captures recorded (evidence order: `none`)
3. **Picocli in the operator's language**
   - What: `CommandLine.setResourceBundle` for usage; an `IParameterExceptionHandler` prints `✗ bogus is not a verb. → help lists them.`
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:javadoc, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-dev-java-cui:cui-logging
   - Done when:
     - In `de_DE.UTF-8`, `bogus` and `pm-operator -h` print no English word outside the glossary (evidence order: `test-first`)
     - Terminal check (screen capture) shows this deliverable's change as specified on: `running-work` in `de_DE.UTF-8` and `en_US.UTF-8`: `answer Q-999 1`, `plan nope`, `bogus`, `pm-operator -h`, with exit codes; before and after captures recorded (evidence order: `none`)
4. **Exit codes**
   - What: `1` refused, `2` usage, `3` needs input, as in `01-commands.adoc#exit-codes`.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:javadoc, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-dev-java-cui:cui-logging
   - Done when:
     - `ExitCodesTest` checks success, refusal and usage error of every verb (evidence order: `test-first`)
     - Terminal check (screen capture) shows this deliverable's change as specified on: `running-work` in `de_DE.UTF-8` and `en_US.UTF-8`: `answer Q-999 1`, `plan nope`, `bogus`, `pm-operator -h`, with exit codes; before and after captures recorded (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-operator verify -Pintegration-tests`, `./mvnw -pl pm-api verify`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- CUI standards (`CLAUDE.md`, `PLAN-77`): a test that takes input data draws it from `Generators` with `@EnableGeneratorController` (the expected value of a contract stays literal); a log call at INFO or above goes through a `LogRecord` of the module's own `LogMessages` class (one per module that logs, never one for the repository) with a `LogAsserts` test; `@NullMarked` applies in a module that declares JSpecify (`pm-operator`, `pm-mock-runtime`); until `PLAN-77` has landed the test-scope CUI dependencies are missing in `pm-api`, `pm-exec`, `pm-relay` and `pm-mock-runtime`, and a test there follows plain JUnit and reports the departure, never adds the dependency itself.
- Java 25, Lombok, records; no generic `Exception`/`RuntimeException`; JUnit 5 only, no Mockito (CLAUDE.md).
- Every new operator text goes through `MessageCatalogue` into `operator.en.properties` and `operator.de.properties`.
- Terminal check of the rendered output through the screen capture harness of `cli-pty-dialogue-harness` (`ScreenCaptureIT`, command line in `doc/design/terminal-client/27-tui-testing.adoc`): write this plan's captures to `pm-operator/src/test/resources/screen-captures/{plan}.txt`, run the set BEFORE the change and copy `pm-operator/target/screens/{plan}/` to `.plan/temp/terminal/{plan}/before/`, then run it AFTER and copy to `.../after/`. Capture at 120 × 30 and 80 × 24 in `en_US.UTF-8` and `de_DE.UTF-8` unless the target names other sizes or environments. Read every `.txt` and `.attrs.txt` with the file tools and compare them with the deliverable; the screen text shows no raw id, link name, state id, `-` placeholder, ISO time or HTTP status, and no text of a previous level. Never run `pm-operator` in the agent's own shell: the operator gate refuses a coding agent by design (`PM-SEC-3`) and is never worked around; only the harness, which clears the environment like every PTY test, runs it. If the harness is not available, the terminal Done-when clauses are NOT met — a blocker, never skipped.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/cli/cross-cutting/plan.adoc#CLI-CC-S-03`, `doc/claude_review/cli/ux/plan.adoc#CLI-UX-S-03` — read at `doc/claude_review/cli/cross-cutting/plan.adoc`
  - verdict: corroborated | checked_at: cc0d62af | by: ui-improvements/cleanup | rescoped: n/a | evidence: cross-cutting/plan.adoc:31; ux/plan.adoc:28
- OBSERVED: `LiteralTextGuardTest` exists in `pm-operator` — read at `pm-operator/src/test/java/de/planmarshall/operator/LiteralTextGuardTest.java`
  - verdict: corroborated | checked_at: cc0d62af | by: ui-improvements/cleanup | rescoped: n/a | evidence: LiteralTextGuardTest.java exists
- HYPOTHESIS: `LiteralTextGuardTest` does not cover dialogues and `askRaw` — confirm/refute at its source (verify-at-outline)
  - verdict: contradicted | checked_at: cc0d62af | by: ui-improvements/cleanup | rescoped: yes | evidence: AnswerCommand clean (PLAN-21/124), OnboardCommand gone (PLAN-97); D1 narrows to Cleanup/Runtime + guard inversion (Re-grounding 2026-11-10)
- Exit codes (structure review, 2026-10-30): this plan owns the check of the one table in `01-commands.adoc#exit-codes` for every verb (`ExitCodesTest`); `PLAN-17` (exit `3` for `--no-input`) and `PLAN-26` (exit `1` on a failed read) implement their verbs from the same table before this plan runs, and this plan fails on a verb that breaks it.
  - verdict: corroborated | checked_at: cc0d62af | by: ui-improvements/cleanup | rescoped: n/a | evidence: ExitCodesTest absent; exit constants scattered; ReadVerbExitCodesTest exists
- Re-grounded note at HEAD 94bbefe1 (cleanup 2026-11-02): Stale: ExitCodes table in doc/design/terminal-client/01-commands.adoc#exit-codes now has 0,1,2,3,4 and 130 (spec names 1/2/3 only); RefusalRenderer, RefusalRendererTest and ExitCodesTest are still absent (only ReadVerbExitCodesTest exists from PLAN-26); ErrorTexts of PLAN-86 exists at pm-api/.../api/resource/ErrorTexts.java; 03-dialogues.adoc passage on common.error.<code> is now at line 269 (spec says 266) and 09-texts-common.adoc still lists common.error.* (lines 169-208), so the planned rewrite is still due. WebCommand/WorkChatDialogue exist and are guarded.
  - verdict: contradicted | checked_at: cc0d62af | by: ui-improvements/cleanup | rescoped: yes | evidence: 09-texts-common has no common.error tables (R4-14); only :29 and 03-dialogues:269 left
- RESCOPE at outline (design audit 2026-11-02): drop the table edits in 09/13 (R4-14: the terminal text tables 09-13 are replaced by links to the implemented .properties packs); move the OnboardCommand strings after D26 (top-level onboard is folded in or removed).
  - verdict: corroborated | checked_at: cc0d62af | by: ui-improvements/cleanup | rescoped: n/a | evidence: 09/13 table edits dropped; D26/PLAN-97 landed #358
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.
  - verdict: unverifiable | checked_at: cc0d62af | by: ui-improvements/cleanup | rescoped: n/a | evidence: process clause

- PLAN-97 (staged 2026-11-04): the `OnboardCommand` strings move after PLAN-97 deliverable 3 (the top-level `onboard` is folded into `pm-operator` without a verb and `config <aspect>`); the drain and accept-edit texts come from PLAN-97 deliverables 1 and 2.
  - verdict: corroborated | checked_at: cc0d62af | by: ui-improvements/cleanup | rescoped: n/a | evidence: OnboardCommand absent; Drain/AcceptEdit use the catalogue
- OBSERVED: OperatorRefusedException.java:33-36 builds the English literal "refused by the runtime (HTTP n)"; AnswerCommand rethrows it at :317,341,388,704,1333,1353,1368,1469,1498 — read at `pm-operator/src/main/java/de/planmarshall/operator/OperatorRefusedException.java` § `refusalMessage` at 1cc0e10e
- OBSERVED: decision.refused.* has two keys (invalid_answer, dependency_unanswered) and no pm-operator main code reads it — read at `pm-api/src/main/resources/messages/decisions.en.properties` :38-39
- HYPOTHESIS: the answer route's refusal body carries the four design codes and, for invalid_answer, a reason — confirm/refute at `pm-api` § `DecisionAnswerRequest`, the mock answer route in `MockRuntimeEngine` and the specification (verify-at-outline)
- OBSERVED: EpicCommand.NextCommand holds English option descriptions and the success literal — read at `pm-operator/src/main/java/de/planmarshall/operator/EpicCommand.java` :406-410,423

## Expected Surface

- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/AnswerCommand.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/WorkActionDialogue.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/CleanupCommand.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/RuntimeCommand.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/OperatorRefusedException.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/RefusalRenderer.java` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/PmOperator.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/LiteralTextGuardTest.java` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/RefusalRendererTest.java` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ExitCodesTest.java` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `doc/design/terminal-client/01-commands.adoc` — present at HEAD c4dc9bb4
- OBSERVED: `doc/design/terminal-client/13-texts-operator.adoc` — present at HEAD c4dc9bb4
- OBSERVED: `doc/design/terminal-client/09-texts-common.adoc` — present at HEAD 1b6c6ae3; lines 169 to 208 still describe a `common.error.<code>` family and say the codes' texts belong to the flows that raise them: this plan rewrites that passage to point at the `error` area of `PLAN-86` (added after its landing)
- OBSERVED: `doc/design/terminal-client/03-dialogues.adoc` — present at HEAD 1b6c6ae3; line 266 says the same: this plan corrects it (added after the landing of `PLAN-86`)
- OBSERVED: `pm-api/src/main/resources/messages/operator.en.properties` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/operator.de.properties` — present at HEAD c4dc9bb4
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/DrainCommand.java` — present at HEAD aa3a9be3
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/AcceptEditCommand.java` — present at HEAD aa3a9be3
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ConfigCommand.java` — present at HEAD aa3a9be3
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/WebCommand.java` — present at HEAD aa3a9be3
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/resource/ErrorTexts.java` — present (read) at HEAD aa3a9be3
- OBSERVED: `pm-operator/src/test/resources/screen-captures/PLAN-30.txt` — absent; created by this plan at HEAD aa3a9be3
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/PmConsole.java` — present at `cc0d62af` (added by the cleanup of 2026-11-10)
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/EntryFlow.java` — present at `cc0d62af` (added by the cleanup of 2026-11-10)
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/RuntimeActDialogue.java` — present at `cc0d62af` (added by the cleanup of 2026-11-10)
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/PlanCommand.java` — present at `cc0d62af` (added by the cleanup of 2026-11-10)
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/EpicCommand.java` — present at `cc0d62af` (added by the cleanup of 2026-11-10)
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/DecomposeCommand.java` — present at `cc0d62af` (added by the cleanup of 2026-11-10)
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/AnswerRefusalTest.java` — present at `1cc0e10e` (added by the residue fold of 2026-11-10)
- OBSERVED: `pm-api/src/main/resources/messages/decisions.en.properties` — present at `1cc0e10e` (added by the residue fold of 2026-11-10)
- OBSERVED: `pm-api/src/main/resources/messages/decisions.de.properties` — present at `1cc0e10e` (added by the residue fold of 2026-11-10)
- OBSERVED: `doc/design/terminal-client/14-transcripts-decisions.adoc` — present at `1cc0e10e` (added by the residue fold of 2026-11-10)

## Dependencies and Sequencing

- Depends on: `PLAN-21` (`cli-answer-dialogue`), `PLAN-22` (`cli-state-dialogue-onboarding`, shipped), `PLAN-23` (`cli-cleanup-dialogue`), `PLAN-17` (`cli-runtime-act-confirm`), `PLAN-86` (`shared-error-texts`)
- Overlaps with: touches the dialogue classes of WS-04: runs after them
- Adjacent to: none

## Re-grounding 2026-11-07 (cleanup)

Checked at `aa3a9be3`. `OnboardCommand.java` is gone (PLAN-97 folded it; only `OnboardCommandTest.java` remains): its strings in deliverable 1 are void. `GUARDED` in `LiteralTextGuardTest` lists StartDialogue, ChatDialogue, WorkActionDialogue and HoldDialogue but not AnswerCommand, CleanupCommand or RuntimeCommand (17-23 English literals each): the rescope to extending the list stands. `RefusalRenderer`, `RefusalRendererTest` and `ExitCodesTest` are absent (`ReadVerbExitCodesTest` exists); `ErrorTexts` exists in `pm-api` (PLAN-86) but `pm-operator` never uses it; `OperatorRefusedException` has no error code. `01-commands.adoc#exit-codes` now lists 0, 1, 2, 3, 4, 130: the "every verb" `ExitCodesTest` covers the PLAN-97 verbs (`drain`, `accept-edit`, `config`) and the codes 4 and 130. PLAN-21 and PLAN-23 (dependencies) are not landed.

## Fold 2026-11-08 (PLAN-118 landing)

Source: `landings/PLAN-118.md` (layer 2), read at `2fc2c214`. The exit-code table of `01-commands.adoc:190-191` defines `1` as "refused by the runtime: an error code of `pm-api`", but `drain` now exits `1` for its local refusals `operator.drain.not_identifiable` and `operator.drain.signal_failed` (`DrainCommand.java:119-121`, `25-system.adoc:39`), which are no runtime refusal. Since this plan settles the exit codes (`ExitCodesTest`): extend row `1` to local refusals, or give them a code of their own, and pin it. Surface unchanged (`01-commands.adoc`, `DrainCommand.java`, `ExitCodesTest.java` are listed).

## Re-grounding 2026-11-10 (cleanup, at `cc0d62af`)

This section supersedes the spec text and earlier notes where they differ.

- Dependencies PLAN-21 (#372), -22, -23 (#381), -17, -86 and PLAN-124 (#385), PLAN-97 (#358) have landed; the
  2026-11-07 "not landed" note is obsolete. The PLAN-77 CUI-dependency clause is obsolete (all modules declare
  `cui-test-generator`).
- **D1 narrower:** `AnswerCommand` has no render literals left (PLAN-21/124) and `OnboardCommand` is gone (PLAN-97). The
  remaining literals: `CleanupCommand` (~22: `:190,204,230-231,239,274,383,433,448,467,515-522,633-637,651,778,783`),
  `RuntimeCommand` (8: `:134,298,364,373,375,384,386,413`), and outside the five named classes `PlanCommand` ~13,
  `EpicCommand` ~13, `DecomposeCommand` ~6, `ClientCommand` ~5, `ReportsCommand`/`QuestionsCommand`/`ProjectsCommand` ~3,
  `NeedsCommand:91`, `ActionSupport` 1. The guard is an allow-list scanned with `Files.list` (top directory only) and its
  receiver regex misses `session.out()`/`terminal.writer()`: INVERT it (scan all of `src/main`, minus a named exception
  list). `CleanupCommand:515-522,651` print INVENTED figures when nothing is due ("214 · 340 MB", "612 old rows",
  "tomorrow 03:00", "30 days … 3 days"): moving them to the catalogue would freeze fake data — OPEN DECISION (drop them or
  read them from the runtime).
- **D2 wider:** `OperatorRefusedException` has a second constructor `(String)` for local refusals (`OperatorGate.verifyAct`,
  exit 4) — needs its own branch; no error-body reader exists (mock body `{"error":{"code","message"}}`,
  `MockRuntimeEngine.java:6466`) — say where the parse lives; the server message passes the sanitizer. Add PLAN-33's
  finding (a): the ten local 404 sites (`PlanCommand.java:152,184,881,1290`, `EpicCommand.java:115`,
  `DecomposeCommand.java:77,86,131,224,231`) print English "… not found." — route them through the renderer with
  `scope_not_found` (`error.en.properties:44`) and the id, exit 1. The consumers that print `getMessage()` —
  `PmConsole.java:315`, `EntryFlow.java:298`, `OverviewFullScreenApp` status line (`:861,945,2509,2873,2892,3321`) — are
  in scope of the renderer.
- **D3:** the fix text "→ help lists them" is wrong in the shell (`help` is console-only, `01-commands.adoc:149`); use
  `pm-operator --help`.
- **D4:** `ExitCodesTest` covers all 23 verbs and 0/1/2/3/4/130. Fold (PLAN-118) still open: `DrainCommand.java:119-121`
  exits 1 for local refusals.
- **Docs:** the 09/13 table edits are void (R4-14 replaced the tables); left: `03-dialogues.adoc:269` and
  `09-texts-common.adoc:29`.
- **PLAN-33 finding (b)** (a refused projects read renders as "Nothing runs", `OverviewFullScreenApp.fetchProjects:486`,
  also `fetchNeedsOperator:475`, `fetchExplorer`) is NOT added here: it needs an error state per full-screen level — a
  separate plan (owner: the orchestrator, staged on the operator's word).
- **Sequencing:** after PLAN-123 and PLAN-84 merge (shared `PlanCommand`, `EpicCommand`, `PmOperator`, `PmConsole`,
  `LiteralTextGuardTest`, `01-commands.adoc`, the catalogue pair). Consider a split "strings and guard" / "refusals,
  Picocli and exit codes" when amending.
- Disposition: AMEND before emit (one open decision: the invented cleanup figures).

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-30-cli-texts-and-errors.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.

## Residue Fold 2026-11-10 (Amendment)

Source: `landings/PLAN-124.md` (L1-1, L1-2, layer 2 D3), re-read at `1cc0e10e`. This section supersedes the spec text
where they differ. Deliverable 2 grows by one numbered addition; the deliverable count stays 4.

R1. **Refusals of the answer verb render per the design, not as the English exception text** (added to deliverable 2)
 - What:
   - `OperatorRefusedException.java:33-36` builds `"<verb> refused by the runtime (HTTP n): <body>"`. `AnswerCommand` rethrows it
     at `:317,341,388,704,1333,1353,1368,1469,1498`. Route it through the deliverable 2 renderer like every other refusal.
   - The four decision refusals `conflict`, `already_answered`, `invalid_answer` and `dependency_unanswered` are not errors of
     the console (`14-transcripts-decisions.adoc:190`, `04-console.adoc:138`): print the decision as it stands, with the
     text of `decision.refused.*` (`decisions.en.properties:38-39` has `invalid_answer` and `dependency_unanswered`; add
     `conflict` and `already_answered`, en and de). `invalid_answer` and `dependency_unanswered` keep an interactive decision
     at its prompt. The exit code of a non-interactive refusal: see OPEN DECISION below.
   - `AnswerRefusalTest` today pins the English literal; rewrite it per code and language.
   - `EpicCommand.NextCommand` (`EpicCommand.java:406-410,423`: option descriptions and `"✓ Next plan for %s started"`) is in
     deliverable 1's `EpicCommand` literal sweep: name these sites there. The behaviour of `epic next` is not here (PLAN-127).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - `AnswerRefusalTest` asserts, per refusal code and per language, the printed decision text and the exit code, and fails on
     a revert of the renderer path (evidence order: `test-first`)
   - `git grep -n "refused by the runtime" -- pm-operator/src/main` is empty (evidence order: `none`)

OPEN DECISION (operator): exit code of a decision refusal printed "as the decision stands" (operator decision 2026-11-10, epic.md Decisions, item 4).

Sequencing: PLAN-127 (the other answer-verb residue) runs BEFORE this plan, so the deliverable 1 guard inversion also
scans PLAN-127's new texts.

Scope-bloat guard: 4 deliverables, but D1 (guard inversion, ~60 literals) and D2 (renderer, Picocli handler, local 404 sites, R1)
are both wide. The earlier suggestion stands: split into "strings and guard" and "refusals, Picocli and exit codes".

## Operator Decisions 2026-11-10 (Amendment)

Recorded in the epic's decision log (AskUserQuestion, 2026-11-10). They settle the open points above and supersede any text here that differs.

- **Exit code of a decision refusal (decision 4):** exit 1 (the refusal row of the exit table), with the decision printed as it stands.
