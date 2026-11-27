envelope_version=1
sender_type=plan
sender_id=rb-71-verify-web-onboarding
epic=ui-improvements
kind=landing
created=2026-11-27T14:38:55Z
revision=1
amended=2026-11-27T14:39:26Z

```landing-facts
schema=landing-facts/1
plan_id=rb-71-verify-web-onboarding
epic=ui-improvements
pr=#599
merge_state=open
cleanup_owed=false
deliverables_total=3
deliverables_done=3
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2183
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:blocked,merge:blocked
unresolved_review_threads=0
review_body_findings_total=0
review_body_findings_handled=0
sonar_open_new_issues=unknown
step.sonar-roundtrip.open_issue_rules=unknown
```

# PLAN-71 (verify the web onboarding): PR #599 open, not merged

The three checks ran against `da3cafdf`, the result file and the matrix edit are pushed on
`chore/71-verify-web-onboarding` (head `c3541d93`), and the two failing checks are filed to this epic's inbox. The
pull request is **open and green** but not merged: the required reviewer CodeRabbit has never reviewed it (its review
quota refused four times), and Sonar has no analysis for this head because the project's CI skips the build for a
docs-only change. The operator chose to stop and retry the reviewer later; an automation re-triggers it at 16:07Z.

## Deliverables

Every Done-when of the spec declares `evidence order: none`.

- **D1 — run the checks: met.**
  - `UC-V-02`: verdict `fail`, with the registry test (green, 188 vitest files / 3481 tests in `pm-web-app` at
    `da3cafdf`), the 92-state Chrome walk, and the three regex hits as evidence. Order `none`.
  - `UC-V-03`: verdict `pass` on the re-grounding (the fallback names `pm-operator`, prints no state id). Order `none`.
  - `UC-V-05`: verdict `fail` on the `harness.none` row of `UC-P-03`; ten of eleven screens pass; the header and
    language clauses pass. Order `none`.
  - Artifacts: `.plan/temp/screens/` — 96 `matrix-*.png`, 92 `sweep-*.png`, the step-bar and interaction captures.
- **D2 — record the result: met.** `doc/claude_review/verification-results/verify-web-onboarding.adoc` (new) and the
  nine-row move in `doc/claude_review/use-cases/analysis.adoc` (totals 14/12/2/1/10). Commits `6928b44c`, `42d2a59e`,
  `629c9fac`, `b4600c1c`, `c3541d93`. The Done-when's `doc-review` clause is satisfied by the AsciiDoc validators
  (0 errors / 0 warnings; 9 links, 0 broken) on the operator's recorded substitution (question of 2026-11-26), as
  `verify-cli-acts.adoc` did.
- **D3 — route what failed: met.** Two `fail` verdicts, two `finding` messages:
  `rb-71-verify-web-onboarding-001.md` (revision 1, `UC-V-02`) and `-002.md` (revision 4, `UC-V-05`, the two
  `not verified` clauses, the stale `UC-V-03` text). Both validated. No repository path belongs to this deliverable.

## Personas and skills

- D1 and D3: `plan-marshall:persona-auditor` (the spec's Persona line); D2: `plan-marshall:persona-documenter` with
  `pm-documents:ref-asciidoc`. Self-review rounds: `plan-marshall:persona-code-reviewer`.
- Skills added: none; the Expected Surface held.

## Hypotheses

The spec carries no HYPOTHESIS line; its two Claim Labels are `OBSERVED` and the verify-first clause, both still true
(PLAN-64 `2fc22f3d`, PLAN-66 `98ca84d0`, PLAN-76 `15b7780c` landed).

## Findings

- Self-review round 1: 17 findings (4 major, 7 minor, 6 nit) — all fixed.
- Round 2: 7 (minor/nit) — all fixed.
- Round 3: 4 (1 major: the machine settings page does draw the conflict notice, so UC-ONB-10 moved) — all fixed.
- Confirming round: round 3's four confirmed closed; 2 nits — fixed, and the operator accepted the fixes as closed.
- CI: green. Sourcery: reviewed, nothing to fix. CodeRabbit: no review (quota).

## Reviewers

- `coderabbit` (required): refused on its review quota at 11:52Z, on the 90-minute retrigger at 13:34Z, on the
  escalated `full review` at 13:50Z, and on the reopened PR at 14:21Z. No review on any head. Per the quota policy
  #596 was closed and #599 opened; the next retry is scheduled (automation, 16:07Z).
- `sourcery` (optional): reviewed `c3541d93` at 14:21Z, "they look great", no findings.

## Operator questions

1. Asked (2026-11-26): D2's `doc-review` clause — options {Accept the validator substitution, Run the full doc-review
   skill, Record D2 as not fully met}; selected "Accept the validator substitution".
2. Asked (2026-11-27): how to close the self-review at the three-round limit — options {Run one confirming round,
   Accept the fixes and record clear, Record budget-exhausted}; selected "Run one confirming round".
3. Asked (2026-11-27): how to close after the confirming round's two nits — options {Accept the two nit fixes and
   record clear, Run one more confirming round, Record budget-exhausted}; selected "Accept the two nit fixes, record
   clear".
4. Asked (2026-11-27): how to proceed with the two merge-gate gaps — options {Stop and retry the reviewer later, Merge
   despite the two gaps, Close the PR unmerged}; selected "Stop and retry the reviewer later".

No free-text operator messages were sent.

## Rule check

Guard: `none` on this harness (unenforced); the guard log `.plan/temp/guard.log` carries nothing for this run, so
nothing was enforced.

The run's own actions that the rules govern, listed honestly:

- Shell file operations issued by the run: two `ls` calls at the start (the plugin and skill directories, and the
  epic inbox `to/` directory), superseded by the Read tool for every later listing; one `rm -f` deleting superseded
  screenshots under `.plan/temp/screens/` (the discarded `ucp03-*`, `lang-before/after-*` and `entry-probe-*` captures
  of a withdrawn state resource); `grep` twice over landing files (PLAN-64/PLAN-76 commits) before switching to the
  Grep tool. Every other file read and search used Read/Grep/Glob.
- Builds: three daemon-routed attempts (two `quality-gate`, one `module-tests`) — one hit the 900 s bound, one failed
  at 1249 s with a job log outside this repository (not read), one ended at exit 1; three in-process runs
  (`quality-gate` twice, `test-compile` once, `module-tests` once). The in-process gate is the green one.
- Executor form: every script ran as `python3 .plan/execute-script.py …` from the main checkout; no `gh`, `glab`,
  `mvnw` or test binary was called directly for an action the lane routes through the executor.
- Spec read: `ledger_worktree`, no `spec_differ` marker.
- `executor-skew`: `match` (0.1.2183 = 0.1.2183).

Three rule-adjacent facts are recorded rather than hidden: the mock's `entry.onboarding` state withdraws the
onboarding-state act for every later state (the walk left it last and the mock was restarted between passes); the
browser surface is shared, and a second session took it after the walk (no later browser step was needed); and one
plan-less `wait --plan-id NO_PLAN` attached to another worktree's job before the run pinned its own job id.

## Build server

Setup preflight `ready`. Mechanisms: `daemon_longpoll` for the three routed attempts, `in_process_fallback` for the
four in-process runs. Fallbacks: the first `module-tests` degraded after the daemon's job failed at 1249 s (reason
unreadable, its log lies outside the repository); `quality-gate` fell back after a 900 s bound expiry; `module-tests`
reached 5684 passed / 0 failed / 3 skipped and then hit the 1800 s bound during packaging. No `killed` job.

## Residue

- **The landing is incomplete by the drain's own rule, and the reason is the truth:** `landing-check` reports
  `complete: false` with `missing_keys = total_tokens, plugin_version, sonar_open_new_issues`. All three are
  `unknown` because nothing measured them — the harness measured no token total, the runtime stamp did not resolve
  `plugin_version`, and Sonar has no analysis for this head. None is a zero and none is a guess.
- **The merge is owed.** PR #599 waits for CodeRabbit's review (quota) and for a Sonar decision (no analysis exists
  for a docs-only head; the CI's `check-changes` skips `sonar-build`). The automation `aut_21d7111cd001Tbf4USkiOc2usH`
  re-triggers the reviewer at 16:07Z.
- **#596 is closed unmerged** (the quota policy's close-and-reopen step); nothing else about it is owed.
- **The mock and the app of this worktree were stopped** after the walk; the worktree and the branch stay for the
  merge.
- **UC-ONB-4, UC-ENT-1 stay `partial`** and `UC-F-03` is retired in
  `doc/claude_review/use-cases/analysis.adoc`; the two `not verified` clauses (the model recommendation and the
  settings conflict notice) are drawn and await a mock that feeds them.
