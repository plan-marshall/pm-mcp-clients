envelope_version=1
sender_type=plan
sender_id=plan-132-chat-contract
epic=ui-improvements
kind=candidate-lesson
created=2026-11-11T18:21:17Z

# Candidate lesson: ChatTurn and ChatTurnStartedEvent accepted markdown on turns the contract requires to be plain

**Source signal:** Q-Gate finding 117224 (6-finalize, code-review, error, taken into account; fixed by the round at 9b8c9033).

**What happened:** The chat contract requires operator, untrusted and notice turns to be plain text, but `ChatTurn` and `ChatTurnStartedEvent` accepted `format=markdown` on them. This matters for safety: the web standards forbid rendering untrusted or model-written text through an HTML sink, and the format flag is what tells the client how to render. The compact constructor checks were added after the review. A follow-up advisory remains: `ChatTurn` still accepts `model` and `steps` on the operator turn, which `ChatTurnStartedEvent` refuses (ChatTurn.java:117).

**Possible corrective rule:** When a record mirrors a constraint that the specification states per variant (per turn kind), check every variant-dependent field in the compact constructor. Test each field across the full grid of variants with a parameterized test, and keep the event and the record checks identical. Otherwise the two drift apart, as `model`/`steps` did here.

**Evidence:** finding 117224; advisories 809f17, a7e73e, 632dfa.
