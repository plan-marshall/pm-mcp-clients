envelope_version=1
sender_type=plan
sender_id=rb-42-cli-verb-modes-and-help
epic=ui-improvements
kind=landing
created=2026-11-23T14:09:23Z

```landing-facts
schema=landing-facts/1
plan_id=rb-42-cli-verb-modes-and-help
epic=ui-improvements
pr=n/a
merge_state=open
cleanup_owed=false
deliverables_total=7
deliverables_done=2
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2183
steps=setup:done,implement:partial,build-gate:done,self-review:not-run,pr:n/a,reviews:n/a,merge:n/a
unresolved_review_threads=unknown
review_body_findings_total=unknown
review_body_findings_handled=unknown
sonar_open_new_issues=unknown
step.sonar-roundtrip.open_issue_rules=unknown
```

guard: none on this harness (unenforced)

# PLAN-42 — checkpoint at deliverable 5 of 7

Branch `feature/42-cli-verb-modes-and-help`, 5 commits, pushed. The operator chose
"one PR for the whole plan" at the start and "checkpoint now and resume later" at the
stop, so no PR was opened: the plan is not complete and a PR would be merged only after
D1–D3 land.

## Deliverables

**D0 — the residue of PLAN-163: met.**
- Printed path feeds `--file`: met. Test `PlanCommandTest.Changes.printedPathFeedsTheFile`;
  order `test-first`; failing run at the pre-fix tree (`4d57b6fa` content), log
  `.plan/local/plans/NO_PLAN/build-results/pm-operator/maven-2026-11-23-115923.log`
  (7 failures in `plan <id> --changes on the mock`); passed at `7b081721`.
- Capture identity on the key-driven open: met. Test
  `ChangesLevelTest.TheKeyDrivenOpen.aCaptureOvertakesTheRead`; order `mutation-after`;
  failing run at `7b081721` with `if (answer != answerReadAgainst) return false;` removed
  (job `e01ef42cd550477b93b6a9072e647f02`, 1 failure); passed at `7b081721`.
- Hunk text, zero-file text, exit codes: met. Tests `printsPipedHunksWithTabsAndOneMarker`,
  `printsTheEmptyList`, `refusesABlankFile`, `refusesConflictingModes`,
  `printsUnavailableAndUnknown`; order `test-first`; same pre-fix run above; passed at
  `7b081721` (211 focused tests).
- `16-plan-view.adoc:114,165` and `01-commands.adoc:118-122` validate: met with a stated
  caveat. `pm-documents:ref-asciidoc:asciidoc validate` reports 1/1 compliant and 0 errors
  for each file. `verify-links` from the worktree reports 3 and 21 "broken" links — every
  one of them a cross-repository `../../../../plan-marshall-documentation/…` or
  `../../../../pm-mcp-core/…` target that only the main checkout can see (the same two
  files at `origin/main` report 0 broken from the main checkout). The diff adds no link
  target. Order `none`.
- Commit `7b081721`.

**D1 — verb modes: not started.**
**D2 — help: not started.**
**D3 — verb-mode table: not started** (must follow D1, whose modes it documents).
**D4 — the `system.confirm.what` forms and the refused `config` state read: met.**
- Fixed-case singular/zero test: met. `RuntimeActDialogueTest.runningNowSingularAndZero`;
  order `mutation-after`; failing run at `37ea9e17` with the pack's `one` branch changed
  (job `afbba51726f44a75af2b181d1a36bbc4`, 1 failure); passed at `37ea9e17`.
- Refused-state-read test: met. `ConfigCommandTest.configAspectReportsARefusedStateRead`;
  order `mutation-after`; failing run at `37ea9e17` with
  `RefusalRenderer.render(err, locale, e)` removed (job
  `73fa2d6a638641c58ef7c22cdce349c5`, 4 failures including the new one); passed at
  `37ea9e17`.
- `mvnw -pl pm-operator verify`: met. `verify -pl pm-operator -am` at `37ea9e17`: success,
  2884 tests, 0 failures (job `f91c8c2aa5b74905ab6aee7b9cc2bcd1`, 726 s). The two added
  tests are `runningNowSingularAndZero` and `configAspectReportsARefusedStateRead`.
- Commit `37ea9e17`.

**D5 — the Traceability correction: two of three Done-whens met.**
- The four searches: met. `decision-log`, `credentials migrate`, `shim` and `Seatbelt`
  return no match under `pm-operator/src` and `pm-exec/src`; `doc/Traceability.adoc` lines
  122 and 178 now read "none in this repository", 245 drops `credentials migrate`, 384
  keeps Landlock and drops Seatbelt. Order `none`.
- The sibling check: met. `../pm-mcp-core/doc/specification/harness-profiles.adoc:13` links
  the clients URL (b closed); `../plan-marshall-documentation/doc/use-cases/01-onboarding.adoc:298`
  links the clients URL of `02-bundle-manifest.adoc` (c closed); no edit made. Order `none`.
- The diff and `trace.py`: **not met as written.** The commit touches only the four lines
  (`90757a51`, 1 file, +4/−4), but the documentation repository's `trace.py` was **not
  run**: the lane's executor notation reaches only this project's mapped scripts, and no
  sanctioned path runs a sibling repository's script.
- Commit `90757a51`.

**D6 — the residue of PLAN-174: one of three Done-whens met as written.**
- Conflict/decline streams and the German tree/list texts: **not met as written.** The
  tests exist and pass (`configSetConflictAppliesAgain`, `secondConflictPrintsConflictLineAgain`,
  the two `declined*`/`endOfInput*` tests, `PlanCommandTest.InGerman.treeIsGerman` and
  `.listTitlesAreGerman`), but they were written after the fix, so the declared order
  `test-first` was never observed; no failing-before-the-fix run exists for them.
- `reports read` with every id refused: met. `ReportsCommandTest.reportsReadAllRefusedPrintsNoSuccessLine`;
  order `mutation-after`; failing run at `19cce18b` with `if (marked > 0)` forced true (job
  `199a5645a5894439b0731f868bf38673`, 1 failure); passed at `19cce18b`.
- The `ExitCodesTest` success rows: **not met as written.** Five rows now name a real
  argument (`status --json`, `web devices`, `project favourite <id>`, `client status`,
  `lesson check <id> --project <id>`) and the success test refuses the parent help, but the
  mutation does not fail the row: dropping `devices` from `web` leaves it green because
  `web`'s no-argument run is its own default action. `credentials`, `recipe` and `roles`
  keep no argument: in this test environment their only real invocations need an
  installation layout or an answer shape the shared setup does not serve.
- Commit `19cce18b`.

Counted as fully met: D0 and D4, so `deliverables_done=2` of 7.

## Personas and skills

- D0: `persona-implementer`; skills `pm-dev-java:java-core, java-javadoc, java-null-safety, junit-core,
  pm-dev-java-cui:cui-testing, pm-documents:ref-asciidoc` (as the spec declares them).
- D6: `persona-implementer`; `pm-dev-java:java-core, junit-core, pm-dev-java-cui:cui-testing, cui-logging`.
- D4: `persona-module-tester`; `pm-dev-java:junit-core, pm-dev-java-cui:cui-testing`.
- D5: `persona-documenter`; `pm-documents:ref-asciidoc, ref-documentation`.
- No skill was added for surface drift.

## Hypotheses

- D0: confirmed — `ChangesLevel.java:370-377` abbreviates the path the one-shot prints,
  `PlanCommand.java:339-343` indents the rows, `:362-368` needs the exact path,
  `OverviewFullScreenApp.java:2817-2847` applied `showFile` with no identity check while
  `:1896-1913` guarded the refresh path.
- D6: confirmed — `ConfigCommand.java:735-751` printed the conflict on stdout,
  `AcceptEditCommand.java:329-332` the decline, `PlanCommand.java:631-639,658,666,669,699,721`
  held English literals, `ReportsCommand.java:290` always printed the success line.
- D4: confirmed — `RuntimeActDialogueTest.java:115-120` draws 2..9, the `one`/`=0` branches
  of `system.en.properties:68` were unasserted, and `ConfigCommandTest.java:685-701`
  answers the state read.
- D5: confirmed — the four `doc/Traceability.adoc` entries named absent code.

## Findings

- The D4 fixed-case test found a real defect: a leading zero group left a leading
  " · " in `SystemView.runningNow`. Fixed in `37ea9e17`.
- The first attempt at the D0 capture-identity test drove a second HTTP read from the fake
  server's handler thread and deadlocked the JVM; the run was killed at its 900 s bound
  (`maven-2026-11-23-115923.log`). The deadlocking test was replaced by a level-level test.
- The first `verify -pl pm-operator` (without `-am`) ran against the stale installed
  `pm-api` snapshot and failed 7 tests whose new catalogue keys were therefore missing
  (`maven-2026-11-23-123849.log`); `-pl pm-operator -am` is green.
- Pre-existing assertions changed deliberately: `PlanCommandTest.printsTheList` (full paths
  instead of abbreviated ones) and `.printsOneFile` (ASCII `-` instead of U+2212), and
  `.printsUnavailableAndUnknown` (exit 1 on stderr). The `ConfigCommandTest` and
  `AcceptEditCommandTest` stream assertions moved to stderr.

## Reviewers

None: no pull request was opened.

## Operator questions

1. "PLAN-42 carries 7 deliverables … each deliverable is its own pull request … the lane's
   contract is one PR per run. How should I proceed?" Options: one PR for the whole plan
   (recommended), one PR per deliverable landed in sequence, stop and re-stage. Selected
   "One PR for the whole plan (Recommended)".
2. "D0, D6, D4 and D5 are committed and pushed … D1, D2 and D3 are feature-scale … How
   should I proceed?" Options: checkpoint now and resume later (recommended), push on with
   D1 now, stop the plan here. Selected "Checkpoint now and resume later".

## Rule check

- guard: none on this harness (unenforced); no guard-log line exists for the run, and none
  is claimed.
- Rule breaks by this run, counted by its own actions: shell `grep` was used about a dozen
  times (file search belongs to the Grep tool), `tail` on build logs, and `ls` of the
  build-results directory; the guard recorded nothing because none is installed.
- `executor-skew`: `match` (executor 0.1.2183, lane 0.1.2183).
- Spec read: `spec_source: ledger_worktree`, with `spec_differ: true` — the main checkout
  still holds the older 5-deliverable copy, so the ledger copy governed and the divergence
  is recorded here rather than left silent.
- The spec's D0 clause (f) is truncated at 2000 characters by every reader available to
  this run; it was reconstructed from the `landings/PLAN-163.md` "stale or misplaced design
  lines" it cites, and the two documents it names were brought in line with the code.

## Build server

- Setup preflight on the worktree: `ready`, registered, `executor_in_worktree: present`.
- Every build ran routed: `daemon_longpoll` throughout (about 12 dispatches), no
  `in_process_fallback`, no `executor_absent_from_worktree`, no launch-fault retry.
- Two abnormal outcomes, both recorded above: the deadlocked D0 test run killed at its
  900 s bound, and the stale-`pm-api` `verify -pl pm-operator` run. Neither was retried
  blindly; the cause of each was fixed and the next run was green.

## Residue

- D1, D2 and D3 remain; the branch is green at `90757a51` and the progress file
  `.plan/temp/rb-42-cli-verb-modes-and-help/progress.md` names D1 as the next deliverable.
- D5's third Done-when needs a sanctioned way to run the documentation repository's
  `trace.py`.
- D6's first and third Done-whens are reported not met as written; the third cannot be met
  for `credentials`, `recipe` and `roles` without setups this test class does not have.
