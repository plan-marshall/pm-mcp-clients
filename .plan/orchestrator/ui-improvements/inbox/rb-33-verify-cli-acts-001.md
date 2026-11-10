envelope_version=1
sender_type=plan
sender_id=rb-33-verify-cli-acts
epic=ui-improvements
kind=finding
created=2026-11-10T07:37:55Z

# Finding: `CLI-UC-V-06` fails on its `onboard --auto --yes` clause

**Check:** `CLI-UC-V-06` of `doc/claude_review/cli/use-cases/verification.adoc` — "`onboard --auto --yes` reaches the
result table".

**Verdict:** `fail` (the rest of the check passes).

**Evidence:** `pm-operator` has no `onboard` verb. In the capture set `PLAN-33` (run on commit `0913d6f3`, scenario
`onboarding`), `pm-operator onboard --auto --yes` writes

```text
Unmatched arguments from index 0: 'onboard', '--auto', '--yes'
Did you mean: pm-operator lesson or pm-operator questions or pm-operator enrol-harness?
```

and exits `2` (capture `onboard-auto-yes-120x30-en.stdout.txt`, artifacts under
`pm-operator/target/screens/PLAN-33/`). The automatic onboarding now starts from the entry's first choice
`1) Decide everything for me…`, not from a verb.

**What the check does prove:** `h` then Enter draws `[1/7] Language`; every state renders catalogue texts, never a link
name; the guided walk reaches `onboard.completed`; `project setup cui-parent` shows the identity state with id, name and
pilot. `OnboardingDialogueIT` and `ProjectSetupDialogueIT` pass.

**Plan concerned:** PLAN-22 (`cli-state-dialogue-onboarding`, "One State Dialogue for Both Onboardings"). Its
deliverable moved the automatic path into the entry dialogue; either the check's wording follows, or the verb is meant to
exist.

**Nothing was fixed:** this plan verifies only.
