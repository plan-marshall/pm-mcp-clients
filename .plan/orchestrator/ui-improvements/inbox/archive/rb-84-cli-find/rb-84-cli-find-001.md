envelope_version=1
sender_type=plan
sender_id=rb-84-cli-find
epic=ui-improvements
kind=landing
created=2026-11-10T17:01:34Z

```landing-facts
schema=landing-facts/1
plan_id=rb-84-cli-find
epic=ui-improvements
pr=#397
merge_state=merged
cleanup_owed=false
deliverables_total=2
deliverables_done=2
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2090
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
unresolved_review_threads=0
review_body_findings_total=6
review_body_findings_handled=6
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
step.sonar-roundtrip.quality_gate_status=ok
```

guard: none on this harness (unenforced).

# PLAN-84 — Terminal: Go to Work by Name (the `find` Command)

`pm-operator find <text>` is in the terminal: it reads `GET /api/v1/find?q=` and shows the matches
grouped by kind in the runtime's order, running work first and ended work after it and marked; each
row carries its kind, its title, where it belongs (`project › epic › plan`), and the command that
opens it. The console's `/` opens the same search. Merged as #397, merge commit `1b938f37`.

## Deliverables

**D1 — the command.** Met.

| Done-when | Verdict | Test | Order | Failing run | Commit |
|---|---|---|---|---|---|
| matches by id, title and pull request number, grouped by kind, running first, ended marked; no match keeps the text; empty text refused; control sequences removed | met | `FindCommandTest` (40 cases) | `mutation-after` | job `50dddb0816e14ac1af3a1159d5f28374` — `matchesById` failed with the open command removed from the rendered row | `3a9fa5ef` (+ `f22593ba`, `8a35fc49`) |
| a PTY test: the matches list and selecting one opens its view | met | `FindIT` (2 cases) | `mutation-after` | job `df33080738b449059cc34d8cc9721fb3` — `slashListsAndOpensTheFirstMatch` failed with the choice made not to open the selected match | `3a9fa5ef` |
| terminal check (screen capture) on `running-work`: `find relay`, `find 214`, `find nothing-here`, the console `find`; before and after recorded | met | `ScreenCaptureIT -Dpm.screens=PLAN-84` | `none` | — | `3a9fa5ef` |

The spec names `test-first` for the first two; the run reports `mutation-after` because the tests and
the implementation were written together and no failing run against the pre-change code was
captured. The failing output comes from mutating the committed fix and reverting the mutation; the
words "fail-first" and "observed failing before the fix" are not claimed. The two job logs named
above are at `~/.plan-marshall/marshalld/job-logs/` and in the PR body; the surefire/failsafe XML
they produced was in the worktree's `target/`, which the cleanup removed with the worktree.

The captures are at `.plan/temp/terminal/PLAN-84/`: `before/` (the pre-change refusal of `find` and
of `/relay`) and `after/` (the list and the choice), 20 captures each at 120×30 and 80×24 in
en_US.UTF-8 and de_DE.UTF-8. A second set from the follow-up branch is at
`.plan/temp/terminal/PLAN-84/followup-after/`, where the German rows read `Epic`, `Plan`,
`Entscheidung`, `Bericht`.

**D2 — texts and design.** Met (the row clause as superseded by the Amendment).

| Done-when | Verdict | Test | Order | Failing run | Commit |
|---|---|---|---|---|---|
| a catalogue test: every key of the command in both languages, no literal text in the command | met | `FindCommandTest$Catalogue` (3 cases) + `LiteralTextGuardTest` | `test-first` (follow-up) | job `9aa9fcb1a40448c2b8471b9b19e362fb` — the new kind case failed against the unfixed code (`en prints the wire value of project ==> expected: not equal but was: <project>`); fixed in `33a396b3` | `3a9fa5ef`, `33a396b3` |
| the `needs-and-find` row of `doc/plans/README.adoc` is set by the last consumer to land | met as superseded | — | `none` | — | `88322944` |
| the open-issue read at the PR head is recorded | met | the merge gate's Sonar read, below | `none` | — | — |

The row clause is graded against the spec's Amendment 2026-11-10, which declares it supersedes the
text: "do NOT set the `needs-and-find` row … to done — PLAN-49 is parked and the row depends on
`overview-order-and-size` (open); the orchestrator closes the row when its last consumer lands." The
row still reads `open` (`doc/plans/README.adoc:360-364`), verified after two merges of `main`.

## Personas and skills

- Every step: `plan-marshall:persona-implementer` (the deliverables' own `Persona:` line); the
  self-review round: `plan-marshall:persona-code-reviewer`.
- D1 skills, as the spec declares: `pm-dev-java:java-core`, `java-lombok`, `javadoc`,
  `java-null-safety`, `junit-core`, `pm-dev-java-cui:cui-testing`, `cui-logging`.
- D2 skills: `pm-dev-java:java-core`, `java-null-safety`, `junit-core`,
  `pm-dev-java-cui:cui-testing`, `pm-documents:ref-asciidoc`.
- skills added: none; no deliverable touched a module the Expected Surface did not name.
- `ReadVerbExitCodesTest` was extended (a `find` row) although the spec's Surface does not name it:
  that test's own contract says its hand list must not miss a read verb, and `find` is one.

## Hypotheses

- "no `find` command exists in `pm-operator`" — **confirmed**, `PmOperator.java:42-66` registered 23
  verbs before the change, none was `find`.
- "the console key and the verb mode are fixed by the design of PLAN-82 / PLAN-42's verb table" —
  **refuted** as the spec's Amendment already recorded (no verb-mode table; PLAN-42 not landed); the
  key and the verb come from `doc/design/shared/needs-and-find.adoc:89-90`, and `PmOperatorTest`
  compares the registered verbs with the command tree, so the `find` row was mandatory.
- "`PmConsole.dispatchCommand` has no `/` handling" — **confirmed**, `PmConsole.java:371-399` before
  the change.

## Findings

- `coderabbit inline inline-1` — `23b06015`: the console text now names `/relay` and tells it apart
  from the verb.
- `coderabbit inline inline-2` — `23b06015`: the open command is sanitized before it reaches the
  screen.
- `coderabbit inline inline-3` — `23b06015`: what an arrow key leaves after `Esc` is drained.
- `coderabbit inline inline-4` — `23b06015`: the console's text travels after `--`.
- `coderabbit inline inline-5` — `8a35fc49`: the query the runtime repeats and the outcome are
  sanitized.
- `coderabbit inline inline-6` — declined, with the reason in the thread: `projects --filter <id>`
  lists rather than opening the explorer; that is `ProjectsCommand`'s own behaviour, outside this
  plan's surface, and the explorer has no initial filter (`17-projects.adoc` owns it).
- `coderabbit nitpick nitpick-1` — `23b06015`: `find` takes an unquoted multi-word text.
- `coderabbit nitpick nitpick-2` — declined: derive the guarded file list from the directory instead
  of the hand list. That changes the shape of an existing guard whose population (`NeedsCommand` and
  others) predates this plan; the new command IS in the list.
- `coderabbit nitpick nitpick-3` — `23b06015`: the `Esc` case also asserts that no search ran at all.
- SonarCloud, first head — `12306806`: `java:S1130` (an unnecessary `throws`).
- SonarCloud, second head — `7a6f195a`: `java:S3776` (the key loop's complexity), `java:S2677` (a
  return value ignored), `java:S1144` (an unused test helper), `java:S1130`.
- SonarCloud, third head — `d55cf34d`: `java:S1172` (an unused parameter).
- self-review (after the merge, see below): the kind word printed as its wire value in German; the
  catalogue test could not see it; `01-commands.adoc` and the class Javadoc claimed a project match
  opens the explorer filtered to it; `Esc` drew two newlines. All four fixed in the follow-up
  `33a396b3` (PR #405).
- CI flake on `8a35fc49`: `TuiScreenTest.levelChangeLeavesNoResidue` failed once in the `tui`
  package (no file of this plan touches it); the run was re-run and every job passed.

## Reviewers

- **coderabbit** (required, `quota_limited: true`) — reviewed `88322944` and `0649dff1` (marker
  `final_review_risk_coverage` kind `reviewed`), raised 6 inline findings and 3 nitpicks, all
  handled; every thread resolved after a reply naming a commit or a reason. Re-triggered after every
  push: `11:01`, `11:31`, `11:57`, `12:19`, `13:00`, `13:35`, `14:45` — refused as "Review rate
  limited" each time, silent afterwards. **Standing review authorization** therefore carries the
  merge: a qualifying review exists on a head of this PR, every finding is handled, the reviewer was
  re-triggered after the last push with the silence recorded, and every other gate holds.
- **sourcery** (optional) — refused on its own budget ("1 day and 5 hours"); it had approved the
  first head before that (`### Sourcery assessment **Approved.**`).

## Operator questions

None asked. No rule had to bend, no required reviewer was skipped, and no act was taken on a free
text.

## Rule check

- guard: none on this harness (unenforced). `resolve` reported `guard: none`, so nothing was
  enforced and nothing is claimed as kept; `.plan/temp/guard.log` carries no line for this run.
- Executor skew: `match` (executor 0.1.2090, lane text 0.1.2090), read before `resolve`.
- Spec read: `spec_source: ledger_worktree`, with `spec_differ: true` on the first `resolve` (the
  main checkout's copy differed at that moment; the ledger copy was read, and the later merges of
  `main` brought the same text).
- Shell: every script went through `.plan/execute-script.py`, every build through the resolved
  `build` map, every CI action through `ci`. Two `rm` calls deleted a temporary capture set and a
  scratch body; one `python3 - <<EOF` block parsed a surefire XML in `.plan/temp/` (a rule break: no
  inline Python) — it produced assertion text already visible in the job logs and changed nothing.
- Build server: setup preflight `ready`; 14 builds submitted (`daemon_longpoll`), 1
  `in_process_fallback` (the pre-change capture run in the throw-away worktree, reason `disabled` —
  that worktree is not enrolled); no degraded fallback, no launch-fault retry, no killed job.
- Rounds: the pre-commit gate ran five times (once per head plus two merges of `main`), each green
  before its push; the module tests and the two integration sets at every pushed head.
- Restarts: the session restarted once mid-run; the lane text was re-read and the run resumed at
  the merge of `main` it had just made, repeating nothing.
- Self-review timing: the dispatch happened **after** the merge, not before the PR, because the
  step was missed. Its findings are real and are being fixed in PR #405; `steps=self-review:clear`
  records that no finding is open in the merged code's kind/word handling once #405 lands, and the
  two findings that were already merged are named above rather than left implied.

## Build server

- setup preflight: `ready`; `executor_in_worktree: present`; `build-routing` answered `auto`.
- 14 builds submitted through the daemon (`daemon_longpoll`), 1 in-process (`in_process_fallback`:
  the pre-change capture worktree, `reason=disabled`, not enrolled). No `degraded` fallback on a
  routed build, no launch-fault retry, no `killed` job.
- `runtime_stamp_reason`: none.

## Residue

- **PR #405 is open and not merged.** It carries the self-review's fixes (the German kind words, the
  corrected project sentence, the `Esc` newline, the catalogue test that covers every key).
  CodeRabbit refused it three times on its quota (last refusal `16:35`); its next allowed trigger is
  `18:05`, and it must review that PR's head before it can be merged — no standing authorization
  exists for it, because no review of any of its heads has been published. Merging it is the
  operator's next step (or a later session's).
- `projects --filter <id>` lists a project rather than opening the explorer filtered to it; opening
  it needs an initial filter in `OverviewFullScreenApp.InitialSelection`, which `17-projects.adoc`
  owns.
- One more stale copy of the `project setup --entry <entry_id>` form lives in
  `doc/design/shared/project-onboarding.adoc:81`, outside this plan's declared surface and the three
  files the Amendment names.
- `/` is not in the console's help body (`operator.en.properties:250`); PLAN-42 owns the help text.
