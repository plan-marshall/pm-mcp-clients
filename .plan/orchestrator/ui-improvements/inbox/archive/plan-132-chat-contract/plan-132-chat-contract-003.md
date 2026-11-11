envelope_version=1
sender_type=plan
sender_id=plan-132-chat-contract
epic=ui-improvements
kind=candidate-lesson
created=2026-11-11T18:21:03Z

# Candidate lesson: the functional review's test-integrity audit reports a test split as weakened_test

**Source signal:** Q-Gate finding cecc65 (6-finalize, functional-review, severity error, resolution accepted by the operator).

**What happened:** A Sonar S5961 fix (too many assertions in one test) split `ProcessTreeTest::shouldRoundTripProcessTree` into three tests that share one fixture. Every assertion was kept. The test-integrity audit counts assertions per test method. It saw 15 assertions leave the original method while the total for the file did not drop by the same amount, and reported a blocking `weakened_test`. That blocked finalize until the operator closed it through AskUserQuestion ("Accept both").

**Possible corrective rule:** Compare the assertion count of the whole file, or of the original method plus the methods newly added to the same class in the same commit, before raising `weakened_test`. When the file's assertion total is unchanged or higher and the new methods reuse the original fixture, treat it as a split: report it as advisory or not at all, not as blocking. A split made to satisfy a static-analysis rule should not need an operator to close it.

**Evidence:** finding cecc65, resolution_detail of 2026-11-11T14:53:03Z.
