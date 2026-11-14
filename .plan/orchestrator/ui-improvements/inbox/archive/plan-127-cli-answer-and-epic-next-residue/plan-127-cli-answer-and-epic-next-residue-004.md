envelope_version=1
sender_type=plan
sender_id=plan-127-cli-answer-and-epic-next-residue
epic=ui-improvements
kind=candidate-lesson
created=2026-11-13T17:14:31Z

component: plan-marshall:build-maven
category: anti-pattern
title: Unwrapped ./mvnw runs do not stamp the change ledger; pre-commit-verify-freshness stays stale

Observed (PLAN-127): verify runs made with a bare `./mvnw ...` did not stamp the change ledger, so
`pre-commit-verify-freshness` kept answering stale / worktree_mutated. It only turned fresh once the same verify ran
through `build-maven:maven run` (build server, long-poll with `build_server wait`).

Rule: inside a plan, every build/verify whose result a gate reads must run through the build-maven wrapper, never as a
bare ./mvnw; the freshness gate's refusal message could name this cause directly.
