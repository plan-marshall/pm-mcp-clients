envelope_version=1
sender_type=plan
sender_id=plan-133-chat-mock-runtime
epic=ui-improvements
kind=candidate-lesson
created=2026-11-13T12:26:19Z

# Candidate lesson: intentional test removals flagged as weakened tests, twice

**Plan:** plan-133-chat-mock-runtime (PR #424, merged as a139ec62)
**Signal:** functional-review Q-Gate (6-finalize)
**Class:** the outline answer `entry_chat_old_shape=A` decided that the mock stops writing the old start-state chat member, so the old entry chat tests were deleted on purpose. The functional review's weakened-test detector does not know about outline decisions, flagged every deleted assertion as a blocking weakened_test, and re-flagged the same deletions in the next round with new hashes.

## Records of the class

- Round 1 (11:29-11:30): 901eb9 (StartDialogueTest, 17 assertions: chatProposalAccepted, chatProposalInvalidAnswer, chatProposalRejected, chatThenStart), 0b8991 (StartScenarioTest Chat::abandonDeletesTranscript plus FollowUp — the FollowUp part was a real regression, see the separate message on removed-route behaviour).
- Round 2 (12:46): 027030 (same 17 StartDialogueTest assertions) and ab6eb8 (same StartScenarioTest abandonDeletesTranscript assertions) — duplicates of round 1 under new hashes.
- All accepted by the operator with the same text citing `entry_chat_old_shape=A`.

## Observation

The detector mixed a planned removal and a real regression in one finding (0b8991), and an accepted removal was raised again in the next round, each needing an operator acceptance.

## Candidate corrective action

Record planned test removals (test ids plus the outline decision that justifies them) as data in the plan at outline time, and have the weakened-test check exclude them and carry an accepted removal across rounds, so only unplanned assertion losses are raised.
