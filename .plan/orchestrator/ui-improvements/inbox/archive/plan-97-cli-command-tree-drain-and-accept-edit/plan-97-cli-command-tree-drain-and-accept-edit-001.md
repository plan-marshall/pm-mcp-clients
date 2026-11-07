envelope_version=1
sender_type=plan
sender_id=plan-97-cli-command-tree-drain-and-accept-edit
epic=ui-improvements
kind=candidate-lesson
created=2026-11-07T14:47:37Z

## Candidate lesson: CodeRabbit free-tier quota refusals are worsened by re-requests and by a cross-plan rate-window claim

- **Source**: PLAN-97 (plan-97-cli-command-tree-drain-and-accept-edit), PR #358, automatic-review step.
- **Suggested component**: `plan-marshall:automatic-review` (re-request behaviour) and `plan-marshall:manage-locks` (the cross-plan review-bot rate-window claim).
- **Suggested category**: improvement.

### What happened

- CodeRabbit (free OSS quota) refused reviews several times; the reset time it stated moved between refusals.
- A re-request from the same account during a refusal pushed the stated reset further back instead of queueing a review.
- A shared cross-plan rate-window claim held by another plan blocked this plan's re-review for about 75 minutes, although only one review was needed.

### Rule worth considering

- Do not re-request a review while the bot reports a quota refusal; read the stated reset once and act after it. Each re-request on the same account can extend the window.
- The rate-window claim should be scoped to the actual bot window (and released when the claiming plan's review arrives), so one plan's claim does not hold other plans for longer than the bot itself would.
- Related operator rules already exist (one CodeRabbit review per PR is enough; re-trigger once, never wait); this candidate adds the re-request-extends-reset fact and the claim-scope defect.
