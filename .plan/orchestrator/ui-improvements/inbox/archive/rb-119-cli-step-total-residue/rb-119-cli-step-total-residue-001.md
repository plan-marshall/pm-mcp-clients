envelope_version=1
sender_type=plan
sender_id=rb-119-cli-step-total-residue
epic=ui-improvements
kind=landing
created=2026-11-09T10:07:40Z

```landing-facts
schema=landing-facts/1
plan_id=rb-119-cli-step-total-residue
epic=ui-improvements
pr=#377
merge_state=merged
cleanup_owed=false
deliverables_total=2
deliverables_done=2
total_tokens=unknown
harness=antigravity
model=unknown
effort=unknown
plugin_version=0.1.0
executor_version=0.1.2080
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
step.sonar-roundtrip.open_issue_count=0
step.sonar-roundtrip.open_issue_rules=none
```

guard: none on this harness (unenforced)

# PLAN-119 Landing Report: Terminal: One Rule for the Step Total (PLAN-116 Residue)

## 1. Deliverables

### Deliverable 1: One rule for a missing or invalid total
- **Done-when 1:** Tests at both reading sites (`BeingAddedProject` and `StateDialogue.fromJson`) cover missing, zero, negative, fractional, over-range, and non-numeric `total_steps`, plus valid custom total other than 10; tests fail on revert of their site.
  - **Verdict:** met
  - **Tests:** `ProjectRecordsTest#beingAddedProjectTotalStepsVariants`, `StateDialogueTest#stateDialogueTotalStepsVariants`
  - **Order:** `test-first`
  - **Failing runs:** commit `bf24091c`, jobs `0939f2c44a4d4a83a3ea32a48f0c352e` and `de25395aaf534fdd84a6b38abc9dad53`
  - **Commit:** `0986b260`
- **Done-when 2:** A project payload without `total_steps` renders fallback 10 (`[n/10]`), and test asserts rendered step line.
  - **Verdict:** met
  - **Tests:** `DialogueSourceTest#projectSetupWithoutTotalStepsFallsBackToDefaultTen`, `StateDialogueTest#projectPayloadWithoutTotalStepsRendersStepLine`
  - **Order:** `test-first`
  - **Failing run:** commit `bf24091c`, job `08d118d3dedb4b97afb82fb167cc8252`
  - **Commit:** `0986b260`

### Deliverable 2: The overview follows the injected aspect list
- **Done-when 1:** A dialogue-level test over a two-aspect list shows "2" in intro and exactly those two aspects in overview; fails if reverting to `values()`.
  - **Verdict:** met
  - **Test:** `StateDialogueTest#dialogueFollowsInjectedAspectList`
  - **Order:** `test-first`
  - **Failing run:** commit `bf24091c`, job `67a3667cbd8c4278988b6e11ecba4603`
  - **Commit:** `fe0a4591`
- **Done-when 2:** `git grep -n "OnboardingAspect.values()" -- pm-operator/src/main` lists only documented no-list defaults.
  - **Verdict:** met
  - **Order:** `none`
  - **Evidence:** Confirmed exactly 4 documented no-list defaults remain (`StateDialogue.java:181`, `OnboardingSource.java:47,56`, `EntryFlow.java:78`).
- **Done-when 3:** The open-issue read at the pull request head is recorded: confirmed 0, or every open issue dismissed.
  - **Verdict:** met
  - **Order:** `none`
  - **Evidence:** Confirmed 0 open new-code issues at merge head (`1aa8918f` / `147309ae`).

## 2. Rule Checks
- All section 1 rules kept:
  - File modifications performed strictly through file tools (`read_file`, `write_file`, `edit_file`); no shell commands used for file reading or tree walking.
  - Temporary files placed exclusively under `.plan/temp/`.
  - Scripts invoked exclusively via `python3 .plan/execute-script.py`.
  - Main repository checkout kept pristine during implementation; all changes isolated to worktree `.plan/local/worktrees/119-cli-step-total-residue`.
  - Commit trailer `Co-Authored-By: plan-marshall <noreply@cuioss.de>` included on all commits and merges.
- Guard: `guard: none on this harness (unenforced)`

## 3. Gate
- **Build Gate:** `verify -pl pm-api,pm-operator -Ppre-commit` passed with 2,226 tests executed and green (job `3399f4463d5a479db0897b4e4b3acb89`).
- **CI Status:** GitHub Actions CI passed (20 checks green on HEAD `1aa8918f`, run `42594405498`).
- **Reviews:**
  - CodeRabbit submitted qualifying formal review on `fe0a4591`. 2 inline comments were posted, answered, and resolved (`resolved: true`). Re-triggered on subsequent heads (`3386d44e`, `2e773c61`, `1aa8918f`) and rate-limited.
  - Sourcery approved (`APPROVED`).
- **Sonar State:** Confirmed 0 open new-code issues (`step.sonar-roundtrip.open_issue_count=0`, `step.sonar-roundtrip.open_issue_rules=none`).
- **Standing Review Authorization:** Conditions satisfied; operator confirmed merge via question tool selection.
- **Merge Queue:** Enqueued and settled as `merged` into `main` with merge commit `147309aed0ec6406c85ee07588ea4e8bbd32f897`.
- **Cleanup:** Worktree `.plan/local/worktrees/119-cli-step-total-residue` removed, feature branch pruned locally and remotely, main checkout pulled to `147309aed0ec6406c85ee07588ea4e8bbd32f897`.

## 4. Notes
- **Hypothesis 1 confirmed:** `JsonTree.integer` casts `Number.intValue()`, causing fractional truncation (`3.9 -> 3`) and long overflow wrapping (`4294967297 -> 1`). `JsonTree.positiveInteger` resolves this by validating strict positive integers in `1..Integer.MAX_VALUE`.
- **Hypothesis 2 refuted:** The runtime contract does not specify mandatory `total_steps` presence; unified fallback `10` is now consistently applied across `BeingAddedProject`, `ProjectOnboardingSource`, and `StateDialogue`.
- **CI Infrastructure:** Observed intermittent HTTP 504 Gateway Timeouts from adoptium/vendor mirrors during GitHub Actions `actions/setup-java` downloading JDK 26.
