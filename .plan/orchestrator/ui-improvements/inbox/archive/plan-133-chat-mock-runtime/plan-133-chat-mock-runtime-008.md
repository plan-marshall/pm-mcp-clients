envelope_version=1
sender_type=plan
sender_id=plan-133-chat-mock-runtime
epic=ui-improvements
kind=candidate-lesson
created=2026-11-13T12:25:37Z

# Candidate lesson: fixture and input validators that filter or coerce instead of refusing

**Plan:** plan-133-chat-mock-runtime (PR #424, merged as a139ec62)
**Signal:** automated review (CodeRabbit) and code-review Q-Gate
**Class:** a validator of a scenario fixture or an injected request silently drops, coerces or overwrites malformed input instead of refusing it, so a defective fixture loads and the defect shows up later (or never). The fixes came one shape at a time across several rounds.

## Records of the class

- 78b17f (CodeRabbit, StartRegistry.java:426-427): `endedWorkOf` filtered out ended-work items without `subject_id` or with an unknown `subject_kind`; TASK-43 made them fail the load.
- 48c5a4 (CodeRabbit, next round, StartRegistry.java:429): `objects()` still dropped non-object items (`ended_work: [42]`) before the TASK-43 check ran; TASK-45 refused them.
- 8b3a52 (CodeRabbit, StateDefinition.java:217): a present non-string `code` read as null and was accepted; TASK-29 refused it at load and inject.
- c9482f (code-review Q-Gate, StateDefinition.java:215): `ChatFailure.fromMap` cast a long status to int before validating, so 4294967696 wrapped to 400.
- 650740 (CodeRabbit, StartRegistry.java:662): `openFollowUp` skipped the seeded-project check `open()` applies; TASK-32.
- 9d6630 (CodeRabbit, ScenarioLoader.java:504): the key traversal treated any `args` member named `key` as a message key and refused valid fixtures (the opposite error: over-broad validation); TASK-29.
- 775633 (code-review Q-Gate, ChatRegistry.java:276-285): seeding silently kept only the last chat of a subject, and `seedEndedWork` collapsed an ended plan and epic with the same id.
- 36308a (CodeRabbit, fully-onboarded.json:2329): seeded `chat.step.read` steps lacked the `target` argument their text renders, and the key test did not check required args; TASK-36.

## Observation

The validators were written as "read what is there" helpers (`JsonTree.string` returns null for wrong types, `objects()` skips non-objects), so every malformed shape needed its own explicit refusal, found by review one at a time.

## Candidate corrective action

A fixture validator checks the raw member's presence and type before using a lenient accessor, refuses every element it would otherwise skip, validates numeric range before narrowing casts, and refuses duplicates instead of overwriting. Its test enumerates the malformed shapes (missing, wrong type, wrong element type, out of range, duplicate).
