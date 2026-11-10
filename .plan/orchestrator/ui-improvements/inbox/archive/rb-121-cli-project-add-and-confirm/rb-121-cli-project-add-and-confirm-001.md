envelope_version=1
sender_type=plan
sender_id=rb-121-cli-project-add-and-confirm
epic=ui-improvements
kind=landing
created=2026-11-09T21:54:26Z

```landing-facts
schema=landing-facts/1
plan_id=rb-121-cli-project-add-and-confirm
epic=ui-improvements
pr=#390
merge_state=merged
cleanup_owed=false
deliverables_total=2
deliverables_done=2
total_tokens=unknown
harness=antigravity
model=unknown
effort=unknown
plugin_version=0.1.0
executor_version=0.1.2090
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
unresolved_review_threads=0
review_body_findings_total=8
review_body_findings_handled=8
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
step.sonar-roundtrip.quality_gate_status=ok
```

# Landing Report: PLAN-121 Terminal: `project add` Onboards, Release and Unenrol Confirm

## Overview
- **Plan ID**: `rb-121-cli-project-add-and-confirm`
- **Epic**: `ui-improvements` (WS-04)
- **PR**: #390 (Merged as commit `06d155c325aca6a31f0624bec75e39ac643ea087`)
- **Status**: Completed, verified, and landed

## Deliverables Summary
1. **D1: `project add` runs project onboarding dialogue**
   - Replaced direct `POST /api/v1/enrol` with project onboarding flow driven via `POST /api/v1/project-onboardings` and `StateDialogue`.
   - `--pilot` answers pilot state in `project.identity`; non-interactive mode without answers exits 3.
   - Tested test-first via `ProjectCommandTest` and transcript harness `ProjectSetupDialogueIT`.
   - Closed open item `project-add-onboarding` in `doc/design/terminal-client/17-projects.adoc` and updated `01-commands.adoc`.
2. **D2: `project release` and `project unenrol` confirm without `--yes`**
   - Both verbs prompt confirmation unless `--yes` is given.
   - `project release` prompts for release reason when `--reason` is missing.
   - Non-interactive mode without required inputs exits 3.
   - Closed open item `release-confirmation-prompt` in `doc/design/terminal-client/17-projects.adoc`.

## Quality & Review Gates
- **CI**: 20/20 checks passed across Java 25 & 26 builds, Quarkus alignment, integration tests, dependency review, and SonarCloud analysis.
- **SonarCloud**: 0 open issues, 0 security hotspots, 83.0% new-code coverage (Quality Gate passed).
- **Reviews**: CodeRabbit reviewed commit `115e374e`. All 8 review threads were replied to and resolved with valid architectural dispositions. CodeRabbit and Sourcery were re-triggered on current heads and recorded quota limit refusals. Enqueued under Standing Review Authorization and successfully merged into main.
