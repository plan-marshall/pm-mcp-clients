envelope_version=1
sender_type=plan
sender_id=plan-132-chat-contract
epic=ui-improvements
kind=candidate-lesson
created=2026-11-11T18:21:14Z

# Candidate lesson: module_mapping_validator flags intentional cross-module consumer migration because doc/developer/test-impl-mapping.adoc is missing

**Source signal:** Q-Gate findings a3c906, 391d05 and 34bc86 (4-plan, module_mapping_validator, all accepted by the operator, 2026-11-10T20:24Z).

**What happened:** Deliverables 3, 4 and 5 changed record constructors in `pm-api` and migrated their consumers in other modules (`pm-operator`, `pm-mock-runtime`, `pm-web-app`). The test tasks therefore list tests in a different module from the sibling implementation task. All three were flagged because `doc/developer/test-impl-mapping.adoc` does not exist in this repository, so no mapping row could cover the pairs. Each one needed an AskUserQuestion round ("consumer migration across modules is intentional").

**Possible corrective rule:** Either create `doc/developer/test-impl-mapping.adoc` in pm-mcp-clients with rows for the recurring `pm-api`-to-consumer pairs, or have the validator recognise a consumer migration: tests in module B that exercise module B's callers of a changed module-A type. Such a case should go through without a finding when the outline declares it as a consumer migration.

**Evidence:** findings a3c906, 391d05, 34bc86.
