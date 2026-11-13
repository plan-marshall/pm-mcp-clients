# Landing Analysis: PLAN-133 — Chat Mock Runtime (Routes, Streaming, Timing, Failures)

epic: ui-improvements
workstream: WS-07
pr: #424 (https://github.com/plan-marshall/pm-mcp-clients/pull/424), squash `a139ec62` (86 files, +8708/-1194), merge head `87e50608`

> Written by the `analyze` verb on 2026-11-13. Source: `plan-133-chat-mock-runtime-024.md` and 23 candidate lessons (`-001` to `-023`).
> Verified against git, the PR's review threads, the SonarCloud public API, and the archived plan
> `.plan/local/archived-plans/2026-11-13-plan-133-chat-mock-runtime/`.
> Run: /plan-marshall, claude-sonnet-5-5 at effort low, plugin `0.1.2106`, ~64 M tokens, ~55 h wall time.

## Deliverable Fidelity vs Spec

| Deliverable | Verdict | Evidence |
|---|---|---|
| D0 Wire keys | shipped (residual) | `ChatMessageKeys.java` maps `chat.*` to `control.chat.*`. `ChatMessageKeysTest` checks it against the en/de packs. Residual: the hand-kept `wireKeys` list; reasons and findings are checked in one direction only. |
| D1 Registry | shipped (residual) | `ChatRegistry` serves plan, epic, entry, decision (5 scopes) and ended work (`:312-348`). `ChatScenarioTest` covers it. Residual: paging, refused-while-writing and accept/reject are tested on plan/entry only. 5 old tests were removed (operator-accepted, `entry_chat_old_shape=A`). The removals lost "abandoning deletes the transcript" (m3). Review and decomposition chats are left out (O10). |
| D2 Timing and events | shipped-as-specified | `MockTiming`, `EventTiming`, `--timing`. `ChatReplyEventsTest.java:111-292` covers ordered and realistic timing, and `chat.ended` for 5 causes. |
| D3 Failures | shipped-as-specified | `ChatRegistry.handleInject` (`:587`). `ChatInjectTest` has one case per kind, plus stop and worker-replaced. |
| D4 Scripts, proposals, second device | mostly | `ChatScripts`, `ChatProposalsTest`, `ChatBroadcastTest`. **m2:** an accepted row or draft proposal only bumps `revision` (`MockRuntimeEngine.java:874-890`). |
| D5 Migration, validation | partly | `ScenarioLoader` and `StateDefinition` validate chat events and keys, failing closed. **M2:** the consumer survey was false. |
| D6 PLAN-132 residue | shipped-as-specified | `ChatTurn.requireOperatorTurnRule`, `EventName.requireStreams`, `EventStream.scopeKind()`, and the absent device stays absent. |
| D8 Verification, Sonar | not for web | CI green; Sonar 0. The web and terminal consumers were left behind (M1). |

Added but not planned: `ChatRoutes` usage in `StartApi`/`ChatDialogue`. The operator start chat was migrated to B1 with streaming, by turn id.

## Three-Layer Analysis

- **Dispatch:**
  - One read-only leaf (`execution-context-level-3`).
  - The orchestrator spot-checked these at `origin/main`:
    - `showroom.spec.js:76-80` (the entry chat step);
    - `StartRegistry.java:616-620` (no `chat` act);
    - `client.ts:3484-3500` (`chatWithSubject` posts `{ text }`).
  - Confirmed.
- **Verdict: no blocker.**
  - **Layer 1:**
    - The spec required migrating "operator and web tests that read the old chat shape". It did not require migrating the consumers' production code.
    - The run itself excluded the web and operator production code ("Exclusions") without asking the operator.
    - D1 ("left out until O10") and the O10 amendment ("once PLAN-132 has the route builders") conflict.
    - The size estimate was 16 files; the PR changed 86.
  - **Layer 2:** D5 and D8 are partly met.
  - **Layer 3, majors:**
    - (M1) The web chat is broken against the mock on `main`: entry, plan, epic, review and ended-work chats, and the decision chat address. So are the terminal's `plan|epic chat` (`WorkChatDialogue`, `WorkActionsClient.java:305-347`). The tests stay green because they run on fakes.
    - (M2) The D5 survey "pm-web-e2e has no chat reference" is false. `showroom.spec.js:76-80` fails under `-Pe2e`, which is not in the PR gate. PLAN-148 to PLAN-156 gate on it at the head.
    - (M3) Lesson `2026-11-12-23-001` understated the gap. The owners of the migration (PLAN-135, 137 and 138) are parked.
  - **Layer 3, minors:**
    - (m1) No plan owns the O10 route builders.
    - (m2) Accepted proposals only bump the revision.
    - (m3) Abandoning no longer deletes the transcript, and no test pins either behaviour.
    - (m4) The `playFailure` check-then-act race. The `indexOf("chat")` route parse.
    - (m5) The `StartApi` poll does not check the interrupt flag.
    - (m6) The PR body is stale: its checkboxes are unticked and it carries no counts.
    - (m7) One `cui-rewrite:disable` suppression (conforms).
- **Confirmed sound:**
  - The mock and `pm-api` code against the spec.
  - The 5 test removals (justified, with replacements).
  - The fail-closed fixture validation.
  - Sonar.
  - The trailer.

## Routing and Merge Behavior

- **Reviews:**
  - CodeRabbit reviewed `f1d98258`, `a83dcea9` and `0e017638`. It did not review the merge head `87e50608` (quota). The one-review rule is met.
  - 16 of 16 threads have a reply and are resolved.
  - "3/3 body findings" counts review bodies, not items. Of the items, 4 were closed without a fix:
    - 2 web Majors: the turn body and the proposal `turn_id`. They now go to PLAN-161.
    - 2 mock Minors (m4).
- **Head `0e017638`** was pushed while the PR was CONFLICTING. No CI ran on it, and the CI wait reported green from older runs (lesson `2026-11-13-14-003`). It was fixed by the baseline merge `78d21a9b`.
- **Sonar:** 0 open issues and 0 hotspots (21 issues, all closed) at `87e50608`, quality gate OK.
- **Merge:** merged by the run through the queue.

## Reconciliation Actions

Operator decisions 2026-11-13 (AskUserQuestion):
- [x] E2E window: "Fold into PLAN-148". PLAN-148 gets **D6**, which marks only the `showroom.spec.js` chat step `test.fixme` with PLAN-161 as owner. The premise note goes to PLAN-149 to PLAN-157.
- [x] Migration: "New bridge plan now". **PLAN-161** `chat-client-bridge` is staged (WS-07, runbook lane). It moves the web client calls, the views (entry, plan, epic, review, ended work, decision when the scope is known) and the terminal `WorkChatDialogue`/`WorkActionsClient` to B1, and lists the consumers. PLAN-135, 137, 138 and 140 are amended to replace the bridge later and re-ground their line references.
- [x] Minors: the recommended split:
  - m2, m3, m4 → PLAN-134 (with the D1 re-ground);
  - m5 → PLAN-140;
  - m1 → epic Open Item (stage the builders plan before PLAN-137 D2 / PLAN-140 D2 once the N3 docs name the addresses);
  - PLAN-155 D5 chat items marked stale.
- [x] Lessons, "Adopt all": 16 filed in plan-marshall's store, `2026-11-13-14-001` to `-016`:
  - consumer enumeration;
  - re-homing a removed route's effects;
  - the CONFLICTING CI wait;
  - planned test removals;
  - verification modules from the mutate set;
  - git `FETCH_FAILURE`;
  - `review_support` nested classes;
  - plan sizing;
  - Sonar limits and the rule pass;
  - hand-kept set copies;
  - S4449 recurrence;
  - unbounded SSE reads;
  - lenient validators;
  - the transition table;
  - `execute_commits_unrecorded`;
  - stale finding records.

  Evidence was added to `2026-11-12-19-002`, `2026-11-13-10-002`, and `2026-11-12-23-001` (pm-mcp-clients' store, now lists the full gap). Candidates 5, 13, 15 and 23 were not filed: 5 was noted into PLAN-138; 13, 15 and 23 were weak or a triage pointer.
- [x] Epic Open Defect and Open Item.
- [x] Row `shipped`, pr 424, landing. Messages 001 to 024 archived. Anchor, log and view.

## Follow-Ups

- **PLAN-148 (running):** its lane reads the spec at start. The D6 amendment reaches it only if the run re-reads the spec. Otherwise the operator should tell the session.
- **PLAN-134 and PLAN-161** become emittable. PLAN-138 still waits for O8/O9 and PLAN-42/44/48.
