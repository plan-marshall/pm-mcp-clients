envelope_version=1
sender_type=plan
sender_id=plan-97-cli-command-tree-drain-and-accept-edit
epic=ui-improvements
kind=candidate-lesson
created=2026-11-07T14:48:02Z

## Candidate lesson: review-fix rounds introduce new Sonar findings that only show up after the push

- **Source**: PLAN-97 (plan-97-cli-command-tree-drain-and-accept-edit), PR #358, automatic-review fix rounds.
- **Suggested component**: project-level (pm-mcp-clients Java modules); possibly `plan-marshall:automatic-review` (fix-round checklist).
- **Suggested category**: anti-pattern.

### What happened

- Code written to answer review comments raised new Sonar issues on the PR: S1192 (duplicated string literal), S2589 (condition always true/false), S1130 (declared exception never thrown).
- The local quality gate (`./mvnw verify -Ppre-commit`) does not run the Sonar rules, so each surfaced only after the push and cost another round. The project's landing rule is zero open new-code Sonar issues.

### Rule worth considering

- Before pushing a fix round, check the fix code for these three recurring rules: extract a repeated literal into a constant, drop a condition the surrounding code already decides, and remove `throws` clauses the body cannot throw.
- Read the PR's open Sonar issues (`workflow-integration-sonar read_open_issues`) after every fix-round push, not only at the end.
