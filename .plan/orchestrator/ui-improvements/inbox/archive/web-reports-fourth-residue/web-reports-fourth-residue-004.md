envelope_version=1
sender_type=plan
sender_id=web-reports-fourth-residue
epic=ui-improvements
kind=candidate-lesson
created=2026-11-10T06:48:25Z

component=plan-marshall:phase-3-outline
category=improvement

# Candidate lesson: A plan sourced from an old residue list must re-verify each claimed defect at HEAD

- **Source**: plan web-reports-fourth-residue, phase 3-outline Q-Gate findings f9b103 and 5f5757 (both accepted 2026-11-09 as false positives after operator review).

## What happened

Two outline deliverables asserted production defects that no longer existed at HEAD:

1. Deliverable 5 named `pm-web-app/src/routes/dev/components/+page.svelte` and `+page.test.ts` for a duplicate accessible-name defect across the two theme blocks. At HEAD the RowMenu fixture labels already carried the theme id and the test already asserted four unique names, so the defect was already fixed.
2. Deliverable 7 listed `pm-web-app/src/lib/components/Button.svelte` as a write to derive variant contrast from tokens. At HEAD every variant already used `var(--color-*)` tokens and carried no literal contrast value; only the missing contrast assertion in `Button.test.ts` was real.

Both deliverables came from a residue list written against an earlier state. Each false positive consumed a Q-Gate triage round and forced an operator decision to trim the outline.

## Rule worth considering

When a request's input is a residue list, backlog, or audit written against an earlier commit, re-verify every claimed defect at HEAD before it becomes a deliverable: open the named file and confirm the defect is present. Record the check in the deliverable ("verified absent at HEAD" is a valid outcome) and trim the write rather than planning a no-op edit. A deliverable that promises a production write must name the defect it removes and the HEAD commit on which it was observed.
