envelope_version=1
sender_type=plan
sender_id=plan-147-web-foundation-kind-links
epic=ui-improvements
kind=candidate-lesson
created=2026-11-12T18:55:28Z

id=2026-11-12-18-006
component=plan-marshall:build-server-client
category=bug
status=active
created=2026-11-12

# build-server wait writes the same running-result line hundreds of times within one second into the work log

## What happened

Plan plan-147-web-foundation-kind-links. The work log grew to 6542 entries (about 1.1 MB as read through `manage-logging read --type work`). Most of them are one line repeated: during a single long-poll of a maven build, `build-server wait result: job_id=... job_status=running elapsed=N eta=E mechanism=daemon_longpoll` is written many dozens to hundreds of times with the same timestamp second, the same hash id and the same elapsed value. Examples:

- job 34e62d93..., 2026-11-12T12:15:22Z, elapsed=105: work-log lines 3633 to well past 3749, all identical
- job fcce7d1f..., 2026-11-12T10:44:25Z, elapsed=174
- job c470c021..., 2026-11-12T17:32:08Z, elapsed=299 and 300

npm builds (short) logged one line each; the spam shows up on the long maven waits.

## Why it matters

Identical lines carry no information, hide the real step and error lines, and make every later reader of the work log (lessons-capture, retrospective, signal-scan) page through megabytes. Root cause not established here: either the wait loop re-logs a cached result without sleeping, or the logging call sits inside a retry loop that spins.

## Rule

A wait/poll loop logs a state change (or at most one line per poll interval), never one line per inner iteration.

## Suggested fix in the component

Find where `build-server wait result` is emitted in build-server-client and log only when `job_status` or the elapsed bucket changes; also check whether the inner loop spins without waiting, which would be a CPU and daemon-load issue as well as a log issue.
