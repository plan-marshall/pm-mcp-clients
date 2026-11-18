envelope_version=1
sender_type=plan
sender_id=rb-164-web-plan-changes-tab
epic=ui-improvements
kind=finding
created=2026-11-18T11:30:14Z

# Amendment — the review residue is closed

Follow-up to this landing, as the operator's answer to the question it recorded ("Fix F1/F2 in a follow-up PR
now").

**#481** — `fix(web): close the review residue of a plan's changes (PLAN-164)` — merged as `f688d7be`.

- **F1** a row whose read failed keeps its own error and retry; selecting another row never shows its hunks.
- **F2** a carried `unavailable` capture announces the plan's last successful figures, which the mock keeps and
  serves (no `0 files` on the size line).
- **F3** `PlanView` tests emit `plan.change_captured`: the size line takes the announced figures, the tab reads
  its list again, and another plan's capture is ignored.
- **F4** a superseded read no longer clears the open read's spinner; each read carries a request token.
- **F5** the `outputSafety` header states the character classes it strips and the controls it keeps.
- **F6** the token guard covers secondary and muted text and the success/danger counts on the diff grounds.
- CodeRabbit's eleven minor findings on that head were answered in the same PR (retained totals seeded from the
  declared size, the simulation keeps the figures, the diagnostic names all four totals, the row identity is a
  JSON tuple with the base, and the tests assert the calls they pin).

Verification at the merged head `b6a6c3dc`: `verify -Ppre-commit` and `verify -Pcoverage` green; the Playwright
suite 454 passed / 75 skipped; CI green; Sonar's gate OK with 0 open new-code issues, 0 hotspots and 81.4%
new-code coverage.

With F2 closed, **D0 is fully met** and the landing's deliverable count becomes **3 of 3**; the open items it
listed are closed, so `self-review` is `clear` on this surface. The worktree and branch of the follow-up were
removed after the merge.
