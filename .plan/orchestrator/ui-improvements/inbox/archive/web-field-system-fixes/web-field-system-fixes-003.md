envelope_version=1
sender_type=plan
sender_id=web-field-system-fixes
epic=ui-improvements
kind=candidate-lesson
created=2026-11-21T18:45:38Z

## Candidate: web input-field changes need async/IME/paste edge cases covered up front, and relayed fix instructions verified first

Component: pm-web-app (field system) / plan-marshall:phase-6-finalize (review-fix loop)

Observation: a web field-system change needed many automated-review rounds, and each round found a new real edge case in async validation, IME composition or paste handling. One round was spent because the orchestrator passed on a wrong fix instruction (whitespace on paste) without checking it against the intended behaviour.

Suggested correction: for changes to text-input or field behaviour, list the async-ordering, IME-composition (compositionstart/end) and paste (including whitespace and multi-line) cases in the outline or test plan before implementing, and cover them with tests. Before passing a reviewer-derived fix instruction to the implementer, check it against the specified behaviour.

Reusable: partially - the edge-case checklist applies to any input-component work in the web modules; the verify-before-relay point applies to every review-fix loop.
