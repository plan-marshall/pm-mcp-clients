# PLAN-171: Java: The pm-operator Tests on Generated Input, Finished Honestly

epic: ui-improvements
workstream: WS-04

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Staged on the operator's word of 2026-11-19 ("make it a small plan, land and emit") from `landings/PLAN-79.md` (the three-layer analysis of #502, `ac538733`, layer 1 L1-1 to L1-8, layer 3 L3-1 to L3-7). This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

PLAN-79 (#502) converted the `pm-operator` tests onto generated input without loosening an assertion, but it counted two things as converted that are not, left one pin that does not cover what it claims, accounted seven classes as input-free although they type incidental input, and left the three test classes of removed commands standing. This plan finishes that work and makes its numbers true: the completer pin covers `run()`, no class carries `@EnableGeneratorController` without drawing from `Generators`, the seven classes are converted or listed with a reason, the three old test classes are re-homed or deleted, and the generated inputs of the converted dialogue tests stay valid when a dialogue gains a choice. It changes tests; the one production change allowed is the seam of deliverable 1, and the landing message names it.

## Deliverables

Read first:
- `landings/PLAN-79.md` of this epic (layer 1, layer 2 L2-5, layer 3 L3-1 to L3-7). The points marked "re-read" were read at `ac538733` by the orchestrator and again at `00a53a1f` for this spec (the dead annotations, the `PmConsole.completer()` change, the three old tests); the others are the analysis leaf's reading and are re-checked at outline.
- `PLAN-79`'s spec (`plans/PLAN-79-cui-testing-operator.md`): the pilot rule (draw `Generators` values inside the test or a `@BeforeEach`, never in a field initialiser; a class whose values are the contract is input-free, not converted; contract values stay literal) and the sequencing clause (a test file that a running or emitted plan changes is left out and named).

1. **The completer pin covers `run()`**
   - What: `OperatorVerbsTest.consoleCompleterMatchesOperatorVerbs` (`OperatorVerbsTest.java:39-48`) calls the new helper `PmConsole.completer()` (`PmConsole.java:131-133`); `run()` calls it at `:155`. A revert of `run()` to a hand-built `new StringsCompleter(handList)` with the helper left in place still passes: the regression PLAN-118 item 2 named (L3-1). The pin is moved so that it fails when the completer the console actually uses is not the registered registry: the lane decides at outline the smallest seam that lets a test read the completer `run()` installed (for example the completer is obtained once and handed to the reader builder through one method the test can reach), and changes `PmConsole.java` no more than that; the change is named in the landing message with its reason.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
   - Done when:
     - The test is run once with `run()` reverted to a hand-built completer (the helper left in place) and fails; the failing run is recorded (evidence order: `mutation-after`).
     - `git diff` of `PmConsole.java` shows only the seam, and the landing message names it (evidence order: `none`).
2. **No class carries `@EnableGeneratorController` without drawing from `Generators`**
   - What: four classes carry the annotation and never call `Generators.`: `overview/OverviewFullScreenAppTest.java`, `ConfigSettingsTest.java`, `RuntimeActDialogueTest.java` and `dialogue/ChoicePromptTest.java` (re-read: one annotation each, no call). Each either draws its incidental input from `Generators` (and is then genuinely converted) or loses the annotation and is listed input-free with the reason. `OperatorVerbsTest` is counted as converted by PLAN-79 with neither: it is listed input-free or converted, not both. The before and after counts are restated truthfully in the landing message: annotated, annotated and generating, input-free with a reason, per group (unit classes, IT and transcript classes).
   - Persona: plan-marshall:persona-module-tester
   - Skills: pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
   - Done when:
     - Every class of `pm-operator/src/test` that carries `@EnableGeneratorController` contains a `Generators.` call, checked by a search that the lane records with its result (evidence order: `none`).
     - The landing message carries the restated counts and the input-free list with one reason per class (evidence order: `none`).
3. **The eight classes with hand-typed incidental input take their incidental input from `Generators`, or are listed with a reason**
   - What: `PmOperatorTest.java` (`web enable --lan --port 8443`, `:396-405`), `OperatorGateScopeTest.java` (`--message hello`, `--reason because`, `--description x`, `--pull-request 4711`, `:114-121,171`), `PmOperatorProcessIT.java` (`--reason archived`, project `my-service`, plan ids, `:447,488-502`), `CredentialsCommandTest.java` (secret `s3cret`, project `fix-relay`, `:246`), `RolesCommandTest.java` (`--project my-service`, `:148-211`), `ControlCommandTest.java` (own-stub ids, `:71-261`) and `DirectFileAccessTest.java` (port 7420 round trip, `:93-97`), and, added by PLAN-165 (#503), `EpicDecompositionDialogueKeysTest.java` (a typed unknown key `z`, scripted answers and ids; no annotation: re-read at `a8ac159d`) hold ids, names, ports, counts and free text that PLAN-79's own definition of incidental input covers. Each is converted by the pilot rule (the generated value is drawn in the test or a `@BeforeEach`, the expected value derived from it, every contract value, such as a catalogue text, a scenario id or a wire name, left literal) or listed input-free with the reason that the value IS the contract. **A file that a running or emitted plan changes is left out and named in the landing message**: `PLAN-168` declares `RolesCommandTest` and `CredentialsCommandTest` (deliverable 2 and the exit-code tests); the lane reads which plans are running at the start and leaves out what they own (PLAN-165 shipped on 2026-11-19 and owns nothing any more).
   - Persona: plan-marshall:persona-module-tester
   - Skills: pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
   - Done when:
     - For each converted class, a test that asserted the literal still asserts the same behaviour: the expected value is re-derived from the generated input and no assertion is loosened (a literal turned into `contains`, `isNotNull` or a removed assertion), read in the diff by the lane and stated in the landing message (evidence order: `none`).
     - For each converted class, the test is run once with the production behaviour it covers mutated (for example the port not put on the request) and fails; one failing run per class is recorded (evidence order: `mutation-after`).
     - `./mvnw -pl pm-operator verify` passes and the number of tests is not lower than before (evidence order: `none`).
4. **The three test classes of removed commands are re-homed or deleted**
   - What: `EnrolCommandTest.java` (10 tests; the test bodies run `project add`, `:97,124,139,150`), `StartCommandTest.java` (5 tests) and `OnboardCommandTest.java` (6 tests, nested `Bare` and `ConfigAspect`) are named after commands that no longer exist: `src/main` holds no `EnrolCommand`, `OnboardCommand` or `StartCommand` (re-read). PLAN-79's spec (re-grounding of 2026-11-10) says to delete or re-home them. Each test is moved into the test class of the command it exercises (the lane reads each test's first `execute(` call to find the command) and each class is deleted once empty; a test that duplicates one that already exists there is deleted with that fact stated. No test is silently dropped: the count of tests before and after is recorded with every deleted duplicate named.
   - Persona: plan-marshall:persona-module-tester
   - Skills: pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
   - Done when:
     - `git ls-files pm-operator/src/test` names none of the three classes (evidence order: `none`).
     - The number of tests after equals the number before minus the named duplicates, recorded (evidence order: `none`); `./mvnw -pl pm-operator verify` passes.
5. **The generated inputs of the converted dialogue tests stay valid when a dialogue changes**
   - What: (a) `transcript/RuntimeStopDialogueIT.java:60` draws an "unknown" answer from `integers(3, 9)`, which is unknown only while the dialogue has exactly two choices (L3-6); it derives the range from the dialogue's own choice count, or states its assumption in a test that fails when the choice count changes. (b) `transcript/PlanChatIT.java:42-44` generates the message and asserts one turn was posted but not that the message reached the wire (L3-5): it asserts the turn's request body carries the message. (c) `PmConsoleBehaviourTest.java:790-793` accepts any blocker that is not a `Thread` as "the lock" (L3-7): it asserts the blocker is the lock the console holds, or the finding is stated as not testable and the weaker check named.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-java:junit-integration, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
   - Done when:
     - Each of (a), (b), (c) is run once with its production behaviour mutated (a third choice added to the dialogue; the message dropped from the turn's body; the wait moved off the console's lock) and fails; the failing runs are recorded (evidence order: `mutation-after`).
     - `./mvnw -pl pm-operator verify -Pintegration-tests` passes and the screen-capture set is byte-identical before and after (evidence order: `none`).

Not in this plan, named so they are not lost (each stays in `epic.md` as an Open Defect): the two items of PLAN-79's spec that its lane did not do and did not mention, the parameterize-repeats fold (2026-11-14 item 2: three or more repeated tests become one `@ParameterizedTest`) and the #432 coverage finding (targeted branch tests with before and after coverage): each is its own piece of work with its own measurement; whether the German golden `console-settings-de.transcript` should hold English runtime-supplied summaries ("English, models English", "4 models on 2 harnesses", read from the mock, not checked); the squash commit text of #502 on `main` that carries CodeRabbit release notes the diff does not bear out (history is not rewritten).

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`). At the head these pass: `./mvnw -pl pm-operator verify` and `./mvnw -pl pm-operator verify -Pintegration-tests` (run as separate invocations); the quality gate `./mvnw verify -Ppre-commit` rewrites files, every rewrite is reviewed and a file this plan does not own is restored from `origin/main`.
- No new dependency or plugin. `pm-operator` does not log: no `LogMessages` class and no `LogAsserts` test is created.
- Test rules of `CLAUDE.md`: JUnit 5 only (`@DisplayName`, `@Nested`, AAA), no Mockito, input from `Generators` with `@EnableGeneratorController` on every class that takes input, the expected value of a contract stays literal, generated values are drawn in the test or a `@BeforeEach` and never in a field initialiser.
- **Behaviour is unchanged and a failing test is reported, never loosened** (PLAN-79's rule): a test that fails after conversion exposes a wrong assumption about a generated value or a real defect; the lane reports it. The one production change is the seam of deliverable 1.
- Generated values that are typed into a prompt, a screen capture or an argument are constrained so that they cannot collide with a choice key, an option name, a column width or a character the prompt treats specially (the rule PLAN-79's review taught: `AcceptEditDialogueIT` prefixes `x` against "yes", `PauseDialogueIT` draws 4..9).
- The screen-capture set under `pm-operator/src/test/resources` is byte-identical before and after.
- ZERO Sonar findings (the operator, 2026-11-05): 0 open new-code issues and 0 hotspots, read at the pull request head after the push (`unknown` for a failed read, never `0`); a dismissal is made only on the operator's selection. This is a merge gate.
- **The merge rule:**
  - At least one CodeRabbit review of some head, with its findings handled. If CodeRabbit refuses on quota, stop and ask the operator.
  - Every review thread replied and resolved, and every review body item (outside-diff, nitpick, additional comments) answered on the pull request, read from GitHub's thread state, never from a count kept in memory. Every reply answers its own finding.
  - The run merges the pull request itself once these gates hold (`CLAUDE.md`, Git Workflow). Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`. The pull request body carries no generator footer.
- The landing verdict is given per Done-when clause, never per deliverable, and the landing message reports `deliverables_done` as the number of deliverables whose every Done-when clause is met. The landing message lists the counts it states by the search that produced them; a number the lane did not recount is `unknown`.
- Pull request size: about 20 files; if the changed set passes 100 files, stop and ask the operator to split.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.
- The mailbox read refuses a plan id with a leading digit (`invalid_target_plan`): report it, never read it as no mail.

## Claim Labels

- OBSERVED: `OverviewFullScreenAppTest`, `ConfigSettingsTest`, `RuntimeActDialogueTest` and `ChoicePromptTest` each carry one `@EnableGeneratorController` and no `Generators.` call — read by `git show origin/main:<path> | grep -c` at `00a53a1f` (the paths: `pm-operator/src/test/java/de/planmarshall/operator/overview/OverviewFullScreenAppTest.java`, `.../ConfigSettingsTest.java`, `.../RuntimeActDialogueTest.java`, `.../dialogue/ChoicePromptTest.java`)
- OBSERVED: `PmConsole.completer()` is `new StringsCompleter(OperatorVerbs.completer())` and `run()` calls it at `:155`; the test calls the helper — read at `PmConsole.java:131-133,155` and `OperatorVerbsTest.java:39-48` at `ac538733`
- OBSERVED: `EnrolCommandTest` (227 lines, 10 tests), `StartCommandTest` (135, 5) and `OnboardCommandTest` (267, 6) exist, and `src/main` holds no `EnrolCommand.java`, `StartCommand.java` or `OnboardCommand.java` — read by `git ls-tree` at `00a53a1f`; the bodies of `EnrolCommandTest` call `project add` (`:97,124,139,150`)
- OBSERVED: `RuntimeStopDialogueIT` draws `Generators.integers(3, 9)` as the unknown answer — read at `transcript/RuntimeStopDialogueIT.java:60`; `PlanChatIT` draws the message at `:42-44`; the lock-wait check at `PmConsoleBehaviourTest.java:784-796` accepts a blocker that is not a `Thread`
- HYPOTHESIS: the seven classes of PLAN-79's landing take incidental input at the lines named — the analysis leaf read them; the orchestrator re-read none of these lines: confirm/refute at outline. The eighth, `EpicDecompositionDialogueKeysTest`, is OBSERVED: no annotation and no `Generators.` call at `a8ac159d` (re-read), typed input `z`, scripted answers and ids
- HYPOTHESIS: the bodies of `StartCommandTest` and `OnboardCommandTest` exercise verbs that exist (`start` as a nested command of `plan` and `runtime`, the config aspects) so each test has a home class — confirm/refute by the first `execute(` call of each test (verify-at-outline)
- HYPOTHESIS: `PmConsole.run()` can hand its completer to a seam a test reaches without a pty — confirm/refute at `PmConsole.java:140-175` (verify-at-outline; if no seam is small enough, the lane stops and reports instead of widening `PmConsole`)
- HYPOTHESIS: PLAN-168 (emitted 2026-11-19) and PLAN-165 (running) change `RolesCommandTest`, `CredentialsCommandTest` and `CleanupCommandTest`, so deliverable 3 leaves them out — read from their specs and the corpus cross-check at staging; the lane re-reads the running plans at the start
- Verify-first clause: before each deliverable, re-read the named lines at the then-current HEAD; PLAN-168 and PLAN-165 edit the same test tree. A refuted claim re-scopes the deliverable and is reported.

## Expected Surface

- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/PmConsole.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/OperatorVerbsTest.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/PmConsoleBehaviourTest.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/overview/OverviewFullScreenAppTest.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ConfigSettingsTest.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/RuntimeActDialogueTest.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/dialogue/ChoicePromptTest.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/PmOperatorTest.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/OperatorGateScopeTest.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/PmOperatorProcessIT.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/CredentialsCommandTest.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/RolesCommandTest.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ControlCommandTest.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/DirectFileAccessTest.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/EpicDecompositionDialogueKeysTest.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/EnrolCommandTest.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/StartCommandTest.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/OnboardCommandTest.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/RuntimeStopDialogueIT.java`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/PlanChatIT.java`

## Dependencies and Sequencing

- Depends on: `PLAN-79` (shipped, #502 `ac538733`).
- Overlaps with (read by `corpus cross-check` at staging, 2026-11-19; shipped, parked and superseded plans are not in the disjointness population): **no running plan** (PLAN-170 shares no file with it; PLAN-165 shipped). Staged: `PLAN-168` (emitted the same day; `CredentialsCommandTest.java` and `RolesCommandTest.java`, which deliverable 3 therefore leaves out and names), `PLAN-42` (`PmConsole.java`, `OperatorVerbsTest.java` and one more), `PLAN-44` (`PmConsole.java`, `PlanChatIT.java` and one more), `PLAN-48` (`PmOperatorProcessIT.java`): whichever lands second rebases and keeps both sides.
- Adjacent to: nothing.

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-171-operator-tests-generated-input-residue.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports its outcome through its PR and its inbox message. The inbox exception's qualifiers and the sole sanctioned write mechanism are stated in `persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
