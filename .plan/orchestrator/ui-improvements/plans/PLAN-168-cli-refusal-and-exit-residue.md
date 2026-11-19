# PLAN-168: Terminal: The Refusal Path Told Straight, Exit Codes That Can Fail, and the Rest of PLAN-30's Residue

epic: ui-improvements
workstream: WS-05

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Staged on the operator's word of 2026-11-19 ("PLAN-30 stays shipped, residue another plan") from `landings/PLAN-30.md` (the three-layer analysis of #438, `2efcaa40`). This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

PLAN-30 (#438) landed with three of nine deliverables met and seven major defects in its refusal path. This plan finishes what that landing left: a refused request is rendered the same way on every verb and exits 1 (`runtime upgrade --check` and `--withdraw` exit 0 today); a local condition is never shown as an unknown runtime code; the fix line of a refusal that names a project id shows the id; a proxy page or stack trace never becomes the message; the decision dialogue names the right part and shows no internal state id. The tests that were meant to prove this are made able to fail: the exit-code table is checked for success, refusal and usage of every verb with the renderer path told apart from the generic failure handler, and the display residue (no-locale callers, the English `plan report`) is closed with a guard that sees it. The design lines the landing called "matching" are made to match. Behaviour that works today does not move.

## Deliverables

Read first:
- `landings/PLAN-30.md` of this epic (layer 3 table L3-1 to L3-20, layer 2 L2-1 to L2-5, layer 1 L1-1 to L1-4); each point below names its row. The layer-3 points marked "re-read" were read at `2efcaa40` by the orchestrator; the others are the analysis leaf's reading and are re-checked at outline.
- `RefusalRenderer.java`, `OperatorRefusedException.java`, `ErrorTexts` and the `error.*` pack, `PmOperator.java` (the exit handling), the design `doc/design/terminal-client/01-commands.adoc#exit-codes`.

1. **One refusal path: local, argumented, bounded and with its audit id**
 - What: (a) An `OperatorRefusedException` with no code and a local text (`ConfigAspectRun:190`, `AspectRunSource:232,238`, `CleanupCommand:568,586`, `OperatorGate:123`) is rendered as what it is, not as "The runtime refused: ..." with an unknown-code cause and "Update the client" (L3-2; `RefusalRenderer.java:81-90`). (b) `render(out, locale, exception)` passes the arguments the code needs: `enrolment_tampered` prints the project id, not `{project_id}`; the exception carries the arguments the body names (L3-3; `RefusalRenderer.java:125`, `error.en.properties:123`). (c) A body that is no JSON object is never the message in full: no exception class, no HTML, a stated length cap; a 404 that names no code is `scope_not_found` only for a route the runtime owns, and an older runtime's missing route is said as such (L3-4; `OperatorRefusedException.java:129-138`). (d) `audit_id` of an `internal_fault` body is read and printed, as `03-dialogues.adoc` promises, or the document is corrected, by the operator's choice at outline (L3-5, L1-2). (e) The "The runtime refused: " prefix is a catalogue key, not a literal in two files (L3-20), and the dead `ore.exitCode()==4` branch is removed (L3-10).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:java-null-safety, pm-dev-java:javadoc, pm-dev-java-cui:cui-logging
 - Done when:
   - A test per case (a), (b), (c) fails at `2efcaa40` and passes: a local refusal renders no "runtime refused" prefix and no unknown-code cause; `enrolment_tampered` through `render(out, locale, exception)` contains the project id and no `{`; a 502 HTML body and a stack-trace body render neither tag nor class name nor more than the cap, and a code-less 404 on an unowned route is not "This entry does not exist" (evidence order: `test-first`).
   - A test for (d) fails at `2efcaa40`: the audit id of a refusal body is on the screen (or the design line no longer promises it), in `en` and `de` (evidence order: `test-first`).
   - `git grep -n "The runtime refused" -- pm-operator/src/main pm-api/src/main/java` names no Java literal (evidence order: `none`).
2. **Every runtime verb exits through the same refusal handling**
 - What: `runtime upgrade --check`, `--withdraw` and the release-notes read render the refusal through `RefusalRenderer` and exit 1; no "HTTP n" is printed (L3-1; `RuntimeCommand.java:362-368,376,388-392`). The machine-settings read prints the refusal and not "Unable to load settings: HTTP {status}" (L3-13; `ConfigCommand.java:1291-1296`); the duplicate key `operator.config.settings_failed` is removed. A runtime status without a `pid` prints a line and exits 0, not `runtime: null` and exit 1 (L3-16; `RuntimeCommand.java:135`). No `HTTP {status}` key remains in `operator.en.properties` that a screen can print.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - Tests fail at `2efcaa40` and pass: a 500 on `upgrade --check`, `upgrade --withdraw` and release notes exits 1 and prints the three-line refusal and no "HTTP"; a 500 on the machine-settings read prints the refusal; a status without `pid` exits 0 (evidence order: `test-first`).
   - `git grep -n "HTTP {" -- pm-api/src/main/resources/messages/operator.en.properties` names no key a command prints (evidence order: `none`).
3. **The decision and epic dialogue lines say what is true**
 - What: `dependency_unanswered` names the part the runtime reports, not "part 1" for every part (L3-6; `AnswerCommand.java:856,1746`; the runtime's body carries the part or the dialogue derives it, decided at outline, and the unnamed case has its own text); `invalid_answer` with no message does not end in a colon and an empty reason (L3-6). The leave line shows no internal state id: "Left epic.raise-scope." becomes a line the design names (L3-7; `WorkActionDialogue.java:342`; `EpicCommandTest` and `ControlDialoguesTest` stop pinning the raw id). `config cleanup --remind never` is refused with the text that says "never" is not offered for a reminder (L3-8; `ConfigCleanupCommand.java:233`).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - Tests fail at `2efcaa40` and pass: a refusal for part 3 prints part 3 (or the unnamed text), an `invalid_answer` without a message ends in no colon, the leave line contains no `epic.` token, `--remind never` prints the remind-specific text (evidence order: `test-first`).
   - The terminal check of `answer` and `epic next` in `en` and `de` is read back (evidence order: `none`).
4. **The tests of PLAN-30 made able to fail**
 - What: `ExitCodesTest` checks success, refusal and usage of EVERY registered verb (success today: one verb, `ExitCodesTest.java:179-201`), exit 130 (interrupt) and 3 and 4 by their own cause; a refusal case asserts the rendered three lines, so the renderer path is told from `PmOperator`'s generic failure handler (L2-1); `RANDOM_ID` is built per test, not at class load. The D9 credential-path act-body assertion uses a route that carries a decision segment, so the old guard would have recorded it (L2-4; `MockRuntimeEngineTest.java:2622`). A test pins that an unready provider is not recorded as verified (PLAN-30 D7; `OnboardingScenarioTest.java:654`), and one pins that a reminder offers no `never` (D8). `PicocliLanguageTest` carries `@EnableGeneratorController` and generated input (L2-5).
 - Persona: plan-marshall:persona-module-tester
 - Skills: pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-dev-java:java-core
 - Done when:
   - Each new or changed assertion fails when its production line is reverted (the lane reverts it once and records the failing run; evidence order: `mutation-after`).
   - `ExitCodesTest` fails when a registered verb is added with no case for success, refusal and usage (evidence order: `mutation-after`).
5. **The display residue of PLAN-30 D5 closed, and a guard that sees it**
 - What: every `MessageCatalogue.get(` without a locale on a screen is given the locale (`PlanCommand.java:619,623`, `EntryFlow.java:435`, `ReportsCommand.java:83`, `LessonSourceCommand.java:113,160`, `EnrolHarnessCommand.java:116-240`; the list is re-made by grep at outline); the printed `plan report` is in the operator's language (`PlanCommand.java` about 1044-1305: the English literals); the dead `operator.cleanup.*` keys that freeze invented figures are removed in both packs (L3-14). `LiteralTextGuardTest` gains `PlanCommand`, `EpicCommand`, `StatusCommand`, `RolesCommand`, `DecomposeCommand`, `ConfigCleanupCommand` and `DrainCommand` and sees a literal inside a ternary and `String.format` (L3 / L2 row D1: blind outside its list).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - A test per item fails at `2efcaa40`: `plan report` in `de` contains no English word outside the glossary; the guard fails on a literal in a ternary in a guarded file; no unreferenced key remains in either pack (evidence order: `test-first`).
   - `git grep -n "MessageCatalogue.get(" -- pm-operator/src/main` names no call without a locale on a screen (evidence order: `none`; the clause is meaningful because `get(locale, key)` and `get(key)` are told apart at outline, see L1-3).
6. **The design lines match the code, and the captures show what PLAN-30 D8 promised**
 - What: `03-dialogues.adoc:269` says what the code prints for `internal_fault` (decided with deliverable 1d); `shared/work-actions.adoc:222` and the new launch-without-asking behaviour agree (the document wins unless the operator says otherwise: **the lane stops and asks, which wins**, per PLAN-30's own rule "if the document is silent, stop and ask"); `26-cleanup.adoc`'s text table lists `operator.cleanup.choose.remind` as the pack has it. The capture set (`PLAN-168` in `ScreenCaptureIT`) contains the choice bar and the delete confirmation of `cleanup`, both sizes, both languages (PLAN-30 D8's capture clause).
 - Persona: plan-marshall:persona-documenter
 - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation
 - Done when:
   - A reader of each of the three documents finds no line the code contradicts, checked by the lane with the table of `landings/PLAN-30.md` L1-2 (evidence order: `none`).
   - The captures are present and read: the choice bar and the delete confirmation in `en` and `de` at 80x24 and 120x30 (evidence order: `none`).

Not in this plan, named so they are not lost (each stays a Watch or Open Defect in `epic.md`): the stream split of refusals (stdout for `plan`/`epic`, stderr elsewhere; the design says err: the operator decides which wins, L3-9), picocli option and subcommand descriptions in German (about 370 literals, L3-12), localising every `ParameterException` (L3-11), `Display.gate` pass-through and stale javadoc (L3-15), the `MessageCatalogue.get` overload swap risk (L3-17), duplicate-valued and unpluralised keys (L3-18), partial state unreported in `config cleanup` (L3-19), PLAN-30 D7's #415 thread replies and D8's #425 replies (posted on merged pull requests by the operator or a trivial change).

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`). At the head, `./mvnw verify -Ppre-commit` passes (every rewrite reviewed and committed), and so do `./mvnw -pl pm-operator verify -Pintegration-tests` and the `pm-api` and `pm-mock-runtime` builds. Run `-Pcoverage` and `-Pintegration-tests` as separate invocations.
- No new dependency or plugin. Texts come from the catalogue in `en` and `de`; the expected text of a contract stays literal in a test.
- Java: JUnit 5, input from `Generators` with `@EnableGeneratorController`, no Mockito; never catch or throw generic `Exception`/`RuntimeException`; no `System.out`/`System.err` except where a CLI writes its own output.
- Every behaviour change has a failing-first run recorded in the progress record (a log or a commit), and the order of each Done-when is the order the evidence has: a `none` Done-when is never described as failing. The word "fail-first" is used only for `test-first` and `mutation-after` rows.
- The quality gate rewrites `@CsvSource({"''"})` to `@ValueSource(strings = {"''"})`, which turns an empty answer into two apostrophes (epic Watch, owner `pm-mcp-parent`): a test that needs an empty answer avoids that form. Revert any file the gate reformats that this plan does not own.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.
- **The merge rule:**
  - At least one CodeRabbit review of some head, with its findings handled. If CodeRabbit refuses on quota, stop and ask the operator.
  - Every review thread replied and resolved, and every review body item (outside-diff, nitpick, additional comments) answered on the pull request, read from GitHub's thread state, never from a count kept in memory. Every reply answers its own finding.
  - The run merges the pull request itself once these gates hold (`CLAUDE.md`, Git Workflow). Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`. The pull request body carries no generator footer.
- ZERO Sonar findings: 0 open new-code issues and 0 hotspots, read at the pull request head after the push (`unknown` for a failed read, never `0`). A dismissal is made only on the operator's selection. This is a merge gate.
- The landing verdict is given per Done-when clause, never per deliverable, and the landing message reports `deliverables_done` as the number of deliverables whose every Done-when clause is met.
- Pull request size: if the changed set passes 100 files, stop and ask the operator to split (CodeRabbit refuses above 100 files). Expected: about 40 files.
- The mailbox read refuses a plan id with a leading digit (`invalid_target_plan`): report it, never read it as no mail.

## Claim Labels

- OBSERVED: `runtime upgrade --check` and `--withdraw` print a "failed: HTTP n" line and `return 0` when the runtime refuses — read at `pm-operator/src/main/java/de/planmarshall/operator/RuntimeCommand.java:376,388-392` (`runCheck`, `withdrawUpgrade`) at `2efcaa40`
- OBSERVED: a refusal with no code and a message is titled "The runtime refused: <message>" — read at `RefusalRenderer.java:81-90` (`titleLine`) at `2efcaa40`
- OBSERVED: `render(out, locale, exception)` passes `Map.of()` and no caller passes arguments, so `error.enrolment_tampered.fix` keeps `{project_id}` — read at `RefusalRenderer.java:125` and `pm-api/src/main/resources/messages/error.en.properties:123`
- OBSERVED: a body that does not start with `{` becomes the message in full, and a 404 without a code becomes `scope_not_found` — read at `OperatorRefusedException.java:129-138` (`parseRefusal`)
- OBSERVED: no `audit_id` or `auditId` occurs in `pm-operator/src/main` — read by `git grep -n "audit_id\|auditId" -- pm-operator/src/main` at `2efcaa40`
- OBSERVED: `dependency_unanswered` is formatted with `n` = 1 — read at `AnswerCommand.java:856` (`Map.of(REASON_KEY, ..., "n", 1)`)
- OBSERVED: the leave line formats the state id `epic.raise-scope` — read at `WorkActionDialogue.java:342`
- OBSERVED: `ExitCodesTest` success covers `-h`, `--help`, `-V`, `--version` and `status` only — read at `pm-operator/src/test/java/de/planmarshall/operator/ExitCodesTest.java:176-201`
- OBSERVED: `get(key)` without a locale is called at `PlanCommand.java:619,623` — read at `2efcaa40`
- OBSERVED: the D9 credential assertion asserts the path `/api/v1/credentials/github` — read at `pm-mock-runtime/src/test/java/de/planmarshall/mock/runtime/MockRuntimeEngineTest.java:2622`
- HYPOTHESIS: `--remind never` is refused with the base key `operator.cleanup.durations.minimum`, and the remind-specific key is used only in `askDays` — confirm/refute at `ConfigCleanupCommand.java:233,368-378` (verify-at-outline)
- HYPOTHESIS: the other no-locale callers are `EntryFlow.java:435`, `ReportsCommand.java:83`, `LessonSourceCommand.java:113,160` and `EnrolHarnessCommand.java:116-240` — confirm/refute by `git grep -n "MessageCatalogue.get(" -- pm-operator/src/main` (verify-at-outline)
- HYPOTHESIS: a runtime status with no `pid` throws a `NullPointerException` in `Map.of("pid", status.get("pid"))` — confirm/refute at `RuntimeCommand.java:135` (verify-at-outline)
- HYPOTHESIS: the dead cleanup keys (`operator.cleanup.footer`, `footer_sweep`, `kept_header`, `none`, `none_header`, `released`, `residue_header`, `residue.*`) are referenced nowhere in main or tests — confirm/refute by `git grep` over `pm-operator` and `pm-api` (verify-at-outline)
- HYPOTHESIS: the old `carriesDecisionBody` guard skipped the credential path the D9 test asserts — confirm/refute at `MockRuntimeEngine.java` of `2efcaa40^1` § `carriesDecisionBody` (verify-at-outline)
- HYPOTHESIS: the runtime's `dependency_unanswered` body names the part, or the dialogue can derive it — confirm/refute at `../pm-mcp-core/doc/specification/state-catalogue/` and `MockRuntimeEngine.java` § the answer route (verify-at-outline)
- HYPOTHESIS: `work-actions.adoc:222` still says an unknown running count "asks as well" — confirm/refute at `doc/design/shared/work-actions.adoc:222` (verify-at-outline)
- Verify-first clause: before each deliverable, re-read the named lines at the then-current HEAD; PLAN-79 (a rewrite of the operator tests) may be running and touches the same test files, see Dependencies. A refuted claim re-scopes the deliverable and is reported.

## Expected Surface

- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/RefusalRenderer.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/OperatorRefusedException.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/PmOperator.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/RuntimeCommand.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ConfigCommand.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ConfigCleanupCommand.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/CleanupCommand.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/AnswerCommand.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/WorkActionDialogue.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/PlanCommand.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/EntryFlow.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ReportsCommand.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/LessonSourceCommand.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/EnrolHarnessCommand.java`
- OBSERVED: `pm-api/src/main/resources/messages/operator.en.properties`
- OBSERVED: `pm-api/src/main/resources/messages/operator.de.properties`
- OBSERVED: `pm-api/src/main/resources/messages/error.en.properties`
- OBSERVED: `pm-api/src/main/resources/messages/error.de.properties`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/RefusalRendererTest.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/OperatorRefusedExceptionTest.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ExitCodesTest.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/AnswerRefusalTest.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/RolesCommandTest.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/EpicCommandTest.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ControlDialoguesTest.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/LiteralTextGuardTest.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/CleanupCommandTest.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ConfigCommandTest.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/CredentialsCommandTest.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/PlanCommandTest.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/PicocliLanguageTest.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/ScreenCaptureIT.java`
- OBSERVED: `pm-operator/src/test/resources/screen-captures/PLAN-30.txt`
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/runtime/MockRuntimeEngineTest.java`
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/OnboardingScenarioTest.java`
- OBSERVED: `doc/design/terminal-client/03-dialogues.adoc`
- OBSERVED: `doc/design/terminal-client/26-cleanup.adoc`
- OBSERVED: `doc/design/terminal-client/01-commands.adoc`
- OBSERVED: `doc/design/shared/work-actions.adoc`
- HYPOTHESIS: `pm-operator/src/main/java/de/planmarshall/operator/OperatorGate.java` (a caller of a local refusal, `OperatorGate.java:123`; sweep: exists at HEAD, verify-at-outline)
- HYPOTHESIS: `pm-operator/src/main/java/de/planmarshall/operator/dialogue/AspectRunSource.java` (a caller of a local refusal, `:232,238`; verify-at-outline)
- HYPOTHESIS: `pm-operator/src/main/java/de/planmarshall/operator/ConfigAspectRun.java` (a caller of a local refusal, `:190`; verify-at-outline)

## Dependencies and Sequencing

- Depends on: `PLAN-30` (shipped, #438 `2efcaa40`); the operator's choice at outline for deliverable 1d (print the audit id or correct the design) and for deliverable 6 (which of `work-actions.adoc` and the new behaviour wins).
- Overlaps with:
  - `PLAN-79` (running when this was staged): `pm-operator/src/test/java/` (directory claim). The test sweep rewrites the same test classes; start after it, or in a different half, and say so in the progress record.
  - `PLAN-165` (running when this was staged): `CleanupCommand.java` and `CleanupCommandTest.java`; deliverable 5 and the guard touch the same file: start after it lands or rebase.
  - `PLAN-42`, `PLAN-44`, `PLAN-48`, `PLAN-138`, `PLAN-139`, `PLAN-140` (staged): the same command classes; this plan lands before any of them is emitted, and its renderer and exit handling are their base.
- Adjacent to: the stream split of refusals (L3-9), left to the operator; the picocli descriptions in German, left unchanged.

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-168-cli-refusal-and-exit-residue.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports its outcome through its PR and its inbox message. The inbox exception's qualifiers and the sole sanctioned write mechanism are stated in `persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
