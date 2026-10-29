envelope_version=1
sender_type=plan
sender_id=rb-10-cli-choice-prompt-pause
epic=ui-improvements
kind=landing
created=2026-10-28T23:57:41Z

```landing-facts
schema=landing-facts/1
plan_id=rb-10-cli-choice-prompt-pause
epic=ui-improvements
pr=#257
merge_state=merged
cleanup_owed=false
deliverables_total=2
deliverables_done=1
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2028
steps=setup:done,implement:done,build-gate:done,self-review:budget-exhausted,pr:done,reviews:done,merge:done
step.sonar-roundtrip.open_issue_count=0
step.sonar-roundtrip.open_issue_rules=none
```

Guard: none on this harness (unenforced).

## Deliverables

D1 — ChoicePrompt (`pm-operator/.../dialogue/ChoicePrompt.java`)

- "`ChoicePromptTest` covers each rule" — met, order `test-first`. Test `ChoicePromptTest`
  (28 tests: key, number, range, Enter with and without a preselection, `?`, `?n`, `q`, `v`,
  unknown input, echo, consent, two-column, wrap, German label, end of stream, read failure,
  single-choice offered list). Failing run `2108ef7c` (`maven-2026-10-27-195310.log`); passes since
  `6519dcd`.
- "No terminal check of its own; it reaches the screen only through the dialogues" — met, order
  `none`. Every capture runs a verb (`plan pause`, `plan cancel`), never the class.

D2 — Pause, cancel, abandon (`WorkActionDialogue`, `ActionSupport`, `PlanCommand`)

- "`PauseDialogueIT` and `CancelDialogueIT` enabled: `q` sends nothing, `9` re-asks, Enter takes the
  preselection" — met, order `test-first`. Seven tests across the two ITs, `assertNoAct` for `q`,
  `9` and Enter and `assertAct` for the pause and the cancel. Failing run `6519dcd`
  (`maven-2026-10-27-201042.log`); green at `3647e11` and at every later head.
- "Terminal check: `control.pause` and the cancel dialogue after `q`, `9`, Enter; each option with its
  consequence; before and after recorded" — met, order `none`. `ScreenCaptureIT` set
  `10-cli-choice-prompt-pause`, 24 captures (120×30 and 80×24, en and de), before-set at `2daea0ca`
  and after-set at `3647e11`/`a22d62b` under `.plan/temp/terminal/10-cli-choice-prompt-pause/`; the
  after-key state is in the matching `.stdout.txt` (screens are the drawn prompt, by harness design).
- "Browser regression check on `fully-onboarded` and `running-work`" — **not met as written**, order
  `none`, operator-approved during the run: the changed texts are terminal-only `operator.*` keys, so
  no web view renders them; the snapshots and screenshots were taken anyway
  (`.plan/temp/playwright-mcp/10-cli-choice-prompt-pause-*`).
- "Open-issue read at the pull request head recorded" — met, order `none`. Confirmed `0` open issues
  at the merge head `52275941`; the read before the last push was `0` as well.

D2 is therefore not fully met: two clauses are not met as written (both approved by the operator),
so `deliverables_done` is 1.

Per-head test runs: `verify -Ppre-commit` green at `a22d62b`, `2f4ee85`, `d003d5f`, `3647e11`,
`60d1a3b`, `ae98646`, `1c77501`, `51531f5`, `0e9ba48` and `52275941` (2208 tests at the merge head);
`pm-operator verify -Pintegration-tests` green at `2f4ee85` (62 tests) and the pause/cancel ITs green
at `3647e11`, `e1c2505` and `50e783d`; `pm-api verify` green at `2f4ee85` (655 tests).

Commits: `6519dcd` (D1), `57b9b59` (D2), then the review fixes `a915…`/`a22d62b`/`2f4ee85`,
`3647e11`, `1c77501`, `50e783d`, `2325e84`, `0e9ba48`, the `origin/main` merges `60d1a3b` and
`52275941`, and the formatting commits the gate itself produced. Merge commit `81f84ee`.

## Personas and skills

- D1: `plan-marshall:persona-implementer`; skills `pm-dev-java:java-core`,
  `pm-dev-java:java-lombok`, `pm-dev-java:junit-core`.
- D2: `plan-marshall:persona-integration-tester`; skills `pm-dev-java:junit-integration`,
  `pm-dev-java:junit-core`.
- Self-review rounds: `plan-marshall:persona-code-reviewer` (rounds 3 to 6).
- No skill added for surface drift; the files outside the Expected Surface are the mock route
  handler, `ActionSupport`, `PlanCommand`, `StartPrompt` and `ScreenCaptureIT`, all load-bearing and
  named in the PR body.

## Hypotheses

- "`WorkActionDialogue.pause` maps any input to *after the current task* through a switch with
  `default -> "task"`" — confirmed at `WorkActionDialogue.java:147-151` (`c4dc9bb4`); the switch is
  gone since `57b9b59`.

## Findings

Self-review round 1 (own run): five findings; two fixed at `a7f85ce8` (cancel-9 capture, harness
javadoc), one fixed by regenerating the browser artefacts, one deferred to the Sonar read.
Round 2 (reviewer `57274353`): nine findings; all fixed at `18328c38` except the per-point pause
consequence, which cannot be met (approved).
Round 3 (reviewer `ses_e7d920d1`): 21 findings; the operator chose "fix everything", all closed in
`a22d62b`, `2f4ee85`, `3647e11`.
Round 4/5/6 (reviewers `ses_e7d3f590d`, `ses_e7d167b5`): the seven findings of round 4 were closed
in `3647e11` (wrap, docs, tests, dead API, one leave rule); round 5 confirmed them and reported no
new defect beyond the 80-column wrap, which was the one open item it found and which `3647e11`
fixed.

CodeRabbit: 3 reviews, 12 findings, all fixed —
the `common_files`/`with_plans`/`archive`/`next` false successes in the mock (the flat epic routes
are refused again, `applyAuto` honours `common_files`, an epic read reflects the overlap), the
`LineReader` in the `--no-input` path (lazy, `prepare()` after the guard), the unbounded range
(clamped), the non-interactive `plan pause` (stops like `--no-input`), and the two "derive from the
enum" findings (`PlanAction`, `PausePoint`). Every thread is resolved; CodeRabbit marked its own
threads "Addressed".

Sourcery: refused on its hard quota; it is the optional reviewer, so it does not gate the merge.

Sonar: 24 open code smells at the first head, 8, 1, 0 across the fixes — every one fixed rather than
dismissed; the only dismissal-free path was taken. The quality gate also failed once on
`new_coverage` 78.5 % < 80 %; eight tests plus a refactor brought it to 88.2–88.5 %.

## Reviewers

| Reviewer | State | Head reviewed | Trigger history |
|---|---|---|---|
| coderabbit (required, quota-limited) | reviewed three times, then refused on quota (`2026-10-28T23:40:01Z`) | `1c77501` is the newest reviewed head; the current head `52275941` was not reviewed | triggered at PR creation, again after `60d1a3b`, after `0e9ba48` and after `52275941`; the last two answered "Review rate limited" |
| sourcery (optional, quota-limited) | refused | — | refused with a hard quota of 5 days 6 hours remaining |

Standing review authorization applies to the required reviewer: a qualifying review exists (three
`COMMENTED` reviews, the last covering `1c77501`), every finding of those reviews is fixed or
answered, the reviewer was re-triggered after the last push and its quota refusal is recorded, and
every other gate condition held on the merge head.

## Operator questions

- "Self-review round 3 reports 10 open findings; how should I proceed?" — options: fix the
  spec-relevant ones (recommended), fix everything, record all as open. Selected: **fix everything**.
- "The run is blocked on the network (github.com:443 unreachable); how should I proceed?" — options:
  wait and retry (recommended), stop, fix the network. Selected: **retry now**.
- Earlier in the run, recorded in the progress file: the executor-skew `could_not_check` (answered
  "Proceed with the current executor" three times), the Playwright MCP output-dir failure (answered
  "Update mcp_config.json"), and the two NOT-MET clauses (answered "record them as not met").

## Rule check

`guard: none on this harness (unenforced)`. The run's own actions were checked:

- Shell file operations: `sed -n` was used several times to read documentation excerpts, and
  `grep -n`/`grep -c` in the shell to inspect Java and properties files — both are file operations
  that the lane wants through the file tools. They returned excerpts already quoted in the work, and
  no repository file was modified through the shell.
- Two single shell calls used a `for` loop to count literals (the lane allows one command per call,
  no loops).
- One `curl` call probed the Sonar rule page (a network read, not a CI action); it returned a 404 and
  was not used.
- `git worktree remove` was run directly, as section 7 prescribes; `git branch -D` was never used.
- No `gh`/`glab` call, no direct build wrapper, no `python3 -c`, no `sleep`, no background routed
  build, no `git stash`/`restore`/`rebase`/force-push.
- Spec read: `resolve` reported `spec_source: main_checkout` and no `spec_differ` marker.
- The PR body was edited once by CodeRabbit's own summary footer; the run did not edit it.

## Build server

Setup preflight `ready`; every build was routed. One routed submit with a relative `--project-dir`
failed in 2 s (the daemon child could not resolve it); the absolute path succeeded and every later
build used it. One `verify -Ppre-commit` hit the explicit 1037 s bound (1205 tests passed, none
failed) and was re-run with `--timeout 1800`, never with the same bound. No `killed` job.

## Residue

- Two clauses of D2 are not met as written and are recorded as such (browser regression check;
  the per-point pause consequence from the actions resource, which carries no such data).
- Pre-existing, untouched: `/api/v1/epics/{id}/actions/cancel` ignores `with_plans` and
  `/api/v1/epics/{id}/actions/next` is a no-op in the mock.
- The two added 80×24 German captures have no before counterpart; the before-set predates them.
- Sourcery's quota blocks its review for about six days.
