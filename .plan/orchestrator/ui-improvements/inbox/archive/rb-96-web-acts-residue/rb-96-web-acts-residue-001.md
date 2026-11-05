envelope_version=1
sender_type=plan
sender_id=rb-96-web-acts-residue
epic=ui-improvements
kind=landing
created=2026-11-05T06:46:06Z

```landing-facts
schema=landing-facts/1
plan_id=rb-96-web-acts-residue
epic=ui-improvements
pr=#320
merge_state=merged
cleanup_owed=false
deliverables_total=4
deliverables_done=4
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2036
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
step.sonar-roundtrip.open_issue_count=0
step.sonar-roundtrip.open_issue_rules=none
```

`guard: none on this harness (unenforced)`

# PLAN-96 landing — web acts residue

PR #320 merged into `main` by the merge queue as `6cf1aac00f13c0c63eed601873f1494597837e9f`.

## 1. Deliverables

Every Done-when met as written. Orders as the spec asks.

**D1 — Confirmation and focus keep their promises**
- Epic cancel asks and sends nothing until confirmed — met; test `asks before an epic cancel and sends nothing until it is confirmed`; `test-first`; failing run `c7d9dcb8` (`maven-2026-11-04-194803.log`); commit `5a63965`.
- Focus returns to the opener after a confirmed pause — met; `gives the focus back to the opener once the confirmed act settles`; `test-first`; failing run `c7d9dcb8`; `5a63965`.
- Focus moves to the first enabled control when the act removed the opener — met; `moves the focus to the first control that can run when the act removes the opener`; `mutation-after` (the browser check found it); commit `58384ee`.
- Double confirm ignored and Escape/cancel inert while busy; `SettingsView` passes it — met; `ignores a double confirm and keeps Escape and cancel inert while the act runs`, `holds the revoke confirmation open and ignores a second confirm while it runs`; `test-first`; failing run `c7d9dcb8`; `5a63965`.
- "Stop now" and withdraw show a refusal and ignore a second click — met; `asks before Stop now and clears the drain when the runtime takes it`, `shows the refusal of Stop now and ignores a second confirm while it runs`, `shows the refusal of a withdraw and ignores a second click`; `test-first` (withdraw) + `mutation-after` (Stop now); failing run `c7d9dcb8`; `dfef6d9`.
- End dialogue trap, `aria-modal`, consequence — met; `keeps the focus in the end dialogue and names its consequence`; `test-first`; failing run `c7d9dcb8`; `5a63965`.
- Browser check (Playwright MCP, ports 20960/20961, `de`/`en`, light/dark, 1440×900 and 390×844, before and after) — `none`; before (main checkout, code-identical to the base): no confirmation, focus on `body`, `2 left: running work` with no list; after: the confirming dialogue, focus on the first enabled act, named items shrinking from 2 to 1, the Stop-now confirmation.

**D2 — The overflow acts and the row menu are usable**
- Disabled act's reason as described-by text and arrow-key reach — met; `names the reason of a disabled entry and keeps it reachable with the arrow keys`; `test-first`; failing run `5a63965` (`maven-2026-11-04-200226.log`); `109e5bb`.
- Row-menu button measured against its cell at 1440 and 390 — met; `keeps the row-menu button inside its cell at 1440/390 in en/de`; `mutation-after`; reverted column failed at `109e5bb` (`maven-2026-11-04-201639.log`); green in `maven-2026-11-05-014042.log`.
- 44 px overflow entry at 390 — met; `gives an overflow entry a 44 px target on a phone`; `mutation-after`; reverted CSS failed at `109e5bb`; green at `dfef6d9`.

**D3 — The drain banner lists what runs and counts down**
- Store/layout test and `system.spec.js` assert list, count, and the count falling — met; `names what the drain still runs and lets the list shrink as work ends`, `caps the names at three and counts the rest`, `lists what the drain still runs, counts it, and shrinks it as it ends`; `test-first`; failing runs `c7d9dcb8` (`maven-2026-11-04-194803.log`) and `026f92a` (`maven-2026-11-04-205654.log`); `026f92a` green.
- Dead handlers and rows gone, no unreachable branch — met (`none`); Grep finds no `withdrawStop`/`withdraw_stop`/`stop_now`/`RUNTIME_ACTS.upgrading|stopped` reference; commit `026f92a`.
- The countdown follows `shared/system.adoc#draining`: "What is left by name, at most three, then `+2 more` … the line shrinks as each ends" — the count is the list's own length and falls as items leave.

**D4 — Act ids and the tests that guard them**
- Each item fails when reverted — met; `mutation-after`; the D4(a) and actsFor tests failed with the mutated `byState` (`maven-2026-11-04-211121.log`, 6 failed), the helper meta-test failed with the old silent-skip helper (`maven-2026-11-04-211231.log`), the strict revoked test failed without the revoked flag (`maven-2026-11-04-212051.log`), and `system.spec` failed without the mock items (`maven-2026-11-04-205654.log`); restored green.
- Open Sonar issue read at the PR head — met (`none`): `sonar read_open_issues` at `5127d97` returned `confirmed` `0`; analysis revision equals the head; SonarCloud Quality Gate passed.

Per-head test runs (`module-tests`; `test-compile` is not a part of this project's build): green at `5a63965`, `109e5bb`, `026f92a`, `20c7af3`+`58384ee`, `dfef6d9`, `9a794b1`, `e89bc61`, `eea5204`, `5127d97`.

## 2. Personas and skills

- Persona applied: `plan-marshall:persona-implementer` for D1–D3 (production web and mock code); `plan-marshall:persona-integration-tester` for D4 (the cross-surface tests); `plan-marshall:persona-code-reviewer` for every self-review round and for triaging every PR comment.
- Domain skills per deliverable: D1 `pm-dev-frontend:javascript`, `pm-dev-frontend:css`, `pm-dev-frontend:jest-testing`, `pm-dev-frontend:javascript-security`; D2 `pm-dev-frontend:javascript`, `pm-dev-frontend:css`, `pm-dev-frontend:jest-testing`; D3 `pm-dev-frontend:javascript`, `pm-dev-frontend:jest-testing`, `pm-dev-java:java-core`; D4 `pm-dev-frontend:javascript`, `pm-dev-frontend:jest-testing`.
- Surface drift: none beyond the Expected Surface — no extra module skill was needed. `skills: none added`.

## 3. Hypotheses

- HYPOTHESIS "the mock never sends `drain_remaining_items`" — confirmed at `MockRuntimeEngine.java:5413` (count only) and `:8274` (remaining_work_count only); the real daemon was not checked (another repository).
- HYPOTHESIS "the dead handlers of `SystemView` cannot run" — confirmed, `SystemView.svelte:391` hides the bar for draining/upgrading/stopped while the switch cases at `:404-429` handled them.
- HYPOTHESIS "the row-menu overhang is still 50 px" — the defect confirmed by measurement; the overhang was ~100 px in a 60 px cell at 1440 (measured failing e2e before the fix), not exactly 50 px.
- OBSERVED claims in the spec (epic cancel straight through; overflow reason as `title` only; confirm calls `closeDialog` then `handleAction`) — confirmed at `WorkControlPanel.svelte:263-282`, `ActionBar.svelte:220`, `WorkControlPanel.svelte:284-304`.

## 4. Findings

- Self-review round 1: M1 (`confirm_now` invented a task/job split), M2 (epic plans count defaulted to 0), M3 (drain list promised a step), M4 (new texts missing from the design docs), M5 (racy shrink assertion), m1 reason requirement, m2 duplicate reason ids, m3 trap/modal mismatch, m4 German width unmeasured, m5 danger-colour scope, plus n4/n7/n8 — all fixed in `b4872df`.
- Self-review round 2: D1 Stop-now refusal test, the German measurement race, the reason decision (documented), the duplicate hint, the danger-text scope, the conflict between two design sentences — all fixed in `58384ee`/`dfef6d9`; round 3 reported "no open findings".
- CodeRabbit: 12 actionable comments and 6 nitpicks on the first heads, plus 5 follow-up findings on the later heads — every one fixed or answered with a reason (the timer-driven shrink test was answered: the direct `shrinkDrain()` test and the scenario-event e2e cover it; a wall-clock test would only add timing dependence). All review threads resolved; the dispositions are on the PR.
- CI failures seen and resolved: `build / build (26)` and `quarkus-alignment` failed on the type error in the layout tests (fixed `c8d0d08`); Sonar failed on S3077/S1192 (fixed `1b0339b`, `eea5204`) and on 59.4% new-code coverage (the shrink test, `75a627c`); a run of `designer.spec` and `reports.spec` flaked on unrelated pages and passed on re-run.

## 5. Reviewers

- `coderabbit` (required, quota-limited): reviewed heads `75a627c`, `9a794b1`, `eea5204` with `kind=reviewed` markers; every finding handled; re-triggered after the last push (`5127d97`) with the trigger comment at 01:44 and got a rate-limited/NEUTRAL silence, recorded. Standing review authorization applied for the merge head (`5127d97` = `eea5204` + the origin/main merge + a formatting-only commit).
- `sourcery` (optional, quota-limited): refused on its 7-day budget (6 days 22 h remaining); no review, not required.

## 6. Operator questions

1. "PR #320 is ready to enqueue … Merge the PR?" — options "Merge now", "Hold"; selected **"Merge now"**. The question stated the head, CodeRabbit's reviewed head, CI, verify and Sonar state read in that step.

No other question was needed; there was no free-text operator message.

## 7. Rule check

- `guard: none on this harness (unenforced)` — the resolve result reported `guard: none`; the run therefore does not claim "all kept" and no guard-log line was read as evidence.
- Executor-skew verdict: `match` (executor 0.1.2036, lane 0.1.2036), read before `resolve`.
- Spec read: `spec_source: ledger_worktree`; `spec_differ` was not reported.
- The run's own shell actions: every script went through `python3 .plan/execute-script.py …`; every build through the resolved Maven executor; every CI action through `ci`. No `gh`/`glab`, no direct build wrapper, no test binary, no `python3 -c`, no `git grep`, no `sleep`. Temp files stayed under `.plan/temp/`. One accidental flag typo (`--execution_mode`) was rejected by argparse and re-run correctly; no other shell rule was bent.
- Builds: the build daemon answered `down` at setup and `ready` later; the first routed build failed `execution_failed` because the daemon could not run `mvn`, so every build ran with `--execution-mode in_process` (recorded degradation). One daemon-routed build at a time; none backgrounded.
- Cleanup verified with `git status --porcelain` (empty), then the worktree removed without force and the refs pruned by the git-workflow verb.

## 8. Build server

- Setup preflight: `down` (`reason: unreachable`).
- First routed attempt later: `ready`, but the routed job failed instantly with `execution_failed` (the daemon's environment could not run `mvn`, exit -1); fallback `in_process` for every build, reason recorded.
- No `killed` job.
- `runtime_stamp_reason` was not reported; `plugin_version` is `unknown`, as the resolve result returned it.

## 9. Residue

- `pm-operator`'s `ConsoleSettingsTranscriptTest.machineSettings` failed on 2026-11-04 with a date-dependent transcript (`last used 8 Sep` vs `<ago>`) and reproduces on the base checkout; the base's own merge (PLAN-93/94 line) fixed the transcript, and the full verify is green on the merge head.
- The mock still ignores the epic-cancel `with_plans`/reason payload (pre-existing, recorded in the epic ledger); the browser check could not observe the choice taking effect.
- The optional Sourcery review is unavailable for ~7 days on its budget.
