envelope_version=1
sender_type=plan
sender_id=plan-147-web-foundation-kind-links
epic=ui-improvements
kind=candidate-lesson
created=2026-11-12T18:55:19Z

id=2026-11-12-18-001
component=plan-marshall:plan-marshall
category=anti-pattern
status=active
created=2026-11-12

# Unified PR triage in 6-finalize drafted multi-file inline edits instead of a fix task; the commit gate refused them (no_sanction)

## What happened

Plan plan-147-web-foundation-kind-links, PR 420. After the first push the unified triage of the PR review findings (`wait-region-unified-triage`, 14 findings: a Sonar issue, Link, ExternalLink, Compose and test hardening) wrote its fixes as inline source edits across 11 files while still in 6-finalize. The commit was then refused by `git-workflow` (2026-11-12T11:28:52Z):

```
Source-tree commit refused in 6-finalize: no step id from the composed phase-6 manifest,
no open loop-back fix task and no review-rubric section-6 inline-correction record.
reason: no_sanction
```

The same work then had to be redone as fix task TASK-019 through a loop-back to 5-execute (11:50 to 12:07), which then re-ran both reviews, the pre-push gate, the push, CI, Sonar and the bot review.

## Why it is wrong

The triage workflow already has the right exit for this shape: Shape 1 (create a fix task, leave the finding not-done, STOP and loop back). The triage agent chose inline edits anyway. The commit gate is correct to refuse them; the edits were simply wasted work (about 22 minutes plus one agent envelope).

## Rule

In 6-finalize triage, any disposition that needs a source change in more than one file (or anything beyond a section-6 inline correction the rubric allows) MUST take Shape 1: write the fix task with the full list of findings and the intended change, and loop back. Do not draft the edits first. Check the commit-sanction rule BEFORE editing, not at commit time.

## Suggested fix in the component

Have the triage workflow evaluate the commit sanction up front (count the files the dispositions would touch; no composed step id and no open fix task means Shape 1), so the inline path is never entered for a change the gate will refuse.
