envelope_version=1
sender_type=plan
sender_id=plan-133-chat-mock-runtime
epic=ui-improvements
kind=candidate-lesson
created=2026-11-13T12:25:59Z

# Candidate lesson: behaviour carried by a removed route is not re-homed

**Plan:** plan-133-chat-mock-runtime (PR #424, merged as a139ec62)
**Signal:** functional-review and code-review Q-Gate (6-finalize)
**Class:** the plan removed an old route (POST ended accept on the project path) and moved its chat to the new chat registry, but the side effect the old route performed (opening a new plan start with the follow-up description) was not carried to the new path. The tests of that side effect were removed with the route, so nothing failed.

## Records of the class

- aa6a55 (functional review, residual D7.R1): accepting the follow-up proposal of ended work changed nothing; the design (work-actions.adoc, "A Chat About Ended Work") says it opens a new plan start with that description. Fixed by TASK-21 (0dbe6b29).
- 812352 (functional review, weakened_test StartContractTest followUpPlan): 3 assertions gone that checked exactly this behaviour; they returned with the D7.R1 fix.
- 0b8991 (functional review, StartScenarioTest FollowUp::followUpScenario): follow-up assertions gone, restored with the fix.
- 9d1f58 (code review, MockRuntimeEngine.java:862): an applied follow-up proposal of work that ended at run time opened no start, and the hook dropped the result without a trace.

## Observation

The functional review's weakened-test detector is what tied the removed assertions to the missing behaviour; without it the regression would have merged.

## Candidate corrective action

When a route or handler is removed, list each observable effect it had (state written, events emitted, starts opened) and name in the outline where each effect lives afterwards; keep or move the assertions of each effect rather than deleting them with the route.
