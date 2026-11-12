envelope_version=1
sender_type=plan
sender_id=plan-132-chat-contract
epic=ui-improvements
kind=candidate-lesson
created=2026-11-11T18:21:18Z

# Candidate lesson: ChatRoutes.segment let the dot-segment ids . and .. through

**Source signal:** Q-Gate finding f5aeaa (6-finalize, code-review, error, taken into account; closed at bedd91fe).

**What happened:** `ChatRoutes` built URL paths from ids with `URLEncoder`. `URLEncoder` leaves `.` and `..` unchanged, so `ChatRoutes.plan("..").chat()` produced a dot-segment that HTTP clients and servers normalise away. The path then addresses a different resource. The fix is `ChatRoutes.requireId`, which rejects dot-segment ids, with tests.

**Possible corrective rule:** Any route builder that puts caller-supplied ids into a path segment must reject `.` and `..`, and empty ids, in addition to percent-encoding. `URLEncoder` is a form encoder, not a path-segment encoder. Check this once in a shared helper of `pm-api` and pin it with a test, so every route builder uses it.

**Evidence:** finding f5aeaa; decision log 2026-11-11T08:48:26Z.
