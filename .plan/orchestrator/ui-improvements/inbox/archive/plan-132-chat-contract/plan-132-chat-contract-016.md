envelope_version=1
sender_type=plan
sender_id=plan-132-chat-contract
epic=ui-improvements
kind=candidate-lesson
created=2026-11-11T18:21:23Z

# Candidate lesson: a message text assumed an optional event field (channel, device) was always present

**Source signal:** review-bot finding 1b0de6 (CodeRabbit, Minor x2, remediated in run by TASK-14, commit c5cd818c).

**What happened:** `DecisionAnsweredEvent.channel` and `.device` are optional. Without a channel, `MessageCatalogue` selected the `other` branch, so the text claimed a terminal answer without evidence. Without a device, the web narrative left a literal `{device}` placeholder in the displayed text. The fix added an explicit `cli` case, a neutral wording for an absent channel, and a device-free variant, in both locales and in the design table. A remaining advisory: the mock runtime writes an absent device as an empty string, while the event and the no-device text treat it as absent (MockRuntimeEngine.java:9852).

**Possible corrective rule:** For every catalogue text whose arguments come from an optional event field, provide a variant for the absent case, plus a catalogue test that renders each text with each optional argument left out and asserts that no placeholder remains. Mocks should leave an optional field out instead of writing an empty string.

**Evidence:** pr-comment 1b0de6; advisory d5b070 / F9 (MockRuntimeEngine absent device as empty string).
