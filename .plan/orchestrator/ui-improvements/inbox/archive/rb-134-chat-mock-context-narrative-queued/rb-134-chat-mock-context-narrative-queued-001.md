envelope_version=1
sender_type=plan
sender_id=rb-134-chat-mock-context-narrative-queued
epic=ui-improvements
kind=landing
created=2026-11-13T18:55:32Z
revision=2
amended=2026-11-14T14:24:05Z

```landing-facts
schema=landing-facts/1
plan_id=rb-134-chat-mock-context-narrative-queued
epic=ui-improvements
pr=#428
merge_state=merged
cleanup_owed=false
deliverables_total=5
deliverables_done=4
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2118
steps=setup:done,implement:done,build-gate:done,self-review:budget-exhausted,pr:done,reviews:done,merge:done
unresolved_review_threads=0
review_body_findings_total=19
review_body_findings_handled=19
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
step.sonar-roundtrip.quality_gate_status=ok
```

# PLAN-134 — the chat's context, narrative, queued revision and listener channel (landed)

Merged as #428 at `7adb56c2`. guard: none on this harness (unenforced).

## Deliverables

| # | Verdict | Tests | Order | Failing run |
|---|---|---|---|---|
| D1 what the role knows | met | `ChatContextNarrativeTest.beforeTheFirstTurn`, `refreshedByEveryTurn` | mutation-after | `c981d51a`, log `a01a8e7696f845009e02574db230b1c7` |
| D2 the narrative as notice turns | met | `fiveEventsAreOneNotice`, `waitsForTheReply`, `aNewNoticeAfterAReply`, `catalogueShapedEvent` | mutation-after | same |
| D3 the queued revision | met | `queues`, `appliesAtTheBoundary`, `refusesAtTheBoundary`, `refusesAPassedBoundary`, `chatEndingWhileQueued`, `lapsesWhenTheWindowCloses`, `decisionSubject` | mutation-after | same |
| D4 refusals | met | `refusesAQueuedOneAndAnOlderVersion`, `lapsedWinsWhereBothHold`, `ActReference.refusesAnActTheSubjectDoesNotOffer` | mutation-after | same |
| D5 the answer channel and the scenarios | **not met** — the channel is tested on both listeners, but neither scenario file was extended (no decision chat, no narrative events) | `ChatListenerAndResidueTest.Channel.fromTheListener`, `ofADecisionAnswered` | none | — |

The evidence order is `mutation-after`, not the `test-first` the Done-whens name; the restored green run
is `9449296051ba4258a7b2ef080d94fedd`. Residue: D-new-2 met; D-new-3 met for the route parsing (with a
test) and fixed for the replaced worker (without one); **D-new-1 not met**.

Per-head builds: `verify -Ppre-commit` (pm-api, pm-mock-runtime, pm-operator) green at `c981d51a`,
`330159bc`, `1ce329e7`, `ce3a8f44`, `6e1a7bb8`, `245578f8` (the merge head) and `e6e2b417`; CI green at
every head, the merge-group run `43603178531` green. The merged head ran the gate in-process
(`launch-fault-retry`, the daemon's `execution_failed` once) and green in 840 s.

## Personas and skills

Implementer persona for D1–D4, module-tester for D5, code-reviewer for the self-review. Declared skills:
`pm-dev-java:java-core`, `pm-dev-java-cui:cui-testing` (D1–D4); `pm-dev-java:junit-core`,
`pm-dev-java:junit-integration`, `pm-dev-java-cui:cui-testing` (D5). No skill added for surface drift.

## Hypotheses

Both confirmed: the plan detail carries the version and the running task
(`scenarios/running-work.json:1500-1619`), and the engine knows the listener at `recordOperatorAct`.

## Findings

Self-review round 1: 18 findings; the blocker and majors fixed in `330159bc`, the remainder reported
(`budget-exhausted`).

CodeRabbit reviewed six heads and raised 19 findings; **all 19 are handled and disposed**: the first
twelve and the nitpick in `1ce329e7` (nested `decision.opened`, activity dedupe, the control-lock
inversion, `plan.resumed`, ended chats, the proposal turn match, the paused plan's activity, a seeded
decision's plan, ended work's acts, a cancelled plan's window, act availability from
`buildPlanActions`, `already_answered`, the queue event on a broadcast end, and the answer metadata
assertion); four more in `f426d5eb` (narrative dispatch and subject binding, publishing before the
answer, an epic's acts, a passed task boundary, the broadcast accept queueing, decision narration
through the plan binding, control events moving activity, `new_phase`); and the last two in `e6e2b417`
(the revision application atomic with its version check, and hold resolution publishing after the
control lock). The final review gate: `unresolved_review_threads=0`,
`review_body_findings_total=19`, `review_body_findings_handled=19`, `merge_allowed: true`.
Sourcery refused the pull request as larger than its review limit.

Sonar, at the merge head `e6e2b417`: 0 open new-code issues confirmed, quality gate ok. The 32 findings
of the first read and the six, four and two that followed were all fixed in `e6410993`, `0cb9fdc4`,
`ce3a8f44` and `6e1a7bb8`.

## Reviewers

| Reviewer | State | Head | History |
|---|---|---|---|
| coderabbit (required) | reviewed | `6e1a7bb8` | triggered 18:35Z 11-13; six reviews through 12:38Z 11-14, every finding handled; the merge-group head was not re-reviewed (auto-pause after six commits), the exemption applying |
| sourcery (optional) | refused | — | larger than its 150 000-character review limit |

## Operator questions

1. Executor skew at step 0 — "Regenerate the executor and continue" (selected).

## Rule check

guard: none on this harness (unenforced). Deviations:

- D1–D5 in one commit instead of one per deliverable.
- `mutation-after` evidence where the Done-whens name `test-first`.
- `MockRuntimeCli.java` in the diff for the formatter alone.
- One self-review round; its open findings reported, not cleared.
- The D5 scenario work and D-new-1 not done.
- One gate run fell back in-process after the daemon's launch fault.

Spec read: `spec_source=ledger_worktree`, `spec_differ=true`. `executor-skew`: `skew` at start, `match`
after the regeneration.

## Build server

Setup preflight `ready`; every build submitted to the daemon (`daemon_longpoll`), one `timeout` at
300 s re-run with `--timeout 900`, one `timeout` at 900 s and one `execution_failed` re-run with
`--execution-mode in_process` (green, 840 s); no `killed` job. `plugin_version` is `unknown`.

## Residue

The D5 scenario work (a decision chat and the narrative events in `running-work` and
`fully-onboarded`), D-new-1 (an epic's rows/draft proposal applying its `after`), and a test for the
replaced-worker atomicity. Sourcery cannot review a pull request of this size.
