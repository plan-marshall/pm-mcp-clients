envelope_version=1
sender_type=plan
sender_id=plan-133-chat-mock-runtime
epic=ui-improvements
kind=candidate-lesson
created=2026-11-13T12:24:54Z

# Candidate lesson: mechanical Sonar rules across three PR rounds

**Plan:** plan-133-chat-mock-runtime (PR #424, merged as a139ec62)
**Signal:** automated review (SonarCloud)
**Class:** small mechanical Sonar code smells that a reviewer applying the Sonar Java rule set would catch locally. They came in three waves (17:10, 23:16 and 08:47), and part of each later wave was introduced by the fixes of the earlier one.

## Records of the class

- Duplicated literals (java:S1192): 64bef6 ChatRegistry "The chat has ended" x3 (TASK-26); 9ed216 ScenarioLoader "representation" x7 (TASK-29); 856a4d ScenarioLoader "status" x4 (TASK-40, second wave).
- Method belongs in the other class (java:S3398): a89c3f MockRuntimeCli.parsePortArg into ValuedOption (TASK-41) — ValuedOption was itself created by the TASK-31 fix of c8a3a8; 3bdaa9 and d529a4 StartApi.codeOf/messageOf into StartActException (TASK-44) — helpers added by the TASK-42 fix of 1e9a87.
- Lambda with more than one throwing call (java:S5778): 73eed8, 2d72a0 ChatMessageKeysTest (TASK-30).
- Wildcard generic in a return type (java:S1452): b71c8f EventTiming.play (TASK-28).
- Restricted identifier as method name (java:S6213): 2d8f59 ChatRegistry (TASK-26).
- Conditional outside the operation (java:S9358): b1b165 ChatRegistry (TASK-26).
- Lambda instead of method reference (java:S1612): a68f06 FullyOnboardedScenarioTest (TASK-35).
- Local variable hides a field (java:S1117): 9f8a8c MockRuntimeEngineTest.brokenState (TASK-45, third wave).

## Observation

Each wave cost a commit, a push, a CI run and a new Sonar analysis; the second and third waves were caused largely by fix commits that added new helpers or test locals without a Sonar-rule pass over the new code.

## Candidate corrective action

Before pushing a fix round, review the new and moved code of the fix itself against the common Sonar Java rules (S1192, S3398, S5778, S1452, S6213, S1612, S1117), not only the finding being fixed.
