# PLAN-94: Terminal: the CodeRabbit Findings Nobody Handled

epic: ui-improvements
workstream: WS-04

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.
> Staged 2026-11-02 from the audit of every CodeRabbit review in the ledger's history (`landings/coderabbit-audit/`). One pull request on purpose: each pull request costs a
> CodeRabbit review (quota-limited), so the open aspects of one area are collected here. Keep the pull request under 100 files
> (CodeRabbit refuses more); if it would exceed that, split it by deliverable and say so in the progress record.

## Objective

Close the findings of CodeRabbit on `pm-operator` (and the `pm-api` records it reads) that were never fixed and never answered with a reason: the system view of PLAN-26, the console and onboarding flow of PLAN-13 and PLAN-22, and the runtime acts of PLAN-17. They were found by reading review BODIES and unresolved threads. The Ctrl+C work of PLAN-91 was `PLAN-92` (shipped 2026-11-02, `c7d9dcb8`); its open clauses are deliverable 5. One pull request.

## Deliverables

1. **The system view of PLAN-26 keeps its promises**
   - What: (a) After a `RuntimeDrainingEvent`, a later status read of upgrading, stopping or stopped keeps the event-sourced "draining" line, because the guard `systemBanner == null || !systemBannerFromEvent` holds for every non-running status; keep the event line only while the status is DRAINING (`OverviewFullScreenApp.java` ~1742; PR 299, unresolved thread). (b) `SystemView.stateLabel` returns the runtime state key unsanitised (`state.replace('_',' ')`, `SystemView.java` ~316-321) while the other runtime names pass through `clean(...)`: an escaped control sequence in a state key reaches the printed work line (PR 299, unresolved thread). (c) `WebCommand.handleInteractive` wraps `processInteractiveChoice` in `catch (IOException | RuntimeException _) { /* ignore */ }` (~472-475): a failed rename, revoke or listener-off in `web devices list --interactive` prints nothing and exits 0; the non-interactive path was fixed in PR 299 (PR 299, unresolved thread). (d) `SystemLevel.rootRows` pairs `SystemView.root` figures with a hard-coded `ids` list by index (`SystemLevel.java` ~350), duplicating `figureOf`'s id-to-figure mapping: derive it. (e) `SystemRecordsTest` has no case where both `state`/`lifecycle_state` and both `drain_withdrawable`/`can_withdraw` are present with conflicting values (~374-379). (f) `SystemFullScreenTest.drillsDownIntoASection` asserts only "locks  3 held" and nothing for the Jobs build and test leaves (~357-387).
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:javadoc, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-dev-java-cui:cui-logging
   - Done when:
     - One test per item (a) to (c) fails before: the draining line leaves on upgrading, stopping and stopped; a state key with a control sequence prints clean; a failed device act in the interactive list exits 1 with the refusal (evidence order: `test-first`)
     - (d) is derived from the same mapping `figureOf` uses; (e) and (f) are added and fail when the code they cover is reverted (evidence order: `mutation-after`)
2. **The console and the onboarding flow keep their state in step**
   - What: `EntryFlow.refreshedAfterDialogue` lets the `IOException` of `StatusCommand.fetch` propagate (~213-217): keep the entry result when the refresh fails (PR 275). The window title is not updated after the setup dialogue (`PmConsole.refreshSetupBadge` ~286-292 replaces badges only; `updateWindowTitle` runs at ~138 and ~575): update it (PR 275). `PmConsole.startEventListener` (~450-475) only flips `reconnecting` and reads no status after a reconnect: read the runtime state (PR 275). `refreshSetupBadge` runs only if `dialogue.run()` returns, so leaving with `q` after a step skips it (`continueOnboarding` ~266-269; PR 275, low value: do it or answer it with a reason). `EntryFlow.badges` never adds badges for open project onboardings, and `ConsolePrompt.formatProjectSetupBadge` is not used by production code (PR 270; the design `04-console.adoc#prompt` requires them). The badge total uses the hard-coded `STEP_KEYS.size()` (~515): derive it from `OnboardingAspect` (PR 275). `StateDialogue.projectStepOf` is a hand-listed switch with `default -> DONE` (~621-629): derive it from the state definition or add a test that ties it to the definitions (PR 275, partial). The final frame of a project setup does not name the project: `project.ready.title` ("{project} is ready", `projects.en.properties` ~126) exists and is used nowhere in production, while `project.completed.title` is generic (PR 275; `StateDialogue.content` ~286). A linkless non-final frame times out after 60 s and `run()` returns it normally, so `onboard` and `project setup` may exit 0 before setup completes (`OnboardCommand` ~112-113; CodeRabbit marked it unverified): verify, and either fix it or record why exit 0 is right.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java:junit-integration, pm-dev-java-cui:cui-testing
   - Done when:
     - One test per item that fails before it (a refresh that throws keeps the result; the title follows the dialogue; a reconnect reads the state; a project with an open onboarding shows its badge; the total follows `OnboardingAspect`; the final frame names the project) (evidence order: `test-first`)
     - The two `unverified` or low-value items carry a recorded decision with the reason, in the progress record and in a comment at the code (evidence order: `none`)
     - Terminal check (screen capture) at 120 × 30 and 80 × 24, `en_US.UTF-8` and `de_DE.UTF-8`, before and after, on the console prompt with an open project onboarding and on the last frame of a project setup (evidence order: `none`)
3. **The runtime acts do not hang on a silent runtime**
   - What: `RuntimeActDialogue.ask()` reads `GET /api/v1/system` before it draws the timing prompt (`RuntimeActDialogue.java` ~160, ~277); the unix-socket client has no read deadline, so a runtime that accepts and never answers blocks the stop, restart and upgrade confirmation (PR 292). The `--no-input` path no longer reads the snapshot.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
   - Done when:
     - A test with a server that accepts and never answers: `stop` leaves with a stated refusal within the bound instead of hanging; it fails before (evidence order: `test-first`)
4. **The harness tests assert what they name**
   - What: `OnboardingDialogueIT` counts the frame title `Set up this machine for plan-marshall` (`onboard.frame.title`) with `>= 2` on the raw transcript (~84), so a title redraw can satisfy it without a re-ask (PR 232; the class was `@Disabled` at merge: check whether it still is, and if it is, say why in the progress record). Not here: `DialogueSourceTest.java:96` (`PLAN-95` takes it with the rest of the project-setup link handling).
   - Persona: plan-marshall:persona-module-tester
   - Skills: pm-dev-java:junit-core, pm-dev-java:junit-integration, pm-dev-java-cui:cui-testing
   - Done when:
     - The re-ask assertion counts a prompt event (the question text the dialogue draws on a re-ask), not a title; it fails when the dialogue stops re-asking (evidence order: `mutation-after`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is fixed or dismissed with a stated reason (evidence order: `none`)
5. **What PLAN-92 left open (`landings/PLAN-92.md`)**
   - What: (a) FUNCTIONAL, first: `WebCommand.confirm()` (~642-657, existed before PLAN-92) returns `true` for a null answer and for `IOException | RuntimeException`, so an end of input (Ctrl+D) or a read failure at the revoke prompt CONFIRMS the destructive DELETE; only the interrupt was carved out. A null or failed read must not confirm (treat it as leaving, like `b`; decide the exit code from the dialogue rules and say which). (b) `StartPrompt.reader()` (~211) opens a provider terminal that is never closed on a direct verb without a terminal scope. (c) `ownsTerminal` compares only the innermost scope and `clearTerminal()` pops without checking which terminal it pops (harmless while every scope pushes the same terminal): make it check, and `OperatorContextTerminalTest.scopesNest` (its final `assertSame` cannot fail because the provider is `view::terminal`) must be able to fail. (d) The `web devices` action-prompt rethrow (`WebCommand` ~473) has no test (only revoke is tested); the ended-chat leave-back mapping to 0 (`OperatorActions` ~108, `ActionSupport` ~288, lane finding F15) has no test; `SystemFullScreenTest.aFailedSystemReadKeepsTheStatusLine` does not assert that the 500 read happened and both new System tests set the console active (they are not direct-verb runs). (e) PLAN-92's deliverable 3 clauses that stayed open: a `projects` verb test for Ctrl+C (`ProjectsFullScreenTest` has none; its view fixture reconnects before the menu opens); an assertion on the terminal attributes beside alternate screen and cursor; `InterruptDialogueIT` waits 700 ms of quiet (`SETTLE`, ~46, ~154-169): wait on a condition; `ReadVerbExitCodesTest` `refusedReads()` is a hand list (the `project <id>` gap CodeRabbit named): derive it from the registered read verbs, or answer it with the reason the registry cannot give one; the token-row tie and the summary assertion of `SystemViewConsistencyTest` (CodeRabbit, PR 303; the lane's attempt was reverted because the rendered summary carries a floor marker: assert the marker). (f) `WebDevicesCommandTest` ~410: malformed formatting with fully qualified types, `Generators` called in a static `@MethodSource` outside the controller's seed, and the literal input `"iPad Oliver"`. (g) The keystroke of `ViewDialogueInterruptIT` and `DirectDialogueInterruptIT` is typed right after `awaitText` with no reader-ready condition (flake risk, inferred): add the condition or show it cannot race.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java:junit-integration, pm-dev-java-cui:cui-testing
   - Done when:
     - A test for (a) fails before: an end of input and a failed read at the revoke prompt send no DELETE (evidence order: `test-first`)
     - Items (b) to (g) each have a test that fails when the code it covers is reverted, or a recorded decision with the reason (evidence order: `mutation-after`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md): `./mvnw -pl pm-operator -am verify -Pintegration-tests` passes at the head.
- No new dependency or plugin.
- CUI standards (`CLAUDE.md`): a test that takes input data draws it from `Generators` with `@EnableGeneratorController` (the expected value of a contract stays literal); a log call at INFO or above goes through a `LogRecord` of the module's `LogMessages` class with a `LogAsserts` test; `@NullMarked` on every package of `pm-operator`; JUnit 5 only, no Mockito, no Hamcrest.
- Java 25, Lombok, records; no generic `Exception`/`RuntimeException`.
- Every new operator text goes through `MessageCatalogue` into `operator.en.properties` and `operator.de.properties`.
- Ports of this plan: mock web port `20940`, app port `20941`. Other plans may run at the same time: never stop or reuse a process this worktree did not start.
- Terminal check of the rendered output through the screen capture harness of `cli-pty-dialogue-harness` (`ScreenCaptureIT`, command line in `doc/design/terminal-client/27-tui-testing.adoc`): captures to `pm-operator/src/test/resources/screen-captures/{plan}.txt`, the set run BEFORE the change and copied to `.plan/temp/terminal/{plan}/before/`, AFTER to `.../after/`. Capture at 120 × 30 and 80 × 24 in `en_US.UTF-8` and `de_DE.UTF-8`.
- The design document is updated in the same pull request as the code (UX standing rule of the epic).
- The design audit of 2026-11-02 (inbox `design-audit-001`): decision D26 folds in or removes the top-level verbs `start`, `onboard`, `enrol` and `unenrol` in a clients package of its own. Do not add work to a handler that package removes without saying so; the `onboard` items of deliverable 2 belong to the console flow, which stays.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`) — treat it as `sonar: configured`. An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason.
- Merge rule (the operator, 2026-11-02): at least one CodeRabbit review of some head with its findings handled; later heads need no review, re-trigger once and never wait. Handle the findings of EVERY CodeRabbit review that exists, a late one included, and read the review BODIES as well as the threads.

## Claim Labels

- OBSERVED: each finding of deliverables 1 to 4 is present on `main` at `94bbefe1` — read by the auditing agent against the merge commit and `main`, with the cited lines; the orchestrator did not re-read each one (verify-at-outline)
- HYPOTHESIS: line numbers are approximate; confirm each finding at the symbol named before changing it
- HYPOTHESIS: a linkless non-final frame lets `onboard` exit 0 (unverified by CodeRabbit and by the auditing agent)
- HYPOTHESIS: `OnboardingDialogueIT` is still `@Disabled` — confirm at the file
- Source: `landings/coderabbit-audit/` (PRs 232, 270, 275, 292, 299), the review bodies of `coderabbitai`.
- OBSERVED: `WebCommand.confirm()` returns `true` for a null answer and for `IOException | RuntimeException` — read by the orchestrator at `c7d9dcb8` (landings/PLAN-92.md lead 1)
- HYPOTHESIS: items (b) to (g) of deliverable 5 — read by the analysing agent at `c7d9dcb8`, not re-read by the orchestrator (verify-at-outline)
- Verify-first clause: before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted finding re-scopes the deliverable and is reported, never silently worked around.

- Sequencing (2026-11-04): PLAN-97 (staged) folds or removes the top-level verbs `start`, `onboard`, `abort` and `unenrol` and rewrites the drain and accept-edit handlers; it starts after this plan lands. Do not add work to a handler that PLAN-97 removes without saying so.

## Expected Surface

- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/overview/OverviewFullScreenApp.java` — present at HEAD 94bbefe1
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/system/SystemView.java` — present at HEAD 94bbefe1
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/system/SystemLevel.java` — present at HEAD 94bbefe1
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/WebCommand.java` — present at HEAD 94bbefe1
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/EntryFlow.java` — present at HEAD 94bbefe1
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/PmConsole.java` — present at HEAD 94bbefe1
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ConsolePrompt.java` — present at HEAD 94bbefe1
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/OnboardCommand.java` — present at HEAD 94bbefe1
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/dialogue/StateDialogue.java` — present at HEAD 94bbefe1
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/RuntimeActDialogue.java` — present at HEAD 94bbefe1
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/system/SystemRecordsTest.java` — present at HEAD 94bbefe1
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/SystemFullScreenTest.java` — present at HEAD 94bbefe1
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/OnboardingDialogueIT.java` — present at HEAD 94bbefe1
- OBSERVED: `pm-api/src/main/resources/messages/projects.en.properties` and `projects.de.properties` — present at HEAD 94bbefe1
- OBSERVED: `doc/design/terminal-client/04-console.adoc` — present at HEAD 94bbefe1
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/WebCommand.java` — present at HEAD c7d9dcb8 (deliverable 5)
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/StartPrompt.java` — present at HEAD c7d9dcb8 (deliverable 5)
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/OperatorContext.java` — present at HEAD c7d9dcb8 (deliverable 5)
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/OperatorActions.java` — present at HEAD c7d9dcb8 (deliverable 5)
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ActionSupport.java` — present at HEAD c7d9dcb8 (deliverable 5)
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/OperatorContextTerminalTest.java` — present at HEAD c7d9dcb8 (deliverable 5)
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ProjectsFullScreenTest.java` — present at HEAD c7d9dcb8 (deliverable 5)
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ReadVerbExitCodesTest.java` — present at HEAD c7d9dcb8 (deliverable 5)
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/SystemFullScreenTest.java` — present at HEAD c7d9dcb8 (deliverable 5)
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/SystemViewConsistencyTest.java` — present at HEAD c7d9dcb8 (deliverable 5)
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/WebDevicesCommandTest.java` — present at HEAD c7d9dcb8 (deliverable 5)
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/DirectDialogueInterruptIT.java` — present at HEAD c7d9dcb8 (deliverable 5)
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/InterruptDialogueIT.java` — present at HEAD c7d9dcb8 (deliverable 5)
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/ViewDialogueInterruptIT.java` — present at HEAD c7d9dcb8 (deliverable 5)

## Dependencies and Sequencing

- Depends on: `PLAN-26` (shipped), `PLAN-17` (shipped), `PLAN-91` (shipped), `PLAN-92` (shipped 2026-11-02, `c7d9dcb8`: read `landings/PLAN-92.md`)
- Overlaps with: `PLAN-95` (`DialogueSourceTest` stays there), `PLAN-21`, `PLAN-23`, `PLAN-30`, `PLAN-36`, `PLAN-42` (name `PmConsole`, `EntryFlow` or the operator catalogue pair), `PLAN-79`
- Adjacent to: the D26 clients package (removes top-level verbs)

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-94-cli-coderabbit-residue.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
