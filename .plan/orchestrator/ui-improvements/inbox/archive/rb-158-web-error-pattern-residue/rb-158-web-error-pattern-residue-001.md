envelope_version=1
sender_type=plan
sender_id=rb-158-web-error-pattern-residue
epic=ui-improvements
kind=landing
created=2026-11-11T13:51:52Z
revision=1
amended=2026-11-11T13:52:46Z

```landing-facts
schema=landing-facts/1
plan_id=rb-158-web-error-pattern-residue
epic=ui-improvements
pr=#408
merge_state=merged
cleanup_owed=false
deliverables_total=5
deliverables_done=4
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2106
steps=setup:done,implement:done,build-gate:done,self-review:budget-exhausted,pr:done,reviews:done,merge:done
unresolved_review_threads=0
review_body_findings_total=1
review_body_findings_handled=1
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
step.sonar-roundtrip.quality_gate_status=ok
```

guard: none on this harness (unenforced)

# PLAN-158 landed: the error and loading pattern's residue, plus the PLAN-129 residue routed here

PR #408, merge queue, merge commit `4a84f727c3181e0c11ad73e29a432cdf3a64689b`. Everything landed in one pull request: the five residues of PLAN-45, the eight items of the orchestrator's advisory `orchestrator-001.md`, and the findings of the self-review rounds and of the CodeRabbit review.

## 1. Deliverables

**D1 — no raw code on an act path: met (2/2).** Commits `529a0389`, with review fixes in `1efda2a9`, `4e4e5cfc`, `7bde4251`.
- *Per view, an act failing with `ClientError('timeout')` shows the sentence and not the word* — **met**, order `test-first`. Tests: `CleanupView.test.ts:217`, `DecisionView.test.ts:135`, `ProjectExplorerView.test.ts:271`, `ProjectOnboardingView.test.ts:292`, `ReportsView.test.ts:1039`, `StartView.test.ts:30`, `SystemView.test.ts:353`. Failing run at the base commit `8568b56e` with only the tests applied: `.plan/local/plans/NO_PLAN/build-results/pm-web-app/maven-2026-11-10-204740.log`, `Tests 7 failed | 1398 passed (1405)` (`Received: timeout`). Passing at `529a0389`: `maven-2026-11-10-205626.log`, `Tests 1405 passed`.
- *The grep names no view* — **met**, order `none`. `git grep -n "as Error)?.message" -- pm-web-app/src ':!*.test.ts'` names no view: the hits left are `api/client.ts`, `stores/runtime.ts` (code tests, not prints) and `routes/+layout.svelte`'s `outcomeUnknown` detector.

**D2 — banners that clear, a deadline that says so, a timer that fires: not fully met as written (4/5 members).** Commit `8c00908d`.
- *A failed act then a reload leaves no banner; a failed toggle then a successful one leaves no banner; a caller deadline mid-body ends in `timeout`; the focus after Retry is not the body* — **met**, order `test-first`. Tests: `DecisionView.test.ts:152`, `ProjectExplorerView.test.ts:290`, `client.test.ts:616`, `ReportsView.test.ts:726`. Failing run at `529a0389`: `maven-2026-11-10-210020.log`, `Tests 4 failed | 1406 passed (1410)`. Passing at `8c00908d`.
- *The fake-timer request ends in `timeout` at the limit* — **not met as written**, order `none`: the test exists (`client.test.ts:642`, nothing at 14 s, `timeout` past 15 s) but it passes at the base commit too, because `failFetch` already answered `timeout` on the client's own deadline. No failing-before output exists to report, so by the evidence rule this clause is not met and the deliverable is not fully met. The code path the clause protects is covered.

**D3 — the mock answers codes the catalogue has: met (2/2).** Commits `492201d7`, refined in `7bde4251` and `9760148b`.
- *The grep names only members or the reported exceptions* — **met**, order `none`: only `scope_not_found`, `target_not_found` (members) remain, plus the three reported exceptions `scenario_not_found`, `state_not_found` (the mock's own `/mock` scenario and goto surface) and `version_not_found` (release notes for an unknown version).
- *A mock test per changed code pins the new one* — **met**, order `mutation-after`: `MockRuntimeEngine.java` reverted to the old literals after the commit — `maven-2026-11-10-210933.log`, `Failures: 8`, e.g. `expected: <target_not_found> but was: <hold_not_found>`; restored, green at `492201d7` (1265 tests). The review then split unknown epic from known epic without the sub-resource and unknown project from enrolled project without a store; both arms pinned in `ControlScenarioTest` and `EnrolScenarioTest`.

**D4 — tests, keys and the design text PLAN-45 owed: met (4/4).** Commits `45bf329b`, `550e39f4`.
- *The new unit tests fail when the pattern is reverted* — **met**, order `mutation-after`: `ErrorState`/`Skeleton` replaced by a plain paragraph in both views — `maven-2026-11-10-211632.log`, `Tests 4 failed | 1408 passed (1412)`; restored. Tests: `EpicDecompositionView.test.ts:88,98`, `StartView.test.ts:48,59`.
- *The e2e hang case asserts ≤ 20000 and passes* — **met**, order `none`: `pm-web-e2e/tests/error-pattern.spec.js:62`; `maven-2026-11-10-212502.log`, `378 passed (5.8m)`, the hang case in 16.7 s. The offline case asserts the heading the dev proxy produces.
- *The grep names no reader-less key; components.adoc states the refresh form* — **met**, order `none`: `plan.view.not_found`, `web.settings.devices.load_failed`, `project.onboarding.retry`, `decomposition.error` are gone in both languages with the catalogue pin; `doc/design/web-app/components.adoc:463` states the refresh form and the announcement rule as the views implement them.

**D5 — the browser matrix PLAN-45 did not take: met (2/2).**
- *Browser check shows the pattern on every combination; snapshots plus screenshots recorded* — **met**, order `none`. 20 screenshots and 20 snapshots under `.plan/temp/playwright-mcp/`: `158-unknown-plan-{de-dark-1440,de-light-390,de-dark-390,en-light-1440,en-dark-1440,en-light-390}-after.png`, the same six for `158-start-unknown-project-…` and `158-mock-stopped-…`, each with its `-snapshot.yml`, plus `158-act-failure-de-dark-1440-after.png` (the System view's stop act held past the limit shows "Versuche es erneut; dauert es weiter so lange, prüfe, ob die Runtime läuft.") and `158-act-failure-en-light-390-after.png`. The mock-stopped target draws the shared unknown fallback, not `network_offline`, because the dev server proxies `/api` and answers a dead mock with an unreadable HTTP failure.
- *The open-issue read at the pull-request head is recorded* — **met**, order `none`: read at `935db6e1` (the merge head), `count_status: confirmed`, `open_issue_count: 0`, no rules left open; the quality gate is `OK` (new coverage 81.8%).

**Advisory `orchestrator-001.md` (the PLAN-129 residue): 8 of 8 fixed.** Commit `e1ee13c2` (then `73e978cd` merged `d1a9e63a`).
- A failed single *Mark read* keeps a row the runtime already reported read; a retained row whose detail closed during a failed mark-all is dropped when the request settles; a list answer older than the latest read, or overtaken by a read event, is not applied; pending read ids are counted per request; a mark-all that empties the list keeps the refresh form instead of drawing skeletons. Tests: six new `ReportsView` tests, order `test-first`, failing at `73e978cd` — `maven-2026-11-11-090147.log`, `Tests 9 failed | 1423 passed (1432)`; green at `e1ee13c2`.
- The test items (`RowMenu` scroll assertions jsdom cannot observe; the reload-in-flight success arm; `ReportsCommandTest`'s missing older-page negative assertion with `Generators`) are test-only and pass before and after; the reload-in-flight success arm was added and the race page made consistent in `7bde4251`.
- Files beyond the declared surface: `pm-web-app/src/lib/components/RowMenu.test.ts`, `pm-operator/src/test/java/de/planmarshall/operator/ReportsCommandTest.java`, `doc/design/shared/reports.adoc`. `pm-operator/src/main/java/de/planmarshall/operator/ReportsCommand.java` was read and not changed.

## 2. Personas and skills

Implemented as instructed: D1 and D2 on `persona-implementer` with `pm-dev-frontend:javascript`, `pm-dev-frontend:javascript-security`, `pm-dev-frontend:jest-testing`; D3 on `persona-implementer` with `pm-dev-java:java-core`, `pm-dev-java:junit-core`, `pm-dev-java-cui:cui-testing`; D4 on `persona-module-tester` with `pm-dev-frontend:jest-testing`, `pm-dev-java:junit-core`, `pm-dev-java-cui:cui-testing`, `pm-documents:ref-asciidoc`; D5 on `persona-integration-tester` with `pm-dev-frontend:javascript`; every self-review round and the review triage on `persona-code-reviewer`.

Skills added for surface drift: none beyond the declared domains. The files the review and the advisory pulled in (`CredentialEntry`, `StartWorkModal`, `PrAdoptPanel`, `LocalChangesDiffPreview`, `RecipeSelector`, `ConfinedFilePicker`, `WorkControlPanel`, `routes/+layout.svelte`, `stores/runtime.ts`, `RowMenu.test.ts`, `ReportsCommandTest.java`, `doc/design/shared/reports.adoc`) are covered by the same declared skills.

## 3. Hypotheses

- *The other `*_not_found` codes of the mock are outside `ApiErrorCode`* — **confirmed**: `ApiErrorCode.java:55-62` declares only `project_not_enrolled`, `scope_not_found`, `target_not_found`; the rest were literals in `MockRuntimeEngine.java`.
- *The orphan keys have no reader* — **confirmed**: `git grep -F` for each of the four finds no reader in `pm-api`, `pm-web-app`, `pm-web-e2e`, `pm-mock-runtime` or `doc`; `decomposition.error` was pinned only by `DecompositionCatalogueTest:79`.

## 4. Findings

- **Self-review round 1** — 1 major residue (five act surfaces still printing `err.message`), the module cycle, a doubled period I had added, the `announce` rule's doc overstatement, stale pins. Fixed in `1efda2a9` except the residue and the cycle, which the operator then chose to close (residue) or accepted (cycle).
- **Self-review round 2** — the previous fix's `announce={false}` silenced genuine arrivals and made the doc false. Fixed in `3bfa6644`: `announce` derives from the content each view holds; the completion report keeps its announcement.
- **Self-review round 3** — four more major raw-code surfaces, the double period, the mock's mapping, the doc claim, plus nits. The operator chose "close them all"; fixed in `4e4e5cfc`. Remaining open and accepted: the mock's per-sub-resource scope/target judgement (now consistent for unknown vs known epics and reviewed) and the latent `errors → display → i18n → client` import cycle (the helper's placement is spec-mandated). Round limit reached — hence `self-review: budget-exhausted`.
- **CodeRabbit review of `e7a943a4`** — 9 inline findings and 1 outside-diff finding, each replied on its thread and resolved; all fixed in `7bde4251`. They were: unknown epic answering `target_not_found`; an enrolled project without a store answering `project_not_enrolled`; a failed act's banner cleared by another act's success; a timed-out placement claiming "not in force"; the onboarding announce predicate cleared before the request; a list answer applied over a later read event; retention removed under a live hold; the race test's inconsistent page; a failed retry not announced; and the decomposition read failure rendered only when a draft existed.
- **SonarCloud** — at `4e4e5cfc`, two `java:S1192` new-code issues on messages the code swap made identical, fixed in `abd56a83`; the gate then failed on new-code coverage (58.6%, then 79.3%, then 79.5%) until the changed 404 branches and the new guards were covered, reaching 81.8% at `9760148b`.
- **Sourcery** — refused on quota for the optional reviewer (250k diff characters per seven days).

## 5. Reviewers

- **coderabbit** (required, `quota_limited`): reviewed head `e7a943a4` — a qualifying review (`final_review_risk_coverage` carries `sourceCommitId`/`coveredCommitId` = `e7a943a4`, `kind: reviewed`), `COMMENTED`. Every finding replied on its thread and resolved. Re-triggered after the pushes to `9760148b` and `935db6e1`; both times it answered rate-limited ("1 review per hour, 0 remaining"), which is the quota policy's recorded silence. The standing authorization applies.
- **sourcery** (optional): refused on quota at 2026-11-11T09:46Z (next request in 13h); its check reports SKIPPED. Optional, not gating.
- The merge gate's `review-gate` verb answered `merge_allowed: false` with `undecidable_read` — its parser cannot read CodeRabbit's informational "Files with no reviewable changes: 8" block in the review body — not because a finding is unhandled. The operator selected "Merge despite the undecidable read" on that exact gap. The three review numbers in the facts block are therefore from `ci pr comments` on the merge head `935db6e1`, not from the gate's own read: 0 unresolved threads (all nine replied and resolved) and 1 review-body finding (the outside-diff one), handled in `7bde4251`.

## 6. Operator questions

1. "How should the pull request handle it?" — the five act surfaces still printing a raw code that D1's list did not name. Options: *Close them now in this PR* / *Ship with the residue recorded*. Selected: **Close them now in this PR**.
2. "Self-review round 3 … still reports items open … How do you want to proceed?" — Options: *Close them all, then open the PR* / *Only the regression I introduced* / *Open the PR with the findings recorded*. Selected: **Close them all, then open the PR**.
3. "The merge gate's review condition cannot decide … How do you want to proceed?" — Options: *Merge despite the undecidable read* / *Stop and leave the PR open*. Selected: **Merge despite the undecidable read**.
- Free text: the operator instructed "read mailbox", which led to the correct mailbox (`rb-158-web-error-pattern-residue`) and the advisory above; the earlier reads of the wrong box were void and are corrected in the progress file.

## 7. Rule check

- **guard: none on this harness (unenforced).** The lane's guard log (`.plan/temp/guard.log`) carries nothing for this run because nothing enforced rules; the check below is the run's own account, not a log count.
- Rule breaks the run made itself: (a) one routed build ran the `test` canonical against the main checkout without `--project-dir` at the start of the work; it changed nothing and was superseded by the worktree builds; (b) many targeted file inspections used the shell tool (`grep`, `sed -n`, `head`, `tail`, `ls`) instead of the file tools — every one a read of a repository file, no writes; (c) one stray no-op shell command. No staging of everything, no stash, restore, rebase or forced push; no `.plan/orchestrator` writes; no edit outside the worktree.
- The spec read: `spec_source: ledger_worktree`, no `spec_differ` marker at either resolve — the ledger worktree's copy and the main checkout's agree.
- `executor-skew`: `match` at the start (executor and lane text both `0.1.2090`); the executor was `0.1.2106` at landing after main's upgrade.
- Restarts: one server restart during the round-3 review dispatch; that dispatch was aborted and re-dispatched, and the lane text was re-read and recorded.

## 8. Build server

Setup preflight: `ready`. Every build ran with the default routing and reports `mechanism: daemon_longpoll` — no `in_process` fallback, no launch-fault retry, no `killed` job. Builds: the module-scoped test runs while iterating, the full `verify -Ppre-commit` gate before the PR and again on each head pushed after a review fix, and the e2e profile (`verify -Pe2e -pl pm-web-e2e`) for D4. The final gate at `935db6e1` (the merge head after main moved) is green: 2996 tests, compile, lint and test.

## 9. Residue

- The mock's per-sub-resource choice of `scope_not_found` vs `target_not_found` is a judgement, now consistent for an unknown epic versus a known epic without the resource and pinned by tests; CodeRabbit's point was answered on the thread.
- The `errors → display → i18n → client` import cycle is latent only (every use is call-time) and the helper's placement in `errors.ts` is what the spec asked for.
- D2's fake-timer clause has order `none` for the reason above; the test stands and the path is covered.
- The e2e offline case and the browser's mock-stopped target show the unknown fallback because the dev server proxies `/api`; a production deployment served by the runtime itself would read `network_offline` there. Recorded, not fixed.
- The `review-gate` verb cannot read CodeRabbit's informational file-list block; the operator authorized the merge on that exact gap. A future run could teach the parser that section.
