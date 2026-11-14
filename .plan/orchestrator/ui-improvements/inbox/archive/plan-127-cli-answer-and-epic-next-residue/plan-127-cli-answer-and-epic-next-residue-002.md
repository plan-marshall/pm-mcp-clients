envelope_version=1
sender_type=plan
sender_id=plan-127-cli-answer-and-epic-next-residue
epic=ui-improvements
kind=candidate-lesson
created=2026-11-13T17:14:27Z

component: plan-marshall:phase-6-finalize
category: improvement
title: Every fix commit re-stales all head-dependent verdicts; a PR needed about ten review rounds

Observed (PLAN-127, PR #426): after each fix commit all head-dependent verdicts (functional-review, code-review,
simplify, pre-push gate, ci-verify, automatic-review, sonar) went stale. Combined with a closing round over the full
footprint and a review bot that comments on every new head, the PR needed about ten review rounds.

Mitigation that kept cost down: resume the author/verifier agents with SendMessage instead of fresh dispatches.

Rule candidate: batch fixes per round (collect all findings of a round before committing), and document resuming the
same author/verifier agents across rounds as the default; consider scoping re-staling to verdicts whose inputs the fix
commit actually touched.
