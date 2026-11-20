# PLAN-174: Terminal: What PLAN-168 Left (the Tests That Can Fail, the Last Failures on stdout, the Guard, the Captures)

epic: ui-improvements
workstream: WS-05

> Staged plan spec, one shippable unit of work, ready for hand-off on the `plan-marshall` lane (medium: six deliverables, about 35 files). Staged on the operator's decision of 2026-11-20 (PLAN-168 / #511: merge after the rebase and the answers, then stage D4, D6's captures and the minors as a residue plan) from `landings/PLAN-168.md` (the final landing section: the per-clause table at `ad4d5960`, the eight findings CodeRabbit refused to mark addressed, the residue list). This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

PLAN-168 (#511, `ad4d5960`) landed with `deliverables_done=2` of 6. It made a refusal render as itself, bounded and in the operator's language, and moved the eight named one-shot sites to stderr. It did not make the tests of PLAN-30 able to fail (D4), did not produce the cleanup captures (D6), and left behind what its own review named: one-shot failures that still print to stdout with their cause dropped, `HTTP {status}` in the enrolment texts, a refusal parser that prints a JSON-wrapped stack trace and discards a plain `Error:` sentence, a literal guard that passes single-word literals and several receivers, locale-less catalogue calls that print English to a German operator, and three design lines that contradict the code. This plan finishes that work, each part with a test that fails before it, so a refusal, an exit code and a screen text of the terminal client are pinned by tests that can fail.

## Deliverables

Read first:
- `landings/PLAN-168.md` of this epic, section "Final landing" (the per-clause table, the eight contested findings, the changes the landing does not name) and `landings/PLAN-30.md` (L2-1 to L2-5, L3-9 to L3-20).
- `plans/PLAN-168-cli-refusal-and-exit-residue.md` (the operator's choices stay: A2, the texts name no audit id; B1, the code wins for `epic next`; the stream rule: a verb run as a command prints its refusal to stderr, the interactive console and the entry flow keep their one stream).
- Every line below marked OBSERVED was read at `ad4d5960` by the orchestrator or the analysis leaf; those only the leaf read are HYPOTHESIS in the Claim Labels and are re-read at outline.

1. **`ExitCodesTest` checks every registered verb and can fail**
   - What: `ExitCodesTest` builds `RANDOM_ID` at class load (`:54`); success covers `-h`, `-V` and `status` (`:176-201`); the refusal table (`:122-161`) has 24 rows with one canned 500 and asserts the exit code only. It checks success, refusal and usage of EVERY registered verb (the usage test at `:109-115` already runs every verb), has a refusal row for each verb that has none today (`web`, `runtime`, `drain`, `credentials`, `recipe`, `enrol-harness`, `accept-edit`, `needs`), asserts the three rendered lines and the stream (a refusal on `err`, nothing on `out`) so the renderer path is told from `PmOperator`'s generic handler, asserts exit 130 (interrupt, `StartPrompt.java:37`), 3 (stopped at an unasked choice) and 4 (a security refusal) by their own cause, and builds `RANDOM_ID` per test. A verb registered without a case fails the suite.
   - Persona: plan-marshall:persona-module-tester
   - Skills: pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-dev-java:java-core
   - Done when:
     - `ExitCodesTest` fails when a verb is registered with no refusal row: the lane registers a verb in the test's own registry once and records the failing run (evidence order: `mutation-after`).
     - Each new refusal row fails when the verb's refusal handling is reverted to the generic handler (one row mutated once, recorded; evidence order: `mutation-after`); the exit 130, 3 and 4 tests fail when their cause is changed (evidence order: `mutation-after`).
     - `RANDOM_ID` is not a class-level constant: `git grep -n "RANDOM_ID =" -- pm-operator/src/test` names no static field (evidence order: `none`).
2. **The other tests of PLAN-30 D4 and D8**
   - What: `MockRuntimeEngineTest.java:2605-2625` asserts the credential path `/api/v1/credentials/github`, not a path with a decision segment (PLAN-30 D9: the old `carriesDecisionBody` guard skipped it): the test uses a path that carries a decision segment and fails against the old guard. `OnboardingScenarioTest.java:654-669` covers the ready-provider case only: an unready provider is not recorded as verified. `PicocliLanguageTest.java:37` has no `@EnableGeneratorController` and no generated input. The pin "a reminder offers no `never`" (PLAN-30 D8) is a test that the offer list of the reminder prompt excludes `never` (only the refusal test `ConfigCommandTest.java:1036` exists today).
   - Persona: plan-marshall:persona-module-tester
   - Skills: pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
   - Done when:
     - Each of the four tests fails once with the production behaviour it covers mutated (the old guard; an unready provider recorded as verified; the offer list with `never`; the picocli language switched), recorded (evidence order: `mutation-after`).
     - `PicocliLanguageTest` carries `@EnableGeneratorController` and draws its incidental input from `Generators` (evidence order: `none`).
3. **Every one-shot failure goes to stderr with its cause**
   - What: these one-shot sites still print a failure to `out` and return 1: `ClientCommand.java:86` (`client status`), `ConfigCommand.java:147-151` (the `config` read), `:820-822`, `:1360-1363` (consents withdraw), `:1501-1504` (documents update), `PlanCommand.java:1617-1620` (`control.settings.refused`), `ProjectCommand.java:350-354`, `AnswerCommand.java:167-169`, `ReportsCommand.java:217-219` (the stream rule of PLAN-168 and `03-dialogues.adoc:269`). The cause-less family (`operator.settings.list_failed=Unable to load settings.`, `operator.config.update_failed=Failed to update {aspect}.`, `operator.client.failed`, and the other `Failed to …` texts that lost their status) shows the refusal's cause through the renderer. `enrolment.{enrol,unenrol,accept,harness}.failed` (`enrolment.en.properties:7,12,29,48` and the German twins) no longer print `HTTP {status}`; `ProjectCommand.java:350` prints through the renderer. `EpicCommand.java:122-125` maps any 404 to `SCOPE_NOT_FOUND` while `PlanCommand.java:185-187,212-215` classify the body: one classification for `epic <id>`, `plan <id>` and `epic decompose`. `ClientCommand.java:126-127` substitutes the wire token into `operator.client.action_failed`, which prints "Der Client konnte nicht install werden." (`operator.de.properties:796`): the action gets its own text in `en` and `de`.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
   - Done when:
     - A test per site fails at `ad4d5960` and passes: a refused runtime read through each verb above prints the three-line refusal on `err` and nothing on `out`, and exits 1 (evidence order: `test-first`).
     - `git grep -n "HTTP {" -- pm-api/src/main/resources/messages` names no key a command prints (evidence order: `none`).
     - A test fails at `ad4d5960` and passes: an empty-body 404 for `epic <id>` and for `plan <id>` renders the same refusal (evidence order: `test-first`); a German `client install` failure prints a grammatical sentence (evidence order: `test-first`).
4. **The refusal parser prints no stack trace and discards no sentence**
   - What: `OperatorRefusedException.java:235` exempts every parsed JSON object from the trace check and `:255` does not cap the message, so a JSON body `{"error":"proxy_fault","message":"java.lang.IllegalStateException: boom\n\tat …"}` prints the trace on one line (CodeRabbit's finding, contested). `containsStackTrace` (`:277-289`) treats any line that contains `Error:` or `Exception:` as a trace, so a plain 5xx body "Error: select a configured provider." becomes UNREADABLE. The JSON message and every body argument are capped like a plain body; a stack trace is recognised by its frames (`\tat ` or `    at `), not by a word, in plain and in JSON bodies; a plain sentence that contains `Error:` is printed. `OperatorRefusedException(String,int)` (`:148`) is called only from tests and goes, and `getMessage()` formats its text through the operator's locale (`:197,200,203` use `format(null, …)`, English).
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
   - Done when:
     - Tests fail at `ad4d5960` and pass: a JSON-wrapped trace is not printed; a plain `Error: …` sentence is printed; a JSON message and an argument longer than the cap are cut (evidence order: `test-first`).
     - The three tests fail once with their production line reverted, recorded (evidence order: `mutation-after`).
5. **The literal guard sees what it was built to see, and no English reaches a German screen**
   - What: `LiteralTextGuardTest` has blind spots: `RENDER_CALL` (`:76-78`) is a fixed receiver list (`termOut.println(` is missed), `withoutCatalogueKeys` (`:183-193`) blanks the next literal even when the key is a variable, `matchingParen` (`:195-209`) counts parentheses inside string literals (`out.println("Nothing is due.)")` is missed), a single word with no space or `.!?:;` is not user-facing (`:251-253`, asserted by `:114`: `config`), a literal assigned to a variable and text passed to `new OperatorRefusedException("…")` are never scanned, and the helper list (`:49-73`) is hand-kept. The guard scans every receiver of a print, parses string literals before it counts parentheses, treats a key as a key only when it is a literal, scans variables assigned from a literal and exception messages, and derives its file list. English that escapes it today is localised: `PlanCommand.java:1609,1614` ("the value does not fit", "it is not one of …"), `ProjectsCommand.java:259,262` ("All projects", "by name"), `ReportsCommand.java:111,113` ("This machine"). The locale-less catalogue calls take the locale: `MessageCatalogue.get("…")` at `PlanCommand.java:669,673` (the depth converter) and the fifteen `format(key, …)` calls (`PlanCommand.java:1032,1469,1472,1560,1570,1577,1579,1617,1624,1629`, `ReportsCommand.java:100,103,173,271,321`: no locale means English). `operator.cleanup.durations.minimum.remind` says "At least {min} days." and prints "At least 1 days." (`operator.en.properties:767`, asserted by `ConfigCommandTest.java:1043`): the plural is by the catalogue. `operator.de.properties:453` is an infinitive against the English past tense.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
   - Done when:
     - A guard test per blind spot (a `termOut` receiver, a parenthesis inside a literal, a variable key, a single-word label, a literal in a variable, an exception message) fails at `ad4d5960` and passes (evidence order: `test-first`).
     - `git grep -n 'MessageCatalogue.get("' -- pm-operator/src/main` and `git grep -n "MessageCatalogue.format(\"" -- pm-operator/src/main` name no call without a locale on a screen (evidence order: `none`).
     - A test fails at `ad4d5960` and passes: `plan report` and `reports` in `de` contain none of the English strings above; "At least 1 day." is singular (evidence order: `test-first`).
6. **The design lines match the code, and the captures show what PLAN-30 D8 promised**
   - What: `03-dialogues.adoc:269` says every one-shot refusal goes to stderr (true after deliverable 3 only: the lane re-reads it then); `19-actions.adoc:190` contradicts itself ("`raise_scope: false`" and "`raise_scope` is sent only on the operator's answer"); `work-actions.adoc:222` says the prompt appears with no launchable plan while the code asks only when a scope-blocked candidate exists (`WorkActionDialogue.java:338-347`, `nextScopeBlocked`); `ApiErrorCode.java:155` still says "with an audit id". The sentence in `19-actions.adoc` that "the runtime lists a row as launchable only when the scope has room" was a HYPOTHESIS nobody confirmed in `../pm-mcp-core`: it is confirmed there (read-only) or changed to say it is the client's reading; the same for the body field names `project_id` and `part` and for the 404 body of an unknown plan or epic. The capture set (`ScreenCaptureIT`, `pm-operator/src/test/resources/screen-captures/`) contains the cleanup choice bar and the delete confirmation, both sizes (80x24 and 120x30), both languages; the capture OUTPUTS are committed (PLAN-168 left them untracked in the main checkout's `.plan/temp/screens-PLAN-168/`); a capture file written outside the worktree is a finding.
   - Persona: plan-marshall:persona-documenter
   - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation
   - Done when:
     - A reader of the three documents finds no line the code contradicts, checked by the lane against the table of `landings/PLAN-168.md` (evidence order: `none`).
     - The captures are present, committed and read: the choice bar and the delete confirmation in `en` and `de` at 80x24 and 120x30; `ScreenCaptureIT` runs them (evidence order: `none`).
     - The three engine facts are named in the landing message as confirmed (with the `pm-mcp-core` row) or as the client's reading (evidence order: `none`).

Not in this plan, named so they are not lost (each stays in `epic.md`): the picocli option and subcommand descriptions in German (about 370 literals, PLAN-30 L3-12), localising every `ParameterException`, the `Display.gate` pass-through, the German infinitive of the leave line beyond `:453`, the web side of the shared `error.internal_fault` pack.

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`): `./mvnw -pl pm-operator verify` and `-Pintegration-tests` (separate invocations), the `pm-api` and `pm-mock-runtime` builds, and the quality gate `./mvnw verify -Ppre-commit` (every rewrite reviewed; a file this plan does not own is restored from `origin/main`).
- No new dependency or plugin. Texts come from the catalogue in `en` and `de`; the expected text of a contract stays literal in a test. Java rules of `CLAUDE.md`: JUnit 5, input from `Generators` with `@EnableGeneratorController`, no Mockito, no generic `Exception`, no `System.out`/`System.err` except where a CLI writes its own output.
- **Sonar is read through the repository's own provider, never through the SonarQube MCP** (which cannot see this project): `plan-marshall:workflow-integration-sonar:sonar read_open_issues --project plan-marshall_pm-mcp-clients --pr N` for the open count, `sonar_rest gate-status` for the quality gate; ZERO open new-code issues and zero hotspots at the head, `unknown` for an undecidable read, never `0`; a dismissal only on the operator's selection. This is a merge gate.
- The order of each Done-when is the order the evidence has: a `none` Done-when is never described as failing, and "fail-first" is used only for `test-first` and `mutation-after` rows.
- **The merge rule:**
  - At least one CodeRabbit review of the head that is merged (not only of an earlier head), with its findings handled. If CodeRabbit refuses on quota or pauses the review, stop and ask the operator.
  - Every review thread replied and resolved, and every review body item answered ON THE PULL REQUEST, one reply per finding, read from GitHub's thread state; **a finding CodeRabbit re-verified and refused to mark addressed is not handled by a reply "handled in …": it is fixed or answered with a reason after its rebuttal**.
  - The run merges the pull request itself once these gates hold (`CLAUDE.md`, Git Workflow). Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`. The pull request body carries no generator footer.
- The landing verdict is given per Done-when clause, never per deliverable, and the landing message reports `deliverables_done` as the number of deliverables whose every Done-when clause is met; a number the lane did not recount is `unknown`; a changed exit code, stream or printed text is named in the landing message.
- Work only in the session's own worktree; capture outputs are written there and committed, never under the main checkout. Files are read and edited with the file tools, one command per call.
- Pull request size: about 35 files; if the changed set passes 100 files, stop and ask the operator to split (CodeRabbit refuses above 100 files). If the review pauses on a size or quota refusal, stop and ask.
- The mailbox read refuses a plan id with a leading digit (`invalid_target_plan`): report it, never read it as no mail.

## Claim Labels

- OBSERVED: `ExitCodesTest.java:54` `private static final String RANDOM_ID = Generators.letterStrings(4, 10).next();`, the per-verb usage test at `:109-115` (`unknownOptionExitsTwo`, every registered verb must exit 2) — read at `ad4d5960`
- OBSERVED: `MessageCatalogue.get("…")` without a locale at `PlanCommand.java:669` and `:673` — read by `git grep -n 'MessageCatalogue.get("' ad4d5960 -- pm-operator/src/main`
- OBSERVED: `ClientCommand.java:86` prints `operator.client.failed` to `out` and sets the exit code; `:126-127` passes `Map.of("action", action)`; `operator.de.properties:796` is `Der Client konnte nicht {action} werden.` — read at `ad4d5960`
- OBSERVED: `OperatorRefusedException.java:233-237` applies the markup and trace heuristics only when `!extracted.parsedObject()`; `containsStackTrace` (`:277-289`) returns true for any line that contains `Error:` or `Exception:` — read at `ad4d5960`
- OBSERVED: `WorkActionDialogue.java:336-350` asks to raise the scope only when `scopeFull || (running == null && launchable == null)` and a candidate (`launchable` or `nextScopeBlocked`) exists, else sends `raise_scope: false` — read at `ad4d5960`
- OBSERVED: `PlanCommand.java:1609,1614` ("the value does not fit", "it is not one of …"), `ProjectsCommand.java:259,262` ("All projects", "by name"), `ReportsCommand.java:111,113,217` — read at `ad4d5960`
- HYPOTHESIS: the sites of deliverable 3 beyond `ClientCommand:86` print the refusal to `out` with the cause dropped: `ConfigCommand.java:147-151,820-822,1360-1363,1501-1504`, `PlanCommand.java:1617-1620`, `ProjectCommand.java:350-354`, `AnswerCommand.java:167-169`, `ReportsCommand.java:217-219` — read by the analysis leaf (and `ConfigCommand:147-151` and `:1360-1363` re-read by the orchestrator at `bd60be5e`); confirm/refute at outline by reading each
- HYPOTHESIS: the fifteen locale-less `format` calls are exactly `PlanCommand:1032,1469,1472,1560,1570,1577,1579,1617,1624,1629` and `ReportsCommand:100,103,173,271,321`, and a call without a locale defaults to English (`MessageCatalogue.java:67,196-199,229-231`) — the leaf scripted the scan; confirm at outline by the same search
- HYPOTHESIS: `LiteralTextGuardTest` blind spots at `:76-78,183-193,195-209,251-253` and the hand-kept helper list at `:49-73` — read by the leaf
- HYPOTHESIS: `ExitCodesTest` has no refusal row for `web`, `runtime`, `drain`, `credentials`, `recipe`, `enrol-harness`, `accept-edit`, `needs`, no exit 130 test, and the 24 rows assert the exit code only — read by the leaf at `:122-161`
- HYPOTHESIS: `MockRuntimeEngineTest.java:2605-2625`, `OnboardingScenarioTest.java:654-669` (ready-provider case only), `PicocliLanguageTest.java:37` (no controller) — read by the leaf; PLAN-30's landing read the same at `2efcaa40`
- HYPOTHESIS: the capture outputs were left untracked under the main checkout's `.plan/temp/screens-PLAN-168/` and the 120x30 cleanup capture shows the list screen only — read by the leaf; confirm at outline
- HYPOTHESIS: the runtime lists a row as launchable only when the scope has room; the refusal body carries `project_id` and `part`; an unknown plan or epic answers a JSON 404 with `scope_not_found` — nobody read `../pm-mcp-core` for these (PLAN-168 carried the first as a HYPOTHESIS, then wrote it into `19-actions.adoc` as a fact); confirm/refute at `../pm-mcp-core/doc/specification/` (verify-at-outline)
- HYPOTHESIS: an empty-body 404 from the real daemon prints "the runtime does not know the request" for `epic decompose`, `plan` and `plan report` since PLAN-168 — depends on the daemon's real 404 body
- Verify-first clause: before each deliverable, re-read the named lines at the then-current HEAD; the staged `PLAN-42` edits the same command classes (see Dependencies). A refuted claim re-scopes the deliverable and is reported.

## Expected Surface

- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ClientCommand.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ConfigCommand.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ConfigCleanupCommand.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/PlanCommand.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ProjectCommand.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/AnswerCommand.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ReportsCommand.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ProjectsCommand.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/EpicCommand.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/OperatorRefusedException.java`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/RefusalRenderer.java`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/resource/ApiErrorCode.java`
- OBSERVED: `pm-api/src/main/resources/messages/operator.en.properties`
- OBSERVED: `pm-api/src/main/resources/messages/operator.de.properties`
- OBSERVED: `pm-api/src/main/resources/messages/enrolment.en.properties`
- OBSERVED: `pm-api/src/main/resources/messages/enrolment.de.properties`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ExitCodesTest.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/PicocliLanguageTest.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/LiteralTextGuardTest.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/OperatorRefusedExceptionTest.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/RefusalRendererTest.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/RefusalStreamTest.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ConfigCommandTest.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ClientCommandTest.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/PlanCommandTest.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/EpicCommandTest.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ProjectCommandTest.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ReportsCommandTest.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/PmOperatorTest.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/ScreenCaptureIT.java`
- OBSERVED: `pm-operator/src/test/resources/screen-captures/PLAN-168.txt`
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/runtime/MockRuntimeEngineTest.java`
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/OnboardingScenarioTest.java`
- OBSERVED: `doc/design/terminal-client/03-dialogues.adoc`
- OBSERVED: `doc/design/terminal-client/19-actions.adoc`
- OBSERVED: `doc/design/shared/work-actions.adoc`
- HYPOTHESIS: `pm-operator/src/test/java/de/planmarshall/operator/OperatorCatalogueHygieneTest.java` (the "no `HTTP {`" key scan: it covers the `operator` area only and is widened to `enrolment`; verify-at-outline)
- HYPOTHESIS: `pm-operator/src/main/java/de/planmarshall/operator/StartApi.java` (a fallback `"HTTP " + status` at about `:374`; verify-at-outline)
- HYPOTHESIS: `pm-operator/src/test/resources/screen-captures/PLAN-174.txt` (the new capture file; the path is the lane's)

## Dependencies and Sequencing

- Depends on: `PLAN-168` (shipped, #511 `ad4d5960`) and `PLAN-171` (shipped, #510 `76315c63`: `ControlCommandTest`, `PlanCommandTest`, `ConfigCommandTest` already carry its generated ids).
- Overlaps with (read by `corpus cross-check` at staging, 2026-11-20; shipped, parked and superseded plans are not in the disjointness population): **no running plan** (PLAN-172 is web), so it is emittable. Staged, all held behind it or elsewhere: `PLAN-42` (11 files, including `19-actions.adoc` and the operator packs), `PLAN-44` (7: `03-dialogues.adoc`, `AnswerCommand`), `PLAN-48` (6: `03-dialogues.adoc`, `EpicCommand`), `PLAN-138` (6), `PLAN-139` (2), `PLAN-140` (`AnswerCommand`, `StartApi`), `PLAN-154` (`ReportsCommandTest`), `PLAN-156` (`work-actions.adoc`): whichever lands second rebases and keeps both sides.
- Adjacent to: **the staged `PLAN-42` (verb modes and help, now with deliverables 4 and 5) cites `PlanCommand`, `EpicCommand`, `ReportsCommand`, `PmOperator`, `ConfigCommand`, `WebCommand` and the operator packs, which #511 changed: its line citations are stale**; this plan lands before PLAN-42 is emitted, as PLAN-168 was to land before it, and PLAN-42 re-reads at outline. `PLAN-138`, `PLAN-139`, `PLAN-140`, `PLAN-44` and `PLAN-48` name the same command classes and follow.

## Hand-Off Command

Lane: `plan-marshall` (medium: six deliverables, about 35 files; the operator's size rule of 2026-11-20).

```text
/plan-marshall task="implement .plan/orchestrator/ui-improvements/plans/PLAN-174-cli-refusal-residue-second.md"
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests, and reads `../pm-mcp-core` read-only. It creates and edits NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports its outcome through its PR and its inbox message. The inbox exception's qualifiers and the sole sanctioned write mechanism are stated in `persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
