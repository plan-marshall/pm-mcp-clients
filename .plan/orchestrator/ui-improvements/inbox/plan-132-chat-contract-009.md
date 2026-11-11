envelope_version=1
sender_type=plan
sender_id=plan-132-chat-contract
epic=ui-improvements
kind=candidate-lesson
created=2026-11-11T18:21:11Z

# Candidate lesson: unexplained script_failure on plan-marshall:manage-findings:manage-findings

**Source signal:** script_failure cluster `plan-marshall:manage-findings:manage-findings` (counted by `manage-logging signal-scan`).

**What happened:** At least one `manage-findings` call in this plan exited non-zero. The specific `[ERROR]` line was not found because the work log is flooded with duplicated build-server poll lines, so the cause is unknown here.

**Suggested pickup:** Read the plan's script-execution log for `manage-findings` entries with a non-zero exit and classify them, for example as an argparse rejection or an internal failure. This record is sent only so that the counted cluster is not dropped silently.
