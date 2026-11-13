envelope_version=1
sender_type=plan
sender_id=plan-131-web-mascot-figure-surfaces
epic=ui-improvements
kind=candidate-lesson
created=2026-11-13T09:18:11Z

component=pm-mcp-clients:pm-operator
category=bug

# Flaky test on main: NeedsItemCommandsTest.everyKindNamesItsSubject

`NeedsItemCommandsTest.everyKindNamesItsSubject` in pm-operator is flaky on main: a generated title can clean to blank, so the "every kind names its subject" assertion fails for that draw. It failed the first CI run of PR 422 (plan-131-web-mascot-figure-surfaces, unrelated to the plan's changes) and passed on re-run.

## Solution

Constrain the generator for the title input so its cleaned form is never blank (or assert on the cleaning contract separately). Do not rely on a CI re-run.

## Impact

Any PR in pm-mcp-clients can fail its first CI run on this test and cost a re-run cycle, which is long when the merge queue is involved.
