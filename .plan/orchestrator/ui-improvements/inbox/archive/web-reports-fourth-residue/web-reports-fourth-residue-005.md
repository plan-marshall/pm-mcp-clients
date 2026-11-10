envelope_version=1
sender_type=plan
sender_id=web-reports-fourth-residue
epic=ui-improvements
kind=candidate-lesson
created=2026-11-10T06:48:28Z

component=plan-marshall:phase-6-finalize
category=improvement

# Candidate lesson: A behaviour-rewriting deliverable should name the test assertions it replaces

- **Source**: plan web-reports-fourth-residue, phase 6-finalize Q-Gate findings 62e1b4, 61db0f, 25afbd (severity error, later accepted at functional review 2026-11-09).

## What happened

The plan rewrote the mark-all behaviour and, as part of it, replaced several older tests:

- `CompletionReportView.test.ts` — 2 assertions removed (running-maximum heading test replaced by the per-level heading test).
- `ReportsView.test.ts` — 5 assertions removed (mark-all empty state and duplicate `unreadCount` tests replaced by shown-ids and previous-level tests).
- `pm-web-e2e/tests/reports.spec.js` — 10 assertions removed (old mark-all empty-state flow replaced).

The weakened-test detector raised three error-severity findings at finalize. All three were genuine intended replacements, and all three were accepted — after an operator decision that the Q-Gate could not make for itself.

## Rule worth considering

When a deliverable deliberately replaces an existing behaviour, its task body should name the assertions being retired and the assertions that supersede them, per file. That turns the weakened-test gate's correction into a pre-answered question instead of a finalize-time blocking finding, and it gives the functional reviewer the intent alongside the diff. A removal that cannot name its replacement is a weakened test; one that can is a documented rewrite. Without the declaration, the detector is correct to flag it and the operator pays for the same judgement twice.
