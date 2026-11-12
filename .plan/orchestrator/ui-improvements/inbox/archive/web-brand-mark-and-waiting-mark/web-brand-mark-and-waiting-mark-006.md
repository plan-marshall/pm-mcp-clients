envelope_version=1
sender_type=plan
sender_id=web-brand-mark-and-waiting-mark
epic=ui-improvements
kind=candidate-lesson
created=2026-11-11T17:31:15Z

Source: plan-marshall:automatic-review outcome on PR #414 (signal_automated_review_count = 1).

Observation: CodeRabbit took part; triage accepted its three inline items. After the PR body was updated, CodeRabbit edited its walkthrough comment, and the edited comment was picked up again as a pending finding. That blocked the merge until the comment was triaged a second time.

Possible corrective: treat an edit of a bot's walkthrough/summary comment that was already triaged as a non-actionable update (or key it by comment id rather than content), so a PR-body update does not re-open a finished review and hold the merge.
