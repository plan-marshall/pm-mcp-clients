envelope_version=1
sender_type=plan
sender_id=rb-05-cli-summary-line-and-start-script
epic=ui-improvements
kind=landing
created=2026-10-27T12:02:33Z

```landing-facts
schema=landing-facts/1
plan_id=rb-05-cli-summary-line-and-start-script
epic=ui-improvements
pr=#238
merge_state=merged
cleanup_owed=false
deliverables_total=2
deliverables_done=1
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=0.1.2017
executor_version=0.1.2017
steps=setup:done,implement:done,build-gate:done,self-review:budget-exhausted,pr:done,reviews:done,merge:done
step.sonar-roundtrip.open_issue_count=0
step.sonar-roundtrip.open_issue_rules=none
```

guard: none on this harness (unenforced). Spec read: `spec_source=main_checkout`, no `spec_differ` marker (no ledger copy was read; the epic runs with `orchestrator.use_worktree` on but the default read is the main checkout).

# PLAN-05 — Summary line after `q`, and the PID file only for `-d`

Merged as `a1a72483` (merge queue, merge_group run 40499192678 success) from PR #238,
branch `feature/05-cli-summary-line-and-start-script`.

## 1. Deliverables

### D1 — Summary line · MET

- **D1.1** "A transcript test leaves `plans` and finds the line" — **met**, order `test-first`.
  `SummaryLineIT.plansThenQPrintsTheLine`, `.planViewThenQNamesItsOwnPlace`,
  `.consoleHandOffPrintsOneLine`. Failing run `6186476d`, job-log
  `maven-2026-10-27-063225.log`: "Tests run: 2, Failures: 2",
  `plansThenQPrintsTheLine:73`, `planViewThenQNamesItsOwnPlace:87 "no leaving line in: …"`.
  Green after `7892567d` (`maven-2026-10-27-064245.log`, 421 tests) and at the head
  (`maven-2026-10-27-073748.log`). Commit `7892567d`, extended in `3163ebb2` and
  `f016c11a`.
- **D1.2** terminal check on `running-work` (`plans`, then `q`), before and after — **met**,
  order `none`. Capture set `screen-captures/05-cli-summary-line-and-start-script.txt`
  (120×30 and 80×24, en and de, 4 captures). After:
  `While the overview was open: nothing arrived.` (en) and
  `Während die Übersicht offen war: nichts kam.` (de) in both sizes; before: no
  leaving line in any of the four captures. Recorded under
  `.plan/temp/terminal/05-cli-summary-line-and-start-script/{before,after}/`.
  Caveat recorded honestly: the *before* set was produced by the pre-fix harness, which by
  construction could not hold post-`q` output, so its absence is vacuous — the failing
  `SummaryLineIT` run above is the proof of the missing line. Commits `7892567d`,
  `d6303322`, `353813a`, `abc15f57`, `f016c11a`.

### D2 — PID file only for `-d` · NOT fully met

- **D2.1** "Two foreground runs at the same time both start (a script test in the build)" —
  **met**, order `test-first`. `StartScriptTest.twoForegroundRunsBothStart` (with
  `backgroundRunWritesPidFile`). Failing run `7892567d`, job-log
  `maven-2026-10-27-064756.log`: "pm-operator is already running (PID: 41438)",
  "the second run reached java", "a foreground run writes no PID file". Green after
  `2012cfdf`. Commit `2012cfdf`, hardened in `353813a`.
- **D2.2** terminal check "shows **this deliverable's** change on `running-work`: `plans`,
  then `q`; the summary line; before and after captures recorded" — **NOT met as written**,
  order `none`. The clause names the overview view and the summary line, i.e. D1's
  observable; no screen capture can show a PID-file decision, and the harness the spec names
  can only drive the `pm-operator` binary, never `start.sh`. The capture set was run exactly
  as the clause enumerates it (see D1.2), and D2's own observable is
  `StartScriptTest.twoForegroundRunsBothStart`, which lives in the build rather than in a
  capture. Reported here so the epic can correct the clause; nothing in this run was allowed
  to bend it.
- **D2.3** browser regression on `fully-onboarded` and `running-work` — **met**, order
  `none`. Two passes with the Playwright MCP, the mock from the main checkout for the
  baseline and from the worktree for the change, route `/overview`: 2 scenarios × 2 locales
  × 2 themes × 2 widths = 32 cells in the first pass, every cell with no `{placeholder}`, no
  raw catalogue key and no ISO time, and byte-identical rendered text before and after
  (744/703/829/778 characters) because the changed keys are terminal-only; a second pass on
  the head read the accessibility tree per cell (61 lines, overview heading present). The
  clause's positive half — "shows the new texts" — is unobservable by construction: no web
  view renders `operator.overview.*`. Screenshots
  `.plan/temp/playwright-mcp/05-cli-summary-line-and-start-script-{running-work,fully-onboarded}-{before,after}.png`.
- **D2.4** open-Sonar-issue read at the PR head recorded — **met**, order `none`. Confirmed
  `0` open new-code issues at `c257bcca` (`sonar read_open_issues`, `count_status:
  confirmed`), read after the last push; `sonar-gate` therefore admits the merge.
  Two issues were found and fixed on the way (`java:S3776` and `java:S1192` at `abc15f57` →
  `0` at `f016c11a`).

Per-head test runs against pushed heads: `6186476d` (unit, green after the fix),
`7892567d` (`verify -Pintegration-tests -pl pm-operator -am`, 421 tests),
`d6303322` (`ScreenCaptureIT` set, 420), `3163ebb2` (`verify -Ppre-commit` 2127 +
`verify -Pintegration-tests -pl pm-operator` 55/28 skipped + `verify -pl pm-api` 649),
`abc15f57` (`verify -Ppre-commit` 2127), `f016c11a` (`verify -Ppre-commit` 2127,
`ScreenCaptureIT` set 420, `baseline` set 420), `c257bcca` (`verify -Ppre-commit` 2127 after
the `origin/main` merge).

`deliverables_done=1`: D1 is fully met; D2 is not, because D2.2 cannot be met as written.

## 2. Personas and skills

- Production code (both deliverables): `plan-marshall:persona-implementer`.
- Unit/module tests (`ConsoleNoticeBufferTest`, `StartScriptTest`, `OverviewCatalogueTest`):
  `plan-marshall:persona-module-tester`.
- Multi-module/PTY tests (`SummaryLineIT`, `ScreenCaptureIT`): `plan-marshall:persona-integration-tester`.
- Documentation (`15-overview.adoc`, `04-console.adoc`, `27-tui-testing.adoc`):
  `plan-marshall:persona-documenter`.
- Every self-review round and every PR-comment triage: `plan-marshall:persona-code-reviewer`.
- Per deliverable, the spec named `pm-dev-java:java-core`, `pm-dev-java:java-lombok` and
  `pm-dev-java:junit-core`; all three were loaded as standards for both deliverables.
- `skills added: none` — every touched file is in `pm-operator` (plus its catalogue in
  `pm-api` and the design docs), the modules the spec's Expected Surface already names.

## 3. Hypotheses

None stated. The spec's *Claim Labels* carries one `OBSERVED` line and the verify-first
clause, but no `HYPOTHESIS` line, so there was nothing to confirm or refute; the claims that
mattered were checked against the code instead, and the two real defects (no line printed
when nothing arrived; a foreground run blocking another through the PID file) were proven
by failing tests rather than assumed.

## 4. Findings

- **Self-review round 1** (`persona-code-reviewer`, head `d6303322`): 16 findings. Fixed in
  `3163ebb2`: the 15-overview catalogue table and §Live still stated the old line; 04-console
  credited the console with printing it; `leavingPlace()` read the stack outside the monitor
  `buildScreen` uses; nothing pinned "one line" on the console hand-off; `StartScriptTest`
  destroyed a PID without checking whose it was; a stale display name. Reported without
  action: the D2.2 spec defect, the D2.3 unobservable web half, the Expected Surface
  omissions, the dated review archive `doc/claude_review/cli/ux/findings.adoc` (deliberately
  frozen), and the pre-fix harness on the before-captures.
- **Self-review round 2** (head `3163ebb2`): 11 findings, no functional defect in the six
  fixes; verdict "ready for a pull request" once D2.2 reaches the inbox and D2.4 is read at
  the head. Fixed in `a3008e4e`: dropped the test-only 2-arg `summarize` that defaulted the
  place, corrected a display name, named the two catalogue fragments in the design table.
- **Self-review round 3** (head `a3008e4e`): 1 finding (a plural variable name for one
  buffer), fixed in `e119f1cf`; no other defect, no caller of the dropped overload, the
  AsciiDoc table intact. At the round limit the remaining items stayed open by reason, so
  the step is reported `budget-exhausted`; the operator was asked and chose to continue.
- **CodeRabbit, review 1** (head `e119f1cf`): 5 actionable findings, all real and all fixed
  in `353813a` — the leaving place came from the pushed level title instead of the live
  header; `stop.sh` exited before reaching its process-name lookup once a `-d` run and a
  foreground run could coexist; `StartScriptTest` read to EOF before its timeout; the capture
  reader could still be appending after the bounded join; the settle rule accepted a quiet
  pre-view stretch. Each thread was answered and resolved, and CodeRabbit confirmed each.
  Its follow-up on the reader race (a quiet window is not proof) was fixed in `f016c11a`
  with `PtySession.awaitReader`.
- **SonarCloud**: `java:S1192` and `java:S3776` on `c257bcca`'s predecessor, fixed in
  `abc15f57`; confirmed `0` open issues after.
- **CI**: one run of head `e119f1cf` failed its `sonar-build` step with "Organization is not
  allowed to access data from non main branches"; the re-run of the same head passed and
  every later head was green. Recorded as an infrastructure transient, not a code failure.

## 5. Reviewers

- `coderabbit` (required, `quota_limited=true`) — **reviewed `e119f1cf`**, which is not the
  current head `c257bcca`. Qualifying review: formal review of 2026-10-27T09:23 with the
  `final_review_risk_coverage` marker `kind=reviewed`, `coveredCommitId=e119f1cf`. Every
  finding of that review and of its follow-up is fixed in a pushed commit (`353813a`,
  `f016c11a`), every thread of its is resolved, and it was re-triggered after each push. Its
  last two triggers were refused on quota ("Review rate limited"; "Next included review
  available in 46 minutes") and its check reports `skipping`. The standing authorization
  therefore applies, and the operator selected "Merge now on the standing authorization"
  knowing that the last two commits are unseen by it.
- `sourcery` (optional, `quota_limited=true`) — **approved `353813a`**; its first request was
  refused on quota, its later checks are `skipping`. It does not gate the merge.
- Trigger and wait history: `@coderabbitai review` and `@sourcery-ai review` at PR creation;
  `@coderabbitai review` after `353813aa` (rate limited), after `f016c11a` (skipped), after
  `c257bcca` (skipped); `ci checks wait` four times, 540 s each, until green.

## 6. Operator questions

1. "The executor-skew check returned `could_not_check` … How do you want to proceed?" —
   options "Proceed with the run" / "Block: regenerate the executor first"; selected
   **"Proceed with the run"**.
2. "The self-review has spent its three rounds … How should I proceed?" — options "Proceed
   to the PR" / "One more review round first"; selected **"Proceed to the PR"**.
3. "PR #238 is ready to merge on every gate condition, but the required reviewer last
   reviewed an earlier head … how do you want to proceed?" — options "Merge now on the
   standing authorization" / "Wait for CodeRabbit to review the head"; selected **"Merge now
   on the standing authorization"**.

No free-text operator messages.

## 7. Rule check

`guard: none on this harness (unenforced)` — the opencode harness has no pre-tool-use hook,
so nothing was enforced and `{guard_log}` holds no line for this run. The check below is the
run's own account, not a guard log.

Kept: every script through the executor; every build through the resolved `build` commands;
every CI action through `ci`; one command per shell call (no `&&`, `;`, pipes, loops);
repository files edited only in the worktree; `.plan/` writes limited to `.plan/temp/`, the
`ci` body slots and this one inbox message; no `git stash`/`restore`/`rebase`/force-push;
every commit ends with the resolved trailer; the branch merged `origin/main` rather than
rebasing; the merge waited for the operator's selected option.

Not kept, in the order they happened:

- Early in setup and in the browser check I used shell file reads — `ls` on
  `.plan/temp/...` and `pm-operator/src/test/...`, `grep -n` on two scripts, `cat` on
  `pm-web-app/start.sh`, `wc -c`/`wc -l` on capture and log files — before re-reading the
  rule list; the file tools were used for every read after that.
- `rm -rf` on `.plan/temp/terminal/05-cli-summary-line-and-start-script/{before,after,harness-check}`
  to clear stale captures between runs (temp only).
- `ps -p 5025 -o pid=,comm=` once, to learn why port 5173 was reported in use; it named a
  Chrome helper socket, and the app was then started on port 5273.
- One stray call `python3 .plan/execute-tool-nonexistent` that failed with exit 2; it had no
  effect and was not repeated.
- The round-2 self-review sub-agent admits it used `git -C <worktree> grep` several times;
  under this contract that is the run's break, not only its own.

Restarts and re-dispatches: one server restart in the middle of the run (the landing-time
`resolve` and the whole gate were re-run after it); three self-review sub-agents, each on the
head current at the time (rounds 1–3); no killed sub-agent.

`spec_source=main_checkout`, and no `spec_differ` marker was carried, so the run never read
an older ledger copy silently.

## 8. Build server

- Setup preflight: `ready`, project registered. Every build of this run, however, ran
  `in_process`: the one routed attempt (`auto`, mechanism `daemon_longpoll`) failed in two
  seconds because the daemon invoked a bare `mvn` (job log
  `8d16de87f39c47a381c118b04d900123.log`, "[EXEC] mvn -l …"), the known PLAN-02/PLAN-04
  watch. The fallback is `in_process_fallback` for every subsequent build, recorded here.
- One `timeout` at the explicit 900 s bound (`maven-2026-10-27-093737.log`) while
  `npm run lint` ran in `pm-web-app`; all 2127 tests had already passed. Not a killed job and
  not blind-retried: the gate was re-run at 1800 s and was green (`maven-2026-10-27-095305.log`).
- `runtime_stamp_reason`: none. `plugin_version` and `executor_version` are both `0.1.2017`
  (read at landing time); `total_tokens` is `unknown` because the opencode harness measures no
  tokens — the epic's standing defect, repeated here.

## 9. Residue

- **The D2.2 clause is a spec defect** (`plans`, `q` and the summary line as the terminal
  check of a `start.sh` change). It needs the orchestrator's correction; the epic's
  `queue/PLAN-05.json` and the staging pattern for the 21 terminal plans are the place for it.
- **CodeRabbit has not seen `abc15f57`, `f016c11a` or `c257bcca`** — two Sonar-driven
  refactors and a merge of `.plan/` ledger files. Its review credit resets about 46 minutes
  after the 10:10 refusal.
- The pre-existing web defects this check brushed against are untouched and owned elsewhere:
  "10 d ago" stays untranslated in the German overview (PLAN-16), and the `✎ 🖥 ⚙` header
  buttons still show their icon glyph (PLAN-19); they are buttons with aria labels.
- `stop.sh`'s fall-through has no test: the process-name lookup matches the real process
  table, so a test could stop another session's operator. Covered by inspection and by
  CodeRabbit's explicit confirmation.
- `leavingPlace()` and `buildScreen()` share `levelBreadcrumb`, but `levelFooter` keeps its
  own small dispatch; the two read the same models and can only drift if a view gains a
  footer without a header.
- The epic's opencode watches still hold: `executor-skew` answers `could_not_check` (the
  lane text's version cannot be derived from the flat install path), the guard is `none`, and
  opencode landings cannot measure `total_tokens` or read `plugin_version` at setup time.
