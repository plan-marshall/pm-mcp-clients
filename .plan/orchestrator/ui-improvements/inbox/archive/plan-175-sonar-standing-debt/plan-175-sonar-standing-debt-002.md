envelope_version=1
sender_type=plan
sender_id=plan-175-sonar-standing-debt
epic=ui-improvements
kind=candidate-lesson
created=2026-11-22T14:35:19Z

## Candidate lesson: a Done-when that needs the pull request's own Sonar read cannot be evidenced before the push

Source: Q-Gate finding 79fbc5 (phase 6-finalize, functional-review, severity error, rule `unevidenced_criterion at D1.C3`), resolution `accepted` by operator decision.

What happened: criterion D1.C3 of PLAN-175 asked that "the pull request's own Sonar read (`read_open_issues --pr N`) names no open new-code issue and no hotspot". The functional review runs before the push, so that read cannot exist when the criterion is graded. The verifier kept the round open twice (`may_close=false`, once on the full round and once on the delta round) on this one blocking finding, and the operator finally accepted D1.C3 as checked later by the post-push sonar-roundtrip step, which then had to read zero new-code issues on PR 1.

Suggested rule: when a spec's Done-when depends on a post-push observation (the PR's Sonar read, CI, a bot review), the spec author should mark it as a post-push criterion, so the functional review grades it against the sonar-roundtrip or ci-verify step rather than treating it as unevidenced before the push. Without that, every plan with such a criterion needs an operator decision at finalize.

Evidence: findings store of plan-175-sonar-standing-debt (hash 79fbc5); decision log entries 45d619 and de2b9a.
