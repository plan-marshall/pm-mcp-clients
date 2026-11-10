envelope_version=1
sender_type=plan
sender_id=web-reports-fourth-residue
epic=ui-improvements
kind=candidate-lesson
created=2026-11-10T06:48:22Z

component=pm-operator
category=improvement

# Candidate lesson: Sonar new-code complexity is only visible on the PR head — check it before calling code review clean

- **Source**: plan web-reports-fourth-residue, PR #394. PR comment c4afd1 (sonarqubecloud, quality gate passed, 1 new issue: `java:S3776` in `ReportsCommand`, Sonar key `AaHCTE_EehKvqN-jZ5Oh`), remediated by TASK-21 (commit c9a666a1); re-check 839840 reports 0 new issues at head 32539c76. Code review had already declared itself clean at head 46f85869 before the Sonar issue appeared.

## What happened

The pre-push quality gate was green (2893 tests) and the code-review step reported "code review clean: 9 deliverables, 3 advisory". The Sonar analysis of the first PR head then reported one new-code issue: `java:S3776` (cognitive complexity) in `ReportsCommand`. The fix was a mechanical extraction (TASK-21), but it arrived after a clean review verdict and forced a further PR head.

The local quality gate cannot see Sonar's new-code rules; that signal exists only once the PR head is analysed. So "code review clean" and "Sonar clean" are two different facts, and the first does not imply the second.

## Rule worth considering

Before declaring code review clean on a Java change, fetch the Sonar new-code analysis for the current PR head and treat any open new-code issue as part of the review round. In particular, check cognitive complexity (`java:S3776`) on newly added branching in a command handler — a long `if`/`switch` chain added to an existing method is the usual trigger, and the fix (extract a method per branch) is cheap before the head moves. Fold the Sonar result into the same review round instead of discovering it as a second head.
