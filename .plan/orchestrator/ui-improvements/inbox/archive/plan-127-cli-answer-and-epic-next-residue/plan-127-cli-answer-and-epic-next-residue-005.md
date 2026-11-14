envelope_version=1
sender_type=plan
sender_id=plan-127-cli-answer-and-epic-next-residue
epic=ui-improvements
kind=candidate-lesson
created=2026-11-13T17:14:34Z

component: plan-marshall:tools-integration-ci
category: bug
title: CI verdict green with the real checks absent when GitHub starts no workflows on a conflicting head

Observed (PLAN-127, PR #426): after another PR landed, the PR conflicted with main and GitHub started no Maven/Sonar
workflows for the new head. `ci_complete_precondition resolve` and `ci_verify run` still reported success from the two
bot checks that did exist (CodeRabbit, Sourcery): a green CI verdict with the required checks missing. Only the sonar
fetch (stale_analysis) caught it.

Corrective action needed: the CI verdict must require the configured required checks to be present for the head (and
check mergeable state / conflict), and report "required checks absent" rather than success when only bot checks exist.
