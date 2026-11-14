envelope_version=1
sender_type=plan
sender_id=plan-133-chat-mock-runtime
epic=ui-improvements
kind=candidate-lesson
created=2026-11-13T12:25:03Z

# Candidate lesson: chat client renders a paged or streamed transcript by count

**Plan:** plan-133-chat-mock-runtime (PR #424, merged as a139ec62)
**Signal:** code-review Q-Gate (6-finalize)
**Class:** a client that follows a chat transcript assumes the whole transcript arrives at once and in one page. When the runtime plays a reply over time, or the transcript is paged, the client tracks progress by a count of rendered turns and misses or blanks content.

## Records of the class

- 77e333 (ChatDialogue/StartApi.java:131-139): the operator start chat printed an empty role line and never the reply or its proposal when the runtime played the reply over time (the new non-instant mock timing exposed it).
- b76df0 (ChatDialogue.java:147-152): the start chat dialogue tracked rendered turns by count into a paged transcript, so replies past the first page were never printed. Re-observed once (reobserved_count 1) before it was fixed.

## Observation

Both defects came into view only because this plan added realistic timing (streamed replies) and paging to the mock runtime; the operator client had been written against an instant, single-page mock. They were caught by the in-run code review, two rounds apart.

## Candidate corrective action

Track transcript progress by turn identity (turn_id) and turn status, not by count, and test chat clients against a mock in streaming timing and with a transcript longer than one page.
