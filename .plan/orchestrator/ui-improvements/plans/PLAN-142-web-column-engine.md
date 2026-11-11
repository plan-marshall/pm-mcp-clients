# PLAN-142: Web: The Column Engine, the Column Window and the Side Column (D2 Navigation Engine, Part 1)

epic: ui-improvements
workstream: WS-02

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Staged on the operator's decision of
> 2026-11-10: the D2 navigation engine is split into three parts (this is the first; addresses are `PLAN-143`, the shell and
> the chain are `PLAN-144`). The engine together with PLAN-144 SUPERSEDES PLAN-83 (the orchestrator transitions its row). This
> spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

D2 (PLAN-113, merged `95df75bb`, AGREED) designed navigation as a chain of levels shown as a window of columns. The code
still has `TriagePage` as a 40/60 split with a viewport breakpoint at 1099 px (its only user is `ReportsView`), `ObjectPage`
with a `path` slot and a 1440 px cap, and no side column on any template. This plan builds the engine the design specifies:
the column window as one tested function of the container width, `TriagePage` as the column engine, the gap strip with its
peek, the focus and the live announcement, `ObjectPage` as the object column, and the side column on `TriagePage` and
`WizardPage` for the chat (D6: `ObjectPage` has no side column of its own). The address scheme is PLAN-143 and Up, the breadcrumb
and the shell are PLAN-144.

## Deliverables

The documents are the specification; read them first: `doc/design/web-app/navigation.adoc` (#levels, #column-types, #missing,
#window, #width-table, #gap-strip, #keys, #phone, #side-columns, #texts), `doc/design/web-app/components.adoc` (#triage-page,
#object-page, #templates), `doc/design/web-app/view-density-and-filtering.adoc` (#templates), `doc/design/web-app/README.adoc`
(#grid). D6 merged (#403, `8568b56e`) and settled the side column's scope (Q5): no interim slot in the chat plans; this plan
delivers `side`, `sideOpen` and `onHideSide` on `TriagePage` and `WizardPage` only, with the API fixed in `components.adoc#triage-page`
(:374) and `#templates` (:361); `ObjectPage` has no side (`components.adoc#object-page` :381).

1. **The column window as one function**
 - What:
   - A pure module `pm-web-app/src/lib/templates/columnWindow.ts` computes, from the container's content width `W`, the chain
     depth, the hidden levels, the side column state and a peek: the number of columns (1 to 4), the widths of the list columns
     and the object column, the strip (`none`, `names` at `136`, `marks` at `44`) and the side column width (`0`, `400`, `480`)
     and whether the side column takes the space.
   - The rule is the one of `navigation.adoc#window`: the largest `n` from 2 to 4 for which `(n − 1) × 220 + 480 + 136 ≤ W`;
     else two with the strip of marks where `220 + 480 + 44 ≤ W`; else one column and no strip; list columns share the room left
     and never exceed `320`; the object column takes the rest; with no level hidden there is no strip. `W` is the container less
     the page inset (`24` each side, `12` below `480`).
   - The side column: `480`, or `400` where `480` would cost the window a column (or would drop a wizard's content below `480`);
     where the window cannot show the object and the side column side by side the side column takes the space. `W` is the
     template's container, never the viewport (D6).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - `columnWindow.test.ts` pins every cell of both tables (`navigation.adoc:118-155` and `:286-308` at `4a84f727`: viewports `360`, `768`, `1024`,
     `1280`, `1440`, `1920`, rail expanded `232` and collapsed `64`, chat closed and open), the boundaries one pixel either side
     (`W` `743/744`, `835/836`), the `320` cap and the one-column case; reverting a constant fails it. Evidence order: `test-first`.
2. **`TriagePage` as the column engine**
 - What:
   - `TriagePage.svelte` takes `levels` (`key`, `kind`, `title`, `href`, `state`), `label`, `side`, `sideOpen` and `onHideSide`
     (deliverable 4), one snippet per level for its column's body and one for the object column. It measures its own container with a `ResizeObserver` (not the viewport)
     and computes again on every change of the container's width.
   - It renders only the columns of the window: a column out of the window is not rendered, not only moved out of sight. Each
     column scrolls on its own and the page never scrolls sideways. Widths are applied with `style:` directives (the CSSOM), never
     a static `style` attribute (`csp.test.ts`).
   - A list column and the object column are the two types of `navigation.adoc#column-types`; a list with nothing selected,
     rightmost, takes the object column's width. A selection the list does not hold is a pinned row with its reason
     (`web.nav.pinned.ended|read|resolved|filtered|page`).
   - The `filters` slot, the `selected` and `hasFilters` props, the `max-width: 1099px` query and the 40/60 grid go.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `TriagePage.test.ts` (with a `ResizeObserver` stub) shows the window following the container width, a hidden column absent
     from the DOM, the object column at least `480`, the pinned row with its reason, and no horizontal page overflow. It fails on
     a revert. Evidence order: `test-first`.
   - `pageWidth.test.ts` and the template fixtures follow the new props. Evidence order: `test-first`.
3. **The gap strip, the peek, the focus and the keys**
 - What:
   - The gap strip at the left edge stacks the hidden levels as `KindMark`s with their names visible, a group (`role="group"`,
     `web.nav.strip.label`) of buttons whose accessible name is kind and name. With `marks` only, the last button, _Names_
     (`web.nav.strip.names`), opens a popover with the names, one step each.
   - Selecting a mark is a peek: the window starts at that level, the rightmost visible column stretches, a strip of marks at the
     right edge (`web.nav.strip.return`) returns it; a peek changes no selection, no address and no history; the next selection
     ends it. With one column there is no strip.
   - After every shift the focus moves to the heading of the column now in view and the frame's polite live region announces it
     (`web.nav.announce`); the focus never falls to the page; shifts slide for `--duration-base` and not at all under reduced
     motion.
   - Keys: `Ctrl+←`/`→` and `[`/`]` (not while the focus is in a field) move the focus to the neighbour column and shift the
     window where it is hidden. Plain arrows, `j`/`k` and `Enter` stay with the widgets and the views.
   - Texts: `web.nav.strip.label|names|mark.title|return`, `web.nav.pinned.*`, `web.nav.announce` in
     `pm-api/src/main/resources/messages/common.{en,de}.properties`.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `GapStrip.test.ts` and `TriagePage.test.ts`: names versus marks, the popover, the peek and its return, the focus after a
     shift, the announcement text, the keys with and without a focused field, reduced motion (no transition). Evidence order:
     `test-first`.
4. **The object column, and the side column on `TriagePage` and `WizardPage`**
 - What:
   - `ObjectPage.svelte` gets `compact` (the head of a list column: `KindMark`, title, state word, the main act as a secondary
     `Button`), loses the `path` slot, and keeps its `1440 px` cap only on a page of its own (no cap inside a column). A slot `up`
     in the head of the first visible column is left for PLAN-144. It has **no side column of its own**: inside the engine the
     side column is the engine's (D6 Q5; `components.adoc#object-page` :381).
   - A side column on `TriagePage` and `WizardPage` only, with the same three props on both (`components.adoc#triage-page` :374):
     `side` (the snippet of the side column, the chat), `sideOpen` (whether it is shown; the view passes it from `chat=open`) and
     `onHideSide` (called by _Hide the chat_ and `Escape`; the view replaces the address). Docked right of the object column, or of
     the wizard's content; never shown by the gap strip; own scroll. Its width is `480`, or `400` where `480` would cost the window a
     column or would drop a wizard's content below `480`; both, and whether the object (or the wizard's content) and the side column
     fit side by side, are computed from the width of the template's **container**, never from the viewport
     (`navigation.adoc#side-columns` :275-284). Where they do not fit, the side column takes the space and the object becomes a hidden
     level; with one column there is no strip, and `onHideSide` returns to the object or the wizard. On a phone (below `768 px`) it
     takes the whole screen, the bottom bar included. The template draws no chat; its content is the chat plans' (`PLAN-135..137`),
     which build no slot of their own.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `ObjectPage.test.ts` shows `compact`, no `path` slot and no side prop. `WizardPage.test.ts` and `TriagePage.test.ts` (with a
     `ResizeObserver` stub on the container) show the side column at `480` and `400` by container width, the wizard's content kept at
     `480` or the side column taking the space, `sideOpen` false rendering no side column, the hide callback on _Hide the chat_ and on
     `Escape`, and the phone case covering the bottom bar. Evidence order: `test-first`.
5. **Migration, fixtures and browser evidence**
 - What:
   - `ReportsView.svelte` (the only consumer) moves to the new `TriagePage` props: the list as the list column and the report's
     detail as the object column; its addressing (`?plan=`) is unchanged here (PLAN-143 changes it). Update the template fixtures.
   - A fixture on `routes/dev/components/+page.svelte` with a synthetic five-level chain, the strip, a peek and the side
     column, in both themes and both locales.
   - A new `pm-web-e2e/tests/column-window.spec.js` on the mock: the window at `1024`, `1280`, `1440`, `1920` px with the rail
     expanded and collapsed, and the phone at `390`, reading the container widths from the DOM.
 - Persona: plan-marshall:persona-integration-tester
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - `column-window.spec.js`, `reports.spec.js` and `mobile.spec.js` pass with `PM_MOCK_WEB_PORT` and `PM_WEB_PORT` set; they wait on
     the layout's own condition, never on time. Evidence order: `test-first`.
   - Browser check (Playwright MCP) on `reports-and-system` and the `/dev/components` fixture at `1280` and `1440` px and on the
     phone, German and English, light and dark; screenshots recorded. Evidence order: `none`.
   - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for
     a failed read, never `0`); a confirmed `0` or every open issue dismissed. Evidence order: `none`.

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`): `./mvnw -pl pm-api,pm-web-app verify`, `./mvnw -pl pm-api,pm-web-app verify -Ppre-commit` (review every rewrite),
  `PM_MOCK_WEB_PORT=<mock port> PM_WEB_PORT=<app port> ./mvnw -pl pm-web-e2e verify -Pe2e` at the head.
- No new dependency or plugin. Web standards of `CLAUDE.md` (ES modules, flat ESLint, no `innerHTML`/`{@html}`, no timer waits in
  tests, the npm scripts stay); no static `style` attribute (CSP; `style:` directives are allowed); every new text from the
  catalogue in English and German; the text guard baseline does not grow. The closed vocabulary of `components.adoc`: a column
  head uses `KindMark`, `Button`, `IconButton`; no new component outside the templates and the strip.
- Frontend tests on Vitest with Testing Library (`pm-dev-frontend:jest-testing`).
- Ports of this plan: mock web port 21420, app port 21421 (`20000 + 10 × 142`) (`AGENTS.md` § Ports). Other plans run at the same
  time: never stop or reuse a process this worktree did not start.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.
- The merge rule: the merge question is asked only when every review thread has a reply and is resolved and every review body is
  answered, and some head has had one CodeRabbit review. The pull request body states thread count, resolved count and review-body
  items from `ci pr comments`, never from memory. Merge only on the operator's word. Commits end with
  `Co-Authored-By: plan-marshall <noreply@cuioss.de>`; the pull request body carries no generator footer. If CodeRabbit refuses on
  quota, stop and ask the operator.
- ZERO Sonar findings (the operator, 2026-11-05): the pull request merges with 0 open new-code issues (SonarCloud project
  `plan-marshall_pm-mcp-clients`), every open issue fixed in a commit of THIS pull request. State `sonar_open_new_issues=N` in the
  landing message. A dismissal needs the operator's word.

## Claim Labels

- OBSERVED: `TriagePage` has the props `selected`, `hasFilters`, the slots `filters`, `list`, `detail`, a `2fr 3fr` grid and a `max-width: 1099px` query — read at `pm-web-app/src/lib/templates/TriagePage.svelte:12-77`
- OBSERVED: `ReportsView.svelte` is the only production user of `TriagePage` — read at `pm-web-app/src/lib/views/ReportsView.svelte:13,855`; `git grep -l TriagePage -- pm-web-app/src` lists fixtures and tests besides
- OBSERVED: `ObjectPage` has the slots `kind`, `path`, `title`, `id`, `state`, `primary`, `secondary`, `overflow`, `attention`, `tabs`, a `max-width: 1440px`, no `compact` and no side slot — read at `pm-web-app/src/lib/templates/ObjectPage.svelte:18-60`
- OBSERVED: `WizardPage` is `max-width: 720px` with no side slot, and no production view uses `ObjectPage` or `WizardPage` (only fixtures and tests) — read at `pm-web-app/src/lib/templates/WizardPage.svelte`; `git grep -n "ObjectPage\|WizardPage" -- pm-web-app/src/lib pm-web-app/src/routes pm-web-app/src/test`
- OBSERVED: no `ResizeObserver` is used outside tests; `matchMedia` is — read by `git grep -n "ResizeObserver\|matchMedia" -- pm-web-app/src`
- OBSERVED: a static `style` attribute is blocked by the policy and a bound `style=` or `style:` directive is allowed — read at `pm-web-app/src/lib/csp.test.ts:1-60`
- OBSERVED: the frame has a polite live region — read at `pm-web-app/src/routes/+layout.svelte:349`
- OBSERVED: `KindMark`, `Link`, `RowLink` do not exist under `pm-web-app/src/lib/components/` — read by `git ls-files pm-web-app/src/lib/components`
- OBSERVED: the throw-away column-window prototype of D2 (`pm-web-app/src/routes/dev/columns/`) is not in the repository — read by `git ls-files pm-web-app/src/routes/dev`
- OBSERVED: every row of both width tables follows from the window rule with `W` = viewport − rail − `48` (`12 × 2` below `480`) — checked by hand against `doc/design/web-app/navigation.adoc:86-155,279-301`; `columnWindow.test.ts` is the machine confirmation
- HYPOTHESIS: the frame's live region can carry the announcement through the runtime store (`quietReportNotice`, `quietDecisionNotice` are its inputs today) or through a region of the engine's own — confirm/refute at `pm-web-app/src/routes/+layout.svelte` § `aria-live` (verify-at-outline)
- HYPOTHESIS: `@testing-library/svelte` with a `ResizeObserver` stub in `setupTests.ts` is enough to test the window — confirm/refute at `pm-web-app/src/setupTests.ts` (verify-at-outline)
- OBSERVED: D6 fixed the side-column API on `TriagePage` and `WizardPage` (`side`, `sideOpen`, `onHideSide`) and took it off `ObjectPage`; the chat plans build no slot of their own — read at `doc/design/web-app/components.adoc:361,374,381`, `doc/design/web-app/navigation.adoc:275-284,383-418` (`4a84f727`, D6 merged `8568b56e`)
- Verify-first clause: re-read `navigation.adoc#side-columns` and `components.adoc#triage-page` at the then-current HEAD before scoping deliverable 4; the D6 API above is binding (no interim slot exists in the chat plans). If `KindMark` and `Link` do not exist yet, stop and loop back.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/templates/TriagePage.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/templates/TriagePage.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/templates/ObjectPage.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/templates/ObjectPage.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/templates/WizardPage.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/templates/WizardPage.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/templates/pageWidth.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/templates/columnWindow.ts` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/templates/columnWindow.test.ts` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/templates/GapStrip.svelte` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/templates/GapStrip.test.ts` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/test/fixtures/TriagePageFixture.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/test/fixtures/TriagePageNoFiltersFixture.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/test/fixtures/ObjectPageFixture.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/test/fixtures/WizardPageFixture.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/ReportsView.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/ReportsView.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/routes/dev/components/+page.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/routes/dev/components/+page.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/setupTests.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/common.en.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/common.de.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/column-window.spec.js` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-e2e/tests/reports.spec.js` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/mobile.spec.js` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/showroom.spec.js` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/web-app/components.adoc` — present at HEAD 1cc0e10e (only the "Source and surface" lines of `TriagePage` and `ObjectPage`, if the code's props differ)

## Dependencies and Sequencing

- Depends on: the D1 foundation part that delivers `KindMark` and `Link` (the strip, the compact head); `PLAN-45` landed (it holds `views/`, `routes/dev/components/+page.svelte`); `PLAN-129` landed (`ReportsView`). The D6 ruling on the side column is merged (#403, `8568b56e`): satisfied.
- Overlaps with:
  - `PLAN-45` (running): `routes/dev/components/+page.svelte`, the `views/` directory.
  - `PLAN-129` (staged): `ReportsView.svelte`, `ReportsView.test.ts`, `reports.spec.js`. It lands first.
  - `PLAN-130` (staged): `routes/dev/components/+page.svelte`.
  - `PLAN-143`: `ReportsView.svelte`. Series: never at the same time.
  - `PLAN-72`, `PLAN-73` (parked): `components/` and `tokens.css`; they follow.
- Adjacent to: `PLAN-144` (Up, the breadcrumb and the shell fill the `up` slot this plan leaves); `PLAN-135..137` (the chat fills the side column through `side`, `sideOpen`, `onHideSide`; they wait for this plan); `PLAN-52`, `55`, `58`, `59`, `62` (they place their views on the engine after PLAN-144).

## Re-Grounding at Restart

Re-read every file above at the then-current HEAD (this spec was grounded at `1cc0e10e`). Re-run `corpus surfaces` against everything that landed since (PLAN-45, PLAN-129, PLAN-130 and the D1 foundation first). Settle each HYPOTHESIS. The D6 documents are merged (#403, `8568b56e`); re-check that `navigation.adoc` still carries the width tables unchanged (at `4a84f727`: `:118-155` and `:286-308`).

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-142-web-column-engine.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger write. The session reports its outcome through its PR and its inbox message (`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary).

## Staging Note 2026-11-10

The packages this spec names by placeholder were staged on 2026-11-10: the D1 foundation part with KindMark and Link is PLAN-147; the tags and RowLink are PLAN-148.
