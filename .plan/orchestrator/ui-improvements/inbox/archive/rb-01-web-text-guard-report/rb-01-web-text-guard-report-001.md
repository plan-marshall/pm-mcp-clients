envelope_version=1
sender_type=plan
sender_id=rb-01-web-text-guard-report
epic=ui-improvements
kind=landing
created=2026-10-27T13:38:49Z

```landing-facts
schema=landing-facts/1
plan_id=rb-01-web-text-guard-report
epic=ui-improvements
pr=#237
merge_state=merged
cleanup_owed=false
deliverables_total=2
deliverables_done=0
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=0.1.2021
executor_version=0.1.2017
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
step.sonar-roundtrip.open_issue_count=0
step.sonar-roundtrip.open_issue_rules=none
```

guard: none on this harness (unenforced)

# PLAN-01 web-text-guard-report — landing

PR #237 `test(e2e): add the CC-P-09 text guard in report mode`, merged into `main` as `1b39f2198a301334f86cec4dd8df668b59b58d42` through the merge queue (merge-group run 40512142038, success). Branch `feature/01-web-text-guard-report` and its remote ref deleted; main checkout fast-forwarded to the merge commit. This run is a **resume**: the branch already carried nine commits from earlier sessions when this session took over.

## 1. Deliverables

Both deliverables are reported **not fully met**, so `deliverables_done=0`. The reason is the same in three places: two clauses demand a demonstrated equality the artifact does not show, and the spec's own browser clause cannot be satisfied by the guard's rule set.

**D1 — guard helper** (`pm-web-e2e/tests/helpers/textGuard.js`, `pm-web-e2e/tests/text-guard.spec.js`)

- "A unit-level spec feeds a page with one violation per rule and gets six named violations" — **met**. Test `Text guard rules › reports one violation per rule, named by rule` (`pm-web-e2e/tests/text-guard.spec.js:35`). `order: test-first` — written against an empty helper at commit `3be32fc9`: `✘ 139 ... reports one violation per rule, named by rule`, `3 failed` in `maven-2026-10-26-174745.log` lines 643 and 964; green at `563cff3` (`maven-2026-10-26-175205.log`).
- "Browser check … the raw words the browser shows match the baseline entries the guard lists" — **not met as written**. `order: none`. Artifacts: `.plan/temp/rb-01-web-text-guard-report/browser-after.json` (10 routes × 2 locales × 2 themes × 2 viewports, 80 screenshots, 3 accessibility snapshots). The word sets are a **superset**, not an equality: the overview route shows `emoji: ☀ ⚙ 🌙 🖥` while the baseline `OverviewView.de` holds exactly `⚙` and `🖥` (`.plan/temp/rb-01-web-text-guard-report/browser-after.json:14` vs `pm-web-e2e/tests/text-guard-baseline.json:4-29`) — the theme-toggle glyphs and chip/aria text are visible to the browser and deliberately not scanned by the guard.
- commits: `563cff3d`, later hardened in `f9efcaaa`, `bc64d596`, `4c980b19`, `91edd5b6`, `b2bbe3db`.

**D2 — guard in the readability spec, report mode** (`pm-web-e2e/tests/theme-readability.spec.js`, `pm-web-e2e/tests/text-guard-baseline.json`)

- "The baseline lists at least the findings CC-F-01 to CC-F-05" — **not met as written**. `order: none`. Present: CC-F-01 (`foreign-word: ago/failed/running`), CC-F-02 (`raw-enum`, `state-id`), CC-F-04 (`iso-time`). Absent: CC-F-03 — the baseline holds **no** `placeholder` entry (`grep '"rule": "placeholder"' pm-web-e2e/tests/text-guard-baseline.json` → no match; the route titles now carry their arguments, fixed upstream); CC-F-05 has no rule among CC-P-09's six, so the guard cannot see it.
- "Adding a raw enum to one view makes the spec fail with that view and that word" — **met**. Mutation `d6ec4065` (a `waiting_for_you` literal in `OverviewView`): `OverviewView (de) gained raw-enum: waiting_for_you` and the `(en)` twin, 2 failed, `maven-2026-10-26-183209.log` lines 1559 and 1588. `order: mutation-after` — the failing output came from mutating the committed fix, then reverted.
- "Browser check" — **not met as written**, same defect as D1's browser clause; the recorded summary also predates the final baseline (summary 2026-10-26 19:41, baseline regenerated 2026-10-27 07:20).
- "The open-issue read at the pull request head is recorded … confirmed 0 or every open issue dismissed" — **met**. `sonar read_open_issues` at `ab8fa07c`: `count_status: confirmed`, `open_issue_count: 0`, `analysis_revision == head_revision`; `sonar-gate` → `merge_allowed: true`, `reason: no_open_issues`. `order: none`.
- commits: `d6ec4065`, `1513e7ec`, `b2bbe3db`.

**Per-head test runs against pushed heads**

| head | run | result |
|---|---|---|
| `b2bbe3db` | `verify -Ppre-commit` | green, 2127 tests (`-Ppre-commit` run: 2124) — `maven-2026-10-27-115400.log` |
| `b2bbe3db` | `-Pe2e … verify` | 288/290 first run (`maven-2026-10-27-120945.log`), 290/290 on re-run (`maven-2026-10-27-121903.log`) |
| `ab8fa07c` | `verify -Ppre-commit` | green, 2127 tests — `maven-2026-10-27-123708.log` |
| `ab8fa07c` | `-Pe2e … verify` | 290 passed (`maven-2026-10-27-124442.log`) |

Earlier heads were gated in the prior sessions (`maven-2026-10-27-063007.log`, `-0649*`, `-093757.log`, `-103128/103935`).

## 2. Personas and skills

- `plan-marshall:persona-integration-tester` for both deliverables' work (the spec's own `Persona:` line); `pm-dev-frontend:javascript` loaded as the domain standard for both (`Skills:` line). `skills: none named` does not apply — the spec names both.
- `plan-marshall:persona-code-reviewer` applied as the standard for triaging every PR comment and for the self-review rounds.
- `plan-marshall:opencode-target-rules` loaded when diagnosing the flat install.
- `skills added: none` — no deliverable touched a module outside the Expected Surface beyond `pm-web-app` (the `data-model-text` markings review required), which `pm-dev-frontend:javascript` already covers.

## 3. Hypotheses

- "the spec has a list of declared views the guard can iterate" — **confirmed**, `pm-web-e2e/tests/theme-readability.spec.js:28` (`DECLARED_VIEWS`, 22 names), and `GUARD_JOURNEYS` reaches exactly those 22.

## 4. Findings

- Self-review round 1: 10 findings (F1–F10) — F4, F6, F8, F9, F10 fixed; plus a journey-coverage test and case-insensitive foreign words; F5 declined (the rule regexes stay as CC-P-09 writes them; recorded as a CC-S-07 gap); F1/F2/F3 reported rather than narrowed; F7 partly open. Round 2: 4 open — N1 (invalid baseline from the port-collision runs) deleted and later regenerated, N2/N3 fixed, F7's evidence wording corrected in the PR body.
- CodeRabbit round 1 (on `1513e7ec`): 2 findings — model text unmarked, settle can accept a loading screen. Both fixed (`bc64d59`, `4c980b19`).
- CodeRabbit round 2 (on `3a65a5b2`): 3 findings — control value must be the displayed text, SettingsView must wait for its device list, CompletionReportView for its title. All fixed in `91edd5b6`.
- CodeRabbit round 3 (on `e1cd72bc`): 2 findings — the settle check must detect a lost observer and a changed read; `GUARD_LOCALES` must stay in sync with the app's `Locale` type. Both fixed in `b2bbe3db`; **this session** replied to both threads with that commit and resolved them (0 unresolved threads on the merge head).
- Sourcery: 8 + 7 findings across two reviews. Fixed: form-control placeholders/values, `checkVisibility` opacity, atomic baseline write, unsettled-return failure, stale-baseline-view check, distinct skip words, model-text markings. Declined with a recorded reason: split tokens (joining an element's inline run produced invented baseline entries `sub_workflowsub`, `remediation.ci-failureplanned`) and mock concurrency (a suite-wide port-isolation change the spec does not ask for; the operator chose to wait for the other lane).
- CI on the merge head: 30 checks, 0 failing.
- Flakes seen and re-run green, not carried as evidence: `designer.spec.js` "asks before leaving a changed draft", the SystemView/CleanupView light-narrow contrast case (verified failing on `origin/main` without these changes), and one pairing timeout at SettingsView light-narrow.

## 5. Reviewers

| reviewer | required | head reviewed | trigger / wait history | outcome |
|---|---|---|---|---|
| coderabbit | yes (`quota_limited`) | `e1cd72bc` (its `final_review_risk_coverage` marker names it) — **not** the merge head | triggered 06:53, 07:59, 10:20, 11:11, 12:02, 12:29, and once more this session; answers: "already reviewed", "Review failed", "Review rate limited" ×3 | refused on quota, then silent (420 s wait, 11 polls, 0 new comments) |
| sourcery | no (`quota_limited`) | `1513e7ec` | triggered 06:54, 08:00, 12:29 | hard quota, 250k diff characters, ~6 days; refusal recorded twice |

**Standing review authorization (CodeRabbit, quota-limited):** qualifying review exists on `e1cd72bc`; every finding from it fixed in `b2bbe3db` and both threads resolved; re-triggered after the last push with the refusal and the silence recorded; every other gate condition green on `ab8fa07c`. The operator selected "Merge now (standing authorization)".

## 6. Operator questions

This session, in order:

1. "The executor could not be compared with the lane text (`could_not_check`, reason `lane text version not derivable from its install path`). How should I proceed?" — options: *Proceed, verify the surface* / *Stop as blocked* → selected **"Proceed, verify the surface"**.
2. "`runbook_lane resolve` fails … `resolve_bundles_root` could not locate a 'plan-marshall' bundle above the OpenCode flat tree; that tree is also incomplete (`generate_executor.py` absent)." — options: *Refresh the OpenCode install, then retry* / *Run the lane from Claude Code instead* / *Override to the Claude cache* / *Stop as blocked* → selected **"Refresh the OpenCode install, then retry"**.
3. "`resolve` now fails with `build_command_unresolvable` — the architecture reports zero modules, the runtime `derived.json` data is absent." — options: *Refresh the architecture yourself, then retry* / *I regenerate the architecture (rule exception)* / *Stop as blocked* → selected **"Refresh the architecture yourself, then retry"**.
4. "Merge PR #237?" — options: *Merge now (standing authorization)* / *Wait for a CodeRabbit review of the head* / *Do not merge* → selected **"Merge now (standing authorization)"**.

Free text from the operator, separately: "retry" (twice, after the install and architecture refreshes) and "I cleaned up. regenerate the executor".

Earlier sessions' questions are recorded in the progress file: the e2e port collision (selected "Wait for the other lane to finish"), the recurring foreign mock on 7420 (selected "Wait until lane 03 is done"), and the earlier skew question.

## 7. Rule check

- **Guard:** `guard: none on this harness (unenforced)` — no pre-tool-use hook is installed for OpenCode, so nothing was enforced and no guard line exists for this run.
- **Deviations I am reporting against section 1:**
  - I read three files **outside the repository root**: two saved tool-output files under `~/.local/share/opencode/` (the tail of the loaded lane text, and one `ci pr comments` output read to avoid a truncated preview). No credential, config or environment file was read.
  - I ran `lsof -nP -iTCP:7420` and `date` — shell commands inspecting process/clock state, not file operations.
  - I regenerated `.plan/execute-script.py` with `generate_executor generate --force`, at the operator's explicit instruction ("regenerate the executor"). That is a `.plan/` write outside `temp/`, which the lane otherwise forbids.
  - I wrote the two prepared comment bodies under `.plan/local/plans/NO_PLAN/work/ci-bodies/` — the slot section 6 prescribes.
  - I ran `git merge --ff-only` in the worktree (answered "Already up to date"); no rebase, no stash, no forced push, no `git restore`/`checkout --`.
- **Routed builds:** 3 submitted, 2 failed at process start (`9798359b`, `e497dbb4`, exit `-1`, duration 0, empty log — the daemon runs bare `mvn`, the in-process path uses `./mvnw`); every later build ran with `--execution-mode in_process` and is recorded as a degradation.
- **Spec read:** `spec_source: main_checkout`, no `spec_differ` marker at setup or at landing time.
- **Restarts and re-dispatches:** the server restarted mid-run earlier (recorded in the progress file); the branch head moved twice during this session (`b2bbe3db` → `ab8fa07c`), the second time by a base merge made outside this session. No sub-agent was dispatched and none was killed. The branch was never force-pushed and never rebased.

## 8. Build server

- Setup preflight at resume: `ready` (the project is enrolled; an earlier session saw `disabled` before the operator enrolled it).
- Every routed attempt failed at process start with `exit_code: -1` and a 0-second duration in both the job log and the inner maven log — the daemon launches `mvn` while the in-process path uses `./mvnw`; recorded as `degraded` with reason `daemon_cannot_start_build_tool`, and all gates ran `in_process` (`mechanism: in_process_fallback`).
- No job was `killed`; the two failed routed jobs were re-run in-process, not blind-retried.
- One bound expiry: the maven verb's own 300 s default (`timeout_used_seconds: 300`, `timeout_source: default`); the gate was re-run with `--timeout 1800` instead of re-submitting the same bound.
- `runtime_stamp_reason`: none — the stamp resolved at landing time (`plugin_version 0.1.2021`, `executor_version 0.1.2017`).

## 9. Residue

- **The spec's Standing Constraint names a profile that does not exist**: `./mvnw -pl pm-web-e2e verify -Pintegration-tests` is a no-op here (Maven warns the profile is missing, both Playwright steps skip, `maven-2026-10-27-120412.log`). The real profile is `e2e`; the constraint should be corrected in the spec.
- **D1's and D2's browser clauses remain unmet as written** — the guard's six rules and the browser's visible text are not equal sets (chips, aria labels and the theme-toggle glyphs). Closing it needs either a rule set that covers them or a clause written as "every violation the guard lists appears in the browser snapshot".
- **CC-F-03 and CC-F-05 are not in the baseline**: `{plan}/{epic}/{project}` placeholders were fixed upstream before the baseline was taken, and CC-F-05 has no CC-P-09 rule that matches it. Both belong to the plan that empties the baseline (CC-S-07).
- **Split tokens** stay invisible to the guard by decision (joining an element's inline run invented baseline entries); recorded for CC-S-07.
- **Suite-wide mock port isolation** was declined here (it is an e2e-harness change, not this spec's); the collision it prevents cost this run several wasted hours.
- **Environment**: the OpenCode flat install still lacks scripts (`ci`, `runbook-lane`, `build-maven`, `maven`, `tools-script-executor` all resolve from the Claude install), and the routed build path cannot start Maven. Both were worked around, neither is fixed by this plan.
