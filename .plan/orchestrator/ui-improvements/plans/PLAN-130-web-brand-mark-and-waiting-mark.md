# PLAN-130: Web: The Brand Icon, the Mascot Tokens and the WaitingMark (D3 Brand Assets, Part 1)

epic: ui-improvements
workstream: WS-02

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Drafted 2026-11-10 from the
> AGREED D3 documents. This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

D3 (PLAN-114, merged `64a18ed1`, AGREED) designed the brand of the web app; the code still draws the old abstract mark
(three ascending sheets, a baton and a node) on a violet plate. This plan delivers the first half of the D3 brand assets
package: the five `--color-mascot-*` tokens, the `BrandMark` redrawn as the marshal star badge (colour and
monochrome, no plate), the browser-tab copies, and the new `WaitingMark` (the clipboard whose lines are ticked one after
another). It also settles seam S12 (distance of `kind-plan` from the clipboard). The mascot figure and its surfaces are
the next package (PLAN-131).

## Deliverables

The documents are the specification; read them first: `doc/design/web-app/brand-mark.adoc` (§ `#palette`, `#icon`,
`#colour`, `#uses`, `#favicon`, `#waiting-mark`, `#theme`, `#implementation`), `doc/design/web-app/components-brand.adoc`
(§ `#brand-mark`, `#waiting-mark`), `doc/design/web-app/components-tags.adoc` (§ `#kind-tints`, `#kind-mark`),
`doc/design/web-app/theme-readability.adoc` (the mascot rows), `doc/resources/brand/*.svg` (the reference drawings).

1. **The mascot tokens**
 - What:
   - Add `--color-mascot-fur` `#E17512`, `--color-mascot-gold` `#F2B630`, `--color-mascot-hat` `#A0602E`,
     `--color-mascot-cream` `#F6DDB0`, `--color-mascot-outline` `#2A1A10` to `pm-web-app/src/styles/tokens.css`, the
     same in both themes.
   - `tokens.test.ts` allows these literals in `tokens.css` only; the kind-tint check reads fur and gold from the
     tokens (`components-tags.adoc` § `#kind-tints`).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - A test asserts the five values in both themes and fails on a different value (evidence order: `test-first`).
   - A test fails when a mascot colour literal appears in any `.svelte` or `.css` file other than `tokens.css` and the
     two static tab copies (evidence order: `test-first`).
2. **`BrandMark` as the star badge**
 - What:
   - Redraw `BrandMark.svelte` as the badge of `doc/resources/brand/star-badge-colour.svg` and `star-badge-mono.svg`:
     inline SVG on `24 × 24` inside `1..23`. In colour its fills are classes of the component's own stylesheet reading
     `--color-mascot-*`, no literal colour. In monochrome every fill is `currentColor`. Under `forced-colors: active` it
     draws the monochrome rendering whatever was asked.
   - Props `rendering` (`colour` default, `monochrome`), `size` (colour `16`, `28`, `64`; monochrome `16`, `20`, `24`,
     `28`, `64`), `label`; `data-testid="brand-mark"`. The `plate` prop and `brand-mark-plate` go. Decorative
     (`aria-hidden`) unless `label` is given (`role="img"`).
   - Callers: `NavRail.svelte` (28 px, colour, no plate; the collapsed rail shows the icon alone with the wordmark as its
     accessible name), `PairingView.svelte` (64 px where the hero figure is not shown, which is every case until
     PLAN-131), the header the onboarding shows before onboarding (28 px; find it), `EmptyState.svelte` `brand`
     (monochrome 64 until PLAN-131 removes `brand`).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `BrandMark.test.ts` covers both renderings, every delivered size, a size outside the set refused by the type,
     `label` and forced colours, and fails on a revert (evidence order: `test-first`).
   - `pm-web-e2e/tests/chrome.spec.js` (the rail-head expectation, currently `brand-mark-plate`) and
     `pairing.spec.js` follow the new drawing; no `brand-mark-plate` remains
     (`git grep -n brand-mark-plate -- pm-web-app pm-web-e2e` is empty; evidence order: `none`).
3. **The browser tab copies**
 - What:
   - Redraw `pm-web-app/static/brand-mark.svg` and `brand-mark-attention.svg` as `doc/resources/brand/favicon.svg` and
     `favicon-attention.svg` (palette baked in, the attention dot `#B86E00` inside a white ring, inside the `1..23`
     grid). `favicon.ts` does not change.
   - Remove `pm-web-app/src/lib/icons/brand/plan-marshall-mark.svg` if nothing uses it after deliverable 2
     (`BrandMark.test.ts:8` reads it today).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css
 - Done when:
   - `favicon.test.ts` still passes unchanged, and a new test asserts both files carry only palette colours and
     keep every coordinate inside `1..23` (evidence order: `test-first`).
4. **`WaitingMark`**
 - What:
   - New `WaitingMark.svelte` (prop `label`): the clipboard as the paths of `doc/resources/brand/waiting-mark.svg` drawn
     inline at the UI set's `1.5` stroke in `currentColor` (`--color-text-secondary`), `20` px beside its text. The mark
     is `aria-hidden`; the line (mark and text) is a polite `role="status"`.
   - The three lines are ticked one after another by CSS in the component's own stylesheet: each tick drawn in `0.3 s`,
     `0.6 s` after the one before, then the full list holds, fades, and starts again; one cycle `3.6 s`. Reduced motion:
     the mark stands still with the list unticked. It sets no `style` attribute (the CSP blocks it).
   - A fixture on `routes/dev/components/+page.svelte` in both themes and both locales.
   - Placing it is not this plan's: the chat's working line is PLAN-135; the onboarding `[S]` and `[M]` states and the
     machine settings' model tests belong to the parked onboarding and settings plans (PLAN-64, 66, 60).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `WaitingMark.test.ts` asserts the status role, the aria-hidden mark, the 20 px size, the unticked still state
     under reduced motion, no inline `style`, and the label rendered as text (evidence order: `test-first`).
5. **Seam S12 and the browser evidence**
 - What:
   - Check that no kind mark reads as the clipboard or the star badge at `16` and `24` px (`brand-mark.adoc` § `#distance`,
     `components-tags.adoc:198`). `icons/ui/kind-plan.svg` is a board with a tick and a line and no clip: compare it with
     `kind-task`, `kind-report` and the badge. Redraw `kind-plan` only if it reads as a clipboard; the final kind marks
     are the D1 foundation package's.
   - Browser check in German and English, light and dark, at `1280` px and on the phone.
 - Persona: plan-marshall:persona-integration-tester
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - Browser check (Playwright MCP) shows the rail head (expanded and collapsed), the pairing page and the tab icon on
     `fully-onboarded` and `running-work`; screenshots recorded, with the S12 finding stated in the PR (evidence order:
     `none`).
   - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open
     (`unknown` for a failed read, never `0`); a confirmed `0` or every open issue dismissed (evidence order: `none`).

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`): `./mvnw -pl pm-web-app verify`, `./mvnw -pl pm-web-app verify -Ppre-commit`
  (review every rewrite), `PM_MOCK_WEB_PORT=<mock port> PM_WEB_PORT=<app port> ./mvnw -pl pm-web-e2e verify -Pe2e` at the head.
- No new dependency or plugin. Web standards of `CLAUDE.md` (ES modules, flat ESLint, no `innerHTML`/`{@html}`, no timer
  waits in tests); no `style` attribute (CSP); every new text from the catalogue in en and de; the text guard baseline
  does not grow.
- Frontend tests on Vitest with Testing Library (`pm-dev-frontend:jest-testing`).
- Ports of this plan: mock web port 21300 (`20000 + 10 × 130`), app port 21301 (`AGENTS.md` § Ports); other plans run
  at the same time: never stop or reuse a process this worktree did not start.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.
- The merge rule: the merge question is asked only when every review thread has a reply and is resolved and every review
  body is answered, and some head has had one CodeRabbit review. The pull request body states thread count, resolved
  count and review-body items from `ci pr comments`, never from memory. Merge only on the operator's word. Commits end
  with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`; the pull request body carries no generator footer.
- ZERO Sonar findings (the operator, 2026-11-05): the pull request merges with 0 open new-code issues (SonarCloud project
  `plan-marshall_pm-mcp-clients`), every open issue fixed in a commit of THIS pull request. State `sonar_open_new_issues=N` in the
  landing message. A dismissal needs the operator's word.
- If CodeRabbit refuses on quota, stop and ask the operator: a lane's standing review authorization never replaces the one
  CodeRabbit review (PLAN-36 merged unreviewed; epic.md Open Defects).

## Claim Labels

- OBSERVED: `BrandMark.svelte` has the props `size` (20, 24, 64), `plate`, `label` and draws sheets, a baton and a node — read at `pm-web-app/src/lib/components/BrandMark.svelte` § props and glyph snippet, at `1cc0e10e`
- OBSERVED: the rail head renders `<BrandMark size={24} plate />` in two places — read at `pm-web-app/src/lib/shell/NavRail.svelte` § `brand-home` and `brand-inert`
- OBSERVED: `PairingView` renders `<BrandMark size={64} />` — read at `pm-web-app/src/lib/views/PairingView.svelte:91`
- OBSERVED: `EmptyState` has the prop `brand` and draws `<BrandMark size={64} />`; its only caller is `ReportsView.svelte:933` — read at `pm-web-app/src/lib/components/EmptyState.svelte` § `brand`; `git grep EmptyState -- pm-web-app/src/lib` (the call site at `ReportsView.svelte:933` is not read for a `brand` attribute)
- OBSERVED: `app.html` links `brand-mark.svg`; `static/` holds `brand-mark.svg` and `brand-mark-attention.svg` — read at `pm-web-app/src/app.html:6`, `pm-web-app/static/`
- OBSERVED: the design's reference drawings exist: `doc/resources/brand/{star-badge-colour,star-badge-mono,favicon,favicon-attention,waiting-mark}.svg`, and `favicon-attention.svg` keeps the dot inside the grid — read at `doc/resources/brand/`
- OBSERVED: no `WaitingMark.svelte` and no `--color-mascot-*` exist — absence read by `git ls-files` and `ls pm-web-app/src/lib/components` at `1cc0e10e`
- HYPOTHESIS: `tokens.test.ts` has a rule that restricts colour literals to `tokens.css` that the mascot literals must be added to — confirm/refute at `pm-web-app/src/styles/tokens.test.ts` § literal-colour rule (verify-at-outline)
- HYPOTHESIS: the header the onboarding shows before onboarding draws no `BrandMark` today and is built in `+layout.svelte` or `OnboardingView` — confirm/refute at `pm-web-app/src/routes/+layout.svelte` and `pm-web-app/src/lib/views/OnboardingView.svelte` § header (verify-at-outline)
- HYPOTHESIS: the kind-tint check that reads fur and gold sits in a Vitest or e2e spec (`theme-readability.spec.js`) — confirm/refute at `pm-web-e2e/tests/theme-readability.spec.js` § kind tints (verify-at-outline)
- Verify-first clause: re-read every named file at the current HEAD before scoping; a part of the old mark found in a view other than the four callers named is added to deliverable 2.

## Expected Surface

- OBSERVED: `pm-web-app/src/styles/tokens.css` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/src/styles/tokens.test.ts` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/src/lib/components/BrandMark.svelte` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/src/lib/components/BrandMark.test.ts` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/src/lib/components/WaitingMark.svelte` — absent at `1cc0e10e`; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/WaitingMark.test.ts` — absent at `1cc0e10e`; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/EmptyState.svelte` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/src/lib/shell/NavRail.svelte` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/src/lib/views/PairingView.svelte` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/static/brand-mark.svg` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/static/brand-mark-attention.svg` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/src/lib/favicon.test.ts` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/src/lib/icons/brand/plan-marshall-mark.svg` — present at `1cc0e10e`; removed if unused
- OBSERVED: `pm-web-app/src/lib/icons/ui/kind-plan.svg` — present at `1cc0e10e`; changed only if S12 requires it
- OBSERVED: `pm-web-app/src/routes/dev/components/+page.svelte` — present at `1cc0e10e`
- OBSERVED: `pm-web-e2e/tests/chrome.spec.js` — present at `1cc0e10e`
- OBSERVED: `pm-web-e2e/tests/pairing.spec.js` — present at `1cc0e10e`
- OBSERVED: `pm-web-e2e/tests/theme-readability.spec.js` — present at `1cc0e10e`

## Dependencies and Sequencing

- Depends on: `PLAN-45` landing (it is running and edits `routes/dev/components/+page.svelte`, `chrome.spec.js`, `routes/+layout.svelte` and `components/`); the D3 documents are AGREED (`64a18ed1`).
- Overlaps with:
  - `PLAN-45` (running): the three files above.
  - `PLAN-85` (parked): `NavRail.svelte`, `PairingView.svelte`, `chrome.spec.js`, `pairing.spec.js`, `theme-readability.spec.js`. This plan lands first and PLAN-85 is re-grounded on it.
  - `PLAN-70` (parked): the mascot rows and the rail-foot opacity check; it follows this plan and PLAN-131.
  - `PLAN-72` (parked): `tokens.css` and the token lint; it follows.
  - `PLAN-64`, `PLAN-66` (parked): the onboarding header (28 px brand icon is this plan's, the figures are PLAN-131 and PLAN-64).
- Adjacent to: `favicon.ts` (unchanged; PLAN-49 will point the badge, the tab title and the tab icon at one count); PLAN-44's terminal "brand line" (the terminal has no brand mark here).

## Re-Grounding at Restart

Re-read every file above at the then-current HEAD (this spec was grounded at `1cc0e10e`). Re-run `corpus surfaces` against everything that landed since (PLAN-45 first). Settle each HYPOTHESIS. Re-check the D3 documents are still `AGREED` and that `brand-mark.adoc` § `#uses` still names the sizes 16/28/64 for colour.

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-130-web-brand-mark-and-waiting-mark.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under
`.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger write.
The session reports its outcome through its PR and its inbox message (`persona-plan-orchestrator/standards/orchestration-model.md`
§ Ledger Write-Boundary).
