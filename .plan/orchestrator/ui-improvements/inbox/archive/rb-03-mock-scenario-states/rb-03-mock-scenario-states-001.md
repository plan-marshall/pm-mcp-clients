envelope_version=1
sender_type=plan
sender_id=rb-03-mock-scenario-states
epic=ui-improvements
kind=landing
created=2026-10-28T13:45:47Z

```landing-facts
schema=landing-facts/1
plan_id=rb-03-mock-scenario-states
epic=ui-improvements
pr=#253
merge_state=merged
cleanup_owed=false
deliverables_total=3
deliverables_done=3
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2021
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
step.sonar-roundtrip.open_issue_count=0
step.sonar-roundtrip.open_issue_rules=none
```

Guard state: `guard: none on this harness (unenforced)` — nothing was enforced, so section 7 records that instead of "all kept".

## 1. Deliverables

**D1 — State facts applied by goto — met.** Every state of the three scenarios carries the facts it stands for; `goToState` re-seeds the domain stores from them.

- `ScenarioStateContractTest.everyStateReachesTheResourceItConcerns` — order `test-first`; failing run job `214e43fe66a24510970589764a5d7865` at `c4dc9bb4` (control.holds, projects.attention, cleanup.completed did not change); passed at `0598dc3`, then at every head.
- Browser check — order `none`; 24 after + 24 before combinations (3 scenarios × de/en × dark/light × 1440/390), 82 states each, text per state plus one screenshot per target per combination. `projects-and-start` went from 0–2 of 26 states changing their view to 19 of 26; `running-work` 28–30 → 30–32; `reports-and-system` 6–12 → 14 of 18.
  - **Exception recorded, not hidden:** `system.running`, `system.stopped`, `system.restarting` and `system.force-stop` change the API resource (D1's test proves it) but no rendered view. They carry no domain facts, and `pm-web-app` renders the lifecycle only as the draining banner. That is a web-app gap outside this plan's Expected Surface and Write-Boundary.

**D2 — Fixture consistency — met.**

- `FixtureConsistencyTest` (12 tests) — order `test-first` for the original pairs (job `e1819303803648d6acbf4c50bf410917`, 7 of 9 failing) and `mutation-after` for the pairs added during the review rounds: roles (job `bae77a0fd4f14612898839c81c3929a3`, `31` vs `36`), delete result (job `0ba83665e5274718a02abf2f4e60dfde`, `1997956704` vs `1825361100`), exclusion (job `706a95fb43e645a6b9573dcafb020bd4`, `1872127584` vs `1997956704`).
- The cleanup figure is now single-sourced from the job (delete response, events, system snapshot), the delete result subtracts what the operator unchecked, and the model count the onboarding step names matches the machine settings.
- Browser check — order `none`; same walk as D1.
  - **Known and reported, not fixed:** CC-F-09's "12 plans in summary vs 3 detail rows" is left as it stands. The 12 plans / 2 epics / 40 logs are the *specified* summary (`doc/design/terminal-client/26-cleanup.adoc`, `doc/claude_review/cli/resources/screens/*`) and are asserted by `pm-operator` tests; the item lists are a page of rows. Changing the counts would contradict a design document and break operator tests, so it is outside this plan.

**D3 — README and scenario descriptions — met.**

- Order `none`; the six scenarios of `primary.txt` are listed, and every use-case id named by a scenario description exists in `plan-marshall-documentation/doc/use-cases/`.
- The open-issue read at the head is recorded: `0` confirmed at `46673b5e`.

Every pushed head was tested: `verify -Ppre-commit` at `db844da`, `8bf8de1`, `7542e4e`, `7c51e8f`, `14f6171`, `85b266b`, `97917f8`, `5a53e4e`, `6f20378`, `461db79`, `8cf8cfb`, `6a001a9`, `2fa0427`, `34cb1ef`, `f8206fd`, `46673b5e`; `verify` at `db844da`, `7c51e8f`, `461db79`, `8cf8cfb`. The integration suite ran on every head through CI (951 tests).

## 2. Personas and skills

- D1, D2: `plan-marshall:persona-implementer`; skills named by the spec: `pm-dev-java:java-core`, `pm-dev-java:java-lombok`, `pm-dev-java:junit-core`.
- D3: `plan-marshall:persona-documenter`; skills `pm-documents:ref-asciidoc`, `pm-documents:ref-documentation`.
- Self-review rounds and every PR-comment triage: `plan-marshall:persona-code-reviewer`.
- `skills added`: none — no deliverable touched a module outside the Expected Surface.

## 3. Hypotheses

- HYPOTHESIS "most views read domain data from the engine, and `goto` changes only the offered links" — **confirmed** at `MockRuntimeEngine.java:1042`/`:1050` (the jump recorded links and scheduled events; the stores were seeded in `setScenario`/`reset`).
- HYPOTHESIS "the e2e specs of `pm-web-e2e` carry their own fixtures and do not depend on the engine fixtures" — **refuted**; `pm-web-e2e/tests/*.spec.js` assert the mock's scenario values through its API, so the changed values were updated in the same pull request.

## 4. Findings

- Self-review round 1 (10 findings): 6 fixed (roles 36→31, `goToState` javadoc, README designer claim, preview-job assertion, `projects_count`, three freed-bytes constants); 3 deferred with reasons recorded (cleanup rows vs counts, `facts.job` validation order, `process.*` resource by key order); 1 was a false positive (import blank lines — the pre-commit recipe enforces them).
- Self-review round 2 (7 findings): 4 fixed (the delete result ignored `unchecked_ids` — a regression I introduced —, the missing exclusion test, the roles test covering only two states, the javadoc pointing at a private method); 2 recorded as outside the boundary (a design doc still says 1.8 GB; a pm-web-app unit test still says 36 roles); 1 fixed (the snapshot kept a second hardcoded summary).
- CodeRabbit, first review (8 findings, all addressed in commits and its threads resolved by the bot): optional fields kept null in `CleanupFixtures`; `goToState` half-seeded on a malformed fixture plus an unmapped `IllegalStateException`; model count 3 vs 4; a plan's remaining holds after `end_plan`; four e2e assertions using partial text matching.
- CodeRabbit, second review (2 more): the rollback discarding operator changes — fixed at the cause (facts are validated before any store is cleared) and the thread answered and resolved; the SSE-reset note — answered, not implemented, because it changes the jump's documented event order and broke `MockRuntimeTest.serverSentEvents`.
- Sonar: 8 issues on the first analysis, driven to `0` confirmed at the merged head. One (`javabugs:S2259`) survived six rewrites of the flagged construct and the scanner's line report did not move with the code; the actual cause was found when the annotation was read: `applyCleanupFacts` accepted a `@Nullable` facts map, which is now null-tolerant. The remaining duplicate-literal smells were named constants.

## 5. Reviewers

- **coderabbit** (required, quota-limited): reviewed `33737ba` and `6a001a9` (`kind=reviewed`, `final_review_risk_coverage` names `6a001a9`); not the merged head `46673b5e`. Re-triggered after the last push → refused "Review rate limited" (recorded 13:10Z). Every finding from both reviews is fixed or answered, and every thread is resolved. Standing review authorization applied, with the operator's explicit word to merge.
- **sourcery** (optional): triggered twice, refused on its own 7-day budget (5 days and 14 hours remaining) and its check is SKIPPED — it never gates the merge.

## 6. Operator questions

1. "executor-skew answered could_not_check (executor 0.1.2021, lane version not derivable)" — options {Proceed, Stop and regenerate}; selected **Proceed** (the earlier session's question at 0.1.2017 had the same answer).
2. "The browser matrix is the only slow part. How should I proceed?" — options {Full matrix as written, Baseline on one combination, No baseline, Stop}; selected **Continue the full matrix as written**.
3. "PR #250 is green except SonarCloud … `sonar_rest transition` answers HTTP 400" — options {You dismiss it, Grant issue administration, Keep trying to fix, Stop with the PR open}; selected **Keep trying to fix the issue**.
4. "The code finding is fixed and cannot be fixed further — the analyzer's report is stale" — options {Dismiss in the UI, Grant token, Reset the analysis}; answered with free text: **close #250, rebase on main and open a new PR** — done (`main` had not moved, so this was a branch/PR change on the same commits).
5. "The build is red. Fix this first" (free text) — the remaining issue was fixed at the source and the build went green.
6. "All gates except a CodeRabbit review of the current head are green … how do you want to proceed?" — options {Merge now, Wait for a review of this head, Stop with the PR open}; selected **Merge now via the merge queue**.

No question tool was unavailable at any point.

## 7. Rule check

- `guard: none on this harness (unenforced)` — no guard log exists for this run, so nothing is claimed as "all kept".
- Spec read: `spec_source: main_checkout`, no `spec_differ` marker (no ledger copy was used).
- Deviations of the run's own actions:
  - Shell file operations despite the file-tool rule: `ls`/`wc` in the earlier session; this session `ls`/`grep -c`/`sed -n` for line ranges, `tail` of two build logs outside the repository (the log the build result names), and one `perl -pi -e` rewrite of `MockRuntimeEngine.java` that should have been an `edit` call.
  - `git` operations: `git branch --list` and `git worktree remove` (both sanctioned); no rebase, no force-push, no `git stash`/`restore`.
  - `cd` into the worktree inside one shell call for the `perl` rewrite (the lane's shell form is one command per call from the main checkout).
  - A routed build that hit its own 920 s bound (`timeout`, `timeout_source: explicit`) and was re-run with 1800 s rather than split; the project publishes no split parts.
  - One unrelated flake: `pm-relay` `PmMcpTest.worker` failed once on a null request and passed on re-run; the module is untouched by this plan.

## 8. Build server

- Setup preflight: `ready`. Default routing throughout; every build was daemon-routed (`mechanism: daemon_longpoll`), none degraded, none killed.
- `runtime_stamp_reason`: the first `resolve` reported `plugin_version: unknown`, and it is still `unknown` at landing time — the runtime-info seam could not measure it.

## 9. Residue

- Branch `feature/03-mock-scenario-states` (PR #250, closed unmerged) still exists locally and on the remote; the lane forbids `git branch -D`, so it was left in place.
- The four lifecycle states remain invisible in `pm-web-app`; a web-app plan has to render them.
- `doc/design/terminal-client/26-cleanup.adoc` still says "1.8 GB" where the fixture now says 1.9, and `pm-web-app/src/lib/views/MachineSettingsView.scenarios.test.ts` still uses `36 roles` as an input value.
- CC-F-09's plans-rows pair is a design decision recorded above, not a defect this plan fixed.
