# PLAN-166: Web: Fixes of the Column Engine Found by the PLAN-142 Landing Analysis

epic: ui-improvements
workstream: WS-02

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Staged on the operator's word of 2026-11-15
> (`analyze` of `rb-142-web-column-engine-001.md`: "Stage a fix plan, emit later"). Defects L3-1 to L3-7 of `landings/PLAN-142.md`, re-read on the merged
> tree `0b9402ec` (#442). This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief. Held, not emitted: it goes
> before `PLAN-144` and before the chat plans `PLAN-135` to `PLAN-137`, which build on the engine.

## Objective

PLAN-142 shipped the column engine (`columnWindow.ts`, `TriagePage`, `GapStrip`, `SideColumn`, the side column of `WizardPage`). A three-layer read of the merge
found two defects that lose state or break the window and five smaller ones, none caught by a test: after a peek, a key move to the object returns a window with no
strip and an object 136 px too wide; a `{#key}` on the window's start remounts every column and the chat on any shift; a stale peek survives a resize; no focus
handling where the side column takes the space; Escape means different things on the two templates; the showroom fixture is unusable on a phone; the phone overlay
sits in an animated, transformed ancestor. This plan fixes each with a test that fails before the fix. Behaviour that is not named here does not move: no text, no
width of the two tables, no prop of the three templates.

## Deliverables

Read first: `landings/PLAN-142.md` (the Layer 3 table, L3-1 to L3-7, is the defect list; the leaf's findings were re-read on `0b9402ec`), `doc/design/web-app/navigation.adoc`
(`#window`, `#gap-strip`, `#keys`, `#phone`, `#side-columns`), `pm-web-app/src/lib/templates/columnWindow.ts`, `TriagePage.svelte`, `WizardPage.svelte`, `SideColumn.svelte`.

1. **A key move after a peek keeps the window honest (L3-1, L3-3)**
 - What:
   - `moveFocus` shifts the window right by `peekAt(next - columns + 1, next)`. When that leaves no level to the right of the window, the result is not a peek: it is the
     object's own window, with its strip and its width. Make `navWindow` (or `peekAt`) return the unpeeked window for a peek that ends at the object, so the strip of hidden left
     levels and the object width are those of the unpeeked window, and `returnStrip` stays false.
   - A peek is kept in `peek` state when a resize gives one column (`navWindow` yields `peeked: null`); clear it there, so widening the container later does not jump back to a stale peek.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `columnWindow.test.ts` and `TriagePage.test.ts` drive a five-level chain at a three-column container: peek the lens, press `]` to the object, and read the strip (hidden levels 0-1, `names`)
     and the object width equal to the unpeeked window's; fails at `0b9402ec` (evidence order: `test-first`).
   - A test peeks, narrows to one column, widens again, and reads the window ending at the object, not at the stale peek; fails at `0b9402ec` (evidence order: `test-first`).
2. **A shift no longer remounts the columns (L3-2, L3-7)**
 - What:
   - `{#key layout.start}` (`TriagePage.svelte:341,408`) remounts every column, the gap strip and the side column on any change of `start`. Replay the slide without remounting: a class or
     attribute toggled for one animation, or the Web Animations API on the window, so a column's scroll position, a chat draft in the side snippet and the focused control survive a rail
     toggle, a resize across a threshold and a peek.
   - The animation puts `transform` on `.triage-page__window`, the containing block of the phone's `position: fixed` overlay for `--duration-base`. Animate so that no ancestor of the overlay
     carries a transform while it is open (animate the columns, not the group that holds the side column), keeping "no transition under reduced motion".
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `TriagePage.test.ts` types into a field of the side snippet, toggles `start` (a resize across a threshold, with the `ResizeObserver` stub), and reads the same field element with its value; and a
     list column's `scrollTop` survives the same toggle; fails at `0b9402ec` (evidence order: `test-first`).
   - A test at a phone width opens the side column and reads that no ancestor of the overlay has a computed `transform` other than `none`, and that the reduced-motion case still runs no transition
     (evidence order: `test-first`).
3. **Focus and Escape agree between the templates (L3-4, L3-5)**
 - What:
   - Where the side column takes the space (container 744 to 927 px, or the phone) and every column unmounts, `TriagePage` moves the focus into the side column and returns it to the control that opened
     it on hide, as `WizardPage` already does (`WizardPage.svelte:79-99`, to be re-read). The focus never falls to the body.
   - Escape hides the chat in one way on both templates: from a listener inside the template's own window group, ignoring an Escape the page's open dialog or menu already handled
     (`defaultPrevented`). `WizardPage` moves off its window-level listener (`:149`).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `TriagePage.test.ts` focuses a list row, opens the chat at a container where it takes the space, and reads the focus inside the side column, then hides it and reads the focus on the opener;
     fails at `0b9402ec` (evidence order: `test-first`).
   - `WizardPage.test.ts` and `TriagePage.test.ts` send an Escape that an open `ConfirmDialog` already handled and read that the chat stays open; fails at `0b9402ec` for the wizard
     (evidence order: `test-first`).
4. **The showroom fixture works on a phone, and the build's format check passes (L3-6, and the two fused lines)**
 - What:
   - `ColumnEngineFixture.svelte` hard-codes `sideOpen` and passes no `onHideSide`. Give it a state and a hide act, so Hide the chat works on a phone and `/dev/components` is usable there.
   - Run `format:check` of `pm-web-app` and `pm-web-e2e` and fix the two statements fused onto the `async () => {` line (`TriagePage.test.ts:355`, `column-window.spec.js:163`); move the
     `svelte-ignore` at `TriagePage.svelte:340` onto the element it covers and settle the `async` callback without `await` at `GapStrip.svelte:57` (both only checked by a run: see Claim Labels).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - A test of the fixture at a phone width presses Hide the chat and reads the side column gone; fails at `0b9402ec` (evidence order: `test-first`).
   - `format:check` is clean in both web modules (evidence order: `none`).
5. **Browser evidence for the strip, the peek and three or four columns**
 - What: `column-window.spec.js` models only the two-level reports chain, so the strip, a peek and the three- and four-column cells have no browser evidence. Add a case on the `/dev/components`
   fixture (the synthetic five-level chain) at 1280 and 1440 px: the window's column count and widths read from the DOM, a peek by the strip and by `[`, and the return. It waits on the layout's own condition,
   never on time.
 - Persona: plan-marshall:persona-integration-tester
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - The new case passes with `PM_MOCK_WEB_PORT` and `PM_WEB_PORT` set, and reverting deliverable 1's fix makes it fail (evidence order: `test-first`).

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`): `./mvnw -pl pm-api,pm-web-app verify`, `./mvnw -pl pm-api,pm-web-app verify -Ppre-commit` (review every rewrite),
  `PM_MOCK_WEB_PORT=<mock port> PM_WEB_PORT=<app port> ./mvnw -pl pm-web-e2e verify -Pe2e` at the head. The first run of the Playwright suite is at the start: `designer.spec.js` flaked in the
  PLAN-142 lane (three of eight runs, unattributed); record whether it still flakes, do not fix it here unless the cause is in this diff.
- No new dependency or plugin. Web standards of `CLAUDE.md` (ES modules, flat ESLint, no `innerHTML`/`{@html}`, no timer waits in tests, the npm scripts stay); no static `style` attribute (CSP;
  `style:` directives are allowed); the text guard baseline does not grow; no new text unless a defect needs one (then English and German).
- The three templates keep their props (`levels`, `label`, `column`, `object`, `up`, `side`, `sideOpen`, `onHideSide`, `pinned`); the two width tables of `navigation.adoc` still hold cell for cell
  (`columnWindow.test.ts` stays green unchanged except for the new cases).
- Ports of this plan: mock web port 21660, app port 21661 (`20000 + 10 × 166`) (`AGENTS.md` § Ports). Other plans run at the same time: never stop or reuse a process this worktree did not start.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.
- The merge rule: the run merges only when every review thread has a reply and is resolved and every review body is answered, and some head has had one CodeRabbit review. The pull request body states thread
  count, resolved count and review-body items from `ci pr comments`, never from memory. The run merges the pull request itself once these gates hold, without a further word of the operator
  (`CLAUDE.md`, Git Workflow). Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`; the pull request body carries no generator footer. If CodeRabbit refuses on quota, stop and ask the operator.
- ZERO Sonar findings (the operator, 2026-11-05): the pull request merges with 0 open new-code issues (SonarCloud project `plan-marshall_pm-mcp-clients`), every open issue fixed in a commit of THIS pull request.
  State `sonar_open_new_issues=N` in the landing message. A dismissal needs the operator's word.

## Claim Labels

- OBSERVED: after a peek, `moveFocus` calls `peekAt(next - layout.columns + 1, next)` (`TriagePage.svelte:296-299`), and `navWindow` returns `peek: true`, strip `none`, `returnStrip: false` when the peek leaves no level to the right (`columnWindow.ts:241-277`); no test reaches it (`TriagePage.test.ts:313-326` stops at the plan column) — read at those lines of `0b9402ec`
  - verdict: corroborated | checked_at: 0b9402ec | by: ui-improvements/analyze | rescoped: n/a | evidence: TriagePage.svelte:296-299 and columnWindow.ts:241-277 re-read on the merged tree
- OBSERVED: `{#key layout.start}` wraps the window group, the columns, the gap strip and the side column — read at `pm-web-app/src/lib/templates/TriagePage.svelte:341,408`
  - verdict: corroborated | checked_at: 0b9402ec | by: ui-improvements/analyze | rescoped: n/a | evidence: TriagePage.svelte:341 opens the key, :408 closes it, re-read
- OBSERVED: the peek is cleared only on a chain change (`TriagePage.svelte:179-189`), while `navWindow` yields `peeked: null` at one column (`columnWindow.ts:241-242`) — read at those lines
  - verdict: corroborated | checked_at: 0b9402ec | by: ui-improvements/analyze | rescoped: n/a | evidence: TriagePage.svelte:179-189 clears peek only on chain change; columnWindow.ts:241-242 re-read
- HYPOTHESIS: `WizardPage` moves the focus into the chat and back where `TriagePage` does not — confirm/refute at `pm-web-app/src/lib/templates/WizardPage.svelte:79-99` and `TriagePage.svelte` § the side column block `:399-406` (verify-at-outline; the `TriagePage` half was read: no focus call outside peek, return, key move and chain change)
- OBSERVED: `WizardPage` hides the chat from `<svelte:window on:keydown>` (`WizardPage.svelte:149`), `TriagePage` from `onkeydown` on its window group (`TriagePage.svelte:310-315,349`) — read at those lines
  - verdict: corroborated | checked_at: 0b9402ec | by: ui-improvements/analyze | rescoped: n/a | evidence: WizardPage.svelte svelte:window keydown re-read; TriagePage.svelte:310-315,349 re-read
- OBSERVED: `ColumnEngineFixture.svelte` passes `sideOpen` as a constant and no `onHideSide` — read at `pm-web-app/src/lib/dev/ColumnEngineFixture.svelte:54-62`
  - verdict: corroborated | checked_at: 0b9402ec | by: ui-improvements/analyze | rescoped: n/a | evidence: ColumnEngineFixture.svelte:54-62 sideOpen constant, no onHideSide, re-read
- OBSERVED: `.triage-page__window` animates `transform` (`TriagePage.svelte:433,470-479`) and `SideColumn.svelte` makes the phone overlay `position: fixed` (`:62-78`) inside it — read at those lines
  - verdict: corroborated | checked_at: 0b9402ec | by: ui-improvements/analyze | rescoped: n/a | evidence: TriagePage.svelte:433-437 animation and :470-479 keyframes transform; SideColumn.svelte:62-78 position fixed, re-read
- OBSERVED: `TriagePage.test.ts:355` and `column-window.spec.js:163` carry a statement fused onto the `async () => {` line — read at those lines of `0b9402ec`
  - verdict: corroborated | checked_at: 0b9402ec | by: ui-improvements/analyze | rescoped: n/a | evidence: TriagePage.test.ts:355 and column-window.spec.js:163 fused statements, re-read
- HYPOTHESIS: the `svelte-ignore` at `TriagePage.svelte:340` sits before `{#key}`, not before the element it should cover, and `GapStrip.svelte:57` has an `async` callback without `await` — confirm/refute by the Svelte lint and Sonar run (verify-at-outline; not shown by reading)
- HYPOTHESIS: `column-window.spec.js` models only a two-level chain (`expectedWindow`) — confirm/refute at `pm-web-e2e/tests/column-window.spec.js:21` (verify-at-outline; the leaf read it, the orchestrator did not)
- Verify-first clause: re-read the Layer 3 table of `landings/PLAN-142.md` and the cited lines at the then-current HEAD before scoping; a defect `PLAN-143`, `PLAN-149` or `PLAN-145` already changed is re-scoped, not re-fixed.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/templates/columnWindow.ts` — present at `0b9402ec`
- OBSERVED: `pm-web-app/src/lib/templates/columnWindow.test.ts` — present at `0b9402ec`
- OBSERVED: `pm-web-app/src/lib/templates/TriagePage.svelte` — present at `0b9402ec`
- OBSERVED: `pm-web-app/src/lib/templates/TriagePage.test.ts` — present at `0b9402ec`
- OBSERVED: `pm-web-app/src/lib/templates/WizardPage.svelte` — present at `0b9402ec`
- OBSERVED: `pm-web-app/src/lib/templates/WizardPage.test.ts` — present at `0b9402ec`
- OBSERVED: `pm-web-app/src/lib/templates/SideColumn.svelte` — present at `0b9402ec`
- OBSERVED: `pm-web-app/src/lib/templates/GapStrip.svelte` — present at `0b9402ec`
- OBSERVED: `pm-web-app/src/lib/dev/ColumnEngineFixture.svelte` — present at `0b9402ec`
- OBSERVED: `pm-web-e2e/tests/column-window.spec.js` — present at `0b9402ec`

## Dependencies and Sequencing

- Depends on: `PLAN-142` shipped (#442, `0b9402ec`). Nothing else.
- Goes before: `PLAN-144` (it also edits `TriagePage.svelte`, `TriagePage.test.ts` and `SideColumn.svelte` for the shell height and the modal boundary) and the chat plans `PLAN-135` to `PLAN-137`.
  Series with `PLAN-144`: never at the same time.
- Overlaps with: `PLAN-144` and `PLAN-135` on the three template files above (declared surfaces); no running plan (`PLAN-145`, `PLAN-149`, `PLAN-162`) declares any of them.
- Adjacent to: the phone overlay's modal boundary (`inert`, `aria-modal`, the layer above the bottom bar) stays with `PLAN-144`, which carries it; this plan only removes the transformed ancestor.

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-166-web-column-engine-fixes.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger write. The session reports its outcome through its PR and its inbox message (`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary).
