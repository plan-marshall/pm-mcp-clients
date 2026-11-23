envelope_version=1
sender_type=plan
sender_id=plan-174-cli-refusal-residue-second
epic=ui-improvements
kind=candidate-lesson
created=2026-11-23T08:19:27Z

# After a branch-cleanup rebase, every head-dependent step must re-fire at the new head

**Source signal:** pre-merge gate refusal in 6-finalize (plan-174-cli-refusal-residue-second, PR #533).

**Observation:** After `branch-cleanup` rebased the branch onto a moved `main`, only simplify, the reviews and the
gate were re-run. `pre_merge_gate` then refused with `merge_candidate_not_gated`, because `ci-verify`,
`sonar-roundtrip` and `automatic-review` had evidence bound to the old head.

**Corrective rule (proposed):** a rebase invalidates EVERY step whose evidence is bound to the head SHA. Re-fire the
complete head-dependent set (ci-verify, sonar-roundtrip, automatic-review, simplify, reviews, gate) at the rebased
head; better, derive that set from the step metadata instead of remembering it.

**Reusability:** every finalize run whose branch is rebased in branch-cleanup (frequent under the merge queue).
