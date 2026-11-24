envelope_version=1
sender_type=plan
sender_id=rb-183-pm-api-and-mock-hygiene-residue
epic=ui-improvements
kind=landing
created=2026-11-24T11:14:29Z

```landing-facts
schema=landing-facts/1
plan_id=rb-183-pm-api-and-mock-hygiene-residue
epic=ui-improvements
pr=#553
merge_state=merged
cleanup_owed=false
deliverables_total=2
deliverables_done=2
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2183
steps=setup:done,implement:done,build-gate:done,self-review:budget-exhausted,pr:done,reviews:done,merge:done
unresolved_review_threads=0
review_body_findings_total=13
review_body_findings_handled=13
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
```

guard: none on this harness (unenforced)

# PLAN-183 — landing

PR #553 merged through the queue as `1c6464892797893ada148065ce551f953cd19e2e`, after two base merges (`2daee400`, `2e966088`) and a head that ended at `2e966088`.

## 1. Deliverables

### D1 — a plan's phase state is the closed activity vocabulary: met

- "a record with each `PlanActivity` wire value reads": met. `ProcessRecordsReadingTest.shouldReadTheClosedActivitySetAndRefuseAnotherValue`, order `test-first`; failing run log `b555359888a94eaa9e64adfaa398e5b7` at the pre-fix tree ("MultipleFailuresError (1 failure)", the reader kept `done`). Commit `c3d3af36`.
- "a value outside the set is refused": met. Same method; `assertThrows(JsonFormatException.class, … fromMap(phase_state=done))`, and `assertThrows(IllegalArgumentException.class, … new EpicProcessPlan(…, "done", …))` — the second assertion's failing evidence is log `c4117ff9ee0c4a56863b74bd2a57fd91` with the constructor guard reverted. Commit `14c3fa20`.
- "a name-spelled member reads as its canonical wire value": met, `mutation-after`, log `e57e48b16bed4799af4e42b747062f84` (`expected: <ended> but was: <ENDED>`). Commit `14c3fa20`.
- "a test of the mock proves `synthesizedPlan` no longer serves `done`": met, `mutation-after`, log `6c1da848f1ec4ec9806de47c772e059a` (`expected: <ended> but was: <done>`, `planStateOf` reverted on `c3d3af36`). Tests `ControlScenarioTest.endedRowServesEndedActivity` and `PlanDetailTest.servesAPlanListedInsideAnEpic`.
- "`waiting for you` reaches `phase_state` as the vocabulary's spelling": met, `mutation-after`, log `362d5a8aff3d456b87f838a908b13619` (`expected: <waiting_for_you> but was: <waiting for you>`). Test `ControlScenarioTest.waitingRowServesTheVocabularySpelling`. Commit `1bd461d4`.
- "the existing `ProcessRecordsReadingTest:244-247` changes with the decision": met — it feeds `done` and asserts the refusal.

### D2 — the mock's epic `cancel` and `next` do what they answer: met

- "`cancel` with `with_plans: true` shows its plans ended on the next read and `with_plans: false` leaves them": met, order `test-first`; failing run log `dd828c2c2d764121a63d1db8ffbc3ead` at `c3d3af36` (5 failures). Test `ControlScenarioTest.cancelWithPlans` (both arms, the plans' own `/actions`, the per-plan `plan.cancelled` on the plan's stream, the landed row keeping `shipped`). Commits `9886ea9a`, `1bd461d4`.
- "a landed row keeps the outcome it ended with": met, `mutation-after`, log `22522208b70142848badae7c4419816d`.
- "the per-plan `plan.cancelled` event": met, `mutation-after`, log `4db36524a1854380802848d6ee8cf832` — with publication removed both event tests fail at the 30-second JUnit bound instead of blocking the suite.
- "`next` shows a started row with its event, or an answer that says none started": met, order `test-first`, log `dd828c2c2d764121a63d1db8ffbc3ead` (`expected: <relay-reconnect-metrics> but was: <null>`). Test `ControlScenarioTest.nextPlan`.
- "`next` never starts a row whose plan already ended": met, `mutation-after`, log `9f8f28f4222c4585a59fab1f138bfd25`. Test `ControlScenarioTest.endedPlanIsNotStarted`. Commit `64eb8984`.

Per-head runs against pushed heads (module tests, plus the full gate where run): `c3d3af36` scoped `verify -Ppre-commit`; `9886ea9a` the same; `1bd461d4` the same; `1b19729c` full mock suite; `dc3608e0` full `verify` + `-Pcoverage` + `-Ppre-commit`; `ca6b8c2e` full `verify`; `14c3fa20` mock suite; `0b121ac3` full `verify`; `2daee400` full `verify`; `64eb8984` full `verify` (log `9d98b4d9c0f74f928caf11031e8f3854`); `2e966088` full `verify` (log `c2e1bf7546194965b69dc9859db249e5`). CI ran on every push and was green on the merged head `2e966088` (all 20 checks).

## 2. Personas and skills

- Production code (both deliverables): `persona-implementer`. Self-review rounds: `persona-code-reviewer`. PR-comment triage: `persona-code-reviewer`.
- D1 skills: `pm-dev-java:java-core`, `pm-dev-java:javadoc`, `pm-dev-java:junit-core`, `pm-dev-java-cui:cui-testing`.
- D2 skills: `pm-dev-java:java-core`, `pm-dev-java:junit-core`, `pm-dev-java-cui:cui-testing`.
- skills added: `pm-dev-java:junit-core` (already named for D1), reason: the surface drifted into `pm-operator`'s test tree, where the epic process view is asserted.

## 3. Hypotheses

- `EpicProcessPlan` documents `phase_state` as running/staged/planned/done and reads it as a free string — confirmed, `EpicProcessPlan.java:26` and `:79` at the branch point.
- `PlanActivity` holds the closed set and `parse("done")` throws — confirmed, `PlanActivity.java:20-27,66-72`.
- `planStateOf` serves a plan `phase_state` of `done` for a shipped/merged/landed row — confirmed, `MockRuntimeEngine.java:12133-12138` via `planRowSummary:12125` and `synthesizedPlan:12193`.
- The epic action handler ignores `withPlans` and lets `next` fall to the default branch — confirmed, `MockRuntimeEngine.java:5983-5986` before the change.
- The engine's contract promises both effects — confirmed at `pm-mcp-core/doc/specification/model-work/08-epic-coordination.adoc:223,228`, cited by the spec.

## 4. Findings

- Self-review round 1 (22 findings: 7 major, 9 minor, 6 nit): fixed 1 (off-vocabulary row statuses now mapped), 4 (a cancel leaves an ended plan as it ended), 5 (the cancel event asserted), 8 (canonical value on read), 9 (scope for every launchable row), 10 (a moved row drops its overlap files), 11 (an unparseable wait is no can-start), 14 (vacuous assertion dropped), 16 (JUnit timeout on both event reads), 19 (test name). Reported 2, 3, 6, 7, 13, 21, 22. Commit `1bd461d4`.
- Self-review round 2 (3 major, 11 minor, 4 nit): fixed the cancel's row/plan divergence, the split wait predicate, the stale Javadoc, the unparseable wait in `not_started`, and two new tests. Commit `1b19729c`; reverted the `running` overlay in round 3.
- Self-review round 3 (1 blocker — the gate trio, since run; 2 major — the `running` overlay changed a scenario's authored behaviour and split the resource/list pair, and an abandoned plan's row became `cancelled`; both withdrawn in code; 4 minor, 6 nit): commit `dc3608e0`. No fourth round ran, so the run reports `self-review: budget-exhausted` rather than clear; the residual items are the reports below and the nits it named.
- CodeRabbit, first review (`dc3608e0`): 7 inline threads + 2 body items. Fixed: Javadoc list → `{@link PlanActivity}`; constructor contract; scenario-ended plan guard; omitted-scope seed; ended plan out of the running count; flat epic cancel routed. Kept as designed with a stated reason: staged plans end with the epic (the archive check refuses a staged row under an ended epic) — the bot withdrew that finding. Reported with a reason: the CLI's `next_started` message (client surface) and the transition-ordering refinement.
- CodeRabbit, second review (`94a50d19`): `completed` as a shipped row status → mapped to `ended` (`0b121ac3`); scope-only waits reach the capacity check (`0b121ac3`).
- CodeRabbit, third review (`2daee400`): already-ended plans excluded from selection and from the answer (`64eb8984`); the resource's running count reported rather than overlaid.
- Sonar, head `dc3608e0`: 4 new-code issues (two S1192, one S3824, one S1612) — all fixed in `ca6b8c2e` and `94a50d19`. Sonar at the merged head `2e966088`: 0 open new-code issues, 0 hotspots, quality gate passed, 90.5 % coverage on new code.

## 5. Reviewers

- coderabbit (required, `quota_limited`, `review_complete_after_handling`): reviewed `dc3608e0` (formal review, COMMENTED), `94a50d19` and `2daee400` (summary reviews with the `kind=reviewed` marker naming the covered commit). Refused on its free-tier review limit twice ("Review limit reached", next window 15/31 minutes); re-triggered once at `01:25Z` and once on request, which produced the second and third reviews. Every finding handled; `review-gate` reports `merge_allowed: true` with 13 of 13 body findings disposed and 0 unresolved threads. Its review of the final head falls outside the win; the done-exemption applies as the lane states.
- sourcery (optional): refused on its own budget ("2 days and 12 hours", 250k diff characters) — never reviewed. Recorded, not gating.

## 6. Operator questions

1. "PR #553 is green everywhere else, but `review-gate` refuses the merge on one review thread…" — options: "Wait for CodeRabbit, then merge", "Merge on the recorded evidence", "Stop and leave the PR open". Selected: "Merge on the recorded evidence" (for head `0b121ac3`).
2. "The earlier 'merge on the recorded evidence' selection was made at 0b121ac3, which is no longer the head…" — options: "Merge this head (Recommended)", "Wait for a review of this head", "Stop and leave the PR open". Selected: "Merge this head" (for `64eb8984`).
3. "The selected head 64eb8984 has been superseded once more…" — options: "Merge this head (Recommended)", "Stop and leave the PR open". Selected: "Merge this head" (for `2e966088`).

No free-text operator message was used as a selection.

## 7. Rule check

- guard: none on this harness (unenforced); `.plan/temp/guard.log` carries no line for this run because nothing is enforced.
- Own actions that broke a section 1 rule:
  - one shell append (`printf … >> .plan/temp/rb-183-…/progress.md`) to record the PR number — a shell file operation with a redirect, where the file tools were required;
  - one shell chain with `;` and `2>/dev/null` (`python3 .plan/execute-places.py …; python3 .plan/execute-script.py …`) whose first part was a typo that ran nothing.
  - Everything else used the file tools; every script call went through `.plan/execute-script.py`; every build through the resolved `maven run` commands; every CI action through `ci`; no `sleep`.
- Restarts: none. Killed sub-agents: none. Re-dispatches: the self-review agent ran three rounds in one session.
- Spec read: `spec_source: ledger_worktree`, no `spec_differ` marker at resolve or at landing.
- Executor skew: `match` (executor 0.1.2183, lane 0.1.2183) — answered, recorded, never asked about.

## 8. Build server

- Setup preflight: `ready` (project enrolled, daemon reachable).
- Builds: `daemon_longpoll` for every build (scoped gates, full `verify` ×6, `-Pcoverage`, `-Ppre-commit`); `in_process_fallback` for none.
- Fallbacks: none — no build degraded, no launch-fault retry, no `killed` job. Two client calls returned `running` at their bounded wait and were re-attached with `build_server wait`; both then reported success.
- runtime_stamp_reason: not reported; `plugin_version` came back `unknown` from the runtime-info seam, so the fact carries `unknown` rather than a guess.

## 9. Residue

- **Reported, not fixed — `raise_scope`.** The mock accepts `raise_scope` on `next` and starts nothing when the scope is full; a client's "Raise to N at once and start it" takes the 200 as success. Needs a full-scope fixture (scenario JSON was out of bounds) — recommended follow-up.
- **Reported — the epic's `running` count.** The resource serves the count its scenario authors; `next` weighs the engine's rule (a pausing or paused plan counts). Overlaying both reads from the same rows is the fix, and it changes a scenario's authored numbers.
- **Reported — a plan that ended by its own act.** Its epic row keeps what the scenario shows, so the plan's `/actions` can read `ended` while its row reads `running`; writing the plan's outcome through needs the outcome, which the control state does not carry.
- **Reported — half-covered plan reads.** `plans/{id}/process`, the plan list and the epic collection keep the scenario fixture.
- **Reported — the vocabulary is written down twice.** `ProcessTree#phaseState` stays a free string; the spec's Expected Surface named only `EpicProcessPlan`. Recommended follow-up.
- **Reported — a cancelled epic carries no `terminal_reason`**, and `state.plan.done`/`state.plan.planned` remain in the shared catalogue, unreachable from any mock path.
- **Repository wart — the pre-commit profile rewrites files this plan does not touch.** `verify -Ppre-commit` rewrites `pm-api/.../posix/Libc.java` (a `TODO` marker injected into a `catch (Throwable)`), `PosixPlatform.java` and nine `pm-operator` files (indentation, import order, `line.equals` flips, `throws IOException` → `throws Exception`). Each run's rewrites were reviewed and restored with the file tools, never committed. The paths are named here because the head does not carry them and the next `-Ppre-commit` run will meet them again.
- **Tooling defect — the resolve'd `inbox_read_command`.** It passes `--plan-id 183-pm-api-and-mock-hygiene-residue`, which the verb refuses (`invalid_target_plan`: the id must match `^[a-z][a-z0-9-]*$`); the mailbox is read with `--plan-id rb-183-pm-api-and-mock-hygiene-residue`. Every runbook-lane run on a plan whose number leads the id meets this.
- **CI flake — `EntryRecordsTest.runtimeEntryResourceRefusesAMalformedLegacyPort`** failed once on `64eb8984` with `Expected JsonFormatException … nothing was thrown` and passed on the rerun of the same tree; the same seed-dependence will recur. **CI infrastructure** — one run failed `build / quarkus-alignment` on Maven Central `HTTP 429`, passing on the rerun.
