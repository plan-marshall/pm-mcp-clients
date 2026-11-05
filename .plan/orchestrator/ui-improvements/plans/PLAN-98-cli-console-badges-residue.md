# PLAN-98: Terminal: What the CodeRabbit-Residue Plan Left Open

epic: ui-improvements
workstream: WS-04

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.
> Staged 2026-11-05 from the landing analysis of PLAN-94 (`landings/PLAN-94.md`: PR #321 merged with EIGHT unresolved, unanswered CodeRabbit threads and clauses met only partly) and of PLAN-89's second landing (`landings/PLAN-89.md`: a golden that breaks again on about 2026-12-08). One pull request on purpose: each pull request costs a CodeRabbit review. Keep it under 100 files; if the count at outline says otherwise, split deliverable 4 into its own pull request and say so in the pull request.

## Objective

PLAN-94 shipped and closed most of its list, but the project badges it added to the console are read as text, unbounded and without a fallback, the step list and the setup badge are still copied from the catalogue, eight CodeRabbit threads of its review were never answered, and several of its own clauses are met only in part. A golden transcript of the machine-settings screen depends on the wall clock and fails again around 2026-12-08. This plan makes the badges robust, derives what is still hand-written, answers every open thread, and finishes the clauses.

## Deliverables

1. **The golden transcript no longer depends on the clock**
   - What: `WebCommand.formatLastUsed` (~612-643) prints "N weeks ago" below 8 weeks (the golden normalises it to `<ago>`) and an absolute `d MMM HH:mm` after that, from `Instant.now()`. The mock rows' `last_used_at` (`onboarding.json:2782, 2790, 2798`: 2026-10-16, 2026-10-13, 2026-09-08) cross 56 days around 2026-12-08 and 2026-12-11, and `console-settings.transcript` (where PLAN-89's PR #314 pinned the `dev-3` row as `8 Sep <time>`) fails again. Make the formatting depend on a clock the test sets (the context already carries a clock or an injectable source: check `OperatorContext`; add one only if none exists) and make the transcript independent of the day it runs: either a fixed clock in the transcript test, or a normaliser that maps the absolute form too.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
   - Done when:
     - A test with a fixed clock for 7 weeks 6 days, exactly 8 weeks and 20 weeks asserts the three forms; it fails before (evidence order: `test-first`)
     - The transcript test passes with the clock moved forward by 120 days; it fails before (evidence order: `mutation-after`)
2. **The project badges are bounded, kept on a failed read and read as data**
   - What: (a) `refreshSetupBadge` and `refreshBadges` replace the old badges with the empty list when the project-onboardings read fails or returns a malformed entry (`EntryFlow.java:566-585`; threads `:570`, `:578`): keep the badges the console shows and mark the failure quietly; one malformed entry is skipped, never all dropped. (b) The project-onboardings read and the status read on the prompt path are bounded (the `SNAPSHOT_BOUND` of PLAN-94's silent-runtime fix is the model; thread `:568`): a stalled runtime must not block the prompt after every verb (`PmConsole.java:313,633`, `followEventStream:518`). (c) Badge refreshes are serialized between the stream thread and the prompt thread (`PmConsole.java:313,633`; thread `:318`). (d) `hasOpenQuestions` and `questionCount` (`PmConsole.java:218-254`; thread `:551`) read the structured fields of the badge entries, not the rendered text: a checkout named "question", "frage" or "waiver" raises no Ctrl+D warning, and a long digit run in a name cannot throw `NumberFormatException` in the EOF handler.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java:junit-integration, pm-dev-java-cui:cui-testing
   - Done when:
     - Tests that fail before: a failed read keeps the previous badges; a malformed entry leaves the others; a stalled runtime lets the prompt return within the bound; two concurrent refreshes never interleave; a project named `question-fix` or with 40 digits raises no warning and no exception (evidence order: `test-first`)
     - A terminal check of the console with a failing runtime, before and after (evidence order: `none`)
3. **One source for the step list, the setup badge and the Enter decision**
   - What: (a) `STEP_KEYS`, `STEP_WHAT_KEYS` and the resume total `Math.min(completedStep+1, 7)` / `"/7"` (`EntryFlow.java:154,157`; thread `:63`) are derived from the same list of aspects as the badge total, so no number 7 stays in the code. (b) `MACHINE_SETUP_BADGE` (`PmConsole.java:65`; thread `:65`) is not a regex copied from the en and de catalogue templates: read the badge as a structured fact, or build the pattern from the catalogue template itself, so a changed template cannot make Enter silently do nothing. (c) Enter on a project badge is inert (`PmConsole.java:268`; thread `:268`): decide it and write the decision into the design (`doc/design/terminal-client/04-console.adoc`, line 24 still says Enter continues "the most recent" unfinished onboarding) — either make Enter continue the project's onboarding or change the design sentence; the orchestrator's recommendation is to make it continue, since the badge shows the work waiting.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
   - Done when:
     - `theBadgeTotalFollowsTheAspects` is no longer circular: it adds an aspect to the list and asserts the total, the intro line and the resume line follow; no literal `7` remains in `EntryFlow` (`git grep`) (evidence order: `mutation-after`)
     - A test changes the badge template in the catalogue and Enter still acts; a test for Enter on a project badge follows the decision (evidence order: `test-first`)
4. **The clauses PLAN-94 met only in part**
   - What: (a) D1c: rename and listener-off have interactive tests with the refusal and exit `1`, and the `act_failed` catch (`WebCommand.java:485-490`) has one with a message assertion; `?` at the revoke prompt shows the help the prompt advertises instead of leaving silently with 0 (`WebCommand.java:660-663`). (b) D1d: the pairing of `rootRows` with `SystemView.root` is by id, not by position (`SystemLevel.java:360-363`), and `SystemLevelTest` fails against the old hard-coded list. (c) D2: the `projectStepOf` tie tests the state definitions, not only catalogue keys, and does not pin the `default -> done` fallback. (d) D5a: the two failing runs for `endOfInputAtTheRevokePromptSendsNoDelete` and `aFailedReadAtTheRevokePromptSendsNoDelete` are recorded (the cited log never ran them), and both assert that the prompt was reached. (e) D5d: `leaveBack` is tested at its two call sites. (f) D5g: `DirectDialogueInterruptIT` carries a recorded decision like `ViewDialogueInterruptIT`. (g) `OperatorContext.clearTerminal` throwing `IllegalStateException` inside a caller's `finally` skips `terminal.close()` and masks the original exception: make it not throw there, or close first. (h) Tests: `SystemViewConsistencyTest.theSummaryNamesItsFloor` (~247) asserts the `≥` on the summary line; `aReconnectReadsTheState` separates the first-open refresh from the reconnect and waits on a condition, not a spin on a shared stream; `aFailedRefreshKeepsTheEntryResult` asserts the second status read; the comment "not by a leave" and the `ReadVerbExitCodesTest` Javadoc "PR 92" are corrected.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java:junit-integration, pm-dev-java-cui:cui-testing
   - Done when:
     - Each item fails when the code it covers is reverted (evidence order: `mutation-after`)
5. **Every open thread is answered, and the leads are read**
   - What: (a) Each of the eight unresolved CodeRabbit threads of PR 321 (`EntryFlow.java:63, 551, 568, 570, 578`; `PmConsole.java:65, 268, 318`) gets a reply naming the commit that fixes it or a stated reason, and is resolved; the six nitpicks of issue comment `6648885809` answered with weak reasons ("covered" by a test that does not enter the interactive dispatch: rename and listener-off) are re-answered after deliverable 4. (b) Read the three leads of the analysis and record the result: `StateDialogue.titleOf` guesses the project fact keys `name`, `project_id`, `project`, `id` (check them against the facts the mock and the specification give for `project.completed`; the real daemon is another repository: say what was not checkable); whether `reader.interrupt()` in `RuntimeActDialogue.snapshot` closes the blocked unix-socket read; whether the two failed-read WebDevices tests reach the `IOException` catch or only JLine's end of input (if only the latter, add the test that does).
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:junit-core
   - Done when:
     - The pull request lists the eight threads with the commit or reason of each, and none is unresolved at the head (evidence order: `none`)
     - The result of each lead is in the progress record (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is fixed or dismissed with a stated reason (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md): `./mvnw -pl pm-operator -am verify -Pintegration-tests` passes at the head (`-am` is needed or the module resolves a stale `pm-api` snapshot).
- No new dependency or plugin.
- CUI standards (`CLAUDE.md`): a test that takes input data draws it from `Generators` with `@EnableGeneratorController` (the expected value of a contract stays literal); a log call at INFO or above goes through a `LogRecord` of the module's `LogMessages` class with a `LogAsserts` test; `@NullMarked` on every package of `pm-operator`; JUnit 5 only, no Mockito, no Hamcrest; Java 25, Lombok, records; no generic `Exception`/`RuntimeException` in production code.
- Every operator text goes through `MessageCatalogue` into `operator.en.properties` and `operator.de.properties`.
- Interrupt rules of PLAN-91 and PLAN-92 (`landings/PLAN-92.md`, `landings/PLAN-94.md`): Ctrl+C leaves with `130` and sends no act; an end of input or a failed read at a confirmation never confirms.
- Ports of this plan: mock web port `20980`, app port `20981`. Other plans may run at the same time: never stop or reuse a process this worktree did not start.
- Terminal check of the rendered output through the screen capture harness (`ScreenCaptureIT`, command line in `doc/design/terminal-client/27-tui-testing.adoc`): captures BEFORE copied to `.plan/temp/terminal/PLAN-98/before/`, AFTER to `.../after/`, at 120 × 30 and 80 × 24 in `en_US.UTF-8` and `de_DE.UTF-8`.
- The design document is updated in the same pull request as the code (UX standing rule of the epic).
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`) — treat it as `sonar: configured`. An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason.
- Merge rule (the operator, 2026-11-02): at least one CodeRabbit review of some head with its findings handled; later heads need no review, re-trigger once and never wait. Handle the findings of EVERY CodeRabbit review that exists, a late one included, and read the review BODIES as well as the threads. PLAN-94 merged with eight threads of its review unanswered: do not repeat that. Before you merge, list the unresolved threads of your own pull request through the GraphQL `reviewThreads` read and handle each one.

## Claim Labels

- OBSERVED: eight threads of review `5779338709` on PR 321 are unresolved with one comment each (the CodeRabbit comment) — read by the orchestrator through GraphQL on 2026-11-05
- OBSERVED: `formatLastUsed` switches from "N weeks ago" to an absolute date at 8 weeks from `Instant.now()`; the mock rows' `last_used_at` are 2026-10-16, 2026-10-13 and 2026-09-08 — read by the orchestrator at `159c310c` (the dates of the next break are by reading, not run)
- OBSERVED: the badge claims of deliverables 2 and 3 are by a read-only analysing agent at `159c310c` (`PmConsole.java:65, 218-254, 268, 313, 318, 633`; `EntryFlow.java:63, 154, 157, 551-585`); the orchestrator re-read the thread state only
- HYPOTHESIS: a project checkout name containing "question" triggers the Ctrl+D warning and a long digit run throws `NumberFormatException` — by reading `leadingCount`, not run
- HYPOTHESIS: `OperatorContext.clearTerminal` can throw inside a `finally` and mask the original exception — no concrete trigger found
- HYPOTHESIS: `reader.interrupt()` closes the blocked read in `RuntimeActDialogue.snapshot`; `StateDialogue.titleOf` finds the real project fact keys — neither checked
- Source: `landings/PLAN-94.md`, `landings/PLAN-89.md` (second landing), `landings/PLAN-92.md`.
- Verify-first clause: before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/EntryFlow.java` — present at HEAD 159c310c
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/PmConsole.java` — present at HEAD 159c310c
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/WebCommand.java` — present at HEAD 159c310c
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/OperatorContext.java` — present at HEAD 159c310c
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ActionSupport.java` — present at HEAD 159c310c
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/RuntimeActDialogue.java` — present at HEAD 159c310c
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/dialogue/StateDialogue.java` — present at HEAD 159c310c
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/system/SystemLevel.java` — present at HEAD 159c310c
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/system/SystemView.java` — present at HEAD 159c310c
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/EntryFlowTest.java` — present at HEAD 159c310c
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/PmConsoleBehaviourTest.java` — present at HEAD 159c310c
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/SystemViewConsistencyTest.java` — present at HEAD 159c310c
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/WebDevicesCommandTest.java` — present at HEAD 159c310c
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ReadVerbExitCodesTest.java` — present at HEAD 159c310c
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/OperatorContextTerminalTest.java` — present at HEAD 159c310c
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ActionSupportTest.java` — present at HEAD 159c310c
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/system/SystemLevelTest.java` — present at HEAD 159c310c
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/DirectDialogueInterruptIT.java` — present at HEAD 159c310c
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/ViewDialogueInterruptIT.java` — present at HEAD 159c310c
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/OnboardingDialogueIT.java` — present at HEAD 159c310c
- OBSERVED: `pm-operator/src/test/resources/transcript/console-settings.transcript` — present at HEAD 159c310c
- OBSERVED: `pm-mock-runtime/src/main/resources/scenarios/onboarding.json` — present at HEAD 159c310c
- OBSERVED: `doc/design/terminal-client/04-console.adoc` — present at HEAD 159c310c

## Dependencies and Sequencing

- Depends on: `PLAN-94` (shipped), `PLAN-92` (shipped)
- Overlaps with: `PLAN-97` (staged; it removes and folds top-level verbs and rewrites their tests, and may rewrite `console-settings.transcript` when it removes `settings`: whichever of the two starts second reads the other's landing and rebases; deliverable 1 here is date-driven and should go first if the break date (2026-12-08) is near), `PLAN-30` (the console strings), `PLAN-79` (the `pm-operator` tests), `PLAN-95` and `PLAN-96` (`MockRuntimeEngine.java` only if the mock scenario rows change: deliverable 1 prefers the clock over a data change)
- Should land before (same files, per `corpus cross-check` on 2026-11-05): `PLAN-34`, `PLAN-36`, `PLAN-42`, `PLAN-44`, `PLAN-50`, `PLAN-79`, `PLAN-84` (all staged: read this plan's landing first)
- Adjacent to: none

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-98-cli-console-badges-residue.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
