envelope_version=1
sender_type=plan
sender_id=plan-132-chat-contract
epic=ui-improvements
kind=candidate-lesson
created=2026-11-11T18:20:58Z

# Candidate lesson: a whole-tree import-order rewrite pushed the PR over the review-bot size cap

**Source signal:** automatic-review (structural refusal), plan plan-132-chat-contract, PR #409.

**What happened:** The `-Ppre-commit` quality gate rewrote the import order across the whole tree (finalize-internal commit 7c1390f8 from step pre-push-quality-gate). The rewrite swept in files the plan never touched, so PR #409 grew to 148 files / 7291 changed lines. CodeRabbit (cap 100 files) and Sourcery (cap 150000 diff characters) refused the review on size (`refusal_structural`, decision log 10:16:09Z). No wait, trigger or loop-back could fix that. The operator split the PR: the import-order rewrite landed on its own as PR #410 with the `skip-bot-review` label, and PR #409 was re-reviewed on the smaller diff. That cost one extra finalize cycle plus a separate PR.

**Possible corrective rule:** When the pre-commit profile's import-order or formatting rewrite touches files outside the plan's footprint, land that rewrite on its own before the feature PR: a mechanical PR labelled `skip-bot-review`. It should not ride the feature PR. One way to do this is to run the formatter on `main` before the plan branches. Another is to have the pre-push gate detect an out-of-footprint rewrite and stop.

**Evidence:** decision log 2026-11-11T10:16:09Z (refusal STRUCTURAL, 148 files) and 11:30:33Z (operator chose to split the PR); 09:50:45Z (finalize-internal import-order commit).
