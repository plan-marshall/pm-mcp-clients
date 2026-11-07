envelope_version=1
sender_type=plan
sender_id=plan-97-cli-command-tree-drain-and-accept-edit
epic=ui-improvements
kind=candidate-lesson
created=2026-11-07T14:47:45Z

## Candidate lesson: pre_merge_gate ignores refire_waiver, so every rebase forces loop-back close of all head-dependent steps

- **Source**: PLAN-97 (plan-marshall plan plan-97-cli-command-tree-drain-and-accept-edit), PR #358, phase 6 finalize.
- **Suggested component**: `plan-marshall:phase-6-finalize` (pre-merge gate) / `plan-marshall:manage-status` (merge authorization).
- **Suggested category**: bug.

### What happened

- `pre_merge_gate` requires `head_at_completion == HEAD` for `simplify` and `functional-review`, and does not consult a recorded `refire_waiver`.
- A rebase onto a moved `main` (no content change of the plan's own diff) therefore invalidated both steps. The only way through was `loop-back close` for every head-dependent step and re-running them on the new head.

### Rule worth considering

- The gate should honour a recorded `refire_waiver` (or compare the plan's own diff/patch-id instead of the raw head SHA), so a pure rebase does not force a full re-run of head-dependent review steps.
- Until fixed: expect a rebase late in finalize to cost a re-run of simplify and functional-review; rebase before those steps run when `main` is known to move.
