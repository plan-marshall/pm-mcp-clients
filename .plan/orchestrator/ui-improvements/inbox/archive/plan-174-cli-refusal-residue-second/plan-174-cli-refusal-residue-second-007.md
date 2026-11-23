envelope_version=1
sender_type=plan
sender_id=plan-174-cli-refusal-residue-second
epic=ui-improvements
kind=candidate-lesson
created=2026-11-23T08:19:43Z

# A source edit during a running whole-tree verify makes that build's evidence ambiguous

**Source signal:** re-run reviews / verify in 6-finalize (plan-174-cli-refusal-residue-second).

**Observation:** A simplify edit was committed while a whole-tree `./mvnw verify` was still running. Afterwards it
was unclear which head the build had actually verified, and the reviews written against the old head had to be
re-run against the new one.

**Corrective rule (proposed):** while a build whose result will serve as evidence is running, no step may change
source in the tree it builds. Make the edit after the build or in a separate tree, and bind the build evidence to the
head SHA it started on, so a later edit makes the evidence visibly stale instead of ambiguous.

**Reusability:** any finalize run in which simplify/review fixes overlap with a long verify.
