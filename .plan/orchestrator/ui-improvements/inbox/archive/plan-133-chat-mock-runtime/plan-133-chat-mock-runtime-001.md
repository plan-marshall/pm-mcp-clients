envelope_version=1
sender_type=plan
sender_id=plan-133-chat-mock-runtime
epic=ui-improvements
kind=candidate-lesson
created=2026-11-13T12:24:22Z

# Candidate lesson: hand-kept copies of a set defined elsewhere

**Plan:** plan-133-chat-mock-runtime (PR #424, merged as a139ec62)
**Signal:** automated review (CodeRabbit), plus one code-review Q-Gate finding
**Class:** a list kept by hand in code or tests mirrors a set that another source already defines (a catalogue, an enum, a dispatch table, a scenario index). The copies drift apart, and tests named "every X" pass while missing members.

## Records of the class

- c73866 (CodeRabbit, ChatMessageKeys.java:78): the knows/applies/step/reason/finding key sets copy the design catalogue; fixed in TASK-30 with a check of each set against the catalogue in both directions.
- 9afdde (CodeRabbit, ChatMessageKeysTest.java:123): the `@ValueSource` knows-item cases copy the `control.chat.knows.item` select; fixed in TASK-30 by reading the branches from the catalogue template.
- c8a3a8 (CodeRabbit, MockRuntimeCli.java:71): a second list of valued options beside `parseOption`; fixed in TASK-31 with one shared option definition.
- f18235 (CodeRabbit, MockRuntimeCli.java:319): help text lists timing values by hand; fixed in TASK-31 with `MockTiming.wireValues()`.
- 4262da (CodeRabbit, ChatRegistry.java:972): `parseChatEvent` hand-lists the six `chat.*` names of `EventName`; fixed in TASK-27 with a lookup built from the catalogue and a test over every `chat.*` name.
- 5afe04 (CodeRabbit, SeededChatMessageKeysTest.java:47): three scenario names instead of `StandardScenarios.PRIMARY_SCENARIOS`; fixed in TASK-36.
- 200226 (CodeRabbit review body, ChatScenarioTest.java:101): `subjects()` keeps a second exhaustive list of chat routes; handled in TASK-35.
- be6610 (code-review Q-Gate, ChatMessageKeys.java:136): the mapping accepted any `chat.knows.name` against its own documented refusal of names outside the design table.

## Observation

Seven of the sixteen fixed CodeRabbit findings of the run were this one class, all found after the push. The repository's CodeRabbit path instructions already name the rule ("a hardcoded list that must mirror a set defined elsewhere is a defect unless it is derived from that source"), but neither implementation nor the in-run code review applied it before the PR.

## Candidate corrective action

When code or a test enumerates members of a set that exists elsewhere, derive the enumeration from that source at build or run time, or add a test that compares the copy with the source in both directions. Have the code-review lens check for this before the push.
