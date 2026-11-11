envelope_version=1
sender_type=plan
sender_id=plan-132-chat-contract
epic=ui-improvements
kind=candidate-lesson
created=2026-11-11T18:21:09Z

# Candidate lesson: unexplained script_failure on plan-marshall:manage-architecture:architecture

**Source signal:** script_failure cluster `plan-marshall:manage-architecture:architecture` (counted by `manage-logging signal-scan`).

**What happened:** At least one `architecture` call in this plan exited non-zero. The specific `[ERROR]` line was not found: the work log is dominated by duplicated build-server poll lines (see the build-server log-flood candidate from this plan), so the cause, whether an argparse rejection or an internal failure, is unknown here.

**Suggested pickup:** Read the plan's script-execution log for `manage-architecture:architecture` entries with a non-zero exit. Classify the failure before deciding whether it is a lesson. This record is sent only so that the counted cluster is not dropped silently.
