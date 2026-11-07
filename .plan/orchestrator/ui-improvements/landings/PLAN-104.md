# Landing analysis: PLAN-104 cli-console-third-residue

epic: ui-improvements
workstream: WS-04
pr: 357

PR #357 `7902d577` (squash-merged 2026-11-06T21:17:21Z; head `5aec11c7`, a main-only merge; 16 commits, 41 files, +1310/-283) and the whitespace follow-up PR #361 `14a6f7c0` (one file, `PmConsoleBehaviourTest`, four continuation lines; `git diff -w` empty; label `skip-bot-review`). Lane `rb-104-cli-console-third-residue` (runbook lane, 14 builds all `in_process_fallback`: no `mvn` on the daemon's PATH): landing message `inbox/rb-104-cli-console-third-residue-001.md`. Analysis by a read-only sub-agent (code read at the merge commit, nothing built or run); the thread counts, the review list, the PR 341 comment list and the Sonar status were re-read by the orchestrator (marked V*).

## Merge rule

PARTLY SATISFIED. CodeRabbit reviewed three heads of PR 357: `12be4500` (review `5804837268`: 7 inline threads, 7 body nitpicks), `3cecb33a` (`5805509142`: 2 nitpicks) and `3f121890` (`5806743402`, 2026-11-06T20:45Z, 18 minutes before the merge: 1 inline thread, 1 outside-diff item). The lane's last comment (21:03) says "Every thread of the last review is answered and resolved": FALSE for the last review.

- Unresolved threads at the merge head (V*: GraphQL): PR 357 **1 of 8** (`EntryFlowStaticsTest.java:116`, "wait for the worker to record its interrupt flag", review `5806743402`, no reply), PR 341 0 of 4, PR 321 0 of 14, PR 332 0 of 10. The lane did not state the count (the facts block has the `step.sonar-roundtrip.*` pair only).
- Unhandled review BODY items: the seven nitpicks of review `5804837268` have no reply anywhere (the 20:03 comment `6694243953` answers the two nitpicks of `5805509142` only); three are visibly undone at `7902d577` (`EntryFlowStaticsTest` ~58 asserts no worker cause; `StateDialogueTest` ~244 still takes cases and expectation from the production map `PROJECT_STEP_BY_STATE`; `OnboardingCatalogueTest` ~120 tests `total=7` only), three are unclear (`QuestionsCommandTest` ~382, `PmConsoleBehaviourTest` ~451, `ConsoleSettingsTranscriptTest` ~70), one is not done (`ConsoleBadgeStallIT` 91-97: nothing asserts the proxy held the read). The outside-diff item of `5806743402` (`PmConsole.java:288`: `ProjectSelector.rewrite` treats a positional entry id starting `--project` as a project selector) is unanswered; real only for such an id.
- PR 341's five body items (reviews `5791531265`, `5792262598`): NO REPLY ON GITHUB (V*: PR 341 has no comment after 2026-11-05T21:32Z). The PR 357 body answers them in a table that mislabels them as "PR 332 review 5791531265". Deliverable 2.3 and 6 asked for a reply.
- D6 replies that exist and are honest: PR 332 comment `6691789090` (F4, F3), reply `4495865723` in the `EntryFlow.java:712` thread, PR 321 comment `6691794330` (items 3 and 6). The `:712` reply is STALE: it says the checkout directory name still stands as the entry address; `eb1de3b0` removed exactly that.
- SonarCloud (V*: check run `SonarCloud Code Analysis` SUCCESS on #357 and bot comment `6695498495`): Quality Gate passed, 0 new issues, 0 accepted, 0 hotspots, 89.3% new-code coverage; #361 head `f6e090f9` also passes. The rule list is `unknown` (the issue API was not readable; the lane's `open_issue_rules=none` is unverified). Mid-PR the lane fixed seven smells and S3776.
- Lane message: facts block lacks `unresolved_review_threads=N`, `review_body_findings_total/handled` and `sonar_open_new_issues=N` (Standing Constraints). It reports "reviewed 3cecb33a (and fde92572); seven threads": three reviews exist. The lane's own Residue section is honest about D2.1 (lock-budget revert unobservable), D3.1 (failing run covers the intro only), D4.2 (order downgraded to `none`), D5.2 (mutation replaces the handling, not the catch type), hand-written literals, web `STEP_KEYS`, `StateDialogueTest` self-derivation.

## Three layers

| Done-when | Implemented | Correct |
|---|---|---|
| D1 Enter sends the contract's address; fixtures from `toMap` only; quote and backslash unaltered | MET (`EntryFlow` ~660-705 reads the entry from `continue_link`; `PmConsole.java:288` runs `project setup --entry <addr>`; no `project_id` read) | tests use `BeingAddedProject.toJson()` with generated ids (`PmConsoleBehaviourTest:443-451, 478, 510`) but assert only that the request reached `/api/v1/entries/{id}`, answered 404; success parsing untested. A badge without a link offers no continuation |
| D1 `04-console.adoc` names addressing, reason, open item and owner | MET (`04-console.adoc:106`, owner `plan-marshall/plan-marshall-mcp`) | against the real daemon Enter on a being-added badge is non-functional until the daemon serves `/api/v1/entries/{id}`; no issue in the owner repo is evidenced |
| D2 each item fails when reverted | PARTLY | row-8, state table, `bounded`, serialisation logs cited, not rerun; lock-budget revert unobservable (self-reported); `StateDialogueTest` expectation still from production; deadline test keeps `REFRESH_BOUND + 1s` (`PmConsoleBehaviourTest:426`) |
| D2 `ForTest` surface gone | MET (`git grep` of `ForTest\|clearCacheForTest` in the two `src/main` trees is empty; no `PROJECT_STATES`, no `getStepName`; `Math.max(1L, …)` kept with a test) | `PmConsole(context, EntryFlow)` public, DI not test surface; remaining `CountDownLatch(1).await()` in `EntryFlowStaticsTest:71,96` is the worker-blocking idiom |
| D2 `bounded` error path | MET (`EntryFlow.java:558-597`: uncaught-exception handler, interrupt on both paths, never `null`) | worker-cause assertion missing; a worker blocked in socket I/O is not freed by `interrupt()` (hypothesis, not addressed) |
| D2 five items answered on GitHub | NOT MET for PR 341 | see Merge rule |
| D2(e) Generators | PARTLY (annotation on the three classes) | hand-written literals remain in two older console tests (self-reported) |
| D3 refresh path and intro follow another aspect size | PARTLY (`PmConsole` ~110 uses `entryFlow.aspects().size()`; intro is `{total}`) | failing run covers the intro only; `OnboardingSource.TOTAL_STEPS` (l.38) still `OnboardingAspect.values().length`, `ProjectOnboardingSource.TOTAL_STEPS = 10` (l.37) a literal: one source for badge and `EntryFlow`, not for the dialogue |
| D3 designs and screens show the real count | MET for the named files (`05-transcripts-entry.adoc:145,160,477`, `web-app/onboarding.adoc:72`, `claude_review/cli/resources/screens/onboarding-console.txt:3`, `console-settings.transcript:5,20`) | prose "seven steps" stays in `01-commands.adoc:123`, `08-transcripts-later.adoc:380`, `05-transcripts-entry.adoc:195,358,398`, `findings-onboarding-entry.adoc:20`, `cli/ux/findings.adoc:23`: hand-copied counts, consistent with 7 today |
| D4 German row via the catalogue; clock mutation | MET (`TranscriptNormalizer` ~115 reads `operator.devices.row`; `console-settings-de.transcript`; no `Instant.now()` in console main) | mutation logs `154837`, `154713` cited, not rerun; per-device age nitpick unanswered |
| D4 stalled and recovered captures, same scenario, before and after | PARTLY (lane says so) | `ConsoleBadgeStallIT` captures both in one session through a test-only `StallingSocketProxy`; asserts the lines differ, not that the stall was reached; no before/after code change |
| D5 generic catches and markers | MET (grep names only `EpicCommand:108`, `PlanCommand:146,178` and their markers, the carve-out for PLAN-97; `EntryFlow:732` cleaned) | eight tests fail when the handling is removed; reverting to the generic catch alone stays green (self-reported) |
| D5 formatting residue | PARTLY (#361 fixes four test lines) | `ConsoleBadge.java:27-31` record components still at column 0 at `14a6f7c0`; `PmConsole.java:~235` `@Nullable` on its own line; 28-space continuations in other main files not attributed to the gate |
| D6 every item answered; 0 unresolved; Sonar recorded | PARTLY | see Merge rule: 1 thread open, nitpicks and PR 341 unanswered, rules `unknown` |

## Defects (at `7902d577`)

1. HIGH V* review `5806743402` was never handled: thread `EntryFlowStaticsTest:116` unresolved and unanswered, outside-diff item `PmConsole:288` (`ProjectSelector.asksForProject` ~168 misreads an entry id starting `--project`) unanswered; the lane's merge comment says the opposite.
2. HIGH V* PR 341's five body items have no reply on GitHub (D2.3, D6); the PR 357 body mislabels them.
3. HIGH V the seven nitpicks of `5804837268` are unanswered; three are visibly undone (above).
4. MED V the landing-facts block omits `unresolved_review_threads`, `review_body_findings_total/handled`, `sonar_open_new_issues`.
5. MED V PR 332 reply `4495865723` is stale after `eb1de3b0`.
6. MED V the step count is not one source in the dialogue (`OnboardingSource:38`, `ProjectOnboardingSource:37`); the refresh half of the D3 test was not failing first.
7. MED V Enter on a being-added badge works only against a daemon that serves `/api/v1/entries/{id}`; the 404 test proves only the address. Whether the owner repo tracks it: L.
8. MED V `StateDialogueTest` derives its expectation from the production map; a dropped state passes (also `OnboardingCatalogueTest` total-7, the stall IT vacuity).
9. LOW V formatting residue `ConsoleBadge.java:27-31` and `PmConsole.java:~235`.
10. LOW V deadline test margin 1 s (`PmConsoleBehaviourTest:426`).
11. LOW V `ConsoleBadgeStallIT` does not await that the stall was reached; a `LockSupport.parkNanos` poll with a 500 ms deadline at `PmConsoleBehaviourTest:689` is a timer wait.
12. LOW L worker blocked in socket I/O is not freed by `interrupt()` (carried from PLAN-103 defect 6; unaddressed).

## Metrics and anomalies

- 16 commits (two `origin/main` merges), 3 CodeRabbit reviews, 3 rate-limited re-triggers, 3 self-review rounds; Sourcery refused on its own budget; lane model deepseek-v4.1-flash/high, tokens `unknown`; two forbidden `python3 -c` probes admitted in the rule check.
- The merge came 18 minutes after the last CodeRabbit review and 19 minutes after the final push; the CodeRabbit check at the merge head was "rate limited, neutral". Under the standing merge rule (one review with its findings handled, later heads need none) the merge was allowed, but the last review's findings were NOT handled, and the lane said they were. Fourth consecutive lane to report "done" for items GitHub does not show.
- CI green on every head checked (build 25, 26, integration tests).

## Reconciliation Actions

- [x] row `status` → `shipped` — `orchestrator queue --transition PLAN-104 --status shipped`
- [x] row `pr` = 357; `landing` = `landings/PLAN-104.md`; `plan_marshall_plan_id` = `104-cli-console-third-residue`
- [x] epic.md: Decisions entry (landing, residue routed to PLAN-116), Queue annotation
- [x] `inbox/rb-104-cli-console-third-residue-001.md` archived under `inbox/archive/rb-104-cli-console-third-residue/`
- [x] resume anchor updated; `queue-view.md` regenerated; ledger committed

## Follow-Ups

- **PLAN-116 `cli-console-fourth-residue`** (WS-04, staged, after PLAN-97 lands): defects 1-3 and 5 (answer review `5806743402`, the seven nitpicks and PR 341's five items in the threads and comments, correct reply `4495865723`; fix the `--project` entry-id case), defects 6 and 8-11 (one step total in the dialogue, independent `StateDialogueTest` expectation, `OnboardingCatalogueTest` total, stall IT awaits the stall, no timer poll), defect 9 (the gate on the final head). It must end with `unresolved_review_threads=0` and the body-item count in its landing facts. Staged after PLAN-97 because both rewrite pm-operator files and PLAN-97 rewrites the command tree.
- PLAN-97: keeps `EpicCommand:108` and `PlanCommand:146,178` and their markers (its deliverable 5); it should run the gate on the final head so the column-0 and 28-space residue does not recur.
- Daemon owner (`plan-marshall/plan-marshall-mcp`): the `/api/v1/entries/{id}` state and act surface (defect 7); recorded in the epic Watch, an outside-repo request for the operator.
- Console-badge and dialogue plans (PLAN-21, 23, 30, 34, 36, 42, 44, 50, 79, 84): a badge without an entry address offers no continuation; the count in prose "seven steps" copies is a candidate for a count lint (not staged).
- Unattributed: defect 12 stays an open watch.
