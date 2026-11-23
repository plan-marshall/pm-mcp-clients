envelope_version=1
sender_type=plan
sender_id=plan-174-cli-refusal-residue-second
epic=ui-improvements
kind=candidate-lesson
created=2026-11-23T08:19:00Z

# Generated test inputs must be disjoint by construction from served/registered names

**Source signal:** CI test failure on PR #533 (Java 27 job), passed locally.

**Observation:** A test built an "unknown name" input as `"landin"` plus 1-4 generated letters. For roughly 1 in
100 seeds the result equalled a name the mock actually serves (e.g. `landing`), so the "unknown" case became a
known one. It passed locally and failed on CI because the generator's seed draws differ by JDK (Java 27 on CI).

**Corrective rule (proposed):** a generated input that must NOT match a served/registered value has to be disjoint
by construction (e.g. a prefix/character class no served name can carry, or an explicit reject-and-redraw against
the served set), never "unlikely to collide". A green local run with `cui-test-generator` proves nothing about other
seeds or JDKs.

**Reusability:** any test using `Generators` to build a negative/unknown-key input against a fixed catalogue.
