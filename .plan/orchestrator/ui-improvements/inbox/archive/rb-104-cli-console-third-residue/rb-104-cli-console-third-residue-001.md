envelope_version=1
sender_type=plan
sender_id=rb-104-cli-console-third-residue
epic=ui-improvements
kind=landing
created=2026-11-06T22:21:25Z

```landing-facts
schema=landing-facts/1
plan_id=rb-104-cli-console-third-residue
epic=ui-improvements
pr=#357
merge_state=merged
cleanup_owed=false
deliverables_total=6
deliverables_done=3
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
guard: none on this harness (unenforced)

# PLAN-104 cli-console-third-residue — landing

PR #357 (merge commit `7902d577`), plus PR #361 (`14a6f7c0`) for the formatter
residue the pre-commit run left after the last commit. Both merged through the
queue with green CI and 0 new Sonar issues at their heads.

## Deliverables

| # | Deliverable | Verdict |
|---|---|---|
| D1 | A being-added project is addressed by what the contract carries | **met** |
| D2 | The five review items and the test-only surface | **not fully met** — the lock-budget revert is unobservable (see D2.1) |
| D3 | The step count has one source | **not fully met** — the failing-before run proved the intro clause only (see D3.1) |
| D4 | The German row and the clock, with evidence that can fail | **not fully met** — the capture order is `none` by your decision (see D4.2) |
| D5 | The generic catches in `pm-operator/src/main` | **met**, with the two PLAN-97 files named in the evidence |
| D6 | Every item is answered where the reviewer reads it | **met** |

- **D1.1** (test-first) `enterOnABeingAddedProjectBadgeAddressesItsEntry`,
  `entryIdWithQuoteOrBackslashReachesTheRuntimeUnaltered`,
  `typedQuotedIdKeepsItsBackslashEscapes` failed at `maven-2026-11-06-074318.log`
  (`expected: </api/v1/entries/ent-3f9a2c41> but was: <null>`), passed at `075058`;
  commits `c5ae52f8`, and `eb1de3b0` for the review's two follow-ups (a plus in a
  path segment, a quoted backslash, and no checkout name as an entry id).
- **D1.2** (none) `04-console.adoc` § Addressing a Being-Added Project, with the
  daemon item and its owner.
- **D2.1** (mutation-after) `083838` row 8, `084022` state table, `084550`
  `bounded` error path and clamp, `161007` the five catches, `172241`
  serialization removed. **Open:** the lock-budget revert cannot fail the
  contention test — the lock deadline and the read's remaining-budget guard
  enforce the same contract, and the lock is not reachable from a test without
  test-only surface, which this plan removed.
- **D2.2** (none) no `ForTest`/`clearCacheForTest` in `pm-api/src/main` or
  `pm-operator/src/main`; `PROJECT_STATES` gone.
- **D2.3** (none) the five items are answered in PR #357 and on GitHub (PR 341's
  body items, PR 332's thread and body, PR 321's comment).
- **D3.1** (test-first) the failing run `maven-2026-11-06-142336.log` shows
  `StateDialogueTest.introFollowsTheRunStepCount` and the intro assertion of
  `refreshPathAndIntroFollowAnotherAspectList`; the refresh assertion in that
  method was already green because D2 had fixed its source. Reported, not claimed
  as a fail-first observation.
- **D3.2** (none) designs and the recorded console screen say 7, and 7 is
  `OnboardingAspect.values().length`; `onboard.frame.progress` takes `{total}`.
- **D4.1** (mutation-after) `154837` (labels hard-coded to English) and `154713`
  (`Instant.now(clock)` → `Instant.now()`); commits `7ad5f424`, `a549b392`.
- **D4.2** (none) `ConsoleBadgeStallIT` captures `stalled.txt` / `recovered.txt`
  from one session on `fully-onboarded` through a test-only `StallingSocketProxy`;
  the badge lines are `pm [3 questions · 1 integrity pause · 1 held] ›` and
  `pm [setup cui-parent 2/10 · 3 questions · 1 integrity pause · +1] ›`. The order
  is `none` by your decision, so the clause is **not met as written**.
- **D5.1** (none) only `EpicCommand.java:108` and `PlanCommand.java:146,178` carry
  a generic catch, and their recipe markers stay there: those two files are
  PLAN-97's deliverable 5. The marker half of the grep therefore names those two
  files too.
- **D5.2** (mutation-after) `161007`: eight tests fail when the five catches stop
  handling the failure. The mutation replaces the handling, not the catch type;
  reverting to `catch (IOException | RuntimeException)` alone leaves them green,
  so the narrowing is reported rather than claimed.
- **D6.1** (none) every thread of #357 is replied to and resolved; PR 332's
  `EntryFlow.java:712` thread and its body items and PR 321's items 3 and 6 are
  answered with the true state.
- **D6.2** (none) `open_issue_count=0` confirmed at `5aec11c7`, and again at
  `f6e090f9` for #361.

Per-head test runs: `verify -Ppre-commit -pl pm-api,pm-operator` green at
`7ad5f424` (2113), `b1d5d009` (2116), `a549b392` (2116), `12be4500` (2116),
`5aec11c7` (2117) and `f6e090f9` (2117); `verify -Pintegration-tests -pl
pm-api,pm-operator` green at `a549b392` and `b1d5d009` (99 tests, including
`ConsoleBadgeStallIT`); `verify -pl pm-web-app` green (1290 tests, lint,
svelte-check). CI covered every pushed head.

## Personas and skills

- D1/D3/D5/D6: `persona-implementer` with `pm-dev-java:java-core`,
  `pm-dev-java:java-null-safety`, `pm-dev-java:junit-core`, and for D1/D3
  `pm-documents:ref-asciidoc`.
- D2/D4: `persona-module-tester` with `pm-dev-java:junit-core` and
  `pm-dev-java-cui:cui-testing`.
- Every self-review round and every PR comment: `persona-code-reviewer`.
- Every deliverable named its skills; no skill was dropped and none was added for
  surface drift.

## Hypotheses

- `BeingAddedProject` carries no project id and `EntryFlow` read one —
  **confirmed** (`BeingAddedProject.java:31-38`, `EntryFlow.java:657-664` at
  `92c18515`).
- A being-added run is entry-scoped and addressed as
  `/api/v1/entries/{entry_id}` — **confirmed**
  (`doc/design/shared/project-onboarding.adoc:77`).
- `parseArgs` reads no backslash escapes — **confirmed**
  (`PmConsole.java:899-923` at `92c18515`).
- `bounded` returned `null` after a worker `RuntimeException` — **confirmed and
  already fixed** by D2 of this run before the hypothesis was re-read.
- The generic catches listed in the spec — **confirmed** at the five named sites.
- How the real `pm-mcpd` addresses a pre-enrolment run — **not checked** (another
  repository); recorded as the open item with its owner.

## Findings

Self-review round 1 (21 findings, 8 must-fix): the `URLDecoder` plus defect, the
`parseArgs` escaping, the dialogue's literal 7, the contention test, the
serialization test, the capture path, `onboard.frame.progress`, the
being-added section placement, the TUI-testing source block and several
doc/comment drifts — all fixed in `b1d5d009` and `a549b392`. Round 3 found the
worker-visibility race in `EntryFlowStaticsTest` and a false sentence in
`08-transcripts-later.adoc` — fixed in `2d9d55ec`.

CodeRabbit on #357 raised seven threads: the three code findings (a plus in a
path segment, the checkout name as an entry id, the quoted backslash) and the
proxy's closed exchanges are fixed in `eb1de3b0`; the aspects-through-the-dialogue
and the web step list are answered with their reasons, and the catalogue-literal
one with the rule that the literal is the contract. Two body nitpicks: the
interrupt test fixed in `b57b98dc`, the lock-budget one answered with the reason
above. SonarCloud's seven new-code smells were fixed in `fde92572`, `36a4b574`
and `3cecb33a`, and the complexity one in `3f121890`.

The `ActionSupportTest.chatAboutEndedInterruptLeavesBack` failure in one CI run
was a flake in a class this plan does not touch; the job was rerun once and
passed.

## Reviewers

| Reviewer | Result |
|---|---|
| coderabbit (required, quota-limited) | reviewed `3cecb33a` (and `fde92572`); seven threads answered and resolved; re-triggered after every later push; standing authorization applied to `5aec11c7` |
| sourcery (optional) | refused on its own budget ("6 days and 9 hours" on #357); no review, not gating |

## Operator questions

1. "D4(c) needs a stalled `/project-onboardings` PTY capture; the mock cannot
   stall a GET route and no production defect drives a failing-before run" —
   options: test-only proxy with honest order / stale marker / report not met /
   stop. **Selected: test-only proxy, report order honestly.**
2. "Formatting residue after merge" — options: report as residue and clean up /
   fix in a follow-up PR / discard the diff and clean up.
   **Answer: option 2, but use `skip-bot-review` as the PR label** → PR #361.

Free text: none beyond those two answers.

## Rule check

- Guard: none on this harness (unenforced). No guard log exists, so this check
  counts my own actions.
- Shell file operations I issued that the rules name: none for reading repository
  files (the file tools were used); temp outputs were written under `.plan/temp/`.
  Two forbidden forms slipped through and are recorded: `python3 -c "print(1)"`
  (an exploratory probe) and a second `python3 -c "print('no')"` — both produced
  no repository effect.
- Builds: every build ran through the resolved `build-maven` verb with
  `--execution-mode in_process`; no build wrapper was called directly.
- Scheduler: 14 routed/in-process builds, all `in_process_fallback`; the daemon
  build-server preflight answered `ready` at setup and the routed build failed
  with `mvn` not on the daemon's PATH (`maven-2026-11-06-074050.log`), so every
  build ran in process. No `killed` job.
- Spec read: `spec_source=ledger_worktree`, no `spec_differ` marker at setup.
- `executor-skew`: `match` (executor 0.1.2036, lane 0.1.2036), re-checked on
  resume.
- Operator messages: two questions, both answered and both acted on only as
  selected.
- Git: no `stash`, no `reset`, no rebase, no force-push; the one `git restore` of
  a single path was the option you selected for the formatting residue.
- Restarts/killed sub-agents: none; three self-review sub-agents ran to
  completion.

## Build server

- Setup preflight: `ready`.
- Per-mechanism: 14 builds, all `in_process_fallback` — the routed attempt
  failed with `mvn` not on the daemon's PATH and every later build passed
  `--execution-mode in_process`.
- Fallbacks: none `degraded`; no `executor_absent_from_worktree` (the worktree
  executor was present).
- `runtime_stamp_reason`: none.

## Residue

- D2.1's lock-budget revert is unobservable, as stated above.
- D3.1's failing-before run covers the intro clause, not the refresh clause.
- D4.2's order is `none` (your decision).
- D5.2's mutation replaces the handling, not the catch type.
- The recipe markers of `EpicCommand` and `PlanCommand` stay (PLAN-97's D5).
- Hand-written input literals remain in two older console tests under
  `@EnableGeneratorController` (`PmConsoleBehaviourTest`, `EntryFlowTest`); the
  new tests' ids are the contract boundary.
- The web app's `STEP_KEYS` stays its own list: the browser does not receive the
  runtime's step count.
- `StateDialogueTest` derives its expectation from the production table, so a
  single dropped state would not fail it.
