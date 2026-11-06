# PLAN-104: CLI: What the Console Second Residue Plan Left Open

epic: ui-improvements
workstream: WS-04

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.
> Staged 2026-11-05 from the landing analysis of PLAN-103 (`landings/PLAN-103.md`: PR #341 merged with 0 unresolved threads and 0 Sonar issues, but five CodeRabbit review-BODY items unanswered, Enter on a project badge still addressing a field the contract does not carry, test-only surface added to production, and the terminal evidence not probative). One pull request on purpose: each pull request costs a CodeRabbit review. Keep it under 100 files.

## Objective

PLAN-103 gave the console bounded, cached badge reads and one step list, but Enter on a project badge reads a `project_id` that `BeingAddedProject` does not have (the test writes the field into its own fixture), five CodeRabbit items of two review bodies were never answered, the step count and its copy still have two sources, `bounded` lost its error path, test-only surface grew in production (`MessageCatalogue.setForTest`, `PROJECT_STATES`, test-only constructors), the German normalizer is half done, the terminal capture compares two different scenarios, and generic catches stand in `pm-operator/src/main`. This plan closes them so that the console addresses a being-added project by what the contract carries, the surface is production code only, every item has an answer where the reviewer reads it, and the evidence can fail.

## Deliverables

1. **A being-added project is addressed by what the contract carries**
   - What: (a) `EntryFlow.java` ~657-664 prefers `JsonTree.string(map, "project_id")`; `BeingAddedProject` (`pm-api`) has only `checkout_path`, `current_step`, `step_number`, `total_steps`, `continue_link` and `abandon_link`, and `doc/design/shared/project-onboarding.adoc:77` says a pre-enrolment run is entry-scoped. The test `PmConsoleBehaviourTest.enterOnAProjectBadgeAddressesDaemonProjectId` writes a `project_id` into its own fixture. Read the contract (`BeingAddedProject`, `project-onboarding.adoc`, how the web app addresses the run: the checkout path or `continue_link`), and make Enter on a project badge send what the contract carries; no field the contract lacks. If the contract needs a field from the daemon, record it as an open item with its owner (`plan-marshall/plan-marshall-mcp`) in the design and the pull request; never invent it in a fixture. (b) The decision and its reason go into `04-console.adoc` (today only a thread reply holds it). (c) `PmConsole.java:288` builds `project setup "…"` with the id inside quotes and `parseArgs` (~899-923) does not read backslash escapes: an id with a quote or a backslash is altered (CodeRabbit follow-up `EntryFlow.java:657-660`, unhandled).
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-documents:ref-asciidoc
   - Done when:
     - Tests that fail before, with fixtures built from `BeingAddedProject.toMap` only (no invented field): Enter on a project badge sends the address the decision of (a) names; an id with a quote or a backslash reaches the command unaltered (evidence order: `test-first`)
     - `04-console.adoc` names the addressing and the reason; the open daemon item, if any, is named with its owner (evidence order: `none`)
2. **The five unanswered review items, and the surface that is test-only**
   - What: (a) CodeRabbit review `5791531265` (follow-up `StateDialogue.java:47-51`: `PROJECT_STATES` is a hand-written list that mirrors the production switch; nitpicks `EntryFlowTest.java` ~589-590: assert row 8, `EntryFlowTest.java:623` `contains("8") && contains("Language")` passes without a row 8; `PmConsoleBehaviourTest` ~420-421: a contention test of the lock budget, the existing concurrency test holds the lock about 200 ms) and review `5792262598` (nitpick `StateDialogueTest.java:245`: `setupStates` repeats all 16 ids). The state-to-step table lives once in production code (a map the switch uses, package-private) and `StateDialogueTest` derives its expectation from the contract, not from a copy. (b) Test-only surface leaves production or becomes test code (PLAN-103's own D1(f) stands): `MessageCatalogue.setForTest` and `clearCacheForTest` (`pm-api/src/main/java/de/planmarshall/api/message/MessageCatalogue.java` ~357-380: public API that mutates a JVM-wide static), the `PmConsole` constructors `(context, initialBadges)` and `(context, initialBadges, machineSteps)` (~101-110), `EntryFlow.getStepName(...)` (~535-545, which hard-codes `OnboardingAspect.values()`). (c) `EntryFlow.bounded` (~574-601) lost its error path in `6f0252fe`: a `RuntimeException` in the worker kills the thread, prints a stack trace and returns `null`, and callers dereference it (`badges.addAll(null)`); restore it without a generic catch (a specific exception the worker can throw, or an uncaught-exception handler that marks the read failed), interrupt the worker on an `InterruptedException` and on a timeout. The `Math.max(1L, bound.toMillis())` clamp (~587) is pinned by a test: a sub-millisecond bound does not wait forever. (d) The Sonar fixes of `6f0252fe` were a disguise in one place: `new CountDownLatch(1).await(4, SECONDS)` in the stalled-server handlers of `PmConsoleBehaviourTest` is a sleep; wait on the condition the code produces (a latch the test releases, a bounded wait on the real signal). The deadline test has 1 s of margin against the 5 s budget: widen it. (e) `ConsoleBadgeTest`, `PmConsoleBehaviourTest` and `EntryFlowTest` take hand-written input literals and carry no `@EnableGeneratorController`: input comes from `Generators`; `refreshBadgesUpdatesBadgesAndWindowTitle` asserts the window title it names.
   - Persona: plan-marshall:persona-module-tester
   - Skills: pm-dev-java:java-core, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
   - Done when:
     - Each item fails when the code it covers is reverted: the row-8 assertion, the state table derived once, the lock contention test, the `bounded` error path (a worker that throws marks the read failed and returns no `null`), the join clamp (evidence order: `mutation-after`), with the log path in the pull request
     - `git grep -n "ForTest\|clearCacheForTest" -- pm-api/src/main pm-operator/src/main` names none (evidence order: `none`)
     - The five items are answered with the commit or the reason in the pull request AND in a reply to CodeRabbit's review comment on GitHub, and none is left (evidence order: `none`)
3. **The step count has one source, in copy and in design**
   - What: `PmConsole.java` ~98-102 takes `machineSteps` from a throw-away `new EntryFlow(context)` while the real flow is created at ~148; the console refresh path is never tested with an aspect list of another size. `onboard.entry.intro` ("Seven steps." / "Sieben Schritte.", `onboarding.en/de.properties:15`) is hard-coded, so one console session prints "7 steps." (`console-settings.transcript:5`) and later "Seven steps." (`:20`). Make the intro follow the aspects, reconcile the two copies, and update the designs that still show "Seven steps." (`05-transcripts-entry.adoc` ~145, ~160, ~477; `web-app/onboarding.adoc:72`; the `doc/claude_review` screens that show it).
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-documents:ref-asciidoc
   - Done when:
     - A test that fails before: the console refresh path and the intro line follow an aspect list of another size (evidence order: `test-first`)
     - No design or screen capture of the epic's client documents shows a count that the code does not produce (evidence order: `none`)
4. **The German row and the clock, with evidence that can fail**
   - What: (a) `TranscriptNormalizer` hard-codes `(?:last used|zuletzt benutzt)`: read the words from the catalogue (`MessageCatalogue`) and add the German transcript line the spec of PLAN-103 asked for, not only a synthetic test. (b) The clock test of PLAN-103 (`ConsoleSettingsTranscriptTest`) was cited with a log that fails because of the reactor (`-pl pm-operator` against a registry-snapshot `pm-api`), not because the console reads the system clock: redo it as a real mutation, the console reading the system clock fails the test with the injected clocks. (c) The terminal check of PLAN-103 compared two scenarios (`status-only` before, `fully-onboarded` after) and its "failing" and "recovered" captures are byte-identical: capture a stalled `/project-onboardings` read and a recovered one against the same runtime scenario, before and after the same code change, with an assertion on the badge line in the capture IT. The cited log `181345` was BUILD SUCCESS under the label "Failing run": the pull request cites a failing and a passing log for each `test-first` clause.
   - Persona: plan-marshall:persona-module-tester
   - Skills: pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
   - Done when:
     - The German row normalizes to the same `<ago>` through the catalogue words; the clock test fails when the console reads the system clock (evidence order: `mutation-after`)
     - The captures of a stalled and a recovered runtime differ in the badge line, are taken before and after the same change on the same scenario, and the IT asserts the line (evidence order: `test-first`)
5. **The generic catches in `pm-operator/src/main`**
   - What: `catch (IOException | RuntimeException` at `tui/TuiScreenDriver.java:232`, `OverviewStateStore.java:95`, `AnswerCommand.java:247`, `WebCommand.java:486`, `:681`; the `/*TODO: Catch specific not Exception …*/` markers of the rewrite recipe in those files; the stale `// cui-rewrite:disable` at `EntryFlow.java:732`. Each becomes the exceptions the code can throw (CLAUDE.md). The markers in `pm-api` (`Libc.java:91`, `ScenarioLoader.java:566`) and in `MockRuntimeEngine` are not this plan's: the mock is PLAN-95/PLAN-97's, the other two are listed in the pull request as open. The formatting churn of the last pre-commit run (`ConsoleBadge.java:27-31` record components at column 0, `PmConsole.java:237` `@Nullable` on its own line, 28-space continuation lines in about eight files) is fixed by running the gate once on the final head and committing what it rewrites.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-null-safety, pm-dev-java:junit-core
   - Done when:
     - `git grep -n "catch (Exception\|catch (RuntimeException\|catch (Throwable\|catch (IOException | RuntimeException" -- pm-operator/src/main` names none outside `EpicCommand.java` and `PlanCommand.java` (those two are PLAN-97's deliverable 5, they are rewritten there), and `git grep -n "TODO: Catch specific\|cui-rewrite:disable" -- pm-operator/src/main` names none (evidence order: `none`)
     - A test per changed catch for the path it now handles, failing when the catch is reverted to a generic one that swallows it (evidence order: `mutation-after`)
6. **Every item is answered where the reviewer reads it**
   - What: (a) The items of the PR 332 review `5785729266` that PLAN-103 answered wrongly: F4 ("derives the aspect list dynamically": it did not), and the PR 321 comment `6666409615` item 3 (`RuntimeActDialogueTest`: "verified … cleanly terminate" with no code change, against the earlier decline `6648885809`) and item 6 ("tests added" that already existed): reply IN the thread or comment with the true state. (b) The thread `EntryFlow.java:712` of PR 332 was resolved with a reply citing `e8ef5f8`; CodeRabbit said pre-enrolment continuation "remains deferred, as agreed": the decision of deliverable 1 is the answer; reply there. (c) The landing message states the facts the PLAN-103 landing omitted.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core
   - Done when:
     - The pull request lists every thread and body item it answers with the commit or reason, and none of its own is unresolved at the head (list the threads through the GraphQL `reviewThreads` read for your own pull request and for PRs 332 and 321) (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md): `./mvnw -pl pm-api,pm-operator verify -Ppre-commit` (the reactor includes `pm-api`: a `-pl pm-operator` run reads a registry snapshot of `pm-api` and fails for the wrong reason) and, for the transcript tests, `./mvnw -pl pm-api,pm-operator verify -Pintegration-tests` pass at the head (a failure beyond the base names is yours).
- No new dependency or plugin.
- Code standards (CLAUDE.md): Java 25, no generic `Exception`/`RuntimeException` catch or throw in production code, `@NullMarked` packages, JUnit 5 only, input test data from `Generators` (`@EnableGeneratorController`), `CuiLogger` through `LogRecord` constants, a documented public type, no arbitrary timer wait.
- The design document is updated in the same pull request as the code (UX standing rule of the epic).
- Ports of this plan: the stub daemon and mock ports `21040` and `21041` where a deliverable starts a process. Other plans may run at the same time: never stop or reuse a process this worktree did not start.
- Terminal check of the console with the terminal capture tooling of the epic (`.plan/temp/terminal/`): before and after captures are of the SAME scenario and must DIFFER where the change is visible; identical captures and captures of two different scenarios prove nothing.
- Every `test-first` clause in your landing message cites a FAILING run and a PASSING run of the named test (a passing log is not a failing run); every `mutation-after` clause cites the reverted code.
- ZERO Sonar findings (the operator, 2026-11-05): this project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`) — treat it as `sonar: configured`. The pull request merges with 0 open new-code issues, every open issue fixed in a commit of THIS pull request (never on a side branch), none dismissed without the operator's word, an undecidable read is a stop ("Wait and resume later"). A fix removes the cause: it does not delete the behaviour a marker pointed at, and it does not replace a sleep by another sleep. State `sonar_open_new_issues=N` in the landing message.
- Merge rule (the operator, 2026-11-02): at least one CodeRabbit review of some head with its findings handled; later heads need no review, re-trigger once and never wait. Handle the findings of EVERY CodeRabbit review that exists, a late one included, and read the review BODIES (Follow-ups and Nitpicks as well as the outside-diff section) as well as the threads: PLAN-103 handled the four threads of its first review and none of the five body items of two reviews, and reported "all findings fixed". Reply IN the thread on GitHub: a list in your pull request body or in a progress file does not answer a thread. Before you merge, list the unresolved threads of your own pull request through the GraphQL `reviewThreads` read and state `unresolved_review_threads=N` at the head, and `review_body_findings_total/handled`, as lines of the landing-facts block of your landing message.

## Claim Labels

- OBSERVED: `BeingAddedProject` (`pm-api/src/main/java/de/planmarshall/api/project/BeingAddedProject.java`) has the fields `checkoutPath`, `currentStep`, `stepNumber`, `totalSteps`, `continueLink`, `abandonLink` and no project id; `EntryFlow.java:657-664` reads `JsonTree.string(map, "project_id")` and falls back to the checkout directory name; `StateDialogue.java:47` holds `PROJECT_STATES` as a hand-written list — read by the orchestrator at `24523d67` (the merge commit of PR 341)
- OBSERVED: PR 341 has 4 threads, 0 unresolved; its two CodeRabbit reviews (`5791531265` on `ccf9e063`: 2 follow-ups, 2 nitpicks; `5792262598` on `e1e05fe5`: 1 nitpick) have no reply anywhere; PR 321 has 14 threads, 0 unresolved; PR 332 has 10, 0 unresolved — read through GraphQL and `gh api` on 2026-11-05
- HYPOTHESIS: `bounded` returns `null` after a worker `RuntimeException` and a caller dereferences it (no realistic trigger found), a worker blocked in socket I/O leaks one thread per refresh, `TuiScreenDriver`/`OverviewStateStore`/`AnswerCommand`/`WebCommand` hold the generic catches listed, the log `172358` failed because of the reactor — by a read-only analysing agent's reading at `24523d67`, not run
- HYPOTHESIS: how the real `pm-mcpd` addresses a pre-enrolment run — not checked (another repository)
- Source: `landings/PLAN-103.md`, `landings/PLAN-98.md`.
- Verify-first clause: before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/EntryFlow.java` — present at HEAD 24523d67
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/PmConsole.java` — present at HEAD 24523d67
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ConsoleBadge.java` — present at HEAD 24523d67
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/dialogue/StateDialogue.java` — present at HEAD 24523d67
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/AnswerCommand.java` — present at HEAD 24523d67
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/WebCommand.java` — present at HEAD 24523d67
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/OverviewStateStore.java` — present at HEAD 24523d67
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/tui/TuiScreenDriver.java` — present at HEAD 24523d67
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/message/MessageCatalogue.java` — present at HEAD 24523d67
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/message/MessageCatalogueTest.java` — present at HEAD 24523d67
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/EntryFlowTest.java` — present at HEAD 24523d67
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/PmConsoleBehaviourTest.java` — present at HEAD 24523d67
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ConsoleBadgeTest.java` — present at HEAD 24523d67
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/dialogue/StateDialogueTest.java` — present at HEAD 24523d67
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/TranscriptNormalizer.java` — present at HEAD 24523d67
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/TranscriptHarnessTest.java` — present at HEAD 24523d67
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/ConsoleSettingsTranscriptTest.java` — present at HEAD 24523d67
- OBSERVED: `pm-operator/src/test/resources/transcript/console-settings.transcript` — present at HEAD 24523d67
- OBSERVED: `pm-api/src/main/resources/messages/onboarding.en.properties` — present at HEAD 24523d67
- OBSERVED: `pm-api/src/main/resources/messages/onboarding.de.properties` — present at HEAD 24523d67
- OBSERVED: `doc/design/terminal-client/04-console.adoc` — present at HEAD 24523d67
- OBSERVED: `doc/design/terminal-client/05-transcripts-entry.adoc` — present at HEAD 24523d67
- OBSERVED: `doc/design/web-app/onboarding.adoc` — present at HEAD 24523d67
- OBSERVED: `doc/design/shared/project-onboarding.adoc` — present at HEAD 24523d67

## Dependencies and Sequencing

- Depends on: `PLAN-103` (shipped), `PLAN-98` (shipped)
- Overlaps with: `PLAN-97` (the command tree: `console-settings.transcript` and the `settings` verb removal; `EpicCommand.java` and `PlanCommand.java` are PLAN-97's alone, deliverable 5 there; whichever plan starts second reads the other's landing and rebases)
- Should land before: `PLAN-21`, `PLAN-23`, `PLAN-30`, `PLAN-34`, `PLAN-36`, `PLAN-42`, `PLAN-44`, `PLAN-50`, `PLAN-79`, `PLAN-84` (same files: they read the console badges and the dialogue states)
- Adjacent to: none

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-104-cli-console-third-residue.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
