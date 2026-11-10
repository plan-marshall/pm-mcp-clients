envelope_version=1
sender_type=plan
sender_id=web-reports-fourth-residue
epic=ui-improvements
kind=candidate-lesson
created=2026-11-10T06:48:37Z

component=plan-marshall:phase-5-execute
category=anti-pattern

# Candidate lesson: Append the change-ledger row with the commit, not in a finalize batch

- **Source**: plan web-reports-fourth-residue, `manage-status transition` failure (2026-11-09T19:20:04Z, exit 1): "8 of 9 worktree-branch commit(s) since diverging from main carry no kind=change ledger row."

## What happened

Eight of the nine commits on the plan branch had no `kind=change` row in the change ledger. The phase transition refused until each unrecorded commit was named and appended, with the refusal printing one `manage-change-ledger append` invocation per missing commit. The rows had been treated as bookkeeping to batch at the end; the transition treats them as part of the commit itself.

## Rule worth considering

Write the `kind=change` ledger row in the same step as the deliverable commit — the phase-5-execute settlement per deliverable, the commit path itself for a finalize commit — before moving on. A batch reconciliation at the end is not equivalent: the transition gate reads the ledger as the record of what each commit did, and a missing row blocks the phase with per-commit repair work. If a batch is unavoidable, run the reconciliation before the transition, not after it fails.
