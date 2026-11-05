# PLAN-97: Terminal: One Command Tree, the Drain Signal and Accept-Edit by Project

epic: ui-improvements
workstream: WS-04

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.
> Staged 2026-11-04 on the operator's word ("make it a plan, or add to an existing") for the three work packages that the design audit (`design-audit-002`, merged with PR #310, `28634b2a`) added to `doc/plans/` as `open`: `operator-drain-signal` (decision D25), `operator-command-tree` (D26) and `accept-edit-enrolment` (R7-7). All three are `pm-operator` work on the same command tree, so they are one plan with three deliverables. Their package files (`doc/plans/operator-drain-signal.adoc`, `operator-command-tree.adoc`, `accept-edit-enrolment.adoc`) hold the tasks and anchors; this spec carries the same content and the ledger's evidence orders. Keep the pull request under 100 files (CodeRabbit refuses more): deliverable 3 removes and folds verbs and rewrites their tests, so it may exceed it; if the count at outline says so, split deliverable 3 into its own pull request and say so in the progress record.

## Objective

Bring `pm-operator` to the command tree of the design (`doc/design/terminal-client/01-commands.adoc#command-tree`): `drain` becomes the signal the specification describes and a drain is withdrawn only as `runtime stop --withdraw`; `accept-edit` takes the forms of the specification (`accept-edit enrolment <project_id>`) with the mock runtime serving the refusal that names it; and every top-level verb the design does not have is folded into its design verb or removed. Also close the two documents' open points that the audit found: the `operator.key.*` keys live in no message pack, and `pm-operator enrol` is in the specification but not in the design.

## Deliverables

1. **`drain` is the signal; a drain is withdrawn as `runtime stop --withdraw`** (package `operator-drain-signal`, D25)
   - What: `pm-operator drain` signals the runtime process recorded in `state/runtime.json`, does nothing when none is recorded (exit `0`), and waits at most the bound defined with the drain timeout; it sends no request to the runtime's API and asks nothing (`shared/system.adoc`: drain asks nothing). `--withdraw` leaves `drain` (a usage error, exit `2`); `runtime stop --withdraw` withdraws a drain the operator asked with `DELETE /api/v1/operator/runtime/stop`, and its refusals print the design's lines (`operator.system.signal_drain` for a drain of a signal, `system.stopping.too_late` when it is too late). Anchors: CLI, Coordination & Security § Without the Runtime (`cli-without-runtime`); Timeouts & Budgets § Drain Timeout (`drain-timeout`, in `pm-mcp-core`); Runtime Lifecycle § Drain, Stop & Upgrade (`withdraw-drain`); `doc/design/terminal-client/25-system.adoc` (`#t-stop`) and `01-commands.adoc`. Not here: the server answers the withdraw `DELETE` with 405 (an item of `plan-marshall-mcp`): the terminal's tests run against the mock runtime.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:javadoc, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-dev-java-cui:cui-logging
   - Done when:
     - `DrainCommandTest` with a fixture process recorded in `state/runtime.json`: the signal reaches it, the command returns once it exits and at the bound when it does not; no record means no signal and exit `0`; no HTTP request is sent; `--withdraw` is a usage error (exit `2`) — each fails before (evidence order: `test-first`)
     - The terminal's tests against the mock runtime withdraw an operator's drain through `runtime stop --withdraw`, are refused for a drain of a signal and too late, with the design's lines (evidence order: `test-first`)
     - Terminal check (screen capture) of the `runtime stop --withdraw` refusals at 120 × 30 and 80 × 24, `en_US.UTF-8` and `de_DE.UTF-8`, before and after (evidence order: `none`)
2. **`accept-edit enrolment <project_id>`, and the refusal that names it** (package `accept-edit-enrolment`, R7-7)
   - What: `accept-edit` takes the forms of the specification, `enrolment <project_id>` among them, in place of `<store path> --project <id>`; the act shows the file and asks the confirmation before it sends the acceptance. The mock runtime serves an `enrolment_tampered` refusal (`ApiErrorCode`, `error.*.properties` exist in `pm-api`) that names `pm-operator accept-edit enrolment <project_id>`, and serves its acceptance (`AcceptEditRequest`, `AcceptEditResult`, `StoreDigestComparison` exist). Anchors: CLI, Coordination & Security § Accept-Edit (`operator-accept-edit`); Concepts § Enrolment (`rebaseline`, `check`); Store Schemas § Project Enrolment Store (`enrolment-store`).
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:javadoc, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
   - Done when:
     - `AcceptEditCommandTest`: `accept-edit enrolment <project_id>` sends the acceptance only after the confirmation; the old form `<store path> --project <id>` is a usage error (exit `2`) — fails before (evidence order: `test-first`)
     - The terminal's tests against the mock runtime run from the `enrolment_tampered` refusal through the named command to the project served again (evidence order: `test-first`)
     - A Ctrl+C or an end of input at the confirmation sends nothing (the rules of `landings/PLAN-92.md`: an interrupt leaves with `130`; an end of input or a failed read never confirms) (evidence order: `test-first`)
3. **`pm-operator` registers the design's command tree and nothing else** (package `operator-command-tree`, D26)
   - What: (a) The verbs `PmOperator` registers (`PmOperator.java` ~36-65: `StartCommand`, `OnboardCommand`, `AbortCommand`, `UnenrolCommand`, `EnrolCommand`, `SettingsCommand`, `RecipeCommand`, `LessonSourceCommand`, `GitSourceCommand`, `QuestionsCommand`, `AnswerCommand`, `ClientCommand` among them) are those of the design's command tree with their options; each other top-level verb is folded into its design verb and removed: `start` into `plan add` and `epic add`, `onboard` into `pm-operator` without a verb and `config <aspect>`, `unenrol` into `project unenrol`, `abort` into `plan cancel|abandon` and `epic cancel`. (b) A registered verb with no counterpart in the design or the specification is decided by the operator (2026-11-04, "agreed" to the orchestrator's proposals); the design is changed in the same pull request where a verb is kept or folded. DECIDED: `enrol` is folded into `project add [<path>]` (the same act: onboard a project from the working directory) and removed; `--pilot` moves to the `project add` line of the design; the specification names `enrol-harness`, not `enrol`, so no specification change. `settings` is removed: `config`, `config show|set|unset` and `config documents --mode copy|move|ask` cover it (`settings machine --set documents_mode=…` is `config documents --mode …`). `git-source` (`adopt-pr`) is folded into `plan add --pull-request <n>` (the design's source list has it); at outline check that this source reaches the adopt lane, and where the capture of local changes has no counterpart the design gets a line first. `lesson check` (UC-STA-8, verify a lesson's premise before a plan starts from it) is KEPT and the design gains its line (`18-starting` and the command tree); the specification names only `lesson release`. `plans`, `questions`, `answer` and `plan <id>` are KEPT as direct shell verbs (PLAN-91 and PLAN-92 built the interrupt handling and verb tests on them; scripts use them with `--no-input`): the design's "In the console only" block changes to "in the console and as direct verbs". `recipe run` is in the design: nothing to decide. `client` is named by the specification (`client install|update|remove|status`) and stays; the design's "other operator verbs" line gains it; the design's `harness` naming is the open package `harness-names-in-code`, NOT this plan. A verb found at outline that is not in this list is reported in the pull request and kept only by the operator's decision. (c) `pm-operator --help` and the console's `help` list the design's verbs only. (d) The open points of the audit: the `operator.key.*` keys are in no message pack — put them in the `pm-api` packs (en and de) or say why they are not texts; `pm-operator enrol` is in neither the specification nor the design (the specification names `enrol-harness <client>`; the first draft of this package was wrong) — decided in (b): folded into `project add`. Anchors: `doc/design/terminal-client/01-commands.adoc#command-tree`, `19-actions.adoc`; CLI, Coordination & Security § Command Tree (`command-tree`), § Operator Channel Isolation (`operator-channel-isolation`).
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:javadoc, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java:junit-integration, pm-dev-java-cui:cui-testing, pm-dev-java-cui:cui-logging
   - Done when:
     - `PmOperatorTest` asserts the registered top-level verbs against the list of the design, and that each removed verb is a usage error (exit `2`); the tests of the folded verbs (`StartCommandTest`, `OnboardCommandTest`, `AbortCommandTest`, the unenrol cases) run through their design verbs — fails before (evidence order: `test-first`)
     - The decisions of (b) are in the pull request per verb (folded: `enrol`, `settings`, `git-source`; kept with a design line: `lesson check`, `plans`, `questions`, `answer`, `client`); `PmOperatorTest` follows them and the design lines are in the same pull request (evidence order: `none`)
     - The transcript test of `help` lists the design's verbs only (evidence order: `test-first`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is fixed or dismissed with a stated reason (evidence order: `none`)

4. **The mock's drain, locks and switch** (PLAN-95 residue, `landings/PLAN-95.md`)
   - What: (a) Lock order in `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java`: `handlePostStopRuntime` (~8511-8571) and `handleDeleteStopRuntime` (~8611-8614) hold `systemLock` and then take the engine monitor (`cancelPendingTasks()`, `scheduleDrainShrink`), while `goToState`, `reset` and `setScenario` hold the engine monitor and take `systemLock` through `initDefaultSystem` (~7325): one order everywhere (CodeRabbit thread `60EE-P` of PR 327, resolved while the bot kept it open). (b) The generation guard in `syncDrainItems` (~7421) reads `drain_generation`, which no producer sets: emit it when a drain starts and reject a stale event in `broadcastEvent` before it is buffered (thread `60EE9o`), or drop the guard and say why the lifecycle check suffices. (c) `cancelPendingTasks()` (~8525, ~8614) cancels all of the scenario's tasks, including a state's own events and `auto` transition: cancel only the drain's own tasks. (d) Decide what "ends" means for a drain: the four-item drain empties the list but `lifecycleState` stays DRAINING (`shrinkDrain` ~8590-8606): either the last step leaves DRAINING (and the design says to what) or the design says an empty drain is still draining. (e) The switch is atomic for `facts.kept` and `facts.residue`: validate before `currentScenario` and the stores change (`applyCleanupFacts` ~7491-7499; thread `60EE8V`). (f) `serveNeedsOperatorPage`: do it or answer with the reason (PLAN-95 D3 clause, never answered). (g) The follow-up of review `5784893187`: `goToState` tells live clients that an operator-created decision was removed, or the reason it need not. (h) The tests: the withdrawal test gets a post-withdrawal boundary (thread `60EE-Z`), the drain tests drive `shrinkDrain()` instead of the real clock, new tests take input from `Generators`, `MockRuntimeEngineTest` (~1019-1024) seeds an act before the rejected switch, (~1149-1157) counts completed finds, `DialogueSourceTest:96` uses `equals`. (i) Each of the three threads resolved while open (`60EE-P`, `60EE9o`, `60EE-Z`) gets a reply with the commit; open the follow-up issues the bot offered on the deferred threads, or record why not.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
   - Done when:
     - Tests that fail before: a stop racing a reset does not deadlock (bounded wait); a stale `runtime.draining` is rejected by its generation; a state's own scheduled event survives a drain start; a four-item drain ends as (d) decides; a malformed `facts.kept` leaves the previous scenario untouched (evidence order: `test-first`)
     - The thread replies and the issues of (i) are listed in the pull request (evidence order: `none`)

5. **The generic catches of `EpicCommand` and `PlanCommand`** (PLAN-103 residue, `landings/PLAN-103.md`)
   - What: `catch (Exception` at `EpicCommand.java:108` and `PlanCommand.java:146`, `:178`, and the `/*TODO: Catch specific not Exception …*/` markers of the rewrite recipe on them, become the exceptions the code can throw (CLAUDE.md). This plan rewrites these two files for the command tree; the catches go with it.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-null-safety, pm-dev-java:junit-core
   - Done when:
     - `git grep -n "catch (Exception\|TODO: Catch specific" -- pm-operator/src/main/java/de/planmarshall/operator/EpicCommand.java pm-operator/src/main/java/de/planmarshall/operator/PlanCommand.java` names none, and a test per changed catch fails when the catch is reverted to a generic one that swallows its path (evidence order: `mutation-after`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md): `./mvnw -pl pm-operator -am verify -Pintegration-tests` passes at the head (`-am` is needed or the module resolves a stale `pm-api` snapshot).
- No new dependency or plugin.
- CUI standards (`CLAUDE.md`): a test that takes input data draws it from `Generators` with `@EnableGeneratorController` (the expected value of a contract stays literal); a log call at INFO or above goes through a `LogRecord` of the module's `LogMessages` class with a `LogAsserts` test; `@NullMarked` on every package of `pm-operator`; JUnit 5 only, no Mockito, no Hamcrest; Java 25, Lombok, records; no generic `Exception`/`RuntimeException` in production code.
- Every operator text goes through `MessageCatalogue` into `operator.en.properties` and `operator.de.properties`.
- A dialogue or handler this plan adds or folds follows the interrupt rules of PLAN-91 and PLAN-92 (`landings/PLAN-92.md`): Ctrl+C leaves with `130` and sends no act; an end of input or a failed read at a confirmation never confirms.
- Ports of this plan: mock web port `20970`, app port `20971`. Other plans may run at the same time: never stop or reuse a process this worktree did not start.
- Terminal check of the rendered output through the screen capture harness of `cli-pty-dialogue-harness` (`ScreenCaptureIT`, command line in `doc/design/terminal-client/27-tui-testing.adoc`): captures to `pm-operator/src/test/resources/screen-captures/{plan}.txt`, the set run BEFORE the change and copied to `.plan/temp/terminal/{plan}/before/`, AFTER to `.../after/`, at 120 × 30 and 80 × 24 in `en_US.UTF-8` and `de_DE.UTF-8`.
- The design document is updated in the same pull request as the code (UX standing rule of the epic); the package rows in `doc/plans/README.adoc` are set `done` in the same commits (`CLAUDE.md` § Work Packages), each by the deliverable that finishes it.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`) — treat it as `sonar: configured`. An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason.
- Merge rule (the operator, 2026-11-02): at least one CodeRabbit review of some head with its findings handled; later heads need no review, re-trigger once and never wait. Handle the findings of EVERY CodeRabbit review that exists, a late one included, and read the review BODIES as well as the threads.

## Claim Labels

- OBSERVED: `PmOperator` registers 29 subcommands including `StartCommand`, `OnboardCommand`, `AbortCommand`, `UnenrolCommand`, `EnrolCommand`, `SettingsCommand`, `RecipeCommand`, `LessonSourceCommand`, `GitSourceCommand`, `QuestionsCommand`, `AnswerCommand` — read by the orchestrator at `c7d9dcb8` (`PmOperator.java` ~36-65)
- OBSERVED: `AcceptEditCommand`, `DrainCommand`, their tests, and the pm-api records `AcceptEditRequest`, `AcceptEditResult`, `StoreDigestComparison` exist; `enrolment_tampered` exists in `ApiErrorCode`, the `error` packs and `ApiContractTest`; the mock has a `/stores/accept-edit` route (`MockRuntimeEngine` ~332) and NO `enrolment_tampered` — read by the orchestrator at `c7d9dcb8`
- OBSERVED: `DrainCommand` today POSTs `/api/v1/runtime/stop` and has `--withdraw`/`-w` sending `DELETE /api/v1/runtime/stop`; `AcceptEditCommand` takes `<store-path>` and `--project` — read by the orchestrator at `c7d9dcb8`
- HYPOTHESIS: the mock needs an enrolment store route (digest and acceptance for `enrolment <project_id>`) beside the store route it has — confirm in `MockRuntimeEngine` and the specification's Accept-Edit section
- HYPOTHESIS: deliverable 3 touches more than 100 files with its tests — count at outline; split if so
- Source: `design-audit-002` (archived), `doc/plans/operator-drain-signal.adoc`, `operator-command-tree.adoc`, `accept-edit-enrolment.adoc` at `28634b2a`; `landings/PLAN-92.md` for the interrupt rules.
- Verify-first clause: before each deliverable, re-read the named files at the current HEAD and the design sections it follows, and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/PmOperator.java` — present at HEAD c7d9dcb8
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/DrainCommand.java` — present at HEAD c7d9dcb8
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/AcceptEditCommand.java` — present at HEAD c7d9dcb8
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/RuntimeCommand.java` — present at HEAD c7d9dcb8
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/StartCommand.java` — present at HEAD c7d9dcb8
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/OnboardCommand.java` — present at HEAD c7d9dcb8
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/AbortCommand.java` — present at HEAD c7d9dcb8
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/UnenrolCommand.java` — present at HEAD c7d9dcb8
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/EnrolCommand.java` — present at HEAD c7d9dcb8
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/PlanCommand.java` — present at HEAD c7d9dcb8
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/EpicCommand.java` — present at HEAD c7d9dcb8
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ProjectCommand.java` — present at HEAD c7d9dcb8
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ConfigCommand.java` — present at HEAD c7d9dcb8
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/PmOperatorTest.java` — present at HEAD c7d9dcb8
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/DrainCommandTest.java` — present at HEAD c7d9dcb8
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/AcceptEditCommandTest.java` — present at HEAD c7d9dcb8
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/StartCommandTest.java` — present at HEAD c7d9dcb8
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/OnboardCommandTest.java` — present at HEAD c7d9dcb8
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/AbortCommandTest.java` — present at HEAD c7d9dcb8
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/EnrolCommandTest.java` — present at HEAD c7d9dcb8
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` — present at HEAD c7d9dcb8
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/resource/ApiErrorCode.java` — present at HEAD c7d9dcb8
- OBSERVED: `doc/design/terminal-client/01-commands.adoc` — present at HEAD c7d9dcb8
- OBSERVED: `doc/design/terminal-client/25-system.adoc` — present at HEAD c7d9dcb8
- OBSERVED: `doc/design/terminal-client/19-actions.adoc` — present at HEAD c7d9dcb8
- OBSERVED: `doc/plans/README.adoc` — present at HEAD c7d9dcb8

## Dependencies and Sequencing

- Depends on: the packages `cli-system`, `command-tree` and `operator-enrolment-verbs` (all `done` in `doc/plans/README.adoc`); `PLAN-91` and `PLAN-92` (shipped); `PLAN-95`, `PLAN-96`, `PLAN-98` (shipped 2026-11-05: deliverable 4 is PLAN-95's residue; read `landings/PLAN-95.md` and `landings/PLAN-98.md`). `PLAN-103` shipped 2026-11-05 (read `landings/PLAN-103.md`); NEW overlap `PLAN-104` (staged 2026-11-05; `console-settings.transcript` and the console files: whichever starts second reads the other's landing and rebases; `EpicCommand`/`PlanCommand` belong to this plan alone)
- Overlaps with: `PLAN-98` (staged 2026-11-05; the console badges, `WebCommand`, `PmConsole`, `EntryFlow` and the `console-settings.transcript` golden: deliverable 3 here may rewrite that golden when `settings` is removed; whichever plan starts second reads the other's landing and rebases), `PLAN-94` (shipped 2026-11-05 as PR #321, read `landings/PLAN-94.md`; `OnboardCommand`, `WebCommand`, the console flow and the handler tests — start this plan after PLAN-94 lands and read its landing first), `PLAN-95` (`MockRuntimeEngine.java`; deliverable 2's mock edit: sequence one after the other), `PLAN-96` (running; `MockRuntimeEngine.java` for the drain list — emit this plan after PLAN-96 lands), `PLAN-21`, `PLAN-23`, `PLAN-30`, `PLAN-36`, `PLAN-42`, `PLAN-84` (name `StartCommand`, `OnboardCommand`, `PlanCommand`, `PmConsole` or the operator catalogue pair)
- Unblocks: `PLAN-42` (help lists every verb: waits for deliverable 3), `PLAN-79` (the `pm-operator` tests: rewrites the tests of Drain, AcceptEdit, Start, Onboard and Enrol after this plan), `PLAN-30` (drops its table edits and moves the `OnboardCommand` strings after deliverable 3)

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-97-cli-command-tree-drain-and-accept-edit.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
