envelope_version=1
sender_type=plan
sender_id=rb-71-verify-web-onboarding
epic=ui-improvements
kind=finding
created=2026-11-26T07:46:07Z
revision=3
amended=2026-11-26T10:28:10Z

# Finding: `UC-V-05` fails on the `harness.none` row of `UC-P-03`

**Check:** `UC-V-05` of `doc/claude_review/use-cases/verification.adoc` — "Per screen of UC-P-03, the change in the
table is visible. Before onboarding is done, the header has no navigation. After 'Use Deutsch for both', the next
screen is German."

**Verdict:** `fail` (the header and language clauses pass).

**Evidence:** the walk at `da3cafdf` (mock scenario `onboarding`, `en`/`de`, light/dark, 1440 × 900 and 390 × 844;
screenshots `.plan/temp/screens/matrix-<state>-<locale>-<theme>-<width>.png`).

- Header: on `onboard.mode` it is `Machine: mock-machine` alone — no breadcrumb, no search, no live indicator, no rail
  destination, the brand inert; the step bar marks no step on the mode screen, and on `onboard.completed` it ticks
  *Done* and marks no step current (`.plan/temp/screens/stepbar-onboard.mode-en-dark-1440.png` and
  `.plan/temp/screens/stepbar-onboard.completed-en-dark-1440.png`).
- Language: after "Use Deutsch for both" the next screen is German (`document.documentElement.lang` `en` → `de`;
  `.plan/temp/screens/lang2-before-nochoice-en-dark-1440.png` and `lang2-after-nochoice-en-dark-1440.png`). A browser
  that chose its own language keeps it, which is the design.
- Ten of the eleven screens show the change of `UC-P-03`. The failure is `harness.none`: the screen shows "No harness
  found", the search line and the install hints (`claude` → `docs.anthropic.com/en/docs/claude-code/setup`, `opencode`
  → `opencode.ai/docs/`), with "Check again" and "Add a harness that is not listed". It shows no per-harness name,
  path, failed check, answer, and fix — the row `UC-P-03` asks for (`matrix-harness.none-<arm>.png`).

The state carries the install hints and no per-harness failure facts; the engine's catalogue describes `harness.none`
as the install hints and names the per-harness cause and fix under `harness.blocked`, where the screen does show them
(`harness.probe-failed`: name, cause, fix). Either the check's row or the state's contract has to move; the screen
follows the catalogue.

**Two further clauses are `not verified`, with their reason:**

- `catalogue.select`: the advice's recommendation preselected and marked "recommended" is not in the mock — the state
  carries no advice and no recommendation. The text field per harness that cannot list its models is present.
- Settings (`UC-ONB-10`): one settings place is present (`/settings` with its tabs; `/settings/machine` resolves into
  it). The conflict notice is drawn on that page when a save conflicts — the current value, the value "yours", keep,
  and "Apply mine again" (`SettingsView.svelte:1072-1088`, fed by `SettingsView.svelte:721-731`). It is not verified
  from the mock: the walk at `settings.conflict` shows the settings place without a notice, because the mock's state
  does not feed a conflicting save (the note on `UC-F-12` says so). The unit tests cover the dialog.

**Also stale (not a failure of the app):** `UC-V-03`'s text asks for the LAN URL, the QR code and the fingerprint
inside the settings frame. `entry.*` states have no web screen (operator decision 2026-11-10,
`doc/design/web-app/onboarding.adoc:45`); the fallback names `pm-operator` and no state id, and that is what the plan's
re-grounding of 2026-11-25 asked to verify. The check's text should say so.

**Plan concerned:** PLAN-66 (`web-onboarding-frame-and-roles`, D5 `harness.none`, the former PLAN-68) and the review
documents for the two stale rows (`UC-P-03`'s `harness.none` and `UC-V-03`'s settings frame).

**Nothing was fixed:** this plan verifies only. The result file is
`doc/claude_review/verification-results/verify-web-onboarding.adoc` (PR of this plan).
