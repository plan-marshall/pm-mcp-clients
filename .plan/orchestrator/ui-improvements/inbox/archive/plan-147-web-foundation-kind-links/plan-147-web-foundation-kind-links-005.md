envelope_version=1
sender_type=plan
sender_id=plan-147-web-foundation-kind-links
epic=ui-improvements
kind=candidate-lesson
created=2026-11-12T18:55:27Z

id=2026-11-12-18-005
component=plan-marshall:phase-6-finalize
category=improvement
status=active
created=2026-11-12

# finalize-step-simplify ran after both reviews had closed at the head, where any edit it makes voids both reviews

## What happened

Plan plan-147-web-foundation-kind-links. In most finalize iterations `finalize-step-simplify` ran before the two reviews. In the iteration after the code-review correction (ExternalLink `https:host` fix) the order was: code-review full round closed 14:00, functional-review delta closed 15:06, then `finalize-step-simplify` ran at 15:08 on eb67093b and produced an edit to ExternalLink.svelte (drop the now-redundant WEB_SCHEMES set and the try/catch, behaviour unchanged). The dispatcher discarded it (decision log 15:08:16Z) because it would change a security-relevant guard after both reviews had closed at that head and force both back through a full round for a cosmetic gain.

## Why it matters

Once both reviews have closed at a head, any source edit simplify makes voids both reviews and re-fires the pre-push gate. Running simplify there spends an agent envelope on output that can only be thrown away or cost a full review cycle.

## Rule

Simplify must run before the reviews of the same head, never after both have closed. If the reviews have already closed at the current head, skip simplify (or run it in report-only mode and record the suggestions as follow-ups).

## Suggested fix in the component

Make the finalize step order on loop-back re-entry put simplify ahead of the reviews, or give simplify a precondition "no review closed at HEAD" that records `skipped` with that reason.
