envelope_version=1
sender_type=plan
sender_id=plan-131-web-mascot-figure-surfaces
epic=ui-improvements
kind=candidate-lesson
created=2026-11-13T09:18:27Z

component=plan-marshall:phase-6-finalize
category=improvement

# A rebase onto a moved origin/main invalidates every head-dependent finalize gate and costs a full re-gating cycle

When sibling plan PRs land on main while a plan is in finalize, the branch is rebased onto the moved `origin/main` and every head-dependent gate (code-review, functional-review, pre-push quality gate, CI, Sonar, automatic-review) is invalidated and re-runs. In plan-131-web-mascot-figure-surfaces this cost one full re-gating cycle (about 1.5 hours) because sibling PLAN-128 (PR 425) landed on main twice on this plan's base while the plan was finalizing.

## Solution

Options for the orchestrator: serialize landings of plans that touch overlapping bases, or schedule the sync-baseline rebase as late as possible (the queue-side rebase is cheaper than a pre-queue rebase that re-triggers all reviews), or make the head-dependent gates reusable across a rebase that replays no content change in the plan's own paths.

## Impact

Plans of one epic that finalize concurrently pay the re-gating cost each time a sibling lands first.
