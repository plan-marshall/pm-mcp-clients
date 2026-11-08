envelope_version=1
sender_type=plan
sender_id=rb-21-cli-answer-dialogue
epic=ui-improvements
kind=landing
created=2026-11-08T17:22:16Z

envelope_version=1
sender_type=plan
sender_id=rb-21-cli-answer-dialogue
epic=ui-improvements
kind=landing
created=2026-11-08T17:22:00Z

```landing-facts
schema=landing-facts/1
plan_id=rb-21-cli-answer-dialogue
epic=ui-improvements
pr=#372
merge_state=merged
cleanup_owed=false
deliverables_total=3
deliverables_done=3
total_tokens=unknown
harness=antigravity
model=gemini-2.5-pro
effort=high
plugin_version=unknown
executor_version=unknown
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
step.sonar-roundtrip.open_issue_count=0
step.sonar-roundtrip.open_issue_rules=none
```
guard: none on this harness (unenforced)

# PLAN-21 cli-answer-dialogue — landing

PR #372 (merge commit `bedc5927dce1b7d79ad1397f2fc38559932b2972`). Merged through the merge queue with green CI, zero unresolved review threads, and zero Sonar open new issues at head.

## Deliverables

| # | Deliverable | Verdict |
|---|---|---|
| D1 | Terminal ownership and errors | **met** |
| D2 | Content from the envelope | **met** |
| D3 | Direct answer | **met** |

### D1: Terminal ownership and errors
- **Failing check**: Unit test `PmConsoleHandoffTest.consoleAnswersTwiceInOneSession` failed initially as expected (`test-first`).
- **Passing check**: After modifying `PmConsole.java` to borrow the scoped terminal via `context.useTerminal` / `clearTerminal`, `PmConsoleHandoffTest` passed cleanly.
- **Terminal check**: Before & after captures recorded in `.plan/temp/terminal/PLAN-21/before/` and `.plan/temp/terminal/PLAN-21/after/`.

### D2: Content from the envelope
- **Failing check**: Tests in `AnswerDialogueIT` failed before implementation (`test-first`).
- **Passing check**: With 26 `operator.decision.*` catalogue keys in `decisions.en.properties` and `decisions.de.properties`, options, votes, dissent, recommendation, and evidence hints implemented in `AnswerCommand.java`, `AnswerDialogueIT` passed.
- **Terminal check**: Screen captures recorded across en/de for options, quorum support, dissent, and waiver answering.

### D3: Direct answer
- **Failing check**: Direct answering `answer Q-007 2` in `AnswerDialogueIT.directAnswerSendsWithoutDialogue` failed before implementation (`test-first`).
- **Passing check**: Direct answer option handling added to `AnswerCommand.java` and verified in `AnswerDialogueIT` with `/mock/acts` verification.
- **Browser check**: Regression check verified via Playwright MCP on `fully-onboarded` and `running-work` scenarios across English and German, dark and light themes, at 1440x900 and 390x844 viewports.
- **Sonar check**: Confirmed 0 open issues on PR #372 with 80.4% new code coverage on SonarCloud.
