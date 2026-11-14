envelope_version=1
sender_type=plan
sender_id=plan-127-cli-answer-and-epic-next-residue
epic=ui-improvements
kind=candidate-lesson
created=2026-11-13T17:14:41Z

component: pm-mcp-clients:build
category: anti-pattern
title: verify -Ppre-commit rewrites unrelated files from the base branch on a rebased tree

Observed (PLAN-127): `./mvnw verify -Ppre-commit` on a rebased tree rewrote a file that came from the base branch and
was not part of the PR (MockRuntimeCli.java). It was restored instead of committed, to keep the PR scoped.

Rule: after the pre-commit profile, commit only rewrites of files in the plan's footprint; restore rewrites of files
outside it (they belong to a separate cleanup on main).
