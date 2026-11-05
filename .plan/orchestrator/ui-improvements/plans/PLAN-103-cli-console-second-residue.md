# PLAN-103: CLI: What the Console Badges Residue Plan Left Open

epic: ui-improvements
workstream: WS-04

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.
> Staged 2026-11-05 from the landing analysis of PLAN-98 (`landings/PLAN-98.md`: PR #332 merged with 0 unresolved threads and 0 Sonar issues, but the ten findings of CodeRabbit's review BODY unhandled, the eight threads of PR 321 still unanswered on GitHub although the lane reports them answered, the design document untouched, and mutation evidence mostly absent). One pull request on purpose: each pull request costs a CodeRabbit review. Keep it under 100 files.

## Objective

PLAN-98 made the console badges bounded and structured, but a malformed daemon answer still empties the project badges, the refresh can wait about ten seconds behind a lock, the step count still has a second source, Enter on a project badge sends a directory name that may not address the open run, the design says something else than the code, the generic catches stand, and eighteen CodeRabbit items (ten in a review body, eight threads of PR 321) were never answered on GitHub. This plan closes them so that the console keeps its badges across a bad answer, the step list has one source, the design and the code agree, and every item has a reply where the reviewer reads it.

## Deliverables

1. **The badge reads, the lock and the step list**
   - What: (a) A 200 answer without an `onboardings` list (`EntryFlow.java` ~697-711) is a failed read: the cached project badges stay and the failure is marked quietly (CodeRabbit F2); the same for a malformed body. (b) `badgeRefreshLock` has a deadline: the prompt does not wait behind two serial 5 s reads (`PmConsole.java` ~324, ~652; the thread `:319` was withdrawn on "by design", the delay is measurable). (c) One source for the step count: remove the static `STEP_KEYS`, `MACHINE_STEPS` and the unused `STEP_WHAT_KEYS` (`EntryFlow.java` ~62-69, ~621, ~639, ~662) so the badge total is `aspects.size()` of the instance (CodeRabbit F1), and the intro "Seven steps." / "Sieben Schritte." (`operator.en.properties` / `operator.de.properties` ~116) follows the aspects. (d) Enter on a project badge sends `project setup <basename>` (`PmConsole.java` ~263): the badge carries the checkout directory name, which may not address the open run or may hit another project with the same id (CodeRabbit thread `EntryFlow.java:712`, resolved while the bot said the concern remains); the web app uses the checkout path. Decide how the badge addresses the run (the run or the project id the daemon returns), carry it, and record the reason. (e) The generic catches in production code go: `catch (Exception e)` at `EntryFlow.java` ~394, ~582, ~819 and `catch (IOException | RuntimeException _)` at ~736 become the exceptions the code can actually throw (CLAUDE.md). (f) The dead or test-only surface leaves production or becomes test code: `formatLastUsed(raw)`, `OperatorContext.withClock`, the `badges(...)` string family, `projectSetupBadges`, `setBadgesForTest`; `ConsoleBadge.Kind` and its factories get Javadoc.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
   - Done when:
     - Tests that fail before: a 200 without `onboardings` keeps the cached project badges; the prompt is not delayed past the deadline by a stalled refresh; the badge total follows an aspect list of another size; Enter on a project badge addresses what the decision of (d) says (evidence order: `test-first`)
     - The catches are specific and `git grep "catch (Exception"` in `pm-operator/src/main` names none of the lines above (evidence order: `none`)
2. **The design, the German row and the clock**
   - What: (a) `doc/design/terminal-client/04-console.adoc` line 24 says "the most recent one when several are open"; the code takes the first machine or project badge in runtime order. Decide which is right, change the other, and put the raw-mode decision (today only a Javadoc in `DirectDialogueInterruptIT`) in the design with the anchor the lane cited. (b) `TranscriptNormalizer.java` (~35) maps only the English "last used": the German row says "zuletzt benutzt"; make the pattern follow the catalogue words and add the German transcript line it needs. (c) `ConsoleSettingsTranscriptTest` (~37, CodeRabbit N2) renders raw ages with a fixed clock, and `machineSettingsClockMovedForward120Days` uses `Clock.offset(systemUTC, 120d)` (the wall clock): a fixed clock that fails when the console ignores the injected one.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-documents:ref-asciidoc
   - Done when:
     - Tests that fail before: the German row normalizes to the same `<ago>`; the clock test fails when the console reads the system clock (evidence order: `test-first`)
     - The design names the Enter rule and the raw-mode decision, and no statement of it differs from the code (evidence order: `none`)
3. **The tests can fail, and the evidence is readable**
   - What: (a) The nitpicks and follow-ups of review `5785729266`: `EntryFlowTest` (~550, F3) asserts the initial badge and the final badge across distinct snapshots, and the `theBadgeTotalFollowsTheAspects` assertion no longer passes `aspects.size()` itself; `StateDialogueTest` (~244, F4) derives its list; `WebDevicesCommandTest` (~693, N1) asserts the re-prompt; `PmConsoleBehaviourTest` (~484, N3) matches the exact setup request, not `contains("cui-http")`, and (~356, N4) stalls only the project-onboardings read with margin over the 5 s bound; `ActionSupportTest` (~140, N5) covers the non-interrupt exit codes at the chat sites; `OperatorContextTerminalTest` (~165, N6) runs production code, not a copy of its try/finally; `reconnectLatch` in `aReconnectReadsTheState` is awaited; the "changes the catalogue template" test changes the catalogue. (b) The `mutation-after` evidence PLAN-98 cited but did not produce: for the rename and listener-off refusals plus `act_failed`, the `?` help, `clearTerminal`, the floor assert and the `leaveBack` sites, each fails when its code is reverted, with the log path in the pull request. (c) The terminal check of PLAN-98 captured identical before and after files: capture the badge line of a stalled and of a recovered runtime so that before and after differ.
   - Persona: plan-marshall:persona-module-tester
   - Skills: pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
   - Done when:
     - Each item fails when the code it covers is reverted (evidence order: `mutation-after`), with the log path in the pull request
4. **Every item is answered where the reviewer reads it**
   - What: (a) Each of the EIGHT threads of PR 321 (`EntryFlow.java` 63, 551, 568, 570, 578; `PmConsole.java` 65, 268, 318; review `5779338709`) still has one comment and no reply: reply IN THE THREAD with the commit or reason (most are fixed by PLAN-98; say which commit) and resolve it. A list in a pull request body does not answer a thread. The six nitpicks of PR 321's comment `6648885809` get a reply too. (b) The ten findings of review `5785729266` of PR 332 (F1-F4, N1-N6) each get a reply naming the commit or the reason, and thread `EntryFlow.java:712` is reopened or answered with the decision of 1(d). (c) Read the open-issue count at the pull request head.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core
   - Done when:
     - The pull request lists the threads and body findings with the commit or reason of each, and none is unresolved at the head, PR 321's eight included (list them through the GraphQL `reviewThreads` read for PR 321 and for your own pull request) (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md): `./mvnw -pl pm-operator verify -Ppre-commit` and, for the transcript tests, `./mvnw -pl pm-operator verify -Pintegration-tests` pass at the head (a failure beyond the base names is yours).
- No new dependency or plugin.
- Code standards (CLAUDE.md): Java 25, no generic `Exception`/`RuntimeException` catch or throw in production code, `@NullMarked` packages, JUnit 5 only, input test data from `Generators`, `CuiLogger` through `LogRecord` constants, a documented public type.
- The design document is updated in the same pull request as the code (UX standing rule of the epic).
- Ports of this plan: the stub daemon and mock ports `21030` and `21031` where a deliverable starts a process. Other plans may run at the same time: never stop or reuse a process this worktree did not start.
- Terminal check of the console with the terminal capture tooling of the epic (`.plan/temp/terminal/`): before and after captures must DIFFER where the change is visible; identical captures prove nothing.
- ZERO Sonar findings (the operator, 2026-11-05): this project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`) — treat it as `sonar: configured`. The pull request merges with 0 open new-code issues, every open issue fixed in a commit of THIS pull request (never on a side branch), none dismissed without the operator's word, an undecidable read is a stop ("Wait and resume later"). State `sonar_open_new_issues=N` in the landing message.
- Merge rule (the operator, 2026-11-02): at least one CodeRabbit review of some head with its findings handled; later heads need no review, re-trigger once and never wait. Handle the findings of EVERY CodeRabbit review that exists, a late one included, and read the review BODIES (Follow-ups and Nitpicks as well as the outside-diff section) as well as the threads. Reply IN the thread on GitHub: a list in your pull request body or in a progress file does not answer a thread (PLAN-98 reported "answered all 8" with all 8 unanswered). Before you merge, list the unresolved threads of your own pull request through the GraphQL `reviewThreads` read and state `unresolved_review_threads=N` at the head in your landing message.

## Claim Labels

- OBSERVED: `04-console.adoc` is not in the PR 332 diff and line 24 still says "the most recent one when several are open"; `MACHINE_STEPS` is used at `EntryFlow.java` :621, :639, :662; `catch (Exception` stands at :394, :582, :819; `PmConsoleBehaviourTest.java:484` matches `contains("cui-http")` — read by the orchestrator at `6be3c48f`
- OBSERVED: the eight threads of PR 321 are unresolved with one comment each; PR 332 has 10 threads, 0 unresolved; review `5785729266` has a 20359-char body with 4 follow-ups and 6 nitpicks — read through GraphQL and `gh api` on 2026-11-05
- HYPOTHESIS: a 200 without `onboardings` empties the project badges, the lock wait can reach 10-20 s, the badge `projectId` is the checkout directory name and Enter addresses it wrongly, the clock test reads the wall clock, the mutation evidence is absent — by a read-only analysing agent's reading at `6be3c48f`, not run
- HYPOTHESIS: the real `pm-mcpd` address of a run for `project setup` — not checked (another repository)
- Source: `landings/PLAN-98.md`, `landings/PLAN-94.md`.
- Verify-first clause: before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/EntryFlow.java` — present at HEAD 6be3c48f
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/PmConsole.java` — present at HEAD 6be3c48f
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/OperatorContext.java` — present at HEAD 6be3c48f
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/WebCommand.java` — present at HEAD 6be3c48f
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ConsoleBadge.java` — present at HEAD 6be3c48f
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/EntryFlowTest.java` — present at HEAD 6be3c48f
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/PmConsoleBehaviourTest.java` — present at HEAD 6be3c48f
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ActionSupportTest.java` — present at HEAD 6be3c48f
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/OperatorContextTerminalTest.java` — present at HEAD 6be3c48f
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/WebDevicesCommandTest.java` — present at HEAD 6be3c48f
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/dialogue/StateDialogueTest.java` — present at HEAD 6be3c48f
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/TranscriptNormalizer.java` — present at HEAD 6be3c48f
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/ConsoleSettingsTranscriptTest.java` — present at HEAD 6be3c48f
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/DirectDialogueInterruptIT.java` — present at HEAD 6be3c48f
- OBSERVED: `pm-operator/src/test/resources/transcript/console-settings.transcript` — present at HEAD 6be3c48f
- OBSERVED: `pm-api/src/main/resources/messages/operator.en.properties` — present at HEAD 6be3c48f
- OBSERVED: `pm-api/src/main/resources/messages/operator.de.properties` — present at HEAD 6be3c48f
- OBSERVED: `doc/design/terminal-client/04-console.adoc` — present at HEAD 6be3c48f

## Dependencies and Sequencing

- Depends on: `PLAN-98` (shipped)
- Overlaps with: `PLAN-97` (staged; the command tree: `console-settings.transcript` and the `settings` verb removal: read this plan's landing first or land PLAN-97 second and rebase), `PLAN-95` (running; the mock only: disjoint)
- Should land before: `PLAN-21`, `PLAN-23`, `PLAN-30`, `PLAN-34`, `PLAN-36`, `PLAN-42`, `PLAN-44`, `PLAN-50`, `PLAN-79`, `PLAN-84` (same files, per `corpus cross-check` on 2026-11-05: they read the console badges)
- Adjacent to: none

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-103-cli-console-second-residue.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
