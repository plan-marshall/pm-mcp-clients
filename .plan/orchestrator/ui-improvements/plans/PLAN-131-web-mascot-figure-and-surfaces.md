# PLAN-131: Web: The Mascot Figure and its Surfaces (D3 Brand Assets, Part 2)

epic: ui-improvements
workstream: WS-02

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Drafted 2026-11-10 from the
> AGREED D3 documents. SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

After PLAN-130 (icon, tokens, WaitingMark), this plan delivers the mascot as illustration: the derived image files,
the `MascotFigure` component, `EmptyState` with `mascot` (replacing `brand`), and the calm surfaces that exist today —
the pairing page's hero, the revoked-device page, the two disconnected states (runtime not reachable, listener off).
The onboarding welcome and done figures and the five new empty states sit in views the parked plans redesign; they are
named below and are not placed here. Poses beyond the source figure are not part of this plan: each surface falls back to
the source figure until the operator accepts a pose.

## Deliverables

Specification: `doc/design/web-app/brand-mark-mascot.adoc` (§ `#surfaces`, `#assets`, `#poses`, `#pose-rules`,
`#implementation`), `doc/design/web-app/components-brand.adoc` (§ `#mascot-figure`), `doc/design/web-app/components.adoc`
(§ `#empty-state`), `doc/design/web-app/entry.adoc` (§ `#pairing`, `#revoked`, `#after-pairing`).

1. **The derived image files**
 - What:
   - From the source `plan-marshall/doc/resources/planmarshall.png` (repository `plan-marshall`, `1122 × 1402`, RGBA;
     checked out beside this repository), make once and commit under `pm-web-app/src/lib/assets/mascot/`:
     `mascot-full` and `mascot-bust`, each as WebP at `1×` and `2×` (full `360`/`720` px tall, bust `200`/`400` px), at most
     `60 KB` per file. The source PNG is never copied into the repository.
   - Imported through Vite so they are files of the app's own origin; `assetsInlineLimit: 0` stays (`vite.config.ts:124`).
   - Record how the files were made (tool, command, crop box) in `brand-mark-mascot.adoc` § `#assets` so the step can be
     repeated. Decisions O4 (the bust crop) and O5 (the tool) are the operator's; this plan follows them.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css
 - Done when:
   - A test asserts every file exists, is WebP, is at most 60 KB, and that no file of `pm-web-app/src` or `build` carries
     a `data:` URL for them (evidence order: `test-first`).
2. **`MascotFigure`**
 - What:
   - New `MascotFigure.svelte` with props `pose` (`welcome`, `done`, `all-clear`, `start-here`, `searching`,
     `access-ended`), `crop` (`full`, `bust`), `height`, `eager`, `entrance`, `src`. A surface names a pose, never a file:
     the piece draws the accepted pose's files, and the source figure's until a pose is accepted (all six today).
   - `srcset` `1×` and `2×`, `width` and `height` set, `loading="lazy"` and `decoding="async"` (`eager` for the pairing
     hero), `alt=""` and `aria-hidden="true"`, no `data:` URL, no `style` attribute.
   - Dark theme: a soft radial halo of `--color-bg-subtle` by a class of its own stylesheet; no filter recolours it.
   - `entrance`: one fade and rise of at most `300 ms`, none under reduced motion.
   - `src`: the disconnected states draw from the object URL the app holds; without it the piece draws nothing and keeps
     no space.
   - A fixture on `/dev/components` in both themes.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `MascotFigure.test.ts` covers both crops, the fallback to the source figure for every pose, `eager`, `entrance`
     with and without reduced motion, `src`, the absence of an inline `style` and of `{@html}` (evidence order: `test-first`).
3. **`EmptyState` with `mascot`**
 - What:
   - `EmptyState` gains `mascot` (a pose id, bust above the title, `120`–`160` px) and loses `brand` (design:
     `components.adoc` § `#empty-state`). The one call site today is `ReportsView.svelte:933` (verify): choose the
     pose by the design table (`all-clear` for "no unread reports").
   - The other four calm empty states ("nothing waits", "no running work", "no open decisions", "no projects yet") are in
     PLAN-49, 51, 52 and 59 (parked); each takes `mascot` when it is re-grounded.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - `EmptyState.test.ts` asserts `mascot` draws the bust above the title and that `brand` no longer exists
     (`git grep -n "brand" -- pm-web-app/src/lib/components/EmptyState.svelte` shows no prop; evidence order: `test-first`).
4. **The surfaces that exist**
 - What:
   - `PairingView`: the full figure as the hero beside the pairing card on a wide screen (`welcome`, `280`–`360` px tall,
     eager, `entrance`), hidden on a phone and where the free height is below the figure's; `BrandMark` `28` px beside the
     wordmark while the hero shows, `64` px where it is hidden. `RevokedView`: `access-ended`, `160`–`200` px, beside the
     text, quieter.
   - `ListenerOffView` and the runtime-not-reachable state: `searching`, `160`–`200` px, beside the text, from an object URL
     fetched once when the app starts and held (`blob:`), with no request of their own when the origin is lost; without it
     the state shows without its figure.
   - Never behind data and one figure per screen (`brand-mark-mascot.adoc` § `#pose-rules`).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - Tests assert each surface's pose, size and its hidden thresholds; a test simulates the lost origin and asserts the
     figure still draws from the held object URL, and that without it nothing is drawn (evidence order: `test-first`).
   - `pairing.spec.js` asserts the 28 px icon beside the hero and the 64 px icon where the hero is hidden (evidence order:
     `test-first`).
5. **The rail-foot bear (only if the operator decides O4 in its favour) and the browser evidence**
 - What:
   - The rail-foot bear as a class of `NavRail` with its asset and the hiding rules of § `#rail-foot` (collapsed rail,
     free height below `260` px, phone, while a figure or a mascot empty state shows), opacity `0.18` light and `0.12` dark,
     never moving. If O4 is decided against, this item is dropped and the plan says so in its PR.
   - Browser check in German and English, light and dark, at `1280` px and on the phone, for every surface; for the pairing
     hero (and the rail foot if built) also a desktop height below and above each threshold, rail expanded and collapsed.
 - Persona: plan-marshall:persona-integration-tester
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - Browser check (Playwright MCP) shows the pairing hero, the revoked page, the two disconnected states and the Reports
     empty state on `fully-onboarded`, `running-work` and the pairing scenario; screenshots recorded (evidence order: `none`).
   - The open-issue read at the pull request head is recorded (open Sonar issue count and rules left open; `unknown` for a
     failed read, never `0`); a confirmed `0` or every open issue dismissed (evidence order: `none`).

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`): `./mvnw -pl pm-web-app verify`, `-Ppre-commit` with every rewrite
  reviewed, `PM_MOCK_WEB_PORT=<mock port> PM_WEB_PORT=<app port> ./mvnw -pl pm-web-e2e verify -Pe2e`.
- No new dependency or plugin; the image files are made with a tool already on the machine (O5), never added to the build.
- Web standards of `CLAUDE.md` (no `innerHTML`/`{@html}`, no timer waits); CSP: `img-src 'self' blob:` only, no `data:` URL, no
  `style` attribute. Every new text from the catalogue in en and de.
- Ports: mock web port 21310 (`20000 + 10 × 131`), app port 21311 (`AGENTS.md` § Ports); never stop or reuse a process
  this worktree did not start.
- Write-set discipline: the main checkout is never edited; only the session's own worktree and branch.
- The merge rule: merge only when every thread has a reply and is resolved, every review body is answered and some head has had
  one CodeRabbit review; counts stated from `ci pr comments`; the run merges itself once the gates hold (no further operator word); commits end with
  `Co-Authored-By: plan-marshall <noreply@cuioss.de>`; no generator footer in the pull request body.
- ZERO Sonar findings (the operator, 2026-11-05): the pull request merges with 0 open new-code issues (SonarCloud project
  `plan-marshall_pm-mcp-clients`), every open issue fixed in a commit of THIS pull request. State `sonar_open_new_issues=N` in the
  landing message. A dismissal needs the operator's word.
- If CodeRabbit refuses on quota, stop and ask the operator: a lane's standing review authorization never replaces the one
  CodeRabbit review (PLAN-36 merged unreviewed; epic.md Open Defects).

## Claim Labels

- OBSERVED: the source figure exists at `/Users/oliver/git/plan-marshall/doc/resources/planmarshall.png` (repository `plan-marshall`, sibling checkout) and shows the clipboard overlapping the chest and the lower face (its clip starts below the chin) — read at `../plan-marshall/doc/resources/planmarshall.png`
- OBSERVED: `pm-web-app/src/lib/assets/` does not exist; no `MascotFigure.svelte` exists — absence read at `1cc0e10e`
- OBSERVED: `vite.config.ts` sets `assetsInlineLimit: 0` — read at `pm-web-app/vite.config.ts:124`
- OBSERVED: `EmptyState` has the prop `brand` and `ReportsView.svelte:933` is the only `EmptyState` call site under `pm-web-app/src/lib` — read at `pm-web-app/src/lib/components/EmptyState.svelte` and `git grep EmptyState -- pm-web-app/src/lib`
- OBSERVED: `PairingView.svelte`, `RevokedView.svelte` and `ListenerOffView.svelte` exist and show no figure — read at `pm-web-app/src/lib/views/` (the absence of any image there is read from the line counts 371, 95, 113 and a `git grep` for `<img` — verify at outline)
- HYPOTHESIS: the "runtime not reachable" state has its own view or branch that is not `ListenerOffView` — confirm/refute at `pm-web-app/src/lib/stores/runtime.ts` § connection state and `pm-web-app/src/routes/+layout.svelte` (verify-at-outline)
- HYPOTHESIS: the app can fetch and hold a blob URL at start without a store of its own (a start hook in `+layout.svelte` or `AppShell.svelte`) — confirm/refute at `pm-web-app/src/routes/+layout.svelte` § onMount (verify-at-outline)
- HYPOTHESIS: the CSP test `csp.test.ts` asserts `img-src 'self' blob:` and will need no change — confirm/refute at `pm-web-app/src/lib/csp.test.ts` § img-src (verify-at-outline)
- Verify-first clause: the bust's crop box and the rail-foot rule "cropped at the chest so the clipboard is hidden" cannot both hold for the source figure (O4); the operator's answer is read before scoping deliverables 1 and 5. The tool for WebP (O5) is settled before scoping deliverable 1.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/assets/mascot/` — absent at `1cc0e10e`; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/MascotFigure.svelte` — absent at `1cc0e10e`; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/MascotFigure.test.ts` — absent at `1cc0e10e`; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/EmptyState.svelte` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/src/lib/components/EmptyState.test.ts` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/src/lib/views/PairingView.svelte` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/src/lib/views/RevokedView.svelte` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/src/lib/views/ListenerOffView.svelte` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/src/lib/views/ReportsView.svelte` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/src/lib/shell/NavRail.svelte` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/src/routes/+layout.svelte` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/src/routes/dev/components/+page.svelte` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/vite.config.ts` — present at `1cc0e10e`
- OBSERVED: `pm-web-e2e/tests/pairing.spec.js` — present at `1cc0e10e`
- OBSERVED: `doc/design/web-app/brand-mark-mascot.adoc` — present at `1cc0e10e`; § `#assets` gains the recipe

## Dependencies and Sequencing

- Depends on: `PLAN-130` (the `BrandMark` API, the tokens); the operator's decisions O4 and O5.
- Overlaps with:
  - `PLAN-85` (parked): `PairingView`, `RevokedView`, `NavRail`; this plan lands first.
  - `PLAN-64`, `PLAN-66` (parked): `OnboardingView` (welcome and done figures are placed there when those plans are re-grounded, calling `MascotFigure`).
  - `PLAN-70` (parked): the rail-foot opacity check against the nearest label; it follows this plan.
  - `PLAN-49`, `PLAN-51`, `PLAN-52`, `PLAN-59` (parked): the four calm empty states take `mascot` at their own re-ground.
  - `PLAN-45` (running): `routes/+layout.svelte` and `routes/dev/components/+page.svelte`; this plan follows it.
- Adjacent to: the voice and chat packages (the mascot is never the chat partner, `chat.adoc` § `#waiting`).

## Re-Grounding at Restart

Re-read every file above at the then-current HEAD; settle the HYPOTHESES; re-run `corpus surfaces`; confirm the operator's answers to O4 and O5 and that the D3 documents are still `AGREED`.

## Hand-Off Command

Lane: `plan-marshall` (re-set 2026-11-12 by the operator's size rule: 19 files, six deliverables of new components, derived images and
browser evidence; it had been emitted on the runbook lane by mistake and not started). Overlap now running: PLAN-147
(`routes/dev/components/+page.svelte`); whichever lands second rebases.

```text
/plan-marshall task="implement .plan/orchestrator/ui-improvements/plans/PLAN-131-web-mascot-figure-and-surfaces.md"
```

## Write-Boundary

The session touches only its own repository source and tests and creates NO file under `.plan/orchestrator/` other than its
own `inbox/{sender}-{seq}` message; the orchestrator owns every other ledger write (`orchestration-model.md` § Ledger
Write-Boundary).

## Operator Decisions 2026-11-10 (Amendment)

Recorded in the epic's decision log (AskUserQuestion, 2026-11-10). They settle the open points above and supersede any text here that differs.

- **Rail-foot bear (O4):** the bust is cropped just below the shoulders and shows the top of the clipboard; this plan amends the line of `brand-mark-mascot.adoc:78` to say so and builds the rail foot at its low opacity.
- **WebP files (O5):** made once with a tool already on the machine (for example `cwebp` or ImageMagick); the exact command and crop box are recorded in `brand-mark-mascot.adoc` § `#assets`; no dependency is added.

## PLAN-130 Landing Residue (Amendment 2026-11-11)

Routed here by the operator (AskUserQuestion, 2026-11-11) from `landings/PLAN-130.md` (#414, squash `078b2ec5`). It supersedes any
text above that differs. Re-read each point at the then-current HEAD before changing it.

6. **The brand pieces' residue**
 - What:
   - `BrandMark.svelte:19` declares `size?: 16 | 20 | 24 | 28 | 64` for both renderings, so `rendering="colour" size={20}` type-checks
     and draws nothing (`DELIVERED_SIZES` at `:6`). Make the props a discriminated type per rendering (colour: 16, 28, 64; monochrome:
     16, 20, 24, 28, 64) so a size the rendering is not delivered at is refused by `svelte-check`; keep the runtime refusal.
   - `favicon.ts:5-6`: the header comment still says "light-theme brand colour"; say what the tab copies are now (the palette
     baked into `static/brand-mark*.svg`). Comment only; behaviour unchanged.
   - `pm-web-e2e/tests/brand-s12-screenshots.spec.js` (414 lines, added by PLAN-130 as one-shot evidence): it imports
     `/src/lib/stores/runtime.ts` inside `page.evaluate` (`:288`, so it only runs against the Vite dev server), renders the tab icon
     at 64 px (`:301-317`) where S12 is about 16 px, asserts nothing, and runs in every e2e run. Remove it from the permanent suite
     (the S12 verdict is recorded in #414's body), or keep it only as a tagged, opt-in spec that asserts and renders the tab icon
     at 16 px — the plan states which, with its reason.
   - `WaitingMark.svelte:60-64`: with `stroke-linecap: round`, `stroke-dasharray: 1` and `stroke-dashoffset: 1`, a tick may show a
     round-cap dot at the start frame. Check in the browser at 20 px (light and dark); if the dot shows, hide the tick until it
     draws (for example opacity `0` at the start frame); reduced motion unchanged.
   - The literal `brand-mark-plate` stays only as absence assertions (`BrandMark.test.ts:208-210`, `NavRail.test.ts:81-82`,
     `chrome.spec.js:125-126`); that is intended — no change.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing, pm-dev-frontend:css
 - Done when:
   - A test (or a `svelte-check` fixture with `@ts-expect-error`) fails when a colour `BrandMark` is given `size={20}`. Evidence
     order: `test-first`.
   - The S12 spec is either gone or opt-in, asserting and rendering the tab icon at 16 px; the default e2e run no longer depends on
     the dev server's module path.
   - The browser check of deliverable 5 includes the waiting mark's start frame at 20 px.

Expected surface added: `pm-web-app/src/lib/components/BrandMark.svelte`, `pm-web-app/src/lib/components/BrandMark.test.ts`,
`pm-web-app/src/lib/favicon.ts`, `pm-web-app/src/lib/components/WaitingMark.svelte`, `pm-web-app/src/lib/components/WaitingMark.test.ts`,
`pm-web-e2e/tests/brand-s12-screenshots.spec.js`.

Also observed (no change here): deliverable 3 replaces `EmptyState`'s `brand` mark, which PLAN-130 drew monochrome at 64 px against
`brand-mark.adoc:180` ("Not used: the empty states") — this plan's `mascot` removes that use; the dependency on `PLAN-130` is satisfied.
