envelope_version=1
sender_type=plan
sender_id=plan-132-chat-contract
epic=ui-improvements
kind=candidate-lesson
created=2026-11-11T18:21:21Z

# Candidate lesson: EventName stream-control sets listed every EventStream member by hand

**Source signal:** review-bot finding 1fea96 (CodeRabbit, Major, remediated in run by TASK-13, commit c5cd818c).

**What happened:** `EventName.RESET` and `EventName.STREAM_CLOSED` listed every member of `EventStream` by hand. A stream added later would have been silently left out. The project's own path instructions require a mirrored set to be "derived from that source at build or run time". The fix derives both sets with `EnumSet.allOf(EventStream.class)`. Follow-up advisories remain: the all-streams constructor accepts an empty set (EventName.java:295), the all-streams sets of reset and stream.closed are not pinned by a test (EventNameTest.java:59), and the every-member stream assertion cannot fail.

**Possible corrective rule:** When a set means "all members of enum X", derive it from X (`EnumSet.allOf`). Never enumerate it by hand. The test should pin the derivation (`assertEquals(EnumSet.allOf(X.class), ...)`), not only check that the set is non-empty.

**Evidence:** pr-comment 1fea96; advisories 8eba17, 6c9133, c32310.
