envelope_version=1
sender_type=plan
sender_id=plan-133-chat-mock-runtime
epic=ui-improvements
kind=candidate-lesson
created=2026-11-13T12:25:24Z

# Candidate lesson: a contract change in the mock leaves consumer clients behind

**Plan:** plan-133-chat-mock-runtime (PR #424, merged as a139ec62)
**Signal:** automated review (CodeRabbit) and code-review Q-Gate
**Class:** the plan changed the chat contract the mock runtime serves (routes moved to plan/epic chat routes, request bodies now require `client_turn_id` / `turn_id`, the start-entry chat link removed), but the clients that call those routes — the web app and the operator CLI — were not changed in the same plan. Their tests passed because they mock the client layer, so the breakage showed only in review.

## Records of the class

- 9ac994 (CodeRabbit, MockRuntimeEngine.java:3860): the web client still used the removed project-path ended-work chat route; fixed in TASK-33.
- 1fbda9 (code-review Q-Gate, pm-web-app client.ts:2447-2456): the web follow-up accept sent an empty body where the route requires the proposal `turn_id`; fixed in the next round.
- 200226 (CodeRabbit review body, outside diff, StartDialogue.java:630-650): the operator open-chat action posted to a link the mock no longer served; fixed in TASK-34.
- 78b17f (CodeRabbit review body): the web client's `chatWithSubject`, `decisionChatTurn` and `answerSubjectChatProposal` still post bodies without `client_turn_id` / `turn_id`. Accepted as out of scope and already filed by triage as **lesson 2026-11-12-23-001** (web client chat migration items) — this message does not duplicate that lesson; it names the general class behind it.

## Observation

Three review rounds each found another consumer call site of the changed contract. The remaining web items were deferred to the existing lesson 2026-11-12-23-001.

## Candidate corrective action

When a plan changes a route or a request/response shape, its outline lists every consumer call site of that contract (web client, operator CLI, relay) found by a content search, and either migrates them in the same plan or records the deferral as an explicit deliverable gap up front — not one call site per review round.
