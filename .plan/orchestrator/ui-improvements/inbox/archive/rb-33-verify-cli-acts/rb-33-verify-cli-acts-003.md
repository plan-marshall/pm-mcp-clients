envelope_version=1
sender_type=plan
sender_id=rb-33-verify-cli-acts
epic=ui-improvements
kind=finding
created=2026-11-10T08:05:57Z

# Finding: `CLI-CC-V-06` fails on the waiver path

**Check:** `CLI-CC-V-06` of `doc/claude_review/cli/cross-cutting/verification.adoc` — "Every dialogue that acts has a PTY
test with the three cases, and `/mock/acts` proves that `q` sends nothing." The regression guard of
`doc/claude_review/cli/use-cases/verification.adoc` names eight dialogues: pause, cancel, answer, **waiver**, onboarding,
project setup, cleanup, runtime stop.

**Verdict:** `fail` (seven of the eight are covered).

**Evidence:** run on commit `0913d6f3`; `mvn verify -Pintegration-tests -pl pm-operator` is green (119 tests, 0
failures), and seven of the eight have their three cases — `PauseDialogueIT` (3), `CancelDialogueIT` (4),
`AnswerDialogueIT` (9), `OnboardingDialogueIT` (4), `ProjectSetupDialogueIT` (4), `CleanupDialogueIT` (6),
`RuntimeStopDialogueIT` (8) — each `qFirstSendsNothing` asserting `Nothing was sent.` and `harness.assertNoAct()` over
`GET /mock/acts` (`DialogueHarness.java:130`). The waiver path has one test only:
`AnswerDialogueIT.waiverAnswers` (`pm-operator/src/test/java/de/planmarshall/operator/transcript/AnswerDialogueIT.java:102-118`)
posts `WR-2` with `y` and its reason. There is no `q`-first case and no unknown-input case for a waiver request, so
nothing proves that `q` on a waiver request sends no act — the case the guard exists for, on the one dialogue whose act
is a gate waiver.

**Plan concerned:** PLAN-21 (`cli-answer-dialogue`), whose dialogue answers the waiver request (`WR-2`). The fix is one
PTY test with the three cases for `WR-2`; this plan files it and fixes nothing.

**Nothing was fixed:** this plan verifies only.
