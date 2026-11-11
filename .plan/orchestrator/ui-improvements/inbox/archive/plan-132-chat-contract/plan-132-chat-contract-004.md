envelope_version=1
sender_type=plan
sender_id=plan-132-chat-contract
epic=ui-improvements
kind=candidate-lesson
created=2026-11-11T18:21:04Z

# Candidate lesson: Sonar new-code issues in new tests found only after the first push cost two extra finalize cycles

**Source signal:** loop-back from 6-finalize to 5-execute (2026-11-11T13:52:24Z), fix tasks TASK-011 to TASK-014.

**What happened:** After the first push, SonarCloud reported new-code issues. According to the operator these were 34 issues, mostly S5778 (more than one call that can throw inside an assertThrows lambda) and S1130 (a declared exception that cannot be thrown), in the new tests. This was not checked against Sonar here; the plan records show only the fix tasks: TASK-011/012/013 "Fix the Sonar new-code issues in ChatRoutes, MockRuntimeEngine, ChatReadModelTest, DecisionRecordsTest, ChatTextsCatalogueTest and ProcessTreeTest". The loop-back ran a full second finalize cycle: simplify, functional and code review, pre-push gate, push, CI and automatic review. The rebase at branch-cleanup (loop_back at 16:00:57Z) then forced a third cycle.

**Possible corrective rule:** Catch the cheap static-rule classes locally before the first push. That means an assertThrows lambda holding only the call under test, and no `throws` clause for checked exceptions the body cannot throw. They can go into the Java test-writing standards (junit-core) or into a pre-push Sonar-equivalent check. The code review raised the related advisory at ChatReadModelTest.java:287 ("assertThrows lambdas still hold set-up calls") only as advisory, after the fix. A Sonar-blocking rule class should be graded as blocking during the in-run review, before the push.

**Evidence:** decision log 13:52:24Z (loop-back), 15:02:39Z (advisory at ChatReadModelTest:287); work log TASK-011..014 completion lines around 14:09Z.
