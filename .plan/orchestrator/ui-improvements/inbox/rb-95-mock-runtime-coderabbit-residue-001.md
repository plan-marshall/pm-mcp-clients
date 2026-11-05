envelope_version=1
sender_type=plan
sender_id=rb-95-mock-runtime-coderabbit-residue
epic=ui-improvements
kind=landing
created=2026-11-05T15:47:17Z

```landing-facts
schema=landing-facts/1
plan_id=rb-95-mock-runtime-coderabbit-residue
epic=ui-improvements
pr=#327
merge_state=merged
cleanup_owed=false
deliverables_total=7
deliverables_done=7
total_tokens=unknown
harness=antigravity
model=unknown
effort=unknown
plugin_version=0.1.0
executor_version=0.1.2036
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
step.sonar-roundtrip.open_issue_count=0
step.sonar-roundtrip.open_issue_rules=none
```

guard: none on this harness (unenforced)

# Landing Report: PLAN-95 (Mock Runtime: the CodeRabbit Findings Nobody Handled)

Merge commit: `9bf4b93606b078fa8a5849844dd3714b3169e584`
Pull Request: #327
Merge gate: `sonar_open_new_issues=0`, `unresolved_review_threads=0`

## 1. Deliverables

- **Deliverable 1: Reset and scenario switch clear every store the engine fills**
  - Done-when: One test per store that fails before: after a reset and after a scenario switch the old content is gone; a plan made through the `create` act answers `/process` with 200.
  - Status: met
  - Tests:
    - `resetAndScenarioSwitchClearStartedPlans`: order `test-first`, failed at `dcc3ef4a` (`maven-2026-11-05-081316.log`), passed at `24b3f359`
    - `resetAndScenarioSwitchClearProjectSetupStates`: order `test-first`, failed at `dcc3ef4a` (`maven-2026-11-05-081338.log`), passed at `24b3f359`
    - `planCreatedThroughStartEntryCreateActAnswersProcess`: order `test-first`, failed at `dcc3ef4a` (`maven-2026-11-05-081508.log`), passed at `24b3f359`
  - Commit: `24b3f359`

- **Deliverable 2: A scenario switch is atomic, and its facts are validated first**
  - Done-when: Tests that fail before: a scenario with a malformed start-state fact is refused with the right error and leaves the engine exactly as it was; a non-object `job` or `durations` fact is rejected, not skipped; a repeated cleanup delete returns the adjusted `freed_bytes`; the system snapshot reflects the live cleanup job. Decision on `broadcastReset` recorded with reason and test.
  - Status: met
  - Tests:
    - `scenarioWithMalformedStartStateFactIsRefused`: order `test-first`, failed at `24b3f359` (`maven-2026-11-05-082823.log`), passed at `859f1a1b`
    - `nonObjectJobOrDurationsFactIsRejected`: order `test-first`, failed at `24b3f359` (`maven-2026-11-05-082823.log`), passed at `859f1a1b`
    - `repeatedCleanupDeleteReturnsAdjustedFreedBytes`: order `test-first`, failed at `24b3f359` (`maven-2026-11-05-082823.log`), passed at `859f1a1b`
    - `systemSnapshotReflectsLiveCleanupJob`: order `test-first`, failed at `24b3f359` (`maven-2026-11-05-082823.log`), passed at `859f1a1b`
    - `jumpEmitsNoResetAndPreservesEventOrder`: order `none`, pinned at `859f1a1b`
  - Decision: `broadcastReset` is not called after `goToState`. Reason: `broadcastReset` clears every ring buffer and sends a `reset` event to all open streams, designed for scenario switches where previous envelopes must not replay; a state jump stays within one scenario and scheduled transitions must retain event ordering for clients reconnecting with `Last-Event-ID`. Commented at `MockRuntimeEngine.java:1211`.
  - Commit: `859f1a1b`

- **Deliverable 3: The find and needs handlers read under their locks and return what the contract says**
  - Done-when: Tests that fail before: a concurrent find during an enrol does not throw; a decision found by find carries the plan id as its location; an unenrolled project is not found; a started epic is found. `NeedsAndFindScenarioTest` asserts find match order for FIX-RELAY and fix-relay-v1. Decision on `NeedsOperatorItem` id documented in design.
  - Status: met
  - Tests:
    - `decisionFoundByFindCarriesPlanIdAsLocation`: order `test-first`, failed at `859f1a1b` (`maven-2026-11-05-083936.log`), passed at `f535ca06`
    - `unenrolledProjectIsNotFound`: order `test-first`, failed at `859f1a1b` (`maven-2026-11-05-083936.log`), passed at `f535ca06`
    - `startedEpicIsFound`: order `test-first`, failed at `859f1a1b` (`maven-2026-11-05-083936.log`), passed at `f535ca06`
    - `concurrentFindDuringEnrolDoesNotThrow`: order `test-first`, failed at `859f1a1b` (`maven-2026-11-05-083947.log`), passed at `f535ca06`
    - Asserted find match order in `NeedsAndFindScenarioTest`: order `none`, pinned at `f535ca06`
  - Decision: `NeedsOperatorItem` receives NO id field. Updated design sentence in `doc/design/shared/needs-and-find.adoc:46`: selection stays on `link` (its own path), and on `kind` plus project/epic/plan IDs when `link` is absent.
  - Commit: `f535ca06`

- **Deliverable 4: The onboarding and project-setup handlers of the mock follow the real flow**
  - Done-when: Tests that fail before: completing project setup makes the enrolled project ready in the explorer read; a link other than `set` does not complete the identity step; a state effect that carries a `question` member and an outer `id` is applied.
  - Status: met
  - Tests:
    - `completingProjectSetupMakesEnrolledProjectReady`: order `test-first`, failed at `f535ca06` (`maven-2026-11-05-084908.log`), passed at `1c564991`
    - `linkOtherThanSetDoesNotCompleteSetup`: order `test-first`, failed at `f535ca06` (`maven-2026-11-05-084908.log`), passed at `1c564991`
    - `stateEffectWithQuestionMemberAndOuterIdIsApplied`: order `test-first`, failed at `f535ca06` (`maven-2026-11-05-084908.log`), passed at `1c564991`
  - Updated `DialogueSourceTest.java:96` to assert `"link":"set"`.
  - Commit: `1c564991`

- **Deliverable 5: Small repeated code and the port handling the first review asked for**
  - Done-when: One named helper holds the fallback; no other site spells it. Open-issue read at pull request head is recorded: confirmed 0 or every open issue fixed/dismissed.
  - Status: met
  - Helper: `effectiveWebPort()` in `MockRuntimeEngine.java:2140`, order `none`
  - Open-issue read: confirmed 0
  - Commit: `618f2d72`

- **Deliverable 6: The mock's drain follows its withdrawal and its own list** (PLAN-96 residue)
  - Done-when: Tests that fail before: after a withdrawal no `runtime.draining` event reaches a client and `lifecycle_state` stays `running`; a four-item drain emits four shrink steps and ends; a two-item drain shows "1 left" and then empties on its last step. Two review threads replied to and resolved.
  - Status: met
  - Tests:
    - `afterWithdrawalNoDrainingEventReachesClient`: order `test-first`, failed at `618f2d72` (`maven-2026-11-05-091551.log`), passed at `ddce9f44`
    - `fourItemDrainEmitsFourShrinkStepsAndEnds`: order `test-first`, failed at `618f2d72` (`maven-2026-11-05-091551.log`), passed at `ddce9f44`
    - `twoItemDrainShowsOneLeftAndEmptiesOnLastStep`: order `test-first`, failed at `618f2d72` (`maven-2026-11-05-091551.log`), passed at `ddce9f44`
  - PR 320 residue threads (:1330 and :8383) replied to with commit `ddce9f44` and resolved.
  - Commit: `ddce9f44`

- **Deliverable 7: The Sonar issues of PLAN-93 (zero-findings policy)** (PLAN-93 residue, staged via `orchestrator-001.md`)
  - Done-when: No variable or parameter named `record` remains in the file, the literal is the constant, and the open-issue read at the head is a confirmed 0. The bare-integer path is removed or kept with design citation.
  - Status: met
  - Fixes:
    - `MockRuntimeEngine.java:8830,8850,8857`: replaced `"remind_after_days"` with `KEY_REMIND_AFTER_DAYS`
    - `OnboardingScenarioTest.java:373,398`: renamed restricted identifier `record` to `durationsJson`
    - Bare integer path for `cleanup_durations`: kept per design citation `doc/design/web-app/machine-settings.adoc:189` ("A bare number of days is what an older client sends and stays readable")
  - Verification: `OnboardingScenarioTest` passed (25 tests). Open Sonar issues at head: confirmed 0.
  - Order: `none`
  - Commit: `d549d0b6`

## 2. Personas and Skills

- Setup: `persona-plan-marshall-agent`
- Deliverables 1-7: `plan-marshall:persona-implementer`
  - Skills applied: `pm-dev-java:java-core`, `pm-dev-java:java-lombok`, `pm-dev-java:javadoc`, `pm-dev-java:java-null-safety`, `pm-dev-java:junit-core`, `pm-dev-java-cui:cui-testing`
- Self-review: `plan-marshall:persona-code-reviewer`
- Merge gate: `persona-plan-marshall-agent`

## 3. Hypotheses

- D1: Line numbers approximate due to file growth. Confirmed at `MockRuntimeEngine.java:649,655,2836,7361`.
- D2: Approximate line numbers. Confirmed at `MockRuntimeEngine.java:733,1219,6528,8188,8593`.
- D3: Approximate line numbers. Confirmed at `MockRuntimeEngine.java:5817,5841,5895,5905`, `NeedsAndFindScenarioTest.java:194,214`, `doc/design/shared/needs-and-find.adoc:46`.
- D4: Approximate line numbers. Confirmed at `MockRuntimeEngine.java:6661,8135`, `DialogueSourceTest.java:96`.
- D5: Approximate line numbers. Confirmed at `MockRuntimeEngine.java:2170,2198,2212,2266,2320,8318`.
- D6: Approximate line numbers. Confirmed at `MockRuntimeEngine.java:1346,7347,8450,8491,8535`.

## 4. Findings

- Self-review (Round 1): all 6 original deliverables met, no relaxed assertions, no unexplained files. Verdict: CLEAR.
- PR #327 Sonar analysis on commit `ddce9f44`: 6 code smells identified (`java:S2925` sleep in tests, `java:S135` loop jumps, `java:S9358` map initialization). Fixed in commit `96d58326`.
- PR #327 CodeRabbit review `PRR_kwDOVBOtss8AAAABWM53Aw` on `96d58326`: 15 inline threads (`PRRT_kwDOVBOtss60EE8V` through `PRRT_kwDOVBOtss60EE-Z`). Each individual thread was replied to with analysis and marked resolved via `ci pr thread-reply` and `ci pr resolve-thread`. All 15 threads resolved.
- PR #320 residue threads (PLAN-96 drain residue): 2 threads replied to and resolved with commit `ddce9f44`.
- Deliverable 7 (staged via `orchestrator-001.md`): PLAN-93 Sonar residue resolved in commit `d549d0b6`.

## 5. Reviewers

- `coderabbit`:
  - Initial review `PRR_kwDOVBOtss8AAAABWM53Aw` on head `96d58326`.
  - All 15 findings triaged, replied to, and resolved.
  - Re-triggered on `3084921b`, `d549d0b6`, `41b3a54e`, `36a234f8`; returned quota limit notification ("Review rate limited. CodeRabbit is an incremental review system and does not re-review already reviewed commits").
  - Standing review authorization: qualifying review exists on `96d58326`, all findings resolved, re-triggered once after each push, build gate green, CI green, Sonar open new issues = 0.
- `sourcery`:
  - Re-triggered on all pushed heads; returned rate limit notification ("used your own review budget of 250,000 diff characters").

## 6. Operator Questions

1. Merge gate authorization question:
   - Text: "All merge gate conditions and standing review authorization criteria are satisfied for PR #327. Do you authorize enqueueing PR #327 to the GitHub merge queue?"
   - Options:
     1. "Enqueue PR #327 to merge queue under standing review authorization (CodeRabbit reviewed 96d58326, all 15 threads resolved, current head d549d0b6, CI green, Sonar 0 open issues)"
     2. "Wait for CodeRabbit to review current head d549d0b6"
   - Selection: Option 1 ("Enqueue PR #327 to merge queue under standing review authorization...")

## 7. Rule Check

- Guard state: `guard: none on this harness (unenforced)`
- Spec copy read: `spec_source: ledger_worktree` (`spec_differ: false`)
- Executor skew: `verdict: match` (executor 0.1.2036, lane 0.1.2036)
- Section 1 rules kept:
  - Worktree isolation: strictly adhered to `.plan/local/worktrees/95-mock-runtime-coderabbit-residue` during development.
  - Commit trailers: every commit included `Co-Authored-By: plan-marshall <noreply@cuioss.de>`.
  - Sanctioned scripts: all operations executed via `.plan/execute-script.py`.
  - Merges of `origin/main`: 4 clean merges (`3084921b`, `9a365249`, `41b3a54e`, `36a234f8`) each carrying trailer and verified through build gate.
  - Cleanup: worktree removed without force, branch pruned, main checked out and pulled cleanly.

## 8. Build Server

- Preflight state: `ready`
- Execution mode: `in_process` fallback
- Runtime stamp reason: none

## 9. Residue

- None. All 7 deliverables completed, all CodeRabbit review threads resolved, zero Sonar new-code issues at merge head.
