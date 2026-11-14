envelope_version=1
sender_type=plan
sender_id=plan-127-cli-answer-and-epic-next-residue
epic=ui-improvements
kind=candidate-lesson
created=2026-11-13T17:14:23Z

component: plan-marshall:execution-context
category: bug
title: Dispatched agents inherit a drifted orchestrator cwd and file into a stray plan store on the main checkout

Observed (PLAN-127): the orchestrator's shell cwd had drifted to the main checkout. Two dispatched agent runs then
filed their pr-comment findings into a stray plan store under the MAIN checkout
(.plan/local/plans/<plan>/artifacts/findings) instead of the worktree's store, and could not call mark-step-done
(file_not_found, plan not visible from scope main).

Corrective action used: a mandatory STORE CHECK in the dispatch prompt (`pwd`, `git rev-parse --show-toplevel`, and the
resolved store_path compared with the plan's worktree) before any store write. It prevented a repeat.

Rule candidate: a dispatched leaf must verify that its cwd / resolved plan store is the plan's worktree store before
its first store write, and refuse (return to the orchestrator) on mismatch, instead of silently creating a stray store.
