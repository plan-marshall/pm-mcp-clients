envelope_version=1
sender_type=plan
sender_id=plan-132-chat-contract
epic=ui-improvements
kind=candidate-lesson
created=2026-11-11T18:21:08Z

# Candidate lesson: the build-server wait poll writes hundreds of identical lines to the work log

**Source signal:** found while reading the script-failure records for this plan (supports signal_script_failure_clusters).

**What happened:** The plan's work log holds 4775 entries (about 785 KB in a read). Most of them are identical `build-server wait result: job_id=... job_status=running elapsed=299 eta=... mechanism=daemon_longpoll` lines with the same timestamp and hash, repeated dozens to hundreds of times for each poll. For example, job a15f5c98... wrote more than 200 identical lines at 14:27:08Z. The flood buried the failure records: this lessons-capture run could not locate the `[ERROR]` lines for two of the four failing notations within a reasonable read budget.

**Possible corrective rule:** Write the build-server long-poll result to the work log once per state change (queued, then running, then terminal), or at most once per poll, not once per retry inside the loop. Check whether a retry or iteration loop in the build-server client or the build-wrapper logs inside an inner loop that does not advance.

**Evidence:** work log, e.g. hash c068ba (more than 200 copies at 2026-11-11T14:27:08Z), a4531d at 16:20:02Z, e07456 and e77e13 at 16:33:12Z.
