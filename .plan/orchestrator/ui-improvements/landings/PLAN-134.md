# Landing Analysis: PLAN-134 — Chat Mock Context, Narrative, Queued Revision

epic: ui-improvements
workstream: WS-07
pr: #428 (https://github.com/plan-marshall/pm-mcp-clients/pull/428), squash `7adb56c2` (12 files, +2971/-164), PR head `e6e2b417`

> Written by the `analyze` verb on 2026-11-14. Source: `rb-134-chat-mock-context-narrative-queued-001.md` (rev 2).
> Checked against `git show 7adb56c2`, the PR's review threads and the SonarCloud public API. The orchestrator
> spot-checked `applyChatProposal`, `revisionWindowOpen`, `activityOf` and `refusedRevision` at the merge commit.
> Run: runbook lane, opencode harness, deepseek-v4.1-flash/high, executor 0.1.2118. Merged by the lane.

## Deliverable Fidelity vs Spec

CR = `ChatRegistry.java`, ME = `MockRuntimeEngine.java` (both in `pm-mock-runtime`, at `7adb56c2`).

| Deliverable | Verdict | Evidence |
|---|---|---|
| D1 knows | shipped for plans, partly for epics and entries | ME:946-984 covers plans. Epics and entries get `description` only (ME:952-953). |
| D2 narrative | partly | CR:983-1009 covers phase, task, decision, hold, activity and instance. Missing: "PR merged", and epic and entry lines. |
| D3 queued revision | partly | Implemented. Row, `activity.changed` and order are pinned for accept only (`ChatRevisionQueueTest:100-128`). |
| D4 refusals | partly | Implemented at CR:1666-1723. No conflict or lapse notice (the catalogue has no key for it). One test covers two refusals. No test for "another subject". |
| D5 channel | shipped | ME:7615-7630 and ME:10621-10624. Tested on both listeners. |
| D5 scenarios | **not shipped** | The diff touches nothing under `src/main/resources`. |
| D-new-1 (m2) | **not shipped** | `applyChatProposal` (ME:918-931) still only bumps the revision. |
| D-new-2 (m3) | shipped | Abandoning deletes the transcript (CR:862-867, 913-922). Tested. |
| D-new-3 (m4) | partly | The route parse is fixed and tested. The race is fixed under one lock (CR:688-699), with no test. |

The landing's "D3/D4 met" and the PR body's "every plan, epic or entry event" overstate what the diff does.

## Three-Layer Analysis

- **Layer 1 (specified): sound, with gaps.**
  - D4 "asserts the notice" cannot be met: pm-mcp-core `state-catalogue.adoc` § `chat-proposal-states` names a notice that the closed line
    catalogue lacks.
  - D2 left out epic and entry events.
  - The plan had 8 items, over the ~6 scope guard. The dropped items were the last ones: the fixtures that
    downstream plans consume.
- **Layer 2 (implemented):** see the table above. The evidence order was `mutation-after`, not `test-first`, and
  a single failing run reverts three pieces at once.
- **Layer 3 (correct):**
  - No lock inversion: the registry monitor is taken before `controlLock`, and publishing happens after the lock
    is released. The revision apply is atomic.
  - **Major M1:** `scope.ended` does not close the revision window. `revisionWindowOpen` ME:994-1014 ignores
    `planActivities`.
  - **Minors:**
    - m1: `pausing` is narrated as `paused`, and the real `paused` is then deduplicated.
    - m2: a decision is bound to a plan by id prefix (ME:1024-1038).
    - m3: a REFUSED proposal cannot be rejected.
    - m4: infeasible is shown as done.
    - m5: a broadcast accept skips the refusal checks and can throw an NPE.
    - m6: a notice is re-completed for each line it gains.
    - m7: the hold `H-1` is hard-coded in scripts.
    - m8: events are delivered before progress is recorded.
    - m9: no `instance.changed` on an applied revision.
    - m10: test gaps.
- **Confirmed sound:**
  - D1 for plans, the D5 channel, transcript deletion and the route parse;
  - concurrency in the paths that were read;
  - conformance with CLAUDE.md (JUnit 5, Generators, javadoc, no generic catch);
  - Sonar.

## Routing and Merge Behavior

- **CodeRabbit:**
  - Reviewed six heads, the last being `6e1a7bb8`. The merge-group head was not re-reviewed (auto-paused); the
    one-review rule is met.
  - All 18 inline threads are resolved, and the landing says 19/19 findings were handled.
  - CodeRabbit wrote "not marking as addressed" on 4 follow-ups that were still unfixed at merge: M1 4573728016,
    m1 4573726390, m2 4569017923, m3 4573384117.
  - The last two inline threads were closed with the replies meant for that review's outside-diff findings
    (4573720085 and 4573722267). The findings were answered by position, not by content.
- **Sourcery** refused the PR: it is over its 150k-character limit.
- **Self-review:** 18 findings. The blocker and the majors were fixed in `330159bc`. The rest were not itemised
  anywhere (`budget-exhausted`).
- **Sonar** at `e6e2b417`:
  - 0 open issues, 0 hotspots, quality gate OK;
  - new-code coverage 87.3%, duplication 0%;
  - 47 issues resolved, all FIXED.
- **CI** was green at every head, and the merge-group run 43603178531 was green.

## Reconciliation Actions

Operator decisions 2026-11-14 (AskUserQuestion):
- [x] **Residue:** "Fold into PLAN-140", as **D-R134**. It runs ahead of the CLI deliverables, in the same stream
  as the `role_unqualified` registry work. It covers:
  - the D5 scenarios;
  - D-new-1;
  - the race test;
  - M1, m1, m2, m4 and m5;
  - the D3/D4 pins;
  - narrative completeness;
  - m6, m7 and m9.
- [x] **D4 notice:** "Add a line key". The key is named first in pm-mcp-core `state-catalogue.adoc` § `chat-narrative`
  (epic Open Item). The mock, `control.en/de.properties` and a notice turn follow in PLAN-140 D-R134.
- [x] **REFUSED:** "Rejectable". `isOpen` is extended for reject only (PLAN-140 D-R134). The contract text is
  edited in pm-mcp-core `state-catalogue.adoc` (epic Open Item "chat proposal contract"). The PLAN-136 and PLAN-138 UIs offer reject.
- [x] **Amendments:**
  - PLAN-136: facts, decisions and the fixture fallback through `/mock/broadcast`.
  - PLAN-139: the verdict is corroborated, plus the pause bug.
  - PLAN-138: the states that exist.
  - PLAN-137: the fixture sequencing.
  - PLAN-135: notice turns and `context`.
  - Engine and registry line-ref re-grounds in PLAN-30, 141, 145, 158, 159 and 160.
- [x] **Lessons:** "Adopt all 7". Filed in plan-marshall's store (see the log entry).
- [x] Row `shipped`, pr 428, landing. Message 001 archived. Anchor, log and view.

## Follow-Ups

- **Sequencing tension:**
  - PLAN-140 depends on PLAN-138 and PLAN-139.
  - PLAN-136, 137 and 139 want the fixtures that are now in PLAN-140 D-R134.
  - Until D-R134 lands, they drive events through `/mock/broadcast`. If that proves too weak, split D-R134 into
    its own plan.
- **PLAN-30 is running.** Its amendment is line refs only, so no mailbox message was sent.
