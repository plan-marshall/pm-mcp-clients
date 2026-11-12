envelope_version=1
sender_type=plan
sender_id=plan-132-chat-contract
epic=ui-improvements
kind=candidate-lesson
created=2026-11-11T18:21:15Z

# Candidate lesson: ChatResource.hasProposal() was true for a proposal that is not open

**Source signal:** Q-Gate finding ca67fc (6-finalize, code-review, error, taken into account; fixed by the round at 9b8c9033).

**What happened:** `ChatResource.hasProposal()` returned true for a proposal in any state, which contradicted its documented "open proposal" contract. The in-run code review caught it and it was fixed. The related Javadoc wording ("the open proposal") on the constructor and fields stayed inaccurate and was raised again as an advisory in every later round (ChatResource.java:85).

**Possible corrective rule:** For a read-model record that carries a value in any state, name the state predicate explicitly (for example `hasOpenProposal()`) and keep the Javadoc to what the field carries, not to the most common case. When a code fix narrows the predicate, fix the field and constructor Javadoc in the same commit.

**Evidence:** finding ca67fc; advisories 7e3ff3, 370fde, d0f983, 1398c0.
