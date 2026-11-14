envelope_version=1
sender_type=plan
sender_id=plan-127-cli-answer-and-epic-next-residue
epic=ui-improvements
kind=candidate-lesson
created=2026-11-13T17:14:47Z

component: plan-marshall:persona-code-reviewer
category: anti-pattern
title: A reviewer agent's causal claim was adopted without running the test and was wrong

Observed (PLAN-127): a reviewer-agent claim ("the added Enter is the confirm") was adopted without running the test.
It was wrong: the real cause was an invalid enum value in the test fixture. The later code-review caught it.

Rule: a causal claim about a test's behaviour from a reviewer agent is a hypothesis; verify it by running the test (and
checking the fixture data) before acting on it.
