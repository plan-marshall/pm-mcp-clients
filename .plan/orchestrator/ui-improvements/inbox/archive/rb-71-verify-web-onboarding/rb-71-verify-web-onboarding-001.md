envelope_version=1
sender_type=plan
sender_id=rb-71-verify-web-onboarding
epic=ui-improvements
kind=finding
created=2026-11-26T07:46:02Z
revision=1
amended=2026-11-26T10:14:04Z

# Finding: `UC-V-02` fails on its state-id regex clause

**Check:** `UC-V-02` of `doc/claude_review/use-cases/verification.adoc` — "No screen in any onboarding or entry state
shows a text that matches a state id (`/[a-z]+\.[a-z-]+/` outside code blocks)".

**Verdict:** `fail` (the unit-test clause passes).

**Evidence:** the walk at `da3cafdf` (mock scenario `onboarding`, all 92 states at 1440 × 900, `en`, dark; screenshots
`.plan/temp/screens/sweep-<state>-en-dark-1440.png`) found no state id of the scenario in any page's text nodes. It
did find three visible texts that match the check's regex outside a `<code>` element:

- `docs.anthropic.com/en/docs/claude-code/setup` and `opencode.ai/docs/` on `harness.none`, in
  `<span class="harness-install__hint mono">` (screenshot
  `.plan/temp/screens/harness-none-hints-en-dark-1440.png`);
- the example model id `e.g. anthropic/claude-sonnet-4-6` in the model-id field's placeholder on `catalogue.select`
  (`pm-api/src/main/resources/messages/onboarding.en.properties:79`; screenshot
  `.plan/temp/screens/matrix-catalogue.select-en-dark-1440.png`).

None is a state id, but the check excludes only *code blocks*. A text-node scan cannot see a `placeholder` attribute;
the screenshot review is what found the third text, so the "no state id" result is proven for text nodes across all 92
states and read off the screenshots, not by an attribute sweep.

The other clause passes: `screens.registry.test.ts` is green in the `pm-web-app` suite (188 files, 3481 tests at
`da3cafdf`), and it reads every state of `pm-mock-runtime`'s `onboarding.json` and fails for one no screen claims
(`screens.registry.test.ts:29-84`). The clause "fails when a state is removed" is proven by the assertion's shape,
not by a mutation run: this plan verifies only and changes no test.

**What the check does prove:** every onboarding and entry state is drawn by a designed screen or the deliberate
fallback, and no screen leaks a state id. The defect is the element or the wording, not the content: a `<code>` element
(or the check's own exclusion) settles it.

**Plan concerned:** PLAN-66 (`web-onboarding-frame-and-roles`, D5 `harness.none`, the former PLAN-68), and the
catalogue text of `UC-P-03`'s `catalogue.select` row.

**Nothing was fixed:** this plan verifies only. The result file is
`doc/claude_review/verification-results/verify-web-onboarding.adoc` (PR of this plan).
