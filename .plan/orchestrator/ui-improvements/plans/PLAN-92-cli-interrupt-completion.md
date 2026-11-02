# PLAN-92: Terminal: Ctrl+C Reaches Every Dialogue and Every Handler Leaves with 130

epic: ui-improvements
workstream: WS-04

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.
> Staged 2026-11-02 from the landing analysis of PLAN-91 (`landings/PLAN-91.md`). One pull request on purpose: each pull request costs a
> CodeRabbit review (quota-limited), so the open aspects of one area are collected here. Keep the pull request under 100 files
> (CodeRabbit refuses more); if it would exceed that, split it by deliverable and say so in the progress record.

## Objective

Finish what PLAN-91 left: the design says a verb run as one shell command exits with 3 or 130, and a real Ctrl+C may not reach a dialogue a full-screen view opened (unproven either way), and more handlers than the eight PLAN-91 changed still turn Ctrl+C into exit `0`, null or "no". Prove the first with a pseudo-terminal test that types the key, fix the cause if it is real, map the interrupt in the remaining handlers, and correct the documents and tests that PLAN-91 left behind.

## Deliverables

1. **A typed Ctrl+C reaches a dialogue a view opened, on a direct verb**
   - What: `StartPrompt.reader()` (`StartPrompt.java:211`) calls `context.createTerminal()`, and `PlansCommand`, `PlanCommand`, `SystemCommand`, `PmOperator` and `ProjectsCommand` do not lend the view's terminal (`useTerminal`; only `PmConsole` does). A dialogue a view opens therefore reads through a second system terminal that is never closed. PLAN-91's lane reports that a Ctrl+C typed into such a dialogue never arrives; the committed evidence never types it (the hold capture uses `q\nq`) and the unit tests share one terminal (`InterruptExitCodeTest:88`, `ControlFixture:285-289`). The mechanism is unproven (under raw mode that keeps ISIG a Ctrl+C normally arrives as SIGINT). The dialogues concerned are every one opened through `runDialogue`: work action, hold, review, the runtime acts (stop, restart, upgrade, check), project add and setup.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java:junit-integration, pm-dev-java-cui:cui-testing
   - Done when:
     - A pseudo-terminal test per dialogue kind (hold, work action, review, one runtime act) opens the dialogue from a view on a DIRECT verb (`pm-operator plans`, not the console), types Ctrl+C (`\c`) at its first prompt, and asserts exit `130` and no non-GET request; it is recorded failing or passing at the base with the log path (evidence order: `test-first`)
     - If it fails at the base: the view and its dialogues share one terminal (`useTerminal`/`clearTerminal` in the five view verbs, an `ownsTerminal` guard in the closers that still open their own — fourteen `try (var terminal = context.createTerminal())` sites at PLAN-91's merge: `WebCommand` x2, `GitSourceCommand`, `ConfigCommand` x5, `EnrolHarnessCommand`, `AnswerCommand`, `RolesCommand`, `LessonSourceCommand`, `AcceptEditCommand`, `OnboardCommand`; `CredentialsCommand:149` opens one without a closer) and the test passes (evidence order: `test-first`). If it passes at the base, the residue of PLAN-91 is refuted: say so, and the finding closes with the log
     - The hold capture of `91-cli-interrupt-and-transcripts.txt` is recaptured with `\c` instead of `q\nq`; terminal check (screen capture) at 120 × 30 and 80 × 24, `en_US.UTF-8` and `de_DE.UTF-8`, before and after (evidence order: `none`)
2. **Every handler a verb can run leaves with 130 on Ctrl+C**
   - What: handlers outside PLAN-91's eight still map the interrupt to 0, null or "no": `StartDialogue:125` (`start.entry.left`, exit 0; `StartPrompt.read` now throws the new marker and this catch swallows it), `PlanSettingsDialogue:77` (catches the parent `StartLeftException`), `WorkChatDialogue:75`, `ProjectSelector:299`, `AnswerCommand:222`, the confirmations in `CredentialsCommand:232`, `EnrolHarnessCommand:241` and `RolesCommand:388`, the `onboard --auto` confirmation (`UserInterruptException` out of `confirm()` reaches `PmOperator.execute` as exit 1), and `OverviewFullScreenApp.askYes:3197`. `01-commands.adoc:184` and `03-dialogues.adoc:255` say a verb run as one shell command exits with 3 or 130. Chat and start nested inside review and work action swallow the interrupt and return to the parent prompt: decide by the design whether that is right and say which.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:javadoc, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
   - Done when:
     - One test per handler named above sends the interrupt at its prompt and asserts exit `130` and no act, each failing before the change (evidence order: `test-first`). A handler where the design says the interrupt leaves back to the caller (the console, `EntryFlow`, a confirmation inside a view) is listed with the design sentence and keeps its behaviour
     - `03-dialogues.adoc`, `04-console.adoc` and `01-commands.adoc` state which dialogues map the interrupt, which end the view with 130 (every one that returns 130 through `runDialogue`, including the runtime acts and project add and setup — `04-console.adoc:197` names only action, hold and review), and which leave back; the cross-reference at `03-dialogues.adoc:44` is a link, not backticked text (evidence order: `none`)
3. **The interrupt tests prove what they claim**
   - What: PLAN-91's tests and CodeRabbit's open follow-ups of its last review (`0c92ada1`). Verb-level 130 tests exist for `plans` and `plan <id>` only; the view tests never assert the terminal is restored after the interrupt; `TuiScreenDriver.stop()` after `suspend()` sends `exit_ca_mode` twice; `InterruptDialogueIT.awaitPrompt` settles on 700 ms of quiet time; `ReadVerbExitCodesTest.refusedReads()` omits `plans` and `lesson check` although it claims every read verb; `InterruptExitCodeTest:81` keeps a literal token; the token-unit tests of `SystemViewConsistencyTest` do not tie a split to its model row and do not assert the summary; `ActionsFullScreenTest:317` is named "Ctrl+C" but raises the signal.
   - Persona: plan-marshall:persona-module-tester
   - Skills: pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
   - Done when:
     - Verb-level exit-`130` tests for `projects`, `system` and bare `pm` (the tui path); the interrupt tests assert `alternateScreenActive()` is false and the cursor and attributes are restored; `exit_ca_mode` is sent once, or the second send is shown harmless with the reason (evidence order: `mutation-after` — each test fails when the code it covers is reverted)
     - `InterruptDialogueIT` waits on the condition the code produces (a prompt on the screen), not on 700 ms of quiet; `refusedReads()` is derived from the command registrations; the token and the other test inputs come from `Generators`; the token splits are tied to their model rows and the summary is asserted; the "Ctrl+C" tests are renamed or the typed path is covered (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is fixed or dismissed with a stated reason (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md): `./mvnw -pl pm-operator -am verify -Pintegration-tests` passes at the head.
- No new dependency or plugin.
- CUI standards (`CLAUDE.md`): a test that takes input data draws it from `Generators` with `@EnableGeneratorController` (the expected value of a contract stays literal); a log call at INFO or above goes through a `LogRecord` of the module's `LogMessages` class with a `LogAsserts` test; `@NullMarked` on every package of `pm-operator`; JUnit 5 only, no Mockito, no Hamcrest.
- Java 25, Lombok, records; no generic `Exception`/`RuntimeException`.
- Every new operator text goes through `MessageCatalogue` into `operator.en.properties` and `operator.de.properties`.
- Ports of this plan: mock web port `20920`, app port `20921`. Other plans may run at the same time: never stop or reuse a process this worktree did not start.
- Terminal check of the rendered output through the screen capture harness of `cli-pty-dialogue-harness` (`ScreenCaptureIT`, command line in `doc/design/terminal-client/27-tui-testing.adoc`): write the captures to `pm-operator/src/test/resources/screen-captures/{plan}.txt`, run the set BEFORE the change and copy `pm-operator/target/screens/{plan}/` to `.plan/temp/terminal/{plan}/before/`, then AFTER to `.../after/`. Capture at 120 × 30 and 80 × 24 in `en_US.UTF-8` and `de_DE.UTF-8`.
- The design document is updated in the same pull request as the code (UX standing rule of the epic).
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`) — treat it as `sonar: configured`. An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason.
- Merge rule (the operator, 2026-11-02): at least one CodeRabbit review of some head with its findings handled; later heads need no review, re-trigger once and never wait. Handle the findings of EVERY CodeRabbit review that exists, a late one included, and read the review BODIES as well as the threads: PR #303 merged with six findings that sat in a review body only.
- The design audit of 2026-11-02 (inbox message `design-audit-001`): D25 says `pm-operator drain` asks nothing (`shared/system.adoc:199`); the command tree follows `terminal-client/01-commands` and the extra top-level verbs `start`, `onboard`, `enrol`, `unenrol` are folded in or removed by a clients package of its own. Do not add Ctrl+C work to a verb that package removes without saying so.

## Claim Labels

- OBSERVED: the eight handlers of PLAN-91 return 130 — read in `landings/PLAN-91.md` and the merge diff `94bbefe1`
- OBSERVED: `StartDialogue:125` and `PlanSettingsDialogue:77` still return 0 on the interrupt — read by the orchestrator at `94bbefe1`
- HYPOTHESIS: the other handlers named in deliverable 2 map the interrupt as the landing report says — read by the analysing agent, not by the orchestrator: confirm per handler (verify-at-outline)
- HYPOTHESIS: a Ctrl+C typed into a view-opened dialogue on a direct verb never reaches it, because of the second terminal — unproven; deliverable 1 settles it
- HYPOTHESIS: `exit_ca_mode` is sent twice on `stop()` after `suspend()` (`TuiScreenDriver:514-539`) — confirm or refute at the file
- Source: `landings/PLAN-91.md` (layer 3), CodeRabbit's review of `0c92ada1` on PR #303, inbox `design-audit-001`.
- Verify-first clause: before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/StartPrompt.java` — present at HEAD 94bbefe1
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/StartDialogue.java` — present at HEAD 94bbefe1
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/PlanSettingsDialogue.java` — present at HEAD 94bbefe1
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/WorkChatDialogue.java` — present at HEAD 94bbefe1
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ProjectSelector.java` — present at HEAD 94bbefe1
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/AnswerCommand.java` — present at HEAD 94bbefe1
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/CredentialsCommand.java` — present at HEAD 94bbefe1
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/EnrolHarnessCommand.java` — present at HEAD 94bbefe1
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/RolesCommand.java` — present at HEAD 94bbefe1
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/OnboardCommand.java` — present at HEAD 94bbefe1
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/PlansCommand.java` — present at HEAD 94bbefe1
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/PlanCommand.java` — present at HEAD 94bbefe1
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ProjectsCommand.java` — present at HEAD 94bbefe1
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/SystemCommand.java` — present at HEAD 94bbefe1
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/PmOperator.java` — present at HEAD 94bbefe1
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/overview/OverviewFullScreenApp.java` — present at HEAD 94bbefe1
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/tui/TuiScreenDriver.java` — present at HEAD 94bbefe1
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/InterruptExitCodeTest.java` — present at HEAD 94bbefe1
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ActionsFullScreenTest.java` — present at HEAD 94bbefe1
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ReadVerbExitCodesTest.java` — present at HEAD 94bbefe1
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/SystemViewConsistencyTest.java` — present at HEAD 94bbefe1
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/InterruptDialogueIT.java` — present at HEAD 94bbefe1
- OBSERVED: `pm-operator/src/test/resources/screen-captures/91-cli-interrupt-and-transcripts.txt` — present at HEAD 94bbefe1
- OBSERVED: `doc/design/terminal-client/03-dialogues.adoc` — present at HEAD 94bbefe1
- OBSERVED: `doc/design/terminal-client/04-console.adoc` — present at HEAD 94bbefe1
- OBSERVED: `doc/design/terminal-client/01-commands.adoc` — present at HEAD 94bbefe1

## Dependencies and Sequencing

- Depends on: `PLAN-91` (shipped), `PLAN-17` (shipped), `PLAN-02` (shipped, the PTY harness)
- Overlaps with: `PLAN-21`, `PLAN-23`, `PLAN-30`, `PLAN-36`, `PLAN-42` (name `PlanCommand`, the operator catalogue pair or `ChoicePrompt`), `PLAN-79` (rewrites the tests of Drain, AcceptEdit, Start, Onboard, Enrol — sequence one of the two after the other), the D26 clients package (removes `start`, `onboard`, `enrol`, `unenrol` as top-level verbs)
- Deliverable 1 is a verification first: if the pseudo-terminal test passes at the base, the fix part is dropped and the plan shrinks to deliverables 2 and 3.

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-92-cli-interrupt-completion.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
