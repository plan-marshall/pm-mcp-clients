envelope_version=1
sender_type=plan
sender_id=rb-116-cli-console-fourth-residue
epic=ui-improvements
kind=landing
created=2026-11-08T10:03:56Z

envelope_version=1
sender_type=plan
sender_id=rb-116-cli-console-fourth-residue
epic=ui-improvements
kind=landing
created=2026-11-08T10:03:00Z

```landing-facts
schema=landing-facts/1
plan_id=rb-116-cli-console-fourth-residue
epic=ui-improvements
pr=#368
merge_state=merged
cleanup_owed=false
deliverables_total=3
deliverables_done=3
total_tokens=unknown
harness=opencode
model=gemini-2.5-pro
effort=high
plugin_version=unknown
executor_version=0.1.2080
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
step.sonar-roundtrip.open_issue_count=0
step.sonar-roundtrip.open_issue_rules=none
```
guard: none on this harness (unenforced)

# PLAN-116 cli-console-fourth-residue — landing

PR #368 (merge commit `6c105a86f9163de6723d5d9fde61ef21f8d8bed9`). Merged through the merge queue with green CI, zero unresolved review threads, and zero Sonar open new issues at head.

## Deliverables

| # | Deliverable | Verdict |
|---|---|---|
| D1 | Every open review item answered where the reviewer reads it | **met** |
| D2 | One step total in the dialogue, expectations from the contract | **met** |
| D3 | The formatting gate on the final head, and the facts the landing message must carry | **met** |

### D1: Every open review item answered where the reviewer reads it
- **Failing check**: `test -pl pm-operator -Dtest=PmConsoleBehaviourTest#entryIdStartingWithProjectReachesTheRuntimeUnaltered` failed as expected with `expected: </api/v1/entries/--project-xyz> but was: <null>` (job log `4642b04290214d03bf757277f56722d3.log`).
- **Passing check**: After passing `--` before the entry address in `PmConsole` and stopping option scanning in `ProjectSelector` upon encountering `--`, the test passed cleanly (job log `7ea34141085c4954b1502e6f68ebb428.log`).
- **Unit & Integration tests**:
  - `ProjectSelectorTest#ignoresArgumentsAfterEndOfOptions`: verifies option scanning ceases after `--`.
  - `EntryFlowStaticsTest:58`: asserts worker exception cause and message.
  - `OnboardingCatalogueTest`: asserts formatted step totals for `total=8` in English and German.
  - `QuestionsCommandTest`: asserts unchecked terminal failure is not misclassified as input failure.
  - `PmConsoleBehaviourTest#enterOnABeingAddedProjectBadgeSucceeds`: covers project continuation end-to-end.
  - `ConsoleSettingsTranscriptTest`: explicit assertions for device ages at base instant and 120 days later.
- **Review Items & Threads**:
  - PR 357 thread `PRRT_kwDOVBOtss60r6WT` (`EntryFlowStaticsTest:116` race): replied citing `629e33ce` and resolved.
  - PR 357: comment `6713948257` posted answering outside-diff item on project prefix and all 7 review nitpicks.
  - PR 341: comment `6713951175` posted answering all 5 review body items citing PLAN-104 and PLAN-116.
  - PR 332 thread `PRRT_kwDOVBOtss60FnxO` (`EntryFlow.java:712`): replied stating true state (`eb1de3b0` removed directory fallback; pre-enrolment runs addressed via entry ID).

### D2: One step total in the dialogue, expectations from the contract
- **Failing check**: `test -pl pm-operator -Dtest=StateDialogueTest#beingAddedRunWithCustomTotalStepsPrintsThatTotal` failed as expected with `expected: <12> but was: <10>` (job log `b01265be65234d3486e6f2b81ad9ef03.log`).
- **Passing check**: `StateDialogueTest,DialogueSourceTest,PmConsoleBehaviourTest#twoConcurrentRefreshesNeverInterleave` passed 19 tests green (job log `bd7d1538d9c947b7bf2f8b1e9c4a1730.log`).
- **Implementation**:
  - Removed static `TOTAL_STEPS` constants from `OnboardingSource` and `ProjectOnboardingSource`.
  - Injected aspect list / step count support in `OnboardingSource`.
  - `StateDialogue.fromJson` derives total steps from reported `total_steps` in payload, falling back to injected aspect size.
  - Invariant check: `git grep -n -E "TOTAL_STEPS =|values\(\)\.length" -- pm-operator/src/main` returned 0 occurrences.
  - `StateDialogueTest`: decoupled expected project steps using independent `EXPECTED_PROJECT_STEP_BY_STATE`.
  - `PmConsoleBehaviourTest`: replaced 500 ms `parkNanos` poll with deterministic condition wait on `TIMED_WAITING` state, and widened deadline margin to 1.5 s.
  - `StallingSocketProxy` & `ConsoleBadgeStallIT`: added `holding` latch and `awaitHolding(READY_TIMEOUT)` before prompt assertions.

### D3: The formatting gate on the final head, and the facts the landing message must carry
- Formatted `ConsoleBadge.java:28-33` record components and `PmConsole.java:238` `@Nullable` placement.
- Pre-commit formatting gate `./mvnw -pl pm-api,pm-operator verify -Ppre-commit` run on head (`5c16e8b98bc44e97af4125a80d326725`).
- Second pass confirmed gate idempotence with 0 rewrites (`7387e1effe29425395ed571ffdba57ef`).
- Resolved 3 new-code Sonar findings on PR #368:
  - `java:S1192` in `StateDialogue.java`: extracted `TOTAL_STEPS_FIELD`, `STEP_FIELD`, `COMPLETED_STEPS_FIELD` constants.
  - `java:S3776` in `ProjectSelector.java`: extracted `isOptionAsking` to reduce cognitive complexity.
  - `java:S3012` in `ProjectSelector.java`: replaced array copy loop with `subList`.
- CodeRabbit review on PR #368: single thread `PRRT_kwDOVBOtss608Lxj` replied and resolved.
- Confirmed zero Sonar open new issues at head.

## Review Audit Facts

unresolved_review_threads=0 (PR 368: 0, PR 357: 0, PR 341: 0, PR 332: 0)
review_body_findings_total=13
review_body_findings_handled=13
sonar_open_new_issues=0
