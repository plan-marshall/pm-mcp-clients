# PLAN-116: CLI: What the Console Third Residue Plan Left Open

epic: ui-improvements
workstream: WS-04

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane (operator rule 2026-11-07).
> This spec is SELF-SUFFICIENT: the prompt below is a one-line pointer and carries no brief.
> Staged 2026-11-07 from the landing analysis of PLAN-104 (`landings/PLAN-104.md`: PR #357 merged with 0 Sonar issues but
> one unresolved thread, an unhandled last review, seven unanswered nitpicks and five unanswered PR 341 body items).
> One pull request on purpose. Keep it under 60 files. **Starts after PLAN-97 has landed** (both rewrite `pm-operator` files).

## Objective

PLAN-104 gave the console an addressing by `continue_link`, a bounded refresh with an error path and no `ForTest` surface, but
the last CodeRabbit review of its pull request (`5806743402`) was never answered, seven nitpicks of review `5804837268` and
the five body items of PR 341 have no reply on GitHub, the dialogue still has two step-count sources, `StateDialogueTest`
copies its expectation from production, and two tests prove less than they claim. This plan closes them so every review item
has an answer where the reviewer reads it and the tests can fail.

## Deliverables

1. **Every open review item answered where the reviewer reads it**
   - What: (a) PR 357: the unresolved thread `EntryFlowStaticsTest.java:116` (wait for the worker to record its interrupt
     flag: fix the race, reply in the thread, resolve it); the outside-diff item of review `5806743402`, `PmConsole.java:288`:
     `ProjectSelector.rewrite`/`asksForProject` (~168) treats a positional entry id that starts `--project` as a project
     selector: pass the entry id so it cannot be read as an option and add the test; the seven body nitpicks of review
     `5804837268` (`EntryFlowStaticsTest` ~58 assert the worker cause; `StateDialogueTest` ~244; `OnboardingCatalogueTest`
     ~120 more than `total=7`; `QuestionsCommandTest` ~382 a test of an unchecked exception that the old broad catch would
     have swallowed; `PmConsoleBehaviourTest` ~451 a successful continuation, not only the 404; `ConsoleBadgeStallIT` 91-97
     assert the proxy held the read; `ConsoleSettingsTranscriptTest` ~70 per-device ages): do or decline each with a reason
     and a reply on the review in the pull request comment thread. (b) PR 341: the five body items of reviews `5791531265`
     and `5792262598` (`EntryFlow` 657-660 follow-up, `StateDialogue` 47-51 follow-up, nitpicks `EntryFlowTest` ~589-590,
     `PmConsoleBehaviourTest` ~420-421, `StateDialogueTest` 245): one reply each on PR 341 with the commit of PLAN-104 or
     this plan that answers it. (c) PR 332: the reply `4495865723` in the `EntryFlow.java:712` thread says the checkout name
     stays the entry address; `eb1de3b0` removed it: reply with the true state.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
   - Done when:
     - The pull request lists every thread and body item it answers, with the commit or the reason, and the replies exist on
       GitHub (list them through the REST/GraphQL reads for PRs 357, 341 and 332) (evidence order: `none`)
     - Tests that fail before: an entry id starting `--project` reaches `project setup --entry` unaltered; the worker-cause
       assertion and the interrupt-race test fail when the code they cover is reverted (evidence order: `test-first`)
2. **One step total in the dialogue, expectations from the contract**
   - What: `OnboardingSource.java:38` `TOTAL_STEPS` is `OnboardingAspect.values().length` (a static that bypasses the aspects
     injected into `EntryFlow`); `ProjectOnboardingSource.java:37` `TOTAL_STEPS = 10` is a literal while `BeingAddedProject`
     reports `totalSteps`. Both take the total from the aspect list / the reported total. `StateDialogueTest` derives its
     expectation from the contract (the onboarding catalogue steps), not from the production map `PROJECT_STEP_BY_STATE`, so a
     dropped state fails it. `ConsoleBadgeStallIT` waits on the signal that the proxy holds the read (a latch the proxy
     releases) before it asserts the stalled line; the `LockSupport.parkNanos` poll with a 500 ms deadline at
     `PmConsoleBehaviourTest` ~689 becomes a condition wait; the deadline test (~426) widens its 1 s margin.
   - Persona: plan-marshall:persona-module-tester
   - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
   - Done when:
     - A test that fails before: a dialogue over an aspect list of another size, and over a being-added run with another
       `totalSteps`, prints that total; dropping one state from the production map fails `StateDialogueTest` (evidence order:
       `test-first`)
     - `git grep -n "TOTAL_STEPS = \|values().length" -- pm-operator/src/main` names no step total outside the aspect source
       (evidence order: `none`)
3. **The formatting gate on the final head, and the facts the landing message must carry**
   - What: `ConsoleBadge.java:27-31` record components at column 0 and `PmConsole.java` ~235 `@Nullable` on its own line:
     run `./mvnw -pl pm-api,pm-operator verify -Ppre-commit` once on the FINAL head and commit what it rewrites (review each
     diff); the landing message ends with the lines `unresolved_review_threads=N` (GraphQL `reviewThreads`, for the own pull
     request and for 357, 341, 332), `review_body_findings_total/handled` and `sonar_open_new_issues=N`.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core
   - Done when:
     - A second gate run rewrites nothing (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown`
       for a failed read, never `0`); it is a confirmed `0` (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md): `./mvnw -pl pm-api,pm-operator verify -Ppre-commit` (the reactor includes
  `pm-api`) and `./mvnw -pl pm-api,pm-operator verify -Pintegration-tests` pass at the head.
- No new dependency or plugin. Code standards (CLAUDE.md): no generic `Exception`/`RuntimeException` catch or throw in
  production code, `@NullMarked`, JUnit 5 only, `Generators` with `@EnableGeneratorController`, no arbitrary timer wait.
- Design document updated in the same pull request where a deliverable changes behaviour (`04-console.adoc` for the
  `--project` entry id, if the addressing text changes).
- Ports of this plan: `21160` and `21161` where a deliverable starts a process; never stop a process this worktree did not
  start.
- Every `test-first` clause cites a FAILING run and a PASSING run; every `mutation-after` clause the reverted code.
- ZERO Sonar findings (the operator, 2026-11-05): `sonar: configured` (project `plan-marshall_pm-mcp-clients`); 0 open
  new-code issues at the merge head, every issue fixed in a commit of this pull request, none dismissed without the
  operator's word; state `sonar_open_new_issues=N`.
- Merge rule (the operator, 2026-11-02): at least one CodeRabbit review of some head with its findings handled; handle the
  findings of EVERY review that exists, a late one included, and read the review BODIES as well as the threads. Reply IN the
  thread. Before you hand off, list the unresolved threads of your own pull request and state `unresolved_review_threads=N`.
  The pull request is merged only on the operator's word; commits end with `Co-Authored-By: plan-marshall
  <noreply@cuioss.de>`; the pull request body carries no generator footer.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.

## Claim Labels

- OBSERVED: the unresolved thread `EntryFlowStaticsTest.java:116`, the reviews `5804837268`, `5805509142`, `5806743402` of PR
  357, the missing replies on PR 341 and the stale reply `4495865723` — read by the orchestrator through GraphQL and REST on
  2026-11-07
- HYPOTHESIS: the three "unclear" nitpicks (`QuestionsCommandTest` ~382, `PmConsoleBehaviourTest` ~451,
  `ConsoleSettingsTranscriptTest` ~70) are undone, `OnboardingSource.TOTAL_STEPS` and `ProjectOnboardingSource.TOTAL_STEPS`
  are as described, `ProjectSelector.asksForProject` misreads an entry id starting `--project` — by a read-only agent's
  reading of `7902d577`, not run; confirm/refute at the files named
- Source: `landings/PLAN-104.md`, `landings/PLAN-103.md`.
- Verify-first clause: before each deliverable, re-read the named files at the current HEAD (PLAN-97 lands first) and confirm
  or refute every HYPOTHESIS; a refuted cause re-scopes the deliverable and is reported.

## Expected Surface

- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/PmConsole.java` — present at HEAD 14a6f7c0
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ProjectSelector.java` — present at HEAD 14a6f7c0
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ConsoleBadge.java` — present at HEAD 14a6f7c0
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/dialogue/OnboardingSource.java` — present at HEAD 14a6f7c0
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/dialogue/ProjectOnboardingSource.java` — present at HEAD 14a6f7c0
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/dialogue/StateDialogue.java` — present at HEAD 14a6f7c0
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/EntryFlowStaticsTest.java` — present at HEAD 14a6f7c0
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/PmConsoleBehaviourTest.java` — present at HEAD 14a6f7c0
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ProjectSelectorTest.java` — present at HEAD 14a6f7c0
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/QuestionsCommandTest.java` — present at HEAD 14a6f7c0
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/dialogue/StateDialogueTest.java` — present at HEAD 14a6f7c0
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/dialogue/OnboardingCatalogueTest.java` — present at HEAD 14a6f7c0
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/ConsoleBadgeStallIT.java` — present at HEAD 14a6f7c0
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/ConsoleSettingsTranscriptTest.java` — present at HEAD 14a6f7c0
- OBSERVED: `doc/design/terminal-client/04-console.adoc` — present at HEAD 14a6f7c0

## Dependencies and Sequencing

- Depends on: `PLAN-104` (shipped), `PLAN-97` (starts after it landed: `PmOperator` and the transcript tests are rewritten there)
- Overlaps with: `PLAN-97` (sequence), PLAN-21, 23, 30, 36, 42, 44, 50, 79, 84 (same console and dialogue files; they start
  after this plan)
- Should land before: PLAN-21, 23, 30, 36, 42, 44, 50, 79, 84
- Adjacent to: none

## Amendment 2026-11-07 (PLAN-108 landing)

The race of the unresolved PR 357 thread `EntryFlowStaticsTest.java:116` is already fixed by PR 362 (`629e33ce`: a `workerLeft` latch is awaited before the assertions; the line numbers of this file moved by about 5): deliverable 1(a) reduces to the reply in the thread citing `629e33ce` and resolving it; the nitpick `EntryFlowStaticsTest` ~58 (assert the worker cause) stays. `ActionSupportTest` now repeats Ctrl+C every 20 ms until the dialogue leaves and 309 `nonEmptyStrings` calls became `nonBlankStrings` across `pm-api` tests (use `nonBlankStrings`). Add a note to the pull request on a possible product window: `StartPrompt.ask`/`askRaw`/`askInline` (~123-146) and `ChoicePrompt.renderFrame()` print the question before `readLine` starts and `pm-operator/src/main` has no `Signal.INT` handler, so a Ctrl+C in that gap takes the JVM default (exit 130) instead of leaving the dialogue (HYPOTHESIS, microseconds wide; confirm or refute at those lines, never fix inside this plan unless a test reproduces it).

## Hand-Off Command

Lane: `runbook` (run with opencode) — a small plan (operator rule 2026-11-07: small plans go to the runbook lane, design work to direct Claude Code sessions). Start it after `PLAN-97` has landed. 

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-116-cli-console-fourth-residue.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source, tests and design documents. It creates and edits
NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message — the orchestrator owns every other
ledger write — and reports its outcome through its PR and its inbox message. The inbox exception's qualifiers and the sole
sanctioned write mechanism are stated in `persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
