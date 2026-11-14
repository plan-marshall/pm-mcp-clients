envelope_version=1
sender_type=plan
sender_id=plan-133-chat-mock-runtime
epic=ui-improvements
kind=candidate-lesson
created=2026-11-13T12:25:49Z

# Candidate lesson: chat state-machine edge cases found one per review round

**Plan:** plan-133-chat-mock-runtime (PR #424, merged as a139ec62)
**Signal:** code-review Q-Gate (6-finalize), plus CodeRabbit
**Class:** the mock's chat registry is a state machine (chat.turn, chat.reply streaming, proposal open, ended) driven by operator acts, timed playback and lifecycle events from other registries. Transitions that cross two of these were not designed as a table, and the code review found them one or two at a time over several rounds.

## Records of the class

- 1f7597 (ChatRegistry.java:826): answering a proposal while a reply is written set the chat to chat.turn and let a second message cancel the first reply, leaving it streaming.
- b6781e (ChatRegistry.java:789): `stop()` on a seeded streaming turn of an ended chat reopened the chat.
- d07ebd (ChatRegistry.java:924): broadcasting chat.turn.delta/progress/completed and unknown `chat.*` names skipped the promised record validation.
- d7b83a (StartRegistry.java:979): an entry's chat was not ended when its plan or epic was created, because plan.started / epic.started carried no entry_id.
- bdd6a0 (MockRuntimeEngine.java:930-931): under ORDERED timing the start state's events were played and then wiped by broadcastReset, so no client received them.
- 0e4812 (ChatRegistry.java:949) and a9e35e (running-work.json:2285): seeded chats named knows items and applies keys inconsistently with the design table.
- 1e9a87 (CodeRabbit, ChatDialogue.java:107): any refusal of the end act was reported as "chat ended" while the chat stayed open; TASK-42 limited it to the already-ended refusal.
- 200226 (CodeRabbit review body, running-work.json:2458): a seeded streaming reply had no playback to complete it, so later messages were refused; TASK-36.

## Observation

These findings were spread over code-review rounds at 13:03, 14:26, 15:32 and 19:21, and contributed to the review budget being exhausted (see the separate message on review budgets).

## Candidate corrective action

For a component with an explicit state machine, write the transition table (state x event, including timed playback, seeded states and external lifecycle events) in the design or outline first, and derive one test per cell, so cross-cutting transitions are covered before review instead of discovered in it.
