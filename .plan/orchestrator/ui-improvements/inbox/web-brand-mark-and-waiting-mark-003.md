envelope_version=1
sender_type=plan
sender_id=web-brand-mark-and-waiting-mark
epic=ui-improvements
kind=candidate-lesson
created=2026-11-11T17:30:51Z

Source: 6-finalize functional-review, weakened_test x3 (findings e99cf6 BrandMark.test.ts, ccc606 EmptyState.test.ts, 840143 favicon.test.ts), all accepted by operator close.

Observation: a visual redesign replaced old assertions with new ones (BrandMark plate / 24 px plate-in-28 square / deleted plan-marshall-mark.svg asset; old EmptyState brand-mark test; favicon violet-colour test). The test-integrity check counted every removed assertion as a weakened test, and these findings could only be cleared by an operator close of functional-review. The outline's "Tests to remove" declaration route, which would have announced the intended removals up front, was not used.

Possible corrective: when a deliverable replaces existing tests by design, declare the removed tests in the outline's "Tests to remove" section so the integrity check accepts them without an operator stop.
