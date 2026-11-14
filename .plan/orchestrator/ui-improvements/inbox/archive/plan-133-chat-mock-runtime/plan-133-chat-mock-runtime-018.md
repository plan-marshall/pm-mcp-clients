envelope_version=1
sender_type=plan
sender_id=plan-133-chat-mock-runtime
epic=ui-improvements
kind=candidate-lesson
created=2026-11-13T12:27:26Z

# Candidate lesson: plan-time declarations did not match the change footprint

**Plan:** plan-133-chat-mock-runtime (PR #424, merged as a139ec62)
**Signal:** Q-Gate findings of 3-outline, 4-plan and 5-execute (all warnings, accepted in run)
**Class:** the outline and the task plan declared verification commands, residue items, module mappings and file scope that did not cover what the change actually touched. Each was raised as a Q-Gate warning and then accepted later as "overtaken by execution" because the PR build verified everything anyway.

## Records of the class

- 36bb3e (3-outline): deliverable 8 mutates `pm-relay/.../PmMcpProcessIT.java`, but both verification commands ran `-pl pm-api,pm-mock-runtime,pm-operator -am`; `-am` pulls dependencies, not dependents, so the relay IT was never built by the declared verification. The deliverable was also labelled `module: pm-mock-runtime` while it mutated pm-operator and pm-relay tests.
- f20f7b (3-outline): the request's five PLAN-132 residue points were split into six items across deliverables 2 and 3, one (ChatResource Javadoc wording) declared without the required failing-on-revert test.
- e164d0 (4-plan): module_mapping_validator — test task 16 wrote 21 pm-operator files and a pm-relay IT while its implementation sibling (task 15) touched only pm-mock-runtime `ScenarioLoader`; `doc/developer/test-impl-mapping.adoc` does not exist, so no mapping row covered the pairs.
- 81cb8a (5-execute): scope creep — 6 files outside the declared scope (threshold 5): OverviewRecordsTest, ProcessTreeTest, MockRuntimeLogMessages, EventTiming, MockTiming, SeededChatMessageKeysTest.

## Observation

All four were accepted after the fact. The verification gap of 36bb3e is real in principle: had the PR build not been a full reactor build, the changed relay IT would have gone unverified by the plan's own commands.

## Candidate corrective action

Derive a deliverable's verification module list from its declared mutate set (every module owning a mutated file, with `-amd` where dependents are affected), split cross-module start-site edits into their own deliverable, and count residue items against the request's list one-to-one at outline time. Consider creating `doc/developer/test-impl-mapping.adoc` if cross-module test routing is a recurring pattern in this repository.
