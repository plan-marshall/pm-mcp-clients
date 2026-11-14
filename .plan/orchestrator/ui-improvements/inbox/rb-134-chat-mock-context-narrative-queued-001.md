envelope_version=1
sender_type=plan
sender_id=rb-134-chat-mock-context-narrative-queued
epic=ui-improvements
kind=landing
created=2026-11-13T18:55:32Z
revision=1
amended=2026-11-13T21:59:17Z

```landing-facts
schema=landing-facts/1
plan_id=rb-134-chat-mock-context-narrative-queued
epic=ui-improvements
pr=#428
merge_state=n/a
cleanup_owed=false
deliverables_total=5
deliverables_done=4
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2118
steps=setup:done,implement:done,build-gate:done,self-review:budget-exhausted,pr:done,reviews:done,merge:n/a
unresolved_review_threads=0
review_body_findings_total=13
review_body_findings_handled=13
sonar_open_new_issues=32
step.sonar-roundtrip.open_issue_rules=java:S1068,java:S1172,java:S1192,java:S135,java:S1481,java:S3358,java:S3776,java:S3824,java:S4449
step.sonar-roundtrip.quality_gate_status=ok
```

# PLAN-134 — the chat's context, narrative, queued revision and listener channel (amended)

Stopped before the merge: every CodeRabbit finding is handled (12 threads replied and resolved, the 13
review-body findings disposed at `1ce329e7`), CI is green and the review gate answers
`merge_allowed: true` — but the Sonar read on the head `1ce329e7` reports **32 open new-code issues**
(code smells: duplicated string literals `java:S1192` mostly, with `S1068`, `S1172`, `S135`, `S1481`,
`S3358`, `S3776`, `S3824`, `S4449`), against the plan's ZERO-findings standing constraint. The pull
request stays open. guard: none on this harness (unenforced).

## Deliverables

| # | Verdict | Tests | Order | Failing run |
|---|---|---|---|---|
| D1 what the role knows | met | `ChatContextNarrativeTest.beforeTheFirstTurn`, `refreshedByEveryTurn` | mutation-after | `c981d51a`, log `a01a8e7696f845009e02574db230b1c7` |
| D2 the narrative as notice turns | met | `fiveEventsAreOneNotice`, `waitsForTheReply`, `aNewNoticeAfterAReply`, `catalogueShapedEvent` | mutation-after | same |
| D3 the queued revision | met | `queues`, `appliesAtTheBoundary`, `refusesAtTheBoundary`, `chatEndingWhileQueued`, `lapsesWhenTheWindowCloses`, `decisionSubject` | mutation-after | same |
| D4 refusals | met | `refusesAQueuedOneAndAnOlderVersion`, `lapsedWinsWhereBothHold`, `ActReference.refusesAnActTheSubjectDoesNotOffer` | mutation-after | same |
| D5 the answer channel and the scenarios | **not met** — the channel is tested on both listeners, but neither scenario file was extended | `ChatListenerAndResidueTest.Channel.fromTheListener`, `ofADecisionAnswered` | none | — |

The evidence order is `mutation-after`, not the `test-first` the Done-whens name; the restored green run
is `9449296051ba4258a7b2ef080d94fedd`.

Residue: D-new-2 met; D-new-3 met for the route parsing (with a test) and fixed for the replaced worker
(without one); **D-new-1 not met**.

Per-head builds: `verify -Ppre-commit` (pm-api, pm-mock-runtime, pm-operator) green at `c981d51a`,
`330159bc` and `1ce329e7`; the full pm-mock-runtime suite 758 tests green at each; CI green at
`330159bc` and `1ce329e7` (all checks, SonarCloud analysis success).

## Personas and skills

Implementer persona for D1–D4, module-tester for D5, code-reviewer for the self-review. Declared skills:
`pm-dev-java:java-core`, `pm-dev-java-cui:cui-testing` and, for D5, `pm-dev-java:junit-core`,
`pm-dev-java:junit-integration`, `pm-dev-java-cui:cui-testing`. No skill added for surface drift.

## Hypotheses

Both confirmed: the plan detail carries the version and the running task
(`scenarios/running-work.json:1500-1619`), and the engine knows the listener at `recordOperatorAct`.

## Findings

Self-review round 1: 18 findings; the blocker and majors fixed in `330159bc`, the rest reported open
(`budget-exhausted`).

CodeRabbit's review of `330159bc`, 12 inline threads and one review body, all **handled and disposed in
`1ce329e7`**: the nested `decision.opened` subject and blocks; activity deduplication moved after the
subject match and cleared on seed; the control-lock inversion (`handlePlanAction` broadcasts after the
lock); `plan.resumed` settling an `on_start` revision; ended chats excluded from version lines; the
proposal turn-id match; the paused plan's own activity; a seeded decision's plan only where the engine
serves one; ended work naming no act; a cancelled plan closing the window; act availability derived
from `buildPlanActions`; `already_answered` for a decision removed meanwhile; `endChat`'s queue event
forwarded; and the answer metadata assertion. The gate then answered `merge_allowed: true` with
`unresolved_review_threads=0`, `review_body_findings_total=13`, `review_body_findings_handled=13`.

Sonar, open on the head: 32 code smells in `ChatRegistry`, `MockRuntimeEngine` and
`ChatRevisionQueueTest` (the rules above), the zero-findings constraint unmet.

## Reviewers

| Reviewer | State | Head | History |
|---|---|---|---|
| coderabbit (required) | reviewed | `330159bc` | triggered 18:35Z, review 18:47Z, `reviewed` marker names `330159bc`; every finding handled at `1ce329e7`, so the review is the review at any head of this PR |
| sourcery (optional) | refused | — | larger than its 150 000-character review limit |

## Operator questions

1. The executor-skew question at step 0 — "Regenerate the executor and continue" (selected).

## Rule check

guard: none on this harness (unenforced). The run's own deviations:

- D1–D5 in one commit instead of one per deliverable.
- `mutation-after` evidence where the Done-whens name `test-first`.
- `MockRuntimeCli.java` in the diff for the formatter's sake alone.
- One self-review round; its open findings are reported, not cleared.
- Two quality-gate runs hit the daemon's 300 s bound and were re-run with `--timeout 900`.
- The Sonar findings are left open rather than fixed or dismissed by the operator.

Spec read: `spec_source=ledger_worktree`, `spec_differ=true`. `executor-skew`: `skew` at start, `match`
after the regeneration.

## Build server

Setup preflight `ready`; every build in this session was submitted to the daemon
(`daemon_longpoll`), two `timeout` outcomes from its own 300 s bound re-run with `--timeout 900`, no
fallback and no `killed` job. `plugin_version` is `unknown`.

## Residue

PR #428 needs the Sonar new-code findings cleared (or the operator's dismissal), the D5 scenario work,
and D-new-1. Sourcery cannot review this pull request at its size.
