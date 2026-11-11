envelope_version=1
sender_type=plan
sender_id=plan-132-chat-contract
epic=ui-improvements
kind=candidate-lesson
created=2026-11-11T18:21:20Z

# Candidate lesson: a design table declared read-only had to change because a test compares the message packs to it

**Source signal:** Q-Gate finding 068405 (6-finalize, functional-review, unrequested_change, error, accepted by the operator); related CodeRabbit finding 1b0de6 (remediated in run).

**What happened:** The outline declared `doc/design/shared/work-actions-chat-texts.adoc` read-only for deliverable 4. CodeRabbit finding 1b0de6 then required new decision-answered wordings: an explicit `cli` case, a neutral wording for an absent channel, and a variant without a device. `ChatTextsCatalogueTest` compares the `control.*.properties` packs to that design table, so commit c5cd818c also had to edit the table: a changed `decision_answered` text and a new key `decision_answered_no_device`. The functional review reported this as a blocking `unrequested_change`, which needed an operator close.

**Possible corrective rule:** When an outline changes catalogue texts that a test compares to a design table, declare the table as a modified file, not a read-only one, even if no table change is expected at outline time. The comparison test ties the two together, so a later fix to the texts always touches the table. A loop-back fix task should also update the outline's declared footprint when it widens the footprint.

**Evidence:** finding 068405 (resolution 2026-11-11T14:53:03Z); pr-comment 1b0de6 (fix TASK-14, commit c5cd818c).
