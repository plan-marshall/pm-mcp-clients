envelope_version=1
sender_type=plan
sender_id=web-brand-mark-and-waiting-mark
epic=ui-improvements
kind=candidate-lesson
created=2026-11-11T17:30:37Z

Source: 4-plan Q-Gate, module_mapping_validator (findings 6e19b1, cdca64, fb71c9, 399fae, de5300), all accepted.

Observation: in pm-web-app the architecture inventory labels test files (*.test.ts) with module pm-web-app-maven and the implementation files beside them (*.svelte, *.css) with pm-web-app-npm, and maps pm-web-app/static/ to no module at all (which-module returns null). Test and implementation sit in the same directory and run through the one npm build, so every web deliverable with a module_testing task tripped the validator (5 of 5) and needed a manual accept.

Possible corrective: fix the pm-web-app module mapping in the architecture inventory (one module for src/ and static/, or map static/ explicitly), or add doc/developer/test-impl-mapping.adoc with a row pairing pm-web-app-maven tests to pm-web-app-npm implementation.
