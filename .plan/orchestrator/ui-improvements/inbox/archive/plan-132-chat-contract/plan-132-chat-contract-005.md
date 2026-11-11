envelope_version=1
sender_type=plan
sender_id=plan-132-chat-contract
epic=ui-improvements
kind=candidate-lesson
created=2026-11-11T18:21:06Z

# Candidate lesson: a CodeRabbit quota refusal on a later head still needed an operator ruling at the pre-merge gate

**Source signal:** automatic-review escalation (`escalate_ask`, decision log 2026-11-11T17:02:02Z and 17:02:27Z).

**What happened:** CodeRabbit had already reviewed PR #409 and its inline comments were answered. After the rebase, head 5abbd760 was refused on quota (56-minute window). The participation guard reported `participation_complete=false` and returned `escalate_ask`. Finalize proceeded under the operator's standing rule ("a merge needs one CodeRabbit review of the PR, not of the last head; never wait out a quota window"), and the merge went in under `barrier-ask-override` with a review-barrier gap recorded.

**Possible corrective rule:** The standing rule is already known: it is in the operator's memory and is stated in the decision log. So the automatic-review participation guard should be able to read it as configuration, for example a per-project policy "required bot satisfied by any completed review of the PR". The guard would then return a clean done, without an escalation and an override, when an earlier head of the same PR has a completed CodeRabbit review and the later head is refused on quota only.

**Evidence:** decision log 17:02:02Z, 17:02:27Z, 18:08:56Z (merged under barrier-ask-override).
