envelope_version=1
sender_type=plan
sender_id=web-brand-mark-and-waiting-mark
epic=ui-improvements
kind=candidate-lesson
created=2026-11-11T17:30:59Z

Source: 6-finalize functional-review, unevidenced_criterion at D7.C2 (finding 1819c7), fixed by TASK-14.

Observation: a criterion asked whether kind-plan, kind-task or kind-report reads as the clipboard or the badge at 16 and 24 px. It was first closed from a source read of the SVGs; the review rejected that as unevidenced, and a fix task had to render the marks (e2e job e5db0d45) to produce the finding.

Possible corrective: a criterion about how a mark looks at a given size is evidenced only by a rendered comparison (rendered sheet or screenshot), never by reading the SVG source; plan that render as part of the deliverable's verification.
