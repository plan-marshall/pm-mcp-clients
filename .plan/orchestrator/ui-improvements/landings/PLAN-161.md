# Landing Analysis: PLAN-161 — The Web and Terminal Chats Talk to the Chat Contract Again (PLAN-133 Residue)

epic: ui-improvements
workstream: WS-07
pr: #429 (https://github.com/plan-marshall/pm-mcp-clients/pull/429), squash `dc811fb3` (28 files, +2014/-633)

> Written by the `analyze` verb on 2026-11-14 from `rb-161-chat-client-bridge-001.md` (revision 2). Verified by reading against git,
> GitHub GraphQL and the SonarCloud public API. Runbook lane: opencode `deepseek-v4.1-flash/high`, executor `0.1.2118`.

## Deliverable Fidelity vs Spec

| Deliverable | Verdict | Evidence (at `dc811fb3`) |
|---|---|---|
| D1 Web client speaks B1 | shipped | One normaliser, `normaliseChat` (`client.ts:1656`). `client_turn_id`, `turn_id`. Replies are followed by turn id and status, bounded at 20 s (`:1710`, `:3763`). Only open proposals are carried. Production code grew by about 900 lines, mostly the follow rule and StartView state. |
| D2 Web views | shipped, with gaps | The entry chat opens through `ChatRoutes`; the plan, epic and review chats run; the showroom fixme is removed (e2e: 395 passed). **M1:** plan, epic, review and decomposition chats are only READ, never OPENED. The StartView refresh machinery has no test and a residual race (m). |
| D3 Terminal | shipped | `WorkActionsClient` is on B1 (20 s bound, checks the interrupt flag). `ChatReadBackResource` is deleted completely. The same GET-only gap applies (`:320`). A failed read after a recorded turn loses the reply (`:395`). There is a null-guard oddity at `WorkChatDialogue.java:142-144`. |
| D4 Consumer list | shipped | The list is complete. The only old-shape route left is the dead decision chat (`client.ts:3807-3860`, unserved). |

## Three-Layer Analysis

- **Dispatch:** one read-only leaf. Verdict: no blocker.
- **Layer 1:** the spec is sound. Both of its hypotheses were settled correctly, and the run followed the stop-and-ask clause.
- **Layer 3, majors:**
  - **M1 — no chat is ever opened.** `POST …/chat` is used for the entry only. Against the mock, an unseeded plan, epic or review chat answers 404 and the panel shows "chat closed" (4 running plans in `running-work`). The terminal refuses. The decomposition read-back sends turns to a chat it never opened.
  - **M2 — two operator decisions rest on contract surfaces no plan delivered.** The ended-work follow-up shows a notice that leads nowhere, and the decision chat says "no chat". PLAN-137 D3/D4, PLAN-138 D4 and PLAN-140 assumed those surfaces.
- **Minors:** the race guard is untested, with a residual same-generation race (CodeRabbit's last word on two threads); Try again draws a new `client_turn_id`; `awaitReply` loses the snapshot on a failed read; the null guard; orphaned old pm-api chat types with no owner; the review chat shares the epic address (O10).
- **Confirmed sound:** wire correctness against `ChatRoutes`, `ChatTurnRequest`, `ProposalAnswerRequest` and `ChatResource`; the bounds; deletion completeness; Sonar.

## Routing and Merge Behavior

- **Review:** CodeRabbit reviewed `c1f71754`, `04e7e2df` and `610d642c`. 9 of 9 threads are replied to and resolved.
- **Review body:** no body-only items. "9/9" counts the threads.
- **Unanswered last words:** two threads end on CodeRabbit's "not marking addressed" (`StartView:77` and `:241`).
- **Sonar:** 0 open issues and 0 hotspots. 16 issues were fixed (S1192, S9382, S1128 ×13, S1172); none was dismissed.
- **Merge:** merged by the lane.

## Reconciliation Actions (operator, 2026-11-14)

- [x] M1, the race and its tests, and Try again → **PLAN-135** (web). The terminal open act, `awaitReply` and the null guard → **PLAN-138**. Operator choice: "Fold into PLAN-135/138", so the chats stay unreachable for unseeded subjects until those plans run.
- [x] M2 + O10 → one **contract Open Item** in the epic (a pm-api + mock plan, staged once the N3 docs name the surfaces). Premises were added to PLAN-137, 138 and 140; PLAN-136 rewords `control.chat.follow_up.opened`.
- [x] PLAN-140 owns the orphaned `control.Chat*` and `StartStateResource.chat` types.
- [x] Lessons: `2026-11-14-10-001` (seeded-only evidence), `-002` (carry a mid-run contract gap into specs), `-003` (a race-fix reply names its test).
- [x] Row `shipped`, pr 429, landing; message archived.
