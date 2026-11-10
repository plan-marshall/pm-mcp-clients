envelope_version=1
sender_type=plan
sender_id=web-reports-fourth-residue
epic=ui-improvements
kind=candidate-lesson
created=2026-11-10T06:48:18Z

component=pm-web-app
category=bug

# Candidate lesson: An async action button owes focus retention and an in-flight cue, asserted in tests

- **Source**: plan web-reports-fourth-residue, PR #394. Q-Gate findings 9d0b77 (code-review round 1: "Mark-all button drops keyboard focus while the request runs") and ca222c (round 3: "Mark-all button shows no in-flight loading cue"), both at `pm-web-app/src/lib/views/ReportsView.svelte:898`.

## What happened

The mark-all button used `aria-disabled` rather than `disabled` so it could stay focusable while the request ran. That choice was right but incomplete:

- Focus was dropped while the pending request ran, so a keyboard user lost their place — found in round 1.
- No in-flight cue was rendered, so the button looked inert while the request was in progress — found two rounds later in round 3.

Neither defect was caught by the unit tests that shipped with the change; both surfaced only in code review, one round apart. The pattern repeated the classic split: the accessibility attribute was handled, the two behaviours it implies (focus stays, busy state shows) were not.

## Rule worth considering

For any button that disables itself around an async request, the deliverable is three facts, not one: the control stays focusable (aria-disabled, not removed/disabled), focus is not moved away while the request runs, and a visible in-flight cue appears until it settles. Assert all three in a component test — a focus assertion and a busy-state assertion — because `aria-disabled` alone passes review-by-inspection while failing the keyboard user. Choose one binding per async action and test focus and busy state together; splitting them across rounds costs a review pass each.
