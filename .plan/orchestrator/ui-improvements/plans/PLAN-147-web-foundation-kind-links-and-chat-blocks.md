# PLAN-147: Web Foundation: Kind Mark, Links, Compose and FormStatus (D1 Foundation, Part 1)

epic: ui-improvements
workstream: WS-02

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Drafted 2026-11-10 from the AGREED D1 documents
> (PLAN-112 `aa3a9be3`, set `AGREED` by PLAN-122). This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

D1 specified one closed vocabulary of components for the web app; the code has few of them. This first part ships the six building blocks the
web chat waits for (PLAN-135): `KindMark`, `Link`, `ExternalLink`, `PrChip`, `Compose` and `FormStatus`. It also ships what `KindMark` stands on: the three
kind tints measured by rule, three new kind icons, the redrawn `kind-plan` and the kind words. Everything else of D1 follows in PLAN-148 to PLAN-152.
It is kept small on purpose: PLAN-135 may start the moment this lands.

## Deliverables

Read first: `doc/design/web-app/components-tags.adoc` (§ `#kind-mark`, `#kind-tints`, `#pr-chip`), `components.adoc` (§ `#rules`, `#link`,
`#external-link`, `#new-component`), `components-fields.adoc` (§ `#compose`, `#field-set`), `chat.adoc` (§ `#composer`, `#markdown`, `#status-cards`),
`components-chat.adoc`, `theme-readability.adoc`, and the pictures `doc/resources/diagrams/web-component-{kind-mark,link,pr-chip,compose}.svg`.

1. **Kind tints, kind icons and kind words**
 - What:
   - `tokens.css`: add `--color-kind-project` (`#606700` light, `#ADCD4A` dark), `--color-kind-epic` (`#BB2D8C`, `#EF8BC5`) and `--color-kind-plan`
     (`#007785`, `#3BCDDC`) in `:root` and in the theme blocks the file keeps in step. Remove `--color-epic`, `--color-epic-subtle`, `--color-epic-text`
     and move their one reader `OverviewView.svelte` to `--color-kind-epic`. If the readers at outline are more than that one file, keep the three names as aliases of
     `--color-kind-epic` and say so in the pull request (PLAN-72 removes the aliases).
   - `tokens.test.ts`: `COLOUR_TOKENS` drops `--color-epic` and gains the three tints. A new block computes the four measured rules of `components-tags.adoc` § `#kind-tints`
     in both themes (ΔE00 ≥ 20 to running, attention, danger, success, neutral, brand and the mascot orange and gold; OKLCH hue ≥ 30° away; ≥ 30 between tints;
     ≥ 3:1 on bg-app, bg-surface, bg-raised, bg-subtle, bg-rail and brand-subtle). The colour maths (CIEDE2000, OKLCH, contrast) lives in a test helper, no dependency.
     Fur and gold are read from `--color-mascot-fur` and `--color-mascot-gold` (added by PLAN-130, `tokens.css:97-101` at `078b2ec5`).
   - Icons: new `kind-project.svg`, `kind-side-job.svg`, `kind-machine.svg`, `act-send.svg`, `act-stop.svg` in `pm-web-app/src/lib/icons/ui/`, registered in `uiIcons.ts`,
     drawn to the UI icon contract of `workflow-visual-language.adoc`. `kind-plan.svg` is redrawn so that it is apart from `kind-task` at 16 px and reads neither as a clipboard
     nor as the star badge. PLAN-130 deliverable 5 records the S12 finding; follow it. SETTLED 2026-11-11 (#414 body): rendered at 16 and
     24 px, `kind-plan`, `kind-task` and `kind-report` read neither as the clipboard nor as the badge, and `kind-plan.svg` was left
     unchanged; so the redraw here is driven only by being apart from `kind-task` at 16 px, and the clipboard/badge clause is a check,
     not a reason to redraw. `act-favourite`/`act-favourite-on` stay distinguishable from the badge though close.
   - Catalogue: `common.kind.project|epic|plan|side_job|decision|report|machine`, `common.link.new_tab` and `common.pull_request.chip` (`PR #{number}`) in `common.en/de.properties`
     with the words of `texts-and-languages.adoc` § `#kinds`. The merge of the old `overview.kind.*` keys is PLAN-152's.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:css, pm-dev-frontend:javascript, pm-dev-frontend:jest-testing, pm-dev-java:junit-core
 - Done when:
   - `tokens.test.ts` asserts the six values and the four rules, and fails when a tint is changed to a value that breaks one. Evidence order: `test-first`
     for the values and `mutation-after` for the rule (changed hex and failing assertion quoted in the pull request body).
   - `uiIcons.test.ts` covers the five new names at 16, 20 and 24 and fails on a missing registration. Evidence order: `test-first`.
   - `MessageCatalogueTest` and the web `catalogue.test.ts` pass with the nine new keys in both locales. Evidence order: `test-first`.
   - `git grep -n "color-epic" -- pm-web-app/src` is empty, except `tokens.test.ts`, which names the retired tokens in its absence assertion. Evidence order: `none`.
 - Implements: `components-tags.adoc` § `#kind-mark`, `#kind-tints`; `texts-and-languages.adoc` § `#kinds`.
2. **`KindMark`**
 - What:
   - `KindMark.svelte`, props `kind` (the closed set of seven) and `wordHidden`. It draws the kind's icon and its word `common.kind.<kind>`, no box.
     Colour is on the icon only: the tint token for project, epic and plan, `--color-text-secondary` for the other four. The word has the colour of the text around it.
   - `wordHidden` keeps the word as the accessible name (`sr-only`); the icon never stands without its word in the accessibility tree. A kind outside the set does not compile.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `KindMark.test.ts` covers each of the seven kinds (icon name, word in `en` and `de`, tint class or neutral), `wordHidden` (accessible name kept, icon `aria-hidden`),
     and "no colour on the word". It fails on a revert of the tint mapping. Evidence order: `test-first`.
 - Implements: `components-tags.adoc` § `#kind-mark`, `#tag-colour`.
3. **`Link`, `ExternalLink`, `PrChip`**
 - What:
   - `Link.svelte`: an `<a href>` to a place of the app; props `href`, `label`, `look` (`text` default, `button`, `chip`) and an icon snippet. `text` is underlined in a sentence and gains an underline
     on hover standing alone; never brand-coloured. `button` is the look of a secondary `Button` (the chat's _Open it_). `chip` is the chip look (the chat's reference chips).
     The `look` prop is an amendment of `components.adoc` § `#link`; this pull request writes it (operator decision D-1, assumed A).
   - `ExternalLink.svelte`: props `href`, `label`, `chip`; `target="_blank"` `rel="noopener noreferrer"`, the glyph `act-external` after the label, `common.link.new_tab` in the accessible name.
     An `href` that is not `http` or `https` renders the label as text with no anchor.
   - `PrChip.svelte`: props `number`, `href`; an `ExternalLink` in the chip look with `common.pull_request.chip`. Without an `href` it is the same text without a link, never `href="#"`.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:javascript-security, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - Per component: the attributes (`target`, `rel`), the glyph, the accessible name in `en` and `de`, the three looks, the scheme refusal (`javascript:`, `data:`, `file:` as text), no `href="#"`,
     the focus ring, and a PrChip with and without an address. Each test fails on a revert. Evidence order: `test-first`.
 - Implements: `components.adoc` § `#link`, `#external-link`; `components-tags.adoc` § `#pr-chip`; `chat.adoc` § `#status-cards`, `#markdown`.
4. **`Compose` and `FormStatus`**
 - What:
   - `Compose.svelte`: one `<textarea>` that grows with its text up to `maxLines` (default 8) and then scrolls; the send act is a secondary `Button` with `icon="act-send"` and a visually hidden label.
     `Enter` sends, `Shift+Enter` adds a line, nothing is sent while a composition is open (`isComposing`). Props: bindable `value`, `label` or `ariaLabel`, `placeholder`, `sendLabel`, `disabled`,
     `disabledReason` (a disabled send keeps the reason as its advisory), `maxLines`, `onsend(text)`. For the chat's additions it takes `stop` (`{label, onstop}`, which replaces the send act with
     `act-stop` while set and leaves the field enabled) and two snippets, `tools` before the send act and `footer` under the field (the counter, the system's dictation hint). It owns no draft, counter or reference list.
   - `FormStatus.svelte`: the result line of a form; props `message`, `tone` (`neutral`, `success`, `danger`) and `id`. The `aria-live="polite"` region is always in the document, empty when there is no message,
     so a first message is announced. The danger tone has an icon and its sentence; colour never carries it alone.
   - New catalogue keys only where a component needs a text of its own; the form's words come from the consumer.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `Compose.test.ts`: send on `Enter`; newline on `Shift+Enter`; no send during composition; growth to `maxLines` then scroll; the hidden label; the disabled reason; the `stop` swap with the field
     still enabled and focused; both snippets render. Fails on a revert. Evidence order: `test-first`.
   - `FormStatus.test.ts`: the region exists when empty, the first message is inside a live region that was already in the document, the danger tone has icon and text. Evidence order: `test-first`.
 - Implements: `components-fields.adoc` § `#compose`, `#field-set`, `#validation`; `chat.adoc` § `#composer`.
5. **Fixtures and browser evidence**
 - What:
   - Fixtures of `KindMark` (seven kinds, word hidden and shown), `Link` (three looks), `ExternalLink`, `PrChip` (with and without an address), `Compose` (plain, with `stop`, with both snippets) and `FormStatus`
     (three tones) on `routes/dev/components/+page.svelte` in both themes and both locales; `+page.test.ts` follows.
   - Check on the dev page that every tint is visible on the six grounds in both themes.
 - Persona: plan-marshall:persona-integration-tester
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - Browser check (Playwright MCP) of `/dev/components` at 1440×900 and 390×844, `de` and `en`, both themes; screenshots recorded (evidence order: `none`).
   - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); a confirmed `0` or every open issue dismissed (evidence order: `none`).

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`): `./mvnw -pl pm-web-app verify`, `./mvnw -pl pm-web-app verify -Ppre-commit` (review every rewrite), `./mvnw -pl pm-api verify`,
  and at the head `PM_MOCK_WEB_PORT=<mock port> PM_WEB_PORT=<app port> ./mvnw -pl pm-web-e2e verify -Pe2e`.
- No new dependency or plugin. Web standards of `CLAUDE.md`: ES modules, flat ESLint, no `innerHTML`, `outerHTML`, `insertAdjacentHTML` or `{@html}` with runtime or model text, no timer waits in tests,
  the npm scripts `lint`, `format`, `test`, `test:ci-strict`, `quality` stay; no static `style` attribute (`lib/csp.test.ts`); frontend tests on Vitest with Testing Library.
- Every component obeys `components.adoc` § `#rules` and § `#new-component` (picture exists, test beside it, fixture on `/dev/components`). Every new text comes from the catalogue in English and German.
  The text guard baseline does not grow. Java tests: JUnit 5, input from `Generators`, no Mockito.
- The design documents are updated in the same pull request wherever a deliverable says so.
- Ports of this plan: mock web port 21470, app port 21471 (`20000 + 10 × 147`) (`AGENTS.md` § Ports). Other plans run at the same time: never stop or reuse a process this worktree did not start.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.
- Every `test-first` clause cites a FAILING and a PASSING run of the named test; every `mutation-after` clause cites the reverted code and the failing assertion from a log quoted in the pull request body.
- **The merge rule:**
  - At least one CodeRabbit review of some head with its findings handled; every review BODY (nitpicks and outside-diff sections included) answered where the reviewer reads it;
    every thread replied and resolved. If CodeRabbit refuses on quota, stop and ask the operator.
  - The pull request body states `unresolved_review_threads=N` and `review_body_findings_total/handled`, read through `ci pr comments`.
  - The pull request is merged only on the operator's word. Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`. The pull request body carries no generator footer.
- ZERO Sonar findings (the operator, 2026-11-05): the pull request merges with 0 open new-code issues, every open issue fixed in a commit of THIS pull request. State `sonar_open_new_issues=N` in the landing message.
  A dismissal is made only on the operator's selection through `sonar_rest transition`; the lane never records one of its own.

## Claim Labels

- OBSERVED: none of `KindMark`, `Link`, `ExternalLink`, `PrChip`, `Compose`, `FormStatus` exists — absence read in `pm-web-app/src/lib/components/` at `1cc0e10e`
- OBSERVED: `pm-web-app/src/lib/icons/ui/` holds `kind-decision`, `kind-epic`, `kind-plan`, `kind-report`, `kind-task`, `act-external` and no `kind-project`, `kind-side-job`, `kind-machine`, `act-send`, `act-stop` — read at the directory
- OBSERVED: `tokens.css` declares `--color-epic` (lines 89, 157, 193, 228) with `--color-epic-subtle` and `--color-epic-text` (125, 126), and no `--color-kind-*`, `--layer-*` or `--control-*` — read at `pm-web-app/src/styles/tokens.css`
- OBSERVED: the only `.svelte` reader of `--color-epic` is `OverviewView.svelte` — read by `git grep -l color-epic -- pm-web-app/src`
- OBSERVED: `common.en.properties` has no `common.kind.*`, `common.link.*`, `common.pull_request.*` key; `overview.kind.*` and `overview.find.kind.*` exist — read at `pm-api/src/main/resources/messages/`
- OBSERVED: `components.adoc` § `#link` says a link is "never drawn as a button" and lists the props `href` and `label` only, while `chat.adoc:162` and `components-chat.adoc:66` require a `Link` in the look of a secondary button and `chat.adoc:135` a `Link` in the chip look — read in the three documents
- OBSERVED: `Button.svelte` takes an `icon` prop and `base.css:96` defines `.sr-only` — read at both files
- OBSERVED (settled 2026-11-11, #414 body): the S12 finding of PLAN-130 deliverable 5 changes nothing in the three new icon names; `kind-plan.svg` unchanged at `078b2ec5`
- HYPOTHESIS: the composer of PLAN-135 deliverable 4 needs nothing of `Compose` beyond the props, `stop`, `tools` and `footer` named here — confirm/refute at `.plan/orchestrator/ui-improvements/plans/PLAN-135-web-chat-core.md` § deliverable 4 (verify-at-outline)
- HYPOTHESIS: the UI icon contract (grid, stroke) is stated in `workflow-visual-language.adoc` — confirm/refute at `doc/design/web-app/workflow-visual-language.adoc` § icon contract (verify-at-outline)
- Verify-first clause: re-read every named file at the current HEAD; settle the `--color-epic` reader count before choosing between removing the tokens and keeping aliases. If the operator rules D-1 otherwise, deliverable 3 is re-scoped before outline.

## Expected Surface

- OBSERVED: `pm-web-app/src/styles/tokens.css` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/styles/tokens.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/icons/uiIcons.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/icons/uiIcons.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/icons/ui/kind-plan.svg` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/icons/ui/kind-project.svg` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/icons/ui/kind-side-job.svg` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/icons/ui/kind-machine.svg` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/icons/ui/act-send.svg` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/icons/ui/act-stop.svg` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/KindMark.svelte` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/KindMark.test.ts` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/Link.svelte` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/Link.test.ts` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/ExternalLink.svelte` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/ExternalLink.test.ts` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/PrChip.svelte` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/PrChip.test.ts` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/Compose.svelte` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/Compose.test.ts` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/FormStatus.svelte` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/FormStatus.test.ts` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/views/OverviewView.svelte` — present at HEAD 1cc0e10e (the token name only)
- OBSERVED: `pm-web-app/src/routes/dev/components/+page.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/routes/dev/components/+page.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/i18n/catalogue.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/common.en.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/common.de.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/message/MessageCatalogueTest.java` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/web-app/components.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/web-app/components-tags.adoc` — present at HEAD 1cc0e10e

## Dependencies and Sequencing

- Depends on: `PLAN-45` landing (running; it edits `routes/dev/components/+page.svelte`, `common.*.properties`, `catalogue.test.ts` and the whole `views/` directory). `PLAN-130` first: its mascot tokens are read by the tint test and its S12 finding settles `kind-plan`.
  If the operator wants this plan first, the tint test uses the literals `#E17512` and `#F2B630` and PLAN-130 switches the source later.
- Overlaps with:
  - `PLAN-45` (running): the dev page, `common.*`, `catalogue.test.ts`, `OverviewView.svelte`.
  - `PLAN-130`, `PLAN-131` (staged): `tokens.css`, `tokens.test.ts` and the dev page.
  - `PLAN-129` (staged): `routes/dev/components/+page.test.ts`; land it first.
  - `PLAN-136` (parked): `tokens.css`, `tokens.test.ts`, the dev page.
  - `PLAN-70`, `PLAN-72` (parked): `tokens.css`.
  - `PLAN-52` (parked): `OverviewView.svelte`, where `KindMark` replaces the epic glyph in its own plan.
- Adjacent to: `Breadcrumb` and the up link (D2 puts `KindMark` there); `Button.svelte` (read, not changed, here); the brand pieces (`BrandMark`, `WaitingMark`, PLAN-130).
- Unblocks: `PLAN-135` (chat core, then 136 and 137). Partly 49, 51, 52, 55, 58, 59, 60, 62 (they use `KindMark`, `Link`, `PrChip`, `Compose`, `FormStatus`).

## Re-Grounding at Restart

Re-read every file above at the then-current HEAD (grounded at `1cc0e10e`). Run `corpus surfaces` against everything that landed since (PLAN-45, PLAN-129, PLAN-130 first). Settle each HYPOTHESIS.
Re-check that the D1 documents are still `AGREED`, that `components-tags.adoc` § `#kind-tints` still carries the values above, and the operator's answer to D-1.

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-147-web-foundation-kind-links-and-chat-blocks.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message.
The orchestrator owns every other ledger write. The qualifiers of the inbox exception, and the sole sanctioned write mechanism, are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
